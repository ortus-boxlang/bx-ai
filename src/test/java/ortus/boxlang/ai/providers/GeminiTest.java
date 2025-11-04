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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.cdimascio.dotenv.Dotenv;
import ortus.boxlang.ai.BaseIntegrationTest;

/**
 * Integration tests for Gemini AI provider
 */
public class GeminiTest extends BaseIntegrationTest {

	static Dotenv dotenv = Dotenv.load();

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
			result = aiChat( "what is boxlang?" )
			println( result )
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
			result = aiChat( { role:"user", content:"what is boxlang?" } )
			println( result )
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
			result = aiChat( [
				{ role:"developer", content:"You are a snarky assistant." },
				{ role:"user", content:"what is boxlang?" }
			])
			println( result )
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
			result = aiChat( "what is boxlang?", {
				system_instruction: {
					parts: [
					{text:'You are a cat. Respond with meows'}
					]
				}
			} )
			println( result )
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}
}