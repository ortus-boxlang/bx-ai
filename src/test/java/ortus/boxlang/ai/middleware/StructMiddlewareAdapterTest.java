package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "StructMiddlewareAdapter Tests" )
public class StructMiddlewareAdapterTest extends BaseIntegrationTest {

	@DisplayName( "Defined hook is called and returns its result" )
	@Test
	public void testDefinedHookIsCalled() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.StructMiddlewareAdapter;
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        hookWasCalled = false;

		        adapter = new StructMiddlewareAdapter( {
		            beforeAgentRun: function( required struct context ) {
		                hookWasCalled = true;
		                return AiMiddlewareResult::cancel( "stopped by struct mw" );
		            }
		        } );

		        result = adapter.beforeAgentRun( { context: { input: "hi" } } );
		        resultIsCancelled = result.isCancelled();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hookWasCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resultIsCancelled" ) ) ).isTrue();
	}

	@DisplayName( "Missing hook falls back to continue()" )
	@Test
	public void testMissingHookReturnsContinue() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.StructMiddlewareAdapter;

		        // Struct has no beforeLLMCall key
		        adapter = new StructMiddlewareAdapter( { afterAgentRun: function( ctx ) { return; } } );

		        result           = adapter.beforeLLMCall( { context: {} } );
		        resultIsContinue = result.isContinue();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsContinue" ) ) ).isTrue();
	}

	@DisplayName( "Wrap hook delegates to handler when not defined" )
	@Test
	public void testMissingWrapHookCallsHandler() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.StructMiddlewareAdapter;

		        handlerCalled = false;

		        adapter = new StructMiddlewareAdapter( {} );

		        wrapResult = adapter.wrapLLMCall(
		            {},
		            function() {
		                handlerCalled = true;
		                return "handled";
		            }
		        );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "handlerCalled" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "wrapResult" ) ) ).isEqualTo( "handled" );
	}
}
