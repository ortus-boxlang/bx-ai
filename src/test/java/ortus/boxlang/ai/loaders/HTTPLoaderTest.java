package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
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

	@DisplayName( "HTTPLoader can load HTML from a real webpage" )
	@Test
	public void testHTTPLoaderLiveHTML() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.HTTPLoader;
				loader = new HTTPLoader( source: "https://www.boxlang.io" )
					.contentType( "html" )
					.extractText( true );
				rawDocs = loader.load();
				result = rawDocs.map( d => d.toStruct() );
		    """,
		    context
		);
		// @formatter:on

		Array	docs	= variables.getAsArray( result );
		IStruct	doc		= ( IStruct ) docs.getFirst();
		String	content	= doc.getAsString( Key.of( "content" ) );

		assertThat( docs.size() ).isEqualTo( 1 );
		assertThat( content ).isNotEmpty();
		assertThat( content ).containsMatch( "(?i)boxlang" ); // Case-insensitive match
		assertThat( content ).doesNotContain( "<html>" ); // Text extracted, no HTML tags
		assertThat( content ).doesNotContain( "<div>" );

		IStruct metadata = ( IStruct ) doc.get( Key.of( "metadata" ) );
		assertThat( metadata.getAsString( Key.of( "url" ) ) ).isEqualTo( "https://www.boxlang.io" );
		assertThat( metadata.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( metadata.getAsString( Key.of( "contentType" ) ) ).isEqualTo( "html" );
	}

	@DisplayName( "HTTPLoader can load plain text from a webpage" )
	@Test
	public void testHTTPLoaderLiveText() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.HTTPLoader;
				loader = new HTTPLoader( source: "https://www.ietf.org/rfc/rfc2616.txt" )
					.contentType( "text" )
					.timeout( 30 );
				rawDocs = loader.load();
				result = rawDocs.map( d => d.toStruct() );
		    """,
		    context
		);
		// @formatter:on

		Array	docs	= ( Array ) variables.get( "result" );
		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );

		assertThat( docs.size() ).isEqualTo( 1 );
		assertThat( content ).isNotEmpty();
		assertThat( content ).contains( "Hypertext Transfer Protocol" );
		assertThat( content ).contains( "HTTP/1.1" );

		IStruct metadata = ( IStruct ) doc.get( Key.of( "metadata" ) );
		assertThat( metadata.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( metadata.getAsString( Key.of( "contentType" ) ) ).isEqualTo( "text" );
	}

	@DisplayName( "HTTPLoader can load JSON from an API" )
	@Test
	public void testHTTPLoaderLiveJSON() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.HTTPLoader;
				loader = new HTTPLoader( source: "https://jsonplaceholder.typicode.com/posts/1" )
					.contentType( "json" );
				rawDocs = loader.load();
				result = rawDocs.map( d => d.toStruct() );
		    """,
		    context
		);
		// @formatter:on

		Array	docs	= ( Array ) variables.get( "result" );
		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );

		assertThat( docs.size() ).isEqualTo( 1 );
		assertThat( content ).isNotEmpty();
		assertThat( content ).contains( "userId" );
		assertThat( content ).contains( "title" );
		assertThat( content ).contains( "body" );

		IStruct metadata = ( IStruct ) doc.get( Key.of( "metadata" ) );
		assertThat( metadata.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( metadata.getAsString( Key.of( "contentType" ) ) ).isEqualTo( "json" );
	}

}
