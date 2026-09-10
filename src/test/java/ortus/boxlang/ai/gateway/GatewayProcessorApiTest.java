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
 * Covers GatewayRequestProcessor's transport-agnostic API — the statics a host framework
 * with its own router (ColdBox's toAiGateway() terminator) calls directly, with no CGI
 * request, no HTTPTransport, and no response writing anywhere in the picture.
 * GatewayRequestProcessorTest covers the same processing through processHttp() instead.
 */
package ortus.boxlang.ai.gateway;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Gateway Processor Transport-Agnostic API Tests" )
public class GatewayProcessorApiTest extends BaseIntegrationTest {

	@AfterEach
	public void clearRegistry() {
		// @formatter:off
		runtime.executeSource(
			"""
				aiGatewayRegistry().unregisterByModule( "" )
			""",
			context
		);
		// @formatter:on
	}

	private static final String IMPORTS = """
	                                      import bxModules.bxai.models.gateway.http.GatewayRequestProcessor;
	                                      import bxModules.bxai.models.gateway.http.GatewaySecurity;
	                                      """;

	@DisplayName( "processInbound() with no session verifies, parses, and returns the normalized messages (200)" )
	@Test
	public void testProcessInboundParseOnly() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				gw = aiGateway( "mock" )
				aiGatewayRegistry().register( gw )

				rawBody = jsonSerialize( { text: "hello from the platform", userID: "u1", conversationID: "c1" } )
				result  = GatewayRequestProcessor::processInbound( "mock", rawBody )

				statusCode  = result.statusCode
				messageText = result.body.messages[ 1 ].text
				gatewayName = result.headers[ "X-Gateway-Name" ]
				contentType = result.contentType
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.get( Key.of( "messageText" ) ) ).isEqualTo( "hello from the platform" );
		assertThat( variables.get( Key.of( "gatewayName" ) ) ).isEqualTo( "mock" );
		assertThat( variables.get( Key.of( "contentType" ) ) ).isEqualTo( "application/json" );
	}

	@DisplayName( "processInbound() with a session acks 202 immediately, reports the thread, and dispatches the turn" )
	@Test
	public void testProcessInboundDispatchesToSession() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				mockSvc = aiService( "mock" )
				mockSvc.setResponses( [ "Hello back!" ] )
				model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
				agent = aiAgent( model: model )

				gw = aiGateway( "mock" )
				aiGatewayRegistry().register( gw )
				gatewaySession = aiGatewaySession( agent: agent, gateways: [ gw ], policy: "queue" )

				rawBody = jsonSerialize( { text: "hello", userID: "u1", conversationID: "c1", threadID: "thread-http" } )
				result  = GatewayRequestProcessor::processInbound(
					gatewayName: "mock",
					rawBody    : rawBody,
					headers    : {},
					session    : gatewaySession
				)

				statusCode = result.statusCode
				accepted   = result.body.accepted
				threadId   = result.body.messages[ 1 ].threadId
				threadHeader = result.headers[ "X-Thread-Id" ]

				// The dispatch is deliberately never joined — that's the whole point of the
				// 202 — so the reply is waited for here, bounded, rather than assumed.
				waited = 0
				while ( gw.getDeliveredEvents().isEmpty() && waited < 5000 ) {
					sleep( 25 )
					waited += 25
				}
				delivered = gw.getDeliveredEvents()
				gotReply  = delivered.len() > 0 && delivered[ 1 ].getData().content == "Hello back!"
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 202 );
		assertThat( variables.get( Key.of( "accepted" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "threadId" ) ) ).isEqualTo( "thread-http" );
		assertThat( variables.get( Key.of( "threadHeader" ) ) ).isEqualTo( "thread-http" );
		assertThat( variables.getAsBoolean( Key.of( "gotReply" ) ) ).isTrue();
	}

	@DisplayName( "processInbound() for an unregistered gateway returns 404" )
	@Test
	public void testProcessInboundUnknownGateway() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				result     = GatewayRequestProcessor::processInbound( "nope", jsonSerialize( { text: "hi" } ) )
				statusCode = result.statusCode
				errorText  = result.body.error
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 404 );
		assertThat( variables.getAsString( Key.of( "errorText" ) ) ).contains( "nope" );
	}

	@DisplayName( "processInbound() rejects an unsigned body for a signature-verifying gateway (401)" )
	@Test
	public void testProcessInboundUnsignedRejected() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				gw = aiGateway( "http", { secret: "test-shared-secret" } )
				aiGatewayRegistry().register( gw )

				result     = GatewayRequestProcessor::processInbound( "http", jsonSerialize( { text: "hi" } ) )
				statusCode = result.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 401 );
	}

	@DisplayName( "processInbound() accepts a correctly signed body for a signature-verifying gateway" )
	@Test
	public void testProcessInboundSignedAccepted() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				secret = "test-shared-secret"
				gw = aiGateway( "http", { secret: secret } )
				aiGatewayRegistry().register( gw )

				rawBody   = jsonSerialize( { text: "signed hello", conversationID: "c9" } )
				timestamp = toString( int( now().getTime() / 1000 ) )
				nonce     = createUUID()
				headers   = {
					"X-Timestamp": timestamp,
					"X-Nonce"    : nonce,
					"X-Signature": GatewaySecurity::sign( secret, timestamp, nonce, rawBody )
				}

				result      = GatewayRequestProcessor::processInbound( "http", rawBody, headers )
				statusCode  = result.statusCode
				messageText = result.body.messages[ 1 ].text
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.get( Key.of( "messageText" ) ) ).isEqualTo( "signed hello" );
	}

	@DisplayName( "processInbound() rejects a body that is not valid JSON (400)" )
	@Test
	public void testProcessInboundInvalidJson() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				gw = aiGateway( "mock" )
				aiGatewayRegistry().register( gw )

				result     = GatewayRequestProcessor::processInbound( "mock", "not json at all" )
				statusCode = result.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 400 );
	}

	@DisplayName( "processHandshake() echoes the platform's challenge as plain text" )
	@Test
	public void testProcessHandshake() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				gw = aiGateway( "mock" )
				aiGatewayRegistry().register( gw )

				result      = GatewayRequestProcessor::processHandshake( "mock", { challenge: "1158201444", mode: "subscribe" } )
				statusCode  = result.statusCode
				body        = result.body
				contentType = result.contentType
				sawParams   = gw.getHandshakes().len() == 1
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.get( Key.of( "body" ) ) ).isEqualTo( "1158201444" );
		assertThat( variables.get( Key.of( "contentType" ) ) ).isEqualTo( "text/plain" );
		assertThat( variables.getAsBoolean( Key.of( "sawParams" ) ) ).isTrue();
	}

	@DisplayName( "processHandshake() on a gateway that does not declare the capability returns 405" )
	@Test
	public void testProcessHandshakeUnsupported() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				gw = aiGateway( "http", { secret: "s" } )
				aiGatewayRegistry().register( gw )

				result     = GatewayRequestProcessor::processHandshake( "http", { challenge: "x" } )
				statusCode = result.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 405 );
	}

	@DisplayName( "readInteraction() returns a pending interaction, and 404 for an unknown id" )
	@Test
	public void testReadInteraction() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw = aiGateway( "http", { secret: "s" } )
				aiGatewayRegistry().register( gw )

				interactionRequest = new HumanInteractionRequest( executionID: "run-1", title: "Approval needed" )
				gw.requestHumanInteraction( interactionRequest, new GatewayContext( gateway: "http", threadID: "thread-9" ) )

				found      = GatewayRequestProcessor::readInteraction( interactionRequest.getId() )
				statusCode = found.statusCode
				status     = found.body.status
				threadID   = found.body.threadID

				missing           = GatewayRequestProcessor::readInteraction( "does-not-exist" )
				missingStatusCode = missing.statusCode
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.get( Key.of( "status" ) ) ).isEqualTo( "pending" );
		assertThat( variables.get( Key.of( "threadID" ) ) ).isEqualTo( "thread-9" );
		assertThat( ( int ) variables.get( Key.of( "missingStatusCode" ) ) ).isEqualTo( 404 );
	}

	@DisplayName( "submitDecision() resolves a pending interaction when signed, and rejects it unsigned" )
	@Test
	public void testSubmitDecision() {
		// @formatter:off
		runtime.executeSource(
			IMPORTS + """
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				secret = "test-shared-secret"
				gw = aiGateway( "http", { secret: secret } )
				aiGatewayRegistry().register( gw )

				unsignedRequest = new HumanInteractionRequest( executionID: "run-2" )
				gw.requestHumanInteraction( unsignedRequest, new GatewayContext( gateway: "http" ) )
				decisionBody     = jsonSerialize( { decision: "approve" } )
				unsignedResult   = GatewayRequestProcessor::submitDecision( unsignedRequest.getId(), decisionBody )
				unsignedStatus   = unsignedResult.statusCode

				signedRequest = new HumanInteractionRequest( executionID: "run-3" )
				gw.requestHumanInteraction( signedRequest, new GatewayContext( gateway: "http", threadID: "thread-7" ) )
				timestamp = toString( int( now().getTime() / 1000 ) )
				nonce     = createUUID()
				headers   = {
					"X-Timestamp": timestamp,
					"X-Nonce"    : nonce,
					"X-Signature": GatewaySecurity::sign( secret, timestamp, nonce, decisionBody )
				}
				signedResult = GatewayRequestProcessor::submitDecision( signedRequest.getId(), decisionBody, headers )
				signedStatus = signedResult.statusCode
				decision     = signedResult.body.decision
				threadID     = signedResult.body.threadID
			""",
			context
		);
		// @formatter:on

		assertThat( ( int ) variables.get( Key.of( "unsignedStatus" ) ) ).isEqualTo( 401 );
		assertThat( ( int ) variables.get( Key.of( "signedStatus" ) ) ).isEqualTo( 200 );
		assertThat( variables.get( Key.of( "decision" ) ) ).isEqualTo( "approve" );
		assertThat( variables.get( Key.of( "threadID" ) ) ).isEqualTo( "thread-7" );
	}

}
