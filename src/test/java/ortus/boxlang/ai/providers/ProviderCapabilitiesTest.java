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
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

/**
 * Tests for the provider capability system introduced in v2.0.0.
 *
 * Verifies:
 * - getCapabilities() returns the correct capability strings per provider
 * - hasCapability() returns true/false for each capability correctly
 * - Chat-only providers do not report "embeddings"
 * - Embeddings-only providers do not report "chat" / "stream"
 * - UnsupportedCapability is thrown by BIFs when the provider lacks the capability
 */
@DisplayName( "Provider Capability System" )
public class ProviderCapabilitiesTest extends BaseIntegrationTest {

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private Array executeGetCapabilities( String providerName ) {
		runtime.executeSource(
		    """
		    service = new bxModules.bxai.models.providers.%sService()
		    service.configure( "test-key" )
		    caps = service.getCapabilities()
		    """.formatted( providerName ),
		    context
		);
		return variables.getAsArray( Key.of( "caps" ) );
	}

	private boolean executeHasCapability( String providerName, String capability ) {
		runtime.executeSource(
		    """
		    service = new bxModules.bxai.models.providers.%sService()
		    service.configure( "test-key" )
		    result = service.hasCapability( "%s" )
		    """.formatted( providerName, capability ),
		    context
		);
		return ( boolean ) variables.get( Key.of( "result" ) );
	}

