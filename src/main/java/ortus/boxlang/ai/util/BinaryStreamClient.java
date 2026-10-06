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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

import ortus.boxlang.runtime.scopes.Key;

/**
 * Streams a binary HTTP response body to a callback as the bytes arrive.
 * <p>
 * The BoxLang runtime HTTP client reads non-SSE streaming responses line by line as text, which corrupts binary
 * payloads such as audio. This helper reads the raw response stream instead, so providers that stream audio
 * bytes (chunked transfer) can be restreamed without buffering the whole response.
 */
public class BinaryStreamClient {

	/**
	 * Default read buffer size in bytes
	 */
	public static final int			DEFAULT_BUFFER_SIZE	= 4096;

	/**
	 * Shared client, HTTP/2 negotiated where available. Honors the JVM default proxy selector.
	 */
	private static final HttpClient	CLIENT				= HttpClient.newBuilder()
	    .connectTimeout( Duration.ofSeconds( 30 ) )
	    .followRedirects( HttpClient.Redirect.NORMAL )
	    .build();

	private BinaryStreamClient() {
	}

	/**
	 * BoxLang structs hand us Key objects rather than plain strings as map keys
	 */
	private static String keyName( Object key ) {
		return key instanceof Key boxKey ? boxKey.getName() : String.valueOf( key );
	}

	/**
	 * Execute a POST and push the response body to the callback in chunks.
	 *
	 * @param url            The URL to call
	 * @param headers        Request headers
	 * @param body           The request body (sent as UTF-8)
	 * @param timeoutSeconds Overall request timeout in seconds
	 * @param bufferSize     Max bytes per chunk, or 0 for the default
	 * @param onChunk        Receives each chunk. Return false to stop streaming early (e.g. barge-in)
	 *
	 * @return The total number of bytes delivered to the callback
	 *
	 * @throws IOException          On network failure
	 * @throws InterruptedException If the calling thread is interrupted
	 */
	public static long streamPost(
	    String url,
	    Map<?, ?> headers,
	    String body,
	    int timeoutSeconds,
	    int bufferSize,
	    Predicate<byte[]> onChunk ) throws IOException, InterruptedException {

		HttpRequest.Builder builder = HttpRequest.newBuilder( URI.create( url ) )
		    .timeout( Duration.ofSeconds( timeoutSeconds > 0 ? timeoutSeconds : 30 ) )
		    .POST( HttpRequest.BodyPublishers.ofString( body, StandardCharsets.UTF_8 ) );
		if ( headers != null ) {
			headers.forEach( ( k, v ) -> builder.header( keyName( k ), String.valueOf( v ) ) );
		}

		HttpResponse<InputStream>	response	= CLIENT.send( builder.build(), HttpResponse.BodyHandlers.ofInputStream() );
		int							status		= response.statusCode();

		try ( InputStream in = response.body() ) {
			// Surface provider errors with their body so callers can build a meaningful message
			if ( status < 200 || status > 299 ) {
				String errorBody = new String( in.readAllBytes(), StandardCharsets.UTF_8 );
				throw new IOException( "HTTP " + status + ": " + errorBody );
			}

			byte[]	buffer	= new byte[ bufferSize > 0 ? bufferSize : DEFAULT_BUFFER_SIZE ];
			long	total	= 0;
			int		read;
			// InputStream.read returns as soon as any bytes are available, which gives us progressive delivery
			while ( ( read = in.read( buffer ) ) != -1 ) {
				if ( read == 0 ) {
					continue;
				}
				total += read;
				if ( !onChunk.test( Arrays.copyOf( buffer, read ) ) ) {
					break;
				}
			}
			return total;
		}
	}

}
