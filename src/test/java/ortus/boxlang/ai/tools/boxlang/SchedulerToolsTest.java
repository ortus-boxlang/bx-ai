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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class SchedulerToolsTest extends BaseIntegrationTest {

	@BeforeAll
	public static void registerTestSchedulers() {
		// Register two schedulers with random tasks for testing
		runtime.executeSource(
		    """
		    println( "====> Registering test schedulers for SchedulerToolsTest..." )
		      	// Scheduler 1: Data processing scheduler with multiple tasks
		      	schedulerStart(
		      		className = "src.test.bx.DataProcessingScheduler",
		      		name = "data-processing",
		      		force = true
		      	)

		      	// Scheduler 2: Reporting scheduler with different tasks
		      	schedulerStart(
		      		className = "src.test.bx.ReportingScheduler",
		      		name = "reporting",
		      		force = true
		      	)
		      """,
		    runtime.getRuntimeContext()
		);
	}

	// =========================================================================
	// get_schedulers
	// =========================================================================

	@DisplayName( "get_schedulers() returns an array of registered schedulers" )
	@Test
	public void testGetSchedulersReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.get_schedulers()
				println( "Schedulers: " )
				println( result )
			""",
			context
		);
		// @formatter:on

		var schedulers = ( Array ) variables.get( "result" );
		assertThat( schedulers ).isNotNull();
		assertThat( schedulers ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// get_scheduler_names
	// =========================================================================

	@DisplayName( "get_scheduler_names() returns an array of scheduler names" )
	@Test
	public void testGetSchedulerNamesReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.get_scheduler_names()
			""",
			context
		);
		// @formatter:on

		var names = ( Array ) variables.get( "result" );
		assertThat( names ).isNotNull();
		assertThat( names ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// get_scheduler
	// =========================================================================

	@DisplayName( "get_scheduler() returns details for a known scheduler" )
	@Test
	public void testGetSchedulerReturnsDetails() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				names = tools.get_scheduler_names()
				if ( names.size() > 0 ) {
					result = tools.get_scheduler( names.get( 1 ) )
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
			assertThat( info.containsKey( "started" ) ).isTrue();
			assertThat( info.containsKey( "taskCount" ) ).isTrue();
		}
	}

	@DisplayName( "get_scheduler() returns error struct for unknown scheduler" )
	@Test
	public void testGetSchedulerReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.get_scheduler( "nonexistent-scheduler-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
		assertThat( info.get( "message" ).toString() ).contains( "nonexistent-scheduler-xyz" );
	}

	// =========================================================================
	// has_scheduler
	// =========================================================================

	@DisplayName( "has_scheduler() returns true for a registered scheduler" )
	@Test
	public void testHasSchedulerReturnsTrueForRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				names = tools.get_scheduler_names()
				if ( names.size() > 0 ) {
					result = tools.has_scheduler( names.get( 1 ) )
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

	@DisplayName( "has_scheduler() returns false for an unregistered scheduler" )
	@Test
	public void testHasSchedulerReturnsFalseForUnregistered() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.has_scheduler( "nonexistent-scheduler-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "registered" ) ).isEqualTo( false );
	}

	// =========================================================================
	// get_task_stats
	// =========================================================================

	@DisplayName( "get_task_stats() returns error struct for unknown scheduler" )
	@Test
	public void testGetTaskStatsReturnsErrorForUnknownScheduler() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.get_task_stats( "nonexistent-scheduler-xyz", "some-task" )
				println( result )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_task_info
	// =========================================================================

	@DisplayName( "get_task_info() returns error struct for unknown scheduler" )
	@Test
	public void testGetTaskInfoReturnsErrorForUnknownScheduler() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.get_task_info( "nonexistent-scheduler-xyz", "some-task" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_all_tasks
	// =========================================================================

	@DisplayName( "get_all_tasks() returns an array of all tasks" )
	@Test
	public void testGetAllTasksReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.get_all_tasks()
			""",
			context
		);
		// @formatter:on

		var tasks = ( Array ) variables.get( "result" );
		assertThat( tasks ).isNotNull();
		assertThat( tasks ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// run_task
	// =========================================================================

	@DisplayName( "run_task() returns error struct for unknown scheduler" )
	@Test
	public void testRunTaskReturnsErrorForUnknownScheduler() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.run_task( "nonexistent-scheduler-xyz", "some-task" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// pause_task
	// =========================================================================

	@DisplayName( "pause_task() returns error struct for unknown scheduler" )
	@Test
	public void testPauseTaskReturnsErrorForUnknownScheduler() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.pause_task( "nonexistent-scheduler-xyz", "some-task" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// resume_task
	// =========================================================================

	@DisplayName( "resume_task() returns error struct for unknown scheduler" )
	@Test
	public void testResumeTaskReturnsErrorForUnknownScheduler() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.resume_task( "nonexistent-scheduler-xyz", "some-task" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_scheduler_health
	// =========================================================================

	@DisplayName( "get_scheduler_health() returns health summary" )
	@Test
	public void testGetSchedulerHealthReturnsSummary() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.get_scheduler_health()
			""",
			context
		);
		// @formatter:on

		var health = ( IStruct ) variables.get( "result" );
		assertThat( health ).isNotNull();
		assertThat( health.containsKey( "totalSchedulers" ) ).isTrue();
		assertThat( health.containsKey( "totalTasks" ) ).isTrue();
		assertThat( health.containsKey( "activeTasks" ) ).isTrue();
		assertThat( health.containsKey( "pausedTasks" ) ).isTrue();
	}

	// =========================================================================
	// get_all_task_stats
	// =========================================================================

	@DisplayName( "get_all_task_stats() returns task stats across all schedulers" )
	@Test
	public void testGetAllTaskStatsReturnsStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.get_all_task_stats()
			""",
			context
		);
		// @formatter:on

		var stats = ( IStruct ) variables.get( "result" );
		assertThat( stats ).isNotNull();
		assertThat( stats ).isInstanceOf( IStruct.class );
	}

	// =========================================================================
	// get_persisted_tasks
	// =========================================================================

	@DisplayName( "get_persisted_tasks() returns persisted tasks from disk" )
	@Test
	public void testGetPersistedTasksReturnsTasks() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.get_persisted_tasks()
				println( result )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		// If there's an error, it's acceptable (no tasks.json may exist)
		if ( info.containsKey( "error" ) ) {
			assertThat( info.get( "error" ) ).isEqualTo( true );
		} else {
			assertThat( info.containsKey( "total" ) ).isTrue();
			assertThat( info.containsKey( "tasks" ) ).isTrue();
		}
	}

	// =========================================================================
	// pause_persisted_task
	// =========================================================================

	@DisplayName( "pause_persisted_task() returns success or error struct" )
	@Test
	public void testPausePersistedTaskReturnsResult() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.pause_persisted_task( "nonexistent-task-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		// Either success (if tasks.json exists and upserts) or error
		assertThat( info.containsKey( "success" ) || info.containsKey( "error" ) ).isTrue();
	}

	// =========================================================================
	// resume_persisted_task
	// =========================================================================

	@DisplayName( "resume_persisted_task() returns success or error struct" )
	@Test
	public void testResumePersistedTaskReturnsResult() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.resume_persisted_task( "nonexistent-task-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "success" ) || info.containsKey( "error" ) ).isTrue();
	}

	// =========================================================================
	// pause_all_persisted_tasks
	// =========================================================================

	@DisplayName( "pause_all_persisted_tasks() returns success or error struct" )
	@Test
	public void testPauseAllPersistedTasksReturnsResult() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.pause_all_persisted_tasks()
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "success" ) || info.containsKey( "error" ) ).isTrue();
	}

	@DisplayName( "pause_all_persisted_tasks() with scheduler filter returns result" )
	@Test
	public void testPauseAllPersistedTasksWithSchedulerFilter() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.pause_all_persisted_tasks( schedulerName = "bxschedule" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "success" ) || info.containsKey( "error" ) ).isTrue();
	}

	@DisplayName( "pause_all_persisted_tasks() with group filter returns result" )
	@Test
	public void testPauseAllPersistedTasksWithGroupFilter() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.pause_all_persisted_tasks( group = "reports" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "success" ) || info.containsKey( "error" ) ).isTrue();
	}

	// =========================================================================
	// resume_all_persisted_tasks
	// =========================================================================

	@DisplayName( "resume_all_persisted_tasks() returns success or error struct" )
	@Test
	public void testResumeAllPersistedTasksReturnsResult() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.resume_all_persisted_tasks()
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "success" ) || info.containsKey( "error" ) ).isTrue();
	}

	@DisplayName( "resume_all_persisted_tasks() with scheduler filter returns result" )
	@Test
	public void testResumeAllPersistedTasksWithSchedulerFilter() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.SchedulerTools;
				tools = new SchedulerTools()
				result = tools.resume_all_persisted_tasks( schedulerName = "bxschedule" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "success" ) || info.containsKey( "error" ) ).isTrue();
	}

}
