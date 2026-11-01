/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for Claude AI provider
 */
public class ClaudeTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "CLAUDE_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "claude" );
	}

	@DisplayName( "Test Claude AI" )
	@Test
	public void testClaude() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result = aiChat( "what is boxlang?" )
			println( result )
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test Claude Tools" )
	@Test
	public void testClaudeTools() {
		moduleRecord.settings.put( "logResponseToConsole", false );
		moduleRecord.settings.put( "logRequestToConsole", false );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			tool = aiTool(
				"get_weather",
				"Get current temperature for a given location.",
				location => {
					if( location contains "Kansas City" ) {
						return "85"
					}

					if( location contains "San Salvador" ){
						return "90"
					}

					return "unknown";
				}).describeLocation( "City and country e.g. Bogotá, Colombia" )

			result = aiChat(
				messages = "How hot is it in Kansas City? What about San Salvador? Answer with only the name of the warmer city, nothing else.",
				params = {
					tools: [ tool ]
				},
				options = {
					logResponseToConsole: true
				} )
			println( result )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "San Salvador" );
	}

	@DisplayName( "Test JSON response" )
	@Test
	public void testJsonResponse() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result = aiChat(
				messages = "Return a JSON object with name 'BoxLang' and version '1.0'. Return ONLY valid JSON, nothing else.",
				options = {
					returnFormat: "json"
				}
			)
			println( result )
			""",
			context
		);
		// @formatter:on

		// Verify we got a struct back
		assertThat( variables.get( "result" ) ).isInstanceOf( ortus.boxlang.runtime.types.IStruct.class );
		var result = ( ortus.boxlang.runtime.types.IStruct ) variables.get( "result" );
		assertThat( result.containsKey( "name" ) || result.containsKey( "NAME" ) ).isTrue();
	}

	@DisplayName( "Test XML response" )
	@Test
	public void testXmlResponse() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result = aiChat(
				messages = "Return an XML document with a root element 'language' containing a child element 'name' with value 'BoxLang'. Return ONLY valid XML, nothing else.",
				options = {
					returnFormat: "xml"
				}
			)
			println( result )
			""",
			context
		);
		// @formatter:on

		// Verify we got an XML document back
		assertThat( variables.get( "result" ) ).isInstanceOf( ortus.boxlang.runtime.types.XML.class );
	}

	@DisplayName( "Test structured output response" )
	@Test
	public void testStructuredOutput() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			// Define structured schema using a struct
			languageSchema = {
				"name": "string",
				"version": "string",
				"type": "string"
			}

			result = aiChat(
				messages = "Tell me about BoxLang. It's a modern JVM language, version 1.0, and it's a dynamic language. Return ONLY valid JSON matching this schema: name, version, type.",
				options = {
					returnFormat: languageSchema
				}
			)
			println( result )
			""",
			context
		);
		// @formatter:on

		// Verify we got a struct back with expected properties
		assertThat( variables.get( "result" ) ).isInstanceOf( ortus.boxlang.runtime.types.IStruct.class );
		var result = ( ortus.boxlang.runtime.types.IStruct ) variables.get( "result" );
		assertThat( result.containsKey( "name" ) || result.containsKey( "NAME" ) ).isTrue();
		assertThat( result.containsKey( "version" ) || result.containsKey( "VERSION" ) ).isTrue();
	}

	@DisplayName( "Structured output forces a tool-use schema for Claude" )
	@Test
	public void testStructuredOutputInjectsForcedTool() {
		// Deterministic / credential-free: a beforeLLMCall middleware captures the request
		// packet and short-circuits before any HTTP call, so we can assert the forced
		// structured_output tool + tool_choice were injected.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService( "claude", { apiKey: "dummy-key" } )

				chatRequest = aiChatRequest(
					aiMessage().user( "Extract the person: John Doe, age 30" ),
					{ model: "claude-sonnet-4-5", max_tokens: 200 },
					{
						provider: "claude",
						schema: {
							"type": "object",
							"properties": {
								"name": { "type": "string" },
								"age":  { "type": "integer" }
							},
							"required": [ "name", "age" ]
						}
					}
				)

				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )

				provider.chat( chatRequest )

				hasTools       = captured.packet.keyExists( "tools" )
				toolCount      = captured.packet.tools.len()
				toolName       = captured.packet.tools[ 1 ].name
				hasInputSchema = captured.packet.tools[ 1 ].keyExists( "input_schema" )
				hasNameProp    = captured.packet.tools[ 1 ].input_schema.properties.keyExists( "name" )
				hasToolChoice  = captured.packet.keyExists( "tool_choice" )
				choiceType     = captured.packet.tool_choice.type
				choiceName     = captured.packet.tool_choice.name
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasTools" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "toolCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "structured_output" );
		assertThat( variables.getAsBoolean( Key.of( "hasInputSchema" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasNameProp" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasToolChoice" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "choiceType" ) ) ).isEqualTo( "tool" );
		assertThat( variables.get( Key.of( "choiceName" ) ) ).isEqualTo( "structured_output" );
	}

	@DisplayName( "formatToolsForClaude formats an MCPTool without throwing" )
	@Test
	public void testFormatToolsForClaudeWithMCPTool() {
		// Deterministic / credential-free: a beforeLLMCall middleware captures the request
		// packet and short-circuits before any HTTP call. MCPTool (unlike ClosureTool) does not
		// implement getArgumentsSchema(), which formatToolsForClaude() calls unconditionally;
		// BaseTool.onMissingMethod() then throws "MissingMethod".
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.tools.MCPTool;

				captured   = {}
				mockClient = new src.test.bx.mocks.MockMCPClient()
				remoteTool = new MCPTool( mockClient, {
					name: "remoteThing",
					description: "a remote thing",
					inputSchema: { type: "object", properties: {}, required: [] }
				} )

				provider = aiService( "claude", { apiKey: "dummy-key" } )

				chatRequest = aiChatRequest(
					aiMessage().user( "hello" ),
					{ model: "claude-sonnet-4-5", max_tokens: 50, tools: [ remoteTool ] },
					{ provider: "claude" }
				)

				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )

				provider.chat( chatRequest )

				toolName       = captured.packet.tools[ 1 ].name
				hasInputSchema = captured.packet.tools[ 1 ].keyExists( "input_schema" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "remoteThing" );
		assertThat( variables.getAsBoolean( Key.of( "hasInputSchema" ) ) ).isTrue();
	}

	@DisplayName( "Structured output extracts the forced tool_use input (canned response)" )
	@Test
	public void testStructuredOutputExtractsFromToolUse() {
		// Deterministic: a wrapLLMCall middleware returns a canned Anthropic response containing
		// the forced structured_output tool_use block, exercising the extraction +
		// populateStructuredOutput path with no HTTP.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "claude-sonnet-4-5" },
					{
						provider: "claude",
						schema: {
							"type": "object",
							"properties": { "name": { "type": "string" }, "age": { "type": "integer" } },
							"required": [ "name", "age" ]
						}
					}
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						return {
							"content": [ {
								"type":  "tool_use",
								"name":  "structured_output",
								"input": { "name": "John Doe", "age": 30 }
							} ],
							"stop_reason": "tool_use",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					}
				} )
				result   = provider.chat( chatRequest )
				isStruct = isStruct( result )
				name     = result.name
				age      = result.age
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "Structured output throws (not silent) when the forced tool block is absent" )
	@Test
	public void testStructuredOutputThrowsWhenToolAbsent() {
		// Deterministic: canned response is a text block (e.g. truncated at max_tokens), so the
		// forced structured_output block is missing. Must throw StructuredOutputError.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "claude-sonnet-4-5" },
					{
						provider: "claude",
						schema: {
							"type": "object",
							"properties": { "name": { "type": "string" } },
							"required": [ "name" ]
						}
					}
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						return {
							"content": [ { "type": "text", "text": "I cannot comply." } ],
							"stop_reason": "max_tokens",
							"usage": { "input_tokens": 5, "output_tokens": 3 }
						}
					}
				} )
				caughtType = ""
				caughtMsg  = ""
				try {
					provider.chat( chatRequest )
				} catch( any e ) {
					caughtType = e.type
					caughtMsg  = e.message
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "caughtType" ) ) ).isEqualTo( "StructuredOutputError" );
		assertThat( variables.get( Key.of( "caughtMsg" ) ).toString() ).contains( "truncated" );
	}

	@DisplayName( "Test streaming chat with Claude" )
	@Test
	public void testChatStream() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			chunks = []
			fullResponse = ""
			aiChatStream(
				"Count to 3",
				( chunk ) => {
					chunks.append( chunk )
					content = chunk.choices?.first()?.delta?.content ?: ""
					fullResponse &= content
				}
			)
			println( "Received " & chunks.len() & " chunks" )
			println( "Full response: " & fullResponse )
			""",
			context
		);
		// @formatter:on

		// Verify we received chunks
		assertThat( variables.get( "chunks" ) ).isNotNull();
		assertThat( variables.get( "fullResponse" ) ).isNotNull();
	}

	@DisplayName( "Test streaming with callback" )
	@Test
	public void testStreamingCallback() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			chunkCount = 0
			aiChatStream(
				"Say hello",
				( chunk ) => {
					chunkCount++
				},
				{},
				{ provider: "claude" }
			)
			println( "Total chunks received: " & chunkCount )
			""",
			context
		);
		// @formatter:on

		// Verify callback was invoked
		assertThat( variables.get( "chunkCount" ) ).isNotNull();
	}

	@DisplayName( "chatStream injects the forced structured_output tool into the stream packet" )
	@Test
	public void testStreamStructuredOutputInjectsForcedTool() {
		// Deterministic / credential-free: a beforeLLMCall middleware captures the streaming
		// request packet and short-circuits before any SSE/HTTP call.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService( "claude", { apiKey: "dummy-key" } )

				chatRequest = aiChatRequest(
					aiMessage().user( "Extract the person: John Doe, age 30" ),
					{ model: "claude-sonnet-4-5", max_tokens: 200 },
					{
						provider: "claude",
						schema: {
							"type": "object",
							"properties": {
								"name": { "type": "string" },
								"age":  { "type": "integer" }
							},
							"required": [ "name", "age" ]
						}
					}
				)

				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )

				provider.chatStream( chatRequest, ( chunk ) => {} )

				isStream       = captured.packet.stream
				toolCount      = captured.packet.tools.len()
				toolName       = captured.packet.tools[ 1 ].name
				hasNameProp    = captured.packet.tools[ 1 ].input_schema.properties.keyExists( "name" )
				choiceType     = captured.packet.tool_choice.type
				choiceName     = captured.packet.tool_choice.name
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isStream" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "toolCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "structured_output" );
		assertThat( variables.getAsBoolean( Key.of( "hasNameProp" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "choiceType" ) ) ).isEqualTo( "tool" );
		assertThat( variables.get( Key.of( "choiceName" ) ) ).isEqualTo( "structured_output" );
	}

	@DisplayName( "Streamed structured_output input_json_delta fragments populate the schema" )
	@Test
	public void testStreamStructuredOutputAccumulatesDeltas() {
		// Deterministic: drives the three public stream helpers with the exact Anthropic SSE
		// events (already deserialized, as the onChunk handler sees them). No HTTP.
		// A real tool_use block on a different index is interleaved to prove the accumulator
		// only claims its own block.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "claude-sonnet-4-5" },
					{
						provider: "claude",
						schema: {
							"type": "object",
							"properties": { "name": { "type": "string" }, "age": { "type": "integer" } },
							"required": [ "name", "age" ]
						}
					}
				)

				soState = provider.newStructuredOutputStreamState()
				events  = [
					{ "type": "message_start", "message": { "id": "msg_1", "model": "claude-sonnet-4-5" } },
					{ "type": "content_block_start", "index": 0,
					  "content_block": { "type": "tool_use", "id": "toolu_other", "name": "get_weather", "input": {} } },
					{ "type": "content_block_delta", "index": 0,
					  "delta": { "type": "input_json_delta", "partial_json": '{"city":"KC"}' } },
					{ "type": "content_block_start", "index": 1,
					  "content_block": { "type": "tool_use", "id": "toolu_1", "name": "structured_output", "input": {} } },
					{ "type": "content_block_delta", "index": 1,
					  "delta": { "type": "input_json_delta", "partial_json": '{"name": "Joh' } },
					{ "type": "content_block_delta", "index": 1,
					  "delta": { "type": "input_json_delta", "partial_json": 'n Doe", "ag' } },
					{ "type": "content_block_delta", "index": 1,
					  "delta": { "type": "input_json_delta", "partial_json": 'e": 30}' } },
					{ "type": "message_delta", "delta": { "stop_reason": "tool_use" } }
				]

				consumedCount = 0
				for( evt in events ){
					if( provider.accumulateStructuredOutputChunk( evt, soState ) ){
						consumedCount++
					}
				}

				result   = provider.finalizeStructuredOutputStream( soState, chatRequest, "tool_use" )
				isStruct = isStruct( result )
				name     = result.name
				age      = result.age
			""",
			context
		);
		// @formatter:on

		// 1 content_block_start + 3 input_json_delta for the structured block only —
		// the real get_weather tool block on index 0 is NOT consumed.
		assertThat( variables.getAsInteger( Key.of( "consumedCount" ) ) ).isEqualTo( 4 );
		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "A second structured_output block RESETS the accumulator instead of concatenating" )
	@Test
	public void testStreamStructuredOutputResetsBufferOnNewBlock() {
		// Two forced blocks in one stream (a retry/continuation): without a reset the second
		// block's fragments were appended to the first's, producing "{...}{...}" — never valid JSON.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "claude-sonnet-4-5" },
					{
						provider: "claude",
						schema: {
							"type": "object",
							"properties": { "name": { "type": "string" }, "age": { "type": "integer" } },
							"required": [ "name", "age" ]
						}
					}
				)

				soState = provider.newStructuredOutputStreamState()
				events  = [
					{ "type": "content_block_start", "index": 0,
					  "content_block": { "type": "tool_use", "id": "toolu_1", "name": "structured_output", "input": {} } },
					{ "type": "content_block_delta", "index": 0,
					  "delta": { "type": "input_json_delta", "partial_json": '{"name": "Stale", "age": 1}' } },
					{ "type": "content_block_start", "index": 1,
					  "content_block": { "type": "tool_use", "id": "toolu_2", "name": "structured_output", "input": {} } },
					{ "type": "content_block_delta", "index": 1,
					  "delta": { "type": "input_json_delta", "partial_json": '{"name": "John Doe", "age": 30}' } },
					{ "type": "message_delta", "delta": { "stop_reason": "tool_use" } }
				]

				for( evt in events ){
					provider.accumulateStructuredOutputChunk( evt, soState )
				}

				bufferIsSecondBlockOnly = soState.buffer == '{"name": "John Doe", "age": 30}'
				result = provider.finalizeStructuredOutputStream( soState, chatRequest, "tool_use" )
				name   = result.name
				age    = result.age
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "bufferIsSecondBlockOnly" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "A seed input on content_block_start MERGES with the streamed deltas" )
	@Test
	public void testStreamStructuredOutputMergesSeedAndBuffer() {
		// Anthropic normally sends an empty {} on content_block_start, but a non-empty seed plus
		// deltas is not a choice between the two — the buffer's keys win, the seed's survive.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "claude-sonnet-4-5" },
					{
						provider: "claude",
						schema: {
							"type": "object",
							"properties": { "name": { "type": "string" }, "age": { "type": "integer" } },
							"required": [ "name", "age" ]
						}
					}
				)

				soState = provider.newStructuredOutputStreamState()
				provider.accumulateStructuredOutputChunk(
					{ "type": "content_block_start", "index": 0,
					  "content_block": { "type": "tool_use", "id": "toolu_1", "name": "structured_output",
					                     "input": { "name": "John Doe", "age": 1 } } },
					soState
				)
				provider.accumulateStructuredOutputChunk(
					{ "type": "content_block_delta", "index": 0,
					  "delta": { "type": "input_json_delta", "partial_json": '{"age": 30}' } },
					soState
				)

				result     = provider.finalizeStructuredOutputStream( soState, chatRequest, "tool_use" )
				mergedName = result.name
				mergedAge  = result.age
			""",
			context
		);
		// @formatter:on

		// seed key survives, buffer key wins
		assertThat( variables.get( Key.of( "mergedName" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "mergedAge" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "A seed plus a CONTINUATION fragment that is not valid JSON alone is stitched together" )
	@Test
	public void testStreamStructuredOutputStitchesSeedContinuation() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "claude-sonnet-4-5" },
					{
						provider: "claude",
						schema: {
							"type": "object",
							"properties": { "name": { "type": "string" }, "age": { "type": "integer" } },
							"required": [ "name", "age" ]
						}
					}
				)

				soState = provider.newStructuredOutputStreamState()
				provider.accumulateStructuredOutputChunk(
					{ "type": "content_block_start", "index": 0,
					  "content_block": { "type": "tool_use", "id": "toolu_1", "name": "structured_output",
					                     "input": { "name": "John Doe" } } },
					soState
				)
				// Fragment alone is NOT valid JSON — it continues the seed's object
				provider.accumulateStructuredOutputChunk(
					{ "type": "content_block_delta", "index": 0,
					  "delta": { "type": "input_json_delta", "partial_json": '"age": 30}' } },
					soState
				)

				result       = provider.finalizeStructuredOutputStream( soState, chatRequest, "tool_use" )
				stitchedName = result.name
				stitchedAge  = result.age
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "stitchedName" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "stitchedAge" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "afterLLMCall middleware can rewrite the streamed structured output before it is emitted" )
	@Test
	public void testStreamStructuredOutputMiddlewareMutationIsHonoured() throws Exception {
		// The hook used to fire AFTER the userCallback had already been handed the original value,
		// so a guard that rewrote streamState.structuredOutput changed the return value only and
		// the stream still carried the unredacted original. A tiny local SSE server stands in for
		// Anthropic so the whole chatStream() path runs offline.
		String		sse		= String.join( "\n\n",
		    "data: {\"type\":\"message_start\",\"message\":{\"id\":\"msg_1\",\"model\":\"claude-sonnet-4-5\"}}",
		    "data: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"tool_use\",\"id\":\"toolu_1\",\"name\":\"structured_output\",\"input\":{}}}",
		    "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"name\\\": \\\"John Doe\\\", \\\"age\\\": 30}\"}}",
		    "data: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"tool_use\"}}",
		    "data: [DONE]",
		    "" );

		HttpServer	server	= HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
		server.createContext( "/v1/messages", exchange -> {
			byte[] body = sse.getBytes( StandardCharsets.UTF_8 );
			exchange.getResponseHeaders().set( "Content-Type", "text/event-stream" );
			exchange.sendResponseHeaders( 200, body.length );
			try ( OutputStream os = exchange.getResponseBody() ) {
				os.write( body );
			}
		} );
		server.start();

		try {
			String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/messages";
			// @formatter:off
			runtime.executeSource(
				"""
					provider = aiService( "claude", { apiKey: "dummy-key" } )
					provider.setChatURL( "%s" )

					chatRequest = aiChatRequest(
						aiMessage().user( "Extract: John Doe, age 30" ),
						{ model: "claude-sonnet-4-5", max_tokens: 200 },
						{
							provider: "claude",
							schema: {
								"type": "object",
								"properties": { "name": { "type": "string" }, "age": { "type": "integer" } },
								"required": [ "name", "age" ]
							}
						}
					)

					chatRequest.addMiddleware( {
						"afterLLMCall": ( ctx ) => {
							if( ctx.keyExists( "streamState" ) && isStruct( ctx.streamState ) && ctx.streamState.keyExists( "structuredOutput" ) ){
								ctx.streamState.structuredOutput = { "name": "REDACTED", "age": 99 }
							}
						}
					} )

					emitted = []
					returned = provider.chatStream( chatRequest, ( chunk ) => {
						if( isStruct( chunk ) && ( chunk.choices ?: [] ).len() && ( chunk.choices[ 1 ].delta ?: {} ).keyExists( "structured_output" ) ){
							emitted.append( chunk.choices[ 1 ].delta.structured_output )
						}
					} )

					emittedOnce     = emitted.len() == 1
					emittedName     = emittedOnce ? ( emitted[ 1 ].name ?: "" ) : ""
					returnedName    = isStruct( returned ) ? ( returned.name ?: "" ) : ""
				""".formatted( url ),
				context
			);
			// @formatter:on

			assertThat( variables.getAsBoolean( Key.of( "emittedOnce" ) ) ).isTrue();
			assertThat( variables.get( Key.of( "emittedName" ) ).toString() ).isEqualTo( "REDACTED" );
			assertThat( variables.get( Key.of( "returnedName" ) ).toString() ).isEqualTo( "REDACTED" );
		} finally {
			server.stop( 0 );
		}
	}

	@DisplayName( "Streamed structured output throws when the forced block never arrives" )
	@Test
	public void testStreamStructuredOutputThrowsWhenBlockAbsent() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe" ),
					{ model: "claude-sonnet-4-5" },
					{
						provider: "claude",
						schema: { "type": "object", "properties": { "name": { "type": "string" } }, "required": [ "name" ] }
					}
				)

				soState = provider.newStructuredOutputStreamState()
				// Only plain text streamed back, truncated at max_tokens
				provider.accumulateStructuredOutputChunk(
					{ "type": "content_block_delta", "index": 0, "delta": { "type": "text_delta", "text": "I cannot comply." } },
					soState
				)

				caughtType = ""
				caughtMsg  = ""
				try {
					provider.finalizeStructuredOutputStream( soState, chatRequest, "max_tokens" )
				} catch( any e ) {
					caughtType = e.type
					caughtMsg  = e.message
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "caughtType" ) ) ).isEqualTo( "StructuredOutputError" );
		assertThat( variables.get( Key.of( "caughtMsg" ) ).toString() ).contains( "truncated" );
	}
}
