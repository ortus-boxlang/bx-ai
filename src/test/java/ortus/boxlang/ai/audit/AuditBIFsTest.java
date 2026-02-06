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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Test cases for audit BIF functions: aiAudit, aiAuditQuery, aiAuditExport
 */
public class AuditBIFsTest extends BaseIntegrationTest {

	private Path tempDir;

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
		try {
			tempDir = Files.createTempDirectory( "auditbifs-test-" );
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

	// ==================== aiAudit() Tests ====================

	@Test
	@DisplayName( "Test aiAudit() creates context with default traceId" )
	public void testAiAuditDefaultTraceId() {
		runtime.executeSource(
		    """
		    context = aiAudit()
		    traceId = context.getTraceId()
		    isRecording = context.isRecording()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isNotEmpty();
		assertThat( variables.getAsBoolean( Key.of( "isRecording" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test aiAudit() with custom traceId" )
	public void testAiAuditCustomTraceId() {
		runtime.executeSource(
		    """
		    context = aiAudit( traceId = "custom-trace-123" )
		    traceId = context.getTraceId()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isEqualTo( "custom-trace-123" );
	}

	@Test
	@DisplayName( "Test aiAudit() with memory store type" )
	public void testAiAuditMemoryStore() {
		runtime.executeSource(
		    """
		    context = aiAudit( store = "memory" )

		    // Add some entries
		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan( output = "response" )

		    entries = context.getEntries()
		    entryCount = entries.len()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "entryCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test aiAudit() with file store type" )
	public void testAiAuditFileStore() {
		runtime.executeSource(
		    String.format( """
		                   context = aiAudit(
		                       store  = "file",
		                       config = { storeConfig: { path: "%s", batchSize: 1 } }
		                   )

		                   context.startSpan( spanType = "model", operation = "chat" )
		                   context.endSpan( output = "response" )

		                   success = true
		                   """, tempDir.toString().replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test aiAudit() with custom store instance" )
	public void testAiAuditCustomStoreInstance() {
		runtime.executeSource(
		    """
		    // Create a custom store
		    customStore = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Pass it to aiAudit
		    context = aiAudit( store = customStore )

		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan( output = "response" )

		    // Store should have the entry
		    stats = customStore.getStats()
		    totalEntries = stats.totalEntries
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "totalEntries" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test aiAudit() with config overrides" )
	public void testAiAuditConfigOverrides() {
		runtime.executeSource(
		    """
		    context = aiAudit( config = {
		        captureInput: false,
		        captureOutput: false,
		        sanitizePatterns: [ "customSecret" ]
		    } )

		    traceId = context.getTraceId()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isNotEmpty();
	}

	@Test
	@DisplayName( "Test aiAudit() throws for unknown store type" )
	public void testAiAuditUnknownStoreType() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    context = aiAudit( store = "unknownStore" )
			    """,
			    context
			);
		} );
	}

	// ==================== aiAuditQuery() Tests ====================

	@Test
	@DisplayName( "Test aiAuditQuery() queries store" )
	public void testAiAuditQuery() {
		runtime.executeSource(
		    """
		    // Create store with entries
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    entry1 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-1", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry1 )

		    entry2 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-2", spanType = "tool", operation = "execute"
		    ).complete()
		    store.store( entry2 )

		    // Query
		    results = aiAuditQuery( store = store )
		    resultCount = results.len()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "resultCount" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test aiAuditQuery() with filters" )
	public void testAiAuditQueryWithFilters() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    entry1 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-filter", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry1 )

		    entry2 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-filter", spanType = "tool", operation = "execute"
		    ).complete()
		    store.store( entry2 )

		    entry3 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-other", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry3 )

		    // Query with filter
		    results = aiAuditQuery( store = store, filters = { traceId: "trace-filter" } )
		    resultCount = results.len()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "resultCount" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test aiAuditQuery() with pagination" )
	public void testAiAuditQueryPagination() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Add 10 entries
		    for( i = 1; i <= 10; i++ ) {
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = "trace-page-" & i, spanType = "model", operation = "chat"
		        ).complete()
		        store.store( entry )
		    }

		    // Get first page
		    page1 = aiAuditQuery( store = store, limit = 3, offset = 0 )
		    page1Count = page1.len()

		    // Get second page
		    page2 = aiAuditQuery( store = store, limit = 3, offset = 3 )
		    page2Count = page2.len()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "page1Count" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "page2Count" ) ) ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test aiAuditQuery() with ordering" )
	public void testAiAuditQueryOrdering() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Add entries with different operations
		    ops = [ "alpha", "beta", "gamma" ]
		    for( op in ops ) {
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = "trace-order", spanType = "model", operation = op
		        ).complete()
		        store.store( entry )
		    }

		    // Query ascending
		    ascResults = aiAuditQuery( store = store, orderBy = "operation", orderDir = "asc" )
		    firstOpAsc = ascResults[1].operation

		    // Query descending
		    descResults = aiAuditQuery( store = store, orderBy = "operation", orderDir = "desc" )
		    firstOpDesc = descResults[1].operation
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "firstOpAsc" ) ) ).isEqualTo( "alpha" );
		assertThat( variables.getAsString( Key.of( "firstOpDesc" ) ) ).isEqualTo( "gamma" );
	}

	// ==================== aiAuditExport() Tests ====================

	@Test
	@DisplayName( "Test aiAuditExport() exports to JSON" )
	public void testAiAuditExportJson() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-export", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry )

		    // Export to JSON
		    exported = aiAuditExport( traceId = "trace-export", store = store, format = "json" )
		    isString = isSimpleValue( exported )
		    hasTraceId = exported.findNoCase( "trace-export" ) > 0
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "isString" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasTraceId" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test aiAuditExport() to file destination" )
	public void testAiAuditExportToFile() {
		String exportFile = tempDir.resolve( "export.json" ).toString();

		runtime.executeSource(
		    String.format( """
		                   store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		                   entry = new bxModules.bxai.models.audit.AuditEntry(
		                       traceId = "trace-file", spanType = "model", operation = "chat"
		                   ).complete()
		                   store.store( entry )

		                   // Export to file
		                   aiAuditExport(
		                       traceId     = "trace-file",
		                       store       = store,
		                       format      = "json",
		                       destination = "%s"
		                   )

		                   fileExists = fileExists( "%s" )
		                   """, exportFile.replace( "\\", "\\\\" ), exportFile.replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "fileExists" ) ) ).isTrue();
	}

	// ==================== Integration Tests ====================

	@Test
	@DisplayName( "Test full audit workflow with BIFs" )
	public void testFullAuditWorkflow() {
		runtime.executeSource(
		    """
		    // Create context
		    context = aiAudit( traceId = "workflow-trace", store = "memory" )

		    // Record some operations
		    context.startSpan( spanType = "agent", operation = "run" )

		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan(
		        output = "Model response",
		        tokens = { prompt: 100, completion: 50, total: 150 }
		    )

		    context.startSpan( spanType = "tool", operation = "execute" )
		    context.endSpan( output = "Tool result" )

		    context.endSpan( output = "Agent result" )

		    context.complete()

		    // Get entries
		    entries = context.getEntries()
		    entryCount = entries.len()

		    // Get summary
		    summary = context.getSummary()
		    spanCount = summary.spanCount
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "entryCount" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "spanCount" ) ) ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test aiAudit() traces are visible via aiAuditQuery() using shared store" )
	public void testSharedStoreBetweenAuditAndQuery() {
		runtime.executeSource(
		    """
		    // Create a shared store instance and pass it to both BIFs
		    sharedStore = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )
		    context = aiAudit( traceId = "shared-store-test", store = sharedStore )

		    // Add some entries via the context
		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan( output = "Model response" )

		    context.startSpan( spanType = "tool", operation = "execute" )
		    context.endSpan( output = "Tool result" )

		    context.complete()

		    // Query using aiAuditQuery() with same store instance
		    results = aiAuditQuery( store = sharedStore, filters = { traceId: "shared-store-test" } )
		    resultCount = results.len()

		    // Verify we can see the entries created via aiAudit()
		    hasModelEntry = false
		    hasToolEntry = false
		    for( entry in results ) {
		        if( entry.spanType == "model" ) hasModelEntry = true
		        if( entry.spanType == "tool" ) hasToolEntry = true
		    }
		    """,
		    context
		);

		// This test verifies that aiAudit() and aiAuditQuery() share the same store
		// If they don't, resultCount would be 0
		assertThat( variables.get( Key.of( "resultCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "hasModelEntry" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasToolEntry" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test aiAuditExport() can export traces created via aiAudit() using shared store" )
	public void testSharedStoreExport() {
		runtime.executeSource(
		    """
		    // Create a shared store instance
		    sharedStore = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )
		    context = aiAudit( traceId = "export-shared-test", store = sharedStore )

		    context.startSpan( spanType = "agent", operation = "run" )
		    context.endSpan( output = "Agent complete" )
		    context.complete()

		    // Export using aiAuditExport() with same store instance
		    exported = aiAuditExport( traceId = "export-shared-test", store = sharedStore, format = "json" )

		    // Verify export contains our data
		    hasTraceId = exported.findNoCase( "export-shared-test" ) > 0
		    hasAgent = exported.findNoCase( "agent" ) > 0
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasTraceId" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasAgent" ) ) ).isTrue();
	}

	// ==================== aiAuditStatus() Tests ====================

	@Test
	@DisplayName( "Test aiAuditStatus() returns struct with expected keys" )
	public void testAiAuditStatusReturnsStruct() {
		runtime.executeSource(
		    """
		    status = aiAuditStatus()
		    hasEnabled = status.keyExists( "enabled" )
		    hasInternalStorage = status.keyExists( "internalStorage" )
		    hasStore = status.keyExists( "store" )
		    hasStoreConfig = status.keyExists( "storeConfig" )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasEnabled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasInternalStorage" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasStore" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasStoreConfig" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test aiAuditStatus() returns boolean for enabled and internalStorage" )
	public void testAiAuditStatusReturnsBooleans() {
		runtime.executeSource(
		    """
		    status = aiAuditStatus()
		    enabledIsBoolean = isBoolean( status.enabled )
		    internalStorageIsBoolean = isBoolean( status.internalStorage )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "enabledIsBoolean" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "internalStorageIsBoolean" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test aiAuditStatus() store defaults to memory" )
	public void testAiAuditStatusDefaultStore() {
		runtime.executeSource(
		    """
		    status = aiAuditStatus()
		    store = status.store
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "store" ) ) ).isEqualTo( "memory" );
	}

	@Test
	@DisplayName( "Test aiAuditStatus() internalStorage defaults to true" )
	public void testAiAuditStatusInternalStorageDefault() {
		runtime.executeSource(
		    """
		    status = aiAuditStatus()
		    internalStorage = status.internalStorage
		    """,
		    context
		);

		// Default should be true (unless env var is set)
		assertThat( variables.getAsBoolean( Key.of( "internalStorage" ) ) ).isTrue();
	}

}
