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

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

/**
 * Shared fixture for every test class that hits real AWS Bedrock.
 *
 * What it does, once per test method:
 *
 * <ul>
 * <li>captures the AWS credentials from .env (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY /
 * AWS_REGION, plus AWS_SESSION_TOKEN for SSO) so {@link #hasAwsCredentials()} can skip the
 * whole class when they are absent;</li>
 * <li>swaps the module's <code>provider</code> and <code>apiKey</code> settings over to
 * Bedrock on the <em>shared static</em> moduleRecord.settings;</li>
 * <li>restores those two settings afterwards, guarded so a @BeforeEach that threw before the
 * capture cannot delete settings this fixture never wrote.</li>
 * </ul>
 *
 * Subclasses must NOT override beforeEach/afterEach — the provider/apiKey swap and its guarded
 * restore are the whole point of this class, and a subclass override would silently leak the
 * struct apiKey into the next test class in the Gradle worker. Per-test setup belongs in the
 * test method itself.
 */
public abstract class BedrockLiveTestBase extends BaseIntegrationTest {

	protected String	awsAccessKeyId;
	protected String	awsSecretAccessKey;
	protected String	awsSessionToken;
	protected String	awsRegion;

	protected boolean	captured;
	protected boolean	hadPriorProvider;
	protected Object	priorProvider;
	protected boolean	hadPriorApiKey;
	protected Object	priorApiKey;

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

	protected boolean hasAwsCredentials() {
		return !awsAccessKeyId.isEmpty() && !awsSecretAccessKey.isEmpty();
	}

	protected void assumeLive( String source ) {
		assumeTrue( hasAwsCredentials(), "AWS credentials not configured" );
		assumeTrue( executeLiveBedrockCall( source, context ), "live Bedrock call timed out" );
	}

	protected String str( String name ) {
		Object v = variables.get( Key.of( name ) );
		return v == null ? "" : v.toString();
	}

}
