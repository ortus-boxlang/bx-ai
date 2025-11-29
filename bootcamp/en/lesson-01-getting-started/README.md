# Lesson 1: Getting Started

**⏱️ Duration: 45 minutes**

Welcome to your first step into AI development with BoxLang! In this lesson, you'll set up your environment and make your first AI call.

## 🎯 What You'll Learn

- Install and configure the bx-ai module
- Understand API keys and providers
- Make your first AI call
- Understand tokens (how AI "sees" text)

---

## 📚 Part 1: Understanding AI Basics (10 mins)

Before writing code, let's understand what we're working with.

### What is a Large Language Model (LLM)?

An LLM is an AI system that:
- **Reads** and understands text
- **Generates** human-like responses
- **Helps** with coding, writing, analysis, and more

```
┌─────────────────────────────────────────────────────────────────┐
│                        HOW AI WORKS                             │
└─────────────────────────────────────────────────────────────────┘

  Your Question          AI Processing           AI Response
  ──────────────        ───────────────         ──────────────
       │                      │                       │
       ▼                      ▼                       ▼
  ┌─────────┐           ┌─────────┐            ┌─────────┐
  │"What is │  ──────▶  │ Neural  │  ──────▶   │"BoxLang │
  │BoxLang?"│           │ Network │            │is a JVM │
  │         │           │(billions│            │language │
  │         │           │of params│            │that..." │
  └─────────┘           └─────────┘            └─────────┘
```

### What is a Token?

AI doesn't see words like we do. It breaks text into **tokens** - small pieces of text.

```
┌─────────────────────────────────────────────────────────────────┐
│                      TOKEN EXAMPLES                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   "Hello"           =  1 token                                  │
│   "Hello world"     =  2 tokens                                 │
│   "BoxLang is cool" =  4 tokens                                 │
│                                                                 │
│   💡 Rule of thumb: ~4 characters = 1 token                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Why tokens matter:**
- ✅ You pay per token (input + output)
- ✅ Models have token limits (context window)
- ✅ More tokens = longer responses

### What is an AI Provider?

A provider is a company that runs AI models in the cloud (or locally):

| Provider | Models | Pricing | Best For |
|----------|--------|---------|----------|
| **OpenAI** | GPT-4, GPT-4o-mini | Paid | Most capable |
| **Claude** | Claude 3.5 Sonnet | Paid | Long context, reasoning |
| **Ollama** | Llama 3.2, Mistral | **Free** | Local/private |

---

## 🛠️ Part 2: Setup (15 mins)

### Step 1: Verify BoxLang

Open your terminal and run:

```bash
boxlang --version
```

You should see something like `BoxLang 1.x.x`. If not, [download BoxLang](https://boxlang.io).

### Step 2: Install bx-ai Module

```bash
install-bx-module bx-ai
```

Or for web apps (CommandBox):
```bash
box install bx-ai
```

### Step 3: Get an API Key

**Option A: OpenAI (Recommended for beginners)**

1. Go to https://platform.openai.com/api-keys
2. Sign up or log in
3. Click "Create new secret key"
4. Copy the key (starts with `sk-`)
5. Add credits to your account ($5 is plenty to start)

**Option B: Ollama (Free, runs locally)**

1. Download from https://ollama.ai
2. Install and run Ollama
3. Pull a model:
   ```bash
   ollama pull llama3.2
   ```
4. No API key needed!

### Step 4: Set Your API Key

Create a `.env` file in your project:

```bash
# For OpenAI
OPENAI_API_KEY=sk-your-key-here

# For Claude
CLAUDE_API_KEY=sk-ant-your-key-here
```

> ⚠️ **Never commit API keys to git!** Add `.env` to your `.gitignore`.

---

## 💻 Part 3: Your First AI Call (10 mins)

### The aiChat() Function

The simplest way to talk to AI:

```java
result = aiChat( "Your message here" )
```

That's it! Let's try it.

### Example 1: Hello AI

Create a file called `hello-ai.bxs`:

```java
// hello-ai.bxs
// Your first AI call!

answer = aiChat( "Say hello to someone learning BoxLang AI!" )
println( answer )
```

Run it:
```bash
boxlang hello-ai.bxs
```

**Expected output:**
```
Hello! Welcome to your BoxLang AI journey! I'm excited to help you 
learn how to build amazing AI-powered applications. Let's get started! 🚀
```

### Example 2: Ask a Question

```java
// ask-question.bxs
question = "What is BoxLang in one sentence?"
answer = aiChat( question )

