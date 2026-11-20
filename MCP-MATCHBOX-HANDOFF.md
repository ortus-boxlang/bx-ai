# Handoff: dual-runtime MCP support in bx-ai

## Purpose and authorization

The operator wants to investigate bx-ai MCP support on native MatchBox while
**preserving BoxLang JVM behavior**. MCP is the first bounded area, not a request
to port the whole AI module.

The current phase is **source review and design discussion**. The operator asked
for this handoff so the conversation can continue in the bx-ai checkout. No
implementation approach has been approved. Discuss demonstrated capability gaps
and proposed boundaries before implementing changes to bx-ai or MatchBox.

Do not deploy, change credentials, invoke a model, consume image-generation quota,
push branches or create PRs under this review authorization.

## Non-negotiable direction

- Keep one bx-ai module supporting JVM and native runtimes, not a MatchBox fork.
- Keep portable BoxLang behavior shared. Do not rewrite shared source into native
  idioms that change or break JVM behavior.
- Preserve public APIs, concurrency semantics, instance identity, lifecycle,
  events, schemas and results. Any intentional API/protocol changes need discussion.
- Assess genuine missing BoxLang semantics as MatchBox issues; do not hide them
  with distorted module syntax or application workarounds.
- Use runtime-specific implementations only where justified. Do not assume every
  Java dependency needs a Rust replacement or that a Rust rewrite is appropriate.
- Existing JVM behavior needs meaningful regression evidence, plus shared contracts
  and native integration checks where applicable. Reuse existing test machinery.
- Do not replace Java atomics/concurrent maps with ordinary increments/structs
  without preserving the required synchronization and ownership semantics.
- Preserve unrelated working-tree changes; use feature branches for eventual PRs.
- Read `AGENTS.md` before continuing. Verify actual source rather than treating
  its illustrative signatures, file counts or line counts as authoritative.

## Immediate application use case

The private image service now runs MatchBox/BoxLang for both UI and backend,
with bx-image's native Rust provider for raster processing. Python has been
removed from its application, tests and build helpers. Node is development-only.

The intended next integration is **MCP server functionality**: Pi connects to the
image service and calls tools that share its existing BoxLang job manager and
SQLite records. Do not introduce another generation worker or persistence path.

Previously discussed tool concepts, not implemented or final API contracts:

| Tool | Intended behavior |
| --- | --- |
| `generate_image` | Admit one asynchronous job and return its ID |
| `get_generation` | Read status/result links without resubmission |
| `search_images` | Search existing titled image records |
| `get_image` | Return a saved record and HTTPS links |

Preserve one image per request, one active job, no automatic retries. Failed or
uncertain jobs may have consumed quota. Title generation belongs in the same
Codex invocation as the image, not another model request.

Generation uses Codex CLI's native image tool through the ChatGPT subscription,
not bx-ai's separately billed image-provider APIs. MCP tool serving must not
implicitly select a model/provider, read developer API keys or invoke generation.
Initially, text/JSON metadata and HTTPS links are sufficient; inline MCP image
content is not a prerequisite.

Earlier discussion deferred authentication for the image-service MCP concept.
That is not permission to remove existing bx-ai authentication features or to
publish an endpoint. The app remains private-network and unauthenticated; Host/
Origin checks are not authentication. Endpoint security still needs explicit design.

## Repository locations and recorded state

- bx-ai: `/home/jacob/dev/ortus-boxlang/bx-ai`
  - Reviewed HEAD: `795326f` — `fix(memory): summarizing a short jdbc conversation no longer empties it (#338)`.
  - Working tree was clean before this handoff file. No MCP source was changed.
- MatchBox: `/home/jacob/dev/ortus-boxlang/matchbox`
  - Local feature branch: `feature/image-service-process-groups`.
  - Reviewed application runtime pin: `db74c4155cf1172becd01ac3cebd87e5be22ac66`.
- bx-image: `/home/jacob/dev/ortus-boxlang/bx-image`
  - Local feature branch: `feature/native-png-publication`.
  - Reviewed application module pin: `af483d60723fa8cf319027962b37eb388ee6aae7`.
