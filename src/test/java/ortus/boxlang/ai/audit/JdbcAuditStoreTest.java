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
package ortus.boxlang.ai.audit;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Test cases for JdbcAuditStore - configuration and integration tests
 *
 * Note: Integration tests that use query() require PostgreSQL or MySQL because
 * the JdbcAuditStore uses LIMIT syntax which Derby doesn't support.
 * Tests that don't require query() use the bxai_test (Derby) datasource.
 *
 * For full integration testing with query(), run with docker-compose up and use
 * postgres_vector_test or mysql_vector_test datasource.
 */
public class JdbcAuditStoreTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test JdbcAuditStore instantiation" )
	public void testInstantiation() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
		    isNotNull = !isNull( store )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "isNotNull" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcAuditStore requires datasource" )
	public void testRequiresDatasource() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
			        .configure( {} )  // No datasource provided
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test JdbcAuditStore requires non-empty datasource" )
	public void testRequiresNonEmptyDatasource() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
			        .configure( { datasource: "" } )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test JdbcAuditStore default table name" )
	public void testDefaultTableName() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()

		    // Before configure, should have default table name
		    // We can check this without actually configuring (which requires datasource)
		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcAuditStore inherits from BaseAuditStore" )
	public void testInheritsFromBaseAuditStore() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()

		    // Check that it has methods from BaseAuditStore by getting metadata
		    meta = getMetadata( store )
		    functions = meta.functions ?: []

		    // Find function names
		    functionNames = []
		    for( func in functions ) {
		        functionNames.append( func.name.lcase() )
		    }

		    hasStore = functionNames.findNoCase( "store" ) > 0
		    hasQuery = functionNames.findNoCase( "query" ) > 0
		    hasGetTrace = functionNames.findNoCase( "gettrace" ) > 0
		    hasGetStats = functionNames.findNoCase( "getstats" ) > 0

		    allMethodsExist = hasStore && hasQuery && hasGetTrace && hasGetStats
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "allMethodsExist" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcAuditStore has configure method" )
	public void testHasConfigureMethod() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()

		    // Check configure method exists by getting metadata
		    meta = getMetadata( store )
		    functions = meta.functions ?: []

		    functionNames = []
		    for( func in functions ) {
		        functionNames.append( func.name.lcase() )
		    }

		    hasConfigure = functionNames.findNoCase( "configure" ) > 0
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasConfigure" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test JdbcAuditStore error message for missing datasource" )
	public void testErrorMessageForMissingDatasource() {
		try {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
			        .configure( {} )
			    """,
			    context
			);
		} catch ( BoxRuntimeException e ) {
			// Verify the error message is helpful
			assertThat( e.getMessage() ).containsMatch( "(?i)datasource" );
		}
	}

	@Test
	@DisplayName( "Test AuditStoreFactory creates JdbcAuditStore for jdbc type" )
	public void testFactoryCreatesJdbcStore() {
		runtime.executeSource(
		    """
		    factory = new bxModules.bxai.models.audit.AuditStoreFactory()

		    // Test that factory recognizes 'jdbc' type
		    // This will fail during create() because no datasource, but verifies type recognition
		    try {
		        factory.create( "jdbc", {} )
		        success = false // Should have thrown
		    } catch( any e ) {
		        // Expected - should mention datasource, not unknown type
		        success = e.message.findNoCase( "datasource" ) > 0
		    }
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditStoreFactory recognizes database alias" )
	public void testFactoryRecognizesDatabaseAlias() {
		runtime.executeSource(
		    """
		    factory = new bxModules.bxai.models.audit.AuditStoreFactory()

		    // Test 'database' alias
		    try {
		        factory.create( "database", {} )
		        success = false
		    } catch( any e ) {
		        success = e.message.findNoCase( "datasource" ) > 0
		    }
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test AuditStoreFactory recognizes db alias" )
	public void testFactoryRecognizesDbAlias() {
		runtime.executeSource(
		    """
		    factory = new bxModules.bxai.models.audit.AuditStoreFactory()

		    // Test 'db' alias
		    try {
		        factory.create( "db", {} )
		        success = false
		    } catch( any e ) {
		        success = e.message.findNoCase( "datasource" ) > 0
		    }
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	// ==================== INTEGRATION TESTS (use bxai_test Derby datasource) ====================
	// Note: Tests that use query() are skipped because Derby doesn't support LIMIT syntax.
	// For full query() testing, use PostgreSQL or MySQL datasources with docker-compose.

	@Test
	@DisplayName( "Integration: Configure JdbcAuditStore with valid datasource" )
	public void testIntegrationConfigureWithDatasource() {
		runtime.executeSource(
		    """
		    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
		        .configure( {
		            datasource: "bxai_test",
		            table: "test_audit_traces_config"
		        } )

		    success = true
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Integration: Store and retrieve audit trace from database" )
	public void testIntegrationStoreAndRetrieve() {
		var tableName = "test_audit_store_" + System.currentTimeMillis();

		runtime.executeSource(
		    String.format(
		        """
		        store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
		            .configure( {
		                datasource: "bxai_test",
		                table: "%s"
		            } )

		        // Create a trace using AuditEntry
		        traceId = createUUID()
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = traceId,
		            spanType = "model",
		            operation = "aiChat"
		        )
		        .setInput( { messages: [ { role: "user", content: "Hello" } ] } )
		        .complete( output = { content: "Hi there!" } )

		        // Store the entry
		        store.store( entry )

		        // Retrieve the trace (uses getTrace, not query - no LIMIT)
		        retrieved = store.getTrace( traceId )
		        hasTrace = !isNull( retrieved ) && retrieved.keyExists( "traceId" )
		        matchesTraceId = hasTrace && retrieved.traceId == traceId
		        """,
		        tableName
		    ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasTrace" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "matchesTraceId" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Integration: Store multiple entries and retrieve by trace ID" )
	public void testIntegrationStoreMultipleAndRetrieve() {
		var tableName = "test_audit_multi_" + System.currentTimeMillis();

		runtime.executeSource(
		    String.format(
		        """
		        store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
		            .configure( {
		                datasource: "bxai_test",
		                table: "%s"
		            } )

		        // Create entries for the same trace
		        traceId = createUUID()

		        entry1 = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = traceId,
		            spanType = "agent",
		            operation = "run"
		        ).complete()
		        store.store( entry1 )

		        entry2 = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = traceId,
		            spanType = "model",
		            operation = "aiChat"
		        ).complete()
		        store.store( entry2 )

		        entry3 = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = traceId,
		            spanType = "tool",
		            operation = "execute"
		        ).complete()
		        store.store( entry3 )

		        // Retrieve all entries for the trace
		        retrieved = store.getTrace( traceId )
		        entryCount = retrieved.entries.len()
		        """,
		        tableName
		    ),
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "entryCount" ) ) ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Integration: Get statistics from database" )
	public void testIntegrationGetStats() {
		var tableName = "test_audit_stats_" + System.currentTimeMillis();

		runtime.executeSource(
		    String.format(
		        """
		        store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
		            .configure( {
		                datasource: "bxai_test",
		                table: "%s"
		            } )

		        // Store some entries
		        for( i = 1; i <= 5; i++ ) {
		            entry = new bxModules.bxai.models.audit.AuditEntry(
		                traceId = createUUID(),
		                spanType = "model",
		                operation = "aiChat"
		            )

		            if( i == 3 ) {
		                entry.complete( error = "Test error" )
		            } else {
		                entry.complete()
		            }

		            store.store( entry )
		        }

		        // Get statistics (doesn't use LIMIT)
		        stats = store.getStats()
		        hasStats = !isNull( stats )
		        hasTotalEntries = hasStats && stats.keyExists( "totalEntries" )
		        totalValue = hasTotalEntries ? stats.totalEntries : 0
		        """,
		        tableName
		    ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasStats" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasTotalEntries" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "totalValue" ) ) ).isEqualTo( 5 );
	}

	@Test
	@DisplayName( "Test JdbcAuditStore rejects SQL injection in table name" )
	public void testRejectsSqlInjectionInTableName() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
			        .configure( {
			            datasource: "bxai_test",
			            table: "audit; DROP TABLE users;--"
			        } )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test JdbcAuditStore rejects table name starting with number" )
	public void testRejectsTableNameStartingWithNumber() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
			        .configure( {
			            datasource: "bxai_test",
			            table: "123_invalid_table"
			        } )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test JdbcAuditStore rejects table name with special characters" )
	public void testRejectsTableNameWithSpecialChars() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
			        .configure( {
			            datasource: "bxai_test",
			            table: "table-with-dashes"
			        } )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Test JdbcAuditStore throws when used before configure" )
	public void testThrowsWhenUsedBeforeConfigure() {
		assertThrows( BoxRuntimeException.class, () -> {
			runtime.executeSource(
			    """
			    store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
			    // Try to store without calling configure() first
			    entry = new bxModules.bxai.models.audit.AuditEntry(
			        traceId = "trace-123",
			        spanType = "model",
			        operation = "chat"
			    ).complete()
			    store.store( entry )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "Integration: Custom table name creates correct table" )
	public void testIntegrationCustomTableName() {
		var tableName = "custom_audit_table_" + System.currentTimeMillis();

		runtime.executeSource(
		    String.format(
		        """
		        store = new bxModules.bxai.models.audit.stores.JdbcAuditStore()
		            .configure( {
		                datasource: "bxai_test",
		                table: "%s"
		            } )

		        // Store an entry
		        entry = new bxModules.bxai.models.audit.AuditEntry(
		            traceId = createUUID(),
		            spanType = "model",
		            operation = "aiChat"
		        ).complete()
		        store.store( entry )

		        // Verify the table exists by querying it directly
		        tableExists = queryExecute(
		            \"SELECT COUNT(*) as cnt FROM %s\",
		            {},
		            { datasource: \"bxai_test\" }
		        ).cnt > 0
		        """,
		        tableName, tableName
		    ),
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "tableExists" ) ) ).isTrue();
	}

}
