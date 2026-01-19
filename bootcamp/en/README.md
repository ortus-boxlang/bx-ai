# 🚀 BoxLang AI Bootcamp

**Get Started with AI Development in One Day**

Welcome to the BoxLang AI Bootcamp! This intensive, hands-on course will take you from zero AI experience to building intelligent applications with BoxLang in just 8-9 hours.

```
╔════════════════════════════════════════════════════════════════════╗
║                                                                    ║
║   ⚡ BoxLang AI Bootcamp ⚡                                         ║
║                                                                    ║
║   🎯 Learn AI fundamentals in one day                             ║
║   🛠️ Build real applications                                       ║
║   🔄 Switch between AI providers                                   ║
║   🤖 Create your first AI agent                                    ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

## 📋 Bootcamp Overview

| | |
|---|---|
| **Duration** | 8-9 hours (1 day) |
| **Level** | Complete Beginner |
| **Prerequisites** | Basic programming knowledge |
| **Goal** | Build AI-powered applications with BoxLang |

### What You'll Learn

By the end of this bootcamp, you will:

- ✅ Understand how AI and Large Language Models work
- ✅ Make your first AI API call in minutes
- ✅ Build multi-turn conversations with context
- ✅ Switch between AI providers (OpenAI, Claude, Ollama)
- ✅ Extract structured data from AI responses
- ✅ Create tools that AI can use
- ✅ Build an autonomous AI agent
- ✅ Load and process documents for RAG systems
- ✅ Create MCP servers and clients for remote tools

### What You'll Build

Throughout this bootcamp, you'll create:

1. 🎱 **Magic 8-Ball** - Your first AI application
2. 💬 **Chat Assistant** - Multi-turn conversation bot
3. 🔄 **Provider Switcher** - Work with different AI models
4. 📝 **Data Extractor** - Type-safe structured output
5. 🌤️ **Weather Bot** - AI with real-time tools
6. 🤖 **Smart Agent** - Autonomous AI assistant
7. 📚 **RAG System** - Document-based knowledge retrieval
8. 🔌 **MCP Server** - Remote tool exposure and integration

---

## 🗺️ Curriculum

```
┌─────────────────────────────────────────────────────────────────┐
│                     BOOTCAMP TIMELINE                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │   Lesson 1   │──▶│   Lesson 2   │──▶│   Lesson 3   │        │
│  │   45 mins    │   │   60 mins    │   │   45 mins    │        │
│  │  🚀 Setup    │   │  💬 Chats    │   │  🔄 Providers│        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
│          │                                    │                 │
│          │            ☕ BREAK                │                 │
│          ▼                                    ▼                 │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │   Lesson 4   │──▶│   Lesson 5   │──▶│   Lesson 6   │        │
│  │   60 mins    │   │   60 mins    │   │   90 mins    │        │
│  │  📝 Output   │   │  🛠️ Tools    │   │  🤖 Agents   │        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
│          │                                    │                 │
│          │            ☕ BREAK                │                 │
│          ▼                                    ▼                 │
│  ┌──────────────┐   ┌──────────────┐                            │
│  │   Lesson 7   │──▶│   Lesson 8   │                            │
│  │   60 mins    │   │   60 mins    │                            │
│  │  📚 Loaders  │   │  🔌 MCP      │                            │
│  └──────────────┘   └──────────────┘                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                         Total: ~8.5 hours
```

### Lesson Breakdown

| Lesson | Title | Duration | What You'll Learn |
|--------|-------|----------|-------------------|
| **1** | [Getting Started](lesson-01-getting-started/) | 45 mins | Setup, first AI call, understanding tokens |
| **2** | [Conversations & Messages](lesson-02-conversations/) | 60 mins | Multi-turn chat, message roles, conversation history |
| **3** | [Switching Providers](lesson-03-providers/) | 45 mins | OpenAI, Claude, Ollama, provider switching |
| **4** | [Structured Output](lesson-04-structured-output/) | 60 mins | Type-safe responses, classes, data extraction |
| **5** | [AI Tools](lesson-05-tools/) | 60 mins | Function calling, real-time data, tool creation |
| **6** | [Building Agents](lesson-06-agents/) | 90 mins | Autonomous AI, memory, multi-step tasks |
| **7** | [Loaders & Documents](lesson-07-loaders-documents/) | 60 mins | File loading, chunking, RAG systems, vector memory |
| **8** | [MCP Servers & Clients](lesson-08-mcp-servers-clients/) | 60 mins | Model Context Protocol, remote tools, server creation |

---

## 🎯 Prerequisites

Before starting, ensure you have:

### Required

1. **BoxLang Installed** ([Download](https://boxlang.io))
   ```bash
   boxlang --version
   # Should output: BoxLang 1.x.x
   ```

2. **Text Editor** (VS Code recommended)

3. **At least ONE API key** (choose one):
   - **OpenAI** (Recommended): https://platform.openai.com/api-keys
   - **Claude**: https://console.anthropic.com/
   - **Ollama** (Free/Local): https://ollama.ai

### Optional but Helpful

- Basic programming knowledge (variables, functions, loops)
- Familiarity with JSON
- Command line basics

---

## ⚡ Quick Start

### 5-Minute Setup

1. **Install the bx-ai module**:
   ```bash
   install-bx-module bx-ai
   ```

2. **Set your API key**:
   ```bash
   # Create .env file
   echo "OPENAI_API_KEY=sk-your-key-here" > .env
   ```

3. **Run your first AI call**:
   ```java
   // hello-ai.bxs
   answer = aiChat( "Hello, AI!" )
   println( answer )
   ```
   ```bash
   boxlang hello-ai.bxs
   ```

4. **You're ready!** 🎉

---

## 📁 Folder Structure

```
bootcamp/
├── README.md                      # This file
├── lesson-01-getting-started/     # Setup & first AI call
│   ├── README.md                  # Lesson content
│   ├── examples/                  # Code examples
│   └── labs/                      # Hands-on exercises
├── lesson-02-conversations/       # Multi-turn chat
│   ├── README.md
│   ├── examples/
│   └── labs/
├── lesson-03-providers/           # OpenAI, Claude, Ollama
│   ├── README.md
│   ├── examples/
│   └── labs/
├── lesson-04-structured-output/   # Type-safe responses
│   ├── README.md
│   ├── examples/
│   └── labs/
├── lesson-05-tools/               # Function calling
│   ├── README.md
│   ├── examples/
│   └── labs/
└── lesson-06-agents/              # Autonomous AI
    ├── README.md
    ├── examples/
    └── labs/
