# Local MCP compatibility probes

Run from the bx-ai checkout. These fixtures need no provider, keys, model, or production application. Keep both servers bound to loopback and stop them after testing.

## JVM: real HTTP dispatch

Build bx-ai (`./gradlew shadowJar`). With a JVM BoxLang MiniServer installed, use a fresh runtime home containing only this checkout's module. For example:

```sh
probe_home=$(mktemp -d "$PWD/build/mcp-home.XXXXXX")
mkdir -p "$probe_home/modules"
ln -s "$PWD/build/module" "$probe_home/modules/bxai"
boxlang-miniserver --host 127.0.0.1 --port 18964 --serverHome "$probe_home" --webroot "$PWD/src/test/bx/mcp/http"
```

Against a fresh server, run `python3 src/test/bx/mcp/probes/http_smoke.py --tools-only`. The site registers a recorder at application startup. The probe checks initialization, discovers its schema, then invokes it twice through the production HTTP processor on separate connections. The expected texts are `first` and `first,second`. Restart the test application before repeating: history intentionally persists. Omit `--tools-only` to require notification compliance too. That assertion **currently fails** on both engines (HTTP 200 and JSON-RPC `-32601` instead of 202 with no body); tool success alone does not establish Pi interoperability.

## MatchBox: packaged native HTTP dispatch

Use a standalone `matchbox_server` built locally without JNI (`cargo build --offline --locked -p matchbox_server --bin matchbox_server` in the MatchBox checkout). The root CLI `server` feature is a different build. With a fresh combined ZIP from `native/package.sh`:

```sh
probe=$(mktemp -d)
mkdir -p "$probe/site" "$probe/module-home/modules/bxai"
cp src/test/bx/mcp/http/{Application.bx,mcp.bxm} "$probe/site/"
unzip -q "$zip" -d "$probe/module-home/modules/bxai"
MATCHBOX_HOME="$probe/module-home" "$matchbox_server" \
  --host 127.0.0.1 --port 18964 --webroot "$probe/site"
```

Run the same Python probe as above. The no-JNI standalone server and JVM MiniServer passed against the same combined package. The native test used one application VM, not multiple workers. No external provider or image service participates.

## MatchBox: routed MCP dispatch

Copy `routed-mcp.bxs` to an isolated application directory. Extract the combined bx-ai ZIP to `$MATCHBOX_HOME/modules/bxai` in a fresh runtime home. Run `matchbox_server --app /path/to/routed-mcp.bxs` and `python3 src/test/bx/mcp/probes/http_smoke.py --tools-only` (default loopback port 18964). Use a fresh process each time. `processRoutedHttp(event, name)` uses the real HTTP processor on the route callback's VM; rendering a view would use a different VM. For current MatchBox builds, the module home variable is `MATCHBOX_HOME`.

## MatchBox: routed VM ownership only

Launch `matchbox_server --app "$PWD/src/test/bx/mcp/probes/routed-state.bxs"` in a fresh process without bx-ai. In another terminal:

```sh
curl --noproxy '*' -fsS -X POST http://127.0.0.1:18965/record/first
curl --noproxy '*' -fsS -X POST http://127.0.0.1:18965/record/second
```

Expected histories: `first`, then `first,second`. This proves a handler in **one routed application VM** retains a callback and its captured data across separate requests. It is not an MCP endpoint or a cross-worker registry.

## Class initialization, without the module

From this directory run `boxlang static-state.bxs` and `matchbox static-state.bxs` in fresh processes. The first call must see the class initializer's data without creating an instance; creating an instance must not reset that data. The JVM and the uncommitted MatchBox `feature/bx-ai-changes` branch pass. The returned scope is an alias: field assignment, indexed assignment, `structUpdate` and `structDelete` must all refuse to replace or remove `final LABELS`, while appending to the array remains valid. On the unmodified `db74c41` pin, MatchBox rejects the static block. No Java services, HTTP server, or bx-ai module are needed for this comparison.

The native positive control is `tests/scripts/vm_class_static_initializer.bxs` in the MatchBox checkout. It covers declaration order, first use through a static method or a constructor, class isolation, and failed initialization without a second attempt.

For the actual module boundary, run `matchbox --module "$PWD/build/module" src/test/bx/mcp/namedServerContract.bxs` **from the checkout root**. The current native branch gets past the final declarations, typed callback parameters, and computed constructor in `bifs/aiDecisionStore.bx`. MatchBox's mapped-module regression verifies constructing a registered BoxLang class by interpolated name with `argumentCollection`, including in a source-free executable. It does not establish on-demand discovery of loose `.bx` files by runtime name.

