# Lesson 7: Memory Systems

**Duration:** 75 minutes
**Prerequisites:** Lessons 1-6 completed

## Learning Objectives

By the end of this lesson, you will:

- Understand AI memory types
- Implement windowed memory (most recent N messages)
- Use summary memory (condensed history)
- Build persistent session memory
- Optimize memory for long conversations

---

## Part 1: Why Memory?

### The Problem

```java
// Each call is independent - no memory
aiChat( "My name is John" )
aiChat( "What's my name?" )
// Response: "I don't know your name"
```

### The Solution: Memory

```java
memory = aiMemory( "windowed", { maxMessages: 10 } )

memory.add( aiMessage().user( "My name is John" ) )
memory.add( aiMessage().assistant( "Nice to meet you, John!" ) )

memory.add( aiMessage().user( "What's my name?" ) )
answer = aiChat( memory.getAll() )
// Response: "Your name is John"
```

---

## Part 2: Memory Types

### Windowed Memory

Keeps last N messages:

```java
memory = aiMemory( "windowed", { maxMessages: 5 } )

// Adds messages, auto-trims to last 5
memory.add( aiMessage().user( "Message 1" ) )
// ... 10 messages later
memory.add( aiMessage().user( "Message 11" ) )

println( memory.count() )  // 5 (trimmed oldest)
```

### Summary Memory

Intelligently compresses old messages while preserving context:

```java
memory = aiMemory( "summary", {
    maxMessages: 20,           // Total message limit
    summaryThreshold: 10,      // Keep last 10 uncompressed
    summaryModel: "gpt-4o-mini"
} )

// After 20 messages:
// - Oldest messages get summarized
// - Summary preserved as context
// - Last 10 messages kept intact
// - No sudden context loss!
```

**When to use:**
- Long conversations (customer support, research)
- Need to reference earlier facts
- Acceptable moderate token cost for better context

### Session Memory

Web session-persisted:

```java
memory = aiMemory( "session", {
    key: "chatbot",
    maxMessages: 20
} )

// Automatically saves to session scope
// Survives page refreshes
```

---

## Part 3: Using Memory

### Basic Pattern

```java
// Create memory
memory = aiMemory( "windowed", { maxMessages: 10 } )

// Chat function
function chat( prompt ) {
    memory.add( aiMessage().user( prompt ) )

    answer = aiChat( memory.getAll() )

    memory.add( aiMessage().assistant( answer ) )

    return answer
}

// Use it
println( chat( "I like pizza" ) )
println( chat( "What do I like?" ) )  // Remembers!
```

---

## Examples to Run

### 1. `windowed-memory.bxs`
Recent message memory

### 2. `summary-memory.bxs`
Condensed history

### 3. `session-memory.bxs`
Persistent web chat

### 4. `memory-comparison.bxs`
Compare memory types

---

## Lab Exercise: Memory-Enhanced Chatbot

**File:** `labs/memory-chatbot.bxs`

**Objective:**
Build a chatbot that remembers conversation context.

---

## Key Takeaways

✅ Memory enables context-aware conversations
✅ **Windowed memory**: Simple, low-cost, discards old messages
✅ **Summary memory**: Intelligent compression preserves full context
✅ **Session memory**: Web-persistent across page refreshes
✅ Choose memory type based on conversation length and context needs

### Memory Selection Guide

| Scenario | Best Memory | Why |
|----------|-------------|-----|
| Quick Q&A | Windowed | Low cost, simple |
| Customer Support | Summary | Long chats, need history |
| Web Chatbot | Session | Persists across pages |
| Research Assistant | Summary | Complex multi-turn analysis |

---

## Next Lesson

**Lesson 8: AI Agents** - Build autonomous AI agents.
