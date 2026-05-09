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

public class DatasourceToolsTest extends BaseIntegrationTest {

	// =========================================================================
	// get_datasources
	// =========================================================================

	@DisplayName( "get_datasources() returns an array of registered datasources" )
	@Test
	public void testGetDatasourcesReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				result = tools.get_datasources()
			""",
			context
		);
		// @formatter:on

		var datasources = ( Array ) variables.get( "result" );
		assertThat( datasources ).isNotNull();
		assertThat( datasources ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// get_datasource_names
	// =========================================================================

	@DisplayName( "get_datasource_names() returns an array of datasource names" )
	@Test
	public void testGetDatasourceNamesReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				result = tools.get_datasource_names()
			""",
			context
		);
		// @formatter:on

		var names = ( Array ) variables.get( "result" );
		assertThat( names ).isNotNull();
		assertThat( names ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// get_datasource
	// =========================================================================

	@DisplayName( "get_datasource() returns details for a known datasource" )
	@Test
	public void testGetDatasourceReturnsDetails() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				names = tools.get_datasource_names()
				if ( names.size() > 0 ) {
					result = tools.get_datasource( names.get( 1 ) )
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
			assertThat( info.containsKey( "driver" ) ).isTrue();
			assertThat( info.containsKey( "poolingStarted" ) ).isTrue();
			assertThat( info.containsKey( "poolMetrics" ) ).isTrue();
		}
	}

	@DisplayName( "get_datasource() returns error struct for unknown datasource" )
	@Test
	public void testGetDatasourceReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				result = tools.get_datasource( "nonexistent-datasource-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
		assertThat( info.get( "message" ).toString() ).contains( "nonexistent-datasource-xyz" );
	}

	// =========================================================================
	// has_datasource
	// =========================================================================

	@DisplayName( "has_datasource() returns true for a registered datasource" )
	@Test
	public void testHasDatasourceReturnsTrueForRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				names = tools.get_datasource_names()
				if ( names.size() > 0 ) {
					result = tools.has_datasource( names.get( 1 ) )
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

	@DisplayName( "has_datasource() returns false for an unregistered datasource" )
	@Test
	public void testHasDatasourceReturnsFalseForUnregistered() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				result = tools.has_datasource( "nonexistent-datasource-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "registered" ) ).isEqualTo( false );
	}

	// =========================================================================
	// get_pool_metrics
	// =========================================================================

	@DisplayName( "get_pool_metrics() returns pool stats for a known datasource" )
	@Test
	public void testGetPoolMetricsReturnsStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				names = tools.get_datasource_names()
				if ( names.size() > 0 ) {
					result = tools.get_pool_metrics( names.get( 1 ) )
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
			assertThat( stats.containsKey( "poolingStarted" ) ).isTrue();
			assertThat( stats.containsKey( "activeConnections" ) ).isTrue();
			assertThat( stats.containsKey( "idleConnections" ) ).isTrue();
		}
	}

	@DisplayName( "get_pool_metrics() returns error struct for unknown datasource" )
	@Test
	public void testGetPoolMetricsReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				result = tools.get_pool_metrics( "nonexistent-datasource-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_datasource_config
	// =========================================================================

	@DisplayName( "get_datasource_config() returns config with masked credentials" )
	@Test
	public void testGetDatasourceConfigMasksCredentials() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				names = tools.get_datasource_names()
				if ( names.size() > 0 ) {
					result = tools.get_datasource_config( names.get( 1 ) )
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
			assertThat( info.containsKey( "config" ) ).isTrue();
			var config = ( IStruct ) info.get( "config" );
			// Verify sensitive fields are masked
			if ( config.containsKey( "password" ) ) {
				assertThat( config.get( "password" ).toString() ).isEqualTo( "********" );
			}
			if ( config.containsKey( "username" ) ) {
				assertThat( config.get( "username" ).toString() ).isEqualTo( "********" );
			}
		}
	}

	@DisplayName( "get_datasource_config() returns error struct for unknown datasource" )
	@Test
	public void testGetDatasourceConfigReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				result = tools.get_datasource_config( "nonexistent-datasource-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// test_datasource
	// =========================================================================

	@DisplayName( "test_datasource() returns error struct for unknown datasource" )
	@Test
	public void testTestDatasourceReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				result = tools.test_datasource( "nonexistent-datasource-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_datasource_health
	// =========================================================================

	@DisplayName( "get_datasource_health() returns health summary" )
	@Test
	public void testGetDatasourceHealthReturnsSummary() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.DatasourceTools;
				tools = new DatasourceTools()
				result = tools.get_datasource_health()
			""",
			context
		);
		// @formatter:on

		var health = ( IStruct ) variables.get( "result" );
		assertThat( health ).isNotNull();
		assertThat( health.containsKey( "totalDatasources" ) ).isTrue();
		assertThat( health.containsKey( "poolingStarted" ) ).isTrue();
		assertThat( health.containsKey( "totalActiveConnections" ) ).isTrue();
		assertThat( health.containsKey( "totalIdleConnections" ) ).isTrue();
		assertThat( health.containsKey( "utilizationPercent" ) ).isTrue();
	}

}
