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

	@DisplayName( "beforeToolCall rewriting ctx.toolArgs is honoured when the tool is invoked" )
	@Test
	public void testBeforeToolCallArgsRewriteIsHonoured() {
		// Pass 1 stores the (possibly rewritten) args on the batch entry; pass 2 used to invoke
		// with the RAW toolCall.input instead, silently discarding validation/coercion edits.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				seenCity = ""
				weather  = aiTool( "get_weather", "Get the weather", ( required string city ) => {
					seenCity = arguments.city
					return "sunny in " & arguments.city
				} )

				provider    = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "claude-sonnet-4-5", tools: [ weather ] },
					{ provider: "claude" }
				)

				llmCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						llmCalls++
						if( llmCalls == 1 ){
							return {
								"content": [ {
									"type":  "tool_use",
									"id":    "toolu_1",
									"name":  "get_weather",
									"input": { "city": "RAW-CITY" }
								} ],
								"stop_reason": "tool_use",
								"usage": { "input_tokens": 5, "output_tokens": 8 }
							}
						}
						return {
							"content": [ { "type": "text", "text": "Done." } ],
							"stop_reason": "end_turn",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					},
					"beforeToolCall": ( ctx ) => {
						ctx.toolArgs = { "city": "REWRITTEN-CITY" }
					}
				} )

				result = provider.chat( chatRequest )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "seenCity" ) ).toString() ).isEqualTo( "REWRITTEN-CITY" );
	}

	@DisplayName( "beforeToolCall patching ctx.toolCall.input (HITL edit shape) reaches the tool - chat()" )
	@Test
	public void testBeforeToolCallInputPatchIsHonoured() {
		// HumanInTheLoopMiddleware.applyEditedArguments() replaces the provider-shaped
		// ctx.toolCall.input rather than the normalized ctx.toolArgs, so honouring only
		// ctx.toolArgs dropped every human "edit" decision.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				seenCity = ""
				weather  = aiTool( "get_weather", "Get the weather", ( required string city ) => {
					seenCity = arguments.city
					return "sunny in " & arguments.city
				} )

				provider    = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "claude-sonnet-4-5", tools: [ weather ] },
					{ provider: "claude" }
				)

				llmCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						llmCalls++
						if( llmCalls == 1 ){
							return {
								"content": [ {
									"type":  "tool_use",
									"id":    "toolu_1",
									"name":  "get_weather",
									"input": { "city": "RAW-CITY" }
								} ],
								"stop_reason": "tool_use",
								"usage": { "input_tokens": 5, "output_tokens": 8 }
							}
						}
						return {
							"content": [ { "type": "text", "text": "Done." } ],
							"stop_reason": "end_turn",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					},
					"beforeToolCall": ( ctx ) => {
						ctx.toolCall.input = { "city": "EDITED-CITY" }
					}
				} )

				result = provider.chat( chatRequest )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "seenCity" ) ).toString() ).isEqualTo( "EDITED-CITY" );
	}

	@DisplayName( "A native-slot edit that changes only letter CASE is still honoured" )
	@Test
	public void testBeforeToolCallInputPatchDifferingOnlyByCaseIsHonoured() {
		// BaseService.resolveToolArgs() decides "did this move?" by comparing canonical JSON.
		// Compared with `==` that comparison is case-INSENSITIVE, so an edit whose only change is
		// the case of a value ("paris" -> "Paris", or a case-corrected enum/ID) read as no edit at
		// all and the tool ran with the pre-hook arguments.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				seenCity = ""
				weather  = aiTool( "get_weather", "Get the weather", ( required string city ) => {
					seenCity = arguments.city
					return "sunny in " & arguments.city
				} )

				provider    = aiService( "claude", { apiKey: "dummy-key" } )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "claude-sonnet-4-5", tools: [ weather ] },
					{ provider: "claude" }
				)

				llmCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						llmCalls++
						if( llmCalls == 1 ){
							return {
								"content": [ {
									"type":  "tool_use",
									"id":    "toolu_1",
									"name":  "get_weather",
									"input": { "city": "paris" }
								} ],
								"stop_reason": "tool_use",
								"usage": { "input_tokens": 5, "output_tokens": 8 }
							}
						}
						return {
							"content": [ { "type": "text", "text": "Done." } ],
							"stop_reason": "end_turn",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					},
					"beforeToolCall": ( ctx ) => {
						// Case-only correction on the PROVIDER-shaped slot, the shape a HITL
						// "edit" decision writes
						ctx.toolCall.input = { "city": "Paris" }
					}
				} )

				result = provider.chat( chatRequest )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "seenCity" ) ).toString() ).isEqualTo( "Paris" );
	}

	@DisplayName( "beforeToolCall patching ctx.toolCall.input (HITL edit shape) reaches the tool - chatStream()" )
	@Test
	public void testStreamBeforeToolCallInputPatchIsHonoured() throws Exception {
		// chatStream() has no wrapLLMCall seam around its SSE transport, so a local server stands
		// in for Anthropic and streams the tool_use turn; the follow-up turn goes through chat(),
		// which IS wrapLLMCall-wrapped.
		String		sse		= String.join( "\n\n",
		    "data: {\"type\":\"message_start\",\"message\":{\"id\":\"msg_1\",\"model\":\"claude-sonnet-4-5\"}}",
		    "data: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"tool_use\",\"id\":\"toolu_1\",\"name\":\"get_weather\",\"input\":{}}}",
		    "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"city\\\": \\\"RAW-CITY\\\"}\"}}",
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
					seenCity = ""
					weather  = aiTool( "get_weather", "Get the weather", ( required string city ) => {
						seenCity = arguments.city
						return "sunny in " & arguments.city
					} )

					provider = aiService( "claude", { apiKey: "dummy-key" } )
					provider.setChatURL( "%s" )

					chatRequest = aiChatRequest(
						aiMessage().user( "weather?" ),
						{ model: "claude-sonnet-4-5", tools: [ weather ] },
						{ provider: "claude" }
					)

					chatRequest.addMiddleware( {
						// Only the follow-up turn after the tool result is sent back is wrapped
						"wrapLLMCall": ( ctx, handler ) => {
							return {
								"content": [ { "type": "text", "text": "Done." } ],
								"stop_reason": "end_turn",
								"usage": { "input_tokens": 5, "output_tokens": 8 }
							}
						},
						"beforeToolCall": ( ctx ) => {
							ctx.toolCall.input = { "city": "EDITED-CITY" }
						}
					} )

					chunks = []
					provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )
				""".formatted( url ),
				context
			);
			// @formatter:on

			assertThat( variables.get( Key.of( "seenCity" ) ).toString() ).isEqualTo( "EDITED-CITY" );
		} finally {
			server.stop( 0 );
		}
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
}