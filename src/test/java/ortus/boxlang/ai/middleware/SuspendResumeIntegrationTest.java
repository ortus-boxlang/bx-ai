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
		            // Original attempt: toolA suspends before toolB runs
		            { toolCalls: [ { name: "toolA", arguments: {} }, { name: "toolB", arguments: {} } ] },
		            // Resume re-runs the agent from the checkpointed input: the LLM is asked again
		            // and returns the same tool calls; this time resumeContext approves toolA
		            { toolCalls: [ { name: "toolA", arguments: {} }, { name: "toolB", arguments: {} } ] },
		            // Follow-up turn after both tool results are sent back
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

	@DisplayName( "Resume with 'reject' stops the chain without ever invoking the tool" )
	@Test
	public void testResumeRejectStopsWithoutInvokingTool() {
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

		        agent.run( "please run toolA", {}, { threadId: "hitl-test-resume-reject" } )

		        finalResult   = agent.resume( "reject", "hitl-test-resume-reject" )
		        isRejected    = isObject( finalResult ) && finalResult.isRejected()
		        toolNotCalled = toolACalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isRejected" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNotCalled" ) ) ).isTrue();
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

	// ---- Streaming: any terminal result (not just suspend) must short-circuit ----

	@DisplayName( "Streaming: a rejected tool call short-circuits without firing afterAgentRun/storeInMemory" )
	@Test
	public void testStreamRejectShortCircuitsWithoutCompletion() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolACalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] }
		        ] )
		        model = new AiModel( service: mockSvc )

		        hitlMw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA" ], mode: "web" )

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
		            middleware: [ hitlMw, auditMw ]
		        )

		        // Simulate a resume-time rejection by pre-seeding the resumeContext directly via
		        // options, so beforeToolCall returns reject() on the very first tool call.
		        chunks = []
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

		        sawRejectSentinel = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" && c.result.isRejected() )
		        toolNotCalled     = toolACalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawRejectSentinel" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNotCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "afterAgentRunFired" ) ) ).isFalse();
	}

}
