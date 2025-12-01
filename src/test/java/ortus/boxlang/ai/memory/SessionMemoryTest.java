package ortus.boxlang.ai.memory;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class SessionMemoryTest extends BaseIntegrationTest {

	@BeforeEach
	public void setupEach() {
		super.setupEach();
		// @formatter:off
		runtime.executeSource(
		    """
				// Setup session management
				bx:application name="unit-test1" sessionmanagement="true";
				structClear( session )
		     """,
		    context
		);
		// @formatter:on
	}

	@Test
	@DisplayName( "Test SessionMemory instantiation with default key" )
	public void testInstantiationDefault() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SessionMemory()
		    key = memory.key()
		    maxMessages = memory.getMaxMessages()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "key" ) ) ).isEqualTo( "bxai" );
		assertThat( variables.getAsInteger( Key.of( "maxMessages" ) ) ).isEqualTo( 100 );
	}

	@Test
	@DisplayName( "Test SessionMemory instantiation with custom key and max" )
	public void testInstantiationCustom() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SessionMemory( "my-session-key", 10 )
		    key = memory.key()
		    maxMessages = memory.getMaxMessages()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "key" ) ) ).isEqualTo( "my-session-key" );
		assertThat( variables.getAsInteger( Key.of( "maxMessages" ) ) ).isEqualTo( 10 );
	}

	@Test
	@DisplayName( "Test SessionMemory stores in session scope" )
	public void testSessionStorage() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SessionMemory( "test-session" )
		        .add( "Message 1" )
		        .add( "Message 2" )

		    // Check session scope directly
		    sessionHasKey = structKeyExists( session, "test-session" )
		    sessionData = session[ "test-session" ]
		    messageCount = sessionData.messages.len()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "sessionHasKey" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "messageCount" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test SessionMemory persists across instances with same key" )
	public void testPersistenceAcrossInstances() {
		runtime.executeSource(
		    """
		    // First instance adds messages
		    memory1 = new bxModules.bxai.models.memory.SessionMemory( "shared-key" )
		        .add( "First message" )
		        .add( "Second message" )

		    count1 = memory1.count()

		    // Second instance with same key should see same messages
		    memory2 = new bxModules.bxai.models.memory.SessionMemory( "shared-key" )
		    count2 = memory2.count()
		    messages2 = memory2.getAll()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count1" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "count2" ) ) ).isEqualTo( 2 );

		var		messages	= variables.getAsArray( Key.of( "messages2" ) );
		IStruct	firstMsg	= ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "First message" );
	}

	@Test
	@DisplayName( "Test SessionMemory auto-trims like WindowMemory" )
	public void testAutoTrim() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SessionMemory( "trim-test", 3 )

		    // Add 5 messages
		    memory.add( "Message 1" )
		    memory.add( "Message 2" )
		    memory.add( "Message 3" )
		    memory.add( "Message 4" )
		    memory.add( "Message 5" )

		    count = memory.count()
		    messages = memory.getAll()
		    """,
		    context
		);

		// Should only have 3 messages (auto-trimmed)
		var count = variables.getAsInteger( Key.of( "count" ) );
		assertThat( count ).isEqualTo( 3 );

		// Should be the last 3 messages (3, 4, 5)
		var		messages	= variables.getAsArray( Key.of( "messages" ) );
		IStruct	firstMsg	= ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 3" );

		IStruct lastMsg = ( IStruct ) messages.get( 2 );
		assertThat( lastMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 5" );
	}

	@Test
	@DisplayName( "Test SessionMemory preserves system message during trim" )
	public void testSystemMessagePreservation() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SessionMemory( "system-test", 2 )
		        .setSystemMessage( "You are helpful" )
		        .add( "User 1" )
		        .add( "Assistant 1" )
		        .add( "User 2" )
		        .add( "Assistant 2" )

		    systemMsg = memory.getSystemMessage()
		    count = memory.count()
		    nonSystemCount = memory.getNonSystemMessages().len()
		    """,
		    context
		);

		// System message should still be there
		assertThat( variables.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "You are helpful" );

		// Total count = system + 2 messages
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 3 );

		// Non-system count should be exactly maxMessages
		assertThat( variables.getAsInteger( Key.of( "nonSystemCount" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test SessionMemory clear empties session storage" )
	public void testClear() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SessionMemory( "clear-test" )
		        .add( "Message 1" )
		        .add( "Message 2" )

		    countBefore = memory.count()
		    memory.clear()
		    countAfter = memory.count()

		    // Check session is empty
		    sessionMessages = session[ "clear-test" ].messages
		    sessionEmpty = sessionMessages.isEmpty()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "countBefore" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "countAfter" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsBoolean( Key.of( "sessionEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SessionMemory metadata persists in session" )
	public void testMetadataPersistence() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SessionMemory( "meta-test" )
		        .metadata( { userId: 123, version: 1 } )

		    // Check metadata from memory
		    meta1 = memory.metadata()

		    // Create new instance with same key
		    memory2 = new bxModules.bxai.models.memory.SessionMemory( "meta-test" )
		    meta2 = memory2.metadata()
		    """,
		    context
		);

		var meta1 = variables.getAsStruct( Key.of( "meta1" ) );
		assertThat( meta1.get( "userId" ) ).isEqualTo( 123 );

		var meta2 = variables.getAsStruct( Key.of( "meta2" ) );
		assertThat( meta2.get( "userId" ) ).isEqualTo( 123 );
		assertThat( meta2.get( "version" ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test SessionMemory export includes session data" )
	public void testExport() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SessionMemory( "export-test", 5 )
		        .metadata( { exported: true } )
		        .add( "Test message" )

		    exported = memory.export()
		    """,
		    context
		);

		var exported = variables.getAsStruct( Key.of( "exported" ) );
		assertThat( exported.getAsString( Key.of( "key" ) ) ).isEqualTo( "export-test" );
		assertThat( exported.get( "maxMessages" ) ).isEqualTo( 5 );
		assertThat( exported.containsKey( Key.of( "messages" ) ) ).isTrue();
		assertThat( exported.containsKey( Key.of( "metadata" ) ) ).isTrue();

		IStruct metadata = ( IStruct ) exported.get( "metadata" );
		assertThat( metadata.getAsBoolean( Key.of( "exported" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SessionMemory import restores to session" )
	public void testImport() {
		runtime.executeSource(
		    """
		    data = {
		        key: "import-test",
		        maxMessages: 3,
		        messages: [
		            { role: "user", content: "Imported 1" },
		            { role: "user", content: "Imported 2" }
		        ],
		        metadata: { imported: true }
		    }

		    memory = new bxModules.bxai.models.memory.SessionMemory( "temp" )
		        .import( data )

		    count = memory.count()
		    messages = memory.getAll()
		    meta = memory.metadata()

		    // Check session storage
		    sessionHasKey = structKeyExists( session, "import-test" )
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "sessionHasKey" ) ) ).isTrue();

		var		messages	= variables.getAsArray( Key.of( "messages" ) );
		IStruct	firstMsg	= ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Imported 1" );

		var meta = variables.getAsStruct( Key.of( "meta" ) );
		assertThat( meta.getAsBoolean( Key.of( "imported" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SessionMemory import with trim" )
	public void testImportWithTrim() {
		runtime.executeSource(
		    """
		    // Create data with 5 messages but maxMessages of 2
		    data = {
		        key: "trim-import",
		        maxMessages: 2,
		        messages: [
		            { role: "user", content: "Message 1" },
		            { role: "user", content: "Message 2" },
		            { role: "user", content: "Message 3" },
		            { role: "user", content: "Message 4" },
		            { role: "user", content: "Message 5" }
		        ]
		    }

		    memory = new bxModules.bxai.models.memory.SessionMemory( "temp" )
		        .import( data )

		    count = memory.count()
		    messages = memory.getAll()
		    """,
		    context
		);

		// Should trim to max of 2
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 2 );

		// Should keep last 2 (4, 5)
		var		messages	= variables.getAsArray( Key.of( "messages" ) );
		IStruct	firstMsg	= ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 4" );

		IStruct lastMsg = ( IStruct ) messages.get( 1 );
		assertThat( lastMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 5" );
	}

	@Test
	@DisplayName( "Test SessionMemory setMaxMessages triggers trim in session" )
	public void testSetMaxMessagesTriggersTrim() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SessionMemory( "max-test", 10 )

		    // Add 5 messages
		    for( i = 1; i <= 5; i++ ) {
		        memory.add( "Message " & i )
		    }

		    countBefore = memory.count()

		    // Reduce max to 2
		    memory.setMaxMessages( 2 )

		    countAfter = memory.count()
		    messages = memory.getAll()

		    // Check session storage
		    sessionCount = session[ "max-test" ].messages.len()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "countBefore" ) ) ).isEqualTo( 5 );
		assertThat( variables.getAsInteger( Key.of( "countAfter" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "sessionCount" ) ) ).isEqualTo( 2 );

		// Should keep last 2 messages (4, 5)
		var		messages	= variables.getAsArray( Key.of( "messages" ) );
		IStruct	firstMsg	= ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 4" );
	}

	@Test
	@DisplayName( "Test SessionMemory getSummary" )
	public void testGetSummary() {
		runtime.executeSource(
		    """
		       memory = new bxModules.bxai.models.memory.SessionMemory( "summary-test", 5 )
		           .setSystemMessage( "Test system" )
		           .add( "Test message" )

		       summary = memory.getSummary()
		    println( summary )
		       """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "type" ) ) ).isEqualTo( "SessionMemory" );
		assertThat( summary.getAsString( Key.of( "key" ) ) ).isEqualTo( "summary-test" );
		assertThat( summary.get( "maxMessages" ) ).isEqualTo( 5 );
		assertThat( summary.get( "messageCount" ) ).isEqualTo( 2 ); // system + user
		assertThat( summary.getAsBoolean( Key.of( "hasSystemMessage" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SessionMemory with AiMessage integration" )
	public void testAiMessageIntegration() {
		runtime.executeSource(
		    """
		    msg = aiMessage()
		        .system( "Be helpful" )
		        .user( "Hello" )
		        .assistant( "Hi there!" )

		    memory = new bxModules.bxai.models.memory.SessionMemory( "aimsg-test" )
		        .add( msg )

		    count = memory.count()
		    messages = memory.getAll()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 3 );

		var messages = variables.getAsArray( Key.of( "messages" ) );
		assertThat( messages.size() ).isEqualTo( 3 );

		IStruct systemMsg = ( IStruct ) messages.get( 0 );
		assertThat( systemMsg.getAsString( Key.of( "role" ) ) ).isEqualTo( "system" );
	}

	@Test
	@DisplayName( "Test SessionMemory isolation between different keys" )
	public void testKeyIsolation() {
		runtime.executeSource(
		    """
		    memory1 = new bxModules.bxai.models.memory.SessionMemory( "key-1" )
		        .add( "Memory 1 message" )

		    memory2 = new bxModules.bxai.models.memory.SessionMemory( "key-2" )
		        .add( "Memory 2 message" )

		    count1 = memory1.count()
		    count2 = memory2.count()
		    messages1 = memory1.getAll()
		    messages2 = memory2.getAll()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count1" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "count2" ) ) ).isEqualTo( 1 );

		var		messages1	= variables.getAsArray( Key.of( "messages1" ) );
		IStruct	msg1		= ( IStruct ) messages1.get( 0 );
		assertThat( msg1.getAsString( Key.of( "content" ) ) ).isEqualTo( "Memory 1 message" );

		var		messages2	= variables.getAsArray( Key.of( "messages2" ) );
		IStruct	msg2		= ( IStruct ) messages2.get( 0 );
		assertThat( msg2.getAsString( Key.of( "content" ) ) ).isEqualTo( "Memory 2 message" );
	}

}
