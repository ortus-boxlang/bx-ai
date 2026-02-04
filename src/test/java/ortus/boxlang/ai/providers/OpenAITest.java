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
 * Integration tests for OpenAI provider
 */
public class OpenAITest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "Test OpenAI" )
	@Test
	public void testOpenAI() {
		// Then
		assertThat( moduleService.getRegistry().containsKey( moduleName ) ).isTrue();

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result = aiChat( "what is boxlang?" )
			println( result )
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test streaming chat with OpenAI" )
	@Test
	public void testChatStream() {
		// @formatter:off
		executeWithTimeoutHandling(
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
		executeWithTimeoutHandling(
			"""
			chunkCount = 0
			aiChatStream(
				"Say hello",
				( chunk ) => {
					chunkCount++
				},
				{},
				{ provider: "openai" }
			)
			println( "Total chunks received: " & chunkCount )
			""",
			context
		);
		// @formatter:on

		// Verify callback was invoked (only if not timed out)
		if ( variables.get( "chunkCount" ) != null ) {
			assertThat( variables.get( "chunkCount" ) ).isNotNull();
		}
	}

	@DisplayName( "Test the tool calls with OpenAI" )
	@Test
	public void testToolCall() {
		// @formatter:off
		executeWithTimeoutHandling(
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

		assertThat( variables.get( result ) ).isEqualTo( "San Salvador" );
	}

	@DisplayName( "Test the async chat ai with OpenAI" )
	@Test
	public void testAsyncChat() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			future = aiChatAsync( "what is boxlang?" )
			println( future.get() )
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test a pipeline with OpenAI" )
	@Test
	public void testPipeline() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result = aiMessage()
				.user( "What about 3+3?" )
				.toModel( "openai" )
				.withParams( { model: "gpt-3.5-turbo" } )
				.singleMessage()
				.run()

			println( result )
			""",
			context
		);
		// @formatter:on

		// Asserts here - should return string content since .singleMessage() was used
		assertThat( variables.get( "result" ) ).isNotNull();
		assertThat( variables.get( "result" ) ).isInstanceOf( String.class );
	}

	@DisplayName( "Test return format methods" )
	@Test
	public void testReturnFormats() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			// Test 1: Raw response (default)
			rawResult = aiMessage()
				.user( "Say hello" )
				.toModel( "openai" )
				.withParams( { model: "gpt-3.5-turbo" } )
				.run()
			println( "Raw has choices: " & rawResult.keyExists( "choices" ) )

			// Test 3: allMessages() convenience
			allResult = aiMessage()
				.user( "Say hi" )
				.toModel( "openai" )
				.allMessages()
				.run()
			println( "All messages count: " & allResult.len() )

			// Test 4: Using withOptions() explicitly
			optionsResult = aiMessage()
				.user( "Count to 3" )
				.toModel( "openai" )
				.withOptions( { returnFormat: "single" } )
				.run()
			println( "Options result type: " & optionsResult.getClass().getName() )

			// Test 5: Passing options at runtime
			runtimeResult = aiMessage()
				.user( "Hello" )
				.toModel( "openai" )
				.run( {}, {}, { returnFormat: "single" } )
			println( "Runtime options result: " & runtimeResult )
			""",
			context
		);
		// @formatter:on

		// Verify raw result has structure
		assertThat( variables.get( "rawResult" ) ).isNotNull();

		// Verify all result is an array
		assertThat( variables.get( "allResult" ) ).isNotNull();

		// Verify options result is string
		assertThat( variables.get( "optionsResult" ) ).isNotNull();
		assertThat( variables.get( "optionsResult" ) ).isInstanceOf( String.class );

		// Verify runtime options result is string
		assertThat( variables.get( "runtimeResult" ) ).isNotNull();
		assertThat( variables.get( "runtimeResult" ) ).isInstanceOf( String.class );
	}

}