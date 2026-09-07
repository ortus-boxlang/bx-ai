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

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * An exception thrown by the CALLER's own stream callback must propagate out of chatStream()
 * rather than being caught by the provider's chunk-parse guard and logged as a stream error
 * while the stream carries on. Mirrors BedrockServiceTest's ConverseStream cases for the
 * providers whose transport is the BoxLang HTTP client, so each one is driven by a canned
 * stream served from a loopback HttpServer instead of a live provider.
 */
public class StreamCallbackPropagationTest extends BaseIntegrationTest {

	/**
	 * Serve one canned response body on `path` from a loopback server and run `script` with
	 * "%s" replaced by the server URL.
	 */
	private void withCannedStream( String path, String contentType, String body, String script ) throws Exception {
		HttpServer server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( path, exchange -> {
			byte[] bytes = body.getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().set( "Content-Type", contentType );
			exchange.sendResponseHeaders( 200, bytes.length );
			try ( OutputStream os = exchange.getResponseBody() ) {
				os.write( bytes );
			}
		} );
		server.start();
		try {
			String url = "http://127.0.0.1:" + server.getAddress().getPort() + path;
			runtime.executeSource( script.formatted( url ), context );
		} finally {
			server.stop( 0 );
		}
	}

	@DisplayName( "BaseService.sendStreamRequest: a caller stream-callback exception propagates out of chatStream()" )
	@Test
	public void testOpenAIStreamCallbackErrorPropagates() throws Exception {
		String sse = String.join( "\n\n",
		    "data: {\"id\":\"c1\",\"model\":\"gpt-4o\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"Hello\"}}]}",
		    "data: {\"id\":\"c1\",\"model\":\"gpt-4o\",\"choices\":[],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":2,\"total_tokens\":7}}",
		    "data: [DONE]",
		    "" );

		// @formatter:off
		withCannedStream( "/v1/chat/completions", "text/event-stream", sse,
			"""
				provider = aiService( "openai", { apiKey: "dummy-key" } )
				provider.setChatURL( "%s" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "gpt-4o" },
					{ provider: "openai" }
				)

				errType = ""
				errMsg  = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {
						if ( isStruct( chunk.usage ?: "" ) ) {
							throw( type: "CallerBoom", message: "caller callback failed" )
						}
					} )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
			"""
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "caller callback failed" );
	}

