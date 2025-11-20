# AI Agents

AI Agents are autonomous entities that can reason, use tools, and maintain conversation memory. Inspired by LangChain agents but "Boxified" for simplicity and productivity, agents handle complex AI workflows by automatically managing state, context, and tool execution.

## What are AI Agents?

An agent is more than a simple chat interface - it's an intelligent entity that:

- **Maintains Memory**: Remembers conversation history across interactions
- **Uses Tools**: Can call functions to access data, perform calculations, or interact with systems
- **Reasons and Plans**: Determines when and how to use tools to accomplish tasks
- **Manages State**: Automatically handles message history and context
- **Integrates with Pipelines**: Works seamlessly in BoxLang AI pipelines

## Creating Agents

### Basic Agent

```java
// Simple agent with default settings
agent = aiAgent(
    name: "Assistant",
    description: "A helpful AI assistant",
    instructions: "Be concise and friendly"
)

response = agent.run( "What is BoxLang?" )
println( response )
```

### Agent with Custom Model

```java
// Agent with specific AI model
model = aiModel( "claude" ).configure( apiKey: "sk-..." )

agent = aiAgent(
    name: "Claude Assistant",
    model: model,
    params: { temperature: 0.7 }
)
```

### Agent with Tools

Tools enable agents to perform real-world actions:

```java
// Create tools
weatherTool = aiTool(
    "get_weather",
    "Get current weather for a location",
    location => {
        // Call weather API
        return getWeatherData( location )
    }
).describeLocation( "City and country, e.g. Boston, MA" )

calculatorTool = aiTool(
    "calculate",
    "Perform mathematical calculations",
    expression => evaluate( expression )
).describeExpression( "Math expression to evaluate" )

// Create agent with tools
agent = aiAgent(
    name: "TaskAgent",
    description: "An agent that can check weather and do math",
    instructions: "Use tools when needed. Be precise and helpful.",
    tools: [ weatherTool, calculatorTool ]
)

// Agent automatically uses tools when needed
response = agent.run( "What's the weather in Boston and what's 15% of 250?" )
```

## Memory Management

Agents automatically maintain conversation history:

### Simple Memory (Default)

```java
// Agent with conversation memory
agent = aiAgent(
    name: "ChatBot",
    description: "A conversational assistant",
    memory: aiMemory( "simple" )
)

// First interaction
agent.run( "My name is Luis" )
// Response: "Nice to meet you, Luis!"

// Second interaction - agent remembers
agent.run( "What's my name?" )
// Response: "Your name is Luis"

// Access memory messages
messages = agent.getMemoryMessages()
println( messages )  // All conversation history

// Clear memory when needed
agent.clearMemory()
```

### Multiple Memory Systems

Agents can use multiple memory instances:

```java
agent = aiAgent(
    name: "MultiMemoryAgent",
    memory: [
        aiMemory( "simple" ),      // Conversation history
        customMemory               // Custom memory implementation
    ]
)

// Agent stores in all memory systems
agent.run( "Remember this fact: BoxLang is awesome" )
```

## Fluent API

Build and configure agents with a fluent interface:

```java
agent = aiAgent()
    .setName( "CodeReviewer" )
    .setDescription( "A code review specialist" )
    .setInstructions( "Review code for best practices, security, and performance" )
    .setModel( aiModel( "openai" ) )
    .addTool( lintTool )
    .addTool( securityTool )
    .addMemory( aiMemory( "simple" ) )
    .setParam( "temperature", 0.3 )
    .setParam( "max_tokens", 1000 )

response = agent.run( "Review this function: ${codeSnippet}" )
```

## Return Formats

Agents support three return formats:

### Single (Default)

Returns just the assistant's content as a string:

```java
content = agent.run( "Hello", {}, { returnFormat: "single" } )
println( content )  // "Hello! How can I help you?"
```

### All Messages

Returns all messages including system, memory context, and response:

```java
allMessages = agent.run( "Hello", {}, { returnFormat: "all" } )
// Returns array:
// [
//   { role: "system", content: "..." },
//   { role: "user", content: "Previous message" },
//   { role: "assistant", content: "Previous response" },
//   { role: "user", content: "Hello" },
//   { role: "assistant", content: "Hello! How can I help you?" }
// ]
```

### Raw Response

Returns the full provider response structure:

```java
rawResponse = agent.run( "Hello", {}, { returnFormat: "raw" } )
// Returns complete OpenAI/Claude/etc response with metadata
println( rawResponse.usage.total_tokens )  // Token count
println( rawResponse.model )                // Model used
```

## Streaming Responses

Stream agent responses in real-time:

```java
agent = aiAgent(
    name: "StreamAgent",
    description: "Streaming assistant"
)

// Stream with callback
agent.stream(
    onChunk: ( chunk ) => {
        // Process each chunk
        content = chunk.choices?.first()?.delta?.content ?: ""
        print( content )
    },
    input: "Write a story about BoxLang"
)
```

## Pipeline Integration

Agents implement `IAiRunnable`, so they work in pipelines:

```java
// Agent in a pipeline
pipeline = aiMessage()
    .user( "Task: ${task}" )
    .to( agent )
    .transform( r => r.toUpper() )

result = pipeline.run( { task: "Summarize AI trends in 2025" } )
```

### Chaining Agents

```java
// Multiple agents in sequence
researchAgent = aiAgent( name: "Researcher" )
summaryAgent = aiAgent( name: "Summarizer" )
editorAgent = aiAgent( name: "Editor" )

pipeline = aiMessage()
    .user( "Research: ${topic}" )
    .to( researchAgent )
    .transform( r => "Summarize this: ${r}" )
    .to( summaryAgent )
    .transform( r => "Edit and polish: ${r}" )
    .to( editorAgent )

result = pipeline.run( { topic: "Quantum Computing" } )
```

