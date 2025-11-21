# Lesson 5: Streaming Responses

**Duration:** 60 minutes
**Prerequisites:** Lessons 1-4 completed

## Learning Objectives

By the end of this lesson, you will:

- Understand streaming vs. non-streaming responses
- Implement real-time streaming with callbacks
- Build interactive chat UIs
- Handle streaming errors gracefully
- Optimize user experience with progressive display

---

## Part 1: Why Streaming?

### Non-Streaming (Default)

```java
// User waits... waits... waits...
answer = aiChat( "Write a long essay" )
// Finally shows complete response (10-30 seconds later)
```

### Streaming

```java
// Shows text as it's generated
aiChatStream(
    "Write a long essay",
    ( chunk ) => print( chunk )
)
// User sees: "The..." "essay..." "begins..."
```

### Benefits

✅ **Better UX** - Immediate feedback
✅ **Perceived speed** - Feels faster
✅ **Interruptible** - Can stop early
✅ **Real-time** - Like ChatGPT interface

---

## Part 2: Basic Streaming

### Simple Streaming

```java
println( "AI: " )

aiChatStream(
    "Tell me a joke",
    ( chunk ) => {
        print( chunk )  // No newline
    }
)

println()  // End with newline
```

### Streaming with Accumulation

```java
fullResponse = ""

aiChatStream(
    "Explain loops",
    ( chunk ) => {
        fullResponse &= chunk
        print( chunk )
    }
)

// Now you have the complete response in fullResponse
```

---

## Part 3: Advanced Streaming

### Streaming with Metadata

```java
tokenCount = 0

aiChatStream(
    "Write a story",
    ( chunk, metadata ) => {
        print( chunk )
        tokenCount++
    }
)

println()
println( "Total tokens streamed: #tokenCount#" )
```

### Multi-Turn Streaming

```java
conversation = []

function streamChat( prompt ) {
    conversation.append( aiMessage().user( prompt ) )

    fullResponse = ""
    println( "AI: " )

    aiChatStream(
        conversation,
        ( chunk ) => {
            fullResponse &= chunk
            print( chunk )
        }
    )

    conversation.append( aiMessage().assistant( fullResponse ) )
    println()
}

streamChat( "Hello!" )
streamChat( "Tell me more" )
```

---

## Examples to Run

### 1. `basic-streaming.bxs`
Simple streaming demo

### 2. `chat-ui.bxs`
Interactive chat with streaming

### 3. `streaming-comparison.bxs`
Streaming vs. non-streaming speed

---

## Lab Exercise: Streaming Chat Bot

**File:** `labs/streaming-chat.bxs`

**Objective:**
Build an interactive chat bot with real-time streaming.

---

## Key Takeaways

✅ Streaming shows responses in real-time
✅ Better user experience (perceived speed)
✅ Use aiChatStream() with callback
✅ Accumulate chunks for full response
✅ Handle errors gracefully

---

## Next Lesson

**Lesson 6: Function Calling (Tools)** - Make AI call your functions.
