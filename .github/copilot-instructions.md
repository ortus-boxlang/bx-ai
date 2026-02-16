# BoxLang AI Module - AI Agent Instructions

## ⚠️ CRITICAL: Do Not Hallucinate

**If you don't know something, DO NOT make it up. Either:**
1. **Ask the user** for clarification
2. **Search the codebase** using available tools (grep_search, semantic_search, read_file)
3. **Check the actual BIF files** in `src/main/bx/bifs/` to verify function names and signatures
4. **Acknowledge uncertainty** - It's better to say "I'm not sure, let me check" than to provide incorrect information

**Never assume:**
- Function names or BIF names that you haven't verified
- API signatures or parameter names
- Class names or file locations
- Feature availability

**Always verify before suggesting code that uses:**
- BIFs (Built-in Functions) - check `src/main/bx/bifs/*.bx`
- Classes - check `src/main/bx/models/`
- Configuration options - check actual source files

**Prefer simplicity/pragmatism over complexity**

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
- **Implicit getters/setters**: Properties declared with `property` automatically get getter/setter methods
  - `property name="serverName"` → `getServerName()` and `setServerName(value)` are auto-generated
  - Do NOT manually create getter/setter methods for properties
  - Access via `obj.getPropertyName()` or `obj.setPropertyName(value)`
- **Rich string functions**: Comprehensive string manipulation BIFs + full Java String API access
  - Reference: https://boxlang.ortusbooks.com/boxlang-language/reference/built-in-functions/string
  - Examples: `char(10)` (newline), `left()`, `right()`, `reReplace()`, `trim()`, etc.

### Code Quality Standards
- **No cryptic variable names**: Use descriptive, self-documenting names (e.g., `maxConnections` not `M`)
- **Avoid acronyms**: Only use acronyms that are universally known (HTTP, URL, API). Prefer full words.
- **Avoid reserved scope names**: BoxLang has built-in scopes that cannot be used as variable names:
  - `server`, `request`, `session`, `application`, `cgi`, `url`, `form`, `cookie`, `variables`
  - Use alternative names like `mcpSrv`, `rpcRequest`, `httpReq`, etc.
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

## Event System (Expanded) 📡

