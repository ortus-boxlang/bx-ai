package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.memory.CacheMemory;
import java.util.HashMap;
import java.util.Map;

public class CheckpointTest extends BaseMiddlewareTest {

	private CacheMemory memory() {
		return new CacheMemory();
	}

	private Map<String, Object> state() {
		Map<String, Object> state = new HashMap<>();
		state.put( "messages", new java.util.ArrayList<>() );
		state.put( "params",   new HashMap<>() );
		state.put( "options",  new HashMap<>() );
		Map<String, Object> pending = new HashMap<>();
		pending.put( "toolName",   "deleteRecord" );
		pending.put( "toolArgs",   new HashMap<>() );
		pending.put( "toolCallId", "tc-001" );
		pending.put( "question",   "Approve?" );
		state.put( "pending", pending );
		return state;
	}

	@Test
	public void testSaveAndLoadState() {
		CacheMemory mem = memory();
		mem.saveState( "thread-1", state(), 30 );

		Map<?, ?> loaded = mem.loadState( "thread-1" );
		assertNotNull( loaded );
		assertTrue( loaded.containsKey( "messages" ) );
		assertTrue( loaded.containsKey( "pending" ) );
	}

	@Test
	public void testLoadStateReturnsCorrectPending() {
		CacheMemory mem = memory();
		mem.saveState( "thread-2", state(), 30 );

		Map<?, ?> loaded  = mem.loadState( "thread-2" );
		Map<?, ?> pending = ( Map<?, ?> ) loaded.get( "pending" );

		assertEquals( "deleteRecord", pending.get( "toolName" ) );
		assertEquals( "Approve?",     pending.get( "question" ) );
	}

	@Test
	public void testClearStateRemovesCheckpoint() {
		CacheMemory mem = memory();
		mem.saveState( "thread-3", state(), 30 );
		mem.clearState( "thread-3" );

		try {
			mem.loadState( "thread-3" );
			fail( "Expected CheckpointNotFoundException after clearState" );
		} catch ( Exception e ) {
			assertTrue( e.getClass().getSimpleName().contains( "CheckpointNotFound" ) );
		}
	}

	@Test
	public void testLoadNonExistentStateThrows() {
		CacheMemory mem = memory();
		try {
			mem.loadState( "nonexistent-thread-id-xyz" );
			fail( "Expected CheckpointNotFoundException" );
		} catch ( Exception e ) {
			assertTrue( e.getClass().getSimpleName().contains( "CheckpointNotFound" ) );
		}
	}

	@Test
	public void testSaveStateOverwritesPrevious() {
		CacheMemory mem = memory();
		mem.saveState( "thread-4", state(), 30 );

		Map<String, Object> updated = state();
		Map<String, Object> updatedPending = new HashMap<>();
		updatedPending.put( "toolName",   "transferFunds" );
		updatedPending.put( "toolArgs",   new HashMap<>() );
		updatedPending.put( "toolCallId", "tc-002" );
		updatedPending.put( "question",   "Transfer $500?" );
		updated.put( "pending", updatedPending );

		mem.saveState( "thread-4", updated, 30 );

		Map<?, ?> loaded  = mem.loadState( "thread-4" );
		Map<?, ?> pending = ( Map<?, ?> ) loaded.get( "pending" );
		assertEquals( "transferFunds", pending.get( "toolName" ) );
	}

	@Test
	public void testDifferentThreadIdsAreIsolated() {
		CacheMemory mem = memory();
		mem.saveState( "thread-A", state(), 30 );

		Map<String, Object> stateB = state();
		Map<String, Object> pendingB = new HashMap<>();
		pendingB.put( "toolName",   "sendEmail" );
		pendingB.put( "toolArgs",   new HashMap<>() );
		pendingB.put( "toolCallId", "tc-B" );
		pendingB.put( "question",   "Send email?" );
		stateB.put( "pending", pendingB );
		mem.saveState( "thread-B", stateB, 30 );

		Map<?, ?> loadedA = mem.loadState( "thread-A" );
		Map<?, ?> loadedB = mem.loadState( "thread-B" );

		assertEquals( "deleteRecord", ( ( Map<?,?> ) loadedA.get( "pending" ) ).get( "toolName" ) );
		assertEquals( "sendEmail",    ( ( Map<?,?> ) loadedB.get( "pending" ) ).get( "toolName" ) );
	}
}