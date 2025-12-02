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
import ortus.boxlang.runtime.types.IStruct;

/**
 * Integration tests for MysqlVectorMemory
 */
public class MysqlVectorMemoryTest extends BaseIntegrationTest {

	static String DATASOURCE_NAME = "mysql_vector_test";

	@BeforeEach
	public void beforeEach() {
		// Set module settings for embedding provider
		moduleRecord.settings.put( "embeddingProvider", "openai" );
		moduleRecord.settings.put( "embeddingModel", "text-embedding-3-small" );
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
	}

	@DisplayName( "Test MysqlVectorMemory basic configuration" )
	@Test
	public void testBasicConfiguration() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MysqlVectorMemory;

			memory = new MysqlVectorMemory( "test_config", "test_collection" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors",
				collection: "test_collection",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small",
				distanceFunction: "COSINE"
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
			import bxModules.bxai.models.memory.vector.MysqlVectorMemory;

			memory = new MysqlVectorMemory( key: createUUID(), collection: "test_store" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_store_%s",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Store a document
			memory.add( "BoxLang is a modern dynamic JVM language" );
			memory.add( "MySQL 9 with native vector support enables semantic search" );

			// Get all documents
			allDocs = memory.getAll();
			docCount = allDocs.len();

			// Clean up
			memory.clear();
			"""
			.formatted( DATASOURCE_NAME, System.currentTimeMillis() ),
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
			import bxModules.bxai.models.memory.vector.MysqlVectorMemory;

			memory = new MysqlVectorMemory( key: createUUID(), collection: "test_search" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_search",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Store test documents
			memory.add( "BoxLang is a modern dynamic JVM language" );
			memory.add( "MySQL is a powerful relational database" );
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
			import bxModules.bxai.models.memory.vector.MysqlVectorMemory;

			memory = new MysqlVectorMemory( key: createUUID(), collection: "test_byid" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_byid",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Add document with explicit ID
			memory.addWithId(
				id: "doc123",
				text: "BoxLang vector memory with MySQL",
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
			import bxModules.bxai.models.memory.vector.MysqlVectorMemory;

			memory = new MysqlVectorMemory( key: createUUID(), collection: "test_remove" );
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
			import bxModules.bxai.models.memory.vector.MysqlVectorMemory;

			memory = new MysqlVectorMemory( key: createUUID(), collection: "test_filter" );
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
				text: "MySQL administration",
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
			import bxModules.bxai.models.memory.vector.MysqlVectorMemory;

			memory = new MysqlVectorMemory( key: createUUID(), collection: "test_seed" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_seed_batch",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Seed multiple documents at once
			result = memory.seed([
				"BoxLang is a modern dynamic JVM language",
				"MySQL 9 with vector support enables semantic search",
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

	@DisplayName( "Test HybridMemory with MySQL" )
	@Test
	public void testHybridMemory() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.HybridMemory;

			// Create hybrid memory with MySQL vector backend
			memory = new HybridMemory( createUUID() );
			memory.configure({
				recentLimit: 3,
				semanticLimit: 3,
				totalLimit: 5,
				vectorProvider: "mysql",
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
			memory.add( "MySQL is my favorite database" );
			memory.add( "What is my name?" );

			// Get relevant messages (should combine recent + semantic)
			results = memory.getRelevant( "Tell me about Alice", 5 );
			resultCount = results.len();

			// Check if any result contains "Alice"
			hasName = false;
			results.each( function( item ) {
				if ( item.keyExists( "text" ) && item.text.findNoCase( "Alice" ) > 0 ) {
					hasName = true;
				}
			} );

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

	@DisplayName( "Test aiMemory BIF with mysql type" )
	@Test
	public void testAiMemoryBifWithMysqlType() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			memory = aiMemory(
				memory: "mysql",
				config: {
					datasource: "%s",
					table: "test_bif_mysql",
					embeddingProvider: "openai",
					embeddingModel: "text-embedding-3-small"
				}
			);

			className = memory.getName();
			isMysqlMemory = className.findNoCase( "MysqlVectorMemory" ) > 0;
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var isMysqlMemory = variables.getAsBoolean( Key.of( "isMysqlMemory" ) );
		assertThat( isMysqlMemory ).isTrue();
	}

	@DisplayName( "Test MysqlVectorMemory with userId and conversationId" )
	@Test
	public void testUserIdAndConversationId() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			memory = aiMemory(
				memory: "mysql",
				userId: "john",
				conversationId: "mysql-test",
				config: {
					datasource: "%s",
					table: "test_user_conv_%s",
					embeddingProvider: "openai",
					embeddingModel: "text-embedding-3-small"
				}
			);

			memory.add( { text: "MySQL vector database" } );

			result = {
				userId: memory.getUserId(),
				conversationId: memory.getConversationId()
			}
			"""
			.formatted( DATASOURCE_NAME, System.currentTimeMillis() ),
			context
		);
		// @formatter:on

		IStruct results = variables.getAsStruct( result );

		assertThat( results.getAsString( Key.of( "userId" ) ) ).isEqualTo( "john" );
		assertThat( results.getAsString( Key.of( "conversationId" ) ) ).isEqualTo( "mysql-test" );
	}

