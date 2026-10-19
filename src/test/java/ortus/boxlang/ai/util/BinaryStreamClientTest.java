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
package ortus.boxlang.ai.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import ortus.boxlang.runtime.types.IStruct;

/**
 * Unit tests for the streaming HTTP client. No BoxLang runtime or network is needed: a local stub server
 * provides stalled, slow, error and SSE responses.
 */
public class BinaryStreamClientTest {

	private static HttpServer			stub;
	private static String				baseURL;
	/** Released when the stalled handler may finish, so the stub never leaks a thread past the test run */
	private static final CountDownLatch	RELEASE	= new CountDownLatch( 1 );

	@BeforeAll
	public static void start() throws IOException {
		stub = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		stub.setExecutor( java.util.concurrent.Executors.newCachedThreadPool() );

		// Sends headers and one piece, then stalls
		stub.createContext( "/stall", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.getResponseHeaders().add( "Content-Type", "audio/mpeg" );
			exchange.sendResponseHeaders( 200, 0 );
			OutputStream out = exchange.getResponseBody();
			out.write( "AAAA".getBytes( StandardCharsets.UTF_8 ) );
			out.flush();
			try {
				RELEASE.await( 20, TimeUnit.SECONDS );
			} catch ( InterruptedException ignored ) {
			}
			try {
				out.close();
			} catch ( IOException ignored ) {
			}
		} );

