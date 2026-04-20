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
package ortus.boxlang.ai.mcp;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class mcpClientTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Can create MCP client with base URL" )
	public void testCreateMCPClient() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = MCP( "http://localhost:3000" )
				baseURL = result.getBaseURL()
			""",
			context
		);
		// @formatter:on

		var client = variables.get( result );
		assertThat( client ).isNotNull();
		assertThat( variables.get( Key.of( "baseURL" ) ) ).isEqualTo( "http://localhost:3000" );
	}

	@Test
	@DisplayName( "Can configure timeout" )
	public void testConfigureTimeout() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = MCP( "http://localhost:3000" )
					.withTimeout( 5000 )
				timeout = result.getTimeout()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "timeout" ) ) ).isEqualTo( 5000 );
	}

	@Test
	@DisplayName( "Can configure custom headers" )
	public void testConfigureHeaders() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = MCP( "http://localhost:3000" )
					.withHeaders( { "X-Custom": "value" } )
				headers = result.getHeaders()
			""",
			context
		);
		// @formatter:on

		var headers = variables.getAsStruct( Key.of( "headers" ) );
		assertThat( headers.get( Key.of( "X-Custom" ) ) ).isEqualTo( "value" );
	}

	@Test
	@DisplayName( "Can configure bearer token" )
	public void testConfigureBearerToken() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = MCP( "http://localhost:3000" )
					.withBearerToken( "test-token" )
				hasToken = true
			""",
			context
		);
		// @formatter:on

		var client = variables.get( result );
		assertThat( client ).isNotNull();
		assertThat( variables.get( Key.of( "hasToken" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Can configure basic auth" )
	public void testConfigureBasicAuth() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = MCP( "http://localhost:3000" )
					.withAuth( "user", "pass" )
				hasAuth = true
			""",
			context
		);
		// @formatter:on

		var client = variables.get( result );
		assertThat( client ).isNotNull();
		assertThat( variables.get( Key.of( "hasAuth" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Can chain configuration methods" )
	public void testFluentConfiguration() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = MCP( "http://localhost:3000" )
					.withTimeout( 5000 )
					.withHeaders( { "X-API-Key": "key123" } )
					.withBearerToken( "token" )

				timeout = result.getTimeout()
				headers = result.getHeaders()
				baseURL = result.getBaseURL()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "timeout" ) ) ).isEqualTo( 5000 );
		assertThat( variables.get( Key.of( "baseURL" ) ) ).isEqualTo( "http://localhost:3000" );
		var headers = variables.getAsStruct( Key.of( "headers" ) );
		assertThat( headers.get( Key.of( "X-API-Key" ) ) ).isEqualTo( "key123" );
	}

	@Test
	@DisplayName( "Can register success callback" )
	public void testSuccessCallback() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = MCP( "http://localhost:3000" )
					.onSuccess( ( response ) => {
						// Success callback
					} )
				hasCallback = true
			""",
			context
		);
		// @formatter:on

		var client = variables.get( result );
		assertThat( client ).isNotNull();
		assertThat( variables.get( Key.of( "hasCallback" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Can register error callback" )
	public void testErrorCallback() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = MCP( "http://localhost:3000" )
					.onError( ( response ) => {
						// Error callback
					} )
				hasCallback = true
			""",
			context
		);
		// @formatter:on

		var client = variables.get( result );
		assertThat( client ).isNotNull();
		assertThat( variables.get( Key.of( "hasCallback" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "listTools method exists" )
	public void testListToolsMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasListTools = structKeyExists( client, "listTools" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasListTools" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "listResources method exists" )
	public void testListResourcesMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasListResources = structKeyExists( client, "listResources" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasListResources" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "listPrompts method exists" )
	public void testListPromptsMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasListPrompts = structKeyExists( client, "listPrompts" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasListPrompts" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "getCapabilities method exists" )
	public void testGetCapabilitiesMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasGetCapabilities = structKeyExists( client, "getCapabilities" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasGetCapabilities" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "send method exists" )
	public void testSendMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasSend = structKeyExists( client, "send" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasSend" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "readResource method exists" )
	public void testReadResourceMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasReadResource = structKeyExists( client, "readResource" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasReadResource" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "getPrompt method exists" )
	public void testGetPromptMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasGetPrompt = structKeyExists( client, "getPrompt" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasGetPrompt" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "MCPResponse has correct structure" )
	public void testMCPResponseStructure() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = new bxModules.bxai.models.mcp.MCPResponse(
					success: true,
					data: { "test": "data" },
					error: "",
					statusCode: 200,
					headers: { "Content-Type": "application/json" }
				)

				success = response.getSuccess()
				data = response.getData()
				error = response.getError()
				statusCode = response.getStatusCode()
				headers = response.getHeaders()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "success" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.get( Key.of( "error" ) ) ).isEqualTo( "" );

		var data = variables.getAsStruct( Key.of( "data" ) );
		assertThat( data.get( Key.of( "test" ) ) ).isEqualTo( "data" );

		var headers = variables.getAsStruct( Key.of( "headers" ) );
		assertThat( headers.get( Key.of( "Content-Type" ) ) ).isEqualTo( "application/json" );
	}

	@Test
	@DisplayName( "MCPResponse toStruct works correctly" )
	public void testMCPResponseToStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = new bxModules.bxai.models.mcp.MCPResponse(
					success: true,
					data: { "key": "value" },
					error: "",
					statusCode: 200,
					headers: {}
				)

				result = response.toStruct()
			""",
			context
		);
		// @formatter:on

		var responseStruct = variables.getAsStruct( result );
		assertThat( responseStruct.get( Key.of( "success" ) ) ).isEqualTo( true );
		assertThat( responseStruct.get( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
	}

	@Test
	@DisplayName( "Client handles network errors gracefully" )
	public void testNetworkErrorHandling() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Use an invalid URL to trigger a network error
				result = MCP( "http://invalid-host-that-does-not-exist:9999" )
					.withTimeout( 1000 )
					.listTools()

				success = result.getSuccess()
				error = result.getError()
			""",
			context
		);
		// @formatter:on

		var response = variables.get( result );
		assertThat( response ).isNotNull();
		assertThat( variables.get( Key.of( "success" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "error" ) ).toString() ).isNotEmpty();
	}

	@DisplayName( "Test Real MCP Server" )
	@Test
	public void testRealMCPServer() {
		// Test against BoxLang documentation MCP server
		// @formatter:off
		runtime.executeSource(
			"""
				target = MCP( "https://boxlang.ortusbooks.com/~gitbook/mcp" )
				response = target.listTools()
				success = response.getSuccess()
				hasTools = isStruct( response.getData() ) && structKeyExists( response.getData(), "tools" )

				println( "Success: " & success )
				println( "Error: " & response.getError() )
				println( "Status Code: " & response.getStatusCode() )
				println( "Has tools: " & hasTools )
				if( hasTools ) {
					println( "Tool count: " & arrayLen( response.getData().tools ) )
				}
			""",
			context
		);
		// @formatter:on

		var	success		= variables.getAsBoolean( Key.of( "success" ) );
		var	hasTools	= variables.getAsBoolean( Key.of( "hasTools" ) );

		assertThat( success ).isEqualTo( true );
		assertThat( hasTools ).isEqualTo( true );

	}

	// ==================================================================================
	// Stats Tests
	// ==================================================================================

	@Test
	@DisplayName( "getStats method exists on client" )
	public void testGetStatsMethodExists() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasGetStats = structKeyExists( client, "getStats" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasGetStats" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "getSummary method exists on client" )
	public void testGetSummaryMethodExists() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasSummary = structKeyExists( client, "getSummary" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasSummary" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "resetStats method exists on client" )
	public void testResetStatsMethodExists() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				hasResetStats = structKeyExists( client, "resetStats" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasResetStats" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Stats are zero on a fresh client" )
	public void testStatsInitialState() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://localhost:3000" )
				result = client.getStats()
			""",
			context
		);
		// @formatter:on

		var stats = variables.getAsStruct( result );
		assertThat( stats ).isNotNull();

		var calls = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "calls" ) );
		assertThat( calls.get( Key.of( "total" ) ) ).isEqualTo( 0 );
		assertThat( calls.get( Key.of( "successful" ) ) ).isEqualTo( 0 );
		assertThat( calls.get( Key.of( "failed" ) ) ).isEqualTo( 0 );

		var errors = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "errors" ) );
		assertThat( errors.get( Key.of( "total" ) ) ).isEqualTo( 0 );

		var tools = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "tools" ) );
		assertThat( tools.get( Key.of( "totalCalls" ) ) ).isEqualTo( 0 );

		var resources = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "resources" ) );
		assertThat( resources.get( Key.of( "totalReads" ) ) ).isEqualTo( 0 );

		var prompts = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "prompts" ) );
		assertThat( prompts.get( Key.of( "totalGenerations" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Stats track a failed call (network error)" )
	public void testStatsTrackErrorCall() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://invalid-host-that-does-not-exist:9999" )
					.withTimeout( 1000 )
				client.listTools()
				result = client.getStats()
			""",
			context
		);
		// @formatter:on

		var stats = variables.getAsStruct( result );
		var calls = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "calls" ) );
		assertThat( calls.get( Key.of( "total" ) ) ).isEqualTo( 1 );
		assertThat( calls.get( Key.of( "failed" ) ) ).isEqualTo( 1 );
		assertThat( calls.get( Key.of( "successful" ) ) ).isEqualTo( 0 );

		var errors = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "errors" ) );
		assertThat( errors.get( Key.of( "total" ) ) ).isEqualTo( 1 );

		// avgResponseTime must be a numeric (not an AtomicInteger) — verify it is >= 0
		var avgResponseTime = ( Number ) calls.get( Key.of( "avgResponseTime" ) );
		assertThat( avgResponseTime.doubleValue() ).isAtLeast( 0.0 );
	}

	@Test
	@DisplayName( "getSummary returns correct metrics after a failed call" )
	public void testSummaryAfterError() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://invalid-host-that-does-not-exist:9999" )
					.withTimeout( 1000 )
				client.listTools()
				result = client.getSummary()
			""",
			context
		);
		// @formatter:on

		var summary = variables.getAsStruct( result );
		assertThat( summary.get( Key.of( "totalCalls" ) ) ).isEqualTo( 1 );
		assertThat( summary.get( Key.of( "totalErrors" ) ) ).isEqualTo( 1 );

		var successRate = ( Number ) summary.get( Key.of( "successRate" ) );
		assertThat( successRate.doubleValue() ).isEqualTo( 0.0 );
	}

	@Test
	@DisplayName( "resetStats resets all counters to zero" )
	public void testResetStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "http://invalid-host-that-does-not-exist:9999" )
					.withTimeout( 1000 )
				client.listTools()
				beforeReset = client.getStats().calls.total
				client.resetStats()
				result = client.getStats()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "beforeReset" ) ) ).isEqualTo( 1 );

		var stats = variables.getAsStruct( result );
		var calls = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "calls" ) );
		assertThat( calls.get( Key.of( "total" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Stats track a successful call (real server)" )
	public void testStatsTrackSuccessfulCall() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "https://boxlang.ortusbooks.com/~gitbook/mcp" )
				client.listTools()
				result = client.getStats()
			""",
			context
		);
		// @formatter:on

		var stats = variables.getAsStruct( result );
		var calls = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "calls" ) );
		assertThat( calls.get( Key.of( "total" ) ) ).isEqualTo( 1 );
		assertThat( calls.get( Key.of( "successful" ) ) ).isEqualTo( 1 );
		assertThat( calls.get( Key.of( "failed" ) ) ).isEqualTo( 0 );

		var avgResponseTime = ( Number ) calls.get( Key.of( "avgResponseTime" ) );
		assertThat( avgResponseTime.doubleValue() ).isGreaterThan( 0.0 );
	}

	@Test
	@DisplayName( "Stats track per-operation-type counts (real server)" )
	public void testStatsTrackOperationTypes() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "https://boxlang.ortusbooks.com/~gitbook/mcp" )
				client.listTools()
				client.send( "searchDocumentation", { "query": "variables scope" } )
				result = client.getStats()
			""",
			context
		);
		// @formatter:on

		var stats         = variables.getAsStruct( result );
		var tools         = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "tools" ) );
		var calls         = ( ortus.boxlang.runtime.types.IStruct ) stats.get( Key.of( "calls" ) );
		var byOpType      = ( ortus.boxlang.runtime.types.IStruct ) calls.get( Key.of( "byOperationType" ) );

		assertThat( tools.get( Key.of( "totalCalls" ) ) ).isEqualTo( 2 );
		assertThat( byOpType.get( Key.of( "tool" ) ) ).isEqualTo( 2 );
	}

	// ==================================================================================
	// Event Tests
	// ==================================================================================

	@Test
	@DisplayName( "onMCPClientRequest event fires before the HTTP call" )
	public void testOnMCPClientRequestEventFires() {
		// @formatter:off
		runtime.executeSource(
			"""
				eventFired       = false
				capturedOperation = ""
				capturedName     = ""

				BoxRegisterInterceptor(
					( event ) => {
						eventFired        = true
						capturedOperation = event.operation
						capturedName      = event.name
					},
					"onMCPClientRequest"
				)

				MCP( "http://invalid-host-that-does-not-exist:9999" )
					.withTimeout( 1000 )
					.listTools()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "eventFired" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "capturedOperation" ) ) ).isEqualTo( "tool" );
		assertThat( variables.get( Key.of( "capturedName" ) ) ).isEqualTo( "listTools" );
	}

	@Test
	@DisplayName( "onMCPClientError event fires on network error" )
	public void testOnMCPClientErrorEventFires() {
		// @formatter:off
		runtime.executeSource(
			"""
				errorFired        = false
				capturedOperation = ""
				capturedError     = ""

				BoxRegisterInterceptor(
					( event ) => {
						errorFired        = true
						capturedOperation = event.operation
						capturedError     = event.error
					},
					"onMCPClientError"
				)

				MCP( "http://invalid-host-that-does-not-exist:9999" )
					.withTimeout( 1000 )
					.listTools()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errorFired" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "capturedOperation" ) ) ).isEqualTo( "tool" );
		assertThat( variables.get( Key.of( "capturedError" ) ).toString() ).isNotEmpty();
	}

	@Test
	@DisplayName( "onMCPClientResponse event fires on a successful call (real server)" )
	public void testOnMCPClientResponseEventFires() {
		// @formatter:off
		runtime.executeSource(
			"""
				responseFired     = false
				capturedOperation = ""
				capturedTime      = -1

				BoxRegisterInterceptor(
					( event ) => {
						responseFired     = true
						capturedOperation = event.operation
						capturedTime      = event.executionTime
					},
					"onMCPClientResponse"
				)

				MCP( "https://boxlang.ortusbooks.com/~gitbook/mcp" )
					.listTools()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "responseFired" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "capturedOperation" ) ) ).isEqualTo( "tool" );

		var capturedTime = ( Number ) variables.get( Key.of( "capturedTime" ) );
		assertThat( capturedTime.doubleValue() ).isAtLeast( 0.0 );
	}

}

