# Lesson 1: Introduction to AI in BoxLang

**Duration**: 1.5 hours
**Level**: Beginner
**Prerequisites**: Basic BoxLang knowledge

## 🎯 Learning Objectives

By the end of this lesson, you will:
- ✅ Understand what AI and Large Language Models (LLMs) are
- ✅ Know the capabilities of the BoxLang AI module
- ✅ Install and configure the bx-ai module
- ✅ Make your first AI API call
- ✅ Understand tokens and AI costs

## 📚 Theory

### What is AI?

**Artificial Intelligence (AI)** is technology that enables computers to perform tasks that typically require human intelligence, such as:
- Understanding and generating human language
- Answering questions
- Writing code
- Analyzing data
- Making decisions

### What are Large Language Models (LLMs)?

**LLMs** are AI systems trained on vast amounts of text data to:
- **Understand** natural language input
- **Generate** human-like responses
- **Complete** tasks like translation, summarization, code generation
- **Reason** through complex problems

Popular LLMs include:
- **GPT-4** (OpenAI) - Most capable, expensive
- **Claude** (Anthropic) - Long context, analytical
- **Gemini** (Google) - Multimodal capabilities
- **Llama** (Meta) - Open source, runs locally

### How LLMs Work

```
User Input → Tokenization → Model Processing → Token Generation → Response
   "Hello"      [15496]      (AI magic)         [15496, 0]        "Hello!"
```

**Key Concepts:**

1. **Tokens**: Words or parts of words that the AI processes
   - Example: "Hello world" = 2 tokens
   - Cost is measured per token

2. **Context Window**: How much text the AI can "remember"
   - GPT-4: 128,000 tokens (~96,000 words)
   - Context = your input + AI's response

3. **Temperature**: Controls randomness (0.0 = consistent, 1.0 = creative)

4. **Prompts**: Instructions you give to the AI

### BoxLang AI Module

The **bx-ai** module provides a unified interface to work with multiple AI providers:

**Benefits:**
- 🔄 **One API**: Works with OpenAI, Claude, Gemini, Ollama, and more
- 🎯 **Simple**: Start with one function call
- 🔧 **Flexible**: Advanced features when you need them
- 💰 **Cost-Effective**: Use free local models with Ollama
- 🔒 **Secure**: Keep data local or use cloud services

**Core Features:**
- Simple chat interactions (`aiChat()`)
- Streaming responses
- Function calling (AI can use your tools)
- Memory systems (conversation context)
- AI Agents (autonomous assistants)
- Pipelines (multi-step workflows)

## 🛠️ Setup

### Step 1: Verify BoxLang Installation

```bash
# Check BoxLang is installed
boxlang --version

# Should output something like: BoxLang 1.0.0
```

