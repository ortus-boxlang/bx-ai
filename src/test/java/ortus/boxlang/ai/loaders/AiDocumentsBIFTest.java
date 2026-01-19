package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "aiDocuments BIF Tests" )
public class AiDocumentsBIFTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

	@DisplayName( "aiDocuments BIF returns a loader for a text file" )
	@Test
	public void testAiDocumentsText() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocuments( "%s/sample.txt" );
				result = loader.load();
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
				rawDocs = aiDocuments( "%s/sample.md" ).load();
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

	@DisplayName( "aiDocuments BIF can specify loader type via config" )
	@Test
	public void testAiDocumentsExplicitType() {
		// @formatter:off
		runtime.executeSource(
		    """
				rawDocs = aiDocuments( "%s/sample.txt", { type: "text" } ).load();
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
				result = aiDocuments( "%s" ).load();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		// Should load multiple files
		assertThat( docs.size() ).isGreaterThan( 1 );
	}

	@DisplayName( "aiDocuments BIF supports fluent chaining with toMemory" )
	@Test
	public void testAiDocumentsToMemory() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory( "WindowMemory" );
				result = aiDocuments( "%s/sample.txt" ).toMemory( memory );
				messageCount = memory.count();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct report = ( IStruct ) variables.get( "result" );
		assertThat( report ).isNotNull();
		assertThat( report.containsKey( Key.of( "documentsIn" ) ) ).isTrue();
		assertThat( report.containsKey( Key.of( "stored" ) ) ).isTrue();

		Integer count = ( Integer ) variables.get( "messageCount" );
		assertThat( count ).isGreaterThan( 0 );
	}

	@DisplayName( "aiDocuments BIF supports filter method" )
	@Test
	public void testAiDocumentsFilter() {
		// @formatter:off
		runtime.executeSource(
		    """
				docs = aiDocuments( "%s" )
					.filter( ( doc ) => doc.hasContent() )
					.load();
				result = docs.len();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Integer count = ( Integer ) variables.get( "result" );
		assertThat( count ).isGreaterThan( 0 );
	}

	@DisplayName( "aiDocuments BIF supports map method" )
	@Test
	public void testAiDocumentsMap() {
		// @formatter:off
		runtime.executeSource(
		    """
				docs = aiDocuments( "%s/sample.txt" )
					.map( ( doc ) => {
						doc.setMeta( "processed", true );
						return doc;
					} )
					.load();
				result = docs[1].getMeta( "processed", false );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Object processed = variables.get( "result" );
		assertThat( processed ).isEqualTo( true );
	}

	@DisplayName( "aiDocuments auto-detects HTTP type for URLs" )
	@Test
	public void testAiDocumentsHTTPDetection() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocuments( "https://example.com/page.html" );
				result = loader.getName();
		    """,
		    context
		);
		// @formatter:on

		String name = ( String ) variables.get( "result" );
		assertThat( name ).isEqualTo( "HTTPLoader" );
	}

	@DisplayName( "aiDocuments can create explicit HTTP loader" )
	@Test
	public void testAiDocumentsExplicitHTTP() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocuments( "https://api.example.com/data", { type: "http" } );
				result = loader.getName();
		    """,
		    context
		);
		// @formatter:on

		String name = ( String ) variables.get( "result" );
		assertThat( name ).isEqualTo( "HTTPLoader" );
	}

}
