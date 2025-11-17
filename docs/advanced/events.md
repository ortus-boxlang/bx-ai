# Event System

The BoxLang AI module provides a comprehensive event system that allows you to intercept, monitor, and customize AI operations at various stages. These events give you fine-grained control over the AI lifecycle, from object creation to request/response handling.

## Table of Contents

- [Overview](#overview)
- [Event Interception](#event-interception)
- [Available Events](#available-events)
- [Event Arguments](#event-arguments)
- [Common Use Cases](#common-use-cases)
- [Examples](#examples)
- [Best Practices](#best-practices)

---

## Overview

The event system allows you to:

- **Monitor**: Log and track AI operations
- **Modify**: Change requests, responses, or configurations
- **Validate**: Check inputs and outputs
- **Audit**: Track usage and costs
- **Secure**: Add authentication and authorization
- **Customize**: Extend behavior without modifying core code

### Event Lifecycle

```
Object Creation Events:
  onAIMessageCreate → onAIRequestCreate → onAIServiceCreate → onAIModelCreate → onAITransformCreate

Request/Response Events:
  beforeAIModelInvoke → onAIRequest → [AI Provider] → onAIResponse → afterAIModelInvoke

Provider Events:
  onAIProviderRequest → onAIProviderCreate
```

---

## Event Interception

To listen to events, create an interceptor and register it in your module or application configuration.

### Creating an Interceptor

```java
// interceptors/AIMonitor.bx
class {

    function configure() {
        // Interceptor configuration
    }

    function onAIRequest( event, interceptData ) {
        // Your event handling logic
    }

    function onAIResponse( event, interceptData ) {
        // Your event handling logic
    }
}
```

### Registering an Interceptor

**In `ModuleConfig.bx`:**

```java
function configure() {
    interceptors = [
        {
            class: "interceptors.AIMonitor",
            properties: {}
        }
    ];
}
```

**In Application.bx:**

```java
this.interceptors = [
    { class: "path.to.AIMonitor" }
];
```

---

## Available Events

### 1. onAIMessageCreate

Fired when an AI message object is created via `aiMessage()`.

**When**: Message template creation
**Frequency**: Once per `aiMessage()` call

#### Event Data

```java
{
    message: AiMessage  // The created message object
}
```

#### Example

```java
function onAIMessageCreate( event, interceptData ) {
    var message = interceptData.message;

    // Log message creation
    writeLog(
        text: "AI Message created with #arrayLen( message.getMessages() )# messages",
        type: "info"
    );

    // Add default system message if none exists
    var messages = message.getMessages();
    if ( messages.isEmpty() || messages[1].role != "system" ) {
        message.system( "You are a helpful assistant" );
    }
}
```

---

### 2. onAIRequestCreate

Fired when an AI request object is created via `aiChatRequest()`.

**When**: Request object instantiation
**Frequency**: Once per `aiChatRequest()` call

#### Event Data

```java
{
    aiRequest: AiRequest  // The created request object
}
```

#### Example

```java
function onAIRequestCreate( event, interceptData ) {
    var request = interceptData.aiRequest;

    // Add tracking metadata
    request.setParams({
        user: getAuthenticatedUser(),
        requestId: createUUID(),
        timestamp: now()
    });

    // Apply organization-wide defaults
    if ( !request.getParams().keyExists( "temperature" ) ) {
        request.setParam( "temperature", 0.7 );
    }
}
```

---

### 3. onAIProviderRequest

Fired when a provider is requested from the factory.

**When**: Before provider/service is created or retrieved
**Frequency**: Once per provider request

#### Event Data

```java
{
    provider: String,      // Provider name (e.g., "openai", "claude")
    apiKey: String,        // API key (if provided)
    params: Struct,        // Request parameters
    options: Struct        // Request options
}
```

#### Example

```java
function onAIProviderRequest( event, interceptData ) {
    var provider = interceptData.provider;

    // Override API keys from secure vault
    interceptData.apiKey = getSecretFromVault( "ai.#provider#.apiKey" );

    // Track provider usage
    trackProviderUsage( provider, getAuthenticatedUser() );

    // Apply rate limiting
    if ( hasExceededRateLimit( provider ) ) {
        throw( "Rate limit exceeded for provider: #provider#" );
    }
}
```

---

### 4. onAIProviderCreate

Fired when a provider/service instance is created.

**When**: After provider instantiation
**Frequency**: Once per unique provider instance

#### Event Data

```java
{
    provider: IService  // The created service instance
}
```

#### Example

```java
function onAIProviderCreate( event, interceptData ) {
    var service = interceptData.provider;

    // Configure provider-specific settings
    service.setTimeout( 60 );

    // Add custom headers
    service.setHeaders({
        "X-App-Version": getAppVersion(),
        "X-Environment": getEnvironment()
    });

    writeLog(
        text: "AI Provider created: #service.getProviderName()#",
        type: "info"
    );
}
```

---

### 5. onAIModelCreate

Fired when an AI model runnable is created via `aiModel()`.

**When**: Model wrapper creation
**Frequency**: Once per `aiModel()` call

#### Event Data

```java
{
    model: AiModel,     // The created model runnable
    service: IService   // The underlying service
}
```

#### Example

```java
function onAIModelCreate( event, interceptData ) {
    var model = interceptData.model;
    var service = interceptData.service;

    // Set default model parameters
    model.setParams({
        temperature: 0.7,
        max_tokens: 2000
    });

    // Track model creation
    logMetric( "ai.model.created", {
        provider: service.getProviderName(),
        timestamp: now()
    });
}
```

---

### 6. onAITransformCreate

Fired when a transform runnable is created via `aiTransform()`.

**When**: Transform function creation
**Frequency**: Once per `aiTransform()` call

#### Event Data

```java
{
    transform: AiTransformRunnable  // The created transform runnable
}
```

#### Example

```java
function onAITransformCreate( event, interceptData ) {
    var transform = interceptData.transform;

    // Wrap transform with error handling
    var originalFn = transform.getTransformFn();
    transform.setTransformFn( function( input, params ) {
        try {
            return originalFn( input, params );
        } catch ( any e ) {
            logError( "Transform error: #e.message#" );
            return { error: true, message: e.message };
        }
    });
}
```

---

### 7. beforeAIModelInvoke

Fired before an AI model is invoked (before sending to provider).

**When**: Before model execution
**Frequency**: Every model invocation

#### Event Data

```java
{
    model: AiModel,      // The model being invoked
    request: AiRequest   // The request being sent
}
```

#### Example

```java
function beforeAIModelInvoke( event, interceptData ) {
    var model = interceptData.model;
    var request = interceptData.request;

    // Validate request
    if ( arrayLen( request.getMessages() ) == 0 ) {
        throw( "Cannot invoke model with empty messages" );
    }

    // Add request tracking
    request.setMetadata({
        invokeTimestamp: now(),
        userId: getAuthenticatedUser()
    });

    // Cost estimation
    var estimatedTokens = estimateTokenCount( request.getMessages() );
    writeLog(
        text: "Model invocation: ~#estimatedTokens# tokens",
        type: "info"
    );
}
```

---

### 8. onAIRequest

Fired immediately before sending the HTTP request to the AI provider.

**When**: Before HTTP request
**Frequency**: Every API call (including streaming)

#### Event Data

```java
{
    dataPacket: Struct,   // The HTTP request data packet
    aiRequest: AiRequest, // The AI request object
    provider: IService    // The service making the request
}
```

#### Example

```java
function onAIRequest( event, interceptData ) {
    var dataPacket = interceptData.dataPacket;
    var request = interceptData.aiRequest;
    var provider = interceptData.provider;

    // Modify request before sending
    dataPacket.headers[ "X-Request-ID" ] = createUUID();

    // Log request details
    writeLog(
        text: "AI Request to #provider.getProviderName()#: " &
              "#arrayLen( request.getMessages() )# messages",
        type: "info",
        log: "ai-requests"
    );

    // Track costs
    trackAPICall( provider.getProviderName(), request.getParams() );

    // Add custom authentication
    if ( getSetting( "useCustomAuth" ) ) {
        dataPacket.headers[ "Authorization" ] = getCustomAuthToken();
    }
}
```

---

### 9. onAIResponse

Fired after receiving the HTTP response from the AI provider.

**When**: After HTTP response
**Frequency**: Every API call (including streaming)

#### Event Data

```java
{
    aiRequest: AiRequest,  // The original request
    response: Struct,      // The deserialized response
    rawResponse: Struct,   // The raw HTTP response
    provider: IService     // The service that made the request
}
```

#### Example

```java
function onAIResponse( event, interceptData ) {
    var response = interceptData.response;
    var request = interceptData.aiRequest;
    var provider = interceptData.provider;

    // Extract usage information
    if ( response.keyExists( "usage" ) ) {
        var usage = response.usage;
        logUsage({
            provider: provider.getProviderName(),
            promptTokens: usage.prompt_tokens ?: 0,
            completionTokens: usage.completion_tokens ?: 0,
            totalTokens: usage.total_tokens ?: 0,
            timestamp: now()
        });
    }

    // Modify response
    if ( response.keyExists( "choices" ) && arrayLen( response.choices ) > 0 ) {
        // Add metadata to response
        response._metadata = {
            processedAt: now(),
            provider: provider.getProviderName(),
            cached: false
        };
    }

    // Cache response
    if ( getSetting( "cacheResponses" ) ) {
        cacheResponse( request, response );
    }
}
```

---

### 10. afterAIModelInvoke

Fired after an AI model completes its invocation.

**When**: After model execution completes
**Frequency**: Every model invocation

#### Event Data

```java
{
    model: AiModel,      // The model that was invoked
    request: AiRequest,  // The request that was sent
    results: Any         // The results returned by the model
}
```

#### Example

```java
function afterAIModelInvoke( event, interceptData ) {
    var model = interceptData.model;
    var request = interceptData.request;
    var results = interceptData.results;

    // Calculate execution time
    var startTime = request.getMetadata().invokeTimestamp ?: now();
    var duration = dateDiff( "s", startTime, now() );

    // Log completion
    writeLog(
        text: "Model invocation completed in #duration#s",
        type: "info"
    );

    // Track metrics
    recordMetric( "ai.model.duration", duration );

    // Validate response
    if ( isStruct( results ) && results.keyExists( "error" ) ) {
        logError( "Model returned error: #results.error.message#" );
    }
}
```

---

## Event Arguments

All event handlers receive two arguments:

### 1. event (EventContext)

The current request context (if applicable). This provides access to:

- Request/response data
- Session information
- Application scope
- Context variables

### 2. interceptData (Struct)

Event-specific data that can be modified. Changes to this struct affect the operation:

- **Read**: Access current values
- **Modify**: Change values to alter behavior
- **Add**: Inject new data

**Important**: Not all properties can be modified. Some are read-only references.

---

## Common Use Cases

### 1. Request Logging and Monitoring

```java
function onAIRequest( event, interceptData ) {
    var logData = {
        timestamp: now(),
        provider: interceptData.provider.getProviderName(),
        messages: interceptData.aiRequest.getMessages(),
        params: interceptData.aiRequest.getParams(),
        user: getAuthenticatedUser()
    };

    // Log to database
    queryExecute(
        "INSERT INTO ai_request_log (data, created_at) VALUES (:data, :timestamp)",
        { data: serializeJSON( logData ), timestamp: now() }
    );
}
```

### 2. Cost Tracking and Budgeting

```java
class {

    function onAIResponse( event, interceptData ) {
        if ( !interceptData.response.keyExists( "usage" ) ) return;

        var usage = interceptData.response.usage;
        var provider = interceptData.provider.getProviderName();

        // Calculate cost based on provider pricing
        var cost = calculateCost( provider, usage );

        // Track against user/org budget
        var user = getAuthenticatedUser();
        var currentUsage = getUserUsage( user );

        updateUserUsage( user, cost );

        // Alert if approaching limit
        if ( currentUsage + cost >= getUserBudget( user ) * 0.9 ) {
            sendBudgetAlert( user );
        }

        // Block if over budget
        if ( currentUsage + cost > getUserBudget( user ) ) {
            throw( "Budget exceeded for user: #user#" );
        }
    }

    private function calculateCost( provider, usage ) {
        // Pricing per 1K tokens (example rates)
        var pricing = {
            openai: {
                prompt: 0.03,
                completion: 0.06
            },
            claude: {
                prompt: 0.015,
                completion: 0.075
            }
        };

        var rates = pricing[ provider ] ?: { prompt: 0.01, completion: 0.01 };

        return (
            ( usage.prompt_tokens / 1000 ) * rates.prompt +
            ( usage.completion_tokens / 1000 ) * rates.completion
        );
    }
}
```

### 3. Response Caching

```java
class {

    function onAIRequest( event, interceptData ) {
        var request = interceptData.aiRequest;
        var cacheKey = generateCacheKey( request );

        // Check cache
        var cached = cacheGet( cacheKey );
        if ( !isNull( cached ) ) {
            // Return cached response and skip actual API call
            interceptData.useCached = true;
            interceptData.cachedResponse = cached;

            writeLog(
                text: "Using cached AI response",
                type: "info"
            );
        }
    }

    function onAIResponse( event, interceptData ) {
        // Don't cache if we used cached response
        if ( interceptData.keyExists( "useCached" ) ) return;

        var request = interceptData.aiRequest;
        var response = interceptData.response;
        var cacheKey = generateCacheKey( request );

        // Cache for 1 hour
        cachePut(
            cacheKey,
            response,
            createTimeSpan( 0, 1, 0, 0 )
        );
    }

    private function generateCacheKey( request ) {
        var key = {
            messages: request.getMessages(),
            params: request.getParams()
        };
        return hash( serializeJSON( key ) );
    }
}
```

### 4. Content Filtering and Moderation

```java
function onAIRequest( event, interceptData ) {
    var request = interceptData.aiRequest;
    var messages = request.getMessages();

    // Check for prohibited content
    for ( var msg in messages ) {
        if ( containsProhibitedContent( msg.content ) ) {
            throw(
                type: "ContentViolation",
                message: "Request contains prohibited content"
            );
        }
    }
}

function onAIResponse( event, interceptData ) {
    var response = interceptData.response;

    // Filter response content
    if ( response.keyExists( "choices" ) ) {
        for ( var choice in response.choices ) {
            if ( choice.keyExists( "message" ) ) {
                choice.message.content = filterContent(
                    choice.message.content
                );
            }
        }
    }
}

private function containsProhibitedContent( text ) {
    var prohibitedPatterns = [
        "pattern1",
        "pattern2"
    ];

    for ( var pattern in prohibitedPatterns ) {
        if ( findNoCase( pattern, text ) ) {
            return true;
        }
    }

    return false;
}

private function filterContent( text ) {
    // Replace sensitive information
    text = reReplace( text, "\b\d{3}-\d{2}-\d{4}\b", "[SSN]", "all" );
    text = reReplace( text, "\b\d{16}\b", "[CREDIT_CARD]", "all" );
    return text;
}
```

### 5. Multi-Provider Fallback

```java
class {

    property name="failedProviders" default={};

    function onAIRequest( event, interceptData ) {
        var provider = interceptData.provider.getProviderName();

        // Check if provider is in cooldown
        if ( failedProviders.keyExists( provider ) ) {
            var cooldownEnd = failedProviders[ provider ];
            if ( now() < cooldownEnd ) {
                // Switch to backup provider
                var backup = getBackupProvider( provider );
                writeLog(
                    text: "Switching from #provider# to #backup# (cooldown)",
                    type: "warning"
                );
                // Recreate request with backup provider
                throw(
                    type: "ProviderCooldown",
                    message: "Provider in cooldown, use backup"
                );
            } else {
                // Cooldown expired, remove from list
                structDelete( failedProviders, provider );
            }
        }
    }

    function onAIResponse( event, interceptData ) {
        var response = interceptData.response;
        var provider = interceptData.provider.getProviderName();

        // Check for rate limiting
        if ( response.keyExists( "error" ) &&
             response.error.type == "rate_limit_exceeded" ) {

            // Put provider in 5-minute cooldown
            failedProviders[ provider ] = dateAdd( "n", 5, now() );

            writeLog(
                text: "Provider #provider# rate limited, cooldown until #failedProviders[provider]#",
                type: "warning"
            );
        }
    }

    private function getBackupProvider( provider ) {
        var backups = {
            "openai": "claude",
            "claude": "gemini",
            "gemini": "openai"
        };
        return backups[ provider ] ?: "openai";
    }
}
```

### 6. A/B Testing Different Models

```java
class {

    function onAIRequest( event, interceptData ) {
        var request = interceptData.aiRequest;
        var user = getAuthenticatedUser();

        // Assign users to test groups
        var testGroup = hash( user ).left( 1 ) < "8" ? "A" : "B";

        if ( testGroup == "A" ) {
            // Group A: GPT-4
            request.setParam( "model", "gpt-4" );
        } else {
            // Group B: Claude
            request.setParam( "model", "claude-3-opus" );
        }

        // Track which group
        request.setMetadata({
            testGroup: testGroup,
            experimentId: "model_comparison_2024"
        });
    }

    function onAIResponse( event, interceptData ) {
        var request = interceptData.aiRequest;
        var response = interceptData.response;
        var metadata = request.getMetadata();

        // Log results for analysis
        logExperiment({
            experimentId: metadata.experimentId,
            testGroup: metadata.testGroup,
            model: request.getParams().model,
            tokensUsed: response.usage?.total_tokens ?: 0,
            timestamp: now()
        });
    }
}
```

### 7. Adding Safety Guardrails

```java
class {

    function beforeAIModelInvoke( event, interceptData ) {
        var request = interceptData.request;

        // Add safety system message
        var messages = request.getMessages();
        var safetyMessage = {
            role: "system",
            content: "You must not provide information about: illegal activities, violence, harmful content, or personal data. If asked, politely decline and explain why."
        };

        // Prepend safety instructions
        arrayPrepend( messages, safetyMessage );
        request.setMessages( messages );
    }

    function afterAIModelInvoke( event, interceptData ) {
        var results = interceptData.results;

        // Check response for policy violations
        if ( isStruct( results ) && results.keyExists( "choices" ) ) {
            for ( var choice in results.choices ) {
                if ( violatesSafetyPolicy( choice.message.content ) ) {
                    // Override response
                    choice.message.content = "I cannot provide that information as it may violate safety policies.";

                    // Log violation
                    logSafetyViolation({
                        request: interceptData.request,
                        originalResponse: choice.message.content,
                        timestamp: now()
                    });
                }
            }
        }
    }

    private function violatesSafetyPolicy( content ) {
        // Implement your safety checks
        var violations = [
            "personal information",
            "illegal activity",
            "violence"
        ];

        for ( var violation in violations ) {
            if ( findNoCase( violation, content ) ) {
                return true;
            }
        }

        return false;
    }
}
```

---

## Best Practices

### 1. Keep Event Handlers Lightweight

Event handlers are called frequently. Keep processing minimal:

```java
// ❌ Bad: Heavy processing
function onAIRequest( event, interceptData ) {
    // This runs complex queries and blocks
    var history = getAllUserAIHistory( getUser() );
    analyzeHistoryForPatterns( history );
}

// ✅ Good: Lightweight, async if needed
function onAIRequest( event, interceptData ) {
    // Quick validation only
    validateRequest( interceptData.aiRequest );

    // Heavy work done async
    runAsync( function() {
        trackRequest( interceptData.aiRequest );
    });
}
```

### 2. Handle Errors Gracefully

Don't let interceptor errors break AI operations:

```java
function onAIResponse( event, interceptData ) {
    try {
        // Your processing
        processResponse( interceptData.response );
    } catch ( any e ) {
        // Log but don't throw
        writeLog(
            text: "Error in interceptor: #e.message#",
            type: "error"
        );
        // Continue without breaking the flow
    }
}
```

### 3. Document Side Effects

Make it clear what your interceptors modify:

```java
/**
 * AI Request Interceptor
 *
 * Modifies:
 * - Adds X-Request-ID header
 * - Overrides temperature to 0.7 if not set
 * - Logs request to database
 *
 * Does NOT modify:
 * - Message content
 * - Model selection
 */
function onAIRequest( event, interceptData ) {
    // Implementation
}
```

### 4. Use Naming Conventions

```java
// Prefix interceptor classes with purpose
AIMonitoringInterceptor.bx
AISecurityInterceptor.bx
AICostTrackingInterceptor.bx
AIContentFilterInterceptor.bx
```

### 5. Order Matters

Interceptors execute in registration order. Be mindful:

```java
interceptors = [
    { class: "SecurityInterceptor" },      // Run first: validate
    { class: "CostTrackingInterceptor" },  // Then: track costs
    { class: "LoggingInterceptor" }        // Finally: log
];
```

### 6. Test Interceptors Independently

Write unit tests for your interceptor logic:

```java
// tests/interceptors/AIMonitorTest.bx
class extends="testbox.system.BaseSpec" {

    function run() {
        describe( "AIMonitor Interceptor", function() {

            it( "should log requests", function() {
                var monitor = new interceptors.AIMonitor();
                var interceptData = {
                    aiRequest: mockRequest(),
                    provider: mockProvider()
                };

                monitor.onAIRequest( {}, interceptData );

                // Verify logging occurred
                expect( getLogEntries() ).toHaveLength( 1 );
            });
        });
    }
}
```

### 7. Make Interceptors Configurable

```java
class {

    property name="enabled" default=true;
    property name="logLevel" default="info";
    property name="destinations" default=["console","file"];

    function configure() {
        // Read from settings
        variables.enabled = getSetting( "monitoring.enabled" );
        variables.logLevel = getSetting( "monitoring.logLevel" );
    }

    function onAIRequest( event, interceptData ) {
        if ( !variables.enabled ) return;

        // Use configuration
        log( interceptData, variables.logLevel );
    }
}
```

---

## Examples

### Complete Monitoring Solution

```java
// interceptors/AICompleteMonitor.bx
class {

    property name="sessionId";

    function configure() {
        variables.sessionId = createUUID();
    }

    function onAIMessageCreate( event, interceptData ) {
        trackEvent( "message_created", {
            messageCount: arrayLen( interceptData.message.getMessages() )
        });
    }

    function onAIModelCreate( event, interceptData ) {
        trackEvent( "model_created", {
            provider: interceptData.service.getProviderName()
        });
    }

    function beforeAIModelInvoke( event, interceptData ) {
        interceptData.request.setMetadata({
            startTime: getTickCount(),
            sessionId: variables.sessionId
        });
    }

    function onAIRequest( event, interceptData ) {
        var request = interceptData.aiRequest;
        var metadata = request.getMetadata();

        trackEvent( "request_sent", {
            provider: interceptData.provider.getProviderName(),
            messageCount: arrayLen( request.getMessages() ),
            sessionId: metadata.sessionId
        });
    }

    function onAIResponse( event, interceptData ) {
        var request = interceptData.aiRequest;
        var response = interceptData.response;

        trackEvent( "response_received", {
            provider: interceptData.provider.getProviderName(),
            tokensUsed: response.usage?.total_tokens ?: 0,
            sessionId: request.getMetadata().sessionId
        });
    }

    function afterAIModelInvoke( event, interceptData ) {
        var request = interceptData.request;
        var metadata = request.getMetadata();
        var duration = getTickCount() - metadata.startTime;

        trackEvent( "invocation_complete", {
            duration: duration,
            sessionId: metadata.sessionId
        });
    }

    private function trackEvent( eventName, data ) {
        // Your analytics implementation
        analyticsService.track( eventName, data );
    }
}
```

### Security and Compliance

```java
// interceptors/AISecurityCompliance.bx
class {

    function onAIRequest( event, interceptData ) {
        var request = interceptData.aiRequest;
        var user = getAuthenticatedUser();

        // Validate user has permission
        if ( !hasPermission( user, "ai.use" ) ) {
            throw(
                type: "SecurityViolation",
                message: "User not authorized for AI operations"
            );
        }

        // Redact sensitive data
        var messages = request.getMessages();
        for ( var msg in messages ) {
            msg.content = redactSensitiveData( msg.content );
        }
        request.setMessages( messages );

        // Add audit trail
        request.setMetadata({
            userId: user.getId(),
            ipAddress: getClientIP(),
            timestamp: now()
        });
    }

    function onAIResponse( event, interceptData ) {
        // Log for compliance
        auditLog({
            userId: interceptData.aiRequest.getMetadata().userId,
            action: "ai_query",
            provider: interceptData.provider.getProviderName(),
            timestamp: now(),
            tokensUsed: interceptData.response.usage?.total_tokens ?: 0
        });
    }

    private function redactSensitiveData( text ) {
        // Email addresses
        text = reReplace( text, "\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b", "[EMAIL]", "all" );
        // Phone numbers
        text = reReplace( text, "\b\d{3}[-.]?\d{3}[-.]?\d{4}\b", "[PHONE]", "all" );
        // SSN
        text = reReplace( text, "\b\d{3}-\d{2}-\d{4}\b", "[SSN]", "all" );
        return text;
    }
}
```

---

## Next Steps

Now that you understand the event system, you can:

- **Monitor**: Track AI usage and performance
- **Secure**: Add authentication and content filtering
- **Optimize**: Implement caching and cost controls
- **Extend**: Build custom behaviors without modifying core code

### Related Documentation

- **[Pipeline Overview](../pipelines/overview.md)** - Understanding AI pipelines
- **[Service-Level Chatting](../simple-interactions/service-chatting.md)** - Direct service control

### Additional Resources

- **BoxLang Interceptor Documentation**: Learn more about the interceptor system
- **Event-Driven Architecture**: Best practices for event handling
- **Security Guidelines**: Protecting AI operations

---

**Copyright** © 2023-2025 Ortus Solutions, Corp
