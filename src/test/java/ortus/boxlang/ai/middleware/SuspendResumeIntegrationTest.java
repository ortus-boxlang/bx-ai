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
 *
 * Streaming is covered for Bedrock and Cohere too (issue #248): both chatStream()s now run the
 * same batch-suspend tool loop as chat() and honour `_resumeContext` via resumeToolBatchStream(),
 * and both route their stream transport through wrapLLMCall — Bedrock's returns a canned AWS
 * event-stream body, Cohere's replays v1 SSE events through the `emitSSEChunk` callback on the
 * middleware context — so the whole agent round trip (stream → batch suspend → checkpoint →
 * resumeStream()) runs offline and deterministically here.
 *
 * Both providers' streaming resumes are exercised through AiAgent.resumeStream(). Bedrock's used to
 * be driven at provider level only: it serializes its streaming request body BEFORE the wrapLLMCall
 * seam, and ClosureTool.doInvoke() mutated the caller's live args struct — the tool_use block's
 * `input` in message history — injecting a `_chatRequest` back-reference, so a second streamed
 * Bedrock turn serialized a cyclic graph and died with a StackOverflowError. doInvoke() now coerces
 * and injects into a shallow copy, and testBedrockStreamResumeApproveAllCompletesWithoutReplay
 * asserts the follow-up body carries no `_chatRequest`. ClosureToolTest pins the injection rule
 * itself at unit level (only a callable that declares `_chatRequest` receives it, and the caller's
 * args struct is never mutated).
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

	@DisplayName( "Streaming: two pending tool calls suspend as ONE batch, resumeStream() with an array resolves them individually" )
	@Test
	public void testStreamMultiplePendingToolCallsBatchAndArrayResume() {
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
		            // resumeStream() finishes the batch directly (no LLM replay)
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

		        chunks = []
		        agent.stream(
		            ( chunk ) => { chunks.append( chunk ) },
		            "please run toolA and toolB",
		            {},
		            { threadId: "hitl-test-stream-batch" }
		        )

		        stopChunk       = chunks.filter( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" ).first()
		        isSuspended     = !isNull( stopChunk ) && stopChunk.result.isSuspended()
		        pendingActions  = !isNull( stopChunk ) ? ( stopChunk.result.getData().pendingActions ?: [] ) : []
		        bothPending     = pendingActions.len() == 2
		        neitherRanYet   = toolACalls == 0 && toolBCalls == 0

		        resumeChunks = []
		        agent.resumeStream(
		            ( chunk ) => { resumeChunks.append( chunk ) },
		            [
		                { decision: "approve" },
		                { decision: "reject", reason: "not needed" }
		            ],
		            "hitl-test-stream-batch"
		        )

		        finalText = resumeChunks
		            .filter( c => isStruct( c ) && c.keyExists( "choices" ) )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )
		        isFinalText  = finalText == "toolA ran, toolB was rejected."
		        toolARan     = toolACalls == 1
		        toolBSkipped = toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothPending" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBSkipped" ) ) ).isTrue();
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

	@DisplayName( "ClaudeService: two tool calls needing approval suspend as ONE batch, resume with an array resolves them individually" )
	@Test
	public void testClaudeBatchSuspendAndArrayResume() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        provider = aiService( "claude", { apiKey: "dummy-key" } )
		        model    = new AiModel( service: provider )

		        callCount = 0
		        cannedLLM = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                callCount++
		                if ( callCount == 1 ) {
		                    return {
		                        "content": [
		                            { "type": "tool_use", "id": "call_a", "name": "toolA", "input": {} },
		                            { "type": "tool_use", "id": "call_b", "name": "toolB", "input": {} }
		                        ],
		                        "stop_reason": "tool_use",
		                        "usage": { "input_tokens": 5, "output_tokens": 5 }
		                    }
		                }
		                return {
		                    "content": [ { "type": "text", "text": "toolA ran, toolB was rejected." } ],
		                    "stop_reason": "end_turn",
		                    "usage": { "input_tokens": 5, "output_tokens": 5 }
		                }
		            }
		        }

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" )
		        checkpointer = aiMemory( "cache" )

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ hitlMw, cannedLLM ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        suspendedResult = agent.run( "please run toolA and toolB", {}, { threadId: "claude-batch-test" } )
		        isSuspended     = isObject( suspendedResult ) && suspendedResult.isSuspended()
		        pendingActions  = suspendedResult.getData().pendingActions ?: []
		        bothPending     = pendingActions.len() == 2
		        neitherRanYet   = toolACalls == 0 && toolBCalls == 0

		        finalResult = agent.resume(
		            [
		                { decision: "approve" },
		                { decision: "reject", reason: "not needed" }
		            ],
		            "claude-batch-test"
		        )

		        isFinalText  = finalResult == "toolA ran, toolB was rejected."
		        toolARan     = toolACalls == 1
		        toolBSkipped = toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothPending" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBSkipped" ) ) ).isTrue();
	}

	@DisplayName( "ClaudeService: chatStream()'s resumeToolBatchStream() finishes a batch from a resume ledger — no LLM replay" )
	@Test
	public void testClaudeStreamResumeToolBatchFinishesLedgerDirectly() {
		// Deterministic / credential-free by construction: unlike a normal suspend, this test
		// never touches Claude's SSE transport at all — ClaudeService.chatStream() has no
		// wrapLLMCall seam around its initial HTTP call (unlike chat(), and unlike Bedrock's and
		// Cohere's chatStream(), whose transports ARE wrapLLMCall-wrapped — see the streaming
		// suspend/resume tests for those two below), so an *initial* streamed tool-call batch
		// can't be simulated for Claude without a real network call. resumeToolBatchStream()
		// itself has no such limitation: it starts from an already-resolved resume ledger (exactly
		// what AiAgent.resumeStream() hands it) and only touches the network for its final
		// follow-up turn, which — like chat() — IS wrapLLMCall-wrapped. This test drives that
		// method directly by pre-seeding _resumeContext, the same shape AiAgent.resumeStream()
		// builds, without needing AiAgent or a real suspend/resume round-trip.
		// @formatter:off
		runtime.executeSource(
		    """
		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )

		        provider = aiService( "claude", { apiKey: "dummy-key" } )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "please run toolA and toolB" ),
		            { model: "claude-sonnet-4-5", tools: [ toolA, toolB ] },
		            {
		                provider: "claude",
		                _resumeContext: {
		                    assistantMessage: {
		                        "role": "assistant",
		                        "content": [
		                            { "type": "tool_use", "id": "call_a", "name": "toolA", "input": {} },
		                            { "type": "tool_use", "id": "call_b", "name": "toolB", "input": {} }
		                        ]
		                    },
		                    resumeLedger: [
		                        { toolName: "toolA", status: "execute" },
		                        { toolName: "toolB", status: "blocked", reason: "not needed" }
		                    ]
		                }
		            }
		        )
		        chatRequest.addMiddleware( {
		            // Only the follow-up turn after tool results are sent back touches the network
		            "wrapLLMCall": ( ctx, handler ) => {
		                return {
		                    "content": [ { "type": "text", "text": "toolA ran, toolB was rejected." } ],
		                    "stop_reason": "end_turn",
		                    "usage": { "input_tokens": 5, "output_tokens": 5 }
		                }
		            }
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        sawMiddlewareStop = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        finalText = chunks
		            .filter( c => isStruct( c ) && c.keyExists( "choices" ) )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )
		        isFinalText  = finalText == "toolA ran, toolB was rejected."
		        toolARan     = toolACalls == 1
		        toolBSkipped = toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawMiddlewareStop" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBSkipped" ) ) ).isTrue();
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
		                region            : "us-east-1",
		                bedrockApi        : "invoke"
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
		                region            : "us-east-1",
		                bedrockApi        : "invoke"
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

	@DisplayName( "BedrockService: two tool calls needing approval suspend as ONE batch, resume with an array resolves them individually" )
	@Test
	public void testBedrockBatchSuspendAndArrayResume() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        provider = aiService(
		            "bedrock",
		            {
		                awsAccessKeyId    : "AKIAIOSFODNN7EXAMPLE",
		                awsSecretAccessKey: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
		                region            : "us-east-1",
		                bedrockApi        : "invoke"
		            }
		        )
		        model = new AiModel( service: provider, params: { model: "anthropic.claude-3-sonnet-20240229-v1:0" } )

		        callCount = 0
		        cannedLLM = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                callCount++
		                if ( callCount == 1 ) {
		                    return {
		                        "content": [
		                            { "type": "tool_use", "id": "call_a", "name": "toolA", "input": {} },
		                            { "type": "tool_use", "id": "call_b", "name": "toolB", "input": {} }
		                        ],
		                        "stop_reason": "tool_use",
		                        "usage": { "input_tokens": 5, "output_tokens": 5 }
		                    }
		                }
		                return {
		                    "content": [ { "type": "text", "text": "toolA ran, toolB was rejected." } ],
		                    "stop_reason": "end_turn",
		                    "usage": { "input_tokens": 5, "output_tokens": 5 }
		                }
		            }
		        }

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" )
		        checkpointer = aiMemory( "cache" )

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ hitlMw, cannedLLM ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        suspendedResult = agent.run( "please run toolA and toolB", {}, { threadId: "bedrock-batch-test" } )
		        isSuspended     = isObject( suspendedResult ) && suspendedResult.isSuspended()
		        pendingActions  = suspendedResult.getData().pendingActions ?: []
		        bothPending     = pendingActions.len() == 2
		        neitherRanYet   = toolACalls == 0 && toolBCalls == 0

		        finalResult = agent.resume(
		            [
		                { decision: "approve" },
		                { decision: "reject", reason: "not needed" }
		            ],
		            "bedrock-batch-test"
		        )

		        isFinalText  = finalResult == "toolA ran, toolB was rejected."
		        toolARan     = toolACalls == 1
		        toolBSkipped = toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothPending" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBSkipped" ) ) ).isTrue();
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

	@DisplayName( "CohereService: two tool calls needing approval suspend as ONE batch, resume with an array resolves them individually" )
	@Test
	public void testCohereBatchSuspendAndArrayResume() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        provider = aiService( "cohere", { apiKey: "dummy-key" } )
		        model    = new AiModel( service: provider )

		        callCount = 0
		        cannedLLM = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                callCount++
		                if ( callCount == 1 ) {
		                    return {
		                        "text"      : "",
		                        "tool_calls": [
		                            { "name": "toolA", "parameters": {} },
		                            { "name": "toolB", "parameters": {} }
		                        ]
		                    }
		                }
		                // The follow-up request after tool results are sent back
		                return { "text": "toolA ran, toolB was rejected." }
		            }
		        }

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" )
		        checkpointer = aiMemory( "cache" )

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ hitlMw, cannedLLM ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        suspendedResult = agent.run( "please run toolA and toolB", {}, { threadId: "cohere-batch-test" } )
		        isSuspended     = isObject( suspendedResult ) && suspendedResult.isSuspended()
		        pendingActions  = suspendedResult.getData().pendingActions ?: []
		        bothPending     = pendingActions.len() == 2
		        neitherRanYet   = toolACalls == 0 && toolBCalls == 0

		        finalResult = agent.resume(
		            [
		                { decision: "approve" },
		                { decision: "reject", reason: "not needed" }
		            ],
		            "cohere-batch-test"
		        )

		        isFinalText  = finalResult == "toolA ran, toolB was rejected."
		        toolARan     = toolACalls == 1
		        toolBSkipped = toolBCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothPending" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBSkipped" ) ) ).isTrue();
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

	// ---- Streaming suspend/resume for Bedrock and Cohere (issue #248) ----------------------------
	//
	// Both providers now stream through a wrapLLMCall seam, so a canned transport body replaces the
	// network entirely: Bedrock's chatStream() takes the AWS event-stream body its wrapLLMCall
	// returns (the string form, via parseBedrockEventStream()'s documented non-binary fallback),
	// and Cohere's hands the middleware an `emitSSEChunk` callback to replay v1 SSE events through.
	// That makes the full agent-level round trip — stream → batch suspend → checkpoint →
	// resumeStream() → resumeToolBatchStream() — deterministic and credential-free.

	/**
	 * Dummy AWS credentials: the streaming leg never reaches the network, but the service still
	 * requires them.
	 */
	private static final String	BEDROCK_PROVIDER		= """
	                                                      provider = aiService(
	                                                          "bedrock",
	                                                          {
	                                                              awsAccessKeyId    : "AKIAIOSFODNN7EXAMPLE",
	                                                              awsSecretAccessKey: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
	                                                              region            : "us-east-1",
	                                                              bedrockApi        : "invoke"
	                                                          }
	                                                      )
	                                                      """;

	/**
	 * Canned Bedrock/Claude event-stream bodies. `evt()` wraps one model event in the
	 * `{"bytes":"<base64>"}` envelope Bedrock uses for chunk frames; concatenating them is enough
	 * for chatStream()'s non-binary fallback, so no binary framing is needed here.
	 */
	private static final String	BEDROCK_STREAM_EVENTS	= """
	                                                      evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
	                                                      twoToolStream = evt( { "type": "content_block_start", "index": 0, "content_block": { "type": "tool_use", "id": "tu_a", "name": "toolA" } } )
	                                                          & evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "input_json_delta", "partial_json": '{}' } } )
	                                                          & evt( { "type": "content_block_stop", "index": 0 } )
	                                                          & evt( { "type": "content_block_start", "index": 1, "content_block": { "type": "tool_use", "id": "tu_b", "name": "toolB" } } )
	                                                          & evt( { "type": "content_block_delta", "index": 1, "delta": { "type": "input_json_delta", "partial_json": '{}' } } )
	                                                          & evt( { "type": "content_block_stop", "index": 1 } )
	                                                          & evt( { "type": "message_delta", "delta": { "stop_reason": "tool_use" }, "usage": { "output_tokens": 8 } } )
	                                                      oneToolStream = evt( { "type": "content_block_start", "index": 0, "content_block": { "type": "tool_use", "id": "tu_1", "name": "deleteRecord" } } )
	                                                          & evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "input_json_delta", "partial_json": '{"id":"5"}' } } )
	                                                          & evt( { "type": "content_block_stop", "index": 0 } )
	                                                          & evt( { "type": "message_delta", "delta": { "stop_reason": "tool_use" }, "usage": { "output_tokens": 6 } } )
	                                                      textStream = evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "text_delta", "text": "all handled" } } )
	                                                          & evt( { "type": "message_delta", "delta": { "stop_reason": "end_turn" }, "usage": { "output_tokens": 2 } } )
	                                                      """;

	@DisplayName( "BedrockService streaming: two approval-requiring tools suspend ONCE as a batch and checkpoint" )
	@Test
	public void testBedrockStreamBatchSuspendsOnceAndCheckpoints() {
		// @formatter:off
		runtime.executeSource(
		    BEDROCK_STREAM_EVENTS + """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )
		    """ + BEDROCK_PROVIDER + """
		        model = new AiModel( service: provider, params: { model: "anthropic.claude-3-sonnet-20240229-v1:0" } )

		        wrapCalls = 0
		        cannedLLM = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                return wrapCalls == 1 ? twoToolStream : textStream
		            }
		        }

		        checkpointer = aiMemory( "cache" )
		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ), cannedLLM ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        chunks = []
		        agent.stream( ( chunk ) => { chunks.append( chunk ) }, "please run toolA and toolB", {}, { threadId: "bedrock-stream-batch" } )

		        stops         = chunks.filter( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        suspendedOnce = stops.len() == 1 && stops.first().result.isSuspended()
		        suspendData   = suspendedOnce ? stops.first().result.getData() : {}
		        bothPending   = ( suspendData.pendingActions ?: [] ).len() == 2
		        hasLedger     = ( suspendData.resumeLedger ?: [] ).len() == 2
		        neitherRanYet = toolACalls == 0 && toolBCalls == 0
		        calledLLMOnce = wrapCalls == 1

		        checkpointSaved = !checkpointer.loadState( "bedrock-stream-batch" ).isEmpty()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspendedOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothPending" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasLedger" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "calledLLMOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "checkpointSaved" ) ) ).isTrue();
	}

	@DisplayName( "BedrockService streaming: resumeStream() approve-all finishes the batch — tools run once, the suspended turn is not replayed" )
	@Test
	public void testBedrockStreamResumeApproveAllCompletesWithoutReplay() {
		// Full agent round trip: agent.stream() suspends the batch and checkpoints, agent.resumeStream()
		// finishes it from the ledger. This used to blow up with a StackOverflowError because
		// ClosureTool.doInvoke() mutated the live tool_use.input in message history, leaving a
		// `_chatRequest` back-reference that made the second streamed request body cyclic. doInvoke()
		// now works on a shallow copy, so the follow-up body serializes cleanly — asserted below.
		// @formatter:off
		runtime.executeSource(
		    BEDROCK_STREAM_EVENTS + """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )
		    """ + BEDROCK_PROVIDER + """
		        model = new AiModel( service: provider, params: { model: "anthropic.claude-3-sonnet-20240229-v1:0" } )

		        wrapCalls    = 0
		        followUpBody = []
		        cannedLLM = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                if( wrapCalls == 1 ){
		                    return twoToolStream
		                }
		                followUpBody = ctx.dataPacket.messages ?: []
		                return textStream
		            }
		        }

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ), cannedLLM ],
		            checkpointer: aiMemory( "cache" ),
		            checkpointTTL: 5
		        )

		        chunks = []
		        agent.stream( ( chunk ) => { chunks.append( chunk ) }, "please run toolA and toolB", {}, { threadId: "bedrock-stream-approve" } )
		        wasSuspended  = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" && c.result.isSuspended() )
		        neitherRanYet = toolACalls == 0 && toolBCalls == 0

		        resumeChunks = []
		        agent.resumeStream(
		            ( chunk ) => { resumeChunks.append( chunk ) },
		            [ { decision: "approve" }, { decision: "approve" } ],
		            "bedrock-stream-approve"
		        )

		        finalText = resumeChunks
		            .filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )
		        isFinalText      = finalText == "all handled"
		        bothRanOnce      = toolACalls == 1 && toolBCalls == 1
		        // Exactly two LLM calls total: the suspended turn, then the follow-up. The resume
		        // finished the SAME batch from the ledger rather than replaying the first turn.
		        twoLLMCallsTotal = wrapCalls == 2
		        noResumeStop     = !resumeChunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        // Regression guard for the ClosureTool.doInvoke() leak: the follow-up request body must
		        // carry no `_chatRequest` inside any assistant tool_use input. sawToolUseInput keeps the
		        // guard honest — without it an empty body would satisfy noChatRequestLeak vacuously.
		        sawToolUseInput = followUpBody.some( m =>
		            isArray( m.content ?: "" )
		            && m.content.some( b => isStruct( b ) && ( b.type ?: "" ) == "tool_use" && isStruct( b.input ?: "" ) )
		        )
		        noChatRequestLeak = !followUpBody.some( m =>
		            isArray( m.content ?: "" )
		            && m.content.some( b =>
		                isStruct( b )
		                && isStruct( b.input ?: "" )
		                && b.input.keyExists( "_chatRequest" )
		            )
		        )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "wasSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "twoLLMCallsTotal" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noResumeStop" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawToolUseInput" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noChatRequestLeak" ) ) ).isTrue();
	}

	@DisplayName( "BedrockService streaming: a 'reject' decision in the resume array blocks that tool, is reported back as a tool_result, and the turn still completes" )
	@Test
	public void testBedrockStreamResumeRejectHonoured() {
		// @formatter:off
		runtime.executeSource(
		    BEDROCK_STREAM_EVENTS + """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )
		    """ + BEDROCK_PROVIDER + """
		        model = new AiModel( service: provider, params: { model: "anthropic.claude-3-sonnet-20240229-v1:0" } )

		        wrapCalls    = 0
		        followUpBody = []
		        cannedLLM = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                if( wrapCalls == 1 ){
		                    return twoToolStream
		                }
		                followUpBody = ctx.dataPacket.messages ?: []
		                return textStream
		            }
		        }

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ), cannedLLM ],
		            checkpointer: aiMemory( "cache" ),
		            checkpointTTL: 5
		        )

		        agent.stream( ( chunk ) => {}, "please run toolA and toolB", {}, { threadId: "bedrock-stream-reject" } )

		        resumeChunks = []
		        agent.resumeStream(
		            ( chunk ) => { resumeChunks.append( chunk ) },
		            [ { decision: "approve" }, { decision: "reject", reason: "not needed" } ],
		            "bedrock-stream-reject"
		        )

		        finalText = resumeChunks
		            .filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )
		        isFinalText      = finalText == "all handled"
		        toolARan         = toolACalls == 1
		        toolBSkipped     = toolBCalls == 0
		        // Only the suspended turn and the follow-up touched the LLM — no replay
		        twoLLMCallsTotal = wrapCalls == 2
		        noStop           = !resumeChunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        sawBlockedResult = followUpBody.some( m =>
		            isArray( m.content ?: "" )
		            && m.content.some( b => ( b.type ?: "" ) == "tool_result" && toString( b.content ?: "" ).findNoCase( "blocked" ) > 0 )
		        )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBSkipped" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "twoLLMCallsTotal" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noStop" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawBlockedResult" ) ) ).isTrue();
	}

	@DisplayName( "BedrockService streaming: a cancelled tool call short-circuits the stream and no tool runs" )
	@Test
	public void testBedrockStreamCancelShortCircuits() {
		// @formatter:off
		runtime.executeSource(
		    BEDROCK_STREAM_EVENTS + """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        toolCalls = 0
		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => { toolCalls++; return "deleted" } )
		    """ + BEDROCK_PROVIDER + """
		        chatRequest = aiChatRequest(
		            aiMessage().user( "Delete record 5" ),
		            { model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
		            { provider: "bedrock" }
		        )

		        wrapCalls = 0
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                return oneToolStream
		            },
		            "beforeToolCall": ( ctx ) => AiMiddlewareResult.cancel( "Max tool calls exceeded" )
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        sawCancelSentinel = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" && c.result.isCancelled() )
		        toolNotCalled     = toolCalls == 0
		        noFollowUpCall    = wrapCalls == 1
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawCancelSentinel" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNotCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noFollowUpCall" ) ) ).isTrue();
	}

	@DisplayName( "CohereService streaming: two approval-requiring tools suspend ONCE as a batch and checkpoint" )
	@Test
	public void testCohereStreamBatchSuspendsOnceAndCheckpoints() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        provider = aiService( "cohere", { apiKey: "dummy-key" } )
		        model    = new AiModel( service: provider )

		        streamCalls   = 0
		        followUpCalls = 0
		        cannedLLM = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    streamCalls++
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type": "tool-calls-generation",
		                        "tool_calls": [ { "name": "toolA", "parameters": {} }, { "name": "toolB", "parameters": {} } ]
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"   : "stream-end",
		                        "finish_reason": "COMPLETE",
		                        "response"     : { "meta": { "billed_units": { "input_tokens": 4, "output_tokens": 4 } } }
		                    } ) } )
		                    return {}
		                }
		                followUpCalls++
		                return { "text": "all handled" }
		            }
		        }

		        checkpointer = aiMemory( "cache" )
		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ), cannedLLM ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        chunks = []
		        agent.stream( ( chunk ) => { chunks.append( chunk ) }, "please run toolA and toolB", {}, { threadId: "cohere-stream-batch" } )

		        stops         = chunks.filter( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        suspendedOnce = stops.len() == 1 && stops.first().result.isSuspended()
		        suspendData   = suspendedOnce ? stops.first().result.getData() : {}
		        bothPending   = ( suspendData.pendingActions ?: [] ).len() == 2
		        hasLedger     = ( suspendData.resumeLedger ?: [] ).len() == 2
		        neitherRanYet = toolACalls == 0 && toolBCalls == 0
		        calledLLMOnce = streamCalls == 1 && followUpCalls == 0

		        checkpointSaved = !checkpointer.loadState( "cohere-stream-batch" ).isEmpty()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspendedOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothPending" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasLedger" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "calledLLMOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "checkpointSaved" ) ) ).isTrue();
	}

	@DisplayName( "CohereService streaming: resumeStream() approve-all finishes the batch — tools run once, the suspended turn is not replayed" )
	@Test
	public void testCohereStreamResumeApproveAllCompletesWithoutReplay() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        provider = aiService( "cohere", { apiKey: "dummy-key" } )
		        model    = new AiModel( service: provider )

		        streamCalls   = 0
		        followUpCalls = 0
		        cannedLLM = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    streamCalls++
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type": "tool-calls-generation",
		                        "tool_calls": [ { "name": "toolA", "parameters": {} }, { "name": "toolB", "parameters": {} } ]
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"   : "stream-end",
		                        "finish_reason": "COMPLETE",
		                        "response"     : { "meta": { "billed_units": { "input_tokens": 4, "output_tokens": 4 } } }
		                    } ) } )
		                    return {}
		                }
		                followUpCalls++
		                return { "text": "all handled" }
		            }
		        }

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ), cannedLLM ],
		            checkpointer: aiMemory( "cache" ),
		            checkpointTTL: 5
		        )

		        chunks = []
		        agent.stream( ( chunk ) => { chunks.append( chunk ) }, "please run toolA and toolB", {}, { threadId: "cohere-stream-approve" } )
		        wasSuspended = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" && c.result.isSuspended() )

		        resumeChunks = []
		        agent.resumeStream(
		            ( chunk ) => { resumeChunks.append( chunk ) },
		            [ { decision: "approve" }, { decision: "approve" } ],
		            "cohere-stream-approve"
		        )

		        finalText = resumeChunks
		            .filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )
		        isFinalText      = finalText == "all handled"
		        bothRanOnce      = toolACalls == 1 && toolBCalls == 1
		        // One streamed turn + one follow-up = two LLM calls total; the suspended turn was
		        // finished from the ledger, never streamed again.
		        twoLLMCallsTotal = streamCalls == 1 && followUpCalls == 1
		        noResumeStop     = !resumeChunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "wasSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "twoLLMCallsTotal" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noResumeStop" ) ) ).isTrue();
	}

	@DisplayName( "CohereService streaming: a 'reject' decision in the resume array blocks that tool and still completes" )
	@Test
	public void testCohereStreamResumeRejectHonoured() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        provider = aiService( "cohere", { apiKey: "dummy-key" } )
		        model    = new AiModel( service: provider )

		        streamCalls     = 0
		        followUpCalls   = 0
		        capturedResults = []
		        cannedLLM = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    streamCalls++
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type": "tool-calls-generation",
		                        "tool_calls": [ { "name": "toolA", "parameters": {} }, { "name": "toolB", "parameters": {} } ]
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"   : "stream-end",
		                        "finish_reason": "COMPLETE",
		                        "response"     : { "meta": { "billed_units": { "input_tokens": 4, "output_tokens": 4 } } }
		                    } ) } )
		                    return {}
		                }
		                followUpCalls++
		                capturedResults = ctx.dataPacket.tool_results ?: []
		                return { "text": "all handled" }
		            }
		        }

		        agent = aiAgent(
		            model       : model,
		            tools       : [ toolA, toolB ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ), cannedLLM ],
		            checkpointer: aiMemory( "cache" ),
		            checkpointTTL: 5
		        )

		        agent.stream( ( chunk ) => {}, "please run toolA and toolB", {}, { threadId: "cohere-stream-reject" } )

		        resumeChunks = []
		        agent.resumeStream(
		            ( chunk ) => { resumeChunks.append( chunk ) },
		            [ { decision: "approve" }, { decision: "reject", reason: "not needed" } ],
		            "cohere-stream-reject"
		        )

		        finalText = resumeChunks
		            .filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )
		        isFinalText      = finalText == "all handled"
		        toolARan         = toolACalls == 1
		        toolBSkipped     = toolBCalls == 0
		        twoLLMCallsTotal = streamCalls == 1 && followUpCalls == 1
		        sawBlockedResult = capturedResults.len() == 2
		            && ( capturedResults[ 2 ].outputs.first().result ?: "" ).findNoCase( "blocked" ) > 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBSkipped" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "twoLLMCallsTotal" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawBlockedResult" ) ) ).isTrue();
	}

	@DisplayName( "CohereService streaming: a cancelled tool call short-circuits the stream and no tool runs" )
	@Test
	public void testCohereStreamCancelShortCircuits() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        toolCalls = 0
		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => { toolCalls++; return "deleted" } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "Delete record 5" ),
		            { model: "command-a-03-2025", tools: [ tool ] },
		            { provider: "cohere" }
		        )

		        streamCalls   = 0
		        followUpCalls = 0
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    streamCalls++
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type": "tool-calls-generation",
		                        "tool_calls": [ { "name": "deleteRecord", "parameters": { "id": "5" } } ]
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( { "event_type": "stream-end", "finish_reason": "COMPLETE", "response": {} } ) } )
		                    return {}
		                }
		                followUpCalls++
		                return { "text": "should never be reached" }
		            },
		            "beforeToolCall": ( ctx ) => AiMiddlewareResult.cancel( "Max tool calls exceeded" )
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        sawCancelSentinel = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" && c.result.isCancelled() )
		        toolNotCalled     = toolCalls == 0
		        noFollowUpCall    = followUpCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawCancelSentinel" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNotCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noFollowUpCall" ) ) ).isTrue();
	}

	@DisplayName( "CohereService streaming: HITL 'edit' resume decision patches toolCall.parameters before the streamed tool runs" )
	@Test
	public void testCohereStreamEditResumePatchesToolParameters() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

		        capturedId = ""
		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => { capturedId = id; return "deleted:" & id } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
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
		        chatRequest.addMiddleware( new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "web" ) )

		        followUpCalls = 0
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type": "tool-calls-generation",
		                        "tool_calls": [ { "name": "deleteRecord", "parameters": { "id": "5" } } ]
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( { "event_type": "stream-end", "finish_reason": "COMPLETE", "response": {} } ) } )
		                    return {}
		                }
		                followUpCalls++
		                return { "text": "done" }
		            }
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        gotEditedArgs = capturedId == "999"
		        completed     = followUpCalls == 1
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "gotEditedArgs" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "completed" ) ) ).isTrue();
	}

	@DisplayName( "BedrockService streaming: HITL 'edit' resume decision patches toolCall.input before the streamed tool runs" )
	@Test
	public void testBedrockStreamEditResumePatchesToolInput() {
		// @formatter:off
		runtime.executeSource(
		    BEDROCK_STREAM_EVENTS + """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

		        capturedId = ""
		        tool = aiTool( "deleteRecord", "Delete a record", ( required string id ) => { capturedId = id; return "deleted:" & id } )
		    """ + BEDROCK_PROVIDER + """
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
		        chatRequest.addMiddleware( new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "web" ) )

		        wrapCalls = 0
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                return wrapCalls == 1 ? oneToolStream : textStream
		            }
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        gotEditedArgs = capturedId == "999"
		        completed     = wrapCalls == 2
		        noStop        = !chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "gotEditedArgs" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "completed" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noStop" ) ) ).isTrue();
	}

	// ---- Rewritten toolArgs must survive a batch suspension (per provider) ----------------
	// A resume NEVER re-fires beforeToolCall, so the resume ledger's `toolArgs` is the only
	// surviving record of a pass-1 `ctx.toolArgs` rewrite. Before the ledger carried it, every
	// resumed batch re-derived its arguments from the raw assistant tool call and silently ran
	// the model's ORIGINAL arguments instead of the middleware-approved ones.
	// One per provider, each in that provider's native tool-call shape.

	@DisplayName( "ClaudeService: a suspended batch records the middleware-rewritten toolArgs in its resume ledger, and the resume runs them" )
	@Test
	public void testClaudeSuspendedLedgerCarriesRewrittenToolArgs() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        aCity = ""
		        bRuns = 0
		        toolA = aiTool( "toolA", "Tool A", ( string city = "DEFAULT" ) => { aCity = city; return "A:" & city } )
		        toolB = aiTool( "toolB", "Tool B", ( string city = "DEFAULT" ) => { bRuns++; return "B:" & city } )

		        provider = aiService( "claude", { apiKey: "dummy-key" } )

		        wrapCalls = 0
		        llmMw = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                if ( wrapCalls == 1 ) {
		                    return {
		                        "content": [
		                            { "type": "tool_use", "id": "call_a", "name": "toolA", "input": { "city": "ORIGINAL" } },
		                            { "type": "tool_use", "id": "call_b", "name": "toolB", "input": { "city": "ORIGINAL" } }
		                        ],
		                        "stop_reason": "tool_use",
		                        "usage": { "input_tokens": 5, "output_tokens": 5 }
		                    }
		                }
		                return {
		                    "content": [ { "type": "text", "text": "both done" } ],
		                    "stop_reason": "end_turn",
		                    "usage": { "input_tokens": 5, "output_tokens": 5 }
		                }
		            }
		        }

		        // toolA's arguments are rewritten in pass 1; toolB needs a human, so the WHOLE
		        // batch suspends before either tool runs.
		        rewriteMw = {
		            "beforeToolCall": ( ctx ) => {
		                if ( ctx.toolName == "toolA" ) {
		                    ctx.toolArgs = { "city": "SAFE" }
		                    return AiMiddlewareResult::continue()
		                }
		                return AiMiddlewareResult::defer( { toolName: ctx.toolName, toolArgs: ctx.toolArgs } )
		            }
		        }

		        chatRequest = aiChatRequest(
		            aiMessage().user( "run both" ),
		            { model: "claude-sonnet-4-5", tools: [ toolA, toolB ] },
		            { provider: "claude" }
		        )
		        chatRequest.addMiddleware( rewriteMw )
		        chatRequest.addMiddleware( llmMw )

		        suspendedResult = provider.chat( chatRequest )
		        suspended     = isObject( suspendedResult ) && suspendedResult.isSuspended()
		        suspendData   = suspended ? suspendedResult.getData() : {}
		        ledger        = suspendData.resumeLedger ?: []
		        ledgerLen     = ledger.len()
		        aStatus       = ledgerLen ? ( ledger[ 1 ].status ?: "" ) : ""
		        aLedgerCity   = ledgerLen ? ( ledger[ 1 ].toolArgs.city ?: "" ) : ""
		        bStatus       = ledgerLen > 1 ? ( ledger[ 2 ].status ?: "" ) : ""
		        neitherRanYet = aCity == "" && bRuns == 0
		        calledLLMOnce = wrapCalls == 1

		        // ---- resume: approve the pending call ------------------------------------------
		        finalLedger = ledger
		        finalLedger[ 2 ].status = "execute"

		        resumeRequest = aiChatRequest(
		            aiMessage().user( "run both" ),
		            { model: "claude-sonnet-4-5", tools: [ toolA, toolB ] },
		            {
		                provider: "claude",
		                _resumeContext: {
		                    assistantMessage: suspendData.assistantMessage,
		                    resumeLedger    : finalLedger
		                }
		            }
		        )
		        resumeRequest.addMiddleware( llmMw )
		        answer = provider.chat( resumeRequest )
		        resumeAddedOneLLMCall = wrapCalls == 2
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspended" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "ledgerLen" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "aStatus" ) ).toString() ).isEqualTo( "execute" );
		assertThat( variables.get( Key.of( "aLedgerCity" ) ).toString() ).isEqualTo( "SAFE" );
		assertThat( variables.get( Key.of( "bStatus" ) ).toString() ).isEqualTo( "pending" );
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "calledLLMOnce" ) ) ).isTrue();
		// Without the ledger's toolArgs the resume re-derived them from tool_use.input and toolA
		// ran with "ORIGINAL" — the rewrite the non-suspended path had honoured was lost.
		assertThat( variables.get( Key.of( "aCity" ) ).toString() ).isEqualTo( "SAFE" );
		assertThat( variables.getAsInteger( Key.of( "bRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "resumeAddedOneLLMCall" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "both done" );
	}

	@DisplayName( "CohereService: a suspended batch records the middleware-rewritten toolArgs in its resume ledger, and the resume runs them" )
	@Test
	public void testCohereSuspendedLedgerCarriesRewrittenToolArgs() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        aCity = ""
		        bRuns = 0
		        toolA = aiTool( "toolA", "Tool A", ( string city = "DEFAULT" ) => { aCity = city; return "A:" & city } )
		        toolB = aiTool( "toolB", "Tool B", ( string city = "DEFAULT" ) => { bRuns++; return "B:" & city } )

		        provider = aiService( "cohere", { apiKey: "dummy-key" } )

		        wrapCalls = 0
		        llmMw = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                if ( wrapCalls == 1 ) {
		                    return {
		                        "text"      : "",
		                        "tool_calls": [
		                            { "name": "toolA", "parameters": { "city": "ORIGINAL" } },
		                            { "name": "toolB", "parameters": { "city": "ORIGINAL" } }
		                        ]
		                    }
		                }
		                // The follow-up turn once both tool results are sent back
		                return { "text": "both done" }
		            }
		        }

		        rewriteMw = {
		            "beforeToolCall": ( ctx ) => {
		                if ( ctx.toolName == "toolA" ) {
		                    ctx.toolArgs = { "city": "SAFE" }
		                    return AiMiddlewareResult::continue()
		                }
		                return AiMiddlewareResult::defer( { toolName: ctx.toolName, toolArgs: ctx.toolArgs } )
		            }
		        }

		        chatRequest = aiChatRequest(
		            aiMessage().user( "run both" ),
		            { model: "command-a-03-2025", tools: [ toolA, toolB ] },
		            { provider: "cohere" }
		        )
		        chatRequest.addMiddleware( rewriteMw )
		        chatRequest.addMiddleware( llmMw )

		        suspendedResult = provider.chat( chatRequest )
		        suspended     = isObject( suspendedResult ) && suspendedResult.isSuspended()
		        suspendData   = suspended ? suspendedResult.getData() : {}
		        ledger        = suspendData.resumeLedger ?: []
		        ledgerLen     = ledger.len()
		        aStatus       = ledgerLen ? ( ledger[ 1 ].status ?: "" ) : ""
		        aLedgerCity   = ledgerLen ? ( ledger[ 1 ].toolArgs.city ?: "" ) : ""
		        bStatus       = ledgerLen > 1 ? ( ledger[ 2 ].status ?: "" ) : ""
		        neitherRanYet = aCity == "" && bRuns == 0
		        calledLLMOnce = wrapCalls == 1

		        // ---- resume: approve the pending call ------------------------------------------
		        finalLedger = ledger
		        finalLedger[ 2 ].status = "execute"

		        // Cohere has no shared `messages` array — everything the follow-up request needs
		        // (tool calls, tools, chat_history, preamble, params) is what chat() captured on
		        // suspendData.assistantMessage, so it is handed straight back.
		        resumeRequest = aiChatRequest(
		            aiMessage().user( "run both" ),
		            { model: "command-a-03-2025", tools: [ toolA, toolB ] },
		            {
		                provider: "cohere",
		                _resumeContext: {
		                    assistantMessage: suspendData.assistantMessage,
		                    resumeLedger    : finalLedger
		                }
		            }
		        )
		        resumeRequest.addMiddleware( llmMw )
		        answer = provider.chat( resumeRequest )
		        resumeAddedOneLLMCall = wrapCalls == 2
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspended" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "ledgerLen" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "aStatus" ) ).toString() ).isEqualTo( "execute" );
		assertThat( variables.get( Key.of( "aLedgerCity" ) ).toString() ).isEqualTo( "SAFE" );
		assertThat( variables.get( Key.of( "bStatus" ) ).toString() ).isEqualTo( "pending" );
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "calledLLMOnce" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "aCity" ) ).toString() ).isEqualTo( "SAFE" );
		assertThat( variables.getAsInteger( Key.of( "bRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "resumeAddedOneLLMCall" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "both done" );
	}

	@DisplayName( "OpenAIService: a suspended batch records the middleware-rewritten toolArgs in its resume ledger, and the resume runs them" )
	@Test
	public void testOpenAISuspendedLedgerCarriesRewrittenToolArgs() {
		// The middleware REPLACES ctx.toolArgs outright (the case OpenAIService used to drop: its
		// pass 1 stored the struct parsed from `function.arguments` before beforeToolCall fired and
		// pass 2 re-parsed the string, so only in-place mutation ever reached the tool). The native
		// `function.arguments` string is deliberately left saying "ORIGINAL": that is what the
		// resume falls back to when the ledger carries no toolArgs, so "SAFE" can only come from
		// the ledger.
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        aCity = ""
		        bRuns = 0
		        toolA = aiTool( "toolA", "Tool A", ( string city = "DEFAULT" ) => { aCity = city; return "A:" & city } )
		        toolB = aiTool( "toolB", "Tool B", ( string city = "DEFAULT" ) => { bRuns++; return "B:" & city } )

		        provider = aiService( "openai", { apiKey: "dummy-key" } )
		        usage    = { "prompt_tokens": 5, "completion_tokens": 3, "total_tokens": 8 }

		        wrapCalls = 0
		        llmMw = {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                if ( wrapCalls == 1 ) {
		                    return {
		                        "choices": [ {
		                            "index": 0,
		                            "message": {
		                                "role": "assistant",
		                                "content": "",
		                                "tool_calls": [
		                                    { "id": "call_a", "type": "function", "function": { "name": "toolA", "arguments": '{"city":"ORIGINAL"}' } },
		                                    { "id": "call_b", "type": "function", "function": { "name": "toolB", "arguments": '{"city":"ORIGINAL"}' } }
		                                ]
		                            },
		                            "finish_reason": "tool_calls"
		                        } ],
		                        "usage": usage
		                    }
		                }
		                return {
		                    "choices": [ { "index": 0, "message": { "role": "assistant", "content": "both done" }, "finish_reason": "stop" } ],
		                    "usage": usage
		                }
		            }
		        }

		        rewriteMw = {
		            "beforeToolCall": ( ctx ) => {
		                if ( ctx.toolName == "toolA" ) {
		                    ctx.toolArgs = { city: "SAFE" }
		                    return AiMiddlewareResult::continue()
		                }
		                return AiMiddlewareResult::defer( { toolName: ctx.toolName, toolArgs: ctx.toolArgs } )
		            }
		        }

		        chatRequest = aiChatRequest(
		            aiMessage().user( "run both" ),
		            { model: "gpt-4o-mini", tools: [ toolA, toolB ] },
		            { provider: "openai" }
		        )
		        chatRequest.addMiddleware( rewriteMw )
		        chatRequest.addMiddleware( llmMw )

		        suspendedResult = provider.chat( chatRequest )
		        suspended     = isObject( suspendedResult ) && suspendedResult.isSuspended()
		        suspendData   = suspended ? suspendedResult.getData() : {}
		        ledger        = suspendData.resumeLedger ?: []
		        ledgerLen     = ledger.len()
		        aStatus       = ledgerLen ? ( ledger[ 1 ].status ?: "" ) : ""
		        aLedgerCity   = ledgerLen ? ( ledger[ 1 ].toolArgs.city ?: "" ) : ""
		        bStatus       = ledgerLen > 1 ? ( ledger[ 2 ].status ?: "" ) : ""
		        neitherRanYet = aCity == "" && bRuns == 0
		        calledLLMOnce = wrapCalls == 1

		        // The native slot still says ORIGINAL — proving the resume below reads the ledger
		        nativeStillOriginal = findNoCase( "ORIGINAL", suspendData.assistantMessage.tool_calls[ 1 ].function.arguments ) > 0

		        // ---- resume: approve the pending call ------------------------------------------
		        finalLedger = ledger
		        finalLedger[ 2 ].status = "execute"

		        resumeRequest = aiChatRequest(
		            aiMessage().user( "run both" ),
		            { model: "gpt-4o-mini", tools: [ toolA, toolB ] },
		            {
		                provider: "openai",
		                _resumeContext: {
		                    assistantMessage: suspendData.assistantMessage,
		                    resumeLedger    : finalLedger
		                }
		            }
		        )
		        resumeRequest.addMiddleware( llmMw )
		        answer = provider.chat( resumeRequest )
		        resumeAddedOneLLMCall = wrapCalls == 2
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspended" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "ledgerLen" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "aStatus" ) ).toString() ).isEqualTo( "execute" );
		assertThat( variables.get( Key.of( "aLedgerCity" ) ).toString() ).isEqualTo( "SAFE" );
		assertThat( variables.get( Key.of( "bStatus" ) ).toString() ).isEqualTo( "pending" );
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "calledLLMOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "nativeStillOriginal" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "aCity" ) ).toString() ).isEqualTo( "SAFE" );
		assertThat( variables.getAsInteger( Key.of( "bRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "resumeAddedOneLLMCall" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "both done" );
	}

	@DisplayName( "Backward compatibility: a resume ledger with no toolArgs field still runs the tool with the assistant message's native arguments" )
	@Test
	public void testResumeLedgerWithoutToolArgsFallsBackToNativeArgs() {
		// A checkpoint written by an older build has ledger entries of the pre-fix shape
		// ({ toolName, status }) with no toolArgs at all. The `?:` fallback must re-derive the
		// arguments from the assistant message's tool call rather than invoking with {} — which
		// would have silently replaced every argument with its schema default.
		// @formatter:off
		runtime.executeSource(
		    """
		        seenCity = ""
		        probe = aiTool( "probe", "Probe a city", ( string city = "DEFAULT" ) => { seenCity = city; return "ok:" & city } )

		        provider = aiService( "claude", { apiKey: "dummy-key" } )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "probe" ),
		            { model: "claude-sonnet-4-5", tools: [ probe ] },
		            {
		                provider: "claude",
		                _resumeContext: {
		                    assistantMessage: {
		                        "role": "assistant",
		                        "content": [
		                            { "type": "tool_use", "id": "call_a", "name": "probe", "input": { "city": "NATIVE" } }
		                        ]
		                    },
		                    // Legacy ledger entry: no toolArgs, no editedArgs
		                    resumeLedger: [ { toolName: "probe", status: "execute" } ]
		                }
		            }
		        )

		        wrapCalls = 0
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                return {
		                    "content": [ { "type": "text", "text": "done" } ],
		                    "stop_reason": "end_turn",
		                    "usage": { "input_tokens": 5, "output_tokens": 5 }
		                }
		            }
		        } )

		        answer         = provider.chat( chatRequest )
		        onlyFollowUpLLM = wrapCalls == 1
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "seenCity" ) ).toString() ).isEqualTo( "NATIVE" );
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "done" );
		assertThat( variables.getAsBoolean( Key.of( "onlyFollowUpLLM" ) ) ).isTrue();
	}

	@DisplayName( "OpenAIService: a zero-argument tool call whose function.arguments is \"\" runs with {} instead of throwing" )
	@Test
	public void testOpenAIEmptyArgumentsStringRunsToolWithEmptyStruct() {
		// A no-argument tool comes back from OpenAI-compatible providers with `arguments` as ""
		// (some send "" rather than "{}"). jsonDeserialize( "" ) throws, so the whole turn died on
		// a perfectly valid tool call — and the same string reached three other parse sites.
		// @formatter:off
		runtime.executeSource(
		    """
		        pings = 0
		        ping  = aiTool( "ping", "Ping", () => { pings++; return "pong" } )

		        provider = aiService( "openai", { apiKey: "dummy-key" } )
		        usage    = { "prompt_tokens": 5, "completion_tokens": 3, "total_tokens": 8 }

		        wrapCalls = 0
		        chatRequest = aiChatRequest(
		            aiMessage().user( "ping please" ),
		            { model: "gpt-4o-mini", tools: [ ping ] },
		            { provider: "openai" }
		        )
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                if ( wrapCalls == 1 ) {
		                    return {
		                        "choices": [ {
		                            "index": 0,
		                            "message": {
		                                "role": "assistant",
		                                "content": "",
		                                "tool_calls": [
		                                    { "id": "call_p", "type": "function", "function": { "name": "ping", "arguments": "" } }
		                                ]
		                            },
		                            "finish_reason": "tool_calls"
		                        } ],
		                        "usage": usage
		                    }
		                }
		                return {
		                    "choices": [ { "index": 0, "message": { "role": "assistant", "content": "pinged" }, "finish_reason": "stop" } ],
		                    "usage": usage
		                }
		            }
		        } )

		        answer = provider.chat( chatRequest )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "pings" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "pinged" );
	}

	@DisplayName( "OpenAIService: a resume ledger with no toolArgs and an empty native arguments string still resumes" )
	@Test
	public void testOpenAIResumeWithEmptyNativeArgumentsString() {
		// The resume fallback re-parses `function.arguments` when the ledger entry carries no
		// toolArgs (a checkpoint from an older build). With "" on the wire that fallback threw, so
		// an approved tool call could never be finished at all.
		// @formatter:off
		runtime.executeSource(
		    """
		        pings = 0
		        ping  = aiTool( "ping", "Ping", () => { pings++; return "pong" } )

		        provider = aiService( "openai", { apiKey: "dummy-key" } )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "ping please" ),
		            { model: "gpt-4o-mini", tools: [ ping ] },
		            {
		                provider: "openai",
		                _resumeContext: {
		                    assistantMessage: {
		                        "role": "assistant",
		                        "content": "",
		                        "tool_calls": [
		                            { "id": "call_p", "type": "function", "function": { "name": "ping", "arguments": "" } }
		                        ]
		                    },
		                    resumeLedger: [ { toolName: "ping", status: "execute" } ]
		                }
		            }
		        )

		        wrapCalls = 0
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                wrapCalls++
		                return {
		                    "choices": [ { "index": 0, "message": { "role": "assistant", "content": "pinged" }, "finish_reason": "stop" } ],
		                    "usage": { "prompt_tokens": 5, "completion_tokens": 3, "total_tokens": 8 }
		                }
		            }
		        } )

		        answer          = provider.chat( chatRequest )
		        onlyFollowUpLLM = wrapCalls == 1
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "pings" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "pinged" );
		assertThat( variables.getAsBoolean( Key.of( "onlyFollowUpLLM" ) ) ).isTrue();
	}

	@DisplayName( "An 'edit' decision with no editedData executes the tool with the ledger's own toolArgs, not {}" )
	@Test
	public void testEditDecisionWithoutEditedDataKeepsLedgerArgs() {
		// resume( "edit" ) with nothing to apply used to write editedArgs = {} on the ledger, and
		// the provider's resume path then invoked the tool with NO arguments — every value silently
		// replaced by its schema default. With the slot left unset the ledger's toolArgs stand.
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        seenCity = ""
		        weather  = aiTool( "get_weather", "Get the weather", ( string city = "DEFAULT" ) => {
		            seenCity = city
		            return "sunny in " & city
		        } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "get_weather", arguments: { city: "LEDGER-CITY" } } ] },
		            "done."
		        ] )

		        checkpointer = aiMemory( "cache" )
		        agent = aiAgent(
		            model       : new AiModel( service: mockSvc ),
		            tools       : [ weather ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "get_weather" ], mode: "web" ) ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        agent.run( "weather?", {}, { threadId: "edit-no-data" } )

		        // "edit" with NO editedData at all
		        answer = agent.resume( "edit", "edit-no-data" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "seenCity" ) ).toString() ).isEqualTo( "LEDGER-CITY" );
	}
}
