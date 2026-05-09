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

public class LoggingToolsTest extends BaseIntegrationTest {

	// =========================================================================
	// get_logging_info
	// =========================================================================

	@DisplayName( "get_logging_info() returns a struct with logging system details" )
	@Test
	public void testGetLoggingInfoReturnsStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.get_logging_info()
				println( result )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.containsKey( "logsDirectory" ) ).isTrue();
		assertThat( info.containsKey( "loggerCount" ) ).isTrue();
		assertThat( info.containsKey( "loggers" ) ).isTrue();
	}

	// =========================================================================
	// get_logging_config
	// =========================================================================

	@DisplayName( "get_logging_config() returns the runtime logging configuration" )
	@Test
	public void testGetLoggingConfigReturnsConfig() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.get_logging_config()
			""",
			context
		);
		// @formatter:on

		var config = ( IStruct ) variables.get( "result" );
		assertThat( config ).isNotNull();
		assertThat( config.containsKey( "rootLevel" ) ).isTrue();
	}

	// =========================================================================
	// get_loggers
	// =========================================================================

	@DisplayName( "get_loggers() returns an array of logger details" )
	@Test
	public void testGetLoggersReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.get_loggers()
			""",
			context
		);
		// @formatter:on

		var loggers = ( Array ) variables.get( "result" );
		assertThat( loggers ).isNotNull();
		assertThat( loggers ).isInstanceOf( Array.class );
		assertThat( loggers.size() ).isAtLeast( 1 );
	}

	// =========================================================================
	// get_logger
	// =========================================================================

	@DisplayName( "get_logger() returns details for a known logger" )
	@Test
	public void testGetLoggerReturnsDetails() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.get_logger( "runtime" )
				println( result )
			""",
			context
		);
		// @formatter:on

		var logger = ( IStruct ) variables.get( "result" );
		assertThat( logger ).isNotNull();
		assertThat( logger.containsKey( "name" ) ).isTrue();
	}

	@DisplayName( "get_logger() returns error struct for unknown logger" )
	@Test
	public void testGetLoggerReturnsErrorForUnknown() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.get_logger( "nonexistent-logger-xyz" )
			""",
			context
		);
		// @formatter:on

		var info = ( IStruct ) variables.get( "result" );
		assertThat( info ).isNotNull();
		assertThat( info.get( "error" ) ).isEqualTo( true );
	}

	// =========================================================================
	// get_root_logger
	// =========================================================================

	@DisplayName( "get_root_logger() returns root logger details" )
	@Test
	public void testGetRootLoggerReturnsDetails() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.get_root_logger()
			""",
			context
		);
		// @formatter:on

		var root = ( IStruct ) variables.get( "result" );
		assertThat( root ).isNotNull();
		assertThat( root.get( "name" ) ).isEqualTo( "ROOT" );
	}

	// =========================================================================
	// get_appenders
	// =========================================================================

	@DisplayName( "get_appenders() returns an array of appender details" )
	@Test
	public void testGetAppendersReturnsArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.get_appenders()
			""",
			context
		);
		// @formatter:on

		var appenders = ( Array ) variables.get( "result" );
		assertThat( appenders ).isNotNull();
		assertThat( appenders ).isInstanceOf( Array.class );
	}

	// =========================================================================
	// read_log_entries
	// =========================================================================

	@DisplayName( "read_log_entries() returns entries from a logger's log file" )
	@Test
	public void testReadLogEntriesReturnsEntries() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.read_log_entries( "runtime", 10 )
			""",
			context
		);
		// @formatter:on

		var result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.containsKey( "loggerName" ) ).isTrue();
		assertThat( result.containsKey( "logFile" ) ).isTrue();
		assertThat( result.containsKey( "entries" ) ).isTrue();
	}

	// =========================================================================
	// get_last_error
	// =========================================================================

	@DisplayName( "get_last_error() searches for errors in a logger's log file" )
	@Test
	public void testGetLastErrorSearchesLogFile() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.get_last_error( "runtime" )
			""",
			context
		);
		// @formatter:on

		var result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.containsKey( "loggerName" ) ).isTrue();
		assertThat( result.containsKey( "logFile" ) ).isTrue();
	}

	// =========================================================================
	// search_log_entries
	// =========================================================================

	@DisplayName( "search_log_entries() finds matching log entries" )
	@Test
	public void testSearchLogEntriesFindsMatches() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.search_log_entries( "runtime", "BoxLang", 10 )
			""",
			context
		);
		// @formatter:on

		var result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.containsKey( "keyword" ) ).isTrue();
		assertThat( result.containsKey( "matches" ) ).isTrue();
	}

	// =========================================================================
	// log_message
	// =========================================================================

	@DisplayName( "log_message() writes a test message to a logger" )
	@Test
	public void testLogMessageWritesToLogger() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.boxlang.LoggingTools;
				tools = new LoggingTools()
				result = tools.log_message( "test-logging", "info", "Test message from LoggingToolsTest" )
			""",
			context
		);
		// @formatter:on

		var result = variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.toString() ).contains( "logged" );
	}
}
