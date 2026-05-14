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
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class aiToolTest extends BaseIntegrationTest {

	@DisplayName( "aiTool() creates a ClosureTool with name and description" )
	@Test
	public void testCreateClosureTool() {
		// @formatter:off
		runtime.executeSource(
			"""
				result   = aiTool( "myTool", "A useful tool", () => "ok" )
				toolName = result.getName()
				toolDesc = result.getDescription()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isNotNull();
		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "myTool" );
		assertThat( variables.get( Key.of( "toolDesc" ) ) ).isEqualTo( "A useful tool" );
	}

	@DisplayName( "aiTool() pass-through returns the exact same ITool instance" )
	@Test
	public void testPassThroughReturnsSameInstance() {
		// @formatter:off
		runtime.executeSource(
			"""
				original = aiTool( "passThrough", "Original", () => "value" )
				result   = aiTool( original )
				same     = ( original === result )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "same" ) ) ).isEqualTo( true );
	}

	@DisplayName( "aiTool() getSchema() returns an OpenAI-compatible function schema struct" )
	@Test
	public void testGetSchemaStructure() {
		// @formatter:off
		runtime.executeSource(
			"""
				schema  = aiTool( "weather", "Get weather for a city", ( required string city ) => "sunny" ).getSchema()
				result      = schema.type
				funcName    = schema.function.name
				funcDesc    = schema.function.description
				paramType   = schema.function[ "function" ]?.parameters?.type ?: schema.function.parameters.type
				isStrict    = schema.function.strict ?: false
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "function" );
		assertThat( variables.get( Key.of( "funcName" ) ) ).isEqualTo( "weather" );
		assertThat( variables.get( Key.of( "funcDesc" ) ) ).isEqualTo( "Get weather for a city" );
		assertThat( variables.get( Key.of( "paramType" ) ) ).isEqualTo( "object" );
		assertThat( variables.get( Key.of( "isStrict" ) ) ).isEqualTo( false );
	}

	@DisplayName( "aiTool() records required parameters in schema" )
	@Test
	public void testSchemaRequiredParameters() {
		// @formatter:off
		runtime.executeSource(
			"""
				schema    = aiTool( "lookup", "Look something up", ( required string query ) => "result" ).getSchema()
				required  = schema.function.parameters.required
				result        = required.contains( "query" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "aiTool() describeArg() annotates argument descriptions in schema" )
	@Test
	public void testDescribeArgInSchema() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool   = aiTool( "finder", "Find things", ( required string query ) => "found" )
				              .describeArg( "query", "The search query to use" )
				schema = tool.getSchema()
				result     = schema.function.parameters.properties.query.description
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "The search query to use" );
	}

	@DisplayName( "aiTool() invoke() calls the underlying callable and returns a string" )
	@Test
	public void testInvoke() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool = aiTool( "echo", "Echo a value", ( required string input ) => arguments.input )
				result   = tool.invoke( { input: "hello" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "hello" );
	}

	@DisplayName( "aiTool() invoke() with struct result serializes to JSON string" )
	@Test
	public void testInvokeSerializesStructToJSON() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool = aiTool( "structTool", "Returns a struct", () => { name: "BoxLang", version: 1 } )
				result   = tool.invoke( {} )
				isString = isSimpleValue( result )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isString" ) ) ).isEqualTo( true );
	}

	@DisplayName( "aiTool() getSchema() maps numeric param to JSON Schema number type" )
	@Test
	public void testSchemaMapsNumericToNumber() {
		// @formatter:off
		runtime.executeSource(
			"""
				schema = aiTool( "calc", "Calculate", ( required string op, numeric factor = 1 ) => "ok" ).getSchema()
				opType     = schema.function.parameters.properties.op.type
				factorType = schema.function.parameters.properties.factor.type
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "opType" ) ) ).isEqualTo( "string" );
		assertThat( variables.get( Key.of( "factorType" ) ) ).isEqualTo( "number" );
	}

	@DisplayName( "aiTool() with no callable throws MissingCallable on invoke()" )
	@Test
	public void testMissingCallableThrows() {
		try {
			// @formatter:off
			runtime.executeSource(
				"""
					tool = aiTool( "noCallable", "No callable set" )
					tool.invoke( {} )
				""",
				context
			);
			// @formatter:on
			fail( "Expected exception was not thrown" );
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).contains( "No callable has been set" );
		}
	}

}
