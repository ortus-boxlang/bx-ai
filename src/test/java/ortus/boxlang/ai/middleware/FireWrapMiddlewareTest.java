package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.AiBaseRequest;
import bx.middleware.BaseAiMiddleware;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireWrapMiddlewareTest extends BaseMiddlewareTest {

	private Map<String, Object> ctx() {
		return new HashMap<>();
	}

	@Test
	public void testSingleWrapMiddlewareCallsHandler() {
		AiBaseRequest request = new AiBaseRequest();
		request.addMiddleware( new BaseAiMiddleware() {
			@Override
			public Object wrapLLMCall( Map<String, Object> context, java.util.concurrent.Callable<Object> handler ) {
				try { return handler.call(); } catch ( Exception e ) { throw new RuntimeException( e ); }
			}
		});

		Object result = request.fireWrapMiddleware( "wrapLLMCall", ctx(), () -> "inner-result" );
		assertEquals( "inner-result", result );
	}

	@Test
	public void testOutermostMiddlewareWrapsInnermost() {
		List<String> log = new ArrayList<>();

		AiBaseRequest request = new AiBaseRequest();

		// A registered first = outermost
		request.addMiddleware( new BaseAiMiddleware() {
			@Override
			public Object wrapLLMCall( Map<String, Object> context, java.util.concurrent.Callable<Object> handler ) {
				log.add( "A-before" );
				try { Object r = handler.call(); log.add( "A-after" ); return r; }
				catch ( Exception e ) { throw new RuntimeException( e ); }
			}
		});

		request.addMiddleware( new BaseAiMiddleware() {
			@Override
			public Object wrapLLMCall( Map<String, Object> context, java.util.concurrent.Callable<Object> handler ) {
				log.add( "B-before" );
				try { Object r = handler.call(); log.add( "B-after" ); return r; }
				catch ( Exception e ) { throw new RuntimeException( e ); }
			}
		});

		request.fireWrapMiddleware( "wrapLLMCall", ctx(), () -> {
			log.add( "inner" );
			return "result";
		});

		assertEquals( List.of( "A-before", "B-before", "inner", "B-after", "A-after" ), log );
	}

	@Test
	public void testEmptyStackCallsInnerDirectly() {
		AiBaseRequest request = new AiBaseRequest();
		Object result = request.fireWrapMiddleware( "wrapLLMCall", ctx(), () -> "direct" );
		assertEquals( "direct", result );
	}

	@Test
	public void testWrapMiddlewareCanReplaceReturnValue() {
		AiBaseRequest request = new AiBaseRequest();
		request.addMiddleware( new BaseAiMiddleware() {
			@Override
			public Object wrapLLMCall( Map<String, Object> context, java.util.concurrent.Callable<Object> handler ) {
				return "replaced";   // ignores handler() return value
			}
		});

		Object result = request.fireWrapMiddleware( "wrapLLMCall", ctx(), () -> "original" );
		assertEquals( "replaced", result );
	}

	@Test
	public void testWrapToolCallIsIndependentOfWrapLLMCall() {
		List<String> log = new ArrayList<>();

		AiBaseRequest request = new AiBaseRequest();
		request.addMiddleware( new BaseAiMiddleware() {
			@Override
			public Object wrapToolCall( Map<String, Object> context, java.util.concurrent.Callable<Object> handler ) {
				log.add( "wrapToolCall" );
				try { return handler.call(); } catch ( Exception e ) { throw new RuntimeException( e ); }
			}
		});

		// wrapLLMCall should not fire wrapToolCall
		request.fireWrapMiddleware( "wrapLLMCall", ctx(), () -> "x" );
		assertFalse( log.contains( "wrapToolCall" ) );

		// wrapToolCall should fire wrapToolCall
		request.fireWrapMiddleware( "wrapToolCall", ctx(), () -> "y" );
		assertTrue( log.contains( "wrapToolCall" ) );
	}
}