- Image service: `/home/jacob/dev/jbeers/image-service`
  - Branch: `feature/matchbox-backend`; implementation commit `6e78765`.
  - `native-lock.json` pins native source commits and builder digest.
  - `worker/app.bxs`, `worker/generate.bxs`, and
    `worker/boxlang_modules/image-service/Jobs.bx` own API/jobs/persistence.
- Infrastructure: `/home/jacob/dev/jbeers/network-services`
  - `services/image-service/README.md` is the deployment/evidence runbook.
  - `services/image-service/app.yaml` owns the deployed image pins/configuration.

Recheck Git status and actual HEADs before working. The local native feature
commits were not pushed and no upstream PRs were created during this conversation.

### Current live image service (context, not permission to operate)

URL: `https://web.image-service.home.arpa/`.

Both Gitea images were published and deployed after separate explicit approval:

- Web: `server.gitea.nas.home.arpa/jacob/image-service@sha256:e54998eff8388631cac05ff40b47571d544e7cea97a1f8f3a9a17ba18fdfd317`
- Worker: `server.gitea.nas.home.arpa/jacob/image-service-worker@sha256:76017863005cfaa55e5c1fa58910bc474328508c77e6ec84e5ebd5a3c66dfc8c`

The corrected pod reached 2/2 Ready with zero restarts. All retained job/record
API data, SQLite bytes and saved PNG hashes matched the pre-rollout baseline;
private ChatGPT login recognition, CA-verified HTTPS, Homepage and no-op reapply
passed. No generation occurred. Real automatic generation/title output and OAuth
refresh remain unverified.

The first replacement exposed a Kubernetes Service-link collision:
`IMAGE_SERVICE_PORT=tcp://...` was parsed as zero. A separately approved explicit
worker env `IMAGE_SERVICE_PORT="8081"` fixed startup. Preserve that override.

The release changed only two image pins and this port override. Deployment and
runbook edits in network-services, and the image-service README status update,
were left **uncommitted**. Do not discard, silently commit or redeploy them.

Private app state is under `/home/k3s-services/image-service/{data,codex}` on lab1.
Only the worker mounts it; none of it is needed for this bx-ai review. Do not read
credentials, kubeconfigs, auth files, private diagnostics or live databases into
agent context. No cluster access is needed for the compatibility assessment.

## Source-review findings

These findings come from source inspection. **No bx-ai tests or MatchBox loading/
protocol probes were run in this review.** Do not describe native compatibility
or Pi interoperability as established.

### 1. Separate client and server scope

- `src/main/bx/bifs/MCPServer.bx`: named server factory.
- `src/main/bx/bifs/MCP.bx`: outbound MCP client factory.
- `src/main/bx/models/mcp/MCPServer.bx`: registration, lifecycle helpers,
  JSON-RPC dispatch, capabilities, optional security and stats integration.
- Client classes include `BaseMCPClient`, `MCPHttpClient`, `MCPStdioClient`,
  OAuth flows and token stores. Leave client/agent/provider compatibility outside
  the initial server slice except where module bootstrap requires investigation.

### 2. Existing request/transport separation

Server-side flow:

```text
MCPServer(...) BIF
  -> MCPServer::getOrBuild(...)
  -> named instance via BoxAiService

HTTPTransport.readRequest()
  -> normalized request struct
  -> MCPRequestProcessor.processWithTransport(...)
  -> security/callback/event handling
  -> MCPServer.handleRequest(...)
  -> protocol response
  -> HTTPTransport.writeResponse()
```

Important files:

- `src/main/bx/models/mcp/MCPRequestProcessor.bx`
- `src/main/bx/models/mcp/transports/ITransport.bx`
- `src/main/bx/models/mcp/transports/BaseTransport.bx`
- `src/main/bx/models/mcp/transports/HTTPTransport.bx`
- `src/main/bx/models/mcp/transports/StdioTransport.bx`

The processor already separates some transport concerns from dispatch. Preserve
that useful structure rather than building a parallel MCP implementation.

