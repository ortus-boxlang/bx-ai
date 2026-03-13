package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;
import java.util.Map;
import java.util.function.Function;

public class StructMiddlewareAdapterTest extends BaseIntegrationTest {
    @Test
    void testClosureDispatch() {
        var middleware = Map.<String, Function<?,?>>of(
            "onToolCall", tool -> {
                assertEquals("calculator", ((Map)tool).get("name"));
                return AiMiddlewareResult.resume();
            },
            "onStart", agent -> { /* noop */ return agent; },
            "onComplete", result -> { /* noop */ return result; }
        );
        
        var agent = new AiAgent().addMiddleware(middleware);
        var result = agent.ask("What is 2+2?");
        assertTrue(result.isComplete());
    }
}
