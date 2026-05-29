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
import ortus.boxlang.runtime.types.Struct;

/**
 * Live integration tests for the AWS Bedrock provider.
 *
 * These hit real Bedrock and are skipped unless AWS credentials are present in
 * the environment (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_REGION, and
 * AWS_SESSION_TOKEN for SSO). The model must also be enabled in that
 * account/region — note that newer Claude models are only reachable on-demand
 * via an inference-profile id (e.g. "us.anthropic.claude-3-5-sonnet-...");
 * adjust BEDROCK_MODEL to one your test account can invoke.
 *
 * For deterministic, credential-free coverage of the tool-use logic see
 * BedrockToolUseTest.
 */
public class BedrockTest extends BaseIntegrationTest {

	private static final String	BEDROCK_MODEL	= "anthropic.claude-3-5-sonnet-20240620-v1:0";

	private String				awsAccessKeyId;
	private String				awsSecretAccessKey;
	private String				awsSessionToken;
	private String				awsRegion;

	@BeforeEach
	public void beforeEach() {
		// Bedrock authenticates via the AWS credential chain, supplied here as a
		// struct apiKey (same pattern as BedrockServiceTest).
		awsAccessKeyId		= dotenv.get( "AWS_ACCESS_KEY_ID", "" );
		awsSecretAccessKey	= dotenv.get( "AWS_SECRET_ACCESS_KEY", "" );
		awsSessionToken		= dotenv.get( "AWS_SESSION_TOKEN", "" );
		awsRegion			= dotenv.get( "AWS_REGION", "us-east-1" );

		moduleRecord.settings.put( "provider", "bedrock" );
		Struct credentials = new Struct();
		credentials.put( "awsAccessKeyId", awsAccessKeyId );
		credentials.put( "awsSecretAccessKey", awsSecretAccessKey );
		credentials.put( "region", awsRegion );
		if ( !awsSessionToken.isEmpty() ) {
			credentials.put( "awsSessionToken", awsSessionToken );
		}
		moduleRecord.settings.put( "apiKey", credentials );
	}

	private boolean hasAwsCredentials() {
		return !awsAccessKeyId.isEmpty() && !awsSecretAccessKey.isEmpty();
	}

	@DisplayName( "Test Bedrock AI chat" )
	@Test
	public void testBedrock() {
		if ( !hasAwsCredentials() ) {
			System.out.println( "Skipping testBedrock - AWS credentials not configured in .env" );
			return;
		}

		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result    = aiChat( messages = "what is boxlang?", options = { model: "%s" } )
			answerLen = len( result )
			println( result )
			""".formatted( BEDROCK_MODEL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "result" ) ) ).isInstanceOf( String.class );
		assertThat( variables.getAsInteger( Key.of( "answerLen" ) ) ).isGreaterThan( 0 );
	}

	@DisplayName( "Test Bedrock Tools" )
	@Test
	public void testBedrockTools() {
		if ( !hasAwsCredentials() ) {
			System.out.println( "Skipping testBedrockTools - AWS credentials not configured in .env" );
			return;
		}

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
				params   = { tools: [ tool ] },
				options  = { model: "%s", logResponseToConsole: true } )
			println( result )
			""".formatted( BEDROCK_MODEL ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "San Salvador" );
	}

}
