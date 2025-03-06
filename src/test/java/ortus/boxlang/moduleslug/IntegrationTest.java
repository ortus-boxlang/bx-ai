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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.cdimascio.dotenv.Dotenv;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

/**
 * This loads the module and runs an integration test on the module.
 */
public class IntegrationTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setup() {
		Dotenv dotenv = Dotenv.load();
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
	}

	@DisplayName( "Test the module loads in BoxLang" )
	@Test
	public void testModuleLoads() {
		// Given

		// Then
		assertThat( moduleService.getRegistry().containsKey( moduleName ) ).isTrue();

		// @formatter:off
		runtime.executeSource(
			"""
			result = aiChat( "what is boxlang?" )
			println( result )
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test the async chat ai" )
	@Test
	public void testAsyncChat() {
		// Given

		// Then
		assertThat( moduleService.getRegistry().containsKey( moduleName ) ).isTrue();

		// @formatter:off
		runtime.executeSource(
			"""
			future = aiChatAsync( "what is boxlang?" )
			println( future.get() )
			""",
			context
		);
		// @formatter:on

		// Asserts here
	}

	@DisplayName( "Test the tool calls" )
	@Test
	public void testToolCall() {
		// Given

		// Then
		assertThat( moduleService.getRegistry().containsKey( moduleName ) ).isTrue();

		// @formatter:off
		runtime.executeSource(
			"""
			tool = new bxmodules.bxai.models.Tool();

			tool.setName( "get_weather" )
				.describe( "Get current temperature for a given location." )
				.describeLocation( "City and country e.g. Bogotá, Colombia" )
				.setFunc( ( location ) => {
					if( location contains "Kansas City" ) {
						return "85"
					}

					if( location contains "San Salvador" ){
						return "90"
					}

					return "unknown";
				});

			result = aiChat( messages = "How hot is it in Kansas City? What about San Salvador? Answer with only the name of the warmer city, nothing else.", data = {
				tools: [ tool ]
			} )
			println( result )
			""",
			context
		);
		// @formatter:on

		// Asserts here
		Struct choice = ( Struct ) variables.getAsStruct( Key.of( "result" ) ).getAsArray( Key.of( "choices" ) ).get( 0 );
		assertThat( choice.getAsStruct( Key.of( "message" ) ).get( "content" ) ).isEqualTo( "San Salvador" );
	}
}
