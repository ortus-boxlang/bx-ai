# MCP Server - Model Context Protocol Server

The BoxLang AI Module provides a complete MCP (Model Context Protocol) server implementation that allows you to expose tools, resources, and prompts to AI clients.

## What is an MCP Server?

An MCP Server is a service that exposes capabilities to AI clients using the standardized Model Context Protocol. It enables:

- 🔧 **Expose Tools**: Register functions that AI clients can invoke
- 📚 **Serve Resources**: Provide documents and data to AI clients
- 💬 **Offer Prompts**: Define reusable prompt templates
- 🌐 **HTTP Endpoint**: Expose your MCP server via a web endpoint

## Quick Start

### 1. Register Tools

Register tools at application startup (e.g., in `Application.bx`):

```java
// Application.bx
class {

    function onApplicationStart() {
        // Get or create an MCP server instance
        mcpServer( "myApp" )
            .setDescription( "My Application MCP Server" )
            .setVersion( "1.0.0" )
            .registerTool(
                aiTool( "search", "Search for documents", ( query ) => {
                    return searchService.search( query )
                } )
            )
            .registerTool(
                aiTool( "calculate", "Perform calculations", ( expression ) => {
                    return evaluate( expression )
                } )
            )
    }

}
```

### 2. Access the MCP Endpoint

The module provides a built-in endpoint at `public/mcp.bxm`:

```
POST http://localhost/~bxai/mcp.bxm
```

Specify a server using either a query parameter or URL segment:

```
# Using query parameter
POST http://localhost/~bxai/mcp.bxm?server=myApp

# Using URL segment
POST http://localhost/~bxai/mcp.bxm/myApp
```

## Server Configuration

### Creating a Server

```java
// Get or create a server instance (singleton by name)
server = mcpServer( "myApp" )

// Multiple servers for different purposes
apiServer = mcpServer( "api" )
adminServer = mcpServer( "admin" )
```

### Configure Description and Version

```java
server = mcpServer( "myApp" )
    .setDescription( "My Application MCP Server" )
    .setVersion( "2.0.0" )
```

### Get Server Info

```java
info = server.getServerInfo()
// { name: "myApp", version: "2.0.0" }
```

## Tool Registration

### Register a Single Tool

```java
server = mcpServer( "myApp" )
    .registerTool(
        aiTool( "getWeather", "Get current weather for a location", ( location ) => {
            return weatherService.getCurrent( location )
        } )
        .describeArg( "location", "City name or coordinates" )
    )
```

### Register Multiple Tools

```java
tools = [
    aiTool( "search", "Search documents", searchHandler ),
    aiTool( "translate", "Translate text", translateHandler ),
    aiTool( "summarize", "Summarize text", summarizeHandler )
]

server = mcpServer( "myApp" )
    .registerTools( tools )
```

### Check and Retrieve Tools

```java
// Check if a tool exists
exists = server.hasTool( "search" )

// Get a specific tool
tool = server.getTool( "search" )

// Get tool count
count = server.getToolCount()

// Get all tools
tools = server.getTools()
```

### List Tools (MCP Format)

```java
// Returns array formatted for MCP protocol
toolsList = server.listTools()
// [
//     {
//         name: "search",
//         description: "Search documents",
//         inputSchema: {
//             type: "object",
//             properties: { ... },
//             required: [ ... ]
//         }
//     }
// ]
```

### Unregister Tools

```java
// Remove a specific tool
server.unregisterTool( "oldTool" )

// Remove all tools
server.clearTools()
```

## Annotation-Based Discovery

The MCP server can automatically discover and register tools, resources, and prompts from annotated methods using the `scan()` method.

### Scan for Annotations

```java
// Scan a class file
mcpServer( "myApp" ).scan( "/path/to/MyTools.bx" )

// Scan a directory (recursively scans all .bx files)
mcpServer( "myApp" ).scan( "/path/to/tools/" )
```

### @mcpTool Annotation

Register methods as MCP tools:

```java
class {

    /**
     * Search for documents
     * @query The search query
     */
    @mcpTool
    function search( required string query ) {
        return searchService.find( query )
    }

    /**
     * @query The query to search
     */
    @mcpTool( "Search for documents in the knowledge base" )
    function searchDocs( required string query ) {
        return docService.search( query )
    }

    @mcpTool( { name: "calculator", description: "Perform calculations", version: "2.0.0" } )
    function calculate( required string expression ) {
        return evaluate( expression )
    }

}
```

Annotation formats:
- `@mcpTool` - Name from method name, description from function hint, version defaults to 1.0.0
- `@mcpTool( "Description" )` - Name from method name, custom description
- `@mcpTool( { name: "...", description: "...", version: "..." } )` - All custom values

### @mcpResource Annotation

Register methods as MCP resources:

```java
class {

    /**
     * Returns the README file
     */
    @mcpResource
    function readme() {
        return fileRead( "/readme.md" )
    }

    @mcpResource( "API documentation for developers" )
    function apiDocs() {
        return generateApiDocs()
    }

    @mcpResource( { uri: "config://app", name: "App Config", description: "Application settings", mimeType: "application/json" } )
    function getConfig() {
        return application.settings
    }

}
```

