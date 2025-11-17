# Working with Models

Learn how to use AI models as pipeline-compatible runnables. Models wrap AI service providers for seamless integration into pipelines.

## Creating Models

The `aiModel()` BIF creates pipeline-compatible AI models.

### Basic Creation

```java
// Uses default provider from config
model = aiModel()

// Specific provider
model = aiModel( "openai" )
model = aiModel( "claude" )
model = aiModel( "gemini" )
model = aiModel( "ollama" )

// With custom API key
model = aiModel( "openai", "sk-your-key-here" )
```

### Model Configuration

```java
model = aiModel( "openai" )
    .withParams( {
        model: "gpt-4",
        temperature: 0.7,
        max_tokens: 1000,
        top_p: 0.9
    } )
    .withName( "my-gpt4-model" )
```

## Models in Pipelines

### Basic Pipeline

```java
pipeline = aiMessage()
    .user( "Explain ${topic}" )
    .to( aiModel( "openai" ) )

result = pipeline.run( { topic: "AI" } )
```

### Using Default Model

```java
// Shortcut for .to( aiModel() )
pipeline = aiMessage()
    .user( "Hello ${name}" )
    .toDefaultModel()

result = pipeline.run( { name: "World" } )
```

### Multiple Models in Sequence

```java
// Generate with OpenAI, review with Claude
pipeline = aiMessage()
    .user( "Write code to ${task}" )
    .to( aiModel( "openai" ).withName( "generator" ) )
    .transform( r => r.content )
    .to( aiMessage().user( "Review: ${code}" ) )
    .to( aiModel( "claude" ).withName( "reviewer" ) )

result = pipeline.run( { task: "sort an array" } )
```

## Model Parameters

### Common Parameters

```java
model = aiModel( "openai" )
    .withParams( {
        model: "gpt-4",              // Model name
        temperature: 0.7,            // 0.0 = focused, 1.0 = creative
        max_tokens: 500,             // Response length limit
        top_p: 0.9,                  // Nucleus sampling
        presence_penalty: 0.0,       // Reduce topic repetition
        frequency_penalty: 0.0       // Reduce word repetition
    } )
```

### Provider-Specific Parameters

**OpenAI:**
```java
model = aiModel( "openai" )
    .withParams( {
        model: "gpt-4",
        response_format: { type: "json_object" },
        seed: 12345,
        user: "user-id-123"
    } )
```

**Claude:**
```java
model = aiModel( "claude" )
    .withParams( {
        model: "claude-3-opus-20240229",
        max_tokens: 4096,  // Required for Claude
        stop_sequences: [ "\n\nHuman:" ]
    } )
```

**Ollama:**
```java
model = aiModel( "ollama" )
    .withParams( {
        model: "llama3.2",
        temperature: 0.7,
        num_predict: 500
    } )
```

### Runtime Parameter Override

```java
model = aiModel( "openai" )
    .withParams( { temperature: 0.7 } )

// Override at runtime
result = model.run(
    { messages: [...] },
    { temperature: 0.9 }  // Uses 0.9
)
```

## Model Patterns

### Task-Specific Models

```java
// Creative writing model
creativeModel = aiModel( "openai" )
    .withParams( {
        model: "gpt-4",
        temperature: 0.9,
        max_tokens: 2000
    } )
    .withName( "creative-writer" )

// Code generation model
codeModel = aiModel( "openai" )
    .withParams( {
        model: "gpt-4",
        temperature: 0.3,
        max_tokens: 1000
    } )
    .withName( "code-generator" )

// Analysis model
analysisModel = aiModel( "claude" )
    .withParams( {
        model: "claude-3-opus-20240229",
        temperature: 0.2,
        max_tokens: 4096
    } )
    .withName( "analyzer" )
```

### Model Factory

```java
component {
    function getModel( required string purpose ) {
        switch( arguments.purpose ) {
            case "creative":
                return aiModel( "openai" )
                    .withParams({ temperature: 0.9, model: "gpt-4" })
            
            case "factual":
                return aiModel( "openai" )
                    .withParams({ temperature: 0.2, model: "gpt-4" })
            
            case "code":
                return aiModel( "openai" )
                    .withParams({ temperature: 0.3, model: "gpt-4" })
            
            case "analysis":
                return aiModel( "claude" )
                    .withParams({ temperature: 0.2, max_tokens: 4096 })
            
            case "local":
                return aiModel( "ollama" )
                    .withParams({ model: "llama3.2" })
            
            default:
                return aiModel()
        }
    }
}

// Usage
factory = new ModelFactory()
result = aiMessage()
    .user( "Write a poem" )
    .to( factory.getModel( "creative" ) )
    .run()
```

### Model Ensemble

