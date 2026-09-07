package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Issue #248 Part B — CohereService.chatStream()'s tool loop.
 *
 * Fully offline. CohereService.chatStream() routes its SSE transport through wrapLLMCall (context
 * carries `stream: true` plus `emitSSEChunk`), so a middleware can replay canned Cohere v1 stream
 * events instead of calling the network — and the follow-up turn after tool results is
 * wrapLLMCall-wrapped exactly like chat()'s blocking call.
 *
 * The service targets Cohere's v1 /chat API (see CohereService.init()), so the events replayed here
 * are the v1 ones: tool-calls-chunk / tool-calls-generation / stream-end.
 */
public class CohereStreamingToolsTest extends BaseIntegrationTest {

	@DisplayName( "Streamed tool_call_delta chunks accumulate into a parsed tool call, beforeToolCall fires, and the follow-up answer is emitted" )
	@Test
	public void testStreamToolCallAccumulationAndToolLoop() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        toolInvoked  = 0
		        capturedCity = ""
		        weatherTool  = aiTool( "getWeather", "Get the weather for a city", ( required string city ) => {
		            toolInvoked++
		            capturedCity = arguments.city
		            return "sunny"
		        } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "What is the weather in Paris?" ),
		            { model: "command-a-03-2025", tools: [ weatherTool ] },
		            { provider: "cohere" }
		        )

		        streamCalls         = 0
		        followUpCalls       = 0
		        capturedToolResults = []
		        beforeToolCallFired = false
		        capturedToolName    = ""
		        capturedToolArgs    = {}

		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    streamCalls++
		                    // Cohere v1 streams the tool call as partial JSON across several chunks
		                    ctx.emitSSEChunk( { "data": jsonSerialize( { "event_type": "stream-start", "generation_id": "gen-1" } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"      : "tool-calls-chunk",
		                        "text"            : "I will look up the weather.",
		                        "tool_call_delta" : { "index": 0, "name": "getWeather", "parameters": '{"city":' }
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"      : "tool-calls-chunk",
		                        "tool_call_delta" : { "index": 0, "parameters": '"Paris"}' }
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"   : "stream-end",
		                        "finish_reason": "COMPLETE",
		                        "response"     : { "meta": { "billed_units": { "input_tokens": 10, "output_tokens": 5 } } }
		                    } ) } )
		                    return {}
		                }

		                // The follow-up turn carrying tool results back to Cohere
		                followUpCalls++
		                capturedToolResults = ctx.dataPacket.tool_results ?: []
		                return { "text": "It is sunny in Paris." }
		            },
		            "beforeToolCall": ( ctx ) => {
		                beforeToolCallFired = true
		                capturedToolName    = ctx.toolName ?: ""
		                capturedToolArgs    = ctx.toolArgs ?: {}
		                return AiMiddlewareResult::continue()
		            }
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        contentChunks = chunks.filter( c => isStruct( c ) && c.keyExists( "choices" ) )
		        finalText = contentChunks.map( c => c.choices.first().delta.content ?: "" ).toList( "" )
		        toolPlan  = contentChunks.map( c => c.choices.first().delta.reasoning ?: "" ).toList( "" )

		        sawMiddlewareStop = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        oneStreamCall     = streamCalls == 1
		        oneFollowUpCall   = followUpCalls == 1
		        toolRanOnce       = toolInvoked == 1
		        cityParsed        = capturedCity == "Paris"
		        argsNormalized    = ( capturedToolArgs.city ?: "" ) == "Paris"
		        toolResultOK      = capturedToolResults.len() == 1
		            && ( capturedToolResults.first().call.name ?: "" ) == "getWeather"
		            && ( capturedToolResults.first().outputs.first().result ?: "" ) == "sunny"
		        isFinalText       = finalText == "It is sunny in Paris."
		        planStreamed      = toolPlan.findNoCase( "look up the weather" ) > 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawMiddlewareStop" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "oneStreamCall" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "oneFollowUpCall" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "cityParsed" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "beforeToolCallFired" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "capturedToolName" ) ) ).isEqualTo( "getWeather" );
		assertThat( variables.getAsBoolean( Key.of( "argsNormalized" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolResultOK" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "planStreamed" ) ) ).isTrue();
	}

	@DisplayName( "Two approval-gated tool calls in a stream suspend ONCE as a single batch, with a resume ledger" )
	@Test
	public void testStreamTwoApprovalToolsSuspendAsOneBatch() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A - requires approval", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "please run toolA and toolB" ),
		            { model: "command-a-03-2025", tools: [ toolA, toolB ] },
		            { provider: "cohere" }
		        )

		        followUpCalls = 0
		        chatRequest.addMiddleware( [
		            new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ),
		            {
		                "wrapLLMCall": ( ctx, handler ) => {
		                    if( ctx.stream ?: false ){
		                        // The authoritative v1 event: the complete tool call list in one frame
		                        ctx.emitSSEChunk( { "data": jsonSerialize( {
		                            "event_type": "tool-calls-generation",
		                            "tool_calls": [
		                                { "name": "toolA", "parameters": {} },
		                                { "name": "toolB", "parameters": {} }
		                            ]
		                        } ) } )
		                        ctx.emitSSEChunk( { "data": jsonSerialize( {
		                            "event_type"   : "stream-end",
		                            "finish_reason": "COMPLETE",
		                            "response"     : { "meta": { "billed_units": { "input_tokens": 4, "output_tokens": 4 } } }
		                        } ) } )
		                        return {}
		                    }
		                    followUpCalls++
		                    return { "text": "should never be reached" }
		                }
		            }
		        ] )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        stopChunks     = chunks.filter( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        suspendedOnce  = stopChunks.len() == 1
		        stopChunk      = stopChunks.first()
		        isSuspended    = !isNull( stopChunk ) && stopChunk.result.isSuspended()
		        suspendData    = !isNull( stopChunk ) ? stopChunk.result.getData() : {}
		        bothPending    = ( suspendData.pendingActions ?: [] ).len() == 2
		        hasLedger      = ( suspendData.resumeLedger ?: [] ).len() == 2
		        hasAssistant   = suspendData.keyExists( "assistantMessage" )
		            && ( suspendData.assistantMessage.toolCalls ?: [] ).len() == 2
		        neitherRan     = toolACalls == 0 && toolBCalls == 0
		        noFollowUpCall = followUpCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspendedOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothPending" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasLedger" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasAssistant" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neitherRan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noFollowUpCall" ) ) ).isTrue();
	}

	@DisplayName( "chatStream() honours _resumeContext: the batch finishes from the ledger with no LLM replay" )
	@Test
	public void testStreamResumeToolBatchFinishesLedgerDirectly() {
		// @formatter:off
		runtime.executeSource(
		    """
		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )

		        provider = aiService( "cohere", { apiKey: "dummy-key" } )

		        chatRequest = aiChatRequest(
		            aiMessage().user( "please run toolA and toolB" ),
		            { model: "command-a-03-2025", tools: [ toolA, toolB ] },
		            {
		                provider: "cohere",
		                _resumeContext: {
		                    // The shape CohereService captures when it suspends a streamed batch
		                    assistantMessage: {
		                        toolCalls    : [ { name: "toolA", parameters: {} }, { name: "toolB", parameters: {} } ],
		                        followUpTools: [],
		                        chatHistory  : [],
		                        preamble     : "",
		                        tempParams   : {}
		                    },
		                    resumeLedger: [
		                        { toolName: "toolA", status: "execute" },
		                        { toolName: "toolB", status: "blocked", reason: "not needed" }
		                    ]
		                }
		            }
		        )

		        llmCalls          = 0
		        capturedResults   = []
		        sawStreamCall     = false
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    sawStreamCall = true
		                    return {}
		                }
		                llmCalls++
		                capturedResults = ctx.dataPacket.tool_results ?: []
		                return { "text": "toolA ran, toolB was rejected." }
		            }
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        sawMiddlewareStop = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        finalText = chunks
		            .filter( c => isStruct( c ) && c.keyExists( "choices" ) )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )
		        isFinalText   = finalText == "toolA ran, toolB was rejected."
		        toolARan      = toolACalls == 1
		        toolBSkipped  = toolBCalls == 0
		        onlyFollowUp  = llmCalls == 1
		        blockedResult = capturedResults.len() == 2
		            && ( capturedResults[ 2 ].outputs.first().result ?: "" ).findNoCase( "blocked" ) > 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawStreamCall" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "sawMiddlewareStop" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBSkipped" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "onlyFollowUp" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "blockedResult" ) ) ).isTrue();
	}

	@DisplayName( "A stream that truncates a tool call's JSON arguments throws instead of running the tool with defaults" )
	@Test
	public void testTruncatedStreamToolArgumentsThrowsAndNeverRunsTool() {
		// @formatter:off
		runtime.executeSource(
		    """
		        toolInvoked = 0
		        weatherTool = aiTool( "getWeather", "Get the weather for a city", ( string city = "" ) => {
		            toolInvoked++
		            return "sunny"
		        } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "What is the weather in Paris?" ),
		            { model: "command-a-03-2025", tools: [ weatherTool ] },
		            { provider: "cohere" }
		        )

		        followUpCalls = 0
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    // The stream dies mid-arguments: only '{"city":' ever arrives
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"      : "tool-calls-chunk",
		                        "tool_call_delta" : { "index": 0, "name": "getWeather", "parameters": '{"city":' }
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"   : "stream-end",
		                        "finish_reason": "MAX_TOKENS",
		                        "response"     : { "meta": { "billed_units": { "input_tokens": 10, "output_tokens": 5 } } }
		                    } ) } )
		                    return {}
		                }
		                followUpCalls++
		                return { "text": "should never be reached" }
		            }
		        } )

		        threw        = false
		        errorType    = ""
		        errorMessage = ""
		        try {
		            provider.chatStream( chatRequest, ( chunk ) => {} )
		        } catch( any e ){
		            threw        = true
		            errorType    = e.type
		            errorMessage = e.message
		        }

		        namesTool    = errorMessage.findNoCase( "getWeather" ) > 0
		        saysTruncated = errorMessage.findNoCase( "truncated" ) > 0
		        saysNotRun   = errorMessage.findNoCase( "NOT executed" ) > 0
		        toolNeverRan = toolInvoked == 0
		        noFollowUp   = followUpCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threw" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "CohereStreamError" );
		assertThat( variables.getAsBoolean( Key.of( "namesTool" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "saysTruncated" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "saysNotRun" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNeverRan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noFollowUp" ) ) ).isTrue();
	}

	@DisplayName( "A follow-up turn that asks for ANOTHER tool continues the same exchange instead of restarting it" )
	@Test
	public void testStreamMultiRoundFollowUpPreservesExchange() {
		// @formatter:off
		runtime.executeSource(
		    """
		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "run toolA then toolB" ),
		            { model: "command-a-03-2025", tools: [ toolA, toolB ] },
		            { provider: "cohere" }
		        )

		        llmCalls     = 0
		        requestBodies = []
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                llmCalls++
		                if( ctx.stream ?: false ){
		                    // Round 1: the stream asks for toolA
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type": "tool-calls-generation",
		                        "tool_calls": [ { "name": "toolA", "parameters": {} } ]
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"   : "stream-end",
		                        "finish_reason": "COMPLETE",
		                        "response"     : { "meta": { "billed_units": { "input_tokens": 4, "output_tokens": 4 } } }
		                    } ) } )
		                    return {}
		                }

		                requestBodies.append( duplicate( ctx.dataPacket ?: {} ) )

		                // Follow-up #1 asks for toolB; follow-up #2 answers.
		                if( requestBodies.len() == 1 ){
		                    return { "tool_calls": [ { "name": "toolB", "parameters": {} } ] }
		                }
		                return { "text": "Both tools ran." }
		            }
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        finalText = chunks
		            .filter( c => isStruct( c ) && c.keyExists( "choices" ) )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )

		        sawMiddlewareStop = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        toolARanOnce      = toolACalls == 1
		        toolBRanOnce      = toolBCalls == 1
		        threeLLMCalls     = llmCalls == 3
		        twoFollowUps      = requestBodies.len() == 2

		        // The SECOND request (follow-up #1) must carry round 1's tool_results
		        firstResults  = requestBodies[ 1 ].tool_results ?: []
		        round1Carried = firstResults.len() == 1
		            && ( firstResults.first().call.name ?: "" ) == "toolA"
		            && ( firstResults.first().outputs.first().result ?: "" ) == "A done"

		        // The THIRD request must still carry round 1 alongside round 2 — progress preserved
		        secondResults  = requestBodies[ 2 ].tool_results ?: []
		        exchangeKept   = secondResults.len() == 2
		            && ( secondResults[ 1 ].call.name ?: "" ) == "toolA"
		            && ( secondResults[ 2 ].call.name ?: "" ) == "toolB"

		        isFinalText = finalText == "Both tools ran."
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawMiddlewareStop" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "toolARanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "threeLLMCalls" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "twoFollowUps" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "round1Carried" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "exchangeKept" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
	}

	@DisplayName( "A resumed batch whose follow-up asks for ANOTHER tool continues the exchange instead of restarting it" )
	@Test
	public void testStreamResumeMultiRoundFollowUpDoesNotRestart() {
		// @formatter:off
		runtime.executeSource(
		    """
		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "please run toolA" ),
		            { model: "command-a-03-2025", tools: [ toolA, toolB ] },
		            {
		                provider: "cohere",
		                _resumeContext: {
		                    assistantMessage: {
		                        toolCalls    : [ { name: "toolA", parameters: {} } ],
		                        followUpTools: [],
		                        chatHistory  : [],
		                        preamble     : "",
		                        tempParams   : {}
		                    },
		                    resumeLedger: [ { toolName: "toolA", status: "execute" } ]
		                }
		            }
		        )

		        llmCalls      = 0
		        sawStreamCall = false
		        requestBodies = []
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    sawStreamCall = true
		                    return {}
		                }
		                llmCalls++
		                requestBodies.append( duplicate( ctx.dataPacket ?: {} ) )
		                // Follow-up #1 asks for toolB; follow-up #2 answers.
		                if( llmCalls == 1 ){
		                    return { "tool_calls": [ { "name": "toolB", "parameters": {} } ] }
		                }
		                return { "text": "Both tools ran." }
		            }
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        finalText = chunks
		            .filter( c => isStruct( c ) && c.keyExists( "choices" ) )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )

		        sawMiddlewareStop = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        toolARanOnce      = toolACalls == 1
		        toolBRanOnce      = toolBCalls == 1
		        twoLLMCalls       = llmCalls == 2
		        isFinalText       = finalText == "Both tools ran."

		        // The SECOND follow-up must still carry round 1's toolA result alongside toolB's
		        secondResults = requestBodies[ 2 ].tool_results ?: []
		        exchangeKept  = secondResults.len() == 2
		            && ( secondResults[ 1 ].call.name ?: "" ) == "toolA"
		            && ( secondResults[ 2 ].call.name ?: "" ) == "toolB"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawStreamCall" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "sawMiddlewareStop" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "toolARanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "twoLLMCalls" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "exchangeKept" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
	}

	@DisplayName( "A resumed batch whose LATER round suspends again emits exactly ONE middleware_stop" )
	@Test
	public void testStreamResumeLaterRoundSuspendEmitsSingleStop() {
		// resumeToolBatchStream() passes the streaming callback straight into resumeToolBatch() as
		// the emit adapter, so executeStreamToolBatch() already pushed the middleware_stop for the
		// round-2 suspension. Returning that same terminal must NOT emit it a second time.
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B - requires approval", () => { toolBCalls++; return "B done" } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "please run toolA" ),
		            { model: "command-a-03-2025", tools: [ toolA, toolB ] },
		            {
		                provider: "cohere",
		                _resumeContext: {
		                    assistantMessage: {
		                        toolCalls    : [ { name: "toolA", parameters: {} } ],
		                        followUpTools: [],
		                        chatHistory  : [],
		                        preamble     : "",
		                        tempParams   : {}
		                    },
		                    resumeLedger: [ { toolName: "toolA", status: "execute" } ]
		                }
		            }
		        )

		        llmCalls = 0
		        chatRequest.addMiddleware( [
		            new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolB" ], mode: "web" ),
		            {
		                "wrapLLMCall": ( ctx, handler ) => {
		                    if( ctx.stream ?: false ){
		                        return {}
		                    }
		                    llmCalls++
		                    // The resumed follow-up asks for toolB, which needs approval -> round 2 suspends.
		                    return { "tool_calls": [ { "name": "toolB", "parameters": {} } ] }
		                }
		            }
		        ] )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        stopChunks    = chunks.filter( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
		        exactlyOneStop = stopChunks.len() == 1
		        stopChunk      = stopChunks.first()
		        isSuspended    = !isNull( stopChunk ) && stopChunk.result.isSuspended()
		        toolARanOnce   = toolACalls == 1
		        toolBNeverRan  = toolBCalls == 0
		        oneLLMCall     = llmCalls == 1
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "exactlyOneStop" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolARanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBNeverRan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "oneLLMCall" ) ) ).isTrue();
	}

	@DisplayName( "chat(): a follow-up that asks for ANOTHER tool continues the exchange instead of restarting it" )
	@Test
	public void testSyncMultiRoundFollowUpPreservesExchange() {
		// @formatter:off
		runtime.executeSource(
		    """
		        toolACalls = 0
		        toolBCalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )
		        toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "run toolA then toolB" ),
		            { model: "command-a-03-2025", tools: [ toolA, toolB ] },
		            { provider: "cohere" }
		        )

		        llmCalls      = 0
		        requestBodies = []
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                llmCalls++
		                requestBodies.append( duplicate( ctx.dataPacket ?: {} ) )
		                if( llmCalls == 1 ){
		                    return { "tool_calls": [ { "name": "toolA", "parameters": {} } ] }
		                }
		                if( llmCalls == 2 ){
		                    return { "tool_calls": [ { "name": "toolB", "parameters": {} } ] }
		                }
		                return { "text": "Both tools ran." }
		            }
		        } )

		        answer = provider.chat( chatRequest )

		        toolARanOnce  = toolACalls == 1
		        toolBRanOnce  = toolBCalls == 1
		        threeLLMCalls = llmCalls == 3
		        isFinalText   = answer == "Both tools ran."

		        // The third request must carry BOTH rounds' results — the exchange, not a restart
		        secondResults = requestBodies[ 3 ].tool_results ?: []
		        exchangeKept  = secondResults.len() == 2
		            && ( secondResults[ 1 ].call.name ?: "" ) == "toolA"
		            && ( secondResults[ 2 ].call.name ?: "" ) == "toolB"

		        // A restart would have re-sent the original user prompt with no tool results at all
		        noRestart = !( requestBodies[ 3 ].keyExists( "message" ) )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "toolARanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolBRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "threeLLMCalls" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "exchangeKept" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noRestart" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
	}

	@DisplayName( "A resumed stream with returnFormat all still delivers a terminating chunk to the consumer" )
	@Test
	public void testStreamResumeNonSimpleReturnFormatStillTerminates() {
		// @formatter:off
		runtime.executeSource(
		    """
		        toolACalls = 0
		        toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "please run toolA" ),
		            { model: "command-a-03-2025", tools: [ toolA ] },
		            {
		                provider     : "cohere",
		                returnFormat : "all",
		                _resumeContext: {
		                    assistantMessage: {
		                        toolCalls    : [ { name: "toolA", parameters: {} } ],
		                        followUpTools: [],
		                        chatHistory  : [],
		                        preamble     : "",
		                        tempParams   : {}
		                    },
		                    resumeLedger: [ { toolName: "toolA", status: "execute" } ]
		                }
		            }
		        )

		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    return {}
		                }
		                return { "text": "toolA ran.", "generation_id": "gen-9" }
		            }
		        } )

		        chunks = []
		        provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

		        contentChunks = chunks.filter( c => isStruct( c ) && c.keyExists( "choices" ) )
		        gotChunks     = contentChunks.len() > 0
		        sawTerminator = contentChunks.some( c => ( c.choices.first().finish_reason ?: "" ) == "stop" )
		        deliveredText = contentChunks
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )
		        contentDelivered = deliveredText.findNoCase( "toolA ran." ) > 0
		        toolRanOnce      = toolACalls == 1
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "gotChunks" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawTerminator" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "contentDelivered" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolRanOnce" ) ) ).isTrue();
	}

	@DisplayName( "A streamed tool call with arguments but no name throws instead of being silently dropped" )
	@Test
	public void testNamelessStreamToolCallBufferThrows() {
		// @formatter:off
		runtime.executeSource(
		    """
		        toolInvoked = 0
		        weatherTool = aiTool( "getWeather", "Get the weather for a city", ( string city = "" ) => {
		            toolInvoked++
		            return "sunny"
		        } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "What is the weather in Paris?" ),
		            { model: "command-a-03-2025", tools: [ weatherTool ] },
		            { provider: "cohere" }
		        )

		        followUpCalls = 0
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    // Complete arguments arrive, but the tool name never does
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"      : "tool-calls-chunk",
		                        "tool_call_delta" : { "index": 0, "parameters": '{"city":"Paris"}' }
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"   : "stream-end",
		                        "finish_reason": "COMPLETE",
		                        "response"     : { "meta": { "billed_units": { "input_tokens": 4, "output_tokens": 4 } } }
		                    } ) } )
		                    return {}
		                }
		                followUpCalls++
		                return { "text": "should never be reached" }
		            }
		        } )

		        chunks       = []
		        threw        = false
		        errorType    = ""
		        errorMessage = ""
		        try {
		            provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )
		        } catch( any e ){
		            threw        = true
		            errorType    = e.type
		            errorMessage = e.message
		        }

		        // The consumer was already told this turn called tools, so a silent drop is a lie
		        sawToolCallsFinish = chunks.some( c => isStruct( c ) && c.keyExists( "choices" ) && ( c.choices.first().finish_reason ?: "" ) == "tool_calls" )
		        namesIndex   = errorMessage.findNoCase( "index [0]" ) > 0
		        saysNotRun   = errorMessage.findNoCase( "NOT executed" ) > 0
		        toolNeverRan = toolInvoked == 0
		        noFollowUp   = followUpCalls == 0
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawToolCallsFinish" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "threw" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "CohereStreamError" );
		assertThat( variables.getAsBoolean( Key.of( "namesIndex" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "saysNotRun" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNeverRan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noFollowUp" ) ) ).isTrue();
	}

	@DisplayName( "beforeToolCall's replacement of ctx.toolArgs is what the tool actually receives (stream)" )
	@Test
	public void testStreamBeforeToolCallArgReplacementReachesTool() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        capturedCity = ""
		        weatherTool  = aiTool( "getWeather", "Get the weather for a city", ( required string city ) => {
		            capturedCity = arguments.city
		            return "sunny"
		        } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "What is the weather in Paris?" ),
		            { model: "command-a-03-2025", tools: [ weatherTool ] },
		            { provider: "cohere" }
		        )

		        wrappedArgs = {}
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type": "tool-calls-generation",
		                        "tool_calls": [ { "name": "getWeather", "parameters": { "city": "Paris" } } ]
		                    } ) } )
		                    ctx.emitSSEChunk( { "data": jsonSerialize( {
		                        "event_type"   : "stream-end",
		                        "finish_reason": "COMPLETE"
		                    } ) } )
		                    return {}
		                }
		                return { "text": "done" }
		            },
		            "beforeToolCall": ( ctx ) => {
		                ctx.toolArgs = { "city": "London" }
		                return AiMiddlewareResult::continue()
		            },
		            "wrapToolCall": ( ctx, handler ) => {
		                wrappedArgs = ctx.toolArgs ?: {}
		                return handler( ctx )
		            }
		        } )

		        provider.chatStream( chatRequest, ( chunk ) => {} )

		        toolSawReplacement = capturedCity == "London"
		        wrapSawReplacement = ( wrappedArgs.city ?: "" ) == "London"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "wrapSawReplacement" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolSawReplacement" ) ) ).isTrue();
	}

	@DisplayName( "beforeToolCall's replacement of ctx.toolArgs is what the tool actually receives (chat)" )
	@Test
	public void testSyncBeforeToolCallArgReplacementReachesTool() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        capturedCity = ""
		        weatherTool  = aiTool( "getWeather", "Get the weather for a city", ( required string city ) => {
		            capturedCity = arguments.city
		            return "sunny"
		        } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "What is the weather in Paris?" ),
		            { model: "command-a-03-2025", tools: [ weatherTool ] },
		            { provider: "cohere" }
		        )

		        llmCalls = 0
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                llmCalls++
		                if( llmCalls == 1 ){
		                    return { "tool_calls": [ { "name": "getWeather", "parameters": { "city": "Paris" } } ] }
		                }
		                return { "text": "done" }
		            },
		            "beforeToolCall": ( ctx ) => {
		                ctx.toolArgs = { "city": "London" }
		                return AiMiddlewareResult::continue()
		            }
		        } )

		        answer             = provider.chat( chatRequest )
		        toolSawReplacement = capturedCity == "London"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "toolSawReplacement" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "answer" ) ) ).isEqualTo( "done" );
	}

	@DisplayName( "Resuming a batch whose tool is no longer bound yields a Tool not found result instead of throwing" )
	@Test
	public void testStreamResumeWithMissingToolDoesNotThrow() {
		// @formatter:off
		runtime.executeSource(
		    """
		        renamedCalls = 0
		        // The tool was renamed between suspend and resume — the ledger still names the old one
		        renamedTool = aiTool( "toolARenamed", "Tool A, renamed", () => { renamedCalls++; return "A done" } )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "please run toolA" ),
		            { model: "command-a-03-2025", tools: [ renamedTool ] },
		            {
		                provider: "cohere",
		                _resumeContext: {
		                    assistantMessage: {
		                        toolCalls    : [ { name: "toolA", parameters: {} } ],
		                        followUpTools: [],
		                        chatHistory  : [],
		                        preamble     : "",
		                        tempParams   : {}
		                    },
		                    resumeLedger: [ { toolName: "toolA", status: "execute" } ]
		                }
		            }
		        )

		        capturedResults = []
		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                if( ctx.stream ?: false ){
		                    return {}
		                }
		                capturedResults = ctx.dataPacket.tool_results ?: []
		                return { "text": "I could not run that tool." }
		            }
		        } )

		        threw     = false
		        errorMsg  = ""
		        chunks    = []
		        try {
		            provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )
		        } catch( any e ){
		            threw    = true
		            errorMsg = e.message
		        }

		        finalText = chunks
		            .filter( c => isStruct( c ) && c.keyExists( "choices" ) )
		            .map( c => c.choices.first().delta.content ?: "" )
		            .toList( "" )

		        notFoundResult = capturedResults.len() == 1
		            && ( capturedResults.first().outputs.first().result ?: "" ).findNoCase( "not found" ) > 0
		        nothingRan  = renamedCalls == 0
		        isFinalText = finalText == "I could not run that tool."
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threw" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "notFoundResult" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "nothingRan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isFinalText" ) ) ).isTrue();
	}

	@DisplayName( "An exception thrown by the caller's stream callback propagates out of chatStream(), announced on onAIError once" )
	@Test
	public void testStreamCallbackErrorPropagates() {
		// @formatter:off
		runtime.executeSource(
		    """
		        // The counter lives in THIS script's variables scope, so a leaked registration only
		        // ever increments a scope no later test reads.
		        errorEvents = 0
		        BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )

		        provider    = aiService( "cohere", { apiKey: "dummy-key" } )
		        chatRequest = aiChatRequest(
		            aiMessage().user( "What is the weather in Paris?" ),
		            { model: "command-a-03-2025" },
		            { provider: "cohere" }
		        )

		        chatRequest.addMiddleware( {
		            "wrapLLMCall": ( ctx, handler ) => {
		                ctx.emitSSEChunk( { "data": jsonSerialize( { "event_type": "text-generation", "text": "It is sunny." } ) } )
		                ctx.emitSSEChunk( { "data": jsonSerialize( {
		                    "event_type"   : "stream-end",
		                    "finish_reason": "COMPLETE",
		                    "response"     : { "meta": { "billed_units": { "input_tokens": 10, "output_tokens": 5 } } }
		                } ) } )
		                return {}
		            }
		        } )

		        errType = ""
		        errMsg  = ""
		        try {
		            // The readme idiom: fine on the text-generation chunk, throws on stream-end,
		            // whose delta carries the terminal finish_reason.
		            provider.chatStream( chatRequest, ( chunk ) => {
		                if ( ( chunk.choices?.first()?.finish_reason ?: "" ).len() ) {
		                    throw( type: "CallerBoom", message: "caller callback failed" )
		                }
		            } )
		        } catch ( any e ) {
		            errType = e.type
		            errMsg  = e.message
		        }
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "caller callback failed" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
	}
}
