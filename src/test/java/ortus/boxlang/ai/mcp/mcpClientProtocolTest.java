/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.ai.mcp;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * How the MCP client talks to a server: its timeout is in milliseconds, an event stream is read for the JSON-RPC answer and not the first
 * message, and tools can be listed page by page. A small local HTTP server stands in for the MCP server.
 */
public class mcpClientProtocolTest extends BaseIntegrationTest {

	private HttpServer			server;
	private int					port;
	private final List<String>	bodies	= new CopyOnWriteArrayList<>();

	@BeforeEach
	public void startServer() throws IOException {
		this.server	= HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		this.port	= this.server.getAddress().getPort();

		this.server.createContext( "/slow", exchange -> {
			try {
				Thread.sleep( 4000 );
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
			try {
				exchange.sendResponseHeaders( 200, -1 );
			} catch ( IOException e ) {
				// The client gave up first, which is what the test wants
			}
			exchange.close();
		} );

		// An event stream that sends a notification before the answer
		this.server.createContext( "/stream", exchange -> {
			String	notice	= "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/progress\",\"params\":{\"progress\":1}}";
			String	answer	= "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[{\"name\":\"search\",\"description\":\"s\",\"inputSchema\":{}}]}}";
			byte[]	bytes	= ( "event: message\ndata: " + notice + "\n\nevent: message\ndata: " + answer + "\n\n" ).getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().add( "Content-Type", "text/event-stream" );
			exchange.sendResponseHeaders( 200, bytes.length );
			exchange.getResponseBody().write( bytes );
			exchange.close();
		} );

		// A server that pages its tools: the first page has a nextCursor, the second (asked with that cursor) does not
		this.server.createContext( "/paged", exchange -> {
			String body = new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 );
			this.bodies.add( body );
			String reply = body.contains( "\"cursor\":\"page2\"" )
			    ? "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[{\"name\":\"second\",\"inputSchema\":{}}]}}"
			    : "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[{\"name\":\"first\",\"inputSchema\":{}}],\"nextCursor\":\"page2\"}}";
			byte[] bytes = reply.getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().add( "Content-Type", "application/json" );
			exchange.sendResponseHeaders( 200, bytes.length );
			exchange.getResponseBody().write( bytes );
			exchange.close();
		} );
		this.server.start();
	}

	@AfterEach
	public void stopServer() {
		this.server.stop( 0 );
	}

	private String base() {
		return "http://127.0.0.1:" + this.port;
	}

	@Test
	@DisplayName( "withTimeout is in milliseconds: 1000 gives up after about a second, not after 1000 seconds" )
	public void testTimeoutIsMilliseconds() {
		// @formatter:off
		runtime.executeSource(
			"""
				started = getTickCount()
				response = MCP( "%s/slow" ).withTimeout( 1000 ).listTools()
				elapsed = getTickCount() - started
				ok = response.isSuccess()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( false );
		assertThat( ( ( Number ) variables.get( Key.of( "elapsed" ) ) ).longValue() ).isLessThan( 3500L );
	}

	@Test
	@DisplayName( "An event stream is read for the JSON-RPC answer, a notification before it is skipped" )
	public void testEventStreamNotification() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = MCP( "%s/stream" ).listTools()
				ok = response.isSuccess()
				names = response.getData().tools.map( t => t.name )
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "names" ) ).toString() ).contains( "search" );
	}

	@Test
	@DisplayName( "listTools( cursor ) asks for the next page" )
	public void testListToolsCursor() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "%s/paged" )
				first = client.listTools()
				cursor = first.getData().nextCursor
				second = client.listTools( cursor )
				firstName = first.getData().tools[ 1 ].name
				secondName = second.getData().tools[ 1 ].name
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "cursor" ) ) ).isEqualTo( "page2" );
		assertThat( variables.get( Key.of( "firstName" ) ) ).isEqualTo( "first" );
		assertThat( variables.get( Key.of( "secondName" ) ) ).isEqualTo( "second" );
		assertThat( this.bodies.get( 0 ) ).doesNotContain( "cursor" );
	}
}
