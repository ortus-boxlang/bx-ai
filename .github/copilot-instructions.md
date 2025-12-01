# BoxLang AI Module - AI Agent Instructions

## Project Overview

This is a **BoxLang module** providing unified AI provider integration. BoxLang is a modern dynamic JVM language (CFML-like syntax) with Java interop. The module exposes **Built-in Functions (BIFs)** written in BoxLang that interface with multiple AI providers (OpenAI, Claude, Gemini, Ollama, etc.) through a consistent API.

**Key Architecture:**
- **Hybrid codebase**: BoxLang (`.bx` files) for business logic + Java for runtime integration
- **Module structure**: `src/main/bx/` contains BoxLang source, compiled into `build/module/` for distribution
- **Provider pattern**: All AI services extend `BaseService` (OpenAI-compatible) implementing `IAiService` interface
- **Runnable pipelines**: Composable AI operations via `IAiRunnable` interface (models, messages, transformers)

## BoxLang Language Conventions

### Syntax Essentials
```java
// BoxLang looks like Java/CFML hybrid
class extends="BaseClass" implements="IInterface" {
    property name="field" type="string" default="";

    function methodName( required arg, optional param = "default" ) {
        return this;  // Fluent APIs are common
    }
}

// Imports - CRITICAL: Classes ALWAYS require imports defined at the top of the class definition
// Do NOT use inline imports inside methods/functions, that can only be used in scripts (bxs) or (bxm)
import bxModules.bxai.models.util.TextChunker;

// Call static methods using :: operator
result = TextChunker::chunk( text, options )

// Static variables must be referenced via static scope
static {
    DEFAULT_OPTIONS = { key: "value" };
}

function someMethod() {
    var config = static.DEFAULT_OPTIONS; // Must use static. prefix
}

// Struct append() without duplicate
var merged = sourceStruct.append( defaultStruct, false ); // false = no override

// Null-safe navigation and Elvis operator
result = service?.invoke( request ) ?: "default"

// Array/struct operations (dynamic and functional)
messages.map( m => m.content ).filter( c => !isNull(c) )
```

### Key Differences from Java
- **No semicolons required** (but allowed)
- **Duck typing**: `any` type allows dynamic dispatch
- **Built-in serialization**: `jsonSerialize()`, `jsonDeserialize()` (NOT serializeJSON/deserializeJSON)
- **Implicit returns**: Last expression in function is returned
- **String interpolation**: `"Hello, ${name}!"` or `"#name#"`
- **OnMissingMethod**: Dynamic method handling (see `AiMessage` for roled messages)
- **Rich string functions**: Comprehensive string manipulation BIFs + full Java String API access
  - Reference: https://boxlang.ortusbooks.com/boxlang-language/reference/built-in-functions/string
  - Examples: `char(10)` (newline), `left()`, `right()`, `reReplace()`, `trim()`, etc.

### Code Quality Standards
- **No cryptic variable names**: Use descriptive, self-documenting names (e.g., `maxConnections` not `M`)
- **Avoid acronyms**: Only use acronyms that are universally known (HTTP, URL, API). Prefer full words.
- **Type casting**: Use `castAs` operator instead of `javaCast()` function
  ```java
  // Good
  arguments.config.diversityFactor castAs "float"
  arguments.config.diversityFactor castAs float

  // Bad
  javaCast( "float", arguments.config.diversityFactor )
  ```

## Development Workflows

### Build & Test
```bash
# Full build (downloads BoxLang runtime, compiles module, runs tests)
./gradlew build

# Skip tests during development
./gradlew shadowJar -x test

# Run specific test class
./gradlew test --tests "ortus.boxlang.ai.bifs.aiMessageTest"

# Start Ollama for local testing
docker compose up -d ollama
curl http://localhost:11434/api/tags  # Verify model availability
```

### Module Development Cycle
1. Edit BoxLang source in `src/main/bx/`
2. Run `./gradlew shadowJar` to compile module structure into `build/module/`
3. Tests load module from `build/module/` (see `BaseIntegrationTest.loadModule()`)
4. Module registration happens at `@BeforeAll` - changes require test restart

### Testing Strategy
- **ALL tests MUST extend `BaseIntegrationTest`** - Provides module loading, runtime setup, and context management
- **Java test harness** (`JUnit 5`) executes **BoxLang test code** via `runtime.executeSource()`
- Tests inject module into BoxLang runtime from `build/module/` directory
- Use `variables.get("varName")` to extract BoxLang execution results from BoxLang execution context
- Test class pattern: `extends BaseIntegrationTest` → access `runtime`, `context`, `variables` properties
- Provider tests are `@Disabled` by default (require API keys in env vars like `OPENAI_API_KEY`)
- Ollama tests require `docker compose up ollama` (auto-pulls `qwen2.5:0.5b-instruct`)
- **Debugging AI Provider HTTP responses**: Add `logResponseToConsole: true` to AI service provider config (OpenAI, Claude, etc.) to see raw API responses in console output - useful for debugging provider integration issues

**BaseIntegrationTest provides:**
```java
protected static BoxRuntime runtime;           // BoxLang runtime instance
protected static ModuleService moduleService;  // Module management
protected static ModuleRecord moduleRecord;    // This module's record
protected ScriptingRequestBoxContext context;  // Execution context (created @BeforeEach)
protected IScope variables;                    // Variables scope for result extraction
```

