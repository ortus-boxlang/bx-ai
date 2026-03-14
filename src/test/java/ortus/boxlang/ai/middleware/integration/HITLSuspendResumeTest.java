package test.java.integration;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;
import java.util.*;

public class HITLSuspendResumeTest extends BaseIntegrationTest {

	// Build a HITL agent that suspends on "deleteRecord" with cache checkpointer
	private Object buildHITLAgent() {
		return bxExecute( """
            return aiAgent(
                name         : "HITLAgent",
                middleware   : [
                    new HumanInTheLoopMiddleware(
                        interruptOn : { deleteRecord: { allowedDecisions: ["approve","reject"], description: "Delete a record" } },
                        autoApprove : [ "readRecord" ],
                        mode        : "suspend"
                    )
                ],
                checkpointer  : aiMemory( "cache" ),
                checkpointTTL : 5
            );
        """ );
	}

	@Test
	void testAgentSuspendsOnWatchedToolCall() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Delete record 42" );

		assertTrue(
			bxInvoke( result, "isSuspended" ).equals( true ),
			"Agent should suspend when deleteRecord is called"
		);
	}

	@Test
	void testSuspendResultContainsPendingData() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Delete record 42" );

		assertTrue( bxInvoke( result, "isSuspended" ).equals( true ) );

		Object data = bxInvoke( result, "getData" );
		assertNotNull( data );

		Object toolName = bxGetKey( data, "toolName" );
		assertEquals( "deleteRecord", toolName );

		Object question = bxGetKey( data, "question" );
		assertNotNull( question );
		assertTrue( question.toString().length() > 0 );
	}

	@Test
	void testResumeWithApproveCompletesRun() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Delete record 42" );

		assertTrue( bxInvoke( result, "isSuspended" ).equals( true ) );

		String threadId = bxInvoke( agent, "getThreadId" ).toString();

		Object decision = invoke( "AiMiddlewareResult", "approve" );
		Object resumed  = bxInvoke( agent, "resume", decision, threadId );

		// After approval, agent should complete — not suspend again
		boolean isStillSuspended = Boolean.TRUE.equals( bxInvoke( resumed, "isSuspended" ) );
		assertFalse( isStillSuspended, "Agent should complete after approve" );
	}

	@Test
	void testCheckpointIsClearedAfterResume() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Delete record 42" );

		assertTrue( bxInvoke( result, "isSuspended" ).equals( true ) );

		String threadId = bxInvoke( agent, "getThreadId" ).toString();
		Object decision = invoke( "AiMiddlewareResult", "approve" );
		bxInvoke( agent, "resume", decision, threadId );

		// Checkpoint should be cleared — a second resume should throw
		assertThrows( Exception.class, () ->
			bxInvoke( agent, "resume", invoke( "AiMiddlewareResult", "approve" ), threadId )
		);
	}

	@Test
	void testAutoApprovedToolDoesNotSuspend() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Read record 42" );

		// readRecord is in autoApprove — should complete without suspension
		boolean isSuspended = Boolean.TRUE.equals( bxInvoke( result, "isSuspended" ) );
		assertFalse( isSuspended, "Auto-approved tool should not cause suspension" );
	}
}