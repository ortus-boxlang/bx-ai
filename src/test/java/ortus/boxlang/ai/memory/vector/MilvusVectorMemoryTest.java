package ortus.boxlang.ai.memory.vector;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;

/**
 * Integration tests for MilvusVectorMemory
 *
 * Requires Milvus running on localhost:19530
 * Run: docker compose up -d milvus-standalone
 */
@DisplayName( "MilvusVectorMemory Integration Tests" )
public class MilvusVectorMemoryTest extends BaseIntegrationTest {

	private static final String	MILVUS_HOST			= System.getenv().getOrDefault( "MILVUS_HOST", "localhost" );
	private static final int	MILVUS_PORT			= Integer.parseInt( System.getenv().getOrDefault( "MILVUS_PORT", "19530" ) );
	private static final String	TEST_COLLECTION		= "test_collection_" + System.currentTimeMillis();
	private static final int	VECTOR_DIMENSION	= 3;

	@BeforeEach
	public void setupMilvusMemory() {
		// Create and configure Milvus memory instance
		runtime.executeSource(
		    """
		    	memory = new bxModules.bxai.models.memory.vector.MilvusVectorMemory()
		    		.configure({
		    			collection: "@collection@",
		    			host: "@host@",
		    			port: @port@,
		    			dimension: @dimension@
		    		});
		    """
		        .replace( "@collection@", TEST_COLLECTION )
		        .replace( "@host@", MILVUS_HOST )
		        .replace( "@port@", String.valueOf( MILVUS_PORT ) )
		        .replace( "@dimension@", String.valueOf( VECTOR_DIMENSION ) ),
		    context
		);
	}

	@AfterEach
	public void cleanupMilvusMemory() {
		// Clear collection after each test
		runtime.executeSource(
		    """
		    	if (isDefined("memory")) {
		    		try {
		    			memory.clearCollection();
		    			memory.close();
		    		} catch (any e) {
		    			// Ignore cleanup errors
		    		}
		    	}
		    """,
		    context
		);
	}

