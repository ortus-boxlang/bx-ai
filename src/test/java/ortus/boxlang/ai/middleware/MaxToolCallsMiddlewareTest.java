package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.builtin.MaxToolCallsMiddleware;
import bx.middleware.AiMiddlewareResult;
import java.util.HashMap;
import java.util.Map;

public class MaxToolCallsMiddlewareTest extends BaseMiddlewareTest {

	private Map<String, Object> toolCtx( String toolName ) {
		Map<String, Object> ctx = new HashMap<>();
		ctx.put( "toolName", toolName );
		return ctx;
	}

	private Map<String, Object> agentCtx() {
		return new HashMap<>();
	}

	@Test
	public void testAllowsCallsUpToLimit() {
		MaxToolCallsMiddleware mw = new MaxToolCallsMiddleware( 3, "end" );
		mw.beforeAgentRun( agentCtx() );

		assertTrue( mw.beforeToolCall( toolCtx( "t" ) ).isContinue() );
		assertTrue( mw.beforeToolCall( toolCtx( "t" ) ).isContinue() );
		assertTrue( mw.beforeToolCall( toolCtx( "t" ) ).isContinue() );
	}

	@Test
	public void testCancelsOnExceedingLimit() {
		MaxToolCallsMiddleware mw = new MaxToolCallsMiddleware( 2, "end" );
		mw.beforeAgentRun( agentCtx() );

		mw.beforeToolCall( toolCtx( "t" ) );  // call 1
		mw.beforeToolCall( toolCtx( "t" ) );  // call 2
		AiMiddlewareResult result = mw.beforeToolCall( toolCtx( "t" ) );  // call 3 — over limit

		assertTrue( result.isCancelled() );
		assertTrue( result.isTerminal() );
	}

	@Test
	public void testExitBehaviorErrorThrows() {
		MaxToolCallsMiddleware mw = new MaxToolCallsMiddleware( 1, "error" );
		mw.beforeAgentRun( agentCtx() );
		mw.beforeToolCall( toolCtx( "t" ) );  // call 1 — allowed

		try {
			mw.beforeToolCall( toolCtx( "t" ) );  // call 2 — over limit
			fail( "Expected MaxToolCallsExceededException" );
		} catch ( Exception e ) {
			assertTrue( e.getClass().getSimpleName().contains( "MaxToolCallsExceeded" ) );
		}
	}

	@Test
	public void testCounterResetsOnBeforeAgentRun() {
		MaxToolCallsMiddleware mw = new MaxToolCallsMiddleware( 2, "end" );

		// First run — use up both calls
		mw.beforeAgentRun( agentCtx() );
		mw.beforeToolCall( toolCtx( "t" ) );
		mw.beforeToolCall( toolCtx( "t" ) );
		AiMiddlewareResult overLimit = mw.beforeToolCall( toolCtx( "t" ) );
		assertTrue( overLimit.isCancelled() );

		// Second run — counter should reset
		mw.beforeAgentRun( agentCtx() );
		assertTrue( mw.beforeToolCall( toolCtx( "t" ) ).isContinue() );
		assertTrue( mw.beforeToolCall( toolCtx( "t" ) ).isContinue() );
	}

	@Test
	public void testDefaultLimitIsTen() {
		MaxToolCallsMiddleware mw = new MaxToolCallsMiddleware();
		mw.beforeAgentRun( agentCtx() );

		for ( int i = 0; i < 10; i++ ) {
			assertTrue( mw.beforeToolCall( toolCtx( "t" ) ).isContinue() );
		}

		AiMiddlewareResult result = mw.beforeToolCall( toolCtx( "t" ) );
		assertTrue( result.isCancelled() );
	}

	@Test
	public void testAfterAgentRunReturnsContinue() {
		MaxToolCallsMiddleware mw = new MaxToolCallsMiddleware();
		mw.beforeAgentRun( agentCtx() );
		assertTrue( mw.afterAgentRun( agentCtx() ).isContinue() );
	}
}