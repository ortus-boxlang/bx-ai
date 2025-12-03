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
package ortus.boxlang.ai.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ortus.boxlang.ai.util.KeyDictionary;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.BaseService;
import ortus.boxlang.runtime.types.Array;

/**
 * This service is in charge of managing all MCP (Model Context Protocol) servers and their lifecycles.
 */
public class AiService extends BaseService {

	/**
	 * Concurrent map that stores all MCP servers by name
	 */
	private final ConcurrentMap<Key, IClassRunnable>	mcpServers	= new ConcurrentHashMap<>();

	/**
	 * The main AI logger (volatile for thread-safe lazy initialization)
	 */
	private volatile BoxLangLogger						logger;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * public no-arg constructor for the ServiceProvider
	 */
	public AiService() {
		this( BoxRuntime.getInstance() );
	}

	/**
	 * Constructor
	 *
	 * @param runtime The BoxRuntime
	 */
	public AiService( BoxRuntime runtime ) {
		super( runtime, KeyDictionary.AiService );
		getLogger().trace( "+ AI Service built" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public void onConfigurationLoad() {
		// Not used by the service, since those are only for core services
	}

	@Override
	public void onShutdown( Boolean force ) {
		getLogger().info( "+ AI Service shutdown requested" );
		clearAllServers();
	}

	@Override
	public void onStartup() {
		getLogger().info( "+ AI Service started" );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * MCP Server Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * How many MCP servers do we store
	 *
	 * @return The number of servers
	 */
	public int getServerCount() {
		return this.mcpServers.size();
	}

	/**
	 * Get or store an MCP server instance
	 *
	 * @param name   The name of the server
	 * @param server The server instance to store
	 *
	 * @return The MCPServer that was found or stored
	 */
	public IClassRunnable getOrBuildServer( Key name, IClassRunnable server ) {
		return this.mcpServers.computeIfAbsent( name, key -> server );
	}

	/**
	 * Put/replace an MCP server instance directly
	 *
	 * @param name   The name of the server
	 * @param server The server instance to store
	 *
	 * @return The previous server instance if any
	 */
	public IClassRunnable putServer( Key name, IClassRunnable server ) {
		return this.mcpServers.put( name, server );
	}

	/**
	 * Get an existing MCP server by name
	 *
	 * @param name The name of the server
	 *
	 * @return The MCPServer if found, null otherwise
	 */
	public IClassRunnable getServer( Key name ) {
		return this.mcpServers.get( name );
	}

	/**
	 * Verifies if the named server exists
	 *
	 * @param name The key of the server
	 *
	 * @return True if the server exists, false if it does not
	 */
	public boolean hasServer( Key name ) {
		return this.mcpServers.containsKey( name );
	}

	/**
	 * Remove a server by key
	 *
	 * @param name The key of the server
	 *
	 * @return True if the server was removed, false if it was not found
	 */
	public boolean removeServer( Key name ) {
		IClassRunnable server = this.mcpServers.remove( name );
		return server != null;
	}

	/**
	 * Get all server names
	 *
	 * @return Set of server names
	 */
	public Array getServerNames() {
		return Array.fromSet( this.mcpServers.keySet().stream().map( Key::getName ).collect( java.util.stream.Collectors.toSet() ) );
	}

	/**
	 * Clear all MCP servers
	 */
	public void clearAllServers() {
		this.mcpServers.clear();
		getLogger().debug( "All MCP servers cleared" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helper methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the AI logger that logs to the "ai" category.
	 */
	public BoxLangLogger getLogger() {
		if ( this.logger == null ) {
			synchronized ( AiService.class ) {
				if ( this.logger == null ) {
					this.logger = runtime.getLoggingService().getLogger( "ai" );
				}
			}
		}
		return this.logger;
	}

}
