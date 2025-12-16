---
description: Core building blocks for AI agents and pipelines in BoxLang - your guide to mastering AI development
icon: puzzle-piece
---

# 🧩 Main Components

Welcome to the heart of BoxLang AI! This section introduces the essential building blocks you'll use to create intelligent applications. Whether you're building chatbots, autonomous agents, or complex AI workflows, these components are your toolkit.

## 🎯 What You'll Learn

BoxLang AI uses a **runnable pipeline architecture** - think of it as composable LEGO blocks for AI:

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Messages   │───▶│  AI Model   │───▶│ Transform   │───▶│   Result    │
│  Template   │    │  (OpenAI)   │    │  (Extract)  │    │             │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

Each component:

- 🔗 **Chains easily** - Connect pieces with `.to()`
- ♻️ **Reuses workflows** - Define once, run many times
- 🧩 **Composes freely** - Mix and match as needed
- 🎯 **Stays flexible** - Swap providers without refactoring


---

## 📚 Learning Path

We recommend learning the components in this order for the best experience:

```
START HERE
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 1️⃣ Models - Connect to AI providers (OpenAI, Claude, etc.)   │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 2️⃣ Messages - Build conversations with templates             │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 3️⃣ Streaming - Real-time responses for better UX             │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 4️⃣ Structured Output - Extract typed data from responses     │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 5️⃣ Tools - Enable AI to call your functions                  │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 6️⃣ Memory - Maintain conversation context                    │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 7️⃣ Agents - Autonomous AI with memory & tools                │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 8️⃣ Pipelines - Build composable AI workflows                 │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 9️⃣ Transformers - Data processing in pipelines               │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 🔟 Vector Memory - Semantic search for RAG apps              │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 1️⃣1️⃣ Document Loaders - Import content from any source       │
└──────────────────────────────────────────────────────────────┘
   ↓
┌──────────────────────────────────────────────────────────────┐
│ 1️⃣2️⃣ RAG - Complete retrieval-augmented generation workflow  │
└──────────────────────────────────────────────────────────────┘
```

**Quick Paths:**

- 🚀 **Building a chatbot?** → Start with Models → Messages → Memory → Agents
- 📊 **Extracting data?** → Start with Models → Structured Output → Transformers → Pipelines
- 🔍 **Building RAG?** → Start with Document Loaders → Vector Memory → RAG → Agents
- 🛠️ **Creating workflows?** → Start with Models → Transformers → Pipelines → Agents

---

## 🧱 Core Components

### 1️⃣ [AI Models](models.md)

**What:** Direct AI provider integrations (OpenAI, Claude, Gemini, Ollama, etc.)

**When to use:** Every AI application - this is your foundation

**Quick example:**

```javascript
model = aiModel( "openai" )
response = model.run( "Explain quantum computing" )
```

**Key concepts:**

- Provider abstraction
- Parameter configuration
- Return formats (text, JSON, XML, raw)
- Pipeline composition

→ **[Read Models Guide](models.md)**

---

### 2️⃣ [Messages](messages.md)

**What:** Reusable message templates with dynamic placeholders and multi-modal content

**When to use:** Repeated prompts, variable content, organized conversations

**Quick example:**

```javascript
template = aiMessage()
    .system( "You are a ${role}" )
    .user( "Explain ${topic} in simple terms" )
	.image( "/opt/images/myImage.png" )

response = template
    .to( aiModel( "openai" ) )
    .run( { role: "teacher", topic: "AI" } )
```

**Key concepts:**

- Role-based messages (system, user, assistant)
- Variable binding with `${}` placeholders
- Multimodal content (images, audio, documents)
- Message reusability

→ **[Read Messages Guide](messages.md)**

---

### 3️⃣ [Streaming](streaming.md)

**What:** Real-time token-by-token response delivery

**When to use:** Interactive UIs, chatbots, long responses

**Quick example:**

```javascript
aiModel( "openai" ).stream(
    onChunk: ( chunk ) => systemOutput( chunk, false ),
    input: "Write a story about a robot"
)
```

**Key concepts:**

- Callback functions
- Progressive UI updates
- Streaming with agents
- Performance optimization

→ **[Read Streaming Guide](streaming.md)**

---

### 4️⃣ [Structured Output](structured-output.md)

**What:** Extract typed data from AI responses into classes/structs

**When to use:** Form extraction, data parsing, type-safe results

**Quick example:**

```javascript
class Person {
    property name="name" type="string";
    property name="age" type="numeric";
}

person = aiChat( "Extract: John is 30" )
    .structuredOutput( new Person() )

println( person.getName() ) // "John"
```

**Key concepts:**
- Class population
- JSON schema generation
- Array extraction
- Validation

→ **[Read Structured Output Guide](structured-output.md)**

---

### 5️⃣ [Tools](tools.md)

**What:** Functions that AI can call to access data or perform actions

**When to use:** Real-time data, external APIs, database queries

**Quick example:**

```javascript
weatherTool = aiTool(
    name: "get_weather",
    description: "Get current weather",
    action: ( location ) => getWeatherAPI( location )
).describeLocation( "City name, e.g. London, New York" )

agent = aiAgent( tools: [ weatherTool ] )
response = agent.run( "What's the weather in Boston?" )
// Agent automatically calls weatherTool
```

