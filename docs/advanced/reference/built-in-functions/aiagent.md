# aiAgent

Create an autonomous AI Agent that can reason, use tools, maintain memory, and execute multi-step tasks.

## 🔄 Agent Execution Flow

```mermaid
sequenceDiagram
    participant U as User
    participant A as Agent
    participant M as Memory
    participant AI as AI Model
    participant T as Tools

    U->>A: run(input)
    A->>M: Retrieve context
    M-->>A: Relevant history

    A->>AI: Send request + context + tools

    alt Tool Call Needed
        AI-->>A: Tool call request
        A->>T: Execute tool
        T-->>A: Tool result
        A->>AI: Send tool result
        AI-->>A: Final response
    else Direct Response
        AI-->>A: Direct response
    end

    A->>M: Store interaction
    A->>U: Return response

    style A fill:#BD10E0
    style M fill:#4A90E2
    style AI fill:#7ED321
    style T fill:#F5A623
```

## Syntax

```javascript
aiAgent(name, description, instructions, model, memory, tools, subAgents, params, options)
```

## Parameters

| Parameter      | Type    | Required | Default      | Description                                                     |
| -------------- | ------- | -------- | ------------ | --------------------------------------------------------------- |
| `name`         | string  | No       | `"BxAi"`     | The agent's name/identifier                                     |
| `description`  | string  | No       | `""`         | The agent's role or purpose description                         |
| `instructions` | string  | No       | `""`         | System instructions for agent behavior                          |
| `model`        | AiModel | No       | `aiModel()`  | The AI model provider to use                                    |
| `memory`       | any     | No       | `aiMemory()` | Single IAiMemory instance or array of memories                  |
| `tools`        | array   | No       | `[]`         | Array of Tool objects the agent can use                         |
| `subAgents`    | array   | No       | `[]`         | Array of sub-agents for task delegation                         |
| `params`       | struct  | No       | `{}`         | Additional provider parameters (temperature, max\_tokens, etc.) |
| `options`      | struct  | No       | `{}`         | Additional options (timeout, logging, etc.)                     |

## Returns

Returns an `AiAgent` instance with fluent API for:

* Running conversations: `run(input)`
* Streaming responses: `stream(callback, input)`
* Tool management: `addTool()`, `removeTool()`
* Memory management: `addMemory()`, `getMemory()`
* Pipeline integration: Agents are IAiRunnable

## Examples

### Basic Agent

```javascript
// Simple conversational agent
agent = aiAgent(
    name: "Assistant",
    description: "General purpose AI assistant"
);

response = agent.run( "What is BoxLang?" );
println( response );
```

### Agent with Instructions

```javascript
// Customer support agent
agent = aiAgent(
    name: "SupportBot",
    description: "Customer support specialist",
    instructions: "You are a friendly customer support agent. Always be helpful and polite. If you don't know something, say so."
);

response = agent.run( "How do I reset my password?" );
```

### Agent with Tools

```javascript
// Create tools
weatherTool = aiTool(
    "get_weather",
    "Get current weather for a location",
    ( location ) => {
        // Simulated weather lookup
        return "72°F and sunny in #location#";
    }
);

timeTool = aiTool(
    "get_time",
    "Get current time",
    () => dateTimeFormat( now(), "full" )
);

// Agent with tools
agent = aiAgent(
    name: "InfoBot",
    instructions: "You can check weather and time. Use tools when needed.",
    tools: [ weatherTool, timeTool ]
);

response = agent.run( "What's the weather in San Francisco?" );
// Agent automatically calls get_weather tool
```

### Agent with Memory

```javascript
// Create vector memory for RAG
vectorMemory = aiMemory( "chroma", {
    collection: "product_docs",
    embeddingProvider: "openai"
} );

// Ingest documentation
aiDocuments( "/docs/products", { type: "directory" } )
    .toMemory( vectorMemory );

// Agent with knowledge base
agent = aiAgent(
    name: "ProductExpert",
    description: "Product documentation assistant",
    instructions: "Answer questions using the provided documentation",
    memory: vectorMemory
);

response = agent.run( "How does the API authentication work?" );
// Agent retrieves relevant docs from memory
```

### Agent with Multiple Memories

