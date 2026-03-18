package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Built-in Middleware Unit Tests" )
public class BuiltinMiddlewareTest extends BaseIntegrationTest {

	// ---- LoggingMiddleware ----

	@DisplayName( "LoggingMiddleware: all hooks return continue()" )
	@Test
	public void testLoggingMiddlewareReturnsContinue() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.LoggingMiddleware;

		        mw = new LoggingMiddleware( logToFile: false, logToConsole: false );

		        r1 = mw.beforeAgentRun( context: { input: "test" } );
		        r2 = mw.beforeLLMCall( context: {} );
		        r3 = mw.beforeToolCall( context: { toolName: "test" } );
		        r4 = mw.afterAgentRun( context: { response: "nothing" } );

		        allContinue = r1.isContinue() && r2.isContinue() && r3.isContinue() && r4.isContinue();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "allContinue" ) ) ).isTrue();
	}

	// ---- MaxToolCallsMiddleware ----

	@DisplayName( "MaxToolCallsMiddleware: cancels when limit is exceeded" )
	@Test
	public void testMaxToolCallsMiddlewareCancels() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.MaxToolCallsMiddleware;

		        mw = new MaxToolCallsMiddleware( maxCalls: 2 );

		        ctx = { toolName: "doSomething" };

		        // Simulate 3 tool calls; 3rd should be cancelled
		        r1 = mw.beforeToolCall( context: ctx );
		        r2 = mw.beforeToolCall( context: ctx );
		        r3 = mw.beforeToolCall( context: ctx );

		        firstIsOk      = r1.isContinue();
		        secondIsOk     = r2.isContinue();
		        thirdIsCancelled = r3.isCancelled();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "firstIsOk" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "secondIsOk" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "thirdIsCancelled" ) ) ).isTrue();
	}

	@DisplayName( "MaxToolCallsMiddleware: counter resets on beforeAgentRun" )
	@Test
	public void testMaxToolCallsReset() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.MaxToolCallsMiddleware;

		        mw     = new MaxToolCallsMiddleware( maxCalls: 1 );
		        ctx    = { context: { toolName: "x" } };

		        // Use up the quota
		        mw.beforeToolCall( ctx );
		        r1 = mw.beforeToolCall( ctx );
		        r1IsCancelled = r1.isCancelled();

		        // Reset via beforeAgentRun
		        mw.beforeAgentRun( {} );

		        r2 = mw.beforeToolCall( ctx );
		        r2IsContinue = r2.isContinue();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "r1IsCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "r2IsContinue" ) ) ).isTrue();
	}

	// ---- GuardrailMiddleware ----

	@DisplayName( "GuardrailMiddleware: blocks tool in blockedTools list" )
	@Test
	public void testGuardrailBlockedTool() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.GuardrailMiddleware;

		        mw = new GuardrailMiddleware( blockedTools: [ "dangerousTool" ] );

		        result = mw.beforeToolCall( { toolName: "dangerousTool", toolArgs: {} } );
		        resultIsRejected = result.isRejected();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsRejected" ) ) ).isTrue();
	}

	@DisplayName( "GuardrailMiddleware: allows non-blocked tool" )
	@Test
	public void testGuardrailAllowedTool() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.GuardrailMiddleware;

		        mw = new GuardrailMiddleware( blockedTools: [ "dangerousTool" ] );

		        result = mw.beforeToolCall( { toolName: "safeTool", toolArgs: {} } );
		        resultIsContinue = result.isContinue();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsContinue" ) ) ).isTrue();
	}

	@DisplayName( "GuardrailMiddleware: blocks tool with matching arg pattern" )
	@Test
	public void testGuardrailArgPattern() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.GuardrailMiddleware;

		        mw = new GuardrailMiddleware(
		            argPatterns: {
		                runQuery: [
		                    { sql: "(?i)\\bDROP\\b" }
		                ]
		            }
		        );

		        dangerous = mw.beforeToolCall( { toolName: "runQuery", toolArgs: { sql: "DROP TABLE users" } } );
		        safe      = mw.beforeToolCall( { toolName: "runQuery", toolArgs: { sql: "SELECT * FROM users" } } );

		        dangerousIsRejected = dangerous.isRejected();
		        safeIsContinue      = safe.isContinue();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "dangerousIsRejected" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "safeIsContinue" ) ) ).isTrue();
	}

	// ---- HumanInTheLoopMiddleware ----

	@DisplayName( "HumanInTheLoopMiddleware: web mode suspends matching tool" )
	@Test
	public void testHITLWebModeSuspends() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.HumanInTheLoopMiddleware;

		        mw = new HumanInTheLoopMiddleware(
		            toolsRequiringApproval: [ "placeOrder" ],
		            mode: "web"
		        );

		        result = mw.beforeToolCall( context: { toolName: "placeOrder", toolCall: {} } );
		        resultIsSuspended = result.isSuspended();
		        resultData = result.getData();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsSuspended" ) ) ).isTrue();
	}

	@DisplayName( "HumanInTheLoopMiddleware: web mode ignores non-listed tool" )
	@Test
	public void testHITLWebModeSkipsUnlisted() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.HumanInTheLoopMiddleware;

		        mw = new HumanInTheLoopMiddleware(
		            toolsRequiringApproval: [ "placeOrder" ],
		            mode: "web"
		        );

		        result = mw.beforeToolCall( context: { toolName: "getWeather", toolCall: {} } );
		        resultIsContinue = result.isContinue();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsContinue" ) ) ).isTrue();
	}

	@DisplayName( "HumanInTheLoopMiddleware: resume with 'approve' returns continue" )
	@Test
	public void testHITLResumeApprove() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.requests.AiChatRequest;

		        mw = new HumanInTheLoopMiddleware(
		            toolsRequiringApproval: [ "placeOrder" ],
		            mode: "web"
		        );

		        // Simulate the chatRequest that AiAgent.resume() creates
		        chatRequest = new AiChatRequest(
		            options: {
		                _resumeContext: {
		                    resumeDecision : "approve",
		                    suspendData    : { toolName: "placeOrder" },
		                    editedData     : {}
		                }
		            }
		        );

		        ctx = { toolName: "placeOrder", toolCall: {}, chatRequest: chatRequest };
		        result = mw.beforeToolCall( context: ctx );
		        resultIsContinue = result.isContinue();

		        // resumeContext should be cleared after consumption
		        resumeContextCleared = chatRequest.getResumeContext().isEmpty();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsContinue" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resumeContextCleared" ) ) ).isTrue();
	}

	@DisplayName( "HumanInTheLoopMiddleware: resume with 'reject' returns reject" )
	@Test
	public void testHITLResumeReject() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.requests.AiChatRequest;

		        mw = new HumanInTheLoopMiddleware(
		            toolsRequiringApproval: [ "placeOrder" ],
		            mode: "web"
		        );

		        chatRequest = new AiChatRequest(
		            options: {
		                _resumeContext: {
		                    resumeDecision : "reject",
		                    suspendData    : { toolName: "placeOrder" },
		                    editedData     : {}
		                }
		            }
		        );

		        ctx    = { toolName: "placeOrder", toolCall: {}, chatRequest: chatRequest };
		        result = mw.beforeToolCall( context: ctx );
		        resultIsRejected = result.isRejected();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsRejected" ) ) ).isTrue();
	}

	@DisplayName( "HumanInTheLoopMiddleware: resume with 'edit' patches tool args and returns continue" )
	@Test
	public void testHITLResumeEdit() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.requests.AiChatRequest;

		        mw = new HumanInTheLoopMiddleware(
		            toolsRequiringApproval: [ "placeOrder" ],
		            mode: "web"
		        );

		        chatRequest = new AiChatRequest(
		            options: {
		                _resumeContext: {
		                    resumeDecision : "edit",
		                    suspendData    : { toolName: "placeOrder" },
		                    editedData     : { correctedArgs: { qty: 5, item: "widget" } }
		                }
		            }
		        );

		        toolCallStruct = { id: "call_123", function: { name: "placeOrder", arguments: '{"qty":1}' } };
		        ctx = { toolName: "placeOrder", toolCall: toolCallStruct, chatRequest: chatRequest };

		        result       = mw.beforeToolCall( context: ctx );
		        resultIsContinue = result.isContinue();

		        // Arguments should be patched in place
		        parsedArgs   = jsonDeserialize( toolCallStruct.function.arguments );
		        argQtyPatched = parsedArgs.qty == 5;
		        argItemSet    = parsedArgs.item == "widget";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsContinue" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "argQtyPatched" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "argItemSet" ) ) ).isTrue();
	}

	@DisplayName( "HumanInTheLoopMiddleware: after resume, next tool call goes through normal HITL" )
	@Test
	public void testHITLResumeContextConsumedOnce() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.requests.AiChatRequest;

		        mw = new HumanInTheLoopMiddleware(
		            toolsRequiringApproval: [ "placeOrder" ],
		            mode: "web"
		        );

		        chatRequest = new AiChatRequest(
		            options: {
		                _resumeContext: {
		                    resumeDecision : "approve",
		                    suspendData    : { toolName: "placeOrder" },
		                    editedData     : {}
		                }
		            }
		        );

		        ctx = { toolName: "placeOrder", toolCall: {}, chatRequest: chatRequest };

		        // First call: resume context is consumed → continue
		        r1 = mw.beforeToolCall( context: ctx );
		        r1IsContinue = r1.isContinue();

		        // Second call on the same tool: normal HITL → suspend again
		        r2 = mw.beforeToolCall( context: ctx );
		        r2IsSuspended = r2.isSuspended();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "r1IsContinue" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "r2IsSuspended" ) ) ).isTrue();
	}
}