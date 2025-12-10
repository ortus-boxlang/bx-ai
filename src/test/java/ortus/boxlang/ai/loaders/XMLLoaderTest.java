package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "XMLLoader Tests" )
public class XMLLoaderTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

	@DisplayName( "XMLLoader can load an XML file" )
	@Test
	public void testXMLLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.XMLLoader;
				loader = new XMLLoader( source: "%s/sample.xml" );
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
		assertThat( content ).contains( "Sample Library" );
		assertThat( content ).contains( "The Great Adventure" );
	}

	@DisplayName( "XMLLoader can extract elements as separate documents" )
	@Test
	public void testXMLLoaderElementsAsDocuments() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.XMLLoader;
				loader = new XMLLoader(
					source: "%s/sample.xml",
					config: {
						elementsAsDocuments: true,
						elementPath: "//book"
					}
				);
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 3 ); // 3 books in sample.xml

		IStruct	firstDoc	= ( IStruct ) docs.get( 0 );
		String	content		= firstDoc.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "The Great Adventure" );
		assertThat( content ).contains( "Jane Smith" );
	}

	@DisplayName( "XMLLoader includes metadata when configured" )
	@Test
	public void testXMLLoaderMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.XMLLoader;
				loader = new XMLLoader(
					source: "%s/sample.xml",
					config: { includeMetadata: true }
				);
				docs = loader.load();
				metadata = docs[1].getMetadata();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct metadata = ( IStruct ) variables.get( "metadata" );
		assertThat( metadata ).isNotNull();
		assertThat( metadata.containsKey( Key.of( "source" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "loader" ) ) ).isTrue();
		assertThat( metadata.getAsString( Key.of( "loader" ) ) ).isEqualTo( "XMLLoader" );
	}

}
