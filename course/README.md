# BoxLang AI Training Course

**Master AI Development with BoxLang - From Beginner to Production**

This comprehensive, hands-on course will take you from zero AI knowledge to building production-ready AI applications using BoxLang. Each lesson includes theory, practical examples, hands-on labs, and real-world projects.

## 🎯 Course Overview

**Duration**: 12 Lessons (~24 hours total)
**Level**: Beginner to Advanced
**Prerequisites**: Basic BoxLang knowledge
**Updated**: November 2025 - Now includes Structured Output, Advanced Agents, and Production Patterns

### What You'll Learn

- ✅ Integrate AI into BoxLang applications
- ✅ Work with multiple AI providers (OpenAI, Claude, Ollama, and more)
- ✅ Build conversational interfaces with context and memory
- ✅ Extract type-safe data with structured output
- ✅ Create AI tools and function calling
- ✅ Implement persistent memory systems
- ✅ Build autonomous AI agents with tools and memory
- ✅ Design reusable AI pipelines and workflows
- ✅ Deploy and monitor production AI applications

### What You'll Build

By the end of this course, you'll have built:
- 🤖 A customer support chatbot with tools and structured output
- 📝 A code review assistant with type-safe responses
- 🔍 A research agent with web search and citations
- 📊 An AI-powered data extraction and analysis system
- 🔄 Multi-model pipelines for content generation
- 🌐 A production-ready AI API with monitoring

## 📚 Course Curriculum

> **Note**: This curriculum represents the recommended learning path. Folder names in `course/` may differ from lesson numbers (e.g., `lesson-06-tools` is conceptually "Lesson 6: Structured Output" in the path below). Follow the conceptual order for best learning experience.

### Part 1: Foundations (Lessons 1-3)

### [Lesson 1: Introduction to AI in BoxLang](lesson-01-introduction/)
**Duration**: 1.5 hours | **Level**: Beginner

**Learning Objectives**:
- Understand AI fundamentals, LLMs, and tokens
- Install and configure the bx-ai module
- Make your first AI call
- Understand costs and token management

**Topics**:
- What is AI and LLM?
- BoxLang AI module overview
- Installation and configuration
- Your first "Hello AI" program
- Understanding tokens and costs

**Examples**: hello-ai.bxs, token-calculation.bxs
**Lab**: Setup environment and create a simple AI chatbot
**Project**: Build a "Magic 8-Ball" AI fortune teller

---

### [Lesson 2: Basic AI Conversations](lesson-02-first-chat/)
**Duration**: 2 hours | **Level**: Beginner

**Learning Objectives**:
- Master the `aiChat()` function
- Build multi-turn conversations
- Understand message roles and context
- Handle different return formats

**Topics**:
- The `aiChat()` function in depth
- Sending messages and receiving responses
- Multi-turn conversations with history
- Message roles (system, user, assistant)
- Return formats (single, all, raw)
- Building conversation arrays

**Examples**: basic-chat.bxs, conversation-history.bxs, system-prompts.bxs
**Lab**: Build a Q&A bot with conversation history
**Project**: Create a personal coding tutor chatbot

---

### [Lesson 3: Working with AI Providers](lesson-03-providers/)
**Duration**: 2 hours | **Level**: Beginner

**Learning Objectives**:
- Work with multiple AI providers
- Switch providers dynamically
- Use local AI with Ollama
- Implement provider fallbacks

**Topics**:
- OpenAI / GPT models (gpt-4o, gpt-4o-mini)
- Claude / Anthropic (claude-3.5-sonnet)
- Google Gemini
- Ollama for local/private AI
- Provider switching and fallbacks
- API key management and security

**Examples**: multi-provider.bxs, ollama-local.bxs, provider-fallback.bxs
**Lab**: Create a multi-provider AI application
**Project**: Build a provider comparison dashboard

---

### Part 2: Advanced Interactions (Lessons 4-6)

### [Lesson 4: Controlling AI Behavior with Parameters](lesson-04-parameters/)
**Duration**: 2 hours | **Level**: Intermediate

