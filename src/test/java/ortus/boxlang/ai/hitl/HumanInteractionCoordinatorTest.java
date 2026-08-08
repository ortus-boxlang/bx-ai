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
 * Verifies HumanInteractionCoordinator's requestApproval()/resolve() lifecycle,
 * the atomic claim (only one of two concurrent resolve() calls wins), and edited
 * argument validation against a tool's declared schema.
 */
package ortus.boxlang.ai.hitl;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "HumanInteractionCoordinator Tests" )
public class HumanInteractionCoordinatorTest extends BaseIntegrationTest {

	@DisplayName( "requestApproval() against an async gateway leaves the suspension pending" )
	@Test
	public void testRequestApprovalAsyncPending() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coordinator = new HumanInteractionCoordinator()
				gw          = aiGateway( "mock" )
				interactionRequest     = new HumanInteractionRequest( executionID: "run-1" )
				ctx         = new GatewayContext( gateway: "mock" )

				suspension  = coordinator.requestApproval( humanRequest: interactionRequest, context: ctx, gateway: gw )
				isPending   = suspension.isPending()
				fetchedBack = coordinator.getSuspension( suspension.getSuspensionID() )
				sameObject  = fetchedBack.getSuspensionID() == suspension.getSuspensionID()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isPending" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sameObject" ) ) ).isTrue();
	}

	@DisplayName( "requestApproval() against a synchronous (scripted) gateway resolves immediately" )
	@Test
	public void testRequestApprovalSyncResolvesImmediately() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coordinator = new HumanInteractionCoordinator()
				gw          = aiGateway( "mock" )
				gw.setScriptedDecisions( [ "approve" ] )
				interactionRequest     = new HumanInteractionRequest( executionID: "run-2" )
				ctx         = new GatewayContext( gateway: "mock" )

				suspension  = coordinator.requestApproval( humanRequest: interactionRequest, context: ctx, gateway: gw )
				isApproved  = suspension.getStatus() == "approved"
				isTerminal  = suspension.isTerminal()
				decision    = coordinator.getDecision( suspension.getSuspensionID() )
				decisionIsApproved = decision.isApproved()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isApproved" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isTerminal" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "decisionIsApproved" ) ) ).isTrue();
	}

	@DisplayName( "resolve() with valid edited arguments transitions to edited" )
	@Test
	public void testResolveEditValidArguments() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coordinator = new HumanInteractionCoordinator()
				gw          = aiGateway( "mock" )
				interactionRequest     = new HumanInteractionRequest( executionID: "run-3" )
				ctx         = new GatewayContext( gateway: "mock" )
				tool        = createObject( "src.test.bx.tools.PlainTool" )

				suspension  = coordinator.requestApproval( humanRequest: interactionRequest, context: ctx, gateway: gw, tool: tool )

				decision = new HumanInteractionDecision(
					requestID : interactionRequest.getId(),
					decision  : "edit",
					editedData: { item: "widget", qty: 5 }
				)
				resolved   = coordinator.resolve( suspension.getSuspensionID(), decision, tool )
				isEdited   = resolved.getStatus() == "edited"

				storedDecision = coordinator.getDecision( suspension.getSuspensionID() )
				storedIsEdit   = storedDecision.isEdit()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isEdited" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "storedIsEdit" ) ) ).isTrue();
	}

	@DisplayName( "resolve() validates edited arguments wrapped as { correctedArgs: {...} } too" )
	@Test
	public void testResolveEditValidArgumentsWrappedInCorrectedArgs() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coordinator = new HumanInteractionCoordinator()
				gw          = aiGateway( "mock" )
				interactionRequest     = new HumanInteractionRequest( executionID: "run-3b" )
				ctx         = new GatewayContext( gateway: "mock" )
				tool        = createObject( "src.test.bx.tools.PlainTool" )

				suspension  = coordinator.requestApproval( humanRequest: interactionRequest, context: ctx, gateway: gw, tool: tool )

				// A gateway/consumer may wrap edited data as { correctedArgs: {...} } — the
				// same convention HumanInTheLoopMiddleware's resume path already unwraps.
				decision = new HumanInteractionDecision(
					requestID : interactionRequest.getId(),
					decision  : "edit",
					editedData: { correctedArgs: { item: "widget", qty: 5 } }
				)
				resolved = coordinator.resolve( suspension.getSuspensionID(), decision, tool )
				isEdited = resolved.getStatus() == "edited"
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isEdited" ) ) ).isTrue();
	}

	@DisplayName( "resolve() with invalid edited arguments (missing required field) downgrades to rejected" )
	@Test
	public void testResolveEditInvalidArgumentsDowngradesToReject() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coordinator = new HumanInteractionCoordinator()
				gw          = aiGateway( "mock" )
				interactionRequest     = new HumanInteractionRequest( executionID: "run-4" )
				ctx         = new GatewayContext( gateway: "mock" )
				tool        = createObject( "src.test.bx.tools.PlainTool" )

				suspension  = coordinator.requestApproval( humanRequest: interactionRequest, context: ctx, gateway: gw, tool: tool )

				// Missing required "qty"
				decision = new HumanInteractionDecision(
					requestID : interactionRequest.getId(),
					decision  : "edit",
					editedData: { item: "widget" }
				)
				resolved    = coordinator.resolve( suspension.getSuspensionID(), decision, tool )
				isRejected  = resolved.getStatus() == "rejected"

				storedDecision  = coordinator.getDecision( suspension.getSuspensionID() )
				storedIsReject  = storedDecision.isRejected()
				hasReason       = len( storedDecision.getReason() ) > 0
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isRejected" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "storedIsReject" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasReason" ) ) ).isTrue();
	}

	@DisplayName( "resolve() with edited arguments outside the schema (additionalProperties:false) downgrades to rejected" )
	@Test
	public void testResolveEditUnknownArgumentDowngradesToReject() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coordinator = new HumanInteractionCoordinator()
				gw          = aiGateway( "mock" )
				interactionRequest     = new HumanInteractionRequest( executionID: "run-5" )
				ctx         = new GatewayContext( gateway: "mock" )
				tool        = createObject( "src.test.bx.tools.PlainTool" )

				suspension  = coordinator.requestApproval( humanRequest: interactionRequest, context: ctx, gateway: gw, tool: tool )

				decision = new HumanInteractionDecision(
					requestID : interactionRequest.getId(),
					decision  : "edit",
					editedData: { item: "widget", qty: 5, discountCode: "HACKED" }
				)
				resolved   = coordinator.resolve( suspension.getSuspensionID(), decision, tool )
				isRejected = resolved.getStatus() == "rejected"
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isRejected" ) ) ).isTrue();
	}

	@DisplayName( "resolve() throws SuspensionNotFound for an unknown suspension id" )
	@Test
	public void testResolveUnknownSuspensionThrows() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;

				coordinator = new HumanInteractionCoordinator()
				decision    = new HumanInteractionDecision( requestID: "nope", decision: "approve" )

				threw = false
				errorType = ""
				try {
					coordinator.resolve( "does-not-exist", decision )
				} catch ( any e ) {
					threw = true
					errorType = e.type
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threw" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "errorType" ) ) ).isEqualTo( "SuspensionNotFound" );
	}

	@DisplayName( "resolve(): of two concurrent calls for the same suspension, exactly one wins" )
	@Test
	public void testConcurrentResolveOnlyOneWins() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coordinator  = new HumanInteractionCoordinator()
				gw           = aiGateway( "mock" )
				interactionRequest      = new HumanInteractionRequest( executionID: "run-6" )
				ctx          = new GatewayContext( gateway: "mock" )

				suspension   = coordinator.requestApproval( humanRequest: interactionRequest, context: ctx, gateway: gw )
				suspensionID = suspension.getSuspensionID()
				requestID    = interactionRequest.getId()

				futureA = asyncRun( () => {
					try {
						coordinator.resolve( suspensionID, new HumanInteractionDecision( requestID: requestID, decision: "approve" ) )
						return "ok"
					} catch ( any e ) {
						return "error:" & e.type
					}
				}, "io-tasks" )

				futureB = asyncRun( () => {
					try {
						coordinator.resolve( suspensionID, new HumanInteractionDecision( requestID: requestID, decision: "reject" ) )
						return "ok"
					} catch ( any e ) {
						return "error:" & e.type
					}
				}, "io-tasks" )

				resultA = futureA.get()
				resultB = futureB.get()

				exactlyOneWon  = ( resultA == "ok" && resultB != "ok" ) || ( resultB == "ok" && resultA != "ok" )
				loserGotRightError = ( resultA == "ok" && resultB == "error:SuspensionAlreadyResolved" ) ||
					( resultB == "ok" && resultA == "error:SuspensionAlreadyResolved" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "exactlyOneWon" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "loserGotRightError" ) ) ).isTrue();
	}

	@DisplayName( "An 'approve_always' decision records a durable grant that auto-approves future requests without presenting" )
	@Test
	public void testApproveAlwaysAutoApprovesViaDecisionStore() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coordinator = new HumanInteractionCoordinator( aiDecisionStore( "cache" ) )
				gw          = aiGateway( "mock" )
				tool        = createObject( "src.test.bx.tools.PlainTool" ).init()
				identity    = "alice-" & createUUID()
				ctx         = new GatewayContext( gateway: "mock", userID: identity )

				gw.setScriptedDecisions( [ "approve_always" ] )
				req1 = new HumanInteractionRequest( executionID: "run-7", allowedDecisions: [ "approve", "approve_always", "reject" ] )
				suspension1 = coordinator.requestApproval( humanRequest: req1, context: ctx, gateway: gw, tool: tool )
				presentedAfterFirst = gw.getPendingInteractions().len()
				approved1 = suspension1.getStatus() == "approved"

				// Same identity + tool, a fresh request — should auto-approve, never reaching the gateway
				req2 = new HumanInteractionRequest( executionID: "run-7b", allowedDecisions: [ "approve", "approve_always", "reject" ] )
				suspension2 = coordinator.requestApproval( humanRequest: req2, context: ctx, gateway: gw, tool: tool )
				presentedAfterSecond = gw.getPendingInteractions().len()
				approved2 = suspension2.getStatus() == "approved"
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "approved1" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "presentedAfterFirst" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "approved2" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "presentedAfterSecond" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "An 'approve_session' decision auto-approves later requests on the same thread only, and needs no decisionStore" )
	@Test
	public void testApproveSessionScopedToThread() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coordinator = new HumanInteractionCoordinator()
				gw          = aiGateway( "mock" )
				tool        = createObject( "src.test.bx.tools.PlainTool" ).init()
				identity    = "bob-" & createUUID()
				ctx         = new GatewayContext( gateway: "mock", userID: identity )

				gw.setScriptedDecisions( [ "approve_session" ] )
				req1 = new HumanInteractionRequest( executionID: "run-8", allowedDecisions: [ "approve", "approve_session", "reject" ] )
				coordinator.requestApproval( humanRequest: req1, context: ctx, gateway: gw, tool: tool, threadID: "thread-A" )

				// Same thread — session grant applies, gateway is not asked again
				req2 = new HumanInteractionRequest( executionID: "run-8b", allowedDecisions: [ "approve", "approve_session", "reject" ] )
				suspension2 = coordinator.requestApproval( humanRequest: req2, context: ctx, gateway: gw, tool: tool, threadID: "thread-A" )
				presentedAfterThreadA = gw.getPendingInteractions().len()
				approved2 = suspension2.getStatus() == "approved"

				// A different thread never saw the grant — gateway IS asked again
				gw.setScriptedDecisions( [ "approve" ] )
				req3 = new HumanInteractionRequest( executionID: "run-8c", allowedDecisions: [ "approve", "approve_session", "reject" ] )
				suspension3 = coordinator.requestApproval( humanRequest: req3, context: ctx, gateway: gw, tool: tool, threadID: "thread-B" )
				presentedAfterThreadB = gw.getPendingInteractions().len()
				approved3 = suspension3.getStatus() == "approved"
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "presentedAfterThreadA" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "approved2" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "presentedAfterThreadB" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "approved3" ) ) ).isTrue();
	}

	// ---- Durability: a suspension survives moving to a brand new coordinator instance ----

	@DisplayName( "setCheckpointer(): a pending suspension is resolvable from a brand new coordinator instance" )
	@Test
	public void testSuspensionSurvivesCoordinatorRestart() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				cp = aiMemory( "cache" )

				// "Process 1": creates a suspension against an unscripted (async, never
				// auto-resolving) gateway, then is discarded — nothing else references it.
				coord1 = new HumanInteractionCoordinator()
				coord1.setCheckpointer( cp )
				gw  = aiGateway( "mock" )
				req = new HumanInteractionRequest( executionID: "run-restart", pendingAction: { toolName: "deleteRecord" } )
				ctx = new GatewayContext( gateway: "mock", threadID: "restart-thread", userID: "alice" )

				suspension = coord1.requestApproval( humanRequest: req, context: ctx, gateway: gw, threadID: "restart-thread" )
				suspensionID = suspension.getSuspensionID()
				createdPending = suspension.isPending()

				// "Restart": a fresh coordinator, same checkpointer, no in-memory knowledge
				// of coord1's suspension at all.
				coord2 = new HumanInteractionCoordinator()
				coord2.setCheckpointer( cp )

				hydrated = coord2.getSuspension( suspensionID )
				foundAfterRestart = !isNull( hydrated )
				stillPendingAfterRestart = !isNull( hydrated ) && hydrated.isPending()

				decision = new HumanInteractionDecision( requestID: req.getId(), decision: "approve", decidedBy: "alice" )
				resolved = coord2.resolve( suspensionID, decision )
				resolvedByNewInstance = resolved.getStatus() == "approved"

				// Another "restart": a third instance should see the resolution durably too
				coord3 = new HumanInteractionCoordinator()
				coord3.setCheckpointer( cp )
				finalState    = coord3.getSuspension( suspensionID )
				finalStatus   = finalState.getStatus()
				finalDecision = coord3.getDecision( suspensionID )
				decisionSurvived = !isNull( finalDecision ) && finalDecision.getDecision() == "approve" && finalDecision.getDecidedBy() == "alice"
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "createdPending" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "foundAfterRestart" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "stillPendingAfterRestart" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resolvedByNewInstance" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "finalStatus" ) ) ).isEqualTo( "approved" );
		assertThat( variables.getAsBoolean( Key.of( "decisionSurvived" ) ) ).isTrue();
	}

	@DisplayName( "without setCheckpointer(): behavior is unchanged — in-memory only, nothing survives a new instance" )
	@Test
	public void testNoCheckpointerMeansNoDurability() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.HumanInteractionCoordinator;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				coord1 = new HumanInteractionCoordinator()
				gw  = aiGateway( "mock" )
				req = new HumanInteractionRequest( executionID: "run-no-cp" )
				ctx = new GatewayContext( gateway: "mock", threadID: "no-cp-thread" )

				suspension = coord1.requestApproval( humanRequest: req, context: ctx, gateway: gw, threadID: "no-cp-thread" )
				suspensionID = suspension.getSuspensionID()

				coord2 = new HumanInteractionCoordinator()
				notFound = isNull( coord2.getSuspension( suspensionID ) )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "notFound" ) ) ).isTrue();
	}

}
