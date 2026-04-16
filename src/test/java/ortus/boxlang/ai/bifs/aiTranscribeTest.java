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
 * Integration tests for the aiTranscribe BIF.
 * Live provider tests are @Disabled by default and require API keys + a sample audio file.
 */
public class aiTranscribeTest extends BaseIntegrationTest {

	/** Absolute path to a sample MP3 file for transcription tests. */
	private static final String SAMPLE_AUDIO = "/src/test/resources/loaders/sample.mp3";

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "aiTranscribe BIF is registered in the function list" )
	@Test
	public void testAiTranscribeBIFRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
			bifExists = getFunctionList().keyExists( "aiTranscribe" )
			""",
			context
		);
		// @formatter:on

		var bifExists = variables.getAsBoolean( Key.of( "bifExists" ) );
		assertThat( bifExists ).isTrue();
	}

	@DisplayName( "aiTranscribe throws UnsupportedCapability when provider does not support transcription" )
	@Test
	public void testAiTranscribeThrowsForUnsupportedProvider() {
		// @formatter:off
		runtime.executeSource(
			"""
			threw = false;
			try {
				aiTranscribe(
					audio   : "src/test/resources/loaders/sample.mp3",
					options : { provider: "voyage" }
				);
			} catch( any e ){
				threw = true;
				errorType = e.type;
			}
			""",
			context
		);
		// @formatter:on

		var threw = variables.getAsBoolean( Key.of( "threw" ) );
		assertThat( threw ).isTrue();
	}

	@DisplayName( "aiTranscribe with OpenAI returns text string by default" )
	@Test
	public void testAiTranscribeOpenAIReturnsText() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiTranscribe( audio: "#SAMPLE_AUDIO#" )
			isText = isSimpleValue( result )
			textLen = result.len()
			""".replace( "#SAMPLE_AUDIO#", SAMPLE_AUDIO ),
			context
		);
		// @formatter:on

		var	isText	= variables.getAsBoolean( Key.of( "isText" ) );
		var	textLen	= variables.getAsInteger( Key.of( "textLen" ) );

		assertThat( isText ).isTrue();
		assertThat( textLen ).isGreaterThan( 0 );
	}

	@DisplayName( "aiTranscribe with returnFormat=response returns AiTranscriptionResponse" )
	@Test
	public void testAiTranscribeReturnsResponseObject() {
		// @formatter:off
		runtime.executeSource(
			"""
			response = aiTranscribe(
				audio  : "#SAMPLE_AUDIO#",
				options: { returnFormat: "response" }
			)
			hasText  = response.getText().len() > 0
			provider = response.getProvider()
			""".replace( "#SAMPLE_AUDIO#", SAMPLE_AUDIO ),
			context
		);
		// @formatter:on

		var	hasText		= variables.getAsBoolean( Key.of( "hasText" ) );
		var	provider	= variables.getAsString( Key.of( "provider" ) );

		assertThat( hasText ).isTrue();
		assertThat( provider ).isEqualTo( "OpenAI" );
	}

	@DisplayName( "aiTranscribe with Groq provider (whisper-large-v3)" )
	@Test
	public void testAiTranscribeGroq() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiTranscribe(
				audio  : "#SAMPLE_AUDIO#",
				options: { provider: "groq", apiKey: "#GROQ_API_KEY#" }
			)
			isText = isSimpleValue( result )
			println( result )
			""".replace( "#SAMPLE_AUDIO#", SAMPLE_AUDIO )
			 .replace( "#GROQ_API_KEY#", dotenv.get( "GROQ_API_KEY", "" ) ),
			context
		);
		// @formatter:on

		var isText = variables.getAsBoolean( Key.of( "isText" ) );
		assertThat( isText ).isTrue();
	}

}
