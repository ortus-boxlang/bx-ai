---
description: "Take full control of AI interactions with service-level chatting in BoxLang, ideal for advanced use cases requiring custom configurations and multiple providers."
icon: bus
---

# Service-Level Chatting

Take full control of AI interactions by working directly with service objects. Perfect for advanced scenarios requiring custom configuration, multiple providers, or direct API access.

## Creating Services

### Basic Service Creation

```java
// Default API key from config
service = aiService( "openai" )

// Custom API key
service = aiService( "openai", "sk-your-key-here" )

// Different providers
openai = aiService( "openai" )
claude = aiService( "claude" )
gemini = aiService( "gemini" )
mistral = aiService( "mistral" )
ollama = aiService( "ollama" )
```

### Service Configuration

```java
service = aiService( "openai" )
    .setChatURL( "https://api.openai.com/v1/chat/completions" )
    .setTimeout( 60 )
    .defaults( {
        model: "gpt-4",
        temperature: 0.7,
        max_tokens: 1000
    } )
```

## Building Chat Requests

Use `aiChatRequest()` for detailed request control:

### Basic Request

```java
request = aiChatRequest( "Hello, world!" )
response = service.invoke( request )
```

### With Messages Array

```java
request = aiChatRequest()
    .setMessages( [
        { role: "system", content: "You are helpful" },
        { role: "user", content: "Explain AI" }
    ] )

response = service.invoke( request )
```

### With Parameters

```java
request = aiChatRequest( "Write a story" )
    .setParams( {
        model: "gpt-4",
        temperature: 0.9,
        max_tokens: 500,
        top_p: 0.95
    } )

response = service.invoke( request )
```

### Complete Request

```java
request = aiChatRequest()
    .setMessages( [
        { role: "user", content: "Hello" }
    ] )
    .setParams( {
        model: "gpt-4",
        temperature: 0.7
    } )
    .setHeaders( {
        "X-Custom-Header": "value"
    } )
    .setOptions( {
        returnFormat: "raw",
        logRequest: true
    } )

response = service.invoke( request )
```

## Service Operations

### Invoke (Synchronous)

```java
service = aiService( "openai" )
request = aiChatRequest( "What is BoxLang?" )

response = service.invoke( request )
println( response )
```

### Invoke Stream

```java
service = aiService( "openai" )
request = aiChatRequest( "Tell me a story" )
    .setStream( true )

service.invokeStream( request, ( chunk ) => {
    content = chunk.choices?.first()?.delta?.content ?: ""
    print( content )
} )
```

## Custom Headers

Add authentication, tracking, or custom headers:

```java
request = aiChatRequest( "Hello" )
    .setHeaders( {
        "Authorization": "Bearer custom-token",
        "X-Request-ID": createUUID(),
        "X-User-ID": "user123"
    } )

response = service.invoke( request )
```

## Multiple Services

Manage multiple providers simultaneously:

```java
class {
    property name="openai";
    property name="claude";
    property name="local";

    function init() {
        variables.openai = aiService( "openai" )
        variables.claude = aiService( "claude" )
        variables.local = aiService( "ollama" )
        return this
    }

    function askAll( required string question ) {
        request = aiChatRequest( arguments.question )

        return {
            openai: variables.openai.invoke( request ),
            claude: variables.claude.invoke( request ),
            local: variables.local.invoke( request )
        }
    }

    function askBest( required string question ) {
        // Use fastest or preferred provider
        try {
            return variables.openai.invoke( aiChatRequest( arguments.question ) )
        } catch( any e ) {
            // Fallback to Claude
            return variables.claude.invoke( aiChatRequest( arguments.question ) )
        }
    }
}
```

## Advanced Patterns

### Retry Logic

```java
function invokeWithRetry( required service, required request, maxRetries = 3 ) {
    for( var i = 1; i <= maxRetries; i++ ) {
        try {
            return arguments.service.invoke( arguments.request )
        } catch( any e ) {
            if( i == maxRetries ) {
                throw( e )
            }
            sleep( 1000 * i )  // Exponential backoff
        }
    }
}

// Usage
response = invokeWithRetry( service, request )
```

### Request Queue

```java
class {
    property name="service";
    property name="queue" type="array";

    function init( required service ) {
        variables.service = arguments.service
        variables.queue = []
        return this
    }

    function enqueue( required request ) {
        variables.queue.append( arguments.request )
    }

    function process() {
        results = []
        for( request in variables.queue ) {
            results.append( variables.service.invoke( request ) )
        }
        variables.queue = []
        return results
    }
}
```

### Load Balancer