**Learning Objectives**:
- Fine-tune AI responses with parameters
- Control creativity and randomness
- Optimize for different use cases
- Handle errors and timeouts

**Topics**:
- Temperature (creativity vs consistency)
- Max tokens (response length control)
- Top-p (nucleus sampling)
- Frequency and presence penalties
- Model selection strategies
- Timeout and error handling
- Best practices for parameter tuning

**Examples**: temperature-control.bxs, token-limits.bxs, creative-vs-factual.bxs
**Lab**: Build a creative writing assistant with parameter controls
**Project**: Create a tone-adjustable content generator

---

### [Lesson 5: Real-Time Streaming](lesson-05-streaming/)
**Duration**: 2 hours | **Level**: Intermediate

**Learning Objectives**:
- Implement streaming responses
- Build responsive user interfaces
- Handle streaming errors
- Use async operations

**Topics**:
- Understanding streaming and chunking
- The `aiChatStream()` function
- Processing chunks and deltas
- Building responsive interfaces
- Progress indicators and feedback
- Error handling in streams
- Async operations with futures

**Examples**: basic-streaming.bxs, streaming-with-progress.bxs, async-operations.bxs
**Lab**: Create a live chat interface with streaming
**Project**: Build a real-time code generator with streaming

---

### [Lesson 6: Structured Output - Type-Safe AI Responses](lesson-06-structured-output/)
**Duration**: 2.5 hours | **Level**: Intermediate

**Learning Objectives**:
- Extract structured data from AI responses
- Use classes and struct templates
- Extract arrays and multiple schemas
- Validate and transform AI output

**Topics**:
- Why structured output matters
- Using classes for type safety
- Struct templates for quick extraction
- Extracting arrays of objects
- Multiple schemas (different entity types)
- The `aiPopulate()` function
- Testing with mock data
- Structured output in pipelines

**Examples**: basic-person.bxs, struct-template.bxs, array-extraction.bxs, multiple-schemas.bxs
**Lab**: Build a data extraction tool with type-safe output
**Project**: Create an invoice parser with structured output

---

### Part 3: Tools and Memory (Lessons 7-8)

### [Lesson 7: AI Tools and Function Calling](lesson-07-tools/)
**Duration**: 2.5 hours | **Level**: Intermediate

**Learning Objectives**:
- Enable AI to execute functions
- Create tools with proper descriptions
- Handle tool results and errors
- Chain multiple tools

**Topics**:
- What are AI tools and function calling?
- Creating tools with `aiTool()`
- Tool descriptions and arguments
- Real-time data access
- Multiple tools and tool selection
- Tool chaining and composition
- Error handling and fallbacks
- Tool result formatting

**Examples**: weather-tool.bxs, database-tool.bxs, multi-tool-agent.bxs
**Lab**: Build a weather bot with live data lookup
**Project**: Create a smart assistant with calculator, weather, and database tools

---

### [Lesson 8: Memory Systems](lesson-08-memory/)
**Duration**: 2 hours | **Level**: Intermediate

**Learning Objectives**:
- Implement conversation memory
- Manage context windows
- Persist conversations
- Optimize memory usage

**Topics**:
- Why memory matters in AI
- Simple memory (RAM-based)
- Window memory (sliding buffer)
- Session memory (web applications)
- File memory (persistence)
- Cache memory (distributed systems)
- Memory management strategies
- Context window optimization

**Examples**: simple-memory.bxs, window-memory.bxs, file-persistence.bxs
**Lab**: Build a conversational assistant that remembers context
**Project**: Create a multi-session chatbot with persistent history

---

### Part 4: Advanced Patterns (Lessons 9-10)

### [Lesson 9: Building AI Agents](lesson-09-agents/)
**Duration**: 3 hours | **Level**: Advanced

**Learning Objectives**:
- Build autonomous AI agents
- Combine memory, tools, and instructions
- Implement multi-step reasoning
- Debug and monitor agents

**Topics**:
- What are AI agents?
- Creating agents with `aiAgent()`
- Combining memory and tools
- Agent instructions and system prompts
- Multi-step reasoning and planning
- Agent debugging techniques
- Monitoring and logging
- Production agent patterns

