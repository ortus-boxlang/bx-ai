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

import com.sun.net.httpserver.HttpServer;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Tests for the Cartesia provider (TTS, streaming TTS, STT).
 * <p>
 * Offline tests run against a local stub server through the `baseURL` option so they verify request mapping,
 * the streaming callback contract, early stop and error handling with no network.
 * Live tests require a CARTESIA_API_KEY environment variable and are skipped without one.
 */
public class CartesiaTest extends BaseIntegrationTest {

	private static final String			LIVE_KEY	= dotenv.get( "CARTESIA_API_KEY", "" );
	private static HttpServer			stub;
	private static String				stubURL;
	/** Last request observed by the stub, keyed by path */
	private static Map<String, String>	seenBodies	= new ConcurrentHashMap<>();
	private static Map<String, String>	seenAuth	= new ConcurrentHashMap<>();
	private static Map<String, String>	seenVersion	= new ConcurrentHashMap<>();

	@BeforeAll
	public static void startStub() throws IOException {
		stub = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		// Slow handlers must not block the other requests
		stub.setExecutor( java.util.concurrent.Executors.newCachedThreadPool() );

		// Progressive raw bytes: three pieces, flushed separately
		stub.createContext( "/tts/bytes", exchange -> {
			record( exchange );
			exchange.getResponseHeaders().add( "Content-Type", "audio/mpeg" );
			exchange.sendResponseHeaders( 200, 0 );
			try ( OutputStream out = exchange.getResponseBody() ) {
				for ( String piece : new String[] { "AAAA", "BBBB", "CCCC" } ) {
					out.write( piece.getBytes( StandardCharsets.UTF_8 ) );
					out.flush();
				}
			}
		} );

		// SSE: two audio chunks around a timestamps event, then done
		stub.createContext( "/tts/sse", exchange -> {
			record( exchange );
			exchange.getResponseHeaders().add( "Content-Type", "text/event-stream" );
			exchange.sendResponseHeaders( 200, 0 );
			String	a		= Base64.getEncoder().encodeToString( "1234".getBytes( StandardCharsets.UTF_8 ) );
			String	b		= Base64.getEncoder().encodeToString( "56".getBytes( StandardCharsets.UTF_8 ) );
			String	events	= "event: chunk\ndata: {\"type\":\"chunk\",\"status_code\":206,\"done\":false,\"data\":\"" + a + "\"}\n\n"
			    + "event: timestamps\ndata: {\"type\":\"timestamps\",\"status_code\":206,\"done\":false,\"word_timestamps\":{\"words\":[\"Hello\"],\"start\":[0.1],\"end\":[0.4]}}\n\n"
			    + "event: chunk\ndata: {\"type\":\"chunk\",\"status_code\":206,\"done\":false,\"data\":\"" + b + "\"}\n\n"
			    + "event: done\ndata: {\"type\":\"done\",\"status_code\":200,\"done\":true}\n\n";
			// A request tagged SLOWSTOP gets one chunk and then silence, to prove an early stop hangs up promptly
			if ( seenBodies.get( "/tts/sse" ).contains( "SLOWSTOP" ) ) {
				try ( OutputStream out = exchange.getResponseBody() ) {
					out.write( ( "event: chunk\ndata: {\"type\":\"chunk\",\"status_code\":206,\"done\":false,\"data\":\"" + a + "\"}\n\n" )
					    .getBytes( StandardCharsets.UTF_8 ) );
					out.flush();
					Thread.sleep( 8000 );
					out.write( events.getBytes( StandardCharsets.UTF_8 ) );
				} catch ( IOException | InterruptedException ignored ) {
					// The client closing the connection is the expected outcome
				}
				return;
			}
			try ( OutputStream out = exchange.getResponseBody() ) {
				out.write( events.getBytes( StandardCharsets.UTF_8 ) );
			}
		} );

		// Transcription
		stub.createContext( "/stt", exchange -> {
			record( exchange );
			byte[] json = "{\"type\":\"transcript\",\"text\":\"Hello from Boxlang.\",\"language\":\"en\",\"duration\":1.5,\"words\":[{\"word\":\" Hello\",\"start\":0.04,\"end\":0.34}]}"
			    .getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().add( "Content-Type", "application/json" );
			exchange.sendResponseHeaders( 200, json.length );
			try ( OutputStream out = exchange.getResponseBody() ) {
				out.write( json );
			}
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

	private static void record( com.sun.net.httpserver.HttpExchange exchange ) throws IOException {
		String path = exchange.getRequestURI().getPath();
		seenBodies.put( path, new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 ) );
		seenAuth.put( path, exchange.getRequestHeaders().getFirst( "Authorization" ) );
		seenVersion.put( path, exchange.getRequestHeaders().getFirst( "Cartesia-Version" ) );
	}

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", LIVE_KEY );
		moduleRecord.settings.put( "provider", "cartesia" );
	}

	// ---------------------------------------------------------------------------------------------
	// Offline: registration and capabilities
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "Cartesia is a registered provider with speech, speechStream and transcription capabilities" )
	@Test
	public void testCapabilities() {
		// @formatter:off
		runtime.executeSource(
			"""
			service = aiService( "cartesia", { apiKey: "k" } )
			name = service.getName()
			hasSpeech = service.hasCapability( "speech" )
			hasStream = service.hasCapability( "speechStream" )
			hasStt = service.hasCapability( "transcription" )
			hasChat = service.hasCapability( "chat" )
			isSpeech = isInstanceOf( service, "IAiSpeechService" )
			isStream = isInstanceOf( service, "IAiSpeechStreamService" )
			isStt = isInstanceOf( service, "IAiTranscriptionService" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "Cartesia" );
		assertThat( variables.getAsBoolean( Key.of( "hasSpeech" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasStream" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasStt" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasChat" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isSpeech" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isStream" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isStt" ) ) ).isTrue();
	}

	@DisplayName( "Cartesia translate() throws UnsupportedCapability" )
	@Test
	public void testTranslateUnsupported() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorType = ""
			audioPath = getTempDirectory() & createUUID() & ".wav"
			fileWrite( audioPath, "RIFFxxxxWAVE" )
			try {
				aiTranslate( audioPath, {}, { provider: "cartesia", apiKey: "k" } )
			} catch( any e ) {
				errorType = e.type
			}
			fileDelete( audioPath )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "UnsupportedCapability" );
	}

	@DisplayName( "Unsupported output formats throw InvalidArgument before any network call" )
	@Test
	public void testUnsupportedFormats() {
		// @formatter:off
		runtime.executeSource(
			"""
			types = []
			for( fmt in [ "flac", "opus", "aac" ] ){
				try {
					aiSpeak( "Hi", {}, { provider: "cartesia", apiKey: "k", outputFormat: fmt, baseURL: "%s" } )
					types.append( "none" )
				} catch( any e ) {
					types.append( e.type )
				}
			}
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "types" ) ).toString() ).isEqualTo( "[InvalidArgument, InvalidArgument, InvalidArgument]" );
	}

	@DisplayName( "aiSpeakStream throws UnsupportedCapability for providers that cannot stream speech" )
	@Test
	public void testStreamUnsupportedProvider() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorType = ""
			try {
				aiSpeakStream( "Hi", ( e ) => {}, {}, { provider: "voyage", apiKey: "k" } )
			} catch( any e ) {
				errorType = e.type
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "UnsupportedCapability" );
	}

	@DisplayName( "aiSpeakStream rejects empty text" )
	@Test
	public void testStreamEmptyText() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorType = ""
			try {
				aiSpeakStream( "  ", ( e ) => {}, {}, { provider: "cartesia", apiKey: "k" } )
			} catch( any e ) {
				errorType = e.type
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "InvalidArgument" );
	}

	// ---------------------------------------------------------------------------------------------
	// Offline: request mapping and streaming contract against the stub
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "speak() maps the request onto the Cartesia body and headers" )
	@Test
	public void testSpeakRequestMapping() {
		// @formatter:off
		runtime.executeSource(
			"""
			response = aiSpeak(
				"Hello there",
				{ voice: "11111111-1111-1111-1111-111111111111", language: "en", emotion: "happy", volume: 1.5 },
				{ provider: "cartesia", apiKey: "test-key", outputFormat: "mp3", speed: 1.2, baseURL: "%s" }
			)
			size = response.getSize()
			format = response.getAudioFormat()
			providerName = response.getProvider()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "size" ) ) ).isEqualTo( 12 );
		assertThat( variables.getAsString( Key.of( "format" ) ) ).isEqualTo( "mp3" );
		assertThat( variables.getAsString( Key.of( "providerName" ) ) ).isEqualTo( "Cartesia" );
		assertThat( seenAuth.get( "/tts/bytes" ) ).isEqualTo( "Bearer test-key" );
		assertThat( seenVersion.get( "/tts/bytes" ) ).isEqualTo( "2026-08-14" );

		String body = seenBodies.get( "/tts/bytes" );
		assertThat( body ).contains( "\"model_id\":\"sonic-3.6\"" );
		assertThat( body ).contains( "\"transcript\":\"Hello there\"" );
		assertThat( body ).contains( "\"voice\":\"11111111-1111-1111-1111-111111111111\"" );
		assertThat( body ).contains( "\"container\":\"mp3\"" );
		assertThat( body ).contains( "\"language\":\"en\"" );
		assertThat( body ).contains( "\"speed\":1.2" );
		assertThat( body ).contains( "\"emotion\":\"happy\"" );
		assertThat( body ).contains( "\"volume\":1.5" );
	}

	@DisplayName( "male() and female() resolve to Cartesia voice IDs" )
	@Test
	public void testGenderedVoices() {
		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeak().text( "Hi" ).provider( "cartesia" ).apiKey( "k" ).female().withOptions( { baseURL: "%s" } ).speak()
			femaleBody = ""
			""".formatted( stubURL ),
			context
		);
		// @formatter:on
		assertThat( seenBodies.get( "/tts/bytes" ) ).contains( "\"voice\":\"db6b0ed5-d5d3-463d-ae85-518a07d3c2b4\"" );

		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeak().text( "Hi" ).provider( "cartesia" ).apiKey( "k" ).male().withOptions( { baseURL: "%s" } ).speak()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on
		assertThat( seenBodies.get( "/tts/bytes" ) ).contains( "\"voice\":\"47c38ca4-5f35-497b-b1a3-415245fb35e1\"" );
	}

	@DisplayName( "mp3 streaming uses /tts/bytes and delivers progressive audio chunks then done" )
	@Test
	public void testStreamBytesMp3() {
		// @formatter:off
		runtime.executeSource(
			"""
			events = []
			summary = aiSpeakStream(
				"Hello there",
				( e ) => { events.append( e ) },
				{},
				{ provider: "cartesia", apiKey: "k", outputFormat: "mp3", baseURL: "%s" }
			)
			audio = events.filter( ( e ) => e.type == "audio" )
			lastType = events.last().type
			firstFormat = audio.first().format
			firstIsBinary = isBinary( audio.first().data )
			total = audio.reduce( ( acc, e ) => acc + e.data.len(), 0 )
			sequences = audio.map( ( e ) => e.sequence ).toList()
			completed = summary.completed
			chunkCount = summary.chunks
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( variables.getAsString( Key.of( "firstFormat" ) ) ).isEqualTo( "mp3" );
		assertThat( variables.getAsBoolean( Key.of( "firstIsBinary" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "total" ) ) ).isEqualTo( 12 );
		assertThat( variables.getAsString( Key.of( "sequences" ) ) ).startsWith( "1" );
		assertThat( seenBodies.get( "/tts/bytes" ) ).contains( "\"container\":\"mp3\"" );

		// Summary struct
		assertThat( variables.getAsBoolean( Key.of( "completed" ) ) ).isTrue();
		// Chunk boundaries depend on transport buffering, so only require progress and the byte total above
		assertThat( ( ( Number ) variables.get( Key.of( "chunkCount" ) ) ).intValue() ).isGreaterThan( 0 );
	}

	@DisplayName( "pcm streaming uses /tts/sse, decodes base64 audio and surfaces timestamps" )
	@Test
	public void testStreamSSEPcm() {
		// @formatter:off
		runtime.executeSource(
			"""
			events = []
			summary = aiSpeakStream(
				"Hello there",
				( e ) => { events.append( e ) },
				{ add_timestamps: true, sample_rate: 16000 },
				{ provider: "cartesia", apiKey: "k", outputFormat: "pcm", baseURL: "%s" }
			)
			types = events.map( ( e ) => e.type ).toList()
			audio = events.filter( ( e ) => e.type == "audio" )
			total = audio.reduce( ( acc, e ) => acc + e.data.len(), 0 )
			rate = audio.first().sampleRate
			fmt = audio.first().format
			stamp = events.filter( ( e ) => e.type == "timestamps" ).first()
			word = stamp.words.first()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "types" ) ) ).isEqualTo( "audio,timestamps,audio,done" );
		assertThat( variables.get( Key.of( "total" ) ) ).isEqualTo( 6 );
		assertThat( variables.get( Key.of( "rate" ) ) ).isEqualTo( 16000 );
		assertThat( variables.getAsString( Key.of( "fmt" ) ) ).isEqualTo( "pcm" );
		assertThat( variables.getAsString( Key.of( "word" ) ) ).isEqualTo( "Hello" );

		String body = seenBodies.get( "/tts/sse" );
		assertThat( body ).contains( "\"container\":\"raw\"" );
		assertThat( body ).contains( "\"encoding\":\"pcm_s16le\"" );
		assertThat( body ).contains( "\"sample_rate\":16000" );
		assertThat( body ).contains( "\"add_timestamps\":true" );
	}

	@DisplayName( "mulaw streaming sends pcm_mulaw at 8000 Hz" )
	@Test
	public void testStreamMulaw() {
		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeakStream( "Hi", ( e ) => {}, {}, { provider: "cartesia", apiKey: "k", outputFormat: "mulaw", baseURL: "%s" } )
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		String body = seenBodies.get( "/tts/sse" );
		assertThat( body ).contains( "\"encoding\":\"pcm_mulaw\"" );
		assertThat( body ).contains( "\"sample_rate\":8000" );
	}

	@DisplayName( "Returning false from the callback stops the stream early and still emits done" )
	@Test
	public void testStreamStopEarly() {
		// @formatter:off
		runtime.executeSource(
			"""
			events = []
			summary = aiSpeakStream(
				"Hello there",
				( e ) => {
					events.append( e )
					if( e.type == "audio" ) return false
				},
				{},
				{ provider: "cartesia", apiKey: "k", outputFormat: "mp3", baseURL: "%s" }
			)
			audioCount = events.filter( ( e ) => e.type == "audio" ).len()
			lastType = events.last().type
			completed = summary.completed
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "audioCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( variables.getAsBoolean( Key.of( "completed" ) ) ).isFalse();
	}

	@DisplayName( "Fluent builder .stream() terminator streams through the same path" )
	@Test
	public void testFluentStream() {
		// @formatter:off
		runtime.executeSource(
			"""
			chunks = 0
			summary = aiSpeak()
				.text( "Hello" )
				.provider( "cartesia" )
				.apiKey( "k" )
				.asMP3()
				.withOptions( { baseURL: "%s" } )
				.stream( ( e ) => { if( e.type == "audio" ) chunks++ } )
			bytes = summary.bytes
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "chunks" ) ) ).isNotEqualTo( 0 );
		assertThat( variables.get( Key.of( "bytes" ) ) ).isEqualTo( 12 );
	}

	@DisplayName( "SSE transport rejects mp3, bytes transport can be forced" )
	@Test
	public void testTransportOverride() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorType = ""
			try {
				aiSpeakStream( "Hi", ( e ) => {}, { transport: "sse" }, { provider: "cartesia", apiKey: "k", outputFormat: "mp3", baseURL: "%s" } )
			} catch( any e ) {
				errorType = e.type
			}
			forced = aiSpeakStream( "Hi", ( e ) => {}, { transport: "bytes" }, { provider: "cartesia", apiKey: "k", outputFormat: "pcm", baseURL: "%s" } )
			forcedBytes = forced.bytes
			""".formatted( stubURL, stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "InvalidArgument" );
		assertThat( variables.get( Key.of( "forcedBytes" ) ) ).isEqualTo( 12 );
	}

	@DisplayName( "Stopping an SSE stream early closes the connection instead of draining it" )
	@Test
	public void testSseEarlyStopReturnsPromptly() {
		// @formatter:off
		runtime.executeSource(
			"""
			start = getTickCount()
			events = []
			summary = aiSpeakStream(
				"SLOWSTOP",
				( e ) => {
					events.append( e )
					if( e.type == "audio" ) return false
				},
				{},
				{ provider: "cartesia", apiKey: "k", outputFormat: "pcm", baseURL: "%s" }
			)
			elapsed = getTickCount() - start
			lastType = events.last().type
			completed = summary.completed
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		// The stub would hold the connection for 8s. Returning well before that proves we hung up.
		assertThat( ( ( Number ) variables.get( Key.of( "elapsed" ) ) ).intValue() ).isLessThan( 5000 );
		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( variables.getAsBoolean( Key.of( "completed" ) ) ).isFalse();
	}

	@DisplayName( "A provider key resolved from the environment wins over the module-global apiKey" )
	@Test
	public void testResolvedKeyBeatsGlobalKey() {
		System.setProperty( "CARTESIA_API_KEY", "convention-key" );
		moduleRecord.settings.put( "apiKey", "global-key-for-another-provider" );
		try {
			// @formatter:off
			runtime.executeSource(
				"""
				expected = getSystemSetting( "CARTESIA_API_KEY", "" )
				aiSpeakStream( "Hi", ( e ) => {}, {}, { provider: "cartesia", outputFormat: "mp3", baseURL: "%s" } )
				streamAuth = ""
				aiSpeak( "Hi", {}, { provider: "cartesia", outputFormat: "mp3", baseURL: "%s" } )
				""".formatted( stubURL, stubURL ),
				context
			);
			// @formatter:on

			String expected = variables.getAsString( Key.of( "expected" ) );
			assertThat( expected ).isNotEmpty();
			assertThat( expected ).isNotEqualTo( "global-key-for-another-provider" );
			// Both the streaming and the whole-file calls hit /tts/bytes for mp3
			assertThat( seenAuth.get( "/tts/bytes" ) ).isEqualTo( "Bearer " + expected );
		} finally {
			System.clearProperty( "CARTESIA_API_KEY" );
		}
	}

	@DisplayName( "Request-specific speed does not leak into a reused generation_config struct" )
	@Test
	public void testGenerationConfigNotMutated() {
		// @formatter:off
		runtime.executeSource(
			"""
			cfg = { volume: 1.5 }
			aiSpeak( "One", { generation_config: cfg }, { provider: "cartesia", apiKey: "k", speed: 1.2, baseURL: "%s" } )
			firstBody = ""
			aiSpeak( "Two", { generation_config: cfg }, { provider: "cartesia", apiKey: "k", speed: 0.8, baseURL: "%s" } )
			cfgHasSpeed = cfg.keyExists( "speed" )
			""".formatted( stubURL, stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "cfgHasSpeed" ) ) ).isFalse();
		String body = seenBodies.get( "/tts/bytes" );
		assertThat( body ).contains( "\"speed\":0.8" );
		assertThat( body ).doesNotContain( "\"speed\":1.2" );
		assertThat( body ).contains( "\"volume\":1.5" );
	}

	@DisplayName( "transcribe() posts to /stt and maps text, language, words and duration" )
	@Test
	public void testTranscribeMapping() {
		// @formatter:off
		runtime.executeSource(
			"""
			audioPath = getTempDirectory() & createUUID() & ".wav"
			fileWrite( audioPath, "RIFFxxxxWAVE" )
			response = aiTranscribe( audioPath, {}, { provider: "cartesia", apiKey: "stt-key", baseURL: "%s", returnFormat: "response" } )
			text = response.getText()
			language = response.getLanguage()
			wordCount = response.getWords().len()
			fileDelete( audioPath )
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "text" ) ) ).isEqualTo( "Hello from Boxlang." );
		assertThat( variables.getAsString( Key.of( "language" ) ) ).isEqualTo( "en" );
		assertThat( variables.get( Key.of( "wordCount" ) ) ).isEqualTo( 1 );
		assertThat( seenAuth.get( "/stt" ) ).isEqualTo( "Bearer stt-key" );
		assertThat( seenVersion.get( "/stt" ) ).isEqualTo( "2026-08-14" );
	}

	@DisplayName( "A Cartesia error response surfaces as a ProviderError with the provider message" )
	@Test
	public void testErrorHandling() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorType = ""
			errorMessage = ""
			try {
				aiSpeak( "Hi", { voice: "not-a-uuid" }, { provider: "cartesia", apiKey: "k", outputFormat: "mp3", baseURL: "http://127.0.0.1:1" } )
			} catch( any e ) {
				errorType = e.type
				errorMessage = e.message
			}
			""",
			context
		);
		// @formatter:on

		// Connection refused is still an error the caller can catch
		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isNotEmpty();
	}

	// ---------------------------------------------------------------------------------------------
	// Live (require CARTESIA_API_KEY)
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "Live: speak() returns playable mp3 and wav audio" )
	@Test
	public void testLiveSpeak() {
		assumeTrue( !LIVE_KEY.isEmpty(), "CARTESIA_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			mp3 = aiSpeak( "Hello from BoxLang.", {}, { provider: "cartesia", outputFormat: "mp3" } )
			wav = aiSpeak( "Hello from BoxLang.", {}, { provider: "cartesia", outputFormat: "wav" } )
			mp3Size = mp3.getSize()
			mp3Mime = mp3.getMimeType()
			wavHeader = charsetEncode( wav.getAudioData().slice( 1, 4 ), "utf-8" )
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "mp3Size" ) ) ).intValue() ).isGreaterThan( 1000 );
		assertThat( variables.getAsString( Key.of( "mp3Mime" ) ) ).isEqualTo( "audio/mpeg" );
		assertThat( variables.getAsString( Key.of( "wavHeader" ) ) ).isEqualTo( "RIFF" );
	}

	@DisplayName( "Live: mp3 streams progressively over /tts/bytes" )
	@Test
	public void testLiveStreamMp3() {
		assumeTrue( !LIVE_KEY.isEmpty(), "CARTESIA_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			events = []
			summary = aiSpeakStream(
				"BoxLang is a modern dynamic language for the JVM. This sentence is long enough to arrive in several pieces.",
				( e ) => { events.append( e ) },
				{},
				{ provider: "cartesia", outputFormat: "mp3" }
			)
			audioChunks = events.filter( ( e ) => e.type == "audio" ).len()
			lastType = events.last().type
			bytes = summary.bytes
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "audioChunks" ) ) ).intValue() ).isGreaterThan( 1 );
		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( ( ( Number ) variables.get( Key.of( "bytes" ) ) ).intValue() ).isGreaterThan( 1000 );
	}

	@DisplayName( "Live: pcm streams over SSE with word timestamps" )
	@Test
	public void testLiveStreamPcmWithTimestamps() {
		assumeTrue( !LIVE_KEY.isEmpty(), "CARTESIA_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			events = []
			summary = aiSpeakStream(
				"Hello streaming world.",
				( e ) => { events.append( e ) },
				{ add_timestamps: true },
				{ provider: "cartesia", outputFormat: "pcm" }
			)
			audioChunks = events.filter( ( e ) => e.type == "audio" ).len()
			stampCount = events.filter( ( e ) => e.type == "timestamps" ).len()
			rate = events.filter( ( e ) => e.type == "audio" ).first().sampleRate
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "audioChunks" ) ) ).intValue() ).isGreaterThan( 0 );
		assertThat( ( ( Number ) variables.get( Key.of( "stampCount" ) ) ).intValue() ).isGreaterThan( 0 );
		assertThat( ( ( Number ) variables.get( Key.of( "rate" ) ) ).intValue() ).isEqualTo( 24000 );
	}

	@DisplayName( "Live: mulaw 8k streams for telephony" )
	@Test
	public void testLiveStreamMulaw() {
		assumeTrue( !LIVE_KEY.isEmpty(), "CARTESIA_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			bytes = aiSpeakStream( "Hello caller.", ( e ) => {}, {}, { provider: "cartesia", outputFormat: "mulaw" } ).bytes
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "bytes" ) ) ).intValue() ).isGreaterThan( 500 );
	}

	@DisplayName( "Live: a bad voice id raises a ProviderError with Cartesia's message" )
	@Test
	public void testLiveBadVoice() {
		assumeTrue( !LIVE_KEY.isEmpty(), "CARTESIA_API_KEY not set" );

		// @formatter:off
		runtime.executeSource(
			"""
			errorType = ""
			errorMessage = ""
			try {
				aiSpeak( "Hi", { voice: "nope" }, { provider: "cartesia" } )
			} catch( any e ) {
				errorType = e.type
				errorMessage = e.message
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "ProviderError" );
		assertThat( variables.getAsString( Key.of( "errorMessage" ) ) ).contains( "Cartesia TTS error" );
	}

	@DisplayName( "Live: speech round trips through Ink-Whisper transcription" )
	@Test
	public void testLiveRoundTrip() {
		assumeTrue( !LIVE_KEY.isEmpty(), "CARTESIA_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			audioPath = getTempDirectory() & createUUID() & ".wav"
			aiSpeak( "Hello from BoxLang.", {}, { provider: "cartesia", outputFormat: "wav", outputFile: audioPath } )
			text = aiTranscribe( audioPath, {}, { provider: "cartesia" } ).lCase()
			fileDelete( audioPath )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "text" ) ) ).contains( "hello" );
	}
}