Run `boxlang dotted-param.bxs` and `matchbox dotted-param.bxs` from this probe directory: both now preserve an existing value and fill the missing field on the supplied struct. The native module also gets past `aiTransform.bx`'s semicolonless switch blocks.

MatchBox now accepts `default public` interface methods; its inheritance regression reads different values from two implementing instances. It also accepts `required` as a local identifier. Discovered module invocation paths now participate in compile-time mappings. The checkout-root command resolves `bxModules/bxai/models/hitl/IApprovalPolicy` from the shipped module; no project-level mapping is needed for this checkout.

To compare with the explicit-mapping run without copying, pruning or editing the module, give the no-JNI CLI a project-level compiler mapping. From the bx-ai checkout, using the local probe build:

```bash
root=$PWD
mb="$root/build/mcp-probe/target/debug/matchbox"
probe="$root/build/mcp-probe/project-mapping"
mkdir -p "$probe"
printf '%s\n' '{"mappings":{"/bxModules/bxai":"../../module"}}' > "$probe/boxlang.json"
(cd "$probe" && "$mb" --bx-home "$root/build/mcp-probe/home" --module "$root/build/module" "$root/src/test/bx/mcp/namedServerContract.bxs")
```

With that mapping, abstract declarations no longer block `BaseDocumentLoader` or `BaseTool`. The full package now passes `IVectorMemory` interface inheritance, the trailing array comma in `WeaviateVectorMemory`, and quoted struct keys using `=`. The mapped run also passes the nested quotes in `BaseService.bx:1124` and the inherited interface implementation in `BedrockService.bx`. The mapped run now passes the functional member references in `GeminiService.bx` and `OpenAIService.bx` and the arrow callback returning a struct in `AiImageResponse.bx`. A separate minimal module containing the shipped tool classes passed `[tool].map(.getSchema)` with a required `label` and returned `first` then `first,second` from the tool. This is not a named-server or HTTP test. Wildcard imports now bind constructors and base classes; MatchBox's regression verifies both after deleting the module source. Both full-package runs (checkout root and explicit mapping) compile and enter `registerRecorder()` in the unchanged contract. The checkout-root run now initializes `mcpLogger` at `MCPServer.bx:158` using MatchBox's native host logging service. **Package selection now matters:** plain `build/module` has the Java service provider but no native artifact. For native bx-ai use the complete ZIP built by [`native/package.sh`](../../../../../native/README.md). Its real `aiService` passes `src/test/bx/mcp/serviceRegistryContract.bxs` on no-JNI MatchBox and the same ZIP passes on the JVM. The same combined package passes `atomicStatsContract.bxs` on both engines: reset clears totals and timestamps without replacing the in-flight counter. The unchanged `namedServerContract.bxs` passes on native MatchBox, including registering the tool and dispatching two direct JSON-RPC calls (`first`, then `first,second`). This bypasses the production HTTP processor. The original standalone webroot failure (`mcpServer()` missing during application startup) is resolved. See the packaged HTTP probe above.

To check tool behavior independently, run `(cd build/module && "$mb" --bx-home "$root/build/mcp-probe/home" "$root/src/test/bx/mcp/probes/closure-tool.bxs")` from the checkout root. This uses the shipped `ClosureTool`, `BaseTool`, and `ITool` as loose classes. It verifies the actual generated schema and gets `first` then `first,second` by invoking the tool twice. It is **not** a packaged-class or HTTP test. In a separate minimal `--module` probe with those same three files in `models/tools`, the mapped `ClosureTool` now returns its inherited `getName()` and `invoke({label:"hello"})` returns `hello`. MatchBox's `mapped_class_inherits_from_its_own_package_after_source_removal` also checks same-named sibling bases in separate packages, including execution after removing the source.

**Separate module-owned boundary: named registry.** `MCPServer.getOrBuild()` needs `getBoxRuntime().getGlobalService("aiService")`. The provider named in the shipped JAR's `META-INF/services/ortus.boxlang.runtime.services.IService` is `ortus.boxlang.ai.services.BoxAiService`; its server map holds JVM `IClassRunnable` objects. In the original isolated probe (before native service registration) with the same JAR in `libs/`, `boxRuntime.getModuleService().getModuleRecord(...)` succeeded, but `boxRuntime.getGlobalService("aiService")` was unsupported. Direct `createObject("java", "ortus.boxlang.ai.services.BoxAiService")` independently reported `Java class not provided ... (JNI is not enabled)`. The combined package now supplies the native implementation; that earlier isolated result was not evidence of cross-worker identity. Service handles remain VM-owned.
