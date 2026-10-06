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
 * Tests for the ElevenLabs provider (TTS, streaming TTS, STT).
 * <p>
 * Offline tests run against a local stub server through the `baseURL` option. Live tests require an
 * ELEVENLABS_API_KEY environment variable and are skipped without one.
 */
public class ElevenLabsTest extends BaseIntegrationTest {

	private static final String			LIVE_KEY	= dotenv.get( "ELEVENLABS_API_KEY", "" );
	private static HttpServer			stub;
	private static String				stubURL;
	/** Last request seen by the stub: full path + query, body and the api key header */
	private static Map<String, String>	seenURI		= new ConcurrentHashMap<>();
	private static Map<String, String>	seenBody	= new ConcurrentHashMap<>();
	private static Map<String, String>	seenKey		= new ConcurrentHashMap<>();

	@BeforeAll
	public static void startStub() throws IOException {
		stub = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );

		// Both whole-file and /stream endpoints live under this prefix
		stub.createContext( "/v1/text-to-speech", exchange -> {
			String key = exchange.getRequestURI().getPath().endsWith( "/stream" ) ? "stream" : "speak";
			record( exchange, key );
			exchange.getResponseHeaders().add( "Content-Type", "audio/mpeg" );
			exchange.sendResponseHeaders( 200, 0 );
			try ( OutputStream out = exchange.getResponseBody() ) {
				for ( String piece : new String[] { "AAAA", "BBBB", "CCCC" } ) {
					out.write( piece.getBytes( StandardCharsets.UTF_8 ) );
					out.flush();
				}
			}
		} );

		stub.createContext( "/v1/speech-to-text", exchange -> {
			record( exchange, "stt" );
			byte[] json = "{\"text\":\"Hello from Boxlang.\",\"language_code\":\"en\",\"words\":[{\"text\":\"Hello\",\"start\":0.0,\"end\":0.3}]}"
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

	private static void record( HttpExchange exchange, String key ) throws IOException {
		seenURI.put( key, exchange.getRequestURI().toString() );
		seenBody.put( key, new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1 ) );
		seenKey.put( key, exchange.getRequestHeaders().getFirst( "xi-api-key" ) );
	}

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", LIVE_KEY );
		moduleRecord.settings.put( "provider", "elevenlabs" );
	}

