package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.builtin.RetryMiddleware;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryMiddlewareTest extends BaseMiddlewareTest {

	private Map<String, Object> ctx() {
		Map<String, Object> ctx = new HashMap<>();
		ctx.put( "toolName", "testTool" );
		ctx.put( "model",    "gpt-4" );
		return ctx;
	}

	@Test
	public void testSuccessOnFirstAttemptReturnsResult() {
		RetryMiddleware mw = new RetryMiddleware( 3, 1.5, new java.util.ArrayList<>(), true, true );
		Object result = mw.wrapToolCall( ctx(), () -> "success" );
		assertEquals( "success", result );
	}

	@Test
	public void testRetriesOnExceptionAndEventuallySucceeds() {
		AtomicInteger attempts = new AtomicInteger( 0 );
		RetryMiddleware mw = new RetryMiddleware( 3, 0.01, new java.util.ArrayList<>(), true, true );

		Object result = mw.wrapToolCall( ctx(), () -> {
			if ( attempts.incrementAndGet() < 3 ) {
				throw new RuntimeException( "transient failure" );
			}
			return "recovered";
		});

		assertEquals( "recovered", result );
		assertEquals( 3, attempts.get() );
	}

	@Test
	public void testRethrowsAfterMaxRetries() {
		AtomicInteger attempts = new AtomicInteger( 0 );
		RetryMiddleware mw = new RetryMiddleware( 2, 0.01, new java.util.ArrayList<>(), true, true );

		try {
			mw.wrapToolCall( ctx(), () -> {
				attempts.incrementAndGet();
				throw new RuntimeException( "always fails" );
			});
			fail( "Expected exception after max retries" );
		} catch ( RuntimeException e ) {
			assertEquals( "always fails", e.getMessage() );
		}

		// maxRetries=2 means 3 total attempts
		assertEquals( 3, attempts.get() );
	}

	@Test
	public void testRetryOnlyMatchingExceptionTypes() {
		AtomicInteger attempts = new AtomicInteger( 0 );
		RetryMiddleware mw = new RetryMiddleware(
			3, 0.01,
			List.of( "NetworkException" ),  // only retry NetworkException
			true, true
		);

		try {
			mw.wrapToolCall( ctx(), () -> {
				attempts.incrementAndGet();
				throw new RuntimeException( "SomeOtherException: unexpected" );
			});
			fail( "Expected immediate rethrow for non-matching exception type" );
		} catch ( RuntimeException e ) {
			// Should rethrow immediately — no retries for non-matching type
			assertEquals( 1, attempts.get() );
		}
	}

	@Test
	public void testRetryLLMFalseSkipsLLMRetry() {
		AtomicInteger attempts = new AtomicInteger( 0 );
		RetryMiddleware mw = new RetryMiddleware( 3, 0.01, new java.util.ArrayList<>(), false, true );

		try {
			mw.wrapLLMCall( ctx(), () -> {
				attempts.incrementAndGet();
				throw new RuntimeException( "llm fail" );
			});
			fail( "Expected exception" );
		} catch ( RuntimeException e ) {
			// retryLLM=false — should throw immediately with no retries
			assertEquals( 1, attempts.get() );
		}
	}

	@Test
	public void testRetryToolsFalseSkipsToolRetry() {
		AtomicInteger attempts = new AtomicInteger( 0 );
		RetryMiddleware mw = new RetryMiddleware( 3, 0.01, new java.util.ArrayList<>(), true, false );

		try {
			mw.wrapToolCall( ctx(), () -> {
				attempts.incrementAndGet();
				throw new RuntimeException( "tool fail" );
			});
			fail( "Expected exception" );
		} catch ( RuntimeException e ) {
			assertEquals( 1, attempts.get() );
		}
	}

	@Test
	public void testLLMRetryRecoversOnSecondAttempt() {
		AtomicInteger attempts = new AtomicInteger( 0 );
		RetryMiddleware mw = new RetryMiddleware( 3, 0.01, new java.util.ArrayList<>(), true, true );

		Object result = mw.wrapLLMCall( ctx(), () -> {
			if ( attempts.incrementAndGet() == 1 ) throw new RuntimeException( "first fail" );
			return "llm-response";
		});

		assertEquals( "llm-response", result );
		assertEquals( 2, attempts.get() );
	}
}