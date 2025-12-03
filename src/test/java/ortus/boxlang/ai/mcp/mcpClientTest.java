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

	@DisplayName( "Can query real MCP server with searchDocumentation tool" )
	@Test
	public void testQueryRealMCPServer() {
		// Test calling the searchDocumentation tool
		// @formatter:off
		runtime.executeSource(
			"""
				target = MCP( "https://boxlang.ortusbooks.com/~gitbook/mcp" )
				response = target.send( "searchDocumentation", { "query": "variables scope" } )
				success = response.getSuccess()
				hasData = isStruct( response.getData() )

				println( "Success: " & success )
				println( "Error: " & response.getError() )
				println( "Status Code: " & response.getStatusCode() )
				println( "Has data: " & hasData )
				if( hasData ) {
					println( "Response keys: " & structKeyList( response.getData() ) )
				}
			""",
			context
		);
		// @formatter:on

		var	success	= variables.getAsBoolean( Key.of( "success" ) );
		var	hasData	= variables.getAsBoolean( Key.of( "hasData" ) );

		assertThat( success ).isEqualTo( true );
		assertThat( hasData ).isEqualTo( true );

	}

}
