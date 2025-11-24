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
package ortus.boxlang.ai.runnables;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for structured output functionality with OpenAI
 */
public class StructuredOutputTest extends BaseIntegrationTest {

	@BeforeEach
	public void setupEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
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
	@DisplayName( "Can use structured output with aiChat() using class" )
	public void testAiChatWithClass() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiChat(
				messages = "Extract the person: John Doe, age 30",
				options = { returnFormat = new src.test.bx.Person() }
			);

			firstName = result.getFirstName();
			lastName = result.getLastName();
			age = result.getAge();
			isPersonInstance = isInstanceOf( result, "Person" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "firstName" ) ).toString() ).isEqualTo( "John" );
		assertThat( variables.get( Key.of( "lastName" ) ).toString() ).isEqualTo( "Doe" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 30 );
		assertThat( variables.getAsBoolean( Key.of( "isPersonInstance" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Can use structured output with aiChat() using struct definition" )
	public void testAiChatWithStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
			structDef = {
				name: "",
				email: "",
				phone: ""
			};

			result = aiChat(
				messages = "Extract contact info: Name is Jane Smith, email jane@example.com, phone 555-1234",
				options = { returnFormat = structDef }
			);

			name = result.name;
			email = result.email;
			phone = result.phone;
			isStruct = isStruct( result );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "name" ) ).toString() ).contains( "Jane" );
		assertThat( variables.get( Key.of( "email" ) ).toString() ).contains( "jane" );
		assertThat( variables.get( Key.of( "phone" ) ).toString() ).contains( "555" );
		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Can use structured output with pipeline" )
	public void testPipelineStructuredOutput() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiModel( "openai" )
				.structuredOutput( new src.test.bx.Product() )
				.run( "Extract product: MacBook Pro 16 inch, price $2499, category Laptops" );

			name = result.getName();
			price = result.getPrice();
			category = result.getCategory();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "name" ) ).toString() ).contains( "MacBook" );
		assertThat( Double.parseDouble( variables.get( Key.of( "price" ) ).toString() ) ).isGreaterThan( 2000.0 );
		assertThat( variables.get( Key.of( "category" ) ).toString() ).contains( "Laptop" );
	}

	@Test
	@DisplayName( "Can use schema() method with JSON schema" )
	public void testSchemaMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
			jsonSchema = {
				"type": "object",
				"properties": {
					"city": { "type": "string" },
					"temperature": { "type": "number" },
					"conditions": { "type": "string" }
				},
				"required": ["city", "temperature", "conditions"]
			};

			result = aiModel( "openai" )
				.schema( jsonSchema )
				.run( "What's the weather like in San Francisco? Temperature 72F, sunny" );

			city = result.city;
			temperature = result.temperature;
			conditions = result.conditions;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "city" ) ).toString() ).contains( "Francisco" );
		assertThat( Double.parseDouble( variables.get( Key.of( "temperature" ) ).toString() ) ).isAtLeast( 70.0 );
		assertThat( variables.get( Key.of( "conditions" ) ).toString().toLowerCase() ).contains( "sunny" );
	}

	@Test
	@DisplayName( "Can use structured output with array of classes" )
	public void testArrayOfClasses() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiChat(
				messages = "Extract tasks: 1. Write report (high priority, not done) 2. Send email (low priority, completed)",
				options = {
					returnFormat = [ new src.test.bx.Task() ],
					logRequestToConsole : false,
					logResponseToConsole : false
				}
			);

			count = result.len();
			firstTaskTitle = result[1].getTitle();
			firstTaskPriority = result[1].getPriority();
			isArray = isArray( result );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "count" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "firstTaskTitle" ) ).toString() ).contains( "report" );
		assertThat( variables.get( Key.of( "firstTaskPriority" ) ).toString() ).containsMatch( "(?i)high" );
		assertThat( variables.getAsBoolean( Key.of( "isArray" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Can use aiChatRequest() with structured output" )
	public void testAiChatRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
			aiRequest = aiChatRequest(
				messages = "Extract event: Tech Conference on March 15, 2024 in Seattle"
			)
				.setStructuredOutput( new src.test.bx.Event() );

			result = aiService().invoke( aiRequest );

			eventName = result.getName();
			eventDate = result.getDate();
			eventLocation = result.getLocation();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "eventName" ) ).toString() ).contains( "Tech" );
		assertThat( variables.get( Key.of( "eventLocation" ) ).toString() ).contains( "Seattle" );
	}

	@Test
	@DisplayName( "Handles inheritance in structured output" )
	public void testInheritanceInStructuredOutput() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiChat(
				messages = "Extract employee: Alice Johnson, ID EMP001, Engineering department",
				options = {
					returnFormat : new src.test.bx.Employee(),
					logRequestToConsole : false,
					logResponseToConsole : false
				}
			);

			println( result )
			println( result.getFirstName() )
			println( result.getEmployeeId() )
			println( result.getDepartment() )

			firstName = result.getFirstName();
			employeeId = result.getEmployeeId();
			department = result.getDepartment();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "firstName" ) ).toString() ).isEqualTo( "Alice" );
		assertThat( variables.get( Key.of( "employeeId" ) ).toString() ).isEqualTo( "EMP001" );
		assertThat( variables.get( Key.of( "department" ) ).toString() ).contains( "Engineering" );
	}

	@Test
	@DisplayName( "Can use structuredOutputs() with multiple schemas" )
	public void testMultipleSchemas() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiModel( "openai" )
				.structuredOutputs([
					{ name: "contact", schema: new src.test.bx.Contact() },
					{ name: "event", schema: new src.test.bx.Event() }
				])
				.withOptions( {
					logRequestToConsole : false,
					logResponseToConsole : false
				} )
				.run( "Extract: Contact is John (john@example.com), Event is Meeting on Monday" );

			contactName = result.contact.getName();
			eventTitle = result.event.getTitle();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "contactName" ) ).toString() ).isEqualTo( "John" );
		assertThat( variables.get( Key.of( "eventTitle" ) ).toString() ).contains( "Meeting" );
	}

	@Test
	@DisplayName( "Structured output works with system messages" )
	public void testWithSystemMessage() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiModel( "openai" )
				.structuredOutput( new src.test.bx.Analysis() )
				.run([
					aiMessage().system( "You are a sentiment analysis expert" ),
					aiMessage().user( "Analyze: This product exceeded my expectations!" )
				]);

			sentiment = result.getSentiment();
			confidence = result.getConfidence();
			summary = result.getSummary();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "sentiment" ) ).toString() ).containsMatch( "(?i)positive" );
		assertThat( Double.parseDouble( variables.get( Key.of( "confidence" ) ).toString() ) ).isAtLeast( 0.5 );
		assertThat( variables.get( Key.of( "summary" ) ).toString() ).isNotEmpty();
	}

}
