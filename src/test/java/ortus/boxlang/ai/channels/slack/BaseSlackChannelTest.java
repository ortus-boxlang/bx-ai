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
package ortus.boxlang.ai.channels.slack;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Base for tests exercising the Slack channel (channels/slack) — loads it as a second
 * module in the SAME runtime/classloader that already has bx-ai's own module loaded
 * (see BaseIntegrationTest), so bxaiSlack's `bxModules.bxai.*` imports resolve against
 * the exact same compiled classes bx-ai's own code uses.
 *
 * This in-process loading (JUnit + a single BoxRuntime, no CommandBox/`box install`, no
 * TestBox) is deliberate: when this channel lived in a separate repo tested via
 * CommandBox + TestBox, bx-ai ended up loaded twice in that CI topology (once via a
 * top-level module symlink, once via a box-installed copy nested in the channel
 * module's own folder) — two independent module instances, each with its own
 * gatewayRegistry() singleton and its own compiled copy of every bx-ai class, so a
 * gateway registered through one was invisible to (and type-mismatched against) code
 * running through the other. Loading both modules once, here, in the same process
 * removes that whole class of bug by construction.
 */
public abstract class BaseSlackChannelTest extends BaseIntegrationTest {

	protected static Key			slackModuleName	= new Key( "bxaiSlack" );
	protected static ModuleRecord	slackModuleRecord;

	@BeforeAll
	public static void setupSlackChannel() {
		// bx-ai itself is already loaded by BaseIntegrationTest's @BeforeAll (JUnit runs
		// superclass @BeforeAll methods before the subclass's own).
		if ( !runtime.getModuleService().hasModule( slackModuleName ) ) {
			System.out.println( "Loading module: " + slackModuleName );
			String physicalPath = Paths.get( "./channels/slack" ).toAbsolutePath().toString();
			slackModuleRecord = new ModuleRecord( physicalPath );

			moduleService.getRegistry().put( slackModuleName, slackModuleRecord );

			slackModuleRecord
			    .loadDescriptor( runtime.getRuntimeContext() )
			    .register( runtime.getRuntimeContext() )
			    .activate( runtime.getRuntimeContext() );

			// Lazily-loaded test modules don't automatically receive the onRuntimeStart
			// broadcast, so invoke it explicitly — this is what registers the configured
			// SlackGateway in bx-ai's gatewayRegistry() (see ModuleConfig.bx).
			slackModuleRecord.moduleConfig.dereferenceAndInvoke(
			    runtime.getRuntimeContext(),
			    Key.of( "onRuntimeStart" ),
			    new Object[] {},
			    false
			);
		} else {
			slackModuleRecord = moduleService.getRegistry().get( slackModuleName );
			System.out.println( "Module already loaded: " + slackModuleName );
		}
	}

}
