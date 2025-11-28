package ortus.boxlang.ai.guardrails;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class GuardrailResultTest extends BaseIntegrationTest {

	@DisplayName( "Can create a passing GuardrailResult" )
	@Test
	public void testPassResult() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.guardrails.GuardrailResult;
				result = GuardrailResult::pass( "TestGuardrail", "All good" )
				isPassed = result.isPassed()
				isFailed = result.isFailed()
				isWarning = result.isWarning()
				name = result.getGuardrailName()
				message = result.getMessage()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isNotNull();
		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isFalse();
		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "TestGuardrail" );
		assertThat( variables.getAsString( Key.of( "message" ) ) ).isEqualTo( "All good" );
	}

	@DisplayName( "Can create a failing GuardrailResult" )
	@Test
	public void testFailResult() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.guardrails.GuardrailResult;
				result = GuardrailResult::fail( "TestGuardrail", "Validation failed", [ "violation1", "violation2" ] )
				isPassed = result.isPassed()
				isFailed = result.isFailed()
				hasViolations = result.hasViolations()
				violations = result.getViolations()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isNotNull();
		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasViolations" ) ) ).isTrue();
		Array violations = ( Array ) variables.get( Key.of( "violations" ) );
		assertThat( violations.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Can create a warning GuardrailResult" )
	@Test
	public void testWarningResult() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.guardrails.GuardrailResult;
				result = GuardrailResult::warning( "TestGuardrail", "Something suspicious" )
				isPassed = result.isPassed()
				isFailed = result.isFailed()
				isWarning = result.isWarning()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isNotNull();
		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isFailed" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isWarning" ) ) ).isTrue();
	}

	@DisplayName( "Can add violations to a result" )
	@Test
	public void testAddViolations() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.guardrails.GuardrailResult;
				result = GuardrailResult::pass( "Test" )
					.addViolation( "violation1" )
					.addViolation( { type: "custom", message: "Custom violation" } )
				hasViolations = result.hasViolations()
				violations = result.getViolations()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasViolations" ) ) ).isTrue();
		Array violations = ( Array ) variables.get( Key.of( "violations" ) );
		assertThat( violations.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Can set and get metadata" )
	@Test
	public void testMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.guardrails.GuardrailResult;
				result = GuardrailResult::pass( "Test" )
					.setMeta( "tokenCount", 100 )
					.setMeta( "provider", "openai" )
				tokenCount = result.getMeta( "tokenCount" )
				provider = result.getMeta( "provider" )
				missing = result.getMeta( "missing", "default" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "tokenCount" ) ) ).intValue() ).isEqualTo( 100 );
		assertThat( variables.getAsString( Key.of( "provider" ) ) ).isEqualTo( "openai" );
		assertThat( variables.getAsString( Key.of( "missing" ) ) ).isEqualTo( "default" );
	}

	@DisplayName( "Can track modifications" )
	@Test
	public void testModifications() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.guardrails.GuardrailResult;
				result = GuardrailResult::pass( "Test" )
					.withModification( "original content", "redacted content" )
				wasModified = result.getWasModified()
				original = result.getOriginalContent()
				modified = result.getModifiedContent()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "wasModified" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "original" ) ) ).isEqualTo( "original content" );
		assertThat( variables.getAsString( Key.of( "modified" ) ) ).isEqualTo( "redacted content" );
	}

	@DisplayName( "Can convert result to struct and JSON" )
	@Test
	public void testSerialization() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.guardrails.GuardrailResult;
				result = GuardrailResult::fail( "TestGuardrail", "Failed", [ "v1" ] )
				resultStruct = result.toStruct()
				resultJson = result.toJSON()
		    """,
		    context
		);
		// @formatter:on

		IStruct resultStruct = ( IStruct ) variables.get( Key.of( "resultStruct" ) );
		assertThat( resultStruct.getAsString( Key.of( "status" ) ) ).isEqualTo( "fail" );
		assertThat( resultStruct.getAsString( Key.of( "guardrailName" ) ) ).isEqualTo( "TestGuardrail" );

		String json = variables.getAsString( Key.of( "resultJson" ) );
		assertThat( json ).contains( "fail" );
		assertThat( json ).contains( "TestGuardrail" );
	}

}
