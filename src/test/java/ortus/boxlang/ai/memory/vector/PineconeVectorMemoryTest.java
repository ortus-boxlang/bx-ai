package ortus.boxlang.ai.memory.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
@DisplayName( "PineconeVectorMemory Integration Tests" )
public class PineconeVectorMemoryTest extends BaseIntegrationTest {

	private static String	PINECONE_API_KEY	= "test-api-key";		// Local server accepts any key
	private static String	PINECONE_INDEX		= "test-index";
	private static String	PINECONE_HOST		= "http://localhost:5080";

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@BeforeAll
	static void checkPineconeAvailability() {
		// For local development, use docker-compose Pinecone service
		// Override with environment variables if connecting to real Pinecone
		if ( System.getenv( "PINECONE_API_KEY" ) != null ) {
			PINECONE_API_KEY	= System.getenv( "PINECONE_API_KEY" );
			PINECONE_INDEX		= System.getenv( "PINECONE_INDEX" );
			PINECONE_HOST		= System.getenv( "PINECONE_HOST" );
		}

		// Verify Pinecone Local is accessible and create index if needed
		try {
			HttpClient				client			= HttpClient.newHttpClient();

			// First, check if index exists
			HttpRequest				listRequest		= HttpRequest.newBuilder()
			    .uri( URI.create( PINECONE_HOST + "/indexes" ) )
			    .header( "Api-Key", PINECONE_API_KEY )
			    .header( "Content-Type", "application/json" )
			    .GET()
			    .build();

			HttpResponse<String>	listResponse	= client.send( listRequest, HttpResponse.BodyHandlers.ofString() );
			assumeTrue( listResponse.statusCode() == 200, "Pinecone Local not accessible at " + PINECONE_HOST + ". Start with: docker compose up pinecone" );

			// Create index if it doesn't exist (for local development)
			if ( !listResponse.body().contains( "\"" + PINECONE_INDEX + "\"" ) ) {
				String					createPayload	= """
				                                          {
				                                            "name": "%s",
				                                            "dimension": 1536,
				                                            "metric": "cosine",
				                                            "spec": {
				                                              "serverless": {
				                                                "cloud": "aws",
				                                                "region": "us-east-1"
				                                              }
				                                            }
				                                          }
				                                          """.formatted( PINECONE_INDEX );

				HttpRequest				createRequest	= HttpRequest.newBuilder()
				    .uri( URI.create( PINECONE_HOST + "/indexes" ) )
				    .header( "Api-Key", PINECONE_API_KEY )
				    .header( "Content-Type", "application/json" )
				    .POST( HttpRequest.BodyPublishers.ofString( createPayload ) )
				    .build();

				HttpResponse<String>	createResponse	= client.send( createRequest, HttpResponse.BodyHandlers.ofString() );
				assumeTrue( createResponse.statusCode() == 201 || createResponse.statusCode() == 200,
				    "Failed to create index: " + createResponse.body() );

				// Wait a moment for index to be ready
				Thread.sleep( 2000 );
			}

			// Now get the index host
			HttpRequest				describeRequest		= HttpRequest.newBuilder()
			    .uri( URI.create( PINECONE_HOST + "/indexes/" + PINECONE_INDEX ) )
			    .header( "Api-Key", PINECONE_API_KEY )
			    .GET()
			    .build();

			HttpResponse<String>	describeResponse	= client.send( describeRequest, HttpResponse.BodyHandlers.ofString() );
			assumeTrue( describeResponse.statusCode() == 200, "Failed to describe index: " + describeResponse.body() );

		} catch ( Exception e ) {
			assumeTrue( false, "Failed to connect to Pinecone at " + PINECONE_HOST + ": " + e.getMessage() + ". Start with: docker compose up pinecone" );
		}
	}