**Direct `MCPServer.handleRequest()` calls bypass the processor's authentication,
body-limit and other transport-level checks.** Do not assume those protections
are active merely because the server has been configured.

### 3. Confirmed JVM integration points

| Area | Existing implementation | Investigation concern |
| --- | --- | --- |
| Server registry | `src/main/java/ortus/boxlang/ai/services/BoxAiService.java`, registered via Java service loading | Runtime-owned named BoxLang instances, concurrency, identity, visibility and shutdown |
| Logging | `getBoxRuntime().getLoggingService().getLogger(...)` in server/processor/transports | Direct JVM runtime calls, including static initialization |
| Stats | `MCPServerStats.bx`: Java `AtomicInteger`, `AtomicReference`, BoxLang locks | Thread safety, active-request counters, enable/disable/reset and snapshots |
| HTTP binding | CGI/URL scopes, `getHTTPRequestData`, `bx:header`, `bx:content` | MatchBox routed handlers use request/response objects; compatibility is unproven |
| Class discovery | `RunnableLoader`, `DynamicInteropService`, resolved file paths, BoxLang metadata | JVM-specific loading vs portable language reflection |
| IP/CIDR | Java `InetAddress` in server helpers | Optional native security behavior must not silently disappear |
| Rate limits | Stores/cache/locks, Java concurrent collections, atomics, clock and hostname lookup | Separate optional compatibility surface |

Specific observations:

- `MCPServer.getBoxAiService()` calls
  `getBoxRuntime().getGlobalService("aiService")`.
- `BoxAiService` stores servers in a `ConcurrentHashMap<Key,IClassRunnable>` and
  clears them on shutdown. This is not merely a BoxLang static struct.
- The BoxLang factory uses a separate `hasServer -> construct -> putServer`
  sequence. A concurrent map does not itself prove exactly-once construction.
  Verify intended concurrent factory semantics before making claims or changes.
- Disabling statistics still constructs `MCPServerStats` and Java atomics. It is
  not a ready-made way around that dependency. Active counters also need review.
- Do not assume a native registry can retain arbitrary VM values across workers
  or request VMs safely. Object ownership/lifetime and scope must be investigated.

### 4. Tool definitions are a strong shared-code candidate

Relevant files:

- `src/main/bx/bifs/aiTool.bx`
- `src/main/bx/models/tools/ITool.bx`
- `src/main/bx/models/tools/BaseTool.bx`
- `src/main/bx/models/tools/ClosureTool.bx`

`aiTool -> ClosureTool -> BaseTool -> callback` provides schema generation,
explicit schemas, argument descriptions, before/after execution events and result
serialization. Keep this application-facing behavior shared where possible.

Watch the actual dependencies:

- `ClosureTool` introspects `callable.$bx.meta.parameters`.
- It invokes the callable through `argumentCollection` and injects `_chatRequest`.
- `BaseTool` has an optional `AiChatRequest` argument, events and serialization.
- `aiTool` auto-registers in `AIToolRegistry` by default; `autoRegister=false`
  exists, but using it for a bounded probe is not a solution for the public
  default's full compatibility.
- MCP `handleToolCall()` currently returns one text content item containing the
  tool's string result. Complex callback results become JSON text. This already
  suits initial image-service job metadata/HTTPS links.

### 5. Module bootstrap is broader than MCP

`src/main/bx/ModuleConfig.bx`:

- Has injected JVM service references and imports `MCPStdioClient`/`ProfileResolver`.
- `onRuntimeStart()` validates profiles, can discover skills, and scans/registers
  core, audio, image and web tools.
- `onUnload()` calls `MCPStdioClient::closeAll()`.
- MCP server rate limits are disabled by default; trusted proxy list is empty.

Assess which hooks/imports the native loader actually invokes/resolves. Do not
assume that selecting MCP APIs automatically isolates module loading from the
rest of bx-ai. Do not remove unrelated JVM bootstrap behavior to get a probe
running. Packaged JAR service loading also needs explicit consideration.

### 6. Protocol compatibility is distinct from native compatibility

