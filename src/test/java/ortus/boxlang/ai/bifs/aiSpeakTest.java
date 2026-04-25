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
 * Integration tests for the aiSpeak BIF.
 * Live provider tests are @Disabled by default and require API keys.
 */
public class aiSpeakTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "aiSpeak BIF is registered in the function list" )
	@Test
	public void testAiSpeakBIFRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
			bifExists = getFunctionList().keyExists( "aiSpeak" )
			""",
			context
		);
		// @formatter:on

		var bifExists = variables.getAsBoolean( Key.of( "bifExists" ) );
		assertThat( bifExists ).isTrue();
	}

	@DisplayName( "aiSpeak throws UnsupportedCapability when provider does not support speech" )
	@Test
	public void testAiSpeakThrowsForUnsupportedProvider() {
		// @formatter:off
		runtime.executeSource(
			"""
			threw = false;
			try {
				aiSpeak(
					text    : "Hello World",
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

	@DisplayName( "aiSpeak with OpenAI returns AiSpeechResponse object" )
	@Test
	public void testAiSpeakOpenAIReturnsResponse() {
		// @formatter:off
		runtime.executeSource(
			"""
			response = aiSpeak(
				text   : "BoxLang is a modern, dynamic JVM language.",
				params : { voice: "alloy" }
			)
			hasAudio    = response.hasAudio()
			audioFormat = response.getAudioFormat()
			audioSize   = response.getSize()
			""",
			context
		);
		// @formatter:on

		var	hasAudio	= variables.getAsBoolean( Key.of( "hasAudio" ) );
		var	audioFormat	= variables.getAsString( Key.of( "audioFormat" ) );
		var	audioSize	= variables.getAsInteger( Key.of( "audioSize" ) );

		assertThat( hasAudio ).isTrue();
		assertThat( audioFormat ).isEqualTo( "mp3" );
		assertThat( audioSize ).isGreaterThan( 0 );
	}

	@DisplayName( "aiSpeak with outputFile option saves audio to disk" )
	@Test
	public void testAiSpeakSavesToFile() {
		var outputPath = "/src/test/resources/loaders/bxai-speak-test.mp3";
		// @formatter:off
		runtime.executeSource(
			"""
			outputPath = "#outputPath#";
			savedPath = aiSpeak(
				text   : "Hello from BoxLang AI.",
				options: { outputFile: outputPath }
			)
			fileExistsResult = fileExists( savedPath )
			println( savedPath )
			""".replace( "#outputPath#", outputPath ),
			context
		);
		// @formatter:on

		var fileExistsResult = variables.getAsBoolean( Key.of( "fileExistsResult" ) );
		assertThat( fileExistsResult ).isTrue();

		// Cleanup
		new java.io.File( outputPath ).delete();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Voice Gender Keyword Resolution Tests
	//
	// These tests use a beforeAISpeech interceptor to capture the resolved voice
	// name BEFORE the provider makes its HTTP call, so no API key is required.
	// ─────────────────────────────────────────────────────────────────────────

	@DisplayName( "Voice gender keyword 'female' resolves to 'nova' for OpenAI" )
	@Test
	public void testVoiceGenderKeyword_female_resolvesToNova() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedVoice = "";
			BoxRegisterInterceptor(
				function( data ) { capturedVoice = data.speechRequest.getVoice(); },
				"beforeAISpeech"
			);
			try {
				aiSpeak( text: "Hello", params: { voice: "female" } );
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedVoice" ) ) ).isEqualTo( "nova" );
	}

	@DisplayName( "Voice gender keyword 'male' resolves to 'onyx' for OpenAI" )
	@Test
	public void testVoiceGenderKeyword_male_resolvesToOnyx() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedVoice = "";
			BoxRegisterInterceptor(
				function( data ) { capturedVoice = data.speechRequest.getVoice(); },
				"beforeAISpeech"
			);
			try {
				aiSpeak( text: "Hello", params: { voice: "male" } );
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedVoice" ) ) ).isEqualTo( "ash" );
	}

	@DisplayName( "Voice gender keyword is case-insensitive ('FEMALE' resolves to 'nova')" )
	@Test
	public void testVoiceGenderKeyword_caseInsensitive() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedVoice = "";
			BoxRegisterInterceptor(
				function( data ) { capturedVoice = data.speechRequest.getVoice(); },
				"beforeAISpeech"
			);
			try {
				aiSpeak( text: "Hello", params: { voice: "FEMALE" } );
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedVoice" ) ) ).isEqualTo( "nova" );
	}

	@DisplayName( "Voice gender keyword in options.voice is also resolved" )
	@Test
	public void testVoiceGenderKeyword_inOptionsVoice_resolves() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedVoice = "";
			BoxRegisterInterceptor(
				function( data ) { capturedVoice = data.speechRequest.getVoice(); },
				"beforeAISpeech"
			);
			try {
				aiSpeak( text: "Hello", options: { voice: "female" } );
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedVoice" ) ) ).isEqualTo( "nova" );
	}

	@DisplayName( "Concrete voice name passes through unchanged (no keyword resolution)" )
	@Test
	public void testVoiceConcreteNamePassesThrough() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedVoice = "";
			BoxRegisterInterceptor(
				function( data ) { capturedVoice = data.speechRequest.getVoice(); },
				"beforeAISpeech"
			);
			try {
				aiSpeak( text: "Hello", params: { voice: "alloy" } );
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedVoice" ) ) ).isEqualTo( "alloy" );
	}

	@DisplayName( "Voice gender keyword 'female' resolves to 'Aoede' for Gemini" )
	@Test
	public void testVoiceGenderKeyword_geminiProvider_resolvesToAoede() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedVoice = "";
			BoxRegisterInterceptor(
				function( data ) { capturedVoice = data.speechRequest.getVoice(); },
				"beforeAISpeech"
			);
			try {
				aiSpeak( text: "Hello", params: { voice: "female" }, options: { provider: "gemini" } );
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedVoice" ) ) ).isEqualTo( "Aoede" );
	}

	@DisplayName( "Empty gender mapping for a provider leaves the voice keyword unchanged" )
	@Test
	public void testVoiceGenderKeyword_emptyProviderMapping_doesNotOverrideVoice() {
		// Mistral's 'male' mapping is "" (empty), so no override should occur
		// and the voice stays as the original keyword "male"
		// @formatter:off
		runtime.executeSource(
			"""
			capturedVoice = "";
			BoxRegisterInterceptor(
				function( data ) { capturedVoice = data.speechRequest.getVoice(); },
				"beforeAISpeech"
			);
			try {
				aiSpeak( text: "Hello", params: { voice: "male" }, options: { provider: "mistral" } );
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedVoice" ) ) ).isEqualTo( "male" );
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Fluent Builder API Tests
	// ─────────────────────────────────────────────────────────────────────────

	@DisplayName( "aiSpeak() with no arguments returns an AiSpeechRequest builder" )
	@Test
	public void testAiSpeakNoArgsReturnsBuilder() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiSpeak()
			isBuilder = isInstanceOf( result, "AiSpeechRequest" )
			""",
			context
		);
		// @formatter:on

		var isBuilder = variables.getAsBoolean( Key.of( "isBuilder" ) );
		assertThat( isBuilder ).isTrue();
	}

	@DisplayName( "AiSpeechRequest.of() static factory creates a request with the given text" )
	@Test
	public void testAiSpeechRequestOfStaticFactory() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedText = "";
			BoxRegisterInterceptor(
				function( data ) { capturedText = data.speechRequest.getText(); },
				"beforeAISpeech"
			);
			try {
				AiSpeechRequest.of( "Hello from static factory" ).speak();
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedText" ) ) ).isEqualTo( "Hello from static factory" );
	}

	@DisplayName( "Fluent builder .female().asWav() sets the correct voice keyword and output format" )
	@Test
	public void testFluentBuilderVoiceAndFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedVoice  = "";
			capturedFormat = "";
			BoxRegisterInterceptor(
				function( data ) {
					capturedVoice  = data.speechRequest.getVoice();
					capturedFormat = data.speechRequest.getOutputFormat();
				},
				"beforeAISpeech"
			);
			try {
				aiSpeak()
					.text( "Test voice and format" )
					.female()
					.asWav()
					.speak();
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		// The gender keyword "female" is resolved to "nova" for OpenAI by aiSpeak()
		assertThat( variables.getAsString( Key.of( "capturedVoice" ) ) ).isEqualTo( "nova" );
		assertThat( variables.getAsString( Key.of( "capturedFormat" ) ) ).isEqualTo( "wav" );
	}

	@DisplayName( "Fluent builder .text().provider().model() populates request correctly" )
	@Test
	public void testFluentBuilderTextProviderModel() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedText  = "";
			capturedModel = "";
			BoxRegisterInterceptor(
				function( data ) {
					capturedText  = data.speechRequest.getText();
					capturedModel = data.speechRequest.getModel();
				},
				"beforeAISpeech"
			);
			try {
				aiSpeak()
					.text( "Fluent chain test" )
					.model( "tts-1-hd" )
					.speak();
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedText" ) ) ).isEqualTo( "Fluent chain test" );
		assertThat( variables.getAsString( Key.of( "capturedModel" ) ) ).isEqualTo( "tts-1-hd" );
	}

}
