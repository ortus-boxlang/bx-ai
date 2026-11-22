/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.ai.tools.search;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * The web search providers that need no API key: duckduckgo, wikipedia and marginalia.
 *
 * The parse tests read saved answers from src/test/resources/search, so they never touch the network. The live tests
 * call each service for real; they are how CI tells whether a keyless provider still answers automated requests.
 */
public class KeylessWebSearchTest extends BaseIntegrationTest {

	private static final String FIXTURES = Paths.get( "src/test/resources/search" ).toAbsolutePath().toString().replace( "\\", "/" );

	// ---- parsing saved answers ----

	@DisplayName( "duckduckgo: reads titles, real URLs and snippets, and leaves the ads out" )
	@Test
	public void testDuckDuckGoParse() {
		// @formatter:off
		runtime.executeSource(
			"""
				provider = new bxModules.bxai.models.tools.search.DuckDuckGoSearch().configure( { maxResults: 5 } )
				results = provider.parseResults( fileRead( "%s/duckduckgo-results.html" ), "boxlang" )
				count = toString( results.len() )
				firstTitle = results[ 1 ].title
				firstUrl = results[ 1 ].url
				firstSnippet = results[ 1 ].snippet
				firstDomain = results[ 1 ].domain
				secondUrl = results[ 2 ].url
			""".formatted( FIXTURES ),
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "count" ) ) ).isEqualTo( "2" );
		assertThat( variables.getAsString( Key.of( "firstTitle" ) ) ).isEqualTo( "BoxLang : A Modern Dynamic JVM Language" );
		assertThat( variables.getAsString( Key.of( "firstUrl" ) ) ).isEqualTo( "https://boxlang.io/" );
		assertThat( variables.getAsString( Key.of( "firstSnippet" ) ) ).isEqualTo( "BoxLang is a modern dynamic JVM language." );
		assertThat( variables.getAsString( Key.of( "firstDomain" ) ) ).isEqualTo( "boxlang.io" );
		assertThat( variables.getAsString( Key.of( "secondUrl" ) ) ).isEqualTo( "https://github.com/ortus-boxlang/BoxLang" );
	}

	@DisplayName( "duckduckgo: maxResults is honoured" )
	@Test
	public void testDuckDuckGoMaxResults() {
		// @formatter:off
		runtime.executeSource(
			"""
				provider = new bxModules.bxai.models.tools.search.DuckDuckGoSearch().configure( { maxResults: 1 } )
				count = toString( provider.parseResults( fileRead( "%s/duckduckgo-results.html" ), "boxlang" ).len() )
			""".formatted( FIXTURES ),
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "count" ) ) ).isEqualTo( "1" );
	}

	@DisplayName( "duckduckgo: a challenge page is reported as blocked, not as no results" )
	@Test
	public void testDuckDuckGoChallenge() {
		// @formatter:off
		runtime.executeSource(
			"""
				provider = new bxModules.bxai.models.tools.search.DuckDuckGoSearch().configure( {} )
				results = provider.parseResults( fileRead( "%s/duckduckgo-challenge.html" ), "boxlang" )
				title = results[ 1 ].title
				redirect = provider.realUrl( "//duckduckgo.com/l/?uddg=https%%3A%%2F%%2Fexample.com%%2Fa%%3Fb%%3D1&rut=x" )
			""".formatted( FIXTURES ),
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "title" ) ) ).isEqualTo( "DuckDuckGo Search Blocked" );
		assertThat( variables.getAsString( Key.of( "redirect" ) ) ).isEqualTo( "https://example.com/a?b=1" );
	}

	@DisplayName( "wikipedia: plain-text snippets and article URLs" )
	@Test
	public void testWikipediaParse() {
		// @formatter:off
		runtime.executeSource(
			"""
				provider = new bxModules.bxai.models.tools.search.WikipediaSearch().configure( {} )
				results = provider.parseResults( fileRead( "%s/wikipedia-results.json" ), "java" )
				count = toString( results.len() )
				firstUrl = results[ 1 ].url
				firstSnippet = results[ 1 ].snippet
				secondUrl = results[ 2 ].url
				german = new bxModules.bxai.models.tools.search.WikipediaSearch().configure( { language: "de" } )
					.parseResults( fileRead( "%s/wikipedia-results.json" ), "java" )[ 1 ].url
			""".formatted( FIXTURES, FIXTURES ),
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "count" ) ) ).isEqualTo( "2" );
		assertThat( variables.getAsString( Key.of( "firstUrl" ) ) ).isEqualTo( "https://en.wikipedia.org/wiki/Java_virtual_machine" );
		assertThat( variables.getAsString( Key.of( "firstSnippet" ) ) )
		    .isEqualTo( "A Java virtual machine (JVM) is a virtual machine that runs Java programs" );
		assertThat( variables.getAsString( Key.of( "secondUrl" ) ) ).isEqualTo( "https://en.wikipedia.org/wiki/AC/DC" );
		assertThat( variables.getAsString( Key.of( "german" ) ) ).isEqualTo( "https://de.wikipedia.org/wiki/Java_virtual_machine" );
	}

	@DisplayName( "marginalia: results and the shared key" )
	@Test
	public void testMarginaliaParse() {
		// @formatter:off
		runtime.executeSource(
			"""
				provider = new bxModules.bxai.models.tools.search.MarginaliaSearch().configure( { maxResults: 5 } )
				results = provider.parseResults( fileRead( "%s/marginalia-results.json" ), "boxlang" )
				count = toString( results.len() )
				firstUrl = results[ 1 ].url
				firstSnippet = results[ 1 ].snippet
				sharedKey = provider.getApiKey()
				empty = provider.parseResults( '{"query":"x","results":[]}', "x" )[ 1 ].title
			""".formatted( FIXTURES ),
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "count" ) ) ).isEqualTo( "2" );
		assertThat( variables.getAsString( Key.of( "firstUrl" ) ) ).isEqualTo( "https://boxlang.io/" );
		assertThat( variables.getAsString( Key.of( "firstSnippet" ) ) ).isEqualTo( "A modern dynamic JVM language." );
		assertThat( variables.getAsString( Key.of( "sharedKey" ) ) ).isEqualTo( "public" );
		assertThat( variables.getAsString( Key.of( "empty" ) ) ).isEqualTo( "No Results" );
	}

	@DisplayName( "the keyless providers are registered with WebSearchTools" )
	@Test
	public void testRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
				names = [ "duckduckgo", "wikipedia", "marginalia" ].map( name =>
					getMetadata( bxModules.bxai.models.tools.search.WebSearchTools::createProvider( name, {} ) ).name
				).toList()
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "names" ) ) ).contains( "DuckDuckGoSearch" );
		assertThat( variables.getAsString( Key.of( "names" ) ) ).contains( "WikipediaSearch" );
		assertThat( variables.getAsString( Key.of( "names" ) ) ).contains( "MarginaliaSearch" );
	}

	// ---- live: does the service answer an automated search from here? ----

	/**
	 * Runs one real search and checks that the first result is a real hit (a URL), not one of the provider's
	 * explanation results (blocked, busy, error, no results). The results are printed so the CI log shows them.
	 */
	private void assertLiveSearch( String provider, String query ) {
		// @formatter:off
		runtime.executeSource(
			"""
				results = new bxModules.bxai.models.tools.search.WebSearchTools( { provider: "%s", maxResults: 3, timeout: 20 } ).webSearch( "%s" )
				println( "LIVE %s: " & jsonSerialize( results ) )
				first = results.first()
				liveTitle = first.title
				liveUrl = first.url
				liveOk = results.len() > 0 && first.url.reFindNoCase( "^https?://" ) > 0
					&& !first.title.reFindNoCase( "(Error|Blocked|Busy|^No Results)" )
			""".formatted( provider, query, provider ),
			context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "liveOk" ) ) ).isTrue();
	}

	@DisplayName( "live: duckduckgo answers an automated search" )
	@Test
	public void testLiveDuckDuckGo() {
		assertLiveSearch( "duckduckgo", "BoxLang programming language" );
	}

	@DisplayName( "live: wikipedia answers an automated search" )
	@Test
	public void testLiveWikipedia() {
		assertLiveSearch( "wikipedia", "Java virtual machine" );
	}

	@DisplayName( "live: marginalia answers an automated search with the shared key" )
	@Test
	public void testLiveMarginalia() {
		assertLiveSearch( "marginalia", "java virtual machine" );
	}

}
