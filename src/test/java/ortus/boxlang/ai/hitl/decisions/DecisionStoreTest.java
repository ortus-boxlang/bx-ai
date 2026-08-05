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
 * Verifies the three IDecisionStore backends (Cache/Jdbc/File) implement grant/isGranted/
 * revoke/listGrants identically, including expiry handling, and that the aiDecisionStore()
 * BIF resolves each type name correctly.
 */
package ortus.boxlang.ai.hitl.decisions;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "IDecisionStore Tests" )
public class DecisionStoreTest extends BaseIntegrationTest {

	@DisplayName( "aiDecisionStore() resolves cache/jdbc/file store types" )
	@Test
	public void testResolvesAllStoreTypes() {
		// @formatter:off
		runtime.executeSource(
			"""
				cacheStore = aiDecisionStore( "cache" )
				jdbcStore  = aiDecisionStore( "jdbc", { datasource: "bxai_test" } )
				fileStore  = aiDecisionStore( "file" )

				cacheOk = !isNull( cacheStore )
				jdbcOk  = !isNull( jdbcStore )
				fileOk  = !isNull( fileStore )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "cacheOk" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "jdbcOk" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "fileOk" ) ) ).isTrue();
	}

	@DisplayName( "aiDecisionStore() throws for an unknown store type" )
	@Test
	public void testUnknownStoreTypeThrows() {
		// @formatter:off
		runtime.executeSource(
			"""
				threw = false
				errorType = ""
				try {
					aiDecisionStore( "not-a-real-store" )
				} catch ( any e ) {
					threw = true
					errorType = e.type
				}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "threw" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "errorType" ) ) ).isEqualTo( "InvalidDecisionStoreType" );
	}

	@DisplayName( "CacheDecisionStore: grant/isGranted/revoke/listGrants round trip" )
	@Test
	public void testCacheDecisionStoreLifecycle() {
		assertLifecycle( "aiDecisionStore( \"cache\" )" );
	}

	@DisplayName( "FileDecisionStore: grant/isGranted/revoke/listGrants round trip" )
	@Test
	public void testFileDecisionStoreLifecycle() {
		assertLifecycle( "aiDecisionStore( \"file\", { directoryPath: getTempDirectory() & \"/bxai-decision-store-test\" } )" );
	}

	@DisplayName( "JdbcDecisionStore: grant/isGranted/revoke/listGrants round trip" )
	@Test
	public void testJdbcDecisionStoreLifecycle() {
		assertLifecycle( "aiDecisionStore( \"jdbc\", { datasource: \"bxai_test\" } )" );
	}

	/**
	 * Shared lifecycle assertion run against each backend — identical grant/isGranted/revoke/
	 * listGrants behavior is the whole point of the interface.
	 */
	private void assertLifecycle( String storeExpr ) {
		// @formatter:off
		runtime.executeSource(
			"""
				store = %s

				identity = "user-" & createUUID()
				notGrantedYet = !store.isGranted( identity, "sendEmail" )

				store.grant( identity, "sendEmail" )
				grantedNow = store.isGranted( identity, "sendEmail" )

				// A different tool for the same identity is unaffected
				otherToolStillUngranted = !store.isGranted( identity, "deleteRecord" )

				grants = store.listGrants( identity )
				listHasOne = grants.len() == 1
				listHasToolName = grants[ 1 ].toolName == "sendEmail"

				store.revoke( identity, "sendEmail" )
				revokedNow = !store.isGranted( identity, "sendEmail" )

				// Expired grant is treated as not-granted
				store.grant( identity, "deleteRecord", "", dateAdd( "s", -10, now() ) )
				expiredIsNotGranted = !store.isGranted( identity, "deleteRecord" )
			""".formatted( storeExpr ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "notGrantedYet" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "grantedNow" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "otherToolStillUngranted" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "listHasOne" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "listHasToolName" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "revokedNow" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "expiredIsNotGranted" ) ) ).isTrue();
	}

}
