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
package ortus.boxlang.moduleslug;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * This loads the module and runs an integration test on the module.
 */
public class IntegrationTest extends BaseIntegrationTest {

	static Dotenv dotenv = Dotenv.load();

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "Can create a provider" )
	@Test
	public void testProviders() {
		List<String> providers = List.of( "deepseek", "gemini", "grok", "openai" ); // Add more if needed

		for ( String provider : providers ) {
			// Execute the runtime source with the current provider
			runtime.executeSource(
			    String.format(
			        """
			        provider = aiService( "%s" )
			        """, provider
			    ),
			    context
			);

			// Assert that the provider is not null
			assertThat( variables.get( "provider" ) ).isNotNull();
		}
	}

	@DisplayName( "Test Grok AI" )
	@Test
	public void testGrok() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "GROK_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "grok" );

		// @formatter:off
		runtime.executeSource(
			"""
			result = aiChat( "what is boxlang?" )
			println( result )
			""",
			context
		);
		// @formatter:on
	}

	@DisplayName( "Test Gemini AI" )
	@Test
	public void testGemini() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "GEMINI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "gemini" );

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
		moduleRecord.settings.put( "apiKey", dotenv.get( "GEMINI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "gemini" );

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
		moduleRecord.settings.put( "apiKey", dotenv.get( "GEMINI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "gemini" );

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

	@DisplayName( "Test the deepseek ai" )
	@Test
	public void testDeepSeek() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "DEEPSEEK_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "deepseek" );

		// @formatter:off
		runtime.executeSource(
			"""
			result = aiChat( "what is a servlet?" )
			println( result )
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test OpenAI" )
	@Test
	public void testOpenAI() {
		// Then
		assertThat( moduleService.getRegistry().containsKey( moduleName ) ).isTrue();

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

	@DisplayName( "Test the async chat ai" )
	@Test
	public void testAsyncChat() {
		// @formatter:off
		runtime.executeSource(
			"""
			future = aiChatAsync( "what is boxlang?" )
			println( future.get() )
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test the tool calls" )
	@Test
	public void testToolCall() {
		// @formatter:off
		runtime.executeSource(
			"""
			tool = new bxmodules.bxai.models.Tool();

			tool.setName( "get_weather" )
				.describe( "Get current temperature for a given location." )
				.describeLocation( "City and country e.g. Bogotá, Colombia" )
				.setFunc( ( location ) => {
					if( location contains "Kansas City" ) {
						return "85"
					}

					if( location contains "San Salvador" ){
						return "90"
					}

					return "unknown";
				});

			result = aiChat( messages = "How hot is it in Kansas City? What about San Salvador? Answer with only the name of the warmer city, nothing else.", data = {
				tools: [ tool ]
			} )
			println( result )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "San Salvador" );
	}

	@DisplayName( "It can create ai message objects" )
	@Test
	public void testAiMessage() {
		// @formatter:off
		runtime.executeSource(
			"""
			message = aiMessage()
			assert message.count() == 0

			message = aiMessage( "hello" )
			println( message.getMessages() )
			assert message.count() == 1
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
	}

	@DisplayName( "It can create an ai chat request" )
	@Test
	public void testAiChatRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
			aiRequest = aiChatRequest()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "aiRequest" ) ).isNotNull();
	}
}
