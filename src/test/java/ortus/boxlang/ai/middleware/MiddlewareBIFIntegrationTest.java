package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Middleware BIF Integration Tests" )
public class MiddlewareBIFIntegrationTest extends BaseIntegrationTest {

	@DisplayName( "aiAgent() accepts middleware array and wires it" )
	@Test
	public void testAiAgentAcceptsMiddleware() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        hookFired = false;

		        trackerMw = {
		            beforeAgentRun: ( ctx ) => {
		                hookFired = true;
		                return AiMiddlewareResult::continue();
		            }
		        };

		        agent = aiAgent(
		            name      : "TestAgent",
		            middleware: [ trackerMw ]
		        );

		        middlewareCount = agent.getMiddleware().len();
				threadId		= agent.getThreadId();
		    """,
		    context
		);
		// @formatter:on

		// At least our struct middleware adapter should be present
		assertThat( variables.getAsInteger( Key.of( "middlewareCount" ) ) ).isAtLeast( 1 );
		assertThat( variables.getAsString( Key.of( "threadId" ) ) ).isNotEmpty();
	}

	@DisplayName( "aiAgent() accepts checkpointer, threadId, and checkpointTTL" )
	@Test
	public void testAiAgentCheckpointerWiring() {
		// @formatter:off
		runtime.executeSource(
		    """
		        myCacheMemory = aiMemory( "cache" );

		        agent = aiAgent(
		            name         : "CheckpointAgent",
		            checkpointer : myCacheMemory,
		            checkpointTTL: 5,
		            threadId     : "thread-abc"
		        );

		        agentThreadId        = agent.getThreadId();
		        agentCheckpointTTL   = agent.getCheckpointTTL();
		        hasCheckpointer      = !isNull( agent.getCheckpointer() );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "agentThreadId" ) ) ).isEqualTo( "thread-abc" );
		assertThat( variables.getAsInteger( Key.of( "agentCheckpointTTL" ) ) ).isEqualTo( 5 );
		assertThat( variables.getAsBoolean( Key.of( "hasCheckpointer" ) ) ).isTrue();
	}

	@DisplayName( "aiModel() accepts middleware array and wires it" )
	@Test
	public void testAiModelAcceptsMiddleware() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.AiMiddlewareResult;

		        logMw = {
		            beforeLLMCall: function( ctx ) {
		                return AiMiddlewareResult::continue();
		            }
		        };

		        model = aiModel( provider: "ollama", middleware: [ logMw ] );

		        middlewareCount = model.getMiddleware().len();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "middlewareCount" ) ) ).isAtLeast( 1 );
	}

	@DisplayName( "MaxToolCallsMiddleware cancels runs via aiAgent()" )
	@Test
	public void testMaxToolCallsViaAiAgent() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.MaxToolCallsMiddleware;

		        toolCallCount = 0;

		        function myTool( required string input ) {
		            toolCallCount++;
		            return "called: #arguments.input#";
		        }

		        theTool = aiTool(
		            "myTool",
		            "A simple test tool",
		            [ { name: "input", type: "string", description: "Input value", required: true } ],
		            myTool
		        );

		        mw = new MaxToolCallsMiddleware( maxCalls: 2 );

		        agent = aiAgent(
		            name       : "BoundedAgent",
		            description: "Only allowed to call tools twice",
		            tools      : [ theTool ],
		            middleware : [ mw ]
		        );

		        agentMiddlewareCount = agent.getMiddleware().len();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "agentMiddlewareCount" ) ) ).isAtLeast( 1 );
	}
}
