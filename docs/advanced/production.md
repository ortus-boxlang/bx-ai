---
description: "Production deployment guide for BoxLang AI - monitoring, error handling, performance optimization, and best practices."
icon: server
---

# 🚀 Production Deployment

Comprehensive guide for deploying BoxLang AI applications to production environments. Learn about monitoring, error handling, performance optimization, security, and operational best practices.

## 📋 Table of Contents

- [Pre-Deployment Checklist](#pre-deployment-checklist)
- [Configuration Management](#configuration-management)
- [Error Handling & Resilience](#error-handling--resilience)
- [Monitoring & Observability](#monitoring--observability)
- [Performance Optimization](#performance-optimization)
- [Cost Management](#cost-management)
- [High Availability](#high-availability)
- [Scaling Strategies](#scaling-strategies)
- [Database & Memory](#database--memory)
- [Container Deployment](#container-deployment)
- [Security Hardening](#security-hardening)
- [Operational Procedures](#operational-procedures)

---

## ✅ Pre-Deployment Checklist

### Essential Requirements

Before deploying to production, ensure you have:

- ✅ **API Keys Secured** - Stored in environment variables or secrets manager
- ✅ **Error Handling** - Comprehensive try-catch blocks around AI calls
- ✅ **Rate Limiting** - Client-side request throttling implemented
- ✅ **Monitoring** - Logging and alerting configured
- ✅ **Fallback Strategy** - Secondary provider or graceful degradation
- ✅ **Timeout Configuration** - Appropriate timeouts for your use case
- ✅ **Cost Limits** - Budget alerts and usage tracking
- ✅ **Health Checks** - Endpoints to verify service availability
- ✅ **Load Testing** - Performance validated under expected load
- ✅ **Backup Provider** - Alternative AI provider configured

### Configuration Validation

```javascript
// Validate configuration on startup
function validateAIConfiguration() {
    required = [
        "OPENAI_API_KEY",
        "CLAUDE_API_KEY",  // Backup provider
        "AI_TIMEOUT_SECONDS",
        "AI_MAX_RETRIES"
    ]

    missing = []
    for ( key in required ) {
        if ( isNull( getSystemSetting( key, "" ) ) || getSystemSetting( key ) == "" ) {
            missing.append( key )
        }
    }

    if ( !missing.isEmpty() ) {
        throw "Missing required AI configuration: #missing.toList()#"
    }

    writeLog( "AI configuration validated successfully" )
}

// Run on application start
BoxAnnounce( "onApplicationStart", {
    listener: validateAIConfiguration
} )
```

---

## ⚙️ Configuration Management

### Environment-Based Configuration

**Separate configs per environment**:

```javascript
// config/ai-config.bx
class {
    function getAIConfig() {
        var env = getSystemSetting( "ENVIRONMENT", "production" )

        var configs = {
            "development": {
                provider: "ollama",           // Free for dev
                model: "llama3.2:3b",
                timeout: 30,
                retries: 1,
                logLevel: "DEBUG"
            },
            "staging": {
                provider: "openai",
                model: "gpt-3.5-turbo",       // Cheaper for staging
                timeout: 15,
                retries: 2,
                logLevel: "INFO"
            },
            "production": {
                provider: "openai",
                model: "gpt-4o",              // Best quality for prod
                fallbackProvider: "claude",
                timeout: 10,
                retries: 3,
                logLevel: "WARN",
                enableCaching: true,
                enableMonitoring: true
            }
        }

        return configs[ env ]
    }
}
```

### Secrets Management

**Never hardcode API keys**:

```javascript
// ❌ WRONG
apiKey = "sk-1234567890abcdef"

// ✅ RIGHT - Environment variables
apiKey = getSystemSetting( "OPENAI_API_KEY" )

// ✅ RIGHT - AWS Secrets Manager
function getAPIKey( secretName ) {
    return awsSecretsManager.getSecret( secretName ).getValue()
}

// ✅ RIGHT - Azure Key Vault
function getAPIKey( secretName ) {
    return azureKeyVault.getSecret( secretName )
}

// ✅ RIGHT - HashiCorp Vault
function getAPIKey( path ) {
    return vaultClient.read( path ).data.apiKey
}
```

### Dynamic Configuration Reloading

```javascript
// Reload config without restarting app
class singleton {
    property name="config" type="struct";
    property name="lastReload" type="date";

    function init() {
        reloadConfig()
        return this
    }

    function reloadConfig() {
        variables.config = deserializeJSON(
            fileRead( "/config/ai-production.json" )
        )
        variables.lastReload = now()
        writeLog( "AI configuration reloaded" )
    }

    function getConfig() {
        // Auto-reload every 5 minutes
        if ( dateDiff( "n", variables.lastReload, now() ) > 5 ) {
            reloadConfig()
        }
        return variables.config
    }
}
```

---

## 🛡️ Error Handling & Resilience

### Comprehensive Error Handling

```javascript
class {
    function safeAIChat( required string prompt, struct params = {}, struct options = {} ) {
        var maxRetries = params.maxRetries ?: 3
        var retryDelay = params.retryDelay ?: 1000  // milliseconds

        for ( var attempt = 1; attempt <= maxRetries; attempt++ ) {
            try {
                return aiChat(
                    arguments.prompt,
                    arguments.params,
                    arguments.options
                )

            } catch ( RateLimitException e ) {
                writeLog(
                    "Rate limit hit (attempt #attempt#/#maxRetries#): #e.message#",
                    "warning"
                )

                if ( attempt < maxRetries ) {
                    // Exponential backoff
                    sleep( retryDelay * attempt )
                } else {
                    // Try fallback provider
                    return tryFallbackProvider( arguments.prompt, arguments.params )
                }

            } catch ( TimeoutException e ) {
                writeLog(
                    "Timeout (attempt #attempt#/#maxRetries#): #e.message#",
                    "error"
                )

                if ( attempt == maxRetries ) {
                    return getDefaultResponse( arguments.prompt )
                }

            } catch ( AuthenticationException e ) {
                writeLog( "Authentication failed: #e.message#", "critical" )
                notifyOps( "AI authentication failure", e )
                throw e  // Don't retry auth errors

            } catch ( any e ) {
                writeLog(
                    "AI error (attempt #attempt#/#maxRetries#): #e.message#",
                    "error"
                )

                if ( attempt == maxRetries ) {
                    notifyOps( "AI service failure", e )
                    return getDefaultResponse( arguments.prompt )
                }
            }
        }
    }

    function tryFallbackProvider( required string prompt, struct params = {} ) {
        writeLog( "Attempting fallback provider", "info" )

        var fallbackProviders = [ "claude", "gemini", "groq" ]

        for ( var provider in fallbackProviders ) {
            try {
                return aiChat(
                    arguments.prompt,
                    arguments.params,
                    { provider: provider }
                )
            } catch ( any e ) {
                writeLog( "Fallback provider #provider# failed: #e.message#", "warning" )
            }
        }

        throw "All AI providers failed"
    }

    function getDefaultResponse( required string prompt ) {
        // Return safe fallback response
        return "I apologize, but I'm experiencing technical difficulties. " &
               "Please try again in a moment or contact support if the issue persists."
    }

    function notifyOps( required string message, any error ) {
        // Send to monitoring system
        // Slack, PagerDuty, email, etc.
    }
}
```

### Circuit Breaker Pattern

Prevent cascading failures:

```javascript
class singleton {
    property name="failures" type="numeric" default="0";
    property name="lastFailure" type="date";
    property name="state" type="string" default="CLOSED";  // CLOSED, OPEN, HALF_OPEN
    property name="threshold" type="numeric" default="5";
    property name="timeout" type="numeric" default="60";  // seconds

    function call( required function operation ) {
        if ( variables.state == "OPEN" ) {
            if ( dateDiff( "s", variables.lastFailure, now() ) > variables.timeout ) {
                variables.state = "HALF_OPEN"
                writeLog( "Circuit breaker entering HALF_OPEN state" )
            } else {
                throw "Circuit breaker is OPEN - service unavailable"
            }
        }

        try {
            var result = arguments.operation()

            if ( variables.state == "HALF_OPEN" ) {
                reset()
            }

            return result

        } catch ( any e ) {
            recordFailure()
            throw e
        }
    }

    function recordFailure() {
        variables.failures++
        variables.lastFailure = now()

        if ( variables.failures >= variables.threshold ) {
            variables.state = "OPEN"
            writeLog( "Circuit breaker OPENED after #variables.failures# failures", "critical" )
            notifyOps( "AI circuit breaker opened" )
        }
    }

    function reset() {
        variables.failures = 0
        variables.state = "CLOSED"
        writeLog( "Circuit breaker CLOSED - service recovered" )
    }

    function getState() {
        return variables.state
    }
}

// Usage
circuitBreaker = getInstance( "CircuitBreaker" )

response = circuitBreaker.call( () => {
    return aiChat( "What is BoxLang?" )
} )
```

---

## 📊 Monitoring & Observability

### Event-Based Monitoring

Use BoxLang AI's event system:

```javascript
// EventListener.bx
class {
    function onAIRequest( event, interceptData ) {
        var startTime = getTickCount()
        interceptData.startTime = startTime

        writeLog(
            "AI Request: provider=#interceptData.provider#, model=#interceptData.chatRequest.model#",
            "info"
        )
    }

    function onAIResponse( event, interceptData ) {
        var duration = getTickCount() - interceptData.startTime
        var tokenCount = interceptData.response.usage?.total_tokens ?: 0

        // Log metrics
        writeLog(
            "AI Response: duration=#duration#ms, tokens=#tokenCount#, provider=#interceptData.provider#",
            "info"
        )

        // Send to monitoring
        sendMetric( "ai.request.duration", duration, {
            provider: interceptData.provider,
            model: interceptData.chatRequest.model
        } )

        sendMetric( "ai.tokens.used", tokenCount, {
            provider: interceptData.provider
        } )

        // Track costs
        var cost = estimateCost( interceptData.provider, tokenCount )
        sendMetric( "ai.cost", cost, {
            provider: interceptData.provider
        } )
    }

    function onAIError( event, interceptData ) {
        writeLog(
            "AI Error: #interceptData.error.message#, provider=#interceptData.provider#",
            "error"
        )

        sendMetric( "ai.errors", 1, {
            provider: interceptData.provider,
            errorType: interceptData.error.type
        } )

        // Alert on high error rate
        if ( getErrorRate() > 0.05 ) {  // 5% error threshold
            notifyOps( "High AI error rate detected" )
        }
    }
}

// Register listeners in ModuleConfig.bx or Application.bx
BoxRegisterInterceptor(
    interceptorObject: new EventListener(),
    interceptorName: "AIMonitoring"
)
```

### Metrics Collection

```javascript
class singleton {
    property name="metrics" type="struct";

    function init() {
        variables.metrics = {
            requests: 0,
            successes: 0,
            failures: 0,
            totalTokens: 0,
            totalCost: 0,
            byProvider: {}
        }
        return this
    }

    function recordRequest(
        required string provider,
        required numeric tokens,
        required numeric cost,
        boolean success = true
    ) {
        lock name="metrics" type="exclusive" timeout="5" {
            variables.metrics.requests++

            if ( arguments.success ) {
                variables.metrics.successes++
            } else {
                variables.metrics.failures++
            }

            variables.metrics.totalTokens += arguments.tokens
            variables.metrics.totalCost += arguments.cost

            if ( !structKeyExists( variables.metrics.byProvider, arguments.provider ) ) {
                variables.metrics.byProvider[ arguments.provider ] = {
                    requests: 0,
                    tokens: 0,
                    cost: 0
                }
            }

            variables.metrics.byProvider[ arguments.provider ].requests++
            variables.metrics.byProvider[ arguments.provider ].tokens += arguments.tokens
            variables.metrics.byProvider[ arguments.provider ].cost += arguments.cost
        }
    }

    function getMetrics() {
        lock name="metrics" type="readonly" timeout="5" {
            return duplicate( variables.metrics )
        }
    }

    function reset() {
        init()
    }
}

// Expose metrics endpoint
// /api/metrics
function metrics() {
    var metricsService = getInstance( "MetricsService" )
    return renderJSON( metricsService.getMetrics() )
}
```

### Health Checks

```javascript
// /health endpoint
function health() {
    var status = {
        status: "healthy",
        timestamp: now(),
        checks: {}
    }

    // Check AI providers
    providers = [ "openai", "claude" ]
    for ( provider in providers ) {
        try {
            var start = getTickCount()
            aiChat( "test", { max_tokens: 1 }, { provider: provider } )
            status.checks[ provider ] = {
                status: "up",
                responseTime: getTickCount() - start
            }
        } catch ( any e ) {
            status.status = "degraded"
            status.checks[ provider ] = {
                status: "down",
                error: e.message
            }
        }
    }

    // Check memory systems
    try {
        var memory = aiMemory( "cache" )
        memory.add( { role: "user", content: "test" } )
        memory.clear()
        status.checks.memory = { status: "up" }
    } catch ( any e ) {
        status.status = "degraded"
        status.checks.memory = {
            status: "down",
            error: e.message
        }
    }

    // Set HTTP status code
    if ( status.status == "healthy" ) {
        setHTTPStatus( 200 )
    } else {
        setHTTPStatus( 503 )
    }

    return renderJSON( status )
}
```

### Logging Best Practices

```javascript
// Structured logging
function logAIInteraction(
    required string action,
    required string provider,
    struct metadata = {}
) {
    var logData = {
        timestamp: now(),
        action: arguments.action,
        provider: arguments.provider,
        userId: session.userId ?: "anonymous",
        requestId: request.requestId ?: createUUID(),
        metadata: arguments.metadata
    }

    writeLog(
        text: serializeJSON( logData ),
        type: "information",
        file: "ai-interactions"
    )
}

// Usage
logAIInteraction(
    action: "chat_request",
    provider: "openai",
    metadata: {
        model: "gpt-4",
        promptLength: len( prompt ),
        temperature: 0.7
    }
)
```

---

## ⚡ Performance Optimization

### Response Caching

```javascript
class {
    function getCachedAIResponse(
        required string prompt,
        struct params = {},
        numeric ttl = 3600
    ) {
        // Generate cache key
        var cacheKey = hash(
            serializeJSON({
                prompt: arguments.prompt,
                params: arguments.params
            }),
            "MD5"
        )

        // Try cache first
        var cached = cacheGet( "ai_#cacheKey#" )
        if ( !isNull( cached ) ) {
            writeLog( "AI cache hit for: #left( arguments.prompt, 50 )#..." )
            return cached
        }

        // Call AI
        var response = aiChat( arguments.prompt, arguments.params )

        // Cache response
        cacheSet(
            "ai_#cacheKey#",
            response,
            arguments.ttl
        )

        return response
    }
}
```

### Connection Pooling

```javascript
// For JDBC memory
datasource = {
    name: "aiMemory",
    driver: "postgresql",
    url: "jdbc:postgresql://localhost:5432/ai_db",
    username: getSystemSetting( "DB_USER" ),
    password: getSystemSetting( "DB_PASSWORD" ),

    // Connection pool settings
    maxConnections: 50,
    minConnections: 10,
    maxIdleTime: 30,
    connectionTimeout: 5000,
    validationQuery: "SELECT 1"
}
```

### Async Processing

```javascript
// Non-blocking AI calls
function processUserRequest( required string prompt ) {
    // Return immediately with request ID
    var requestId = createUUID()

    // Process in background
    runAsync( () => {
        try {
            var response = aiChat( arguments.prompt )

            // Store result
            cacheSet( "ai_result_#requestId#", {
                status: "completed",
                response: response
            }, 300 )

            // Notify user (websocket, email, etc.)
            notifyUser( session.userId, requestId, response )

        } catch ( any e ) {
            cacheSet( "ai_result_#requestId#", {
                status: "failed",
                error: e.message
            }, 300 )
        }
    } )

    return {
        requestId: requestId,
        status: "processing"
    }
}

// Check result endpoint
function checkResult( required string requestId ) {
    var result = cacheGet( "ai_result_#arguments.requestId#" )

    if ( isNull( result ) ) {
        return { status: "processing" }
    }

    return result
}
```

### Batch Processing

```javascript
// Process multiple prompts efficiently
function batchAIChat( required array prompts ) {
    var futures = []

    // Start all requests in parallel
    for ( var prompt in arguments.prompts ) {
        futures.append(
            aiChatAsync( prompt )
        )
    }

    // Collect results
    var results = []
    for ( var future in futures ) {
        try {
            results.append({
                success: true,
                response: future.get( 30, "seconds" )
            })
        } catch ( any e ) {
            results.append({
                success: false,
                error: e.message
            })
        }
    }

    return results
}
```

---

## 💰 Cost Management

### Usage Tracking

```javascript
class singleton {
    property name="dailyUsage" type="struct";

    function init() {
        variables.dailyUsage = {}
        return this
    }

    function trackUsage(
        required string provider,
        required numeric tokens,
        required numeric cost
    ) {
        var today = dateFormat( now(), "yyyy-mm-dd" )

        lock name="usage_#today#" type="exclusive" timeout="5" {
            if ( !structKeyExists( variables.dailyUsage, today ) ) {
                variables.dailyUsage[ today ] = {
                    totalTokens: 0,
                    totalCost: 0,
                    byProvider: {}
                }
            }

            variables.dailyUsage[ today ].totalTokens += arguments.tokens
            variables.dailyUsage[ today ].totalCost += arguments.cost

            if ( !structKeyExists( variables.dailyUsage[ today ].byProvider, arguments.provider ) ) {
                variables.dailyUsage[ today ].byProvider[ arguments.provider ] = {
                    tokens: 0,
                    cost: 0
                }
            }

            variables.dailyUsage[ today ].byProvider[ arguments.provider ].tokens += arguments.tokens
            variables.dailyUsage[ today ].byProvider[ arguments.provider ].cost += arguments.cost
        }

        // Check budget limits
        checkBudget( today )
    }

    function checkBudget( required string date ) {
        var dailyBudget = getSystemSetting( "AI_DAILY_BUDGET", 100 )
        var usage = variables.dailyUsage[ arguments.date ]

        if ( usage.totalCost > dailyBudget ) {
            writeLog(
                "Daily AI budget exceeded: $#usage.totalCost# > $#dailyBudget#",
                "critical"
            )
            notifyOps( "AI budget alert", {
                date: arguments.date,
                spent: usage.totalCost,
                budget: dailyBudget
            } )
        } else if ( usage.totalCost > ( dailyBudget * 0.8 ) ) {
            writeLog(
                "AI budget at 80%: $#usage.totalCost# / $#dailyBudget#",
                "warning"
            )
        }
    }

    function getDailyUsage( string date ) {
        var targetDate = arguments.date ?: dateFormat( now(), "yyyy-mm-dd" )
        return variables.dailyUsage[ targetDate ] ?: {}
    }
}
```

### Cost Estimation

```javascript
function estimateCost(
    required string provider,
    required numeric inputTokens,
    required numeric outputTokens
) {
    // Pricing per 1M tokens (as of Dec 2024)
    var pricing = {
        "openai": {
            "gpt-4o": { input: 2.50, output: 10.00 },
            "gpt-4-turbo": { input: 10.00, output: 30.00 },
            "gpt-3.5-turbo": { input: 0.50, output: 1.50 }
        },
        "claude": {
            "claude-3-opus": { input: 15.00, output: 75.00 },
            "claude-3-sonnet": { input: 3.00, output: 15.00 },
            "claude-3-haiku": { input: 0.25, output: 1.25 }
        },
        "gemini": {
            "gemini-1.5-pro": { input: 1.25, output: 5.00 },
            "gemini-1.5-flash": { input: 0.075, output: 0.30 }
        }
    }

    var rates = pricing[ arguments.provider ][ model ] ?: { input: 0, output: 0 }

    var inputCost = ( arguments.inputTokens / 1000000 ) * rates.input
    var outputCost = ( arguments.outputTokens / 1000000 ) * rates.output

    return inputCost + outputCost
}
```

---

## 🔄 High Availability

### Provider Failover

```javascript
class {
    property name="providers" type="array";
    property name="currentProvider" type="numeric" default="1";

    function init() {
        variables.providers = [
            { name: "openai", priority: 1 },
            { name: "claude", priority: 2 },
            { name: "gemini", priority: 3 }
        ]
        return this
    }

    function callWithFailover( required string prompt, struct params = {} ) {
        for ( var provider in variables.providers ) {
            try {
                writeLog( "Attempting provider: #provider.name#" )

                return aiChat(
                    arguments.prompt,
                    arguments.params,
                    { provider: provider.name }
                )

            } catch ( any e ) {
                writeLog(
                    "Provider #provider.name# failed: #e.message#",
                    "warning"
                )

                // Continue to next provider
                if ( provider.priority == arrayLen( variables.providers ) ) {
                    throw "All AI providers failed"
                }
            }
        }
    }
}
```

### Load Balancing

```javascript
class {
    property name="providers" type="array";
    property name="currentIndex" type="numeric" default="1";

    function init() {
        variables.providers = [ "openai", "claude", "gemini" ]
        return this
    }

    function getNextProvider() {
        lock name="loadbalancer" type="exclusive" timeout="5" {
            var provider = variables.providers[ variables.currentIndex ]
            variables.currentIndex++

            if ( variables.currentIndex > arrayLen( variables.providers ) ) {
                variables.currentIndex = 1
            }

            return provider
        }
    }

    function callBalanced( required string prompt, struct params = {} ) {
        var provider = getNextProvider()
        return aiChat(
            arguments.prompt,
            arguments.params,
            { provider: provider }
        )
    }
}
```

---

## 📦 Container Deployment

### Docker Configuration

```dockerfile
# Dockerfile
FROM ortussolutions/boxlang:1.0.0

# Install module
RUN box install bx-ai

# Copy application
COPY . /app
WORKDIR /app

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Environment variables (override in deployment)
ENV OPENAI_API_KEY=""
ENV CLAUDE_API_KEY=""
ENV AI_TIMEOUT=10
ENV AI_MAX_RETRIES=3

EXPOSE 8080

CMD ["boxlang", "server.bxs"]
```

### Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - CLAUDE_API_KEY=${CLAUDE_API_KEY}
      - DATABASE_URL=${DATABASE_URL}
      - REDIS_URL=${REDIS_URL}
    depends_on:
      - postgres
      - redis
    restart: unless-stopped

  postgres:
    image: pgvector/pgvector:pg16
    environment:
      - POSTGRES_DB=ai_db
      - POSTGRES_USER=aiuser
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    restart: unless-stopped

  chroma:
    image: chromadb/chroma:latest
    ports:
      - "8000:8000"
    volumes:
      - chroma_data:/chroma/chroma
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
  chroma_data:
```

### Kubernetes Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: boxlang-ai-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: boxlang-ai
  template:
    metadata:
      labels:
        app: boxlang-ai
    spec:
      containers:
      - name: app
        image: myregistry/boxlang-ai:latest
        ports:
        - containerPort: 8080
        env:
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: ai-secrets
              key: openai-api-key
        - name: CLAUDE_API_KEY
          valueFrom:
            secretKeyRef:
              name: ai-secrets
              key: claude-api-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: boxlang-ai-service
spec:
  selector:
    app: boxlang-ai
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

---

## 🔐 Security Hardening

### API Key Rotation

```javascript
class singleton {
    property name="apiKeys" type="struct";
    property name="currentKeyIndex" type="struct";

    function init() {
        variables.apiKeys = {}
        variables.currentKeyIndex = {}
        loadKeys()
        return this
    }

    function loadKeys() {
        // Load multiple keys per provider
        variables.apiKeys = {
            "openai": [
                getSystemSetting( "OPENAI_API_KEY_1" ),
                getSystemSetting( "OPENAI_API_KEY_2" )
            ],
            "claude": [
                getSystemSetting( "CLAUDE_API_KEY_1" ),
                getSystemSetting( "CLAUDE_API_KEY_2" )
            ]
        }

        // Initialize index
        for ( var provider in variables.apiKeys ) {
            variables.currentKeyIndex[ provider ] = 1
        }
    }

    function getAPIKey( required string provider ) {
        var keys = variables.apiKeys[ arguments.provider ]
        var index = variables.currentKeyIndex[ arguments.provider ]

        return keys[ index ]
    }

    function rotateKey( required string provider ) {
        lock name="keyrotation_#arguments.provider#" type="exclusive" timeout="5" {
            var keys = variables.apiKeys[ arguments.provider ]
            variables.currentKeyIndex[ arguments.provider ]++

            if ( variables.currentKeyIndex[ arguments.provider ] > arrayLen( keys ) ) {
                variables.currentKeyIndex[ arguments.provider ] = 1
            }

            writeLog( "Rotated API key for provider: #arguments.provider#" )
        }
    }
}
```

### Request Validation

```javascript
function validateAIRequest( required struct request ) {
    // Check for injection attempts
    if ( request.prompt.findNoCase( "ignore previous" ) ||
         request.prompt.findNoCase( "disregard instructions" ) ) {
        writeLog(
            "Potential prompt injection detected: #left( request.prompt, 100 )#",
            "security"
        )
        throw "Invalid request"
    }

    // Rate limiting per user
    if ( !checkRateLimit( session.userId ) ) {
        throw "Rate limit exceeded"
    }

    // Input size limits
    if ( len( request.prompt ) > 50000 ) {
        throw "Prompt exceeds maximum length"
    }

    return true
}
```

**More security details**: [Security Guide](security.md)

---

## 🔧 Operational Procedures

### Deployment Steps

1. **Pre-deployment**:
   ```bash
   # Run tests
   box task run test

   # Validate configuration
   box task run validate:config

   # Build artifacts
   box task run build
   ```

2. **Deploy**:
   ```bash
   # Blue-green deployment
   kubectl apply -f deployment-blue.yaml
   kubectl rollout status deployment/boxlang-ai-blue

   # Switch traffic
   kubectl patch service boxlang-ai -p '{"spec":{"selector":{"version":"blue"}}}'

   # Monitor
   kubectl logs -f deployment/boxlang-ai-blue
   ```

3. **Verify**:
   ```bash
   # Health check
   curl https://api.example.com/health

   # Smoke tests
   box task run test:smoke
   ```

4. **Rollback** (if needed):
   ```bash
   # Switch back to green
   kubectl patch service boxlang-ai -p '{"spec":{"selector":{"version":"green"}}}'
   ```

### Monitoring Alerts

Configure alerts for:

- ❗ **Error rate > 5%** - High error threshold
- ❗ **Response time > 10s** - Performance degradation
- ❗ **Cost > daily budget** - Budget exceeded
- ❗ **Circuit breaker open** - Service unavailable
- ❗ **Provider failover** - Backup provider activated
- ⚠️ **Memory usage > 80%** - Resource warning
- ⚠️ **Token usage spike** - Unusual activity

### Incident Response

**AI service outage**:
1. Check provider status pages
2. Attempt provider failover
3. Enable caching of recent responses
4. Activate maintenance mode if necessary
5. Notify users of degraded service
6. Document incident and resolution

---

## 📚 Additional Resources

- 🔐 [Security Guide](security.md)
- 📖 [Main Documentation](../README.md)
- 🎯 [Best Practices](../chatting/advanced-chatting.md)
- 💭 [Memory Systems](../main-components/memory.md)
- 🔮 [Vector Memory](../main-components/vector-memory.md)
- 🛠️ [Events System](events.md)

---

## ✅ Production Readiness Checklist

Before going live:

- [ ] API keys in secrets manager (not hardcoded)
- [ ] Error handling on all AI calls
- [ ] Rate limiting implemented
- [ ] Fallback providers configured
- [ ] Monitoring and alerting active
- [ ] Health check endpoint working
- [ ] Budget limits and tracking
- [ ] Load testing completed
- [ ] Security audit passed
- [ ] Backup and recovery tested
- [ ] Documentation updated
- [ ] Runbooks created
- [ ] On-call rotation established
