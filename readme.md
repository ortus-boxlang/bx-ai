# ⚡︎ BoxLang AI Module

```
|:------------------------------------------------------:|
| ⚡︎ B o x L a n g ⚡︎
| Dynamic : Modular : Productive
|:------------------------------------------------------:|
```

<blockquote>
Copyright Since 2023 by Ortus Solutions, Corp<br>
<a href="https://www.boxlang.io">www.boxlang.io</a> |
<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</blockquote>

----

![BoxLang AI Module](BoxLangAI.png)

**BoxLang AI** brings the power of artificial intelligence to your [BoxLang](https://www.boxlang.io) applications with a fluent, productive API. Write once, use any provider—switch between OpenAI, Claude, Gemini, Ollama, and more with zero code changes.

> **Built for Productivity.** BoxLang AI was designed from the ground up to make AI integration effortless. Stop wrestling with provider-specific APIs and start building intelligent applications in minutes.

## 🚀 Quick Start

```bash
install-bx-module bx-ai
```

```java
// Your first AI chat - it's that simple!
answer = aiChat( "What is BoxLang?" )
println( answer )
```

Configure your provider in `boxlang.json`:

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

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| **Multi-Provider** | OpenAI, Claude, Gemini, Grok, Groq, DeepSeek, Ollama, OpenRouter, Perplexity, HuggingFace |
| **AI Agents** | Autonomous agents with memory, tools, and sub-agent orchestration |
| **Structured Output** | Type-safe responses using classes, structs, or JSON schemas |
| **Memory Systems** | Windowed, summary, session, file, cache, JDBC, and vector memory |
| **Real-time Tools** | Let AI call your functions for live data access |
| **Streaming** | Real-time response streaming for interactive UIs |
| **Embeddings** | Generate vectors for semantic search and similarity |
| **Pipelines** | Chain AI operations into reusable workflows |
| **MCP Client** | Connect to Model Context Protocol servers |
| **Async Support** | Non-blocking operations with BoxLang futures |

## 🛠️ Built-in Functions (BIFs)

```java
// Chat & Messaging
aiChat( message )              // Simple chat
aiChatAsync( message )         // Async chat with futures  
aiChatStream( message, cb )    // Streaming responses
aiMessage()                    // Fluent message builder

// Agents & Memory
aiAgent( config )              // Create autonomous agents
aiMemory( type )               // Conversation memory systems

// Tools & Utilities
aiTool( name, desc, fn )       // Create callable tools
aiEmbed( text )                // Generate embeddings
aiService( provider )          // Direct provider access
aiModel( provider )            // Model wrapper for pipelines
MCP( url )                     // Model Context Protocol client
```

## 💡 Examples

**Fluent Messages:**
```java
response = aiChat(
    aiMessage()
        .system( "You are a helpful assistant" )
        .user( "Explain recursion" )
)
```

**AI Agent with Memory:**
```java
agent = aiAgent(
    name: "Support Bot",
    instructions: "Help users with their questions",
    memory: aiMemory( "windowed", { maxMessages: 20 } )
)
agent.run( "How do I install BoxLang?" )
```

**Structured Output:**
```java
class Person { 
    property name; 
    property age; 
}
person = aiMessage()
    .user( "Extract: John Doe, 30 years old" )
    .toDefaultModel()
    .structuredOutput( new Person() )
    .run()
```

**Real-time Tools:**
```java
weatherTool = aiTool( 
    "get_weather", 
    "Get current weather for a location",
    location => getWeatherAPI( location ) 
).describeLocation( "City name" )

response = aiChat( "What's the weather in Miami?", { tools: [ weatherTool ] } )
```

## 📚 Learn More

| Resource | Description |
|----------|-------------|
| 📖 **[Documentation](docs/)** | Complete API reference and guides |
| 🚀 **[Bootcamp](bootcamp/)** | Get started in one day (6-7 hours) |
| 🎓 **[Full Course](course/)** | 12 lessons from beginner to production |
| 💻 **[Examples](examples/)** | Working code samples |

## 🤖 Supported Providers

| Provider | Tools | Embeddings | Structured Output |
|----------|:-----:|:----------:|:-----------------:|
| OpenAI | ✅ | ✅ | ✅ |
| Claude | ✅ | ❌ | ✅ |
| Gemini | 🔜 | ✅ | ✅ |
| Grok | ✅ | ✅ | ✅ |
| Groq | ✅ | ✅ | ✅ |
| DeepSeek | ✅ | ✅ | ✅ |
| Ollama | ✅ | ✅ | ✅ |
| OpenRouter | ✅ | ✅ | ✅ |
| Perplexity | ✅ | ❌ | ✅ |
| HuggingFace | ✅ | ✅ | ✅ |

## 📄 License

Apache 2.0 - Open source and free to use. [Enterprise support available](https://www.boxlang.io/plans).

## 🤝 Collaborate & Support

We'd love your help making BoxLang AI even better!

- ⭐ **Star this repo** - Show your support
- 🐛 **[Report Issues](https://github.com/ortus-boxlang/bx-ai/issues)** - Help us improve
- 💡 **[Contribute](CONTRIBUTING.md)** - Submit PRs and ideas
- 💬 **[Community](https://community.boxlang.io)** - Join the discussion
- ❤️ **[Sponsor](https://patreon.com/ortussolutions)** - Support development

---

<p align="center">
<strong>Built with ❤️ by <a href="https://www.ortussolutions.com">Ortus Solutions</a></strong><br>
<em>"I am the way, and the truth, and the life" - John 14:6</em>
</p>
