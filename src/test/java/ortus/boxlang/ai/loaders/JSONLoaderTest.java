package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "JSONLoader Tests" )
public class JSONLoaderTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

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

}
