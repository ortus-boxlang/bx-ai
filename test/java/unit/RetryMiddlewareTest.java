package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;

public class RetryMiddlewareTest extends BaseIntegrationTest {
    @Test
    void testRetriesOnException() {
        var flakyTool = new FlakyTool(50); // 50% fail rate
        var middleware = new RetryMiddleware(Map.of(
            "maxRetries", 3, "backoff", 0
        ));
        
        var agent = new AiAgent()
            .addTool(flakyTool)
            .addMiddleware(middleware);
            
        var result = agent.ask("calculate 2+2");
        assertTrue(result.isComplete());
        assertTrue(middleware.getRetryCount() <= 3);
    }
}
