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
 * The client options that let a caller control what an MCP client may reach: URL guard, redirects, response size, name prefix, tool filter and
 * the initialize handshake. A small local HTTP server stands in for the MCP server, so no network is needed.
 */
public class mcpClientHooksTest extends BaseIntegrationTest {

	private HttpServer			server;
	private int					port;
	private final List<String>	requests	= new CopyOnWriteArrayList<>();
	private final List<String>	sessionIds	= new CopyOnWriteArrayList<>();

	@BeforeEach
	public void startServer() throws IOException {
		this.server	= HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		this.port	= this.server.getAddress().getPort();

		this.server.createContext( "/mcp", exchange -> {
			String	body	= new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 );
			String	method	= body.replaceAll( "(?s).*\"method\"\\s*:\\s*\"([^\"]+)\".*", "$1" );
			this.requests.add( method );
			this.sessionIds.add( exchange.getRequestHeaders().getFirst( "Mcp-Session-Id" ) == null ? ""
			    : exchange.getRequestHeaders().getFirst( "Mcp-Session-Id" ) );
			String	reply	= switch ( method ) {
								case "initialize" -> "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"capabilities\":{}}}";
								case "notifications/initialized" -> "{}";
								case "tools/list" ->
								    "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[{\"name\":\"search\",\"description\":\"s\",\"inputSchema\":{}},{\"name\":\"sendFeedback\",\"description\":\"f\",\"inputSchema\":{}}]}}";
								default -> "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}}";
							};
			byte[]	bytes	= reply.getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().add( "Content-Type", "application/json" );
			if ( method.equals( "initialize" ) ) {
				exchange.getResponseHeaders().add( "Mcp-Session-Id", "sess-42" );
			}
			exchange.sendResponseHeaders( 200, bytes.length );
			exchange.getResponseBody().write( bytes );
			exchange.close();
		} );

		this.server.createContext( "/redirect", exchange -> {
			this.requests.add( "redirect-hit" );
			exchange.getResponseHeaders().add( "Location", "http://127.0.0.1:" + this.port + "/mcp" );
			exchange.sendResponseHeaders( 302, -1 );
			exchange.close();
		} );

		this.server.createContext( "/big", exchange -> {
			byte[] bytes = "x".repeat( 5000 ).getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().add( "Content-Type", "text/plain" );
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
	@DisplayName( "A URL guard that returns false refuses the call and nothing is sent" )
	public void testGuardRefuses() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = MCP( "%s/mcp" ).withUrlGuard( ( url ) => false ).listTools()
				ok = response.isSuccess()
				error = response.getError()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( false );
		assertThat( ( String ) variables.get( Key.of( "error" ) ) ).contains( "refused" );
		assertThat( this.requests ).isEmpty();
	}

	@Test
	@DisplayName( "A URL guard that throws refuses the call with its message" )
	public void testGuardThrows() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = MCP( "%s/mcp" ).withUrlGuard( ( url ) => { throw( "Blocked: private address" ) } ).listTools()
				error = response.getError()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( ( String ) variables.get( Key.of( "error" ) ) ).contains( "Blocked: private address" );
		assertThat( this.requests ).isEmpty();
	}

	@Test
	@DisplayName( "A URL guard that allows the URL lets the call through" )
	public void testGuardAllows() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = MCP( "%s/mcp" ).withUrlGuard( ( url ) => true ).listTools()
				ok = response.isSuccess()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( true );
		assertThat( this.requests ).containsExactly( "tools/list" );
	}

	@Test
	@DisplayName( "Redirects are followed by default" )
	public void testRedirectFollowedByDefault() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = MCP( "%s/redirect" ).listTools()
				ok = response.isSuccess()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( true );
		assertThat( this.requests ).contains( "redirect-hit" );
	}

	@Test
	@DisplayName( "withRedirects(false) turns a redirect into a failed response and never calls the new address" )
	public void testRedirectRefused() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = MCP( "%s/redirect" ).withRedirects( false ).listTools()
				ok = response.isSuccess()
				status = response.getStatusCode()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( false );
		assertThat( ( ( Number ) variables.get( Key.of( "status" ) ) ).intValue() ).isEqualTo( 302 );
		assertThat( this.requests ).containsExactly( "redirect-hit" );
	}

	@Test
	@DisplayName( "withMaxResponseBytes refuses an oversized response" )
	public void testMaxResponseBytes() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = MCP( "%s/big" ).withMaxResponseBytes( 1000 ).listTools()
				ok = response.isSuccess()
				error = response.getError()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( false );
		assertThat( ( String ) variables.get( Key.of( "error" ) ) ).contains( "larger than 1000" );
	}

	@Test
	@DisplayName( "A response under the limit is accepted" )
	public void testMaxResponseBytesUnderLimit() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = MCP( "%s/mcp" ).withMaxResponseBytes( 100000 ).listTools()
				ok = response.isSuccess()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "The handshake sends initialize first and then sends back the session id" )
	public void testHandshake() {
		// @formatter:off
		runtime.executeSource(
			"""
				client = MCP( "%s/mcp" ).withHandshake()
				response = client.listTools()
				ok = response.isSuccess()
				session = client.getSessionId()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "session" ) ) ).isEqualTo( "sess-42" );
		assertThat( this.requests ).containsExactly( "initialize", "notifications/initialized", "tools/list" ).inOrder();
		assertThat( this.sessionIds.get( 0 ) ).isEmpty();
		assertThat( this.sessionIds.get( 2 ) ).isEqualTo( "sess-42" );
	}

	@Test
	@DisplayName( "Without the handshake no initialize is sent" )
	public void testNoHandshakeByDefault() {
		// @formatter:off
		runtime.executeSource(
			"""
				MCP( "%s/mcp" ).listTools()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( this.requests ).containsExactly( "tools/list" );
	}

	@Test
	@DisplayName( "A prefix names tools prefix__name and the server still receives the original name" )
	public void testPrefix() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel( "openai", { apiKey: "x" } ).withMCPServer( "%s/mcp", { prefix: "boxlang" } )
				names = model.listTools().map( t => t.name ?: t )
				result = model.getTools().filter( t => t.getName() == "boxlang__search" )[ 1 ].invoke( { query: "hi" } )
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "names" ) ).toString() ).contains( "boxlang__search" );
		assertThat( variables.get( Key.of( "names" ) ).toString() ).contains( "boxlang__sendFeedback" );
		assertThat( this.requests ).contains( "tools/call" );
	}

	@Test
	@DisplayName( "A tool filter exposes only the named tools" )
	public void testToolFilter() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel( "openai", { apiKey: "x" } ).withMCPServer( "%s/mcp", { toolFilter: [ "search" ] } )
				names = model.listTools().map( t => t.name ?: t )
			""".formatted( base() ),
			context
		);
		// @formatter:on

		String names = variables.get( Key.of( "names" ) ).toString();
		assertThat( names ).contains( "search" );
		assertThat( names ).doesNotContain( "sendFeedback" );
	}

	@Test
	@DisplayName( "Hooks in the config struct of withMCPServer reach the client" )
	public void testConfigHooks() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel( "openai", { apiKey: "x" } ).withMCPServer( "%s/mcp", { urlGuard: ( url ) => false } )
				count = model.listMCPServers().len()
			""".formatted( base() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "count" ) ) ).isEqualTo( 0 );
		assertThat( this.requests ).isEmpty();
	}
}
