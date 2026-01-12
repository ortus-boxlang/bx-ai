# aiChatRequest

Create a reusable AI Chat Request object that can be sent to any AI service provider. This is useful for building requests programmatically, managing conversations, or testing AI workflows.

## Syntax

```javascript
aiChatRequest(messages, params, options, headers)
```

## Parameters

| Parameter  | Type   | Required | Default | Description                                                                                                    |
| ---------- | ------ | -------- | ------- | -------------------------------------------------------------------------------------------------------------- |
| `messages` | any    | No       | `null`  | Initial message(s) to add. Can be a string, struct, array of messages, or AiMessage object                     |
| `params`   | struct | No       | `{}`    | Request parameters for the AI provider (e.g., `{ temperature: 0.5, max_tokens: 100, model: "gpt-3.5-turbo" }`) |
| `options`  | struct | No       | `{}`    | Request options (e.g., `{ provider: "openai", apiKey: "...", returnFormat: "single" }`)                        |
| `headers`  | struct | No       | `{}`    | Custom HTTP headers to include with the request                                                                |

### Message Format

Messages can be:

* **String**: Added as a user message
* **Struct**: With `role` and `content` keys
* **Array**: Array of message structs
* **AiMessage**: Fluent message object

### Options Structure

| Option         | Type    | Default      | Description                                            |
| -------------- | ------- | ------------ | ------------------------------------------------------ |
| `provider`     | string  | (config)     | The AI provider to use (openai, claude, etc.)          |
| `apiKey`       | string  | (config/env) | API key for the provider                               |
| `returnFormat` | string  | `"single"`   | Response format: "single", "all", "raw", "json", "xml" |
| `timeout`      | numeric | `30`         | Request timeout in seconds                             |
| `logResponse`  | boolean | `false`      | Log the AI response                                    |
| `logRequest`   | boolean | `false`      | Log the AI request                                     |

## Returns

Returns an `AiRequest` object with fluent API for:

* Adding messages: `addMessage()`, `addSystemMessage()`, `addUserMessage()`
* Setting parameters: `setParams()`, `setOptions()`, `setHeaders()`
* Inspecting: `getMessages()`, `getParams()`, `getOptions()`
* Sending: Use with `aiService().invoke(request)` or `aiChat()` equivalents

## Examples

### Basic Request Object

```javascript
// Create empty request and build it up
request = aiChatRequest()
    .addSystemMessage( "You are a helpful assistant" )
    .addUserMessage( "What is BoxLang?" )
    .setParams({ temperature: 0.7 })
    .setOptions({ provider: "openai" });

// Send to service
service = aiService( "openai" );
response = service.invoke( request );
println( response );
```

### Request with Initial Message

```javascript
// Create request with initial message
request = aiChatRequest( "Explain quantum computing" )
    .setParams({
        temperature: 0.5,
        max_tokens: 500
    });

// Send to default provider
response = aiChat( request );
```

### Multi-Turn Conversation

```javascript
// Build conversation history
request = aiChatRequest()
    .addSystemMessage( "You are a technical writer" )
    .addUserMessage( "Write a function to reverse a string" )
    .addAssistantMessage( "Here's a function: function reverse(str) { return str.reverse(); }" )
    .addUserMessage( "Now add error handling" );

response = aiChat( request );
```

### Reusable Request Template

```javascript
// Create template for similar requests
baseRequest = aiChatRequest()
    .setSystemMessage( "You are a code reviewer" )
    .setParams({
        temperature: 0.3,
        max_tokens: 1000
    });

// Clone and customize for each use
review1 = baseRequest.clone()
    .addUserMessage( "Review this code: ${code1}" );

review2 = baseRequest.clone()
    .addUserMessage( "Review this code: ${code2}" );
```

### Different Providers

```javascript
// Same request to multiple providers
question = "What is the capital of France?";

openAIRequest = aiChatRequest( question )
    .setOptions({ provider: "openai" });

claudeRequest = aiChatRequest( question )
    .setOptions({ provider: "claude" });

ollamaRequest = aiChatRequest( question )
    .setOptions({ provider: "ollama" });

// Compare responses
openAIResponse = aiChat( openAIRequest );
claudeResponse = aiChat( claudeRequest );
ollamaResponse = aiChat( ollamaRequest );
```

### Custom Headers

```javascript
// Add custom headers for tracking
request = aiChatRequest( "Hello!" )
    .setHeaders({
        "X-Request-ID": createUUID(),
        "X-User-ID": session.userID
    });

response = aiChat( request );
```

### Testing and Debugging

```javascript
// Enable logging for debugging
request = aiChatRequest( "Test message" )
    .setOptions({
        logRequest: true,
        logResponse: true,
        timeout: 60
    });

response = aiChat( request );
```

### Working with AiMessage

```javascript
// Use fluent message builder
messages = aiMessage()
    .system( "You are a poet" )
    .user( "Write a haiku about ${topic}" );

request = aiChatRequest( messages )
    .setParams({ temperature: 1.0 });

// Run with different topics
haiku1 = aiChat( request, { topic: "mountains" } );
haiku2 = aiChat( request, { topic: "ocean" } );
```

### Structured Output Request

```javascript
// Request JSON response
request = aiChatRequest( "List 3 programming languages" )
    .setParams({
        response_format: { type: "json_object" }
    })
    .setOptions({
        returnFormat: "json"
    });

languages = aiChat( request );
println( languages.languages ); // Array of languages
```

## Notes

* 📦 **Reusability**: Request objects are reusable - great for templates and testing
* 🔄 **Immutability**: Methods return the request object for fluent chaining
* 🎯 **Provider Agnostic**: Same request works with any provider by changing options
* 🔍 **Debugging**: Enable logging options to inspect request/response data
* 🎨 **Flexibility**: Build requests incrementally or clone and customize templates
* 🚀 **Events**: Fires `onAIRequestCreate` event for interceptor integration

## Related Functions

* [`aiChat()`](aichat.md) - Send chat requests synchronously
* [`aiChatAsync()`](aichatasync.md) - Send chat requests asynchronously
* [`aiChatStream()`](aichatstream.md) - Stream chat responses
* [`aiMessage()`](aimessage.md) - Build message structures fluently
* [`aiService()`](aiservice.md) - Get AI service provider instances

## Best Practices

✅ **Use for complex workflows** - When you need full control over request lifecycle

✅ **Template common requests** - Create base requests and clone for variations

✅ **Separate concerns** - Build requests separately from sending them

✅ **Test without API calls** - Inspect request structure before sending

✅ **Add request tracking** - Use custom headers for logging and debugging

❌ **Don't overcomplicate simple cases** - Use `aiChat()` directly for one-off requests

❌ **Don't modify shared requests** - Clone before customizing to avoid side effects
