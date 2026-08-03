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
 * ----------------------------------------------------------------------------------
 * Most cases here instantiate SlackGateway directly (`new SlackGateway().configure({...})`)
 * rather than going through `aiGateway( "slack" )` — the module registers ONE shared
 * instance in gatewayRegistry() at load time (see ModuleConfig.bx), and `aiGateway()`
 * reconfigures/returns that SAME instance on every call. Sharing it across cases would
 * leak state (e.g. requestTTLSeconds: -1 from the expiry case would poison every later
 * interaction). Direct instantiation keeps each case isolated.
 */
package ortus.boxlang.ai.channels.slack;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

@DisplayName( "SlackGateway — bx-ai IGateway implementation backed by Slack" )
public class SlackGatewayTest extends BaseSlackChannelTest {

	/** Common imports every case needs, prefixed onto each script. */
	private static final String IMPORTS = """
	    import bxModules.bxaiSlack.models.SlackGateway;
	    import bxModules.bxai.models.gateway.contracts.GatewayContext;
	    import bxModules.bxai.models.gateway.contracts.GatewayEvent;
	    import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;
	    import bxModules.bxai.models.gateway.contracts.HumanInteractionDecision;
	    """;

	@DisplayName( "configure() merges options instead of replacing them wholesale" )
	@Test
	public void testConfigureMergesOptions() {
		runtime.executeSource(
		    IMPORTS + """
		    gw = new SlackGateway().configure( { botToken: "xoxb-first", signingSecret: "secret-1" } )
		    gw.configure( { apiBaseUrl: "https://slack.test/api" } )

		    botToken      = gw.getOptions().botToken
		    signingSecret = gw.getOptions().signingSecret
		    apiBaseUrl    = gw.getOptions().apiBaseUrl
		    """,
		    context
		);
		assertThat( variables.get( Key.of( "botToken" ) ) ).isEqualTo( "xoxb-first" );
		assertThat( variables.get( Key.of( "signingSecret" ) ) ).isEqualTo( "secret-1" );
		assertThat( variables.get( Key.of( "apiBaseUrl" ) ) ).isEqualTo( "https://slack.test/api" );
	}

