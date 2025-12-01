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
package ortus.boxlang.ai.Mcp;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class mcpServerTest extends BaseIntegrationTest {

	@AfterEach
	public void clearServers() {
		// Clear all server instances after each test to avoid cross-test contamination
		// @formatter:off
		runtime.executeSource(
			"""
				bxModules.bxai.models.mcp.MCPServer::clearAllInstances()
			""",
			context
		);
		// @formatter:on
	}

	@Test
	@DisplayName( "Can create MCP server with default name" )
	public void testCreateMCPServerDefault() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = mcpServer()
				serverName = result.getServerName()
			""",
			context
		);
		// @formatter:on

		var server = variables.get( Key.of( "result" ) );
		assertThat( server ).isNotNull();
		assertThat( variables.get( Key.of( "serverName" ) ) ).isEqualTo( "default" );
	}

	@Test
	@DisplayName( "Can create MCP server with custom name" )
	public void testCreateMCPServerWithName() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = mcpServer( "myApp" )
				serverName = result.getServerName()
			""",
			context
		);
		// @formatter:on

		var server = variables.get( Key.of( "result" ) );
		assertThat( server ).isNotNull();
		assertThat( variables.get( Key.of( "serverName" ) ) ).isEqualTo( "myApp" );
	}

	@Test
	@DisplayName( "Same name returns same instance" )
	public void testSameInstanceByName() {
		// @formatter:off
		runtime.executeSource(
			"""
				server1 = mcpServer( "testApp" )
				server2 = mcpServer( "testApp" )
				areSame = server1 == server2
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "areSame" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Different names return different instances" )
	public void testDifferentInstancesByName() {
		// @formatter:off
		runtime.executeSource(
			"""
				server1 = mcpServer( "app1" )
				server2 = mcpServer( "app2" )
				areDifferent = server1 != server2
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "areDifferent" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Can register a tool" )
	public void testRegisterTool() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "toolTest" )
					.registerTool(
						aiTool( "search", "Search for documents", ( query ) => "Found: " & query )
					)

				hasTool = server.hasTool( "search" )
				toolCount = server.getToolCount()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasTool" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Can register multiple tools" )
	public void testRegisterMultipleTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				tools = [
					aiTool( "search", "Search docs", ( query ) => "search: " & query ),
					aiTool( "calculate", "Do math", ( expr ) => "calc: " & expr )
				]

				server = mcpServer( "multiToolTest" )
					.registerTools( tools )

				hasSearch = server.hasTool( "search" )
				hasCalc = server.hasTool( "calculate" )
				toolCount = server.getToolCount()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasSearch" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasCalc" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Can unregister a tool" )
	public void testUnregisterTool() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "unregTest" )
					.registerTool( aiTool( "myTool", "A tool", ( x ) => x ) )

				beforeCount = server.getToolCount()
				beforeHas = server.hasTool( "myTool" )

				server.unregisterTool( "myTool" )

				afterCount = server.getToolCount()
				afterHas = server.hasTool( "myTool" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "beforeCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "beforeHas" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "afterCount" ) ) ).isEqualTo( 0 );
		assertThat( variables.get( Key.of( "afterHas" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "Can list tools in MCP format" )
	public void testListTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "listToolsTest" )
					.registerTool(
						aiTool( "search", "Search for documents", ( query ) => "result" )
							.describeArg( "query", "The search query" )
					)

				tools = server.listTools()
				firstTool = tools[ 1 ]
			""",
			context
		);
		// @formatter:on

		var tools = variables.getAsArray( Key.of( "tools" ) );
		assertThat( tools.size() ).isEqualTo( 1 );

		var firstTool = variables.getAsStruct( Key.of( "firstTool" ) );
		assertThat( firstTool.get( Key.of( "name" ) ) ).isEqualTo( "search" );
		assertThat( firstTool.get( Key.of( "description" ) ) ).isEqualTo( "Search for documents" );
	}

	@Test
	@DisplayName( "Can clear all tools" )
	public void testClearTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "clearToolsTest" )
					.registerTool( aiTool( "tool1", "Tool 1", ( x ) => x ) )
					.registerTool( aiTool( "tool2", "Tool 2", ( x ) => x ) )

				beforeCount = server.getToolCount()
				server.clearTools()
				afterCount = server.getToolCount()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "beforeCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "afterCount" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Can register a resource" )
	public void testRegisterResource() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "resourceTest" )
					.registerResource(
						uri: "docs://readme",
						name: "README",
						description: "Project readme file",
						mimeType: "text/markdown",
						handler: () => "# Hello World"
					)

				hasResource = server.hasResource( "docs://readme" )
				resources = server.listResources()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasResource" ) ) ).isEqualTo( true );

		var resources = variables.getAsArray( Key.of( "resources" ) );
		assertThat( resources.size() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Can read a resource" )
	public void testReadResource() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "readResourceTest" )
					.registerResource(
						uri: "config://settings",
						name: "Settings",
						description: "App settings",
						handler: () => { setting1: "value1", setting2: "value2" }
					)

				result = server.readResource( "config://settings" )
				contents = result.contents[ 1 ]
			""",
			context
		);
		// @formatter:on

		var readResult = variables.getAsStruct( Key.of( "result" ) );
		assertThat( readResult.containsKey( Key.of( "contents" ) ) ).isTrue();

		var contents = variables.getAsStruct( Key.of( "contents" ) );
		assertThat( contents.get( Key.of( "uri" ) ) ).isEqualTo( "config://settings" );
	}

	@Test
	@DisplayName( "Can register a prompt" )
	public void testRegisterPrompt() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "promptTest" )
					.registerPrompt(
						name: "greeting",
						description: "Generate a greeting",
						arguments: [ { name: "name", description: "Person name", required: true } ],
						handler: ( args ) => [
							{ role: "user", content: "Say hello to " & args.name }
						]
					)

				hasPrompt = server.hasPrompt( "greeting" )
				prompts = server.listPrompts()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasPrompt" ) ) ).isEqualTo( true );

		var prompts = variables.getAsArray( Key.of( "prompts" ) );
		assertThat( prompts.size() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Can get a prompt" )
	public void testGetPrompt() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "getPromptTest" )
					.registerPrompt(
						name: "summarize",
						description: "Summarize text",
						handler: ( args ) => [
							{ role: "user", content: "Summarize: " & ( args.text ?: "nothing" ) }
						]
					)

				result = server.getPrompt( "summarize", { text: "Hello World" } )
				messages = result.messages
			""",
			context
		);
		// @formatter:on

		var getPromptResult = variables.getAsStruct( Key.of( "result" ) );
		assertThat( getPromptResult.containsKey( Key.of( "messages" ) ) ).isTrue();

		var messages = variables.getAsArray( Key.of( "messages" ) );
		assertThat( messages.size() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Can set server description and version" )
	public void testServerConfiguration() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "configTest" )
					.setDescription( "My Custom MCP Server" )
					.setVersion( "2.0.0" )

				info = server.getServerInfo()
				description = server.getDescription()
			""",
			context
		);
		// @formatter:on

		var info = variables.getAsStruct( Key.of( "info" ) );
		assertThat( info.get( Key.of( "name" ) ) ).isEqualTo( "configTest" );
		assertThat( info.get( Key.of( "version" ) ) ).isEqualTo( "2.0.0" );
		assertThat( variables.get( Key.of( "description" ) ) ).isEqualTo( "My Custom MCP Server" );
	}

	@Test
	@DisplayName( "Can get server capabilities" )
	public void testGetCapabilities() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "capTest" )
					.registerTool( aiTool( "tool1", "A tool", ( x ) => x ) )
					.registerResource(
						uri: "test://resource",
						name: "Resource",
						handler: () => "content"
					)

				caps = server.getCapabilities()
				hasTools = structKeyExists( caps, "tools" )
				hasResources = structKeyExists( caps, "resources" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasTools" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasResources" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Can handle initialize request" )
	public void testHandleInitializeRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "initTest" )
					.registerTool( aiTool( "testTool", "Test", ( x ) => x ) )

				request = {
					"jsonrpc": "2.0",
					"method": "initialize",
					"id": "init-1",
					"params": {}
				}

				response = server.handleRequest( request )
				hasResult = structKeyExists( response, "result" )
				hasProtocolVersion = structKeyExists( response.result, "protocolVersion" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasResult" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasProtocolVersion" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Can handle tools/list request" )
	public void testHandleToolsListRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "toolsListTest" )
					.registerTool( aiTool( "search", "Search docs", ( q ) => "result" ) )

				request = {
					"jsonrpc": "2.0",
					"method": "tools/list",
					"id": "tools-1"
				}

				response = server.handleRequest( request )
				tools = response.result.tools
				toolCount = arrayLen( tools )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Can handle tools/call request" )
	public void testHandleToolsCallRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "toolsCallTest" )
					.registerTool(
						aiTool( "echo", "Echo input", ( message ) => "Echo: " & message )
					)

				request = {
					"jsonrpc": "2.0",
					"method": "tools/call",
					"id": "call-1",
					"params": {
						"name": "echo",
						"arguments": { "message": "Hello" }
					}
				}

				response = server.handleRequest( request )
				content = response.result.content[ 1 ]
				text = content.text
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "text" ) ).toString() ).contains( "Echo: Hello" );
	}

	@Test
	@DisplayName( "Handles unknown method with error" )
	public void testHandleUnknownMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "unknownMethodTest" )

				request = {
					"jsonrpc": "2.0",
					"method": "unknown/method",
					"id": "unknown-1"
				}

				response = server.handleRequest( request )
				hasError = structKeyExists( response, "error" )
				errorCode = response.error.code
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasError" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "errorCode" ) ) ).isEqualTo( -32601 );
	}

	@Test
	@DisplayName( "Can handle JSON string request" )
	public void testHandleJSONStringRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "jsonStringTest" )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				requestJSON = '{"jsonrpc":"2.0","method":"tools/list","id":"1"}'

				response = server.handleRequest( requestJSON )
				hasResult = structKeyExists( response, "result" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasResult" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Static hasInstance works correctly" )
	public void testStaticHasInstance() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Before creating
				beforeHas = bxModules.bxai.models.mcp.MCPServer::hasInstance( "staticTest" )

				// Create instance
				server = mcpServer( "staticTest" )

				// After creating
				afterHas = bxModules.bxai.models.mcp.MCPServer::hasInstance( "staticTest" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "beforeHas" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "afterHas" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Static removeInstance works correctly" )
	public void testStaticRemoveInstance() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "removeTest" )
				beforeRemove = bxModules.bxai.models.mcp.MCPServer::hasInstance( "removeTest" )

				wasRemoved = bxModules.bxai.models.mcp.MCPServer::removeInstance( "removeTest" )

				afterRemove = bxModules.bxai.models.mcp.MCPServer::hasInstance( "removeTest" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "beforeRemove" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "wasRemoved" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "afterRemove" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "Static getInstanceNames works correctly" )
	public void testStaticGetInstanceNames() {
		// @formatter:off
		runtime.executeSource(
			"""
				mcpServer( "app1" )
				mcpServer( "app2" )
				mcpServer( "app3" )

				names = bxModules.bxai.models.mcp.MCPServer::getInstanceNames()
				nameCount = arrayLen( names )
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "nameCount" ) ) ).isAtLeast( 3 );
	}

	@Test
	@DisplayName( "Fluent API chaining works" )
	public void testFluentChaining() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "fluentTest" )
					.setDescription( "Fluent Test Server" )
					.setVersion( "3.0.0" )
					.registerTool( aiTool( "tool1", "Tool 1", ( x ) => x ) )
					.registerTool( aiTool( "tool2", "Tool 2", ( x ) => x ) )
					.registerResource(
						uri: "test://res",
						name: "Resource",
						handler: () => "content"
					)
					.registerPrompt(
						name: "prompt1",
						description: "Prompt 1",
						handler: ( args ) => [{ role: "user", content: "test" }]
					)

				toolCount = server.getToolCount()
				description = server.getDescription()
				version = server.getVersion()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "description" ) ) ).isEqualTo( "Fluent Test Server" );
		assertThat( variables.get( Key.of( "version" ) ) ).isEqualTo( "3.0.0" );
	}

}
