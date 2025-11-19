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
package ortus.boxlang.ai;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that BaseIntegrationTest correctly initializes
 * and that the module name is properly set without KeyDictionary
 */
public class BaseIntegrationTestVerification extends BaseIntegrationTest {

	@DisplayName( "Module name should be 'bxai'" )
	@Test
	public void testModuleName() {
		assertThat( moduleName ).isNotNull();
		assertThat( moduleName.getName() ).isEqualTo( "bxai" );
	}

	@DisplayName( "Module should be loaded" )
	@Test
	public void testModuleLoaded() {
		assertThat( runtime ).isNotNull();
		assertThat( moduleService ).isNotNull();
		assertThat( moduleService.hasModule( moduleName ) ).isTrue();
	}

	@DisplayName( "Module record should be accessible" )
	@Test
	public void testModuleRecord() {
		assertThat( moduleRecord ).isNotNull();
		assertThat( moduleRecord.mapping.getName() ).isEqualTo( "bxai" );
	}
}
