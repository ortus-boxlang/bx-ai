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

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

/**
 * Pull requests run without the API keys of the providers that charge per call (the CI workflow sets
 * AI_TESTS_PAID_PROVIDERS=false). A test that then fails because the provider says it has no valid key is skipped,
 * not failed. Any other failure still fails. Without the variable, or with any other value, nothing is changed:
 * a missing key is a failure, as it always was.
 */
public class PaidProviderGuard implements TestExecutionExceptionHandler {

	/**
	 * Text the providers answer when the key is missing or not accepted
	 */
	private static final List<String> MISSING_KEY_MARKERS = List.of(
	    "api key",
	    "api-key",
	    "x-api-key",
	    "invalid_api_key",
	    "authentication",
	    "unauthorized",
	    "login fail"
	);

	/**
	 * true when this run was told not to use the paid providers
	 */
	public static boolean paidProvidersDisabled() {
		return "false".equalsIgnoreCase( System.getenv( "AI_TESTS_PAID_PROVIDERS" ) );
	}

	@Override
	public void handleTestExecutionException( ExtensionContext context, Throwable throwable ) throws Throwable {
		if ( paidProvidersDisabled() && isMissingKey( throwable ) ) {
			throw new TestAbortedException( "Skipped: paid providers are disabled for this run (" + firstLine( throwable ) + ")", throwable );
		}
		throw throwable;
	}

	static boolean isMissingKey( Throwable throwable ) {
		for ( Throwable current = throwable; current != null; current = current.getCause() ) {
			String message = current.getMessage();
			if ( message != null ) {
				String lower = message.toLowerCase( Locale.ROOT );
				for ( String marker : MISSING_KEY_MARKERS ) {
					if ( lower.contains( marker ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static String firstLine( Throwable throwable ) {
		String message = throwable.getMessage();
		if ( message == null ) {
			return throwable.getClass().getSimpleName();
		}
		String line = message.strip().lines().findFirst().orElse( "" );
		return line.length() > 120 ? line.substring( 0, 120 ) : line;
	}

}
