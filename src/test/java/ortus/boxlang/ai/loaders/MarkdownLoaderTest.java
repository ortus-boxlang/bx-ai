package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "MarkdownLoader Tests" )
public class MarkdownLoaderTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

	@DisplayName( "MarkdownLoader can load a markdown file" )
	@Test
	public void testMarkdownLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.MarkdownLoader;
				loader = new MarkdownLoader( source: "%s/sample.md" );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );

		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "Sample Markdown Document" );
	}

	@DisplayName( "MarkdownLoader can split by headers" )
	@Test
	public void testMarkdownLoaderSplitByHeaders() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.MarkdownLoader;
				loader = new MarkdownLoader( source: "%s/sample.md" );
				loader.headerSplit( 2 );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		// Should have multiple sections based on headers
		assertThat( docs.size() ).isGreaterThan( 1 );
	}

	@DisplayName( "MarkdownLoader can remove code blocks" )
	@Test
	public void testMarkdownLoaderRemoveCodeBlocks() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.MarkdownLoader;
				loader = new MarkdownLoader( source: "%s/sample.md" );
				loader.removeCodeBlocks();
				rawDocs = loader.load();
				result = rawDocs[1].getContent();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		String content = ( String ) variables.get( "result" );
		assertThat( content ).doesNotContain( "System.out.println" );
	}

}
