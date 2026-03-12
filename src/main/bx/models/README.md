# bx-ai Middleware

## What is Middleware?

Middleware lets you intercept and control AI agent execution at specific points — before/after LLM calls, before/after tool calls, and around the full agent run. Each middleware can inspect the context, allow execution to continue, block it, or suspend it for human review.

---

## Lifecycle & Hook Points

Middleware executes in the order it was registered on the way **in**, and in **reverse order** on the way out:

```
Agent Run
│
├─► beforeAgentRun        [middleware 1 → 2 → 3]
│
│   LLM Call Loop
│   ├─► beforeLLMCall     [middleware 1 → 2 → 3]
│   ├─► wrapLLMCall       [middleware 1 → 2 → 3]  ← wraps the actual API call
│   └─► afterLLMCall      [middleware 3 → 2 → 1]  ← reverse
│
│   Tool Execution
│   ├─► beforeToolCall    [middleware 1 → 2 → 3]
│   ├─► wrapToolCall      [middleware 1 → 2 → 3]  ← wraps the actual tool call
│   └─► afterToolCall     [middleware 3 → 2 → 1]  ← reverse
│
└─► afterAgentRun         [middleware 3 → 2 → 1]  ← reverse
```

### Hook Point Context Structs

| Hook | Context Keys |
|------|-------------|
| `beforeAgentRun` | `agent`, `input` |
| `afterAgentRun` | `agent`, `input`, `response`, `suspensions` |
| `beforeLLMCall` | `model`, `chatRequest`, `messages` |
| `afterLLMCall` | `model`, `chatRequest`, `messages`, `response` |
| `wrapLLMCall` | `model`, `chatRequest`, `messages` + `handler` function |
| `beforeToolCall` | `toolName`, `toolArgs`, `toolCallId` |
| `afterToolCall` | `toolName`, `toolArgs`, `toolCallId`, `result` |
| `wrapToolCall` | `toolName`, `toolArgs`, `toolCallId` + `handler` function |
| `onSuspend` | `reason`, `threadId`, `data` |

---

## Return Values

Every hook must return an `AiMiddlewareResult`:

```js
// Allow execution to continue
return AiMiddlewareResult.continue();

// Block execution with a reason
return AiMiddlewareResult.reject( "Blocked by security policy" );

// Pause for human review (HITL)
return AiMiddlewareResult.suspend( data = {}, reason = "Human review required" );
```

---

## Registration Patterns

### 1. Class instance (most common)
```js
agent.withMiddleware( new LoggingMiddleware() );
```

### 2. With constructor arguments
```js
agent.withMiddleware( new RetryMiddleware( maxRetries=5, backoffFactor=2 ) );
```

### 3. Multiple middleware (chained)
```js
agent
    .withMiddleware( new LoggingMiddleware() )
    .withMiddleware( new RetryMiddleware() )
    .withMiddleware( new GuardrailMiddleware( blockedTools=["deleteFile"] ) );
```

### 4. Array registration
```js
agent.withMiddleware([
    new LoggingMiddleware(),
    new RetryMiddleware(),
    new MaxToolCallsMiddleware( maxCalls=20 )
]);
```

### 5. Model-level middleware
Middleware can also be attached directly to a model. Model middleware fires at the `beforeLLMCall`/`afterLLMCall` level, after any agent middleware:
```js
model.withMiddleware( new LoggingMiddleware( logTokens=true ) );
```

### 6. Struct of closures (lightweight, no class needed)
```js
agent.withMiddleware({
    beforeToolCall: function( context ) {
        writeLog( "Tool: #context.toolName#", "information", "myapp" );
        return AiMiddlewareResult.continue();
    }
});
```

---

## Execution Order: Sequential In, Reverse Out

Given three middleware registered in order `[A, B, C]`:

```
→  A.beforeToolCall
→  B.beforeToolCall
→  C.beforeToolCall
       [tool executes]
←  C.afterToolCall
←  B.afterToolCall
←  A.afterToolCall
```

This means the **last middleware registered wraps closest to the actual call** — like layers of an onion.

---

## Built-in Middleware

### 1. LoggingMiddleware

Logs all agent lifecycle events to a log file.

```js
new LoggingMiddleware(
    logName   = "ai",       // log file name
    logLevel  = "debug",    // debug | info | warn | error
    logArgs   = true,       // include tool arguments (security-sensitive)
    logTokens = false       // include token usage counts
)
```

**Example:**
```js
agent.withMiddleware(
    new LoggingMiddleware( logName="myapp", logLevel="info", logTokens=true )
);
```

Logs entries like:
```
[bx-ai][beforeAgentRun] Agent 'MyAgent' starting | Input: Tell me the weather...
[bx-ai][beforeLLMCall]  LLM call starting | Model: gpt-4o | Messages: 3 | Tools: 2
[bx-ai][beforeToolCall] Tool 'getWeather' starting | Args: {"city":"London"}
[bx-ai][afterToolCall]  Tool 'getWeather' completed | Result: {"temp":18,"unit":"C"}
```

---

### 2. RetryMiddleware

Automatically retries failed LLM calls and tool executions with exponential backoff.

```js
new RetryMiddleware(
    maxRetries    = 3,      // maximum retry attempts
    backoffFactor = 1.5,    // wait = backoffFactor ^ attemptNumber (seconds)
    retryOn       = [],     // [] = retry all; or ["NetworkException","TimeoutException"]
    retryLLM      = true,   // retry failed LLM calls
    retryTools    = true    // retry failed tool calls
)
```

