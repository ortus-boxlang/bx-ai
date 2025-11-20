# Quick Start Guide

Get up and running with BoxLang AI in minutes. This guide walks you through your first AI interactions.

## Prerequisites

- BoxLang installed and configured
- bx-ai module installed ([Installation Guide](installation.md))
- API key for your chosen provider OR Ollama installed locally

## Your First AI Chat

The simplest way to use AI is with the `aiChat()` function:

```java
// hello.bxs
answer = aiChat( "What is BoxLang?" )
println( answer )
```

Run it:
```bash
boxlang hello.bxs
```

**Output:**
```
BoxLang is a modern, dynamic programming language for the JVM that combines the best features of CFML with modern language design...
```

## Understanding the Basics

### The `aiChat()` Function

```java
aiChat( message, params, options )
```

- **message**: Your question or prompt (string or array of messages)
- **params**: Model parameters like temperature, max_tokens (optional)
- **options**: Provider, API key, return format (optional)

### Simple Examples

**Ask a question:**

```java
answer = aiChat( "Explain recursion" )
```

**Get creative:**

```java
poem = aiChat(
    "Write a haiku about coding",
    { temperature: 0.9 }
)
```

**Use a specific model:**

```java
answer = aiChat(
    "Explain quantum physics",
    { model: "gpt-4", temperature: 0.3 }
)
```

## Working with Different Providers

### Cloud Providers

**OpenAI:**

```java
answer = aiChat(
    "Hello!",
    {},
    { provider: "openai", apiKey: "sk-..." }
)
```

**Claude:**

```java
answer = aiChat(
    "Analyze this code",
    { model: "claude-3-opus-20240229" },
    { provider: "claude" }
)
```

**Gemini:**

```java
answer = aiChat(
    "What's new in AI?",
    {},
    { provider: "gemini" }
)
```

### Local AI with Ollama

**No API key needed, runs on your machine:**

```java
// First time: pull a model
// ollama pull llama3.2

answer = aiChat(
    "Explain variables",
    { model: "llama3.2" },
    { provider: "ollama" }
)
```

**Benefits of Ollama:**
- 🔒 **Privacy**: Data never leaves your machine
- 💰 **Cost**: Zero API charges
- 🚀 **Speed**: No network latency
- 🔌 **Offline**: Works without internet

## Building Conversations

### Multi-Turn Dialogue

```java
conversation = [
    { role: "system", content: "You are a helpful tutor" },
    { role: "user", content: "What is a variable?" },
    { role: "assistant", content: "A variable is a container for data..." },
    { role: "user", content: "Show me an example" }
]

answer = aiChat( conversation )
```

### Using Message Builder

```java
message = aiMessage()
    .system( "You are a code reviewer" )
    .user( "Review: function add(a,b) { return a+b }" )

answer = aiChat( message.getMessages() )
```

## Controlling AI Behavior

### Temperature (Creativity)

```java
// Focused/deterministic (0.0 - 0.3)
technical = aiChat(
    "Explain TCP/IP",
    { temperature: 0.2 }
)

// Balanced (0.5 - 0.7)
normal = aiChat(
    "Write a blog post",
    { temperature: 0.7 }
)

// Creative/random (0.8 - 1.0)
creative = aiChat(
    "Write a sci-fi story",
    { temperature: 0.95 }
)
```

### Response Length

```java
// Short response
summary = aiChat(
    "Summarize quantum physics",
    { max_tokens: 100 }
)

// Detailed response
detailed = aiChat(
    "Explain quantum physics in detail",
    { max_tokens: 2000 }
)
```

## Practical Examples

### Code Assistant

```java
// code-helper.bxs
code = aiChat(
    "Write a BoxLang function to reverse a string",
    {
        model: "gpt-4",
        temperature: 0.3
    }
)

println( "Generated Code:" )
println( code )
```

### Content Generator

```java
// blog-writer.bxs
topic = "Benefits of local AI"

article = aiChat(
    "Write a 3-paragraph blog post about: " & topic,
    {
        temperature: 0.7,
        max_tokens: 500
    }
)

println( article )
```