		// Error status whose body stalls after the headers
		stub.createContext( "/stall-error", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders( 500, 0 );
			OutputStream out = exchange.getResponseBody();
			out.write( "partial".getBytes( StandardCharsets.UTF_8 ) );
			out.flush();
			try {
				RELEASE.await( 20, TimeUnit.SECONDS );
			} catch ( InterruptedException ignored ) {
			}
			try {
				out.close();
			} catch ( IOException ignored ) {
			}
		} );

		// Immediate error with a body
		stub.createContext( "/error", exchange -> {
			exchange.getRequestBody().readAllBytes();
			byte[] body = "{\"message\":\"bad voice\"}".getBytes( StandardCharsets.UTF_8 );
			exchange.sendResponseHeaders( 400, body.length );
			try ( OutputStream out = exchange.getResponseBody() ) {
				out.write( body );
			}
		} );

		// Healthy stream that takes longer overall than the idle timeout, as a long synthesis would
		stub.createContext( "/long", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders( 200, 0 );
			try ( OutputStream out = exchange.getResponseBody() ) {
				for ( int i = 0; i < 6; i++ ) {
					out.write( "BBBB".getBytes( StandardCharsets.UTF_8 ) );
					out.flush();
					try {
						Thread.sleep( 700 );
					} catch ( InterruptedException ignored ) {
					}
				}
			}
		} );

		// SSE with comments, multi-line data, CRLF and a trailing event with no blank line
		stub.createContext( "/sse", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.getResponseHeaders().add( "Content-Type", "text/event-stream" );
			exchange.sendResponseHeaders( 200, 0 );
			String events = ": keep-alive\n\n"
			    + "event: one\nid: 1\ndata: first\n\n"
			    + "data: line1\ndata: line2\n\n"
			    + "event: crlf\r\ndata: windows\r\n\r\n"
			    + "data: tail";
			try ( OutputStream out = exchange.getResponseBody() ) {
				out.write( events.getBytes( StandardCharsets.UTF_8 ) );
			}
		} );

		// SSE that sends one event then dawdles, to prove an early stop closes the connection
		stub.createContext( "/sse-slow", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.getResponseHeaders().add( "Content-Type", "text/event-stream" );
			exchange.sendResponseHeaders( 200, 0 );
			OutputStream out = exchange.getResponseBody();
			try {
				out.write( "data: one\n\n".getBytes( StandardCharsets.UTF_8 ) );
				out.flush();
				RELEASE.await( 20, TimeUnit.SECONDS );
				out.write( "data: two\n\n".getBytes( StandardCharsets.UTF_8 ) );
				out.flush();
			} catch ( IOException | InterruptedException ignored ) {
				// The client closing the connection is the expected outcome
			} finally {
				try {
					out.close();
				} catch ( IOException ignored ) {
				}
			}
		} );

		stub.start();
		baseURL = "http://127.0.0.1:" + stub.getAddress().getPort();
	}

	@AfterAll
	public static void stop() {
		RELEASE.countDown();
		stub.stop( 0 );
	}

	@DisplayName( "A stream that stalls after headers is aborted by the idle timeout instead of blocking forever" )
	@Test
	public void testIdleTimeoutAbortsStalledBody() {
		long			start		= System.nanoTime();
		AtomicInteger	received	= new AtomicInteger();

		IOException		error		= assertThrows( IOException.class, () -> BinaryStreamClient.streamPost( baseURL + "/stall", Map.of(), "{}", 2, 0, chunk -> {
										received.addAndGet( chunk.length );
										return true;
									} ) );

		long			seconds		= TimeUnit.NANOSECONDS.toSeconds( System.nanoTime() - start );
		assertThat( error.getMessage() ).contains( "timed out" );
		assertThat( received.get() ).isEqualTo( 4 );
		assertThat( seconds ).isAtMost( 8L );
	}

	@DisplayName( "An error response whose body stalls is also bounded by the timeout" )
	@Test
	public void testIdleTimeoutAbortsStalledErrorBody() {
		long		start	= System.nanoTime();
		IOException	error	= assertThrows( IOException.class,
		    () -> BinaryStreamClient.streamPost( baseURL + "/stall-error", Map.of(), "{}", 2, 0, chunk -> true ) );

		assertThat( error.getMessage() ).contains( "timed out" );
		assertThat( TimeUnit.NANOSECONDS.toSeconds( System.nanoTime() - start ) ).isAtMost( 8L );
	}

	@DisplayName( "A healthy stream longer than the idle timeout is not cut off" )
	@Test
	public void testIdleTimeoutDoesNotTruncateHealthyStream() throws Exception {
		AtomicInteger	received	= new AtomicInteger();
		// 6 pieces, 700ms apart (about 4s total) with a 2s idle timeout
		long			total		= BinaryStreamClient.streamPost( baseURL + "/long", Map.of(), "{}", 2, 0, chunk -> {
										received.addAndGet( chunk.length );
										return true;
									} );

		assertThat( total ).isEqualTo( 24L );
		assertThat( received.get() ).isEqualTo( 24 );
	}

	@DisplayName( "A non-2xx response surfaces the status and the provider body" )
	@Test
	public void testErrorBodySurfaced() {
		IOException error = assertThrows( IOException.class, () -> BinaryStreamClient.streamPost( baseURL + "/error", Map.of(), "{}", 5, 0, chunk -> true ) );
		assertThat( error.getMessage() ).contains( "HTTP 400" );
		assertThat( error.getMessage() ).contains( "bad voice" );

		IOException sseError = assertThrows( IOException.class,
		    () -> BinaryStreamClient.streamSsePost( baseURL + "/error", Map.of(), "{}", 5, event -> true ) );
		assertThat( sseError.getMessage() ).contains( "HTTP 400" );
		assertThat( sseError.getMessage() ).contains( "bad voice" );
	}

	@DisplayName( "Returning false from the byte callback stops after the first chunk" )
	@Test
	public void testBinaryEarlyStop() throws Exception {
		AtomicInteger	calls	= new AtomicInteger();
		long			total	= BinaryStreamClient.streamPost( baseURL + "/long", Map.of(), "{}", 5, 0, chunk -> {
									calls.incrementAndGet();
									return false;
								} );
		assertThat( calls.get() ).isEqualTo( 1 );
		assertThat( total ).isEqualTo( 4L );
	}

	@DisplayName( "SSE parsing handles comments, ids, multi-line data, CRLF and an unterminated final event" )
	@Test
	public void testSseParsing() throws Exception {
		List<String>	seen		= new ArrayList<>();
		long			delivered	= BinaryStreamClient.streamSsePost( baseURL + "/sse", Map.of(), "{}", 5, event -> {
										seen.add( event.get( "event" ) + "|" + event.get( "id" ) + "|" + event.get( "data" ) );
										return true;
									} );

		assertThat( delivered ).isEqualTo( 4L );
		assertThat( seen ).containsExactly( "one|1|first", "||line1\nline2", "crlf||windows", "||tail" ).inOrder();
	}

	@DisplayName( "Returning false from an SSE callback closes the connection immediately" )
	@Test
	public void testSseEarlyStopClosesConnection() throws Exception {
		long			start	= System.nanoTime();
		AtomicInteger	events	= new AtomicInteger();

		// The server would wait up to 20s before the next event. A prompt return proves we hung up.
		long			result	= BinaryStreamClient.streamSsePost( baseURL + "/sse-slow", Map.of(), "{}", 30, ( IStruct event ) -> {
									events.incrementAndGet();
									return false;
								} );

		long			millis	= TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start );
		assertThat( result ).isEqualTo( 1L );
		assertThat( events.get() ).isEqualTo( 1 );
		assertThat( millis ).isLessThan( 5000L );
	}
}
