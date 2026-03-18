package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

@DisplayName( "Middleware Pipeline / AiBaseRequest Tests" )
public class MiddlewarePipelineTest extends BaseIntegrationTest {

	@DisplayName( "fireMiddleware returns first terminal result and stops chain" )
	@Test
	public void testChainStopsOnFirstTerminal() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        import bxModules.bxai.models.middleware.StructMiddlewareAdapter;
		        import bxModules.bxai.models.requests.AiChatRequest;

		        invocationOrder = [];

		        mw1 = new StructMiddlewareAdapter( {
		            beforeLLMCall: function( ctx ) {
		                invocationOrder.append( "mw1" );
		                return AiMiddlewareResult::cancel( "mw1 cancelled" );
		            }
		        } );

		        mw2 = new StructMiddlewareAdapter( {
		            beforeLLMCall: function( ctx ) {
		                invocationOrder.append( "mw2" );
		                return AiMiddlewareResult::continue();
		            }
		        } );

		        req = new AiChatRequest()
		        req.addMiddleware( mw1 );
		        req.addMiddleware( mw2 );

		        result           = req.fireMiddleware( "beforeLLMCall", { input: "test" } );
		        resultIsCancelled= result.isCancelled();
		        invocationCount  = invocationOrder.len();
		    """,
		    context
		);
		// @formatter:on

		// Only mw1 should have been invoked because it was terminal
		assertThat( variables.getAsBoolean( Key.of( "resultIsCancelled" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "invocationCount" ) ) ).isEqualTo( 1 );
		Array order = ( Array ) variables.get( Key.of( "invocationOrder" ) );
		assertThat( order.get( 0 ).toString() ).isEqualTo( "mw1" );
	}

	@DisplayName( "fireMiddleware with reverse=true calls middleware in reverse order" )
	@Test
	public void testReverseOrder() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        import bxModules.bxai.models.middleware.StructMiddlewareAdapter;
		        import bxModules.bxai.models.requests.AiChatRequest;

		        invocationOrder = [];

		        mw1 = new StructMiddlewareAdapter( {
		            afterLLMCall: function( ctx ) {
		                invocationOrder.append( "mw1" );
		                return AiMiddlewareResult::continue();
		            }
		        } );

		        mw2 = new StructMiddlewareAdapter( {
		            afterLLMCall: function( ctx ) {
		                invocationOrder.append( "mw2" );
		                return AiMiddlewareResult::continue();
		            }
		        } );

		        req = new AiChatRequest()
		        req.addMiddleware( mw1 );
		        req.addMiddleware( mw2 );

		        // after* hooks fire in reverse so mw2 runs first
		        req.fireMiddleware( "afterLLMCall", { output: "hi" }, true );
		        firstCalled = invocationOrder[1];
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "firstCalled" ) ) ).isEqualTo( "mw2" );
	}

	@DisplayName( "addMiddleware auto-wraps plain struct" )
	@Test
	public void testAddMiddlewareWrapsStruct() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.requests.AiChatRequest;
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        hookFired = false;

		        req = new AiChatRequest()
		        req.addMiddleware( {
		            beforeAgentRun: function( ctx ) {
		                hookFired = true;
		                return AiMiddlewareResult::continue();
		            }
		        } );

		        req.fireMiddleware( "beforeAgentRun", {} );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hookFired" ) ) ).isTrue();
	}

	@DisplayName( "fireWrapMiddleware chains wraps and calls inner function" )
	@Test
	public void testWrapChain() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.requests.AiChatRequest;
		        import bxModules.bxai.models.middleware.StructMiddlewareAdapter;

		        wrapOrder = [];

		        mw1 = new StructMiddlewareAdapter( {
		            wrapLLMCall: function( ctx, handler ) {
		                wrapOrder.append( "before-mw1" );
		                var r = handler();
		                wrapOrder.append( "after-mw1" );
		                return r;
		            }
		        } );

		        mw2 = new StructMiddlewareAdapter( {
		            wrapLLMCall: function( ctx, handler ) {
		                wrapOrder.append( "before-mw2" );
		                var r = handler();
		                wrapOrder.append( "after-mw2" );
		                return r;
		            }
		        } );

		        innerCalled = false;
		        req = new AiChatRequest()
		        req.addMiddleware( mw1 );
		        req.addMiddleware( mw2 );

		        wrapResult = req.fireWrapMiddleware(
		            "wrapLLMCall",
		            {},
		            function() {
		                innerCalled = true;
		                wrapOrder.append( "inner" );
		                return "inner-result";
		            }
		        );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "innerCalled" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "wrapResult" ) ) ).isEqualTo( "inner-result" );

		// Outermost wrapper (mw1) should fire first, innermost (mw2) second
		Array order = ( Array ) variables.get( Key.of( "wrapOrder" ) );
		assertThat( order.get( 0 ).toString() ).isEqualTo( "before-mw1" );
		assertThat( order.get( 1 ).toString() ).isEqualTo( "before-mw2" );
		assertThat( order.get( 2 ).toString() ).isEqualTo( "inner" );
		assertThat( order.get( 3 ).toString() ).isEqualTo( "after-mw2" );
		assertThat( order.get( 4 ).toString() ).isEqualTo( "after-mw1" );
	}
}
