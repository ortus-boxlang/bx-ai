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
						region: "%s",
						bedrockApi: "invoke"
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
						region: "%s",
						bedrockApi: "invoke"
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
						region: "%s",
						bedrockApi: "invoke"
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
						region: "%s",
						bedrockApi: "invoke"
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
						region: "%s",
						bedrockApi: "invoke"
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
						region: "%s",
						bedrockApi: "invoke"
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
						region: "%s",
						bedrockApi: "invoke"
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
						region: "%s",
						bedrockApi: "invoke"
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
							region: "%s",
							bedrockApi: "invoke"
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
							region: "%s",
							bedrockApi: "invoke"
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
						region: "%s",
						bedrockApi: "invoke"
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
							region: "%s",
							bedrockApi: "invoke"
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
							region: "%s",
							bedrockApi: "invoke"
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
						region: "%s",
						bedrockApi: "invoke"
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
						region: "%s",
						bedrockApi: "invoke"
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
				service = aiService( "bedrock", { awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" } )
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
				service = aiService( "bedrock", { awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" } )
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
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
					// Cohere now has its own request transform: Command R/R+ get the chat shape,
					// legacy command-text gets the prompt/generate shape.
					{ modelId: "cohere.command-r-v1:0",                 expected: "cohere"  },
					{ modelId: "cohere.command-r-plus-v1:0",            expected: "cohere"  },
					{ modelId: "cohere.command-text-v14",               expected: "cohere-legacy" },
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
						region: "%s",
						bedrockApi: "invoke"
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
					} else if ( packet.keyExists( "message" ) ) {
						shape = "cohere"
					} else if ( packet.keyExists( "inputText" ) ) {
						shape = "titan"
					} else if ( packet.keyExists( "prompt" ) && packet.keyExists( "max_gen_len" ) ) {
						shape = "llama"
					} else if ( packet.keyExists( "prompt" ) && packet.prompt.findNoCase( "[INST]" ) > 0 ) {
						// Legacy Mistral's prompt is the only one wrapped in <s>[INST]...[/INST];
						// legacy Cohere's is the same key carrying the raw flattened messages.
						shape = "mistral"
					} else if ( packet.keyExists( "prompt" ) ) {
						shape = "cohere-legacy"
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

	// ============================================================================================
	// Streaming: AWS event-stream decoding, tool loop, suspend/resume, structured output
	// ============================================================================================

	/**
	 * Build one AWS event-stream frame: prelude(8) + preludeCRC(4) + headers + payload +
	 * messageCRC(4). CRCs are zero — the decoder does not validate them.
	 *
	 * @param headers alternating name/value pairs; a Boolean value is encoded as header value
	 *                type 0/1 (no value bytes), a String as type 7 (2-byte length prefix)
	 */
	private static byte[] frame( byte[] payload, Object... headers ) {
		java.io.ByteArrayOutputStream hb = new java.io.ByteArrayOutputStream();
		for ( int i = 0; i < headers.length; i += 2 ) {
			byte[] name = ( ( String ) headers[ i ] ).getBytes( java.nio.charset.StandardCharsets.UTF_8 );
			hb.write( name.length );
			hb.write( name, 0, name.length );
			Object value = headers[ i + 1 ];
			if ( value instanceof Boolean ) {
				hb.write( ( ( Boolean ) value ) ? 0 : 1 );
			} else {
				byte[] v = ( ( String ) value ).getBytes( java.nio.charset.StandardCharsets.UTF_8 );
				hb.write( 7 );
				hb.write( ( v.length >> 8 ) & 0xFF );
				hb.write( v.length & 0xFF );
				hb.write( v, 0, v.length );
			}
		}
		byte[]				headerBytes	= hb.toByteArray();
		int					total		= 8 + 4 + headerBytes.length + payload.length + 4;
		java.nio.ByteBuffer	bb			= java.nio.ByteBuffer.allocate( total );
		bb.putInt( total );
		bb.putInt( headerBytes.length );
		bb.putInt( 0 );
		bb.put( headerBytes );
		bb.put( payload );
		bb.putInt( 0 );
		return bb.array();
	}

	/** Wrap a model JSON event in the `{"bytes":"<base64>"}` envelope Bedrock uses for chunk frames. */
	private static byte[] chunkPayload( String modelJson ) {
		String b64 = java.util.Base64.getEncoder()
		    .encodeToString( modelJson.getBytes( java.nio.charset.StandardCharsets.UTF_8 ) );
		return ( "{\"bytes\":\"" + b64 + "\"}" ).getBytes( java.nio.charset.StandardCharsets.UTF_8 );
	}

	/** Concatenate frames and base64 them, ready for binaryDecode() inside a BoxLang test script. */
	private static String streamBody( byte[]... frames ) {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		for ( byte[] f : frames ) {
			out.write( f, 0, f.length );
		}
		return java.util.Base64.getEncoder().encodeToString( out.toByteArray() );
	}

	private static final String	EVENT_HEADERS_MSG	= ":message-type";
	private static final String	EVENT_HEADERS_EVT	= ":event-type";

	@Test
	@DisplayName( "Event-stream decoder walks the frame header block and delivers the chunk payloads" )
	public void testEventStreamDecoderParsesHeaderedFrames() {
		// A hand-built binary event-stream: three `:message-type: event` / `:event-type: chunk`
		// frames. The first carries a BOOLEAN header (value type 0, no value bytes) ahead of the
		// string headers, so the header walk has to honour per-type widths — get that wrong and
		// the payload offset is garbage and nothing decodes.
		String body = streamBody(
		    frame( chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"Hello \"}}" ),
		        "x-flag", Boolean.TRUE, EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk", ":content-type", "application/json" ),
		    frame( chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"world\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"},\"usage\":{\"output_tokens\":3}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" )
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				text = chunks
					.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
					.map( c => c.choices.first().delta.content ?: "" )
					.toList( "" )
				sawStop = chunks.some( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() && ( c.choices.first().finish_reason ?: "" ) == "end_turn" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION, body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "text" ) ).toString() ).isEqualTo( "Hello world" );
		assertThat( variables.getAsBoolean( Key.of( "sawStop" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "An exception frame mid-stream throws BedrockStreamError instead of ending silently" )
	public void testEventStreamExceptionFrameThrows() {
		// Before the header block was decoded, a throttlingException frame was just an untyped
		// payload: it transformed to a null chunk and was logged at DEBUG, so a throttled stream
		// was indistinguishable from a short answer.
		String body = streamBody(
		    frame( chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"partial\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( "{\"message\":\"Too many tokens, please wait\"}".getBytes( java.nio.charset.StandardCharsets.UTF_8 ),
		        EVENT_HEADERS_MSG, "exception", ":exception-type", "throttlingException", ":content-type", "application/json" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" )
				} )

				errType = ""
				errMsg  = ""
				chunks  = []
				try {
					provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
				sawException  = errType == "BedrockStreamError"
				namesKind     = errMsg.findNoCase( "throttlingException" ) > 0
				carriesDetail = errMsg.findNoCase( "Too many tokens" ) > 0
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION, body ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawException" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "namesKind" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "carriesDetail" ) ) ).isTrue();
	}

	/**
	 * BoxLang preamble shared by the streaming tool tests: `evt()` renders one model event into the
	 * `{"bytes":"..."}` envelope, and the concatenation is fed to wrapLLMCall as a STRING body,
	 * which parseBedrockEventStream() handles through its documented non-binary fallback.
	 */
	private static final String STREAM_EVENT_HELPER = """
	                                                  evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
	                                                  toolStream = evt( { "type": "message_start", "message": { "id": "msg_1", "model": "claude", "usage": { "input_tokens": 7 } } } )
	                                                      & evt( { "type": "content_block_start", "index": 0, "content_block": { "type": "tool_use", "id": "tu_1", "name": "getWeather" } } )
	                                                      & evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "input_json_delta", "partial_json": '{"city":"Par' } } )
	                                                      & evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "input_json_delta", "partial_json": 'is"}' } } )
	                                                      & evt( { "type": "content_block_stop", "index": 0 } )
	                                                      & evt( { "type": "message_delta", "delta": { "stop_reason": "tool_use" }, "usage": { "output_tokens": 11 } } )
	                                                  textStream = evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "text_delta", "text": "Sunny in Paris" } } )
	                                                      & evt( { "type": "message_delta", "delta": { "stop_reason": "end_turn" }, "usage": { "output_tokens": 4 } } )
	                                                  """;

	@Test
	@DisplayName( "Streamed tool_use is accumulated across content_block_start/input_json_delta/stop into a normalized tool call" )
	public void testStreamedToolUseAccumulation() {
		// @formatter:off
		executeWithTimeoutHandling(
			STREAM_EVENT_HELPER + """
				tool     = aiTool( "getWeather", "Get weather", ( required string city ) => "sunny in " & city )
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				wrapCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return wrapCalls == 1 ? toolStream : textStream
					}
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				toolChunks = chunks.filter( c =>
					isStruct( c )
					&& isArray( c.choices ?: "" )
					&& c.choices.len()
					&& isStruct( c.choices.first().delta ?: "" )
					&& c.choices.first().delta.keyExists( "tool_calls" )
				)
				toolChunkCount = toolChunks.len()
				emittedCall    = toolChunkCount ? toolChunks.first().choices.first().delta.tool_calls.first() : {}
				emittedName    = emittedCall.function.name ?: ""
				parsedArgs     = toolChunkCount ? jsonDeserialize( emittedCall.function.arguments ) : {}
				parsedCity     = parsedArgs.city ?: ""
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "toolChunkCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "emittedName" ) ).toString() ).isEqualTo( "getWeather" );
		assertThat( variables.get( Key.of( "parsedCity" ) ).toString() ).isEqualTo( "Paris" );
	}

	@Test
	@DisplayName( "Streaming tool loop fires beforeToolCall, executes the tool and issues the follow-up turn" )
	public void testStreamingToolLoopRunsMiddlewareAndFollowUp() {
		// @formatter:off
		executeWithTimeoutHandling(
			STREAM_EVENT_HELPER + """
				toolRuns = 0
				capturedCity = ""
				tool = aiTool( "getWeather", "Get weather", ( required string city ) => {
					toolRuns++
					capturedCity = city
					return "sunny in " & city
				} )
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				wrapCalls           = 0
				beforeToolCallFired = false
				capturedArgs        = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return wrapCalls == 1 ? toolStream : textStream
					},
					"beforeToolCall": ( ctx ) => {
						beforeToolCallFired = true
						capturedArgs        = ctx.toolArgs ?: {}
					}
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				finalText = chunks
					.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
					.map( c => c.choices.first().delta.content ?: "" )
					.toList( "" )
				sawFollowUp  = wrapCalls == 2
				sawRealArgs  = ( capturedArgs.city ?: "" ) == "Paris"
				toolRanOnce  = toolRuns == 1
				sawToolResult = chatRequest.getMessages().some( m => isArray( m.content ?: "" ) && m.content.some( b => ( b.type ?: "" ) == "tool_result" ) )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "beforeToolCallFired" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawRealArgs" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawFollowUp" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawToolResult" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "finalText" ) ).toString() ).isEqualTo( "Sunny in Paris" );
	}

	@Test
	@DisplayName( "Two approval-requiring tools suspend once as a batch in streaming; resumeToolBatchStream finishes it without replaying the LLM" )
	public void testStreamingBatchSuspendAndResume() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
				twoToolStream = evt( { "type": "content_block_start", "index": 0, "content_block": { "type": "tool_use", "id": "tu_a", "name": "toolA" } } )
					& evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "input_json_delta", "partial_json": '{}' } } )
					& evt( { "type": "content_block_stop", "index": 0 } )
					& evt( { "type": "content_block_start", "index": 1, "content_block": { "type": "tool_use", "id": "tu_b", "name": "toolB" } } )
					& evt( { "type": "content_block_delta", "index": 1, "delta": { "type": "input_json_delta", "partial_json": '{}' } } )
					& evt( { "type": "content_block_stop", "index": 1 } )
					& evt( { "type": "message_delta", "delta": { "stop_reason": "tool_use" }, "usage": { "output_tokens": 8 } } )
				textStream = evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "text_delta", "text": "both done" } } )
					& evt( { "type": "message_delta", "delta": { "stop_reason": "end_turn" }, "usage": { "output_tokens": 2 } } )

				toolACalls = 0
				toolBCalls = 0
				toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )
				toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )

				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)

				wrapCalls = 0
				llmMw = {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return wrapCalls == 1 ? twoToolStream : textStream
					}
				}

				chatRequest = aiChatRequest(
					aiMessage().user( "run both tools" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ toolA, toolB ] },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ) )
				chatRequest.addMiddleware( llmMw )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				stops        = chunks.filter( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
				suspended    = stops.len() == 1 && stops.first().result.isSuspended()
				suspendData  = suspended ? stops.first().result.getData() : {}
				pendingCount = ( suspendData.pendingActions ?: [] ).len()
				ledgerCount  = ( suspendData.resumeLedger ?: [] ).len()
				neitherRanYet = toolACalls == 0 && toolBCalls == 0
				calledLLMOnce = wrapCalls == 1

				// ---- resume: approve both, finish the SAME batch, no replay of the suspended turn ----
				resumeRequest = aiChatRequest(
					aiMessage().user( "run both tools" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ toolA, toolB ] },
					{
						provider: "bedrock",
						_resumeContext: {
							assistantMessage: suspendData.assistantMessage,
							resumeLedger    : [
								{ toolName: "toolA", status: "execute" },
								{ toolName: "toolB", status: "execute" }
							]
						}
					}
				)
				resumeRequest.addMiddleware( llmMw )

				resumeChunks = []
				provider.chatStream( resumeRequest, ( chunk ) => { resumeChunks.append( chunk ) } )

				finalText    = resumeChunks
					.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
					.map( c => c.choices.first().delta.content ?: "" )
					.toList( "" )
				bothRanOnce  = toolACalls == 1 && toolBCalls == 1
				// Only the follow-up turn touched the LLM — the suspended turn was NOT replayed
				resumeAddedOneLLMCall = wrapCalls == 2
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspended" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "pendingCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "ledgerCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "calledLLMOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "bothRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resumeAddedOneLLMCall" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "finalText" ) ).toString() ).isEqualTo( "both done" );
	}

	@Test
	@DisplayName( "Streaming structured output accumulates the forced tool block and populates the struct" )
	public void testStreamingStructuredOutputPopulates() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
				soStream = evt( { "type": "content_block_start", "index": 0, "content_block": { "type": "tool_use", "id": "tu_so", "name": "structured_output" } } )
					& evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "input_json_delta", "partial_json": '{"name":"John ' } } )
					& evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "input_json_delta", "partial_json": 'Doe","age":30}' } } )
					& evt( { "type": "content_block_stop", "index": 0 } )
					& evt( { "type": "message_delta", "delta": { "stop_reason": "tool_use" }, "usage": { "output_tokens": 12 } } )

				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
				forcedToolName = ""
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						forcedToolName = ctx.dataPacket.tool_choice.name ?: ""
					},
					"wrapLLMCall": ( ctx, handler ) => soStream
				} )

				chunks = []
				result = provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				isResultStruct = isStruct( result )
				emittedOnStream = chunks.some( c =>
					isStruct( c )
					&& isArray( c.choices ?: "" )
					&& c.choices.len()
					&& isStruct( c.choices.first().delta ?: "" )
					&& c.choices.first().delta.keyExists( "structured_output" )
				)
				resultName     = result.name ?: ""
				resultAge      = result.age ?: 0
				// the schema carrier is never emitted as an executable tool call
				sawToolCallChunk = chunks.some( c =>
					isStruct( c )
					&& isArray( c.choices ?: "" )
					&& c.choices.len()
					&& isStruct( c.choices.first().delta ?: "" )
					&& c.choices.first().delta.keyExists( "tool_calls" )
				)
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "forcedToolName" ) ).toString() ).isEqualTo( "structured_output" );
		assertThat( variables.getAsBoolean( Key.of( "isResultStruct" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "resultName" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "resultAge" ) ) ).isEqualTo( 30 );
		assertThat( variables.getAsBoolean( Key.of( "sawToolCallChunk" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "emittedOnStream" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Streaming structured output throws StructuredOutputError when the forced block never arrives" )
	public void testStreamingStructuredOutputThrowsWhenAbsent() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
				proseStream = evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "text_delta", "text": "I cannot comply." } } )
					& evt( { "type": "message_delta", "delta": { "stop_reason": "max_tokens" }, "usage": { "output_tokens": 4 } } )

				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
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
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => proseStream } )

				errType = ""
				errMsg  = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
				threwStructuredError = errType == "StructuredOutputError"
				mentionsTruncation   = errMsg.findNoCase( "max_tokens" ) > 0
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threwStructuredError" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "mentionsTruncation" ) ) ).isTrue();
	}

	// ============================================================================================
	// Stage 2: OpenAI-shaped families, Cohere transforms, cache_control on system
	// ============================================================================================

	@Test
	@DisplayName( "OpenAI-family request carries tools, tool_choice, response_format, reasoning_effort and stop" )
	public void testOpenAIRequestCarriesFullParameterSet() {
		// transformRequestForOpenAI used to emit only { messages, max_tokens, temperature, top_p },
		// so everything below was silently dropped before it reached Bedrock.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => "sunny" )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{
						model            : "openai.gpt-oss-120b-1:0",
						max_tokens       : 321,
						temperature      : 0.4,
						top_p            : 0.8,
						stop             : [ "STOP" ],
						reasoning_effort : "high",
						tool_choice      : "auto",
						tools            : [ tool ],
						stream           : true,
						response_format  : { "type": "text" }
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

				packet             = captured.packet
				maxCompletion      = packet.max_completion_tokens
				hasLegacyMaxTokens = packet.keyExists( "max_tokens" )
				temperatureVal     = packet.temperature
				topPVal            = packet.top_p
				stopVal            = packet.stop[ 1 ]
				reasoningEffort    = packet.reasoning_effort
				toolChoice         = packet.tool_choice
				responseFormatType = packet.response_format.type
				toolCount          = packet.tools.len()
				toolName           = packet.tools[ 1 ].function.name
				hasModel           = packet.keyExists( "model" )
				hasStream          = packet.keyExists( "stream" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "maxCompletion" ) ) ).isEqualTo( 321 );
		assertThat( variables.getAsBoolean( Key.of( "hasLegacyMaxTokens" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "temperatureVal" ) ).toString() ).isEqualTo( "0.4" );
		assertThat( variables.get( Key.of( "topPVal" ) ).toString() ).isEqualTo( "0.8" );
		assertThat( variables.get( Key.of( "stopVal" ) ) ).isEqualTo( "STOP" );
		assertThat( variables.get( Key.of( "reasoningEffort" ) ) ).isEqualTo( "high" );
		assertThat( variables.get( Key.of( "toolChoice" ) ) ).isEqualTo( "auto" );
		assertThat( variables.get( Key.of( "responseFormatType" ) ) ).isEqualTo( "text" );
		assertThat( variables.getAsInteger( Key.of( "toolCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "getWeather" );
		assertThat( variables.getAsBoolean( Key.of( "hasModel" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "hasStream" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Capability gate no longer throws for tools on an OpenAI-shaped (gpt-oss) model" )
	public void testCapabilityGateAllowsGptOssWithTools() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				tool = aiTool( "noop", "does nothing", () => "ok" )
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "openai.gpt-oss-20b-1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )
				caughtType = ""
				try {
					provider.chat( chatRequest )
				} catch( any e ) {
					caughtType = e.type
				}
				reachedTransform = captured.keyExists( "packet" )
				toolName         = reachedTransform ? captured.packet.tools[ 1 ].function.name : ""
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "caughtType" ) ) ).isEqualTo( "" );
		assertThat( variables.getAsBoolean( Key.of( "reachedTransform" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "noop" );
	}

	@Test
	@DisplayName( "Legacy Cohere Command still throws UnsupportedProviderCapability for tools" )
	public void testCapabilityGateStillBlocksLegacyCohereTools() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				tool = aiTool( "noop", "does nothing", () => "ok" )
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "cohere.command-text-v14", tools: [ tool ] },
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
		assertThat( variables.get( Key.of( "caughtMsg" ) ).toString() ).contains( "legacy cohere Command" );
	}

	@Test
	@DisplayName( "OpenAI-family sync tool loop fires beforeToolCall, executes the tool and issues the follow-up turn" )
	public void testOpenAIFamilySyncToolLoop() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				toolRuns = 0
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => {
					toolRuns++
					return "sunny in " & city
				} )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "openai.gpt-oss-120b-1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				usage  = { "prompt_tokens": 5, "completion_tokens": 3, "total_tokens": 8 }
				calls  = 0
				beforeToolCallFired = false
				capturedArgs        = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						calls++
						if ( calls == 1 ) {
							return {
								"choices": [ {
									"index": 0,
									"message": {
										"role": "assistant",
										"content": "",
										"tool_calls": [ {
											"id": "call_1",
											"type": "function",
											"function": { "name": "getWeather", "arguments": '{"city":"Paris"}' }
										} ]
									},
									"finish_reason": "tool_calls"
								} ],
								"usage": usage
							}
						}
						return {
							"choices": [ { "index": 0, "message": { "role": "assistant", "content": "Sunny in Paris" }, "finish_reason": "stop" } ],
							"usage": usage
						}
					},
					"beforeToolCall": ( ctx ) => {
						beforeToolCallFired = true
						capturedArgs        = ctx.toolArgs ?: {}
					}
				} )

				answer       = provider.chat( chatRequest )
				sawFollowUp  = calls == 2
				toolRanOnce  = toolRuns == 1
				sawRealArgs  = ( capturedArgs.city ?: "" ) == "Paris"
				toolMessages = chatRequest.getMessages().filter( m => ( m.role ?: "" ) == "tool" )
				toolMsgCount = toolMessages.len()
				toolMsgId    = toolMsgCount ? toolMessages.first().tool_call_id : ""
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "beforeToolCallFired" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawRealArgs" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawFollowUp" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "toolMsgCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "toolMsgId" ) ) ).isEqualTo( "call_1" );
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "Sunny in Paris" );
	}

	@Test
	@DisplayName( "OpenAI-family structured output sends response_format and populates the struct" )
	public void testOpenAIFamilyStructuredOutput() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "openai.gpt-oss-120b-1:0" },
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
					"beforeLLMCall": ( ctx ) => { captured.packet = ctx.dataPacket },
					"wrapLLMCall" : ( ctx, handler ) => {
						return {
							"choices": [ { "index": 0, "message": { "role": "assistant", "content": '{"name":"John Doe","age":30}' }, "finish_reason": "stop" } ],
							"usage": { "prompt_tokens": 5, "completion_tokens": 8, "total_tokens": 13 }
						}
					}
				} )
				result       = provider.chat( chatRequest )
				formatType   = captured.packet.response_format.type
				schemaName   = captured.packet.response_format.json_schema.name
				hasSchema    = captured.packet.response_format.json_schema.keyExists( "schema" )
				isStructRes  = isStruct( result )
				name         = result.name
				age          = result.age
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "formatType" ) ) ).isEqualTo( "json_schema" );
		assertThat( variables.get( Key.of( "schemaName" ) ) ).isEqualTo( "response" );
		assertThat( variables.getAsBoolean( Key.of( "hasSchema" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isStructRes" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	/**
	 * OpenAI-shaped streaming events, delivered inside the Bedrock event-stream `{"bytes":...}`
	 * envelope: one tool call spread over two `delta.tool_calls` fragments, closed by a
	 * finish_reason "tool_calls" chunk, then a plain text follow-up stream.
	 */
	private static final String OPENAI_STREAM_EVENT_HELPER = """
	                                                         evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
	                                                         oaToolStream = evt( { "choices": [ { "index": 0, "delta": { "role": "assistant", "tool_calls": [ { "index": 0, "id": "call_1", "type": "function", "function": { "name": "getWeather", "arguments": '{"city":"Par' } } ] } } ] } )
	                                                             & evt( { "choices": [ { "index": 0, "delta": { "tool_calls": [ { "index": 0, "function": { "arguments": 'is"}' } } ] } } ] } )
	                                                             & evt( { "choices": [ { "index": 0, "delta": {}, "finish_reason": "tool_calls" } ], "usage": { "prompt_tokens": 7, "completion_tokens": 11, "total_tokens": 18 } } )
	                                                         oaTextStream = evt( { "choices": [ { "index": 0, "delta": { "content": "Sunny in Paris" } } ] } )
	                                                             & evt( { "choices": [ { "index": 0, "delta": {}, "finish_reason": "stop" } ], "usage": { "prompt_tokens": 9, "completion_tokens": 4, "total_tokens": 13 } } )
	                                                         """;

	@Test
	@DisplayName( "OpenAI-family stream accumulates delta.tool_calls fragments into a normalized tool call and runs the follow-up" )
	public void testOpenAIFamilyStreamToolCalls() {
		// @formatter:off
		executeWithTimeoutHandling(
			OPENAI_STREAM_EVENT_HELPER + """
				toolRuns = 0
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => {
					toolRuns++
					return "sunny in " & city
				} )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "openai.gpt-oss-120b-1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				wrapCalls           = 0
				beforeToolCallFired = false
				capturedArgs        = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return wrapCalls == 1 ? oaToolStream : oaTextStream
					},
					"beforeToolCall": ( ctx ) => {
						beforeToolCallFired = true
						capturedArgs        = ctx.toolArgs ?: {}
					}
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				toolChunks = chunks.filter( c =>
					isStruct( c )
					&& isArray( c.choices ?: "" )
					&& c.choices.len()
					&& isStruct( c.choices.first().delta ?: "" )
					&& c.choices.first().delta.keyExists( "tool_calls" )
				)
				toolChunkCount = toolChunks.len()
				emittedCall    = toolChunkCount ? toolChunks.first().choices.first().delta.tool_calls.first() : {}
				emittedName    = emittedCall.function.name ?: ""
				emittedId      = emittedCall.id ?: ""
				parsedCity     = toolChunkCount ? ( jsonDeserialize( emittedCall.function.arguments ).city ?: "" ) : ""
				finishReason   = toolChunkCount ? ( toolChunks.first().choices.first().finish_reason ?: "" ) : ""

				finalText = chunks
					.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
					.map( c => c.choices.first().delta.content ?: "" )
					.toList( "" )
				sawFollowUp = wrapCalls == 2
				toolRanOnce = toolRuns == 1
				sawRealArgs = ( capturedArgs.city ?: "" ) == "Paris"
				sawToolMsg  = chatRequest.getMessages().some( m => ( m.role ?: "" ) == "tool" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "toolChunkCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "emittedName" ) ).toString() ).isEqualTo( "getWeather" );
		assertThat( variables.get( Key.of( "emittedId" ) ).toString() ).isEqualTo( "call_1" );
		assertThat( variables.get( Key.of( "parsedCity" ) ).toString() ).isEqualTo( "Paris" );
		assertThat( variables.get( Key.of( "finishReason" ) ).toString() ).isEqualTo( "tool_calls" );
		assertThat( variables.getAsBoolean( Key.of( "beforeToolCallFired" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawRealArgs" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawFollowUp" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawToolMsg" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "finalText" ) ).toString() ).isEqualTo( "Sunny in Paris" );
	}

	@Test
	@DisplayName( "Cohere Command R request is built in Cohere's chat shape, not Claude's Messages shape" )
	public void testCohereCommandRRequestTransform() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => "sunny" )
				chatRequest = aiChatRequest(
					aiMessage().user( "ignored" ),
					{
						model       : "cohere.command-r-plus-v1:0",
						max_tokens  : 512,
						temperature : 0.3,
						top_p       : 0.7,
						top_k       : 40,
						stop        : [ "STOP" ],
						tools       : [ tool ]
					},
					{ provider: "bedrock" }
				)
				chatRequest.setMessages( [
					{ "role": "system",    "content": "You are terse." },
					{ "role": "user",      "content": "Who discovered gravity?" },
					{ "role": "assistant", "content": "Newton." },
					{ "role": "user",      "content": "What are some skills?" }
				] )
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )
				provider.chat( chatRequest )

				packet            = captured.packet
				hasAnthropic      = packet.keyExists( "anthropic_version" )
				message           = packet.message
				preamble          = packet.preamble
				historyLen        = packet.chat_history.len()
				historyRole1      = packet.chat_history[ 1 ].role
				historyRole2      = packet.chat_history[ 2 ].role
				historyMsg2       = packet.chat_history[ 2 ].message
				maxTokens         = packet.max_tokens
				pVal              = packet.p
				kVal              = packet.k
				hasTopP           = packet.keyExists( "top_p" )
				stopSeq           = packet.stop_sequences[ 1 ]
				toolName          = packet.tools[ 1 ].name
				paramType         = packet.tools[ 1 ].parameter_definitions.city.type
				paramRequired     = packet.tools[ 1 ].parameter_definitions.city.required
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasAnthropic" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "message" ) ) ).isEqualTo( "What are some skills?" );
		assertThat( variables.get( Key.of( "preamble" ) ) ).isEqualTo( "You are terse." );
		assertThat( variables.getAsInteger( Key.of( "historyLen" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "historyRole1" ) ) ).isEqualTo( "USER" );
		assertThat( variables.get( Key.of( "historyRole2" ) ) ).isEqualTo( "CHATBOT" );
		assertThat( variables.get( Key.of( "historyMsg2" ) ) ).isEqualTo( "Newton." );
		assertThat( variables.getAsInteger( Key.of( "maxTokens" ) ) ).isEqualTo( 512 );
		assertThat( variables.get( Key.of( "pVal" ) ).toString() ).isEqualTo( "0.7" );
		assertThat( variables.getAsInteger( Key.of( "kVal" ) ) ).isEqualTo( 40 );
		assertThat( variables.getAsBoolean( Key.of( "hasTopP" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "stopSeq" ) ) ).isEqualTo( "STOP" );
		assertThat( variables.get( Key.of( "toolName" ) ) ).isEqualTo( "getWeather" );
		assertThat( variables.get( Key.of( "paramType" ) ) ).isEqualTo( "string" );
		assertThat( variables.getAsBoolean( Key.of( "paramRequired" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Legacy Cohere Command request is built in the prompt/generate shape" )
	public void testCohereLegacyRequestTransform() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				captured = {}
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "Tell me a joke" ),
					{ model: "cohere.command-text-v14", max_tokens: 128, top_p: 0.6, top_k: 20 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						captured.packet = ctx.dataPacket
						return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
					}
				} )
				provider.chat( chatRequest )

				packet     = captured.packet
				prompt     = packet.prompt
				maxTokens  = packet.max_tokens
				pVal       = packet.p
				kVal       = packet.k
				hasMessage = packet.keyExists( "message" )
				hasHistory = packet.keyExists( "chat_history" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "prompt" ) ) ).isEqualTo( "Tell me a joke" );
		assertThat( variables.getAsInteger( Key.of( "maxTokens" ) ) ).isEqualTo( 128 );
		assertThat( variables.get( Key.of( "pVal" ) ).toString() ).isEqualTo( "0.6" );
		assertThat( variables.getAsInteger( Key.of( "kVal" ) ) ).isEqualTo( 20 );
		assertThat( variables.getAsBoolean( Key.of( "hasMessage" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "hasHistory" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Cohere stream events (text-generation / stream-end) are normalized instead of being read as Claude chunks" )
	public void testCohereStreamChunkTransform() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
				cohereStream = evt( { "event_type": "stream-start", "generation_id": "g1" } )
					& evt( { "event_type": "text-generation", "text": "Hello " } )
					& evt( { "event_type": "text-generation", "text": "world" } )
					& evt( { "event_type": "stream-end", "finish_reason": "COMPLETE", "meta": { "billed_units": { "input_tokens": 3, "output_tokens": 2 } } } )

				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "cohere.command-r-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => cohereStream } )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				text = chunks
					.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
					.map( c => c.choices.first().delta.content ?: "" )
					.toList( "" )
				sawStop  = chunks.some( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() && ( c.choices.first().finish_reason ?: "" ) == "stop" )
				sawUsage = chunks.some( c => isStruct( c ) && isStruct( c.usage ?: "" ) && ( c.usage.prompt_tokens ?: 0 ) == 3 )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "text" ) ).toString() ).isEqualTo( "Hello world" );
		assertThat( variables.getAsBoolean( Key.of( "sawStop" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawUsage" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Cohere Command R stream tool-calls-generation is normalized and drives the tool loop" )
	public void testCohereStreamToolCallsGeneration() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
				cohereToolStream = evt( { "event_type": "stream-start", "generation_id": "g1" } )
					& evt( { "event_type": "tool-calls-generation", "tool_calls": [ { "name": "getWeather", "parameters": { "city": "Paris" } } ] } )
					& evt( { "event_type": "stream-end", "finish_reason": "COMPLETE" } )
				cohereTextStream = evt( { "event_type": "text-generation", "text": "Sunny in Paris" } )
					& evt( { "event_type": "stream-end", "finish_reason": "COMPLETE" } )

				toolRuns = 0
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => {
					toolRuns++
					return "sunny in " & city
				} )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "cohere.command-r-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				wrapCalls    = 0
				followUpBody = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 2 ) { followUpBody = ctx.dataPacket }
						return wrapCalls == 1 ? cohereToolStream : cohereTextStream
					}
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				toolChunks = chunks.filter( c =>
					isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len()
					&& isStruct( c.choices.first().delta ?: "" )
					&& c.choices.first().delta.keyExists( "tool_calls" )
				)
				toolChunkCount = toolChunks.len()
				emittedName    = toolChunkCount ? ( toolChunks.first().choices.first().delta.tool_calls.first().function.name ?: "" ) : ""
				toolRanOnce    = toolRuns == 1
				sawFollowUp    = wrapCalls == 2
				hasToolResults = isStruct( followUpBody ) && followUpBody.keyExists( "tool_results" )
				resultName     = hasToolResults ? followUpBody.tool_results[ 1 ].call.name : ""
				resultCity     = hasToolResults ? followUpBody.tool_results[ 1 ].call.parameters.city : ""
				resultOutput   = hasToolResults ? followUpBody.tool_results[ 1 ].outputs[ 1 ].result : ""
				noMessageKey   = hasToolResults && !followUpBody.keyExists( "message" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "toolChunkCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "emittedName" ) ).toString() ).isEqualTo( "getWeather" );
		assertThat( variables.getAsBoolean( Key.of( "toolRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawFollowUp" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasToolResults" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "resultName" ) ) ).isEqualTo( "getWeather" );
		assertThat( variables.get( Key.of( "resultCity" ) ) ).isEqualTo( "Paris" );
		assertThat( variables.get( Key.of( "resultOutput" ) ).toString() ).isEqualTo( "sunny in Paris" );
		assertThat( variables.getAsBoolean( Key.of( "noMessageKey" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Claude transform promotes system to text blocks when cache_control is present, and keeps the plain string otherwise" )
	public void testClaudeSystemCacheControlPreserved() {
		// Flattening system messages to a string silently dropped cache_control, and with it
		// prompt caching on the (usually large) system prompt.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)

				capture = ( messages ) => {
					var captured = {}
					var req = aiChatRequest(
						aiMessage().user( "hi" ),
						{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
						{ provider: "bedrock" }
					)
					req.setMessages( messages )
					req.addMiddleware( {
						"beforeLLMCall": ( ctx ) => {
							captured.packet = ctx.dataPacket
							return new src.main.bx.models.middleware.AiMiddlewareResult( "cancel", "test-capture" )
						}
					} )
					provider.chat( req )
					return captured.packet
				}

				cached = capture( [
					{ "role": "system", "content": "Big system prompt", "cache_control": { "type": "ephemeral" } },
					{ "role": "user",   "content": "hi" }
				] )
				plain = capture( [
					{ "role": "system", "content": "Big system prompt" },
					{ "role": "user",   "content": "hi" }
				] )
				blocks = capture( [
					{ "role": "system", "content": [ { "type": "text", "text": "Block prompt", "cache_control": { "type": "ephemeral" } } ] },
					{ "role": "user",   "content": "hi" }
				] )

				cachedIsArray  = isArray( cached.system )
				cachedType     = cachedIsArray ? cached.system[ 1 ].type : ""
				cachedText     = cachedIsArray ? cached.system[ 1 ].text : ""
				cachedControl  = cachedIsArray ? cached.system[ 1 ].cache_control.type : ""
				plainIsString  = isSimpleValue( plain.system )
				plainValue     = plain.system
				blocksIsArray  = isArray( blocks.system )
				blocksText     = blocksIsArray ? blocks.system[ 1 ].text : ""
				blocksControl  = blocksIsArray ? blocks.system[ 1 ].cache_control.type : ""
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "cachedIsArray" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "cachedType" ) ) ).isEqualTo( "text" );
		assertThat( variables.get( Key.of( "cachedText" ) ) ).isEqualTo( "Big system prompt" );
		assertThat( variables.get( Key.of( "cachedControl" ) ) ).isEqualTo( "ephemeral" );
		assertThat( variables.getAsBoolean( Key.of( "plainIsString" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "plainValue" ) ) ).isEqualTo( "Big system prompt" );
		assertThat( variables.getAsBoolean( Key.of( "blocksIsArray" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "blocksText" ) ) ).isEqualTo( "Block prompt" );
		assertThat( variables.get( Key.of( "blocksControl" ) ) ).isEqualTo( "ephemeral" );
	}

	@Test
	@DisplayName( "gpt-oss stream whose tool_calls turn ends with finish_reason 'stop' still runs the tool with the REAL arguments" )
	public void testOpenAIStreamToolCallsDrainedOnNonToolFinishReason() {
		// The buffers were only finalized on finish_reason == "tool_calls"; any other terminator
		// left the accumulated call sitting in streamState with `input: {}` and the tool loop —
		// gated on toolCalls.len() alone — ran it with empty arguments.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
				oaToolStream = evt( { "choices": [ { "index": 0, "delta": { "role": "assistant", "tool_calls": [ { "index": 0, "id": "call_1", "type": "function", "function": { "name": "getWeather", "arguments": '{"city":"Par' } } ] } } ] } )
					& evt( { "choices": [ { "index": 0, "delta": { "tool_calls": [ { "index": 0, "function": { "arguments": 'is"}' } } ] } } ] } )
					& evt( { "choices": [ { "index": 0, "delta": {}, "finish_reason": "stop" } ], "usage": { "prompt_tokens": 7, "completion_tokens": 11, "total_tokens": 18 } } )
				oaTextStream = evt( { "choices": [ { "index": 0, "delta": { "content": "Sunny in Paris" } } ] } )
					& evt( { "choices": [ { "index": 0, "delta": {}, "finish_reason": "stop" } ] } )

				toolRuns     = 0
				capturedCity = ""
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => {
					toolRuns++
					capturedCity = city
					return "sunny in " & city
				} )
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "openai.gpt-oss-120b-1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				wrapCalls    = 0
				capturedArgs = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return wrapCalls == 1 ? oaToolStream : oaTextStream
					},
					"beforeToolCall": ( ctx ) => { capturedArgs = ctx.toolArgs ?: {} }
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				toolRanOnce = toolRuns == 1
				sawRealArgs = ( capturedArgs.city ?: "" ) == "Paris"
				sawFollowUp = wrapCalls == 2
				drainedChunkEmitted = chunks.some( c =>
					isStruct( c )
					&& isArray( c.choices ?: "" )
					&& c.choices.len()
					&& isStruct( c.choices.first().delta ?: "" )
					&& c.choices.first().delta.keyExists( "tool_calls" )
				)
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "toolRanOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawRealArgs" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "capturedCity" ) ).toString() ).isEqualTo( "Paris" );
		assertThat( variables.getAsBoolean( Key.of( "sawFollowUp" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "drainedChunkEmitted" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Claude stream truncated mid input_json_delta (max_tokens, no content_block_stop) throws instead of running the tool with {}" )
	public void testClaudeStreamTruncatedToolInputThrows() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				evt = ( data ) => '{"bytes":"' & binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'
				truncatedStream = evt( { "type": "content_block_start", "index": 0, "content_block": { "type": "tool_use", "id": "tu_1", "name": "getWeather" } } )
					& evt( { "type": "content_block_delta", "index": 0, "delta": { "type": "input_json_delta", "partial_json": '{"city":"Par' } } )
					& evt( { "type": "message_delta", "delta": { "stop_reason": "max_tokens" }, "usage": { "output_tokens": 11 } } )

				toolRuns = 0
				tool = aiTool( "getWeather", "Get weather", ( required string city ) => { toolRuns++; return "sunny" } )
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				wrapCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => { wrapCalls++; return truncatedStream }
				} )

				errType = ""
				errMsg  = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
				threwStreamError = errType == "BedrockStreamError"
				namesTruncation  = errMsg.findNoCase( "truncated" ) > 0
				namesStopReason  = errMsg.findNoCase( "max_tokens" ) > 0
				toolNeverRan     = toolRuns == 0
				noFollowUpTurn   = wrapCalls == 1
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threwStreamError" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "namesTruncation" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "namesStopReason" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "toolNeverRan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noFollowUpTurn" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Streaming beforeLLMCall/wrapLLMCall context carries stream:true, and a struct return is a BedrockStreamError not a cast error" )
	public void testStreamWrapLLMCallContractIsAdvertisedAndEnforced() {
		// A FlightRecorder-style replay that returns the recorded chat STRUCT used to reach
		// parseBedrockEventStream(), which ran reMatchNoCase() over it and died with a cast error
		// outside any try.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)

				beforeStreamFlag = ""
				wrapStreamFlag   = ""
				wrapTransport    = ""
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { beforeStreamFlag = ctx.stream ?: "missing" },
					"wrapLLMCall"  : ( ctx, handler ) => {
						wrapStreamFlag = ctx.stream ?: "missing"
						wrapTransport  = ctx.transport ?: ""
						// deliberately wrong: the sync-shaped canned chat response
						return { "content": [ { "type": "text", "text": "replayed" } ], "stop_reason": "end_turn" }
					}
				} )

				errType = ""
				errMsg  = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
				threwStreamError = errType == "BedrockStreamError"
				namesStruct      = errMsg.findNoCase( "struct" ) > 0
				namesContract    = errMsg.findNoCase( "context.stream" ) > 0
				beforeSawStream  = beforeStreamFlag == true
				wrapSawStream    = wrapStreamFlag == true
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "beforeSawStream" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "wrapSawStream" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "wrapTransport" ) ).toString() ).isEqualTo( "bedrock-event-stream" );
		assertThat( variables.getAsBoolean( Key.of( "threwStreamError" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "namesStruct" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "namesContract" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "A truncated event-stream header block fails cleanly instead of being decoded as a normal event" )
	public void testTruncatedHeaderFrameThrows() {
		// One well-formed frame, then a frame cut off inside its header block: the prelude still
		// claims the full headersLength, so the header walk used to read past the end, have the
		// exception swallowed, and hand the half-decoded headers on as an ordinary event.
		byte[]	good	= frame(
		    chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"partial\"}}" ),
		    EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" );
		byte[]	full	= frame(
		    chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"never seen\"}}" ),
		    EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" );
		byte[]	cut		= java.util.Arrays.copyOf( full, 17 );
		String	body	= streamBody( good, cut );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" )
				} )

				errType = ""
				errMsg  = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
				threwStreamError = errType == "BedrockStreamError"
				namesMalformed   = errMsg.findNoCase( "truncated" ) > 0 || errMsg.findNoCase( "malformed" ) > 0
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION, body ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threwStreamError" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "namesMalformed" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "resumeToolBatchStream enforces the max-interactions ceiling instead of letting each resume cycle raise it" )
	public void testResumeToolBatchStreamHonoursMaxInteractions() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				toolRuns = 0
				tool = aiTool( "toolA", "Tool A", () => { toolRuns++; return "A done" } )
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				wrapCalls = 0
				resumeRequest = aiChatRequest(
					aiMessage().user( "run toolA" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{
						provider       : "bedrock",
						maxInteractions: 2,
						_resumeContext : {
							assistantMessage: {
								"role"   : "assistant",
								"content": [ { "type": "tool_use", "id": "tu_a", "name": "toolA", "input": {} } ]
							},
							resumeLedger: [ { toolName: "toolA", status: "execute" } ]
						}
					}
				)
				resumeRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => { wrapCalls++; return "" }
				} )

				errType = ""
				try {
					// interactionCount already at the ceiling: the resume must not buy another turn
					provider.chatStream( resumeRequest, ( chunk ) => {}, 2 )
				} catch ( any e ) {
					errType = e.type
				}
				threwMax     = errType == "MaxInteractionsExceeded"
				neverCalledLLM = wrapCalls == 0
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threwMax" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "neverCalledLLM" ) ) ).isTrue();
	}

	// ============================================================================================
	// Converse / ConverseStream API (#272)
	// ============================================================================================

	/** A Bedrock service configured for the (default) Converse API. */
	private String converseService() {
		return "aiService( \"bedrock\", { awsAccessKeyId: \"" + DUMMY_AWS_ACCESS_KEY_ID
		    + "\", awsSecretAccessKey: \"" + DUMMY_AWS_SECRET_ACCESS_KEY
		    + "\", region: \"" + DUMMY_AWS_REGION + "\" } )";
	}

	/** One ConverseStream frame: the event JSON is the RAW payload, named by its `:event-type` header. */
	private static byte[] converseFrame( String eventType, String json ) {
		return frame(
		    json.getBytes( java.nio.charset.StandardCharsets.UTF_8 ),
		    EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, eventType, ":content-type", "application/json"
		);
	}

	// ---- request transform ---------------------------------------------------------------------

	@Test
	@DisplayName( "Converse is the default API: the body is messages/content-blocks + inferenceConfig, on /converse" )
	public void testConverseIsDefaultAndBuildsInferenceConfig() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().system( "Be brief" ).user( "Hello" ),
					{ model: "amazon.nova-pro-v1:0", max_tokens: 128, temperature: 0.4, top_p: 0.9, stop: [ "STOP" ] },
					{ provider: "bedrock" }
				)
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( {
						"output": { "message": { "role": "assistant", "content": [ { "text": "Hi" } ] } },
						"stopReason": "end_turn",
						"usage": { "inputTokens": 4, "outputTokens": 2, "totalTokens": 6 }
					} )
				} )
				answer = provider.chat( chatRequest )

				hasAnthropicVersion = captured.keyExists( "anthropic_version" )
				systemText   = captured.system.first().text
				userRole     = captured.messages.first().role
				userText     = captured.messages.first().content.first().text
				maxTokens    = captured.inferenceConfig.maxTokens
				temperature  = captured.inferenceConfig.temperature
				topP         = captured.inferenceConfig.topP
				stopSeq      = captured.inferenceConfig.stopSequences.first()
				converseUrl  = provider.getConversePath( "amazon.nova-pro-v1:0" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "Hi" );
		assertThat( variables.getAsBoolean( Key.of( "hasAnthropicVersion" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "systemText" ) ).toString() ).isEqualTo( "Be brief" );
		assertThat( variables.get( Key.of( "userRole" ) ).toString() ).isEqualTo( "user" );
		assertThat( variables.get( Key.of( "userText" ) ).toString() ).isEqualTo( "Hello" );
		assertThat( variables.getAsInteger( Key.of( "maxTokens" ) ) ).isEqualTo( 128 );
		assertThat( variables.get( Key.of( "temperature" ) ).toString() ).isEqualTo( "0.4" );
		assertThat( variables.get( Key.of( "topP" ) ).toString() ).isEqualTo( "0.9" );
		assertThat( variables.get( Key.of( "stopSeq" ) ).toString() ).isEqualTo( "STOP" );
		assertThat( variables.get( Key.of( "converseUrl" ) ).toString() )
		    .isEqualTo( "/model/amazon.nova-pro-v1%3A0/converse" );
	}

	@Test
	@DisplayName( "Converse system prompt with cache_control emits a sibling cachePoint block" )
	public void testConverseSystemCachePoint() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				messages = aiMessage()
				messages.add( { role: "system", content: "Big reusable preamble", cache_control: { type: "ephemeral" } } )
				messages.user( "Go" )
				chatRequest = aiChatRequest( messages, { model: "anthropic.claude-3-sonnet-20240229-v1:0" }, { provider: "bedrock" } )
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( chatRequest )

				systemLen        = captured.system.len()
				firstIsText      = captured.system.first().keyExists( "text" )
				lastIsCachePoint = captured.system.last().keyExists( "cachePoint" )
				cachePointType   = captured.system.last().cachePoint.type
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "systemLen" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "firstIsText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "lastIsCachePoint" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "cachePointType" ) ).toString() ).isEqualTo( "default" );
	}

	@Test
	@DisplayName( "Converse toolConfig carries toolSpec.inputSchema.json and maps tool_choice onto the ToolChoice union" )
	public void testConverseToolConfigAndToolChoice() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => "sunny" )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "meta.llama3-70b-instruct-v1:0", tools: [ tool ], tool_choice: "required" },
					{ provider: "bedrock" }
				)
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( chatRequest )

				toolName    = captured.toolConfig.tools.first().toolSpec.name
				toolDesc    = captured.toolConfig.tools.first().toolSpec.description
				schemaType  = captured.toolConfig.tools.first().toolSpec.inputSchema.json.type
				hasCity     = captured.toolConfig.tools.first().toolSpec.inputSchema.json.properties.keyExists( "city" )
				choiceIsAny = captured.toolConfig.toolChoice.keyExists( "any" )

				// { type: "function", function: { name } } and { type: "tool", name } both force one tool
				fnChoice   = provider.converseToolChoice( { type: "function", function: { name: "getWeather" } } )
				toolChoice = provider.converseToolChoice( { type: "tool", name: "getWeather" } )
				autoChoice = provider.converseToolChoice( "auto" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "toolName" ) ).toString() ).isEqualTo( "getWeather" );
		assertThat( variables.get( Key.of( "toolDesc" ) ).toString() ).isEqualTo( "Get the weather" );
		assertThat( variables.get( Key.of( "schemaType" ) ).toString() ).isEqualTo( "object" );
		assertThat( variables.getAsBoolean( Key.of( "hasCity" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "choiceIsAny" ) ) ).isTrue();
		assertThat( variables.getAsStruct( Key.of( "fnChoice" ) ).getAsStruct( Key.of( "tool" ) ).get( Key.of( "name" ) ) )
		    .isEqualTo( "getWeather" );
		assertThat( variables.getAsStruct( Key.of( "toolChoice" ) ).getAsStruct( Key.of( "tool" ) ).get( Key.of( "name" ) ) )
		    .isEqualTo( "getWeather" );
		assertThat( variables.getAsStruct( Key.of( "autoChoice" ) ).containsKey( Key.of( "auto" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Converse structured output forces the synthetic tool for a NON-Claude family (no Claude-only gate)" )
	public void testConverseStructuredOutputForNonClaudeFamily() {
		// On the InvokeModel path this same request throws UnsupportedProviderCapability for
		// Titan/Llama and uses response_format for the OpenAI-shaped families. Converse has one
		// model-agnostic toolConfig, so the forced-tool trick applies to every family.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "amazon.nova-pro-v1:0" },
					{
						provider: "bedrock",
						schema: {
							"type": "object",
							"properties": { "name": { "type": "string" }, "age": { "type": "integer" } },
							"required": [ "name", "age" ]
						}
					}
				)
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( {
						"output": { "message": { "role": "assistant", "content": [
							{ "toolUse": { "toolUseId": "tu_1", "name": "structured_output", "input": { "name": "John Doe", "age": 30 } } }
						] } },
						"stopReason": "tool_use",
						"usage": { "inputTokens": 5, "outputTokens": 8, "totalTokens": 13 }
					} )
				} )
				result = provider.chat( chatRequest )

				forcedName    = captured.toolConfig.tools.last().toolSpec.name
				forcedChoice  = captured.toolConfig.toolChoice.tool.name
				noResponseFmt = !captured.keyExists( "response_format" )
				isStructRes   = isStruct( result )
				name          = result.name
				age           = result.age
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "forcedName" ) ).toString() ).isEqualTo( "structured_output" );
		assertThat( variables.get( Key.of( "forcedChoice" ) ).toString() ).isEqualTo( "structured_output" );
		assertThat( variables.getAsBoolean( Key.of( "noResponseFmt" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isStructRes" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@Test
	@DisplayName( "Params Converse does not model (thinking, top_k, anthropic_beta) go to additionalModelRequestFields" )
	public void testConverseAdditionalModelRequestFields() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "think" ),
					{
						model: "anthropic.claude-3-7-sonnet-20250219-v1:0",
						max_tokens: 2048,
						top_k: 40,
						anthropic_beta: [ "output-128k-2025-02-19" ],
						thinking: { type: "enabled", budget_tokens: 1024 }
					},
					{ provider: "bedrock", providerOptions: { additionalModelResponseFieldPaths: [ "/stop_sequence" ] } }
				)
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( chatRequest )

				thinkingType   = captured.additionalModelRequestFields.thinking.type
				thinkingBudget = captured.additionalModelRequestFields.thinking.budget_tokens
				topK           = captured.additionalModelRequestFields.top_k
				beta           = captured.additionalModelRequestFields.anthropic_beta.first()
				// max_tokens is native (inferenceConfig), so it must NOT be duplicated here
				noMaxTokens    = !captured.additionalModelRequestFields.keyExists( "max_tokens" )
				responsePath   = captured.additionalModelResponseFieldPaths.first()
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "thinkingType" ) ).toString() ).isEqualTo( "enabled" );
		assertThat( variables.getAsInteger( Key.of( "thinkingBudget" ) ) ).isEqualTo( 1024 );
		assertThat( variables.getAsInteger( Key.of( "topK" ) ) ).isEqualTo( 40 );
		assertThat( variables.get( Key.of( "beta" ) ).toString() ).isEqualTo( "output-128k-2025-02-19" );
		assertThat( variables.getAsBoolean( Key.of( "noMaxTokens" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "responsePath" ) ).toString() ).isEqualTo( "/stop_sequence" );
	}

	@Test
	@DisplayName( "Guardrail + performance config become Converse body fields, and the header form is dropped" )
	public void testConverseGuardrailAndPerformanceConfig() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{
						provider: "bedrock",
						providerOptions: {
							guardrailIdentifier: "gr-123",
							guardrailVersion: "DRAFT",
							guardrailTrace: "ENABLED",
							performanceConfigLatency: "optimized"
						}
					}
				)
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( chatRequest )

				guardrailId  = captured.guardrailConfig.guardrailIdentifier
				guardrailVer = captured.guardrailConfig.guardrailVersion
				guardrailTr  = captured.guardrailConfig.trace
				latency      = captured.performanceConfig.latency

				// Same providerOptions on the invoke path still produce the headers, unchanged
				invokeHeaders   = provider.buildProviderOptionHeaders( chatRequest.getProviderOptions(), "invoke" )
				converseHeaders = provider.buildProviderOptionHeaders( chatRequest.getProviderOptions(), "converse" )
				invokeHasGuard   = invokeHeaders.keyExists( "X-Amzn-Bedrock-GuardrailIdentifier" )
				converseHasGuard = converseHeaders.keyExists( "X-Amzn-Bedrock-GuardrailIdentifier" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "guardrailId" ) ).toString() ).isEqualTo( "gr-123" );
		assertThat( variables.get( Key.of( "guardrailVer" ) ).toString() ).isEqualTo( "DRAFT" );
		assertThat( variables.get( Key.of( "guardrailTr" ) ).toString() ).isEqualTo( "enabled" );
		assertThat( variables.get( Key.of( "latency" ) ).toString() ).isEqualTo( "optimized" );
		assertThat( variables.getAsBoolean( Key.of( "invokeHasGuard" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "converseHasGuard" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Multimodal: an OpenAI image_url data URI and an Anthropic base64 document become image/document blocks" )
	public void testConverseMultimodalBlocks() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				messages = aiMessage()
				messages.add( { role: "user", content: [
					{ "type": "text", "text": "What is in these?" },
					{ "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,QUJD" } },
					{ "type": "image", "source": { "type": "base64", "media_type": "image/png", "data": "REVG" } },
					{ "type": "document", "name": "Q3 report", "source": { "type": "base64", "media_type": "application/pdf", "data": "R0hJ" } },
					{ "type": "image_url", "image_url": { "url": "https://example.com/remote.png" } }
				] } )
				chatRequest = aiChatRequest( messages, { model: "anthropic.claude-3-sonnet-20240229-v1:0" }, { provider: "bedrock" } )
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( chatRequest )

				blocks       = captured.messages.first().content
				// the remote-URL image has no Converse representation and is dropped, not faked
				blockCount   = blocks.len()
				jpegFormat   = blocks[ 2 ].image.format
				jpegBytes    = blocks[ 2 ].image.source.bytes
				pngFormat    = blocks[ 3 ].image.format
				docFormat    = blocks[ 4 ].document.format
				docName      = blocks[ 4 ].document.name
				docBytes     = blocks[ 4 ].document.source.bytes
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "blockCount" ) ) ).isEqualTo( 4 );
		assertThat( variables.get( Key.of( "jpegFormat" ) ).toString() ).isEqualTo( "jpeg" );
		assertThat( variables.get( Key.of( "jpegBytes" ) ).toString() ).isEqualTo( "QUJD" );
		assertThat( variables.get( Key.of( "pngFormat" ) ).toString() ).isEqualTo( "png" );
		assertThat( variables.get( Key.of( "docFormat" ) ).toString() ).isEqualTo( "pdf" );
		assertThat( variables.get( Key.of( "docName" ) ).toString() ).isEqualTo( "Q3 report" );
		assertThat( variables.get( Key.of( "docBytes" ) ).toString() ).isEqualTo( "R0hJ" );
	}

	@Test
	@DisplayName( "Converse tool loop: toolUse -> toolResult follow-up, and the re-transform of Converse-shaped messages is idempotent" )
	public void testConverseToolLoopRoundTripIsIdempotent() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				toolRuns = 0
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => {
					toolRuns++
					return "sunny in " & city
				} )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				wrapCalls   = 0
				secondBody  = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { if ( wrapCalls == 1 ) { secondBody = ctx.dataPacket } },
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							return {
								"output": { "message": { "role": "assistant", "content": [
									{ "text": "Let me check." },
									{ "toolUse": { "toolUseId": "tu_9", "name": "getWeather", "input": { "city": "Paris" } } }
								] } },
								"stopReason": "tool_use",
								"usage": { "inputTokens": 7, "outputTokens": 11, "totalTokens": 18 }
							}
						}
						return {
							"output": { "message": { "role": "assistant", "content": [ { "text": "It is sunny in Paris" } ] } },
							"stopReason": "end_turn",
							"usage": { "inputTokens": 20, "outputTokens": 5, "totalTokens": 25 }
						}
					}
				} )

				answer = provider.chat( chatRequest )

				turns              = wrapCalls
				followUpMessages   = secondBody.messages.len()
				assistantRole      = secondBody.messages[ 2 ].role
				// The assistant turn's Converse blocks survive the second transform verbatim
				assistantToolUseId = secondBody.messages[ 2 ].content.last().toolUse.toolUseId
				assistantText      = secondBody.messages[ 2 ].content.first().text
				resultRole         = secondBody.messages[ 3 ].role
				resultToolUseId    = secondBody.messages[ 3 ].content.first().toolResult.toolUseId
				resultText         = secondBody.messages[ 3 ].content.first().toolResult.content.first().text
				// F3: `status` is emitted only for a FAILED tool result — "success" is Converse's
				// default and is not supported by every family, so a successful result omits it.
				resultHasStatus    = secondBody.messages[ 3 ].content.first().toolResult.keyExists( "status" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "toolRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "turns" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "followUpMessages" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "assistantRole" ) ).toString() ).isEqualTo( "assistant" );
		assertThat( variables.get( Key.of( "assistantText" ) ).toString() ).isEqualTo( "Let me check." );
		assertThat( variables.get( Key.of( "assistantToolUseId" ) ).toString() ).isEqualTo( "tu_9" );
		assertThat( variables.get( Key.of( "resultRole" ) ).toString() ).isEqualTo( "user" );
		assertThat( variables.get( Key.of( "resultToolUseId" ) ).toString() ).isEqualTo( "tu_9" );
		assertThat( variables.get( Key.of( "resultText" ) ).toString() ).isEqualTo( "sunny in Paris" );
		assertThat( variables.getAsBoolean( Key.of( "resultHasStatus" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "It is sunny in Paris" );
	}

	@Test
	@DisplayName( "Two Converse tool results are merged into ONE user turn, keeping the roles alternating" )
	public void testConverseMergesConsecutiveToolResults() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				a = aiTool( "toolA", "A", () => "resultA" )
				b = aiTool( "toolB", "B", () => "resultB" )
				chatRequest = aiChatRequest(
					aiMessage().user( "both please" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ a, b ] },
					{ provider: "bedrock" }
				)
				wrapCalls  = 0
				secondBody = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { if ( wrapCalls == 1 ) { secondBody = ctx.dataPacket } },
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							return {
								"output": { "message": { "role": "assistant", "content": [
									{ "toolUse": { "toolUseId": "tu_a", "name": "toolA", "input": {} } },
									{ "toolUse": { "toolUseId": "tu_b", "name": "toolB", "input": {} } }
								] } },
								"stopReason": "tool_use"
							}
						}
						return { "output": { "message": { "content": [ { "text": "done" } ] } }, "stopReason": "end_turn" }
					}
				} )
				provider.chat( chatRequest )

				messageCount   = secondBody.messages.len()
				lastRole       = secondBody.messages.last().role
				lastBlockCount = secondBody.messages.last().content.len()
				firstId        = secondBody.messages.last().content[ 1 ].toolResult.toolUseId
				secondId       = secondBody.messages.last().content[ 2 ].toolResult.toolUseId
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "messageCount" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "lastRole" ) ).toString() ).isEqualTo( "user" );
		assertThat( variables.getAsInteger( Key.of( "lastBlockCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "firstId" ) ).toString() ).isEqualTo( "tu_a" );
		assertThat( variables.get( Key.of( "secondId" ) ).toString() ).isEqualTo( "tu_b" );
	}

	// ---- response transform --------------------------------------------------------------------

	@Test
	@DisplayName( "transformResponseFromConverse maps every stopReason onto the normalized finish_reason" )
	public void testConverseStopReasonMapping() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				expected = {
					"end_turn": "stop",
					"tool_use": "tool_calls",
					"max_tokens": "length",
					"stop_sequence": "stop",
					"guardrail_intervened": "content_filter",
					"content_filtered": "content_filter",
					"model_context_window_exceeded": "length"
				}
				mismatches = []
				for ( key in expected ) {
					got = provider.transformResponseFromConverse(
						{ "output": { "message": { "content": [ { "text": "x" } ] } }, "stopReason": key },
						"anthropic.claude-3-sonnet-20240229-v1:0"
					).choices.first().finish_reason
					if ( got != expected[ key ] ) {
						mismatches.append( "#key# -> #got# (expected #expected[ key ]#)" )
					}
				}
				mismatchCount   = mismatches.len()
				mismatchSummary = mismatches.toList( char( 10 ) )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		if ( variables.getAsInteger( Key.of( "mismatchCount" ) ) > 0 ) {
			System.out.println( variables.get( Key.of( "mismatchSummary" ) ) );
		}
		assertThat( variables.getAsInteger( Key.of( "mismatchCount" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Converse response transform: text join, reasoning, tool_calls, cache-aware usage, metrics and trace" )
	public void testConverseResponseTransform() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				normalized = provider.transformResponseFromConverse(
					{
						"output": { "message": { "role": "assistant", "content": [
							{ "reasoningContent": { "reasoningText": { "text": "thinking hard", "signature": "sig-1" } } },
							{ "text": "first" },
							{ "text": "second" },
							{ "toolUse": { "toolUseId": "tu_1", "name": "getWeather", "input": { "city": "Paris" } } }
						] } },
						"stopReason": "tool_use",
						"usage": {
							"inputTokens": 10, "outputTokens": 4, "totalTokens": 114,
							"cacheReadInputTokens": 80, "cacheWriteInputTokens": 20
						},
						"metrics": { "latencyMs": 412 },
						"trace": { "guardrail": { "actionReason": "none" } }
					},
					"anthropic.claude-3-sonnet-20240229-v1:0"
				)
				content       = normalized.choices.first().message.content
				reasoning     = normalized.choices.first().message.reasoning
				finishReason  = normalized.choices.first().finish_reason
				toolCallName  = normalized.choices.first().message.tool_calls.first().function.name
				toolCallArgs  = normalized.choices.first().message.tool_calls.first().function.arguments
				promptTokens  = normalized.usage.prompt_tokens
				cacheRead     = normalized.usage.cache_read_input_tokens
				cacheWrite    = normalized.usage.cacheWriteInputTokens
				uncached      = normalized.usage.uncached_input_tokens
				totalTokens   = normalized.usage.total_tokens
				latencyMs     = normalized.bedrock.latencyMs
				traceReason   = normalized.bedrock.trace.guardrail.actionReason
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "content" ) ).toString() ).isEqualTo( "first\nsecond" );
		assertThat( variables.get( Key.of( "reasoning" ) ).toString() ).isEqualTo( "thinking hard" );
		assertThat( variables.get( Key.of( "finishReason" ) ).toString() ).isEqualTo( "tool_calls" );
		assertThat( variables.get( Key.of( "toolCallName" ) ).toString() ).isEqualTo( "getWeather" );
		assertThat( variables.get( Key.of( "toolCallArgs" ) ).toString() ).contains( "Paris" );
		// inputTokens EXCLUDES cached tokens on Converse: 10 + 80 + 20
		assertThat( variables.getAsInteger( Key.of( "promptTokens" ) ) ).isEqualTo( 110 );
		assertThat( variables.getAsInteger( Key.of( "cacheRead" ) ) ).isEqualTo( 80 );
		assertThat( variables.getAsInteger( Key.of( "cacheWrite" ) ) ).isEqualTo( 20 );
		assertThat( variables.getAsInteger( Key.of( "uncached" ) ) ).isEqualTo( 10 );
		assertThat( variables.getAsInteger( Key.of( "totalTokens" ) ) ).isEqualTo( 114 );
		assertThat( variables.getAsInteger( Key.of( "latencyMs" ) ) ).isEqualTo( 412 );
		assertThat( variables.get( Key.of( "traceReason" ) ).toString() ).isEqualTo( "none" );
	}

	// ---- ConverseStream ------------------------------------------------------------------------

	@Test
	@DisplayName( "ConverseStream: an exception thrown by the caller's stream callback propagates instead of being logged at debug" )
	public void testConverseStreamCallbackErrorPropagates() {
		String body = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"Hello\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"end_turn\"}" ),
		    converseFrame( "metadata", "{\"usage\":{\"inputTokens\":9,\"outputTokens\":3,\"totalTokens\":12},\"metrics\":{\"latencyMs\":88}}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )

				errType = ""
				errMsg  = ""
				try {
					// The readme idiom: fine on content chunks, throws on the usage-only metadata chunk
					// whose choices is []. That throw belongs to the caller and must surface.
					provider.chatStream( chatRequest, ( chunk ) => {
						if ( isStruct( chunk.usage ?: "" ) ) {
							throw( type: "CallerBoom", message: "caller callback failed" )
						}
					} )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "caller callback failed" );
	}

	@Test
	@DisplayName( "ConverseStream: text deltas, reasoning deltas, messageStop and metadata usage" )
	public void testConverseStreamTextReasoningAndMetadata() {
		String body = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"reasoningContent\":{\"text\":\"pondering\"}}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":1,\"delta\":{\"text\":\"Hello \"}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":1,\"delta\":{\"text\":\"world\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":1}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"end_turn\"}" ),
		    converseFrame( "metadata",
		        "{\"usage\":{\"inputTokens\":9,\"outputTokens\":3,\"totalTokens\":12,\"cacheReadInputTokens\":4,\"cacheWriteInputTokens\":0},\"metrics\":{\"latencyMs\":88}}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				withChoices = chunks.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
				text        = withChoices.map( c => c.choices.first().delta.content ?: "" ).toList( "" )
				reasoning   = withChoices.map( c => c.choices.first().delta.reasoning ?: "" ).toList( "" )
				sawStop     = withChoices.some( c => ( c.choices.first().finish_reason ?: "" ) == "stop" )
				usageChunks = chunks.filter( c => isStruct( c ) && isStruct( c.usage ?: "" ) )
				promptTok   = usageChunks.len() ? usageChunks.last().usage.prompt_tokens : -1
				cacheRead   = usageChunks.len() ? usageChunks.last().usage.cache_read_input_tokens : -1
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "text" ) ).toString() ).isEqualTo( "Hello world" );
		assertThat( variables.get( Key.of( "reasoning" ) ).toString() ).isEqualTo( "pondering" );
		assertThat( variables.getAsBoolean( Key.of( "sawStop" ) ) ).isTrue();
		// inputTokens (9) + cacheRead (4) + cacheWrite (0)
		assertThat( variables.getAsInteger( Key.of( "promptTok" ) ) ).isEqualTo( 13 );
		assertThat( variables.getAsInteger( Key.of( "cacheRead" ) ) ).isEqualTo( 4 );
	}

	/** A ConverseStream tool-call turn: contentBlockStart.toolUse, two input fragments, stop. */
	private String converseToolStreamBody() {
		return streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockStart",
		        "{\"contentBlockIndex\":0,\"start\":{\"toolUse\":{\"toolUseId\":\"tu_7\",\"name\":\"getWeather\"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"toolUse\":{\"input\":\"{\\\"city\\\":\\\"Par\"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"toolUse\":{\"input\":\"is\\\"}\"}}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"tool_use\"}" ),
		    converseFrame( "metadata", "{\"usage\":{\"inputTokens\":7,\"outputTokens\":11,\"totalTokens\":18}}" )
		);
	}

	private String converseTextStreamBody() {
		return streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"Sunny in Paris\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"end_turn\"}" )
		);
	}

	@Test
	@DisplayName( "ConverseStream toolUse is accumulated across start/delta/stop and drives the streaming tool loop" )
	public void testConverseStreamToolLoop() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				toolRuns = 0
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => {
					toolRuns++
					return "sunny in " & city
				} )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				wrapCalls    = 0
				capturedArgs = {}
				followUpBody = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { if ( wrapCalls == 1 ) { followUpBody = ctx.dataPacket } },
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return binaryDecode( wrapCalls == 1 ? "%s" : "%s", "base64" )
					},
					"beforeToolCall": ( ctx ) => { capturedArgs = ctx.toolArgs ?: {} }
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				toolChunks = chunks.filter( c =>
					isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len()
					&& isStruct( c.choices.first().delta ?: "" )
					&& c.choices.first().delta.keyExists( "tool_calls" )
				)
				emittedToolName = toolChunks.len() ? toolChunks.first().choices.first().delta.tool_calls.first().function.name : ""
				emittedToolId   = toolChunks.len() ? toolChunks.first().choices.first().delta.tool_calls.first().id : ""
				capturedCity    = capturedArgs.city ?: ""
				turns           = wrapCalls
				resultBlock     = followUpBody.messages.last().content.first().toolResult
				resultId        = resultBlock.toolUseId
				resultText      = resultBlock.content.first().text
				finalText       = chunks
					.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
					.map( c => c.choices.first().delta.content ?: "" )
					.toList( "" )
			""".formatted( converseService(), converseToolStreamBody(), converseTextStreamBody() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "emittedToolName" ) ).toString() ).isEqualTo( "getWeather" );
		assertThat( variables.get( Key.of( "emittedToolId" ) ).toString() ).isEqualTo( "tu_7" );
		assertThat( variables.get( Key.of( "capturedCity" ) ).toString() ).isEqualTo( "Paris" );
		assertThat( variables.getAsInteger( Key.of( "toolRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "turns" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "resultId" ) ).toString() ).isEqualTo( "tu_7" );
		assertThat( variables.get( Key.of( "resultText" ) ).toString() ).isEqualTo( "sunny in Paris" );
		assertThat( variables.get( Key.of( "finalText" ) ).toString() ).isEqualTo( "Sunny in Paris" );
	}

	@Test
	@DisplayName( "A ConverseStream validationException frame throws BedrockStreamError, not a silent short answer" )
	public void testConverseStreamExceptionFrame() {
		String body = streamBody(
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"partial\"}}" ),
		    frame( "{\"message\":\"Malformed input request\"}".getBytes( java.nio.charset.StandardCharsets.UTF_8 ),
		        EVENT_HEADERS_MSG, "exception", ":exception-type", "validationException", ":content-type", "application/json" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )

				errType = ""
				errMsg  = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "BedrockStreamError" );
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "validationException" );
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "Malformed input request" );
	}

	@Test
	@DisplayName( "ConverseStream tool batch suspends once and resumeToolBatchStream finishes it without replaying the LLM" )
	public void testConverseStreamSuspendAndResume() {
		String twoToolBody = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockStart",
		        "{\"contentBlockIndex\":0,\"start\":{\"toolUse\":{\"toolUseId\":\"tu_a\",\"name\":\"toolA\"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"toolUse\":{\"input\":\"{}\"}}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "contentBlockStart",
		        "{\"contentBlockIndex\":1,\"start\":{\"toolUse\":{\"toolUseId\":\"tu_b\",\"name\":\"toolB\"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":1,\"delta\":{\"toolUse\":{\"input\":\"{}\"}}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":1}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"tool_use\"}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				toolACalls = 0
				toolBCalls = 0
				toolA = aiTool( "toolA", "Tool A", () => { toolACalls++; return "A done" } )
				toolB = aiTool( "toolB", "Tool B", () => { toolBCalls++; return "B done" } )

				provider = %s

				wrapCalls  = 0
				followUpBody = {}
				llmMw = {
					"beforeLLMCall": ( ctx ) => { if ( wrapCalls == 1 ) { followUpBody = ctx.dataPacket } },
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return binaryDecode( wrapCalls == 1 ? "%s" : "%s", "base64" )
					}
				}

				chatRequest = aiChatRequest(
					aiMessage().user( "run both tools" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ toolA, toolB ] },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "toolA", "toolB" ], mode: "web" ) )
				chatRequest.addMiddleware( llmMw )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				stops         = chunks.filter( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
				suspended     = stops.len() == 1 && stops.first().result.isSuspended()
				suspendData   = suspended ? stops.first().result.getData() : {}
				pendingCount  = ( suspendData.pendingActions ?: [] ).len()
				ledgerCount   = ( suspendData.resumeLedger ?: [] ).len()
				// The captured assistant turn is in CONVERSE block shape, not Claude's tool_use blocks
				assistantBlocks = ( suspendData.assistantMessage.content ?: [] ).len()
				firstToolUseId  = assistantBlocks ? suspendData.assistantMessage.content.first().toolUse.toolUseId : ""
				neitherRanYet   = toolACalls == 0 && toolBCalls == 0
				calledLLMOnce   = wrapCalls == 1

				// ---- resume: approve toolA, block toolB; finish the SAME batch with no replay ----
				resumeRequest = aiChatRequest(
					aiMessage().user( "run both tools" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ toolA, toolB ] },
					{
						provider: "bedrock",
						_resumeContext: {
							assistantMessage: suspendData.assistantMessage,
							resumeLedger    : [
								{ toolName: "toolA", status: "execute" },
								{ toolName: "toolB", status: "blocked", reason: "denied by operator" }
							]
						}
					}
				)
				resumeRequest.addMiddleware( llmMw )

				resumeChunks = []
				provider.chatStream( resumeRequest, ( chunk ) => { resumeChunks.append( chunk ) } )

				finalText = resumeChunks
					.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
					.map( c => c.choices.first().delta.content ?: "" )
					.toList( "" )
				onlyARan              = toolACalls == 1 && toolBCalls == 0
				resumeAddedOneLLMCall = wrapCalls == 2
				// Both tool calls are answered, merged into ONE alternating user turn
				resultBlocks  = followUpBody.messages.last().content
				resultCount   = resultBlocks.len()
				okHasStatus   = resultBlocks.first().toolResult.keyExists( "status" )
				blockedStatus = resultBlocks.last().toolResult.status
			""".formatted( converseService(), twoToolBody, converseTextStreamBody() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspended" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "pendingCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "ledgerCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "assistantBlocks" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "firstToolUseId" ) ).toString() ).isEqualTo( "tu_a" );
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "calledLLMOnce" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "onlyARan" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "resumeAddedOneLLMCall" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "finalText" ) ).toString() ).isEqualTo( "Sunny in Paris" );
		assertThat( variables.getAsInteger( Key.of( "resultCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "okHasStatus" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "blockedStatus" ) ).toString() ).isEqualTo( "error" );
	}

	@Test
	@DisplayName( "ConverseStream structured output accumulates the forced tool block and populates the struct" )
	public void testConverseStreamStructuredOutput() {
		String body = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockStart",
		        "{\"contentBlockIndex\":0,\"start\":{\"toolUse\":{\"toolUseId\":\"tu_so\",\"name\":\"structured_output\"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"toolUse\":{\"input\":\"{\\\"name\\\":\\\"John \"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"toolUse\":{\"input\":\"Doe\\\",\\\"age\\\":30}\"}}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"tool_use\"}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "amazon.nova-pro-v1:0" },
					{
						provider: "bedrock",
						schema: {
							"type": "object",
							"properties": { "name": { "type": "string" }, "age": { "type": "integer" } },
							"required": [ "name", "age" ]
						}
					}
				)
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" )
				} )

				chunks = []
				result = provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				forcedChoice = captured.toolConfig.toolChoice.tool.name
				isStructRes  = isStruct( result )
				name         = result.name
				age          = result.age
				// the schema carrier is NEVER emitted as a tool_calls chunk
				toolChunks = chunks.filter( c =>
					isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len()
					&& isStruct( c.choices.first().delta ?: "" )
					&& c.choices.first().delta.keyExists( "tool_calls" )
				).len()
				soChunks = chunks.filter( c =>
					isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len()
					&& isStruct( c.choices.first().delta ?: "" )
					&& c.choices.first().delta.keyExists( "structured_output" )
				).len()
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "forcedChoice" ) ).toString() ).isEqualTo( "structured_output" );
		assertThat( variables.getAsBoolean( Key.of( "isStructRes" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
		assertThat( variables.getAsInteger( Key.of( "toolChunks" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "soChunks" ) ) ).isEqualTo( 1 );
	}

	// ---- API selection + fallback ---------------------------------------------------------------

	@Test
	@DisplayName( "A known no-Converse model id routes to InvokeModel even though Converse is the default" )
	public void testNoConverseModelFallsBackToInvoke() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "cohere.command-text-v14", max_tokens: 40 },
					{ provider: "bedrock" }
				)
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "generations": [ { "text": "hi", "finish_reason": "COMPLETE" } ] } )
				} )
				provider.chat( chatRequest )

				// legacy Cohere's { prompt } generate shape, not Converse's messages[]
				isInvokeShape = captured.keyExists( "prompt" ) && !captured.keyExists( "messages" )
				resolvedApi   = provider.resolveBedrockApi( chatRequest, "cohere.command-text-v14" )
				j2Api         = provider.resolveBedrockApi( chatRequest, "ai21.j2-ultra-v1" )
				novaApi       = provider.resolveBedrockApi( chatRequest, "amazon.nova-pro-v1:0" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isInvokeShape" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "resolvedApi" ) ).toString() ).isEqualTo( "invoke" );
		assertThat( variables.get( Key.of( "j2Api" ) ).toString() ).isEqualTo( "invoke" );
		assertThat( variables.get( Key.of( "novaApi" ) ).toString() ).isEqualTo( "converse" );
	}

	@Test
	@DisplayName( "A Converse ValidationException reporting an unsupported model retries ONCE over InvokeModel" )
	public void testConverseValidationExceptionFallsBackOnce() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 40 },
					{ provider: "bedrock" }
				)
				bodies = []
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						bodies.append( ctx.dataPacket )
						// First call is Converse (messages[] + no anthropic_version); answer with the
						// AWS ValidationException that says this model can't do it.
						if ( ctx.dataPacket.keyExists( "anthropic_version" ) ) {
							return {
								"content": [ { "type": "text", "text": "answered over InvokeModel" } ],
								"stop_reason": "end_turn",
								"usage": { "input_tokens": 3, "output_tokens": 4 }
							}
						}
						return { "error": { "message": "Bedrock request failed with status 400: {""message"":""ValidationException: This model doesn't support the Converse operation.""}" } }
					}
				} )
				answer = provider.chat( chatRequest )

				callCount     = bodies.len()
				firstWasConverse = bodies.first().keyExists( "messages" ) && !bodies.first().keyExists( "anthropic_version" )
				secondWasInvoke  = bodies.last().keyExists( "anthropic_version" )

				// A DIFFERENT error must never fall back
				throttled = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				throttleCalls = 0
				throttled.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						throttleCalls++
						return { "error": { "message": "Bedrock request failed with status 429: ThrottlingException" } }
					}
				} )
				threwProviderError = false
				try {
					provider.chat( throttled )
				} catch ( any e ) {
					threwProviderError = e.type == "ProviderError"
				}

				// shouldFallbackToInvoke is the single decision point, and is directly testable
				// The sentence must name the OPERATION, not just "the model" — see the on-demand
				// throughput case in testFallbackDecisionIgnoresModelIds().
				fallbackOnUnsupported = provider.shouldFallbackToInvoke( "ValidationException: The model does not support the Converse operation." )
				// A bare "the model does not support <feature>" is a feature-level 400: InvokeModel
				// would reject it just as hard, so it is surfaced rather than retried.
				fallbackOnFeature     = provider.shouldFallbackToInvoke( "ValidationException: The model does not support tool use." )
				fallbackOnThrottle    = provider.shouldFallbackToInvoke( "ThrottlingException: too many requests" )
				fallbackOnOtherValid  = provider.shouldFallbackToInvoke( "ValidationException: messages.0.content: field required" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "callCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "firstWasConverse" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "secondWasInvoke" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "answered over InvokeModel" );
		assertThat( variables.getAsInteger( Key.of( "throttleCalls" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "threwProviderError" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "fallbackOnUnsupported" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "fallbackOnFeature" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "fallbackOnThrottle" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "fallbackOnOtherValid" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "providerOptions.rawBody is an InvokeModel-only escape hatch and replaces the transform" )
	public void testRawBodyEscapeHatch() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{
						provider: "bedrock",
						providerOptions: { rawBody: { "anthropic_version": "bedrock-2023-05-31", "max_tokens": 7, "messages": [ { "role": "user", "content": "verbatim" } ] } }
					}
				)
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "content": [ { "type": "text", "text": "ok" } ], "stop_reason": "end_turn" } )
				} )
				provider.chat( chatRequest )

				api        = provider.resolveBedrockApi( chatRequest, "anthropic.claude-3-sonnet-20240229-v1:0" )
				maxTokens  = captured.max_tokens
				bodyText   = captured.messages.first().content
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "api" ) ).toString() ).isEqualTo( "invoke" );
		assertThat( variables.getAsInteger( Key.of( "maxTokens" ) ) ).isEqualTo( 7 );
		assertThat( variables.get( Key.of( "bodyText" ) ).toString() ).isEqualTo( "verbatim" );
	}

	@Test
	@DisplayName( "providerOptions.bedrockApi beats the configured service setting, in both directions" )
	public void testBedrockApiProviderOptionPrecedence() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				converseProvider = %s
				invokeProvider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				requestFor = ( opts ) => aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock", providerOptions: opts }
				)

				serviceDefault  = converseProvider.resolveBedrockApi( requestFor( {} ), "anthropic.claude-3-sonnet-20240229-v1:0" )
				serviceInvoke   = invokeProvider.resolveBedrockApi( requestFor( {} ), "anthropic.claude-3-sonnet-20240229-v1:0" )
				optionInvoke    = converseProvider.resolveBedrockApi( requestFor( { bedrockApi: "invoke" } ), "anthropic.claude-3-sonnet-20240229-v1:0" )
				optionConverse  = invokeProvider.resolveBedrockApi( requestFor( { bedrockApi: "converse" } ), "anthropic.claude-3-sonnet-20240229-v1:0" )
				// An unrecognized value is ignored, not thrown on: it falls through to the next source
				optionGarbage   = invokeProvider.resolveBedrockApi( requestFor( { bedrockApi: "nonsense" } ), "anthropic.claude-3-sonnet-20240229-v1:0" )
			""".formatted( converseService(), DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "serviceDefault" ) ).toString() ).isEqualTo( "converse" );
		assertThat( variables.get( Key.of( "serviceInvoke" ) ).toString() ).isEqualTo( "invoke" );
		assertThat( variables.get( Key.of( "optionInvoke" ) ).toString() ).isEqualTo( "invoke" );
		assertThat( variables.get( Key.of( "optionConverse" ) ).toString() ).isEqualTo( "converse" );
		assertThat( variables.get( Key.of( "optionGarbage" ) ).toString() ).isEqualTo( "invoke" );
	}

	@Test
	@DisplayName( "BOXLANG_MODULES_BXAI_BEDROCK_API beats the configured module/service setting" )
	public void testBedrockApiEnvVarPrecedence() {
		System.setProperty( "BOXLANG_MODULES_BXAI_BEDROCK_API", "invoke" );
		try {
			// @formatter:off
			executeWithTimeoutHandling(
				"""
					// The service is explicitly configured for converse; the env var must still win.
					provider = aiService(
						"bedrock",
						{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "converse" }
					)
					chatRequest = aiChatRequest(
						aiMessage().user( "hi" ),
						{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
						{ provider: "bedrock" }
					)
					envWins = provider.resolveBedrockApi( chatRequest, "anthropic.claude-3-sonnet-20240229-v1:0" )

					// ...but a per-request providerOption still outranks the env var.
					explicitRequest = aiChatRequest(
						aiMessage().user( "hi" ),
						{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
						{ provider: "bedrock", providerOptions: { bedrockApi: "converse" } }
					)
					optionWins = provider.resolveBedrockApi( explicitRequest, "anthropic.claude-3-sonnet-20240229-v1:0" )
				""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
				context
			);
			// @formatter:on

			assertThat( variables.get( Key.of( "envWins" ) ).toString() ).isEqualTo( "invoke" );
			assertThat( variables.get( Key.of( "optionWins" ) ).toString() ).isEqualTo( "converse" );
		} finally {
			System.clearProperty( "BOXLANG_MODULES_BXAI_BEDROCK_API" );
		}
	}

	@Test
	@DisplayName( "G2: the Converse capability gate is narrowed, not dropped — Titan tools and forced-toolChoice structured output still pre-flight" )
	public void testConverseCapabilityGateNarrowed() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => "sunny" )
				okBody = ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )

				// (1) Llama + tools on Converse: NOT gated any more. Converse has one
				// model-agnostic toolConfig, and AWS is the authority on whether the model can use it.
				llamaRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "meta.llama3-70b-instruct-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				llamaRequest.addMiddleware( { "wrapLLMCall": okBody } )
				llamaErrType = ""
				try {
					llamaAnswer = provider.chat( llamaRequest )
				} catch ( any e ) {
					llamaErrType = e.type
				}

				// (2) Titan + tools: still gated on BOTH APIs — Titan has no tool-use capability at all.
				titanConverse = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "amazon.titan-text-express-v1", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				titanConverse.addMiddleware( { "wrapLLMCall": okBody } )
				titanConverseErr = ""
				try {
					provider.chat( titanConverse )
				} catch ( any e ) {
					titanConverseErr = e.type
				}

				titanInvoke = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "amazon.titan-text-express-v1", tools: [ tool ] },
					{ provider: "bedrock", providerOptions: { bedrockApi: "invoke" } }
				)
				titanInvoke.addMiddleware( { "wrapLLMCall": okBody } )
				titanInvokeErr = ""
				try {
					provider.chat( titanInvoke )
				} catch ( any e ) {
					titanInvokeErr = e.type
				}

				// (3) Schema-typed structured output on Llama over Converse: gated. This module
				// implements it as a FORCED toolChoice, which Llama does not honour — the forced
				// block would simply never arrive, after a paid round trip.
				soRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "meta.llama3-70b-instruct-v1:0" },
					{ provider: "bedrock", schema: { "type": "object", "properties": { "city": { "type": "string" } }, "required": [ "city" ] } }
				)
				soRequest.addMiddleware( { "wrapLLMCall": okBody } )
				soErrType = ""
				try {
					provider.chat( soRequest )
				} catch ( any e ) {
					soErrType = e.type
				}
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "llamaErrType" ) ).toString() ).isEmpty();
		assertThat( variables.get( Key.of( "llamaAnswer" ) ).toString() ).isEqualTo( "ok" );
		assertThat( variables.get( Key.of( "titanConverseErr" ) ).toString() ).isEqualTo( "UnsupportedProviderCapability" );
		assertThat( variables.get( Key.of( "titanInvokeErr" ) ).toString() ).isEqualTo( "UnsupportedProviderCapability" );
		assertThat( variables.get( Key.of( "soErrType" ) ).toString() ).isEqualTo( "UnsupportedProviderCapability" );
	}

	// ============================================================================================
	// Converse review findings (#272 follow-up)
	// ============================================================================================

	@Test
	@DisplayName( "F1: a Converse failure once the conversation is in the Converse dialect surfaces as itself, with no InvokeModel retry" )
	public void testNoFallbackOnceConverseDialectMessagesExist() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)

				// Turn 2 of a tool loop: the recorded turns are Converse content blocks, which the
				// per-vendor InvokeModel transforms cannot express.
				msgs = chatRequest.getMessages()
				msgs.append( { "role": "assistant", "content": [
					{ "text": "checking" },
					{ "toolUse": { "toolUseId": "tu_1", "name": "getWeather", "input": { "city": "Paris" } } }
				] } )
				msgs.append( { "role": "user", "content": [
					{ "toolResult": { "toolUseId": "tu_1", "content": [ { "text": "sunny" } ] } }
				] } )
				chatRequest.setMessages( msgs )

				wrapCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return { "error": { "message": "Bedrock request failed with status 400: {""message"":""ValidationException: This model doesn't support the Converse operation.""}" } }
					}
				} )

				errMsg = ""
				errType = ""
				try {
					provider.chat( chatRequest )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}

				// The SAME request on turn 1 (no Converse-dialect blocks) still falls back.
				freshRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 20 },
					{ provider: "bedrock" }
				)
				freshCalls = 0
				freshRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						freshCalls++
						if ( ctx.dataPacket.keyExists( "anthropic_version" ) ) {
							return { "content": [ { "type": "text", "text": "over invoke" } ], "stop_reason": "end_turn" }
						}
						return { "error": { "message": "Bedrock request failed with status 400: {""message"":""ValidationException: This model doesn't support the Converse operation.""}" } }
					}
				} )
				freshAnswer = provider.chat( freshRequest )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "wrapCalls" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "ProviderError" );
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "doesn't support the Converse operation" );
		assertThat( variables.getAsInteger( Key.of( "freshCalls" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "freshAnswer" ) ).toString() ).isEqualTo( "over invoke" );
	}

	@Test
	@DisplayName( "F2: a validationException frame delivered mid-stream is NOT replayed over InvokeModel" )
	public void testMidStreamValidationExceptionIsNotRetried() {
		String midStreamFailure = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"Hel\"}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"lo\"}}" ),
		    frame(
		        "{\"message\":\"This model doesn't support the Converse operation.\"}"
		            .getBytes( java.nio.charset.StandardCharsets.UTF_8 ),
		        EVENT_HEADERS_MSG, "exception", ":exception-type", "validationException", ":content-type", "application/json" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				wrapCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return binaryDecode( "%s", "base64" )
					}
				} )

				chunks  = []
				errType = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )
				} catch ( any e ) {
					errType = e.type
				}

				textChunks = chunks.filter( c =>
					isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len()
					&& len( c.choices.first().delta.content ?: "" )
				)
				deliveredText = textChunks.map( c => c.choices.first().delta.content ).toList( "" )
				chunkCount    = textChunks.len()
			""".formatted( converseService(), midStreamFailure ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "BedrockStreamError" );
		assertThat( variables.getAsInteger( Key.of( "chunkCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "deliveredText" ) ).toString() ).isEqualTo( "Hello" );
		// The one thing that must NOT have happened: a second call replaying the same turn.
		assertThat( variables.getAsInteger( Key.of( "wrapCalls" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "F3: a FEATURE-level Converse 400 surfaces verbatim; only a model/operation one falls back" )
	public void testFeatureLevelValidationErrorsDoNotFallBack() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				wrapCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return { "error": { "message": "Bedrock request failed with status 400: {""message"":""ValidationException: This model does not support toolChoice.""}" } }
					}
				} )
				errMsg = ""
				try {
					provider.chat( chatRequest )
				} catch ( any e ) {
					errMsg = e.message
				}

				// The decision function itself
				featureToolChoice  = provider.shouldFallbackToInvoke( "ValidationException: This model does not support toolChoice." )
				featureToolResult  = provider.shouldFallbackToInvoke( "ValidationException: toolResult.status is not supported by this model." )
				featureImages      = provider.shouldFallbackToInvoke( "ValidationException: This model does not support image content blocks." )
				featureSystem      = provider.shouldFallbackToInvoke( "ValidationException: system messages are not supported by this model." )
				apiLevelModel      = provider.shouldFallbackToInvoke( "ValidationException: The model does not support Converse." )
				apiLevelOperation  = provider.shouldFallbackToInvoke( "ValidationException: This operation is not supported for this model." )
				unrelatedSentence  = provider.shouldFallbackToInvoke( "ValidationException: messages.0.content: field required" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "wrapCalls" ) ) ).isEqualTo( 1 );
		// AWS's own diagnosis reaches the caller, not this module's UnsupportedProviderCapability
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "does not support toolChoice" );
		assertThat( variables.getAsBoolean( Key.of( "featureToolChoice" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "featureToolResult" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "featureImages" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "featureSystem" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "apiLevelModel" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "apiLevelOperation" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "unrelatedSentence" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "F4/G4: a ConverseStream fallback re-fires beforeLLMCall for the rebuilt invoke body, flagged fallbackRetry" )
	public void testStreamFallbackRefiresBeforeLLMCallWithFlag() {
		String claudeStream = streamBody(
		    frame( chunkPayload( "{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"over invoke\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"message_stop\"}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				beforeCalls = 0
				wrapCalls   = 0
				firstFlag   = "unset"
				secondFlag  = "unset"
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						beforeCalls++
						if ( beforeCalls == 1 ) { firstFlag  = toString( ctx.fallbackRetry ?: "missing" ) }
						if ( beforeCalls == 2 ) { secondFlag = toString( ctx.fallbackRetry ?: "missing" ) }
					},
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							throw(
								type   : "ProviderError",
								message: "Bedrock stream request failed with status 400: {""message"":""ValidationException: This model doesn't support the ConverseStream operation.""}"
							)
						}
						return binaryDecode( "%s", "base64" )
					}
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )
				text = chunks
					.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
					.map( c => c.choices.first().delta.content ?: "" )
					.toList( "" )
			""".formatted( converseService(), claudeStream ),
			context
		);
		// @formatter:on

		// One logical call, two legs: the invoke leg's body was rebuilt from scratch, so the hook
		// gets to see (and re-edit) it — told apart by ctx.fallbackRetry.
		assertThat( variables.getAsInteger( Key.of( "beforeCalls" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "firstFlag" ) ).toString() ).isEqualTo( "false" );
		assertThat( variables.get( Key.of( "secondFlag" ) ).toString() ).isEqualTo( "true" );
		assertThat( variables.getAsInteger( Key.of( "wrapCalls" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "text" ) ).toString() ).isEqualTo( "over invoke" );
	}

	@Test
	@DisplayName( "F5: total_tokens is prompt + completion on BOTH paths, with the raw AWS total kept under bedrock.totalTokens" )
	public void testTotalTokensIsCacheInclusiveOnBothPaths() {
		String cachedUsageStream = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"hi\"}}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"end_turn\"}" ),
		    converseFrame( "metadata",
		        "{\"usage\":{\"inputTokens\":10,\"outputTokens\":5,\"totalTokens\":15,\"cacheReadInputTokens\":4,\"cacheWriteInputTokens\":2}}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				syncRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "amazon.nova-pro-v1:0" },
					{ provider: "bedrock", returnFormat: "raw" }
				)
				syncRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => ( {
						"output": { "message": { "content": [ { "text": "hi" } ] } },
						"stopReason": "end_turn",
						"usage": { "inputTokens": 10, "outputTokens": 5, "totalTokens": 15, "cacheReadInputTokens": 4, "cacheWriteInputTokens": 2 }
					} )
				} )
				raw = provider.chat( syncRequest )
				syncPrompt     = raw.usage.prompt_tokens
				syncCompletion = raw.usage.completion_tokens
				syncTotal      = raw.usage.total_tokens
				syncAwsTotal   = raw.bedrock.totalTokens

				streamRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "amazon.nova-pro-v1:0" },
					{ provider: "bedrock" }
				)
				streamRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )
				streamChunks = []
				provider.chatStream( streamRequest, ( chunk ) => { streamChunks.append( chunk ) } )
				usageChunks     = streamChunks.filter( c => isStruct( c ) && isStruct( c.usage ?: "" ) )
				streamPrompt    = usageChunks.last().usage.prompt_tokens
				streamTotal     = usageChunks.last().usage.total_tokens
				streamAwsTotal  = usageChunks.last().usage.bedrock.totalTokens
			""".formatted( converseService(), cachedUsageStream ),
			context
		);
		// @formatter:on

		// prompt is cache-INCLUSIVE (10 + 4 + 2), so the total must be too
		assertThat( variables.getAsInteger( Key.of( "syncPrompt" ) ) ).isEqualTo( 16 );
		assertThat( variables.getAsInteger( Key.of( "syncCompletion" ) ) ).isEqualTo( 5 );
		assertThat( variables.getAsInteger( Key.of( "syncTotal" ) ) ).isEqualTo( 21 );
		assertThat( variables.getAsInteger( Key.of( "syncAwsTotal" ) ) ).isEqualTo( 15 );
		assertThat( variables.getAsInteger( Key.of( "streamPrompt" ) ) ).isEqualTo( 16 );
		assertThat( variables.getAsInteger( Key.of( "streamTotal" ) ) ).isEqualTo( 21 );
		assertThat( variables.getAsInteger( Key.of( "streamAwsTotal" ) ) ).isEqualTo( 15 );
	}

	@Test
	@DisplayName( "F6: a document format is derived from the raw file name before the name is stripped of its dot" )
	public void testDocumentFormatSurvivesNameNormalization() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "amazon.nova-pro-v1:0" },
					{ provider: "bedrock" }
				)
				chatRequest.setMessages( [ { "role": "user", "content": [
					{ "type": "text", "text": "summarize" },
					{ "type": "document", "name": "Q3 report.docx", "source": { "media_type": "application/octet-stream", "data": "QUJD" } },
					{ "type": "document", "name": "sheet.xlsx", "source": { "media_type": "application/octet-stream", "data": "QUJD" } }
				] } ] )
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( chatRequest )

				blocks     = captured.messages.first().content
				docFormat  = blocks[ 2 ].document.format
				docName    = blocks[ 2 ].document.name
				xlsxFormat = blocks[ 3 ].document.format
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "docFormat" ) ).toString() ).isEqualTo( "docx" );
		// The name itself is still normalized — "." is not in Converse's allowed character set
		assertThat( variables.get( Key.of( "docName" ) ).toString() ).isEqualTo( "Q3 report docx" );
		assertThat( variables.get( Key.of( "xlsxFormat" ) ).toString() ).isEqualTo( "xlsx" );
	}

	@Test
	@DisplayName( "F7: guardrailConfig.streamProcessingMode is sent on ConverseStream only" )
	public void testStreamProcessingModeIsStreamOnly() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				guardOptions = {
					guardrailIdentifier          : "gr-123",
					guardrailVersion             : "2",
					guardrailStreamProcessingMode: "async"
				}

				syncRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "amazon.nova-pro-v1:0" },
					{ provider: "bedrock", providerOptions: guardOptions }
				)
				syncBody = {}
				syncRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { syncBody = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( syncRequest )

				streamRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "amazon.nova-pro-v1:0" },
					{ provider: "bedrock", providerOptions: guardOptions }
				)
				streamedBody = {}
				streamRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { streamedBody = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" )
				} )
				provider.chatStream( streamRequest, ( chunk ) => {} )

				syncHasMode   = syncBody.guardrailConfig.keyExists( "streamProcessingMode" )
				syncGuardId   = syncBody.guardrailConfig.guardrailIdentifier
				streamHasMode = streamedBody.guardrailConfig.keyExists( "streamProcessingMode" )
				streamMode    = streamedBody.guardrailConfig.streamProcessingMode
			""".formatted( converseService(), converseTextStreamBody() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "syncHasMode" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "syncGuardId" ) ).toString() ).isEqualTo( "gr-123" );
		assertThat( variables.getAsBoolean( Key.of( "streamHasMode" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "streamMode" ) ).toString() ).isEqualTo( "async" );
	}

	@Test
	@DisplayName( "F8: a ConverseStream replayed as {eventType,bytes} string frames yields text and toolUse" )
	public void testConverseStringFrameReplayCarriesEventType() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				evt = ( type, data ) => '{"eventType":"' & type & '","bytes":"'
					& binaryEncode( charsetDecode( jsonSerialize( data ), "utf-8" ), "base64" ) & '"}'

				toolStream = evt( "messageStart", { "role": "assistant" } )
					& evt( "contentBlockDelta", { "contentBlockIndex": 0, "delta": { "text": "Let me check." } } )
					& evt( "contentBlockStart", { "contentBlockIndex": 1, "start": { "toolUse": { "toolUseId": "tu_r1", "name": "getWeather" } } } )
					& evt( "contentBlockDelta", { "contentBlockIndex": 1, "delta": { "toolUse": { "input": '{"city":"Paris"}' } } } )
					& evt( "contentBlockStop", { "contentBlockIndex": 1 } )
					& evt( "messageStop", { "stopReason": "tool_use" } )
				textStream = evt( "messageStart", { "role": "assistant" } )
					& evt( "contentBlockDelta", { "contentBlockIndex": 0, "delta": { "text": "Sunny in Paris" } } )
					& evt( "messageStop", { "stopReason": "end_turn" } )

				toolRuns = 0
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => {
					toolRuns++
					return "sunny in " & city
				} )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				wrapCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return wrapCalls == 1 ? toolStream : textStream
					}
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				withChoices = chunks.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
				replayText  = withChoices.map( c => c.choices.first().delta.content ?: "" ).toList( "" )
				toolChunks  = withChoices.filter( c => isStruct( c.choices.first().delta ?: "" ) && c.choices.first().delta.keyExists( "tool_calls" ) )
				toolName    = toolChunks.len() ? toolChunks.first().choices.first().delta.tool_calls.first().function.name : ""
				turns       = wrapCalls
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "replayText" ) ).toString() ).isEqualTo( "Let me check.Sunny in Paris" );
		assertThat( variables.get( Key.of( "toolName" ) ).toString() ).isEqualTo( "getWeather" );
		assertThat( variables.getAsInteger( Key.of( "toolRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "turns" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "F9: toolResult.status comes from the loop's own outcome, not from sniffing the marker text" )
	public void testToolResultStatusIsExplicit() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				// No tools registered: the model asks for one that cannot be found.
				chatRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				wrapCalls  = 0
				secondBody = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { if ( wrapCalls == 1 ) { secondBody = ctx.dataPacket } },
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							return {
								"output": { "message": { "role": "assistant", "content": [
									{ "toolUse": { "toolUseId": "tu_missing", "name": "ghostTool", "input": {} } }
								] } },
								"stopReason": "tool_use"
							}
						}
						return { "output": { "message": { "content": [ { "text": "done" } ] } }, "stopReason": "end_turn" }
					}
				} )
				provider.chat( chatRequest )

				missingResult = secondBody.messages.last().content.first().toolResult
				missingStatus = missingResult.status
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "missingStatus" ) ).toString() ).isEqualTo( "error" );
	}

	@Test
	@DisplayName( "F10: a JSON STRING rawBody is honoured; invalid JSON fails with a clear message" )
	public void testStringRawBodyIsDeserialized() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{
						provider: "bedrock",
						providerOptions: { rawBody: '{"anthropic_version":"bedrock-2023-05-31","max_tokens":7,"messages":[{"role":"user","content":"verbatim"}]}' }
					}
				)
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "content": [ { "type": "text", "text": "ok" } ], "stop_reason": "end_turn" } )
				} )
				provider.chat( chatRequest )
				maxTokens = captured.max_tokens
				bodyText  = captured.messages.first().content

				badRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock", providerOptions: { rawBody: "{ not json at all" } }
				)
				badRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => ( { "content": [], "stop_reason": "end_turn" } ) } )
				badMsg = ""
				try {
					provider.chat( badRequest )
				} catch ( any e ) {
					badMsg = e.message
				}
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "maxTokens" ) ) ).isEqualTo( 7 );
		assertThat( variables.get( Key.of( "bodyText" ) ).toString() ).isEqualTo( "verbatim" );
		assertThat( variables.get( Key.of( "badMsg" ) ).toString() ).contains( "not valid JSON" );
	}

	@Test
	@DisplayName( "F11a: a streamed Converse assistant turn keeps its text and signed reasoning, not just the toolUse blocks" )
	public void testStreamAssistantTurnKeepsTextAndReasoning() {
		String reasoningToolStream = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta",
		        "{\"contentBlockIndex\":0,\"delta\":{\"reasoningContent\":{\"text\":\"thinking hard\"}}}" ),
		    converseFrame( "contentBlockDelta",
		        "{\"contentBlockIndex\":0,\"delta\":{\"reasoningContent\":{\"signature\":\"sig-abc\"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":1,\"delta\":{\"text\":\"Let me check.\"}}" ),
		    converseFrame( "contentBlockStart",
		        "{\"contentBlockIndex\":2,\"start\":{\"toolUse\":{\"toolUseId\":\"tu_r\",\"name\":\"getWeather\"}}}" ),
		    converseFrame( "contentBlockDelta",
		        "{\"contentBlockIndex\":2,\"delta\":{\"toolUse\":{\"input\":\"{\\\"city\\\":\\\"Paris\\\"}\"}}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":2}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"tool_use\"}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => "sunny in " & city )
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				wrapCalls    = 0
				followUpBody = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { if ( wrapCalls == 1 ) { followUpBody = ctx.dataPacket } },
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return binaryDecode( wrapCalls == 1 ? "%s" : "%s", "base64" )
					}
				} )
				provider.chatStream( chatRequest, ( chunk ) => {} )

				assistantBlocks = followUpBody.messages[ 2 ].content
				blockCount      = assistantBlocks.len()
				reasoningText   = assistantBlocks[ 1 ].reasoningContent.reasoningText.text
				reasoningSig    = assistantBlocks[ 1 ].reasoningContent.reasoningText.signature
				assistantText   = assistantBlocks[ 2 ].text
				toolUseId       = assistantBlocks[ 3 ].toolUse.toolUseId
			""".formatted( converseService(), reasoningToolStream, converseTextStreamBody() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "blockCount" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "reasoningText" ) ).toString() ).isEqualTo( "thinking hard" );
		assertThat( variables.get( Key.of( "reasoningSig" ) ).toString() ).isEqualTo( "sig-abc" );
		assertThat( variables.get( Key.of( "assistantText" ) ).toString() ).isEqualTo( "Let me check." );
		assertThat( variables.get( Key.of( "toolUseId" ) ).toString() ).isEqualTo( "tu_r" );
	}

	@Test
	@DisplayName( "F11b/c: a video part becomes a Converse video block, and cache_control on a tool result emits a cachePoint" )
	public void testVideoBlockAndToolMessageCachePoint() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "amazon.nova-pro-v1:0" },
					{ provider: "bedrock" }
				)
				chatRequest.setMessages( [
					{ "role": "user", "content": [
						{ "type": "text", "text": "what happens here?" },
						{ "type": "video", "video": { "url": "data:video/quicktime;base64,QUJD" } }
					] },
					{ "role": "assistant", "content": "", "tool_calls": [
						{ "id": "tc_1", "type": "function", "function": { "name": "lookup", "arguments": "{}" } }
					] },
					{ "role": "tool", "tool_call_id": "tc_1", "content": "a long cached tool payload", "cache_control": { "type": "ephemeral" } }
				] )
				captured = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { captured = ctx.dataPacket },
					"wrapLLMCall": ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( chatRequest )

				videoBlock    = captured.messages.first().content[ 2 ].video
				videoFormat   = videoBlock.format
				videoBytes    = videoBlock.source.bytes
				toolTurn      = captured.messages.last().content
				toolBlocks    = toolTurn.len()
				hasCachePoint = toolTurn.last().keyExists( "cachePoint" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "videoFormat" ) ).toString() ).isEqualTo( "mov" );
		assertThat( variables.get( Key.of( "videoBytes" ) ).toString() ).isEqualTo( "QUJD" );
		assertThat( variables.getAsInteger( Key.of( "toolBlocks" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "hasCachePoint" ) ) ).isTrue();
	}

	// ============================================================================================
	// Converse review round 2 (#272 follow-up)
	// ============================================================================================

	@Test
	@DisplayName( "G1: header-form guardrail providerOptions (and bedrockHeaders) still reach Converse's guardrailConfig" )
	public void testConverseGuardrailFromHeaderForm() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				okBody = ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )

				headerRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock", providerOptions: {
						"X-Amzn-Bedrock-GuardrailIdentifier" : "gr-header",
						"X-Amzn-Bedrock-GuardrailVersion"    : "3",
						"X-Amzn-Bedrock-Trace"               : "ENABLED"
					} }
				)
				headerPacket = {}
				headerRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { headerPacket = ctx.dataPacket },
					"wrapLLMCall"  : okBody
				} )
				provider.chat( headerRequest )
				headerId      = headerPacket.guardrailConfig.guardrailIdentifier
				headerVersion = headerPacket.guardrailConfig.guardrailVersion
				headerTrace   = headerPacket.guardrailConfig.trace

				shorthandRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock", providerOptions: { bedrockHeaders: {
						"X-Amzn-Bedrock-GuardrailIdentifier" : "gr-shorthand",
						"X-Amzn-Bedrock-GuardrailVersion"    : "9"
					} } }
				)
				shorthandPacket = {}
				shorthandRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { shorthandPacket = ctx.dataPacket },
					"wrapLLMCall"  : okBody
				} )
				provider.chat( shorthandRequest )
				shorthandId = shorthandPacket.guardrailConfig.guardrailIdentifier

				// The camelCase form still wins when both are present
				bothRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock", providerOptions: {
						guardrailIdentifier                  : "gr-camel",
						guardrailVersion                     : "1",
						"X-Amzn-Bedrock-GuardrailIdentifier" : "gr-header"
					} }
				)
				bothPacket = {}
				bothRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { bothPacket = ctx.dataPacket },
					"wrapLLMCall"  : okBody
				} )
				provider.chat( bothRequest )
				camelWins = bothPacket.guardrailConfig.guardrailIdentifier
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "headerId" ) ).toString() ).isEqualTo( "gr-header" );
		assertThat( variables.get( Key.of( "headerVersion" ) ).toString() ).isEqualTo( "3" );
		assertThat( variables.get( Key.of( "headerTrace" ) ).toString() ).isEqualTo( "enabled" );
		assertThat( variables.get( Key.of( "shorthandId" ) ).toString() ).isEqualTo( "gr-shorthand" );
		assertThat( variables.get( Key.of( "camelWins" ) ).toString() ).isEqualTo( "gr-camel" );
	}

	@Test
	@DisplayName( "G3: an InvokeModel-shaped body reaching the Converse transform is a hard BedrockError, not an empty green envelope" )
	public void testConverseTransformRejectsInvokeShapedBody() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				// A Claude InvokeModel body — the exact shape a replay fixture recorded against the
				// other API hands back.
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => ( { "content": [ { "type": "text", "text": "invoke shaped" } ], "stop_reason": "end_turn" } )
				} )
				errType = ""
				errMsg  = ""
				try {
					provider.chat( chatRequest )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "BedrockError" );
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "not a Converse body" );
	}

	@Test
	@DisplayName( "G3b: a wrapLLMCall middleware returning a STRING for a sync call fails naming the contract" )
	public void testSyncWrapLLMCallMustReturnStruct() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => "just some text" } )
				errType = ""
				errMsg  = ""
				try {
					provider.chat( chatRequest )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "BedrockError" );
		assertThat( variables.get( Key.of( "errMsg" ) ).toString() ).contains( "NON-STREAMING" );
	}

	@Test
	@DisplayName( "G4a: the sync fallback re-fires beforeLLMCall over the REBUILT invoke body, flagged fallbackRetry" )
	public void testSyncFallbackRefiresBeforeLLMCallWithFlag() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 64 },
					{ provider: "bedrock" }
				)
				beforeCalls = 0
				wrapCalls   = 0
				firstFlag   = "unset"
				secondFlag  = "unset"
				retryPacket = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						beforeCalls++
						if ( beforeCalls == 1 ) { firstFlag = toString( ctx.fallbackRetry ?: "missing" ) }
						if ( beforeCalls == 2 ) {
							secondFlag = toString( ctx.fallbackRetry ?: "missing" )
							ctx.dataPacket[ "mwMarker" ] = "edited-on-retry"
						}
					},
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							return { "error": {
								"message"    : "Bedrock request failed with status 400: ValidationException: The model does not support Converse.",
								"statusCode" : 400
							} }
						}
						retryPacket = ctx.dataPacket
						return { "content": [ { "type": "text", "text": "over invoke" } ], "stop_reason": "end_turn" }
					}
				} )
				answer = provider.chat( chatRequest )

				retryHasEdit      = retryPacket.keyExists( "mwMarker" )
				retryIsInvokeBody = retryPacket.keyExists( "max_tokens" ) && !retryPacket.keyExists( "inferenceConfig" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "over invoke" );
		assertThat( variables.getAsInteger( Key.of( "beforeCalls" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "firstFlag" ) ).toString() ).isEqualTo( "false" );
		assertThat( variables.get( Key.of( "secondFlag" ) ).toString() ).isEqualTo( "true" );
		assertThat( variables.getAsInteger( Key.of( "wrapCalls" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "retryHasEdit" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "retryIsInvokeBody" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "G4b: a middleware-substituted call that errors on BOTH legs announces onAIError exactly once" )
	public void testFallbackAnnouncesErrorExactlyOnce() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				errorEvents = 0
				BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => ( { "error": {
						"message"    : "Bedrock request failed with status 400: ValidationException: The model does not support Converse.",
						"statusCode" : 400
					} } )
				} )
				errType = ""
				try {
					provider.chat( chatRequest )
				} catch ( any e ) {
					errType = e.type
				}
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "ProviderError" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "S3: a runtime fallback STICKS — turn 2 of the tool loop goes out over InvokeModel, not Converse again" )
	public void testFallbackSticksForTheNextTurn() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				toolRuns = 0
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => { toolRuns++; return "sunny in " & city } )

				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				wrapCalls  = 0
				turn2Body  = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							return { "error": {
								"message"    : "Bedrock request failed with status 400: ValidationException: The model does not support Converse.",
								"statusCode" : 400
							} }
						}
						if ( wrapCalls == 2 ) {
							// The invoke (Claude) leg asks for a tool
							return {
								"content" : [ { "type": "tool_use", "id": "tu_1", "name": "getWeather", "input": { "city": "Paris" } } ],
								"stop_reason" : "tool_use"
							}
						}
						turn2Body = ctx.dataPacket
						return { "content": [ { "type": "text", "text": "Sunny." } ], "stop_reason": "end_turn" }
					}
				} )
				answer = provider.chat( chatRequest )

				// Turn 2's body is a Claude InvokeModel body, NOT a Converse one — the fallback stuck.
				turn2IsInvoke = turn2Body.keyExists( "anthropic_version" ) && !turn2Body.keyExists( "inferenceConfig" )
				stickyApi     = provider.resolveBedrockApi( chatRequest, "anthropic.claude-3-sonnet-20240229-v1:0" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "Sunny." );
		assertThat( variables.getAsInteger( Key.of( "toolRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "wrapCalls" ) ) ).isEqualTo( 3 );
		assertThat( variables.getAsBoolean( Key.of( "turn2IsInvoke" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "stickyApi" ) ).toString() ).isEqualTo( "invoke" );
	}

	@Test
	@DisplayName( "S2: a resume reads the dialect the suspended turn was BUILT in, not whatever the config resolves to now" )
	public void testResumeUsesPersistedDialect() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				toolRuns = 0
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => { toolRuns++; return "sunny in " & city } )

				// The recorded assistant turn is in the CLAUDE (invoke) dialect, because that run
				// fell back at request time. The service default is still Converse.
				claudeTurn = {
					"role"    : "assistant",
					"content" : [
						{ "type": "text", "text": "Let me check." },
						{ "type": "tool_use", "id": "tu_1", "name": "getWeather", "input": { "city": "Paris" } }
					]
				}

				resumeRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{
						provider: "bedrock",
						_resumeContext: {
							assistantMessage: claudeTurn,
							resumeLedger    : [ { toolName: "getWeather", status: "execute" } ],
							suspendData     : { bedrockApi: "invoke", toolDialect: "claude" }
						}
					}
				)
				followUp = {}
				resumeRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						followUp = ctx.dataPacket
						return { "content": [ { "type": "text", "text": "Sunny." } ], "stop_reason": "end_turn" }
					}
				} )
				answer = provider.chat( resumeRequest )
				followUpIsInvoke = followUp.keyExists( "anthropic_version" ) && !followUp.keyExists( "inferenceConfig" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		// Without the persisted dialect the resume re-resolves "converse", reads ZERO tool calls
		// off a Claude-shaped turn, and the approved tool is silently skipped.
		assertThat( variables.getAsInteger( Key.of( "toolRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "Sunny." );
		assertThat( variables.getAsBoolean( Key.of( "followUpIsInvoke" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "S2b: a resume ledger that does not match the recorded turn's tool calls throws ResumeLedgerMismatch" )
	public void testResumeLedgerMismatchThrows() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				toolRuns = 0
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => { toolRuns++; return "sunny" } )

				claudeTurn = {
					"role"    : "assistant",
					"content" : [
						{ "type": "tool_use", "id": "tu_1", "name": "getWeather", "input": { "city": "Paris" } },
						{ "type": "tool_use", "id": "tu_2", "name": "getWeather", "input": { "city": "Rome" } }
					]
				}

				resumeRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{
						provider: "bedrock",
						_resumeContext: {
							assistantMessage: claudeTurn,
							resumeLedger    : [ { toolName: "getWeather", status: "execute" } ],
							suspendData     : { bedrockApi: "invoke", toolDialect: "claude" }
						}
					}
				)
				resumeRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => ( { "content": [ { "type": "text", "text": "x" } ], "stop_reason": "end_turn" } ) } )
				errType = ""
				try {
					provider.chat( resumeRequest )
				} catch ( any e ) {
					errType = e.type
				}
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "ResumeLedgerMismatch" );
		assertThat( variables.getAsInteger( Key.of( "toolRuns" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "T2: model ids and ARNs in an AWS message no longer veto (or break) the fallback decision" )
	public void testFallbackDecisionIgnoresModelIds() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s

				// (a) the veto words live inside MODEL IDS, not in a field reference
				titanImage  = provider.shouldFallbackToInvoke( "ValidationException: The model amazon.titan-image-generator-v1 does not support the Converse operation." )
				stableImage = provider.shouldFallbackToInvoke( "ValidationException: Model stability.stable-image-core-v1:0 is not supported by Converse." )

				// (b) a dotted model id between the subject and the verb no longer breaks the window
				cohereText  = provider.shouldFallbackToInvoke( "ValidationException: The model cohere.command-text-v14 doesn't support the Converse operation." )
				arnForm     = provider.shouldFallbackToInvoke( "ValidationException: The model arn:aws:bedrock:us-east-1::foundation-model/amazon.titan-text-express-v1 does not support Converse." )

				// feature-level 400s still veto — AWS is describing THIS request accurately
				featureToolChoice = provider.shouldFallbackToInvoke( "ValidationException: This model does not support toolChoice." )
				featureToolResult = provider.shouldFallbackToInvoke( "ValidationException: toolResult.status is not supported by this model." )
				featureImages     = provider.shouldFallbackToInvoke( "ValidationException: This model does not support image content blocks." )
				featureSystem     = provider.shouldFallbackToInvoke( "ValidationException: system messages are not supported by this model." )
				unrelated         = provider.shouldFallbackToInvoke( "ValidationException: messages.0.content: field required" )
				notValidation     = provider.shouldFallbackToInvoke( "ThrottlingException: Too many requests, the model is not supported right now" )

				// R1: the single most common Bedrock 400 there is. It is a ValidationException, it
				// says "isn't supported", and the subject reads as "model ID <id>" — but it is
				// about THROUGHPUT MODE, not about the API. InvokeModel rejects it identically, and
				// falling back throws away AWS's own remediation ("use an inference profile").
				onDemandThroughput = provider.shouldFallbackToInvoke(
					"ValidationException: Invocation of model ID anthropic.claude-3-5-sonnet-20241022-v2:0 with on-demand throughput isn't supported. Retry your request with the ID or ARN of an inference profile that contains this model."
				)
				// The same message shape without the hyphen, and the provisioned-throughput sibling
				onDemandNoHyphen   = provider.shouldFallbackToInvoke(
					"ValidationException: Invocation of model ID meta.llama3-70b-instruct-v1:0 with on demand throughput isn't supported."
				)
				provisioned        = provider.shouldFallbackToInvoke(
					"ValidationException: The provided model doesn't support provisioned throughput for the Converse operation."
				)
				// IAM denials are not an API-capability statement either
				notAuthorized      = provider.shouldFallbackToInvoke(
					"ValidationException: You are not authorized to perform the Converse operation with this model."
				)
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "titanImage" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "stableImage" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "cohereText" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "arnForm" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "featureToolChoice" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "featureToolResult" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "featureImages" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "featureSystem" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "unrelated" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "notValidation" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "onDemandThroughput" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "onDemandNoHyphen" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "provisioned" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "notAuthorized" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "M1: an AWS 400 whose exception name came from the x-amzn-errortype HEADER still triggers the fallback" )
	public void testHeaderOnlyErrorTypeTriggersFallback() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				// The message shape sendBedrockRequest() now builds when the body carries only
				// {"message":...} and the exception name arrived in the response header.
				headerShaped = provider.shouldFallbackToInvoke(
					"Bedrock request failed with status 400 [ValidationException]: {""message"":""This model does not support the Converse operation.""}"
				)
				// A header-typed throttle is still not a fallback trigger
				throttleShaped = provider.shouldFallbackToInvoke(
					"Bedrock request failed with status 429 [ThrottlingException]: {""message"":""Too many requests""}"
				)

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				wrapCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							return { "error": {
								"message"    : "Bedrock request failed with status 400 [ValidationException]: {""message"":""This model does not support the Converse operation.""}",
								"statusCode" : 400
							} }
						}
						return { "content": [ { "type": "text", "text": "recovered" } ], "stop_reason": "end_turn" }
					}
				} )
				answer = provider.chat( chatRequest )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "headerShaped" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "throttleShaped" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "recovered" );
		assertThat( variables.getAsInteger( Key.of( "wrapCalls" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "T1: Cohere folds only the TRAILING tool exchange into tool_results, and synthesizes unique ids per turn" )
	public void testCohereFoldsOnlyTrailingToolExchange() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "bedrock", { awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" } )
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => "sunny in " & city )

				// A conversation that ALREADY contains two completed tool exchanges plus a third
				// awaiting its follow-up turn.
				history = [
					{ role: "user", content: "turn 1" },
					{ role: "assistant", content: "", tool_calls: [ { id: "c1", type: "function", function: { name: "getWeather", arguments: "{""city"":""Paris""}" } } ] },
					{ role: "tool", tool_call_id: "c1", content: "sunny in Paris" },
					{ role: "assistant", content: "It is sunny in Paris." },
					{ role: "user", content: "turn 2" },
					{ role: "assistant", content: "", tool_calls: [ { id: "c2", type: "function", function: { name: "getWeather", arguments: "{""city"":""Rome""}" } } ] },
					{ role: "tool", tool_call_id: "c2", content: "rainy in Rome" },
					{ role: "assistant", content: "It is rainy in Rome." },
					{ role: "user", content: "turn 3" },
					{ role: "assistant", content: "", tool_calls: [ { id: "c3", type: "function", function: { name: "getWeather", arguments: "{""city"":""Oslo""}" } } ] },
					{ role: "tool", tool_call_id: "c3", content: "snowy in Oslo" }
				]

				chatRequest = aiChatRequest(
					history,
					{ model: "cohere.command-r-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				packet = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { packet = ctx.dataPacket },
					"wrapLLMCall"  : ( ctx, handler ) => ( { "text": "ok", "finish_reason": "COMPLETE" } )
				} )
				provider.chat( chatRequest )

				resultCount  = ( packet.tool_results ?: [] ).len()
				resultCity   = resultCount ? packet.tool_results.first().call.parameters.city : ""
				resultOutput = resultCount ? packet.tool_results.first().outputs.first().result : ""
				// Nothing from the earlier turns is replayed as a tool RESULT — but the earlier
				// rounds are narrated into chat_history as CHATBOT turns, so turn 3 still knows
				// what turns 1 and 2 asked for and were told (R12).
				historyRoles = ( packet.chat_history ?: [] ).map( h => h.role ).toList( "," )
				narratedTurn1 = ( packet.chat_history ?: [] ).some( h => ( h.message ?: "" ).findNoCase( "Tool getWeather returned: sunny in Paris" ) > 0 )
				narratedTurn2 = ( packet.chat_history ?: [] ).some( h => ( h.message ?: "" ).findNoCase( "Tool getWeather returned: rainy in Rome" ) > 0 )
				// Turn 3 is the TRAILING exchange: it is tool_results, never chat_history
				narratedTurn3 = ( packet.chat_history ?: [] ).some( h => ( h.message ?: "" ).findNoCase( "snowy in Oslo" ) > 0 )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "resultCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "resultCity" ) ).toString() ).isEqualTo( "Oslo" );
		assertThat( variables.get( Key.of( "resultOutput" ) ).toString() ).isEqualTo( "snowy in Oslo" );
		// Per completed round: the narrated tool result, then the assistant's own prose.
		assertThat( variables.get( Key.of( "historyRoles" ) ).toString() ).isEqualTo( "USER,CHATBOT,CHATBOT,USER,CHATBOT,CHATBOT,USER" );
		assertThat( variables.getAsBoolean( Key.of( "narratedTurn1" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "narratedTurn2" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "narratedTurn3" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "T1b: synthesized Cohere tool-call ids do not collide across tool-loop turns" )
	public void testCohereSyntheticToolIdsAreUniquePerTurn() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "bedrock", { awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" } )
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => "sunny in " & city )

				chatRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "cohere.command-r-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				wrapCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls <= 2 ) {
							return { "tool_calls": [ { "name": "getWeather", "parameters": { "city": "Paris" } } ], "finish_reason": "TOOL_CALL" }
						}
						return { "text": "done", "finish_reason": "COMPLETE" }
					}
				} )
				provider.chat( chatRequest )

				toolIds = chatRequest.getMessages()
					.filter( m => isStruct( m ) && ( m.role ?: "" ) == "tool" )
					.map( m => m.tool_call_id ?: "" )
				idCount    = toolIds.len()
				idsUnique  = toolIds.len() == toolIds.toList( "," ).listToArray( "," ).len() && toolIds.first() != toolIds.last()
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "idCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "idsUnique" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "M5: a string event-stream replay matches frames whatever order jsonSerialize put the keys in" )
	public void testStringFrameReplayIsKeyOrderIndependent() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				// Frames built the way a replaying middleware builds them: jsonSerialize(), which
				// emits struct keys ALPHABETICALLY — "bytes" before "eventType".
				frames = [
					{ "eventType": "messageStart",      "bytes": toBase64( '{"role":"assistant"}' ) },
					{ "eventType": "contentBlockDelta", "bytes": toBase64( '{"contentBlockIndex":0,"delta":{"text":"Hello from replay"}}' ) },
					{ "eventType": "contentBlockStop",  "bytes": toBase64( '{"contentBlockIndex":0}' ) },
					{ "eventType": "messageStop",       "bytes": toBase64( '{"stopReason":"end_turn"}' ) }
				].map( f => jsonSerialize( f ) ).toList( "" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				replayBody = frames
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => replayBody } )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )
				text = chunks
					.filter( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() )
					.map( c => c.choices.first().delta.content ?: "" )
					.toList( "" )
				sawStop = chunks.some( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() && ( c.choices.first().finish_reason ?: "" ) == "stop" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "text" ) ).toString() ).isEqualTo( "Hello from replay" );
		assertThat( variables.getAsBoolean( Key.of( "sawStop" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "S1: an invoke Claude stream records the WHOLE assistant turn — signed thinking, then text, then tool_use" )
	public void testInvokeClaudeStreamRecordsFullAssistantTurn() {
		String	toolStream	= streamBody(
		    frame( chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"thinking_delta\",\"thinking\":\"weighing it up\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"signature_delta\",\"signature\":\"sig-abc\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"content_block_delta\",\"index\":1,\"delta\":{\"type\":\"text_delta\",\"text\":\"Let me check.\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame(
		        chunkPayload(
		            "{\"type\":\"content_block_start\",\"index\":2,\"content_block\":{\"type\":\"tool_use\",\"id\":\"tu_1\",\"name\":\"getWeather\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload(
		        "{\"type\":\"content_block_delta\",\"index\":2,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"city\\\":\\\"Paris\\\"}\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"content_block_stop\",\"index\":2}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"tool_use\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" )
		);
		String	textStream	= streamBody(
		    frame( chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"Sunny.\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"message_stop\"}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "bedrock", { awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" } )
				toolRuns = 0
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => { toolRuns++; return "sunny in " & city } )

				chatRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)
				wrapCalls = 0
				followUp  = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { if ( wrapCalls == 1 ) { followUp = ctx.dataPacket } },
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return binaryDecode( wrapCalls == 1 ? "%s" : "%s", "base64" )
					}
				} )
				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				// followUp.messages: [ user, assistant(turn), user(tool_result) ]
				assistantTurn  = followUp.messages[ 2 ].content
				blockCount     = assistantTurn.len()
				firstType      = assistantTurn[ 1 ].type
				firstSignature = assistantTurn[ 1 ].signature ?: ""
				firstThinking  = assistantTurn[ 1 ].thinking ?: ""
				secondType     = assistantTurn[ 2 ].type
				secondText     = assistantTurn[ 2 ].text ?: ""
				thirdType      = assistantTurn[ 3 ].type
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION, toolStream, textStream ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "toolRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "blockCount" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "firstType" ) ).toString() ).isEqualTo( "thinking" );
		assertThat( variables.get( Key.of( "firstThinking" ) ).toString() ).isEqualTo( "weighing it up" );
		assertThat( variables.get( Key.of( "firstSignature" ) ).toString() ).isEqualTo( "sig-abc" );
		assertThat( variables.get( Key.of( "secondType" ) ).toString() ).isEqualTo( "text" );
		assertThat( variables.get( Key.of( "secondText" ) ).toString() ).isEqualTo( "Let me check." );
		assertThat( variables.get( Key.of( "thirdType" ) ).toString() ).isEqualTo( "tool_use" );
	}

	@Test
	@DisplayName( "M2/M3/M6: a scalar image_url, an ARRAY params.system, and numeric-STRING inference params all survive the Converse transform" )
	public void testConverseTransformCoercions() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					[ { role: "user", content: [ { type: "image_url", image_url: "data:image/jpeg;base64,QUJD" } ] } ],
					{
						model       : "anthropic.claude-3-sonnet-20240229-v1:0",
						system      : [ { type: "text", text: "You are terse.", cache_control: { type: "ephemeral" } } ],
						max_tokens  : "512",
						temperature : "0.7",
						top_p       : "0.9"
					},
					{ provider: "bedrock" }
				)
				packet = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { packet = ctx.dataPacket },
					"wrapLLMCall"  : ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( chatRequest )

				imageFormat = packet.messages.first().content.first().image.format
				imageBytes  = packet.messages.first().content.first().image.source.bytes
				systemCount = packet.system.len()
				systemText  = packet.system.first().text
				systemCache = packet.system.last().keyExists( "cachePoint" )
				serialized  = jsonSerialize( packet.inferenceConfig )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "imageFormat" ) ).toString() ).isEqualTo( "jpeg" );
		assertThat( variables.get( Key.of( "imageBytes" ) ).toString() ).isEqualTo( "QUJD" );
		assertThat( variables.getAsInteger( Key.of( "systemCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "systemText" ) ).toString() ).isEqualTo( "You are terse." );
		assertThat( variables.getAsBoolean( Key.of( "systemCache" ) ) ).isTrue();
		// Typed JSON numbers, not strings — Converse's inferenceConfig is a typed shape
		String inference = variables.get( Key.of( "serialized" ) ).toString();
		assertThat( inference ).contains( "512" );
		assertThat( inference ).doesNotContain( "\"512\"" );
		assertThat( inference ).doesNotContain( "\"0.7\"" );
		assertThat( inference ).doesNotContain( "\"0.9\"" );
	}

	@Test
	@DisplayName( "M8: a catch-all (non openai.*) model gets max_tokens, not max_completion_tokens" )
	public void testCatchAllFamilyEmitsMaxTokens() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService( "bedrock", { awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" } )
				okBody = ( ctx, handler ) => ( { "choices": [ { "message": { "role": "assistant", "content": "ok" }, "finish_reason": "stop" } ] } )

				catchAll = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "deepseek.r1-v1:0", max_tokens: 128 },
					{ provider: "bedrock" }
				)
				catchAllPacket = {}
				catchAll.addMiddleware( { "beforeLLMCall": ( ctx ) => { catchAllPacket = ctx.dataPacket }, "wrapLLMCall": okBody } )
				provider.chat( catchAll )

				openAI = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "openai.gpt-oss-120b-1:0", max_tokens: 128 },
					{ provider: "bedrock" }
				)
				openAIPacket = {}
				openAI.addMiddleware( { "beforeLLMCall": ( ctx ) => { openAIPacket = ctx.dataPacket }, "wrapLLMCall": okBody } )
				provider.chat( openAI )

				catchAllKey   = catchAllPacket.keyExists( "max_tokens" ) && !catchAllPacket.keyExists( "max_completion_tokens" )
				openAIKey     = openAIPacket.keyExists( "max_completion_tokens" ) && !openAIPacket.keyExists( "max_tokens" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "catchAllKey" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "openAIKey" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "T3: a caller-supplied tool result with no status is NOT sniffed for marker text and flagged as an error" )
	public void testToolResultWithoutStatusIsNotAnError() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				history = [
					{ role: "user", content: "run it" },
					{ role: "assistant", content: "", tool_calls: [ { id: "c1", type: "function", function: { name: "lookup", arguments: "{}" } } ] },
					{ role: "tool", tool_call_id: "c1", content: "[Tool call blocked: this is legitimate OUTPUT text, not a marker]" }
				]
				chatRequest = aiChatRequest( history, { model: "anthropic.claude-3-sonnet-20240229-v1:0" }, { provider: "bedrock" } )
				packet = {}
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { packet = ctx.dataPacket },
					"wrapLLMCall"  : ( ctx, handler ) => ( { "output": { "message": { "content": [ { "text": "ok" } ] } }, "stopReason": "end_turn" } )
				} )
				provider.chat( chatRequest )
				resultBlock = packet.messages.last().content.first().toolResult
				hasStatus   = resultBlock.keyExists( "status" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasStatus" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "M4: a ConverseStream stopReason reaches afterLLMCall as the NORMALIZED finish reason" )
	public void testConverseStreamFinishReasonNormalized() {
		String body = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"cut off\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"max_tokens\"}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				seenFinish = ""
				seenRaw    = ""
				chatRequest.addMiddleware( {
					"afterLLMCall": ( ctx ) => {
						seenFinish = ctx.streamState.finishReason ?: ""
						seenRaw    = ctx.streamState.stopReason ?: ""
					},
					"wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" )
				} )
				provider.chatStream( chatRequest, ( chunk ) => {} )
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "seenFinish" ) ).toString() ).isEqualTo( "length" );
		assertThat( variables.get( Key.of( "seenRaw" ) ).toString() ).isEqualTo( "max_tokens" );
	}

	@Test
	@DisplayName( "Converse sync chat(): malformed_model_output/malformed_tool_use map to finish_reason 'stop' and never start a tool loop" )
	public void testConverseSyncMalformedStopReasonsFinishAndSkipToolLoop() {
		// Both reasons signal the model itself produced something unusable (a broken message, or a
		// broken tool-call payload) — there are no toolUse content blocks to run, and CONVERSE_STOP_REASONS
		// maps both onto "stop" rather than "tool_calls" or an error finish.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				tool = aiTool( "getWeather", "Get the weather", ( required string city ) => "sunny in " & city )
				expected = { "malformed_model_output": "stop", "malformed_tool_use": "stop" }
				mismatches = []
				for ( key in expected ) {
					wrapCalls = 0
					chatRequest = aiChatRequest(
						aiMessage().user( "weather in Paris?" ),
						{ model: "anthropic.claude-3-sonnet-20240229-v1:0", tools: [ tool ] },
						{ provider: "bedrock", returnFormat: "raw" }
					)
					chatRequest.addMiddleware( {
						"wrapLLMCall": ( ctx, handler ) => {
							wrapCalls++
							return {
								"output": { "message": { "role": "assistant", "content": [ { "text": "incomplete" } ] } },
								"stopReason": key,
								"usage": { "inputTokens": 5, "outputTokens": 2, "totalTokens": 7 }
							}
						}
					} )
					result = provider.chat( chatRequest )
					got = result.choices[ 1 ].finish_reason
					hasToolCalls = result.choices[ 1 ].message.keyExists( "tool_calls" )
					if ( got != expected[ key ] || wrapCalls != 1 || hasToolCalls ) {
						mismatches.append( "#key# -> finish=#got# wrapCalls=#wrapCalls# hasToolCalls=#hasToolCalls#" )
					}
				}
				mismatchCount   = mismatches.len()
				mismatchSummary = mismatches.toList( char( 10 ) )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		if ( variables.getAsInteger( Key.of( "mismatchCount" ) ) > 0 ) {
			System.out.println( variables.get( Key.of( "mismatchSummary" ) ) );
		}
		assertThat( variables.getAsInteger( Key.of( "mismatchCount" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "ConverseStream: malformed_model_output/malformed_tool_use messageStop emits finish_reason 'stop'" )
	public void testConverseStreamMalformedStopReasonsEmitStop() {
		String	malformedModelOutputBody	= streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"incomplete\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"malformed_model_output\"}" )
		);
		String	malformedToolUseBody		= streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"incomplete\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"malformed_tool_use\"}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s

				chunks1 = []
				req1 = aiChatRequest( aiMessage().user( "hi" ), { model: "anthropic.claude-3-sonnet-20240229-v1:0" }, { provider: "bedrock" } )
				req1.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )
				provider.chatStream( req1, ( chunk ) => { chunks1.append( chunk ) } )
				sawStop1 = chunks1.some( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() && ( c.choices.first().finish_reason ?: "" ) == "stop" )

				chunks2 = []
				req2 = aiChatRequest( aiMessage().user( "hi" ), { model: "anthropic.claude-3-sonnet-20240229-v1:0" }, { provider: "bedrock" } )
				req2.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )
				provider.chatStream( req2, ( chunk ) => { chunks2.append( chunk ) } )
				sawStop2 = chunks2.some( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() && ( c.choices.first().finish_reason ?: "" ) == "stop" )
			""".formatted( converseService(), malformedModelOutputBody, malformedToolUseBody ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawStop1" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawStop2" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "ConverseStream: model_context_window_exceeded -> 'length' and guardrail_intervened -> 'content_filter'" )
	public void testConverseStreamContextWindowAndGuardrailStopReasons() {
		String	contextWindowBody	= streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"too much\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"model_context_window_exceeded\"}" )
		);
		String	guardrailBody		= streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"blocked\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"guardrail_intervened\"}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s

				chunks1 = []
				req1 = aiChatRequest( aiMessage().user( "hi" ), { model: "anthropic.claude-3-sonnet-20240229-v1:0" }, { provider: "bedrock" } )
				req1.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )
				provider.chatStream( req1, ( chunk ) => { chunks1.append( chunk ) } )
				sawLength = chunks1.some( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() && ( c.choices.first().finish_reason ?: "" ) == "length" )

				chunks2 = []
				req2 = aiChatRequest( aiMessage().user( "hi" ), { model: "anthropic.claude-3-sonnet-20240229-v1:0" }, { provider: "bedrock" } )
				req2.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )
				provider.chatStream( req2, ( chunk ) => { chunks2.append( chunk ) } )
				sawContentFilter = chunks2.some( c => isStruct( c ) && isArray( c.choices ?: "" ) && c.choices.len() && ( c.choices.first().finish_reason ?: "" ) == "content_filter" )
			""".formatted( converseService(), contextWindowBody, guardrailBody ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawLength" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawContentFilter" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "R2a: a runtime fallback never mutates the caller's providerOptions struct, and the NEXT request re-resolves Converse" )
	public void testFallbackDoesNotPinCallerProviderOptions() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s

				// ONE providerOptions struct, reused for two requests — exactly what
				// AiBaseRunnable.getMergedOptions() does for every turn of an agent run.
				sharedProviderOptions = {}
				sharedOptions         = { provider: "bedrock", providerOptions: sharedProviderOptions }

				req1 = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					sharedOptions
				)
				calls = 0
				req1.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						calls++
						if ( calls == 1 ) {
							return { "error": {
								"message"    : "Bedrock request failed with status 400: ValidationException: The model does not support the Converse operation.",
								"statusCode" : 400
							} }
						}
						return { "content": [ { "type": "text", "text": "ok" } ], "stop_reason": "end_turn" }
					}
				} )
				provider.chat( req1 )

				// The caller's own struct must be untouched — a pin here outlives the request
				pinnedOnCaller = sharedProviderOptions.keyExists( "bedrockApi" )
				// ...while the decision is still sticky for the request that made it
				stuckOnReq1    = provider.resolveBedrockApi( req1, "anthropic.claude-3-sonnet-20240229-v1:0" )

				req2 = aiChatRequest(
					aiMessage().user( "hi again" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					sharedOptions
				)
				apiForReq2 = provider.resolveBedrockApi( req2, "anthropic.claude-3-sonnet-20240229-v1:0" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "calls" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsBoolean( Key.of( "pinnedOnCaller" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "stuckOnReq1" ) ).toString() ).isEqualTo( "invoke" );
		assertThat( variables.get( Key.of( "apiForReq2" ) ).toString() ).isEqualTo( "converse" );
	}

	@Test
	@DisplayName( "R2b: an explicit providerOptions.bedrockApi = converse pin survives a fallback and still governs the next request" )
	public void testExplicitConversePinSurvivesFallback() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s

				sharedProviderOptions = { bedrockApi: "converse" }
				sharedOptions         = { provider: "bedrock", providerOptions: sharedProviderOptions }

				req1 = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					sharedOptions
				)
				calls = 0
				req1.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						calls++
						if ( calls == 1 ) {
							return { "error": {
								"message"    : "Bedrock request failed with status 400: ValidationException: The model does not support the Converse operation.",
								"statusCode" : 400
							} }
						}
						return { "content": [ { "type": "text", "text": "ok" } ], "stop_reason": "end_turn" }
					}
				} )
				provider.chat( req1 )

				// The pin the caller wrote is still the pin the caller wrote
				pinStillConverse = sharedProviderOptions.bedrockApi
				// The runtime decision outranks it for THIS request only
				stuckOnReq1      = provider.resolveBedrockApi( req1, "anthropic.claude-3-sonnet-20240229-v1:0" )

				req2 = aiChatRequest(
					aiMessage().user( "hi again" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					sharedOptions
				)
				apiForReq2 = provider.resolveBedrockApi( req2, "anthropic.claude-3-sonnet-20240229-v1:0" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "pinStillConverse" ) ).toString() ).isEqualTo( "converse" );
		assertThat( variables.get( Key.of( "stuckOnReq1" ) ).toString() ).isEqualTo( "invoke" );
		assertThat( variables.get( Key.of( "apiForReq2" ) ).toString() ).isEqualTo( "converse" );
	}

	@Test
	@DisplayName( "R4b: the OpenAI-shaped invoke packet holds a COPY of the messages, not the live conversation array" )
	public void testOpenAiTransformCopiesMessages() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "openai.gpt-oss-20b-1:0" },
					{ provider: "bedrock" }
				)
				capturedPacket = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						capturedPacket = ctx.dataPacket
						return { "choices": [ { "message": { "role": "assistant", "content": "ok" }, "finish_reason": "stop" } ] }
					}
				} )
				provider.chat( chatRequest )

				lengthAtCapture = capturedPacket.messages.len()
				// The tool loop appends to the request's own array after the call; a packet that
				// ALIASED it would grow too, and anything holding the packet (a FlightRecorder
				// tape, a serialized suspend payload) would describe a request never sent.
				chatRequest.getMessages().append( { "role": "user", "content": "sent later" } )
				lengthAfterMutation = capturedPacket.messages.len()
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "lengthAtCapture" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "lengthAfterMutation" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "R6: Cohere Command R is gated for schema-typed structured output on the CONVERSE path too" )
	public void testCohereStructuredOutputGatedOnConverse() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "Extract: John Doe, age 30" ),
					{ model: "cohere.command-r-plus-v1:0" },
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

				// Plain TOOL CALLING on Command R over Converse is untouched by the gate
				toolRequest = aiChatRequest(
					aiMessage().user( "weather?" ),
					{ model: "cohere.command-r-plus-v1:0", tools: [ aiTool( "getWeather", "Get the weather", ( required string city ) => "sunny" ) ] },
					{ provider: "bedrock" }
				)
				toolRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => ( {
						"output": { "message": { "role": "assistant", "content": [ { "text": "it is sunny" } ] } },
						"stopReason": "end_turn"
					} )
				} )
				toolsThrew = false
				try {
					provider.chat( toolRequest )
				} catch( any e ) {
					toolsThrew = true
				}
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "caughtType" ) ).toString() ).isEqualTo( "UnsupportedProviderCapability" );
		assertThat( variables.get( Key.of( "caughtMsg" ) ).toString() ).contains( "structured output" );
		// The message must no longer send the caller to Command R as the fix for this
		assertThat( variables.get( Key.of( "caughtMsg" ) ).toString() ).doesNotContain( "or Cohere Command R" );
		assertThat( variables.getAsBoolean( Key.of( "toolsThrew" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "R8: a beforeLLMCall middleware that REPLACES context.dataPacket has its replacement sent on the first leg" )
	public void testBeforeLLMCallPacketReplacementIsSentOnFirstLeg() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				announcedPacket = {}
				BoxRegisterInterceptor( function( data ) { announcedPacket = data.dataPacket }, "onAIChatRequest" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock", timeout: 1 }
				)
				// A REPLACEMENT, not an in-place edit: the middleware hands back a whole new struct.
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => {
						ctx.dataPacket = { "messages": [ { "role": "user", "content": [ { "text": "replaced body" } ] } ], "_marker": "replaced" }
					}
				} )
				// The transport is deliberately NOT substituted — onAIChatRequest fires inside it,
				// with the packet that is actually about to go on the wire. The call itself then
				// fails (dummy credentials / no route), which is irrelevant to this assertion.
				try {
					provider.chat( chatRequest )
				} catch ( any e ) {
				}
				sentMarker = announcedPacket._marker ?: ""
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "sentMarker" ) ).toString() ).isEqualTo( "replaced" );
	}

	@Test
	@DisplayName( "R9: an errored call that no transport ever ran (middleware-substituted, no fallback possible) still announces onAIError" )
	public void testSubstitutedErrorAnnouncesWhenNoFallbackPossible() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				errorEvents = 0
				BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0" },
					{ provider: "bedrock" }
				)
				wrapCalls = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						return { "error": {
							"message"    : "Bedrock request failed with status 400: ValidationException: messages.0.content: field required",
							"statusCode" : 400
						} }
					}
				} )
				errType = ""
				try {
					// interactionCount > 0 makes mayFallback FALSE, so the transport was told to
					// announce its own error — but the middleware substituted the call, so no
					// transport ever ran and nobody announced anything.
					provider.chat( chatRequest, 1 )
				} catch ( any e ) {
					errType = e.type
				}
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "wrapCalls" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "ProviderError" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "R12: earlier Cohere tool rounds are narrated into chat_history; only the trailing round is tool_results" )
	public void testCohereEarlierToolRoundsSurviveInChatHistory() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "weather in Paris?" ),
					{ model: "cohere.command-r-plus-v1:0" },
					{ provider: "bedrock" }
				)
				// Three completed tool rounds, in the OpenAI shape the shared tool loop records.
				chatRequest.setMessages( [
					{ "role": "user", "content": "weather in Paris?" },
					{ "role": "assistant", "tool_calls": [ { "id": "c1", "type": "function", "function": { "name": "getWeather", "arguments": '{"city":"Paris"}' } } ] },
					{ "role": "tool", "tool_call_id": "c1", "content": "sunny in Paris" },
					{ "role": "user", "content": "and London?" },
					{ "role": "assistant", "tool_calls": [ { "id": "c2", "type": "function", "function": { "name": "getWeather", "arguments": '{"city":"London"}' } } ] },
					{ "role": "tool", "tool_call_id": "c2", "content": "rainy in London" },
					{ "role": "user", "content": "and Berlin?" },
					{ "role": "assistant", "tool_calls": [ { "id": "c3", "type": "function", "function": { "name": "getWeather", "arguments": '{"city":"Berlin"}' } } ] },
					{ "role": "tool", "tool_call_id": "c3", "content": "cold in Berlin" }
				] )
				capturedPacket = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						capturedPacket = ctx.dataPacket
						return { "text": "ok", "finish_reason": "COMPLETE" }
					}
				} )
				provider.chat( chatRequest )

				history        = capturedPacket.chat_history ?: []
				toolResultsLen = ( capturedPacket.tool_results ?: [] ).len()
				// Round 1's result is remembered as a CHATBOT turn rather than vanishing
				sawTurn1 = history.some( h => ( h.role ?: "" ) == "CHATBOT" && ( h.message ?: "" ).findNoCase( "sunny in Paris" ) > 0 )
				sawTurn2 = history.some( h => ( h.role ?: "" ) == "CHATBOT" && ( h.message ?: "" ).findNoCase( "rainy in London" ) > 0 )
				// ...and it names the tool that produced it
				namesTool = history.some( h => ( h.role ?: "" ) == "CHATBOT" && ( h.message ?: "" ).findNoCase( "Tool getWeather returned" ) > 0 )
				// Only the TRAILING round is replayed as tool_results
				sawTurn3AsResult = history.some( h => ( h.message ?: "" ).findNoCase( "cold in Berlin" ) > 0 )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "toolResultsLen" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "sawTurn1" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawTurn2" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "namesTool" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawTurn3AsResult" ) ) ).isFalse();
	}

	// ============================================================================================
	// Tool-argument resolution, resume ledger and streaming context parity
	// ============================================================================================

	@Test
	@DisplayName( "T1: a beforeToolCall middleware that serializes ctx.toolArgs falls back to the model's own arguments, never {}" )
	public void testConverseNonStructToolArgsFallsBackToNativeArgs() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.middleware.AiMiddlewareResult;

				provider = %s
				seenCity = ""
				tool = aiTool( "probe", "Probe a city", ( string city = "DEFAULT" ) => {
					seenCity = city
					return "probed " & city
				} )
				chatRequest = aiChatRequest(
					aiMessage().user( "probe Paris" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				wrapCalls = 0
				chatRequest.addMiddleware( {
					"beforeToolCall": ( ctx ) => {
						// A logging/redaction shim that hands the arguments on as JSON. The
						// normalized slot is no longer a struct, which is NOT the same thing as an
						// empty argument set: the tool must still run with what the model asked for.
						ctx.toolArgs = jsonSerialize( ctx.toolArgs )
						return AiMiddlewareResult::continue()
					},
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							return {
								"output": { "message": { "role": "assistant", "content": [
									{ "toolUse": { "toolUseId": "tu_1", "name": "probe", "input": { "city": "ORIGINAL" } } }
								] } },
								"stopReason": "tool_use"
							}
						}
						return { "output": { "message": { "role": "assistant", "content": [ { "text": "done" } ] } }, "stopReason": "end_turn" }
					}
				} )

				answer = provider.chat( chatRequest )
				turns  = wrapCalls
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		// Before the fix the non-struct slot was read as "the middleware set the args to {}" and
		// the tool ran on its declared default.
		assertThat( variables.get( Key.of( "seenCity" ) ).toString() ).isEqualTo( "ORIGINAL" );
		assertThat( variables.getAsInteger( Key.of( "turns" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "done" );
	}

	@Test
	@DisplayName( "T2: a native-only argument edit is honoured even when the model's arguments hold a JSON null" )
	public void testConverseNativeToolArgEditHonouredWithNullHoldingArgs() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.middleware.AiMiddlewareResult;

				provider = %s
				seenCity = ""
				tool = aiTool( "probe", "Probe a city", ( string city = "DEFAULT", string memo = "" ) => {
					seenCity = city
					return "probed " & city
				} )
				chatRequest = aiChatRequest(
					aiMessage().user( "probe" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", tools: [ tool ] },
					{ provider: "bedrock" }
				)

				// Built by deserializing a literal so the toolUse input really holds BoxLang's null
				// sentinel. That sentinel compares by IDENTITY, so the pre-hook duplicate() of the
				// arguments was never equals() to the original — the resolver concluded the
				// normalized slot had "moved", never looked at the tool call, and dropped the edit.
				toolTurn = jsonDeserialize( '{"output":{"message":{"role":"assistant","content":[{"toolUse":{"toolUseId":"tu_1","name":"probe","input":{"city":"ORIGINAL","memo":null}}}]}},"stopReason":"tool_use"}' )

				wrapCalls = 0
				chatRequest.addMiddleware( {
					// A third-party HITL that only knows the provider-shaped call: it REPLACES
					// toolCall.input and never touches ctx.toolArgs.
					"beforeToolCall": ( ctx ) => {
						ctx.toolCall.input = { "city": "NATIVE", "memo": "x" }
						return AiMiddlewareResult::continue()
					},
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							return toolTurn
						}
						return { "output": { "message": { "role": "assistant", "content": [ { "text": "done" } ] } }, "stopReason": "end_turn" }
					}
				} )

				answer = provider.chat( chatRequest )
				turns  = wrapCalls
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "seenCity" ) ).toString() ).isEqualTo( "NATIVE" );
		assertThat( variables.getAsInteger( Key.of( "turns" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "done" );
	}

	@Test
	@DisplayName( "T3: the normalized toolArgs slot stays authoritative down a middleware chain, which is why a human edit writes BOTH slots" )
	public void testConverseChainedToolArgEditsPreferTheNormalizedSlot() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.middleware.AiMiddlewareResult;

				provider = %s

				toolTurn = {
					"output": { "message": { "role": "assistant", "content": [
						{ "toolUse": { "toolUseId": "tu_1", "name": "probe", "input": { "city": "ORIGINAL" } } }
					] } },
					"stopReason": "tool_use"
				}
				textTurn = { "output": { "message": { "role": "assistant", "content": [ { "text": "done" } ] } }, "stopReason": "end_turn" }

				// ---- variant A: the human decision writes BOTH slots (HumanInTheLoopMiddleware) ----
				bothCity = ""
				bothTool = aiTool( "probe", "Probe a city", ( string city = "DEFAULT" ) => { bothCity = city; return "ok" } )
				bothReq  = aiChatRequest(
					aiMessage().user( "probe" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", tools: [ bothTool ] },
					{ provider: "bedrock" }
				)
				bothWrap = 0
				bothReq.addMiddleware( {
					"beforeToolCall": ( ctx ) => { ctx.toolArgs = { "city": "MW" }; return AiMiddlewareResult::continue() }
				} )
				bothReq.addMiddleware( {
					"beforeToolCall": ( ctx ) => {
						ctx.toolArgs       = { "city": "HUMAN" }
						ctx.toolCall.input = { "city": "HUMAN" }
						return AiMiddlewareResult::continue()
					}
				} )
				bothReq.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => { bothWrap++; return bothWrap == 1 ? duplicate( toolTurn ) : textTurn }
				} )
				provider.chat( bothReq )

				// ---- variant B: the human decision writes ONLY the native slot ----------------
				// Documented, deliberate outcome: an EARLIER middleware already moved the
				// normalized slot, so that slot wins and the native-only edit is not seen. This is
				// exactly why HumanInTheLoopMiddleware.applyEditedArguments() now writes both.
				nativeCity = ""
				nativeTool = aiTool( "probe", "Probe a city", ( string city = "DEFAULT" ) => { nativeCity = city; return "ok" } )
				nativeReq  = aiChatRequest(
					aiMessage().user( "probe" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", tools: [ nativeTool ] },
					{ provider: "bedrock" }
				)
				nativeWrap = 0
				nativeReq.addMiddleware( {
					"beforeToolCall": ( ctx ) => { ctx.toolArgs = { "city": "MW" }; return AiMiddlewareResult::continue() }
				} )
				nativeReq.addMiddleware( {
					"beforeToolCall": ( ctx ) => { ctx.toolCall.input = { "city": "HUMAN" }; return AiMiddlewareResult::continue() }
				} )
				nativeReq.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => { nativeWrap++; return nativeWrap == 1 ? duplicate( toolTurn ) : textTurn }
				} )
				provider.chat( nativeReq )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "bothCity" ) ).toString() ).isEqualTo( "HUMAN" );
		assertThat( variables.get( Key.of( "nativeCity" ) ).toString() ).isEqualTo( "MW" );
	}

	@Test
	@DisplayName( "T4: a suspended Converse batch records the middleware-rewritten toolArgs in its resume ledger, and the resume runs them" )
	public void testConverseSuspendedLedgerCarriesRewrittenToolArgs() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.middleware.AiMiddlewareResult;

				provider = %s
				aCity = ""
				bRuns = 0
				toolA = aiTool( "toolA", "Tool A", ( string city = "DEFAULT" ) => { aCity = city; return "A:" & city } )
				toolB = aiTool( "toolB", "Tool B", ( string city = "DEFAULT" ) => { bRuns++; return "B:" & city } )

				wrapCalls = 0
				llmMw = {
					"wrapLLMCall": ( ctx, handler ) => {
						wrapCalls++
						if ( wrapCalls == 1 ) {
							return {
								"output": { "message": { "role": "assistant", "content": [
									{ "toolUse": { "toolUseId": "tu_a", "name": "toolA", "input": { "city": "ORIGINAL" } } },
									{ "toolUse": { "toolUseId": "tu_b", "name": "toolB", "input": { "city": "ORIGINAL" } } }
								] } },
								"stopReason": "tool_use"
							}
						}
						return { "output": { "message": { "role": "assistant", "content": [ { "text": "both done" } ] } }, "stopReason": "end_turn" }
					}
				}

				chatRequest = aiChatRequest(
					aiMessage().user( "run both" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", tools: [ toolA, toolB ] },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					// toolA's arguments are rewritten in pass 1; toolB needs a human, so the WHOLE
					// batch suspends before either runs.
					"beforeToolCall": ( ctx ) => {
						if ( ctx.toolName == "toolA" ) {
							ctx.toolArgs = { "city": "SAFE" }
							return AiMiddlewareResult::continue()
						}
						return AiMiddlewareResult::defer( { toolName: ctx.toolName, toolArgs: ctx.toolArgs } )
					}
				} )
				chatRequest.addMiddleware( llmMw )

				suspendedResult = provider.chat( chatRequest )
				suspended     = isObject( suspendedResult ) && suspendedResult.isSuspended()
				suspendData   = suspended ? suspendedResult.getData() : {}
				ledger        = suspendData.resumeLedger ?: []
				ledgerLen     = ledger.len()
				aStatus       = ledgerLen ? ( ledger[ 1 ].status ?: "" ) : ""
				aLedgerCity   = ledgerLen ? ( ledger[ 1 ].toolArgs.city ?: "" ) : ""
				bStatus       = ledgerLen > 1 ? ( ledger[ 2 ].status ?: "" ) : ""
				neitherRanYet = aCity == "" && bRuns == 0
				calledLLMOnce = wrapCalls == 1

				// ---- resume: approve the pending call. A resume NEVER re-fires beforeToolCall, so
				// the ledger's toolArgs are the only surviving record of the pass-1 rewrite.
				finalLedger = ledger
				finalLedger[ 2 ].status = "execute"

				resumeRequest = aiChatRequest(
					aiMessage().user( "run both" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", tools: [ toolA, toolB ] },
					{
						provider: "bedrock",
						_resumeContext: {
							assistantMessage: suspendData.assistantMessage,
							resumeLedger    : finalLedger,
							suspendData     : {
								bedrockApi : suspendData.bedrockApi ?: "converse",
								toolDialect: suspendData.toolDialect ?: "converse"
							}
						}
					}
				)
				resumeRequest.addMiddleware( llmMw )
				answer = provider.chat( resumeRequest )
				resumeAddedOneLLMCall = wrapCalls == 2
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspended" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "ledgerLen" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "aStatus" ) ).toString() ).isEqualTo( "execute" );
		assertThat( variables.get( Key.of( "aLedgerCity" ) ).toString() ).isEqualTo( "SAFE" );
		assertThat( variables.get( Key.of( "bStatus" ) ).toString() ).isEqualTo( "pending" );
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "calledLLMOnce" ) ) ).isTrue();
		// Before the ledger carried toolArgs the resume re-derived them from the raw tool call and
		// toolA ran with "ORIGINAL" — the rewrite the non-suspended path had honoured was lost.
		assertThat( variables.get( Key.of( "aCity" ) ).toString() ).isEqualTo( "SAFE" );
		assertThat( variables.getAsInteger( Key.of( "bRuns" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "resumeAddedOneLLMCall" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "answer" ) ).toString() ).isEqualTo( "both done" );
	}

	@Test
	@DisplayName( "T5: a suspended ConverseStream batch records the middleware-rewritten toolArgs in its resume ledger too" )
	public void testConverseStreamSuspendedLedgerCarriesRewrittenToolArgs() {
		String twoToolBody = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockStart",
		        "{\"contentBlockIndex\":0,\"start\":{\"toolUse\":{\"toolUseId\":\"tu_a\",\"name\":\"toolA\"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"toolUse\":{\"input\":\"{\\\"city\\\":\\\"ORIGINAL\\\"}\"}}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "contentBlockStart",
		        "{\"contentBlockIndex\":1,\"start\":{\"toolUse\":{\"toolUseId\":\"tu_b\",\"name\":\"toolB\"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":1,\"delta\":{\"toolUse\":{\"input\":\"{\\\"city\\\":\\\"ORIGINAL\\\"}\"}}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":1}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"tool_use\"}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.middleware.AiMiddlewareResult;

				provider = %s
				aCity = ""
				bRuns = 0
				toolA = aiTool( "toolA", "Tool A", ( string city = "DEFAULT" ) => { aCity = city; return "A:" & city } )
				toolB = aiTool( "toolB", "Tool B", ( string city = "DEFAULT" ) => { bRuns++; return "B:" & city } )

				chatRequest = aiChatRequest(
					aiMessage().user( "run both" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", tools: [ toolA, toolB ] },
					{ provider: "bedrock" }
				)
				wrapCalls = 0
				chatRequest.addMiddleware( {
					"beforeToolCall": ( ctx ) => {
						if ( ctx.toolName == "toolA" ) {
							ctx.toolArgs = { "city": "SAFE" }
							return AiMiddlewareResult::continue()
						}
						return AiMiddlewareResult::defer( { toolName: ctx.toolName, toolArgs: ctx.toolArgs } )
					},
					"wrapLLMCall": ( ctx, handler ) => { wrapCalls++; return binaryDecode( "%s", "base64" ) }
				} )

				chunks = []
				provider.chatStream( chatRequest, ( chunk ) => { chunks.append( chunk ) } )

				stops         = chunks.filter( c => isStruct( c ) && ( c.type ?: "" ) == "middleware_stop" )
				suspended     = stops.len() == 1 && stops.first().result.isSuspended()
				suspendData   = suspended ? stops.first().result.getData() : {}
				ledger        = suspendData.resumeLedger ?: []
				ledgerLen     = ledger.len()
				aStatus       = ledgerLen ? ( ledger[ 1 ].status ?: "" ) : ""
				aLedgerCity   = ledgerLen ? ( ledger[ 1 ].toolArgs.city ?: "" ) : ""
				bStatus       = ledgerLen > 1 ? ( ledger[ 2 ].status ?: "" ) : ""
				neitherRanYet = aCity == "" && bRuns == 0
				calledLLMOnce = wrapCalls == 1
			""".formatted( converseService(), twoToolBody ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "suspended" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "ledgerLen" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "aStatus" ) ).toString() ).isEqualTo( "execute" );
		assertThat( variables.get( Key.of( "aLedgerCity" ) ).toString() ).isEqualTo( "SAFE" );
		assertThat( variables.get( Key.of( "bStatus" ) ).toString() ).isEqualTo( "pending" );
		assertThat( variables.getAsBoolean( Key.of( "neitherRanYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "calledLLMOnce" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "T6: wrapLLMCall receives the SAME streaming context struct beforeLLMCall was handed" )
	public void testConverseStreamHooksShareOneContextStruct() {
		String body = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"hi\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"end_turn\"}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.middleware.AiMiddlewareResult;

				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)

				// The sync path reuses one llmContext for both hooks; the streaming path built a
				// second struct, so anything stashed in beforeLLMCall was invisible to wrapLLMCall.
				seenTrace = "missing"
				chatRequest.addMiddleware( {
					"beforeLLMCall": ( ctx ) => { ctx.traceId = "abc"; return AiMiddlewareResult::continue() },
					"wrapLLMCall": ( ctx, handler ) => {
						seenTrace = ctx.traceId ?: "missing"
						return binaryDecode( "%s", "base64" )
					}
				} )

				text = ""
				provider.chatStream( chatRequest, ( chunk ) => {
					if ( isArray( chunk.choices ?: "" ) && chunk.choices.len() ) {
						text &= ( chunk.choices.first().delta.content ?: "" )
					}
				} )
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "seenTrace" ) ).toString() ).isEqualTo( "abc" );
		assertThat( variables.get( Key.of( "text" ) ).toString() ).isEqualTo( "hi" );
	}

	@Test
	@DisplayName( "T7: an exception from the caller's stream callback is announced on onAIError exactly once, then propagates" )
	public void testConverseStreamCallbackErrorAnnouncesOnAIError() {
		String body = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"Hello\"}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"end_turn\"}" ),
		    converseFrame( "metadata", "{\"usage\":{\"inputTokens\":9,\"outputTokens\":3,\"totalTokens\":12},\"metrics\":{\"latencyMs\":88}}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				// Same idiom as testFallbackAnnouncesErrorExactlyOnce / R9: the counter lives in
				// THIS script's variables scope, so a leaked registration only ever increments a
				// scope no later test reads.
				errorEvents = 0
				BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )

				errType = ""
				try {
					// Throws on the usage-only metadata chunk, after the model has already been
					// billed — a call that happened and failed, which audit/replay middleware must
					// still see.
					provider.chatStream( chatRequest, ( chunk ) => {
						if ( isStruct( chunk.usage ?: "" ) ) {
							throw( type: "CallerBoom", message: "caller callback failed" )
						}
					} )
				} catch ( any e ) {
					errType = e.type
				}
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
	}

	// ============================================================================================
	// Review follow-ups (#279 / #282 / #283 / #284)
	// ============================================================================================

	@Test
	@DisplayName( "RF1: a stream-callback exception is announced once and propagates, but only AFTER onAITokenCount" )
	public void testStreamCallbackErrorStillAccountsUsage() {
		// The model has already been called and billed by the time a chunk reaches the caller, so a
		// callback failure must not cost the onAITokenCount announce every cost/audit listener
		// reads. The throw was previously re-raised from inside the parse catch, before the usage
		// block ran at all.
		String body = streamBody(
		    converseFrame( "messageStart", "{\"role\":\"assistant\"}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"Hi\"}}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"end_turn\"}" ),
		    converseFrame( "metadata", "{\"usage\":{\"inputTokens\":11,\"outputTokens\":4,\"totalTokens\":15}}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				errorEvents  = 0
				errorMessage = ""
				tokenEvents  = 0
				promptTokens = 0
				BoxRegisterInterceptor( function( data ) { errorEvents++; errorMessage = data.errorMessage }, "onAIError" )
				BoxRegisterInterceptor( function( data ) { tokenEvents++; promptTokens = data.promptTokens }, "onAITokenCount" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )

				errType = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {
						if ( isStruct( chunk.usage ?: "" ) ) {
							throw( type: "CallerBoom", message: "caller callback failed" )
						}
					} )
				} catch ( any e ) {
					errType = e.type
				}
				namesCallback = errorMessage.findNoCase( "Stream callback error" ) > 0
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "CallerBoom" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsBoolean( Key.of( "namesCallback" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "tokenEvents" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsInteger( Key.of( "promptTokens" ) ) ).isEqualTo( 11 );
	}

	@Test
	@DisplayName( "RF2: a truncated event-stream frame announces onAIError exactly once" )
	public void testTruncatedFrameAnnouncesOnAIErrorOnce() {
		// The stream catch used to skip the announce for EVERY BedrockStreamError on the grounds
		// that throwBedrockStreamError() had already made it — but the malformed-frame, truncated
		// tool argument and drain throws announce nothing, so those failures were invisible.
		byte[]	full	= converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"never seen\"}}" );
		String	body	= streamBody(
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"partial\"}}" ),
		    java.util.Arrays.copyOf( full, 17 )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				errorEvents = 0
				BoxRegisterInterceptor( function( data ) { errorEvents++ }, "onAIError" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )

				errType = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errType = e.type
				}
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "BedrockStreamError" );
		assertThat( variables.getAsInteger( Key.of( "errorEvents" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "RF3: an exception frame with an empty payload reports the `:error-message` header" )
	public void testExceptionFrameFallsBackToErrorMessageHeader() {
		byte[]	errorFrame	= frame(
		    new byte[ 0 ],
		    EVENT_HEADERS_MSG, "exception", ":exception-type", "throttlingException",
		    ":error-message", "Too many tokens, slow down" );
		String	body		= streamBody( errorFrame );

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )

				errMsg = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errMsg = e.message
				}
				namesDetail = errMsg.findNoCase( "slow down" ) > 0
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "namesDetail" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "RF4: a body cut mid-PAYLOAD throws instead of reporting a complete stream" )
	public void testTruncatedPayloadThrows() {
		byte[]	full	= converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"this text never arrives\"}}" );
		// Keep every byte of prelude + CRC + headers, drop the tail of the payload: the prelude
		// still declares the full length, so the frame claims a payload the body does not contain.
		String	body	= streamBody(
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"seen\"}}" ),
		    java.util.Arrays.copyOf( full, full.length - 12 )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				responses = 0
				BoxRegisterInterceptor( function( data ) { responses++ }, "onAIChatResponse" )

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )

				errType = ""
				errMsg  = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
				namesPayload = errMsg.findNoCase( "payload" ) > 0
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "BedrockStreamError" );
		assertThat( variables.getAsBoolean( Key.of( "namesPayload" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "responses" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "RF5: a Claude InvokeModel stream keeps message_start's prompt_tokens through message_delta" )
	public void testMessageDeltaDoesNotZeroPromptTokens() {
		// message_delta's usage carries OUTPUT tokens only. Emitting a literal prompt_tokens: 0
		// there overwrote the input_tokens message_start had already captured, so every stream
		// without a trailing amazon-bedrock-invocationMetrics event reported promptTokens 0.
		String body = streamBody(
		    frame( chunkPayload( "{\"type\":\"message_start\",\"message\":{\"usage\":{\"input_tokens\":31}}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"hi\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"},\"usage\":{\"output_tokens\":7}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				promptTokens     = -1
				completionTokens = -1
				BoxRegisterInterceptor(
					function( data ) { promptTokens = data.promptTokens; completionTokens = data.completionTokens },
					"onAITokenCount"
				)

				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )
				provider.chatStream( chatRequest, ( chunk ) => {} )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION, body ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "promptTokens" ) ) ).isEqualTo( 31 );
		assertThat( variables.getAsInteger( Key.of( "completionTokens" ) ) ).isEqualTo( 7 );
	}

	@Test
	@DisplayName( "RF6: an unparseable buffered tool-argument JSON surfaces its OWN error, not the drain's" )
	public void testTruncatedToolArgumentsSurfaceFromContentBlockStop() {
		// processBedrockEvent()'s catch swallowed the BedrockStreamError finalizeStreamToolCall()
		// raises, so the far vaguer drain message ("no content_block_stop") replaced it downstream
		// and named neither the cause nor the truncated JSON.
		String body = streamBody(
		    frame( chunkPayload( "{\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"tool_use\",\"id\":\"t1\",\"name\":\"probe\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame(
		        chunkPayload( "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"city\\\":\"}}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"type\":\"content_block_stop\",\"index\":0}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				toolRuns = 0
				tool = aiTool( "probe", "Probe", ( string city = "NONE" ) => { toolRuns++; return city } )
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-sonnet-20240229-v1:0", max_tokens: 50, tools: [ tool ] },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => binaryDecode( "%s", "base64" ) } )

				errType = ""
				errMsg  = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errType = e.type
					errMsg  = e.message
				}
				namesArguments = errMsg.findNoCase( "argument" ) > 0
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION, body ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "BedrockStreamError" );
		assertThat( variables.getAsBoolean( Key.of( "namesArguments" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "toolRuns" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "RF7: a cancelled STREAMED tool call still records a blocked toolResult after the toolUse" )
	public void testStreamedCancelRecordsToolResult() {
		String body = streamBody(
		    converseFrame( "contentBlockStart", "{\"contentBlockIndex\":0,\"start\":{\"toolUse\":{\"toolUseId\":\"t1\",\"name\":\"probe\"}}}" ),
		    converseFrame( "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"toolUse\":{\"input\":\"{\\\"city\\\":\\\"Rome\\\"}\"}}}" ),
		    converseFrame( "contentBlockStop", "{\"contentBlockIndex\":0}" ),
		    converseFrame( "messageStop", "{\"stopReason\":\"tool_use\"}" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				import bxModules.bxai.models.middleware.AiMiddlewareResult;
				provider = %s
				tool = aiTool( "probe", "Probe", ( string city = "NONE" ) => city )
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50, tools: [ tool ] },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall"   : ( ctx, handler ) => binaryDecode( "%s", "base64" ),
					"beforeToolCall": ( ctx ) => AiMiddlewareResult::cancel( "policy" )
				} )
				provider.chatStream( chatRequest, ( chunk ) => {} )

				// The assistant turn carrying the toolUse block, then the blocked toolResult: a
				// toolUse with no matching toolResult is a conversation the next call rejects.
				finalMessages = chatRequest.getMessages()
				lastContent   = finalMessages.last().content
				hasToolResult = isArray( lastContent ) && lastContent.some( ( part ) => isStruct( part ) && part.keyExists( "toolResult" ) )
				blockedText   = jsonSerialize( lastContent ).findNoCase( "Tool call blocked" ) > 0
			""".formatted( converseService(), body ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasToolResult" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "blockedText" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "RF8: a guardContent-only message is Converse dialect, so no InvokeModel fallback is attempted" )
	public void testGuardContentBlockIsConverseDialect() {
		// The dialect detector's member list was missing guardContent while converseContentBlock()'s
		// had it, so a conversation only expressible in Converse looked re-expressible as a vendor
		// body and a ValidationException triggered a fallback that could only fail again.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				calls = 0
				chatRequest = aiChatRequest(
					aiMessage(),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50 },
					{ provider: "bedrock" }
				)
				chatRequest.setMessages( [ { role: "user", content: [ { guardContent: { text: { text: "check me" } } } ] } ] )
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						calls++
						return { "error": {
							"message"   : "Bedrock request failed with status 400: ValidationException: The model does not support Converse.",
							"statusCode": 400
						} }
					}
				} )
				errMsg = ""
				try {
					provider.chat( chatRequest )
				} catch ( any e ) {
					errMsg = e.message
				}
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		// Exactly ONE leg: the Converse call. No InvokeModel retry.
		assertThat( variables.getAsInteger( Key.of( "calls" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "RF9: tool_choice 'none' omits toolConfig entirely on the Converse body" )
	public void testConverseToolChoiceNoneOmitsToolConfig() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				tool = aiTool( "probe", "Probe", () => "ok" )
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50, tools: [ tool ], tool_choice: "none" },
					{ provider: "bedrock" }
				)
				seenPacket = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						seenPacket = ctx.dataPacket
						return { output: { message: { role: "assistant", content: [ { text: "done" } ] } }, stopReason: "end_turn" }
					}
				} )
				provider.chat( chatRequest )
				hasToolConfig = seenPacket.keyExists( "toolConfig" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasToolConfig" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "RF10: `response_format` is not forwarded anywhere in the Converse body" )
	public void testConverseDropsResponseFormat() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50, response_format: { type: "json_object" } },
					{ provider: "bedrock" }
				)
				seenPacket = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						seenPacket = ctx.dataPacket
						return { output: { message: { role: "assistant", content: [ { text: "done" } ] } }, stopReason: "end_turn" }
					}
				} )
				provider.chat( chatRequest )
				mentionsResponseFormat = jsonSerialize( seenPacket ).findNoCase( "response_format" ) > 0
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "mentionsResponseFormat" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "RF11: the bedrockHeaders shorthand goes through the Converse guardrail header filter" )
	public void testBedrockHeadersShorthandFilteredOnConverse() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				options = {
					bedrockHeaders: {
						"X-Amzn-Bedrock-GuardrailIdentifier": "gr-1",
						"X-Amzn-Bedrock-Service-Tier"       : "flex"
					}
				}
				converseHeaders = provider.buildProviderOptionHeaders( options, "converse" )
				invokeHeaders   = provider.buildProviderOptionHeaders( options, "invoke" )
				converseHasGuardrail = converseHeaders.keyExists( "X-Amzn-Bedrock-GuardrailIdentifier" )
				converseHasTier      = converseHeaders.keyExists( "X-Amzn-Bedrock-Service-Tier" )
				invokeHasGuardrail   = invokeHeaders.keyExists( "X-Amzn-Bedrock-GuardrailIdentifier" )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "converseHasGuardrail" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "converseHasTier" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "invokeHasGuardrail" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "RF12: a cross-region openai.* inference profile id gets max_completion_tokens" )
	public void testCrossRegionOpenAIProfileUsesMaxCompletionTokens() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "us.openai.gpt-oss-120b-1:0", max_tokens: 64 },
					{ provider: "bedrock" }
				)
				seenPacket = {}
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						seenPacket = ctx.dataPacket
						return { choices: [ { message: { role: "assistant", content: "done" }, finish_reason: "stop" } ] }
					}
				} )
				provider.chat( chatRequest )
				hasCompletionTokens = seenPacket.keyExists( "max_completion_tokens" )
				hasMaxTokens        = seenPacket.keyExists( "max_tokens" )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasCompletionTokens" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasMaxTokens" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "RF13: streaming structured output over InvokeModel on an OpenAI-shaped model is refused before the call" )
	public void testStreamingStructuredOutputRefusedForOpenAIInvoke() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				calls = 0
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "openai.gpt-oss-120b-1:0", max_tokens: 64 },
					{ provider: "bedrock" }
				)
				chatRequest.setStructuredOutput( { city: "string" } )
				chatRequest.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => { calls++; return "" } } )

				errType = ""
				try {
					provider.chatStream( chatRequest, ( chunk ) => {} )
				} catch ( any e ) {
					errType = e.type
				}
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "errType" ) ).toString() ).isEqualTo( "UnsupportedProviderCapability" );
		assertThat( variables.getAsInteger( Key.of( "calls" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "RF14: two index-less OpenAI-shaped tool fragments with distinct ids become TWO tool calls" )
	public void testIndexLessToolFragmentsDoNotCollapse() {
		// Every fragment keyed to "0" when `index` was absent, so N parallel calls collapsed into
		// one whose arguments were the concatenation of all of them.
		String body = streamBody(
		    frame( chunkPayload(
		        "{\"choices\":[{\"delta\":{\"tool_calls\":[{\"id\":\"a1\",\"function\":{\"name\":\"probe\",\"arguments\":\"{\\\"city\\\":\\\"Rome\\\"}\"}}]}}]}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload(
		        "{\"choices\":[{\"delta\":{\"tool_calls\":[{\"id\":\"a2\",\"function\":{\"name\":\"probe\",\"arguments\":\"{\\\"city\\\":\\\"Oslo\\\"}\"}}]}}]}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"choices\":[{\"delta\":{},\"finish_reason\":\"tool_calls\"}]}" ),
		        EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				seenCities = []
				tool = aiTool( "probe", "Probe", ( string city = "NONE" ) => { seenCities.append( city ); return city } )
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "openai.gpt-oss-120b-1:0", max_tokens: 64, tools: [ tool ] },
					{ provider: "bedrock" }
				)
				turns = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						turns++
						if ( turns == 1 ) {
							return binaryDecode( "%s", "base64" )
						}
						return jsonSerialize( { bytes: toBase64( charsetDecode(
							jsonSerialize( { choices: [ { delta: { content: "done" }, finish_reason: "stop" } ] } ), "UTF-8"
						) ) } )
					}
				} )
				provider.chatStream( chatRequest, ( chunk ) => {} )
				cityList = seenCities.toList( "," )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION, body ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "cityList" ) ) ).isEqualTo( "Rome,Oslo" );
	}

	@Test
	@DisplayName( "RF15: a Llama stream reports a normalized finishReason on streamState" )
	public void testLlamaStreamRecordsFinishReason() {
		String body = streamBody(
		    frame( chunkPayload( "{\"generation\":\"hello\"}" ), EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" ),
		    frame( chunkPayload( "{\"generation\":\"\",\"stop_reason\":\"length\"}" ), EVENT_HEADERS_MSG, "event", EVENT_HEADERS_EVT, "chunk" )
		);

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s", bedrockApi: "invoke" }
				)
				seenFinish = ""
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "meta.llama3-70b-instruct-v1:0", max_tokens: 8 },
					{ provider: "bedrock" }
				)
				chatRequest.addMiddleware( {
					"wrapLLMCall" : ( ctx, handler ) => binaryDecode( "%s", "base64" ),
					"afterLLMCall": ( ctx ) => { seenFinish = ctx.streamState.finishReason ?: "" }
				} )
				provider.chatStream( chatRequest, ( chunk ) => {} )
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION, body ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "seenFinish" ) ) ).isEqualTo( "length" );
	}

	@Test
	@DisplayName( "RF16: a string-replay frame with an EMPTY bytes value is still processed" )
	public void testEmptyBytesReplayFrameIsProcessed() {
		// `[^"]+` skipped an empty bytes value entirely, so a ConverseStream replay whose
		// contentBlockStop carried no payload never delivered its terminator and the drain then
		// reported the tool arguments as truncated.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				seenCity = ""
				tool = aiTool( "probe", "Probe", ( string city = "NONE" ) => { seenCity = city; return city } )
				chatRequest = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: "anthropic.claude-3-7-sonnet-20250219-v1:0", max_tokens: 50, tools: [ tool ] },
					{ provider: "bedrock" }
				)
				evt = ( kind, data ) => jsonSerialize( { eventType: kind, bytes: toBase64( charsetDecode( jsonSerialize( data ), "UTF-8" ) ) } )
				turns = 0
				chatRequest.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						turns++
						if ( turns > 1 ) {
							return evt( "messageStop", { stopReason: "end_turn" } )
						}
						return evt( "contentBlockStart", { contentBlockIndex: 0, start: { toolUse: { toolUseId: "t1", name: "probe" } } } )
							& evt( "contentBlockDelta", { contentBlockIndex: 0, delta: { toolUse: { input: '{"city":"Rome"}' } } } )
							& '{"eventType":"contentBlockStop","bytes":""}'
							& evt( "messageStop", { stopReason: "tool_use" } )
					}
				} )
				provider.chatStream( chatRequest, ( chunk ) => {} )
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "seenCity" ) ) ).isEqualTo( "Rome" );
	}

	@Test
	@DisplayName( "RF17: the once-only Converse fallback log is keyed on model id AND reason" )
	public void testConverseFallbackLogKeyedOnModelAndReason() {
		// Keyed on the model id alone, resolveBedrockApi()'s read-only rawBody resolution wrote the
		// key and permanently silenced the far more interesting runtime line — a Converse call that
		// FAILED with a ValidationException and was re-sent over InvokeModel.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				provider = %s
				model = "anthropic.claude-3-haiku-20240307-v1:0"

				// Leg 1: rawBody forces invoke through the read-only resolution path.
				rawReq = aiChatRequest(
					aiMessage().user( "hi" ),
					{ model: model, max_tokens: 20 },
					{ provider: "bedrock", providerOptions: { rawBody: { prompt: "hi" } } }
				)
				rawReq.addMiddleware( { "wrapLLMCall": ( ctx, handler ) => { return { content: [ { type: "text", text: "raw" } ], stop_reason: "end_turn" } } } )
				provider.chat( rawReq )

				// Leg 2: a ValidationException fallback for the SAME model.
				valReq = aiChatRequest( aiMessage().user( "hi" ), { model: model, max_tokens: 20 }, { provider: "bedrock" } )
				valReq.addMiddleware( {
					"wrapLLMCall": ( ctx, handler ) => {
						if ( !( ctx.fallbackRetry ?: false ) ) {
							return { "error": {
								"message"   : "Bedrock request failed with status 400: ValidationException: The model does not support Converse.",
								"statusCode": 400
							} }
						}
						return { content: [ { type: "text", text: "recovered" } ], stop_reason: "end_turn" }
					}
				} )
				answer = provider.chat( valReq )

				logKeys = provider.CONVERSE_FALLBACK_LOGGED.keyArray().filter( ( k ) => k.findNoCase( model ) > 0 )
				keyCount = logKeys.len()
			""".formatted( converseService() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "answer" ) ) ).isEqualTo( "recovered" );
		assertThat( variables.getAsInteger( Key.of( "keyCount" ) ) ).isEqualTo( 2 );
	}
}
