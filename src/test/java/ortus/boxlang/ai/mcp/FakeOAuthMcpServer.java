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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A small MCP server that is protected the way the MCP authorization specification says, together with the authorization server that protects
 * it, on one local port: Protected Resource Metadata, authorization server metadata, dynamic client registration, a token endpoint with PKCE
 * and refresh tokens, and an MCP endpoint that answers 401 until it gets a valid bearer token.
 *
 * Tests set the public flags to make it behave in different ways and read the lists to see what the client sent.
 */
public class FakeOAuthMcpServer {

	private final HttpServer						server;
	private final int								port;

	/** Seconds the access tokens it issues are good for */
	public volatile int								tokenLifetime		= 3600;
	/** Whether a refresh gives a new refresh token (true) or keeps the old one (false) */
	public volatile boolean							rotateRefresh		= true;
	/** Whether the authorization server offers dynamic client registration */
	public volatile boolean							dcr					= true;
	/** Whether the authorization server says it supports Client ID Metadata Documents */
	public volatile boolean							cimd				= false;
	/** Whether the authorization server says it sends the issuer back (RFC 9207) */
	public volatile boolean							issParameter		= false;
	/** Whether it leaves the S256 method out of the PKCE methods it lists */
	public volatile boolean							noPkce				= false;
	/** Whether every refresh is refused with invalid_grant */
	public volatile boolean							refuseRefresh		= false;
	/** Whether the MCP server also answers the handshake with a session id */
	public volatile boolean							sessions			= false;
	/** Whether the MCP endpoint refuses every token, even one it issued */
	public volatile boolean							rejectAllTokens		= false;
	/** Whether the MCP endpoint answers 403 insufficient_scope for a valid token */
	public volatile boolean							insufficientScope	= false;

	/** The JSON bodies of the registration requests */
	public final List<String>						registrations		= new CopyOnWriteArrayList<>();
	/** The form of every token request, as a map */
	public final List<Map<String, String>>			tokenRequests		= new CopyOnWriteArrayList<>();
	/** Every call to the MCP endpoint as method|bearer token|session id */
	public final List<String>						mcpCalls			= new CopyOnWriteArrayList<>();

	private final AtomicInteger						counter				= new AtomicInteger();
	private final Map<String, Map<String, String>>	codes				= new ConcurrentHashMap<>();
	private final Set<String>						accessTokens		= ConcurrentHashMap.newKeySet();
	private final Set<String>						refreshTokens		= ConcurrentHashMap.newKeySet();
	private final Set<String>						clients				= ConcurrentHashMap.newKeySet();

