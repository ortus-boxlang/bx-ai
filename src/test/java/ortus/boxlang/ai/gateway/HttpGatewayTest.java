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
 * Verifies HttpGateway in isolation (no real HTTP) — signing/verification via
 * GatewaySecurity, inbound parsing, pending-interaction lifecycle, atomic resolve,
 * and expiry. GatewayRequestProcessorTest covers the real end-to-end HTTP round trip.
 */
package ortus.boxlang.ai.gateway;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "HttpGateway Tests" )
public class HttpGatewayTest extends BaseIntegrationTest {

	@DisplayName( "declares inbound/outbound/humanApproval capabilities" )
	@Test
	public void testDeclaresCapabilities() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw = aiGateway( "http", { secret: "test-secret" } )
				supportsIn   = gw.supports( "inboundMessages" )
				supportsOut  = gw.supports( "outboundMessages" )
				supportsHitl = gw.supports( "humanApproval" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "supportsIn" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "supportsOut" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "supportsHitl" ) ) ).isTrue();
	}

	@DisplayName( "GatewaySecurity: sign() then verify() round-trips as valid" )
	@Test
	public void testSecurityRoundTrip() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.http.GatewaySecurity;

				secret    = "shh-its-a-secret"
				timestamp = toString( int( now().getTime() / 1000 ) )
				nonce     = createUUID()
				rawBody   = '{"hello":"world"}'

				signature = GatewaySecurity::sign( secret, timestamp, nonce, rawBody )
				verifyResult = GatewaySecurity::verify( secret, timestamp, nonce, rawBody, signature )
				isValid      = verifyResult.valid
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isValid" ) ) ).isTrue();
	}

	@DisplayName( "GatewaySecurity: verify() rejects a tampered body" )
	@Test
	public void testSecurityRejectsTamperedBody() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.http.GatewaySecurity;

				secret    = "shh-its-a-secret"
				timestamp = toString( int( now().getTime() / 1000 ) )
				nonce     = createUUID()

				signature = GatewaySecurity::sign( secret, timestamp, nonce, '{"amount":10}' )
				verifyResult = GatewaySecurity::verify( secret, timestamp, nonce, '{"amount":99999}', signature )
				isValid      = verifyResult.valid
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isValid" ) ) ).isFalse();
	}

	@DisplayName( "requestHumanInteraction() tracks a pending interaction; resolveInteraction() resolves it exactly once" )
	@Test
	public void testInteractionLifecycle() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw = aiGateway( "http", { secret: "test-secret" } )
				interactionRequest = new HumanInteractionRequest( executionID: "run-1" )
				ctx = new GatewayContext( gateway: "http", threadID: "thread-1" )

				delivery = gw.requestHumanInteraction( interactionRequest, ctx )
				hasDecisionYet = delivery.hasDecision()

				record = gw.getInteraction( interactionRequest.getId() )
				trackedThreadID = record.context.getThreadID()

				decision = new HumanInteractionDecision( requestID: interactionRequest.getId(), decision: "approve" )
				resolved = gw.resolveInteraction( interactionRequest.getId(), decision )
				resolvedIsApproved = resolved.isApproved()

				secondAttemptThrew = false
				try {
					gw.resolveInteraction( interactionRequest.getId(), decision )
				} catch ( any e ) {
					secondAttemptThrew = true
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasDecisionYet" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "trackedThreadID" ) ) ).isEqualTo( "thread-1" );
		assertThat( variables.getAsBoolean( Key.of( "resolvedIsApproved" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "secondAttemptThrew" ) ) ).isTrue();
	}

	// ---- Durability: a pending interaction survives moving to a brand new gateway instance ----

	@DisplayName( "setCheckpointer(): a pending interaction is resolvable from a brand new gateway instance" )
	@Test
	public void testInteractionSurvivesGatewayRestart() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				cp = aiMemory( "cache" )

				// "Process 1": presents an interaction, then is discarded — nothing else
				// references it.
				gw1 = aiGateway( "http", { secret: "test-secret" } )
				gw1.setCheckpointer( cp )
				interactionRequest = new HumanInteractionRequest( executionID: "run-restart", pendingAction: { toolName: "deleteRecord" } )
				ctx = new GatewayContext( gateway: "http", threadID: "restart-thread", userID: "alice" )
				gw1.requestHumanInteraction( interactionRequest, ctx )

				// "Restart": a fresh gateway instance, same checkpointer, no in-memory
				// knowledge of gw1's interaction at all.
				gw2 = aiGateway( "http", { secret: "test-secret" } )
				gw2.setCheckpointer( cp )

				hydrated = gw2.getInteraction( interactionRequest.getId() )
				foundAfterRestart = !isNull( hydrated )
				threadSurvived = !isNull( hydrated ) && hydrated.context.getThreadID() == "restart-thread"

				decision = new HumanInteractionDecision( requestID: interactionRequest.getId(), decision: "approve", decidedBy: "alice" )
				resolved = gw2.resolveInteraction( interactionRequest.getId(), decision )
				resolvedByNewInstance = resolved.isApproved()

				// A THIRD instance should see the resolution durably too, and reject a
				// second resolve attempt exactly as it would within a single process.
				gw3 = aiGateway( "http", { secret: "test-secret" } )
				gw3.setCheckpointer( cp )
				finalRecord = gw3.getInteraction( interactionRequest.getId() )
				decisionSurvived = !isNull( finalRecord.decision ) && finalRecord.decision.getDecidedBy() == "alice"

				secondResolveThrew = false
				try {
					gw3.resolveInteraction( interactionRequest.getId(), decision )
				} catch ( any e ) {
					secondResolveThrew = true
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "foundAfterRestart" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "threadSurvived" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resolvedByNewInstance" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "decisionSurvived" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "secondResolveThrew" ) ) ).isTrue();
	}

	@DisplayName( "without setCheckpointer(): behavior is unchanged — in-memory only, nothing survives a new instance" )
	@Test
	public void testNoCheckpointerMeansNoDurability() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw1 = aiGateway( "http", { secret: "test-secret" } )
				interactionRequest = new HumanInteractionRequest( executionID: "run-no-cp" )
				ctx = new GatewayContext( gateway: "http", threadID: "no-cp-thread" )
				gw1.requestHumanInteraction( interactionRequest, ctx )

				gw2 = aiGateway( "http", { secret: "test-secret" } )
				notFound = isNull( gw2.getInteraction( interactionRequest.getId() ) )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "notFound" ) ) ).isTrue();
	}

	@DisplayName( "MockGateway/CliGateway inherit setCheckpointer() as a harmless no-op" )
	@Test
	public void testOtherGatewaysIgnoreSetCheckpointer() {
		// @formatter:off
		runtime.executeSource(
			"""
				mockGw = aiGateway( "mock" )
				cliGw  = aiGateway( "cli" )
				cp     = aiMemory( "cache" )

				didNotThrow = true
				try {
					mockGw.setCheckpointer( cp )
					cliGw.setCheckpointer( cp )
				} catch ( any e ) {
					didNotThrow = false
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "didNotThrow" ) ) ).isTrue();
	}

	@DisplayName( "setCheckpointer(): survives a real File-backed store, not just an in-process cache" )
	@Test
	public void testInteractionSurvivesWithFileBackedStore() {
		// Same reasoning as HumanInteractionCoordinatorTest's file-backed test: this proves
		// gw2 reads what gw1 wrote purely from a JSON file on disk, not anything shared
		// in-process — the actual claim "survives a restart" rests on.
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				storeDir = getTempDirectory() & "/bxai-http-gateway-file-restart-" & createUUID()

				gw1 = aiGateway( "http", { secret: "test-secret" } )
				gw1.setCheckpointer( aiMemory( memory: "file", config: { directoryPath: storeDir } ) )
				interactionRequest = new HumanInteractionRequest( executionID: "run-file-restart", pendingAction: { toolName: "deleteRecord" } )
				ctx = new GatewayContext( gateway: "http", threadID: "file-restart-thread", userID: "alice" )
				gw1.requestHumanInteraction( interactionRequest, ctx )

				checkpointFileExists = fileExists( storeDir & "/checkpoints/hitl_http-interaction_" & interactionRequest.getId().reReplace( "[^a-zA-Z0-9_\\-]", "_", "all" ) & ".json" )

				gw2 = aiGateway( "http", { secret: "test-secret" } )
				gw2.setCheckpointer( aiMemory( memory: "file", config: { directoryPath: storeDir } ) )

				decision = new HumanInteractionDecision( requestID: interactionRequest.getId(), decision: "approve", decidedBy: "alice" )
				resolved = gw2.resolveInteraction( interactionRequest.getId(), decision )
				resolvedByNewInstance = resolved.isApproved()

				gw3 = aiGateway( "http", { secret: "test-secret" } )
				gw3.setCheckpointer( aiMemory( memory: "file", config: { directoryPath: storeDir } ) )
				finalRecord = gw3.getInteraction( interactionRequest.getId() )
				decisionSurvived = !isNull( finalRecord.decision ) && finalRecord.decision.getDecidedBy() == "alice"
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "checkpointFileExists" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resolvedByNewInstance" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "decisionSurvived" ) ) ).isTrue();
	}

	@DisplayName( "onGatewayMessageReceived fires from parseInbound() with the request's threadId" )
	@Test
	public void testOnGatewayMessageReceivedFires() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw = aiGateway( "http", { secret: "test-secret" } )

				fired          = false
				capturedThread = ""
				BoxRegisterInterceptor(
					function( data ) {
						fired          = true
						capturedThread = data.threadId
					},
					"onGatewayMessageReceived"
				)

				gw.parseInbound( { text: "hello", userID: "U1", conversationID: "C1", threadID: "T1" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "fired" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "capturedThread" ) ) ).isEqualTo( "T1" );
	}

	@DisplayName( "onGatewayMessageSent fires from deliver() even when no callbackUrl is configured" )
	@Test
	public void testOnGatewayMessageSentFires() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw = aiGateway( "http", { secret: "test-secret" } )

				fired          = false
				capturedThread = ""
				BoxRegisterInterceptor(
					function( data ) {
						fired          = true
						capturedThread = data.threadId
					},
					"onGatewayMessageSent"
				)

				event = new bxModules.bxai.models.gateway.contracts.GatewayEvent( type: "response.completed", data: { content: "hi" } )
				ctx   = new bxModules.bxai.models.gateway.contracts.GatewayContext( threadID: "T2" )
				gw.deliver( event, ctx )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "fired" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "capturedThread" ) ) ).isEqualTo( "T2" );
	}

	@DisplayName( "isRunning() tracks start()/stop() via the inherited BaseGateway template method, even with no real connect step overridden" )
	@Test
	public void testIsRunningTracksLifecycle() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw = aiGateway( "http", { secret: "test-secret" } )
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

}
