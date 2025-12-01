package ortus.boxlang.ai.memory;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Test cases for JdbcMemory - JDBC-based persistent memory storage
 */
public class JdbcMemoryTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test JdbcMemory instantiation with datasource configuration" )
	public void testInstantiationWithDatasource() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "jdbc", "test-key", { datasource: "bxai_test" } )
		    datasource = memory.datasource()
		    table = memory.table()
		    """,
		    context
		);

		var	datasource	= variables.getAsString( Key.of( "datasource" ) );
		var	table		= variables.getAsString( Key.of( "table" ) );

		assertThat( datasource ).isEqualTo( "bxai_test" );
		assertThat( table ).isEqualTo( "bx_ai_memories" );
	}

	@Test
	@DisplayName( "Test JdbcMemory with custom table name" )
	public void testCustomTableName() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "jdbc", "custom-table-key", {
		        datasource: "bxai_test",
		        table: "custom_ai_memories"
		    } )
		    table = memory.table()

		    // Cleanup
		    memory.clear()
		    """,
		    context
		);

		var table = variables.getAsString( Key.of( "table" ) );
		assertThat( table ).isEqualTo( "custom_ai_memories" );
	}

	@Test
	@DisplayName( "Test JdbcMemory persists messages to database" )
	public void testPersistMessages() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "jdbc", "persist-test", { datasource: "bxai_test" } )
		        .add( "Message 1" )
		        .add( "Message 2" )

		    count1 = memory.count()

		    // Create new instance with same key - should load from database
		    memory2 = aiMemory( "jdbc", "persist-test", { datasource: "bxai_test" } )
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
		assertThat( count2 ).isEqualTo( 2 ); // Should load from database
	}

	@Test
	@DisplayName( "Test JdbcMemory clear removes from database" )
	public void testClearRemovesFromDatabase() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "jdbc", "clear-test", { datasource: "bxai_test" } )
		        .add( "Test message" )

		    count1 = memory.count()

		    // Clear it
		    memory.clear()

		    // Create new instance - should be empty
		    memory2 = aiMemory( "jdbc", "clear-test", { datasource: "bxai_test" } )
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
	@DisplayName( "Test JdbcMemory system message persistence" )
	public void testSystemMessagePersistence() {
		runtime.executeSource(
		    """
		    // Create memory with system message
		    memory1 = aiMemory( "jdbc", "system-test", { datasource: "bxai_test" } )
		        .setSystemMessage( "You are a helpful assistant" )
		        .add( "Hello" )

		    sysMsg1 = memory1.getSystemMessage()

		    // Create new instance - should load system message
		    memory2 = aiMemory( "jdbc", "system-test", { datasource: "bxai_test" } )
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
	@DisplayName( "Test JdbcMemory getSummary includes database info" )
	public void testGetSummary() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "jdbc", "summary-test", { datasource: "bxai_test" } )
		        .add( "Test" )

		    summary = memory.getSummary()

		    // Cleanup
		    memory.clear()
		    """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "type" ) ) ).isEqualTo( "JdbcMemory" );
		assertThat( summary.getAsString( Key.of( "datasource" ) ) ).isEqualTo( "bxai_test" );
		assertThat( summary.getAsString( Key.of( "table" ) ) ).isEqualTo( "bx_ai_memories" );
		assertThat( summary.getAsBoolean( Key.of( "persisted" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcMemory export includes datasource info" )
	public void testExport() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "jdbc", "export-test", { datasource: "bxai_test" } )
		        .add( "Test message" )

		    exported = memory.export()

		    // Cleanup
		    memory.clear()
		    """,
		    context
		);

		var exported = variables.getAsStruct( Key.of( "exported" ) );
		assertThat( exported.getAsString( Key.of( "type" ) ) ).isEqualTo( "JdbcMemory" );
		assertThat( exported.getAsString( Key.of( "datasource" ) ) ).isEqualTo( "bxai_test" );
		assertThat( exported.getAsString( Key.of( "table" ) ) ).isEqualTo( "bx_ai_memories" );
	}

	@Test
	@DisplayName( "Test JdbcMemory import persists to database" )
	public void testImport() {
		runtime.executeSource(
		    """
		    data = {
		        key: "import-test",
		        datasource: "bxai_test",
		        table: "bx_ai_memories",
		        messages: [
		            { role: "user", content: "Hello", timestamp: now() },
		            { role: "assistant", content: "Hi there", timestamp: now() }
		        ],
		        metadata: { session: "test" },
		        config: { test: true }
		    }

		    // Import data
		    memory1 = aiMemory( "jdbc", "temp-key", { datasource: "bxai_test" } )
		        .import( data )

		    count1 = memory1.count()

		    // Create new instance - should load from database
		    memory2 = aiMemory( "jdbc", "import-test", { datasource: "bxai_test" } )
		    count2 = memory2.count()

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		var	count1	= variables.getAsInteger( Key.of( "count1" ) );
		var	count2	= variables.getAsInteger( Key.of( "count2" ) );

		assertThat( count1 ).isEqualTo( 2 );
		assertThat( count2 ).isEqualTo( 2 ); // Should load from database
	}

	@Test
	@DisplayName( "Test JdbcMemory with metadata persistence" )
	public void testMetadataPersistence() {
		runtime.executeSource(
		    """
		    // Create memory with metadata
		    memory1 = aiMemory( "jdbc", "metadata-test", { datasource: "bxai_test" } )
		        .metadata( { userId: "123", sessionId: "abc" } )
		        .add( "Test" )

		    meta1 = memory1.metadata()

		    // Create new instance - should load metadata
		    memory2 = aiMemory( "jdbc", "metadata-test", { datasource: "bxai_test" } )
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
	@DisplayName( "Test JdbcMemory handles non-existent key gracefully" )
	public void testNonExistentKey() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "jdbc", "non-existent-key", { datasource: "bxai_test" } )
		    count = memory.count()
		    isEmpty = memory.isEmpty()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcMemory with AiMessage integration" )
	public void testAiMessageIntegration() {
		runtime.executeSource(
		    """
		    msg = aiMessage()
		        .system( "Be helpful" )
		        .user( "Hello" )

		    memory = aiMemory( "jdbc", "aimessage-test", { datasource: "bxai_test" } )
		        .add( msg )

		    // Load from database with same key
		    memory2 = aiMemory( "jdbc", "aimessage-test", { datasource: "bxai_test" } )
		    count = memory2.count()

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		assertThat( count ).isEqualTo( 2 ); // system + user message
	}

	@Test
	@DisplayName( "Test JdbcMemory multiple instances with different keys" )
	public void testMultipleInstances() {
		runtime.executeSource(
		    """
		    memory1 = aiMemory( "jdbc", "multi-key-1", { datasource: "bxai_test" } )
		        .add( "Message A" )

		    memory2 = aiMemory( "jdbc", "multi-key-2", { datasource: "bxai_test" } )
		        .add( "Message B" )
		        .add( "Message C" )

		    count1 = memory1.count()
		    count2 = memory2.count()

		    // Verify isolation
		    msg1 = memory1.getAll()[1].content
		    msg2 = memory2.getAll()[1].content

		    // Cleanup
		    memory1.clear()
		    memory2.clear()
		    """,
		    context
		);

		var	count1	= variables.getAsInteger( Key.of( "count1" ) );
		var	count2	= variables.getAsInteger( Key.of( "count2" ) );
		var	msg1	= variables.getAsString( Key.of( "msg1" ) );
		var	msg2	= variables.getAsString( Key.of( "msg2" ) );

		assertThat( count1 ).isEqualTo( 1 );
		assertThat( count2 ).isEqualTo( 2 );
		assertThat( msg1 ).isEqualTo( "Message A" );
		assertThat( msg2 ).isEqualTo( "Message B" );
	}

	@Test
	@DisplayName( "Test JdbcMemory update existing record" )
	public void testUpdateExistingRecord() {
		runtime.executeSource(
		    """
		    // Create initial memory
		    memory1 = aiMemory( "jdbc", "update-test", { datasource: "bxai_test" } )
		        .add( "Message 1" )

		    count1 = memory1.count()

		    // Load and add more messages
		    memory2 = aiMemory( "jdbc", "update-test", { datasource: "bxai_test" } )
		        .add( "Message 2" )
		        .add( "Message 3" )

		    count2 = memory2.count()

		    // Load again to verify updates persisted
		    memory3 = aiMemory( "jdbc", "update-test", { datasource: "bxai_test" } )
		    count3 = memory3.count()

		    // Cleanup
		    memory3.clear()
		    """,
		    context
		);

		var	count1	= variables.getAsInteger( Key.of( "count1" ) );
		var	count2	= variables.getAsInteger( Key.of( "count2" ) );
		var	count3	= variables.getAsInteger( Key.of( "count3" ) );

		assertThat( count1 ).isEqualTo( 1 );
		assertThat( count2 ).isEqualTo( 3 ); // 1 original + 2 new
		assertThat( count3 ).isEqualTo( 3 ); // Should persist
	}

	@DisplayName( "Test that it can trim when you set a limit" )
	@Test
	public void testTrimWhenLimitSet() {
		runtime.executeSource(
		    """
		    // Create memory with maxMessages limit
		    memory = aiMemory( "jdbc", "trim-test", { datasource: "bxai_test" } )
		    	.configure( {
		    		maxMessages: 5
		    	} )

		    // Add 10 messages
		    for( i = 1; i <= 10; i++ ) {
		    	memory.add( "Message " & i )
		    }

		    count = memory.count()
		    messages = memory.getAll()

		    // Create new instance to verify persistence
		    memory2 = aiMemory( "jdbc", "trim-test", { datasource: "bxai_test" } )
		    	.configure( {
		    		maxMessages: 5
		    	} )

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
}
