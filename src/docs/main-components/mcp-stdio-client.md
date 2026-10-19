---
description: Use an MCP server that runs as a child process, over stdin and stdout
icon: terminal
---

# 🖥️ MCP Stdio Client

An MCP server does not have to listen on a URL. Many servers, such as `bxplaywright mcp` or `npx -y @modelcontextprotocol/server-github`, are programs that you launch and talk to over their **standard input and output**. This is the *stdio transport* of the [MCP specification](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#stdio).

BoxLang AI launches the program for you, does the MCP handshake, keeps it alive, restarts it if it dies, and shuts it down. You use it exactly like an HTTP MCP server: the same methods, the same `MCPResponse`, and the same agent integration.

## 🚀 Quick Start

```javascript
// Launch the server, list its tools and call one
client = MCP( { command: [ "bxplaywright", "mcp" ] } )

tools = client.listTools()
response = client.send( "browser_visit", { url: "https://boxlang.io" } )

if ( response.isSuccess() ) {
    println( response.getData() )
} else {
    println( "Failed: " & response.getError() )
}

// Stop the child process when you are done
client.close()
```

Give the tools to an agent and let it use them:

```javascript
agent = aiAgent(
    name: "browser-agent",
    model: aiModel( provider: "openai" ),
    mcpServers: [
        { command: [ "bxplaywright", "mcp" ], prefix: "browser", toolFilter: [ "browser_visit" ] }
    ]
)

agent.run( "Open boxlang.io and tell me the page title" )

// Releases the child process
agent.closeMCPServers()
```

## 🧱 The Client Classes

All MCP clients implement `IMCPClient` and share the code of `BaseMCPClient`. You never create them with `new`, the `MCP()` BIF picks the class from its argument.

| Class | Created by | Transport |
| ----- | ---------- | --------- |
| `MCPHttpClient` | `MCP( "https://host/mcp" )` | HTTP, with SSE answers |
| `MCPStdioClient` | `MCP( { command: [ ... ] } )` | Child process, stdin and stdout |

{% hint style="warning" %}
`MCPClient` was renamed to `MCPHttpClient`. Code that uses the `MCP()` BIF is not affected. Code that did `new bxModules.bxai.models.mcp.MCPClient( url )` must use `MCP( url )` instead.
{% endhint %}

## ⚙️ Configuration

`MCP()` takes a struct for a stdio server.

| Key | Type | Default | Description |
| --- | ---- | ------- | ----------- |
| `command` | array | required | The executable followed by its arguments, one element each. Never a single string, never run through a shell. |
| `env` | struct | `{}` | Environment variables added to the child process. Names keep their case. |
| `cwd` | string | current directory | Working directory of the child process. |
| `inheritEnv` | boolean | `true` | `true` starts the child with the environment of the current process plus `env`. `false` starts it with only `env`. |
| `maxRestarts` | numeric | `2` | How many times in a row a dead server is started again before the client gives up. See [Restarts](#restarts). |
| `commandGuard` | function | none | `( command ) => boolean`, called with the command array before every launch. Return `false` or throw to refuse it. |
| `timeout` | numeric | `30000` | Milliseconds to wait for each answer, also used for the handshake. Same as `client.withTimeout( ms )`. |

```javascript
client = MCP( {
    command    : [ "npx", "-y", "@modelcontextprotocol/server-github" ],
    env        : { GITHUB_PERSONAL_ACCESS_TOKEN: githubToken },
    cwd        : "/srv/work",
    inheritEnv : true,
    maxRestarts: 2,
    timeout    : 60000
} )
```

{% hint style="info" %}
Values such as `${env.TOKEN}` in a BoxLang configuration file are resolved by BoxLang before they reach BoxLang AI. The module only ever sees the resolved value.
{% endhint %}

A bad configuration (no `command`, an empty array, a command that is a string) throws `InvalidArgument` when `MCP()` is called. So does an option that a stdio server does not have: the HTTP-only ones (`token`, `headers`, `urlGuard`, ...), `prefix` and `toolFilter` (give those to `withMCPServer()`), or a typo such as `commandGaurd`. A misspelled security option is never ignored.

## 📚 Methods

The stdio client has the same surface as the HTTP client. Every call returns an `MCPResponse`.

| Method | Description |
| ------ | ----------- |
| `listTools( cursor="" )` | Tools of the server. Pass the `nextCursor` of a previous answer to read the next page. |
| `send( tool, args={} )` | Call a tool. |
| `listResources()` / `readResource( uri )` | Resources. |
| `listPrompts()` / `getPrompt( name, args={} )` | Prompts. |
| `getCapabilities()` | What the server answered to `initialize`. It does not send `initialize` again. |
| `withTimeout( ms )` | Milliseconds to wait for each answer. |
| `onSuccess( fn )` / `onError( fn )` | Callbacks that receive the `MCPResponse` of every call. |
| `getStats()` / `getSummary()` / `resetStats()` | Call statistics. |
| `close()` | Shut the child process down. Safe to call more than once. |

Only on `MCPStdioClient`:

| Method | Description |
| ------ | ----------- |
| `isRunning()` | `true` while the child process is alive. |
| `getProtocolVersion()` | The MCP protocol version negotiated with the server, empty before the first call. |
| `getStderrTail()` | The last 50 lines the server wrote to stderr, oldest first. |
| `getBaseURL()` | A label for the server, `stdio:` and the executable name. Arguments are left out because they can hold secrets. |

### The MCPResponse

It is the same object as for HTTP.

| Situation | `isSuccess()` | `getStatusCode()` | `getError()` |
| --------- | ------------- | ----------------- | ------------ |
| The server answered with a result | `true` | `200` | empty |
| The server answered with a JSON-RPC error (unknown tool, bad params) | `false` | `200` | the message of the error |
| The call never got an answer (timeout, the process exited, the launch failed, the handshake failed) | `false` | `0` | what happened, with the exit code and the last stderr lines when the process died |

Nothing throws from a call. Use `onError` or check `isSuccess()`.

## 🤖 Agents and Models

`withMCPServer()` and `withMCPServers()` on `aiAgent()` and `aiModel()` accept a stdio struct anywhere they accept a URL. The agent connects, lists the tools, and wraps each one so the model can call it.

### Long form

```javascript
agent.withMCPServer(
    { command: [ "bxplaywright", "mcp" ], env: { HEADLESS: "true" } },
    { prefix: "browser", toolFilter: [ "browser_visit", "browser_click" ], timeout: 45000 }
)
```

The first struct builds the client, the second says how to register its tools.

| Registration option | Description |
| ------------------- | ----------- |
| `prefix` | Tools are exposed as `prefix__name`, so two servers cannot clash. The server still receives the original name. |
| `toolFilter` | Array of tool names. Only these are exposed. |
| `timeout` | Same as the `timeout` key of the client config. |

### Many servers at once

An entry can be a URL, an HTTP struct, a stdio struct, or a ready-made client. In a stdio struct, `prefix` and `toolFilter` sit next to `command`.

```javascript
agent.withMCPServers( [
    "https://docs.example.com/mcp",
    { url: "https://api.example.com/mcp", token: "secret" },
    { command: [ "npx", "-y", "@modelcontextprotocol/server-github" ], prefix: "gh" },
    MCP( { command: [ "bxplaywright", "mcp" ] } )
] )

// The same list as the mcpServers argument
agent = aiAgent( name: "ops", model: aiModel( provider: "openai" ), mcpServers: [ ... ] )
```

### Closing

```javascript
agent.closeMCPServers()
```

`closeMCPServers()` closes the clients the agent built itself, that is the ones it made from a URL or a struct. A client you passed in ready-made is left alone because you own it, close it yourself.

### Options that only exist for HTTP

`token`, `headers`, `user`, `password`, `urlGuard`, `redirects`, `maxResponseBytes`, `httpVersion` and `handshake` do not apply to a child process. Using one with a stdio server throws `InvalidArgument` instead of being ignored.

### When a server cannot start

If the executable is missing, exits at once, or fails the handshake, the agent logs the reason to the `ai` log and carries on without those tools. It does not throw, and it does not leave a half started process behind. A command that is **refused** by the allow list or the guard is different: that throws, because a security refusal must never go unnoticed.

`listMCPServers()` lists the connected servers, with `stdio:<executable>` as the `url` of a stdio one.

## 🔄 How It Works

```
 your code                MCPStdioClient                        child process
 ─────────                ──────────────                        ─────────────
 client.send(...) ──────► launch (first call only)  ─────────►  starts
                          initialize ───────────────── stdin ►
                                     ◄──────────────── stdout ─ result
                          notifications/initialized ── stdin ►
                          tools/call ───────────────── stdin ►
                                     ◄──────────────── stdout ─ result  (matched by JSON-RPC id)
                                     ◄──────────────── stderr ─ log lines (to the "ai" log)
 client.close()   ──────► close stdin, wait, terminate, kill
```

### Framing

Messages are single lines of JSON, one per line, UTF-8, as the specification asks. The client never writes anything else to the stdin of the server. It reads the stdout of the server on one background thread, and hands each answer to the caller that is waiting for that JSON-RPC id, so many threads can share one client.

Lines on stdout that are not a JSON-RPC message are skipped and logged as a warning. Servers should not write them, but a banner or a stray `println` does not break the client.

### Handshake

Before the first request the client sends `initialize` with the protocol version `2025-06-18`, checks the version in the answer, and sends `notifications/initialized`. The versions it speaks are `2025-06-18`, `2025-03-26` and `2024-11-05`. If the server answers with another version, or refuses to initialize, the call fails with the reason and the process is stopped.

### Launch

The process is started on the first call, never at construction, one process per client. A client you never use costs nothing.

The first call also waits for the launch and the handshake, and `timeout` covers that wait. A program that downloads itself on its first run, such as `npx -y some-package`, can need much more than the default 30 seconds: give the client a larger `timeout` (for example `120000`). A first start that does not get through the handshake is not counted as a restart, so the next call simply tries again.

### Timeouts

Every request waits at most `timeout` milliseconds. When the time is up the call returns a failed response (`The MCP server [stdio:x] did not answer within N ms`), the client sends `notifications/cancelled` for that request as the specification asks, and the client stays usable. A late answer to a request that already timed out is dropped.

### Restarts

If the process has died when you make the next call, the client starts it again and does the handshake again. If it keeps dying, it gives up:

* A call that is **in flight** when the process dies fails. It is never repeated automatically, because repeating a tool call could do its work twice.
* The next call starts the server again, and the count of restarts goes up. A successful answer sets the count back to zero.
* After `maxRestarts` restarts in a row without a successful answer, calls fail with `keeps exiting and was started again N times`, and the last stderr lines. Only a server that had got through the handshake before counts as restarted.
* `maxRestarts: 0` never starts a dead server again.

{% hint style="warning" %}
A restarted server starts empty. For a server that holds state, such as the page that a browser server has open, the state is gone after a restart.
{% endhint %}

### Shutdown

`close()` follows the specification: it closes the stdin of the process, waits 2 seconds for it to exit, then terminates it and the processes it started (a launcher such as `npx` starts the real server), waits 2 seconds more, then kills them. It never waits for a write that is stuck because the server does not read, ending the process ends that write too. After `close()` the client is closed for good and calls fail with `is closed`.

Close the clients you create. A client that is never closed keeps its child process and two threads until the module unloads or the JVM exits, so do not build a stdio client per web request without closing it. Live clients are also closed as a safety net:

* when the module unloads (`onUnload`), and
* when the JVM shuts down, by a shutdown hook.

### stderr

Servers write their logs to stderr. The client reads it on its own thread so a chatty server can never block, writes each line to the `ai` log at the `debug` level as `[stdio:name] line`, and keeps the last 50 lines for `getStderrTail()` and for the message of a failure. stderr is never mixed into the protocol.

## 🔐 Security

A stdio client **runs a program**. Treat the `command` like any other thing that executes code.

* The command is an array that is handed to the operating system as is. On Linux and macOS there is **no shell**, so there is no shell expansion, and a value that holds spaces or `;` is just an argument. On Windows a `.cmd` or `.bat` file is started through `cmd.exe`, see [Windows](#windows).
* If the command, the arguments or the `env` come from configuration that users can edit, validate them or limit them with the controls below.

### Allow list

The module setting `mcp.stdio.allowedCommands` lists the executables that may be launched. An empty list, the default, allows any command.

```json
{
    "modules": {
        "bxai": {
            "settings": {
                "mcp": {
                    "stdio": {
                        "allowedCommands": [ "bxplaywright", "npx" ]
                    }
                }
            }
        }
    }
}
```

How a command is matched:

* A command **without a directory** (`npx`, `npx.cmd`) is matched by its name, without a Windows extension and ignoring case: `npx`, `NPX` and `npx.cmd` are the same entry.
* A command **with a directory** (`/usr/bin/npx`, `./npx`) is another file than the one the `PATH` finds, so it is allowed only when the list has that exact path.
* If the setting cannot be read, the command is refused.

A command that is not allowed throws `MCPCommandRefused` when the client is built.

The list limits which program starts, not what the program does: an allowed `node` or `python` can still run any code passed as an argument. Use the command guard to check arguments.

### Command guard

For rules that a list cannot say, pass a function. It receives a copy of the command array when the client is built and before every launch.

```javascript
client = MCP( {
    command: [ "npx", "-y", "@modelcontextprotocol/server-github" ],
    commandGuard: ( command ) => command[ 1 ] == "npx" && command.len() <= 3
} )
```

Return `false` or throw to refuse. Refusing throws `MCPCommandRefused`. The guard sees only the command array, not `env`, `cwd` or `inheritEnv`: if those come from input you do not control, validate them yourself, because variables such as `LD_PRELOAD` or `NODE_OPTIONS` change what a program does.

### Environment

By default the child inherits **all** of your environment variables, including secrets that the server may not need. Set `inheritEnv: false` and pass only what the server needs in `env` when you run servers you do not fully trust.

### Windows

On Windows an executable without a directory is looked up in the `PATH` using the `PATHEXT` extensions, and a `.cmd` or `.bat` file such as `npx.cmd` is started through `cmd.exe /c` with the arguments passed separately. This lets `[ "npx", "-y", "server" ]` work on every platform. `cmd.exe` interprets characters such as `&`, `|`, `%` and `^` in the arguments of a `.cmd` or `.bat` file, so never pass untrusted text as such an argument. This resolution is not covered by the automated tests, which run on Linux.

## 📡 Events and Statistics

The existing client events fire for stdio clients too, with two differences in the data: `baseURL` is the `stdio:<executable>` label, and there is a new `transport` key that is `"stdio"` (or `"http"` for HTTP clients).

| Event | When |
| ----- | ---- |
| `onMCPClientRequest` | Before a request is written |
| `onMCPClientResponse` | After a successful answer |
| `onMCPClientError` | After a failed call |

The handshake is internal, it does not fire events and it does not count in the statistics.

```javascript
BoxRegisterInterceptor( ( event ) => {
    println( "#event.transport# #event.baseURL# #event.name# took #event.executionTime# ms" )
}, "onMCPClientResponse" )

client.getSummary()   // { totalCalls, successRate, avgResponseTime, totalToolCalls, totalErrors, ... }
```

## 🧯 Troubleshooting

| You see | Cause |
| ------- | ----- |
| `Could not start the MCP server [stdio:x]` | The executable is not found or cannot run. Check the `PATH` of the BoxLang process, or give the full path. |
| `exited (exit code N) before it answered` | The server crashed or quit. The message ends with its last stderr lines. |
| `did not answer within N ms` | The server is slow or stuck. Raise `timeout`, and read `getStderrTail()`. On the first call of a program that downloads itself (`npx -y ...`) the download counts too: use a `timeout` of `120000` or more. |
| `is not in the module setting mcp.stdio.allowedCommands` | The command is not allowed. Add its name, or its exact path when the command has a directory. |
| `Unknown option(s) for a stdio MCP server` | A misspelled option, or one that only exists for HTTP. The message lists the valid options. |
| `answered protocol version [x], this client speaks ...` | The server speaks a version of the MCP specification that this client does not. |
| `keeps exiting and was started again N times` | The restart limit was reached. Fix the server, then build a new client. |
| `is closed` | `close()` was called. Build a new client. |
| `wrote a line to stdout that is not JSON` in the `ai` log | The server writes something other than protocol messages to stdout. The line is skipped, but fix the server. |

To see everything, set the `ai` log to `debug`: it shows the stderr of the server.

## 🛠️ Writing a Stdio Server

BoxLang AI can also be the server side of a stdio connection, see the MCP server guide and `examples/mcp/09-mcp-stdio.bxs`. Two rules for any stdio server:

1. **Only protocol messages on stdout**, one JSON object per line. Send logs and banners to stderr (`systemOutput( text, true, true )` in BoxLang).
2. **Exit when stdin closes.** That is how a client asks a server to stop. `MCPRequestProcessor::processStdio()` does this, and your own server should too, otherwise the client has to terminate it after the 2 second grace period.

## ✅ Recipes

```javascript
// bx-playwright
MCP( { command: [ "bxplaywright", "mcp" ] } )

// The GitHub MCP server, with a token only this server sees.
// npx needs PATH and HOME (it starts node), and its first run downloads the package, hence the larger timeout
MCP( {
    command   : [ "npx", "-y", "@modelcontextprotocol/server-github" ],
    env       : {
        PATH                         : getSystemSetting( "PATH" ),
        HOME                         : getSystemSetting( "HOME" ),
        GITHUB_PERSONAL_ACCESS_TOKEN : getSystemSetting( "GITHUB_TOKEN" )
    },
    inheritEnv: false,
    timeout   : 120000
} )
```

{% hint style="info" %}
With `inheritEnv: false` the child has no `PATH`. Pass the full path of the executable, or add `PATH` to `env`. A program that starts other programs, such as `npx` (it starts `node`), fails with `'node': No such file or directory` without `PATH`, and often needs `HOME` too.
{% endhint %}
