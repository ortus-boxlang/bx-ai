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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@TestMethodOrder( MethodOrderer.OrderAnnotation.class )
@DisplayName( "QdrantVectorMemory Integration Tests" )
public class QdrantVectorMemoryTest extends BaseIntegrationTest {

	private static final String	QDRANT_HOST	= "localhost";
	private static final int	QDRANT_PORT	= 6333;
	private static final String	QDRANT_URL	= "http://" + QDRANT_HOST + ":" + QDRANT_PORT;

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@BeforeAll
	static void checkQdrantAvailability() {
		// Verify Qdrant is accessible
		try {
			HttpClient				client		= HttpClient.newHttpClient();
			// Check health endpoint
			HttpRequest				request		= HttpRequest.newBuilder()
			    .uri( URI.create( QDRANT_URL + "/healthz" ) )
			    .GET()
			    .build();

			HttpResponse<String>	response	= client.send( request, HttpResponse.BodyHandlers.ofString() );
			assumeTrue( response.statusCode() == 200,
			    "Qdrant not accessible at " + QDRANT_URL + ". Start with: docker compose up qdrant" );

			// Clean up any existing test collections
			cleanupQdrantCollections( client );

		} catch ( Exception e ) {
			assumeTrue( false,
			    "Failed to connect to Qdrant at " + QDRANT_URL + ": " + e.getMessage() + ". Start with: docker compose up qdrant" );
		}
	}

	/**
	 * Delete all collections from Qdrant to ensure clean test state
	 */
	private static void cleanupQdrantCollections( HttpClient client ) {
		try {
			// Get all collections
			HttpRequest				collectionsRequest	= HttpRequest.newBuilder()
			    .uri( URI.create( QDRANT_URL + "/collections" ) )
			    .GET()
			    .build();

			HttpResponse<String>	collectionsResponse	= client.send( collectionsRequest, HttpResponse.BodyHandlers.ofString() );

			if ( collectionsResponse.statusCode() == 200 ) {
				String	body	= collectionsResponse.body();
				// Parse JSON to extract collection names (simple regex approach)
				var		pattern	= java.util.regex.Pattern.compile( "\"name\"\\s*:\\s*\"([^\"]+)\"" );
				var		matcher	= pattern.matcher( body );

				while ( matcher.find() ) {
					String		collectionName	= matcher.group( 1 );
					// Delete the collection
					HttpRequest	deleteRequest	= HttpRequest.newBuilder()
					    .uri( URI.create( QDRANT_URL + "/collections/" + collectionName ) )
					    .DELETE()
					    .build();

					client.send( deleteRequest, HttpResponse.BodyHandlers.ofString() );
					System.out.println( "Deleted Qdrant collection: " + collectionName );
				}
			}
		} catch ( Exception e ) {
			System.err.println( "Warning: Failed to cleanup Qdrant collections: " + e.getMessage() );
		}
	}

