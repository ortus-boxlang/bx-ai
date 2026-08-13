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
 * Tests for AiAgent.cancelRun()/steerRun(): the public, threadId-addressable API an external
 * caller uses to cancel or steer an AiAgent run already in flight. Every agent auto-attaches a
 * RunControlMiddleware internally (see RunControlMiddleware.bx) — there is no user-constructed
 * token or middleware in this design, only threadId in, boolean out.
 */
package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "AiAgent Run Control (cancelRun/steerRun) Tests" )
public class RunControlMiddlewareTest extends BaseIntegrationTest {

	@DisplayName( "cancelRun()/steerRun() on a threadId with no active run are safe no-ops" )
	@Test
	public void testNoActiveRunIsSafeNoOp() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [ "hi there" ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )

		        agent = aiAgent( model: model )

		        cancelResult = agent.cancelRun( "never-started-thread" )
		        steerResult  = agent.steerRun( "never-started-thread", "hello" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "cancelResult" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "steerResult" ) ) ).isFalse();
	}

	@DisplayName( "cancelRun() mid-loop stops the run before the next tool call; already-run tool call still ran" )
	@Test
	public void testCancelRunMidLoop() {
		// @formatter:off
		runtime.executeSource(
		    """
		        toolACalls = 0
		        toolBCalls = 0

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            { toolCalls: [ { name: "toolB", arguments: {} } ] },
		            "Final answer."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )

		        agent = aiAgent( model: model )

		        toolA = aiTool( "toolA", "Tool A", () => {
		            toolACalls++
		            agent.cancelRun( "thread-cancel-mid-loop", "stop after A" )
		            return "A done"
		        } )
		        toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )
		        agent.withTools( [ toolA, toolB ] )

		        result = agent.run( "do toolA then toolB", {}, { threadId: "thread-cancel-mid-loop" } )

		        isCancelled = isObject( result ) && result.isCancelled()
		        aRanBNot    = toolACalls == 1 && toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "aRanBNot" ) ) ).isTrue();
	}

	@DisplayName( "steerRun() mid-loop injects a message and the run completes normally, reflecting it" )
	@Test
	public void testSteerRunMidLoopCompletesNormally() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "Final answer with BANANA mentioned."
		        ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )

		        agent = aiAgent( model: model )

		        toolA = aiTool( "toolA", "Tool A", () => {
		            agent.steerRun( "thread-steer-mid-loop", "Please also mention BANANA." )
		            return "A done"
		        } )
		        agent.withTools( [ toolA ] )

		        result = agent.run( "do toolA", {}, { threadId: "thread-steer-mid-loop" } )

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

	@DisplayName( "Two threads running on the same agent don't cross-contaminate: cancelling one doesn't affect the other" )
	@Test
	public void testTwoThreadsDoNotCrossContaminate() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvcX = aiService( "mock" )
		        mockSvcX.setResponses( [ "response for X" ] )
		        modelX = new bxModules.bxai.models.runnables.AiModel( service: mockSvcX )
		        agentX = aiAgent( model: modelX )

		        mockSvcY = aiService( "mock" )
		        mockSvcY.setResponses( [ "response for Y" ] )
		        modelY = new bxModules.bxai.models.runnables.AiModel( service: mockSvcY )
		        agentY = aiAgent( model: modelY )

		        // Cancelling thread "x" on agentX must not touch agentY's independent thread "y"
		        resultX = agentX.run( "hello x", {}, { threadId: "thread-x" } )
		        resultY = agentY.run( "hello y", {}, { threadId: "thread-y" } )

		        xUnaffected = resultX == "response for X"
		        yUnaffected = resultY == "response for Y"

		        // Both threads are finished now — cancelling either is a safe no-op
		        cancelXAfterFinish = agentX.cancelRun( "thread-x" )
		        cancelYAfterFinish = agentY.cancelRun( "thread-y" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "xUnaffected" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "yUnaffected" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "cancelXAfterFinish" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "cancelYAfterFinish" ) ) ).isFalse();
	}

	@DisplayName( "A run that is never cancelled or steered completes normally, unaffected by run control" )
	@Test
	public void testCleanRunNoInterference() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [ "Just a plain answer." ] )
		        model = new bxModules.bxai.models.runnables.AiModel( service: mockSvc )

		        agent = aiAgent( model: model )
		        result = agent.run( "say hi", {}, { threadId: "thread-clean" } )

		        unaffected = result == "Just a plain answer."
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "unaffected" ) ) ).isTrue();
	}

}
