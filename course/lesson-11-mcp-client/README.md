# Lesson 11: Model Context Protocol (MCP) Client

**Duration:** 90 minutes  
**Prerequisites:** Lessons 1-6 completed

## Learning Objectives

By the end of this lesson, you will:

- Understand the Model Context Protocol (MCP)
- Create and configure MCP clients
- Discover tools, resources, and prompts from MCP servers
- Invoke tools and access resources
- Integrate MCP with AI chat
- Handle errors and responses
- Build real-world MCP integrations

---

## Part 1: What is MCP?

### The Problem

AI models are powerful but limited:
- They can't access real-time data
- They don't know about your specific documentation
- They can't use your internal tools
- They're disconnected from your systems

### The Solution: Model Context Protocol

MCP is a standardized protocol that enables AI models to:

- 🔧 **Use Tools**: Execute functions on remote servers
- 📚 **Access Resources**: Read documents, databases, and external data
- 💬 **Use Prompts**: Leverage server-defined prompt templates
- 🔍 **Discover Capabilities**: Dynamically learn what a server offers

### MCP Architecture

```
┌─────────────┐      MCP      ┌─────────────┐
│   AI App    │ ◄──────────►  │ MCP Server  │
│  (BoxLang)  │   HTTP/JSON   │             │
└─────────────┘               └──────┬──────┘
                                     │
                            ┌────────┴────────┐
                            │   Tools         │
                            │   Resources     │
                            │   Prompts       │
                            └─────────────────┘
```

---

## Part 2: Creating an MCP Client

### Basic Client Creation

```java
// Create a client pointing to an MCP server
client = MCP( "http://localhost:3000" )

// That's it! Now you can use the client
```

### Configuration Options

```java
// Configure the client with fluent API
client = MCP( "http://localhost:3000" )
    .withTimeout( 10000 )                        // 10 second timeout
    .withBearerToken( "your-token-here" )        // Authentication
    .withHeaders( { "X-API-Key": "key123" } )    // Custom headers
```

### Authentication Methods

```java
// Bearer Token (recommended)
client = MCP( "http://api.example.com" )
    .withBearerToken( getSystemSetting( "MCP_TOKEN" ) )

// Basic Auth
client = MCP( "http://api.example.com" )
    .withAuth( "username", "password" )
```

### Callbacks for Observability

```java
client = MCP( "http://localhost:3000" )
    .onSuccess( ( response ) => {
        writeLog( "MCP request succeeded: #response.getStatusCode()#" )
    } )
    .onError( ( response ) => {
        writeLog( "MCP request failed: #response.getError()#" )
    } )
```

**Practice Exercise:**

Create an MCP client with:
- 5 second timeout
- Bearer token from environment variable
- Success callback that logs the status code

<details>
<summary>Solution</summary>

```java
client = MCP( "http://localhost:3000" )
    .withTimeout( 5000 )
    .withBearerToken( getSystemSetting( "MCP_TOKEN" ) )
    .onSuccess( ( response ) => {
        writeLog( 
            type: "info",
            text: "MCP Success: #response.getStatusCode()#"
        )
    } )
```
</details>

---

## Part 3: Discovery - What Can the Server Do?

### List Available Tools

```java
// Discover what tools the server offers
tools = MCP( "http://localhost:3000" ).listTools()

if ( tools.getSuccess() ) {
    for ( tool in tools.getData() ) {
        writeOutput( "Tool: #tool.name#<br>" )
        writeOutput( "Description: #tool.description#<br>" )
        writeOutput( "Parameters: #serializeJSON( tool.parameters )#<hr>" )
    }
}
```

### List Resources

```java
// See what resources are available
resources = MCP( "http://localhost:3000" ).listResources()

if ( resources.getSuccess() ) {
    for ( resource in resources.getData() ) {
        writeOutput( "Resource: #resource.uri# (#resource.type#)<br>" )
    }
}
```

### List Prompts

```java
// Get available prompt templates
prompts = MCP( "http://localhost:3000" ).listPrompts()

if ( prompts.getSuccess() ) {
    for ( prompt in prompts.getData() ) {
        writeOutput( "Prompt: #prompt.name#<br>" )
        writeOutput( "Arguments: #prompt.arguments.toList()#<br>" )
    }
}
```

