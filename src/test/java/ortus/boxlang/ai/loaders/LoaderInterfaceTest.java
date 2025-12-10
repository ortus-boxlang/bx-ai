package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "Loader Interface and Base Class Tests" )
public class LoaderInterfaceTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

	@DisplayName( "Loader supports getSourceMetadata()" )
	@Test
	public void testLoaderGetSourceMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				result = loader.getSourceMetadata();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct metadata = ( IStruct ) variables.get( "result" );
		assertThat( metadata ).isNotNull();
		assertThat( metadata.containsKey( Key.of( "source" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "exists" ) ) ).isTrue();
		assertThat( metadata.getAsBoolean( Key.of( "isFile" ) ) ).isTrue();
	}

	@DisplayName( "Loader supports getDocumentCount()" )
	@Test
	public void testLoaderGetDocumentCount() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				beforeLoad = loader.getDocumentCount();
				// Use loadBatch which caches documents
				batch = loader.loadBatch( 100 );
				afterLoad = loader.getDocumentCount();
				result = { before: beforeLoad, after: afterLoad };
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		// Before load, count is -1 (unknown)
		assertThat( result.getAsInteger( Key.of( "before" ) ) ).isEqualTo( -1 );
		// After load via loadBatch, count is actual number
		assertThat( result.getAsInteger( Key.of( "after" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "Loader supports getErrors()" )
	@Test
	public void testLoaderGetErrors() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				loader.load();
				result = loader.getErrors();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array errors = ( Array ) variables.get( "result" );
		assertThat( errors ).isNotNull();
		// No errors for valid file
		assertThat( errors.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Loader can store documents to memory" )
	@Test
	public void testLoaderToMemory() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				memory = aiMemory( "WindowMemory" );
				docs = loader.load();
				result = loader.toMemory( memory );
				messageCount = memory.count();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct report = ( IStruct ) variables.get( "result" );
		assertThat( report ).isNotNull();
		assertThat( report.containsKey( Key.of( "documentsIn" ) ) ).isTrue();

		Integer count = ( Integer ) variables.get( "messageCount" );
		assertThat( count ).isGreaterThan( 0 );
	}

	@DisplayName( "Loader can chunk documents before storing" )
	@Test
	public void testLoaderToMemoryWithChunking() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.TextLoader;
				loader = new TextLoader( source: "%s/sample.txt" );
				memory = aiMemory( "WindowMemory" );
				result = loader.toMemory(
					memory,
					{ chunkSize: 50, overlap: 10 }
				);
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct report = ( IStruct ) variables.get( "result" );
		assertThat( report ).isNotNull();
		assertThat( report.getAsInteger( Key.of( "chunksOut" ) ) ).isGreaterThan( 1 );
	}

}
