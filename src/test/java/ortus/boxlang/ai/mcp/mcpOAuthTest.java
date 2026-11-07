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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * OAuth for an HTTP MCP client, against a local server that is protected the way the MCP authorization specification says
 * (see FakeOAuthMcpServer): discovery, client registration, PKCE, the sign in, the token on every request, refresh, and
 * the people who must sign in again.
 */
public class mcpOAuthTest extends BaseIntegrationTest {

	private FakeOAuthMcpServer fake;

	/**
	 * Start a fresh fake OAuth protected MCP server for each test.
	 *
	 * @throws IOException when the port cannot be opened
	 */
	@BeforeEach
	public void startFake() throws IOException {
		this.fake = new FakeOAuthMcpServer();
	}

	/**
	 * Stop the fake server after each test.
	 */
	@AfterEach
	public void stopFake() {
		this.fake.stop();
	}

	/** A client for the fake server, with the options a test adds to the defaults */
	private void makeClient( String extraOptions ) {
		// @formatter:off
		runtime.executeSource(
			"""
				mcpClient = MCP( "%s/mcp" ).withOAuth( {
					redirectUri : "http://127.0.0.1:9/callback"%s
				} )
			""".formatted( this.fake.base(), extraOptions ),
			context
		);
		// @formatter:on
	}

	/** Start a sign in for a person, play the person at the authorization server, and finish it at the callback */
	private void signIn( String subject ) {
		// @formatter:off
		runtime.executeSource(
			"""
				started = mcpClient.startAuthorization( "%s" )
				authUrl = started.authorizationUrl
			""".formatted( subject ),
			context
		);
		// @formatter:on
		Map<String, String> answer = this.fake.authorize( variables.getAsString( Key.of( "authUrl" ) ) );
		variables.put( Key.of( "cbCode" ), answer.get( "code" ) );
		variables.put( Key.of( "cbState" ), answer.get( "state" ) );
		variables.put( Key.of( "cbIss" ), answer.getOrDefault( "iss", "" ) );
		// @formatter:off
		runtime.executeSource( "signedIn = mcpClient.completeAuthorization( cbCode, cbState, cbIss )", context );
		// @formatter:on
	}

	@Test
	@DisplayName( "Starting a sign in finds the authorization server, registers the client and builds a PKCE authorization URL" )
	public void testStartBuildsTheAuthorizationUrl() {
		makeClient( "" );
		// @formatter:off
		runtime.executeSource(
			"""
				started = mcpClient.startAuthorization( "alice" )
				authUrl = started.authorizationUrl
				state   = started.state
			""",
			context
		);
		// @formatter:on

		String url = variables.getAsString( Key.of( "authUrl" ) );
		assertThat( url ).startsWith( this.fake.base() + "/authorize?" );
		assertThat( url ).contains( "response_type=code" );
		assertThat( url ).contains( "client_id=client-1" );
		assertThat( url ).contains( "code_challenge_method=S256" );
		assertThat( url ).contains( "redirect_uri=http%3A%2F%2F127.0.0.1%3A9%2Fcallback" );
		// the resource indicator names the MCP server (RFC 8707)
		assertThat( url ).contains( "resource=" + URLEncoder.encode( this.fake.resource(), StandardCharsets.UTF_8 ) );
		// no scope was configured and the server gave none: everything the resource supports
		assertThat( url ).contains( "scope=mail.read%20mail.send" );
		assertThat( variables.getAsString( Key.of( "state" ) ) ).hasLength( 43 );

		// the client registered itself once, as a native app because the redirect address is local
		assertThat( this.fake.registrations ).hasSize( 1 );
		assertThat( this.fake.registrations.get( 0 ) ).contains( "\"application_type\":\"native\"" );
		assertThat( this.fake.registrations.get( 0 ) ).contains( "\"token_endpoint_auth_method\":\"none\"" );
	}

