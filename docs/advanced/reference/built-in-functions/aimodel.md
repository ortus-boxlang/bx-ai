# aiModel

Create an AI Model runnable that wraps a service provider for use in pipelines. This is the pipeline-friendly version of `aiService()`, designed for composable AI operations.

## Syntax

```javascript
aiModel(provider, apiKey, tools)
```

## Parameters

| Parameter  | Type   | Required | Default      | Description                                                             |
| ---------- | ------ | -------- | ------------ | ----------------------------------------------------------------------- |
| `provider` | string | No       | (config)     | The provider to use (openai, claude, ollama, etc.)                      |
| `apiKey`   | string | No       | (config/env) | Optional API key override                                               |
| `tools`    | any    | No       | `[]`         | ITool instance or array of Tool instances for tool-augmented generation |

## Returns

Returns an `AiModel` runnable with:

* Pipeline integration: `run()`, `stream()`, `to()` methods
* Tool binding: `bindTools()` for function calling
* Service wrapping: Access to underlying service provider
* IAiRunnable interface: Compatible with all runnable pipelines

## Examples

### Basic Model Pipeline

```javascript
// Simple model in pipeline
result = aiMessage( "What is BoxLang?" )
    .to( aiModel( "openai" ) )
    .run();

println( result.content );
```

### With Transformation

```javascript
// Chain model with transformations
pipeline = aiMessage( "List 3 colors" )
    .to( aiModel( "openai" ) )
    .to( aiTransform( r => r.content.uCase() ) );

result = pipeline.run();
println( result ); // "RED, GREEN, BLUE"
```

### Default Model

```javascript
// Use configured default provider
model = aiModel();

// Or use convenience method
result = aiMessage( "Hello" )
    .toDefaultModel()
    .run();
```

### Specific Provider

```javascript
// Use Claude
claudeModel = aiModel( "claude" );

result = aiMessage( "Explain AI" )
    .to( claudeModel )
    .run();

// Use Ollama (local)
ollamaModel = aiModel( "ollama" );

result = aiMessage( "What is AI?" )
    .to( ollamaModel )
    .run();
```

### With API Key Override

```javascript
// Override API key
model = aiModel( "openai", "sk-custom-key-123" );

result = aiMessage( "Hello" )
    .to( model )
    .run();
```

### Template with Model

```javascript
// Reusable template
greeter = aiMessage()
    .system( "You are a ${style} greeter" )
    .user( "Greet ${name}" )
    .to( aiModel( "openai" ) )
    .to( aiTransform( r => r.content ) );

// Run with different inputs
greeting1 = greeter.run({ style: "formal", name: "Dr. Smith" });
greeting2 = greeter.run({ style: "casual", name: "Bob" });
```

### Multi-Step Pipeline

```javascript
// Complex pipeline
analyzer = aiMessage()
    .system( "You are a sentiment analyzer" )
    .user( "Analyze: ${text}" )
    .to( aiModel( "openai" ) )
    .to( aiTransform( r => r.content ) )
    .to( aiTransform( text => text.trim() ) )
    .to( aiTransform( text => {
        sentiment: text,
        timestamp: now()
    }));

result = analyzer.run({ text: "This product is amazing!" });
```

### Streaming with Model

```javascript
// Stream from model
storyteller = aiMessage()
    .user( "Tell a story about ${topic}" )
    .to( aiModel( "openai" ) );

storyteller.stream(
    onChunk: ( chunk ) => writeOutput( chunk ),
    input: { topic: "dragons" }
);
```

### Model with Tools

```javascript
// Bind tools to model
searchTool = aiTool(
    name: "search",
    description: "Search the web",
    handler: ( query ) => searchEngine.search( query )
);

model = aiModel( "openai" )
    .bindTools( searchTool );

result = aiMessage( "Find latest BoxLang news" )
    .to( model )
    .run();
```

### Multiple Tool Binding

```javascript
// Multiple tools
tools = [
    aiTool( "getWeather", "Get weather", ( city ) => weatherAPI.get( city ) ),
    aiTool( "getTime", "Get current time", () => now() ),
    aiTool( "calculate", "Do math", ( expr ) => evaluate( expr ) )
];

model = aiModel( "openai", tools: tools );

// Model can now call any of these tools
result = aiMessage( "What's the weather in Paris?" )
    .to( model )
    .run();
```

### Parallel Model Comparison

