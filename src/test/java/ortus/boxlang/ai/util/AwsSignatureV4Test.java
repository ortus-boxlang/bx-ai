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
	@DisplayName( "Can instantiate AwsSignatureV4" )
	public void testCanInstantiate() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();
			isNotNull = !isNull( signer );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isNotNull" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Returns a struct with required headers" )
	public void testReturnsRequiredHeaders() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();
			result = signer.signRequest(
				method = "POST",
				host = "bedrock-runtime.us-east-1.amazonaws.com",
				path = "/model/anthropic.claude-3-sonnet/invoke",
				queryString = "",
				headers = { "content-type": "application/json" },
				payload = '{"message":"test"}',
				region = "us-east-1",
				service = "bedrock",
				accessKeyId = "AKIAEXAMPLE",
				secretAccessKey = "secretkey123"
			);

			isStruct = isStruct( result );
			hasAuthorization = result.keyExists( "Authorization" );
			hasAmzDate = result.keyExists( "x-amz-date" );
			hasContentSha256 = result.keyExists( "x-amz-content-sha256" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasAuthorization" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasAmzDate" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasContentSha256" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Generates Authorization header with correct format" )
	public void testAuthorizationHeaderFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();
			result = signer.signRequest(
				method = "POST",
				host = "bedrock-runtime.eu-west-2.amazonaws.com",
				path = "/model/test-model/invoke",
				queryString = "",
				headers = {},
				payload = "{}",
				region = "eu-west-2",
				service = "bedrock",
				accessKeyId = "AKIAEXAMPLE",
				secretAccessKey = "secretkey123"
			);

			authHeader = result[ "Authorization" ];
			startsWithAws4 = authHeader.startsWith( "AWS4-HMAC-SHA256" );
			containsCredential = authHeader.contains( "Credential=AKIAEXAMPLE/" );
			containsRegion = authHeader.contains( "/eu-west-2/bedrock/aws4_request" );
			containsSignedHeaders = authHeader.contains( "SignedHeaders=" );
			containsSignature = authHeader.contains( "Signature=" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "startsWithAws4" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "containsCredential" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "containsRegion" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "containsSignedHeaders" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "containsSignature" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Includes session token header when provided" )
	public void testSessionTokenIncluded() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();
			result = signer.signRequest(
				method = "POST",
				host = "bedrock-runtime.us-east-1.amazonaws.com",
				path = "/model/test/invoke",
				queryString = "",
				headers = {},
				payload = "{}",
				region = "us-east-1",
				service = "bedrock",
				accessKeyId = "ASIATEMP123",
				secretAccessKey = "tempsecret",
				sessionToken = "tempsessiontoken=="
			);

			hasSecurityToken = result.keyExists( "x-amz-security-token" );
			tokenValue = result[ "x-amz-security-token" ];
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasSecurityToken" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "tokenValue" ) ) ).isEqualTo( "FwoGZXIvYXdzEBYaDKtest123sessiontoken==" );
	}

	@Test
	@DisplayName( "Does not include session token when empty" )
	public void testNoSessionTokenWhenEmpty() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();
			result = signer.signRequest(
				method = "POST",
				host = "bedrock-runtime.us-east-1.amazonaws.com",
				path = "/model/test/invoke",
				queryString = "",
				headers = {},
				payload = "{}",
				region = "us-east-1",
				service = "bedrock",
				accessKeyId = "AKIATEST",
				secretAccessKey = "secret",
				sessionToken = ""
			);

			hasSecurityToken = result.keyExists( "x-amz-security-token" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasSecurityToken" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Generates correct payload hash for empty body" )
	public void testEmptyPayloadHash() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();
			result = signer.signRequest(
				method = "GET",
				host = "example.amazonaws.com",
				path = "/",
				queryString = "",
				headers = {},
				payload = "",
				region = "us-east-1",
				service = "test",
				accessKeyId = "AKIATEST",
				secretAccessKey = "secret"
			);

			// SHA-256 of empty string
			contentSha256 = result[ "x-amz-content-sha256" ];
			isEmptyHash = contentSha256 == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isEmptyHash" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Generates x-amz-date in correct format" )
	public void testDateFormat() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();
			result = signer.signRequest(
				method = "POST",
				host = "test.amazonaws.com",
				path = "/",
				queryString = "",
				headers = {},
				payload = "{}",
				region = "us-east-1",
				service = "test",
				accessKeyId = "AKIATEST",
				secretAccessKey = "secret"
			);

			amzDate = result[ "x-amz-date" ];
			// Format should be: YYYYMMDDTHHmmssZ (e.g., 20240115T123456Z)
			matchesFormat = reFind( "^\\d{8}T\\d{6}Z$", amzDate ) > 0;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "matchesFormat" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Handles paths with colons correctly (Bedrock model IDs)" )
	public void testPathsWithColons() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();
			result = signer.signRequest(
				method = "POST",
				host = "bedrock-runtime.us-east-1.amazonaws.com",
				path = "/model/anthropic.claude-3-sonnet-20240229-v1:0/invoke",
				queryString = "",
				headers = {},
				payload = "{}",
				region = "us-east-1",
				service = "bedrock",
				accessKeyId = "AKIATEST",
				secretAccessKey = "secret"
			);

			hasSignature = result[ "Authorization" ].contains( "Signature=" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasSignature" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Works with different regions" )
	public void testDifferentRegions() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();

			// Test us-east-1
			result1 = signer.signRequest(
				method = "POST",
				host = "bedrock-runtime.us-east-1.amazonaws.com",
				path = "/test",
				queryString = "",
				headers = {},
				payload = "{}",
				region = "us-east-1",
				service = "bedrock",
				accessKeyId = "AKIATEST",
				secretAccessKey = "secret"
			);
			valid1 = result1[ "Authorization" ].contains( "/us-east-1/bedrock/aws4_request" );

			// Test eu-west-2
			result2 = signer.signRequest(
				method = "POST",
				host = "bedrock-runtime.eu-west-2.amazonaws.com",
				path = "/test",
				queryString = "",
				headers = {},
				payload = "{}",
				region = "eu-west-2",
				service = "bedrock",
				accessKeyId = "AKIATEST",
				secretAccessKey = "secret"
			);
			valid2 = result2[ "Authorization" ].contains( "/eu-west-2/bedrock/aws4_request" );

			allValid = valid1 && valid2;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "allValid" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Generates different signatures for different payloads" )
	public void testDifferentPayloadsProduceDifferentSignatures() {
		// @formatter:off
		runtime.executeSource(
			"""
			signer = new src.main.bx.models.util.AwsSignatureV4();

			result1 = signer.signRequest(
				method = "POST",
				host = "test.amazonaws.com",
				path = "/test",
				queryString = "",
				headers = {},
				payload = '{"message":"hello"}',
				region = "us-east-1",
				service = "test",
				accessKeyId = "AKIATEST",
				secretAccessKey = "secret"
			);

			result2 = signer.signRequest(
				method = "POST",
				host = "test.amazonaws.com",
				path = "/test",
				queryString = "",
				headers = {},
				payload = '{"message":"goodbye"}',
				region = "us-east-1",
				service = "test",
				accessKeyId = "AKIATEST",
				secretAccessKey = "secret"
			);

			// Content hashes should be different
			hashesAreDifferent = result1[ "x-amz-content-sha256" ] != result2[ "x-amz-content-sha256" ];
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hashesAreDifferent" ) ) ).isTrue();
	}

}
