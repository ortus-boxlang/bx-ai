# Lesson 3: AI Providers

**Duration:** 60 minutes  
**Prerequisites:** Lessons 1-2 completed

## Learning Objectives

By the end of this lesson, you will:

- Understand different AI providers (OpenAI, Claude, Gemini, Ollama)
- Configure and use each provider
- Compare provider capabilities and costs
- Switch between providers seamlessly
- Use local models with Ollama

---

## Part 1: Understanding AI Providers

### What is an AI Provider?

An **AI provider** is a service that hosts and serves AI models. BoxLang AI supports:

| Provider | Company | Models | Cost | Local |
|----------|---------|--------|------|-------|
| **OpenAI** | OpenAI | GPT-3.5, GPT-4, GPT-4-turbo | $$ | No |
| **Claude** | Anthropic | Claude 3 (Opus, Sonnet, Haiku) | $$ | No |
| **Gemini** | Google | Gemini Pro, Gemini Ultra | $ | No |
| **Ollama** | Ollama | Llama, Mistral, Qwen, etc. | FREE | Yes |

### Why Multiple Providers?

- **Cost optimization**: Different pricing for different tasks
- **Capability matching**: Some models excel at specific tasks
- **Redundancy**: Fallback options if one provider is down
- **Local development**: Use Ollama for free testing

---

## Part 2: OpenAI Provider

### Setup

```bash
# Set API key
export OPENAI_API_KEY="sk-..."

# Or in .env file
OPENAI_API_KEY=sk-...
```

### Basic Usage

```java
// Default provider is OpenAI
answer = aiChat( "Hello!" )

// Explicit provider
answer = aiChat(
    "Hello!",
    {},
    { provider: "openai" }
)

// Specify model
answer = aiChat(
    "Hello!",
    { model: "gpt-4-turbo" },
    { provider: "openai" }
)
```

### OpenAI Models

```java
models = {
    "gpt-3.5-turbo": {
        cost: "Low",
        speed: "Fast",
        context: "16K tokens",
        bestFor: "Simple tasks, high volume"
    },
    "gpt-4-turbo": {
        cost: "Medium",
        speed: "Fast",
        context: "128K tokens",
        bestFor: "Complex reasoning, long context"
    },
    "gpt-4": {
        cost: "High",
        speed: "Slower",
        context: "8K tokens",
        bestFor: "Highest quality responses"
    }
}
```

### Advanced OpenAI Configuration

```java
// Using aiService for full control
service = aiService( "openai" )
    .configure( apiKey: getenv( "OPENAI_API_KEY" ) )

answer = service.invoke(
    aiChatRequest()
        .setMessages( [ aiMessage().user( "Hello!" ) ] )
        .setModel( "gpt-4-turbo" )
        .setTemperature( 0.7 )
        .setMaxTokens( 500 )
)
```

---

## Part 3: Claude Provider

### Setup

```bash
# Set API key
export CLAUDE_API_KEY="sk-ant-..."

# Or in .env file
CLAUDE_API_KEY=sk-ant-...
```

### Basic Usage

```java
// Use Claude
answer = aiChat(
    "Explain quantum computing",
    { model: "claude-3-sonnet-20240229" },
    { provider: "claude" }
)
```

### Claude Models

```java
models = {
    "claude-3-haiku-20240307": {
        cost: "Lowest",
        speed: "Fastest",
        context: "200K tokens",
        bestFor: "Speed, high volume, simple tasks"
    },
    "claude-3-sonnet-20240229": {
        cost: "Medium",
        speed: "Fast",
        context: "200K tokens",
        bestFor: "Balanced performance and cost"
    },
    "claude-3-opus-20240229": {
        cost: "Highest",
        speed: "Slower",
        context: "200K tokens",
        bestFor: "Complex analysis, creative tasks"
    }
}
```

### Claude Strengths

- **Long context**: 200K tokens (huge documents)
- **Analysis**: Excellent at detailed analysis
- **Safety**: Strong content moderation
- **Code**: Great for code review and generation

---

## Part 4: Gemini Provider

### Setup

```bash
# Set API key
export GEMINI_API_KEY="AIza..."

# Or in .env file
GEMINI_API_KEY=AIza...
```

### Basic Usage

```java
// Use Gemini
answer = aiChat(
    "What's new in AI?",
    { model: "gemini-pro" },
    { provider: "gemini" }
)
```

