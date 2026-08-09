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
 * Integration tests proving that a terminal (suspend/cancel/reject) middleware result
 * actually halts the tool-calling loop end-to-end and reaches AiAgent — not just that
 * HumanInTheLoopMiddleware.beforeToolCall() returns the right AiMiddlewareResult in
 * isolation (coreMiddlewareTest.java already covers that).
 *
 * Before this fix, OpenAIService.chat()/chatStream() processed tool calls inside nested
 * .each() closures where a `return` on suspend only exited the inner callback — the
 * remaining tool calls in the batch still ran, and the recursive chat() call still fired,
 * so the suspension never reached AiAgent.run()/stream() and no checkpoint was ever saved.
 * The same pattern (direct tool.invoke() with no middleware hooks at all) meant
 * ClaudeService, BedrockService, and CohereService never fired beforeToolCall/afterToolCall
 * in the first place, so HITL middleware was a silent no-op for those providers.
 */
package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Suspend/Resume Propagation Integration Tests" )
public class SuspendResumeIntegrationTest extends BaseIntegrationTest {

	// ---- OpenAI-family (MockService reuses OpenAIService.chat()/chatStream() unmodified) ----

	@DisplayName( "Suspend stops the tool-call batch immediately and saves a checkpoint" )
	@Test
	public void testSuspendBlocksRemainingToolCallsAndSavesCheckpoint() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - no approval needed", () => { toolBCalls++; return "B done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} }, { name: "toolB", arguments: {} } ] }
		        ] )
		        model = new AiModel( service: mockSvc )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA" ], mode: "web" )
		        checkpointer = aiMemory( "cache" )

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ hitlMw ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        result = agent.run( "please run toolA and toolB", {}, { threadId: "hitl-test-suspend-blocks" } )

		        isSuspended     = isObject( result ) && result.isSuspended()
		        toolANotCalled  = toolACalls == 0
		        toolBNotCalled  = toolBCalls == 0

		        savedState      = checkpointer.loadState( "hitl-test-suspend-blocks" )
		        checkpointSaved = !savedState.isEmpty()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolANotCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBNotCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "checkpointSaved" ) ) ).isTrue();
	}

	@DisplayName( "Resume with 'approve' restarts the run, executes the tools, and completes" )
	@Test
	public void testResumeApproveExecutesToolsAndCompletes() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - no approval needed", () => { toolBCalls++; return "B done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            // Original attempt: neither tool runs yet — the whole batch suspends together
		            // once toolA (which needs approval) is seen, before any tool actually executes.
		            { toolCalls: [ { name: "toolA", arguments: {} }, { name: "toolB", arguments: {} } ] },
		            // Resume finishes that same batch directly (no LLM replay) and continues into
		            // the next turn once both tool results are sent back.
		            "All done."
		        ] )
		        model = new AiModel( service: mockSvc )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA" ], mode: "web" )
		        checkpointer = aiMemory( "cache" )

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ hitlMw ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        suspendedResult = agent.run( "please run toolA and toolB", {}, { threadId: "hitl-test-resume-approve" } )
		        wasSuspended    = isObject( suspendedResult ) && suspendedResult.isSuspended()

		        finalResult = agent.resume( "approve", "hitl-test-resume-approve" )

		        isFinalText = finalResult == "All done."
		        toolACalled = toolACalls == 1
		        toolBCalled = toolBCalls == 1
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "wasSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolACalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBCalled" ) ) ).isTrue();
	}

	@DisplayName( "Resume with 'reject' never invokes the tool, but the run continues and completes normally" )
	@Test
	public void testResumeRejectSkipsToolAndContinues() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            // Resume finishes the batch directly (no LLM replay) — this is the only other
		            // scripted response needed, for the turn after the rejection is sent back.
		            "Understood, I will not run toolA."
		        ] )
		        model = new AiModel( service: mockSvc )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA" ], mode: "web" )
		        checkpointer = aiMemory( "cache" )

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA ],
		            middleware  : [ hitlMw ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        agent.run( "please run toolA", {}, { threadId: "hitl-test-resume-reject" } )

		        // A rejected tool call is NOT a hard stop: it's absorbed as tool feedback and the
		        // run continues, producing a normal completion — not a raw AiMiddlewareResult.
		        finalResult         = agent.resume( "reject", "hitl-test-resume-reject" )
		        isNormalCompletion  = finalResult == "Understood, I will not run toolA."
		        toolNotCalled       = toolACalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isNormalCompletion" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNotCalled" ) ) ).isTrue();
	}

	@DisplayName( "Resume with 'approve_always' through an async gateway records a durable grant that auto-approves a later, separate run" )
	@Test
	public void testResumeApproveAlwaysThroughAsyncGatewayRecordsGrant() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
				import bxModules.bxai.models.runnables.AiModel;

				toolACalls = 0
				toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )

				mockSvc = aiService( "mock" )
				mockSvc.setResponses( [
					// Run 1: suspends (the Mock gateway is unscripted, so it's asynchronous)
					{ toolCalls: [ { name: "toolA", arguments: {} } ] },
					// Resume finishes the batch directly (no LLM replay) — approve_always
					// resolves the pending item and records a durable grant
					"Run 1 done.",
					// Run 2 (separate thread, same identity): should auto-approve via the
					// recorded grant and never suspend at all
					{ toolCalls: [ { name: "toolA", arguments: {} } ] },
					"Run 2 done."
				] )
				model = new AiModel( service: mockSvc )

				gw = aiGateway( "mock" )
				hitlMw = new HumanInTheLoopMiddleware(
					toolsRequiringApproval: [ "toolA" ],
					gateway               : gw,
					decisionStore         : aiDecisionStore( "cache" )
				)
				checkpointer = aiMemory( "cache" )

				agent = aiAgent(
					model       : model,
					tools       : [ toolA ],
					middleware  : [ hitlMw ],
					checkpointer: checkpointer,
					checkpointTTL: 5
				)

				// Run 1: suspends — the gateway presented the request but nothing resolved it yet
				r1 = agent.run( "please run toolA", {}, { threadId: "grant-test-t1", userId: "alice" } )
				r1Suspended = isObject( r1 ) && r1.isSuspended()
				presentedAfterRun1 = gw.getPendingInteractions().len()

				// Resume with approve_always, attributing the decision to alice
				finalResult1 = agent.resume( "approve_always", "grant-test-t1", {}, "alice", "" )
				isFinal1 = finalResult1 == "Run 1 done."

				// Run 2: a completely separate thread, same identity — the durable grant recorded
				// on resume above should auto-approve this without ever presenting to the gateway
				r2 = agent.run( "please run toolA again", {}, { threadId: "grant-test-t2", userId: "alice" } )
				r2NotSuspended = !( isObject( r2 ) && r2.isSuspended() )
				isFinal2 = r2 == "Run 2 done."
				presentedAfterRun2 = gw.getPendingInteractions().len()

				toolCalledTwice = toolACalls == 2
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "r1Suspended" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "presentedAfterRun1" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "isFinal1" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "r2NotSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinal2" ) ) ).isTrue();
		// Run 2 never presented to the gateway at all — the grant short-circuited it
		assertThat( variables.getAsInteger( Key.of( "presentedAfterRun2" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "toolCalledTwice" ) ) ).isTrue();
	}

	@DisplayName( "A rejected tool call does not block the rest of the batch — an unrelated, safe tool call still runs" )
	@Test
	public void testRejectContinuesToRemainingToolCallsInBatch() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.GuardrailMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        dangerousCalls = 0
		        safeCalls      = 0
		        dangerousTool  = aiTool( "runQuery", "Run a SQL query", ( required string sql ) => { dangerousCalls++; return "rows" } )
		        safeTool       = aiTool( "getWeather", "Get the weather", () => { safeCalls++; return "sunny" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "runQuery", arguments: { sql: "DROP TABLE users" } }, { name: "getWeather", arguments: {} } ] },
		            "I avoided dropping the table and checked the weather."
		        ] )
		        model = new AiModel( service: mockSvc )

		        guardMw = new GuardrailMiddleware( argPatterns: { runQuery: [ { sql: "(?i)\\bDROP\\b" } ] } )

		        agent = aiAgent( model: model, tools: [ dangerousTool, safeTool ], middleware: [ guardMw ] )

		        result = agent.run( "Drop the users table and check the weather" )

		        isNormalCompletion  = result == "I avoided dropping the table and checked the weather."
		        dangerousToolBlocked = dangerousCalls == 0
		        safeToolStillRan     = safeCalls == 1
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isNormalCompletion" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "dangerousToolBlocked" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "safeToolStillRan" ) ) ).isTrue();
	}

	@DisplayName( "Cancel (unlike reject) still hard-stops the entire batch immediately" )
	@Test
	public void testCancelStopsWholeBatch() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} }, { name: "toolB", arguments: {} } ] }
		        ] )
		        model = new AiModel( service: mockSvc )

		        cancelMw = {
		            "beforeToolCall": ( ctx ) => AiMiddlewareResult.cancel( "Max tool calls exceeded" )
		        }

		        agent = aiAgent( model: model, tools: [ toolA, toolB ], middleware: [ cancelMw ] )

		        result = agent.run( "please run toolA and toolB" )

		        isCancelled    = isObject( result ) && result.isCancelled()
		        neitherToolRan = toolACalls == 0 && toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherToolRan" ) ) ).isTrue();
	}

	@DisplayName( "Two tool calls needing approval in the same turn suspend ONCE, as one batch — not one at a time" )
	@Test
	public void testMultiplePendingToolCallsSuspendAsOneBatch() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            // Both tool calls need approval — the whole turn suspends together, before
		            // EITHER tool runs, instead of stopping after evaluating only toolA.
		            { toolCalls: [ { name: "toolA", arguments: {} }, { name: "toolB", arguments: {} } ] }
		        ] )
		        model = new AiModel( service: mockSvc )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" )
		        checkpointer = aiMemory( "cache" )

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ hitlMw ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        result = agent.run( "please run toolA and toolB", {}, { threadId: "hitl-test-batch-suspend" } )

		        isSuspended     = isObject( result ) && result.isSuspended()
		        pendingActions  = result.getData().pendingActions ?: []
		        bothPending     = pendingActions.len() == 2
		        pendingNames    = pendingActions.map( ( a ) => a.toolName ).sort( "textnocase" ).toList()
		        neitherRanYet   = toolACalls == 0 && toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothPending" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "pendingNames" ) ).toString() ).isEqualTo( "toolA,toolB" );
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
	}

	@DisplayName( "Resume with an array of per-call decisions resolves a batch suspension individually" )
	@Test
	public void testResumeWithArrayOfDecisionsResolvesBatchIndividually() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} }, { name: "toolB", arguments: {} } ] },
		            // Resume finishes the batch directly (no LLM replay) — one more scripted
		            // response for the turn after both tool results are sent back.
		            "toolA ran, toolB was rejected."
		        ] )
		        model = new AiModel( service: mockSvc )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" )
		        checkpointer = aiMemory( "cache" )

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ hitlMw ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        agent.run( "please run toolA and toolB", {}, { threadId: "hitl-test-batch-array-resume" } )

		        // One decision per pending tool call, in the order they were presented (toolA, toolB)
		        finalResult = agent.resume(
		            [
		                { decision: "approve" },
		                { decision: "reject", reason: "not needed" }
		            ],
		            "hitl-test-batch-array-resume"
		        )

		        isFinalText  = finalResult == "toolA ran, toolB was rejected."
		        toolARan     = toolACalls == 1
		        toolBSkipped = toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBSkipped" ) ) ).isTrue();
	}

	@DisplayName( "Streaming: a suspended tool call emits a middleware_stop sentinel and saves a checkpoint" )
	@Test
	public void testStreamSuspendEmitsSentinelAndSavesCheckpoint() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] }
		        ] )
		        model = new AiModel( service: mockSvc )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA" ], mode: "web" )
		        checkpointer = aiMemory( "cache" )

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA ],
		            middleware  : [ hitlMw ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        chunks = []
		        agent.stream(
		            ( chunk ) => { chunks.append( chunk ) },
		            "please run toolA",
		            {},
		            { threadId: "hitl-test-stream-suspend" }
		        )

		        sawMiddlewareStop = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" && c.result.isSuspended() )
		        toolNotCalled     = toolACalls == 0

		        savedState        = checkpointer.loadState( "hitl-test-stream-suspend" )
		        checkpointSaved   = !savedState.isEmpty()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawMiddlewareStop" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNotCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "checkpointSaved" ) ) ).isTrue();
	}

	// ---- Claude / Bedrock / Cohere: newly-wired beforeToolCall/afterToolCall coverage ----
	// These providers previously invoked tools directly with no middleware hooks at all.
	// Deterministic / credential-free: a wrapLLMCall middleware returns a canned tool-call
	// response so no HTTP call is made, exercising only the tool-call loop fix.

	@DisplayName( "ClaudeService: beforeToolCall now fires and a suspend stops the tool chain" )
	@Test
	public void testClaudeToolCallSuspendStopsChain() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => "deleted" )
		        provider = aiService( "claude", { apiKey: "dummy-key" } )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "Delete record 5" ),
		            { model: "claude-sonnet-4-5", tools: [ tool ] },
		            { provider: "claude" }
		        )

		        wrapCallCount       = 0
		        beforeToolCallFired = false
		        capturedToolArgs    = {}

		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCallCount++
		                return {
		                    "content": [ { "type": "tool_use", "id": "call_1", "name": "deleteRecord", "input": { "id": "5" } } ],
		                    "stop_reason": "tool_use",
		                    "usage": { "input_tokens": 5, "output_tokens": 5 }
		                }
		            },
		            "beforeToolCall": ( ctx ) => {
		                beforeToolCallFired = true
		                capturedToolArgs    = ctx.toolArgs ?: {}
		                return AiMiddlewareResult.suspend( { toolName: ctx.toolCall.name } )
		            }
		        } )

		        result = provider.chat( chatRequest )

		        isSuspended    = isObject( result ) && result.isSuspended()
		        firedOnce      = wrapCallCount == 1
		        // GuardrailMiddleware/FlightRecorderMiddleware-style argument inspection: prove
		        // ctx.toolArgs carries the real arguments instead of being empty/absent.
		        sawRealArgs    = capturedToolArgs.keyExists( "id" ) && capturedToolArgs.id == "5"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "beforeToolCallFired" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firedOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawRealArgs" ) ) ).isTrue();
	}

	@DisplayName( "ClaudeService: HITL 'edit' resume decision patches toolCall.input and the tool receives the edited arguments" )
	@Test
	public void testClaudeEditResumePatchesToolInput() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

		        capturedId = ""
		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => { capturedId = id; return "deleted:" & id } )
		        provider = aiService( "claude", { apiKey: "dummy-key" } )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "web" )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "Delete record 5" ),
		            { model: "claude-sonnet-4-5", tools: [ tool ] },
		            {
		                provider: "claude",
		                _resumeContext: {
		                    resumeDecision: "edit",
		                    suspendData   : { toolName: "deleteRecord" },
		                    editedData    : { correctedArgs: { id: "999" } }
		                }
		            }
		        )
		        chatRequest.addMiddleware( hitlMw )
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                return {
		                    "content": [ { "type": "tool_use", "id": "call_1", "name": "deleteRecord", "input": { "id": "5" } } ],
		                    "stop_reason": "tool_use",
		                    "usage": { "input_tokens": 5, "output_tokens": 5 }
		                }
		            }
		        } )

		        result = provider.chat( chatRequest )

		        // The tool must have received the EDITED id ("999"), not the original ("5")
		        gotEditedArgs = capturedId == "999"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "gotEditedArgs" ) ) ).isTrue();
	}

	@DisplayName( "BedrockService: beforeToolCall now fires and a suspend stops the tool chain" )
	@Test
	public void testBedrockToolCallSuspendStopsChain() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => "deleted" )
		        provider = aiService(
		            "bedrock",
		            {
		                awsAccessKeyId    : "AKIAIOSFODNN7EXAMPLE",
		                awsSecretAccessKey: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
		                region            : "us-east-1"
		            }
		        )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "Delete record 5" ),
		            { model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
		            { provider: "bedrock" }
		        )

		        wrapCallCount       = 0
		        beforeToolCallFired = false
		        capturedToolArgs    = {}

		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCallCount++
		                return {
		                    "content": [ { "type": "tool_use", "id": "call_1", "name": "deleteRecord", "input": { "id": "5" } } ],
		                    "stop_reason": "tool_use",
		                    "usage": { "input_tokens": 5, "output_tokens": 5 }
		                }
		            },
		            "beforeToolCall": ( ctx ) => {
		                beforeToolCallFired = true
		                capturedToolArgs    = ctx.toolArgs ?: {}
		                return AiMiddlewareResult.suspend( { toolName: ctx.toolCall.name } )
		            }
		        } )

		        result = provider.chat( chatRequest )

		        isSuspended = isObject( result ) && result.isSuspended()
		        firedOnce   = wrapCallCount == 1
		        sawRealArgs = capturedToolArgs.keyExists( "id" ) && capturedToolArgs.id == "5"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "beforeToolCallFired" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firedOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawRealArgs" ) ) ).isTrue();
	}

	@DisplayName( "BedrockService: HITL 'edit' resume decision patches toolCall.input and the tool receives the edited arguments" )
	@Test
	public void testBedrockEditResumePatchesToolInput() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

		        capturedId = ""
		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => { capturedId = id; return "deleted:" & id } )
		        provider = aiService(
		            "bedrock",
		            {
		                awsAccessKeyId    : "AKIAIOSFODNN7EXAMPLE",
		                awsSecretAccessKey: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
		                region            : "us-east-1"
		            }
		        )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "web" )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "Delete record 5" ),
		            { model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
		            {
		                provider: "bedrock",
		                _resumeContext: {
		                    resumeDecision: "edit",
		                    suspendData   : { toolName: "deleteRecord" },
		                    editedData    : { correctedArgs: { id: "999" } }
		                }
		            }
		        )
		        chatRequest.addMiddleware( hitlMw )
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                return {
		                    "content": [ { "type": "tool_use", "id": "call_1", "name": "deleteRecord", "input": { "id": "5" } } ],
		                    "stop_reason": "tool_use",
		                    "usage": { "input_tokens": 5, "output_tokens": 5 }
		                }
		            }
		        } )

		        result = provider.chat( chatRequest )

		        gotEditedArgs = capturedId == "999"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "gotEditedArgs" ) ) ).isTrue();
	}

	@DisplayName( "CohereService: beforeToolCall now fires and a suspend stops the tool chain" )
	@Test
	public void testCohereToolCallSuspendStopsChain() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => "deleted" )
		        provider = aiService( "cohere", { apiKey: "dummy-key" } )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "Delete record 5" ),
		            { model: "command-a-03-2025", tools: [ tool ] },
		            { provider: "cohere" }
		        )

		        wrapCallCount       = 0
		        beforeToolCallFired = false
		        capturedToolArgs    = {}

		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCallCount++
		                return {
		                    "text"     : "",
		                    "tool_calls": [ { "name": "deleteRecord", "parameters": { "id": "5" } } ]
		                }
		            },
		            "beforeToolCall": ( ctx ) => {
		                beforeToolCallFired = true
		                capturedToolArgs    = ctx.toolArgs ?: {}
		                return AiMiddlewareResult.suspend( { toolName: ctx.toolCall.name } )
		            }
		        } )

		        result = provider.chat( chatRequest )

		        isSuspended = isObject( result ) && result.isSuspended()
		        firedOnce   = wrapCallCount == 1
		        sawRealArgs = capturedToolArgs.keyExists( "id" ) && capturedToolArgs.id == "5"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "beforeToolCallFired" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firedOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawRealArgs" ) ) ).isTrue();
	}

	@DisplayName( "CohereService: HITL 'edit' resume decision patches toolCall.parameters and the tool receives the edited arguments" )
	@Test
	public void testCohereEditResumePatchesToolParameters() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

		        capturedId = ""
		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => { capturedId = id; return "deleted:" & id } )
		        provider = aiService( "cohere", { apiKey: "dummy-key" } )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "web" )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "Delete record 5" ),
		            { model: "command-a-03-2025", tools: [ tool ] },
		            {
		                provider: "cohere",
		                _resumeContext: {
		                    resumeDecision: "edit",
		                    suspendData   : { toolName: "deleteRecord" },
		                    editedData    : { correctedArgs: { id: "999" } }
		                }
		            }
		        )
		        chatRequest.addMiddleware( hitlMw )
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                return {
		                    "text"     : "",
		                    "tool_calls": [ { "name": "deleteRecord", "parameters": { "id": "5" } } ]
		                }
		            }
		        } )

		        result = provider.chat( chatRequest )

		        gotEditedArgs = capturedId == "999"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "gotEditedArgs" ) ) ).isTrue();
	}

	// ---- Streaming: hard-stop terminal results (suspend/cancel) must short-circuit; reject must not ----

	@DisplayName( "Streaming: a cancelled tool call short-circuits without firing afterAgentRun/storeInMemory" )
	@Test
	public void testStreamCancelShortCircuitsWithoutCompletion() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] }
		        ] )
		        model = new AiModel( service: mockSvc )

		        cancelMw = {
		            "beforeToolCall": ( ctx ) => AiMiddlewareResult.cancel( "Max tool calls exceeded" )
		        }

		        afterAgentRunFired = false
		        auditMw = {
		            "afterAgentRun": ( ctx ) => {
		                afterAgentRunFired = true
		                return AiMiddlewareResult.continue()
		            }
		        }

		        agent = aiAgent(
		            model     : model,
		            tools     : [ toolA ],
		            middleware: [ cancelMw, auditMw ]
		        )

		        chunks = []
		        agent.stream(
		            ( chunk ) => { chunks.append( chunk ) },
		            "please run toolA",
		            {},
		            { threadId: "hitl-test-stream-cancel" }
		        )

		        sawCancelSentinel = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" && c.result.isCancelled() )
		        toolNotCalled     = toolACalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawCancelSentinel" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNotCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "afterAgentRunFired" ) ) ).isFalse();
	}

	@DisplayName( "Streaming: a rejected tool call does NOT short-circuit — it's absorbed and the stream completes normally" )
	@Test
	public void testStreamRejectDoesNotShortCircuit() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "Understood, I will not run toolA."
		        ] )
		        model = new AiModel( service: mockSvc )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA" ], mode: "web" )

		        chunks = []
		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA ],
		            middleware  : [ hitlMw ],
		            checkpointer: aiMemory( "cache" )
		        )

		        // Simulate a resume-time rejection by pre-seeding the resumeContext directly via
		        // options, so beforeToolCall returns reject() on the very first tool call.
		        agent.stream(
		            ( chunk ) => { chunks.append( chunk ) },
		            "please run toolA",
		            {},
		            {
		                threadId       : "hitl-test-stream-reject",
		                _resumeContext : {
		                    resumeDecision: "reject",
		                    suspendData   : { toolName: "toolA" },
		                    editedData    : {}
		                }
		            }
		        )

		        sawMiddlewareStop = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        sawFinalContent   = chunks.some( c => isStruct( c ) && ( c.choices?.first()?.delta?.content ?: "" ) contains "Understood" )
		        toolNotCalled     = toolACalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawMiddlewareStop" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "sawFinalContent" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNotCalled" ) ) ).isTrue();
	}

}
