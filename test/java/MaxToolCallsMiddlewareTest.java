package test.java;
import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.builtin.MaxToolCallsMiddleware;
import bx.middleware.AiMiddlewareResult;

public class MaxToolCallsMiddlewareTest extends BaseMiddlewareTest {
    @Test
    public void testLimitReached() {
        var middleware = new MaxToolCallsMiddleware({ maxCalls: 1 });
        middleware.beforeToolCall(new struct()); // 1st call
        var result = middleware.beforeToolCall(new struct()); // 2nd call
        assertTrue(result.isCancelled());
    }
}
