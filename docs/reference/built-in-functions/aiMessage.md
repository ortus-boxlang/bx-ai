# aiMessage

Build AI message structures fluently using a chainable API. Supports multiple roles (system, user, assistant, tool), template interpolation, and seamless integration with chat requests and pipelines.

## Syntax

```javascript
aiMessage(message)
```

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `message` | any | No | `null` | Initial message to add. Can be a string, struct, array, or AiMessage object |

### Message Format

Messages can be:
- **String**: Added as a user message
- **Struct**: With `role` and `content` keys (e.g., `{ role: "user", content: "Hello" }`)
- **Array**: Array of message structs
- **AiMessage**: Returns as-is (idempotent)

## Returns

Returns an `AiMessage` object with fluent API for:
- Adding messages: `system()`, `user()`, `assistant()`, `tool()`
- Building conversations: Chaining multiple role methods
- Template support: Variable interpolation with `${var}` syntax
- Pipeline integration: `to()`, `toDefaultModel()`, `transform()`
- Running: `run()`, `stream()` for direct execution

## Examples

### Basic Message Creation

```javascript
// Simple user message
msg = aiMessage( "What is BoxLang?" );

// Or build fluently
msg = aiMessage()
    .user( "What is BoxLang?" );
```

### System Instructions

```javascript
// Add system message for behavior control
msg = aiMessage()
    .system( "You are a helpful coding assistant" )
    .user( "Write a hello world function" );

response = aiChat( msg );
```

### Multi-Turn Conversation

```javascript
// Build conversation history
conversation = aiMessage()
    .system( "You are a technical writer" )
    .user( "Explain functions in BoxLang" )
    .assistant( "Functions in BoxLang are defined using the function keyword..." )
    .user( "Now show an example" );

response = aiChat( conversation );
```

### Template Interpolation

```javascript
// Use variables in messages
template = aiMessage()
    .system( "You are a ${role}" )
    .user( "Explain ${topic} in ${style} style" );

// Run with different inputs
response1 = template.toDefaultModel().run({
    role: "teacher",
    topic: "arrays",
    style: "simple"
});

response2 = template.toDefaultModel().run({
    role: "expert",
    topic: "closures",
    style: "technical"
});
```

### Pipeline Integration

```javascript
// Create message pipeline
pipeline = aiMessage()
    .system( "You are a concise summarizer" )
    .user( "Summarize: ${text}" )
    .toDefaultModel()
    .transform( r => r.content );

// Reuse pipeline
summary1 = pipeline.run({ text: "Long article..." });
summary2 = pipeline.run({ text: "Another article..." });
```

### Streaming Messages

```javascript
// Stream response from message
msg = aiMessage()
    .system( "You are a storyteller" )
    .user( "Tell a short story about ${topic}" );

msg.toDefaultModel().stream(
    onChunk: ( chunk ) => writeOutput( chunk ),
    input: { topic: "dragons" }
);
```

### Tool Messages

```javascript
// Add tool/function call results
msg = aiMessage()
    .user( "What's the weather in Paris?" )
    .tool( "getWeather", { result: "22°C, sunny" } );

response = aiChat( msg );
```

### Dynamic Message Building

```javascript
// Build messages programmatically
builder = aiMessage().system( "You are helpful" );

// Add user messages based on conditions
if ( includeContext ) {
    builder.user( "Context: ${context}" );
}

builder.user( "Question: ${question}" );

response = aiChat( builder, {
    context: "BoxLang docs",
    question: "What is a BIF?"
});
```

### Role-Based Messages

```javascript
// Use dynamic role methods
msg = aiMessage()
    .system( "You are an AI assistant" )
    .user( "Hello!" )
    .assistant( "Hi! How can I help?" )
    .user( "Tell me about AI" );

// Supports: system, user, assistant, tool
```

### Reusable Templates

