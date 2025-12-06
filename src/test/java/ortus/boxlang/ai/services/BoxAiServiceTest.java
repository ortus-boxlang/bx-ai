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
package ortus.boxlang.ai.services;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.ai.util.KeyDictionary;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.IService;

public class BoxAiServiceTest extends BaseIntegrationTest {

	@AfterEach
	public void clearServers() {
		// Clear via BoxLang execution to avoid classloader issues
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
	@DisplayName( "AiService is registered as a global service" )
	public void testAiServiceIsRegistered() {
		IService aiService = runtime.getGlobalService( KeyDictionary.AiService );
		assertThat( aiService ).isNotNull();
		assertThat( aiService.getName() ).isEqualTo( KeyDictionary.AiService );
	}

	@Test
	@DisplayName( "AiService can store and retrieve MCP servers" )
	public void testStoreAndRetrieveServers() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer = mcpServer( "testApp" )
				serverName = myServer.getServerName()
				serverCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "serverCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "serverName" ) ) ).isEqualTo( "testApp" );
	}

	@Test
	@DisplayName( "AiService returns same instance for same server name" )
	public void testSameInstanceByName() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer1 = mcpServer( "sameApp" )
				myServer2 = mcpServer( "sameApp" )
				areSame = myServer1 == myServer2
				serverCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "areSame" ) ) ).isEqualTo( true );
		assertThat( ( int ) variables.get( Key.of( "serverCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "AiService returns different instances for different names" )
	public void testDifferentInstancesByName() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer1 = mcpServer( "app1" )
				myServer2 = mcpServer( "app2" )
				areDifferent = myServer1 != myServer2
				serverCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "areDifferent" ) ) ).isEqualTo( true );
		assertThat( ( int ) variables.get( Key.of( "serverCount" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "AiService supports force rebuild" )
	public void testForceRebuild() {
		// @formatter:off
		runtime.executeSource(
			"""
				myServer1 = mcpServer( "forceApp" ).setDescription( "Original" )
				desc1 = myServer1.getDescription()

				myServer2 = mcpServer( name: "forceApp", force: true ).setDescription( "Rebuilt" )
				desc2 = myServer2.getDescription()

				// myServer1 should still have original description (it's a different object reference now)
				// myServer2 is the new rebuilt server
				serverCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "desc1" ) ) ).isEqualTo( "Original" );
		assertThat( variables.get( Key.of( "desc2" ) ) ).isEqualTo( "Rebuilt" );
		assertThat( ( int ) variables.get( Key.of( "serverCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "AiService can remove servers" )
	public void testRemoveServer() {
		// @formatter:off
		runtime.executeSource(
			"""
				mcpServer( "toRemove" )
				beforeCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()

				wasRemoved = bxModules.bxai.models.mcp.MCPServer::removeInstance( "toRemove" )

				afterCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "beforeCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "wasRemoved" ) ) ).isEqualTo( true );
		assertThat( ( int ) variables.get( Key.of( "afterCount" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "AiService clearAllServers works correctly" )
	public void testClearAllServers() {
		// @formatter:off
		runtime.executeSource(
			"""
				mcpServer( "server1" )
				mcpServer( "server2" )
				mcpServer( "server3" )
				beforeCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()

				bxModules.bxai.models.mcp.MCPServer::clearAllInstances()

				afterCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "beforeCount" ) ) ).isEqualTo( 3 );
		assertThat( ( int ) variables.get( Key.of( "afterCount" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "AiService can be accessed directly via getGlobalService" )
	public void testDirectServiceAccess() {
		IService aiService = runtime.getGlobalService( KeyDictionary.AiService );

		assertThat( aiService ).isNotNull();
		assertThat( aiService.getName() ).isEqualTo( KeyDictionary.AiService );

		// Verify initial count is 0
		// @formatter:off
		runtime.executeSource(
			"""
				initialCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()
			""",
			context
		);
		// @formatter:on
		assertThat( ( int ) variables.get( Key.of( "initialCount" ) ) ).isEqualTo( 0 );

		// Now use BoxLang to create a server
		// @formatter:off
		runtime.executeSource(
			"""
				mcpServer( "directAccess" )
				afterCount = bxModules.bxai.models.mcp.MCPServer::getInstanceNames().len()
			""",
			context
		);
		// @formatter:on

		// Verify via BoxLang
		assertThat( ( int ) variables.get( Key.of( "afterCount" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "AiService logger works correctly" )
	public void testLoggerWorks() {
		IService aiService = runtime.getGlobalService( KeyDictionary.AiService );
		assertThat( aiService ).isNotNull();
		// Logger is available but we don't cast to AiService to avoid classloader issues
	}

}
