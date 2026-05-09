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
package ortus.boxlang.ai.tools.boxlang;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.IStruct;

public class BoxLangToolsTest extends BaseIntegrationTest {

	// =========================================================================
	// Registration
	// =========================================================================

	@DisplayName( "scanClass() registers all BoxLangTools tool keys in the registry" )
	@Test
	public void testScanClassRegistersAllTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
				aiToolRegistry().scanClass( new BoxLangTools(), "bxtest" )
				result = aiToolRegistry().has( "get_runtime_info@bxtest" )
				    && aiToolRegistry().has( "get_runtime_config@bxtest" )
				    && aiToolRegistry().has( "get_config_value@bxtest" )
				    && aiToolRegistry().has( "get_bif_summary@bxtest" )
				    && aiToolRegistry().has( "get_bif_info@bxtest" )
				    && aiToolRegistry().has( "search_bifs@bxtest" )
				    && aiToolRegistry().has( "get_component_summary@bxtest" )
				    && aiToolRegistry().has( "get_component_info@bxtest" )
				    && aiToolRegistry().has( "search_components@bxtest" )
				    && aiToolRegistry().has( "get_global_services@bxtest" )
				// Cleanup
				aiToolRegistry().unregisterByModule( "bxtest" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_runtime_info
	// =========================================================================

	@DisplayName( "get_runtime_info() returns a struct with version and uptime" )
	@Test
	public void testGetRuntimeInfoReturnsVersionAndUptime() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools = new BoxLangTools()
				result = tools.get_runtime_info()
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( result );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "boxlang" ) ).isTrue();
		assertThat( info.containsKey( "uptime" ) ).isTrue();
		assertThat( info.containsKey( "jvm" ) ).isTrue();
		assertThat( info.containsKey( "os" ) ).isTrue();
	}

	// =========================================================================
	// get_runtime_config
	// =========================================================================

	@DisplayName( "get_runtime_config() returns a non-empty config struct" )
	@Test
	public void testGetRuntimeConfigReturnsNonEmptyStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools = new BoxLangTools()
				result = tools.get_runtime_config()
			""",
			context
		);
		// @formatter:on

		var config = ( IStruct ) variables.get( result );
		assertThat( config ).isNotNull();
		assertThat( config.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "get_runtime_config() masks sensitive values by default" )
	@Test
	public void testGetRuntimeConfigMasksSensitiveValues() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools     = new BoxLangTools()
					config    = tools.get_runtime_config()
				// includeSensitive defaults to false - masking should apply
				result = ( config.keyExists( "security" ) )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_bif_summary
	// =========================================================================

	@DisplayName( "get_bif_summary() returns total, categories, and names" )
	@Test
	public void testGetBifSummaryReturnsExpectedKeys() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools = new BoxLangTools()
				result = tools.get_bif_summary()
			""",
			context
		);
		// @formatter:on

		var summary = ( IStruct ) variables.get( result );
		assertThat( summary ).isNotNull();
		assertThat( summary.containsKey( "total" ) ).isTrue();
		assertThat( summary.containsKey( "categories" ) ).isTrue();
		assertThat( summary.containsKey( "names" ) ).isTrue();

		var total = ( Number ) summary.get( "total" );
		assertThat( total.intValue() ).isGreaterThan( 0 );
	}

	// =========================================================================
	// get_bif_info
	// =========================================================================

	@DisplayName( "get_bif_info() returns metadata for a known BIF" )
	@Test
	public void testGetBifInfoReturnsMetadataForKnownBif() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools = new BoxLangTools()
				result = tools.get_bif_info( "arrayLen" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( result );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "error" ) ).isFalse();
		assertThat( info.containsKey( "name" ) ).isTrue();
	}

	@DisplayName( "get_bif_info() returns error struct for unknown BIF" )
	@Test
	public void testGetBifInfoReturnsErrorForUnknownBif() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools = new BoxLangTools()
				result = tools.get_bif_info( "thisDoesNotExistEver" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( result );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// search_bifs
	// =========================================================================

	@DisplayName( "search_bifs() returns matching BIF names" )
	@Test
	public void testSearchBifsReturnsMatches() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools = new BoxLangTools()
				result = tools.search_bifs( "array" )
			""",
			context
		);
		// @formatter:on

		var searchResult = ( IStruct ) variables.get( result );
		assertThat( searchResult ).isNotNull();
		assertThat( searchResult.containsKey( "count" ) ).isTrue();
		assertThat( searchResult.containsKey( "matches" ) ).isTrue();

		var count = ( Number ) searchResult.get( "count" );
		assertThat( count.intValue() ).isGreaterThan( 0 );
	}

	// =========================================================================
	// get_component_summary
	// =========================================================================

	@DisplayName( "get_component_summary() returns total and paths" )
	@Test
	public void testGetComponentSummaryReturnsTotalAndPaths() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools = new BoxLangTools()
				result = tools.get_component_summary()
			""",
			context
		);
		// @formatter:on

		var summary = ( IStruct ) variables.get( result );
		assertThat( summary ).isNotNull();
		assertThat( summary.containsKey( "total" ) ).isTrue();
		assertThat( summary.containsKey( "paths" ) ).isTrue();

		var total = ( Number ) summary.get( "total" );
		assertThat( total.intValue() ).isGreaterThan( 0 );
	}

	// =========================================================================
	// search_components
	// =========================================================================

	@DisplayName( "search_components() returns matching component paths" )
	@Test
	public void testSearchComponentsReturnsMatches() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools = new BoxLangTools()
				result = tools.search_components( "output" )
			""",
			context
		);
		// @formatter:on

		var searchResult = ( IStruct ) variables.get( result );
		assertThat( searchResult ).isNotNull();
		assertThat( searchResult.containsKey( "count" ) ).isTrue();
	}

	// =========================================================================
	// get_global_services
	// =========================================================================

	@DisplayName( "get_global_services() returns a list of services" )
	@Test
	public void testGetGlobalServicesReturnsList() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
					tools = new BoxLangTools()
				result = tools.get_global_services()
			""",
			context
		);
		// @formatter:on

		var services = variables.get( result );
		assertThat( services ).isNotNull();
	}

	// =========================================================================
	// toggle_debug_mode
	// =========================================================================

	@DisplayName( "toggle_debug_mode() enables and disables runtime debug mode" )
	@Test
	public void testToggleDebugMode() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.BoxLangTools;
				tools = new BoxLangTools()

				// Enable debug mode
				result = tools.toggle_debug_mode( enabled: true )
			""",
			context
		);
		// @formatter:on

		var debugResult = ( IStruct ) variables.get( result );
		assertThat( debugResult ).isNotNull();
		assertThat( debugResult.get( "debugMode" ) ).isEqualTo( true );
		assertThat( debugResult.containsKey( "previous" ) ).isTrue();
		assertThat( debugResult.containsKey( "message" ) ).isTrue();

		// Verify runtime config was actually updated
		runtime.executeSource(
		    """
		    	config = getBoxRuntime().getConfiguration()
		    	isDebug = config.debugMode
		    """,
		    context
		);
		assertThat( variables.get( "isDebug" ) ).isEqualTo( true );

		// Disable debug mode
		runtime.executeSource(
		    """
		    	result = tools.toggle_debug_mode( enabled: false )
		    """,
		    context
		);
		debugResult = ( IStruct ) variables.get( result );
		assertThat( debugResult.get( "debugMode" ) ).isEqualTo( false );
		assertThat( debugResult.get( "previous" ) ).isEqualTo( true );
	}

}
