package ortus.boxlang.ai.memory;

import static com.google.common.truth.Truth.assertThat;

import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

/**
 * The BoxLang script every memory test runs to check that one memory keeps each user and conversation apart.
 *
 * One memory, built with no user or conversation of its own, is used through the per-call ids by alice/c1, alice/c2
 * and bob/c2, the way one agent serves several people. Every memory type must give the same answers: before, window
 * and summary memory kept one list for everyone (measured: an agent on the default window memory showed Bob all of
 * Alice's messages).
 */
final class ConversationIsolationScript {

	private ConversationIsolationScript() {
	}

	/**
	 * @param memoryExpression BoxLang expression that builds the memory, with no user or conversation of its own
	 *
	 * @return The script, which sets aliceC1, aliceC2, bobC2, ownCount, bobHasSystem, bobSeesAlice, aliceAfterBobClear,
	 *         aliceC1Summaries, bobAfterSummarize and bobPromptHasPrevious
	 */
	static String of( String memoryExpression ) {
		return """
		       import bxModules.bxai.models.providers.MockService;

		       memory = %s
		       fill = ( tag, userId, conversationId, count ) => {
		           for ( var i = 1; i <= count; i++ ) {
		               memory.add( { role : ( i %% 2 ? "user" : "assistant" ), content : "#tag#-#i#" }, userId, conversationId )
		           }
		       }

		       fill( "alice-one", "alice", "c1", 3 )
		       fill( "alice-two", "alice", "c2", 1 )
		       fill( "bob", "bob", "c2", 2 )
		       memory.setSystemMessage( "alice's instructions", "alice", "c1" )

		       aliceC1 = memory.getNonSystemMessages( "alice", "c1" ).len()
		       aliceC2 = memory.getAll( "alice", "c2" ).len()
		       bobC2 = memory.getAll( "bob", "c2" ).len()
		       ownCount = memory.getAll().len()
		       bobHasSystem = memory.hasSystemMessage( "bob", "c2" )
		       bobSeesAlice = memory.getAll( "bob", "c2" ).some( m => m.content contains "alice" )

		       memory.clear( "bob", "c2" )
		       aliceAfterBobClear = memory.getNonSystemMessages( "alice", "c1" ).len()

		       fill( "bob-again", "bob", "c2", 3 )
		       MockService::clearRecorded()
		       memory.summarize( { provider : "mock", model : "mock-model", keepRecent : 1 }, "alice", "c1" )
		       aliceC1Summaries = memory.getAll( "alice", "c1" ).filter( m => m.isSummary ?: false ).len()
		       bobAfterSummarize = memory.getAll( "bob", "c2" ).len()
		       memory.summarize( { provider : "mock", model : "mock-model", keepRecent : 1 }, "bob", "c2" )
		       bobPromptHasPrevious = MockService::getRecorded().last().messages.last().content.find( "Previous conversation summary" ) > 0

		       memory.clear( "alice", "c1" )
		       memory.clear( "alice", "c2" )
		       memory.clear( "bob", "c2" )
		       """.formatted( memoryExpression );
	}

	/**
	 * What every memory must report after {@link #of(String)} ran.
	 *
	 * @param variables The variables scope the script ran in
	 */
	static void assertIsolated( IScope variables ) {
		assertThat( variables.getAsInteger( Key.of( "aliceC1" ) ) ).isEqualTo( 3 );
		assertThat( variables.getAsInteger( Key.of( "aliceC2" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "bobC2" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "ownCount" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsBoolean( Key.of( "bobHasSystem" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "bobSeesAlice" ) ) ).isFalse();
		assertThat( variables.getAsInteger( Key.of( "aliceAfterBobClear" ) ) ).isEqualTo( 3 );
		assertThat( variables.getAsInteger( Key.of( "aliceC1Summaries" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "bobAfterSummarize" ) ) ).isEqualTo( 3 );
		assertThat( variables.getAsBoolean( Key.of( "bobPromptHasPrevious" ) ) ).isFalse();
	}

}
