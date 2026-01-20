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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for OpenSearchVectorMemory
 *
 * Requires a running OpenSearch instance. Set the following environment variables:
 * - OPENSEARCH_HOST (default: localhost)
 * - OPENSEARCH_PORT (default: 9200)
 * - OPENSEARCH_SCHEME (default: https)
 */
public class OpenSearchVectorMemoryTest extends BaseIntegrationTest {

	static String	OPENSEARCH_HOST		= System.getenv( "OPENSEARCH_HOST" ) != null ? System.getenv( "OPENSEARCH_HOST" ) : "localhost";
	static int		OPENSEARCH_PORT		= System.getenv( "OPENSEARCH_PORT" ) != null ? Integer.parseInt( System.getenv( "OPENSEARCH_PORT" ) ) : 9200;
	static String	OPENSEARCH_SCHEME	= System.getenv( "OPENSEARCH_SCHEME" ) != null ? System.getenv( "OPENSEARCH_SCHEME" ) : "https";

	static boolean	openSearchAvailable	= false;

	@BeforeAll
	static void checkOpenSearchAvailability() {
		openSearchAvailable = isOpenSearchAvailable();
	}

	/**
	 * Check if OpenSearch is available at the configured host/port
	 */
	static boolean isOpenSearchAvailable() {
		try {
			URL					url			= new URL( OPENSEARCH_SCHEME + "://" + OPENSEARCH_HOST + ":" + OPENSEARCH_PORT );
			HttpURLConnection	connection	= ( HttpURLConnection ) url.openConnection();
			connection.setRequestMethod( "GET" );
			connection.setConnectTimeout( 3000 );
			connection.setReadTimeout( 3000 );
			int responseCode = connection.getResponseCode();
			connection.disconnect();
			return responseCode == 200;
		} catch ( Exception e ) {
			return false;
		}
	}

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "Test OpenSearchVectorMemory basic configuration" )
	@Test
	public void testBasicConfiguration() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			memory = new OpenSearchVectorMemory( "test_config", "test_collection" );

			// Set properties directly without connecting
			memory.host = "%s";
			memory.port = %d;
			memory.scheme = "%s";
			memory.index = "test_index";
			memory.engine = "lucene";
			memory.spaceType = "cosinesimilarity";

			hostValue = memory.host;
			portValue = memory.port;
			schemeValue = memory.scheme;
			indexValue = memory.index;
			engineValue = memory.engine;
			spaceTypeValue = memory.spaceType;
			"""
			.formatted( OPENSEARCH_HOST, OPENSEARCH_PORT, OPENSEARCH_SCHEME ),
			context
		);
		// @formatter:on

		var	hostValue		= variables.getAsString( Key.of( "hostValue" ) );
		var	portValue		= variables.getAsInteger( Key.of( "portValue" ) );
		var	schemeValue		= variables.getAsString( Key.of( "schemeValue" ) );
		var	indexValue		= variables.getAsString( Key.of( "indexValue" ) );
		var	engineValue		= variables.getAsString( Key.of( "engineValue" ) );
		var	spaceTypeValue	= variables.getAsString( Key.of( "spaceTypeValue" ) );

		assertThat( hostValue ).isEqualTo( OPENSEARCH_HOST );
		assertThat( portValue ).isEqualTo( OPENSEARCH_PORT );
		assertThat( schemeValue ).isEqualTo( OPENSEARCH_SCHEME );
		assertThat( indexValue ).isEqualTo( "test_index" );
		assertThat( engineValue ).isEqualTo( "lucene" );
		assertThat( spaceTypeValue ).isEqualTo( "cosinesimilarity" );
	}

	@DisplayName( "Test OpenSearch version compatibility mappings" )
	@Test
	public void testVersionCompatibilityMappings() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			// Check the static version mappings
			v2Mapping = OpenSearchVectorMemory::SPACE_TYPES_V2;
			v3Mapping = OpenSearchVectorMemory::SPACE_TYPES_V3;

			// OpenSearch 2.x uses "cosinesimil"
			v2CosineValue = v2Mapping["cosinesimilarity"];
			v2L2Value = v2Mapping["l2"];

			// OpenSearch 3.x uses "innerproduct" for cosinesimilarity
			v3CosineValue = v3Mapping["cosinesimilarity"];
			v3L2Value = v3Mapping["l2"];
			"""
			,
			context
		);
		// @formatter:on

		var	v2CosineValue	= variables.getAsString( Key.of( "v2CosineValue" ) );
		var	v2L2Value		= variables.getAsString( Key.of( "v2L2Value" ) );
		var	v3CosineValue	= variables.getAsString( Key.of( "v3CosineValue" ) );
		var	v3L2Value		= variables.getAsString( Key.of( "v3L2Value" ) );

		// Version 2.x should map to "cosinesimil"
		assertThat( v2CosineValue ).isEqualTo( "cosinesimil" );
		assertThat( v2L2Value ).isEqualTo( "l2" );

		// Version 3.x should use "innerproduct" for cosinesimilarity (cosinesimilarity not supported in lucene/faiss)
		assertThat( v3CosineValue ).isEqualTo( "innerproduct" );
		assertThat( v3L2Value ).isEqualTo( "l2" );
	}

	@DisplayName( "Test invalid scheme validation" )
	@Test
	public void testInvalidSchemeValidation() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			errorThrown = false;
			errorType = "";

			try {
				memory = new OpenSearchVectorMemory( "test_invalid", "test_collection" );
				memory.configure({
					host: "localhost",
					scheme: "ftp"  // Invalid scheme
				});
			} catch ( any e ) {
				errorThrown = true;
				errorType = e.type;
			}
			"""
			,
			context
		);
		// @formatter:on

		var	errorThrown	= variables.getAsBoolean( Key.of( "errorThrown" ) );
		var	errorType	= variables.getAsString( Key.of( "errorType" ) );

		assertThat( errorThrown ).isTrue();
		assertThat( errorType ).contains( "InvalidScheme" );
	}

	@DisplayName( "Test invalid engine validation" )
	@Test
	public void testInvalidEngineValidation() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			errorThrown = false;
			errorType = "";

			try {
				memory = new OpenSearchVectorMemory( "test_invalid", "test_collection" );
				memory.configure({
					host: "localhost",
					engine: "invalid_engine"  // Invalid engine
				});
			} catch ( any e ) {
				errorThrown = true;
				errorType = e.type;
			}
			"""
			,
			context
		);
		// @formatter:on

		var	errorThrown	= variables.getAsBoolean( Key.of( "errorThrown" ) );
		var	errorType	= variables.getAsString( Key.of( "errorType" ) );

		assertThat( errorThrown ).isTrue();
		assertThat( errorType ).contains( "InvalidEngine" );
	}

	@DisplayName( "Test invalid space type validation" )
	@Test
	public void testInvalidSpaceTypeValidation() throws Exception {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			errorThrown = false;
			errorType = "";

			try {
				memory = new OpenSearchVectorMemory( "test_invalid", "test_collection" );
				memory.configure({
					host: "localhost",
					spaceType: "invalid_space"  // Invalid space type
				});
			} catch ( any e ) {
				errorThrown = true;
				errorType = e.type;
			}
			"""
			,
			context
		);
		// @formatter:on

		var	errorThrown	= variables.getAsBoolean( Key.of( "errorThrown" ) );
		var	errorType	= variables.getAsString( Key.of( "errorType" ) );

		assertThat( errorThrown ).isTrue();
		assertThat( errorType ).contains( "InvalidSpaceType" );
	}

	@DisplayName( "Test store and retrieve document" )
	@Test
	public void testStoreAndRetrieve() throws Exception {
		assumeTrue( openSearchAvailable, "OpenSearch not available at " + OPENSEARCH_HOST + ":" + OPENSEARCH_PORT );

		var indexName = "bx_ai_test_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			memory = new OpenSearchVectorMemory( createUUID(), "test_store" );
			memory.configure({
				host: "%s",
				port: %d,
				scheme: "%s",
				index: "%s",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Store documents
			memory.add( "BoxLang is a modern dynamic JVM language" );
			memory.add( "OpenSearch enables semantic search with k-NN" );

			// Small delay for indexing
			sleep( 1000 );

			// Get all documents
			allDocs = memory.getAll();
			docCount = allDocs.len();

			// Clean up
			memory.clear();
			"""
			.formatted( OPENSEARCH_HOST, OPENSEARCH_PORT, OPENSEARCH_SCHEME, indexName ),
			context
		);
		// @formatter:on

		var docCount = variables.getAsInteger( Key.of( "docCount" ) );
		assertThat( docCount ).isEqualTo( 2 );
	}

	@DisplayName( "Test semantic search" )
	@Test
	public void testSemanticSearch() throws Exception {
		assumeTrue( openSearchAvailable, "OpenSearch not available at " + OPENSEARCH_HOST + ":" + OPENSEARCH_PORT );

		var indexName = "bx_ai_test_search_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			memory = new OpenSearchVectorMemory( createUUID(), "test_search" );
			memory.configure({
				host: "%s",
				port: %d,
				scheme: "%s",
				index: "%s",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Store test documents
			memory.add( "BoxLang is a modern dynamic JVM language" );
			memory.add( "OpenSearch is a powerful search engine" );
			memory.add( "Vector databases enable semantic search capabilities" );

			// Small delay for indexing
			sleep( 1000 );

			// Search for similar content
			results = memory.getRelevant( "What is BoxLang?", 2 );
			resultCount = results.len();
			hasScore = resultCount > 0 && results[1].keyExists( "score" );
			hasText = resultCount > 0 && results[1].keyExists( "text" );

			// Clean up
			memory.clear();
			"""
			.formatted( OPENSEARCH_HOST, OPENSEARCH_PORT, OPENSEARCH_SCHEME, indexName ),
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
		assumeTrue( openSearchAvailable, "OpenSearch not available at " + OPENSEARCH_HOST + ":" + OPENSEARCH_PORT );

		var indexName = "bx_ai_test_byid_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			memory = new OpenSearchVectorMemory( createUUID(), "test_byid" );
			memory.configure({
				host: "%s",
				port: %d,
				scheme: "%s",
				index: "%s",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Add document with explicit ID
			memory.addWithId(
				id: "doc123",
				text: "BoxLang vector memory with OpenSearch",
				metadata: { category: "documentation", version: 1 }
			);

			// Small delay for indexing
			sleep( 1000 );

			// Retrieve by ID
			doc = memory.getById( "doc123" );
			hasDoc = !doc.isEmpty();
			docId = hasDoc ? doc.id : "";
			docText = hasDoc ? doc.text : "";

			// Clean up
			memory.clear();
			"""
			.formatted( OPENSEARCH_HOST, OPENSEARCH_PORT, OPENSEARCH_SCHEME, indexName ),
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
		assumeTrue( openSearchAvailable, "OpenSearch not available at " + OPENSEARCH_HOST + ":" + OPENSEARCH_PORT );

		var indexName = "bx_ai_test_remove_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			memory = new OpenSearchVectorMemory( createUUID(), "test_remove" );
			memory.configure({
				host: "%s",
				port: %d,
				scheme: "%s",
				index: "%s",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Add documents
			memory.addWithId( id: "remove1", text: "Document to remove" );
			memory.addWithId( id: "remove2", text: "Document to keep" );

			// Small delay for indexing
			sleep( 1000 );

			// Verify both exist
			countBefore = memory.getAll().len();

			// Remove one document
			removed = memory.remove( "remove1" );

			// Small delay for deletion
			sleep( 1000 );

			// Verify count decreased
			countAfter = memory.getAll().len();

			// Clean up
			memory.clear();
			"""
			.formatted( OPENSEARCH_HOST, OPENSEARCH_PORT, OPENSEARCH_SCHEME, indexName ),
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

	@DisplayName( "Test batch seed operation" )
	@Test
	public void testBatchSeed() throws Exception {
		assumeTrue( openSearchAvailable, "OpenSearch not available at " + OPENSEARCH_HOST + ":" + OPENSEARCH_PORT );

		var indexName = "bx_ai_test_seed_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			memory = new OpenSearchVectorMemory( key: createUUID(), collection: "test_seed" );
			memory.configure({
				host: "%s",
				port: %d,
				scheme: "%s",
				index: "%s",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Seed multiple documents at once
			result = memory.seed([
				"BoxLang is a modern dynamic JVM language",
				"OpenSearch with k-NN enables semantic search",
				{ text: "Vector databases store embeddings", metadata: { type: "concept" } }
			]);

			addedCount = result.added;
			failedCount = result.failed;

			// Small delay for indexing
			sleep( 1000 );

			// Verify all were added
			allDocs = memory.getAll();
			totalCount = allDocs.len();

			// Clean up
			memory.clear();
			"""
			.formatted( OPENSEARCH_HOST, OPENSEARCH_PORT, OPENSEARCH_SCHEME, indexName ),
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

	@DisplayName( "Test multi-tenant isolation with userId and conversationId" )
	@Test
	public void testMultiTenantIsolation() throws Exception {
		assumeTrue( openSearchAvailable, "OpenSearch not available at " + OPENSEARCH_HOST + ":" + OPENSEARCH_PORT );

		var indexName = "bx_ai_test_tenant_" + System.currentTimeMillis();

		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.memory.vector.OpenSearchVectorMemory;

			// Create memory for user alice
			memoryAlice = new OpenSearchVectorMemory(
				key: createUUID(),
				userId: "alice",
				conversationId: "chat1",
				collection: "tenant_test"
			);
			memoryAlice.configure({
				host: "%s",
				port: %d,
				scheme: "%s",
				index: "%s",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Create memory for user bob (same index but different tenant)
			memoryBob = new OpenSearchVectorMemory(
				key: createUUID(),
				userId: "bob",
				conversationId: "chat1",
				collection: "tenant_test"
			);
			memoryBob.configure({
				host: "%s",
				port: %d,
				scheme: "%s",
				index: "%s",
				embeddingProvider: "openai",
				embeddingModel: "text-embedding-3-small"
			});

			// Add documents to each memory
			memoryAlice.add( "Alice's document about OpenSearch" );
			memoryBob.add( "Bob's document about databases" );

			// Small delay for indexing
			sleep( 1000 );

			// Each user should only see their own documents
			aliceDocs = memoryAlice.getAll();
			bobDocs = memoryBob.getAll();

			aliceCount = aliceDocs.len();
			bobCount = bobDocs.len();

			// Clean up
			memoryAlice.clear();
			memoryBob.clear();
			"""
			.formatted(
				OPENSEARCH_HOST, OPENSEARCH_PORT, OPENSEARCH_SCHEME, indexName,
				OPENSEARCH_HOST, OPENSEARCH_PORT, OPENSEARCH_SCHEME, indexName
			),
			context
		);
		// @formatter:on

		var	aliceCount	= variables.getAsInteger( Key.of( "aliceCount" ) );
		var	bobCount	= variables.getAsInteger( Key.of( "bobCount" ) );

		// Each user should only see their own documents (tenant isolation)
		assertThat( aliceCount ).isEqualTo( 1 );
		assertThat( bobCount ).isEqualTo( 1 );
	}

}
