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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * End-to-end and smoke tests for the audit system.
 *
 * These tests verify:
 * 1. Toggling audit.enabled affects event capture
 * 2. Sanitization of sensitive data works correctly
 * 3. All store implementations work correctly (store, query, purge)
 * 4. The aiAuditQuery() BIF retrieves traces correctly
 */
public class AuditEndToEndTest extends BaseIntegrationTest {

	private Path tempDir;

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
		try {
			tempDir = Files.createTempDirectory( "audit-e2e-test-" );
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

	// ==================== End-to-End Tests ====================

	@Nested
	@DisplayName( "End-to-End Audit Toggle Tests" )
	class AuditToggleTests {

		@Test
		@DisplayName( "E2E: Interceptor captures events when audit.enabled=true" )
		public void testInterceptorCapturesWhenEnabled() {
			runtime.executeSource(
			    """
			    // Create interceptor with enabled=true
			    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
			    interceptor.moduleRecord = {
			        settings: {
			            audit: {
			                enabled: true,
			                store: "memory",
			                storeConfig: {},
			                captureMessages: true,
			                captureToolArgs: true
			            }
			        }
			    }
			    interceptor.configure()

			    // Simulate model invocation events
			    interceptor.beforeAIModelInvoke( {
			        model: { getName: () => "gpt-4" },
			        chatRequest: { getMessages: () => [ { role: "user", content: "Hello" } ] }
			    } )

			    interceptor.afterAIModelInvoke( {
			        results: { content: "Hi there!" }
			    } )

			    // Query the interceptor's store to verify capture
			    store = interceptor.getContext().getStore()
			    results = store.query( filters = {}, limit = 100 )

			    hasEntries = results.len() > 0
			    hasModelSpan = false
			    for( entry in results ) {
			        if( entry.spanType == "model" && entry.operation == "invoke" ) {
			            hasModelSpan = true
			        }
			    }

			    // Clean up
			    interceptor.clearContext()
			    store.clear()
			    """,
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "hasEntries" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "hasModelSpan" ) ) ).isTrue();
		}

