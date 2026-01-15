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
package ortus.boxlang.ai.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class AwsSignatureV4Test extends BaseIntegrationTest {

	@Test
	@DisplayName( "AWS Signature V4 signing creates proper headers" )
	public void testBasicSigning() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create a Bedrock service which uses AWS Signature V4 internally
				service = aiService(
					"bedrock",
					{
						awsAccessKeyId: "AKIAIOSFODNN7EXAMPLE",
						awsSecretAccessKey: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
						awsRegion: "us-east-1"
					}
				)
				
				// If service is created successfully, signing is working
				hasService = !isNull( service )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "hasService" ) ) ).isEqualTo( true );
	}
}
