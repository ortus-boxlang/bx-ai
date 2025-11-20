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
package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

/**
 * Integration tests for aiEmbedding BIF
 */
public class aiEmbeddingTest extends BaseIntegrationTest {

	@DisplayName( "Test aiEmbedding with single text (raw format)" )
	@Test
	public void testEmbeddingSingleTextRaw() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbedding( "Hello World" )
			println( "Embedding result keys: " & result.keyList() )
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result ).isNotNull();
		// OpenAI format should have "data", "model", "usage" keys
		assertThat( result.containsKey( "data" ) ).isTrue();
	}

	@DisplayName( "Test aiEmbedding with single text (embeddings format)" )
	@Test
	public void testEmbeddingSingleTextEmbeddingsFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbedding(
				input: "Hello World",
				options: { returnFormat: "embeddings" }
			)
			println( "Embedding vectors count: " & result.len() )
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsArray( Key.of( "result" ) );
		assertThat( result ).isNotNull();
		assertThat( result.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "Test aiEmbedding with single text (first format)" )
	@Test
	public void testEmbeddingSingleTextFirstFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbedding(
				input: "Hello World",
				options: { returnFormat: "first" }
			)
			println( "Embedding vector length: " & result.len() )
			isArray = isArray( result )
			""",
			context
		);
		// @formatter:on

		var	result	= variables.get( Key.of( "result" ) );
		var	isArray	= variables.getAsBoolean( Key.of( "isArray" ) );
		assertThat( result ).isNotNull();
		assertThat( isArray ).isTrue();
	}

	@DisplayName( "Test aiEmbedding with batch texts" )
	@Test
	public void testEmbeddingBatchTexts() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbedding(
				input: ["Hello", "World", "BoxLang"],
				options: { returnFormat: "raw" }
			)
			println( "Batch result: " & result.keyList() )
			embeddingCount = result.data.len()
			""",
			context
		);
		// @formatter:on

		var	result			= variables.getAsStruct( Key.of( "result" ) );
		var	embeddingCount	= variables.getAsInteger( Key.of( "embeddingCount" ) );
		assertThat( result ).isNotNull();
		assertThat( result.containsKey( "data" ) ).isTrue();
		assertThat( embeddingCount ).isEqualTo( 3 );
	}

	@DisplayName( "Test aiEmbedding with custom model" )
	@Test
	public void testEmbeddingWithCustomModel() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbedding(
				input: "Hello World",
				params: { model: "text-embedding-3-small" },
				options: { returnFormat: "raw" }
			)
			println( "Model used: " & result.model )
			modelName = result.model
			""",
			context
		);
		// @formatter:on

		var	result		= variables.getAsStruct( Key.of( "result" ) );
		var	modelName	= variables.getAsString( Key.of( "modelName" ) );
		assertThat( result ).isNotNull();
		assertThat( modelName ).contains( "embedding" );
	}

	@DisplayName( "Test aiEmbedding BIF is registered" )
	@Test
	public void testEmbeddingBIFRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
			bifExists = isDefined( "aiEmbedding" )
			""",
			context
		);
		// @formatter:on

		var bifExists = variables.getAsBoolean( Key.of( "bifExists" ) );
		assertThat( bifExists ).isTrue();
	}
}
