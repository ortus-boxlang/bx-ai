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

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PaidProviderGuardTest {

	@Test
	@DisplayName( "The answers providers give for a missing or refused key are recognised, also inside a cause" )
	public void testMissingKeyAnswers() {
		assertThat( PaidProviderGuard.isMissingKey( new RuntimeException( "You didn't provide an API key. You need to" ) ) ).isTrue();
		assertThat(
		    PaidProviderGuard.isMissingKey( new RuntimeException( "{ type : \"authentication_error\", message : \"x-api-key header is required\" }" ) ) )
		    .isTrue();
		assertThat( PaidProviderGuard.isMissingKey( new RuntimeException( "Missing Authentication header" ) ) ).isTrue();
		assertThat( PaidProviderGuard.isMissingKey( new RuntimeException( "login fail: Please carry the API secret key" ) ) ).isTrue();
		assertThat( PaidProviderGuard.isMissingKey( new RuntimeException( "wrapper", new RuntimeException( "Invalid API Key" ) ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Other failures are not mistaken for a missing key, they still fail the test" )
	public void testOtherFailuresAreNotSkipped() {
		assertThat( PaidProviderGuard.isMissingKey( new AssertionError( "expected: 2 but was: 3" ) ) ).isFalse();
		assertThat( PaidProviderGuard.isMissingKey( new RuntimeException( "Connection refused" ) ) ).isFalse();
		assertThat( PaidProviderGuard.isMissingKey( new RuntimeException( ( String ) null ) ) ).isFalse();
	}

}
