# Lesson 8: MCP Servers & Clients

**⏱️ Duration: 60 minutes**

Welcome to Model Context Protocol (MCP)! In this final lesson, you'll learn how to build MCP servers that expose tools to AI clients, and MCP clients that connect to external MCP servers, enabling your AI applications to access remote capabilities.

## 🎯 What You'll Learn

- Understand Model Context Protocol (MCP)
- Create MCP clients to connect to servers
- Build your first MCP server
- Expose tools via MCP
- Choose between HTTP and STDIO transports
- Integrate MCP servers with AI agents

---

## 📚 Part 1: Understanding MCP (10 mins)

### What is Model Context Protocol?

MCP is a **standardized protocol** that allows AI applications to connect to external tools, data sources, and capabilities.

```
┌─────────────────────────────────────────────────────────────────┐
│                    MCP ARCHITECTURE                             │
└─────────────────────────────────────────────────────────────────┘

  ┌─────────────┐                              ┌─────────────┐
  │ AI Client   │                              │ MCP Server  │
  │ (Your App)  │                              │ (Remote)    │
  └──────┬──────┘                              └──────┬──────┘
         │                                            │
         │  1. "List available tools"                 │
         │───────────────────────────────────────────▶│
         │                                            │
         │  2. [weatherTool, stockTool, newsTool]     │
         │◀───────────────────────────────────────────│
         │                                            │
         │  3. "Call weatherTool(city: Boston)"       │
         │───────────────────────────────────────────▶│
         │                                            │
         │  4. { temp: 72, conditions: "sunny" }      │
         │◀───────────────────────────────────────────│
         │                                            │
         ▼                                            ▼


  💡 Think of MCP as "API standards for AI tools"
```

### Why MCP?

**Without MCP (Traditional APIs):**
```javascript
// Custom integration for each service
weatherAPI = new WeatherAPI( apiKey )
stockAPI = new StockAPI( apiKey )
newsAPI = new NewsAPI( apiKey )

// Different interfaces for each
weather = weatherAPI.getCurrentWeather( "Boston" )
stocks = stockAPI.getQuote( "AAPL" )
news = newsAPI.getHeadlines( "technology" )
```

**With MCP (Standardized Protocol):**
```javascript
// Uniform interface for all services
mcpClient = MCP( "http://multi-service-server.com" )

// Discover capabilities
tools = mcpClient.listTools()

// Call any tool the same way
weather = mcpClient.send( "weatherTool", { city: "Boston" } )
stocks = mcpClient.send( "stockTool", { symbol: "AAPL" } )
news = mcpClient.send( "newsTool", { category: "technology" } )
```

### MCP Components

```
┌─────────────────────────────────────────────────────────────────┐
│                     MCP CAPABILITIES                            │
└─────────────────────────────────────────────────────────────────┘

  ┌─────────────┐       ┌─────────────┐       ┌─────────────┐
  │   🔧 TOOLS  │       │ 📚 RESOURCES│       │ 💬 PROMPTS  │
  └─────────────┘       └─────────────┘       └─────────────┘
        │                      │                      │
        ▼                      ▼                      ▼
  Functions AI            Documents and           Reusable
  can invoke              data sources            templates


  Example: Weather Server
  ────────────────────────
  Tools:        getCurrentWeather(), getForecast()
  Resources:    Historical weather data, climate reports
  Prompts:      "Weather report template", "Forecast template"
```

### MCP vs Regular Tools

| Aspect | Regular Tools | MCP Tools |
|--------|--------------|-----------|
| **Location** | Local functions | Remote servers |
| **Discovery** | Hardcoded | Dynamic via `listTools()` |
| **Protocol** | Custom | Standardized JSON-RPC |
| **Sharing** | Code distribution | HTTP/STDIO endpoint |
| **Updates** | Redeploy app | Server updates automatically |

---

## 🔌 Part 2: MCP Clients (15 mins)

### Creating an MCP Client

```javascript
// Connect to an MCP server
client = MCP( "http://localhost:3000" )

// Discover available tools
tools = client.listTools()

// Call a tool
result = client.send( "toolName", { param: "value" } )
```

### Example 1: Connecting to an MCP Server

```javascript
// 01-mcp-client-basic.bxs

// Connect to a public MCP server (example)
client = MCP( "http://localhost:3000" )

// Discover what tools are available
println( "Discovering tools..." )
toolsResponse = client.listTools()

if ( toolsResponse.getSuccess() ) {
    tools = toolsResponse.getData()

    println( "Available tools: #arrayLen( tools )#" )

    for ( tool in tools ) {
        println( "- #tool.name#: #tool.description#" )
    }
} else {
    println( "Error: #toolsResponse.getError()#" )
}
```

