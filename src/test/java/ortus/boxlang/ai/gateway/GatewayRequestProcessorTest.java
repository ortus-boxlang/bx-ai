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
 * ----------------------------------------------------------------------------------
 * Proves the Phase 4 exit criterion with real HTTP requests (via the bx-web-support
 * mock exchange, the same technique mcpServerTest.java uses for MCPRequestProcessor):
 * full suspend -> GET pending -> signed POST decision -> resume round trip; duplicate
 * decision rejected; expired interaction rejected; unsigned/bad-signature requests
 * rejected.
 */
package ortus.boxlang.ai.gateway;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Gateway HTTP Request Processor Tests" )
public class GatewayRequestProcessorTest extends BaseIntegrationTest {

	@AfterEach
	public void clearRegistry() {
		// @formatter:off
		runtime.executeSource(
			"""
				gatewayRegistry().unregisterByModule( "" )
			""",
			context
		);
		// @formatter:on
	}

	/**
	 * Shared setup every test needs: register an HttpGateway and stub out the
	 * transport's writeResponse() so tests can inspect the full response struct
	 * (statusCode/headers) instead of just the written content.
	 */
	private static final String SETUP = """
	                                    import bxModules.bxai.models.gateway.http.GatewayRequestProcessor;
	                                    import bxModules.bxai.models.gateway.http.GatewaySecurity;
	                                    import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
	                                    import bxModules.bxai.models.gateway.contracts.GatewayContext;

	                                    secret = "test-shared-secret"
	                                    gw = aiGateway( "http", { secret: secret } )
	                                    gatewayRegistry().register( gw )

	                                    capturedResponse = {}
	                                    gwTransport = GatewayRequestProcessor::getHttpTransport()
	                                    gwTransport.writeResponse = ( response ) => {
	                                    	capturedResponse = response
	                                    	return response
	                                    }

	                                    function signedHeaders( rawBody ) {
	                                    	var timestamp = toString( int( now().getTime() / 1000 ) )
	                                    	var nonce     = createUUID()
	                                    	var signature = GatewaySecurity::sign( secret, timestamp, nonce, rawBody )
	                                    	return { "X-Timestamp": timestamp, "X-Nonce": nonce, "X-Signature": signature, "Content-Type": "application/json" }
	                                    }
	                                    """;

