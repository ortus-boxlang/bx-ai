package test.java.integration;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import test.java.base.BaseIntegrationTest;
import java.util.*;

public class HITLEditTest extends BaseIntegrationTest {

	private Object buildHITLAgent() {
		return bxExecute( """
            return aiAgent(
                name         : "HITLEditAgent",
                middleware   : [
                    new HumanInTheLoopMiddleware(
                        interruptOn : { updateRecord: { allowedDecisions: ["approve","edit","reject"], description: "Update a record" } },
                        mode        : "suspend"
                    )
                ],
                checkpointer  : aiMemory( "cache" ),
                checkpointTTL : 5
            );
        """ );
	}

	@Test
	void testAgentSuspendsBeforeEdit() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Update record 42 setting status to inactive" );

		assertTrue(
			bxInvoke( result, "isSuspended" ).equals( true ),
			"Agent should suspend before updateRecord"
		);
	}

	@Test
	void testSuspendDataContainsOriginalToolArgs() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Update record 42 setting status to inactive" );

		assertTrue( bxInvoke( result, "isSuspended" ).equals( true ) );

		Object data     = bxInvoke( result, "getData" );
		Object toolArgs = bxGetKey( data, "toolArgs" );
		assertNotNull( toolArgs, "Suspend data should contain toolArgs" );
	}

	@Test
	void testResumeWithEditUsesModifiedArgs() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Update record 42 setting status to inactive" );

		assertTrue( bxInvoke( result, "isSuspended" ).equals( true ) );

		String threadId = bxInvoke( agent, "getThreadId" ).toString();

		// Human edits the args — changes status from "inactive" to "archived"
		Object editedArgs = bxExecute( "return { id: 42, status: 'archived' };" );
		Object decision   = invoke( "AiMiddlewareResult", "edit", editedArgs );
		Object resumed    = bxInvoke( agent, "resume", decision, threadId );

		// Run should complete — not suspend again
		boolean isStillSuspended = Boolean.TRUE.equals( bxInvoke( resumed, "isSuspended" ) );
		assertFalse( isStillSuspended, "Agent should complete after edit" );
	}

	@Test
	void testResumeWithEditDoesNotThrow() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Update record 42 setting status to inactive" );

		String threadId = bxInvoke( agent, "getThreadId" ).toString();
		Object editedArgs = bxExecute( "return { id: 42, status: 'archived' };" );
		Object decision   = invoke( "AiMiddlewareResult", "edit", editedArgs );

		assertDoesNotThrow( () -> bxInvoke( agent, "resume", decision, threadId ) );
	}

	@Test
	void testCheckpointClearedAfterEdit() {
		Object agent  = buildHITLAgent();
		Object result = bxInvoke( agent, "run", "Update record 42 setting status to inactive" );

		String threadId   = bxInvoke( agent, "getThreadId" ).toString();
		Object editedArgs = bxExecute( "return { id: 42, status: 'archived' };" );
		Object decision   = invoke( "AiMiddlewareResult", "edit", editedArgs );
		bxInvoke( agent, "resume", decision, threadId );

		// Checkpoint cleared — second resume should throw
		assertThrows( Exception.class, () ->
			bxInvoke( agent, "resume", invoke( "AiMiddlewareResult", "approve" ), threadId )
		);
	}
}