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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Streams an HTTP response body to a callback as it arrives, for binary audio and Server-Sent Events.
 * <p>
 * The BoxLang runtime HTTP client reads non-SSE streaming responses line by line as text, which corrupts binary
 * payloads such as audio, and its SSE mode cannot be cancelled once started. This helper reads the raw response
 * stream instead, so providers that stream audio can be restreamed without buffering the whole response, and a
 * caller that stops early (for example a voice agent being interrupted) closes the connection immediately.
 * <p>
 * <strong>Timeouts:</strong> the timeout is an <em>idle</em> timeout. It bounds the wait for the response headers
 * and the gap between received bytes, in both the success and error paths. A stalled provider is aborted instead
 * of blocking forever, while a healthy stream of any length is never cut off.
 */
public class BinaryStreamClient {

	/**
	 * Default read buffer size in bytes
	 */
	public static final int							DEFAULT_BUFFER_SIZE		= 4096;

	/**
	 * Default idle timeout in seconds
	 */
	public static final int							DEFAULT_TIMEOUT_SECONDS	= 30;

	/**
	 * Shared client, HTTP/2 negotiated where available. Honors the JVM default proxy selector.
	 */
	private static final HttpClient					CLIENT					= HttpClient.newBuilder()
	    .connectTimeout( Duration.ofSeconds( 30 ) )
	    .followRedirects( HttpClient.Redirect.NORMAL )
	    .build();

	/**
	 * Single daemon thread that closes stalled streams
	 */
	private static final ScheduledExecutorService	WATCHDOG				= Executors.newSingleThreadScheduledExecutor( runnable -> {
																				Thread thread = new Thread( runnable, "bxai-stream-watchdog" );
																				thread.setDaemon( true );
																				return thread;
																			} );

	private BinaryStreamClient() {
	}

	/**
	 * BoxLang structs hand us Key objects rather than plain strings as map keys
	 */
	private static String keyName( Object key ) {
		return key instanceof Key boxKey ? boxKey.getName() : String.valueOf( key );
	}

	/**
	 * Build the POST request
	 */
	private static HttpRequest buildRequest( String url, Map<?, ?> headers, String body, int timeoutSeconds ) {
		HttpRequest.Builder builder = HttpRequest.newBuilder( URI.create( url ) )
		    // Bounds the wait for response headers. Body reads are bounded by the idle watchdog below.
		    .timeout( Duration.ofSeconds( timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS ) )
		    .POST( HttpRequest.BodyPublishers.ofString( body, StandardCharsets.UTF_8 ) );
		if ( headers != null ) {
			headers.forEach( ( k, v ) -> builder.header( keyName( k ), String.valueOf( v ) ) );
		}
		return builder.build();
	}

	/**
	 * Idle watchdog: closes the stream if no bytes arrive for the timeout. Closing unblocks the pending read.
	 */
	private static final class Watchdog implements AutoCloseable {

		private final AtomicLong			lastActivity	= new AtomicLong( System.nanoTime() );
		private final AtomicBoolean			timedOut		= new AtomicBoolean( false );
		private final ScheduledFuture<?>	task;
		private final long					timeoutNanos;
		private final int					timeoutSeconds;

		Watchdog( InputStream in, int timeoutSeconds ) {
			this.timeoutSeconds	= timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
			this.timeoutNanos	= TimeUnit.SECONDS.toNanos( this.timeoutSeconds );
			this.task			= WATCHDOG.scheduleAtFixedRate( () -> {
									if ( System.nanoTime() - lastActivity.get() > timeoutNanos && timedOut.compareAndSet( false, true ) ) {
										try {
											in.close();
										} catch ( IOException ignored ) {
											// Closing is best effort, the read fails either way
										}
									}
								}, 250, 250, TimeUnit.MILLISECONDS );
		}

		void touch() {
			lastActivity.set( System.nanoTime() );
		}

		/**
		 * Translate a read failure into a clear timeout error when the watchdog caused it
		 */
		IOException translate( IOException e ) {
			return timedOut.get()
			    ? new IOException( "Stream timed out after " + timeoutSeconds + "s without receiving data", e )
			    : e;
		}

		boolean timedOut() {
			return timedOut.get();
		}

		@Override
		public void close() {
			task.cancel( false );
		}
	}

	/**
	 * Send the request and return the response, or throw with the provider's error body for non-2xx statuses.
	 * The returned stream is guarded by the idle watchdog and must be closed by the caller.
	 */
	private static HttpResponse<InputStream> send( String url, Map<?, ?> headers, String body, int timeoutSeconds ) throws IOException, InterruptedException {
		return CLIENT.send( buildRequest( url, headers, body, timeoutSeconds ), HttpResponse.BodyHandlers.ofInputStream() );
	}

	/**
	 * Read an error body (bounded by the watchdog) and throw it as an IOException
	 */
	private static void throwHttpError( int status, InputStream in, Watchdog watchdog ) throws IOException {
		try {
			String errorBody = new String( in.readAllBytes(), StandardCharsets.UTF_8 );
			throw new IOException( "HTTP " + status + ": " + errorBody );
		} catch ( IOException e ) {
			if ( watchdog.timedOut() ) {
				throw watchdog.translate( e );
			}
			throw e;
		}
	}

