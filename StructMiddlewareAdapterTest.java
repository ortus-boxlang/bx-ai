package test.java;
import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.StructMiddlewareAdapter;

public class StructMiddlewareAdapterTest extends BaseMiddlewareTest {
    
    @Test
    public void testClosureDispatch() {
        var adapter = new StructMiddlewareAdapter({
            beforeToolCall: (ctx) => AiMiddlewareResult.cancel("test")
        });
        
        var result = adapter.beforeToolCall(new struct());
        assertTrue(result.isCancelled());
    }
}
