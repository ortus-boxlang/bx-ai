package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;

public class MaxToolCallsMiddlewareTest extends BaseIntegrationTest {
    @Test
    void testCancelsAtLimit() {
        var middleware = new MaxToolCallsMiddleware(2);
        var agent = new AiAgent().addMiddleware(middleware);
        
        agent.ask("Call calculator 3 times");
        assertEquals(2, middleware.getCallCount());
        assertTrue(agent.ask("retry").isRejected());
    }
    
    @Test
    void testResetsPerRun() {
        var middleware = new MaxToolCallsMiddleware(1);
        var agent = new AiAgent().addMiddleware(middleware);
        
        agent.ask("first");  // Uses 1 call
        agent.ask("second"); // Should reset counter
        assertEquals(1, middleware.getCallCount());
    }
}
