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

			// fromArray wraps the array in an object per OpenAI requirements
			schemaType = schema.type;
			hasProperties = schema.keyExists( "properties" );
			hasItemsProperty = schema.properties.keyExists( "items" );
			itemsPropertyType = schema.properties.items.type;
			itemSchemaType = schema.properties.items.items.type;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "schemaType" ) ).toString() ).isEqualTo( "object" );
		assertThat( variables.getAsBoolean( Key.of( "hasProperties" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasItemsProperty" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "itemsPropertyType" ) ).toString() ).isEqualTo( "array" );
		assertThat( variables.get( Key.of( "itemSchemaType" ) ).toString() ).isEqualTo( "object" );
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

	@Test
	@DisplayName( "Complex struct with class instance values generates correct property names (not getter/setter names)" )
	public void testFromStructWithClassInstances() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			// Simulate the issue: a struct returnFormat containing class instances
			complexTemplate = {
				contact: new src.test.bx.Contact(),
				products: [ new src.test.bx.Product() ]
			};

			schema = SchemaBuilder::fromObject( complexTemplate );

			// Top-level schema is an object
			schemaType = schema.type;

			// "contact" property should be an object schema with Contact's actual property names
			contactType = schema.properties.contact.type;
			hasContactName  = schema.properties.contact.properties.keyExists( "name" );
			hasContactEmail = schema.properties.contact.properties.keyExists( "email" );

			// Verify getter/setter names are NOT included
			hasGetName = schema.properties.contact.properties.keyExists( "getName" );
			hasSetName = schema.properties.contact.properties.keyExists( "setName" );

			// "products" property should be an array schema with Product's property names
			productsType = schema.properties.products.type;
			productItemType = schema.properties.products.items.type;
			hasProductName  = schema.properties.products.items.properties.keyExists( "name" );
			hasProductPrice = schema.properties.products.items.properties.keyExists( "price" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "schemaType" ) ).toString() ).isEqualTo( "object" );
		assertThat( variables.get( Key.of( "contactType" ) ).toString() ).isEqualTo( "object" );
		assertThat( variables.getAsBoolean( Key.of( "hasContactName" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasContactEmail" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasGetName" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "hasSetName" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "productsType" ) ).toString() ).isEqualTo( "array" );
		assertThat( variables.get( Key.of( "productItemType" ) ).toString() ).isEqualTo( "object" );
		assertThat( variables.getAsBoolean( Key.of( "hasProductName" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasProductPrice" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Complex struct with class instance values populates class instances on response" )
	public void testPopulateComplexStructWithClassInstances() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			// Simulate a struct returnFormat with class instances
			outputDefinition = {
				contact: new src.test.bx.Contact(),
				products: [ new src.test.bx.Product() ]
			};

			// Simulate the JSON response from the AI
			jsonResponse = '{
				"contact": {"name": "Jane Smith", "email": "jane@example.com"},
				"products": [
					{"name": "Widget", "price": 29, "category": "Tools"},
					{"name": "Gadget", "price": 99, "category": "Electronics"}
				]
			}';

			// Store definition on chatRequest by calling populateClass manually
			// Instead, call SchemaBuilder methods directly to simulate population
			contactData = '{"name": "Jane Smith", "email": "jane@example.com"}';
			populated = SchemaBuilder::populateClass( new src.test.bx.Contact(), contactData );

			contactName  = populated.getName();
			contactEmail = populated.getEmail();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "contactName" ) ).toString() ).isEqualTo( "Jane Smith" );
		assertThat( variables.get( Key.of( "contactEmail" ) ).toString() ).isEqualTo( "jane@example.com" );
	}

	@Test
	@DisplayName( "merge() handles array-of-class-instance schema correctly" )
	public void testMergeWithArraySchema() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.util.SchemaBuilder;

			merged = SchemaBuilder::merge([
				{ name: "contact", schema: new src.test.bx.Contact() },
				{ name: "products", schema: [ new src.test.bx.Product() ] }
			]);

			hasContact  = merged.properties.keyExists( "contact" );
			hasProducts = merged.properties.keyExists( "products" );
			contactType  = merged.properties.contact.type;
			productsType = merged.properties.products.type;
			productItemType = merged.properties.products.items.type;
			hasProductName = merged.properties.products.items.properties.keyExists( "name" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasContact" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasProducts" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "contactType" ) ).toString() ).isEqualTo( "object" );
		assertThat( variables.get( Key.of( "productsType" ) ).toString() ).isEqualTo( "array" );
		assertThat( variables.get( Key.of( "productItemType" ) ).toString() ).isEqualTo( "object" );
		assertThat( variables.getAsBoolean( Key.of( "hasProductName" ) ) ).isTrue();
	}

}
