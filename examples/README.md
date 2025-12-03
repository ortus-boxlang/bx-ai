# BoxLang AI Examples

Comprehensive examples demonstrating all features of the BoxLang AI module, organized by complexity and use case.

## ⚙️ Requirements

### API Keys

Most examples require an API key from your chosen AI provider. You have two options:

**Option 1: Environment Variables (Recommended)**

Set the appropriate environment variable for your provider:

```bash
# OpenAI (GPT models)
export OPENAI_API_KEY="sk-..."

# Anthropic (Claude models)
export CLAUDE_API_KEY="sk-ant-..."

# Google (Gemini models)
export GEMINI_API_KEY="..."

# Cohere
export COHERE_API_KEY="..."

# Voyage AI (embeddings)
export VOYAGE_API_KEY="..."

# Groq
export GROQ_API_KEY="..."

# DeepSeek
export DEEPSEEK_API_KEY="..."
```

**Option 2: Pass API Key Directly**

You can pass the API key in the `options` parameter:

```java
response = aiChat(
    messages: [ aiMessage().user( "Hello!" ) ],
    options: {
        provider: "openai",
        apiKey: "sk-..."
    }
)
```

**No API Key Required:**

- **Ollama** examples work locally without any API key
- Perfect for learning, development, and privacy-focused applications

### Get API Keys

- **OpenAI**: https://platform.openai.com/api-keys
- **Anthropic (Claude)**: https://console.anthropic.com/
- **Google (Gemini)**: https://makersuite.google.com/app/apikey
- **Cohere**: https://dashboard.cohere.com/api-keys
- **Voyage AI**: https://www.voyageai.com/
- **Groq**: https://console.groq.com/
- **DeepSeek**: https://platform.deepseek.com/

## 📁 Example Organization

All examples are organized into folders by topic for easy navigation.

### `/basic` - Getting Started

Essential examples for learning the basics:

- **ollama-example.bxs** - Using local AI with Ollama (no API key needed)
- **streaming-example.bxs** - Real-time streaming output
- **return-formats-example.bxs** - Different response formats (single, all, raw)
- **json-xml-formats-example.bxs** - JSON and XML output formats

### `/structured` - Structured Output 🆕

Type-safe AI responses with classes and templates:

- **01-basic-class.bxs** - Extract data into typed classes
- **02-struct-template.bxs** - Using struct templates for quick extraction
- **03-array-extraction.bxs** - Extracting multiple items into arrays
- **04-multiple-schemas.bxs** - Extract different entity types simultaneously
- **05-aipopulate.bxs** - Manual population from JSON (testing/caching)
- **06-pipeline-structured-output.bxs** - Structured output in reusable pipelines

### `/pipelines` - Pipeline Workflows

Composable multi-step AI workflows:

- **runnable-example.bxs** - Basic pipeline workflows and runnables
- **01-simple-pipeline.bxs** - Message templates, transformations, and reusable patterns
- **02-multi-model-pipeline.bxs** - Using different AI models in workflows
- **03-streaming-pipeline.bxs** - Real-time pipeline execution with progress tracking
- **04-message-templates.bxs** - Reusable prompt templates and few-shot learning

### `/agents` - AI Agents

Autonomous agents with memory and tools:

- **01-basic-agent.bxs** - Simple agent with memory and context
- **02-agent-with-tools.bxs** - Agent with weather, calculator, and database tools
- **03-customer-support-agent.bxs** - Production support bot with hybrid memory (recent + past tickets)
- **04-research-agent.bxs** - Research agent with vector memory for semantic article search
- **05-multi-memory-agent.bxs** - One agent using multiple memory types simultaneously
- **06-sub-agents.bxs** - Agent orchestration with specialized sub-agents

### `/advanced` - Advanced Features

Advanced capabilities and memory systems:

