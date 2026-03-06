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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for audio BIF registration and local validation behavior.
 */
public class aiAudioTest extends BaseIntegrationTest {

	@BeforeAll
	public static void reloadModuleForAudioTests() {
		// Ensure tests use the freshly built module structure (with newly added BIFs)
		moduleService.getRegistry().remove( moduleName );
		loadModule( runtime.getRuntimeContext() );
	}

	@DisplayName( "Audio BIFs are registered" )
	@Test
	public void testAudioBIFRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
			audioToTextExists = getFunctionList().keyExists( "aiAudioToText" )
			textToAudioExists = getFunctionList().keyExists( "aiTextToAudio" )
			transcribeExists = getFunctionList().keyExists( "aiTranscribe" )
			speakExists = getFunctionList().keyExists( "aiSpeak" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "audioToTextExists" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "textToAudioExists" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "transcribeExists" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "speakExists" ) ) ).isTrue();
	}

	@DisplayName( "aiAudioToText() validates unsupported file extensions" )
	@Test
	public void testAudioToTextValidation() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorMessage = ""
			try {
				aiAudioToText( filePath: "C:/tmp/not-supported.ogg", options: { provider: "openai" } )
			} catch( any e ) {
				errorMessage = e.message
			}
			""",
			context
		);
		// @formatter:on

		var errorMessage = variables.getAsString( Key.of( "errorMessage" ) );
		assertThat( errorMessage ).contains( "Unsupported transcription file type" );
	}

}
