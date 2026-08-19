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
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

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

	@Test
	@DisplayName( "parseCredentialsFile reads the requested profile, skipping comments and other profiles" )
	public void testParseCredentialsFile() {
		// This function had NO coverage, which is how a call to chr() — not a BoxLang function at all —
		// survived in it: every invocation threw "Function 'chr' not found", so AWS profile-file
		// credentials never worked for any caller of this class.
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			// Built as lines joined by char(10): "##" is how a literal hash is written in BoxLang, so
			// an INI comment marker would otherwise open an interpolation.
			ini = [
				"; a leading comment",
				"",
				"[default]",
				"aws_access_key_id = AKIADEFAULTDEFAULT12",
				"aws_secret_access_key = default-secret",
				"",
				"[work]",
				"## another comment",
				"aws_access_key_id = AKIAWORKWORKWORKWORK",
				"aws_secret_access_key = work-secret",
				"aws_session_token = work-token"
			].toList( char( 10 ) );
			work    = provider.parseCredentialsFile( ini, "work" );
			def     = provider.parseCredentialsFile( ini, "default" );
			missing = provider.parseCredentialsFile( ini, "nope" );
			""",
			context
		);
		// @formatter:on

		@SuppressWarnings( "unchecked" )
		IStruct work = ( IStruct ) variables.get( Key.of( "work" ) );
		assertThat( work.get( Key.of( "awsAccessKeyId" ) ) ).isEqualTo( "AKIAWORKWORKWORKWORK" );
		assertThat( work.get( Key.of( "awsSecretAccessKey" ) ) ).isEqualTo( "work-secret" );
		assertThat( work.get( Key.of( "awsSessionToken" ) ) ).isEqualTo( "work-token" );

		@SuppressWarnings( "unchecked" )
		IStruct def = ( IStruct ) variables.get( Key.of( "def" ) );
		assertWithMessage( "a later profile's keys must not bleed into an earlier one" )
		    .that( def.get( Key.of( "awsAccessKeyId" ) ) ).isEqualTo( "AKIADEFAULTDEFAULT12" );
		assertThat( def.get( Key.of( "awsSessionToken" ) ) ).isEqualTo( "" );

		@SuppressWarnings( "unchecked" )
		IStruct missing = ( IStruct ) variables.get( Key.of( "missing" ) );
		assertThat( missing.get( Key.of( "awsAccessKeyId" ) ) ).isEqualTo( "" );
	}

	@Test
	@DisplayName( "isTrustedContainerCredentialUri allows loopback/ECS/EKS over HTTP and any host over HTTPS" )
	public void testContainerCredentialUriTrust() {
		// The container credential request carries a bearer token, so a FULL_URI pointing at an
		// arbitrary plain-HTTP host must not be contacted.
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			ecs        = provider.isTrustedContainerCredentialUri( "http://169.254.170.2/creds" );
			eks        = provider.isTrustedContainerCredentialUri( "http://169.254.170.23/v1/credentials" );
			loopback   = provider.isTrustedContainerCredentialUri( "http://127.0.0.1:8080/creds" );
			localhost  = provider.isTrustedContainerCredentialUri( "http://localhost/creds" );
			httpsAny   = provider.isTrustedContainerCredentialUri( "https://vault.internal.example/creds" );
			plainOther = provider.isTrustedContainerCredentialUri( "http://evil.example.com/creds" );
			imdsSpoof  = provider.isTrustedContainerCredentialUri( "http://169.254.169.254/creds" );
			// userinfo bypass: the real host is evil.example, not 127.0.0.1
			userinfo   = provider.isTrustedContainerCredentialUri( "http://127.0.0.1:80@evil.example/creds" );
			// bracketed IPv6 must be parsed, not mangled into "["
			ipv6       = provider.isTrustedContainerCredentialUri( "http://[::1]:8080/creds" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "ecs" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "eks" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "loopback" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "localhost" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "httpsAny" ) ) ).isTrue();
		assertWithMessage( "an arbitrary plain-HTTP host must never receive the credential token" )
		    .that( variables.getAsBoolean( Key.of( "plainOther" ) ) ).isFalse();
		assertWithMessage( "the IMDS address is not a container credential endpoint" )
		    .that( variables.getAsBoolean( Key.of( "imdsSpoof" ) ) ).isFalse();
		assertWithMessage( "userinfo before the host must not make an arbitrary host look trusted" )
		    .that( variables.getAsBoolean( Key.of( "userinfo" ) ) ).isFalse();
		assertWithMessage( "a bracketed IPv6 loopback literal is a trusted endpoint" )
		    .that( variables.getAsBoolean( Key.of( "ipv6" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "isCredentialCacheValid requires key material, not just an unexpired timestamp" )
	public void testCredentialCacheValidity() {
		// A container endpoint answering `200 {}` would otherwise cache as a valid credential set
		// until it "expired", suppressing the rest of the chain.
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			emptyButUnexpired = provider.isCredentialCacheValid( {
				"awsAccessKeyId": "", "awsSecretAccessKey": "", "awsSessionToken": "",
				"expiration": dateAdd( "n", 60, now() )
			} );
			populated = provider.isCredentialCacheValid( {
				"awsAccessKeyId": "AKIAEXAMPLEEXAMPLE12", "awsSecretAccessKey": "s", "awsSessionToken": "",
				"expiration": dateAdd( "n", 60, now() )
			} );
			expiringSoon = provider.isCredentialCacheValid( {
				"awsAccessKeyId": "AKIAEXAMPLEEXAMPLE12", "awsSecretAccessKey": "s", "awsSessionToken": "",
				"expiration": dateAdd( "n", 2, now() )
			} );
			noExpiry = provider.isCredentialCacheValid( {
				"awsAccessKeyId": "AKIAEXAMPLEEXAMPLE12", "awsSecretAccessKey": "s", "awsSessionToken": ""
			} );
			""",
			context
		);
		// @formatter:on

		assertWithMessage( "an expiry alone is not evidence of credentials" )
		    .that( variables.getAsBoolean( Key.of( "emptyButUnexpired" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "populated" ) ) ).isTrue();
		assertWithMessage( "inside the refresh buffer must not count as valid" )
		    .that( variables.getAsBoolean( Key.of( "expiringSoon" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "noExpiry" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Auto-resolution negative-caches a failure and the cache seam clears it" )
	public void testNegativeCacheAndClearSeam() {
		// The caching orchestration had no test at all and no way to reset the JVM-static cache, so
		// it could not be exercised even in principle. Off AWS the first resolution finds nothing and
		// records a failure; the state seam makes that observable and clearCredentialCache() undoes it.
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			provider.clearCredentialCache();

			before = provider.credentialCacheState();

			// No explicit credentials, so this falls through to container/IMDS. On a machine with no
			// metadata service that resolves nothing and arms the negative cache.
			provider.getCredentials();
			after = provider.credentialCacheState();

			provider.clearCredentialCache();
			cleared = provider.credentialCacheState();
			""",
			context
		);
		// @formatter:on

		@SuppressWarnings( "unchecked" )
		IStruct before = ( IStruct ) variables.get( Key.of( "before" ) );
		assertWithMessage( "clearCredentialCache() must leave no cached state" )
		    .that( before.get( Key.of( "negativeCached" ) ) ).isEqualTo( false );
		assertThat( before.get( Key.of( "cached" ) ) ).isEqualTo( false );

		@SuppressWarnings( "unchecked" )
		IStruct	after	= ( IStruct ) variables.get( Key.of( "after" ) );
		// Either a failure was recorded (no metadata service) or real credentials were cached
		// (running on AWS) — both are valid, and both prove the cache is actually being written.
		boolean	wrote	= ( Boolean ) after.get( Key.of( "negativeCached" ) ) || ( Boolean ) after.get( Key.of( "cached" ) );
		assertWithMessage( "resolution must record something in the shared cache" ).that( wrote ).isTrue();

		@SuppressWarnings( "unchecked" )
		IStruct cleared = ( IStruct ) variables.get( Key.of( "cleared" ) );
		assertThat( cleared.get( Key.of( "negativeCached" ) ) ).isEqualTo( false );
		assertThat( cleared.get( Key.of( "cached" ) ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "isValidRelativeCredentialUri rejects userinfo and non-path values" )
	public void testRelativeCredentialUriValidation() {
		// RELATIVE_URI is concatenated onto the fixed ECS address, so "@evil.example/creds" would
		// assemble into a URL whose real host is evil.example — the one input that never reaches the
		// full-URI trust check.
		// @formatter:off
		runtime.executeSource(
			"""
			provider = new src.main.bx.models.util.AwsCredentialProvider();
			ok        = provider.isValidRelativeCredentialUri( "/v2/credentials/abc-123" );
			userinfo  = provider.isValidRelativeCredentialUri( "@evil.example/creds" );
			noSlash   = provider.isValidRelativeCredentialUri( "v2/credentials" );
			embedded  = provider.isValidRelativeCredentialUri( "/v2/creds@evil.example" );
			backslash = provider.isValidRelativeCredentialUri( "/v2" & char( 92 ) & "evil.example" );
			""",
			context
		);
		// @formatter:on
		assertThat( variables.getAsBoolean( Key.of( "ok" ) ) ).isTrue();
		assertWithMessage( "a leading @ makes the assembled host attacker-controlled" )
		    .that( variables.getAsBoolean( Key.of( "userinfo" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "noSlash" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "embedded" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "backslash" ) ) ).isFalse();
	}

}
