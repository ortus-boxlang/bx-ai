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
		    memory = aiMemory( "cache" )
		        .key( "test-session" )
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
		    memory1 = aiMemory( "cache" )
		        .key( "persist-test" )
		        .configure( { cacheName: "default" } )
		        .add( "First message" )
		        .add( "Second message" )

		    count1 = memory1.count()

		    // Create new instance with same key - should load from cache
		    memory2 = aiMemory( "cache" )
		        .key( "persist-test" )
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
		    memory1 = aiMemory( "cache" )
		        .key( "clear-test" )
		        .configure( { cacheName: "default" } )
		        .add( "Test message" )

		    count1 = memory1.count()

		    // Clear it
		    memory1.clear()

		    // Create new instance - should be empty
		    memory2 = aiMemory( "cache" )
		        .key( "clear-test" )
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
		    memory1 = aiMemory( "cache" )
		        .key( "system-test" )
		        .configure( { cacheName: "default" } )
		        .setSystemMessage( "You are a helpful assistant" )
		        .add( "Hello" )

		    sysMsg1 = memory1.getSystemMessage()

		    // Create new instance - should load system message
		    memory2 = aiMemory( "cache" )
		        .key( "system-test" )
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
		    memory = aiMemory( "cache" )
		        .key( "summary-test" )
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
		    memory = aiMemory( "cache" )
		        .key( "export-test" )
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
		    memory1 = aiMemory( "cache" )
		        .configure( { cacheName: "default" } )
		        .import( data )

		    count1 = memory1.count()

		    // Create new instance - should load from cache
		    memory2 = aiMemory( "cache" )
		        .key( "import-test" )
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
		    memory1 = aiMemory( "cache" )
		        .key( "metadata-test" )
		        .configure( { cacheName: "default" } )
		        .metadata( { userId: "123", sessionId: "abc" } )
		        .add( "Test" )

		    meta1 = memory1.metadata()

		    // Create new instance - should load metadata
		    memory2 = aiMemory( "cache" )
		        .key( "metadata-test" )
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

}
