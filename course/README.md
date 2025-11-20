# BoxLang AI Training Course

**Master AI Development with BoxLang - From Beginner to Production**

This comprehensive, hands-on course will take you from zero AI knowledge to building production-ready AI applications using BoxLang. Each lesson includes theory, practical examples, hands-on labs, and real-world projects.

## 🎯 Course Overview

**Duration**: 10 Lessons (~20 hours total)
**Level**: Beginner to Intermediate
**Prerequisites**: Basic BoxLang knowledge

### What You'll Learn

- ✅ Integrate AI into BoxLang applications
- ✅ Work with multiple AI providers (OpenAI, Claude, Ollama)
- ✅ Build conversational interfaces with context
- ✅ Create AI tools and function calling
- ✅ Implement persistent memory systems
- ✅ Build autonomous AI agents
- ✅ Design AI pipelines and workflows
- ✅ Deploy production AI applications

### What You'll Build

By the end of this course, you'll have built:
- 🤖 A customer support chatbot with tools
- 📝 A code review assistant
- 🔍 A research agent with web search
- 📊 An AI-powered data analysis system
- 🌐 A production-ready AI API

## 📚 Course Curriculum

### [Lesson 1: Introduction to AI in BoxLang](lesson-01-introduction/)
**Duration**: 1.5 hours
**Objective**: Understand AI fundamentals and setup your development environment

- What is AI and LLM?
- BoxLang AI module overview
- Installation and configuration
- Your first "Hello AI" program
- Understanding tokens and costs

**Lab**: Install the module and make your first AI call

---

### [Lesson 2: Your First AI Chat](lesson-02-first-chat/)
**Duration**: 2 hours
**Objective**: Master basic AI chat interactions

- The `aiChat()` function
- Sending messages and receiving responses
- Multi-turn conversations
- Message roles (system, user, assistant)
- Return formats (single, all, raw)

**Lab**: Build a simple Q&A bot

---

### [Lesson 3: Working with AI Providers](lesson-03-providers/)
**Duration**: 2 hours
**Objective**: Learn to work with different AI providers

- OpenAI / GPT models
- Claude / Anthropic
- Google Gemini
- Local AI with Ollama
- Provider switching and fallbacks
- API key management

**Lab**: Create a multi-provider AI application

---

### [Lesson 4: Controlling AI Behavior](lesson-04-parameters/)
**Duration**: 2 hours
**Objective**: Fine-tune AI responses with parameters

- Temperature (creativity vs consistency)
- Max tokens (response length)
- Top-p and frequency penalty
- Model selection
- Timeout and error handling
- Best practices for parameter tuning

**Lab**: Build a creative writing assistant with parameter controls

---

### [Lesson 5: Real-Time Streaming](lesson-05-streaming/)
**Duration**: 2 hours
**Objective**: Implement streaming responses for better UX

- Understanding streaming
- The `aiChatStream()` function
- Handling chunks and deltas
- Building responsive interfaces
- Error handling in streams
- Async operations

**Lab**: Create a live chat interface with streaming

---

### [Lesson 6: AI Tools and Function Calling](lesson-06-tools/)
**Duration**: 2.5 hours
**Objective**: Enable AI to execute functions and access data

- What are AI tools?
- Creating tools with `aiTool()`
- Tool descriptions and arguments
- Real-time data access
- Multiple tools and tool chaining
- Error handling and fallbacks

**Lab**: Build a weather bot with live data lookup

---

### [Lesson 7: Memory Systems](lesson-07-memory/)
**Duration**: 2 hours
**Objective**: Implement conversation memory and context

- Why memory matters
- Simple memory (RAM-based)
- Window memory (sliding buffer)
- Session memory (web apps)
- File memory (persistence)
- Cache memory (distributed)
- Memory management strategies

**Lab**: Build a conversational assistant that remembers context

---

### [Lesson 8: Building AI Agents](lesson-08-agents/)
**Duration**: 3 hours
**Objective**: Create autonomous AI agents with memory and tools

- What are AI agents?
- Creating agents with `aiAgent()`
- Combining memory and tools
- Agent instructions and behavior
- Multi-step reasoning
- Agent debugging and monitoring

**Lab**: Build a customer support agent with order management

---

### [Lesson 9: AI Pipelines and Workflows](lesson-09-pipelines/)
**Duration**: 2.5 hours
**Objective**: Design complex multi-step AI workflows

- Understanding pipelines
- Message templates and reusability
- Chaining transformations
- Multi-model workflows
- Pipeline streaming
- Error handling in pipelines

**Lab**: Create a content generation pipeline

---

### [Lesson 10: Production Deployment](lesson-10-production/)
**Duration**: 2.5 hours
**Objective**: Deploy and maintain AI applications in production

- Performance optimization
- Cost management and monitoring
- Security best practices
- Rate limiting and caching
- Error handling and fallbacks
- Testing AI applications
- Monitoring and observability

**Lab**: Deploy a production-ready AI API

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
Lesson 1: Setup & Basics
    ↓
Lesson 2-3: Core AI Interaction
    ↓
Lesson 4-5: Advanced Features
    ↓
Lesson 6-7: Tools & Memory
    ↓
Lesson 8: AI Agents
    ↓
Lesson 9: Pipelines
    ↓
Lesson 10: Production
```

### Recommended Pace

- **Week 1**: Lessons 1-3 (Foundation)
- **Week 2**: Lessons 4-5 (Advanced Interactions)
- **Week 3**: Lessons 6-7 (Tools & Memory)
- **Week 4**: Lessons 8-9 (Agents & Pipelines)
- **Week 5**: Lesson 10 + Final Project

## 🏆 Course Projects

Throughout the course, you'll build progressively more complex projects:

1. **Hello AI** (Lesson 1) - Your first AI interaction
2. **Q&A Bot** (Lesson 2) - Simple chatbot
3. **Multi-Provider App** (Lesson 3) - Provider switching
4. **Writing Assistant** (Lesson 4) - Parameter-controlled generation
5. **Live Chat** (Lesson 5) - Streaming interface
6. **Weather Bot** (Lesson 6) - Tool-enabled assistant
7. **Context Bot** (Lesson 7) - Memory-enabled conversations
8. **Support Agent** (Lesson 8) - Full-featured agent
9. **Content Pipeline** (Lesson 9) - Multi-step workflow
10. **Production API** (Lesson 10) - Deployable AI service

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

After completing all 10 lessons and projects, you'll have:
- ✅ Built 10 complete AI applications
- ✅ Mastered BoxLang AI development
- ✅ Portfolio-ready projects
- ✅ Production deployment experience

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