**Examples**: basic-agent.bxs, agent-with-tools.bxs, support-agent.bxs, research-agent.bxs
**Lab**: Build a customer support agent with order management
**Project**: Create a research agent with web search and citations

---

### [Lesson 10: AI Pipelines and Workflows](lesson-10-pipelines/)
**Duration**: 2.5 hours | **Level**: Advanced

**Learning Objectives**:
- Design reusable AI pipelines
- Chain transformations
- Use multiple models in workflows
- Stream through pipelines

**Topics**:
- Understanding pipelines and runnables
- Message templates and reusability
- Chaining transformations
- Multi-model workflows
- Pipeline streaming
- Structured output in pipelines
- Error handling and fallbacks
- Testing pipelines

**Examples**: simple-pipeline.bxs, multi-model.bxs, streaming-pipeline.bxs, templates.bxs
**Lab**: Create a content generation pipeline
**Project**: Build a multi-stage document processor

---

### Part 5: Production (Lessons 11-12)

### [Lesson 11: Production Deployment](lesson-11-production/)
**Duration**: 2.5 hours | **Level**: Advanced

**Learning Objectives**:
- Deploy AI applications to production
- Optimize performance and costs
- Implement security best practices
- Monitor and maintain AI systems

**Topics**:
- Performance optimization techniques
- Cost management and monitoring
- Security best practices
- API key rotation and management
- Rate limiting and throttling
- Caching strategies
- Error handling and fallbacks
- Testing AI applications
- Monitoring and observability
- Logging and debugging

**Examples**: caching.bxs, rate-limiting.bxs, error-handling.bxs, monitoring.bxs
**Lab**: Deploy a production-ready AI API
**Project**: Build a fully monitored AI service with all best practices

---

### [Lesson 12: Advanced Topics and Integration](lesson-12-advanced/)
**Duration**: 2 hours | **Level**: Advanced

**Learning Objectives**:
- Integrate embeddings and vector search
- Use vision models for image analysis
- Implement custom integrations
- Build complete AI systems

**Topics**:
- Vector embeddings with `aiEmbed()`
- Semantic search and similarity
- Vision models and image analysis
- Text chunking and processing
- Token counting and estimation
- Event system and interceptors
- Custom provider integration
- Building complete AI systems

**Examples**: embeddings.bxs, vision-analysis.bxs, chunking.bxs, events.bxs
**Lab**: Build a semantic search system
**Project**: Create a complete AI-powered application

---

## 🚀 Getting Started

### Prerequisites

Before starting this course, ensure you have:

1. **BoxLang Installed** (version 1.0.0 or higher)
   ```bash
   # Verify installation
   boxlang --version
   ```

2. **bx-ai Module Installed**
   ```bash
   # Install via box CLI
   box install bx-ai
   ```

3. **API Keys** (at least one):
   - OpenAI: https://platform.openai.com/api-keys
   - OR Ollama installed locally (free, no API key needed)

4. **Code Editor**: VS Code with BoxLang extension recommended

### Course Setup

1. **Clone or download this course**:
   ```bash
   cd course
   ```

2. **Set up environment variables**:
   ```bash
   # Create .env file
   echo "OPENAI_API_KEY=your-key-here" > .env
   ```

3. **Configure BoxLang**:
   ```json
   // boxlang.json
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

4. **Test your setup**:
   ```bash
   boxlang lesson-01-introduction/labs/test-setup.bxs
   ```

## 📖 How to Use This Course

### For Self-Paced Learning

1. **Read the lesson** - Start with the README.md in each lesson folder
2. **Review examples** - Study the example code in the `examples/` directory
3. **Complete the lab** - Work through hands-on exercises in `labs/`
4. **Check solutions** - Compare with provided solutions in `labs/solutions/`
5. **Build the project** - Apply what you learned in the lesson project

### For Instructors

- Each lesson is self-contained with teaching materials
- Labs include step-by-step instructions
- Solutions are provided for all exercises
- Projects can be assigned as homework
- Additional resources and quiz questions included

### Time Commitment

- **Fast Track**: Focus on examples and labs (10 hours)
- **Standard**: Complete all materials (20 hours)
- **Deep Dive**: Including all projects and exercises (30 hours)

## 🎓 Learning Path

```
Part 1: Foundations (Lessons 1-3)
Lesson 1: AI Basics & Setup
    ↓
