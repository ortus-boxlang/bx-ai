# 🔐 Message Context

The BoxLang AI module provides a powerful context system for AI messages that allows you to inject security information, RAG (Retrieval Augmented Generation) data, and other contextual information into your AI operations.

## 📋 Table of Contents

- [Overview](#overview)
- [Using Context with aiChat()](#using-context-with-aichat)
- [Using Context with AiMessage](#using-context-with-aimessage)
- [Using Context with Runnable Pipelines](#using-context-with-runnable-pipelines)
- [Using Context with Agents](#using-context-with-agents)
- [Common Use Cases](#common-use-cases)
- [Best Practices](#best-practices)

---

## 🔍 Overview

The context system provides a way to inject contextual data into AI messages:

- **Security context**: User roles, permissions, tenant IDs, authentication tokens
- **RAG context**: Retrieved documents, embeddings, search results
- **Application context**: Request metadata, session info, environment data

The `${context}` placeholder in messages is automatically replaced with JSON-serialized context data.

---

## 🚀 Using Context with aiChat()

The simplest way to use context is directly with `aiChat()`:

```javascript
// Pass context in options
response = aiChat(
    "You are an assistant. User context: ${context}. Help the user.",
    {},
    {
        context: {
            userId: "user-123",
            role: "admin",
            permissions: ["read", "write"]
        }
    }
);
```

The `${context}` placeholder is replaced with the JSON-serialized context data before sending to the AI provider.

### Without Placeholder

If you don't use `${context}` in your message, the context is still available for interceptors but won't be automatically injected:

```javascript
// Context available for interceptors, but not injected into message
response = aiChat(
    "Hello, how are you?",
    {},
    { context: { userId: "user-123" } }
);
```

---

## 📝 Using Context with AiMessage

For more control, use `AiMessage` directly with fluent context methods:

### Fluent API

```javascript
message = aiMessage( "Help me with my account. Context: ${context}" )
    .addContext( "userId", session.userId )
    .addContext( "tenantId", session.tenantId )
    .mergeContext({
        ragDocuments: ragService.searchDocuments( "account help" ),
        sessionMetadata: {
            timestamp: now()
        }
    });

// Render applies the context binding
response = aiChat( message.render() );
```

### Set Entire Context

```javascript
message = aiMessage( "Query with context: ${context}" )
    .setContext({
        userId: "user-123",
        environment: "production",
        featureFlags: ["beta-features", "new-ui"]
    });

response = aiChat( message.render() );
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

response = aiChat( message.render() );
```

---

## 🔄 Using Context with Runnable Pipelines

Context works seamlessly with `AiModel` in runnable pipelines:

### Direct Model Invocation

```javascript
// Create a model
model = aiModel( "openai" );

// Run with context in options
response = model.run(
    "You are an assistant. User info: ${context}. Answer the question.",
    {},
    {
        context: {
            userId: "user-123",
            subscription: "premium"
        }
    }
);
```

### Pipeline with Context

```javascript
// Build a pipeline
pipeline = aiModel( "openai" )
    .to( aiTransform( response => response.toUpperCase() ) );

// Run with context
response = pipeline.run(
    "Context: ${context}. What should I do?",
    {},
    {
        context: {
            task: "review code",
            deadline: "today"
        }
    }
);
```

### With AiMessage in Pipelines

```javascript
// AiMessage with context flows through pipeline
message = aiMessage( "Context: ${context}. Help me." )
    .setContext({ userId: "user-123" });

// Model extracts and renders messages with context
response = aiModel( "openai" ).run( message );
```

---

## 🤖 Using Context with Agents

Context is fully supported in AI agents:

### Basic Agent with Context

```javascript
// Create an agent
agent = aiAgent(
    name: "SupportBot",
    instructions: "You are a support agent. User context: ${context}"
);

// Run with context
response = agent.run(
    "Help me with my order",
    {},
    {
        context: {
            userId: "user-123",
            orderHistory: ["ORD-001", "ORD-002"],
            subscriptionTier: "premium"
        }
    }
);
```

### Agent with RAG Context

```javascript
// Search for relevant documents
relevantDocs = vectorStore.search( query: userQuestion, limit: 5 );

// Create agent with RAG
agent = aiAgent(
    name: "KnowledgeBot",
    instructions: """
        You are a knowledge assistant.
        Use only this context to answer: ${context}
        If the answer is not in the context, say you don't know.
    """
);

// Run with RAG context
response = agent.run(
    userQuestion,
    {},
    {
        context: {
            documents: relevantDocs.map( doc => doc.content ),
            sources: relevantDocs.map( doc => doc.metadata.source )
        }
    }
);
```

### Agent Streaming with Context

```javascript
agent = aiAgent(
    name: "StreamBot",
    instructions: "User info: ${context}. Be helpful."
);

agent.stream(
    onChunk: chunk => print( chunk.content ),
    input: "Tell me about my account",
    options: {
        context: {
            userId: "user-123",
            accountType: "business"
        }
    }
);
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

response = aiChat(
    "Use this context to answer: ${context}. Question: " & userQuestion,
    {},
    {
        context: {
            ragDocuments: relevantDocs.map( doc => doc.content ),
            ragMetadata: relevantDocs.map( doc => { source: doc.source, score: doc.score } )
        }
    }
);
```

### 2. Secure Multi-Tenant Application

```javascript
// In your controller/handler
function askAI( required string question ) {
    var user = getAuthenticatedUser();

    return aiChat(
        "You are an assistant. User context: ${context}. " & arguments.question,
        {},
        {
            context: {
                userId: user.id,
                tenantId: user.tenantId,
                permissions: user.permissions,
                department: user.department,
                subscriptionTier: user.subscription.tier
            }
        }
    );
}
```

### 3. Contextual Customer Support with Agent

```javascript
// Load customer context
customer = customerService.get( customerId );

agent = aiAgent(
    name: "SupportAgent",
    instructions: "You are a customer support AI. Customer data: ${context}"
);

response = agent.run(
    customerQuestion,
    {},
    {
        context: {
            customerId: customer.id,
            customerName: customer.name,
            accountType: customer.type,
            recentTickets: supportService.getRecentTickets( customerId ),
            orderHistory: orderService.getHistory( customerId )
        }
    }
);
```

---

## ✅ Best Practices

### 1. Don't Send Sensitive Data to AI

Context is sent to the AI provider. Be careful what gets injected:

```javascript
// ❌ Bad: Injecting sensitive data directly
context: {
    userPassword: user.password,  // Never!
    apiKey: config.apiKey,        // Never!
    ssn: user.ssn                 // Never!
}

// ✅ Good: Use IDs and references
context: {
    userId: user.id,
    hasVerifiedEmail: user.emailVerified,
    subscriptionTier: user.subscription.tier
}
```

### 2. Keep Context Lightweight

Don't overload context with large data:

```javascript
// ❌ Bad: Large data in context
context: {
    allDocuments: fileRead( "/path/to/huge/file.txt" ),
    entireDatabase: queryExecute( "SELECT * FROM everything" )
}

// ✅ Good: References and summaries
context: {
    documentIds: ["doc1", "doc2", "doc3"],
    relevantExcerpts: getRelevantExcerpts( userQuery, 500 )
}
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
context: {
    "#CONTEXT_USER_ID#": user.id,
    "#CONTEXT_TENANT_ID#": tenant.id
}
```

---

## 🔗 Related Documentation

- **[AiMessage Documentation](../main-components/messages.md)** - Full message builder documentation
- **[Agents Documentation](../main-components/agents.md)** - AI Agent documentation
- **[Pipelines Documentation](../main-components/pipelines.md)** - Runnable pipelines documentation
- **[Event System](events.md)** - Interceptor documentation

---

**Copyright** © 2023-2025 Ortus Solutions, Corp
