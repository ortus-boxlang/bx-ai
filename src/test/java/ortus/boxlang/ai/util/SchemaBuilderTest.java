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
package ortus.boxlang.ai.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class SchemaBuilderTest extends BaseIntegrationTest {

	@BeforeEach
	public void setupEach() {
		super.setupEach();
		// Clear schema cache before each test
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.SchemaBuilder;
		    SchemaBuilder::clearCache();
		    """,
		    context
		);
	}

	@Test
	@DisplayName( "Can build schema from BoxLang class" )
	public void testFromClass() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			person = new src.test.bx.Person();

			schema = SchemaBuilder::fromClass( person );
			hasType = schema.keyExists( "type" );
			hasProperties = schema.keyExists( "properties" );
			hasFirstName = schema.properties.keyExists( "firstName" );
			hasAge = schema.properties.keyExists( "age" );
			firstNameType = schema.properties.firstName.type;
			ageType = schema.properties.age.type;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasType" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasProperties" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasFirstName" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasAge" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "firstNameType" ) ).toString() ).isEqualTo( "string" );
		assertThat( variables.get( Key.of( "ageType" ) ).toString() ).isEqualTo( "number" );
	}

	@Test
	@DisplayName( "Can build schema from struct" )
	public void testFromStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			structDef = {
				name: "",
				age: 0,
				active: true,
				tags: []
			};

			schema = SchemaBuilder::fromStruct( structDef );
			nameType = schema.properties.name.type;
			ageType = schema.properties.age.type;
			activeType = schema.properties.active.type;
			tagsType = schema.properties.tags.type;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "nameType" ) ).toString() ).isEqualTo( "string" );
		assertThat( variables.get( Key.of( "ageType" ) ).toString() ).isEqualTo( "number" );
		assertThat( variables.get( Key.of( "activeType" ) ).toString() ).isEqualTo( "boolean" );
		assertThat( variables.get( Key.of( "tagsType" ) ).toString() ).isEqualTo( "array" );
	}

	@Test
	@DisplayName( "Can build schema from JSON schema" )
	public void testFromJSONSchema() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			jsonSchema = {
				"type": "object",
				"properties": {
					"name": { "type": "string" },
					"count": { "type": "number" }
				}
			};

			schema = SchemaBuilder::fromJSONSchema( jsonSchema );
			hasType = schema.keyExists( "type" );
			hasProperties = schema.keyExists( "properties" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasType" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasProperties" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Can build schema from array" )
	public void testFromArray() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			schema = SchemaBuilder::fromArray( new src.test.bx.Product() );
			schemaType = schema.type;
			hasItems = schema.keyExists( "items" );
			itemType = schema.items.type;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "schemaType" ) ).toString() ).isEqualTo( "array" );
		assertThat( variables.getAsBoolean( Key.of( "hasItems" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "itemType" ) ).toString() ).isEqualTo( "object" );
	}

	@Test
	@DisplayName( "Can merge multiple schemas" )
	public void testMerge() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			merged = SchemaBuilder::merge([
				{ name: "contact", schema: new src.test.bx.Contact() },
				{ name: "event", schema: new src.test.bx.Event() }
			]);

			hasContact = merged.properties.keyExists( "contact" );
			hasEvent = merged.properties.keyExists( "event" );
			contactIsObject = merged.properties.contact.type == "object";
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasContact" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasEvent" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "contactIsObject" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Can populate class from JSON" )
	public void testPopulateClass() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			jsonData = '{"firstName":"John","lastName":"Doe","age":30}';
			person = SchemaBuilder::populateClass( new src.test.bx.Person(), jsonData );

			firstName = person.getFirstName();
			lastName = person.getLastName();
			age = person.getAge();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "firstName" ) ).toString() ).isEqualTo( "John" );
		assertThat( variables.get( Key.of( "lastName" ) ).toString() ).isEqualTo( "Doe" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@Test
	@DisplayName( "Can populate struct from JSON" )
	public void testPopulateStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			jsonData = '{"name":"Test","count":5,"active":true}';
			result = SchemaBuilder::populateStruct( jsonData );

			name = result.name;
			count = result.count;
			active = result.active;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "Test" );
		assertThat( variables.get( Key.of( "count" ) ) ).isEqualTo( 5 );
		assertThat( variables.getAsBoolean( Key.of( "active" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Schema caching works" )
	public void testSchemaCaching() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			// Generate schema twice
			schema1 = SchemaBuilder::fromClass( new src.test.bx.Person() );
			schema2 = SchemaBuilder::fromClass( new src.test.bx.Person() );

			// They should be the same reference (cached)
			areSame = schema1.toString() == schema2.toString();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "areSame" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Handles inheritance in classes" )
	public void testInheritance() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			schema = SchemaBuilder::fromClass( new src.test.bx.Employee() );

			// Should have properties from both classes
			hasFirstName = schema.properties.keyExists( "firstName" );
			hasEmployeeId = schema.properties.keyExists( "employeeId" );
			propertyCount = schema.properties.keyArray().len();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasFirstName" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasEmployeeId" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "propertyCount" ) ) ).isEqualTo( 7 );
	}

	@Test
	@DisplayName( "Can populate array of class instances" )
	public void testPopulateArray() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			jsonData = '[{"name":"Laptop","price":999},{"name":"Mouse","price":29}]';
			products = SchemaBuilder::populateArray( new src.test.bx.Product(), jsonData );

			count = products.len();
			firstProductName = products[1].getName();
			secondProductPrice = products[2].getPrice();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "count" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "firstProductName" ) ).toString() ).isEqualTo( "Laptop" );
		assertThat( variables.get( Key.of( "secondProductPrice" ) ) ).isEqualTo( 29 );
	}

}