```

---

## 🧠 Core Concepts

Before diving in, here are the key concepts we'll cover:

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI APPLICATION FLOW                          │
└─────────────────────────────────────────────────────────────────┘

  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
  │   You   │───▶│ BoxLang │───▶│   AI    │───▶│ Response│
  │  (User) │    │   App   │    │Provider │    │  (Text) │
  └─────────┘    └─────────┘    └─────────┘    └─────────┘
       │              │              │              │
       │              │              │              │
   "What is         aiChat()     API Call      "BoxLang is
   BoxLang?"                                   a modern..."


  ┌─────────────────────────────────────────────────────────────┐
  │                         KEY TERMS                            │
  ├──────────────────┬──────────────────────────────────────────┤
  │ LLM              │ Large Language Model - AI that           │
  │                  │ understands and generates text           │
  ├──────────────────┼──────────────────────────────────────────┤
  │ Token            │ A piece of text the AI processes         │
  │                  │ (~4 characters = 1 token)                │
  ├──────────────────┼──────────────────────────────────────────┤
  │ Provider         │ Company that runs AI models              │
  │                  │ (OpenAI, Claude, Google, Ollama)         │
  ├──────────────────┼──────────────────────────────────────────┤
  │ Prompt           │ The message/question you send to AI      │
  ├──────────────────┼──────────────────────────────────────────┤
  │ Tool             │ A function the AI can call               │
  ├──────────────────┼──────────────────────────────────────────┤
  │ Agent            │ AI that can plan and execute tasks       │
  └──────────────────┴──────────────────────────────────────────┘
```

---

## 📊 Learning Path

This bootcamp is designed to build skills progressively:

```
                          SKILL PROGRESSION
     ─────────────────────────────────────────────────────▶

     BEGINNER              INTERMEDIATE              ADVANCED
         │                      │                        │
         ▼                      ▼                        ▼
    ┌─────────┐            ┌─────────┐             ┌─────────┐
    │Lesson 1 │            │Lesson 4 │             │Lesson 6 │
    │Lesson 2 │            │Lesson 5 │             │         │
    │Lesson 3 │            │         │             │         │
    └─────────┘            └─────────┘             └─────────┘
         │                      │                        │
         ▼                      ▼                        ▼
    • First call           • Structured data        • Full agents
    • Chat basics          • Function calling       • Autonomous AI
    • Providers            • Real-time tools        • Complex tasks
```

---

## 💡 Tips for Success

### Before You Start

1. **Have your API key ready** - You'll need this immediately
2. **Keep docs nearby** - The [main readme](../../readme.md) is a great reference
3. **Follow along** - Type the code, don't just copy/paste
4. **Experiment** - Change things and see what happens!

### During the Bootcamp

1. **Complete all labs** - They reinforce the concepts
2. **Take breaks** - After lessons 3 and 5 are good spots
3. **Ask questions** - If something's unclear, look it up
4. **Build incrementally** - Each lesson builds on the last

### After the Bootcamp

1. **Explore the full course** - [12-lesson course](../../course/) goes deeper
2. **Check examples** - The [examples folder](../../examples/) has more code
3. **Read the docs** - [Documentation](https://ai.ortusbooks.com/) covers everything
4. **Build something!** - Best way to learn is by doing

---

## 🆘 Getting Help

Stuck? Here's where to get help:

| Resource | Link |
|----------|------|
| Full Documentation | [docs/](https://ai.ortusbooks.com/) |
| Code Examples | [examples/](../../examples/README.md) |
| GitHub Issues | [Report a bug](https://github.com/ortus-boxlang/bx-ai/issues) |
| Community | [BoxLang Community](https://boxlang.io/community) |

---

## 🎓 What's Next?

After completing this bootcamp, you can:

1. **Go Deeper** - Take the [full 12-lesson course](../../course/)
2. **Explore Advanced Topics**:
   - Vector embeddings and semantic search
   - Streaming responses
   - Pipeline workflows
   - Production deployment
3. **Build Real Projects**:
   - Customer support bots
   - Content generators
   - Data analysis tools
   - Research assistants

---

## 📜 License

This bootcamp is released under the Apache 2.0 license, same as the bx-ai module.

---

**Ready to start?** Let's go! 🚀

👉 **[Start Lesson 1: Getting Started](lesson-01-getting-started/)**
