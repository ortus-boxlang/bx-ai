package ortus.boxlang.ai.runnables;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Issue #248 Part C — AiAgent.resumeStream() must not hand a resume context to a provider whose
 * chatStream() ignores it. Doing so silently replays the turn against the LLM and never runs the
 * tools the human just approved.
 *
 * Support is detected structurally (BaseService.supportsStreamingToolResume() → does the provider
 * have a resumeToolBatchStream()?), so these tests also pin that detection down.
 *
 * Fully offline: no provider is ever called, only interrogated.
 */
public class AiAgentResumeStreamGuardTest extends BaseIntegrationTest {

	@DisplayName( "Providers that implement resumeToolBatchStream() report streaming-resume support" )
	@Test
	public void testSupportsStreamingToolResumeDetection() {
		// @formatter:off
		runtime.executeSource(
		    """
		        openaiSupported = aiService( "openai", { apiKey: "dummy-key" } ).supportsStreamingToolResume()
		        claudeSupported = aiService( "claude", { apiKey: "dummy-key" } ).supportsStreamingToolResume()
		        cohereSupported = aiService( "cohere", { apiKey: "dummy-key" } ).supportsStreamingToolResume()
		        mockSupported   = aiService( "mock" ).supportsStreamingToolResume()
		        bedrockSupported = aiService( "bedrock", { awsAccessKeyId: "AKIAIOSFODNN7EXAMPLE", awsSecretAccessKey: "x", region: "us-east-1" } ).supportsStreamingToolResume()

		        // Voyage extends BaseService directly and has no streaming tool machinery at all
		        voyageSupported = aiService( "voyage", { apiKey: "dummy-key" } ).supportsStreamingToolResume()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "openaiSupported" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "claudeSupported" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "cohereSupported" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "mockSupported" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bedrockSupported" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "voyageSupported" ) ) ).isFalse();
	}

	@DisplayName( "resumeStream() throws StreamingResumeUnsupported instead of replaying the LLM" )
	@Test
	public void testResumeStreamThrowsForUnsupportedProvider() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.runnables.AiModel;

		        toolCalls = 0
		        tool      = aiTool( "toolA", "Tool A", () => { toolCalls++; return "A done" } )

		        // Voyage has no chatStream() resume path — exactly the shape the guard protects against
		        model        = new AiModel( service: aiService( "voyage", { apiKey: "dummy-key" } ) )
		        checkpointer = aiMemory( "cache" )
		        agent        = aiAgent( model: model, tools: [ tool ], checkpointer: checkpointer )

		        // Hand-rolled batch checkpoint, the shape a suspend would have written
		        checkpointer.saveState( "guard-test-unsupported", {
		            input      : "please run toolA",
		            params     : {},
		            options    : {},
		            suspendData: {
		                assistantMessage: { toolCalls: [ { name: "toolA", parameters: {} } ] },
		                pendingActions  : [ { toolName: "toolA", toolArgs: {}, suspensionID: "" } ]
		            }
		        } )

		    """,
		    context
		);
		// @formatter:on

		var thrown = assertThrows(
		    BoxRuntimeException.class,
		    () -> runtime.executeSource( "agent.resumeStream( ( chunk ) => {}, \"approve\", \"guard-test-unsupported\" )", context )
		);

		assertThat( thrown.getType() ).isEqualTo( "AiAgent.StreamingResumeUnsupported" );
		assertThat( thrown.getMessage() ).contains( "Voyage" );
		assertThat( thrown.getMessage() ).contains( "resume()" );

		runtime.executeSource( "toolNeverRan = toolCalls == 0", context );
		assertThat( variables.getAsBoolean( Key.of( "toolNeverRan" ) ) ).isTrue();

		// The guard runs BEFORE clearState() — a rejected resumeStream() must leave the
		// checkpoint intact so resume() (non-streaming) is still a usable fallback.
		// @formatter:off
		runtime.executeSource(
		    """
		        reloaded          = checkpointer.loadState( "guard-test-unsupported" )
		        checkpointSurvived = !reloaded.isEmpty() && ( reloaded.suspendData ?: {} ).keyExists( "assistantMessage" )

		        // resume() must get past the checkpoint lookup (it fails later, on Voyage having
		        // no chat path at all — never with CheckpointNotFound)
		        resumeErrType = ""
		        try {
		            agent.resume( "approve", "guard-test-unsupported" )
		        } catch ( any e ) {
		            resumeErrType = e.type
		        }
		        resumeFoundCheckpoint = resumeErrType != "AiAgent.CheckpointNotFound"
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "checkpointSurvived" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resumeFoundCheckpoint" ) ) ).isTrue();
	}

	@DisplayName( "The guard does NOT fire for a single-tool-call checkpoint on an unsupported provider" )
	@Test
	public void testSingleCallResumeStreamIsNotGuarded() {
		// Single-call resume is provider-agnostic: HumanInTheLoopMiddleware.beforeToolCall()
		// consumes resumeDecision on a deliberate LLM replay, so resumeToolBatchStream() is
		// irrelevant. Guarding it here would break Gemini and every other provider reporting false.
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.runnables.AiModel;

		        model        = new AiModel( service: aiService( "voyage", { apiKey: "dummy-key" } ) )
		        checkpointer = aiMemory( "cache" )
		        agent        = aiAgent( model: model, checkpointer: checkpointer )

		        // Single-tool-call checkpoint: no assistantMessage key
		        checkpointer.saveState( "guard-test-single", {
		            input      : "please run toolA",
		            params     : {},
		            options    : {},
		            suspendData: { toolName: "toolA", toolArgs: {} }
		        } )

		    """,
		    context
		);
		// @formatter:on

		// It must get past the guard and into stream(); Voyage then fails for its own reasons
		// (no chat/chatStream at all), which is fine — just never with the guard's type.
		var thrown = assertThrows(
		    Throwable.class,
		    () -> runtime.executeSource( "agent.resumeStream( ( chunk ) => {}, \"approve\", \"guard-test-single\" )", context )
		);

		if ( thrown instanceof BoxRuntimeException bre ) {
			assertThat( bre.getType() ).isNotEqualTo( "AiAgent.StreamingResumeUnsupported" );
		}
	}