	@DisplayName( "Test MysqlVectorMemory export includes userId and conversationId" )
	@Test
	public void testExportIncludesIdentifiers() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			memory = aiMemory(
				memory: "mysql",
				userId: "jane",
				conversationId: "export-test",
				config: {
					datasource: "%s",
					table: "test_export_%s",
					embeddingProvider: "openai",
					embeddingModel: "text-embedding-3-small"
				}
			);

			memory.add( { text: "Export test document" } );

			exported = memory.export();
			"""
			.formatted( DATASOURCE_NAME, System.currentTimeMillis() ),
			context
		);
		// @formatter:on

		IStruct exported = variables.getAsStruct( Key.of( "exported" ) );

		assertThat( exported.getAsString( Key.of( "userId" ) ) ).isEqualTo( "jane" );
		assertThat( exported.getAsString( Key.of( "conversationId" ) ) ).isEqualTo( "export-test" );
	}

	@DisplayName( "Test multi-tenant isolation with userId and conversationId filtering" )
	@Test
	public void testMultiTenantIsolation() throws Exception {
		var uniqueTable = "test_multi_tenant_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			uniqueTable = "%s";

			// Create memory for user alice, conversation chat1
			memoryAliceChat1 = aiMemory(
				memory: "mysql",
				userId: "alice",
				conversationId: "chat1",
				config: {
					datasource: "%s",
					table: uniqueTable,
					embeddingProvider: "openai",
					embeddingModel: "text-embedding-3-small"
				}
			);

			// Create memory for user alice, conversation chat2
			memoryAliceChat2 = aiMemory(
				memory: "mysql",
				userId: "alice",
				conversationId: "chat2",
				config: {
					datasource: "%s",
					table: uniqueTable,
					embeddingProvider: "openai",
					embeddingModel: "text-embedding-3-small"
				}
			);

			// Create memory for user bob, conversation chat1
			memoryBobChat1 = aiMemory(
				memory: "mysql",
				userId: "bob",
				conversationId: "chat1",
				config: {
					datasource: "%s",
					table: uniqueTable,
					embeddingProvider: "openai",
					embeddingModel: "text-embedding-3-small"
				}
			);

			// Add documents to each memory
			memoryAliceChat1.add( { text: "Alice chat1: MySQL is relational" } );
			memoryAliceChat2.add( { text: "Alice chat2: Vector search is fast" } );
			memoryBobChat1.add( { text: "Bob chat1: Database indexing" } );

			// Search in Alice's chat1 - should only return Alice's chat1 documents
			resultsAliceChat1 = memoryAliceChat1.getRelevant( query: "MySQL", limit: 10 );

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

			// Clean up
			memoryAliceChat1.clear();
			"""
			.formatted( uniqueTable, DATASOURCE_NAME, DATASOURCE_NAME, DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var	countAliceChat1		= variables.getAsInteger( Key.of( "countAliceChat1" ) );
		var	countAliceChat2		= variables.getAsInteger( Key.of( "countAliceChat2" ) );
		var	countBobChat1		= variables.getAsInteger( Key.of( "countBobChat1" ) );
		var	allCountAliceChat1	= variables.getAsInteger( Key.of( "allCountAliceChat1" ) );
		var	allCountAliceChat2	= variables.getAsInteger( Key.of( "allCountAliceChat2" ) );
		var	allCountBobChat1	= variables.getAsInteger( Key.of( "allCountBobChat1" ) );
		var	aliceChat1UserId	= variables.getAsString( Key.of( "aliceChat1UserId" ) );
		var	aliceChat1ConvId	= variables.getAsString( Key.of( "aliceChat1ConvId" ) );
		var	aliceChat2UserId	= variables.getAsString( Key.of( "aliceChat2UserId" ) );
		var	aliceChat2ConvId	= variables.getAsString( Key.of( "aliceChat2ConvId" ) );
		var	bobChat1UserId		= variables.getAsString( Key.of( "bobChat1UserId" ) );
		var	bobChat1ConvId		= variables.getAsString( Key.of( "bobChat1ConvId" ) );

		// Each memory should only see its own documents
		assertThat( countAliceChat1 ).isEqualTo( 1 );
		assertThat( countAliceChat2 ).isEqualTo( 1 );
		assertThat( countBobChat1 ).isEqualTo( 1 );

		// getAll should also only return isolated documents
		assertThat( allCountAliceChat1 ).isEqualTo( 1 );
		assertThat( allCountAliceChat2 ).isEqualTo( 1 );
		assertThat( allCountBobChat1 ).isEqualTo( 1 );

		// Verify metadata contains correct userId and conversationId
		assertThat( aliceChat1UserId ).isEqualTo( "alice" );
		assertThat( aliceChat1ConvId ).isEqualTo( "chat1" );
		assertThat( aliceChat2UserId ).isEqualTo( "alice" );
		assertThat( aliceChat2ConvId ).isEqualTo( "chat2" );
		assertThat( bobChat1UserId ).isEqualTo( "bob" );
		assertThat( bobChat1ConvId ).isEqualTo( "chat1" );
	}

}
