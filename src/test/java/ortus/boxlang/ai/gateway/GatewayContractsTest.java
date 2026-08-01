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
 * Verifies the normalized Gateway contract classes construct with sensible defaults,
 * round-trip through jsonSerialize()/jsonDeserialize() (needed later for the HTTP
 * gateway and any persisted/transported representation), and that AgentSuspension's
 * state-machine helpers behave correctly.
 */
package ortus.boxlang.ai.gateway;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Gateway Contract Classes Tests" )
public class GatewayContractsTest extends BaseIntegrationTest {

	@DisplayName( "GatewayContext constructs with defaults and round-trips through JSON" )
	@Test
	public void testGatewayContextRoundTrip() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				ctx = new GatewayContext( gateway: "slack", conversationID: "C1", userID: "U1" )
				json    = jsonSerialize( ctx.toStruct() )
				parsed  = jsonDeserialize( json )
				result  = ( parsed.gateway == "slack" && parsed.conversationID == "C1" && parsed.userID == "U1" )

				// Defaults
				defaultCtx = new GatewayContext()
				defaultsOk = ( defaultCtx.getGateway() == "" && isStruct( defaultCtx.getIdentity() ) && defaultCtx.getIdentity().isEmpty() )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "defaultsOk" ) ) ).isTrue();
	}

	@DisplayName( "GatewayMessage constructs with an auto-generated id and default context" )
	@Test
	public void testGatewayMessageDefaults() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.GatewayMessage;

				msg = new GatewayMessage( text: "hi" )
				result = ( msg.getId().len() > 0 && msg.getType() == "message" && !isNull( msg.getContext() ) && isArray( msg.getAttachments() ) )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
	}

	@DisplayName( "GatewayEvent exposes well-known TYPES constants" )
	@Test
	public void testGatewayEventTypes() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.GatewayEvent;

				result = (
					GatewayEvent.TYPES.EXECUTION_SUSPENDED == "execution.suspended" &&
					GatewayEvent.TYPES.RESPONSE_DELTA       == "response.delta" &&
					GatewayEvent.TYPES.EXECUTION_FAILED     == "execution.failed"
				)

				event = new GatewayEvent( type: GatewayEvent.TYPES.EXECUTION_SUSPENDED, executionID: "run-1", data: { toolName: "deleteRecord" } )
				dataOk = ( event.getData().toolName == "deleteRecord" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "dataOk" ) ) ).isTrue();
	}

	@DisplayName( "HumanInteractionRequest defaults allowedDecisions to approve/reject and exposes TYPES" )
	@Test
	public void testHumanInteractionRequestDefaults() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;

				req = new HumanInteractionRequest( executionID: "run-1" )
				result = (
					req.getAllowedDecisions().len() == 2 &&
					req.getAllowedDecisions().findNoCase( "approve" ) > 0 &&
					req.getAllowedDecisions().findNoCase( "reject" ) > 0 &&
					req.getInteractionType() == HumanInteractionRequest.TYPES.TOOL_APPROVAL &&
					!req.isExpired()
				)
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
	}

	@DisplayName( "HumanInteractionDecision predicate methods match the decided value" )
	@Test
	public void testHumanInteractionDecisionPredicates() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;

				approve = new HumanInteractionDecision( requestID: "r1", decision: "approve" )
				reject  = new HumanInteractionDecision( requestID: "r1", decision: "reject" )
				edit    = new HumanInteractionDecision( requestID: "r1", decision: "edit", editedData: { id: 5 } )
				cancel  = new HumanInteractionDecision( requestID: "r1", decision: "cancel" )

				result = (
					approve.isApproved() && !approve.isRejected() &&
					reject.isRejected()  && !reject.isApproved()  &&
					edit.isEdit()        && edit.getEditedData().id == 5 &&
					cancel.isCancelled()
				)
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
	}

	@DisplayName( "AgentSuspension tracks pending/terminal state correctly across statuses" )
	@Test
	public void testAgentSuspensionStateMachine() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.AgentSuspension;

				pending = new AgentSuspension( executionID: "run-1", threadID: "t-1", status: AgentSuspension.STATUSES.PENDING )
				resolved = new AgentSuspension( executionID: "run-1", threadID: "t-1", status: AgentSuspension.STATUSES.APPROVED )

				result = (
					pending.isPending()  && !pending.isTerminal() &&
					!resolved.isPending() && resolved.isTerminal()
				)
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
	}

}
