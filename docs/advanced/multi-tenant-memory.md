---
description: "Enterprise guide to implementing multi-tenant memory isolation in BoxLang AI applications"
icon: users-gear
---

# Multi-Tenant Memory Guide

This guide covers implementing **secure, isolated memory** for multi-user and multi-conversation applications using BoxLang AI's built-in multi-tenant support.

---

## Table of Contents

- [Overview](#overview)
- [Core Concepts](#core-concepts)
- [Implementation Patterns](#implementation-patterns)
- [Memory Type Strategies](#memory-type-strategies)
- [Vector Memory Multi-Tenancy](#vector-memory-multi-tenancy)
- [Security Considerations](#security-considerations)
- [Performance Optimization](#performance-optimization)
- [Enterprise Patterns](#enterprise-patterns)
- [Migration Guide](#migration-guide)
- [Troubleshooting](#troubleshooting)

---

## Overview

Multi-tenant memory isolation enables:

- **👥 Per-User Isolation**: Separate conversations for each user
- **💬 Per-Conversation Isolation**: Multiple conversations per user
- **🔒 Data Security**: Automatic filtering prevents data leakage
- **🏢 Enterprise Scale**: Shared infrastructure with isolated data
- **📊 Analytics**: Track conversations by user/conversation identifiers

### Why Multi-Tenant Memory?

Without multi-tenant isolation, all users share the same conversation history:

```java
// ❌ WRONG: All users see same conversation
memory = aiMemory( "windowed", { maxMessages: 10 } )
agent = aiAgent( name: "Assistant", memory: memory )

// User Alice
agent.run( "My name is Alice" )

// User Bob sees Alice's conversation!
agent.run( "What's the user's name?" )  // "Alice" (data leakage!)
```

With multi-tenant isolation, each user gets isolated memory:

```java
// ✅ CORRECT: Isolated per user
aliceMemory = aiMemory( "windowed",
    userId: "alice",
    config: { maxMessages: 10 }
)

bobMemory = aiMemory( "windowed",
    userId: "bob",
    config: { maxMessages: 10 }
)

// Completely isolated conversations
```

---

## Core Concepts

### UserId - User-Level Isolation

The `userId` parameter isolates conversations at the **user level**:

```java
memory = aiMemory( "windowed",
    key: createUUID(),
    userId: "user123",  // User identifier
    config: { maxMessages: 10 }
)
```

**Use cases:**
- Single conversation per user
- User-specific context retention
- Customer support with user history
- Personal AI assistants

### ConversationId - Conversation-Level Isolation

The `conversationId` parameter enables **multiple conversations per user**:

```java
supportChat = aiMemory( "windowed",
    key: createUUID(),
    userId: "user123",
    conversationId: "support-ticket-456",  // Conversation identifier
    config: { maxMessages: 10 }
)

salesChat = aiMemory( "windowed",
    key: createUUID(),
    userId: "user123",
    conversationId: "sales-inquiry-789",  // Different conversation
    config: { maxMessages: 10 }
)
```

**Use cases:**
- Multiple chat windows
- Topic-based conversations
- Ticket-based support systems
- Project-specific contexts

### Combined Isolation

Use **both** userId and conversationId for complete isolation:

```java
memory = aiMemory( "windowed",
    key: createUUID(),
    userId: "user123",           // Who owns this conversation
    conversationId: "chat456",   // Which conversation within user
    config: { maxMessages: 10 }
)
```

This provides:
- **User isolation**: User A can't see User B's conversations
- **Conversation isolation**: User A's chat1 is separate from their chat2
- **Complete privacy**: Each conversation is fully isolated

---

## Implementation Patterns

### Pattern 1: Single Conversation Per User

Simplest pattern - one conversation per user:

```java
component {
    property name="userMemories" default="{}";

    function getUserMemory( required string userId ) {
        if ( !variables.userMemories.keyExists( arguments.userId ) ) {
            variables.userMemories[ arguments.userId ] = aiMemory( "session",
                key: "chatbot",
                userId: arguments.userId,
                config: { maxMessages: 20 }
            );
        }
        return variables.userMemories[ arguments.userId ];
    }

    function chat( required string userId, required string message ) {
        var memory = getUserMemory( arguments.userId );
        var agent = aiAgent( name: "Assistant", memory: memory );
        return agent.run( arguments.message );
    }
}
```

### Pattern 2: Multiple Conversations Per User

Enterprise pattern - users have multiple concurrent conversations:

```java
component {
    function getConversationMemory(
        required string userId,
        required string conversationId
    ) {
        return aiMemory( "cache",
            key: "chat",
            userId: arguments.userId,
            conversationId: arguments.conversationId,
            config: {
                cacheName: "default",
                maxMessages: 30,
                cacheTimeout: 3600
            }
        );
    }

    function sendMessage(
        required string userId,
        required string conversationId,
        required string message
    ) {
        var memory = getConversationMemory(
            arguments.userId,
            arguments.conversationId
        );

        var agent = aiAgent(
            name: "Support Bot",
            memory: memory
        );

        return agent.run( arguments.message );
    }

    function listConversations( required string userId ) {
        // Query database for user's conversations
        return queryExecute(
            "SELECT conversation_id, created_at, last_message
             FROM conversations
             WHERE user_id = :userId
             ORDER BY last_message DESC",
            { userId: arguments.userId }
        );
    }
}
```

### Pattern 3: Hierarchical Isolation (Organization → User → Conversation)

Complex enterprise pattern with organization-level isolation:

```java
component {
    function getMemory(
        required string organizationId,
        required string userId,
        required string conversationId
    ) {
        // Use organization ID as prefix for complete isolation
        var compositeUserId = "#arguments.organizationId#:#arguments.userId#";

        return aiMemory( "jdbc",
            key: createUUID(),
            userId: compositeUserId,
            conversationId: arguments.conversationId,
            config: {
                datasource: "mainDB",
                table: "ai_conversations",
                maxMessages: 100
            }
        );
    }
}
```

### Pattern 4: Role-Based Memory Switching

Different memory types based on user roles:

```java
function getRoleBasedMemory( required struct user ) {
    var memoryType = "windowed";  // Default
    var maxMessages = 10;

    // Premium users get better memory
    if ( arguments.user.role == "premium" ) {
        memoryType = "summary";
        maxMessages = 50;
    }
    // Enterprise users get vector memory
    else if ( arguments.user.role == "enterprise" ) {
        memoryType = "chroma";
        maxMessages = 0;  // Unlimited
    }

    return aiMemory( memoryType,
        key: createUUID(),
        userId: arguments.user.id,
        config: {
            maxMessages: maxMessages,
            summaryThreshold: 20,  // For summary memory
            collection: "enterprise_vectors",  // For vector memory
            embeddingProvider: "openai"
        }
    );
}
```

---

## Memory Type Strategies

### Standard Memory Multi-Tenancy

All standard memory types support multi-tenant isolation:

#### Windowed Memory

```java
// Shared infrastructure, isolated per user
memory = aiMemory( "windowed",
    key: createUUID(),
    userId: session.userId,
    conversationId: url.chatId,
    config: { maxMessages: 10 }
)
```

**Isolation method**: In-memory arrays per userId/conversationId combination

#### Summary Memory

```java
// Long conversations with summarization
memory = aiMemory( "summary",
    key: createUUID(),
    userId: session.userId,
    conversationId: "support",
    config: {
        maxMessages: 30,
        summaryThreshold: 15,
        summaryModel: "gpt-4o-mini"
    }
)
```

**Isolation method**: Summaries stored per userId/conversationId

#### Session Memory

```java
// Automatic session-based isolation
memory = aiMemory( "session",
    key: "chatbot",
    userId: session.userId,
    conversationId: request.conversationId,
    config: { maxMessages: 20 }
)
```

**Isolation method**: Composite session key (key + userId + conversationId)

#### File Memory

```java
// File-based isolation with automatic naming
memory = aiMemory( "file",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: {
        directoryPath: "/app/memories",
        maxMessages: 50
    }
)
// Creates: /app/memories/[key]_user123_chat456.json
```

**Isolation method**: Separate JSON file per userId/conversationId

#### Cache Memory

```java
// Distributed cache with isolation
memory = aiMemory( "cache",
    key: "chat",
    userId: session.userId,
    conversationId: url.conversationId,
    config: {
        cacheName: "redis",
        cacheTimeout: 3600,
        maxMessages: 30
    }
)
```

**Isolation method**: Composite cache key (key + userId + conversationId)

#### JDBC Memory

```java
// Database with userId/conversationId columns
memory = aiMemory( "jdbc",
    key: createUUID(),
    userId: session.userId,
    conversationId: request.ticketId,
    config: {
        datasource: "mainDB",
        table: "ai_conversations",
        maxMessages: 100,
        autoCreate: true
    }
)
```

**Isolation method**: Database columns with WHERE filtering

**Table structure:**
```sql
CREATE TABLE ai_conversations (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(100),           -- Multi-tenant support
    conversation_id VARCHAR(100),   -- Multi-conversation support
    role VARCHAR(20),
    content TEXT,
    metadata TEXT,
    created_at TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_conversation (conversation_id),
    INDEX idx_tenant (user_id, conversation_id)  -- Composite index
);
```

---

## Vector Memory Multi-Tenancy

All 11 vector memory providers support multi-tenant isolation:

### BoxVector (In-Memory)

```java
memory = aiMemory( "boxvector",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: {
        collection: "dev_vectors",
        embeddingProvider: "openai"
    }
)
```

**Storage**: Metadata with in-memory filtering

### Chroma

```java
memory = aiMemory( "chroma",
    key: createUUID(),
    userId: "user123",
    conversationId: "support",
    config: {
        collection: "customer_support",
        embeddingProvider: "openai",
        host: "localhost",
        port: 8000
    }
)
```

**Storage**: Metadata with $and operator filtering

### PostgreSQL (pgvector)

```java
memory = aiMemory( "postgres",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: {
        collection: "ai_vectors",
        datasource: "mainDB",
        embeddingProvider: "openai",
        dimensions: 1536
    }
)
```

**Storage**: Dedicated VARCHAR(255) columns with composite index

### MySQL (9+ Native Vectors)

```java
memory = aiMemory( "mysql",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: {
        collection: "ai_vectors",
        datasource: "mainDB",
        embeddingProvider: "openai",
        dimensions: 1536
    }
)
```

**Storage**: Dedicated VARCHAR(255) columns with composite index

### TypeSense

```java
memory = aiMemory( "typesense",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: {
        collection: "fast_search",
        host: "localhost",
        port: 8108,
        apiKey: "xyz",
        embeddingProvider: "openai"
    }
)
```

**Storage**: Root-level fields with := filter syntax

### Pinecone

```java
memory = aiMemory( "pinecone",
    key: createUUID(),
    userId: "user123",
    conversationId: "prod",
    config: {
        collection: "production_vectors",
        apiKey: getSystemSetting( "PINECONE_API_KEY" ),
        environment: "us-west1-gcp",
        embeddingProvider: "openai"
    }
)
```

**Storage**: Metadata with $eq operators

### Qdrant

```java
memory = aiMemory( "qdrant",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: {
        collection: "high_perf_vectors",
        host: "localhost",
        port: 6333,
        embeddingProvider: "openai"
    }
)
```

**Storage**: Payload root-level fields with match filters

### Weaviate

```java
memory = aiMemory( "weaviate",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: {
        collection: "KnowledgeVectors",  // PascalCase
        host: "localhost",
        port: 8080,
        embeddingProvider: "openai"
    }
)
```

**Storage**: Properties root-level with GraphQL Equal operator

### Milvus

```java
memory = aiMemory( "milvus",
    key: createUUID(),
    userId: "user123",
    conversationId: "chat456",
    config: {
        collection: "enterprise_vectors",
        host: "localhost",
        port: 19530,
        embeddingProvider: "openai"
    }
)
```

**Storage**: Metadata with filter expressions

### Hybrid Memory

```java
memory = aiMemory( "hybrid",
    key: createUUID(),
    userId: "user123",
    conversationId: "support",
    config: {
        recentLimit: 5,
        semanticLimit: 5,
        vectorProvider: "chroma",
        vectorConfig: {
            collection: "hybrid_vectors",
            embeddingProvider: "openai"
        }
    }
)
```

**Storage**: Delegates to underlying vector provider (Chroma in this example)

---

## Security Considerations

### 1. Always Validate User Identifiers

Never trust client-provided userId/conversationId without server-side validation:

```java
// ❌ WRONG: Direct use of untrusted input
memory = aiMemory( "windowed",
    userId: url.userId,  // Client could manipulate this!
    config: { maxMessages: 10 }
)

// ✅ CORRECT: Server-side validation
function getUserMemory( required string requestedUserId ) {
    // Verify authenticated user matches requested user
    if ( session.user.id != arguments.requestedUserId ) {
        throw( type="SecurityViolation", message="Unauthorized access" );
    }

    return aiMemory( "windowed",
        userId: session.user.id,  // Use authenticated session
        config: { maxMessages: 10 }
    );
}
```

### 2. Use Session-Based UserId

Always derive userId from authenticated session:

```java
function getAuthenticatedMemory( required string conversationId ) {
    // Ensure user is authenticated
    if ( !session.keyExists( "user" ) || !session.user.isAuthenticated ) {
        throw( type="Unauthorized", message="Login required" );
    }

    return aiMemory( "session",
        key: "chat",
        userId: session.user.id,  // From authenticated session
        conversationId: arguments.conversationId,
        config: { maxMessages: 20 }
    );
}
```

### 3. Implement Authorization Checks

Verify user has permission to access specific conversations:

```java
function getConversationMemory(
    required string userId,
    required string conversationId
) {
    // Verify authenticated user matches userId
    if ( session.user.id != arguments.userId ) {
        throw( type="Unauthorized", message="Access denied" );
    }

    // Verify user owns this conversation
    var conversation = queryExecute(
        "SELECT user_id FROM conversations
         WHERE id = :conversationId AND user_id = :userId",
        {
            conversationId: arguments.conversationId,
            userId: arguments.userId
        }
    );

    if ( conversation.recordCount == 0 ) {
        throw( type="NotFound", message="Conversation not found" );
    }

    return aiMemory( "jdbc",
        key: createUUID(),
        userId: arguments.userId,
        conversationId: arguments.conversationId,
        config: {
            datasource: "mainDB",
            table: "ai_conversations"
        }
    );
}
```

### 4. Sanitize Identifiers

Prevent injection attacks by sanitizing userId/conversationId:

```java
function sanitizeIdentifier( required string input ) {
    // Allow only alphanumeric, hyphens, underscores
    return reReplace( arguments.input, "[^a-zA-Z0-9\-_]", "", "ALL" );
}

function getSafeMemory( required string userId, required string conversationId ) {
    return aiMemory( "file",
        key: createUUID(),
        userId: sanitizeIdentifier( arguments.userId ),
        conversationId: sanitizeIdentifier( arguments.conversationId ),
        config: {
            directoryPath: "/secure/memories",
            maxMessages: 50
        }
    );
}
```

### 5. Log Access for Auditing

Track who accesses which conversations:

```java
function getAuditedMemory(
    required string userId,
    required string conversationId
) {
    // Log access
    writeLog(
        type: "information",
        file: "memory-access",
        text: "User #arguments.userId# accessed conversation #arguments.conversationId# from IP #cgi.remote_addr#"
    );

    return aiMemory( "jdbc",
        key: createUUID(),
        userId: arguments.userId,
        conversationId: arguments.conversationId,
        config: {
            datasource: "mainDB",
            table: "ai_conversations"
        }
    );
}
```

### 6. Implement Rate Limiting

Prevent abuse by limiting conversation access:

```java
component {
    property name="accessCounts" default="{}";

    function getRateLimitedMemory( required string userId ) {
        // Track access count
        if ( !variables.accessCounts.keyExists( arguments.userId ) ) {
            variables.accessCounts[ arguments.userId ] = {
                count: 0,
                resetAt: dateAdd( "h", 1, now() )
            };
        }

        var userAccess = variables.accessCounts[ arguments.userId ];

        // Reset if expired
        if ( now() > userAccess.resetAt ) {
            userAccess.count = 0;
            userAccess.resetAt = dateAdd( "h", 1, now() );
        }

        // Check limit
        if ( userAccess.count >= 100 ) {
            throw( type="RateLimitExceeded", message="Too many requests" );
        }

        userAccess.count++;

        return aiMemory( "session",
            userId: arguments.userId,
            config: { maxMessages: 20 }
        );
    }
}
```

---

## Performance Optimization

### 1. Use Appropriate Memory Types

Choose memory types based on scale:

```java
// Small scale (< 100 users)
memory = aiMemory( "session",
    userId: session.user.id,
    config: { maxMessages: 20 }
)

// Medium scale (100-10,000 users)
memory = aiMemory( "cache",
    userId: session.user.id,
    config: {
        cacheName: "redis",
        maxMessages: 30
    }
)

// Large scale (> 10,000 users)
memory = aiMemory( "jdbc",
    userId: session.user.id,
    config: {
        datasource: "mainDB",
        table: "ai_conversations",
        maxMessages: 100
    }
)
```

### 2. Index Database Columns

For JDBC and vector memory, ensure proper indexing:

```sql
-- PostgreSQL / MySQL
CREATE INDEX idx_user_id ON ai_conversations(user_id);
CREATE INDEX idx_conversation_id ON ai_conversations(conversation_id);
CREATE INDEX idx_composite ON ai_conversations(user_id, conversation_id);

-- For vector tables
CREATE INDEX idx_vector_user ON ai_vectors(user_id);
CREATE INDEX idx_vector_conversation ON ai_vectors(conversation_id);
CREATE INDEX idx_vector_composite ON ai_vectors(user_id, conversation_id);
```

### 3. Implement Caching

Cache memory instances to avoid repeated creation:

```java
component {
    property name="memoryCache" default="{}";

    function getOptimizedMemory( required string userId, required string conversationId ) {
        var cacheKey = "#arguments.userId#:#arguments.conversationId#";

        if ( !variables.memoryCache.keyExists( cacheKey ) ) {
            variables.memoryCache[ cacheKey ] = aiMemory( "jdbc",
                key: createUUID(),
                userId: arguments.userId,
                conversationId: arguments.conversationId,
                config: {
                    datasource: "mainDB",
                    table: "ai_conversations",
                    maxMessages: 50
                }
            );
        }

        return variables.memoryCache[ cacheKey ];
    }
}
```

### 4. Cleanup Inactive Conversations

Periodically remove old conversations:

```java
function cleanupInactiveConversations( numeric daysInactive = 30 ) {
    var cutoffDate = dateAdd( "d", -arguments.daysInactive, now() );

    queryExecute(
        "DELETE FROM ai_conversations
         WHERE created_at < :cutoffDate",
        { cutoffDate: cutoffDate }
    );

    // Also cleanup vector memory if using database-backed provider
    queryExecute(
        "DELETE FROM ai_vectors
         WHERE created_at < :cutoffDate",
        { cutoffDate: cutoffDate }
    );
}
```

### 5. Use Connection Pooling

For database-backed memory, configure connection pooling:

```json
{
    "runtime": {
        "datasources": {
            "mainDB": {
                "driver": "mysql",
                "connectionString": "jdbc:mysql://localhost:3306/mydb",
                "username": "user",
                "password": "pass",
                "maxConnections": 50,
                "minConnections": 10,
                "connectionTimeout": 30000
            }
        }
    }
}
```

---

## Enterprise Patterns

### Pattern 1: Multi-Organization SaaS

Isolate by organization → user → conversation:

```java
component {
    function getEnterpriseMemory(
        required string organizationId,
        required string userId,
        required string conversationId
    ) {
        // Validate organization membership
        if ( !userBelongsToOrganization( arguments.userId, arguments.organizationId ) ) {
            throw( type="Unauthorized", message="User not in organization" );
        }

        // Use composite userId for complete isolation
        var isolatedUserId = "#arguments.organizationId#:#arguments.userId#";

        return aiMemory( "postgres",
            key: createUUID(),
            userId: isolatedUserId,
            conversationId: arguments.conversationId,
            config: {
                collection: "org_#arguments.organizationId#_vectors",
                datasource: "mainDB",
                embeddingProvider: "openai"
            }
        );
    }

    private boolean function userBelongsToOrganization(
        required string userId,
        required string organizationId
    ) {
        var result = queryExecute(
            "SELECT 1 FROM organization_users
             WHERE user_id = :userId AND organization_id = :organizationId",
            {
                userId: arguments.userId,
                organizationId: arguments.organizationId
            }
        );
        return result.recordCount > 0;
    }
}
```

### Pattern 2: Customer Support Ticketing

Map conversations to support tickets:

```java
component {
    function getTicketMemory( required string ticketId ) {
        // Get ticket details
        var ticket = getTicketById( arguments.ticketId );

        // Verify access
        if ( session.user.id != ticket.customerId &&
             !session.user.hasRole( "support" ) ) {
            throw( type="Unauthorized", message="Access denied" );
        }

        return aiMemory( "hybrid",
            key: createUUID(),
            userId: ticket.customerId,
            conversationId: arguments.ticketId,
            config: {
                recentLimit: 10,
                semanticLimit: 5,
                vectorProvider: "pinecone",
                vectorConfig: {
                    collection: "support_history",
                    embeddingProvider: "openai"
                }
            }
        );
    }

    function createTicket( required string customerId, required string subject ) {
        var ticketId = createUUID();

        queryExecute(
            "INSERT INTO support_tickets (id, customer_id, subject, created_at)
             VALUES (:id, :customerId, :subject, :createdAt)",
            {
                id: ticketId,
                customerId: arguments.customerId,
                subject: arguments.subject,
                createdAt: now()
            }
        );

        return getTicketMemory( ticketId );
    }
}
```

### Pattern 3: Departmental Isolation

Separate conversations by department:

```java
function getDepartmentMemory(
    required string userId,
    required string department
) {
    // Verify user is in department
    if ( !userInDepartment( arguments.userId, arguments.department ) ) {
        throw( type="Unauthorized", message="Not authorized for this department" );
    }

    return aiMemory( "chroma",
        key: createUUID(),
        userId: "#arguments.department#:#arguments.userId#",
        conversationId: "dept-chat",
        config: {
            collection: "dept_#arguments.department#_vectors",
            embeddingProvider: "openai"
        }
    );
}
```

---

## Migration Guide

### Migrating from Non-Multi-Tenant Memory

If you have existing non-multi-tenant memory implementations:

#### Step 1: Identify Current Usage

```java
// Old (single-tenant)
memory = aiMemory( "windowed", { maxMessages: 10 } )
agent = aiAgent( name: "Assistant", memory: memory )
```

#### Step 2: Add UserId Parameter

```java
// New (multi-tenant)
memory = aiMemory( "windowed",
    key: createUUID(),
    userId: session.user.id,  // Add user identifier
    config: { maxMessages: 10 }
)
agent = aiAgent( name: "Assistant", memory: memory )
```

#### Step 3: Update Existing Data

For database-backed memory (JDBC, Postgres, MySQL):

```sql
-- Add columns if missing
ALTER TABLE ai_conversations ADD COLUMN user_id VARCHAR(100);
ALTER TABLE ai_conversations ADD COLUMN conversation_id VARCHAR(100);

-- Migrate existing data (example: assign to default user)
UPDATE ai_conversations
SET user_id = 'legacy-user',
    conversation_id = 'default'
WHERE user_id IS NULL;

-- Add indexes
CREATE INDEX idx_user_id ON ai_conversations(user_id);
CREATE INDEX idx_conversation_id ON ai_conversations(conversation_id);
```

#### Step 4: Update Application Code

```java
// Before
function chat( message ) {
    var memory = aiMemory( "windowed", { maxMessages: 10 } );
    var agent = aiAgent( name: "Bot", memory: memory );
    return agent.run( message );
}

// After
function chat( userId, conversationId, message ) {
    var memory = aiMemory( "windowed",
        key: createUUID(),
        userId: arguments.userId,
        conversationId: arguments.conversationId,
        config: { maxMessages: 10 }
    );
    var agent = aiAgent( name: "Bot", memory: memory );
    return agent.run( message );
}
```

---

## Troubleshooting

### Issue: Users Seeing Other Users' Conversations

**Symptoms:**
- User A sees User B's conversation history
- Conversations mixing between users

**Solution:**
```java
// Ensure you're passing userId correctly
memory = aiMemory( "windowed",
    key: createUUID(),
    userId: session.user.id,  // ✅ Use authenticated session
    config: { maxMessages: 10 }
)

// NOT this:
// userId: url.userId  ❌ Never trust client input
```

### Issue: Conversations Not Isolated Within User

**Symptoms:**
- User's different chat windows share history
- Multiple conversations bleeding together

**Solution:**
```java
// Add conversationId for isolation
memory = aiMemory( "windowed",
    key: createUUID(),
    userId: session.user.id,
    conversationId: request.chatId,  // ✅ Add conversation identifier
    config: { maxMessages: 10 }
)
```

### Issue: Poor Performance with Many Users

**Symptoms:**
- Slow memory retrieval
- Database queries timing out

**Solutions:**

1. **Add database indexes:**
```sql
CREATE INDEX idx_composite ON ai_conversations(user_id, conversation_id);
```

2. **Use cache-based memory:**
```java
memory = aiMemory( "cache",
    userId: session.user.id,
    config: {
        cacheName: "redis",
        maxMessages: 30
    }
)
```

3. **Implement memory pooling:**
```java
// Cache memory instances
if ( !application.memoryPool.keyExists( userId ) ) {
    application.memoryPool[ userId ] = aiMemory( "windowed",
        userId: userId,
        config: { maxMessages: 10 }
    );
}
return application.memoryPool[ userId ];
```

### Issue: Memory Leakage Between Tenants

**Symptoms:**
- Data from other users occasionally appears
- Intermittent cross-contamination

**Solution:**
```java
// Ensure you're creating NEW memory instances, not reusing
function getUserMemory( userId ) {
    // ❌ WRONG: Reusing same instance
    // return variables.sharedMemory;

    // ✅ CORRECT: New instance per user
    return aiMemory( "session",
        key: createUUID(),  // Unique key per instance
        userId: arguments.userId,
        config: { maxMessages: 20 }
    );
}
```

### Issue: Export/Import Losing Tenant Information

**Symptoms:**
- Imported conversations lose userId/conversationId
- Restored memory shows wrong owner

**Solution:**
```java
// Export preserves identifiers
exported = memory.export();
// {
//     userId: "user123",
//     conversationId: "chat456",
//     messages: [...]
// }

// Import restores identifiers
newMemory = aiMemory( "windowed", {
    maxMessages: 10
});
newMemory.import( exported );  // Preserves userId/conversationId

// Verify restoration
println( newMemory.getUserId() );  // "user123"
println( newMemory.getConversationId() );  // "chat456"
```

---

## See Also

- [Memory Systems Guide](../main-components/memory.md) - Standard conversation memory
- [Vector Memory Guide](../main-components/vector-memory.md) - Semantic search with isolation
- [Agents Documentation](../main-components/agents.md) - Using memory in agents
- [Security Best Practices](../security/best-practices.md) - Application security
- [Examples](../../examples/advanced/) - Complete working examples

---

**Need Help?** Join the [BoxLang Discord](https://discord.gg/boxlang) or check the [GitHub Discussions](https://github.com/ortus-boxlang/bx-ai/discussions) for community support.
