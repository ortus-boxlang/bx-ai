---
description: Learn how to get started with the BoxLang AI module, including installation, basic usage, and key features.
icon: rocket
---

# Getting Started

Welcome to the BoxLang AI Module! This section will help you get up and running quickly with AI-powered features in your BoxLang applications.

## What is BoxLang AI?

BoxLang AI (`bx-ai`) is a comprehensive AI integration module for BoxLang that provides:

- **🤖 Multi-Provider Support** - OpenAI, Claude, Gemini, Ollama, Grok, DeepSeek, Perplexity, and more
- **💬 Simple Chat Interface** - Start with one-line AI conversations
- **🔄 Composable Pipelines** - Build complex AI workflows by chaining operations
- **🧠 Intelligent Agents** - Create autonomous agents with memory and tools
- **📊 Structured Output** - Extract data into classes, structs, or arrays
- **🎙️ Multimodal Content** - Process images, audio, video, and documents
- **🛠️ Real-Time Tools** - Enable AI to call functions and APIs
- **💭 Memory Systems** - Maintain conversation context across interactions
- **📡 Streaming Support** - Real-time response streaming for better UX

## Quick Navigation

### 🚀 New to BoxLang AI?

**[Installation Guide](installation.md)**
Get the module installed and configured in minutes.

**[Quickstart Tutorial](quickstart.md)**
Your first AI conversation in 5 lines of code.

**[AI Agents Quick Start](agent-quickstart.md)**
Build your first autonomous agent with tools and memory.

---

### 📚 Learning Path

We recommend following this path to master BoxLang AI:

#### 1. **Installation** (5 minutes)

Install the module and configure your API keys.
→ [Installation Guide](installation.md)

#### 2. **First Chat** (10 minutes)

Learn basic chat interactions and message formatting.
→ [Quickstart Tutorial](quickstart.md)

#### 3. **Advanced Features** (20 minutes)

Explore streaming, tools, structured output, and multimodal content.
→ [Basic Chatting](../chatting/basic-chatting.md)
→ [Advanced Chatting](../chatting/advanced-chatting.md)

#### 4. **AI Agents** (30 minutes)

Build autonomous agents that reason, remember, and use tools.
→ [Agent Quickstart](agent-quickstart.md)
→ [Full Agent Documentation](../main-components/agents.md)

#### 5. **Pipelines & Advanced Topics** (45 minutes)

Master composable workflows and advanced patterns.
→ [Pipeline Overview](../main-components/overview.md)
→ [Memory Systems](../main-components/memory.md)

---

## Key Concepts

### Simple Interactions

For quick AI tasks, use the Built-in Functions (BIFs):

```java
// One-line chat
result = aiChat( "What is BoxLang?" );

// With specific provider
result = aiChat(
    provider: "claude",
    message: "Explain quantum computing",
    model: "claude-3-5-sonnet-20241022"
);
```

**Best for:** Quick questions, single interactions, simple automations

### AI Pipelines

For complex workflows, use composable pipelines:

```java
// Build reusable pipelines
var chatbot = aiModel( "openai" )
    .to( aiMessage().system( "You are a helpful assistant" ) )
    .to( aiTransform( data => data.toUpper() ) );

// Execute multiple times
result1 = chatbot.run( "Hello!" );
result2 = chatbot.run( "How are you?" );
```

**Best for:** Reusable workflows, complex processing, multi-step operations

### AI Agents

For autonomous behavior, use agents:

```java
// Create an agent with tools and memory
var agent = aiAgent()
    .withInstructions( "You are a research assistant" )
    .withTools( [ searchTool, calculatorTool ] )
    .withMemory( "windowed", { maxMessages: 20 } )
    .build();

// Agent remembers context and uses tools automatically
response = agent.run( "Find the latest BoxLang release" );
```

**Best for:** Context-aware conversations, autonomous tool use, complex reasoning

---

## Choose Your Starting Point

### 🎯 I want to...

**"Just make a simple AI call"**
→ Start with [Quickstart](quickstart.md), then [Basic Chatting](../chatting/basic-chatting.md)

**"Build a chatbot that remembers conversations"**
→ Go to [Agent Quickstart](agent-quickstart.md) and learn about [Memory Systems](../main-components/memory.md)

**"Let AI call my functions and APIs"**
→ Check out [Tools documentation](../main-components/overview.md#tools) and [Agent Tools](../main-components/agents.md#tools)

**"Extract structured data from text"**
→ See [Structured Output](../main-components/structured-output.md) and [Advanced Chatting](../chatting/advanced-chatting.md#structured-output)

**"Process images, audio, or documents"**
→ Read [Multimodal Content](../chatting/advanced-chatting.md#multimodal-content)

**"Build complex AI workflows"**
→ Start with [Pipeline Overview](../main-components/overview.md) and [Transformers](../main-components/transformers.md)

**"Use AI locally without API keys"**
→ Install [Ollama](installation.md#ollama-setup) and use local models

---

## Example: From Simple to Advanced

### Level 1: Simple Chat

```java
result = aiChat( "Tell me a joke" );
println( result ); // prints the joke
```

### Level 2: Structured Output

```java
result = aiChat(
    message: "Extract: John is 30 and works as a developer in NYC",
    structured: {
        name: "string",
        age: "numeric",
        job: "string",
        location: "string"
    }
);
println( result.name ); // "John"
```

### Level 3: Agent with Tools

```java
var agent = aiAgent()
    .withInstructions( "Help users with calculations and weather" )
    .withTools( [ calculatorTool, weatherTool ] )
    .build();

response = agent.run( "What's 15% of 230, and what's the weather in Boston?" );
// Agent automatically calls both tools and synthesizes the answer
```

### Level 4: Custom Pipeline

```java
var pipeline = aiModel( "openai" )
    .to( aiMessage().system( "You are a JSON formatter" ) )
    .to( aiTransform( data => {
        return deserializeJSON( data );
    } ) )
    .to( aiTransform( data => {
        return data.filter( ( k, v ) => !isNull( v ) );
    } ) );

cleaned = pipeline.run( "Convert this to JSON: Name: Alice, Age: 25, City: null" );
```

---

## Common Use Cases

### Customer Support Bot

- **Components**: Agent + Memory + Tools
- **Guide**: [Agent Quickstart](agent-quickstart.md)

### Data Extraction

- **Components**: Structured Output + Batch Processing
- **Guide**: [Structured Output](../main-components/structured-output.md)

### Content Generation

- **Components**: Message Templates + Streaming
- **Guide**: [Message Templates](../main-components/messages.md)

### Document Analysis

- **Components**: Multimodal + Structured Output
- **Guide**: [Advanced Chatting](../chatting/advanced-chatting.md#multimodal-content)

### Research Assistant

- **Components**: Agent + Tools + Memory + MCP
- **Guide**: [MCP Client](../advanced/mcp-client.md)
