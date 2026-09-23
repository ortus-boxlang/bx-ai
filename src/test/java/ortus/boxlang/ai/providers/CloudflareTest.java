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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

/**
 * Integration tests for the Cloudflare Workers AI provider
 * Requires CLOUDFLARE_API_KEY and CLOUDFLARE_ACCOUNT_ID
 */
public class CloudflareTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "CLOUDFLARE_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "cloudflare" );
	}

	@DisplayName( "Test Cloudflare chat" )
	@Test
	public void testChat() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result = aiChat( "Reply with only the word: hello", {}, { accountId: "#ACCOUNT_ID#" } )
			println( result )
			""".replace( "#ACCOUNT_ID#", dotenv.get( "CLOUDFLARE_ACCOUNT_ID", "" ) ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).isNotEmpty();
	}

	@DisplayName( "Test Cloudflare streaming chat" )
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
					fullResponse &= chunk.choices?.first()?.delta?.content ?: ""
				},
				{},
				{ accountId: "#ACCOUNT_ID#" }
			)
			println( "Full response: " & fullResponse )
			""".replace( "#ACCOUNT_ID#", dotenv.get( "CLOUDFLARE_ACCOUNT_ID", "" ) ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsArray( Key.of( "chunks" ) ).size() ).isGreaterThan( 0 );
		assertThat( variables.getAsString( Key.of( "fullResponse" ) ) ).isNotEmpty();
	}

	@DisplayName( "Test Cloudflare embeddings" )
	@Test
	public void testEmbeddings() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result = aiEmbed( "BoxLang is a modern JVM language", {}, { provider: "cloudflare", accountId: "#ACCOUNT_ID#" } )
			""".replace( "#ACCOUNT_ID#", dotenv.get( "CLOUDFLARE_ACCOUNT_ID", "" ) ),
			context
		);
		// @formatter:on

		assertThat( ( ( Array ) variables.get( result ) ).size() ).isGreaterThan( 0 );
	}

	@DisplayName( "Test Cloudflare tool calling" )
	@Test
	public void testToolCalling() {
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
				}
			).describeLocation( "City and country e.g. Bogota, Colombia" )

			result = aiChat(
				"How hot is it in Kansas City? What about San Salvador? Answer with only the name of the warmer city, nothing else.",
				{ tools: [ tool ] },
				{ accountId: "#ACCOUNT_ID#" }
			)
			println( result )
			""".replace( "#ACCOUNT_ID#", dotenv.get( "CLOUDFLARE_ACCOUNT_ID", "" ) ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( result ) ).contains( "San Salvador" );
	}

	@DisplayName( "Cloudflare throws a clear error when no account ID is configured" )
	@Test
	public void testMissingAccountId() {
		// @formatter:off
		Throwable error = assertThrows( Throwable.class, () -> runtime.executeSource(
			"""
			service = aiService( "cloudflare", { apiKey: "test-key" } )
			service.setAccountId( "" )
			service.chat( aiChatRequest( "hello" ) )
			""",
			context
		) );
		// @formatter:on

		assertThat( error.getMessage() ).contains( "CLOUDFLARE_ACCOUNT_ID" );
	}

	@DisplayName( "Cloudflare builds its URLs from the account ID" )
	@Test
	public void testUrlsFromAccountId() {
		// @formatter:off
		runtime.executeSource(
			"""
			service = aiService( "cloudflare", { apiKey: "test-key", accountId: "abc123" } )
			chatURL = service.getChatURL()
			embeddingsURL = service.getEmbeddingsURL()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "chatURL" ) ) )
		    .isEqualTo( "https://api.cloudflare.com/client/v4/accounts/abc123/ai/v1/chat/completions" );
		assertThat( variables.getAsString( Key.of( "embeddingsURL" ) ) )
		    .isEqualTo( "https://api.cloudflare.com/client/v4/accounts/abc123/ai/v1/embeddings" );
	}
}
