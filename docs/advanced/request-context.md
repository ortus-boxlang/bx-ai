# 🔐 Message Context

The BoxLang AI module provides a powerful context system for AI messages that allows you to inject security information, RAG (Retrieval Augmented Generation) data, and other contextual information into your AI operations.

## 📋 Table of Contents

- [Overview](#overview)
- [Adding Context](#adding-context)
- [Convention-Based Injection](#convention-based-injection)
- [Common Use Cases](#common-use-cases)
- [Best Practices](#best-practices)

---

## 🔍 Overview

The context system provides a dedicated struct property on `AiMessage` where you can store:

- **Security context**: User roles, permissions, tenant IDs, authentication tokens
- **RAG context**: Retrieved documents, embeddings, search results
- **Application context**: Request metadata, session info, environment data

Context is injected into messages using the existing binding system via the `render()` method. The `${context}` placeholder is automatically replaced with JSON-serialized context data.

```
Context Flow:
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   aiMessage()                                                       │
│        │                                                            │
│        ├── .addContext() / .setContext() / .mergeContext()         │
│        │                                                            │
│        ▼                                                            │
│   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐        │
│   │  AiMessage  │ ──▶  │  render()   │ ──▶  │  Messages   │        │
│   │  + context  │      │  ${context} │      │  (bound)    │        │
│   └─────────────┘      └─────────────┘      └─────────────┘        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📥 Adding Context

### Fluent API (Recommended)

Build context incrementally using chaining:

```javascript
message = aiMessage( "Help me with my account" )
    .addContext( "userId", session.userId )
    .addContext( "tenantId", session.tenantId )
    .addContext( "permissions", userService.getPermissions( session.userId ) )
    .mergeContext({
        ragDocuments: ragService.searchDocuments( "account help" ),
        sessionMetadata: {
            ip: getClientIP(),
            userAgent: getUserAgent(),
            timestamp: now()
        }
    });
```

### Set Entire Context

Replace the entire context at once:

```javascript
message = aiMessage( "Query" )
    .setContext({
        userId: "user-123",
        environment: "production",
        featureFlags: ["beta-features", "new-ui"]
    });
```

### Access Context

```javascript
// Check for context
if ( message.hasContext() ) {
    println( "Context is available" );
}

// Get entire context
context = message.getContext();

// Get specific values with defaults
userId = message.getContextValue( "userId", "anonymous" );
tenantId = message.getContextValue( "tenantId", "default" );
```

---

## 🎯 Convention-Based Injection

The simplest way to inject context into your messages is using the `${context}` placeholder. When `render()` is called, this placeholder is automatically replaced with the JSON-serialized context data.

### Basic Usage

```javascript
// Context is automatically injected where ${context} appears
message = aiMessage( "You are a helpful assistant. User context: ${context}. Please help the user." )
    .setContext({
        userId: "user-123",
        role: "premium",
        preferences: { language: "en", tone: "friendly" }
    });

// Render applies the bindings
renderedMessages = message.render();
```

The rendered message will contain:
```
You are a helpful assistant. User context: {"userId":"user-123","role":"premium","preferences":{"language":"en","tone":"friendly"}}. Please help the user.
```

### With System Messages

```javascript
message = aiMessage()
    .system( "You are a customer service AI. Customer data: ${context}" )
    .user( "What's my order status?" )
    .setContext({
        customerId: "C-12345",
        name: "John Doe",
        recentOrders: [
            { id: "ORD-001", status: "shipped" },
            { id: "ORD-002", status: "processing" }
        ]
    });

renderedMessages = message.render();
```

### Combining with Other Bindings

Context works seamlessly with other bindings:

```javascript
message = aiMessage( "Hello ${name}, your context is: ${context}" )
    .bind( { name: "John" } )
    .setContext( { role: "admin", permissions: ["read", "write"] } );

renderedMessages = message.render();
// Result: "Hello John, your context is: {"role":"admin","permissions":["read","write"]}"
```

### RAG Pattern

```javascript
// Retrieve relevant documents
relevantDocs = vectorStore.search( query: userQuestion, limit: 3 );

message = aiMessage()
    .system("""
        Use the following context to answer the question:
        ${context}
        
        Answer based only on the provided context.
    """)
    .user( userQuestion )
    .setContext({
        documents: relevantDocs.map( doc => doc.content ),
        sources: relevantDocs.map( doc => doc.metadata.source )
    });

renderedMessages = message.render();
```

### No Placeholder = No Injection

If your message doesn't contain `${context}`, the context is still available on the message object but won't be injected into the message content:

```javascript
// Context is available but not injected (no ${context} in message)
message = aiMessage( "Hello, how are you?" )
    .setContext({ userId: "user-123" });

// Context is still accessible
userId = message.getContextValue( "userId" );  // "user-123"

// But render won't change the message
renderedMessages = message.render();  // "Hello, how are you?"
```

---

## 💡 Common Use Cases

### 1. RAG (Retrieval Augmented Generation)

```javascript
// Search for relevant documents
relevantDocs = vectorStore.search(
    query: userQuestion,
    limit: 5,
    threshold: 0.7
);

message = aiMessage()
    .system( "Use this context to answer: ${context}" )
    .user( userQuestion )
    .setContext({
        ragDocuments: relevantDocs.map( doc => doc.content ),
        ragMetadata: relevantDocs.map( doc => { source: doc.source, score: doc.score } )
    });

response = aiChat( message.render() );
```

### 2. Secure Multi-User Application

```javascript
// In your controller/handler
function askAI( required string question ) {
    var user = getAuthenticatedUser();

    var message = aiMessage()
        .system( "You are an assistant. User context: ${context}" )
        .user( arguments.question )
        .setContext({
            userId: user.id,
            tenantId: user.tenantId,
            permissions: user.permissions,
            department: user.department,
            subscriptionTier: user.subscription.tier
        });

    return aiChat( message.render() );
}
```

### 3. Contextual Customer Support

```javascript
// Load customer context
customer = customerService.get( customerId );
recentTickets = supportService.getRecentTickets( customerId );
orderHistory = orderService.getHistory( customerId );

message = aiMessage()
    .system( "You are a customer support AI. Customer data: ${context}" )
    .user( customerQuestion )
    .setContext({
        customerId: customer.id,
        customerName: customer.name,
        accountType: customer.type,
        recentTickets: recentTickets,
        orderHistory: orderHistory
    });

response = aiChat( message.render() );
```

---

## ✅ Best Practices

### 1. Don't Send Sensitive Data to AI

Context is sent to the AI provider. Be careful what gets injected:

```javascript
// ❌ Bad: Injecting sensitive data directly
.setContext({
    userPassword: user.password,  // Never!
    apiKey: config.apiKey,        // Never!
    ssn: user.ssn                 // Never!
})

// ✅ Good: Use IDs and references
.setContext({
    userId: user.id,
    hasVerifiedEmail: user.emailVerified,
    subscriptionTier: user.subscription.tier
})
```

### 2. Keep Context Lightweight

Don't overload context with large data:

```javascript
// ❌ Bad: Large data in context
.setContext({
    allDocuments: fileRead( "/path/to/huge/file.txt" ),  // Could be MB of data!
    entireDatabase: queryExecute( "SELECT * FROM everything" )
})

// ✅ Good: References and summaries
.setContext({
    documentIds: ["doc1", "doc2", "doc3"],
    relevantExcerpts: getRelevantExcerpts( userQuery, 500 )  // Limited size
})
```

### 3. Use Typed Context Keys

Establish conventions for context keys:

```javascript
// Define standard keys
static {
    CONTEXT_USER_ID = "userId";
    CONTEXT_TENANT_ID = "tenantId";
    CONTEXT_PERMISSIONS = "permissions";
    CONTEXT_RAG_DOCS = "ragDocuments";
}

// Use consistently
message.addContext( CONTEXT_USER_ID, user.id )
       .addContext( CONTEXT_TENANT_ID, tenant.id );
```

---

## 🔗 Related Documentation

- **[AiMessage Documentation](../main-components/messages.md)** - Full message builder documentation
- **[Event System](events.md)** - Interceptor documentation
- **[Embeddings](embeddings.md)** - Generate embeddings for RAG

---

**Copyright** © 2023-2025 Ortus Solutions, Corp
