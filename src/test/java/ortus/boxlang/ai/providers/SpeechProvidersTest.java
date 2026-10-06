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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Speech tests for the OpenAI-family providers: OpenAI, Mistral, Gemini and Grok.
 * <p>
 * Offline tests run against a local stub through the `baseURL` option and pin the request each provider sends and
 * how its response is parsed. Live tests need the provider API key (OPENAI_API_KEY, MISTRAL_API_KEY, GEMINI_API_KEY,
 * GROK_API_KEY) and are skipped without one.
 */
public class SpeechProvidersTest extends BaseIntegrationTest {

	private static final String			OPENAI_KEY	= dotenv.get( "OPENAI_API_KEY", "" );
	private static final String			MISTRAL_KEY	= dotenv.get( "MISTRAL_API_KEY", "" );
	private static final String			GEMINI_KEY	= dotenv.get( "GEMINI_API_KEY", "" );
	private static final String			GROK_KEY	= dotenv.get( "GROK_API_KEY", "" );

	private static HttpServer			stub;
	private static String				stubURL;
	private static Map<String, String>	seenBody	= new ConcurrentHashMap<>();
	private static Map<String, String>	seenHeader	= new ConcurrentHashMap<>();

	private static String b64( String text ) {
		return Base64.getEncoder().encodeToString( text.getBytes( StandardCharsets.UTF_8 ) );
	}

