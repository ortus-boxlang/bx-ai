package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "MockService Provider Tests" )
public class MockServiceTest extends BaseIntegrationTest {

	@DisplayName( "aiChat with per-request scripted responses" )
	@Test
	public void testAiChatScriptedResponse() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Hello", {}, {
		            provider       : "mock",
		            providerOptions: { responses: [ "Hi there!" ] }
		        } );
		        isScripted = result == "Hi there!";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isScripted" ) ) ).isTrue();
	}

	@DisplayName( "aiChat falls back to the default mock response" )
	@Test
	public void testDefaultResponse() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Hello", {}, { provider: "mock" } );
		        isDefault = result == "Mock response";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isDefault" ) ) ).isTrue();
	}

	@DisplayName( "raw return format yields the full OpenAI-shaped completion" )
	@Test
	public void testRawReturnFormat() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Hello", {}, {
		            provider       : "mock",
		            returnFormat   : "raw",
		            providerOptions: { responses: [ "Raw content" ] }
		        } );
		        hasChoices = result.keyExists( "choices" );
		        hasUsage   = result.keyExists( "usage" );
		        content    = result.choices.first().message.content == "Raw content";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasChoices" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasUsage" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "content" ) ) ).isTrue();
	}

	@DisplayName( "scripted tool-call turns drive the full tool-calling loop offline" )
	@Test
	public void testToolCallLoop() {
		// @formatter:off
		runtime.executeSource(
		    """
		        weatherTool = aiTool( "getWeather", "Get the weather for a city", ( required string city ) => {
		            return "Sunny in " & city;
		        } );

		        result = aiChat( "What is the weather in Miami?", { tools: [ weatherTool ] }, {
		            provider       : "mock",
		            providerOptions: {
		                responses: [
		                    { toolCalls: [ { name: "getWeather", arguments: { city: "Miami" } } ] },
		                    "The weather in Miami is sunny."
		                ]
		            }
		        } );
		        isAnswer = result == "The weather in Miami is sunny.";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isAnswer" ) ) ).isTrue();
	}

	@DisplayName( "json return format parses a clean JSON reply" )
	@Test
	public void testJsonReturnFormatCleanReply() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Hello", {}, {
		            provider       : "mock",
		            returnFormat   : "json",
		            providerOptions: { responses: [ '{"a":1}' ] }
		        } );
		        hasA = result.a == 1;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasA" ) ) ).isTrue();
	}

	@DisplayName( "json return format parses JSON with leading prose" )
	@Test
	public void testJsonReturnFormatLeadingProse() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Hello", {}, {
		            provider       : "mock",
		            returnFormat   : "json",
		            providerOptions: { responses: [ 'Here you go: {"a":1}' ] }
		        } );
		        hasA = result.a == 1;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasA" ) ) ).isTrue();
	}

	@DisplayName( "json return format parses JSON with trailing prose" )
	@Test
	public void testJsonReturnFormatTrailingProse() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Hello", {}, {
		            provider       : "mock",
		            returnFormat   : "json",
		            providerOptions: { responses: [ '{"a":1} Let me know if you need anything else!' ] }
		        } );
		        hasA = result.a == 1;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasA" ) ) ).isTrue();
	}

	@DisplayName( "json return format still parses a fenced ```json block" )
	@Test
	public void testJsonReturnFormatFencedBlock() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Hello", {}, {
		            provider       : "mock",
		            returnFormat   : "json",
		            providerOptions: { responses: [ '```json
		{"a":1}
		```' ] }
		        } );
		        hasA = result.a == 1;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasA" ) ) ).isTrue();
	}

	@DisplayName( "json return format degrades to an empty struct instead of throwing on garbage" )
	@Test
	public void testJsonReturnFormatGarbageDoesNotThrow() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Hello", {}, {
		            provider       : "mock",
		            returnFormat   : "json",
		            providerOptions: { responses: [ 'This is not JSON at all, sorry!' ] }
		        } );
		        isEmpty = isStruct( result ) && structIsEmpty( result );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
	}

	@DisplayName( "instance usage: setResponses queue + request recording" )
	@Test
	public void testInstanceQueueAndRecording() {
		// @formatter:off
		runtime.executeSource(
		    """
		        mock = aiService( "mock" ).setResponses( [ "first", "second" ] );

		        r1 = mock.chat( aiChatRequest( "One", {}, { returnFormat: "single" } ) );
		        r2 = mock.chat( aiChatRequest( "Two", {}, { returnFormat: "single" } ) );

		        ordered  = r1 == "first" && r2 == "second";
		        recorded = mock.getReceivedRequests().len() == 2;
		        sawFirst = mock.getReceivedRequests()[ 1 ].messages[ 1 ].content == "One";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "ordered" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "recorded" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawFirst" ) ) ).isTrue();
	}

	@DisplayName( "static shared log records requests across instances (aiChat path)" )
	@Test
	public void testStaticRecording() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.providers.MockService;

		        MockService::clearRecorded();

		        aiChat( "Recorded call", {}, {
		            provider       : "mock",
		            providerOptions: { responses: [ "ok" ] }
		        } );

		        recordedCount = MockService::getRecorded().len();
		        hasRecording  = recordedCount == 1;
		        sawContent    = MockService::getRecorded()[ 1 ].messages[ 1 ].content == "Recorded call";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasRecording" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawContent" ) ) ).isTrue();
	}

	@DisplayName( "recorded requests show the post-sanitization payload" )
	@Test
	public void testRecordingShowsSanitizedPayload() {
		// @formatter:off
		runtime.executeSource(
		    """
		        import bxModules.bxai.models.providers.MockService;

		        MockService::clearRecorded();

		        // Zero-width characters are stripped by default-on hygiene before the provider sees them
		        aiChat( "Hel" & char( 8203 ) & "lo", {}, {
		            provider       : "mock",
		            providerOptions: { responses: [ "ok" ] }
		        } );

		        sentContent = MockService::getRecorded()[ 1 ].messages[ 1 ].content;
		        isSanitized = sentContent == "Hello";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isSanitized" ) ) ).isTrue();
	}

}