### Translator

```java
// translator.bxs
function translate( text, to = "Spanish" ) {
    return aiChat(
        "Translate to #to#: #text#",
        { temperature: 0.3 }
    )
}

spanish = translate( "Hello, how are you?" )
french = translate( "Thank you", "French" )

println( spanish )
println( french )
```

### Smart Q&A

```java
// qa.bxs
context = "
BoxLang is a modern dynamic JVM language.
It runs on Java 21+ and provides CFML compatibility.
BoxLang supports modules, package management, and modern syntax.
"

question = "What Java version does BoxLang require?"

answer = aiChat( [
    { role: "system", content: "Answer based only on the context provided" },
    { role: "user", content: "Context: " & context },
    { role: "user", content: "Question: " & question }
], { temperature: 0.2 } )

println( answer )
// "BoxLang requires Java 21 or higher"
```

## Async Operations

For non-blocking AI calls:

```java
// Start request
future = aiChatAsync( "Explain machine learning" )

// Do other work
println( "Processing..." )
doOtherWork()

// Get result when ready
answer = future.get()
println( answer )
```

## Streaming Responses

Get responses in real-time:

```java
print( "AI: " )

aiChatStream(
    "Tell me a short story",
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        print( content )
    }
)

println( "\nDone!" )
```

## Return Formats

### Single (Default)

Returns just the content as a string:

```java
answer = aiChat( "Hello" )
// "Hello! How can I help you?"
```

### All Messages

Returns complete conversation array:

```java
messages = aiChat(
    "Hello",
    {},
    { returnFormat: "all" }
)
// [{ role: "assistant", content: "Hello!..." }]
```

### Raw Response

Returns complete API response:

```java
raw = aiChat(
    "Hello",
    {},
    { returnFormat: "raw" }
)
// { id: "chatcmpl-...", choices: [...], usage: {...} }
```

## Error Handling

```java
try {
    answer = aiChat( "Hello" )
    println( answer )
} catch( any e ) {
    println( "Error: " & e.message )

    // Fallback or retry logic
    if( e.message contains "timeout" ) {
        // Retry with longer timeout
    }
}
```

## Configuration Best Practices

### Use Environment Variables

```bash
# .env
OPENAI_API_KEY=sk-...
CLAUDE_API_KEY=sk-ant-...
```

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

### Set Sensible Defaults

```json
{
  "modules": {
    "bxai": {
      "settings": {
        "provider": "openai",
        "defaultParams": {
          "model": "gpt-4",
          "temperature": 0.7,
          "max_tokens": 1000
        },
        "timeout": 60
      }
    }
  }
}
```

## Introduction to Pipelines

Pipelines let you chain AI operations together for more complex workflows. Here are some quick examples:

### Simple Pipeline

```java
// Create a reusable pipeline
pipeline = aiMessage()
    .system( "You are a helpful coding assistant" )
    .user( "Explain ${topic}" )
    .toDefaultModel()
    .transform( r => r.content )

// Run it with different topics
explanation = pipeline.run({ topic: "recursion" })
println( explanation )

result = pipeline.run({ topic: "closures" })
println( result )
```

### FAQ Bot Pipeline

```java
// Build a reusable FAQ pipeline
faqBot = aiMessage()
    .system( "You are a helpful FAQ assistant. Answer briefly and clearly." )
    .user( "${question}" )
    .toDefaultModel()
    .transform( r => r.content )

// Use it multiple times
answer1 = faqBot.run({ question: "What are your business hours?" })
answer2 = faqBot.run({ question: "Do you offer refunds?" })
answer3 = faqBot.run({ question: "How do I reset my password?" })
```

### Multi-Step Pipeline

