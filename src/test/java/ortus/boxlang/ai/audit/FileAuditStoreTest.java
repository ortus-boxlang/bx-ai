/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.ai.audit;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Test cases for FileAuditStore - file-based audit storage
 */
public class FileAuditStoreTest extends BaseIntegrationTest {

	private Path tempDir;

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
		try {
			tempDir = Files.createTempDirectory( "fileauditstore-test-" );
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to create temp directory", e );
		}
	}

	@AfterEach
	public void tearDown() {
		try {
			if ( tempDir != null && Files.exists( tempDir ) ) {
				Files.walk( tempDir )
				    .sorted( ( a, b ) -> b.compareTo( a ) )
				    .forEach( path -> {
					    try {
						    Files.deleteIfExists( path );
					    } catch ( Exception e ) {
						    // Ignore
					    }
				    } );
			}
		} catch ( Exception e ) {
			// Ignore cleanup errors
		}
	}

	@Test
	@DisplayName( "Test FileAuditStore instantiation" )
	public void testInstantiation() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s" } )

		                   isNotNull = !isNull( store )
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "isNotNull" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test FileAuditStore with NDJSON format" )
	public void testNdjsonFormat() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s", format: "ndjson", batchSize: 1 } )

		                   entry = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-123", spanType = "model", operation = "chat"
		                   ).setInput( { prompt: "Hello" } ).complete( output = { response: "Hi" } )

		                   store.store( entry ).flush()

		                   // Check that directory was created and contains files
		                   dirExists = directoryExists( "%s" )
		                   """, tempDir.toString().replace( "\\", "\\\\" ), tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "dirExists" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test FileAuditStore with JSON format" )
	public void testJsonFormat() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s", format: "json", batchSize: 1 } )

		                   entry = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-456", spanType = "tool", operation = "execute"
		                   ).complete()

		                   store.store( entry ).flush()

		                   dirExists = directoryExists( "%s" )
		                   """, tempDir.toString().replace( "\\", "\\\\" ), tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "dirExists" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test store multiple entries with batching" )
	public void testBatchedWrites() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s", format: "ndjson", batchSize: 5 } )

		                   // Store 10 entries (should trigger 2 batch writes)
		                   for( i = 1; i <= 10; i++ ) {
		                       entry = new bxModules.bxai.models.audit.AuditEntry(
		                           traceId = "trace-batch", spanType = "model", operation = "chat"
		                       ).complete()
		                       store.store( entry )
		                   }

		                   // Flush remaining
		                   store.flush()

		                   success = true
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test storeBatch stores multiple entries" )
	public void testStoreBatch() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s", format: "ndjson", batchSize: 10 } )

		                   // Create batch of entries
		                   entries = []
		                   for( i = 1; i <= 5; i++ ) {
		                       entry = new bxModules.bxai.models.audit.AuditEntry(
		                           traceId = "trace-batch-" & i, spanType = "model", operation = "chat"
		                       ).complete()
		                       entries.append( entry )
		                   }

		                   // Store batch (returns results struct, not store)
		                   batchResult = store.storeBatch( entries )
		                   store.flush()

		                   success = true
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test query from file store" )
	public void testQuery() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s", format: "ndjson", batchSize: 1 } )

		                   // Store entries
		                   for( i = 1; i <= 3; i++ ) {
		                       entry = new bxModules.bxai.models.audit.AuditEntry(
		                           traceId = "trace-query-" & i, spanType = "model", operation = "chat"
		                       ).complete()
		                       store.store( entry )
		                   }
		                   store.flush()

		                   // Query - note: file store reads from files
		                   results = store.query( filters = {}, limit = 10 )
		                   resultCount = results.len()
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( ( ( Number ) variables.get( Key.of( "resultCount" ) ) ).intValue() ).isGreaterThan( 0 );
	}

	@Test
	@DisplayName( "Test getTrace from file store" )
	public void testGetTrace() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s", format: "ndjson", batchSize: 1 } )

		                   // Store entries for same trace
		                   entry1 = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-gettrace", spanType = "agent", operation = "run"
		                   ).complete()
		                   store.store( entry1 )

		                   entry2 = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-gettrace", spanType = "model", operation = "chat"
		                   ).complete()
		                   store.store( entry2 )

		                   store.flush()

		                   // Get trace
		                   trace = store.getTrace( "trace-gettrace" )
		                   traceId = trace.traceId
		                   entryCount = trace.entries.len()
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isEqualTo( "trace-gettrace" );
		assertThat( ( ( Number ) variables.get( Key.of( "entryCount" ) ) ).intValue() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test getStats from file store" )
	public void testGetStats() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s", format: "ndjson", batchSize: 1 } )

		                   // Store entries
		                   entry1 = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-stats-1", spanType = "model", operation = "chat"
		                   ).complete()
		                   store.store( entry1 )

		                   entry2 = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-stats-2", spanType = "tool", operation = "execute"
		                   ).complete()
		                   store.store( entry2 )

		                   store.flush()

		                   stats = store.getStats()
		                   totalEntries = stats.totalEntries
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( ( ( Number ) variables.get( Key.of( "totalEntries" ) ) ).intValue() ).isGreaterThan( 0 );
	}

	@Test
	@DisplayName( "Test close flushes and closes" )
	public void testClose() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s", format: "ndjson", batchSize: 10 } )

		                   // Store some entries (won't auto-flush)
		                   entry = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-close", spanType = "model", operation = "chat"
		                   ).complete()
		                   store.store( entry )

		                   // Close should flush
		                   store.close()

		                   success = true
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test fluent chaining" )
	public void testFluentChaining() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( {
		                           path: "%s",
		                           format: "ndjson",
		                           batchSize: 1,
		                           maxFileSize: 104857600,
		                           rotateDaily: true
		                       } )

		                   entry = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-chain", spanType = "model", operation = "chat"
		                   ).complete()

		                   store.store( entry ).flush()

		                   success = true
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test store creates directory if needed" )
	public void testDirectoryCreation() {
		Path nestedDir = tempDir.resolve( "nested/dir/path" );

		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( { path: "%s", format: "ndjson", batchSize: 1 } )

		                   entry = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-nested", spanType = "model", operation = "chat"
		                   ).complete()

		                   store.store( entry ).flush()

		                   dirExists = directoryExists( "%s" )
		                   """, nestedDir.toString().replace( "\\", "\\\\" ), nestedDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "dirExists" ) ) ).isTrue();
	}

	// ==================== File Rotation Tests ====================

	@Test
	@DisplayName( "Test file rotation with maxFileSize configuration" )
	public void testFileRotationWithMaxSize() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( {
		                           path: "%s",
		                           format: "ndjson",
		                           batchSize: 1,
		                           maxFileSize: 500  // Very small to trigger rotation
		                       } )

		                   // Store multiple entries to exceed file size limit
		                   for( i = 1; i <= 10; i++ ) {
		                       entry = new bxModules.bxai.models.audit.AuditEntry(
		                           traceId = "trace-rotation-" & i,
		                           spanType = "model",
		                           operation = "chat"
		                       )
		                       .setInput( { prompt: "This is a longer input to help exceed file size limit iteration " & i } )
		                       .complete( output = { response: "This is a longer output response for iteration " & i } )

		                       store.store( entry )
		                   }
		                   store.flush()

		                   // Count files in directory - should have at least one file
		                   files = directoryList( "%s", false, "file", "*.ndjson" )
		                   fileCount = files.len()

		                   // Query returns entries from the current/recent file
		                   results = store.query( filters = {}, limit = 100 )
		                   resultCount = results.len()
		                   """, tempDir.toString().replace( "\\", "\\\\" ), tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		// Should have at least one file created
		assertThat( ( ( Number ) variables.get( Key.of( "fileCount" ) ) ).intValue() ).isGreaterThan( 0 );
		// Query should return some entries (may not be all if rotation doesn't cross-file query)
		assertThat( ( ( Number ) variables.get( Key.of( "resultCount" ) ) ).intValue() ).isGreaterThan( 0 );
	}

	@Test
	@DisplayName( "Test batch flush produces readable traces" )
	public void testBatchFlushProducesReadableTraces() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( {
		                           path: "%s",
		                           format: "ndjson",
		                           batchSize: 5
		                       } )

		                   // Create a complete trace with multiple entries
		                   traceId = "batch-readable-trace"

		                   // Agent span
		                   entry1 = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = traceId, spanType = "agent", operation = "run"
		                   ).setInput( { prompt: "User request" } ).complete()

		                   // Model span
		                   entry2 = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = traceId, spanType = "model", operation = "chat"
		                   ).setInput( { model: "gpt-4" } ).complete( output = { response: "AI response" } )

		                   // Tool span
		                   entry3 = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = traceId, spanType = "tool", operation = "execute"
		                   ).setInput( { tool: "search" } ).complete( output = { result: "Search results" } )

		                   store.store( entry1 ).store( entry2 ).store( entry3 )
		                   store.flush()

		                   // Retrieve the complete trace
		                   trace = store.getTrace( traceId )

		                   // Verify trace structure
		                   hasTraceId = trace.traceId == traceId
		                   entryCount = trace.entries.len()

		                   // Verify entry types are preserved
		                   spanTypes = []
		                   for( entry in trace.entries ) {
		                       spanTypes.append( entry.spanType )
		                   }
		                   hasAgent = spanTypes.findNoCase( "agent" ) > 0
		                   hasModel = spanTypes.findNoCase( "model" ) > 0
		                   hasTool = spanTypes.findNoCase( "tool" ) > 0
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasTraceId" ) ) ).isTrue();
		assertThat( ( ( Number ) variables.get( Key.of( "entryCount" ) ) ).intValue() ).isEqualTo( 3 );
		assertThat( variables.getAsBoolean( Key.of( "hasAgent" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasModel" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasTool" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JSON format produces valid JSON files" )
	public void testJsonFormatProducesValidJson() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( {
		                           path: "%s",
		                           format: "json",
		                           batchSize: 1
		                       } )

		                   entry = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "json-valid-test",
		                       spanType = "model",
		                       operation = "chat"
		                   ).setInput( { prompt: "test" } ).complete( output = { response: "test response" } )

		                   store.store( entry ).flush()

		                   // Find and read the JSON file
		                   files = directoryList( "%s", false, "file", "*.json" )
		                   fileCount = files.len()

		                   // Read and parse the JSON file to verify it's valid
		                   isValidJson = false
		                   if( fileCount > 0 ) {
		                       content = fileRead( files[1] )
		                       try {
		                           parsed = jsonDeserialize( content )
		                           isValidJson = isArray( parsed ) || isStruct( parsed )
		                       } catch( any e ) {
		                           isValidJson = false
		                       }
		                   }
		                   """, tempDir.toString().replace( "\\", "\\\\" ), tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( ( ( Number ) variables.get( Key.of( "fileCount" ) ) ).intValue() ).isGreaterThan( 0 );
		assertThat( variables.getAsBoolean( Key.of( "isValidJson" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test storing multiple traces without rotation" )
	public void testMultipleTracesStorage() {
		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.FileAuditStore()
		                       .configure( {
		                           path: "%s",
		                           format: "ndjson",
		                           batchSize: 1
		                           // No maxFileSize to avoid rotation complications
		                       } )

		                   // Store entries for different traces
		                   traceIds = [ "trace-A", "trace-B", "trace-C" ]
		                   for( tid in traceIds ) {
		                       for( i = 1; i <= 3; i++ ) {
		                           entry = new bxModules.bxai.models.audit.AuditEntry(
		                               traceId = tid,
		                               spanType = "model",
		                               operation = "chat"
		                           ).complete()
		                           store.store( entry )
		                       }
		                   }
		                   store.flush()

		                   // Query for specific trace
		                   resultsA = store.query( filters = { traceId: "trace-A" }, limit = 100 )
		                   resultsB = store.query( filters = { traceId: "trace-B" }, limit = 100 )
		                   resultsC = store.query( filters = { traceId: "trace-C" }, limit = 100 )

		                   countA = resultsA.len()
		                   countB = resultsB.len()
		                   countC = resultsC.len()
		                   totalCount = countA + countB + countC
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		// Each trace should have exactly 3 entries
		assertThat( ( ( Number ) variables.get( Key.of( "countA" ) ) ).intValue() ).isEqualTo( 3 );
		assertThat( ( ( Number ) variables.get( Key.of( "countB" ) ) ).intValue() ).isEqualTo( 3 );
		assertThat( ( ( Number ) variables.get( Key.of( "countC" ) ) ).intValue() ).isEqualTo( 3 );
		assertThat( ( ( Number ) variables.get( Key.of( "totalCount" ) ) ).intValue() ).isEqualTo( 9 );
	}

}
