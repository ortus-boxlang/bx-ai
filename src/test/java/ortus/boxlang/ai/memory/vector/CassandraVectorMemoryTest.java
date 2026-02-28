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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@DisplayName( "CassandraVectorMemory configuration tests" )
public class CassandraVectorMemoryTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Throws when datasource is missing" )
	void testDatasourceIsRequired() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    aiMemory( memory: "cassandra", config: {
			        contactPoints: "127.0.0.1:9042",
			        keyspace: "bx_ai",
			        autoCreateSchema: false
			    } )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Creates Cassandra memory when required config is provided" )
	void testCreateWithExplicitConfig() {
		runtime.executeSource(
		    """
		    memory = aiMemory( memory: "cassandra", config: {
		        datasource: "test_cassandra",
		        contactPoints: "127.0.0.1:9042",
		        keyspace: "bx_ai",
		        table: "bx_ai_vectors_test",
		        dimensions: 8,
		        autoCreateSchema: false
		    } )

		    result = {
		        type: memory.getName(),
		        collection: memory.getCollection()
		    }
		    """,
		    context
		);

		IStruct resultData = variables.getAsStruct( result );
		assertThat( resultData.getAsString( Key.of( "type" ) ) ).isEqualTo( "CassandraVectorMemory" );
		assertThat( resultData.getAsString( Key.of( "collection" ) ) ).isNotEmpty();
	}

	@Test
	@DisplayName( "Reads config defaults from CASSANDRA_* environment variables" )
	void testEnvironmentVariableFallbacks() {
		System.setProperty( "CASSANDRA_DATASOURCE", "env_cassandra" );
		System.setProperty( "CASSANDRA_CONTACT_POINTS", "localhost:9042" );
		System.setProperty( "CASSANDRA_KEYSPACE", "env_keyspace" );
		System.setProperty( "CASSANDRA_TABLE", "env_table" );

		try {
			runtime.executeSource(
		    """
		    memory = aiMemory( memory: "cassandra", config: {
		        autoCreateSchema: false,
		        dimensions: 8
		    } )

		    cfg = memory.getConfig()
		    result = {
		        datasource: cfg.datasource,
		        contactPoints: cfg.contactPoints,
		        keyspace: cfg.keyspace,
		        table: cfg.table
		    }
		    """,
		    context
		);

		IStruct resultData = variables.getAsStruct( result );
		assertThat( resultData.getAsString( Key.of( "datasource" ) ) ).isEqualTo( "env_cassandra" );
		assertThat( resultData.getAsString( Key.of( "contactPoints" ) ) ).isEqualTo( "localhost:9042" );
		assertThat( resultData.getAsString( Key.of( "keyspace" ) ) ).isEqualTo( "env_keyspace" );
		assertThat( resultData.getAsString( Key.of( "table" ) ) ).isEqualTo( "env_table" );
		} finally {
			System.clearProperty( "CASSANDRA_DATASOURCE" );
			System.clearProperty( "CASSANDRA_CONTACT_POINTS" );
			System.clearProperty( "CASSANDRA_KEYSPACE" );
			System.clearProperty( "CASSANDRA_TABLE" );
		}
	}
}
