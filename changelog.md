# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### 🥊 New Features

- **Web Search Tools & BIF**: New `webSearch()` BIF and `WebSearchTools` class providing multi-provider web search for AI agents.
  - **`webSearch(query, params, options)`** BIF — simple entry point for web search.
  - **`webSearch@bxai` tool** — auto-registered AI tool enabling agents to search the web during conversations.
  - **`webSearchAsync(query, options)`** BIF — non-blocking variant returning a `BoxFuture` resolved on the `io-tasks` executor; all providers also expose `searchAsync()` directly.
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

[unreleased]: https://github.com/ortus-boxlang/bx-ai/compare/v3.1.0...HEAD
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
