---
description: >-
  Learn how to get started with the BoxLang AI module, including installation,
  basic usage, and key features.
icon: crosshairs-simple
---

# Overview

Welcome to BoxLang AI! This section covers everything you need to get up and running with AI-powered features in your BoxLang applications.

## 📚 In This Section

### 📦 [Installation](installation/)

Quick guide to installing the BoxLang AI module.

**What you'll learn:**

* Installing via BoxLang Module Installer, CommandBox, or package dependencies
* Basic configuration setup
* Running Ollama with Docker for production deployments
* Verification and next steps

**Time:** 5 minutes

***

### 🧩 [Provider Setup & Configuration](installation/provider-setup.md)

Comprehensive guide to configuring all supported AI providers.

**What you'll learn:**

* Provider comparison and recommendations
* Getting API keys for 12+ cloud providers (OpenAI, Claude, Gemini, etc.)
* Setting up Ollama for local AI (no API costs!)
* Configuration best practices
* Environment variables and security
* Multiple provider management
* Troubleshooting provider issues

**Time:** 10-15 minutes

* **🤖 Multi-Provider Support** - OpenAI, Claude, Gemini, Ollama, Grok, Groq, DeepSeek, Perplexity, and more
* **💬 Simple Chat Interface** - Start with one-line AI conversations
* **🔄 Composable Pipelines** - Build complex AI workflows by chaining operations
* **🧠 Intelligent Agents** - Create autonomous agents with memory and tools
* **📊 Structured Output** - Extract data into classes, structs, or arrays
* **🎙️ Multimodal Content** - Process images, audio, video, and documents
* **🛠️ Real-Time Tools** - Enable AI to call functions and APIs
* **💭 Memory Systems** - Maintain conversation context across interactions
* **📡 Streaming Support** - Real-time response streaming for better UX

## 🧭 Quick Navigation

### 🆕 New to BoxLang AI?

[**Installation Guide**](installation/) Get the module installed in minutes.

[**Provider Setup**](installation/provider-setup.md) Configure your AI providers.

[**Quickstart Tutorial**](quickstart.md) Your first AI conversation in 5 lines of code.

***

### ⚡ [Quick Start Guide](quickstart.md)

Your first AI conversation in 5 lines of code, plus essential patterns and examples.

**What you'll learn:**

* Making your first AI chat request
* Understanding basic BIF usage (`aiChat`, `aiMessage`, `aiModel`)
* Provider switching and model selection
* Streaming responses in real-time
* Working with structured output
* Building your first AI agent with tools and memory
* Common patterns and best practices

**Time:** 15-20 minutes

***

### 📖 [Key Concepts](concepts.md)

Essential AI terminology and concepts explained in plain language.

**What you'll learn:**

* AI & Machine Learning fundamentals (LLMs, training vs inference)
* Language model parameters (temperature, top-p, tokens, context windows)
* Messages & conversations (roles, system messages, multi-turn)
* Embeddings & vectors (dimensions, cosine similarity, vector databases)
* Memory systems (conversation, vector, hybrid, multi-tenant)
* RAG (Retrieval Augmented Generation) workflow
* Tools & function calling
* Streaming & async operations
* Pipelines & composition
* Providers & services comparison
* Tokens & costs

**Time:** 30 minutes reference

***

### ❓ [Frequently Asked Questions](../readme/faq.md)

Quick answers to the most common questions about BoxLang AI.

**Topics covered:**

* Getting started (why use BoxLang AI, free options, offline usage)
* Providers & models (comparison, recommendations, model selection)
* Costs & pricing (optimization, estimation, budget management)
* Performance & reliability (response variation, speed, handling downtime)
* Features & capabilities (structured output, tools, memory, RAG, multimodal)
* Memory & context (conversation vs vector, multi-tenant isolation)
* Security & privacy (data privacy, prompt injection, API keys)
* Troubleshooting (common errors, rate limits, context length)
* Best practices (aiChat vs aiAgent, memory sizing, caching, error handling)

**Time:** Quick reference

***

## 🎯 Learning Path

We recommend this progression:

1. **📦** [**Install**](installation/) - Get the module installed (5 min)
2. **🧩** [**Configure Providers**](installation/provider-setup.md) - Set up your AI providers (10 min)
3. **⚡** [**Quick Start**](quickstart.md) - Your first AI conversation (10 min)
4. **📖** [**Key Concepts**](concepts.md) - Learn AI terminology (reference as needed)
5. **💬** [**Basic Chatting**](../main-components/chatting/basic-chatting.md) - Simple interactions and parameters (15 min)
6. **🎯** [**Advanced Chatting**](../main-components/chatting/advanced-chatting.md) - Streaming, tools, multimodal content (20 min)
7. **🤖** [**AI Agents**](../main-components/agents.md) - Build autonomous agents (30 min)

**💡 Pro Tip:** Keep the [FAQ](../readme/faq.md) handy for quick answers to common questions!

***

## 💡 Quick Examples

### Simple Chat

```javascript
result = aiChat( "What is BoxLang?" );
println( result );
```

### Structured Output

```javascript
person = aiChat(
    message: "Extract info: John is 30 and lives in NYC",
    structured: {
        name: "string",
        age: "numeric",
        city: "string"
    }
);
println( person.name ); // "John"
```

### AI Agent

```javascript
agent = aiAgent()
    .withInstructions( "You are a helpful assistant" )
    .withMemory( "windowed" )
    .build();

response = agent.run( "Hello! Remember my name is Alice." );
```

***

## 🔗 Related Documentation

After mastering the basics, explore these advanced topics:

* **🔄** [**Pipelines**](../main-components/main-components/overview.md) - Build composable AI workflows
* **💭** [**Memory Systems**](../main-components/memory/) - Maintain conversation context
* **🛠️** [**Tools**](../main-components/tools.md) - Enable AI to call functions
* **📄** [**Document Loaders**](../rag/document-loaders.md) - Process various file formats
* **🔮** [**Vector Memory**](../main-components/vector-memory.md) - Semantic search with embeddings

***

## ❓ Need Help?

* **💬 Questions?** Check the [main documentation](../)
* **🐛 Found a bug?** [Report it on GitHub](https://github.com/ortus-boxlang/bx-ai/issues)
* **💡 Have an idea?** [Start a discussion](https://github.com/ortus-boxlang/bx-ai/discussions)
