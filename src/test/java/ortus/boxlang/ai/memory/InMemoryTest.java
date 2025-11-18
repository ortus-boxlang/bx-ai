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

public class InMemoryTest extends BaseIntegrationTest {

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test InMemory instantiation" )
	public void testInstantiation() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.InMemory()
		    """,
		    context
		);

		var memory = variables.get( Key.of( "memory" ) );
		assertThat( memory ).isNotNull();
	}

	@Test
	@DisplayName( "Test InMemory getSummary()" )
	public void testGetSummary() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.InMemory()
		        .key( "test-key" )
		        .setSystemMessage( "Test system" )
		        .add( "User message" )

		    summary = memory.getSummary()
		    """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "type" ) ) ).isEqualTo( "InMemory" );
		assertThat( summary.getAsString( Key.of( "key" ) ) ).isEqualTo( "test-key" );
		assertThat( summary.get( "messageCount" ) ).isEqualTo( 2 ); // system + user
		assertThat( summary.getAsBoolean( Key.of( "hasSystemMessage" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test InMemory export()" )
	public void testExport() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.InMemory()
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
	@DisplayName( "Test InMemory import()" )
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

		    memory = new bxModules.bxai.models.memory.InMemory()
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
	@DisplayName( "Test InMemory export/import roundtrip" )
	public void testExportImportRoundtrip() {
		runtime.executeSource(
		    """
		    original = new bxModules.bxai.models.memory.InMemory()
		        .key( "roundtrip" )
		        .metadata( { version: 1 } )
		        .setSystemMessage( "System prompt" )
		        .add( "User: Hello" )
		        .add( { role: "assistant", content: "Assistant: Hi!" } )

		    exported = original.export()

		    restored = new bxModules.bxai.models.memory.InMemory()
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
	@DisplayName( "Test InMemory message timestamps" )
	public void testMessageTimestamps() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.InMemory()
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
	@DisplayName( "Test InMemory invalid message format throws error" )
	public void testInvalidMessageFormat() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    memory = new bxModules.bxai.models.memory.InMemory()
			        .add( { invalidKey: "no role or content" } )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test InMemory with AiMessage integration" )
	public void testAiMessageIntegration() {
		runtime.executeSource(
		    """
		       msg = aiMessage()
		           .system( "Be helpful" )
		           .user( "Hello" )

		       memory = new bxModules.bxai.models.memory.InMemory()
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
	@DisplayName( "Test InMemory configuration persistence" )
	public void testConfigurationPersistence() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.InMemory()
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
	@DisplayName( "Test InMemory replaces system message" )
	public void testSystemMessageReplacement() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.InMemory()
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
	@DisplayName( "Test InMemory getRecent with more messages than limit" )
	public void testGetRecentWithLimit() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.InMemory()

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

}
