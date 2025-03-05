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
package ortus.boxlang.moduleslug;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * This loads the module and runs an integration test on the module.
 */
public class IntegrationTest extends BaseIntegrationTest {

	@DisplayName( "Test the module loads in BoxLang" )
	@Test
	public void testModuleLoads() {
		// Given

		// Then
		assertThat( moduleService.getRegistry().containsKey( moduleName ) ).isTrue();

		// Verify things got registered
		// assertThat( datasourceService.hasDriver( Key.of( "derby" ) ) ).isTrue();

		// Register a named datasource
		// runtime.getConfiguration().runtime.datasources.put(
		// Key.of( "derby" ),
		// DatasourceConfig.fromStruct( Struct.of(
		// "name", "derby",
		// "driver", "derby",
		// "properties", Struct.of(
		// "database", "testDB",
		// "protocol", "memory"
		// )
		// ) )
		// );

		// @formatter:off
		runtime.executeSource(
		    """
			// Testing code here
			""",
		    context
		);
		// @formatter:on

		// Asserts here

	}
}
