# Lesson 2: Conversations & Messages

**⏱️ Duration: 60 minutes**

In the last lesson, you made single AI calls. Now let's build real conversations where the AI remembers what you said!

## 🎯 What You'll Learn

- Understand message roles (system, user, assistant)
- Build multi-turn conversations
- Use the `aiMessage()` function for fluent message building
- Control AI behavior with system prompts

---

## 📚 Part 1: Understanding Messages (15 mins)

### The Three Roles

Every AI conversation uses three types of messages:

```
┌─────────────────────────────────────────────────────────────────┐
│                      MESSAGE ROLES                              │
└─────────────────────────────────────────────────────────────────┘

  ┌─────────────┐
  │   SYSTEM    │  Sets the AI's personality and rules
  │   (hidden)  │  "You are a helpful coding assistant..."
  └─────────────┘
         │
         ▼
  ┌─────────────┐
  │    USER     │  Your messages (questions, requests)
  │   (you)     │  "How do I write a loop?"
  └─────────────┘
         │
         ▼
  ┌─────────────┐
  │  ASSISTANT  │  AI's responses
  │   (AI)      │  "Here's how to write a loop..."
  └─────────────┘
```

### Message Structure

Messages are just structs with `role` and `content`:

```java
// A single message
message = { role: "user", content: "Hello!" }

// An array of messages (a conversation)
messages = [
    { role: "system", content: "You are a helpful assistant." },
    { role: "user", content: "Hi there!" },
    { role: "assistant", content: "Hello! How can I help?" },
    { role: "user", content: "What's the weather like?" }
]
```

### Why Conversations Matter

Without conversation history, AI can't remember anything:

```
WITHOUT HISTORY                 WITH HISTORY
───────────────                ─────────────
You: My name is Alex           You: My name is Alex
AI: Nice to meet you!          AI: Nice to meet you, Alex!

You: What's my name?           You: What's my name?
AI: I don't know...            AI: Your name is Alex!
     (no memory!)                   (remembers!)
```

---

## 💻 Part 2: Building Conversations (20 mins)

### Method 1: Array of Messages

The most explicit way to build conversations:

```java
// conversation-array.bxs
messages = [
    { role: "system", content: "You are a friendly math tutor. Be encouraging!" },
    { role: "user", content: "What is 5 + 3?" }
]

answer = aiChat( messages )
println( answer )
// Output: "Great question! 5 + 3 equals 8. You're doing great!"
```

### Method 2: The aiMessage() Function

A cleaner, fluent way to build messages:

```java
// fluent-messages.bxs
messages = aiMessage()
    .system( "You are a friendly math tutor. Be encouraging!" )
    .user( "What is 5 + 3?" )

answer = aiChat( messages )
println( answer )
```

### Method 3: Dynamic Conversations

Build up a conversation as you go:

```java
// dynamic-conversation.bxs
conversation = aiMessage()
    .system( "You are a helpful assistant. Keep responses brief." )

// First exchange
conversation.user( "Hi, my name is Jordan" )
response1 = aiChat( conversation )
println( "AI: " & response1 )
conversation.assistant( response1 )

// Second exchange
conversation.user( "What's my name?" )
response2 = aiChat( conversation )
println( "AI: " & response2 )
// Output: "Your name is Jordan!"
```

---

## 🎨 Part 3: System Prompts (15 mins)

The system message shapes how the AI behaves.

### Example: Different Personalities

```java
// personalities.bxs

// Pirate personality
pirateChat = aiMessage()
    .system( "You are a friendly pirate. Speak like a pirate in all responses. Use 'arr' and 'matey' often." )
    .user( "How do I make coffee?" )

println( "🏴‍☠️ Pirate says:" )
println( aiChat( pirateChat ) )
println()

// Professor personality
professorChat = aiMessage()
    .system( "You are a distinguished professor. Explain things academically with proper terminology." )
    .user( "How do I make coffee?" )

println( "🎓 Professor says:" )
println( aiChat( professorChat ) )
```

### System Prompt Best Practices

```
┌─────────────────────────────────────────────────────────────────┐
│                  SYSTEM PROMPT TEMPLATE                         │
└─────────────────────────────────────────────────────────────────┘

You are a [ROLE].

[PERSONALITY TRAITS]

[RULES/CONSTRAINTS]

[OUTPUT FORMAT]
```

