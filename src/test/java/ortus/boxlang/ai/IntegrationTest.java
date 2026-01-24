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
package ortus.boxlang.ai;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * This loads the module and runs integration tests on core module functionality.
 * Provider-specific tests are in the providers package.
 */
public class IntegrationTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "Can create a core provider" )
	@Test
	public void testCoreProviders() {
		List<String> providers = List.of( "claude", "deepseek", "gemini", "grok", "mistral", "ollama", "openai", "perplexity" ); // Add more if needed

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

	@DisplayName( "On invalid provider, it throws an error" )
	@Test
	public void testInvalidProvider() {
		assertThrows(
		    Exception.class,
		    () -> runtime.executeSource(
		        """
		        provider = aiService( "invalid" )
		        """,
		        context
		    )
		);
	}

	@DisplayName( "Provides a custom provider" )
	@Test
	public void testCustomProvider() {
		// @formatter:off
		runtime.executeSource(
			"""
			boxRegisterInterceptor(
				( data ) -> {
					if( data.provider == "myCustomLLM" ) {
						data.service = {
							getName : () => "myCustomLLM"
						}
					}
				},
				"onAIProviderRequest"
			)
			provider = aiService( "myCustomLLM" )
			println( provider.getName() )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "provider" ) ).isNotNull();
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
			chatRequest = aiChatRequest()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "chatRequest" ) ).isNotNull();
	}
}
