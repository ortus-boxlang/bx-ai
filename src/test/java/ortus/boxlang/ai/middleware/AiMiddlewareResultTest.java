package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "AiMiddlewareResult Tests" )
public class AiMiddlewareResultTest extends BaseIntegrationTest {

	@DisplayName( "continue() result is not terminal" )
	@Test
	public void testContinueResult() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        result = AiMiddlewareResult::continue();
		        resultType       = result.getType();
		        resultIsTerminal = result.isTerminal();
		        resultIsContinue = result.isContinue();
		    """,
		    context
		);
		// @formatter:on

		IStruct res = ( IStruct ) variables.get( Key.of( "result" ) );
		assertThat( res ).isNotNull();
		assertThat( variables.getAsString( Key.of( "resultType" ) ) ).isEqualTo( "continue" );
		assertThat( variables.getAsBoolean( Key.of( "resultIsTerminal" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "resultIsContinue" ) ) ).isTrue();
	}

	@DisplayName( "cancel() result is terminal" )
	@Test
	public void testCancelResult() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        result           = AiMiddlewareResult::cancel( "too risky" );
		        resultType       = result.getType();
		        resultReason     = result.getReason();
		        resultIsTerminal = result.isTerminal();
		        resultIsCancelled= result.isCancelled();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "resultType" ) ) ).isEqualTo( "cancel" );
		assertThat( variables.getAsString( Key.of( "resultReason" ) ) ).isEqualTo( "too risky" );
		assertThat( variables.getAsBoolean( Key.of( "resultIsTerminal" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resultIsCancelled" ) ) ).isTrue();
	}

	@DisplayName( "reject() result is terminal" )
	@Test
	public void testRejectResult() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        result           = AiMiddlewareResult::reject( "not allowed" );
		        resultIsTerminal = result.isTerminal();
		        resultIsRejected = result.isRejected();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsTerminal" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resultIsRejected" ) ) ).isTrue();
	}

	@DisplayName( "suspend() result is terminal and carries data" )
	@Test
	public void testSuspendResult() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        pendingData      = { toolName: "doSomething", toolArgs: { x: 1 } };
		        result           = AiMiddlewareResult::suspend( pendingData );
		        resultIsTerminal = result.isTerminal();
		        resultIsSuspend  = result.isSuspended();
		        resultData       = result.getData();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsTerminal" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resultIsSuspend" ) ) ).isTrue();
		IStruct data = ( IStruct ) variables.get( Key.of( "resultData" ) );
		assertThat( data.getAsString( Key.of( "toolName" ) ) ).isEqualTo( "doSomething" );
	}

	@DisplayName( "edit() result carries edited arguments" )
	@Test
	public void testEditResult() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        edited    = { query: "sanitized query" };
		        result    = AiMiddlewareResult::edit( edited );
		        resultIsEdit = result.isEdit();
		        resultData   = result.getData();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "resultIsEdit" ) ) ).isTrue();
		IStruct data = ( IStruct ) variables.get( Key.of( "resultData" ) );
		assertThat( data.getAsString( Key.of( "query" ) ) ).isEqualTo( "sanitized query" );
	}

	@DisplayName( "approve() and continue() are not terminal" )
	@Test
	public void testApproveResult() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;
		        approved         = AiMiddlewareResult::approve();
		        continued        = AiMiddlewareResult::continue();
		        approveTerminal  = approved.isTerminal();
		        continueTerminal = continued.isTerminal();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "approveTerminal" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "continueTerminal" ) ) ).isFalse();
	}
}
