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

	@DisplayName( "invoke() coerces a struct arg to a JSON string for a string-typed param" )
	@Test
	public void testInvokeCoercesStructToJsonStringForStringParam() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool(
					"jsonTool",
					"Accepts a JSON-encoded struct",
					( required string source_context ) => arguments.source_context
				)
				result = tool.invoke( { source_context: { repo: "acme/app", branch: "main" } } )
				parsed = jsonDeserialize( result )
				repoOut   = parsed.repo
				branchOut = parsed.branch
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "repoOut" ) ) ).isEqualTo( "acme/app" );
		assertThat( variables.get( Key.of( "branchOut" ) ) ).isEqualTo( "main" );
	}

	@DisplayName( "invoke() coerces an array arg to a JSON string for a string-typed param" )
	@Test
	public void testInvokeCoercesArrayToJsonStringForStringParam() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool(
					"listTool",
					"Accepts a JSON-encoded array",
					( required string files_modified ) => arguments.files_modified
				)
				result = tool.invoke( { files_modified: [ "a.bx", "b.bx" ] } )
				parsed = jsonDeserialize( result )
				firstOut  = parsed[ 1 ]
				lengthOut = parsed.len()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "firstOut" ) ) ).isEqualTo( "a.bx" );
		assertThat( variables.get( Key.of( "lengthOut" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "invoke() leaves already-stringified args unchanged for string-typed params" )
	@Test
	public void testInvokeLeavesStringArgUnchanged() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool(
					"echoTool",
					"Echoes its input",
					( required string payload ) => arguments.payload
				)
				result = tool.invoke( { payload: '{"already":"json"}' } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "{\"already\":\"json\"}" );
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
				isStrict   = schema.function.strict ?: false
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "schemaType" ) ) ).isEqualTo( "function" );
		assertThat( variables.get( Key.of( "funcName" ) ) ).isEqualTo( "greet" );
		assertThat( variables.get( Key.of( "paramType" ) ) ).isEqualTo( "object" );
		assertThat( variables.get( Key.of( "isStrict" ) ) ).isEqualTo( true );
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
	// Type-aware schema generation
	// -------------------------------------------------------------------------

	@DisplayName( "getSchema() maps numeric param to JSON Schema number type" )
	@Test
	public void testSchemaMapsNumericToNumber() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				schema = new ClosureTool( "calc", "Calculate", ( required string op, numeric factor = 1 ) => "ok" ).getSchema()
				opType     = schema.function.parameters.properties.op.type
				factorType = schema.function.parameters.properties.factor.type
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "opType" ) ) ).isEqualTo( "string" );
		assertThat( variables.get( Key.of( "factorType" ) ) ).isEqualTo( "number" );
	}

	@DisplayName( "getSchema() maps boolean param to JSON Schema boolean type" )
	@Test
	public void testSchemaMapsBooleanToBoolean() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				schema = new ClosureTool( "toggle", "Toggle", ( required boolean active ) => "ok" ).getSchema()
				result = schema.function.parameters.properties.active.type
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "boolean" );
	}

	@DisplayName( "getSchema() maps array param to JSON Schema array type with items" )
	@Test
	public void testSchemaMapsArrayToArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				schema = new ClosureTool( "list", "List", ( required array tags ) => "ok" ).getSchema()
				propType  = schema.function.parameters.properties.tags.type
				hasItems  = schema.function.parameters.properties.tags.keyExists( "items" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "propType" ) ) ).isEqualTo( "array" );
		assertThat( variables.get( Key.of( "hasItems" ) ) ).isEqualTo( true );
	}

	@DisplayName( "getSchema() maps struct param to JSON Schema object type" )
	@Test
	public void testSchemaMapsStructToObject() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				schema = new ClosureTool( "config", "Config", ( required struct options ) => "ok" ).getSchema()
				result = schema.function.parameters.properties.options.type
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "object" );
	}

	@DisplayName( "getSchema() maps integer/float/double to JSON Schema number type" )
	@Test
	public void testSchemaMapsIntegerFloatDoubleToNumber() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				schema = new ClosureTool( "multi", "Multi", ( integer a, float b, double c ) => "ok" ).getSchema()
				aType = schema.function.parameters.properties.a.type
				bType = schema.function.parameters.properties.b.type
				cType = schema.function.parameters.properties.c.type
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "aType" ) ) ).isEqualTo( "number" );
		assertThat( variables.get( Key.of( "bType" ) ) ).isEqualTo( "number" );
		assertThat( variables.get( Key.of( "cType" ) ) ).isEqualTo( "number" );
	}

	@DisplayName( "getSchema() defaults untyped params to JSON Schema string type" )
	@Test
	public void testSchemaDefaultsUntypedToString() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				schema = new ClosureTool( "untyped", "Untyped", ( required query ) => "ok" ).getSchema()
				result = schema.function.parameters.properties.query.type
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "string" );
	}

	@DisplayName( "invoke() passes native numeric value for numeric-typed param" )
	@Test
	public void testInvokePassesNativeNumeric() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "doubleIt", "Double a number", ( required numeric value ) => arguments.value * 2 )
				result = tool.invoke( { value: 5 } )
			""",
			context
		);
		// @formatter:on

		// invoke() serializes all results to string via BaseTool.serializeResult()
		assertThat( variables.get( result ) ).isEqualTo( "10" );
	}

	@DisplayName( "invoke() passes native boolean value for boolean-typed param" )
	@Test
	public void testInvokePassesNativeBoolean() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "isActive", "Check active", ( required boolean active ) => arguments.active )
				result = tool.invoke( { active: true } )
			""",
			context
		);
		// @formatter:on

		// invoke() serializes all results to string via BaseTool.serializeResult()
		assertThat( variables.get( result ) ).isEqualTo( "true" );
	}

	@DisplayName( "invoke() passes native array for array-typed param" )
	@Test
	public void testInvokePassesNativeArray() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "countTags", "Count tags", ( required array tags ) => arguments.tags.len() )
				result = tool.invoke( { tags: [ "urgent", "internal" ] } )
			""",
			context
		);
		// @formatter:on

		// invoke() serializes all results to string via BaseTool.serializeResult()
		assertThat( variables.get( result ) ).isEqualTo( "2" );
	}

	@DisplayName( "invoke() passes native struct for struct-typed param" )
	@Test
	public void testInvokePassesNativeStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.tools.ClosureTool;
				tool = new ClosureTool( "getKey", "Get key", ( required struct options ) => arguments.options.key )
				result = tool.invoke( { options: { key: "value" } } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "value" );
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
