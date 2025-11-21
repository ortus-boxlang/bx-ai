# BoxLang AI Examples

Comprehensive examples demonstrating all features of the BoxLang AI module, organized by complexity and use case.

## 📁 Example Files

Practical examples demonstrating key features:

### Basic Features

- **ollama-example.bxs** - Using local AI with Ollama (no API key needed)
- **streaming-example.bxs** - Real-time streaming output
- **return-formats-example.bxs** - Different response formats (single, all, raw)
- **runnable-example.bxs** - Pipeline workflows and runnables

### Advanced Features

- **vision-example.bxs** - Image analysis with vision-capable models (NEW!)
- **embeddings-example.bxs** - Vector embeddings for semantic search
- **token-counting-example.bxs** - Estimating and managing token usage
- **json-xml-formats-example.bxs** - Structured output formats
- **memory-file.bxs** - File-based conversation memory

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
# Local AI (no API key needed)
boxlang examples/ollama-example.bxs

# Streaming responses
boxlang examples/streaming-example.bxs

# Vision/image analysis (NEW!)
boxlang examples/vision-example.bxs

# Vector embeddings
boxlang examples/embeddings-example.bxs

# Pipeline workflows
boxlang examples/runnable-example.bxs
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

- `ollama-example.bxs` - Local AI basics
- `streaming-example.bxs` - Real-time responses
- `return-formats-example.bxs` - Understanding responses

**🟡 Intermediate**

- `vision-example.bxs` - Image analysis (NEW!)
- `memory-file.bxs` - Managing conversation history
- `runnable-example.bxs` - Pipeline fundamentals
- `json-xml-formats-example.bxs` - Structured output

**🔴 Advanced**

- `embeddings-example.bxs` - Vector search and semantic similarity
- `token-counting-example.bxs` - Cost optimization

### By Use Case

**💬 Chat & Conversation**

- `streaming-example.bxs` - Real-time chat responses
- `memory-file.bxs` - Conversation history

**👁️ Vision & Images (NEW!)**

- `vision-example.bxs` - Image analysis, document scanning, multi-image comparison

**🔄 Workflows**

- `runnable-example.bxs` - Pipeline basics and chaining
- `return-formats-example.bxs` - Response format options

**📊 Data Processing**

- `embeddings-example.bxs` - Semantic search and similarity
- `token-counting-example.bxs` - Cost estimation
- `json-xml-formats-example.bxs` - Structured output

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
4. `runnable-example.bxs` - Build pipelines
5. `memory-file.bxs` - Add conversation memory

### Path 2: Cloud AI & Advanced Features
1. `streaming-example.bxs` - Start here (needs API key)
2. `vision-example.bxs` - Image analysis
3. `json-xml-formats-example.bxs` - Structured output
4. `embeddings-example.bxs` - Semantic search
5. `token-counting-example.bxs` - Optimize costs

### Path 3: Local/Privacy Focused
1. `ollama-example.bxs` - Local AI setup
2. `memory-file.bxs` - File-based memory
3. `runnable-example.bxs` - Build offline workflows
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