### Get Server Capabilities

```java
// Check what the server supports
caps = MCP( "http://localhost:3000" ).getCapabilities()

if ( caps.getSuccess() ) {
    info = caps.getData()
    writeOutput( "Server: #info.serverInfo.name#<br>" )
    writeOutput( "Version: #info.serverInfo.version#<br>" )
    writeOutput( "Supports Tools: #info.capabilities.tools#<br>" )
}
```

**Practice Exercise:**

Write a function that connects to an MCP server and returns a struct with:
- Number of available tools
- Number of available resources
- Server name from capabilities

<details>
<summary>Solution</summary>

```java
function getMCPServerInfo( required string url ) {
    var client = MCP( arguments.url )
    
    var tools = client.listTools()
    var resources = client.listResources()
    var caps = client.getCapabilities()
    
    return {
        toolCount: tools.getSuccess() ? tools.getData().len() : 0,
        resourceCount: resources.getSuccess() ? resources.getData().len() : 0,
        serverName: caps.getSuccess() ? caps.getData().serverInfo.name : "Unknown"
    }
}

// Usage
info = getMCPServerInfo( "http://localhost:3000" )
writeOutput( "Server '#info.serverName#' has #info.toolCount# tools" )
```
</details>

---

## Part 4: Executing Operations

### Send Tool Request

```java
// Invoke a tool with arguments
result = MCP( "http://localhost:3000" )
    .send( "searchDocs", {
        query: "BoxLang installation",
        limit: 10
    } )

if ( result.getSuccess() ) {
    data = result.getData()
    writeOutput( "Found #data.results.len()# results" )
}
```

### Read Resource

```java
// Get content from a resource
content = MCP( "http://localhost:3000" )
    .readResource( "docs://getting-started.md" )

if ( content.getSuccess() ) {
    markdown = content.getData().content
    writeOutput( markdown )
}
```

### Get Prompt

```java
// Get a prompt template with arguments
prompt = MCP( "http://localhost:3000" )
    .getPrompt( "generateCode", {
        language: "java",
        description: "Sort an array"
    } )

if ( prompt.getSuccess() ) {
    promptText = prompt.getData().messages[ 1 ].content
    
    // Use with AI
    code = aiChat( promptText )
    writeOutput( code )
}
```

**Practice Exercise:**

Create a function that:
1. Searches docs via MCP
2. Returns only the titles and excerpts
3. Handles errors gracefully

<details>
<summary>Solution</summary>

```java
function searchDocs( required string query, numeric limit = 5 ) {
    var client = MCP( "http://localhost:3000" )
    
    var result = client.send( "searchDocs", {
        query: arguments.query,
        limit: arguments.limit
    } )
    
    if ( !result.getSuccess() ) {
        return {
            error: true,
            message: result.getError()
        }
    }
    
    var results = result.getData().results
    
    return {
        error: false,
        items: results.map( r => {
            return {
                title: r.title,
                excerpt: r.excerpt
            }
        } )
    }
}
```
</details>

---

## Part 5: Response Structure

### Understanding MCPResponse

Every MCP method returns an `MCPResponse` object:

```java
response = client.send( "tool", {} )

// Get properties
success = response.getSuccess()       // Boolean: true/false
data = response.getData()            // Any: response data
error = response.getError()          // String: error message
statusCode = response.getStatusCode() // Numeric: HTTP status
headers = response.getHeaders()      // Struct: response headers

// Convert to struct
struct = response.toStruct()
```

### Always Check Success

```java
result = client.send( "searchDocs", { query: "test" } )

if ( result.getSuccess() ) {
    // Process successful response
    processData( result.getData() )
} else {
    // Handle error
    writeLog( "Error: #result.getError()#" )
    writeLog( "Status: #result.getStatusCode()#" )
}
```

---

## Part 6: Error Handling

### Network Errors

```java
// Server is unreachable
result = MCP( "http://invalid-host:9999" )
    .withTimeout( 1000 )
    .listTools()

if ( !result.getSuccess() ) {
    // statusCode will be 0 for network errors
    writeOutput( "Cannot connect: " & result.getError() )
}
```

