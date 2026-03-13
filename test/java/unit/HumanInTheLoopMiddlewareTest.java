package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;
import ai.middleware.AiMiddlewareResult;

public class HumanInTheLoopMiddlewareTest extends BaseIntegrationTest {
    @Test
    void testAutoApproveMode() {
        var middleware = new HumanInTheLoopMiddleware("auto");
        var agent = new AiAgent().addMiddleware(middleware);
        var result = agent.ask("send email");
        assertTrue(result.isComplete());
    }
    
    @Test
    void testSuspendMode() {
        var middleware = new HumanInTheLoopMiddleware("suspend");
        var agent = new AiAgent().addMiddleware(middleware);
        var result = agent.ask("database query");
        assertTrue(result.isSuspended());
    }
    
    @Test
    void testRejectMode() {
        var middleware = new HumanInTheLoopMiddleware("reject");
        var agent = new AiAgent().addMiddleware(middleware);
        var result = agent.ask("exec shell");
        assertTrue(result.isRejected());
    }
    
    @Test
    void testResumeAfterSuspend() {
        var middleware = new HumanInTheLoopMiddleware("suspend");
        var agent = new AiAgent().addMiddleware(middleware);
        
        var suspendResult = agent.ask("test");
        var finalResult = agent.resume(AiMiddlewareResult.approve(), suspendResult.getThreadId());
        assertTrue(finalResult.isComplete());
    }
}