	@DisplayName( "full round trip: suspend -> GET pending -> signed POST decision -> resolved decision returned" )
	@Test
	public void testFullRoundTrip() {
		// @formatter:off
		runtime.executeSource(
			SETUP + """

				interactionRequest = new HumanInteractionRequest( executionID: "run-1", title: "Approval needed", message: "Delete record 5?" )
				ctx = new GatewayContext( gateway: "http", threadID: "thread-42" )
				gw.requestHumanInteraction( interactionRequest, ctx )

				// GET the pending interaction
				mockRequestNew( method: "GET", path: "/gateway.bxm", headers: {} )
					.setRequestPathInfo( "/interactions/" & interactionRequest.getId() )
				GatewayRequestProcessor::processHttp()
				getStatusCode = capturedResponse.statusCode
				getBody       = jsonDeserialize( capturedResponse.content )

				// POST a signed decision
				decisionBody = jsonSerialize( { decision: "approve", reason: "looks fine", decidedBy: "alice" } )
				mockRequestNew( method: "POST", path: "/gateway.bxm", body: decisionBody, headers: signedHeaders( decisionBody ) )
					.setRequestPathInfo( "/interactions/" & interactionRequest.getId() & "/decisions" )
				GatewayRequestProcessor::processHttp()
				postStatusCode = capturedResponse.statusCode
				postBody       = jsonDeserialize( capturedResponse.content )
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "getStatusCode" ) ) ).isEqualTo( 200 );
		var getBody = variables.getAsStruct( Key.of( "getBody" ) );
		assertThat( getBody.get( Key.of( "status" ) ) ).isEqualTo( "pending" );
		assertThat( getBody.get( Key.of( "threadID" ) ) ).isEqualTo( "thread-42" );

		assertThat( ( int ) variables.get( Key.of( "postStatusCode" ) ) ).isEqualTo( 200 );
		var postBody = variables.getAsStruct( Key.of( "postBody" ) );
		assertThat( postBody.get( Key.of( "decision" ) ) ).isEqualTo( "approve" );
		assertThat( postBody.get( Key.of( "threadID" ) ) ).isEqualTo( "thread-42" );
	}

	@DisplayName( "a duplicate decision POST for an already-resolved interaction is rejected (409)" )
	@Test
	public void testDuplicateDecisionRejected() {
		// @formatter:off
		runtime.executeSource(
			SETUP + """

				interactionRequest = new HumanInteractionRequest( executionID: "run-2" )
				ctx = new GatewayContext( gateway: "http" )
				gw.requestHumanInteraction( interactionRequest, ctx )

				decisionBody = jsonSerialize( { decision: "approve" } )

				mockRequestNew( method: "POST", path: "/gateway.bxm", body: decisionBody, headers: signedHeaders( decisionBody ) )
					.setRequestPathInfo( "/interactions/" & interactionRequest.getId() & "/decisions" )
				GatewayRequestProcessor::processHttp()
				firstStatusCode = capturedResponse.statusCode

				mockRequestNew( method: "POST", path: "/gateway.bxm", body: decisionBody, headers: signedHeaders( decisionBody ) )
					.setRequestPathInfo( "/interactions/" & interactionRequest.getId() & "/decisions" )
				GatewayRequestProcessor::processHttp()
				secondStatusCode = capturedResponse.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "firstStatusCode" ) ) ).isEqualTo( 200 );
		assertThat( ( int ) variables.get( Key.of( "secondStatusCode" ) ) ).isEqualTo( 409 );
	}

	@DisplayName( "an expired interaction is rejected (410)" )
	@Test
	public void testExpiredInteractionRejected() {
		// @formatter:off
		runtime.executeSource(
			SETUP.replace( "{ secret: secret }", "{ secret: secret, requestTTLSeconds: -1 }" ) + """

				interactionRequest = new HumanInteractionRequest( executionID: "run-3" )
				ctx = new GatewayContext( gateway: "http" )
				gw.requestHumanInteraction( interactionRequest, ctx )

				decisionBody = jsonSerialize( { decision: "approve" } )
				mockRequestNew( method: "POST", path: "/gateway.bxm", body: decisionBody, headers: signedHeaders( decisionBody ) )
					.setRequestPathInfo( "/interactions/" & interactionRequest.getId() & "/decisions" )
				GatewayRequestProcessor::processHttp()
				statusCode = capturedResponse.statusCode

				mockRequestNew( method: "GET", path: "/gateway.bxm", headers: {} )
					.setRequestPathInfo( "/interactions/" & interactionRequest.getId() )
				GatewayRequestProcessor::processHttp()
				getStatusCode = capturedResponse.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 410 );
		assertThat( ( int ) variables.get( Key.of( "getStatusCode" ) ) ).isEqualTo( 410 );
	}

	@DisplayName( "a decision POST with no signature headers is rejected (401)" )
	@Test
	public void testMissingSignatureRejected() {
		// @formatter:off
		runtime.executeSource(
			SETUP + """

				interactionRequest = new HumanInteractionRequest( executionID: "run-4" )
				ctx = new GatewayContext( gateway: "http" )
				gw.requestHumanInteraction( interactionRequest, ctx )

				decisionBody = jsonSerialize( { decision: "approve" } )
				mockRequestNew( method: "POST", path: "/gateway.bxm", body: decisionBody, headers: { "Content-Type": "application/json" } )
					.setRequestPathInfo( "/interactions/" & interactionRequest.getId() & "/decisions" )
				GatewayRequestProcessor::processHttp()
				statusCode = capturedResponse.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 401 );
	}

	@DisplayName( "a decision POST with a bad signature is rejected (401)" )
	@Test
	public void testBadSignatureRejected() {
		// @formatter:off
		runtime.executeSource(
			SETUP + """

				interactionRequest = new HumanInteractionRequest( executionID: "run-5" )
				ctx = new GatewayContext( gateway: "http" )
				gw.requestHumanInteraction( interactionRequest, ctx )

				decisionBody  = jsonSerialize( { decision: "approve" } )
				badHeaders    = signedHeaders( decisionBody )
				badHeaders[ "X-Signature" ] = "0000deadbeef0000deadbeef0000deadbeef0000deadbeef0000deadbeef00"

				mockRequestNew( method: "POST", path: "/gateway.bxm", body: decisionBody, headers: badHeaders )
					.setRequestPathInfo( "/interactions/" & interactionRequest.getId() & "/decisions" )
				GatewayRequestProcessor::processHttp()
				statusCode = capturedResponse.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 401 );
	}

	@DisplayName( "replaying the same nonce on a second, distinct request is rejected (401)" )
	@Test
	public void testNonceReplayRejected() {
		// @formatter:off
		runtime.executeSource(
			SETUP + """

				requestA = new HumanInteractionRequest( executionID: "run-6a" )
				requestB = new HumanInteractionRequest( executionID: "run-6b" )
				ctx = new GatewayContext( gateway: "http" )
				gw.requestHumanInteraction( requestA, ctx )
				gw.requestHumanInteraction( requestB, ctx )

				decisionBody = jsonSerialize( { decision: "approve" } )
				reusedHeaders = signedHeaders( decisionBody )

				mockRequestNew( method: "POST", path: "/gateway.bxm", body: decisionBody, headers: reusedHeaders )
					.setRequestPathInfo( "/interactions/" & requestA.getId() & "/decisions" )
				GatewayRequestProcessor::processHttp()
				firstStatusCode = capturedResponse.statusCode

				// Same signature/timestamp/nonce triplet, against a DIFFERENT (still-pending)
				// interaction — must fail purely on the replayed nonce, not on prior resolution.
				mockRequestNew( method: "POST", path: "/gateway.bxm", body: decisionBody, headers: reusedHeaders )
					.setRequestPathInfo( "/interactions/" & requestB.getId() & "/decisions" )
				GatewayRequestProcessor::processHttp()
				secondStatusCode = capturedResponse.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "firstStatusCode" ) ) ).isEqualTo( 200 );
		assertThat( ( int ) variables.get( Key.of( "secondStatusCode" ) ) ).isEqualTo( 401 );
	}

	@DisplayName( "GET for an unknown interaction id returns 404" )
	@Test
	public void testUnknownInteractionReturns404() {
		// @formatter:off
		runtime.executeSource(
			SETUP + """

				mockRequestNew( method: "GET", path: "/gateway.bxm", headers: {} )
					.setRequestPathInfo( "/interactions/does-not-exist" )
				GatewayRequestProcessor::processHttp()
				statusCode = capturedResponse.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 404 );
	}

	@DisplayName( "a signed inbound event is verified, parsed, and returned as a normalized message" )
	@Test
	public void testInboundEventParsed() {
		// @formatter:off
		runtime.executeSource(
			SETUP + """

				eventBody = jsonSerialize( { text: "hello from the platform", userID: "U1", conversationID: "C1" } )
				mockRequestNew( method: "POST", path: "/gateway.bxm", body: eventBody, headers: signedHeaders( eventBody ) )
					.setRequestPathInfo( "/gateways/" & gw.getName() & "/events" )
				GatewayRequestProcessor::processHttp()
				statusCode = capturedResponse.statusCode
				body       = jsonDeserialize( capturedResponse.content )
				firstMessageText = body.messages[ 1 ].text
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.get( Key.of( "firstMessageText" ) ) ).isEqualTo( "hello from the platform" );
	}

	@DisplayName( "an unsigned inbound event is rejected (401)" )
	@Test
	public void testUnsignedInboundEventRejected() {
		// @formatter:off
		runtime.executeSource(
			SETUP + """

				eventBody = jsonSerialize( { text: "hello" } )
				mockRequestNew( method: "POST", path: "/gateway.bxm", body: eventBody, headers: { "Content-Type": "application/json" } )
					.setRequestPathInfo( "/gateways/" & gw.getName() & "/events" )
				GatewayRequestProcessor::processHttp()
				statusCode = capturedResponse.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 401 );
	}

}