	private static void record( HttpExchange exchange, String key ) throws IOException {
		seenBody.put( key, new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 ) );
		seenHeader.put( key + ".auth", String.valueOf( exchange.getRequestHeaders().getFirst( "Authorization" ) ) );
		seenHeader.put( key + ".google", String.valueOf( exchange.getRequestHeaders().getFirst( "x-goog-api-key" ) ) );
		seenHeader.put( key + ".uri", exchange.getRequestURI().toString() );
	}

	private static void respond( HttpExchange exchange, String contentType, byte[] body ) throws IOException {
		exchange.getResponseHeaders().add( "Content-Type", contentType );
		exchange.sendResponseHeaders( 200, body.length );
		try ( OutputStream out = exchange.getResponseBody() ) {
			out.write( body );
		}
	}

	@BeforeAll
	public static void startStub() throws IOException {
		stub = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );

		// OpenAI: chunked raw audio in three flushed pieces
		stub.createContext( "/openai/audio/speech", exchange -> {
			record( exchange, "openai" );
			exchange.getResponseHeaders().add( "Content-Type", "audio/pcm" );
			exchange.sendResponseHeaders( 200, 0 );
			try ( OutputStream out = exchange.getResponseBody() ) {
				for ( String piece : new String[] { "AAAA", "BBBB", "CCCC" } ) {
					out.write( piece.getBytes( StandardCharsets.UTF_8 ) );
					out.flush();
				}
			}
		} );

		// Mistral: JSON for whole file, SSE when stream:true
		stub.createContext( "/mistral/audio/speech", exchange -> {
			record( exchange, "mistral" );
			if ( seenBody.get( "mistral" ).contains( "\"stream\":true" ) ) {
				String events = "event: speech.audio.delta\ndata: {\"type\":\"speech.audio.delta\",\"audio_data\":\"" + b64( "1234" ) + "\"}\n\n"
				    + "event: speech.audio.delta\ndata: {\"type\":\"speech.audio.delta\",\"audio_data\":\"" + b64( "56" ) + "\"}\n\n"
				    + "event: speech.audio.done\ndata: {\"type\":\"speech.audio.done\",\"usage\":{\"characters\":5}}\n\n";
				exchange.getResponseHeaders().add( "Content-Type", "text/event-stream" );
				exchange.sendResponseHeaders( 200, 0 );
				try ( OutputStream out = exchange.getResponseBody() ) {
					out.write( events.getBytes( StandardCharsets.UTF_8 ) );
				}
			} else {
				respond( exchange, "application/json", ( "{\"audio_data\":\"" + b64( "MP3DATA" ) + "\"}" ).getBytes( StandardCharsets.UTF_8 ) );
			}
		} );

		// Gemini: unary generateContent returns headerless L16 PCM, Interactions streams step.delta audio events
		stub.createContext( "/gemini/models", exchange -> {
			record( exchange, "gemini" );
			String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"inlineData\":{\"mimeType\":\"audio/L16;codec=pcm;rate=24000\",\"data\":\""
			    + b64( "PCMPCM" )
			    + "\"}}]}}]}";
			respond( exchange, "application/json", json.getBytes( StandardCharsets.UTF_8 ) );
		} );
		stub.createContext( "/gemini/interactions", exchange -> {
			record( exchange, "geminiStream" );
			String events = "event: step.start\ndata: {\"event_type\":\"step.start\"}\n\n"
			    + "event: step.delta\ndata: {\"event_type\":\"step.delta\",\"delta\":{\"type\":\"audio\",\"data\":\"" + b64( "1234" ) + "\"}}\n\n"
			    + "event: step.delta\ndata: {\"event_type\":\"step.delta\",\"delta\":{\"type\":\"audio\",\"data\":\"" + b64( "56" ) + "\"}}\n\n"
			    + "event: step.stop\ndata: {\"event_type\":\"step.stop\"}\n\n";
			exchange.getResponseHeaders().add( "Content-Type", "text/event-stream" );
			exchange.sendResponseHeaders( 200, 0 );
			try ( OutputStream out = exchange.getResponseBody() ) {
				out.write( events.getBytes( StandardCharsets.UTF_8 ) );
			}
		} );

		// Grok: raw audio bytes
		stub.createContext( "/grok/tts", exchange -> {
			record( exchange, "grok" );
			respond( exchange, "audio/mpeg", "GROKAUDIO".getBytes( StandardCharsets.UTF_8 ) );
		} );

		stub.start();
		stubURL = "http://127.0.0.1:" + stub.getAddress().getPort();
	}

	@AfterAll
	public static void stopStub() {
		if ( stub != null ) {
			stub.stop( 0 );
		}
	}

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", OPENAI_KEY );
		moduleRecord.settings.put( "provider", "openai" );
	}

	// ---------------------------------------------------------------------------------------------
	// Offline: capabilities
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "Only providers that can stream speech report the speechStream capability" )
	@Test
	public void testStreamCapabilityMatrix() {
		// @formatter:off
		runtime.executeSource(
			"""
			caps = {}
			for( p in [ "openai", "mistral", "gemini", "grok", "groq", "deepseek", "claude" ] ){
				caps[ p ] = aiService( p, { apiKey: "k" } ).hasCapability( "speechStream" )
			}
			""",
			context
		);
		// @formatter:on

		String caps = variables.get( Key.of( "caps" ) ).toString();
		assertThat( caps ).contains( "openai : true" );
		assertThat( caps ).contains( "mistral : true" );
		assertThat( caps ).contains( "gemini : true" );
		assertThat( caps ).contains( "grok : false" );
		assertThat( caps ).contains( "groq : false" );
		assertThat( caps ).contains( "deepseek : false" );
		assertThat( caps ).contains( "claude : false" );
	}

	@DisplayName( "aiSpeakStream refuses inheriting providers that cannot stream (Grok, DeepSeek)" )
	@Test
	public void testStreamRefusedForNonStreamingProviders() {
		// @formatter:off
		runtime.executeSource(
			"""
			types = []
			for( p in [ "grok", "deepseek" ] ){
				try {
					aiSpeakStream( "Hi", ( e ) => {}, {}, { provider: p, apiKey: "k" } )
					types.append( "none" )
				} catch( any e ) {
					types.append( e.type )
				}
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "types" ) ).toString() ).isEqualTo( "[UnsupportedCapability, UnsupportedCapability]" );
	}

	// ---------------------------------------------------------------------------------------------
	// Offline: OpenAI
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "OpenAI streams chunked pcm audio and drops stream_format" )
	@Test
	public void testOpenAIStream() {
		// @formatter:off
		runtime.executeSource(
			"""
			events = []
			summary = aiSpeakStream(
				"Hello there",
				( e ) => { events.append( e ) },
				{ model: "gpt-4o-mini-tts", voice: "nova", stream_format: "sse" },
				{ provider: "openai", apiKey: "oa-key", outputFormat: "pcm", baseURL: "%s/openai" }
			)
			audio = events.filter( ( e ) => e.type == "audio" )
			total = audio.reduce( ( acc, e ) => acc + e.data.len(), 0 )
			fmt = audio.first().format
			rate = audio.first().sampleRate
			lastType = events.last().type
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "total" ) ) ).isEqualTo( 12 );
		assertThat( variables.getAsString( Key.of( "fmt" ) ) ).isEqualTo( "pcm" );
		assertThat( variables.get( Key.of( "rate" ) ) ).isEqualTo( 24000 );
		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( seenHeader.get( "openai.auth" ) ).isEqualTo( "Bearer oa-key" );

		String body = seenBody.get( "openai" );
		assertThat( body ).contains( "\"model\":\"gpt-4o-mini-tts\"" );
		assertThat( body ).contains( "\"voice\":\"nova\"" );
		assertThat( body ).contains( "\"input\":\"Hello there\"" );
		assertThat( body ).contains( "\"response_format\":\"pcm\"" );
		assertThat( body ).doesNotContain( "stream_format" );
	}

	@DisplayName( "OpenAI stream stops early when the callback returns false" )
	@Test
	public void testOpenAIStopEarly() {
		// @formatter:off
		runtime.executeSource(
			"""
			audioCount = 0
			summary = aiSpeakStream(
				"Hi",
				( e ) => { if( e.type == "audio" ){ audioCount++; return false } },
				{},
				{ provider: "openai", apiKey: "k", baseURL: "%s/openai" }
			)
			completed = summary.completed
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "audioCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "completed" ) ) ).isFalse();
	}

	// ---------------------------------------------------------------------------------------------
	// Offline: Mistral
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "Mistral whole-file speech decodes the JSON base64 audio_data response" )
	@Test
	public void testMistralSpeakJson() {
		// @formatter:off
		runtime.executeSource(
			"""
			response = aiSpeak( "Hello", {}, { provider: "mistral", apiKey: "k", baseURL: "%s/mistral" } )
			text = charsetEncode( response.getAudioData(), "utf-8" )
			size = response.getSize()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "text" ) ) ).isEqualTo( "MP3DATA" );
		assertThat( variables.get( Key.of( "size" ) ) ).isEqualTo( 7 );
	}

	@DisplayName( "Mistral streams SSE audio, sends stream:true and maps a UUID voice onto voice_id" )
	@Test
	public void testMistralStream() {
		// @formatter:off
		runtime.executeSource(
			"""
			events = []
			summary = aiSpeakStream(
				"Hello there",
				( e ) => { events.append( e ) },
				{ voice: "22222222-2222-2222-2222-222222222222" },
				{ provider: "mistral", apiKey: "mk", outputFormat: "pcm", baseURL: "%s/mistral" }
			)
			types = events.map( ( e ) => e.type ).toList()
			audio = events.filter( ( e ) => e.type == "audio" )
			total = audio.reduce( ( acc, e ) => acc + e.data.len(), 0 )
			fmt = audio.first().format
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "types" ) ) ).isEqualTo( "audio,audio,done" );
		assertThat( variables.get( Key.of( "total" ) ) ).isEqualTo( 6 );
		assertThat( variables.getAsString( Key.of( "fmt" ) ) ).isEqualTo( "pcm" );
		assertThat( seenHeader.get( "mistral.auth" ) ).isEqualTo( "Bearer mk" );

		String body = seenBody.get( "mistral" );
		assertThat( body ).contains( "\"stream\":true" );
		assertThat( body ).contains( "\"response_format\":\"pcm\"" );
		assertThat( body ).contains( "\"voice_id\":\"22222222-2222-2222-2222-222222222222\"" );
		assertThat( body ).doesNotContain( "\"voice\":" );
	}

	// ---------------------------------------------------------------------------------------------
	// Offline: Gemini
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "Gemini whole-file speech wraps headerless L16 PCM into a playable wav" )
	@Test
	public void testGeminiSpeakWrapsWav() {
		// @formatter:off
		runtime.executeSource(
			"""
			response = aiSpeak( "Hello", {}, { provider: "gemini", apiKey: "gk", baseURL: "%s/gemini" } )
			header = charsetEncode( response.getAudioData().slice( 1, 4 ), "utf-8" )
			size = response.getSize()
			format = response.getAudioFormat()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "header" ) ) ).isEqualTo( "RIFF" );
		assertThat( variables.get( Key.of( "size" ) ) ).isEqualTo( 44 + 6 );
		assertThat( variables.getAsString( Key.of( "format" ) ) ).isEqualTo( "wav" );
	}

	@DisplayName( "Gemini streams step.delta audio through the Interactions API" )
	@Test
	public void testGeminiStream() {
		// @formatter:off
		runtime.executeSource(
			"""
			events = []
			summary = aiSpeakStream(
				"Hello there",
				( e ) => { events.append( e ) },
				{ voice: "Puck", model: "gemini-test-tts" },
				{ provider: "gemini", apiKey: "gk", baseURL: "%s/gemini" }
			)
			types = events.map( ( e ) => e.type ).toList()
			audio = events.filter( ( e ) => e.type == "audio" )
			total = audio.reduce( ( acc, e ) => acc + e.data.len(), 0 )
			fmt = audio.first().format
			rate = audio.first().sampleRate
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "types" ) ) ).isEqualTo( "audio,audio,done" );
		assertThat( variables.get( Key.of( "total" ) ) ).isEqualTo( 6 );
		assertThat( variables.getAsString( Key.of( "fmt" ) ) ).isEqualTo( "pcm" );
		assertThat( variables.get( Key.of( "rate" ) ) ).isEqualTo( 24000 );
		assertThat( seenHeader.get( "geminiStream.google" ) ).isEqualTo( "gk" );

		String body = seenBody.get( "geminiStream" );
		assertThat( body ).contains( "\"stream\":true" );
		assertThat( body ).contains( "\"model\":\"gemini-test-tts\"" );
		assertThat( body ).contains( "\"text\":\"Hello there\"" );
		assertThat( body ).contains( "\"voice\":\"Puck\"" );
		assertThat( body ).contains( "\"type\":\"audio\"" );
	}

	// ---------------------------------------------------------------------------------------------
	// Offline: Grok
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "Grok sends the documented xAI body: text, voice_id, language and output_format" )
	@Test
	public void testGrokRequestShape() {
		// @formatter:off
		runtime.executeSource(
			"""
			response = aiSpeak(
				"Hello there",
				{ voice_id: "ara", language: "en" },
				{ provider: "grok", apiKey: "xk", outputFormat: "wav", speed: 1.2, baseURL: "%s/grok" }
			)
			format = response.getAudioFormat()
			size = response.getSize()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "format" ) ) ).isEqualTo( "wav" );
		assertThat( variables.get( Key.of( "size" ) ) ).isEqualTo( 9 );
		assertThat( seenHeader.get( "grok.auth" ) ).isEqualTo( "Bearer xk" );

		String body = seenBody.get( "grok" );
		assertThat( body ).contains( "\"text\":\"Hello there\"" );
		assertThat( body ).contains( "\"voice_id\":\"ara\"" );
		assertThat( body ).contains( "\"language\":\"en\"" );
		assertThat( body ).contains( "\"codec\":\"wav\"" );
		assertThat( body ).contains( "\"sample_rate\":24000" );
		assertThat( body ).contains( "\"speed\":1.2" );
		assertThat( body ).doesNotContain( "\"input\"" );
	}

	@DisplayName( "Grok defaults: voice eve, language auto, mp3 128k. The voice option and male()/female() are honored" )
	@Test
	public void testGrokDefaultsAndVoices() {
		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeak( "Hi", {}, { provider: "grok", apiKey: "k", baseURL: "%s/grok" } )
			""".formatted( stubURL ),
			context
		);
		// @formatter:on
		String body = seenBody.get( "grok" );
		assertThat( body ).contains( "\"voice_id\":\"eve\"" );
		assertThat( body ).contains( "\"language\":\"auto\"" );
		assertThat( body ).contains( "\"codec\":\"mp3\"" );
		assertThat( body ).contains( "\"bit_rate\":128000" );

		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeak( "Hi", {}, { provider: "grok", apiKey: "k", voice: "rex", baseURL: "%s/grok" } )
			""".formatted( stubURL ),
			context
		);
		// @formatter:on
		assertThat( seenBody.get( "grok" ) ).contains( "\"voice_id\":\"rex\"" );

		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeak().text( "Hi" ).provider( "grok" ).apiKey( "k" ).male().withOptions( { baseURL: "%s/grok" } ).speak()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on
		assertThat( seenBody.get( "grok" ) ).contains( "\"voice_id\":\"rex\"" );
	}

	@DisplayName( "Grok rejects flac and opus" )
	@Test
	public void testGrokUnsupportedFormats() {
		// @formatter:off
		runtime.executeSource(
			"""
			types = []
			for( fmt in [ "flac", "opus" ] ){
				try {
					aiSpeak( "Hi", {}, { provider: "grok", apiKey: "k", outputFormat: fmt, baseURL: "%s/grok" } )
					types.append( "none" )
				} catch( any e ) {
					types.append( e.type )
				}
			}
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "types" ) ).toString() ).isEqualTo( "[InvalidArgument, InvalidArgument]" );
	}

	// ---------------------------------------------------------------------------------------------
	// Live (require provider keys; skipped locally without them)
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "Live OpenAI: speak and chunked streaming" )
	@Test
	public void testLiveOpenAI() {
		assumeTrue( !OPENAI_KEY.isEmpty(), "OPENAI_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			whole = aiSpeak( "Hello from BoxLang.", {}, { provider: "openai" } )
			events = []
			summary = aiSpeakStream(
				"BoxLang is a modern dynamic language for the JVM. This sentence is long enough to arrive in pieces.",
				( e ) => { events.append( e ) },
				{},
				{ provider: "openai", outputFormat: "pcm" }
			)
			wholeSize = whole.getSize()
			audioChunks = events.filter( ( e ) => e.type == "audio" ).len()
			lastType = events.last().type
			bytes = summary.bytes
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "wholeSize" ) ) ).intValue() ).isGreaterThan( 1000 );
		assertThat( ( ( Number ) variables.get( Key.of( "audioChunks" ) ) ).intValue() ).isGreaterThan( 0 );
		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( ( ( Number ) variables.get( Key.of( "bytes" ) ) ).intValue() ).isGreaterThan( 1000 );
	}

	@DisplayName( "Live Mistral: speak (JSON base64) and SSE streaming" )
	@Test
	public void testLiveMistral() {
		assumeTrue( !MISTRAL_KEY.isEmpty(), "MISTRAL_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			whole = aiSpeak( "Hello from BoxLang.", {}, { provider: "mistral" } )
			events = []
			summary = aiSpeakStream(
				"Hello from BoxLang.",
				( e ) => { events.append( e ) },
				{},
				{ provider: "mistral" }
			)
			wholeSize = whole.getSize()
			audioChunks = events.filter( ( e ) => e.type == "audio" ).len()
			lastType = events.last().type
			bytes = summary.bytes
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "wholeSize" ) ) ).intValue() ).isGreaterThan( 500 );
		assertThat( ( ( Number ) variables.get( Key.of( "audioChunks" ) ) ).intValue() ).isGreaterThan( 0 );
		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( ( ( Number ) variables.get( Key.of( "bytes" ) ) ).intValue() ).isGreaterThan( 500 );
	}

	@DisplayName( "Live Gemini: speak returns a playable wav and Interactions streams PCM" )
	@Test
	public void testLiveGemini() {
		assumeTrue( !GEMINI_KEY.isEmpty(), "GEMINI_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			whole = aiSpeak( "Hello from BoxLang.", {}, { provider: "gemini" } )
			header = charsetEncode( whole.getAudioData().slice( 1, 4 ), "utf-8" )
			events = []
			summary = aiSpeakStream(
				"Hello from BoxLang.",
				( e ) => { events.append( e ) },
				{},
				{ provider: "gemini" }
			)
			audioChunks = events.filter( ( e ) => e.type == "audio" ).len()
			lastType = events.last().type
			bytes = summary.bytes
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "header" ) ) ).isEqualTo( "RIFF" );
		assertThat( ( ( Number ) variables.get( Key.of( "audioChunks" ) ) ).intValue() ).isGreaterThan( 0 );
		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( ( ( Number ) variables.get( Key.of( "bytes" ) ) ).intValue() ).isGreaterThan( 500 );
	}

	@DisplayName( "Live Grok: speak returns audio" )
	@Test
	public void testLiveGrok() {
		assumeTrue( !GROK_KEY.isEmpty(), "GROK_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			whole = aiSpeak( "Hello from BoxLang.", {}, { provider: "grok" } )
			wholeSize = whole.getSize()
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "wholeSize" ) ) ).intValue() ).isGreaterThan( 500 );
	}
}