- **cohere-embeddings-example.bxs** - Cohere embeddings with multilingual support (100+ languages)
- **cohere-tools-example.bxs** - Cohere function calling and tool use
- **voyage-embeddings-example.bxs** - Voyage AI state-of-the-art embeddings
- **embeddings-example.bxs** - Vector embeddings for semantic search (OpenAI)
- **multimodal-example.bxs** - Images, audio, video, and documents in AI conversations
- **vision-example.bxs** - Image analysis with vision-capable models
- **token-counting-example.bxs** - Estimating and managing token usage
- **memory-file.bxs** - File-based persistent conversation memory
- **memory-windowed.bxs** - Windowed memory (keeps last N messages)
- **memory-summary.bxs** - Summary memory (auto-summarizes old messages)
- **memory-session.bxs** - Session-based memory for web applications
- **memory-cache.bxs** - Distributed cache memory with CacheBox
- **memory-comparison.bxs** - Side-by-side comparison of memory types
- **test-multimodal.bxs** - Quick verification script for multimodal methods

### `/vector-memory` - Semantic Search Memory

Vector-based memory for semantic search and knowledge retrieval:

- **boxvector-memory.bxs** - In-memory vector storage (no external dependencies)
- **hybrid-memory.bxs** - Combines recent messages + semantic search (best of both)
- **memory-type-comparison.bxs** - Standard vs vector memory comparison
- **vector-memory-demo.bxs** - ChromaDB vector memory demonstration (requires ChromaDB)

## 🚀 Quick Start

### Run Any Example

```bash
# Local AI (no API key needed)
boxlang examples/basic/ollama-example.bxs

# Streaming responses
boxlang examples/basic/streaming-example.bxs

# Structured output - Extract data into typed classes
boxlang examples/structured/01-basic-class.bxs

# Structured output - Extract multiple items
boxlang examples/structured/03-array-extraction.bxs

# Vision/image analysis
boxlang examples/advanced/vision-example.bxs

# Vector embeddings (OpenAI)
boxlang examples/advanced/embeddings-example.bxs

# Cohere embeddings (multilingual)
boxlang examples/advanced/cohere-embeddings-example.bxs

# Voyage AI embeddings (state-of-the-art)
boxlang examples/advanced/voyage-embeddings-example.bxs

# AI agents with tools
boxlang examples/agents/02-agent-with-tools.bxs

# Agent with sub-agents
boxlang examples/agents/06-sub-agents.bxs

# Pipeline workflows
boxlang examples/pipelines/runnable-example.bxs
```

### Ollama (Local AI)

Several examples use Ollama for local AI - no API key required!

**Setup Ollama:**

```bash
# Install from https://ollama.ai
# Pull a model
ollama pull llama3.2

# Start service (usually auto-starts)
ollama serve
```

## 📚 Example Categories

### By Complexity

**🟢 Beginner**

- `basic/ollama-example.bxs` - Local AI basics
- `basic/streaming-example.bxs` - Real-time responses
- `basic/return-formats-example.bxs` - Understanding responses
- `structured/01-basic-class.bxs` - Type-safe data extraction

**🟡 Intermediate**

- `structured/02-struct-template.bxs` - Quick structured extraction
- `structured/03-array-extraction.bxs` - Extract multiple items
- `structured/04-multiple-schemas.bxs` - Multiple entity types
- `pipelines/01-simple-pipeline.bxs` - Basic pipeline patterns
- `pipelines/04-message-templates.bxs` - Reusable templates
- `agents/01-basic-agent.bxs` - Agent with memory
- `agents/02-agent-with-tools.bxs` - Agent with function calling
- `advanced/vision-example.bxs` - Image analysis
- `advanced/cohere-embeddings-example.bxs` - Multilingual embeddings
- `advanced/voyage-embeddings-example.bxs` - Advanced embeddings
- `advanced/memory-file.bxs` - Managing conversation history
- `pipelines/runnable-example.bxs` - Pipeline fundamentals
- `basic/json-xml-formats-example.bxs` - JSON/XML output

**🔴 Advanced**

