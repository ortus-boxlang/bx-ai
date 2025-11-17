# ⚡︎ BoxLang AI Module Documentation

Welcome to the **BoxLang AI Module** - your gateway to integrating AI capabilities into BoxLang applications. This module provides a unified, easy-to-use API for interacting with multiple AI providers, from simple chat requests to complex multi-step pipelines.

## What is BoxLang AI?

BoxLang AI is a comprehensive module that brings artificial intelligence capabilities to the BoxLang ecosystem. Whether you're building chatbots, content generators, code assistants, or complex AI workflows, this module provides everything you need.

### Key Features

- **Multi-Provider Support**: Work with OpenAI, Claude, Gemini, Grok, DeepSeek, Ollama, and more
- **Unified API**: One consistent interface across all providers
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
| **DeepSeek** | Cloud | Code generation, reasoning |
| **Ollama** | Local | Privacy, offline use, no API costs |
| **OpenRouter** | Gateway | Access multiple models through one API |
| **Perplexity** | Cloud | Research, citations, factual answers |

### Use Cases

- **Chatbots**: Build conversational interfaces
- **Content Generation**: Create articles, documentation, marketing copy
- **Code Assistance**: Generate, review, and explain code
- **Data Analysis**: Extract insights from text and data
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

---

### Simple AI Interactions

💬 **[Basic Chatting](simple-interactions/basic-chatting.md)**
Simple question-answer interactions, parameters, and provider switching.

🎯 **[Advanced Chatting](simple-interactions/advanced-chatting.md)**
Multi-message conversations, AI tools, async requests, and streaming responses.

⚙️ **[Service-Level Chatting](simple-interactions/service-chatting.md)**
Direct service control, custom requests, headers, and managing multiple providers.

---

### AI Pipelines

🔗 **[Understanding Pipelines](pipelines/overview.md)**
Core concepts of AI pipelines, composability, and building workflows.

🤖 **[Working with Models](pipelines/models.md)**
Creating model runnables, configuration, and integrating AI providers into pipelines.

✉️ **[Message Templates](pipelines/messages.md)**
Building reusable prompts with dynamic placeholders and binding strategies.

🔧 **[Transformers](pipelines/transformers.md)**
Processing and transforming data between pipeline steps.

📡 **[Pipeline Streaming](pipelines/streaming.md)**
Real-time streaming through pipelines for responsive applications.

---

### Advanced Topics

🎪 **[Event System](advanced/events.md)**
Intercept and customize AI operations with comprehensive event hooks for monitoring, security, and extensibility.

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