Annotation formats:
- `@mcpResource` - URI and name from method name, description from function hint
- `@mcpResource( "Description" )` - URI and name from method name, custom description
- `@mcpResource( { uri: "...", name: "...", description: "...", mimeType: "..." } )` - All custom values

### @mcpPrompt Annotation

Register methods as MCP prompts:

```java
class {

    /**
     * Generate a greeting message
     * @name The person's name
     */
    @mcpPrompt
    function greeting( required string name ) {
        return [
            { role: "system", content: "You are a friendly assistant." },
            { role: "user", content: "Say hello to #name#" }
        ]
    }

    @mcpPrompt( "Generate code based on a description" )
    function codeGen( required string description, string language = "java" ) {
        return [
            { role: "system", content: "You are a code generator for #language#." },
            { role: "user", content: description }
        ]
    }

    @mcpPrompt( { name: "reviewer", description: "Code review prompt", arguments: [ { name: "code", required: true } ] } )
    function reviewCode( required string code ) {
        return [
            { role: "system", content: "Review this code for issues." },
            { role: "user", content: code }
        ]
    }

}
```

Annotation formats:
- `@mcpPrompt` - Name from method name, description from function hint
- `@mcpPrompt( "Description" )` - Name from method name, custom description
- `@mcpPrompt( { name: "...", description: "...", arguments: [...] } )` - All custom values

## Resource Registration

Resources provide access to documents and data:

### Register a Resource

```java
server = mcpServer( "myApp" )
    .registerResource(
        uri: "docs://readme",
        name: "README",
        description: "Project documentation",
        mimeType: "text/markdown",
        handler: () => {
            return fileRead( expandPath( "/readme.md" ) )
        }
    )
```

### Register Dynamic Resources

```java
// Database content
server.registerResource(
    uri: "db://users",
    name: "User List",
    description: "Current user data",
    mimeType: "application/json",
    handler: () => {
        return userService.getAllUsers()
    }
)

// Configuration
server.registerResource(
    uri: "config://app",
    name: "App Configuration",
    description: "Application settings",
    mimeType: "application/json",
    handler: () => {
        return application.settings
    }
)
```

### List and Read Resources

```java
// List available resources
resources = server.listResources()

// Check if resource exists
exists = server.hasResource( "docs://readme" )

// Read a resource
content = server.readResource( "docs://readme" )
// {
//     contents: [
//         {
//             uri: "docs://readme",
//             mimeType: "text/markdown",
//             text: "# Readme content..."
//         }
//     ]
// }
```

### Manage Resources

```java
// Remove a resource
server.unregisterResource( "docs://readme" )

// Clear all resources
server.clearResources()
```

## Prompt Registration

Prompts provide reusable prompt templates:

### Register a Prompt

```java
server = mcpServer( "myApp" )
    .registerPrompt(
        name: "codeReview",
        description: "Code review prompt template",
        args: [
            { name: "language", description: "Programming language", required: true },
            { name: "code", description: "Code to review", required: true }
        ],
        handler: ( args ) => {
            return [
                {
                    role: "system",
                    content: "You are a code reviewer specializing in #args.language#."
                },
                {
                    role: "user",
                    content: "Please review this code:\n\n```#args.language#\n#args.code#\n```"
                }
            ]
        }
    )
```

### List and Get Prompts

```java
// List available prompts
prompts = server.listPrompts()

// Check if prompt exists
exists = server.hasPrompt( "codeReview" )

// Get a prompt with arguments
result = server.getPrompt( "codeReview", {
    language: "java",
    code: "public void test() {}"
} )
// {
//     description: "Code review prompt template",
//     messages: [
//         { role: "system", content: { type: "text", text: "..." } },
//         { role: "user", content: { type: "text", text: "..." } }
//     ]
// }
```

## Handling MCP Requests

### Direct Request Handling

```java
// Handle a JSON-RPC request
request = {
    "jsonrpc": "2.0",
    "method": "tools/list",
    "id": "1"
}

response = server.handleRequest( request )
// {
//     jsonrpc: "2.0",
//     result: { tools: [ ... ] },
//     id: "1"
// }
```

### Handle JSON String

```java
jsonRequest = '{"jsonrpc":"2.0","method":"tools/call","id":"1","params":{"name":"search","arguments":{"query":"test"}}}'

response = server.handleRequest( jsonRequest )
```

### Supported Methods

| Method | Description |
|--------|-------------|
| `initialize` | Get server capabilities and info |
| `tools/list` | List available tools |
| `tools/call` | Invoke a tool |
| `resources/list` | List available resources |
| `resources/read` | Read a resource |
| `prompts/list` | List available prompts |
| `prompts/get` | Get a prompt with arguments |
| `ping` | Health check |

## HTTP Endpoint (mcp.bxm)

The module includes a pre-built HTTP endpoint at `public/mcp.bxm`:

### Discovery (GET)

```bash
curl http://localhost/~bxai/mcp.bxm
```

Returns server capabilities:

```json
{
    "jsonrpc": "2.0",
    "result": {
        "protocolVersion": "2024-11-05",
        "capabilities": {
            "tools": {},
            "resources": {},
            "prompts": {}
        },
        "serverInfo": {
            "name": "default",
            "version": "1.0.0"
        }
    }
}
```

### JSON-RPC Requests (POST)

```bash
curl -X POST http://localhost/~bxai/mcp.bxm \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"tools/list","id":"1"}'
```

### Multiple Servers

Specify the server using either a query parameter or URL segment:

```bash
# Using query parameter
curl http://localhost/~bxai/mcp.bxm?server=api
curl http://localhost/~bxai/mcp.bxm?server=admin

