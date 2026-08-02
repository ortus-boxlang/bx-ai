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

	@BeforeEach
	public void beforeEach() {
		// Load AWS credentials from .env file (same pattern as other provider tests)
		awsAccessKeyId		= dotenv.get( "AWS_ACCESS_KEY_ID", "" );
		awsSecretAccessKey	= dotenv.get( "AWS_SECRET_ACCESS_KEY", "" );
		awsSessionToken		= dotenv.get( "AWS_SESSION_TOKEN", "" );
		awsRegion			= dotenv.get( "AWS_REGION", "us-east-1" );

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
		if ( !hasAwsCredentials() ) {
			System.out.println( "Skipping testRealClaudeCall - AWS credentials not configured in .env" );
			return;
		}

		// @formatter:off
		executeWithTimeoutHandling(
			"""
				// aiChat signature: invoke(messages, params, options, headers)
				response = aiChat(
					aiMessage().user( "Say 'Bedrock test successful' and nothing else" ),
					{
						model: "anthropic.claude-3-sonnet-20240229-v1:0",
						max_tokens: 100
					},
					{
						provider: "bedrock",
						returnFormat: "single"
					}
				)

				hasContent = !isNull( response )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasContent" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Bedrock service loads credentials from environment" )
	public void testEnvironmentCredentials() {
		if ( !hasAwsCredentials() ) {
			System.out.println( "Skipping testEnvironmentCredentials - AWS credentials not configured in .env" );
			return;
		}

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
	@DisplayName( "getBedrockPath's encoded model ID, re-encoded by AwsSignatureV4's canonical-request pass, round-trips without residual reserved characters" )
	public void testPathEncodingAgreesWithSigner() {
		// item-2 fix: the wire path (single-encoded) is passed as-is into
		// AwsSignatureV4.signRequest()'s `path` argument, which re-encodes it a second time
		// internally (AWS's documented double-URI-encode-except-S3 rule) to build the canonical
		// request. Verifying via the public encodeComponent() seam that encoding the same raw
		// value twice produces the expected double-escaped form confirms the two paths agree.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				signer = new src.main.bx.models.util.AwsSignatureV4()
				arn = "arn:aws:bedrock:us-east-1:123456789012:inference-profile/abc"
				once  = signer.encodeComponent( arn )
				twice = signer.encodeComponent( once )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "once" ) ).toString() )
		    .isEqualTo( "arn%3Aaws%3Abedrock%3Aus-east-1%3A123456789012%3Ainference-profile%2Fabc" );
		// Every "%" from the first pass must itself be escaped to "%25" by the second pass
		assertThat( variables.get( Key.of( "twice" ) ).toString() )
		    .isEqualTo( "arn%253Aaws%253Abedrock%253Aus-east-1%253A123456789012%253Ainference-profile%252Fabc" );
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

	// -----------------------------------------------------------------------
	// Chunk 2 — item 14: bearer / API-key auth
	// -----------------------------------------------------------------------

	@Test
	@DisplayName( "configure() with a plain string sets it as the apiKey (bearer auth), not modelId" )
	public void testConfigureStringIsApiKeyNotModelId() {
		// item-14 fix: configure(string) now follows the module-wide "string = apiKey" contract
		// instead of the old (incorrect) "string = modelId" behavior.
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = new bxModules.bxai.models.providers.BedrockService()
				service.configure( "my-bedrock-api-key" )
				apiKey        = service.getApiKey()
				modelIdEmpty  = !len( service.getModelId() )
				usesBearer    = service.useBearerAuth()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "apiKey" ) ).toString() ).isEqualTo( "my-bedrock-api-key" );
		assertThat( variables.getAsBoolean( Key.of( "modelIdEmpty" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "usesBearer" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "useBearerAuth() is false for struct-based AWS credential configuration" )
	public void testStructConfigureDoesNotUseBearerAuth() {
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
		// in this flow (see beforeEach), which must NOT be mistaken for a bearer-mode string key.
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
	@DisplayName( "buildProviderOptionHeaders honors the bedrockHeaders struct shorthand" )
	public void testBedrockHeadersStructShorthand() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
				service = aiService(
					"bedrock",
					{ awsAccessKeyId: "%s", awsSecretAccessKey: "%s", region: "%s" }
				)
				headers = service.buildProviderOptionHeaders( {
					bedrockHeaders: {
						"Service-Tier": "flex",
						"Request-Metadata": "project=demo"
					}
				} )
				serviceTier = headers[ "Service-Tier" ]
				requestMeta = headers[ "Request-Metadata" ]
			""".formatted( DUMMY_AWS_ACCESS_KEY_ID, DUMMY_AWS_SECRET_ACCESS_KEY, DUMMY_AWS_REGION ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "serviceTier" ) ) ).isEqualTo( "flex" );
		assertThat( variables.get( Key.of( "requestMeta" ) ).toString() ).contains( "demo" );
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
}