### HTTP Errors

```java
// Server returns 4xx or 5xx
result = MCP( "http://localhost:3000" )
    .send( "unknownTool", {} )

if ( !result.getSuccess() ) {
    // statusCode will be the HTTP error code
    writeOutput( "HTTP #result.getStatusCode()#: #result.getError()#" )
}
```

### Try-Catch Pattern

```java
try {
    client = MCP( "http://localhost:3000" )
    result = client.send( "tool", { param: "value" } )
    
    if ( !result.getSuccess() ) {
        throw( 
            type: "MCPError",
            message: result.getError()
        )
    }
    
    return result.getData()
    
} catch ( any e ) {
    writeLog( "MCP failed: #e.message#" )
    return { error: true }
}
```

---

## Part 7: Integrating MCP with AI

### RAG Pattern: Retrieval Augmented Generation

```java
function aiWithContext( required string question ) {
    // 1. Search docs via MCP
    var mcpClient = MCP( "http://localhost:3000" )
    var searchResult = mcpClient.send( "searchDocs", {
        query: arguments.question
    } )
    
    if ( !searchResult.getSuccess() ) {
        return aiChat( arguments.question )
    }
    
    // 2. Extract relevant content
    var docs = searchResult.getData().results
    var context = docs.map( d => d.content ).toList( chr(10) & chr(10) )
    
    // 3. Ask AI with context
    var answer = aiChat( [
        aiMessage().system( "Answer using this context: #context#" ),
        aiMessage().user( arguments.question )
    ] )
    
    return answer
}

// Usage
answer = aiWithContext( "How do I install BoxLang?" )
writeOutput( answer )
```

### Dynamic Tool Creation

```java
// Convert MCP tools to AI tools automatically
function createAIToolFromMCP( required string mcpURL, required string toolName ) {
    var mcpClient = MCP( arguments.mcpURL )
    
    // Get tool definition
    var tools = mcpClient.listTools()
    var toolDef = tools.getData().find( t => t.name == arguments.toolName )
    
    // Create AI tool that uses MCP
    return aiTool(
        name: toolDef.name,
        description: toolDef.description,
        parameters: toolDef.parameters,
        callback: ( args ) => {
            var result = mcpClient.send( arguments.toolName, args )
            return result.getSuccess() ? result.getData() : { error: result.getError() }
        }
    )
}

// Usage
searchTool = createAIToolFromMCP( "http://localhost:3000", "searchDocs" )

answer = aiChat(
    "Find documentation about variables",
    { tools: [ searchTool ] }
)
```

---

## Part 8: Real-World Project

### Build a Documentation Assistant

Let's build a complete documentation assistant that uses MCP:

```java
component {
    property name="mcpClient";
    property name="cache";
    
    function init( required string mcpURL ) {
        variables.mcpClient = MCP( arguments.mcpURL )
            .withTimeout( 10000 )
            .onError( logError )
        
        variables.cache = {}
        return this
    }
    
    /**
     * Search documentation
     */
    function search( required string query, numeric limit = 10 ) {
        var result = variables.mcpClient.send( "searchDocs", {
            query: arguments.query,
            limit: arguments.limit
        } )
        
        if ( !result.getSuccess() ) {
            return []
        }
        
        return result.getData().results
    }
    
    /**
     * Get cached or fetch resource
     */
    function getResource( required string uri ) {
        // Check cache
        if ( structKeyExists( variables.cache, arguments.uri ) ) {
            return variables.cache[ arguments.uri ]
        }
        
        // Fetch from MCP
        var result = variables.mcpClient.readResource( arguments.uri )
        
        if ( !result.getSuccess() ) {
            throw( "Failed to read resource: " & result.getError() )
        }
        
        // Cache and return
        var content = result.getData().content
        variables.cache[ arguments.uri ] = content
        return content
    }
    
    /**
     * Ask AI with context from docs
     */
    function ask( required string question ) {
        // Search for relevant docs
        var docs = search( arguments.question, 3 )
        
        if ( docs.isEmpty() ) {
            return aiChat( arguments.question )
        }
        
        // Build context from docs
        var context = ""
        for ( var doc in docs ) {
            var content = getResource( doc.uri )
            context &= "### #doc.title#" & chr(10)
            context &= content & chr(10) & chr(10)
        }
        
        // Ask AI with context
        return aiChat( [
            aiMessage().system( "You are a helpful documentation assistant. Answer using the provided context." ),
            aiMessage().system( "Context:#chr(10)##context#" ),
            aiMessage().user( arguments.question )
        ] )
    }
    
    /**
     * Log errors
     */
    private function logError( required any response ) {
        writeLog(
            type: "error",
            text: "MCP Error [#response.getStatusCode()#]: #response.getError()#"
        )
    }
}

// Usage
assistant = new DocsAssistant( "http://localhost:3000" )

// Search docs
results = assistant.search( "installation" )
writeOutput( "Found #results.len()# results<br>" )

// Get specific resource
content = assistant.getResource( "docs://getting-started.md" )
writeOutput( "<pre>#encodeForHTML( content )#</pre>" )

// Ask AI with context
answer = assistant.ask( "How do I configure BoxLang AI?" )
writeOutput( "<p>#answer#</p>" )
```

