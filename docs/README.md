# ⚡︎ BoxLang AI Module Documentation

Welcome to the **BoxLang AI Module** - your gateway to integrating AI capabilities into BoxLang applications. This module provides a unified, easy-to-use API for interacting with multiple AI providers, from simple chat requests to complex multi-step pipelines.

## What is BoxLang AI?

BoxLang AI is a comprehensive module that brings artificial intelligence capabilities to the BoxLang ecosystem. Whether you're building chatbots, content generators, code assistants, or complex AI workflows, this module provides everything you need.

### Key Features

- **Multi-Provider Support**: Work with OpenAI, Claude, Gemini, Grok, Groq, DeepSeek, Ollama, and more
- **Unified API**: One consistent interface across all providers
- **Multi-Tenant Memory**: Enterprise-grade isolation with userId and conversationId across all 20 memory types
- **Multimodal Content**: Process images, audio, video, and documents alongside text
- **Local AI Support**: Run models locally with Ollama for privacy and offline use
- **AI Pipelines**: Chain operations together for complex multi-step workflows
- **Streaming Responses**: Get real-time responses as they're generated
- **Tool Integration**: Enable AI to call functions and access real-time data
- **Async Support**: Non-blocking operations for better performance
- **Template System**: Create reusable prompts with dynamic placeholders

### Supported Providers

| Provider | Type | Best For |
|----------|------|----------|
| **OpenAI** | Cloud | General purpose, GPT-4, GPT-3.5 |
| **Claude** | Cloud | Long context, detailed analysis |
| **Gemini** | Cloud | Google integration, multimodal |
| **Grok** | Cloud | Real-time data, Twitter integration |
| **HuggingFace** | Cloud | Open-source models, community-driven |
| **Groq** | Cloud | Ultra-fast inference, LPU architecture |
| **DeepSeek** | Cloud | Code generation, reasoning |
| **Ollama** | Local | Privacy, offline use, no API costs |
| **OpenRouter** | Gateway | Access multiple models through one API |
| **Perplexity** | Cloud | Research, citations, factual answers |
| **Voyage** | Cloud | State-of-the-art embeddings, specialized for RAG |
| **Cohere** | Cloud | Embeddings, multilingual, chat, tool calling |

### Use Cases

- **Chatbots**: Build conversational interfaces
- **Content Generation**: Create articles, documentation, marketing copy
- **Code Assistance**: Generate, review, and explain code
- **Data Analysis**: Extract insights from text and data
- **Document Processing**: Analyze PDFs, contracts, and reports
- **Media Analysis**: Process images, audio, and video content
- **Translations**: Multi-language content translation
- **Summarization**: Condense long documents
- **Question Answering**: Build knowledge bases and FAQs
- **Custom Workflows**: Multi-step AI processing pipelines

## Table of Contents

### Getting Started

📚 **[Installation & Configuration](getting-started/installation.md)**
Learn how to install the module, configure providers, and set up your first AI integration.

📚 **[Quick Start Guide](getting-started/quickstart.md)**
Get up and running in minutes with simple examples and your first AI chat.

🤖 **[AI Agents Quick Start](getting-started/agent-quickstart.md)**
Build autonomous AI agents with memory, tools, and reasoning capabilities.

---

### Simple AI Interactions

💬 **[Basic Chatting](chatting/basic-chatting.md)**
Simple question-answer interactions, parameters, and provider switching.

🎯 **[Advanced Chatting](chatting/advanced-chatting.md)**
Multi-message conversations, AI tools, async requests, and streaming responses.

⚙️ **[Service-Level Chatting](chatting/service-chatting.md)**
Direct service control, custom requests, headers, and managing multiple providers.

📦 **[Structured Output](chatting/structured-output.md)**
Extract type-safe, validated data from AI responses using classes, structs, or schemas.

---

### AI Pipelines

🔗 **[Understanding Pipelines](main-components/overview.md)**
Core concepts of AI pipelines, composability, and building workflows.

🤖 **[AI Agents](main-components/agents.md)**
Create autonomous agents with memory, tools, and reasoning capabilities. Simplify complex AI workflows.

🧠 **[Working with Models](main-components/models.md)**
Creating model runnables, configuration, and integrating AI providers into pipelines.

✉️ **[Message Templates](main-components/messages.md)**
Building reusable prompts with dynamic placeholders and binding strategies.

💭 **[Memory Systems](main-components/memory.md)**
Maintain conversation context with standard memory types: Windowed, Summary, Session, File, Cache, and JDBC memory for different use cases.

🧠 **[Vector Memory](main-components/vector-memory.md)**
Semantic search and retrieval using vector embeddings. Integrate with ChromaDB, Pinecone, PostgreSQL, MySQL, TypeSense, Qdrant, Weaviate, Milvus, or use in-memory vectors.

🔧 **[Transformers](main-components/transformers.md)**
Processing and transforming data between pipeline steps.

📡 **[Pipeline Streaming](main-components/streaming.md)**
Real-time streaming through pipelines for responsive applications.

📄 **[Document Loaders](main-components/document-loaders.md)**
Load documents from files, directories, and URLs. Supports text, Markdown, HTML, CSV, and JSON with automatic chunking and memory integration.

---

### Advanced Topics

🔐 **[Message Context](advanced/message-context.md)**
Inject security, RAG, and application context into AI messages. Learn multi-tenant patterns, RAG implementation, and contextual AI interactions.

🎪 **[Event System](advanced/events.md)**
Intercept and customize AI operations with comprehensive event hooks for monitoring, security, and extensibility.

