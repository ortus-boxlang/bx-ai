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
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class BedrockServiceTest extends BaseIntegrationTest {

	// Dummy AWS credentials for tests that don't make real API calls
	private static final String	DUMMY_AWS_ACCESS_KEY_ID		= "AKIAIOSFODNN7EXAMPLE";
	private static final String	DUMMY_AWS_SECRET_ACCESS_KEY	= "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
	private static final String	DUMMY_AWS_REGION			= "us-east-1";

	private String				awsAccessKeyId;
	private String				awsSecretAccessKey;
	private String				awsSessionToken;
	private String				awsRegion;

	/** Prior values of the shared module settings this class overwrites, so they can be restored. */
	private boolean				hadPriorProvider;
	private Object				priorProvider;
	private boolean				hadPriorApiKey;
	private Object				priorApiKey;

	@BeforeEach
	public void beforeEach() {
		// Load AWS credentials from .env file (same pattern as other provider tests)
		awsAccessKeyId		= dotenv.get( "AWS_ACCESS_KEY_ID", "" );
		awsSecretAccessKey	= dotenv.get( "AWS_SECRET_ACCESS_KEY", "" );
		awsSessionToken		= dotenv.get( "AWS_SESSION_TOKEN", "" );
		awsRegion			= dotenv.get( "AWS_REGION", "us-east-1" );

		// moduleRecord.settings is static and shared with every other test class in this Gradle
		// worker. Overwriting provider/apiKey without restoring them leaked Bedrock's struct
		// credentials into whichever class ran next — MockService would then receive a credential
		// struct as its API key. Capture the prior values (including absence) for afterEach.
		hadPriorProvider	= moduleRecord.settings.containsKey( "provider" );
		priorProvider		= moduleRecord.settings.get( "provider" );
		hadPriorApiKey		= moduleRecord.settings.containsKey( "apiKey" );
		priorApiKey			= moduleRecord.settings.get( "apiKey" );

		// Configure module settings with AWS credentials as a struct (Bedrock uses struct-based apiKey)
		moduleRecord.settings.put( "provider", "bedrock" );
		Struct credentials = new Struct();
		credentials.put( "awsAccessKeyId", awsAccessKeyId );
		credentials.put( "awsSecretAccessKey", awsSecretAccessKey );
		credentials.put( "region", awsRegion );
		// Add session token if present (required for temporary credentials from SSO/STS)
		if ( !awsSessionToken.isEmpty() ) {
			credentials.put( "awsSessionToken", awsSessionToken );
		}
		moduleRecord.settings.put( "apiKey", credentials );
	}

	@AfterEach
	public void afterEach() {
		// Restore the shared settings this class overwrote, so later test classes in the same
		// worker don't inherit Bedrock's provider and credential struct.
		if ( hadPriorProvider ) {
			moduleRecord.settings.put( "provider", priorProvider );
		} else {
			moduleRecord.settings.remove( "provider" );
		}
		if ( hadPriorApiKey ) {
			moduleRecord.settings.put( "apiKey", priorApiKey );
		} else {
			moduleRecord.settings.remove( "apiKey" );
		}
	}

	private boolean hasAwsCredentials() {
		return !awsAccessKeyId.isEmpty() && !awsSecretAccessKey.isEmpty();
	}

	@Test
	@DisplayName( "Can instantiate Bedrock service via aiService BIF" )
	public void testInstantiateBedrock() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)
				serviceName = service.getName()
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "serviceName" ) ) ).isEqualTo( "Bedrock" );
	}

	@Test
	@DisplayName( "Bedrock service can be configured with AWS credentials" )
	public void testConfiguration() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "us-west-2",
						model: "anthropic.claude-3-sonnet-20240229-v1:0"
					}
				)

				hasName = !isNull( service.getName() )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasName" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Bedrock service can make real API call to Claude" )
	public void testRealClaudeCall() {
		assumeTrue( hasAwsCredentials(), "AWS credentials not configured in .env" );

		// @formatter:off
		assumeTrue( executeLiveBedrockCall(
			"""
				// aiChat signature: invoke(messages, params, options, headers)
				response = aiChat(
					aiMessage().user( "Say 'Bedrock test successful' and nothing else" ),
					{
						model: "%s",
						max_tokens: 100
					},
					{
						provider: "bedrock",
						returnFormat: "single"
					}
				)

				hasContent = !isNull( response )
			""".formatted( BEDROCK_MODEL ),
			context
		), "live Bedrock call timed out" );
		// @formatter:on

		assertThat( variables.get( Key.of( "hasContent" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Bedrock service loads credentials from environment" )
	public void testEnvironmentCredentials() {
		assumeTrue( hasAwsCredentials(), "AWS credentials not configured in .env" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				// Should load from module settings (configured in beforeEach from .env)
				service = aiService( "bedrock", {} )

				hasService = !isNull( service )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasService" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Bedrock service can be created with minimum configuration" )
	public void testMinimalConfiguration() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				isConfigured = !isNull( service )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isConfigured" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Structured output forces a tool-use schema for Claude on Bedrock" )
	public void testStructuredOutputInjectsForcedTool() {
		// Deterministic / credential-free: a beforeLLMCall middleware captures the request
		// packet and short-circuits before any signing or HTTP call, so we can assert the
		// forced structured_output tool + tool_choice were injected.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				chatRequest = aiChatRequest(
					aiMessage().user( "Extract the person: John Doe, age 30" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 200 },
					{
						provider: "bedrock",
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
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
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

	@Test
	@DisplayName( "formatToolsForClaude formats an MCPTool without throwing" )
	public void testFormatToolsForClaudeWithMCPTool() {
		// Deterministic / credential-free: a beforeLLMCall middleware captures the request
		// packet and short-circuits before any signing or HTTP call. MCPTool (unlike ClosureTool)
		// does not implement getArgumentsSchema(), which formatToolsForClaude() calls
		// unconditionally; BaseTool.onMissingMethod() then throws "MissingMethod".
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

				provider = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				chatRequest = aiChatRequest(
					aiMessage().user( "hello" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 50, tools: [ remoteTool ] },
					{ provider: "bedrock" }
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
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "remoteThing" );
		assertThat( variables.getAsBoolean( Key.of( "hasInputSchema" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Structured output extracts the forced tool_use input (canned response)" )
	public void testStructuredOutputExtractsFromToolUse() {
		// Deterministic: a wrapLLMCall middleware returns a canned Bedrock Claude response
		// containing the forced structured_output tool_use block, exercising the extraction
		// + populateStructuredOutput path with no HTTP.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{
						provider: "bedrock",
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
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@Test
	@DisplayName( "Structured output throws (not silent) when the forced tool block is absent" )
	public void testStructuredOutputThrowsWhenToolAbsent() {
		// Deterministic: canned response is a text block (e.g. truncated at max_tokens), so the
		// forced structured_output block is missing. Must throw StructuredOutputError, not feed
		// prose into the JSON populator.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{
						provider: "bedrock",
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
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "caughtType" ) ) ).isEqualTo( "StructuredOutputError" );
		assertThat( variables.get( Key.of( "caughtMsg" ) ).toString() ).contains( "truncated" );
	}

	@Test
	@DisplayName( "Claude transform passes caller params through instead of allow-listing" )
	public void testClaudeTransformPassesParamsThrough() {
		// Deterministic / credential-free: a beforeLLMCall middleware captures the request
		// packet and cancels before any signing or HTTP call, so we can inspect exactly what
		// transformRequestForClaude built.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				chatRequest = aiChatRequest(
					aiMessage().user( "Hello" ),
					{
						model: "anthropic.claude-3-sonnet-20240229-v1:0",
						temperature: 0.5,
						stop_sequences: [ "STOP" ],
						top_p: 0.9,
						top_k: 40,
						tool_choice: { type: "auto" },
						stream: true
					},
					{ provider: "bedrock" }
				)

				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )

				provider.chat( chatRequest )

				packet = captured.packet

				hasStopSequences  = packet.keyExists( "stop_sequences" )
				stopSequenceVal   = packet.stop_sequences[ 1 ]
				hasTopP           = packet.keyExists( "top_p" )
				topPVal           = packet.top_p
				hasTopK           = packet.keyExists( "top_k" )
				topKVal           = packet.top_k
				hasToolChoice     = packet.keyExists( "tool_choice" )
				toolChoiceType    = packet.tool_choice.type
				hasTemperature    = packet.keyExists( "temperature" )
				temperatureVal    = packet.temperature

				hasModel          = packet.keyExists( "model" )
				hasStream         = packet.keyExists( "stream" )

				anthropicVersion  = packet.anthropic_version
				maxTokens         = packet.max_tokens
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasStopSequences" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "stopSequenceVal" ) ) ).isEqualTo( "STOP" );
		assertThat( variables.getAsBoolean( Key.of( "hasTopP" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "topPVal" ) ).toString() ).isEqualTo( "0.9" );
		assertThat( variables.getAsBoolean( Key.of( "hasTopK" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "topKVal" ) ) ).isEqualTo( 40 );
		assertThat( variables.getAsBoolean( Key.of( "hasToolChoice" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "toolChoiceType" ) ) ).isEqualTo( "auto" );
		assertThat( variables.getAsBoolean( Key.of( "hasTemperature" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "temperatureVal" ) ).toString() ).isEqualTo( "0.5" );

		assertThat( variables.getAsBoolean( Key.of( "hasModel" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "hasStream" ) ) ).isFalse();

		assertThat( variables.get( Key.of( "anthropicVersion" ) ) ).isEqualTo( "bedrock-2023-05-31" );
		assertThat( variables.getAsInteger( Key.of( "maxTokens" ) ) ).isEqualTo( 4096 );
	}

	@Test
	@DisplayName( "Claude transform still formats tools and default max_tokens correctly" )
	public void testClaudeTransformKeepsToolsAndMaxTokensDefault() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				chatRequest = aiChatRequest(
					aiMessage().user( "What is the weather?" ),
					{
						model: "anthropic.claude-3-sonnet-20240229-v1:0",
						tools: [
							aiTool(
								"getWeather",
								"Get the weather for a location",
								location => "sunny"
							)
						]
					},
					{ provider: "bedrock" }
				)

				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )

				provider.chat( chatRequest )

				packet      = captured.packet
				maxTokens   = packet.max_tokens
				hasTools    = packet.keyExists( "tools" )
				toolName    = packet.tools[ 1 ].name
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "maxTokens" ) ) ).isEqualTo( 4096 );
		assertThat( variables.getAsBoolean( Key.of( "hasTools" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "getWeather" );
	}

	@Test
	@DisplayName( "Claude transform passes params.system through when no system message exists" )
	public void testClaudeTransformPassesParamsSystemThrough() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				chatRequest = aiChatRequest(
					aiMessage().user( "Hello" ),
					{
						model: "anthropic.claude-3-sonnet-20240229-v1:0",
						system: "You are a pirate."
					},
					{ provider: "bedrock" }
				)

				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )

				provider.chat( chatRequest )

				packet    = captured.packet
				hasSystem = packet.keyExists( "system" )
				systemVal = packet.system
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasSystem" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "systemVal" ) ) ).isEqualTo( "You are a pirate." );
	}

	@Test
	@DisplayName( "Claude transform: a real system message wins over params.system" )
	public void testClaudeTransformSystemMessageWinsOverParamsSystem() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				chatRequest = aiChatRequest(
					aiMessage().system( "You are a helpful assistant." ).user( "Hello" ),
					{
						model: "anthropic.claude-3-sonnet-20240229-v1:0",
						system: "You are a pirate."
					},
					{ provider: "bedrock" }
				)

				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )

				provider.chat( chatRequest )

				packet    = captured.packet
				hasSystem = packet.keyExists( "system" )
				systemVal = packet.system
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasSystem" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "systemVal" ) ) ).isEqualTo( "You are a helpful assistant." );
	}

	@Test
	@DisplayName( "AiChatRequest supports providerOptions for provider-specific settings" )
	public void testProviderOptions() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				// Test getProviderOption with default when no options set
				request = new src.main.bx.models.requests.AiChatRequest(
					aiMessage().user( "test" ),
					{ model: "test-model" },
					{}
				);
				defaultResult = request.getProviderOption( "someKey", "defaultValue" );

				// Test that providerOptions works when passed in constructor options
				requestWithOptions = new src.main.bx.models.requests.AiChatRequest(
					aiMessage().user( "test" ),
					{ model: "test-model" },
					{
						provider: "bedrock",
						providerOptions: {
							inferenceProfileArn: "arn:aws:test:123",
							customKey: "customValue"
						}
					}
				);
				profileArn = requestWithOptions.getProviderOption( "inferenceProfileArn" );
				customVal = requestWithOptions.getProviderOption( "customKey" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "defaultResult" ) ) ).isEqualTo( "defaultValue" );
		assertThat( variables.get( Key.of( "profileArn" ) ) ).isEqualTo( "arn:aws:test:123" );
		assertThat( variables.get( Key.of( "customVal" ) ) ).isEqualTo( "customValue" );
	}

	@Test
	@DisplayName( "Claude-on-Bedrock response transform joins all text content blocks, not just the first" )
	public void testClaudeContentJoinsMultipleTextBlocks() {
		// Deterministic: a wrapLLMCall middleware returns a canned Bedrock Claude response with
		// TWO separate "type":"text" content blocks (e.g. text interleaved around a thinking/
		// tool_use block). Bedrock previously read only content[1], silently dropping the rest.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Say two things" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						return {
							"content": [
								{ "type": "text", "text": "Hello" },
								{ "type": "text", "text": "World" }
							],
							"stop_reason": "end_turn",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					}
				} )
				result = provider.chat( chatRequest )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "result" ) ).toString() ).contains( "Hello" );
		assertThat( variables.get( Key.of( "result" ) ).toString() ).contains( "World" );
	}

	@Test
	@DisplayName( "Claude-on-Bedrock content join still allows tool_use extraction from the same content array" )
	public void testClaudeContentJoinDoesNotBreakToolUse() {
		// Regression guard for the item-6 fix: joining all "text" blocks must not interfere with
		// the separate tool_use extraction path, which filters the same result.content array.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{
						provider: "bedrock",
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
							"content": [
								{ "type": "text", "text": "Sure, here you go:" },
								{ "type": "tool_use", "name": "structured_output", "input": { "name": "John Doe", "age": 30 } }
							],
							"stop_reason": "tool_use",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					}
				} )
				result = provider.chat( chatRequest )
				name   = result.name
				age    = result.age
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@Test
	@DisplayName( "Claude-on-Bedrock surfaces extended-thinking blocks as message.reasoning, kept out of content" )
	public void testClaudeSyncReasoningSurfaces() {
		// Upstream normalized reasoning onto choices[].message.reasoning and patched Bedrock's
		// STREAM path, but the sync path filters content to type=="text" — thinking blocks were
		// dropped on the floor, so Bedrock alone reported reasoning when streaming and nothing
		// when not. normalizeReasoningMessage() cannot rescue this: no native `thinking` key
		// survives onto the message for it to map from.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Think, then answer" ),
					{ model: "anthropic.claude-3-5-sonnet-20241022-v2:0" },
					{ provider: "bedrock", returnFormat: "raw" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						return {
							"content": [
								{ "type": "thinking", "thinking": "step one" },
								{ "type": "text", "text": "Answer" }
							],
							"stop_reason": "end_turn",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					}
				} )
				result    = provider.chat( chatRequest )
				message   = result.choices[ 1 ].message
				reasoning = message.reasoning ?: ""
				content   = message.content ?: ""
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "reasoning" ) ).toString() ).isEqualTo( "step one" );
		// Reasoning must never be folded into the answer - it is not what the model said.
		assertThat( variables.get( Key.of( "content" ) ).toString() ).isEqualTo( "Answer" );
	}

	@Test
	@DisplayName( "Claude-on-Bedrock omits message.reasoning entirely when the model did not think" )
	public void testClaudeSyncReasoningAbsentOmitsKey() {
		// Absence is normal, never an error: the key must be missing, not present-but-empty, so
		// `message.reasoning ?: ""` degrades the same way it does on every other provider.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Just answer" ),
					{ model: "anthropic.claude-3-5-sonnet-20241022-v2:0" },
					{ provider: "bedrock", returnFormat: "raw" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						return {
							"content": [ { "type": "text", "text": "Answer" } ],
							"stop_reason": "end_turn",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					}
				} )
				result       = provider.chat( chatRequest )
				hasReasoning = result.choices[ 1 ].message.keyExists( "reasoning" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasReasoning" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "OpenAI-shaped Bedrock model's native reasoning_content is normalized onto message.reasoning" )
	public void testOpenAIShapedReasoningContentNormalized() {
		// transformResponseFromOpenAI() early-returns an already-OpenAI-shaped body verbatim, and
		// Bedrock overrides chat() so it never reaches BaseService.sendChatRequest() where the
		// normalization lives. DeepSeek-on-Bedrock spells it `reasoning_content`.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Think, then answer" ),
					{ model: "deepseek.r1-v1:0" },
					{ provider: "bedrock", returnFormat: "raw" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						return {
							"choices": [
								{
									"message": {
										"role": "assistant",
										"content": "Answer",
										"reasoning_content": "step one"
									},
									"finish_reason": "stop",
									"index": 0
								}
							],
							"usage": { "prompt_tokens": 5, "completion_tokens": 8, "total_tokens": 13 }
						}
					}
				} )
				result    = provider.chat( chatRequest )
				reasoning = result.choices[ 1 ].message.reasoning ?: ""
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "reasoning" ) ).toString() ).isEqualTo( "step one" );
	}

	@Test
	@DisplayName( "OutputGuard redacts a secret that appears ONLY in Claude-on-Bedrock reasoning" )
	public void testOutputGuardRedactsReasoningOnly() {
		// The answer is clean; the secret is confined to the thinking. Before reasoning was
		// surfaced this leaked untouched, and the guard's empty-content bail meant a thinking-only
		// turn was never even scanned.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				guard = new bxModules.bxai.models.middleware.security.OutputGuardMiddleware( action: "redact" )
				chatRequest = aiChatRequest(
					aiMessage().user( "Think, then answer" ),
					{ model: "anthropic.claude-3-5-sonnet-20241022-v2:0" },
					{ provider: "bedrock", returnFormat: "raw" }
				)
				chatRequest.addMiddleware( guard )
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						return {
							"content": [
								{ "type": "thinking", "thinking": "operator is leaked.person@example.com", "signature": "sig-abc" },
								{ "type": "text", "text": "All done" }
							],
							"stop_reason": "end_turn",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					}
				} )
				result    = provider.chat( chatRequest )
				reasoning = result.choices[ 1 ].message.reasoning ?: ""
				content   = result.choices[ 1 ].message.content ?: ""
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		String reasoning = variables.get( Key.of( "reasoning" ) ).toString();
		assertWithMessage( "reasoning must not carry the raw secret" ).that( reasoning ).doesNotContain( "leaked.person@example.com" );
		assertThat( reasoning ).isNotEmpty();
		// The clean answer is untouched.
		assertThat( variables.get( Key.of( "content" ) ).toString() ).isEqualTo( "All done" );
	}

	@Test
	@DisplayName( "OutputGuard redaction of reasoning leaves the native thinking block unmodified for the provider round trip" )
	public void testOutputGuardLeavesThinkingBlockIntact() {
		// Bedrock rejects a modified thinking block on the next tool-use turn ("thinking or
		// redacted_thinking blocks in the latest assistant message cannot be modified"), so the
		// scrub must land on the derived copy only. Captures the raw body the middleware saw.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				guard   = new bxModules.bxai.models.middleware.security.OutputGuardMiddleware( action: "redact" )
				rawBody = {
					"content": [
						{ "type": "thinking", "thinking": "operator is leaked.person@example.com", "signature": "sig-abc" },
						{ "type": "text", "text": "All done" }
					],
					"stop_reason": "end_turn",
					"usage": { "input_tokens": 5, "output_tokens": 8 }
				}
				chatRequest = aiChatRequest(
					aiMessage().user( "Think, then answer" ),
					{ model: "anthropic.claude-3-5-sonnet-20241022-v2:0" },
					{ provider: "bedrock", returnFormat: "raw" }
				)
				chatRequest.addMiddleware( guard )
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => rawBody } )
				result = provider.chat( chatRequest )

				thinkingBlock = rawBody.content[ 1 ]
				thinkingText  = thinkingBlock.thinking
				signature     = thinkingBlock.signature
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertWithMessage( "the native thinking block must go back to Bedrock byte-identical" )
		    .that( variables.get( Key.of( "thinkingText" ) ).toString() )
		    .contains( "leaked.person@example.com" );
		assertThat( variables.get( Key.of( "signature" ) ).toString() ).isEqualTo( "sig-abc" );
	}

	@Test
	@DisplayName( "OutputGuard action=block fires on a secret found only in reasoning" )
	public void testOutputGuardBlocksOnReasoningOnly() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				guard = new bxModules.bxai.models.middleware.security.OutputGuardMiddleware( action: "block" )
				chatRequest = aiChatRequest(
					aiMessage().user( "Think, then answer" ),
					{ model: "anthropic.claude-3-5-sonnet-20241022-v2:0" },
					{ provider: "bedrock", returnFormat: "raw" }
				)
				chatRequest.addMiddleware( guard )
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						return {
							"content": [
								{ "type": "thinking", "thinking": "operator is leaked.person@example.com", "signature": "sig-abc" },
								{ "type": "text", "text": "All done" }
							],
							"stop_reason": "end_turn",
							"usage": { "input_tokens": 5, "output_tokens": 8 }
						}
					}
				} )
				blocked = false
				try {
					provider.chat( chatRequest )
				} catch( any e ) {
					blocked = true
				}
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "blocked" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "configure() applies BaseService's module-settings merge (defaultParams + providers.Bedrock)" )
	public void testConfigureMergesModuleSettings() {
		// item-5 fix: struct-based configure() must call super.configure() so
		// settings.defaultParams and settings.providers.Bedrock.{params,options} are merged into
		// variables.params, the same as every other provider. Mutate the shared module settings
		// struct in place and restore it afterward so other provider tests aren't affected.
		IStruct	defaultParams	= ( IStruct ) moduleRecord.settings.get( "defaultParams" );
		IStruct	providers		= ( IStruct ) moduleRecord.settings.get( "providers" );

		defaultParams.put( "temperature", 0.42d );

		Struct bedrockParams = new Struct();
		bedrockParams.put( "max_tokens", 999 );
		Struct bedrockOptions = new Struct();
		bedrockOptions.put( "customBedrockOption", "yes" );
		Struct bedrockProviderSettings = new Struct();
		bedrockProviderSettings.put( "params", bedrockParams );
		bedrockProviderSettings.put( "options", bedrockOptions );
		providers.put( "Bedrock", bedrockProviderSettings );

		try {
			// @formatter:off
			executeWithTimeoutHandling(
				"""
					service = aiService(
						"bedrock",
						{
							awsAccessKeyId: "%s",
							awsSecretAccessKey: "%s",
							region: "%s"
						}
					)
					params         = service.getParams()
					hasTemperature = params.keyExists( "temperature" )
					temperature    = params.temperature
					maxTokens      = params.max_tokens
				""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
				context
			);
			// @formatter:on

			assertThat( variables.getAsBoolean( Key.of( "hasTemperature" ) ) ).isTrue();
			assertThat( variables.get( Key.of( "temperature" ) ) ).isEqualTo( 0.42d );
			assertThat( variables.getAsInteger( Key.of( "maxTokens" ) ) ).isEqualTo( 999 );
		} finally {
			defaultParams.remove( "temperature" );
			providers.remove( "Bedrock" );
		}
	}

	@Test
	@DisplayName( "Merged module-settings params reach the InvokeModel body, not just service.getParams()" )
	public void testMergedModuleSettingsParamsReachRequestBody() {
		// testConfigureMergesModuleSettings above proves the merge lands in variables.params, but
		// variables.params was only ever read for .model — every other provider pushes it into the
		// chat request via mergeServiceParams(), so settings.defaultParams / providers.Bedrock.params
		// silently never reached the wire. Capture the built packet to assert on what AWS would see.
		IStruct	defaultParams		= ( IStruct ) moduleRecord.settings.get( "defaultParams" );
		IStruct	providers			= ( IStruct ) moduleRecord.settings.get( "providers" );

		// moduleRecord.settings is static and shared by every test class in this fork, so capture
		// the prior values (including absence) and restore exactly, rather than removing keys.
		boolean	hadTemperature		= defaultParams.containsKey( "temperature" );
		Object	priorTemperature	= defaultParams.get( "temperature" );
		boolean	hadBedrock			= providers.containsKey( "Bedrock" );
		Object	priorBedrock		= providers.get( "Bedrock" );

		defaultParams.put( "temperature", 0.42d );

		Struct bedrockParams = new Struct();
		bedrockParams.put( "max_tokens", 999 );
		Struct bedrockProviderSettings = new Struct();
		bedrockProviderSettings.put( "params", bedrockParams );
		providers.put( "Bedrock", bedrockProviderSettings );

		try {
			// @formatter:off
			executeWithTimeoutHandling(
				"""
					captured = {}
					provider = aiService(
						"bedrock",
						{
							awsAccessKeyId: "%s",
							awsSecretAccessKey: "%s",
							region: "%s"
						}
					)

					// Note: no temperature / max_tokens on the request itself — they must arrive
					// purely from the merged module settings.
					chatRequest = aiChatRequest(
						aiMessage().user( "Hello" ),
						{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
						{ provider: "bedrock" }
					)

					chatRequest.addMiddleware( {
						"beforeLLMCall": ( ctx ) => {
							captured.packet = ctx.dataPacket
							return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
						}
					} )

					provider.chat( chatRequest )

					packet         = captured.packet
					hasTemperature = packet.keyExists( "temperature" )
					temperatureVal = hasTemperature ? packet.temperature : 0
					maxTokens      = packet.max_tokens

					// The resolved model must be reported back on the request; merging service
					// params must NOT overwrite it with the service's default Claude id.
					reportedModel = chatRequest.getModel()
				""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
				context
			);
			// @formatter:on

			assertThat( variables.getAsBoolean( Key.of( "hasTemperature" ) ) ).isTrue();
			assertThat( variables.getAsDouble( Key.of( "temperatureVal" ) ) ).isEqualTo( 0.42d );
			// providers.Bedrock.params.max_tokens must win over the hardcoded 4096 default
			assertThat( variables.getAsInteger( Key.of( "maxTokens" ) ) ).isEqualTo( 999 );
			assertThat( variables.get( Key.of( "reportedModel" ) ) ).isEqualTo( "anthropic.claude-3-sonnet-20240229-v1:0" );
		} finally {
			if ( hadTemperature ) {
				defaultParams.put( "temperature", priorTemperature );
			} else {
				defaultParams.remove( "temperature" );
			}
			if ( hadBedrock ) {
				providers.put( "Bedrock", priorBedrock );
			} else {
				providers.remove( "Bedrock" );
			}
		}
	}

	@Test
	@DisplayName( "Service default max_tokens is not imposed on families with a lower ceiling" )
	public void testServiceDefaultMaxTokensDoesNotOverrideFamilyFallback() {
		// DEFAULT_CHAT_PARAMS.max_tokens is Claude-shaped (4096). Merging it into every request
		// would override transformRequestForLlama's own 2048 fallback, and Bedrock's Meta Llama
		// max_gen_len ceiling is 2048 — so a blanket merge 400s every Llama call. Only a value
		// that differs from the built-in default counts as configured.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				chatRequest = aiChatRequest(
					aiMessage().user( "Hello" ),
					{ model: "meta.llama3-70b-instruct-v1:0" },
					{ provider: "bedrock" }
				)

				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )

				provider.chat( chatRequest )

				maxGenLen = captured.packet.max_gen_len
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "maxGenLen" ) ) ).isEqualTo( 2048 );
	}

	@Test
	@DisplayName( "Merged module-settings params reach the embeddings payload, not just chat" )
	public void testMergedModuleSettingsParamsReachEmbeddings() {
		// chat()/chatStream() merge configuredServiceParams(), but embeddings() did not — so a
		// module-configured input_type / dimensions / normalize never reached the InvokeModel body.
		IStruct	defaultParams	= ( IStruct ) moduleRecord.settings.get( "defaultParams" );
		IStruct	providers		= ( IStruct ) moduleRecord.settings.get( "providers" );

		boolean	hadBedrock		= providers.containsKey( "Bedrock" );
		Object	priorBedrock	= providers.get( "Bedrock" );

		Struct	bedrockParams	= new Struct();
		bedrockParams.put( "input_type", "classification" );
		Struct bedrockProviderSettings = new Struct();
		bedrockProviderSettings.put( "params", bedrockParams );
		providers.put( "Bedrock", bedrockProviderSettings );

		try {
			// @formatter:off
			executeWithTimeoutHandling(
				"""
					provider = aiService(
						"bedrock",
						{
							awsAccessKeyId: "%s",
							awsSecretAccessKey: "%s",
							region: "%s"
						}
					)

					embeddingRequest = new src.main.bx.models.requests.AiEmbeddingRequest(
						"hello",
						{ model: "cohere.embed-english-v3" }
					)

					// The HTTP call fails with dummy credentials; the merge must already have
					// happened by then, so inspect the request afterwards.
					try {
						provider.embeddings( embeddingRequest )
					} catch ( any e ) {
					}

					mergedParams  = embeddingRequest.getParams()
					hasInputType  = mergedParams.keyExists( "input_type" )
					inputTypeVal  = hasInputType ? mergedParams.input_type : ""
				""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
				context
			);
			// @formatter:on

			assertThat( variables.getAsBoolean( Key.of( "hasInputType" ) ) ).isTrue();
			assertThat( variables.get( Key.of( "inputTypeVal" ) ) ).isEqualTo( "classification" );
		} finally {
			if ( hadBedrock ) {
				providers.put( "Bedrock", priorBedrock );
			} else {
				providers.remove( "Bedrock" );
			}
		}
	}

	@Test
	@DisplayName( "settings.providers.Bedrock.options are honoured, not just params" )
	public void testConfigureHonoursMergedProviderOptions() {
		// super.configure() merges providers.Bedrock.options into variables.options, but Bedrock
		// then read region/credentials/baseURL/bearerToken out of the raw arguments.options, so
		// module-level provider options had no effect.
		IStruct	providers		= ( IStruct ) moduleRecord.settings.get( "providers" );
		boolean	hadBedrock		= providers.containsKey( "Bedrock" );
		Object	priorBedrock	= providers.get( "Bedrock" );

		Struct	bedrockOptions	= new Struct();
		bedrockOptions.put( "region", "eu-west-2" );
		bedrockOptions.put( "baseURL", "https://bedrock.internal.example" );
		Struct bedrockProviderSettings = new Struct();
		bedrockProviderSettings.put( "options", bedrockOptions );
		providers.put( "Bedrock", bedrockProviderSettings );

		try {
			// @formatter:off
			executeWithTimeoutHandling(
				"""
					// Deliberately supply NO region and NO baseURL at the call site — both must
					// come from the merged provider options.
					provider = aiService(
						"bedrock",
						{
							awsAccessKeyId: "%s",
							awsSecretAccessKey: "%s"
						}
					)

					resolvedEndpoint = provider.getBedrockEndpoint( "anthropic.claude-3-sonnet-20240229-v1:0" )
				""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY ),
				context
			);
			// @formatter:on

			assertThat( variables.get( Key.of( "resolvedEndpoint" ) ).toString() ).contains( "bedrock.internal.example" );
		} finally {
			if ( hadBedrock ) {
				providers.put( "Bedrock", priorBedrock );
			} else {
				providers.remove( "Bedrock" );
			}
		}
	}

	@Test
	@DisplayName( "Container credentials read AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE (EKS Pod Identity)" )
	public void testContainerAuthorizationTokenFile() throws java.io.IOException {
		// EKS Pod Identity supplies AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE rather than the inline
		// AWS_CONTAINER_AUTHORIZATION_TOKEN, and rotates the file's contents. Reading only the
		// inline variable means the Authorization header is omitted and the request is rejected.
		java.nio.file.Path tokenFile = java.nio.file.Files.createTempFile( "bxai-container-token", ".tmp" );
		java.nio.file.Files.writeString( tokenFile, "pod-identity-token-value" );

		System.setProperty( "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE", tokenFile.toAbsolutePath().toString() );
		try {
			// @formatter:off
			executeWithTimeoutHandling(
				"""
					provider = aiService(
						"bedrock",
						{
							awsAccessKeyId: "%s",
							awsSecretAccessKey: "%s",
							region: "%s"
						}
					)

					resolvedToken = provider.containerAuthorizationToken()
				""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
				context
			);
			// @formatter:on

			assertThat( variables.get( Key.of( "resolvedToken" ) ) ).isEqualTo( "pod-identity-token-value" );
		} finally {
			System.clearProperty( "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE" );
			java.nio.file.Files.deleteIfExists( tokenFile );
		}
	}

	@Test
	@DisplayName( "Cohere-on-Bedrock responses are transformed by their own shape, not Claude's" )
	public void testCohereResponseTransform() {
		// detectModelFamily() routes cohere.* to family "cohere", but the response switch fell
		// through to transformResponseFromClaude, which looks for a content[] array. Cohere returns
		// neither shape, so every Command R reply normalized to content = "" and the whole response
		// was silently dropped.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				// Command R / R+ chat shape. usage is the header backfill sendBedrockRequest
				// writes onto the body when Cohere omits it — it must be read back, not zeroed.
				commandR = provider.transformResponseFromModel(
					{
						"text": "Hello from Command R",
						"finish_reason": "COMPLETE",
						"usage": { "prompt_tokens": 812, "completion_tokens": 45, "total_tokens": 857 }
					},
					"cohere",
					"cohere.command-r-plus-v1:0"
				)
				commandRContent = commandR.choices[ 1 ].message.content
				commandRFinish  = commandR.choices[ 1 ].finish_reason
				commandRRole    = commandR.choices[ 1 ].message.role
				commandRPrompt  = commandR.usage.prompt_tokens
				commandRTotal   = commandR.usage.total_tokens

				// Legacy Command generate shape
				legacy = provider.transformResponseFromModel(
					{ "generations": [ { "text": "Hello from Command", "finish_reason": "MAX_TOKENS" } ] },
					"cohere",
					"cohere.command-text-v14"
				)
				legacyContent = legacy.choices[ 1 ].message.content
				legacyFinish  = legacy.choices[ 1 ].finish_reason

				// An empty finish_reason must fall back to "stop", not pass "" through
				emptyFinish = provider.transformResponseFromModel(
					{ "text": "hi", "finish_reason": "" },
					"cohere",
					"cohere.command-r-v1:0"
				).choices[ 1 ].finish_reason

				// Only ERROR_TOXIC is a content-filter signal; a bare ERROR is a generic failure
				// and must not tell callers their content was moderated.
				toxicFinish = provider.transformResponseFromModel(
					{ "text": "", "finish_reason": "ERROR_TOXIC" },
					"cohere",
					"cohere.command-r-v1:0"
				).choices[ 1 ].finish_reason

				// error_limit is a context-length outcome, not a moderation one
				limitFinish = provider.transformResponseFromModel(
					{ "text": "", "finish_reason": "error_limit" },
					"cohere",
					"cohere.command-r-v1:0"
				).choices[ 1 ].finish_reason

				// user_cancel has no OpenAI equivalent — normalizes to stop
				cancelFinish = provider.transformResponseFromModel(
					{ "text": "partial", "finish_reason": "user_cancel" },
					"cohere",
					"cohere.command-r-v1:0"
				).choices[ 1 ].finish_reason

				// finish_reason "error" is a provider failure and must NOT normalize to a
				// successful empty completion — callers need to see it to retry or report.
				errorThrew = false
				try {
					provider.transformResponseFromModel(
						{ "text": "", "finish_reason": "error" },
						"cohere",
						"cohere.command-r-v1:0"
					)
				} catch ( ProviderError e ) {
					errorThrew = true
				}

				// Neither shape: must return empty content rather than throwing
				unmatched        = provider.transformResponseFromModel(
					{ "something_else": true },
					"cohere",
					"cohere.command-r-v1:0"
				)
				unmatchedContent = unmatched.choices[ 1 ].message.content
				unmatchedTokens  = unmatched.usage.total_tokens

				// Claude must be unaffected by the switch split
				claude = provider.transformResponseFromModel(
					{ "content": [ { "type": "text", "text": "Hello from Claude" } ] },
					"claude",
					"anthropic.claude-3-sonnet-20240229-v1:0"
				)
				claudeContent = claude.choices[ 1 ].message.content
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "commandRContent" ) ) ).isEqualTo( "Hello from Command R" );
		// Cohere's vocabulary is mapped onto OpenAI's, which the struct claims to speak
		assertThat( variables.get( Key.of( "commandRFinish" ) ) ).isEqualTo( "stop" );
		assertThat( variables.get( Key.of( "commandRRole" ) ) ).isEqualTo( "assistant" );
		assertThat( variables.getAsInteger( Key.of( "commandRPrompt" ) ) ).isEqualTo( 812 );
		assertThat( variables.getAsInteger( Key.of( "commandRTotal" ) ) ).isEqualTo( 857 );
		assertThat( variables.get( Key.of( "legacyContent" ) ) ).isEqualTo( "Hello from Command" );
		assertThat( variables.get( Key.of( "legacyFinish" ) ) ).isEqualTo( "length" );
		assertThat( variables.get( Key.of( "emptyFinish" ) ) ).isEqualTo( "stop" );
		assertThat( variables.get( Key.of( "toxicFinish" ) ) ).isEqualTo( "content_filter" );
		assertThat( variables.get( Key.of( "limitFinish" ) ) ).isEqualTo( "length" );
		assertThat( variables.get( Key.of( "cancelFinish" ) ) ).isEqualTo( "stop" );
		// a provider failure must surface, not normalize to a successful empty completion
		assertThat( variables.getAsBoolean( Key.of( "errorThrew" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "unmatchedContent" ) ) ).isEqualTo( "" );
		assertThat( variables.getAsInteger( Key.of( "unmatchedTokens" ) ) ).isEqualTo( 0 );
		assertThat( variables.get( Key.of( "claudeContent" ) ) ).isEqualTo( "Hello from Claude" );
	}

	@Test
	@DisplayName( "OutputGuard can resolve and redact Bedrock's native Titan/Llama/Mistral bodies in place" )
	public void testPromptSecurityResolvesBedrockNativeShapes() {
		// afterLLMCall hands middleware the raw provider body (as Claude/Cohere/Gemini do), but
		// PromptSecurity only knew the OpenAI/Claude/Gemini/Cohere shapes — so it resolved "" for
		// Bedrock's titan/llama/mistral bodies and OutputGuardMiddleware silently no-op'd,
		// action:"block" included. The resolver now knows those shapes; redaction must also write
		// back IN PLACE, because the Claude tool-call path reuses that same struct as the assistant
		// turn appended to message history.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				titan    = { "results": [ { "outputText": "titan secret" } ] }
				llama    = { "generation": "llama secret" }
				mistral  = { "outputs": [ { "text": "mistral secret" } ] }
				cohereLg = { "generations": [ { "text": "cohere secret" } ] }

				titanRead    = src.main.bx.models.security.PromptSecurity::getResponseText( { "result": titan } )
				llamaRead    = src.main.bx.models.security.PromptSecurity::getResponseText( { "result": llama } )
				mistralRead  = src.main.bx.models.security.PromptSecurity::getResponseText( { "result": mistral } )
				cohereLgRead = src.main.bx.models.security.PromptSecurity::getResponseText( { "result": cohereLg } )

				titanWrote   = src.main.bx.models.security.PromptSecurity::setResponseText( { "result": titan }, "[REDACTED]" )
				llamaWrote   = src.main.bx.models.security.PromptSecurity::setResponseText( { "result": llama }, "[REDACTED]" )
				mistralWrote = src.main.bx.models.security.PromptSecurity::setResponseText( { "result": mistral }, "[REDACTED]" )

				// in-place: the original structs must now carry the redacted text
				titanAfter   = titan.results[ 1 ].outputText
				llamaAfter   = llama.generation
				mistralAfter = mistral.outputs[ 1 ].text
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "titanRead" ) ) ).isEqualTo( "titan secret" );
		assertThat( variables.get( Key.of( "llamaRead" ) ) ).isEqualTo( "llama secret" );
		assertThat( variables.get( Key.of( "mistralRead" ) ) ).isEqualTo( "mistral secret" );
		assertThat( variables.get( Key.of( "cohereLgRead" ) ) ).isEqualTo( "cohere secret" );

		assertThat( variables.getAsBoolean( Key.of( "titanWrote" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "llamaWrote" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "mistralWrote" ) ) ).isTrue();

		assertThat( variables.get( Key.of( "titanAfter" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( variables.get( Key.of( "llamaAfter" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( variables.get( Key.of( "mistralAfter" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "OutputGuard sees and rewrites EVERY Claude text block, not just the first" )
	public void testPromptSecurityCoversAllClaudeTextBlocks() {
		// transformResponseFromClaude joins every text block into the returned content, but the
		// resolver used to read only the first — so a safe opening block followed by one carrying a
		// secret was returned completely unguarded. Redaction had the mirror bug: it rewrote block
		// one and left the secret in block two, which the transform then joined back in.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				// a tool_use block is interleaved deliberately: it must survive untouched
				claudeBody = {
					"content": [
						{ "type": "text",     "text": "Here you go." },
						{ "type": "tool_use", "id": "tu_1", "name": "lookup", "input": {} },
						{ "type": "text",     "text": "key is AKIAIOSFODNN7EXAMPLE" }
					]
				}

				// the guard must SEE the later block
				seenText = src.main.bx.models.security.PromptSecurity::getResponseText( { "result": claudeBody } )

				wrote = src.main.bx.models.security.PromptSecurity::setResponseText( { "result": claudeBody }, "[REDACTED]" )

				// and the rewrite must leave nothing unredacted behind for the transform to re-join
				remainingTextBlocks = claudeBody.content.filter( b -> ( b.type ?: "" ) == "text" )
				textBlockCount      = remainingTextBlocks.len()
				firstBlockText      = remainingTextBlocks[ 1 ].text
				joinedAfter         = src.main.bx.models.security.PromptSecurity::getResponseText( { "result": claudeBody } )
				toolBlockSurvived   = claudeBody.content.filter( b -> ( b.type ?: "" ) == "tool_use" ).len()
			""",
			context
		);
		// @formatter:on

		// the secret in block two must be visible to the guard
		assertThat( variables.get( Key.of( "seenText" ) ).toString() ).contains( "AKIAIOSFODNN7EXAMPLE" );
		assertThat( variables.get( Key.of( "seenText" ) ).toString() ).contains( "Here you go." );

		assertThat( variables.getAsBoolean( Key.of( "wrote" ) ) ).isTrue();
		// collapsed into a single text block carrying the redacted text
		assertThat( variables.getAsInteger( Key.of( "textBlockCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "firstBlockText" ) ) ).isEqualTo( "[REDACTED]" );
		// nothing unredacted survives anywhere in the struct
		assertThat( variables.get( Key.of( "joinedAfter" ) ).toString() ).doesNotContain( "AKIAIOSFODNN7EXAMPLE" );
		// the interleaved tool_use block is untouched
		assertThat( variables.getAsInteger( Key.of( "toolBlockSurvived" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "afterLLMCall receives the RAW Bedrock body, so in-place redaction reaches the tool-call history" )
	public void testAfterLLMCallReceivesRawBody() {
		// Regression guard. Firing this hook with the NORMALIZED response instead breaks
		// Claude-on-Bedrock: PromptSecurity mutates in place, and the tool-call path reuses the raw
		// body as the assistant turn appended to message history — so redacting a normalized copy
		// leaves the secret in what is re-sent to AWS. A wrapLLMCall middleware substitutes a
		// Titan body without touching the network.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				chatRequest = aiChatRequest(
					aiMessage().user( "Hello" ),
					{ model: "amazon.titan-text-express-v1" },
					{ provider: "bedrock" }
				)

				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, next ) => {
						return { "results": [ { "outputText": "titan secret", "completionReason": "FINISH" } ] }
					},
					"afterLLMCall": ( ctx ) => {
						captured.result = ctx.result
						// redact in place, exactly as OutputGuardMiddleware does
						src.main.bx.models.security.PromptSecurity::setResponseText( ctx, "[REDACTED]" )
					}
				} )

				response = provider.chat( chatRequest )

				seen          = captured.result
				sawRawShape   = seen.keyExists( "results" )
				sawNormalized = seen.keyExists( "choices" )
				// the redaction must have landed on the raw struct the provider still holds
				rawAfter      = seen.results[ 1 ].outputText
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawRawShape" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawNormalized" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "rawAfter" ) ) ).isEqualTo( "[REDACTED]" );
	}

	// -----------------------------------------------------------------------
	// Chunk 2 — item 2: routing/encoding, path construction
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "getBedrockPath/getBedrockStreamPath route through /model/{id}/invoke for a plain model ID, encoding its colon" )
	public void testPathForPlainModelId() {
		// item-2 fix: the only real Bedrock InvokeModel route is /model/{modelId}/invoke — there
		// is no "/application-inference-profile/..." route. The modelId is percent-encoded as a
		// single path segment (its literal ":" included), matching what AwsSignatureV4 expects
		// so the wire path and the signed canonical path agree.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				path       = service.getBedrockPath( "anthropic.claude-3-sonnet-20240229-v1:0" )
				streamPath = service.getBedrockStreamPath( "anthropic.claude-3-sonnet-20240229-v1:0" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "path" ) ).toString() )
		    .isEqualTo( "/model/anthropic.claude-3-sonnet-20240229-v1%3A0/invoke" );
		assertThat( variables.get( Key.of( "streamPath" ) ).toString() )
		    .isEqualTo( "/model/anthropic.claude-3-sonnet-20240229-v1%3A0/invoke-with-response-stream" );
	}

	@Test
	@DisplayName( "getBedrockPath routes an inference-profile ARN through /model/{arn}/invoke, not /application-inference-profile/..." )
	public void testPathForInferenceProfileArn() {
		// item-2 fix: previously this emitted "/application-inference-profile/{arn}/invoke",
		// a route that does not exist in the Bedrock API, with the ARN interpolated unencoded.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				arn  = "arn:aws:bedrock:us-east-1:123456789012:application-inference-profile/my-profile"
				path = service.getBedrockPath( arn )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		String path = variables.get( Key.of( "path" ) ).toString();
		assertWithMessage( "must not use the non-existent application-inference-profile route" )
		    .that( path ).doesNotContain( "application-inference-profile/arn" );
		assertThat( path ).startsWith( "/model/" );
		assertThat( path ).endsWith( "/invoke" );
		// ":" and the ARN's embedded "/" must both be percent-encoded as one opaque path segment
		assertThat( path ).contains( "arn%3Aaws%3Abedrock%3Aus-east-1%3A123456789012%3Aapplication-inference-profile%2Fmy-profile" );
	}

	@Test
	@DisplayName( "getBedrockPath's wire path, re-encoded by AwsSignatureV4's actual canonical-request computation, double-encodes as AWS's non-S3 rule requires" )
	public void testPathEncodingAgreesWithSigner() {
		// Test-honesty fix (review finding): the previous version of this test never touched the
		// signer at all — it only called encodeComponent() twice, which re-derives the same math
		// AwsSignatureV4 uses internally without exercising the signer itself, and so could never
		// catch a regression there. This version calls BedrockService.getBedrockPath() for the
		// real wire path, then AwsSignatureV4.computeCanonicalPath() — a small public seam that
		// runs the exact same private uriEncodePath() signRequest() calls internally — to get the
		// actual canonical-request path the signer would compute and sign. Confirms the documented
		// contract (see getBedrockPath()'s and AwsSignatureV4.encodeComponent()'s docblocks): the
		// wire path is single-encoded, the canonical (signed) path is double-encoded, and that
		// double-encoding is deliberate/correct (AWS's "double URI-encode except S3" rule,
		// matching botocore), not a bug.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				arn = "arn:aws:bedrock:us-east-1:123456789012:inference-profile/abc"
				wirePath      = service.getBedrockPath( arn )
				signer        = new src.main.bx.models.util.AwsSignatureV4()
				canonicalPath = signer.computeCanonicalPath( wirePath )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		String	wirePath		= variables.get( Key.of( "wirePath" ) ).toString();
		String	canonicalPath	= variables.get( Key.of( "canonicalPath" ) ).toString();

		assertWithMessage( "the wire path must carry a single-encoded ':'" )
		    .that( wirePath ).contains( "%3A" );
		assertWithMessage( "the wire path must not already be double-encoded" )
		    .that( wirePath ).doesNotContain( "%253A" );
		assertWithMessage( "the canonical request path (what AwsSignatureV4 actually signs) must be double-encoded" )
		    .that( canonicalPath ).contains( "%253A" );
	}

	// -----------------------------------------------------------------------
	// Chunk 2 — item 12: baseURL / endpoint override
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "baseURL config overrides the Bedrock host for signing and the request URL" )
	public void testBaseURLOverride() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s",
						baseURL: "http://localhost:4566"
					}
				)
				endpoint = service.getBedrockEndpoint( "anthropic.claude-3-sonnet-20240229-v1:0" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "endpoint" ) ).toString() )
		    .isEqualTo( "http://localhost:4566/model/anthropic.claude-3-sonnet-20240229-v1%3A0/invoke" );
	}

	@Test
	@DisplayName( "Without a baseURL override, the default regional AWS endpoint is used" )
	public void testDefaultEndpointUnchanged() {
		// Instantiate + configure() directly rather than via the aiService() BIF: the test
		// class's beforeEach() puts a credentials struct (with its own .env-derived region) on
		// moduleRecord.settings.apiKey, and aiService() merges that in ahead of configure()'s own
		// struct-vs-nested-apiKey precedence — irrelevant to what this test verifies (the default
		// host template), but it would shadow the region asserted on below.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				service.configure( { awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "us-west-2" } )
				endpoint = service.getBedrockEndpoint( "amazon.titan-text-express-v1" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "endpoint" ) ).toString() )
		    .isEqualTo( "https://bedrock-runtime.us-west-2.amazonaws.com/model/amazon.titan-text-express-v1/invoke" );
	}

	@Test
	@DisplayName( "baseURL with a path prefix is preserved in both the wire endpoint and getBedrockPath/getBedrockStreamPath, not silently dropped" )
	public void testBaseURLPathPrefix() {
		// Review finding: getBedrockHost()'s listFirst() only ever returned the host, silently
		// discarding any path prefix in baseURL (e.g. a proxy/gateway mounted under "/bedrock").
		// getBedrockPathPrefix() now recovers that prefix and getBedrockPath()/getBedrockStreamPath()
		// prepend it ahead of "/model/...", so both the wire URL and the path handed to
		// AwsSignatureV4.signRequest() for signing carry it.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s",
						baseURL: "https://proxy.internal/bedrock"
					}
				)
				endpoint       = service.getBedrockEndpoint( "anthropic.claude-3-sonnet-20240229-v1:0" )
				path           = service.getBedrockPath( "anthropic.claude-3-sonnet-20240229-v1:0" )
				streamEndpoint = service.getBedrockStreamEndpoint( "anthropic.claude-3-sonnet-20240229-v1:0" )
				streamPath     = service.getBedrockStreamPath( "anthropic.claude-3-sonnet-20240229-v1:0" )
				host           = service.getBedrockHost()
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "endpoint" ) ).toString() )
		    .isEqualTo( "https://proxy.internal/bedrock/model/anthropic.claude-3-sonnet-20240229-v1%3A0/invoke" );
		assertThat( variables.get( Key.of( "path" ) ).toString() )
		    .isEqualTo( "/bedrock/model/anthropic.claude-3-sonnet-20240229-v1%3A0/invoke" );
		assertThat( variables.get( Key.of( "streamEndpoint" ) ).toString() )
		    .isEqualTo( "https://proxy.internal/bedrock/model/anthropic.claude-3-sonnet-20240229-v1%3A0/invoke-with-response-stream" );
		assertThat( variables.get( Key.of( "streamPath" ) ).toString() )
		    .isEqualTo( "/bedrock/model/anthropic.claude-3-sonnet-20240229-v1%3A0/invoke-with-response-stream" );
		// The Host header / SigV4 host must stay just the host — the path prefix belongs on the
		// path, not folded into Host.
		assertThat( variables.get( Key.of( "host" ) ).toString() ).isEqualTo( "proxy.internal" );
	}

	// -----------------------------------------------------------------------
	// Chunk 2 — item 14: bearer / API-key auth
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "configure() with a plain string sets it as modelId (pre-#227 behavior restored), not apiKey" )
	public void testConfigureStringSetsModelId() {
		// Bearer auth is now explicit opt-in only (bearerToken / AWS_BEARER_TOKEN_BEDROCK) —
		// configure(string) keeps its original, non-breaking meaning: set modelId. See
		// BedrockService.configure()'s docblock for why Bedrock deliberately deviates from
		// BaseService's "string = apiKey" contract here.
		assumeTrue( !hasAmbientSetting( "AWS_BEARER_TOKEN_BEDROCK" ), "AWS_BEARER_TOKEN_BEDROCK is set in the ambient environment" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				service.configure( "anthropic.claude-3-sonnet-20240229-v1:0" )
				modelId    = service.getModelId()
				apiKeyEmpty = !len( service.getApiKey() )
				usesBearer  = service.useBearerAuth()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "modelId" ) ).toString() ).isEqualTo( "anthropic.claude-3-sonnet-20240229-v1:0" );
		assertThat( variables.getAsBoolean( Key.of( "apiKeyEmpty" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "usesBearer" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "configure() with a plain-string apiKey (module-settings footgun) does NOT select bearer auth without an explicit bearerToken" )
	public void testStringApiKeyAloneDoesNotUseBearerAuth() {
		// Regression test for the footgun this rework closes: aiService()/aiChat() inject the
		// module-wide settings.apiKey (or a BEDROCK_API_KEY env var) into EVERY provider's options
		// as a plain string — including Bedrock's — so a caller with (say) a global OpenAI key and
		// no AWS credentials configured must NOT have that string silently selected as a Bedrock
		// bearer token. Bearer auth only activates via the explicit bearerToken key or the
		// AWS_BEARER_TOKEN_BEDROCK env var (guarded below).
		assumeTrue( !hasAmbientSetting( "AWS_BEARER_TOKEN_BEDROCK" ), "AWS_BEARER_TOKEN_BEDROCK is set in the ambient environment" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				service.configure( { apiKey: "sk-some-other-providers-global-key" } )
				usesBearer = service.useBearerAuth()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "usesBearer" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "useBearerAuth() is true when an explicit bearerToken is configured and no AWS access key is available" )
	public void testExplicitBearerTokenUsesBearerAuth() {
		assumeTrue( !hasAmbientSetting( "AWS_BEARER_TOKEN_BEDROCK" ), "AWS_BEARER_TOKEN_BEDROCK is set in the ambient environment" );
		// loadAwsCredentialsFromEnvironment() picks up an ambient AWS_ACCESS_KEY_ID, and useBearerAuth()
		// short-circuits to false on any access key — so this assertion only holds without one.
		assumeTrue( !hasAmbientSetting( "AWS_ACCESS_KEY_ID" ), "AWS_ACCESS_KEY_ID is set in the ambient environment" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				service.configure( { bearerToken: "my-bedrock-bearer-token" } )
				usesBearer  = service.useBearerAuth()
				bearerToken = service.getBearerToken()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "usesBearer" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "bearerToken" ) ).toString() ).isEqualTo( "my-bedrock-bearer-token" );
	}

	@Test
	@DisplayName( "useBearerAuth() is false for struct-based AWS credential configuration" )
	public void testStructConfigureDoesNotUseBearerAuth() {
		// Guard: when aiService() merges in moduleRecord.settings.apiKey (see beforeEach) ahead of
		// the explicit awsAccessKeyId passed below, variables.awsAccessKeyId can end up empty if
		// dotenv has no real AWS creds — at that point this assertion depends solely on the
		// ambient AWS_BEARER_TOKEN_BEDROCK env var being unset.
		assumeTrue( !hasAmbientSetting( "AWS_BEARER_TOKEN_BEDROCK" ), "AWS_BEARER_TOKEN_BEDROCK is set in the ambient environment" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				usesBearer = service.useBearerAuth()
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "usesBearer" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "useBearerAuth() is false for nested apiKey credentials struct (aiService flow), even though apiKey is set" )
	public void testNestedCredentialsStructDoesNotUseBearerAuth() {
		// Guards against a false positive: variables.apiKey holds the nested credentials STRUCT
		// in this flow (see beforeEach). apiKey is never consulted by useBearerAuth() any more
		// (bearer auth keys off bearerToken/AWS_BEARER_TOKEN_BEDROCK only), but this still confirms
		// the struct-credentials flow doesn't accidentally end up in bearer mode. When dotenv has
		// no real AWS creds, variables.awsAccessKeyId ends up empty too, so this assertion falls
		// through to depending on the ambient AWS_BEARER_TOKEN_BEDROCK env var being unset.
		assumeTrue( !hasAmbientSetting( "AWS_BEARER_TOKEN_BEDROCK" ), "AWS_BEARER_TOKEN_BEDROCK is set in the ambient environment" );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService( "bedrock", {} )
				usesBearer = service.useBearerAuth()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "usesBearer" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "useBearerAuth() is false when an explicit bearerToken AND struct AWS credentials are both configured (explicit AWS creds win)" )
	public void testExplicitAwsCredsWinOverBearerToken() {
		// The precedence guard kept from the original review finding: explicit AWS credentials
		// must win over bearer auth even when a bearerToken is ALSO supplied in the same
		// configure() call — cheap insurance, even though bearerToken is itself an explicit opt-in.
		// Must hold regardless of the ambient AWS_BEARER_TOKEN_BEDROCK env var, since
		// variables.awsAccessKeyId is non-empty here (short-circuits useBearerAuth() to false).
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				service.configure( {
					bearerToken: "some-bearer-token",
					awsAccessKeyId: "%s",
					awsSecretAccessKey: "%s",
					region: "%s"
				} )
				usesBearer = service.useBearerAuth()
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "usesBearer" ) ) ).isFalse();
	}

	// -----------------------------------------------------------------------
	// Chunk 2 — item 16 + item 15: header passthrough + Guardrails
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "buildProviderOptionHeaders passes through generic x-amzn-bedrock-* providerOptions keys" )
	public void testGenericHeaderPassthrough() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				headers = service.buildProviderOptionHeaders( {
					"x-amzn-bedrock-performanceconfig-latency": "optimized",
					"Service-Tier": "flex",
					unrelatedOption: "ignored"
				} )
				hasLatency     = headers.keyExists( "x-amzn-bedrock-performanceconfig-latency" )
				latencyVal     = headers[ "x-amzn-bedrock-performanceconfig-latency" ]
				hasServiceTier = headers.keyExists( "Service-Tier" )
				hasUnrelated   = headers.keyExists( "unrelatedOption" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasLatency" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "latencyVal" ) ) ).isEqualTo( "optimized" );
		assertWithMessage( "keys not shaped like x-amzn-bedrock-* must not pass through" )
		    .that( variables.getAsBoolean( Key.of( "hasServiceTier" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "hasUnrelated" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "buildProviderOptionHeaders honors the bedrockHeaders struct shorthand, verbatim" )
	public void testBedrockHeadersStructShorthand() {
		// Uses FULL X-Amzn-Bedrock-* header names, which is the contract. `bedrockHeaders` is
		// appended verbatim and nothing prefixes it, so an earlier version of this test asserting
		// bare `Service-Tier` / `Request-Metadata` keys passed while proving nothing — those would
		// have gone on the wire as header names Bedrock does not recognize.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				headers = service.buildProviderOptionHeaders( {
					bedrockHeaders: {
						"X-Amzn-Bedrock-Service-Tier": "flex",
						"X-Amzn-Bedrock-Request-Metadata": "project=demo"
					}
				} )
				serviceTier = headers[ "X-Amzn-Bedrock-Service-Tier" ]
				requestMeta = headers[ "X-Amzn-Bedrock-Request-Metadata" ]
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "serviceTier" ) ) ).isEqualTo( "flex" );
		assertThat( variables.get( Key.of( "requestMeta" ) ).toString() ).contains( "demo" );
	}

	@Test
	@DisplayName( "bedrockHeaders is appended verbatim: a bare suffix is NOT prefixed for the caller" )
	public void testBedrockHeadersAreNotPrefixed() {
		// Pins the contract the previous test used to blur, so nobody "fixes" this by silently
		// prefixing and changing what goes on the wire.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				headers = service.buildProviderOptionHeaders( {
					bedrockHeaders: { "Service-Tier": "flex" }
				} )
				kept     = headers.keyExists( "Service-Tier" )
				prefixed = headers.keyExists( "X-Amzn-Bedrock-Service-Tier" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "kept" ) ) ).isTrue();
		assertWithMessage( "bedrockHeaders keys are forwarded as-is; nothing adds the prefix" )
		    .that( variables.getAsBoolean( Key.of( "prefixed" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "buildProviderOptionHeaders maps guardrailIdentifier/Version/Trace to X-Amzn-Bedrock-Guardrail* headers" )
	public void testGuardrailHeaders() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				headers = service.buildProviderOptionHeaders( {
					guardrailIdentifier: "gr-abc123",
					guardrailVersion: "1",
					guardrailTrace: "ENABLED"
				} )
				identifier = headers[ "X-Amzn-Bedrock-GuardrailIdentifier" ]
				version    = headers[ "X-Amzn-Bedrock-GuardrailVersion" ]
				trace      = headers[ "X-Amzn-Bedrock-Trace" ]
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "identifier" ) ) ).isEqualTo( "gr-abc123" );
		assertThat( variables.get( Key.of( "version" ) ) ).isEqualTo( "1" );
		assertThat( variables.get( Key.of( "trace" ) ) ).isEqualTo( "ENABLED" );
	}

	@Test
	@DisplayName( "buildProviderOptionHeaders returns an empty struct when no relevant providerOptions are set" )
	public void testNoExtraHeadersWhenNoProviderOptions() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				headers   = service.buildProviderOptionHeaders( { inferenceProfileArn: "arn:aws:test:123" } )
				isEmpty   = structIsEmpty( headers )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
	}

	// -----------------------------------------------------------------------
	// Chunk 3 — item 8: loud UnsupportedProviderCapability for tools / structured
	// output on non-Claude model families
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "chat() throws UnsupportedProviderCapability when tools are supplied for a non-Claude model family" )
	public void testCapabilityGateThrowsForToolsOnNonClaudeFamily() {
		// Deterministic / credential-free: assertCapabilitySupported() throws before any request
		// packet is built or HTTP call attempted, so no middleware interception is needed here.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				tool = aiTool( "noop", "does nothing", () => "ok" )
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "amazon.titan-text-express-v1", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				caughtType = ""
				caughtMsg  = ""
				try {
					provider.chat( chatRequest )
				} catch( any e ) {
					caughtType = e.type
					caughtMsg  = e.message
				}
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "caughtType" ) ) ).isEqualTo( "UnsupportedProviderCapability" );
		assertThat( variables.get( Key.of( "caughtMsg" ) ).toString() ).contains( "titan" );
	}

	@Test
	@DisplayName( "chat() throws UnsupportedProviderCapability when schema-typed structured output is requested for a non-Claude model family" )
	public void testCapabilityGateThrowsForStructuredOutputOnNonClaudeFamily() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "amazon.titan-text-express-v1" },
					{
						provider: "bedrock",
						schema: {
							"type": "object",
							"properties": { "name": { "type": "string" } },
							"required": [ "name" ]
						}
					}
				)
				caughtType = ""
				caughtMsg  = ""
				try {
					provider.chat( chatRequest )
				} catch( any e ) {
					caughtType = e.type
					caughtMsg  = e.message
				}
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "caughtType" ) ) ).isEqualTo( "UnsupportedProviderCapability" );
		assertThat( variables.get( Key.of( "caughtMsg" ) ).toString() ).contains( "structured output" );
	}

	@Test
	@DisplayName( "chat() does not gate tools for the Claude model family" )
	public void testCapabilityGateAllowsClaudeWithTools() {
		// Deterministic: a beforeLLMCall middleware captures the packet and cancels before any
		// HTTP call, confirming Claude reaches request-building with tools intact (no throw).
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				tool = aiTool( "noop", "does nothing", () => "ok" )
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )
				provider.chat( chatRequest )
				hasTools = captured.packet.keyExists( "tools" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasTools" ) ) ).isTrue();
	}

	// -----------------------------------------------------------------------
	// Chunk 3 — item 9: flattenMessageContent array-content hardening
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "flattenMessageContent returns a plain string unchanged" )
	public void testFlattenMessageContentPassthroughString() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				flattened = service.flattenMessageContent( "hello world" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "flattened" ) ) ).isEqualTo( "hello world" );
	}

	@Test
	@DisplayName( "flattenMessageContent joins array text parts and skips non-text parts" )
	public void testFlattenMessageContentJoinsTextPartsSkipsNonText() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				flattened = service.flattenMessageContent( [
					{ "type": "text", "text": "Hello" },
					{ "type": "tool_use", "name": "x", "input": {} },
					{ "type": "text", "text": "World" }
				] )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "flattened" ) ).toString() ).isEqualTo( "Hello World" );
	}

	@Test
	@DisplayName( "flattenMessageContent returns an empty string when the array has no text parts" )
	public void testFlattenMessageContentAllNonTextReturnsEmpty() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				flattened = service.flattenMessageContent( [
					{ "type": "tool_use", "name": "x", "input": {} },
					{ "type": "image", "source": {} }
				] )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "flattened" ) ).toString() ).isEqualTo( "" );
	}

	// -----------------------------------------------------------------------
	// Chunk 3 — item 11: Cohere / Titan-v2 embeddings shape routing
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "detectEmbeddingModelFamily routes cohere.embed-*, titan-embed-text-v2*, and defaults to titan-v1" )
	public void testDetectEmbeddingModelFamily() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				cohereFamily  = service.detectEmbeddingModelFamily( "cohere.embed-english-v3" )
				titanV2Family = service.detectEmbeddingModelFamily( "amazon.titan-embed-text-v2:0" )
				titanV1Family = service.detectEmbeddingModelFamily( "amazon.titan-embed-text-v1" )
				defaultFamily = service.detectEmbeddingModelFamily( "some.other.embedding-model" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "cohereFamily" ) ) ).isEqualTo( "cohere" );
		assertThat( variables.get( Key.of( "titanV2Family" ) ) ).isEqualTo( "titan-v2" );
		assertThat( variables.get( Key.of( "titanV1Family" ) ) ).isEqualTo( "titan-v1" );
		assertThat( variables.get( Key.of( "defaultFamily" ) ) ).isEqualTo( "titan-v1" );
	}

	@Test
	@DisplayName( "buildCohereEmbeddingRequest builds { texts, input_type }, defaulting input_type to search_document" )
	public void testBuildCohereEmbeddingRequest() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				defaultPacket  = service.buildCohereEmbeddingRequest( [ "a", "b" ], {} )
				overridePacket = service.buildCohereEmbeddingRequest( [ "a" ], { input_type: "search_query" } )
				textCount      = defaultPacket.texts.len()
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "textCount" ) ) ).isEqualTo( 2 );
		@SuppressWarnings( "unchecked" )
		IStruct	defaultPacket	= ( IStruct ) variables.get( Key.of( "defaultPacket" ) );
		@SuppressWarnings( "unchecked" )
		IStruct	overridePacket	= ( IStruct ) variables.get( Key.of( "overridePacket" ) );
		assertThat( defaultPacket.get( Key.of( "input_type" ) ) ).isEqualTo( "search_document" );
		assertThat( overridePacket.get( Key.of( "input_type" ) ) ).isEqualTo( "search_query" );
	}

	@Test
	@DisplayName( "buildTitanEmbeddingRequest adds dimensions/normalize only for v2 when present in params" )
	public void testBuildTitanEmbeddingRequest() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				v1Packet          = service.buildTitanEmbeddingRequest( "hello", {}, false )
				v2PacketNoExtras  = service.buildTitanEmbeddingRequest( "hello", {}, true )
				v2PacketWithExtras= service.buildTitanEmbeddingRequest( "hello", { dimensions: 512, normalize: true }, true )

				v1HasDimensions   = v1Packet.keyExists( "dimensions" )
				v2NoExtrasHasDims = v2PacketNoExtras.keyExists( "dimensions" )
				v2Dimensions      = v2PacketWithExtras.dimensions
				v2Normalize       = v2PacketWithExtras.normalize
				v1InputText       = v1Packet.inputText
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "v1InputText" ) ) ).isEqualTo( "hello" );
		assertThat( variables.getAsBoolean( Key.of( "v1HasDimensions" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "v2NoExtrasHasDims" ) ) ).isFalse();
		assertThat( variables.getAsInteger( Key.of( "v2Dimensions" ) ) ).isEqualTo( 512 );
		assertThat( variables.getAsBoolean( Key.of( "v2Normalize" ) ) ).isTrue();
	}

	// -----------------------------------------------------------------------
	// Chunk 3 — item 1: AWS default credential chain (ECS/EKS + IMDSv2) + caching
	// -----------------------------------------------------------------------
	// Container/IMDS endpoints (169.254.170.2 / 169.254.169.254) are not reachable from the test
	// sandbox, so loadContainerCredentials()/loadImdsCredentials() themselves are not exercised
	// here. What IS covered deterministically: explicit-credential resolution priority, and the
	// pure 5-minute-refresh-buffer cache-validity check that governs when the chain re-resolves.

	@Test
	@DisplayName( "resolveAwsCredentials returns the explicit/struct-configured credentials" )
	public void testResolveAwsCredentialsReturnsExplicitCreds() {
		// Instantiate + configure() directly rather than via the aiService() BIF: per
		// testDefaultEndpointUnchanged's note, aiService() merges moduleRecord.settings.apiKey
		// (the dotenv-derived struct this test class's beforeEach() sets) ahead of configure()'s
		// own struct-vs-nested-apiKey precedence, which would shadow the explicit creds asserted
		// on below.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				service.configure( {
					awsAccessKeyId: "%s",
					awsSecretAccessKey: "%s",
					awsSessionToken: "test-session-token",
					region: "%s"
				} )
				creds = service.resolveAwsCredentials()
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		@SuppressWarnings( "unchecked" )
		IStruct creds = ( IStruct ) variables.get( Key.of( "creds" ) );
		assertThat( creds.get( Key.of( "accessKeyId" ) ) ).isEqualTo( DUMMY_AWS_ACCESS_KEY_ID );
		assertThat( creds.get( Key.of( "secretAccessKey" ) ) ).isEqualTo( DUMMY_AWS_SECRET_ACCESS_KEY );
		assertThat( creds.get( Key.of( "sessionToken" ) ) ).isEqualTo( "test-session-token" );
	}

	@Test
	@DisplayName( "Explicit credentials do not inherit an unrelated AWS_SESSION_TOKEN from the environment" )
	public void testExplicitCredentialsDoNotMixWithEnvironmentSessionToken() {
		// init() seeds the instance from the environment, so assigning configure()'s keys
		// individually let an explicit long-term key pair keep the environment's session token —
		// a token belonging to different credentials. SigV4 signed the mismatched triple and AWS
		// rejected it. Credentials must resolve as an atomic set.
		//
		// Simulated by pre-seeding the session token the way loadAwsCredentialsFromEnvironment()
		// would, then configuring an explicit long-term pair with no token of its own.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				service.configure( {
					awsAccessKeyId: "AKIAENVENVENVENVENVE",
					awsSecretAccessKey: "env-secret",
					awsSessionToken: "env-session-token",
					region: "%s"
				} )
				// A later explicit configure() with long-term credentials and NO session token
				service.configure( {
					awsAccessKeyId: "%s",
					awsSecretAccessKey: "%s",
					region: "%s"
				} )
				creds = service.resolveAwsCredentials()
			""".formatted( DUMMY_AWS_REGION, DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		@SuppressWarnings( "unchecked" )
		IStruct creds = ( IStruct ) variables.get( Key.of( "creds" ) );
		assertThat( creds.get( Key.of( "accessKeyId" ) ) ).isEqualTo( DUMMY_AWS_ACCESS_KEY_ID );
		assertThat( creds.get( Key.of( "secretAccessKey" ) ) ).isEqualTo( DUMMY_AWS_SECRET_ACCESS_KEY );
		assertWithMessage( "an explicit key pair must not be signed with a session token from another credential source" )
		    .that( creds.get( Key.of( "sessionToken" ) ) )
		    .isEqualTo( "" );
	}

	@Test
	@DisplayName( "An access key configured without a secret does NOT blank the environment secret" )
	public void testPartialExplicitCredentialsKeepEnvironmentSecret() {
		// The atomic-set fix originally keyed off keyExists( "awsAccessKeyId" ) and assigned the
		// secret via ?: "", so configuring only an access key id blanked a secret that init() had
		// loaded from AWS_SECRET_ACCESS_KEY. resolveAwsCredentials() then failed its len(secret)
		// check, fell through to container/IMDS, and signed with an empty secret. Atomicity must
		// apply to a COMPLETE explicit pair, not to the presence of one key.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				// Stand in for the environment values init() would have seeded.
				service.configure( { awsAccessKeyId: "AKIAENVENVENVENVENVE", awsSecretAccessKey: "env-secret", region: "%s" } )
				// A later partial configure() supplying ONLY an access key id.
				service.configure( { awsAccessKeyId: "%s" } )
				creds = service.resolveAwsCredentials()
			""".formatted( DUMMY_AWS_REGION, DUMMY_AWS_ACCESS_KEY_ID ),
			context
		);
		// @formatter:on

		@SuppressWarnings( "unchecked" )
		IStruct creds = ( IStruct ) variables.get( Key.of( "creds" ) );
		assertThat( creds.get( Key.of( "accessKeyId" ) ) ).isEqualTo( DUMMY_AWS_ACCESS_KEY_ID );
		assertWithMessage( "a partial override must not blank a secret from an earlier source" )
		    .that( creds.get( Key.of( "secretAccessKey" ) ) ).isEqualTo( "env-secret" );
	}

	@Test
	@DisplayName( "Explicit per-call options beat module-level providers.Bedrock.options" )
	public void testExplicitOptionsBeatModuleProviderOptions() {
		// super.configure() appends providers.Bedrock.options with override=true, so reading the
		// merged struct let a module-wide region silently win over an explicit per-call one and
		// send the request to the wrong region.
		IStruct	providers		= Struct.of( "Bedrock", Struct.of( "options", Struct.of( "region", "us-east-1" ) ) );
		Object	priorProviders	= moduleRecord.settings.get( Key.of( "providers" ) );
		moduleRecord.settings.put( Key.of( "providers" ), providers );

		try {
			// @formatter:off
			executeWithTimeoutHandling(
				"""
					service = new bxModules.bxai.models.providers.BedrockService()
					service.configure( { region: "eu-west-2", awsAccessKeyId: "%s", awsSecretAccessKey: "%s" } )
					resolvedRegion = service.getRegion()
				""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY ),
				context
			);
			// @formatter:on

			assertWithMessage( "an explicit per-call region must win over settings.providers.Bedrock.options" )
			    .that( variables.get( Key.of( "resolvedRegion" ) ).toString() ).isEqualTo( "eu-west-2" );
		} finally {
			if ( priorProviders == null ) {
				moduleRecord.settings.remove( Key.of( "providers" ) );
			} else {
				moduleRecord.settings.put( Key.of( "providers" ), priorProviders );
			}
		}
	}

	@Test
	@DisplayName( "Explicit caller credentials and region beat a nested apiKey credentials struct" )
	public void testExplicitCredentialsBeatNestedApiKeyStruct() {
		// credSource used to switch WHOLESALE to resolvedOptions.apiKey whenever aiService() had
		// injected the module-level apiKey struct, discarding the caller's own region and key pair —
		// so an explicit eu-west-2 pair silently signed as the module's us-east-1 account.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				service.configure( {
					region: "eu-west-2",
					awsAccessKeyId: "%s",
					awsSecretAccessKey: "%s",
					apiKey: {
						region: "us-east-1",
						awsAccessKeyId: "AKIAMODULEMODULEMODU",
						awsSecretAccessKey: "module-secret"
					}
				} )
				creds  = service.resolveAwsCredentials()
				region = service.getRegion()
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY ),
			context
		);
		// @formatter:on

		@SuppressWarnings( "unchecked" )
		IStruct creds = ( IStruct ) variables.get( Key.of( "creds" ) );
		assertWithMessage( "an explicit caller key pair must not be replaced by the nested apiKey struct" )
		    .that( creds.get( Key.of( "accessKeyId" ) ) ).isEqualTo( DUMMY_AWS_ACCESS_KEY_ID );
		assertThat( creds.get( Key.of( "secretAccessKey" ) ) ).isEqualTo( DUMMY_AWS_SECRET_ACCESS_KEY );
		assertThat( variables.get( Key.of( "region" ) ).toString() ).isEqualTo( "eu-west-2" );
	}

	@Test
	@DisplayName( "Provider-specific options beat a module-global apiKey credentials struct" )
	public void testProviderOptionsBeatGlobalApiKeyStruct() {
		// Even overlaying the injected apiKey struct rather than switching to it, the global apiKey is
		// the BROADEST configured scope, so it must lose to settings.providers.Bedrock.options as
		// well as to explicit per-call arguments.
		IStruct	providers		= Struct.of(
		    "Bedrock",
		    Struct.of( "options", Struct.of( "region", "eu-west-2", "awsAccessKeyId", "AKIAPROVIDERPROVIDE", "awsSecretAccessKey", "provider-secret" ) )
		);
		Object	priorProviders	= moduleRecord.settings.get( Key.of( "providers" ) );
		moduleRecord.settings.put( Key.of( "providers" ), providers );

		try {
			// @formatter:off
			executeWithTimeoutHandling(
				"""
					service = new bxModules.bxai.models.providers.BedrockService()
					service.configure( {
						apiKey: {
							region: "us-east-1",
							awsAccessKeyId: "AKIAGLOBALGLOBALGLOB",
							awsSecretAccessKey: "global-secret"
						}
					} )
					creds  = service.resolveAwsCredentials()
					region = service.getRegion()
				""",
				context
			);
			// @formatter:on

			@SuppressWarnings( "unchecked" )
			IStruct creds = ( IStruct ) variables.get( Key.of( "creds" ) );
			assertWithMessage( "providers.Bedrock.options must beat the module-global apiKey struct" )
			    .that( creds.get( Key.of( "accessKeyId" ) ) ).isEqualTo( "AKIAPROVIDERPROVIDE" );
			assertThat( creds.get( Key.of( "secretAccessKey" ) ) ).isEqualTo( "provider-secret" );
			assertThat( variables.get( Key.of( "region" ) ).toString() ).isEqualTo( "eu-west-2" );
		} finally {
			if ( priorProviders == null ) {
				moduleRecord.settings.remove( Key.of( "providers" ) );
			} else {
				moduleRecord.settings.put( Key.of( "providers" ), priorProviders );
			}
		}
	}

	@Test
	@DisplayName( "isCredentialCacheValid requires key material, not just an unexpired timestamp" )
	public void testCredentialCacheValidityRequiresKeyMaterial() {
		// A container endpoint answering `200 {}` produced empty keys plus a synthesized 60-minute
		// expiry, which read as a valid cache entry — suppressing the IMDS fallback and signing every
		// request with an empty secret for an hour.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService( "bedrock", { awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" } )
				emptyButUnexpired = service.isCredentialCacheValid( {
					"accessKeyId": "", "secretAccessKey": "", "sessionToken": "",
					"expiration": dateAdd( "n", 60, now() )
				} )
				populated = service.isCredentialCacheValid( {
					"accessKeyId": "AKIAEXAMPLEEXAMPLE12", "secretAccessKey": "s", "sessionToken": "",
					"expiration": dateAdd( "n", 60, now() )
				} )
				expiringSoon = service.isCredentialCacheValid( {
					"accessKeyId": "AKIAEXAMPLEEXAMPLE12", "secretAccessKey": "s", "sessionToken": "",
					"expiration": dateAdd( "n", 2, now() )
				} )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertWithMessage( "an expiry alone is not evidence of credentials" )
		    .that( variables.getAsBoolean( Key.of( "emptyButUnexpired" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "populated" ) ) ).isTrue();
		assertWithMessage( "inside the refresh buffer must not count as valid" )
		    .that( variables.getAsBoolean( Key.of( "expiringSoon" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "isTrustedContainerCredentialUri refuses to send the credential token to an arbitrary HTTP host" )
	public void testContainerCredentialUriTrust() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService( "bedrock", { awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" } )
				ecs        = service.isTrustedContainerCredentialUri( "http://169.254.170.2/creds" )
				eks        = service.isTrustedContainerCredentialUri( "http://169.254.170.23/v1/credentials" )
				loopback   = service.isTrustedContainerCredentialUri( "http://127.0.0.1:8080/creds" )
				httpsAny   = service.isTrustedContainerCredentialUri( "https://vault.internal.example/creds" )
				plainOther = service.isTrustedContainerCredentialUri( "http://evil.example.com/creds" )
				imdsSpoof  = service.isTrustedContainerCredentialUri( "http://169.254.169.254/creds" )
				// userinfo bypass: the real host is evil.example, not 127.0.0.1
				userinfo   = service.isTrustedContainerCredentialUri( "http://127.0.0.1:80@evil.example/creds" )
				userinfo2  = service.isTrustedContainerCredentialUri( "http://169.254.170.2@evil.example/creds" )
				// bracketed IPv6 loopback must be accepted, not mangled into "["
				ipv6       = service.isTrustedContainerCredentialUri( "http://[::1]:8080/creds" )
				ipv6Eks    = service.isTrustedContainerCredentialUri( "http://[fd00:ec2::23]/creds" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "ecs" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "eks" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "loopback" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "httpsAny" ) ) ).isTrue();
		assertWithMessage( "an arbitrary plain-HTTP host must never receive the credential token" )
		    .that( variables.getAsBoolean( Key.of( "plainOther" ) ) ).isFalse();
		assertWithMessage( "the IMDS address is not a container credential endpoint" )
		    .that( variables.getAsBoolean( Key.of( "imdsSpoof" ) ) ).isFalse();
		assertWithMessage( "userinfo before the host must not make an arbitrary host look trusted" )
		    .that( variables.getAsBoolean( Key.of( "userinfo" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "userinfo2" ) ) ).isFalse();
		assertWithMessage( "a bracketed IPv6 loopback literal is a trusted endpoint" )
		    .that( variables.getAsBoolean( Key.of( "ipv6" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "ipv6Eks" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "An explicit key pair never inherits a session token from a lower-priority source" )
	public void testSessionTokenStaysAtomicWithItsKeyPair() {
		// Credential sources were merged into one struct and the token read off the merged result, so
		// a caller's complete pair could pick up a session token belonging to the module-global
		// credentials — SigV4 then signs a mismatched triple and AWS rejects it.
		IStruct	providers		= Struct.of(
		    "Bedrock",
		    Struct.of( "options", Struct.of( "awsSessionToken", "provider-session-token" ) )
		);
		Object	priorProviders	= moduleRecord.settings.get( Key.of( "providers" ) );
		moduleRecord.settings.put( Key.of( "providers" ), providers );

		try {
			// @formatter:off
			executeWithTimeoutHandling(
				"""
					service = new bxModules.bxai.models.providers.BedrockService()
					// A COMPLETE explicit pair with no session token of its own, while both a provider
					// setting and the global apiKey struct offer one.
					service.configure( {
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						apiKey: { awsSessionToken: "global-session-token" }
					} )
					creds = service.resolveAwsCredentials()
				""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY ),
				context
			);
			// @formatter:on

			@SuppressWarnings( "unchecked" )
			IStruct creds = ( IStruct ) variables.get( Key.of( "creds" ) );
			assertThat( creds.get( Key.of( "accessKeyId" ) ) ).isEqualTo( DUMMY_AWS_ACCESS_KEY_ID );
			assertWithMessage( "the session token must come from the same source as the key pair, or not at all" )
			    .that( creds.get( Key.of( "sessionToken" ) ) ).isEqualTo( "" );
		} finally {
			if ( priorProviders == null ) {
				moduleRecord.settings.remove( Key.of( "providers" ) );
			} else {
				moduleRecord.settings.put( Key.of( "providers" ), priorProviders );
			}
		}
	}

	@Test
	@DisplayName( "Auto-resolved credential state is shared across service instances, not per-instance" )
	public void testCredentialCacheIsSharedAcrossInstances() {
		// aiService() builds a fresh provider per call, so an instance-scoped cache was read exactly
		// once and never hit: every request re-paid a container/IMDS round-trip and the negative
		// cache could never fire. The cache now lives in static.CREDENTIAL_CACHE keyed by
		// credentialCacheKey(), so two separate instances must agree on the key and see one entry.
		//
		// Asserted via the key rather than by counting metadata calls, since neither endpoint is
		// reachable from a test host — an off-AWS box is exactly the negative-cache case, and the
		// shared entry is what makes the second instance skip the timeout.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				serviceA = new bxModules.bxai.models.providers.BedrockService()
				serviceB = new bxModules.bxai.models.providers.BedrockService()
				serviceA.configure( { region: "%s" } )
				serviceB.configure( { region: "%s" } )

				// No explicit credentials, so both fall through to the auto-resolved path.
				startA = getTickCount()
				credsA = serviceA.resolveAwsCredentials()
				elapsedA = getTickCount() - startA

				startB = getTickCount()
				credsB = serviceB.resolveAwsCredentials()
				elapsedB = getTickCount() - startB

				sharedKeys = serviceA.credentialCacheKey() == serviceB.credentialCacheKey()
				resolvedA  = isStruct( credsA )
				resolvedB  = isStruct( credsB )
			""".formatted( DUMMY_AWS_REGION, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertWithMessage( "two instances with the same region and credential source must share one cache entry" )
		    .that( variables.getAsBoolean( Key.of( "sharedKeys" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resolvedA" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resolvedB" ) ) ).isTrue();

		// The second instance must not re-pay the metadata timeout the first one already ate: with a
		// per-instance cache both calls paid it. Deliberately a generous bound rather than a tight
		// one — off-AWS the first call spends seconds in container+IMDS timeouts and the second
		// returns from the negative cache immediately, and ON AWS both are fast, so this cannot fail
		// spuriously in either environment; it only fails if the cache stopped being shared.
		long elapsedB = ( ( Number ) variables.get( Key.of( "elapsedB" ) ) ).longValue();
		assertWithMessage( "second instance re-paid the metadata round-trip; credential cache is not shared (elapsedB=" + elapsedB + "ms)" )
		    .that( elapsedB < 1000L ).isTrue();
	}

	@Test
	@DisplayName( "isCredentialCacheValid: fresh cache (>5 min to expiry) is valid; empty/missing-expiration/soon-to-expire/already-expired are not" )
	public void testIsCredentialCacheValidExpiryBuffer() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)

				freshCache             = { accessKeyId: "x", secretAccessKey: "y", sessionToken: "", expiration: dateAdd( "n", 30, now() ) }
				soonToExpireCache      = { accessKeyId: "x", secretAccessKey: "y", sessionToken: "", expiration: dateAdd( "n", 3, now() ) }
				alreadyExpiredCache    = { accessKeyId: "x", secretAccessKey: "y", sessionToken: "", expiration: dateAdd( "n", -5, now() ) }
				emptyCache             = {}
				missingExpirationCache = { accessKeyId: "x" }

				freshIsValid              = service.isCredentialCacheValid( freshCache )
				soonToExpireIsValid       = service.isCredentialCacheValid( soonToExpireCache )
				alreadyExpiredIsValid     = service.isCredentialCacheValid( alreadyExpiredCache )
				emptyIsValid              = service.isCredentialCacheValid( emptyCache )
				missingExpirationIsValid  = service.isCredentialCacheValid( missingExpirationCache )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "freshIsValid" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "soonToExpireIsValid" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "alreadyExpiredIsValid" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "emptyIsValid" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "missingExpirationIsValid" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "detectModelFamily() routes post-2024 Bedrock families to the correct request shape" )
	public void testModelFamilyDetectionTable() {
		// detectModelFamily() is private, so this exercises it indirectly through the shape of
		// the dataPacket produced by transformRequestForModel(): a beforeLLMCall middleware
		// captures the packet and cancels before any signing/HTTP call (same technique as
		// testStructuredOutputInjectsForcedTool), then a shape fingerprint (which keys are
		// present) is mapped back to the family that must have produced it. Covers the
		// GitHub #226 detection table, including the mistral legacy/modern split.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				cases = [
					{ modelId: "zai.glm-4.7-flash",                     expected: "openai"  },
					{ modelId: "qwen.qwen3-32b-v1:0",                   expected: "openai"  },
					{ modelId: "deepseek.v3.2",                         expected: "openai"  },
					{ modelId: "moonshotai.kimi-k2.5",                  expected: "openai"  },
					{ modelId: "minimax.minimax-m2.5",                  expected: "openai"  },
					{ modelId: "nvidia.nemotron-super-3-120b",          expected: "openai"  },
					{ modelId: "google.gemma-3-27b-it",                 expected: "openai"  },
					{ modelId: "mistral.ministral-3-8b-instruct",       expected: "openai"  },
					{ modelId: "openai.gpt-oss-20b-1:0",                expected: "openai"  },
					{ modelId: "mistral.mistral-7b-instruct-v0:2",      expected: "mistral" },
					{ modelId: "mistral.mixtral-8x7b-instruct-v0:1",    expected: "mistral" },
					{ modelId: "eu.anthropic.claude-sonnet-4-6",        expected: "claude"  },
					{ modelId: "amazon.titan-text-express-v1",          expected: "titan"   },
					{ modelId: "meta.llama3-70b-instruct-v1:0",         expected: "llama"   },
					// No dedicated Cohere transform exists; the fix preserves Cohere on the
					// Claude-shape request rather than letting it fall into the new openai default.
					{ modelId: "cohere.command-r-v1:0",                 expected: "claude"  },
					// ai21 Jamba is OpenAI-shaped; the removed ai21 branch used to route it to
					// the Claude transform instead (silent zero chunks on stream).
					{ modelId: "ai21.jamba-1-5-large-v1:0",             expected: "openai"  },
					{ modelId: "mistral.mistral-large-2402-v1:0",       expected: "mistral" },
					{ modelId: "mistral.mistral-small-2402-v1:0",       expected: "mistral" },
					{ modelId: "mistral.mistral-large-2407-v1:0",       expected: "openai"  },
					{ modelId: "mistral.magistral-small-2509-v1:0",     expected: "openai"  },
					// Region-prefixed legacy Mistral: the anchored regex must still match past
					// the "eu." prefix.
					{ modelId: "eu.mistral.mixtral-8x7b-instruct-v0:1", expected: "mistral" },
					// Opaque inference-profile ARN with no vendor substring: previously fell
					// through to the Claude default; the ARN guard preserves that explicitly.
					{ modelId: "arn:aws:bedrock:eu-west-2:123456789012:application-inference-profile/abc123", expected: "claude" },
					// Amazon Nova has no dedicated transform; documents the chosen openai fallback.
					{ modelId: "amazon.nova-lite-v1:0",                 expected: "openai"  }
				]

				mismatches = []
				provider  = aiService(
					"bedrock",
					{
						awsAccessKeyId: "%s",
						awsSecretAccessKey: "%s",
						region: "%s"
					}
				)

				for ( testCase in cases ) {
					captured = {}
					chatRequest = aiChatRequest(
						aiMessage().user( "test" ),
						{ model: testCase.modelId, max_tokens: 50 },
						{ provider: "bedrock" }
					)
					chatRequest.addMiddleware( {
						"beforeLLMCall": ( ctx ) => {
							captured.packet = ctx.dataPacket
							return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
						}
					} )
					provider.chat( chatRequest )
					packet = captured.packet

					shape = "unrecognized-shape"
					if ( packet.keyExists( "anthropic_version" ) ) {
						shape = "claude"
					} else if ( packet.keyExists( "inputText" ) ) {
						shape = "titan"
					} else if ( packet.keyExists( "prompt" ) && packet.keyExists( "max_gen_len" ) ) {
						shape = "llama"
					} else if ( packet.keyExists( "prompt" ) ) {
						shape = "mistral"
					} else if ( packet.keyExists( "messages" ) ) {
						shape = "openai"
					}

					if ( shape != testCase.expected ) {
						mismatches.append( testCase.modelId & ": expected " & testCase.expected & " but got " & shape )
					}
				}

				mismatchCount   = mismatches.len()
				mismatchSummary = mismatches.toList( char( 10 ) )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		int mismatchCount = variables.getAsInteger( Key.of( "mismatchCount" ) );
		if ( mismatchCount > 0 ) {
			System.out.println( "Model family detection mismatches:\n" + variables.get( Key.of( "mismatchSummary" ) ) );
		}
		assertThat( mismatchCount ).isEqualTo( 0 );
	}
}