# Using URL segment
curl http://localhost/~bxai/mcp.bxm/api
curl http://localhost/~bxai/mcp.bxm/admin
```

### CORS Support

The endpoint includes CORS headers for cross-origin requests.

## Static Server Management

### Check Server Existence

```java
// Check if a server exists
exists = bxModules.bxai.models.mcp.MCPServer::hasInstance( "myApp" )
```

### Get All Server Names

```java
names = bxModules.bxai.models.mcp.MCPServer::getInstanceNames()
// [ "default", "api", "admin" ]
```

### Remove a Server

```java
wasRemoved = bxModules.bxai.models.mcp.MCPServer::removeInstance( "oldApp" )
```

### Clear All Servers

```java
bxModules.bxai.models.mcp.MCPServer::clearAllInstances()
```

## Complete Example

### Application Setup

```java
// Application.bx
class {

    function onApplicationStart() {
        // Create the MCP server
        var server = mcpServer( "myApp" )
            .setDescription( "My Application API" )
            .setVersion( "1.0.0" )

        // Register tools
        server.registerTool(
            aiTool( "searchProducts", "Search product catalog", ( query, category ) => {
                return productService.search(
                    query: arguments.query,
                    category: arguments.category ?: ""
                )
            } )
            .describeArg( "query", "Search keywords" )
            .describeArg( "category", "Optional category filter" )
        )

        server.registerTool(
            aiTool( "getOrderStatus", "Get order status", ( orderId ) => {
                return orderService.getStatus( arguments.orderId )
            } )
            .describeArg( "orderId", "The order ID to check" )
        )

        // Register resources
        server.registerResource(
            uri: "catalog://products",
            name: "Product Catalog",
            description: "Full product listing",
            mimeType: "application/json",
            handler: () => productService.getAll()
        )

        server.registerResource(
            uri: "docs://api",
            name: "API Documentation",
            description: "API reference documentation",
            mimeType: "text/markdown",
            handler: () => fileRead( expandPath( "/docs/api.md" ) )
        )

        // Register prompts
        server.registerPrompt(
            name: "productRecommendation",
            description: "Get product recommendations",
            args: [
                { name: "preferences", description: "User preferences", required: true }
            ],
            handler: ( args ) => [
                {
                    role: "system",
                    content: "You are a product recommendation assistant."
                },
                {
                    role: "user",
                    content: "Based on these preferences, suggest products: #args.preferences#"
                }
            ]
        )

        return true
    }

    function onApplicationEnd() {
        // Clean up
        bxModules.bxai.models.mcp.MCPServer::removeInstance( "myApp" )
    }

}
```

### Connecting from AI Client

```java
// Use the MCP client to connect to the server
client = MCP( "http://localhost/~bxai/mcp.bxm?server=myApp" )

// List available tools
tools = client.listTools()

// Invoke a tool
result = client.send( "searchProducts", {
    query: "laptop",
    category: "electronics"
} )

if ( result.getSuccess() ) {
    products = result.getData()
    writeOutput( "Found #arrayLen( products )# products" )
}
```

## Best Practices

### 1. Use Descriptive Names

```java
// Good
aiTool( "searchProducts", "Search the product catalog", handler )

// Bad
aiTool( "sp", "search", handler )
```

### 2. Register at Application Start

```java
// Application.bx onApplicationStart
// Ensures tools are available for all requests
```

### 3. Clean Up on Shutdown

```java
// Application.bx onApplicationEnd
bxModules.bxai.models.mcp.MCPServer::removeInstance( "myApp" )
```

### 4. Use Separate Servers for Different Purposes

```java
// API server for external clients
mcpServer( "api" )
    .registerTool( publicTool1 )
    .registerTool( publicTool2 )

// Admin server for internal tools
mcpServer( "admin" )
    .registerTool( adminTool1 )
    .registerTool( adminTool2 )
```

### 5. Document Your Tools

```java
aiTool( "calculateShipping", "Calculate shipping cost based on weight and destination", handler )
    .describeArg( "weight", "Package weight in pounds" )
    .describeArg( "destination", "Destination zip code" )
    .describeArg( "expedited", "Whether to use expedited shipping (true/false)" )
```

## Related Documentation

- [MCP Client](./mcp-client.md) - Consuming MCP servers
- [AI Tools](../main-components/tools.md) - Creating tools
- [AI Agents](../main-components/agents.md) - Using agents with tools

## External Resources

- [Model Context Protocol Specification](https://modelcontextprotocol.io)
- [MCP Implementation Examples](https://github.com/modelcontextprotocol)