Lesson 2: Basic Conversations
    ↓
Lesson 3: Multiple Providers
    ↓
Part 2: Advanced Interactions (Lessons 4-6)
Lesson 4: Parameters & Control
    ↓
Lesson 5: Streaming
    ↓
Lesson 6: Structured Output ⭐
    ↓
Part 3: Tools & Memory (Lessons 7-8)
Lesson 7: Tools & Functions
    ↓
Lesson 8: Memory Systems
    ↓
Part 4: Advanced Patterns (Lessons 9-10)
Lesson 9: AI Agents
    ↓
Lesson 10: Pipelines & Workflows
    ↓
Part 5: Production (Lessons 11-12)
Lesson 11: Production Deployment
    ↓
Lesson 12: Advanced Topics
```

### Recommended Pace

- **Week 1**: Lessons 1-3 (Foundations)
- **Week 2**: Lessons 4-6 (Advanced Interactions + Structured Output)
- **Week 3**: Lessons 7-8 (Tools & Memory)
- **Week 4**: Lessons 9-10 (Agents & Pipelines)
- **Week 5**: Lessons 11-12 (Production & Advanced)
- **Week 6**: Final Project Integration

## 🏆 Course Projects

Throughout the course, you'll build progressively more complex projects:

1. **Magic 8-Ball AI** (Lesson 1) - Your first AI interaction with token management
2. **Coding Tutor Bot** (Lesson 2) - Multi-turn conversation chatbot
3. **Provider Dashboard** (Lesson 3) - Multi-provider comparison tool
4. **Tone-Adjustable Writer** (Lesson 4) - Parameter-controlled content generator
5. **Real-Time Code Gen** (Lesson 5) - Streaming code generation interface
6. **Invoice Parser** (Lesson 6) - Structured data extraction with type safety ⭐
7. **Smart Assistant** (Lesson 7) - Multi-tool AI with calculator, weather, database
8. **Multi-Session Chat** (Lesson 8) - Persistent conversation history
9. **Research Agent** (Lesson 9) - Autonomous agent with web search and citations
10. **Document Processor** (Lesson 10) - Multi-stage content pipeline
11. **Monitored AI Service** (Lesson 11) - Production-ready API with observability
12. **AI-Powered App** (Lesson 12) - Complete system with embeddings and vision

## 📋 Assessment

Each lesson includes:
- ✅ Knowledge check questions
- ✅ Hands-on lab exercises
- ✅ Coding challenges
- ✅ Project work

## 🆘 Getting Help

- **Documentation**: [BoxLang AI Docs](../docs/README.md)
- **Examples**: [Code Examples](../examples/README.md)
- **Issues**: [GitHub Issues](https://github.com/ortus-boxlang/bx-ai/issues)
- **Community**: [BoxLang Community](https://boxlang.io/community)

## 📜 Certificate of Completion

After completing all 12 lessons and projects, you'll have:

- ✅ Built 12 complete AI applications
- ✅ Mastered BoxLang AI development from basics to advanced
- ✅ Experience with structured output, agents, pipelines, and production deployment
- ✅ Portfolio-ready projects demonstrating real-world AI integration
- ✅ Deep understanding of AI tools, memory systems, and best practices

## 📚 Additional Resources

### Recommended Reading
- [OpenAI Documentation](https://platform.openai.com/docs)
- [Anthropic Claude Docs](https://docs.anthropic.com/)
- [Prompt Engineering Guide](https://www.promptingguide.ai/)

### Tools & Extensions
- VS Code BoxLang Extension
- Ollama for local AI
- Postman for API testing

## 🤝 Contributing

Found an error or want to improve the course? Contributions welcome!

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## 📝 License

This course is released under the Apache 2.0 license, same as the bx-ai module.

---

**Ready to start?** Head to [Lesson 1: Introduction](lesson-01-introduction/) and begin your AI journey! 🚀
