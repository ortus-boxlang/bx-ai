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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
@DisplayName( "TypesenseVectorMemory Integration Tests" )
public class TypesenseVectorMemoryTest extends BaseIntegrationTest {

	private static final String	TYPESENSE_HOST	= "localhost";
	private static final int	TYPESENSE_PORT	= 8108;

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@BeforeAll
	static void checkTypesenseAvailability() {
		// Skip tests if TypeSense is not available
		boolean typesenseAvailable = isTypesenseAvailable();
		assumeTrue( typesenseAvailable, "TypeSense not available at " + TYPESENSE_HOST + ":" + TYPESENSE_PORT );

		// Clean up all existing collections
		if ( typesenseAvailable ) {
			cleanupTypesenseCollections();
		}
	}

	/**
	 * Delete all collections from TypeSense to ensure clean test state
	 */
	private static void cleanupTypesenseCollections() {
		try {
			java.net.http.HttpClient			client				= java.net.http.HttpClient.newHttpClient();

			// Get all collections
			java.net.http.HttpRequest			collectionsRequest	= java.net.http.HttpRequest.newBuilder()
			    .uri( java.net.URI.create( "http://" + TYPESENSE_HOST + ":" + TYPESENSE_PORT + "/collections" ) )
			    .header( "X-TYPESENSE-API-KEY", "xyz" )
			    .GET()
			    .build();

			java.net.http.HttpResponse<String>	collectionsResponse	= client.send(
			    collectionsRequest,
			    java.net.http.HttpResponse.BodyHandlers.ofString()
			);

			if ( collectionsResponse.statusCode() == 200 ) {
				String body = collectionsResponse.body();
				// Parse JSON array of collections and delete each one
				if ( body.contains( "[" ) && body.contains( "name" ) ) {
					// Simple JSON parsing - extract collection names
					String[] parts = body.split( "\"name\"\\s*:\\s*\"" );
					for ( int i = 1; i < parts.length; i++ ) {
						String						collectionName	= parts[ i ].split( "\"" )[ 0 ];

						// Delete collection
						java.net.http.HttpRequest	deleteRequest	= java.net.http.HttpRequest.newBuilder()
						    .uri( java.net.URI.create( "http://" + TYPESENSE_HOST + ":" + TYPESENSE_PORT + "/collections/" + collectionName ) )
						    .header( "X-TYPESENSE-API-KEY", "xyz" )
						    .DELETE()
						    .build();

						client.send( deleteRequest, java.net.http.HttpResponse.BodyHandlers.ofString() );
					}
				}
			}
		} catch ( Exception e ) {
			System.err.println( "Warning: Failed to cleanup TypeSense collections: " + e.getMessage() );
		}
	}

	@Test
	@Order( 1 )
	@DisplayName( "Test TypesenseVectorMemory creation and configuration" )
	void testTypesenseVectorMemoryCreation() throws Exception {

		runtime.executeSource(
		    """
		    // Create TypesenseVectorMemory instance
		    memory = aiMemory( memory: "typesense", key: createUUID(), config: {
		        host: "localhost",
		        port: 8108,
		        protocol: "http",
		        apiKey: "xyz",
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

		assertEquals( "TypesenseVectorMemory", testResult.getAsString( Key.of( "type" ) ) );
		assertEquals( "test_collection", testResult.getAsString( Key.of( "collection" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "configured" ) ) );
	}

	@Test
	@Order( 2 )
	@DisplayName( "Test adding and retrieving messages with TypesenseVectorMemory" )
	void testAddAndRetrieveMessages() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory and add test messages
		    memory = aiMemory( memory: "typesense", key: createUUID(), config: {
		        host: "localhost",
		        port: 8108,
		        protocol: "http",
		        apiKey: "xyz",
		        collection: "test_messages",
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add some messages
		    memory.add( "I love programming in Java" );
		    memory.add( "Python is great for data science" );
		    memory.add( "JavaScript is essential for web development" );
		    memory.add( "Machine learning is fascinating" );

		    // Test semantic search
		    relevantMessages = memory.getRelevant( "programming languages", 2 );

		    result = {
		        messageCount: relevantMessages.len(),
		        hasScores: relevantMessages.len() > 0 && relevantMessages[1].keyExists( "score" ),
		        firstMessage: relevantMessages.len() > 0 ? relevantMessages[1] : {}
		    };
		    """,
		    context );

		IStruct	testResult		= variables.getAsStruct( result );
		int		messageCount	= testResult.getAsInteger( Key.of( "messageCount" ) );
		boolean	hasScores		= testResult.getAsBoolean( Key.of( "hasScores" ) );
		System.out.println( "Message Count: " + messageCount + ", Has Scores: " + hasScores );

		assertTrue( messageCount > 0, "Expected messages but got " + messageCount );
		assertTrue( hasScores, "Expected messages to have scores" );

		if ( testResult.getAsInteger( Key.of( "messageCount" ) ) > 0 ) {
			IStruct firstMessage = testResult.getAsStruct( Key.of( "firstMessage" ) );
			assertTrue( firstMessage.containsKey( Key.of( "text" ) ) );
			assertTrue( firstMessage.containsKey( Key.of( "score" ) ) );
		}

	}

