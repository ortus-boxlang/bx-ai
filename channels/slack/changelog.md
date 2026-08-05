# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

----

## [Unreleased]

### 🥊 Added

* Initial release: `SlackGateway` implementing bx-ai's `IGateway` SPI — inbound Slack Events API
  messages, outbound `chat.postMessage` delivery, and human-in-the-loop approvals presented as
  Block Kit Approve/Reject buttons resolved via Slack's Interactivity endpoint.
* `SlackSignature` — Slack's HMAC-SHA256 "v0" request-signing verification, with timestamp
  freshness checking and constant-time comparison.
* `SlackRequestProcessor` + `public/slack.bxm` — the HTTP front controller exposing
  `/events` and `/interactions`, reusing bx-ai's `mcp/transports/HTTPTransport` for CGI/CORS
  handling.
* `ModuleConfig.bx` registers a configured `SlackGateway` in bx-ai's `gatewayRegistry()` under
  `"slack"` at runtime start (with a lazy fallback on the first real HTTP request, since
  cross-module startup order isn't guaranteed) — `aiGateway( "slack", options )` resolves and
  reconfigures that same instance (options merge onto its existing configuration rather than
  replacing it), so no event listener wiring is required and no changes to bx-ai itself are needed.
* `onSlackInboundMessage` / `onSlackInteractionResolved` interception points so applications can
  feed an agent or resume a suspended run without this module needing to know about either.

* Bot-message filtering, system-subtype filtering, unique per-button `action_id`s, an
  Eve-inspired "answered-card" update on resolution, and mrkdwn text truncation for Slack's
  3000-char section limit.

### 🐛 Fixed

* This channel now lives in bx-ai's own monorepo (`channels/slack/`) instead of a separate
  `bx-ai-gateway-slack` repository. That separate-repo setup required CommandBox `box install`
  + a manually-symlinked `bx-ai` module for CI, which surfaced a real, previously-undiagnosed bug
  (see next item) as a confusing module-loading/registry issue — several CI rounds were spent
  chasing module-boundary theories before finding the actual cause. Testing now runs through
  bx-ai's own Java/Gradle suite (`src/test/java/ortus/boxlang/ai/channels/slack/`), loading this
  module in the same `BoxRuntime` as bx-ai itself.
* **Root cause of the above**: `request` is a BoxLang built-in scope name, and a variable/parameter
  named `request` resolves to that ambient scope instead of the local value — even when declared
  `required HumanInteractionRequest request` on a function, or `var request = ...` inside a
  closure. `SlackGateway.requestHumanInteraction()`'s parameters and the two test spec files were
  all using `request` as a variable name; renamed to `humanRequest` throughout.
* `SlackGateway.bx` used `SlackSignature::verify()` without importing `SlackSignature`.
* `public/slack.bxm` was missing a semicolon on its `import` statement.
* `SlackRequestProcessor.resolveGateway()` returned `null` immediately if `gatewayRegistry()`
  itself threw (not-yet-callable at startup), skipping the lazy-registration fallback entirely —
  the whole point of that fallback was to handle exactly this case.
* `handleEvents()` called `jsonDeserialize()` on the raw request body unconditionally; a
  malformed/non-JSON body now gets a clean 400 instead of an uncaught exception surfacing as a 500
  (which would make Slack retry).
* `handleEvents()` fired `onSlackInboundMessage` even when `parseInbound()` filtered every message
  out (e.g. an all-bot-messages event) — now only fires when there's at least one message.
* `requestHumanInteraction()` recorded a pending interaction even when the Slack API call failed,
  leaving an orphaned entry a human never actually saw but that could still later be "resolved".
  Now only tracked on a successful delivery.
