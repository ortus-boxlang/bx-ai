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

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "CassandraVectorMemory integration tests" )
public class CassandraVectorMemoryIntegrationTest extends BaseIntegrationTest {

	private String datasource;
	private String contactPoints;
	private String keyspace;
	private String table;
	private String collection;

	@BeforeEach
	void setupIntegration() {
		assumeTrue( "true".equalsIgnoreCase( System.getenv( "RUN_VECTOR_DB_TESTS" ) ),
		    "Skipping Cassandra integration tests: set RUN_VECTOR_DB_TESTS=true to enable." );

		datasource = System.getenv( "CASSANDRA_TEST_DATASOURCE" );
		contactPoints = System.getenv( "CASSANDRA_TEST_CONTACT_POINTS" );

		assumeTrue( datasource != null && !datasource.isBlank(),
		    "Skipping Cassandra integration tests: CASSANDRA_TEST_DATASOURCE is not set." );
		assumeTrue( contactPoints != null && !contactPoints.isBlank(),
		    "Skipping Cassandra integration tests: CASSANDRA_TEST_CONTACT_POINTS is not set." );

		String runId = UUID.randomUUID().toString().replace( "-", "" ).substring( 0, 12 ).toLowerCase();
		keyspace = "bxai_it_" + runId;
		table = "vectors_" + runId;
		collection = "collection_" + runId;
	}

	@AfterEach
	void cleanup() {
		if ( datasource == null || datasource.isBlank() || keyspace == null || keyspace.isBlank() ) {
			return;
		}

		try {
			runtime.executeSource(
			    """
			    try {
			        queryExecute(
			            "DROP TABLE IF EXISTS #keyspace#.#table#",
			            {},
			            { datasource: "%s" }
			        )
			    } catch ( any e ) {}

			    try {
			        queryExecute(
			            "DROP KEYSPACE IF EXISTS #keyspace#",
			            {},
			            { datasource: "%s" }
			        )
			    } catch ( any e ) {}
			    """.formatted( datasource, datasource ),
			    context );
		} catch ( Exception e ) {
			// best effort cleanup only
		}
	}

	@Test
	@DisplayName( "Creates temporary schema, stores vectors, searches, and cleans up" )
	void testCassandraEndToEnd() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "cassandra", key: createUUID(), config: {
		        datasource: "%s",
		        contactPoints: "%s",
		        keyspace: "%s",
		        table: "%s",
		        collection: "%s",
		        dimensions: 3,
		        autoCreateSchema: true,
		        createKeyspaceIfMissing: true
		    } )

		    memory.storeDocument( "doc-1", "BoxLang language", [0.91, 0.1, 0.2], { topic: "lang" } )
		    memory.storeDocument( "doc-2", "Cassandra database", [0.1, 0.85, 0.2], { topic: "db" } )

		    allDocs = memory.getAllDocuments()
		    result = memory.searchByVector( [0.9, 0.12, 0.15], 5, { topic: "lang" } )

		    testResult = {
		        docCount: allDocs.len(),
		        resultCount: result.len(),
		        firstHasTopic: result.len() > 0 && result[ 1 ].metadata.keyExists( "topic" ),
		        firstTopic: result.len() > 0 ? result[ 1 ].metadata.topic : ""
		    }
		    """.formatted( datasource, contactPoints, keyspace, table, collection ),
		    context );

		IStruct testResult = variables.getAsStruct( Key.of( "testResult" ) );
		assertThat( testResult.getAsInteger( Key.of( "docCount" ) ) ).isEqualTo( 2 );
		assertThat( testResult.getAsInteger( Key.of( "resultCount" ) ) ).isAtLeast( 1 );
		assertThat( testResult.getAsBoolean( Key.of( "firstHasTopic" ) ) ).isTrue();
		assertThat( testResult.getAsString( Key.of( "firstTopic" ) ) ).isEqualTo( "lang" );
	}
}
