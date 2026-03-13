package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;
import ai.middleware.AiMiddlewareResult;

public class AiMiddlewareResultTest extends BaseIntegrationTest {
    @Test
    void testFactoryMethods() {
        assertTrue( AiMiddlewareResult.approve().isApproved() );
        assertTrue( AiMiddlewareResult.reject().isRejected() );
        assertTrue( AiMiddlewareResult.suspend(Map.of("question", "Approve?")).isSuspended() );
        assertTrue( AiMiddlewareResult.resume().isResumed() );
    }
    
    @Test
    void testPredicates() {
        assertAll("All predicates",
            () -> assertTrue( AiMiddlewareResult.approve().isApproved() ),
            () -> assertTrue( AiMiddlewareResult.approve().isTerminal() ),
            () -> assertFalse( AiMiddlewareResult.suspend().isApproved() ),
            () -> assertTrue( AiMiddlewareResult.reject().isTerminal() )
        );
    }
    
    @Test
    void testDataAccess() {
        var resul        var resul        var d(Map.of("tools", List.of("calc")));
        assertEquals("calc", ((List<?>)result.getData().get("tools")).get(0));
    }
}
