package test.java.integration;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamingSuspendTest extends BaseIntegrationTest {

	private Object buildStreamingHITLAgent() {
		return bxExecute( """
            return aiAgent(
                name         : "StreamingHITLAgent",
                middleware   : [
                    new HumanInTheLoopMiddleware(
                        interruptOn : { deleteRecord: { allowedDecisions: ["approve","reject"], description: "Delete a record" } },
                        mode        : "suspend"
                    )
                ],
                checkpointer  : aiMemory( "cache" ),
                checkpointTTL : 5
            );
        """ );
	}

	@Test
	void testStreamEmitsSuspendChunkWhenToolIntercepted() {
		Object agent = buildStreamingHITLAgent();

		List<Object> chunks       = new ArrayList<>();
		AtomicBoolean didSuspend  = new AtomicBoolean( false );

		Object callback = closure( chunk -> {
			chunks.add( chunk );
			if ( isStruct( chunk ) ) {
				Object type = bxGetKey( chunk, "type" );
				if ( "suspend".equals( type ) ) {
					didSuspend.set( true );
				}
			}
			return null;
		});

		bxInvoke( agent, "stream", callback, "Delete record 42" );

		assertTrue( didSuspend.get(), "Stream should emit a suspend chunk" );
	}

	@Test
	void testSuspendChunkIsLastChunkInStream() {
		Object agent = buildStreamingHITLAgent();

		List<Object> chunks      = new ArrayList<>();
		AtomicBoolean afterSuspend = new AtomicBoolean( false );

		Object callback = closure( chunk -> {
			if ( didSuspendAlready( chunks ) ) {
				afterSuspend.set( true );
			}
			chunks.add( chunk );
			return null;
		});

		bxInvoke( agent, "stream", callback, "Delete record 42" );

		assertFalse( afterSuspend.get(), "No chunks should arrive after the suspend chunk" );
	}

	@Test
	void testSuspendChunkContainsRequiredData() {
		Object agent  = buildStreamingHITLAgent();
		List<Object> chunks = new ArrayList<>();

		Object callback = closure( chunk -> { chunks.add( chunk ); return null; } );
		bxInvoke( agent, "stream", callback, "Delete record 42" );

		Object suspendChunk = chunks.stream()
			.filter( c -> isStruct( c ) && "suspend".equals( bxGetKey( c, "type" ) ) )
			.findFirst()
			.orElse( null );

		assertNotNull( suspendChunk, "Suspend chunk should be present in stream" );

		Object data = bxGetKey( suspendChunk, "data" );
		assertNotNull( data );
		assertNotNull( bxGetKey( data, "toolName" ) );
		assertNotNull( bxGetKey( data, "question" ) );
		assertNotNull( bxGetKey( data, "toolCallId" ) );
	}

	@Test
	void testResumeStreamAfterSuspendEmitsChunks() {
		Object agent  = buildStreamingHITLAgent();
		List<Object> initialChunks = new ArrayList<>();

		Object initialCallback = closure( chunk -> { initialChunks.add( chunk ); return null; } );
		bxInvoke( agent, "stream", initialCallback, "Delete record 42" );

		// Confirm suspension
		boolean suspended = initialChunks.stream()
			.anyMatch( c -> isStruct( c ) && "suspend".equals( bxGetKey( c, "type" ) ) );
		assertTrue( suspended, "Initial stream should suspend" );

		// Resume with streaming
		String threadId = bxInvoke( agent, "getThreadId" ).toString();
		List<Object> resumeChunks = new ArrayList<>();

		Object resumeCallback = closure( chunk -> { resumeChunks.add( chunk ); return null; } );
		Object decision       = invoke( "AiMiddlewareResult", "approve" );

		bxInvoke( agent, "resumeStream", resumeCallback, decision, threadId );

		// Resumed stream should emit at least one chunk
		assertFalse( resumeChunks.isEmpty(), "resumeStream should emit chunks after approval" );

		// Resumed stream should not contain another suspend chunk
		boolean resumeSuspended = resumeChunks.stream()
			.anyMatch( c -> isStruct( c ) && "suspend".equals( bxGetKey( c, "type" ) ) );
		assertFalse( resumeSuspended, "Resumed stream should not suspend again" );
	}

	@Test
	void testStreamCompletesCleanlyWithoutSuspension() {
		// Agent with no HITL middleware — stream should complete without suspend chunk
		Object agent = bxExecute( "return aiAgent( name: 'CleanStreamAgent' );" );

		List<Object> chunks      = new ArrayList<>();
		AtomicBoolean suspended  = new AtomicBoolean( false );

		Object callback = closure( chunk -> {
			chunks.add( chunk );
			if ( isStruct( chunk ) && "suspend".equals( bxGetKey( chunk, "type" ) ) ) {
				suspended.set( true );
			}
			return null;
		});

		bxInvoke( agent, "stream", callback, "What is 2 + 2?" );

		assertFalse( suspended.get(), "Clean stream should not emit suspend chunk" );
		assertFalse( chunks.isEmpty(), "Clean stream should emit at least one chunk" );
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private boolean didSuspendAlready( List<Object> chunks ) {
		return chunks.stream()
			.anyMatch( c -> isStruct( c ) && "suspend".equals( bxGetKey( c, "type" ) ) );
	}

	private boolean isStruct( Object obj ) {
		if ( obj == null ) return false;
		return obj instanceof Map;
	}
}
