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
				result = loader.load();
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
				docs = loader.load();
				result = docs[1].getMetadata();
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
				result = loader.load();
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
				loader.splitByHeaders( 2 );
				result = loader.load();
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
				docs = loader.load();
				result = docs[1].getContent();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		String content = ( String ) variables.get( "result" );
		assertThat( content ).doesNotContain( "System.out.println" );
	}

	// ===========================================
	// HTMLLoader Tests
	// ===========================================

	@DisplayName( "HTMLLoader can load an HTML file" )
	@Test
	public void testHTMLLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.HTMLLoader;
				loader = new HTMLLoader( source: "%s/sample.html" );
				result = loader.load();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );

		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "Welcome to the Sample Page" );
	}

	@DisplayName( "HTMLLoader removes scripts by default" )
	@Test
	public void testHTMLLoaderRemovesScripts() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.HTMLLoader;
				loader = new HTMLLoader( source: "%s/sample.html" );
				docs = loader.load();
				result = docs[1].getContent();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		String content = ( String ) variables.get( "result" );
		assertThat( content ).doesNotContain( "console.log" );
	}

	@DisplayName( "HTMLLoader extracts title and description metadata" )
	@Test
	public void testHTMLLoaderMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.HTMLLoader;
				loader = new HTMLLoader( source: "%s/sample.html" );
				docs = loader.load();
				result = docs[1].getMetadata();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct metadata = ( IStruct ) variables.get( "result" );
		assertThat( metadata.containsKey( Key.of( "title" ) ) ).isTrue();
		assertThat( metadata.getAsString( Key.of( "title" ) ) ).isEqualTo( "Sample HTML Document" );
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
				result = loader.load();
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
				result = loader.load();
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
				result = loader.load();
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
				docs = loader.load();
				result = docs[1].getContent();
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
				result = loader.load();
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
				result = loader.load();
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
				result = loader.load();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );
	}

	// ===========================================
	// aiLoad BIF Tests
	// ===========================================

	@DisplayName( "aiLoad BIF can load a text file" )
	@Test
	public void testAiLoadText() {
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

	@DisplayName( "aiLoad BIF auto-detects loader type from extension" )
	@Test
	public void testAiLoadAutoDetect() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiLoad( "%s/sample.md" );
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

	@DisplayName( "aiLoad BIF can specify loader type explicitly" )
	@Test
	public void testAiLoadExplicitType() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiLoad( source: "%s/sample.txt", type: "text" );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );
	}

	@DisplayName( "aiLoad BIF can load a directory" )
	@Test
	public void testAiLoadDirectory() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiLoad( "%s" );
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
				memory = aiMemory( "windowed" );
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
				memory = aiMemory( "windowed" );
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

}
