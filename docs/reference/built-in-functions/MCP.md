# MCP

Create a fluent MCP (Model Context Protocol) client for consuming external MCP servers. MCP enables AI applications to connect to tools, data sources, and external systems through a standardized protocol.

## 🔌 MCP Client-Server Flow

```mermaid
sequenceDiagram
    participant App as Your Application
    participant MCP as MCP Client
    participant Server as MCP Server
    participant Res as External Resources
    
    App->>MCP: MCP(baseURL)
    App->>MCP: withTimeout/withAuth
    
    alt Discovery Phase
        App->>MCP: listTools()
        MCP->>Server: GET /tools
        Server-->>MCP: Available tools
        MCP-->>App: Tool definitions
    end
    
    alt Invocation Phase
        App->>MCP: callTool(name, args)
        MCP->>Server: POST /call
        Server->>Res: Access data/API
        Res-->>Server: Resource data
        Server-->>MCP: Tool result
        MCP-->>App: Response data
    end
    
    alt Resource Access
        App->>MCP: getResource(uri)
        MCP->>Server: GET /resource
        Server->>Res: Fetch resource
        Res-->>Server: Resource content
        Server-->>MCP: Resource data
        MCP-->>App: Resource
    end
    
    style MCP fill:#4A90E2
    style Server fill:#7ED321
    style Res fill:#F5A623
```

## Syntax

```javascript
MCP(baseURL)
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `baseURL` | string | Yes | The base URL of the MCP server to connect to |

## Returns

Returns an `MCPClient` instance with fluent API for:
- Configuration: `withTimeout()`, `withBearerToken()`, `withHeaders()`
- Callbacks: `onSuccess()`, `onError()`
- Discovery: `listTools()`, `listResources()`, `listPrompts()`
- Invocation: `send()`, `callTool()`, `getResource()`, `getPrompt()`

## Examples

### Basic MCP Client

```javascript
// Connect to MCP server
client = MCP( "http://localhost:3000" );

// Call a tool
result = client.send( "searchDocs", { query: "syntax" } );
println( result.getData() );
```

### With Configuration

```javascript
// Configure client with options
client = MCP( "http://localhost:3000" )
    .withTimeout( 5000 )
    .withBearerToken( "my-secret-token" )
    .withHeaders({
        "X-Client-ID": "boxlang-app",
        "X-Request-ID": createUUID()
    });

result = client.send( "getTasks", {} );
```

### Callback Handlers

```javascript
// Add success and error handlers
client = MCP( "http://localhost:3000" )
    .onSuccess( ( response ) => {
        writeLog( "Success: #response.getData()#" );
    })
    .onError( ( response ) => {
        writeLog( "Error: #response.getError()#", "error" );
    });

client.send( "processData", { input: "test" } );
```

### Discover Tools

```javascript
// List available tools from server
client = MCP( "http://localhost:3000" );

tools = client.listTools();
println( "Available Tools:" );
tools.each( tool => {
    println( "  - #tool.name#: #tool.description#" );
});
```

### Call Specific Tool

```javascript
// Call a tool by name
client = MCP( "http://localhost:3000" );

result = client.callTool( "search", {
    query: "BoxLang documentation",
    limit: 10
});

println( result.getData() );
```

### Discover Resources

```javascript
// List available resources
client = MCP( "http://localhost:3000" );

resources = client.listResources();
println( "Available Resources:" );
resources.each( resource => {
    println( "  - #resource.uri#: #resource.description#" );
});
```

### Get Resource

```javascript
// Fetch a specific resource
client = MCP( "http://localhost:3000" );

resource = client.getResource( "docs://readme" );
println( resource.getData() );
```

### Discover Prompts

```javascript
// List available prompts
client = MCP( "http://localhost:3000" );

prompts = client.listPrompts();
println( "Available Prompts:" );
prompts.each( prompt => {
    println( "  - #prompt.name#: #prompt.description#" );
});
```

### Get Prompt

```javascript
// Get a prompt template
client = MCP( "http://localhost:3000" );

prompt = client.getPrompt( "greeting", {
    name: "John",
    language: "Spanish"
});

messages = prompt.getData();
// Use messages with AI model
```

### Authentication

```javascript
// Bearer token authentication
client = MCP( "https://api.example.com/mcp" )
    .withBearerToken( getSystemSetting( "MCP_TOKEN" ) );

result = client.send( "secureOperation", { data: "sensitive" } );
```

### Custom Headers

```javascript
// Add custom headers for tracking
client = MCP( "http://localhost:3000" )
    .withHeaders({
        "X-User-ID": session.userID,
        "X-Request-ID": createUUID(),
        "X-App-Version": "1.0.0"
    });

result = client.send( "loggedOperation", {} );
```

### Error Handling

```javascript
// Handle errors explicitly
client = MCP( "http://localhost:3000" );

try {
    result = client.send( "riskyOperation", { input: data } );

    if ( result.isError() ) {
        println( "MCP Error: #result.getError()#" );
    } else {
        println( "Success: #result.getData()#" );
    }
} catch ( any e ) {
    println( "Connection Error: #e.message#" );
}
```

### Timeout Configuration

```javascript
// Set custom timeout for long operations
client = MCP( "http://localhost:3000" )
    .withTimeout( 30000 ); // 30 seconds