**Example:**

```java
systemPrompt = "
You are a senior BoxLang developer.

You are patient, helpful, and explain concepts clearly.

Rules:
- Always include code examples
- Keep explanations under 100 words
- If you don't know something, say so

Format: Use markdown for code blocks.
"
```

---

## 🔄 Part 4: Conversation Patterns (10 mins)

### Pattern: Chat Loop

```java
// chat-loop.bxs
conversation = aiMessage()
    .system( "You are a helpful assistant. Be concise." )

println( "=== Chat with AI ===" )
println( "Type 'quit' to exit" )
println()

running = true
while( running ) {
        userInput = cliRead( "You: " )

    if( userInput == "quit" ) {
        running = false
        println( "Goodbye!" )
    } else {
        conversation.user( userInput )
        response = aiChat( conversation )
        println( "AI: " & response )
        conversation.assistant( response )
        println()
    }
}
```

### Pattern: Simple Context Injection

Add information the AI should know:

```java
// context-injection.bxs
context = "
Today's date: #now().format( 'yyyy-MM-dd' )#
User: Premium member
Available products: BoxLang Pro, BoxLang Enterprise, BoxLang Cloud
"

conversation = aiMessage()
    .system( "You are a sales assistant. Use this context: " & context )
    .user( "What products do you have?" )

answer = aiChat( conversation )
println( answer )
```

---

## 🔐 Part 5: Message Context System (20 mins)

BoxLang AI provides a powerful **message context system** for injecting structured data like security info, RAG documents, user preferences, and more!

### What is Message Context?

Context is **structured data** that gets automatically injected into your messages using the special `${context}` placeholder:

```
┌─────────────────────────────────────────────────────────────────┐
│                  MESSAGE CONTEXT SYSTEM                         │
└─────────────────────────────────────────────────────────────────┘

  Context Data               ${context}              AI Message
  ────────────              ──────────              ───────────

  { userId: \"123\"    ─────▶  Placeholder   ────▶  \"User 123,
    role: \"admin\"              in message          role: admin
    prefs: {...} }                                  prefs: ...\"
```

### Why Use Context Instead of String Concatenation?

```java
// ❌ Old way: Manual string building (messy)
userInfo = \"User: #user.name#, Role: #user.role#\"
message = aiMessage()
    .system( \"Help this user: \" & userInfo )

// ✅ New way: Structured context (clean)
message = aiMessage()
    .system( \"You are a helpful assistant. User info: ${context}\" )
    .setContext({
        userId: user.id,
        name: user.name,
        role: user.role,
        permissions: user.permissions
    })
```

### Context Methods

```java
// Set entire context at once
message.setContext({ key: \"value\", data: [1,2,3] })

// Add individual values
message.addContext( \"userId\", \"user-123\" )
message.addContext( \"tenantId\", \"tenant-456\" )

// Merge new data with existing context
message.mergeContext({ newKey: \"newValue\" })

// Check if context exists
if ( message.hasContext() ) {
    fullContext = message.getContext()
    userId = message.getContextValue( \"userId\", \"anonymous\" )
}
```

### Understanding render() vs format()

**Two ways to apply context:**

```java
// render() - Uses stored bindings and context only
message = aiMessage()
    .system( \"User: ${context}\" )
    .setContext({ name: \"Alex\" })

rendered = message.render()  // Context already applied
aiChat( rendered )           // Send pre-rendered messages

// format() - Requires runtime bindings
message = aiMessage()
    .system( \"User: ${name}\" )

formatted = message.format({ name: \"Alex\" })  // Apply bindings
aiChat( formatted )

// Combined: bindings + context
message = aiMessage()
    .system( \"Hello ${name}! Context: ${context}\" )
    .bind({ name: \"Alex\" })
    .setContext({ role: \"admin\" })

rendered = message.render()  // Both applied!
```

### Use Case 1: Security Context

Perfect for multi-tenant applications:

