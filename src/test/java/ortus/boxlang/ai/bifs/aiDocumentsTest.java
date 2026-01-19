package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "Document Loader BIFs Tests" )
public class aiDocumentsTest extends BaseIntegrationTest {

	private static final String TEST_RESOURCES = Paths.get( "src/test/resources/loaders" ).toAbsolutePath().toString();

	// ===========================================
	// aiDocuments() Tests - Now returns loader for fluent API
	// ===========================================

	@DisplayName( "aiDocuments() returns a loader that can load a text file" )
	@Test
	public void testAiDocumentsText() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiDocuments( "%s/sample.txt" ).load();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isEqualTo( 1 );
	}

	@DisplayName( "aiDocuments() can load with explicit type via config" )
	@Test
	public void testAiDocumentsExplicitType() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiDocuments( "%s/sample.md", { type: "markdown" } ).load();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "aiDocuments() can load a directory" )
	@Test
	public void testAiDocumentsDirectory() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiDocuments( "%s" ).load();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs ).isNotNull();
		assertThat( docs.size() ).isGreaterThan( 1 );
	}

	// ===========================================
	// Fluent Configuration Tests
	// ===========================================

	@DisplayName( "aiDocuments() supports fluent chunkSize() method" )
	@Test
	public void testAiDocumentsFluentChunkSize() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocuments( "%s/sample.txt" ).chunkSize( 500 );
				result = loader.getConfig().chunkSize;
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Object chunkSize = variables.get( "result" );
		assertThat( ( ( Number ) chunkSize ).intValue() ).isEqualTo( 500 );
	}

	@DisplayName( "aiDocuments() supports fluent overlap() method" )
	@Test
	public void testAiDocumentsFluentOverlap() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocuments( "%s/sample.txt" ).overlap( 50 );
				result = loader.getConfig().overlap;
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Object overlap = variables.get( "result" );
		assertThat( ( ( Number ) overlap ).intValue() ).isEqualTo( 50 );
	}

	@DisplayName( "aiDocuments() supports fluent recursive() method for directories" )
	@Test
	public void testAiDocumentsFluentRecursive() {
		// @formatter:off
		runtime.executeSource(
		    """
				loader = aiDocuments( "%s" ).recursive();
				result = loader.getConfig().recursive;
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Boolean recursive = ( Boolean ) variables.get( "result" );
		assertThat( recursive ).isTrue();
	}

	// ===========================================
	// Filter/Map/Transform Tests
	// ===========================================

	@DisplayName( "aiDocuments() supports filter() method" )
	@Test
	public void testAiDocumentsFilter() {
		// @formatter:off
		runtime.executeSource(
		    """
				allDocs = aiDocuments( "%s" ).load();
				filteredDocs = aiDocuments( "%s" )
					.filter( ( doc ) => doc.hasContent() )
					.load();
				result = {
					allCount: allDocs.len(),
					filteredCount: filteredDocs.len()
				};
		    """.formatted( TEST_RESOURCES, TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		// All docs should have content, so counts should be equal
		assertThat( ( ( Number ) result.get( Key.of( "filteredCount" ) ) ).intValue() ).isGreaterThan( 0 );
	}

	@DisplayName( "aiDocuments() supports map() method" )
	@Test
	public void testAiDocumentsMap() {
		// @formatter:off
		runtime.executeSource(
		    """
				docs = aiDocuments( "%s/sample.txt" )
					.map( ( doc ) => {
						doc.setMeta( "processed", true );
						return doc;
					} )
					.load();
				result = docs[1].getMeta( "processed" );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Boolean processed = ( Boolean ) variables.get( "result" );
		assertThat( processed ).isTrue();
	}

	// ===========================================
	// toMemory() Tests
	// ===========================================

	@DisplayName( "aiDocuments().toMemory() ingests documents to memory" )
	@Test
	public void testAiDocumentsToMemory() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory( "WindowMemory" );
				result = aiDocuments( "%s/sample.txt" ).toMemory( memory );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.containsKey( Key.of( "documentsIn" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "chunksOut" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "stored" ) ) ).isTrue();
		assertThat( ( ( Number ) result.get( Key.of( "documentsIn" ) ) ).intValue() ).isEqualTo( 1 );
	}

	@DisplayName( "aiDocuments().toMemory() returns detailed report" )
	@Test
	public void testAiDocumentsToMemoryReport() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory( "WindowMemory" );
				result = aiDocuments( "%s/sample.txt" ).toMemory( memory );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.containsKey( Key.of( "documentsIn" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "chunksOut" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "stored" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "skipped" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "deduped" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "tokenCount" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "embeddingCalls" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "estimatedCost" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "errors" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "memorySummary" ) ) ).isTrue();
		assertThat( result.containsKey( Key.of( "duration" ) ) ).isTrue();
	}

	@DisplayName( "aiDocuments().toMemory() can ingest with chunking" )
	@Test
	public void testAiDocumentsToMemoryWithChunking() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory( "WindowMemory" );
				result = aiDocuments( "%s/sample.txt" )
					.toMemory( memory, { chunkSize: 50, overlap: 10 } );
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( ( ( Number ) result.get( Key.of( "documentsIn" ) ) ).intValue() ).isEqualTo( 1 );
		// Chunking should produce more chunks than input documents
		assertThat( ( ( Number ) result.get( Key.of( "chunksOut" ) ) ).intValue() ).isGreaterThan( 1 );
	}

	@DisplayName( "aiDocuments().toMemory() can fan-out to multiple memories" )
	@Test
	public void testAiDocumentsToMemoryMulti() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory1 = aiMemory( "WindowMemory" );
				memory2 = aiMemory( "WindowMemory" );
				result = aiDocuments( "%s/sample.txt" )
					.toMemory( [ memory1, memory2 ] );
				memory1Count = memory1.count();
				memory2Count = memory2.count();
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result ).isNotNull();

		// Both memories should have data
		Integer	count1	= ( Integer ) variables.get( "memory1Count" );
		Integer	count2	= ( Integer ) variables.get( "memory2Count" );
		assertThat( count1 ).isGreaterThan( 0 );
		assertThat( count2 ).isGreaterThan( 0 );

		// Memory summary should be an array for multi-memory
		Array summaries = ( Array ) result.get( Key.of( "memorySummary" ) );
		assertThat( summaries.size() ).isEqualTo( 2 );
	}

	// ===========================================
	// each() Tests
	// ===========================================

	@DisplayName( "aiDocuments().each() processes documents one by one" )
	@Test
	public void testAiDocumentsEach() {
		// @formatter:off
		runtime.executeSource(
		    """
				processedCount = 0;
				aiDocuments( "%s" )
					.filter( ( doc ) => doc.hasContent() )
					.each( ( doc ) => {
						processedCount++;
					} );
				result = processedCount;
		    """.formatted( TEST_RESOURCES ),
		    context
		);
		// @formatter:on

		Integer count = ( Integer ) variables.get( "result" );
		assertThat( count ).isGreaterThan( 0 );
	}

}