🛠️ **[Utility Functions](advanced/utilities.md)**
Text chunking, token counting, and optimization techniques for AI processing. Learn to manage large documents and estimate costs.

🔢 **[Embeddings](advanced/embeddings.md)**
Generate vector representations for semantic search, recommendations, clustering, and similarity detection. Complete guide to embeddings.

🔌 **[MCP Client](advanced/mcp-client.md)**
Connect to Model Context Protocol (MCP) servers for external tools, resources, and prompts. Fluent API for MCP integration.

🎨 **[Custom Memory](advanced/custom-memory.md)**
Build your own memory implementations by extending BaseMemory. Create specialized memory systems for unique requirements.

🧩 **[Custom Vector Memory](advanced/custom-vector-memory.md)**
Implement custom vector memory providers by extending BaseVectorMemory. Integrate with any vector database or search system.

---

## Built-In Functions (BIFs) Overview

BoxLang AI provides a comprehensive set of BIFs for different AI operations:

| BIF | Purpose | Return Type | Example Use Case |
|-----|---------|-------------|-----------------|
| `aiChat()` | Simple one-shot chat request | String | Quick Q&A, content generation |
| `aiChatAsync()` | Non-blocking chat request | Future | Background processing, parallel requests |
| `aiChatRequest()` | Build structured chat requests | AiRequest | Complex requests with tools |
| `aiChatStream()` | Real-time streaming responses | void | Live chat, progressive output |
| `aiChunk()` | Split text into manageable chunks | Array | Processing large documents |
| `aiDocuments()` | Load documents from files/URLs | Array | Document processing, RAG |
| `aiDocumentLoader()` | Create document loader instance | IDocumentLoader | Advanced loader configuration |
| `aiDocumentLoaders()` | Get all registered loaders | Struct | Loader metadata and capabilities |
| `aiEmbed()` | Generate vector embeddings | Array/Struct | Semantic search, similarity |
| `aiMemory()` | Create conversation memory systems | Memory | Context-aware conversations, history |
| `aiMemoryIngest()` | Ingest documents into memory | Report | RAG pipelines, knowledge bases |
| `aiMessage()` | Build message pipelines | AiMessage | Reusable prompts, templates |
| `aiModel()` | Create model runnables | AiModel | Pipeline integration |
| `aiService()` | Get AI service instances | Service | Multi-provider management |
| `aiTokens()` | Estimate token counts | Numeric | Cost estimation, limits |
| `aiTool()` | Define callable functions | Tool | Real-time data, function calling |
| `aiTransform()` | Create data transformers | Transformer | Pipeline data processing |
| `MCP()` | Connect to MCP servers | MCPClient | External tools, resources, prompts |

**Quick Reference:**

- **Simple Operations**: `aiChat()`, `aiChatAsync()`, `aiChatStream()`
- **Structured Requests**: `aiChatRequest()`, `aiMessage()`, `aiModel()`
- **Advanced Features**: `aiTool()`, `aiMemory()`, `aiTransform()`
- **Utilities**: `aiChunk()`, `aiTokens()`, `aiEmbed()`
- **Service Management**: `aiService()`

---

## Quick Examples

### Simple Chat

```java
answer = aiChat( "What is BoxLang?" )
println( answer )
```

### Chat with Parameters

```java
answer = aiChat(
    "Write a haiku about coding",
    { temperature: 0.9, model: "gpt-4" }
)
```

### Build a Pipeline

```java
pipeline = aiMessage()
    .system( "You are a helpful assistant" )
    .user( "Explain ${topic}" )
    .toDefaultModel()
    .transform( r => r.content )

result = pipeline.run( { topic: "recursion" } )
```

### Stream Responses

```java
aiChatStream(
    "Tell me a story",
    ( chunk ) => print( chunk.choices?.first()?.delta?.content ?: "" )
)
```

### Get JSON Responses

```java
// Automatically parse JSON responses
user = aiChat(
    "Create a user profile with name, age, and email for Alice",
    { returnFormat: "json" }
)

println( "Name: #user.name#" )
println( "Age: #user.age#" )
println( "Email: #user.email#" )
```

### Use AI Tools

```java
// Let AI call functions for real-time data
getWeather = aiTool(
    name: "get_weather",
    description: "Get current weather for a location",
    parameters: {
        location: { type: "string", description: "City name" }
    },
    callback: ( args ) => {
        return { temp: 72, condition: "sunny", location: args.location }
    }
)

response = aiChat(
    "What's the weather in San Francisco?",
    { tools: [ getWeather ] }
)
```

### Generate Embeddings

```java
// Create vector embeddings for semantic search
embeddings = aiEmbed([
    "BoxLang is a modern JVM language",
    "Java is a programming language",
    "Python is popular for AI"
])

// Use embeddings for similarity comparison
println( "Generated #embeddings.len()# embeddings" )
```

---

## Need Help?

- **📖 Full Documentation**: Explore the sections above
- **💡 Examples**: Check the `/examples` folder in the repository
- **👥 Community**: [BoxLang Community Forum](https://community.boxlang.io)
- **🐛 Issues**: [GitHub Issues](https://github.com/ortus-boxlang/bx-ai/issues)
- **✉️ Support**: support@ortussolutions.com

## Upgrade to Plus

Want more? Check out **bx-aiplus** for additional features, and enterprise support:

- Enterprise modules
- Advanced tooling
- Priority support
- Enterprise features

Learn more at [boxlang.io/plans](https://boxlang.io/plans)

**Copyright** © 2023-2025 Ortus Solutions, Corp
**License**: Apache 2.0
**Website**: [boxlang.io](https://boxlang.io)
