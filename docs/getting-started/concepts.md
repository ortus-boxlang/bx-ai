---
description: >-
  Essential concepts and terminology for understanding BoxLang AI - your guide
  to AI, embeddings, RAG, and more.
icon: book
---

# Key Concepts

Understanding these core concepts will help you make the most of BoxLang AI. This guide explains the terminology and ideas you'll encounter throughout the documentation.

## 📋 Table of Contents

* [AI & Machine Learning](concepts.md#ai--machine-learning)
* [Language Models](concepts.md#language-models)
* [Messages & Conversations](concepts.md#messages--conversations)
* [Embeddings & Vectors](concepts.md#embeddings--vectors)
* [Memory Systems](concepts.md#memory-systems)
* [RAG (Retrieval Augmented Generation)](concepts.md#rag-retrieval-augmented-generation)
* [Tools & Function Calling](concepts.md#tools--function-calling)
* [Streaming & Async](concepts.md#streaming--async)
* [Pipelines & Composition](concepts.md#pipelines--composition)
* [Providers & Services](concepts.md#providers--services)
* [Tokens & Costs](concepts.md#tokens--costs)

***

## 🤖 AI & Machine Learning

### Artificial Intelligence (AI)

Computer systems that can perform tasks that typically require human intelligence, such as understanding language, recognizing patterns, and making decisions.

**In BoxLang AI**: All the AI providers (OpenAI, Grok, Claude, etc.) use AI to understand and respond to your prompts.

### Large Language Model (LLM)

A type of AI trained on massive amounts of text data to understand and generate human-like text. Examples: GPT-4, Claude, Grok, Gemini.

**Key characteristics**:

* Trained on billions of text examples
* Can understand context and nuance
* Generate coherent, contextual responses
* Follow instructions and answer questions

### Training vs Inference

* **Training**: The process of teaching an AI model (done by provider companies, **not by you**)
* **Inference**: Using a trained model to generate responses (what you do with BoxLang AI)

***

## 💬 Language Models

### Temperature

Controls randomness in AI responses. Range: 0.0 to 2.0+. Please note also that some providers may not even offer temperature settings. Or some offer different ranges. Check your provider's documentation for details.

```javascript
// Temperature scale:
0.0   - Deterministic, focused, consistent
0.3   - Factual, precise (good for data extraction)
0.7   - Balanced, natural (default for most tasks)
1.0   - Creative, varied responses
1.5+  - Highly random, experimental
```

**Boxlang AI Example**:

```javascript
// Consistent answers
aiChat( "What is 2+2?", { temperature: 0.0 } )
// Always: "4"

// Creative writing
aiChat( "Write a story opening", { temperature: 1.2 } )
// Varied, creative results each time
```

### Top P (Nucleus Sampling)

Alternative to temperature. Limits token selection to top percentage of probability mass. Please note also that some providers may not offer topP settings or may have different ranges. Check your provider's documentation for details.

* `topP: 0.1` - Very focused (top 10% of likely words)
* `topP: 0.5` - Moderate variety
* `topP: 1.0` - Full vocabulary available (default)

**Pro tip**: Use either `temperature` OR `topP`, not both.

### Max Tokens

Maximum length of the AI's response, measured in tokens. It is important because it affects both cost and the amount of information the AI can provide.

```javascript
aiChat( "Explain AI", { max_tokens: 100 } )
// Short response (about 75 words)

aiChat( "Explain AI", { max_tokens: 1000 } )
// Detailed response (about 750 words)
```

**Note**: Token limits include BOTH input (your prompt) and output (AI response).

### Context Window

The maximum total tokens (input + output) a model can handle in one request.

| Model          | Context Window                   |
| -------------- | -------------------------------- |
| GPT-4 Turbo    | 128,000 tokens (\~96,000 words)  |
| Claude 3 Opus  | 200,000 tokens (\~150,000 words) |
| Gemini 1.5 Pro | 2,000,000 tokens (\~1.5M words)  |
| Llama 3.1 8B   | 128,000 tokens                   |

> Please verify with your provider for the exact limits of the model you are using.

**Why it matters**: Determines how much conversation history or document context you can include.

***

## 📨 Messages & Conversations

### Message Roles

Every message in a conversation has a role:

```javascript
{
    role: "system",      // Instructions for AI behavior
    role: "user",        // Your questions/prompts
    role: "assistant",   // AI's responses
    role: "tool"         // Tool execution results
}
```

**Example conversation**:

```javascript
[
    { role: "system", content: "You are a helpful math tutor" },
    { role: "user", content: "What is calculus?" },
    { role: "assistant", content: "Calculus is the study of change..." },
    { role: "user", content: "Can you give an example?" }
]
```

### System Messages

Special instructions that guide AI behavior throughout the conversation. It is important to set the right tone and constraints for the AI.

**Best practices**:

* **Only ONE** system message per conversation
* Place at the beginning
* Be specific and clear
* Define personality, constraints, and output format

```javascript
// Good system message
"You are a customer support agent for TechCorp. Be friendly, concise,
and always provide documentation links. If you don't know something,
say so and offer to escalate to a human agent."

// Bad system message
"You are an AI."  // Too vague
```

### Multi-Turn Conversations

Conversations with multiple back-and-forth exchanges. The AI remembers context from previous messages.

**Visual comparison**:

```
WITHOUT MEMORY (Stateless)          WITH MEMORY (Stateful)
────────────────────────            ──────────────────────

┌──────────────┐                    ┌──────────────┐
│ "I'm Alice"  │                    │ "I'm Alice"  │
└──────┬───────┘                    └──────┬───────┘
       │                                   │
       ▼                                   ▼
   ┌────────┐                          ┌────────┐
   │   AI   │                          │   AI   │
   └────┬───┘                          └────┬───┘
        │                                   │
        │                                   │ Stored
        ▼                                   ▼
   "Hello!"                             ┌─────────┐
                                        │ Memory  │
┌──────────────┐                        │ Alice   │
│ "My name?"   │                        └────┬────┘
└──────┬───────┘                             │
       │                            ┌────────┴────────┐
       ▼                            │  "My name?"     │
   ┌────────┐                       └────────┬────────┘
   │   AI   │ (no context)                   │
   └────┬───┘                                ▼
        │                                ┌────────┐
        ▼                                │   AI   │ (with context)
   "I don't                              └────┬───┘
    know"                                     │
                                              ▼
                                         "You're Alice"
```

**Without memory** (stateless):

```javascript
aiChat( "My name is Alice" )  // Response: "Nice to meet you, Alice"
aiChat( "What's my name?" )   // Response: "I don't know your name"
```

**With memory** (stateful):

```javascript
agent = aiAgent( memory: aiMemory( "window" ) )
agent.run( "My name is Alice" )  // Stored in memory
agent.run( "What's my name?" )   // "Your name is Alice"
```

### Multimodal Capabilities

Modern AI models can process and generate multiple types of content beyond just text, including images, audio, and video.

**Supported modalities**:

* 📝 **Text** - Natural language input and output (all models)
* 🖼️ **Images** - Image understanding and generation (GPT-4 Vision, Claude 3, Gemini)
* 🎵 **Audio** - Speech recognition and synthesis (Whisper, TTS models)
* 🎥 **Video** - Video analysis (some advanced models)

**Vision with aiMessage()** (fluent API):

```javascript
// Simple image URL
response = aiChat([
    aiMessage()
        .user( "What's in this image?" )
        .image( "https://example.com/photo.jpg" )
])
// AI: "This image shows a golden retriever playing in a park..."

// Multiple images
response = aiChat([
    aiMessage()
        .user( "Compare these two photos" )
        .image( "https://example.com/photo1.jpg" )
        .image( "https://example.com/photo2.jpg" )
])

// Local file (auto-converts to base64)
response = aiChat([
    aiMessage()
        .user( "Describe this receipt" )
        .imageFile( "/path/to/receipt.jpg" )
])

// Mix text and images in conversation
messages = [
    aiMessage().system( "You are an image analysis expert" ),
    aiMessage()
        .user( "What breed is this dog?" )
        .image( "https://example.com/dog.jpg" ),
    aiMessage().assistant( "That's a Golden Retriever" ),
    aiMessage()
        .user( "What about this one?" )
        .imageFile( "/path/to/another-dog.jpg" )
]
```

**Vision example** (raw format for advanced use):

```javascript
// If you need full control, use raw content format
response = aiChat(
    messages: [
        aiMessage().user(
            content: [
                { type: "text", text: "What's in this image?" },
                { type: "image_url", image_url: { url: "https://example.com/photo.jpg" } }
            ]
        )
    ],
    { provider: "openai", model: "gpt-4o" }
)
```

**Common use cases**:

* 📸 **Image analysis** - Describe photos, extract text from images (OCR)
* 🏷️ **Content moderation** - Detect inappropriate visual content
* 📋 **Document processing** - Extract data from receipts, forms, invoices
* 🔍 **Visual search** - Find similar images or products
* ♿ **Accessibility** - Generate alt text for images
* 🎨 **Image generation** - Create images from text descriptions

**Model support**:

| Model                | Text | Vision | Audio           |
| -------------------- | ---- | ------ | --------------- |
| GPT-4o               | ✅    | ✅      | ✅ (via Whisper) |
| GPT-4 Turbo          | ✅    | ✅      | ❌               |
| Claude 3 Opus/Sonnet | ✅    | ✅      | ❌               |
| Gemini 1.5 Pro       | ✅    | ✅      | ✅               |
| Llama 3.2 Vision     | ✅    | ✅      | ❌               |

**Note**: Check your provider's documentation for specific model capabilities and pricing for multimodal inputs.

***

## 🧬 Embeddings & Vectors

### Embeddings

Numerical representations of text as vectors (arrays of numbers) that capture semantic meaning. These are used for tasks like semantic search and similarity comparisons.

```javascript
// Text to vector
"cat"    → [0.2, -0.5, 0.8, 0.1, ...]  (1536 dimensions)
"kitten" → [0.3, -0.4, 0.7, 0.2, ...]  (similar vector)
"car"    → [-0.6, 0.3, -0.2, 0.9, ...] (different vector)
```

**Key properties**:

* Similar meanings = similar vectors
* Mathematical operations preserve semantic relationships
* Enables semantic search (find by meaning, not just keywords)

### Vector Dimensions

The number of values in an embedding vector. Different models produce different dimensions:

* OpenAI `text-embedding-3-small`: 1536 dimensions
* OpenAI `text-embedding-3-large`: 3072 dimensions
* Cohere `embed-english-v3.0`: 1024 dimensions
* Voyage `voyage-2`: 1024 dimensions

**Trade-off**: More dimensions = better accuracy but more storage/compute.

### Cosine Similarity

Measures how similar two vectors are (0 to 1). Cosine similarity is commonly used to compare embeddings.

* `1.0` - Identical meaning
* `0.8+` - Very similar
* `0.5` - Somewhat related
* `0.0` - Unrelated

**Used for**: Finding the most relevant documents in semantic search. The mathematical formula is:

```
cosine_similarity(A, B) = (A · B) / (||A|| ||B||)
```

Where `A · B` is the dot product of vectors A and B, and `||A||` and `||B||` are the magnitudes of vectors A and B.

**Visual representation**:

```
Vectors as arrows in space:

       ↑ Vector A [0.8, 0.6]
       │   ╱
       │  ╱
       │ ╱  angle θ (small = similar)
       │╱
───────┼────────→
       │╲
       │ ╲
       │  ╲ angle φ (large = different)
       │   ╲
       ↓    Vector C [-0.3, -0.5]
    Vector B [0.7, 0.5]


Similarity scores:

"cat"    → [0.8, 0.6, 0.3, ...]  ───┐
                                     ├─→ cos_sim = 0.98 (very similar!)
"kitten" → [0.7, 0.5, 0.4, ...]  ───┘

"cat"    → [0.8, 0.6, 0.3, ...]  ───┐
                                     ├─→ cos_sim = 0.12 (different)
"car"    → [-0.3, -0.5, 0.9, ...] ──┘


Example calculation (simplified 2D):

A = [0.8, 0.6]  (cat)
B = [0.7, 0.5]  (kitten)

1. Dot product: (0.8 × 0.7) + (0.6 × 0.5) = 0.56 + 0.30 = 0.86
2. Magnitude A:  √(0.8² + 0.6²) = √(0.64 + 0.36) = 1.0
3. Magnitude B:  √(0.7² + 0.5²) = √(0.49 + 0.25) = 0.86
4. Cosine similarity: 0.86 / (1.0 × 0.86) = 1.0

Result: Perfect similarity! (in this simplified example)
```

### Vector Database

Specialized database optimized for storing and searching vector embeddings.

**Popular options in BoxLang AI**:

* ChromaDB - Local/cloud, easy to start
* PostgreSQL (pgvector) - Enterprise-ready
* Pinecone - Managed cloud service
* Qdrant - High-performance
* BoxVector - Built-in, simple in memory option
* Weaviate - Scalable, cloud-native
* MySQL (with vector support) - Common relational DB

***

## 💭 Memory Systems

### Conversation Memory

Stores chat history to maintain context across interactions.

**Types**:

* **Window**: Keep last N messages (simple, memory-efficient)
* **Summary**: Auto-summarize old messages (long conversations)
* **Session**: Web session-based (per-user in web apps)
* **File**: Persist to disk (survives restarts)
* **Cache**: Distributed storage (multiple servers)
* **JDBC**: Database-backed (enterprise apps)

### Vector Memory

Stores documents as embeddings for semantic search. Enables RAG.

**Types**: ChromaDB, PostgreSQL, Pinecone, Qdrant, Weaviate, MySQL, TypeSense, BoxVector, Milvus

**Use cases**:

* Knowledge bases
* Document search
* Question answering with context
* Recommendation systems

### Hybrid Memory

BoxLang AI offers hybrid memory that combines conversation and vector memory. This allows agents to maintain chat context while also retrieving relevant documents.

**Visual architecture**:

```
User Question: "What's our refund policy for the Premium plan?"
     │
     ▼
┌─────────────────────────────────────────┐
│         HYBRID MEMORY                   │
│                                         │
│  ┌──────────────┐   ┌────────────────┐  │
│  │ Conversation │   │ Vector Memory  │  │
│  │   Memory     │   │   (Semantic)   │  │
│  │              │   │                │  │
│  │ • Last 10    │   │ Search: "refund│  │
│  │   messages   │   │ policy premium"│  │
│  │              │   │                │  │
│  │ User: "I'm   │   │ ┌────────────┐ │  │
│  │  on Premium" │   │ │ Policy Doc │ │  │
│  │              │   │ │ (0.95 sim) │ │  │
│  │ AI: "Great!" │   │ └────────────┘ │  │
│  │              │   │                │  │
│  │ User: "How   │   │ ┌────────────┐ │  │
│  │  about..."   │   │ │ FAQ Entry  │ │  │
│  └──────────────┘   │ │ (0.87 sim) │ │  │
│         │           │ └────────────┘ │  │
│         │           └────────┬───────┘  │
│         │                    │          │
│         └──────┬─────────────┘          │
└────────────────┼────────────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │   AI Model     │
        │                │
        │ Context:       │
        │ • Chat history │
        │ • Relevant docs│
        └────────┬───────┘
                 │
                 ▼
        "Based on our Premium plan policy
         [retrieved from docs], you can get
         a full refund within 30 days..."
```

```javascript
memory = aiMemory( "hybrid", {
    conversationMemory: aiMemory( "window", { maxMessages: 10 } ),
    vectorMemory: aiMemory( "chroma", { collection: "docs" } )
} )
```

**Best of both worlds**: Recent chat history + relevant document retrieval.

### Multi-Tenant Memory

BoxLang AI also supports multi-tenant memory for applications with multiple users by isolating memory per user or conversation.

```javascript
aiMemory( "window", {
    userId: "user123",          // Separate memory per user
    conversationId: "chat456"   // Separate per conversation
} )
```

**Why important**: Prevents users from seeing each other's data in shared applications.

***

## 🎯 RAG (Retrieval Augmented Generation)

### What is RAG?

**Retrieval Augmented Generation** - A technique that gives AI models access to external knowledge by retrieving relevant documents and including them in the prompt.

**The problem RAG solves**:

* AI models have a knowledge cutoff date
* Can't access your private/proprietary data
* May hallucinate facts

**The RAG solution**:

1. Store documents as embeddings in vector memory
2. When user asks a question, search for relevant docs
3. Include retrieved docs in the prompt as context
4. AI answers based on YOUR data, not just training data

### RAG Workflow

```
┌─────────────┐
│ Your Docs   │
└──────┬──────┘
       │ 1. Load & chunk
       ▼
┌─────────────┐
│ Embeddings  │
└──────┬──────┘
       │ 2. Store
       ▼
┌─────────────┐
│ Vector DB   │──────┐
└─────────────┘      │ 3. Search
                     │
User Question ───────┘
       │
       │ 4. Retrieve relevant docs
       ▼
┌─────────────┐
│ AI + Context│
└──────┬──────┘
       │ 5. Generate answer
       ▼
    Answer
```

### Chunking

Breaking large documents into smaller segments that fit in context windows. BoxLang AI offers several chunking strategies.

**Strategies**:

* **Recursive** (recommended): Split by paragraphs → sentences → words
* **Fixed size**: Equal-sized chunks
* **Semantic**: Split by meaning/topics

```javascript
chunks = aiChunk( longDocument, {
    chunkSize: 2000,    // Max characters per chunk
    overlap: 200        // Overlap between chunks (preserves context)
} )
```

**Why overlap matters**: Ensures context isn't lost at chunk boundaries.

***

## 🛠️ Tools & Function Calling

### AI Tools

Functions that AI can call to access real-time data or perform actions. This is how you extend AI capabilities beyond text generation.

**Example use cases**:

* Get current weather
* Search databases
* Execute calculations
* Call external APIs
* Retrieve user data

```javascript
weatherTool = aiTool(
    name: "get_weather",
    description: "Get current weather for a city",
    callback: ( city ) => {
        return getWeatherAPI( city )
    }
).describeCity( "The city to get weather for" )
```

### Function Calling

The process where AI decides to call a tool, executes it, and uses the result in its response.

**Visual flow**:

```
User Question
     │
     ▼
┌─────────────────┐
│   AI Model      │
│ (analyzes need) │
└────────┬────────┘
         │
         │ Decides to call tool
         ▼
┌─────────────────┐
│   Tool Call     │
│  get_weather()  │
└────────┬────────┘
         │
         │ Executes function
         ▼
┌─────────────────┐
│ Tool Response   │
│ {temp: 15°C}    │
└────────┬────────┘
         │
         │ Returns data
         ▼
┌─────────────────┐
│   AI Model      │
│ (with context)  │
└────────┬────────┘
         │
         ▼
   Final Answer
```

1. User: "What's the weather in Boston?"
2. AI thinks: "I need weather data, I'll call get\_weather tool"
3. Tool executes: `get_weather("Boston")` → `{temp: 15, condition: "cloudy"}`
4. AI responds: "The weather in Boston is 15°C and cloudy"

**Key point**: AI automatically decides when to use tools based on the conversation.

### Tool Schemas

JSON description of tool parameters that AI uses to call functions correctly.

```javascript
{
    name: "search_database",
    description: "Search products by name or category",
    parameters: {
        query: {
            type: "string",
            description: "Search term"
        },
        limit: {
            type: "number",
            description: "Max results",
            default: 10
        }
    }
}
```

**Tip**: Clear descriptions help AI use tools correctly.

***

## 📡 Streaming & Async Computations

### Streaming

Receiving AI responses in real-time as tokens are generated, rather than waiting for the complete response. BoxLang AI supports streaming for better user experience.

**Benefits**:

* Better UX (immediate feedback)
* Feels faster
* Can display partial results
* Process data as it arrives

```javascript
aiChatStream(
    "Write a long story",
    ( chunk ) => {
    	print( chunk )  // Display each word as it's generated
       	bx:flush;
    }
)
```

**Use when**: Building chat UIs, long responses, real-time applications.

### Server-Sent Events (SSE)

The underlying protocol used for streaming. Providers send data chunks over HTTP as they're generated. BoxLang offers native SSE support for compatible providers.

### Async (Asynchronous)

Non-blocking operations that return immediately with a "promise" (Future) of the result.

```javascript
// Blocking (waits for response)
response = aiChat( "Hello" )  // 2-3 seconds

// Non-blocking (continues immediately)
boxFuture = aiChatAsync( "Hello" )
// Do other work...
response = boxFuture.get()  // Wait when you need the result

// Non-blocking with pipelines
aiChatAsync( "process data" )
	.then( result => {
	    println( "Got result: #result#" )
	} )
	.catch( error => {
	    println( "Error: #error#" )
	} )

// then somewhere else in code
response = boxFuture.get()  // Blocks only if not ready
```

**Use when**: Making multiple AI calls in parallel, background processing, non-UI operations.

### Futures

A "promise" of a value that will be available later. Returned by async operations. You can read more about BoxLang Futures here: https://boxlang.ortusbooks.com/boxlang-framework/asynchronous-programming/box-futures

```javascript
boxFuture = aiChatAsync( "Explain AI" )

// Check if ready
if ( boxFuture.isDone() ) {
    result = boxFuture.get()
}

// Wait with timeout
result = boxFuture.get( 10, "seconds" )
// Cancel if needed
boxFuture.cancel()
```

***

## 🔗 Pipelines & Composition

### Pipelines

Composable workflows that chain AI operations together. Inspired by Unix pipes.

```javascript
pipeline = aiMessage()
    .user( "Analyze: ${data}" )
	// Pipe to model
    .to( aiModel( "openai" ) )
	// Pipe to transform
    .transform( response => response.toUpper() )
	// Pipe to final processing
    .transform( text => text.trim() )
```

**Benefits**:

* Reusable components
* Testable steps
* Clear data flow
* Easy to modify

### Runnables

Components that can be executed and chained in pipelines. Must implement `run()` and optionally `stream()` (Implements our `AiRunnable` interface)

**Runnable types**:

* `AiModel` - AI provider integration
* `AiMessage` - Message templates
* `AiTransform` - Data transformations
* `AiAgent` - Autonomous agents

### Chaining

Connecting runnables using `.to()` method.

```javascript
result = aiMessage()
    .user( "Translate to Spanish: ${text}" )
    .to( aiModel() )              // Chain to model
    .to( aiTransform( trim ) )    // Chain to transform
    .run( { text: "Hello World" } )
```

### Variable Binding

Using placeholders in templates that get replaced with actual values at runtime.

```javascript
template = aiMessage()
    .system( "You are a ${role}" )
    .user( "Explain ${topic}" )

// Bind variables when running
template.run( {
    role: "teacher",
    topic: "quantum physics"
} )
```

***

## 🌐 Providers & Services

### AI Provider

A company/service that offers AI models (OpenAI, Anthropic, Google, etc.).

**BoxLang AI supports**: OpenAI, Claude, Gemini, Groq, Grok, DeepSeek, Ollama, Perplexity, HuggingFace, Mistral, OpenRouter, Cohere, Voyage.

### Service Instance

A configured connection to a specific AI provider.

```javascript
// Get service instance
service = aiService( "openai" )

// Configure
service.configure({
    apiKey: getSystemSetting( "OPENAI_API_KEY" ),
    model: "gpt-4",
    temperature: 0.7
})

// Use service
response = service.invoke( request )
```

**When to use**: Need fine-grained control, multiple configurations, or service reuse.

### Model

A specific AI model within a provider (e.g., `gpt-4`, `claude-3-opus`, `gemini-pro`).

**Model selection matters**:

* **Speed**: Smaller models are faster
* **Cost**: Larger models cost more per token
* **Quality**: Larger models generally perform better
* **Features**: Some features only work with specific models

### Local vs Cloud

* **Cloud providers** (OpenAI, Claude): Hosted remotely, requires API key, charges per use
* **Local providers** (Ollama): Runs on your machine, free, private, offline-capable

**Ollama advantages**:

* ✅ No API costs
* ✅ Complete privacy
* ✅ Works offline
* ✅ No rate limits

**Cloud advantages**:

* ✅ More powerful models
* ✅ No hardware requirements
* ✅ Always up-to-date

***

## 💰 Tokens & Costs

### Token

The basic unit of text processing in language models. Roughly:

* 1 token ≈ 4 characters
* 1 token ≈ 0.75 words
* 100 tokens ≈ 75 words

**Example**:

```
"Hello, how are you?" = 5 tokens
"The quick brown fox jumps" = 5 tokens
```

### Token Count

The number of tokens in a text. Important for:

* **Cost estimation** (charged per token)
* **Context limits** (max tokens per request)
* **Response sizing** (limit output length)

```javascript
count = aiTokens( "Your text here" )
println( "Tokens: #count#" )

// Estimate cost
tokens = aiTokens( myPrompt )
cost = tokens * 0.00003  // $0.03 per 1K tokens for GPT-4
```

### Input vs Output Tokens

* **Input tokens**: Your prompt + conversation history
* **Output tokens**: AI's response

**Cost difference**: Output tokens often cost 2-3x more than input tokens!

### Rate Limits

Maximum number of requests allowed per time period by providers.

**Typical limits**:

* Free tier: 3-20 requests/minute
* Paid tier: 60-10,000 requests/minute
* Enterprise: Custom limits

**Handling rate limits**:

```javascript
try {
    response = aiChat( "Hello" )
} catch ( RateLimitException e ) {
    sleep( 60000 )  // Wait 1 minute
    retry()
}
```

***

## 🎯 Related Guides

* 📦 [Installation](installation/) - Get BoxLang AI set up
* ⚡ [Quick Start](quickstart.md) - Your first AI interaction
* 🧩 [Provider Setup](installation/provider-setup.md) - Configure AI providers
* 💬 [Basic Chatting](../main-components/chatting/basic-chatting.md) - Simple AI conversations
* 🤖 [AI Agents](../main-components/agents.md) - Autonomous AI assistants
* 🔮 [Vector Memory](../main-components/vector-memory.md) - Semantic search
* 📄 [RAG Guide](../rag/rag.md) - Retrieval Augmented Generation

***

## 💡 Quick Reference

**Most Important Concepts**:

1. **Temperature** - Controls randomness (0.0 = consistent, 1.0+ = creative)
2. **Tokens** - Basic unit of text (≈0.75 words, used for cost/limits)
3. **Embeddings** - Text as vectors for semantic search
4. **RAG** - Give AI access to your documents
5. **Tools** - Let AI call your functions
6. **Memory** - Maintain conversation context
7. **Streaming** - Real-time token-by-token responses
8. **Pipelines** - Chain AI operations together

**When to use what**:

* 🔥 **Quick answers**: `aiChat()`
* 💭 **Conversations**: `aiAgent()` with memory
* 📄 **Your data**: RAG with vector memory
* 🛠️ **Real-time data**: Tools/function calling
* 🎨 **Consistent format**: Structured output
* ⚡ **Better UX**: Streaming responses