	@Test
	@Order( 3 )
	@DisplayName( "Test batch seeding with TypesenseVectorMemory" )
	void testBatchSeeding() throws Exception {

		runtime.executeSource(
		    """
		     // Create memory for seeding test
		     memory = aiMemory( memory: "typesense", key: createUUID(), config: {
		         host: "localhost",
		         port: 8108,
		         protocol: "http",
		         apiKey: "xyz",
		         collection: "test_seeding",
		         embeddingProvider: "openai",
		         embeddingModel: "text-embedding-3-small"
		     } );

		     // Prepare documents for seeding
		     documents = [
		         "Artificial intelligence is transforming technology",
		         "Machine learning algorithms improve with data",
		         "Deep learning uses neural networks",
		         "Natural language processing understands text"
		     ];

		     // Seed the documents
		     seedResults = memory.seed( documents );

		     result = {
		         added: seedResults.added,
		         failed: seedResults.failed,
		         totalDocuments: documents.len()
		     };
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertEquals( 4, result.getAsInteger( Key.of( "totalDocuments" ) ) );
		assertTrue( result.getAsInteger( Key.of( "added" ) ) > 0 );
		assertEquals( 0, result.getAsInteger( Key.of( "failed" ) ) );
	}

	@Test
	@Order( 4 )
	@DisplayName( "Test typo-tolerant search with TypesenseVectorMemory" )
	void testTypoTolerantSearch() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory for typo-tolerant test
		    memory = aiMemory( memory: "typesense", key: createUUID(), config: {
		        host: "localhost",
		        port: 8108,
		        protocol: "http",
		        apiKey: "xyz",
		        collection: "test_typo",
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add messages about programming
		    memory.add( "How to reset your password" );
		    memory.add( "Payment processing is secure" );
		    memory.add( "Customer service is available 24/7" );

		    // Search with typos (TypeSense handles typos well)
		    relevantMessages = memory.getRelevant( "pasword reset", 2 );

		    result = {
		        messageCount: relevantMessages.len(),
		        foundPasswordMessage: relevantMessages.len() > 0
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsInteger( Key.of( "messageCount" ) ) > 0 );
		assertTrue( testResult.getAsBoolean( Key.of( "foundPasswordMessage" ) ) );
	}

	@Test
	@Order( 5 )
	@DisplayName( "Test HybridMemory with TypeSense backend" )
	void testHybridMemory() throws Exception {

		runtime.executeSource(
		    """
		     // Create hybrid memory
		     hybridMemory = aiMemory( memory: "hybrid", key: createUUID(), config: {
		         recentLimit: 2,
		         semanticLimit: 2,
		         totalLimit: 4,
		         vectorProvider: "typesense",
		         vectorConfig: {
		             host: "localhost",
		             port: 8108,
		             protocol: "http",
		             apiKey: "xyz",
		             collection: "test_hybrid",
		             embeddingProvider: "openai",
		             embeddingModel: "text-embedding-3-small"
		         }
		     } );

		     // Add messages to hybrid memory
		     hybridMemory.add( "I want to learn cooking" );
		     hybridMemory.add( "What are some easy recipes?" );
		     hybridMemory.add( "How do I bake a chocolate cake?" );
		     hybridMemory.add( "I also enjoy Italian cuisine" );

		     // Get relevant messages
		     relevantMessages = hybridMemory.getRelevant( "cooking and recipes", 4 );

		     result = {
		         type: hybridMemory.getName(),
		         messageCount: relevantMessages.len(),
		         hasRecentAndSemantic: relevantMessages.len() > 0
		     };
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertEquals( "HybridMemory", result.getAsString( Key.of( "type" ) ) );
		assertTrue( result.getAsInteger( Key.of( "messageCount" ) ) > 0 );
		assertTrue( result.getAsBoolean( Key.of( "hasRecentAndSemantic" ) ) );
	}

	@Test
	@Order( 6 )
	@DisplayName( "Test document retrieval by ID" )
	public void testGetDocumentById() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "typesense", key: createUUID(), config: {
		        collection: "test_get_by_id",
		        host: "localhost",
		        port: 8108,
		        apiKey: "xyz"
		    } );

