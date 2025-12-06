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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

/**
 * Integration tests for Voyage AI embeddings provider
 * These tests require a valid VOYAGE_API_KEY environment variable
 *
 * Voyage AI is a specialized embeddings provider with state-of-the-art models
 * for semantic search, RAG, and clustering applications.
 *
 * NOTE: Free tier has 3 RPM rate limit. Tests include delays to avoid hitting limits.
 *
 * @see https://docs.voyageai.com/docs/embeddings
 * @see https://docs.voyageai.com/docs/pricing
 */
@Disabled( "Due to rate limits and API key requirements, enable when needed" )
public class VoyageTest extends BaseIntegrationTest {

	/**
	 * Rate limit delay in milliseconds
	 * Free tier: 3 RPM = 20 seconds between requests
	 * Set to 21 seconds to be safe
	 */
	private static final long RATE_LIMIT_DELAY_MS = 21000;

	@BeforeEach
	public void beforeEach() throws InterruptedException {
		moduleRecord.settings.put( "apiKey", dotenv.get( "VOYAGE_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "voyage" );
	}

	@DisplayName( "Test Voyage embedding with single text" )
	@Test
	public void testVoyageEmbeddingSingle() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbed(
				input: "BoxLang is a modern dynamic JVM language",
				options: { provider: "voyage" }
			)
			println( result )
			""",
			context
		);
		// @formatter:on

		Array embeddings = variables.getAsArray( Key.of( "result" ) );
		assertThat( embeddings.size() ).isEqualTo( 1024 );
	}

	@DisplayName( "Test Voyage embedding with batch texts" )
	@Test
	public void testVoyageEmbeddingBatch() {
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
					provider: "voyage",
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

	@DisplayName( "Test Voyage embedding with specific model" )
	@Test
	public void testVoyageEmbeddingWithModel() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbed(
				input: "BoxLang and AI integration",
				params: { model: "voyage-3-lite" },
				options: { provider: "voyage" }
			)
			isArray = isArray( result )
			embeddingLength = result.len()
			println( "voyage-3-lite embedding dimensions: " & embeddingLength )
			""",
			context
		);
		// @formatter:on

		var	isArray			= variables.getAsBoolean( Key.of( "isArray" ) );
		var	embeddingLength	= variables.getAsInteger( Key.of( "embeddingLength" ) );

		assertThat( isArray ).isTrue();
		// voyage-3-lite produces 512-dimensional vectors
		assertThat( embeddingLength ).isEqualTo( 512 );
	}

	@DisplayName( "Test Voyage embedding default returnFormat (first)" )
	@Test
	public void testVoyageEmbeddingFirstFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Default returnFormat is "first" - returns single embedding array
			result = aiEmbed(
				input: "BoxLang embeddings",
				options: { provider: "voyage" }
			)
			isArray = isArray( result )
			embeddingLength = result.len()
			println( "Default (first) format returns array with " & embeddingLength & " dimensions" )
			""",
			context
		);
		// @formatter:on

		var	isArray			= variables.getAsBoolean( Key.of( "isArray" ) );
		var	embeddingLength	= variables.getAsInteger( Key.of( "embeddingLength" ) );

		assertThat( isArray ).isTrue();
		assertThat( embeddingLength ).isEqualTo( 1024 );
	}

	@DisplayName( "Test Voyage embedding with input type parameter" )
	@Test
	public void testVoyageEmbeddingInputType() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Voyage supports input_type: "query" or "document"
			// "query" for search queries, "document" for documents being searched
			result = aiEmbed(
				input: "What is BoxLang?",
				params: {
					model: "voyage-3",
					input_type: "query"
				},
				options: { provider: "voyage"  }
			)
			""",
			context
		);
		// @formatter:on

		Array embeddings = variables.getAsArray( Key.of( "result" ) );
		assertThat( embeddings.size() ).isEqualTo( 1024 );
	}

	@DisplayName( "Test that Voyage chat throws error" )
	@Test
	public void testVoyageChatThrowsError() {
		// @formatter:off
		runtime.executeSource(
			"""
			try {
				result = aiChat(
					"Hello",
					{},
					{ provider: "voyage" }
				)
				errorThrown = false
			} catch( any e ) {
				errorThrown = true
				errorType = e.type
				errorMessage = e.message
				println( "Expected error: " & e.message )
			}
			""",
			context
		);
		// @formatter:on

		var	errorThrown		= variables.getAsBoolean( Key.of( "errorThrown" ) );
		var	errorType		= variables.getAsString( Key.of( "errorType" ) );
		var	errorMessage	= variables.getAsString( Key.of( "errorMessage" ) );

		assertThat( errorThrown ).isTrue();
		assertThat( errorType ).isEqualTo( "UnsupportedOperation" );
		assertThat( errorMessage ).contains( "only supports embeddings" );
	}
}
