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

public class JdbcMemoryTest extends BaseIntegrationTest {

	private String datasourceName = "testMemoryDB";

	@BeforeEach
	public void setupEach() {
		super.setupEach();
		
		// Clean up any existing test data
		try {
			runtime.executeSource(
				"""
				try {
					queryExecute(
						"DROP TABLE ai_memory",
						{},
						{ datasource: "testMemoryDB" }
					)
				} catch( any e ) {
					// Table might not exist, that's OK
				}
				""",
				context
			);
		} catch ( Exception e ) {
			// Ignore cleanup errors
		}
	}

	@Test
	@DisplayName( "Test JdbcMemory instantiation" )
	public void testInstantiation() {
		runtime.executeSource(
			"""
			memory = new bxModules.bxai.models.memory.JdbcMemory( "testMemoryDB", "ai_memory" )
			""",
			context
		);

		var memory = variables.get( Key.of( "memory" ) );
		assertThat( memory ).isNotNull();
	}

	@Test
	@DisplayName( "Test JdbcMemory with configuration" )
	public void testConfiguration() {
		runtime.executeSource(
			"""
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
			
			ds = memory.datasource()
			tbl = memory.table()
			""",
			context
		);

		var ds = variables.getAsString( Key.of( "ds" ) );
		var tbl = variables.getAsString( Key.of( "tbl" ) );
		
		assertThat( ds ).isEqualTo( "testMemoryDB" );
		assertThat( tbl ).isEqualTo( "ai_memory" );
	}