	// ---------------------------------------------------------------------------------------------
	// Offline
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "ElevenLabs declares speech, speechStream and transcription" )
	@Test
	public void testCapabilities() {
		// @formatter:off
		runtime.executeSource(
			"""
			service = aiService( "elevenlabs", { apiKey: "k" } )
			hasSpeech = service.hasCapability( "speech" )
			hasStream = service.hasCapability( "speechStream" )
			hasStt = service.hasCapability( "transcription" )
			isStream = isInstanceOf( service, "IAiSpeechStreamService" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasSpeech" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasStream" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasStt" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isStream" ) ) ).isTrue();
	}

	@DisplayName( "speak() honors the requested voice, model, output format and speed (previously ignored)" )
	@Test
	public void testSpeakHonorsRequest() {
		// @formatter:off
		runtime.executeSource(
			"""
			response = aiSpeak(
				"Hello there",
				{ voice: "voiceABC", model_id: "eleven_flash_v2_5" },
				{ provider: "elevenlabs", apiKey: "test-key", outputFormat: "pcm", speed: 1.1, baseURL: "%s" }
			)
			format = response.getAudioFormat()
			size = response.getSize()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "format" ) ) ).isEqualTo( "pcm" );
		assertThat( variables.get( Key.of( "size" ) ) ).isEqualTo( 12 );
		assertThat( seenKey.get( "speak" ) ).isEqualTo( "test-key" );
		assertThat( seenURI.get( "speak" ) ).contains( "/v1/text-to-speech/voiceABC" );
		assertThat( seenURI.get( "speak" ) ).contains( "output_format=pcm_24000" );
		assertThat( seenBody.get( "speak" ) ).contains( "\"model_id\":\"eleven_flash_v2_5\"" );
		assertThat( seenBody.get( "speak" ) ).contains( "\"speed\":1.1" );
	}

	@DisplayName( "male()/female() voices and the voice option are no longer shadowed by the default voice_id" )
	@Test
	public void testVoiceNotShadowed() {
		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeak( "Hi", {}, { provider: "elevenlabs", apiKey: "k", voice: "optionVoice", baseURL: "%s" } )
			""".formatted( stubURL ),
			context
		);
		// @formatter:on
		assertThat( seenURI.get( "speak" ) ).contains( "/v1/text-to-speech/optionVoice" );

		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeak().text( "Hi" ).provider( "elevenlabs" ).apiKey( "k" ).female().withOptions( { baseURL: "%s" } ).speak()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on
		assertThat( seenURI.get( "speak" ) ).contains( "/v1/text-to-speech/EXAVITQu4vr4xnSDxMaL" );

		// Default voice still applies when nothing is requested
		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeak( "Hi", {}, { provider: "elevenlabs", apiKey: "k", baseURL: "%s" } )
			""".formatted( stubURL ),
			context
		);
		// @formatter:on
		assertThat( seenURI.get( "speak" ) ).contains( "/v1/text-to-speech/EXAVITQu4vr4xnSDxMaL" );
	}

	@DisplayName( "wav output wraps raw PCM in a RIFF header" )
	@Test
	public void testWavWrapped() {
		// @formatter:off
		runtime.executeSource(
			"""
			response = aiSpeak( "Hi", {}, { provider: "elevenlabs", apiKey: "k", outputFormat: "wav", baseURL: "%s" } )
			header = charsetEncode( response.getAudioData().slice( 1, 4 ), "utf-8" )
			size = response.getSize()
			format = response.getAudioFormat()
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "header" ) ) ).isEqualTo( "RIFF" );
		assertThat( variables.get( Key.of( "size" ) ) ).isEqualTo( 44 + 12 );
		assertThat( variables.getAsString( Key.of( "format" ) ) ).isEqualTo( "wav" );
		assertThat( seenURI.get( "speak" ) ).contains( "output_format=pcm_24000" );
	}

	@DisplayName( "Format mapping: mulaw, alaw, opus and unsupported formats" )
	@Test
	public void testFormatMapping() {
		// @formatter:off
		runtime.executeSource(
			"""
			aiSpeak( "Hi", {}, { provider: "elevenlabs", apiKey: "k", outputFormat: "mulaw", baseURL: "%s" } )
			""".formatted( stubURL ),
			context
		);
		// @formatter:on
		assertThat( seenURI.get( "speak" ) ).contains( "output_format=ulaw_8000" );

		// @formatter:off
		runtime.executeSource(
			"""
			types = []
			for( fmt in [ "flac", "aac" ] ){
				try {
					aiSpeak( "Hi", {}, { provider: "elevenlabs", apiKey: "k", outputFormat: fmt, baseURL: "%s" } )
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

	@DisplayName( "Streaming hits /stream and delivers progressive audio chunks then done" )
	@Test
	public void testStream() {
		// @formatter:off
		runtime.executeSource(
			"""
			events = []
			summary = aiSpeakStream(
				"Hello there",
				( e ) => { events.append( e ) },
				{ voice: "voiceStream" },
				{ provider: "elevenlabs", apiKey: "k", outputFormat: "mp3", baseURL: "%s" }
			)
			audio = events.filter( ( e ) => e.type == "audio" )
			total = audio.reduce( ( acc, e ) => acc + e.data.len(), 0 )
			lastType = events.last().type
			fmt = audio.first().format
			completed = summary.completed
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "total" ) ) ).isEqualTo( 12 );
		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( variables.getAsString( Key.of( "fmt" ) ) ).isEqualTo( "mp3" );
		assertThat( variables.getAsBoolean( Key.of( "completed" ) ) ).isTrue();
		assertThat( seenURI.get( "stream" ) ).contains( "/v1/text-to-speech/voiceStream/stream" );
		assertThat( seenURI.get( "stream" ) ).contains( "output_format=mp3_44100_128" );
	}

	@DisplayName( "wav cannot be streamed" )
	@Test
	public void testStreamWavRejected() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorType = ""
			try {
				aiSpeakStream( "Hi", ( e ) => {}, {}, { provider: "elevenlabs", apiKey: "k", outputFormat: "wav", baseURL: "%s" } )
			} catch( any e ) {
				errorType = e.type
			}
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "InvalidArgument" );
	}

	@DisplayName( "Returning false stops the stream early" )
	@Test
	public void testStreamStopEarly() {
		// @formatter:off
		runtime.executeSource(
			"""
			audioCount = 0
			summary = aiSpeakStream(
				"Hi",
				( e ) => { if( e.type == "audio" ){ audioCount++; return false } },
				{},
				{ provider: "elevenlabs", apiKey: "k", baseURL: "%s" }
			)
			completed = summary.completed
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "audioCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "completed" ) ) ).isFalse();
	}

	@DisplayName( "transcribe() uploads the audio under the `file` field and maps the response" )
	@Test
	public void testTranscribeFieldName() {
		// @formatter:off
		runtime.executeSource(
			"""
			audioPath = getTempDirectory() & createUUID() & ".wav"
			fileWrite( audioPath, "RIFFxxxxWAVE" )
			text = aiTranscribe( audioPath, {}, { provider: "elevenlabs", apiKey: "stt-key", baseURL: "%s" } )
			fileDelete( audioPath )
			""".formatted( stubURL ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "text" ) ) ).isEqualTo( "Hello from Boxlang." );
		assertThat( seenKey.get( "stt" ) ).isEqualTo( "stt-key" );
		assertThat( seenBody.get( "stt" ) ).contains( "name=\"file\"" );
		assertThat( seenBody.get( "stt" ) ).doesNotContain( "name=\"audio\"" );
	}

	// ---------------------------------------------------------------------------------------------
	// Live (require ELEVENLABS_API_KEY)
	// ---------------------------------------------------------------------------------------------

	@DisplayName( "Live: speak() returns mp3 and wav audio" )
	@Test
	public void testLiveSpeak() {
		assumeTrue( !LIVE_KEY.isEmpty(), "ELEVENLABS_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			mp3 = aiSpeak( "Hello from BoxLang.", {}, { provider: "elevenlabs", outputFormat: "mp3" } )
			wav = aiSpeak( "Hello from BoxLang.", {}, { provider: "elevenlabs", outputFormat: "wav" } )
			mp3Size = mp3.getSize()
			wavHeader = charsetEncode( wav.getAudioData().slice( 1, 4 ), "utf-8" )
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "mp3Size" ) ) ).intValue() ).isGreaterThan( 1000 );
		assertThat( variables.getAsString( Key.of( "wavHeader" ) ) ).isEqualTo( "RIFF" );
	}

	@DisplayName( "Live: mp3 and pcm stream progressively" )
	@Test
	public void testLiveStream() {
		assumeTrue( !LIVE_KEY.isEmpty(), "ELEVENLABS_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			mp3Events = []
			aiSpeakStream(
				"BoxLang is a modern dynamic language for the JVM. This sentence is long enough to arrive in pieces.",
				( e ) => { mp3Events.append( e ) },
				{},
				{ provider: "elevenlabs", outputFormat: "mp3" }
			)
			pcmBytes = aiSpeakStream( "Hello from BoxLang.", ( e ) => {}, {}, { provider: "elevenlabs", outputFormat: "pcm" } ).bytes
			mp3Chunks = mp3Events.filter( ( e ) => e.type == "audio" ).len()
			lastType = mp3Events.last().type
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "mp3Chunks" ) ) ).intValue() ).isGreaterThan( 0 );
		assertThat( variables.getAsString( Key.of( "lastType" ) ) ).isEqualTo( "done" );
		assertThat( ( ( Number ) variables.get( Key.of( "pcmBytes" ) ) ).intValue() ).isGreaterThan( 1000 );
	}

	@DisplayName( "Live: speech round trips through Scribe transcription" )
	@Test
	public void testLiveRoundTrip() {
		assumeTrue( !LIVE_KEY.isEmpty(), "ELEVENLABS_API_KEY not set" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			audioPath = getTempDirectory() & createUUID() & ".mp3"
			aiSpeak( "Hello from BoxLang.", {}, { provider: "elevenlabs", outputFile: audioPath } )
			text = aiTranscribe( audioPath, {}, { provider: "elevenlabs" } ).lCase()
			fileDelete( audioPath )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "text" ) ) ).contains( "hello" );
	}

	@DisplayName( "Live: a bad voice id raises a ProviderError" )
	@Test
	public void testLiveBadVoice() {
		assumeTrue( !LIVE_KEY.isEmpty(), "ELEVENLABS_API_KEY not set" );

		// @formatter:off
		runtime.executeSource(
			"""
			errorType = ""
			try {
				aiSpeak( "Hi", { voice_id: "does-not-exist" }, { provider: "elevenlabs" } )
			} catch( any e ) {
				errorType = e.type
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "ProviderError" );
	}
}