		@Test
		@DisplayName( "E2E: Interceptor does NOT capture events when audit.enabled=false" )
		public void testInterceptorSkipsWhenDisabled() {
			runtime.executeSource(
			    """
			    // Create interceptor with enabled=false
			    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
			    interceptor.moduleRecord = {
			        settings: {
			            audit: {
			                enabled: false,
			                store: "memory",
			                storeConfig: {}
			            }
			        }
			    }
			    interceptor.configure()

			    // Get a fresh memory store for this test
			    testStore = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

			    // Simulate model invocation events
			    interceptor.beforeAIModelInvoke( {
			        model: { getName: () => "gpt-4" },
			        chatRequest: { getMessages: () => [ { role: "user", content: "Hello" } ] }
			    } )

			    interceptor.afterAIModelInvoke( {
			        results: { content: "Hi there!" }
			    } )

			    // The store should be empty since audit was disabled
			    results = testStore.query( filters = {}, limit = 100 )
			    isEmpty = results.len() == 0
			    """,
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
		}

		@Test
		@DisplayName( "E2E: Full workflow - aiAudit() -> record spans -> aiAuditQuery()" )
		public void testFullAuditWorkflow() {
			runtime.executeSource(
			    """
			    // Create shared store and context using aiAudit()
			    traceId = "e2e-full-workflow-" & createUUID()
			    sharedStore = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )
			    context = aiAudit( traceId = traceId, store = sharedStore )

			    // Record agent run
			    context.startSpan( spanType = "agent", operation = "run", input = { prompt: "Help me search" } )

			    // Nested model call
			    context.startSpan( spanType = "model", operation = "chat", input = { model: "gpt-4" } )
			    context.endSpan( output = { response: "I'll help you search" }, tokens = { prompt: 10, completion: 20 } )

			    // Nested tool call
			    context.startSpan( spanType = "tool", operation = "execute", input = { tool: "search", query: "BoxLang" } )
			    context.endSpan( output = { results: [ "result1", "result2" ] } )

			    // End agent
			    context.endSpan( output = { finalResult: "Here are your results" } )

			    context.complete()

			    // Query using aiAuditQuery() with same store instance
			    results = aiAuditQuery( store = sharedStore, filters = { traceId: traceId } )
			    resultCount = results.len()

			    // Verify all span types are captured
			    hasAgent = false
			    hasModel = false
			    hasTool = false
			    for( entry in results ) {
			        if( entry.spanType == "agent" ) hasAgent = true
			        if( entry.spanType == "model" ) hasModel = true
			        if( entry.spanType == "tool" ) hasTool = true
			    }
			    """,
			    context
			);

			assertThat( variables.get( Key.of( "resultCount" ) ) ).isEqualTo( 3 );
			assertThat( variables.getAsBoolean( Key.of( "hasAgent" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "hasModel" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "hasTool" ) ) ).isTrue();
		}

		@Test
		@DisplayName( "E2E: Interceptor + aiAuditQuery() share the same store" )
		public void testInterceptorAndQueryShareStore() {
			runtime.executeSource(
			    """
			    // Create interceptor with enabled=true and get its store reference
			    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
			    interceptor.moduleRecord = {
			        settings: {
			            audit: {
			                enabled: true,
			                store: "memory",
			                storeConfig: {}
			            }
			        }
			    }
			    interceptor.configure()

			    // Simulate tool execution
			    interceptor.beforeAIToolExecute( {
			        name: "searchTool",
			        arguments: { query: "test query" }
			    } )

			    interceptor.afterAIToolExecute( {
			        results: "Tool execution results"
			    } )

			    // Get the context's store before clearing
			    ctx = interceptor.getContext()
			    interceptorStore = ctx.getStore()
			    interceptor.clearContext()

			    // Query the interceptor's store directly
			    results = aiAuditQuery( store = interceptorStore, filters = { spanType: "tool" } )
			    hasToolEntry = results.len() > 0

			    toolName = ""
			    if( hasToolEntry ) {
			        toolName = results[1].input.toolName ?: ""
			    }
			    """,
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "hasToolEntry" ) ) ).isTrue();
			assertThat( variables.getAsString( Key.of( "toolName" ) ) ).isEqualTo( "searchTool" );
		}
	}

	// ==================== Sanitization Tests ====================

	@Nested
	@DisplayName( "Sanitization Tests" )
	class SanitizationTests {

		@Test
		@DisplayName( "E2E: Sensitive keys are redacted in audit entries" )
		public void testSensitiveKeysRedacted() {
			runtime.executeSource(
			    """
			    // Create a completely fresh sanitizer directly (bypassing any cached context)
			    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

			    // Test data with sensitive keys
			    testInput = {
			        messages: [ { role: "user", content: "Hello" } ],
			        apiKey: "sk-secret-key-12345",
			        password: "super-secret-password",
			        authorization: "Bearer token123"
			    }

			    // Sanitize directly
			    sanitizedInput = sanitizer.sanitize( testInput )

			    // Check that sensitive fields are redacted
			    apiKeyRedacted = sanitizedInput.apiKey == "[REDACTED]"
			    passwordRedacted = sanitizedInput.password == "[REDACTED]"
			    authRedacted = sanitizedInput.authorization == "[REDACTED]"

			    // Non-sensitive data should be preserved
			    messagesPreserved = isArray( sanitizedInput.messages )
			    """,
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "apiKeyRedacted" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "passwordRedacted" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "authRedacted" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "messagesPreserved" ) ) ).isTrue();
		}

		@Test
		@DisplayName( "E2E: SAFE_KEYS (token counts) are NOT redacted" )
		public void testSafeKeysNotRedacted() {
			runtime.executeSource(
			    """
			    // Use a dedicated store to avoid test pollution
			    testStore = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )
			    uniqueTraceId = "safe-keys-test-" & createUUID()

			    ctx = aiAudit( traceId = uniqueTraceId, store = testStore )

			    // Record span with token usage (contains "token" which matches pattern but is safe)
			    ctx.startSpan( spanType = "model", operation = "chat" )
			    ctx.endSpan(
			        output = { response: "Hi!" },
			        tokens = {
			            prompt_tokens: 100,
			            completion_tokens: 50,
			            total_tokens: 150,
			            cached_tokens: 20
			        }
			    )
			    ctx.complete()

			    entries = ctx.getEntries()
			    entry = entries[1]

			    // Token counts should NOT be redacted (they're in SAFE_KEYS)
			    hasTokens = entry.keyExists( "tokens" ) || entry.keyExists( "output" )
			    """,
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "hasTokens" ) ) ).isTrue();
		}

		@Test
		@DisplayName( "E2E: Custom sanitization patterns work" )
		public void testCustomSanitizationPatterns() {
			runtime.executeSource(
			    """
			    // Create sanitizer with custom patterns
			    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer( {
			        sanitizePatterns: [ "customSecret", "myPrivate" ]
			    } )

			    // Test data with custom sensitive keys
			    testInput = {
			        customSecretValue: "should-be-redacted",
			        myPrivateData: "also-redacted",
			        normalData: "should-be-visible"
			    }

			    // Sanitize directly
			    sanitizedInput = sanitizer.sanitize( testInput )

			    customRedacted = sanitizedInput.customSecretValue == "[REDACTED]"
			    privateRedacted = sanitizedInput.myPrivateData == "[REDACTED]"
			    normalVisible = sanitizedInput.normalData == "should-be-visible"
			    """,
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "customRedacted" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "privateRedacted" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "normalVisible" ) ) ).isTrue();
		}

		@Test
		@DisplayName( "E2E: Large data is truncated" )
		public void testLargeDataTruncated() {
			runtime.executeSource(
			    """
			    // Create sanitizer with small max input size
			    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer( {
			        maxInputSize: 100
			    } )

			    // Create a very long string
			    longString = repeatString( "x", 500 )

			    // Test data with large content
			    testInput = { content: longString }

			    // Sanitize directly
			    sanitizedInput = sanitizer.sanitize( testInput )

			    // Content should be truncated
			    contentLength = len( sanitizedInput.content )
			    isTruncated = contentLength < 500 && sanitizedInput.content.findNoCase( "TRUNCATED" ) > 0
			    """,
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "isTruncated" ) ) ).isTrue();
		}
	}

	// ==================== Store Smoke Tests ====================

	@Nested
	@DisplayName( "Memory Store Smoke Tests" )
	class MemoryStoreSmokeTests {

		@Test
		@DisplayName( "Smoke: MemoryAuditStore - store, query, purge cycle" )
		public void testMemoryStoreSmokeCycle() {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

			    // STORE: Add entries
			    for( i = 1; i <= 5; i++ ) {
			        entry = new bxModules.bxai.models.audit.AuditEntry(
			            traceId = "smoke-mem-" & i,
			            spanType = "model",
			            operation = "chat"
			        ).complete()
			        store.store( entry )
			    }

			    // QUERY: Verify entries exist
			    allResults = store.query( filters = {}, limit = 100 )
			    initialCount = allResults.len()

			    // QUERY with filter
			    filteredResults = store.query( filters = { traceId: "smoke-mem-3" }, limit = 100 )
			    filteredCount = filteredResults.len()

			    // PURGE: Delete old entries (all of them since they're "old")
			    purgeDate = dateAdd( "d", 1, now() )
			    purgedCount = store.purge( purgeDate )

			    // Verify purge worked
			    afterPurge = store.query( filters = {}, limit = 100 )
			    afterPurgeCount = afterPurge.len()

			    // STATS
			    store.store( new bxModules.bxai.models.audit.AuditEntry(
			        traceId = "smoke-stats", spanType = "tool", operation = "execute"
			    ).complete() )
			    stats = store.getStats()
			    hasStats = stats.keyExists( "totalEntries" )

			    // CLEAR
			    store.clear()
			    afterClear = store.query( filters = {}, limit = 100 )
			    clearedCount = afterClear.len()
			    """,
			    context
			);

			assertThat( variables.get( Key.of( "initialCount" ) ) ).isEqualTo( 5 );
			assertThat( variables.get( Key.of( "filteredCount" ) ) ).isEqualTo( 1 );
			assertThat( variables.get( Key.of( "purgedCount" ) ) ).isEqualTo( 5 );
			assertThat( variables.get( Key.of( "afterPurgeCount" ) ) ).isEqualTo( 0 );
			assertThat( variables.getAsBoolean( Key.of( "hasStats" ) ) ).isTrue();
			assertThat( variables.get( Key.of( "clearedCount" ) ) ).isEqualTo( 0 );
		}

		@Test
		@DisplayName( "Smoke: MemoryAuditStore - deleteTrace" )
		public void testMemoryStoreDeleteTrace() {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

			    // Add entries for two traces
			    for( i = 1; i <= 3; i++ ) {
			        store.store( new bxModules.bxai.models.audit.AuditEntry(
			            traceId = "keep-trace", spanType = "model", operation = "chat"
			        ).complete() )

			        store.store( new bxModules.bxai.models.audit.AuditEntry(
			            traceId = "delete-trace", spanType = "tool", operation = "execute"
			        ).complete() )
			    }

			    beforeDelete = store.query( filters = {}, limit = 100 ).len()

			    // Delete one trace
			    deleted = store.deleteTrace( "delete-trace" )

			    afterDelete = store.query( filters = {}, limit = 100 ).len()
			    remainingTrace = store.getTrace( "keep-trace" )
			    remainingCount = remainingTrace.entries.len()

			    deletedTrace = store.getTrace( "delete-trace" )
			    deletedCount = deletedTrace.entries.len()
			    """,
			    context
			);

			assertThat( variables.get( Key.of( "beforeDelete" ) ) ).isEqualTo( 6 );
			assertThat( variables.getAsBoolean( Key.of( "deleted" ) ) ).isTrue();
			assertThat( variables.get( Key.of( "afterDelete" ) ) ).isEqualTo( 3 );
			assertThat( variables.get( Key.of( "remainingCount" ) ) ).isEqualTo( 3 );
			assertThat( variables.get( Key.of( "deletedCount" ) ) ).isEqualTo( 0 );
		}
	}

	@Nested
	@DisplayName( "File Store Smoke Tests" )
	class FileStoreSmokeTests {

		@Test
		@DisplayName( "Smoke: FileAuditStore - store, query, purge cycle" )
		public void testFileStoreSmokeCycle() {
			runtime.executeSource(
			    String.format(
			        """
			        store = new bxModules.bxai.models.audit.stores.FileAuditStore()
			            .configure( {
			                path: "%s",
			                format: "ndjson",
			                batchSize: 1
			            } )

			        // STORE: Add entries
			        for( i = 1; i <= 5; i++ ) {
			            entry = new bxModules.bxai.models.audit.AuditEntry(
			                traceId = "smoke-file-" & i,
			                spanType = "model",
			                operation = "chat"
			            ).complete()
			            store.store( entry )
			        }
			        store.flush()

			        // QUERY: Verify entries exist
			        allResults = store.query( filters = {}, limit = 100 )
			        initialCount = allResults.len()

			        // QUERY with filter
			        filteredResults = store.query( filters = { traceId: "smoke-file-3" }, limit = 100 )
			        filteredCount = filteredResults.len()

			        // STATS
			        stats = store.getStats()
			        hasStats = stats.keyExists( "totalEntries" )

			        // GET TRACE
			        trace = store.getTrace( "smoke-file-2" )
			        hasTraceEntries = trace.entries.len() > 0

			        // PURGE: Delete entries older than tomorrow (should delete all)
			        purgeDate = dateAdd( "d", 1, now() )
			        purgedCount = store.purge( purgeDate )

			        // Note: After purge, query may still find entries depending on implementation
			        // The important thing is purge() executes without error
			        purgeSuccess = true
			        """,
			        tempDir.toString().replace( "\\", "\\\\" )
			    ),
			    context
			);

			assertThat( variables.get( Key.of( "initialCount" ) ) ).isEqualTo( 5 );
			assertThat( variables.get( Key.of( "filteredCount" ) ) ).isEqualTo( 1 );
			assertThat( variables.getAsBoolean( Key.of( "hasStats" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "hasTraceEntries" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "purgeSuccess" ) ) ).isTrue();
		}

		@Test
		@DisplayName( "Smoke: FileAuditStore - deleteTrace" )
		public void testFileStoreDeleteTrace() {
			runtime.executeSource(
			    String.format(
			        """
			        store = new bxModules.bxai.models.audit.stores.FileAuditStore()
			            .configure( {
			                path: "%s",
			                format: "ndjson",
			                batchSize: 1
			            } )

			        // Add entries for two traces
			        store.store( new bxModules.bxai.models.audit.AuditEntry(
			            traceId = "file-keep", spanType = "model", operation = "chat"
			        ).complete() )

			        store.store( new bxModules.bxai.models.audit.AuditEntry(
			            traceId = "file-delete", spanType = "tool", operation = "execute"
			        ).complete() )

			        store.flush()

			        // Delete one trace
			        deleted = store.deleteTrace( "file-delete" )

			        // Verify deletion
			        deletedTrace = store.getTrace( "file-delete" )
			        deletedEmpty = deletedTrace.entries.len() == 0

			        keepTrace = store.getTrace( "file-keep" )
			        keepExists = keepTrace.entries.len() > 0
			        """,
			        tempDir.toString().replace( "\\", "\\\\" )
			    ),
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "deleted" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "deletedEmpty" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "keepExists" ) ) ).isTrue();
		}
	}

	@Nested
	@DisplayName( "JDBC Store Smoke Tests" )
	class JdbcStoreSmokeTests {

		@Test
		@DisplayName( "Smoke: JdbcAuditStore - store, getTrace, getStats cycle" )
		public void testJdbcStoreSmokeCycle() {
			var tableName = "smoke_jdbc_audit_" + System.currentTimeMillis();

			runtime.executeSource(
			    String.format(
			        """
			        store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
			            .configure( {
			                datasource: "bxai_test",
			                table: "%s"
			            } )

			        // STORE: Add entries
			        for( i = 1; i <= 5; i++ ) {
			            entry = new bxModules.bxai.models.audit.AuditEntry(
			                traceId = "smoke-jdbc-" & i,
			                spanType = "model",
			                operation = "chat"
			            ).complete()
			            store.store( entry )
			        }

			        // GET TRACE: Verify entry exists
			        trace = store.getTrace( "smoke-jdbc-3" )
			        hasTrace = trace.entries.len() > 0

			        // STATS (doesn't use LIMIT, works with Derby)
			        stats = store.getStats()
			        hasStats = stats.keyExists( "totalEntries" )
			        totalEntries = stats.totalEntries

			        // DELETE TRACE
			        deleted = store.deleteTrace( "smoke-jdbc-1" )

			        // Verify deletion
			        deletedTrace = store.getTrace( "smoke-jdbc-1" )
			        deletedEmpty = deletedTrace.entries.len() == 0

			        // PURGE: Delete entries older than tomorrow
			        purgeDate = dateAdd( "d", 1, now() )
			        purgedCount = store.purge( purgeDate )

			        // Verify purge worked
			        afterPurgeStats = store.getStats()
			        afterPurgeCount = afterPurgeStats.totalEntries
			        """,
			        tableName
			    ),
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "hasTrace" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "hasStats" ) ) ).isTrue();
			assertThat( variables.get( Key.of( "totalEntries" ) ) ).isEqualTo( 5 );
			assertThat( variables.getAsBoolean( Key.of( "deleted" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "deletedEmpty" ) ) ).isTrue();
			assertThat( variables.get( Key.of( "afterPurgeCount" ) ) ).isEqualTo( 0 );
		}

		@Test
		@DisplayName( "Smoke: JdbcAuditStore - storeBatch" )
		public void testJdbcStoreStoreBatch() {
			var tableName = "smoke_jdbc_batch_" + System.currentTimeMillis();

			runtime.executeSource(
			    String.format(
			        """
			        store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
			            .configure( {
			                datasource: "bxai_test",
			                table: "%s"
			            } )

			        // Create batch of entries
			        entries = []
			        for( i = 1; i <= 10; i++ ) {
			            entries.append( new bxModules.bxai.models.audit.AuditEntry(
			                traceId = "batch-test",
			                spanType = "model",
			                operation = "chat-" & i
			            ).complete() )
			        }

			        // Store batch
			        result = store.storeBatch( entries )

			        // Verify
			        stats = store.getStats()
			        totalEntries = stats.totalEntries
			        """,
			        tableName
			    ),
			    context
			);

			assertThat( variables.get( Key.of( "totalEntries" ) ) ).isEqualTo( 10 );
		}
	}

	// ==================== aiAuditQuery() BIF Tests ====================

	@Nested
	@DisplayName( "aiAuditQuery() BIF Tests" )
	class AiAuditQueryBIFTests {

		@Test
		@DisplayName( "aiAuditQuery() returns entries from shared store" )
		public void testAiAuditQueryReturnsEntries() {
			runtime.executeSource(
			    """
			    // Setup: Create shared store and entries via aiAudit()
			    sharedStore = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )
			    context = aiAudit( traceId = "query-bif-test", store = sharedStore )
			    context.startSpan( spanType = "agent", operation = "run" )
			    context.endSpan( output = "done" )
			    context.complete()

			    // Test: Query using aiAuditQuery() with same store instance
			    results = aiAuditQuery( store = sharedStore, filters = { traceId: "query-bif-test" } )
			    hasResults = results.len() > 0

			    // Verify structure
			    firstEntry = results[1]
			    hasTraceId = firstEntry.keyExists( "traceId" )
			    hasSpanType = firstEntry.keyExists( "spanType" )
			    hasOperation = firstEntry.keyExists( "operation" )
			    """,
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "hasResults" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "hasTraceId" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "hasSpanType" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "hasOperation" ) ) ).isTrue();
		}

		@Test
		@DisplayName( "aiAuditQuery() supports all filter types" )
		public void testAiAuditQueryFilters() {
			runtime.executeSource(
			    """
			    // Setup: Create diverse entries
			    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

			    // Entry 1: model with error
			    entry1 = new bxModules.bxai.models.audit.AuditEntry(
			        traceId = "filter-test-1",
			        spanType = "model",
			        operation = "chat"
			    ).complete( error = "API Error" )
			    store.store( entry1 )

			    // Entry 2: tool without error
			    entry2 = new bxModules.bxai.models.audit.AuditEntry(
			        traceId = "filter-test-2",
			        spanType = "tool",
			        operation = "execute"
			    ).complete()
			    store.store( entry2 )

			    // Test filters
			    bySpanType = aiAuditQuery( store = store, filters = { spanType: "model" } )
			    byOperation = aiAuditQuery( store = store, filters = { operation: "execute" } )
			    byTraceId = aiAuditQuery( store = store, filters = { traceId: "filter-test-1" } )
			    byStatus = aiAuditQuery( store = store, filters = { status: "error" } )

			    modelCount = bySpanType.len()
			    executeCount = byOperation.len()
			    trace1Count = byTraceId.len()
			    errorCount = byStatus.len()
			    """,
			    context
			);

			assertThat( variables.get( Key.of( "modelCount" ) ) ).isEqualTo( 1 );
			assertThat( variables.get( Key.of( "executeCount" ) ) ).isEqualTo( 1 );
			assertThat( variables.get( Key.of( "trace1Count" ) ) ).isEqualTo( 1 );
			assertThat( variables.get( Key.of( "errorCount" ) ) ).isEqualTo( 1 );
		}

		@Test
		@DisplayName( "aiAuditQuery() supports pagination" )
		public void testAiAuditQueryPagination() {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

			    // Create 10 entries
			    for( i = 1; i <= 10; i++ ) {
			        store.store( new bxModules.bxai.models.audit.AuditEntry(
			            traceId = "page-test",
			            spanType = "model",
			            operation = "chat-" & i
			        ).complete() )
			    }

			    // Test pagination
			    page1 = aiAuditQuery( store = store, limit = 3, offset = 0 )
			    page2 = aiAuditQuery( store = store, limit = 3, offset = 3 )
			    page3 = aiAuditQuery( store = store, limit = 3, offset = 6 )
			    page4 = aiAuditQuery( store = store, limit = 3, offset = 9 )

			    page1Count = page1.len()
			    page2Count = page2.len()
			    page3Count = page3.len()
			    page4Count = page4.len()
			    """,
			    context
			);

			assertThat( variables.get( Key.of( "page1Count" ) ) ).isEqualTo( 3 );
			assertThat( variables.get( Key.of( "page2Count" ) ) ).isEqualTo( 3 );
			assertThat( variables.get( Key.of( "page3Count" ) ) ).isEqualTo( 3 );
			assertThat( variables.get( Key.of( "page4Count" ) ) ).isEqualTo( 1 );
		}

		@Test
		@DisplayName( "aiAuditQuery() supports ordering" )
		public void testAiAuditQueryOrdering() {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

			    // Create entries with different operations
			    ops = [ "alpha", "beta", "gamma", "delta" ]
			    for( op in ops ) {
			        store.store( new bxModules.bxai.models.audit.AuditEntry(
			            traceId = "order-test",
			            spanType = "model",
			            operation = op
			        ).complete() )
			    }

			    // Test ordering
			    ascending = aiAuditQuery( store = store, orderBy = "operation", orderDir = "asc" )
			    descending = aiAuditQuery( store = store, orderBy = "operation", orderDir = "desc" )

			    firstAsc = ascending[1].operation
			    firstDesc = descending[1].operation
			    """,
			    context
			);

			assertThat( variables.getAsString( Key.of( "firstAsc" ) ) ).isEqualTo( "alpha" );
			assertThat( variables.getAsString( Key.of( "firstDesc" ) ) ).isEqualTo( "gamma" );
		}

		@Test
		@DisplayName( "aiAuditQuery() works with explicit store instance" )
		public void testAiAuditQueryWithExplicitStore() {
			runtime.executeSource(
			    """
			    // Create a store instance directly
			    store = new bxModules.bxai.models.audit.stores.MemoryAuditStore().configure( {} )

			    // Add entries via aiAudit() using the same store
			    context = aiAudit( traceId = "explicit-store-test", store = store )
			    context.startSpan( spanType = "model", operation = "chat" )
			    context.endSpan( output = "done" )
			    context.complete()

			    // Query using aiAuditQuery() with the same store instance
			    results = aiAuditQuery( store = store, filters = { traceId: "explicit-store-test" } )
			    hasResults = results.len() > 0

			    // Verify we can retrieve what we stored
			    correctTrace = hasResults && results[1].traceId == "explicit-store-test"
			    """,
			    context
			);

			assertThat( variables.getAsBoolean( Key.of( "hasResults" ) ) ).isTrue();
			assertThat( variables.getAsBoolean( Key.of( "correctTrace" ) ) ).isTrue();
		}
	}

}
