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

	private static final String	TYPESENSE_HOST		= "localhost";
	private static final int	TYPESENSE_PORT		= 8108;
	private static final String	TYPESENSE_API_KEY	= "xyz";	// Default API key for local TypeSense

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
	}

	@Test
	@Order( 1 )
	@DisplayName( "Test TypesenseVectorMemory creation and configuration" )
	void testTypesenseVectorMemoryCreation() throws Exception {

		runtime.executeSource(
		    """
		    // Create TypesenseVectorMemory instance
		    memory = aiMemory( "typesense", createUUID(), {
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
		    memory = aiMemory( "typesense", createUUID(), {
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
		     memory = aiMemory( "typesense", createUUID(), {
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
		    memory = aiMemory( "typesense", createUUID(), {
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
		     hybridMemory = aiMemory( "hybrid", createUUID(), {
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
