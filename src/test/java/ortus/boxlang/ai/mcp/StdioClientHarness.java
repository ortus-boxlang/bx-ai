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
 * A JVM that starts a stdio MCP client and then exits WITHOUT calling close(), so a test can check that the shutdown hook of the client
 * still shuts the child process down. Used by mcpStdioClientTest, not a test itself.
 *
 * Arguments: the BoxLang array literal of the command to launch, and a file to write the process id of the server to.
 */
public class StdioClientHarness extends BaseIntegrationTest {

	public static void main( String[] args ) {
		BaseIntegrationTest.setup();
		IBoxContext context = new ScriptingRequestBoxContext();
		runtime.executeSource(
		    "client = MCP( { command: " + args[ 0 ] + " } )\nfileWrite( \"" + args[ 1 ] + "\", client.send( \"pid\", {} ).getData().content[ 1 ].text )",
		    context );
		// Leave without close(): the shutdown hook has to do it
		System.exit( 0 );
	}

}
