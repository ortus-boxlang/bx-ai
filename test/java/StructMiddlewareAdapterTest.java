package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.StructMiddlewareAdapter;
import bx.middleware.AiMiddlewareResult;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class StructMiddlewareAdapterTest extends BaseMiddlewareTest {

	// Helper — build a context struct
	private Map<String, Object> ctx() {
		Map<String, Object> context = new HashMap<>();
		context.put( "toolName",   "testTool" );
		context.put( "toolArgs",   new HashMap<>() );
		context.put( "toolCallId", "tc-1" );
		return context;
	}

	@Test
	public void testPresentHookIsCalled() {
		AtomicBoolean fired = new AtomicBoolean( false );

		Map<String, Object> struct = new HashMap<>();
		struct.put( "beforeToolCall", ( java.util.function.Function<Map<String,Object>, AiMiddlewareResult> )
			context -> {
				fired.set( true );
				return AiMiddlewareResult.continueResult();
			}
		);

		StructMiddlewareAdapter adapter = new StructMiddlewareAdapter( struct );
		AiMiddlewareResult result = adapter.beforeToolCall( ctx() );

		assertTrue( fired.get() );
		assertTrue( result.isContinue() );
	}

	@Test
	public void testMissingHookDefaultsToContinue() {
		// Empty struct — no hooks defined
		StructMiddlewareAdapter adapter = new StructMiddlewareAdapter( new HashMap<>() );

		assertTrue( adapter.beforeAgentRun( ctx() ).isContinue() );
		assertTrue( adapter.afterAgentRun( ctx() ).isContinue() );
		assertTrue( adapter.beforeLLMCall( ctx() ).isContinue() );
		assertTrue( adapter.afterLLMCall( ctx() ).isContinue() );
		assertTrue( adapter.beforeToolCall( ctx() ).isContinue() );
		assertTrue( adapter.afterToolCall( ctx() ).isContinue() );
		assertTrue( adapter.onError( ctx() ).isContinue() );
	}

	@Test
	public void testMissingWrapHookCallsHandler() {
		StructMiddlewareAdapter adapter = new StructMiddlewareAdapter( new HashMap<>() );
		AtomicBoolean handlerCalled = new AtomicBoolean( false );

		Object result = adapter.wrapLLMCall( ctx(), () -> {
			handlerCalled.set( true );
			return "llm-response";
		});

		assertTrue( handlerCalled.get() );
		assertEquals( "llm-response", result );
	}

	@Test
	public void testPresentHookCanReturnTerminalResult() {
		Map<String, Object> struct = new HashMap<>();
		struct.put( "beforeToolCall", ( java.util.function.Function<Map<String,Object>, AiMiddlewareResult> )
			context -> AiMiddlewareResult.reject( "blocked" )
		);

		StructMiddlewareAdapter adapter = new StructMiddlewareAdapter( struct );
		AiMiddlewareResult result = adapter.beforeToolCall( ctx() );

		assertTrue( result.isRejected() );
		assertTrue( result.isTerminal() );
		assertEquals( "blocked", result.getReason() );
	}

	@Test
	public void testContextIsPassedThroughToHook() {
		AtomicBoolean contextReceived = new AtomicBoolean( false );

		Map<String, Object> struct = new HashMap<>();
		struct.put( "beforeToolCall", ( java.util.function.Function<Map<String,Object>, AiMiddlewareResult> )
			context -> {
				contextReceived.set( "testTool".equals( context.get( "toolName" ) ) );
				return AiMiddlewareResult.continueResult();
			}
		);

		StructMiddlewareAdapter adapter = new StructMiddlewareAdapter( struct );
		adapter.beforeToolCall( ctx() );

		assertTrue( contextReceived.get() );
	}

	@Test
	public void testWrapHookIsCalledWhenPresent() {
		AtomicBoolean wrapFired   = new AtomicBoolean( false );
		AtomicBoolean handlerFired = new AtomicBoolean( false );

		Map<String, Object> struct = new HashMap<>();
		struct.put( "wrapToolCall", ( java.util.function.BiFunction<Map<String,Object>, java.util.concurrent.Callable<Object>, Object> )
			( context, handler ) -> {
				wrapFired.set( true );
				try { return handler.call(); } catch ( Exception e ) { throw new RuntimeException( e ); }
			}
		);

		StructMiddlewareAdapter adapter = new StructMiddlewareAdapter( struct );
		adapter.wrapToolCall( ctx(), () -> {
			handlerFired.set( true );
			return "tool-result";
		});

		assertTrue( wrapFired.get() );
		assertTrue( handlerFired.get() );
	}
}