	@Test
	@DisplayName( "After the sign in, requests carry the person's access token and the tools can be used" )
	public void testSignedInRequestsCarryTheToken() {
		makeClient( "" );
		signIn( "default" );

		// @formatter:off
		runtime.executeSource(
			"""
				authorized = mcpClient.isAuthorized( "default" )
				listed     = mcpClient.listTools()
				listedOk   = listed.isSuccess()
				called     = mcpClient.send( "inbox", {} )
				calledText = called.getData().content[ 1 ].text
				scope      = signedIn.scope
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "authorized" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "listedOk" ) ) ).isEqualTo( true );
		assertThat( variables.getAsString( Key.of( "calledText" ) ) ).startsWith( "inbox read with at-" );
		assertThat( variables.getAsString( Key.of( "scope" ) ) ).isEqualTo( "mail.read" );
		// the token request was a code exchange with the PKCE verifier and the resource
		Map<String, String> exchange = this.fake.tokenRequests.get( 0 );
		assertThat( exchange.get( "grant_type" ) ).isEqualTo( "authorization_code" );
		assertThat( exchange.get( "code_verifier" ) ).hasLength( 43 );
		assertThat( exchange.get( "resource" ) ).isEqualTo( this.fake.resource() );
	}

	@Test
	@DisplayName( "A call with no tokens never reaches the server, fails with a sign in message and fires onMCPAuthorizationRequired" )
	public void testNoTokensAsksForSignIn() {
		makeClient( "" );
		// @formatter:off
		runtime.executeSource(
			"""
				fired = []
				BoxRegisterInterceptor(
					( event ) => { fired.append( { subject: event.subject, reason: event.reason } ) },
					"onMCPAuthorizationRequired"
				)
				response = mcpClient.listTools()
				ok       = response.isSuccess()
				error    = response.getError()
				authorized = mcpClient.isAuthorized( "default" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( false );
		assertThat( variables.getAsString( Key.of( "error" ) ) ).contains( "Sign in" );
		assertThat( variables.get( Key.of( "authorized" ) ) ).isEqualTo( false );
		assertThat( this.fake.mcpCalls ).isEmpty();
		runtimeAssertFired( "no_token" );
	}

	/**
	 * Check that the onMCPAuthorizationRequired listener a test registered ran once, and with the given reason.
	 *
	 * @param reason The reason the event should have carried
	 */
	private void runtimeAssertFired( String reason ) {
		// @formatter:off
		runtime.executeSource( "firedCount = fired.len()\nfiredReason = fired.len() ? fired[ 1 ].reason : ''", context );
		// @formatter:on
		assertThat( variables.get( Key.of( "firedCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsString( Key.of( "firedReason" ) ) ).isEqualTo( reason );
	}

	@Test
	@DisplayName( "An access token that is about to expire is refreshed before it is used, and a rotated refresh token is kept" )
	public void testExpiredTokenIsRefreshed() {
		// a lifetime inside the refresh margin: the token counts as expired at once
		this.fake.tokenLifetime = 30;
		makeClient( "" );
		signIn( "default" );

		// @formatter:off
		runtime.executeSource(
			"""
				first  = mcpClient.send( "inbox", {} ).getData().content[ 1 ].text
				second = mcpClient.send( "inbox", {} ).getData().content[ 1 ].text
			""",
			context
		);
		// @formatter:on

		// the code exchange, then a refresh for each of the two calls
		assertThat( this.fake.tokenRequests ).hasSize( 3 );
		assertThat( this.fake.tokenRequests.get( 1 ).get( "grant_type" ) ).isEqualTo( "refresh_token" );
		assertThat( this.fake.tokenRequests.get( 1 ).get( "resource" ) ).isEqualTo( this.fake.resource() );
		// the second refresh used the refresh token the first one returned, not the original
		assertThat( this.fake.tokenRequests.get( 2 ).get( "refresh_token" ) ).isNotEqualTo( this.fake.tokenRequests.get( 1 ).get( "refresh_token" ) );
		assertThat( variables.getAsString( Key.of( "first" ) ) ).isNotEqualTo( variables.getAsString( Key.of( "second" ) ) );
	}

	@Test
	@DisplayName( "A refresh that does not rotate the refresh token keeps using the one it has" )
	public void testRefreshWithoutRotationKeepsTheRefreshToken() {
		this.fake.tokenLifetime	= 30;
		this.fake.rotateRefresh	= false;
		makeClient( "" );
		signIn( "default" );

		// @formatter:off
		runtime.executeSource( "mcpClient.send( 'inbox', {} )\nmcpClient.send( 'inbox', {} )", context );
		// @formatter:on

		assertThat( this.fake.tokenRequests.get( 1 ).get( "refresh_token" ) ).isEqualTo( this.fake.tokenRequests.get( 2 ).get( "refresh_token" ) );
	}

	@Test
	@DisplayName( "A refresh token that is refused ends the sign in: the tokens are deleted and the person must sign in again" )
	public void testRefusedRefreshNeedsSignIn() {
		this.fake.tokenLifetime = 30;
		makeClient( "" );
		signIn( "default" );
		this.fake.refuseRefresh = true;

		// @formatter:off
		runtime.executeSource(
			"""
				response   = mcpClient.send( "inbox", {} )
				ok         = response.isSuccess()
				error      = response.getError()
				authorized = mcpClient.isAuthorized( "default" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( false );
		assertThat( variables.getAsString( Key.of( "error" ) ) ).contains( "refuse" );
		assertThat( variables.get( Key.of( "authorized" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "A 401 for a token that looks valid refreshes it and tries once more" )
	public void testUnauthorizedTokenIsRefreshedAndRetried() {
		makeClient( "" );
		signIn( "default" );
		// the server forgets the access token (revoked, or the server restarted) but the refresh token still works
		this.fake.revokeAccessTokens();

		// @formatter:off
		runtime.executeSource(
			"""
				response = mcpClient.send( "inbox", {} )
				ok       = response.isSuccess()
				text     = ok ? response.getData().content[ 1 ].text : ""
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( true );
		// one call that was refused, one refresh, one call that worked
		assertThat( this.fake.mcpCalls ).hasSize( 2 );
		assertThat( this.fake.tokenRequests ).hasSize( 2 );
		assertThat( this.fake.tokenRequests.get( 1 ).get( "grant_type" ) ).isEqualTo( "refresh_token" );
	}

	@Test
	@DisplayName( "A token that is refused even after a refresh ends in a failed call and an unauthorized event, after one retry only" )
	public void testSecondUnauthorizedStops() {
		makeClient( "" );
		signIn( "default" );
		this.fake.rejectAllTokens = true;

		// @formatter:off
		runtime.executeSource(
			"""
				fired = []
				BoxRegisterInterceptor(
					( event ) => { fired.append( { reason: event.reason, subject: event.subject } ) },
					"onMCPAuthorizationRequired"
				)
				response = mcpClient.send( "inbox", {} )
				ok       = response.isSuccess()
				error    = response.getError()
				status   = response.getStatusCode()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "status" ) ).toString() ).isEqualTo( "401" );
		assertThat( variables.getAsString( Key.of( "error" ) ) ).contains( "did not accept" );
		// the first call, one refresh, and one retry: never a loop
		assertThat( this.fake.mcpCalls ).hasSize( 2 );
		assertThat( this.fake.tokenRequests ).hasSize( 2 );
		runtimeAssertFired( "unauthorized" );
	}

	@Test
	@DisplayName( "A 403 that asks for more scope fires the event with the scope and fails the call" )
	public void testInsufficientScope() {
		makeClient( "" );
		signIn( "default" );
		this.fake.insufficientScope = true;

		// @formatter:off
		runtime.executeSource(
			"""
				fired = []
				BoxRegisterInterceptor(
					( event ) => { fired.append( { reason: event.reason, scope: event.scope } ) },
					"onMCPAuthorizationRequired"
				)
				response = mcpClient.send( "inbox", {} )
				ok       = response.isSuccess()
				status   = response.getStatusCode()
				eventScope = fired.len() ? fired[ 1 ].scope : ""
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "ok" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "status" ) ).toString() ).isEqualTo( "403" );
		assertThat( variables.getAsString( Key.of( "eventScope" ) ) ).isEqualTo( "mail.send" );
		// not a reason to refresh or retry
		assertThat( this.fake.mcpCalls ).hasSize( 1 );
		runtimeAssertFired( "insufficient_scope" );
	}

	@Test
	@DisplayName( "People sign in on their own: the subject function picks the tokens from the chat request of the call" )
	public void testEachPersonHasTheirOwnTokens() {
		makeClient( ", subject: ( context ) => context.chatRequest.getUserId()" );
		signIn( "alice" );

		// @formatter:off
		runtime.executeSource(
			"""
				alice = { chatRequest: { getUserId: () => "alice" } }
				bob   = { chatRequest: { getUserId: () => "bob" } }
				aliceResponse = mcpClient.send( "inbox", {}, alice )
				bobResponse   = mcpClient.send( "inbox", {}, bob )
				aliceOk       = aliceResponse.isSuccess()
				bobOk         = bobResponse.isSuccess()
				aliceToken    = aliceOk ? aliceResponse.getData().content[ 1 ].text : ""
				aliceAuthorized = mcpClient.isAuthorized( "alice" )
				bobAuthorized   = mcpClient.isAuthorized( "bob" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "aliceOk" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "bobOk" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "aliceAuthorized" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "bobAuthorized" ) ) ).isEqualTo( false );

		// now Bob signs in: he gets a token of his own
		signIn( "bob" );
		// @formatter:off
		runtime.executeSource(
			"""
				bobAfter  = mcpClient.send( "inbox", {}, bob )
				bobText   = bobAfter.getData().content[ 1 ].text
				aliceText = mcpClient.send( "inbox", {}, alice ).getData().content[ 1 ].text
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "bobText" ) ) ).isNotEqualTo( variables.getAsString( Key.of( "aliceText" ) ) );
	}

	@Test
	@DisplayName( "MCP tools pass the chat request, so each person's tokens are used by the agent's tools" )
	public void testMcpToolUsesTheCallersTokens() {
		makeClient( ", subject: ( context ) => context.chatRequest.getUserId()" );
		signIn( "alice" );

		// @formatter:off
		runtime.executeSource(
			"""
				tool = new bxModules.bxai.models.tools.MCPTool(
					mcpClient,
					{ name: "inbox", description: "Read mail", inputSchema: { type: "object", properties: {} } },
					""
				)
				asAlice = new bxModules.bxai.models.requests.AiChatRequest( new bxModules.bxai.models.runnables.AiMessage(), {}, { userId: "alice" } )
				asBob   = new bxModules.bxai.models.requests.AiChatRequest( new bxModules.bxai.models.runnables.AiMessage(), {}, { userId: "bob" } )
				aliceAnswer = tool.invoke( {}, asAlice )
				bobAnswer   = tool.invoke( {}, asBob )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "aliceAnswer" ) ) ).contains( "inbox read with at-" );
		assertThat( variables.getAsString( Key.of( "bobAnswer" ) ) ).contains( "Error from MCP tool" );
	}

	@Test
	@DisplayName( "With the handshake on, each person gets a session of their own" )
	public void testSessionsArePerPerson() {
		this.fake.sessions = true;
		makeClient( ", subject: ( context ) => context.chatRequest.getUserId()" );
		// @formatter:off
		runtime.executeSource( "mcpClient.withHandshake( true )", context );
		// @formatter:on
		signIn( "alice" );
		signIn( "bob" );

		// @formatter:off
		runtime.executeSource(
			"""
				alice = { chatRequest: { getUserId: () => "alice" } }
				bob   = { chatRequest: { getUserId: () => "bob" } }
				mcpClient.send( "inbox", {}, alice )
				mcpClient.send( "inbox", {}, bob )
				mcpClient.send( "inbox", {}, alice )
				aliceSession = mcpClient.getSessionId( "alice" )
				bobSession   = mcpClient.getSessionId( "bob" )
			""",
			context
		);
		// @formatter:on

		String	alice	= variables.getAsString( Key.of( "aliceSession" ) );
		String	bob		= variables.getAsString( Key.of( "bobSession" ) );
		assertThat( alice ).startsWith( "sess-" );
		assertThat( bob ).startsWith( "sess-" );
		assertThat( alice ).isNotEqualTo( bob );
		// every call after a handshake sent the session of the person who made it
		assertThat( this.fake.mcpCalls.stream().filter( c -> c.startsWith( "tools/call" ) && c.endsWith( "|" + alice ) ).count() ).isEqualTo( 2 );
		assertThat( this.fake.mcpCalls.stream().filter( c -> c.startsWith( "tools/call" ) && c.endsWith( "|" + bob ) ).count() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "A callback with an unknown or used state is refused, and a sign in can only be finished once" )
	public void testStateIsChecked() {
		makeClient( "" );
		// @formatter:off
		runtime.executeSource(
			"""
				try { mcpClient.completeAuthorization( "code", "never-started" ) } catch ( any e ) { unknownType = e.type }
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "unknownType" ) ) ).isEqualTo( "MCPOAuthInvalidState" );

		signIn( "default" );
		// the same callback again: the state was used up by the first
		// @formatter:off
		runtime.executeSource(
			"""
				try { mcpClient.completeAuthorization( cbCode, cbState ) } catch ( any e ) { usedType = e.type }
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "usedType" ) ) ).isEqualTo( "MCPOAuthInvalidState" );
	}

	@Test
	@DisplayName( "The issuer in the callback is checked against the one the sign in was started with (RFC 9207)" )
	public void testIssuerIsChecked() {
		this.fake.issParameter = true;
		makeClient( "" );
		// @formatter:off
		runtime.executeSource( "started = mcpClient.startAuthorization( 'default' )\nauthUrl = started.authorizationUrl", context );
		// @formatter:on
		Map<String, String> answer = this.fake.authorize( variables.getAsString( Key.of( "authUrl" ) ) );
		variables.put( Key.of( "cbCode" ), answer.get( "code" ) );
		variables.put( Key.of( "cbState" ), answer.get( "state" ) );

		// @formatter:off
		runtime.executeSource(
			"""
				try { mcpClient.completeAuthorization( cbCode, cbState, "https://evil.example" ) } catch ( any e ) { wrongType = e.type }
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "wrongType" ) ) ).isEqualTo( "MCPOAuthIssuerMismatch" );
		// nothing was sent to the token endpoint
		assertThat( this.fake.tokenRequests ).isEmpty();

		// a server that says it sends the issuer, and does not, is refused too
		makeClient( "" );
		// @formatter:off
		runtime.executeSource( "started = mcpClient.startAuthorization( 'default' )\nauthUrl = started.authorizationUrl", context );
		// @formatter:on
		answer = this.fake.authorize( variables.getAsString( Key.of( "authUrl" ) ) );
		variables.put( Key.of( "cbCode" ), answer.get( "code" ) );
		variables.put( Key.of( "cbState" ), answer.get( "state" ) );
		// @formatter:off
		runtime.executeSource(
			"""
				try { mcpClient.completeAuthorization( cbCode, cbState ) } catch ( any e ) { missingType = e.type }
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "missingType" ) ) ).isEqualTo( "MCPOAuthIssuerMismatch" );
	}

	@Test
	@DisplayName( "A client id given in the options is used and nothing is registered" )
	public void testPreRegisteredClient() {
		makeClient( ", clientId: \"my-registered-client\", clientSecret: \"s3cret\"" );
		// @formatter:off
		runtime.executeSource( "started = mcpClient.startAuthorization( 'default' )\nauthUrl = started.authorizationUrl", context );
		// @formatter:on
		String url = variables.getAsString( Key.of( "authUrl" ) );
		assertThat( url ).contains( "client_id=my-registered-client" );
		assertThat( this.fake.registrations ).isEmpty();
	}

	@Test
	@DisplayName( "A Client ID Metadata Document address is the client id when the server supports them" )
	public void testClientIdMetadataDocument() {
		this.fake.cimd = true;
		makeClient( ", clientMetadataUrl: \"https://app.example.com/oauth/client.json\"" );
		// @formatter:off
		runtime.executeSource( "started = mcpClient.startAuthorization( 'default' )\nauthUrl = started.authorizationUrl", context );
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "authUrl" ) ) ).contains( "client_id=https%3A%2F%2Fapp.example.com%2Foauth%2Fclient.json" );
		assertThat( this.fake.registrations ).isEmpty();
	}

	@Test
	@DisplayName( "Without a client id and without a registration endpoint the sign in says what to do" )
	public void testNoWayToGetAClient() {
		this.fake.dcr = false;
		makeClient( "" );
		// @formatter:off
		runtime.executeSource(
			"""
				try { mcpClient.startAuthorization( "default" ) } catch ( any e ) { noClientType = e.type; noClientMessage = e.message }
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "noClientType" ) ) ).isEqualTo( "MCPOAuthNoClient" );
		assertThat( variables.getAsString( Key.of( "noClientMessage" ) ) ).contains( "clientId" );
	}

	@Test
	@DisplayName( "An authorization server that does not list PKCE with S256 is refused" )
	public void testPkceIsRequired() {
		this.fake.noPkce = true;
		makeClient( "" );
		// @formatter:off
		runtime.executeSource(
			"""
				try { mcpClient.startAuthorization( "default" ) } catch ( any e ) { pkceType = e.type }
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "pkceType" ) ) ).isEqualTo( "MCPOAuthDiscoveryFailed" );
	}

	@Test
	@DisplayName( "Credentials never go over plain http, except to the machine itself" )
	public void testInsecureUrlIsRefused() {
		// @formatter:off
		runtime.executeSource(
			"""
				insecure = MCP( "http://mcp.example.com/mcp" ).withOAuth( { redirectUri: "https://app.example.com/callback" } )
				try { insecure.startAuthorization( "default" ) } catch ( any e ) { insecureType = e.type }
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "insecureType" ) ) ).isEqualTo( "MCPOAuthInsecureUrl" );
	}

	@Test
	@DisplayName( "OAuth methods on a client without withOAuth say so" )
	public void testOAuthMustBeTurnedOn() {
		// @formatter:off
		runtime.executeSource(
			"""
				plain = MCP( "http://127.0.0.1:9/mcp" )
				try { plain.startAuthorization( "default" ) } catch ( any e ) { offType = e.type }
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "offType" ) ) ).isEqualTo( "MCPOAuthNotEnabled" );
	}

	@Test
	@DisplayName( "A server config with an oauth key builds a client that signs in" )
	public void testOauthKeyInServerConfig() {
		// @formatter:off
		runtime.executeSource(
			"""
				agent = aiAgent( name: "oauth-config-test" )
				agent.withMCPServer( "%s/mcp", { oauth: { redirectUri: "http://127.0.0.1:9/callback" } } )
				toolCount = agent.getTools().len()
			""".formatted( this.fake.base() ),
			context
		);
		// @formatter:on
		// not signed in: the server could not be listed, so the agent has no tools yet, and nothing broke
		assertThat( variables.get( Key.of( "toolCount" ) ) ).isEqualTo( 0 );
		assertThat( this.fake.mcpCalls ).isEmpty();
	}

	@Test
	@DisplayName( "After signing in, handing the client to an agent gives it the server's tools" )
	public void testAgentGetsToolsAfterSignIn() {
		makeClient( "" );
		signIn( "default" );
		// @formatter:off
		runtime.executeSource(
			"""
				agent = aiAgent( name: "oauth-tools-test" )
				agent.withMCPServer( mcpClient )
				toolNames = agent.getTools().map( t => t.getName() )
			""",
			context
		);
		// @formatter:on
		assertThat( variables.get( Key.of( "toolNames" ) ).toString() ).contains( "inbox" );
	}

}