	/**
	 * Start the server on a free local port. Its endpoints are listed in the class comment.
	 *
	 * @throws IOException when the port cannot be opened
	 */
	public FakeOAuthMcpServer() throws IOException {
		this.server	= HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		this.port	= this.server.getAddress().getPort();

		this.server.createContext( "/mcp", this::mcp );
		this.server.createContext( "/.well-known/oauth-protected-resource", exchange -> {
			String doc = "{\"resource\":\"" + resource() + "\",\"authorization_servers\":[\"" + base()
			    + "\"],\"scopes_supported\":[\"mail.read\",\"mail.send\"]}";
			json( exchange, 200, doc );
		} );
		this.server.createContext( "/.well-known/oauth-authorization-server", exchange -> {
			String doc = "{\"issuer\":\"" + base() + "\",\"authorization_endpoint\":\"" + base() + "/authorize\",\"token_endpoint\":\"" + base()
			    + "/token\",\"code_challenge_methods_supported\":" + ( this.noPkce ? "[\"plain\"]" : "[\"S256\"]" )
			    + ( this.dcr ? ",\"registration_endpoint\":\"" + base() + "/register\"" : "" )
			    + ",\"client_id_metadata_document_supported\":" + this.cimd
			    + ",\"authorization_response_iss_parameter_supported\":" + this.issParameter + "}";
			json( exchange, 200, doc );
		} );
		this.server.createContext( "/register", exchange -> {
			String body = new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 );
			this.registrations.add( body );
			String id = "client-" + this.counter.incrementAndGet();
			this.clients.add( id );
			json( exchange, 201, "{\"client_id\":\"" + id + "\"}" );
		} );
		this.server.createContext( "/token", this::token );
		this.server.start();
	}

	/**
	 * @return The address of the server, for example http://127.0.0.1:41233 (also the issuer of its authorization server)
	 */
	public String base() {
		return "http://127.0.0.1:" + this.port;
	}

	/**
	 * @return The address of the protected MCP endpoint, which is also the canonical resource
	 */
	public String resource() {
		return base() + "/mcp";
	}

	/**
	 * Stop the server.
	 */
	public void stop() {
		this.server.stop( 0 );
	}

	/**
	 * Play the person who signs in: check the authorization request the client built and hand back the code and the state it would be sent to
	 * the callback with.
	 *
	 * @return code, state and (when the server sends it) iss
	 */
	public Map<String, String> authorize( String authorizationUrl ) {
		Map<String, String> query = parseForm( URI.create( authorizationUrl ).getRawQuery() );
		for ( String required : List.of( "response_type", "client_id", "redirect_uri", "state", "code_challenge", "code_challenge_method", "resource" ) ) {
			if ( !query.containsKey( required ) ) {
				throw new IllegalStateException( "The authorization request has no " + required + ": " + authorizationUrl );
			}
		}
		if ( !query.get( "code_challenge_method" ).equals( "S256" ) || !query.get( "response_type" ).equals( "code" ) ) {
			throw new IllegalStateException( "Bad PKCE or response type: " + authorizationUrl );
		}
		String code = "code-" + this.counter.incrementAndGet();
		this.codes.put( code, new HashMap<>( query ) );
		Map<String, String> answer = new LinkedHashMap<>();
		answer.put( "code", code );
		answer.put( "state", query.get( "state" ) );
		if ( this.issParameter ) {
			answer.put( "iss", base() );
		}
		return answer;
	}

	/** Make every access token stop working, as a revocation would, but leave the refresh tokens valid */
	public void revokeAccessTokens() {
		this.accessTokens.clear();
	}

	// -------------------------------------------------------------------------
	// Endpoints
	// -------------------------------------------------------------------------

	/**
	 * The protected MCP endpoint: 401 with a WWW-Authenticate challenge until the bearer token is one it issued, then answers initialize, tools/list and
	 * tools/call.
	 *
	 * @param exchange The request
	 *
	 * @throws IOException when the answer cannot be written
	 */
	private void mcp( HttpExchange exchange ) throws IOException {
		String	body	= new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 );
		String	auth	= exchange.getRequestHeaders().getFirst( "Authorization" );
		String	token	= auth != null && auth.startsWith( "Bearer " ) ? auth.substring( 7 ) : "";
		String	session	= exchange.getRequestHeaders().getFirst( "Mcp-Session-Id" );
		String	method	= body.contains( "\"method\":\"tools/call\"" ) ? "tools/call"
		    : body.contains( "\"method\":\"tools/list\"" ) ? "tools/list" : body.contains( "\"method\":\"initialize\"" ) ? "initialize" : "other";
		this.mcpCalls.add( method + "|" + token + "|" + ( session == null ? "" : session ) );

		if ( this.insufficientScope && this.accessTokens.contains( token ) ) {
			exchange.getResponseHeaders().add( "WWW-Authenticate",
			    "Bearer error=\"insufficient_scope\", scope=\"mail.send\", resource_metadata=\"" + base() + "/.well-known/oauth-protected-resource/mcp\"" );
			json( exchange, 403, "{\"error\":\"insufficient_scope\"}" );
			return;
		}
		if ( this.rejectAllTokens || !this.accessTokens.contains( token ) ) {
			exchange.getResponseHeaders().add( "WWW-Authenticate",
			    "Bearer resource_metadata=\"" + base() + "/.well-known/oauth-protected-resource/mcp\", scope=\"mail.read\"" );
			json( exchange, 401, "{\"error\":\"invalid_token\"}" );
			return;
		}
		String reply = switch ( method ) {
			case "initialize" -> "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"capabilities\":{}}}";
			case "tools/list" ->
			    "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[{\"name\":\"inbox\",\"description\":\"Read mail\",\"inputSchema\":{\"type\":\"object\",\"properties\":{}}}]}}";
			case "tools/call" -> "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"inbox read with " + token + "\"}]}}";
			default -> "{}";
		};
		if ( method.equals( "initialize" ) && this.sessions ) {
			exchange.getResponseHeaders().add( "Mcp-Session-Id", "sess-" + this.counter.incrementAndGet() );
		}
		json( exchange, 200, reply );
	}

	/**
	 * The token endpoint: trades a code (checking the PKCE verifier, redirect address, client and resource) or a refresh token for new tokens.
	 *
	 * @param exchange The request
	 *
	 * @throws IOException when the answer cannot be written
	 */
	private void token( HttpExchange exchange ) throws IOException {
		Map<String, String> form = parseForm( new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 ) );
		this.tokenRequests.add( form );

		if ( !resource().equals( form.get( "resource" ) ) ) {
			json( exchange, 400, "{\"error\":\"invalid_target\"}" );
			return;
		}
		String grant = form.getOrDefault( "grant_type", "" );
		if ( grant.equals( "authorization_code" ) ) {
			Map<String, String> issued = this.codes.remove( form.get( "code" ) );
			if ( issued == null || !issued.get( "client_id" ).equals( form.get( "client_id" ) )
			    || !issued.get( "redirect_uri" ).equals( form.get( "redirect_uri" ) )
			    || !challengeOf( form.getOrDefault( "code_verifier", "" ) ).equals( issued.get( "code_challenge" ) ) ) {
				json( exchange, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"code or verifier not accepted\"}" );
				return;
			}
			String refresh = "rt-" + this.counter.incrementAndGet();
			this.refreshTokens.add( refresh );
			json( exchange, 200, tokenAnswer( refresh ) );
			return;
		}
		if ( grant.equals( "refresh_token" ) ) {
			String old = form.getOrDefault( "refresh_token", "" );
			if ( this.refuseRefresh || !this.refreshTokens.contains( old ) ) {
				json( exchange, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"refresh token not accepted\"}" );
				return;
			}
			String refresh = null;
			if ( this.rotateRefresh ) {
				this.refreshTokens.remove( old );
				refresh = "rt-" + this.counter.incrementAndGet();
				this.refreshTokens.add( refresh );
			}
			json( exchange, 200, tokenAnswer( refresh ) );
			return;
		}
		json( exchange, 400, "{\"error\":\"unsupported_grant_type\"}" );
	}

	/**
	 * Issue a new access token and build the token endpoint's JSON answer.
	 *
	 * @param refresh The refresh token to send back, or null to send none (a refresh that does not rotate)
	 *
	 * @return The JSON text
	 */
	private String tokenAnswer( String refresh ) {
		String access = "at-" + this.counter.incrementAndGet();
		this.accessTokens.add( access );
		return "{\"access_token\":\"" + access + "\",\"token_type\":\"Bearer\",\"expires_in\":" + this.tokenLifetime + ",\"scope\":\"mail.read\""
		    + ( refresh != null ? ",\"refresh_token\":\"" + refresh + "\"" : "" ) + "}";
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * The S256 code challenge of a PKCE verifier: base64url( SHA-256( verifier ) ).
	 *
	 * @param verifier The code verifier the client sent
	 *
	 * @return The code challenge
	 */
	private static String challengeOf( String verifier ) {
		try {
			byte[] digest = MessageDigest.getInstance( "SHA-256" ).digest( verifier.getBytes( StandardCharsets.US_ASCII ) );
			return Base64.getUrlEncoder().withoutPadding().encodeToString( digest );
		} catch ( Exception e ) {
			throw new IllegalStateException( e );
		}
	}

	/**
	 * Read a form or a query string into a map, decoding the values.
	 *
	 * @param form The text, such as a=1&b=2
	 *
	 * @return The fields in the order they came
	 */
	private static Map<String, String> parseForm( String form ) {
		Map<String, String> out = new LinkedHashMap<>();
		if ( form == null || form.isEmpty() ) {
			return out;
		}
		for ( String pair : form.split( "&" ) ) {
			int		eq		= pair.indexOf( '=' );
			String	key		= URLDecoder.decode( eq < 0 ? pair : pair.substring( 0, eq ), StandardCharsets.UTF_8 );
			String	value	= eq < 0 ? "" : URLDecoder.decode( pair.substring( eq + 1 ), StandardCharsets.UTF_8 );
			out.put( key, value );
		}
		return out;
	}

	/**
	 * Send a JSON answer and close the exchange.
	 *
	 * @param exchange The request
	 * @param status   The HTTP status
	 * @param body     The JSON text
	 *
	 * @throws IOException when the answer cannot be written
	 */
	private static void json( HttpExchange exchange, int status, String body ) throws IOException {
		byte[] bytes = body.getBytes( StandardCharsets.UTF_8 );
		exchange.getResponseHeaders().add( "Content-Type", "application/json" );
		exchange.sendResponseHeaders( status, bytes.length );
		exchange.getResponseBody().write( bytes );
		exchange.close();
	}

}
