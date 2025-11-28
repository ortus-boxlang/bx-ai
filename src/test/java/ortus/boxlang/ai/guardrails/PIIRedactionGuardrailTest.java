package ortus.boxlang.ai.guardrails;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

public class PIIRedactionGuardrailTest extends BaseIntegrationTest {

	@DisplayName( "Can create a PIIRedactionGuardrail using aiGuardrail BIF" )
	@Test
	public void testCreateGuardrail() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
				name = guardrail.getName()
				isEnabled = guardrail.isEnabled()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "guardrail" ) ).isNotNull();
		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "PIIRedaction" );
		assertThat( variables.getAsBoolean( Key.of( "isEnabled" ) ) ).isTrue();
	}

	@DisplayName( "Can detect email addresses" )
	@Test
	public void testDetectEmail() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "warn" )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Contact me at john@example.com" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isWarning = result.isWarning()
				violations = result.getViolations()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isTrue();
		Array violations = ( Array ) variables.get( Key.of( "violations" ) );
		assertThat( violations.size() ).isEqualTo( 1 );
	}

	@DisplayName( "Can detect phone numbers" )
	@Test
	public void testDetectPhone() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "warn" )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Call me at 555-123-4567" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isWarning = result.isWarning()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isTrue();
	}

	@DisplayName( "Can detect SSN" )
	@Test
	public void testDetectSSN() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "warn" )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "My SSN is 123-45-6789" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isWarning = result.isWarning()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isTrue();
	}

	@DisplayName( "Can detect credit card numbers" )
	@Test
	public void testDetectCreditCard() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "warn" )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Pay with 4111-1111-1111-1111" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isWarning = result.isWarning()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isTrue();
	}

	@DisplayName( "Can block on PII detection" )
	@Test
	public void testBlockAction() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "block" )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "My email is test@test.com" } ]
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

	@DisplayName( "Can redact PII" )
	@Test
	public void testRedactAction() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "redact" )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Email: test@test.com" } ]
					},
					dataPacket: {
						messages: [ { role: "user", content: "Email: test@test.com" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isPassed = result.isPassed()
				wasModified = result.getWasModified()
				originalContent = result.getOriginalContent()
				modifiedContent = result.getModifiedContent()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "wasModified" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "originalContent" ) ) ).contains( "test@test.com" );
		assertThat( variables.getAsString( Key.of( "modifiedContent" ) ) ).contains( "[EMAIL]" );
	}

	@DisplayName( "Can enable/disable specific PII types" )
	@Test
	public void testPIITypeConfig() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "warn" )
					.setPIIType( "email", false )  // Disable email detection

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Email: test@test.com" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isPassed = result.isPassed()
		    """,
		    context
		);
		// @formatter:on

		// Email detection is disabled, so should pass
		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isTrue();
	}

	@DisplayName( "Can add custom PII patterns" )
	@Test
	public void testCustomPatterns() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "warn" )
					.disableAll()  // Disable default patterns
					.addPattern( "employeeId", "EMP-\\\\d{6}", "[EMPLOYEE_ID]" )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Employee EMP-123456 needs help" } ]
					}
				}
				result = guardrail.validateRequest( data )
				isWarning = result.isWarning()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isTrue();
	}

	@DisplayName( "Passes when no PII found" )
	@Test
	public void testNoPIIFound() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "warn" )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Hello world, how are you?" } ]
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

	@DisplayName( "Can check responses for PII" )
	@Test
	public void testResponsePIICheck() {
		// @formatter:off
		runtime.executeSource(
		    """
				guardrail = aiGuardrail( "pii" )
					.setAction( "warn" )

				data = {
					response: {
						choices: [
							{ message: { content: "Contact john@example.com for details" } }
						]
					}
				}
				result = guardrail.validateResponse( data )
				isWarning = result.isWarning()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isTrue();
	}

}
