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
