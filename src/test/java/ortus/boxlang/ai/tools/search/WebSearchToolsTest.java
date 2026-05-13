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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;

public class WebSearchToolsTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "braveApiKey", dotenv.get( "BRAVE_API_KEY", "" ) );
		moduleRecord.settings.put( "googleApiKey", dotenv.get( "GOOGLE_API_KEY", "" ) );
		moduleRecord.settings.put( "googleSearchEngineId", dotenv.get( "GOOGLE_SEARCH_ENGINE_ID", "" ) );
		moduleRecord.settings.put( "tavilyApiKey", dotenv.get( "TAVILY_API_KEY", "" ) );
		moduleRecord.settings.put( "exaApiKey", dotenv.get( "EXA_API_KEY", "" ) );
	}

	private void assertProviderSearch( String providerName, String constructorConfig, String query ) {
		// @formatter:off
		runtime.executeSource(
			"""
				tool = new bxModules.bxai.models.tools.search.WebSearchTools( { provider: "%s", %s } )
				results = tool.webSearch( "%s" )
				println( results )
				first = results.first()
				result = isArray( results ) && results.len() > 0 && structKeyExists( first, "title" ) && structKeyExists( first, "url" ) && structKeyExists( first, "snippet" )
			""".formatted( providerName, constructorConfig, query ),
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "WebSearchTools http provider returns fetched content for URLs" )
	@Test
	public void testBraveProvider() {
		var braveApiKey = dotenv.get( "BRAVE_API_KEY", "" );
		Assumptions.assumeTrue( braveApiKey != null && !braveApiKey.isBlank(), "BRAVE_API_KEY not set" );

		assertProviderSearch(
		    "brave",
		    "maxResults: 3, timeout: 15",
		    "BoxLang programming language"
		);
	}

	@DisplayName( "WebSearchTools google provider returns results" )
	@Test
	public void testGoogleProvider() {
		var	googleApiKey			= dotenv.get( "GOOGLE_API_KEY", "" );
		var	googleSearchEngineId	= dotenv.get( "GOOGLE_SEARCH_ENGINE_ID", "" );
		Assumptions.assumeTrue( googleApiKey != null && !googleApiKey.isBlank(), "GOOGLE_API_KEY not set" );
		Assumptions.assumeTrue( googleSearchEngineId != null && !googleSearchEngineId.isBlank(), "GOOGLE_SEARCH_ENGINE_ID not set" );

		assertProviderSearch(
		    "google",
		    "maxResults: 3, timeout: 15",
		    "BoxLang programming language"
		);
	}

	@DisplayName( "WebSearchTools tavily provider returns results" )
	@Test
	public void testTavilyProvider() {
		var tavilyApiKey = dotenv.get( "TAVILY_API_KEY", "" );
		Assumptions.assumeTrue( tavilyApiKey != null && !tavilyApiKey.isBlank(), "TAVILY_API_KEY not set" );

		assertProviderSearch(
		    "tavily",
		    "maxResults: 3, timeout: 15",
		    "BoxLang programming language"
		);
	}

	@DisplayName( "WebSearchTools exa provider returns results" )
	@Test
	public void testExaProvider() {
		var exaApiKey = dotenv.get( "EXA_API_KEY", "" );
		Assumptions.assumeTrue( exaApiKey != null && !exaApiKey.isBlank(), "EXA_API_KEY not set" );

		assertProviderSearch(
		    "exa",
		    "maxResults: 3, timeout: 15",
		    "BoxLang programming language"
		);
	}

	@DisplayName( "WebSearchTools http provider returns fetched content for URLs" )
	@Test
	public void testHttpProvider() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool = new bxModules.bxai.models.tools.search.WebSearchTools( { provider: "http", maxResults: 1, timeout: 15 } )
				results = tool.webSearch( "https://example.com" )
				println( results )
				first = results[ 1 ]
				result = isArray( results ) && results.len() == 1 && len( first.title ) > 0 && first.url == "https://example.com"
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// -------------------------------------------------------------------------
	// webSearch() BIF
	// -------------------------------------------------------------------------

	@DisplayName( "webSearch BIF returns results with default http provider" )
	@Test
	public void testWebSearchBif() {
		// @formatter:off
		runtime.executeSource(
			"""
				results = webSearch( "https://example.com" )
				first = results.first()
				result = isArray( results ) && results.len() > 0 && structKeyExists( first, "title" ) && structKeyExists( first, "url" ) && structKeyExists( first, "snippet" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "webSearch BIF with brave provider returns results" )
	@Test
	public void testWebSearchBifBrave() {
		var braveApiKey = dotenv.get( "BRAVE_API_KEY", "" );
		Assumptions.assumeTrue( braveApiKey != null && !braveApiKey.isBlank(), "BRAVE_API_KEY not set" );

		// @formatter:off
		runtime.executeSource(
			"""
				results = webSearch( "BoxLang programming language", { provider: "brave" } )
				first = results.first()
				result = isArray( results ) && results.len() > 0 && structKeyExists( first, "title" ) && structKeyExists( first, "url" ) && structKeyExists( first, "snippet" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "searchAsync() on provider returns BoxFuture resolving to results array" )
	@Test
	public void testSearchAsync() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool    = new bxModules.bxai.models.tools.search.HttpSearch()
				future  = tool.searchAsync( "https://example.com" )
				results = future.get()
				first   = results.first()
				result  = isArray( results ) && results.len() > 0 && structKeyExists( first, "title" ) && structKeyExists( first, "url" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "webSearchAsync BIF returns BoxFuture resolving to results array" )
	@Test
	public void testWebSearchAsyncBif() {
		// @formatter:off
		runtime.executeSource(
			"""
				future  = webSearchAsync( "https://example.com" )
				results = future.get()
				first   = results.first()
				result  = isArray( results ) && results.len() > 0 && structKeyExists( first, "title" ) && structKeyExists( first, "url" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

}
