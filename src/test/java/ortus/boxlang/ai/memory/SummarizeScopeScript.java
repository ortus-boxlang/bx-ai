package ortus.boxlang.ai.memory;

/**
 * The BoxLang script the memory tests run to check that summarize() keeps each conversation's summary to itself.
 *
 * One memory, built with no user or conversation of its own, holds two conversations (alice/c1 and bob/c2) through
 * the per-call ids. Alice's is summarized, then Bob's, then Alice's again after a few more messages. The mock
 * provider records every request, so the script reads back exactly what each summary prompt sent:
 *
 * - bobPromptHasPrevious: Bob's first summary prompt carries an earlier summary. It must be false: Bob has none, and
 * before the fix it was Alice's (measured on cache and file memory).
 * - alicePromptHasPrevious: Alice's second summary prompt carries her own earlier summary. It must be true.
 */
final class SummarizeScopeScript {

	private SummarizeScopeScript() {
	}

	/**
	 * @param memoryExpression BoxLang expression that builds the memory, for example {@code aiMemory( "cache", "k" )}
	 *
	 * @return The script, which sets bobPromptHasPrevious, alicePromptHasPrevious, aliceCount and bobCount
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
		       lastPrompt = () => MockService::getRecorded().last().messages.last().content
		       config = { provider : "mock", model : "mock-model", keepRecent : 2 }

		       fill( "alice", "alice", "c1", 6 )
		       fill( "bob", "bob", "c2", 6 )
		       MockService::clearRecorded()

		       memory.summarize( config, "alice", "c1" )
		       memory.summarize( config, "bob", "c2" )
		       bobPromptHasPrevious = lastPrompt().find( "Previous conversation summary" ) > 0

		       fill( "alice-more", "alice", "c1", 4 )
		       memory.summarize( config, "alice", "c1" )
		       alicePromptHasPrevious = lastPrompt().find( "Previous conversation summary" ) > 0

		       aliceCount = memory.getAll( "alice", "c1" ).len()
		       bobCount = memory.getAll( "bob", "c2" ).len()
		       memory.clear( "alice", "c1" )
		       memory.clear( "bob", "c2" )
		       """.formatted( memoryExpression );
	}

}
