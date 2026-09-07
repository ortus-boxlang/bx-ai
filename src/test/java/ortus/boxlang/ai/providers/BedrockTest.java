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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

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
public class BedrockTest extends BedrockLiveTestBase {

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
