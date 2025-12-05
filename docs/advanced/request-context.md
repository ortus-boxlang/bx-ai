# 🔐 Request Context

The BoxLang AI module provides a powerful context system for AI requests that allows you to inject security information, RAG (Retrieval Augmented Generation) data, and other contextual information into your AI operations.

## 📋 Table of Contents

- [Overview](#overview)
- [Adding Context](#adding-context)
- [Accessing Context](#accessing-context)
- [Using Context with Interceptors](#using-context-with-interceptors)
- [Common Use Cases](#common-use-cases)
- [Best Practices](#best-practices)

---

## 🔍 Overview

The context system provides a dedicated struct property on AI requests where you can store:

- **Security context**: User roles, permissions, tenant IDs, authentication tokens
- **RAG context**: Retrieved documents, embeddings, search results
- **Application context**: Request metadata, session info, environment data

Context is **not automatically injected** into messages sent to the AI provider. Instead, it flows through the request lifecycle and is accessible via interceptors, giving you complete control over how context is used:

```
Context Flow:
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   aiChat/aiChatRequest                                              │
│        │                                                            │
│        ▼                                                            │
│   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐        │
│   │  AiRequest  │ ──▶  │ Interceptors │ ──▶  │  AI Provider│        │
│   │  + context  │      │ (access &   │      │  (messages) │        │
│   └─────────────┘      │  modify)    │      └─────────────┘        │
│                        └─────────────┘                              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📥 Adding Context

### Via Options (Recommended)

Pass context directly when making AI calls:

```javascript
// Simple chat with context
response = aiChat(
    "What products can I order?",
    {},
    {
        context: {
            userId: "user-123",
            tenantId: "acme-corp",
            roles: ["customer", "premium"],
            ragDocuments: [
                "Available products: Widget A, Widget B, Widget C"
            ]
        }
    }
);

// Creating a request object with context
request = aiChatRequest(
    messages: "Summarize my orders",
    options: {
        context: {
            userId: session.userId,
            orderHistory: orderService.getRecentOrders( session.userId )
        }
    }
);
```

### Fluent API

Build context incrementally using chaining:

```javascript
request = aiChatRequest( messages: "Help me with my account" )
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
request = aiChatRequest( messages: "Query" )
    .setContext({
        userId: "user-123",
        environment: "production",
        featureFlags: ["beta-features", "new-ui"]
    });
```

---

## 📤 Accessing Context

### Check for Context

```javascript
request = aiChatRequest( messages: "Hello", options: { context: { userId: "123" } } );

if ( request.hasContext() ) {
    println( "Context is available" );
}
```

### Get Entire Context

```javascript
context = request.getContext();
// Returns: { userId: "123", tenantId: "acme", ... }
```

### Get Specific Values

```javascript
// Get with default value
userId = request.getContextValue( "userId", "anonymous" );
tenantId = request.getContextValue( "tenantId", "default" );

// Check and use
permissions = request.getContextValue( "permissions", [] );
if ( permissions.contains( "admin" ) ) {
    // Admin-specific logic
}
```

---

## 🔌 Using Context with Interceptors

The real power of context comes from interceptors. Context is accessible throughout the request lifecycle, allowing you to inject content, validate permissions, and customize behavior.

### Injecting RAG Content

The most common use case is injecting retrieved documents into the AI conversation:

```javascript
// interceptors/RAGInterceptor.bx
class {

    function onAIRequest( event, interceptData ) {
        var aiRequest = interceptData.aiRequest;
        var context = aiRequest.getContext();

        // Check if we have RAG documents to inject
        if ( context.keyExists( "ragDocuments" ) && !context.ragDocuments.isEmpty() ) {
            var messages = aiRequest.getMessages();

            // Build RAG context message
            var ragContent = "Use the following context to answer the user's question:" & chr(10) & chr(10);
            for ( var doc in context.ragDocuments ) {
                ragContent &= "---" & chr(10) & doc & chr(10);
            }
            ragContent &= "---" & chr(10) & chr(10) & "Answer based only on the context above.";

            // Prepend as system message or inject into existing system message
            if ( messages.len() > 0 && messages[1].role == "system" ) {
                messages[1].content = ragContent & chr(10) & chr(10) & messages[1].content;
            } else {
                arrayPrepend( messages, { role: "system", content: ragContent } );
            }

            aiRequest.setMessages( messages );
        }
    }
}
```

### Security Validation

Validate user permissions before allowing AI operations:

```javascript
// interceptors/SecurityInterceptor.bx
class {

    function onAIRequest( event, interceptData ) {
        var aiRequest = interceptData.aiRequest;
        var context = aiRequest.getContext();

        // Require authentication
        if ( !context.keyExists( "userId" ) || context.userId.isEmpty() ) {
            throw( type: "SecurityViolation", message: "Authentication required for AI operations" );
        }

        // Check permissions
        var permissions = context.keyExists( "permissions" ) ? context.permissions : [];
        if ( !permissions.contains( "ai.chat" ) ) {
            throw( type: "SecurityViolation", message: "User lacks permission for AI chat" );
        }

        // Rate limiting per user
        var userId = context.userId;
        if ( isRateLimited( userId ) ) {
            throw( type: "RateLimitExceeded", message: "Too many AI requests. Please wait." );
        }

        incrementRequestCount( userId );
    }
}
```

### Multi-Tenant Isolation

Use context for tenant-specific behavior:

```javascript
// interceptors/TenantInterceptor.bx
class {

    function onAIRequest( event, interceptData ) {
        var aiRequest = interceptData.aiRequest;
        var context = aiRequest.getContext();
        var tenantId = context.keyExists( "tenantId" ) ? context.tenantId : "default";

        // Load tenant-specific configuration
        var tenantConfig = getTenantConfig( tenantId );

        // Override model based on tenant subscription
        if ( tenantConfig.plan == "enterprise" ) {
            aiRequest.getParams().model = "gpt-4";
        } else {
            aiRequest.getParams().model = "gpt-3.5-turbo";
        }

        // Add tenant-specific system instructions
        var messages = aiRequest.getMessages();
        if ( tenantConfig.keyExists( "systemPrompt" ) ) {
            arrayPrepend( messages, {
                role: "system",
                content: tenantConfig.systemPrompt
            });
            aiRequest.setMessages( messages );
        }

        // Track usage per tenant
        trackTenantUsage( tenantId, "chat_request" );
    }

    function onAIResponse( event, interceptData ) {
        var context = interceptData.aiRequest.getContext();
        var tenantId = context.keyExists( "tenantId" ) ? context.tenantId : "default";

        // Track token usage per tenant
        if ( interceptData.response.keyExists( "usage" ) ) {
            trackTenantTokens( tenantId, interceptData.response.usage.total_tokens );
        }
    }
}
```

### Personalization

Customize AI behavior based on user preferences:

```javascript
// interceptors/PersonalizationInterceptor.bx
class {

    function beforeAIModelInvoke( event, interceptData ) {
        var aiRequest = interceptData.aiRequest;
        var context = aiRequest.getContext();

        // Get user preferences
        var preferences = context.keyExists( "userPreferences" ) ? context.userPreferences : {};

        // Adjust temperature based on preference
        if ( preferences.keyExists( "creativity" ) ) {
            var params = aiRequest.getParams();
            params.temperature = preferences.creativity == "high" ? 0.9 : 0.3;
        }

        // Add personalized system context
        var messages = aiRequest.getMessages();
        var personalContext = "";

        if ( preferences.keyExists( "language" ) ) {
            personalContext &= "Respond in #preferences.language#. ";
        }
        if ( preferences.keyExists( "expertise" ) ) {
            personalContext &= "Assume #preferences.expertise# level expertise. ";
        }
        if ( preferences.keyExists( "tone" ) ) {
            personalContext &= "Use a #preferences.tone# tone. ";
        }

        if ( personalContext.len() > 0 ) {
            arrayPrepend( messages, { role: "system", content: personalContext.trim() } );
            aiRequest.setMessages( messages );
        }
    }
}
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

// Pass to AI with context
response = aiChat(
    userQuestion,
    {},
    {
        context: {
            ragDocuments: relevantDocs.map( doc => doc.content ),
            ragMetadata: relevantDocs.map( doc => { source: doc.source, score: doc.score } )
        }
    }
);
```

### 2. Secure Multi-User Application

```javascript
// In your controller/handler
function askAI( required string question ) {
    var user = getAuthenticatedUser();

    return aiChat(
        arguments.question,
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

### 3. Contextual Customer Support

```javascript
// Load customer context
customer = customerService.get( customerId );
recentTickets = supportService.getRecentTickets( customerId );
orderHistory = orderService.getHistory( customerId );

response = aiChat(
    customerQuestion,
    {},
    {
        context: {
            customerId: customer.id,
            customerName: customer.name,
            accountType: customer.type,
            ragDocuments: [
                "Customer Details: Name=#customer.name#, Account Type=#customer.type#, Since=#customer.createdAt#",
                "Recent Support Tickets: " & serializeJSON( recentTickets ),
                "Recent Orders: " & serializeJSON( orderHistory )
            ],
            instructions: "You are a helpful customer support agent. Use the customer context to provide personalized assistance."
        }
    }
);
```

### 4. Code Assistant with Project Context

```javascript
// Get project context
projectFiles = fileService.listFiles( projectPath );
dependencies = packageManager.getDependencies( projectPath );
recentChanges = gitService.getRecentCommits( projectPath, 5 );

response = aiChat(
    developerQuestion,
    {},
    {
        context: {
            projectType: "boxlang-module",
            projectFiles: projectFiles,
            dependencies: dependencies,
            recentChanges: recentChanges,
            codingStandards: getTeamCodingStandards()
        }
    }
);
```

---

## ✅ Best Practices

### 1. Don't Send Sensitive Data to AI

Context is for **your application's use**. Be careful what gets injected into messages:

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

### 2. Validate Context in Interceptors

Always validate context data before using it:

```javascript
function onAIRequest( event, interceptData ) {
    var context = interceptData.aiRequest.getContext();

    // Validate required fields
    if ( !context.keyExists( "tenantId" ) ) {
        throw( "tenantId is required in context" );
    }

    // Sanitize data
    if ( context.keyExists( "ragDocuments" ) ) {
        context.ragDocuments = context.ragDocuments.map( doc => sanitizeContent( doc ) );
    }
}
```

### 3. Keep Context Lightweight

Don't overload context with large data:

```javascript
// ❌ Bad: Large data in context
context: {
    allDocuments: fileRead( "/path/to/huge/file.txt" ),  // Could be MB of data!
    entireDatabase: queryExecute( "SELECT * FROM everything" )
}

// ✅ Good: References and summaries
context: {
    documentIds: ["doc1", "doc2", "doc3"],
    relevantExcerpts: getRelevantExcerpts( userQuery, 500 )  // Limited size
}
```

### 4. Use Typed Context Keys

Establish conventions for context keys:

```javascript
// Define standard keys
static {
    CONTEXT_USER_ID = "userId";
    CONTEXT_TENANT_ID = "tenantId";
    CONTEXT_PERMISSIONS = "permissions";
    CONTEXT_RAG_DOCS = "ragDocuments";
    CONTEXT_RAG_METADATA = "ragMetadata";
}

// Use consistently
request.addContext( CONTEXT_USER_ID, user.id )
       .addContext( CONTEXT_TENANT_ID, tenant.id );
```

### 5. Document Your Context Schema

Create documentation for your application's context structure:

```javascript
/**
 * AI Request Context Schema
 *
 * Security Context:
 * - userId (string): Authenticated user ID
 * - tenantId (string): Multi-tenant organization ID
 * - permissions (array): User permission strings
 * - roles (array): User role names
 *
 * RAG Context:
 * - ragDocuments (array): Retrieved document contents
 * - ragMetadata (array): Source and score info for each document
 * - searchQuery (string): Original search query used
 *
 * Application Context:
 * - requestId (string): Unique request identifier
 * - environment (string): dev/staging/production
 * - featureFlags (array): Enabled feature flags
 */
```

---

## 🔗 Related Documentation

- **[Event System](events.md)** - Full interceptor documentation
- **[Embeddings](embeddings.md)** - Generate embeddings for RAG
- **[Multi-Tenant Memory](multi-tenant-memory.md)** - Tenant isolation patterns

---

**Copyright** © 2023-2025 Ortus Solutions, Corp
