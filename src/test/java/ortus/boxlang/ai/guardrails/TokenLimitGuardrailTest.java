package ortus.boxlang.ai.guardrails;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class TokenLimitGuardrailTest extends BaseIntegrationTest {

	@DisplayName( "Can create a TokenLimitGuardrail using aiGuardrail BIF" )
	@Test
	public void testCreateGuardrail() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "tokenLimit" )
				name = guardrail.getName()
				isEnabled = guardrail.isEnabled()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "guardrail" ) ).isNotNull();
		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "TokenLimit" );
		assertThat( variables.getAsBoolean( Key.of( "isEnabled" ) ) ).isTrue();
	}

	@DisplayName( "Can estimate tokens" )
	@Test
	public void testEstimateTokens() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "tokenLimit" )
				// Default is ~4 chars per token
				tokens = guardrail.estimateTokens( "Hello world, this is a test message" )
		    """,
		    context
		);
		// @formatter:on

		// "Hello world, this is a test message" = 36 chars / 4 = 9 tokens
		int tokens = ( ( Number ) variables.get( Key.of( "tokens" ) ) ).intValue();
		assertThat( tokens ).isEqualTo( 9 );
	}

	@DisplayName( "Fails when exceeding max tokens" )
	@Test
	public void testExceedMaxTokens() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "tokenLimit" )
					.setMaxTokens( 10 )

				// Create content that exceeds 10 tokens (~40+ chars)
				data = {
					aiRequest: {
						getMessages: () => [
							{ role: "user", content: "This is a very long message that definitely exceeds ten tokens by a significant margin" }
						]
					}
				}
				result = guardrail.validateRequest( data )
				isFailed = result.isFailed()
				estimatedTokens = result.getMeta( "estimatedTokens" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isTrue();
		int estimated = ( ( Number ) variables.get( Key.of( "estimatedTokens" ) ) ).intValue();
		assertThat( estimated ).isGreaterThan( 10 );
	}

	@DisplayName( "Passes when within token limits" )
	@Test
	public void testWithinLimits() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "tokenLimit" )
					.setMaxTokens( 100 )

				data = {
					aiRequest: {
						getMessages: () => [
							{ role: "user", content: "Hello world" }
						]
					}
				}
				result = guardrail.validateRequest( data )
				isPassed = result.isPassed()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isTrue();
	}

	@DisplayName( "Warns when approaching limit" )
	@Test
	public void testWarningThreshold() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "tokenLimit" )
					.setMaxTokens( 10 )
					.setWarningThreshold( 0.5 )  // Warn at 50%

				// Create content around 8 tokens (80% of limit) - should warn
				data = {
					aiRequest: {
						getMessages: () => [
							{ role: "user", content: "This is some text for testing" }
						]
					}
				}
				result = guardrail.validateRequest( data )
				isWarning = result.isWarning()
		    """,
		    context
		);
		// @formatter:on

		// Message is ~30 chars / 4 = ~8 tokens, which is 80% of 10
		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isTrue();
	}

	@DisplayName( "Can set max tokens per message" )
	@Test
	public void testMaxTokensPerMessage() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "tokenLimit" )
					.setMaxTokensPerMessage( 5 )

				data = {
					aiRequest: {
						getMessages: () => [
							{ role: "system", content: "Hi" },
							{ role: "user", content: "This is a longer message that exceeds five tokens" }
						]
					}
				}
				result = guardrail.validateRequest( data )
				isFailed = result.isFailed()
				violations = result.getViolations()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isTrue();
	}

	@DisplayName( "Can use word-based estimation" )
	@Test
	public void testWordBasedEstimation() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "tokenLimit" )
					.setEstimationMethod( "words" )
				// 7 words / 0.75 = ~10 tokens
				tokens = guardrail.estimateTokens( "Hello world this is a test message" )
		    """,
		    context
		);
		// @formatter:on

		int tokens = ( ( Number ) variables.get( Key.of( "tokens" ) ) ).intValue();
		assertThat( tokens ).isGreaterThan( 5 );
	}

	@DisplayName( "Passes with no limits set" )
	@Test
	public void testNoLimits() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "tokenLimit" )
				// No limits set - should pass

				data = {
					aiRequest: {
						getMessages: () => [
							{ role: "user", content: "Any content should pass" }
						]
					}
				}
				result = guardrail.validateRequest( data )
				isPassed = result.isPassed()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isTrue();
	}

	@DisplayName( "Can configure via constructor" )
	@Test
	public void testConstructorConfig() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "tokenLimit", {
					maxTokens: 50,
					maxTokensPerMessage: 25,
					warningThreshold: 0.7
				} )

				data = {
					aiRequest: {
						getMessages: () => [
							{ role: "user", content: "Short" }
						]
					}
				}
				result = guardrail.validateRequest( data )
				isPassed = result.isPassed()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isTrue();
	}

}
