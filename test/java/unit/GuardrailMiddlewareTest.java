package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;
import java.util.List;
import java.util.Map;

public class GuardrailMiddlewareTest extends BaseIntegrationTest {
    @Test
    void testBlocksListedTools() {
        var middleware = new GuardrailMiddleware(Map.of(
            "blockTools", List.of("delete_file", "exec_shell")
        ));
        var agent = new AiAgent().addMiddleware(middleware);
        
        var result = agent.ask("delete all files");
        assertTrue(result.isRejected());
    }
    
    @Test
    void testBlocksPatterns() {
        var middleware = new GuardrailMiddleware(Map.of(
            "blockPatterns", List.of("DELETE.*FROM", "rm -rf")
        ));
        var agent = new AiAgent().addMiddleware(middleware);
        
        var result = agent.ask("DELETE FROM users");
        assertTrue(result.isRejected());
    }
}
