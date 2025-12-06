# Lesson 3: Switching Providers

**⏱️ Duration: 45 minutes**

BoxLang AI lets you work with multiple AI providers through a unified API. This means you can switch between OpenAI, Claude, Ollama, and more without changing your code!

## 🎯 What You'll Learn

- Understand different AI providers and their strengths
- Switch providers at runtime
- Use local AI with Ollama (free!)
- Implement provider fallbacks

---

## 📚 Part 1: Understanding Providers (10 mins)

### What is a Provider?

A provider is a company (or software) that runs AI models:

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI PROVIDER LANDSCAPE                        │
└─────────────────────────────────────────────────────────────────┘

  ☁️ CLOUD PROVIDERS (Paid)              🏠 LOCAL (Free)
  ───────────────────────                 ───────────────
  
  ┌──────────┐  ┌──────────┐             ┌──────────┐
  │  OpenAI  │  │  Claude  │             │  Ollama  │
  │  (GPT-4) │  │(Anthropic│             │ (Local)  │
  └──────────┘  └──────────┘             └──────────┘
       │             │                         │
       │             │                         │
       ▼             ▼                         ▼
  ┌──────────┐  ┌──────────┐             ┌──────────┐
  │  Gemini  │  │   Grok   │             │ Your PC! │
  │ (Google) │  │   (xAI)  │             │ Private  │
  └──────────┘  └──────────┘             └──────────┘
```

### Provider Comparison

#### Chat Providers

| Provider | Strengths | Best For | Cost |
|----------|-----------|----------|------|
| **OpenAI** | Most capable, best tools | Production apps | $$ |
| **Claude** | Long context, reasoning | Complex analysis | $$ |
| **Gemini** | Multimodal (images/video) | Visual tasks | $ |
| **Mistral** | Fast, efficient, multilingual | European data residency | $ |
| **HuggingFace** | Open-source models | Community-driven, customizable | $ |
| **Groq** | Ultra-fast inference | Speed-critical apps | $ |
| **Ollama** | Private, no internet | Development, privacy | Free |
| **Grok** | Real-time data | Current events | $$ |
| **DeepSeek** | Code generation | Programming tasks | $ |
| **OpenRouter** | Access multiple models | Model comparison | $ |
| **Perplexity** | Research with citations | Fact-checking | $$ |

#### Embedding Providers

| Provider | Models | Dimensions | Best For |
|----------|--------|------------|----------|
| **OpenAI** | text-embedding-3-small/large | 1536/3072 | General embeddings |
| **Voyage** | voyage-2, voyage-code-2 | 1024/1536 | RAG, semantic search |
| **Cohere** | embed-english-v3.0 | 1024 | Multilingual |
| **Ollama** | nomic-embed-text, mxbai-embed | 768 | Local/private |
| **Gemini** | text-embedding-004 | 768 | Google ecosystem |

### Why Switch Providers?

1. **Cost** - Use cheaper models for simple tasks
2. **Privacy** - Run locally with Ollama
3. **Capabilities** - Different models excel at different things
4. **Fallback** - If one fails, try another
5. **Speed** - Some are faster than others

---

## 💻 Part 2: Using Different Providers (15 mins)

### Method 1: Options Parameter

Pass the provider in the options struct:

```java
// openai-call.bxs
answer = aiChat( 
    "What is 2 + 2?",
    {},                        // params
    { provider: "openai" }     // options
)
println( "OpenAI: " & answer )
```

### Method 2: With API Key

Some providers need specific API keys:

```java
// claude-call.bxs
answer = aiChat( 
    "What is 2 + 2?",
    {},                        
    { 
        provider: "claude",
        apiKey: getSystemSetting( "CLAUDE_API_KEY" )
    }
)
println( "Claude: " & answer )
```

### Method 3: Using aiService()

Create a reusable service:

```java
// service-example.bxs
openaiService = aiService( "openai" )
claudeService = aiService( "claude" )

// Same question, different providers
question = aiMessage().user( "What is BoxLang?" )

openaiAnswer = openaiService.invoke( 
    aiChatRequest( question )
)
claudeAnswer = claudeService.invoke( 
    aiChatRequest( question )
)
```

### Provider Quick Reference

**Chat Providers:**

```java
// OpenAI
aiChat( "message", {}, { provider: "openai" } )

// Claude
aiChat( "message", {}, { provider: "claude" } )

// Gemini
aiChat( "message", {}, { provider: "gemini" } )

// Mistral
aiChat( "message", {}, { provider: "mistral" } )

// HuggingFace
aiChat( "message", {}, { provider: "huggingface" } )

// Groq (fast!)
aiChat( "message", {}, { provider: "groq" } )

// Ollama (local)
aiChat( "message", { model: "llama3.2" }, { provider: "ollama" } )

// OpenRouter
aiChat( "message", {}, { provider: "openrouter" } )

// Grok
aiChat( "message", {}, { provider: "grok" } )

// DeepSeek
aiChat( "message", {}, { provider: "deepseek" } )

// Perplexity
aiChat( "message", {}, { provider: "perplexity" } )
```

**Embedding Providers:**

```java
// OpenAI embeddings
embedding = aiEmbedding(
    input: "BoxLang is a modern JVM language",
    options: { provider: "openai" }
)

// Voyage embeddings (best for RAG)
embedding = aiEmbedding(
    input: "Document text for semantic search",
    options: { provider: "voyage", model: "voyage-2" }
)

// Cohere embeddings (multilingual)
embedding = aiEmbedding(
    input: "Text in any language",
    options: { provider: "cohere" }
)

// Ollama embeddings (local/private)
embedding = aiEmbedding(
    input: "Private document",
    options: { provider: "ollama", model: "nomic-embed-text" }
)
```

---

## 🏠 Part 3: Using Ollama (Local AI) (10 mins)

Ollama lets you run AI completely locally - **free, private, no internet required!**

### Setup Ollama

1. **Download**: https://ollama.ai
2. **Install**: Run the installer
3. **Pull a model**:
   ```bash
   ollama pull llama3.2       # General purpose
   ollama pull codellama      # Coding focused
   ollama pull mistral        # Fast and capable
   ```

### Using Ollama in BoxLang

```java
// ollama-example.bxs
answer = aiChat( 
    "Write a haiku about programming",
    { model: "llama3.2" },
    { provider: "ollama" }
)
println( answer )
```

### Why Use Ollama?

```
┌─────────────────────────────────────────────────────────────────┐
│                    OLLAMA BENEFITS                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  🆓  FREE        - No API costs                                 │
│  🔒  PRIVATE     - Data never leaves your machine              │
│  🌐  OFFLINE     - Works without internet                      │
│  ⚡  FAST        - No network latency                          │
│  🛠️   DEV-READY  - Perfect for development/testing             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Popular Ollama Models

| Model | Size | Best For |
|-------|------|----------|
| `llama3.2` | 2GB | General purpose |
| `llama3.2:1b` | 1GB | Fast, simple tasks |
| `codellama` | 4GB | Code generation |
| `mistral` | 4GB | High quality responses |
| `phi3` | 2GB | Microsoft's efficient model |

---

## 🔄 Part 4: Provider Fallbacks (10 mins)

### Pattern: Try Multiple Providers

If one provider fails, try another:

```java
// fallback-example.bxs
function chatWithFallback( message ) {
    // Try providers in order
    providers = [ "openai", "claude", "ollama" ]
    
    for( provider in providers ) {
        try {
            println( "Trying #provider#..." )
            
            options = { provider: provider }
            if( provider == "ollama" ) {
                params = { model: "llama3.2" }
            } else {
                params = {}
            }
            
            return aiChat( message, params, options )
        } catch( any e ) {
            println( "  ❌ #provider# failed: #e.message#" )
            continue
        }
    }
    
    throw( message = "All providers failed!" )
}

// Use it
try {
    answer = chatWithFallback( "What is 2 + 2?" )
    println( "✅ Answer: " & answer )
} catch( any e ) {
    println( "❌ " & e.message )
}
```

### Pattern: Choose by Task

```java
// task-router.bxs
function routeByTask( task, message ) {
    switch( task ) {
        case "code":
            // Use OpenAI for coding (best tools)
            return aiChat( message, { model: "gpt-4o" }, { provider: "openai" } )
        
        case "analysis":
            // Use Claude for long analysis
            return aiChat( message, {}, { provider: "claude" } )
        
        case "quick":
            // Use local Ollama for quick tasks
            return aiChat( message, { model: "llama3.2" }, { provider: "ollama" } )
        
        default:
            return aiChat( message )
    }
}

// Use it
codeAnswer = routeByTask( "code", "Write a function to sort an array" )
quickAnswer = routeByTask( "quick", "What is 5 + 5?" )
```

---

## 🧪 Lab: Multi-Provider App

### The Goal

Create an app that:
1. Lets the user choose a provider
2. Compares responses from different providers
3. Handles errors gracefully

### Solution

```java
// multi-provider.bxs
println( "🔀 Multi-Provider AI Demo" )
println()
println( "Available providers:" )
println( "1. OpenAI" )
println( "2. Claude" )
println( "3. Ollama (local)" )
println( "4. Compare all!" )
println()

print( "Choose option (1-4): " )
choice = readLine()

print( "Enter your question: " )
question = readLine()

println()
println( "─".repeat( 50 ) )
println()

function callProvider( name, options, params = {} ) {
    try {
        startTime = getTickCount()
        answer = aiChat( question, params, options )
        duration = getTickCount() - startTime
        
        println( "✅ #name# (#duration#ms):" )
        println( answer )
        println()
        return true
    } catch( any e ) {
        println( "❌ #name# failed: #e.message#" )
        println()
        return false
    }
}

switch( choice ) {
    case "1":
        callProvider( "OpenAI", { provider: "openai" } )
        break
    
    case "2":
        callProvider( "Claude", { provider: "claude" } )
        break
    
    case "3":
        callProvider( "Ollama", { provider: "ollama" }, { model: "llama3.2" } )
        break
    
    case "4":
        println( "🔄 Comparing all providers..." )
        println()
        
        callProvider( "OpenAI", { provider: "openai" } )
        callProvider( "Claude", { provider: "claude" } )
        callProvider( "Ollama", { provider: "ollama" }, { model: "llama3.2" } )
        
        println( "✨ Comparison complete!" )
        break
    
    default:
        println( "Invalid choice!" )
}
```

### Run It

```bash
boxlang multi-provider.bxs
```

### Sample Output

```
🔀 Multi-Provider AI Demo

Available providers:
1. OpenAI
2. Claude
3. Ollama (local)
4. Compare all!

Choose option (1-4): 4
Enter your question: What is BoxLang?

──────────────────────────────────────────────────

🔄 Comparing all providers...

✅ OpenAI (842ms):
BoxLang is a modern dynamic JVM language that combines the best 
features of Java with dynamic language productivity.

✅ Claude (1203ms):
BoxLang is a contemporary programming language that runs on the 
Java Virtual Machine, offering both dynamic and static typing...

✅ Ollama (234ms):
BoxLang is a JVM-based language for building modern applications...

✨ Comparison complete!
```

---

## ✅ Knowledge Check

1. **Which provider runs locally and is free?**
   - [ ] OpenAI
   - [ ] Claude
   - [x] Ollama
   - [ ] Gemini

2. **How do you specify a provider?**
   - [x] Pass it in the options parameter
   - [ ] Use a special function
   - [ ] Change a global variable
   - [ ] Edit a config file

3. **Why might you switch providers?**
   - [x] Cost, privacy, different capabilities
   - [ ] It's required for each call
   - [ ] BoxLang forces it
   - [ ] For security only

4. **What's a fallback pattern?**
   - [ ] A way to format responses
   - [x] Try another provider if one fails
   - [ ] A debugging technique
   - [ ] A message format

---

## 📝 Summary

You learned:

| Concept | Description |
|---------|-------------|
| **Provider** | Company/software running AI models |
| **Options** | Pass `{ provider: "name" }` to switch |
| **Ollama** | Free, local, private AI |
| **Fallback** | Try multiple providers |
| **aiService()** | Create reusable provider connections |

### Key Code Patterns

```java
// Switch provider
aiChat( "msg", {}, { provider: "claude" } )

// Use Ollama
aiChat( "msg", { model: "llama3.2" }, { provider: "ollama" } )

// Create service
service = aiService( "openai" )
service.invoke( aiChatRequest( messages ) )
```

---

## ⏭️ Next Lesson

Now you can switch providers! Let's learn how to get structured data from AI.

👉 **[Lesson 4: Structured Output](../lesson-04-structured-output/)**

---

## 📁 Lesson Files

```
lesson-03-providers/
├── README.md (this file)
├── examples/
│   ├── openai-call.bxs
│   ├── claude-call.bxs
│   ├── ollama-example.bxs
│   ├── service-example.bxs
│   └── fallback-example.bxs
└── labs/
    └── multi-provider.bxs
```
