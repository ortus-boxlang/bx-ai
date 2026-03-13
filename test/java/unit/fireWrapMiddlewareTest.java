package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;

public class fireWrapMiddlewareTest extends BaseIntegrationTest {
    @Test
    void testNestedHandlerChain() {
        var outer = new AiMiddleware() {
            public AiMiddlewareResult onStart(AiAgent agent) {
                var innerChain = new LoggingMiddleware();
                return innerChain.fireStart(agent);
            }
        };
        
        var agent = new AiAgent().addMiddleware(outer);
        var result = agent.ask("test");
        assertTrue(result.isComplete());
    }
}
