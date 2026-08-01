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
package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "aiGateway() BIF Tests" )
public class aiGatewayTest extends BaseIntegrationTest {

	@DisplayName( "aiGateway('mock') resolves to a configured IGateway instance" )
	@Test
	public void testResolvesMockGateway() {
		// @formatter:off
		runtime.executeSource(
			"""
				gw           = aiGateway( "mock" )
				isGateway    = isInstanceOf( gw, "IGateway" )
				name         = gw.getName()
				capabilities = gw.getCapabilities()
				supportsIn   = gw.supports( "inboundMessages" )
				supportsOut  = gw.supports( "outboundMessages" )
				supportsHitl = gw.supports( "humanApproval" )
				supportsStream = gw.supports( "streaming" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isGateway" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "mock" );
		assertThat( variables.getAsBoolean( Key.of( "supportsIn" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "supportsOut" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "supportsHitl" ) ) ).isTrue();
		// MockGateway does not implement IStreamingGateway
		assertThat( variables.getAsBoolean( Key.of( "supportsStream" ) ) ).isFalse();
	}

	@DisplayName( "aiGateway('unknown') throws GatewayNotSupported when no listener supplies one" )
	@Test
	public void testUnknownGatewayThrows() {
		assertThrows(
		    Exception.class,
		    () -> runtime.executeSource(
		        """
		        gw = aiGateway( "totally-unknown-gateway" )
		        """,
		        context
		    )
		);
	}

	@DisplayName( "aiGateway('unknown') fires onMissingGateway so an external module can supply one" )
	@Test
	public void testOnMissingGatewayExtensionPoint() {
		// @formatter:off
		runtime.executeSource(
			"""
				boxRegisterInterceptor(
					( data ) -> {
						if( data.name == "myCustomGateway" ) {
							data.gateway = {
								getName : () => "myCustomGateway"
							}
						}
					},
					"onMissingGateway"
				)
				gw     = aiGateway( "myCustomGateway" )
				result = gw.getName()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( "myCustomGateway" );
	}

}
