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
package ortus.boxlang.ai;

import ortus.boxlang.runtime.scopes.Key;

/**
 * Keys for the AI module.
 */
public class AiKeys {

	public static final Key	AiService			= Key.of( "aiService" );
	public static final Key	bxai				= Key.of( "bxai" );
	public static final Key	serverName			= Key.of( "serverName" );
	public static final Key	description			= Key.of( "description" );
	public static final Key	version				= Key.of( "version" );
	public static final Key	cors				= Key.of( "cors" );
	public static final Key	onRequest			= Key.of( "onRequest" );
	public static final Key	onResponse			= Key.of( "onResponse" );

	// MCP Events
	public static final Key	onMCPServerCreate	= Key.of( "onMCPServerCreate" );
	public static final Key	onMCPServerRemove	= Key.of( "onMCPServerRemove" );
	public static final Key	onMCPRequest		= Key.of( "onMCPRequest" );
	public static final Key	onMCPResponse		= Key.of( "onMCPResponse" );

}
