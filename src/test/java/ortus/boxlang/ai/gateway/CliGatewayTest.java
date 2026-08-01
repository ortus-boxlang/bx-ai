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
 * Verifies CliGateway — the Phase 3 canonical/reference IGateway wrapping the
 * original cliApprove() prompt logic — using an injected inputReader so these tests
 * never touch real stdin.
 */
package ortus.boxlang.ai.gateway;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "CliGateway Tests" )
public class CliGatewayTest extends BaseIntegrationTest {

	@DisplayName( "declares the humanApproval capability" )
	@Test
	public void testDeclaresCapability() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.cli.CliGateway;

				gw = new CliGateway()
				supportsHitl = gw.supports( "humanApproval" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "supportsHitl" ) ) ).isTrue();
	}

	@DisplayName( "'a' approves and the delivery already carries the decision" )
	@Test
	public void testApprove() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.cli.CliGateway;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw = new CliGateway( inputReader: () => "a" )
				interactionRequest = new HumanInteractionRequest( title: "Approval needed", message: "Run tool X?" )
				ctx = new GatewayContext( gateway: "cli" )

				result = gw.requestHumanInteraction( interactionRequest, ctx )
				hasDecision = result.hasDecision()
				isApproved  = result.getDecision().isApproved()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasDecision" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isApproved" ) ) ).isTrue();
	}

	@DisplayName( "'reject' rejects with a reason" )
	@Test
	public void testReject() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.cli.CliGateway;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw = new CliGateway( inputReader: () => "reject" )
				interactionRequest = new HumanInteractionRequest( title: "Approval needed", message: "Run tool X?" )
				ctx = new GatewayContext( gateway: "cli" )

				result = gw.requestHumanInteraction( interactionRequest, ctx )
				isRejected = result.getDecision().isRejected()
				hasReason  = len( result.getDecision().getReason() ) > 0
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isRejected" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasReason" ) ) ).isTrue();
	}

	@DisplayName( "'q' cancels with a reason" )
	@Test
	public void testQuitCancels() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.cli.CliGateway;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw = new CliGateway( inputReader: () => "q" )
				interactionRequest = new HumanInteractionRequest( title: "Approval needed", message: "Run tool X?" )
				ctx = new GatewayContext( gateway: "cli" )

				result = gw.requestHumanInteraction( interactionRequest, ctx )
				isCancelled = result.getDecision().isCancelled()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCancelled" ) ) ).isTrue();
	}

	@DisplayName( "unrecognized input re-prompts, then succeeds once a valid decision arrives" )
	@Test
	public void testRepromptsOnUnrecognizedInput() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.cli.CliGateway;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				inputs = [ "xyz", "a" ]
				gw = new CliGateway( inputReader: () => {
					var next = inputs[ 1 ]
					inputs.deleteAt( 1 )
					return next
				} )
				interactionRequest = new HumanInteractionRequest( title: "Approval needed", message: "Run tool X?" )
				ctx = new GatewayContext( gateway: "cli" )

				result = gw.requestHumanInteraction( interactionRequest, ctx )
				isApproved = result.getDecision().isApproved()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isApproved" ) ) ).isTrue();
	}

	@DisplayName( "exhausting all re-prompt attempts on unrecognized input falls back to cancel" )
	@Test
	public void testRepromptExhaustionCancels() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.gateway.cli.CliGateway;
				import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
				import bxModules.bxai.models.gateway.contracts.GatewayContext;

				gw = new CliGateway( inputReader: () => "not-a-valid-choice" )
				interactionRequest = new HumanInteractionRequest( title: "Approval needed", message: "Run tool X?" )
				ctx = new GatewayContext( gateway: "cli" )

				result = gw.requestHumanInteraction( interactionRequest, ctx )
				isCancelled = result.getDecision().isCancelled()
				hasReason   = len( result.getDecision().getReason() ) > 0
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCancelled" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasReason" ) ) ).isTrue();
	}

}
