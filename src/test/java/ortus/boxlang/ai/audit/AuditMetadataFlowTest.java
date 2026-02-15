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
 * Tests for audit metadata flow through the options.audit pipeline.
 *
 * Verifies the flow: options.audit → request.auditMetadata → event data → AuditInterceptor → stored entries
 */
public class AuditMetadataFlowTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	// ==================== Event Handler auditMetadata Pickup ====================

	@Test
	@DisplayName( "onAIChatRequest picks up auditMetadata and applies to context" )
	public void testOnAIChatRequestPicksUpAuditMetadata() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
		    interceptor.moduleRecord = {
		        settings: {
		            audit: { enabled: true, store: "memory", storeConfig: {} }
		        }
		    }
		    interceptor.configure()

		    interceptor.onAIChatRequest( {
		        chatRequest: {
		            getMessages: () => [],
		            getModel: () => "test-model",
		            getTimeout: () => 30,
		            getAuditMetadata: () => {}
		        },
		        provider: { getName: () => "TestProvider" },
		        auditMetadata: { feature: "chatAnalysis", requestId: "req-001" }
		    } )

		    ctx = interceptor.getContext()
		    meta = ctx.getContextMetadata()
		    hasApp = meta.keyExists( "app" )
		    feature = hasApp ? ( meta.app.feature ?: "" ) : ""
		    requestId = hasApp ? ( meta.app.requestId ?: "" ) : ""

		    interceptor.onAIChatResponse( {
		        chatRequest: {
		            getMessages: () => [],
		            getModel: () => "test-model",
		            getAuditMetadata: () => {}
		        },
		        response: "test response",
		        rawResponse: {}
		    } )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasApp" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "feature" ) ) ).isEqualTo( "chatAnalysis" );
		assertThat( variables.getAsString( Key.of( "requestId" ) ) ).isEqualTo( "req-001" );
	}

	@Test
	@DisplayName( "Event handler handles missing auditMetadata gracefully" )
	public void testEventHandlerWithoutAuditMetadata() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
		    interceptor.moduleRecord = {
		        settings: {
		            audit: { enabled: true, store: "memory", storeConfig: {} }
		        }
		    }
		    interceptor.configure()

		    // No auditMetadata key in event data
		    interceptor.onAIChatRequest( {
		        chatRequest: {
		            getMessages: () => [],
		            getModel: () => "test-model",
		            getTimeout: () => 30,
		            getAuditMetadata: () => {}
		        },
		        provider: { getName: () => "TestProvider" }
		    } )

		    success = true

		    interceptor.onAIChatResponse( {
		        chatRequest: {
		            getMessages: () => [],
		            getModel: () => "test-model",
		            getAuditMetadata: () => {}
		        },
		        response: "test response",
		        rawResponse: {}
		    } )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	// ==================== Full Pipeline ====================

	@Test
	@DisplayName( "Full pipeline: auditMetadata flows through interceptor into stored entries" )
	public void testFullPipelineStoreVerification() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
		    interceptor.moduleRecord = {
		        settings: {
		            audit: { enabled: true, store: "memory", storeConfig: {} }
		        }
		    }
		    interceptor.configure()

		    // Simulate framework auto-layer (like ColdBox preProcess)
		    interceptor.setApplicationMetadata( "app", {
		        handler: "api",
		        action: "process"
		    } )

		    // Simulate onAIChatRequest with per-call auditMetadata
		    interceptor.onAIChatRequest( {
		        chatRequest: {
		            getMessages: () => [ { role: "user", content: "Hello" } ],
		            getModel: () => "gpt-4",
		            getTimeout: () => 30,
		            getAuditMetadata: () => {}
		        },
		        provider: { getName: () => "OpenAIService" },
		        auditMetadata: { feature: "contentGeneration", contentType: "email" }
		    } )

		    // Complete the span so it gets stored
		    interceptor.onAIChatResponse( {
		        chatRequest: {
		            getMessages: () => [],
		            getModel: () => "gpt-4",
		            getAuditMetadata: () => {}
		        },
		        response: "Generated content",
		        rawResponse: {}
		    } )

		    // Verify entries in the store contain the metadata
		    store = interceptor.getStore()
		    hasStore = !isNull( store )
		    storedEntries = hasStore ? store.query( filters = {}, limit = 100 ) : []
		    entryCount = storedEntries.len()

		    hasMetadataInStore = false
		    storedFeature = ""
		    for ( entry in storedEntries ) {
		        if ( isStruct( entry.metadata ) && entry.metadata.keyExists( "app" ) ) {
		            hasMetadataInStore = true
		            storedFeature = entry.metadata.app.feature ?: ""
		        }
		    }

		    interceptor.clearContext()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasStore" ) ) ).isTrue();
		assertThat( ( int ) variables.getAsNumber( Key.of( "entryCount" ) ).intValue() ).isGreaterThan( 0 );
		assertThat( variables.getAsBoolean( Key.of( "hasMetadataInStore" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "storedFeature" ) ) ).isEqualTo( "contentGeneration" );
	}

	@Test
	@DisplayName( "Metadata preserved across nested agent > model > tool spans" )
	public void testMetadataPreservedAcrossNestedSpans() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
		    interceptor.moduleRecord = {
		        settings: {
		            audit: { enabled: true, store: "memory", storeConfig: {} }
		        }
		    }
		    interceptor.configure()

		    // Agent start with audit metadata
		    interceptor.beforeAIAgentRun( {
		        agent: { getAgentName: () => "DataProcessor" },
		        input: "Process this data",
		        auditMetadata: { feature: "dataProcessing", batchId: "batch-001" }
		    } )

		    // Nested model call
		    interceptor.beforeAIModelInvoke( {
		        model: { getName: () => "gpt-4" },
		        chatRequest: { getMessages: () => [] }
		    } )

		    // Nested tool call
		    interceptor.beforeAIToolExecute( { name: "dataTransform", arguments: {} } )
		    interceptor.afterAIToolExecute( { results: "transformed data" } )
		    interceptor.afterAIModelInvoke( { results: "model response" } )

		    // After nested calls complete, context metadata should still be available
		    ctx = interceptor.getContext()
		    meta = ctx.getContextMetadata()
		    hasApp = meta.keyExists( "app" )
		    feature = hasApp ? ( meta.app.feature ?: "" ) : ""

		    interceptor.afterAIAgentRun( { response: "processing complete" } )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasApp" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "feature" ) ) ).isEqualTo( "dataProcessing" );
	}

	@Test
	@DisplayName( "Sequential AI calls each get their own audit metadata" )
	public void testSequentialCallsWithDifferentMetadata() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
		    interceptor.moduleRecord = {
		        settings: {
		            audit: { enabled: true, store: "memory", storeConfig: {} }
		        }
		    }
		    interceptor.configure()

		    // First call
		    interceptor.onAIChatRequest( {
		        chatRequest: {
		            getMessages: () => [],
		            getModel: () => "model-1",
		            getTimeout: () => 30,
		            getAuditMetadata: () => {}
		        },
		        provider: { getName: () => "Provider1" },
		        auditMetadata: { feature: "callA" }
		    } )

		    feature1 = interceptor.getContext().getContextMetadata().app.feature ?: ""

		    interceptor.onAIChatResponse( {
		        chatRequest: {
		            getMessages: () => [],
		            getModel: () => "model-1",
		            getAuditMetadata: () => {}
		        },
		        response: "response1",
		        rawResponse: {}
		    } )

		    // Second call with different metadata
		    interceptor.onAIChatRequest( {
		        chatRequest: {
		            getMessages: () => [],
		            getModel: () => "model-2",
		            getTimeout: () => 30,
		            getAuditMetadata: () => {}
		        },
		        provider: { getName: () => "Provider2" },
		        auditMetadata: { feature: "callB" }
		    } )

		    feature2 = interceptor.getContext().getContextMetadata().app.feature ?: ""

		    interceptor.onAIChatResponse( {
		        chatRequest: {
		            getMessages: () => [],
		            getModel: () => "model-2",
		            getAuditMetadata: () => {}
		        },
		        response: "response2",
		        rawResponse: {}
		    } )
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "feature1" ) ) ).isEqualTo( "callA" );
		assertThat( variables.getAsString( Key.of( "feature2" ) ) ).isEqualTo( "callB" );
	}
}
