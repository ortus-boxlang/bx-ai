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
 * Tests for CancellationToken + RunControlMiddleware: an external caller cancelling or
 * steering an AiAgent run already in flight, checked at beforeLLMCall/beforeToolCall —
 * the finest-grained checkpoints a provider's tool-loop fires.
 */
package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "CancellationToken + RunControlMiddleware Tests" )
public class RunControlMiddlewareTest extends BaseIntegrationTest {

	// ---- CancellationToken in isolation ----

	@DisplayName( "CancellationToken: starts uncancelled with no pending steer" )
	@Test
	public void testTokenInitialState() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.runnables.CancellationToken;

		        token = new CancellationToken();

		        notCancelled  = !token.isCancelled();
		        noPendingSteer = !token.hasPendingSteer();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "notCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noPendingSteer" ) ) ).isTrue();
	}

	@DisplayName( "CancellationToken: cancel() sets cancelled + reason" )
	@Test
	public void testTokenCancel() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.runnables.CancellationToken;

		        token = new CancellationToken();
		        token.cancel( "stop it" );

		        isCancelled  = token.isCancelled();
		        reasonCorrect = token.getCancelReason() == "stop it";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "reasonCorrect" ) ) ).isTrue();
	}

	@DisplayName( "CancellationToken: steer() queues messages, consumeSteerMessages() drains them exactly once" )
	@Test
	public void testTokenSteerQueueDrains() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.runnables.CancellationToken;

		        token = new CancellationToken();
		        token.steer( "first" );
		        token.steer( "second" );

		        hasPendingBeforeDrain = token.hasPendingSteer();
		        drained = token.consumeSteerMessages();
		        drainedTwo = drained.len() == 2;
		        drainedInOrder = drained[ 1 ] == "first" && drained[ 2 ] == "second";
		        hasPendingAfterDrain = token.hasPendingSteer();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasPendingBeforeDrain" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "drainedTwo" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "drainedInOrder" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasPendingAfterDrain" ) ) ).isFalse();
	}

	// ---- RunControlMiddleware in isolation ----

	@DisplayName( "RunControlMiddleware: continues when token is not cancelled and has no pending steer" )
	@Test
	public void testMiddlewareContinuesWhenClean() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.RunControlMiddleware;
		        import bxModules.bxai.models.runnables.CancellationToken;

		        token = new CancellationToken();
		        mw    = new RunControlMiddleware( token );

		        r1 = mw.beforeLLMCall( context: {} );
		        r2 = mw.beforeToolCall( context: {} );

		        bothContinue = r1.isContinue() && r2.isContinue();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "bothContinue" ) ) ).isTrue();
	}

	@DisplayName( "RunControlMiddleware: cancelled token produces a cancel result with the token's reason" )
	@Test
	public void testMiddlewareCancels() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.RunControlMiddleware;
		        import bxModules.bxai.models.runnables.CancellationToken;

		        token = new CancellationToken();
		        mw    = new RunControlMiddleware( token );
		        token.cancel( "external stop" );

		        result = mw.beforeLLMCall( context: {} );

		        isCancelled  = result.isCancelled();
		        reasonCorrect = result.getReason() == "external stop";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "reasonCorrect" ) ) ).isTrue();
	}

	@DisplayName( "RunControlMiddleware: pending steer appends to chatRequest messages and continues" )
	@Test
	public void testMiddlewareSteersAppendsMessage() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.RunControlMiddleware;
		        import bxModules.bxai.models.runnables.CancellationToken;
		        import bxModules.bxai.models.requests.AiChatRequest;

		        token = new CancellationToken();
		        mw    = new RunControlMiddleware( token );
		        token.steer( "extra instruction" );

		        chatRequest = new AiChatRequest( aiMessage( [ { role: "user", content: "original" } ] ) );
		        result = mw.beforeLLMCall( context: { chatRequest: chatRequest } );

		        messages = chatRequest.getMessages();
		        appended = messages.len() == 2 && messages[ 2 ].role == "user" && messages[ 2 ].content == "extra instruction";
		        stillContinues = result.isContinue();
		        drainedAfter = !token.hasPendingSteer();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "appended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "stillContinues" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "drainedAfter" ) ) ).isTrue();
	}

	// ---- End-to-end against a mock-provider agent (same aiService("mock").setResponses()
	// ---- instance-queue pattern SuspendResumeIntegrationTest.java already uses) ----

	@DisplayName( "End-to-end: cancelling before run() starts stops it before any tool call executes" )
	@Test
	public void testEndToEndCancelBeforeRun() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.RunControlMiddleware;
		        import bxModules.bxai.models.runnables.CancellationToken;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "Final answer."
		        ] )
		        model = new AiModel( service: mockSvc )

		        token = new CancellationToken()
		        agent = aiAgent( model: model, tools: [ toolA ], middleware: [ new RunControlMiddleware( token ) ] )

		        token.cancel( "stop before start" )
		        result = agent.run( "do toolA" )

		        isCancelled  = isObject( result ) && result.isCancelled()
		        neitherRan   = toolACalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherRan" ) ) ).isTrue();
	}

	@DisplayName( "End-to-end: cancelling mid-loop stops before the next tool call, first tool call already ran" )
	@Test
	public void testEndToEndCancelMidLoop() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.RunControlMiddleware;
		        import bxModules.bxai.models.runnables.CancellationToken;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        token = new CancellationToken()

		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; token.cancel( "stop after A" ); return "A done" } )
		        toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            { toolCalls: [ { name: "toolB", arguments: {} } ] },
		            "Final answer."
		        ] )
		        model = new AiModel( service: mockSvc )

		        agent = aiAgent( model: model, tools: [ toolA, toolB ], middleware: [ new RunControlMiddleware( token ) ] )
		        result = agent.run( "do toolA then toolB" )

		        isCancelled = isObject( result ) && result.isCancelled()
		        aRanBNot    = toolACalls == 1 && toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "aRanBNot" ) ) ).isTrue();
	}

	@DisplayName( "End-to-end: steering mid-loop injects a message and the run completes normally, reflecting it" )
	@Test
	public void testEndToEndSteerMidLoopCompletesNormally() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.RunControlMiddleware;
		        import bxModules.bxai.models.runnables.CancellationToken;
		        import bxModules.bxai.models.runnables.AiModel;

		        token = new CancellationToken()
		        toolA = aiTool( "toolA", "Tool A", () => { token.steer( "Please also mention BANANA." ); return "A done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "Final answer with BANANA mentioned."
		        ] )
		        model = new AiModel( service: mockSvc )

		        agent = aiAgent( model: model, tools: [ toolA ], middleware: [ new RunControlMiddleware( token ) ] )
		        result = agent.run( "do toolA" )

		        completedNormally = !isObject( result )
		        gotFinalAnswer    = result == "Final answer with BANANA mentioned."

		        // Confirm the steered message actually reached the 2nd request the mock received
		        lastMessages = mockSvc.getReceivedRequests().last().messages
		        foundSteered = false
		        lastMessages.each( ( m ) => {
		            if ( isSimpleValue( m.content ?: "" ) && m.content contains "BANANA" ) {
		                foundSteered = true
		            }
		        } )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "completedNormally" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "gotFinalAnswer" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "foundSteered" ) ) ).isTrue();
	}

	@DisplayName( "End-to-end: an untouched token never interferes with a normal run" )
	@Test
	public void testEndToEndCleanTokenNoInterference() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.RunControlMiddleware;
		        import bxModules.bxai.models.runnables.CancellationToken;
		        import bxModules.bxai.models.runnables.AiModel;

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [ "Just a plain answer." ] )
		        model = new AiModel( service: mockSvc )

		        token = new CancellationToken()
		        agent = aiAgent( model: model, middleware: [ new RunControlMiddleware( token ) ] )

		        result = agent.run( "say hi" )
		        unaffected = result == "Just a plain answer."
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "unaffected" ) ) ).isTrue();
	}

}