### Gemini Models

```java
models = {
    "gemini-pro": {
        cost: "Low",
        speed: "Fast",
        context: "32K tokens",
        bestFor: "General tasks, cost-effective"
    },
    "gemini-ultra": {
        cost: "Medium",
        speed: "Medium",
        context: "32K tokens",
        bestFor: "Complex reasoning, high quality"
    }
}
```

### Gemini Strengths

- **Multimodal**: Can process images (future support)
- **Cost**: Competitive pricing
- **Speed**: Fast responses
- **Google integration**: Works well with Google services

---

## Part 5: Ollama Provider (Local)

### Setup

```bash
# Install Ollama
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Windows
# Download from https://ollama.ai

# Start Ollama
ollama serve

# Pull a model
ollama pull llama3.2
ollama pull qwen2.5:0.5b-instruct
ollama pull mistral
```

### Basic Usage

```java
// Use Ollama (no API key needed!)
answer = aiChat(
    "Hello!",
    { model: "llama3.2" },
    { provider: "ollama" }
)
```

### Popular Ollama Models

```java
models = {
    "llama3.2": {
        size: "3B",
        speed: "Fast",
        bestFor: "General purpose, chat"
    },
    "qwen2.5:0.5b-instruct": {
        size: "0.5B",
        speed: "Very fast",
        bestFor: "Testing, simple tasks"
    },
    "mistral": {
        size: "7B",
        speed: "Medium",
        bestFor: "Code, technical content"
    },
    "codellama": {
        size: "7B-13B",
        speed: "Medium",
        bestFor: "Code generation and review"
    }
}
```

### Ollama Configuration

```java
// Custom Ollama endpoint
answer = aiChat(
    "Hello!",
    { model: "llama3.2" },
    { 
        provider: "ollama",
        baseURL: "http://localhost:11434"
    }
)
```

### Ollama Advantages

✅ **FREE** - No API costs  
✅ **Private** - Data never leaves your machine  
✅ **Offline** - Works without internet  
✅ **Fast** - No network latency  
✅ **Development** - Perfect for testing

---

## Part 6: Provider Comparison

### Use Case Guide

```java
// Simple Q&A - Use cheap, fast models
aiChat( "What is 2+2?", { model: "gpt-3.5-turbo" } )
// OR
aiChat( "What is 2+2?", { model: "llama3.2" }, { provider: "ollama" } )

// Complex reasoning - Use powerful models
aiChat( 
    "Analyze the philosophical implications...",
    { model: "gpt-4-turbo" }
)
// OR
aiChat(
    "Analyze the philosophical implications...",
    { model: "claude-3-opus-20240229" },
    { provider: "claude" }
)

// Long documents - Use high context models
aiChat(
    "Summarize: " & longDocument,
    { model: "claude-3-sonnet-20240229" },
    { provider: "claude" }
)

// Development/testing - Use free local
aiChat( "Test prompt", { model: "llama3.2" }, { provider: "ollama" } )
```

### Cost Comparison (as of 2024)

| Task | OpenAI | Claude | Gemini | Ollama |
|------|--------|--------|--------|--------|
| 1M tokens (input) | $0.50 - $30 | $3 - $15 | $0.25 - $1.25 | FREE |
| 1M tokens (output) | $1.50 - $60 | $15 - $75 | $0.50 - $5 | FREE |
| 1000 requests | $0.50 - $100 | $10 - $200 | $0.25 - $20 | FREE |

---

## Part 7: Provider Switching Patterns

### Pattern 1: Fallback Chain

```java
function aiChatWithFallback( prompt ) {
    providers = [ "openai", "claude", "gemini", "ollama" ]
    
    for ( provider in providers ) {
        try {
            return aiChat( prompt, {}, { provider: provider } )
        } catch( any e ) {
            println( "Provider #provider# failed, trying next..." )
        }
    }
    
    throw( "All providers failed" )
}
```

### Pattern 2: Cost-Based Selection

```java
function aiChatSmart( prompt, complexity = "simple" ) {
    configs = {
        "simple": { 
            model: "gpt-3.5-turbo", 
            provider: "openai" 
        },
        "medium": { 
            model: "gemini-pro", 
            provider: "gemini" 
        },
        "complex": { 
            model: "claude-3-opus-20240229", 
            provider: "claude" 
        }
    }
    
    config = configs[ complexity ]
    return aiChat( prompt, { model: config.model }, { provider: config.provider } )
}
```

