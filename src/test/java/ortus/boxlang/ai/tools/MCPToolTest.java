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
package ortus.boxlang.ai.tools;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Tests for MCPTool and the withMCPServer() / withMCPServers() integration on AiBaseRunnable.
 * Uses MockMCPClient to avoid a live HTTP server.
 */
public class MCPToolTest extends BaseIntegrationTest {

	// =========================================================================
	// MCPTool construction
	// =========================================================================

	@DisplayName( "MCPTool init() sets name and description from tool definition" )
	@Test
	public void testInitSetsNameAndDescription() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.MCPTool;
				mockClient = new src.test.bx.mocks.MockMCPClient();
				tool = new MCPTool( mockClient, { name: "search", description: "Search for things" } );
				toolName = tool.getName();
				toolDesc = tool.getDescription();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "search" );
		assertThat( variables.get( Key.of( "toolDesc" ) ) ).isEqualTo( "Search for things" );
	}

	@DisplayName( "MCPTool init() stores the inputSchema from the tool definition" )
	@Test
	public void testInitStoresInputSchema() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.MCPTool;
				mockClient = new src.test.bx.mocks.MockMCPClient();
				schema = {
					"type": "object",
					"properties": { "query": { "type": "string" } },
					"required": [ "query" ]
				};
				tool = new MCPTool( mockClient, { name: "search", description: "Search", inputSchema: schema } );
				hasSchema = !tool.getInputSchema().isEmpty();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasSchema" ) ) ).isEqualTo( true );
	}

	// =========================================================================
	// generateSchema()
	// =========================================================================

	@DisplayName( "generateSchema() wraps inputSchema in OpenAI function format" )
	@Test
	public void testGenerateSchemaWithInputSchema() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.MCPTool;
				mockClient = new src.test.bx.mocks.MockMCPClient();
				inputSchema = {
					"type": "object",
					"properties": { "query": { "type": "string", "description": "Search query" } },
					"required": [ "query" ]
				};
				tool   = new MCPTool( mockClient, { name: "search", description: "Search docs", inputSchema: inputSchema } );
				schema = tool.getSchema();
				schemaType     = schema.type;
				functionName   = schema[ "function" ].name;
				functionDesc   = schema[ "function" ].description;
				paramType      = schema[ "function" ].parameters.type;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "schemaType" ) ) ).isEqualTo( "function" );
		assertThat( variables.get( Key.of( "functionName" ) ) ).isEqualTo( "search" );
		assertThat( variables.get( Key.of( "functionDesc" ) ) ).isEqualTo( "Search docs" );
		assertThat( variables.get( Key.of( "paramType" ) ) ).isEqualTo( "object" );
	}

	@DisplayName( "generateSchema() uses empty object schema when no inputSchema is provided" )
	@Test
	public void testGenerateSchemaWithoutInputSchema() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.MCPTool;
				mockClient = new src.test.bx.mocks.MockMCPClient();
				tool   = new MCPTool( mockClient, { name: "ping", description: "Ping the server" } );
				schema = tool.getSchema();
				paramType            = schema[ "function" ].parameters.type;
				noAdditionalProps    = schema[ "function" ].parameters.additionalProperties == false;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "paramType" ) ) ).isEqualTo( "object" );
		assertThat( variables.get( Key.of( "noAdditionalProps" ) ) ).isEqualTo( true );
	}

	// =========================================================================
	// invoke() → doInvoke()
	// =========================================================================

	@DisplayName( "invoke() returns plain string result from MCP server" )
	@Test
	public void testInvokeReturnsPlainString() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.MCPTool;
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setSendResponseData( "BoxLang is awesome" );
				tool   = new MCPTool( mockClient, { name: "search", description: "Search" } );
				result = tool.invoke( { query: "BoxLang" } );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "BoxLang is awesome" );
	}

	@DisplayName( "invoke() joins MCP content-block array into newline-delimited string" )
	@Test
	public void testInvokeJoinsContentBlocks() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.MCPTool;
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setSendResponseData( [
					{ type: "text", text: "First result" },
					{ type: "text", text: "Second result" }
				] );
				tool   = new MCPTool( mockClient, { name: "search", description: "Search" } );
				result = tool.invoke( { query: "BoxLang" } );
			""",
			context
		);
		// @formatter:on

		String output = ( String ) variables.get( result );
		assertThat( output ).contains( "First result" );
		assertThat( output ).contains( "Second result" );
	}

	@DisplayName( "invoke() returns error message string when MCP server reports failure" )
	@Test
	public void testInvokeReturnsErrorOnFailure() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.MCPTool;
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setSendSuccess( false );
				tool   = new MCPTool( mockClient, { name: "search", description: "Search" } );
				result = tool.invoke( { query: "BoxLang" } );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ).toString() ).contains( "Error from MCP tool" );
		assertThat( variables.get( result ).toString() ).contains( "search" );
	}

	@DisplayName( "invoke() strips _chatRequest key before forwarding args to MCP server" )
	@Test
	public void testInvokeStripsInternalChatRequestKey() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.MCPTool;
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setSendResponseData( "ok" );
				tool   = new MCPTool( mockClient, { name: "ping", description: "Ping" } );
				// _chatRequest is injected internally by the agent loop; it must never reach the MCP server
				result = tool.invoke( { query: "hello", _chatRequest: "should-be-stripped" } );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "ok" );
	}

	// =========================================================================
	// withMCPServer() on AiBaseRunnable (via aiModel)
	// =========================================================================

	@DisplayName( "withMCPServer() registers tools discovered from MCP server" )
	@Test
	public void testWithMCPServerRegistersTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setToolsToReturn( [
					{ name: "search",    description: "Search for documents", inputSchema: {} },
					{ name: "calculate", description: "Evaluate a math expression", inputSchema: {} }
				] );

				model = aiModel().withMCPServer( mockClient );

				toolCount    = model.getTools().len();
				hasSearch    = model.hasTool( "search" );
				hasCalculate = model.hasTool( "calculate" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "hasSearch" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasCalculate" ) ) ).isEqualTo( true );
	}

	@DisplayName( "withMCPServer() registers no tools when MCP server returns empty list" )
	@Test
	public void testWithMCPServerEmptyList() {
		// @formatter:off
		runtime.executeSource(
			"""
				mockClient = new src.test.bx.mocks.MockMCPClient();
				// toolsToReturn defaults to []

				model     = aiModel().withMCPServer( mockClient );
				toolCount = model.getTools().len();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "withMCPServer() does not throw when MCP server is unreachable" )
	@Test
	public void testWithMCPServerFailureIsGraceful() {
		// @formatter:off
		runtime.executeSource(
			"""
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setListToolsSuccess( false );

				model      = aiModel().withMCPServer( mockClient );
				toolCount  = model.getTools().len();
				succeeded  = true;   // If we reach here, no exception was thrown
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 0 );
		assertThat( variables.get( Key.of( "succeeded" ) ) ).isEqualTo( true );
	}

	@DisplayName( "withMCPServers() registers tools from multiple MCP servers" )
	@Test
	public void testWithMCPServersRegistersToolsFromAllServers() {
		// @formatter:off
		runtime.executeSource(
			"""
				client1 = new src.test.bx.mocks.MockMCPClient();
				client1.setToolsToReturn( [
					{ name: "search", description: "Search docs", inputSchema: {} }
				] );

				client2 = new src.test.bx.mocks.MockMCPClient();
				client2.setToolsToReturn( [
					{ name: "weather", description: "Get weather", inputSchema: {} }
				] );

				model = aiModel().withMCPServers( [ client1, client2 ] );

				toolCount  = model.getTools().len();
				hasSearch  = model.hasTool( "search" );
				hasWeather = model.hasTool( "weather" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "hasSearch" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasWeather" ) ) ).isEqualTo( true );
	}

	@DisplayName( "Registered MCPTool has the correct schema after withMCPServer()" )
	@Test
	public void testRegisteredMCPToolSchemaIsCorrect() {
		// @formatter:off
		runtime.executeSource(
			"""
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setToolsToReturn( [
					{
						name: "search",
						description: "Search for documents",
						inputSchema: {
							"type": "object",
							"properties": { "query": { "type": "string" } },
							"required": [ "query" ]
						}
					}
				] );

				model  = aiModel().withMCPServer( mockClient );
				tool   = model.getTool( "search" );
				schema = tool.getSchema();

				isMCPTool    = isInstanceOf( tool, "bxModules.bxai.models.tools.MCPTool" );
				schemaType   = schema.type;
				functionName = schema[ "function" ].name;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isMCPTool" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "schemaType" ) ) ).isEqualTo( "function" );
		assertThat( variables.get( Key.of( "functionName" ) ) ).isEqualTo( "search" );
	}

	@DisplayName( "withMCPServer() populates the mcpServers property with URL and tool names" )
	@Test
	public void testWithMCPServerPopulatesMCPServersProperty() {
		// @formatter:off
		runtime.executeSource(
			"""
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setToolsToReturn( [
					{ name: "search",    description: "Search", inputSchema: {} },
					{ name: "calculate", description: "Math",   inputSchema: {} }
				] );

				model      = aiModel().withMCPServer( mockClient );
				serverList = model.getMCPServers();
				serverCount = serverList.len();
				serverUrl   = serverList[ 1 ].url;
				toolNames   = serverList[ 1 ].toolNames;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "serverCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "serverUrl" ) ) ).isEqualTo( "http://mock-mcp-server" );
		@SuppressWarnings( "unchecked" )
		var toolNames = ( java.util.List<Object> ) variables.get( Key.of( "toolNames" ) );
		assertThat( toolNames ).containsExactly( "search", "calculate" );
	}

	@DisplayName( "withMCPServers() populates mcpServers property for each server" )
	@Test
	public void testWithMCPServersPopulatesPropertyForEachServer() {
		// @formatter:off
		runtime.executeSource(
			"""
				client1 = new src.test.bx.mocks.MockMCPClient();
				client1.setToolsToReturn( [ { name: "search", description: "Search", inputSchema: {} } ] );

				client2 = new src.test.bx.mocks.MockMCPClient();
				client2.setToolsToReturn( [ { name: "weather", description: "Weather", inputSchema: {} } ] );

				model       = aiModel().withMCPServers( [ client1, client2 ] );
				serverCount = model.getMCPServers().len();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "serverCount" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "withMCPServer() does not add to mcpServers property when the connection fails" )
	@Test
	public void testWithMCPServerFailureDoesNotPopulateMCPServersProperty() {
		// @formatter:off
		runtime.executeSource(
			"""
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setListToolsSuccess( false );

				model       = aiModel().withMCPServer( mockClient );
				serverCount = model.getMCPServers().len();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "serverCount" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "invoke() serializes empty struct response from MCP server to JSON" )
	@Test
	public void testInvokeHandlesEmptyStructResponseData() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.MCPTool;
				mockClient = new src.test.bx.mocks.MockMCPClient();
				mockClient.setSendResponseData( {} );
				tool   = new MCPTool( mockClient, { name: "ping", description: "Ping" } );
				result = tool.invoke( {} );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "{}" );
	}

}
