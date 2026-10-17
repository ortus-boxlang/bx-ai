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

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;

/**
 * A JVM that runs bx-ai's own MCP server on stdin/stdout (MCPRequestProcessor::processStdio), the server side of the stdio transport.
 * mcpStdioClientTest launches it as the child process of an MCPStdioClient to prove that the client and the server agree.
 * Not a test itself.
 */
public class StdioServerHarness extends BaseIntegrationTest {

	public static void main( String[] args ) {
		BaseIntegrationTest.setup();
		IBoxContext context = new ScriptingRequestBoxContext();
		// @formatter:off
		runtime.executeSource(
			"""
				import bxModules.bxai.models.mcp.MCPRequestProcessor;
				MCPServer( "default" )
					.registerTool( aiTool( "echo", "Echo the message back", ( message ) => "echo: " & message ) )
				MCPRequestProcessor::processStdio( "default" )
			""",
			context
		);
		// @formatter:on
		// The server loop ended because stdin closed
		System.exit( 0 );
	}

}
