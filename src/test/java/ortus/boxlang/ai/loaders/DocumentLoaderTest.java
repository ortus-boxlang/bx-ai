package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Document Loaders Tests" )
public class DocumentLoaderTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

	// ===========================================
	// TextLoader Tests
	// ===========================================

	@DisplayName( "TextLoader can load a plain text file" )
	@Test
	public void testTextLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );

		IStruct doc = ( IStruct ) docs.get( 0 );
		assertThat( doc.getAsString( Key.of( "content" ) ) ).contains( "sample text file" );
	}

	@DisplayName( "TextLoader includes file metadata" )
	@Test
	public void testTextLoaderMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				rawDocs = loader.load();
				result = rawDocs[1].getMetadata();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct metadata = ( IStruct ) variables.get( "result" );
		assertThat( metadata ).isNotNull();
		assertThat( metadata.containsKey( Key.of( "source" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "loader" ) ) ).isTrue();
		assertThat( metadata.getAsString( Key.of( "fileType" ) ) ).isEqualTo( "text" );
	}

	// ===========================================
	// MarkdownLoader Tests
	// ===========================================

	@DisplayName( "MarkdownLoader can load a markdown file" )
	@Test
	public void testMarkdownLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.MarkdownLoader;
				loader = new MarkdownLoader( source: "%s/sample.md" );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );

		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "Sample Markdown Document" );
	}

	@DisplayName( "MarkdownLoader can split by headers" )
	@Test
	public void testMarkdownLoaderSplitByHeaders() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.MarkdownLoader;
				loader = new MarkdownLoader( source: "%s/sample.md" );
				loader.headerSplit( 2 );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		// Should have multiple sections based on headers
		assertThat( docs.size() ).isGreaterThan( 1 );
	}

	@DisplayName( "MarkdownLoader can remove code blocks" )
	@Test
	public void testMarkdownLoaderRemoveCodeBlocks() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.MarkdownLoader;
				loader = new MarkdownLoader( source: "%s/sample.md" );
				loader.removeCodeBlocks();
				rawDocs = loader.load();
				result = rawDocs[1].getContent();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		String content = ( String ) variables.get( "result" );
		assertThat( content ).doesNotContain( "System.out.println" );
	}

	// ===========================================
	// CSVLoader Tests
	// ===========================================

	@DisplayName( "CSVLoader can load a CSV file" )
	@Test
	public void testCSVLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.CSVLoader;
				loader = new CSVLoader( source: "%s/sample.csv" );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );

		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "John Doe" );
	}

	@DisplayName( "CSVLoader can create row documents" )
	@Test
	public void testCSVLoaderRowDocuments() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.CSVLoader;
				loader = new CSVLoader( source: "%s/sample.csv" );
				loader.rowsAsDocuments();
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		// Should have 3 rows (excluding header)
		assertThat( docs.size() ).isEqualTo( 3 );
	}

	// ===========================================
	// JSONLoader Tests
	// ===========================================

	@DisplayName( "JSONLoader can load a JSON file" )
	@Test
	public void testJSONLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.JSONLoader;
				loader = new JSONLoader( source: "%s/sample.json" );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );

		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "Sample JSON Document" );
	}

	@DisplayName( "JSONLoader can extract content from specific field" )
	@Test
	public void testJSONLoaderContentField() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.JSONLoader;
				loader = new JSONLoader( source: "%s/sample.json" );
				loader.contentField( "content" );
				rawDocs = loader.load();
				result = rawDocs[1].getContent();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		String content = ( String ) variables.get( "result" );
		assertThat( content ).isEqualTo( "This is the main content of the document." );
	}

	@DisplayName( "JSONLoader can create documents from array" )
	@Test
	public void testJSONLoaderArrayAsDocuments() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.JSONLoader;
				loader = new JSONLoader( source: "%s/array.json" );
				loader.arrayAsDocuments();
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 3 );
	}

	// ===========================================
	// DirectoryLoader Tests
	// ===========================================

	@DisplayName( "DirectoryLoader can load all files from a directory" )
	@Test
	public void testDirectoryLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		// Should load multiple files from the test directory
		assertThat( docs.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "DirectoryLoader can filter by extension" )
	@Test
	public void testDirectoryLoaderExtensionFilter() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" );
				loader.extensions( ["txt"] );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );
	}

	// ===========================================
	// aiDocuments BIF Tests
	// ===========================================

	@DisplayName( "aiDocuments BIF can load a text file" )
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

	@DisplayName( "aiDocuments BIF auto-detects loader type from extension" )
	@Test
	public void testAiDocumentsAutoDetect() {
		// @formatter:off
		runtime.executeSource(
		    """
				rawDocs = aiDocuments( "%s/sample.md" );
				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isGreaterThan( 0 );

		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "Sample Markdown Document" );
	}

	@DisplayName( "aiDocuments BIF can specify loader type explicitly" )
	@Test
	public void testAiDocumentsExplicitType() {
		// @formatter:off
		runtime.executeSource(
		    """
				rawDocs = aiDocuments( source: "%s/sample.txt", type: "text" );
				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );
	}

	@DisplayName( "aiDocuments BIF can load a directory" )
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
		// Should load multiple files
		assertThat( docs.size() ).isGreaterThan( 1 );
	}

	// ===========================================
	// Document Class Tests
	// ===========================================

	@DisplayName( "Document class can be created and manipulated" )
	@Test
	public void testDocumentClass() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;
				doc = new Document( content: "Test content", metadata: { source: "test" } );
				result = {
					content: doc.getContent(),
					hasContent: doc.hasContent(),
					length: doc.getContentLength(),
					source: doc.getMeta( "source" )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "content" ) ) ).isEqualTo( "Test content" );
		assertThat( result.get( Key.of( "hasContent" ) ) ).isEqualTo( true );
		assertThat( result.getAsInteger( Key.of( "length" ) ) ).isEqualTo( 12 );
		assertThat( result.getAsString( Key.of( "source" ) ) ).isEqualTo( "test" );
	}

	@DisplayName( "Document can be serialized to JSON and back" )
	@Test
	public void testDocumentSerialization() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;
				original = new Document( content: "Test", metadata: { key: "value" } );
				json = original.toJson();
				restored = Document::fromJson( json );
				result = {
					content: restored.getContent(),
					key: restored.getMeta( "key" )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "content" ) ) ).isEqualTo( "Test" );
		assertThat( result.getAsString( Key.of( "key" ) ) ).isEqualTo( "value" );
	}

	// ===========================================
	// Integration with Memory
	// ===========================================

	@DisplayName( "Loader can store documents to memory" )
	@Test
	public void testLoaderToMemory() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				memory = aiMemory( "WindowMemory" );
				result = loader.loadTo( memory );
				messageCount = memory.count();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isGreaterThan( 0 );

		Integer count = ( Integer ) variables.get( "messageCount" );
		assertThat( count ).isGreaterThan( 0 );
	}

	@DisplayName( "Loader can chunk documents before storing" )
	@Test
	public void testLoaderToMemoryWithChunking() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				memory = aiMemory( "WindowMemory" );
				result = loader.loadTo(
					memory,
					{ chunkSize: 50, overlap: 10 }
				);
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		// Should have multiple chunks
		assertThat( docs.size() ).isGreaterThan( 1 );
	}

	// ===========================================
	// HTTPLoader Tests
	// ===========================================

	@DisplayName( "HTTPLoader can be created with URL source" )
	@Test
	public void testHTTPLoaderCreation() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.HTTPLoader;
				loader = new HTTPLoader( source: "https://example.com" );
				result = {
					name: loader.getName(),
					source: loader.getSource()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "name" ) ) ).isEqualTo( "HTTPLoader" );
		assertThat( result.getAsString( Key.of( "source" ) ) ).isEqualTo( "https://example.com" );
	}

	@DisplayName( "HTTPLoader supports fluent configuration" )
	@Test
	public void testHTTPLoaderFluentConfig() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.HTTPLoader;
				loader = new HTTPLoader( source: "https://example.com" )
					.contentType( "html" )
					.timeout( 60 )
					.method( "GET" )
					.header( "Accept", "text/html" );
				result = loader.getConfig();
		    """,
		    context
		);
		// @formatter:on

		IStruct config = ( IStruct ) variables.get( "result" );
		assertThat( config.getAsString( Key.of( "contentType" ) ) ).isEqualTo( "html" );
		assertThat( config.getAsInteger( Key.of( "timeout" ) ) ).isEqualTo( 60 );
		assertThat( config.getAsString( Key.of( "method" ) ) ).isEqualTo( "GET" );
	}

	@DisplayName( "HTTPLoader can get source metadata" )
	@Test
	public void testHTTPLoaderSourceMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.HTTPLoader;
				loader = new HTTPLoader( source: "https://example.com/page.html" );
				result = loader.getSourceMetadata();
		    """,
		    context
		);
		// @formatter:on

		IStruct metadata = ( IStruct ) variables.get( "result" );
		assertThat( metadata.getAsBoolean( Key.of( "isURL" ) ) ).isTrue();
		assertThat( metadata.getAsString( Key.of( "source" ) ) ).isEqualTo( "https://example.com/page.html" );
	}

	// ===========================================
	// TikaLoader Tests
	// ===========================================

	@DisplayName( "TikaLoader can be created with file source" )
	@Test
	public void testTikaLoaderCreation() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TikaLoader;
				loader = new TikaLoader( source: "/path/to/document.pdf" );
				result = {
					name: loader.getName(),
					source: loader.getSource()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "name" ) ) ).isEqualTo( "TikaLoader" );
		assertThat( result.getAsString( Key.of( "source" ) ) ).isEqualTo( "/path/to/document.pdf" );
	}

	@DisplayName( "TikaLoader supports fluent configuration" )
	@Test
	public void testTikaLoaderFluentConfig() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TikaLoader;
				loader = new TikaLoader( source: "/path/to/doc.pdf" )
					.maxLength( 10000 )
					.enableOCR( false )
					.password( "secret" );
				result = loader.getConfig();
		    """,
		    context
		);
		// @formatter:on

		IStruct config = ( IStruct ) variables.get( "result" );
		assertThat( config.getAsInteger( Key.of( "maxLength" ) ) ).isEqualTo( 10000 );
		assertThat( config.getAsBoolean( Key.of( "ocrEnabled" ) ) ).isFalse();
		assertThat( config.getAsString( Key.of( "passwordProtected" ) ) ).isEqualTo( "secret" );
	}

	@DisplayName( "TikaLoader can check supported extensions" )
	@Test
	public void testTikaLoaderSupportedExtensions() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TikaLoader;
				result = {
					pdfSupported: TikaLoader::isSupported( "/path/file.pdf" ),
					docxSupported: TikaLoader::isSupported( "/path/file.docx" ),
					txtNotSupported: TikaLoader::isSupported( "/path/file.txt" ),
					extensions: TikaLoader::getSupportedExtensions()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsBoolean( Key.of( "pdfSupported" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "docxSupported" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "txtNotSupported" ) ) ).isFalse();

		Array extensions = result.getAsArray( Key.of( "extensions" ) );
		assertThat( extensions ).isNotNull();
		assertThat( extensions.size() ).isGreaterThan( 5 );
	}

	// ===========================================
	// aiDocumentLoader BIF Tests for New Loaders
	// ===========================================

	@DisplayName( "aiDocumentLoader auto-detects HTTP type for URLs" )
	@Test
	public void testAiDocumentLoaderHTTPDetection() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocumentLoader( "https://example.com/page.html" );
				result = loader.getName();
		    """,
		    context
		);
		// @formatter:on

		String name = ( String ) variables.get( "result" );
		assertThat( name ).isEqualTo( "HTTPLoader" );
	}

	@DisplayName( "aiDocumentLoader auto-detects Tika type for PDF" )
	@Test
	public void testAiDocumentLoaderTikaDetection() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocumentLoader( "/path/to/file.pdf" );
				result = loader.getName();
		    """,
		    context
		);
		// @formatter:on

		String name = ( String ) variables.get( "result" );
		assertThat( name ).isEqualTo( "TikaLoader" );
	}

	@DisplayName( "aiDocumentLoader can create explicit HTTP loader" )
	@Test
	public void testAiDocumentLoaderExplicitHTTP() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocumentLoader( source: "https://api.example.com/data", type: "http" );
				result = loader.getName();
		    """,
		    context
		);
		// @formatter:on

		String name = ( String ) variables.get( "result" );
		assertThat( name ).isEqualTo( "HTTPLoader" );
	}

	@DisplayName( "aiDocumentLoader can create explicit Tika loader" )
	@Test
	public void testAiDocumentLoaderExplicitTika() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocumentLoader( source: "/path/to/file.doc", type: "tika" );
				result = loader.getName();
		    """,
		    context
		);
		// @formatter:on

		String name = ( String ) variables.get( "result" );
		assertThat( name ).isEqualTo( "TikaLoader" );
	}

	// ===========================================
	// aiDocumentLoaders BIF Tests for New Loaders
	// ===========================================

	@DisplayName( "aiDocumentLoaders includes HTTP and Tika loaders" )
	@Test
	public void testAiDocumentLoadersIncludesNewLoaders() {
		// @formatter:off
		runtime.executeSource(
		    """
				loaders = aiDocumentLoaders();
				result = {
					hasHTTP: loaders.keyExists( "http" ),
					hasTika: loaders.keyExists( "tika" ),
					httpName: loaders.http.name,
					tikaName: loaders.tika.name,
					tikaExtensions: loaders.tika.extensions
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsBoolean( Key.of( "hasHTTP" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "hasTika" ) ) ).isTrue();
		assertThat( result.getAsString( Key.of( "httpName" ) ) ).isEqualTo( "HTTPLoader" );
		assertThat( result.getAsString( Key.of( "tikaName" ) ) ).isEqualTo( "TikaLoader" );

		Array tikaExtensions = result.getAsArray( Key.of( "tikaExtensions" ) );
		assertThat( tikaExtensions ).isNotNull();
		assertThat( tikaExtensions.size() ).isGreaterThan( 5 );
	}

	// ===========================================
	// Document Class Tests with New Properties
	// ===========================================

	@DisplayName( "Document has id and embedding properties" )
	@Test
	public void testDocumentIdAndEmbedding() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;
				doc = new Document(
					id: "doc-123",
					content: "Test content",
					metadata: { source: "test" },
					embedding: [0.1, 0.2, 0.3]
				);
				result = {
					id: doc.getId(),
					content: doc.getContent(),
					hasEmbedding: doc.hasEmbedding(),
					embedding: doc.getEmbedding()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "id" ) ) ).isEqualTo( "doc-123" );
		assertThat( result.getAsString( Key.of( "content" ) ) ).isEqualTo( "Test content" );
		assertThat( result.getAsBoolean( Key.of( "hasEmbedding" ) ) ).isTrue();

		Array embedding = result.getAsArray( Key.of( "embedding" ) );
		assertThat( embedding.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Document auto-generates id if not provided" )
	@Test
	public void testDocumentAutoGenerateId() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;
				doc1 = new Document( content: "Test 1" );
				doc2 = new Document( content: "Test 2" );
				result = {
					id1: doc1.getId(),
					id2: doc2.getId(),
					idsAreDifferent: doc1.getId() != doc2.getId()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "id1" ) ) ).isNotEmpty();
		assertThat( result.getAsString( Key.of( "id2" ) ) ).isNotEmpty();
		assertThat( result.getAsBoolean( Key.of( "idsAreDifferent" ) ) ).isTrue();
	}

	@DisplayName( "Document serialization includes id and embedding" )
	@Test
	public void testDocumentSerializationWithIdAndEmbedding() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.Document;
				original = new Document(
					id: "test-id",
					content: "Test",
					metadata: { key: "value" },
					embedding: [0.5, 0.6]
				);
				json = original.toJson();
				restored = Document::fromJson( json );
				result = {
					id: restored.getId(),
					content: restored.getContent(),
					embedding: restored.getEmbedding()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "id" ) ) ).isEqualTo( "test-id" );
		assertThat( result.getAsString( Key.of( "content" ) ) ).isEqualTo( "Test" );

		Array embedding = result.getAsArray( Key.of( "embedding" ) );
		assertThat( embedding.size() ).isEqualTo( 2 );
	}

	// ===========================================
	// IDocumentLoader Interface Tests
	// ===========================================

	@DisplayName( "Loader supports getSourceMetadata()" )
	@Test
	public void testLoaderGetSourceMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				result = loader.getSourceMetadata();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct metadata = ( IStruct ) variables.get( "result" );
		assertThat( metadata ).isNotNull();
		assertThat( metadata.containsKey( Key.of( "source" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "exists" ) ) ).isTrue();
		assertThat( metadata.getAsBoolean( Key.of( "isFile" ) ) ).isTrue();
	}

	@DisplayName( "Loader supports getDocumentCount()" )
	@Test
	public void testLoaderGetDocumentCount() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				beforeLoad = loader.getDocumentCount();
				// Use loadBatch which caches documents
				batch = loader.loadBatch( 100 );
				afterLoad = loader.getDocumentCount();
				result = { before: beforeLoad, after: afterLoad };
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		// Before load, count is -1 (unknown)
		assertThat( result.getAsInteger( Key.of( "before" ) ) ).isEqualTo( -1 );
		// After load via loadBatch, count is actual number
		assertThat( result.getAsInteger( Key.of( "after" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "Loader supports loadBatch()" )
	@Test
	public void testLoaderLoadBatch() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" );
				batch1 = loader.loadBatch( 2 );
				batch2 = loader.loadBatch( 2 );
				result = {
					batch1Size: batch1.len(),
					batch2Size: batch2.len()
				};
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		// First batch should have up to 2 documents
		assertThat( result.getAsInteger( Key.of( "batch1Size" ) ) ).isGreaterThan( 0 );
		assertThat( result.getAsInteger( Key.of( "batch1Size" ) ) ).isLessThan( 3 );
	}

	@DisplayName( "Loader supports loadAsStream()" )
	@Test
	public void testLoaderLoadAsStream() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" );
				stream = loader.loadAsStream();
				result = stream.count();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Long count = ( Long ) variables.get( "result" );
		assertThat( count ).isGreaterThan( 0 );
	}

	@DisplayName( "Loader supports getErrors()" )
	@Test
	public void testLoaderGetErrors() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				loader.load();
				result = loader.getErrors();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array errors = ( Array ) variables.get( "result" );
		assertThat( errors ).isNotNull();
		// No errors for valid file
		assertThat( errors.size() ).isEqualTo( 0 );
	}

}