```javascript
// Create reusable message template
baseTemplate = aiMessage()
    .system( "You are a ${role}" )
    .user( "${prompt}" );

// Teacher use case
teacherMsg = baseTemplate.clone()
    .run({
        role: "teacher",
        prompt: "Explain loops"
    });

// Expert use case
expertMsg = baseTemplate.clone()
    .run({
        role: "expert",
        prompt: "Analyze algorithm complexity"
    });
```

### Transformation Chains

```javascript
// Message with multiple transformations
chain = aiMessage()
    .user( "List 3 ${items}" )
    .toDefaultModel()
    .transform( r => r.content )
    .transform( content => content.uCase() )
    .transform( content => "RESULT: " & content );

output = chain.run({ items: "colors" });
println( output ); // "RESULT: RED, BLUE, GREEN"
```

### Multi-Modal Messages

```javascript
// Image with text (for vision models)
msg = aiMessage()
    .user({
        content: "What's in this image?",
        images: [ "https://example.com/photo.jpg" ]
    });

response = aiChat( msg, { provider: "openai", model: "gpt-4-vision" });
```

### Conversation Context

```javascript
// Maintain conversation state
conversation = aiMessage().system( "You are a chatbot" );

// User turn 1
conversation.user( "Hello" );
reply1 = aiChat( conversation );
conversation.assistant( reply1 );

// User turn 2
conversation.user( "Tell me a joke" );
reply2 = aiChat( conversation );
conversation.assistant( reply2 );

// Full history maintained
```

### Inspect Messages

```javascript
// Build and inspect
msg = aiMessage()
    .system( "You are helpful" )
    .user( "Hello" );

// Get message array
messages = msg.getMessages();
println( "Total messages: #messages.len()#" );

messages.each( m => {
    println( "#m.role#: #m.content#" );
});
```

### Custom Providers

```javascript
// Send to specific provider
msg = aiMessage()
    .user( "What is AI?" )
    .to( aiModel( "claude" ) );

response = msg.run();
```

### Advanced Pipeline

```javascript
// Complex pipeline with error handling
pipeline = aiMessage()
    .system( "You are a data processor" )
    .user( "Process: ${data}" )
    .toDefaultModel()
    .transform( r => {
        try {
            return jsonDeserialize( r.content );
        } catch ( any e ) {
            return { error: "Invalid JSON" };
        }
    });

result = pipeline.run({ data: "[1,2,3]" });
```

## Notes

- 🔗 **Fluent API**: All role methods return the message object for chaining
- 🎨 **Templates**: Use `${variable}` for dynamic content interpolation
- 🔄 **Reusable**: Message objects are reusable across multiple requests
- 🎯 **Type Safe**: Automatic validation of message structure
- 🚀 **Pipeline Ready**: Seamlessly integrates with `aiModel()` and `aiTransform()`
- 📝 **Dynamic Roles**: Uses `onMissingMethod` for flexible role names
- 🎪 **Events**: Fires `onAIMessageCreate` event for interceptors

## Related Functions

- [`aiChat()`](aiChat.md) - Send messages to AI providers
- [`aiChatRequest()`](aiChatRequest.md) - Build complete request objects
- [`aiModel()`](aiModel.md) - Create model runnables for pipelines
- [`aiTransform()`](aiTransform.md) - Transform message outputs
- [`aiAgent()`](aiAgent.md) - Create agents with message handling

## Best Practices

✅ **Use system messages** - Set behavior and context upfront

✅ **Template reusable prompts** - Create templates with `${vars}` for flexibility

✅ **Chain fluently** - Build complex messages in one expression

✅ **Maintain conversation** - Keep message history for multi-turn chats

✅ **Use pipelines** - Combine with `toDefaultModel()` for reusable workflows

❌ **Don't nest messages** - Keep message structure flat (one level of role/content)

❌ **Don't mutate shared messages** - Clone before modifying reused templates

❌ **Don't overload system** - Keep system message concise and focused
