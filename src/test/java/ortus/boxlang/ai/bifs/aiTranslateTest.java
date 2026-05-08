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
 * Integration tests for the aiTranslate BIF.
 * Live provider tests are @Disabled by default and require API keys + a sample non-English audio file.
 */
public class aiTranslateTest extends BaseIntegrationTest {

	/** Absolute path to a sample non-English MP3 file for translation tests. */
	private static final String SAMPLE_AUDIO = "/src/test/resources/loaders/sample.mp3";

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "aiTranslate BIF is registered in the function list" )
	@Test
	public void testAiTranslateBIFRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
			bifExists = getFunctionList().keyExists( "aiTranslate" )
			""",
			context
		);
		// @formatter:on

		var bifExists = variables.getAsBoolean( Key.of( "bifExists" ) );
		assertThat( bifExists ).isTrue();
	}

	@DisplayName( "aiTranslate throws UnsupportedCapability when provider does not support translation" )
	@Test
	public void testAiTranslateThrowsForUnsupportedProvider() {
		// @formatter:off
		runtime.executeSource(
			"""
			threw = false
			errorType = ""
			try {
				aiTranslate(
					audio   : "src/test/resources/loaders/sample.mp3",
					options : { provider: "voyage" }
				);
			} catch( any e ){
				threw = true;
				errorType = e.type;
				println( "Caught error of type: #errorType#" )
			}
			""",
			context
		);
		// @formatter:on

		var	threw		= variables.getAsBoolean( Key.of( "threw" ) );
		var	errorType	= variables.getAsString( Key.of( "errorType" ) );
		assertThat( errorType ).isEqualTo( "UnsupportedCapability" );
		assertThat( threw ).isTrue();
	}

	@DisplayName( "aiTranslate with OpenAI returns English text" )
	@Test
	public void testAiTranslateOpenAIReturnsText() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiTranslate( audio: "#SAMPLE_AUDIO#" )
			isText = isSimpleValue( result )
			println( "Translation result: #result.toString()#" )
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

	@DisplayName( "aiTranslate with returnFormat=response returns AiTranscriptionResponse" )
	@Test
	public void testAiTranslateReturnsResponseObject() {
		// @formatter:off
		runtime.executeSource(
			"""
			response = aiTranslate(
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

	@DisplayName( "aiTranslate with Mistral provider" )
	@Test
	public void testAiTranslateMistral() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiTranslate(
				audio  : "#SAMPLE_AUDIO#",
				options: { provider: "mistral", apiKey: "#MISTRAL_API_KEY#" }
			 )
			 println( result )
			isText = isSimpleValue( result )
			""".replace( "#SAMPLE_AUDIO#", SAMPLE_AUDIO )
			 .replace( "#MISTRAL_API_KEY#", dotenv.get( "MISTRAL_API_KEY", "" ) ),
			context
		);
		// @formatter:on

		var isText = variables.getAsBoolean( Key.of( "isText" ) );
		assertThat( isText ).isTrue();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Fluent Builder API Tests
	// ─────────────────────────────────────────────────────────────────────────

	@DisplayName( "aiTranslate() with no arguments returns an AiTranscriptionRequest builder" )
	@Test
	public void testAiTranslateNoArgsReturnsBuilder() {
		// @formatter:off
		runtime.executeSource(
			"""
			result    = aiTranslate()
			isBuilder = isInstanceOf( result, "AiTranscriptionRequest" )
			""",
			context
		);
		// @formatter:on

		var isBuilder = variables.getAsBoolean( Key.of( "isBuilder" ) );
		assertThat( isBuilder ).isTrue();
	}

	@DisplayName( "AiTranscriptionRequest.of() builder .translate() terminator fires beforeAITranslation event" )
	@Test
	public void testFluentBuilderTranslateTerminatorFiresEvent() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.requests.AiTranscriptionRequest
			eventFired = false;
			BoxRegisterInterceptor(
				function( data ) { eventFired = true; },
				"beforeAITranslation"
			)
			AiTranscriptionRequest
				.of( "#SAMPLE_AUDIO#" )
				.translate()
			""".replace( "#SAMPLE_AUDIO#", SAMPLE_AUDIO ),
			context
		);
		// @formatter:on

		var eventFired = variables.getAsBoolean( Key.of( "eventFired" ) );
		assertThat( eventFired ).isTrue();
	}

	@DisplayName( "Fluent builder shared between transcribe() and translate() — .diarize() sets flag correctly" )
	@Test
	public void testFluentBuilderDiarizeFlag() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedDiarize = false;
			BoxRegisterInterceptor(
				function( data ) { capturedDiarize = data.transcriptionRequest.getDiarize(); },
				"beforeAITranslation"
			)
			aiTranslate()
				.file( "#SAMPLE_AUDIO#" )
				.diarize( true )
				.translate()
			""".replace( "#SAMPLE_AUDIO#", SAMPLE_AUDIO ),
			context
		);
		// @formatter:on

		var capturedDiarize = variables.getAsBoolean( Key.of( "capturedDiarize" ) );
		assertThat( capturedDiarize ).isTrue();
	}

}
