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

	@DisplayName( "formatToolsForClaude formats an MCPTool without throwing (bug repro)" )
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
}