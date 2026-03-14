package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.builtin.HumanInTheLoopMiddleware;
import bx.middleware.AiMiddlewareResult;
import java.util.*;

public class HumanInTheLoopMiddlewareTest extends BaseMiddlewareTest {

	private Map<String, Object> toolCtx( String toolName ) {
		Map<String, Object> ctx = new HashMap<>();
		ctx.put( "toolName",   toolName );
		ctx.put( "toolArgs",   new HashMap<>() );
		ctx.put( "toolCallId", "tc-1" );
		return ctx;
	}

	@Test
	public void testAutoApproveReturnsApprove() {
		HumanInTheLoopMiddleware mw = new HumanInTheLoopMiddleware(
			new HashMap<>(),
			List.of( "readRecord", "listRecords" ),
			"suspend"
		);

		assertTrue( mw.beforeToolCall( toolCtx( "readRecord" ) ).isApproved() );
		assertTrue( mw.beforeToolCall( toolCtx( "listRecords" ) ).isApproved() );
	}

	@Test
	public void testSuspendModeReturnsSuspend() {
		HumanInTheLoopMiddleware mw = new HumanInTheLoopMiddleware(
			new HashMap<>(),   // empty = interrupt on ALL tools
			new ArrayList<>(),
			"suspend"
		);

		AiMiddlewareResult result = mw.beforeToolCall( toolCtx( "deleteRecord" ) );
		assertTrue( result.isSuspended() );
		assertTrue( result.isTerminal() );
	}

	@Test
	public void testSuspendDataContainsRequiredKeys() {
		HumanInTheLoopMiddleware mw = new HumanInTheLoopMiddleware(
			new HashMap<>(),
			new ArrayList<>(),
			"suspend"
		);

		AiMiddlewareResult result = mw.beforeToolCall( toolCtx( "deleteRecord" ) );
		Map<?, ?> data = ( Map<?, ?> ) result.getData();

		assertNotNull( data.get( "question" ) );
		assertEquals( "deleteRecord", data.get( "toolName" ) );
		assertNotNull( data.get( "toolArgs" ) );
		assertNotNull( data.get( "toolCallId" ) );
	}

	@Test
	public void testToolNotInInterruptOnContinues() {
		Map<String, Object> interruptOn = new HashMap<>();
		interruptOn.put( "deleteRecord", new HashMap<>() );

		HumanInTheLoopMiddleware mw = new HumanInTheLoopMiddleware(
			interruptOn,
			new ArrayList<>(),
			"suspend"
		);

		// readRecord is not in interruptOn — should pass through
		assertTrue( mw.beforeToolCall( toolCtx( "readRecord" ) ).isContinue() );
	}

	@Test
	public void testToolInInterruptOnSuspends() {
		Map<String, Object> interruptOn = new HashMap<>();
		interruptOn.put( "deleteRecord", new HashMap<>() );

		HumanInTheLoopMiddleware mw = new HumanInTheLoopMiddleware(
			interruptOn,
			new ArrayList<>(),
			"suspend"
		);

		assertTrue( mw.beforeToolCall( toolCtx( "deleteRecord" ) ).isSuspended() );
	}

	@Test
	public void testAutoApproveBeatsInterruptOn() {
		// Tool is in both autoApprove and interruptOn — autoApprove wins
		Map<String, Object> interruptOn = new HashMap<>();
		interruptOn.put( "readRecord", new HashMap<>() );

		HumanInTheLoopMiddleware mw = new HumanInTheLoopMiddleware(
			interruptOn,
			List.of( "readRecord" ),
			"suspend"
		);

		assertTrue( mw.beforeToolCall( toolCtx( "readRecord" ) ).isApproved() );
	}

	@Test
	public void testEmptyInterruptOnInterruptsAllTools() {
		HumanInTheLoopMiddleware mw = new HumanInTheLoopMiddleware(
			new HashMap<>(),   // empty = all tools
			new ArrayList<>(),
			"suspend"
		);

		assertTrue( mw.beforeToolCall( toolCtx( "anything" ) ).isSuspended() );
		assertTrue( mw.beforeToolCall( toolCtx( "whatever" ) ).isSuspended() );
	}
}