- `structured/05-aipopulate.bxs` - Manual population & caching
- `structured/06-pipeline-structured-output.bxs` - Pipeline integration
- `pipelines/02-multi-model-pipeline.bxs` - Multi-model workflows
- `pipelines/03-streaming-pipeline.bxs` - Streaming pipelines
- `agents/03-customer-support-agent.bxs` - Production support bot
- `agents/04-research-agent.bxs` - Multi-source research agent
- `agents/06-sub-agents.bxs` - Agent orchestration patterns
- `advanced/embeddings-example.bxs` - Vector search and semantic similarity
- `advanced/cohere-tools-example.bxs` - Function calling with Cohere
- `advanced/token-counting-example.bxs` - Cost optimization
- `advanced/multimodal-example.bxs` - Multi-format AI interactions

### By Use Case

**💬 Chat & Conversation**

- `basic/streaming-example.bxs` - Real-time chat responses
- `advanced/memory-windowed.bxs` - Recent message memory
- `advanced/memory-summary.bxs` - Auto-summarizing memory
- `advanced/memory-file.bxs` - Persistent conversation storage
- `advanced/memory-comparison.bxs` - Compare all memory types

**🧠 Memory & Context Management (NEW!)**

- `advanced/memory-windowed.bxs` - Keeps last N messages (cost-effective)
- `advanced/memory-summary.bxs` - Auto-summarizes old messages (context preservation)
- `advanced/memory-session.bxs` - Web session-based memory
- `advanced/memory-cache.bxs` - Distributed cache with CacheBox
- `advanced/memory-comparison.bxs` - Side-by-side memory comparison
- `vector-memory/boxvector-memory.bxs` - In-memory semantic search
- `vector-memory/hybrid-memory.bxs` - Recent + semantic combination
- `vector-memory/memory-type-comparison.bxs` - Standard vs vector memory

**🤖 AI Agents**

- `agents/01-basic-agent.bxs` - Simple conversational agent
- `agents/02-agent-with-tools.bxs` - Function calling and tool use
- `agents/03-customer-support-agent.bxs` - Hybrid memory (recent + tickets)
- `agents/04-research-agent.bxs` - Vector memory for knowledge retrieval
- `agents/05-multi-memory-agent.bxs` - Multiple memory types in one agent
- `agents/06-sub-agents.bxs` - Agent orchestration with specialized sub-agents

**🎯 Structured Output (NEW!)**

- `structured/01-basic-class.bxs` - Extract into typed classes
- `structured/02-struct-template.bxs` - Quick struct templates
- `structured/03-array-extraction.bxs` - Extract multiple items
- `structured/04-multiple-schemas.bxs` - Multiple entity types
- `structured/05-aipopulate.bxs` - Manual population & caching
- `structured/06-pipeline-structured-output.bxs` - Pipeline integration

**👁️ Vision & Images**

- `advanced/vision-example.bxs` - Image analysis, document scanning, multi-image comparison

**🔄 Workflows & Pipelines**

- `pipelines/runnable-example.bxs` - Pipeline basics and chaining
- `pipelines/01-simple-pipeline.bxs` - Message templates and transformations
- `pipelines/02-multi-model-pipeline.bxs` - Multi-model workflows
- `pipelines/03-streaming-pipeline.bxs` - Real-time streaming
- `pipelines/04-message-templates.bxs` - Reusable prompt patterns
- `basic/return-formats-example.bxs` - Response format options

**🤖 AI Agents**

- `agents/01-basic-agent.bxs` - Memory and context
- `agents/02-agent-with-tools.bxs` - Function calling
- `agents/03-customer-support-agent.bxs` - Production support bot
- `agents/04-research-agent.bxs` - Research and synthesis
- `agents/05-multi-memory-agent.bxs` - Multiple memory types
- `agents/06-sub-agents.bxs` - Agent orchestration

**📊 Data Processing & Embeddings**

- `advanced/embeddings-example.bxs` - OpenAI embeddings for semantic search
- `advanced/cohere-embeddings-example.bxs` - Cohere multilingual embeddings (100+ languages)
- `advanced/voyage-embeddings-example.bxs` - Voyage AI state-of-the-art embeddings
- `advanced/cohere-tools-example.bxs` - Cohere function calling
- `advanced/token-counting-example.bxs` - Cost estimation
- `basic/json-xml-formats-example.bxs` - JSON/XML formats

### By Provider

