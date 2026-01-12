# aiChat

Initiate an AI chat conversation against the default or custom AI Provider with a simple, synchronous interface.

## Syntax

```javascript
aiChat(messages, params, options)
```

## Parameters

| Parameter  | Type   | Required | Description                                                                                                    |
| ---------- | ------ | -------- | -------------------------------------------------------------------------------------------------------------- |
| `messages` | any    | Yes      | The messages to pass to the AI model. Can be a string, struct, array of messages, or AiMessage object          |
| `params`   | struct | No       | Request parameters for the AI provider (e.g., `{ temperature: 0.5, max_tokens: 100, model: "gpt-3.5-turbo" }`) |
| `options`  | struct | No       | Request options for controlling behavior                                                                       |

### Options Structure

| Option         | Type    | Default      | Description                                            |
| -------------- | ------- | ------------ | ------------------------------------------------------ |
| `provider`     | string  | (config)     | The AI provider to use (openai, claude, etc.)          |
| `apiKey`       | string  | (config/env) | API key for the provider                               |
| `returnFormat` | string  | `"single"`   | Response format: "single", "all", "raw", "json", "xml" |
| `timeout`      | numeric | `30`         | Request timeout in seconds                             |
| `logResponse`  | boolean | `false`      | Log the AI response to ai.log                          |
| `logRequest`   | boolean | `false`      | Log the AI request to ai.log                           |

## Returns

Returns the AI response based on `returnFormat`:

* **"single"** (default): String content of the first message
* **"all"**: Array of all response messages
* **"raw"**: Complete API response with metadata
* **"json"**: Parsed JSON object from response
* **"xml"**: Parsed XML document from response

## Examples

### Simple Chat

```javascript
// Basic string message
response = aiChat( "What is BoxLang?" );
println( response );
// Returns: "BoxLang is a modern dynamic JVM language..."
```

### With Parameters

```javascript
// Control AI behavior with parameters
response = aiChat(
    "Write a haiku about coding",
    { temperature: 0.9, max_tokens: 50 }
);
```

### Multiple Messages

```javascript
// Conversation context with array
messages = [
    { role: "system", content: "You are a helpful coding assistant" },
    { role: "user", content: "How do I reverse a string in BoxLang?" }
];

response = aiChat( messages );
```

### Using AiMessage Object

```javascript
// Fluent message building
conversation = aiMessage()
    .system( "You are a technical writer" )
    .user( "Explain dependency injection" )
    .assistant( "Dependency injection is a design pattern..." )
    .user( "Show me an example" );

response = aiChat( conversation );
```

### Different Providers

```javascript
// Use Claude instead of default
response = aiChat(
    "Explain quantum computing",
    {},
    { provider: "claude", model: "claude-3-opus-20240229" }
);

// Use local Ollama
response = aiChat(
    "Hello world",
    { model: "llama2" },
    { provider: "ollama" }
);
```

### Return Formats

```javascript
// Get full response metadata
fullResponse = aiChat(
    "Tell me a joke",
    {},
    { returnFormat: "raw" }
);
println( "Tokens used: #fullResponse.usage.total_tokens#" );

// Parse JSON response
userData = aiChat(
    "Return user data as JSON: name=John, age=30",
    {},
    { returnFormat: "json" }
);
println( userData.name ); // "John"
```

### Structured Output

```javascript
// Define expected structure
class Person {
    property name="firstName" type="string";
    property name="lastName" type="string";
    property name="age" type="numeric";
}

person = aiChat(
    "Extract person info: John Doe is 30 years old",
    {},
    { returnFormat: new Person() }
);

println( person.firstName ); // "John"
```

### With Logging

```javascript
// Debug request/response
response = aiChat(
    "What's 2+2?",
    {},
    {
        logRequest: true,
        logResponse: true
    }
);
// Check logs/ai.log for details
```

## Notes

* **Defaults to "single" format**: Unlike pipelines, `aiChat()` defaults to returning just the content string for convenience
* **Automatic provider selection**: If no provider specified, uses module configuration default
* **API key detection**: Automatically detects keys from environment variables like `OPENAI_API_KEY`, `CLAUDE_API_KEY`, etc.
* **Synchronous**: Blocks until response received. Use `aiChatAsync()` for async execution
* **Message normalization**: Simple strings are automatically converted to `{ role: "user", content: "..." }` format
* **Parameter merging**: Passed params are merged with module default params (passed params take precedence)

## Related Functions

* [`aiChatAsync()`](aichatasync.md) - Asynchronous version returning a Future
* [`aiChatStream()`](aichatstream.md) - Streaming version with callback
* [`aiChatRequest()`](aichatrequest.md) - Create reusable request objects
* [`aiMessage()`](aimessage.md) - Build complex message structures
* [`aiAgent()`](aiagent.md) - Create agents with memory and tools

## Error Handling

```javascript
try {
    response = aiChat( "Hello" );
} catch( "InvalidArgument" e ) {
    // Invalid returnFormat or parameters
    writeLog( e.message );
} catch( "ProviderError" e ) {
    // API error from provider
    writeLog( "Provider failed: #e.detail#" );
}
```
