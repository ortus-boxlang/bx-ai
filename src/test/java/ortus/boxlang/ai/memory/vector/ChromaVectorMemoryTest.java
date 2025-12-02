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
@DisplayName( "ChromaVectorMemory Integration Tests" )
public class ChromaVectorMemoryTest extends BaseIntegrationTest {

	private static final String	CHROMA_HOST	= "localhost";
	private static final int	CHROMA_PORT	= 8000;

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@BeforeAll
	static void checkChromaAvailability() {
		// Skip tests if ChromaDB is not available
		boolean chromaAvailable = isChromaAvailable();
		assumeTrue( chromaAvailable, "ChromaDB not available at " + CHROMA_HOST + ":" + CHROMA_PORT );
	}

	@Test
	@Order( 1 )
	@DisplayName( "Test ChromaVectorMemory creation and configuration" )
	void testChromaVectorMemoryCreation() throws Exception {

		runtime.executeSource(
		    """
		    // Create ChromaVectorMemory instance
		    memory = aiMemory( memory: "chroma", key: createUUID(), config: {
		        host: "localhost",
		        port: 8000,
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

		assertEquals( "ChromaVectorMemory", testResult.getAsString( Key.of( "type" ) ) );
		assertEquals( "test_collection", testResult.getAsString( Key.of( "collection" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "configured" ) ) );
	}

	@Test
	@Order( 2 )
	@DisplayName( "Test adding and retrieving messages with ChromaVectorMemory" )
	void testAddAndRetrieveMessages() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory and add test messages
		    memory = aiMemory( memory: "chroma", key: createUUID(), config: {
		        host: "localhost",
		        port: 8000,
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

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsInteger( Key.of( "messageCount" ) ) > 0 );
		assertTrue( testResult.getAsBoolean( Key.of( "hasScores" ) ) );

		if ( testResult.getAsInteger( Key.of( "messageCount" ) ) > 0 ) {
			IStruct firstMessage = testResult.getAsStruct( Key.of( "firstMessage" ) );
			assertTrue( firstMessage.containsKey( Key.of( "text" ) ) );
			assertTrue( firstMessage.containsKey( Key.of( "score" ) ) );
		}

	}

	@Test
	@Order( 3 )
	@DisplayName( "Test batch seeding with ChromaVectorMemory" )
	void testBatchSeeding() throws Exception {

		runtime.executeSource(
		    """
		     // Create memory for seeding test
		     memory = aiMemory( memory: "chroma", key: createUUID(), config: {
		         host: "localhost",
		         port: 8000,
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
	@Order( 3 )
	@DisplayName( "Test async batch seeding with ChromaVectorMemory" )
	void testAsyncBatchSeeding() throws Exception {

		runtime.executeSource(
		    """
		        // Create memory for seeding test
		        memory = aiMemory( memory: "chroma", key: createUUID(), config: {
		            host: "localhost",
		            port: 8000,
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

		        // Seed the documents, this returns a BoxFuture
		        future = memory.seedAsync( documents )

		    // Await the future to get results
		    seedResults = future.get()

		        result = {
		            added: seedResults.added,
		            failed: seedResults.failed,
		            totalDocuments: documents.len()
		        }
		       """,
		    context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertEquals( 4, result.getAsInteger( Key.of( "totalDocuments" ) ) );
		assertTrue( result.getAsInteger( Key.of( "added" ) ) > 0 );
		assertEquals( 0, result.getAsInteger( Key.of( "failed" ) ) );
	}

	@Test
	@Order( 4 )
	@DisplayName( "Test HybridMemory with ChromaDB backend" )
	void testHybridMemory() throws Exception {

		runtime.executeSource(
		    """
		     // Create hybrid memory
		     hybridMemory = aiMemory( memory: "hybrid", key: createUUID(), config: {
		         recentLimit: 2,
		         semanticLimit: 2,
		         totalLimit: 4,
		         vectorProvider: "chroma",
		         vectorConfig: {
		             host: "localhost",
		             port: 8000,
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
	@Order( 5 )
	@DisplayName( "Test ChromaVectorMemory with userId and conversationId" )
	void testUserIdAndConversationId() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "chroma",
		        userId: "charlie",
		        conversationId: "chroma-test",
		        config: {
		            host: "localhost",
		            port: 8000,
		            collection: "test_user_conversation",
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    memory.add( { text: "ChromaDB vector storage" } );

		    result = {
		        userId: memory.getUserId(),
		        conversationId: memory.getConversationId()
		    };
		     """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( "charlie", testResult.getAsString( Key.of( "userId" ) ) );
		assertEquals( "chroma-test", testResult.getAsString( Key.of( "conversationId" ) ) );
	}

	@Test
	@Order( 6 )
	@DisplayName( "Test ChromaVectorMemory export includes userId and conversationId" )
	void testExportIncludesIdentifiers() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "chroma",
		        userId: "diana",
		        conversationId: "export-test",
		        config: {
		            host: "localhost",
		            port: 8000,
		            collection: "test_export_identifiers",
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
		assertEquals( "diana", testResult.getAsString( Key.of( "userId" ) ) );
		assertEquals( "export-test", testResult.getAsString( Key.of( "conversationId" ) ) );
	}

	@Test
	@Order( 7 )
	@DisplayName( "Test multi-tenant isolation with userId and conversationId filtering" )
	void testMultiTenantIsolation() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory for user alice, conversation chat1
		    memoryAliceChat1 = aiMemory(
		        memory: "chroma",
		        userId: "alice",
		        conversationId: "chat1",
		        config: {
		            host: "localhost",
		            port: 8000,
		            collection: "test_multi_tenant",
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Create memory for user alice, conversation chat2
		    memoryAliceChat2 = aiMemory(
		        memory: "chroma",
		        userId: "alice",
		        conversationId: "chat2",
		        config: {
		            host: "localhost",
		            port: 8000,
		            collection: "test_multi_tenant",
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Create memory for user bob, conversation chat1
		    memoryBobChat1 = aiMemory(
		        memory: "chroma",
		        userId: "bob",
		        conversationId: "chat1",
		        config: {
		            host: "localhost",
		            port: 8000,
		            collection: "test_multi_tenant",
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Add documents to each memory
		    memoryAliceChat1.add( { text: "Alice chat1: ChromaDB is powerful" } );
		    memoryAliceChat2.add( { text: "Alice chat2: Vector search works great" } );
		    memoryBobChat1.add( { text: "Bob chat1: Semantic search is amazing" } );

		    // Search in Alice's chat1 - should only return Alice's chat1 documents
		    resultsAliceChat1 = memoryAliceChat1.getRelevant( query: "ChromaDB", limit: 10 );

		    // Search in Alice's chat2 - should only return Alice's chat2 documents
		    resultsAliceChat2 = memoryAliceChat2.getRelevant( query: "Vector", limit: 10 );

		    // Search in Bob's chat1 - should only return Bob's chat1 documents
		    resultsBobChat1 = memoryBobChat1.getRelevant( query: "Semantic", limit: 10 );

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
	@Order( 8 )
	@DisplayName( "Test document count with ChromaVectorMemory" )
	void testDocumentCount() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "chroma", key: createUUID(), config: {
		        host: "localhost",
		        port: 8000,
		        collection: "test_count",
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    initialCount = memory.count();

		    memory.add( { id: "doc1", text: "First document" } );
		    memory.add( { id: "doc2", text: "Second document" } );
		    memory.add( { id: "doc3", text: "Third document" } );

		    afterAddCount = memory.count();

		    result = {
		        initialCount: initialCount,
		        afterAddCount: afterAddCount
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( 0, testResult.getAsInteger( Key.of( "initialCount" ) ) );
		assertEquals( 3, testResult.getAsInteger( Key.of( "afterAddCount" ) ) );
	}

	@Test
	@Order( 9 )
	@DisplayName( "Test clearing ChromaVectorMemory collection" )
	void testClearCollection() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "chroma", key: createUUID(), config: {
		        host: "localhost",
		        port: 8000,
		        collection: "test_clear",
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    memory.add( { id: "doc1", text: "Document to clear" } );
		    memory.add( { id: "doc2", text: "Another document" } );

		    beforeClearCount = memory.count();

		    memory.clear();

		    afterClearCount = memory.count();

		    result = {
		        beforeClearCount: beforeClearCount,
		        afterClearCount: afterClearCount
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( 2, testResult.getAsInteger( Key.of( "beforeClearCount" ) ) );
		assertEquals( 0, testResult.getAsInteger( Key.of( "afterClearCount" ) ) );
	}

	@Test
	@Order( 10 )
	@DisplayName( "Test export includes memory type" )
	void testExportType() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "chroma", key: createUUID(), config: {
		        host: "localhost",
		        port: 8000,
		        collection: "test_export_type",
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
		assertEquals( "ChromaVectorMemory", testResult.getAsString( Key.of( "type" ) ) );
	}

	/**
	 * Helper method to check if ChromaDB is available
	 */
	private static boolean isChromaAvailable() {
		try {
			java.net.Socket socket = new java.net.Socket();
			socket.connect( new java.net.InetSocketAddress( CHROMA_HOST, CHROMA_PORT ), 3000 );
			socket.close();
			return true;
		} catch ( Exception e ) {
			System.out.println( "ChromaDB not available: " + e.getMessage() );
			return false;
		}
	}
}