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

import ortus.boxlang.ai.BaseIntegrationTest;

/**
 * Integration tests for Ollama AI provider
 *
 * Note: These tests require the test Ollama instance from docker-compose
 * The docker-compose file should be started before running tests
 */
@Disabled
public class OllamaTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		// Configure the module to use the test Ollama instance
		moduleRecord.settings.put( "apiKey", "" ); // No auth needed for local instance
		moduleRecord.settings.put( "provider", "ollama" );

		// Use the docker-compose test instance
		moduleRecord.settings.put( "chatURL", "http://localhost:11434/api/generate" );
	}

	@Test
	@DisplayName( "Should respond to basic AI chat using Ollama" )
	public void testBasicOllamaChat() {
		// Execute aiChat BIF with a simple question
		runtime.executeSource(
		    """
		       result = aiChat( "What is 2+2? Answer with just the number." )
		    println( result )
		       """,
		    context
		);

		// Verify we got a response
		var result = variables.get( "result" );
		assertThat( result ).isNotNull();
		System.out.println( "Ollama response: " + result );
	}

	@Test
	@DisplayName( "Should handle custom model parameter for Ollama" )
	public void testOllamaWithCustomModel() {
		// Test with the lightweight model
		runtime.executeSource(
		    """
		    result = aiChat(
		        messages = "Respond with 'Hello from qwen'",
		        options = {
		            "model": "qwen2.5:0.5b-instruct"
		        }
		    )
		    """,
		    context
		);

		var result = variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.toString() ).contains( "hello" );
		System.out.println( "Ollama custom model response: " + result );
	}

	@Test
	@DisplayName( "Should handle streaming chat with Ollama" )
	public void testOllamaStreamingChat() {
		// Test streaming functionality
		runtime.executeSource(
		    """
		    result = aiChatStream(
		        messages = "Count from 1 to 3, each number on a new line",
		        options = {
		            "model": "qwen2.5:0.5b-instruct"
		        }
		    )
		    """,
		    context
		);

		var result = variables.get( "result" );
		assertThat( result ).isNotNull();
		System.out.println( "Ollama streaming response: " + result );
	}
}