If not installed, download from [boxlang.io](https://boxlang.io)

### Step 2: Install bx-ai Module

```bash
# Using box CLI
box install bx-ai

# Or add to box.json
{
  "dependencies": {
    "bx-ai": "^2.0.0"
  }
}
```

### Step 3: Get an API Key

**Option A: OpenAI (Recommended for beginners)**

1. Go to https://platform.openai.com/api-keys
2. Create account (requires phone verification)
3. Click "Create new secret key"
4. Copy the key (starts with `sk-`)
5. **Important**: Add $5+ credits to your account

**Option B: Ollama (Free, local)**

1. Download from https://ollama.ai
2. Install and run
3. Pull a model: `ollama pull llama3.2`
4. No API key needed!

### Step 4: Configure Environment

Create a `.env` file in your project:

```bash
OPENAI_API_KEY=sk-your-key-here
# Or if using Claude
CLAUDE_API_KEY=sk-ant-your-key-here
```

### Step 5: Configure BoxLang

Create or update `boxlang.json`:

```json
{
  "name": "my-ai-app",
  "modules": {
    "bxai": {
      "settings": {
        "provider": "openai",
        "apiKey": "${OPENAI_API_KEY}",
        "defaultParams": {
          "model": "gpt-3.5-turbo",
          "temperature": 0.7,
          "max_tokens": 1000
        }
      }
    }
  }
}
```

## 💻 Examples

### Example 1: Hello AI

```java
// hello-ai.bxs
answer = aiChat( "Say hello!" )
println( answer )
```

**Run it:**
```bash
boxlang hello-ai.bxs
```

**Output:**
```
Hello! How can I assist you today?
```

### Example 2: Ask a Question

```java
// ask-question.bxs
question = "What is BoxLang?"
answer = aiChat( question )
println( "Q: " & question )
println( "A: " & answer )
```

### Example 3: Using Ollama (Local)

```java
// ollama-hello.bxs
answer = aiChat(
    "Explain what a variable is in programming",
    { model: "llama3.2" },
    { provider: "ollama" }
)
println( answer )
```

### Example 4: With Error Handling

```java
// safe-chat.bxs
try {
    answer = aiChat( "Tell me a joke" )
    println( answer )
} catch( any e ) {
    println( "Error: " & e.message )
    println( "Make sure your API key is set!" )
}
```

## 🧪 Lab: Your First AI Application

### Lab Objective
Create a simple AI-powered fact checker that answers questions about BoxLang.

### Instructions

1. **Create `fact-checker.bxs`**:

```java
// fact-checker.bxs
/**
 * BoxLang Fact Checker
 * Answers questions about BoxLang using AI
 */

// The facts we want the AI to know
context = "
BoxLang is a modern dynamic JVM language.
It runs on Java 21 or higher.
BoxLang provides CFML compatibility.
It supports modules and package management.
BoxLang has a modern syntax and is fully interoperable with Java.
"

// Get question from user
println( "=== BoxLang Fact Checker ===" )
print( "Ask a question about BoxLang: " )
question = readLine()

// Ask AI using the context
answer = aiChat( [
    { role: "system", content: "Answer based only on this context: " & context },
    { role: "user", content: question }
], { temperature: 0.3 } )

println( "\nAnswer: " & answer )
```

2. **Run it:**
```bash
boxlang fact-checker.bxs
```

3. **Test with questions:**
   - "What Java version does BoxLang require?"
   - "Does BoxLang support CFML?"
   - "Can BoxLang work with Java?"

### Expected Behavior

```
=== BoxLang Fact Checker ===
Ask a question about BoxLang: What Java version does BoxLang require?

Answer: BoxLang requires Java 21 or higher.
```

### Lab Challenge

Extend the fact checker to:
1. Keep asking questions in a loop
2. Type "exit" to quit
3. Show how many questions were asked

**Hint**: Use a `while` loop and counter variable.

## 🧩 Understanding Tokens

### What Are Tokens?

Tokens are pieces of text that AI models process. Understanding tokens helps manage costs!

```java
// token-demo.bxs
import bxModules.bxai.models.util.TokenCounter;

text1 = "Hello"
text2 = "Hello world!"
text3 = "The quick brown fox jumps over the lazy dog"

println( "Text: '#text1#'" )
println( "Tokens: #TokenCounter::count( text1 )#" )
println()

println( "Text: '#text2#'" )
println( "Tokens: #TokenCounter::count( text2 )#" )
println()

println( "Text: '#text3#'" )
println( "Tokens: #TokenCounter::count( text3 )#" )
```

**Output:**
```
Text: 'Hello'
Tokens: 1

Text: 'Hello world!'
Tokens: 3

Text: 'The quick brown fox jumps over the lazy dog'
Tokens: 9
```

### Estimating Costs

```java
// cost-calculator.bxs
/**
 * Calculate estimated AI costs
 */

// Pricing (as of 2024)
GPT_4_INPUT_COST_PER_1K = 0.03  // $0.03 per 1K tokens
GPT_4_OUTPUT_COST_PER_1K = 0.06  // $0.06 per 1K tokens

prompt = "Write a 500-word essay about AI"
estimatedInputTokens = 10
estimatedOutputTokens = 650  // ~500 words

inputCost = ( estimatedInputTokens / 1000 ) * GPT_4_INPUT_COST_PER_1K
outputCost = ( estimatedOutputTokens / 1000 ) * GPT_4_OUTPUT_COST_PER_1K
totalCost = inputCost + outputCost

println( "Estimated cost for this request:" )
println( "Input: $#numberFormat( inputCost, '0.0000' )#" )
println( "Output: $#numberFormat( outputCost, '0.0000' )#" )
println( "Total: $#numberFormat( totalCost, '0.0000' )#" )
```

**Key Takeaways:**
- Input tokens (your prompt) are cheaper
- Output tokens (AI's response) cost more
- Limit `max_tokens` to control costs
- Use cheaper models (gpt-3.5-turbo) for simple tasks

## ✅ Knowledge Check

Test your understanding:

1. **What is a token?**
   - a) A secret API key
   - b) A piece of text the AI processes
   - c) A type of AI model
   - d) A programming language

2. **Which is true about temperature?**
   - a) Higher = more consistent responses
   - b) Lower = more creative responses
   - c) 0.0 = most random
   - d) Lower = more focused/deterministic

3. **What does the aiChat() function return by default?**
   - a) A number
   - b) The AI's response as a string
   - c) An array of tokens
   - d) A struct of metadata

4. **Which provider is free and runs locally?**
   - a) OpenAI
   - b) Claude
   - c) Ollama
   - d) Gemini

5. **What file stores API keys?**
   - a) api-keys.txt
   - b) .env
   - c) config.bxs
   - d) secrets.json

**Answers**: 1-b, 2-d, 3-b, 4-c, 5-b

## 📝 Summary

In this lesson, you learned:

✅ **AI Basics**: What LLMs are and how they work
✅ **BoxLang AI**: Benefits of the bx-ai module
✅ **Setup**: Installed and configured the module
✅ **First Call**: Made your first AI interaction
✅ **Tokens**: Understood costs and token counting

### Key Functions Learned
- `aiChat( message )` - Send a message, get a response

### Best Practices
1. Always handle errors with try/catch
2. Use environment variables for API keys
3. Start with cheaper models (gpt-3.5-turbo)
4. Monitor token usage to control costs
5. Test with Ollama for free development

## 🎯 Homework

Before the next lesson:

1. ✅ Complete the fact checker lab
2. ✅ Try all example code
3. ✅ Make 5 different AI chat calls
4. ✅ Calculate estimated costs for a 1000-word essay
5. ✅ Optional: Install Ollama and try local AI

## 📚 Additional Resources

- [OpenAI Pricing](https://openai.com/pricing)
- [Ollama Models](https://ollama.ai/library)
- [BoxLang AI Docs](../../docs/README.md)
- [Token Counter Reference](../../docs/advanced/utilities.md)

## ⏭️ Next Lesson

Ready for more? Head to [Lesson 2: Your First AI Chat](../lesson-02-first-chat/) to master basic chat interactions!

---

**Questions?** Open an issue on [GitHub](https://github.com/ortus-boxlang/bx-ai/issues)