	/**
	 * Execute a POST and push the response body to the callback in chunks.
	 *
	 * @param url            The URL to call
	 * @param headers        Request headers
	 * @param body           The request body (sent as UTF-8)
	 * @param timeoutSeconds Idle timeout in seconds: the longest wait for headers or between received bytes
	 * @param bufferSize     Max bytes per chunk, or 0 for the default
	 * @param onChunk        Receives each chunk. Return false to stop streaming early and close the connection
	 *
	 * @return The total number of bytes delivered to the callback
	 *
	 * @throws IOException          On network failure, a non-2xx status or an idle timeout
	 * @throws InterruptedException If the calling thread is interrupted
	 */
	public static long streamPost(
	    String url,
	    Map<?, ?> headers,
	    String body,
	    int timeoutSeconds,
	    int bufferSize,
	    Predicate<byte[]> onChunk ) throws IOException, InterruptedException {

		HttpResponse<InputStream> response = send( url, headers, body, timeoutSeconds );

		try ( InputStream in = response.body(); Watchdog watchdog = new Watchdog( in, timeoutSeconds ) ) {
			int status = response.statusCode();
			if ( status < 200 || status > 299 ) {
				throwHttpError( status, in, watchdog );
			}

			byte[]	buffer	= new byte[ bufferSize > 0 ? bufferSize : DEFAULT_BUFFER_SIZE ];
			long	total	= 0;
			int		read;
			try {
				// InputStream.read returns as soon as any bytes are available, which gives us progressive delivery
				while ( ( read = in.read( buffer ) ) != -1 ) {
					watchdog.touch();
					if ( read == 0 ) {
						continue;
					}
					total += read;
					if ( !onChunk.test( Arrays.copyOf( buffer, read ) ) ) {
						break;
					}
				}
			} catch ( IOException e ) {
				throw watchdog.translate( e );
			}
			return total;
		}
	}

	/**
	 * Execute a POST to a Server-Sent Events endpoint and push each event to the callback.
	 * Returning false from the callback closes the connection immediately, which stops the provider generating.
	 *
	 * @param url            The URL to call
	 * @param headers        Request headers
	 * @param body           The request body (sent as UTF-8)
	 * @param timeoutSeconds Idle timeout in seconds: the longest wait for headers or between received bytes
	 * @param onEvent        Receives each event as a struct with `data`, `event` and `id`. Return false to stop early
	 *
	 * @return The number of events delivered to the callback
	 *
	 * @throws IOException          On network failure, a non-2xx status or an idle timeout
	 * @throws InterruptedException If the calling thread is interrupted
	 */
	public static long streamSsePost(
	    String url,
	    Map<?, ?> headers,
	    String body,
	    int timeoutSeconds,
	    Predicate<IStruct> onEvent ) throws IOException, InterruptedException {

		HttpResponse<InputStream> response = send( url, headers, body, timeoutSeconds );

		try ( InputStream in = response.body(); Watchdog watchdog = new Watchdog( in, timeoutSeconds ) ) {
			int status = response.statusCode();
			if ( status < 200 || status > 299 ) {
				throwHttpError( status, in, watchdog );
			}

			BufferedReader	reader		= new BufferedReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) );
			StringBuilder	data		= new StringBuilder();
			String			eventName	= "";
			String			eventId		= "";
			boolean			hasData		= false;
			long			delivered	= 0;

			try {
				String line;
				while ( ( line = reader.readLine() ) != null ) {
					watchdog.touch();

					// A blank line dispatches the accumulated event
					if ( line.isEmpty() ) {
						if ( hasData ) {
							delivered++;
							IStruct event = Struct.of( "data", data.toString(), "event", eventName, "id", eventId );
							if ( !onEvent.test( event ) ) {
								return delivered;
							}
						}
						data.setLength( 0 );
						eventName	= "";
						eventId		= "";
						hasData		= false;
						continue;
					}

					// Comment / keep-alive
					if ( line.startsWith( ":" ) ) {
						continue;
					}

					int		colon	= line.indexOf( ':' );
					String	field	= colon < 0 ? line : line.substring( 0, colon );
					String	value	= colon < 0 ? "" : line.substring( colon + 1 );
					if ( value.startsWith( " " ) ) {
						value = value.substring( 1 );
					}

					switch ( field ) {
						case "data" :
							if ( hasData ) {
								data.append( '\n' );
							}
							data.append( value );
							hasData = true;
							break;
						case "event" :
							eventName = value;
							break;
						case "id" :
							eventId = value;
							break;
						default :
							// retry and unknown fields are ignored
							break;
					}
				}

				// Stream ended without a trailing blank line: deliver what is pending
				if ( hasData ) {
					delivered++;
					onEvent.test( Struct.of( "data", data.toString(), "event", eventName, "id", eventId ) );
				}
			} catch ( IOException e ) {
				throw watchdog.translate( e );
			}
			return delivered;
		}
	}

}