	@Test
	@Order( 1 )
	@DisplayName( "Test PineconeVectorMemory creation and configuration" )
	void testPineconeVectorMemoryCreation() throws Exception {

		runtime.executeSource(
		    """
		    	import java.lang.System;

		    	// Get Pinecone config from environment
		    	apiKey = System.getenv( "PINECONE_API_KEY" );
		    	indexName = System.getenv( "PINECONE_INDEX" );
		    	host = System.getenv( "PINECONE_HOST" );

		    // Create PineconeVectorMemory instance
		    memory = aiMemory( "pinecone", createUUID(), {
		        apiKey: apiKey,
		        indexName: indexName,
		        host: host,
		        namespace: "test_namespace",
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

		assertEquals( "PineconeVectorMemory", testResult.getAsString( Key.of( "type" ) ) );
		assertEquals( "test_namespace", testResult.getAsString( Key.of( "collection" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "configured" ) ) );
	}

	@Test
	@Order( 2 )
	@DisplayName( "Test storing and retrieving documents with PineconeVectorMemory" )
	void testStoreAndRetrieve() throws Exception {

		runtime.executeSource(
		    """
		    	import java.lang.System;

		    	// Get Pinecone config
		    	apiKey = System.getenv( "PINECONE_API_KEY" );
		    	indexName = System.getenv( "PINECONE_INDEX" );
		    	host = System.getenv( "PINECONE_HOST" );

		    // Create memory with unique namespace
		    testNamespace = "test_store_" & left( createUUID(), 8 );
		    memory = aiMemory( "pinecone", testNamespace, {
		        apiKey: apiKey,
		        indexName: indexName,
		        host: host,
		        namespace: testNamespace,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Store documents
		    doc1Id = createUUID();
		    doc2Id = createUUID();

		    memory.add( doc1Id, "BoxLang is a modern JVM language", { category: "programming" } );
		    memory.add( doc2Id, "Vector databases store embeddings", { category: "database" } );

		    // Small delay to allow indexing
		    sleep( 2000 );

		    // Retrieve by semantic search
		    results = memory.getRelevant( "programming languages", 2 );

		    result = {
		        resultCount: results.len(),
		        hasScores: results.len() > 0 && results[1].keyExists( "score" ),
		        hasText: results.len() > 0 && results[1].keyExists( "text" )
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
	}

	@Test
	@Order( 3 )
	@DisplayName( "Test semantic search with PineconeVectorMemory" )
	void testSemanticSearch() throws Exception {

		runtime.executeSource(
		    """
		    	import java.lang.System;

		    	// Get Pinecone config
		    	apiKey = System.getenv( "PINECONE_API_KEY" );
		    	indexName = System.getenv( "PINECONE_INDEX" );
		    	host = System.getenv( "PINECONE_HOST" );

		    // Create memory
		    testNamespace = "test_search_" & left( createUUID(), 8 );
		    memory = aiMemory( "pinecone", testNamespace, {
		        apiKey: apiKey,
		        indexName: indexName,
		        host: host,
		        namespace: testNamespace,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add test documents
		    memory.add( "I love programming in Java" );
		    memory.add( "Python is great for data science" );
		    memory.add( "JavaScript powers the web" );

		    // Wait for indexing
		    sleep( 2000 );

		    // Search
		    results = memory.getRelevant( "programming languages", 2 );

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
		    	import java.lang.System;

		    	// Get Pinecone config
		    	apiKey = System.getenv( "PINECONE_API_KEY" );
		    	indexName = System.getenv( "PINECONE_INDEX" );
		    	host = System.getenv( "PINECONE_HOST" );

		    // Create memory
		    testNamespace = "test_getbyid_" & left( createUUID(), 8 );
		    memory = aiMemory( "pinecone", testNamespace, {
		        apiKey: apiKey,
		        indexName: indexName,
		        host: host,
		        namespace: testNamespace,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Store with explicit ID
		    testId = "test-doc-123";
		    memory.add( testId, "Test document content", { type: "test" } );

		    // Wait for indexing
		    sleep( 2000 );

		    // Retrieve by ID
		    doc = memory.getById( testId );

		    result = {
		        found: !doc.isEmpty(),
		        hasText: doc.keyExists( "text" ),
		        hasMetadata: doc.keyExists( "metadata" ),
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
		assertEquals( "test-doc-123", testResult.getAsString( Key.of( "id" ) ) );
	}

	@Test
	@Order( 5 )
	@DisplayName( "Test document deletion" )
	void testDeleteDocument() throws Exception {

		runtime.executeSource(
		    """
		    	import java.lang.System;

		    	// Get Pinecone config
		    	apiKey = System.getenv( "PINECONE_API_KEY" );
		    	indexName = System.getenv( "PINECONE_INDEX" );
		    	host = System.getenv( "PINECONE_HOST" );

		    // Create memory
		    testNamespace = "test_delete_" & left( createUUID(), 8 );
		    memory = aiMemory( "pinecone", testNamespace, {
		        apiKey: apiKey,
		        indexName: indexName,
		        host: host,
		        namespace: testNamespace,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Store document
		    testId = "doc-to-delete";
		    memory.add( testId, "This will be deleted" );

		    // Wait for indexing
		    sleep( 2000 );

		    // Delete
		    deleted = memory.remove( testId );

		    // Wait for deletion
		    sleep( 1000 );

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
		    	import java.lang.System;

		    	// Get Pinecone config
		    	apiKey = System.getenv( "PINECONE_API_KEY" );
		    	indexName = System.getenv( "PINECONE_INDEX" );
		    	host = System.getenv( "PINECONE_HOST" );

		    // Create memory
		    testNamespace = "test_filter_" & left( createUUID(), 8 );
		    memory = aiMemory( "pinecone", testNamespace, {
		        apiKey: apiKey,
		        indexName: indexName,
		        host: host,
		        namespace: testNamespace,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add documents with metadata
		    memory.add( "Programming tutorials for beginners", { category: "tutorial" } );
		    memory.add( "Advanced programming concepts", { category: "advanced" } );
		    memory.add( "Programming reference guide", { category: "reference" } );

		    // Wait for indexing
		    sleep( 2000 );

		    // Search with filter
		    results = memory.getRelevant(
		        "programming",
		        5,
		        { category: "tutorial" }
		    );

		    result = {
		        resultCount: results.len(),
		        allTutorials: true
		    };

		    // Verify all results have tutorial category
		    results.each( function( item ) {
		        if ( !item.metadata.keyExists( "category" ) || item.metadata.category != "tutorial" ) {
		            result.allTutorials = false;
		        }
		    } );

		    // Cleanup
		    try {
		        memory.clearAll();
		    } catch( any e ) {
		        // Ignore
		    }
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsInteger( Key.of( "resultCount" ) ) >= 1 );
		assertTrue( testResult.getAsBoolean( Key.of( "allTutorials" ) ) );
	}

	@Test
	@Order( 7 )
	@DisplayName( "Test batch seeding with PineconeVectorMemory" )
	void testBatchSeeding() throws Exception {

		runtime.executeSource(
		    """
		    	import java.lang.System;

		    	// Get Pinecone config
		    	apiKey = System.getenv( "PINECONE_API_KEY" );
		    	indexName = System.getenv( "PINECONE_INDEX" );
		    	host = System.getenv( "PINECONE_HOST" );

		    // Create memory
		    testNamespace = "test_batch_" & left( createUUID(), 8 );
		    memory = aiMemory( "pinecone", testNamespace, {
		        apiKey: apiKey,
		        indexName: indexName,
		        host: host,
		        namespace: testNamespace,
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Prepare documents
		    documents = [
		        "AI is transforming technology",
		        "Machine learning improves with data",
		        { text: "Deep learning uses neural networks", metadata: { type: "concept" } },
		        "Natural language processing"
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

		assertEquals( 4, result.getAsInteger( Key.of( "totalDocuments" ) ) );
		assertTrue( result.getAsInteger( Key.of( "added" ) ) >= 3 ); // Allow for some failures
	}

	@Test
	@Order( 8 )
	@DisplayName( "Test HybridMemory integration with PineconeVectorMemory" )
	void testHybridMemory() throws Exception {

		runtime.executeSource(
		    """
		    	import java.lang.System;

		    	// Get Pinecone config
		    	apiKey = System.getenv( "PINECONE_API_KEY" );
		    	indexName = System.getenv( "PINECONE_INDEX" );
		    	host = System.getenv( "PINECONE_HOST" );

		    // Create hybrid memory with Pinecone
		    testNamespace = "test_hybrid_" & left( createUUID(), 8 );
		    hybridMemory = aiMemory( "hybrid", testNamespace, {
		        vectorConfig: {
		            type: "pinecone",
		            apiKey: apiKey,
		            indexName: indexName,
		            host: host,
		            namespace: testNamespace,
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        },
		        conversationMemory: aiMemory( "conversation", "test", {} )
		    } );

		    // Add messages
		    hybridMemory.add( "User question about AI" );
		    hybridMemory.add( "AI response explaining concepts" );

		    // Wait for indexing
		    sleep( 2000 );

		    // Test retrieval
		    messages = hybridMemory.getAll();
		    relevant = hybridMemory.getRelevant( "artificial intelligence", 1 );

		    result = {
		        messageCount: messages.len(),
		        relevantCount: relevant.len()
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

		assertEquals( 2, result.getAsInteger( Key.of( "messageCount" ) ) );
		assertTrue( result.getAsInteger( Key.of( "relevantCount" ) ) >= 1 );
	}

}