**Key concepts:**

- Function calling
- Parameter schemas
- Tool registration
- Autonomous invocation

→ **[Read Tools Guide](tools.md)**

---

### 6️⃣ [Memory](memory.md)

**What:** Conversation context management strategies

**When to use:** Multi-turn conversations, context preservation

**Quick example:**

```javascript
// Keep last 20 messages
memory = aiMemory( "windowed", { maxMessages: 20 } )

agent = aiAgent( memory: memory )
agent.run( "My name is Alice" )
agent.run( "What's my name?" ) // "Alice"
```

**Key concepts:**
- Memory types (windowed, summary, session, file)
- Context limits
- Memory persistence
- Multiple memory strategies

→ **[Read Memory Guide](memory.md)**

---

### 7️⃣ [Agents](agents.md)

**What:** Autonomous AI entities with memory, tools, and reasoning

**When to use:** Complex workflows, multi-step tasks, autonomous behavior

**Quick example:**

```javascript
agent = aiAgent(
    name: "Assistant",
    instructions: "Help users with research",
    tools: [ searchTool, calculatorTool ],
    memory: aiMemory( "windowed" )
)

response = agent.run( "Find info about quantum computing" )
// Agent decides which tools to use automatically
```

**Key concepts:**
- Autonomous reasoning
- Tool selection
- Memory integration
- Sub-agents

→ **[Read Agents Guide](agents.md)**

---

### 8️⃣ [Pipelines](pipelines.md)

**What:** Composable AI workflows - chain models, messages, and transformers

**When to use:** Complex multi-step workflows, reusable templates, data processing flows

**Quick example:**

```javascript
// Reusable pipeline
translator = aiMessage()
    .user( "Translate to ${lang}: ${text}" )
    .toDefaultModel()
    .transform( r => r.content )

spanish = translator.run({ text: "Hello", lang: "Spanish" })
french = translator.run({ text: "Hello", lang: "French" })
```

**Key concepts:**
- Runnable interface (IAiRunnable)
- Fluent chaining with `.to()`
- Template reusability
- Multi-step workflows
- Data flow and transformations

→ **[Read Pipelines Guide](pipelines.md)**

---

### 9️⃣ [Transformers](transformers.md)

**What:** Data processing steps in pipelines

**When to use:** Format conversion, data extraction, custom logic

**Quick example:**

```javascript
pipeline = aiModel( "openai" )
    .to( aiTransform( r => r.content ) )
    .to( aiTransform( text => text.toUpper() ) )

result = pipeline.run( "hello" ) // "HELLO!"
```

**Key concepts:**

- Pipeline transformations
- Data extraction
- Format conversion
- Custom processors

→ **[Read Transformers Guide](transformers.md)**

---

### 🔟 [Vector Memory](vector-memory.md)

**What:** Semantic search through conversation history

**When to use:** RAG applications, knowledge bases, semantic retrieval

**Quick example:**
```javascript
memory = aiMemory( "chromavector" )

// Add documents
memory.add( "Paris is the capital of France" )
memory.add( "Tokyo is the capital of Japan" )

// Search by meaning
results = memory.getRelevant( "French capital", 1 )
// Returns: "Paris is the capital of France"
```

**Key concepts:**
- Embedding generation
- Similarity search
- Vector stores (Chroma, Pinecone, etc.)
- RAG workflows

→ **[Read Vector Memory Guide](vector-memory.md)**

---

### 1️⃣1️⃣ [Document Loaders](document-loaders.md)

**What:** Import content from files, directories, URLs, databases, and APIs

**When to use:** Building knowledge bases, RAG systems, data ingestion pipelines

**Quick example:**
```javascript
// Load a single file
docs = aiDocuments( "/path/to/file.txt" ).load()

// Load entire directory
docs = aiDocuments( "/docs", { recursive: true } ).load()

// Load and chunk for RAG
aiDocuments( "/docs" )
    .chunk( 1000, 200 )
    .toMemory( aiMemory( "chroma" ) )
```

**Key concepts:**
- 12+ built-in loaders (Text, Markdown, CSV, JSON, XML, PDF, etc.)
- Automatic metadata extraction
- Chunking strategies
- Directory traversal
- Direct vector memory integration

→ **[Read Document Loaders Guide](document-loaders.md)**

---

### 1️⃣2️⃣ [RAG (Retrieval-Augmented Generation)](rag.md)

**What:** Complete workflow for answering questions using your documents

**When to use:** Q&A systems, documentation search, knowledge bases, chatbots with domain expertise

**Quick example:**
```javascript
// Complete RAG in 5 lines
memory = aiMemory( "chroma" )
aiDocuments( "./knowledge-base" ).toMemory( memory )

agent = aiAgent(
    instructions: "Answer using provided context",
    memory: memory
)

response = agent.run( "How do I install BoxLang?" )
// Agent retrieves relevant docs, then answers
```

**Key concepts:**
- Document loading and chunking
- Embedding generation
- Vector similarity search
- Context injection
- Source attribution
- Hybrid search (keyword + semantic)

