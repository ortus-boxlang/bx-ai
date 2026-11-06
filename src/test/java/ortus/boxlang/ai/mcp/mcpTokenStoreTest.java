/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.ai.mcp;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * The three stores that ship for MCP OAuth state (cache, file and JDBC) keep to the same contract: get, set (with an optional lifetime)
 * and remove, with an empty struct for what is not there.
 */
public class mcpTokenStoreTest extends BaseIntegrationTest {

	private static final String PACKAGE = "bxModules.bxai.models.mcp.oauth.";

	/** Run the same checks against a store made by the given BoxLang expression */
	private void assertContract( String makeStore ) {
		// @formatter:off
		runtime.executeSource(
			"""
				store = %s
				key   = "token:https://mcp.example.com/mcp|alice"

				missing = store.get( key )
				store.set( key, { accessToken: "a1", expiresAt: 1790000000000, nested: { scopes: [ "mail.read", "mail.send" ] } } )
				found = store.get( key )

				// a save replaces what was there
				store.set( key, { accessToken: "a2" } )
				replaced = store.get( key )

				// keys do not touch each other
				store.set( "token:https://mcp.example.com/mcp|bob", { accessToken: "b1" } )
				aliceAgain = store.get( key ).accessToken
				bob        = store.get( "token:https://mcp.example.com/mcp|bob" ).accessToken

				store.remove( key )
				afterRemove = store.get( key )
				// removing what is not there is not an error
				store.remove( key )
				store.remove( "token:https://mcp.example.com/mcp|bob" )
			""".formatted( makeStore ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsStruct( Key.of( "missing" ) ) ).isEmpty();
		var found = variables.getAsStruct( Key.of( "found" ) );
		assertThat( found.getAsString( Key.of( "accessToken" ) ) ).isEqualTo( "a1" );
		assertThat( found.get( Key.of( "expiresAt" ) ).toString() ).isEqualTo( "1790000000000" );
		assertThat( found.getAsStruct( Key.of( "nested" ) ).getAsArray( Key.of( "scopes" ) ) ).hasSize( 2 );
		assertThat( variables.getAsStruct( Key.of( "replaced" ) ).getAsString( Key.of( "accessToken" ) ) ).isEqualTo( "a2" );
		assertThat( variables.getAsString( Key.of( "aliceAgain" ) ) ).isEqualTo( "a2" );
		assertThat( variables.getAsString( Key.of( "bob" ) ) ).isEqualTo( "b1" );
		assertThat( variables.getAsStruct( Key.of( "afterRemove" ) ) ).isEmpty();
	}

	/** A record saved with a lifetime is gone once the lifetime has passed */
	private void assertLifetime( String makeStore ) {
		// @formatter:off
		runtime.executeSource(
			"""
				store = %s
				store.set( "flow:short", { subject: "alice" }, 1 )
				store.set( "flow:long", { subject: "bob" }, 600 )
				store.set( "flow:never", { subject: "carol" } )
				before = store.get( "flow:short" ).keyExists( "subject" )
				sleep( 2200 )
				shortAfter = store.get( "flow:short" )
				longAfter  = store.get( "flow:long" ).keyExists( "subject" )
				neverAfter = store.get( "flow:never" ).keyExists( "subject" )
			""".formatted( makeStore ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "before" ) ) ).isEqualTo( true );
		assertThat( variables.getAsStruct( Key.of( "shortAfter" ) ) ).isEmpty();
		assertThat( variables.get( Key.of( "longAfter" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "neverAfter" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "CacheMCPTokenStore keeps to the contract" )
	public void testCacheStore() {
		assertContract( "new " + PACKAGE + "CacheMCPTokenStore( \"default\" )" );
	}

	@Test
	@DisplayName( "CacheMCPTokenStore forgets a record after its lifetime" )
	public void testCacheStoreLifetime() {
		assertLifetime( "new " + PACKAGE + "CacheMCPTokenStore( \"default\" )" );
	}

	@Test
	@DisplayName( "FileMCPTokenStore keeps to the contract" )
	public void testFileStore( @TempDir Path folder ) {
		assertContract( "new " + PACKAGE + "FileMCPTokenStore( \"" + folder.toString().replace( "\\", "/" ) + "\" )" );
	}

	@Test
	@DisplayName( "FileMCPTokenStore forgets a record after its lifetime" )
	public void testFileStoreLifetime( @TempDir Path folder ) {
		assertLifetime( "new " + PACKAGE + "FileMCPTokenStore( \"" + folder.toString().replace( "\\", "/" ) + "\" )" );
	}

	@Test
	@DisplayName( "FileMCPTokenStore makes the folder, treats a damaged file as no record and leaves no half written files" )
	public void testFileStoreDetails( @TempDir Path folder ) throws Exception {
		String path = folder.resolve( "tokens/deeper" ).toString().replace( "\\", "/" );
		// @formatter:off
		runtime.executeSource(
			"""
				store = new %sFileMCPTokenStore( "%s" )
				store.set( "token:one", { accessToken: "x" } )
			""".formatted( PACKAGE, path ),
			context
		);
		// @formatter:on
		Path dir = folder.resolve( "tokens/deeper" );
		assertThat( Files.isDirectory( dir ) ).isTrue();
		try ( var files = Files.list( dir ) ) {
			var names = files.map( f -> f.getFileName().toString() ).toList();
			assertThat( names ).hasSize( 1 );
			assertThat( names.get( 0 ) ).endsWith( ".json" );
			// the key is hashed, so a key with slashes and colons cannot escape the folder or break the file name
			assertThat( names.get( 0 ) ).doesNotContain( "token" );
			if ( !System.getProperty( "os.name" ).toLowerCase().contains( "win" ) ) {
				assertThat( Files.getPosixFilePermissions( dir.resolve( names.get( 0 ) ) ) ).containsExactly( PosixFilePermission.OWNER_READ,
				    PosixFilePermission.OWNER_WRITE );
			}
			Files.writeString( dir.resolve( names.get( 0 ) ), "{ this is not json" );
		}
		// @formatter:off
		runtime.executeSource( "damaged = store.get( 'token:one' )", context );
		// @formatter:on
		assertThat( variables.getAsStruct( Key.of( "damaged" ) ) ).isEmpty();
	}

	@Test
	@DisplayName( "FileMCPTokenStore needs a folder" )
	public void testFileStoreNeedsAFolder() {
		// @formatter:off
		runtime.executeSource(
			"""
				try { new %sFileMCPTokenStore( "" ) } catch ( any e ) { errorType = e.type }
			""".formatted( PACKAGE ),
			context
		);
		// @formatter:on
		assertThat( variables.getAsString( Key.of( "errorType" ) ) ).isEqualTo( "FileMCPTokenStore.MissingDirectory" );
	}

	private String jdbcStore() {
		return "new " + PACKAGE + "JdbcMCPTokenStore( \"bxai_test\", \"mcp_oauth_" + Long.toHexString( System.nanoTime() ) + "\" )";
	}

	@Test
	@DisplayName( "JdbcMCPTokenStore keeps to the contract" )
	public void testJdbcStore() {
		assertContract( jdbcStore() );
	}

	@Test
	@DisplayName( "JdbcMCPTokenStore forgets a record after its lifetime" )
	public void testJdbcStoreLifetime() {
		assertLifetime( jdbcStore() );
	}

	@Test
	@DisplayName( "JdbcMCPTokenStore creates its table once, needs a datasource, refuses an odd table name and a record that does not fit" )
	public void testJdbcStoreDetails() {
		// @formatter:off
		runtime.executeSource(
			"""
				table = "mcp_oauth_again"
				first = new %sJdbcMCPTokenStore( "bxai_test", table )
				first.set( "k", { v: 1 } )
				// a second store on the same table finds what the first saved and does not try to create the table again
				second = new %sJdbcMCPTokenStore( "bxai_test", table )
				shared = second.get( "k" ).v

				try { new %sJdbcMCPTokenStore( "" ) } catch ( any e ) { noSource = e.type }
				try { new %sJdbcMCPTokenStore( "bxai_test", "bad; DROP TABLE x" ) } catch ( any e ) { badTable = e.type }
				try { first.set( "big", { text: repeatString( "x", 9000 ) } ) } catch ( any e ) { tooBig = e.type }
			""".formatted( PACKAGE, PACKAGE, PACKAGE, PACKAGE ),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "shared" ) ).toString() ).isEqualTo( "1" );
		assertThat( variables.getAsString( Key.of( "noSource" ) ) ).isEqualTo( "JdbcMCPTokenStore.MissingDatasource" );
		assertThat( variables.getAsString( Key.of( "badTable" ) ) ).isEqualTo( "JdbcMCPTokenStore.InvalidTable" );
		assertThat( variables.getAsString( Key.of( "tooBig" ) ) ).isEqualTo( "JdbcMCPTokenStore.TooLarge" );
	}

	@Test
	@DisplayName( "A store that is a class path is built with the store options, and a store instance is used as it is" )
	public void testCustomStores() {
		// @formatter:off
		runtime.executeSource(
			"""
				// an instance
				mine = new %sCacheMCPTokenStore( "default" )
				viaInstance = MCP( "https://mcp.example.com/mcp" ).withOAuth( { store: mine, redirectUri: "https://app.example.com/cb" } )
				// a class path, with the options handed to its init
				viaClass = MCP( "https://mcp.example.com/mcp" ).withOAuth( {
					store       : "%sCacheMCPTokenStore",
					storeOptions: { cacheName: "default" },
					redirectUri : "https://app.example.com/cb"
				} )
				// the three names
				viaFile = MCP( "https://mcp.example.com/mcp" ).withOAuth( { store: "file", storeOptions: { path: "%s" }, redirectUri: "https://app.example.com/cb" } )
				viaJdbc = MCP( "https://mcp.example.com/mcp" ).withOAuth( { store: "jdbc", storeOptions: { datasource: "bxai_test" }, redirectUri: "https://app.example.com/cb" } )
				try { MCP( "https://mcp.example.com/mcp" ).withOAuth( { store: "nope" } ) } catch ( any e ) { unknownType = e.type }
				built = [ viaInstance.isAuthorized(), viaClass.isAuthorized(), viaFile.isAuthorized(), viaJdbc.isAuthorized() ]
			""".formatted( PACKAGE, PACKAGE, System.getProperty( "java.io.tmpdir" ).replace( "\\", "/" ) + "/bxai-mcp-oauth-" + System.nanoTime() ),
			context
		);
		// @formatter:on

		assertThat( variables.getAsArray( Key.of( "built" ) ).toString() ).isEqualTo( "[false, false, false, false]" );
		assertThat( variables.getAsString( Key.of( "unknownType" ) ) ).isEqualTo( "InvalidArgument" );
	}

}