	@Test
	@DisplayName( "Test JdbcMemory creates table on configure" )
	public void testTableCreation() {
		runtime.executeSource(
			"""
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
			
			// Check that table exists by querying it
			result = queryExecute(
				"SELECT COUNT(*) as cnt FROM ai_memory",
				{},
				{ datasource: "testMemoryDB" }
			)
			
			tableExists = result.recordCount > 0
			""",
			context
		);

		assertThat( variables.getAsBoolean( Key.of( "tableExists" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcMemory persists messages to database" )
	public void testPersistMessages() {
		runtime.executeSource(
			"""
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "test-key" )
				.add( "Hello World" )
			
			// Query database directly to verify
			result = queryExecute(
				"SELECT memory_data FROM ai_memory WHERE memory_key = 'test-key'",
				{},
				{ datasource: "testMemoryDB" }
			)
			
			recordExists = result.recordCount > 0
			""",
			context
		);

		assertThat( variables.getAsBoolean( Key.of( "recordExists" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcMemory loads messages from database" )
	public void testLoadFromDatabase() {
		runtime.executeSource(
			"""
			// Create first memory and add messages
			memory1 = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "persist-key" )
				.add( "Message 1" )
				.add( "Message 2" )
			
			// Create new memory instance that loads from the same database
			memory2 = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "persist-key" )
			
			count = memory2.count()
			messages = memory2.getAll()
			""",
			context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		var messages = variables.getAsArray( Key.of( "messages" ) );

		assertThat( count ).isEqualTo( 2 );
		assertThat( messages.size() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test JdbcMemory clear removes database content" )
	public void testClearRemovesContent() {
		runtime.executeSource(
			"""
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "clear-test" )
				.add( "Message 1" )
				.add( "Message 2" )
				.clear()
			
			count = memory.count()
			
			// Verify database is empty
			result = queryExecute(
				"SELECT COUNT(*) as cnt FROM ai_memory WHERE memory_key = 'clear-test'",
				{},
				{ datasource: "testMemoryDB" }
			)
			
			dbCount = result.cnt
			""",
			context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "dbCount" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test JdbcMemory getSummary()" )
	public void testGetSummary() {
		runtime.executeSource(
			"""
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "test-key" )
				.setSystemMessage( "Test system" )
				.add( "User message" )
			
			summary = memory.getSummary()
			""",
			context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "type" ) ) ).isEqualTo( "JdbcMemory" );
		assertThat( summary.getAsString( Key.of( "key" ) ) ).isEqualTo( "test-key" );
		assertThat( summary.get( "messageCount" ) ).isEqualTo( 2 ); // system + user
		assertThat( summary.getAsBoolean( Key.of( "hasSystemMessage" ) ) ).isTrue();
		assertThat( summary.getAsString( Key.of( "datasource" ) ) ).isEqualTo( "testMemoryDB" );
		assertThat( summary.getAsString( Key.of( "table" ) ) ).isEqualTo( "ai_memory" );
	}

	@Test
	@DisplayName( "Test JdbcMemory export()" )
	public void testExport() {
		runtime.executeSource(
			"""
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
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
		assertThat( exported.getAsString( Key.of( "datasource" ) ) ).isEqualTo( "testMemoryDB" );
		assertThat( exported.getAsString( Key.of( "table" ) ) ).isEqualTo( "ai_memory" );
	}

	@Test
	@DisplayName( "Test JdbcMemory import()" )
	public void testImport() {
		runtime.executeSource(
			"""
			data = {
				key: "imported-key",
				metadata: { imported: true },
				config: { setting: "value" },
				messages: [
					{ role: "user", content: "Imported message" }
				],
				datasource: "testMemoryDB",
				table: "ai_memory"
			}
			
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.import( data )
			
			result = {
				key: memory.key(),
				count: memory.count(),
				metadata: memory.metadata(),
				config: memory.getConfig(),
				datasource: memory.datasource(),
				table: memory.table()
			}
			""",
			context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "key" ) ) ).isEqualTo( "imported-key" );
		assertThat( result.get( "count" ) ).isEqualTo( 1 );
		assertThat( result.getAsString( Key.of( "datasource" ) ) ).isEqualTo( "testMemoryDB" );
		assertThat( result.getAsString( Key.of( "table" ) ) ).isEqualTo( "ai_memory" );

		IStruct metadata = ( IStruct ) result.get( "metadata" );
		assertThat( metadata.getAsBoolean( Key.of( "imported" ) ) ).isTrue();

		IStruct config = ( IStruct ) result.get( "config" );
		assertThat( config.getAsString( Key.of( "setting" ) ) ).isEqualTo( "value" );
	}

	@Test
	@DisplayName( "Test JdbcMemory export/import roundtrip" )
	public void testExportImportRoundtrip() {
		runtime.executeSource(
			"""
			original = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "roundtrip" )
				.metadata( { version: 1 } )
				.setSystemMessage( "System prompt" )
				.add( "User: Hello" )
				.add( { role: "assistant", content: "Assistant: Hi!" } )
			
			exported = original.export()
			
			// Clean up the original key to test fresh import
			queryExecute(
				"DELETE FROM ai_memory WHERE memory_key = 'roundtrip'",
				{},
				{ datasource: "testMemoryDB" }
			)
			
			restored = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.import( exported )
			
			result = {
				key: restored.key(),
				count: restored.count(),
				systemMsg: restored.getSystemMessage(),
				messages: restored.getAll(),
				datasource: restored.datasource(),
				table: restored.table()
			}
			""",
			context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "key" ) ) ).isEqualTo( "roundtrip" );
		assertThat( result.get( "count" ) ).isEqualTo( 3 );
		assertThat( result.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "System prompt" );
		assertThat( result.getAsArray( Key.of( "messages" ) ).size() ).isEqualTo( 3 );
		assertThat( result.getAsString( Key.of( "datasource" ) ) ).isEqualTo( "testMemoryDB" );
		assertThat( result.getAsString( Key.of( "table" ) ) ).isEqualTo( "ai_memory" );
	}

	@Test
	@DisplayName( "Test JdbcMemory with metadata persistence" )
	public void testMetadataPersistence() {
		runtime.executeSource(
			"""
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "metadata-test" )
				.metadata( { userId: "123", sessionId: "abc" } )
				.add( "Test message" )
			
			// Load from database
			memory2 = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "metadata-test" )
			
			metadata = memory2.metadata()
			""",
			context
		);

		var metadata = variables.getAsStruct( Key.of( "metadata" ) );
		assertThat( metadata.getAsString( Key.of( "userId" ) ) ).isEqualTo( "123" );
		assertThat( metadata.getAsString( Key.of( "sessionId" ) ) ).isEqualTo( "abc" );
	}

	@Test
	@DisplayName( "Test JdbcMemory with aiMemory BIF" )
	public void testWithAiMemoryBIF() {
		runtime.executeSource(
			"""
			memory = aiMemory( "jdbc", { 
				datasource: "testMemoryDB",
				table: "ai_memory"
			} )
				.key( "bif-test" )
				.add( "BIF message" )
			
			count = memory.count()
			name = memory.name()
			""",
			context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "JdbcMemory" );
	}

	@Test
	@DisplayName( "Test JdbcMemory system message persistence" )
	public void testSystemMessagePersistence() {
		runtime.executeSource(
			"""
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "system-test" )
				.setSystemMessage( "Be helpful" )
				.add( "User message" )
			
			// Load from database
			memory2 = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "system-test" )
			
			systemMsg = memory2.getSystemMessage()
			count = memory2.count()
			""",
			context
		);

		assertThat( variables.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "Be helpful" );
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test JdbcMemory with AiMessage integration" )
	public void testAiMessageIntegration() {
		runtime.executeSource(
			"""
			msg = aiMessage()
				.system( "Be helpful" )
				.user( "Hello" )
			
			memory = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "aimsg-test" )
				.add( msg )
			
			// Load from database
			memory2 = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "aimsg-test" )
			
			count = memory2.count()
			""",
			context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		assertThat( count ).isEqualTo( 2 ); // system + user message
	}

	@Test
	@DisplayName( "Test JdbcMemory requires datasource" )
	public void testRequiresDatasource() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
				"""
				memory = new bxModules.bxai.models.memory.JdbcMemory()
					.configure( { table: "ai_memory" } )
				""",
				context
			);
		} );
	}

	@Test
	@DisplayName( "Test JdbcMemory with multiple keys" )
	public void testMultipleKeys() {
		runtime.executeSource(
			"""
			// Create memories with different keys
			memory1 = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "key1" )
				.add( "Message for key1" )
			
			memory2 = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "key2" )
				.add( "Message for key2" )
			
			// Reload and verify they don't interfere with each other
			reloaded1 = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "key1" )
			
			reloaded2 = new bxModules.bxai.models.memory.JdbcMemory()
				.configure( { 
					datasource: "testMemoryDB",
					table: "ai_memory"
				} )
				.key( "key2" )
			
			count1 = reloaded1.count()
			count2 = reloaded2.count()
			msg1 = reloaded1.getAll()[1].content
			msg2 = reloaded2.getAll()[1].content
			""",
			context
		);

		assertThat( variables.getAsInteger( Key.of( "count1" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "count2" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsString( Key.of( "msg1" ) ) ).isEqualTo( "Message for key1" );
		assertThat( variables.getAsString( Key.of( "msg2" ) ) ).isEqualTo( "Message for key2" );
	}

}
