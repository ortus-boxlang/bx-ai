package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "HTTPLoader Tests" )
public class HTTPLoaderTest extends BaseIntegrationTest {

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

}
