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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for aiEmbed BIF
 */
public class aiEmbedTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "Test aiEmbed with single text (raw format)" )
	@Test
	public void testEmbeddingSingleTextRaw() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbed( "Hello World" )
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

	@DisplayName( "Test aiEmbed with single text (embeddings format)" )
	@Test
	public void testEmbedSingleTextEmbeddingsFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbed(
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

	@DisplayName( "Test aiEmbed with single text (first format)" )
	@Test
	public void testEmbedSingleTextFirstFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbed(
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

	@DisplayName( "Test aiEmbed with batch texts" )
	@Test
	public void testEmbedBatchTexts() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbed(
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

	@DisplayName( "Test aiEmbed with custom model" )
	@Test
	public void testEmbedWithCustomModel() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiEmbed(
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

	@DisplayName( "Test aiEmbed BIF is registered" )
	@Test
	public void testEmbedBIFRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
			bifExists = getFunctionList().keyExists( "aiEmbed" )
			""",
			context
		);
		// @formatter:on

		var bifExists = variables.getAsBoolean( Key.of( "bifExists" ) );
		assertThat( bifExists ).isTrue();
	}
}
