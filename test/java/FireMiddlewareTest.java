package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.AiBaseRequest;
import bx.middleware.AiMiddlewareResult;
import bx.middleware.BaseAiMiddleware;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class FireMiddlewareTest extends BaseMiddlewareTest {

	// Helper — build minimal context
	private Map<String, Object> ctx() {
		return new HashMap<>();
	}

	// Helper — middleware that appends a label and returns continue
	private BaseAiMiddleware trackingMiddleware( List<String> log, String label ) {
		return new BaseAiMiddleware() {
			@Override
			public AiMiddlewareResult beforeToolCall( Map<String, Object> context ) {
				log.add( label );
				return AiMiddlewareResult.continueResult();
			}
			@Override
			public AiMiddlewareResult afterToolCall( Map<String, Object> context ) {
				log.add( label + "-after" );
				return AiMiddlewareResult.continueResult();
			}
		};
	}

	@Test
	public void testForwardOrderOnBeforeHook() {
		List<String> log = new ArrayList<>();
		AiBaseRequest request = new AiBaseRequest();
		request.addMiddleware( trackingMiddleware( log, "A" ) );
		request.addMiddleware( trackingMiddleware( log, "B" ) );
		request.addMiddleware( trackingMiddleware( log, "C" ) );

		request.fireMiddleware( "beforeToolCall", ctx() );

		assertEquals( List.of( "A", "B", "C" ), log );
	}

	@Test
	public void testReverseOrderOnAfterHook() {
		List<String> log = new ArrayList<>();
		AiBaseRequest request = new AiBaseRequest();
		request.addMiddleware( trackingMiddleware( log, "A" ) );
		request.addMiddleware( trackingMiddleware( log, "B" ) );
		request.addMiddleware( trackingMiddleware( log, "C" ) );

		request.fireMiddleware( "afterToolCall", ctx(), true );

		assertEquals( List.of( "C-after", "B-after", "A-after" ), log );
	}

	@Test
	public void testFirstTerminalResultStopsChain() {
		List<String> log = new ArrayList<>();

		BaseAiMiddleware rejecter = new BaseAiMiddleware() {
			@Override
			public AiMiddlewareResult beforeToolCall( Map<String, Object> context ) {
				log.add( "rejecter" );
				return AiMiddlewareResult.reject( "blocked" );
			}
		};

		BaseAiMiddleware shouldNotFire = new BaseAiMiddleware() {
			@Override
			public AiMiddlewareResult beforeToolCall( Map<String, Object> context ) {
				log.add( "shouldNotFire" );
				return AiMiddlewareResult.continueResult();
			}
		};

		AiBaseRequest request = new AiBaseRequest();
		request.addMiddleware( trackingMiddleware( log, "A" ) );
		request.addMiddleware( rejecter );
		request.addMiddleware( shouldNotFire );

		AiMiddlewareResult result = request.fireMiddleware( "beforeToolCall", ctx() );

		assertEquals( List.of( "A", "rejecter" ), log );
		assertTrue( result.isRejected() );
		assertFalse( log.contains( "shouldNotFire" ) );
	}

	@Test
	public void testAllContinueReturnsContinue() {
		AiBaseRequest request = new AiBaseRequest();
		request.addMiddleware( trackingMiddleware( new ArrayList<>(), "A" ) );
		request.addMiddleware( trackingMiddleware( new ArrayList<>(), "B" ) );

		AiMiddlewareResult result = request.fireMiddleware( "beforeToolCall", ctx() );

		assertTrue( result.isContinue() );
	}

	@Test
	public void testEmptyStackReturnsContinue() {
		AiBaseRequest request = new AiBaseRequest();
		AiMiddlewareResult result = request.fireMiddleware( "beforeToolCall", ctx() );
		assertTrue( result.isContinue() );
	}

	@Test
	public void testErrorInMiddlewareFiresOnErrorAndRethrows() {
		List<String> log = new ArrayList<>();

		BaseAiMiddleware thrower = new BaseAiMiddleware() {
			@Override
			public AiMiddlewareResult beforeToolCall( Map<String, Object> context ) {
				throw new RuntimeException( "middleware exploded" );
			}
			@Override
			public AiMiddlewareResult onError( Map<String, Object> context ) {
				log.add( "onError" );
				return AiMiddlewareResult.continueResult();
			}
		};

		AiBaseRequest request = new AiBaseRequest();
		request.addMiddleware( thrower );

		try {
			request.fireMiddleware( "beforeToolCall", ctx() );
			fail( "Expected exception to be rethrown" );
		} catch ( RuntimeException e ) {
			assertEquals( "middleware exploded", e.getMessage() );
		}

		assertTrue( log.contains( "onError" ) );
	}

	@Test
	public void testCancelIsTerminalAndStopsChain() {
		List<String> log = new ArrayList<>();

		BaseAiMiddleware canceller = new BaseAiMiddleware() {
			@Override
			public AiMiddlewareResult beforeToolCall( Map<String, Object> context ) {
				return AiMiddlewareResult.cancel( "stop" );
			}
		};

		BaseAiMiddleware shouldNotFire = new BaseAiMiddleware() {
			@Override
			public AiMiddlewareResult beforeToolCall( Map<String, Object> context ) {
				log.add( "shouldNotFire" );
				return AiMiddlewareResult.continueResult();
			}
		};

		AiBaseRequest request = new AiBaseRequest();
		request.addMiddleware( canceller );
		request.addMiddleware( shouldNotFire );

		AiMiddlewareResult result = request.fireMiddleware( "beforeToolCall", ctx() );

		assertTrue( result.isCancelled() );
		assertFalse( log.contains( "shouldNotFire" ) );
	}
}