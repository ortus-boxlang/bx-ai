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
		    memory = aiMemory( memory: "jdbc", key: "test-key", config: { datasource: "bxai_test" } )
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
		    memory = aiMemory( memory: "jdbc", key: "custom-table-key", config: {
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
		    memory = aiMemory( memory: "jdbc", key: "persist-test", config: { datasource: "bxai_test" } )
		        .add( "Message 1" )
		        .add( "Message 2" )

		    count1 = memory.count()

		    // Create new instance with same key - should load from database
		    memory2 = aiMemory( memory: "jdbc", key: "persist-test", config: { datasource: "bxai_test" } )
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
		    memory = aiMemory( memory: "jdbc", key: "clear-test", config: { datasource: "bxai_test" } )
		        .add( "Test message" )

		    count1 = memory.count()

		    // Clear it
		    memory.clear()

		    // Create new instance - should be empty
		    memory2 = aiMemory( memory: "jdbc", key: "clear-test", config: { datasource: "bxai_test" } )
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
		    memory1 = aiMemory( memory: "jdbc", key: "system-test", config: { datasource: "bxai_test" } )
		        .setSystemMessage( "You are a helpful assistant" )
		        .add( "Hello" )

		    sysMsg1 = memory1.getSystemMessage()

		    // Create new instance - should load system message
		    memory2 = aiMemory( memory: "jdbc", key: "system-test", config: { datasource: "bxai_test" } )
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
		    memory = aiMemory( memory: "jdbc", key: "summary-test", config: { datasource: "bxai_test" } )
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
		    memory = aiMemory( memory: "jdbc", key: "export-test", config: { datasource: "bxai_test" } )
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
		    memory1 = aiMemory( memory: "jdbc", key: "temp-key", config: { datasource: "bxai_test" } )
		        .import( data )

		    count1 = memory1.count()

		    // Create new instance - should load from database
		    memory2 = aiMemory( memory: "jdbc", key: "import-test", config: { datasource: "bxai_test" } )
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
		    memory1 = aiMemory( memory: "jdbc", key: "metadata-test", config: { datasource: "bxai_test" } )
		        .metadata( { userId: "123", sessionId: "abc" } )
		        .add( "Test" )

		    meta1 = memory1.metadata()

		    // Create new instance - should load metadata
		    memory2 = aiMemory( memory: "jdbc", key: "metadata-test", config: { datasource: "bxai_test" } )
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
		    memory = aiMemory( memory: "jdbc", key: "non-existent-key", config: { datasource: "bxai_test" } )
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

		    memory = aiMemory( memory: "jdbc", key: "aimessage-test", config: { datasource: "bxai_test" } )
		        .add( msg )

		    // Load from database with same key
		    memory2 = aiMemory( memory: "jdbc", key: "aimessage-test", config: { datasource: "bxai_test" } )
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
		    memory1 = aiMemory( memory: "jdbc", key: "multi-key-1", config: { datasource: "bxai_test" } )
		        .add( "Message A" )

		    memory2 = aiMemory( memory: "jdbc", key: "multi-key-2", config: { datasource: "bxai_test" } )
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
		    memory1 = aiMemory( memory: "jdbc", key: "update-test", config: { datasource: "bxai_test" } )
		        .add( "Message 1" )

		    count1 = memory1.count()

		    // Load and add more messages
		    memory2 = aiMemory( memory: "jdbc", key: "update-test", config: { datasource: "bxai_test" } )
		        .add( "Message 2" )
		        .add( "Message 3" )

		    count2 = memory2.count()

		    // Load again to verify updates persisted
		    memory3 = aiMemory( memory: "jdbc", key: "update-test", config: { datasource: "bxai_test" } )
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
		    memory = aiMemory( memory: "jdbc", key: "trim-test", config: { datasource: "bxai_test" } )
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
		    memory2 = aiMemory( memory: "jdbc", key: "trim-test", config: { datasource: "bxai_test" } )
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

	@Test
	@DisplayName( "Test JdbcMemory persists userId and conversationId to database" )
	public void testPersistUserIdAndConversationId() {
		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "jdbc",
		        key: "user-conv-test",
		        userId: "user456",
		        conversationId: "support-789",
		        config: { datasource: "bxai_test" }
		    )
		    memory.add( "Support inquiry" )

		    // Create new instance with same key - should load identifiers from database
		    memory2 = aiMemory( memory: "jdbc", key: "user-conv-test", config: { datasource: "bxai_test" } )

		    userId = memory2.getUserId()
		    conversationId = memory2.getConversationId()
		    count = memory2.count()

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user456" );
		assertThat( variables.getAsString( Key.of( "conversationId" ) ) ).isEqualTo( "support-789" );
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test JdbcMemory allows empty userId and conversationId" )
	public void testEmptyIdentifiers() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "jdbc", key: "empty-ids-test", config: { datasource: "bxai_test" } )
		    memory.add( "Message without identifiers" )

		    userId = memory.getUserId()
		    conversationId = memory.getConversationId()

		    // Create new instance - should handle empty identifiers
		    memory2 = aiMemory( memory: "jdbc", key: "empty-ids-test", config: { datasource: "bxai_test" } )
		    count = memory2.count()

		    // Cleanup
		    memory2.clear()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "userId" ) ) ).isEmpty();
		assertThat( variables.getAsString( Key.of( "conversationId" ) ) ).isEmpty();
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test JdbcMemory getSummary includes userId and conversationId" )
	public void testJdbcSummaryIncludesIdentifiers() {
		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "jdbc",
		        userId: "charlie",
		        conversationId: "order-555",
		        config: { datasource: "bxai_test" }
		    )
		    memory.add( "Order placed" )

		    summary = memory.getSummary()
		    summaryUserId = summary.userId
		    summaryConvId = summary.conversationId

		    // Cleanup
		    memory.clear()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "summaryUserId" ) ) ).isEqualTo( "charlie" );
		assertThat( variables.getAsString( Key.of( "summaryConvId" ) ) ).isEqualTo( "order-555" );
	}

	// ==================== Checkpoint Tests ====================

	@Test
	@DisplayName( "Test JdbcMemory saveState persists checkpoint to database" )
	public void testSaveState() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "jdbc", key: "cp-save-test", config: { datasource: "bxai_test" } )

		    state = { step: "awaiting-hitl", input: "drop table users", toolName: "runQuery" }
		    memory.saveState( "jdbc-thread-001", state )

		    loaded = memory.loadState( "jdbc-thread-001" )

		    // Cleanup
		    memory.clearState( "jdbc-thread-001" )
		    memory.clear()
		    """,
		    context
		);

		var loaded = variables.getAsStruct( Key.of( "loaded" ) );
		assertThat( loaded.getAsString( Key.of( "step" ) ) ).isEqualTo( "awaiting-hitl" );
		assertThat( loaded.getAsString( Key.of( "input" ) ) ).isEqualTo( "drop table users" );
		assertThat( loaded.getAsString( Key.of( "toolName" ) ) ).isEqualTo( "runQuery" );
	}

	@Test
	@DisplayName( "Test JdbcMemory loadState returns empty struct for unknown threadId" )
	public void testLoadStateUnknownThread() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "jdbc", key: "cp-unknown-test", config: { datasource: "bxai_test" } )

		    loaded  = memory.loadState( "nonexistent-jdbc-thread" )
		    isEmpty = loaded.isEmpty()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcMemory clearState removes checkpoint row" )
	public void testClearState() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "jdbc", key: "cp-clear-test", config: { datasource: "bxai_test" } )

		    memory.saveState( "jdbc-thread-002", { status: "suspended" } )

		    beforeClear = !memory.loadState( "jdbc-thread-002" ).isEmpty()

		    memory.clearState( "jdbc-thread-002" )

		    afterClear = memory.loadState( "jdbc-thread-002" ).isEmpty()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "beforeClear" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "afterClear" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcMemory saveState overwrites existing checkpoint for same threadId" )
	public void testSaveStateOverwrite() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "jdbc", key: "cp-overwrite-test", config: { datasource: "bxai_test" } )

		    memory.saveState( "jdbc-thread-003", { version: 1, status: "pending" } )
		    memory.saveState( "jdbc-thread-003", { version: 2, status: "resumed" } )

		    loaded = memory.loadState( "jdbc-thread-003" )

		    // Cleanup
		    memory.clearState( "jdbc-thread-003" )
		    memory.clear()
		    """,
		    context
		);

		var loaded = variables.getAsStruct( Key.of( "loaded" ) );
		assertThat( loaded.get( "version" ) ).isEqualTo( 2 );
		assertThat( loaded.getAsString( Key.of( "status" ) ) ).isEqualTo( "resumed" );
	}

	@Test
	@DisplayName( "Test JdbcMemory saveState isolates multiple threads" )
	public void testSaveStateThreadIsolation() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "jdbc", key: "cp-isolation-test", config: { datasource: "bxai_test" } )

		    memory.saveState( "jdbc-thread-A", { agent: "alice", task: "summarize" } )
		    memory.saveState( "jdbc-thread-B", { agent: "bob",   task: "analyze" } )

		    stateA = memory.loadState( "jdbc-thread-A" )
		    stateB = memory.loadState( "jdbc-thread-B" )

		    // Cleanup
		    memory.clearState( "jdbc-thread-A" )
		    memory.clearState( "jdbc-thread-B" )
		    memory.clear()
		    """,
		    context
		);

		var	stateA	= variables.getAsStruct( Key.of( "stateA" ) );
		var	stateB	= variables.getAsStruct( Key.of( "stateB" ) );
		assertThat( stateA.getAsString( Key.of( "agent" ) ) ).isEqualTo( "alice" );
		assertThat( stateA.getAsString( Key.of( "task" ) ) ).isEqualTo( "summarize" );
		assertThat( stateB.getAsString( Key.of( "agent" ) ) ).isEqualTo( "bob" );
		assertThat( stateB.getAsString( Key.of( "task" ) ) ).isEqualTo( "analyze" );
	}

	/**
	 * One memory object, three scopes: two conversations of one user and a second user. Before the fix all three read
	 * the same row, so c2 and the other user saw c1's messages. Also checks the rows themselves: the key carries the
	 * scope (memory_key is the primary key, so it has to) and user_id and conversation_id are filled in.
	 */
	@Test
	@DisplayName( "Test JdbcMemory keeps each user and conversation in a row of its own when ids are passed per call" )
	public void testPerCallScopesAreSeparate() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "jdbc", key: "scoped-jdbc", config: { datasource: "bxai_test" } )
		    memory.clear( "u1", "c1" )
		    memory.clear( "u1", "c2" )
		    memory.add( { role: "user", content: "alpha" }, "u1", "c1" )
		    memory.add( { role: "user", content: "alpha2" }, "u1", "c1" )
		    memory.add( { role: "user", content: "beta" }, "u1", "c2" )

		    c1 = memory.getAll( "u1", "c1" ).len()
		    c2 = memory.getAll( "u1", "c2" ).len()
		    otherUser = memory.getAll( "u2", "c1" ).len()
		    unscoped = memory.getAll().len()

		    rows = queryExecute(
		        "SELECT user_id, conversation_id FROM bx_ai_memories WHERE memory_key LIKE 'scoped-jdbc::%' ORDER BY conversation_id",
		        {},
		        { datasource: "bxai_test" }
		    )
		    rowCount = rows.recordCount
		    firstRowConversation = rows.conversation_id[ 1 ]
		    firstRowUser = rows.user_id[ 1 ]

		    memory.clear( "u1", "c1" )
		    memory.clear( "u1", "c2" )
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "c1" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "c2" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "otherUser" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "unscoped" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "rowCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsString( Key.of( "firstRowConversation" ) ) ).isEqualTo( "c1" );
		assertThat( variables.getAsString( Key.of( "firstRowUser" ) ) ).isEqualTo( "u1" );
	}

	/**
	 * The messages of another scope live in the database, not on the object: a new instance with the same key reads them
	 * back, and clear() for one conversation deletes that row and leaves the other conversation's row.
	 */
	@Test
	@DisplayName( "Test JdbcMemory reads a scope back from the database in a new instance, and clear only removes that scope" )
	public void testPerCallScopesSurviveANewInstance() {
		runtime.executeSource(
		    """
		    first = aiMemory( memory: "jdbc", key: "scoped-reload", config: { datasource: "bxai_test" } )
		    first.clear( "u1", "c1" )
		    first.clear( "u1", "c2" )
		    first.add( { role: "user", content: "alpha" }, "u1", "c1" )
		    first.add( { role: "user", content: "beta" }, "u1", "c2" )

		    second = aiMemory( memory: "jdbc", key: "scoped-reload", config: { datasource: "bxai_test" } )
		    readC1 = second.getAll( "u1", "c1" ).len()
		    readC2 = second.getAll( "u1", "c2" ).len()

		    second.clear( "u1", "c1" )
		    afterClearC1 = second.getAll( "u1", "c1" ).len()
		    afterClearC2 = second.getAll( "u1", "c2" ).len()

		    third = aiMemory( memory: "jdbc", key: "scoped-reload", config: { datasource: "bxai_test" } )
		    thirdC1 = third.getAll( "u1", "c1" ).len()
		    thirdC2 = third.getAll( "u1", "c2" ).len()
		    third.clear( "u1", "c2" )
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "readC1" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "readC2" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "afterClearC1" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "afterClearC2" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "thirdC1" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "thirdC2" ) ) ).isEqualTo( 1 );
	}

	/**
	 * summarize() for a scope other than the memory's own used to throw JdbcMemory.UnsupportedScope. It now works on the
	 * row for that scope. The row for c2 is empty here, so there is nothing to summarize; what is checked is that the
	 * call does not throw and that this memory's own message is still there afterwards.
	 */
	@Test
	@DisplayName( "Test JdbcMemory summarize for another scope no longer throws and leaves the memory's own messages alone" )
	public void testSummarizeOtherScope() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "jdbc", key: "scoped-summarize", userId: "u1", conversationId: "c1", config: { datasource: "bxai_test" } )
		    memory.clear()
		    memory.add( { role: "user", content: "mine" } )
		    memory.summarize( {}, "u1", "c2" )
		    mine = memory.getAll().len()
		    memory.clear()
		    memory.clear( "u1", "c2" )
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "mine" ) ) ).isEqualTo( 1 );
	}

	/**
	 * Nothing about another scope is cached in the process. The reader is created before the writer writes and still
	 * sees the new message, as a second process on the same database would.
	 */
	@Test
	@DisplayName( "Test JdbcMemory reads a scope afresh on every call, so a second instance sees what the first wrote" )
	public void testPerCallScopeIsReadAfreshEachCall() {
		runtime.executeSource(
		    """
		    reader = aiMemory( memory: "jdbc", key: "scoped-fresh", config: { datasource: "bxai_test" } )
		    writer = aiMemory( memory: "jdbc", key: "scoped-fresh", config: { datasource: "bxai_test" } )
		    reader.clear( "u1", "c1" )

		    before = reader.getAll( "u1", "c1" ).len()
		    writer.add( { role: "user", content: "from the other instance" }, "u1", "c1" )
		    after = reader.getAll( "u1", "c1" ).len()

		    reader.clear( "u1", "c1" )
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "before" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "after" ) ) ).isEqualTo( 1 );
	}

	/**
	 * A parent agent that delegates to a sub-agent hands over the user and the conversation it runs for. The sub-agent's
	 * jdbc memory then keeps one row per person and conversation, instead of one row for everyone who ever delegated.
	 */
	@Test
	@DisplayName( "Test a delegated sub-agent keeps memory per user and conversation of its parent" )
	public void testSubAgentMemoryFollowsParentUserAndConversation() {
		runtime.executeSource(
		    """
		    probe = aiMemory( memory: "jdbc", key: "sub-scope", config: { datasource: "bxai_test" } )
		    probe.clear( "alice", "c1" )
		    probe.clear( "bob", "c2" )
		    probe.clear( "", "" )

		    worker = aiAgent(
		        name: "worker",
		        description: "Does the work",
		        model: aiModel( provider: "mock", options: { providerOptions: { responses: [ "worked" ] } } ),
		        memory: aiMemory( memory: "jdbc", key: "sub-scope", config: { datasource: "bxai_test" } )
		    )
		    call = { toolCalls: [ { name: "delegate_to_worker", arguments: { task: "do it" } } ] }
		    boss = aiAgent(
		        name: "boss",
		        model: aiModel( provider: "mock", options: { providerOptions: { responses: [ call, "done", call, "done" ] } } ),
		        subAgents: [ worker ]
		    )

		    boss.run( "go", {}, { userId: "alice", conversationId: "c1" } )
		    boss.run( "go", {}, { userId: "bob", conversationId: "c2" } )

		    aliceCount  = probe.getAll( "alice", "c1" ).len()
		    bobCount    = probe.getAll( "bob", "c2" ).len()
		    sharedCount = probe.getAll( "", "" ).len()

		    probe.clear( "alice", "c1" )
		    probe.clear( "bob", "c2" )
		    probe.clear( "", "" )
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "aliceCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "bobCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "sharedCount" ) ) ).isEqualTo( 0 );
	}

	/**
	 * Without a user or a conversation on the parent, delegation behaves as it always did: the sub-agent uses its own
	 * options, here none, so everything lands in the one unscoped row.
	 */
	@Test
	@DisplayName( "Test a delegated sub-agent without ids on the parent still works unscoped" )
	public void testSubAgentWithoutIdsStaysUnscoped() {
		runtime.executeSource(
		    """
		    probe = aiMemory( memory: "jdbc", key: "sub-unscoped", config: { datasource: "bxai_test" } )
		    probe.clear( "", "" )

		    worker = aiAgent(
		        name: "worker",
		        description: "Does the work",
		        model: aiModel( provider: "mock", options: { providerOptions: { responses: [ "worked" ] } } ),
		        memory: aiMemory( memory: "jdbc", key: "sub-unscoped", config: { datasource: "bxai_test" } )
		    )
		    call = { toolCalls: [ { name: "delegate_to_worker", arguments: { task: "do it" } } ] }
		    boss = aiAgent(
		        name: "boss",
		        model: aiModel( provider: "mock", options: { providerOptions: { responses: [ call, "done" ] } } ),
		        subAgents: [ worker ]
		    )

		    boss.run( "go" )
		    // Unscoped memory is the instance's own state, so read it through a fresh instance after the run
		    fresh = aiMemory( memory: "jdbc", key: "sub-unscoped", config: { datasource: "bxai_test" } )
		    unscopedCount = fresh.getAll().len()
		    fresh.clear()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "unscopedCount" ) ) ).isEqualTo( 2 );
	}

	/**
	 * Twelve threads add to one conversation that has no row yet, on one memory shared by all of them, as a served agent
	 * is. Every message has to be kept and none of the threads may fail on the row that another one just created.
	 */
	@Test
	@DisplayName( "Test JdbcMemory keeps every message when many threads add to one new conversation" )
	public void testConcurrentAddsToOneNewConversation() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "jdbc", key: "concurrent-new", config: { datasource: "bxai_test" } )
		    memory.clear( "u1", "c1" )

		    launch = ( n ) => runAsync( () => memory.add( { role: "user", content: "message #n#" }, "u1", "c1" ) )
		    futures = []
		    for ( i = 1; i <= 12; i++ ) {
		        futures.append( launch( i ) )
		    }
		    failures = 0
		    futures.each( f => {
		        try { f.get() } catch ( any e ) { failures++ }
		    } )
		    kept = memory.getAll( "u1", "c1" ).len()

		    memory.clear( "u1", "c1" )
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "failures" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "kept" ) ) ).isEqualTo( 12 );
	}

	/**
	 * The same, with a memory of its own in every thread: two servers' worth of instances in one process meet in the row.
	 */
	@Test
	@DisplayName( "Test JdbcMemory keeps every message when separate instances add to one conversation" )
	public void testConcurrentAddsFromSeparateInstances() {
		runtime.executeSource(
		    """
		    reader = aiMemory( memory: "jdbc", key: "concurrent-many", config: { datasource: "bxai_test" } )
		    reader.clear( "u1", "c1" )

		    launch = ( n ) => runAsync( () => {
		        own = aiMemory( memory: "jdbc", key: "concurrent-many", config: { datasource: "bxai_test" } )
		        own.add( { role: "user", content: "message #n#" }, "u1", "c1" )
		    } )
		    futures = []
		    for ( i = 1; i <= 12; i++ ) {
		        futures.append( launch( i ) )
		    }
		    failures = 0
		    futures.each( f => {
		        try { f.get() } catch ( any e ) { failures++ }
		    } )
		    kept = reader.getAll( "u1", "c1" ).len()

		    reader.clear( "u1", "c1" )
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "failures" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "kept" ) ) ).isEqualTo( 12 );
	}
}
