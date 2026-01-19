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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

/**
 * Integration tests for Cohere embeddings provider
 * These tests require a valid COHERE_API_KEY environment variable
 *
 * Cohere is known for high-quality embeddings with excellent multilingual
 * support
 * and specialized input types for search optimization.
 *
 * @see https://docs.cohere.com/reference/embed
 * @see https://docs.cohere.com/docs/cohere-embed
 */
public class CohereTest extends BaseIntegrationTest {

	/**
	 * Rate limit delay in milliseconds
	 * Free tier: Check Cohere's rate limits
	 */
	private static final long RATE_LIMIT_DELAY_MS = 500;

	@BeforeEach
	public void beforeEach() throws InterruptedException {
		moduleRecord.settings.put( "apiKey", dotenv.get( "COHERE_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "cohere" );

		// Add delay between tests to respect rate limits
		Thread.sleep( RATE_LIMIT_DELAY_MS );
	}

	@DisplayName( "Test Cohere embedding with single text" )
	@Test
	public void testCohereEmbeddingSingle() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbed(
				input: "BoxLang is a modern dynamic JVM language",
				options: { provider: "cohere" }
			)
			println( "Cohere Embedding result type: " & result.getClass().getName() )
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
		assertThat( embeddingLength ).isAtLeast( 1024 );
	}

	@DisplayName( "Test Cohere embedding with batch texts" )
	@Test
	public void testCohereEmbeddingBatch() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Use returnFormat: "embeddings" to get array of arrays for batch
			result = aiEmbed(
				input: [
					"BoxLang is awesome",
					"AI embeddings for semantic search",
					"Vector databases and RAG"
				],
				options: {
					provider: "cohere",
					returnFormat: "embeddings"
				}
			)
			isArray = isArray( result )
			embeddingCount = result.len()
			firstEmbeddingLength = result.first().len()
			println( "Got " & embeddingCount & " embeddings, each with " & firstEmbeddingLength & " dimensions" )
			""",
			context
		);
		// @formatter:on

		var	isArray					= variables.getAsBoolean( Key.of( "isArray" ) );
		var	embeddingCount			= variables.getAsInteger( Key.of( "embeddingCount" ) );
		var	firstEmbeddingLength	= variables.getAsInteger( Key.of( "firstEmbeddingLength" ) );

		assertThat( isArray ).isTrue();
		assertThat( embeddingCount ).isEqualTo( 3 );
		assertThat( firstEmbeddingLength ).isAtLeast( 1024 );
	}

	@DisplayName( "Test Cohere embedding with specific model" )
	@Test
	public void testCohereEmbeddingWithModel() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbed(
				input: "BoxLang and AI integration",
				params: { model: "embed-english-light-v3.0" },
				options: { provider: "cohere" }
			)
			isArray = isArray( result )
			""",
			context
		);
		// @formatter:on

