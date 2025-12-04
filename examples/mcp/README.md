# MCP Server Examples

This directory contains comprehensive examples demonstrating how to use the BoxLang AI module's Model Context Protocol (MCP) server functionality.

## Examples Overview

### 1. `basic-server.bxs` - Getting Started
**What it demonstrates:**
- Creating a simple MCP server
- Registering tools with arguments
- Registering resources
- Registering prompts
- Handling JSON-RPC requests

**Run it:**
```bash
boxlang run examples/mcp/basic-server.bxs
```

**Key concepts:**
- `mcpServer()` - Create or get a server instance
- `registerTool()` - Add callable tools
- `registerResource()` - Add readable resources
- `registerPrompt()` - Add prompt templates
- `handleRequest()` - Process JSON-RPC requests

---

### 2. `http-endpoint.bxs` - Web Integration
**What it demonstrates:**
- Exposing MCP over HTTP
- Handling POST requests
- JSON-RPC over HTTP
- CORS headers
- Production-ready endpoint

**Deploy it:**
1. Copy to your web root
2. Access via: `http://localhost:8080/http-endpoint.bxs`

**Test it:**
```bash
curl -X POST http://localhost:8080/http-endpoint.bxs \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":"1"}'
```

**Key concepts:**
- Singleton pattern for server instances
- HTTP request/response handling
- Content negotiation
- CORS configuration

---

### 3. `annotation-based.bxs` - Declarative Tools
**What it demonstrates:**
- Using `@mcpTool` annotations
- Using `@mcpResource` annotations
- Using `@mcpPrompt` annotations
- Automatic tool discovery with `scan()`

**Run it:**
```bash
boxlang run examples/mcp/annotation-based.bxs
```

**Key concepts:**
- Annotation formats (simple, string, struct)
- Automatic argument extraction
- Class-based tool organization
- `scan()` method for directory scanning

---

### 4. `multi-server.bxs` - Multiple Servers
**What it demonstrates:**
- Managing multiple MCP servers
- Server isolation
- Static server management
- Use cases: multi-tenant, API versions

**Run it:**
```bash
boxlang run examples/mcp/multi-server.bxs
```

**Key concepts:**
- Named server instances
- `hasInstance()` / `removeInstance()` / `getInstanceNames()`
- Server lifecycle management
- Access control patterns

---

### 5. `mcp-security-example.bxs` - Enterprise Security Features ✨

**What it demonstrates:**

- CORS configuration with wildcard patterns
- Request body size limits
- Custom API key validation
- HTTP Basic Authentication
- Combined security configurations
- Environment-based security setup

**Run it:**

```bash
boxlang run examples/mcp/mcp-security-example.bxs
```

**Key concepts:**

- `withCors()` - Configure allowed origins with wildcard support
- `withBodyLimit()` - Protect against oversized payloads
- `withApiKeyProvider()` - Custom API key validation logic
- `withBasicAuth()` - HTTP Basic Authentication
- Security processing order: Body size → CORS → Basic Auth → API Key
- Automatic security headers in all responses

**Security features covered:**

- ✅ CORS: Wildcard patterns (`*.example.com`), multiple origins, dynamic addition
- ✅ Body limits: Per-server size limits, 413 error responses
- ✅ API keys: Custom validation callbacks, multi-tier access control
- ✅ Basic auth: Simple username/password protection
- ✅ Security headers: Automatic X-Content-Type-Options, X-Frame-Options, CSP, HSTS, etc.

---

### 6. `advanced-features.bx` - Production Patterns

**What it demonstrates:**

- Request/response interceptors
- Authentication/authorization
- Error handling
- Complex tool implementations
- Dynamic resources
- Metadata and logging

**Run it:**

```bash
boxlang run examples/mcp/advanced-features.bx
```

**Key concepts:**

- `onRequest()` / `onResponse()` callbacks
- Security patterns
- Tool validation
- Resource generation
- Structured responses

---

## Quick Reference

### Creating a Server

```javascript
myServer = mcpServer( "myApp" )
    .setDescription( "My MCP Server" )
    .setVersion( "1.0.0" )
```

### Registering a Tool

```javascript

myServer.registerTool(
    aiTool( "toolName", "Description", ( arg1, arg2 ) => {
        return "result"
    } )
        .describeArg( "arg1", "First argument" )
        .describeArg( "arg2", "Second argument" )
)
```

### Registering a Resource

```javascript
myServer.registerResource(
    uri: "docs://readme",
    name: "README",
    description: "Documentation",
    mimeType: "text/markdown",
    handler: () => "# Content"
)
```

### Registering a Prompt

```javascript
myServer.registerPrompt(
    name: "greeting",
    description: "Generate greeting",
    args: [
        { name: "name", description: "Person name", required: true }
    ],
    handler: ( args ) => [
        { role: "user", content: "Say hello to #args.name#" }
    ]
)
```

### Handling Requests

```javascript
request = {
    "jsonrpc": "2.0",
    "method": "tools/list",
    "id": "1"
}

response = myServer.handleRequest( request )
```

## JSON-RPC Methods

| Method | Description |
|--------|-------------|
| `initialize` | Get server capabilities and info |
| `tools/list` | List all available tools |
| `tools/call` | Invoke a specific tool |
| `resources/list` | List all available resources |
| `resources/read` | Read a specific resource |
| `prompts/list` | List all available prompts |
| `prompts/get` | Get a specific prompt |
| `ping` | Health check |

## Testing with CLI

### Using curl

```bash
# List tools
curl -X POST http://localhost:8080/mcp-endpoint.bxs \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":"1"}'

# Call a tool
curl -X POST http://localhost:8080/mcp-endpoint.bxs \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "id": "2",
    "params": {
      "name": "search",
      "arguments": {"query": "test"}
    }
  }'
```

### Using BoxLang

```javascript
// Create HTTP request
response = httpRequest( "http://localhost:8080/mcp-endpoint.bxs" )
    .setMethod( "POST" )
    .addHeader( "Content-Type", "application/json" )
    .setBody( jsonSerialize( {
        "jsonrpc": "2.0",
        "method": "tools/list",
        "id": "1"
    } ) )
    .send()

result = jsonDeserialize( response.fileContent )
```

## Best Practices

1. **Use descriptive names** - Tool and resource names should be clear and self-documenting
2. **Validate inputs** - Always validate tool arguments before processing
3. **Handle errors gracefully** - Use try/catch and return meaningful error messages
4. **Document arguments** - Use `describeArg()` for all tool parameters
5. **Singleton pattern** - Use the same server name to get the same instance
6. **Security first** - Implement authentication in `onRequest()` callback
7. **Keep tools focused** - Each tool should do one thing well
8. **Use structured responses** - Return objects/structs rather than plain strings when possible

## Next Steps

- Read the [MCP Documentation](../../src/docs/mcp-server.md)
- Review the [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- Check the [Model Context Protocol](https://modelcontextprotocol.io/) official docs
- Explore the [test suite](../../src/test/java/ortus/boxlang/ai/mcp/mcpServerTest.java) for more examples

## Need Help?

- GitHub Issues: https://github.com/ortus-boxlang/bx-ai/issues
- Documentation: https://github.com/ortus-boxlang/bx-ai
- Community: https://community.ortussolutions.com
