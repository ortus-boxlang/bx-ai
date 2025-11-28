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
		    memory = aiMemory( "boxvector", createUUID(), {
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
		    memory = aiMemory( "boxvector", createUUID(), {
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
		    memory = aiMemory( "boxvector", createUUID(), {
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
		    memory = aiMemory( "boxvector", createUUID(), {
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
		    memory = aiMemory( "boxvector", createUUID(), {
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
		    memory = aiMemory( "boxvector", createUUID(), {
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
		    memory = aiMemory( "boxvector", createUUID(), {
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
		    memory = aiMemory( "boxvector", createUUID(), {
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
		    memory = aiMemory( "boxvector", createUUID(), {
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

}
