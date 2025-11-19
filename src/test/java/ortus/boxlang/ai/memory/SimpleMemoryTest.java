package ortus.boxlang.ai.memory;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class SimpleMemoryTest extends BaseIntegrationTest {

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test SimpleMemory instantiation" )
	public void testInstantiation() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()
		    """,
		    context
		);

		var memory = variables.get( Key.of( "memory" ) );
		assertThat( memory ).isNotNull();
	}

	@Test
	@DisplayName( "Test SimpleMemory getSummary()" )
	public void testGetSummary() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()
		        .key( "test-key" )
		        .setSystemMessage( "Test system" )
		        .add( "User message" )

		    summary = memory.getSummary()
		    """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "type" ) ) ).isEqualTo( "SimpleMemory" );
		assertThat( summary.getAsString( Key.of( "key" ) ) ).isEqualTo( "test-key" );
		assertThat( summary.get( "messageCount" ) ).isEqualTo( 2 ); // system + user
		assertThat( summary.getAsBoolean( Key.of( "hasSystemMessage" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SimpleMemory export()" )
	public void testExport() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()
		        .key( "export-key" )
		        .metadata( { userId: 789 } )
		        .configure( { maxSize: 100 } )
		        .add( { role: "user", content: "Test" } )

		    exported = memory.export()
		    """,
		    context
		);

		var exported = variables.getAsStruct( Key.of( "exported" ) );
		assertThat( exported.getAsString( Key.of( "key" ) ) ).isEqualTo( "export-key" );
		assertThat( exported.containsKey( Key.of( "metadata" ) ) ).isTrue();
		assertThat( exported.containsKey( Key.of( "config" ) ) ).isTrue();
		assertThat( exported.containsKey( Key.of( "messages" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SimpleMemory import()" )
	public void testImport() {
		runtime.executeSource(
		    """
		    data = {
		        key: "imported-key",
		        metadata: { imported: true },
		        config: { setting: "value" },
		        messages: [
		            { role: "user", content: "Imported message" }
		        ]
		    }

		    memory = new bxModules.bxai.models.memory.SimpleMemory()
		        .import( data )

		    result = {
		        key: memory.key(),
		        count: memory.count(),
		        metadata: memory.metadata(),
		        config: memory.getConfig()
		    }
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "key" ) ) ).isEqualTo( "imported-key" );
		assertThat( result.get( "count" ) ).isEqualTo( 1 );

		IStruct metadata = ( IStruct ) result.get( "metadata" );
		assertThat( metadata.getAsBoolean( Key.of( "imported" ) ) ).isTrue();

		IStruct config = ( IStruct ) result.get( "config" );
		assertThat( config.getAsString( Key.of( "setting" ) ) ).isEqualTo( "value" );
	}

	@Test
	@DisplayName( "Test SimpleMemory export/import roundtrip" )
	public void testExportImportRoundtrip() {
		runtime.executeSource(
		    """
		    original = new bxModules.bxai.models.memory.SimpleMemory()
		        .key( "roundtrip" )
		        .metadata( { version: 1 } )
		        .setSystemMessage( "System prompt" )
		        .add( "User: Hello" )
		        .add( { role: "assistant", content: "Assistant: Hi!" } )

		    exported = original.export()

		    restored = new bxModules.bxai.models.memory.SimpleMemory()
		        .import( exported )

		    result = {
		        key: restored.key(),
		        count: restored.count(),
		        systemMsg: restored.getSystemMessage(),
		        messages: restored.getAll()
		    }
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "key" ) ) ).isEqualTo( "roundtrip" );
		assertThat( result.get( "count" ) ).isEqualTo( 3 );
		assertThat( result.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "System prompt" );
		assertThat( result.getAsArray( Key.of( "messages" ) ).size() ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test SimpleMemory message timestamps" )
	public void testMessageTimestamps() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()
		        .add( "Message with timestamp" )

		    messages = memory.getAll()
		    firstMessage = messages[1]
		    hasTimestamp = structKeyExists( firstMessage, "timestamp" )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasTimestamp" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SimpleMemory invalid message format throws error" )
	public void testInvalidMessageFormat() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    memory = new bxModules.bxai.models.memory.SimpleMemory()
			        .add( { invalidKey: "no role or content" } )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test SimpleMemory with AiMessage integration" )
	public void testAiMessageIntegration() {
		runtime.executeSource(
		    """
		       msg = aiMessage()
		           .system( "Be helpful" )
		           .user( "Hello" )

		       memory = new bxModules.bxai.models.memory.SimpleMemory()
		           .add( msg )

		       count = memory.count()
		    println( memory.getAll() )
		       """,
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		assertThat( count ).isEqualTo( 2 ); // system + user message
	}

	@Test
	@DisplayName( "Test SimpleMemory configuration persistence" )
	public void testConfigurationPersistence() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()
		        .configure( { option1: "value1" } )
		        .configure( { option2: "value2" } )

		    config = memory.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertThat( config.getAsString( Key.of( "option1" ) ) ).isEqualTo( "value1" );
		assertThat( config.getAsString( Key.of( "option2" ) ) ).isEqualTo( "value2" );
	}

	@Test
	@DisplayName( "Test SimpleMemory replaces system message" )
	public void testSystemMessageReplacement() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()
		        .setSystemMessage( "First system message" )
		        .add( "User message" )
		        .setSystemMessage( "Second system message" )

		    systemMsg = memory.getSystemMessage()
		    count = memory.count()
		    """,
		    context
		);

		// Should only have one system message (the second one)
		assertThat( variables.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "Second system message" );
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 2 ); // system + user
	}

	@Test
	@DisplayName( "Test SimpleMemory getRecent with more messages than limit" )
	public void testGetRecentWithLimit() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()

		    // Add 10 messages
		    for( i = 1; i <= 10; i++ ) {
		        memory.add( "Message " & i )
		    }

		    recent3 = memory.getRecent( 3 )
		    recent20 = memory.getRecent( 20 )
		    """,
		    context
		);

		var	recent3		= variables.getAsArray( Key.of( "recent3" ) );
		var	recent20	= variables.getAsArray( Key.of( "recent20" ) );

		assertThat( recent3.size() ).isEqualTo( 3 );
		assertThat( recent20.size() ).isEqualTo( 10 ); // Only 10 messages exist

		// Verify we got the last 3 messages (8, 9, 10)
		IStruct firstRecent = ( IStruct ) recent3.get( 0 );
		assertThat( firstRecent.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 8" );
	}

	@Test
	@DisplayName( "Test SimpleMemory search() case-insensitive" )
	public void testSearchCaseInsensitive() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()
		        .add( "Hello world" )
		        .add( "Goodbye world" )
		        .add( "Testing BoxLang" )
		        .add( "Another message" )

		    results = memory.search( "WORLD" )
		    noResults = memory.search( "notfound" )
		    """,
		    context
		);

		var	results		= variables.getAsArray( Key.of( "results" ) );
		var	noResults	= variables.getAsArray( Key.of( "noResults" ) );

		assertThat( results.size() ).isEqualTo( 2 );
		IStruct first = ( IStruct ) results.get( 0 );
		assertThat( first.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello world" );

		assertThat( noResults.size() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test SimpleMemory search() case-sensitive" )
	public void testSearchCaseSensitive() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()
		        .add( "Hello World" )
		        .add( "hello world" )
		        .add( "HELLO WORLD" )

		    results = memory.search( "World", true )
		    allResults = memory.search( "world", false )
		    """,
		    context
		);

		var	results		= variables.getAsArray( Key.of( "results" ) );
		var	allResults	= variables.getAsArray( Key.of( "allResults" ) );

		// Case-sensitive should only match "Hello World"
		assertThat( results.size() ).isEqualTo( 1 );
		IStruct match = ( IStruct ) results.get( 0 );
		assertThat( match.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello World" );

		// Case-insensitive should match all 3
		assertThat( allResults.size() ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test SimpleMemory getRange() basic" )
	public void testGetRangeBasic() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()

		    // Add 5 messages
		    for( i = 1; i <= 5; i++ ) {
		        memory.add( "Message " & i )
		    }

		    range = memory.getRange( 2, 4 )
		    """,
		    context
		);

		var range = variables.getAsArray( Key.of( "range" ) );

		assertThat( range.size() ).isEqualTo( 3 );
		IStruct	first	= ( IStruct ) range.get( 0 );
		IStruct	last	= ( IStruct ) range.get( 2 );
		assertThat( first.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 2" );
		assertThat( last.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 4" );
	}

	@Test
	@DisplayName( "Test SimpleMemory getRange() default endIndex" )
	public void testGetRangeDefaultEnd() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()

		    // Add 5 messages
		    for( i = 1; i <= 5; i++ ) {
		        memory.add( "Message " & i )
		    }

		    range = memory.getRange( 3 )
		    """,
		    context
		);

		var range = variables.getAsArray( Key.of( "range" ) );

		// Should get messages 3, 4, 5
		assertThat( range.size() ).isEqualTo( 3 );
		IStruct	first	= ( IStruct ) range.get( 0 );
		IStruct	last	= ( IStruct ) range.get( 2 );
		assertThat( first.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 3" );
		assertThat( last.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 5" );
	}

	@Test
	@DisplayName( "Test SimpleMemory getRange() invalid indices" )
	public void testGetRangeInvalidIndices() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SimpleMemory()

		    // Add 5 messages
		    for( i = 1; i <= 5; i++ ) {
		        memory.add( "Message " & i )
		    }

		    outOfBounds = memory.getRange( 10, 20 )
		    negativeStart = memory.getRange( 0, 3 )
		    endBeforeStart = memory.getRange( 4, 2 )
		    """,
		    context
		);

		var	outOfBounds		= variables.getAsArray( Key.of( "outOfBounds" ) );
		var	negativeStart	= variables.getAsArray( Key.of( "negativeStart" ) );
		var	endBeforeStart	= variables.getAsArray( Key.of( "endBeforeStart" ) );

		assertThat( outOfBounds.size() ).isEqualTo( 0 );
		assertThat( negativeStart.size() ).isEqualTo( 0 );
		assertThat( endBeforeStart.size() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test SimpleMemory clone()" )
	public void testClone() {
		runtime.executeSource(
		    """
		    original = new bxModules.bxai.models.memory.SimpleMemory()
		        .key( "original-key" )
		        .metadata( { userId: "123" } )
		        .add( "Message 1" )
		        .add( "Message 2" )

		    cloned = original.clone()

		    // Modify clone
		    cloned.add( "Message 3" )

		    originalCount = original.count()
		    clonedCount = cloned.count()
		    """,
		    context
		);

		var	originalCount	= variables.getAsInteger( Key.of( "originalCount" ) );
		var	clonedCount		= variables.getAsInteger( Key.of( "clonedCount" ) );

		assertThat( originalCount ).isEqualTo( 2 );
		assertThat( clonedCount ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test SimpleMemory merge() without duplicates" )
	public void testMergeNoDuplicates() {
		runtime.executeSource(
		    """
		    memory1 = new bxModules.bxai.models.memory.SimpleMemory()
		        .add( "Message 1" )
		        .add( "Message 2" )

		    memory2 = new bxModules.bxai.models.memory.SimpleMemory()
		        .add( "Message 3" )
		        .add( "Message 4" )

		    memory1.merge( memory2 )

		    count = memory1.count()
		    messages = memory1.getAll()
		    """,
		    context
		);

		var	count		= variables.getAsInteger( Key.of( "count" ) );
		var	messages	= variables.getAsArray( Key.of( "messages" ) );

		assertThat( count ).isEqualTo( 4 );
		IStruct last = ( IStruct ) messages.get( 3 );
		assertThat( last.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 4" );
	}

	@Test
	@DisplayName( "Test SimpleMemory merge() with skipDuplicates" )
	public void testMergeSkipDuplicates() {
		runtime.executeSource(
		    """
		    memory1 = new bxModules.bxai.models.memory.SimpleMemory()
		        .add( "Message 1" )
		        .add( "Message 2" )

		    memory2 = new bxModules.bxai.models.memory.SimpleMemory()
		        .add( "Message 2" )
		        .add( "Message 3" )

		    memory1.merge( memory2, true )

		    count = memory1.count()
		    messages = memory1.getAll()
		    """,
		    context
		);

		var	count		= variables.getAsInteger( Key.of( "count" ) );
		var	messages	= variables.getAsArray( Key.of( "messages" ) );

		// Should have 3 messages (1, 2, 3) - duplicate "Message 2" skipped
		assertThat( count ).isEqualTo( 3 );
		IStruct last = ( IStruct ) messages.get( 2 );
		assertThat( last.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 3" );
	}

}
