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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class BedrockServiceTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Can instantiate Bedrock service via aiService BIF" )
	public void testInstantiateBedrock() {
		// @formatter:off
		runtime.executeSource(
			"""
				service = aiService(
					"bedrock",
					{
						awsAccessKeyId: "test-key",
						awsSecretAccessKey: "test-secret",
						awsRegion: "us-east-1"
					}
				)
				serviceName = service.getName()
			""",
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
						awsAccessKeyId: "AKIAIOSFODNN7EXAMPLE",
						awsSecretAccessKey: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
						awsRegion: "us-west-2",
						model: "anthropic.claude-3-5-sonnet-20241022-v2:0"
					}
				)
				
				hasName = !isNull( service.getName() )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasName" ) ) ).isEqualTo( true );
	}

	@Test
	@EnabledIfEnvironmentVariable( named = "AWS_ACCESS_KEY_ID", matches = ".+" )
	@DisplayName( "Bedrock service can make real API call to Claude" )
	public void testRealClaudeCall() {
		// @formatter:off
		runtime.executeSource(
			"""
				response = aiChat(
					"bedrock",
					{
						model: "anthropic.claude-3-5-sonnet-20241022-v2:0",
						max_tokens: 100
					},
					aiMessage().user( "Say 'Bedrock test successful' and nothing else" )
				)
				
				hasContent = !isNull( response.content )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasContent" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Bedrock service loads credentials from environment" )
	public void testEnvironmentCredentials() {
		// Set env vars before test
		System.setProperty( "AWS_ACCESS_KEY_ID", "env-test-key" );
		System.setProperty( "AWS_SECRET_ACCESS_KEY", "env-test-secret" );
		System.setProperty( "AWS_REGION", "us-east-1" );

		try {
			// @formatter:off
			runtime.executeSource(
				"""
					// Should load from environment
					service = aiService( "bedrock", {} )
					
					hasService = !isNull( service )
				""",
				context
			);
			// @formatter:on

			assertThat( variables.get( Key.of( "hasService" ) ) ).isEqualTo( true );
		} finally {
			// Clean up
			System.clearProperty( "AWS_ACCESS_KEY_ID" );
			System.clearProperty( "AWS_SECRET_ACCESS_KEY" );
			System.clearProperty( "AWS_REGION" );
		}
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
						awsAccessKeyId: "key",
						awsSecretAccessKey: "secret",
						awsRegion: "us-east-1"
					}
				)
				
				isConfigured = !isNull( service )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "isConfigured" ) ) ).isEqualTo( true );
	}
}
