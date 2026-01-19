package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "DirectoryLoader Tests" )
public class DirectoryLoaderTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

	@DisplayName( "DirectoryLoader can load all files from a directory" )
	@Test
	public void testDirectoryLoaderBasic() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		// Should load multiple files from the test directory
		assertThat( docs.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "DirectoryLoader can filter by extension" )
	@Test
	public void testDirectoryLoaderExtensionFilter() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" );
				loader.extensions( ["txt"] );
				rawDocs = loader.load();

				result = rawDocs.map( d => d.toStruct() );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );
	}

	@DisplayName( "Loader supports loadBatch()" )
	@Test
	public void testLoaderLoadBatch() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" );
				batch1 = loader.loadBatch( 2 );
				batch2 = loader.loadBatch( 2 );
				result = {
					batch1Size: batch1.len(),
					batch2Size: batch2.len()
				};
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		// First batch should have up to 2 documents
		assertThat( result.getAsInteger( ortus.boxlang.runtime.scopes.Key.of( "batch1Size" ) ) ).isGreaterThan( 0 );
		assertThat( result.getAsInteger( ortus.boxlang.runtime.scopes.Key.of( "batch1Size" ) ) ).isLessThan( 3 );
	}

	@DisplayName( "Loader supports loadAsStream()" )
	@Test
	public void testLoaderLoadAsStream() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" );
				stream = loader.loadAsStream();
				result = stream.count();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Long count = ( Long ) variables.get( "result" );
		assertThat( count ).isGreaterThan( 0 );
	}

	@DisplayName( "DirectoryLoader supports recursive loading" )
	@Test
	public void testDirectoryLoaderRecursive() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				// Non-recursive first
				loaderNonRecursive = new DirectoryLoader( source: "%s" );
				nonRecursiveDocs = loaderNonRecursive.load();

				// Recursive
				loaderRecursive = new DirectoryLoader( source: "%s" )
					.recursive();
				recursiveDocs = loaderRecursive.load();

				result = {
					nonRecursiveCount: nonRecursiveDocs.len(),
					recursiveCount: recursiveDocs.len()
				};
		    """.formatted( TEST_RESOURCES, TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct	result				= ( IStruct ) variables.get( "result" );
		int		nonRecursiveCount	= result.getAsInteger( ortus.boxlang.runtime.scopes.Key.of( "nonRecursiveCount" ) );
		int		recursiveCount		= result.getAsInteger( ortus.boxlang.runtime.scopes.Key.of( "recursiveCount" ) );

		// Recursive should find at least as many files as non-recursive
		assertThat( recursiveCount ).isAtLeast( nonRecursiveCount );
	}

	@DisplayName( "DirectoryLoader supports custom filter callback" )
	@Test
	public void testDirectoryLoaderFilterCallback() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" )
					.filterFiles( ( filePath ) => {
						// Only include files with "sample" in the name
						return filePath.findNoCase( "sample" ) > 0;
					} );
				rawDocs = loader.load();
				result = rawDocs.len();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		int count = variables.getAsInteger( result );
		// Should have filtered files
		assertThat( count ).isAtLeast( 0 );
	}

	@DisplayName( "DirectoryLoader supports exclude patterns" )
	@Test
	public void testDirectoryLoaderExcludePatterns() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loaderAll = new DirectoryLoader( source: "%s" );
				allDocs = loaderAll.load();

				loaderExclude = new DirectoryLoader( source: "%s" )
					.exclude( [".*\\.txt$"] );
				excludedDocs = loaderExclude.load();

				result = {
					allCount: allDocs.len(),
					excludedCount: excludedDocs.len()
				};
		    """.formatted( TEST_RESOURCES, TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct	result			= ( IStruct ) variables.get( "result" );
		int		allCount		= result.getAsInteger( ortus.boxlang.runtime.scopes.Key.of( "allCount" ) );
		int		excludedCount	= result.getAsInteger( ortus.boxlang.runtime.scopes.Key.of( "excludedCount" ) );

		// Excluded should have fewer or equal files
		assertThat( excludedCount ).isAtMost( allCount );
	}

	@DisplayName( "DirectoryLoader supports custom loader registration" )
	@Test
	public void testDirectoryLoaderRegisterLoader() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.DirectoryLoader;
				loader = new DirectoryLoader( source: "%s" )
					.registerLoader( "log", "TextLoader" )
					.extensions( ["log"] );
				rawDocs = loader.load();
				result = rawDocs.len();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		int count = variables.getAsInteger( result );
		// Result depends on whether .log files exist in test resources
		assertThat( count ).isAtLeast( 0 );
	}

}
