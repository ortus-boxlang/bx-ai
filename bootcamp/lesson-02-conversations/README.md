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
    print( "You: " )
    userInput = readLine()
    
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

### Pattern: Context Injection

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

print( "Enter 1, 2, or 3: " )
choice = readLine()

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
    print( "You: " )
    userInput = readLine()
    
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