```java
// security-context.bxs
function createSecureMessage( userSession ) {
    return aiMessage()
        .system( \"
            You are a secure assistant.
            User context: ${context}
            Only show data this user can access.
        \" )
        .setContext({
            userId: userSession.userId,
            tenantId: userSession.tenantId,
            role: userSession.role,
            permissions: userSession.permissions,
            subscriptionTier: userSession.subscription.tier
        })
}

// Use it
userSession = {
    userId: \"user-123\",
    tenantId: \"company-abc\",
    role: \"manager\",
    permissions: [\"read\", \"write\", \"approve\"],
    subscription: { tier: \"premium\" }
}

message = createSecureMessage( userSession )
    .user( \"What can I do?\" )

response = aiChat( message.render() )
println( response )
// AI sees: User 123 from company-abc, manager role with read/write/approve permissions
```

### Use Case 2: RAG (Retrieval Augmented Generation)

Inject retrieved documents into context:

```java
// rag-context.bxs
function searchDocuments( query ) {
    // Simulate vector search
    return [
        {
            content: \"BoxLang is a modern dynamic JVM language.\",
            source: \"docs/intro.md\",
            score: 0.95
        },
        {
            content: \"BoxLang supports both dynamic and static typing.\",
            source: \"docs/types.md\",
            score: 0.89
        },
        {
            content: \"Install with: box install boxlang\",
            source: \"docs/install.md\",
            score: 0.82
        }
    ]
}

// User asks a question
userQuestion = \"How do I install BoxLang?\"

// Retrieve relevant documents
docs = searchDocuments( userQuestion )

// Create message with RAG context
message = aiMessage()
    .system( \"
        Answer using ONLY the provided context.
        If the answer isn't in the context, say you don't know.

        Context: ${context}
    \" )
    .user( userQuestion )
    .setContext({
        query: userQuestion,
        documents: docs.map( d => d.content ),
        sources: docs.map( d => d.source ),
        relevanceScores: docs.map( d => d.score )
    })

response = aiChat( message.render() )
println( response )
// Output: \"To install BoxLang, use: box install boxlang\"
```

### Use Case 3: User Preferences

Remember user preferences across sessions:

```java
// preferences-context.bxs
userPreferences = {
    userId: \"user-789\",
    language: \"en\",
    tone: \"professional\",
    verbosity: \"concise\",
    topics: [\"technology\", \"business\"],
    timezone: \"America/New_York\",
    dateFormat: \"MM/DD/YYYY\"
}

message = aiMessage()
    .system( \"
        Tailor responses to user preferences: ${context}
        - Use their preferred tone and verbosity
        - Format dates/times for their timezone
        - Focus on their topics of interest
    \" )
    .setContext( userPreferences )
    .user( \"Tell me about today's tech news\" )

response = aiChat( message.render() )
println( response )
// AI responds in professional, concise tone about tech/business
```

### Use Case 4: Dynamic Application State

```java
// app-state-context.bxs
appState = {
    currentPage: \"dashboard\",
    userStats: {
        loginCount: 42,
        lastLogin: \"2025-12-05\",
        tasksCompleted: 15
    },
    features: {
        betaAccess: true,
        darkMode: true,
        notifications: false
    }
}

message = aiMessage()
    .system( \"You are a helpful app assistant. App state: ${context}\" )
    .setContext( appState )
    .user( \"What have I been up to?\" )

response = aiChat( message.render() )
println( response )
// AI knows: 42 logins, last login yesterday, 15 tasks done, beta access enabled
```

### Streaming with Context

Context works with streaming too:

```java
// streaming-context.bxs
message = aiMessage()
    .system( \"User info: ${context}. Be helpful and personalized.\" )
    .setContext({
        name: \"Alex\",
        preferences: { tone: \"friendly\" }
    })
    .user( \"Tell me a joke\" )

aiChatStream(
    onChunk: ( chunk ) => print( chunk.content ),
    message: message.render()
)
```

### Best Practices

