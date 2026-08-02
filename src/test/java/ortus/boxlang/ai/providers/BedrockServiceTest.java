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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
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
