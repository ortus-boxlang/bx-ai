package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;
import static org.mockito.Mockito.*;

public class LoggingMiddlewareTest extends BaseIntegrationTest {
    @Test
    void testAllHooksFire() {
        var mockLogger = mock(Logger.class);
        var middleware = new LoggingMiddleware(mockLogger);
        var agent = new AiAgent().addMiddleware(middleware);
        
        agent.ask("What is 2+2?");
        
        verify(mockLogger, atLeastOnce()).info(contains("START"));
        verify(mockLogger, atLeastOnce()).debug(contains("TOOL"));
        verify(mockLogger, atLeastOnce()).info(contains("COMPLETE"));
    }
}
