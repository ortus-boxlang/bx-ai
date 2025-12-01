package ortus.boxlang.ai.memory;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class WindowMemoryTest extends BaseIntegrationTest {

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test WindowMemory instantiation with default max" )
	public void testInstantiationDefault() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered" )
		    maxMessages = memory.getMaxMessages()
		    """,
		    context
		);

		var maxMessages = variables.getAsInteger( Key.of( "maxMessages" ) );
		assertThat( maxMessages ).isEqualTo( 100 );
	}

	@Test
	@DisplayName( "Test WindowMemory with custom max via constructor" )
	public void testCustomMaxViaConstructor() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.WindowMemory( "test-key", 5 )
		    maxMessages = memory.getMaxMessages()
		    """,
		    context
		);

		var maxMessages = variables.getAsInteger( Key.of( "maxMessages" ) );
		assertThat( maxMessages ).isEqualTo( 5 );
	}

	@Test
	@DisplayName( "Test WindowMemory with custom max via config" )
	public void testCustomMaxViaConfig() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered", "test-key", { maxMessages: 3 } )
		    maxMessages = memory.getMaxMessages()
		    """,
		    context
		);

		var maxMessages = variables.getAsInteger( Key.of( "maxMessages" ) );
		assertThat( maxMessages ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test WindowMemory auto-trims messages" )
	public void testAutoTrim() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered", "test-key", { maxMessages: 3 } )

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
	@DisplayName( "Test WindowMemory preserves system message during trim" )
	public void testSystemMessagePreservation() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered", "test-key", { maxMessages: 2 } )
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
	@DisplayName( "Test WindowMemory setMaxMessages triggers trim" )
	public void testSetMaxMessagesTriggersTrim() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered", "test-key", { maxMessages: 10 } )

		    // Add 5 messages
		    for( i = 1; i <= 5; i++ ) {
		        memory.add( "Message " & i )
		    }

		    countBefore = memory.count()

		    // Reduce max to 2
		    memory.setMaxMessages( 2 )

		    countAfter = memory.count()
		    messages = memory.getAll()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "countBefore" ) ) ).isEqualTo( 5 );
		assertThat( variables.getAsInteger( Key.of( "countAfter" ) ) ).isEqualTo( 2 );

		// Should keep last 2 messages (4, 5)
		var		messages	= variables.getAsArray( Key.of( "messages" ) );
		IStruct	firstMsg	= ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 4" );
	}

	@Test
	@DisplayName( "Test WindowMemory getSummary includes max" )
	public void testGetSummary() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered", "buffered-test", { maxMessages: 5 } )
		        .add( "Test" )

		    summary = memory.getSummary()
		    """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "type" ) ) ).isEqualTo( "WindowMemory" );
		assertThat( summary.get( "maxMessages" ) ).isEqualTo( 5 );
		assertThat( summary.get( "messageCount" ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test WindowMemory export includes maxMessages" )
	public void testExport() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered", "export-test", { maxMessages: 7 } )
		        .add( "Message" )

		    exported = memory.export()
		    """,
		    context
		);

		var exported = variables.getAsStruct( Key.of( "exported" ) );
		assertThat( exported.getAsString( Key.of( "type" ) ) ).isEqualTo( "WindowMemory" );
		assertThat( exported.get( "maxMessages" ) ).isEqualTo( 7 );
		assertThat( exported.containsKey( Key.of( "messages" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test WindowMemory import trims if over max" )
	public void testImportWithTrim() {
		runtime.executeSource(
		    """
		    // Create data with 5 messages
		    data = {
		        key: "import-test",
		        maxMessages: 2,
		        messages: [
		            { role: "user", content: "Message 1" },
		            { role: "user", content: "Message 2" },
		            { role: "user", content: "Message 3" },
		            { role: "user", content: "Message 4" },
		            { role: "user", content: "Message 5" }
		        ]
		    }

		    memory = aiMemory( "buffered" )
		        .import( data )

		    count = memory.count()
		    maxMessages = memory.getMaxMessages()
		    messages = memory.getAll()
		    """,
		    context
		);

		// Should trim to max of 2
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "maxMessages" ) ) ).isEqualTo( 2 );

		// Should keep last 2 (4, 5)
		var		messages	= variables.getAsArray( Key.of( "messages" ) );
		IStruct	firstMsg	= ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 4" );
	}

	@Test
	@DisplayName( "Test WindowMemory with system message doesn't count toward limit" )
	public void testSystemMessageNotCountedInLimit() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered", "test-key", { maxMessages: 2 } )
		        .setSystemMessage( "System" )

		    // Add 3 non-system messages
		    memory.add( "User 1" )
		    memory.add( "User 2" )
		    memory.add( "User 3" )

		    total = memory.count()
		    nonSystem = memory.getNonSystemMessages().len()
		    systemMsg = memory.getSystemMessage()
		    """,
		    context
		);

		// Total = system + 2 user messages (trimmed from 3)
		assertThat( variables.getAsInteger( Key.of( "total" ) ) ).isEqualTo( 3 );

		// Non-system should be exactly maxMessages
		assertThat( variables.getAsInteger( Key.of( "nonSystem" ) ) ).isEqualTo( 2 );

		// System message should still exist
		assertThat( variables.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "System" );
	}

	@Test
	@DisplayName( "Test WindowMemory trim() method" )
	public void testManualTrim() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered", "test-key", { maxMessages: 5 } )

		    // Add 3 messages (under limit)
		    memory.add( "Message 1" )
		    memory.add( "Message 2" )
		    memory.add( "Message 3" )

		    // Manually reduce max and trim
		    memory.setMaxMessages( 1 )

		    count = memory.count()
		    messages = memory.getAll()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 1 );

		var		messages	= variables.getAsArray( Key.of( "messages" ) );
		IStruct	msg			= ( IStruct ) messages.get( 0 );
		assertThat( msg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 3" );
	}

	@Test
	@DisplayName( "Test WindowMemory adding array triggers single trim" )
	public void testAddArrayTriggersTrim() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "buffered", "test-key", { maxMessages: 3 } )

		    // Add array of 5 messages at once
		    memory.add( [
		        { role: "user", content: "Msg 1" },
		        { role: "user", content: "Msg 2" },
		        { role: "user", content: "Msg 3" },
		        { role: "user", content: "Msg 4" },
		        { role: "user", content: "Msg 5" }
		    ] )

		    count = memory.count()
		    messages = memory.getAll()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 3 );

		var		messages	= variables.getAsArray( Key.of( "messages" ) );
		IStruct	firstMsg	= ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).isEqualTo( "Msg 3" );
	}

}
