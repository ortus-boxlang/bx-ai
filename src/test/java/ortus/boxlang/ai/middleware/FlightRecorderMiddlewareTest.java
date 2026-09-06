package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@DisplayName( "FlightRecorderMiddleware Unit Tests" )
public class FlightRecorderMiddlewareTest extends BaseIntegrationTest {

	// ---- Passthrough ----

	@DisplayName( "passthrough mode: wrapLLMCall delegates to handler and returns result" )
	@Test
	public void testPassthroughCallsHandler() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

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

	// ---- Passthrough: wrapToolCall ----

	@DisplayName( "passthrough mode: wrapToolCall also delegates to handler and returns result" )
	@Test
	public void testPassthroughWrapToolCallDelegates() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "passthrough" );

		        handlerCalled = false;
		        result = mw.wrapToolCall(
		            context: { toolCall: { function: { name: "ping", arguments: '{}' } } },
		            handler: function() {
		                handlerCalled = true;
		                return "pong";
		            }
		        );

		        handlerWasCalled = handlerCalled;
		        resultCorrect    = result == "pong";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "handlerWasCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resultCorrect" ) ) ).isTrue();
	}

	// ---- Missing fixture path ----

	@DisplayName( "replay mode: throws FlightRecorder.MissingFixturePath when fixturePath is not set" )
	@Test
	public void testReplayMissingFixturePathThrows() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        // replay mode with no fixturePath
		        mw = new FlightRecorderMiddleware( mode: "replay" );

		        threw = false;
		        try {
		            mw.beforeAgentRun( context: {} );
		        } catch( e ) {
		            threw = e.type contains "MissingFixturePath";
		        }
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threw" ) ) ).isTrue();
	}

	// ---- Lazy init (no beforeAgentRun) ----

	@DisplayName( "record mode: lazy init works when beforeAgentRun is not called" )
	@Test
	public void testRecordLazyInit( @TempDir Path tempDir ) throws IOException {
		String fixturePath = tempDir.resolve( "lazy.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        // Deliberately skip beforeAgentRun — lazy init should kick in on first wrap call
		        mw = new FlightRecorderMiddleware(
		            mode       : "record",
		            fixturePath: "%s"
		        );

		        mw.wrapLLMCall(
		            context: {},
		            handler: function() { return { id: "lazy-1" }; }
		        );

		        tape            = mw.getTape();
		        tapeHasOneEntry = tape.interactions.len() == 1;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "tapeHasOneEntry" ) ) ).isTrue();
		assertThat( Files.exists( Path.of( fixturePath ) ) ).isTrue();
	}

	// ---- reset() ----

	@DisplayName( "reset() clears in-memory tape and re-enables lazy init" )
	@Test
	public void testResetClearsState( @TempDir Path tempDir ) {
		String fixturePath = tempDir.resolve( "reset.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode       : "record",
		            fixturePath: "%s"
		        );
		        mw.beforeAgentRun( context: {} );

		        mw.wrapLLMCall(
		            context: {},
		            handler: function() { return { id: "before-reset" }; }
		        );

		        tapeBeforeReset = mw.getTape().interactions.len();

		        mw.reset();
		        tapeAfterReset = mw.getTape().isEmpty();
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "tapeBeforeReset" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "tapeAfterReset" ) ) ).isTrue();
	}

	// ---- storageCallback: record mode ----

	@DisplayName( "record mode: storageCallback fires with tape + interaction after each append" )
	@Test
	public void testStorageCallbackFiresInRecordMode( @TempDir Path tempDir ) {
		String fixturePath = tempDir.resolve( "callback-record.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        callbackCount     = 0;
		        callbackTape      = {};
		        callbackInteraction = {};

		        mw = new FlightRecorderMiddleware(
		            mode           : "record",
		            fixturePath    : "%s",
		            storageCallback: function( tape, interaction ) {
		                callbackCount++;
		                callbackTape = tape;
		                callbackInteraction = interaction;
		            }
		        );
		        mw.beforeAgentRun( context: {} );

		        mw.wrapLLMCall(
		            context : { dataPacket: { model: "gpt-4", messages: [] } },
		            handler : function() {
		                return { id: "resp-1", choices: [ { message: { content: "Paris" } } ] };
		            }
		        );

		        firedOnce       = callbackCount == 1;
		        tapeHasEntry    = callbackTape.interactions.len() == 1;
		        interactionIsLLM = callbackInteraction.type == "llm";
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "firedOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "tapeHasEntry" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "interactionIsLLM" ) ) ).isTrue();
	}

	// ---- observe mode ----

	@DisplayName( "observe mode: accumulates tape, fires storageCallback, writes NO fixture file" )
	@Test
	public void testObserveModeAccumulatesNoFile( @TempDir Path tempDir ) {
		String fixturePath = tempDir.resolve( "callback-observe.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        callbackCount = 0;

		        mw = new FlightRecorderMiddleware(
		            mode           : "observe",
		            fixturePath    : "%s",
		            storageCallback: function( tape, interaction ) {
		                callbackCount++;
		            }
		        );
		        mw.beforeAgentRun( context: {} );

		        mw.wrapLLMCall(
		            context : { dataPacket: { model: "gpt-4", messages: [] } },
		            handler : function() {
		                return { id: "resp-1", choices: [ { message: { content: "Paris" } } ] };
		            }
		        );

		        mw.afterAgentRun( context: {} );

		        tape          = mw.getTape();
		        tapeHasEntry  = tape.interactions.len() == 1;
		        firedOnce     = callbackCount == 1;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "tapeHasEntry" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firedOnce" ) ) ).isTrue();

		// No fixture file should ever be written in observe mode
		assertThat( Files.exists( Path.of( fixturePath ) ) ).isFalse();
	}

	// ---- storageCallback exception swallowed ----

	@DisplayName( "storageCallback exception is swallowed and does not break the call" )
	@Test
	public void testStorageCallbackExceptionSwallowed( @TempDir Path tempDir ) {
		String fixturePath = tempDir.resolve( "callback-throws.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware(
		            mode           : "record",
		            fixturePath    : "%s",
		            storageCallback: function( tape, interaction ) {
		                throw( type: "BoomError", message: "callback boom" );
		            }
		        );
		        mw.beforeAgentRun( context: {} );

		        result = mw.wrapLLMCall(
		            context : { dataPacket: { model: "gpt-4", messages: [] } },
		            handler : function() {
		                return { id: "resp-1", choices: [ { message: { content: "Paris" } } ] };
		            }
		        );

		        callSucceeded = result.id == "resp-1";
		        tapeStillHasEntry = mw.getTape().interactions.len() == 1;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "callSucceeded" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "tapeStillHasEntry" ) ) ).isTrue();
	}

	// ---- Invalid mode ----

	@DisplayName( "invalid mode throws FlightRecorder.InvalidMode at init" )
	@Test
	public void testInvalidModeThrowsAtInit() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        threw = false;
		        try {
		            mw = new FlightRecorderMiddleware( mode: "bogus" );
		        } catch( e ) {
		            threw = e.type contains "InvalidMode";
		        }
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threw" ) ) ).isTrue();
	}

	// ---- Streaming transports (ctx.stream == true) ----

	@DisplayName( "record mode: a stream call returning a raw body is taped with a stream marker" )
	@Test
	public void testRecordStreamRawBody( @TempDir Path tempDir ) {
		String fixturePath = tempDir.resolve( "stream-raw.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "record", fixturePath: "%s" );
		        mw.beforeAgentRun( context: {} );

		        returned = mw.wrapLLMCall(
		            context : { stream: true, transport: "bedrock-event-stream", dataPacket: { model: "anthropic.claude" } },
		            handler : function() { return "RAW-EVENT-STREAM-BODY"; }
		        );

		        tape          = mw.getTape();
		        oneEntry      = tape.interactions.len() == 1;
		        markedStream  = tape.interactions[1].stream == true;
		        bodyRecorded  = tape.interactions[1].response == "RAW-EVENT-STREAM-BODY";
		        bodyReturned  = returned == "RAW-EVENT-STREAM-BODY";
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "oneEntry" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "markedStream" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bodyRecorded" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bodyReturned" ) ) ).isTrue();
	}

	@DisplayName( "replay mode: a recorded stream body is returned verbatim, not a chat struct" )
	@Test
	public void testReplayStreamRawBody( @TempDir Path tempDir ) throws IOException {
		String	fixturePath	= tempDir.resolve( "stream-replay.json" ).toString();
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "llm", "stream": true, "request": { "model": "anthropic.claude" }, "response": "RAW-EVENT-STREAM-BODY" }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "replay", fixturePath: "%s" );
		        mw.beforeAgentRun( context: {} );

		        handlerCalled = false;
		        result = mw.wrapLLMCall(
		            context: { stream: true, dataPacket: {} },
		            handler: function() { handlerCalled = true; return "LIVE"; }
		        );

		        handlerNotCalled = !handlerCalled;
		        gotRawBody       = isSimpleValue( result ) && result == "RAW-EVENT-STREAM-BODY";
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "handlerNotCalled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "gotRawBody" ) ) ).isTrue();
	}

	@DisplayName( "record/replay: a BINARY stream body is taped base64 and replayed as a byte-equal binary" )
	@Test
	public void testRecordAndReplayBinaryStreamBody( @TempDir Path tempDir ) {
		String fixturePath = tempDir.resolve( "stream-binary.json" ).toString().replace( "\\", "\\\\" );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        // Stand-in for Bedrock's binary event-stream body: a byte[], which fails isSimpleValue()
		        rawBody = toBinary( toBase64( "RAW-BINARY-EVENT-STREAM-BODY" ) );

		        rec = new FlightRecorderMiddleware( mode: "record", fixturePath: "%s" );
		        rec.beforeAgentRun( context: {} );

		        returned = rec.wrapLLMCall(
		            context : { stream: true, transport: "bedrock-event-stream", dataPacket: { model: "anthropic.claude" } },
		            handler : function() { return rawBody; }
		        );

		        tape         = rec.getTape();
		        oneEntry     = tape.interactions.len() == 1;
		        markedStream = tape.interactions[1].stream == true;
		        markedB64    = ( tape.interactions[1].encoding ?: "" ) == "base64";
		        b64Recorded  = tape.interactions[1].response == toBase64( rawBody );
		        binReturned  = isBinary( returned ) && toBase64( returned ) == toBase64( rawBody );

		        rec.afterAgentRun( context: {} );

		        // Replay from the fixture the recorder just wrote
		        rep = new FlightRecorderMiddleware( mode: "replay", fixturePath: "%s" );
		        rep.beforeAgentRun( context: {} );

		        handlerCalled = false;
		        replayed = rep.wrapLLMCall(
		            context: { stream: true, transport: "bedrock-event-stream", dataPacket: {} },
		            handler: function() { handlerCalled = true; return "LIVE"; }
		        );

		        noLiveCall     = !handlerCalled;
		        replayedBinary = isBinary( replayed );
		        byteEqual      = replayedBinary && toBase64( replayed ) == toBase64( rawBody );
		    """.formatted( fixturePath, fixturePath ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "oneEntry" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "markedStream" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "markedB64" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "b64Recorded" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "binReturned" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noLiveCall" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "replayedBinary" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "byteEqual" ) ) ).isTrue();
	}

	@DisplayName( "record mode: an emit-based stream call is passthrough with no fixture entry" )
	@Test
	public void testRecordStreamEmitBasedIsPassthrough( @TempDir Path tempDir ) {
		String fixturePath = tempDir.resolve( "stream-emit.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "record", fixturePath: "%s" );
		        mw.beforeAgentRun( context: {} );

		        emitted      = [];
		        handlerRan   = false;
		        mw.wrapLLMCall(
		            context : { stream: true, emitSSEChunk: ( c ) => { emitted.append( c ) }, dataPacket: {} },
		            handler : function() { handlerRan = true; return { done: true }; }
		        );

		        tape         = mw.getTape();
		        noEntry      = tape.interactions.len() == 0;
		        handlerCalled = handlerRan;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "noEntry" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "handlerCalled" ) ) ).isTrue();
	}

	@DisplayName( "replay mode + strict=false: a stream call with no recorded stream body passes through, never returns a struct" )
	@Test
	public void testReplayStreamFallsThroughToHandler( @TempDir Path tempDir ) throws IOException {
		// Tape holds only an ordinary chat interaction — handing that struct to a stream
		// transport is exactly the crash/silence this guard prevents.
		String	fixturePath	= tempDir.resolve( "stream-nomatch.json" ).toString();
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "llm", "request": {}, "response": { "id": "chat-resp" } }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "replay", strict: false, fixturePath: "%s" );
		        mw.beforeAgentRun( context: {} );

		        handlerCalled = false;
		        // No try/catch here on purpose: a ReplayExhausted/TypeMismatch throw would fail the
		        // test outright, which is exactly the assertion.
		        result = mw.wrapLLMCall(
		            context: { stream: true, dataPacket: {} },
		            handler: function() { handlerCalled = true; return "LIVE-STREAM"; }
		        );

		        passedThrough = handlerCalled && result == "LIVE-STREAM";
		        notAStruct    = !isStruct( result );
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "passedThrough" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "notAStruct" ) ) ).isTrue();
	}

	// ---- Stream/sync interaction discrimination ----

	@DisplayName( "replay mode + strict=false: the sync llm path SKIPS a recorded raw stream body" )
	@Test
	public void testSyncReplaySkipsStreamInteraction( @TempDir Path tempDir ) throws IOException {
		// A `stream: true` entry is a raw provider body. Handing it to the sync path (which expects
		// a parsed chat struct) yields garbage downstream, so it must never satisfy an llm request.
		String	fixturePath	= tempDir.resolve( "sync-skips-stream.json" ).toString();
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "llm", "stream": true, "transport": "bedrock-event-stream", "request": {}, "response": "RAW-EVENT-STREAM-BODY" },
		                          { "seq": 2, "type": "llm", "request": {}, "response": { "id": "chat-resp" } }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "replay", strict: false, fixturePath: "%s" );
		        mw.beforeAgentRun( context: {} );

		        handlerCalled = false;
		        result = mw.wrapLLMCall(
		            context: { dataPacket: {} },
		            handler: function() { handlerCalled = true; return { id: "LIVE" }; }
		        );

		        noLiveCall    = !handlerCalled;
		        gotChatStruct = isStruct( result ) && ( result.id ?: "" ) == "chat-resp";
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "noLiveCall" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "gotChatStruct" ) ) ).isTrue();
	}

	@DisplayName( "replay mode + strict=true: the sync llm path THROWS on a recorded raw stream body" )
	@Test
	public void testSyncReplayStrictRejectsStreamInteraction( @TempDir Path tempDir ) throws IOException {
		String	fixturePath	= tempDir.resolve( "sync-rejects-stream.json" ).toString();
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "llm", "stream": true, "request": {}, "response": "RAW-EVENT-STREAM-BODY" }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "replay", fixturePath: "%s" );
		        mw.beforeAgentRun( context: {} );
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		var thrown = assertThrows(
		    BoxRuntimeException.class,
		    () -> runtime.executeSource(
		        "mw.wrapLLMCall( context: { dataPacket: {} }, handler: function() { return { id: \"LIVE\" }; } )",
		        context
		    )
		);
		assertThat( thrown.getType() ).isEqualTo( "FlightRecorder.TypeMismatch" );
	}

	@DisplayName( "replay mode: a stream entry recorded on another transport is NOT consumed" )
	@Test
	public void testStreamReplayTransportMismatchDoesNotConsume( @TempDir Path tempDir ) throws IOException {
		// The Cohere/Bedrock case: an emit-based Cohere stream must not swallow Bedrock's raw
		// event-stream body just because both are `stream: true`.
		String	fixturePath	= tempDir.resolve( "stream-transport.json" ).toString();
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "llm", "stream": true, "transport": "bedrock-event-stream", "request": { "model": "anthropic.claude" }, "response": "RAW-EVENT-STREAM-BODY" }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "replay", strict: false, fixturePath: "%s" );
		        mw.beforeAgentRun( context: {} );

		        // Cohere-shaped call: no transport marker at all
		        cohereHandlerCalled = false;
		        cohereResult = mw.wrapLLMCall(
		            context: { stream: true, dataPacket: { model: "command-r" } },
		            handler: function() { cohereHandlerCalled = true; return "COHERE-LIVE"; }
		        );

		        cohereFellThrough = cohereHandlerCalled && cohereResult == "COHERE-LIVE";

		        // Same transport but a DIFFERENT request also misses
		        wrongRequestHandlerCalled = false;
		        mw.wrapLLMCall(
		            context: { stream: true, transport: "bedrock-event-stream", dataPacket: { model: "amazon.titan", messages: [ {}, {} ] } },
		            handler: function() { wrongRequestHandlerCalled = true; return "OTHER-LIVE"; }
		        );

		        wrongRequestFellThrough = wrongRequestHandlerCalled;

		        // The Bedrock entry was never consumed, so the matching call still replays it
		        bedrockHandlerCalled = false;
		        bedrockResult = mw.wrapLLMCall(
		            context: { stream: true, transport: "bedrock-event-stream", dataPacket: { model: "anthropic.claude" } },
		            handler: function() { bedrockHandlerCalled = true; return "BEDROCK-LIVE"; }
		        );

		        bedrockReplayed = !bedrockHandlerCalled && bedrockResult == "RAW-EVENT-STREAM-BODY";
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "cohereFellThrough" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "wrongRequestFellThrough" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bedrockReplayed" ) ) ).isTrue();
	}

	@DisplayName( "replay mode + strict=true: an unmatched stream call throws StreamReplayUnsupported" )
	@Test
	public void testStrictUnmatchedStreamThrows( @TempDir Path tempDir ) throws IOException {
		// A strict replay promises zero live calls — an emit-based transport (nothing taped) must
		// fail loudly rather than quietly hitting the network.
		String	fixturePath	= tempDir.resolve( "stream-strict-nomatch.json" ).toString();
		String	fixture		= """
		                      {
		                        "version": "1",
		                        "recordedAt": "2026-01-01T00:00:00",
		                        "agentName": "test-agent",
		                        "interactions": [
		                          { "seq": 1, "type": "llm", "request": {}, "response": { "id": "chat-resp" } }
		                        ]
		                      }
		                      """;
		Files.writeString( Path.of( fixturePath ), fixture );

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        mw = new FlightRecorderMiddleware( mode: "replay", fixturePath: "%s" );
		        mw.beforeAgentRun( context: {} );
		        liveCalled = false;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		var thrown = assertThrows(
		    BoxRuntimeException.class,
		    () -> runtime.executeSource(
		        "mw.wrapLLMCall( context: { stream: true, dataPacket: {} }, handler: function() { liveCalled = true; return \"LIVE\"; } )",
		        context
		    )
		);
		assertThat( thrown.getType() ).isEqualTo( "FlightRecorder.StreamReplayUnsupported" );

		runtime.executeSource( "noLiveCall = !liveCalled", context );
		assertThat( variables.getAsBoolean( Key.of( "noLiveCall" ) ) ).isTrue();
	}

	@DisplayName( "storageCallback receives an independent tape snapshot, unaffected by later appends" )
	@Test
	public void testStorageCallbackSnapshotIsIndependent( @TempDir Path tempDir ) {
		// A shallow .copy() shared the SAME interactions array, so the "snapshot" a callback
		// persisted kept growing behind its back.
		String fixturePath = tempDir.resolve( "callback-snapshot.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        snapshots = [];

		        mw = new FlightRecorderMiddleware(
		            mode           : "record",
		            fixturePath    : "%s",
		            storageCallback: function( tape, interaction ) { snapshots.append( tape ); }
		        );
		        mw.beforeAgentRun( context: {} );

		        mw.wrapLLMCall(
		            context : { dataPacket: { model: "gpt-4", messages: [] } },
		            handler : function() { return { id: "resp-1" }; }
		        );
		        mw.wrapLLMCall(
		            context : { dataPacket: { model: "gpt-4", messages: [] } },
		            handler : function() { return { id: "resp-2" }; }
		        );

		        twoSnapshots     = snapshots.len() == 2;
		        firstStillHasOne = snapshots[ 1 ].interactions.len() == 1;
		        secondHasTwo     = snapshots[ 2 ].interactions.len() == 2;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "twoSnapshots" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "firstStillHasOne" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "secondHasTwo" ) ) ).isTrue();
	}

	@DisplayName( "record mode: the taped request is frozen, so later mutation of the live messages array cannot break replay" )
	@Test
	public void testRecordedRequestIsSnapshotNotAlias( @TempDir Path tempDir ) {
		// Providers hand wrapLLMCall an ALIAS of the live messages array and the tool loop keeps
		// appending to it; _saveSnapshot() rewrites the whole tape on every interaction, so an
		// aliased request would fingerprint every turn with the FINAL message count and strict
		// stream replay would then reject turn 1.
		String fixturePath = tempDir.resolve( "aliased-request.json" ).toString();

		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.middleware.core.FlightRecorderMiddleware;

		        // ONE live array, mutated between the two turns — exactly what a tool loop does.
		        liveMessages = [ { role: "user", content: "hi" } ];
		        packet       = { model: "command-r", messages: liveMessages };

		        rec = new FlightRecorderMiddleware( mode: "record", fixturePath: "%s" );
		        rec.beforeAgentRun( context: {} );

		        rec.wrapLLMCall(
		            context : { stream: true, transport: "cohere-sse", dataPacket: packet },
		            handler : function() { return "BODY-TURN-1"; }
		        );

		        // The tool loop appends the assistant turn + the tool result to the SAME array
		        liveMessages.append( { role: "assistant", content: "calling tool" } );
		        liveMessages.append( { role: "tool", content: "tool output" } );

		        rec.wrapLLMCall(
		            context : { stream: true, transport: "cohere-sse", dataPacket: packet },
		            handler : function() { return "BODY-TURN-2"; }
		        );

		        // Replay with a FRESH strict instance reading the fixture off disk
		        play = new FlightRecorderMiddleware( mode: "replay", fixturePath: "%s" );
		        play.beforeAgentRun( context: {} );

		        liveCalls = 0;
		        turn1 = play.wrapLLMCall(
		            context : { stream: true, transport: "cohere-sse", dataPacket: { model: "command-r", messages: [ { role: "user", content: "hi" } ] } },
		            handler : function() { liveCalls++; return "LIVE"; }
		        );
		        turn2 = play.wrapLLMCall(
		            context : { stream: true, transport: "cohere-sse", dataPacket: { model: "command-r", messages: [ {}, {}, {} ] } },
		            handler : function() { liveCalls++; return "LIVE"; }
		        );

		        bothReplayed = turn1 == "BODY-TURN-1" && turn2 == "BODY-TURN-2" && liveCalls == 0;
		    """.formatted( fixturePath.replace( "\\", "\\\\" ), fixturePath.replace( "\\", "\\\\" ) ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "bothReplayed" ) ) ).isTrue();
	}

	// ---- Subclass overriding _saveSnapshot ----
	//
	// SKIPPED: item (e) — a test proving a subclass override of _saveSnapshot() is invoked.
	// This test file's existing patterns only ever construct FlightRecorderMiddleware directly
	// via `new FlightRecorderMiddleware( ... )` inside a runtime.executeSource() string; none of
	// them define an inline .bx subclass to extend it from Java-side test source. Doing so would
	// require inventing an untested inline-class-definition + instantiation idiom not used
	// anywhere else in this suite, so per the task instructions this case is skipped rather than
	// guessed at. _saveSnapshot() has been made `public` (see FlightRecorderMiddleware.bx) so a
	// real .bx subclass file can override it; that mechanism is exercised manually instead.
}
