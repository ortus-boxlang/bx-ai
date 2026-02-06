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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Test cases for MemoryAuditStore - in-memory audit storage
 */
public class MemoryAuditStoreTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test MemoryAuditStore instantiation" )
	public void testInstantiation() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore()
		    stats = store.getStats()
		    totalEntries = stats.totalEntries
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "totalEntries" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test store and retrieve single entry" )
	public void testStoreAndRetrieve() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    ).setInput( { prompt: "Hello" } ).complete( output = { response: "Hi" } )

		    store.store( entry )

		    retrieved = store.getById( entry.getSpanId() )
		    hasSpanId = retrieved.keyExists( "spanId" )
		    spanType = retrieved.spanType
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasSpanId" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "spanType" ) ) ).isEqualTo( "model" );
	}

	@Test
	@DisplayName( "Test store multiple entries and query" )
	public void testStoreAndQuery() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Store multiple entries
		    for( i = 1; i <= 5; i++ ) {
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = "trace-" & i, spanType = "model", operation = "chat"
		        ).complete()
		        store.store( entry )
		    }

		    results = store.query( filters = {}, limit = 10 )
		    resultCount = results.len()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "resultCount" ) ) ).isEqualTo( 5 );
	}

	@Test
	@DisplayName( "Test query with filters" )
	public void testQueryWithFilters() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Store entries with different types
		    entry1 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-1", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry1 )

		    entry2 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-1", spanType = "tool", operation = "execute"
		    ).complete()
		    store.store( entry2 )

		    entry3 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-2", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry3 )

		    // Query for model spans only
		    modelResults = store.query( filters = { spanType: "model" } )
		    modelCount = modelResults.len()

		    // Query for specific trace
		    traceResults = store.query( filters = { traceId: "trace-1" } )
		    traceCount = traceResults.len()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "modelCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "traceCount" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test query with pagination" )
	public void testQueryPagination() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Store 10 entries
		    for( i = 1; i <= 10; i++ ) {
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = "trace-" & i, spanType = "model", operation = "chat"
		        ).complete()
		        store.store( entry )
		    }

		    // Get first page
		    page1 = store.query( filters = {}, limit = 3, offset = 0 )
		    page1Count = page1.len()

		    // Get second page
		    page2 = store.query( filters = {}, limit = 3, offset = 3 )
		    page2Count = page2.len()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "page1Count" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "page2Count" ) ) ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test getTrace returns complete trace" )
	public void testGetTrace() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Store entries for same trace
		    entry1 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-abc", spanType = "agent", operation = "run"
		    ).complete()
		    store.store( entry1 )

		    entry2 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-abc", parentSpanId = entry1.getSpanId(),
		        spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry2 )

		    // Get the trace
		    trace = store.getTrace( "trace-abc" )
		    traceId = trace.traceId
		    entryCount = trace.entries.len()
		    hasSummary = trace.keyExists( "summary" )
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isEqualTo( "trace-abc" );
		assertThat( variables.get( Key.of( "entryCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "hasSummary" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test deleteTrace removes all trace entries" )
	public void testDeleteTrace() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Store entries for two traces
		    entry1 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-delete", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry1 )

		    entry2 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-delete", spanType = "tool", operation = "execute"
		    ).complete()
		    store.store( entry2 )

		    entry3 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-keep", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry3 )

		    countBefore = store.getStats().totalEntries

		    // Delete one trace
		    deleted = store.deleteTrace( "trace-delete" )

		    countAfter = store.getStats().totalEntries
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "countBefore" ) ) ).isEqualTo( 3 );
		assertThat( variables.getAsBoolean( Key.of( "deleted" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "countAfter" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test purge removes old entries" )
	public void testPurge() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Store entries
		    entry1 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-1", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry1 )

		    // Purge entries older than tomorrow (should delete all)
		    purgeDate = dateAdd( "d", 1, now() )
		    purgedCount = store.purge( purgeDate )

		    countAfter = store.getStats().totalEntries
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "purgedCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "countAfter" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test getStats returns accurate statistics" )
	public void testGetStats() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Store various entries
		    entry1 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-1", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry1 )

		    entry2 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-1", spanType = "tool", operation = "execute"
		    ).complete()
		    store.store( entry2 )

		    entry3 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-2", spanType = "model", operation = "chat"
		    ).complete()
		    store.store( entry3 )

		    stats = store.getStats()
		    totalEntries = stats.totalEntries
		    totalTraces = stats.totalTraces
		    bySpanType = stats.bySpanType
		    modelCount = bySpanType.model
		    toolCount = bySpanType.tool
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "totalEntries" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "totalTraces" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "modelCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test clear removes all entries" )
	public void testClear() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Store entries
		    for( i = 1; i <= 5; i++ ) {
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = "trace-" & i, spanType = "model", operation = "chat"
		        ).complete()
		        store.store( entry )
		    }

		    countBefore = store.getStats().totalEntries

		    // Clear
		    store.clear()

		    countAfter = store.getStats().totalEntries
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "countBefore" ) ) ).isEqualTo( 5 );
		assertThat( variables.get( Key.of( "countAfter" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test storeBatch stores multiple entries" )
	public void testStoreBatch() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Create batch of entries
		    entries = []
		    for( i = 1; i <= 5; i++ ) {
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = "trace-batch", spanType = "model", operation = "chat"
		        ).complete()
		        entries.append( entry )
		    }

		    // Store batch
		    store.storeBatch( entries )

		    stats = store.getStats()
		    totalEntries = stats.totalEntries
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "totalEntries" ) ) ).isEqualTo( 5 );
	}

	@Test
	@DisplayName( "Test fluent chaining" )
	public void testFluentChaining() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore()
		        .configure( { maxSize: 1000 } )

		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-chain", spanType = "model", operation = "chat"
		    ).complete()

		    // Chain store operations
		    store.store( entry ).flush()

		    stats = store.getStats()
		    totalEntries = stats.totalEntries
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "totalEntries" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test query with sorting" )
	public void testQuerySorting() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

		    // Store entries with different operations
		    ops = [ "alpha", "beta", "gamma" ]
		    for( op in ops ) {
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = "trace-sort", spanType = "model", operation = op
		        ).complete()
		        store.store( entry )
		    }

		    // Query sorted ascending
		    ascResults = store.query(
		        filters = {},
		        limit = 10,
		        orderBy = "operation",
		        orderDir = "asc"
		    )
		    firstOpAsc = ascResults[1].operation

		    // Query sorted descending
		    descResults = store.query(
		        filters = {},
		        limit = 10,
		        orderBy = "operation",
		        orderDir = "desc"
		    )
		    firstOpDesc = descResults[1].operation
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "firstOpAsc" ) ) ).isEqualTo( "alpha" );
		assertThat( variables.getAsString( Key.of( "firstOpDesc" ) ) ).isEqualTo( "gamma" );
	}

}
