# Lesson 10: Production Deployment

**Duration:** 90 minutes
**Prerequisites:** Lessons 1-9 completed

## Learning Objectives

By the end of this lesson, you will:

- Handle errors gracefully in production
- Implement caching for performance
- Monitor AI usage and costs
- Optimize for scale
- Follow security best practices
- Deploy BoxLang AI applications

---

## Part 1: Error Handling

### Robust Error Handling

```java
function safeAIChat( prompt, retries = 3 ) {
    for ( i = 1; i <= retries; i++ ) {
        try {
            return aiChat( prompt )
        } catch( any e ) {
            if ( i == retries ) {
                // Log error
                logger.error( "AI call failed: #e.message#" )

                // Return fallback
                return "I'm having trouble processing that. Please try again."
            }

            // Wait before retry (exponential backoff)
            sleep( 2 ^ i * 1000 )
        }
    }
}
```

### Graceful Degradation

```java
function smartAICall( prompt ) {
    try {
        // Try primary provider
        return aiChat( prompt, {}, { provider: "openai" } )
    } catch( any e ) {
        try {
            // Fallback to secondary
            return aiChat( prompt, {}, { provider: "claude" } )
        } catch( any e2 ) {
            // Final fallback to local
            return aiChat( prompt, {}, { provider: "ollama" } )
        }
    }
}
```

---

## Part 2: Caching

### Response Caching

```java
cache = {}

function cachedAIChat( prompt ) {
    cacheKey = hash( prompt, "MD5" )

    if ( cache.keyExists( cacheKey ) ) {
        return cache[ cacheKey ]
    }

    answer = aiChat( prompt )
    cache[ cacheKey ] = answer

    return answer
}
```

### Time-Based Cache

```java
cache = {}

function cachedAIChatWithTTL( prompt, ttlMinutes = 60 ) {
    cacheKey = hash( prompt, "MD5" )

    if ( cache.keyExists( cacheKey ) ) {
        entry = cache[ cacheKey ]
        age = dateDiff( "n", entry.timestamp, now() )

        if ( age < ttlMinutes ) {
            return entry.response
        }
    }

    answer = aiChat( prompt )
    cache[ cacheKey ] = {
        response: answer,
        timestamp: now()
    }

    return answer
}
```

---

## Part 3: Monitoring

### Usage Tracking

```java
component {
    property name="usageStats" default="{}";

    function trackUsage( provider, model, tokens, cost ) {
        key = "#provider#_#model#"

        if ( !variables.usageStats.keyExists( key ) ) {
            variables.usageStats[ key ] = {
                calls: 0,
                tokens: 0,
                cost: 0
            }
        }

        variables.usageStats[ key ].calls++
        variables.usageStats[ key ].tokens += tokens
        variables.usageStats[ key ].cost += cost
    }

    function getStats() {
        return variables.usageStats
    }
}
```

### Cost Monitoring

```java
function aiChatWithCostTracking( prompt ) {
    startTime = getTickCount()

    answer = aiChat( prompt )

    elapsed = getTickCount() - startTime

    // Calculate cost
    tokens = TokenCounter::count( prompt ) + TokenCounter::count( answer )
    cost = tokens / 1000000 * 0.002

    // Log metrics
    logger.info( "AI call: #tokens# tokens, $#cost#, #elapsed#ms" )

    // Alert if expensive
    if ( cost > 0.10 ) {
        sendAlert( "Expensive AI call: $#cost#" )
    }

    return answer
}
```

---

## Part 4: Performance Optimization

### Batch Processing

```java
function processBatch( prompts ) {
    results = []

    prompts.each( prompt => {
        // Process with delay to avoid rate limits
        results.append( aiChat( prompt ) )
        sleep( 100 )  // 100ms delay
    } )

    return results
}
```

### Async Processing

```java
function asyncAIChat( prompt, callback ) {
    thread name="ai_#createUUID()#" {
        try {
            answer = aiChat( prompt )
            callback( answer, null )
        } catch( any e ) {
            callback( null, e )
        }
    }
}

// Usage
asyncAIChat( "Hello", ( answer, error ) => {
    if ( !isNull( error ) ) {
        println( "Error: #error.message#" )
    } else {
        println( "Answer: #answer#" )
    }
} )
```

---

## Part 5: Security

### API Key Protection

```bash
# .env (NEVER commit!)
OPENAI_API_KEY=sk-...

# .gitignore
.env
*.key
```

