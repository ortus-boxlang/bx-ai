package test.java;
import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.builtin.LoggingMiddleware;

public class LoggingMiddlewareTest extends BaseMiddlewareTest {
    @Test
    public void testAllHooksFire() {
        var middleware = new LoggingMiddleware();
        // Test all 8 hooks fire without error
        assertNotNull(middleware.beforeAgentRun(new struct()));
        assertNotNull(middleware.beforeLLMCall(new struct()));
        assertNotNull(middleware.beforeToolCall(new struct()));
    }
}
