package ortus.boxlang.ai.memory.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "JVectorMemory Integration Tests" )
public class JVectorMemoryTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		// No special setup needed for JVector in-memory
	}

	@Test
	@Order( 1 )
	@DisplayName( "Test JVectorMemory creation and basic operations" )
	void testBasicOperations() throws Exception {

		runtime.executeSource(
		    """
		    // Create JVectorMemory instance (high-performance HNSW index)
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure( {
		        M: 16,
		        efConstruction: 100,
		        efSearch: 50
		    } );

		    // Test adding vectors
		    vec1 = [1.0, 0.0, 0.0];
		    vec2 = [0.0, 1.0, 0.0];
		    vec3 = [0.0, 0.0, 1.0];

		    id1 = memory.add( vec1, { text: "First vector", category: "test" } );
		    id2 = memory.add( vec2, { text: "Second vector", category: "test" }, "custom-id-2" );
		    id3 = memory.add( vec3, { text: "Third vector", category: "other" } );

		    // Test count
		    count = memory.count();

		    // Test retrieval
		    retrieved = memory.get( id2 );

		    result = {
		        name: memory.getName(),
		        count: count,
		        id1: id1,
		        id2: id2,
		        id3: id3,
		        hasRetrieved: !retrieved.isEmpty(),
		        retrievedId: retrieved.keyExists( "id" ) ? retrieved.id : "",
		        retrievedText: retrieved.keyExists( "metadata" ) && retrieved.metadata.keyExists( "text" ) ? retrieved.metadata.text : ""
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( "JVectorMemory", testResult.getAsString( Key.of( "name" ) ) );
		assertEquals( 3, testResult.getAsInteger( Key.of( "count" ) ) );
		assertTrue( testResult.getAsString( Key.of( "id1" ) ).startsWith( "vec_" ) );
		assertEquals( "custom-id-2", testResult.getAsString( Key.of( "id2" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "hasRetrieved" ) ) );
		assertEquals( "custom-id-2", testResult.getAsString( Key.of( "retrievedId" ) ) );
		assertEquals( "Second vector", testResult.getAsString( Key.of( "retrievedText" ) ) );
	}

	@Test
	@Order( 2 )
	@DisplayName( "Test HNSW graph-based similarity search" )
	void testHNSWSearch() throws Exception {

		runtime.executeSource(
		    """
		    // Create and populate memory with more vectors for HNSW
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure( {
		        M: 16,
		        efConstruction: 100,
		        efSearch: 50
		    } );

		    // Add vectors in different regions of vector space
		    memory.add( [1.0, 0.0, 0.0], { text: "Pure X axis", type: "axis" } );
		    memory.add( [0.9, 0.1, 0.0], { text: "Mostly X", type: "axis" } );
		    memory.add( [0.8, 0.2, 0.0], { text: "Slightly less X", type: "axis" } );
		    memory.add( [0.0, 1.0, 0.0], { text: "Pure Y axis", type: "axis" } );
		    memory.add( [0.0, 0.0, 1.0], { text: "Pure Z axis", type: "axis" } );

		    // Search for vectors similar to [1.0, 0.0, 0.0] using HNSW index
		    queryVector = [1.0, 0.0, 0.0];
		    results = memory.search( queryVector, 3 );

		    result = {
		        resultCount: results.len(),
		        topScore: results.len() > 0 ? results[1].score : 0,
		        topText: results.len() > 0 ? results[1].metadata.text : "",
		        secondScore: results.len() > 1 ? results[2].score : 0,
		        allHaveScores: true,
		        allHaveMetadata: true,
		        allHaveVectors: true
		    };

		    // Verify all results have required fields
		    results.each( function( item ) {
		        if ( !item.keyExists( "score" ) ) result.allHaveScores = false;
		        if ( !item.keyExists( "metadata" ) ) result.allHaveMetadata = false;
		        if ( !item.keyExists( "vector" ) ) result.allHaveVectors = false;
		    } );
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( 3, testResult.getAsInteger( Key.of( "resultCount" ) ) );
		assertTrue( DoubleCaster.cast( testResult.get( Key.of( "topScore" ) ) ) > 0.9 ); // Should be ~1.0
		assertEquals( "Pure X axis", testResult.getAsString( Key.of( "topText" ) ) );
		assertTrue( DoubleCaster.cast( testResult.get( Key.of( "secondScore" ) ) ) > 0.8 ); // "Mostly X"
		assertTrue( testResult.getAsBoolean( Key.of( "allHaveScores" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "allHaveMetadata" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "allHaveVectors" ) ) );
	}

	@Test
	@Order( 3 )
	@DisplayName( "Test vector deletion and index rebuild" )
	void testDeletion() throws Exception {

		runtime.executeSource(
		    """
		    // Create and populate memory
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure();

		    id1 = memory.add( [1.0, 0.0], { text: "First" } );
		    id2 = memory.add( [0.0, 1.0], { text: "Second" } );
		    id3 = memory.add( [0.5, 0.5], { text: "Third" } );

		    initialCount = memory.count();

		    // Delete one vector (should trigger index rebuild)
		    deleted = memory.delete( id2 );

		    afterDeleteCount = memory.count();

		    // Try to retrieve deleted vector
		    retrieved = memory.get( id2 );

		    // Search should still work after deletion
		    searchResults = memory.search( [1.0, 0.0], 10 );

		    // Try to delete non-existent vector
		    deletedNonExistent = memory.delete( "non-existent-id" );

		    result = {
		        initialCount: initialCount,
		        afterDeleteCount: afterDeleteCount,
		        deleted: deleted,
		        retrievedAfterDelete: !retrieved.isEmpty(),
		        deletedNonExistent: deletedNonExistent,
		        searchStillWorks: searchResults.len() > 0
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( 3, testResult.getAsInteger( Key.of( "initialCount" ) ) );
		assertEquals( 2, testResult.getAsInteger( Key.of( "afterDeleteCount" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "deleted" ) ) );
		assertFalse( testResult.getAsBoolean( Key.of( "retrievedAfterDelete" ) ) );
		assertFalse( testResult.getAsBoolean( Key.of( "deletedNonExistent" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "searchStillWorks" ) ) );
	}

	@Test
	@Order( 4 )
	@DisplayName( "Test clear all vectors" )
	void testClear() throws Exception {

		runtime.executeSource(
		    """
		    // Create and populate memory
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure();

		    memory.add( [1.0, 0.0, 0.0], { text: "Vector 1" } );
		    memory.add( [0.0, 1.0, 0.0], { text: "Vector 2" } );
		    memory.add( [0.0, 0.0, 1.0], { text: "Vector 3" } );

		    beforeClearCount = memory.count();

		    // Clear all
		    memory.clear();

		    afterClearCount = memory.count();

		    result = {
		        beforeClearCount: beforeClearCount,
		        afterClearCount: afterClearCount
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( 3, testResult.getAsInteger( Key.of( "beforeClearCount" ) ) );
		assertEquals( 0, testResult.getAsInteger( Key.of( "afterClearCount" ) ) );
	}

	@Test
	@Order( 5 )
	@DisplayName( "Test dimension validation" )
	void testDimensionValidation() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure();

		    // Add vector with 3 dimensions
		    memory.add( [1.0, 0.0, 0.0], { text: "3D vector" } );

		    // Try to add vector with different dimensions
		    errorThrown = false;
		    errorMessage = "";
		    try {
		        memory.add( [1.0, 0.0], { text: "2D vector" } );
		    } catch( any e ) {
		        errorThrown = true;
		        errorMessage = e.message;
		    }

		    // Try to search with different dimensions
		    searchErrorThrown = false;
		    try {
		        memory.search( [1.0, 0.0] );
		    } catch( any e ) {
		        searchErrorThrown = true;
		    }

		    result = {
		        errorThrown: errorThrown,
		        searchErrorThrown: searchErrorThrown,
		        errorMessage: errorMessage
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsBoolean( Key.of( "errorThrown" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "searchErrorThrown" ) ) );
		assertTrue( testResult.getAsString( Key.of( "errorMessage" ) ).toLowerCase().contains( "dimension" ) );
	}

	@Test
	@Order( 6 )
	@DisplayName( "Test empty search results" )
	void testEmptySearch() throws Exception {

		runtime.executeSource(
		    """
		    // Create empty memory
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure();

		    // Search with no data
		    results = memory.search( [1.0, 0.0, 0.0] );

		    result = {
		        resultCount: results.len()
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( 0, testResult.getAsInteger( Key.of( "resultCount" ) ) );
	}

	@Test
	@Order( 7 )
	@DisplayName( "Test HNSW configuration parameters" )
	void testHNSWParameters() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory with custom HNSW parameters
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure( {
		        M: 32,               // More connections per node
		        efConstruction: 200, // Larger candidate list during construction
		        efSearch: 100        // Larger search parameter
		    } );

		    // Add test vectors
		    for ( i = 1; i <= 20; i++ ) {
		        memory.add(
		            [i * 0.05, 1.0 - i * 0.05, sin(i * 0.1)],
		            { text: "Vector #i#", index: i }
		        );
		    }

		    // Search with custom parameters
		    results = memory.search( [0.5, 0.5, 0.0], 5 );

		    result = {
		        configured: true,
		        vectorCount: memory.count(),
		        searchResultCount: results.len(),
		        hasResults: results.len() > 0
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsBoolean( Key.of( "configured" ) ) );
		assertEquals( 20, testResult.getAsInteger( Key.of( "vectorCount" ) ) );
		assertEquals( 5, testResult.getAsInteger( Key.of( "searchResultCount" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "hasResults" ) ) );
	}

	@Test
	@Order( 8 )
	@DisplayName( "Test threshold filtering in search" )
	void testThresholdFiltering() throws Exception {

		runtime.executeSource(
		    """
		    // Create and populate memory
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure();

		    // Add vectors with varying similarity to [1.0, 0.0]
		    memory.add( [1.0, 0.0], { text: "Perfect match" } );      // similarity = 1.0
		    memory.add( [0.9, 0.1], { text: "Close match" } );        // high similarity
		    memory.add( [0.5, 0.5], { text: "Medium match" } );       // medium similarity
		    memory.add( [0.0, 1.0], { text: "Perpendicular" } );      // similarity = 0.0

		    // Search with high threshold
		    highThresholdResults = memory.search( [1.0, 0.0], 10, 0.9 );

		    // Search with low threshold
		    lowThresholdResults = memory.search( [1.0, 0.0], 10, 0.0 );

		    result = {
		        highThresholdCount: highThresholdResults.len(),
		        lowThresholdCount: lowThresholdResults.len()
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		// High threshold should only match perfect and close matches
		assertTrue( testResult.getAsInteger( Key.of( "highThresholdCount" ) ) <= 2 );
		// Low threshold should match all vectors
		assertEquals( 4, testResult.getAsInteger( Key.of( "lowThresholdCount" ) ) );
	}

	@Test
	@Order( 9 )
	@DisplayName( "Test limit parameter in search" )
	void testSearchLimit() throws Exception {

		runtime.executeSource(
		    """
		    // Create and populate memory with many vectors
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure();

		    for ( i = 1; i <= 10; i++ ) {
		        memory.add( [i * 0.1, 1.0 - i * 0.1], { text: "Vector #i#", index: i } );
		    }

		    // Search with limit of 3
		    results = memory.search( [0.5, 0.5], 3 );

		    result = {
		        resultCount: results.len(),
		        limited: results.len() == 3
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( 3, testResult.getAsInteger( Key.of( "resultCount" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "limited" ) ) );
	}

	@Test
	@Order( 10 )
	@DisplayName( "Test aiMemory BIF integration" )
	void testAiMemoryIntegration() throws Exception {

		runtime.executeSource(
		    """
		    // Create JVectorMemory via aiMemory BIF
		    memory1 = aiMemory( "jvector", "test-collection" );
		    memory2 = aiMemory( "JVectorMemory", "another-collection" );

		    result = {
		        type1: memory1.getName(),
		        type2: memory2.getName()
		    }
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( "JVectorMemory", testResult.getAsString( Key.of( "type1" ) ) );
		assertEquals( "JVectorMemory", testResult.getAsString( Key.of( "type2" ) ) );
	}

	@Test
	@Order( 11 )
	@DisplayName( "Test performance with larger dataset" )
	void testLargeDataset() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory and add 100 vectors
		    memory = new bxModules.bxai.models.memory.vector.JVectorMemory();
		    memory.configure( {
		        M: 16,
		        efConstruction: 200,
		        efSearch: 50
		    } );

		    // Add 100 random-ish vectors
		    for ( i = 1; i <= 100; i++ ) {
		        memory.add(
		            [cos(i), sin(i), cos(i * 2), sin(i * 2)],
		            { text: "Document #i#", index: i }
		        );
		    }

		    // Perform search
		    queryStart = getTickCount();
		    results = memory.search( [0.5, 0.5, 0.5, 0.5], 10 );
		    queryTime = getTickCount() - queryStart;

		    result = {
		        vectorCount: memory.count(),
		        searchResultCount: results.len(),
		        queryTimeMs: queryTime,
		        fastEnough: queryTime < 1000  // Should be very fast with HNSW
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( 100, testResult.getAsInteger( Key.of( "vectorCount" ) ) );
		assertEquals( 10, testResult.getAsInteger( Key.of( "searchResultCount" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "fastEnough" ) ) );
		System.out.println( "JVector search time for 100 vectors: " + testResult.getAsInteger( Key.of( "queryTimeMs" ) ) + "ms" );
	}

}
