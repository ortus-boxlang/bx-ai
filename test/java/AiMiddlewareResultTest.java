package test.java;
import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.AiMiddlewareResult;

public class AiMiddlewareResultTest extends BaseMiddlewareTest {

    @Test
    public void testFactories() {
        assertTrue(AiMiddlewareResult.continue().isContinue());
        assertTrue(AiMiddlewareResult.approve().isApproved());
        assertTrue(AiMiddlewareResult.suspend(new struct()).isSuspended());
        assertTrue(AiMiddlewareResult.cancel("reason").isCancelled());
    }

    @Test
    public void testPredicates() {
        var result = AiMiddlewareResult.reject("bad");
        assertTrue(result.isRejected());
        assertTrue(result.isTerminal());
        assertFalse(result.isContinue());
    }
}
