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

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;

/**
 * Drives bx-ai's MCP server on STDIO (StdioServerHarness, a child JVM) with the messages a real MCP client sends,
 * and checks what comes back on stdout and stderr.
 */
public class mcpStdioServerTest extends BaseIntegrationTest {

	private static String			stdoutText;
	private static List<String>		stdoutLines;
	private static String			stderrText;

	// @formatter:off
	private static final String[] MESSAGES = {
		"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}",
		"{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}",
		"{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}",
		"{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{\"requestId\":2}}",
		"{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"echo\",\"arguments\":{\"message\":\"hi\"}}}",
		"{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"setTheme\",\"arguments\":{\"theme\":\"ocean\"}}}"
	};
	// @formatter:on

	@BeforeAll
	public static void runServer() throws Exception {
		String						java	= Paths.get( System.getProperty( "java.home" ), "bin", "java" ).toString();
		Process						server	= new ProcessBuilder( java, "-cp", System.getProperty( "java.class.path" ), StdioServerHarness.class.getName() )
		    .start();

		CompletableFuture<String>	stdout	= CompletableFuture
		    .supplyAsync( () -> readAll( server.getInputStream() ) );
		CompletableFuture<String>	stderr	= CompletableFuture
		    .supplyAsync( () -> readAll( server.getErrorStream() ) );

		try ( OutputStream stdin = server.getOutputStream() ) {
			for ( String message : MESSAGES ) {
				stdin.write( ( message + "\n" ).getBytes( StandardCharsets.UTF_8 ) );
				stdin.flush();
			}
		}

		assertThat( server.waitFor( 120, TimeUnit.SECONDS ) ).isTrue();
		// The child JVM boots the runtime and loads the module before the server starts, what that prints is not the server's.
		// Everything from the first message on is written while the server runs.
		stdoutText	= stdout.get( 10, TimeUnit.SECONDS );
		stdoutLines	= new ArrayList<>();
		for ( String line : stdoutText.split( "\n" ) ) {
			if ( !stdoutLines.isEmpty() || line.startsWith( "{" ) ) {
				stdoutLines.add( line.trim() );
			}
		}
		stdoutLines.removeIf( String::isBlank );
		stderrText = stderr.get( 10, TimeUnit.SECONDS );
	}

	private static String readAll( java.io.InputStream stream ) {
		try {
			return new String( stream.readAllBytes(), StandardCharsets.UTF_8 );
		} catch ( java.io.IOException e ) {
			throw new RuntimeException( e );
		}
	}

	@Test
	@DisplayName( "stdout carries only JSON-RPC messages, the logs go to stderr" )
	public void testStdoutCarriesOnlyMessages() {
		assertThat( stdoutLines ).isNotEmpty();
		for ( String line : stdoutLines ) {
			assertThat( line ).startsWith( "{" );
			assertThat( line ).contains( "\"jsonrpc\":\"2.0\"" );
		}
		// The server's own log lines are on stderr, never on stdout
		assertThat( stdoutText ).doesNotContain( "STDIO transport" );
		assertThat( stdoutText ).doesNotContain( "STDIN closed" );
		assertThat( stderrText ).contains( "Starting STDIO transport" );
		assertThat( stderrText ).contains( "STDIO transport stopped" );
	}

	@Test
	@DisplayName( "Notifications get no response: one answer per request, in order" )
	public void testNotificationsGetNoResponse() {
		assertThat( stdoutLines ).hasSize( 4 );
		for ( int index = 0; index < 4; index++ ) {
			assertThat( stdoutLines.get( index ) ).doesNotContain( "Method not found" );
			assertThat( stdoutLines.get( index ) ).contains( "\"id\":" + ( index + 1 ) );
		}
	}

	@Test
	@DisplayName( "tools/list gives @mcpTool tools their parameters" )
	public void testAnnotatedToolParameters() {
		String toolsList = stdoutLines.get( 1 );
		assertThat( toolsList ).contains( "\"name\":\"setTheme\"" );
		assertThat( toolsList ).contains( "\"required\":[\"theme\"]" );
		assertThat( toolsList ).contains( "\"description\":\"The theme name\"" );
		assertThat( stdoutLines.get( 3 ) ).contains( "theme is now ocean" );
	}

}
