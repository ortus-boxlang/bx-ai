# MCPServer

Get or create an MCP (Model Context Protocol) server instance for registering and managing tools, resources, and prompts. MCP servers expose your application's capabilities to AI clients through a standardized protocol.

## Syntax

```javascript
MCPServer(name, description, version, cors, statsEnabled, force)
```

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `name` | string | No | `"default"` | Unique name/identifier for the server instance |
| `description` | string | No | `"BoxLang AI MCP Server"` | Server description for MCP capabilities response |
| `version` | string | No | `"1.0.0"` | Server version |
| `cors` | string | No | `""` | CORS allowed origin (empty = no CORS header, secure by default) |
| `statsEnabled` | boolean | No | `true` | Enable statistics tracking |
| `force` | boolean | No | `false` | Force rebuild of server instance even if exists |

## Returns

Returns an `MCPServer` instance with fluent API for:
- Tool management: `registerTool()`, `getTool()`, `removeTool()`, `listTools()`
- Resource management: `registerResource()`, `getResource()`, `removeResource()`, `listResources()`
- Prompt management: `registerPrompt()`, `getPrompt()`, `removePrompt()`, `listPrompts()`
- Request handling: `handleRequest()`, `handleToolCall()`, `handleResourceGet()`, `handlePromptGet()`
- Configuration: `setDescription()`, `setVersion()`, `setCORS()`, `enableStats()`
- Statistics: `getStats()`, `getToolCount()`, `getResourceCount()`, `getPromptCount()`

## Examples

### Basic MCP Server

```javascript
// Create server and register tools
myServer = MCPServer( "myApp" )
    .setDescription( "My Application MCP Server" )
    .setVersion( "1.0.0" );

// Register a tool
myServer.registerTool(
    aiTool( "search", "Search for documents", ( query ) => {
        return searchDatabase( query );
    })
);
```

### Complete Server Setup

```javascript
// Application.bx - Register tools at startup
function onApplicationStart() {
    server = MCPServer( 
        name: "myApp",
        description: "My Application MCP Server",
        version: "1.0.0",
        cors: "https://myapp.com"
    );
    
    // Register multiple tools
    server.registerTool(
        aiTool( "search", "Search documents", ( query ) => search( query ) )
    );
    
    server.registerTool(
        aiTool( "getData", "Get user data", ( userId ) => getUserData( userId ) )
    );
    
    server.registerTool(
        aiTool( "calculate", "Do math", ( expr ) => evaluate( expr ) )
    );
}
```

### HTTP Endpoint

```javascript
// mcp-endpoint.bxm - Expose MCP over HTTP
server = MCPServer( "myApp" );

// Get request body
requestBody = getHttpRequestData().content;

// Process MCP request
response = server.handleRequest( requestBody );

// Return JSON response
cfheader( name: "Content-Type", value: "application/json" );
writeOutput( serializeJSON( response ) );
```

### Register Resource

```javascript
// Expose documentation as resource
server = MCPServer( "myApp" );

server.registerResource(
    uri: "docs://readme",
    name: "README",
    description: "Application documentation",
    mimeType: "text/markdown",
    handler: () => fileRead( "/docs/README.md" )
);

server.registerResource(
    uri: "docs://api",
    name: "API Reference",
    description: "API documentation",
    mimeType: "text/html",
    handler: () => generateAPIDocs()
);
```

### Register Prompt

```javascript
// Register prompt templates
server = MCPServer( "myApp" );

server.registerPrompt(
    name: "greeting",
    description: "Generate a personalized greeting",
    args: [
        { name: "name", description: "Person's name", required: true },
        { name: "language", description: "Language for greeting", required: false }
    ],
    handler: ( args ) => {
        var lang = args.language ?: 'English';
        return [
            {
                role: "user",
                content: "Generate a greeting for #args.name# in #lang#"
            }
        ];
    }
);
```

### Multiple Tools

```javascript
// Register collection of tools
server = MCPServer( "dataAPI" )
    .setDescription( "Data API MCP Server" );

// Database tools
server
    .registerTool( aiTool( "getUsers", "Get all users", () => db.getUsers() ) )
    .registerTool( aiTool( "getUser", "Get user by ID", ( id ) => db.getUser( id ) ) )
    .registerTool( aiTool( "createUser", "Create user", ( data ) => db.createUser( data ) ) )
    .registerTool( aiTool( "updateUser", "Update user", ( id, data ) => db.updateUser( id, data ) ) );
```

### CORS Configuration

```javascript
// Enable CORS for specific domain
publicServer = MCPServer( 
    name: "public-api",
    cors: "https://app.example.com"
);

// Or multiple domains (set in handler)
server = MCPServer( "myApp" );
// Handle CORS in your endpoint wrapper
```

### Statistics Tracking

```javascript
// Enable statistics
server = MCPServer( 
    name: "myApp",
    statsEnabled: true
);

// Register and use tools
server.registerTool(
    aiTool( "search", "Search", ( q ) => search( q ) )
);

// Later, check stats
stats = server.getStats();
println( "Total calls: #stats.totalCalls#" );
println( "Tool calls: #stats.toolCalls#" );
println( "Most used: #stats.mostUsedTool#" );
```

### Multi-Server Architecture

```javascript
// Public server
publicServer = MCPServer( "public-api" )
    .setDescription( "Public API Server" );

publicServer.registerTool(
    aiTool( "search", "Public search", ( query ) => publicSearch( query ) )
);

// Admin server
adminServer = MCPServer( "admin-api" )
    .setDescription( "Admin API Server" );

adminServer.registerTool(
    aiTool( "deleteData", "Delete data", ( id ) => adminDelete( id ) )
);

// Get all active servers
activeServers = bxModules.bxai.models.mcp.MCPServer::getInstanceNames();
println( "Active servers: #activeServers.toJSON()#" );
```

