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
package ortus.boxlang.ai.tools;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class ClosureToolTest extends BaseIntegrationTest {

	// -------------------------------------------------------------------------
	// Construction
	// -------------------------------------------------------------------------

	@DisplayName( "ClosureTool init() sets name, description, and callable" )
	@Test
	public void testInitSetsProperties() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "myTool", "Does things", () => "done" )
				name     = tool.getName()
				desc     = tool.getDescription()
				result   = ( !isNull( tool.getCallable() ) )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "myTool" );
		assertThat( variables.get( Key.of( "desc" ) ) ).isEqualTo( "Does things" );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "ClosureTool call() sets the callable fluently" )
	@Test
	public void testCallSetsFluently() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "fluentTool", "Fluent test" )
				            .call( () => "fluent" )
				result   = ( !isNull( tool.getCallable() ) )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	// -------------------------------------------------------------------------
	// doInvoke / invoke
	// -------------------------------------------------------------------------

	@DisplayName( "invoke() calls the callable with the provided args" )
	@Test
	public void testInvokeCallsCallable() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "echoTool", "Echoes an input", ( required string value ) => arguments.value )
				result   = tool.invoke( { value: "hello world" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "hello world" );
	}

	@DisplayName( "invoke() serializes a struct result to a JSON string" )
	@Test
	public void testInvokeSerializesStructResult() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "structTool", "Returns struct", () => { city: "London", temp: 20 } )
				result   = tool.invoke( {} )
				isString = isSimpleValue( result )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isString" ) ) ).isEqualTo( true );
	}

	@DisplayName( "invoke() without a callable throws MissingCallable" )
	@Test
	public void testInvokeWithNoCallableThrows() {
		try {
			// @formatter:off
			runtime.executeSource(
				"""
					import bxModules.bxai.models.tools.ClosureTool;
					tool = new ClosureTool( "noFn", "No callable" )
					tool.invoke( {} )
				""",
				context
			);
			// @formatter:on
			fail( "Expected MissingCallable exception was not thrown" );
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).contains( "No callable has been set" );
		}
	}

	// -------------------------------------------------------------------------
	// Schema generation
	// -------------------------------------------------------------------------

	@DisplayName( "getSchema() returns an OpenAI-compatible function schema" )
	@Test
	public void testGetSchemaReturnsOpenAIFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				schema = new ClosureTool( "greet", "Greet someone", ( required string personName ) => "Hi #arguments.personName#" ).getSchema()
				schemaType = schema.type
				funcName   = schema.function.name
				paramType  = schema.function.parameters.type
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "schemaType" ) ) ).isEqualTo( "function" );
		assertThat( variables.get( Key.of( "funcName" ) ) ).isEqualTo( "greet" );
		assertThat( variables.get( Key.of( "paramType" ) ) ).isEqualTo( "object" );
	}

	@DisplayName( "getSchema() marks required parameters correctly" )
	@Test
	public void testGetSchemaRequiredParameters() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				schema   = new ClosureTool( "search", "Search", ( required string query, numeric limit = 10 ) => "results" ).getSchema()
				required = schema.function.parameters.required
				result       = required.contains( "query" ) && !required.contains( "limit" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "getArgumentsSchema() throws MissingCallable when no callable is set" )
	@Test
	public void testGetArgumentsSchemaThrowsWithoutCallable() {
		try {
			// @formatter:off
			runtime.executeSource(
				"""
					import bxModules.bxai.models.tools.ClosureTool;
					tool = new ClosureTool( "noFnSchema", "No callable" )
					tool.getArgumentsSchema()
				""",
				context
			);
			// @formatter:on
			fail( "Expected MissingCallable exception was not thrown" );
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).contains( "No callable has been set" );
		}
	}

	// -------------------------------------------------------------------------
	// Fluent describe methods
	// -------------------------------------------------------------------------

	@DisplayName( "describeArg() adds argument description to schema properties" )
	@Test
	public void testDescribeArgAddsToSchema() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				schema = new ClosureTool( "located", "Find location", ( required string city ) => "lat,lon" )
				               .describeArg( "city", "The city to look up, e.g. 'Paris, France'" )
				               .getSchema()
				result = schema.function.parameters.properties.city.description
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "The city to look up, e.g. 'Paris, France'" );
	}

	@DisplayName( "describeFunction() sets the tool description" )
	@Test
	public void testDescribeFunctionSetsDescription() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "descrTool", "" )
				             .call( () => "ok" )
				             .describeFunction( "Updated description" )
				result   = tool.getDescription()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "Updated description" );
	}

	@DisplayName( "onMissingMethod describe[Arg]() syntax works as describeArg()" )
	@Test
	public void testOnMissingMethodFluent() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "mmTool", "MM test", ( required string query ) => "result" )
				             .describeQuery( "The search query text" )
				schema = tool.getSchema()
				result     = schema.function.parameters.properties.query.description
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "The search query text" );
	}

	@DisplayName( "setSchema() overrides generated schema with a custom one" )
	@Test
	public void testSetSchemaOverridesGenerated() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				customSchema = { type: "custom", function: { name: "override" } }
				tool         = new ClosureTool( "customSchemaTool", "Custom schema", () => "x" )
				                     .setSchema( customSchema )
				schema       = tool.getSchema()
				result           = schema.function.name
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "override" );
	}

}