### Complete Interception Points
Module defines **40 custom interception points** in [ModuleConfig.bx](src/main/bx/ModuleConfig.bx#L151-L201) organized by category:

**Agent Events** (3):
- `beforeAIAgentRun`, `afterAIAgentRun` - Agent execution lifecycle
- `onAIAgentCreate` - Agent instantiation

**Model & Provider Events** (5):
- `beforeAIModelInvoke`, `afterAIModelInvoke` - Model execution lifecycle
- `onAIModelCreate` - Model instantiation
- `onAIProviderCreate`, `onMissingAiProvider` - Provider management

**Request & Response Events** (5):
- `onAIChatRequest`, `onAIChatRequestCreate` - Request creation
- `onAIChatResponse` - Response received
- `onAIRateLimitHit`, `onAIError` - Error handling

**Embedding Events** (4):
- `beforeAIEmbed`, `afterAIEmbed` - Embedding lifecycle
- `onAIEmbedRequest`, `onAIEmbedResponse` - Embedding API calls

**Pipeline Events** (2):
- `beforeAIPipelineRun`, `afterAIPipelineRun` - Pipeline execution

**Tool Events** (3):
- `beforeAIToolExecute`, `afterAIToolExecute` - Tool invocation
- `onAIToolCreate` - Tool registration

**Component Creation Events** (7):
- `onAiLoaderCreate` - Document loader creation
- `onAiMemoryCreate` - Memory instance creation
- `onAIMessageCreate` - Message creation
- `onAITransformerCreate` - Transformer creation

**Utility Events** (1):
- `onAITokenCount` - Token usage tracking (includes multi-tenant: `tenantId`, `usageMetadata`)

**MCP Events** (5):
- `onMCPServerCreate`, `onMCPServerRemove` - Server lifecycle
- `onMCPRequest`, `onMCPResponse`, `onMCPError` - MCP operations

### Event Data Structures
Events include rich context. Example for `onAITokenCount`:
```javascript
{
    provider: providerInstance,
    operation: "chat",              // or "embed", "stream"
    model: "gpt-4",
    promptTokens: 100,
    completionTokens: 50,
    totalTokens: 150,
    aiRequest: chatRequest,         // Full request object
    usage: rawUsageObject,          // Provider's raw usage data
    tenantId: "tenant-123",         // Multi-tenant tracking
    usageMetadata: { costCenter: "engineering" },
    providerOptions: {},
    timestamp: now()
}
```

### Event Registration
- **For modules**: Add to `interceptors` array in [ModuleConfig.bx](src/main/bx/ModuleConfig.bx)
  ```javascript
  interceptors: [
      { class: "path.to.MyInterceptor" }
  ]
  ```
- **For applications/scripts**: Use `BoxRegisterInterceptor()` BIF
  ```javascript
  BoxRegisterInterceptor( "onAITokenCount", function( event ) {
      // Track usage
  });
  ```

**Important**: Use `BoxAnnounce()` (capital B, capital A) to fire events from code.

## Memory Systems 🧠

### Memory Interface
- **Interface**: `IAiMemory` ([models/memory/IAiMemory.bx](src/main/bx/models/memory/IAiMemory.bx) - 297 lines)
  - **Identity Methods**: `name()`, `key()`, `metadata()`
  - **Management**: `add()`, `seed()`, `seedAsync()`, `getAll()`, `clear()`, `count()`
  - **Retrieval**: `getLast()`, `getFirst()`, `trim()`, `getSystemMessage()`
  - **Context**: `getContext()`, `setContext()`, `mergeContext()`
  - **Async Pattern**: `seedAsync()` returns `BoxFuture`, uses `asyncRun(() => {}, "io-tasks")`

### Base Memory Implementation
- **Base Class**: `BaseMemory` ([models/memory/BaseMemory.bx](src/main/bx/models/memory/BaseMemory.bx) - 682 lines)
  - **Properties**: `key`, `userId`, `conversationId`, `metadata`, `name`, `config`, `messages`, `maxMessages`
  - **Features**:
    - Auto-trims on add when maxMessages exceeded
    - Multi-tenant isolation via `userId` and `conversationId`
    - Message normalization (accepts string, struct, AiMessage, Document)
    - System message handling (only ONE allowed per conversation)

### Memory Types

**Standard Memory Implementations** (in [models/memory/](src/main/bx/models/memory/)):

1. **CacheMemory** - Uses BoxLang cache providers (default: `default` cache)
   - Persistent across requests within cache TTL
   - Configurable cache name

2. **FileMemory** - JSON file persistence
   - Stores conversations in `.json` files
   - Auto-creates directory structure

3. **SessionMemory** - HTTP session storage (web-only)
   - Bound to user's HTTP session
   - Cleared on session timeout

4. **WindowMemory** - Fixed-size sliding window
   - Keeps only last N messages
   - Most efficient for limited context

5. **SummaryMemory** - AI-powered conversation summarization
   - Summarizes old messages to preserve context
   - Reduces token usage for long conversations

6. **JdbcMemory** - Database storage via JDBC
   - Full persistence with SQL queries
   - Multi-tenant ready with indexed columns

7. **HybridMemory** - **Combines WindowMemory + VectorMemory**
   - Properties: `recentMemory`, `vectorMemory`, `recentLimit`, `semanticLimit`, `totalLimit`, `recentWeight`
   - Returns recent messages + relevant older messages from vector search
   - Smart deduplication when merging sources
   - Best for long-running conversations with semantic retrieval

```javascript
// Example: Using hybrid memory
var memory = aiMemory( "hybrid", {
    recentLimit: 10,        // Last 10 messages always included
    semanticLimit: 5,       // Up to 5 relevant older messages
    recentWeight: 0.7,      // Prioritize recent over semantic
    vectorMemory: aiMemory( "box", { collection: "conversation" } )
});
```

## Vector Storage 🔍

### Vector Memory Interface
- **Interface**: `IVectorMemory` ([models/memory/vector/IVectorMemory.bx](src/main/bx/models/memory/vector/IVectorMemory.bx))
  - **Extends**: `IAiMemory` (all standard memory methods available)
  - **Semantic Retrieval**:
    - `getRelevant(query, limit, filter, minScore)` - Text-based semantic search
    - `findSimilar(embedding, limit, filter)` - Pre-computed vector search
  - **Storage**:
    - `addWithId(id, text, metadata)` - Upsert by custom ID
    - `upsert()` - Alias for addWithId
  - **Management**:
    - `getById(id)`, `remove(id)`, `removeWhere(filter)`
    - `createCollection(name)`, `deleteCollection(name)`, `listCollections()`

### Base Vector Implementation
- **Base Class**: `BaseVectorMemory` ([models/memory/vector/BaseVectorMemory.bx](src/main/bx/models/memory/vector/BaseVectorMemory.bx) - 808 lines)
  - **Properties**:
    - `collection` - Collection/index name
    - `embeddingProvider`, `embeddingModel` - Auto-embedding via `aiEmbed()`
    - `dimensions`, `metric` - Vector configuration
    - `embeddingOptions` - Provider-specific embedding params
  - **Caching**: `useCache`, `cacheName`, `cacheTimeout`, `cacheInstance`
  - **Features**:
    - Auto-generates embeddings for text input
    - Hash-based ID generation (`SHA-256` of text)
    - Multi-tenant filtering by `userId` and `conversationId`
    - Intelligent caching of embeddings

### Vector Providers

**11 Vector Store Implementations** (in [models/memory/vector/](src/main/bx/models/memory/vector/)):

1. **BoxVectorMemory** - In-memory Java-based (default, no external dependencies)
2. **ChromaVectorMemory** - Chroma DB
3. **PineconeVectorMemory** - Pinecone cloud service
4. **QdrantVectorMemory** - Qdrant
5. **WeaviateVectorMemory** - Weaviate
6. **MilvusVectorMemory** - Milvus
7. **TypesenseVectorMemory** - Typesense
8. **OpenSearchVectorMemory** - OpenSearch
9. **PostgresVectorMemory** - PostgreSQL with pgvector extension
10. **MysqlVectorMemory** - MySQL with vector support

**Distance Metrics**: `cosine` (default), `euclidean`, `dot_product`

```javascript
// Example: Vector memory with custom embeddings
var vectorMemory = aiMemory( "pinecone", {
    apiKey: "your-api-key",
    collection: "customer-support",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    dimensions: 1536,
    metric: "cosine",
    useCache: true
});

// Semantic search
var relevantMessages = vectorMemory.getRelevant( "shipping issues", 5 );
```

## Document Loaders 📂

### Loader Interface
- **Interface**: `IDocumentLoader` ([models/loaders/IDocumentLoader.bx](src/main/bx/models/loaders/IDocumentLoader.bx) - 244 lines)
  - **Core Loading**:
    - `load()` - Load all documents into memory
    - `loadAsync()` - Returns `BoxFuture` for non-blocking
    - `loadBatch(batchSize)` - Memory-efficient batch iteration
    - `loadAsStream()` - Returns Java Stream for lazy processing
  - **Functional API** (chainable):
    - `each(callback)` - Process documents one-by-one
    - `filter(predicate)` - Filter during load
    - `map(transformer)` - Transform during load
    - `chunk(chunkSize, overlap, strategy)` - Auto-chunk all documents
  - **Advanced Ingestion**:
    - `ingest(memory, options)` - Direct to memory with chunking/deduplication
    - `ingestAsync(memory, options)` - Non-blocking ingest with progress tracking

### Base Loader Implementation
- **Base Class**: `BaseDocumentLoader` ([models/loaders/BaseDocumentLoader.bx](src/main/bx/models/loaders/BaseDocumentLoader.bx) - 767 lines)
  - **Properties**: `source`, `config`, `documents`, `currentIndex`, `documentsLoaded`, `errors`
  - **Chain Support**: `filter`, `map`, `progressCallback` functions
  - **Default Config**:
    ```javascript
    {
        encoding: "UTF-8",
        includeMetadata: true,
        continueOnError: true,
        chunkSize: 0,
        overlap: 0,
        chunkStrategy: "recursive"
    }
    ```
  - **Ingest Options**:
    - Deduplication via similarity threshold
    - Token counting and tracking
    - Async batch processing
    - Progress callbacks

### Loader Types

**14 Document Loader Implementations** (in [models/loaders/](src/main/bx/models/loaders/)):

1. **TextLoader** - Plain text files (.txt)
2. **CSVLoader** - CSV with column mapping
3. **JSONLoader** - JSON with JSONPath extraction
4. **XMLLoader** - XML with XPath queries
5. **MarkdownLoader** - Markdown files with front matter
6. **PDFLoader** - PDF text extraction
7. **LogLoader** - Log file parsing with pattern matching
8. **HTTPLoader** - Web page scraping
9. **FeedLoader** - RSS/Atom feed parsing
10. **WebCrawlerLoader** - Multi-page recursive crawling
11. **DirectoryLoader** - Recursive directory scanning
12. **SQLLoader** - Database query results as documents

```javascript
// Example: Load PDFs with chunking, directly into vector memory
var loader = aiDocuments( "pdf", "./docs/*.pdf" )
    .filter( doc => doc.getContentLength() > 100 )
    .chunk( 1000, 200, "recursive" );

// Ingest with deduplication
loader.ingest( vectorMemory, {
    deduplication: true,
    similarityThreshold: 0.95,
    trackTokens: true
});
```

### Document Model
- **Class**: `Document` ([models/Document.bx](src/main/bx/models/Document.bx) - 422 lines)
  - **Properties**: `id`, `content`, `metadata`, `embedding`
  - **Content Methods**:
    - `hasContent()`, `getContentLength()`, `preview(length)`
  - **Token Methods**:
    - `getTokenCount()` - Uses 4 chars/token heuristic
    - `exceedsTokenLimit(limit)` - Validation
  - **Chunking**:
    - `chunk(chunkSize, overlap, strategy)` - Returns array of Documents
    - Preserves metadata, generates unique IDs for chunks
  - **Validation**:
    - `validate(minLength, maxLength, requiredMetadata)` - Throws on invalid
  - **Serialization**:
    - `toStruct()`, `toJSON()` - Export for storage/API

## MCP (Model Context Protocol) 🔌

### MCP Client
- **Class**: `MCPClient` ([models/mcp/MCPClient.bx](src/main/bx/models/mcp/MCPClient.bx) - 449 lines)
  - **Purpose**: Fluent API for consuming external MCP servers
  - **Configuration** (chainable):
    - `withTimeout(ms)`, `withHeaders(headers)`
    - `withAuth(user, pass)`, `withBearerToken(token)`
    - `onSuccess(callback)`, `onError(callback)`
  - **Discovery**:
    - `listTools()` - Get available tools from server
    - `listResources()` - Get available resources
    - `listPrompts()` - Get available prompt templates
  - **Execution**:
    - `callTool(name, params)` - Invoke remote tool
    - `getResource(uri)` - Fetch resource by URI
    - `getPrompt(name, params)` - Get prompt with parameter substitution
  - **Response Handling**: Returns `MCPResponse` with fluent checking methods

```javascript
// Example: Consume an MCP server
var client = MCP( "http://localhost:3000" )
    .withTimeout( 5000 )
    .withBearerToken( "secret" )
    .onError( error => systemOutput( error, true ) );

var tools = client.listTools();
var result = client.callTool( "search", { query: "BoxLang" } );
```

### MCP Server
- **Class**: `MCPServer` ([models/mcp/MCPServer.bx](src/main/bx/models/mcp/MCPServer.bx) - **1542 lines!** - This is HUGE)
  - **Purpose**: Create an MCP server exposing tools/resources/prompts
  - **Core Properties**:
    - Identity: `serverName`, `description`, `version`
    - Security: `corsAllowedOrigins`, `basicAuthUsername`, `basicAuthPassword`, `apiKeyProvider`
    - Limits: `maxRequestBodySize`, `rateLimitPerMinute`
    - Callbacks: `onRequest`, `onResponse`, `onError`
    - Resources: `tools`, `resources`, `prompts` (registration maps)
    - Monitoring: `stats` (MCPServerStats with request counts, errors, latency)

  - **Registration Methods**:
    - `registerTool(tool)` - Add AI tool for invocation
    - `registerResource(uri, definition)` - Add accessible resource
    - `registerPrompt(name, definition)` - Add prompt template
    - `enableCORS(origins)` - Configure CORS headers
    - `requireAuth(user, pass)` - Enable HTTP Basic Auth
    - `requireApiKey(provider)` - Custom API key validation

  - **Request Handling**:
    - `handleRequest(requestBody)` - JSON-RPC 2.0 processor
    - `processRequest(normalized)` - Internal routing to methods
    - Methods: `tools/list`, `resources/list`, `prompts/list`, `tools/call`, `resources/read`, `prompts/get`

  - **JSON-RPC Error Codes** (static):
    - `INVALID_REQUEST: -32600` - Malformed JSON-RPC
    - `METHOD_NOT_FOUND: -32601` - Unknown method
    - `INVALID_PARAMS: -32602` - Invalid parameters
    - `INTERNAL_ERROR: -32603` - Server error
    - `PARSE_ERROR: -32700` - JSON parse failed
    - `RATE_LIMIT_EXCEEDED: -32002` - Too many requests
    - `CONTENT_TYPE_ERROR: -32050` - Wrong content type
    - `TIMEOUT_ERROR: -32070` - Request timeout

```javascript
// Example: Create MCP server
var mcpSrv = MCPServer( "my-tools-server", "Provides custom business tools" )
    .registerTool( aiTool( "getCustomer", "Fetch customer by ID", { id: "string" }, fetchCustomer ) )
    .registerResource( "config://app", { type: "config", getData: getAppConfig } )
    .enableCORS( ["*"] )
    .requireAuth( "admin", "secret" );

// Handle HTTP request
var response = mcpSrv.handleRequest( requestBody );
```

### MCP Transports
**Transport Implementations** (in [models/mcp/transports/](src/main/bx/models/mcp/transports/)):

1. **ITransport** - Interface for transport layers
2. **BaseTransport** - Abstract base with common logic
3. **HTTPTransport** - REST API over HTTP/HTTPS
4. **StdioTransport** - Standard I/O for CLI tools
   - Uses `cliRead()` for blocking line input
   - JSON-RPC notifications for server-initiated messages
   - Ideal for process-based integrations

### MCP Supporting Classes
- **MCPRequestProcessor** ([models/mcp/MCPRequestProcessor.bx](src/main/bx/models/mcp/MCPRequestProcessor.bx))
  - Normalizes HTTP/STDIO requests into common format
  - Extracts server name, method, body, headers
  - Request metadata tracking

- **MCPResponse** ([models/mcp/MCPResponse.bx](src/main/bx/models/mcp/MCPResponse.bx))
  - Fluent response checking: `isSuccess()`, `isError()`, `getData()`, `getError()`

## Agent System 🤖

### AI Agents
- **Class**: `AiAgent` ([models/AiAgent.bx](src/main/bx/models/AiAgent.bx) - 630 lines)
  - **Purpose**: Autonomous entities with reasoning, tools, memory, and sub-agents
  - **Core Properties**:
    - Identity: `agentName`, `description`, `instructions`
    - AI: `aiModel` (model instance or name)
    - Tools: `tools` (array of Tool instances)
    - Memory: `memories` (array of IAiMemory - multi-memory support!)
    - Delegation: `subAgents` (array of AiAgent - auto-wrapped as tools)
    - Config: `params`, `options`

  - **Key Features**:
    - **Multi-memory support**: Can use multiple memory sources (e.g., vector + cache)
    - **Tool delegation**: Automatically registers tools for execution
    - **Sub-agent composition**: Child agents become callable tools
    - **System message auto-generation**: Combines description + instructions
    - **Memory integration**: Auto-loads context before execution, saves after

  - **Execution**:
    - `run(input, params, options)` - Execute with memory loading/saving
    - `stream(callback, input, params, options)` - Streaming execution
    - Implements `IAiRunnable` - can be used in pipelines via `.to()`

  - **Return Formats**: `single` (default), `all`, `raw`, `structuredOutput`

```javascript
// Example: Agent with tools and memory
var supportAgent = aiAgent(
    agentName: "customer-support",
    description: "Expert customer support agent",
    instructions: "Always be polite and check knowledge base first",
    aiModel: "openai",
    tools: [
        aiTool( "searchKB", "Search knowledge base", { query: "string" }, searchKnowledgeBase ),
        aiTool( "createTicket", "Create support ticket", { issue: "string", priority: "string" }, createTicket )
    ],
    memories: [
        aiMemory( "cache", { key: "support-session" } )
    ]
);

var response = supportAgent.run( "My order hasn't arrived" );
```

### Sub-Agents Pattern
```javascript
// Example: Main agent delegating to specialized sub-agents
var researchAgent = aiAgent( agentName: "researcher", instructions: "Research topics", ... );
var writerAgent = aiAgent( agentName: "writer", instructions: "Write articles", ... );

var coordinatorAgent = aiAgent(
    agentName: "coordinator",
    description: "Coordinates research and writing",
    subAgents: [ researchAgent, writerAgent ]  // Auto-wrapped as tools!
);

// Coordinator can now call "researcher" and "writer" as tools
var result = coordinatorAgent.run( "Write an article about BoxLang AI" );
```

## Transformers 🔄

### Transformer Interface
- **Interface**: `ITransformer` ([models/transformers/ITransformer.bx](src/main/bx/models/transformers/ITransformer.bx))
  - Methods: `configure(config)`, `transform(input)` (abstract)

- **Base Class**: `BaseTransformer` ([models/transformers/BaseTransformer.bx](src/main/bx/models/transformers/BaseTransformer.bx))
  - Config management: `setConfigValue()`, `getConfigValue()`, `hasConfigValue()`
  - Validation and defaults handling

### Transformer Types
**4 Built-in Transformers** (in [models/transformers/](src/main/bx/models/transformers/)):

1. **JSONExtractorTransformer** - Extract/parse JSON from AI responses
   - Config: `stripMarkdown`, `strictMode`, `extractPath`, `validateSchema`
   - Handles markdown code blocks: ` ```json ... ``` `

2. **XMLExtractorTransformer** - Extract/parse XML from responses
   - XPath support for targeted extraction

3. **CodeExtractorTransformer** - Extract code blocks by language
   - Config: `language` (e.g., "javascript", "python")
   - Returns clean code without fence markers

4. **TextCleanerTransformer** - Normalize and clean text
   - Trim whitespace, normalize line endings, remove special chars

```javascript
// Example: Pipeline with JSON extraction
var pipeline = aiModel( "openai" )
    .to( aiTransform( "json", { stripMarkdown: true, strictMode: false } ) );

var data = pipeline.run( "Generate a JSON object with user info" );
// Returns parsed struct, not raw JSON string
```

### Integration with Pipelines
- Transformers work seamlessly with `AiTransformRunnable`
- Used via `aiTransform()` BIF or `AiTransformRunnable` directly
- Can be chained: `model.to(transform1).to(transform2)`

## Utilities 🛠️

### Text Chunking
- **Class**: `TextChunker` ([models/util/TextChunker.bx](src/main/bx/models/util/TextChunker.bx) - 423 lines)
  - **Static class**: `TextChunker::chunk(text, options)`
  - **Strategies**:
    1. `recursive` - Recursive splitting by multiple delimiters (default)
    2. `characters` - Fixed character count
    3. `words` - Split by word boundaries
    4. `sentences` - Sentence-based chunking
    5. `paragraphs` - Paragraph-based chunking
  - **Default Config**: 2000 chars, 200 overlap
  - **Returns**: Array of text chunks with overlap for context preservation
  - **Used By**: `Document.chunk()`, loaders, memory ingestion

```javascript
// Example: Chunk text
var chunks = TextChunker::chunk( longText, {
    chunkSize: 1000,
    overlap: 100,
    strategy: "recursive"
});
```

### Token Counting
- **Class**: `TokenCounter` ([models/util/TokenCounter.bx](src/main/bx/models/util/TokenCounter.bx) - 100 lines)
  - **Static class**: `TokenCounter::count(text, options)`
  - **Methods**:
    - `characters` - 4 characters per token (default heuristic)
    - `words` - Word count × 1.3 multiplier
  - **Returns**: Numeric count or detailed stats if `detailed: true`

```javascript
// Example: Count tokens
var tokens = TokenCounter::count( message, { method: "characters" } );
// ~250 tokens for 1000 chars
```

### Schema Builder
- **Class**: `SchemaBuilder` ([models/util/SchemaBuilder.bx](src/main/bx/models/util/SchemaBuilder.bx) - 554 lines)
  - **Purpose**: Convert BoxLang classes/structs to JSON schemas for structured AI output
  - **Static Methods**:
    - `fromObject(target)` - Auto-detect type and build schema
    - `fromClass(instance)` - Build from class properties
    - `fromStruct(struct)` - Build from struct definition
    - `fromArray(arrayTemplate)` - Build array schema
  - **Features**:
    - Caches schemas by class path for performance
    - Supports nested objects and arrays
    - Handles BoxLang property metadata (type, required, default)
  - **Population**:
    - `populateFromResponse(schema, response)` - Maps AI response back to typed objects
    - Used by `aiPopulate()` BIF

```javascript
// Example: Structured output with schema
class User {
    property name="firstName" type="string";
    property name="lastName" type="string";
    property name="age" type="numeric";
}

var user = aiChat(
    "Extract user info: John Doe, 30 years old",
    returnFormat: new User()
);
// Returns populated User instance, not raw text!
```

### AWS Signature V4
- **Class**: `AwsSignatureV4` ([models/util/AwsSignatureV4.bx](src/main/bx/models/util/AwsSignatureV4.bx) - 318 lines)
  - **Purpose**: AWS Signature Version 4 signing for Bedrock API
  - **Method**: `signRequest(method, host, path, payload, region, service, credentials)`
  - **Features**:
    - Handles temporary credentials with session tokens
    - HMAC-SHA256 signature generation
    - Canonical request formatting
  - **Used By**: BedrockService for authenticated API calls

## Tools System (Expanded) 🔧

### Tool Interface
- **Interface**: `ITool` ([models/ITool.bx](src/main/bx/models/ITool.bx) - 41 lines)
  - **Methods**: `getName()`, `getSchema()`, `invoke(args)`
  - Very simple contract for maximum flexibility

### Tool Implementation
- **Class**: `Tool` ([models/Tool.bx](src/main/bx/models/Tool.bx))
  - **Purpose**: Real-time function calling during AI conversations
  - **Properties**: `name`, `description`, `parameters`, `callback`
  - **Schema Generation**:
    - Auto-generates JSON schema from `parameters` struct
    - Parameter format: `{ name: "query", type: "string", description: "...", required: true }`
    - Defaults description to parameter name if missing (lenient validation)
  - **Invocation**:
    - Validates required parameters
    - Executes `callback` with arguments
    - Returns string result (auto-serializes structs/arrays if needed)
  - **Integration**: Works seamlessly with provider tool calling (OpenAI format)

```javascript
// Example: Tool creation and usage
function searchProducts( required string query, numeric maxResults = 10 ) {
    // Search logic
    return queryResults.toJSON();
}

var tool = aiTool(
    "searchProducts",
    "Search product catalog",
    [
        { name: "query", type: "string", description: "Search query", required: true },
        { name: "maxResults", type: "number", description: "Max results", required: false }
    ],
    searchProducts
);

// Use with model
var result = aiChat(
    "Find me wireless headphones",
    tools: [ tool ]
);
// AI automatically calls searchProducts() when needed
```

### Dynamic Tool Registration
Tools can be registered with:
- **Models**: `aiModel("openai").addTools([tools])`
- **Agents**: `aiAgent(tools: [tool1, tool2])`
- **MCP Servers**: `mcpServer().registerTool(tool)`

### Sub-Agents as Tools
- **Pattern**: `aiAgent(subAgents: [agent1, agent2])` auto-wraps agents as callable tools
- Each sub-agent becomes a tool with its name and description
- Enables hierarchical agent delegation

## Configuration Architecture (Expanded) ⚙️

### Module Settings Structure
Complete settings from [ModuleConfig.bx](src/main/bx/ModuleConfig.bx#L103-L147):
```javascript
{
    provider: "openai",               // Default provider name
    apiKey: "",                       // Default API key (or use env vars)
    defaultParams: {},                // Default request parameters
    memory: {                         // Default memory configuration
        provider: "window",           // Memory type
        config: {}                    // Memory-specific config
    },
    providers: {                      // Provider-specific overrides
        openai: {
            params: { model: "gpt-4", temperature: 0.7 },
            options: { timeout: 60 }
        },
        ollama: {
            params: { model: "qwen2.5:0.5b-instruct" }
        }
    },
    timeout: 30,                      // Default HTTP timeout (seconds)
    logRequest: false,                // Log requests to file
    logRequestToConsole: false,       // Print requests to console
    logResponse: false,               // Log responses to file
    logResponseToConsole: false,      // Print responses to console (DEBUGGING!)
    returnFormat: "single"            // Default return format
}
```

### Provider Configuration Patterns
**Standard Provider**:
```javascript
// Simple API key
aiService( "openai", "sk-..." )

// Full configuration
aiService( "openai", {
    apiKey: "sk-...",
    params: { model: "gpt-4", temperature: 0.7, max_tokens: 2000 },
    options: { logResponseToConsole: true, timeout: 60 }
})
```

**AWS Bedrock (Special Case)**:
```javascript
aiService( "bedrock", {
    region: "us-east-1",
    awsAccessKeyId: "AKI...",
    awsSecretAccessKey: "...",
    modelId: "anthropic.claude-3-sonnet-20240229-v1:0",
    params: { max_tokens: 4096 }
})
```

### Request Configuration Hierarchy
1. **AiBaseRequest** - Base for all requests
   - Common: `params`, `provider`, `apiKey`, `model`, `timeout`, `returnFormat`
   - Logging: `logRequest`, `logRequestToConsole`, `logResponse`, `logResponseToConsole`
   - Advanced: `headers`, `maxInteractions`, `tenantId`, `usageMetadata`, `providerOptions`

2. **AiChatRequest** - Chat-specific (extends AiBaseRequest)
   - Adds: `messages`, `stream`, `structuredOutput`, `aiMessage`, `tools`
   - Context injection: `options.context` merges variables into message templates

## Streaming Architecture 🌊

### Stream Methods Across Components
- **Provider Level**: `invokeStream(chatRequest, callback)`
- **Model Level**: `stream(callback, input, params)`
- **Agent Level**: `stream(callback, input, params, options)`

### BaseService Streaming Implementation
From [BaseService.bx](src/main/bx/models/providers/BaseService.bx#L394-L428):

```javascript
function chatStream( required chatRequest, required callback ) {
    // Uses BoxLang's httpRequestStream() BIF for SSE
    var response = httpRequestStream( variables.chatURL )
        .setMethod( "POST" )
        .addHeader( "Authorization", "Bearer #variables.apiKey#" )
        .setBody( serializeJSON( dataPacket ) )
        .onChunk( function( chunk ) {
            // Filter SSE events: "data: {...}"
            if ( !chunk.startsWith( "data: " ) ) return;

            try {
                var jsonData = jsonDeserialize( chunk.mid( 7 ) );
                var delta = jsonData.choices[1].delta.content ?: "";

                if ( !isNull( delta ) && delta != "" ) {
                    callback( delta );  // User callback with incremental text
                }
            } catch( any e ) {
                // Graceful error handling - log and continue
            }
        })
        .send();
}
```

**Callback Signature**: `function(chunk)` where chunk is incremental text

**Chunk Processing**:
1. Filter SSE event lines (start with `data: `)
2. Parse JSON chunk
3. Extract `delta.content` or `choices[0].delta.content`
4. Call user callback with incremental text only

### Provider-Specific Streaming
- **OpenAI-compatible**: Standard SSE format via BaseService
- **Ollama**: Custom `chatStream()` override with different delta structure
- **Non-streaming providers**: Throw error in `chatStream()` (e.g., VoyageService for embeddings)

### Error Handling in Streams
- Try/catch around JSON parsing for malformed chunks
- Graceful handling of null/undefined deltas
- Stream interruption on critical errors (logs to `ai` logger)
- Continue on recoverable errors (missing fields, etc.)

## Request/Response Flow 🔄

### Complete Execution Flow
1. **User calls BIF**: `aiChat("prompt", params, options)`
2. **BIF creates request objects**:
   - Creates `AiChatRequest` with merged params
   - Creates `AiMessage` if string prompt provided
   - Merges module settings → provider defaults → user params
3. **Service routing**:
   - Lookup/create provider via `aiService()` or module default
   - Fire `onAIProviderCreate` event if new
4. **Service execution**:
   - `configure()` - Set API key, URLs, params
   - `invoke()` - Route to `chat()` method
   - Fire `beforeAIModelInvoke` event
5. **HTTP request** (`BaseService.sendRequest()`):
   - Build request via `httpRequest` BIF
   - Add headers (Authorization, Content-Type)
   - Send POST with JSON body
6. **Response processing**:
   - Check for errors → throw `ProviderError` with details
   - Fire `onAITokenCount` if usage data present (multi-tenant tracking)
   - Extract response content
7. **Tool call handling** (if `tool_calls` present):
   - Extract tool calls from response
   - Find tool via `chatRequest.getTool(name)`
   - Invoke: `tool.invoke(JSONDeserialize(arguments))`
   - Append result as `role: tool` message
   - Increment interaction counter
   - **Recurse to step 5** until no more tool calls OR maxInteractions reached
8. **Return format processing**:
   - `single` - Return first message content only (default)
   - `all` - Return all messages array
   - `raw` - Return full provider response
   - `json` - Parse and return JSON
   - `xml` - Parse and return XML
   - `structuredOutput` - Populate object via SchemaBuilder
9. **Fire events**: `afterAIModelInvoke`, `onAIChatResponse`

### Tool Execution Recursion
```javascript
// Simplified flow
function chat( chatRequest ) {
    var response = sendRequest( chatRequest );

    // Check for tool calls
    if ( response.tool_calls.len() > 0 && chatRequest.maxInteractions > 0 ) {
        // Execute each tool
        response.tool_calls.each( toolCall => {
            var tool = chatRequest.getTool( toolCall.name );
            var result = tool.invoke( jsonDeserialize( toolCall.arguments ) );

            // Append tool result as new message
            chatRequest.addMessage( role: "tool", content: result, tool_call_id: toolCall.id );
        });

        // Recurse with updated messages, decrement interactions
        chatRequest.maxInteractions--;
        return chat( chatRequest );  // RECURSIVE CALL
    }

    return response;
}
```

## Key Architectural Patterns 🎯

### 1. Fluent APIs Everywhere
Nearly all classes return `this` for method chaining:
```javascript
var agent = aiAgent( "support" )
    .addTool( searchTool )
    .addMemory( cacheMemory )
    .run( "Help me" );

var pipeline = aiModel( "openai" )
    .to( aiTransform( "json" ) )
    .to( aiTransform( data => data.result ) )
    .run( "Generate data" );
```

### 2. Static Utility Classes
Use `::` operator for static methods, `static.` for static variables:
```javascript
// Static methods
var chunks = TextChunker::chunk( text, options );
var tokens = TokenCounter::count( text );
var schema = SchemaBuilder::fromObject( new User() );

// Static variables in classes
static {
    DEFAULT_CHUNK_SIZE = 2000;
}

function someMethod() {
    var size = static.DEFAULT_CHUNK_SIZE;  // NOT variables.DEFAULT_CHUNK_SIZE
}
```

### 3. Interface-Driven Design
All major components have interfaces enabling polymorphism:
- `IAiService` → 18 provider implementations
- `IAiMemory` → 7 memory types + `IVectorMemory` → 11 vector stores
- `IDocumentLoader` → 14 loader types
- `ITool` → Custom tool implementations
- `IAiRunnable` → Models, agents, messages, transformers
- `ITransformer` → 4 transformer types
- `ITransport` → HTTP, STDIO transports

### 4. Async-First for I/O
Methods ending in `Async()` return `BoxFuture`, use `asyncRun()` for non-blocking:
```javascript
// Non-blocking document loading
var future = loader.loadAsync();
var docs = future.get();  // Wait for completion

// Non-blocking memory seeding
memory.seedAsync( documents ).then( result => {
    systemOutput( "Seeded #result.count# documents" );
});

// Custom async operations
var future = asyncRun( () => {
    return expensiveOperation();
}, "io-tasks" );  // Use io-tasks executor
```

### 5. Event-Driven Extensibility
40 interception points for complete customization:
```javascript
// Track all AI API costs
BoxRegisterInterceptor( "onAITokenCount", function( event ) {
    var cost = calculateCost( event.totalTokens, event.model );
    logCost( event.tenantId, event.operation, cost );
});

// Custom provider registration
BoxRegisterInterceptor( "onMissingAiProvider", function( event ) {
    if ( event.providerName == "custom" ) {
        return new CustomService();
    }
});
```

### 6. Multi-Tenant by Design
Built-in isolation and tracking:
```javascript
// Memory isolation
var memory = aiMemory( "cache", {
    userId: "user-123",
    conversationId: "conv-456"
});

// Usage tracking
var response = aiChat( "Hello", {
    tenantId: "acme-corp",
    usageMetadata: { department: "sales", costCenter: "CC-100" }
});
// onAITokenCount event includes tenantId + usageMetadata
```

### 7. Provider Abstraction
OpenAI-compatible standard in BaseService, override only what differs:
```javascript
// Most providers just configure URLs and params
class OllamaService extends="BaseService" {
    function configure( config ) {
        variables.chatURL = "http://localhost:11434/api/chat";
        // BaseService handles everything else!
        return this;
    }

    // Only override if provider differs from OpenAI standard
    function chatStream( chatRequest, callback ) {
        // Custom SSE handling for Ollama format
    }
}
```

### 8. Structured Output via Schemas
Pass class instance, struct, or array to `returnFormat` for typed responses:
```javascript
class Product {
    property name="title" type="string";
    property name="price" type="numeric";
    property name="inStock" type="boolean";
}

// AI returns populated object, not raw text!
var product = aiChat(
    "Extract product: Wireless Mouse - $29.99 - In Stock",
    returnFormat: new Product()
);

systemOutput( product.title );   // "Wireless Mouse"
systemOutput( product.price );   // 29.99 (numeric!)
systemOutput( product.inStock ); // true (boolean!)
```

## File Organization Logic

```
src/main/bx/
├── ModuleConfig.bx          # Module descriptor, settings, 40 interceptor points
├── bifs/                    # 18 Global BIFs (aiChat, aiMessage, aiService, aiTool, etc.)
├── models/
│   ├── AiAgent.bx          # Agent system (630 lines) - multi-memory, tools, sub-agents
│   ├── AiMessage.bx        # Fluent message builder with OnMissingMethod for roles
│   ├── AiModel.bx          # AI provider runnable wrapper
│   ├── AiRequest.bx        # Request object with validation/merging logic
│   ├── Document.bx         # Document model (422 lines) - chunking, tokens, validation
│   ├── Tool.bx             # Real-time function calling (implements ITool)
│   ├── providers/
│   │   ├── IAiService.bx   # Service interface (configure, invoke, getName)
│   │   ├── BaseService.bx  # OpenAI-compatible base (986 lines!) - HTTP, streaming, tools
│   │   └── *Service.bx     # 18 provider implementations (OpenAI, Claude, Gemini, Ollama, etc.)
│   ├── memory/
│   │   ├── IAiMemory.bx    # Memory interface (297 lines)
│   │   ├── BaseMemory.bx   # Base implementation (682 lines)
│   │   ├── *Memory.bx      # 7 memory types (Cache, File, Session, Window, Summary, JDBC, Hybrid)
│   │   └── vector/
│   │       ├── IVectorMemory.bx      # Vector memory interface
│   │       ├── BaseVectorMemory.bx   # Base vector (808 lines)
│   │       └── *VectorMemory.bx      # 11 vector providers (Box, Chroma, Pinecone, etc.)
│   ├── loaders/
│   │   ├── IDocumentLoader.bx       # Loader interface (244 lines) - functional API
│   │   ├── BaseDocumentLoader.bx    # Base loader (767 lines)
│   │   └── *Loader.bx               # 14 loader types (Text, CSV, JSON, PDF, HTTP, etc.)
│   ├── mcp/
│   │   ├── MCPClient.bx            # MCP client (449 lines) - fluent API
│   │   ├── MCPServer.bx            # MCP server (1542 lines!) - JSON-RPC 2.0
│   │   ├── MCPResponse.bx          # Response wrapper
│   │   ├── MCPRequestProcessor.bx  # Request normalization
│   │   └── transports/
│   │       ├── ITransport.bx       # Transport interface
│   │       ├── HTTPTransport.bx    # HTTP/REST transport
│   │       └── StdioTransport.bx   # CLI/process transport
│   ├── runnables/
│   │   ├── IAiRunnable.bx          # Pipeline interface (run, stream, to)
│   │   ├── AiBaseRunnable.bx       # Base implementation
│   │   ├── AiRunnableSequence.bx   # Pipeline chaining
│   │   └── AiTransformRunnable.bx  # Transformation wrapper
│   ├── transformers/
│   │   ├── ITransformer.bx         # Transformer interface
│   │   ├── BaseTransformer.bx      # Base implementation
│   │   └── *Transformer.bx         # 4 types (JSON, XML, Code, TextCleaner)
│   └── util/
│       ├── TextChunker.bx          # Static chunking (423 lines) - 5 strategies
│       ├── TokenCounter.bx         # Static token counting (100 lines)
│       ├── SchemaBuilder.bx        # JSON schema generation (554 lines)
│       └── AwsSignatureV4.bx       # AWS signing (318 lines) - for Bedrock

build/module/                # Compiled module (shadowJar output) - tests load from here!
```

## Critical Implementation Details 🚨

### Must-Know Gotchas

**🚨 Critical (Breaking if ignored):**

1. **Imports placement**: ALWAYS define imports at top of class definition, NEVER inline in methods
   ```javascript
   // ✅ Correct - at top of class
   import bxModules.bxai.models.util.TextChunker;

   class {
       function myMethod() {
           var chunks = TextChunker::chunk( text );  // Works!
       }
   }

   // ❌ Wrong - inline import (only works in .bxs/.bxm scripts)
   class {
       function myMethod() {
           import bxModules.bxai.models.util.TextChunker;  // FAILS!
       }
   }
   ```

2. **Static variable access**: Use `static.VAR` not `variables.VAR`
   ```javascript
   static {
       DEFAULT_MODEL = "gpt-4";
   }

   function getModel() {
       return static.DEFAULT_MODEL;  // ✅ Correct
       // return variables.DEFAULT_MODEL;  // ❌ Wrong - undefined!
   }
   ```

3. **Module loading for tests**: Run `./gradlew shadowJar` before testing - tests load from `build/module/`
   - Changes to `src/main/bx/` are NOT visible until compiled

4. **Only ONE system message** allowed per AI request (provider limitation)
   ```javascript
   // ❌ Wrong - multiple system messages
   messages: [
       { role: "system", content: "You are helpful" },
       { role: "system", content: "Be concise" }  // ERROR!
   ]

   // ✅ Correct - combine into one
   messages: [
       { role: "system", content: "You are helpful. Be concise." }
   ]
   ```

**⚠️ Important (Avoid bugs):**

5. **Property getters/setters**: Auto-generated - DON'T create manually
   ```javascript
   class User {
       property name="firstName" type="string";

       // ❌ Wrong - don't define getters/setters for properties
       // function getFirstName() { return variables.firstName; }
   }

   var user = new User();
   user.setFirstName( "John" );  // ✅ Auto-generated setter
   systemOutput( user.getFirstName() );  // ✅ Auto-generated getter
   ```

6. **Type casting**: Use `castAs` operator, NOT `javaCast()` function
   ```javascript
   // ✅ Correct
   var floatValue = config.temperature castAs "float";
   var floatValue = config.temperature castAs float;

   // ❌ Deprecated
   var floatValue = javaCast( "float", config.temperature );
   ```

7. **Reserved scope names**: Avoid as variable names - `server`, `request`, `session`, `application`, `cgi`, `url`, `form`, `cookie`, `variables`
   ```javascript
   // ❌ Wrong - conflicts with BoxLang scope
   var server = MCPServer( "my-server" );

   // ✅ Correct - use alternative name
   var mcpSrv = MCPServer( "my-server" );
   ```

8. **Event announcements**: Use `BoxAnnounce()` (capital B, capital A)
   ```javascript
   // ✅ Correct
   BoxAnnounce( "onAITokenCount", eventData );

   // ❌ Wrong - not a valid BIF
   announce( "onAITokenCount", eventData );
   ```

9. **Ollama model names**: Must include version tags
   ```javascript
   // ✅ Correct
   params: { model: "qwen2.5:0.5b-instruct" }

   // ❌ Wrong - missing version tag
   params: { model: "qwen2.5" }  // ERROR: model not found
   ```

10. **Tool argument descriptions**: Default to parameter name if missing (lenient validation)
    ```javascript
    // Both work - description optional
    { name: "query", type: "string", description: "Search query", required: true }
    { name: "query", type: "string", required: true }  // Uses "query" as description
    ```

**💡 Performance Tips:**

11. **API key detection**: BIFs auto-detect `<PROVIDER>_API_KEY` env vars
    ```javascript
    // Explicit
    aiService( "openai", "sk-..." )

    // Auto-detected from OPENAI_API_KEY env var
    aiService( "openai" )
    ```

12. **Streaming format**: Each provider has unique SSE chunk structure - handle nulls gracefully
    ```javascript
    var delta = jsonData.choices[1].delta.content ?: "";
    if ( !isNull( delta ) && delta != "" ) {
        callback( delta );
    }
    ```

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

## Documentation Standards

### Code Block Syntax

- Use javascript for BoxLang code examples (not java)
- Only use java for actual Java code
- This provides better syntax highlighting for BoxLang's CFML-like syntax, until BoxLang is natively supported

### Writing Style

- **Use emojis when appropriate** - They improve readability and visual scanning
  - Don't overboard - typically 1-2 per section/heading where helpful
  - Examples: ✅ Good, ❌ Bad, 🚨 Warning, 📖 Documentation, 💡 Tip
- **Keep code samples simple** - Focus on the concept being demonstrated
  - Avoid complex, multi-layered examples unless necessary
  - Use clear variable names
  - Comment only when the code isn't self-explanatory

### Interceptor Registration

- **Module registration**: Use `ModuleConfig.bx` approach with `interceptors` array
  - This is ONLY for BoxLang modules
  - Document as "For BoxLang Module registration"
- **Non-module registration**: Use `BoxRegisterInterceptor()` BIF
  - Reference: https://boxlang.ortusbooks.com/boxlang-language/reference/built-in-functions/system/boxregisterinterceptor
  - Document as "For application/script registration"

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
