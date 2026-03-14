package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.AiMiddlewareResult;
import java.util.HashMap;
import java.util.Map;

public class AiMiddlewareResultTest extends BaseMiddlewareTest {

	// -------------------------------------------------------------------------
	// Factory methods
	// -------------------------------------------------------------------------

	@Test
	public void testContinueFactory() {
		// "continue" is a reserved Java keyword — BoxLang exposes it as continueResult()
		AiMiddlewareResult result = AiMiddlewareResult.continueResult();
		assertTrue( result.isContinue() );
	}

	@Test
	public void testApproveFactory() {
		AiMiddlewareResult result = AiMiddlewareResult.approve();
		assertTrue( result.isApproved() );
	}

	@Test
	public void testCancelFactory() {
		AiMiddlewareResult result = AiMiddlewareResult.cancel( "too many calls" );
		assertTrue( result.isCancelled() );
		assertEquals( "too many calls", result.getReason() );
	}

	@Test
	public void testRejectFactory() {
		AiMiddlewareResult result = AiMiddlewareResult.reject( "not allowed" );
		assertTrue( result.isRejected() );
		assertEquals( "not allowed", result.getReason() );
	}

	@Test
	public void testRejectFactoryEmptyReason() {
		AiMiddlewareResult result = AiMiddlewareResult.reject( "" );
		assertTrue( result.isRejected() );
		assertEquals( "", result.getReason() );
	}

	@Test
	public void testSuspendFactory() {
		Map<String, Object> pending = new HashMap<>();
		pending.put( "question",   "Approve deletion?" );
		pending.put( "toolName",   "deleteRecord" );
		pending.put( "toolArgs",   new HashMap<>() );
		pending.put( "toolCallId", "tc-001" );

		AiMiddlewareResult result = AiMiddlewareResult.suspend( pending );
		assertTrue( result.isSuspended() );
		assertNotNull( result.getData() );
	}

	@Test
	public void testEditFactory() {
		Map<String, Object> args = new HashMap<>();
		args.put( "id",      42 );
		args.put( "confirm", true );

		AiMiddlewareResult result = AiMiddlewareResult.edit( args );
		assertTrue( result.isEdit() );
		assertNotNull( result.getData() );
		assertEquals( args, result.getData() );
	}

	// -------------------------------------------------------------------------
	// Predicates — mutual exclusion
	// -------------------------------------------------------------------------

	@Test
	public void testOnlyContinueIsContinue() {
		assertTrue(  AiMiddlewareResult.continueResult().isContinue() );
		assertFalse( AiMiddlewareResult.approve().isContinue() );
		assertFalse( AiMiddlewareResult.cancel( "x" ).isContinue() );
		assertFalse( AiMiddlewareResult.reject( "x" ).isContinue() );
	}

	@Test
	public void testOnlyApproveIsApproved() {
		assertTrue(  AiMiddlewareResult.approve().isApproved() );
		assertFalse( AiMiddlewareResult.continueResult().isApproved() );
		assertFalse( AiMiddlewareResult.cancel( "x" ).isApproved() );
		assertFalse( AiMiddlewareResult.reject( "x" ).isApproved() );
	}

	@Test
	public void testOnlyCancelIsCancelled() {
		assertTrue(  AiMiddlewareResult.cancel( "x" ).isCancelled() );
		assertFalse( AiMiddlewareResult.continueResult().isCancelled() );
		assertFalse( AiMiddlewareResult.approve().isCancelled() );
		assertFalse( AiMiddlewareResult.reject( "x" ).isCancelled() );
	}

	@Test
	public void testOnlyRejectIsRejected() {
		assertTrue(  AiMiddlewareResult.reject( "x" ).isRejected() );
		assertFalse( AiMiddlewareResult.continueResult().isRejected() );
		assertFalse( AiMiddlewareResult.approve().isRejected() );
		assertFalse( AiMiddlewareResult.cancel( "x" ).isRejected() );
	}

	// -------------------------------------------------------------------------
	// isTerminal()
	// -------------------------------------------------------------------------

	@Test
	public void testTerminalResults() {
		assertTrue( AiMiddlewareResult.cancel( "x" ).isTerminal() );
		assertTrue( AiMiddlewareResult.reject( "x" ).isTerminal() );

		Map<String, Object> pending = new HashMap<>();
		pending.put( "question",   "ok?" );
		pending.put( "toolName",   "t" );
		pending.put( "toolArgs",   new HashMap<>() );
		pending.put( "toolCallId", "1" );
		assertTrue( AiMiddlewareResult.suspend( pending ).isTerminal() );
	}

	@Test
	public void testNonTerminalResults() {
		Map<String, Object> args = new HashMap<>();
		args.put( "id", 1 );

		assertFalse( AiMiddlewareResult.continueResult().isTerminal() );
		assertFalse( AiMiddlewareResult.approve().isTerminal() );
		assertFalse( AiMiddlewareResult.edit( args ).isTerminal() );
	}

	// -------------------------------------------------------------------------
	// getData() and getReason()
	// -------------------------------------------------------------------------

	@Test
	public void testGetReasonOnCancel() {
		assertEquals( "limit reached", AiMiddlewareResult.cancel( "limit reached" ).getReason() );
	}

	@Test
	public void testGetReasonOnReject() {
		assertEquals( "not authorised", AiMiddlewareResult.reject( "not authorised" ).getReason() );
	}

	@Test
	public void testGetDataOnEdit() {
		Map<String, Object> args = new HashMap<>();
		args.put( "key", "value" );
		AiMiddlewareResult result = AiMiddlewareResult.edit( args );
		assertEquals( "value", ( ( Map<?, ?> ) result.getData() ).get( "key" ) );
	}

	@Test
	public void testGetDataOnSuspend() {
		Map<String, Object> pending = new HashMap<>();
		pending.put( "question",   "Approve?" );
		pending.put( "toolName",   "deleteTool" );
		pending.put( "toolArgs",   new HashMap<>() );
		pending.put( "toolCallId", "tc-99" );

		AiMiddlewareResult result = AiMiddlewareResult.suspend( pending );
		Map<?, ?> data = ( Map<?, ?> ) result.getData();
		assertEquals( "Approve?",    data.get( "question" ) );
		assertEquals( "deleteTool",  data.get( "toolName" ) );
		assertEquals( "tc-99",       data.get( "toolCallId" ) );
	}

	@Test
	public void testGetDataDefaultsToEmptyStruct() {
		// continue, approve, cancel, reject all have no meaningful data payload
		assertNotNull( AiMiddlewareResult.continueResult().getData() );
		assertNotNull( AiMiddlewareResult.approve().getData() );
		assertNotNull( AiMiddlewareResult.cancel( "x" ).getData() );
		assertNotNull( AiMiddlewareResult.reject( "x" ).getData() );
	}
}