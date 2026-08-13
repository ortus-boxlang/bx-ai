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
 * Proves the Phase 1 exit criterion: a gateway can exchange messages and represent
 * approval requests using only the IGateway SPI and its normalized contracts —
 * with zero CLI-specific or platform-specific code anywhere in these tests.
 */
package ortus.boxlang.ai.gateway;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "MockGateway (Gateway SPI reference implementation) Tests" )
public class MockGatewayTest extends BaseIntegrationTest {

	@DisplayName( "parseInbound() normalizes a raw payload into a GatewayMessage" )
	@Test
	public void testParseInbound() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw = aiGateway( "mock" )
				messages = gw.parseInbound( { text: "hello world", userID: "U1", conversationID: "C1" } )
				msg      = messages[ 1 ]
				result   = ( msg.getText() == "hello world" && msg.getContext().getUserID() == "U1" && msg.getContext().getConversationID() == "C1" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
	}

	@DisplayName( "deliver() records the event and returns a successful GatewayDeliveryResult" )
	@Test
	public void testDeliver() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.GatewayEvent;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw      = aiGateway( "mock" )
				event   = new GatewayEvent( type: GatewayEvent.TYPES.RESPONSE_COMPLETED, executionID: "run-1" )
				ctx     = new GatewayContext( gateway: "mock", conversationID: "C1" )
				result  = gw.deliver( event, ctx )

				success  = result.getSuccess()
				delivered = gw.getDeliveredEvents()
				recorded  = ( delivered.len() == 1 && delivered[ 1 ].getExecutionID() == "run-1" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "recorded" ) ) ).isTrue();
	}

	@DisplayName( "requestHumanInteraction() presents a tool-approval request and defers the decision (async gateway)" )
	@Test
	public void testRequestHumanInteractionAsync() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw      = aiGateway( "mock" )
				interactionRequest = new HumanInteractionRequest(
					executionID  : "run-2",
					interactionType: "tool_approval",
					title        : "Approval needed",
					message      : "Delete record 5?",
					pendingAction: { toolName: "deleteRecord", toolArgs: { id: 5 } }
				)
				ctx = new GatewayContext( gateway: "mock", conversationID: "C1" )

				result = gw.requestHumanInteraction( interactionRequest, ctx )
				hasDecisionYet = result.hasDecision()

				// The human responds later, out-of-band (as a real async gateway would)
				inboundEvent = gw.simulateDecision( interactionRequest.getId(), "approve", {}, "looks fine" )
				decision     = gw.parseHumanDecision( inboundEvent )

				isApproved  = decision.isApproved()
				reasonMatch = ( decision.getReason() == "looks fine" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasDecisionYet" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isApproved" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "reasonMatch" ) ) ).isTrue();
	}

	@DisplayName( "requestHumanInteraction() resolves immediately when a decision is scripted ahead of time" )
	@Test
	public void testRequestHumanInteractionScriptedSync() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw = aiGateway( "mock" )
				gw.setScriptedDecisions( [ "reject" ] )

				interactionRequest = new HumanInteractionRequest( executionID: "run-3", pendingAction: { toolName: "dropTable" } )
				ctx     = new GatewayContext( gateway: "mock" )

				result       = gw.requestHumanInteraction( interactionRequest, ctx )
				hasDecision  = result.hasDecision()
				isRejected   = result.getDecision().isRejected()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasDecision" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isRejected" ) ) ).isTrue();
	}

	@DisplayName( "isRunning() reflects start()/stop(); a gateway that never overrides them stays false" )
	@Test
	public void testIsRunningTracksLifecycle() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw = aiGateway( "mock" )
				notRunningInitially = !gw.isRunning()

				gw.start()
				runningAfterStart = gw.isRunning()

				gw.stop()
				notRunningAfterStop = !gw.isRunning()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "notRunningInitially" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "runningAfterStart" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "notRunningAfterStop" ) ) ).isTrue();
	}

	@DisplayName( "onError() registers a callback that simulateError() invokes with the given error info" )
	@Test
	public void testOnErrorCallbackFires() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw = aiGateway( "mock" )

				fired         = false
				capturedError = ""
				gw.onError( function( error ) {
					fired         = true
					capturedError = error
				} )

				gw.simulateError( "connection dropped" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "fired" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "capturedError" ) ) ).isEqualTo( "connection dropped" );
	}

	@DisplayName( "simulateError() with no onError() callback registered is a safe no-op" )
	@Test
	public void testSimulateErrorWithoutCallbackIsNoOp() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw = aiGateway( "mock" )
				gw.simulateError( "nobody is listening" )
				survived = true
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "survived" ) ) ).isTrue();
	}

}