```java
// Create a pipeline with multiple transformations
analyzer = aiMessage()
    .system( "You are a code analyzer" )
    .user( "Analyze this code: ${code}" )
    .toDefaultModel()
    .transform( r => r.content )
    .transform( analysis => {
        return {
            timestamp: now(),
            analysis: analysis,
            codeLength: len( code )
        }
    })

// Run analysis
report = analyzer.run({
    code: "function hello() { return 'world'; }"
})

println( report.analysis )
```

### Extract and Parse Pipeline

```java
// Pipeline that extracts JSON data
dataExtractor = aiMessage()
    .system( "Extract data as JSON" )
    .user( "From this text, extract name and email: ${text}" )
    .toDefaultModel()
    .transform( r => r.content )
    .transform( json => deserializeJSON( json ) )

// Extract structured data
userData = dataExtractor.run({
    text: "Contact John Doe at john@example.com"
})

println( userData.name )   // "John Doe"
println( userData.email )  // "john@example.com"
```

### Pipeline with Multiple Models

```java
// Use different models in a workflow
translator = aiMessage()
    .user( "Translate to Spanish: ${text}" )
    .to( aiModel( "openai", { model: "gpt-4" } ) )
    .transform( r => r.content )

reviewer = aiMessage()
    .user( "Review this translation: ${translation}" )
    .to( aiModel( "claude", { model: "claude-3-opus" } ) )
    .transform( r => r.content )

// Translate then review
text = "Hello, how are you?"
translation = translator.run({ text: text })
review = reviewer.run({ translation: translation })
```

### Why Use Pipelines?

**Reusability**: Create once, run many times with different inputs

```java
// Define once
greeter = aiMessage()
    .system( "You are a friendly greeter" )
    .user( "Greet ${name} in ${style} style" )
    .toDefaultModel()
    .transform( r => r.content )

// Use many times
greeter.run({ name: "Alice", style: "formal" })
greeter.run({ name: "Bob", style: "casual" })
greeter.run({ name: "Charlie", style: "funny" })
```

**Composability**: Chain operations together

```java
// Each step does one thing well
pipeline = aiMessage()
    .user( "${prompt}" )
    .toDefaultModel()              // Get AI response
    .transform( r => r.content )   // Extract text
    .transform( text => uCase( text ) )  // Transform
    .transform( text => len( text ) )    // Analyze
```

**Separation of Concerns**: Template, model, and transformation logic separated

```java
// Template (what to ask)
template = aiMessage()
    .system( "You are an expert" )
    .user( "${question}" )

// Model (how to ask)
model = aiModel( "openai", { temperature: 0.7 } )

// Pipeline (complete flow)
pipeline = template
    .to( model )
    .transform( r => r.content )
```

Learn more about pipelines in the [Pipeline Overview](../pipelines/overview.md) section.

## Next Steps

Now that you're comfortable with the basics, explore:

### Quick Starts
- **[AI Agents Quick Start](agent-quickstart.md)** - Build autonomous agents with memory and tools

### Simple Interactions
- **[Basic Chatting](../simple-interactions/basic-chatting.md)** - Master the fundamentals
- **[Advanced Chatting](../simple-interactions/advanced-chatting.md)** - Tools, async, streaming
- **[Service-Level Control](../simple-interactions/service-chatting.md)** - Direct service management

### AI Pipelines
- **[Pipeline Overview](../pipelines/overview.md)** - Learn about composable workflows
- **[Working with Models](../pipelines/models.md)** - Pipeline-compatible AI models
- **[Message Templates](../pipelines/messages.md)** - Reusable prompts

### Advanced Topics
- **[Event System](../advanced/events.md)** - Intercept and customize AI operations

### Examples
Check the `/examples` folder in the repository for more complete applications.

## Common Issues

**"No API key provided"**
- Set API key in `boxlang.json` or pass directly in options

**"Connection timeout"**
- Increase timeout in settings or pass longer timeout in options

**"Model not found"**
- Check provider documentation for available model names
- For Ollama: make sure you've pulled the model with `ollama pull <model>`

**Ollama not responding**
- Start Ollama: `ollama serve`
- Check status: `curl http://localhost:11434/api/tags`