### Client Configuration

```javascript
// Configure timeout (milliseconds)
client = MCP( "http://localhost:3000" )
    .withTimeout( 5000 )  // 5 second timeout

// Add authentication
client = MCP( "http://localhost:3000" )
    .withBearerToken( "your-api-token" )

// Or basic auth
client = MCP( "http://localhost:3000" )
    .withAuth( "username", "password" )

// Add custom headers
client = MCP( "http://localhost:3000" )
    .withHeaders( {
        "X-API-Key": "your-key",
        "X-Custom": "value"
    } )
```

### Example 2: Using MCP Tools

```javascript
// 02-mcp-client-call-tool.bxs

client = MCP( "http://localhost:3000" )
    .withTimeout( 10000 )

// Call a weather tool
response = client.send( "getWeather", {
    city: "Boston",
    units: "fahrenheit"
} )

if ( response.getSuccess() ) {
    weather = response.getData()
    println( "Weather in Boston:" )
    println( "Temperature: #weather.temperature#°F" )
    println( "Conditions: #weather.conditions#" )
} else {
    println( "Error: #response.getError()#" )
}
```

### Handling Responses

All MCP client methods return a response object:

```javascript
response = client.send( "toolName", args )

// Check if successful
if ( response.getSuccess() ) {
    data = response.getData()
    println( "Success: #data#" )
} else {
    println( "Error: #response.getError()#" )
    println( "Status: #response.getStatusCode()#" )
}
```

---

## 🌐 Part 3: Building an MCP Server (20 mins)

### Server Transports

MCP servers can use two transport types:

```
┌─────────────────────────────────────────────────────────────────┐
│                    TRANSPORT COMPARISON                         │
└─────────────────────────────────────────────────────────────────┘

  HTTP Transport                    STDIO Transport
  ──────────────                    ───────────────
  ✅ Web-based                      ✅ Command-line tools
  ✅ REST API style                 ✅ Desktop apps (Claude, VS Code)
  ✅ Browser clients                ✅ Process-to-process
  ✅ Remote access                  ✅ Local execution

  POST http://localhost/mcp.bxm    stdin → MCP Server → stdout
```

### Creating an MCP Server

```javascript
// Get or create an MCP server instance
mcpSrv = mcpServer( "myServer" )

// Register tools
mcpSrv.registerTool(
    aiTool()
        .name( "toolName" )
        .description( "What this tool does" )
        .arguments( { param: "string" } )
        .execute( ( args ) => {
            return "Tool result"
        } )
)
```

### Example 3: Your First MCP Server

```javascript
// 03-first-mcp-server.bxs

// Create server
mcpSrv = mcpServer( "mathServer" )

// Register a calculator tool
mcpSrv.registerTool(
    aiTool()
        .name( "calculate" )
        .description( "Performs basic math operations" )
        .arguments( {
            operation: "string - add, subtract, multiply, divide",
            a: "numeric - first number",
            b: "numeric - second number"
        } )
        .execute( ( args ) => {
            switch ( args.operation ) {
                case "add":
                    return args.a + args.b
                case "subtract":
                    return args.a - args.b
                case "multiply":
                    return args.a * args.b
                case "divide":
                    return args.a / args.b
                default:
                    throw( message: "Unknown operation: #args.operation#" )
            }
        } )
)

println( "✅ Math MCP Server registered" )
println( "Tools available: #arrayLen( mcpSrv.getTools() )#" )

// List tools
for ( tool in mcpSrv.getTools() ) {
    println( "- #tool.name#: #tool.description#" )
}
```

### HTTP Endpoint

To expose your MCP server via HTTP, create an endpoint file:

```javascript
// www/mcp.bxm
<bx:script>
import bxModules.bxai.models.mcp.MCPRequestProcessor

// Handle the HTTP request
MCPRequestProcessor::startHttp()
</bx:script>
```

**Access your server:**
```bash
# List tools
POST http://localhost/mcp.bxm
{ "method": "tools/list" }

# Call a tool
POST http://localhost/mcp.bxm
{
    "method": "tools/call",
    "params": {
        "name": "calculate",
        "arguments": { "operation": "add", "a": 5, "b": 3 }
    }
}
```

### Example 4: Weather MCP Server

