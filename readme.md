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

**BoxLang AI** eliminates vendor lock-in and simplifies AI integration by providing a single, consistent interface across **16+ AI providers**. Whether you're using OpenAI, Claude, Gemini, Grok, DeepSeek, MiniMax, Ollama, or Perplexity—your code stays the same. Switch providers, combine models, and orchestrate complex workflows with simple configuration changes. 🔄

## ✨ Key Features

- 🔌 **16+ AI Providers** - Single API for OpenAI, Claude, AWS Bedrock, Gemini, Grok, MiniMax, Ollama, DeepSeek, and more
- 🤖 **AI Agents** - Autonomous agents with memory, tools, sub-agents, and multi-step reasoning
- 🔒 **Multi-Tenant Memory** - Enterprise-grade isolation with 20+ memory types (standard + vector)
- 🧬 **Vector Memory & RAG** - 12 vector databases with semantic search (ChromaDB, Pinecone, PostgreSQL, OpenSearch, etc.)
- 📚 **Document Loaders** - 30+ file formats including PDF, Word, CSV, JSON, XML, web scraping, and databases
- 🛠️ **Real-Time Tools** - Function calling for APIs, databases, and external system integration
- 🌊 **Streaming Support** - Real-time token streaming through pipelines for responsive applications
- 📦 **Structured Output** - Type-safe responses using BoxLang classes, structs, or JSON schemas
- 🔗 **AI Pipelines** - Composable workflows with models, transformers, and custom logic
- 🧩 **Middleware** - Cross-cutting controls for agents/models (logging, retries, guardrails, approval, and replay)
- 🎓 **AI Skills** - Reusable, composable knowledge blocks following the [Claude Agent Skills](https://www.anthropic.com/news/agent-skills) open standard for modular agent behavior
- 📡 **MCP Protocol** - Build and consume Model Context Protocol servers for distributed AI
- 💬 **Fluent Interface** - Chainable, expressive syntax that makes AI integration intuitive
- 🦙 **Local AI** - Full Ollama support for privacy, offline use, and zero API costs
- ⚡ **Async Operations** - Non-blocking `runAsync()` on every runnable; `aiParallel()` for concurrent parallel pipelines
- 🎯 **Event-Driven** - 35+ lifecycle events for logging, monitoring, and custom workflows
- 🏭 **Production-Ready** - Timeout controls, error handling, rate limiting, and debugging tools
- 🧪 **Testable** - Deterministic replay for reliable unit and integration testing

## 📃 Table of Contents

- [📄 License](#-license)
- [🚀 Getting Started](#-getting-started)
- [🤖 Supported Providers](#-supported-providers)
  - [📊 Provider Support Matrix](#-provider-support-matrix)
  - [🔍 Provider Capability Discovery](#-provider-capability-discovery)
- [📤 Return Formats](#-return-formats)
- [🥊 Quick Overview](#-quick-overview)
  - [💬 Chats](#-chats)
  - [🔗 Pipelines](#-pipelines)
  - [🧩 Middleware](#-middleware)
  - [🗂️ Tool Registry](#️-tool-registry)
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

**📚 New to AI concepts?** Check out our [Key Concepts Guide](https://ai.ortusbooks.com/getting-started/concepts) for terminology and fundamentals, or browse our [FAQ](https://ai.ortusbooks.com/readme/faq) for quick answers to common questions.  We also have a [Quick Start Guide](https://ai.ortusbooks.com/getting-started/quickstart) and our intense [AI BootCamp](https://github.com/ortus-boxlang/bx-ai-bootcamp) available to you as well.

### OS

You can easily get started with BoxLang AI by using the module installer for building operating system applications:

```bash
install-bx-module bx-ai
```

This will install the latest version of the BoxLang AI module in your BoxLang environment. Once installed, configure your default AI provider and API key in `boxlang.json`:

```json
{
    "modules": {
        "bxai": {
            "settings": {
                "provider": "openai",
                "apiKey": "${OPENAI_API_KEY}"
            }
        }
    }
}
```

> 💡 **Tip:** Use environment variable placeholders like `${OPENAI_API_KEY}` so you never commit secrets to source control. Each provider also auto-detects its own env var (e.g. `OPENAI_API_KEY`, `CLAUDE_API_KEY`, `GEMINI_API_KEY`).

### ⚙️ All Available Settings

Below is the full reference of every setting you can place under `settings` in `boxlang.json`:

```json
{
    "modules": {
        "bxai": {
            "settings": {
                "provider": "openai",
                "apiKey": "${OPENAI_API_KEY}",

                "defaultParams": {
                    "model": "gpt-4o",
                    "temperature": 0.7,
                    "max_tokens": 2000
                },

                "memory": {
                    "provider": "window",
                    "config": {
                        "maxMessages": 20
                    }
                },

                "providers": {
                    "openai": {
                        "params": { "model": "gpt-4o", "temperature": 0.7 },
                        "options": { "timeout": 60 }
                    },
                    "claude": {
                        "params": { "model": "claude-3-5-sonnet-20241022" }
                    },
                    "ollama": {
                        "params": { "model": "qwen2.5:0.5b-instruct" }
                    }
                },

                "timeout": 45,

                "logRequest": false,
                "logRequestToConsole": false,
                "logResponse": false,
                "logResponseToConsole": false,

                "returnFormat": "single",

                "skillsDirectory": "/.ai/skills",
                "autoLoadSkills": true,
                "globalSkills": []
            }
        }
    }
}
```

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `provider` | `string` | `"openai"` | Default AI provider to use for all requests |
| `apiKey` | `string` | `""` | Default API key; each provider also reads its own env var (e.g. `OPENAI_API_KEY`) |
| `defaultParams` | `struct` | `{}` | Default request parameters sent to every provider (e.g. `model`, `temperature`, `max_tokens`) |
| `memory.provider` | `string` | `"window"` | Default memory type: `window`, `cache`, `file`, `session`, `summary`, `jdbc`, `hybrid`, or any vector provider |
| `memory.config` | `struct` | `{}` | Provider-specific memory configuration (e.g. `maxMessages`, `cacheName`) |
| `providers` | `struct` | `{}` | Per-provider overrides — keys are provider names, values have `params` and `options` structs |
| `timeout` | `numeric` | `45` | Default HTTP request timeout in seconds |
| `logRequest` | `boolean` | `false` | Log outgoing AI requests to `ai.log` |
| `logRequestToConsole` | `boolean` | `false` | Print outgoing AI requests to the console (useful for debugging) |
| `logResponse` | `boolean` | `false` | Log AI responses to `ai.log` |
| `logResponseToConsole` | `boolean` | `false` | Print AI responses to the console (useful for debugging) |
| `returnFormat` | `string` | `"single"` | Default response format: `single`, `all`, `raw`, `json`, `xml`, or `structuredOutput` |
| `skillsDirectory` | `string` | `"/.ai/skills"` | Directory scanned for `SKILL.md` files at startup. Set to `""` to disable auto-discovery |
| `autoLoadSkills` | `boolean` | `true` | When `true`, skills found in `skillsDirectory` are auto-loaded and injected into every `aiAgent()` as global skills |
| `globalSkills` | `array` | `[]` | Internal — populated at startup with auto-discovered skills; access via `aiGlobalSkills()` |

After that you can leverage the global functions (BIFs) in your BoxLang code.  Here is a simple example:

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
- 🎙️ [ElevenLabs](https://elevenlabs.io/) - Premium text-to-speech and speech-to-text
- 💎 [Gemini](https://gemini.google.com/)
- ⚡ [Grok](https://grok.com/)
- 🚀 [Groq](https://groq.com/)
- 🤗 [HuggingFace](https://huggingface.co/)
- 🌀 [Mistral](https://mistral.ai/)
- 🌟 [MiniMax](https://platform.minimax.io/)
- 🦙 [Ollama](https://ollama.ai/)
- 🟢 [OpenAI](https://www.openai.com/)
- 🔌 [OpenAI-Compatible](https://platform.openai.com/docs/api-reference) - Any OpenAI-compatible API
- 🔀 [OpenRouter](https://openrouter.ai/)
- 🔮 [Perplexity](https://docs.perplexity.ai/)
- 🚢 [Voyage AI](https://www.voyageai.com/)

### 📊 Provider Support Matrix

Here is a matrix of the providers and their feature support. Please keep checking as we will be adding more providers and features to this module. 🔄

| Provider            | Chat & Streaming | Real-time Tools | Embeddings       | TTS (Speech)     | STT (Transcription) |
|---------------------|------------------|-----------------|------------------|------------------|---------------------|
| AWS Bedrock         | ✅               | ✅              | ✅               | ❌               | ❌                  |
| Claude              | ✅               | ✅              | ❌               | ❌               | ❌                  |
| Cohere              | ✅               | ✅              | ✅               | ❌               | ❌                  |
| DeepSeek            | ✅               | ✅              | ✅               | ❌               | ❌                  |
| Docker Model Runner | ✅               | ✅              | ✅               | ❌               | ❌                  |
| ElevenLabs          | ❌               | ❌              | ❌               | ✅ (Premium)     | ✅ (Scribe v1)      |
| Gemini              | ✅               | [Coming Soon]   | ✅               | ✅               | ✅                  |
| Grok                | ✅               | ✅              | ✅               | ✅               | ❌                  |
| Groq                | ✅               | ✅              | ✅               | ❌               | ✅ (Whisper)        |
| HuggingFace         | ✅               | ✅              | ✅               | ❌               | ❌                  |
| Mistral             | ✅               | ✅              | ✅               | ✅ (Voxtral)     | ✅ (Voxtral)        |
| MiniMax             | ✅               | ✅              | ✅               | ❌               | ❌                  |
| Ollama              | ✅               | ✅              | ✅               | ❌               | ❌                  |
| OpenAI              | ✅               | ✅              | ✅               | ✅               | ✅ (Whisper)        |
| OpenAI-Compatible   | ✅               | ✅              | ✅               | ❌               | ❌                  |
| OpenRouter          | ✅               | ✅              | ✅               | ❌               | ❌                  |
| Perplexity          | ✅               | ✅              | ❌               | ❌               | ❌                  |
| Voyage              | ❌               | ❌              | ✅ (Specialized) | ❌               | ❌                  |

### 🔍 Provider Capability Discovery

Every provider exposes a **runtime capability API** so you can introspect what it supports without consulting documentation — and without risking cryptic errors when you call an unsupported operation. 🛡️

```javascript
// Get all capabilities a provider supports
var provider = aiService( "openai" );
var caps = provider.getCapabilities();
// → [ "chat", "stream", "embeddings" ]

// Check a specific capability before using it
if ( provider.hasCapability( "embeddings" ) ) {
    var embedding = aiEmbed( "Hello world" );
}

// Voyage is embeddings-only — getCapabilities() reflects this
var voyage = aiService( "voyage" );
voyage.getCapabilities(); // → [ "embeddings" ]
voyage.hasCapability( "chat" ); // → false
```

The built-in BIFs (`aiChat`, `aiChatStream`, `aiEmbed`) automatically use this system and throw a clear `UnsupportedCapability` exception when the selected provider does not implement the required capability:

```javascript
// This will throw UnsupportedCapability — Voyage has no chat capability
aiChat( "Hello?", provider: "voyage" );

// This will throw UnsupportedCapability — Claude has no embeddings capability
aiEmbed( "some text", provider: "claude" );
```

Capabilities map to the following **capability interfaces** (in `models/providers/capabilities/`):

| Capability String | Interface                  | Methods Provided                         |
|-------------------|----------------------------|-------------------------------------------|
| `chat`, `stream`  | `IAiChatService`           | `chat()`, `chatStream()`                  |
| `embeddings`      | `IAiEmbeddingsService`     | `embeddings()`                            |
| `speech`          | `IAiSpeechService`         | `speak()`                                 |
| `transcription`   | `IAiTranscriptionService`  | `transcribe()`, `translate()`             |

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

In the following sections, we provide a quick overview of the main components of BoxLang AI including Chats, Pipelines, Middleware, Agents, Structured Output, Memory Systems, Document Loaders & RAG, and MCP Client/Server. Each section includes quick examples and links to more detailed documentation.  For further details, please refer to the [official documentation](https://ai.ortusbooks.com/), this is just a high-level overview to get you started quickly. 🚀

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

**Parallel Pipelines:**

Run multiple runnables concurrently with the same input and receive a named struct of results. Mirrors LangChain's `RunnableParallel` — parallelism is a developer/framework concern, not something the LLM decides.

```javascript
// Fan out to multiple agents/models in parallel
results = aiParallel({
    summary:  summaryAgent,
    analysis: analysisAgent,
    keywords: keywordModel
}).run( "Some long document..." )

// results.summary, results.analysis, results.keywords — all ran concurrently

// Compose in a pipeline — parallel branch then merge
pipeline = aiMessage( "Analyze: ${text}" )
    .to( aiParallel({ researcher: researchAgent, writer: writerAgent }) )
    .transform( r => "Research: #r.researcher#\nDraft: #r.writer#" )

// Or dispatch the same agent for multiple independent inputs asynchronously
futures = [
    researchAgent.runAsync( "Topic A" ),
    researchAgent.runAsync( "Topic B" ),
    researchAgent.runAsync( "Topic C" )
]
results = futures.map( f => f.get() )
```

#### 📚 Learn More

- 📖 **Full Guide**: [Runnables & Pipelines](https://ai.ortusbooks.com/main-components/runnables.md)
- 🎯 **Overview**: [Main Components](https://ai.ortusbooks.com/main-components/overview.md)
- 🔧 **Custom Runnables**: [Building Custom Components](https://ai.ortusbooks.com/advanced/custom-runnables.md)
- 💻 **Examples**: Check `examples/pipelines/` for complete examples

----

### 🧩 Middleware

Add **cross-cutting behavior** around model and agent execution without changing your business logic. Use middleware for observability, reliability, safety, approvals, and deterministic replay in testing. 🎯

#### 🤔 Why Use Middleware?

- 🔍 **Observability** - Capture lifecycle logs and execution traces
- ♻️ **Reliability** - Retry transient model/tool failures
- 🛡️ **Safety** - Block risky tools or argument patterns
- 👤 **Human Approval** - Require confirmation for sensitive tool calls
- 🎬 **Replayability** - Record and replay runs for regression testing

#### 📦 Core Middleware Included

| Middleware | Purpose |
|------------|---------|
| `LoggingMiddleware` | Logs agent/model lifecycle activity for observability and troubleshooting. |
| `RetryMiddleware` | Retries transient LLM/tool failures with configurable backoff. |
| `MaxToolCallsMiddleware` | Enforces a per-run cap on total tool calls to prevent runaway execution. |
| `GuardrailMiddleware` | Blocks disallowed tools and rejects risky tool arguments by pattern rules. |
| `HumanInTheLoopMiddleware` | Requires human approval for selected tool calls (CLI or suspend/resume flow). |
| `FlightRecorderMiddleware` | Records and replays LLM/tool interactions for deterministic testing and CI. |

#### 📌 Registration

Middleware can be attached to **agents**, **models**, or both. They are executed in the order registered; `after*` hooks fire in reverse order (cleanup order).

**Via `aiAgent()` / `aiModel()` BIF parameter:**

```javascript
import bxModules.bxai.models.middleware.core.LoggingMiddleware;
import bxModules.bxai.models.middleware.core.RetryMiddleware;
import bxModules.bxai.models.middleware.core.GuardrailMiddleware;

agent = aiAgent(
    name: "Safe Assistant",
    middleware: [
        new LoggingMiddleware( logToConsole: true ),
        new RetryMiddleware( maxRetries: 2, initialDelay: 250 ),
        new GuardrailMiddleware( blockedTools: [ "deleteRecord" ] )
    ]
)
```

**Via `withMiddleware()` on a runnable (fluent API):**

```javascript
agent
    .withMiddleware( new LoggingMiddleware() )
    .withMiddleware( new RetryMiddleware( maxRetries: 3 ) )

// Or pass an array — flattened automatically
agent.withMiddleware( [ mw1, mw2, mw3 ] )
```

**Management methods** (available on `AiAgent` and `AiModel`):

| Method | Returns | Description |
|--------|---------|-------------|
| `withMiddleware( any middleware )` | `this` | Add one or more middleware (instance, struct, or array) |
| `clearMiddleware()` | `this` | Remove all registered middleware |
| `listMiddleware()` | `array` | Return array of `{ name, description }` for all middleware |

When an agent runs, its middleware is **prepended** to any middleware already on the model, so agent-level hooks always fire first.

---

#### 🪝 Hooks Reference

Middleware exposes two hook styles:

**Sequential hooks** — called in order; return `AiMiddlewareResult`. Chain stops if any hook returns a terminal result.

| Hook | Fires | Direction |
|------|-------|-----------|
| `beforeAgentRun( context )` | Before agent starts | Forward |
| `afterAgentRun( context )` | After agent completes | **Reverse** |
| `beforeLLMCall( context )` | Before each LLM provider call | Forward |
| `afterLLMCall( context )` | After each LLM provider call | **Reverse** |
| `beforeToolCall( context )` | Before each tool is invoked | Forward |
| `afterToolCall( context )` | After each tool returns | **Reverse** |
| `onError( context )` | When any hook throws an exception | — |

**Wrap hooks** — called as nested closures; call `handler()` to proceed and return a value.

| Hook | Purpose |
|------|---------|
| `wrapLLMCall( context, handler )` | Surround each LLM provider call (retry, caching, tracing) |
| `wrapToolCall( context, handler )` | Surround each tool invocation (retry, mocking, sandboxing) |

For wrap hooks, the first registered middleware is the outermost wrapper:

```
mw1.wrapLLMCall( ctx, () =>
    mw2.wrapLLMCall( ctx, () =>
        actualProviderCall()
    )
)
```

---

#### 📬 AiMiddlewareResult

Every sequential hook must return an `AiMiddlewareResult`. Use the static factory methods:

```javascript
import bxModules.bxai.models.middleware.AiMiddlewareResult;

// Continue the chain normally
return AiMiddlewareResult.continue()

// Stop the chain immediately (terminal)
return AiMiddlewareResult.cancel( "Too many sensitive operations" )

// Human approved (HITL)
return AiMiddlewareResult.approve()

// Human rejected (terminal)
return AiMiddlewareResult.reject( "Operator rejected this action" )

// Human edited the tool arguments (passes modified args to tool)
return AiMiddlewareResult.edit( { correctedArgs: { query: "safe query" } } )

// Suspend for async human review (terminal — web mode HITL)
return AiMiddlewareResult.suspend( { toolName: "deleteRecord", args: toolArgs } )
```

**Checking results:**

| Predicate | Meaning |
|-----------|---------|
| `isContinue()` | Chain proceeds normally |
| `isCancelled()` | Chain was stopped (terminal) |
| `isApproved()` | Human approved |
| `isRejected()` | Human rejected (terminal) |
| `isEdit()` | Arguments were modified |
| `isSuspended()` | Waiting for async human input (terminal) |
| `isTerminal()` | `cancel`, `reject`, or `suspend` — stops the chain |

---

#### 📚 Learn More

- 📖 **Full Guide**: [Middleware Documentation](https://ai.ortusbooks.com/main-components/middleware)
- 💻 **Examples**: Check `examples/agents/` and `examples/pipelines/`

----

### 🗂️ Tool Registry

The **AI Tool Registry** is a global singleton that stores named `ITool` instances. Register tools once — at module load, on application start, or anywhere in your code — and then reference them by string name wherever tools are accepted. This decouples tool definitions from call sites and makes it easy to share tools across agents, models, and pipelines. 🎯

#### 🤔 Why Use the Registry?

- 🔑 **By-name references** — pass `"now@bxai"` as a string instead of a live object
- 📦 **Module scoping** — namespace tools as `toolName@moduleName` to avoid collisions
- 🔍 **Lazy resolution** — tools are resolved to `ITool` instances right before each LLM request
- 🔌 **Auto-scanning** — annotate methods with `@AITool` and call `scan()` to register them all at once
- ⚡ **Built-in tools** — `now@bxai` (current date/time), `speak@bxai` (text-to-speech), `transcribe@bxai` (speech-to-text), and `translate@bxai` (audio-to-English) are registered automatically on module load

#### 💡 Quick Examples

**Using the registry:**

```javascript
// Register a tool once (e.g., in Application.bx or a module's onLoad)
aiToolRegistry().register(
    name        : "searchProducts",
    description : "Search the product catalog",
    callback    : ( required string query ) => productService.search( query )
)

// Later, reference by name — no object needed
result = aiChat(
    "Find me wireless headphones under $50",
    { tools: [ "searchProducts" ] }
)
```

**Module-namespaced tools:**

```javascript
// Namespaced registration avoids collisions across modules
aiToolRegistry().register(
    name        : "lookup",
    description : "Look up a customer by ID",
    callback    : id => customerService.find( id ),
    module      : "my-app"
)

// Retrieve by full key or bare name (auto-resolved if unambiguous)
var tool = aiToolRegistry().get( "lookup@my-app" )
var tool = aiToolRegistry().get( "lookup" )       // works if only one "lookup" exists
```

**Scanning a class for `@AITool` annotations:**

```javascript
// Annotate methods in a class
class WeatherTools {
    @AITool( "Get current weather for a city" )
    public string function getWeather( required string city ) {
        return weatherAPI.fetch( city )
    }

    @AITool( "Get a 7-day forecast for a city" )
    public string function getForecast( required string city ) {
        return weatherAPI.forecast( city )
    }
}

// Register all annotated methods in one call
aiToolRegistry().scan( new WeatherTools(), "my-module" )
// Registered as: getWeather@my-module, getForecast@my-module
```

**Using the built-in `now@bxai` tool:**

```javascript
// Auto-registered on module load — just reference it by name
result = aiChat(
    "What should I have for dinner tonight?",
    { tools: [ "now@bxai" ] }
)
// AI knows the current date/time without any extra wiring
```

**Using the built-in audio tools (`speak@bxai`, `transcribe@bxai`, `translate@bxai`):**

```javascript
// All three are auto-registered on module load — opt in by name
var agent = aiAgent(
    name         : "VoiceAssistant",
    instructions : "You are a helpful voice assistant. Speak responses aloud.",
    tools        : [ "now@bxai", "speak@bxai", "transcribe@bxai", "translate@bxai" ]
)

// Agent can now convert text to speech, transcribe audio files, or translate audio to English
agent.run( "Say hello to the user and tell them today's date" )
// → AI calls speak@bxai with the greeting text, returns the saved audio file path

// Standalone transcription — agent calls transcribe@bxai automatically
agent.run( "Please transcribe the file at /recordings/meeting.mp3" )

// Translation — agent calls translate@bxai for non-English audio
agent.run( "Translate the Spanish audio at /audio/mensaje.mp3 to English" )
```

**Opt-in `httpGet` tool (NOT auto-registered):**

```javascript
// Register explicitly when your application needs web access
import bxModules.bxai.models.tools.core.CoreTools;
aiToolRegistry().scan( new CoreTools(), "bxai" )  // registers httpGet@bxai too
```

#### 🧑‍💻 Custom Tools via `BaseTool`

For more complex tools ones that need their own state, unit tests, or reusable class structure extend `BaseTool` directly instead of using a closure. You only need to implement two abstract methods:

| Method | Purpose |
|--------|---------|
| `doInvoke( required struct args, AiChatRequest chatRequest )` | The tool logic. Return any value serialization is handled automatically. |
| `generateSchema()` | Return the OpenAI function-calling schema struct. Called by `getSchema()` unless a manual schema override has been set. |

`invoke()` is `final` on `BaseTool` it fires the `beforeAIToolExecute` / `afterAIToolExecute` events and serializes the result before calling your `doInvoke()`, so you never need to wire those up yourself.

```javascript
// MySearchTool.bx
class extends="bxModules.bxai.models.tools.BaseTool" {

    property name="searchClient";

    function init( required any searchClient ) {
        variables.name        = "searchProducts"
        variables.description = "Search the product catalog and return matching items"
        variables.searchClient = arguments.searchClient
        return this
    }

    /**
     * Core tool logic return any type, BaseTool serializes it automatically.
     */
    public any function doInvoke( required struct args, AiChatRequest chatRequest ) {
        return variables.searchClient.search(
            query      : args.query,
            maxResults : args.maxResults ?: 5
        )
    }

    /**
     * OpenAI function-calling schema for this tool.
     */
    public struct function generateSchema() {
        return {
            "type": "function",
            "function": {
                "name"       : variables.name,
                "description": variables.description,
                "parameters" : {
                    "type"      : "object",
                    "properties": {
                        "query"     : { "type": "string",  "description": "Search query text" },
                        "maxResults": { "type": "integer", "description": "Maximum number of results to return" }
                    },
                    "required": [ "query" ]
                }
            }
        }
    }
}
```

Register and use it like any other tool:

```javascript
// Register in the global registry
aiToolRegistry().register( new MySearchTool( searchClient ), "my-app" )

// Reference by key name anywhere tools are accepted
result = aiChat( "Find wireless headphones", { tools: [ "searchProducts@my-app" ] } )
```

**Fluent schema helpers** (inherited from `BaseTool`) let you skip writing `generateSchema()` manually when `ClosureTool`'s auto-introspection isn't available:

```javascript
tool = new MySearchTool( client )
    .describeFunction( "Search the product catalog" )   // sets description
    .describeQuery( "Search term to look up" )           // describeArg( "query", "..." )
    .describeMaxResults( "Max items to return" )         // describeArg( "maxResults", "..." )
```

Or supply a fully hand-crafted schema with `setSchema( schemaStruct )` when set, it takes precedence over `generateSchema()`.

#### 📚 Learn More

- 📖 **Full Guide**: [AI Tool Registry Documentation](https://ai.ortusbooks.com/main-components/tool-registry)
- 💻 **Examples**: Check `examples/advanced/` for complete registry examples

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

**Multi-Agent Hierarchy (Sub-Agents):**

```javascript
// Create specialist sub-agents
researchAgent = aiAgent(
    name: "researcher",
    description: "Researches topics in depth",
    instructions: "Provide thorough research summaries"
)

writerAgent = aiAgent(
    name: "writer",
    description: "Writes polished content",
    instructions: "Turn research into engaging articles"
)

// Coordinator automatically registers sub-agents as callable tools
coordinator = aiAgent(
    name: "coordinator",
    description: "Orchestrates research and writing",
    subAgents: [ researchAgent, writerAgent ]
)

// Coordinator decides when to delegate
coordinator.run( "Write an article about BoxLang AI" )

// Inspect the hierarchy
writeln( researchAgent.getAgentPath() )   // /coordinator/researcher
writeln( researchAgent.getAgentDepth() )  // 1
writeln( researchAgent.isRootAgent() )    // false
writeln( coordinator.getRootAgent().getAgentName() ) // coordinator
```

#### 📚 Learn More

- 📖 **Full Guide**: [AI Agents Documentation](https://ai.ortusbooks.com/main-components/agents.md)
- 🎓 **Interactive Course**: [Lesson 6 - Building AI Agents](course/lesson-06-agents/)
- 🔧 **Advanced Patterns**: [Agent Orchestration](https://ai.ortusbooks.com/advanced/agent-orchestration.md)
- 💻 **Examples**: Check `examples/agents/` for complete working examples

----

### 🎯 AI Skills

Give agents and models reusable, composable **knowledge blocks** 📚 that can be injected into the system message at runtime. Skills follow the [Claude Agent Skills open standard](https://www.anthropic.com/news/agent-skills) — a `description` field tells the LLM when to apply the skill, and the body contains the full instructions. 🧩

#### 🤔 Why Use Skills?

- 📖 **Reusable knowledge** - Define domain expertise once, share across many agents and models
- 🗂️ **File-based management** - Store skills as `SKILL.md` files in your project, commit alongside code
- ⚡ **Two loading modes** - Always-on for universal guidance; lazy-loaded for large skill libraries
- 🔌 **Zero-code discovery** - Drop a `SKILL.md` into `.ai/skills/my-skill/` and it's available automatically
- 🌐 **Global skill pool** - Register global skills once in module config, automatically available to all agents

#### 📋 Skill File Format

Skills live in named subdirectories under `.ai/skills/`:

```
.ai/skills/
    sql-optimizer/
        SKILL.md
    boxlang-expert/
        SKILL.md
    customer-tone/
        SKILL.md
```

Each `SKILL.md` file uses optional YAML frontmatter and a Markdown body:

```markdown
---
description: Optimise SQL queries for maximum performance. Apply when writing or reviewing database queries.
---

## SQL Optimisation Rules

- Always use indexed columns in WHERE clauses
- Prefer JOINs over subqueries for large datasets
- Use EXPLAIN to verify query plans before deploying
- Avoid SELECT * in production queries
```

> **Tip:** If you omit the frontmatter, the first paragraph of the body is used as the `description`.

#### 💡 Quick Examples

**Inline skill on a model:**

```javascript
// Create an inline skill (no files needed)
sqlSkill = aiSkill(
    name       : "sql-optimizer",
    description: "Apply SQL optimisation rules when writing or reviewing queries",
    content    : "Always use indexed columns. Prefer JOINs over subqueries."
)

// Always-on: injected into every call
model = aiModel( "openai" ).withSkills( [ sqlSkill ] )
response = model.run( "Write a query to get all orders" )
```

**Load skills from the filesystem:**

```javascript
// Load all SKILL.md files from .ai/skills/ (recursive by default)
skills = aiSkill( ".ai/skills" )

// Or load a single skill file
sqlSkill = aiSkill( ".ai/skills/sql-optimizer/SKILL.md" )

// Seed an agent with all discovered skills
agent = aiAgent(
    name           : "data-assistant",
    availableSkills: skills    // Lazy pool — LLM loads on demand
)
```

**Always-on vs lazy skills:**

```javascript
// Always-on: full content injected every call (small, universal skills)
coreSkill = aiSkill( ".ai/skills/writing-style/SKILL.md" )
agent.withSkills( [ coreSkill ] )

// Lazy pool: only a compact index is included; LLM calls loadSkill() as needed
bigLibrary = aiSkill( ".ai/skills" )   // Hundreds of skills
agent.withAvailableSkills( bigLibrary )

// activateSkill() promotes a lazy skill to always-on mid-session
agent.activateSkill( "sql-optimizer" )
```

**Global skills auto-injected into every agent:**

```javascript
// In ModuleConfig.bx settings — all agents get these automatically
settings = {
    globalSkills: aiSkill( expandPath( ".ai/skills" ) )
}

// Or register programmatically via the BIF
globalSkillPool = aiGlobalSkills()   // returns the current global pool
```

**Inspect skill state:**

```javascript
config = agent.getConfig()
writeln( config.activeSkillCount )         // always-on skills count
writeln( config.availableSkillCount )      // lazy skills count

// Render the full skill system-message block for debugging
writeln( agent.buildSkillsContent() )
```

#### 📚 Learn More

- 💻 **Examples**: Check `examples/skills/` for complete working examples

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

### 🔊 Audio — Speech & Transcription

BoxLang AI provides three dedicated BIFs for **voice and audio AI** — convert text to natural-sounding speech, transcribe audio to text, and translate non-English audio to English. 🎙️

#### 🗣️ Text-to-Speech (TTS)

```javascript
// Convert text to speech and save as MP3
var audioPath = aiSpeak(
    text   : "BoxLang makes AI simple and expressive.",
    options: { outputFile: "/tmp/welcome.mp3" }
)

// Or get an AiSpeechResponse for programmatic access
var response = aiSpeak( text: "Hello World", params: { voice: "nova" } )
response.saveToFile( "/tmp/hello.mp3" )
println( "Audio size: " & response.getSize() & " bytes" )
```

#### 🎤 Speech-to-Text (STT)

```javascript
// Transcribe audio (returns text string by default)
var transcript = aiTranscribe( audio: "/path/to/audio.mp3" )
println( transcript )

// Get the full response object with metadata
var response = aiTranscribe(
    audio  : "/path/to/audio.mp3",
    options: { returnFormat: "response" }
)
println( "Text: " & response.getText() )

// Translate non-English audio to English
var englishText = aiTranslate( audio: "/path/to/spanish-audio.mp3" )
```

> **Provider Support:** OpenAI (TTS + STT), Mistral/Voxtral (TTS + STT), Groq/Whisper (STT + translation), xAI/Grok (TTS), Gemini (TTS + STT), ElevenLabs (premium TTS + STT). Use `provider: "elevenlabs"` in options for ElevenLabs.

#### ✅ Supported Audio Providers

| Provider | TTS | STT | Translation |
|----------|-----|-----|-------------|
| OpenAI | ✅ (`tts-1`, `tts-1-hd`) | ✅ (Whisper) | ✅ |
| ElevenLabs | ✅ (multilingual v2) | ✅ (Scribe) | ✅ (via transcribe) |
| Mistral | ✅ (Voxtral) | ✅ (Voxtral) | ❌ |
| Gemini | ✅ (TTS Preview) | ✅ (Flash) | ❌ |
| Groq | ❌ | ✅ (Whisper) | ✅ |
| xAI/Grok | ✅ | ❌ | ❌ |

#### 📚 Learn More

- 💻 **Examples**: Check `examples/advanced/` for TTS and STT working examples

----

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

**Per-Call Multi-Tenant Identity (Singleton-Safe):**

Memory instances are **stateless** and safe to use as singletons. Pass `userId` and `conversationId` directly to `run()` / `stream()` via `options`, and they flow through to every memory read/write automatically:

```java
// One shared memory instance for all users
sharedMemory = aiMemory( "cache" )
agent = aiAgent( name: "Support", memory: sharedMemory )

// Each call is routed to its own isolated conversation
agent.run( "Hello!",             {}, { userId: "alice", conversationId: "session-1" } )
agent.run( "What did I say?",    {}, { userId: "alice", conversationId: "session-1" } )  // Remembers
agent.run( "Any prior context?", {}, { userId: "bob",   conversationId: "session-2" } )  // Isolated
```

You can also override identity per-call directly on memory methods:

```java
memory.getAll( userId: "alice", conversationId: "session-1" )
memory.add( message, userId: "alice", conversationId: "session-1" )
memory.clear( userId: "alice", conversationId: "session-1" )
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

**Seed an Agent with MCP Tools:**

Pass one or more MCP server URLs to `aiAgent()` via `mcpServers` and every tool the server exposes is automatically discovered and registered — no manual Tool construction required.

```javascript
// Seed at construction time (simplest)
agent = aiAgent(
    name       : "Data Assistant",
    description: "Assistant with filesystem and database access",
    mcpServers : [
        "http://localhost:3001",                                      // URL string
        { url: "http://localhost:3002", token: "my-api-key" }         // with auth
    ]
)

// Fluent seeding after construction
agent = aiAgent( name: "Data Assistant" )
    .withMCPServer( "http://localhost:3001" )
    .withMCPServer( "http://localhost:3002", { token: "my-api-key", timeout: 5000 } )

// Pass a pre-configured MCPClient for full control
filesystemMcp = MCP( "http://localhost:3001" ).withTimeout( 5000 ).withBearerToken( "token" )
agent = aiAgent( name: "Data Assistant" ).withMCPServer( filesystemMcp )

// Seed a model directly (without an agent)
model = aiModel( mcpServers: [ "http://localhost:3001" ] )

// The agent/model now has all MCP tools and can use them automatically
response = agent.run( "Read config.json and update the database with its contents" )

// The agent knows what it has — ask it directly
response = agent.run( "What tools do you have and which MCP servers are you connected to?" )
```

**Inspect tools and servers programmatically:**

```javascript
// List all tools (name + description)
tools = agent.listTools()
// => [{ name: "read_file", description: "Read a file..." }, ...]

// Full config including tools and connected servers
config = agent.getConfig()
config.tools      // [{ name, description }]
config.mcpServers // [{ url: "http://localhost:3001", toolNames: ["read_file", "write_file"] }]
config.toolCount  // 2
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
myServer = mcpServer(
    name: "my-tools",
    description: "Custom BoxLang tools"
)

// Register a tool by ITool instance
myServer.registerTool(
    aiTool(
        name: "calculate_tax",
        description: "Calculate tax for a given amount",
        callable: ( amount, rate = 0.08 ) => {
            return amount * rate;
        }
    )
)

// Or register by registry key (tool must be in the global AIToolRegistry)
myServer.registerTool( "now@bxai" )           // built-in current date/time tool
myServer.registerTool( "searchProducts" )      // any registered tool by name
```

#### 📚 Learn More

- 📖 **Full Guide**: [MCP Server Documentation](https://ai.ortusbooks.com/advanced/mcp-server.md)
- 🌍 **MCP Protocol**: [Model Context Protocol Specification](https://spec.modelcontextprotocol.io)
- 🔧 **Advanced Features**: [Custom Transports & Authentication](https://ai.ortusbooks.com/advanced/mcp-server-advanced.md)
- 💻 **Examples**: Check `examples/mcp/server/` for complete examples

----

## 🛠️ Global Functions (BIFs)

| Function | Purpose | Parameters | Return Type | Async Support |
|----------|---------|------------|-------------|---------------|
| `aiAgent()` | Create autonomous AI agent | `name`, `description`, `instructions`, `model`, `memory`, `tools`, `subAgents`, `params`, `options`, `mcpServers=[]`, `skills=[]`, `availableSkills=[]` | AiAgent Object (supports `runAsync()`) | ✅ |
| `aiChat()` | Chat with AI provider | `messages`, `params={}`, `options={}` | String/Array/Struct | ❌ |
| `aiChatAsync()` | Async chat with AI provider | `messages`, `params={}`, `options={}` | BoxLang Future | ✅ |
| `aiChatRequest()` | Compose a reusable chat request object (useful for advanced pipelines and middleware) | `messages`, `params`, `options`, `headers` | AiChatRequest Object | N/A |
| `aiChatStream()` | Stream chat responses from AI provider | `messages`, `callback`, `params={}`, `options={}` | void | N/A |
| `aiChunk()` | Split text into chunks for RAG ingestion or token-window management | `text`, `options={}` _(chunkSize, overlap, strategy)_ | Array of Strings | N/A |
| `aiDocuments()` | Create fluent document loader | `source`, `config={}` | IDocumentLoader Object | N/A |
| `aiEmbed()` | Generate embeddings | `input`, `params={}`, `options={}` | Array/Struct | N/A |
| `aiMemory()` | Create memory instance | `memory`, `key`, `userId`, `conversationId`, `config={}` | IAiMemory Object | N/A |
| `aiMessage()` | Build message object | `message` | ChatMessage Object | N/A |
| `aiModel()` | Create AI model wrapper | `provider`, `apiKey`, `tools`, `mcpServers=[]`, `skills=[]` | AiModel Object | N/A |
| `aiPopulate()` | Populate class/struct from JSON | `target`, `data` | Populated Object | N/A |
| `aiService()` | Create AI service provider | `provider`, `apiKey` | IService Object | N/A |
| `aiSkill()` | Create or discover AI skills | `path`, `name`, `description`, `content`, `recurse=true` | AiSkill / Array | N/A |
| `aiGlobalSkills()` | Get the globally shared skill pool | _(none)_ | Array of AiSkill | N/A |
| `aiSpeak()` | Convert text to speech (TTS) | `text`, `params={}`, `options={}` | AiSpeechResponse / File path | N/A |
| `aiTokens()` | Estimate token count for a text string | `text`, `options={}` _(method: characters\|words)_ | Numeric | N/A |
| `aiTool()` | Create tool for real-time processing | `name`, `description`, `callable` | Tool Object | N/A |
| `aiToolRegistry()` | Get the singleton AI Tool Registry | _(none)_ | AIToolRegistry Object | N/A |
| `aiTranscribe()` | Transcribe audio to text (STT) | `audio`, `params={}`, `options={}` | String / AiTranscriptionResponse | N/A |
| `aiTranslate()` | Translate non-English audio to English | `audio`, `params={}`, `options={}` | String / AiTranscriptionResponse | N/A |
| `aiParallel()` | Run multiple named runnables concurrently and collect results | `runnables` (struct of `{ name: IAiRunnable }`) | AiRunnableParallel Object | ✅ (via `runAsync()`) |
| `aiTransform()` | Create data transformer | `transformer`, `config={}` | Transformer Runnable | N/A |
| `MCP()` | Create MCP client for Model Context Protocol servers | `baseURL` | MCPClient Object | N/A |
| `mcpServer()` | Get or create MCP server for exposing tools | `name="default"`, `description`, `version`, `cors`, `statsEnabled`, `force` | MCPServer Object | N/A |

> **Note on Return Formats:** When using pipelines (runnable chains), the default return format is `raw` (full API response), giving you access to all metadata. Use `.singleMessage()`, `.allMessages()`, or `.withFormat()` to extract specific data. The `aiChat()` BIF defaults to `single` format (content string) for convenience. See the [Pipeline Return Formats](https://ai.ortusbooks.com/main-components/overview.md#return-formats) documentation for details.

## 🌐 GitHub Repository and Reporting Issues

Visit the [GitHub repository](https://github.com/ortus-boxlang/bx-ai) for release notes. You can also file a bug report or improvement suggestion  via [GitHub Issues](https://github.com/ortus-boxlang/bx-ai/issues).

## 🧪 Testing

This module includes tests for all AI providers. To run the tests:

```bash
./gradlew test
```

## 💖 Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://ai.ortussolutions.com). Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more. If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

> "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
