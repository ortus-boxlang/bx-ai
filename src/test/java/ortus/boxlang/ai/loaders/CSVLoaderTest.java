package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "CSVLoader Tests" )
public class CSVLoaderTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

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

}