## Advanced Patterns

### Agent with Dynamic Tools

```java
// Function that returns tools based on context
function getToolsForUser( userRole ) {
    if ( userRole == "admin" ) {
        return [ adminTool, userTool, reportTool ]
    }
    return [ userTool ]
}

// Create agent with dynamic tools
agent = aiAgent( name: "ContextAgent" )
    .setTools( getToolsForUser( getCurrentUserRole() ) )
```

### Agent Configuration Object

```java
// Get agent configuration
agent = aiAgent( name: "Inspector" )
config = agent.getConfig()

println( config.name )          // "Inspector"
println( config.toolCount )     // Number of tools
println( config.memoryCount )   // Number of memory systems
println( config.model )         // Model name
```

### Conditional Agent Execution

```java
// Execute agent based on conditions
function processRequest( userInput, requiresTools ) {
    if ( requiresTools ) {
        agent = aiAgent(
            name: "ToolAgent",
            tools: [ weatherTool, calculatorTool ]
        )
    } else {
        agent = aiAgent( name: "SimpleAgent" )
    }

    return agent.run( userInput )
}
```

## Event Interception

Agents fire events during execution:

```java
// Listen to agent events
interceptor = {
    beforeAIAgentRun: function( data ) {
        writeLog( "Agent ${data.agent.getName()} starting with: ${data.input}" )
    },
    afterAIAgentRun: function( data ) {
        writeLog( "Agent completed. Response: ${data.response}" )
    }
}

// Register interceptor
BoxRegisterInterceptor( interceptor )

// Run agent - events will fire
agent.run( "Hello" )
```

## Best Practices

### 1. Provide Clear Instructions

```java
// Good: Specific instructions
agent = aiAgent(
    name: "SupportAgent",
    description: "Customer support specialist",
    instructions: """
        You are a friendly customer support agent.
        - Always be polite and professional
        - Ask clarifying questions when needed
        - Provide step-by-step solutions
        - Use tools to look up order information
        - Escalate complex issues to human agents
    """
)
```

### 2. Choose Appropriate Tools

```java
// Only add tools the agent actually needs
agent = aiAgent(
    name: "WeatherAgent",
    tools: [ weatherTool ]  // Don't add unrelated tools
)
```

### 3. Manage Memory Lifecycle

```java
// Clear memory at appropriate times
agent = aiAgent( name: "SessionAgent", memory: aiMemory( "simple" ) )

// Process request
agent.run( "Help me with task X" )

// When session ends or topic changes
agent.clearMemory()
```

### 4. Set Appropriate Parameters

```java
// For creative tasks: higher temperature
creativeAgent = aiAgent(
    name: "Writer",
    params: { temperature: 0.8, max_tokens: 1000 }
)

// For factual tasks: lower temperature
factualAgent = aiAgent(
    name: "Analyzer",
    params: { temperature: 0.2, max_tokens: 500 }
)
```

### 5. Handle Errors Gracefully

```java
try {
    response = agent.run( userInput )
} catch ( any e ) {
    writeLog( "Agent error: ${e.message}", "error" )
    // Fallback logic
    response = "I encountered an error. Please try again."
}
```

## Real-World Examples

### Customer Support Agent

```java
lookupOrderTool = aiTool(
    "lookup_order",
    "Look up order details by order ID",
    orderId => getOrderDetails( orderId )
).describeOrderId( "The order ID to look up" )

checkInventoryTool = aiTool(
    "check_inventory",
    "Check product inventory",
    productId => getInventory( productId )
).describeProductId( "The product ID to check" )

supportAgent = aiAgent(
    name: "SupportBot",
    description: "Customer support specialist",
    instructions: "Help customers with orders and inventory questions. Be friendly and efficient.",
    tools: [ lookupOrderTool, checkInventoryTool ],
    memory: aiMemory( "simple" )
)

// Customer interaction
response = supportAgent.run( "What's the status of order #12345?" )
// Agent uses lookup_order tool and responds with order details
```

### Code Review Agent

```java
lintTool = aiTool(
    "lint_code",
    "Run linter on code",
    code => runLinter( code )
).describeCode( "The code to lint" )

testTool = aiTool(
    "run_tests",
    "Run unit tests",
    testFile => runTests( testFile )
).describeTestFile( "Path to test file" )

reviewAgent = aiAgent(
    name: "CodeReviewer",
    description: "Expert code reviewer",
    instructions: """
        Review code for:
        - Best practices
        - Security vulnerabilities
        - Performance issues
        - Test coverage
        Use lint_code and run_tests tools to validate code quality.
    """,
    tools: [ lintTool, testTool ],
    params: { temperature: 0.3 }
)

review = reviewAgent.run( "Review this code: ${codeSnippet}" )
```

### Research Assistant

```java
searchTool = aiTool(
    "search_web",
    "Search the web for information",
    query => performWebSearch( query )
).describeQuery( "Search query" )

researchAgent = aiAgent(
    name: "Researcher",
    description: "Research assistant",
    instructions: "Research topics thoroughly. Use web search when needed. Cite sources.",
    tools: [ searchTool ],
    memory: aiMemory( "simple" )
)

// Multi-turn research conversation
researchAgent.run( "Research quantum computing trends" )
researchAgent.run( "What are the top 3 companies in this space?" )
researchAgent.run( "Compare their approaches" )

// Get full conversation
conversation = researchAgent.getMemoryMessages()
```

## Next Steps

- Explore [Memory Systems](../advanced/memory.md) for custom memory implementations
- Learn about [Tools](./tools.md) for function calling patterns
- See [Events](../advanced/events.md) for agent event handling
- Check [Pipeline Patterns](./overview.md) for advanced agent workflows
