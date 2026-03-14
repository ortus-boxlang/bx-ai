package test.java.integration;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MiddlewarePipelineIntegrationTest extends BaseIntegrationTest {

	@Test
	void testLoggingMiddlewareFiresOnAllHooks() {
		// Track which hooks fired via struct-of-closures middleware
		List<String> hookLog = new ArrayList<>();

		Object tracker = createStruct( Map.of(
			"beforeAgentRun", closure( ctx -> { hookLog.add( "beforeAgentRun" ); return invoke( "AiMiddlewareResult", "continueResult" ); } ),
			"beforeLLMCall",  closure( ctx -> { hookLog.add( "beforeLLMCall"  ); return invoke( "AiMiddlewareResult", "continueResult" ); } ),
			"afterLLMCall",   closure( ctx -> { hookLog.add( "afterLLMCall"   ); return invoke( "AiMiddlewareResult", "continueResult" ); } ),
			"afterAgentRun",  closure( ctx -> { hookLog.add( "afterAgentRun"  ); return invoke( "AiMiddlewareResult", "continueResult" ); } )
		));

		Object agent = bxExecute( """
            return aiAgent(
                name       : "TestAgent",
                middleware : [ new LoggingMiddleware( logToConsole: false ) ]
            );
        """ );

		bxInvoke( agent, "withMiddleware", tracker );
		bxInvoke( agent, "run", "What is 2 + 2?" );

		assertTrue( hookLog.contains( "beforeAgentRun" ), "beforeAgentRun did not fire" );
		assertTrue( hookLog.contains( "beforeLLMCall" ),  "beforeLLMCall did not fire" );
		assertTrue( hookLog.contains( "afterLLMCall" ),   "afterLLMCall did not fire" );
		assertTrue( hookLog.contains( "afterAgentRun" ),  "afterAgentRun did not fire" );
	}

	@Test
	void testHookOrderIsCorrect() {
		List<String> hookLog = new ArrayList<>();

		Object tracker = createStruct( Map.of(
			"beforeAgentRun", closure( ctx -> { hookLog.add( "beforeAgentRun" ); return invoke( "AiMiddlewareResult", "continueResult" ); } ),
			"beforeLLMCall",  closure( ctx -> { hookLog.add( "beforeLLMCall"  ); return invoke( "AiMiddlewareResult", "continueResult" ); } ),
			"afterLLMCall",   closure( ctx -> { hookLog.add( "afterLLMCall"   ); return invoke( "AiMiddlewareResult", "continueResult" ); } ),
			"afterAgentRun",  closure( ctx -> { hookLog.add( "afterAgentRun"  ); return invoke( "AiMiddlewareResult", "continueResult" ); } )
		));

		Object agent = bxExecute( "return aiAgent( name: 'OrderTest' );" );
		bxInvoke( agent, "withMiddleware", tracker );
		bxInvoke( agent, "run", "Hello" );

		// beforeAgentRun must precede beforeLLMCall
		assertTrue( hookLog.indexOf( "beforeAgentRun" ) < hookLog.indexOf( "beforeLLMCall" ),
			"beforeAgentRun must fire before beforeLLMCall" );

		// afterLLMCall must precede afterAgentRun
		assertTrue( hookLog.indexOf( "afterLLMCall" ) < hookLog.indexOf( "afterAgentRun" ),
			"afterLLMCall must fire before afterAgentRun" );
	}

	@Test
	void testAfterHooksFiredInReverseOrder() {
		List<String> hookLog = new ArrayList<>();

		Object mwA = createStruct( Map.of(
			"afterAgentRun", closure( ctx -> { hookLog.add( "A-after" ); return invoke( "AiMiddlewareResult", "continueResult" ); } )
		));
		Object mwB = createStruct( Map.of(
			"afterAgentRun", closure( ctx -> { hookLog.add( "B-after" ); return invoke( "AiMiddlewareResult", "continueResult" ); } )
		));
		Object mwC = createStruct( Map.of(
			"afterAgentRun", closure( ctx -> { hookLog.add( "C-after" ); return invoke( "AiMiddlewareResult", "continueResult" ); } )
		));

		Object agent = bxExecute( "return aiAgent( name: 'ReverseTest' );" );
		bxInvoke( agent, "withMiddleware", mwA );
		bxInvoke( agent, "withMiddleware", mwB );
		bxInvoke( agent, "withMiddleware", mwC );
		bxInvoke( agent, "run", "Hello" );

		// Registered A, B, C — after hooks fire C, B, A
		int idxA = hookLog.indexOf( "A-after" );
		int idxB = hookLog.indexOf( "B-after" );
		int idxC = hookLog.indexOf( "C-after" );

		assertTrue( idxC < idxB, "C-after should fire before B-after" );
		assertTrue( idxB < idxA, "B-after should fire before A-after" );
	}

	@Test
	void testTerminalResultFromBeforeAgentRunShortCircuits() {
		AtomicBoolean llmCalled = new AtomicBoolean( false );

		Object canceller = createStruct( Map.of(
			"beforeAgentRun", closure( ctx -> invoke( "AiMiddlewareResult", "cancel", "blocked in test" ) ),
			"beforeLLMCall",  closure( ctx -> { llmCalled.set( true ); return invoke( "AiMiddlewareResult", "continueResult" ); } )
		));

		Object agent = bxExecute( "return aiAgent( name: 'CancelTest' );" );
		bxInvoke( agent, "withMiddleware", canceller );

		Object result = bxInvoke( agent, "run", "Should not reach LLM" );

		assertFalse( llmCalled.get(), "LLM should not have been called after cancel" );
		assertTrue( bxInvoke( result, "isCancelled" ).equals( true ), "Result should be cancelled" );
	}
}
