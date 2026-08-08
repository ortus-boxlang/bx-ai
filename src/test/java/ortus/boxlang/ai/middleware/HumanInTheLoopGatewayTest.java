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
 * Verifies HumanInTheLoopMiddleware's Phase 2 gateway-attached path: presentation
 * delegated to an IGateway (via HumanInteractionCoordinator) instead of the
 * built-in cli/web behavior, plus the policy and approvalCallback wiring.
 */
package ortus.boxlang.ai.middleware;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@DisplayName( "HumanInTheLoopMiddleware Gateway-Attached Tests" )
public class HumanInTheLoopGatewayTest extends BaseIntegrationTest {

	@DisplayName( "gateway attached + scripted 'approve': beforeToolCall returns approve" )
	@Test
	public void testGatewayScriptedApprove() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				gw = aiGateway( "mock" )
				gw.setScriptedDecisions( [ "approve" ] )

				mw = new HumanInTheLoopMiddleware(
					toolsRequiringApproval: [ "placeOrder" ],
					gateway               : gw
				)

				result = mw.beforeToolCall( context: { toolName: "placeOrder", toolCall: {} } )
				isApproved = result.isApproved()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isApproved" ) ) ).isTrue();
	}

	@DisplayName( "with no decisionStore supplied, the coordinator defaults from settings.hitl.decisionStore" )
	@Test
	public void testCoordinatorDefaultsDecisionStoreFromSettings() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "placeOrder" ], gateway: aiGateway( "mock" ) )
				storeAttached = !isNull( mw.getCoordinator().getDecisionStore() )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "storeAttached" ) ) ).isTrue();
	}

	@DisplayName( "an explicit decisionStore passed to the middleware wins over the settings default" )
	@Test
	public void testExplicitDecisionStoreOverridesDefault() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				myStore = aiDecisionStore( "file", { directoryPath: getTempDirectory() & "/bxai-mw-decision-store-test" } )
				mw = new HumanInTheLoopMiddleware(
					toolsRequiringApproval: [ "placeOrder" ],
					gateway               : aiGateway( "mock" ),
					decisionStore         : myStore
				)
				sameInstance = mw.getCoordinator().getDecisionStore() == myStore
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sameInstance" ) ) ).isTrue();
	}

	@DisplayName( "gateway attached + scripted 'reject': beforeToolCall returns reject with reason" )
	@Test
	public void testGatewayScriptedReject() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				gw = aiGateway( "mock" )
				gw.setScriptedDecisions( [ { decision: "reject", reason: "too risky" } ] )

				mw = new HumanInTheLoopMiddleware(
					toolsRequiringApproval: [ "placeOrder" ],
					gateway               : gw
				)

				result = mw.beforeToolCall( context: { toolName: "placeOrder", toolCall: {} } )
				isRejected = result.isRejected()
				reason     = result.getReason()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isRejected" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "reason" ) ) ).isEqualTo( "too risky" );
	}

	@DisplayName( "gateway attached + scripted 'edit': patches tool args and returns continue" )
	@Test
	public void testGatewayScriptedEdit() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				gw = aiGateway( "mock" )
				gw.setScriptedDecisions( [ { decision: "edit", editedData: { correctedArgs: { qty: 9, item: "gadget" } } } ] )

				mw = new HumanInTheLoopMiddleware(
					toolsRequiringApproval: [ "placeOrder" ],
					gateway               : gw
				)

				toolCallStruct = { id: "call_1", function: { name: "placeOrder", arguments: '{"qty":1}' } }
				result = mw.beforeToolCall( context: { toolName: "placeOrder", toolCall: toolCallStruct } )
				isContinue = result.isContinue()

				parsedArgs    = jsonDeserialize( toolCallStruct.function.arguments )
				argQtyPatched = parsedArgs.qty == 9
				argItemSet    = parsedArgs.item == "gadget"
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isContinue" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "argQtyPatched" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "argItemSet" ) ) ).isTrue();
	}

	@DisplayName( "gateway attached + no scripted decision (async): beforeToolCall suspends carrying a suspensionID" )
	@Test
	public void testGatewayAsyncSuspends() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				gw = aiGateway( "mock" )

				mw = new HumanInTheLoopMiddleware(
					toolsRequiringApproval: [ "placeOrder" ],
					gateway               : gw
				)

				result = mw.beforeToolCall( context: { toolName: "placeOrder", toolCall: {} } )
				isSuspended  = result.isSuspended()
				hasSuspensionID = len( result.getData().suspensionID ?: "" ) > 0

				pending = gw.getPendingInteractions()
				presentedToGateway = pending.count() == 1
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasSuspensionID" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "presentedToGateway" ) ) ).isTrue();
	}

	@DisplayName( "gateway attached: unlisted tool still skips approval entirely (no gateway call)" )
	@Test
	public void testGatewaySkipsUnlistedTool() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				gw = aiGateway( "mock" )

				mw = new HumanInTheLoopMiddleware(
					toolsRequiringApproval: [ "placeOrder" ],
					gateway               : gw
				)

				result = mw.beforeToolCall( context: { toolName: "getWeather", toolCall: {} } )
				isContinue = result.isContinue()

				pending = gw.getPendingInteractions()
				noGatewayCall = pending.count() == 0
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isContinue" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noGatewayCall" ) ) ).isTrue();
	}

	@DisplayName( "a custom IApprovalPolicy overrides toolsRequiringApproval" )
	@Test
	public void testCustomPolicyOverridesToolNames() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
				import bxModules.bxai.models.hitl.policies.ToolNameApprovalPolicy;

				gw = aiGateway( "mock" )
				gw.setScriptedDecisions( [ "approve" ] )

				mw = new HumanInTheLoopMiddleware(
					toolsRequiringApproval: [ "thisNameIsIgnored" ],
					policy                : new ToolNameApprovalPolicy( [ "actualTool" ] ),
					gateway               : gw
				)

				ignoredMatch = mw.beforeToolCall( context: { toolName: "thisNameIsIgnored", toolCall: {} } )
				ignoredMatchIsContinue = ignoredMatch.isContinue()

				realMatch = mw.beforeToolCall( context: { toolName: "actualTool", toolCall: {} } )
				realMatchIsApproved = realMatch.isApproved()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "ignoredMatchIsContinue" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "realMatchIsApproved" ) ) ).isTrue();
	}

	@DisplayName( "approvalCallback decides whether approval is needed when no explicit policy is given" )
	@Test
	public void testApprovalCallbackWiring() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				gw = aiGateway( "mock" )
				gw.setScriptedDecisions( [ "approve" ] )

				mw = new HumanInTheLoopMiddleware(
					approvalCallback: ( ctx ) => ( ctx.toolArgs.amount ?: 0 ) > 1000,
					gateway         : gw
				)

				bigSpend   = mw.beforeToolCall( context: { toolName: "transfer", toolArgs: { amount: 5000 } } )
				bigIsApproved = bigSpend.isApproved()

				smallSpend = mw.beforeToolCall( context: { toolName: "transfer", toolArgs: { amount: 5 } } )
				smallIsContinue = smallSpend.isContinue()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "bigIsApproved" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "smallIsContinue" ) ) ).isTrue();
	}

	// -------------------------------------------------------------------------
	// Phase 3 compatibility: default (mode "cli") auto-attaches a CliGateway
	// -------------------------------------------------------------------------

	@DisplayName( "compat: with no mode/gateway specified, the default constructed middleware auto-attaches a CliGateway" )
	@Test
	public void testDefaultConstructorAttachesCliGateway() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ] )
				isCliGateway = isInstanceOf( mw.getGateway(), "CliGateway" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCliGateway" ) ) ).isTrue();
	}

	@DisplayName( "compat: mode 'cli' explicitly also auto-attaches a CliGateway" )
	@Test
	public void testExplicitCliModeAttachesCliGateway() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "cli" )
				isCliGateway = isInstanceOf( mw.getGateway(), "CliGateway" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCliGateway" ) ) ).isTrue();
	}

	@DisplayName( "compat: mode 'web' still has no gateway attached and suspends as before" )
	@Test
	public void testWebModeStillHasNoGateway() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "web" )
				gatewayIsNull = isNull( mw.getGateway() )

				result = mw.beforeToolCall( context: { toolName: "deleteRecord", toolCall: {} } )
				isSuspended = result.isSuspended()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "gatewayIsNull" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isSuspended" ) ) ).isTrue();
	}

	@DisplayName( "compat: an unrecognized mode still falls back to a CliGateway rather than failing" )
	@Test
	public void testUnrecognizedModeFallsBackToCliGateway() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;

				mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "carrier-pigeon" )
				isCliGateway = isInstanceOf( mw.getGateway(), "CliGateway" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isCliGateway" ) ) ).isTrue();
	}

	// ---- onAttach(): linking to the owning agent's checkpointer ----

	@DisplayName( "onAttach: default CliGateway needs no checkpointer and attaches without error" )
	@Test
	public void testOnAttachCliGatewayNeedsNoCheckpointer() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
				import bxModules.bxai.models.runnables.AiModel;

				mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ] )
				model = new AiModel( service: aiService( "mock" ) )

				// No checkpointer configured on the agent at all
				agent = aiAgent( model: model, middleware: [ mw ] )
				checkpointerStillNull = isNull( mw.getCheckpointer() )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "checkpointerStillNull" ) ) ).isTrue();
	}

	@DisplayName( "onAttach: links the middleware's checkpointer to the agent's when one is configured" )
	@Test
	public void testOnAttachLinksCheckpointer() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
				import bxModules.bxai.models.runnables.AiModel;

				mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "placeOrder" ], gateway: aiGateway( "mock" ) )
				model = new AiModel( service: aiService( "mock" ) )
				cp = aiMemory( "cache" )

				agent = aiAgent( model: model, middleware: [ mw ], checkpointer: cp )
				sameInstance = mw.getCheckpointer() == cp
				sameAsAgent = mw.getCheckpointer() == agent.getCheckpointer()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sameInstance" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sameAsAgent" ) ) ).isTrue();
	}

	@DisplayName( "onAttach: throws at attach time when a non-CLI gateway is attached with no checkpointer" )
	@Test
	public void testOnAttachThrowsForAsyncGatewayWithoutCheckpointer() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> {
			// @formatter:off
			runtime.executeSource(
				"""
					import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
					import bxModules.bxai.models.runnables.AiModel;

					mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "placeOrder" ], gateway: aiGateway( "mock" ) )
					model = new AiModel( service: aiService( "mock" ) )

					// No checkpointer configured — this should fail loudly right here, not later
					agent = aiAgent( model: model, middleware: [ mw ] )
				""",
				context
			);
			// @formatter:on
		} );

		assertThat( e.getMessage() ).contains( "no checkpointer configured" );
	}

	@DisplayName( "onAttach: throws at attach time for mode 'web' (no gateway at all) with no checkpointer" )
	@Test
	public void testOnAttachThrowsForWebModeWithoutCheckpointer() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> {
			// @formatter:off
			runtime.executeSource(
				"""
					import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
					import bxModules.bxai.models.runnables.AiModel;

					mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "web" )
					model = new AiModel( service: aiService( "mock" ) )

					agent = aiAgent( model: model, middleware: [ mw ] )
				""",
				context
			);
			// @formatter:on
		} );

		assertThat( e.getMessage() ).contains( "mode \"web\"" );
	}

	@DisplayName( "onAttach: mode 'web' with a checkpointer configured attaches without error" )
	@Test
	public void testOnAttachWebModeWithCheckpointerIsFine() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
				import bxModules.bxai.models.runnables.AiModel;

				mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ], mode: "web" )
				model = new AiModel( service: aiService( "mock" ) )
				cp = aiMemory( "cache" )

				agent = aiAgent( model: model, middleware: [ mw ], checkpointer: cp )
				linked = mw.getCheckpointer() == cp
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "linked" ) ) ).isTrue();
	}

	@DisplayName( "onAttach: the middleware is reachable back out via agent.getMiddlewareByName()" )
	@Test
	public void testHitlMiddlewareReachableViaGetMiddleware() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.middleware.core.HumanInTheLoopMiddleware;
				import bxModules.bxai.models.runnables.AiModel;

				mw = new HumanInTheLoopMiddleware( toolsRequiringApproval: [ "deleteRecord" ] )
				model = new AiModel( service: aiService( "mock" ) )

				agent = aiAgent( model: model, middleware: [ mw ] )
				sameInstance = agent.getMiddlewareByName( mw.getName() ) == mw
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sameInstance" ) ) ).isTrue();
	}

}
