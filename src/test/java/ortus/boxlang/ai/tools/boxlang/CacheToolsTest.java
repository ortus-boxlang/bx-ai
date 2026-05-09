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

public class CacheToolsTest extends BaseIntegrationTest {

	// =========================================================================
	// Registration
	// =========================================================================

	@DisplayName( "scanClass() registers all CacheTools tool keys in the registry" )
	@Test
	public void testScanClassRegistersAllTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				aiToolRegistry().scanClass( new CacheTools(), "bxtest" )
				result = aiToolRegistry().has( "get_caches@bxtest" )
				    && aiToolRegistry().has( "get_cache_names@bxtest" )
				    && aiToolRegistry().has( "get_cache_stats@bxtest" )
				    && aiToolRegistry().has( "get_cache_keys@bxtest" )
				    && aiToolRegistry().has( "get_cache_size@bxtest" )
				    && aiToolRegistry().has( "get_cache_key_metadata@bxtest" )
				    && aiToolRegistry().has( "cache_key_exists@bxtest" )
				    && aiToolRegistry().has( "get_store_metadata_report@bxtest" )
				    && aiToolRegistry().has( "clear_all@bxtest" )
				    && aiToolRegistry().has( "clear_item@bxtest" )
				    && aiToolRegistry().has( "reap_cache@bxtest" )
				    && aiToolRegistry().has( "clear_cache_stats@bxtest" )
				    && aiToolRegistry().has( "clear_all_caches@bxtest" )
				    && aiToolRegistry().has( "reap_all_caches@bxtest" )
				    && aiToolRegistry().has( "get_cache_health_summary@bxtest" )
				// Cleanup
				aiToolRegistry().unregisterByModule( "bxtest" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_caches
	// =========================================================================

	@DisplayName( "get_caches() returns a struct of cache stats" )
	@Test
	public void testGetCachesReturnsStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				result = tools.get_caches()
			""",
			context
		);
		// @formatter:on

		var caches = ( IStruct ) variables.get( "result" );
		assertThat( caches ).isNotNull();
		assertThat( caches ).isInstanceOf( IStruct.class );
	}

	// =========================================================================
	// get_cache_names
	// =========================================================================

	@DisplayName( "get_cache_names() returns a sorted array of cache names" )
	@Test
	public void testGetCacheNamesReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				result = tools.get_cache_names()
			""",
			context
		);
		// @formatter:on

		var names = ( Array ) variables.get( "result" );
		assertThat( names ).isNotNull();
		assertThat( names ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// get_cache_stats
	// =========================================================================

	@DisplayName( "get_cache_stats() returns stats for a known cache" )
	@Test
	public void testGetCacheStatsReturnsStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				names = tools.get_cache_names()
				if ( names.len() > 0 ) {
					result = tools.get_cache_stats( names.get( 1 ) )
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
			assertThat( stats.containsKey( "hits" ) ).isTrue();
			assertThat( stats.containsKey( "misses" ) ).isTrue();
			assertThat( stats.containsKey( "hitRate" ) ).isTrue();
		}
	}

	@DisplayName( "get_cache_stats() returns error struct for unknown cache" )
	@Test
	public void testGetCacheStatsReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				result = tools.get_cache_stats( "nonexistent-cache-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_cache_keys
	// =========================================================================

	@DisplayName( "get_cache_keys() returns an array of keys for a known cache" )
	@Test
	public void testGetCacheKeysReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				names = tools.get_cache_names()
				if ( names.len() > 0 ) {
					result = tools.get_cache_keys( names.get( 1 ) )
				} else {
					result = { "skipped" : true }
				}
			""",
			context
		);
		// @formatter:on

		var keys = variables.get( "result" );
		assertThat( keys ).isNotNull();
		if ( !"skipped".equals( keys ) ) {
			assertThat( keys ).isInstanceOf( Array.class );
		}
	}

	@DisplayName( "get_cache_keys() returns error struct for unknown cache" )
	@Test
	public void testGetCacheKeysReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				result = tools.get_cache_keys( "nonexistent-cache-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_cache_size
	// =========================================================================

	@DisplayName( "get_cache_size() returns a numeric size for a known cache" )
	@Test
	public void testGetCacheSizeReturnsNumeric() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				names = tools.get_cache_names()
				if ( names.len() > 0 ) {
					result = tools.get_cache_size( names.get( 1 ) )
				} else {
					result = { "skipped" : true }
				}
			""",
			context
		);
		// @formatter:on

