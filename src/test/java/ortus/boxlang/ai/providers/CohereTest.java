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
 * Cohere is known for high-quality embeddings with excellent multilingual support
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
		assertThat( embeddingLength ).isEqualTo( 1024 );
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
		assertThat( firstEmbeddingLength ).isEqualTo( 1024 );
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
		assertThat( embeddings.size() ).isEqualTo( 1024 );
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

}