result = client.send( "longRunningTask", { input: largeDataset } );
```

### Integration with AI Agent

```javascript
// Use MCP tools with AI agent
client = MCP( "http://localhost:3000" );

// Discover available tools
mcpTools = client.listTools();

// Create AI tools from MCP tools
aiTools = mcpTools.map( mcpTool => {
    return aiTool(
        name: mcpTool.name,
        description: mcpTool.description,
        handler: ( args ) => {
            result = client.callTool( mcpTool.name, args );
            return result.getData();
        }
    );
});

// Use with agent
agent = aiAgent( tools: aiTools );
response = agent.run( "Search for BoxLang docs" );
```

### Multiple MCP Servers

```javascript
// Connect to multiple servers
docsServer = MCP( "http://docs.example.com:3000" );
apiServer = MCP( "http://api.example.com:3001" );

// Use different servers
docs = docsServer.callTool( "search", { query: "API" } );
data = apiServer.callTool( "getData", { id: 123 } );
```

### Health Check

```javascript
// Check if MCP server is available
function checkMCPHealth( baseURL ) {
    try {
        client = MCP( baseURL ).withTimeout( 2000 );
        tools = client.listTools();
        return true;
    } catch ( any e ) {
        return false;
    }
}

if ( checkMCPHealth( "http://localhost:3000" ) ) {
    println( "MCP server is available" );
} else {
    println( "MCP server is unavailable" );
}
```

### Logging Wrapper

```javascript
// Wrap client with logging
function createLoggingMCPClient( baseURL ) {
    return MCP( baseURL )
        .onSuccess( ( response ) => {
            writeLog(
                "MCP Success: #serializeJSON(response.getData())#",
                "information"
            );
        })
        .onError( ( response ) => {
            writeLog(
                "MCP Error: #response.getError()#",
                "error"
            );
        });
}

client = createLoggingMCPClient( "http://localhost:3000" );
```

### Retry Logic

```javascript
// Implement retry logic for MCP calls
function mcpCallWithRetry( client, toolName, args, maxRetries = 3 ) {
    for ( var i = 1; i <= maxRetries; i++ ) {
        try {
            result = client.callTool( toolName, args );

            if ( !result.isError() ) {
                return result.getData();
            }

            if ( i < maxRetries ) {
                sleep( 1000 * i ); // Exponential backoff
            }
        } catch ( any e ) {
            if ( i == maxRetries ) {
                throw( e );
            }
        }
    }

    throw( "MCP call failed after #maxRetries# attempts" );
}
```

### Dynamic Tool Discovery

```javascript
// Dynamically discover and use tools
client = MCP( "http://localhost:3000" );

// Find tool by name pattern
tools = client.listTools();
searchTool = tools.find( t => t.name.findNoCase( "search" ) );

if ( !isNull( searchTool ) ) {
    result = client.callTool( searchTool.name, { query: "BoxLang" } );
    println( result.getData() );
}
```

### Caching MCP Responses

```javascript
// Cache MCP tool responses
function callMCPWithCache( client, toolName, args, ttl = 60 ) {
    cacheKey = "mcp_#toolName#_#hash(serializeJSON(args))#";

    if ( cacheExists( cacheKey ) ) {
        return cacheGet( cacheKey );
    }

    result = client.callTool( toolName, args );
    data = result.getData();

    cachePut( cacheKey, data, ttl );
    return data;
}

client = MCP( "http://localhost:3000" );
data = callMCPWithCache( client, "expensiveTool", { id: 123 }, 300 );
```

## Notes

- 🔌 **Protocol**: Implements Model Context Protocol standard for AI integrations
- 🌐 **HTTP-Based**: Works with any MCP server over HTTP/HTTPS
- 🔧 **Fluent API**: Chainable configuration methods
- 🎯 **Discovery**: Dynamically discover tools, resources, and prompts
- 🔐 **Security**: Supports bearer tokens and custom headers
- ⚡ **Configurable**: Timeout and header customization
- 📡 **Callbacks**: Success and error handlers for async workflows

## Related Functions

- [`MCPServer()`](MCPServer.md) - Create MCP server to expose tools
- [`aiTool()`](aiTool.md) - Create AI tools (can wrap MCP tools)
- [`aiAgent()`](aiAgent.md) - Use MCP tools with agents

## Best Practices

✅ **Discover before using** - List tools/resources to understand capabilities

✅ **Handle errors** - Check response status and handle errors gracefully

✅ **Use timeouts** - Set appropriate timeouts for network operations

✅ **Secure connections** - Use HTTPS and bearer tokens for production

✅ **Cache responses** - Cache expensive MCP calls when appropriate

✅ **Retry on failure** - Implement retry logic for transient errors

❌ **Don't hardcode URLs** - Use environment variables for MCP server URLs

❌ **Don't ignore errors** - Always check if response is error before using data

❌ **Don't skip authentication** - Use tokens for protected MCP servers