→ **[Read RAG Guide](rag.md)**

---

## 🔗 Understanding Pipelines

Pipelines are the foundation of BoxLang AI - they connect components into workflows:

### Basic Pipeline Flow

```
Input Data
   ↓
┌─────────────────┐
│ Message Builder │ ← Constructs conversation
└─────────────────┘
   ↓
┌─────────────────┐
│   AI Model      │ ← Generates response
└─────────────────┘
   ↓
┌─────────────────┐
│  Transformer    │ ← Processes output
└─────────────────┘
   ↓
Final Result
```

### The `.to()` Method

Chain components together:

```javascript
pipeline = aiMessage()
    .user( "Explain ${topic}" )
    .to( aiModel( "openai" ) )
    .to( aiTransform( r => r.content.toUpper() ) )

result = pipeline.run( { topic: "AI" } )
```

### Pipeline Benefits

```
┌─────────────────────────────────────────────────────────┐
│                    PIPELINE BENEFITS                    │
├─────────────────────────────────────────────────────────┤
│ ✅ Reusability  - Define once, run many times           │
│ ✅ Composability - Mix and match components             │
│ ✅ Testability  - Test each step independently          │
│ ✅ Flexibility  - Swap providers without refactoring    │
│ ✅ Clarity      - Self-documenting code flow            │
└─────────────────────────────────────────────────────────┘
```

**Example - Reusable Pipeline:**

```javascript
// Define once
explainer = aiMessage()
    .system( "You are a ${style} teacher" )
    .user( "Explain ${topic}" )
    .to( aiModel( "openai" ) )

// Use many times
explainer.run( { style: "patient", topic: "variables" } )
explainer.run( { style: "concise", topic: "functions" } )
explainer.run( { style: "detailed", topic: "classes" } )
```

---

## 🎨 Common Patterns

### Pattern 1: Simple Q&A

```javascript
// Basic question answering
response = aiModel( "openai" ).run( "What is BoxLang?" )
```

### Pattern 2: Templated Conversations

```javascript
// Reusable templates with variables
template = aiMessage()
    .system( "You are ${persona}" )
    .user( "${question}" )
    .to( aiModel( "openai" ) )

// Use with different inputs
template.run( { persona: "a scientist", question: "Explain gravity" } )
template.run( { persona: "a chef", question: "How to make pasta" } )
```

### Pattern 3: Agent with Tools

```javascript
// Autonomous agent with function calling
agent = aiAgent(
    tools: [ weatherTool, databaseTool, apiTool ],
    memory: aiMemory( "windowed" )
)

agent.run( "What's the weather and show me last 5 users" )
// Agent automatically calls appropriate tools
```

### Pattern 4: RAG (Retrieval Augmented Generation)

```javascript
// Semantic search + AI generation
memory = aiMemory( "chromavector" )

// Load knowledge base
aiDocuments( "./docs" ).toMemory( memory )

// Create RAG agent
agent = aiAgent(
    instructions: "Answer using provided context",
    memory: memory
)

response = agent.run( "How do I install BoxLang?" )
// Agent retrieves relevant docs, then answers
```

### Pattern 5: Multi-Step Processing

```javascript
// Generate → Review → Format pipeline
pipeline = aiMessage()
    .user( "Write code to ${task}" )
    .to( aiModel( "openai" ) )
    .to( aiMessage().user( "Review this code: ${code}" ) )
    .to( aiModel( "claude" ) )
    .to( aiTransform( r => r.content.trim() ) )

result = pipeline.run( { task: "sort an array" } )
```

---

## 🚀 Quick Start Examples

### Example 1: Your First Pipeline (3 lines)

```javascript
// Message → Model → Run
response = aiMessage()
    .user( "Tell me a joke" )
    .to( aiModel( "openai" ) )
    .run()
```

### Example 2: Chatbot with Memory (5 lines)

```javascript
agent = aiAgent(
    memory: aiMemory( "windowed", { maxMessages: 10 } )
)

agent.run( "My favorite color is blue" )
agent.run( "What's my favorite color?" ) // "Blue"
```

### Example 3: Function Calling (8 lines)

```javascript
calculatorTool = aiTool(
    name: "calculate",
    description: "Do math",
    action: ( a, b ) => a * b
)
.describeA( "First number" )
.describeB( "Second number")

agent = aiAgent( tools: [ calculatorTool ] )
response = agent.run( "What is 25 times 48?" )
// Agent calls calculator → "1200"
```

### Example 4: Data Extraction (6 lines)

```javascript
class Contact {
    property name="name" type="string";
    property name="email" type="string";
}

contact = aiChat( "Extract: John Doe, john@example.com" )
    .structuredOutput( new Contact() )
```

---

## 💡 Tips for Success

1. **Start simple** - Master models and messages before agents
2. **Test incrementally** - Build pipelines step by step
3. **Reuse components** - Create libraries of templates and agents
4. **Monitor costs** - Use appropriate models for each task
5. **Read the guides** - Each component page has detailed examples

**Ready to build?** Start with **[AI Models →](models.md)**