Confirmed current server behavior:

- Server and request processor advertise fixed protocol version `2024-11-05`.
- `handleInitialize()` returns the fixed version, rather than inspecting the
  requested version to select a supported one.
- HTTP GET returns JSON discovery information.
- A POST response can be wrapped in a completed SSE message based on Accept.
- `notifications/initialized` is not an explicit case in the server dispatcher.
- Missing-ID messages still produce response structs, with the ID omitted;
  the HTTP processor serializes those responses instead of suppressing a reply.
- CORS handling primarily adds/omits response headers. A non-allowed origin is
  not automatically rejected by that branch.

Therefore, loading/running on MatchBox does **not** establish modern Streamable
HTTP interoperability with Pi. One SSE-formatted response does not establish a
complete Streamable HTTP implementation. Test initialization, notifications,
request IDs, content negotiation, supported HTTP methods and applicable protocol
headers against an actual client before declaring support. Sessions are optional
in a stateless design; do not invent a session subsystem without a requirement.

These questions also affect JVM behavior. Discuss whether changes should be
backward-compatible enhancements or a distinct transport before altering the
existing JVM HTTP API. Do not silently change it as part of a native port.

### 7. Existing JVM tests should be reused

Tests live in `src/test/java/ortus/boxlang/ai/mcp/`, including:

- `mcpServerTest.java`: instances/registration, initialize/list/call, resources,
  prompts, stats, auth/CORS/body limits, annotations, pause/resume and IP filters.
- `mcpRateLimitTest.java`, `mcpRateLimitHistoryTest.java`: stores, exact limits,
  concurrency, forwarded-header policy, reporting and failure behavior.
- Client/protocol/stdin/OAuth tests and Java fake servers/harnesses also exist.
  Do not confuse outbound-client coverage with server transport coverage.

The JUnit harness extends `BaseIntegrationTest` and executes BoxLang snippets.
It loads `build/module`, not source directly. Build current sources before using
results as evidence, and follow repository instructions about Gradle test inputs.
Direct protocol-dispatch tests are not a replacement for real HTTP/client checks.
No existing test was run or claimed green in this review.

## Suggested next discussion

Start with the **server registry/lifecycle boundary**, then logging/statistics and
HTTP binding. These are required even for a small named-server example.

A proposed first working slice, not yet approved for implementation:

> One named MCP server, explicitly registered closure tools, initialization,
> discovery and invocation over HTTP, with no model/provider required.

Use a harmless echo/status tool, not image generation, for eventual probes.
Include initialized-notification handling in the observable client lifecycle.
Investigate against the actual pinned MatchBox runtime, not documentation alone.

Classify findings before selecting fixes:

1. Portable BoxLang: preserve shared code.
2. Valid BoxLang unsupported by MatchBox: discuss a runtime fix.
3. Incidental Java with a semantics-preserving portable equivalent: consider a
   shared change with JVM/native evidence.
4. Genuinely runtime-specific mechanisms: discuss a small consistent boundary
   with separate implementations and shared contracts.

Do not yet decide that this requires a new Rust MCP backend, a general Java
compatibility layer, a registry framework or a complete bx-ai port. bx-image is
a packaging precedent, not proof that the same internal design fits bx-ai.

## Documentation and skills

Read only the relevant guidance when moving from review to implementation:

- bx-ai `AGENTS.md`, current source and focused examples/tests.
- `/home/jacob/dev/jbeers/agent-skills/skills/matchbox-create-module/SKILL.md`
- `/home/jacob/dev/jbeers/agent-skills/skills/matchbox-load-module/SKILL.md`
- `/home/jacob/dev/jbeers/agent-skills/skills/general-implementation-guidance/SKILL.md`
- `/home/jacob/dev/jbeers/agent-skills/skills/behavior-driven-tdd/SKILL.md`
- MatchBox `AGENTS.md`, native-plugin docs and SDK guidance before runtime changes.

The operator's last direction was to review what exists and begin discussing a
bounded dual-runtime MCP approach. Continue that discussion before writing a port.