	@DisplayName( "resume() rejecting a wrong decision count leaves the checkpoint retryable" )
	@Test
	public void testResumeDecisionCountMismatchKeepsCheckpoint() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolCalls = 0
		        toolA     = aiTool( "toolA", "Tool A", () => { toolCalls++; return "A done" } )
		        toolB     = aiTool( "toolB", "Tool B", () => { toolCalls++; return "B done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} }, { name: "toolB", arguments: {} } ] },
		            "both ran."
		        ] )

		        checkpointer = aiMemory( "cache" )
		        agent = aiAgent(
		            model       : new AiModel( service: mockSvc ),
		            tools       : [ toolA, toolB ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ) ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        agent.run( "please run both tools", {}, { threadId: "count-mismatch-sync" } )
		    """,
		    context
		);
		// @formatter:on

		// WRONG count: two pending calls, one decision
		var thrown = assertThrows(
		    BoxRuntimeException.class,
		    () -> runtime.executeSource( "agent.resume( [ { decision: \"approve\" } ], \"count-mismatch-sync\" )", context )
		);
		assertThat( thrown.getType() ).isEqualTo( "AiAgent.ResumeDecisionCountMismatch" );

		// @formatter:off
		runtime.executeSource(
		    """
		        // Checkpoint must have survived the rejected resume
		        reloaded           = checkpointer.loadState( "count-mismatch-sync" )
		        checkpointSurvived = !reloaded.isEmpty() && ( reloaded.suspendData ?: {} ).keyExists( "assistantMessage" )
		        toolsNotRunYet     = toolCalls == 0

		        // Retry with the CORRECT count now succeeds
		        agent.resume( [ { decision: "approve" }, { decision: "approve" } ], "count-mismatch-sync" )
		        bothToolsRan = toolCalls == 2
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "checkpointSurvived" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolsNotRunYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothToolsRan" ) ) ).isTrue();
	}

	@DisplayName( "resumeStream() rejecting a wrong decision count leaves the checkpoint retryable" )
	@Test
	public void testResumeStreamDecisionCountMismatchKeepsCheckpoint() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolCalls = 0
		        toolA     = aiTool( "toolA", "Tool A", () => { toolCalls++; return "A done" } )
		        toolB     = aiTool( "toolB", "Tool B", () => { toolCalls++; return "B done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} }, { name: "toolB", arguments: {} } ] },
		            "both ran."
		        ] )

		        checkpointer = aiMemory( "cache" )
		        agent = aiAgent(
		            model       : new AiModel( service: mockSvc ),
		            tools       : [ toolA, toolB ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ) ],
		            checkpointer: checkpointer,
		            checkpointTTL: 5
		        )

		        agent.stream( ( chunk ) => {}, "please run both tools", {}, { threadId: "count-mismatch-stream" } )
		    """,
		    context
		);
		// @formatter:on

		var thrown = assertThrows(
		    BoxRuntimeException.class,
		    () -> runtime.executeSource(
		        "agent.resumeStream( ( chunk ) => {}, [ { decision: \"approve\" } ], \"count-mismatch-stream\" )",
		        context
		    )
		);
		assertThat( thrown.getType() ).isEqualTo( "AiAgent.ResumeDecisionCountMismatch" );

		// @formatter:off
		runtime.executeSource(
		    """
		        reloaded           = checkpointer.loadState( "count-mismatch-stream" )
		        checkpointSurvived = !reloaded.isEmpty() && ( reloaded.suspendData ?: {} ).keyExists( "assistantMessage" )
		        toolsNotRunYet     = toolCalls == 0

		        agent.resumeStream( ( chunk ) => {}, [ { decision: "approve" }, { decision: "approve" } ], "count-mismatch-stream" )
		        bothToolsRan = toolCalls == 2
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "checkpointSurvived" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolsNotRunYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothToolsRan" ) ) ).isTrue();
	}

	@DisplayName( "The guard leaves a supported provider's single-call resumeStream() untouched" )
	@Test
	public void testResumeStreamStillWorksForSupportedProvider() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
		        import bxModules.bxai.models.runnables.AiModel;

		        toolCalls = 0
		        tool      = aiTool( "toolA", "Tool A - requires approval", () => { toolCalls++; return "A done" } )

		        mockSvc = aiService( "mock" )
		        mockSvc.setResponses( [
		            { toolCalls: [ { name: "toolA", arguments: {} } ] },
		            "toolA ran."
		        ] )

		        agent = aiAgent(
		            model       : new AiModel( service: mockSvc ),
		            tools       : [ tool ],
		            middleware  : [ new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA" ], mode: "web" ) ],
		            checkpointer: aiMemory( "cache" ),
		            checkpointTTL: 5
		        )

		        chunks = []
		        agent.stream( ( chunk ) => { chunks.append( chunk ) }, "please run toolA", {}, { threadId: "guard-test-supported" } )

		        suspended = chunks.some( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )

		        resumeChunks = []
		        agent.resumeStream( ( chunk ) => { resumeChunks.append( chunk ) }, "approve", "guard-test-supported" )

		        toolRan = toolCalls == 1
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolRan" ) ) ).isTrue();
	}

}
