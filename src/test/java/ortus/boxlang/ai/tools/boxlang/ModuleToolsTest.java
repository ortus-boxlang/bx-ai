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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class ModuleToolsTest extends BaseIntegrationTest {

	// =========================================================================
	// Registration
	// =========================================================================

	@DisplayName( "scanClass() registers all ModuleTools tool keys in the registry" )
	@Test
	public void testScanClassRegistersAllTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				aiToolRegistry().scanClass( new ModuleTools(), "bxtest" )
				result = aiToolRegistry().has( "get_modules@bxtest" )
				    && aiToolRegistry().has( "get_module_names@bxtest" )
				    && aiToolRegistry().has( "get_module_info@bxtest" )
				    && aiToolRegistry().has( "get_module_settings@bxtest" )
				    && aiToolRegistry().has( "get_module_paths@bxtest" )
				    && aiToolRegistry().has( "reload_module@bxtest" )
				    && aiToolRegistry().has( "reload_all_modules@bxtest" )
				    && aiToolRegistry().has( "has_module@bxtest" )
				    && aiToolRegistry().has( "get_module_stats@bxtest" )
				// Cleanup
				aiToolRegistry().unregisterByModule( "bxtest" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_modules
	// =========================================================================

	@DisplayName( "get_modules() returns a struct of registered modules" )
	@Test
	public void testGetModulesReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				result = tools.get_modules()
			""",
			context
		);
		// @formatter:on

		var modules = ( IStruct ) variables.get( "result" );
		assertThat( modules ).isNotNull();
		assertThat( modules ).isInstanceOf( IStruct.class );
		// At least the bxai module should be registered
		assertThat( modules.size() ).isAtLeast( 1 );

		var bxaiModule = ( IStruct ) modules.get( Key.of( "bxai" ) );
		assertThat( bxaiModule.containsKey( "name" ) ).isTrue();
		assertThat( bxaiModule.containsKey( "version" ) ).isTrue();
		assertThat( bxaiModule.containsKey( "activated" ) ).isTrue();
		assertThat( bxaiModule.containsKey( "physicalPath" ) ).isTrue();
	}

	// =========================================================================
	// get_module_names
	// =========================================================================

	@DisplayName( "get_module_names() returns a sorted array of module names" )
	@Test
	public void testGetModuleNamesReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				result = tools.get_module_names()
			""",
			context
		);
		// @formatter:on

		var names = ( Array ) variables.get( "result" );
		assertThat( names ).isNotNull();
		assertThat( names ).isInstanceOf( Array.class );
		assertThat( names.size() ).isAtLeast( 1 );
	}

	// =========================================================================
	// get_module_info
	// =========================================================================

	@DisplayName( "get_module_info() returns details for a known module" )
	@Test
	public void testGetModuleInfoReturnsDetails() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				names = tools.get_module_names()
				result = tools.get_module_info( names.get( 1 ) )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "error" ) ).isFalse();
		assertThat( info.containsKey( "name" ) ).isTrue();
		assertThat( info.containsKey( "id" ) ).isTrue();
		assertThat( info.containsKey( "version" ) ).isTrue();
		assertThat( info.containsKey( "physicalPath" ) ).isTrue();
		assertThat( info.containsKey( "settings" ) ).isTrue();
	}

	@DisplayName( "get_module_info() returns error struct for unknown module" )
	@Test
	public void testGetModuleInfoReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				result = tools.get_module_info( "nonexistent-module-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
		assertThat( info.get( "message" ).toString() ).contains( "nonexistent-module-xyz" );
	}

	// =========================================================================
	// get_module_settings
	// =========================================================================

	@DisplayName( "get_module_settings() returns settings for a known module" )
	@Test
	public void testGetModuleSettingsReturnsSettings() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				names = tools.get_module_names()
				println( names )
				result = tools.get_module_settings( names.get( 1 ) )
			""",
			context
		);
		// @formatter:on

		var settings = ( IStruct ) variables.get( "result" );
		assertThat( settings ).isNotNull();
		assertThat( settings.containsKey( "error" ) ).isFalse();
		assertThat( settings.containsKey( "name" ) ).isTrue();
		assertThat( settings.containsKey( "settings" ) ).isTrue();
	}

	@DisplayName( "get_module_settings() returns error struct for unknown module" )
	@Test
	public void testGetModuleSettingsReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				result = tools.get_module_settings( "nonexistent-module-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_module_paths
	// =========================================================================

	@DisplayName( "get_module_paths() returns an array of module search paths" )
	@Test
	public void testGetModulePathsReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				result = tools.get_module_paths()
			""",
			context
		);
		// @formatter:on

		var paths = ( Array ) variables.get( "result" );
		assertThat( paths ).isNotNull();
		assertThat( paths ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// reload_module
	// =========================================================================

	@DisplayName( "reload_module() returns error struct for unknown module" )
	@Test
	public void testReloadModuleReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				result = tools.reload_module( "nonexistent-module-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
		assertThat( info.get( "message" ).toString() ).contains( "nonexistent-module-xyz" );
	}

	// =========================================================================
	// reload_all_modules
	// =========================================================================

	@DisplayName( "reload_all_modules() returns results for all modules" )
	@Test
	public void testReloadAllModulesReturnsResults() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				result = tools.reload_all_modules()
			""",
			context
		);
		// @formatter:on

		var results = ( IStruct ) variables.get( "result" );
		assertThat( results ).isNotNull();
		assertThat( results.containsKey( "total" ) ).isTrue();
		assertThat( results.containsKey( "results" ) ).isTrue();
		assertThat( ( ( Array ) results.get( "results" ) ).size() ).isAtLeast( 1 );
	}

	// =========================================================================
	// has_module
	// =========================================================================

	@DisplayName( "has_module() returns true for a registered module" )
	@Test
	public void testHasModuleReturnsTrueForRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				names = tools.get_module_names()
				result = tools.has_module( names.get( 1 ) )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "registered" ) ).isEqualTo( true );
	}

	@DisplayName( "has_module() returns false for an unregistered module" )
	@Test
	public void testHasModuleReturnsFalseForUnregistered() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				result = tools.has_module( "nonexistent-module-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "registered" ) ).isEqualTo( false );
	}

	// =========================================================================
	// get_module_stats
	// =========================================================================

	@DisplayName( "get_module_stats() returns module statistics" )
	@Test
	public void testGetModuleStatsReturnsStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.ModuleTools;
				tools = new ModuleTools()
				result = tools.get_module_stats()
			""",
			context
		);
		// @formatter:on

		var stats = ( IStruct ) variables.get( "result" );
		assertThat( stats ).isNotNull();
		assertThat( stats.containsKey( "totalModules" ) ).isTrue();
		assertThat( stats.containsKey( "activatedModules" ) ).isTrue();
		assertThat( stats.containsKey( "enabledModules" ) ).isTrue();
		assertThat( stats.containsKey( "totalBifs" ) ).isTrue();
		assertThat( stats.containsKey( "totalComponents" ) ).isTrue();
		assertThat( stats.containsKey( "totalInterceptors" ) ).isTrue();
		assertThat( stats.containsKey( "modulePaths" ) ).isTrue();
	}

}
