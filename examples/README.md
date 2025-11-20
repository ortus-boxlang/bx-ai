# BoxLang AI Examples

Comprehensive examples demonstrating all features of the BoxLang AI module, organized by complexity and use case.

## 📁 Directory Structure

### `/basic` - Getting Started

Simple, focused examples perfect for learning the fundamentals:

- **simple-chat.bxs** - Basic AI chat interactions
- **ollama-local.bxs** - Using local AI with Ollama
- **streaming-responses.bxs** - Real-time streaming output
- **return-formats.bxs** - Different response formats (single, all, raw)

### `/advanced` - Advanced Features

More complex examples showcasing advanced capabilities:

- **embeddings.bxs** - Vector embeddings for semantic search
- **token-counting.bxs** - Estimating and managing token usage
- **json-xml-formats.bxs** - Structured output formats
- **memory-systems.bxs** - Different memory implementations

### `/agents` - AI Agents

Autonomous agents with memory and tools:

- **basic-agent.bxs** - Simple agent with memory
- **agent-with-tools.bxs** - Agent that can execute functions
- **customer-support-agent.bxs** - Support bot with multiple tools
- **research-agent.bxs** - Agent that searches and synthesizes information

### `/pipelines` - Pipeline Workflows

Composable multi-step AI workflows:

- **simple-pipeline.bxs** - Basic pipeline with transforms
- **message-template-pipeline.bxs** - Reusable prompt templates
- **multi-model-pipeline.bxs** - Chaining different AI models
- **streaming-pipeline.bxs** - Real-time pipeline execution

## 🚀 Quick Start

### Run Any Example

```bash
# Basic examples
boxlang examples/basic/simple-chat.bxs

# With Ollama (no API key needed)
boxlang examples/basic/ollama-local.bxs

# Advanced features
boxlang examples/advanced/embeddings.bxs

# Agents
boxlang examples/agents/basic-agent.bxs

# Pipelines
boxlang examples/pipelines/simple-pipeline.bxs
```

### Environment Setup

Most examples require API keys. Set them in your environment:

```bash
export OPENAI_API_KEY="sk-..."
export CLAUDE_API_KEY="sk-ant-..."
export GEMINI_API_KEY="..."
```

Or configure in `boxlang.json`:

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

- `basic/simple-chat.bxs` - Start here!
- `basic/ollama-local.bxs` - Local AI basics
- `basic/return-formats.bxs` - Understanding responses

**🟡 Intermediate**

- `advanced/memory-systems.bxs` - Managing conversation history
- `agents/basic-agent.bxs` - Your first agent
- `pipelines/simple-pipeline.bxs` - Pipeline fundamentals

**🔴 Advanced**

- `agents/customer-support-agent.bxs` - Production-ready agent
- `pipelines/multi-model-pipeline.bxs` - Complex workflows
- `advanced/embeddings.bxs` - Vector search

### By Use Case

**💬 Chat & Conversation**

- `basic/simple-chat.bxs` - Basic Q&A
- `basic/streaming-responses.bxs` - Real-time chat
- `agents/basic-agent.bxs` - Conversational agent

**🛠️ Function Calling**

- `agents/agent-with-tools.bxs` - Basic tool usage
- `agents/customer-support-agent.bxs` - Multiple tools
- `agents/research-agent.bxs` - Web search tools

**🔄 Workflows**

- `pipelines/simple-pipeline.bxs` - Basic workflow
- `pipelines/message-template-pipeline.bxs` - Reusable templates
- `pipelines/multi-model-pipeline.bxs` - Model chaining

**📊 Data Processing**

- `advanced/embeddings.bxs` - Semantic search
- `advanced/token-counting.bxs` - Cost estimation
- `advanced/json-xml-formats.bxs` - Structured output

**🧠 Memory & State**

- `advanced/memory-systems.bxs` - All memory types
- `agents/basic-agent.bxs` - Agent with memory
- File, cache, session memory examples

### By Provider

**☁️ Cloud Providers**

- OpenAI - Most examples use GPT models
- Claude - See provider-specific examples
- Gemini - Google AI examples

**💻 Local AI**

- `basic/ollama-local.bxs` - Ollama basics
- Privacy-focused examples
- Offline capabilities

## 🎯 Learning Path

### Path 1: Absolute Beginner
1. `basic/simple-chat.bxs` - Learn the basics
2. `basic/return-formats.bxs` - Understand responses
3. `basic/streaming-responses.bxs` - Real-time output
4. `agents/basic-agent.bxs` - Add memory
5. `agents/agent-with-tools.bxs` - Function calling

### Path 2: Quick to Advanced
1. `basic/simple-chat.bxs` - Start here
2. `agents/basic-agent.bxs` - Jump to agents
3. `agents/customer-support-agent.bxs` - Production patterns
4. `pipelines/simple-pipeline.bxs` - Learn workflows
5. `pipelines/multi-model-pipeline.bxs` - Complex systems

### Path 3: Local/Privacy Focused
1. `basic/ollama-local.bxs` - Local AI setup
2. `agents/basic-agent.bxs` - Change to Ollama provider
3. `advanced/memory-systems.bxs` - File-based memory
4. Build privacy-first applications

## 📖 Documentation Links

### Getting Started
- [Installation Guide](../docs/getting-started/installation.md)
- [Quick Start](../docs/getting-started/quickstart.md)
- [Agent Quick Start](../docs/getting-started/agent-quickstart.md)

### Core Concepts
- [Basic Chatting](../docs/simple-interactions/basic-chatting.md)
- [Advanced Chatting](../docs/simple-interactions/advanced-chatting.md)
- [Pipeline Overview](../docs/pipelines/overview.md)

### Deep Dives
- [AI Agents](../docs/pipelines/agents.md)
- [Working with Models](../docs/pipelines/models.md)
- [Message Templates](../docs/pipelines/messages.md)

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
