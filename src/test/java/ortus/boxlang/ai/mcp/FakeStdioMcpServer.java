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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tiny MCP server over stdin/stdout, used by the MCPStdioClient tests. It is a plain Java main so a test can launch it quickly as a child
 * process. It reads one JSON-RPC message per line and answers with one line of JSON.
 *
 * Arguments: the first one is a mode, the optional second one is a file path some modes use.
 *
 * <ul>
 * <li>ok: a well behaved server</li>
 * <li>badversion: answers initialize with a protocol version nobody speaks</li>
 * <li>initerror: answers initialize with a JSON-RPC error</li>
 * <li>nohandshake: never answers initialize</li>
 * <li>exitonstart: writes to stderr and exits with code 7 before reading anything</li>
 * <li>dieaftershake: exits with code 9 as soon as the initialized notification arrives</li>
 * <li>noise: writes lines that are not JSON to stdout before every answer</li>
 * <li>ignoreeof: does not exit when stdin closes (the client has to terminate it)</li>
 * <li>marker: writes the file given as second argument when stdin reaches its end (a graceful shutdown)</li>
 * <li>noread: after the handshake it stops reading stdin for two minutes (a busy server), so a large write to it blocks</li>
 * <li>slowthree: the first three launches wait 3 seconds before they read anything (a slow first start). The second argument is a counter file</li>
 * <li>orphan: starts a "sleep" child process, then exits when stdin closes and leaves the child running (a launcher that does not take its server with it)</li>
 * <li>orphanstubborn: like orphan, but the child ignores SIGTERM, only a kill ends it</li>
 * <li>grandchild: starts a "sleep" child process and ignores the end of stdin. The tool gcpid returns the pid of that child</li>
 * <li>strict: answers any request other than initialize with an error until notifications/initialized has arrived, as a server that follows the lifecycle may</li>
 * </ul>
 *
 * Tools: structid (writes a message whose id is a struct, then answers), gcpid, echo (text), pid, getenv (name), cwd, big (size), slow (ms), crash, pingcheck, cancelled, args (returns the raw arguments).
 */
public class FakeStdioMcpServer {

	private static final Pattern	ID_PATTERN		= Pattern.compile( "\"id\"\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|-?[0-9.]+)" );
	private static final Pattern	METHOD_PATTERN	= Pattern.compile( "\"method\"\\s*:\\s*\"([^\"]*)\"" );
	private static final Pattern	NAME_PATTERN	= Pattern.compile( "\"name\"\\s*:\\s*\"([^\"]*)\"" );
	private static final Pattern	SIZE_PATTERN	= Pattern.compile( "\"size\"\\s*:\\s*([0-9]+)" );
	private static final Pattern	MS_PATTERN		= Pattern.compile( "\"ms\"\\s*:\\s*([0-9]+)" );
	private static final Pattern	ENVNAME_PATTERN	= Pattern.compile( "\"name\"\\s*:\\s*\"([^\"]*)\"\\s*[,}]" );

	private static PrintStream		out;
	private static String			mode;
	private static int				cancelNotices	= 0;
	private static boolean			initialized		= false;
	private static Process			grandchild;

