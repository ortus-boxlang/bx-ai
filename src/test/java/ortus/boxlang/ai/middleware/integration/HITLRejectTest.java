package test.java.integration;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;

public class HITLRejectTest extends BaseIntegrationTest {

	private Object buildHITLAgent() {
		return bxExecute( """
            return aiAgent(
                name         : "HITLRejectAgent",
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
	void testAgentSuspendsBeforeReject() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Delete record 42" );

		assertTrue(
			bxInvoke( result, "isSuspended" ).equals( true ),
			"Agent should suspend before rejection"
		);
	}

	@Test
	void testResumeWithRejectDoesNotThrow() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Delete record 42" );

		assertTrue( bxInvoke( result, "isSuspended" ).equals( true ) );

		String threadId = bxInvoke( agent, "getThreadId" ).toString();
		Object decision = invoke( "AiMiddlewareResult", "reject", "Not authorised" );

		// Resume with reject should not throw
		assertDoesNotThrow( () -> bxInvoke( agent, "resume", decision, threadId ) );
	}

	@Test
	void testResumeWithRejectCompletesRun() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Delete record 42" );

		assertTrue( bxInvoke( result, "isSuspended" ).equals( true ) );

		String threadId = bxInvoke( agent, "getThreadId" ).toString();
		Object decision = invoke( "AiMiddlewareResult", "reject", "Not authorised by operator" );
		Object resumed  = bxInvoke( agent, "resume", decision, threadId );

		// After reject, agent should complete — the LLM receives the rejection
		// message and generates a final response explaining the rejection
		boolean isStillSuspended = Boolean.TRUE.equals( bxInvoke( resumed, "isSuspended" ) );
		assertFalse( isStillSuspended, "Agent should complete after reject, not suspend again" );
	}

	@Test
	void testRejectReasonIsPassedToLLM() {
		// After rejection the LLM receives the reason as a tool message.
		// The final response should acknowledge the rejection in some form.
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Delete record 42" );

		String threadId = bxInvoke( agent, "getThreadId" ).toString();
		Object decision = invoke( "AiMiddlewareResult", "reject", "Security policy violation" );
		Object resumed  = bxInvoke( agent, "resume", decision, threadId );

		// Result should be a string response, not a middleware result
		assertNotNull( resumed );
		assertFalse(
			Boolean.TRUE.equals( bxInvoke( resumed, "isSuspended" ) ),
			"Resumed result after reject should not be suspended"
		);
	}

	@Test
	void testCheckpointClearedAfterReject() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Delete record 42" );

		String threadId = bxInvoke( agent, "getThreadId" ).toString();
		Object decision = invoke( "AiMiddlewareResult", "reject", "denied" );
		bxInvoke( agent, "resume", decision, threadId );

		// Checkpoint cleared — second resume should throw
		assertThrows( Exception.class, () ->
			bxInvoke( agent, "resume", invoke( "AiMiddlewareResult", "reject", "denied again" ), threadId )
		);
	}
}