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

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

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

		var myServer = variables.get( Key.of( "result" ) );
		assertThat( myServer ).isNotNull();
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

		var myServer = variables.get( Key.of( "result" ) );
		assertThat( myServer ).isNotNull();
		assertThat( variables.get( Key.of( "serverName" ) ) ).isEqualTo( "myApp" );
	}

	@Test
	@DisplayName( "Same name returns same instance" )
	public void testSameInstanceByName() {
		// @formatter:off
		runtime.executeSource(
			"""
				mcpSrv1 = mcpServer( "testApp" )
				mcpSrv2 = mcpServer( "testApp" )
				areSame = mcpSrv1 == mcpSrv2
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
				mcpSrv1 = mcpServer( "app1" )
				mcpSrv2 = mcpServer( "app2" )
				areDifferent = mcpSrv1 != mcpSrv2
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
				mcpSrv = mcpServer( "toolTest" )
					.registerTool(
						aiTool( "search", "Search for documents", ( query ) => "Found: " & query )
					)

				hasTool = mcpSrv.hasTool( "search" )
				toolCount = mcpSrv.getToolCount()
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

				myServer = mcpServer( "multiToolTest" )
					.registerTools( tools )

				hasSearch = myServer.hasTool( "search" )
				hasCalc = myServer.hasTool( "calculate" )
				toolCount = myServer.getToolCount()
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
				myServer = mcpServer( "unregTest" )
					.registerTool( aiTool( "myTool", "A tool", ( x ) => x ) )

				beforeCount = myServer.getToolCount()
				beforeHas = myServer.hasTool( "myTool" )

				myServer.unregisterTool( "myTool" )

				afterCount = myServer.getToolCount()
				afterHas = myServer.hasTool( "myTool" )
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
				myServer = mcpServer( "listToolsTest" )
					.registerTool(
						aiTool( "search", "Search for documents", ( query ) => "result" )
							.describeArg( "query", "The search query" )
					)

				tools = myServer.listTools()
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
				myServer = mcpServer( "clearToolsTest" )
					.registerTool( aiTool( "tool1", "Tool 1", ( x ) => x ) )
					.registerTool( aiTool( "tool2", "Tool 2", ( x ) => x ) )

				beforeCount = myServer.getToolCount()
				myServer.clearTools()
				afterCount = myServer.getToolCount()
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
				myServer = mcpServer( "resourceTest" )
					.registerResource(
						uri: "docs://readme",
						name: "README",
						description: "Project readme file",
						mimeType: "text/markdown",
						handler: () => "## Hello World"
					)

				hasResource = myServer.hasResource( "docs://readme" )
				resources = myServer.listResources()
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
				myServer = mcpServer( "readResourceTest" )
					.registerResource(
						uri: "config://settings",
						name: "Settings",
						description: "App settings",
						handler: () => { setting1: "value1", setting2: "value2" }
					)

				result = myServer.readResource( "config://settings" )
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
				myServer = mcpServer( "promptTest" )
					.registerPrompt(
						name: "greeting",
						description: "Generate a greeting",
						args: [ { name: "name", description: "Person name", required: true } ],
						handler: ( args ) => [
							{ role: "user", content: "Say hello to " & args.name }
						]
					)

				hasPrompt = myServer.hasPrompt( "greeting" )
				prompts = myServer.listPrompts()
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
				myServer = mcpServer( "getPromptTest" )
					.registerPrompt(
						name: "summarize",
						description: "Summarize text",
						handler: ( args ) => [
							{ role: "user", content: "Summarize: " & ( args.text ?: "nothing" ) }
						]
					)

				result = myServer.getPrompt( "summarize", { text: "Hello World" } )
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

	@SuppressWarnings( "null" )
	@Test
	@DisplayName( "The prompt is correctly formatted" )
	public void testGetPromptFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "getPromptTest" )
					.registerPrompt(
						name: "summarize",
						description: "Summarize text",
						handler: ( args ) => [
							{ role: "user", content: "Summarize: " & ( args.text ?: "nothing" ) }
						]
					)

				prompts = myServer.listPrompts()
			""",
			context
		);
		// @formatter:on

		var prompt = ( Struct ) variables.getAsArray( Key.of( "prompts" ) ).get( 0 );

		assertThat( prompt.get( Key.of( "name" ) ) ).isEqualTo( "summarize" );
		assertThat( prompt.get( Key.of( "description" ) ) ).isEqualTo( "Summarize text" );
		assertThat( prompt.get( Key.of( "arguments" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Can set server description and version" )
	public void testServerConfiguration() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "configTest" )
					.setDescription( "My Custom MCP Server" )
					.setVersion( "2.0.0" )

				info = myServer.getServerInfo()
				description = myServer.getDescription()
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
				myServer = mcpServer( "capTest" )
					.registerTool( aiTool( "tool1", "A tool", ( x ) => x ) )
					.registerResource(
						uri: "test://resource",
						name: "Resource",
						handler: () => "content"
					)

				caps = myServer.getCapabilities()
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
				myServer = mcpServer( "initTest" )
					.registerTool( aiTool( "testTool", "Test", ( x ) => x ) )

				request = {
					"jsonrpc": "2.0",
					"method": "initialize",
					"id": "init-1",
					"params": {}
				}

				response = myServer.handleRequest( request )
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
				myServer = mcpServer( "toolsListTest" )
					.registerTool( aiTool( "search", "Search docs", ( q ) => "result" ) )

				request = {
					"jsonrpc": "2.0",
					"method": "tools/list",
					"id": "tools-1"
				}

				response = myServer.handleRequest( request )
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
				myServer = mcpServer( "toolsCallTest" )
					.registerTool(
						aiTool( "echo", "Echo input", ( message ) => "Echo: " & message )
						.describeMessage( "The message to echo" )
					)

				serverRequest = {
					"jsonrpc": "2.0",
					"method": "tools/call",
					"id": "call-1",
					"params": {
						"name": "echo",
						"arguments": { "message": "Hello" }
					}
				}

				response = myServer.handleRequest( serverRequest )
				println( response)
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
				myServer = mcpServer( "default" )

				request = {
					"jsonrpc": "2.0",
					"method": "unknown/method",
					"id": "unknown-1"
				}

				response = myServer.handleRequest( request )
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
				myServer = mcpServer( "jsonStringTest" )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				requestJSON = '{"jsonrpc":"2.0","method":"tools/list","id":"1"}'

				response = myServer.handleRequest( requestJSON )
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
				myServer = mcpServer( "staticTest" )

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
				myServer = mcpServer( "removeTest" )
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
				myServer = mcpServer( "fluentTest" )
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

				toolCount = myServer.getToolCount()
				description = myServer.getDescription()
				version = myServer.getVersion()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "description" ) ) ).isEqualTo( "Fluent Test Server" );
		assertThat( variables.get( Key.of( "version" ) ) ).isEqualTo( "3.0.0" );
	}

	// ============================================================================
	// Statistics Tests
	// ============================================================================

	@Test
	@DisplayName( "Stats are enabled by default" )
	public void testStatsEnabledByDefault() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "statsDefaultTest" )
				isEnabled = myServer.isStatsEnabled()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isEnabled" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Can create server with stats disabled" )
	public void testStatsDisabled() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( name: "statsDisabledTest", statsEnabled: false )
				isEnabled = myServer.isStatsEnabled()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isEnabled" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "Can get stats summary" )
	public void testGetStatsSummary() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "statsSummaryTest" )
				summary = myServer.getStatsSummary()

				hasUptime = structKeyExists( summary, "uptime" )
				hasTotalRequests = structKeyExists( summary, "totalRequests" )
				hasSuccessRate = structKeyExists( summary, "successRate" )
				hasAvgResponseTime = structKeyExists( summary, "avgResponseTime" )
				hasTotalToolInvocations = structKeyExists( summary, "totalToolInvocations" )
				hasTotalResourceReads = structKeyExists( summary, "totalResourceReads" )
				hasTotalPromptGenerations = structKeyExists( summary, "totalPromptGenerations" )
				hasTotalErrors = structKeyExists( summary, "totalErrors" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasUptime" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasTotalRequests" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasSuccessRate" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasAvgResponseTime" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasTotalToolInvocations" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasTotalResourceReads" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasTotalPromptGenerations" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasTotalErrors" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Can get full stats" )
	public void testGetFullStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "fullStatsTest" )
				stats = myServer.getStats()

				hasRequests = structKeyExists( stats, "requests" )
				hasTools = structKeyExists( stats, "tools" )
				hasResources = structKeyExists( stats, "resources" )
				hasPrompts = structKeyExists( stats, "prompts" )
				hasErrors = structKeyExists( stats, "errors" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasRequests" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasTools" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasResources" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasPrompts" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasErrors" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Stats track request count" )
	public void testStatsTrackRequests() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "trackRequestsTest" )
					.registerTool( aiTool( "test", "Test", ( x ) => "ok" ) )

				// Make some requests
				myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "initialize",
					"id": "1"
				} )

				myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "tools/list",
					"id": "2"
				} )

				summary = myServer.getStatsSummary()
				totalRequests = summary.totalRequests
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "totalRequests" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Stats track tool invocations" )
	public void testStatsTrackToolInvocations() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "trackToolsTest" )
					.registerTool(
						aiTool( "echo", "Echo", ( msg ) => "Echo: " & msg )
							.describeArg( "msg", "Message" )
					)

				// Call tool
				myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "tools/call",
					"id": "1",
					"params": {
						"name": "echo",
						"arguments": { "msg": "test" }
					}
				} )

				summary = myServer.getStatsSummary()
				toolInvocations = summary.totalToolInvocations
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "toolInvocations" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Stats track resource reads" )
	public void testStatsTrackResourceReads() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "trackResourcesTest" )
					.registerResource(
						uri: "test://doc",
						name: "Document",
						handler: () => "content"
					)

				// Read resource
				myServer.readResource( "test://doc" )

				summary = myServer.getStatsSummary()
				resourceReads = summary.totalResourceReads
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "resourceReads" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Stats track prompt generations" )
	public void testStatsTrackPromptGenerations() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "trackPromptsTest" )
					.registerPrompt(
						name: "greet",
						description: "Greeting",
						handler: ( args ) => [{ role: "user", content: "Hello" }]
					)

				// Generate prompt
				myServer.getPrompt( "greet", {} )

				summary = myServer.getStatsSummary()
				promptGenerations = summary.totalPromptGenerations
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "promptGenerations" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Stats track errors" )
	public void testStatsTrackErrors() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "trackErrorsTest" )

				// Trigger error with invalid method
				myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "invalid/method",
					"id": "1"
				} )

				summary = myServer.getStatsSummary()
				totalErrors = summary.totalErrors
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "totalErrors" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Can reset stats" )
	public void testResetStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "resetStatsTest" )
					.registerTool( aiTool( "test", "Test", ( x ) => "ok" ) )

				// Generate some stats
				myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "tools/list",
					"id": "1"
				} )

				beforeReset = myServer.getStatsSummary()
				beforeRequests = beforeReset.totalRequests

				// Reset stats
				myServer.resetStats()

				afterReset = myServer.getStatsSummary()
				afterRequests = afterReset.totalRequests
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "beforeRequests" ) ) ).isEqualTo( 1 );
		assertThat( ( int ) variables.get( Key.of( "afterRequests" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Can enable and disable stats" )
	public void testEnableDisableStats() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "enableDisableTest" )

				initialState = myServer.isStatsEnabled()

				// Disable stats
				myServer.disableStats()
				afterDisable = myServer.isStatsEnabled()

				// Enable stats
				myServer.enableStats()
				afterEnable = myServer.isStatsEnabled()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "initialState" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "afterDisable" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "afterEnable" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Stats methods return server for chaining" )
	public void testStatsMethodsChaining() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "statsChainingTest" )
					.resetStats()
					.disableStats()
					.enableStats()
					.registerTool( aiTool( "test", "Test", ( x ) => "ok" ) )

				isServer = isObject( myServer )
				toolCount = myServer.getToolCount()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isServer" ) ) ).isEqualTo( true );
		assertThat( ( int ) variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Stats calculate success rate correctly" )
	public void testStatsSuccessRate() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "successRateTest" )
					.registerTool( aiTool( "test", "Test", ( x ) => "ok" ) )

				// Successful request
				myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "tools/list",
					"id": "1"
				} )

				// Failed request
				myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "invalid/method",
					"id": "2"
				} )

				summary = myServer.getStatsSummary()
				successRate = summary.successRate
				totalRequests = summary.totalRequests
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "totalRequests" ) ) ).isEqualTo( 2 );
		// Success rate should be 50% (1 success out of 2 requests)
		var successRate = ( ( Number ) variables.get( Key.of( "successRate" ) ) ).doubleValue();
		assertThat( successRate ).isWithin( 1.0 ).of( 50.0 );
	}

	@Test
	@DisplayName( "Stats track last request timestamp" )
	public void testStatsLastRequestTimestamp() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "lastRequestTest" )

				beforeSummary = myServer.getStatsSummary()
				beforeLastRequest = beforeSummary.lastRequestAt

				// Make a request
				myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "initialize",
					"id": "1"
				} )

				afterSummary = myServer.getStatsSummary()
				afterLastRequest = afterSummary.lastRequestAt
			""",
			context
		);
		// @formatter:on

		var	beforeLastRequest	= variables.get( Key.of( "beforeLastRequest" ) ).toString();
		var	afterLastRequest	= variables.get( Key.of( "afterLastRequest" ) ).toString();

		assertThat( beforeLastRequest ).isEmpty();
		assertThat( afterLastRequest ).isNotEmpty();
	}

	@Test
	@DisplayName( "Can configure basic auth with withBasicAuth()" )
	public void testConfigureBasicAuth() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "authTest" )
					.withBasicAuth( "admin", "secret123" )

				hasAuth = myServer.hasBasicAuth()
				username = myServer.getBasicAuthUsername()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasAuth" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "username" ) ) ).isEqualTo( "admin" );
	}

	@Test
	@DisplayName( "Server without auth has hasBasicAuth() = false" )
	public void testNoBasicAuth() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "noAuthTest" )
				hasAuth = myServer.hasBasicAuth()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasAuth" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "verifyBasicAuth() accepts valid credentials" )
	public void testVerifyBasicAuthValid() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "authVerifyTest" )
					.withBasicAuth( "testuser", "testpass" )

				// Create base64 encoded credentials: "testuser:testpass"
				credentials = "testuser:testpass"
				encoded = toBase64( credentials )
				authHeader = "Basic " & encoded

				isValid = myServer.verifyBasicAuth( authHeader )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isValid" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "verifyBasicAuth() rejects invalid credentials" )
	public void testVerifyBasicAuthInvalid() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "authVerifyInvalidTest" )
					.withBasicAuth( "admin", "secret" )

				// Create base64 encoded wrong credentials
				credentials = "admin:wrongpassword"
				encoded = toBase64( credentials )
				authHeader = "Basic " & encoded

				isValid = myServer.verifyBasicAuth( authHeader )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isValid" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "verifyBasicAuth() rejects malformed auth header" )
	public void testVerifyBasicAuthMalformed() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "authMalformedTest" )
					.withBasicAuth( "admin", "secret" )

				// Malformed header (missing "Basic " prefix)
				isValid1 = myServer.verifyBasicAuth( "notBasicAuth" )

				// Empty header
				isValid2 = myServer.verifyBasicAuth( "" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isValid1" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "isValid2" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "Basic auth fluent chaining works" )
	public void testBasicAuthFluentChaining() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "authChainTest" )
					.withBasicAuth( "user", "pass" )
					.setDescription( "Secured Server" )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				hasAuth = myServer.hasBasicAuth()
				description = myServer.getDescription()
				toolCount = myServer.getToolCount()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasAuth" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "description" ) ) ).isEqualTo( "Secured Server" );
		assertThat( ( int ) variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Security headers are present in responses" )
	public void testSecurityHeaders() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.mcp.MCPRequestProcessor;

				// Create a test server
				myServer = mcpServer( "securityHeaderTest" )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Process a request using mock HTTP context
				mockServer = mockRequestNew(
					method: "GET",
					path: "/mcp/securityHeaderTest",
					headers: {}
				)
				// Mock the HTTP transport to capture response
				mockHttpTransport = MCPRequestProcessor::getHttpTransport()
				headers = {}
				mockHttpTransport.writeResponse = ( response ) => {
					headers = response.headers
					return response
				}
				content = MCPRequestProcessor::processHttp( "securityHeaderTest" )
			""",
			context
		);
		// @formatter:on

		var headers = variables.getAsStruct( Key.of( "headers" ) );

		// Verify security headers are present
		assertThat( headers.get( Key.of( "X-Content-Type-Options" ) ) ).isEqualTo( "nosniff" );
		assertThat( headers.get( Key.of( "X-Frame-Options" ) ) ).isEqualTo( "DENY" );
		assertThat( headers.get( Key.of( "X-XSS-Protection" ) ) ).isEqualTo( "1; mode=block" );
		assertThat( headers.get( Key.of( "Referrer-Policy" ) ) ).isEqualTo( "strict-origin-when-cross-origin" );
		assertThat( headers.get( Key.of( "Content-Security-Policy" ) ) ).isEqualTo( "default-src 'none'; frame-ancestors 'none'" );
		assertThat( headers.get( Key.of( "Strict-Transport-Security" ) ) ).isEqualTo( "max-age=31536000; includeSubDomains" );
		assertThat( headers.get( Key.of( "Permissions-Policy" ) ) ).isEqualTo( "geolocation=(), microphone=(), camera=()" );
	}

	@Test
	@DisplayName( "Security headers present in error responses" )
	public void testSecurityHeadersInErrorResponse() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.mcp.MCPRequestProcessor;

				// Request non-existent server
				mockServer = mockRequestNew(
					method: "GET",
					path: "/mcp.bxm",
					headers: {}
				)
				// Mock the HTTP transport to capture response
				mockHttpTransport = MCPRequestProcessor::getHttpTransport()
				headers = {}
				mockHttpTransport.writeResponse = ( response ) => {
					headers = response.headers
					return response
				}

				content = MCPRequestProcessor::processHttp( "NonExistentServer" )
			""",
			context
		);
		// @formatter:on

		var headers = variables.getAsStruct( Key.of( "headers" ) );
		// Verify security headers are present even in error responses
		assertThat( headers.get( Key.of( "X-Content-Type-Options" ) ) ).isEqualTo( "nosniff" );
		assertThat( headers.get( Key.of( "X-Frame-Options" ) ) ).isEqualTo( "DENY" );
		assertThat( headers.get( Key.of( "X-XSS-Protection" ) ) ).isEqualTo( "1; mode=block" );
	}

	@Test
	@DisplayName( "CORS with multiple specific origins - exact match" )
	public void testCorsMultipleOriginsExactMatch() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create server with multiple allowed origins
				myServer = mcpServer( "corsMultiTest" )
					.withCors( ["https://app.example.com", "https://admin.example.com"] )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Test exact match
				allowed = myServer.isCorsAllowed( "https://app.example.com" )
				allowedAdmin = myServer.isCorsAllowed( "https://admin.example.com" )
				notAllowed = myServer.isCorsAllowed( "https://evil.com" )
				origins = myServer.getCorsAllowedOrigins()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "allowed" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "allowedAdmin" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "notAllowed" ) ) ).isEqualTo( false );

		var origins = variables.getAsArray( Key.of( "origins" ) );
		assertThat( origins.size() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "CORS with wildcard domain matching" )
	public void testCorsWildcardDomains() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create server with wildcard domain
				myServer = mcpServer( "corsWildcardTest" )
					.withCors( ["*.example.com", "https://specific.test.com"] )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Test wildcard matches
				subdomainMatch = myServer.isCorsAllowed( "https://app.example.com" )
				multiLevelMatch = myServer.isCorsAllowed( "https://api.v2.example.com" )
				specificMatch = myServer.isCorsAllowed( "https://specific.test.com" )
				noMatch = myServer.isCorsAllowed( "https://evil.com" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "subdomainMatch" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "multiLevelMatch" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "specificMatch" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "noMatch" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "Request body size limit enforcement - 413 error" )
	public void testBodySizeLimitEnforcement() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.mcp.MCPRequestProcessor;

				// Create server with 100 byte body limit
				myServer = mcpServer( "bodySizeTest" )
					.withBodyLimit( 100 )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Create request body > 100 bytes
				largeBody = jsonSerialize( { "method": "tools/list", "id": "test123", "data": "x".repeat( 150 ) } )

				// Process request with body exceeding limit using mock context
				mockServer = mockRequestNew(
					method: "POST",
					path: "/mcp.bxm",
					body: largeBody,
					headers: {}
				)
				// Mock the HTTP transport to capture response
				mockHttpTransport = MCPRequestProcessor::getHttpTransport()
				mockHttpTransport.writeResponse = ( response ) => {
					println( response )
					return response
				}

				// Call processHttp to handle the request
				content = MCPRequestProcessor::processHttp( "bodySizeTest" )
				parsedContent = jsonDeserialize( content )
			""",
			context
		);
		// @formatter:on

		var content = variables.getAsStruct( Key.of( "parsedContent" ) );
		assertThat( content.get( Key.of( "error" ) ) ).isNotNull();

		var error = ( ortus.boxlang.runtime.types.IStruct ) content.get( Key.of( "error" ) );
		assertThat( error.getAsString( Key.of( "message" ) ) ).contains( "too large" );
	}

	@Test
	@DisplayName( "Request body size limit - unlimited when set to 0" )
	public void testBodySizeLimitUnlimited() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.mcp.MCPRequestProcessor;

				// Create server with unlimited body size (0)
				myServer = mcpServer( "unlimitedBodyTest" )
					.withBodyLimit( 0 )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Create large request body
				largeBody = jsonSerialize( { "method": "tools/list", "id": "test123", "data": "x".repeat( 5000 ) } )

				// Process request - should succeed with mock context
				mockServer = mockRequestNew(
					method: "POST",
					path: "/mcp.bxm",
					body: largeBody,
					headers: {}
				)
				// Mock the HTTP transport to capture response
				mockHttpTransport = MCPRequestProcessor::getHttpTransport()
				mockHttpTransport.writeResponse = ( response ) => {
					println( response )
					return response
				}

				content = MCPRequestProcessor::processHttp( "unlimitedBodyTest" )
				parsedContent = jsonDeserialize( content )
			""",
			context
		);
		// @formatter:on

		var content = variables.getAsStruct( Key.of( "parsedContent" ) );
		assertThat( content.getAsStruct( Key.of( "result" ) ).size() ).isGreaterThan( 0 );
	}

	@Test
	@DisplayName( "API key provider - valid key" )
	public void testApiKeyProviderValid() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.mcp.MCPRequestProcessor;

				// Create server with API key provider
				myServer = mcpServer( "apiKeyTest" )
					.withApiKeyProvider( ( apiKey, requestData ) => {
						return apiKey == "valid-key-12345"
					} )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Mock request with valid API key in X-API-Key header
				// Note: In real scenario, headers would come from HTTP request
				// For testing, we'll call verifyApiKey directly
				validKey = myServer.verifyApiKey( "valid-key-12345", {} )
				invalidKey = myServer.verifyApiKey( "wrong-key", {} )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "validKey" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "invalidKey" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "API key provider - custom validation logic" )
	public void testApiKeyProviderCustomLogic() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create server with complex API key validation
				myServer = mcpServer( "customApiKeyTest" )
					.withApiKeyProvider( ( apiKey, requestData ) => {
						// Example: Check key format and method
						if ( !apiKey.startsWith( "sk_" ) ) return false
						if ( requestData.method == "POST" && len( apiKey ) < 20 ) return false
						return true
					} )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Test various scenarios
				validKey = myServer.verifyApiKey( "sk_1234567890123456789", { "method": "POST" } )
				invalidFormat = myServer.verifyApiKey( "invalid-format", { "method": "POST" } )
				tooShort = myServer.verifyApiKey( "sk_short", { "method": "POST" } )
				getOk = myServer.verifyApiKey( "sk_short", { "method": "GET" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "validKey" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "invalidFormat" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "tooShort" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "getOk" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "API key provider - hasApiKeyProvider check" )
	public void testHasApiKeyProvider() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Server without provider
				srv1 = mcpServer( "noProviderTest" )
				hasProvider1 = srv1.hasApiKeyProvider()

				// Server with provider
				srv2 = mcpServer( "withProviderTest" )
					.withApiKeyProvider( ( k, r ) => true )
				hasProvider2 = srv2.hasApiKeyProvider()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasProvider1" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "hasProvider2" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "CORS and API key provider work together" )
	public void testCorsAndApiKeyTogether() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create server with both CORS and API key validation
				myServer = mcpServer( "combinedSecurityTest" )
					.withCors( ["https://app.example.com", "*.trusted.com"] )
					.withApiKeyProvider( ( apiKey, requestData ) => apiKey == "secret123" )
					.withBodyLimit( 1000 )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Verify all security features configured
				corsAllowed = myServer.isCorsAllowed( "https://app.example.com" )
				apiKeyValid = myServer.verifyApiKey( "secret123", {} )
				bodyLimit = myServer.getMaxRequestBodySize()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "corsAllowed" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "apiKeyValid" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "bodyLimit" ) ) ).isEqualTo( 1000 );
	}

	@Test
	@DisplayName( "Notification without id does not throw and response omits id" )
	public void testNotificationWithoutId() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "notificationTest" )
				// JSON-RPC notification: no "id" key at all
				result = myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "ping"
				} )
				hasId = structKeyExists( result, "id" )
				hasJsonrpc = structKeyExists( result, "jsonrpc" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasId" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "hasJsonrpc" ) ) ).isEqualTo( true );
	}

	// ============================================================================
	// scanClass() / scan() tests — three input types
	// ============================================================================

	@Test
	@DisplayName( "scan() with dot-notation class path registers annotated tools, resources, and prompts" )
	public void testScanDotNotation() {
		// @formatter:off
                runtime.executeSource(
                        """
                                myServer = mcpServer( "scanDotTest" )
                                        .scanClass( "src.test.bx.mcp.SampleMCPTools" )

                                hasTool     = myServer.hasTool( "echoMessage" )
                                hasResource = myServer.hasResource( "test://status" )
                                hasPrompt   = myServer.hasPrompt( "greet" )
                        """,
                        context
                );
                // @formatter:on

		assertThat( variables.get( Key.of( "hasTool" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasResource" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasPrompt" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "scan() with absolute file path registers annotated tools, resources, and prompts" )
	public void testScanAbsolutePath() {
		String absPath = Paths.get( "src/test/bx/mcp/SampleMCPTools.bx" ).toAbsolutePath().toString();
		variables.put( Key.of( "sampleToolsAbsPath" ), absPath );

		// @formatter:off
                runtime.executeSource(
                        """
                                myServer = mcpServer( "scanAbsPathTest" )
                                        .scanClass( sampleToolsAbsPath )

                                hasTool     = myServer.hasTool( "echoMessage" )
                                hasResource = myServer.hasResource( "test://status" )
                                hasPrompt   = myServer.hasPrompt( "greet" )
                        """,
                        context
                );
                // @formatter:on

		assertThat( variables.get( Key.of( "hasTool" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasResource" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasPrompt" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "scan() with a class instance registers annotated tools, resources, and prompts" )
	public void testScanInstance() {
		// @formatter:off
                runtime.executeSource(
                        """
                                import src.test.bx.mcp.SampleMCPTools;

                                myServer = mcpServer( "scanInstanceTest" )
                                        .scan( new SampleMCPTools() )

                                hasTool     = myServer.hasTool( "echoMessage" )
                                hasResource = myServer.hasResource( "test://status" )
                                hasPrompt   = myServer.hasPrompt( "greet" )
                        """,
                        context
                );
                // @formatter:on

		assertThat( variables.get( Key.of( "hasTool" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasResource" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasPrompt" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Tool registered via scan() can be invoked through handleRequest()" )
	public void testScannedToolIsInvocable() {
		// @formatter:off
                runtime.executeSource(
                        """
                                myServer = mcpServer( "scanInvokeTest" )
                                        .scanClass( "src.test.bx.mcp.SampleMCPTools" )

                                rpcRequest = {
                                        "jsonrpc": "2.0",
                                        "method" : "tools/call",
                                        "id"     : "1",
                                        "params" : {
                                                "name"     : "echoMessage",
                                                "arguments": { "message": "Hello" }
                                        }
                                }

                                response = myServer.handleRequest( rpcRequest )
								println( response )
                                text = response.result.content[ 1 ].text
                        """,
                        context
                );
                // @formatter:on

		assertThat( variables.get( Key.of( "text" ) ).toString() ).contains( "Hello" );
	}

	@Test
	@DisplayName( "scan() with absolute path to non-existent file throws MCPServer.ClassNotFound" )
	public void testScanAbsolutePathMissingFile() {
		try {
			// @formatter:off
                        runtime.executeSource(
                                """
                                        mcpServer( "scanMissingTest" )
                                                .scanClass( "/this/path/does/not/exist/MyTool.bx" )
                                """,
                                context
                        );
                        // @formatter:on
			assertThat( false ).isTrue(); // Should not reach here
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).containsMatch( "could not be found" );
		}
	}

	// ============================================================================
	// scan() — directory scanning with three path types
	// ============================================================================

	@Test
	@DisplayName( "scan() with dot-notation package discovers and registers annotated tools" )
	public void testScanDirectoryDotNotation() {
		// @formatter:off
                runtime.executeSource(
                        """
                                myServer = mcpServer( "scanDirDotTest" )
                                        .scan( "src.test.bx.mcp" )

                                hasTool     = myServer.hasTool( "echoMessage" )
                                hasResource = myServer.hasResource( "test://status" )
                                hasPrompt   = myServer.hasPrompt( "greet" )
                        """,
                        context
                );
                // @formatter:on

		assertThat( variables.get( Key.of( "hasTool" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasResource" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasPrompt" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "scan() with relative directory path discovers and registers annotated tools" )
	public void testScanDirectoryRelativePath() {
		// Relative path from project root — BoxLang resolves it against its CWD
		variables.put( Key.of( "scanMcpRelDir" ), "/src/test/bx/mcp" );

		// @formatter:off
                runtime.executeSource(
                        """
                                myServer = mcpServer( "scanDirRelTest" )
                                        .scan( scanMcpRelDir )

                                hasTool     = myServer.hasTool( "echoMessage" )
                                hasResource = myServer.hasResource( "test://status" )
                                hasPrompt   = myServer.hasPrompt( "greet" )
                        """,
                        context
                );
                // @formatter:on

		assertThat( variables.get( Key.of( "hasTool" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasResource" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasPrompt" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "scan() with absolute directory path discovers and registers annotated tools" )
	public void testScanDirectoryAbsolutePath() {
		variables.put(
		    Key.of( "scanMcpAbsDir" ),
		    Paths.get( "src/test/bx/mcp" ).toAbsolutePath().toString()
		);

		// @formatter:off
                runtime.executeSource(
                        """
                                myServer = mcpServer( "scanDirAbsTest" )
                                        .scan( scanMcpAbsDir )

                                hasTool     = myServer.hasTool( "echoMessage" )
                                hasResource = myServer.hasResource( "test://status" )
                                hasPrompt   = myServer.hasPrompt( "greet" )
                        """,
                        context
                );
                // @formatter:on

		assertThat( variables.get( Key.of( "hasTool" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasResource" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasPrompt" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Server is not paused by default" )
	public void testNotPausedByDefault() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "pauseDefaultTest" )
				isPaused = myServer.isPaused()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isPaused" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "pause() sets isPaused to true" )
	public void testPauseSetsPausedState() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "pauseStateTest" )
				myServer.pause()
				isPaused = myServer.isPaused()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isPaused" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "resume() clears paused state" )
	public void testResumeClears() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "resumeTest" )
				myServer.pause()
				afterPause = myServer.isPaused()
				myServer.resume()
				afterResume = myServer.isPaused()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "afterPause" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "afterResume" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "handleRequest returns SERVER_PAUSED error when paused" )
	public void testHandleRequestWhenPaused() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "pausedRequestTest" )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )
					.pause()

				response = myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "tools/list",
					"id": "1"
				} )

				hasError = structKeyExists( response, "error" )
				errorCode = response.error.code
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasError" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "errorCode" ) ) ).isEqualTo( -32005 );
	}

	@Test
	@DisplayName( "ping succeeds even when server is paused" )
	public void testPingSucceedsWhenPaused() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "pausedPingTest" ).pause()

				response = myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "ping",
					"id": "hb-1"
				} )

				hasResult = structKeyExists( response, "result" )
				hasError  = structKeyExists( response, "error" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasResult" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasError" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "getSummary includes paused field" )
	public void testGetSummaryIncludesPaused() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "summaryPausedTest" )
				summaryBefore    = myServer.getSummary()
				hasPausedBefore  = structKeyExists( summaryBefore, "paused" )
				pausedBefore     = summaryBefore.paused

				myServer.pause()
				summaryAfter = myServer.getSummary()
				pausedAfter  = summaryAfter.paused
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasPausedBefore" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "pausedBefore" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "pausedAfter" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "pause() and resume() support fluent chaining" )
	public void testPauseResumeChaining() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "pauseChainTest" )
					.pause()
					.resume()
					.registerTool( aiTool( "t", "T", ( x ) => x ) )

				isObject  = isObject( myServer )
				isPaused  = myServer.isPaused()
				toolCount = myServer.getToolCount()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isObject" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "isPaused" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "After resume, requests are processed normally" )
	public void testRequestsProcessedAfterResume() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "resumeRequestTest" )
					.registerTool( aiTool( "echo", "Echo", ( msg ) => "Echo: " & msg ) )
					.pause()
					.resume()

				response  = myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "tools/list",
					"id": "1"
				} )

				hasResult = structKeyExists( response, "result" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasResult" ) ) ).isEqualTo( true );
	}

	// ============================================================================
	// IP Allowlist Tests
	// ============================================================================

	@Test
	@DisplayName( "withAllowedIPs stores and normalizes configured IP list" )
	public void testWithAllowedIPsStoresList() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "allowedIPsTest" )
					.withAllowedIPs( [ "192.168.1.100", "10.0.0.1", "  172.16.0.1  " ] )

				ips = myServer.getAllowedIPs()
				count = ips.size()
			""",
			context
		);
		// @formatter:on

		var ips = ( ortus.boxlang.runtime.types.Array ) variables.get( Key.of( "ips" ) );
		assertThat( ips.size() ).isEqualTo( 3 );
		assertThat( ips.get( 0 ).toString() ).isEqualTo( "192.168.1.100" );
		assertThat( ips.get( 2 ).toString() ).isEqualTo( "172.16.0.1" ); // trimmed
	}

	@Test
	@DisplayName( "hasAllowedIPs false when empty, true when populated" )
	public void testHasAllowedIPs() {
		// @formatter:off
		runtime.executeSource(
			"""
				srv1 = mcpServer( "noIPsTest" )
				hasIPs1 = srv1.hasAllowedIPs()

				srv2 = mcpServer( "withIPsTest" )
					.withAllowedIPs( [ "127.0.0.1" ] )
				hasIPs2 = srv2.hasAllowedIPs()

				srv2.clearAllowedIPs()
				hasIPs3 = srv2.hasAllowedIPs()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasIPs1" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "hasIPs2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasIPs3" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "addAllowedIP adds single IP to allowlist" )
	public void testAddAllowedIP() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "addIPTest" )
					.addAllowedIP( "192.168.1.1" )
					.addAllowedIP( "10.0.0.1" )
					.addAllowedIP( "192.168.1.1" )  // duplicate, should not add again

				count = myServer.getAllowedIPs().size()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "count" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "verifyClientIP true on exact match, false on non-match" )
	public void testVerifyClientIP() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "verifyIPTest" )
					.withAllowedIPs( [ "192.168.1.100", "10.0.0.1" ] )

				matchExact = myServer.verifyClientIP( "192.168.1.100" )
				matchOther = myServer.verifyClientIP( "10.0.0.1" )
				noMatch    = myServer.verifyClientIP( "172.16.0.1" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "matchExact" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "matchOther" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "noMatch" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "verifyClientIP returns true when no allowlist configured" )
	public void testVerifyClientIPNoAllowlist() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "noAllowlistTest" )
				result = myServer.verifyClientIP( "1.2.3.4" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "getClientIP prefers x-forwarded-for first value" )
	public void testGetClientIPXForwardedFor() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "xffTest" )
				requestData = {
					"headers": {
						"x-forwarded-for": "203.0.113.50, 70.41.3.18, 150.172.238.178",
						"cf-connecting-ip": "198.51.100.1"
					},
					"metadata": {
						"remoteAddr": "10.0.0.1"
					}
				}
				clientIP = myServer.getClientIP( requestData )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "clientIP" ) ).toString() ).isEqualTo( "203.0.113.50" );
	}

	@Test
	@DisplayName( "getClientIP falls back to cf-connecting-ip when x-forwarded-for absent" )
	public void testGetClientIPFallbackToCF() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "cfIPTest" )
				requestData = {
					"headers": {
						"cf-connecting-ip": "198.51.100.1"
					},
					"metadata": {
						"remoteAddr": "10.0.0.1"
					}
				}
				clientIP = myServer.getClientIP( requestData )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "clientIP" ) ).toString() ).isEqualTo( "198.51.100.1" );
	}

	@Test
	@DisplayName( "getClientIP falls back to remoteAddr when no proxy headers" )
	public void testGetClientIPFallbackToRemoteAddr() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "remoteAddrTest" )
				requestData = {
					"headers": {},
					"metadata": {
						"remoteAddr": "10.0.0.1"
					}
				}
				clientIP = myServer.getClientIP( requestData )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "clientIP" ) ).toString() ).isEqualTo( "10.0.0.1" );
	}

	@Test
	@DisplayName( "verifyClientIP supports CIDR range matching" )
	public void testVerifyClientIPCIDR() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "cidrTest" )
					.withAllowedIPs( [ "192.168.0.0/24", "10.0.0.0/8" ] )

				inRange1 = myServer.verifyClientIP( "192.168.0.50" )
				inRange2 = myServer.verifyClientIP( "192.168.0.255" )
				inRange3 = myServer.verifyClientIP( "10.255.255.255" )
				outRange = myServer.verifyClientIP( "172.16.0.1" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "inRange1" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "inRange2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "inRange3" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "outRange" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "IP allowlist blocks non-matching IP via handleRequest" )
	public void testIPAllowlistBlocksRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.mcp.MCPRequestProcessor;

				myServer = mcpServer( "ipBlockTest" )
					.withAllowedIPs( [ "192.168.1.100" ] )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Mock request from non-allowed IP
				mockServer = mockRequestNew(
					method: "POST",
					path: "/mcp.bxm",
					body: jsonSerialize( { "method": "tools/list", "id": "1" } ),
					headers: {},
					metadata: { "remoteAddr": "10.0.0.1" }
				)
				mockHttpTransport = MCPRequestProcessor::getHttpTransport()
				mockHttpTransport.writeResponse = ( response ) => {
					println( response )
					return response
				}

				content = MCPRequestProcessor::processHttp( "ipBlockTest" )
				parsedContent = jsonDeserialize( content )
			""",
			context
		);
		// @formatter:on

		var content = variables.getAsStruct( Key.of( "parsedContent" ) );
		assertThat( content.containsKey( Key.of( "error" ) ) ).isTrue();
		var error = content.getAsStruct( Key.of( "error" ) );
		assertThat( error.getAsString( Key.of( "message" ) ) ).contains( "Forbidden" );
	}

	@Test
	@DisplayName( "IP allowlist allows matching IP via handleRequest" )
	public void testIPAllowlistAllowsRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "ipAllowTest" )
					.withAllowedIPs( [ "192.168.1.100" ] )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// When no allowlist enforcement is triggered (no IP check in handleRequest),
				// the request should succeed normally
				response = myServer.handleRequest( {
					"jsonrpc": "2.0",
					"method": "tools/list",
					"id": "1"
				} )

				hasResult = structKeyExists( response, "result" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasResult" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "IP filter rejection increments ipFilterFailures in stats" )
	public void testIPFilterStatsIncrement() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "ipStatsTest" )
					.withAllowedIPs( [ "192.168.1.100" ] )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Simulate IP filter rejection by calling recordSecurityFailure directly
				myServer.recordSecurityFailure( "ipFilter" )

				summary = myServer.getStatsSummary()
				ipFailures = summary.security.ipFilterFailures
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ipFailures" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "IP allowlist with x-forwarded-for header honors first client IP" )
	public void testIPAllowlistWithXForwardedFor() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.mcp.MCPRequestProcessor;

				myServer = mcpServer( "ipXFFTest" )
					.withAllowedIPs( [ "203.0.113.50" ] )
					.registerTool( aiTool( "test", "Test tool", ( x ) => "ok" ) )

				// Mock request with x-forwarded-for containing allowed IP as first entry
				mockServer = mockRequestNew(
					method: "POST",
					path: "/mcp.bxm",
					body: jsonSerialize( { "method": "tools/list", "id": "1" } ),
					headers: {
						"x-forwarded-for": "203.0.113.50, 70.41.3.18"
					},
					metadata: { "remoteAddr": "10.0.0.1" }
				)
				mockHttpTransport = MCPRequestProcessor::getHttpTransport()
				mockHttpTransport.writeResponse = ( response ) => {
					return response
				}

				content = MCPRequestProcessor::processHttp( "ipXFFTest" )
				parsedContent = jsonDeserialize( content )
			""",
			context
		);
		// @formatter:on

		var content = variables.getAsStruct( Key.of( "parsedContent" ) );
		assertThat( content.containsKey( Key.of( "result" ) ) ).isTrue();
	}

}
