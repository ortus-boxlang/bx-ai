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
		runtime.executeSource(
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
		runtime.executeSource(
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
		runtime.executeSource(
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
		runtime.executeSource(
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
		runtime.executeSource(
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
	@DisplayName( "AiChatRequest supports providerOptions for provider-specific settings" )
	public void testProviderOptions() {
		// @formatter:off
		runtime.executeSource(
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
}