	@DisplayName( "verifyInbound() is false without a configured signingSecret" )
	@Test
	public void testVerifyInboundFalseWithoutSigningSecret() {
		runtime.executeSource(
		    IMPORTS + """
		    gw    = new SlackGateway().configure( { botToken: "xoxb-test" } )
		    valid = gw.verifyInbound( {}, { headers: {}, rawBody: "" } )
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "valid" ) ) ).isFalse();
	}

	@DisplayName( "parseInbound() normalizes a Slack event_callback payload" )
	@Test
	public void testParseInboundNormalizesEventCallback() {
		runtime.executeSource(
		    IMPORTS + """
		    gw      = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )
		    payload = {
		        type   : "event_callback",
		        team_id: "T789",
		        event  : { type: "message", channel: "C123", user: "U456", text: "hello", ts: "1700000000.000001" }
		    }

		    messages     = gw.parseInbound( payload, {} )
		    messageCount = messages.len()
		    firstText    = messages[ 1 ].getText()
		    conversation = messages[ 1 ].getContext().getConversationID()
		    userId       = messages[ 1 ].getContext().getUserID()
		    teamId       = messages[ 1 ].getContext().getMetadata().slackTeamId
		    """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "messageCount" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "firstText" ) ) ).isEqualTo( "hello" );
		assertThat( variables.get( Key.of( "conversation" ) ) ).isEqualTo( "C123" );
		assertThat( variables.get( Key.of( "userId" ) ) ).isEqualTo( "U456" );
		assertThat( variables.get( Key.of( "teamId" ) ) ).isEqualTo( "T789" );
	}

	@DisplayName( "parseInbound() returns empty array for non-event_callback payloads" )
	@Test
	public void testParseInboundEmptyForNonEventCallback() {
		runtime.executeSource(
		    IMPORTS + """
		    gw = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )
		    verificationCount = gw.parseInbound( { type: "url_verification", challenge: "xyz" }, {} ).len()
		    emptyCount        = gw.parseInbound( {}, {} ).len()
		    """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "verificationCount" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "emptyCount" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "parseInbound() filters out bot messages to prevent self-echo" )
	@Test
	public void testParseInboundFiltersBotMessages() {
		runtime.executeSource(
		    IMPORTS + """
		    gw      = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )
		    payload = {
		        type : "event_callback",
		        event: { type: "message", channel: "C123", bot_id: "B123", text: "I am a bot", ts: "1700000000.000002" }
		    }
		    messageCount = gw.parseInbound( payload, {} ).len()
		    """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "messageCount" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "parseInbound() filters out system message subtypes except file_share" )
	@Test
	public void testParseInboundFiltersSystemSubtypes() {
		runtime.executeSource(
		    IMPORTS + """
		    gw = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )

		    editPayload = {
		        type : "event_callback",
		        event: { type: "message", subtype: "message_changed", channel: "C123", user: "U456", ts: "1700000000.000003" }
		    }
		    editCount = gw.parseInbound( editPayload, {} ).len()

		    filePayload = {
		        type : "event_callback",
		        event: { type: "message", subtype: "file_share", channel: "C123", user: "U456", text: "here is a file", ts: "1700000000.000004" }
		    }
		    fileCount = gw.parseInbound( filePayload, {} ).len()
		    """,
		    context
		);
		assertThat( variables.getAsInteger( Key.of( "editCount" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "fileCount" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "parseInbound() sets slackThreadTs to ts when thread_ts is absent" )
	@Test
	public void testParseInboundThreadTsFallback() {
		runtime.executeSource(
		    IMPORTS + """
		    gw      = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )
		    payload = {
		        type : "event_callback",
		        event: { type: "message", channel: "C123", user: "U456", text: "hi", ts: "1700000001.000000" }
		    }
		    messages     = gw.parseInbound( payload, {} )
		    slackThreadTs = messages[ 1 ].getContext().getMetadata().slackThreadTs
		    """,
		    context
		);
		assertThat( variables.get( Key.of( "slackThreadTs" ) ) ).isEqualTo( "1700000001.000000" );
	}

	@DisplayName( "requestHumanInteraction() posts Block Kit approve/reject buttons with unique action_ids and tracks a pending interaction" )
	@Test
	public void testRequestHumanInteractionPostsButtons() {
		runtime.executeSource(
		    IMPORTS + """
		    gw = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )

		    captured = { method: "", body: {} }
		    gw.callSlackApi = ( method, body ) => {
		        captured.method = method
		        captured.body   = body
		        return { ok: true, channel: body.channel ?: "", ts: "1700000000.000100" }
		    }

		    gwContext = new GatewayContext( gateway: "slack", conversationID: "C123", threadID: "thread-1" )
		    humanRequest = new HumanInteractionRequest(
		        title        : "Approve tool call?",
		        message      : "The agent wants to call deleteFile(path=/tmp/x)",
		        pendingAction: { toolName: "deleteFile", toolArgs: { path: "/tmp/x" } }
		    )

		    result  = gw.requestHumanInteraction( humanRequest, gwContext )
		    success = result.getSuccess()
		    method  = captured.method
		    elements = captured.body.blocks[ 2 ].elements
		    elementCount = elements.len()
		    action1 = elements[ 1 ].action_id
		    action2 = elements[ 2 ].action_id

		    record = gw.getInteraction( humanRequest.getId() )
		    decisionIsNull = isNull( record.decision )
		    promptBlockCount = record.promptBlocks.len()
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "method" ) ) ).isEqualTo( "chat.postMessage" );
		assertThat( variables.getAsInteger( Key.of( "elementCount" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "action1" ) ) ).isNotEqualTo( variables.get( Key.of( "action2" ) ) );
		assertThat( variables.get( Key.of( "action1" ) ).toString() ).contains( "button:1" );
		assertThat( variables.get( Key.of( "action2" ) ).toString() ).contains( "button:2" );
		assertThat( variables.getAsBoolean( Key.of( "decisionIsNull" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "promptBlockCount" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "requestHumanInteraction() does not track a pending interaction when Slack delivery fails" )
	@Test
	public void testRequestHumanInteractionSkipsTrackingOnDeliveryFailure() {
		runtime.executeSource(
		    IMPORTS + """
		    gw = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )
		    gw.callSlackApi = ( method, body ) => {
		        return { ok: false, error: "channel_not_found" }
		    }

		    gwContext    = new GatewayContext( gateway: "slack", conversationID: "C123" )
		    humanRequest = new HumanInteractionRequest( title: "Approve?", message: "msg" )

		    result  = gw.requestHumanInteraction( humanRequest, gwContext )
		    success = result.getSuccess()
		    record  = gw.getInteraction( humanRequest.getId() )
		    hasRecord = !isNull( record )
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "success" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "hasRecord" ) ) ).isFalse();
	}

	@DisplayName( "resolveInteraction() atomically resolves and rejects a duplicate" )
	@Test
	public void testResolveInteractionAtomicallyResolvesAndRejectsDuplicate() {
		runtime.executeSource(
		    IMPORTS + """
		    gw = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )
		    gw.callSlackApi = ( method, body ) => {
		        return { ok: true, channel: body.channel ?: "", ts: "1700000000.000200" }
		    }

		    gwContext    = new GatewayContext( gateway: "slack", conversationID: "C123" )
		    humanRequest = new HumanInteractionRequest( title: "Approve?", message: "msg" )
		    gw.requestHumanInteraction( humanRequest, gwContext )

		    decision = new HumanInteractionDecision( requestID: humanRequest.getId(), decision: "approve", decidedBy: "U999" )
		    resolved = gw.resolveInteraction( humanRequest.getId(), decision )
		    resolvedDecision = resolved.getDecision()

		    threwAlreadyResolved = false
		    try {
		        gw.resolveInteraction( humanRequest.getId(), decision )
		    } catch ( InteractionAlreadyResolved e ) {
		        threwAlreadyResolved = true
		    }
		    """,
		    context
		);
		assertThat( variables.get( Key.of( "resolvedDecision" ) ) ).isEqualTo( "approve" );
		assertThat( variables.getAsBoolean( Key.of( "threwAlreadyResolved" ) ) ).isTrue();
	}

	@DisplayName( "resolveInteraction() rejects an unknown requestID" )
	@Test
	public void testResolveInteractionRejectsUnknownRequestId() {
		runtime.executeSource(
		    IMPORTS + """
		    gw = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )
		    decision = new HumanInteractionDecision( requestID: "unknown-id", decision: "approve" )

		    threwNotFound = false
		    try {
		        gw.resolveInteraction( "unknown-id", decision )
		    } catch ( InteractionNotFound e ) {
		        threwNotFound = true
		    }
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "threwNotFound" ) ) ).isTrue();
	}

	@DisplayName( "resolveInteraction() rejects an expired interaction" )
	@Test
	public void testResolveInteractionRejectsExpired() {
		runtime.executeSource(
		    IMPORTS + """
		    gw = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh", requestTTLSeconds: -1 } )
		    gw.callSlackApi = ( method, body ) => {
		        return { ok: true, channel: body.channel ?: "", ts: "1700000000.000300" }
		    }

		    gwContext    = new GatewayContext( gateway: "slack", conversationID: "C123" )
		    humanRequest = new HumanInteractionRequest( title: "Approve?", message: "msg" )
		    gw.requestHumanInteraction( humanRequest, gwContext )

		    decision = new HumanInteractionDecision( requestID: humanRequest.getId(), decision: "approve" )
		    threwExpired = false
		    try {
		        gw.resolveInteraction( humanRequest.getId(), decision )
		    } catch ( InteractionExpired e ) {
		        threwExpired = true
		    }
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "threwExpired" ) ) ).isTrue();
	}

	@DisplayName( "parseHumanDecision() builds a HumanInteractionDecision from an interaction.decision event" )
	@Test
	public void testParseHumanDecisionBuildsDecision() {
		runtime.executeSource(
		    IMPORTS + """
		    gw    = new SlackGateway().configure( { botToken: "xoxb-test", signingSecret: "shhh" } )
		    event = new GatewayEvent(
		        type: "interaction.decision",
		        data: { requestID: "req-1", decision: "reject", decidedBy: "U123" }
		    )

		    decision   = gw.parseHumanDecision( event )
		    requestID  = decision.getRequestID()
		    theDecision = decision.getDecision()
		    decidedBy  = decision.getDecidedBy()
		    """,
		    context
		);
		assertThat( variables.get( Key.of( "requestID" ) ) ).isEqualTo( "req-1" );
		assertThat( variables.get( Key.of( "theDecision" ) ) ).isEqualTo( "reject" );
		assertThat( variables.get( Key.of( "decidedBy" ) ) ).isEqualTo( "U123" );
	}

}