---

## Part 9: Best Practices

### 1. Use Configuration Objects

```java
// Reusable configuration
function createMCPClient( string env = "production" ) {
    var config = {
        development: {
            url: "http://localhost:3000",
            timeout: 5000,
            token: ""
        },
        production: {
            url: getSystemSetting( "MCP_URL" ),
            timeout: 30000,
            token: getSystemSetting( "MCP_TOKEN" )
        }
    }
    
    var settings = config[ arguments.env ]
    
    return MCP( settings.url )
        .withTimeout( settings.timeout )
        .withBearerToken( settings.token )
}
```

### 2. Cache Responses

```java
component {
    property name="cache" default={};
    
    function getCachedTool( required string tool, required struct args ) {
        var cacheKey = arguments.tool & "_" & hash( serializeJSON( arguments.args ) )
        
        if ( structKeyExists( variables.cache, cacheKey ) ) {
            return variables.cache[ cacheKey ]
        }
        
        var result = mcpClient.send( arguments.tool, arguments.args )
        
        if ( result.getSuccess() ) {
            variables.cache[ cacheKey ] = result.getData()
        }
        
        return result
    }
}
```

### 3. Secure Credentials

```java
// Never hardcode tokens
// ❌ Bad
client = MCP( "http://api.example.com" )
    .withBearerToken( "hardcoded-token" )

// ✅ Good
client = MCP( "http://api.example.com" )
    .withBearerToken( getSystemSetting( "MCP_TOKEN" ) )
```

---

## Exercises

### Exercise 1: Basic MCP Client

Create an MCP client that:
1. Connects to a test server
2. Lists all available tools
3. Displays tool names and descriptions

### Exercise 2: Tool Invocation

Build a function that:
1. Takes a search query as input
2. Uses MCP to search documentation
3. Returns formatted results

### Exercise 3: Error Handling

Create a robust MCP wrapper that:
1. Tries multiple servers (fallback)
2. Retries on timeout
3. Logs all errors

### Exercise 4: AI Integration

Build a chatbot that:
1. Uses MCP to search docs
2. Feeds context to AI
3. Answers questions accurately

---

## Summary

You've learned:

- ✅ What MCP is and why it's useful
- ✅ How to create and configure MCP clients
- ✅ Discovering tools, resources, and prompts
- ✅ Executing operations and handling responses
- ✅ Integrating MCP with AI chat
- ✅ Error handling and best practices
- ✅ Building real-world applications

## Next Steps

- Explore MCP server implementations
- Build custom MCP servers
- Integrate MCP into production apps
- Check out [MCP specification](https://modelcontextprotocol.io)

## Additional Resources

- [MCP Client Documentation](../../docs/advanced/mcp-client.md)
- [BoxLang AI Documentation](../../docs/README.md)
- [Model Context Protocol](https://modelcontextprotocol.io)

---

**Duration:** 90 minutes  
**Difficulty:** Intermediate  
**Prerequisites:** Lessons 1-6

Happy coding! 🚀
