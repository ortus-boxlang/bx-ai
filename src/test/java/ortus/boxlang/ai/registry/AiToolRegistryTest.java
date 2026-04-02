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

public class AiToolRegistryTest extends BaseIntegrationTest {

	// -------------------------------------------------------------------------
	// Registration
	// -------------------------------------------------------------------------

	@DisplayName( "register() shorthand style builds and stores a ClosureTool" )
	@Test
	public void testRegisterShorthand() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiToolRegistry()
				reg.register( name: "shorthandTool", description: "Shorthand test", callback: () => "hello" )
				result = reg.has( "shorthandTool" )
				// Cleanup
				reg.unregister( "shorthandTool" )
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
				reg = aiToolRegistry()
				reg.register( name: "nsTool", description: "Namespaced", callback: () => "ns", module: "test-mod" )
				result     = reg.has( "nsTool@test-mod" )
				resultBare = reg.has( "nsTool" )
				// Cleanup
				reg.unregisterByModule( "test-mod" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
		// bare name lookup also resolves via partial-match
		assertThat( variables.get( Key.of( "resultBare" ) ) ).isEqualTo( true );
	}

	@DisplayName( "register() with an existing ITool instance stores it directly" )
	@Test
	public void testRegisterIToolInstance() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg  = aiToolRegistry()
				tool = aiTool( "iToolDirect", "Direct ITool registration", () => "direct" )
				reg.register( name: tool.getName(), item: tool )
				result = reg.has( "iToolDirect" )
				// Cleanup
				reg.unregister( "iToolDirect" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "register() without valid args throws InvalidArgument" )
	@Test
	public void testRegisterInvalidArgsThrows() {
		try {
			// @formatter:off
			runtime.executeSource(
				"""
					aiToolRegistry().register()
				""",
				context
			);
			// @formatter:on
			fail( "Expected exception was not thrown" );
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).contains( "requires an ITool instance" );
		}
	}

	// -------------------------------------------------------------------------
	// Retrieval
	// -------------------------------------------------------------------------

	@DisplayName( "get() by exact key returns the registered tool" )
	@Test
	public void testGetByExactKey() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiToolRegistry()
				reg.register( name: "exactGetTool", description: "Exact get test", callback: () => "found" )
				tool = reg.get( "exactGetTool" )
				result   = tool.getName()
				// Cleanup
				reg.unregister( "exactGetTool" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "exactGetTool" );
	}

	@DisplayName( "get() by bare name resolves a namespaced tool via partial match" )
	@Test
	public void testGetByBareNameResolvesNamespaced() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiToolRegistry()
				reg.register( name: "partialTool", description: "Partial match", callback: () => "partial", module: "test-partial" )
				tool = reg.get( "partialTool" )
				result   = tool.getName()
				// Cleanup
				reg.unregisterByModule( "test-partial" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "partialTool" );
	}

	@DisplayName( "has() returns false for unregistered keys" )
	@Test
	public void testHasReturnsFalseForMissing() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = aiToolRegistry().has( "totallyMissingTool_xyz" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	// -------------------------------------------------------------------------
	// Module-scoped operations
	// -------------------------------------------------------------------------

	@DisplayName( "unregisterByModule() removes all tools registered under that module" )
	@Test
	public void testUnregisterByModuleRemovesAllTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiToolRegistry()
				reg.register( name: "modTool1", description: "First",  callback: () => "1", module: "test-cleanup" )
				reg.register( name: "modTool2", description: "Second", callback: () => "2", module: "test-cleanup" )
				reg.unregisterByModule( "test-cleanup" )
				result1 = reg.has( "modTool1@test-cleanup" )
				result2 = reg.has( "modTool2@test-cleanup" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( false );
	}

	@DisplayName( "getByModule() returns only tools registered under that module" )
	@Test
	public void testGetByModuleFiltersCorrectly() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiToolRegistry()
				reg.register( name: "byModA", description: "A", callback: () => "a", module: "test-bymod" )
				reg.register( name: "byModB", description: "B", callback: () => "b", module: "test-bymod" )
				tools = reg.getByModule( "test-bymod" )
				result    = tools.len()
				// Cleanup
				reg.unregisterByModule( "test-bymod" )
			""",
			context
		);
		// @formatter:on

		assertThat( ( Integer ) variables.getAsInteger( result ) ).isAtLeast( 2 );
	}

	// -------------------------------------------------------------------------
	// resolveTools()
	// -------------------------------------------------------------------------

	@DisplayName( "resolveTools() converts string keys to ITool instances" )
	@Test
	public void testResolveToolsConvertsStrings() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiToolRegistry()
				reg.register( name: "resolveTool", description: "Resolve test", callback: () => "resolved" )
				resolved = aiToolRegistry().resolveTools( [ "resolveTool" ] )
				result       = resolved[ 1 ].getName()
				// Cleanup
				reg.unregister( "resolveTool" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "resolveTool" );
	}

	@DisplayName( "resolveTools() passes through existing ITool instances unchanged" )
	@Test
	public void testResolveToolsPassesThroughIToolInstances() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool     = aiTool( "passThroughResolve", "Pass-through", () => "pass" )
				resolved = aiToolRegistry().resolveTools( [ tool ] )
				result       = ( resolved[ 1 ] === tool )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// -------------------------------------------------------------------------
	// Singleton
	// -------------------------------------------------------------------------

	@DisplayName( "getInstance() returns the same instance on repeated calls" )
	@Test
	public void testGetInstanceIsSingleton() {
		// @formatter:off
		runtime.executeSource(
			"""
				r1 = aiToolRegistry()
				r2 = aiToolRegistry()
				result = ( r1 === r2 )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

}