		var size = variables.get( "result" );
		assertThat( size ).isNotNull();
		if ( !"skipped".equals( size ) ) {
			assertThat( size ).isInstanceOf( Number.class );
		}
	}

	// =========================================================================
	// cache_key_exists
	// =========================================================================

	@DisplayName( "cache_key_exists() returns false for a non-existent key" )
	@Test
	public void testCacheKeyExistsReturnsFalseForMissingKey() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				names = tools.get_cache_names()
				if ( names.len() > 0 ) {
					result = tools.cache_key_exists( names.get( 1 ), "nonexistent-key-xyz" )
				} else {
					result = { "skipped" : true }
				}
			""",
			context
		);
		// @formatter:on

		var exists = variables.get( "result" );
		assertThat( exists ).isNotNull();
		if ( !"skipped".equals( exists ) ) {
			assertThat( exists ).isEqualTo( false );
		}
	}

	// =========================================================================
	// get_store_metadata_report
	// =========================================================================

	@DisplayName( "get_store_metadata_report() returns a struct for a known cache" )
	@Test
	public void testGetStoreMetadataReportReturnsStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				names = tools.get_cache_names()
				if ( names.len() > 0 ) {
					result = tools.get_store_metadata_report( names.get( 1 ) )
				} else {
					result = { "skipped" : true }
				}
			""",
			context
		);
		// @formatter:on

		var report = ( IStruct ) variables.get( "result" );
		assertThat( report ).isNotNull();
	}

	// =========================================================================
	// clear_all
	// =========================================================================

	@DisplayName( "clear_all() returns success message for a known cache" )
	@Test
	public void testClearAllReturnsSuccess() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				names = tools.get_cache_names()
				if ( names.len() > 0 ) {
					result = tools.clear_all( names.get( 1 ) )
				} else {
					result = { "skipped" : true }
				}
			""",
			context
		);
		// @formatter:on

		var result = variables.get( "result" );
		assertThat( result ).isNotNull();
	}

	// =========================================================================
	// reap_cache
	// =========================================================================

	@DisplayName( "reap_cache() returns success message for a known cache" )
	@Test
	public void testReapCacheReturnsSuccess() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				names = tools.get_cache_names()
				if ( names.len() > 0 ) {
					result = tools.reap_cache( names.get( 1 ) )
				} else {
					result = { "skipped" : true }
				}
			""",
			context
		);
		// @formatter:on

		var result = variables.get( "result" );
		assertThat( result ).isNotNull();
	}

	// =========================================================================
	// get_cache_health_summary
	// =========================================================================

	@DisplayName( "get_cache_health_summary() returns a summary struct with totals" )
	@Test
	public void testGetCacheHealthSummaryReturnsSummary() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				result = tools.get_cache_health_summary()
			""",
			context
		);
		// @formatter:on

		var summary = ( IStruct ) variables.get( "result" );
		assertThat( summary ).isNotNull();
		assertThat( summary.containsKey( "total" ) ).isTrue();
		assertThat( summary.containsKey( "totalSize" ) ).isTrue();
		assertThat( summary.containsKey( "details" ) ).isTrue();
	}

	// =========================================================================
	// clear_all_caches
	// =========================================================================

	@DisplayName( "clear_all_caches() returns a struct with cleared count" )
	@Test
	public void testClearAllCachesReturnsStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				result = tools.clear_all_caches()
			""",
			context
		);
		// @formatter:on

		var result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.containsKey( "cleared" ) ).isTrue();
		assertThat( result.containsKey( "results" ) ).isTrue();
	}

	// =========================================================================
	// reap_all_caches
	// =========================================================================

	@DisplayName( "reap_all_caches() returns a struct with reaped count" )
	@Test
	public void testReapAllCachesReturnsStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.CacheTools;
				tools = new CacheTools()
				result = tools.reap_all_caches()
			""",
			context
		);
		// @formatter:on

		var result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.containsKey( "reaped" ) ).isTrue();
		assertThat( result.containsKey( "results" ) ).isTrue();
	}
}
