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
		    memory = aiMemory( "chroma", createUUID(), {
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
		    memory = aiMemory( "chroma", createUUID(), {
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
		     memory = aiMemory( "chroma", createUUID(), {
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
	@Order( 4 )
	@DisplayName( "Test HybridMemory with ChromaDB backend" )
	void testHybridMemory() throws Exception {

		runtime.executeSource(
		    """
		     // Create hybrid memory
		     hybridMemory = aiMemory( "hybrid", createUUID(), {
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