/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

/**
 * Live matrix for the Converse / ConverseStream path across model families, plus the
 * InvokeModel opt-out. Same credential + skip contract as {@link BedrockTest}: skipped without
 * AWS credentials, skipped on environment errors (no entitlement, expired token, throttling),
 * failed on anything that looks like a request-transform regression.
 *
 * Every case here is a paid call, so the class is excluded from the default <code>test</code>
 * task and runs only under the dedicated gradle task <code>testBedrockLive</code>.
 *
 * Cases, one per distinct request shape:
 *
 * <pre>
 * ConverseStream text + streaming tool loop (Claude)
 * OpenAI gpt-oss stream, and structured output via forced toolChoice (which also covers the
 *                            sync Converse leg for that family)
 * Amazon Nova chat, Mistral chat
 * Claude extended thinking (additionalModelRequestFields) — also asserts normalized usage
 * CountTokens against the Converse body
 * InvokeModel opt-out (bedrockApi=invoke) for both chat and stream
 * </pre>
 *
 * Model ids other than the shared BEDROCK_MODEL are overridable from .env:
 *
 * <pre>
 * BEDROCK_OPENAI_MODEL=...   # default openai.gpt-oss-120b-1:0
 * BEDROCK_NOVA_MODEL=...     # default amazon.nova-micro-v1:0
 * BEDROCK_MISTRAL_MODEL=...  # default mistral.mistral-7b-instruct-v0:2
 * BEDROCK_COUNT_MODEL=...    # default anthropic.claude-haiku-4-5-20251001-v1:0 — CountTokens
 *                            # accepts only a bare Anthropic foundation-model id, not a
 *                            # `global.`/`eu.` inference profile and not other vendors (live, 2026-09)
 * </pre>
 */
public class BedrockConverseLiveTest extends BedrockLiveTestBase {

	private static final String	OPENAI_MODEL	= dotenv.get( "BEDROCK_OPENAI_MODEL", "openai.gpt-oss-120b-1:0" );
	private static final String	NOVA_MODEL		= dotenv.get( "BEDROCK_NOVA_MODEL", "amazon.nova-micro-v1:0" );
	private static final String	MISTRAL_MODEL	= dotenv.get( "BEDROCK_MISTRAL_MODEL", "mistral.mistral-7b-instruct-v0:2" );
	private static final String	COUNT_MODEL		= dotenv.get( "BEDROCK_COUNT_MODEL", "anthropic.claude-haiku-4-5-20251001-v1:0" );

	// ---------------------------------------------------------------- ConverseStream

	@DisplayName( "ConverseStream: text chunks arrive and concatenate (Claude)" )
	@Test
	public void testConverseStreamText() {
		// @formatter:off
		assumeLive( """
			chunks  = 0
			content = ""
			aiChatStream(
				"Reply with exactly the three words: alpha beta gamma",
				( chunk ) => {
					chunks++
					content &= ( chunk.choices ?: [] ).len() ? ( chunk.choices.first().delta.content ?: "" ) : ""
				},
				{ model: "%s", max_tokens: 50 }
			)
			println( "stream chunks=#chunks# content=#content#" )
			""".formatted( BEDROCK_MODEL ) );
		// @formatter:on
		assertThat( variables.getAsInteger( Key.of( "chunks" ) ) ).isGreaterThan( 1 );
		assertThat( str( "content" ).toLowerCase() ).contains( "alpha" );
		assertThat( str( "content" ).toLowerCase() ).contains( "gamma" );
	}

	@DisplayName( "ConverseStream: streaming tool loop runs the tool and streams the follow-up (Claude)" )
	@Test
	public void testConverseStreamTools() {
		// @formatter:off
		assumeLive( """
			calls   = []
			content = ""
			tool = aiTool(
				"get_weather",
				"Get current temperature for a given location.",
				location => {
					calls.append( location )
					if( location contains "Kansas City" ) return "85"
					if( location contains "San Salvador" ) return "90"
					return "unknown"
				}).describeLocation( "City and country e.g. Bogotá, Colombia" )

			aiChatStream(
				"How hot is it in Kansas City? What about San Salvador? Answer with only the name of the warmer city, nothing else.",
				( chunk ) => { content &= ( chunk.choices ?: [] ).len() ? ( chunk.choices.first().delta.content ?: "" ) : "" },
				{ model: "%s", tools: [ tool ] }
			)
			callCount = calls.len()
			println( "stream tools calls=#calls.toString()# content=#content#" )
			""".formatted( BEDROCK_MODEL ) );
		// @formatter:on
		assertThat( variables.getAsInteger( Key.of( "callCount" ) ) ).isAtLeast( 2 );
		assertThat( str( "content" ) ).contains( "San Salvador" );
	}

