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
 * Verifies each built-in IApprovalPolicy decides needsApproval() correctly in
 * isolation, and that CompositeApprovalPolicy combines several with the documented
 * any/all semantics.
 */
package ortus.boxlang.ai.hitl;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Approval Policy Tests" )
public class ApprovalPolicyTest extends BaseIntegrationTest {

	// -------------------------------------------------------------------------
	// ToolNameApprovalPolicy
	// -------------------------------------------------------------------------

	@DisplayName( "ToolNameApprovalPolicy: matches a listed tool name, case-insensitively" )
	@Test
	public void testToolNamePolicyMatches() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.ToolNameApprovalPolicy;

				policy   = new ToolNameApprovalPolicy( [ "deleteRecord" ] )
				matches  = policy.needsApproval( { toolName: "DELETERECORD" } )
				noMatch  = policy.needsApproval( { toolName: "getWeather" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "matches" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noMatch" ) ) ).isFalse();
	}

	// -------------------------------------------------------------------------
	// AnnotationApprovalPolicy
	// -------------------------------------------------------------------------

	@DisplayName( "AnnotationApprovalPolicy: true for a tool whose doInvoke() carries the annotation" )
	@Test
	public void testAnnotationPolicyMatchesAnnotatedTool() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.AnnotationApprovalPolicy;

				policy    = new AnnotationApprovalPolicy()
				tool      = createObject( "src.test.bx.tools.RequiresApprovalTool" )
				needsIt   = policy.needsApproval( { tool: tool } )

				plainTool = createObject( "src.test.bx.tools.PlainTool" )
				noNeed    = policy.needsApproval( { tool: plainTool } )

				noTool    = policy.needsApproval( { toolName: "whatever" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "needsIt" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noNeed" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "noTool" ) ) ).isFalse();
	}

	// -------------------------------------------------------------------------
	// RiskLevelApprovalPolicy
	// -------------------------------------------------------------------------

	@DisplayName( "RiskLevelApprovalPolicy: requires approval when declared risk meets the threshold" )
	@Test
	public void testRiskLevelPolicyMeetsThreshold() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.RiskLevelApprovalPolicy;

				policy     = new RiskLevelApprovalPolicy( minLevel: "high" )
				riskyTool  = createObject( "src.test.bx.tools.HighRiskTool" )
				needsIt    = policy.needsApproval( { tool: riskyTool } )

				plainTool  = createObject( "src.test.bx.tools.PlainTool" )
				noNeed     = policy.needsApproval( { tool: plainTool } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "needsIt" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "noNeed" ) ) ).isFalse();
	}

	@DisplayName( "RiskLevelApprovalPolicy: a low threshold catches the unannotated default level too" )
	@Test
	public void testRiskLevelPolicyDefaultLevel() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.RiskLevelApprovalPolicy;

				policy    = new RiskLevelApprovalPolicy( minLevel: "low" )
				plainTool = createObject( "src.test.bx.tools.PlainTool" )
				needsIt   = policy.needsApproval( { tool: plainTool } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "needsIt" ) ) ).isTrue();
	}

	@DisplayName( "RiskLevelApprovalPolicy: an invalid minLevel/defaultLevel throws instead of misbehaving" )
	@Test
	public void testRiskLevelPolicyRejectsInvalidLevels() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.RiskLevelApprovalPolicy;

				badMinLevelThrew = false
				try {
					new RiskLevelApprovalPolicy( minLevel: "extreme" )
				} catch ( any e ) {
					badMinLevelThrew = true
				}

				badDefaultLevelThrew = false
				try {
					new RiskLevelApprovalPolicy( defaultLevel: "meh" )
				} catch ( any e ) {
					badDefaultLevelThrew = true
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "badMinLevelThrew" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "badDefaultLevelThrew" ) ) ).isTrue();
	}

	// -------------------------------------------------------------------------
	// CallbackApprovalPolicy
	// -------------------------------------------------------------------------

	@DisplayName( "CallbackApprovalPolicy: delegates the decision to the given closure" )
	@Test
	public void testCallbackPolicy() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.CallbackApprovalPolicy;

				policy   = new CallbackApprovalPolicy( ( context ) => context.toolArgs.amount > 1000 )
				bigSpend = policy.needsApproval( { toolArgs: { amount: 5000 } } )
				smallSpend = policy.needsApproval( { toolArgs: { amount: 10 } } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "bigSpend" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "smallSpend" ) ) ).isFalse();
	}

	// -------------------------------------------------------------------------
	// CompositeApprovalPolicy
	// -------------------------------------------------------------------------

	@DisplayName( "CompositeApprovalPolicy: mode 'any' requires approval if a single policy does" )
	@Test
	public void testCompositePolicyAnyMode() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.ToolNameApprovalPolicy;
				import bxModules.bxai.models.hitl.policies.CallbackApprovalPolicy;
				import bxModules.bxai.models.hitl.policies.CompositeApprovalPolicy;

				byName     = new ToolNameApprovalPolicy( [ "deleteRecord" ] )
				byCallback = new CallbackApprovalPolicy( ( context ) => false )
				composite  = new CompositeApprovalPolicy( [ byName, byCallback ], "any" )

				matchesOne = composite.needsApproval( { toolName: "deleteRecord" } )
				matchesNone = composite.needsApproval( { toolName: "getWeather" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "matchesOne" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "matchesNone" ) ) ).isFalse();
	}

	@DisplayName( "CompositeApprovalPolicy: mode 'all' requires approval only if every policy does" )
	@Test
	public void testCompositePolicyAllMode() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.ToolNameApprovalPolicy;
				import bxModules.bxai.models.hitl.policies.CallbackApprovalPolicy;
				import bxModules.bxai.models.hitl.policies.CompositeApprovalPolicy;

				byName        = new ToolNameApprovalPolicy( [ "deleteRecord" ] )
				alwaysTrue    = new CallbackApprovalPolicy( ( context ) => true )
				alwaysFalse   = new CallbackApprovalPolicy( ( context ) => false )

				bothTrue  = new CompositeApprovalPolicy( [ byName, alwaysTrue ], "all" )
				oneFalse  = new CompositeApprovalPolicy( [ byName, alwaysFalse ], "all" )

				allMatch    = bothTrue.needsApproval( { toolName: "deleteRecord" } )
				notAllMatch = oneFalse.needsApproval( { toolName: "deleteRecord" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "allMatch" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "notAllMatch" ) ) ).isFalse();
	}

	@DisplayName( "CompositeApprovalPolicy: an empty policy list never requires approval" )
	@Test
	public void testCompositePolicyEmpty() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.CompositeApprovalPolicy;

				composite = new CompositeApprovalPolicy( [] )
				result    = composite.needsApproval( { toolName: "anything" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isFalse();
	}

	@DisplayName( "CompositeApprovalPolicy: mode is normalized case-insensitively" )
	@Test
	public void testCompositePolicyModeIsCaseInsensitive() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.ToolNameApprovalPolicy;
				import bxModules.bxai.models.hitl.policies.CallbackApprovalPolicy;
				import bxModules.bxai.models.hitl.policies.CompositeApprovalPolicy;

				byName      = new ToolNameApprovalPolicy( [ "deleteRecord" ] )
				alwaysTrue  = new CallbackApprovalPolicy( ( context ) => true )
				composite   = new CompositeApprovalPolicy( [ byName, alwaysTrue ], "ALL" )

				result = composite.needsApproval( { toolName: "deleteRecord" } )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
	}

	@DisplayName( "CompositeApprovalPolicy: an invalid mode throws instead of silently defaulting" )
	@Test
	public void testCompositePolicyRejectsInvalidMode() {
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.hitl.policies.CompositeApprovalPolicy;

				threw = false
				try {
					new CompositeApprovalPolicy( [], "sometimes" )
				} catch ( any e ) {
					threw = true
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threw" ) ) ).isTrue();
	}

}