```javascript
// 04-weather-mcp-server.bxs

mcpSrv = mcpServer( "weatherServer" )

// Mock weather data (in real app, call API)
weatherData = {
    "Boston": { temp: 72, conditions: "Sunny", humidity: 65 },
    "New York": { temp: 68, conditions: "Cloudy", humidity: 70 },
    "Miami": { temp: 85, conditions: "Partly Cloudy", humidity: 80 }
}

// Register weather tool
mcpSrv.registerTool(
    aiTool()
        .name( "getWeather" )
        .description( "Get current weather for a city" )
        .arguments( {
            city: "string - The city name (Boston, New York, Miami)"
        } )
        .execute( ( args ) => {
            if ( structKeyExists( weatherData, args.city ) ) {
                return weatherData[ args.city ]
            } else {
                return { error: "City not found. Available cities: Boston, New York, Miami" }
            }
        } )
)

// Register forecast tool
mcpSrv.registerTool(
    aiTool()
        .name( "getForecast" )
        .description( "Get 3-day weather forecast" )
        .arguments( {
            city: "string - The city name"
        } )
        .execute( ( args ) => {
            return [
                { day: "Today", high: 75, low: 60, conditions: "Sunny" },
                { day: "Tomorrow", high: 72, low: 58, conditions: "Cloudy" },
                { day: "Day 3", high: 70, low: 55, conditions: "Rain" }
            ]
        } )
)

println( "✅ Weather MCP Server ready" )
println( "Registered tools:" )
for ( tool in mcpSrv.getTools() ) {
    println( "- #tool.name#" )
}
```

### 🧪 Lab 1: Build an MCP Server (10 mins)

**Task:** Create an MCP server with 2-3 custom tools.

**Ideas:**
- **String Tools** - `reverseString()`, `countWords()`, `toUpperCase()`
- **Data Tools** - `convertUnits()`, `formatDate()`, `validateEmail()`
- **Utility Tools** - `generateUUID()`, `hashString()`, `randomNumber()`

**Requirements:**
1. Create `mcpServer()` with a name
2. Register at least 2 tools with `registerTool()`
3. Each tool should have clear description and arguments
4. Test locally by calling tools directly

**Starter code in `labs/lab-01-build-mcp-server.bxs`**

---

## 🤖 Part 4: Integrating MCP with Agents (10 mins)

### MCP Tools in AI Agents

MCP tools can be used with AI agents just like regular tools:

```javascript
// Option 1: Auto-discover tools from MCP server
agent = aiAgent()
    .withInstructions( "You are a helpful assistant" )
    .withMCPServer( "http://localhost:3000" )
    .build()

// Option 2: Manually add specific MCP tools
mcpClient = MCP( "http://localhost:3000" )
tools = mcpClient.listTools().getData()

agent = aiAgent()
    .withInstructions( "You are a helpful assistant" )
    .withTools( tools )
    .build()
```

### Example 5: Agent with MCP Server

```javascript
// 05-agent-with-mcp.bxs

// Create MCP server with utility tools
mcpSrv = mcpServer( "utilities" )

mcpSrv.registerTool(
    aiTool()
        .name( "getCurrentTime" )
        .description( "Get the current time" )
        .execute( () => now() )
)

mcpSrv.registerTool(
    aiTool()
        .name( "generateUUID" )
        .description( "Generate a random UUID" )
        .execute( () => createUUID() )
)

// Get the tools
tools = mcpSrv.getTools()

// Create agent with MCP tools
agent = aiAgent()
    .withInstructions( "You are a helpful utility assistant. Use the available tools when needed." )
    .withTools( tools )
    .build()

// Test the agent
println( agent.run( "What time is it?" ) )
println( agent.run( "Generate a UUID for me" ) )
println( agent.run( "Generate 3 UUIDs and tell me the current time" ) )
```

---

## 🔐 Part 5: Production MCP Servers (5 mins)

### Security Best Practices

```javascript
// Add authentication to MCP server
mcpSrv = mcpServer( "secureServer", {
    requireAuth: true,
    apiKeys: [ "your-secret-key-1", "your-secret-key-2" ]
} )

// Require specific headers
mcpSrv.setAuthHandler( ( headers ) => {
    apiKey = headers[ "X-API-Key" ] ?: ""
    return arrayContains( getApiKeys(), apiKey )
} )
```

### Rate Limiting

```javascript
// Configure rate limits
mcpSrv = mcpServer( "limitedServer", {
    rateLimit: {
        maxRequests: 100,
        windowSeconds: 60
    }
} )
```

### CORS Configuration

