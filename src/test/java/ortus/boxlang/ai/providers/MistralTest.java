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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.cdimascio.dotenv.Dotenv;
import ortus.boxlang.ai.BaseIntegrationTest;

/**
 * Integration tests for Mistral AI provider
 * These tests require a valid MISTRAL_API_KEY environment variable
 */
@Disabled( "Requires MISTRAL_API_KEY environment variable to be set in repository secrets" )
public class MistralTest extends BaseIntegrationTest {

	static Dotenv dotenv = Dotenv.load();

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "MISTRAL_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "mistral" );
	}

	@DisplayName( "Test Mistral AI" )
	@Test
	public void testMistral() {
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

	@DisplayName( "Test streaming chat with Mistral" )
	@Test
	public void testChatStream() {
		// @formatter:off
		runtime.executeSource(
			"""
			chunks = []
			fullResponse = ""
			aiChatStream(
				"Count to 3",
				( chunk ) => {
					chunks.append( chunk )
					content = chunk.choices?.first()?.delta?.content ?: ""
					fullResponse &= content
				}
			)
			println( "Received " & chunks.len() & " chunks" )
			println( "Full response: " & fullResponse )
			""",
			context
		);
		// @formatter:on

		// Verify we received chunks
		assertThat( variables.get( "chunks" ) ).isNotNull();
		assertThat( variables.get( "fullResponse" ) ).isNotNull();
	}

	@DisplayName( "Test streaming with callback" )
	@Test
	public void testStreamingCallback() {
		// @formatter:off
		runtime.executeSource(
			"""
			chunkCount = 0
			aiChatStream(
				"Say hello",
				( chunk ) => {
					chunkCount++
				},
				{},
				{ provider: "mistral" }
			)
			println( "Total chunks received: " & chunkCount )
			""",
			context
		);
		// @formatter:on

		// Verify callback was invoked
		assertThat( variables.get( "chunkCount" ) ).isNotNull();
	}

	@DisplayName( "Test tool calls with Mistral" )
	@Test
	public void testToolCall() {
		// @formatter:off
		runtime.executeSource(
			"""
			tool = aiTool(
				"get_weather",
				"Get current temperature for a given location.",
				location => {
				if( location contains "Kansas City" ) {
					return "85"
				}

				if( location contains "San Salvador" ){
					return "90"
				}

				return "unknown";
			}).describeLocation( "City and country e.g. Bogotá, Colombia" )

			result = aiChat( messages = "How hot is it in Kansas City? What about San Salvador? Answer with only the name of the warmer city, nothing else.", params = {
				tools: [ tool ],
				seed: 27
			} )
			println( result )

			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( "San Salvador" );
	}
}
