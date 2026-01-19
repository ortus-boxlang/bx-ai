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

/**
 * Integration tests for OpenAI embeddings provider
 * These tests require a valid OPENAI_API_KEY environment variable
 */
@Disabled( "Requires OPENAI_API_KEY environment variable" )
public class OpenAIEmbeddingTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "Test OpenAI embedding with single text" )
	@Test
	public void testOpenAIEmbeddingSingle() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbedding(
				input: "BoxLang is awesome",
				options: { provider: "openai" }
			)
			println( "OpenAI Embedding result: " & result.keyList() )
			hasData = result.keyExists( "data" )
			hasModel = result.keyExists( "model" )
			hasUsage = result.keyExists( "usage" )
			embeddingLength = result.data.first().embedding.len()
			""",
			context
		);
		// @formatter:on

		var	hasData			= variables.getAsBoolean( Key.of( "hasData" ) );
		var	hasModel		= variables.getAsBoolean( Key.of( "hasModel" ) );
		var	hasUsage		= variables.getAsBoolean( Key.of( "hasUsage" ) );
		var	embeddingLength	= variables.getAsInteger( Key.of( "embeddingLength" ) );

		assertThat( hasData ).isTrue();
		assertThat( hasModel ).isTrue();
		assertThat( hasUsage ).isTrue();
		// text-embedding-3-small produces 1536-dimensional vectors
		assertThat( embeddingLength ).isGreaterThan( 0 );
	}

	@DisplayName( "Test OpenAI embedding with batch texts" )
	@Test
	public void testOpenAIEmbeddingBatch() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbedding(
				input: ["Hello", "World", "BoxLang"],
				options: { provider: "openai", returnFormat: "embeddings" }
			)
			println( "OpenAI Batch embeddings count: " & result.len() )
			embeddingCount = result.len()
			firstEmbeddingLength = result.first().len()
			""",
			context
		);
		// @formatter:on

		var	embeddingCount			= variables.getAsInteger( Key.of( "embeddingCount" ) );
		var	firstEmbeddingLength	= variables.getAsInteger( Key.of( "firstEmbeddingLength" ) );

		assertThat( embeddingCount ).isEqualTo( 3 );
		assertThat( firstEmbeddingLength ).isGreaterThan( 0 );
	}

	@DisplayName( "Test OpenAI embedding with specific model" )
	@Test
	public void testOpenAIEmbeddingWithModel() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbedding(
				input: "BoxLang is awesome",
				params: { model: "text-embedding-3-large" },
				options: { provider: "openai" }
			)
			println( "Model used: " & result.model )
			modelName = result.model
			embeddingLength = result.data.first().embedding.len()
			""",
			context
		);
		// @formatter:on

		var	modelName		= variables.getAsString( Key.of( "modelName" ) );
		var	embeddingLength	= variables.getAsInteger( Key.of( "embeddingLength" ) );

		assertThat( modelName ).contains( "text-embedding-3-large" );
		// text-embedding-3-large produces 3072-dimensional vectors
		assertThat( embeddingLength ).isGreaterThan( 1536 );
	}

	@DisplayName( "Test OpenAI embedding with returnFormat first" )
	@Test
	public void testOpenAIEmbeddingFirstFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbedding(
				input: "BoxLang",
				options: { provider: "openai", returnFormat: "first" }
			)
			isArray = isArray( result )
			embeddingLength = result.len()
			""",
			context
		);
		// @formatter:on

		var	isArray			= variables.getAsBoolean( Key.of( "isArray" ) );
		var	embeddingLength	= variables.getAsInteger( Key.of( "embeddingLength" ) );

		assertThat( isArray ).isTrue();
		assertThat( embeddingLength ).isGreaterThan( 0 );
	}
}
