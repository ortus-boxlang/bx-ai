# 🌐 MCP Server - Model Context Protocol Server

The BoxLang AI Module provides a complete MCP (Model Context Protocol) server implementation that allows you to expose tools, resources, and prompts to AI clients.

## 📋 Table of Contents

- [🎯 What is an MCP Server?](#-what-is-an-mcp-server)
- [Transport Types](#transport-types)
- [Quick Start](#quick-start)
- [Server Configuration](#server-configuration)
- [Tool Registration](#tool-registration)
- [Annotation-Based Discovery](#annotation-based-discovery)
- [Resource Registration](#resource-registration)
- [Prompt Registration](#prompt-registration)
- [Handling MCP Requests](#handling-mcp-requests)
- [HTTP Endpoint (mcp.bxm)](#http-endpoint-mcpbxm)
- [Static Server Management](#static-server-management)
- [Complete Example](#complete-example)
- [Events & Interception 🎯](#events--interception-)
- [Statistics & Monitoring 📊](#statistics--monitoring-)
- [Best Practices](#best-practices)
- [Related Documentation](#related-documentation)
- [External Resources](#external-resources)

## 🎯 What is an MCP Server?

An MCP Server is a service that exposes capabilities to AI clients using the standardized Model Context Protocol. It enables:

- 🔧 **Expose Tools**: Register functions that AI clients can invoke
- 📚 **Serve Resources**: Provide documents and data to AI clients
- 💬 **Offer Prompts**: Define reusable prompt templates
- 🌐 **HTTP & STDIO Transports**: Expose your MCP server via web or command-line

### 🏗️ MCP Architecture

```mermaid
graph TB
    subgraph "AI Clients"
        C1[Claude Desktop]
        C2[VS Code]
        C3[Web App]
        C4[Custom Client]
    end

    subgraph "Transport Layer"
        H[HTTP Transport]
        S[STDIO Transport]
    end

    subgraph "MCP Server"
        MS[MCP Server Core]
        TR[Tool Registry]
        RR[Resource Registry]
        PR[Prompt Registry]
    end

    subgraph "Your Application"
        T[Tools/Functions]
        D[Data Sources]
        P[Prompt Templates]
    end

    C1 --> S
    C2 --> S
    C3 --> H
    C4 --> H

    H --> MS
    S --> MS

    MS --> TR
    MS --> RR
    MS --> PR

    TR --> T
    RR --> D
    PR --> P

    style MS fill:#BD10E0
    style H fill:#4A90E2
    style S fill:#7ED321
    style TR fill:#B8E986
```

## 🚀 Transport Types

BoxLang AI provides two transport mechanisms for MCP servers:

### 🔄 Transport Flow Comparison

```mermaid
sequenceDiagram
    participant C as AI Client
    participant H as HTTP Transport
    participant S as STDIO Transport
    participant M as MCP Server
    participant T as Tool

    Note over C,T: HTTP Transport Flow
    C->>H: POST /mcp.bxm
    H->>M: Parse JSON-RPC
    M->>T: Execute tool
    T->>M: Return result
    M->>H: JSON response
    H->>C: HTTP 200 + JSON

    Note over C,T: STDIO Transport Flow
    C->>S: JSON-RPC via STDIN
    S->>M: Parse line
    M->>T: Execute tool
    T->>M: Return result
    M->>S: JSON response
    S->>C: Write to STDOUT

    Note over C,T: Both use same protocol,<br/>different transport layers
```

### 🌐 HTTP Transport (Web)

**Best for:** Web applications, REST APIs, browser-based clients

**Entry Point:** Automatic via convention or manual endpoint creation

**Automatic (Convention):**

```javascript
// Access the built-in endpoint
POST http://localhost/~bxai/mcp.bxm
POST http://localhost/~bxai/mcp.bxm?server=myApp
POST http://localhost/~bxai/mcp.bxm/myApp
```

**Manual (Custom Endpoint):**

```javascript
// api/my-mcp.bxm
<bx:script>
import bxModules.bxai.models.mcp.MCPRequestProcessor
MCPRequestProcessor::startHttp()
</bx:script>
```

**Features:**

- ✅ CORS support with wildcard patterns
- ✅ Body size limits
- ✅ API key authentication
- ✅ HTTP Basic Auth
- ✅ Security headers
- ✅ Server-Sent Events (SSE) streaming
- ✅ Discovery endpoint (GET)

### 🖥️ STDIO Transport (Command-Line)

**Best for:** Desktop applications, CLI tools, IDE integrations, local AI assistants

**Entry Point:** Create a script entry point

**Example Script** (`mcp-stdio.bxs`):

```javascript
import bxModules.bxai.models.mcp.MCPRequestProcessor

// Register your server and tools
MCPServer( "myApp" )
    .registerTool( aiTool( "echo", "Echo tool", ( msg ) => msg ) )

// Get server name from CLI args
serverName = getSystemSetting( "MCP_SERVER_NAME", "default" )
parsedArgs = cliGetArgs()

if ( structKeyExists( parsedArgs.options, "server" ) ) {
    serverName = parsedArgs.options.server
}

// Start STDIO transport (blocks until shutdown)
MCPRequestProcessor::startStdio( serverName )
```

**Run:**

```bash
# Start with default server
boxlang mcp-stdio.bxs

# Start with specific server
boxlang mcp-stdio.bxs --server myApp

# Or via environment variable
MCP_SERVER_NAME=myApp boxlang mcp-stdio.bxs
```

**Features:**

- ✅ JSON-RPC over STDIN/STDOUT
- ✅ Line-based protocol
- ✅ Graceful shutdown signal
- ✅ Process lifecycle management
- ❌ No HTTP headers/CORS (not needed)
- ❌ No status codes (JSON-RPC error codes used)

**Communication:**

```bash
# Input (STDIN)
{"jsonrpc":"2.0","method":"tools/list","params":{},"id":1}

# Output (STDOUT)
{"jsonrpc":"2.0","result":{"tools":[...]},"id":1}

# Shutdown
{"jsonrpc":"2.0","method":"shutdown","params":{},"id":99}
```

**📖 Complete Example:** See `examples/mcp/09-mcp-stdio.bxs` for a full working STDIO entry point with argument parsing.

### 🎯 Choosing a Transport

| Scenario | Recommended Transport |
|----------|----------------------|
| Web application with browser clients | HTTP |
| REST API for external services | HTTP |
| Desktop AI assistant (Claude Desktop, etc.) | STDIO |
| VS Code extension / IDE integration | STDIO |
| Command-line tool | STDIO |
| Docker container as MCP service | Both (HTTP for API, STDIO for container tools) |
| Testing/Development | HTTP (easier to test with curl) |

## Quick Start

### 1. Register Tools

Register tools at application startup (e.g., in `Application.bx`):

```java
// Application.bx
class {

    function onApplicationStart() {
        // Get or create an MCP server instance
        MCPServer( "myApp" )
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

### Custom URL Entry Points

You can create your own MCP server endpoints at any URL by importing and rendering the `MCPRequestProcessor`:

```javascript
// api/mcp-endpoint.bxm
<bx:script>
import bxModules.bxai.models.mcp.MCPRequestProcessor
MCPRequestProcessor::startHttp()
</bx:script>
```

This creates a fully functional MCP endpoint at your custom location:

```
POST http://localhost/api/mcp-endpoint.bxm?server=myApp
```

**Use Cases:**

- **Custom routing**: Place MCP endpoints under your API structure (`/api/v1/mcp`, `/admin/mcp`, etc.)
- **Security**: Put endpoints behind authentication middleware or custom security rules
- **Multiple entry points**: Create different endpoints for different purposes (public API, admin tools, etc.)
- **Framework integration**: Integrate MCP servers into existing URL routing schemes

**Example - Secured Admin Endpoint:**

```javascript
// admin/mcp-admin.bxm
<bx:script>
// Check admin authentication
if ( !session.isAdmin ) {
    writeOutput( serializeJSON({
        jsonrpc: "2.0",
        error: {
            code: -32000,
            message: "Unauthorized"
        },
        id: null
    }) )
    abort;
}

// Render MCP processor
import bxModules.bxai.models.mcp.MCPRequestProcessor
MCPRequestProcessor::startHttp()
</bx:script>
```

**Example - API Versioned Endpoint:**

```javascript
// api/v2/ai/mcp.bxm
<bx:script>
// Set API version header
request.setHeader( "X-API-Version", "2.0" )

// Render MCP processor
import bxModules.bxai.models.mcp.MCPRequestProcessor
MCPRequestProcessor::startHttp()
</bx:script>
```

The processor automatically:

- ✅ Extracts server name from query parameter (`?server=name`) or URL segment
- ✅ Handles JSON-RPC 2.0 request parsing
- ✅ Routes to the correct MCP server instance
- ✅ Returns properly formatted JSON-RPC responses
- ✅ Fires all MCP events (`onMCPRequest`, `onMCPResponse`, `onMCPError`)

## Server Configuration

### Creating a Server

```java
// Get or create a server instance (singleton by name)
server = MCPServer( "myApp" )

// Multiple servers for different purposes
apiServer = MCPServer( "api" )
adminServer = MCPServer( "admin" )
```

### Configure Description and Version

```java
server = MCPServer( "myApp" )
    .setDescription( "My Application MCP Server" )
    .setVersion( "2.0.0" )
```

### Get Server Info

```javascript
info = server.getServerInfo()
// { name: "myApp", version: "2.0.0" }
```

### Basic Authentication 🔒

Protect your MCP server with HTTP Basic Authentication:

```javascript
// Configure basic auth credentials
server = MCPServer( "myApp" )
    .withBasicAuth( "admin", "secretPassword123" )
    .registerTool( myTool )
```

**How it works:**
- Credentials are verified **before** any request processing
- Returns `401 Unauthorized` with `WWW-Authenticate` header if credentials are invalid
- Uses standard HTTP Basic Authentication (base64-encoded `username:password`)
- Zero performance overhead when not configured
- Fluent API for easy configuration

**Example - Secured Admin Server:**

```javascript
// Application.bx
function onApplicationStart() {
    MCPServer( "admin" )
        .withBasicAuth( "admin", application.adminPassword )
        .setDescription( "Admin MCP Server - Requires Authentication" )
        .registerTool( adminTool )
        .registerTool( systemTool )
}
```

**Making authenticated requests:**

```bash
# Using curl with basic auth
curl -X POST http://localhost/~bxai/mcp.bxm?server=admin \
  -u admin:secretPassword123 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":"1"}'

# Or with Authorization header
curl -X POST http://localhost/~bxai/mcp.bxm?server=admin \
  -H "Authorization: Basic YWRtaW46c2VjcmV0UGFzc3dvcmQxMjM=" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":"1"}'
```

**Check if authentication is enabled:**

```javascript
if ( server.hasBasicAuth() ) {
    writeOutput( "This server requires authentication" )
}
```

**Security Best Practices:**

- ✅ **Always use HTTPS** in production to prevent credential interception
- ✅ Store passwords in environment variables or encrypted configuration
- ✅ Use strong, unique passwords for each server
- ✅ Combine with CORS settings for additional security
- ✅ Consider rotating credentials periodically
- ✅ Log authentication failures for security monitoring

**Example with environment variables:**

```javascript
// Application.bx
function onApplicationStart() {
    var adminUser = getEnv( "MCP_ADMIN_USER" )
    var adminPass = getEnv( "MCP_ADMIN_PASS" )

    if ( len( adminUser ) && len( adminPass ) ) {
        MCPServer( "admin" )
            .withBasicAuth( adminUser, adminPass )
            .registerTool( adminTool )
    }
}
```

### CORS Configuration 🌐

Configure Cross-Origin Resource Sharing (CORS) to control which origins can access your MCP server:

```javascript
// Allow a single origin
server = MCPServer( "myApp" )
    .withCors( "https://example.com" )

// Allow multiple origins
server = MCPServer( "myApp" )
    .withCors( [ "https://example.com", "https://app.example.com" ] )

// Allow all origins (use with caution!)
server = MCPServer( "myApp" )
    .withCors( "*" )

// Wildcard subdomain matching
server = MCPServer( "myApp" )
    .withCors( [ "*.example.com", "https://trusted.org" ] )
```

**Wildcard Pattern Matching:**

The CORS implementation supports wildcard patterns for flexible domain matching:

- `*.example.com` - Matches any subdomain: `app.example.com`, `api.example.com`, `admin.example.com`
- `*` - Matches all origins (returns the requesting origin in `Access-Control-Allow-Origin`)
- Exact matches: `https://example.com` - Only matches exactly this origin

**Dynamic CORS Management:**

```javascript
// Add origins incrementally
server = MCPServer( "myApp" )
    .addCorsOrigin( "https://example.com" )
    .addCorsOrigin( "https://app.example.com" )

// Get configured origins
origins = server.getCorsAllowedOrigins()
// Returns: [ "https://example.com", "https://app.example.com" ]

// Check if an origin is allowed
if ( server.isCorsAllowed( "https://example.com" ) ) {
    writeOutput( "Origin is allowed" )
}
```

**How CORS Works:**

When a browser makes a cross-origin request:

1. Browser sends `Origin` header with the request
2. Server checks if origin matches allowed patterns
3. If allowed, server responds with `Access-Control-Allow-Origin` header
4. Server also includes CORS headers in OPTIONS preflight responses

**CORS Headers Set by Server:**

- `Access-Control-Allow-Origin` - Allowed origin (dynamic or `*`)
- `Access-Control-Allow-Methods` - `GET, POST, OPTIONS`
- `Access-Control-Allow-Headers` - `Content-Type, Authorization, X-API-Key`
- `Access-Control-Max-Age` - `86400` (24 hours)

**Security Best Practices:**

- ✅ **Avoid `*` in production** - Specify exact origins or wildcard patterns
- ✅ **Use HTTPS origins** - Always prefer secure origins
- ✅ **Combine with authentication** - CORS doesn't replace authentication
- ✅ **Review periodically** - Remove unused origins
- ✅ **Test preflight requests** - Verify OPTIONS requests work correctly

**Example - Multi-Environment Setup:**

```javascript
// Application.bx
function onApplicationStart() {
    var allowedOrigins = []

    // Add origins based on environment
    if ( getEnv( "ENVIRONMENT" ) == "production" ) {
        allowedOrigins = [
            "https://app.example.com",
            "https://admin.example.com"
        ]
    } else {
        // Development - allow local testing
        allowedOrigins = [
            "http://localhost:3000",
            "http://localhost:8080",
            "*.ngrok.io"  // For tunneling/testing
        ]
    }

    MCPServer( "api" )
        .withCors( allowedOrigins )
        .registerTool( myTool )
}
```

### Request Body Size Limits 📏

Protect your server from large payloads by setting request body size limits:

```javascript
// Limit to 1MB (in bytes)
server = MCPServer( "myApp" )
    .withBodyLimit( 1048576 )

// Limit to 500KB
server = MCPServer( "myApp" )
    .withBodyLimit( 500 * 1024 )

// Unlimited (default: 0)
server = MCPServer( "myApp" )
    .withBodyLimit( 0 )
```

**How it works:**

- Server checks `len(requestBody)` before processing
- If body exceeds limit, returns `413 Payload Too Large` error
- Default is `0` (unlimited)
- Limit applies to entire JSON-RPC request body

**Error Response (413):**

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32000,
    "message": "Request body too large (max: 1048576 bytes)"
  },
  "id": null
}
```

**Get current limit:**

```javascript
maxSize = server.getMaxRequestBodySize()
// Returns: 1048576 (or 0 for unlimited)
```

**Use Cases:**

- **Public APIs** - Prevent abuse from extremely large payloads
- **Resource constraints** - Match server memory/processing limits
- **Tool-specific limits** - Different servers can have different limits
- **DoS prevention** - Basic protection against payload attacks

**Example - Tiered Limits:**

```javascript
// Public API - strict limits
MCPServer( "public" )
    .withBodyLimit( 100 * 1024 )  // 100KB
    .registerTool( publicTool )

// Internal API - generous limits
MCPServer( "internal" )
    .withBodyLimit( 10 * 1024 * 1024 )  // 10MB
    .withBasicAuth( "admin", "secret" )
    .registerTool( adminTool )

// Data import - unlimited
MCPServer( "import" )
    .withBodyLimit( 0 )  // No limit
    .withBasicAuth( "importer", "secret" )
    .registerTool( importTool )
```

### Custom API Key Validation 🔑

Implement custom API key authentication logic with provider callbacks:

```javascript
// Simple API key validation
server = MCPServer( "myApp" )
    .withApiKeyProvider( ( apiKey, requestData ) => {
        return apiKey == "my-secret-key-12345"
    } )

// Database lookup
server = MCPServer( "myApp" )
    .withApiKeyProvider( ( apiKey, requestData ) => {
        var user = userService.findByApiKey( apiKey )
        return !isNull( user ) && user.isActive
    } )

// Complex validation with rate limiting
server = MCPServer( "myApp" )
    .withApiKeyProvider( ( apiKey, requestData ) => {
        var key = apiKeyService.validate( apiKey )
        if ( isNull( key ) ) return false

        // Check rate limits
        if ( rateLimiter.isExceeded( key.userId ) ) {
            throw( "Rate limit exceeded", "RateLimitError" )
        }

        // Log usage
        auditLog.record( key.userId, requestData.method )

        return true
    } )
```

**Provider Function Signature:**

```javascript
function apiKeyProvider(
    required string apiKey,      // The API key from request
    required struct requestData  // Request context
) {
    // Return true to allow, false to deny
    // Throw exception for custom error messages
    return true
}
```

**Request Data Struct:**

The `requestData` argument contains:
- `method` - MCP method being called (e.g., `"tools/list"`)
- `serverName` - Name of the MCP server
- `body` - Full request body as string
- Any other request metadata

**API Key Extraction:**

The server automatically extracts API keys from:
1. `X-API-Key` header
2. `Authorization: Bearer <token>` header

**How it works:**

1. Server checks for API key in headers
2. If found, calls your provider function with key and request data
3. If provider returns `false` or throws, returns `401 Unauthorized`
4. If provider returns `true`, request proceeds normally

**Error Response (401):**

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32000,
    "message": "Invalid API key"
  },
  "id": null
}
```

**Check if provider is configured:**

```javascript
if ( server.hasApiKeyProvider() ) {
    writeOutput( "API key validation is enabled" )
}
```

**Making authenticated requests:**

```bash
# Using X-API-Key header
curl -X POST http://localhost/~bxai/mcp.bxm?server=myApp \
  -H "X-API-Key: my-secret-key-12345" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":"1"}'

# Using Bearer token
curl -X POST http://localhost/~bxai/mcp.bxm?server=myApp \
  -H "Authorization: Bearer my-secret-key-12345" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":"1"}'
```

**Advanced Example - Multi-Tenant with Usage Tracking:**

```javascript
// Application.bx
function onApplicationStart() {
    MCPServer( "api" )
        .withApiKeyProvider( ( apiKey, requestData ) => {
            // Validate and get tenant
            var tenant = tenantService.validateApiKey( apiKey )
            if ( isNull( tenant ) ) return false

            // Check feature access
            if ( !tenant.hasFeature( requestData.method ) ) {
                throw( "Feature not available in your plan", "AccessDenied" )
            }

            // Store tenant context for later use
            request.tenantId = tenant.id
            request.tenantName = tenant.name

            // Track usage
            usageTracker.record(
                tenantId: tenant.id,
                method: requestData.method,
                timestamp: now()
            )

            return true
        } )
        .registerTool( myTool )
}
```

**Combining Security Features:**

You can use multiple security features together:

```javascript
server = MCPServer( "enterprise" )
    // CORS - restrict origins
    .withCors( [ "https://app.example.com", "*.example.com" ] )
    // Body limits - prevent abuse
    .withBodyLimit( 1048576 )  // 1MB
    // API keys - identify clients
    .withApiKeyProvider( validateApiKey )
    // Basic auth - admin access
    .withBasicAuth( "admin", "secret" )
    // Tools
    .registerTool( toolOne )
    .registerTool( toolTwo )
```

**Security Priority:**

1. **Body size check** - Happens first (reject oversized payloads immediately)
2. **CORS validation** - Checks Origin header against allowed patterns
3. **Basic authentication** - HTTP Basic Auth credentials check
4. **API key validation** - Custom provider callback execution
5. **Request processing** - Only if all checks pass

### Security Headers 🛡️

The MCP server automatically includes industry-standard security headers in all responses:

**Headers Included:**

- `X-Content-Type-Options: nosniff` - Prevents MIME type sniffing
- `X-Frame-Options: DENY` - Prevents clickjacking attacks
- `X-XSS-Protection: 1; mode=block` - Enables XSS filtering
- `Referrer-Policy: strict-origin-when-cross-origin` - Controls referrer information
- `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` - Restricts resource loading
- `Strict-Transport-Security: max-age=31536000; includeSubDomains` - Forces HTTPS (when applicable)
- `Permissions-Policy: geolocation=(), microphone=(), camera=()` - Disables sensitive browser features

**Automatic Application:**

Security headers are added to:
- ✅ Successful responses (200 OK)
- ✅ Error responses (400, 401, 404, 413, 500)
- ✅ CORS preflight responses (OPTIONS)

No configuration needed - these headers are applied automatically to enhance security posture.

## Tool Registration

### Register a Single Tool

```java
server = MCPServer( "myApp" )
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

server = MCPServer( "myApp" )
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
MCPServer( "myApp" ).scan( "/path/to/MyTools.bx" )

// Scan a directory (recursively scans all .bx files)
MCPServer( "myApp" ).scan( "/path/to/tools/" )
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
server = MCPServer( "myApp" )
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
server = MCPServer( "myApp" )
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
        var server = MCPServer( "myApp" )
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

## Events & Interception 🎯

The MCP Server fires custom events during its lifecycle, allowing you to add custom logging, monitoring, alerting, and integration with other systems.

> **📡 Transport Agnostic**: All MCP events work identically for both HTTP and STDIO transports. The same event handlers will be called regardless of how clients connect to your MCP server.

### Available Events

#### `onMCPServerCreate`
Fired when a new MCP server instance is created.

```javascript
function onMCPServerCreate( event, interceptData ) {
    // interceptData contains:
    // - server: The newly created MCPServer instance
    // - name: The server name
    writeLog( "MCP server created: #interceptData.name#" )
}
```

#### `onMCPServerRemove`
Fired when an MCP server instance is removed from the registry.

```javascript
function onMCPServerRemove( event, interceptData ) {
    // interceptData contains:
    // - name: The server name that was removed
    writeLog( "MCP server removed: #interceptData.name#" )
}
```

#### `onMCPRequest`
Fired before processing an incoming MCP request.

```javascript
function onMCPRequest( event, interceptData ) {
    // interceptData contains:
    // - server: The MCPServer instance
    // - requestData: The parsed request { id, method, params }

    writeLog(
        type: "information",
        file: "mcp-requests",
        text: "Request: #interceptData.requestData.method# (ID: #interceptData.requestData.id#)"
    )
}
```

#### `onMCPResponse`
Fired after processing an MCP request and before returning the response.

```javascript
function onMCPResponse( event, interceptData ) {
    // interceptData contains:
    // - server: The MCPServer instance
    // - requestData: The original request
    // - responseData: The response being returned
    // - success: Whether the request was successful
    // - responseTime: Time taken in milliseconds

    if ( !interceptData.success ) {
        writeLog(
            type: "warning",
            file: "mcp-failures",
            text: "Failed request: #interceptData.requestData.method#"
        )
    }
}
```

#### `onMCPError` ⚠️
Fired when an exception occurs during MCP server operations.

```javascript
function onMCPError( event, interceptData ) {
    // interceptData contains:
    // - server: The MCPServer instance
    // - context: Where the error occurred ("handleRequest", "scanClass", etc.)
    // - exception: The exception object with message, detail, stacktrace
    // - Additional context-specific fields

    var exception = interceptData.exception
    var context = interceptData.context

    // Log detailed error
    writeLog(
        type: "error",
        file: "mcp-errors",
        text: "MCP Error in #context#: #exception.message#"
    )

    // Context-specific handling
    if ( context == "handleRequest" ) {
        // Additional fields available: method, requestId, params, responseTime, errorCode
        writeLog(
            type: "error",
            file: "mcp-errors",
            text: "Request failed - Method: #interceptData.method#, Error: #exception.message#"
        )

        // Send alert
        emailService.sendAlert(
            subject: "MCP Server Error",
            body: "Method: #interceptData.method#\nError: #exception.message#\nStacktrace: #exception.stackTrace#"
        )
    }
}
```

### Registering Event Listeners

#### For BoxLang Module Registration

In your module's `ModuleConfig.bx`:

```javascript
function configure() {
    interceptors = [
        {
            class: "MyMCPInterceptor",
            properties: {}
        }
    ];
}
```

#### For Application/Script Registration

Use `BoxRegisterInterceptor()` to register listeners:

```javascript
// Application.bx
class {

    function onApplicationStart() {
        // Register self as interceptor for MCP events
        BoxRegisterInterceptor( this, "onMCPRequest,onMCPResponse,onMCPError" )

        // Create server
        MCPServer( "myApp" )
            .registerTool( myTool )
    }

    function onMCPRequest( event, interceptData ) {
        // Track incoming requests
        metrics.increment( "mcp.requests" )
    }

    function onMCPResponse( event, interceptData ) {
        // Track response times
        metrics.gauge( "mcp.responseTime", interceptData.responseTime )
    }

    function onMCPError( event, interceptData ) {
        // Handle errors
        errorTracker.captureException( interceptData.exception )
    }

}
```

### Event Use Cases

#### Custom Logging
```javascript
function onMCPRequest( event, interceptData ) {
    writeLog(
        type: "information",
        file: "mcp-audit",
        text: "User: #session.user.id#, Method: #interceptData.requestData.method#"
    )
}
```

#### Metrics & Monitoring
```javascript
function onMCPResponse( event, interceptData ) {
    // Send to monitoring service
    metrics.gauge( "mcp.responseTime", interceptData.responseTime )
    metrics.increment( "mcp.requests", { method: interceptData.requestData.method } )
}

function onMCPError( event, interceptData ) {
    metrics.increment( "mcp.errors", { context: interceptData.context } )
}
```

#### Rate Limiting
```javascript
function onMCPRequest( event, interceptData ) {
    var clientId = request.getHeader( "X-Client-ID" )

    if ( !rateLimiter.allow( clientId ) ) {
        // Reject request
        interceptData.reject = true
        interceptData.errorMessage = "Rate limit exceeded"
    }
}
```

#### Error Alerting
```javascript
function onMCPError( event, interceptData ) {
    // Critical error alerting
    if ( interceptData.context == "handleRequest" ) {
        slackService.sendMessage(
            channel: "##alerts",
            text: "MCP Error: #interceptData.exception.message#"
        )
    }
}
```

## Statistics & Monitoring 📊

The MCP server automatically tracks performance and usage metrics for real-time monitoring and analytics.

### Enabling Statistics

Statistics are **enabled by default** when creating an MCP server:

```java
// Stats enabled by default
server = MCPServer( "myApp" )

// Explicitly control stats tracking
server = MCPServer(
    name: "myApp",
    statsEnabled: true  // or false to disable
)
```

### Retrieving Statistics

#### Get Summary Statistics

Quick overview of key metrics (lightweight):

```java
summary = server.getStatsSummary()

writeOutput( "Total Requests: #summary.totalRequests#" )
writeOutput( "Success Rate: #summary.successRate#%" )
writeOutput( "Avg Response Time: #summary.avgResponseTime#ms" )
writeOutput( "Tool Calls: #summary.totalToolInvocations#" )
writeOutput( "Resource Reads: #summary.totalResourceReads#" )
writeOutput( "Prompts Generated: #summary.totalPromptGenerations#" )
writeOutput( "Errors: #summary.totalErrors#" )
writeOutput( "Uptime: #summary.uptime / 1000#s" )
```

Summary includes:
- `uptime` - Server uptime in milliseconds
- `totalRequests` - Total requests processed
- `successRate` - Success rate as percentage
- `avgResponseTime` - Average response time in milliseconds
- `totalToolInvocations` - Total tool calls
- `totalResourceReads` - Total resource reads
- `totalPromptGenerations` - Total prompt generations
- `totalErrors` - Total errors encountered
- `lastRequestAt` - Timestamp of last request (empty if no requests)

#### Get Detailed Statistics

Complete stats with breakdowns:

```java
stats = server.getStats()

// Request breakdown
writeOutput( "Successful: #stats.requests.successful#" )
writeOutput( "Failed: #stats.requests.failed#" )
writeOutput( "By Method: #serializeJSON( stats.requests.byMethod )#" )
writeOutput( "Min/Max Response Time: #stats.requests.minResponseTime# / #stats.requests.maxResponseTime#ms" )

// Tool breakdown
writeOutput( "Tools by name: #serializeJSON( stats.tools.byTool )#" )
writeOutput( "Avg Execution Time: #stats.tools.avgExecutionTime#ms" )

// Resource breakdown
writeOutput( "Reads by URI: #serializeJSON( stats.resources.byUri )#" )

// Prompt breakdown
writeOutput( "Generations by name: #serializeJSON( stats.prompts.byName )#" )

// Error breakdown
writeOutput( "Errors by code: #serializeJSON( stats.errors.byCode )#" )
if ( !stats.errors.lastError.isEmpty() ) {
    writeOutput( "Last Error: #stats.errors.lastError.message# at #stats.errors.lastError.timestamp#" )
}
```

Detailed stats include:
- **Requests**: total, successful, failed, byMethod (map), response times (array), avg/min/max times, lastRequestAt
- **Tools**: totalInvocations, byTool (map with count/totalTime/avgTime per tool), execution times (array), avg/min/max times
- **Resources**: totalReads, byUri (map of read counts)
- **Prompts**: totalGenerations, byName (map of generation counts)
- **Errors**: total, byCode (map of error counts), lastError (code/message/timestamp)

### Managing Statistics

#### Check if Stats are Enabled

```java
if ( server.isStatsEnabled() ) {
    writeOutput( "Stats tracking is active" )
}
```

#### Enable/Disable Tracking

```java
// Disable stats tracking (stops recording new data)
server.disableStats()

// Re-enable stats tracking
server.enableStats()

// Fluent chaining works
server.enableStats()
    .registerTool( myTool )
    .registerResource( myResource )
```

#### Reset Statistics

Clear all statistics back to zero:

```java
// Reset all counters and data
server.resetStats()

writeOutput( "Statistics have been reset" )

// Fluent chaining
server.resetStats()
    .enableStats()
```

### Dashboard Example

Create a real-time monitoring dashboard:

```java
// Get server stats
server = MCPServer( "myApp" )
stats = server.getStatsSummary()

writeOutput( "
    <div class='stats-dashboard'>
        <h2>MCP Server: myApp</h2>

        <div class='stat-box'>
            <label>Uptime</label>
            <value>#numberFormat( stats.uptime / 1000, '0' )#s</value>
        </div>

        <div class='stat-box'>
            <label>Total Requests</label>
            <value>#stats.totalRequests#</value>
        </div>

        <div class='stat-box'>
            <label>Success Rate</label>
            <value>#numberFormat( stats.successRate, '0.00' )#%</value>
        </div>

        <div class='stat-box'>
            <label>Avg Response</label>
            <value>#numberFormat( stats.avgResponseTime, '0' )#ms</value>
        </div>

        <div class='stat-box'>
            <label>Tool Calls</label>
            <value>#stats.totalToolInvocations#</value>
        </div>

        <div class='stat-box error'>
            <label>Errors</label>
            <value>#stats.totalErrors#</value>
        </div>
    </div>
" )
```

### Using Stats with Events

Combine statistics with event interception for custom monitoring:

```java
// Application.bx
class {

    function onApplicationStart() {
        MCPServer( "myApp" )
            .registerTool( myTool )

        // Register interceptor for custom metrics
        BoxRegisterInterceptor( this, "onMCPRequest,onMCPResponse" )
    }

    function onMCPRequest( event, interceptData ) {
        // Log incoming requests
        writeLog(
            type: "information",
            file: "mcp-requests",
            text: "Request: #interceptData.requestData.method#"
        )
    }

    function onMCPResponse( event, interceptData ) {
        // Get current stats after each request
        var server = interceptData.server
        var summary = server.getStatsSummary()

        // Alert on high error rate
        if ( summary.successRate < 90 && summary.totalRequests > 10 ) {
            writeLog(
                type: "error",
                file: "mcp-errors",
                text: "High error rate: #summary.successRate#%"
            )
        }

        // Alert on slow responses
        if ( summary.avgResponseTime > 1000 ) {
            writeLog(
                type: "warning",
                file: "mcp-performance",
                text: "Slow avg response: #summary.avgResponseTime#ms"
            )
        }
    }

    function onMCPError( event, interceptData ) {
        // Handle MCP server errors
        var exception = interceptData.exception
        var context = interceptData.context

        // Log detailed error information
        writeLog(
            type: "error",
            file: "mcp-errors",
            text: "MCP Error in #context#: #exception.message# - #exception.detail#"
        )

        // Send alert for critical errors
        if ( context == "handleRequest" ) {
            // Alert operations team
            emailService.sendAlert(
                subject: "MCP Server Error",
                body: "Method: #interceptData.method#, Error: #exception.message#"
            )
        }
    }

}
```

### Performance Notes

- **Zero Overhead When Disabled**: When `statsEnabled: false`, no performance impact
- **Memory Efficient**: Only last 1000 timing samples retained per metric
- **Thread Safe**: Uses atomic operations for concurrent request handling
- **Real-Time**: Stats updated immediately on each operation
- **Lightweight Summary**: `getStatsSummary()` is optimized for frequent polling

### API Integration Example

Expose stats via REST API:

```java
// In your API handler
class {

    function getServerStats( required string serverName ) {
        var server = MCPServer( arguments.serverName )

        if ( isNull( server ) ) {
            return {
                success: false,
                error: "Server not found"
            }
        }

        return {
            success: true,
            data: server.getStatsSummary()
        }
    }

    function resetServerStats( required string serverName ) {
        var server = MCPServer( arguments.serverName )

        if ( isNull( server ) ) {
            return { success: false }
        }

        server.resetStats()

        return {
            success: true,
            message: "Statistics reset successfully"
        }
    }

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

```javascript
// API server for external clients
MCPServer( "api" )
    .registerTool( publicTool1 )
    .registerTool( publicTool2 )

// Admin server for internal tools
MCPServer( "admin" )
    .withBasicAuth( "admin", getEnv( "ADMIN_PASSWORD" ) )
    .registerTool( adminTool1 )
    .registerTool( adminTool2 )
```

### 5. Secure Sensitive Servers

```javascript
// Protect production servers with authentication
MCPServer( "production" )
    .withBasicAuth( getEnv( "MCP_USER" ), getEnv( "MCP_PASS" ) )
    .setCors( "https://trusted-domain.com" )
    .registerTool( sensitiveDataTool )

// Public servers can remain open
MCPServer( "public" )
    .setCors( "*" )
    .registerTool( publicSearchTool )
```

**Security Checklist:**
- ✅ Use `.withBasicAuth()` for sensitive operations
- ✅ Configure CORS with `.setCors()` to restrict origins
- ✅ Store credentials in environment variables
- ✅ Use HTTPS in production
- ✅ Monitor authentication failures via `onMCPError` events
- ✅ Implement rate limiting if needed

### 6. Document Your Tools

```javascript
aiTool( "calculateShipping", "Calculate shipping cost based on weight and destination", handler )
    .describeArg( "weight", "Package weight in pounds" )
    .describeArg( "destination", "Destination zip code" )
    .describeArg( "expedited", "Whether to use expedited shipping (true/false)" )
```

### 7. Monitor Performance with Statistics

```java
// Enable stats for production monitoring
server = MCPServer(
    name: "production",
    statsEnabled: true
)

// Periodically check performance
scheduled task: "checkMCPPerformance", interval: "5m" {
    var stats = server.getStatsSummary()

    if ( stats.avgResponseTime > 500 ) {
        // Alert: slow responses
        notificationService.alert( "MCP server responding slowly" )
    }

    if ( stats.successRate < 95 ) {
        // Alert: high error rate
        notificationService.alert( "MCP server error rate elevated" )
    }
}
```

## Related Documentation

- [MCP Client](./mcp-client.md) - Consuming MCP servers
- [AI Tools](../main-components/tools.md) - Creating tools
- [AI Agents](../main-components/agents.md) - Using agents with tools

## External Resources

- [Model Context Protocol Specification](https://modelcontextprotocol.io)
- [MCP Implementation Examples](https://github.com/modelcontextprotocol)
