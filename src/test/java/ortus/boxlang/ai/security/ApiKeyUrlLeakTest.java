package ortus.boxlang.ai.security;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Credential-in-URL leak protection.
 *
 * Most providers authenticate with an Authorization header, and buildRequestLog only ever logs
 * header NAMES. Gemini authenticates with a `?key=` QUERY PARAMETER, and it used to bake that key
 * into `variables.chatURL` — an instance property on a long-lived shared service — which
 * BaseService then printed verbatim as `Endpoint: ...` whenever logRequest / logRequestToConsole
 * was enabled. That put a live API key into the `ai` log and stdout.
 *
 * Two independent defenses are asserted:
 * 1. PromptSecurity::redactURLSecrets() masks secret query params in anything logged — this
 * protects EVERY provider, including any future key-in-URL one.
 * 2. GeminiService keeps the key out of shared instance state entirely, so it can neither be
 * logged nor bleed between concurrent requests using different API keys.
 *
 * The source-level guards below are deliberate: they encode the exact shape of the original bug
 * and were verified to FAIL when it is reintroduced.
 */
@DisplayName( "API Key URL Leak Tests" )
public class ApiKeyUrlLeakTest extends BaseIntegrationTest {

	// ==================== The redactor itself (real production code) ====================

	@DisplayName( "redactURLSecrets masks the key but keeps non-secret params" )
	@Test
	public void testRedactsKeyKeepsOtherParams() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;

		        url    = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?key=LEAKY-KEY-999&alt=sse";
		        masked = PromptSecurity::redactURLSecrets( url );

		        stillLeaks = masked.findNoCase( "LEAKY-KEY-999" ) > 0;
		        wasMasked  = masked.findNoCase( "REDACTED" ) > 0;
		        keptParam  = masked.findNoCase( "alt=sse" ) > 0;
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "stillLeaks" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "wasMasked" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "keptParam" ) ) ).isTrue();
	}

	@DisplayName( "redactURLSecrets covers token / api_key / secret / signature variants" )
	@Test
	public void testRedactsOtherSecretParamNames() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;

		        leaked = [ "api_key", "apikey", "token", "access_token", "secret", "password", "signature" ]
		            .filter( p => PromptSecurity::redactURLSecrets( "https://x.dev/v1?" & p & "=TOPSECRET123" )
		                .findNoCase( "TOPSECRET123" ) > 0 );
		        leakedList = leaked.toList();
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "leakedList" ) ) ).isEmpty();
	}

	@DisplayName( "redactURLSecrets leaves a clean URL untouched" )
	@Test
	public void testCleanUrlUnchanged() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        url       = "https://api.openai.com/v1/chat/completions";
		        unchanged = PromptSecurity::redactURLSecrets( url ) == url;
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "unchanged" ) ) ).isTrue();
	}

	// ==================== Source guards: the bug must not come back ====================

	@DisplayName( "Gemini never assigns a key-bearing URL to shared instance state" )
	@Test
	public void testGeminiDoesNotAssignKeyToInstanceURL() throws IOException {
		String	src			= Files.readString(
		    Path.of( "src/main/bx/models/providers/GeminiService.bx" ),
		    StandardCharsets.UTF_8
		);

		// The original bug: `variables.chatURL = ... ?key=#chatRequest.getApiKey()#`
		String	offenders	= src.lines()
		    .filter( line -> line.contains( "variables.chatURL" )
		        && line.contains( "=" )
		        && line.contains( "key=#" ) )
		    .reduce( "", ( a, b ) -> a + b.trim() + "\n" );

		assertThat( offenders ).isEmpty();
	}

	@DisplayName( "every logged endpoint routes through the redactor" )
	@Test
	public void testLogBuildersRedact() throws IOException {
		String	src			= Files.readString(
		    Path.of( "src/main/bx/models/providers/BaseService.bx" ),
		    StandardCharsets.UTF_8
		);

		String	unredacted	= src.lines()
		    .filter( line -> line.contains( "\"Endpoint: #" ) )
		    .filter( line -> !line.contains( "redactURLSecrets(" ) )
		    .reduce( "", ( a, b ) -> a + b.trim() + "\n" );

		assertThat( unredacted ).isEmpty();
	}
}
