package ortus.boxlang.ai.guardrails;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class GuardrailChainTest extends BaseIntegrationTest {

	@DisplayName( "Can create a GuardrailChain using aiGuardrailChain BIF" )
	@Test
	public void testCreateChain() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain()
				guardrails = chain.getGuardrails()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "chain" ) ).isNotNull();
		assertThat( ( ( ortus.boxlang.runtime.types.Array ) variables.get( Key.of( "guardrails" ) ) ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can add guardrails to chain" )
	@Test
	public void testAddGuardrails() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain()
					.add( aiGuardrail( "contentFilter" ).block( "test" ) )
					.add( aiGuardrail( "pii" ) )
					.add( aiGuardrail( "tokenLimit" ).setMaxTokens( 100 ) )

				guardrails = chain.getGuardrails()
				count = guardrails.len()
		    """,
		    context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "count" ) ) ).intValue() ).isEqualTo( 3 );
	}

	@DisplayName( "Can add multiple guardrails at once" )
	@Test
	public void testAddAll() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain()
					.addAll( [
						aiGuardrail( "contentFilter" ),
						aiGuardrail( "pii" )
					] )

				count = chain.getGuardrails().len()
		    """,
		    context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "count" ) ) ).intValue() ).isEqualTo( 2 );
	}

	@DisplayName( "Can initialize chain with guardrails" )
	@Test
	public void testInitWithGuardrails() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain( [
					aiGuardrail( "contentFilter" ),
					aiGuardrail( "pii" )
				] )

				count = chain.getGuardrails().len()
		    """,
		    context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "count" ) ) ).intValue() ).isEqualTo( 2 );
	}

	@DisplayName( "Can validate request through chain" )
	@Test
	public void testValidateRequest() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain( [
					aiGuardrail( "contentFilter" )
				], { throwOnFailure: false } )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Hello world" } ]
					}
				}

				chainResult = chain.validateRequest( data )
				isPassed = chainResult.isPassed()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isTrue();
	}

	@DisplayName( "Chain stops on first failure by default" )
	@Test
	public void testStopOnFirstFailure() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain( [
					aiGuardrail( "contentFilter" ).block( "hello" ),
					aiGuardrail( "contentFilter" ).block( "world" )
				], { throwOnFailure: false } )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "hello world" } ]
					}
				}

				chainResult = chain.validateRequest( data )
				isPassed = chainResult.isPassed()
				failureCount = chainResult.getFailures().len()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isFalse();
		// Only one failure because chain stops on first
		assertThat( ( ( Number ) variables.get( Key.of( "failureCount" ) ) ).intValue() ).isEqualTo( 1 );
	}

	@DisplayName( "Chain can continue after failures" )
	@Test
	public void testContinueAfterFailure() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain( [
					aiGuardrail( "contentFilter" ).block( "hello" ),
					aiGuardrail( "contentFilter" ).block( "world" )
				], { throwOnFailure: false, stopOnFirstFailure: false } )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "hello world" } ]
					}
				}

				chainResult = chain.validateRequest( data )
				failureCount = chainResult.getFailures().len()
		    """,
		    context
		);
		// @formatter:on

		// Both guardrails should have failed
		assertThat( ( ( Number ) variables.get( Key.of( "failureCount" ) ) ).intValue() ).isEqualTo( 2 );
	}

	@DisplayName( "Can remove guardrail from chain" )
	@Test
	public void testRemoveGuardrail() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain()
					.add( aiGuardrail( "contentFilter" ) )
					.add( aiGuardrail( "pii" ) )
					.remove( "ContentFilter" )

				count = chain.getGuardrails().len()
				hasContentFilter = chain.hasGuardrail( "ContentFilter" )
				hasPII = chain.hasGuardrail( "PIIRedaction" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "count" ) ) ).intValue() ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "hasContentFilter" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "hasPII" ) ) ).isTrue();
	}

	@DisplayName( "Can get guardrail by name" )
	@Test
	public void testGetGuardrailByName() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain()
					.add( aiGuardrail( "contentFilter" ) )

				guardrail = chain.getGuardrail( "ContentFilter" )
				name = guardrail.getName()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "ContentFilter" );
	}

	@DisplayName( "ChainResult provides summary" )
	@Test
	public void testChainResultSummary() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain( [
					aiGuardrail( "contentFilter" ),
					aiGuardrail( "pii" )
				], { throwOnFailure: false } )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Safe content" } ]
					}
				}

				chainResult = chain.validateRequest( data )
				summary = chainResult.getSummary()
				counts = chainResult.getCounts()
		    """,
		    context
		);
		// @formatter:on

		String summary = variables.getAsString( Key.of( "summary" ) );
		assertThat( summary ).contains( "passed" );

		IStruct counts = ( IStruct ) variables.get( Key.of( "counts" ) );
		assertThat( ( ( Number ) counts.get( Key.of( "total" ) ) ).intValue() ).isEqualTo( 2 );
		assertThat( ( ( Number ) counts.get( Key.of( "passed" ) ) ).intValue() ).isEqualTo( 2 );
	}

	@DisplayName( "ChainResult can be serialized" )
	@Test
	public void testChainResultSerialization() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain( [
					aiGuardrail( "contentFilter" )
				], { throwOnFailure: false } )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "Hello" } ]
					}
				}

				chainResult = chain.validateRequest( data )
				resultStruct = chainResult.toStruct()
				resultJson = chainResult.toJSON()
		    """,
		    context
		);
		// @formatter:on

		IStruct resultStruct = ( IStruct ) variables.get( Key.of( "resultStruct" ) );
		assertThat( resultStruct.containsKey( Key.of( "passed" ) ) ).isTrue();
		assertThat( resultStruct.containsKey( Key.of( "results" ) ) ).isTrue();

		String json = variables.getAsString( Key.of( "resultJson" ) );
		assertThat( json ).contains( "passed" );
	}

	@DisplayName( "Skips disabled guardrails" )
	@Test
	public void testSkipDisabledGuardrails() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain( [
					aiGuardrail( "contentFilter" ).block( "hello" ).setEnabled( false ),
					aiGuardrail( "contentFilter" ).block( "world" )
				], { throwOnFailure: false } )

				data = {
					aiRequest: {
						getMessages: () => [ { role: "user", content: "hello there" } ]
					}
				}

				chainResult = chain.validateRequest( data )
				isPassed = chainResult.isPassed()
		    """,
		    context
		);
		// @formatter:on

		// First guardrail is disabled, so "hello" is not blocked
		assertThat( variables.getAsBoolean( Key.of( "isPassed" ) ) ).isTrue();
	}

	@DisplayName( "Can clear all guardrails" )
	@Test
	public void testClearGuardrails() {
		// @formatter:off
		runtime.executeSource(
		    """
				chain = aiGuardrailChain( [
					aiGuardrail( "contentFilter" ),
					aiGuardrail( "pii" )
				] ).clear()

				count = chain.getGuardrails().len()
		    """,
		    context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "count" ) ) ).intValue() ).isEqualTo( 0 );
	}

}
