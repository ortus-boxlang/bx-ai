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
package ortus.boxlang.ai.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class YamlFrontmatterParserTest extends BaseIntegrationTest {

	// -------------------------------------------------------------------------
	// parse() tests
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "parse() extracts all frontmatter keys and body" )
	public void testParseFullBlock() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    content = "---\nname: billing-policy\ndescription: Handles billing questions\nwhenToUse: User asks about charges\n---\nFull skill content here.";
		    result = YamlFrontmatterParser::parse( content );

		    fmName        = result.frontmatter.name;
		    fmDescription = result.frontmatter.description;
		    fmWhenToUse   = result.frontmatter.whenToUse;
		    bodyText      = result.body;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "fmName" ) ).toString() ).isEqualTo( "billing-policy" );
		assertThat( variables.get( Key.of( "fmDescription" ) ).toString() ).isEqualTo( "Handles billing questions" );
		assertThat( variables.get( Key.of( "fmWhenToUse" ) ).toString() ).isEqualTo( "User asks about charges" );
		assertThat( variables.get( Key.of( "bodyText" ) ).toString() ).isEqualTo( "Full skill content here." );
	}

	@Test
	@DisplayName( "parse() returns empty frontmatter and full content as body when no --- delimiter" )
	public void testParseNoFrontmatter() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    content = "Just plain text with no frontmatter.";
		    result = YamlFrontmatterParser::parse( content );

		    hasFrontmatter = result.frontmatter.isEmpty();
		    bodyText       = result.body;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasFrontmatter" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "bodyText" ) ).toString() ).isEqualTo( "Just plain text with no frontmatter." );
	}

	@Test
	@DisplayName( "parse() treats content as body when opening --- has no closing ---" )
	public void testParseUnclosedFrontmatter() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    content = "---\nname: orphaned\nNo closing delimiter";
		    result = YamlFrontmatterParser::parse( content );

		    hasFrontmatter = result.frontmatter.isEmpty();
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasFrontmatter" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "parse() normalizes CRLF line endings" )
	public void testParseCRLF() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    // Simulate Windows CRLF line endings
		    content = "---\r\nname: crlf-skill\r\n---\r\nBody after CRLF.";
		    result = YamlFrontmatterParser::parse( content );

		    fmName   = result.frontmatter.name;
		    bodyText = result.body;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "fmName" ) ).toString() ).isEqualTo( "crlf-skill" );
		assertThat( variables.get( Key.of( "bodyText" ) ).toString() ).isEqualTo( "Body after CRLF." );
	}

	@Test
	@DisplayName( "parse() handles empty frontmatter block" )
	public void testParseEmptyFrontmatterBlock() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    content = "---\n---\nBody only.";
		    result = YamlFrontmatterParser::parse( content );

		    hasFrontmatter = result.frontmatter.isEmpty();
		    bodyText       = result.body;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasFrontmatter" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "bodyText" ) ).toString() ).isEqualTo( "Body only." );
	}

	@Test
	@DisplayName( "parse() trims whitespace from body" )
	public void testParseBodyIsTrimmed() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    content = "---\nname: trim-test\n---\n\n\n  Body with leading blank lines.  ";
		    result = YamlFrontmatterParser::parse( content );

		    bodyText = result.body;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "bodyText" ) ).toString() ).isEqualTo( "Body with leading blank lines." );
	}

	@Test
	@DisplayName( "parse() supports hyphenated keys such as when-to-use" )
	public void testParseHyphenatedKey() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    content = "---\nwhen-to-use: Handles edge cases\n---\nBody.";
		    result = YamlFrontmatterParser::parse( content );

		    hasKey  = result.frontmatter.keyExists( "when-to-use" );
		    keyValue = result.frontmatter[ "when-to-use" ];
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasKey" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "keyValue" ) ).toString() ).isEqualTo( "Handles edge cases" );
	}

	@Test
	@DisplayName( "parse() handles multi-line body correctly" )
	public void testParseMultiLineBody() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    content = "---\nname: multi\n---\nLine one.\nLine two.\nLine three.";
		    result = YamlFrontmatterParser::parse( content );

		    bodyLineCount = result.body.listLen( char(10) );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "bodyLineCount" ) ) ).isEqualTo( 3 );
	}

	// -------------------------------------------------------------------------
	// stripQuotes() tests
	// -------------------------------------------------------------------------

	@Test
	@DisplayName( "stripQuotes() removes surrounding double quotes" )
	public void testStripDoubleQuotes() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    result = YamlFrontmatterParser::stripQuotes( '"hello world"' );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ).toString() ).isEqualTo( "hello world" );
	}

	@Test
	@DisplayName( "stripQuotes() removes surrounding single quotes" )
	public void testStripSingleQuotes() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    result = YamlFrontmatterParser::stripQuotes( "'hello world'" );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ).toString() ).isEqualTo( "hello world" );
	}

	@Test
	@DisplayName( "stripQuotes() leaves unquoted values untouched" )
	public void testStripNoQuotes() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    result = YamlFrontmatterParser::stripQuotes( "plain value" );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ).toString() ).isEqualTo( "plain value" );
	}

	@Test
	@DisplayName( "stripQuotes() does not strip mismatched quote characters" )
	public void testStripMismatchedQuotes() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    result = YamlFrontmatterParser::stripQuotes( '"mismatched''' );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ).toString() ).isEqualTo( "\"mismatched'" );
	}

	@Test
	@DisplayName( "stripQuotes() handles values with internal quotes" )
	public void testStripInternalQuotes() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    result = YamlFrontmatterParser::stripQuotes( '"say ""hello"" there"' );
		    """,
		    context
		);
		// @formatter:on

		// Outer quotes stripped; internal doubled-quotes remain as-is
		assertThat( variables.get( result ).toString() ).isEqualTo( "say \"\"hello\"\" there" );
	}

	@Test
	@DisplayName( "parse() strips quotes from frontmatter values automatically" )
	public void testParseFrontmatterValuesAreUnquoted() {
		// @formatter:off
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.util.YamlFrontmatterParser;

		    // Use double-quoted string: "" is BoxLang's escape for a literal double-quote inside a double-quoted string
		    content = "---\nname: ""quoted-name""\ndescription: 'single-quoted'\n---\nBody.";
		    result = YamlFrontmatterParser::parse( content );

		    fmName        = result.frontmatter.name;
		    fmDescription = result.frontmatter.description;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "fmName" ) ).toString() ).isEqualTo( "quoted-name" );
		assertThat( variables.get( Key.of( "fmDescription" ) ).toString() ).isEqualTo( "single-quoted" );
	}

}
