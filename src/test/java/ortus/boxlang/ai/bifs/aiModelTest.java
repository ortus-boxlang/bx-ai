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

import org.junit.jupiter.api.DisplayName;
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

	@Test
	public void testGetConfig() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel( "openai" )
					.withParams( { temperature: 0.7, model: "gpt-4" } )
					.withName( "TestModel" )
				config = model.getConfig()
			""",
			context
		);
		// @formatter:on

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertThat( config.get( Key.of( "name" ) ) ).isEqualTo( "TestModel" );
		assertThat( config.get( Key.of( "provider" ) ) ).isEqualTo( "OpenAI" );
		assertThat( config.get( Key.of( "toolCount" ) ) ).isEqualTo( 0 );

		var params = ( ortus.boxlang.runtime.types.IStruct ) config.get( Key.of( "params" ) );
		assertThat( params.get( Key.of( "model" ) ) ).isEqualTo( "gpt-4" );
		assertThat( params.get( Key.of( "temperature" ) ).toString() ).isEqualTo( "0.7" );
	}

	@Test
	public void testGetConfigWithTools() {
		// @formatter:off
		runtime.executeSource(
			"""
				tool1 = aiTool(
					"calculator",
					"Performs calculations",
					args => evaluate( args.expression )
				).describeExpression( "Math expression to evaluate" )

				tool2 = aiTool(
					"search",
					"Searches information",
					args => "Search result"
				).describeQuery( "Search query" )

				model = aiModel( "openai" )
					.withTools( [ tool1, tool2 ] )

				config = model.getConfig()
			""",
			context
		);
		// @formatter:on

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertThat( config.get( Key.of( "toolCount" ) ) ).isEqualTo( 2 );
	}

	@Test
	public void testGetConfigDefaultModel() {
		// @formatter:off
		runtime.executeSource(
			"""
				model = aiModel()
				config = model.getConfig()
			""",
			context
		);
		// @formatter:on

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertThat( config.get( Key.of( "name" ) ).toString() ).contains( "AiModel" );
		assertThat( config.get( Key.of( "provider" ) ) ).isNotNull();
		assertThat( config.containsKey( Key.of( "toolCount" ) ) ).isTrue();
		assertThat( config.containsKey( Key.of( "params" ) ) ).isTrue();
		assertThat( config.containsKey( Key.of( "options" ) ) ).isTrue();
	}

	// ==================== SKILL TESTS ====================

	@Test
	@DisplayName( "Model accepts always-on skills at construction time via withSkills()" )
	public void testModelWithSkills() {
		// @formatter:off
		runtime.executeSource(
			"""
				skill1 = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
				skill2 = aiSkill( name: "code-style", description: "Code style guide", content: "Prefer tabs over spaces." )

				model = aiModel( "openai" ).withSkills( [ skill1, skill2 ] )
				skillCount = model.getSkills().len()
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "skillCount" ) ) ).intValue() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Model accepts a single always-on skill via addSkill()" )
	public void testModelAddSkillFluent() {
		// @formatter:off
		runtime.executeSource(
			"""
				skill = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
				model = aiModel( "openai" ).addSkill( skill )
				skillCount = model.getSkills().len()
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "skillCount" ) ) ).intValue() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Model accepts lazy available-skills via withAvailableSkills()" )
	public void testModelWithAvailableSkills() {
		// @formatter:off
		runtime.executeSource(
			"""
				skill1 = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
				skill2 = aiSkill( name: "code-style", description: "Code style guide", content: "Prefer tabs over spaces." )

				model = aiModel( "openai" ).withAvailableSkills( [ skill1, skill2 ] )
				availCount = model.getAvailableSkills().len()
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "availCount" ) ) ).intValue() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Model accepts a single lazy skill via addAvailableSkill()" )
	public void testModelAddAvailableSkillFluent() {
		// @formatter:off
		runtime.executeSource(
			"""
				skill = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
				model = aiModel( "openai" ).addAvailableSkill( skill )
				availCount = model.getAvailableSkills().len()
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "availCount" ) ) ).intValue() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "buildSkillsContent() returns non-empty string when always-on skills are present" )
	public void testModelBuildSkillsContent() {
		// @formatter:off
		runtime.executeSource(
			"""
				skill = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
				model = aiModel( "openai" ).withSkills( [ skill ] )

				content       = model.buildSkillsContent()
				hasHeader     = content.findNoCase( "## Skills" ) > 0
				hasSkillBlock = content.findNoCase( "#### Skill: sql-tips" ) > 0
				hasContent    = content.findNoCase( "Always use indexed columns." ) > 0
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasHeader" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasSkillBlock" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasContent" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "buildSkillsContent() includes lazy skill index when availableSkills are set" )
	public void testModelBuildSkillsContentWithAvailableSkills() {
		// @formatter:off
		runtime.executeSource(
			"""
				skill = aiSkill( name: "lazy-skill", description: "Use for lazy operations", content: "Be lazy." )
				model = aiModel( "openai" ).withAvailableSkills( [ skill ] )

				content         = model.buildSkillsContent()
				hasAvailHeader  = content.findNoCase( "## Available Skills" ) > 0
				hasSkillIndex   = content.findNoCase( "- lazy-skill:" ) > 0
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasAvailHeader" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasSkillIndex" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "buildSkillsContent() returns empty string when no skills are set" )
	public void testModelBuildSkillsContentEmpty() {
		// @formatter:off
		runtime.executeSource(
			"""
				model   = aiModel( "openai" )
				content = model.buildSkillsContent()
				isEmpty = content.isEmpty()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isEmpty" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "activateSkill() promotes a lazy skill to always-on pool" )
	public void testModelActivateSkill() {
		// @formatter:off
		runtime.executeSource(
			"""
				skill = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
				model = aiModel( "openai" ).withAvailableSkills( [ skill ] )

				// Before activation
				beforeAvail  = model.getAvailableSkills().len()
				beforeActive = model.getSkills().len()

				// Activate
				model.activateSkill( "sql-tips" )

				// After activation
				afterAvail  = model.getAvailableSkills().len()
				afterActive = model.getSkills().len()
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "beforeAvail" ) ) ).intValue() ).isEqualTo( 1 );
		assertThat( ( ( Number ) variables.get( Key.of( "beforeActive" ) ) ).intValue() ).isEqualTo( 0 );
		assertThat( ( ( Number ) variables.get( Key.of( "afterAvail" ) ) ).intValue() ).isEqualTo( 0 );
		assertThat( ( ( Number ) variables.get( Key.of( "afterActive" ) ) ).intValue() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "aiModel() BIF accepts skills at construction time" )
	public void testAiModelBIFWithSkills() {
		// @formatter:off
		runtime.executeSource(
			"""
				skill = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
				model = aiModel( skills: [ skill ] )
				skillCount = model.getSkills().len()
			""",
			context
		);
		// @formatter:on

		assertThat( ( ( Number ) variables.get( Key.of( "skillCount" ) ) ).intValue() ).isEqualTo( 1 );
	}
}