		    // Add a test document with explicit ID
		    testId = createUUID();
		    memory.add( {
		        id: testId,
		        text: "Test message for retrieval",
		        metadata: { type: "test" }
		    } );

		    // Retrieve by ID
		    doc = memory.getById( testId );

		    result = {
		        hasDoc: !doc.isEmpty(),
		        textMatches: doc.keyExists( "text" ) && doc.text == "Test message for retrieval",
		        hasMetadata: doc.keyExists( "metadata" ),
		        hasEmbedding: doc.keyExists( "embedding" )
		    };
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertTrue( result.getAsBoolean( Key.of( "hasDoc" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "textMatches" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasMetadata" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasEmbedding" ) ) );
	}

	@Test
	@Order( 7 )
	@DisplayName( "Test document deletion" )
	public void testDeleteDocument() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "typesense", key: createUUID(), config: {
		        collection: "test_deletion",
		        host: "localhost",
		        port: 8108,
		        apiKey: "xyz"
		    } );

		    // Add test documents with explicit IDs
		    id1 = createUUID();
		    id2 = createUUID();
		    memory.add( { id: id1, text: "Document to delete", metadata: {} } );
		    memory.add( { id: id2, text: "Document to keep", metadata: {} } );

		    // Get all documents
		    allDocs = memory.getAll();

		    // Delete one document
		    memory.remove( id1 );

		    // Verify deletion
		    remainingDocs = memory.getAll();

		    // Try to get deleted document
		    deletedDoc = memory.getById( id1 );

		    result = {
		        isDeleted: deletedDoc.isEmpty()
		    };
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertTrue( result.getAsBoolean( Key.of( "isDeleted" ) ) );
	}

	@Test
	@Order( 8 )
	@DisplayName( "Test metadata filtering" )
	public void testMetadataFiltering() {
		// @formatter:off
		runtime.executeSource(
		    """
				memory = aiMemory( memory: "typesense", key: createUUID(), config: {
					collection: "test_filtering",
					host: "localhost",
					port: 8108,
					apiKey: "xyz"
				} )

				// Add documents with different metadata
				memory.add( { text: "Message from user Alice", metadata: { user: "alice", priority: "high" } } )
				memory.add( { text: "Message from user Bob", metadata: { user: "bob", priority: "low" } } )
				memory.add( { text: "Another message from Alice", metadata: { user: "alice", priority: "low" } } )		    // Search with metadata filter

				aliceMessages = memory.getRelevant(
					"message",
					3,
					{ user: "alice" }
				);

				result = {
					count: arrayLen( aliceMessages ),
					allFromAlice: true
				}

				// Verify all results are from Alice
				for( msg in aliceMessages ) {
					if( !structKeyExists(msg, "metadata") || msg.metadata.user != "alice" ) {
						result.allFromAlice = false
						break
					}
				}
		       """,
		    context );
		// @formatter:on

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertEquals( 2, result.getAsInteger( Key.of( "count" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "allFromAlice" ) ) );
	}

	@Test
	@Order( 9 )
	@DisplayName( "Test collection management operations" )
	public void testCollectionManagement() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "typesense", key: createUUID(), config: {
		        collection: "test_mgmt_ops",
		        host: "localhost",
		        port: 8108,
		        apiKey: "xyz"
		    } );

		    // Collection should exist after configure
		    existsAfterCreate = memory.collectionExists( "test_mgmt_ops" );

		    // Add some data
		    memory.add( "Test data" );

		    // Delete the collection
		    memory.deleteCollection( "test_mgmt_ops" );

		    // Check it no longer exists
		    existsAfterDelete = memory.collectionExists( "test_mgmt_ops" );

		    // Recreate it
		    memory.createCollection( "test_mgmt_ops" );
		    existsAfterRecreate = memory.collectionExists( "test_mgmt_ops" );

		    result = {
		        existsAfterCreate: existsAfterCreate,
		        existsAfterDelete: existsAfterDelete,
		        existsAfterRecreate: existsAfterRecreate
		    };
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertTrue( result.getAsBoolean( Key.of( "existsAfterCreate" ) ) );
		assertTrue( !result.getAsBoolean( Key.of( "existsAfterDelete" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "existsAfterRecreate" ) ) );
	}

	@Test
	@Order( 10 )
	@DisplayName( "Test getAllDocuments operation" )
	public void testGetAllDocuments() {
		runtime.executeSource(
		    """
		       memory = aiMemory( memory: "typesense", key: createUUID(), config: {
		           collection: "test_get_all",
		           host: "localhost",
		           port: 8108,
		           apiKey: "xyz"
		       } )

		    memory.clear()

		       // Add multiple documents
		       messages = [
		           "First document",
		           "Second document",
		           "Third document"
		       ]

		       for( msg in messages ) {
		           memory.add( msg )
		       }

		       // Get all documents
		       allDocs = memory.getAll()

		       result = {
		           count: arrayLen(allDocs),
		           hasText: allDocs.len() > 0 && structKeyExists(allDocs[1], "text"),
		           hasMetadata: allDocs.len() > 0 && structKeyExists(allDocs[1], "metadata"),
		           hasEmbedding: allDocs.len() > 0 && structKeyExists(allDocs[1], "embedding"),
		           hasId: allDocs.len() > 0 && structKeyExists(allDocs[1], "id")
		       }
		       """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertEquals( 3, result.getAsInteger( Key.of( "count" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasText" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasMetadata" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasEmbedding" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasId" ) ) );
	}

	@Test
	@Order( 11 )
	@DisplayName( "Test userId and conversationId getters" )
	public void testUserIdAndConversationId() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "typesense", key: createUUID(), userId: "user123", conversationId: "conv456", config: {
		        collection: "test_tenant_getters",
		        host: "localhost",
		        port: 8108,
		        apiKey: "xyz"
		    } );

		    result = {
		        userId: memory.getUserId(),
		        conversationId: memory.getConversationId()
		    };
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertEquals( "user123", result.getAsString( Key.of( "userId" ) ) );
		assertEquals( "conv456", result.getAsString( Key.of( "conversationId" ) ) );
	}

	@Test
	@Order( 12 )
	@DisplayName( "Test export includes userId and conversationId" )
	public void testExportIncludesIdentifiers() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "typesense", key: createUUID(), userId: "testUser", conversationId: "testConv", config: {
		        collection: "test_export_identifiers",
		        host: "localhost",
		        port: 8108,
		        apiKey: "xyz"
		    } );

		    memory.add( "Test message" );
		    exported = memory.export();

		    result = {
		        hasUserId: exported.keyExists( "userId" ),
		        hasConversationId: exported.keyExists( "conversationId" ),
		        userIdMatches: exported.userId == "testUser",
		        conversationIdMatches: exported.conversationId == "testConv"
		    };
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertTrue( result.getAsBoolean( Key.of( "hasUserId" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "hasConversationId" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "userIdMatches" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "conversationIdMatches" ) ) );
	}

	@Test
	@Order( 13 )
	@DisplayName( "Test multi-tenant isolation with userId and conversationId" )
	public void testMultiTenantIsolation() {
		runtime.executeSource(
		    """
		    // Create unique collection name with timestamp to avoid conflicts
		    collectionName = "test_isolation_" & now().getTime();

		    // Create memories for different users and conversations
		    aliceChat1 = aiMemory( memory: "typesense", key: createUUID(), userId: "alice", conversationId: "chat1", config: {
		        collection: collectionName,
		        host: "localhost",
		        port: 8108,
		        apiKey: "xyz"
		    } );

		    aliceChat2 = aiMemory( memory: "typesense", key: createUUID(), userId: "alice", conversationId: "chat2", config: {
		        collection: collectionName,
		        host: "localhost",
		        port: 8108,
		        apiKey: "xyz"
		    } );

		    bobChat1 = aiMemory( memory: "typesense", key: createUUID(), userId: "bob", conversationId: "chat1", config: {
		        collection: collectionName,
		        host: "localhost",
		        port: 8108,
		        apiKey: "xyz"
		    } );

		    // Add distinct messages to each memory
		    aliceChat1.add( "Alice chat1 message about TypeSense vector database" );
		    aliceChat2.add( "Alice chat2 message about document storage" );
		    bobChat1.add( "Bob chat1 message about search functionality" );

		    // Verify isolation - each memory should only see its own messages
		    aliceChat1Results = aliceChat1.getRelevant( "message", 10 );
		    aliceChat2Results = aliceChat2.getRelevant( "message", 10 );
		    bobChat1Results = bobChat1.getRelevant( "message", 10 );

		    // Also test getAll() for isolation
		    aliceChat1All = aliceChat1.getAll();
		    aliceChat2All = aliceChat2.getAll();
		    bobChat1All = bobChat1.getAll();

		    result = {
		        aliceChat1Count: aliceChat1Results.len(),
		        aliceChat2Count: aliceChat2Results.len(),
		        bobChat1Count: bobChat1Results.len(),
		        aliceChat1AllCount: aliceChat1All.len(),
		        aliceChat2AllCount: aliceChat2All.len(),
		        bobChat1AllCount: bobChat1All.len(),
		        aliceChat1HasCorrectText: aliceChat1Results.len() > 0 && aliceChat1Results[1].text.findNoCase("Alice chat1") > 0,
		        aliceChat2HasCorrectText: aliceChat2Results.len() > 0 && aliceChat2Results[1].text.findNoCase("Alice chat2") > 0,
		        bobChat1HasCorrectText: bobChat1Results.len() > 0 && bobChat1Results[1].text.findNoCase("Bob chat1") > 0
		    };

		    // Cleanup
		    aliceChat1.deleteCollection( collectionName );
		    """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		// Each memory should only have 1 document
		assertEquals( 1, result.getAsInteger( Key.of( "aliceChat1Count" ) ) );
		assertEquals( 1, result.getAsInteger( Key.of( "aliceChat2Count" ) ) );
		assertEquals( 1, result.getAsInteger( Key.of( "bobChat1Count" ) ) );

		// getAll() should also respect isolation
		assertEquals( 1, result.getAsInteger( Key.of( "aliceChat1AllCount" ) ) );
		assertEquals( 1, result.getAsInteger( Key.of( "aliceChat2AllCount" ) ) );
		assertEquals( 1, result.getAsInteger( Key.of( "bobChat1AllCount" ) ) );

		// Verify correct text in results
		assertTrue( result.getAsBoolean( Key.of( "aliceChat1HasCorrectText" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "aliceChat2HasCorrectText" ) ) );
		assertTrue( result.getAsBoolean( Key.of( "bobChat1HasCorrectText" ) ) );
	}

	/**
	 * Helper method to check if TypeSense is available
	 */
	private static boolean isTypesenseAvailable() {
		try {
			java.net.Socket socket = new java.net.Socket();
			socket.connect( new java.net.InetSocketAddress( TYPESENSE_HOST, TYPESENSE_PORT ), 3000 );
			socket.close();
			return true;
		} catch ( Exception e ) {
			System.out.println( "TypeSense not available: " + e.getMessage() );
			return false;
		}
	}
}
