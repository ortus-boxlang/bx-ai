# ⚡︎ BoxLang AI

```
|:------------------------------------------------------:|
| ⚡︎ B o x L a n g ⚡︎
| Dynamic : Modular : Productive
|:------------------------------------------------------:|
```

<blockquote>
	Copyright Since 2023 by Ortus Solutions, Corp
	<br>
	<a href="https://ai.boxlang.io">ai.boxlang.io</a> |
	<a href="https://www.boxlang.io">www.boxlang.io</a>
	<br>
	<a href="https://ai.ortussolutions.com">ai.ortussolutions.com</a> |
	<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</blockquote>

<p>&nbsp;</p>

## 👋 Welcome

![BoxLang AI Module](BoxLangAI.png)

Welcome to the **BoxLang AI Module** 🚀 The official AI library for BoxLang that provides a unified, fluent API to orchestrate multi-model workflows, autonomous agents, RAG pipelines, and AI-powered applications. **One API → Unlimited AI Power!** ✨

**BoxLang AI** eliminates vendor lock-in and simplifies AI integration by providing a single, consistent interface across **15+ AI providers**. Whether you're using OpenAI, Claude, Gemini, Grok, DeepSeek, Ollama, or Perplexity—your code stays the same. Switch providers, combine models, and orchestrate complex workflows with simple configuration changes. 🔄

## ✨ Key Features

- 🔌 **15+ AI Providers** - Single API for OpenAI, Claude, AWS Bedrock, Gemini, Grok, Ollama, DeepSeek, and more
- 🤖 **AI Agents** - Autonomous agents with memory, tools, sub-agents, and multi-step reasoning
- 🔒 **Multi-Tenant Memory** - Enterprise-grade isolation with 20+ memory types (standard + vector)
- 🧬 **Vector Memory & RAG** - 10+ vector databases with semantic search (ChromaDB, Pinecone, PostgreSQL, etc.)
- 📚 **Document Loaders** - 30+ file formats including PDF, Word, CSV, JSON, XML, web scraping, and databases
- 🛠️ **Real-Time Tools** - Function calling for APIs, databases, and external system integration
- 🌊 **Streaming Support** - Real-time token streaming through pipelines for responsive applications
- 📦 **Structured Output** - Type-safe responses using BoxLang classes, structs, or JSON schemas
- 🔗 **AI Pipelines** - Composable workflows with models, transformers, and custom logic
- 📡 **MCP Protocol** - Build and consume Model Context Protocol servers for distributed AI
- 💬 **Fluent Interface** - Chainable, expressive syntax that makes AI integration intuitive
- 🦙 **Local AI** - Full Ollama support for privacy, offline use, and zero API costs
- ⚡ **Async Operations** - Non-blocking futures for concurrent AI requests
- 🎯 **Event-Driven** - 25+ lifecycle events for logging, monitoring, and custom workflows
- 🏭 **Production-Ready** - Timeout controls, error handling, rate limiting, and debugging tools

## 📃 Table of Contents

