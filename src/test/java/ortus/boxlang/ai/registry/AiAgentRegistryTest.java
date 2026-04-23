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
package ortus.boxlang.ai.registry;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class AiAgentRegistryTest extends BaseIntegrationTest {

	// -------------------------------------------------------------------------
	// Registration
	// -------------------------------------------------------------------------

	@DisplayName( "register() stores an AiAgent instance and has() returns true" )
	@Test
	public void testRegisterStoresAgent() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg   = aiAgentRegistry()
				agent = aiAgent( name: "TestRegisterAgent", description: "Test agent" )
				reg.register( agent )
				result = reg.has( "TestRegisterAgent" )
				// Cleanup
				reg.unregister( "TestRegisterAgent" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "register() with module creates a namespaced key" )
	@Test
	public void testRegisterWithModuleNamespacesKey() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg   = aiAgentRegistry()
				agent = aiAgent( name: "NsAgent", description: "Namespaced agent" )
				reg.register( agent, "test-mod" )
				result     = reg.has( "NsAgent@test-mod" )
				resultBare = reg.has( "NsAgent" )
				// Cleanup
				reg.unregisterByModule( "test-mod" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "resultBare" ) ) ).isEqualTo( true );
	}

	@DisplayName( "register() without a valid AiAgent throws InvalidArgument" )
	@Test
	public void testRegisterInvalidArgThrows() {
		try {
			// @formatter:off
			runtime.executeSource(
				"""
					aiAgentRegistry().register( "notAnAgent" )
				""",
				context
			);
			// @formatter:on
			fail( "Expected exception was not thrown" );
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).contains( "requires an AiAgent instance" );
		}
	}

	// -------------------------------------------------------------------------
	// Retrieval
	// -------------------------------------------------------------------------

	@DisplayName( "get() by exact key returns the registered agent" )
	@Test
	public void testGetByExactKey() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg   = aiAgentRegistry()
				agent = aiAgent( name: "ExactGetAgent", description: "Exact get test" )
				reg.register( agent )
				fetched = reg.get( "ExactGetAgent" )
				result  = fetched.getAgentName()
				// Cleanup
				reg.unregister( "ExactGetAgent" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "ExactGetAgent" );
	}

	@DisplayName( "get() by bare name resolves a namespaced agent via partial match" )
	@Test
	public void testGetByBareNameResolvesNamespaced() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg   = aiAgentRegistry()
				agent = aiAgent( name: "PartialAgent", description: "Partial match" )
				reg.register( agent, "test-partial" )
				fetched = reg.get( "PartialAgent" )
				result  = fetched.getAgentName()
				// Cleanup
				reg.unregisterByModule( "test-partial" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "PartialAgent" );
	}

	@DisplayName( "has() returns false for unregistered keys" )
	@Test
	public void testHasReturnsFalseForMissing() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = aiAgentRegistry().has( "totallyMissingAgent_xyz" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	// -------------------------------------------------------------------------
	// Module-scoped operations
	// -------------------------------------------------------------------------

	@DisplayName( "unregisterByModule() removes all agents registered under that module" )
	@Test
	public void testUnregisterByModuleRemovesAllAgents() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg    = aiAgentRegistry()
				agent1 = aiAgent( name: "ModAgent1", description: "First" )
				agent2 = aiAgent( name: "ModAgent2", description: "Second" )
				reg.register( agent1, "test-cleanup" )
				reg.register( agent2, "test-cleanup" )
				reg.unregisterByModule( "test-cleanup" )
				result1 = reg.has( "ModAgent1@test-cleanup" )
				result2 = reg.has( "ModAgent2@test-cleanup" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( false );
	}

	@DisplayName( "getByModule() returns only agents registered under that module" )
	@Test
	public void testGetByModuleFiltersCorrectly() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg    = aiAgentRegistry()
				agent1 = aiAgent( name: "ByModA", description: "A" )
				agent2 = aiAgent( name: "ByModB", description: "B" )
				reg.register( agent1, "test-bymod" )
				reg.register( agent2, "test-bymod" )
				agents = reg.getByModule( "test-bymod" )
				result = agents.len()
				// Cleanup
				reg.unregisterByModule( "test-bymod" )
			""",
			context
		);
		// @formatter:on

		assertThat( ( Integer ) variables.getAsInteger( result ) ).isAtLeast( 2 );
	}

	// -------------------------------------------------------------------------
	// resolveAgents()
	// -------------------------------------------------------------------------

	@DisplayName( "resolveAgents() converts string keys to AiAgent instances" )
	@Test
	public void testResolveAgentsConvertsStrings() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg   = aiAgentRegistry()
				agent = aiAgent( name: "ResolveAgent", description: "Resolve test" )
				reg.register( agent )
				resolved = reg.resolveAgents( [ "ResolveAgent" ] )
				result   = resolved[ 1 ].getAgentName()
				// Cleanup
				reg.unregister( "ResolveAgent" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "ResolveAgent" );
	}

	@DisplayName( "resolveAgents() passes through existing AiAgent instances unchanged" )
	@Test
	public void testResolveAgentsPassesThroughInstances() {
		// @formatter:off
		runtime.executeSource(
			"""
				agent    = aiAgent( name: "PassThroughAgent", description: "Pass-through" )
				resolved = aiAgentRegistry().resolveAgents( [ agent ] )
				result   = ( resolved[ 1 ] === agent )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// -------------------------------------------------------------------------
	// Observability / Analytics
	// -------------------------------------------------------------------------

	@DisplayName( "listAgents() returns a struct with name, description, and module for each agent" )
	@Test
	public void testListAgentsReturnsInfo() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg   = aiAgentRegistry()
				agent = aiAgent( name: "ListAgent", description: "Listed agent" )
				reg.register( agent, "test-list" )
				listing = reg.listAgents()
				info    = listing[ "ListAgent@test-list" ]
				result  = ( info.name == "ListAgent" && info.description == "Listed agent" && info.module == "test-list" )
				// Cleanup
				reg.unregisterByModule( "test-list" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "getAgentInfo() returns name, description, and module for a registry key" )
	@Test
	public void testGetAgentInfoReturnsStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg   = aiAgentRegistry()
				agent = aiAgent( name: "InfoAgent", description: "Info test agent" )
				reg.register( agent, "test-info" )
				info   = reg.getAgentInfo( "InfoAgent@test-info" )
				result = ( info.name == "InfoAgent" && info.description == "Info test agent" && info.module == "test-info" )
				// Cleanup
				reg.unregisterByModule( "test-info" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// -------------------------------------------------------------------------
	// Auto-registration via aiAgent() BIF flag
	// -------------------------------------------------------------------------

	@DisplayName( "aiAgent( register: true ) automatically registers the agent in the registry" )
	@Test
	public void testAiAgentBifAutoRegister() {
		// @formatter:off
		runtime.executeSource(
			"""
				agent  = aiAgent( name: "AutoRegistered", description: "Auto-reg agent", register: true, module: "test-auto" )
				result = aiAgentRegistry().has( "AutoRegistered@test-auto" )
				// Cleanup
				aiAgentRegistry().unregisterByModule( "test-auto" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "aiAgent( register: false ) does not register the agent (default)" )
	@Test
	public void testAiAgentBifNoAutoRegisterByDefault() {
		// @formatter:off
		runtime.executeSource(
			"""
				agent  = aiAgent( name: "NotAutoRegistered_xyz", description: "Not registered" )
				result = aiAgentRegistry().has( "NotAutoRegistered_xyz" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	// -------------------------------------------------------------------------
	// Singleton
	// -------------------------------------------------------------------------

	@DisplayName( "aiAgentRegistry() returns the same singleton instance on repeated calls" )
	@Test
	public void testGetInstanceIsSingleton() {
		// @formatter:off
		runtime.executeSource(
			"""
				r1     = aiAgentRegistry()
				r2     = aiAgentRegistry()
				result = ( r1 === r2 )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

}