		var isArray = variables.getAsBoolean( Key.of( "isArray" ) );
		assertThat( isArray ).isTrue();
	}

	@DisplayName( "Test Cohere embedding with input_type parameter" )
	@Test
	public void testCohereEmbeddingInputType() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Cohere supports input_type: "search_query" or "search_document"
			// "search_query" for queries, "search_document" for documents
			result = aiEmbed(
				input: "What is BoxLang?",
				params: {
					model: "embed-english-v3.0",
					input_type: "search_query"
				},
				options: { provider: "cohere" }
			)
			""",
			context
		);
		// @formatter:on

		Array embeddings = variables.getAsArray( Key.of( "result" ) );
		assertThat( embeddings.size() ).isAtLeast( 1024 );
	}

	@DisplayName( "Test Cohere chat" )
	@Test
	public void testCohereChat() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiChat(
				"What is 2+2?",
				{},
				{ provider: "cohere" }
			)
			println( "Cohere chat result: " & result )
			isString = isSimpleValue( result )
			""",
			context
		);
		// @formatter:on

		var	isString	= variables.getAsBoolean( Key.of( "isString" ) );
		var	result		= variables.getAsString( Key.of( "result" ) );

		assertThat( isString ).isTrue();
		assertThat( result ).isNotEmpty();
		assertThat( result.toLowerCase() ).contains( "4" );
	}

	@DisplayName( "Can chat with tools" )
	@Test
	public void testCohereChatWithTools() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Define a simple calculator tool
			calculatorTool = aiTool(
				name: "calculator",
				description: "Performs basic arithmetic operations",
				callable: ( operation="add", a, b ) =>{
					switch( arguments.operation ) {
						case "add":
							return arguments.a + arguments.b;
						case "subtract":
							return arguments.a - arguments.b;
						case "multiply":
							return arguments.a * arguments.b;
						case "divide":
							return arguments.a / arguments.b;
						default:
							return "Unknown operation";
					}
				} )
				.describeOperation( "The arithmetic operation to perform: add, subtract, multiply, divide" )
				.describeA( "The first number" )
				.describeB( "The second number" )

			result = aiChat(
				"What is 15 multiplied by 7?",
				{
					tools: [ calculatorTool ]
				},
				{ provider: "cohere" }
			)

			println( "Cohere chat with tools result: " & result )
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result.toString() ).contains( "105" );
	}

	@DisplayName( "Test chat with structured output" )
	@Test
	public void testCohereChatStructuredOutput() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Use existing Person class from test/bx
			result = aiChat(
				"Tell me about a software engineer named Alice who is 32 years old.",
				{},
				{ provider: "cohere", returnFormat: new src.test.bx.Person() }
			)

			// Parse the JSON response
			firstName = result.getFirstName();
			lastName = result.getLastName();
			age = result.getAge();
			isPersonInstance = isInstanceOf( result, "Person" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "firstName" ) ).toString() ).isEqualTo( "Alice" );
		assertThat( variables.get( Key.of( "lastName" ) ).toString() ).isEqualTo( "Smith" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 32 );
		assertThat( variables.getAsBoolean( Key.of( "isPersonInstance" ) ) ).isTrue();
	}

	@DisplayName( "Test Cohere streaming chat" )
	@Test
	public void testCohereStreamingChat() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Track chunks and build full response
			chunks = [];
			fullResponse = "";

			// Use aiChatStream BIF for streaming - callback is 2nd parameter
			aiChatStream(
				"Tell me a short fact about the number 42 in one sentence.",
				( chunk ) => {
					chunks.append( chunk );
					// Cohere returns text in event_type: "text-generation" chunks
					if( chunk.keyExists( "text" ) && !chunk.text.isEmpty() ){
						fullResponse &= chunk.text;
					}
				},
				{},
				{ provider: "cohere" }
			)

			chunkCount = chunks.len();
			hasContent = fullResponse.len() > 0;

			println( "Received " & chunkCount & " chunks" );
			println( "Full response: " & fullResponse );
			""",
			context
		);
		// @formatter:on

		var	chunkCount		= variables.getAsInteger( Key.of( "chunkCount" ) );
		var	hasContent		= variables.getAsBoolean( Key.of( "hasContent" ) );
		var	fullResponse	= variables.getAsString( Key.of( "fullResponse" ) );

		assertThat( chunkCount ).isGreaterThan( 0 );
		assertThat( hasContent ).isTrue();
		assertThat( fullResponse ).isNotEmpty();
		assertThat( fullResponse.toLowerCase() ).contains( "42" );
	}

	@DisplayName( "Test Cohere streaming with simple math" )
	@Test
	public void testCohereStreamingSimpleMath() {
		// @formatter:off
		runtime.executeSource(
			"""
			fullResponse = "";
			chunkCount = 0;

			// Test with simple math question
			aiChatStream(
				"What is 5+5? Answer with just the number.",
				( chunk ) => {
					chunkCount++;
					// Cohere streaming chunks have event_type and text fields
					if( chunk.keyExists( "text" ) && !chunk.text.isEmpty() ){
						fullResponse &= chunk.text;
					}
				},
				{},
				{ provider: "cohere" }
			)

			println( "Simple math test - chunks: " & chunkCount & ", response: " & fullResponse );
			""",
			context
		);
		// @formatter:on

		var	fullResponse	= variables.getAsString( Key.of( "fullResponse" ) );
		var	chunkCount		= variables.getAsInteger( Key.of( "chunkCount" ) );

		assertThat( chunkCount ).isGreaterThan( 0 );
		assertThat( fullResponse ).isNotEmpty();
		assertThat( fullResponse.toLowerCase() ).contains( "10" );
	}

}
