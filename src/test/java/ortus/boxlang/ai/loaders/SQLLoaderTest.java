package ortus.boxlang.ai.loaders;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "SQLLoader Tests" )
@Timeout( value = 30, unit = TimeUnit.SECONDS )
public class SQLLoaderTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setupDatabase() {
		// Create test table and populate with test data once for all tests
		// Derby in-memory database will be cleaned up automatically after test run
		// @formatter:off
		runtime.executeSource(
		    """
				// Drop table if it exists from previous run
				try {
					queryExecute("DROP TABLE books", {}, { datasource: "bxai_test" });
				} catch(any e) {
					// Ignore if table doesn't exist
				}

				// Create test table
				queryExecute(
					"CREATE TABLE books (
						id INTEGER NOT NULL PRIMARY KEY,
						title VARCHAR(200),
						author VARCHAR(100),
						pub_year INTEGER,
						description VARCHAR(500),
						category VARCHAR(50)
					)",
					{},
					{ datasource: "bxai_test" }
				);

				// Insert test data
				queryExecute(
					"INSERT INTO books (id, title, author, pub_year, description, category) VALUES
						(1, 'The Great Adventure', 'Jane Smith', 2020, 'An exciting tale of discovery', 'fiction'),
						(2, 'Quantum Computing', 'Dr. John Doe', 2022, 'A guide to quantum principles', 'science'),
						(3, 'Ancient Civilizations', 'Prof. Maria Garcia', 2019, 'Exploring great civilizations', 'history')",
					{},
					{ datasource: "bxai_test" }
				);
		    """
		);
		// @formatter:on
	}

	@DisplayName( "SQLLoader can load rows as separate documents" )
	@Test
	public void testSQLLoaderRowsAsDocuments() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.SQLLoader;
				loader = new SQLLoader(
					source: "SELECT * FROM books ORDER BY id",
					config: {
						datasource: "bxai_test",
						rowsAsDocuments: true,
						contentColumns: ["title", "author", "description"]
					}
				);
				rawDocs = loader.load();
				result = rawDocs.map( d => d.toStruct() );
		    """,
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs.size() ).isEqualTo( 3 );

		// Check first document
		IStruct	firstDoc	= ( IStruct ) docs.get( 0 );
		String	content		= firstDoc.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "The Great Adventure" );
		assertThat( content ).contains( "Jane Smith" );
		assertThat( content ).contains( "exciting tale" );
	}

	@DisplayName( "SQLLoader can load entire query as single document" )
	@Test
	public void testSQLLoaderSingleDocument() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.SQLLoader;
				loader = new SQLLoader(
					source: "SELECT title, author, category FROM books ORDER BY pub_year DESC",
					config: {
						datasource: "bxai_test",
						rowsAsDocuments: false
					}
				);
				rawDocs = loader.load();
				result = rawDocs.map( d => d.toStruct() );
		    """,
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs.size() ).isEqualTo( 1 );

		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );

		// Should contain all book data in tabular format
		assertThat( content ).contains( "Quantum Computing" );
		assertThat( content ).contains( "The Great Adventure" );
		assertThat( content ).contains( "Ancient Civilizations" );
	}

	@DisplayName( "SQLLoader can use parameterized queries" )
	@Test
	public void testSQLLoaderParameterizedQuery() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.SQLLoader;
				loader = new SQLLoader(
					source: "SELECT * FROM books WHERE category = :category",
					config: {
						datasource: "bxai_test",
						params: { category: "science" },
						rowsAsDocuments: true
					}
				);
				rawDocs = loader.load();
				result = rawDocs.map( d => d.toStruct() );
		    """,
		    context
		);
		// @formatter:on

		Array docs = ( Array ) variables.get( "result" );
		assertThat( docs.size() ).isEqualTo( 1 );

		IStruct	doc		= ( IStruct ) docs.get( 0 );
		String	content	= doc.getAsString( Key.of( "content" ) );
		assertThat( content ).contains( "Quantum Computing" );
		assertThat( content ).contains( "Dr. John Doe" );
	}

	@DisplayName( "SQLLoader includes metadata when configured" )
	@Test
	public void testSQLLoaderMetadata() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.SQLLoader;
				loader = new SQLLoader(
					source: "SELECT * FROM books WHERE id = 1",
					config: {
						datasource: "bxai_test",
						includeMetadata: true,
						metadataColumns: ["category", "pub_year"],
						idColumn: "id"
					}
				);
				docs = loader.load();
				metadata = docs[1].getMetadata();
		    """,
		    context
		);
		// @formatter:on

		IStruct metadata = ( IStruct ) variables.get( "metadata" );
		assertThat( metadata ).isNotNull();
		assertThat( metadata.containsKey( Key.of( "category" ) ) ).isTrue();
		assertThat( metadata.containsKey( Key.of( "pub_year" ) ) ).isTrue();
		assertThat( metadata.getAsString( Key.of( "category" ) ) ).isEqualTo( "fiction" );
		assertThat( metadata.getAsInteger( Key.of( "pub_year" ) ) ).isEqualTo( 2020 );
	}

	@DisplayName( "SQLLoader can limit result rows" )
	@Test
	public void testSQLLoaderMaxRows() {
		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.loaders.SQLLoader;
				loader = new SQLLoader(
					source: "SELECT * FROM books ORDER BY id",
					config: {
						datasource: "bxai_test",
						maxRows: 2,
						rowsAsDocuments: true
					}
				);
				rawDocs = loader.load();
				result = rawDocs.len();
		    """,
		    context
		);
		// @formatter:on

		Integer count = ( Integer ) variables.get( "result" );
		assertThat( count ).isEqualTo( 2 );
	}

}
