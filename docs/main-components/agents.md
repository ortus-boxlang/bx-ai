---
description: "The complete guide to AI Agents in BoxLang, covering creation, memory management, tool usage, configuration, and advanced patterns."
icon: robot
---

# AI Agents

AI Agents are autonomous entities that can reason, use tools, and maintain conversation memory. Inspired by LangChain agents but "Boxified" for simplicity and productivity, agents handle complex AI workflows by automatically managing state, context, and tool execution.

## What are AI Agents?

An agent is more than a simple chat interface - it's an intelligent entity that:

- **Maintains Memory**: Remembers conversation history across interactions
- **Uses Tools**: Can call functions to access data, perform calculations, or interact with systems
- **Reasons and Plans**: Determines when and how to use tools to accomplish tasks
- **Manages State**: Automatically handles message history and context
- **Integrates with Pipelines**: Works seamlessly in BoxLang AI pipelines
- **Delegates to Sub-Agents**: Can orchestrate specialized sub-agents for complex tasks

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

### Window Memory (Default)

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

## Configuration

### Constructor-Based Configuration

Agents are configured primarily through the constructor:

```java
agent = aiAgent(
    name: "CodeReviewer",
    description: "A code review specialist",
    instructions: "Review code for best practices, security, and performance",
    model: aiModel( "openai" ),
    tools: [ lintTool, securityTool ],
    memory: aiMemory( "simple" ),
    params: { temperature: 0.3, max_tokens: 1000 }
)

response = agent.run( "Review this function: ${codeSnippet}" )
```

### Fluent Configuration

For runtime configuration changes, use setter methods:

```java
agent = aiAgent(
    name: "Assistant",
    description: "Helpful assistant"
)
    .setModel( aiModel( "claude" ) )
    .addTool( searchTool )
    .addMemory( customMemory )
    .setParam( "temperature", 0.7 )

response = agent.run( "Help me with this task" )
```

## Return Formats

Agents support five return formats: `single`, `all`, `json`, `xml`, and `raw`.

### Single (Default)

Agents default to "single" format, returning just the assistant's content as a string:

```java
// Default behavior - returns string
content = agent.run( "Hello" )
println( content )  // "Hello! How can I help you?"

// Explicitly specify (same result)
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

### JSON Format

```java
jsonResponse = agent.run( "Hello", {}, { returnFormat: "json" } )
// Returns response as JSON string
println( jsonResponse )  // JSON formatted string
```

### XML Format

```java
xmlResponse = agent.run( "Hello", {}, { returnFormat: "xml" } )
// Returns response as XML string
println( xmlResponse )  // XML formatted string
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

### Agent Introspection

Inspect agent configuration at runtime using `getConfig()`:

```java
// Create an agent
agent = aiAgent(
    name: "Inspector",
    description: "Analysis agent",
    instructions: "Analyze data carefully",
    model: aiModel( "openai", { temperature: 0.7 } ),
    tools: [ searchTool, calculatorTool ],
    params: { maxTokens: 2000 }
)

// Get comprehensive configuration
config = agent.getConfig()

// Access agent properties
println( config.name )          // "Inspector"
println( config.description )   // "Analysis agent"
println( config.instructions )  // "Analyze data carefully"

// Access model configuration object
println( config.model.name )         // "gpt-4o-mini"
println( config.model.provider )     // "openai"
println( config.model.toolCount )    // 2
println( config.model.params.temperature )  // 0.7

// Access memories (array of memory summaries)
config.memories.each( function( mem ) {
    println( mem.type )          // e.g., "SessionMemory"
    println( mem.messageCount )  // Number of stored messages
} )

// Access execution parameters
println( config.params.maxTokens )  // 2000
println( config.options.returnFormat )  // "single" (default)
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

## Sub-Agents

Sub-agents allow you to create specialized agents that can be delegated to by a parent agent. When you register a sub-agent, it is automatically wrapped as an internal tool that the parent agent can invoke.

### Creating Agents with Sub-Agents

```java
// Create specialized sub-agents
mathAgent = aiAgent(
    name: "MathAgent",
    description: "A mathematics expert",
    instructions: "You help with mathematical calculations and concepts"
)

codeAgent = aiAgent(
    name: "CodeAgent",
    description: "A programming expert",
    instructions: "You help with code review and writing"
)

// Create parent agent with sub-agents
mainAgent = aiAgent(
    name: "OrchestratorAgent",
    description: "Main coordinator that delegates to specialists",
    instructions: """
        Analyze each request and delegate to appropriate sub-agents:
        - MathAgent: For mathematical tasks
        - CodeAgent: For programming tasks
        Answer directly for simple queries.
    """,
    subAgents: [ mathAgent, codeAgent ]
)

// The parent agent automatically has delegation tools available
response = mainAgent.run( "Write a function to calculate factorial" )
```

### Fluent Sub-Agent API

You can also add sub-agents using the fluent API:

```java
// Create sub-agents
helperAgent = aiAgent( name: "HelperAgent", description: "General helper" )
specialistAgent = aiAgent( name: "SpecialistAgent", description: "Specialist" )

// Add sub-agents fluently
mainAgent = aiAgent( name: "MainAgent" )
    .addSubAgent( helperAgent )
    .addSubAgent( specialistAgent )

// Or replace all sub-agents
mainAgent.setSubAgents( [ newAgent1, newAgent2 ] )
```

### Sub-Agent Management

```java
// Check if a sub-agent exists
if ( mainAgent.hasSubAgent( "MathAgent" ) ) {
    println( "Math agent is available" )
}

// Get a specific sub-agent
mathAgent = mainAgent.getSubAgent( "MathAgent" )
if ( !isNull( mathAgent ) ) {
    // Use the sub-agent directly
    result = mathAgent.run( "What is 2 + 2?" )
}

// Get all sub-agents
allSubAgents = mainAgent.getSubAgents()
println( "Total sub-agents: #allSubAgents.len()#" )
```

### Sub-Agents in Configuration

Sub-agent information is included in `getConfig()`:

```java
config = mainAgent.getConfig()

println( config.subAgentCount )  // Number of sub-agents

// Sub-agent details
config.subAgents.each( agent => {
    println( "Name: #agent.name#" )
    println( "Description: #agent.description#" )
} )
```

### How Sub-Agents Work

When you add a sub-agent, it is automatically converted to a tool:

1. **Tool Name**: `delegate_to_{agent_name}` (lowercase, special characters replaced with underscores)
2. **Tool Description**: Includes the sub-agent's name and description
3. **Tool Parameter**: A `task` parameter for the query to delegate

The parent agent's AI model decides when to use the delegation tool based on the task context.

```java
// Behind the scenes, adding a sub-agent creates a tool like:
// Tool name: "delegate_to_mathagent"
// Tool description: "Delegate a task to the 'MathAgent' sub-agent..."
// The tool calls: subAgent.run( task )
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
