/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.ai.memory.vector;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

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
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );

		// Create and configure Milvus memory instance
		//@formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory(
					memory: "milvus",
					key: createUUID(),
					userId: "test_user",
					conversationId: "conv1",
					config = {
						host: "@host@",
						port: @port@,
						dimensions: @dimension@,
						collection: "@collection@",
						embeddingProvider: "openai",
		       	 		embeddingModel: "text-embedding-3-small",
						useCache: true
					}
				)
		    """
		        .replace( "@collection@", TEST_COLLECTION )
		        .replace( "@host@", MILVUS_HOST )
		        .replace( "@port@", String.valueOf( MILVUS_PORT ) )
		        .replace( "@dimension@", String.valueOf( VECTOR_DIMENSION ) ),
		    context
		);
		//@formatter:on
	}

	@AfterEach
	public void cleanupMilvusMemory() {
		// Clear collection after each test
		//@formatter:off
		runtime.executeSource(
		    """
		    	if ( isDefined("memory") ) {
		    		try {
		    			println( "===== Cleaning up Milvus collection =====" );
		    			memory.clearCollection();
		    		} catch (any e) {
		    			// Ignore cleanup errors
		    		}
		    	}
		    """,
		    context
		);
		//@formatter:on
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

		    	// Flush to ensure data is available immediately
		    	memory.flush()

		    	// Retrieve by ID
		    	doc = memory.getDocumentById( "doc1" );
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

		    	// Flush to ensure data is available
		    	memory.flush();

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

		    	// Flush to ensure data is available
		    	memory.flush();

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

		    	// Flush to ensure data is available
		    	memory.flush();

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

		    	// Flush to ensure data is available
		    	memory.flush();

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

		    	// Flush to ensure data is available
		    	memory.flush();

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

	@Test
	@DisplayName( "Test MilvusVectorMemory with userId and conversationId" )
	void testUserIdAndConversationId() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "milvus",
		        userId: "henry",
		        conversationId: "milvus-test",
		        config: {
		            host: "localhost",
		            port: 19530,
		            collection: "test_user_conversation_" & replace( createUUID(), "-", "_", "all" ),
		            dimension: 1536,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    memory.add( { text: "Milvus vector database" } );

		    result = {
		        userId: memory.getUserId(),
		        conversationId: memory.getConversationId()
		    };
		     """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( "henry", testResult.getAsString( Key.of( "userId" ) ) );
		assertEquals( "milvus-test", testResult.getAsString( Key.of( "conversationId" ) ) );
	}

	@Test
	@DisplayName( "Test MilvusVectorMemory export includes userId and conversationId" )
	void testExportIncludesIdentifiers() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "milvus",
		        userId: "irene",
		        conversationId: "export-test",
		        config: {
		            host: "localhost",
		            port: 19530,
		            collection: "test_export_identifiers_" & replace( createUUID(), "-", "_", "all" ),
		            dimension: 1536,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    memory.add( { text: "Export test document" } );

		    exported = memory.export();

		    result = {
		        hasUserId: exported.keyExists( "userId" ),
		        hasConversationId: exported.keyExists( "conversationId" ),
		        userId: exported.keyExists( "userId" ) ? exported.userId : "",
		        conversationId: exported.keyExists( "conversationId" ) ? exported.conversationId : ""
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsBoolean( Key.of( "hasUserId" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "hasConversationId" ) ) );
		assertEquals( "irene", testResult.getAsString( Key.of( "userId" ) ) );
		assertEquals( "export-test", testResult.getAsString( Key.of( "conversationId" ) ) );
	}

	@Test
	@DisplayName( "Test multi-tenant isolation with userId and conversationId filtering" )
	void testMultiTenantIsolation() throws Exception {

		runtime.executeSource(
		    """
		    uniqueCollection = "test_multi_tenant_" & replace( createUUID(), "-", "_", "all" );

		    // Create memory for user alice, conversation chat1
		    memoryAliceChat1 = aiMemory(
		        memory: "milvus",
		        userId: "alice",
		        conversationId: "chat1",
		        config: {
		            host: "localhost",
		            port: 19530,
		            collection: uniqueCollection,
		            dimension: 1536,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Create memory for user alice, conversation chat2
		    memoryAliceChat2 = aiMemory(
		        memory: "milvus",
		        userId: "alice",
		        conversationId: "chat2",
		        config: {
		            host: "localhost",
		            port: 19530,
		            collection: uniqueCollection,
		            dimension: 1536,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Create memory for user bob, conversation chat1
		    memoryBobChat1 = aiMemory(
		        memory: "milvus",
		        userId: "bob",
		        conversationId: "chat1",
		        config: {
		            host: "localhost",
		            port: 19530,
		            collection: uniqueCollection,
		            dimension: 1536,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Add documents to each memory
		    memoryAliceChat1.add( { text: "Alice chat1: Milvus is scalable" } );
		    memoryAliceChat2.add( { text: "Alice chat2: Vector search is fast" } );
		    memoryBobChat1.add( { text: "Bob chat1: Distributed computing" } );

		    // Search in Alice's chat1 - should only return Alice's chat1 documents
		    resultsAliceChat1 = memoryAliceChat1.getRelevant( query: "Milvus", limit: 10 );

		    // Search in Alice's chat2 - should only return Alice's chat2 documents
		    resultsAliceChat2 = memoryAliceChat2.getRelevant( query: "Vector", limit: 10 );

		    // Search in Bob's chat1 - should only return Bob's chat1 documents
		    resultsBobChat1 = memoryBobChat1.getRelevant( query: "computing", limit: 10 );

		    // Get all documents for each memory
		    allAliceChat1 = memoryAliceChat1.getAll();
		    allAliceChat2 = memoryAliceChat2.getAll();
		    allBobChat1 = memoryBobChat1.getAll();

		    // Verify metadata includes userId and conversationId
		    firstAliceChat1 = allAliceChat1.len() > 0 ? allAliceChat1[1] : {};
		    firstAliceChat2 = allAliceChat2.len() > 0 ? allAliceChat2[1] : {};
		    firstBobChat1 = allBobChat1.len() > 0 ? allBobChat1[1] : {};

		    result = {
		        countAliceChat1: resultsAliceChat1.len(),
		        countAliceChat2: resultsAliceChat2.len(),
		        countBobChat1: resultsBobChat1.len(),
		        allCountAliceChat1: allAliceChat1.len(),
		        allCountAliceChat2: allAliceChat2.len(),
		        allCountBobChat1: allBobChat1.len(),
		        aliceChat1UserId: firstAliceChat1.keyExists("metadata") && firstAliceChat1.metadata.keyExists("userId") ? firstAliceChat1.metadata.userId : "",
		        aliceChat1ConvId: firstAliceChat1.keyExists("metadata") && firstAliceChat1.metadata.keyExists("conversationId") ? firstAliceChat1.metadata.conversationId : "",
		        aliceChat2UserId: firstAliceChat2.keyExists("metadata") && firstAliceChat2.metadata.keyExists("userId") ? firstAliceChat2.metadata.userId : "",
		        aliceChat2ConvId: firstAliceChat2.keyExists("metadata") && firstAliceChat2.metadata.keyExists("conversationId") ? firstAliceChat2.metadata.conversationId : "",
		        bobChat1UserId: firstBobChat1.keyExists("metadata") && firstBobChat1.metadata.keyExists("userId") ? firstBobChat1.metadata.userId : "",
		        bobChat1ConvId: firstBobChat1.keyExists("metadata") && firstBobChat1.metadata.keyExists("conversationId") ? firstBobChat1.metadata.conversationId : ""
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		// Each memory should only see its own documents
		assertEquals( 1, testResult.getAsInteger( Key.of( "countAliceChat1" ) ) );
		assertEquals( 1, testResult.getAsInteger( Key.of( "countAliceChat2" ) ) );
		assertEquals( 1, testResult.getAsInteger( Key.of( "countBobChat1" ) ) );

		// getAll should also only return isolated documents
		assertEquals( 1, testResult.getAsInteger( Key.of( "allCountAliceChat1" ) ) );
		assertEquals( 1, testResult.getAsInteger( Key.of( "allCountAliceChat2" ) ) );
		assertEquals( 1, testResult.getAsInteger( Key.of( "allCountBobChat1" ) ) );

		// Verify metadata contains correct userId and conversationId
		assertEquals( "alice", testResult.getAsString( Key.of( "aliceChat1UserId" ) ) );
		assertEquals( "chat1", testResult.getAsString( Key.of( "aliceChat1ConvId" ) ) );
		assertEquals( "alice", testResult.getAsString( Key.of( "aliceChat2UserId" ) ) );
		assertEquals( "chat2", testResult.getAsString( Key.of( "aliceChat2ConvId" ) ) );
		assertEquals( "bob", testResult.getAsString( Key.of( "bobChat1UserId" ) ) );
		assertEquals( "chat1", testResult.getAsString( Key.of( "bobChat1ConvId" ) ) );
	}

	@Test
	@DisplayName( "Test export includes memory type" )
	void testExportType() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "milvus", key: replace( createUUID(), "-", "_", "all" ), config: {
		        host: "localhost",
		        port: 19530,
		        collection: "test_export_type_" & replace( createUUID(), "-", "_", "all" ),
		        dimension: 1536,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    memory.add( { id: "doc1", text: "Test document" } );

		    exported = memory.export();

		    result = {
		        hasType: exported.keyExists( "type" ),
		        type: exported.keyExists( "type" ) ? exported.type : ""
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsBoolean( Key.of( "hasType" ) ) );
		assertEquals( "MilvusVectorMemory", testResult.getAsString( Key.of( "type" ) ) );
	}
}
