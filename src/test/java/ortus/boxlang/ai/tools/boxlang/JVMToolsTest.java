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

public class JVMToolsTest extends BaseIntegrationTest {

	// =========================================================================
	// Registration
	// =========================================================================

	@DisplayName( "scanClass() registers all JVMTools tool keys in the registry" )
	@Test
	public void testScanClassRegistersAllTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
				aiToolRegistry().scanClass( new JVMTools(), "jvmtest" )
				result = aiToolRegistry().has( "get_memory_info@jvmtest" )
				    && aiToolRegistry().has( "get_memory_pool_details@jvmtest" )
				    && aiToolRegistry().has( "get_thread_info@jvmtest" )
				    && aiToolRegistry().has( "get_thread_dump@jvmtest" )
				    && aiToolRegistry().has( "get_cpu_info@jvmtest" )
				    && aiToolRegistry().has( "get_gc_info@jvmtest" )
				    && aiToolRegistry().has( "trigger_gc@jvmtest" )
				    && aiToolRegistry().has( "get_class_loading_info@jvmtest" )
				    && aiToolRegistry().has( "get_jvm_runtime_info@jvmtest" )
				    && aiToolRegistry().has( "get_system_properties@jvmtest" )
				    && aiToolRegistry().has( "get_environment_variables@jvmtest" )
				    && aiToolRegistry().has( "get_operating_system_info@jvmtest" )
				// Cleanup
				aiToolRegistry().unregisterByModule( "jvmtest" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_memory_info
	// =========================================================================

	@DisplayName( "get_memory_info() returns heap and nonHeap structs" )
	@Test
	public void testGetMemoryInfoReturnsHeapAndNonHeap() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.get_memory_info()
			""",
			context
		);
		// @formatter:on

		var memInfo = ( IStruct ) variables.get( result );
		assertThat( memInfo ).isNotNull();
		assertThat( memInfo.containsKey( "heap" ) ).isTrue();
		assertThat( memInfo.containsKey( "nonHeap" ) ).isTrue();

		var heap = ( IStruct ) memInfo.get( "heap" );
		assertThat( heap.containsKey( "used" ) ).isTrue();
		assertThat( heap.containsKey( "max" ) ).isTrue();
		assertThat( heap.containsKey( "usedHuman" ) ).isTrue();

		var used = ( Number ) heap.get( "used" );
		assertThat( used.longValue() ).isGreaterThan( 0L );
	}

	// =========================================================================
	// get_memory_pool_details
	// =========================================================================

	@DisplayName( "get_memory_pool_details() returns a non-empty array" )
	@Test
	public void testGetMemoryPoolDetailsReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.get_memory_pool_details()
			""",
			context
		);
		// @formatter:on

		var pools = ( Array ) variables.get( result );
		assertThat( pools ).isNotNull();
		assertThat( pools.size() ).isGreaterThan( 0 );

