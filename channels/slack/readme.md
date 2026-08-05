# ⚡︎ BoxLang AI Slack Gateway

```
|:------------------------------------------------------:|
| ⚡︎ B o x L a n g ⚡︎
| Dynamic : Modular : Productive |
| :----------------------------: |
```

<blockquote>
	Copyright Since 2023 by Ortus Solutions, Corp
	<br>
	<a href="https://www.boxlang.io">www.boxlang.io</a> |
	<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</blockquote>

<p>&nbsp;</p>

Slack gateway for [BoxLang AI (bx-ai)](https://github.com/ortus-boxlang/bx-ai) — presents inbound Slack messages and human-in-the-loop agent approvals as interactive Slack messages, implementing bx-ai's `IGateway` SPI.

----

## What this module does

bx-ai defines a gateway-neutral abstraction (`IGateway`) so an agent's suspended human-in-the-loop
approvals — and any inbound/outbound messaging — can be presented over any platform (CLI, generic
HTTP, Slack, ...) without the agent or middleware code knowing which one is on the other end. This
module is the Slack implementation:

- **Inbound messages** — Slack Events API `message`/`app_mention` callbacks are verified and
  normalized into bx-ai `GatewayMessage`s.
- **Outbound messages** — plain events are posted to a channel via `chat.postMessage`.
- **Human-in-the-loop approvals** — a pending tool-call approval is posted as a Block Kit message
  with **Approve**/**Reject** buttons; the human's click arrives via Slack's Interactivity endpoint
  and atomically resolves the pending interaction (a duplicate/retried click is rejected, not
  double-applied).

Like bx-ai's own `HttpGateway`, this is a gateway with real network exposure, so every inbound
request — an Events API callback or a button click — is verified against Slack's own
[request signing scheme](https://api.slack.com/authentication/verifying-requests-from-slack)
before it's trusted.

**Scope note:** Slack buttons don't have a natural way to submit *edited* arguments inline (that
needs a modal), so only Approve/Reject are offered as buttons in this version — not "edit". This
gateway also does not itself call `AiAgent.resume()` — that requires knowing which agent/checkpointer
an interaction belongs to, which is your application's state, not this module's. Set
`GatewayContext.threadID` (and `agentID`, if useful) when presenting a request; both are persisted
and available on the resolved interaction record so your app can correlate the decision back to
`agent.resume(decision, threadID, editedData)` itself — typically from an `onSlackInteractionResolved`
interceptor (see below).

## Setup

1. Create a Slack app at <https://api.slack.com/apps> (or reuse an existing one).
2. Under **OAuth & Permissions**, add at least the `chat:write` bot scope, then install the app to
   your workspace and copy the **Bot User OAuth Token** (`xoxb-...`).
3. Under **Basic Information**, copy the **Signing Secret**.
4. Under **Event Subscriptions**, enable events and set the Request URL to:
   `https://yourhost/~bxaiSlack/slack.bxm/events`
   Subscribe to the bot events you need (e.g. `message.channels`, `app_mention`).
5. Under **Interactivity & Shortcuts**, enable interactivity and set the Request URL to:
   `https://yourhost/~bxaiSlack/slack.bxm/interactions`

Slack requires both URLs to respond to a verification handshake / return `200` quickly — this
module handles the Events API `url_verification` challenge and always acks interactivity payloads
immediately.

Configure the bot token and signing secret via environment variables (read automatically by this
module's `ModuleConfig.bx`), or pass them explicitly to `aiGateway()`:

```bash
export SLACK_BOT_TOKEN=xoxb-...
export SLACK_SIGNING_SECRET=...
```

## How it integrates with bx-ai

This module registers a `SlackGateway` instance in bx-ai's `gatewayRegistry()` — keyed as
`"slack"` — the moment it loads, configured from the environment variables above. No wiring is
required on your part for this to happen; simply installing/enabling the module is enough.

`aiGateway( "slack", options )` resolves that same registered instance and reconfigures it in
place with `options` (merged onto whatever it already has — so a partial call doesn't wipe
previously-set values). This means `aiGateway( "slack" )` and `gatewayRegistry().get( "slack" )`
return the **same object** throughout your application's lifetime; there's exactly one Slack
gateway per running instance.

**Timing note:** this module registers the gateway as a best-effort step during BoxLang's own
startup broadcast, and separately guarantees registration on the first real Slack HTTP request —
both are reliable in practice, but neither is instantaneous at the very first moment the runtime
comes up. If you need a Slack gateway available at the earliest possible point in your own
application's startup code (before any request), skip the name lookup and construct/register it
yourself, which always works regardless of load order:

```js
import bxModules.bxaiSlack.models.SlackGateway;
gatewayRegistry().register( new SlackGateway().configure( { botToken: "xoxb-...", signingSecret: "..." } ) )
```

## Usage

```js
// If SLACK_BOT_TOKEN / SLACK_SIGNING_SECRET are set, the module already registered a working
// gateway at load time — you can start using it immediately:
gw = aiGateway( "slack" )
// ...or override/set credentials explicitly (merges onto the module's existing configuration):
// gw = aiGateway( "slack", { botToken: "xoxb-...", signingSecret: "..." } )
```

Present a human-in-the-loop approval (typically from your own `IApprovalPolicy`/gateway-attached
`HumanInTheLoopMiddleware` flow — see bx-ai's HITL docs):

```js
import bxModules.bxai.models.gateway.contracts.GatewayContext;
import bxModules.bxai.models.gateway.contracts.HumanInteractionRequest;

gw = aiGateway( "slack" )

context = new GatewayContext(
	gateway       : "slack",
	conversationID: "C0123456789", // the Slack channel to post to
	threadID      : agentRun.threadId,
	agentID       : "my-agent"
)

request = new HumanInteractionRequest(
	title        : "Approve tool call?",
	message      : "The agent wants to delete a file.",
	pendingAction: { toolName: "deleteFile", toolArgs: { path: "/tmp/report.csv" } }
)

gw.requestHumanInteraction( request, context )
```

React to a resolved decision and resume your agent — this is your application's responsibility,
via the `onSlackInteractionResolved` interceptor this module fires:

```js
class {
	function onSlackInteractionResolved( data ){
		// data.requestID, data.decision (HumanInteractionDecision), data.gateway, data.record
		myAgent.resume(
			decision  : data.decision.getDecision(),
			threadID  : data.record.context.getThreadID(),
			editedData: data.decision.getEditedData()
		)
	}
}
```

Inbound Slack messages (not tied to an interaction) fire `onSlackInboundMessage` with normalized
`GatewayMessage`s — wire that up to feed an agent, log, or route however your app needs.

## Directory Structure

This channel lives inside the [bx-ai](https://github.com/ortus-boxlang/bx-ai) monorepo at
`channels/slack/` — each channel gets its own folder with its own `box.json` (so it stays
independently packagable/publishable to ForgeBox), but is developed and tested alongside bx-ai
core rather than in a separate repository.

- `models/SlackGateway.bx` — the `IGateway` implementation
- `models/SlackSignature.bx` — Slack's HMAC-SHA256 "v0" request-signing scheme
- `models/SlackRequestProcessor.bx` — the HTTP front-controller logic (`route()` is the testable
  seam, independent of any running web server)
- `public/slack.bxm` — the real HTTP entry point Slack talks to
- `tests/specs` — TestBox specs (reference/documentation coverage; see "Running Tests" below for
  the CI-verified suite)

## Running Tests

This channel's CI-verified tests live in bx-ai's own Java/Gradle suite, not TestBox — running
everything in a single `BoxRuntime` (bx-ai's own module plus this one, loaded the same way
`BaseIntegrationTest` loads bx-ai) avoids the cross-module gotchas of a separate-repo,
CommandBox/`box install`-based CI pipeline. From the repo root:

```bash
./gradlew test --tests "ortus.boxlang.ai.channels.slack.*"
```

See `src/test/java/ortus/boxlang/ai/channels/slack/` — `BaseSlackChannelTest` is the loader,
`SlackSignatureTest`/`SlackGatewayTest`/`SlackRequestProcessorTest` are the suites.

The TestBox specs under `tests/specs/` still work standalone (`box install && testbox/run` from
within `channels/slack/`) if you want to exercise this channel outside the monorepo, e.g. before
an independent ForgeBox publish.

## Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://www.ortussolutions.com). Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more. If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

> "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
