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

public class GatewayRegistryTest extends BaseIntegrationTest {

	// -------------------------------------------------------------------------
	// Registration
	// -------------------------------------------------------------------------

	@DisplayName( "register() stores an IGateway instance and has() returns true" )
	@Test
	public void testRegisterStoresGateway() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiGatewayRegistry()
				gw  = aiGateway( "mock" )
				reg.register( gw )
				result = reg.has( "mock" )
				// Cleanup
				reg.unregister( "mock" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( true );
	}

	@DisplayName( "register() with module creates a namespaced key" )
	@Test
	public void testRegisterWithModuleNamespacesKey() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiGatewayRegistry()
				gw  = aiGateway( "mock" )
				reg.register( gw, "test-mod" )
				result     = reg.has( "mock@test-mod" )
				resultBare = reg.has( "mock" )
				// Cleanup
				reg.unregisterByModule( "test-mod" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "resultBare" ) ) ).isEqualTo( true );
	}

	@DisplayName( "register() without a valid IGateway throws InvalidArgument" )
	@Test
	public void testRegisterInvalidArgThrows() {
		try {
			// @formatter:off
			runtime.executeSource(
				"""
					aiGatewayRegistry().register( "notAGateway" )
				""",
				context
			);
			// @formatter:on
			fail( "Expected exception was not thrown" );
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).contains( "requires an IGateway instance" );
		}
	}

	// -------------------------------------------------------------------------
	// Retrieval
	// -------------------------------------------------------------------------

	@DisplayName( "get() by exact key returns the registered gateway" )
	@Test
	public void testGetByExactKey() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiGatewayRegistry()
				gw  = aiGateway( "mock" )
				reg.register( gw, "test-exact" )
				fetched = reg.get( "mock@test-exact" )
				result  = fetched.getName()
				// Cleanup
				reg.unregisterByModule( "test-exact" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( "mock" );
	}

	@DisplayName( "has() returns false for unregistered keys" )
	@Test
	public void testHasReturnsFalseForMissing() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = aiGatewayRegistry().has( "totallyMissingGateway_xyz" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( false );
	}

	// -------------------------------------------------------------------------
	// Module-scoped operations
	// -------------------------------------------------------------------------

	@DisplayName( "unregisterByModule() removes all gateways registered under that module" )
	@Test
	public void testUnregisterByModuleRemovesAllGateways() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiGatewayRegistry()
				gw1 = aiGateway( "mock" )
				gw2 = aiGateway( "mock" )
				reg.register( gw1, "test-cleanup" )
				reg.register( gw2, "test-cleanup2" )
				reg.unregisterByModule( "test-cleanup" )
				reg.unregisterByModule( "test-cleanup2" )
				result1 = reg.has( "mock@test-cleanup" )
				result2 = reg.has( "mock@test-cleanup2" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( false );
	}

	// -------------------------------------------------------------------------
	// Observability
	// -------------------------------------------------------------------------

	@DisplayName( "listGateways() returns a struct with name, capabilities, and module for each gateway" )
	@Test
	public void testListGatewaysReturnsInfo() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiGatewayRegistry()
				gw  = aiGateway( "mock" )
				reg.register( gw, "test-list" )
				listing = reg.listGateways()
				info    = listing[ "mock@test-list" ]
				result  = ( info.name == "mock" && isArray( info.capabilities ) && info.module == "test-list" )
				// Cleanup
				reg.unregisterByModule( "test-list" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( true );
	}

	// -------------------------------------------------------------------------
	// Singleton
	// -------------------------------------------------------------------------

	@DisplayName( "aiGatewayRegistry() returns the same singleton instance on repeated calls" )
	@Test
	public void testGetInstanceIsSingleton() {
		// @formatter:off
		runtime.executeSource(
			"""
				r1     = aiGatewayRegistry()
				r2     = aiGatewayRegistry()
				result = ( r1 === r2 )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( true );
	}

}
