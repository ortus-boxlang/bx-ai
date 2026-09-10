# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### ⚠️ Migration

- **Minimum BoxLang runtime is now 1.14.0** (`box.json` `minimumVersion`, previously 1.8.0). BoxLang ≤1.13 resolves unqualified identifiers inside a `catch` body against the class `variables` scope before locals and `arguments`; 1.14.0 fixed this. The module still qualifies its own catch reads, but middleware, tools, and interceptors authored against this module should not rely on older runtimes. The build now compiles and tests against 1.17.0.

### 🥊 Added

- **Transport-agnostic gateway processing API**: `GatewayRequestProcessor` now exposes `processInbound()`, `processHandshake()`, `readInteraction()`, and `submitDecision()` as statics that touch no transport at all — they take the pieces of a request (gateway name, raw body, headers, query params) and return a normalized `{ statusCode, body, contentType, headers }` result. A host framework that already has a router (ColdBox's `route( "/gateways" ).toAiGateway()` terminator) mounts the gateway surface wherever it likes instead of being tied to `cgi.PATH_INFO` and `public/gateway.bxm`. `processHttp()` is now a thin layer over the same statics, so the existing `/~bxai/gateway.bxm` surface is unchanged.
- **`processInbound()` dispatches, not just parses**: pass a `GatewaySession` and every parsed message is handed to `handleInbound()` and acked `202` immediately — the returned BoxFutures are deliberately never joined, since a platform webhook times out in seconds while an agent turn does not. The response reports which thread each message landed on (`{ accepted, messages: [ { id, threadId } ] }`, plus an `X-Thread-Id` header for single-message events) so a caller can correlate the reply that arrives later. Called without a session it parses only and returns `200` with the normalized messages, exactly as before.
- **`IGateway.verifyHandshake()` + the `"verifyHandshake"` capability**: answers the GET a platform sends to verify a webhook URL before it starts POSTing to it (Meta's WhatsApp Cloud `hub.challenge` echo being the canonical example). Defaults to `GatewayCapabilityNotSupported`, so only gateways whose platform performs one override it; `MockGateway` is the reference implementation, and `processHandshake()` answers `405` for any gateway that does not declare it.
- **`GatewaySession.resolveThreadId()` is now public**: pure and side-effect free, it was already the only way to know which thread a message resolves to. A caller that dispatches without waiting on the turn needs it to report where the work landed, and `getQueueDepth()`/`cancelRun()` are addressed by a thread id the caller has to be able to compute in the first place.

- **Cohere streaming tool loop**: `CohereService.chatStream()` forwarded only `parsedChunk.text` and hardcoded `toolCalls: []`, so tools bound to a streamed Cohere call were sent to the model and then silently ignored — the stream ended on the tool plan and no tool ever ran. The stream now accumulates Cohere v1's tool events (`tool-calls-chunk`'s per-index `tool_call_delta`, whose `parameters` arrive as partial JSON, plus the authoritative `tool-calls-generation`, falling back to `stream-end.response.tool_calls`) into the same normalized tool calls the blocking path produces, then runs the full tool loop: `beforeToolCall`/`wrapToolCall`/`afterToolCall`, the same "decide, then execute" split so nothing runs while any call could still suspend, `afterToolBatch`/`defer` participation with a combined suspension carrying `assistantMessage` + `resumeLedger`, and a `tool_results` follow-up turn that honours `interactionCount`. `chatStream()` reads `_resumeContext` like `chat()` does and finishes a suspended batch through a new `resumeToolBatchStream()` — no LLM replay, nothing already executed runs twice. The tool plan is normalized onto `delta.reasoning` (never merged into `content`), and the SSE transport now goes through `wrapLLMCall` (context carries `stream: true` and an `emitSSEChunk` feeder) so record/replay middleware is no longer blind to Cohere streaming. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **Bedrock streaming tool loop**: `BedrockService.chatStream()` hardcoded `toolCalls: []` and `transformStreamChunk()` discarded `content_block_start`/`content_block_stop` outright, so a streamed Claude-on-Bedrock tool call was invisible — tools were sent to the model and the stream simply ended on the tool plan with no tool ever running. The stream now accumulates Anthropic's tool events (`content_block_start` of type `tool_use` → `input_json_delta` fragments → `content_block_stop`, where the arguments are parsed) into the same normalized tool calls the blocking path produces, emits them on the standard `choices[].delta.tool_calls` envelope, populates `streamState.toolCalls` for `afterLLMCall`, and then runs the full tool loop: `beforeToolCall`/`wrapToolCall`/`afterToolCall`, the same "decide, then execute" split so nothing runs while any call could still suspend, `afterToolBatch`/`defer` participation with a combined suspension carrying `assistantMessage` + `resumeLedger`, and a `tool_result` follow-up turn that honours `interactionCount` (it re-streams the continuation rather than falling back to a blocking call). `chatStream()` reads `_resumeContext` like `chat()` does and finishes a suspended batch through a new `resumeToolBatchStream()` — no LLM replay, nothing already executed runs twice. The `InvokeModelWithResponseStream` transport now goes through `wrapLLMCall` (its inner function returns the raw event-stream body), so record/replay middleware is no longer blind to Bedrock streaming. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **Structured output on `BedrockService.chatStream()`**: `.schema()` combined with streaming injected no forced tool on Bedrock and the caller silently got prose. The stream path now injects the same `structured_output` forced tool + `tool_choice` sync `chat()` uses, accumulates that block's `input_json_delta` fragments separately from the real tool-call loop (so `beforeToolCall`/execution/a follow-up turn never fire for the schema carrier, and it is never emitted as a `tool_calls` chunk), and at end of stream populates the schema — emitted on the normalized envelope as `choices[].delta.structured_output` and returned from `chatStream()`, matching `ClaudeService`. Fails loud with `StructuredOutputError` (including the `stop_reason`) if the block never arrives. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **OpenAI-shaped model families on Bedrock now support tools and structured output**: `transformRequestForOpenAI()` emitted only `{ messages, max_tokens, temperature, top_p }` and `"openai"` sat in `NO_TOOL_SUPPORT_FAMILIES`, so `openai.gpt-oss-20b/120b` — and every family that falls through to the OpenAI chat-completions shape on Bedrock (Amazon Nova, DeepSeek, Qwen, AI21 Jamba, modern Mistral, GLM, Kimi, Nemotron, Gemma) — threw `UnsupportedProviderCapability` for tools and had `tool_choice` / `response_format` / `reasoning_effort` / `stop` / `seed` / penalties silently dropped. The transform is now a passthrough-with-exclusions (mirroring the Claude one): everything the caller sets reaches the wire, tools are formatted with the same `getSchema()` helper `OpenAIService` uses, and `max_completion_tokens` is emitted (the field AWS's own InvokeModel examples use, with `max_tokens` accepted as an inbound alias). `chat()` runs OpenAI-shaped `tool_calls` through the SAME tool loop Claude uses — `beforeToolCall` / `wrapToolCall` / `afterToolCall`, the decide-then-execute split, `afterToolBatch` / `defer` batch suspension and `resumeToolBatch` — via six small dialect helpers that are now the only family-specific part of it, so there is one loop, not two. `chatStream()` accumulates `delta.tool_calls` fragments (`index` / `id` / `function.name` / partial `function.arguments`) into the same normalized `streamState.toolCalls` shape the Claude accumulator produces, closes them on `finish_reason: "tool_calls"`, and flows into the existing streaming tool loop and `resumeToolBatchStream()`. Structured output goes on the wire as a native `response_format` json_schema block on both paths. The capability gate now only blocks families whose transform genuinely has no representation for tools (Titan, Llama, legacy Mistral, legacy Cohere Command). ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **Cohere on Bedrock has a real request transform**: `transformRequestForModel()` routed the `cohere` family to `transformRequestForClaude()`, which builds an `anthropic_version` / `messages` body Cohere rejects outright — so Cohere-on-Bedrock was never functional end to end even though `transformResponseFromCohere()` already understood both response shapes. New `transformRequestForCohere()` builds Command R / R+'s chat body (`message`, `chat_history` with `USER`/`CHATBOT`/`SYSTEM` roles, `preamble` from system messages, `max_tokens`, `temperature`, `p`, `k`, `stop_sequences`, `tools` with `parameter_definitions`, and `tool_results` on a follow-up turn — where `message` is correctly omitted, since Cohere accepts one or the other) and legacy Command's `{ prompt, ... }` generate body for `cohere.command-text-*` / `command-light-text-*`. `transformStreamChunk()` gained a Cohere branch for the `event_type` vocabulary (`text-generation`, `tool-calls-generation`, `stream-end` with `mapCohereFinishReason` + `meta.billed_units` usage) plus the legacy `{ text, finish_reason }` generate stream. Request, response and stream switches now agree for all three Cohere shapes. Command R tool calling works sync and streamed; legacy Command still throws `UnsupportedProviderCapability` for tools, and schema-typed structured output stays gated for the whole family because Bedrock's Cohere body documents no `response_format`. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **`cache_control` survives on Claude-on-Bedrock system prompts**: `transformRequestForClaude()` flattened every system message to a plain string, so a `cache_control` marker — on the message or inside already block-shaped content — was silently dropped, taking prompt caching on the (usually largest) part of the prompt with it. `system` is now emitted as an array of `{ type: "text", text, cache_control? }` blocks whenever a system message actually needs it, and stays a plain string otherwise, so nothing changes for the common case. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **Bedrock speaks the Converse / ConverseStream API by default**: `BedrockService` hand-built each vendor's native InvokeModel body (Anthropic Messages for Claude, chat-completions for gpt-oss, `message`/`chat_history` for Cohere, `inputText` for Titan, `prompt` for Llama/Mistral) and parsed each vendor's native response and stream — six per-family switch arms × {request, response, stream}, where every gap found so far (#227, #233, #248, #249) was one arm disagreeing with the others. New `transformRequestForConverse()` / `transformResponseFromConverse()` collapse all three switches into one model-agnostic pair: typed content blocks (`text`, `image`, `document`, `toolUse`, `toolResult`, `reasoningContent` with its signature round-tripped, `cachePoint`), `system[]`, `inferenceConfig`, a `toolConfig` built from the same tool schemas with `tool_choice` mapped onto the `ToolChoice` union, first-class `guardrailConfig` / `performanceConfig` (from the same `providerOptions` the header approach reads — headers are still used on the invoke path, and suppressed on the Converse one so nothing is sent twice), `additionalModelResponseFieldPaths` passthrough, and `additionalModelRequestFields` for everything Converse does not model itself (`thinking`, `top_k`, `reasoning_effort`, `anthropic_beta`, …). `cache_control` on a system message or a message becomes a sibling `cachePoint` block. Multimodal maps both the OpenAI `image_url` data-URI shape and the Anthropic `source.base64` shape; a remote `http(s)` URL is dropped with a warning rather than faked, since Converse's `ImageSource`/`DocumentSource` carry bytes only. Consecutive same-role turns are merged into one message, so a multi-tool turn keeps Converse's strict user/assistant alternation. `stopReason` maps onto the normalized `finish_reason` (`end_turn`→`stop`, `tool_use`→`tool_calls`, `max_tokens`→`length`, `stop_sequence`→`stop`, `guardrail_intervened`/`content_filtered`→`content_filter`, and the malformed/context-window reasons), and `usage` reports `cache_read_input_tokens`/`cache_creation_input_tokens` alongside cache-inclusive OpenAI-shaped totals — `total_tokens` is always `prompt_tokens + completion_tokens` on both the sync and streaming paths (AWS's own `usage.totalTokens` is cache-EXCLUSIVE and is kept verbatim under `bedrock.totalTokens`) — with `metrics.latencyMs` and the guardrail `trace` namespaced under `bedrock`. `toolResult.status` is emitted only for a tool call that actually failed (blocked, cancelled, tool not found): `success` is Converse's default and AWS documents the member as supported by Nova and Claude 3/4 only, so a model that rejects it now only ever meets it on a genuine failure. `guardrailConfig.streamProcessingMode` is sent on ConverseStream only, where the member exists. ConverseStream reuses the event-stream framing from #248 — the already-decoded `:event-type` header now reaches `transformStreamChunk()`, which maps `messageStart` / `contentBlockStart.start.toolUse` / `contentBlockDelta.delta.{text,toolUse.input,reasoningContent}` / `contentBlockStop` / `messageStop` / `metadata` onto the SAME `streamState` accumulator, so `drainPendingStreamToolCalls()`, the streaming tool loop and `resumeToolBatchStream()` are unchanged. Structured output is the forced `structured_output` tool for **all** families on the Converse path (the Claude-only gate was a limitation of the per-vendor bodies, not of the models), sync and streamed. The `UnsupportedProviderCapability` gate is narrowed to the InvokeModel path for most families — on Converse an unsupported combination is reported by AWS's own `ValidationException` verbatim — but stays as a pre-flight for the two known-unserviceable cases: tools on Titan, and schema-typed structured output (a forced `toolChoice`) on Titan / Llama / Mistral / legacy Cohere. New `countTokens( chatRequest )` calls `/model/{id}/count-tokens`, sending `input.converse` or `input.invokeModel` according to the API the request itself would use. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **Bedrock API selection and automatic InvokeModel fallback**: `providerOptions.bedrockApi` (`"converse"` — the default — or `"invoke"`), a `bedrockApi` module/service setting, and the `BOXLANG_MODULES_BXAI_BEDROCK_API` environment variable, resolved by a single `resolveBedrockApi( chatRequest, modelId )` in that precedence (request option > env > `bxai_bedrockApi` in the application scope > module setting > `converse`); an unrecognized value falls through instead of throwing. It also applies the automatic fallbacks: a model id on `NO_CONVERSE_MODEL_PATTERNS` (the text-completion models with no `messages[]` representation — `cohere.command-text-*` / `command-light-text-*` and `ai21.j2-*`; AWS retired its consolidated Converse support matrix, so the list is deliberately short and the runtime check below is the real safety net), and `providerOptions.rawBody`, which is now a genuine InvokeModel-only escape hatch whose struct — or JSON string — replaces the request transform wholesale. A Converse `ValidationException` whose message reports the **model or operation** as unsupported rebuilds the body and retries **once** over InvokeModel — sync and streaming — and nothing else ever falls back: a throttle, an access denial, a genuinely malformed request, or a feature-level 400 (`does not support toolChoice` / `toolResult.status` / images / documents / system prompts) surfaces with AWS's own wording, since re-sending an unsupported FEATURE over the other API only replaces an accurate message with a generic one. The retry is also refused once the conversation is already in the Converse dialect — from turn 2 of a tool loop the recorded `toolUse`/`toolResult` blocks have no InvokeModel representation, so the original error surfaces instead — and, when streaming, once any chunk has reached the caller: a `validationException` frame delivered mid-stream ends the call rather than replaying output the caller has already consumed. A fallback rebuilds the body against the other API, so `beforeLLMCall` and `wrapLLMCall` are re-fired over the new packet with `fallbackRetry: true` in their context (middleware that must not double-count can tell the legs apart; middleware that substitutes the transport can still answer the retry leg), while `onAIChatRequest` — a per-application-call event — fires exactly once, and the recovered failure is not announced on `onAIError`. The chosen API is recorded on the request, so the tool loop's later turns and any resume speak the API the conversation is actually being built in. Each fallback is logged once per model id at `info` on the `ai` log, via a JVM-static set (aiService() builds a fresh provider per call, so an instance-scoped set would make "once" mean "every request"). ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

### 🐛 Fixed

- An inbound event whose body is not valid JSON is now rejected with `400` and a message naming the parse failure, instead of surfacing as a generic `500`.
- OpenAI-compatible chat providers now fail with a `ProviderError` when a response omits `choices` and use top-level `message` when present, preventing intermittent Mistral tool-call crashes from `KeyNotFoundException`.

- **Bedrock exception frames with an empty payload report their `:error-message` header**: a throttled or unavailable ConverseStream call produced a bare "Bedrock stream error [throttlingException]" with no detail. ([#283](https://github.com/ortus-boxlang/bx-ai/pull/283))

- **Case-only tool-argument edits are honoured**: `BaseService::resolveToolArgs()` compared the pre-hook argument snapshot with the middleware's candidate using `==`, which is case-insensitive for strings, so an edit whose only change was letter case ("paris" → "Paris", a case-corrected enum or ID) read as "no edit" and the tool ran with the original arguments. The comparison is now `compare()`-based, and the snapshot is serialized once per resolution instead of twice. ([#280](https://github.com/ortus-boxlang/bx-ai/pull/280))

- **Empty OpenAI tool arguments no longer break the turn**: OpenAI-compatible providers send `function.arguments` as `""` for a zero-argument tool call, and `jsonDeserialize( "" )` throws — killing the chat, the stream and the resume paths on a perfectly valid call. `OpenAIService` now routes every tool-argument read through a guarded `toolCallArgs()` helper (empty → `{}`, unparseable → `{}` plus a warning log), the same shape `BedrockService` already used. ([#280](https://github.com/ortus-boxlang/bx-ai/pull/280))

- **An "edit" resume decision with no edited data keeps the ledger's arguments**: `AiAgent`'s decision translation wrote `editedArgs = {}` when an "edit" decision carried no `editedData`, and the provider resume paths then invoked the tool with no arguments at all — every value silently replaced by its schema default. The slot is now written only when a non-empty struct was actually supplied, and the provider read sites guard it with `isStruct()` rather than `!isNull()`. ([#280](https://github.com/ortus-boxlang/bx-ai/pull/280))

- **_chatRequest injection follows the callable, not the schema override**: `ClosureTool.doInvoke()` decided whether to inject `_chatRequest` from `methodParameters` — a hand-set schema override used by the MCP/remote-tool path — so an override that did not list the (internal, never-advertised) `_chatRequest` parameter suppressed injection for a closure that explicitly declared it. Injection is now driven by the callable's real declared parameters. Behaviour change: `_chatRequest` was previously handed to EVERY callable, and is now passed only to callables that declare it. ([#280](https://github.com/ortus-boxlang/bx-ai/pull/280))

- **Claude streaming logs an unparseable accumulated tool input**: the `input_json_delta` accumulator swallowed a JSON parse failure in an empty `catch`, so a truncated tool-input stream ran the tool with stale arguments and left no trace anywhere. It now logs a warning naming the tool. Also removed a write-only `streamState.toolCallIndexMap` and two dead `capturedToolCall` locals. ([#280](https://github.com/ortus-boxlang/bx-ai/pull/280))

- **FlightRecorderMiddleware is stream-aware**: Cohere and Bedrock route their SSE transports through `wrapLLMCall`, where the recorder happily taped a streaming call's transport return value as if it were a response body and, on replay, handed that back instead of emitting frames — a stream the caller never receives. Record mode now tapes a `{ stream: true }` marker and passes through; replay mode throws `FlightRecorderStreamingReplayUnsupported` naming the limitation. ([#281](https://github.com/ortus-boxlang/bx-ai/pull/281))

- **Seven providers dropped the tool-call interaction count when streaming**: `PerplexityService`, `HuggingFaceService`, `GrokService`, `GroqService`, `MiniMaxService`, `MistralService` and `OpenRouterService` override `chatStream()` to seed defaults and delegated to `super.chatStream()` without passing `interactionCount`, resetting the max-interaction guard to 0 on every streamed tool round trip (an unbounded tool loop). They now pass it through as DeepSeek and Ollama already did. Pre-existing, not introduced by this stack. ([#279](https://github.com/ortus-boxlang/bx-ai/pull/279))

- **Cohere follow-up rounds now surface provider errors**: A tool-results follow-up turn that came back in Cohere's error shape (`{ message }` with no `text`) was handed back as the assistant's answer, or streamed to the consumer as content. Every round now applies the same `ProviderError` check the first turn does. ([#281](https://github.com/ortus-boxlang/bx-ai/pull/281))

- **Cohere streaming afterLLMCall fires before a truncated tool call throws**: A stream whose tool arguments were truncated (or whose tool call arrived without a name) threw `CohereStreamError` before `afterLLMCall` ran, so audit/logging middleware lost the billed call entirely. The error is now raised after the hook. ([#281](https://github.com/ortus-boxlang/bx-ai/pull/281))

- **Cohere streaming always terminates the stream**: A tool exchange whose follow-up answer was empty emitted no chunk at all, leaving a consumer waiting on a `finish_reason` forever. Both the streamed tool loop and the resumed batch now emit the same empty-content `finish_reason: "stop"` terminator. ([#281](https://github.com/ortus-boxlang/bx-ai/pull/281))

- **Cohere streaming wrapLLMCall parity**: The streamed request body is now serialized inside the `wrapLLMCall` inner function, so a middleware that replaces or edits `context.dataPacket` reaches the wire; the accumulators reset per attempt, so a retrying middleware no longer doubles the streamed content, usage and tool calls; and a middleware that returns a canned struct for a streaming call without feeding `context.emitSSEChunk` now fails with a `CohereStreamError` naming the contract instead of silently producing no chunks. ([#281](https://github.com/ortus-boxlang/bx-ai/pull/281))

- **Cohere beforeLLMCall fires on every LLM round**: Only the first turn of a tool exchange fired `beforeLLMCall`, so a budget or guard middleware could not stop (or rewrite the packet of) any follow-up round. Each round now fires the hook, honours a terminal result, and re-reads a replaced `context.dataPacket`. ([#281](https://github.com/ortus-boxlang/bx-ai/pull/281))

- **Cohere onAITokenCount per follow-up round**: Only the first round of a Cohere tool exchange announced `onAITokenCount`, under-counting every cost and audit listener for multi-round runs. Each round now announces its own billed usage. ([#281](https://github.com/ortus-boxlang/bx-ai/pull/281))

- **Cohere tool schemas without `required` or descriptions no longer throw**: Building a Cohere request from a tool whose schema omitted `required`, `description` or a parameter `type` (routine for MCP tools and hand-rolled `setSchema()` calls) failed while building the packet. The conversion now defaults them, matching `BedrockService.formatToolsForCohere()`. Pre-existing, surfaced by the streaming tool loop. ([#281](https://github.com/ortus-boxlang/bx-ai/pull/281))

- **Cohere stream chunk-parse log qualified**: The error log for a malformed SSE frame read an unqualified `chunk.data` inside a `catch` body, which resolves against the class `variables` scope on BoxLang 1.13 and earlier. ([#279](https://github.com/ortus-boxlang/bx-ai/pull/279))

- **Bedrock stream-callback exceptions now keep their usage accounting**: a caller stream callback that throws mid-stream is announced once on `onAIError` and re-raised only after `onAITokenCount` and `onAIChatResponse` have fired, matching every other provider. ([#279](https://github.com/ortus-boxlang/bx-ai/pull/279))

- **Bedrock streaming failures are always announced**: the stream catch keyed its `onAIError` announce on the exception TYPE, so the malformed-frame, truncated-tool-argument and end-of-stream drain throws were never announced at all; only a frame that has genuinely already announced is now skipped, and the drain's own throw is announced too. ([#284](https://github.com/ortus-boxlang/bx-ai/pull/284))

- **A deliberate Bedrock stream failure is no longer swallowed at debug**: `processBedrockEvent()` caught the `BedrockStreamError` raised while transforming a frame, so truncated tool arguments surfaced downstream as the drain's far vaguer "no content_block_stop" message instead of naming the cause. ([#282](https://github.com/ortus-boxlang/bx-ai/pull/282))

- **A Bedrock event-stream body cut mid-payload now fails loudly**: a frame declaring a payload the delivered bytes do not contain was silently skipped and the stream reported as complete, making a truncated answer indistinguishable from a short one. ([#282](https://github.com/ortus-boxlang/bx-ai/pull/282))

- **A cancelled STREAMED Bedrock tool call records its blocked toolResult**: `chatStream()` broke out of the tool loop leaving a `toolUse` block with no matching `toolResult`, which is a malformed conversation the next Bedrock call rejects outright. `chat()` already did this. ([#282](https://github.com/ortus-boxlang/bx-ai/pull/282))

- **`guardContent` counts as Converse dialect**: the dialect detector's ContentBlock member list had drifted from the one the block mapper uses, so a conversation only expressible in Converse looked re-expressible as a vendor body and a ValidationException triggered an InvokeModel fallback that could only fail again. Both sites now share one list. ([#283](https://github.com/ortus-boxlang/bx-ai/pull/283))

- **`tool_choice: "none"` is honoured on the Converse API**: Converse's ToolChoice union has no `none` member, so the tools went out with no toolChoice at all — Converse's default of `auto`. The `toolConfig` is now omitted entirely, which is the only way to express "none" there. ([#283](https://github.com/ortus-boxlang/bx-ai/pull/283))

- **`response_format` on a Converse call now warns instead of vanishing**: Converse has no such field, so a caller asking for a response shape this API cannot honour ran with no constraint and no indication of it. ([#283](https://github.com/ortus-boxlang/bx-ai/pull/283))

- **The `bedrockHeaders` shorthand goes through the Converse guardrail filter**: appended verbatim it bypassed the filter the loose `x-amzn-bedrock-*` keys pass through, so a caller who set guardrails the header way sent them BOTH as headers and as `guardrailConfig` in the body — the double-specification 400 the filter exists to prevent. ([#284](https://github.com/ortus-boxlang/bx-ai/pull/284))

- **Cross-region `openai.*` inference profiles get `max_completion_tokens`**: the check was a `startsWith( "openai." )`, so `us.openai.gpt-oss-120b-1:0` matched only its region prefix and was sent `max_tokens`, which these models reject. ([#282](https://github.com/ortus-boxlang/bx-ai/pull/282))

- **Claude InvokeModel streams keep their prompt token count**: the `message_delta` chunk emitted a literal `prompt_tokens: 0`, overwriting the `input_tokens` `message_start` had already captured, so `onAITokenCount` reported 0 prompt tokens for every stream with no trailing `amazon-bedrock-invocationMetrics` event. ([#282](https://github.com/ortus-boxlang/bx-ai/pull/282))

- **Cohere, Titan, Llama and Mistral streams now report a finish reason**: these four families never wrote `streamState.stopReason`, so `afterLLMCall` middleware saw an empty `finishReason`; Cohere additionally leaked its raw `COMPLETE`/`MAX_TOKENS` vocabulary instead of the normalized value. ([#284](https://github.com/ortus-boxlang/bx-ai/pull/284))

- **Index-less OpenAI-shaped streamed tool calls no longer collapse into one**: some Bedrock models omit `index` on tool-call fragments, and every fragment then keyed to `"0"` — N parallel calls became one whose arguments were the concatenation of all of them. ([#282](https://github.com/ortus-boxlang/bx-ai/pull/282))

- **Streaming structured output on an OpenAI-shaped model over InvokeModel is refused before the call**: the streaming accumulator can only collect a forced tool block, which no `openai.*` InvokeModel body produces, so the request ran, was billed, streamed prose, and then returned prose where a typed struct was promised. Applies to the Converse-to-InvokeModel fallback leg too. ([#284](https://github.com/ortus-boxlang/bx-ai/pull/284))

- **The once-per-JVM Converse fallback log is keyed on model id AND reason**: `resolveBedrockApi()` writes that key from its read-only resolution path, so a model that had ever resolved to InvokeModel via `rawBody` or the no-Converse list permanently silenced the far more interesting runtime line — a Converse call that FAILED and was re-sent. ([#283](https://github.com/ortus-boxlang/bx-ai/pull/283))

- **A string-replay stream frame may carry an empty `bytes` value**: the frame regex required at least one character, so a ConverseStream replay whose `contentBlockStop` carried no payload never delivered its terminator and the drain then reported the tool arguments as truncated. A non-empty replay body that matches no frames at all now warns rather than streaming nothing in silence. ([#283](https://github.com/ortus-boxlang/bx-ai/pull/283))

- **`countTokens()` documents the scope of its count**: the number is for the API a FRESH logical call would use, not a mid-loop follow-up that inherited a runtime InvokeModel fallback. ([#284](https://github.com/ortus-boxlang/bx-ai/pull/284))

- Tool-argument edits made by middleware are resolved once, in `BaseService::resolveToolArgs`, and honoured everywhere — including `OpenAIService`, which never consulted `ctx.toolArgs` at all and re-parsed `function.arguments` in pass 2, so a middleware that *reassigned* `ctx.toolArgs` was silently ignored there. Three further fixes to what was a per-provider copy in `ClaudeService` and `CohereService`: a HumanInTheLoop "edit" decision was silently discarded whenever an earlier middleware had also rewritten `ctx.toolArgs` (HITL now writes both the normalized and the native slot); change detection used `duplicate().equals()`, which is false for any struct holding a JSON `null`, so such tool calls always looked "edited"; and a middleware that set `ctx.toolArgs` to a non-struct wiped every argument instead of falling back to the native ones. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- Suspend/resume across `ClaudeService`, `CohereService` and `OpenAIService`: the resume ledger now carries each entry's beforeToolCall-resolved `toolArgs`, so a batch that suspended for a human decision executes the same (middleware-rewritten) arguments the non-suspended path would have; previously the resume path re-derived them from the raw tool call and silently dropped every middleware rewrite. Old checkpoints without the field still resume. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **Cohere folds only the trailing tool exchange**: `transformRequestForCohere()` collected `tool_results` from *every* completed exchange in the conversation, so on turn 3 the model received turns 1 and 2's results again as fresh tool output; earlier exchanges are now plain history. Synthetic tool-call ids (`cohere_tool_N`, for a shape that carries no ids) restarted at 1 on every turn and therefore collided across turns, matching a result to the wrong call — they now carry a per-batch discriminator. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **Truncated Cohere streaming tool arguments no longer run the tool with defaults**: `finalizeToolCallBuffers()` deserialized the accumulated per-index `tool_call_delta.parameters` buffer inside a `try`/`catch` that swallowed a parse failure and substituted `{}`. A stream cut off mid-arguments (max_tokens, a dropped connection) — delivering only `{"city":` — therefore reached the tool loop as a *valid-looking* tool call with no arguments, and the tool executed against its defaults: a silent, potentially side-effecting wrong action. A new `parseStreamToolArguments()` (mirroring `BedrockService`'s) now throws `CohereStreamError` naming the tool, the finish reason and the fact that the tool was NOT executed — raised while folding the buffers, before Pass 1 of the batch, so no `beforeToolCall` fires, no tool runs and no follow-up turn is sent. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **A Cohere streaming follow-up that asks for MORE tools no longer discards the exchange**: `sendToolResultsFollowUp()` handled a follow-up response carrying `tool_calls` by calling `chat( thisChatRequest, count + 1 )` on the *unchanged original* request — throwing away the follow-up response and re-sending the original prompt with no tool results at all, so round 1's progress was lost and any side-effecting tool from round 1 could run a second time. The follow-up now continues the same exchange: the decide-then-execute batch machinery is factored out into `executeStreamToolBatch()` (shared with `runStreamToolBatch()`, so `beforeToolCall`/`afterToolBatch`/`wrapToolCall`/`afterToolCall` and batch suspension behave identically in every round), and `sendToolResultsFollowUp()` loops — carrying the same tools/`chat_history`/preamble/params plus the accumulated `tool_results` forward, and applying the same max-interactions guard. A suspension in a later round captures the completed rounds as `assistantMessage.priorToolResults`, which `resumeToolBatch()` replays ahead of the resumed round's results. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **The same discard now fixed on Cohere's resume and blocking paths**: `resumeToolBatch()` — the path that finishes a human-approved batch — and `chat()`'s own multi-step branch both ended in `chat( chatRequest, count + 1 )` on the unchanged original request, so a follow-up that asked for one MORE tool threw away the approved batch's results, re-sent the original prompt with no `tool_results`, and could re-run every already-approved side-effecting tool. Both now share one follow-up loop with the streaming path (`runToolResultsFollowUp()`, with `sendToolResultsFollowUp()` as its text-returning wrapper): the accumulated `tool_results` plus the same tools/`chat_history`/preamble/params are carried forward, each new batch runs through `executeStreamToolBatch()` (so `beforeToolCall`/`afterToolBatch`/`wrapToolCall`/`afterToolCall` behave identically in every round), and the max-interactions guard applies per round. `executeStreamToolBatch()` now also reports the `AiMiddlewareResult` that stopped it, so a later-round cancel or suspension is RETURNED on the blocking path while the streaming path keeps emitting its `middleware_stop` sentinel. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **A resumed Cohere stream with a non-`single` `returnFormat` left the consumer hanging**: `resumeToolBatchStream()` emitted text only when the resumed batch's final response was a simple value, so `returnFormat` `all` / `raw` / `json` / `structuredOutput` produced ZERO chunks — no content and, worse, no terminator, leaving a consumer waiting on `finish_reason` forever. The final response is now rendered into the content chunk (its `text` when it carries one, otherwise its JSON serialization) and a terminating `finish_reason: "stop"` chunk is ALWAYS emitted. The value returned to the caller is unchanged. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **A streamed Cohere tool call with arguments but no name is no longer silently dropped**: `finalizeToolCallBuffers()` skipped any per-index buffer whose `name` never arrived — but the stream had already told the consumer this turn called tools (`finish_reason: "tool_calls"`), so the run continued as if no tool had ever been requested and answered from nothing. A buffer that accumulated arguments without a name now throws `CohereStreamError` naming the index, in the same shape as the truncated-arguments error and raised in the same place (while folding buffers, before Pass 1) so no tool runs and no follow-up turn is sent. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **Cohere invoked tools with the pre-middleware arguments**: both the blocking and the streamed Pass 2 called `tool.invoke( toolCall.parameters, … )` while telling `wrapToolCall` the arguments were `entry.toolArgs` — so a `beforeToolCall` middleware that rewrote `ctx.toolArgs` (a guardrail sanitizing a path, a redactor stripping a secret) saw its replacement honoured in the middleware context and ignored by the tool itself. Both paths now invoke with the resolved arguments, matching `OpenAIService`. Resolution covers both places a middleware can move them — the normalized `ctx.toolArgs` and the provider-shaped `ctx.toolCall.parameters` that `HumanInTheLoopMiddleware`'s "edit" decision replaces — by comparing each against a pre-hook snapshot. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **A Cohere tool call naming an unbound tool threw instead of telling the model**: three lookups used `tools.filter( … ).first()`, which raises on an empty array rather than returning null, so a hallucinated tool name — or a tool renamed between suspend and resume — crashed `chat()`, `resumeToolBatch()` and the streamed batch instead of feeding the model a "Tool not found" result it could recover from. All three now use `chatRequest.getTool( name )`'s `Attempt` (`isPresent()` / `get()`), matching every other provider, and log the miss. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **Bedrock streaming exceptions no longer look like a short answer**: `parseBedrockEventStream()` walked each AWS event-stream frame's prelude but never decoded its header block, so a mid-stream `throttlingException` / `modelStreamErrorException` / `validationException` / `serviceUnavailableException` — delivered as a frame whose `:message-type` header is `exception`, not `event` — arrived as an untyped payload, transformed to a null chunk, and was logged at DEBUG. A throttled stream was indistinguishable from a complete, short response. The decoder now parses the header block per the event-stream spec (1-byte name length + name + value type, with the correct width for every value type) and reads `:message-type`, `:event-type` and `:exception-type`; an exception or error frame throws `BedrockStreamError` naming the exception type and its payload message, announced through the same `onAIError` path the HTTP-error branch uses with `canRetry` set for the transient AWS exception types. The `{"bytes":...}` string fallback is unchanged. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **`AiAgent.resumeStream()` no longer silently replays the LLM on providers without streaming resume**: it passed `_resumeContext` into `stream()` unconditionally, so a provider whose `chatStream()` ignores it would just start the turn over — the human's approval discarded and the approved tools never run, presented as a successful resume. `resumeStream()` now checks `IAiService.supportsStreamingToolResume()` first and throws `AiAgent.StreamingResumeUnsupported` (naming the provider and pointing at `resume()`) instead. Support is detected structurally in `BaseService` — a provider supports it exactly when it has a `resumeToolBatchStream()` — so providers opt in by implementing that method, with no per-provider flag to keep in sync. The guard runs **before** `clearState()`, so a rejected `resumeStream()` leaves the checkpoint intact and `resume()` remains a working fallback rather than pointing at a checkpoint it just destroyed; and it applies **only to the batch path** — a single-tool-call checkpoint replays the LLM by design (`HumanInTheLoopMiddleware.beforeToolCall()` consumes `resumeDecision`), which is provider-agnostic and must keep working on Gemini and every other provider reporting `false`. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **A rejected `resume()` / `resumeStream()` no longer destroys the checkpoint**: both cleared the checkpoint first and only then called `resolvePendingActions()`, so passing the wrong number of per-call decisions threw `AiAgent.ResumeDecisionCountMismatch` **after** the only record of the suspension was gone — the run was unrecoverable and the human's approval had to be collected again from scratch. All validation that can reject the call (the decision-count check, the array-decision-on-a-single-call check and, for `resumeStream()`, the streaming-resume guard) now runs before `clearState()`, so a rejected resume leaves the checkpoint loadable and the caller can simply retry with the corrected decisions. The `StreamingResumeUnsupported` message no longer promises that `resume()` will work, only that it is the path to try. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- **Claude invoked tools with the pre-middleware arguments**: both `chat()`'s and `chatStream()`'s Pass 2 called `tool.invoke( toolCall.input, … )` while telling `wrapToolCall` the arguments were `entry.toolArgs` — so a `beforeToolCall` middleware that rewrote `ctx.toolArgs` saw its replacement honoured in the middleware context and ignored by the tool itself. Both paths now invoke with the resolved arguments the batch entry carries, matching `OpenAIService`. ([#248](https://github.com/ortus-boxlang/bx-ai/issues/248))

- `BedrockService` streaming: an exception thrown by the **caller's own stream callback** was caught by `processBedrockEvent`'s catch and logged at debug as if the frame were unparseable, and the stream carried on (the enclosing `parseBedrockEventStream` guard would have done the same at warning level once the inner one was removed). Found live: the readme idiom `chunk.choices.first()` throws on the usage-only `metadata` / `message_start` chunks (`choices: []`) and nobody ever saw it. The callback now runs outside every guard, so the caller's error propagates out of `chatStream()` — and is announced on `onAIError` first, since the model has already been billed. The readme streaming snippet now guards on `len()`. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- `BedrockService` streaming middleware parity with the sync path: `wrapLLMCall` now receives the same context struct `beforeLLMCall` did (keys stashed in one hook are visible in the other), and the request body is serialized inside the transport closure, so a `wrapLLMCall` middleware that edits `context.dataPacket` in place before calling `handler()` reaches the wire on `chatStream()` as it already did on `chat()`. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- `BedrockService::countTokens()` no longer honours a previous call's Converse → InvokeModel fallback decision: it counts the body the *next* logical call will send, which clears that decision before resolving. The unsupported-model 400 now carries a hint that CountTokens accepts only a bare Anthropic foundation-model id. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **Guardrails set the header way were silently dropped on Converse**: `buildProviderOptionHeaders()` strips every `X-Amzn-Bedrock-Guardrail*` / `X-Amzn-Bedrock-Trace` header on the Converse path (Converse models them in the body), but `transformRequestForConverse()` only ever read the camelCase `guardrailIdentifier` / `guardrailVersion` / `guardrailTrace` provider options — so a caller who configured guardrails under the header names, or inside the `bedrockHeaders` shorthand, ran with **no guardrail at all** on the now-default API, with nothing said. Both spellings (and `bedrockHeaders`) are now sources for `guardrailConfig` and `performanceConfig.latency`, and a guardrail option that cannot be mapped (an identifier with no version) logs a warning naming the fact that the request runs unguarded. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **A Converse → InvokeModel fallback now sticks for the rest of the request**: the fallback rebuilt one body and returned, but every later `resolveBedrockApi()` on the same request — turn 2 of the tool loop, the dialect helpers, a resume — re-resolved the configured default and went back to Converse, while `canFallbackToInvoke()` refuses to fall back again from turn 2. So a model that cannot serve Converse recovered on turn 1 and then failed on turn 2 with the same `ValidationException`, unrecoverably. The chosen API is now recorded on the request itself and honoured first by `resolveBedrockApi()`. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **A suspended Bedrock tool batch resumes in the dialect it was BUILT in**: `resumeToolBatch()` / `resumeToolBatchStream()` re-derived the tool dialect from configuration, so a run that fell back to InvokeModel at request time was resumed as Converse — `extractAssistantToolCalls()` then read zero tool calls off the Claude-shaped recorded turn and every human-approved tool was silently skipped, the run continuing as though nothing had been asked for. The suspend payload now carries `bedrockApi` / `toolDialect` and the resume reads them back; a ledger whose length no longer matches the recorded turn's tool calls throws `ResumeLedgerMismatch` rather than pairing decisions with the wrong calls. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **A streamed Claude/OpenAI assistant turn kept only its tool calls**: `buildStreamAssistantMessage()` recorded `content: toolCalls` (Claude) and `content: ""` (OpenAI-shaped), throwing away every word the model streamed alongside the tool call — the next turn was answered against a turn that never happened. Both branches now carry the accumulated text, and the Claude branch carries the thinking block ahead of it when a `signature_delta` was captured (newly handled on the invoke stream; an unsigned thinking block is omitted rather than sent, since Anthropic rejects one on a tool-use round trip). ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **An InvokeModel-shaped body reaching the Converse response transform is now a hard error**: every read in `transformResponseFromConverse()` safe-navigates, so a body with no `output` (a replay fixture or cache entry recorded against the other API) produced a valid-looking envelope with no text, no tool calls and no error — a silently empty answer, and a green test asserting nothing. It now throws `BedrockError` naming the mismatch. Likewise, a `wrapLLMCall` middleware that returns a non-struct for a *sync* Bedrock call now fails naming the contract, mirroring the check the streaming path already had. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **The fallback decision no longer trips over model ids**: `shouldFallbackToInvoke()` vetoed on a bare `image` / `document` / `system` anywhere in the message, so `"The model amazon.titan-image-generator-v1 does not support the Converse operation"` was read as a request-feature error and surfaced instead of being retried; and its `[^.]{0,40}` proximity window was broken by the dots inside any model id sitting between the subject and the verb. Model ids and ARNs are now stripped before matching, the window tolerates dots, and the feature veto is anchored to field context (`toolConfig`, `toolChoice`, `toolResult.status`, `image block/content/format`, …). The exception name is also read from the `x-amzn-errortype` response header and folded into the error message, so AWS's body-only error shape (`{"message":…}`) is recognised as a `ValidationException` at all. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **A string event-stream replay no longer depends on key order**: `parseBedrockEventStream()` matched replay frames with a pattern anchored to `{"eventType":…,"bytes":…}`, but `jsonSerialize()` emits struct keys alphabetically — so a frame built the obvious way (`{"bytes":…,"eventType":…}`) matched nothing and the whole replay yielded zero chunks. Frames are now matched as whole objects and the event name read off the deserialized struct. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **Converse request transform coercions**: a scalar `image_url` (`{ type: "image_url", image_url: "data:…" }`) silently produced an empty block; an ARRAY `params.system` (the Anthropic block form the invoke path accepts) hit `trim()` and threw, and now becomes `SystemContentBlock`s with `cache_control` carried across as a `cachePoint`; numeric-STRING `max_tokens` / `temperature` / `top_p` were serialized as JSON strings into Converse's typed `inferenceConfig` and rejected, and are now cast; a mime-less `data:` URI reported its media type as the literal `"base64"`; a streamed Converse `stopReason` reached `afterLLMCall` as `streamState.finishReason` in Converse's raw vocabulary instead of the normalized one; a caller-supplied tool result with no status had its TEXT sniffed for this module's own blocked-call marker wording and could be mislabelled an error; and the OpenAI-shaped catch-all families (Nova, DeepSeek, Qwen, AI21 Jamba, …) were sent `max_completion_tokens`, which only the `openai.*` models accept — they now get `max_tokens`. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- **OutputGuard now sees the answer on the default Bedrock Converse path**: `PromptSecurity::getResponseText()` / `setResponseText()` resolved OpenAI, Claude, Gemini, Cohere and Bedrock's native InvokeModel bodies, but not a Converse body — and `afterLLMCall` fires with the RAW Converse response (`BedrockService.chat()` only calls `transformResponseFromConverse()` afterwards). With Converse now the default, `OutputGuardMiddleware` and `LLMGuardMiddleware` resolved `""` on every Bedrock call and silently no-op'd, so a secret in the answer was neither redacted nor blocked. The getter now joins every `output.message.content[].text` block (matching the transform's `char(10)` join, skipping `toolUse` and `reasoningContent`), and the setter collapses the redaction into the first text block, deletes the rest, and leaves the non-text blocks byte-identical in order — Bedrock rejects a modified `reasoningContent` block, and the tool-call path re-sends that same array as the assistant turn. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

- `BedrockService` now routes its tool loops through `BaseService::resolveToolArgs`, and its resume ledger carries each entry's beforeToolCall-resolved `toolArgs`, matching the other providers. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

### 🧠 Updated

- `PromptSecurity::getResponseText()` / `setResponseText()`: the Converse and Claude content-block branches were line-for-line duplicates differing only in what counts as a text block. They now delegate to shared `readTextBlocks()` / `writeTextBlocks()` helpers and are thin adapters over them; behaviour is unchanged. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

### ⚠️ Migration

- **A Bedrock Converse → InvokeModel fallback is TWO transport legs inside one logical call.** When AWS answers a Converse call with a `ValidationException` reporting the model or operation as unsupported, the body is rebuilt and re-sent over InvokeModel, and both legs go through `wrapLLMCall` — so a `wrapLLMCall` middleware fires **twice** for one `chat()` / `chatStream()`, with `context.fallbackRetry` `false` on the Converse leg and `true` on the invoke leg. `beforeLLMCall` is likewise re-fired over the rebuilt packet with `fallbackRetry: true` (the Converse packet it edited no longer exists). `onAIChatRequest` still fires exactly once, and a recovered failure is never announced on `onAIError`. Two consequences to plan for: middleware that **counts** calls (metering, billing, rate limiting) must skip contexts where `fallbackRetry` is true or it will double-count; and `FlightRecorderMiddleware` records **two `llm` interactions** for such a call, so a tape captured against a model that falls back has an extra interaction — re-record affected fixtures rather than hand-editing them, and note that a replay must answer both legs. Middleware that substitutes the transport (a cache, a replay, a test double) must be able to answer the invoke leg too, since the retry is wrapped rather than bypassing the wrapper. ([#272](https://github.com/ortus-boxlang/bx-ai/issues/272))

## [3.4.0] - 2026-09-01

### 🥊 Added

- **Normalized reasoning (`delta.reasoning` / `message.reasoning`)**: reasoning-capable models could always be _enabled_ — `params` passes straight through to the provider body, so `params: { thinking: { type: "enabled", budget_tokens: 10000 } }` (Claude) or `params: { reasoning_effort: "high" }` (OpenAI) already reached the API. But the reasoning that came back was parsed out and **silently dropped**: Claude's stream reader only handled `text_delta`/`input_json_delta`/`tool_use`, and `BaseService` only extracted `delta.content`/`delta.tool_calls`. You paid for thinking tokens and never saw them. Reasoning now surfaces on the **same OpenAI envelope every provider already normalizes onto** — `choices[].delta.reasoning` when streaming, `choices[].message.reasoning` synchronously — so implementers read it identically regardless of which model is behind it, with no provider branching. Providers each spelling it differently on the wire (Anthropic `thinking_delta`, DeepSeek `reasoning_content`) are mapped onto that one key on **both** paths: streaming in `BaseService.sendStreamRequest()` (which OpenAI and every provider extending it already routes through) and synchronously in `BaseService.sendChatRequest()`. `MockService` replaces the transport wholesale, so it applies the identical normalization in its own overrides — otherwise tests written against the mock would be asserting a contract production doesn't have. **Every chat provider is covered**, by one of three routes: those delegating to `super.chat()`/`super.chatStream()` (Grok, Groq, Mistral, DeepSeek, OpenRouter, Perplexity, MiniMax, HuggingFace, DockerModelRunner, OpenAICompatible) and those calling the inherited transport directly (Cohere, Gemini) inherit it from `BaseService`; those building their own chunks get explicit mapping (Claude `thinking_delta`, Ollama `message.thinking`, Claude-on-Bedrock `delta.thinking` plus OpenAI-shaped Bedrock models in either spelling). **Absence is normal, never an error** — a provider or model with no reasoning simply omits the key, and `message.reasoning ?: ""` degrades silently; deliberately _not_ modeled as a capability interface, since reasoning varies per _model_ (Sonnet vs. Haiku, gpt-5 vs. gpt-4o), which a per-provider interface can't express. Reasoning is kept strictly separate from `content` and is never folded into the assistant message persisted to memory — otherwise the model's private thinking would be replayed back to it as if it had said it. `MockService` can script reasoning (`{ content: "...", reasoning: "..." }`, emitted before content just like a real provider), so reasoning-aware consumers are testable offline with no reasoning-capable model.
- **`aiGatewaySession( agent, gateways, policy )`**: wires one `AiAgent` to one-or-more `IGateway` instances for inbound message handling — a message arrives, `GatewaySession` dispatches it as an agent turn (async, off the caller's thread), and relays output back through whichever gateway it arrived on (streamed chunk-by-chunk for gateways that declare `"streaming"`, buffered into one delivery otherwise). `gateways` accepts a single gateway or an array, and each entry can be a string name (resolved via `aiGateway( name )` — core names or anything in `aiGatewayRegistry()`) or an already-constructed `IGateway` instance — mix and match. A second message arriving on a thread that already has a turn in flight is handled per a configurable `policy`: `"reject"` (immediate refusal), `"queue"` (default — buffered, dispatched once the current turn finishes), `"steer"` (spliced into the _live_ turn via `steerRun()` — not a new turn), or `"interrupt"` (`cancelRun()` the current turn, then queue the new message next). `maxQueueDepth` bounds buffered messages per thread before falling back to reject. `session.isRunning()`/`getActiveThreadIds()`/`getQueueDepth( threadId )` for lifecycle/observability queries. Fires `onGatewaySessionCreate` when constructed.
- **`AiAgent.cancelRun( threadId )` / `steerRun( threadId, message )`**: cancel or steer an agent run already in flight, addressed purely by `threadId` — no token to construct or wire up, every agent supports this out of the box. Takes effect at the run's next `beforeLLMCall`/`beforeToolCall` checkpoint: cancelling stops the run with a terminal `AiMiddlewareResult.cancel()`; steering splices a new message into the live request without restarting anything already in progress. Both return `false` as a safe no-op when the thread has no run currently in flight. Fires `onAIAgentRunCancel`/`onAIAgentRunSteer` (with `agent`/`threadId`/`reason`/`input`) whenever one actually affects a run — not on the no-op case.
- **Batch tool-call approvals into one suspension, resume without replaying the LLM call**: when a turn requests multiple tool calls needing approval, they now suspend together as ONE checkpoint instead of one at a time (the rest used to be silently skipped). `agent.resume()`/`resumeStream()` accept a single decision or an array of per-call decisions, and finish the batch directly — no LLM replay, nothing already executed runs twice. Consistent across OpenAI, Claude, Bedrock, and Cohere; streaming batching covers OpenAI and Claude (the only two with streaming tool-call support today).
- **Bedrock provider parity**: bearer-token auth (explicit `bearerToken` or `AWS_BEARER_TOKEN_BEDROCK`, opt-in only and never inferred from `apiKey`), the AWS default credential chain (explicit → env → ECS/EKS container → EC2 IMDSv2, with expiry-aware caching and a negative cache), Guardrails plus `x-amzn-bedrock-*` header passthrough, a `baseURL` endpoint override honoured by both the request URL and the SigV4 `Host` header, and Cohere / Titan-v2 embedding request shapes. ([#227](https://github.com/ortus-boxlang/bx-ai/issues/227))
- New Gateway SPI (`IGateway`, `AgentSuspension`, `GatewayRegistry`, `aiGateway()`) with a `MockGateway` reference implementation — foundation for HTTP/CLI and future platform gateway modules.
- HITL extracted into a `models/hitl/` package: pluggable `IApprovalPolicy` implementations, a `HumanInteractionCoordinator` owning the suspend/resolve lifecycle, and gateway-attached `HumanInTheLoopMiddleware`.
- CLI gateway extracted as the reference `IGateway` implementation; `HumanInTheLoopMiddleware` now always presents through an attached gateway (zero behavior change for existing usage).
- `aiGateway()` now resolves external gateways via `aiGatewayRegistry()` instead of an interception point.
- Generic HTTP/webhook gateway (`HttpGateway`) with HMAC request signing, nonce dedup, TTL-bounded interactions, and atomic decision claims.
- Durable/session approval grants (`approve_always`/`approve_session`) via a pluggable `IDecisionStore` — cache, JDBC, and file-backed implementations, resolved through `aiDecisionStore()`.
- `CliGateway`'s approval prompt now offers `approve_always`/`approve_session`, not just approve/reject/quit.
- `HumanInTheLoopMiddleware` now defaults its `IDecisionStore` from `settings.hitl.decisionStore` instead of never having one.
- **`aiGateway( ..., register, module )`**: opt into auto-registering the constructed gateway into `aiGatewayRegistry()` — `aiGateway( "http", register: true )`, matching `aiAgent()`'s/`aiTool()`'s own auto-register pattern. Defaults to `false`. Renamed the registry accessor BIF from `gatewayRegistry()` to `aiGatewayRegistry()` to match the `aiAgentRegistry()`/`aiToolRegistry()` naming convention (breaking rename — no alias kept).
- **`IGateway.isRunning()` / `onError()`**: two new additive lifecycle hooks, alongside `onMessage()`. `isRunning()` reports whether `start()` has been called and `stop()` hasn't since. `onError()` registers a callback invoked when a gateway's connection drops unexpectedly, so a caller doesn't have to poll `isRunning()` to notice. Both default to safe no-ops; `MockGateway` is now a full reference implementation of both (plus a `simulateError()` test helper), and none of the existing core gateways need to change.
- **Gateway observability events**: `onGatewayConnect`/`onGatewayDisconnect` fire on a real `start()`/`stop()` state transition (never on a redundant call while already in that state) — `BaseGateway.start()`/`stop()` are now template methods handling this and `isRunning()` automatically for every gateway, so a concrete gateway overrides `onStart()`/`onStop()` for its own connect/disconnect logic instead of `start()`/`stop()` directly, and gets the tracking/events for free. `onGatewayMessageReceived`/`onGatewayMessageSent` fire from `parseInbound()`/`deliver()` (implemented in `MockGateway` and `HttpGateway`, the two gateways with real inbound/outbound logic today) with payload including `threadId`/`userId`/`conversationId` — so "who sent this, on which thread" is always available to an observability consumer, independent of whether `GatewaySession` is in the picture.
- `OutputGuardMiddleware` — redacts secrets/PII (email, SSN, credit card w/ Luhn check, API keys, JWTs, etc.) and strips data-exfiltration markdown from model responses. Fully offline.
- `LLMGuardMiddleware` — LLM-as-judge classification of requests/responses for prompt-injection or harmful content, using a second (cheaper/local) model.
- Untrusted-content fencing (`aiFence()`, `AiMessage.addUntrusted()`) — RAG/tool/web content is wrapped in tamper-resistant boundary markers; `${context}` is auto-fenced by default.
- Security & Guardrails Phase 1: `PromptSecurity` heuristic injection scanning, `InputSanitizerMiddleware`, global `settings.security` auto-attach, and a new `mock` AI provider for deterministic offline testing.
- `beforeLLMCall`/`afterLLMCall` now fire on the streaming path for every provider, not just OpenAI-family.
- `SummaryMemory` supports a token-based trigger (`maxTokens`) as an alternative to `maxMessages`.
- `summarize()` is now available on every conversation memory type, not just `SummaryMemory`.
- New interception points: `onAIMemorySummarize`, `onAiDecisionStoreCreate`, `onGatewayCreate`, `onGatewaySessionCreate`, `onGatewayRegistryRegister`, `onGatewayRegistryUnregister`, `onGatewayConnect`, `onGatewayDisconnect`, `onGatewayMessageReceived`, `onGatewayMessageSent`, `onAIAgentRunCancel`, `onAIAgentRunSteer`.

### 🧠 Updated

- Default AI request timeout (`settings.timeout`) bumped from 45 to 90 seconds. A timed-out HTTP call surfaces as a confusing `JsonDeserializationError` ("Failed to parse JSON... Request Timeout") rather than a clear timeout error, and 45s was too tight for slower providers/models under load; raising the default reduces intermittent failures for CLI/`.bxs` usage. Still overridable per-request via `options.timeout` or per-provider via `settings.providers.<name>.options.timeout`.
- **Groq's default chat model** bumped from the now-deprecated `llama-3.1-8b-instant` (removed by Groq on 2026-08-16) to its recommended replacement, `openai/gpt-oss-20b`.
- OpenAI provider's default chat model bumped from `gpt-5-nano` to `gpt-5.6-luna`.
- Claude provider's default chat model bumped from `claude-sonnet-4-5` to `claude-sonnet-5`.
- `SummaryMemory`: `maxMessages` now triggers compression and `summaryThreshold` is the keep-window (previously threshold did both and `maxMessages` was unused).
- `AiMessage` escapes `${...}` inside binding values by default to prevent template-confusion injection.
- Inbound user content is now NFKC-normalized and stripped of zero-width characters by default, even without `settings.security` enabled.

### 🪲 Fixed

- **`IAiMemory.summarize()` ignored `userId`/`conversationId` scoping**, unlike every other memory method (`add`, `getAll`, `trim`, etc.). On a shared/stateless memory instance serving multiple users or conversations, calling `summarize()` always compressed whichever scope happened to resolve to the instance default — effectively collapsing all users'/conversations' history together instead of the one actually intended. `summarize( config, userId, conversationId )` now accepts the same optional `userId`/`conversationId` overrides as the rest of the interface, correctly scoping the read, the AI compression, and the persisted result (cache/file/database/session) to just that user, that conversation, or — with neither passed — the instance default, same as before. `SummaryMemory`'s auto-trigger (`trim()`) now forwards its own `userId`/`conversationId` into `summarize()` instead of dropping them. Also fixes a related gap where `SessionMemory` had no `summarize()` override at all, so any triggered summary silently never made it back into session storage. Each `summarize()` call is now serialized per `(key, userId, conversationId)` scope via a named lock, so concurrent calls for different scopes on one shared instance no longer risk clobbering each other's in-flight compression.
- **Enabling Claude extended thinking broke `aiChat()` outright.** The synchronous path read the answer as `result.content.first().text`, but with `params: { thinking: {...} }` Anthropic returns one or more `thinking` blocks _before_ the text block — so that read was null and every sync return format (`single`, `json`, `xml`, `structuredOutput`) silently produced an empty answer. Now selects the first **text** block. Bedrock had a matching hole on the streaming side: its `transformStreamChunk()` dropped any chunk with no content or finish reason, which is exactly what every chunk looks like while a model is still reasoning.
- `approve_always`/`approve_session` grants never actually persisted for async (non-CLI) gateways — missing identity on `GatewayContext` and an incomplete resume-path decision handler. Fixed; new integration test covers suspend → resume with a grant → auto-approve on a later run.
- HITL suspend/cancel/reject were silently swallowed by a closure-scoping bug in OpenAI's tool-call loop — batches now stop/skip correctly and checkpoint as expected.
- `RunControlMiddleware.afterAgentRun()` released a run's cancellation token by an unconditional registry remove rather than a compare-and-swap — two `run()`/`stream()` calls sharing a `threadId` while both were in flight (now a real scenario under `GatewaySession`'s `queue`/`interrupt` policies) could have the first call to finish evict the _other_ call's still-active token, silently breaking `cancelRun`/`steerRun` for the run still going.
- `beforeToolCall`/`afterToolCall`/`wrapToolCall` never fired for Claude, Bedrock, or Cohere (tools were invoked directly) — all three now go through the same middleware pipeline as OpenAI.
- Claude/Bedrock/Cohere tool-call context lacked normalized `toolName`/`toolArgs`, so argument-based guardrails and HITL's edit-resume path silently no-op'd for those providers.
- `MockService` streamed a non-standard chunk shape, and `AiAgent.stream()`'s middleware-stop sentinel check used unsafe dot-access; both now match production streaming behavior.
- `returnFormat: "json"`/`asJson()` returned `{}` when the reply had a ```` ```json ```` marker inside a string value or in prose, or braces before the real payload. Fence extraction is now parse-validated and the brace/bracket scan retries at each candidate; an unusable reply logs a warning instead of failing silently. ([#222](https://github.com/ortus-boxlang/bx-ai/issues/222))
- Complex struct `returnFormat` generated getter/setter names instead of property names — BoxLang class instances satisfy `isStruct()`, so `SchemaBuilder` now checks `isObject()` first throughout, including for arrays of class/struct instances. ([#182](https://github.com/ortus-boxlang/bx-ai/issues/182))
- MCP tools crashed the Claude and Bedrock providers — `BaseTool` now provides a default `getArgumentsSchema()`. ([#231](https://github.com/ortus-boxlang/bx-ai/issues/231))
- Renamed several bare `request`/`server`/`url` locals that could shadow BoxLang's reserved scopes (hygiene fix; no confirmed live bug found in this codebase).

#### AWS Bedrock

- **AWS profile-file credentials never worked**: `AwsCredentialProvider.parseCredentialsFile()` called `chr()`, which is not a BoxLang function, so every attempt to read `~/.aws/credentials` threw `Function [chr] not found` and step 3 of the documented credential chain was dead. The function had no test coverage, which is how it survived. Now uses `char()`, with tests. ([#259](https://github.com/ortus-boxlang/bx-ai/issues/259))
- **EKS Pod Identity could not authenticate through `AwsCredentialProvider`**, so `OpenSearchVectorMemory` — its only consumer — failed against any OpenSearch domain using pod-level IAM. The container credential request sent **no `Authorization` header at all**, which both EKS Pod Identity and ECS-with-`AWS_CONTAINER_CREDENTIALS_FULL_URI` require. The token is now resolved per the AWS container credential provider spec — `AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE` first (re-read on every call, since Pod Identity rotates it) then the inline `AWS_CONTAINER_AUTHORIZATION_TOKEN` — and sent with the request. ([#259](https://github.com/ortus-boxlang/bx-ai/issues/259), [#227](https://github.com/ortus-boxlang/bx-ai/issues/227))
- **`AWS_CONTAINER_CREDENTIALS_FULL_URI` was fetched verbatim while carrying a bearer token.** Plain HTTP is now restricted to loopback and the documented ECS (`169.254.170.2`) / EKS Pod Identity (`169.254.170.23`) link-local addresses; any other host must use HTTPS, and an untrusted endpoint is refused with a warning rather than being handed the token — matching the AWS SDKs' own restriction. ([#259](https://github.com/ortus-boxlang/bx-ai/issues/259))
- **Bedrock's auto-resolved credential cache never actually cached anything.** `aiService()` builds a fresh provider on every call, but `resolvedCredentials`, the negative-cache timestamp and the lock name (a `createUUID()`) were all per-instance — so the cache was written once and never read, every request re-hit the ECS/EKS container or EC2 IMDS endpoint, and the 60-second negative cache could never fire, meaning a host with no metadata service paid the full container+IMDS timeout on _every_ call rather than once a minute. Auto-resolved credentials now live in a cache shared across instances, keyed by region plus credential source, with the named lock derived from that key so concurrent refreshes for one source collapse into a single metadata round-trip while different sources don't serialize against each other. Explicit and environment credentials still bypass the whole mechanism. ([#258](https://github.com/ortus-boxlang/bx-ai/issues/258))
- **Explicit AWS credentials could be signed with an unrelated session token.** `init()` seeds the instance from the environment, so a later `configure({ awsAccessKeyId, awsSecretAccessKey })` carrying no token of its own replaced only the key pair and kept `AWS_SESSION_TOKEN` — a token belonging to different credentials. SigV4 signed the mismatched triple and AWS rejected the request. Credentials now resolve as an atomic set: an explicit access key defines the whole set, so the secret and session token come from that same source or not at all. ([#227](https://github.com/ortus-boxlang/bx-ai/issues/227))
- Bedrock model-family detection was inconsistent across request/response/stream transforms (AI21, Cohere, legacy Mistral, and bare inference-profile ARNs could get mismatched parsing). ([#226](https://github.com/ortus-boxlang/bx-ai/issues/226))
- Bedrock's Claude request transform could throw on tool objects holding non-serializable data; now shallow-copies params instead of deep-duplicating. ([#221](https://github.com/ortus-boxlang/bx-ai/issues/221))
- Bedrock provider parity: chat events were announced under legacy `onAIRequest`/`onAIResponse` names nothing listened for, 429s weren't recognized as rate limits (no `onAIRateLimitHit`), `configure()`'s struct path skipped the settings merge, multi-block Claude responses dropped all but the first `text` block, header-only token usage reported zero, streaming logging ignored `logRequest`/`logResponse`, inference-profile ARNs routed to a non-existent path, array-shaped content corrupted Titan/Llama/Mistral requests, `onAIError` was announced twice, `checkGuardrailIntervention()` iterated an unguarded struct, and a `baseURL` path prefix was silently dropped. ([#227](https://github.com/ortus-boxlang/bx-ai/issues/227))
- Tools and schema-typed `returnFormat` were silently dropped on non-Claude Bedrock model families; they now throw `UnsupportedProviderCapability` naming the detected family. ([#227](https://github.com/ortus-boxlang/bx-ai/issues/227))
- Merged module-settings params never reached Bedrock's **embeddings** payload either: `chat()`/`chatStream()` merge the configured service params, but `embeddings()` did not, so a module-configured `input_type`, `dimensions` or `normalize` was silently dropped from the InvokeModel body. ([#227](https://github.com/ortus-boxlang/bx-ai/issues/227))
- Merged module-settings params never reached Bedrock's request body: `configure()` merges `settings.defaultParams` and `settings.providers.Bedrock.params` into `variables.params`, but `variables.params` was only ever read for `.model`, so a module-configured `temperature`/`top_p`/`max_tokens` was silently dropped. `chat()`/`chatStream()` now call `mergeServiceParams( variables.params )` like every other provider (request-level params still win). ([#227](https://github.com/ortus-boxlang/bx-ai/issues/227))
- **`OutputGuardMiddleware` silently did nothing on Bedrock's non-Claude families**, `action: "block"` included. `afterLLMCall` hands middleware the raw provider body (as Claude, Cohere and Gemini all do), but `PromptSecurity::getResponseText`/`setResponseText` only knew the OpenAI, Claude, Gemini and Cohere-chat shapes — so Bedrock's Titan (`results[].outputText`), Llama (`generation`), Mistral (`outputs[].text`) and legacy-Cohere (`generations[].text`) bodies resolved to an empty string and every guard no-op'd. The resolver now understands those four shapes and writes back in place, so a redaction also scrubs the content reused as the assistant turn on the tool-call path. It also now reads and rewrites **every** Claude `content[]` text block rather than only the first: `transformResponseFromClaude()` joins all of them into the returned response, so a safe opening block followed by one carrying a secret was previously returned without the guard ever seeing it, and a redaction rewrote block one while leaving the secret in block two to be joined back in. Redaction collapses the text blocks into one, leaving interleaved `tool_use` blocks untouched.
- **Claude's reasoning also bypassed the guard**: `ClaudeService` attached the derived `result.reasoning` _after_ `afterLLMCall` had already run, and its streaming middleware context omitted the accumulated reasoning entirely — so both paths returned reasoning that no guard had scanned. The attach now happens before the hook fires, and the streaming context carries `reasoning`. Note that **streaming guards are detection-only, not prevention**, for reasoning and content alike and on every provider: `afterLLMCall` fires once the stream has ended, so `block` throws after the caller's callback already received the chunks and `redact` rewrites an aggregate the provider has finished emitting. Documented on `OutputGuardMiddleware`; withholding mid-stream needs a per-chunk hook, tracked separately.
- **Bedrock's `onAIChatResponse` listeners were the only consumers that never saw reasoning.** `BaseService.sendChatRequest()` normalizes reasoning ahead of its own announce so listeners and the caller share one shape, but Bedrock announced the untouched native body. The derived `reasoning` key is now attached before the announce.
- **`OutputGuardMiddleware` never inspected the model's reasoning**, so a secret Claude named while thinking and never repeated in its answer was returned to the caller unscrubbed — and because the guard bailed out on empty content, a thinking-only turn was not scanned at all. `action: "block"` never fired for it either. Reasoning is now resolved and scrubbed independently of the answer via `PromptSecurity::getResponseReasoning`/`setResponseReasoning`, covering the provider-attached derived key, the OpenAI envelope in either native spelling (`reasoning`/`reasoning_content`), and accumulated streaming reasoning. **The native `thinking` blocks are deliberately never touched**: Bedrock requires the thinking blocks of the latest assistant message to be passed back complete and unmodified within a tool-use turn, and rejects a modified block outright (`thinking or redacted_thinking blocks in the latest assistant message cannot be modified`), so the scrub lands on a derived copy that never reaches the wire while the blocks go back byte-identical. Bedrock's streaming path now also accumulates reasoning so a guard can inspect it there, and reasoning is kept strictly out of `content` throughout. ([AWS extended-thinking docs](https://docs.aws.amazon.com/bedrock/latest/userguide/claude-messages-extended-thinking.html))
- Cohere-on-Bedrock generation failures were normalized as successful empty completions: `finish_reason: "error"` (which AWS documents as "the generation could not be completed due to an error", returned with `text: ""`) produced a normal-looking response with `finish_reason: "stop"`, so callers had no way to retry or report it. It now raises `ProviderError`. `error_limit` maps to `length` (a context-limit outcome), `error_toxic` to `content_filter`, and `user_cancel` to `stop`.
- Cohere-on-Bedrock responses were normalized by the Claude transform, which looks for a `content[]` array. Command R returns `{ text }` and legacy Command returns `{ generations: [ { text } ] }`, so every reply became an empty string, and the header-backfilled token counts were reported as zero. Added `transformResponseFromCohere()` covering both shapes, reading `response.usage` back like the Mistral transform, mapping Cohere's `COMPLETE`/`MAX_TOKENS`/`ERROR_TOXIC` onto OpenAI finish reasons, and logging rather than silently returning empty when a body matches neither shape. **Note:** this is the response half only — `transformRequestForModel()` still deliberately sends Cohere a Claude-shaped request, so Cohere-on-Bedrock is not yet functional end to end; a dedicated request transform is tracked separately.
- **Four Bedrock response-header reads were not normalized for array-shaped multi-value headers**, unlike every other header read in the provider. `checkGuardrailIntervention()` read `x-amzn-bedrock-guardrailaction` raw, so an array-shaped header turned the `findNoCase()` below it into an `arrayFindNoCase()` against header values and silently missed the intervention it exists to report. `retryAfter` in the `onAIRateLimitHit` payload had the same gap in all three 429 handlers (chat, stream, embeddings), so a rate-limit-aware retry middleware doing `val( retryAfter )` would get `0` instead of the real backoff. All four now go through `firstHeaderValue()`. ([#227](https://github.com/ortus-boxlang/bx-ai/issues/227))
- **Bedrock reported reasoning only when streaming.** Upstream normalized reasoning onto `choices[].message.reasoning` (sync) / `choices[].delta.reasoning` (stream) and patched Bedrock's _stream_ path, but Bedrock overrides `chat()` wholesale, so it never passes through `BaseService.sendChatRequest()` where `normalizeReasoningMessage()` runs — and its Claude response transform filtered content to `type == "text"`, dropping `thinking` blocks entirely. Claude-on-Bedrock extended thinking is now surfaced as `message.reasoning` (joined across multiple thinking blocks, never folded into `content`), and OpenAI-shaped Bedrock models answering with a native `reasoning_content` — DeepSeek-on-Bedrock — are normalized too, since `transformResponseFromOpenAI()` returns already-OpenAI-shaped bodies verbatim. The key is **omitted** when the model didn't think, matching the contract's "absence is normal". ([#227](https://github.com/ortus-boxlang/bx-ai/issues/227))
- **`beforeLLMCall` middleware could not mutate the request body on Bedrock's streaming path**: `sendBedrockStreamRequest()` serialized `dataPacket` to JSON _before_ firing the hook, so anything the hook changed was signed and sent as the pre-hook body. The sync path already ordered these correctly. Serialization now happens after the hook, as it does in every other provider. ([#227](https://github.com/ortus-boxlang/bx-ai/issues/227))

### 🔐 Security Fixes

- Gemini's API key was leaking into logs via the request URL (`?key=...`). Key is now request-local, and `PromptSecurity::redactURLSecrets()` masks key/token/secret query params in any logged endpoint as defense in depth.

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

[unreleased]: https://github.com/ortus-boxlang/bx-ai/compare/v3.4.0...HEAD
[3.4.0]: https://github.com/ortus-boxlang/bx-ai/compare/v3.3.2...v3.4.0
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