```java
// Get multiple perspectives
function askEnsemble( required string question ) {
    models = [
        aiModel( "openai" ).withName( "openai" ),
        aiModel( "claude" ).withName( "claude" ),
        aiModel( "ollama" ).withName( "ollama" )
    ]
    
    message = aiMessage().user( arguments.question )
    
    return models.map( m => {
        return {
            model: m.getName(),
            response: message.to( m ).run()
        }
    } )
}

// Usage
responses = askEnsemble( "What is the future of AI?" )
responses.each( r => {
    println( r.model & ": " & r.response )
} )
```

## Advanced Usage

### Conditional Model Selection

```java
function getAppropriateModel( required string taskType, required numeric complexity ) {
    if( arguments.taskType == "creative" ) {
        return aiModel( "openai" ).withParams({ temperature: 0.9 })
    }
    
    if( arguments.complexity > 8 ) {
        return aiModel( "openai" ).withParams({ model: "gpt-4" })
    }
    
    if( arguments.complexity < 3 ) {
        return aiModel( "ollama" ).withParams({ model: "llama3.2:1b" })
    }
    
    return aiModel( "openai" ).withParams({ model: "gpt-3.5-turbo" })
}

// Usage
model = getAppropriateModel( "analysis", 9 )
result = aiMessage().user( "Complex task" ).to( model ).run()
```

### Model with Fallback

```java
function robustPipeline( required string question ) {
    message = aiMessage().user( arguments.question )
    
    try {
        // Try primary model
        return message.to( aiModel( "openai" ) ).run()
    } catch( any e ) {
        try {
            // Fallback to Claude
            return message.to( aiModel( "claude" ) ).run()
        } catch( any e2 ) {
            // Last resort: local model
            return message.to( aiModel( "ollama" ) ).run()
        }
    }
}
```

### Cost-Aware Model Selection

```java
component {
    property name="budget" type="numeric" default="0";
    property name="spent" type="numeric" default="0";
    
    function init( required numeric budget ) {
        variables.budget = arguments.budget
        return this
    }
    
    function getModel() {
        remaining = variables.budget - variables.spent
        
        if( remaining > 0.10 ) {
            return aiModel( "openai" ).withParams({ model: "gpt-4" })
        } else if( remaining > 0.01 ) {
            return aiModel( "openai" ).withParams({ model: "gpt-3.5-turbo" })
        } else {
            return aiModel( "ollama" )  // Free
        }
    }
    
    function trackUsage( required numeric cost ) {
        variables.spent += arguments.cost
    }
}
```

## Model Introspection

### Getting Model Information

```java
model = aiModel( "openai" )
    .withParams({ model: "gpt-4", temperature: 0.7 })
    .withName( "my-model" )

// Get name
println( model.getName() )  // "my-model"

// Get service
service = model.getService()
println( service.getName() )  // "openai"

// Get effective parameters
params = model.getMergedParams()
println( params )  // { model: "gpt-4", temperature: 0.7 }
```

### Pipeline Inspection

```java
pipeline = aiMessage()
    .user( "Hello" )
    .withName( "greeting" )
    .to( aiModel( "openai" ).withName( "gpt-model" ) )
    .transform( r => r.content )

// Get pipeline structure
steps = pipeline.getSteps()
println( "Pipeline has " & steps.len() & " steps:" )
steps.each( (s, i) => {
    println( "#i#. #s.getName()#" )
})
```

## Best Practices

1. **Name Your Models**: Use `.withName()` for debugging
2. **Set Appropriate Temperature**: Match creativity to task
3. **Limit Max Tokens**: Control costs and response time
4. **Use Local Models**: For privacy and development
5. **Cache Model Instances**: Reuse configured models
6. **Handle Errors**: Models can timeout or fail
7. **Monitor Costs**: Track usage with raw responses

## Examples

### Document Processor

```java
summarizer = aiMessage()
    .system( "Summarize concisely" )
    .user( "${document}" )
    .to( aiModel( "openai" ).withParams({ temperature: 0.3 }) )
    .transform( r => r.content )

extractor = aiMessage()
    .system( "Extract key points" )
    .user( "${document}" )
    .to( aiModel( "claude" ).withParams({ max_tokens: 4096 }) )
    .transform( r => r.content )

document = "Long document text..."
summary = summarizer.run( { document: document } )
keyPoints = extractor.run( { document: document } )
```

### Multi-Model Validator

```java
pipeline = aiMessage()
    .user( "Generate code to ${task}" )
    .to( aiModel( "openai" ).withName( "generator" ) )
    .transform( r => r.content )
    .to( aiMessage().user( "Validate: ${code}" ) )
    .to( aiModel( "claude" ).withName( "validator" ) )
    .transform( r => r.content )

result = pipeline.run( { task: "sort array" } )
```

## Next Steps

- **[Message Templates](messages.md)** - Build dynamic prompts
- **[Transformers](transformers.md)** - Process model outputs
- **[Pipeline Streaming](streaming.md)** - Real-time responses
