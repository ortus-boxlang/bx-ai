package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "TextLoader Tests" )
public class TextLoaderTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

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
		assertThat( metadata.getAsString( Key.of( "type" ) ) ).isEqualTo( "file" );
		assertThat( metadata.containsKey( Key.of( "name" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "size" ) ) ).isTrue();
	}

}