### Dynamic Tool Registration

```javascript
// Register tools based on user permissions
server = MCPServer( "userApp" );

if ( user.hasPermission( "read" ) ) {
    server.registerTool(
        aiTool( "getData", "Get data", () => getData() )
    );
}

if ( user.hasPermission( "write" ) ) {
    server.registerTool(
        aiTool( "saveData", "Save data", ( data ) => saveData( data ) )
    );
}

if ( user.hasPermission( "admin" ) ) {
    server.registerTool(
        aiTool( "deleteData", "Delete data", ( id ) => deleteData( id ) )
    );
}
```

### Inspect Server

```javascript
// Check server state
server = MCPServer( "myApp" );

println( "Tools: #server.getToolCount()#" );
println( "Resources: #server.getResourceCount()#" );
println( "Prompts: #server.getPromptCount()#" );

// List registered tools
tools = server.listTools();
tools.each( tool => {
    println( "  - #tool.name#" );
});
```

### Force Rebuild

```javascript
// Rebuild server (useful for development)
server = MCPServer( 
    name: "myApp",
    force: true  // Clears existing and creates new
);

// Re-register all tools
registerTools( server );
```

### Singleton Pattern

```javascript
// MCP servers are singletons by name
function getAppMCPServer() {
    // Returns same instance if already created
    return MCPServer( "myApp" );
}

// Called from different places, same instance
server1 = getAppMCPServer();
server2 = getAppMCPServer();

// server1 === server2 (same instance)
```

### Remove Tools/Resources

```javascript
// Manage tool lifecycle
server = MCPServer( "myApp" );

// Register
server.registerTool( 
    aiTool( "tempTool", "Temporary tool", () => "temp" ) 
);

// Later, remove
server.removeTool( "tempTool" );

// Check if exists
if ( !isNull( server.getTool( "tempTool" ) ) ) {
    println( "Tool still exists" );
}
```

### Error Handling

```javascript
// Handle MCP request errors
server = MCPServer( "myApp" );

server.registerTool(
    aiTool( "riskyOperation", "Risky op", ( data ) => {
        try {
            return processData( data );
        } catch ( any e ) {
            throw( 
                type: "MCPToolError",
                message: "Operation failed: #e.message#"
            );
        }
    })
);
```

### Integration with Agent

```javascript
// Use MCP server tools with local agent
server = MCPServer( "myApp" );

// Register tools
server.registerTool( aiTool( "getWeather", "Weather", ( city ) => getWeather( city ) ) );
server.registerTool( aiTool( "getTime", "Time", () => now() ) );

// Get registered tools for agent
mcpTools = server.listTools().map( toolDef => {
    return server.getTool( toolDef.name );
});

// Create agent with MCP tools
agent = aiAgent( tools: mcpTools );
response = agent.run( "What's the weather in Paris?" );
```

### Secure Endpoint

```javascript
// Secure MCP endpoint with authentication
server = MCPServer( "secureApp" );

// In endpoint (e.g., mcp.bxm)
function handleMCPRequest() {
    // Verify auth token
    token = getHttpRequestData().headers[ "Authorization" ];
    
    if ( !verifyToken( token ) ) {
        cfheader( statusCode: 401 );
        return { error: "Unauthorized" };
    }
    
    // Process MCP request
    requestBody = getHttpRequestData().content;
    return server.handleRequest( requestBody );
}
```

### Development vs Production

```javascript
// Different configs for environments
if ( application.environment == "development" ) {
    server = MCPServer( 
        name: "devApp",
        cors: "*",  // Allow all in dev
        statsEnabled: true
    );
} else {
    server = MCPServer(
        name: "prodApp",
        cors: "https://myapp.com",  // Strict CORS
        statsEnabled: false  // Disable stats in prod
    );
}
```

## Notes

- 🌐 **Singleton**: Servers are singleton by name - same instance returned for same name
- 🔧 **Global Storage**: Stored globally, accessible from anywhere (Application.bx → endpoint)
- 🎯 **Protocol**: Implements Model Context Protocol standard
- 🔐 **Security**: No CORS by default - explicitly set allowed origins
- 📊 **Statistics**: Track tool usage, call counts, and performance
- 🚀 **Performance**: Efficient request handling and tool dispatch
- 🔄 **Hot Reload**: Use `force: true` to rebuild during development

## Related Functions

- [`MCP()`](MCP.md) - Create MCP client to consume servers
- [`aiTool()`](aiTool.md) - Create tools to register with server
- [`aiAgent()`](aiAgent.md) - Use MCP tools with agents

## Best Practices

✅ **Register at startup** - Register tools in `Application.bx` `onApplicationStart()`

✅ **Use singleton pattern** - Access same server by name throughout app

✅ **Secure endpoints** - Validate authentication before handling requests

✅ **Set CORS carefully** - Explicitly whitelist allowed origins

✅ **Document tools** - Provide clear descriptions for AI clients

✅ **Handle errors** - Wrap tool handlers in try/catch for robustness

❌ **Don't create per request** - Servers are singletons, get by name

❌ **Don't allow all CORS** - Never use `*` in production

❌ **Don't skip authentication** - Always verify requests in public endpoints

❌ **Don't register duplicate names** - Tool/resource/prompt names must be unique
