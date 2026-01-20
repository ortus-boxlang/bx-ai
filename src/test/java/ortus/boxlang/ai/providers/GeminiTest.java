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

/**
 * Integration tests for Gemini AI provider
 */
public class GeminiTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "GEMINI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "gemini" );
	}

	@DisplayName( "Test Gemini AI" )
	@Test
	public void testGemini() {
		// @formatter:off
		runtime.executeSource(
			"""
			try {
				result = aiChat( "what is boxlang?" )
				println( result )
			} catch( any e ) {
				// Handle 503 errors gracefully (model overloaded or no credits)
				if( e.message contains "503" || e.message contains "overloaded" || e.message contains "UNAVAILABLE" ) {
					println( "⚠️ Gemini API unavailable (503/overloaded), skipping test: " & e.message )
					testSkipped = true
				} else {
					rethrow
				}
			}
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test Gemini AI, Struct of Message" )
	@Test
	public void testGeminiStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
			try {
				result = aiChat( { role:"user", content:"what is boxlang?" } )
				println( result )
			} catch( any e ) {
				// Handle 503 errors gracefully (model overloaded or no credits)
				if( e.message contains "503" || e.message contains "overloaded" || e.message contains "UNAVAILABLE" ) {
					println( "⚠️ Gemini API unavailable (503/overloaded), skipping test: " & e.message )
					testSkipped = true
				} else {
					rethrow
				}
			}
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test Gemini AI, Array of Messages" )
	@Test
	public void testGeminiArray() {
		// @formatter:off
		runtime.executeSource(
			"""
			try {
				result = aiChat( [
					{ role:"developer", content:"You are a snarky assistant." },
					{ role:"user", content:"what is boxlang?" }
				])
				println( result )
			} catch( any e ) {
				// Handle 503 errors gracefully (model overloaded or no credits)
				if( e.message contains "503" || e.message contains "overloaded" || e.message contains "UNAVAILABLE" ) {
					println( "⚠️ Gemini API unavailable (503/overloaded), skipping test: " & e.message )
					testSkipped = true
				} else {
					rethrow
				}
			}
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test Gemini AI, System Instruction" )
	@Test
	public void testGeminiSystemInstruction() {
		// @formatter:off
		runtime.executeSource(
			"""
			try {
				result = aiChat( "what is boxlang?", {
					system_instruction: {
						parts: [
						{text:'You are a cat. Respond with meows'}
						]
					}
				} )
				println( result )
			} catch( any e ) {
				// Handle 503 errors gracefully (model overloaded or no credits)
				if( e.message contains "503" || e.message contains "overloaded" || e.message contains "UNAVAILABLE" ) {
					println( "⚠️ Gemini API unavailable (503/overloaded), skipping test: " & e.message )
					testSkipped = true
				} else {
					rethrow
				}
			}
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test streaming chat with Gemini" )
	@Test
	public void testChatStream() {
		// @formatter:off
		runtime.executeSource(
			"""
			try {
				chunks = []
				fullResponse = ""
				aiChatStream(
					"Count to 3",
					( chunk ) => {
						chunks.append( chunk )
						// Gemini format: candidates[0].content.parts[0].text
						content = chunk.candidates?.first()?.content?.parts?.first()?.text ?: ""
						fullResponse &= content
					}
				)
				println( "Received " & chunks.len() & " chunks" )
				println( "Full response: " & fullResponse )
			} catch( any e ) {
				// Handle 503 errors gracefully (model overloaded or no credits)
				if( e.message contains "503" || e.message contains "overloaded" || e.message contains "UNAVAILABLE" ) {
					println( "⚠️ Gemini API unavailable (503/overloaded), skipping test: " & e.message )
					testSkipped = true
				} else {
					rethrow
				}
			}
			""",
			context
		);
		// @formatter:on

		// Only verify if test was not skipped
		if ( variables.get( "testSkipped" ) == null ) {
			// Verify we received chunks
			assertThat( variables.get( "chunks" ) ).isNotNull();
			assertThat( variables.get( "fullResponse" ) ).isNotNull();
		}
	}

	@DisplayName( "Test streaming with callback" )
	@Test
	public void testStreamingCallback() {
		// @formatter:off
		runtime.executeSource(
			"""
			try {
				chunkCount = 0
				fullText = ""
				aiChatStream(
					"Say hello",
					( chunk ) => {
						chunkCount++
						// Extract text using Gemini format
						content = chunk.candidates?.first()?.content?.parts?.first()?.text ?: ""
						fullText &= content
					},
					{},
					{ provider: "gemini" }
				)
				println( "Total chunks received: " & chunkCount )
				println( "Full text: " & fullText )
			} catch( any e ) {
				// Handle 503 errors gracefully (model overloaded or no credits)
				if( e.message contains "503" || e.message contains "overloaded" || e.message contains "UNAVAILABLE" ) {
					println( "⚠️ Gemini API unavailable (503/overloaded), skipping test: " & e.message )
					testSkipped = true
				} else {
					rethrow
				}
			}
			""",
			context
		);
		// @formatter:on

		// Only verify if test was not skipped
		if ( variables.get( "testSkipped" ) == null ) {
			// Verify callback was invoked
			assertThat( variables.get( "chunkCount" ) ).isNotNull();
			assertThat( variables.get( "fullText" ) ).isNotNull();
		}
	}
}