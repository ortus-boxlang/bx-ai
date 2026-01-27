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

public class AwsCredentialProviderTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Can instantiate AwsCredentialProvider" )
	public void testCanInstantiate() {
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			isNotNull = !isNull( provider );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isNotNull" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Returns explicit credentials when provided" )
	public void testExplicitCredentials() {
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			creds = provider.getCredentials( {
				awsAccessKeyId: "AKIAEXPLICIT123",
				awsSecretAccessKey: "explicitSecretKey456",
				awsSessionToken: "explicitSessionToken789"
			} );

			accessKeyId = creds.awsAccessKeyId;
			secretAccessKey = creds.awsSecretAccessKey;
			sessionToken = creds.awsSessionToken;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "accessKeyId" ) ) ).isEqualTo( "AKIAEXPLICIT123" );
		assertThat( variables.get( Key.of( "secretAccessKey" ) ) ).isEqualTo( "explicitSecretKey456" );
		assertThat( variables.get( Key.of( "sessionToken" ) ) ).isEqualTo( "explicitSessionToken789" );
	}

	@Test
	@DisplayName( "Handles missing session token in explicit credentials" )
	public void testMissingSessionToken() {
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			creds = provider.getCredentials( {
				awsAccessKeyId: "AKIAEXPLICIT123",
				awsSecretAccessKey: "explicitSecretKey456"
			} );

			accessKeyId = creds.awsAccessKeyId;
			sessionToken = creds.awsSessionToken;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "accessKeyId" ) ) ).isEqualTo( "AKIAEXPLICIT123" );
		assertThat( variables.get( Key.of( "sessionToken" ) ) ).isEqualTo( "" );
	}

	@Test
	@DisplayName( "Credential struct always has all three keys" )
	public void testCredentialStructHasAllKeys() {
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			creds = provider.getCredentials();

			hasAccessKeyId = creds.keyExists( "awsAccessKeyId" );
			hasSecretAccessKey = creds.keyExists( "awsSecretAccessKey" );
			hasSessionToken = creds.keyExists( "awsSessionToken" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasAccessKeyId" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasSecretAccessKey" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasSessionToken" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "getRegion returns region from environment or default" )
	public void testGetRegion() {
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			region = provider.getRegion();
			regionWithDefault = provider.getRegion( "eu-central-1" );

			regionNotEmpty = len( region ) > 0;
			regionWithDefaultNotEmpty = len( regionWithDefault ) > 0;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "regionNotEmpty" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "regionWithDefaultNotEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "getProfileName returns profile name" )
	public void testGetProfileName() {
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			profileName = provider.getProfileName();

			profileNotEmpty = len( profileName ) > 0;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "profileNotEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "isRunningOnAWS returns boolean" )
	public void testIsRunningOnAWS() {
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			result = provider.isRunningOnAWS();

			isBoolean = isBoolean( result );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isBoolean" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Can retrieve credentials from current environment" )
	public void testEnvironmentCredentials() {
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			creds = provider.getCredentials();

			isStruct = isStruct( creds );
			envAccessKeyId = getSystemSetting( "AWS_ACCESS_KEY_ID", "" );

			// If env var is set, credential should match
			matchesEnv = !len( envAccessKeyId ) || creds.awsAccessKeyId == envAccessKeyId;
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "matchesEnv" ) ) ).isTrue();
	}

}