	// ---------------------------------------------------------------- OpenAI on Bedrock

	@DisplayName( "ConverseStream: OpenAI gpt-oss stream" )
	@Test
	public void testOpenAiStream() {
		// @formatter:off
		assumeLive( """
			chunks  = 0
			content = ""
			aiChatStream(
				"Reply with exactly the one word: pong",
				( chunk ) => { chunks++; content &= ( chunk.choices ?: [] ).len() ? ( chunk.choices.first().delta.content ?: "" ) : "" },
				{ model: "%s", max_tokens: 400 }
			)
			println( "gpt-oss stream chunks=#chunks# content=#content#" )
			""".formatted( OPENAI_MODEL ) );
		// @formatter:on
		assertThat( variables.getAsInteger( Key.of( "chunks" ) ) ).isGreaterThan( 0 );
		assertThat( str( "content" ).toLowerCase() ).contains( "pong" );
	}

	@DisplayName( "Converse: structured output via forced toolChoice on OpenAI gpt-oss" )
	@Test
	public void testOpenAiStructuredOutput() {
		// @formatter:off
		assumeLive( """
			result = aiChat(
				"John Doe is 30 years old and lives in Seattle. Extract his details.",
				{ model: "%s", max_tokens: 300 },
				{ schema: {
					"type": "object",
					"properties": {
						"name": { "type": "string" },
						"age":  { "type": "integer" },
						"city": { "type": "string" }
					},
					"required": [ "name", "age", "city" ]
				} }
			)
			isStruct = isStruct( result )
			name = result.name ?: ""
			age  = result.age  ?: 0
			city = result.city ?: ""
			println( "gpt-oss structured=#result.toString()#" )
			""".formatted( OPENAI_MODEL ) );
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
		assertThat( str( "name" ) ).contains( "John" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
		assertThat( str( "city" ) ).contains( "Seattle" );
	}

	// ---------------------------------------------------------------- Other families

	@DisplayName( "Converse: Amazon Nova chat" )
	@Test
	public void testNovaChat() {
		// @formatter:off
		assumeLive( """
			result = aiChat( "Reply with exactly the one word: pong", { model: "%s", max_tokens: 20 } )
			println( "nova chat=#result#" )
			""".formatted( NOVA_MODEL ) );
		// @formatter:on
		assertThat( str( "result" ).trim() ).isNotEmpty();
	}

	@DisplayName( "Converse: Mistral chat" )
	@Test
	public void testMistralChat() {
		// @formatter:off
		assumeLive( """
			result = aiChat( "Reply with exactly the one word: pong", { model: "%s", max_tokens: 20 } )
			println( "mistral chat=#result#" )
			""".formatted( MISTRAL_MODEL ) );
		// @formatter:on
		assertThat( str( "result" ).toLowerCase() ).contains( "pong" );
	}

	// ---------------------------------------------------------------- Converse extras

	@DisplayName( "Converse: Claude extended thinking via additionalModelRequestFields, reasoning normalized, usage carries prompt/completion/total tokens" )
	@Test
	public void testClaudeReasoning() {
		// @formatter:off
		assumeLive( """
			raw = aiChat(
				"What is 17 * 23? Think it through, then answer with only the number.",
				{ model: "%s", max_tokens: 2000, thinking: { type: "enabled", budget_tokens: 1024 } },
				{ returnFormat: "raw" }
			)
			msg        = ( raw.choices ?: [] ).len() ? raw.choices.first().message : {}
			reasoning  = msg.reasoning ?: ""
			content    = msg.content   ?: ""
			usage      = raw.usage ?: {}
			prompt     = usage.prompt_tokens     ?: 0
			completion = usage.completion_tokens ?: 0
			total      = usage.total_tokens      ?: 0
			println( "reasoning len=#len( reasoning )# content=#content# usage=#usage.toString()#" )
			""".formatted( BEDROCK_MODEL ) );
		// @formatter:on
		assertThat( str( "reasoning" ).length() ).isGreaterThan( 0 );
		assertThat( str( "content" ) ).contains( "391" );

		int	prompt		= variables.getAsInteger( Key.of( "prompt" ) );
		int	completion	= variables.getAsInteger( Key.of( "completion" ) );
		assertThat( prompt ).isGreaterThan( 0 );
		assertThat( completion ).isGreaterThan( 0 );
		assertThat( variables.getAsInteger( Key.of( "total" ) ) ).isEqualTo( prompt + completion );
	}

	@DisplayName( "CountTokens: counts the Converse body the request would send" )
	@Test
	public void testCountTokens() {
		// @formatter:off
		assumeLive( """
			svc     = aiService( "bedrock" )
			req     = aiChatRequest( aiMessage().user( "Count the tokens in this short sentence please." ), { model: "%s" } )
			count   = svc.countTokens( req )
			println( "countTokens=#count#" )
			""".formatted( COUNT_MODEL ) );
		// @formatter:on
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isGreaterThan( 3 );
	}

	// ---------------------------------------------------------------- tool_choice on Converse

	@DisplayName( "Converse: object tool_choice forces the named tool, and none sends no toolConfig (Claude)" )
	@Test
	public void testConverseObjectToolChoiceForcesNamedTool() {
		// Two plausible tools, one forced by the OpenAI-shaped object form
		// { type: "function", function: { name } } that converseToolChoice() maps onto Converse's
		// toolChoice.tool. A forced tool choice is re-sent on every interaction, so the tool loop
		// can only end by hitting maxInteractions — hence the try/catch and the low cap.
		// @formatter:off
		assumeLive( """
			weatherCalls         = 0
			timeCalls            = 0
			forcedHasToolConfig  = false
			forcedToolChoiceJson = ""
			forcedError          = ""

			weatherTool = aiTool( "get_weather", "Get the current temperature for a city.", city => {
				weatherCalls++
				return "18C"
			} ).describeCity( "City name e.g. Paris, France" )

			timeTool = aiTool( "get_time", "Get the current local time for a city.", city => {
				timeCalls++
				return "14:05"
			} ).describeCity( "City name e.g. Paris, France" )

			forcedRecorder = { "wrapLLMCall": ( ctx, handler ) => {
				if ( forcedToolChoiceJson == "" ) {
					forcedHasToolConfig  = structKeyExists( ctx.dataPacket, "toolConfig" )
					forcedToolChoiceJson = forcedHasToolConfig ? jsonSerialize( ctx.dataPacket.toolConfig.toolChoice ?: {} ) : ""
				}
				return handler()
			} }

			try {
				aiChat(
					"I am planning a call with someone in Paris. What should I know?",
					{
						model      : "%s",
						max_tokens : 200,
						tools      : [ weatherTool, timeTool ],
						tool_choice: { type: "function", function: { name: "get_time" } }
					},
					{ maxInteractions: 2, middleware: [ forcedRecorder ] }
				)
			} catch( any e ) {
				forcedError = e.type & " | " & e.message
			}
			forcedTimeCalls    = timeCalls
			forcedWeatherCalls = weatherCalls
			println( "forced toolChoice=#forcedToolChoiceJson# time=#forcedTimeCalls# weather=#forcedWeatherCalls# error=#forcedError#" )

			// ---- same request with tool_choice "none": no toolConfig at all, so no tool can run
			weatherCalls      = 0
			timeCalls         = 0
			noneHasToolConfig = true
			noneRecorder = { "wrapLLMCall": ( ctx, handler ) => {
				noneHasToolConfig = structKeyExists( ctx.dataPacket, "toolConfig" )
				return handler()
			} }
			noneResult = aiChat(
				"I am planning a call with someone in Paris. What should I know?",
				{
					model      : "%s",
					max_tokens : 200,
					tools      : [ weatherTool, timeTool ],
					tool_choice: "none"
				},
				{ maxInteractions: 2, middleware: [ noneRecorder ] }
			)
			noneTimeCalls    = timeCalls
			noneWeatherCalls = weatherCalls
			println( "none hasToolConfig=#noneHasToolConfig# time=#noneTimeCalls# weather=#noneWeatherCalls#" )
			""".formatted( BEDROCK_MODEL, BEDROCK_MODEL ) );
		// @formatter:on

		// The forced tool ran and the body carried toolChoice.tool. Note what Converse actually
		// does with toolChoice.tool (observed live, Claude haiku 4.5, 2026-09): it makes the named
		// tool REQUIRED, not EXCLUSIVE — Claude returned a get_time tool_use block on every turn
		// and also emitted a get_weather block alongside it. So the assertion is "the forced tool
		// ran at least as often as its sibling", not "the sibling never ran".
		assertThat( variables.getAsInteger( Key.of( "forcedTimeCalls" ) ) ).isAtLeast( 1 );
		assertThat( variables.getAsInteger( Key.of( "forcedTimeCalls" ) ) )
		    .isAtLeast( variables.getAsInteger( Key.of( "forcedWeatherCalls" ) ) );
		assertThat( variables.getAsBoolean( Key.of( "forcedHasToolConfig" ) ) ).isTrue();
		assertThat( str( "forcedToolChoiceJson" ) ).contains( "tool" );
		assertThat( str( "forcedToolChoiceJson" ) ).contains( "get_time" );

		// tool_choice "none" omits toolConfig entirely, so neither tool can be called.
		assertThat( variables.getAsBoolean( Key.of( "noneHasToolConfig" ) ) ).isFalse();
		assertThat( variables.getAsInteger( Key.of( "noneTimeCalls" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "noneWeatherCalls" ) ) ).isEqualTo( 0 );
	}

	// ---------------------------------------------------------------- stream call context

	@DisplayName( "ConverseStream: beforeLLMCall context identifies the stream operation (Claude)" )
	@Test
	public void testConverseStreamBeforeLLMCallContext() {
		// @formatter:off
		assumeLive( """
			import bxModules.bxai.models.middleware.AiMiddlewareResult;

			seenStream    = false
			seenTransport = ""
			seenModelId   = ""
			seenOperation = ""
			recorder = { "beforeLLMCall": ( ctx ) => {
				seenStream    = ctx.stream    ?: false
				seenTransport = ctx.transport ?: ""
				seenModelId   = ctx.modelId   ?: ""
				seenOperation = ctx.operation ?: ""
				return AiMiddlewareResult::continue()
			} }

			aiChatStream(
				"Reply with exactly the one word: pong",
				( chunk ) => {},
				{ model: "%s", max_tokens: 50 },
				{ middleware: [ recorder ] }
			)
			println( "stream ctx stream=#seenStream# transport=#seenTransport# modelId=#seenModelId# operation=#seenOperation#" )
			""".formatted( BEDROCK_MODEL ) );
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "seenStream" ) ) ).isTrue();
		assertThat( str( "seenTransport" ) ).isEqualTo( "bedrock-event-stream" );
		assertThat( str( "seenModelId" ).toLowerCase() ).contains( "claude" );
		assertThat( str( "seenOperation" ) ).isEqualTo( "converse-stream" );
	}

	// ---------------------------------------------------------------- InvokeModel opt-out

	@DisplayName( "InvokeModel opt-out: bedrockApi=invoke still chats and streams (Claude)" )
	@Test
	public void testInvokeOptOut() {
		// @formatter:off
		assumeLive( """
			result  = aiChat( "Reply with exactly the one word: pong", { model: "%s", max_tokens: 20 }, { providerOptions: { bedrockApi: "invoke" } } )
			content = ""
			aiChatStream(
				"Reply with exactly the one word: pong",
				( chunk ) => { content &= ( chunk.choices ?: [] ).len() ? ( chunk.choices.first().delta.content ?: "" ) : "" },
				{ model: "%s", max_tokens: 20 },
				{ providerOptions: { bedrockApi: "invoke" } }
			)
			println( "invoke chat=#result# stream=#content#" )
			""".formatted( BEDROCK_MODEL, BEDROCK_MODEL ) );
		// @formatter:on
		assertThat( str( "result" ).toLowerCase() ).contains( "pong" );
		assertThat( str( "content" ).toLowerCase() ).contains( "pong" );
	}

}
