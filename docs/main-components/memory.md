---
description: "Comprehensive guide on using standard memory systems in BoxLang AI for conversation history and context retention."
icon: memory
---

# Memory Systems

Memory systems enable AI to maintain context across multiple interactions, making conversations more coherent and contextually aware. This guide covers **standard conversation memory** types that store and manage message history.

> **Looking for Vector Memory?** For semantic search and retrieval using embeddings, see the [Vector Memory Guide](vector-memory.md).

---

## Table of Contents

- [Multi-Tenant Isolation](#multi-tenant-isolation)
- [Overview](#overview)
- [Memory Types](#memory-types)
- [Creating Memory](#creating-memory)
- [Using Memory in Pipelines](#using-memory-in-pipelines)
- [Memory Patterns](#memory-patterns)
- [Best Practices](#best-practices)
- [Advanced Memory](#advanced-memory)

---

## Multi-Tenant Isolation

All memory types support multi-tenant isolation through `userId` and `conversationId` parameters:

- **userId**: Isolate conversations per user in multi-user applications
- **conversationId**: Separate multiple conversations for the same user
- **Combined**: Use both for complete conversation isolation

Multi-tenant support is built into ALL memory types including:
- Standard memories: Window, Summary, Session, File, Cache, JDBC
- Vector memories: All 11 vector providers (see Vector Memory Guide)
- Hybrid memory: Combines recent + semantic with isolation

### Basic Multi-Tenant Usage

```java
// Per-user isolation
memory = aiMemory( "windowed",
    key: createUUID(),
    userId: "user123",
    config: { maxMessages: 10 }
)

// Per-conversation isolation (same user, different chats)
chat1 = aiMemory( "windowed",
    key: createUUID(),
    userId: "user123",
    conversationId: "support-ticket-456",
    config: { maxMessages: 10 }
)

chat2 = aiMemory( "windowed",
    key: createUUID(),
    userId: "user123",
    conversationId: "sales-inquiry-789",
    config: { maxMessages: 10 }
)
```

### Accessing Tenant Identifiers

```java
memory = aiMemory( "session",
    userId: "alice",
    conversationId: "chat1",
    config: { key: "support" }
)

// Get identifiers
userId = memory.getUserId()           // "alice"
conversationId = memory.getConversationId()  // "chat1"

// Export includes identifiers
exported = memory.export()
// { userId: "alice", conversationId: "chat1", messages: [...], ... }
```

For advanced patterns including security considerations, filtering strategies, and enterprise multi-tenancy, see the [Multi-Tenant Memory Guide](../advanced/multi-tenant-memory.md).

---

## Overview

Memory in AI systems allows for:

- **Context retention** across multiple turns
- **Conversation history** management
- **State persistence** in long-running applications
- **Flexible storage** options (memory, session, file, database)

Without memory, each AI call is independent with no knowledge of previous interactions. Standard memory types focus on managing conversation messages chronologically, while [Vector Memory](vector-memory.md) provides semantic search capabilities.

---

## Memory Types

BoxLang AI provides several standard memory implementations for conversation history management:

### Memory Type Comparison

Choose the right memory type for your use case:

| Feature | Windowed | Summary | Session | File | Cache | JDBC |
|---------|----------|---------|---------|------|-------|------|
| **Context Preservation** | Recent only | Full (compressed) | Recent only | All messages | Recent only | All messages |
| **Old Messages** | Discarded | Summarized | Discarded | Kept | Expired | Kept |
| **Token Usage** | Low | Moderate | Low-Moderate | High | Low | Medium-High |
| **Memory Loss** | High | Low | Medium | None | Medium | None |
| **Multi-Tenant** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Best For** | Quick chats | Long conversations | Web apps | Audit trails | Distributed apps | Enterprise systems |
| **Setup Complexity** | Simple | Moderate | Simple | Simple | Moderate | Complex |
| **Cost** | Lowest | Low-Medium | Low | Low | Medium | Medium-High |
| **Historical Awareness** | None | Excellent | Limited | Perfect | None | Perfect |
| **Persistence** | None | None | Session scope | File system | Cache provider | Database |

> **Need Semantic Search?** Check out [Vector Memory](vector-memory.md) for embedding-based retrieval including BoxVector (in-memory), ChromaDB, PostgreSQL pgvector, Pinecone, Qdrant, Weaviate, Milvus, and Hybrid memory combining recent + semantic.

### Windowed Memory

Maintains the most recent N messages, automatically discarding older messages when the limit is reached.

```java
// Basic usage (single-tenant)
memory = aiMemory( "windowed", {
    maxMessages: 10  // Keep last 10 messages
} )

// Multi-tenant usage
memory = aiMemory( "windowed",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: { maxMessages: 10 }
)
```

**Best for:**
- Short conversations
- Real-time chat applications
- Cost-conscious applications
- Simple context requirements

**Limitations:**
- Loses all context from discarded messages
- No awareness of earlier conversation history
- Can lose important facts mentioned earlier

### Summary Memory

Automatically summarizes older messages when the limit is reached, keeping summaries + recent messages. This provides the best of both worlds: full context awareness with controlled token usage.

```java
// Basic usage (single-tenant)
memory = aiMemory( "summary", {
    maxMessages: 20,           // Total messages before summarization
    summaryThreshold: 10,      // Keep last 10 messages unsummarized
    summaryModel: "gpt-4o-mini",  // Model for generating summaries
    summaryProvider: "openai"  // Provider for summarization
} )

// Multi-tenant usage
memory = aiMemory( "summary",
    key: createUUID(),
    userId: "user123",
    conversationId: "support-chat",
    config: {
        maxMessages: 20,
        summaryThreshold: 10,
        summaryModel: "gpt-4o-mini",
        summaryProvider: "openai"
    }
)
```

**How it works:**

1. Messages accumulate normally until `maxMessages` is reached
2. When threshold exceeded, older messages are summarized
3. Summary is kept as a special assistant message
4. Recent messages (last N) remain unmodified
5. Progressive summarization: new summaries build on previous ones

**Best for:**

- Long conversations with important history
- Complex context requirements
- Applications needing historical awareness
- Customer support systems
- Research or analysis tasks
- Multi-session interactions

**Advantages:**

- Preserves key facts and decisions from old messages
- Maintains full conversation awareness
- Moderate token usage (lower than keeping all messages)
- No sudden context loss
- AI can reference earlier conversation points

### Session Memory

Persists conversation history in the web session scope, surviving page refreshes. Session memory automatically creates a composite key combining `key + userId + conversationId` to ensure complete isolation.

```java
// Basic usage (single-tenant)
memory = aiMemory( "session", {
    key: "chatbot",  // Session key for storage
    maxMessages: 20
} )

// Multi-tenant usage - automatic isolation
memory = aiMemory( "session",
    userId: "user123",
    conversationId: "support",
    config: {
        key: "chat",  // Base key
        maxMessages: 20
    }
)
// Internally stored as: session["chat_user123_support"]

// Each user+conversation combination is isolated
aliceSupport = aiMemory( "session", userId: "alice", conversationId: "support", config: { key: "chat" } )
aliceSales = aiMemory( "session", userId: "alice", conversationId: "sales", config: { key: "chat" } )
bobSupport = aiMemory( "session", userId: "bob", conversationId: "support", config: { key: "chat" } )
// All three are completely isolated in session scope
```

**Best for:**
- Web applications
- Multi-page conversations
- User-specific context
- Persistent chat interfaces
- Multi-user web apps

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

### Cache Memory

Stores conversation history in CacheBox for distributed applications. Cache memory automatically creates a composite cache key combining `cacheKey + userId + conversationId` for isolation.

```java
// Basic usage (single-tenant)
memory = aiMemory( "cache", {
    cacheName: "default",           // CacheBox cache name
    cacheKey: "chat",               // Base key for this conversation type
    maxMessages: 30,
    cacheTimeout: 3600,             // Timeout in seconds (optional)
    cacheLastAccessTimeout: 1800    // Last access timeout (optional)
} )

// Multi-tenant usage - automatic cache key isolation
memory = aiMemory( "cache",
    userId: "user123",
    conversationId: "support",
    config: {
        cacheName: "default",
        cacheKey: "chat",
        maxMessages: 30,
        cacheTimeout: 3600
    }
)
// Internally uses cache key: "chat_user123_support"

// Each user+conversation gets its own cache entry
aliceSupport = aiMemory( "cache", userId: "alice", conversationId: "support",
    config: { cacheKey: "chat" } )
// Cache key: "chat_alice_support"

aliceSales = aiMemory( "cache", userId: "alice", conversationId: "sales",
    config: { cacheKey: "chat" } )
// Cache key: "chat_alice_sales"
```

**Best for:**
- Distributed applications
- Load-balanced environments
- Applications with existing CacheBox
- Scalable session management
- Multi-user distributed systems

**Features:**
- Integrates with any CacheBox provider (Redis, Memcached, Couchbase, etc.)
- Automatic expiration policies
- Distributed cache support
- High-performance access

### JDBC Memory

Stores conversation history in a database using JDBC for enterprise persistence. JDBC memory includes `userId` and `conversationId` columns for automatic multi-tenant isolation.

```java
// Basic usage (single-tenant)
memory = aiMemory( "jdbc", {
    datasource: "myDS",             // JDBC datasource name
    tableName: "ai_conversations",   // Table to store messages
    conversationId: "chat123",       // Unique conversation identifier
    maxMessages: 100,
    autoCreate: true                 // Auto-create table if missing
} )

// Multi-tenant usage - automatic database isolation
memory = aiMemory( "jdbc",
    userId: "user123",
    conversationId: "support-ticket-456",
    config: {
        datasource: "myDS",
        tableName: "ai_conversations",
        maxMessages: 100,
        autoCreate: true
    }
)
// Queries automatically filter by: WHERE user_id = 'user123' AND conversation_id = 'support-ticket-456'
```

**Best for:**
- Enterprise applications
- Multi-user systems
- Compliance requirements
- Centralized storage
- Cross-platform access
- Advanced querying and reporting

**Features:**
- Works with any JDBC-compatible database
- Automatic table creation with multi-tenant columns
- Query conversation history by user/conversation
- Full ACID compliance
- Supports PostgreSQL, MySQL, SQL Server, Oracle, etc.

**Table Structure:**
```sql
CREATE TABLE ai_conversations (
    id VARCHAR(50) PRIMARY KEY,
    conversation_id VARCHAR(100),
    user_id VARCHAR(100),           -- Multi-tenant: user identifier
    role VARCHAR(20),
    content TEXT,
    metadata TEXT,
    created_at TIMESTAMP,
    INDEX idx_conversation (conversation_id),
    INDEX idx_user_conv (user_id, conversation_id)  -- Multi-tenant index
)
```

**Query Examples:**
```sql
-- Get all conversations for a user
SELECT DISTINCT conversation_id FROM ai_conversations
WHERE user_id = 'user123'

-- Get messages for specific user conversation
SELECT * FROM ai_conversations
WHERE user_id = 'user123' AND conversation_id = 'support-ticket-456'
ORDER BY created_at

-- Count messages per user
SELECT user_id, COUNT(*) as message_count
FROM ai_conversations
GROUP BY user_id
```

---

## Creating Memory

### Basic Memory Creation

```java
// Create windowed memory (single-tenant)
memory = aiMemory( "windowed", { maxMessages: 10 } )

// Create multi-tenant memory
memory = aiMemory( "windowed",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: { maxMessages: 10 }
)

// Add a system message
memory.add( aiMessage().system( "You are a helpful assistant" ) )

// Add user message
memory.add( aiMessage().user( "Hello!" ) )

// Get all messages
messages = memory.getAll()

// Access tenant identifiers
userId = memory.getUserId()           // "user123"
conversationId = memory.getConversationId()  // "chat456"
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

### Window Memory-Enabled Chat

```java
// Single-tenant chat
memory = aiMemory( "windowed", { maxMessages: 10 } )
memory.add( aiMessage().system( "You are a friendly assistant" ) )

function chat( userInput ) {
    memory.add( aiMessage().user( userInput ) )
    response = aiChat( memory.getAll() )
    memory.add( aiMessage().assistant( response ) )
    return response
}

println( chat( "My name is John" ) )
println( chat( "What's my name?" ) )  // AI remembers: "Your name is John"

// Multi-tenant chat with user/conversation isolation
function chatMultiTenant( userId, conversationId, userInput ) {
    // Each user+conversation gets its own isolated memory
    memory = aiMemory( "session",
        userId: arguments.userId,
        conversationId: arguments.conversationId,
        config: { key: "chat", maxMessages: 10 }
    )

    memory.add( aiMessage().user( arguments.userInput ) )
    response = aiChat( memory.getAll() )
    memory.add( aiMessage().assistant( response ) )

    return response
}

// Each call is automatically isolated
println( chatMultiTenant( "alice", "chat1", "My name is Alice" ) )
println( chatMultiTenant( "bob", "chat1", "My name is Bob" ) )
println( chatMultiTenant( "alice", "chat1", "What's my name?" ) )  // "Alice"
println( chatMultiTenant( "bob", "chat1", "What's my name?" ) )    // "Bob"
```

### Memory in Model Pipelines

```java
// Single-tenant pipeline
memory = aiMemory( "windowed", { maxMessages: 10 } )

pipeline = aiModel( "openai" )
    .withMemory( memory )
    .withSystemPrompt( "You are a helpful assistant" )

response = pipeline.run( "Hello!" )
response = pipeline.run( "What did I just say?" )  // Context preserved

// Multi-tenant pipeline with isolation
function createUserPipeline( userId, conversationId ) {
    memory = aiMemory( "session",
        userId: arguments.userId,
        conversationId: arguments.conversationId,
        config: { key: "pipeline", maxMessages: 10 }
    )

    return aiModel( "openai" )
        .withMemory( memory )
        .withSystemPrompt( "You are a helpful assistant" )
}

// Each user gets isolated pipeline
alicePipeline = createUserPipeline( "alice", "support" )
bobPipeline = createUserPipeline( "bob", "support" )

alicePipeline.run( "My order is #12345" )
bobPipeline.run( "My order is #67890" )

alicePipeline.run( "What's my order number?" )  // "#12345"
bobPipeline.run( "What's my order number?" )    // "#67890"
```

### Summary Memory in Long Conversations

```java
// Multi-tenant summary memory for customer support
function createSupportAgent( userId, ticketId ) {
    memory = aiMemory( "summary",
        userId: arguments.userId,
        conversationId: arguments.ticketId,
        config: {
            maxMessages: 30,
            summaryThreshold: 15,
            summaryModel: "gpt-4o-mini"
        }
    )

    return aiAgent(
        name: "SupportAgent",
        memory: memory,
        instructions: "Help customers with their orders"
    )
}

// Each customer ticket is isolated
aliceAgent = createSupportAgent( "alice", "ticket-001" )
bobAgent = createSupportAgent( "bob", "ticket-002" )

// Long conversation - history is preserved via summarization
aliceAgent.run( "My order #12345 is late" )
aliceAgent.run( "I ordered it 2 weeks ago" )
aliceAgent.run( "It was supposed to arrive last Friday" )
// ... 20 more exchanges about refunds, shipping, etc ...
aliceAgent.run( "By the way, what was my order number again?" )
// Agent responds: "Your order number is #12345"
// Context preserved even though it was mentioned 25 messages ago!

// Bob's conversation is completely separate
bobAgent.run( "My order #67890 hasn't shipped" )
// No cross-contamination with Alice's conversation
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

Modern approach using built-in `userId` and `conversationId` parameters:

```java
// Modern multi-tenant approach
function getUserMemory( userId, conversationId = "" ) {
    return aiMemory( "session",
        key: "chat",
        userId: arguments.userId,
        conversationId: arguments.conversationId,
        config: { maxMessages: 20 }
    )
}

function chat( userId, message, conversationId = "" ) {
    memory = getUserMemory( arguments.userId, arguments.conversationId )
    memory.add( aiMessage().user( arguments.message ) )

    response = aiChat( memory.getAll() )

    memory.add( aiMessage().assistant( response ) )

    return response
}

// Usage - each call is automatically isolated by userId/conversationId
println( chat( "alice", "My name is Alice", "support" ) )
println( chat( "alice", "I need help", "sales" ) )
println( chat( "bob", "My name is Bob", "support" ) )
println( chat( "alice", "What's my name?", "support" ) )  // "Alice" - correct context
println( chat( "bob", "What's my name?", "support" ) )    // "Bob" - isolated

// Alternative: Legacy pattern with manual dictionary (not recommended)
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
}
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
// Create multi-tenant memory
memory = aiMemory( "windowed",
    userId: "user123",
    conversationId: "support-456",
    config: { maxMessages: 10 }
)

// Store additional metadata
memory.metadata( {
    sessionId: "session789",
    startTime: now(),
    topic: "customer_support",
    priority: "high"
} )

// Add messages
memory.add( aiMessage().user( "I need help" ) )

// Get metadata
info = memory.metadata()
println( "User: #memory.getUserId()#, Topic: #info.topic#" )

// Export with metadata AND tenant identifiers
export = memory.export()
// Contains: { userId: "user123", conversationId: "support-456", messages: [...], metadata: {...} }

// Import preserves tenant identifiers
newMemory = aiMemory( "windowed", { maxMessages: 10 } )
newMemory.import( export )
println( newMemory.getUserId() )  // "user123"
println( newMemory.getConversationId() )  // "support-456"
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
// Short, cost-sensitive chats (single-tenant)
memory = aiMemory( "windowed", { maxMessages: 5 } )

// Short chats with multi-tenant isolation
memory = aiMemory( "windowed",
    userId: "user123",
    conversationId: "chat456",
    config: { maxMessages: 5 }
)

// Long, context-heavy conversations
memory = aiMemory( "summary", { maxMessages: 30 } )

// Web applications with multi-user support
memory = aiMemory( "session",
    userId: "user123",
    conversationId: "support",
    config: { key: "chat", maxMessages: 20 }
)

// Audit trails / compliance with user tracking
memory = aiMemory( "file",
    userId: "user123",
    conversationId: "ticket-789",
    config: { filePath: "chats/memory.json" }
)
// Automatically stored as: chats/memory_user123_ticket-789.json
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

// Export for analysis (includes userId/conversationId if present)
function saveConversation( memory ) {
    export = memory.export()
    // Export contains: { userId, conversationId, messages, metadata }

    // Build filename with tenant identifiers
    filename = "conversation_#now()#"
    if ( !isNull( memory.getUserId() ) ) {
        filename &= "_#memory.getUserId()#"
    }
    if ( !isNull( memory.getConversationId() ) ) {
        filename &= "_#memory.getConversationId()#"
    }
    filename &= ".json"

    fileWrite( filename, serializeJSON( export, true ) )
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
// Save memory state (preserves userId/conversationId)
function saveMemoryState( memory, filename ) {
    state = {
        userId: memory.getUserId(),
        conversationId: memory.getConversationId(),
        messages: memory.getAll(),
        metadata: memory.metadata(),
        timestamp: now()
    }
    fileWrite( filename, serializeJSON( state, true ) )
}

// Restore memory state (including tenant identifiers)
function loadMemoryState( filename, userId = "", conversationId = "" ) {
    if ( !fileExists( filename ) ) {
        return aiMemory( "windowed",
            userId: arguments.userId,
            conversationId: arguments.conversationId,
            config: { maxMessages: 10 }
        )
    }

    state = deserializeJSON( fileRead( filename ) )

    // Restore memory with tenant identifiers from saved state
    memory = aiMemory( "windowed",
        userId: state.userId ?: arguments.userId,
        conversationId: state.conversationId ?: arguments.conversationId,
        config: { maxMessages: 10 }
    )

    state.messages.each( msg => memory.add( msg ) )
    memory.metadata( state.metadata )

    return memory
}

// Usage examples
// Save
memory = aiMemory( "windowed", userId: "alice", conversationId: "support", config: { maxMessages: 10 } )
saveMemoryState( memory, "alice_support.json" )

// Restore
restoredMemory = loadMemoryState( "alice_support.json" )
println( restoredMemory.getUserId() )  // "alice"
println( restoredMemory.getConversationId() )  // "support"
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

### 8. Multi-Tenant Security Considerations

```java
// CRITICAL: Always validate userId comes from authenticated session
function createUserMemory( userId, conversationId ) {
    // Validate userId matches authenticated user
    if ( session.user.id != arguments.userId ) {
        throw( type="SecurityException", message="User ID mismatch" )
    }

    return aiMemory( "session",
        userId: arguments.userId,
        conversationId: arguments.conversationId,
        config: { key: "chat", maxMessages: 20 }
    )
}

// For JDBC memory, ensure queries filter by authenticated user
function loadUserConversation( userId, conversationId ) {
    // Verify user owns this conversation
    if ( !hasAccessToConversation( session.user.id, arguments.conversationId ) ) {
        throw( type="SecurityException", message="Access denied" )
    }

    return aiMemory( "jdbc",
        userId: arguments.userId,
        conversationId: arguments.conversationId,
        config: { datasource: "myDS", tableName: "conversations" }
    )
}

// List conversations only for authenticated user
function getUserConversations( userId ) {
    if ( session.user.id != arguments.userId ) {
        throw( type="SecurityException", message="Access denied" )
    }

    query = queryExecute(
        "SELECT DISTINCT conversation_id FROM ai_conversations WHERE user_id = ?",
        [ arguments.userId ],
        { datasource: "myDS" }
    )

    return query
}
```

---

## Advanced Examples

### Example 1: RAG with Memory

Combine retrieval-augmented generation with conversation memory:

```java
// Multi-tenant RAG system
function chatWithKnowledge( userId, conversationId, userQuery ) {
    // Create user-specific memory
    memory = aiMemory( "session",
        userId: arguments.userId,
        conversationId: arguments.conversationId,
        config: { key: "rag", maxMessages: 10 }
    )

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

// Usage with automatic isolation
response1 = chatWithKnowledge( "alice", "research-1", "What is quantum computing?" )
response2 = chatWithKnowledge( "alice", "research-2", "What is machine learning?" )
response3 = chatWithKnowledge( "bob", "research-1", "What is quantum computing?" )
// All three conversations are isolated
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

## Advanced Memory

### Vector Memory

For semantic search and retrieval using embeddings, see the comprehensive [Vector Memory Guide](vector-memory.md) which covers:

- **BoxVectorMemory** - In-memory vector storage for development
- **HybridMemory** - Combines recent messages with semantic search
- **ChromaVectorMemory** - ChromaDB integration
- **PostgresVectorMemory** - PostgreSQL pgvector extension
- **PineconeVectorMemory** - Pinecone cloud vector database
- **QdrantVectorMemory** - Qdrant vector search engine
- **WeaviateVectorMemory** - Weaviate knowledge graph
- **MilvusVectorMemory** - Milvus vector database

Vector memory enables finding relevant past conversations based on meaning rather than recency.

### Custom Memory

You can create custom memory implementations for specialized requirements:

```java
// Extend BaseMemory
class extends="bxModules.bxai.models.memory.BaseMemory" {

    function configure( required struct config ) {
        super.configure( arguments.config );
        // Custom configuration
        return this;
    }

    function add( required any message ) {
        // Custom storage logic
        return this;
    }

    function getAll() {
        // Custom retrieval logic
        return [];
    }
}
```

See the [Custom Memory Guide](../advanced/custom-memory.md) for complete examples and patterns.

---

## See Also

- [Vector Memory Guide](vector-memory.md) - Semantic search and retrieval
- [Custom Memory Guide](../advanced/custom-memory.md) - Build your own memory types
- [Messages Documentation](messages.md) - Building message objects
- [Agents Documentation](agents.md) - Using memory in agents
- [Pipeline Overview](overview.md) - Memory in pipelines
- [Memory BIF Reference](../../readme.md#aimemory) - aiMemory() function reference

---

**Next Steps:** Learn about [Vector Memory](vector-memory.md) for semantic search or [streaming in pipelines](streaming.md) for real-time responses.
