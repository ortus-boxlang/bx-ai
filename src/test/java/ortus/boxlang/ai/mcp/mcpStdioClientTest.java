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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * The stdio MCP client against FakeStdioMcpServer, a small MCP server that runs as a child process. Each test picks the behavior of the server
 * through its mode: crash, slow answers, big payloads, noise on stdout and stderr, a wrong protocol version, and so on.
 */
public class mcpStdioClientTest extends BaseIntegrationTest {

	private static final String IMPORT = "import bxModules.bxai.models.mcp.MCPStdioClient;\n";

	/**
	 * A BoxLang array literal for the command that launches the fake server in a mode
	 */
	private static String command( String mode, String... extra ) {
		try {
			String			java	= Paths.get( System.getProperty( "java.home" ), "bin", "java" ).toString();
			String			classes	= Paths.get( FakeStdioMcpServer.class.getProtectionDomain().getCodeSource().getLocation().toURI() ).toString();
			StringBuilder	cmd		= new StringBuilder( "[ \"" ).append( java ).append( "\", \"-cp\", \"" ).append( classes ).append( "\", \"" )
			    .append( FakeStdioMcpServer.class.getName() ).append( "\", \"" ).append( mode ).append( "\"" );
			for ( String part : extra ) {
				cmd.append( ", \"" ).append( part ).append( "\"" );
			}
			return cmd.append( " ]" ).toString();
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	private void run( String code ) {
		runtime.executeSource( IMPORT + code, context );
	}

	private Object get( String name ) {
		return variables.get( Key.of( name ) );
	}

	@AfterEach
	public void closeEveryClient() {
		// Never leave a child JVM behind, whatever the test did
		runtime.executeSource( IMPORT + "MCPStdioClient::closeAll()", context );
	}

	// -------------------------------------------------------------------------------------------------
	// Protocol
	// -------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "The client handshakes, lists the tools and negotiates the protocol version" )
	public void testHandshakeAndListTools() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			response = client.listTools()
			ok = response.isSuccess()
			names = response.getData().tools.map( t => t.name ).toList()
			version = client.getProtocolVersion()
			label = client.getBaseURL()
			transport = client.getTransportName()
			isClient = client instanceof "bxModules.bxai.models.mcp.IMCPClient"
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "ok" ) ).isEqualTo( true );
		assertThat( get( "names" ).toString() ).isEqualTo( "echo,pid,big" );
		assertThat( get( "version" ).toString() ).isEqualTo( "2025-06-18" );
		assertThat( get( "label" ).toString() ).isEqualTo( "stdio:java" );
		assertThat( get( "transport" ).toString() ).isEqualTo( "stdio" );
		assertThat( get( "isClient" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "getCapabilities returns what the handshake got and the MCP() BIF gives the right class for a URL and a struct" )
	public void testCapabilitiesAndClassSelection() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			caps = client.getCapabilities()
			serverName = caps.getData().serverInfo.name
			stdioClass = getMetadata( client ).name
			httpClass = getMetadata( MCP( "http://127.0.0.1:1/mcp" ) ).name
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "serverName" ).toString() ).isEqualTo( "fake" );
		assertThat( get( "stdioClass" ).toString() ).endsWith( "MCPStdioClient" );
		assertThat( get( "httpClass" ).toString() ).endsWith( "MCPHttpClient" );
	}

	@Test
	@DisplayName( "A tool call round trips text with quotes, newlines and unicode untouched" )
	public void testToolCallRoundTrip() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			text = "line1" & char( 10 ) & 'say "hi"' & char( 9 ) & "ünï 日本 \\ backslash"
			response = client.send( "echo", { text: text } )
			ok = response.isSuccess()
			same = response.getData().content[ 1 ].text == text
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "ok" ) ).isEqualTo( true );
		assertThat( get( "same" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Resources and prompts work like tools" )
	public void testResourcesAndPrompts() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			resources = client.listResources().getData().resources.len()
			body = client.readResource( "fake://one" ).getData().contents[ 1 ].text
			prompts = client.listPrompts().getData().prompts.len()
			prompt = client.getPrompt( "greet", {} ).getData().messages[ 1 ].role
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "resources" ) ).isEqualTo( 1 );
		assertThat( get( "body" ).toString() ).isEqualTo( "resource body" );
		assertThat( get( "prompts" ) ).isEqualTo( 1 );
		assertThat( get( "prompt" ).toString() ).isEqualTo( "user" );
	}

	@Test
	@DisplayName( "A JSON-RPC error from the server is a failed response with its message" )
	public void testJsonRpcError() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			response = client.send( "no-such-tool", {} )
			ok = response.isSuccess()
			error = response.getError()
			stillWorks = client.listTools().isSuccess()
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "ok" ) ).isEqualTo( false );
		assertThat( get( "error" ).toString() ).contains( "Unknown tool" );
		assertThat( get( "stillWorks" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Payloads of a million characters go in and come out, on one line" )
	public void testLargePayload() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } ).withTimeout( 60000 )
			text = repeatString( "abcdefghij" & char( 10 ), 100000 )
			echoed = client.send( "echo", { text: text } ).getData().content[ 1 ].text
			echoSame = echoed == text
			bigLength = len( client.send( "big", { size: 2000000 } ).getData().content[ 1 ].text )
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "echoSame" ) ).isEqualTo( true );
		assertThat( ( ( Number ) get( "bigLength" ) ).intValue() ).isEqualTo( 2000000 );
	}

	@Test
	@DisplayName( "Many threads share one client and every caller gets its own answer" )
	public void testConcurrentCalls() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } ).withTimeout( 30000 )
			client.listTools()
			numbers = []
			for( i = 1; i <= 40; i++ ){ numbers.append( i ) }
			results = numbers.map( ( n ) => {
				return client.send( "echo", { text: "call-" & n } ).getData().content[ 1 ].text
			}, true, 8 )
			allMatch = true
			for( i = 1; i <= 40; i++ ){
				if( results[ i ] != "call-" & i ){ allMatch = false }
			}
			total = client.getSummary().totalCalls
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "allMatch" ) ).isEqualTo( true );
		assertThat( ( ( Number ) get( "total" ) ).intValue() ).isEqualTo( 41 );
	}

	@Test
	@DisplayName( "Threads that make the very first calls at the same time wait for the handshake, none reaches the server before it is initialized" )
	public void testFirstCallsWaitForHandshake() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } ).withTimeout( 30000 )
			numbers = [ 1, 2, 3, 4, 5, 6, 7, 8 ]
			// Thread 1 starts the server. The others arrive one after the other while it starts and does the handshake
			results = numbers.map( ( n ) => {
				sleep( ( n - 1 ) * 60 )
				return client.send( "echo", { text: "first-" & n } )
			}, true, 8 )
			okCount = results.filter( r => r.isSuccess() ).len()
			errors = results.filter( r => !r.isSuccess() ).map( r => r.getError() ).toList( "|" )
			client.close()
		""".formatted( command( "strict" ) ) );
		// @formatter:on

		assertThat( get( "errors" ).toString() ).isEmpty();
		assertThat( get( "okCount" ) ).isEqualTo( 8 );
	}

	@Test
	@DisplayName( "Requests from the server are handled: ping is answered and an unknown method is refused with -32601" )
	public void testServerRequests() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			ping = client.send( "pingcheck", {} ).getData().content[ 1 ].text
			unknown = client.send( "unknowncheck", {} ).getData().content[ 1 ].text
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "ping" ).toString() ).isEqualTo( "ping-answered" );
		assertThat( get( "unknown" ).toString() ).isEqualTo( "refused" );
	}

	@Test
	@DisplayName( "Round trip against bx-ai's own MCP server on stdio, run as a child BoxLang process" )
	public void testRoundTripAgainstOwnServer() {
		String	java	= Paths.get( System.getProperty( "java.home" ), "bin", "java" ).toString();
		String	command	= "[ \"" + java + "\", \"-cp\", \"" + System.getProperty( "java.class.path" ).replace( "\\", "\\\\" ) + "\", \""
		    + StdioServerHarness.class.getName() + "\" ]";

		// @formatter:off
		run( """
			client = MCP( { command: %s, timeout: 60000 } )
			tools = client.listTools()
			toolNames = tools.getData().tools.map( t => t.name ).toList()
			version = client.getProtocolVersion()
			answer = client.send( "echo", { message: "hello server" } )
			answerOk = answer.isSuccess()
			answerText = jsonSerialize( answer.getData() )
			unknown = client.send( "no-such-tool", {} )
			unknownOk = unknown.isSuccess()
			closeStarted = getTickCount()
			client.close()
			closeMs = getTickCount() - closeStarted
		""".formatted( command ) );
		// @formatter:on

		assertThat( get( "toolNames" ).toString() ).contains( "echo" );
		assertThat( get( "version" ).toString() ).isNotEmpty();
		assertThat( get( "answerOk" ) ).isEqualTo( true );
		assertThat( get( "answerText" ).toString() ).contains( "echo: hello server" );
		assertThat( get( "unknownOk" ) ).isEqualTo( false );
		// The server leaves its loop when stdin closes, so close() does not wait for the 2 second grace period to end
		assertThat( ( ( Number ) get( "closeMs" ) ).longValue() ).isLessThan( 1500L );
	}

	@Test
	@DisplayName( "Interop with the official MCP reference server, a Node program started with npx (set MCP_STDIO_INTEROP=true)" )
	public void testInteropWithReferenceServer() {
		org.junit.jupiter.api.Assumptions.assumeTrue( "true".equals( System.getenv( "MCP_STDIO_INTEROP" ) ), "MCP_STDIO_INTEROP is not true" );

		// @formatter:off
		run( """
			client = MCP( { command: [ "npx", "-y", "@modelcontextprotocol/server-everything" ], timeout: 180000 } )
			tools = client.listTools()
			ok = tools.isSuccess()
			toolNames = ok ? tools.getData().tools.map( t => t.name ).toList() : tools.getError()
			version = client.getProtocolVersion()
			serverName = client.getCapabilities().getData().serverInfo.name ?: ""
			echoed = client.send( "echo", { message: "hello from bx-ai" } )
			echoText = echoed.isSuccess() ? jsonSerialize( echoed.getData() ) : echoed.getError()
			client.close()
			running = client.isRunning()
		""" );
		// @formatter:on

		assertThat( get( "ok" ) ).isEqualTo( true );
		assertThat( get( "toolNames" ).toString() ).contains( "echo" );
		assertThat( get( "version" ).toString() ).isNotEmpty();
		assertThat( get( "echoText" ).toString() ).contains( "hello from bx-ai" );
		assertThat( get( "running" ) ).isEqualTo( false );
	}

	// -------------------------------------------------------------------------------------------------
	// Found in the release review
	// -------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "Options that make no sense for a stdio server are refused, also the typo of a security option, instead of being ignored" )
	public void testUnknownOptionsRefused() {
		// @formatter:off
		run( """
			function typeAndMessage( action ){
				try {
					action()
					return "no error"
				} catch( any e ){
					return e.type & ": " & e.message
				}
			}
			viaModel   = typeAndMessage( () => aiModel().withMCPServer( { command: %s, token: "secret" } ) )
			viaServers = typeAndMessage( () => aiModel().withMCPServers( [ { command: %s, headers: { a: "b" } } ] ) )
			typo       = typeAndMessage( () => MCP( { command: %s, commandGaurd: ( c ) => false } ) )
			model      = aiModel().withMCPServer( { command: %s, prefix: "p" } )
			prefixed   = model.hasTool( "p__echo" )
			model.closeMCPServers()
		""".formatted( command( "ok" ), command( "ok" ), command( "ok" ), command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "viaModel" ).toString() ).startsWith( "InvalidArgument" );
		assertThat( get( "viaModel" ).toString() ).contains( "token" );
		assertThat( get( "viaServers" ).toString() ).startsWith( "InvalidArgument" );
		assertThat( get( "viaServers" ).toString() ).contains( "headers" );
		assertThat( get( "typo" ).toString() ).startsWith( "InvalidArgument" );
		assertThat( get( "typo" ).toString() ).contains( "commandGaurd" );
		assertThat( get( "prefixed" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "A first start that is slower than the timeout does not use up the restarts: the client works once the server is fast enough" )
	public void testSlowFirstStartKeepsRestartBudget() throws IOException {
		Path counter = Files.createTempFile( "mcp-stdio-counter", ".txt" );
		Files.delete( counter );
		try {
			// @formatter:off
			run( """
				client = MCP( { command: %s, timeout: 600, maxRestarts: 2 } )
				errors = []
				for( attempt = 1; attempt <= 3; attempt++ ){
					errors.append( client.listTools().getError() )
				}
				client.withTimeout( 20000 )
				fourth = client.listTools()
				fourthOk = fourth.isSuccess()
				fourthError = fourth.getError()
				joined = errors.toList( "|" )
				client.close()
			""".formatted( command( "slowthree", counter.toString() ) ) );
			// @formatter:on

			assertThat( get( "joined" ).toString() ).doesNotContain( "keeps exiting" );
			assertThat( get( "joined" ).toString() ).contains( "did not answer" );
			assertThat( get( "fourthError" ).toString() ).isEmpty();
			assertThat( get( "fourthOk" ) ).isEqualTo( true );
		} finally {
			Files.deleteIfExists( counter );
		}
	}

	@Test
	@DisplayName( "A message the client cannot make sense of does not stop it from reading: the next call still works" )
	public void testUnreadableMessageDoesNotKillTheReader() {
		// @formatter:off
		run( """
			client = MCP( { command: %s, timeout: 5000 } )
			first = client.send( "structid", {} )
			firstOk = first.isSuccess()
			started = getTickCount()
			second = client.send( "echo", { text: "still alive" } )
			elapsed = getTickCount() - started
			secondOk = second.isSuccess()
			secondError = second.getError()
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "firstOk" ) ).isEqualTo( true );
		assertThat( get( "secondError" ).toString() ).isEmpty();
		assertThat( get( "secondOk" ) ).isEqualTo( true );
	}

	@Test
	@Timeout( 90 )
	@DisplayName( "close() still returns and ends the process when a large request is stuck in the pipe of a server that does not read" )
	public void testCloseWhileWriteIsBlocked() {
		// @formatter:off
		run( """
			client = MCP( { command: %s, timeout: 60000 } )
			client.listTools()
			big = repeatString( "0123456789abcdef", 400000 )
			sender = asyncRun( () => client.send( "echo", { text: big } ) )
			sleep( 2000 )
			started = getTickCount()
			client.close()
			closeMs = getTickCount() - started
			running = client.isRunning()
			senderResult = sender.get()
			senderOk = senderResult.isSuccess()
		""".formatted( command( "noread" ) ) );
		// @formatter:on

		assertThat( get( "running" ) ).isEqualTo( false );
		assertThat( get( "senderOk" ) ).isEqualTo( false );
		assertThat( ( ( Number ) get( "closeMs" ) ).longValue() ).isLessThan( 15000L );
	}

	@Test
	@DisplayName( "close() also ends the processes the server started, they do not outlive it" )
	public void testCloseEndsGrandchildren() throws Exception {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			grandchildPid = client.send( "gcpid", {} ).getData().content[ 1 ].text
			client.close()
		""".formatted( command( "grandchild" ) ) );
		// @formatter:on

		long pid = Long.parseLong( get( "grandchildPid" ).toString() );
		try {
			long deadline = System.currentTimeMillis() + 10_000;
			while ( ProcessHandle.of( pid ).map( ProcessHandle::isAlive ).orElse( false ) && System.currentTimeMillis() < deadline ) {
				Thread.sleep( 200 );
			}
			assertThat( ProcessHandle.of( pid ).map( ProcessHandle::isAlive ).orElse( false ) ).isFalse();
		} finally {
			ProcessHandle.of( pid ).ifPresent( ProcessHandle::destroyForcibly );
		}
	}

	@Test
	@DisplayName( "close() ends the processes a launcher left behind when it exited on its own, also the ones that ignore terminate" )
	public void testCloseEndsOrphans() throws Exception {
		for ( String serverMode : new String[] { "orphan", "orphanstubborn" } ) {
			// @formatter:off
			run( """
				client = MCP( { command: %s } )
				orphanPid = client.send( "gcpid", {} ).getData().content[ 1 ].text
				client.close()
			""".formatted( command( serverMode ) ) );
			// @formatter:on

			long pid = Long.parseLong( get( "orphanPid" ).toString() );
			try {
				long deadline = System.currentTimeMillis() + 10_000;
				while ( ProcessHandle.of( pid ).map( ProcessHandle::isAlive ).orElse( false ) && System.currentTimeMillis() < deadline ) {
					Thread.sleep( 200 );
				}
				assertThat( ProcessHandle.of( pid ).map( ProcessHandle::isAlive ).orElse( false ) ).isFalse();
			} finally {
				ProcessHandle.of( pid ).ifPresent( ProcessHandle::destroyForcibly );
			}
		}
	}

	@Test
	@DisplayName( "MCP() still takes the old named argument baseURL" )
	public void testBaseUrlNamedArgument() {
		// @formatter:off
		run( """
			httpClass = getMetadata( MCP( baseURL: "http://127.0.0.1:1/mcp" ) ).name
			positional = getMetadata( MCP( "http://127.0.0.1:1/mcp" ) ).name
			named = getMetadata( MCP( mcpServer: "http://127.0.0.1:1/mcp" ) ).name
		""" );
		// @formatter:on

		assertThat( get( "httpClass" ).toString() ).endsWith( "MCPHttpClient" );
		assertThat( get( "positional" ).toString() ).endsWith( "MCPHttpClient" );
		assertThat( get( "named" ).toString() ).endsWith( "MCPHttpClient" );
	}

	// -------------------------------------------------------------------------------------------------
	// stdout and stderr
	// -------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "Lines on stdout that are not JSON-RPC are skipped and the call still works" )
	public void testNoiseOnStdout() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			response = client.send( "echo", { text: "still fine" } )
			ok = response.isSuccess()
			text = response.getData().content[ 1 ].text
			client.close()
		""".formatted( command( "noise" ) ) );
		// @formatter:on

		assertThat( get( "ok" ) ).isEqualTo( true );
		assertThat( get( "text" ).toString() ).isEqualTo( "still fine" );
	}

	@Test
	@DisplayName( "stderr is captured apart from the protocol and kept as a tail" )
	public void testStderrCaptured() throws InterruptedException {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			client.listTools()
			tail = []
			for( attempt = 1; attempt <= 40; attempt++ ){
				tail = client.getStderrTail()
				if( tail.len() ){ break }
				sleep( 50 )
			}
			joined = tail.toList( "|" )
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "joined" ).toString() ).contains( "fake-mcp-server starting in mode ok" );
	}

	// -------------------------------------------------------------------------------------------------
	// Failures
	// -------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "A request that gets no answer times out, tells the server it was cancelled, and the client keeps working" )
	public void testTimeout() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } ).withTimeout( 800 )
			client.listTools()
			started = getTickCount()
			slow = client.send( "slow", { ms: 2500 } )
			elapsed = getTickCount() - started
			ok = slow.isSuccess()
			error = slow.getError()
			client.withTimeout( 20000 )
			cancelled = client.send( "cancelled", {} ).getData().content[ 1 ].text
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "ok" ) ).isEqualTo( false );
		assertThat( get( "error" ).toString() ).contains( "did not answer within 800 ms" );
		assertThat( ( ( Number ) get( "elapsed" ) ).longValue() ).isLessThan( 2400L );
		assertThat( get( "cancelled" ).toString() ).isEqualTo( "1" );
	}

	@Test
	@DisplayName( "When the server dies in the middle of a call the call fails with the exit code and the last stderr, and the next call starts it again" )
	public void testCrashAndRestart() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			pidBefore = client.send( "pid", {} ).getData().content[ 1 ].text
			crash = client.send( "crash", {} )
			crashOk = crash.isSuccess()
			crashError = crash.getError()
			runningAfterCrash = client.isRunning()
			pidAfter = client.send( "pid", {} ).getData().content[ 1 ].text
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "crashOk" ) ).isEqualTo( false );
		assertThat( get( "crashError" ).toString() ).contains( "exited (exit code 3)" );
		assertThat( get( "crashError" ).toString() ).contains( "crashing on purpose" );
		assertThat( get( "runningAfterCrash" ) ).isEqualTo( false );
		assertThat( get( "pidAfter" ).toString() ).isNotEqualTo( get( "pidBefore" ).toString() );
	}

	@Test
	@DisplayName( "A server that keeps dying is started again maxRestarts times and then the client gives up" )
	public void testMaxRestartsExhausted() {
		// @formatter:off
		run( """
			client = MCP( { command: %s, maxRestarts: 2 } )
			errors = []
			for( i = 1; i <= 4; i++ ){
				errors.append( client.listTools().getError() )
			}
			client.close()
		""".formatted( command( "dieaftershake" ) ) );
		// @formatter:on

		String errors = get( "errors" ).toString();
		assertThat( errors ).contains( "keeps exiting and was started again 2 times" );
	}

	@Test
	@DisplayName( "With maxRestarts 0 a dead server is not started again" )
	public void testNoRestarts() {
		// @formatter:off
		run( """
			client = MCP( { command: %s, maxRestarts: 0 } )
			client.send( "pid", {} )
			client.send( "crash", {} )
			next = client.send( "pid", {} )
			ok = next.isSuccess()
			error = next.getError()
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "ok" ) ).isEqualTo( false );
		assertThat( get( "error" ).toString() ).contains( "started again 0 times" );
	}

	@Test
	@DisplayName( "A server that exits at once gives a failed response with its exit code and stderr" )
	public void testExitOnStart() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			response = client.listTools()
			ok = response.isSuccess()
			error = response.getError()
			client.close()
		""".formatted( command( "exitonstart" ) ) );
		// @formatter:on

		assertThat( get( "ok" ) ).isEqualTo( false );
		assertThat( get( "error" ).toString() ).contains( "exit code 7" );
	}

	@Test
	@DisplayName( "A protocol version nobody speaks, an initialize error and a silent server all fail the handshake with a reason" )
	public void testHandshakeFailures() {
		// @formatter:off
		run( """
			bad = MCP( { command: %s } ).listTools()
			refused = MCP( { command: %s } ).listTools()
			silent = MCP( { command: %s, timeout: 600 } ).listTools()
			badError = bad.getError()
			refusedError = refused.getError()
			silentError = silent.getError()
		""".formatted( command( "badversion" ), command( "initerror" ), command( "nohandshake" ) ) );
		// @formatter:on

		assertThat( get( "badError" ).toString() ).contains( "protocol version [1999-01-01]" );
		assertThat( get( "refusedError" ).toString() ).contains( "init refused" );
		assertThat( get( "silentError" ).toString() ).contains( "did not answer within 600 ms" );
	}

	@Test
	@DisplayName( "An executable that does not exist is a failed response, not an exception" )
	public void testMissingExecutable() {
		// @formatter:off
		run( """
			client = MCP( { command: [ "definitely-not-a-real-binary-xyz", "--flag" ] } )
			response = client.listTools()
			ok = response.isSuccess()
			error = response.getError()
		""" );
		// @formatter:on

		assertThat( get( "ok" ) ).isEqualTo( false );
		assertThat( get( "error" ).toString() ).contains( "Could not start the MCP server [stdio:definitely-not-a-real-binary-xyz]" );
	}

	@Test
	@DisplayName( "A bad command config is refused when the client is built" )
	public void testBadConfig() {
		// @formatter:off
		run( """
			function typeOf( config ){
				try {
					MCP( config )
					return "no error"
				} catch( any e ){
					return e.type
				}
			}
			messages = [ {}, { command: [] }, { command: "bxplaywright mcp" }, { command: [ "" ] } ].map( ( config ) => typeOf( config ) )
			result = messages.toList()
		""" );
		// @formatter:on

		assertThat( get( "result" ).toString() ).isEqualTo( "InvalidArgument,InvalidArgument,InvalidArgument,InvalidArgument" );
	}

	// -------------------------------------------------------------------------------------------------
	// Environment
	// -------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "env is added to the inherited environment and keeps the case of its names" )
	public void testEnvInherited() {
		// @formatter:off
		run( """
			client = MCP( { command: %s, env: { Bx_Test_Var: "abc" } } )
			added = client.send( "getenv", { name: "Bx_Test_Var" } ).getData().content[ 1 ].text
			inherited = client.send( "getenv", { name: "PATH" } ).getData().content[ 1 ].text != "<unset>"
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "added" ).toString() ).isEqualTo( "abc" );
		assertThat( get( "inherited" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "inheritEnv false starts the server with only the given env" )
	public void testEnvClean() {
		// @formatter:off
		run( """
			client = MCP( { command: %s, inheritEnv: false, env: { ONLY_THIS: "1" } } )
			only = client.send( "getenv", { name: "ONLY_THIS" } ).getData().content[ 1 ].text
			path = client.send( "getenv", { name: "PATH" } ).getData().content[ 1 ].text
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "only" ).toString() ).isEqualTo( "1" );
		assertThat( get( "path" ).toString() ).isEqualTo( "<unset>" );
	}

	@Test
	@DisplayName( "cwd sets the working directory of the server" )
	public void testCwd() throws IOException {
		Path directory = Files.createTempDirectory( "mcp-stdio-cwd" );
		try {
			// @formatter:off
			run( """
				client = MCP( { command: %s, cwd: "%s" } )
				workingDir = client.send( "cwd", {} ).getData().content[ 1 ].text
				client.close()
			""".formatted( command( "ok" ), directory.toString() ) );
			// @formatter:on

			assertThat( get( "workingDir" ).toString() ).endsWith( directory.getFileName().toString() );
		} finally {
			Files.deleteIfExists( directory );
		}
	}

	// -------------------------------------------------------------------------------------------------
	// Shutdown
	// -------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "close() closes stdin first, so a server that watches stdin exits on its own, and close() is safe to repeat" )
	public void testGracefulClose() throws IOException {
		Path marker = Files.createTempFile( "mcp-stdio-marker", ".txt" );
		Files.delete( marker );
		try {
			// @formatter:off
			run( """
				client = MCP( { command: %s } )
				client.listTools()
				runningBefore = client.isRunning()
				client.close()
				client.close()
				runningAfter = client.isRunning()
				afterClose = client.listTools()
				afterError = afterClose.getError()
			""".formatted( command( "marker", marker.toString() ) ) );
			// @formatter:on

			assertThat( get( "runningBefore" ) ).isEqualTo( true );
			assertThat( get( "runningAfter" ) ).isEqualTo( false );
			assertThat( Files.exists( marker ) ).isTrue();
			assertThat( get( "afterError" ).toString() ).contains( "is closed" );
		} finally {
			Files.deleteIfExists( marker );
		}
	}

	@Test
	@DisplayName( "When the JVM exits without close(), the shutdown hook terminates a server that would outlive its parent" )
	public void testShutdownHook() throws Exception {
		Path	pidFile		= Files.createTempFile( "mcp-stdio-pid", ".txt" );
		long	serverPid	= -1;
		try {
			String	java	= Paths.get( System.getProperty( "java.home" ), "bin", "java" ).toString();
			// The server ignores the end of stdin and sleeps for a minute, so only an active shutdown can end it early
			Process	harness	= new ProcessBuilder( java, "-cp", System.getProperty( "java.class.path" ), StdioClientHarness.class.getName(),
			    command( "ignoreeof" ), pidFile.toString() ).redirectErrorStream( true ).redirectOutput( ProcessBuilder.Redirect.DISCARD ).start();
			assertThat( harness.waitFor( 90, TimeUnit.SECONDS ) ).isTrue();

			serverPid = Long.parseLong( Files.readString( pidFile ).trim() );
			long deadline = System.currentTimeMillis() + 15_000;
			while ( ProcessHandle.of( serverPid ).map( ProcessHandle::isAlive ).orElse( false ) && System.currentTimeMillis() < deadline ) {
				Thread.sleep( 200 );
			}
			assertThat( ProcessHandle.of( serverPid ).map( ProcessHandle::isAlive ).orElse( false ) ).isFalse();
		} finally {
			if ( serverPid > 0 ) {
				ProcessHandle.of( serverPid ).ifPresent( ProcessHandle::destroyForcibly );
			}
			Files.deleteIfExists( pidFile );
		}
	}

	@Test
	@DisplayName( "close() terminates a server that ignores the end of stdin" )
	public void testCloseTerminates() {
		// @formatter:off
		run( """
			client = MCP( { command: %s } )
			client.listTools()
			started = getTickCount()
			client.close()
			elapsed = getTickCount() - started
			running = client.isRunning()
		""".formatted( command( "ignoreeof" ) ) );
		// @formatter:on

		assertThat( get( "running" ) ).isEqualTo( false );
		assertThat( ( ( Number ) get( "elapsed" ) ).longValue() ).isLessThan( 10000L );
	}

	@Test
	@DisplayName( "MCPStdioClient::closeAll() shuts down every live client, as the module does when it unloads" )
	public void testCloseAll() {
		// @formatter:off
		run( """
			first = MCP( { command: %s } )
			second = MCP( { command: %s } )
			first.listTools()
			second.listTools()
			bothRunning = first.isRunning() && second.isRunning()
			MCPStdioClient::closeAll()
			noneRunning = !first.isRunning() && !second.isRunning()
		""".formatted( command( "ok" ), command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "bothRunning" ) ).isEqualTo( true );
		assertThat( get( "noneRunning" ) ).isEqualTo( true );
	}

	// -------------------------------------------------------------------------------------------------
	// Security
	// -------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "The commandGuard sees the command array and can refuse it" )
	public void testCommandGuard() {
		// @formatter:off
		run( """
			seen = []
			allowed = MCP( { command: %s, commandGuard: ( cmd ) => { seen = cmd; return true } } )
			allowedOk = allowed.listTools().isSuccess()
			allowed.close()
			try {
				MCP( { command: %s, commandGuard: ( cmd ) => false } )
				refusedType = "not refused"
			} catch( any e ){
				refusedType = e.type
			}
		""".formatted( command( "ok" ), command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "allowedOk" ) ).isEqualTo( true );
		assertThat( get( "refusedType" ).toString() ).isEqualTo( "MCPCommandRefused" );
	}

	@Test
	@DisplayName( "The module setting mcp.stdio.allowedCommands limits the executables: a bare name matches the name, a command with a directory must be listed with that exact path" )
	public void testAllowedCommands() {
		String	java		= Paths.get( System.getProperty( "java.home" ), "bin", "java" ).toString();
		String	classes		= command( "ok" );
		String	bareJava	= classes.replaceFirst( "^\\[ \"[^\"]*\"", "[ \"java\"" );

		// @formatter:off
		run( """
			settings = getModuleInfo( "bxai" ).settings
			function refusal( action ){
				try {
					action()
					return "not refused"
				} catch( any e ){
					return e.type
				}
			}
			try {
				settings.mcp.stdio.allowedCommands = [ "npx", "bxplaywright" ]
				notListed = refusal( () => MCP( { command: %s } ) )

				// A bare name is looked up in the PATH, "JAVA" matches "java", case does not matter
				settings.mcp.stdio.allowedCommands = [ "JAVA" ]
				bare = MCP( { command: %s } )
				bareOk = bare.listTools().isSuccess()
				bare.close()
				// The same name with a directory in front is another file: it must be listed with its path
				withDirectory = refusal( () => MCP( { command: %s } ) )

				settings.mcp.stdio.allowedCommands = [ "%s" ]
				exact = MCP( { command: %s } )
				exactOk = exact.listTools().isSuccess()
				exact.close()
			} finally {
				settings.mcp.stdio.allowedCommands = []
			}
		""".formatted( command( "ok" ), bareJava, command( "ok" ), java.replace( "\\", "\\\\" ), command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "notListed" ).toString() ).isEqualTo( "MCPCommandRefused" );
		assertThat( get( "bareOk" ) ).isEqualTo( true );
		assertThat( get( "withDirectory" ).toString() ).isEqualTo( "MCPCommandRefused" );
		assertThat( get( "exactOk" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "A missing, null or malformed mcp.stdio.allowedCommands refuses the command, only an explicit empty array allows all" )
	public void testAllowedCommandsFailClosed() {
		// @formatter:off
		run( """
			settings = getModuleInfo( "bxai" ).settings
			function refusal( action ){
				try {
					action()
					return "not refused"
				} catch( any e ){
					return e.type
				}
			}
			try {
				structDelete( settings.mcp.stdio, "allowedCommands" )
				missing = refusal( () => MCP( { command: %1$s } ) )

				settings.mcp.stdio.allowedCommands = javacast( "null", "" )
				nullValue = refusal( () => MCP( { command: %1$s } ) )

				settings.mcp.stdio.allowedCommands = "java"
				asString = refusal( () => MCP( { command: %1$s } ) )

				settings.mcp.stdio.allowedCommands = { java: true }
				asStruct = refusal( () => MCP( { command: %1$s } ) )

				settings.mcp.stdio.allowedCommands = []
				empty = refusal( () => MCP( { command: %1$s } ).close() )
			} finally {
				settings.mcp.stdio.allowedCommands = []
			}
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "missing" ).toString() ).isEqualTo( "MCPCommandRefused" );
		assertThat( get( "nullValue" ).toString() ).isEqualTo( "MCPCommandRefused" );
		assertThat( get( "asString" ).toString() ).isEqualTo( "MCPCommandRefused" );
		assertThat( get( "asStruct" ).toString() ).isEqualTo( "MCPCommandRefused" );
		assertThat( get( "empty" ).toString() ).isEqualTo( "not refused" );
	}

	@Test
	@DisplayName( "A command with a directory is matched with the exact case of the listed path" )
	public void testAllowedCommandsPathIsCaseSensitive() {
		String	java	= Paths.get( System.getProperty( "java.home" ), "bin", "java" ).toString().replace( "\\", "\\\\" );

		// @formatter:off
		run( """
			settings = getModuleInfo( "bxai" ).settings
			function refusal( action ){
				try {
					action()
					return "not refused"
				} catch( any e ){
					return e.type
				}
			}
			try {
				settings.mcp.stdio.allowedCommands = [ "%s" ]
				otherCase = refusal( () => MCP( { command: %s } ) )
			} finally {
				settings.mcp.stdio.allowedCommands = []
			}
		""".formatted( java.toUpperCase(), command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "otherCase" ).toString() ).isEqualTo( "MCPCommandRefused" );
	}

	// -------------------------------------------------------------------------------------------------
	// Events, stats, agents
	// -------------------------------------------------------------------------------------------------

	@Test
	@DisplayName( "Stats, callbacks and the client events work, and the events say the transport is stdio" )
	public void testStatsCallbacksEvents() {
		// @formatter:off
		run( """
			events = []
			BoxRegisterInterceptor( ( data ) => { events.append( data.transport & ":" & data.baseURL ) }, "onMCPClientResponse" )
			successes = 0
			failures = 0
			client = MCP( { command: %s } )
				.onSuccess( ( r ) => { successes++ } )
				.onError( ( r ) => { failures++ } )
			client.send( "echo", { text: "a" } )
			client.send( "no-such-tool", {} )
			total = client.getSummary().totalCalls
			errors = client.getSummary().totalErrors
			client.close()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "successes" ) ).isEqualTo( 1 );
		assertThat( get( "failures" ) ).isEqualTo( 1 );
		assertThat( get( "total" ) ).isEqualTo( 2 );
		assertThat( get( "errors" ) ).isEqualTo( 1 );
		assertThat( get( "events" ).toString() ).contains( "stdio:stdio:java" );
	}

	@Test
	@DisplayName( "withMCPServer takes a stdio config, applies prefix and toolFilter, calls the tool, and closeMCPServers() ends the process" )
	public void testWithMCPServerStdio() {
		// @formatter:off
		run( """
			model = aiModel().withMCPServer( { command: %s }, { prefix: "fake", toolFilter: [ "echo", "pid" ] } )
			toolNames = model.getTools().map( t => t.getName() ).sort( "text" ).toList()
			answer = model.getTool( "fake__echo" ).invoke( { text: "through the tool" } )
			servers = model.listMCPServers()
			label = servers[ 1 ].url
			serverToolCount = servers[ 1 ].toolNames.len()
			client = model.getOwnedMCPClients()[ 1 ]
			runningBefore = client.isRunning()
			model.closeMCPServers()
			runningAfter = client.isRunning()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "toolNames" ).toString() ).isEqualTo( "fake__echo,fake__pid" );
		assertThat( get( "answer" ).toString() ).contains( "through the tool" );
		assertThat( get( "label" ).toString() ).isEqualTo( "stdio:java" );
		assertThat( get( "serverToolCount" ) ).isEqualTo( 2 );
		assertThat( get( "runningBefore" ) ).isEqualTo( true );
		assertThat( get( "runningAfter" ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "Two agents do not share their MCP clients: closing the servers of one leaves the other running" )
	public void testAgentsDoNotShareMcpClients() {
		// @formatter:off
		run( """
			first  = aiModel().withMCPServer( { command: %s } )
			second = aiModel().withMCPServer( { command: %s } )
			firstClients  = first.getOwnedMCPClients().len()
			secondClients = second.getOwnedMCPClients().len()
			firstServers  = first.listMCPServers().len()
			secondServers = second.listMCPServers().len()
			first.closeMCPServers()
			firstRunning  = first.getOwnedMCPClients().len()
			secondStillRunning = second.getOwnedMCPClients()[ 1 ].isRunning()
			second.closeMCPServers()
		""".formatted( command( "ok" ), command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "firstClients" ) ).isEqualTo( 1 );
		assertThat( get( "secondClients" ) ).isEqualTo( 1 );
		assertThat( get( "firstServers" ) ).isEqualTo( 1 );
		assertThat( get( "secondServers" ) ).isEqualTo( 1 );
		assertThat( get( "firstRunning" ) ).isEqualTo( 0 );
		assertThat( get( "secondStillRunning" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "withMCPServers takes stdio structs next to other entries, and an agent can be given them" )
	public void testWithMCPServersMixed() {
		// @formatter:off
		run( """
			agent = aiAgent(
				name: "stdio-agent",
				model: aiModel( provider: "openai" ),
				mcpServers: [ { command: %s, prefix: "gh", env: { A_VAR: "1" } } ]
			)
			hasTool = agent.hasTool( "gh__echo" )
			agent.closeMCPServers()
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "hasTool" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "HTTP-only options on a stdio server are an error, not silently ignored, and no process is left behind" )
	public void testHttpOptionsRefused() {
		// @formatter:off
		run( """
			try {
				aiModel().withMCPServer( { command: %s }, { token: "secret" } )
				errorType = "no error"
			} catch( any e ){
				errorType = e.type
				errorMessage = e.message
			}
		""".formatted( command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "errorType" ).toString() ).isEqualTo( "InvalidArgument" );
		assertThat( get( "errorMessage" ).toString() ).contains( "token" );
	}

	@Test
	@DisplayName( "A command that is refused throws out of withMCPServer and aiAgent, it is never swallowed like a connection failure" )
	public void testRefusedCommandThrowsFromAgents() {
		// @formatter:off
		run( """
			function typeOf( action ){
				try {
					action()
					return "no error"
				} catch( any e ){
					return e.type
				}
			}
			refuse = ( cmd ) => false
			viaModel = typeOf( () => aiModel().withMCPServer( { command: %s, commandGuard: refuse } ) )
			viaAgent = typeOf( () => aiAgent( name: "x", model: aiModel( provider: "openai" ), mcpServers: [ { command: %s, commandGuard: refuse } ] ) )
		""".formatted( command( "ok" ), command( "ok" ) ) );
		// @formatter:on

		assertThat( get( "viaModel" ).toString() ).isEqualTo( "MCPCommandRefused" );
		assertThat( get( "viaAgent" ).toString() ).isEqualTo( "MCPCommandRefused" );
	}

	@Test
	@DisplayName( "A stdio server that cannot start leaves the agent without its tools and does not throw" )
	public void testUnavailableServerDoesNotBreakTheAgent() {
		// @formatter:off
		run( """
			model = aiModel().withMCPServer( { command: [ "definitely-not-a-real-binary-xyz" ] } )
			toolCount = model.getTools().len()
			serverCount = model.listMCPServers().len()
		""" );
		// @formatter:on

		assertThat( get( "toolCount" ) ).isEqualTo( 0 );
		assertThat( get( "serverCount" ) ).isEqualTo( 0 );
	}

}
