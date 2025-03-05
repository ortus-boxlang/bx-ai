package ortus.boxlang.moduleslug.interceptors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Struct;

public class ExampleInterceptorTest {

	static BoxRuntime			runtime;
	static InterceptorService	interceptorService;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		runtime				= BoxRuntime.getInstance( true );
		interceptorService	= runtime.getInterceptorService();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Test my interceptor" )
	@Test
	public void testInterceptor() {
		// Register the interceptor with the interceptor service
		interceptorService.register(
		    new ExampleInterceptor()
		);

		// Announce the event the interceptor listens to
		interceptorService.announce(
		    Key.of( "onApplicationStart" ),
		    Struct.of( "data", "some data" )
		);

		// Assertions go here

	}

}
