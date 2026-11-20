# Native bx-ai service (MatchBox, no JNI)

`BoxAiService.java` is still the JVM service provider. For a combined package, The precompiled plugin declares `models/registry/NativeBoxAiService.bx` through `matchbox.global-services.v1`. MatchBox constructs the service before `onLoad()` and shuts it down on unload or failed activation. The shared `ModuleConfig.bx` does not detect either backend. Both consumers call `getBoxRuntime().getGlobalService("aiService")`; there is no per-request runtime switch. Use MatchBox's `feature/native-class-providers` branch until the required SDK and host capabilities are released. A Java class import is not a service lookup.

## Build and check locally

From the bx-ai checkout with JVM BoxLang, a JDK and a Rust toolchain available:

```bash
./gradlew shadowJar
boxlang native/package.bxs
```

`package.bxs` uses the sibling MatchBox SDK by default; set `MATCHBOX_SDK` to choose another local SDK checkout. It writes a target-specific ZIP in `build/distributions/` containing the complete JVM module, shared `.bx` files, native library, target, ABI and SHA-256. `build/module` remains the ordinary JVM staging directory. Linux x86-64 GNU is tested. No consumer Cargo build or startup download is needed.

Extract the combined ZIP into an empty `bxai/` directory. With a fresh runtime home, run `matchbox --module /path/to/bxai src/test/bx/mcp/serviceRegistryContract.bxs`. On JVM BoxLang install that same directory as `$BOXLANG_HOME/modules/bxai` and run `boxlang src/test/bx/mcp/serviceRegistryContract.bxs`. Run `src/test/bx/mcp/atomicStatsContract.bxs` the same way to check the stats factory and reset semantics. `src/test/bx/mcp/namedServerContract.bxs` now also passes on no-JNI MatchBox: it constructs the real server, registers a closure, and makes two direct JSON-RPC tool calls through separate named lookups. These scripts do not exercise HTTP.

## Ownership and compatibility

- Each declaring VM owns one map. A map operation runs synchronously on that VM; `getOrBuildServer` is one lookup-or-insert operation. The existing `MCPServer.getOrBuild()` multi-call factory is unchanged. No closure handle crosses a thread or VM.
- The plugin uses SDK retained values, not saved call handles, so lookups return the actual instance. Replacement, removal, clear, service shutdown, and payload destruction release the corresponding roots. Keys are case-insensitive; listing preserves the original spelling, not insertion order.
- `getAllServers()` returns the same live native map (not a serialized copy). Its native surface is `get`, `containsKey`, `size`, `isEmpty`, `put`, `putIfAbsent`, `remove(key)`, and `clear`, plus the facade helpers `names` and `getOrInsert`. It is **not** a JVM `ConcurrentMap` implementation with Java iterators, overloads or reflection. The JVM method and return type have not changed.
- MatchBox VM ownership provides isolation, not a process-global registry shared across independent workers. Cross-worker dispatch is not implemented. HTTP MCP dispatch is separate from direct named-server dispatch.

## MCP statistics

Both stats classes import the bx-ai-owned `ortus.boxlang.ai.util.AtomicValues`. `integer(initial)` returns a JVM `AtomicInteger` or a native signed 32-bit counter (`get`, `incrementAndGet`, `decrementAndGet`). The native operations use sequentially consistent atomics and wrap on overflow. `reference(initial)` returns a JVM `AtomicReference` or a GC-traced MatchBox cell with `get` and `set`. The native cell only holds values in its owning VM and never serializes or moves a timestamp to another worker. The Rust plugin declares the exact `java:ortus.boxlang.ai.util.AtomicValues` provider during initialization. No BoxLang wrapper or descriptor entry is needed for this class. The plugin requires MatchBox's ABI-1 `matchbox.class-providers.v1` and `matchbox.object-state.v1` capabilities; a host without them cannot load this version. Use the matching SDK when building with `MATCHBOX_SDK`. The JVM class is unchanged. These factories do not register any `java.util.*` names; the `aiService` startup and shutdown lifecycle is separate.