- [📄 License](#-license)
- [🚀 Getting Started](#-getting-started)
- [🤖 Supported Providers](#-supported-providers)
  - [📊 Provider Support Matrix](#-provider-support-matrix)
- [📤 Return Formats](#-return-formats)
- [🥊 Quick Overview](#-quick-overview)
  - [💬 Chats](#-chats)
  - [🔗 Pipelines](#-pipelines)
  - [🤖 AI Agents](#-ai-agents)
  - [📦 Structured Output](#-structured-output)
  - [🧠 Memory Systems](#-memory-systems)
  - [📚 Document Loaders & RAG](#-document-loaders--rag)
  - [🔌 MCP Client](#-mcp-client)
  - [🖥️ MCP Server](#️-mcp-server)
- [⚙️ Settings](#️-settings)
- [🛠️ Global Functions (BIFs)](#️-global-functions-bifs)
- [📢 Events](#-events)
- [🌐 GitHub Repository and Reporting Issues](#-github-repository-and-reporting-issues)
- [🧪 Testing](#-testing)
- [💖 Ortus Sponsors](#-ortus-sponsors)

## 📃 License

BoxLang is open source and licensed under the [Apache 2](https://www.apache.org/licenses/LICENSE-2.0.html) license. 🎉 You can also get a professionally supported version with enterprise features and support via our BoxLang +/++ Plans (www.boxlang.io/plans). 💼

## 🚀 Getting Started

You can use BoxLang AI in both operating system applications, AWS Lambda, and web applications.  For OS applications, you can use the module installer to install the module globally.  For AWS Lambda and web applications, you can use the module installer to install it locally in your project or CommandBox as the package manager, which is our preferred method for web applications.

**📚 New to AI concepts?** Check out our [Key Concepts Guide](https://ai.ortusbooks.com/getting-started/concepts) for terminology and fundamentals, or browse our [FAQ](https://ai.ortusbooks.com/readme/faq) for quick answers to common questions.  We also have a [Quick Start Guide](https://ai.ortusbooks.com/getting-started/quickstart) and our intense [AI BootCamp](https://github.com/ortus-boxlang/bx-ai/tree/development/bootcamp) available to you as well.

### OS

You can easily get started with BoxLang AI by using the module installer for building operating system applications:

```bash
install-bx-module bx-ai
```

This will install the latest version of the BoxLang AI module in your BoxLang environment. Once installed, make sure you setup any of the supported AI providers and their API keys in your `boxlang.json` configuration file or environment variables.  After that you can leverage the global functions (BIFs) in your BoxLang code.  Here is a simple example:

```java
// chat.bxs
answer = aiChat( "How amazing is BoxLang?" )
println( answer )
```

You can then run your BoxLang script like this:

```bash
boxlang chat.bxs
```

### AWS Lambda

In order to build AWS Lambda functions with Boxlang AI for serverless AI agents and applications, you can use the [Boxlang AWS Runtime](https://boxlang.ortusbooks.com/getting-started/running-boxlang/aws-lambda) and our [AWS Lambda Starter Template](https://github.com/ortus-boxlang/boxlang-starter-aws-lambda).   You will use the `install-bx-module` as well to install the module locally using the `--local` flag in the `resources` folder of your project:

```bash
cd src/resources
install-bx-module bx-ai --local
```

Or you can use CommandBox as well and store your dependencies in the `box.json` descriptor.

```bash
box install bx-ai resources/modules/
```

### Web Applications

To use BoxLang AI in your web applications, you can use CommandBox as the package manager to install the module locally in your project.  You can do this by running the following command in your project root:

```bash
box install bx-ai
```

Just make sure you have already a server setup with BoxLang.  You can check our [Getting Started with BoxLang Web Applications](https://boxlang.ortusbooks.com/getting-started/running-boxlang/commandbox) guide for more details on how to get started with BoxLang web applications.

## 🤖 Supported Providers

The following are the AI providers supported by this module. **Please note that in order to interact with these providers you will need to have an account with them and an API key.** 🔑

- ☁️ [AWS Bedrock](https://aws.amazon.com/bedrock/) - Claude, Titan, Llama, Mistral via AWS
- 🧠 [Claude Anthropic](https://www.anthropic.com/claude)
- 🧬 [Cohere](https://cohere.com/)
- 🔍 [DeepSeek](https://www.deepseek.com/)
- 🐳 [Docker Model Runner](https://docs.docker.com/ai/model-runner/) - Local models via Docker Desktop
- 💎 [Gemini](https://gemini.google.com/)
- ⚡ [Grok](https://grok.com/)
- 🚀 [Groq](https://groq.com/)
- 🤗 [HuggingFace](https://huggingface.co/)
- 🌀 [Mistral](https://mistral.ai/)
- 🦙 [Ollama](https://ollama.ai/)
- 🟢 [OpenAI](https://www.openai.com/)
- 🔌 [OpenAI-Compatible](https://platform.openai.com/docs/api-reference) - Any OpenAI-compatible API
- 🔀 [OpenRouter](https://openrouter.ai/)
- 🔮 [Perplexity](https://docs.perplexity.ai/)
- 🚢 [Voyage AI](https://www.voyageai.com/)

### 📊 Provider Support Matrix

Here is a matrix of the providers and their feature support. Please keep checking as we will be adding more providers and features to this module. 🔄

| Provider   | Real-time Tools | Embeddings | Structured Output |
|------------|-----------------|------------|-------------------|
| AWS Bedrock  | ✅ | ✅ | ✅ |
| Claude    	| ✅ | ❌ | ✅ |
| Cohere       | ✅ | ✅ | ✅ |
| DeepSeek  | ✅ | ✅ | ✅ |
| Docker Model Runner | ✅ | ✅ | ✅ |
| Gemini    	| [Coming Soon]   | ✅ | ✅ |
| Grok      	 | ✅ | ✅ | ✅ |
| Groq         | ✅ | ✅ | ✅ |
| HuggingFace | ✅ | ✅ | ✅ |
| Mistral      | ✅ | ✅ | ✅ |
| Ollama       | ✅ | ✅ | ✅ |
| OpenAI       | ✅ | ✅ | ✅ (Native) |
| OpenAI-Compatible | ✅ | ✅ | ✅ |
| OpenRouter   | ✅ | ✅ | ✅ |
| Perplexity   | ✅ | ❌ | ✅ |
| Voyage       | ❌ | ✅ (Specialized) | ❌ |

## 📤 Return Formats

BoxLang not only makes it extremely easy to interact with multiple AI providers, but it also gives you the flexibility to choose how you want the responses returned to you. You can specify the return format using the `responseFormat` parameter in your AI calls. Here are the available formats:

| Format | Description |
|--------|-------------|
| `single` | Returns a single message as a string (the content from the first choice). This is the default format for BIFs. |
| `all` | Returns an array of all choice messages. Each message is a struct with `role` and `content` keys. |
| `json` | Returns the parsed JSON object from the content string. Automatically parses JSON responses. |
| `xml` | Returns the parsed XML document from the content string. Automatically parses XML responses. |
| `raw` | Returns the full raw response from the AI provider. This is useful for debugging or when you need the full response structure with metadata. This is the default for pipelines. |
| `structuredOutput` | Used internally when `.structuredOutput()` is called. Returns a populated class/struct based on the schema. |

## 🥊 Quick Overview

In the following sections, we provide a quick overview of the main components of BoxLang AI including Chats, Pipelines, Agents, Structured Output, Memory Systems, Document Loaders & RAG, and MCP Client/Server. Each section includes quick examples and links to more detailed documentation.  For further details, please refer to the [official documentation](https://ai.ortusbooks.com/), this is just a high-level overview to get you started quickly. 🚀

### 💬 Chats

Interact with AI models through **simple and powerful chat interfaces** 🎯 supporting both one-shot responses and streaming conversations. BoxLang AI provides fluent APIs for building everything from basic Q&A to complex multi-turn dialogues with system prompts, message history, and structured outputs. 💡

#### 🤔 Why Use Chats?

- ⚡ **Simple & Fast** - One-line chat interactions with `aiChat()`
- 🔄 **Streaming Support** - Real-time token streaming with `aiChatStream()`
- 💾 **Memory Integration** - Automatic conversation history with memory systems
- 🎨 **Flexible Messages** - Support for text, images, files, and structured data
- 🌊 **Fluent API** - Chain message builders for readable, maintainable code

#### 💡 Quick Examples

**Simple One-Shot Chat:**

```javascript
// Quick question-answer
response = aiChat( "What is BoxLang?" )
println( response )

// With custom model and options
response = aiChat(
    messages: "Explain quantum computing",
    params: { model: "gpt-4", temperature: 0.7, max_tokens: 500 }
)
```

**Multi-Turn Conversation with Memory:**

```javascript
// Create agent with memory
agent = aiAgent(
    name: "Assistant",
    memory: aiMemory( "window", config: { maxMessages: 10 } )
)

// First turn
response = agent.run( "My name is Luis" )

// Second turn - Agent remembers context
response = agent.run( "What's my name?" )
println( response ) // "Your name is Luis"
```

**Streaming Chat:**

```javascript
// Stream tokens as they arrive
aiChatStream(
    messages: "Write a short story about a robot",
    callback: chunk => {
        writeOutput( chunk.choices?.first()?.delta?.content ?: "" )
		bx:flush;
    },
    params: { model: "claude-3-5-sonnet-20241022" }
)
```

**Fluent Message Builder:**

```javascript
// Build complex message chains
messages = aiMessage()
    .system( "You are a helpful coding assistant" )
    .user( "How do I create a REST API in BoxLang?" )
    .image( "diagram.png" )

response = aiChat(
    messages: messages,
    params: { model: "gpt-4o", temperature: 0.7 }
)
```

#### 📚 Learn More

- 🚀 **Quick Start**: [Getting Started Guide](https://ai.ortusbooks.com/getting-started/quickstart.md)
- 📖 **Full Guide**: [Chatting Documentation](https://ai.ortusbooks.com/chatting/)
- 🌊 **Streaming**: [Streaming Guide](https://ai.ortusbooks.com/chatting/streaming.md)
- 🎨 **Message Formats**: [Message Builder Guide](https://ai.ortusbooks.com/chatting/messages.md)

----

### 🔗 Pipelines

Build **composable AI workflows** 🎯 using BoxLang AI's powerful runnable pipeline system. Chain models, transformers, tools, and custom logic into reusable, testable components that flow data through processing stages. Perfect for complex AI workflows, data transformations, and multi-step reasoning. 💡

#### 🤔 Why Use Pipelines?

- 🔄 **Composable** - Chain any runnable components together with `.to()`
- 🧪 **Testable** - Each pipeline stage is independently testable
- ♻️ **Reusable** - Build once, use in multiple workflows
- 🌊 **Streaming** - Full streaming support through entire pipeline
- 🎯 **Type-Safe** - Input/output contracts ensure data flows correctly

#### 💡 Quick Examples

**Simple Transformation Pipeline:**

```javascript
// Create a pipeline with model and transformers
pipeline = aiModel( provider: "openai" )
    .transform( data => data.toUpperCase() )
    .transform( data => data.trim() )

// Run input through the pipeline
result = pipeline.run( "hello world" )
println( result ) // "HELLO WORLD"
```

**Multi-Stage AI Pipeline:**

```javascript
// Define transformation stages as closures
summarizer = ( text ) => {
    return aiChatAsync(
        aiMessage().system( "Summarize in one sentence" ).user( text ),
        { model: "gpt-4o-mini" }
    )
}

translator = ( summary ) => {
    return aiChatAsync(
        aiMessage().system( "Translate to Spanish" ).user( summary ),
        { model: "gpt-4o" }
    )
}

formatter = ( translatedText ) => {
    return {
        summary: translatedText,
        timestamp: now()
    }
}

// Compose pipeline using async futures
result = summarizer( "Long article text here..." )
    .then( summary => translator( summary ) )
    .then( translated => formatter( translated ) )
    .get()

println( result.summary ) // Spanish summary
```

**Streaming Pipeline:**

```javascript
// Stream through entire pipeline
pipeline = aiModel( provider: "claude", params: { model: "claude-3-5-sonnet-20241022" } )
    .transform( chunk => chunk.toUpperCase() )
	.stream(
		onChunk: ( chunk ) => writeOutput( chunk ),
		input: "Tell me a story"
	)
```

**Custom Runnable Class:**

```javascript
// Implement IAiRunnable for custom logic
class implements="IAiRunnable" {

    function run( input, params = {} ) {
        // Custom processing
        return processedData;
    }

    function stream( onChunk, input, params = {} ) {
        // Streaming support
        onChunk( processedChunk );
    }

    function to( nextRunnable ) {
        // Chain to next stage
        return createPipeline( this, nextRunnable );
    }
}

// Use in pipeline
customStage = new CustomRunnable()
pipeline = aiModel( provider: "openai", params: { model: "gpt-4o" } )
	.to( customStage )
```

#### 📚 Learn More

- 📖 **Full Guide**: [Runnables & Pipelines](https://ai.ortusbooks.com/main-components/runnables.md)
- 🎯 **Overview**: [Main Components](https://ai.ortusbooks.com/main-components/overview.md)
- 🔧 **Custom Runnables**: [Building Custom Components](https://ai.ortusbooks.com/advanced/custom-runnables.md)
- 💻 **Examples**: Check `examples/pipelines/` for complete examples

----

### 🤖 AI Agents

Build **autonomous AI agents** 🎯 that can use tools, maintain memory, and orchestrate complex workflows. BoxLang AI agents combine LLMs with function calling, memory systems, and orchestration patterns to create intelligent assistants that can interact with external systems and solve complex tasks. 💡

#### 🤔 Why Use Agents?

- 🛠️ **Tool Integration** - Agents can execute functions, call APIs, and interact with external systems
- 🧠 **Stateful Intelligence** - Built-in memory keeps context across multi-turn interactions
- 🔄 **Self-Orchestration** - Agents decide which tools to use and when
- 🎯 **Goal-Oriented** - Give high-level instructions, agents figure out the steps
- 🤝 **Human-in-the-Loop** - Optional approval workflows for sensitive operations

#### 💡 Quick Examples

**Simple Agent with Tools:**

```javascript
// Define tools the agent can use
weatherTool = aiTool(
    name: "get_weather",
    description: "Get current weather for a location",
    callable: ( location ) => {
        return { temp: 72, condition: "sunny", location: location };
    }
)

// Create agent with memory
agent = aiAgent(
    name: "Weather Assistant",
    description: "Helpful weather assistant",
    tools: [ weatherTool ],
    memory: aiMemory( "window" )
)

// Agent decides when to call tools
response = agent.run( "What's the weather in Miami?" )
println( response ) // Agent calls get_weather tool and responds
```

**Autonomous Agent with Multiple Tools:**

```javascript
// Agent with database and email tools
agent = aiAgent(
    name: "Customer Support Agent",
    tools: [
        aiTool( name: "query_orders", description: "Query customer orders", callable: orderQueryFunction ),
        aiTool( name: "send_email", description: "Send email to customer", callable: emailFunction ),
        aiTool( name: "create_ticket", description: "Create support ticket", callable: ticketFunction )
    ],
    memory: aiMemory( "session" ),
    params: { max_iterations: 5 }
)

// Agent orchestrates multiple tool calls
agent.run( "Find order #12345, email the customer with status, and create a ticket if there's an issue" )
```

#### 📚 Learn More

- 📖 **Full Guide**: [AI Agents Documentation](https://ai.ortusbooks.com/main-components/agents.md)
- 🎓 **Interactive Course**: [Lesson 6 - Building AI Agents](course/lesson-06-agents/)
- 🔧 **Advanced Patterns**: [Agent Orchestration](https://ai.ortusbooks.com/advanced/agent-orchestration.md)
- 💻 **Examples**: Check `examples/agents/` for complete working examples

----

### 📦 Structured Output

Get **type-safe, validated responses** ✅ from AI providers by defining expected output schemas using BoxLang classes, structs, or JSON schemas. The module automatically converts AI responses into properly typed objects, eliminating manual parsing and validation. 🎯

#### 🤔 Why Use Structured Output?

- ✅ **Type Safety** - Get validated objects instead of parsing JSON strings
- 🔒 **Automatic Validation** - Schema constraints ensure correct data types and required fields
- 🎯 **Better Reliability** - Reduces hallucinations by constraining response format
- 💻 **Developer Experience** - Work with native BoxLang objects immediately
- 🧪 **Testing & Caching** - Use `aiPopulate()` to create objects from JSON for tests or cached responses

#### 💡 Quick Examples

**Using a Class:**

```java
class Person {
    property name="name" type="string";
    property name="age" type="numeric";
    property name="email" type="string";
}

result = aiChat(
    messages: "Extract person info: John Doe, 30, john@example.com",
    options: { returnFormat: new Person() }
)

writeOutput( "Name: #result.getName()#, Age: #result.getAge()#" );
```

**Using a Struct Template:**

```java
template = {
    "title": "",
    "summary": "",
    "tags": [],
    "sentiment": ""
};

result = aiChat(
    messages: "Analyze this article: [long text]",
    options: { returnFormat: template }
)

writeOutput( "Tags: #result.tags.toList()#" );
```

**Extracting Arrays:**

```java
class Task {
    property name="title" type="string";
    property name="priority" type="string";
    property name="dueDate" type="string";
}

tasks = aiChat(
    messages: "Extract tasks from: Finish report by Friday (high priority), Review code tomorrow",
    options: { returnFormat: [ new Task() ] }
)

for( task in tasks ) {
    writeOutput( "#task.getTitle()# - Priority: #task.getPriority()#<br>" );
}
```

**Multiple Schemas (Extract Different Types Simultaneously):**

```java
result = aiChat(
    messages: "Extract person and company: John Doe, 30 works at Acme Corp, founded 2020",
    options: {
        returnFormat: {
            "person": new Person(),
            "company": new Company()
        }
    }
)

writeOutput( "Person: #result.person.getName()#<br>" );
writeOutput( "Company: #result.company.getName()#<br>" );
```

#### 🔧 Manual Population with aiPopulate()

Convert JSON responses or cached data into typed objects without making AI calls:

```java
// From JSON string
jsonData = '{"name":"John Doe","age":30,"email":"john@example.com"}';
person = aiPopulate( new Person(), jsonData );

// From struct
data = { name: "Jane", age: 25, email: "jane@example.com" };
person = aiPopulate( new Person(), data );

// Populate array
tasksJson = '[{"title":"Task 1","priority":"high"},{"title":"Task 2","priority":"low"}]';
tasks = aiPopulate( [ new Task() ], tasksJson );
```

**Perfect for:** ⭐

- 🧪 Testing with mock data
- 💾 Using cached AI responses
- 🔄 Converting existing JSON data to typed objects
- ✅ Validating data structures

#### ✅ Provider Support

All providers support structured output! 🎉 OpenAI offers native structured output with strict validation, while others use JSON mode with schema guidance (which works excellently in practice). 💪

#### 📚 Learn More

- 🚀 **Quick Start**: [Simple Interactions Guide](https://ai.ortusbooks.com/chatting/structured-output.md)
- 🔧 **Advanced Pipelines**: [Pipeline Integration Guide](https://ai.ortusbooks.com/main-components/structured-output.md)
- 🎓 **Interactive Course**: [Lesson 12 - Structured Output](course/lesson-12-structured-output/)
- 💻 **Examples**: Check `examples/structured/` for complete working examples

### 🧠 Memory Systems

Build **stateful, context-aware AI applications** 🎯 with flexible memory systems that maintain conversation history, enable semantic search, and preserve context across interactions. BoxLang AI provides both traditional conversation memory and advanced vector-based memory for semantic understanding. 💡

#### 🤔 Why Use Memory?

- 💭 **Context Retention** - AI remembers previous messages and maintains coherent conversations
- 💬 **Stateful Applications** - Build chat interfaces that remember user preferences and conversation history
- 🔍 **Semantic Search** - Find relevant past conversations using vector embeddings
- 💾 **Flexible Storage** - Choose from in-memory, file-based, database, session, or vector storage
- ⚙️ **Automatic Management** - Memory handles message limits, summarization, and context windows

#### 📋 Memory Types

**Standard Memory** 💬 (Conversation History):

| Type | Description | Best For |
|------|-------------|----------|
| **Windowed** | Keeps last N messages | Quick chats, cost-conscious apps |
| **Summary** | Auto-summarizes old messages | Long conversations, context preservation |
| **Session** | Web session persistence | Multi-page web applications |
| **File** | File-based storage | Audit trails, long-term storage |
| **Cache** | CacheBox-backed | Distributed applications |
| **JDBC** | Database storage | Enterprise apps, multi-user systems |

**Vector Memory** 🔍 (Semantic Search):

| Type | Description | Best For |
|------|-------------|----------|
| **BoxVector** | In-memory vectors | Development, testing, small datasets |
| **Hybrid** | Recent + semantic | Best of both worlds approach |
| **Chroma** | ChromaDB integration | Python-based infrastructure |
| **Postgres** | PostgreSQL pgvector | Existing PostgreSQL deployments |
| **MySQL** | MySQL 9 native vectors | Existing MySQL infrastructure |
| **OpenSearch** | OpenSearch k-NN vectors | AWS OpenSearch, self-hosted search |
| **TypeSense** | Fast typo-tolerant search | Low-latency search, autocomplete |
| **Pinecone** | Cloud vector database | Production, scalable semantic search |
| **Qdrant** | High-performance vectors | Large-scale deployments |
| **Weaviate** | GraphQL vector database | Complex queries, knowledge graphs |
| **Milvus** | Enterprise vector DB | Massive datasets, high throughput |

#### 💡 Quick Examples

**Windowed Memory (Multi-Tenant):**

```java
// Automatic per-user isolation
memory = aiMemory(
    memory: "window",
    key: createUUID(),
    userId: "user123",
    config: { maxMessages: 10 }
)
agent = aiAgent( name: "Assistant", memory: memory )

agent.run( "My name is John" )
agent.run( "What's my name?" )  // "Your name is John"
```

**Summary Memory (Preserves Full Context):**

```java
memory = aiMemory( "summary", config: {
    maxMessages: 30,
    summaryThreshold: 15,
    summaryModel: "gpt-4o-mini"
} )
agent = aiAgent( name: "Support", memory: memory )
// Long conversation - older messages summarized automatically
```

**Vector Memory (Semantic Search + Multi-Tenant):**

```java
memory = aiMemory(
    memory: "chroma",
    key: createUUID(),
    userId: "user123",
    conversationId: "support",
    config: {
        collection: "customer_support",
        embeddingProvider: "openai",
        embeddingModel: "text-embedding-3-small"
    }
)
// Retrieves semantically relevant past conversations
// Automatically filtered by userId/conversationId
```

**Hybrid Memory (Recent + Semantic):**

```java
memory = aiMemory( "hybrid", config: {
    recentLimit: 5,       // Keep last 5 messages
    semanticLimit: 5,     // Add 5 semantic matches
    vectorProvider: "chroma"
} )
// Combines recency with relevance
```

#### 📚 Learn More

- 💬 **Standard Memory**: [Memory Systems Guide](https://ai.ortusbooks.com/main-components/memory.md)
- 🔍 **Vector Memory**: [Vector Memory Guide](https://ai.ortusbooks.com/main-components/vector-memory.md)
- 🔧 **Custom Memory**: [Building Custom Memory](https://ai.ortusbooks.com/advanced/custom-memory.md)
- 🎓 **Interactive Course**: [Lesson 7 - Memory Systems](course/lesson-07-memory/)
- 💻 **Examples**: Check `examples/advanced/` and `examples/vector-memory/` for complete examples

----

### 📚 Document Loaders & RAG

BoxLang AI provides **12+ built-in document loaders** for ingesting content from files, databases, web sources, and more. These loaders integrate seamlessly with vector memory systems to enable **Retrieval-Augmented Generation (RAG)** workflows.

#### 🔄 RAG Workflow

```mermaid
graph LR
    LOAD[📄 Load Documents] --> CHUNK[✂️ Chunk Text]
    CHUNK --> EMBED[🧬 Generate Embeddings]
    EMBED --> STORE[💾 Store in Vector Memory]
    STORE --> QUERY[❓ User Query]
    QUERY --> RETRIEVE[🔍 Retrieve Relevant Docs]
    RETRIEVE --> INJECT[💉 Inject into Context]
    INJECT --> AI[🤖 AI Response]

    style LOAD fill:#4A90E2
    style EMBED fill:#BD10E0
    style STORE fill:#50E3C2
    style RETRIEVE fill:#F5A623
    style AI fill:#7ED321
```

#### 📄 Available Loaders

| Loader | Type | Use Case | Example |
|--------|------|----------|---------|
| 📝 **TextLoader** | `text` | Plain text files | `.txt`, `.log` |
| 📘 **MarkdownLoader** | `markdown` | Markdown files | `.md` documents |
| 📊 **CSVLoader** | `csv` | CSV files | Data files, exports |
| 🗂️ **JSONLoader** | `json` | JSON files | Configuration, data |
| 🏷️ **XMLLoader** | `xml` | XML files | Config, structured data |
| 📄 **PDFLoader** | `pdf` | PDF documents | Reports, documentation |
| 📋 **LogLoader** | `log` | Log files | Application logs |
| 🌐 **HTTPLoader** | `http` | Web pages | Documentation, articles |
| 📰 **FeedLoader** | `feed` | RSS/Atom feeds | News, blogs |
| 💾 **SQLLoader** | `sql` | Database queries | Query results |
| 📁 **DirectoryLoader** | `directory` | File directories | Batch processing |
| 🕷️ **WebCrawlerLoader** | `webcrawler` | Website crawling | Multi-page docs |

#### ✨ Quick Examples

**Load a Single Document:**

```javascript
// Load a PDF document
docs = aiDocuments(
    source: "/path/to/document.pdf",
    config: { type: "pdf" }
).load()
println( "#docs.len()# documents loaded" )

// Load with configuration
docs = aiDocuments(
    source: "/path/to/document.pdf",
    config: {
        type: "pdf",
        sortByPosition: true,
        addMoreFormatting: true,
        startPage: 1,
        endPage: 10
    }
).load()
```

**Load Multiple Documents:**

```javascript
// Load all markdown files from a directory
docs = aiDocuments(
    source: "/knowledge-base",
    config: {
        type: "directory",
        recursive: true,
        extensions: ["md", "txt"],
        excludePatterns: ["node_modules", ".git"]
    }
).load()
```

**Ingest into Vector Memory:**

```javascript
// Create vector memory
vectorMemory = aiMemory( "chroma", config: {
    collection: "docs",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small"
} )

// Ingest documents with chunking and embedding
result = aiDocuments(
    source: "/knowledge-base",
    config: {
        type: "directory",
        recursive: true,
        extensions: ["md", "txt", "pdf"]
    }
).toMemory(
    memory: vectorMemory,
    options: { chunkSize: 1000, overlap: 200 }
)

println( "✅ Loaded #result.documentsIn# docs as #result.chunksOut# chunks" )
println( "💰 Estimated cost: $#result.estimatedCost#" )
```

**RAG with Agent:**

```javascript
// Create agent with vector memory
agent = aiAgent(
    name: "KnowledgeAssistant",
    description: "AI assistant with access to knowledge base",
    memory: vectorMemory
)

// Query automatically retrieves relevant documents
response = agent.run( "What is BoxLang?" )
println( response )
```

#### 📚 Learn More

- 📖 **Full Guide**: [Document Loaders Guide](https://ai.ortusbooks.com/main-components/document-loaders.md)
- 🧬 **RAG Workflow**: [RAG Implementation Guide](https://ai.ortusbooks.com/main-components/rag.md)
- 🔧 **Custom Loaders**: [Building Custom Loaders](https://ai.ortusbooks.com/advanced/custom-loader.md)
- 💻 **Examples**: Check `examples/loaders/` and `examples/rag/` for complete examples

----

### 🔌 MCP Client

Connect to **Model Context Protocol (MCP) servers** 🎯 and use their tools, prompts, and resources in your AI applications. BoxLang AI's MCP client provides seamless integration with the growing MCP ecosystem, allowing your agents to access databases, APIs, filesystems, and more through standardized interfaces. 💡

#### 🤔 Why Use MCP Client?

- 🌍 **Ecosystem Access** - Use any MCP server (filesystems, databases, APIs, tools)
- 🔒 **Secure Integration** - Standardized permissions and authentication
- 🎯 **Tool Discovery** - Automatically discover and use server capabilities
- 🔄 **Dynamic Resources** - Access changing data sources (files, DB records, etc.)
- 🤖 **Agent Integration** - Seamlessly add MCP tools to your AI agents

#### 💡 Quick Examples

**Connect to MCP Server:**

```javascript
// Connect to MCP server via HTTP
mcpClient = MCP( "http://localhost:3000" )
    .withTimeout( 5000 )

// List available tools
tools = mcpClient.listTools()
println( tools ) // Returns available MCP tools
```

**Use MCP Tools in Agent:**

```javascript
// Connect to MCP servers
filesystemMcp = MCP( "http://localhost:3001" ).withTimeout( 5000 )
databaseMcp = MCP( "http://localhost:3002" ).withTimeout( 5000 )

// Create agent (MCP integration depends on agent implementation)
agent = aiAgent(
    name: "Data Assistant",
    description: "Assistant with MCP tool access"
)

// Agent automatically discovers and uses MCP tools
response = agent.run( "Read config.json and update the database with its contents" )

// Agent automatically uses MCP tools
agent.run( "Read config.json and update the database with its contents" )
```

**Access MCP Resources:**

```javascript
// List available resources
resources = mcpClient.listResources()

// Read resource content
content = mcpClient.readResource( "file:///docs/readme.md" )
println( content )

// Use prompts from server
prompts = mcpClient.listPrompts()
prompt = mcpClient.getPrompt( "code-review", { language: "BoxLang" } )
```

### 📚 Learn More

- 📖 **Full Guide**: [MCP Client Documentation](https://ai.ortusbooks.com/advanced/mcp-client.md)
- 🌍 **MCP Ecosystem**: [Model Context Protocol](https://modelcontextprotocol.io)
- 🔧 **Available Servers**: [MCP Servers List](https://github.com/modelcontextprotocol/servers)
- 💻 **Examples**: Check `examples/mcp/` for complete examples

### 🖥️ MCP Server

Expose your **BoxLang functions and data as MCP tools** 🎯 for use by AI agents and applications. Build custom MCP servers that provide tools, prompts, and resources through the standardized Model Context Protocol, making your functionality accessible to any MCP client. 💡

#### 🤔 Why Build MCP Servers?

- 🔌 **Universal Access** - Any MCP client can use your tools
- 🎯 **Standardized Interface** - No custom integration code needed
- 🛠️ **Expose Functionality** - Make BoxLang functions available to AI agents
- 📊 **Share Resources** - Provide data sources, templates, and prompts
- 🏢 **Enterprise Integration** - Connect AI to internal systems safely

#### 💡 Quick Examples

**Simple MCP Server:**

```javascript
// Create server with tools
server = mcpServer(
    name: "my-tools",
    description: "Custom BoxLang tools"
)

// Register tool
server.registerTool(
    aiTool(
        name: "calculate_tax",
        description: "Calculate tax for a given amount",
        callable: ( amount, rate = 0.08 ) => {
            return amount * rate;
        }
    )
)

// Start server
server.start() // Listens on stdio by default
```

**Advanced Server with Resources:**

```javascript
// Create server with tools, prompts, and resources
server = mcpServer(
    name: "enterprise-api",
    description: "Internal enterprise tools"
)

// Register multiple tools
server.registerTool( aiTool(
    name: "query_orders",
    description: "Query customer orders",
    callable: queryOrdersFunction
) )
server.registerTool( aiTool(
    name: "create_invoice",
    description: "Create customer invoice",
    callable: createInvoiceFunction
) )
server.registerTool( aiTool(
    name: "send_notification",
    description: "Send customer notification",
    callable: notifyFunction
) )

// Provide templates as prompts
server.registerPrompt(
    name: "customer-email",
    description: "Generate customer email",
    template: ( orderNumber ) => {
        return "Write a professional email about order ##orderNumber#";
    }
)

// Expose data resources
server.registerResource(
    uri: "config://database",
    description: "Database configuration",
    getData: () => {
        return fileRead( "/config/database.json" );
    }
)

// Start with custom transport
server.start( transport: "http", port: 3000 )
```

**Integration with BoxLang Web App:**

```javascript
// In your BoxLang app's Application.bx
component {
    function onApplicationStart() {
        // Start MCP server on app startup
        application.mcpServer = aiMcpServer( "myapp-api" )
            .registerTool( "search", variables.searchFunction )
            .registerTool( "create", variables.createFunction )
            .start( background: true )
    }

    function onApplicationEnd() {
        application.mcpServer.stop()
    }
}
```

#### 📚 Learn More

- 📖 **Full Guide**: [MCP Server Documentation](https://ai.ortusbooks.com/advanced/mcp-server.md)
- 🌍 **MCP Protocol**: [Model Context Protocol Specification](https://spec.modelcontextprotocol.io)
- 🔧 **Advanced Features**: [Custom Transports & Authentication](https://ai.ortusbooks.com/advanced/mcp-server-advanced.md)
- 💻 **Examples**: Check `examples/mcp/server/` for complete examples

---

## ⚙️ Settings

Here are the settings you can place in your `boxlang.json` file:

```json
{
	"modules" : {
		"bxai" : {
			"settings": {
				// The default provider to use: openai, claude, deepseek, gemini, grok, mistral, ollama, openrouter, perplexity
				"provider" : "openai",
				// The default API Key for the provider
				"apiKey" : "",
				// The default request params to use when calling a provider
				// Ex: { temperature: 0.5, max_tokens: 100, model: "gpt-3.5-turbo" }
				"defaultParams" : {
					// model: "gpt-3.5-turbo"
				},
				// The default timeout of the ai requests
				"timeout" : 30,
				// If true, log request to the ai.log
				"logRequest" : false,
				// If true, log request to the console
				"logRequestToConsole" : false,
				// If true, log the response to the ai.log
				"logResponse" : false,
				// If true, log the response to the console
				"logResponseToConsole" : false,
				// The default return format of the AI response: single, all, raw
				"returnFormat" : "single"
			}
		}
	}
}
```

### 🦙 Ollama Configuration

**Ollama** allows you to run AI models locally on your machine. It's perfect for privacy, offline use, and cost savings. 💰

#### 🔧 Setup Ollama

1. 📥 **Install**: Download from [https://ollama.ai](https://ollama.ai)
2. ⬇️ **Pull a model**: `ollama pull llama3.2` (or any supported model)
3. ▶️ **Start service**: Ollama runs on `http://localhost:11434` by default

### 📝 Configuration

```json
{
	"modules": {
		"bxai": {
			"settings": {
				"provider": "ollama",
				"apiKey": "",  // Optional: for remote/secured Ollama instances
				"chatURL": "http://localhost:11434",  // Default local instance
				"defaultParams": {
					"model": "llama3.2"  // Any Ollama model you have pulled
				}
			}
		}
	}
}
```

### 🌟 Popular Ollama Models

- 🦙 `llama3.2` - Latest Llama model (recommended)
- ⚡ `llama3.2:1b` - Smaller, faster model
- 💻 `codellama` - Code-focused model
- 🎯 `mistral` - High-quality general model
- 🔷 `phi3` - Microsoft's efficient model

## 🛠️ Global Functions (BIFs)

| Function | Purpose | Parameters | Return Type | Async Support |
|----------|---------|------------|-------------|---------------|
| `aiAgent()` | Create autonomous AI agent | `name`, `description`, `instructions`, `model`, `memory`, `tools`, `subAgents`, `params`, `options` | AiAgent Object | ❌ |
| `aiChat()` | Chat with AI provider | `messages`, `params={}`, `options={}` | String/Array/Struct | ❌ |
| `aiChatAsync()` | Async chat with AI provider | `messages`, `params={}`, `options={}` | BoxLang Future | ✅ |
| `aiChatRequest()` | Compose raw chat request | `messages`, `params`, `options`, `headers` | AiRequestObject | N/A |
| `aiChatStream()` | Stream chat responses from AI provider | `messages`, `callback`, `params={}`, `options={}` | void | N/A |
| `aiChunk()` | Split text into chunks | `text`, `options={}` | Array of Strings | N/A |
| `aiDocuments()` | Create fluent document loader | `source`, `config={}` | IDocumentLoader Object | N/A |
| `aiEmbed()` | Generate embeddings | `input`, `params={}`, `options={}` | Array/Struct | N/A |
| `aiMemory()` | Create memory instance | `memory`, `key`, `userId`, `conversationId`, `config={}` | IAiMemory Object | N/A |
| `aiMessage()` | Build message object | `message` | ChatMessage Object | N/A |
| `aiModel()` | Create AI model wrapper | `provider`, `apiKey`, `tools` | AiModel Object | N/A |
| `aiPopulate()` | Populate class/struct from JSON | `target`, `data` | Populated Object | N/A |
| `aiService()` | Create AI service provider | `provider`, `apiKey` | IService Object | N/A |
| `aiTokens()` | Estimate token count | `text`, `options={}` | Numeric | N/A |
| `aiTool()` | Create tool for real-time processing | `name`, `description`, `callable` | Tool Object | N/A |
| `aiTransform()` | Create data transformer | `transformer`, `config={}` | Transformer Runnable | N/A |
| `MCP()` | Create MCP client for Model Context Protocol servers | `baseURL` | MCPClient Object | N/A |
| `mcpServer()` | Get or create MCP server for exposing tools | `name="default"`, `description`, `version`, `cors`, `statsEnabled`, `force` | MCPServer Object | N/A |

> **Note on Return Formats:** When using pipelines (runnable chains), the default return format is `raw` (full API response), giving you access to all metadata. Use `.singleMessage()`, `.allMessages()`, or `.withFormat()` to extract specific data. The `aiChat()` BIF defaults to `single` format (content string) for convenience. See the [Pipeline Return Formats](https://ai.ortusbooks.com/main-components/overview.md#return-formats) documentation for details.

## 📢 Events

The BoxLang AI module emits several events throughout the AI processing lifecycle that allow you to intercept, modify, or extend functionality. These events are useful for logging, debugging, custom providers, and response processing.

Read more about [Events in BoxLang AI](https://ai.ortusbooks.com/advanced/events).

### Event Reference Table

| Event | When Fired | Data Emitted | Use Cases |
|-------|------------|--------------|-----------|
| `afterAIAgentRun` | After agent completes execution | `agent`, `response` | Agent monitoring, result tracking |
| `afterAIEmbed` | After generating embeddings | `embeddingRequest`, `service`, `result` | Result processing, caching |
| `afterAIModelInvoke` | After model invocation completes | `model`, `aiRequest`, `results` | Performance tracking, validation |
| `afterAIPipelineRun` | After pipeline execution completes | `sequence`, `result`, `executionTime` | Pipeline monitoring, metrics |
| `afterAIToolExecute` | After tool execution completes | `tool`, `results`, `executionTime` | Tool performance tracking |
| `beforeAIAgentRun` | Before agent starts execution | `agent`, `input`, `messages`, `params` | Agent validation, preprocessing |
| `beforeAIEmbed` | Before generating embeddings | `embeddingRequest`, `service` | Request validation, preprocessing |
| `beforeAIModelInvoke` | Before model invocation starts | `model`, `aiRequest` | Request validation, cost estimation |
| `beforeAIPipelineRun` | Before pipeline execution starts | `sequence`, `stepCount`, `steps`, `input` | Pipeline validation, tracking |
| `beforeAIToolExecute` | Before tool execution starts | `tool`, `name`, `arguments` | Permission checks, validation |
| `onAIAgentCreate` | When agent is created | `agent` | Agent registration, configuration |
| `onAIEmbedRequest` | Before sending embedding request | `dataPacket`, `embeddingRequest`, `provider` | Request logging, modification |
| `onAIEmbedResponse` | After receiving embedding response | `embeddingRequest`, `response`, `provider` | Response processing, caching |
| `onAIError` | When AI operation error occurs | `error`, `errorMessage`, `provider`, `operation`, `canRetry` | Error handling, retry logic, alerts |
| `onAiMemoryCreate` | When memory instance is created | `memory`, `type`, `config` | Memory configuration, tracking |
| `onAIMessageCreate` | When message is created | `message` | Message validation, formatting |
| `onAIModelCreate` | When model wrapper is created | `model`, `service` | Model configuration, tracking |
| `onAIProviderCreate` | After provider is created | `provider` | Provider initialization, configuration |
| `onAIProviderRequest` | When provider is requested | `provider`, `apiKey`, `service` | Custom provider registration |
| `onAIRateLimitHit` | When rate limit (429) is encountered | `provider`, `statusCode`, `retryAfter` | Rate limit handling, provider switching |
| `onAIRequest` | Before sending HTTP request | `dataPacket`, `aiRequest`, `provider` | Request logging, modification, authentication |
| `onAIRequestCreate` | When request object is created | `aiRequest` | Request validation, modification |
| `onAIResponse` | After receiving HTTP response | `aiRequest`, `response`, `rawResponse`, `provider` | Response processing, logging, caching |
| `onAITokenCount` | When token usage data is available | `provider`, `model`, `promptTokens`, `completionTokens`, `totalTokens`, `tenantId`, `usageMetadata`, `providerOptions`, `timestamp` | Cost tracking, budget enforcement, multi-tenant billing |
| `onAIToolCreate` | When tool is created | `tool`, `name`, `description` | Tool registration, validation |
| `onAITransformerCreate` | When transformer is created | `transform` | Transform configuration, tracking |

## 🌐 GitHub Repository and Reporting Issues

Visit the [GitHub repository](https://github.com/ortus-boxlang/bx-ai) for release notes. You can also file a bug report or improvement suggestion  via [GitHub Issues](https://github.com/ortus-boxlang/bx-ai/issues).

## 🧪 Testing

This module includes tests for all AI providers. To run the tests:

```bash
./gradlew test
```

### Ollama Testing

For Ollama provider tests, you need to start the test Ollama service first:

```bash
# Start the Ollama test service
docker-compose up -d ollama-test

# Wait for it to be ready (this may take a few minutes for the first run)
# The service will automatically pull the qwen2.5:0.5b model

# Run the tests
./gradlew test --tests "ortus.boxlang.ai.providers.OllamaTest"

# Clean up when done
docker-compose down -v
```

You can also use the provided test script:

```bash
./test-ollama.sh
```

This will start the service, verify it's working, and run a basic test.

**Note**: The first time you run this, it will download the `qwen2.5:0.5b` model (~500MB), so it may take several minutes.

## 💖 Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://ai.ortussolutions.com). Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more. If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

> "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
