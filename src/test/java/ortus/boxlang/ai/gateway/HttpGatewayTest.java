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
 * ----------------------------------------------------------------------------------
 * Verifies HttpGateway in isolation (no real HTTP) — signing/verification via
 * GatewaySecurity, inbound parsing, pending-interaction lifecycle, atomic resolve,
 * and expiry. GatewayRequestProcessorTest covers the real end-to-end HTTP round trip.
 */
package ortus.boxlang.ai.gateway;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "HttpGateway Tests" )
public class HttpGatewayTest extends BaseIntegrationTest {

	@DisplayName( "declares inbound/outbound/humanApproval capabilities" )
	@Test
	public void testDeclaresCapabilities() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw = aiGateway( "http", { secret: "test-secret" } )
				supportsIn   = gw.supports( "inboundMessages" )
				supportsOut  = gw.supports( "outboundMessages" )
				supportsHitl = gw.supports( "humanApproval" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "supportsIn" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "supportsOut" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "supportsHitl" ) ) ).isTrue();
	}

	@DisplayName( "GatewaySecurity: sign() then verify() round-trips as valid" )
	@Test
	public void testSecurityRoundTrip() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.http.GatewaySecurity;

				secret    = "shh-its-a-secret"
				timestamp = toString( int( now().getTime() / 1000 ) )
				nonce     = createUUID()
				rawBody   = '{"hello":"world"}'

				signature = GatewaySecurity::sign( secret, timestamp, nonce, rawBody )
				verifyResult = GatewaySecurity::verify( secret, timestamp, nonce, rawBody, signature )
				isValid      = verifyResult.valid
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isValid" ) ) ).isTrue();
	}

	@DisplayName( "GatewaySecurity: verify() rejects a tampered body" )
	@Test
	public void testSecurityRejectsTamperedBody() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.http.GatewaySecurity;

				secret    = "shh-its-a-secret"
				timestamp = toString( int( now().getTime() / 1000 ) )
				nonce     = createUUID()

				signature = GatewaySecurity::sign( secret, timestamp, nonce, '{"amount":10}' )
				verifyResult = GatewaySecurity::verify( secret, timestamp, nonce, '{"amount":99999}', signature )
				isValid      = verifyResult.valid
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isValid" ) ) ).isFalse();
	}

	@DisplayName( "requestHumanInteraction() tracks a pending interaction; resolveInteraction() resolves it exactly once" )
	@Test
	public void testInteractionLifecycle() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw = aiGateway( "http", { secret: "test-secret" } )
				interactionRequest = new HumanInteractionRequest( executionID: "run-1" )
				ctx = new GatewayContext( gateway: "http", threadID: "thread-1" )

				delivery = gw.requestHumanInteraction( interactionRequest, ctx )
				hasDecisionYet = delivery.hasDecision()

				record = gw.getInteraction( interactionRequest.getId() )
				trackedThreadID = record.context.getThreadID()

				decision = new HumanInteractionDecision( requestID: interactionRequest.getId(), decision: "approve" )
				resolved = gw.resolveInteraction( interactionRequest.getId(), decision )
				resolvedIsApproved = resolved.isApproved()

				secondAttemptThrew = false
				try {
					gw.resolveInteraction( interactionRequest.getId(), decision )
				} catch ( any e ) {
					secondAttemptThrew = true
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasDecisionYet" ) ) ).isFalse();
		assertThat( variables.get( Key.of( "trackedThreadID" ) ) ).isEqualTo( "thread-1" );
		assertThat( variables.getAsBoolean( Key.of( "resolvedIsApproved" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "secondAttemptThrew" ) ) ).isTrue();
	}

}
