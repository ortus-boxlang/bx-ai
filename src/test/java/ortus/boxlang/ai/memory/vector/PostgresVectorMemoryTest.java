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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for PostgresVectorMemory
 */
public class PostgresVectorMemoryTest extends BaseIntegrationTest {

	static String DATASOURCE_NAME = "postgres_vector_test";

	@BeforeEach
	public void beforeEach() {
		// Set module settings for embedding provider
		moduleRecord.settings.put( "embeddingProvider", "openai" );
		moduleRecord.settings.put( "embeddingModel", "text-embedding-3-small" );
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
	}

	@DisplayName( "Test PostgresVectorMemory basic configuration" )
	@Test
	public void testBasicConfiguration() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.PostgresVectorMemory;

			memory = new PostgresVectorMemory( "test_config", "test_collection" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors",
				collection: "test_collection",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small",
				distanceFunction: "COSINE",
				indexType: "HNSW"
			});

			collectionName = memory.getCollection();
			table = memory.getTable();
			datasourceName = memory.getDatasource();
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var	collectionName	= variables.getAsString( Key.of( "collectionName" ) );
		var	table			= variables.getAsString( Key.of( "table" ) );
		var	datasourceName	= variables.getAsString( Key.of( "datasourceName" ) );

		assertThat( collectionName ).isEqualTo( "test_collection" );
		assertThat( table ).isEqualTo( "test_vectors" );
		assertThat( datasourceName ).isEqualTo( DATASOURCE_NAME );
	}

	@DisplayName( "Test store and retrieve document" )
	@Test
	public void testStoreAndRetrieve() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.PostgresVectorMemory;

			memory = new PostgresVectorMemory( createUUID(), "test_store" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_store",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Store a document
			memory.add( "BoxLang is a modern dynamic JVM language" );
			memory.add( "PostgreSQL with pgvector enables semantic search" );

			// Get all documents
			allDocs = memory.getAll();
			docCount = allDocs.len();

			// Clean up
			memory.clear();
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var docCount = variables.getAsInteger( Key.of( "docCount" ) );
		assertThat( docCount ).isEqualTo( 2 );
	}

	@DisplayName( "Test semantic search" )
	@Test
	public void testSemanticSearch() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.PostgresVectorMemory;

			memory = new PostgresVectorMemory( createUUID(), "test_search" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_search",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Store test documents
			memory.add( "BoxLang is a modern dynamic JVM language" );
			memory.add( "PostgreSQL is a powerful relational database" );
			memory.add( "Vector databases enable semantic search capabilities" );

			// Search for similar content
			results = memory.getRelevant( "What is BoxLang?", 2 );
			resultCount = results.len();
			hasScore = results[1].keyExists( "score" );
			hasText = results[1].keyExists( "text" );

			// Clean up
			memory.clear();
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var	resultCount	= variables.getAsInteger( Key.of( "resultCount" ) );
		var	hasScore	= variables.getAsBoolean( Key.of( "hasScore" ) );
		var	hasText		= variables.getAsBoolean( Key.of( "hasText" ) );

		assertThat( resultCount ).isEqualTo( 2 );
		assertThat( hasScore ).isTrue();
		assertThat( hasText ).isTrue();
	}

	@DisplayName( "Test addWithId and getById" )
	@Test
	public void testAddWithIdAndGetById() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.PostgresVectorMemory;

			memory = new PostgresVectorMemory( createUUID(), "test_byid" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_byid",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Add document with explicit ID
			memory.addWithId(
				id: "doc123",
				text: "BoxLang vector memory with PostgreSQL",
				metadata: { category: "documentation", version: 1 }
			);

			// Retrieve by ID
			doc = memory.getById( "doc123" );
			hasDoc = !doc.isEmpty();
			docId = hasDoc ? doc.id : "";
			docText = hasDoc ? doc.text : "";

			// Clean up
			memory.clear();
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var	hasDoc	= variables.getAsBoolean( Key.of( "hasDoc" ) );
		var	docId	= variables.getAsString( Key.of( "docId" ) );
		var	docText	= variables.getAsString( Key.of( "docText" ) );

		assertThat( hasDoc ).isTrue();
		assertThat( docId ).isEqualTo( "doc123" );
		assertThat( docText ).contains( "BoxLang" );
	}

	@DisplayName( "Test remove document" )
	@Test
	public void testRemoveDocument() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.PostgresVectorMemory;

			memory = new PostgresVectorMemory( createUUID(), "test_remove" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_remove",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Add documents
			memory.addWithId( id: "remove1", text: "Document to remove" );
			memory.addWithId( id: "remove2", text: "Document to keep" );

			// Verify both exist
			countBefore = memory.getAll().len();

			// Remove one document
			removed = memory.remove( "remove1" );

			// Verify count decreased
			countAfter = memory.getAll().len();

			// Clean up
			memory.clear();
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var	countBefore	= variables.getAsInteger( Key.of( "countBefore" ) );
		var	removed		= variables.getAsBoolean( Key.of( "removed" ) );
		var	countAfter	= variables.getAsInteger( Key.of( "countAfter" ) );

		assertThat( countBefore ).isEqualTo( 2 );
		assertThat( removed ).isTrue();
		assertThat( countAfter ).isEqualTo( 1 );
	}

	@DisplayName( "Test metadata filtering" )
	@Test
	public void testMetadataFiltering() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.PostgresVectorMemory;

			memory = new PostgresVectorMemory( createUUID(), "test_filter" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_filter",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Add documents with metadata
			memory.addWithId(
				id: "doc1",
				text: "BoxLang programming guide",
				metadata: { category: "tutorial", language: "boxlang" }
			);
			memory.addWithId(
				id: "doc2",
				text: "PostgreSQL administration",
				metadata: { category: "tutorial", language: "sql" }
			);
			memory.addWithId(
				id: "doc3",
				text: "BoxLang API reference",
				metadata: { category: "reference", language: "boxlang" }
			);

			// Search with metadata filter
			results = memory.getRelevant(
				query: "BoxLang programming",
				limit: 5,
				filter: { category: "tutorial" }
			);

			resultCount = results.len();
			firstDocId = results.len() > 0 ? results[1].id : "";

			// Clean up
			memory.clear();
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var	resultCount	= variables.getAsInteger( Key.of( "resultCount" ) );
		var	firstDocId	= variables.getAsString( Key.of( "firstDocId" ) );

		// Should only return tutorial documents
		assertThat( resultCount ).isGreaterThan( 0 );
		assertThat( firstDocId ).isEqualTo( "doc1" );
	}

	@DisplayName( "Test batch seed operation" )
	@Test
	public void testBatchSeed() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.PostgresVectorMemory;

			memory = new PostgresVectorMemory( createUUID(), "test_seed" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_seed",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Seed multiple documents at once
			result = memory.seed([
				"BoxLang is a modern dynamic JVM language",
				"PostgreSQL with pgvector enables semantic search",
				{ text: "Vector databases store embeddings", metadata: { type: "concept" } }
			]);

			addedCount = result.added;
			failedCount = result.failed;

			// Verify all were added
			allDocs = memory.getAll();
			totalCount = allDocs.len();

			// Clean up
			memory.clear();
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var	addedCount	= variables.getAsInteger( Key.of( "addedCount" ) );
		var	failedCount	= variables.getAsInteger( Key.of( "failedCount" ) );
		var	totalCount	= variables.getAsInteger( Key.of( "totalCount" ) );

		assertThat( addedCount ).isEqualTo( 3 );
		assertThat( failedCount ).isEqualTo( 0 );
		assertThat( totalCount ).isEqualTo( 3 );
	}

	@DisplayName( "Test HybridMemory with PostgreSQL" )
	@Test
	public void testHybridMemory() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.HybridMemory;

			// Create hybrid memory with Postgres vector backend
			memory = new HybridMemory( createUUID() );
			memory.configure({
				recentLimit: 3,
				semanticLimit: 3,
				totalLimit: 5,
				vectorProvider: "postgres",
				vectorConfig: {
					datasource: "%s",
					table: "test_vectors_hybrid",
					collection: "hybrid_test",
					embeddingProvider: "openai",
					embeddingModel: "text-embedding-3-small"
				}
			});

			// Add messages
			memory.add( "My name is Alice" );
			memory.add( "I work as a software engineer" );
			memory.add( "I like BoxLang programming" );
			memory.add( "PostgreSQL is my favorite database" );
			memory.add( "What is my name?" );

			// Get relevant messages (should combine recent + semantic)
			results = memory.getRelevant( "Tell me about Alice", 5 );
			resultCount = results.len();
			hasName = results.toList( "text" ).findNoCase( "Alice" ) > 0;

			// Clean up
			memory.clear();
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var	resultCount	= variables.getAsInteger( Key.of( "resultCount" ) );
		var	hasName		= variables.getAsBoolean( Key.of( "hasName" ) );

		assertThat( resultCount ).isGreaterThan( 0 );
		assertThat( hasName ).isTrue();
	}

}
