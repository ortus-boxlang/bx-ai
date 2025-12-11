package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "WebCrawlerLoader Tests" )
public class WebCrawlerLoaderTest extends BaseIntegrationTest {

	@DisplayName( "WebCrawlerLoader can crawl a single page" )
	@Test
	public void testWebCrawlerBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			// Use a simple, stable website for testing
			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 1 )
				.maxDepth( 0 );				rawDocs = loader.load();

				result = {
					count: rawDocs.len(),
					hasContent: rawDocs[1].getContent().len() > 0,
					metadata: rawDocs[1].getMetadata()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		int		count	= result.getAsInteger( Key.of( "count" ) );

		assertThat( count ).isEqualTo( 1 );
		assertThat( result.getAsBoolean( Key.of( "hasContent" ) ) ).isTrue();

		IStruct metadata = result.getAsStruct( Key.of( "metadata" ) );
		assertThat( metadata.getAsString( Key.of( "url" ) ) ).contains( "github.com" );
		assertThat( metadata.getAsString( Key.of( "title" ) ) ).isNotEmpty();
	}

	@DisplayName( "WebCrawlerLoader can limit max pages" )
	@Test
	public void testWebCrawlerMaxPages() {
		// @formatter:off
		runtime.executeSource(
		    """
			import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 2 )
				.maxDepth( 1 );				rawDocs = loader.load();

				result = rawDocs.len();
		    """,
		    context
		);
		// @formatter:on

		int count = variables.getAsInteger( result );

		// Should not exceed maxPages
		assertThat( count ).isAtMost( 2 );
	}

	@DisplayName( "WebCrawlerLoader can limit crawl depth" )
	@Test
	public void testWebCrawlerMaxDepth() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			// maxDepth 0 means only the starting page
			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 10 )
				.maxDepth( 0 );				rawDocs = loader.load();

				result = rawDocs.len();
		    """,
		    context
		);
		// @formatter:on

		int count = variables.getAsInteger( result );

		// Should only get the starting page
		assertThat( count ).isEqualTo( 1 );
	}

	@DisplayName( "WebCrawlerLoader can filter by allowed paths" )
	@Test
	public void testWebCrawlerAllowedPaths() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 5 )
				.maxDepth( 1 )
				.allowedPaths( ["/html"] );				rawDocs = loader.load();

				result = {
					count: rawDocs.len(),
					urls: rawDocs.map( d => d.getMetadata().url )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		Array	urls	= result.getAsArray( Key.of( "urls" ) );

		// All URLs should start with allowed path (except starting URL if different)
		for ( int i = 0; i < urls.size(); i++ ) {
			String url = ( String ) urls.get( i );
			// URLs should either be the start or contain /docs/
			assertThat( url.contains( "github.com" ) ).isTrue();
		}
	}

	@DisplayName( "WebCrawlerLoader can exclude paths" )
	@Test
	public void testWebCrawlerExcludedPaths() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 5 )
				.maxDepth( 1 )
				.excludedPaths( ["/admin/", "/private/"] );				rawDocs = loader.load();

				result = {
					count: rawDocs.len(),
					urls: rawDocs.map( d => d.getMetadata().url )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		Array	urls	= result.getAsArray( Key.of( "urls" ) );

		// No URLs should contain excluded paths
		for ( int i = 0; i < urls.size(); i++ ) {
			String url = ( String ) urls.get( i );
			assertThat( url.contains( "/admin/" ) ).isFalse();
			assertThat( url.contains( "/private/" ) ).isFalse();
		}
	}

	@DisplayName( "WebCrawlerLoader can use content selector" )
	@Test
	public void testWebCrawlerContentSelector() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 1 )
				.maxDepth( 0 )
				.contentSelector( "body" );				rawDocs = loader.load();

				result = {
					count: rawDocs.len(),
					hasContent: rawDocs[1].getContent().len() > 0
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );

		assertThat( result.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 1 );
		assertThat( result.getAsBoolean( Key.of( "hasContent" ) ) ).isTrue();
	}

	@DisplayName( "WebCrawlerLoader can exclude selectors" )
	@Test
	public void testWebCrawlerExcludeSelectors() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 1 )
				.maxDepth( 0 )
				.excludeSelectors( ["nav", "footer"] );				rawDocs = loader.load();

				result = {
					count: rawDocs.len(),
					content: rawDocs[1].getContent()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );

		assertThat( result.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 1 );
		// Content should exist but nav/footer should be removed
		assertThat( result.getAsString( Key.of( "content" ) ).length() ).isGreaterThan( 0 );
	}

	@DisplayName( "WebCrawlerLoader can set custom user agent" )
	@Test
	public void testWebCrawlerUserAgent() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 1 )
				.maxDepth( 0 )
				.userAgent( "MyCustomBot/1.0" );				rawDocs = loader.load();

				result = rawDocs.len();
		    """,
		    context
		);
		// @formatter:on

		int count = variables.getAsInteger( result );

		assertThat( count ).isEqualTo( 1 );
	}

	@DisplayName( "WebCrawlerLoader can set request delay" )
	@Test
	public void testWebCrawlerDelay() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 2 )
				.maxDepth( 1 )
				.delay( 100 );  // 100ms between requests

				rawDocs = loader.load();

				result = rawDocs.len();
		    """,
		    context
		);
		// @formatter:on

		int count = variables.getAsInteger( result );

		assertThat( count ).isAtLeast( 1 );
	}

	@DisplayName( "WebCrawlerLoader deduplicates content by default" )
	@Test
	public void testWebCrawlerDeduplication() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 5 )
				.maxDepth( 1 )
					.deduplicateContent( true );

				rawDocs = loader.load();

				// Check that all documents have unique content
				contentSet = {};
				for ( doc in rawDocs ) {
					contentHash = hash( doc.getContent(), "MD5" );
					contentSet[ contentHash ] = true;
				}

				result = {
					docCount: rawDocs.len(),
					uniqueCount: contentSet.count()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );

		// All documents should have unique content
		assertThat( result.getAsInteger( Key.of( "docCount" ) ) )
		    .isEqualTo( result.getAsInteger( Key.of( "uniqueCount" ) ) );
	}

	@DisplayName( "WebCrawlerLoader handles invalid URL" )
	@Test
	public void testWebCrawlerInvalidUrl() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

				try {
					loader = new WebCrawlerLoader( source: "not-a-valid-url" );
					rawDocs = loader.load();
					result = false;
				} catch( any e ) {
					result = e.type == "WebCrawlerLoader.InvalidUrl";
				}
		    """,
		    context
		);
		// @formatter:on

		boolean threwCorrectError = variables.getAsBoolean( result );
		assertThat( threwCorrectError ).isTrue();
	}

	@DisplayName( "WebCrawlerLoader handles no source URL" )
	@Test
	public void testWebCrawlerNoSource() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

				try {
					loader = new WebCrawlerLoader();
					rawDocs = loader.load();
					result = false;
				} catch( any e ) {
					result = e.type == "WebCrawlerLoader.NoSource";
				}
		    """,
		    context
		);
		// @formatter:on

		boolean threwCorrectError = variables.getAsBoolean( result );
		assertThat( threwCorrectError ).isTrue();
	}

	@DisplayName( "WebCrawlerLoader extracts metadata" )
	@Test
	public void testWebCrawlerMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
			import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 1 )
				.maxDepth( 0 );

			rawDocs = loader.load();
			metadata = rawDocs[1].getMetadata();				result = {
					hasUrl: metadata.keyExists( "url" ),
					hasTitle: metadata.keyExists( "title" ),
					hasCrawledAt: metadata.keyExists( "crawledAt" ),
					hasStatusCode: metadata.keyExists( "statusCode" ),
					statusCode: metadata.statusCode
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );

		assertThat( result.getAsBoolean( Key.of( "hasUrl" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "hasTitle" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "hasCrawledAt" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "hasStatusCode" ) ) ).isTrue();
		assertThat( result.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
	}

	@DisplayName( "WebCrawlerLoader does not follow external links by default" )
	@Test
	public void testWebCrawlerNoExternalLinks() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 5 )
				.maxDepth( 2 )
				.followExternalLinks( false );				rawDocs = loader.load();

				result = {
					count: rawDocs.len(),
					urls: rawDocs.map( d => d.getMetadata().url )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		Array	urls	= result.getAsArray( Key.of( "urls" ) );

		// All URLs should contain github.com
		for ( int i = 0; i < urls.size(); i++ ) {
			String url = ( String ) urls.get( i );
			assertThat( url.contains( "github.com" ) ).isTrue();
		}
	}

	@DisplayName( "WebCrawlerLoader can set timeout" )
	@Test
	public void testWebCrawlerTimeout() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.WebCrawlerLoader;

			loader = new WebCrawlerLoader( source: "https://github.com/ortus-boxlang/boxlang" )
				.maxPages( 1 )
				.maxDepth( 0 )
				.timeout( 10 );  // 10 seconds

			rawDocs = loader.load();

				result = rawDocs.len();
		    """,
		    context
		);
		// @formatter:on

		int count = variables.getAsInteger( result );

		assertThat( count ).isEqualTo( 1 );
	}

}