### Pattern 3: A/B Testing

```java
function compareProviders( prompt ) {
    results = {}
    
    providers = [
        { name: "OpenAI", provider: "openai", model: "gpt-4-turbo" },
        { name: "Claude", provider: "claude", model: "claude-3-sonnet-20240229" },
        { name: "Ollama", provider: "ollama", model: "llama3.2" }
    ]
    
    providers.each( config => {
        startTime = getTickCount()
        
        try {
            answer = aiChat(
                prompt,
                { model: config.model },
                { provider: config.provider }
            )
            
            results[ config.name ] = {
                success: true,
                answer: answer,
                time: getTickCount() - startTime
            }
        } catch( any e ) {
            results[ config.name ] = {
                success: false,
                error: e.message
            }
        }
    } )
    
    return results
}
```

---

## Part 8: Best Practices

### 1. Environment-Based Configuration

```java
// Development: Use Ollama
if ( getenv( "ENVIRONMENT" ) == "development" ) {
    defaultProvider = "ollama"
    defaultModel = "llama3.2"
}
// Production: Use OpenAI
else {
    defaultProvider = "openai"
    defaultModel = "gpt-3.5-turbo"
}
```

### 2. API Key Management

```bash
# .env file (NEVER commit to git)
OPENAI_API_KEY=sk-...
CLAUDE_API_KEY=sk-ant-...
GEMINI_API_KEY=AIza...
```

### 3. Provider Health Checks

```java
function checkProvider( provider ) {
    try {
        aiChat( "test", { model: "test" }, { provider: provider } )
        return true
    } catch( any e ) {
        return false
    }
}
```

---

## Examples to Run

### 1. `provider-comparison.bxs`
Compare responses from all providers

### 2. `ollama-setup.bxs`
Setup and test Ollama models

### 3. `cost-analysis.bxs`
Calculate costs across providers

### 4. `fallback-chain.bxs`
Implement automatic fallback

### 5. `provider-benchmark.bxs`
Speed and quality comparison

---

## Lab Exercise: Multi-Provider Chat Bot

**File:** `labs/multi-provider-bot.bxs`

**Objective:**  
Create a chat bot that can switch between providers based on user commands or task complexity.

**Requirements:**
1. Support at least 3 providers (OpenAI, Claude, Ollama)
2. Allow user to switch providers with commands like "/use openai"
3. Auto-detect complexity and suggest appropriate provider
4. Show cost comparison before making expensive calls
5. Implement fallback if provider fails

**Bonus Challenges:**
- A/B test same prompt across all providers
- Benchmark response times
- Calculate total cost across conversation
- Save provider usage statistics

---

## Knowledge Check

1. **Which provider is completely free?**
   - Answer: Ollama (runs locally)

2. **What's Claude's biggest advantage?**
   - Answer: 200K token context window (long documents)

3. **When should you use gpt-3.5-turbo?**
   - Answer: Simple tasks, high volume, cost-conscious applications

4. **How do you set a provider in aiChat()?**
   - Answer: Third parameter: `{ provider: "openai" }`

5. **What's the best provider for development?**
   - Answer: Ollama (free, fast, private)

---

## Homework

### Assignment 1: Provider Cost Calculator
Create a tool that estimates costs for a given prompt across all providers.

### Assignment 2: Ollama Model Zoo
Test and compare 5 different Ollama models on the same task.

### Assignment 3: Smart Router
Build a function that automatically routes requests to the best provider based on prompt analysis.

---

## Key Takeaways

✅ Multiple providers offer different strengths  
✅ OpenAI: Industry standard, reliable  
✅ Claude: Long context, excellent analysis  
✅ Gemini: Cost-effective, fast  
✅ Ollama: Free, local, private  
✅ Use provider fallbacks for reliability  
✅ Match provider to task complexity

---

## Next Lesson

**Lesson 4: Model Parameters** - Learn to control AI behavior with temperature, max_tokens, and other parameters.

---

## Additional Resources

- [OpenAI Models](https://platform.openai.com/docs/models)
- [Claude Models](https://www.anthropic.com/claude)
- [Gemini Documentation](https://ai.google.dev/docs)
- [Ollama Library](https://ollama.ai/library)