```javascript
// Short-term conversation memory
chatMemory = aiMemory( "window", { maxMessages: 10 } );

// Long-term knowledge base
knowledgeMemory = aiMemory( "chroma", { collection: "kb" } );

// Agent with both memories
agent = aiAgent(
    name: "SmartAssistant",
    memory: [ chatMemory, knowledgeMemory ]
);

// Maintains conversation context + retrieves knowledge
agent.run( "Tell me about BoxLang" );
agent.run( "What else can you tell me?" ); // Uses chat history
```

### Sub-Agents (Delegation)

```javascript
// Create specialized sub-agents
researchAgent = aiAgent(
    name: "Researcher",
    description: "Searches and analyzes information",
    tools: [ searchTool, analyzeTool ]
);

writerAgent = aiAgent(
    name: "Writer",
    description: "Creates well-formatted content"
);

// Coordinator agent with sub-agents
coordinator = aiAgent(
    name: "Coordinator",
    description: "Delegates tasks to specialized agents",
    subAgents: [ researchAgent, writerAgent ]
);

// Coordinator decides which sub-agent to use
response = coordinator.run( "Research BoxLang and write a summary" );
```

### Streaming Agent

```javascript
agent = aiAgent(
    name: "StreamBot",
    description: "Streaming response agent"
);

// Stream response chunks
agent.stream(
    ( chunk ) => {
        write( chunk );
        flush;
    },
    "Write a long story about AI"
);
```

### Pipeline Integration

```javascript
// Agent in a pipeline
pipeline = aiMessage()
    .user( "Analyze this: ${data}" )
    .to( aiAgent(
        name: "Analyzer",
        instructions: "Analyze data and return insights"
    ) )
    .transform( response => response.ucase() );

result = pipeline.run({ data: "sales data..." });
```

### Custom Model and Parameters

```javascript
// Agent with specific model configuration
agent = aiAgent(
    name: "CreativeWriter",
    model: aiModel( "claude" ),
    params: {
        model: "claude-3-opus-20240229",
        temperature: 0.9,
        max_tokens: 2000
    }
);

response = agent.run( "Write a creative story" );
```

## Agent Capabilities

Agents are autonomous entities that can:

### 🧠 Reasoning

* Analyze complex problems
* Plan multi-step solutions
* Make decisions based on context

### 🔧 Tool Usage

* Automatically select and use tools
* Chain tool calls for complex tasks
* Handle tool results intelligently

### 💾 Memory

* Maintain conversation history
* Retrieve relevant context from vector memory
* Support multiple memory types simultaneously

### 🎯 Task Execution

* Execute multi-step workflows
* Retry on failures
* Track progress and state

### 🤝 Delegation

* Delegate tasks to specialized sub-agents
* Coordinate between multiple agents
* Combine capabilities

## Notes

* **Autonomous execution**: Agents decide when to use tools, search memory, or respond directly
* **Automatic memory search**: Vector memories are automatically searched for relevant context
* **Tool selection**: Agent analyzes task and selects appropriate tools
* **Sub-agent registration**: Sub-agents are automatically converted to callable tools
* **Conversation state**: Window/session memories maintain conversation context automatically
* **Event hooks**: Announces `onAIAgentCreate` event for custom logic

## Related Functions

* [`aiChat()`](aichat.md) - Simple chat without agent capabilities
* [`aiModel()`](aimodel.md) - Create model runnables
* [`aiMemory()`](aimemory.md) - Create memory instances
* [`aiTool()`](aitool.md) - Create tools for agents
* [`aiMessage()`](aimessage.md) - Build message structures

## Best Practices

```javascript
// ✅ Provide clear instructions
agent = aiAgent(
    instructions: "You are an expert in BoxLang. Provide code examples."
);

// ✅ Use descriptive tool names and descriptions
tool = aiTool(
    "search_documentation",
    "Search BoxLang documentation for specific topics",
    (query) => searchDocs(query)
);

// ✅ Combine conversation and vector memory
agent = aiAgent(
    memory: [
        aiMemory("window"),      // Short-term context
        aiMemory("chroma")       // Long-term knowledge
    ]
);

// ✅ Keep sub-agents focused
researchAgent = aiAgent(
    name: "Researcher",
    description: "Only searches and retrieves information"
);
```