	@DisplayName( "BaseService.sendStreamRequest: the caller's stream-callback failure is announced on onAIError exactly once" )
	@Test
	public void testOpenAIStreamCallbackErrorAnnouncesOnce() throws Exception {
		String sse = String.join( "\n\n",
		    "data: {\"id\":\"c1\",\"model\":\"gpt-4o\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"Hello\"}}]}",
		    "data: {\"id\":\"c1\",\"model\":\"gpt-4o\",\"choices\":[],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":2,\"total_tokens\":7}}",
		    "data: [DONE]",
		    "" );

		// @formatter:off
		withCannedStream( "/v1/chat/completions", "text/event-stream", sse,
			"""
				// The counter lives in THIS script's variables scope, so a leaked registration only
				// ever increments a scope no later test reads.
				errorEvents = 0
				BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )
				tokenEvents = 0
				totalTokens = 0
				BoxRegisterInterceptor( function( data ) { tokenEvents++; totalTokens = data.totalTokens }, "onAITokenCount" )

				provider = aiService( "openai", { apiKey: "dummy-key" } )
				provider.setChatURL( "%s" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "gpt-4o" },
					{ provider: "openai" }
				)

				errType = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {
						if ( isStruct( chunk.usage ?: "" ) ) {
							throw( type: "CallerBoom", message: "caller callback failed" )
						}
					} )
				} catch ( any e ) {
					errType = e.type
				}
			"""
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
		// The callback throws on the TERMINAL usage-only chunk: the model was already called and
		// billed by then, so the usage still has to be accounted for before the exception is let out.
		assertThat( variables.getAsInteger( Key.of( "tokenEvents" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "totalTokens" ) ) ).isEqualTo( 7 );
	}

	@DisplayName( "Mock: a caller stream-callback exception propagates, is announced, and usage is still accounted for" )
	@Test
	public void testMockStreamCallbackErrorPropagatesAndAnnounces() {
		// The mock has no HTTP client between its emission and the caller's callback, so the
		// guard/holder plumbing has to live in the mock's own transport — otherwise a callback that
		// throws mid-stream escapes with no onAIError and skips the onAITokenCount of a call the
		// provider already "billed".
		// @formatter:off
		runtime.executeSource(
		    """
		        errorEvents = 0
		        BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )
		        tokenEvents = 0
		        BoxRegisterInterceptor( function( data ) { tokenEvents++ }, "onAITokenCount" )

		        provider = aiService( "mock" )
		        provider.setResponses( [ "one two three four" ] )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "hi" ),
		            { model: "mock-model" },
		            { provider: "mock" }
		        )

		        chunks  = 0
		        errType = ""
		        errMsg  = ""
		        try {
		            provider.chatStream( chatRequest, ( chunk ) => {
		                chunks++
		                throw( type: "CallerBoom", message: "caller callback failed" )
		            } )
		        } catch ( any e ) {
		            errType = e.type
		            errMsg  = e.message
		        }
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "caller callback failed" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "tokenEvents" ) ) ).isEqualTo( 1 );
		// The stream stops at the first failed emission instead of pushing every remaining word
		// into a callback that has already blown up.
		assertThat( variables.getAsInteger( Key.of( "chunks" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "BaseService.sendStreamRequest: a malformed frame is still logged and skipped, not raised" )
	@Test
	public void testMalformedFrameIsStillSwallowed() throws Exception {
		String sse = String.join( "\n\n",
		    "data: {not json at all",
		    "data: {\"id\":\"c1\",\"model\":\"gpt-4o\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"Hello\"}}]}",
		    "data: [DONE]",
		    "" );

		// @formatter:off
		withCannedStream( "/v1/chat/completions", "text/event-stream", sse,
			"""
				provider = aiService( "openai", { apiKey: "dummy-key" } )
				provider.setChatURL( "%s" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "gpt-4o" },
					{ provider: "openai" }
				)

				text    = ""
				errType = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {
						text &= chunk.choices?.first()?.delta?.content ?: ""
					} )
				} catch ( any e ) {
					errType = e.type
				}
			"""
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEmpty();
		assertThat( variables.get( Key.of( "text" ) ).toString() ).isEqualTo( "Hello" );
	}

	@DisplayName( "Gemini: a caller stream-callback exception propagates out of chatStream()" )
	@Test
	public void testGeminiStreamCallbackErrorPropagates() throws Exception {
		String sse = String.join( "\n\n",
		    "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello\"}]}}]}",
		    "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"\"}]},\"finishReason\":\"STOP\"}],\"usageMetadata\":{\"promptTokenCount\":5,\"candidatesTokenCount\":2,\"totalTokenCount\":7}}",
		    "" );

		// @formatter:off
		withCannedStream( "/v1/streamGenerateContent", "text/event-stream", sse,
			"""
				errorEvents = 0
				BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )

				provider = aiService( "gemini", { apiKey: "dummy-key" } )
				// chatStream() swaps generateContent -> streamGenerateContent and appends ?key=&alt=sse
				provider.setChatURL( "%s".replace( "streamGenerateContent", "generateContent" ) )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "gemini-2.0-flash" },
					{ provider: "gemini" }
				)

				errType = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {
						if ( isStruct( chunk.raw?.usageMetadata ?: "" ) ) {
							throw( type: "CallerBoom", message: "caller callback failed" )
						}
					} )
				} catch ( any e ) {
					errType = e.type
				}
			"""
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "Ollama: a caller stream-callback exception propagates out of chatStream()" )
	@Test
	public void testOllamaStreamCallbackErrorPropagates() throws Exception {
		// Ollama streams NDJSON, and each HTTP chunk must be one complete JSON object - so the
		// canned body is the single final `done` frame that carries the token counts.
		String ndjson = "{\"model\":\"llama3.2\",\"message\":{\"role\":\"assistant\",\"content\":\"Hello\"},\"done\":true,\"prompt_eval_count\":5,\"eval_count\":2}\n";

		// @formatter:off
		withCannedStream( "/api/chat", "application/x-ndjson", ndjson,
			"""
				errorEvents = 0
				BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )

				provider = aiService( "ollama", {} )
				provider.setChatURL( "%s" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "llama3.2" },
					{ provider: "ollama" }
				)

				errType = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {
						if ( isStruct( chunk.usage ?: "" ) ) {
							throw( type: "CallerBoom", message: "caller callback failed" )
						}
					} )
				} catch ( any e ) {
					errType = e.type
				}
			"""
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "Gemini: a frame with no candidates (blocked prompt) is skipped, not blamed on the caller" )
	@Test
	public void testGeminiEmptyCandidatesFrameDoesNotAbortStream() throws Exception {
		// Gemini's normalizer IS the callback BaseService.sendStreamRequest emits through, so its own
		// `.first()` reads run outside the transport's parse guard. `candidates` is EMPTY on a blocked
		// prompt and `.first()` throws on an empty array - which would abort the stream and be
		// announced as the CALLER's failure. It has to be skipped like any other unusable frame.
		String sse = String.join( "\n\n",
		    "data: {\"candidates\":[],\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}",
		    "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello\"}]}}]}",
		    "data: {\"candidates\":[{\"content\":{\"parts\":[]},\"finishReason\":\"STOP\"}],\"usageMetadata\":{\"promptTokenCount\":5,\"candidatesTokenCount\":2,\"totalTokenCount\":7}}",
		    "" );

		// @formatter:off
		withCannedStream( "/v1/streamGenerateContent", "text/event-stream", sse,
			"""
				errorEvents = 0
				BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )

				provider = aiService( "gemini", { apiKey: "dummy-key" } )
				provider.setChatURL( "%s".replace( "streamGenerateContent", "generateContent" ) )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "gemini-2.0-flash" },
					{ provider: "gemini" }
				)

				text    = ""
				chunks  = 0
				errType = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {
						chunks++
						text &= chunk.choices.first().delta.content ?: ""
					} )
				} catch ( any e ) {
					errType = e.type
				}
			"""
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEmpty();
		assertThat( variables.get( Key.of( "text" ) ).toString() ).isEqualTo( "Hello" );
		// Two emitted chunks, not three - the candidate-less frame never reaches the caller.
		assertThat( variables.getAsInteger( Key.of( "chunks" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "Claude: a callback failure on the POST-stream follow-up emission propagates, announced on onAIError once" )
	@Test
	public void testClaudePostStreamEmissionAnnouncesAndPropagates() throws Exception {
		// Claude only streams the first turn; anything produced afterwards is a blocking chat() call
		// emitted as one synthetic chunk. Those emissions sit outside the transport entirely, so the
		// throw propagates on its own - but the announce is the half that used to be missing, leaving
		// a billed, failed call with ZERO onAIError.
		//
		// The stream below carries no text at all (state-only frames), which is exactly the
		// "no-text fallback" branch: chatStream() then calls chat() and emits its answer.
		String			sse			= String.join( "\n\n",
		    "data: {\"type\":\"message_start\",\"message\":{\"id\":\"msg_1\",\"model\":\"claude-sonnet-4-5\",\"usage\":{\"input_tokens\":5}}}",
		    "data: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"},\"usage\":{\"output_tokens\":2}}",
		    "data: [DONE]",
		    "" );
		String			followUp	= "{\"id\":\"msg_2\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-sonnet-4-5\","
		    + "\"content\":[{\"type\":\"text\",\"text\":\"Follow-up answer\"}],\"stop_reason\":\"end_turn\","
		    + "\"usage\":{\"input_tokens\":5,\"output_tokens\":3}}";

		AtomicInteger	calls		= new AtomicInteger();
		HttpServer		server		= HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/v1/messages", exchange -> {
			boolean	isStream	= calls.incrementAndGet() == 1;
			byte[]	body		= ( isStream ? sse : followUp ).getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().set( "Content-Type", isStream ? "text/event-stream" : "application/json" );
			exchange.sendResponseHeaders( 200, body.length );
			try ( OutputStream os = exchange.getResponseBody() ) {
				os.write( body );
			}
		} );
		server.start();

		try {
			String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/messages";
			// @formatter:off
			runtime.executeSource(
				"""
					errorEvents = 0
					BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )

					provider = aiService( "claude", { apiKey: "dummy-key" } )
					provider.setChatURL( "%s" )

					chatRequest = aiChatRequest(
						aiMessage().user( "hi" ),
						{ model: "claude-sonnet-4-5", max_tokens: 50 },
						{ provider: "claude" }
					)

					// The streamed turn emits nothing, so the ONLY chunk the caller ever sees is the
					// post-stream follow-up.
					seen    = ""
					errType = ""
					errMsg  = ""
					try {
						provider.chatStream( chatRequest, ( chunk ) => {
							seen &= chunk.choices.first().delta.content ?: ""
							throw( type: "CallerBoom", message: "caller callback failed" )
						} )
					} catch ( any e ) {
						errType = e.type
						errMsg  = e.message
					}
				""".formatted( url ),
				context
			);
			// @formatter:on

			assertThat( variables.get( Key.of( "seen" ) ).toString() ).isEqualTo( "Follow-up answer" );
			assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
			assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "caller callback failed" );
			assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
		} finally {
			server.stop( 0 );
		}
	}
}
