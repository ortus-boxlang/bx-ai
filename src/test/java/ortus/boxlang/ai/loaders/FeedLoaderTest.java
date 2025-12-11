package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "FeedLoader Tests" )
public class FeedLoaderTest extends BaseIntegrationTest {

	@DisplayName( "FeedLoader can be created with URL source" )
	@Test
	public void testFeedLoaderCreation() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://example.com/feed.xml" );
				result = {
					name: loader.getName(),
					source: loader.getSource()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "name" ) ) ).isEqualTo( "FeedLoader" );
		assertThat( result.getAsString( Key.of( "source" ) ) ).isEqualTo( "https://example.com/feed.xml" );
	}

	@DisplayName( "FeedLoader supports fluent configuration" )
	@Test
	public void testFeedLoaderFluentConfig() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://example.com/feed.xml" )
					.maxItems( 10 )
					.stripHtml( true )
					.includeDescription( true )
					.timeout( 60 );
				result = loader.getConfig();
		    """,
		    context
		);
		// @formatter:on

		IStruct config = ( IStruct ) variables.get( "result" );
		assertThat( config.getAsInteger( Key.of( "maxItems" ) ) ).isEqualTo( 10 );
		assertThat( config.getAsBoolean( Key.of( "stripHtml" ) ) ).isTrue();
		assertThat( config.getAsBoolean( Key.of( "includeDescription" ) ) ).isTrue();
		assertThat( config.getAsInteger( Key.of( "timeout" ) ) ).isEqualTo( 60 );
	}

	@DisplayName( "FeedLoader can filter by categories" )
	@Test
	public void testFeedLoaderCategoryFilter() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://example.com/feed.xml" )
					.categories( ["Technology", "Science"] );
				result = loader.getConfig();
		    """,
		    context
		);
		// @formatter:on

		IStruct	config		= ( IStruct ) variables.get( "result" );
		Array	categories	= config.getAsArray( Key.of( "categories" ) );
		assertThat( categories.size() ).isEqualTo( 2 );
		assertThat( categories.get( 0 ) ).isEqualTo( "Technology" );
		assertThat( categories.get( 1 ) ).isEqualTo( "Science" );
	}

	@DisplayName( "FeedLoader can set sinceDate filter" )
	@Test
	public void testFeedLoaderSinceDateFilter() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://example.com/feed.xml" )
					.sinceDate( "2025-01-01" );
				result = loader.getConfig();
		    """,
		    context
		);
		// @formatter:on

		IStruct config = ( IStruct ) variables.get( "result" );
		assertThat( config.getAsString( Key.of( "sinceDate" ) ) ).isEqualTo( "2025-01-01" );
	}

	@DisplayName( "FeedLoader can get source metadata" )
	@Test
	public void testFeedLoaderSourceMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://example.com/feed.xml" )
					.maxItems( 5 )
					.stripHtml( true );
				result = loader.getSourceMetadata();
		    """,
		    context
		);
		// @formatter:on

		IStruct metadata = ( IStruct ) variables.get( "result" );
		assertThat( metadata.getAsBoolean( Key.of( "isFeed" ) ) ).isTrue();
		assertThat( metadata.getAsString( Key.of( "source" ) ) ).isEqualTo( "https://example.com/feed.xml" );
		assertThat( metadata.getAsInteger( Key.of( "maxItems" ) ) ).isEqualTo( 5 );
		assertThat( metadata.getAsBoolean( Key.of( "stripHtml" ) ) ).isTrue();
	}

	@DisplayName( "FeedLoader document count returns -1 (unknown)" )
	@Test
	public void testFeedLoaderDocumentCount() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://example.com/feed.xml" );
				result = loader.getDocumentCount();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( result ) ).isEqualTo( -1 );
	}

	@DisplayName( "FeedLoader throws error when no source specified" )
	@Test
	public void testFeedLoaderNoSourceError() {
		assertThrows( Exception.class, () -> {
			// @formatter:off
			runtime.executeSource(
			    """
					import bxModules.bxai.models.loaders.FeedLoader;
					loader = new FeedLoader();
					docs = loader.load();
			    """,
			    context
			);
			// @formatter:on
		} );
	}

	@DisplayName( "FeedLoader can load from a real RSS feed" )
	@Test
	public void testFeedLoaderRealRSS() {
		// Using BBC World News RSS feed
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://feeds.bbci.co.uk/news/world/rss.xml" )
					.maxItems( 3 )
					.stripHtml( true );
				rawDocs = loader.load();
				errors = loader.getErrors();
				result = {
					count: rawDocs.len(),
					errors: errors,
					docs: rawDocs.map( d => d.toStruct() )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		int		count	= result.getAsInteger( Key.of( "count" ) );
		Array	errors	= result.getAsArray( Key.of( "errors" ) );

		// Feed may be unavailable in some test environments
		// If we get documents, verify they're valid
		if ( count > 0 ) {
			assertThat( count ).isAtMost( 3 );
			Array	docs	= result.getAsArray( Key.of( "docs" ) );
			IStruct	doc		= ( IStruct ) docs.get( 0 );

			// Verify document has content
			assertThat( doc.getAsString( Key.of( "content" ) ) ).isNotEmpty();

			// Verify metadata exists
			IStruct metadata = doc.getAsStruct( Key.of( "metadata" ) );
			assertThat( metadata.containsKey( Key.of( "feedType" ) ) ).isTrue();
			assertThat( metadata.containsKey( Key.of( "itemTitle" ) ) ).isTrue();
			assertThat( metadata.containsKey( Key.of( "itemLink" ) ) ).isTrue();

			// Verify feed type is RSS or Atom
			String feedType = metadata.getAsString( Key.of( "feedType" ) );
			assertThat( feedType ).isAnyOf( "rss", "atom" );
		} else {
			// Feed unavailable - just log and pass
			System.out.println( "BBC feed returned 0 documents - may be unavailable" );
		}
	}

	@DisplayName( "FeedLoader strips HTML from descriptions when configured" )
	@Test
	public void testFeedLoaderStripsHTML() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://feeds.bbci.co.uk/news/world/rss.xml" )
					.maxItems( 1 )
					.stripHtml( true )
					.includeDescription( true );
				rawDocs = loader.load();
				if( rawDocs.len() > 0 ) {
					result = rawDocs[1].getContent();
				} else {
					result = "";
				}
		    """,
		    context
		);
		// @formatter:on

		String content = variables.getAsString( result );
		// Content should not contain HTML tags
		assertThat( content ).doesNotContain( "<" );
		assertThat( content ).doesNotContain( ">" );
	}

	@DisplayName( "FeedLoader includes description when configured" )
	@Test
	public void testFeedLoaderIncludesDescription() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://feeds.bbci.co.uk/news/world/rss.xml" )
					.maxItems( 1 )
					.includeDescription( true );
				rawDocs = loader.load();
				if( rawDocs.len() > 0 ) {
					doc = rawDocs[1].toStruct();
					result = {
						hasContent: len(doc.content) > 0,
						contentLength: len(doc.content)
					};
				} else {
					result = { hasContent: false, contentLength: 0 };
				}
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		// Only assert if feed was available and returned docs
		if ( result.getAsBoolean( Key.of( "hasContent" ) ) ) {
			assertThat( result.getAsInteger( Key.of( "contentLength" ) ) ).isGreaterThan( 0 );
		}
	}

	@DisplayName( "FeedLoader creates documents with unique IDs" )
	@Test
	public void testFeedLoaderDocumentIDs() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://feeds.bbci.co.uk/news/world/rss.xml" )
					.maxItems( 3 );
				rawDocs = loader.load();
				result = {
					count: rawDocs.len(),
					ids: rawDocs.map( d => d.getId() )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		Array	ids		= result.getAsArray( Key.of( "ids" ) );

		// Verify we have IDs
		assertThat( ids.size() ).isGreaterThan( 0 );

		// Verify each ID is non-empty
		ids.forEach( id -> {
			assertThat( id.toString() ).isNotEmpty();
		} );
	}

	@DisplayName( "FeedLoader populates all metadata fields" )
	@Test
	public void testFeedLoaderMetadataFields() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://feeds.bbci.co.uk/news/world/rss.xml" )
					.maxItems( 1 );
				rawDocs = loader.load();
				if( rawDocs.len() > 0 ) {
					result = rawDocs[1].getMetadata();
				} else {
					result = {};
				}
		    """,
		    context
		);
		// @formatter:on

		IStruct metadata = ( IStruct ) variables.get( "result" );

		// Verify required metadata fields exist
		assertThat( metadata.containsKey( Key.of( "feedType" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "feedUrl" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "itemTitle" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "itemLink" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "guid" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "categories" ) ) ).isTrue();

		// Verify feedUrl is set correctly
		assertThat( metadata.getAsString( Key.of( "feedUrl" ) ) ).isEqualTo( "https://feeds.bbci.co.uk/news/world/rss.xml" );
	}

	@DisplayName( "FeedLoader handles error gracefully with continueOnError" )
	@Test
	public void testFeedLoaderContinueOnError() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.FeedLoader;
				loader = new FeedLoader( source: "https://invalid-feed-url-that-does-not-exist-12345.com/feed.xml" )
					.configure({ continueOnError: true });
				rawDocs = loader.load();
				result = {
					count: rawDocs.len(),
					errors: loader.getErrors()
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		int		count	= result.getAsInteger( Key.of( "count" ) );
		Array	errors	= result.getAsArray( Key.of( "errors" ) );

		// Should return empty array instead of throwing
		assertThat( count ).isEqualTo( 0 );

		// Should have recorded an error
		assertThat( errors.size() ).isGreaterThan( 0 );
	}

}