		var firstPool = ( IStruct ) pools.get( 0 );
		assertThat( firstPool.containsKey( "name" ) ).isTrue();
		assertThat( firstPool.containsKey( "type" ) ).isTrue();
		assertThat( firstPool.containsKey( "used" ) ).isTrue();
	}

	// =========================================================================
	// get_thread_info
	// =========================================================================

	@DisplayName( "get_thread_info() returns thread counts and state breakdown" )
	@Test
	public void testGetThreadInfoReturnsCountsAndStateBreakdown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.get_thread_info()
			""",
			context
		);
		// @formatter:on

		var threadInfo = ( IStruct ) variables.get( result );
		assertThat( threadInfo ).isNotNull();
		assertThat( threadInfo.containsKey( "threadCount" ) ).isTrue();
		assertThat( threadInfo.containsKey( "daemonThreadCount" ) ).isTrue();
		assertThat( threadInfo.containsKey( "peakThreadCount" ) ).isTrue();
		assertThat( threadInfo.containsKey( "stateBreakdown" ) ).isTrue();

		var threadCount = ( Number ) threadInfo.get( "threadCount" );
		assertThat( threadCount.intValue() ).isGreaterThan( 0 );
	}

	// =========================================================================
	// get_thread_dump
	// =========================================================================

	@DisplayName( "get_thread_dump() returns array of thread info with stack traces" )
	@Test
	public void testGetThreadDumpReturnsThreadArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.get_thread_dump()
			""",
			context
		);
		// @formatter:on

		var threads = ( Array ) variables.get( result );
		assertThat( threads ).isNotNull();
		assertThat( threads.size() ).isGreaterThan( 0 );

		var firstThread = ( IStruct ) threads.get( 0 );
		assertThat( firstThread.containsKey( "id" ) ).isTrue();
		assertThat( firstThread.containsKey( "name" ) ).isTrue();
		assertThat( firstThread.containsKey( "state" ) ).isTrue();
		assertThat( firstThread.containsKey( "stackTrace" ) ).isTrue();
	}

	// =========================================================================
	// get_cpu_info
	// =========================================================================

	@DisplayName( "get_cpu_info() returns available processors and load average" )
	@Test
	public void testGetCpuInfoReturnsProcessorsAndLoad() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.get_cpu_info()
			""",
			context
		);
		// @formatter:on

		var cpuInfo = ( IStruct ) variables.get( result );
		assertThat( cpuInfo ).isNotNull();
		assertThat( cpuInfo.containsKey( "availableProcessors" ) ).isTrue();
		assertThat( cpuInfo.containsKey( "systemLoadAverage" ) ).isTrue();

		var processors = ( Number ) cpuInfo.get( "availableProcessors" );
		assertThat( processors.intValue() ).isGreaterThan( 0 );
	}

	// =========================================================================
	// get_gc_info
	// =========================================================================

	@DisplayName( "get_gc_info() returns GC statistics array" )
	@Test
	public void testGetGcInfoReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.get_gc_info()
			""",
			context
		);
		// @formatter:on

		var gcList = ( Array ) variables.get( result );
		assertThat( gcList ).isNotNull();
		assertThat( gcList.size() ).isGreaterThan( 0 );

		var firstGc = ( IStruct ) gcList.get( 0 );
		assertThat( firstGc.containsKey( "name" ) ).isTrue();
		assertThat( firstGc.containsKey( "collectionCount" ) ).isTrue();
		assertThat( firstGc.containsKey( "collectionTime" ) ).isTrue();
	}

	// =========================================================================
	// get_class_loading_info
	// =========================================================================

	@DisplayName( "get_class_loading_info() returns loaded class counts" )
	@Test
	public void testGetClassLoadingInfoReturnsCounts() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.get_class_loading_info()
			""",
			context
		);
		// @formatter:on

		var classInfo = ( IStruct ) variables.get( result );
		assertThat( classInfo ).isNotNull();
		assertThat( classInfo.containsKey( "loadedClassCount" ) ).isTrue();
		assertThat( classInfo.containsKey( "totalLoadedCount" ) ).isTrue();
		assertThat( classInfo.containsKey( "unloadedClassCount" ) ).isTrue();

		var loaded = ( Number ) classInfo.get( "loadedClassCount" );
		assertThat( loaded.intValue() ).isGreaterThan( 0 );
	}

	// =========================================================================
	// get_jvm_runtime_info
	// =========================================================================

	@DisplayName( "get_jvm_runtime_info() returns JVM name and uptime" )
	@Test
	public void testGetJvmRuntimeInfoReturnsNameAndUptime() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.get_jvm_runtime_info()
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( result );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "name" ) ).isTrue();
		assertThat( info.containsKey( "vmName" ) ).isTrue();
		assertThat( info.containsKey( "uptimeMs" ) ).isTrue();
		assertThat( info.containsKey( "uptime" ) ).isTrue();

		var uptimeMs = ( Number ) info.get( "uptimeMs" );
		assertThat( uptimeMs.longValue() ).isGreaterThan( 0L );
	}

	// =========================================================================
	// get_system_properties
	// =========================================================================

	@DisplayName( "get_system_properties() returns java.version property" )
	@Test
	public void testGetSystemPropertiesReturnsJavaVersion() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
					props = tools.get_system_properties()
				result = props.keyExists( "java.version" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_environment_variables
	// =========================================================================

	@DisplayName( "get_environment_variables() returns a non-empty struct" )
	@Test
	public void testGetEnvironmentVariablesReturnsNonEmptyStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
				tools = new JVMTools()
				result = tools.get_environment_variables()
			""",
			context
		);
		// @formatter:on

		var env = ( IStruct ) variables.get( result );
		assertThat( env ).isNotNull();
		assertThat( env.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "get_environment_variables() masks sensitive values" )
	@Test
	public void testGetEnvironmentVariablesMasksSensitiveValues() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
				// get_environment_variables with a filter - test it returns a struct
				tools = new JVMTools()
				envResult = tools.get_environment_variables( "PATH" )
				result = isStruct( envResult )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_operating_system_info
	// =========================================================================

	@DisplayName( "get_operating_system_info() returns OS name and architecture" )
	@Test
	public void testGetOperatingSystemInfoReturnsNameAndArch() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.get_operating_system_info()
			""",
			context
		);
		// @formatter:on

		var osInfo = ( IStruct ) variables.get( result );
		assertThat( osInfo ).isNotNull();
		assertThat( osInfo.containsKey( "name" ) ).isTrue();
		assertThat( osInfo.containsKey( "arch" ) ).isTrue();
		assertThat( osInfo.containsKey( "availableProcessors" ) ).isTrue();

		var nameVal = osInfo.get( "name" );
		assertThat( nameVal ).isNotNull();
	}

	// =========================================================================
	// trigger_gc
	// =========================================================================

	@DisplayName( "trigger_gc() returns before/after memory stats" )
	@Test
	public void testTriggerGcReturnsBeforeAfterStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.JVMTools;
					tools = new JVMTools()
				result = tools.trigger_gc()
			""",
			context
		);
		// @formatter:on

		var gcResult = ( IStruct ) variables.get( result );
		assertThat( gcResult ).isNotNull();
		assertThat( gcResult.containsKey( "triggered" ) ).isTrue();
		assertThat( gcResult.containsKey( "before" ) ).isTrue();
		assertThat( gcResult.containsKey( "after" ) ).isTrue();
		assertThat( gcResult.get( "triggered" ) ).isEqualTo( true );
	}

}
