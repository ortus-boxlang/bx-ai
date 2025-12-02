package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Document Loader BIFs Tests" )
public class DocumentLoaderBIFsTest extends BaseIntegrationTest {

	private static final String	TEST_RESOURCES	= Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

	// ===========================================
	// aiDocuments() Tests
	// ===========================================

	@DisplayName( "aiDocuments() can load a text file" )
	@Test
	public void testAiDocumentsText() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiDocuments( "%s/sample.txt" );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );
	}

	@DisplayName( "aiDocuments() can load with explicit type" )
	@Test
	public void testAiDocumentsExplicitType() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiDocuments( source: "%s/sample.md", type: "markdown" );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "aiDocuments() can load a directory" )
	@Test
	public void testAiDocumentsDirectory() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiDocuments( "%s" );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isGreaterThan( 1 );
	}

	// ===========================================
	// aiDocumentLoader() Tests
	// ===========================================

	@DisplayName( "aiDocumentLoader() creates a text loader" )
	@Test
	public void testAiDocumentLoaderText() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocumentLoader( source: "%s/sample.txt", type: "text" );
				result = loader.load();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );
	}

	@DisplayName( "aiDocumentLoader() creates a markdown loader" )
	@Test
	public void testAiDocumentLoaderMarkdown() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocumentLoader( source: "%s/sample.md", type: "markdown" );
				result = loader.load();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "aiDocumentLoader() auto-detects loader type from extension" )
	@Test
	public void testAiDocumentLoaderAutoDetect() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocumentLoader( source: "%s/sample.json" );
				loaderName = loader.getName();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		String loaderName = ( String ) variables.get( "loaderName" );
		assertThat( loaderName ).isEqualTo( "JSONLoader" );
	}

	@DisplayName( "aiDocumentLoader() creates a directory loader" )
	@Test
	public void testAiDocumentLoaderDirectory() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocumentLoader( source: "%s", type: "directory" );
				result = loader.load();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isGreaterThan( 1 );
	}

	// ===========================================
	// aiDocumentLoaders() Tests
	// ===========================================

	@DisplayName( "aiDocumentLoaders() returns all registered loaders" )
	@Test
	public void testAiDocumentLoaders() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiDocumentLoaders();
		    """,
		    context
		);
		// @formatter:on

		IStruct loaders = ( IStruct ) variables.get( "result" );
		assertThat( loaders ).isNotNull();
		assertThat( loaders.containsKey( Key.of( "text" ) ) ).isTrue();
		assertThat( loaders.containsKey( Key.of( "markdown" ) ) ).isTrue();
		assertThat( loaders.containsKey( Key.of( "html" ) ) ).isTrue();
		assertThat( loaders.containsKey( Key.of( "csv" ) ) ).isTrue();
		assertThat( loaders.containsKey( Key.of( "json" ) ) ).isTrue();
		assertThat( loaders.containsKey( Key.of( "directory" ) ) ).isTrue();
	}

	@DisplayName( "aiDocumentLoaders() returns loader metadata" )
	@Test
	public void testAiDocumentLoadersMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				loaders = aiDocumentLoaders();
				textLoader = loaders.text;
				result = {
					name: textLoader.name,
					hasExtensions: textLoader.keyExists( "extensions" ),
					hasCapabilities: textLoader.keyExists( "capabilities" ),
					hasConfigOptions: textLoader.keyExists( "configOptions" )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "name" ) ) ).isEqualTo( "TextLoader" );
		assertThat( result.get( Key.of( "hasExtensions" ) ) ).isEqualTo( true );
		assertThat( result.get( Key.of( "hasCapabilities" ) ) ).isEqualTo( true );
		assertThat( result.get( Key.of( "hasConfigOptions" ) ) ).isEqualTo( true );
	}

	// ===========================================
	// aiMemoryIngest() Tests
	// ===========================================

	@DisplayName( "aiMemoryIngest() can ingest documents to memory" )
	@Test
	public void testAiMemoryIngestBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory( "windowed" );
				result = aiMemoryIngest(
					memory = memory,
					source = "%s/sample.txt",
					type   = "text"
				);
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.containsKey( Key.of( "documentsIn" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "chunksOut" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "stored" ) ) ).isTrue();
		assertThat( result.getAsInteger( Key.of( "documentsIn" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "aiMemoryIngest() returns detailed report" )
	@Test
	public void testAiMemoryIngestReport() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory( "windowed" );
				result = aiMemoryIngest(
					memory = memory,
					source = "%s/sample.txt",
					type   = "text"
				);
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.containsKey( Key.of( "documentsIn" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "chunksOut" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "stored" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "skipped" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "deduped" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "tokenCount" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "embeddingCalls" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "estimatedCost" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "errors" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "memorySummary" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "duration" ) ) ).isTrue();
	}

	@DisplayName( "aiMemoryIngest() can ingest with chunking" )
	@Test
	public void testAiMemoryIngestWithChunking() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory( "windowed" );
				result = aiMemoryIngest(
					memory        = memory,
					source        = "%s/sample.txt",
					type          = "text",
					ingestOptions = { chunkSize: 50, overlap: 10 }
				);
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.getAsInteger( Key.of( "documentsIn" ) ) ).isEqualTo( 1 );
		// Chunking should produce more chunks than input documents
		assertThat( result.getAsInteger( Key.of( "chunksOut" ) ) ).isGreaterThan( 1 );
	}

	@DisplayName( "aiMemoryIngest() can ingest directory to memory" )
	@Test
	public void testAiMemoryIngestDirectory() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory( "windowed" );
				result = aiMemoryIngest(
					memory       = memory,
					source       = "%s",
					type         = "directory",
					loaderConfig = { extensions: ["txt"] }
				);
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.getAsInteger( Key.of( "documentsIn" ) ) ).isGreaterThan( 0 );
	}

	@DisplayName( "aiMemoryIngest() can fan-out to multiple memories" )
	@Test
	public void testAiMemoryIngestMultiMemory() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory1 = aiMemory( "windowed" );
				memory2 = aiMemory( "windowed" );
				result = aiMemoryIngest(
					memory = [ memory1, memory2 ],
					source = "%s/sample.txt",
					type   = "text"
				);
				memory1Count = memory1.count();
				memory2Count = memory2.count();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();

		// Both memories should have data
		Integer count1 = ( Integer ) variables.get( "memory1Count" );
		Integer count2 = ( Integer ) variables.get( "memory2Count" );
		assertThat( count1 ).isGreaterThan( 0 );
		assertThat( count2 ).isGreaterThan( 0 );

		// Memory summary should be an array for multi-memory
		Array summaries = ( Array ) result.get( Key.of( "memorySummary" ) );
		assertThat( summaries.size() ).isEqualTo( 2 );
	}

	// ===========================================
	// Backward Compatibility Tests
	// ===========================================

	@DisplayName( "aiLoad() still works (backward compatibility)" )
	@Test
	public void testAiLoadBackwardCompatibility() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiLoad( "%s/sample.txt" );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );
	}

}
