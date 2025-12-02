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

import ortus.boxlang.ai.util.KeyDictionary;
import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class AiServiceTest extends BaseIntegrationTest {

	@AfterEach
	public void clearServers() {
		// Clear all server instances after each test
		AiService aiService = ( AiService ) runtime.getGlobalService( KeyDictionary.AiService );
		if ( aiService != null ) {
			aiService.clearAllServers();
		}
	}

	@Test
	@DisplayName( "AiService is registered as a global service" )
	public void testAiServiceIsRegistered() {
		AiService aiService = ( AiService ) runtime.getGlobalService( KeyDictionary.AiService );
		assertThat( aiService ).isNotNull();
	}

	@Test
	@DisplayName( "AiService can store and retrieve MCP servers" )
	public void testStoreAndRetrieveServers() {
		// @formatter:off
		runtime.executeSource(
			"""
				server = mcpServer( "testApp" )
				serverName = server.getServerName()
			""",
			context
		);
		// @formatter:on

		AiService aiService = ( AiService ) runtime.getGlobalService( KeyDictionary.AiService );
		assertThat( aiService.getServerCount() ).isEqualTo( 1 );
		assertThat( aiService.hasServer( Key.of( "testApp" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "serverName" ) ) ).isEqualTo( "testApp" );
	}

	@Test
	@DisplayName( "AiService returns same instance for same server name" )
	public void testSameInstanceByName() {
		// @formatter:off
		runtime.executeSource(
			"""
				server1 = mcpServer( "sameApp" )
				server2 = mcpServer( "sameApp" )
				areSame = server1 == server2
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "areSame" ) ) ).isEqualTo( true );

		AiService aiService = ( AiService ) runtime.getGlobalService( KeyDictionary.AiService );
		assertThat( aiService.getServerCount() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "AiService returns different instances for different names" )
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

		AiService aiService = ( AiService ) runtime.getGlobalService( KeyDictionary.AiService );
		assertThat( aiService.getServerCount() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "AiService supports force rebuild" )
	public void testForceRebuild() {
		// @formatter:off
		runtime.executeSource(
			"""
				server1 = mcpServer( "forceApp" ).setDescription( "Original" )
				desc1 = server1.getDescription()

				server2 = mcpServer( name: "forceApp", force: true ).setDescription( "Rebuilt" )
				desc2 = server2.getDescription()

				// server1 should still have original description (it's a different object reference now)
				// server2 is the new rebuilt server
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "desc1" ) ) ).isEqualTo( "Original" );
		assertThat( variables.get( Key.of( "desc2" ) ) ).isEqualTo( "Rebuilt" );

		AiService aiService = ( AiService ) runtime.getGlobalService( KeyDictionary.AiService );
		assertThat( aiService.getServerCount() ).isEqualTo( 1 );
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
		AiService aiService = ( AiService ) runtime.getGlobalService( KeyDictionary.AiService );

		assertThat( aiService ).isNotNull();
		assertThat( aiService.getName() ).isEqualTo( KeyDictionary.AiService );
		assertThat( aiService.getServerCount() ).isEqualTo( 0 );

		// Now use BoxLang to create a server
		// @formatter:off
		runtime.executeSource(
			"""
				mcpServer( "directAccess" )
			""",
			context
		);
		// @formatter:on

		// Verify via Java
		assertThat( aiService.getServerCount() ).isEqualTo( 1 );
		assertThat( aiService.hasServer( Key.of( "directAccess" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "AiService logger works correctly" )
	public void testLoggerWorks() {
		AiService aiService = ( AiService ) runtime.getGlobalService( KeyDictionary.AiService );
		assertThat( aiService.getLogger() ).isNotNull();
	}

}
