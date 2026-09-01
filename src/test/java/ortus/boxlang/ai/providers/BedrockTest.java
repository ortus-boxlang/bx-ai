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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterEach;
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
 * AWS_SESSION_TOKEN for SSO). A call this account/role simply cannot make —
 * no entitlement, an expired session token, throttling — is skipped rather
 * than failed; see BaseIntegrationTest::executeLiveBedrockCall.
 *
 * The model must also be enabled in that account/region. Both ids are
 * overridable from .env, defined in BaseIntegrationTest:
 *
 * <pre>
 * BEDROCK_MODEL=...             # plain chat and tool-use
 * BEDROCK_STRUCTURED_MODEL=...  # structured output via forced tool-use
 * </pre>
 *
 * The defaults are "global." inference profiles so they resolve in any region.
 * Override BEDROCK_MODEL only with a model that supports tool use — the
 * tool-use test shares it, and Bedrock answers a model that does not with a
 * ValidationException, which is deliberately NOT tolerated.
 *
 * For deterministic, credential-free coverage of the tool-use logic see
 * BedrockToolUseTest.
 */
public class BedrockTest extends BaseIntegrationTest {

	private String	awsAccessKeyId;
	private String	awsSecretAccessKey;
	private String	awsSessionToken;
	private String	awsRegion;

	private boolean	captured;
	private boolean	hadPriorProvider;
	private Object	priorProvider;
	private boolean	hadPriorApiKey;
	private Object	priorApiKey;

	@BeforeEach
	public void beforeEach() {
		// Bedrock authenticates via the AWS credential chain, supplied here as a
		// struct apiKey (same pattern as BedrockServiceTest).
		awsAccessKeyId		= dotenv.get( "AWS_ACCESS_KEY_ID", "" );
		awsSecretAccessKey	= dotenv.get( "AWS_SECRET_ACCESS_KEY", "" );
		awsSessionToken		= dotenv.get( "AWS_SESSION_TOKEN", "" );
		awsRegion			= dotenv.get( "AWS_REGION", "us-east-1" );

		// moduleRecord.settings is static and shared with every other test class in this Gradle
		// worker, and Bedrock is the only provider whose apiKey is a struct — leaking it makes
		// setApiKeyIfEmpty( required string ) throw a type error in whichever class runs next.
		hadPriorProvider	= moduleRecord.settings.containsKey( "provider" );
		priorProvider		= moduleRecord.settings.get( "provider" );
		hadPriorApiKey		= moduleRecord.settings.containsKey( "apiKey" );
		priorApiKey			= moduleRecord.settings.get( "apiKey" );
		captured			= true;

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

	@AfterEach
	public void afterEach() {
		// A @BeforeEach that threw before the capture leaves the flags false — restoring then would
		// delete shared settings this class never wrote. JUnit runs @AfterEach either way.
		if ( !captured ) {
			return;
		}
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

	@DisplayName( "Test Bedrock AI chat" )
	@Test
	public void testBedrock() {
		assumeTrue( hasAwsCredentials(), "AWS credentials not configured in .env" );

		// @formatter:off
		assumeTrue( executeLiveBedrockCall(
			"""
			result    = aiChat( messages = "what is boxlang?", options = { model: "%s" } )
			answerLen = len( result )
			println( result )
			""".formatted( BEDROCK_MODEL ),
			context
		), "live Bedrock call timed out" );
		// @formatter:on

		assertThat( variables.get( Key.of( "result" ) ) ).isInstanceOf( String.class );
		assertThat( variables.getAsInteger( Key.of( "answerLen" ) ) ).isGreaterThan( 0 );
	}

	@DisplayName( "Test Bedrock Tools" )
	@Test
	public void testBedrockTools() {
		assumeTrue( hasAwsCredentials(), "AWS credentials not configured in .env" );

		// @formatter:off
		assumeTrue( executeLiveBedrockCall(
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
		), "live Bedrock call timed out" );
		// @formatter:on

		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "San Salvador" );
	}

	@DisplayName( "Test Bedrock structured output via forced tool-use" )
	@Test
	public void testBedrockStructuredOutput() {
		assumeTrue( hasAwsCredentials(), "AWS credentials not configured in .env" );

		// @formatter:off
		assumeTrue( executeLiveBedrockCall(
			"""
			result = aiChat(
				messages = "John Doe is 30 years old and lives in Seattle. Extract his details.",
				params   = { model: "%s", max_tokens: 300 },
				options  = {
					provider: "bedrock",
					schema: {
						"type": "object",
						"properties": {
							"name": { "type": "string" },
							"age":  { "type": "integer" },
							"city": { "type": "string" }
						},
						"required": [ "name", "age", "city" ]
					}
				}
			)
			isStruct = isStruct( result )
			name = result.name ?: ""
			age  = result.age  ?: 0
			city = result.city ?: ""
			println( result )
			""".formatted( BEDROCK_STRUCTURED_MODEL ),
			context
		), "live Bedrock call timed out" );
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).contains( "John" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
		assertThat( variables.get( Key.of( "city" ) ).toString() ).contains( "Seattle" );
	}

}
