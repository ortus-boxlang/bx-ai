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
 */
package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class aiModelTest extends BaseIntegrationTest {

	@Test
	public void testCreateDefaultModel() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = aiModel()
				name = result.getName()
			""",
			context
		);
		// @formatter:on

		var model = variables.get( result );
		assertThat( model ).isNotNull();
		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "AiModel-OpenAI" );
	}

	@Test
	public void testCreateModelWithProvider() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = aiModel( "openrouter" )
				name = result.getName()
			""",
			context
		);
		// @formatter:on

		var model = variables.get( result );
		assertThat( model ).isNotNull();
		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "AiModel-OpenRouter" );
	}

	@Test
	public void testModelHasService() {
		// @formatter:off
		runtime.executeSource(
			"""
				result = aiModel( "openai" )
				service = result.getService()
				serviceName = service.getName()
			""",
			context
		);
		// @formatter:on

		var service = variables.get( Key.of( "service" ) );
		assertThat( service ).isNotNull();
		assertThat( variables.get( Key.of( "serviceName" ) ) ).isEqualTo( "OpenAI" );
	}

	@Test
	public void testModelAsRunnable() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel( "openai" )
				result = {
					hasRun: structKeyExists( model, "run" ),
					hasStream: structKeyExists( model, "stream" ),
					hasTo: structKeyExists( model, "to" )
				}
			""",
			context
		);
		// @formatter:on

		var resultStruct = variables.getAsStruct( result );
		assertThat( resultStruct.get( Key.of( "hasRun" ) ) ).isEqualTo( true );
		assertThat( resultStruct.get( Key.of( "hasStream" ) ) ).isEqualTo( true );
		assertThat( resultStruct.get( Key.of( "hasTo" ) ) ).isEqualTo( true );
	}

	@Test
	public void testModelWithParams() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel( "openai" )
					.withParams( { temperature: 0.5, model: "gpt-4" } )
				result = model.getMergedParams()
			""",
			context
		);
		// @formatter:on

		var params = variables.getAsStruct( result );
		assertThat( params.get( Key.of( "temperature" ) ).toString() ).isEqualTo( "0.5" );
		assertThat( params.get( Key.of( "model" ) ) ).isEqualTo( "gpt-4" );
	}

	@Test
	public void testModelWithName() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel( "openai" )
					.withName( "MyCustomModel" )
				result = model.getName()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isEqualTo( "MyCustomModel" );
	}

	@Test
	public void testModelChaining() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel( "openai" )
				transformer = aiTransform( r => r.content ?: "no content" )
				chain = model.to( transformer )
				result = chain.count()
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( result ) ).intValue() ).isEqualTo( 2 );
	}

	@Test
	public void testModelTransform() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel( "openai" )
				chain = model.transform( r => r.content ?: "default" )
				result = chain.count()
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( result ) ).intValue() ).isEqualTo( 2 );
	}
}
