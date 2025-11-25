package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class aiMemoryTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Test aiMemory() creates WindowMemory instance by default" )
	public void testDefaultMemoryCreation() {
		runtime.executeSource(
		    """
		    result = aiMemory()
		    isEmpty = result.isEmpty()
		    """,
		    context
		);

		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isNotNull();
		// Verify it's a working memory instance by checking isEmpty method
		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test aiMemory() with explicit WindowMemory type" )
	public void testExplicitWindowMemory() {
		runtime.executeSource(
		    """
		    result = aiMemory( "WindowMemory" )
		    """,
		    context
		);

		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test aiMemory() with config" )
	public void testMemoryWithConfig() {
		runtime.executeSource(
		    """
		    memory = aiMemory( config = {
		        maxMessages: 100,
		        autoTrim: true
		    } )
		    config = memory.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertThat( config.get( "maxMessages" ) ).isEqualTo( 100 );
		assertThat( config.get( "autoTrim" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Test aiMemory() throws error for unknown type" )
	public void testUnknownMemoryType() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    result = aiMemory( "UnknownType" )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test memory key() getter and setter" )
	public void testMemoryKey() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .key( "session-123" )

		    result = memory.key()
		    """,
		    context
		);

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "session-123" );
	}

	@Test
	@DisplayName( "Test memory metadata() getter and setter" )
	public void testMemoryMetadata() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .metadata( { userId: 123, sessionId: "abc" } )

		    result = memory.metadata()
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( "userId" ) ).isEqualTo( 123 );
		assertThat( result.getAsString( Key.of( "sessionId" ) ) ).isEqualTo( "abc" );
	}

	@Test
	@DisplayName( "Test adding string message to memory" )
	public void testAddStringMessage() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .add( "Hello, how are you?" )

		    messages = memory.getAll()
		    """,
		    context
		);

		var messages = variables.getAsArray( Key.of( "messages" ) );
		assertThat( messages.size() ).isEqualTo( 1 );

		IStruct firstMsg = ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, how are you?" );
	}

	@Test
	@DisplayName( "Test adding struct message to memory" )
	public void testAddStructMessage() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .add( { role: "assistant", content: "I'm doing well!" } )

		    messages = memory.getAll()
		    """,
		    context
		);

		var messages = variables.getAsArray( Key.of( "messages" ) );
		assertThat( messages.size() ).isEqualTo( 1 );

		IStruct firstMsg = ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "role" ) ) ).isEqualTo( "assistant" );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "I'm doing well!" );
	}

	@Test
	@DisplayName( "Test adding array of messages to memory" )
	public void testAddArrayMessages() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .add( [
		            { role: "user", content: "Hello" },
		            { role: "assistant", content: "Hi there!" }
		        ] )

		    count = memory.count()
		    """,
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		assertThat( count ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test memory count() and isEmpty()" )
	public void testCountAndIsEmpty() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		    emptyCheck = memory.isEmpty()

		    memory.add( "Test message" )
		    count = memory.count()
		    notEmptyCheck = memory.isEmpty()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "emptyCheck" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "notEmptyCheck" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Test memory clear()" )
	public void testClear() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .add( "Message 1" )
		        .add( "Message 2" )

		    beforeClear = memory.count()
		    memory.clear()
		    afterClear = memory.count()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "beforeClear" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "afterClear" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test getRecent() with limit" )
	public void testGetRecent() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .add( "Message 1" )
		        .add( "Message 2" )
		        .add( "Message 3" )
		        .add( "Message 4" )
		        .add( "Message 5" )

		    recent = memory.getRecent( 3 )
		    """,
		    context
		);

		var recent = variables.getAsArray( Key.of( "recent" ) );
		assertThat( recent.size() ).isEqualTo( 3 );

		// Should get the last 3 messages (3, 4, 5)
		IStruct firstRecent = ( IStruct ) recent.get( 0 );
		assertThat( firstRecent.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 3" );
	}

	@Test
	@DisplayName( "Test getByRole() filter" )
	public void testGetByRole() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .add( { role: "user", content: "User message 1" } )
		        .add( { role: "assistant", content: "Assistant reply" } )
		        .add( { role: "user", content: "User message 2" } )

		    userMessages = memory.getByRole( "user" )
		    assistantMessages = memory.getByRole( "assistant" )
		    """,
		    context
		);

		var	userMessages		= variables.getAsArray( Key.of( "userMessages" ) );
		var	assistantMessages	= variables.getAsArray( Key.of( "assistantMessages" ) );

		assertThat( userMessages.size() ).isEqualTo( 2 );
		assertThat( assistantMessages.size() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test system message management" )
	public void testSystemMessage() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .setSystemMessage( "You are a helpful assistant" )
		        .add( "User message" )

		    systemMsg = memory.getSystemMessage()
		    allMessages = memory.getAll()
		    nonSystemMessages = memory.getNonSystemMessages()

		    memory.removeSystemMessage()
		    afterRemoval = memory.getSystemMessage()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "You are a helpful assistant" );
		assertThat( variables.getAsArray( Key.of( "allMessages" ) ).size() ).isEqualTo( 2 );
		assertThat( variables.getAsArray( Key.of( "nonSystemMessages" ) ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsString( Key.of( "afterRemoval" ) ) ).isEmpty();
	}

	@Test
	@DisplayName( "Test fluent chaining" )
	public void testFluentChaining() {
		runtime.executeSource(
		    """
		    memory = aiMemory()
		        .key( "test-session" )
		        .metadata( { userId: 456 } )
		        .setSystemMessage( "Be helpful" )
		        .add( "First message" )
		        .add( { role: "assistant", content: "Got it!" } )

		    result = {
		        key: memory.key(),
		        count: memory.count(),
		        hasSystem: len( memory.getSystemMessage() ) > 0
		    }
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "key" ) ) ).isEqualTo( "test-session" );
		assertThat( result.get( "count" ) ).isEqualTo( 3 ); // system + 2 messages
		assertThat( result.getAsBoolean( Key.of( "hasSystem" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test WindowMemory export/import" )
	public void testExportImport() {
		runtime.executeSource(
		    """
		    original = aiMemory()
		        .key( "export-test" )
		        .metadata( { test: true } )
		        .add( "Test message" )

		    exported = original.export()

		    imported = aiMemory()
		        .import( exported )

		    result = {
		        key: imported.key(),
		        count: imported.count(),
		        metadata: imported.metadata()
		    }
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "key" ) ) ).isEqualTo( "export-test" );
		assertThat( result.get( "count" ) ).isEqualTo( 1 );

		IStruct metadata = ( IStruct ) result.get( "metadata" );
		assertThat( metadata.getAsBoolean( Key.of( "test" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SummaryMemory creation" )
	public void testSummaryMemoryCreation() {
		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "summary",
		        config: {
		            maxMessages: 20,
		            summaryThreshold: 10,
		            summaryModel: "gpt-4o-mini"
		        }
		    )

		    summary = memory.getSummary()
		    """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.get( "maxMessages" ) ).isEqualTo( 20 );
		assertThat( summary.get( "summaryThreshold" ) ).isEqualTo( 10 );
		assertThat( summary.getAsString( Key.of( "summaryModel" ) ) ).isEqualTo( "gpt-4o-mini" );
	}

	@Test
	@DisplayName( "Test SummaryMemory basic operations" )
	public void testSummaryMemoryBasicOps() {
		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "summary",
		        config: {
		            maxMessages: 5,
		            summaryThreshold: 2
		        }
		    )

		    // Add messages below threshold
		    memory.add( "Message 1" )
		    memory.add( "Message 2" )
		    memory.add( "Message 3" )

		    count = memory.count()
		    isEmpty = memory.isEmpty()
		    """,
		    context
		);

		assertThat( variables.get( "count" ) ).isEqualTo( 3 );
		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Test SummaryMemory with actual LLM summarization (integration)" )
	public void testSummaryMemoryWithLLM() {
		runtime.executeSource(
		    """
		    // Create summary memory with small limits to force summarization
		    memory = aiMemory(
		        memory: "summary",
		        config: {
		            maxMessages: 6,
		            summaryThreshold: 3,
		            summaryModel: "gpt-4o-mini",
		            summaryProvider: "openai"
		        }
		    )

		    // Add system message
		    memory.add( aiMessage().system( "You are a helpful assistant" ) )

		    // Simulate a conversation that will trigger summarization
		    memory.add( aiMessage().user( "My favorite color is blue" ) )
		    memory.add( aiMessage().assistant( "That's nice! Blue is a calming color." ) )
		    memory.add( aiMessage().user( "I also love playing guitar" ) )
		    memory.add( aiMessage().assistant( "Guitar is a wonderful instrument!" ) )
		    memory.add( aiMessage().user( "And my name is Alice" ) )
		    memory.add( aiMessage().assistant( "Nice to meet you, Alice!" ) )

		    // This should trigger summarization (7 total messages, exceeds maxMessages of 6)
		    memory.add( aiMessage().user( "What's my favorite color?" ) )

		    // Get all messages to verify summarization occurred
		    allMessages = memory.getAll()
		    summary = memory.getSummary()

		    // Check that we have a summary
		    hasSummaryMessage = false
		    summaryContent = ""

		    allMessages.each( function( msg ) {
		        if ( msg.keyExists( "isSummary" ) && msg.isSummary ) {
		            hasSummaryMessage = true
		            summaryContent = msg.content
		        }
		    } )

		    result = {
		        hasSummary: summary.hasSummary,
		        hasSummaryMessage: hasSummaryMessage,
		        messageCount: allMessages.len(),
		        summaryContainsBlue: summaryContent.findNoCase( "blue" ) > 0,
		        summaryContainsGuitar: summaryContent.findNoCase( "guitar" ) > 0
		    }
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );

		// Verify summarization occurred
		assertThat( result.getAsBoolean( Key.of( "hasSummary" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "hasSummaryMessage" ) ) ).isTrue();

		// Verify messages were compressed (should be less than 7)
		assertThat( ( int ) result.get( "messageCount" ) ).isLessThan( 7 );

		// Verify summary contains key information from old messages
		assertThat( result.getAsBoolean( Key.of( "summaryContainsBlue" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "summaryContainsGuitar" ) ) ).isTrue();
	}

}
