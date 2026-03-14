package test.java.base;

import org.junit.jupiter.api.BeforeAll;
import testbox.TestBox;

public abstract class BaseIntegrationTest extends TestBox {

	@BeforeAll
	static void setupModule() {
		loadModule( "AiMiddleware" );
	}

}
