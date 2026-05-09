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

public class AsyncToolsTest extends BaseIntegrationTest {

	// =========================================================================
	// Registration
	// =========================================================================

	@DisplayName( "scanClass() registers all AsyncTools tool keys in the registry" )
	@Test
	public void testScanClassRegistersAllTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				aiToolRegistry().scanClass( new AsyncTools(), "bxtest" )
				result = aiToolRegistry().has( "get_executors@bxtest" )
				    && aiToolRegistry().has( "get_executor_names@bxtest" )
				    && aiToolRegistry().has( "get_executor_info@bxtest" )
				    && aiToolRegistry().has( "get_executor_stats@bxtest" )
				    && aiToolRegistry().has( "get_executor_health@bxtest" )
				    && aiToolRegistry().has( "get_executor_health_summary@bxtest" )
				// Cleanup
				aiToolRegistry().unregisterByModule( "bxtest" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_executors
	// =========================================================================

	@DisplayName( "get_executors() returns a struct of executor stats" )
	@Test
	public void testGetExecutorsReturnsStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				tools = new AsyncTools()
				result = tools.get_executors()
			""",
			context
		);
		// @formatter:on

		var executors = ( IStruct ) variables.get( "result" );
		assertThat( executors ).isNotNull();
		assertThat( executors ).isInstanceOf( IStruct.class );
	}

	// =========================================================================
	// get_executor_names
	// =========================================================================

	@DisplayName( "get_executor_names() returns a sorted array of executor names" )
	@Test
	public void testGetExecutorNamesReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				tools = new AsyncTools()
				result = tools.get_executor_names()
			""",
			context
		);
		// @formatter:on

		var names = ( Array ) variables.get( "result" );
		assertThat( names ).isNotNull();
		assertThat( names ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// get_executor_info
	// =========================================================================

	@DisplayName( "get_executor_info() returns details for a known executor" )
	@Test
	public void testGetExecutorInfoReturnsDetails() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				tools = new AsyncTools()
				names = tools.get_executor_names()
				if ( names.len() > 0 ) {
					result = tools.get_executor_info( names.get( 1 ) )
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
			assertThat( info.containsKey( "name" ) ).isTrue();
			assertThat( info.containsKey( "type" ) ).isTrue();
			assertThat( info.containsKey( "isHealthy" ) ).isTrue();
		}
	}

	@DisplayName( "get_executor_info() returns error struct for unknown executor" )
	@Test
	public void testGetExecutorInfoReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				tools = new AsyncTools()
				result = tools.get_executor_info( "nonexistent-executor-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
		assertThat( info.get( "message" ).toString() ).contains( "nonexistent-executor-xyz" );
	}

	// =========================================================================
	// get_executor_stats
	// =========================================================================

	@DisplayName( "get_executor_stats() returns stats for a known executor" )
	@Test
	public void testGetExecutorStatsReturnsStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				tools = new AsyncTools()
				names = tools.get_executor_names()
				if ( names.len() > 0 ) {
					result = tools.get_executor_stats( names.get( 1 ) )
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
			assertThat( stats.containsKey( "healthStatus" ) ).isTrue();
			assertThat( stats.containsKey( "healthReport" ) ).isTrue();
		}
	}

	@DisplayName( "get_executor_stats() returns error struct for unknown executor" )
	@Test
	public void testGetExecutorStatsReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				tools = new AsyncTools()
				result = tools.get_executor_stats( "nonexistent-executor-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_executor_health
	// =========================================================================

	@DisplayName( "get_executor_health() returns health report for a known executor" )
	@Test
	public void testGetExecutorHealthReturnsReport() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				tools = new AsyncTools()
				names = tools.get_executor_names()
				if ( names.len() > 0 ) {
					result = tools.get_executor_health( names.get( 1 ) )
				} else {
					result = { "skipped" : true }
				}
				println( result )
			""",
			context
		);
		// @formatter:on

		var report = ( IStruct ) variables.get( "result" );
		assertThat( report ).isNotNull();
		if ( !report.containsKey( "skipped" ) ) {
			assertThat( report.containsKey( "status" ) ).isTrue();
			assertThat( report.containsKey( "issues" ) ).isTrue();
			assertThat( report.containsKey( "recommendations" ) ).isTrue();
		}
	}

	@DisplayName( "get_executor_health() returns error struct for unknown executor" )
	@Test
	public void testGetExecutorHealthReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				tools = new AsyncTools()
				result = tools.get_executor_health( "nonexistent-executor-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_executor_health_summary
	// =========================================================================

	@DisplayName( "get_executor_health_summary() returns a summary struct with counts" )
	@Test
	public void testGetExecutorHealthSummaryReturnsSummary() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.AsyncTools;
				tools = new AsyncTools()
				result = tools.get_executor_health_summary()
			""",
			context
		);
		// @formatter:on

		var summary = ( IStruct ) variables.get( "result" );
		assertThat( summary ).isNotNull();
		assertThat( summary.containsKey( "total" ) ).isTrue();
		assertThat( summary.containsKey( "healthy" ) ).isTrue();
		assertThat( summary.containsKey( "details" ) ).isTrue();
	}
}
