---
description: "Learn how to use the productive BoxLang AI chatting features for building conversational AI applications with ease."
icon: messages
---

# Chatting

Simple, powerful AI interactions using Built-in Functions (BIFs). This section covers everything from basic chat to advanced features like streaming, tools, and structured output.

## Overview

The `aiChat()` BIF provides the fastest way to interact with AI providers. Whether you need a quick answer or a sophisticated multi-turn conversation, these patterns cover your use cases without the overhead of pipelines or agents.

**Perfect for:**

- Quick AI queries and one-off interactions
- Prototyping and experimentation
- Simple conversational interfaces
- Direct provider control

---

## Guides

### 💬 [Basic Chatting](basic-chatting.md)

Master the fundamentals of AI interactions.

**What you'll learn:**

- Making your first AI call with `aiChat()`
- Understanding message roles (system, user, assistant)
- Managing conversation history
- Choosing and configuring providers
- Working with different return formats

**Start here if:** You're new to BoxLang AI or need simple question-answer interactions.

---

### 🎯 [Advanced Chatting](advanced-chatting.md)

Unlock powerful features for sophisticated applications.

**What you'll learn:**

- **Streaming responses** - Real-time token-by-token output
- **Function calling (tools)** - Let AI call your functions and APIs
- **Structured output** - Extract data into classes, structs, or arrays
- **Multimodal content** - Process images, audio, video, and documents
- **JSON mode** - Force valid JSON responses
- **Temperature & creativity** - Control response randomness

**Start here if:** You need real-time updates, tool integration, or data extraction.

---

### ⚙️ [Service-Level Chatting](service-chatting.md)

Direct service instance management for reusability and control.

**What you'll learn:**

- Creating reusable service instances
- Configuring providers with custom settings
- Managing multiple configurations
- Invoking services directly vs using BIFs
- When to use services vs `aiChat()`

**Start here if:** You need fine-grained control, custom configurations, or want to reuse service instances.

---

## Quick Examples

### Simple Chat

```java
result = aiChat( "What is BoxLang?" );
```

### With Specific Provider

```java
result = aiChat(
    provider: "claude",
    message: "Explain quantum computing",
    model: "claude-3-5-sonnet-20241022"
);
```

### Streaming Response

```java
aiChat(
    message: "Write a poem about code",
    stream: true,
    onChunk: ( chunk ) => print( chunk )
);
```

### Function Calling

```java
weatherTool = aiTool()
    .setFunction( getWeather )
    .setDescription( "Get weather for a location" );

result = aiChat(
    message: "What's the weather in Boston?",
    tools: [ weatherTool ]
);
```

### Structured Output

```java
result = aiChat(
    message: "Extract: John is 30, works as developer in NYC",
    structured: {
        name: "string",
        age: "numeric",
        job: "string",
        location: "string"
    }
);
```

---

## Choosing Your Path

**"I just need simple AI responses"**
→ [Basic Chatting](basic-chatting.md)

**"I want real-time streaming or tool calling"**
→ [Advanced Chatting](advanced-chatting.md)

**"I need reusable service configurations"**
→ [Service-Level Chatting](service-chatting.md)

**"I want to build complex workflows"**
→ See [Main Components](../main-components/README.md) for pipelines and agents

---

## Key Concepts

### Message Roles

- **system** - Instructions that define AI behavior
- **user** - Your questions or prompts
- **assistant** - AI's responses

### Return Formats

- **`single`** (default) - Returns content string directly
- **`all`** - Returns full response struct with metadata
- **`raw`** - Returns complete API response

### Provider Selection

Use the `provider` parameter or set `OPENAI_API_KEY`, `CLAUDE_API_KEY`, etc. in your environment.

---

## Next Steps

1. **Start with basics** - [Basic Chatting](basic-chatting.md)
2. **Add advanced features** - [Advanced Chatting](advanced-chatting.md)
3. **Optimize with services** - [Service-Level Chatting](service-chatting.md)
4. **Scale with pipelines** - [Main Components](../main-components/README.md)
