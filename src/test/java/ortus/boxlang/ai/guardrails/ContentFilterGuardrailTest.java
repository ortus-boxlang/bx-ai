package ortus.boxlang.ai.guardrails;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

public class ContentFilterGuardrailTest extends BaseIntegrationTest {

	@DisplayName( "Can create a ContentFilterGuardrail using aiGuardrail BIF" )
	@Test
	public void testCreateGuardrail() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "contentFilter" )
				name = guardrail.getName()
				isEnabled = guardrail.isEnabled()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "guardrail" ) ).isNotNull();
		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "ContentFilter" );
		assertThat( variables.getAsBoolean( Key.of( "isEnabled" ) ) ).isTrue();
	}

	@DisplayName( "Can add blocked patterns" )
	@Test
	public void testBlockPatterns() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "contentFilter" )
					.block( "prohibited" )
					.block( "illegal" )
					.blockAll( [ "dangerous", "harmful" ] )

				// Create mock data with prohibited content
				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "This is prohibited content" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isFailed = result.isFailed()
				hasViolations = result.hasViolations()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasViolations" ) ) ).isTrue();
	}

	@DisplayName( "Can add warning patterns" )
	@Test
	public void testWarnPatterns() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "contentFilter" )
					.warn( "maybe" )
					.warnAll( [ "perhaps", "possibly" ] )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "maybe something" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isWarning = result.isWarning()
				isFailed = result.isFailed()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isFalse();
	}

	@DisplayName( "Passes when no patterns match" )
	@Test
	public void testPassWhenNoMatch() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "contentFilter" )
					.block( "prohibited" )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Hello world, this is fine" } ]
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

	@DisplayName( "Can check responses" )
	@Test
	public void testResponseValidation() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "contentFilter" )
					.block( "secret" )

				data = {
					response: {
						choices: [
							{ message: { content: "This contains a secret word" } }
						]
					}
				}
				result = guardrail.validateResponse( data )
				isFailed = result.isFailed()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isTrue();
	}

	@DisplayName( "Can enable/disable guardrail" )
	@Test
	public void testEnableDisable() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "contentFilter" )
					.block( "prohibited" )
					.setEnabled( false )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "prohibited content" } ]
					}
				}
				// Even though content matches, guardrail is disabled
				isEnabled = guardrail.isEnabled()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isEnabled" ) ) ).isFalse();
	}

	@DisplayName( "Can configure via constructor" )
	@Test
	public void testConstructorConfig() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "contentFilter", {
					blockedPatterns: [ "bad", "evil" ],
					warnPatterns: [ "suspicious" ],
					checkRequests: true,
					checkResponses: false
				} )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "bad content" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isFailed = result.isFailed()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isTrue();
	}

	@DisplayName( "Supports regex patterns" )
	@Test
	public void testRegexPatterns() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "contentFilter" )
					.block( "\\\\d{4}-\\\\d{4}-\\\\d{4}-\\\\d{4}" )  // Credit card pattern

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "My card is 1234-5678-9012-3456" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isFailed = result.isFailed()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isTrue();
	}

	@DisplayName( "Can clear patterns" )
	@Test
	public void testClearPatterns() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "contentFilter" )
					.block( "prohibited" )
					.clearPatterns()

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "prohibited content" } ]
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
