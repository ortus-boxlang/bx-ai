package test.java.unit;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;

public class CheckpointTest extends BaseIntegrationTest {
    @Test
    void testCacheMemoryStateOperations() {
        var checkpoint = new Checkpoint(new CacheMemory());
        var state = Map.of("step", 1, "tools", List.of("calc"));
        
        checkpoint.saveState("thread1", state);
        var loaded = checkpoint.loadState("thread1");
        
        assertEquals(1, loaded.get("step"));
        assertEquals("calc", ((List<?>)loaded.get("tools")).get(0));
        
        checkpoint.clearState("thread1");
        assertNull(checkpoint.loadState("thread1"));
    }
}
