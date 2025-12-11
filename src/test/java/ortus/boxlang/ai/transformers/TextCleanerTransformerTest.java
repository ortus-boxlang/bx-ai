package ortus.boxlang.ai.transformers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

@DisplayName( "TextCleanerTransformer Tests" )
public class TextCleanerTransformerTest extends BaseIntegrationTest {

	@DisplayName( "TextCleanerTransformer can trim whitespace" )
	@Test
	public void testTrimWhitespace() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer();
				result = transformer.transform( "  Hello World  " );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "TextCleanerTransformer can normalize line breaks" )
	@Test
	public void testNormalizeLineBreaks() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer();
				// Test CRLF and CR conversion to LF
				text = "Line1" & char(13) & char(10) & "Line2" & char(13) & "Line3";
				result = transformer.transform( text );
		    """,
		    context
		);
		// @formatter:on

		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "Line1\nLine2\nLine3" );
	}

	@DisplayName( "TextCleanerTransformer can remove extra spaces" )
	@Test
	public void testRemoveExtraSpaces() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer();
				result = transformer.transform( "Hello    World   Test" );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "Hello World Test" );
	}

	@DisplayName( "TextCleanerTransformer can strip HTML tags" )
	@Test
	public void testStripHTML() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer( { stripHTML: true } );
				html = "<p>Hello <strong>World</strong></p><script>alert('test')</script>";
				result = transformer.transform( html );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "TextCleanerTransformer can decode HTML entities" )
	@Test
	public void testDecodeHTMLEntities() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer( { stripHTML: true } );
				html = "Hello&nbsp;&lt;World&gt;&amp;&quot;Test&quot;";
				result = transformer.transform( html );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "Hello <World>&\"Test\"" );
	}

	@DisplayName( "TextCleanerTransformer can strip markdown syntax" )
	@Test
	public void testStripMarkdown() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer( { stripMarkdown: true } );
				markdown = "## Header" & char(10) & "**bold** and *italic* text" & char(10) & "[link](http://example.com)";
				result = transformer.transform( markdown );
		    """,
		    context
		);
		// @formatter:on

		String result = variables.getAsString( Key.of( "result" ) );
		// Should remove markdown but keep the text content
		assertThat( result ).contains( "Header" );
		assertThat( result ).contains( "bold" );
		assertThat( result ).contains( "italic" );
		assertThat( result ).contains( "link" );
		assertThat( result ).doesNotContain( "**" );
		assertThat( result ).doesNotContain( "##" );
	}

	@DisplayName( "TextCleanerTransformer can remove control characters" )
	@Test
	public void testRemoveControlCharacters() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer();
				// Include control characters (keeping tab, newline)
				text = "Hello" & char(7) & " World" & char(9) & "Test";
				result = transformer.transform( text );
		    """,
		    context
		);
		// @formatter:on

		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "Hello World\tTest" );
	}

	@DisplayName( "TextCleanerTransformer can collapse whitespace" )
	@Test
	public void testCollapseWhitespace() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer( { collapseWhitespace: true } );
				text = "Hello" & char(10) & "  World" & char(9) & char(9) & "Test";
				result = transformer.transform( text );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "Hello World Test" );
	}

	@DisplayName( "TextCleanerTransformer can remove empty lines" )
	@Test
	public void testRemoveEmptyLines() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer( { removeEmptyLines: true } );
				text = "Line1" & char(10) & char(10) & "Line2" & char(10) & "   " & char(10) & "Line3";
				result = transformer.transform( text );
		    """,
		    context
		);
		// @formatter:on

		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "Line1\nLine2\nLine3" );
	}

	@DisplayName( "TextCleanerTransformer can limit consecutive newlines" )
	@Test
	public void testLimitConsecutiveNewlines() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer( { maxConsecutiveNewlines: 2 } );
				text = "Line1" & char(10) & char(10) & char(10) & char(10) & "Line2";
				result = transformer.transform( text );
		    """,
		    context
		);
		// @formatter:on

		String result = variables.getAsString( Key.of( "result" ) );
		// Should have max 2 newlines between Line1 and Line2
		assertThat( result ).isEqualTo( "Line1\n\nLine2" );
	}

	@DisplayName( "TextCleanerTransformer can normalize unicode characters" )
	@Test
	public void testNormalizeUnicode() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer( { normalizeUnicode: true } );
				// Unicode quotes and dashes
				text = char(8220) & "Hello" & char(8221) & " " & char(8211) & " - World";
				result = transformer.transform( text );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "\"Hello\" - - World" );
	}

	@DisplayName( "TextCleanerTransformer can transform arrays of text" )
	@Test
	public void testTransformArray() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer();
				texts = [ "  Hello  ", "  World  ", "  Test  " ];
				result = transformer.transform( texts );
		    """,
		    context
		);
		// @formatter:on

		Array result = ( Array ) variables.get( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 3 );
		assertThat( result.get( 0 ) ).isEqualTo( "Hello" );
		assertThat( result.get( 1 ) ).isEqualTo( "World" );
		assertThat( result.get( 2 ) ).isEqualTo( "Test" );
	}

	@DisplayName( "TextCleanerTransformer can be reconfigured" )
	@Test
	public void testConfigure() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer();
				// Initially should trim
				result1 = transformer.transform( "  Hello  " );
				
				// Reconfigure to not trim and not remove extra spaces
				transformer.configure( { trim: false, removeExtraSpaces: false } );
				result2 = transformer.transform( "  Hello  " );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "result1" ) ) ).isEqualTo( "Hello" );
		assertThat( variables.getAsString( Key.of( "result2" ) ) ).isEqualTo( "  Hello  " );
	}

	@DisplayName( "TextCleanerTransformer can handle complex real-world text" )
	@Test
	public void testComplexText() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.transformers.TextCleanerTransformer;
				
				transformer = new TextCleanerTransformer( {
					stripHTML: true,
					removeExtraSpaces: true,
					normalizeLineBreaks: true,
					maxConsecutiveNewlines: 1
				} );
				
				// Messy HTML with extra spaces and line breaks
				html = "<div>  Hello    World  </div>" & char(13) & char(10) & char(13) & char(10) & 
				       "<p>This  is   a   <strong>test</strong></p>";
				result = transformer.transform( html );
		    """,
		    context
		);
		// @formatter:on

		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "Hello World \nThis is a test" );
	}

}
