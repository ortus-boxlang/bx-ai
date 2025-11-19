package ortus.boxlang.ai.memory;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class FileMemoryTest extends BaseIntegrationTest {

	private Path tempDir;

	@BeforeEach
	public void setupEach() {
		super.setupEach();
		try {
			tempDir = Files.createTempDirectory( "filememory-test-" );
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to create temp directory", e );
		}
	}

	@AfterEach
	public void tearDown() {
		try {
			// Clean up temp files
			if ( tempDir != null && Files.exists( tempDir ) ) {
				Files.walk( tempDir )
				    .sorted( ( a, b ) -> b.compareTo( a ) )
				    .forEach( path -> {
					    try {
						    Files.deleteIfExists( path );
					    } catch ( Exception e ) {
						    // Ignore
					    }
				    } );
			}
		} catch ( Exception e ) {
			// Ignore cleanup errors
		}
	}

	@Test
	@DisplayName( "Test FileMemory instantiation" )
	public void testInstantiation() {
		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "test-key", \"%s\" )
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		var memory = variables.get( Key.of( "memory" ) );
		assertThat( memory ).isNotNull();
	}

	@Test
	@DisplayName( "Test FileMemory with directoryPath configuration" )
	public void testDirectoryPathConfiguration() {
		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "test-key" )
		                       .configure( { directoryPath: \"%s\" } )

		                   result = memory.directoryPath()
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( tempDir.toString() );
	}

	@Test
	@DisplayName( "Test FileMemory persists messages to file" )
	public void testPersistMessages() {
		String expectedFile = tempDir.toString() + "/memory-test-key.json";
		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( \"test-key\", \"%s\" )
		                       .add( \"Hello World\" )

		                   fileExists = fileExists( \"%s\" )
		                   """, tempDir.toString().replace( "\\", "\\\\" ), expectedFile.replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "fileExists" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test FileMemory loads messages from file" )
	public void testLoadFromFile() {
		runtime.executeSource(
		    String.format( """
		                   // Create first memory and add messages
		                   memory1 = new bxModules.bxai.models.memory.FileMemory( "persist-key", "%s" )
		                       .add( "Message 1" )
		                       .add( "Message 2" )

		                   // Create new memory instance with same key to load from the same file
		                   memory2 = new bxModules.bxai.models.memory.FileMemory( "persist-key", "%s" )
		                       .configure( {} )

		                   count = memory2.count()
		                   messages = memory2.getAll()
		                   key = memory2.key()
		                   """, tempDir.toString().replace( "\\", "\\\\" ), tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		var	count		= variables.getAsInteger( Key.of( "count" ) );
		var	messages	= variables.getAsArray( Key.of( "messages" ) );
		var	key			= variables.getAsString( Key.of( "key" ) );

		assertThat( count ).isEqualTo( 2 );
		assertThat( messages.size() ).isEqualTo( 2 );
		assertThat( key ).isEqualTo( "persist-key" );
	}

	@Test
	@DisplayName( "Test FileMemory clear removes file content" )
	public void testClearRemovesContent() {
		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "clear-key", "%s" )
		                       .add( "Message 1" )
		                       .add( "Message 2" )
		                       .clear()

		                   count = memory.count()

		                   // Load from file again to verify it's deleted
		                   memory2 = new bxModules.bxai.models.memory.FileMemory( "clear-key", "%s" )
		                       .configure( {} )

		                   count2 = memory2.count()
		                   """, tempDir.toString().replace( "\\", "\\\\" ), tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "count2" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test FileMemory getSummary()" )
	public void testGetSummary() {
		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "test-key", "%s" )
		                       .setSystemMessage( "Test system" )
		                       .add( "User message" )

		                   summary = memory.getSummary()
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		String	expectedFilePath	= tempDir.toString() + "/memory-test-key.json";
		var		summary				= variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "type" ) ) ).isEqualTo( "FileMemory" );
		assertThat( summary.getAsString( Key.of( "key" ) ) ).isEqualTo( "test-key" );
		assertThat( summary.get( "messageCount" ) ).isEqualTo( 2 ); // system + user
		assertThat( summary.getAsBoolean( Key.of( "hasSystemMessage" ) ) ).isTrue();
		assertThat( summary.getAsString( Key.of( "filePath" ) ) ).isEqualTo( expectedFilePath );
		assertThat( summary.getAsBoolean( Key.of( "fileExists" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test FileMemory export()" )
	public void testExport() {
		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "export-key", "%s" )
		                       .metadata( { userId: 789 } )
		                       .configure( { maxSize: 100 } )
		                       .add( { role: "user", content: "Test" } )

		                   exported = memory.export()
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		var exported = variables.getAsStruct( Key.of( "exported" ) );
		assertThat( exported.getAsString( Key.of( "key" ) ) ).isEqualTo( "export-key" );
		assertThat( exported.containsKey( Key.of( "metadata" ) ) ).isTrue();
		assertThat( exported.containsKey( Key.of( "config" ) ) ).isTrue();
		assertThat( exported.containsKey( Key.of( "messages" ) ) ).isTrue();
		assertThat( exported.getAsString( Key.of( "directoryPath" ) ) ).isEqualTo( tempDir.toString() );
	}

	@Test
	@DisplayName( "Test FileMemory import()" )
	public void testImport() {
		runtime.executeSource(
		    String.format( """
		                   data = {
		                       key: "imported-key",
		                       metadata: { imported: true },
		                       config: { setting: "value" },
		                       messages: [
		                           { role: "user", content: "Imported message" }
		                       ],
		                       directoryPath: "%s"
		                   }

		                   memory = new bxModules.bxai.models.memory.FileMemory( "temp-key" )
		                       .import( data )

		                   result = {
		                       key: memory.key(),
		                       count: memory.count(),
		                       metadata: memory.metadata(),
		                       config: memory.getConfig(),
		                       directoryPath: memory.directoryPath()
		                   }
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "key" ) ) ).isEqualTo( "imported-key" );
		assertThat( result.get( "count" ) ).isEqualTo( 1 );
		assertThat( result.getAsString( Key.of( "directoryPath" ) ) ).isEqualTo( tempDir.toString() );

		IStruct metadata = ( IStruct ) result.get( "metadata" );
		assertThat( metadata.getAsBoolean( Key.of( "imported" ) ) ).isTrue();

		IStruct config = ( IStruct ) result.get( "config" );
		assertThat( config.getAsString( Key.of( "setting" ) ) ).isEqualTo( "value" );
	}

	@Test
	@DisplayName( "Test FileMemory export/import roundtrip" )
	public void testExportImportRoundtrip() {
		runtime.executeSource(
		    String.format( """
		                   original = new bxModules.bxai.models.memory.FileMemory( "roundtrip", "%s" )
		                       .metadata( { version: 1 } )
		                       .setSystemMessage( "System prompt" )
		                       .add( "User: Hello" )
		                       .add( { role: "assistant", content: "Assistant: Hi!" } )

		                   exported = original.export()

		                   restored = new bxModules.bxai.models.memory.FileMemory( "temp-key" )
		                       .import( exported )

		                   result = {
		                       key: restored.key(),
		                       count: restored.count(),
		                       systemMsg: restored.getSystemMessage(),
		                       messages: restored.getAll(),
		                       directoryPath: restored.directoryPath()
		                   }
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "key" ) ) ).isEqualTo( "roundtrip" );
		assertThat( result.get( "count" ) ).isEqualTo( 3 );
		assertThat( result.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "System prompt" );
		assertThat( result.getAsArray( Key.of( "messages" ) ).size() ).isEqualTo( 3 );
		assertThat( result.getAsString( Key.of( "directoryPath" ) ) ).isEqualTo( tempDir.toString() );
	}

	@Test
	@DisplayName( "Test FileMemory with metadata persistence" )
	public void testMetadataPersistence() {
		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "metadata-key", "%s" )
		                       .metadata( { userId: "123", sessionId: "abc" } )
		                       .add( "Test message" )

		                   // Load from file with same key
		                   memory2 = new bxModules.bxai.models.memory.FileMemory( "metadata-key", "%s" )
		                       .configure( {} )

		                   metadata = memory2.metadata()
		                   """, tempDir.toString().replace( "\\", "\\\\" ), tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		var metadata = variables.getAsStruct( Key.of( "metadata" ) );
		assertThat( metadata.getAsString( Key.of( "userId" ) ) ).isEqualTo( "123" );
		assertThat( metadata.getAsString( Key.of( "sessionId" ) ) ).isEqualTo( "abc" );
	}

	@Test
	@DisplayName( "Test FileMemory handles missing file gracefully" )
	public void testMissingFileGraceful() {
		String nonExistentPath = tempDir.resolve( "nonexistent.json" ).toString();

		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "test-key", "%s" )
		                       .configure( {} )

		                   count = memory.count()
		                   isEmpty = memory.isEmpty()
		                   """, nonExistentPath.replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test FileMemory creates directory if needed" )
	public void testDirectoryCreation() {
		Path	nestedDir		= tempDir.resolve( "nested/dir" );
		String	expectedFile	= nestedDir.toString() + "/memory-nested-key.json";

		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "nested-key", \"%s\" )
		                       .add( "Test message" )

		                   fileExists = fileExists( \"%s\" )
		                   """, nestedDir.toString().replace( "\\", "\\\\" ), expectedFile.replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "fileExists" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test FileMemory with aiMemory BIF" )
	public void testWithAiMemoryBIF() {
		runtime.executeSource(
		    String.format( """
		                   memory = aiMemory( "file", "bif-test", { directoryPath: "%s" } )
		                       .add( "BIF message" )

		                   count = memory.count()
		                   name = memory.name()
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "FileMemory" );
	}

	@Test
	@DisplayName( "Test FileMemory system message persistence" )
	public void testSystemMessagePersistence() {
		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "system-key", \"%s\" )
		                       .setSystemMessage( "Be helpful" )
		                       .add( "User message" )

		                   // Load from file with same key
		                   memory2 = new bxModules.bxai.models.memory.FileMemory( "system-key", \"%s\" )
		                       .configure( {} )

		                   systemMsg = memory2.getSystemMessage()
		                   count = memory2.count()
		                   """, tempDir.toString().replace( "\\", "\\\\" ), tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "Be helpful" );
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test FileMemory handles empty file" )
	public void testEmptyFileHandling() throws Exception {
		// Create an empty file with the expected naming convention
		String emptyFileName = tempDir.toString() + "/memory-empty-key.json";
		Files.writeString( Path.of( emptyFileName ), "" );

		runtime.executeSource(
		    String.format( """
		                   memory = new bxModules.bxai.models.memory.FileMemory( "empty-key", "%s" )
		                       .configure( {} )

		                   count = memory.count()
		                   isEmpty = memory.isEmpty()
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test FileMemory with AiMessage integration" )
	public void testAiMessageIntegration() {
		runtime.executeSource(
		    String.format( """
		                   msg = aiMessage()
		                       .system( "Be helpful" )
		                       .user( "Hello" )

		                   memory = new bxModules.bxai.models.memory.FileMemory( "aimessage-key", "%s" )
		                       .add( msg )

		                   // Load from file with same key
		                   memory2 = new bxModules.bxai.models.memory.FileMemory( "aimessage-key", "%s" )
		                       .configure( {} )

		                   count = memory2.count()
		                   """, tempDir.toString().replace( "\\", "\\\\" ), tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		assertThat( count ).isEqualTo( 2 ); // system + user message
	}

}
