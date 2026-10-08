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
 * Integration tests for MariaDBVectorMemory (requires MariaDB 11.7+ and the bx-mariadb module)
 */
public class MariaDBVectorMemoryTest extends BaseIntegrationTest {

	static String DATASOURCE_NAME = "mariadb_vector_test";

	@BeforeEach
	public void beforeEach() {
		// Set module settings for embedding provider
		moduleRecord.settings.put( "embeddingProvider", "openai" );
		moduleRecord.settings.put( "embeddingModel", "text-embedding-3-small" );
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
	}

	@DisplayName( "Test MariaDBVectorMemory basic configuration" )
	@Test
	public void testBasicConfiguration() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( "test_config", "test_collection" );
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
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_store" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_store_%s",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Store a document
			memory.add( "BoxLang is a modern dynamic JVM language" );
			memory.add( "MariaDB with native vector support enables semantic search" );

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
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_search" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_search",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Store test documents
			memory.add( "BoxLang is a modern dynamic JVM language" );
			memory.add( "MariaDB is a powerful relational database" );
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
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_byid" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_byid",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Add document with explicit ID
			memory.addWithId(
				id: "doc123",
				text: "BoxLang vector memory with MariaDB",
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
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_remove" );
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
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_filter" );
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
				text: "MariaDB administration",
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
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_seed" );
			memory.configure({
				datasource: "%s",
				table: "test_vectors_seed_batch",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Seed multiple documents at once
			result = memory.seed([
				"BoxLang is a modern dynamic JVM language",
				"MariaDB with vector support enables semantic search",
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

	@DisplayName( "Test HybridMemory with MariaDB" )
	@Test
	public void testHybridMemory() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.HybridMemory;

			// Create hybrid memory with MariaDB vector backend
			memory = new HybridMemory( createUUID() );
			memory.configure({
				recentLimit: 3,
				semanticLimit: 3,
				totalLimit: 5,
				vectorProvider: "mariadb",
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
			memory.add( "MariaDB is my favorite database" );
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

	@DisplayName( "Test aiMemory BIF with mariadb type" )
	@Test
	public void testAiMemoryBifWithMariadbType() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			memory = aiMemory(
				memory: "mariadb",
				config: {
					datasource: "%s",
					table: "test_bif_mariadb",
					embeddingProvider: "openai",
					embeddingModel: "text-embedding-3-small"
				}
			);

			className = memory.getName();
			isMariaDBMemory = className.findNoCase( "MariaDBVectorMemory" ) > 0;
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var isMariaDBMemory = variables.getAsBoolean( Key.of( "isMariaDBMemory" ) );
		assertThat( isMariaDBMemory ).isTrue();
	}

	@DisplayName( "Test MariaDBVectorMemory with userId and conversationId" )
	@Test
	public void testUserIdAndConversationId() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			memory = aiMemory(
				memory: "mariadb",
				userId: "john",
				conversationId: "mariadb-test",
				config: {
					datasource: "%s",
					table: "test_user_conv_%s",
					embeddingProvider: "openai",
					embeddingModel: "text-embedding-3-small"
				}
			);

			memory.add( { text: "MariaDB vector database" } );

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
		assertThat( results.getAsString( Key.of( "conversationId" ) ) ).isEqualTo( "mariadb-test" );
	}

	@DisplayName( "Test MariaDBVectorMemory export includes userId and conversationId" )
	@Test
	public void testExportIncludesIdentifiers() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			memory = aiMemory(
				memory: "mariadb",
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
				memory: "mariadb",
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
				memory: "mariadb",
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
				memory: "mariadb",
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
			memoryAliceChat1.add( { text: "Alice chat1: MariaDB is relational" } );
			memoryAliceChat2.add( { text: "Alice chat2: Vector search is fast" } );
			memoryBobChat1.add( { text: "Bob chat1: Database indexing" } );

			// Search in Alice's chat1 - should only return Alice's chat1 documents
			resultsAliceChat1 = memoryAliceChat1.getRelevant( query: "MariaDB", limit: 10 );

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

		IStruct	result				= variables.getAsStruct( Key.of( "result" ) );

		var		countAliceChat1		= result.getAsInteger( Key.of( "countAliceChat1" ) );
		var		countAliceChat2		= result.getAsInteger( Key.of( "countAliceChat2" ) );
		var		countBobChat1		= result.getAsInteger( Key.of( "countBobChat1" ) );
		var		allCountAliceChat1	= result.getAsInteger( Key.of( "allCountAliceChat1" ) );
		var		allCountAliceChat2	= result.getAsInteger( Key.of( "allCountAliceChat2" ) );
		var		allCountBobChat1	= result.getAsInteger( Key.of( "allCountBobChat1" ) );
		var		aliceChat1UserId	= result.getAsString( Key.of( "aliceChat1UserId" ) );
		var		aliceChat1ConvId	= result.getAsString( Key.of( "aliceChat1ConvId" ) );
		var		aliceChat2UserId	= result.getAsString( Key.of( "aliceChat2UserId" ) );
		var		aliceChat2ConvId	= result.getAsString( Key.of( "aliceChat2ConvId" ) );
		var		bobChat1UserId		= result.getAsString( Key.of( "bobChat1UserId" ) );
		var		bobChat1ConvId		= result.getAsString( Key.of( "bobChat1ConvId" ) );

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

	@DisplayName( "Test DOT distance is rejected since MariaDB has no native dot product" )
	@Test
	public void testDotDistanceRejected() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			errorType = "";
			try {
				memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_dot" );
				memory.configure({ datasource: "%s", distanceFunction: "DOT" });
			} catch ( any e ) {
				errorType = e.type;
			}
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "MariaDBVectorMemory.InvalidDistanceFunction" );
	}

	@DisplayName( "Test missing datasource and invalid table name are rejected" )
	@Test
	public void testConfigValidation() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			missingDsType = "";
			badTableType = "";
			badMType = "";
			try {
				new MariaDBVectorMemory( key: createUUID(), collection: "c" ).configure({});
			} catch ( any e ) {
				missingDsType = e.type;
			}
			try {
				new MariaDBVectorMemory( key: createUUID(), collection: "c" ).configure({ datasource: "%s", table: "bad; DROP TABLE x" });
			} catch ( any e ) {
				badTableType = e.type;
			}
			try {
				new MariaDBVectorMemory( key: createUUID(), collection: "c" ).configure({ datasource: "%s", m: 500 });
			} catch ( any e ) {
				badMType = e.type;
			}
			"""
			.formatted( DATASOURCE_NAME, DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "missingDsType" ) ) ).isEqualTo( "MariaDBVectorMemory.MissingDatasource" );
		assertThat( variables.getAsString( Key.of( "badTableType" ) ) ).isEqualTo( "MariaDBVectorMemory.InvalidTable" );
		assertThat( variables.getAsString( Key.of( "badMType" ) ) ).isEqualTo( "MariaDBVectorMemory.InvalidM" );
	}

	@DisplayName( "Test autoCreate false fails when the table does not exist" )
	@Test
	public void testAutoCreateFalse() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			errorType = "";
			try {
				new MariaDBVectorMemory( key: createUUID(), collection: "c" ).configure({
					datasource: "%s",
					table: "table_that_does_not_exist_%s",
					autoCreate: false
				});
			} catch ( any e ) {
				errorType = e.type;
			}
			"""
			.formatted( DATASOURCE_NAME, System.currentTimeMillis() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "MariaDBVectorMemory.TableNotFound" );
	}

	@DisplayName( "Test an existing table built for a different metric is rejected" )
	@Test
	public void testDistanceMismatch() throws Exception {
		var uniqueTable = "test_mismatch_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			// Creates the table with a cosine index
			first = new MariaDBVectorMemory( key: createUUID(), collection: "c" );
			first.configure({ datasource: "%s", table: "%s", distanceFunction: "COSINE" });

			second = new MariaDBVectorMemory( key: createUUID(), collection: "c" );
			errorType = "";
			try {
				second.configure({ datasource: "%s", table: "%s", distanceFunction: "L2" });
			} catch ( any e ) {
				errorType = e.type;
			}
			"""
			.formatted( DATASOURCE_NAME, uniqueTable, DATASOURCE_NAME, uniqueTable ),
			context
		);
		// @formatter:on

		// Clean up the table
		// @formatter:off
		runtime.executeSource(
			"""
			queryExecute( "DROP TABLE %s", {}, { datasource: "%s" } );
			"""
			.formatted( uniqueTable, DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "MariaDBVectorMemory.DistanceMismatch" );
	}

	@DisplayName( "Test embedding with the wrong dimensions gives a clear error" )
	@Test
	public void testDimensionMismatch() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_dims" );
			memory.configure({ datasource: "%s", table: "test_vectors_dims", dimensions: 8 });

			errorType = "";
			try {
				memory.findSimilar( [ 0.1, 0.2, 0.3 ], 5 );
			} catch ( any e ) {
				errorType = e.type;
			}
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "MariaDBVectorMemory.DimensionMismatch" );
	}

	@DisplayName( "Test native L2 search ranks the closest vector first" )
	@Test
	public void testNativeL2Ranking() throws Exception {
		var uniqueTable = "test_l2_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_l2" );
			memory.configure({ datasource: "%s", table: "%s", dimensions: 3, distanceFunction: "L2" });

			// Store pre-computed embeddings so no embedding provider is needed
			memory.storeDocument( "near", "near", [ 1, 0, 0 ], {} );
			memory.storeDocument( "mid", "mid", [ 0.5, 0.5, 0 ], {} );
			memory.storeDocument( "far", "far", [ 0, 0, 1 ], {} );

			results = memory.findSimilar( [ 1, 0, 0 ], 3 );
			ids = results.map( r => r.id );
			topScore = results[ 1 ].score;
			lastScore = results[ 3 ].score;

			queryExecute( "DROP TABLE %s", {}, { datasource: "%s" } );
			"""
			.formatted( DATASOURCE_NAME, uniqueTable, uniqueTable, DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		var ids = variables.getAsArray( Key.of( "ids" ) );
		assertThat( ids.get( 0 ) ).isEqualTo( "near" );
		assertThat( ids.get( 1 ) ).isEqualTo( "mid" );
		assertThat( ids.get( 2 ) ).isEqualTo( "far" );
		assertThat( ( ( Number ) variables.get( Key.of( "topScore" ) ) ).doubleValue() ).isGreaterThan( ( ( Number ) variables.get( Key.of( "lastScore" ) ) ).doubleValue() );
	}

	@DisplayName( "Test native cosine search ranks by direction, not magnitude" )
	@Test
	public void testNativeCosineRanking() throws Exception {
		var uniqueTable = "test_cosine_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_cosine" );
			memory.configure({ datasource: "%s", table: "%s", dimensions: 3, distanceFunction: "COSINE" });

			memory.storeDocument( "same-direction", "a", [ 10, 0, 0 ], {} );
			memory.storeDocument( "orthogonal", "b", [ 0, 1, 0 ], {} );

			results = memory.findSimilar( [ 1, 0, 0 ], 2 );
			firstId = results[ 1 ].id;
			firstScore = results[ 1 ].score;

			queryExecute( "DROP TABLE %s", {}, { datasource: "%s" } );
			"""
			.formatted( DATASOURCE_NAME, uniqueTable, uniqueTable, DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "firstId" ) ) ).isEqualTo( "same-direction" );
		assertThat( ( ( Number ) variables.get( Key.of( "firstScore" ) ) ).doubleValue() ).isWithin( 0.001 ).of( 1.0 );
	}

	@DisplayName( "Test unsafe filter keys are rejected" )
	@Test
	public void testUnsafeFilterKeyRejected() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "test_unsafe" );
			memory.configure({ datasource: "%s", table: "test_vectors_unsafe", dimensions: 3 });

			searchError = "";
			deleteError = "";
			try {
				memory.findSimilar( [ 1, 0, 0 ], 5, { "x') OR 1=1 -- ": "y" } );
			} catch ( any e ) {
				searchError = e.type;
			}
			try {
				memory.removeWhere( { "a.b": "y" } );
			} catch ( any e ) {
				deleteError = e.type;
			}
			"""
			.formatted( DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "searchError" ) ) ).isEqualTo( "MariaDBVectorMemory.InvalidFilterKey" );
		assertThat( variables.getAsString( Key.of( "deleteError" ) ) ).isEqualTo( "MariaDBVectorMemory.InvalidFilterKey" );
	}

	@DisplayName( "Test tenant isolation still returns results when many other-tenant rows are closer" )
	@Test
	public void testTenantIsolationWithCloserOtherTenantRows() throws Exception {
		var uniqueTable = "test_tenant_closer_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			config = { datasource: "%s", table: "%s", dimensions: 3, distanceFunction: "COSINE" };

			other = new MariaDBVectorMemory( key: createUUID(), collection: "shared", userId: "other-user" );
			other.configure( config );
			mine = new MariaDBVectorMemory( key: createUUID(), collection: "shared", userId: "me" );
			mine.configure( config );

			// 30 other-tenant rows are nearer to the query than my only row
			for ( i = 1; i <= 30; i++ ) {
				other.storeDocument( "o#i#", "other #i#", [ 1, 0.001 * i, 0 ], { userId: "other-user" } );
			}
			mine.storeDocument( "m1", "mine", [ 0, 1, 0 ], { userId: "me" } );

			results = mine.findSimilar( [ 1, 0, 0 ], 5 );
			resultCount = results.len();
			firstId = resultCount ? results[ 1 ].id : "";

			queryExecute( "DROP TABLE %s", {}, { datasource: "%s" } );
			"""
			.formatted( DATASOURCE_NAME, uniqueTable, uniqueTable, DATASOURCE_NAME ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "resultCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.getAsString( Key.of( "firstId" ) ) ).isEqualTo( "m1" );
	}

	@DisplayName( "Test CRUD, upsert, JSON metadata filter and tenant scoping with precomputed embeddings" )
	@Test
	public void testCrudWithPrecomputedEmbeddings() throws Exception {
		var uniqueTable = "test_crud_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.MariaDBVectorMemory;

			memory = new MariaDBVectorMemory( key: createUUID(), collection: "crud", userId: "alice", conversationId: "c1" );
			memory.configure({ datasource: "%s", table: "%s", dimensions: 3 });

			memory.storeDocument( "d1", "first", [ 1, 0, 0 ], { category: "tutorial", userId: "alice", conversationId: "c1" } );
			memory.storeDocument( "d2", "second", [ 0, 1, 0 ], { category: "reference", userId: "alice", conversationId: "c1" } );
			memory.storeDocument( "d3", "other conversation", [ 0, 0, 1 ], { category: "tutorial", userId: "alice", conversationId: "c2" } );

			// Upsert: same id replaces text and vector, no duplicate row
			memory.storeDocument( "d1", "first updated", [ 0.9, 0.1, 0 ], { category: "tutorial", userId: "alice", conversationId: "c1" } );

			byId = memory.getById( "d1" );
			missing = memory.getById( "nope" );
			allMine = memory.getAll();

			// JSON metadata filter + tenant scoping
			tutorials = memory.findSimilar( [ 1, 0, 0 ], 10, { category: "tutorial" } );

			removed = memory.remove( "d2" );
			removedAgain = memory.remove( "d2" );
			removedWhere = memory.removeWhere( { category: "tutorial", conversationId: "c2" } );

			result = {
				text: byId.text,
				embeddingLen: byId.embedding.len(),
				missingEmpty: missing.isEmpty(),
				allCount: allMine.len(),
				tutorialIds: tutorials.map( r => r.id ),
				removed: removed,
				removedAgain: removedAgain,
				removedWhere: removedWhere
			};
			"""
			.formatted( DATASOURCE_NAME, uniqueTable ),
			context
		);
		// @formatter:on

		runtime.executeSource( "queryExecute( \"DROP TABLE " + uniqueTable + "\", {}, { datasource: \"" + DATASOURCE_NAME + "\" } );", context );

		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "text" ) ) ).isEqualTo( "first updated" );
		assertThat( result.getAsInteger( Key.of( "embeddingLen" ) ) ).isEqualTo( 3 );
		assertThat( result.getAsBoolean( Key.of( "missingEmpty" ) ) ).isTrue();
		// d1 and d2 belong to conversation c1, d3 to c2
		assertThat( result.getAsInteger( Key.of( "allCount" ) ) ).isEqualTo( 2 );
		// Only d1 is a tutorial in this conversation
		assertThat( result.getAsArray( Key.of( "tutorialIds" ) ) ).containsExactly( "d1" );
		assertThat( result.getAsBoolean( Key.of( "removed" ) ) ).isTrue();
		assertThat( result.getAsBoolean( Key.of( "removedAgain" ) ) ).isFalse();
		assertThat( result.getAsInteger( Key.of( "removedWhere" ) ) ).isEqualTo( 1 );
	}

}
