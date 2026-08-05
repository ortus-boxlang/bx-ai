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
 * ----------------------------------------------------------------------------------
 * route( requestData, path ) is the testable seam: `path` is passed explicitly instead
 * of being read from CGI.PATH_INFO, so these cases can drive the full processor without
 * a running web server or CGI mocking. Each case configures its own SlackGateway and
 * registers it fresh in gatewayRegistry() under "slack" (overwriting any prior
 * registration) so cases stay isolated from each other and from onRuntimeStart's
 * best-effort registration.
 */
package ortus.boxlang.ai.channels.slack;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "SlackRequestProcessor — /events and /interactions HTTP front controller" )
public class SlackRequestProcessorTest extends BaseSlackChannelTest {

	private static final String SETUP = """
	                                    import bxModules.bxaiSlack.models.SlackRequestProcessor;
	                                    import bxModules.bxaiSlack.models.SlackSignature;
	                                    import bxModules.bxaiSlack.models.SlackGateway;
	                                    import bxModules.bxai.models.gateway.contracts.GatewayContext;
	                                    import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;

	                                    secret = "shared-secret"
	                                    gw = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: secret } )
	                                    gw.callSlackApi = ( method, body ) => {
	                                        return { ok: true, channel: body.channel ?: "", ts: "1700000000.000900" }
	                                    }
	                                    gatewayRegistry().register( gw )

	                                    signedHeaders = ( rawBody ) => {
	                                        timestamp = toString( int( now().getTime() / 1000 ) )
	                                        return {
	                                            "X-Slack-Signature"        : SlackSignature::sign( secret, timestamp, rawBody ),
	                                            "X-Slack-Request-Timestamp": timestamp,
	                                            "Content-Type"             : "application/json"
	                                        }
	                                    }
	                                    """;

	@DisplayName( "echoes the challenge for a signed url_verification handshake" )
	@Test
	public void testEchoesChallengeForUrlVerification() {
		runtime.executeSource(
		    SETUP + """
		            body     = '{"type":"url_verification","challenge":"abc123"}'
		            response = SlackRequestProcessor::route(
		                { method: "POST", body: body, headers: signedHeaders( body ), requestId: "req-1" },
		                "/events"
		            )
		            statusCode = response.statusCode
		            content    = response.content
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.get( Key.of( "content" ) ) ).isEqualTo( "abc123" );
	}

	@DisplayName( "rejects an events POST with a bad signature" )
	@Test
	public void testRejectsEventsWithBadSignature() {
		runtime.executeSource(
		    SETUP + """
		            body     = '{"type":"url_verification","challenge":"abc123"}'
		            response = SlackRequestProcessor::route(
		                {
		                    method   : "POST",
		                    body     : body,
		                    headers  : { "X-Slack-Signature": "v0=deadbeef", "X-Slack-Request-Timestamp": toString( int( now().getTime() / 1000 ) ) },
		                    requestId: "req-2"
		                },
		                "/events"
		            )
		            statusCode = response.statusCode
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 401 );
	}

	@DisplayName( "returns 400 for an events POST with a non-JSON body" )
	@Test
	public void testRejectsEventsWithNonJsonBody() {
		runtime.executeSource(
		    SETUP + """
		            body     = "not { valid json"
		            response = SlackRequestProcessor::route(
		                { method: "POST", body: body, headers: signedHeaders( body ), requestId: "req-json" },
		                "/events"
		            )
		            statusCode = response.statusCode
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 400 );
	}

	@DisplayName( "acks a signed event_callback and fires onSlackInboundMessage" )
	@Test
	public void testAcksEventCallbackAndFiresInterceptor() {
		runtime.executeSource(
		    SETUP + """
		            received = { fired: false, text: "" }
		            boxRegisterInterceptor(
		                ( data ) => {
		                    received.fired = true
		                    received.text  = data.messages[ 1 ].getText()
		                },
		                "onSlackInboundMessage"
		            )

		            body = jsonSerialize( {
		                type : "event_callback",
		                event: { type: "message", channel: "C1", user: "U1", text: "hi there", ts: "1700000000.000001" }
		            } )
		            response = SlackRequestProcessor::route(
		                { method: "POST", body: body, headers: signedHeaders( body ), requestId: "req-3" },
		                "/events"
		            )
		            statusCode = response.statusCode
		            fired      = received.fired
		            text       = received.text
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.getAsBoolean( Key.of( "fired" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "text" ) ) ).isEqualTo( "hi there" );
	}

