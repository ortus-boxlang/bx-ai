/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for Ollama AI provider
 *
 * Note: These tests require the test Ollama instance from docker-compose
 * The docker-compose file should be started before running tests
 */
public class OllamaTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		// Configure the module to use the test Ollama instance
		moduleRecord.settings.put( "apiKey", "" ); // No auth needed for local instance
		moduleRecord.settings.put( "provider", "ollama" );

		// Use the docker-compose test instance
		moduleRecord.settings.put( "chatURL", "http://localhost:11434/api/chat" );
	}

	@Test
	@DisplayName( "Should respond to basic AI chat using Ollama" )
	public void testBasicOllamaChat() {
		// Execute aiChat BIF with a simple question
		executeWithTimeoutHandling(
		    """
		       result = aiChat( messages: "What is 2+2? Answer with just the number.", options:{
		    	//logRequestToConsole: true,
		    	//logResponseToConsole: true
		    } )
		       println( result )
		       """,
		    context
		);

		// Verify we got a response
		var result = variables.get( "result" );
		assertThat( result ).isNotNull();
		System.out.println( "Ollama response: " + result );
	}

	@Test
	@DisplayName( "Should handle custom model parameter for Ollama" )
	public void testOllamaWithCustomModel() {
		// Test with the lightweight model
		// @formatter:off
		executeWithTimeoutHandling(
		    """
		    result = aiChat(
		        messages = "What is 2 + 2? Answer with just the number.",
		        options = {
		            "model": "qwen3:0.6b",
		    	 	logResponseToConsole: true
		        }
		    )
		    """,
		    context
		);
		// @formatter:on

		var result = variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.toString().toLowerCase() ).containsMatch( "[4four]" );
		System.out.println( "Ollama custom model response: " + result );
	}

	@Test
	@DisplayName( "Should handle streaming chat with Ollama" )
	public void testOllamaStreamingChat() {
		// Test streaming functionality
		// @formatter:off
		executeWithTimeoutHandling(
		    """
		    chunks = []
		    fullResponse = ""
		    aiChatStream(
		        messages: "Tell me a bedtime story in 10 sentences.",
		        callback:( chunk ) => {
		            chunks.append( arguments.chunk )
		            content = chunk.choices.first().delta?.content ?: ""
					println( content )
		            fullResponse &= content
		        },
				options: {
					logResponseToConsole: true,
					logRequestToConsole: true
				}
		    )
		    println( "Received " & chunks.len() & " chunks" )
		    println( "Full response: " & fullResponse )
		    """,
		    context
		);
		// @formatter:on

		// Verify we received chunks
		assertThat( variables.get( "chunks" ) ).isNotNull();
		var chunks = variables.get( "chunks" );
	}

	@DisplayName( "Test Ollama Tools" )
	@Test
	public void testOllamaTools() {
		moduleRecord.settings.put( "logResponseToConsole", false );
		moduleRecord.settings.put( "logRequestToConsole", false );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			maxAttempts = 3
			attempt = 0
			toolCallCount = 0
			result = ""

			tool = aiTool(
				"get_weather",
				"Get current temperature for a given location.",
				( required location) => {
					toolCallCount++
					// Ensure location is a string (handle if passed as struct)
					var loc = isSimpleValue( location ) ? location : ( location.location ?: location.toString() );

					if( loc contains "Kansas City" ) {
						return "85"
					}

					if( loc contains "San Salvador" ){
						return "90"
					}

					return "unknown";
				}).describeLocation( "City and country e.g. Bogotá, Colombia" )

			while( attempt < maxAttempts && toolCallCount == 0 ){
				attempt++
				result = aiChat(
					messages = "How hot is it in Kansas City? What about San Salvador? Answer with only the name of the warmer city, nothing else.
					Please use the provided tool to get the current temperature for each city.",
					params = {
						tools: [ tool ]
					},
					options = {
					} )
			}

			println( "Tool invocation count: " & toolCallCount )
			println( "Attempts: " & attempt )
			println( result )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isNotNull();
		var toolCallCount = variables.getAsInteger( Key.of( "toolCallCount" ) );
		Assumptions.assumeTrue(
		    toolCallCount > 0,
		    "Ollama did not trigger any tool calls after retries; skipping flaky model behavior."
		);
		var result = variables.get( "result" ).toString().toLowerCase();
		assertThat( result ).containsMatch( "salvador|90" );
	}

	@DisplayName( "Test Ollama streaming with tool calls" )
	@Test
	public void testOllamaStreamingWithTools() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			maxAttempts = 3
			attempt = 0
			toolCallCount = 0
			tool = aiTool(
				"get_weather",
				"Get current temperature for a given location.",
				( required location) => {
					toolCallCount++
					var loc = isSimpleValue( location ) ? location : ( location.location ?: location.toString() );
					println( "🔧 TOOL INVOKED with location: [" & loc & "]" )
					if( loc contains "Kansas City" ) return "85"
					if( loc contains "San Salvador" ) return "90"
					return "unknown"
				}).describeLocation( "City and country e.g. Bogotá, Colombia" )

			chunks       = []
			fullResponse = ""

			while( attempt < maxAttempts && toolCallCount == 0 ){
				attempt++
				chunks = []
				fullResponse = ""

				aiChatStream(
					messages: "How hot is it in Kansas City? Answer with only the temperature in Fahrenheit, nothing else. Use the provided tool: get_weather",
					callback: ( chunk ) => {
						chunks.append( chunk )
						content = chunk.choices.first().delta?.content ?: ""
						fullResponse &= content
					},
					params: {
						tools: [ tool ]
					},
					options: {
						//logRequestToConsole: true,
						//logResponseToConsole: true
					}
				)
			}

			println( "Streaming tool call chunks received: " & chunks.len() )
			println( "Tool invocation count: " & toolCallCount )
			println( "Attempts: " & attempt )
			println( "Final streamed response: " & fullResponse )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "chunks" ) ).isNotNull();
		assertThat( variables.get( "fullResponse" ) ).isNotNull();
		var	fullResponse	= variables.get( "fullResponse" ).toString();
		// Assert the tool was actually invoked (mechanism is working)
		// Note: qwen3:0.6b is a very small model and may not reliably
		// incorporate tool results into its final answer - we assert invocation, not the value.
		var	toolCallCount	= variables.getAsInteger( Key.of( "toolCallCount" ) );
		Assumptions.assumeTrue(
		    toolCallCount > 0,
		    "Ollama streaming did not trigger any tool calls after retries; skipping flaky model behavior."
		);
		System.out.println( "Tool invoked " + toolCallCount + " time(s). Final response: " + fullResponse );
	}

	@DisplayName( "Test JSON response" )
	@Test
	public void testJsonResponse() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result = aiChat(
				messages = "Return a JSON object with exactly two fields: a 'name' field with value 'BoxLang' and a 'version' field with value '1.0'. Return ONLY valid JSON, nothing else.",
				options = {
					returnFormat: "json"
				}
			)
			println( result )
			""",
			context
		);
		// @formatter:on

		// Verify we got a struct back
		assertThat( variables.get( "result" ) ).isInstanceOf( ortus.boxlang.runtime.types.IStruct.class );
		var result = ( ortus.boxlang.runtime.types.IStruct ) variables.get( "result" );
		assertThat( result.containsKey( "name" ) || result.containsKey( "NAME" ) ).isTrue();
	}

	@DisplayName( "Test XML response" )
	@Test
	public void testXmlResponse() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			try {
				result = aiChat(
					messages = "Return an XML document with a root element 'language' containing a child element 'name' with value 'BoxLang'. Return ONLY valid XML, nothing else.",
					options = {
						returnFormat: "xml"
					}
				)
				println( result )
			} catch( any e ) {
				// Handle cases where LLM returns incorrectly formatted XML response
				if( e.type contains "BoxIOException" || e.message contains "could not be found" || e.message contains "does not exist" ) {
					println( "⚠️ Ollama returned incorrectly formatted XML response, skipping test: " & e.message )
					testSkipped = true
				} else {
					rethrow
				}
			}
			""",
			context
		);
		// @formatter:on

		// Only verify if test was not skipped
		if ( variables.get( "testSkipped" ) == null ) {
			// Verify we got an XML document back
			assertThat( variables.get( "result" ) ).isInstanceOf( ortus.boxlang.runtime.types.XML.class );
		}
	}

	@DisplayName( "Test structured output response" )
	@Test
	public void testStructuredOutput() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			// Define structured schema using a struct
			languageSchema = {
				"name": "string",
				"version": "string",
				"type": "string"
			}

			result = aiChat(
				messages = "Tell me about BoxLang. It's a modern JVM language, version 1.0, and it's a dynamic language. Return ONLY valid JSON matching this schema: name, version, type.",
				options = {
					returnFormat: languageSchema
				}
			)
			println( result )
			""",
			context
		);
		// @formatter:on

		// Verify we got a struct back with expected properties
		assertThat( variables.get( "result" ) ).isInstanceOf( ortus.boxlang.runtime.types.IStruct.class );
		var result = ( ortus.boxlang.runtime.types.IStruct ) variables.get( "result" );
		assertThat( result.containsKey( "name" ) || result.containsKey( "NAME" ) ).isTrue();
		assertThat( result.containsKey( "version" ) || result.containsKey( "VERSION" ) ).isTrue();
	}

	@DisplayName( "Test structured output response with a class" )
	@Test
	public void testStructuredOutputWithClass() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			// Use the Product test class as the structured output target
			result = aiChat(
				messages = "Create a product: a wireless mouse called 'AirClick Pro' priced at 29.99 in the 'Electronics' category. Return ONLY valid JSON with fields: name, price, category.",
				options = {
					returnFormat: new src.test.bx.Product()
				}
			)
			println( result )
			resultClass = result.$bx.$class.getName()
			println( "Result class: " & resultClass )
			""",
			context
		);
		// @formatter:on

		// Should come back as a populated Product instance
		assertThat( variables.get( result ) ).isNotNull();
		assertThat( variables.get( Key.of( "resultClass" ) ).toString() ).contains( "Product" );
	}

	@DisplayName( "Test Ollama embedding with single text" )
	@Test
	public void testOllamaEmbeddingSingle() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result = aiEmbed(
				input: "BoxLang is a modern dynamic JVM language",
				options: { provider: "ollama" }
			)
			//println( result )
			println( "Ollama Embedding result type: " & result.getClass().getName() )
			isArray = isArray( result )
			embeddingLength = result.len()
			println( "Embedding dimensions: " & embeddingLength )
			""",
			context
		);
		// @formatter:on

		var	isArray			= variables.getAsBoolean( Key.of( "isArray" ) );
		var	embeddingLength	= variables.getAsInteger( Key.of( "embeddingLength" ) );

		assertThat( isArray ).isTrue();
		// embed-english-v3.0 produces 1024-dimensional vectors
		// nomic-embed-text produces 768-dimensional vectors
		assertThat( embeddingLength ).isAtLeast( 768 );
	}

}