```javascript
// Compare providers
question = aiMessage( "What is AI?" );

openAI = question.to( aiModel( "openai" ) );
claude = question.to( aiModel( "claude" ) );
ollama = question.to( aiModel( "ollama" ) );

// Run in parallel (pseudo-code, actual async would use aiChatAsync)
results = {
    openai: openAI.run(),
    claude: claude.run(),
    ollama: ollama.run()
};
```

### Conditional Model Selection

```javascript
// Select model based on logic
function getModel( complexity ) {
    if ( complexity == "high" ) {
        return aiModel( "claude" ); // More capable
    } else {
        return aiModel( "openai" ); // Faster/cheaper
    }
}

// Use in pipeline
result = aiMessage( "Simple question" )
    .to( getModel( "low" ) )
    .run();
```

### Model Pipeline Factory

```javascript
// Factory for model pipelines
function createPipeline( provider, systemPrompt ) {
    return aiMessage()
        .system( systemPrompt )
        .user( "${input}" )
        .to( aiModel( provider ) )
        .to( aiTransform( r => r.content ) );
}

// Create specialized pipelines
coder = createPipeline( "openai", "You are a coding assistant" );
writer = createPipeline( "claude", "You are a creative writer" );

code = coder.run({ input: "Write a function" });
story = writer.run({ input: "Write a story" });
```

### Extract and Transform

```javascript
// Extract specific content
extractor = aiMessage()
    .user( "List pros and cons of: ${topic}" )
    .to( aiModel( "openai" ) )
    .to( aiTransform( "jsonExtractor" ) )
    .to( aiTransform( data => data.pros.len() ));

prosCount = extractor.run({ topic: "AI development" });
```

### Error Handling

```javascript
// Handle errors in pipeline
safePipeline = aiMessage()
    .user( "${prompt}" )
    .to( aiModel( "openai" ) )
    .to( aiTransform( r => {
        try {
            return r.content;
        } catch ( any e ) {
            return "Error: #e.message#";
        }
    }));

result = safePipeline.run({ prompt: "Hello" });
```

### Reusable Model Components

```javascript
// Define models once
openAIModel = aiModel( "openai" );
claudeModel = aiModel( "claude" );

// Use in multiple pipelines
summarizer = aiMessage( "Summarize: ${text}" )
    .to( openAIModel )
    .to( aiTransform( r => r.content ) );

translator = aiMessage( "Translate to ${lang}: ${text}" )
    .to( claudeModel )
    .to( aiTransform( r => r.content ) );
```

### Dynamic Tool Binding

```javascript
// Add tools dynamically
model = aiModel( "openai" );

// Start without tools
result1 = aiMessage( "Hello" ).to( model ).run();

// Bind tools later
model.bindTools([
    aiTool( "search", "Search", ( q ) => search( q ) )
]);

// Now has tools
result2 = aiMessage( "Search for AI" ).to( model ).run();
```

## Notes

* 🔄 **IAiRunnable**: Implements full runnable interface for pipelines
* 🎯 **Service Wrapper**: Wraps `aiService()` for pipeline compatibility
* 🔧 **Tool Support**: Bind tools for function calling capabilities
* 📦 **Reusable**: Create once, use in multiple pipelines
* 🚀 **Events**: Fires `onAIModelCreate` event for interceptors
* 💡 **Difference**: Use `aiModel()` for pipelines, `aiService()` for direct invocation
* ⚡ **Performance**: Same underlying service, just different interface

## Related Functions

* [`aiService()`](aiservice.md) - Get service provider instances (direct invocation)
* [`aiMessage()`](aimessage.md) - Build messages for model input
* [`aiTransform()`](aitransform.md) - Transform model outputs
* [`aiTool()`](aitool.md) - Create tools for model function calling
* [`aiAgent()`](aiagent.md) - Create agents (higher-level abstraction)

## Best Practices

✅ **Use in pipelines** - Designed for `to()` chaining with messages and transforms

✅ **Reuse model instances** - Create once, use in multiple pipelines

✅ **Bind tools early** - Attach tools when creating model if known

✅ **Combine with transforms** - Chain with `aiTransform()` for data processing

✅ **Template with variables** - Use `aiMessage()` templates for flexibility

❌ **Don't use for direct invocation** - Use `aiService()` instead for simple calls

❌ **Don't create models per request** - Reuse model instances for efficiency

❌ **Don't mix with non-runnables** - Ensure entire pipeline uses IAiRunnable interface
