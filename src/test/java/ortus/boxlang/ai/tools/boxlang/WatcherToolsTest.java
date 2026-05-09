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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class WatcherToolsTest extends BaseIntegrationTest {

	// =========================================================================
	// get_watchers
	// =========================================================================

	@DisplayName( "get_watchers() returns an array of registered watchers" )
	@Test
	public void testGetWatchersReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.get_watchers()
			""",
			context
		);
		// @formatter:on

		var watchers = ( Array ) variables.get( "result" );
		assertThat( watchers ).isNotNull();
		assertThat( watchers ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// get_watcher_names
	// =========================================================================

	@DisplayName( "get_watcher_names() returns an array of watcher names" )
	@Test
	public void testGetWatcherNamesReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.get_watcher_names()
			""",
			context
		);
		// @formatter:on

		var names = ( Array ) variables.get( "result" );
		assertThat( names ).isNotNull();
		assertThat( names ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// get_watcher
	// =========================================================================

	@DisplayName( "get_watcher() returns details for a known watcher" )
	@Test
	public void testGetWatcherReturnsDetails() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				names = tools.get_watcher_names()
				if ( names.size() > 0 ) {
					result = tools.get_watcher( names.get( 1 ) )
				} else {
					result = { "skipped" : true }
				}
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		if ( !info.containsKey( "skipped" ) ) {
			assertThat( info.containsKey( "error" ) ).isFalse();
			assertThat( info.containsKey( "name" ) ).isTrue();
			assertThat( info.containsKey( "state" ) ).isTrue();
			assertThat( info.containsKey( "isRunning" ) ).isTrue();
			assertThat( info.containsKey( "watchPaths" ) ).isTrue();
		}
	}

	@DisplayName( "get_watcher() returns error struct for unknown watcher" )
	@Test
	public void testGetWatcherReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.get_watcher( "nonexistent-watcher-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
		assertThat( info.get( "message" ).toString() ).contains( "nonexistent-watcher-xyz" );
	}

	// =========================================================================
	// has_watcher
	// =========================================================================

	@DisplayName( "has_watcher() returns true for a registered watcher" )
	@Test
	public void testHasWatcherReturnsTrueForRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				names = tools.get_watcher_names()
				if ( names.size() > 0 ) {
					result = tools.has_watcher( names.get( 1 ) )
				} else {
					result = { "skipped" : true }
				}
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		if ( !info.containsKey( "skipped" ) ) {
			assertThat( info.get( "registered" ) ).isEqualTo( true );
		}
	}

	@DisplayName( "has_watcher() returns false for an unregistered watcher" )
	@Test
	public void testHasWatcherReturnsFalseForUnregistered() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.has_watcher( "nonexistent-watcher-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "registered" ) ).isEqualTo( false );
	}

	// =========================================================================
	// get_watcher_stats
	// =========================================================================

	@DisplayName( "get_watcher_stats() returns stats for a known watcher" )
	@Test
	public void testGetWatcherStatsReturnsStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				names = tools.get_watcher_names()
				if ( names.size() > 0 ) {
					result = tools.get_watcher_stats( names.get( 1 ) )
				} else {
					result = { "skipped" : true }
				}
			""",
			context
		);
		// @formatter:on

		var stats = ( IStruct ) variables.get( "result" );
		assertThat( stats ).isNotNull();
		if ( !stats.containsKey( "skipped" ) ) {
			assertThat( stats.containsKey( "name" ) ).isTrue();
			assertThat( stats.containsKey( "state" ) ).isTrue();
			assertThat( stats.containsKey( "paths" ) ).isTrue();
		}
	}

	@DisplayName( "get_watcher_stats() returns error struct for unknown watcher" )
	@Test
	public void testGetWatcherStatsReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.get_watcher_stats( "nonexistent-watcher-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// start_watcher
	// =========================================================================

	@DisplayName( "start_watcher() returns error struct for unknown watcher" )
	@Test
	public void testStartWatcherReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.start_watcher( "nonexistent-watcher-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// stop_watcher
	// =========================================================================

	@DisplayName( "stop_watcher() returns error struct for unknown watcher" )
	@Test
	public void testStopWatcherReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.stop_watcher( "nonexistent-watcher-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// restart_watcher
	// =========================================================================

	@DisplayName( "restart_watcher() returns error struct for unknown watcher" )
	@Test
	public void testRestartWatcherReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.restart_watcher( "nonexistent-watcher-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// remove_watcher
	// =========================================================================

	@DisplayName( "remove_watcher() returns error struct for unknown watcher" )
	@Test
	public void testRemoveWatcherReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.remove_watcher( "nonexistent-watcher-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_watcher_health
	// =========================================================================

	@DisplayName( "get_watcher_health() returns health summary" )
	@Test
	public void testGetWatcherHealthReturnsSummary() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.WatcherTools;
				tools = new WatcherTools()
				result = tools.get_watcher_health()
			""",
			context
		);
		// @formatter:on

		var health = ( IStruct ) variables.get( "result" );
		assertThat( health ).isNotNull();
		assertThat( health.containsKey( "totalWatchers" ) ).isTrue();
		assertThat( health.containsKey( "runningWatchers" ) ).isTrue();
		assertThat( health.containsKey( "stoppedWatchers" ) ).isTrue();
		assertThat( health.containsKey( "totalErrors" ) ).isTrue();
	}

}
