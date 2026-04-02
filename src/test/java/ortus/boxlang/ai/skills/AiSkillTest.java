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
package ortus.boxlang.ai.skills;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@DisplayName( "AiSkill Domain Object Tests" )
public class AiSkillTest extends BaseIntegrationTest {

	private static final String SKILLS_DIR = Paths.get( "src/test/resources/skills" ).toAbsolutePath().toString();

	// -------------------------------------------------------------------------
	// init() — inline construction
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "init() creates skill from explicit inline values" )
	public void testInlineConstruction() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill = new AiSkill(
		        name       : "code-style",
		        description: "Enforces consistent code style",
		        content    : "Always use tabs for indentation."
		    );

		    skillName  = skill.getName();
		    skillDesc  = skill.getDescription();
		    skillBody  = skill.getContent();
		    skillPath  = skill.getFilePath();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "skillName" ) ).toString() ).isEqualTo( "code-style" );
		assertThat( variables.get( Key.of( "skillDesc" ) ).toString() ).isEqualTo( "Enforces consistent code style" );
		assertThat( variables.get( Key.of( "skillBody" ) ).toString() ).isEqualTo( "Always use tabs for indentation." );
		assertThat( variables.get( Key.of( "skillPath" ) ).toString() ).isEmpty();
	}

	@Test
	@DisplayName( "init() delegates to fromPath() when path is provided" )
	public void testInitWithPath() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill     = new AiSkill( path: "%s/sql-optimizer/SKILL.md" );
		    skillName = skill.getName();
		    skillPath = skill.getFilePath();
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "skillName" ) ).toString() ).isEqualTo( "sql-optimizer" );
		assertThat( variables.get( Key.of( "skillPath" ) ).toString() ).isNotEmpty();
	}

	// -------------------------------------------------------------------------
	// fromPath()
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "fromPath() parses the description frontmatter field" )
	public void testFromPathFullFrontmatter() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill = AiSkill::fromPath( "%s/sql-optimizer/SKILL.md" );

		    skillName = skill.getName();
		    skillDesc = skill.getDescription();
		    hasBody   = skill.getContent().len() > 0;
		    hasPath   = skill.getFilePath().len() > 0;
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "skillName" ) ).toString() ).isEqualTo( "sql-optimizer" );
		assertThat( variables.get( Key.of( "skillDesc" ) ).toString() )
		    .isEqualTo( "Expert SQL query optimization guidance. Use when writing or reviewing SQL queries." );
		assertThat( variables.getAsBoolean( Key.of( "hasBody" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasPath" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "fromPath() reads the combined description field (Claude standard)" )
	public void testFromPathDescriptionOnly() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill     = AiSkill::fromPath( "%s/coding/boxlang-expert/SKILL.md" );
		    skillDesc = skill.getDescription();
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "skillDesc" ) ).toString() )
		    .isEqualTo( "BoxLang language rules and idioms. Use when writing or reviewing BoxLang code." );
	}

	@Test
	@DisplayName( "fromPath() derives name from filename and description from first paragraph when no frontmatter" )
	public void testFromPathDerivedName() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill     = AiSkill::fromPath( "%s/no-frontmatter/SKILL.md" );
		    skillName = skill.getName();
		    skillDesc = skill.getDescription();
		    hasBody   = skill.getContent().len() > 0;
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		// No frontmatter name → derived from the literal filename "SKILL" (without extension)
		assertThat( variables.get( Key.of( "skillName" ) ).toString() ).isEqualTo( "SKILL" );
		// No frontmatter description → falls back to first paragraph of body
		assertThat( variables.get( Key.of( "skillDesc" ) ).toString() ).isEqualTo( "No frontmatter here, just plain body content." );
		assertThat( variables.getAsBoolean( Key.of( "hasBody" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "fromPath() appends .md extension automatically when missing" )
	public void testFromPathAutoExtension() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    // Path without extension — should resolve to SKILL.md
		    skill     = AiSkill::fromPath( "%s/sql-optimizer/SKILL" );
		    skillName = skill.getName();
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "skillName" ) ).toString() ).isEqualTo( "sql-optimizer" );
	}

	@Test
	@DisplayName( "fromPath() throws AiSkill.NotFound for a non-existent file" )
	public void testFromPathNotFound() {
		assertThrows( BoxRuntimeException.class, () -> {
			// @formatter:off
			runtime.executeSource(
			    """
			    import bxModules.bxai.models.skills.AiSkill;
			    AiSkill::fromPath( "/absolutely/missing/SKILL.md" );
			    """,
			    context
			);
			// @formatter:on
		} );
	}

	// -------------------------------------------------------------------------
	// fromDirectory()
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "fromDirectory() discovers all SKILL.md files recursively by default" )
	public void testFromDirectoryRecursive() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skills    = AiSkill::fromDirectory( "%s" );
		    countIs3  = skills.len() == 3;
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		// 3 fixtures: sql-optimizer, coding/boxlang-expert, no-frontmatter
		assertThat( variables.getAsBoolean( Key.of( "countIs3" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "fromDirectory() with recurse=false only finds top-level SKILL.md files" )
	public void testFromDirectoryNonRecursive() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skills        = AiSkill::fromDirectory( "%s", false );
		    countIsZero   = skills.len() == 0;
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		// No SKILL.md at the root level — all are in subdirectories, so recurse=false yields 0
		assertThat( variables.getAsBoolean( Key.of( "countIsZero" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "fromDirectory() returns empty array when directory does not exist" )
	public void testFromDirectoryMissingDir() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skills       = AiSkill::fromDirectory( "/path/that/does/not/exist" );
		    countIsZero  = skills.len() == 0;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "countIsZero" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "fromDirectory() returns AiSkill instances with correct properties" )
	public void testFromDirectorySkillProperties() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skills     = AiSkill::fromDirectory( "%s" );
		    matching   = skills.filter( s => s.getName() == "sql-optimizer" );
		    foundSkill = matching.len() > 0;
		    skillDesc  = foundSkill ? matching[1].getDescription() : "";
		    """.formatted( SKILLS_DIR ),
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "foundSkill" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "skillDesc" ) ).toString() )
		    .isEqualTo( "Expert SQL query optimization guidance. Use when writing or reviewing SQL queries." );
	}

	// -------------------------------------------------------------------------
	// toIndexLine()
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "toIndexLine() uses description as the hint" )
	public void testToIndexLineWithDescription() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill  = new AiSkill( name: "sql-tips", description: "Use for SQL reviews" );
		    result = skill.toIndexLine();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ).toString() ).isEqualTo( "- sql-tips: Use for SQL reviews" );
	}

	@Test
	@DisplayName( "toIndexLine() includes description as the hint" )
	public void testToIndexLineFallsBackToDescription() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill  = new AiSkill( name: "code-style", description: "Enforces code standards" );
		    result = skill.toIndexLine();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ).toString() ).isEqualTo( "- code-style: Enforces code standards" );
	}

	@Test
	@DisplayName( "toIndexLine() omits the hint when both whenToUse and description are empty" )
	public void testToIndexLineNoHint() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill  = new AiSkill( name: "bare-skill" );
		    result = skill.toIndexLine();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ).toString() ).isEqualTo( "- bare-skill" );
	}

	// -------------------------------------------------------------------------
	// toContentBlock()
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "toContentBlock() includes skill name, description, and content" )
	public void testToContentBlockFull() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill  = new AiSkill(
		        name        : "sql-tips",
		        description : "Use for SQL reviews",
		        content     : "Prefer indexed columns."
		    );
		    result = skill.toContentBlock();

		    hasHeader     = result.findNoCase( "#### Skill: sql-tips" ) > 0;
		    hasDesc       = result.findNoCase( "Use for SQL reviews" ) > 0;
		    hasContent    = result.findNoCase( "Prefer indexed columns." ) > 0;
		    hasWhenPrefix = result.findNoCase( "**When to use:**" ) > 0;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasHeader" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasDesc" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasContent" ) ) ).isTrue();
		// description is inlined directly — no "When to use:" label
		assertThat( variables.getAsBoolean( Key.of( "hasWhenPrefix" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "toContentBlock() falls back to description when whenToUse is empty" )
	public void testToContentBlockDescriptionFallback() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill  = new AiSkill( name: "tips", description: "General tips", content: "Body." );
		    result = skill.toContentBlock();

		    hasDesc = result.findNoCase( "General tips" ) > 0;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasDesc" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "toContentBlock() omits hint section when description and whenToUse are both empty" )
	public void testToContentBlockNoHint() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.skills.AiSkill;

		    skill  = new AiSkill( name: "bare", content: "Content only." );
		    result = skill.toContentBlock();

		    hasHeader  = result.findNoCase( "#### Skill: bare" ) > 0;
		    hasWhen    = result.findNoCase( "**When to use:**" ) > 0;
		    hasContent = result.findNoCase( "Content only." ) > 0;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasHeader" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasWhen" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "hasContent" ) ) ).isTrue();
	}

}
