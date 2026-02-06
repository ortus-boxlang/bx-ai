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
 * Test cases for AuditContext - manages trace hierarchy and span lifecycle
 */
public class AuditContextTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test AuditContext instantiation with default traceId" )
	public void testDefaultInstantiation() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext()

		    traceId = context.getTraceId()
		    isRecording = context.isRecording()
		    currentSpanId = context.getCurrentSpanId()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isNotEmpty();
		assertThat( variables.getAsBoolean( Key.of( "isRecording" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "currentSpanId" ) ) ).isEmpty();
	}

	@Test
	@DisplayName( "Test AuditContext with custom traceId" )
	public void testCustomTraceId() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "custom-trace-123" )

		    traceId = context.getTraceId()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isEqualTo( "custom-trace-123" );
	}

	@Test
	@DisplayName( "Test startSpan creates a new span" )
	public void testStartSpan() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    spanId = context.startSpan(
		        spanType  = "model",
		        operation = "chat",
		        input     = { prompt: "Hello" }
		    )

		    currentSpanId = context.getCurrentSpanId()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "spanId" ) ) ).isNotEmpty();
		assertThat( variables.getAsString( Key.of( "currentSpanId" ) ) ).isEqualTo( variables.getAsString( Key.of( "spanId" ) ) );
	}

	@Test
	@DisplayName( "Test endSpan completes the current span" )
	public void testEndSpan() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    context.startSpan( spanType = "model", operation = "chat" )

		    // Simulate work
		    sleep( 10 )

		    context.endSpan( output = { response: "Hi there!" } )

		    entries = context.getEntries()
		    entryCount = entries.len()
		    currentSpanId = context.getCurrentSpanId()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "entryCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsString( Key.of( "currentSpanId" ) ) ).isEmpty();
	}

	@Test
	@DisplayName( "Test nested spans maintain hierarchy" )
	public void testNestedSpans() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    // Start parent span
		    parentSpanId = context.startSpan( spanType = "agent", operation = "run" )

		    // Start child span
		    childSpanId = context.startSpan( spanType = "model", operation = "chat" )

		    // End child
		    context.endSpan( output = "child output" )

		    // End parent
		    context.endSpan( output = "parent output" )

		    entries = context.getEntries()
		    entryCount = entries.len()

		    // Get the child entry
		    childEntry = entries.filter( e => e.spanId == childSpanId )[1]
		    childParentId = childEntry.parentSpanId
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "entryCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsString( Key.of( "childParentId" ) ) ).isEqualTo( variables.getAsString( Key.of( "parentSpanId" ) ) );
	}

	@Test
	@DisplayName( "Test addEntry creates a standalone entry" )
	public void testAddEntry() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    context.addEntry(
		        spanType  = "metrics",
		        operation = "tokenCount",
		        data      = { promptTokens: 100, completionTokens: 50 }
		    )

		    entries = context.getEntries()
		    entryCount = entries.len()
		    firstEntry = entries[1]
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "entryCount" ) ) ).isEqualTo( 1 );

		var firstEntry = variables.getAsStruct( Key.of( "firstEntry" ) );
		assertThat( firstEntry.getAsString( Key.of( "spanType" ) ) ).isEqualTo( "metrics" );
		assertThat( firstEntry.getAsString( Key.of( "operation" ) ) ).isEqualTo( "tokenCount" );
	}

	@Test
	@DisplayName( "Test getFullTrace returns hierarchical structure" )
	public void testGetFullTrace() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    // Create hierarchy: agent -> model -> tool
		    context.startSpan( spanType = "agent", operation = "run" )
		    context.startSpan( spanType = "model", operation = "chat" )
		    context.startSpan( spanType = "tool", operation = "execute" )
		    context.endSpan( output = "tool result" )
		    context.endSpan( output = "model response" )
		    context.endSpan( output = "agent result" )

		    trace = context.getFullTrace()
		    hasTraceId = trace.keyExists( "traceId" )
		    hasEntries = trace.keyExists( "entries" )
		    hasSummary = trace.keyExists( "summary" )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasTraceId" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasEntries" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasSummary" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test getSummary aggregates trace data" )
	public void testGetSummary() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    // Add entries with tokens
		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan(
		        output = "response",
		        tokens = { prompt: 100, completion: 50, total: 150 },
		        cost   = { amount: 0.003, currency: "USD" }
		    )

		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan(
		        output = "response2",
		        tokens = { prompt: 200, completion: 100, total: 300 },
		        cost   = { amount: 0.006, currency: "USD" }
		    )

		    summary = context.getSummary()
		    """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "traceId" ) ) ).isEqualTo( "trace-123" );
		assertThat( summary.get( "spanCount" ) ).isEqualTo( 2 );

		var tokens = summary.getAsStruct( Key.of( "tokens" ) );
		assertThat( tokens.get( "prompt" ) ).isEqualTo( 300 );
		assertThat( tokens.get( "completion" ) ).isEqualTo( 150 );
		assertThat( tokens.get( "total" ) ).isEqualTo( 450 );
	}

	@Test
	@DisplayName( "Test setRecording enables/disables recording" )
	public void testSetRecording() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    // Disable recording
		    context.setRecording( false )

		    // This span should not be recorded
		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan( output = "response" )

		    entriesWhileDisabled = context.getEntries().len()

		    // Re-enable recording
		    context.setRecording( true )

		    // This span should be recorded
		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan( output = "response" )

		    entriesAfterReEnable = context.getEntries().len()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "entriesWhileDisabled" ) ) ).isEqualTo( 0 );
		assertThat( variables.get( Key.of( "entriesAfterReEnable" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test setContextMetadata applies to all entries" )
	public void testSetContextMetadata() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )
		        .setContextMetadata( { requestId: "req-456", environment: "test" } )

		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan( output = "response" )

		    entries = context.getEntries()
		    metadata = entries[1].metadata
		    """,
		    context
		);

		var metadata = variables.getAsStruct( Key.of( "metadata" ) );
		assertThat( metadata.getAsString( Key.of( "requestId" ) ) ).isEqualTo( "req-456" );
		assertThat( metadata.getAsString( Key.of( "environment" ) ) ).isEqualTo( "test" );
	}

	@Test
	@DisplayName( "Test multi-tenant fields propagate to entries" )
	public void testMultiTenantFields() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )
		        .setUserId( "user-abc" )
		        .setConversationId( "conv-def" )
		        .setTenantId( "tenant-ghi" )

		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan( output = "response" )

		    entries = context.getEntries()
		    firstEntry = entries[1]
		    userId = firstEntry.userId
		    conversationId = firstEntry.conversationId
		    tenantId = firstEntry.tenantId
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-abc" );
		assertThat( variables.getAsString( Key.of( "conversationId" ) ) ).isEqualTo( "conv-def" );
		assertThat( variables.getAsString( Key.of( "tenantId" ) ) ).isEqualTo( "tenant-ghi" );
	}

	@Test
	@DisplayName( "Test export to JSON format" )
	public void testExportJson() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan( output = "response" )

		    jsonExport = context.export( format = "json" )
		    isString = isSimpleValue( jsonExport )
		    hasTraceId = jsonExport.findNoCase( "trace-123" ) > 0
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "isString" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasTraceId" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test complete() closes unclosed spans" )
	public void testComplete() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    // Start spans without closing them
		    context.startSpan( spanType = "agent", operation = "run" )
		    context.startSpan( spanType = "model", operation = "chat" )

		    // Complete should close both
		    context.complete()

		    entries = context.getEntries()
		    entryCount = entries.len()
		    summary = context.getSummary()
		    isCompleted = summary.completed
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "entryCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "isCompleted" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test error tracking in spans" )
	public void testErrorTracking() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )

		    context.startSpan( spanType = "model", operation = "chat" )
		    context.endSpan( output = "", error = "Rate limit exceeded" )

		    entries = context.getEntries()
		    firstEntry = entries[1]
		    status = firstEntry.status
		    error = firstEntry.error

		    summary = context.getSummary()
		    errorCount = summary.errorCount
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "status" ) ) ).isEqualTo( "error" );
		assertThat( variables.getAsString( Key.of( "error" ) ) ).isEqualTo( "Rate limit exceeded" );
		assertThat( variables.get( Key.of( "errorCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Test fluent chaining" )
	public void testFluentChaining() {
		runtime.executeSource(
		    """
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-123" )
		        .setUserId( "user-1" )
		        .setConversationId( "conv-1" )
		        .setTenantId( "tenant-1" )
		        .setContextMetadata( { source: "test" } )
		        .setRecording( true )

		    traceId = context.getTraceId()
		    isRecording = context.isRecording()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isEqualTo( "trace-123" );
		assertThat( variables.getAsBoolean( Key.of( "isRecording" ) ) ).isTrue();
	}

}
