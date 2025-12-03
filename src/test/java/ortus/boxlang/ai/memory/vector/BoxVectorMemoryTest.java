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
@DisplayName( "BoxVectorMemory Integration Tests" )
public class BoxVectorMemoryTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@Test
	@Order( 1 )
	@DisplayName( "Test BoxVectorMemory creation and basic operations" )
	void testBasicOperations() throws Exception {

		runtime.executeSource(
		    """
		    // Create BoxVectorMemory instance
		    memory = aiMemory( memory: "boxvector", key: createUUID(), config: {
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    result = {
		        type: memory.getName(),
		        configured: true
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( "BoxVectorMemory", testResult.getAsString( Key.of( "type" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "configured" ) ) );
	}

	@Test
	@Order( 2 )
	@DisplayName( "Test storing and retrieving documents" )
	void testStoreAndRetrieve() throws Exception {

		runtime.executeSource(
		    """
		    // Create BoxVectorMemory instance with embeddings
		    memory = aiMemory( memory: "boxvector", key: createUUID(), config: {
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add test documents
		    memory.add( {
		        id: "doc1",
		        text: "BoxLang is a modern JVM language",
		        metadata: { category: "programming" }
		    } );

		    memory.add( {
		        id: "doc2",
		        text: "Java is an object-oriented programming language",
		        metadata: { category: "programming" }
		    } );

		    memory.add( {
		        id: "doc3",
		        text: "Cooking pasta requires boiling water",
		        metadata: { category: "cooking" }
		    } );

		    // Semantic search
		    results = memory.getRelevant( "programming languages", 2 );

		    result = {
		        resultCount: results.len(),
		        hasScores: results.len() > 0 && results[1].keyExists( "score" ),
		        hasText: results.len() > 0 && results[1].keyExists( "text" ),
		        hasMetadata: results.len() > 0 && results[1].keyExists( "metadata" ),
		        firstCategory: results.len() > 0 ? results[1].metadata.category : ""
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsInteger( Key.of( "resultCount" ) ) >= 2 );
		assertTrue( testResult.getAsBoolean( Key.of( "hasScores" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "hasText" ) ) );
		assertTrue( testResult.getAsBoolean( Key.of( "hasMetadata" ) ) );
		assertEquals( "programming", testResult.getAsString( Key.of( "firstCategory" ) ) );
	}

	@Test
	@Order( 3 )
	@DisplayName( "Test document count" )
	void testCount() throws Exception {

		runtime.executeSource(
		    """
		    // Create BoxVectorMemory instance
		    memory = aiMemory( memory: "boxvector", key: createUUID(), config: {
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    initialCount = memory.count();

		    // Add test documents
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
	@Order( 4 )
	@DisplayName( "Test clear all documents" )
	void testClear() throws Exception {

		runtime.executeSource(
		    """
		    // Create and populate memory
		    memory = aiMemory( memory: "boxvector", key: createUUID(), config: {
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    memory.add( { id: "doc1", text: "First document" } );
		    memory.add( { id: "doc2", text: "Second document" } );
		    memory.add( { id: "doc3", text: "Third document" } );

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
	@DisplayName( "Test multiple document additions" )
	void testMultipleAdditions() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory with multiple documents
		    memory = aiMemory( memory: "boxvector", key: createUUID(), config: {
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    memory.add( { id: "doc1", text: "Java programming", metadata: { type: "code" } } );
		    memory.add( { id: "doc2", text: "Python scripting", metadata: { type: "code" } } );
		    memory.add( { id: "doc3", text: "Italian pasta recipe", metadata: { type: "recipe" } } );

		    // Search for relevant documents
		    results = memory.getRelevant( "programming", 10 );

		    result = {
		        totalCount: memory.count(),
		        resultCount: results.len(),
		        hasResults: results.len() > 0
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( 3, testResult.getAsInteger( Key.of( "totalCount" ) ) );
		assertTrue( testResult.getAsInteger( Key.of( "resultCount" ) ) >= 1 );
		assertTrue( testResult.getAsBoolean( Key.of( "hasResults" ) ) );
	}

	@Test
	@Order( 6 )
	@DisplayName( "Test empty search results" )
	void testEmptySearch() throws Exception {

		runtime.executeSource(
		    """
		    // Create empty memory
		    memory = aiMemory( memory: "boxvector", key: createUUID(), config: {
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Search with no data
		    results = memory.getRelevant( "test query", 10 );

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
	@DisplayName( "Test memory type name" )
	void testMemoryType() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory
		    memory = aiMemory( memory: "boxvector", key: createUUID(), config: {
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    result = {
		        typeName: memory.getName()
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( "BoxVectorMemory", testResult.getAsString( Key.of( "typeName" ) ) );
	}

	@Test
	@Order( 8 )
	@DisplayName( "Test limit parameter in search" )
	void testSearchLimit() throws Exception {

		runtime.executeSource(
		    """
		    // Create and populate memory with many documents
		    memory = aiMemory( memory: "boxvector", key: createUUID(), config: {
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    for ( i = 1; i <= 10; i++ ) {
		        memory.add( { id: "doc#i#", text: "Document number #i# about programming", metadata: { index: i } } );
		    }

		    // Search with limit of 3
		    results = memory.getRelevant( "programming", 3 );

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
	@Order( 9 )
	@DisplayName( "Test aiMemory BIF integration" )
	void testAiMemoryIntegration() throws Exception {

		runtime.executeSource(
		    """
		    // Create BoxVectorMemory via aiMemory BIF
		    memory1 = aiMemory( "boxvector", "test-collection" );
		    memory2 = aiMemory( "BoxVectorMemory", "another-collection" );

		    result = {
		        type1: memory1.getName(),
		        type2: memory2.getName()
		    }
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( "BoxVectorMemory", testResult.getAsString( Key.of( "type1" ) ) );
		assertEquals( "BoxVectorMemory", testResult.getAsString( Key.of( "type2" ) ) );
	}

	@Test
	@Order( 10 )
	@DisplayName( "Test export includes memory type" )
	void testExportType() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "boxvector", key: createUUID(), config: {
		        embeddingProvider: "openai",
		        embeddingModel: "text-embedding-3-small"
		    } );

		    // Add a document
		    memory.add( { id: "doc1", text: "Test document" } );

		    // Export to check type property
		    exported = memory.export();

		    result = {
		        hasType: exported.keyExists( "type" ),
		        type: exported.keyExists( "type" ) ? exported.type : ""
		    };
		    """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertTrue( testResult.getAsBoolean( Key.of( "hasType" ) ) );
		assertEquals( "BoxVectorMemory", testResult.getAsString( Key.of( "type" ) ) );
	}

	@Test
	@Order( 11 )
	@DisplayName( "Test BoxVectorMemory with userId and conversationId" )
	void testUserIdAndConversationId() throws Exception {

		runtime.executeSource(
		    """
		    memory = aiMemory(
		        memory: "boxvector",
		        userId: "emma",
		        conversationId: "docs-search",
		        config: {
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    memory.add( { text: "Documentation about APIs" } );

		    result = {
		        userId: memory.getUserId(),
		        conversationId: memory.getConversationId()
		    };
		     """,
		    context );

		IStruct testResult = variables.getAsStruct( result );

		assertEquals( "emma", testResult.getAsString( Key.of( "userId" ) ) );
		assertEquals( "docs-search", testResult.getAsString( Key.of( "conversationId" ) ) );
	}

	@Test
	@Order( 12 )
	@DisplayName( "Test BoxVectorMemory export includes userId and conversationId" )
	void testExportIncludesIdentifiers() throws Exception {

		runtime.executeSource(
		    """
		       memory = aiMemory(
		           memory: "boxvector",
		           userId: "frank",
		           conversationId: "vector-test",
		           config: {
		               embeddingProvider: "openai",
		               embeddingModel: "text-embedding-3-small"
		           }
		       );

		    memory.add( { text: "Vector search test" } );

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
		assertEquals( "frank", testResult.getAsString( Key.of( "userId" ) ) );
		assertEquals( "vector-test", testResult.getAsString( Key.of( "conversationId" ) ) );
	}

	@Test
	@Order( 13 )
	@DisplayName( "Test multi-tenant isolation with userId and conversationId filtering" )
	void testMultiTenantIsolation() throws Exception {

		runtime.executeSource(
		    """
		    // Create memory for user alice, conversation chat1
		    memoryAliceChat1 = aiMemory(
		        memory: "boxvector",
		        userId: "alice",
		        conversationId: "chat1",
		        config: {
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Create memory for user alice, conversation chat2
		    memoryAliceChat2 = aiMemory(
		        memory: "boxvector",
		        userId: "alice",
		        conversationId: "chat2",
		        config: {
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Create memory for user bob, conversation chat1
		    memoryBobChat1 = aiMemory(
		        memory: "boxvector",
		        userId: "bob",
		        conversationId: "chat1",
		        config: {
		            embeddingProvider: "openai",
		            embeddingModel: "text-embedding-3-small"
		        }
		    );

		    // Add documents to each memory
		    memoryAliceChat1.add( { text: "Alice chat1: BoxLang is awesome" } );
		    memoryAliceChat2.add( { text: "Alice chat2: Java integration rocks" } );
		    memoryBobChat1.add( { text: "Bob chat1: Vector databases are cool" } );

		    // Search in Alice's chat1 - should only return Alice's chat1 documents
		    resultsAliceChat1 = memoryAliceChat1.getRelevant( query: "BoxLang", limit: 10 );

		    // Search in Alice's chat2 - should only return Alice's chat2 documents
		    resultsAliceChat2 = memoryAliceChat2.getRelevant( query: "Java", limit: 10 );

		    // Search in Bob's chat1 - should only return Bob's chat1 documents
		    resultsBobChat1 = memoryBobChat1.getRelevant( query: "Vector", limit: 10 );

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

}