	public static void main( String[] args ) throws Exception {
		mode	= args.length > 0 ? args[ 0 ] : "ok";
		out		= new PrintStream( System.out, true, StandardCharsets.UTF_8 );
		String markerPath = args.length > 1 ? args[ 1 ] : null;

		System.err.println( "fake-mcp-server starting in mode " + mode );

		if ( mode.equals( "exitonstart" ) ) {
			System.err.println( "boom: cannot start" );
			System.exit( 7 );
		}

		if ( mode.equals( "slowthree" ) && markerPath != null ) {
			Path	counter	= Path.of( markerPath );
			int		seen	= Files.exists( counter ) ? Files.readAllLines( counter ).size() : 0;
			Files.writeString( counter, "x\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND );
			if ( seen < 3 ) {
				Thread.sleep( 3000 );
			}
		}
		if ( mode.equals( "grandchild" ) || mode.equals( "orphan" ) ) {
			grandchild = new ProcessBuilder( "sleep", "120" ).start();
		}
		if ( mode.equals( "orphanstubborn" ) ) {
			// Signals that are ignored stay ignored across exec: the sleep does not die of SIGTERM
			grandchild = new ProcessBuilder( "sh", "-c", "trap '' TERM; exec sleep 120" ).start();
		}

		BufferedReader	in	= new BufferedReader( new InputStreamReader( System.in, StandardCharsets.UTF_8 ) );
		String			line;
		while ( ( line = in.readLine() ) != null ) {
			if ( line.isBlank() ) {
				continue;
			}
			handle( line, in );
		}

		// stdin closed
		if ( mode.equals( "marker" ) && markerPath != null ) {
			Files.writeString( Path.of( markerPath ), "eof" );
		}
		if ( mode.equals( "ignoreeof" ) || mode.equals( "grandchild" ) ) {
			Thread.sleep( 60_000 );
		}
	}

	private static void handle( String line, BufferedReader in ) throws IOException, InterruptedException {
		String	method	= first( METHOD_PATTERN, line );
		String	id		= first( ID_PATTERN, line );

		if ( method == null ) {
			return;
		}

		if ( method.equals( "notifications/initialized" ) ) {
			initialized = true;
			if ( mode.equals( "noread" ) ) {
				Thread.sleep( 120_000 );
			}
			if ( mode.equals( "dieaftershake" ) ) {
				System.exit( 9 );
			}
			return;
		}
		if ( method.equals( "notifications/cancelled" ) ) {
			cancelNotices++;
			return;
		}
		if ( id == null ) {
			return;
		}

		if ( mode.equals( "strict" ) && !initialized && !method.equals( "initialize" ) ) {
			reply( id, null, "{\"code\":-32600,\"message\":\"Server not initialized\"}" );
			return;
		}

		if ( mode.equals( "noise" ) ) {
			out.println( "this line is not json" );
			out.println( "{\"not\":\"a jsonrpc message\"}" );
		}

		switch ( method ) {
			case "initialize" :
				if ( mode.equals( "nohandshake" ) ) {
					return;
				}
				if ( mode.equals( "initerror" ) ) {
					reply( id, null, "{\"code\":-32603,\"message\":\"init refused\"}" );
					return;
				}
				String version = mode.equals( "badversion" ) ? "1999-01-01" : requestedVersion( line );
				reply( id,
				    "{\"protocolVersion\":\"" + version + "\",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\"fake\",\"version\":\"1.0\"}}",
				    null );
				break;
			case "tools/list" :
				reply( id,
				    "{\"tools\":[" + tool( "echo", "Echo the text back" ) + "," + tool( "pid", "Process id" ) + "," + tool( "big", "A big answer" ) + "]}",
				    null );
				break;
			case "resources/list" :
				reply( id, "{\"resources\":[{\"uri\":\"fake://one\",\"name\":\"one\"}]}", null );
				break;
			case "resources/read" :
				reply( id, "{\"contents\":[{\"uri\":\"fake://one\",\"text\":\"resource body\"}]}", null );
				break;
			case "prompts/list" :
				reply( id, "{\"prompts\":[{\"name\":\"greet\",\"description\":\"Greets\"}]}", null );
				break;
			case "prompts/get" :
				reply( id, "{\"messages\":[{\"role\":\"user\",\"content\":{\"type\":\"text\",\"text\":\"hello\"}}]}", null );
				break;
			case "tools/call" :
				callTool( id, line, in );
				break;
			default :
				reply( id, null, "{\"code\":-32601,\"message\":\"Method not found: " + method + "\"}" );
		}
	}

	private static void callTool( String id, String line, BufferedReader in ) throws IOException, InterruptedException {
		String name = first( NAME_PATTERN, line );
		if ( name == null ) {
			reply( id, null, "{\"code\":-32602,\"message\":\"no tool name\"}" );
			return;
		}
		switch ( name ) {
			case "echo" :
				String raw = rawString( line, "text" );
				text( id, raw == null ? "\"\"" : raw );
				break;
			case "pid" :
				text( id, "\"" + ProcessHandle.current().pid() + "\"" );
				break;
			case "getenv" :
				String envName = first( Pattern.compile( "\"arguments\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]*)\"" ), line );
				String value = envName == null ? null : System.getenv( envName );
				text( id, "\"" + ( value == null ? "<unset>" : value ) + "\"" );
				break;
			case "structid" :
				out.println( "{\"jsonrpc\":\"2.0\",\"id\":{\"a\":1},\"result\":{}}" );
				text( id, "\"ok\"" );
				break;
			case "gcpid" :
				text( id, "\"" + ( grandchild == null ? -1 : grandchild.pid() ) + "\"" );
				break;
			case "cwd" :
				text( id, "\"" + System.getProperty( "user.dir" ).replace( "\\", "\\\\" ) + "\"" );
				break;
			case "big" :
				int size = Integer.parseInt( first( SIZE_PATTERN, line ) );
				StringBuilder big = new StringBuilder( size + 2 );
				big.append( '"' );
				for ( int i = 0; i < size; i++ ) {
					big.append( i % 80 == 79 ? 'x' : 'a' );
				}
				big.append( '"' );
				text( id, big.toString() );
				break;
			case "slow" :
				Thread.sleep( Long.parseLong( first( MS_PATTERN, line ) ) );
				text( id, "\"done\"" );
				break;
			case "crash" :
				System.err.println( "crashing on purpose" );
				System.exit( 3 );
				break;
			case "pingcheck" :
				out.println( "{\"jsonrpc\":\"2.0\",\"id\":\"srv-1\",\"method\":\"ping\"}" );
				String pingReply = in.readLine();
				boolean ok = pingReply != null && pingReply.contains( "\"srv-1\"" ) && pingReply.contains( "\"result\"" );
				text( id, ok ? "\"ping-answered\"" : "\"ping-not-answered\"" );
				break;
			case "unknowncheck" :
				out.println( "{\"jsonrpc\":\"2.0\",\"id\":\"srv-2\",\"method\":\"sampling/createMessage\",\"params\":{}}" );
				String unknownReply = in.readLine();
				boolean refused = unknownReply != null && unknownReply.contains( "-32601" );
				text( id, refused ? "\"refused\"" : "\"not-refused\"" );
				break;
			case "cancelled" :
				text( id, "\"" + cancelNotices + "\"" );
				break;
			case "args" :
				String args = line.substring( line.indexOf( "\"arguments\"" ) );
				text( id, "\"" + args.replace( "\\", "\\\\" ).replace( "\"", "\\\"" ) + "\"" );
				break;
			default :
				reply( id, null, "{\"code\":-32602,\"message\":\"Unknown tool: " + name + "\"}" );
		}
	}

	private static String tool( String name, String description ) {
		return "{\"name\":\"" + name + "\",\"description\":\"" + description + "\",\"inputSchema\":{\"type\":\"object\",\"properties\":{}}}";
	}

	private static void text( String id, String rawJsonString ) {
		reply( id, "{\"content\":[{\"type\":\"text\",\"text\":" + rawJsonString + "}]}", null );
	}

	private static void reply( String id, String result, String error ) {
		StringBuilder message = new StringBuilder( "{\"jsonrpc\":\"2.0\",\"id\":" ).append( id );
		if ( error != null ) {
			message.append( ",\"error\":" ).append( error );
		} else {
			message.append( ",\"result\":" ).append( result );
		}
		out.println( message.append( '}' ) );
	}

	private static String requestedVersion( String line ) {
		String version = first( Pattern.compile( "\"protocolVersion\"\\s*:\\s*\"([^\"]*)\"" ), line );
		return version == null ? "2025-06-18" : version;
	}

	/**
	 * The raw JSON string (quotes included) that follows "key": in the line. Scans by hand because a regex stack-overflows on a huge string.
	 */
	private static String rawString( String line, String key ) {
		int keyAt = line.indexOf( "\"" + key + "\"" );
		if ( keyAt < 0 ) {
			return null;
		}
		int start = line.indexOf( '"', line.indexOf( ':', keyAt ) );
		if ( start < 0 ) {
			return null;
		}
		for ( int i = start + 1; i < line.length(); i++ ) {
			char c = line.charAt( i );
			if ( c == '\\' ) {
				i++;
			} else if ( c == '"' ) {
				return line.substring( start, i + 1 );
			}
		}
		return null;
	}

	private static String first( Pattern pattern, String text ) {
		Matcher matcher = pattern.matcher( text );
		return matcher.find() ? matcher.group( 1 ) : null;
	}

}