**☁️ Cloud Providers**

- OpenAI - GPT models (including vision with gpt-4o)
- Claude - Anthropic models (claude-3 with vision)
- Gemini - Google AI models (gemini-pro-vision)

**💻 Local AI**

- `ollama-example.bxs` - Ollama basics
- Privacy-focused, offline-capable AI

## 🎯 Learning Path

### Path 1: Absolute Beginner

1. `ollama-example.bxs` - Start with local AI (no API key)
2. `return-formats-example.bxs` - Understand responses
3. `streaming-example.bxs` - Real-time output
4. `memory-windowed.bxs` - Basic conversation memory
5. `agents/01-basic-agent.bxs` - Simple agent

### Path 2: Memory & Context Management (NEW!)

1. `memory-windowed.bxs` - Start with simple windowed memory
2. `memory-summary.bxs` - Learn auto-summarization
3. `memory-comparison.bxs` - Compare standard memory types
4. `vector-memory/boxvector-memory.bxs` - Semantic search basics
5. `vector-memory/hybrid-memory.bxs` - Best of both worlds
6. `agents/05-multi-memory-agent.bxs` - Multiple memories together

### Path 3: AI Agents

1. `agents/01-basic-agent.bxs` - Basic agent with memory
2. `agents/02-agent-with-tools.bxs` - Add tool calling
3. `agents/03-customer-support-agent.bxs` - Hybrid memory in production
4. `agents/04-research-agent.bxs` - Vector memory for knowledge
5. `agents/05-multi-memory-agent.bxs` - Advanced memory strategies
6. `agents/06-sub-agents.bxs` - Agent orchestration and delegation

### Path 4: Cloud AI & Advanced Features

1. `streaming-example.bxs` - Start here (needs API key)
2. `vision-example.bxs` - Image analysis
3. `json-xml-formats-example.bxs` - Structured output
4. `embeddings-example.bxs` - Semantic search
5. `token-counting-example.bxs` - Optimize costs

### Path 5: Local/Privacy Focused

1. `ollama-example.bxs` - Local AI setup
2. `memory-file.bxs` - File-based memory
3. `vector-memory/boxvector-memory.bxs` - In-memory vectors (no external DB)
4. `runnable-example.bxs` - Build offline workflows
5. Build privacy-first applications

## 📖 Documentation Links

### Getting Started

- [Installation Guide](../docs/getting-started/installation.md)
- [Quick Start](../docs/getting-started/quickstart.md)
- [Agent Quick Start](../docs/getting-started/agent-quickstart.md)

### Core Concepts

- [Basic Chatting](../docs/chatting/basic-chatting.md)
- [Advanced Chatting](../docs/chatting/advanced-chatting.md)
- [Pipeline Overview](../docs/main-components/overview.md)

### Deep Dives

- [AI Agents](../docs/main-components/agents.md)
- [Working with Models](../docs/main-components/models.md)
- [Message Templates](../docs/main-components/messages.md)
- [Memory Systems](../docs/main-components/memory.md) 🆕
- [Vector Memory](../docs/main-components/vector-memory.md) 🆕
- [Custom Memory](../docs/advanced/custom-memory.md) 🆕

## 🤝 Contributing Examples

Have a great example? Please contribute!

### Example Template

```boxlang
// example-name.bxs
/**
 * [Example Title]
 *
 * Brief description of what this example demonstrates.
 *
 * Prerequisites:
 * - List any requirements
 * - API keys needed
 * - External services
 *
 * Learn more: [Link to relevant docs]
 */

// Clear, commented code here
// Explain key concepts as you go

println( "✅ Example complete!" )
```

### Guidelines

- Keep examples focused on one concept
- Include clear comments
- Show expected output
- Mention prerequisites
- Link to relevant documentation

## ❓ Getting Help

- **Issues**: [GitHub Issues](https://github.com/ortus-boxlang/bx-ai/issues)
- **Docs**: [Full Documentation](../docs/README.md)
- **Community**: [BoxLang Community](https://boxlang.io/community)

## 📝 License

All examples are released under the same license as the bx-ai module (Apache 2.0).