```javascript
// Configure CORS for web clients
mcpSrv = mcpServer( "webServer", {
    cors: {
        allowOrigin: [ "https://myapp.com", "https://app.example.com" ],
        allowMethods: [ "POST", "OPTIONS" ],
        allowHeaders: [ "Content-Type", "Authorization" ]
    }
} )
```

---

## 🌍 Part 6: Real-World Example (5 mins)

### Example 6: Multi-Service MCP Server

```javascript
// 06-multi-service-mcp.bxs

mcpSrv = mcpServer( "multiService" )

// User service
mcpSrv.registerTool(
    aiTool()
        .name( "getUser" )
        .description( "Get user information by ID" )
        .arguments( { userId: "numeric - User ID" } )
        .execute( ( args ) => {
            // In real app, query database
            return {
                id: args.userId,
                name: "John Doe",
                email: "john@example.com",
                createdDate: "2024-01-15"
            }
        } )
)

// Order service
mcpSrv.registerTool(
    aiTool()
        .name( "getUserOrders" )
        .description( "Get user's order history" )
        .arguments( { userId: "numeric - User ID" } )
        .execute( ( args ) => {
            return [
                { orderId: 1001, total: 99.99, status: "Delivered" },
                { orderId: 1002, total: 149.99, status: "Shipped" }
            ]
        } )
)

// Analytics service
mcpSrv.registerTool(
    aiTool()
        .name( "getUserStats" )
        .description( "Get user statistics and metrics" )
        .arguments( { userId: "numeric - User ID" } )
        .execute( ( args ) => {
            return {
                totalOrders: 12,
                totalSpent: 1234.56,
                averageOrderValue: 102.88,
                lastLoginDate: "2024-12-15"
            }
        } )
)

println( "✅ Multi-Service MCP Server ready" )
println( "Registered #arrayLen( mcpSrv.getTools() )# tools" )

// Create agent that uses the MCP tools
agent = aiAgent()
    .withInstructions( "You are a customer service agent. Use the available tools to help customers." )
    .withTools( mcpSrv.getTools() )
    .build()

// Customer inquiry
println( "\n" & agent.run( "Tell me about user 12345 - their info, orders, and statistics" ) )
```

### 🧪 Lab 2: Complete MCP System (10 mins)

**Task:** Build a complete MCP server and connect it with an AI agent.

**Requirements:**
1. Create an MCP server with at least 3 related tools (e.g., product catalog, inventory, pricing)
2. Create an HTTP endpoint (`mcp.bxm`) to expose the server
3. Create an MCP client that connects to your server
4. Create an AI agent that uses your MCP tools
5. Test with complex multi-step queries

**Starter code in `labs/lab-02-complete-mcp-system.bxs`**

---

## 🎯 Summary

### What You Learned

✅ **MCP Basics** - Understanding Model Context Protocol
✅ **MCP Clients** - Connecting to and using MCP servers
✅ **MCP Servers** - Building and exposing tools via MCP
✅ **Transports** - HTTP and STDIO communication
✅ **Agent Integration** - Using MCP tools with AI agents
✅ **Production** - Security, rate limiting, CORS

### Key BIFs & Components

| Function | Purpose |
|----------|---------|
| `MCP()` | Create MCP client |
| `mcpServer()` | Create or get MCP server |
| `registerTool()` | Add tools to MCP server |
| `listTools()` | Discover server capabilities |
| `send()` | Call MCP server tools |

### MCP Benefits

- 🔌 **Standardization** - Uniform protocol for all tools
- 🌐 **Remote Access** - Tools anywhere, not just local
- 🔍 **Discovery** - Dynamic tool detection
- 🔄 **Updates** - Server updates without redeploying clients
- 🤝 **Sharing** - Easy tool distribution

---

## 📝 Resources

- [MCP Client Documentation](../../../docs/advanced/mcp-client.md)
- [MCP Server Documentation](../../../docs/advanced/mcp-server.md)
- [Official MCP Specification](https://modelcontextprotocol.io/)
- [BoxLang AI Full Documentation](../../../docs/README.md)

---

**🎉 Congratulations!** You've completed the BoxLang AI Bootcamp!

### 🚀 What's Next?

1. **Build Real Projects** - Apply what you've learned
2. **Explore Advanced Topics**:
   - Custom providers and transformers
   - Advanced RAG with vector memory
   - Pipeline workflows
   - Production deployment
3. **Join the Community** - Share your projects and get help
4. **Read Full Documentation** - Deep dive into every feature

---

**You now have the skills to build production-ready AI applications with BoxLang!** 🌟

👉 **[Back to Bootcamp Overview](../README.md)**
