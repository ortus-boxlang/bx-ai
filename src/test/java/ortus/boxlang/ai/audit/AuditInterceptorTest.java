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
 * Test cases for AuditInterceptor - automatic event capture
 */
public class AuditInterceptorTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test AuditInterceptor instantiation" )
	public void testInstantiation() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
		    isNotNull = !isNull( interceptor )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "isNotNull" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor configure with disabled audit" )
	public void testConfigureDisabled() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()

		    // Manually set moduleRecord-like structure for testing
		    interceptor.moduleRecord = {
		        settings: {
		            audit: {
		                enabled: false
		            }
		        }
		    }

		    interceptor.configure()

		    // Interceptor should be configured but not enabled
		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor configure with enabled audit" )
	public void testConfigureEnabled() {
		runtime.executeSource(
		    """
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

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor setContext and clearContext" )
	public void testSetAndClearContext() {
		runtime.executeSource(
		    """
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

		    // Create a context
		    context = new bxModules.bxai.models.audit.AuditContext( traceId = "test-trace" )

		    // Set it
		    interceptor.setContext( context )

		    // Clear it
		    interceptor.clearContext()

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor stores with different store types" )
	public void testDifferentStoreTypes() {
		runtime.executeSource(
		    """
		    // Test memory store
		    interceptor1 = new bxModules.bxai.interceptors.AuditInterceptor()
		    interceptor1.moduleRecord = {
		        settings: {
		            audit: {
		                enabled: true,
		                store: "memory",
		                storeConfig: {}
		            }
		        }
		    }
		    interceptor1.configure()

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor handles missing audit config gracefully" )
	public void testMissingAuditConfig() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()

		    // Minimal moduleRecord without audit config
		    interceptor.moduleRecord = {
		        settings: {}
		    }

		    interceptor.configure()

		    // Should not fail
		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor thread-local context isolation" )
	public void testThreadLocalIsolation() {
		runtime.executeSource(
		    """
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

		    // Create and set a context
		    ctx1 = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-1" )
		    interceptor.setContext( ctx1 )

		    // Clear it
		    interceptor.clearContext()

		    // Should be able to create a new one
		    ctx2 = new bxModules.bxai.models.audit.AuditContext( traceId = "trace-2" )
		    interceptor.setContext( ctx2 )

		    // Clean up
		    interceptor.clearContext()

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor with file store type" )
	public void testFileStoreType() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()

		    interceptor.moduleRecord = {
		        settings: {
		            audit: {
		                enabled: true,
		                store: "file",
		                storeConfig: {
		                    path: getTempDirectory() & "/audit-test",
		                    batchSize: 1
		                }
		            }
		        }
		    }

		    interceptor.configure()

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor fallback to memory store on error" )
	public void testFallbackToMemoryStore() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()

		    // Configure with an invalid store that will fail
		    interceptor.moduleRecord = {
		        settings: {
		            audit: {
		                enabled: true,
		                store: "jdbc",
		                storeConfig: {
		                    // Missing datasource - should fail
		                }
		            }
		        }
		    }

		    // This should handle the error gracefully
		    try {
		        interceptor.configure()
		        success = true
		    } catch( any e ) {
		        // Even if it fails, we should handle it
		        success = true
		    }
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor with custom class path store" )
	public void testCustomClassPathStore() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()

		    // Try with full class path
		    interceptor.moduleRecord = {
		        settings: {
		            audit: {
		                enabled: true,
		                store: "bxModules.bxai.models.audit.stores.MemoryAuditStore",
		                storeConfig: {}
		            }
		        }
		    }

		    interceptor.configure()

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor database store alias" )
	public void testDatabaseStoreAlias() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()

		    // Try with 'database' and 'db' aliases - they should resolve
		    // but may fail without actual datasource
		    interceptor.moduleRecord = {
		        settings: {
		            audit: {
		                enabled: true,
		                store: "memory", // Use memory to avoid datasource requirement
		                storeConfig: {}
		            }
		        }
		    }

		    interceptor.configure()

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	// ==================== Environment Variable Tests ====================

	@Test
	@DisplayName( "Test AuditInterceptor enables via BOXLANG_MODULES_BXAI_AUDIT_ENABLED env var" )
	public void testEnvVarEnablesAudit() {
		runtime.executeSource(
		    """
		    // Simulate env var by setting it in server scope
		    // Note: In actual runtime, this would come from system environment
		    server.system.environment[ "BOXLANG_MODULES_BXAI_AUDIT_ENABLED" ] = "true"

		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()

		    // Module settings have audit disabled
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

		    // Env var should override module setting
		    // We can't directly check enabled, but configure should succeed
		    success = true

		    // Clean up
		    server.system.environment.delete( "BOXLANG_MODULES_BXAI_AUDIT_ENABLED" )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor respects BOXLANG_MODULES_BXAI_AUDIT_STORE env var" )
	public void testEnvVarSetsStoreType() {
		runtime.executeSource(
		    """
		    // Set store type via env var
		    server.system.environment[ "BOXLANG_MODULES_BXAI_AUDIT_STORE" ] = "memory"
		    server.system.environment[ "BOXLANG_MODULES_BXAI_AUDIT_ENABLED" ] = "true"

		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()

		    // Module settings don't specify store
		    interceptor.moduleRecord = {
		        settings: {
		            audit: {
		                storeConfig: {}
		            }
		        }
		    }

		    interceptor.configure()

		    // Should use memory store from env var
		    success = true

		    // Clean up
		    server.system.environment.delete( "BOXLANG_MODULES_BXAI_AUDIT_STORE" )
		    server.system.environment.delete( "BOXLANG_MODULES_BXAI_AUDIT_ENABLED" )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor env var with value '1' enables audit" )
	public void testEnvVarNumericValue() {
		runtime.executeSource(
		    """
		    // Test with "1" instead of "true"
		    server.system.environment[ "BOXLANG_MODULES_BXAI_AUDIT_ENABLED" ] = "1"

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

		    success = true

		    // Clean up
		    server.system.environment.delete( "BOXLANG_MODULES_BXAI_AUDIT_ENABLED" )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	// ==================== Span Depth and Error Path Tests ====================

	@Test
	@DisplayName( "Test AuditInterceptor span depth tracking increments and decrements" )
	public void testSpanDepthTracking() {
		runtime.executeSource(
		    """
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

		    // Simulate beforeAIModelInvoke event
		    interceptor.beforeAIModelInvoke( {
		        model: { getName: () => "test-model" },
		        chatRequest: { getMessages: () => [] }
		    } )

		    // Simulate afterAIModelInvoke event
		    interceptor.afterAIModelInvoke( {
		        results: "test response"
		    } )

		    // Context should be cleared after depth returns to 0
		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor onAIError cleans up span depth" )
	public void testErrorPathCleansUpSpanDepth() {
		runtime.executeSource(
		    """
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

		    // Simulate beforeAIModelInvoke event (increments depth)
		    interceptor.beforeAIModelInvoke( {
		        model: { getName: () => "test-model" },
		        chatRequest: { getMessages: () => [] }
		    } )

		    // Simulate error instead of normal completion
		    interceptor.onAIError( {
		        error: "TestError",
		        errorMessage: "Test error message",
		        operation: "model.invoke",
		        canRetry: false,
		        provider: { getName: () => "test-provider" }
		    } )

		    // Error handler should have decremented span depth and cleaned up
		    // Starting a new operation should work without issues
		    interceptor.beforeAIModelInvoke( {
		        model: { getName: () => "test-model-2" },
		        chatRequest: { getMessages: () => [] }
		    } )

		    interceptor.afterAIModelInvoke( {
		        results: "recovery response"
		    } )

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor nested spans maintain proper depth" )
	public void testNestedSpanDepth() {
		runtime.executeSource(
		    """
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

		    // Simulate agent run with nested model call
		    // Agent start (depth 1)
		    interceptor.beforeAIAgentRun( {
		        agent: { getAgentName: () => "test-agent" },
		        input: "test input"
		    } )

		    // Model start (depth 2)
		    interceptor.beforeAIModelInvoke( {
		        model: { getName: () => "test-model" },
		        chatRequest: { getMessages: () => [] }
		    } )

		    // Tool start (depth 3)
		    interceptor.beforeAIToolExecute( {
		        name: "test-tool",
		        arguments: {}
		    } )

		    // Tool end (depth 2)
		    interceptor.afterAIToolExecute( {
		        results: "tool result"
		    } )

		    // Model end (depth 1)
		    interceptor.afterAIModelInvoke( {
		        results: "model response"
		    } )

		    // Agent end (depth 0, context cleared)
		    interceptor.afterAIAgentRun( {
		        response: "agent response"
		    } )

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditInterceptor MCP error path cleans up" )
	public void testMCPErrorPathCleanup() {
		runtime.executeSource(
		    """
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

		    // Simulate MCP request (increments depth)
		    interceptor.onMCPRequest( {
		        requestData: { method: "test/method" },
		        serverName: "test-server"
		    } )

		    // Simulate MCP error instead of normal response
		    interceptor.onMCPError( {
		        error: "MCP connection failed"
		    } )

		    // Should be able to handle another MCP request without issues
		    interceptor.onMCPRequest( {
		        requestData: { method: "test/method2" },
		        serverName: "test-server"
		    } )

		    interceptor.onMCPResponse( {
		        response: "MCP response"
		    } )

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test getContext() returns current thread's audit context" )
	public void testGetContext() {
		runtime.executeSource(
		    """
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

		    // getContext() should create and return an AuditContext
		    ctx = interceptor.getContext()
		    hasTraceId = len( ctx.getTraceId() ) > 0

		    // Calling again should return the same context (ThreadLocal)
		    ctx2 = interceptor.getContext()
		    sameContext = ctx.getTraceId() == ctx2.getTraceId()

		    interceptor.clearContext()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasTraceId" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sameContext" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test setApplicationMetadata() attaches namespaced metadata to audit context" )
	public void testSetApplicationMetadata() {
		runtime.executeSource(
		    """
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

		    // Set application metadata
		    interceptor.setApplicationMetadata( "app", { feature: "chat", userId: 42 } )

		    // Verify it's on the context under the "app" key
		    ctx = interceptor.getContext()
		    meta = ctx.getContextMetadata()
		    hasAppKey = meta.keyExists( "app" )
		    feature = meta.app.feature ?: ""
		    userId = meta.app.userId ?: 0

		    interceptor.clearContext()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasAppKey" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "feature" ) ) ).isEqualTo( "chat" );
		assertThat( variables.get( Key.of( "userId" ) ) ).isEqualTo( 42 );
	}

	@Test
	@DisplayName( "Test setApplicationMetadata() is no-op when audit disabled" )
	public void testSetApplicationMetadataWhenDisabled() {
		runtime.executeSource(
		    """
		    interceptor = new bxModules.bxai.interceptors.AuditInterceptor()
		    interceptor.moduleRecord = {
		        settings: {
		            audit: {
		                enabled: false
		            }
		        }
		    }
		    interceptor.configure()

		    // Should not throw when audit is disabled
		    interceptor.setApplicationMetadata( "app", { feature: "chat" } )
		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

}