## Critical Patterns

### BIF Creation (`src/main/bx/bifs/*.bx`)
```java
@BoxBIF  // Required annotation for BIF registration
class {
    static MODULE_SETTINGS = getModuleInfo( "bxai" ).settings;

    // BIF functions must be standalone (no instance state)
    // Access module settings via getModuleInfo()
}
```

### Provider Implementation (`src/main/bx/models/providers/*.bx`)
```java
class extends="BaseService" {
    function configure( required any apiKey ) {
        variables.apiKey = arguments.apiKey;
        return this;  // Always return this for fluent API
    }

    // Override invoke() and invokeStream() if provider differs from OpenAI standard
}
```

### Runnable Pipeline Pattern
```java
// All runnables implement: run(input, params), stream(onChunk, input, params), to(next)
var pipeline = aiModel("openai")
    .to( aiTransform( data => data.toUpper() ) )
    .to( aiTransform( data => data.trim() ) );

result = pipeline.run( "input" );  // Chains execution
```

### Event Interception Points
Module defines custom interception points in `ModuleConfig.bx`:
- `onAIRequest` / `onAIResponse` - Request/response lifecycle
- `onAIProviderCreate` - Custom provider registration
- `beforeAIModelInvoke` / `afterAIModelInvoke` - Pipeline hooks

## File Organization Logic

```
src/main/bx/
├── ModuleConfig.bx          # Module descriptor, settings, interceptor registration
├── bifs/                    # Global functions (aiChat, aiMessage, aiService, aiTool, etc.)
├── models/
│   ├── AiMessage.bx         # Fluent message builder (extends AiBaseRunnable)
│   ├── AiModel.bx           # AI provider runnable wrapper
│   ├── AiRequest.bx         # Request object with validation/merging logic
│   ├── Tool.bx              # Real-time function calling (implements ITool)
│   ├── providers/
│   │   ├── IAiService.bx    # Service interface (configure, invoke, getName)
│   │   ├── BaseService.bx   # OpenAI-compatible base (HTTP client logic)
│   │   └── *Service.bx      # Provider implementations (override as needed)
│   ├── runnables/
│   │   ├── IAiRunnable.bx   # Pipeline interface (run, stream, to)
│   │   └── AiBaseRunnable.bx # Base implementation
│   └── transformers/
│       └── AiTransformRunnable.bx # Data transformation in pipelines

build/module/                # Compiled module (shadowJar output)
```

## Common Pitfalls

1. **Module not found errors**: Run `./gradlew shadowJar` before testing - tests load from `build/module/`
2. **API key detection**: BIFs auto-detect `<PROVIDER>_API_KEY` env vars (e.g., `OPENAI_API_KEY`)
3. **Tool argument descriptions**: If missing, defaults to argument name (don't throw errors)
4. **Ollama model names**: Must include version tags (`qwen2.5:0.5b-instruct`, not `qwen2.5`)
5. **Streaming format**: Each provider has slightly different SSE chunk structure - handle nulls gracefully
6. **System messages**: Only ONE system message per request allowed by AI providers

## Integration Points

### HTTP Client (BaseService)
Uses BoxLang's `httpRequest` BIF with Java's HttpClient under the hood:
```java
var response = httpRequest( variables.chatURL )
    .setMethod( "POST" )
    .addHeader( "Authorization", "Bearer #variables.apiKey#" )
    .setBody( serializeJSON( dataPacket ) )
    .send();
```

### Event System
Leverage BoxLang's `BoxAnnounce()` BIF for module interception:
```java
BoxAnnounce( "onAIRequest", { dataPacket: payload, chatRequest: request, provider: this } );
```

**Important:** Use `BoxAnnounce()` (capital B, capital A) - this is the correct BoxLang BIF for event announcements, not `announce()`.

### GitHub Actions CI/CD
- Uses `hoverkraft-tech/compose-action@v2.0.2` to start Ollama service
- Waits for service readiness with timeout: `curl -f http://localhost:11434/api/tags`
- API keys injected via GitHub Secrets (`OPENAI_API_KEY`, `CLAUDE_API_KEY`, etc.)

## Documentation Locations

- **User docs**: `src/docs/` (markdown, organized by topic)
- **Main README**: `readme.md` (comprehensive BIF reference, examples)
- **Changelog**: `changelog.md` (Keep a Changelog format)
- **Examples**: `examples/*.bx` (runnable BoxLang scripts)

## When Making Changes

1. **Adding a BIF**: Create `src/main/bx/bifs/newBif.bx`, annotate with `@BoxBIF`, rebuild
2. **New provider**: Extend `BaseService`, implement `configure()/invoke()`, add to tests
3. **Breaking changes**: Update changelog with migration guide, bump major version
4. **Tests**: Match Java test class naming (`*Test.java`), use `@DisplayName` for readability
5. **Model defaults**: Update in provider's `configure()` or `defaults()` method

## Questions to Clarify

- Are there specific provider quirks or edge cases you've encountered that need special handling?
- Do you prefer tool argument validation to be strict (throw errors) or lenient (default values)?
- Should streaming responses accumulate full text or only pass chunks to callbacks?
