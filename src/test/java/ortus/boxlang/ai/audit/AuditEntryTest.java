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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Test cases for AuditEntry model - the core data model for audit records
 */
public class AuditEntryTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test AuditEntry instantiation with required parameters" )
	public void testInstantiation() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId   = "trace-123",
		        spanType  = "model",
		        operation = "chat"
		    )

		    spanId = entry.getSpanId()
		    traceId = entry.getTraceId()
		    spanType = entry.getSpanType()
		    operation = entry.getOperation()
		    status = entry.getStatus()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "spanId" ) ) ).isNotEmpty();
		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isEqualTo( "trace-123" );
		assertThat( variables.getAsString( Key.of( "spanType" ) ) ).isEqualTo( "model" );
		assertThat( variables.getAsString( Key.of( "operation" ) ) ).isEqualTo( "chat" );
		assertThat( variables.getAsString( Key.of( "status" ) ) ).isEqualTo( "ok" );
	}

	@Test
	@DisplayName( "Test AuditEntry with custom spanId and parentSpanId" )
	public void testWithParentSpan() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        spanId       = "span-custom",
		        traceId      = "trace-456",
		        parentSpanId = "parent-span-789",
		        spanType     = "tool",
		        operation    = "execute"
		    )

		    spanId = entry.getSpanId()
		    traceId = entry.getTraceId()
		    parentSpanId = entry.getParentSpanId()
		    spanType = entry.getSpanType()
		    operation = entry.getOperation()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "spanId" ) ) ).isEqualTo( "span-custom" );
		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isEqualTo( "trace-456" );
		assertThat( variables.getAsString( Key.of( "parentSpanId" ) ) ).isEqualTo( "parent-span-789" );
		assertThat( variables.getAsString( Key.of( "spanType" ) ) ).isEqualTo( "tool" );
		assertThat( variables.getAsString( Key.of( "operation" ) ) ).isEqualTo( "execute" );
	}

	@Test
	@DisplayName( "Test AuditEntry generates unique span IDs" )
	public void testUniqueSpanIds() {
		runtime.executeSource(
		    """
		    entry1 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-1", spanType = "model", operation = "chat"
		    )
		    entry2 = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-1", spanType = "model", operation = "chat"
		    )

		    spanId1 = entry1.getSpanId()
		    spanId2 = entry2.getSpanId()
		    areEqual = spanId1 == spanId2
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "areEqual" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Test AuditEntry setInput and toStruct" )
	public void testSetInput() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    ).setInput( { prompt: "Hello, how are you?", maxTokens: 100 } )

		    data = entry.toStruct()
		    input = data.input
		    """,
		    context
		);

		var input = variables.getAsStruct( Key.of( "input" ) );
		assertThat( input.getAsString( Key.of( "prompt" ) ) ).isEqualTo( "Hello, how are you?" );
		assertThat( input.get( "maxTokens" ) ).isEqualTo( 100 );
	}

	@Test
	@DisplayName( "Test AuditEntry setOutput and complete" )
	public void testSetOutputAndComplete() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    ).complete( output = { response: "I'm doing well!", finishReason: "stop" } )

		    data = entry.toStruct()
		    output = data.output
		    status = entry.getStatus()
		    """,
		    context
		);

		var output = variables.getAsStruct( Key.of( "output" ) );
		assertThat( output.getAsString( Key.of( "response" ) ) ).isEqualTo( "I'm doing well!" );
		assertThat( output.getAsString( Key.of( "finishReason" ) ) ).isEqualTo( "stop" );
		assertThat( variables.getAsString( Key.of( "status" ) ) ).isEqualTo( "ok" );
	}

	@Test
	@DisplayName( "Test AuditEntry token tracking with setTokens" )
	public void testTokenTracking() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    ).setTokens( { prompt: 50, completion: 100, total: 150 } )

		    data = entry.toStruct()
		    tokens = data.tokens
		    """,
		    context
		);

		var tokens = variables.getAsStruct( Key.of( "tokens" ) );
		assertThat( tokens.get( "prompt" ) ).isEqualTo( 50 );
		assertThat( tokens.get( "completion" ) ).isEqualTo( 100 );
		assertThat( tokens.get( "total" ) ).isEqualTo( 150 );
	}

	@Test
	@DisplayName( "Test AuditEntry cost tracking with setCost" )
	public void testCostTracking() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    ).setCost( { amount: 0.0025, currency: "USD" } )

		    data = entry.toStruct()
		    cost = data.cost
		    """,
		    context
		);

		var cost = variables.getAsStruct( Key.of( "cost" ) );
		assertThat( ( ( Number ) cost.get( "amount" ) ).doubleValue() ).isWithin( 0.0001 ).of( 0.0025 );
		assertThat( cost.getAsString( Key.of( "currency" ) ) ).isEqualTo( "USD" );
	}

	@Test
	@DisplayName( "Test AuditEntry error tracking" )
	public void testErrorTracking() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    ).complete( output = "", error = "API rate limit exceeded" )

		    status = entry.getStatus()
		    data = entry.toStruct()
		    error = data.error
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "status" ) ) ).isEqualTo( "error" );
		assertThat( variables.getAsString( Key.of( "error" ) ) ).isEqualTo( "API rate limit exceeded" );
	}

	@Test
	@DisplayName( "Test AuditEntry metadata" )
	public void testMetadata() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "agent", operation = "run"
		    ).setMetadata( { userId: "user-456", requestId: "req-789" } )

		    data = entry.toStruct()
		    metadata = data.metadata
		    """,
		    context
		);

		var metadata = variables.getAsStruct( Key.of( "metadata" ) );
		assertThat( metadata.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-456" );
		assertThat( metadata.getAsString( Key.of( "requestId" ) ) ).isEqualTo( "req-789" );
	}

	@Test
	@DisplayName( "Test AuditEntry complete() calculates duration" )
	public void testComplete() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    )

		    // Simulate some processing time
		    sleep( 50 )

		    entry.complete()

		    durationMs = entry.getDurationMs()
		    data = entry.toStruct()
		    hasEndTime = len( data.endTime ) > 0
		    """,
		    context
		);

		var durationMs = ( ( Number ) variables.get( Key.of( "durationMs" ) ) ).longValue();
		assertThat( durationMs ).isAtLeast( 50 );
		assertThat( variables.getAsBoolean( Key.of( "hasEndTime" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditEntry toStruct() serialization" )
	public void testToStruct() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    )
		        .setInput( { prompt: "Hello" } )
		        .setTokens( { prompt: 10, completion: 5, total: 15 } )
		        .setCost( { amount: 0.001, currency: "USD" } )
		        .setMetadata( { key: "value" } )
		        .complete( output = { response: "Hi" } )

		    data = entry.toStruct()
		    """,
		    context
		);

		var data = variables.getAsStruct( Key.of( "data" ) );
		assertThat( data.containsKey( Key.of( "spanId" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "traceId" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "spanType" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "operation" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "startTime" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "endTime" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "durationMs" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "input" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "output" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "tokens" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "cost" ) ) ).isTrue();
		assertThat( data.containsKey( Key.of( "metadata" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditEntry fromStruct() deserialization" )
	public void testFromStruct() {
		runtime.executeSource(
		    """
		    data = {
		        spanId: "span-123",
		        traceId: "trace-456",
		        parentSpanId: "parent-789",
		        spanType: "tool",
		        operation: "search",
		        startTime: now(),
		        endTime: now(),
		        durationMs: 100,
		        status: "ok",
		        input: { query: "test" },
		        output: { results: [] },
		        tokens: { prompt: 25, completion: 50, total: 75 },
		        cost: { amount: 0.002, currency: "USD" },
		        error: "",
		        reasoning: "",
		        metadata: { source: "test" },
		        userId: "",
		        conversationId: "",
		        tenantId: ""
		    }

		    entry = bxModules.bxai.models.audit.AuditEntry::fromStruct( data )

		    spanId = entry.getSpanId()
		    traceId = entry.getTraceId()
		    spanType = entry.getSpanType()
		    status = entry.getStatus()
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "spanId" ) ) ).isEqualTo( "span-123" );
		assertThat( variables.getAsString( Key.of( "traceId" ) ) ).isEqualTo( "trace-456" );
		assertThat( variables.getAsString( Key.of( "spanType" ) ) ).isEqualTo( "tool" );
		assertThat( variables.getAsString( Key.of( "status" ) ) ).isEqualTo( "ok" );
	}

	@Test
	@DisplayName( "Test AuditEntry fluent chaining" )
	public void testFluentChaining() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    )
		        .setInput( { prompt: "Hello" } )
		        .setOutput( { response: "Hi" } )
		        .setTokens( { prompt: 10, completion: 20, total: 30 } )
		        .setCost( { amount: 0.003, currency: "USD" } )
		        .setMetadata( { user: "test" } )
		        .setUserId( "user-123" )
		        .setConversationId( "conv-456" )
		        .setTenantId( "tenant-789" )
		        .complete()

		    data = entry.toStruct()
		    hasAllFields = data.keyExists( "userId" ) && data.keyExists( "conversationId" ) && data.keyExists( "tenantId" )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasAllFields" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditEntry multi-tenant fields" )
	public void testMultiTenantFields() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    )
		        .setUserId( "user-abc" )
		        .setConversationId( "conv-def" )
		        .setTenantId( "tenant-ghi" )

		    data = entry.toStruct()
		    userId = data.userId
		    conversationId = data.conversationId
		    tenantId = data.tenantId
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-abc" );
		assertThat( variables.getAsString( Key.of( "conversationId" ) ) ).isEqualTo( "conv-def" );
		assertThat( variables.getAsString( Key.of( "tenantId" ) ) ).isEqualTo( "tenant-ghi" );
	}

	@Test
	@DisplayName( "Test AuditEntry reasoning field" )
	public void testReasoningField() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "agent", operation = "decide"
		    ).setReasoning( "User asked about weather, so I will call the weather API" )

		    data = entry.toStruct()
		    reasoning = data.reasoning
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "reasoning" ) ) ).isEqualTo( "User asked about weather, so I will call the weather API" );
	}

	@Test
	@DisplayName( "Test AuditEntry.fromStruct() throws for invalid status" )
	public void testFromStructInvalidStatus() {
		// Invalid status should now throw InvalidAuditEntry to prevent data corruption
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    data = {
			        traceId: "trace-123",
			        spanType: "model",
			        operation: "chat",
			        status: "invalid_status"
			    }
			    entry = bxModules.bxai.models.audit.AuditEntry::fromStruct( data )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test AuditEntry.fromStruct() throws for missing traceId" )
	public void testFromStructMissingTraceId() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    data = { spanType: "model", operation: "chat" }
			    entry = bxModules.bxai.models.audit.AuditEntry::fromStruct( data )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test AuditEntry.fromStruct() throws for missing spanType" )
	public void testFromStructMissingSpanType() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    data = { traceId: "trace-123", operation: "chat" }
			    entry = bxModules.bxai.models.audit.AuditEntry::fromStruct( data )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test AuditEntry.fromStruct() throws for missing operation" )
	public void testFromStructMissingOperation() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    data = { traceId: "trace-123", spanType: "model" }
			    entry = bxModules.bxai.models.audit.AuditEntry::fromStruct( data )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test AuditEntry.fromStruct() throws for empty traceId" )
	public void testFromStructEmptyTraceId() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    data = { traceId: "", spanType: "model", operation: "chat" }
			    entry = bxModules.bxai.models.audit.AuditEntry::fromStruct( data )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test AuditEntry.complete() is idempotent (double-completion)" )
	public void testCompleteIsIdempotent() {
		runtime.executeSource(
		    """
		    entry = new bxModules.bxai.models.audit.AuditEntry(
		        traceId = "trace-123", spanType = "model", operation = "chat"
		    )

		    // Complete once
		    entry.complete( output = "First output" )
		    firstDuration = entry.getDurationMs()
		    firstOutput = entry.toStruct().output

		    // Sleep and complete again
		    sleep( 50 )
		    entry.complete( output = "Second output" )
		    secondDuration = entry.getDurationMs()
		    secondOutput = entry.toStruct().output

		    // Duration and output should NOT change on second complete
		    durationUnchanged = firstDuration == secondDuration
		    outputUnchanged = firstOutput == secondOutput
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "durationUnchanged" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "outputUnchanged" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditEntry span types" )
	public void testSpanTypes() {
		runtime.executeSource(
		    """
		    types = [
		        "agent",
		        "model",
		        "tool",
		        "provider",
		        "mcp",
		        "embed",
		        "pipeline"
		    ]

		    entries = []
		    for( type in types ) {
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = "trace-1", spanType = type, operation = "test"
		        )
		        entries.append( entry.getSpanType() )
		    }

		    result = entries
		    """,
		    context
		);

		var result = variables.getAsArray( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 7 );
		assertThat( result.get( 0 ) ).isEqualTo( "agent" );
		assertThat( result.get( 1 ) ).isEqualTo( "model" );
		assertThat( result.get( 2 ) ).isEqualTo( "tool" );
	}

}
