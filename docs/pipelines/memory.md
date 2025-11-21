# Memory in Pipelines

Memory systems enable AI to maintain context across multiple interactions, making conversations more coherent and contextually aware. This guide covers how to use memory within BoxLang AI pipelines.

---

## Table of Contents

- [Overview](#overview)
- [Memory Types](#memory-types)
- [Creating Memory](#creating-memory)
- [Using Memory in Pipelines](#using-memory-in-pipelines)
- [Memory Patterns](#memory-patterns)
- [Best Practices](#best-practices)

---

## Overview

Memory in AI systems allows for:

- **Context retention** across multiple turns
- **Conversation history** management
- **State persistence** in long-running applications
- **Selective context** for relevant information

Without memory, each AI call is independent with no knowledge of previous interactions.

---

## Memory Types

BoxLang AI provides several memory implementations:

### Windowed Memory

Maintains the most recent N messages, automatically discarding older messages when the limit is reached.

```java
memory = aiMemory( "windowed", {
    maxMessages: 10  // Keep last 10 messages
} )
```

**Best for:**
- Short conversations
- Real-time chat applications
- Cost-conscious applications
- Simple context requirements

### Summary Memory

Automatically summarizes older messages when the limit is reached, keeping summaries + recent messages.

```java
memory = aiMemory( "summary", {
    maxMessages: 20,  // Trigger summary after 20 messages
    summaryModel: "gpt-3.5-turbo"  // Model to use for summarization
} )
```

**Best for:**
- Long conversations
- Complex context requirements
- Applications needing historical awareness
- Customer support systems

### Session Memory

Persists conversation history in the web session scope, surviving page refreshes.

```java
memory = aiMemory( "session", {
    key: "chatbot",  // Session key for storage
    maxMessages: 20
} )
```

**Best for:**
- Web applications
- Multi-page conversations
- User-specific context
- Persistent chat interfaces

### File Memory

Stores conversation history in files for long-term persistence.

```java
memory = aiMemory( "file", {
    filePath: "/path/to/memory.json",
    maxMessages: 50
} )
```

**Best for:**
- Long-term storage
- Audit trails
- Offline analysis
- Cross-session continuity

---

## Creating Memory

### Basic Memory Creation

```java
// Create windowed memory
memory = aiMemory( "windowed", { maxMessages: 10 } )

// Add a system message
memory.add( aiMessage().system( "You are a helpful assistant" ) )

// Add user message
memory.add( aiMessage().user( "Hello!" ) )

// Get all messages
messages = memory.getAll()
```

### Memory with Configuration

```java
memory = aiMemory( "windowed", {
    maxMessages: 10,
    trimToMaxMessages: true,  // Auto-trim when limit reached
    includeSystemMessage: true  // Keep system message when trimming
} )
```

### Pre-populated Memory

```java
// Start with conversation history
initialMessages = [
    aiMessage().system( "You are a coding tutor" ),
    aiMessage().user( "What is a variable?" ),
    aiMessage().assistant( "A variable is a named storage location..." )
]

memory = aiMemory( "windowed", { maxMessages: 10 } )
initialMessages.each( msg => memory.add( msg ) )
```

---

## Using Memory in Pipelines

### Simple Memory-Enabled Chat

```java
// Create memory
memory = aiMemory( "windowed", { maxMessages: 10 } )

// Add system message
memory.add( aiMessage().system( "You are a friendly assistant" ) )

// Chat function
function chat( userInput ) {
    // Add user message
    memory.add( aiMessage().user( userInput ) )
    
    // Get AI response with full context
    response = aiChat( memory.getAll() )
    
    // Add assistant response to memory
    memory.add( aiMessage().assistant( response ) )
    
    return response
}

// Use it
println( chat( "My name is John" ) )
println( chat( "What's my name?" ) )  // AI remembers: "Your name is John"
```

### Memory in Model Pipelines

```java
memory = aiMemory( "windowed", { maxMessages: 10 } )

// Create a pipeline with memory
pipeline = aiModel( "openai" )
    .withMemory( memory )
    .withSystemPrompt( "You are a helpful assistant" )

// Run pipeline - automatically manages memory
response = pipeline.run( "Hello!" )
response = pipeline.run( "What did I just say?" )  // Context preserved
```

### Streaming with Memory

```java
memory = aiMemory( "windowed", { maxMessages: 10 } )
memory.add( aiMessage().system( "You are a concise assistant" ) )

function streamChat( userInput ) {
    memory.add( aiMessage().user( userInput ) )
    
    fullResponse = ""
    
    aiChatStream(
        memory.getAll(),
        ( chunk ) => {
            fullResponse &= chunk
            print( chunk )
        }
    )
    
    memory.add( aiMessage().assistant( fullResponse ) )
    println()
}
```

---

## Memory Patterns

### Pattern 1: Conversation Manager

Encapsulate memory logic in a reusable component:

```java
component {
    property name="memory";
    property name="systemPrompt";
    
    function init( type = "windowed", config = {} ) {
        variables.memory = aiMemory( arguments.type, arguments.config )
        return this
    }
    
    function setSystemPrompt( prompt ) {
        variables.systemPrompt = arguments.prompt
        variables.memory.add( aiMessage().system( arguments.prompt ) )
        return this
    }
    
    function chat( userInput ) {
        variables.memory.add( aiMessage().user( arguments.userInput ) )
        
        response = aiChat( variables.memory.getAll() )
        
        variables.memory.add( aiMessage().assistant( response ) )
        
        return response
    }
    
    function reset() {
        variables.memory.clear()
        if ( !isNull( variables.systemPrompt ) ) {
            variables.memory.add( aiMessage().system( variables.systemPrompt ) )
        }
        return this
    }
    
    function export() {
        return variables.memory.export()
    }
    
    function getHistory() {
        return variables.memory.getAll()
    }
}

// Usage
chatManager = new ConversationManager( "windowed", { maxMessages: 10 } )
    .setSystemPrompt( "You are a helpful coding tutor" )

println( chatManager.chat( "What is a loop?" ) )
println( chatManager.chat( "Show me an example" ) )
```

### Pattern 2: Multi-User Memory

Separate memory per user:

```java
component {
    property name="userMemories" default="{}";
    
    function getUserMemory( userId ) {
        if ( !variables.userMemories.keyExists( arguments.userId ) ) {
            variables.userMemories[ arguments.userId ] = aiMemory( "windowed", {
                maxMessages: 20
            } )
        }
        return variables.userMemories[ arguments.userId ]
    }
    
    function chat( userId, message ) {
        memory = getUserMemory( arguments.userId )
        memory.add( aiMessage().user( arguments.message ) )
        
        response = aiChat( memory.getAll() )
        
        memory.add( aiMessage().assistant( response ) )
        
        return response
    }
}

// Usage
chatService = new MultiUserChatService()

// Each user has separate memory
println( chatService.chat( "user123", "My name is Alice" ) )
println( chatService.chat( "user456", "My name is Bob" ) )
println( chatService.chat( "user123", "What's my name?" ) )  // "Alice"
println( chatService.chat( "user456", "What's my name?" ) )  // "Bob"
```

### Pattern 3: Contextual Memory Switching

Switch memory contexts based on conversation topics:

```java
component {
    property name="memories" default="{}";
    property name="currentContext" default="general";
    
    function switchContext( context ) {
        variables.currentContext = arguments.context
        
        if ( !variables.memories.keyExists( arguments.context ) ) {
            variables.memories[ arguments.context ] = aiMemory( "windowed", {
                maxMessages: 10
            } )
        }
        
        return this
    }
    
    function chat( message ) {
        memory = variables.memories[ variables.currentContext ]
        memory.add( aiMessage().user( arguments.message ) )
        
        response = aiChat( memory.getAll() )
        
        memory.add( aiMessage().assistant( response ) )
        
        return response
    }
}

// Usage
bot = new ContextualBot()

bot.switchContext( "coding" ).chat( "Explain variables" )
bot.switchContext( "cooking" ).chat( "How do I make pasta?" )
bot.switchContext( "coding" ).chat( "What did we discuss?" )  // Remembers coding context
```

### Pattern 4: Memory with Metadata

Track additional context with metadata:

```java
memory = aiMemory( "windowed", { maxMessages: 10 } )

// Store metadata
memory.metadata( {
    userId: "user123",
    sessionId: "session456",
    startTime: now(),
    topic: "customer_support"
} )

// Add messages
memory.add( aiMessage().user( "I need help" ) )

// Get metadata
info = memory.metadata()
println( "User: #info.userId#, Topic: #info.topic#" )

// Export with metadata
export = memory.export()
// Contains both messages and metadata
```

### Pattern 5: Memory Summarization

Explicitly summarize conversation history:

```java
function summarizeConversation( memory ) {
    messages = memory.getAll()
    
    summaryPrompt = "Summarize this conversation in 3 bullet points:\n\n"
    
    messages.each( msg => {
        summaryPrompt &= "#msg.role#: #msg.content#\n"
    } )
    
    summary = aiChat( summaryPrompt )
    
    return summary
}

// Usage
memory = aiMemory( "windowed", { maxMessages: 20 } )

// ... have a long conversation ...

// Get summary
summary = summarizeConversation( memory )
println( "Conversation summary:\n#summary#" )
```

---

## Best Practices

### 1. Choose the Right Memory Type

```java
// Short, cost-sensitive chats
memory = aiMemory( "windowed", { maxMessages: 5 } )

// Long, context-heavy conversations
memory = aiMemory( "summary", { maxMessages: 30 } )

// Web applications
memory = aiMemory( "session", { key: "chat", maxMessages: 20 } )

// Audit trails / compliance
memory = aiMemory( "file", { filePath: "chats/user123.json" } )
```

### 2. Set Appropriate Limits

```java
// Balance context vs. cost
memory = aiMemory( "windowed", {
    maxMessages: 10,  // Enough context without excessive tokens
    trimToMaxMessages: true
} )
```

### 3. Always Include System Messages

```java
memory = aiMemory( "windowed", { maxMessages: 10 } )

// Set behavior upfront
memory.add( aiMessage().system( 
    "You are a helpful assistant. Be concise and accurate."
) )
```

### 4. Handle Memory Lifecycle

```java
// Reset when changing topics
function newConversation() {
    memory.clear()
    memory.add( aiMessage().system( systemPrompt ) )
}

// Export for analysis
function saveConversation() {
    export = memory.export()
    fileWrite( "conversation_#now()#.json", serializeJSON( export ) )
}
```

### 5. Monitor Token Usage

```java
import bxModules.bxai.models.util.TokenCounter;

function estimateMemoryCost( memory ) {
    messages = memory.getAll()
    totalTokens = 0
    
    messages.each( msg => {
        totalTokens += TokenCounter::count( msg.content )
    } )
    
    return totalTokens
}

// Check before making expensive calls
tokens = estimateMemoryCost( memory )
if ( tokens > 3000 ) {
    println( "Warning: High token count (#tokens#). Consider trimming memory." )
}
```

### 6. Implement Memory Persistence

```java
// Save memory state
function saveMemoryState( memory, filename ) {
    state = {
        messages: memory.getAll(),
        metadata: memory.metadata(),
        timestamp: now()
    }
    fileWrite( filename, serializeJSON( state, true ) )
}

// Restore memory state
function loadMemoryState( filename ) {
    if ( !fileExists( filename ) ) {
        return aiMemory( "windowed", { maxMessages: 10 } )
    }
    
    state = deserializeJSON( fileRead( filename ) )
    memory = aiMemory( "windowed", { maxMessages: 10 } )
    
    state.messages.each( msg => memory.add( msg ) )
    memory.metadata( state.metadata )
    
    return memory
}
```

### 7. Handle Edge Cases

```java
function safeMemoryAdd( memory, message ) {
    try {
        memory.add( message )
    } catch( any e ) {
        // Memory full or other error
        logger.error( "Failed to add to memory: #e.message#" )
        
        // Clear oldest messages and retry
        memory.clear()
        memory.add( aiMessage().system( systemPrompt ) )
        memory.add( message )
    }
}
```

---

## Advanced Examples

### Example 1: RAG with Memory

Combine retrieval-augmented generation with conversation memory:

```java
memory = aiMemory( "windowed", { maxMessages: 10 } )

function chatWithKnowledge( userQuery ) {
    // Retrieve relevant documents
    relevantDocs = searchDocuments( userQuery )
    
    // Build context
    context = "Relevant information:\n" & relevantDocs.toList()
    
    // Add to memory
    memory.add( aiMessage().system( context ) )
    memory.add( aiMessage().user( userQuery ) )
    
    // Generate response
    response = aiChat( memory.getAll() )
    
    memory.add( aiMessage().assistant( response ) )
    
    return response
}
```

### Example 2: Multi-Stage Memory Pipeline

```java
// Stage 1: Collect information
infoMemory = aiMemory( "windowed", { maxMessages: 5 } )
infoMemory.add( aiMessage().system( "Collect user requirements" ) )

// ... gather requirements ...

// Stage 2: Generate solution using collected info
solutionMemory = aiMemory( "windowed", { maxMessages: 10 } )
solutionMemory.add( aiMessage().system( "Generate solution based on requirements" ) )

// Transfer relevant context
summary = summarizeConversation( infoMemory )
solutionMemory.add( aiMessage().user( "Requirements: #summary#" ) )

// Generate solution
solution = aiChat( solutionMemory.getAll() )
```

### Example 3: Adaptive Memory

Adjust memory size based on conversation complexity:

```java
component {
    property name="memory";
    property name="baseLimit" default="10";
    
    function init() {
        variables.memory = aiMemory( "windowed", { maxMessages: variables.baseLimit } )
        return this
    }
    
    function chat( message ) {
        variables.memory.add( aiMessage().user( message ) )
        
        // Detect if conversation is getting complex
        if ( isComplexQuery( message ) ) {
            // Increase memory limit temporarily
            expandMemory( variables.baseLimit * 2 )
        }
        
        response = aiChat( variables.memory.getAll() )
        
        variables.memory.add( aiMessage().assistant( response ) )
        
        return response
    }
    
    function isComplexQuery( message ) {
        keywords = [ "explain", "detailed", "comprehensive", "analyze" ]
        return keywords.some( kw => message.findNoCase( kw ) > 0 )
    }
    
    function expandMemory( newLimit ) {
        // Create new memory with larger limit
        oldMessages = variables.memory.getAll()
        variables.memory = aiMemory( "windowed", { maxMessages: newLimit } )
        oldMessages.each( msg => variables.memory.add( msg ) )
    }
}
```

---

## See Also

- [Messages Documentation](messages.md)
- [Agents Documentation](agents.md)
- [Pipeline Overview](overview.md)
- [Memory BIF Reference](../../readme.md#aimemory)

---

**Next Steps:** Learn about [streaming in pipelines](streaming.md) for real-time responses.
