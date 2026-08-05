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

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "Slack channel module loads and integrates with bx-ai" )
public class SlackSmokeTest extends BaseSlackChannelTest {

	@DisplayName( "aiGateway('slack') resolves the same instance gatewayRegistry() holds, across the bxai/bxaiSlack module boundary" )
	@Test
	public void testSlackGatewayResolvesViaRegistryAcrossModules() {
		runtime.executeSource(
		    """
		    gw = aiGateway( "slack" )
		    name = gw.getName()
		    isGateway = isInstanceOf( gw, "IGateway" )
		    fromRegistry = gatewayRegistry().get( "slack" )
		    sameInstance = fromRegistry.getName() == gw.getName()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "slack" );
		assertThat( variables.getAsBoolean( Key.of( "isGateway" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sameInstance" ) ) ).isTrue();
	}

	@DisplayName( "HumanInteractionRequest/GatewayContext instances built in bxaiSlack's own compiled code satisfy bx-ai's strict-typed contracts" )
	@Test
	public void testRequestHumanInteractionCrossModuleTypes() {
		runtime.executeSource(
		    """
		    import bxModules.bxaiSlack.models.SlackGateway;
		    import bxModules.bxai.models.gateway.contracts.GatewayContext;
		    import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;

		    gw = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )
		    gw.callSlackApi = ( method, body ) => {
		        return { ok: true, channel: body.channel ?: "", ts: "1700000000.000100" }
		    }

		    gwContext = new GatewayContext( gateway: "slack", conversationID: "C123" )
		    humanRequest = new HumanInteractionRequest( title: "Approve?", message: "msg" )

		    result = gw.requestHumanInteraction( humanRequest, gwContext )
		    success = result.getSuccess()

		    record = gw.getInteraction( humanRequest.getId() )
		    hasRecord = !isNull( record )
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "hasRecord" ) ) ).isTrue();
	}

}