	@Test
	@Order( 1 )
	@DisplayName( "Test QdrantVectorMemory creation and configuration" )
	void testQdrantVectorMemoryCreation() throws Exception {

		runtime.executeSource(
		    """
		    // Create QdrantVectorMemory instance
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: "test_collection",
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small",
		        useCache: true
		    } );

		    result = {
		        type: memory.getName(),
		        collection: memory.getCollection(),
		        configured: true
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( "QdrantVectorMemory", testResult.getAsString( Key.of( "type" ) ) );
		assertEquals( "test_collection", testResult.getAsString( Key.of( "collection" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "configured" ) ) );
	}

	@Test
	@Order( 2 )
	@DisplayName( "Test storing and retrieving documents with QdrantVectorMemory" )
	void testStoreAndRetrieve() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory with unique collection
		    testCollection = "test_store_" & left( createUUID(), 8 );
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: testCollection,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Store documents with explicit IDs
		    doc1Id = createUUID();
		    doc2Id = createUUID();

		    memory.add( {
		        id: doc1Id,
		        text: "BoxLang is a modern JVM language",
		        metadata: { category: "programming" }
		    } );
		    memory.add( {
		        id: doc2Id,
		        text: "Vector databases store embeddings",
		        metadata: { category: "database" }
		    } );

		    // Small delay to allow indexing
		    sleep( 1000 );

		    // Retrieve by semantic search
		    results = memory.getRelevant( "programming languages", 2 );

		    result = {
		        resultCount: results.len(),
		        hasScores: results.len() > 0 && results[1].keyExists( "score" ),
		        hasText: results.len() > 0 && results[1].keyExists( "text" ),
		        hasMetadata: results.len() > 0 && results[1].keyExists( "metadata" )
		    };

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore cleanup errors
		    }
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsInteger( Key.of( "resultCount" ) ) >= 1 );
		assertTrue( testResult.getAsBoolean( Key.of( "hasScores" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "hasText" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "hasMetadata" ) ) );
	}

	@Test
	@Order( 3 )
	@DisplayName( "Test semantic search with QdrantVectorMemory" )
	void testSemanticSearch() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory
		    testCollection = "test_search_" & left( createUUID(), 8 );
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: testCollection,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add test documents
		    memory.add( "I love programming in Java" );
		    memory.add( "Python is great for data science" );
		    memory.add( "JavaScript powers the web" );
		    memory.add( "Rust is gaining popularity for systems programming" );

		    // Wait for indexing
		    sleep( 1000 );

		    // Search
		    results = memory.getRelevant( "programming languages", 3 );

		    result = {
		        messageCount: results.len(),
		        hasScores: results.len() > 0 && results[1].keyExists( "score" ),
		        firstMessage: results.len() > 0 ? results[1] : {}
		    };

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsInteger( Key.of( "messageCount" ) ) > 0 );
		assertTrue( testResult.getAsBoolean( Key.of( "hasScores" ) ) );

		IStruct firstMessage = testResult.getAsStruct( Key.of( "firstMessage" ) );
		assertTrue( firstMessage.containsKey( Key.of( "text" ) ) );
		assertTrue( firstMessage.containsKey( Key.of( "score" ) ) );
	}

	@Test
	@Order( 4 )
	@DisplayName( "Test document retrieval by ID" )
	void testGetById() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory
		    testCollection = "test_getbyid_" & left( createUUID(), 8 );
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: testCollection,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Store with explicit ID
		    testId = createUUID();
		    memory.add( {
		        id: testId,
		        text: "Test document content",
		        metadata: { type: "test", author: "tester" }
		    } );

		    // Wait for indexing
		    sleep( 1000 );

		    // Retrieve by ID
		    doc = memory.getById( testId );

		    result = {
		        found: !doc.isEmpty(),
		        hasText: doc.keyExists( "text" ),
		        hasMetadata: doc.keyExists( "metadata" ),
		        textMatches: doc.keyExists( "text" ) && doc.text == "Test document content",
		        id: doc.keyExists( "id" ) ? doc.id : ""
		    };

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsBoolean( Key.of( "found" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "hasText" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "hasMetadata" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "textMatches" ) ) );
	}

	@Test
	@Order( 5 )
	@DisplayName( "Test document deletion" )
	void testDeleteDocument() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory
		    testCollection = "test_delete_" & left( createUUID(), 8 );
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: testCollection,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Store document
		    testId = createUUID();
		    memory.add( {
		        id: testId,
		        text: "This will be deleted",
		        metadata: { status: "temporary" }
		    } );

		    // Wait for indexing
		    sleep( 1000 );

		    // Delete
		    deleted = memory.remove( testId );

		    // Wait for deletion
		    sleep( 500 );

		    // Try to retrieve
		    doc = memory.getById( testId );

		    result = {
		        deleted: deleted,
		        notFound: doc.isEmpty()
		    };

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsBoolean( Key.of( "deleted" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "notFound" ) ) );
	}

	@Test
	@Order( 6 )
	@DisplayName( "Test metadata filtering" )
	void testMetadataFiltering() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory
		    testCollection = "test_filter_" & left( createUUID(), 8 );
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: testCollection,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add documents with metadata
		    memory.add( { text: "Programming tutorials for beginners", metadata: { category: "tutorial", level: "beginner" } } );
		    memory.add( { text: "Advanced programming concepts", metadata: { category: "advanced", level: "expert" } } );
		    memory.add( { text: "Programming reference guide", metadata: { category: "reference", level: "all" } } );

		    // Wait for indexing (Qdrant may need more time for metadata indexing)
		    sleep( 1000 );

		    // Search with filter
		    results = memory.getRelevant(
		        "programming",
		        5,
		        { category: "tutorial" }
		    );

		    result = {
		        resultCount: results.len(),
		        hasResults: results.len() > 0,
		        firstResult: results.len() > 0 ? results[1] : {}
		    };

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		// Qdrant has excellent metadata filtering support
		assertTrue( testResult.getAsInteger( Key.of( "resultCount" ) ) >= 1, "Should have filtered results" );
		assertTrue( testResult.getAsBoolean( Key.of( "hasResults" ) ) );

		// Verify the result has the correct category
		IStruct firstResult = testResult.getAsStruct( Key.of( "firstResult" ) );
		if ( !firstResult.isEmpty() ) {
			IStruct metadata = firstResult.getAsStruct( Key.of( "metadata" ) );
			assertEquals( "tutorial", metadata.getAsString( Key.of( "category" ) ) );
		}
	}

	@Test
	@Order( 7 )
	@DisplayName( "Test batch seeding with QdrantVectorMemory" )
	void testBatchSeeding() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory
		    testCollection = "test_batch_" & left( createUUID(), 8 );
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: testCollection,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Prepare documents
		    documents = [
		        "AI is transforming technology",
		        "Machine learning improves with data",
		        { text: "Deep learning uses neural networks", metadata: { type: "concept" } },
		        "Natural language processing enables understanding",
		        { text: "Computer vision analyzes images", metadata: { type: "application" } }
		    ];

		    // Seed
		    seedResults = memory.seed( documents );

		    result = {
		        added: seedResults.added,
		        failed: seedResults.failed,
		        totalDocuments: documents.len()
		    };

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertEquals( 5, result.getAsInteger( Key.of( "totalDocuments" ) ) );
		assertTrue( result.getAsInteger( Key.of( "added" ) ) >= 4 ); // Allow for some failures
		assertTrue( result.getAsInteger( Key.of( "failed" ) ) <= 1 );
	}

	@Test
	@Order( 8 )
	@DisplayName( "Test async batch seeding with QdrantVectorMemory" )
	void testAsyncBatchSeeding() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory
		    testCollection = "test_async_batch_" & left( createUUID(), 8 );
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: testCollection,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Prepare documents
		    documents = [
		        "Artificial intelligence is transforming technology",
		        "Machine learning algorithms improve with data",
		        "Deep learning uses neural networks",
		        "Natural language processing understands text"
		    ];

		    // Seed asynchronously - returns BoxFuture
		    future = memory.seedAsync( documents );

		    // Await the future to get results
		    seedResults = future.get();

		    result = {
		        added: seedResults.added,
		        failed: seedResults.failed,
		        totalDocuments: documents.len()
		    };

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertEquals( 4, result.getAsInteger( Key.of( "totalDocuments" ) ) );
		assertTrue( result.getAsInteger( Key.of( "added" ) ) > 0 );
	}

	@Test
	@Order( 9 )
	@DisplayName( "Test HybridMemory integration with QdrantVectorMemory" )
	void testHybridMemory() throws Exception {

		runtime.executeSource(
		    """
		    // Create hybrid memory with Qdrant backend
		    testCollection = "test_hybrid_" & left( createUUID(), 8 );
		    hybridMemory = aiMemory( memory: "hybrid", key: createUUID(), config: {
		        recentLimit: 2,
		        semanticLimit: 2,
		        totalLimit: 4,
		        vectorProvider: "qdrant",
		        vectorConfig: {
		            host: "localhost",
		            port: 6333,
		            scheme: "http",
		            collection: testCollection,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    } );

		    // Add messages to hybrid memory
		    hybridMemory.add( "I want to learn cooking" );
		    hybridMemory.add( "What are some easy recipes?" );
		    hybridMemory.add( "How do I bake a chocolate cake?" );
		    hybridMemory.add( "I also enjoy Italian cuisine" );

		    // Wait for indexing
		    sleep( 1000 );

		    // Get relevant messages
		    relevantMessages = hybridMemory.getRelevant( "cooking and recipes", 4 );

		    result = {
		        created: true,
		        hasVectorMemory: !isNull( hybridMemory.getVectorMemory() ),
		        vectorType: hybridMemory.getVectorMemory().getName(),
		        messageCount: relevantMessages.len(),
		        hasRecentAndSemantic: relevantMessages.len() > 0
		    };

		    // Cleanup
		    try {
		        hybridMemory.getVectorMemory().clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertTrue( result.getAsBoolean( Key.of( "created" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasVectorMemory" ) ) );
		assertEquals( "QdrantVectorMemory", result.getAsString( Key.of( "vectorType" ) ) );
		assertTrue( result.getAsInteger( Key.of( "messageCount" ) ) > 0 );
		assertTrue( result.getAsBoolean( Key.of( "hasRecentAndSemantic" ) ) );
	}

	@Test
	@Order( 10 )
	@DisplayName( "Test collection management operations" )
	void testCollectionManagement() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory instance
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: "temp_test_collection",
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add a document to trigger collection creation
		    memory.add( { id: createUUID(), text: "Test document", metadata: {} } );

		    // Wait for indexing
		    sleep( 500 );

		    // Check if collection was created
		    exists = memory.collectionExists( "temp_test_collection" );

		    // Delete collection
		    memory.deleteCollection( "temp_test_collection" );

		    // Verify deletion
		    sleep( 500 );
		    existsAfterDelete = memory.collectionExists( "temp_test_collection" );

		    result = {
		        existedBefore: exists,
		        existsAfter: existsAfterDelete
		    };
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertTrue( result.getAsBoolean( Key.of( "existedBefore" ) ) );
		assertFalse( result.getAsBoolean( Key.of( "existsAfter" ) ) );
	}

	@Test
	@Order( 11 )
	@DisplayName( "Test getAllDocuments operation" )
	void testGetAllDocuments() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory
		    testCollection = "test_getall_" & left( createUUID(), 8 );
		    memory = aiMemory( memory: "qdrant", key: createUUID(), config: {
		        host: "localhost",
		        port: 6333,
		        scheme: "http",
		        collection: testCollection,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add documents
		    memory.add( "First document" );
		    memory.add( "Second document" );
		    memory.add( { text: "Third document", metadata: { order: 3 } } );

		    // Wait for indexing
		    sleep( 1000 );

		    // Get all documents
		    allDocs = memory.getAllDocuments();

		    result = {
		        count: allDocs.len(),
		        hasDocuments: allDocs.len() > 0,
		        hasText: allDocs.len() > 0 && allDocs[1].keyExists( "text" ),
		        hasMetadata: allDocs.len() > 0 && allDocs[1].keyExists( "metadata" )
		    };

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertTrue( result.getAsInteger( Key.of( "count" ) ) >= 3 );
		assertTrue( result.getAsBoolean( Key.of( "hasDocuments" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasText" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasMetadata" ) ) );
	}

	@DisplayName( "Test QdrantVectorMemory with userId and conversationId" )
	@Test
	@Order( 12 )
	public void testUserIdAndConversationId() throws Exception {
		// @formatter:off
		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "qdrant",
		        userId: "john",
		        conversationId: "qdrant-test",
		        config: {
		            host: "localhost",
		            port: 6333,
		            collection: "test_user_conv_%s",
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    memory.add( { text: "Qdrant vector database" } );

		    // Wait for indexing
		    sleep( 1000 );

		    result = {
		        userId: memory.getUserId(),
		        conversationId: memory.getConversationId()
		    };

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """
		    .formatted( System.currentTimeMillis() ),
		    context );
		// @formatter:on

		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertEquals( "john", result.getAsString( Key.of( "userId" ) ) );
		assertEquals( "qdrant-test", result.getAsString( Key.of( "conversationId" ) ) );
	}

	@DisplayName( "Test QdrantVectorMemory export includes userId and conversationId" )
	@Test
	@Order( 13 )
	public void testExportIncludesIdentifiers() throws Exception {
		// @formatter:off
		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "qdrant",
		        userId: "jane",
		        conversationId: "export-test",
		        config: {
		            host: "localhost",
		            port: 6333,
		            collection: "test_export_%s",
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    memory.add( { text: "Export test document" } );

		    // Wait for indexing
		    sleep( 1000 );

		    exported = memory.export();

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """
		    .formatted( System.currentTimeMillis() ),
		    context );
		// @formatter:on

		IStruct exported = variables.getAsStruct( Key.of( "exported" ) );
		assertTrue( exported.containsKey( Key.of( "userId" ) ) );
		assertTrue( exported.containsKey( Key.of( "conversationId" ) ) );
		assertEquals( "jane", exported.getAsString( Key.of( "userId" ) ) );
		assertEquals( "export-test", exported.getAsString( Key.of( "conversationId" ) ) );
	}

	@DisplayName( "Test multi-tenant isolation with userId and conversationId filtering" )
	@Test
	@Order( 14 )
	public void testMultiTenantIsolation() throws Exception {
		var uniqueCollection = "test_multi_tenant_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
		    """
		    uniqueCollection = "%s";

		    // Create memory for user alice, conversation chat1
		    memoryAliceChat1 = aiMemory(
		        memory: "qdrant",
		        userId: "alice",
		        conversationId: "chat1",
		        config: {
		            host: "localhost",
		            port: 6333,
		            collection: uniqueCollection,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Create memory for user alice, conversation chat2
		    memoryAliceChat2 = aiMemory(
		        memory: "qdrant",
		        userId: "alice",
		        conversationId: "chat2",
		        config: {
		            host: "localhost",
		            port: 6333,
		            collection: uniqueCollection,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Create memory for user bob, conversation chat1
		    memoryBobChat1 = aiMemory(
		        memory: "qdrant",
		        userId: "bob",
		        conversationId: "chat1",
		        config: {
		            host: "localhost",
		            port: 6333,
		            collection: uniqueCollection,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Add documents to each memory
		    memoryAliceChat1.add( { text: "Alice chat1: Qdrant is powerful" } );
		    memoryAliceChat2.add( { text: "Alice chat2: Vector search is fast" } );
		    memoryBobChat1.add( { text: "Bob chat1: Database indexing" } );

		    // Wait for indexing
		    sleep( 2000 );

		    // Search in Alice's chat1 - should only return Alice's chat1 documents
		    resultsAliceChat1 = memoryAliceChat1.getRelevant( query: "Qdrant", limit: 10 );

		    // Search in Alice's chat2 - should only return Alice's chat2 documents
		    resultsAliceChat2 = memoryAliceChat2.getRelevant( query: "Vector", limit: 10 );

		    // Search in Bob's chat1 - should only return Bob's chat1 documents
		    resultsBobChat1 = memoryBobChat1.getRelevant( query: "database", limit: 10 );

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

		    // Cleanup
		    try {
		        memoryAliceChat1.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """
		    .formatted( uniqueCollection ),
		    context );
		// @formatter:on

		IStruct	result				= variables.getAsStruct( Key.of( "result" ) );
		int		countAliceChat1		= result.getAsInteger( Key.of( "countAliceChat1" ) );
		int		countAliceChat2		= result.getAsInteger( Key.of( "countAliceChat2" ) );
		int		countBobChat1		= result.getAsInteger( Key.of( "countBobChat1" ) );
		int		allCountAliceChat1	= result.getAsInteger( Key.of( "allCountAliceChat1" ) );
		int		allCountAliceChat2	= result.getAsInteger( Key.of( "allCountAliceChat2" ) );
		int		allCountBobChat1	= result.getAsInteger( Key.of( "allCountBobChat1" ) );
		String	aliceChat1UserId	= result.getAsString( Key.of( "aliceChat1UserId" ) );
		String	aliceChat1ConvId	= result.getAsString( Key.of( "aliceChat1ConvId" ) );
		String	aliceChat2UserId	= result.getAsString( Key.of( "aliceChat2UserId" ) );
		String	aliceChat2ConvId	= result.getAsString( Key.of( "aliceChat2ConvId" ) );
		String	bobChat1UserId		= result.getAsString( Key.of( "bobChat1UserId" ) );
		String	bobChat1ConvId		= result.getAsString( Key.of( "bobChat1ConvId" ) );

		// Each memory should only see its own documents
		assertEquals( 1, countAliceChat1 );
		assertEquals( 1, countAliceChat2 );
		assertEquals( 1, countBobChat1 );

		// getAll should also only return isolated documents
		assertEquals( 1, allCountAliceChat1 );
		assertEquals( 1, allCountAliceChat2 );
		assertEquals( 1, allCountBobChat1 );

		// Verify metadata contains correct userId and conversationId
		assertEquals( "alice", aliceChat1UserId );
		assertEquals( "chat1", aliceChat1ConvId );
		assertEquals( "alice", aliceChat2UserId );
		assertEquals( "chat2", aliceChat2ConvId );
		assertEquals( "bob", bobChat1UserId );
		assertEquals( "chat1", bobChat1ConvId );
	}

}