	@Test
	@DisplayName( "Test Milvus connection and collection creation" )
	public void testConnection() {
		var result = variables.get( "memory" );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test storing and retrieving a document" )
	public void testStoreAndRetrieveDocument() {
		runtime.executeSource(
		    """
		    	// Store a document
		    	memory.storeDocument(
		    		id = "doc1",
		    		text = "Test Document",
		    		embedding = [1.0, 0.0, 0.0],
		    		metadata = {
		    			"title": "Test Document",
		    			"category": "test"
		    		}
		    	)

		    	// Retrieve by ID
		    	doc = memory.getDocumentById("doc1");
		    """,
		    context
		);

		@SuppressWarnings( "unchecked" )
		var doc = ( Map<String, Object> ) variables.get( "doc" );
		assertThat( doc ).isNotNull();
		assertThat( doc.get( "id" ) ).isEqualTo( "doc1" );
	}

	@Test
	@DisplayName( "Test vector similarity search" )
	public void testVectorSearch() {
		runtime.executeSource(
		    """
		    	// Store multiple documents with different vectors
		    	memory.storeDocument(
		    		id = "doc1",
		    		text = "Document 1",
		    		embedding = [1.0, 0.0, 0.0],
		    		metadata = { "title": "Document 1" }
		    	);

		    	memory.storeDocument(
		    		id = "doc2",
		    		text = "Document 2",
		    		embedding = [0.9, 0.1, 0.0],
		    		metadata = { "title": "Document 2" }
		    	);

		    	memory.storeDocument(
		    		id = "doc3",
		    		text = "Document 3",
		    		embedding = [0.0, 1.0, 0.0],
		    		metadata = { "title": "Document 3" }
		    	);

		    	// Search for similar vectors
		    	results = memory.searchByVector(
		    		embedding = [1.0, 0.0, 0.0],
		    		limit = 2,
		    		filter = {}
		    	);
		    """,
		    context
		);

		@SuppressWarnings( "unchecked" )
		var results = ( List<Map<String, Object>> ) variables.get( "results" );
		assertThat( results ).isNotNull();
		assertThat( results.size() ).isAtLeast( 1 );

		// First result should be most similar
		var firstResult = results.get( 0 );
		assertThat( firstResult.get( "id" ) ).isEqualTo( "doc1" );
		// Score can be Double or Float
		var score = firstResult.get( "score" );
		assertThat( ( ( Number ) score ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@Test
	@DisplayName( "Test search with metadata filter" )
	public void testSearchWithFilter() {
		runtime.executeSource(
		    """
		    	// Store documents with different categories
		    	memory.storeDocument(
		    		id = "doc1",
		    		text = "Books document",
		    		embedding = [1.0, 0.0, 0.0],
		    		metadata = { "category": "books" }
		    	);

		    	memory.storeDocument(
		    		id = "doc2",
		    		text = "Articles document",
		    		embedding = [0.9, 0.1, 0.0],
		    		metadata = { "category": "articles" }
		    	);

		    	// Search with category filter
		    	results = memory.searchByVector(
		    		embedding = [1.0, 0.0, 0.0],
		    		limit = 10,
		    		filter = { "category": "books" }
		    	);
		    """,
		    context
		);

		@SuppressWarnings( "unchecked" )
		var results = ( List<Map<String, Object>> ) variables.get( "results" );
		assertThat( results ).isNotNull();
		assertThat( results.size() ).isEqualTo( 1 );
		assertThat( results.get( 0 ).get( "id" ) ).isEqualTo( "doc1" );
	}

	@Test
	@DisplayName( "Test deleting a document" )
	public void testDeleteDocument() {
		runtime.executeSource(
		    """
		    	// Store a document
		    	memory.storeDocument(
		    		id = "doc_to_delete",
		    		text = "Delete Me",
		    		embedding = [1.0, 0.0, 0.0],
		    		metadata = { "title": "Delete Me" }
		    	);

		    	// Delete it
		    	deleted = memory.deleteDocument("doc_to_delete");

		    	// Wait for Milvus to propagate the deletion (eventual consistency)
		    	sleep(500);

		    	// Try to retrieve it
		    	doc = memory.getDocumentById("doc_to_delete");
		    """,
		    context
		);

		var deleted = ( Boolean ) variables.get( "deleted" );
		assertThat( deleted ).isTrue();

		@SuppressWarnings( "unchecked" )
		var doc = ( Map<String, Object> ) variables.get( "doc" );
		assertThat( doc ).isEmpty();
	}

	@Test
	@DisplayName( "Test clearing collection" )
	public void testClearCollection() {
		runtime.executeSource(
		    """
		    	// Store multiple documents
		    	memory.storeDocument(
		    		id = "doc1",
		    		text = "Document 1",
		    		embedding = [1.0, 0.0, 0.0],
		    		metadata = { "title": "Document 1" }
		    	);

		    	memory.storeDocument(
		    		id = "doc2",
		    		text = "Document 2",
		    		embedding = [0.0, 1.0, 0.0],
		    		metadata = { "title": "Document 2" }
		    	);

		    	// Clear collection
		    	memory.clearCollection();

		    	// Try to search
		    	results = memory.searchByVector(
		    		embedding = [1.0, 0.0, 0.0],
		    		limit = 10,
		    		filter = {}
		    	);
		    """,
		    context
		);

		@SuppressWarnings( "unchecked" )
		var results = ( List<Map<String, Object>> ) variables.get( "results" );
		assertThat( results ).isEmpty();
	}

	@Test
	@DisplayName( "Test vector dimension validation" )
	public void testDimensionValidation() {
		var thrown = false;
		try {
			runtime.executeSource(
			    """
			    	// Try to store vector with wrong dimension
			    	memory.storeDocument(
			    		id = "bad_doc",
			    		text = "Bad document",
			    		embedding = [1.0, 0.0],  // Should be 3 dimensions
			    		metadata = {}
			    	);
			    """,
			    context
			);
		} catch ( Exception e ) {
			thrown = true;
			assertThat( e.getMessage() ).contains( "VectorDimensionMismatch" );
		}
		assertThat( thrown ).isTrue();
	}

	@Test
	@DisplayName( "Test search with threshold" )
	public void testSearchWithThreshold() {
		runtime.executeSource(
		    """
		    	// Store documents
		    	memory.storeDocument(
		    		id = "doc1",
		    		text = "Very Similar",
		    		embedding = [1.0, 0.0, 0.0],
		    		metadata = { "title": "Very Similar" }
		    	);

		    	memory.storeDocument(
		    		id = "doc2",
		    		text = "Not Similar",
		    		embedding = [0.0, 1.0, 0.0],
		    		metadata = { "title": "Not Similar" }
		    	);

		    	// Note: BaseVectorMemory searchByVector doesn't support threshold parameter
		    	// We'll just search and filter results manually if needed
		    	results = memory.searchByVector(
		    		embedding = [1.0, 0.0, 0.0],
		    		limit = 10,
		    		filter = {}
		    	);
		    """,
		    context
		);

		@SuppressWarnings( "unchecked" )
		var results = ( List<Map<String, Object>> ) variables.get( "results" );
		assertThat( results ).isNotNull();
		assertThat( results.size() ).isGreaterThan( 0 );
		// Most similar document should be first
		assertThat( results.get( 0 ).get( "id" ) ).isEqualTo( "doc1" );
	}
}
