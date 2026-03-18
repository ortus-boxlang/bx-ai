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
}