```java
class {
    property name="services" type="array";
    property name="currentIndex" type="numeric" default="1";

    function init( required array providers ) {
        variables.services = arguments.providers.map( p => aiService( p ) )
        return this
    }

    function invoke( required request ) {
        service = variables.services[ variables.currentIndex ]
        variables.currentIndex = variables.currentIndex < variables.services.len()
            ? variables.currentIndex + 1
            : 1

        return service.invoke( arguments.request )
    }
}

// Usage
balancer = new LoadBalancer( [ "openai", "claude", "gemini" ] )
response1 = balancer.invoke( request1 )  // Uses OpenAI
response2 = balancer.invoke( request2 )  // Uses Claude
response3 = balancer.invoke( request3 )  // Uses Gemini
```

## Provider-Specific Features

### OpenAI

```java
service = aiService( "openai" )
request = aiChatRequest( "Hello" )
    .setParams( {
        model: "gpt-4",
        response_format: { type: "json_object" },
        seed: 12345,  // Deterministic responses
        user: "user123"  // Track usage
    } )
```

### Claude

```java
service = aiService( "claude" )
request = aiChatRequest()
    .setMessages( [
        { role: "user", content: "Long document analysis..." }
    ] )
    .setParams( {
        model: "claude-3-opus-20240229",
        max_tokens: 4096  // Claude requires this
    } )
```

### Ollama

```java
service = aiService( "ollama" )
    .setChatURL( "http://localhost:11434" )

request = aiChatRequest( "Hello" )
    .setParams( {
        model: "llama3.2",
        temperature: 0.7
    } )
```

## Request Options

### Return Formats

```java
// Single string
request = aiChatRequest( "Hello" )
    .setOptions( { returnFormat: "single" } )

// All messages
request = aiChatRequest( "Hello" )
    .setOptions( { returnFormat: "all" } )

// Raw API response
request = aiChatRequest( "Hello" )
    .setOptions( { returnFormat: "raw" } )
```

### Logging

```java
request = aiChatRequest( "Hello" )
    .setOptions( {
        logRequest: true,
        logResponse: true,
        logRequestToConsole: true
    } )
```

## Practical Examples

### Cost Tracker

```java
class {
    property name="service";
    property name="totalTokens" default="0";

    function init( required service ) {
        variables.service = arguments.service
        return this
    }

    function invoke( required request ) {
        arguments.request.setOptions( { returnFormat: "raw" } )
        response = variables.service.invoke( arguments.request )

        variables.totalTokens += response.usage.total_tokens

        return response.choices[1].message.content
    }

    function getCost( costPerToken = 0.00002 ) {
        return variables.totalTokens * arguments.costPerToken
    }
}
```

### Response Cache

```java
class {
    property name="service";
    property name="cache" type="struct";

    function init( required service ) {
        variables.service = arguments.service
        variables.cache = {}
        return this
    }

    function invoke( required request ) {
        cacheKey = hash( serializeJSON( arguments.request ) )

        if( structKeyExists( variables.cache, cacheKey ) ) {
            return variables.cache[ cacheKey ]
        }

        response = variables.service.invoke( arguments.request )
        variables.cache[ cacheKey ] = response

        return response
    }
}
```

### A/B Testing

```java
function abTest( required string question, modelA, modelB ) {
    serviceA = aiService( "openai" )
    serviceB = aiService( "claude" )

    requestA = aiChatRequest( arguments.question )
        .setParams( { model: arguments.modelA } )
        .setOptions( { returnFormat: "raw" } )

    requestB = aiChatRequest( arguments.question )
        .setParams( { model: arguments.modelB } )
        .setOptions( { returnFormat: "raw" } )

    responseA = serviceA.invoke( requestA )
    responseB = serviceB.invoke( requestB )

    return {
        modelA: {
            response: responseA.choices[1].message.content,
            tokens: responseA.usage.total_tokens,
            model: responseA.model
        },
        modelB: {
            response: responseB.choices[1].message.content,
            tokens: responseB.usage.total_tokens,
            model: responseB.model
        }
    }
}
```

## Best Practices

1. **Reuse Service Objects**: Create once, use many times
2. **Handle Errors**: Wrap invoke() in try/catch
3. **Set Timeouts**: Prevent hanging requests
4. **Use Raw Format**: For detailed debugging and cost tracking
5. **Cache Responses**: Save money on repeated questions
6. **Implement Retries**: Handle transient failures
7. **Monitor Usage**: Track tokens and costs

## Next Steps

- **[Pipeline Overview](../main-components/overview.md)** - Learn about AI pipelines
- **[Working with Models](../main-components/models.md)** - Services in pipelines
- **[Basic Chatting](basic-chatting.md)** - Back to basics