**Example — retry only network errors, up to 5 times:**
```js
agent.withMiddleware(
    new RetryMiddleware(
        maxRetries    = 5,
        backoffFactor = 2,
        retryOn       = ["NetworkException", "TimeoutException"]
    )
);
```

**Backoff timing** with `backoffFactor=1.5`:
- Attempt 1 fail → wait 1.5s
- Attempt 2 fail → wait 2.25s
- Attempt 3 fail → wait 3.375s

---

### 3. GuardrailMiddleware

Pattern-based security that blocks tool calls matching forbidden names, arguments, or value patterns.

```js
new GuardrailMiddleware(
    blockedTools = [],   // tool names to block entirely
    blockedArgs  = {},   // { toolName: ["argName"] } — block if arg present
    argPatterns  = {}    // { argName: "regexPattern" } — block if value matches
)
```

**Example — block dangerous tools and SQL injection patterns:**
```js
agent.withMiddleware(
    new GuardrailMiddleware(
        blockedTools = ["deleteDatabase", "execShell"],
        blockedArgs  = { "runQuery": ["dropTable"] },
        argPatterns  = { "query": "(DROP|DELETE|TRUNCATE).+TABLE" }
    )
);
```

Three security checks run in order:
1. Is the tool name in `blockedTools`? → reject
2. Does the tool have a forbidden argument from `blockedArgs`? → reject
3. Does any argument value match a pattern from `argPatterns`? → reject

---

### 4. MaxToolCallsMiddleware

Prevents infinite tool call loops by capping the number of tool executions per agent run.

> **Note:** This is auto-added by `AiAgent` if not explicitly configured. It replaces the deprecated `AiAgent.maxInteractions` property.

```js
new MaxToolCallsMiddleware(
    maxCalls     = 10,                                        // maximum tool calls allowed
    exitBehavior = "end",                                     // "end" | "error"
    limitMessage = "Maximum tool calls reached. Task cannot proceed further."
)
```

**Example — strict limit with error on breach:**
```js
agent.withMiddleware(
    new MaxToolCallsMiddleware(
        maxCalls     = 25,
        exitBehavior = "error"
    )
);
```

- `"end"` — returns a graceful cancellation message and stops the agent
- `"error"` — throws `MaxToolCallsExceededException`

---

### 5. HumanInTheLoopMiddleware (HITL)

Pauses agent execution before sensitive tool calls for human approval, editing, or rejection. Integrates with the checkpointing system for web-based suspend/resume workflows.

```js
new HumanInTheLoopMiddleware(
    mode           = "dangerous",   // always | dangerous | regex | never
    dangerousTools = [...],         // override the default dangerous tool list
    regexPatterns  = [],            // patterns for "regex" mode
    reviewPrompt   = "Review this tool call:",
    includeArgs    = true           // include tool args in suspension payload
)
```

**Modes:**

| Mode | Behavior |
|------|----------|
| `always` | Suspend every tool call |
| `dangerous` | Suspend tools matching the dangerous list (delete, exec, send_email, etc.) |
| `regex` | Suspend tools whose names match any provided regex pattern |
| `never` | Disabled — never suspend |

**Default dangerous tool list:** `delete, remove, rm, drop, truncate, exec, shell, bash, cmd, write, save, create, send_email, transfer, withdraw, purchase`

---

## HITL Patterns

### CLI Mode (synchronous)

For scripts and command-line agents where you can block and wait for input:

```js
agent.withMiddleware(
    new HumanInTheLoopMiddleware( mode="dangerous" )
);

// In your onSuspend handler:
agent.withMiddleware({
    onSuspend: function( context ) {
        var data = context.data;
        systemOutput( "⏸ Review required: #data.toolName#" );
        systemOutput( "Args: #serializeJSON(data.toolArgs)#" );
        systemOutput( "Approve? (y/n): " );
        var input = getInput();  // blocking read

        if ( input == "y" ) {
            return AiMiddlewareResult.continue();
        } else {
            return AiMiddlewareResult.reject( "Rejected by user" );
        }
    }
});
```

### Web Suspend/Resume Mode (async)

For web applications where you need to pause, persist state, and resume later:

```js
// 1. Run the agent — it will suspend and return a threadId
var result = agent.run( userInput );

if ( result.suspended ) {
    var threadId = result.threadId;
    // Save threadId to session/database
    // Return response to user asking for approval
}

// 2. User approves via a web form — resume the agent
var resumeResult = agent.resume(
    threadId = threadId,
    decision = "approve"   // or "reject" or "edit"
);
```

---

## Checkpointer Configuration

The checkpointer persists agent state so suspended runs can be resumed across requests:

```js
agent.withCheckpointer(
    new FileCheckpointer( directory="/tmp/ai-checkpoints" )
);

// Or database-backed:
agent.withCheckpointer(
    new DBCheckpointer( datasource="myDB", table="ai_checkpoints" )
);
```

Checkpointer is required for web suspend/resume HITL workflows. Without it, suspension only works within the same request lifecycle (CLI mode).

---

## Writing Custom Middleware

Extend `BaseAiMiddleware` and override only the hooks you need:

```js
component extends="models.middleware.BaseAiMiddleware" {

    function beforeToolCall( required struct context ) {
        // Your logic here
        writeLog( "About to call: #context.toolName#", "information", "myapp" );
        return AiMiddlewareResult.continue();
    }

    function afterToolCall( required struct context ) {
        writeLog( "Finished: #context.toolName# → #context.result#", "information", "myapp" );
        return AiMiddlewareResult.continue();
    }

}
```

Then register it:
```js
agent.withMiddleware( new MyCustomMiddleware() );
```
