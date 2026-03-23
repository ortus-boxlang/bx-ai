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
package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;

public class aiToolRegistryTest extends BaseIntegrationTest {

	@DisplayName( "aiToolRegistry() returns a non-null registry instance" )
	@Test
	public void testReturnsNonNull() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = aiToolRegistry()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isNotNull();
	}

	@DisplayName( "aiToolRegistry() returns the same singleton on repeated calls" )
	@Test
	public void testReturnsSingleton() {
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

	@DisplayName( "aiToolRegistry() register() and has() work together" )
	@Test
	public void testRegisterAndHas() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiToolRegistry()
				reg.register( name: "bifTestTool", description: "A BIF test tool", callback: () => "done" )
				result = reg.has( "bifTestTool" )
				// Cleanup
				reg.unregister( "bifTestTool" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "aiToolRegistry() get() retrieves a registered tool by key" )
	@Test
	public void testGetRegisteredTool() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiToolRegistry()
				reg.register( name: "bifGetTool", description: "Test get", callback: () => "value" )
				tool = reg.get( "bifGetTool" )
				result   = tool.getName()
				// Cleanup
				reg.unregister( "bifGetTool" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "bifGetTool" );
	}

	@DisplayName( "aiToolRegistry() size() reflects registered tools" )
	@Test
	public void testSize() {
		// @formatter:off
		runtime.executeSource(
			"""
				reg = aiToolRegistry()
				sizeBefore = reg.size()
				reg.register( name: "bifSizeTool", description: "Count test", callback: () => "x" )
				result = ( reg.size() > sizeBefore )
				// Cleanup
				reg.unregister( "bifSizeTool" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

}
