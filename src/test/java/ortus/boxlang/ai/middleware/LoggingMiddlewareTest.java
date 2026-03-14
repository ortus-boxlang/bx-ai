package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.builtin.LoggingMiddleware;
import bx.middleware.AiMiddlewareResult;
import java.util.HashMap;
import java.util.Map;

public class LoggingMiddlewareTest extends BaseMiddlewareTest {

	private Map<String, Object> ctx() {
		Map<String, Object> context = new HashMap<>();
		context.put( "toolName",   "testTool" );
		context.put( "toolArgs",   new HashMap<>() );
		context.put( "toolCallId", "tc-1" );
		context.put( "input",      "test input" );
		context.put( "messages",   new java.util.ArrayList<>() );
		context.put( "model",      "gpt-4" );
		return context;
	}

	@Test
	public void testAllHooksReturnContinue() {
		LoggingMiddleware mw = new LoggingMiddleware();

		assertTrue( mw.beforeAgentRun( ctx() ).isContinue() );
		assertTrue( mw.afterAgentRun( ctx() ).isContinue() );
		assertTrue( mw.beforeLLMCall( ctx() ).isContinue() );
		assertTrue( mw.afterLLMCall( ctx() ).isContinue() );
		assertTrue( mw.beforeToolCall( ctx() ).isContinue() );
		assertTrue( mw.afterToolCall( ctx() ).isContinue() );
		assertTrue( mw.onError( ctx() ).isContinue() );
	}

	@Test
	public void testNeverReturnsTerminalResult() {
		LoggingMiddleware mw = new LoggingMiddleware();

		assertFalse( mw.beforeAgentRun( ctx() ).isTerminal() );
		assertFalse( mw.beforeLLMCall( ctx() ).isTerminal() );
		assertFalse( mw.beforeToolCall( ctx() ).isTerminal() );
		assertFalse( mw.afterToolCall( ctx() ).isTerminal() );
		assertFalse( mw.afterAgentRun( ctx() ).isTerminal() );
	}

	@Test
	public void testWrapLLMCallInvokesHandler() {
		LoggingMiddleware mw = new LoggingMiddleware();
		Object result = mw.wrapLLMCall( ctx(), () -> "llm-response" );
		assertEquals( "llm-response", result );
	}

	@Test
	public void testWrapToolCallInvokesHandler() {
		LoggingMiddleware mw = new LoggingMiddleware();
		Object result = mw.wrapToolCall( ctx(), () -> "tool-response" );
		assertEquals( "tool-response", result );
	}

	@Test
	public void testWrapLLMCallRethrowsException() {
		LoggingMiddleware mw = new LoggingMiddleware();
		try {
			mw.wrapLLMCall( ctx(), () -> { throw new RuntimeException( "provider down" ); } );
			fail( "Expected exception to propagate" );
		} catch ( RuntimeException e ) {
			assertEquals( "provider down", e.getMessage() );
		}
	}

	@Test
	public void testLogTokensFalseByDefault() {
		// logTokens=false by default — afterLLMCall still returns continue
		// even when response has no usage key
		LoggingMiddleware mw = new LoggingMiddleware();
		assertTrue( mw.afterLLMCall( ctx() ).isContinue() );
	}

	@Test
	public void testLogArgsFalseDoesNotThrow() {
		LoggingMiddleware mw = new LoggingMiddleware(
			"ai", "information", false, false, false
		);
		// Should not throw even with no toolArgs in context
		Map<String, Object> bare = new HashMap<>();
		bare.put( "toolName", "t" );
		assertTrue( mw.beforeToolCall( bare ).isContinue() );
	}
}