### Input Sanitization

```java
function sanitizeInput( prompt ) {
    // Remove potential injection attempts
    cleaned = prompt
        .reReplace( "<script[^>]*>.*?</script>", "", "ALL" )
        .reReplace( "javascript:", "", "ALL" )
        .trim()

    // Limit length
    if ( len( cleaned ) > 5000 ) {
        cleaned = cleaned.left( 5000 )
    }

    return cleaned
}
```

### Rate Limiting

```java
component {
    property name="requestCounts" default="{}";
    property name="maxRequestsPerMinute" default="60";

    function checkRateLimit( userId ) {
        key = "#userId#_#dateFormat( now(), 'yyyy-mm-dd-HH-mm' )#"

        if ( !variables.requestCounts.keyExists( key ) ) {
            variables.requestCounts[ key ] = 0
        }

        variables.requestCounts[ key ]++

        if ( variables.requestCounts[ key ] > variables.maxRequestsPerMinute ) {
            throw( "Rate limit exceeded. Please try again later." )
        }
    }
}
```

---

## Part 6: Production Checklist

### Pre-Deployment

- [ ] Environment variables configured
- [ ] Error handling implemented
- [ ] Logging configured
- [ ] Caching strategy in place
- [ ] Rate limiting enabled
- [ ] Cost alerts configured
- [ ] Security review completed
- [ ] Performance testing done
- [ ] Monitoring dashboard ready
- [ ] Backup provider configured

### Deployment

- [ ] Use production API keys
- [ ] Enable HTTPS
- [ ] Configure timeouts
- [ ] Set up health checks
- [ ] Enable auto-scaling
- [ ] Configure CDN (if applicable)
- [ ] Set up error tracking (e.g., Sentry)
- [ ] Enable request logging

### Post-Deployment

- [ ] Monitor error rates
- [ ] Track AI costs
- [ ] Review performance metrics
- [ ] Collect user feedback
- [ ] Optimize based on usage patterns

---

## Examples to Run

### 1. `error-handling.bxs`
Robust error handling patterns

### 2. `caching-strategies.bxs`
Response caching implementations

### 3. `monitoring.bxs`
Usage tracking and alerting

### 4. `production-ready.bxs`
Complete production-ready setup

---

## Lab Exercise: Production-Ready AI Service

**File:** `labs/production-service.bxs`

**Objective:**
Build a production-ready AI service with all best practices.

**Requirements:**
1. Comprehensive error handling
2. Response caching with TTL
3. Usage tracking and cost monitoring
4. Rate limiting
5. Security measures
6. Health check endpoint
7. Graceful degradation
8. Detailed logging

---

## Final Project Ideas

### 1. Customer Support Bot
- Multi-turn conversations
- Tool-based ticket creation
- Session memory
- Streaming responses
- Production-ready deployment

### 2. Code Review Assistant
- File analysis
- Pattern detection
- Suggestion generation
- Cost optimization
- Batch processing

### 3. Document Intelligence System
- RAG pipeline
- Multiple document formats
- Caching strategy
- Search capabilities
- Production deployment

### 4. Research Assistant
- Multi-provider fallback
- Web search tools
- Summary generation
- Citation tracking
- Monitoring dashboard

---

## Key Takeaways

✅ Production requires robust error handling
✅ Caching reduces costs and improves speed
✅ Monitor usage, costs, and performance
✅ Security is critical (API keys, input validation)
✅ Use multiple providers for reliability
✅ Plan for scale from the start

---

## Congratulations! 🎉

You've completed the BoxLang AI training course! You now know:

- AI fundamentals and tokens
- Message roles and conversations
- Multiple AI providers
- Model parameters
- Streaming responses
- Function calling (tools)
- Memory systems
- Autonomous agents
- Complex pipelines
- Production deployment

---

## Next Steps

1. **Build Projects** - Apply what you've learned
2. **Read Documentation** - Dive deeper into specific topics
3. **Join Community** - Share and learn from others
4. **Contribute** - Help improve BoxLang AI
5. **Stay Updated** - AI evolves rapidly

---

## Additional Resources

- [BoxLang AI Documentation](../../docs/README.md)
- [GitHub Repository](https://github.com/ortus-boxlang/bx-ai)
- [BoxLang Discord](https://boxlang.io/community)
- [AI Best Practices](https://platform.openai.com/docs/guides/best-practices)

---

**Thank you for learning with us! Happy coding! 🚀**