```
┌─────────────────────────────────────────────────────────────────┐
│                    CONTEXT BEST PRACTICES                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ✅ DO:                           ❌ DON'T:                      │
│  ─────                            ────────                      │
│  • Use IDs, not sensitive data    • Send passwords             │
│  • Keep context lightweight       • Send PII unnecessarily     │
│  • Structure data clearly         • Include huge datasets      │
│  • Use for RAG documents          • Mix concerns              │
│  • Document context schema        • Forget to sanitize        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Context vs Bindings: When to Use What

| Use Context When | Use Bindings When |\n|------------------|--------------------|
| Security/user info | Template variables |
| RAG documents | Simple placeholders |
| App state | Reusable prompts |
| Multi-tenant data | Dynamic values |
| Structured data | String replacement |

**Example combining both:**

```java
// Bindings for template, context for data
message = aiMessage()
    .system( \"You are a ${role} assistant\" )         // Binding
    .user( \"${action} using context: ${context}\" )   // Both!
    .bind({ role: \"helpful\", action: \"Assist user\" })
    .setContext({
        userId: \"123\",
        permissions: [\"read\", \"write\"]
    })

response = aiChat( message.render() )
```

---

---

## 📡 Part 6: Streaming Responses (15 mins)

For real-time responses (like ChatGPT), use **streaming**:

### Basic Streaming

```java
// streaming-basic.bxs
println( \"AI: \" )

aiChatStream(
    onChunk: ( chunk ) => print( chunk.content ),
    message: \"Write a haiku about programming\"
)

println()  // New line after stream completes
```

**Output appears word-by-word:**
```
AI: Code flows like stream
Bugs hide in silent shadows
Coffee makes it work
```

### Streaming with Conversations

```java
// streaming-conversation.bxs
conversation = aiMessage()
    .system( \"You are a storyteller. Be dramatic!\" )
    .user( \"Tell me a short story about a robot\" )

print( \"AI: \" )
fullResponse = \"\"

aiChatStream(
    onChunk: ( chunk ) => {
        print( chunk.content )
        fullResponse &= chunk.content
    },
    message: conversation
)

println()

// Add AI response to conversation for context
conversation.assistant( fullResponse )
```

### Async Responses

For background processing, use `aiChatAsync()`:

```java
// async-example.bxs
println( \"Starting AI request in background...\" )

// Returns a Future immediately
future = aiChatAsync( \"Write a paragraph about AI\" )

println( \"Doing other work while AI thinks...\" )
sleep( 1000 )
println( \"Still working...\" )

// Get result when ready (blocks if not complete)
result = future.get()
println( \"AI finished: \" & result )
```

### Streaming vs Async vs Regular

```
┌─────────────────────────────────────────────────────────────────┐
│                  RESPONSE TYPE COMPARISON                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  📄 aiChat()           ⚡ aiChatStream()      🔄 aiChatAsync()  │
│  ────────────         ────────────────       ───────────────   │
│  • Waits for all      • Real-time chunks     • Non-blocking    │
│  • Simple to use      • Progressive display  • Returns Future  │
│  • Best for scripts   • Best for UIs         • Parallel tasks  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Choose based on your use case:**

```java
// Script/CLI: Regular
answer = aiChat( \"Quick question\" )

// Web UI/Chat: Streaming
aiChatStream(
    onChunk: ( chunk ) => writeOutput( chunk.content ),
    message: \"User question\"
)

// Background: Async
future = aiChatAsync( \"Long analysis\" )
// Do other work...
result = future.get()
```

---

## 🧪 Lab: Build a Chat Assistant

### The Goal

Create an interactive chat assistant that:
1. Has a custom personality
2. Remembers the conversation
3. Can be customized by the user

### Instructions

1. Create `chat-assistant.bxs`
2. Let user choose a personality (Helpful, Funny, Serious)
3. Start a chat loop
4. The AI should remember previous messages

### Solution

```java
// chat-assistant.bxs
println( "🤖 Welcome to AI Chat Assistant!" )
println()
println( "Choose a personality:" )
println( "1. Helpful - Friendly and supportive" )
println( "2. Funny - Witty with jokes" )
println( "3. Serious - Professional and formal" )
println()

choice = cliRead( "Enter 1, 2, or 3: " )

// Set personality based on choice
switch( choice ) {
    case "1":
        personality = "You are a helpful, friendly assistant. Be warm and supportive."
        println( "✅ Helpful mode activated!" )
        break
    case "2":
        personality = "You are a funny assistant who loves jokes and puns. Make people laugh!"
        println( "😄 Funny mode activated!" )
        break
    case "3":
        personality = "You are a serious, professional assistant. Be formal and precise."
        println( "📋 Serious mode activated!" )
        break
    default:
        personality = "You are a helpful assistant."
        println( "✅ Default mode activated!" )
}

println()
println( "Chat started! Type 'quit' to exit." )
println( "─".repeat( 40 ) )

// Initialize conversation with personality
conversation = aiMessage()
    .system( personality )

// Chat loop
messageCount = 0
running = true

while( running ) {
        userInput = cliRead( "You: " )

    if( userInput.trim() == "quit" ) {
        running = false
        println()
        println( "📊 Stats: #messageCount# messages exchanged" )
        println( "👋 Goodbye!" )
    } else {
        conversation.user( userInput )

        try {
            response = aiChat( conversation )
            println( "AI: " & response )
            conversation.assistant( response )
            messageCount++
        } catch( any e ) {
            println( "❌ Error: " & e.message )
        }

        println()
    }
}
```

### Run It

```bash
boxlang chat-assistant.bxs
```

### Sample Output

```
🤖 Welcome to AI Chat Assistant!

Choose a personality:
1. Helpful - Friendly and supportive
2. Funny - Witty with jokes
3. Serious - Professional and formal

Enter 1, 2, or 3: 2
😄 Funny mode activated!

Chat started! Type 'quit' to exit.
────────────────────────────────────────
You: Tell me about BoxLang
AI: BoxLang? Oh, it's like Java went to a party, had too much fun,
    and came back as the cool kid on the JVM block! It's dynamic,
    it's modern, and it doesn't judge you for your bracket placement! 😄

You: quit

📊 Stats: 1 messages exchanged
👋 Goodbye!
```

---

## ✅ Knowledge Check

1. **What are the three message roles?**
   - [x] system, user, assistant
   - [ ] admin, user, bot
   - [ ] input, process, output
   - [ ] start, middle, end

2. **What does the system message do?**
   - [ ] Stores user data
   - [x] Sets AI personality and rules
   - [ ] Sends error messages
   - [ ] Manages the database

3. **How do you add history to a conversation?**
   - [x] Include previous messages in the array
   - [ ] Use a special history() function
   - [ ] AI automatically remembers
   - [ ] You can't add history

4. **What's the fluent way to build messages?**
   - [ ] buildMessage()
   - [x] aiMessage()
   - [ ] createChat()
   - [ ] messageBuilder()

---

## 📝 Summary

You learned:

| Concept | Description |
|---------|-------------|
| **system** | Sets AI personality and rules |
| **user** | Your messages to the AI |
| **assistant** | AI's responses |
| **aiMessage()** | Fluent message builder |
| **Conversation** | Array of messages with context |
| **Message Context** | Structured data injection with `${context}` |
| **setContext()** | Add security, RAG, user data |
| **render()** | Apply stored bindings and context |
| **aiChatStream()** | Real-time streaming responses |
| **aiChatAsync()** | Non-blocking async responses |

### Key Code Patterns

```java
// Array method
messages = [
    { role: "system", content: "Be helpful" },
    { role: "user", content: "Hello" }
]

// Fluent method
messages = aiMessage()
    .system( "Be helpful" )
    .user( "Hello" )

// Building conversation over time
conversation.user( "Question" )
response = aiChat( conversation )
conversation.assistant( response )

// Message context (RAG, security, preferences)
message = aiMessage()
    .system( "User context: ${context}" )
    .setContext({
        userId: "123",
        role: "admin",
        documents: retrievedDocs
    })
response = aiChat( message.render() )

// Streaming
aiChatStream(
    onChunk: ( chunk ) => print( chunk.content ),
    message: "Tell me a story"
)

// Async
future = aiChatAsync( "Long analysis task" )
result = future.get()
```

---

## ⏭️ Next Lesson

Now you can build conversations! Let's learn how to switch between different AI providers.

👉 **[Lesson 3: Switching Providers](../lesson-03-providers/)**

---

## 📁 Lesson Files

```
lesson-02-conversations/
├── README.md (this file)
├── examples/
│   ├── conversation-array.bxs
│   ├── fluent-messages.bxs
│   ├── dynamic-conversation.bxs
│   ├── personalities.bxs
│   └── chat-loop.bxs
└── labs/
    └── chat-assistant.bxs
```
