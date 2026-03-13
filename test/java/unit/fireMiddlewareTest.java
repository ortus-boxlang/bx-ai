package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;

public class fireMiddlewareTest extends BaseIntegrationTest {
    @Test
    void testChainOrderFirstTerminalWins() {
        var agent = new AiAgent()
            .addMiddleware( middlewareThatRejectsAll() )
            .addMiddleware( new LoggingMiddleware() );
            
        var result = agent.ask("test");
        assertTrue(result.isRejected()); // First middleware wins
    }
    
    @Test
    void testReverseOrder() {
        var agent = new AiAgent()
            .addMiddleware( new LoggingMiddleware() )
            .addMiddleware( middlewareThatSuspends() );
        var result = agent.ask("test");
        assertTrue(result.isSuspended());
    }
    
    @Test
    void testErrorPropagation() {
        var agent = new AiAgent()
            .addMiddleware( middlewareThatThrows() );
        assertThrows(RuntimeException.class, () -> agent.ask("test"));
    }
    
    private AiMiddleware middlewareThatRejectsAll() {
        return new AiMiddleware() {
            public AiMiddlewareResult onStart(AiAgent agent) {
                return AiMiddlewareResult.reject();
            }
        };
    }
}
