package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Verifies the Phase 3 Part A universality fix: beforeLLMCall / afterLLMCall now fire on the
 * streaming path and expose the accumulated assistant content via streamState.content, so that
 * after/before-response middleware (OutputGuard, LLMGuard, logging, flight-recorder) work on
 * streaming — not just the OpenAI-family non-streaming path.
 *
 * These run offline against MockService (which extends OpenAIService and simulates the SSE
 * transport word-by-word). The four custom providers (Claude/Gemini/Cohere/Bedrock) share the
 * same fire contract and are exercised end-to-end by the live CI suite.
 */
@DisplayName( "Streaming Middleware Parity Tests (Phase 3 Part A)" )
public class StreamingMiddlewareParityTest extends BaseIntegrationTest {

	@DisplayName( "afterLLMCall fires on streaming and sees the accumulated content" )
	@Test
	public void testAfterLLMCallFiresOnStream() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        fired        = false;
		        seenContent  = "";
		        recorder     = {
		            afterLLMCall: ( context ) => {
		                fired       = true;
		                seenContent = context.streamState.content ?: "";
		                return AiMiddlewareResult::continue();
		            }
		        };

		        aiChatStream(
		            "Say hello world",
		            ( chunk ) => {},
		            {},
		            {
		                provider       : "mock",
		                providerOptions: { responses: [ "hello world from the stream" ] },
		                middleware     : [ recorder ]
		            }
		        );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "fired" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "seenContent" ) ) ).contains( "hello world" );
	}

	@DisplayName( "beforeLLMCall fires on streaming" )
	@Test
	public void testBeforeLLMCallFiresOnStream() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        beforeFired = false;
		        recorder    = {
		            beforeLLMCall: ( context ) => {
		                beforeFired = true;
		                return AiMiddlewareResult::continue();
		            }
		        };

		        aiChatStream(
		            "hi",
		            ( chunk ) => {},
		            {},
		            {
		                provider       : "mock",
		                providerOptions: { responses: [ "some streamed answer" ] },
		                middleware     : [ recorder ]
		            }
		        );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "beforeFired" ) ) ).isTrue();
	}

	@DisplayName( "OutputGuard redacts secrets on the streaming path" )
	@Test
	public void testOutputGuardRedactsOnStream() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;

        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        guard    = new OutputGuardMiddleware( action: "redact" );
		        finalCtx = { "content": "" };

		        // afterLLMCall fires in REVERSE order, so with [ recorder, guard ] the guard runs
		        // first (redacting streamState.content in place) and the recorder runs after it,
		        // observing the already-redacted content.
		        recorder = {
		            afterLLMCall: ( context ) => {
		                finalCtx.content = context.streamState.content ?: "";
		                return AiMiddlewareResult::continue();
		            }
		        };

		        aiChatStream(
		            "leak",
		            ( chunk ) => {},
		            {},
		            {
		                provider       : "mock",
		                providerOptions: { responses: [ "your key is AKIAIOSFODNN7EXAMPLE ok" ] },
		                middleware     : [ recorder, guard ]
		            }
		        );

		        masked = finalCtx.content.contains( "[REDACTED]" ) && !finalCtx.content.contains( "AKIAIOSFODNN7EXAMPLE" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}
}
