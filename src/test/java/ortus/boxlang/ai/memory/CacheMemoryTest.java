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

}
