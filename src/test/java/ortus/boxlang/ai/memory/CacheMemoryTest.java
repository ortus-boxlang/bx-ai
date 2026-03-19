package ortus.boxlang.ai.memory;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class CacheMemoryTest extends BaseIntegrationTest {

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test CacheMemory instantiation with default cache" )
	public void testInstantiationDefault() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache" )
		        .key( "test-session" )
		        .configure( { cacheName: "default" } )

		    cacheName = memory.getSummary().cacheName
		    """,
		    context
		);

		var cacheName = variables.getAsString( Key.of( "cacheName" ) );
		assertThat( cacheName ).isEqualTo( "default" );
	}

	@Test
	@DisplayName( "Test CacheMemory with custom cache name via config" )
	public void testCustomCacheViaConfig() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "test-session-2" )
		        .configure( { cacheName: "default" } )

		    summary = memory.getSummary()
		    """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "cacheName" ) ) ).isEqualTo( "default" );
	}

	@Test
	@DisplayName( "Test CacheMemory persists messages" )
	public void testPersistence() {
		runtime.executeSource(
		    """
		    // Create memory and add messages
		    memory1 = aiMemory( "cache", "persist-test" )
		        .configure( { cacheName: "default" } )
		        .add( "First message" )
		        .add( "Second message" )

		    count1 = memory1.count()

		    // Create new instance with same key - should load from cache
		    memory2 = aiMemory( "cache", "persist-test" )
		        .configure( { cacheName: "default" } )

		    count2 = memory2.count()
		    messages = memory2.getAll()

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		var	count1	= variables.getAsInteger( Key.of( "count1" ) );
		var	count2	= variables.getAsInteger( Key.of( "count2" ) );

		assertThat( count1 ).isEqualTo( 2 );
		assertThat( count2 ).isEqualTo( 2 ); // Should load from cache
	}

	@Test
	@DisplayName( "Test CacheMemory clear removes from cache" )
	public void testClearRemovesFromCache() {
		runtime.executeSource(
		    """
		    // Create memory and add messages
		    memory1 = aiMemory( "cache", "clear-test" )
		        .configure( { cacheName: "default" } )
		        .add( "Test message" )

		    count1 = memory1.count()

		    // Clear it
		    memory1.clear()

		    // Create new instance - should be empty
		    memory2 = aiMemory( "cache", "clear-test" )
		        .configure( { cacheName: "default" } )

		    count2 = memory2.count()
		    isEmpty = memory2.isEmpty()
		    """,
		    context
		);

		var	count1	= variables.getAsInteger( Key.of( "count1" ) );
		var	count2	= variables.getAsInteger( Key.of( "count2" ) );
		var	isEmpty	= variables.getAsBoolean( Key.of( "isEmpty" ) );

		assertThat( count1 ).isEqualTo( 1 );
		assertThat( count2 ).isEqualTo( 0 );
		assertThat( isEmpty ).isTrue();
	}

	@Test
	@DisplayName( "Test CacheMemory system message persistence" )
	public void testSystemMessagePersistence() {
		runtime.executeSource(
		    """
		    // Create memory with system message
		    memory1 = aiMemory( "cache", "system-test" )
		        .configure( { cacheName: "default" } )
		        .setSystemMessage( "You are a helpful assistant" )
		        .add( "Hello" )

		    sysMsg1 = memory1.getSystemMessage()

		    // Create new instance - should load system message
		    memory2 = aiMemory( "cache", "system-test" )
		        .configure( { cacheName: "default" } )

		    sysMsg2 = memory2.getSystemMessage()

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		var	sysMsg1	= variables.getAsString( Key.of( "sysMsg1" ) );
		var	sysMsg2	= variables.getAsString( Key.of( "sysMsg2" ) );

		assertThat( sysMsg1 ).isEqualTo( "You are a helpful assistant" );
		assertThat( sysMsg2 ).isEqualTo( "You are a helpful assistant" );
	}

	@Test
	@DisplayName( "Test CacheMemory getSummary includes cache info" )
	public void testGetSummary() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "summary-test" )
		        .configure( { cacheName: "default" } )
		        .add( "Test" )

		    summary = memory.getSummary()

		    // Cleanup
		    memory.clear()
		    """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "type" ) ) ).isEqualTo( "CacheMemory" );
		assertThat( summary.getAsString( Key.of( "cacheName" ) ) ).isEqualTo( "default" );
		assertThat( summary.getAsBoolean( Key.of( "cached" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test CacheMemory export includes cacheName" )
	public void testExport() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "export-test" )
		        .configure( { cacheName: "default" } )
		        .add( "Test message" )

		    exported = memory.export()

		    // Cleanup
		    memory.clear()
		    """,
		    context
		);

		var exported = variables.getAsStruct( Key.of( "exported" ) );
		assertThat( exported.getAsString( Key.of( "type" ) ) ).isEqualTo( "CacheMemory" );
		assertThat( exported.getAsString( Key.of( "cacheName" ) ) ).isEqualTo( "default" );
	}

	@Test
	@DisplayName( "Test CacheMemory import persists to cache" )
	public void testImport() {
		runtime.executeSource(
		    """
		    data = {
		        key: "import-test",
		        cacheName: "default",
		        messages: [
		            { role: "user", content: "Hello", timestamp: now() },
		            { role: "assistant", content: "Hi there", timestamp: now() }
		        ],
		        metadata: { session: "test" },
		        config: { test: true }
		    }

		    // Import data
		    memory1 = aiMemory( "cache", "temp-key" )
		        .configure( { cacheName: "default" } )
		        .import( data )

		    count1 = memory1.count()

		    // Create new instance - should load from cache
		    memory2 = aiMemory( "cache", "import-test" )
		        .configure( { cacheName: "default" } )

		    count2 = memory2.count()

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		var	count1	= variables.getAsInteger( Key.of( "count1" ) );
		var	count2	= variables.getAsInteger( Key.of( "count2" ) );

		assertThat( count1 ).isEqualTo( 2 );
		assertThat( count2 ).isEqualTo( 2 ); // Should load from cache
	}

	@Test
	@DisplayName( "Test CacheMemory with metadata persistence" )
	public void testMetadataPersistence() {
		runtime.executeSource(
		    """
		    // Create memory with metadata
		    memory1 = aiMemory( "cache", "metadata-test" )
		        .configure( { cacheName: "default" } )
		        .metadata( { userId: "123", sessionId: "abc" } )
		        .add( "Test" )

		    meta1 = memory1.metadata()

		    // Create new instance - should load metadata
		    memory2 = aiMemory( "cache", "metadata-test" )
		        .configure( { cacheName: "default" } )

		    meta2 = memory2.metadata()

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		var	meta1	= variables.getAsStruct( Key.of( "meta1" ) );
		var	meta2	= variables.getAsStruct( Key.of( "meta2" ) );

		assertThat( meta1.getAsString( Key.of( "userId" ) ) ).isEqualTo( "123" );
		assertThat( meta2.getAsString( Key.of( "userId" ) ) ).isEqualTo( "123" );
	}

	@Test
	@DisplayName( "Test CacheMemory search() persists across instances" )
	public void testSearchPersistence() {
		runtime.executeSource(
		    """
		    // Create memory and add messages
		    memory1 = aiMemory( "cache", "search-test" )
		        .configure( { cacheName: "default" } )
		        .add( "Hello world" )
		        .add( "Goodbye world" )
		        .add( "Testing BoxLang" )

		    // Create new instance and search
		    memory2 = aiMemory( "cache", "search-test" )
		        .configure( { cacheName: "default" } )

		    results = memory2.search( "world" )
		    boxlangResults = memory2.search( "BoxLang" )

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		var	results			= variables.getAsArray( Key.of( "results" ) );
		var	boxlangResults	= variables.getAsArray( Key.of( "boxlangResults" ) );

		assertThat( results.size() ).isEqualTo( 2 );
		assertThat( boxlangResults.size() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test CacheMemory search() case-sensitive" )
	public void testSearchCaseSensitive() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "case-test" )
		        .configure( { cacheName: "default" } )
		        .add( "Hello World" )
		        .add( "hello world" )
		        .add( "HELLO WORLD" )

		    caseSensitive = memory.search( "World", true )
		    caseInsensitive = memory.search( "world", false )

		    // Cleanup
		    memory.clear()
		    """,
		    context
		);

		var	caseSensitive	= variables.getAsArray( Key.of( "caseSensitive" ) );
		var	caseInsensitive	= variables.getAsArray( Key.of( "caseInsensitive" ) );

		assertThat( caseSensitive.size() ).isEqualTo( 1 );
		assertThat( caseInsensitive.size() ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test CacheMemory getRange() from cache" )
	public void testGetRange() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "range-test" )
		        .configure( { cacheName: "default" } )

		    // Add 10 messages
		    for( i = 1; i <= 10; i++ ) {
		        memory.add( "Message " & i )
		    }

		    // Create new instance and get range
		    memory2 = aiMemory( "cache", "range-test" )
		        .configure( { cacheName: "default" } )

		    range = memory2.getRange( 3, 7 )
		    rangeToEnd = memory2.getRange( 8 )

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		var	range		= variables.getAsArray( Key.of( "range" ) );
		var	rangeToEnd	= variables.getAsArray( Key.of( "rangeToEnd" ) );

		assertThat( range.size() ).isEqualTo( 5 ); // Messages 3-7
		assertThat( rangeToEnd.size() ).isEqualTo( 3 ); // Messages 8-10
	}

	@Test
	@DisplayName( "Test CacheMemory getRange() invalid indices" )
	public void testGetRangeInvalid() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "range-invalid-test" )
		        .configure( { cacheName: "default" } )

		    // Add 5 messages
		    for( i = 1; i <= 5; i++ ) {
		        memory.add( "Message " & i )
		    }

		    outOfBounds = memory.getRange( 10, 20 )
		    invalid = memory.getRange( 0, 3 )

		    // Cleanup
		    memory.clear()
		    """,
		    context
		);

		var	outOfBounds	= variables.getAsArray( Key.of( "outOfBounds" ) );
		var	invalid		= variables.getAsArray( Key.of( "invalid" ) );

		assertThat( outOfBounds.size() ).isEqualTo( 0 );
		assertThat( invalid.size() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test CacheMemory clone() creates independent instance" )
	public void testClone() {
		runtime.executeSource(
		    """
		    original = aiMemory( "cache", "clone-test" )
		        .configure( { cacheName: "default" } )
		        .metadata( { userId: "123" } )
		        .add( "Message 1" )
		        .add( "Message 2" )

		    cloned = original.clone()

		    // Modify clone
		    cloned.add( "Message 3" )

		    originalCount = original.count()
		    clonedCount = cloned.count()

		    // Cleanup
		    original.clear()
		    cloned.clear()
		    """,
		    context
		);

		var	originalCount	= variables.getAsInteger( Key.of( "originalCount" ) );
		var	clonedCount		= variables.getAsInteger( Key.of( "clonedCount" ) );

		assertThat( originalCount ).isEqualTo( 2 );
		assertThat( clonedCount ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test CacheMemory merge() persists to cache" )
	public void testMerge() {
		runtime.executeSource(
		    """
		    memory1 = aiMemory( "cache", "merge-test-1" )
		        .configure( { cacheName: "default" } )
		        .add( "Message 1" )
		        .add( "Message 2" )

		    memory2 = aiMemory( "cache", "merge-test-2" )
		        .configure( { cacheName: "default" } )
		        .add( "Message 3" )
		        .add( "Message 4" )

		    memory1.merge( memory2 )

		    // Create new instance to verify persistence
		    memory3 = aiMemory( "cache", "merge-test-1" )
		        .configure( { cacheName: "default" } )

		    count = memory3.count()

		    // Cleanup
		    memory1.clear()
		    memory2.clear()
		    """,
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );

		assertThat( count ).isEqualTo( 4 );
	}

	@Test
	@DisplayName( "Test CacheMemory merge() with skipDuplicates" )
	public void testMergeSkipDuplicates() {
		runtime.executeSource(
		    """
		    memory1 = aiMemory( "cache", "merge-dup-test-1" )
		        .configure( { cacheName: "default" } )
		        .add( "Message 1" )
		        .add( "Message 2" )

		    memory2 = aiMemory( "cache", "merge-dup-test-2" )
		        .configure( { cacheName: "default" } )
		        .add( "Message 2" )
		        .add( "Message 3" )

		    memory1.merge( memory2, true )

		    count = memory1.count()

		    // Cleanup
		    memory1.clear()
		    memory2.clear()
		    """,
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );

		// Should have 3 messages (1, 2, 3) - duplicate "Message 2" skipped
		assertThat( count ).isEqualTo( 3 );
	}

	@DisplayName( "Test that it can trim when you set a limit" )
	@Test
	public void testTrimWhenLimitSet() {
		runtime.executeSource(
		    """
		    // Create memory with maxMessages limit
		    memory = aiMemory( "cache", "trim-test" )
		    	.configure( {
		    		cacheName: "default",
		    		maxMessages: 5
		    	} )

		    // Add 10 messages
		    for( i = 1; i <= 10; i++ ) {
		    	memory.add( "Message " & i )
		    }

		    count = memory.count()
		    messages = memory.getAll()
		    firstMessage = messages[ 1 ].content
		    lastMessage = messages[ count ].content

		    // Verify oldest messages were removed
		    hasMessage1 = memory.search( "Message 1" ).len() > 0
		    hasMessage6 = memory.search( "Message 6" ).len() > 0
		    hasMessage10 = memory.search( "Message 10" ).len() > 0

		    // Create new instance to verify persistence
		    memory2 = aiMemory( "cache", "trim-test" )
		    	.configure( { cacheName: "default" } )

		    persistedCount = memory2.count()

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		var	count			= variables.getAsInteger( Key.of( "count" ) );
		var	persistedCount	= variables.getAsInteger( Key.of( "persistedCount" ) );

		assertThat( count ).isEqualTo( 5 );
		assertThat( persistedCount ).isEqualTo( 5 ); // Persisted trimmed state
	}

	// ==================== Checkpoint Tests ====================

	@Test
	@DisplayName( "Test CacheMemory saveState persists state by threadId" )
	public void testSaveState() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "checkpoint-save-test" )
		        .configure( { cacheName: "default" } )

		    state = { step: "tool-call", input: "Hello World", toolResults: [ "result1" ] }
		    memory.saveState( "thread-001", state )

		    loaded = memory.loadState( "thread-001" )

		    // Cleanup
		    memory.clearState( "thread-001" )
		    memory.clear()
		    """,
		    context
		);

		var loaded = variables.getAsStruct( Key.of( "loaded" ) );
		assertThat( loaded.getAsString( Key.of( "step" ) ) ).isEqualTo( "tool-call" );
		assertThat( loaded.getAsString( Key.of( "input" ) ) ).isEqualTo( "Hello World" );
		assertThat( loaded.getAsArray( Key.of( "toolResults" ) ).size() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test CacheMemory loadState returns empty struct for unknown threadId" )
	public void testLoadStateUnknownThread() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "checkpoint-unknown-test" )
		        .configure( { cacheName: "default" } )

		    loaded = memory.loadState( "nonexistent-thread" )
		    isEmpty = loaded.isEmpty()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test CacheMemory clearState removes stored state" )
	public void testClearState() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "checkpoint-clear-test" )
		        .configure( { cacheName: "default" } )

		    state = { pending: "data", messages: [ "msg1", "msg2" ] }
		    memory.saveState( "thread-002", state )

		    // Verify it was saved
		    beforeClear = memory.loadState( "thread-002" )
		    beforeEmpty = beforeClear.isEmpty()

		    // Clear and verify
		    memory.clearState( "thread-002" )
		    afterClear = memory.loadState( "thread-002" )
		    afterEmpty = afterClear.isEmpty()

		    // Cleanup
		    memory.clear()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "beforeEmpty" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "afterEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test CacheMemory saveState isolates multiple threads" )
	public void testSaveStateThreadIsolation() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "checkpoint-isolation-test" )
		        .configure( { cacheName: "default" } )

		    memory.saveState( "thread-A", { agent: "alice", step: 1 } )
		    memory.saveState( "thread-B", { agent: "bob",   step: 9 } )

		    stateA = memory.loadState( "thread-A" )
		    stateB = memory.loadState( "thread-B" )

		    // Cleanup
		    memory.clearState( "thread-A" )
		    memory.clearState( "thread-B" )
		    memory.clear()
		    """,
		    context
		);

		var	stateA	= variables.getAsStruct( Key.of( "stateA" ) );
		var	stateB	= variables.getAsStruct( Key.of( "stateB" ) );
		assertThat( stateA.getAsString( Key.of( "agent" ) ) ).isEqualTo( "alice" );
		assertThat( stateA.get( "step" ) ).isEqualTo( 1 );
		assertThat( stateB.getAsString( Key.of( "agent" ) ) ).isEqualTo( "bob" );
		assertThat( stateB.get( "step" ) ).isEqualTo( 9 );
	}

	@Test
	@DisplayName( "Test CacheMemory saveState overwrites existing state for same threadId" )
	public void testSaveStateOverwrite() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "cache", "checkpoint-overwrite-test" )
		        .configure( { cacheName: "default" } )

		    memory.saveState( "thread-003", { version: 1, status: "pending" } )
		    memory.saveState( "thread-003", { version: 2, status: "resumed" } )

		    loaded = memory.loadState( "thread-003" )

		    // Cleanup
		    memory.clearState( "thread-003" )
		    memory.clear()
		    """,
		    context
		);

		var loaded = variables.getAsStruct( Key.of( "loaded" ) );
		assertThat( loaded.get( "version" ) ).isEqualTo( 2 );
		assertThat( loaded.getAsString( Key.of( "status" ) ) ).isEqualTo( "resumed" );
	}

}
