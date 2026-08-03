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
package ortus.boxlang.ai.channels.slack;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "SlackSignature — Slack's v0 HMAC-SHA256 request-signing scheme" )
public class SlackSignatureTest extends BaseSlackChannelTest {

	@DisplayName( "signs with the v0= prefix" )
	@Test
	public void testSignsWithV0Prefix() {
		runtime.executeSource(
		    """
		    import bxModules.bxaiSlack.models.SlackSignature;
		    sig = SlackSignature::sign( "secret", "1700000000", "body" )
		    prefix = sig.left( 3 )
		    """,
		    context
		);
		assertThat( variables.get( Key.of( "prefix" ) ) ).isEqualTo( "v0=" );
	}

	@DisplayName( "verifies a correctly signed request" )
	@Test
	public void testVerifiesCorrectlySignedRequest() {
		runtime.executeSource(
		    """
		    import bxModules.bxaiSlack.models.SlackSignature;
		    secret    = "test-signing-secret"
		    timestamp = toString( int( now().getTime() / 1000 ) )
		    rawBody   = "token=abc&team_id=T123"
		    sig       = SlackSignature::sign( secret, timestamp, rawBody )

		    result = SlackSignature::verify( secret, timestamp, rawBody, sig )
		    valid = result.valid
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "valid" ) ) ).isTrue();
	}

	@DisplayName( "rejects a tampered body" )
	@Test
	public void testRejectsTamperedBody() {
		runtime.executeSource(
		    """
		    import bxModules.bxaiSlack.models.SlackSignature;
		    secret    = "test-signing-secret"
		    timestamp = toString( int( now().getTime() / 1000 ) )
		    rawBody   = "token=abc&team_id=T123"
		    sig       = SlackSignature::sign( secret, timestamp, rawBody )

		    result = SlackSignature::verify( secret, timestamp, rawBody & "TAMPERED", sig )
		    valid = result.valid
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "valid" ) ) ).isFalse();
	}

	@DisplayName( "rejects a mismatched signature" )
	@Test
	public void testRejectsMismatchedSignature() {
		runtime.executeSource(
		    """
		    import bxModules.bxaiSlack.models.SlackSignature;
		    secret    = "test-signing-secret"
		    timestamp = toString( int( now().getTime() / 1000 ) )
		    rawBody   = "token=abc&team_id=T123"

		    result = SlackSignature::verify( secret, timestamp, rawBody, "v0=deadbeef" )
		    valid = result.valid
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "valid" ) ) ).isFalse();
	}

	@DisplayName( "rejects a request outside the timestamp tolerance" )
	@Test
	public void testRejectsRequestOutsideTolerance() {
		runtime.executeSource(
		    """
		    import bxModules.bxaiSlack.models.SlackSignature;
		    secret       = "test-signing-secret"
		    rawBody      = "token=abc&team_id=T123"
		    oldTimestamp = toString( int( now().getTime() / 1000 ) - 10000 )
		    oldSig       = SlackSignature::sign( secret, oldTimestamp, rawBody )

		    result = SlackSignature::verify( secret, oldTimestamp, rawBody, oldSig )
		    valid = result.valid
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "valid" ) ) ).isFalse();
	}

	@DisplayName( "rejects a missing/non-numeric timestamp" )
	@Test
	public void testRejectsNonNumericTimestamp() {
		runtime.executeSource(
		    """
		    import bxModules.bxaiSlack.models.SlackSignature;
		    result = SlackSignature::verify( "secret", "not-a-number", "body", "v0=whatever" )
		    valid = result.valid
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "valid" ) ) ).isFalse();
	}

}