println( "Q: " & question )
println( "A: " & answer )
```

### Example 3: Using Ollama (Free/Local)

If you installed Ollama:

```java
// local-ai.bxs
answer = aiChat(
    "What is 2 + 2?",
    { model: "llama3.2" },
    { provider: "ollama" }
)
println( answer )
```

### Example 4: Error Handling

Always wrap AI calls in try/catch:

```java
// safe-call.bxs
try {
    answer = aiChat( "Tell me a programming joke" )
    println( answer )
} catch( any e ) {
    println( "❌ Error: " & e.message )
    println( "💡 Check your API key!" )
}
```

---

## 🧪 Part 4: Lab - Magic 8-Ball (10 mins)

Let's build your first AI application: a Magic 8-Ball!

### The Goal

Create an AI-powered fortune teller that answers yes/no questions.

### Instructions

1. Create a file `magic-8-ball.bxs`
2. Ask the user for a question
3. Send it to the AI with special instructions
4. Display the mystical answer

### Starter Code

```java
// magic-8-ball.bxs

println( "🎱 Welcome to the AI Magic 8-Ball! 🎱" )
println( "Ask me a yes/no question..." )
println( "" )

// Get user's question
print( "Your question: " )
question = readLine()

// Create the magic prompt
prompt = "
You are a mystical Magic 8-Ball. 
Answer the following yes/no question with ONE of these classic responses:
- It is certain
- Without a doubt
- Yes definitely
- You may rely on it
- Most likely
- Outlook good
- Signs point to yes
- Reply hazy, try again
- Ask again later
- Cannot predict now
- Don't count on it
- My sources say no
- Outlook not so good
- Very doubtful

Question: #question#

Respond with ONLY the Magic 8-Ball phrase, nothing else.
"

// Get the mystical answer
try {
    answer = aiChat( prompt, { temperature: 0.9 } )
    println( "" )
    println( "🔮 The Magic 8-Ball says..." )
    println( "   " & answer )
} catch( any e ) {
    println( "❌ The spirits are unclear: " & e.message )
}
```

### Run It

```bash
boxlang magic-8-ball.bxs
```

### Sample Output

```
🎱 Welcome to the AI Magic 8-Ball! 🎱
Ask me a yes/no question...

Your question: Will I learn BoxLang AI today?

🔮 The Magic 8-Ball says...
   It is certain
```

### Challenge

Modify the Magic 8-Ball to:
1. Keep asking questions in a loop
2. Type "quit" to exit
3. Count how many questions were asked

---

## ✅ Knowledge Check

Test your understanding:

1. **What does an LLM do?**
   - [ ] Only writes code
   - [x] Understands and generates text
   - [ ] Stores databases
   - [ ] Runs servers

2. **What is a token?**
   - [ ] A password
   - [x] A piece of text the AI processes
   - [ ] A type of variable
   - [ ] An error message

3. **Which provider is free and runs locally?**
   - [ ] OpenAI
   - [ ] Claude
   - [x] Ollama
   - [ ] Gemini

4. **What function makes a simple AI call?**
   - [ ] ai()
   - [x] aiChat()
   - [ ] sendAI()
   - [ ] chatBot()

---

## 📝 Summary

You learned:

| Concept | What It Means |
|---------|---------------|
| **LLM** | AI that understands and generates text |
| **Token** | A piece of text (~4 characters) |
| **Provider** | Company running AI models |
| **aiChat()** | Function to send messages to AI |

### Key Code

```java
// Basic AI call
answer = aiChat( "Your message" )

// With options
answer = aiChat( 
    "Message",
    { temperature: 0.7 },         // Parameters
    { provider: "openai" }        // Options
)
```

---

## ⏭️ Next Lesson

You've made your first AI call! Now let's learn to have real conversations.

👉 **[Lesson 2: Conversations & Messages](../lesson-02-conversations/)**

---

## 📁 Lesson Files

```
lesson-01-getting-started/
├── README.md (this file)
├── examples/
│   ├── hello-ai.bxs
│   ├── ask-question.bxs
│   ├── local-ai.bxs
│   └── safe-call.bxs
└── labs/
    └── magic-8-ball.bxs
```