	@DisplayName( "resolves a signed interactive button click end-to-end" )
	@Test
	public void testResolvesInteractiveButtonClickEndToEnd() {
		runtime.executeSource(
		    SETUP + """
		            gwContext    = new GatewayContext( gateway: "slack", conversationID: "C123" )
		            humanRequest = new HumanInteractionRequest( title: "Approve?", message: "msg" )
		            gw.requestHumanInteraction( humanRequest, gwContext )

		            actionValue = jsonSerialize( { requestID: humanRequest.getId(), decision: "approve" } )
		            payloadJson = jsonSerialize( {
		                type   : "block_actions",
		                user   : { id: "U999" },
		                actions: [ { value: actionValue } ]
		            } )
		            formBody = "payload=" & urlEncodedFormat( payloadJson )

		            response = SlackRequestProcessor::route(
		                { method: "POST", body: formBody, headers: signedHeaders( formBody ), requestId: "req-4" },
		                "/interactions"
		            )
		            statusCode = response.statusCode

		            record         = gw.getInteraction( humanRequest.getId() )
		            decisionIsNull = isNull( record.decision )
		            decisionValue  = decisionIsNull ? "" : record.decision.getDecision()
		            decidedBy      = decisionIsNull ? "" : record.decision.getDecidedBy()
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.getAsBoolean( Key.of( "decisionIsNull" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "decisionValue" ) ) ).isEqualTo( "approve" );
		assertThat( variables.get( Key.of( "decidedBy" ) ) ).isEqualTo( "U999" );
	}

	@DisplayName( "rejects a duplicate decision for an already-resolved interaction" )
	@Test
	public void testRejectsDuplicateDecision() {
		runtime.executeSource(
		    SETUP + """
		            gwContext    = new GatewayContext( gateway: "slack", conversationID: "C123" )
		            humanRequest = new HumanInteractionRequest( title: "Approve?", message: "msg" )
		            gw.requestHumanInteraction( humanRequest, gwContext )

		            actionValue = jsonSerialize( { requestID: humanRequest.getId(), decision: "approve" } )
		            payloadJson = jsonSerialize( { type: "block_actions", user: { id: "U999" }, actions: [ { value: actionValue } ] } )
		            formBody    = "payload=" & urlEncodedFormat( payloadJson )
		            reqData     = { method: "POST", body: formBody, headers: signedHeaders( formBody ), requestId: "req-5" }

		            first  = SlackRequestProcessor::route( reqData, "/interactions" )
		            second = SlackRequestProcessor::route( reqData, "/interactions" )

		            firstStatus  = first.statusCode
		            secondStatus = second.statusCode
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "firstStatus" ) ) ).isEqualTo( 200 );
		assertThat( variables.getAsInteger( Key.of( "secondStatus" ) ) ).isEqualTo( 409 );
	}

	@DisplayName( "returns 404 for an interaction decision on an unknown requestID" )
	@Test
	public void testReturns404ForUnknownRequestId() {
		runtime.executeSource(
		    SETUP + """
		            actionValue = jsonSerialize( { requestID: "totally-unknown", decision: "approve" } )
		            payloadJson = jsonSerialize( { type: "block_actions", user: { id: "U999" }, actions: [ { value: actionValue } ] } )
		            formBody    = "payload=" & urlEncodedFormat( payloadJson )

		            response = SlackRequestProcessor::route(
		                { method: "POST", body: formBody, headers: signedHeaders( formBody ), requestId: "req-6" },
		                "/interactions"
		            )
		            statusCode = response.statusCode
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 404 );
	}

	@DisplayName( "returns 400 for an interactions POST with an empty actions array" )
	@Test
	public void testReturns400ForEmptyActionsArray() {
		runtime.executeSource(
		    SETUP + """
		            payloadJson = jsonSerialize( { type: "block_actions", user: { id: "U999" }, actions: [] } )
		            formBody    = "payload=" & urlEncodedFormat( payloadJson )

		            response = SlackRequestProcessor::route(
		                { method: "POST", body: formBody, headers: signedHeaders( formBody ), requestId: "req-8" },
		                "/interactions"
		            )
		            statusCode = response.statusCode
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 400 );
	}

	@DisplayName( "does not fire onSlackInboundMessage for bot messages" )
	@Test
	public void testDoesNotFireInterceptorForBotMessages() {
		runtime.executeSource(
		    SETUP + """
		            fired = false
		            boxRegisterInterceptor(
		                ( data ) => { fired = true },
		                "onSlackInboundMessage"
		            )

		            body = jsonSerialize( {
		                type : "event_callback",
		                event: { type: "message", channel: "C1", bot_id: "B123", text: "bot message", ts: "1700000000.000010" }
		            } )
		            response = SlackRequestProcessor::route(
		                { method: "POST", body: body, headers: signedHeaders( body ), requestId: "req-9" },
		                "/events"
		            )
		            statusCode = response.statusCode
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( variables.getAsBoolean( Key.of( "fired" ) ) ).isFalse();
	}

	@DisplayName( "returns 404 for an unrecognized route" )
	@Test
	public void testReturns404ForUnrecognizedRoute() {
		runtime.executeSource(
		    SETUP + """
		            response = SlackRequestProcessor::route(
		                { method: "POST", body: "", headers: {}, requestId: "req-7" },
		                "/not-a-real-route"
		            )
		            statusCode = response.statusCode
		            """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 404 );
	}

}
