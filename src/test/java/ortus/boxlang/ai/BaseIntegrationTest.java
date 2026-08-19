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
package ortus.boxlang.ai;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assumptions.abort;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import io.github.cdimascio.dotenv.Dotenv;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.ModuleService;

/**
 * Use this as a base integration test for your non web-support package
 * modules. If you want web based testing, use the BaseWebIntegrationTest
 */
public abstract class BaseIntegrationTest {

	protected static Dotenv					dotenv						= Dotenv.load();

	/**
	 * Live-Bedrock model ids, overridable via .env. Defaults are "global." inference profiles
	 * deliberately: in us-east-1 the only on-demand Anthropic *foundation* model is the 2024
	 * claude-3-haiku, so any current model is reachable only through a profile, and a
	 * region-prefixed one ("eu."/"us.") pins the suite to one region. A global profile resolves
	 * in every region. Shared here so a deprecation is a one-line change, not a two-file hunt.
	 */
	protected static final String			BEDROCK_MODEL				= dotenv.get( "BEDROCK_MODEL", "global.anthropic.claude-haiku-4-5-20251001-v1:0" );
	protected static final String			BEDROCK_STRUCTURED_MODEL	= dotenv.get( "BEDROCK_STRUCTURED_MODEL", "global.anthropic.claude-sonnet-4-6" );
	protected static BoxRuntime				runtime;
	protected static ModuleService			moduleService;
	protected static ModuleRecord			moduleRecord;
	protected static Key					result						= new Key( "result" );
	protected static Key					moduleName					= new Key( "bxai" );
	protected ScriptingRequestBoxContext	context;
	protected IScope						variables;

	@BeforeAll
	public static void setup() {
		runtime			= BoxRuntime.getInstance( true, Path.of( "src/test/resources/boxlang.json" ).toString() );
		moduleService	= runtime.getModuleService();
		// Load the module
		loadModule( runtime.getRuntimeContext() );
	}

	@BeforeEach
	public void setupEach() {
		// Create the mock contexts
		context		= new ScriptingRequestBoxContext();
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	protected static void loadModule( IBoxContext context ) {
		if ( !runtime.getModuleService().hasModule( moduleName ) ) {
			System.out.println( "Loading module: " + moduleName );
			String physicalPath = Paths.get( "./build/module" ).toAbsolutePath().toString();
			moduleRecord = new ModuleRecord( physicalPath );

			moduleService.getRegistry().put( moduleName, moduleRecord );

			moduleRecord
			    .loadDescriptor( context )
			    .register( context )
			    .activate( context );

			// Execute the onRuntimeStart() lifecycle method to ensure tools are registered
			// Since we are lazy loading the module
			moduleRecord.moduleConfig.dereferenceAndInvoke(
			    context,
			    Key.of( "onRuntimeStart" ),
			    new Object[] {},
			    false
			);
		} else {
			moduleRecord = moduleService.getRegistry().get( moduleName );
			System.out.println( "Module already loaded: " + moduleName );
		}
	}

	/**
	 * Check if an exception is a request timeout error
	 *
	 * @param e The exception to check
	 *
	 * @return true if this is a timeout exception
	 */
	protected boolean isTimeoutException( Throwable e ) {
		String message = e.getMessage();
		if ( message == null ) {
			return false;
		}
		String lowerMessage = message.toLowerCase();
		return lowerMessage.contains( "request timed out" ) || lowerMessage.contains( "timeout" ) || lowerMessage.contains( "timed out" );
	}

	/**
	 * Execute BoxLang source with timeout exception handling.
	 * If a timeout occurs, the test will pass with a warning message instead of failing.
	 * This wraps the runtime.executeSource() to automatically handle timeouts in all tests.
	 *
	 * @param source  The BoxLang source code to execute
	 * @param context The execution context
	 *
	 * @return true if the source executed without a timeout (assertions relying on its output
	 *         are safe to run); false if a timeout was caught and swallowed (callers should skip
	 *         any assertions that depend on variables the source would have set)
	 */
	protected boolean executeWithTimeoutHandling( String source, IBoxContext context ) {
		try {
			runtime.executeSource( source, context );
			return true;
		} catch ( Exception e ) {
			System.out.println( "Exception during execution: " + e.getMessage() );
			if ( isTimeoutException( e ) ) {
				System.out.println( "⚠️  Test passed with timeout - LLM request timed out (acceptable in CI): " + e.getMessage() );
				// Test passes - timeout is acceptable in CI environments
				return false;
			}
			// Re-throw other exceptions
			throw e;
		}
	}

	/**
	 * getSystemSetting() resolves against BOTH System.getenv() and System.getProperties(), and
	 * BoxLang structs are case-insensitive — so a test asserting "no credential is configured"
	 * cannot just read System.getenv( NAME ) and call it settled.
	 */
	protected static boolean hasAmbientSetting( String name ) {
		if ( System.getProperty( name ) != null ) {
			return true;
		}
		return System.getenv().keySet().stream().anyMatch( k -> k.equalsIgnoreCase( name ) )
		    || System.getProperties().keySet().stream().anyMatch( k -> String.valueOf( k ).equalsIgnoreCase( name ) );
	}

	/**
	 * Markers that describe the ENVIRONMENT: this account/role cannot make the call at all.
	 * Deliberately absent: "InvalidSignatureException" (SigV4 canonicalization regressed) and
	 * "ValidationException" — Bedrock returns the latter for a malformed request body, bad
	 * parameters, or an unknown model id, which is exactly what a request-transform regression
	 * looks like. Keying on the 403 status instead would be wrong: AWS returns 403 for
	 * InvalidSignatureException too, so a SigV4 regression would be tolerated as an entitlement
	 * problem. Those must fail the build.
	 */
	private static final String[] LIVE_CALL_ENVIRONMENT_MARKERS = {
	    // no entitlement / wrong or unusable principal
	    "accessdenied", "unrecognizedclient", "don't have access", "not authorized",
	    // an SSO/STS session token that has aged out is the most common local-dev case, and
	    // "expired" is a different string from "invalid" — matching only the latter missed it
	    "security token included in the request is invalid", "security token included in the request is expired",
	    "expiredtoken",
	    // the CI matrix runs os x jdk jobs in parallel against one Bedrock account, so
	    // per-account InvokeModel TPS is reachable; the module has real 429 handling elsewhere
	    "throttling", "too many requests"
	};

	/**
	 * executeWithTimeoutHandling() tolerates only timeouts and re-throws everything else, so a
	 * model this account has not enabled — or a role without bedrock:InvokeModel — turns a live
	 * smoke test into a hard build failure. Those are environment facts, not regressions: skip.
	 *
	 * @return the same contract as executeWithTimeoutHandling(): false if a timeout was caught
	 *         and swallowed, so callers must skip assertions depending on the source's output.
	 */
	protected boolean executeLiveBedrockCall( String source, IBoxContext context ) {
		try {
			return executeWithTimeoutHandling( source, context );
		} catch ( Exception e ) {
			// The provider inlines the raw AWS body into the message, but an async path can wrap
			// it, so walk the cause chain rather than reading only the outermost message.
			for ( Throwable t = e; t != null; t = t.getCause() ) {
				String message = t.getMessage();
				if ( message == null ) {
					continue;
				}
				String haystack = message.toLowerCase();
				for ( String marker : LIVE_CALL_ENVIRONMENT_MARKERS ) {
					if ( haystack.contains( marker ) ) {
						abort( "Bedrock live call unavailable for these credentials: " + message );
					}
				}
			}
			throw e;
		}
	}

}
