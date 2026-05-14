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

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "aiSkill() BIF Tests" )
public class aiSkillBIFTest extends BaseIntegrationTest {

	private static final String SKILLS_DIR = Paths.get( "src/test/resources/skills" ).toAbsolutePath().toString();

	@Test
	@DisplayName( "aiSkill() returns an empty array when the default /.agents/skills directory does not exist" )
	public void testDefaultDirectoryMissing() {
		// @formatter:off
		runtime.executeSource(
		    """
		    skills      = aiSkill();
		    isArray     = isArray( skills );
		    countIsZero = skills.len() == 0;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isArray" ) ) ).isTrue();
		// The default /.agents/skills dir does not exist in the test environment
		assertThat( variables.getAsBoolean( Key.of( "countIsZero" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "aiSkill( directory ) discovers all SKILL.md files recursively" )
	public void testCustomDirectoryRecursive() {
		// @formatter:off
		runtime.executeSource(
		    """
		    skills   = aiSkill( "%s" );
			println( skills )
		    isArray  = isArray( skills );
		    countIs3 = skills.len() == 3;
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		// 3 fixture files: sql-optimizer, coding/boxlang-expert, no-frontmatter
		assertThat( variables.getAsBoolean( Key.of( "isArray" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "countIs3" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "aiSkill( directory, false ) finds only SKILL.md files directly in the given directory" )
	public void testCustomDirectoryNonRecursive() {
		// @formatter:off
		runtime.executeSource(
		    """
		    skills       = aiSkill( path:"%s", recurse: false );
		    countIsZero  = skills.len() == 0;
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		// All fixture SKILL.md files are inside subdirectories; none sit directly in the root
		assertThat( variables.getAsBoolean( Key.of( "countIsZero" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "aiSkill() returns an empty array when the given directory does not exist" )
	public void testMissingDirectory() {
		// @formatter:off
		runtime.executeSource(
		    """
		    skills       = aiSkill( "/path/that/does/not/exist" );
		    isArray      = isArray( skills );
		    countIsZero  = skills.len() == 0;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isArray" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "countIsZero" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "aiSkill() items are AiSkill instances with expected properties" )
	public void testSkillItemProperties() {
		// @formatter:off
		runtime.executeSource(
		    """
		    skills    = aiSkill( "%s" );
		    matching  = skills.filter( s => s.getName() == "sql-optimizer" );
		    hasSkill  = matching.len() > 0;
		    skillDesc = hasSkill ? matching[1].getDescription() : "";
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasSkill" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "skillDesc" ) ).toString() )
		    .isEqualTo( "Expert SQL query optimization guidance. Use when writing or reviewing SQL queries." );
	}

	@Test
	@DisplayName( "aiSkill() works with a single-skill subdirectory path" )
	public void testSingleSkillDirectory() {
		// @formatter:off
		runtime.executeSource(
		    """
		    skills    = aiSkill( "%s/sql-optimizer" );
		    countIs1  = skills.len() == 1;
		    skillName = countIs1 ? skills[1].getName() : "";
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "countIs1" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "skillName" ) ).toString() ).isEqualTo( "sql-optimizer" );
	}

	@DisplayName( "aiSkill() with name, etc, creates a new skill instance without needing a file" )
	@Test
	public void testInlineSkillCreation() {
		// @formatter:off
		runtime.executeSource(
		    """
		    skill = aiSkill( name: "inline-skill", description: "An inline skill created directly from arguments.", content: "This is the body of the inline skill." );

		    hasName        = skill.getName() == "inline-skill";
		    hasDescription = skill.getDescription() == "An inline skill created directly from arguments.";
		    hasContent     = skill.getContent() == "This is the body of the inline skill.";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasName" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasDescription" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasContent" ) ) ).isTrue();
	}

	@DisplayName( "aiSkill() with a single file path loads that skill" )
	@Test
	public void testSingleFilePath() {
		// @formatter:off
		runtime.executeSource(
		    """
		    skill = aiSkill( path: "%s/no-frontmatter/SKILL.md" );

		    hasName    = skill.getName() == "SKILL";
		    hasContent = skill.getContent() == "No frontmatter here, just plain body content.\n\nSecond paragraph: skill name is derived from the filename.";
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasName" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasContent" ) ) ).isTrue();
	}

}
