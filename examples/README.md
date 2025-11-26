# BoxLang AI Examples

Comprehensive examples demonstrating all features of the BoxLang AI module, organized by complexity and use case.

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
- **03-customer-support-agent.bxs** - Production-ready support bot with multiple tools
- **04-research-agent.bxs** - Multi-source research agent with citations

### `/advanced` - Advanced Features

Advanced capabilities and integrations:

- **multimodal-example.bxs** - 🆕 Images, audio, video, and documents in AI conversations
- **vision-example.bxs** - Image analysis with vision-capable models
- **embeddings-example.bxs** - Vector embeddings for semantic search
- **token-counting-example.bxs** - Estimating and managing token usage
- **memory-file.bxs** - File-based conversation memory

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

# Vector embeddings
boxlang examples/advanced/embeddings-example.bxs

# Pipeline workflows
boxlang examples/pipelines/runnable-example.bxs
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
- `advanced/embeddings-example.bxs` - Vector search and semantic similarity
- `advanced/token-counting-example.bxs` - Cost optimization

### By Use Case

**💬 Chat & Conversation**

- `basic/streaming-example.bxs` - Real-time chat responses
- `advanced/memory-file.bxs` - Conversation history

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

**📊 Data Processing**

- `advanced/embeddings-example.bxs` - Semantic search and similarity
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
