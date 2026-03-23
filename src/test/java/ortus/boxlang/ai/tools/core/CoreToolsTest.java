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
package ortus.boxlang.ai.tools.core;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class CoreToolsTest extends BaseIntegrationTest {

	// -------------------------------------------------------------------------
	// Registration (module onLoad)
	// -------------------------------------------------------------------------

	@DisplayName( "now@bxai tool is registered in the registry after module load" )
	@Test
	public void testNowToolRegisteredOnLoad() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = aiToolRegistry().has( "now@bxai" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "now@bxai tool can be retrieved from the registry" )
	@Test
	public void testNowToolRetrievable() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool = aiToolRegistry().get( "now@bxai" )
				result   = tool.getName()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "now" );
	}

	// -------------------------------------------------------------------------
	// now() tool behaviour
	// -------------------------------------------------------------------------

	@DisplayName( "now@bxai tool invocation returns a non-empty string" )
	@Test
	public void testNowToolReturnsString() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool = aiToolRegistry().get( "now@bxai" )
				result   = tool.invoke( {} )
			""",
			context
		);
		// @formatter:on

		var nowResult = variables.getAsString( result );
		assertThat( nowResult ).isNotEmpty();
	}

	@DisplayName( "now@bxai tool can be resolved by bare name 'now'" )
	@Test
	public void testNowToolBareNameLookup() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool = aiToolRegistry().get( "now" )
				result   = tool.getName()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "now" );
	}

	@DisplayName( "now@bxai tool schema follows OpenAI function format" )
	@Test
	public void testNowToolSchemaFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool   = aiToolRegistry().get( "now@bxai" )
				schema = tool.getSchema()
				schemaType = schema.type
				funcName   = schema.function.name
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "schemaType" ) ) ).isEqualTo( "function" );
		assertThat( variables.get( Key.of( "funcName" ) ) ).isEqualTo( "now" );
	}

}
