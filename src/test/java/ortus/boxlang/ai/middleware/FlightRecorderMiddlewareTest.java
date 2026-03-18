package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "FlightRecorderMiddleware Unit Tests" )
public class FlightRecorderMiddlewareTest extends BaseIntegrationTest {

	// ---- Passthrough ----

	@DisplayName( "passthrough mode: wrapLLMCall delegates to handler and returns result" )
	@Test
	public void testPassthroughCallsHandler() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "passthrough" );

		        handlerCalled = false;
		        result = mw.wrapLLMCall(
		            context : { dataPacket: { model: "gpt-4", messages: [] } },
		            handler : function() {
		                handlerCalled = true;
		                return { id: "chatcmpl-test", choices: [ { message: { content: "hello" } } ] };
		            }
		        );

		        handlerWasCalled = handlerCalled;
		        tapeIsEmpty      = mw.getTape().isEmpty();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "handlerWasCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "tapeIsEmpty" ) ) ).isTrue();
	}

	// ---- Record: LLM ----

	@DisplayName( "record mode: wrapLLMCall captures one LLM interaction in the tape" )
	@Test
	public void testRecordCaptureLLMInteraction( @TempDir Path tempDir ) throws IOException {
		String fixturePath = tempDir.resolve( "test-llm.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode       : "record",
		            fixturePath: "%s"
		        );
		        mw.beforeAgentRun( context: {} );

		        mw.wrapLLMCall(
		            context : { dataPacket: { model: "gpt-4", messages: [] } },
		            handler : function() {
		                return { id: "resp-1", choices: [ { message: { content: "Paris" } } ] };
		            }
		        );

		        tape               = mw.getTape();
		        tapeHasInteraction = tape.interactions.len() == 1;
		        firstIsLLM         = tape.interactions[1].type == "llm";
		        firstSeq           = tape.interactions[1].seq == 1;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "tapeHasInteraction" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firstIsLLM" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firstSeq" ) ) ).isTrue();

		// Fixture file must exist on disk after every interaction (crash-safe)
		assertThat( Files.exists( Path.of( fixturePath ) ) ).isTrue();
	}

	// ---- Record: Tool ----

	@DisplayName( "record mode: wrapToolCall captures one tool interaction in the tape" )
	@Test
	public void testRecordCaptureToolInteraction( @TempDir Path tempDir ) throws IOException {
		String fixturePath = tempDir.resolve( "test-tool.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode       : "record",
		            fixturePath: "%s",
		            recordTools: true
		        );
		        mw.beforeAgentRun( context: {} );

		        result = mw.wrapToolCall(
		            context: {
		                toolCall: { function: { name: "getWeather", arguments: '{"city":"London"}' } }
		            },
		            handler: function() {
		                return "72F, sunny";
		            }
		        );

		        tape               = mw.getTape();
		        tapeHasTool        = tape.interactions.len() == 1;
		        firstIsTool        = tape.interactions[1].type == "tool";
		        firstToolName      = tape.interactions[1].toolName == "getWeather";
		        firstToolResult    = tape.interactions[1].result == "72F, sunny";
		        resultPassed       = result == "72F, sunny";
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "tapeHasTool" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firstIsTool" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firstToolName" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firstToolResult" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resultPassed" ) ) ).isTrue();
	}

	// ---- Record: recordTools=false ----

	@DisplayName( "record mode: recordTools=false passes through tool call without taping it" )
	@Test
	public void testRecordToolsFalseNoTapeEntry( @TempDir Path tempDir ) {
		String fixturePath = tempDir.resolve( "test-notool.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode       : "record",
		            fixturePath: "%s",
		            recordTools: false
		        );
		        mw.beforeAgentRun( context: {} );

		        handlerCalled = false;
		        mw.wrapToolCall(
		            context: {
		                toolCall: { function: { name: "search", arguments: '{"q":"test"}' } }
		            },
		            handler: function() {
		                handlerCalled = true;
		                return "some result";
		            }
		        );

		        tapeEmpty     = mw.getTape().interactions.len() == 0;
		        handlerFired  = handlerCalled;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "tapeEmpty" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "handlerFired" ) ) ).isTrue();
	}

	// ---- Replay: LLM ----

	@DisplayName( "replay mode: wrapLLMCall returns recorded response and does NOT call handler" )
	@Test
	public void testReplayLLMWithoutCallingHandler( @TempDir Path tempDir ) throws IOException {
		// Write a minimal fixture to disk
		String	fixturePath	= tempDir.resolve( "recorded.json" ).toString();
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "llm", "request": { "model": "gpt-4" }, "response": { "id": "replay-resp", "choices": [ { "message": { "content": "replayed answer" } } ] } }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode       : "replay",
		            fixturePath: "%s"
		        );
		        mw.beforeAgentRun( context: {} );

		        handlerCalled = false;
		        result = mw.wrapLLMCall(
		            context: { dataPacket: { model: "gpt-4" } },
		            handler: function() {
		                handlerCalled = true;
		                return {};
		            }
		        );

		        handlerNotCalled   = !handlerCalled;
		        replayedId         = result.id == "replay-resp";
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "handlerNotCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "replayedId" ) ) ).isTrue();
	}

	// ---- Replay: Tool ----

	@DisplayName( "replay mode: wrapToolCall returns recorded result and does NOT invoke tool" )
	@Test
	public void testReplayToolWithoutCallingTool( @TempDir Path tempDir ) throws IOException {
		String	fixturePath	= tempDir.resolve( "recorded-tool.json" ).toString();
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "tool", "toolName": "getWeather", "arguments": { "city": "London" }, "result": "17C, cloudy" }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode       : "replay",
		            fixturePath: "%s"
		        );
		        mw.beforeAgentRun( context: {} );

		        invoked = false;
		        result = mw.wrapToolCall(
		            context: { toolCall: { function: { name: "getWeather", arguments: '{"city":"London"}' } } },
		            handler: function() {
		                invoked = true;
		                return "live result";
		            }
		        );

		        handlerNotInvoked  = !invoked;
		        replayedResult     = result == "17C, cloudy";
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "handlerNotInvoked" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "replayedResult" ) ) ).isTrue();
	}

	// ---- Replay Exhausted ----

	@DisplayName( "replay mode: throws FlightRecorder.ReplayExhausted when tape runs out" )
	@Test
	public void testReplayExhaustionThrows( @TempDir Path tempDir ) throws IOException {
		String	fixturePath	= tempDir.resolve( "one-interaction.json" ).toString();
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "llm", "request": {}, "response": { "id": "only-one" } }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode       : "replay",
		            fixturePath: "%s"
		        );
		        mw.beforeAgentRun( context: {} );

		        // Consume the one recorded interaction
		        mw.wrapLLMCall( context: {}, handler: function() { return {}; } );

		        // Second call should throw
		        exhausted = false;
		        try {
		            mw.wrapLLMCall( context: {}, handler: function() { return {}; } );
		        } catch( e ) {
		            exhausted = e.type contains "ReplayExhausted";
		        }
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "exhausted" ) ) ).isTrue();
	}

	// ---- Strict TypeMismatch ----

	@DisplayName( "replay mode + strict=true: throws FlightRecorder.TypeMismatch when types differ" )
	@Test
	public void testStrictTypeMismatchThrows( @TempDir Path tempDir ) throws IOException {
		String	fixturePath	= tempDir.resolve( "type-mismatch.json" ).toString();
		// Tape has a "tool" interaction but test requests an "llm" interaction
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "tool", "toolName": "search", "arguments": {}, "result": "some result" }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode       : "replay",
		            fixturePath: "%s",
		            strict     : true
		        );
		        mw.beforeAgentRun( context: {} );

		        typeMismatch = false;
		        try {
		            // Tape has "tool" but we ask for "llm"
		            mw.wrapLLMCall( context: {}, handler: function() { return {}; } );
		        } catch( e ) {
		            typeMismatch = e.type contains "TypeMismatch";
		        }
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "typeMismatch" ) ) ).isTrue();
	}

	// ---- Non-strict skips ahead ----

	@DisplayName( "replay mode + strict=false: skips mismatched type and finds next matching entry" )
	@Test
	public void testNonStrictTypeMismatchSkips( @TempDir Path tempDir ) throws IOException {
		String	fixturePath	= tempDir.resolve( "lenient.json" ).toString();
		// Tape: tool, tool, llm — asking for llm should skip to seq 3
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "tool", "toolName": "a", "arguments": {}, "result": "r1" },
		                          { "seq": 2, "type": "tool", "toolName": "b", "arguments": {}, "result": "r2" },
		                          { "seq": 3, "type": "llm",  "request": {}, "response": { "id": "llm-3" } }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode       : "replay",
		            fixturePath: "%s",
		            strict     : false
		        );
		        mw.beforeAgentRun( context: {} );

		        result   = mw.wrapLLMCall( context: {}, handler: function() { return {}; } );
		        skippedToLLM = result.id == "llm-3";
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "skippedToLLM" ) ) ).isTrue();
	}

	// ---- End-to-end: record then replay ----

	@DisplayName( "end-to-end: record a run, then replay it with a fresh instance" )
	@Test
	public void testEndToEndRecordThenReplay( @TempDir Path tempDir ) throws IOException {
		String fixturePath = tempDir.resolve( "e2e.json" ).toString();

		// Phase 1 — record
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        recorder = new FlightRecorderMiddleware(
		            mode       : "record",
		            fixturePath: "%s"
		        );
		        recorder.beforeAgentRun( context: {} );

		        r1 = recorder.wrapLLMCall(
		            context : { dataPacket: { model: "gpt-4" } },
		            handler : function() {
		                return { id: "r1", choices: [ { message: { content: "The capital is Paris." } } ] };
		            }
		        );
		        r2 = recorder.wrapToolCall(
		            context : { toolCall: { function: { name: "lookup", arguments: '{"q":"Paris"}' } } },
		            handler : function() { return "Paris is the capital of France"; }
		        );
		        r3 = recorder.wrapLLMCall(
		            context : { dataPacket: { model: "gpt-4" } },
		            handler : function() {
		                return { id: "r3", choices: [ { message: { content: "Final answer: Paris." } } ] };
		            }
		        );
		        recorder.afterAgentRun( context: {} );
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( Files.exists( Path.of( fixturePath ) ) ).isTrue();

		// Phase 2 — replay in a fresh context
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.builtin.FlightRecorderMiddleware;

		        replayer = new FlightRecorderMiddleware(
		            mode       : "replay",
		            fixturePath: "%s"
		        );
		        replayer.beforeAgentRun( context: {} );

		        handlerCallCount = 0;
		        noOpHandler = function() { handlerCallCount++; return {}; };
		        noOpToolHandler = function() { handlerCallCount++; return ""; };

		        pr1 = replayer.wrapLLMCall( context: {}, handler: noOpHandler );
		        pt1 = replayer.wrapToolCall(
		            context: { toolCall: { function: { name: "lookup", arguments: '{}' } } },
		            handler: noOpToolHandler
		        );
		        pr3 = replayer.wrapLLMCall( context: {}, handler: noOpHandler );

		        llm1Correct  = pr1.id == "r1";
		        toolCorrect  = pt1 == "Paris is the capital of France";
		        llm3Correct  = pr3.id == "r3";
		        noRealCalls  = handlerCallCount == 0;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "llm1Correct" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolCorrect" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "llm3Correct" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noRealCalls" ) ) ).isTrue();
	}
}
