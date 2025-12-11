package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "LogLoader Tests" )
public class LogLoaderTest extends BaseIntegrationTest {

	@DisplayName( "LogLoader can load log file with one document per line" )
	@Test
	public void testLogLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.LogLoader;
				
				// Create a test log file
				logPath = getTempFile( getTempDirectory(), "test" ) & ".log";
				fileWrite( logPath, "[2025-12-11 10:00:00] [INFO] [MyApp] Application started
[2025-12-11 10:00:01] [DEBUG] [MyApp] Loading configuration
[2025-12-11 10:00:02] [ERROR] [MyApp] Failed to connect" );
				
				loader = new LogLoader( source: logPath );
				rawDocs = loader.load();
				
				result = {
					count: rawDocs.len(),
					docs: rawDocs.map( d => d.toStruct() )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		int		count	= result.getAsInteger( Key.of( "count" ) );
		
		// Should have 3 documents (one per line)
		assertThat( count ).isEqualTo( 3 );
		
		Array docs = result.getAsArray( Key.of( "docs" ) );
		IStruct firstDoc = ( IStruct ) docs.get( 0 );
		IStruct metadata = firstDoc.getAsStruct( Key.of( "metadata" ) );
		
		// Verify metadata was parsed
		assertThat( metadata.getAsString( Key.of( "level" ) ) ).isEqualTo( "INFO" );
		assertThat( metadata.getAsString( Key.of( "logger" ) ) ).isEqualTo( "MyApp" );
		assertThat( metadata.getAsInteger( Key.of( "lineNumber" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "LogLoader can filter by log level" )
	@Test
	public void testLogLoaderLevelFilter() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.LogLoader;
				
				logPath = getTempFile( getTempDirectory(), "test" ) & ".log";
				fileWrite( logPath, "[2025-12-11 10:00:00] [INFO] [MyApp] Info message
[2025-12-11 10:00:01] [DEBUG] [MyApp] Debug message
[2025-12-11 10:00:02] [ERROR] [MyApp] Error message
[2025-12-11 10:00:03] [WARN] [MyApp] Warning message" );
				
				loader = new LogLoader( source: logPath )
					.minLevel( "WARN" );
				rawDocs = loader.load();
				
				result = {
					count: rawDocs.len(),
					levels: rawDocs.map( d => d.getMetadata().level )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		int		count	= result.getAsInteger( Key.of( "count" ) );
		
		// Should only have WARN and ERROR (2 lines)
		assertThat( count ).isEqualTo( 2 );
		
		Array levels = result.getAsArray( Key.of( "levels" ) );
		assertThat( levels.get( 0 ) ).isEqualTo( "ERROR" );
		assertThat( levels.get( 1 ) ).isEqualTo( "WARN" );
	}

	@DisplayName( "LogLoader can filter by logger name" )
	@Test
	public void testLogLoaderLoggerFilter() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.LogLoader;
				
				logPath = getTempFile( getTempDirectory(), "test" ) & ".log";
				fileWrite( logPath, "[2025-12-11 10:00:00] [INFO] [App1] Message from App1
[2025-12-11 10:00:01] [INFO] [App2] Message from App2
[2025-12-11 10:00:02] [INFO] [App1] Another from App1" );
				
				loader = new LogLoader( source: logPath )
					.includeLoggers( ["App1"] );
				rawDocs = loader.load();
				
				result = {
					count: rawDocs.len(),
					loggers: rawDocs.map( d => d.getMetadata().logger )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		int		count	= result.getAsInteger( Key.of( "count" ) );
		
		// Should only have App1 messages (2 lines)
		assertThat( count ).isEqualTo( 2 );
		
		Array loggers = result.getAsArray( Key.of( "loggers" ) );
		assertThat( loggers.get( 0 ) ).isEqualTo( "App1" );
		assertThat( loggers.get( 1 ) ).isEqualTo( "App1" );
	}

	@DisplayName( "LogLoader can exclude specific loggers" )
	@Test
	public void testLogLoaderExcludeLoggers() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.LogLoader;
				
				logPath = getTempFile( getTempDirectory(), "test" ) & ".log";
				fileWrite( logPath, "[2025-12-11 10:00:00] [INFO] [App1] Message from App1
[2025-12-11 10:00:01] [INFO] [App2] Message from App2
[2025-12-11 10:00:02] [INFO] [App3] Message from App3" );
				
				loader = new LogLoader( source: logPath )
					.excludeLoggers( ["App2"] );
				rawDocs = loader.load();
				
				result = {
					count: rawDocs.len(),
					loggers: rawDocs.map( d => d.getMetadata().logger )
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct	result	= ( IStruct ) variables.get( "result" );
		int		count	= result.getAsInteger( Key.of( "count" ) );
		
		// Should have App1 and App3, but not App2 (2 lines)
		assertThat( count ).isEqualTo( 2 );
		
		Array loggers = result.getAsArray( Key.of( "loggers" ) );
		assertThat( loggers.get( 0 ) ).isEqualTo( "App1" );
		assertThat( loggers.get( 1 ) ).isEqualTo( "App3" );
	}

	@DisplayName( "LogLoader skips empty lines by default" )
	@Test
	public void testLogLoaderSkipEmptyLines() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.LogLoader;
				
				logPath = getTempFile( getTempDirectory(), "test" ) & ".log";
				fileWrite( logPath, "[2025-12-11 10:00:00] [INFO] [MyApp] First line

[2025-12-11 10:00:02] [INFO] [MyApp] Third line" );
				
				loader = new LogLoader( source: logPath );
				rawDocs = loader.load();
				
				result = rawDocs.len();
		    """,
		    context
		);
		// @formatter:on

		int count = variables.getAsInteger( result );
		
		// Should skip the empty line (2 documents)
		assertThat( count ).isEqualTo( 2 );
	}

	@DisplayName( "LogLoader can parse structured log format" )
	@Test
	public void testLogLoaderStructuredParsing() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.LogLoader;
				
				logPath = getTempFile( getTempDirectory(), "test" ) & ".log";
				fileWrite( logPath, "[2025-12-11 10:00:00] [ERROR] [ortus.boxlang.runtime] Connection timeout occurred" );
				
				loader = new LogLoader( source: logPath );
				rawDocs = loader.load();
				doc = rawDocs[1];
				
				result = {
					content: doc.getContent(),
					timestamp: doc.getMetadata().timestamp,
					level: doc.getMetadata().level,
					logger: doc.getMetadata().logger
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		
		assertThat( result.getAsString( Key.of( "content" ) ) ).contains( "Connection timeout" );
		assertThat( result.getAsString( Key.of( "timestamp" ) ) ).isEqualTo( "2025-12-11 10:00:00" );
		assertThat( result.getAsString( Key.of( "level" ) ) ).isEqualTo( "ERROR" );
		assertThat( result.getAsString( Key.of( "logger" ) ) ).isEqualTo( "ortus.boxlang.runtime" );
	}

	@DisplayName( "LogLoader can disable structure parsing" )
	@Test
	public void testLogLoaderNoStructureParsing() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.LogLoader;
				
				logPath = getTempFile( getTempDirectory(), "test" ) & ".log";
				fileWrite( logPath, "[2025-12-11 10:00:00] [ERROR] [MyApp] Error message" );
				
				loader = new LogLoader( source: logPath )
					.parseStructure( false );
				rawDocs = loader.load();
				doc = rawDocs[1];
				
				result = {
					content: doc.getContent(),
					level: doc.getMetadata().level
				};
		    """,
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		
		// Content should be the entire line when parsing is disabled
		assertThat( result.getAsString( Key.of( "content" ) ) ).contains( "[2025-12-11 10:00:00]" );
		// Level should be empty when parsing is disabled
		assertThat( result.getAsString( Key.of( "level" ) ) ).isEmpty();
	}

	@DisplayName( "LogLoader handles file not found error" )
	@Test
	public void testLogLoaderFileNotFound() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.LogLoader;
				
				try {
					loader = new LogLoader( source: "/nonexistent/file.log" );
					rawDocs = loader.load();
					result = false;
				} catch( any e ) {
					result = e.type == "LogLoader.FileNotFound";
				}
		    """,
		    context
		);
		// @formatter:on

		boolean threwCorrectError = variables.getAsBoolean( result );
		assertThat( threwCorrectError ).isTrue();
	}

}
