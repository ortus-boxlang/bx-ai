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
				input   : "BoxLang is a modern, dynamic JVM language.",
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
				input   : "Hello from BoxLang AI.",
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

}