	// -----------------------------------------------------------------------
	// Chat + Embeddings providers
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "OpenAI reports chat, stream and embeddings capabilities" )
	public void testOpenAICapabilities() {
		Array caps = executeGetCapabilities( "OpenAI" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
		assertThat( executeHasCapability( "OpenAI", "chat" ) ).isTrue();
		assertThat( executeHasCapability( "OpenAI", "stream" ) ).isTrue();
		assertThat( executeHasCapability( "OpenAI", "embeddings" ) ).isTrue();
	}

	@Test
	@DisplayName( "Groq reports only chat and stream — no embeddings" )
	public void testGroqCapabilities() {
		Array caps = executeGetCapabilities( "Groq" );
		assertThat( caps ).containsAtLeast( "chat", "stream" );
		assertWithMessage( "Groq must NOT report embeddings" )
		    .that( caps ).doesNotContain( "embeddings" );
		assertThat( executeHasCapability( "Groq", "chat" ) ).isTrue();
		assertThat( executeHasCapability( "Groq", "embeddings" ) ).isFalse();
	}

	@Test
	@DisplayName( "Grok reports only chat and stream — no embeddings" )
	public void testGrokCapabilities() {
		Array caps = executeGetCapabilities( "Grok" );
		assertThat( caps ).containsAtLeast( "chat", "stream" );
		assertWithMessage( "Grok must NOT report embeddings" )
		    .that( caps ).doesNotContain( "embeddings" );
		assertThat( executeHasCapability( "Grok", "chat" ) ).isTrue();
		assertThat( executeHasCapability( "Grok", "embeddings" ) ).isFalse();
	}

	@Test
	@DisplayName( "Mistral reports chat, stream and embeddings capabilities" )
	public void testMistralCapabilities() {
		Array caps = executeGetCapabilities( "Mistral" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
		assertThat( executeHasCapability( "Mistral", "embeddings" ) ).isTrue();
	}

	@Test
	@DisplayName( "DeepSeek reports chat, stream and embeddings capabilities" )
	public void testDeepSeekCapabilities() {
		Array caps = executeGetCapabilities( "DeepSeek" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
	}

	@Test
	@DisplayName( "Gemini reports chat, stream and embeddings capabilities" )
	public void testGeminiCapabilities() {
		Array caps = executeGetCapabilities( "Gemini" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
		assertThat( executeHasCapability( "Gemini", "chat" ) ).isTrue();
		assertThat( executeHasCapability( "Gemini", "embeddings" ) ).isTrue();
	}

	@Test
	@DisplayName( "Cohere reports chat, stream and embeddings capabilities" )
	public void testCohereCapabilities() {
		Array caps = executeGetCapabilities( "Cohere" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
	}

	@Test
	@DisplayName( "HuggingFace reports chat, stream and embeddings capabilities" )
	public void testHuggingFaceCapabilities() {
		Array caps = executeGetCapabilities( "HuggingFace" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
	}

	@Test
	@DisplayName( "Ollama reports chat, stream and embeddings capabilities" )
	public void testOllamaCapabilities() {
		Array caps = executeGetCapabilities( "Ollama" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
		assertThat( executeHasCapability( "Ollama", "chat" ) ).isTrue();
		assertThat( executeHasCapability( "Ollama", "embeddings" ) ).isTrue();
	}

	@Test
	@DisplayName( "OpenRouter reports chat, stream and embeddings capabilities" )
	public void testOpenRouterCapabilities() {
		Array caps = executeGetCapabilities( "OpenRouter" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
	}

	@Test
	@DisplayName( "MiniMax reports chat, stream and embeddings capabilities" )
	public void testMiniMaxCapabilities() {
		Array caps = executeGetCapabilities( "MiniMax" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
	}

	@Test
	@DisplayName( "OpenAICompatible reports chat, stream and embeddings capabilities" )
	public void testOpenAICompatibleCapabilities() {
		Array caps = executeGetCapabilities( "OpenAICompatible" );
		assertThat( caps ).containsAtLeast( "chat", "stream", "embeddings" );
	}

	// -----------------------------------------------------------------------
	// Chat-only providers
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "Claude reports only chat and stream — no embeddings" )
	public void testClaudeCapabilities() {
		Array caps = executeGetCapabilities( "Claude" );
		assertThat( caps ).containsAtLeast( "chat", "stream" );
		assertWithMessage( "Claude must NOT report embeddings" )
		    .that( caps ).doesNotContain( "embeddings" );
		assertThat( executeHasCapability( "Claude", "chat" ) ).isTrue();
		assertThat( executeHasCapability( "Claude", "embeddings" ) ).isFalse();
	}

	@Test
	@DisplayName( "Perplexity reports only chat and stream — no embeddings" )
	public void testPerplexityCapabilities() {
		Array caps = executeGetCapabilities( "Perplexity" );
		assertThat( caps ).containsAtLeast( "chat", "stream" );
		assertWithMessage( "Perplexity must NOT report embeddings" )
		    .that( caps ).doesNotContain( "embeddings" );
		assertThat( executeHasCapability( "Perplexity", "chat" ) ).isTrue();
		assertThat( executeHasCapability( "Perplexity", "embeddings" ) ).isFalse();
	}

	// -----------------------------------------------------------------------
	// Embeddings-only providers
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "Voyage reports only embeddings — no chat or stream" )
	public void testVoyageCapabilities() {
		Array caps = executeGetCapabilities( "Voyage" );
		assertThat( caps ).contains( "embeddings" );
		assertWithMessage( "Voyage must NOT report chat" )
		    .that( caps ).doesNotContain( "chat" );
		assertWithMessage( "Voyage must NOT report stream" )
		    .that( caps ).doesNotContain( "stream" );
		assertThat( executeHasCapability( "Voyage", "embeddings" ) ).isTrue();
		assertThat( executeHasCapability( "Voyage", "chat" ) ).isFalse();
		assertThat( executeHasCapability( "Voyage", "stream" ) ).isFalse();
	}

	// -----------------------------------------------------------------------
	// hasCapability — unknown value
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "hasCapability returns false for unknown capability string" )
	public void testHasCapabilityUnknown() {
		assertThat( executeHasCapability( "OpenAI", "unknown-capability" ) ).isFalse();
		assertThat( executeHasCapability( "Claude", "speak" ) ).isFalse();
	}

	// -----------------------------------------------------------------------
	// BIF guard — aiChat blocks embeddings-only provider
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "aiChat throws UnsupportedCapability for a chat-incapable provider (Voyage)" )
	public void testAiChatGuardBlocksVoyage() {
		boolean threw = false;
		try {
			runtime.executeSource(
			    """
			    aiChat( messages: "Hello", options: { provider: "voyage" } )
			    """,
			    context
			);
		} catch ( Exception e ) {
			threw = true;
			assertWithMessage( "Exception message should mention UnsupportedCapability" )
			    .that( e.getMessage() ).containsMatch( "(?i)unsupported" );
		}
		assertWithMessage( "aiChat should throw when provider does not support chat" )
		    .that( threw ).isTrue();
	}

	// -----------------------------------------------------------------------
	// BIF guard — aiEmbed blocks chat-only provider
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "aiEmbed throws UnsupportedCapability for a chat-only provider (Claude)" )
	public void testAiEmbedGuardBlocksClaude() {
		boolean threw = false;
		try {
			runtime.executeSource(
			    """
			    aiEmbed( input: "Hello", options: { provider: "claude" } )
			    """,
			    context
			);
		} catch ( Exception e ) {
			threw = true;
			assertWithMessage( "Exception message should mention UnsupportedCapability" )
			    .that( e.getMessage() ).containsMatch( "(?i)unsupported" );
		}
		assertWithMessage( "aiEmbed should throw when provider does not support embeddings" )
		    .that( threw ).isTrue();
	}

}
