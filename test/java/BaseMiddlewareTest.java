package test.java;
import org.junit.Before;
import bx.module.BxModuleLoader;

public abstract class BaseMiddlewareTest {
    @Before
    public void setUp() throws Exception {
        BxModuleLoader.loadModule("bx-ai");
    }
}
