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
 * Tests for GatewaySession: the sole orchestrator wiring one-or-more IGateway instances to
 * one AiAgent, deciding — via handleInbound()'s configurable policy — what happens when a
 * second inbound message arrives on a thread that already has a turn in flight. Races are
 * simulated deterministically by triggering the second handleInbound() call from inside a
 * tool closure that fires synchronously during the first dispatched turn (the same trick
 * RunControlMiddlewareTest already uses for cancelRun()/steerRun()), never with real
 * OS-thread timing. handleInbound()'s returned BoxFuture is used to wait for a turn — and
 * everything it drains from the queue — to finish, with no sleeps.
 */
package ortus.boxlang.ai.gateway;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "GatewaySession Tests" )
public class GatewaySessionTest extends BaseIntegrationTest {

	@DisplayName( "Single gateway, free thread: dispatches immediately and delivers the reply" )
	@Test
	public void testFreeThreadDispatchesImmediately() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [ "Hello back!" ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        gw = aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: [ gw ], policy: "queue" )

		        messages = gw.parseInbound( { text: "hello", userID: "u1", conversationID: "c1", threadID: "thread-free" } )
		        future = session.handleInbound( messages[ 1 ], gw )
		        wasDispatched = !isNull( future )
		        future.get()

		        delivered = gw.getDeliveredEvents()
		        gotOneDelivery = delivered.len() == 1
		        gotReply       = delivered[ 1 ].getData().content == "Hello back!"
		        noLongerActive = session.getActiveThreadIds().isEmpty()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "wasDispatched" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "gotOneDelivery" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "gotReply" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noLongerActive" ) ) ).isTrue();
	}

	@DisplayName( "reject policy: a second message on a busy thread is immediately rejected and never reaches the agent" )
	@Test
	public void testRejectPolicy() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "Outer final."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        gw = aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: [ gw ], policy: "reject" )

		        innerDispatched = true
		        toolA = aiTool( "toolA", "Tool A", () => {
		            msg2 = gw.parseInbound( { text: "second", userID: "u1", conversationID: "c1", threadID: "thread-reject" } )
		            innerDispatched = !isNull( session.handleInbound( msg2[ 1 ], gw ) )
		            return "A done"
		        } )
		        agent.withTools( [ toolA ] )

		        msg1 = gw.parseInbound( { text: "first", userID: "u1", conversationID: "c1", threadID: "thread-reject" } )
		        session.handleInbound( msg1[ 1 ], gw ).get()

		        delivered = gw.getDeliveredEvents()
		        sawRejection  = delivered.some( ( e ) => e.getType() == "message.rejected" )
		        sawCompletion = delivered.some( ( e ) => e.getType() == "response.completed" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "innerDispatched" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "sawRejection" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawCompletion" ) ) ).isTrue();
	}

	@DisplayName( "queue policy: messages enqueue and drain in order after the current turn, including a 3rd enqueued during the 2nd" )
	@Test
	public void testQueuePolicyDrainsInOrder() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "First.",
		            { toolCalls: [ { name: "toolB", arguments: {} } ] },
		            "Second.",
		            "Third."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        gw = aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: [ gw ], policy: "queue" )

		        toolA = aiTool( "toolA", "Tool A", () => {
		            msg2 = gw.parseInbound( { text: "second msg", userID: "u1", conversationID: "c1", threadID: "thread-queue" } )
		            session.handleInbound( msg2[ 1 ], gw )
		            return "A done"
		        } )
		        toolB = aiTool( "toolB", "Tool B", () => {
		            msg3 = gw.parseInbound( { text: "third msg", userID: "u1", conversationID: "c1", threadID: "thread-queue" } )
		            session.handleInbound( msg3[ 1 ], gw )
		            return "B done"
		        } )
		        agent.withTools( [ toolA, toolB ] )

		        msg1 = gw.parseInbound( { text: "first msg", userID: "u1", conversationID: "c1", threadID: "thread-queue" } )
		        session.handleInbound( msg1[ 1 ], gw ).get()

		        delivered = gw.getDeliveredEvents()
		        contents  = delivered.map( ( e ) => e.getData().content )
		        gotAllThree   = delivered.len() == 3
		        gotInOrder    = contents.len() == 3 && contents[ 1 ] == "First." && contents[ 2 ] == "Second." && contents[ 3 ] == "Third."
		        noLongerActive = session.getActiveThreadIds().isEmpty()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "gotAllThree" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "gotInOrder" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noLongerActive" ) ) ).isTrue();
	}

	@DisplayName( "steer policy: a busy-thread message splices into the SAME live turn — not a new turn" )
	@Test
	public void testSteerPolicySplicesIntoLiveTurn() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "Final with BANANA."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        gw = aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: [ gw ], policy: "steer" )

		        toolA = aiTool( "toolA", "Tool A", () => {
		            msg2 = gw.parseInbound( { text: "Please mention BANANA.", userID: "u1", conversationID: "c1", threadID: "thread-steer" } )
		            session.handleInbound( msg2[ 1 ], gw )
		            return "A done"
		        } )
		        agent.withTools( [ toolA ] )

		        msg1 = gw.parseInbound( { text: "do toolA", userID: "u1", conversationID: "c1", threadID: "thread-steer" } )
		        session.handleInbound( msg1[ 1 ], gw ).get()

		        delivered = gw.getDeliveredEvents()
		        onlyOneCompletion = delivered.len() == 1 && delivered[ 1 ].getType() == "response.completed"

		        lastMessages = mockSvc.getReceivedRequests().last().messages
		        steeredReachedRequest = false
		        lastMessages.each( ( m ) => {
		            if ( isSimpleValue( m.content ?: "" ) && m.content contains "BANANA" ) {
		                steeredReachedRequest = true
		            }
		        } )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "onlyOneCompletion" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "steeredReachedRequest" ) ) ).isTrue();
	}

	@DisplayName( "interrupt policy: the current turn is cancelled cleanly, then the queued message dispatches as its own fresh turn" )
	@Test
	public void testInterruptPolicyCancelsThenDispatchesNext() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "Interrupt reply."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        gw = aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: [ gw ], policy: "interrupt" )

		        toolA = aiTool( "toolA", "Tool A", () => {
		            msg2 = gw.parseInbound( { text: "interrupt msg", userID: "u1", conversationID: "c1", threadID: "thread-interrupt" } )
		            session.handleInbound( msg2[ 1 ], gw )
		            return "A done"
		        } )
		        agent.withTools( [ toolA ] )

		        msg1 = gw.parseInbound( { text: "do toolA", userID: "u1", conversationID: "c1", threadID: "thread-interrupt" } )
		        session.handleInbound( msg1[ 1 ], gw ).get()

		        delivered = gw.getDeliveredEvents()
		        gotTwoDeliveries    = delivered.len() == 2
		        firstWasCancelled   = delivered[ 1 ].getData().content == ""
		        secondIsQueuedReply = delivered[ 2 ].getData().content == "Interrupt reply."
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "gotTwoDeliveries" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firstWasCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "secondIsQueuedReply" ) ) ).isTrue();
	}

	@DisplayName( "Thread-ID namespacing: two gateways sharing a raw conversation id get independent claims and both dispatch immediately" )
	@Test
	public void testThreadIdNamespacingAcrossGateways() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvcX = aiService( "mock" )
		        mockSvcX.setResponses( [ "response for X" ] )
		        modelX = new bxModules.bxai.models.runnables.AiModel( service: mockSvcX )
		        agentX = aiAgent( model: modelX )

		        gwA = aiGateway( "mock" )
		        gwB = aiGateway( "mock" )
		        // Two MockGateway instances default to the same name ("mock") — rename one so
		        // GatewaySession's own duplicate-name guard doesn't reject construction; the
		        // namespacing behavior under test is driven by gateway NAME either way.
		        gwB.setName( "mock-b" )

		        session = aiGatewaySession( agent: agentX, gateways: [ gwA, gwB ], policy: "reject" )

		        // Same raw conversationID on both gateways, NO explicit threadID — namespacing
		        // must fall back to "gatewayName:conversationID", so these resolve to different
		        // threads and neither blocks the other.
		        msgA = gwA.parseInbound( { text: "hi", userID: "u1", conversationID: "shared-conv" } )
		        msgB = gwB.parseInbound( { text: "hi", userID: "u1", conversationID: "shared-conv" } )

		        futureA = session.handleInbound( msgA[ 1 ], gwA )
		        futureB = session.handleInbound( msgB[ 1 ], gwB )

		        bothDispatchedImmediately = !isNull( futureA ) && !isNull( futureB )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "bothDispatchedImmediately" ) ) ).isTrue();
	}

	@DisplayName( "Reply routing: a message queued while it arrived via gateway B replies through gateway B, not gateway A" )
	@Test
	public void testReplyRoutingAcrossMultipleGateways() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "First.",
		            "From B."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        gwA = aiGateway( "mock" )
		        gwB = aiGateway( "mock" )
		        gwB.setName( "mock-b" )

		        session = aiGatewaySession( agent: agent, gateways: [ gwA, gwB ], policy: "queue" )

		        toolA = aiTool( "toolA", "Tool A", () => {
		            // Arrives via gateway B, while gateway A's message holds the claim.
		            msgB = gwB.parseInbound( { text: "via B", userID: "u1", conversationID: "c1", threadID: "thread-multi-gw" } )
		            session.handleInbound( msgB[ 1 ], gwB )
		            return "A done"
		        } )
		        agent.withTools( [ toolA ] )

		        msgA = gwA.parseInbound( { text: "via A", userID: "u1", conversationID: "c1", threadID: "thread-multi-gw" } )
		        session.handleInbound( msgA[ 1 ], gwA ).get()

		        aDeliveries = gwA.getDeliveredEvents()
		        bDeliveries = gwB.getDeliveredEvents()

		        aGotOnlyItsOwnReply = aDeliveries.len() == 1 && aDeliveries[ 1 ].getData().content == "First."
		        bGotItsOwnReply     = bDeliveries.len() == 1 && bDeliveries[ 1 ].getData().content == "From B."
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "aGotOnlyItsOwnReply" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bGotItsOwnReply" ) ) ).isTrue();
	}

	@DisplayName( "maxQueueDepth overflow: a message arriving past the configured depth falls back to an immediate rejection" )
	@Test
	public void testMaxQueueDepthOverflowFallsBackToReject() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "Outer final."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        gw = aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: [ gw ], policy: "queue", maxQueueDepth: 1 )

		        toolA = aiTool( "toolA", "Tool A", () => {
		            msg2 = gw.parseInbound( { text: "fills the queue", userID: "u1", conversationID: "c1", threadID: "thread-overflow" } )
		            session.handleInbound( msg2[ 1 ], gw )   // fills the 1-deep queue

		            msg3 = gw.parseInbound( { text: "overflows", userID: "u1", conversationID: "c1", threadID: "thread-overflow" } )
		            session.handleInbound( msg3[ 1 ], gw )   // should overflow -> immediate reject

		            return "A done"
		        } )
		        agent.withTools( [ toolA ] )

		        msg1 = gw.parseInbound( { text: "first", userID: "u1", conversationID: "c1", threadID: "thread-overflow" } )
		        session.handleInbound( msg1[ 1 ], gw ).get()

		        delivered = gw.getDeliveredEvents()
		        sawOverflowRejection = delivered.some( ( e ) => e.getType() == "message.rejected" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawOverflowRejection" ) ) ).isTrue();
	}

	@DisplayName( "cancelRun()/steerRun() no-op events don't fire for interrupt-policy-triggered cancellation: onAIAgentRunCancel DOES fire" )
	@Test
	public void testInterruptPolicyFiresOnAIAgentRunCancelEvent() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "Interrupt reply."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        eventFired = false
		        BoxRegisterInterceptor(
		            function( data ) { eventFired = true },
		            "onAIAgentRunCancel"
		        )

		        gw = aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: [ gw ], policy: "interrupt" )

		        toolA = aiTool( "toolA", "Tool A", () => {
		            msg2 = gw.parseInbound( { text: "interrupt msg", userID: "u1", conversationID: "c1", threadID: "thread-interrupt-event" } )
		            session.handleInbound( msg2[ 1 ], gw )
		            return "A done"
		        } )
		        agent.withTools( [ toolA ] )

		        msg1 = gw.parseInbound( { text: "do toolA", userID: "u1", conversationID: "c1", threadID: "thread-interrupt-event" } )
		        session.handleInbound( msg1[ 1 ], gw ).get()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "eventFired" ) ) ).isTrue();
	}

	@DisplayName( "isRunning() reflects start()/stop() on the session itself" )
	@Test
	public void testSessionIsRunningTracksLifecycle() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [ "hi" ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        gw = aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: [ gw ] )

		        notRunningInitially = !session.isRunning()
		        session.start()
		        runningAfterStart = session.isRunning()
		        session.stop()
		        notRunningAfterStop = !session.isRunning()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "notRunningInitially" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "runningAfterStart" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "notRunningAfterStop" ) ) ).isTrue();
	}

	@DisplayName( "getQueueDepth() reports buffered messages per thread and drops to 0 once fully drained" )
	@Test
	public void testGetQueueDepthTracksBufferedMessages() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "First.",
		            "Second.",
		            "Third."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        gw = aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: [ gw ], policy: "queue" )

		        depthDuringToolCall = -1
		        toolA = aiTool( "toolA", "Tool A", () => {
		            depthBefore = session.getQueueDepth( "thread-depth" )

		            msg2 = gw.parseInbound( { text: "second", userID: "u1", conversationID: "c1", threadID: "thread-depth" } )
		            session.handleInbound( msg2[ 1 ], gw )
		            msg3 = gw.parseInbound( { text: "third", userID: "u1", conversationID: "c1", threadID: "thread-depth" } )
		            session.handleInbound( msg3[ 1 ], gw )

		            depthDuringToolCall = session.getQueueDepth( "thread-depth" )
		            return "A done"
		        } )
		        agent.withTools( [ toolA ] )

		        depthBeforeAnythingStarts = session.getQueueDepth( "thread-depth" )

		        msg1 = gw.parseInbound( { text: "first", userID: "u1", conversationID: "c1", threadID: "thread-depth" } )
		        session.handleInbound( msg1[ 1 ], gw ).get()

		        depthAfterFullyDrained = session.getQueueDepth( "thread-depth" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "depthBeforeAnythingStarts" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "depthDuringToolCall" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "depthAfterFullyDrained" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "gateways accepts a single gateway name as a string, resolved via aiGateway() same as passing the instance directly" )
	@Test
	public void testSingleGatewayAsStringResolvesViaAiGateway() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [ "Hello back!" ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        // "mock" as a bare string instead of aiGateway( "mock" )
		        session = aiGatewaySession( agent: agent, gateways: "mock" )

		        gw = session.getGateways()[ "mock" ]
		        resolvedToRealGateway = !isNull( gw ) && gw.getName() == "mock"

		        messages = gw.parseInbound( { text: "hello", userID: "u1", conversationID: "c1", threadID: "thread-str" } )
		        session.handleInbound( messages[ 1 ], gw ).get()

		        delivered = gw.getDeliveredEvents()
		        gotReply  = delivered.len() == 1 && delivered[ 1 ].getData().content == "Hello back!"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resolvedToRealGateway" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "gotReply" ) ) ).isTrue();
	}

	@DisplayName( "gateways array can mix string names and IGateway instances" )
	@Test
	public void testGatewaysArrayMixesStringsAndInstances() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [ "hi" ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        httpGw = aiGateway( "http", { secret: "test-secret" } )
		        httpGw.setName( "http-b" )

		        // Mix: "mock" resolved by name, httpGw passed as an already-constructed instance
		        session = aiGatewaySession( agent: agent, gateways: [ "mock", httpGw ] )

		        gateways    = session.getGateways()
		        hasMock     = gateways.keyExists( "mock" )
		        hasHttpB    = gateways.keyExists( "http-b" )
		        sameHttpRef = gateways[ "http-b" ] == httpGw
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasMock" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasHttpB" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sameHttpRef" ) ) ).isTrue();
	}

	@DisplayName( "an unresolvable gateway name propagates aiGateway()'s own GatewayNotSupported error" )
	@Test
	public void testUnknownGatewayNameThrows() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [ "hi" ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )
		        agent = aiAgent( model: model )

		        threw = false
		        try {
		            aiGatewaySession( agent: agent, gateways: "totally-not-a-real-gateway" )
		        } catch ( "GatewayNotSupported" e ) {
		            threw = true
		        }
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threw" ) ) ).isTrue();
	}

}
