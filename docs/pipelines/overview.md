# Understanding AI Pipelines

Learn how to build powerful, composable AI workflows using the pipeline pattern. Pipelines allow you to chain operations together, creating reusable and maintainable AI processing flows.

## What are Pipelines?

Pipelines are sequences of **runnables** - components that process data and pass results to the next step. Think of them as assembly lines for AI processing.

### Basic Pipeline Structure

```
Input → Message Template → AI Model → Transform → Output
```

Each step:

1. Receives input from the previous step
2. Processes the data
3. Passes output to the next step

### Why Use Pipelines?

**Composability**: Chain multiple operations together

```java
pipeline = aiMessage().user( "Hello" ).toDefaultModel().transform( r => r.content )
```

**Reusability**: Define once, use with different inputs

```java
greeter = aiMessage().user( "Greet ${name}" ).toDefaultModel()
greeter.run( { name: "Alice" } )  // "Hello Alice!"
greeter.run( { name: "Bob" } )    // "Hello Bob!"
```

**Immutability**: Each operation creates a new pipeline

```java
base = aiMessage().user( "Hello" )
pipeline1 = base.toDefaultModel()  // Doesn't modify base
pipeline2 = base.to( aiModel( "claude" ) )  // Different pipeline
```

**Flexibility**: Mix models, transforms, and custom logic

```java
complex = aiMessage()
    .user( "Task: ${task}" )
    .to( aiModel( "openai" ) )
    .transform( r => r.content )
    .to( aiMessage().user( "Review: ${review}" ) )
    .to( aiModel( "claude" ) )
```

## Core Concepts

### Runnables

All pipeline components implement the `IAiRunnable` interface:

```java
interface IAiRunnable {
    // Synchronous execution
    any function run( any input = {}, struct params = {} )

    // Streaming execution
    void function stream( function onChunk, any input = {}, struct params = {} )

    // Chaining
    IAiRunnable function to( IAiRunnable next )

    // Introspection
    string function getName()
}
```

**Built-in Runnables:**

- `AiMessage` - Message templates
- `AiModel` - AI providers wrapped for pipelines
- `AiTransformRunnable` - Data transformers
- `AiRunnableSequence` - Pipeline chains

### Input and Output

**Input types:**

- Empty struct `{}` - No input
- Struct with bindings `{ key: "value" }`
- Messages array `[{ role: "user", content: "..." }]`
- Previous step output

**Output types:**

- Messages array
- AI response struct
- Transformed data (string, struct, array, etc.)

### Parameters

Runtime parameters merge with stored defaults:

```java
model = aiModel()
    .withParams( { temperature: 0.7 } )  // Stored default

// Runtime params override
model.run( {}, { temperature: 0.9 } )  // Uses 0.9
```

## Building Your First Pipeline

### Step 1: Create a Message Template

```java
message = aiMessage()
    .system( "You are a helpful assistant" )
    .user( "Explain ${topic}" )
```

### Step 2: Add an AI Model

```java
pipeline = message.toDefaultModel()
```

### Step 3: Add a Transformer

```java
pipeline = message
    .toDefaultModel()
    .transform( response => response.content )
```

### Step 4: Run It

```java
result = pipeline.run( { topic: "recursion" } )
println( result )
```

### Complete Example

```java
// Create pipeline
explainer = aiMessage()
    .system( "You are a ${style} teacher" )
    .user( "Explain ${topic} in simple terms" )
    .toDefaultModel()
    .transform( r => r.content )
    .transform( s => s.trim() )

// Use multiple times
println( explainer.run( { style: "patient", topic: "variables" } ) )
println( explainer.run( { style: "concise", topic: "functions" } ) )
```

## Chaining Operations

### The `.to()` Method

Connects runnables in sequence:

```java
step1 = aiMessage().user( "Hello" )
step2 = aiModel( "openai" )
step3 = aiTransform( r => r.content )

pipeline = step1.to( step2 ).to( step3 )
```

### Helper Methods

**`.toDefaultModel()`** - Connect to default model:

```java
pipeline = aiMessage()
    .user( "Hello" )
    .toDefaultModel()  // Equivalent to .to( aiModel() )
```

**`.transform()`** - Add a transformer:

```java
pipeline = aiMessage()
    .user( "Hello" )
    .toDefaultModel()
    .transform( r => r.content.ucase() )
```

## Pipeline Patterns

### Linear Pipeline

```java
// Simple sequence: A → B → C
pipeline = aiMessage()
    .user( "Write code to ${task}" )
    .toDefaultModel()
    .transform( r => r.content )
```

### Multi-Step Processing

```java
// Generate, then review
pipeline = aiMessage()
    .user( "Write a function to ${task}" )
    .to( aiModel( "openai" ).withName( "generator" ) )
    .transform( r => r.content )
    .to( aiMessage().user( "Review this code: ${code}" ) )
    .to( aiModel( "claude" ).withName( "reviewer" ) )
    .transform( r => r.content )
```

### Branching Logic

```java
// Create different pipelines based on conditions
basePipeline = aiMessage().user( "Analyze ${data}" )

if( needsDeepAnalysis ) {
    pipeline = basePipeline.to( aiModel( "gpt-4" ).withParams({ temperature: 0.3 }) )
} else {
    pipeline = basePipeline.to( aiModel( "gpt-3.5-turbo" ) )
}
```

### Reusable Components

```java
// Create reusable steps
systemPrompt = aiMessage().system( "You are an expert ${role}" )
reviewer = aiModel( "claude" ).withParams({ temperature: 0.3 })
formatter = aiTransform( r => r.content.trim().ucase() )

// Combine in different ways
pipeline1 = systemPrompt.user( "Task 1" ).to( reviewer ).to( formatter )
pipeline2 = systemPrompt.user( "Task 2" ).to( reviewer )  // Without formatter
```

## Working with Pipeline Results

### Running Pipelines

```java
// With bindings
result = pipeline.run( { key: "value" } )

// Without bindings
result = pipeline.run()

// With runtime parameters
result = pipeline.run( { key: "value" }, { temperature: 0.9 } )
```

### Inspecting Pipelines

```java
pipeline = aiMessage()
    .user( "Hello" )
    .toDefaultModel()
    .transform( r => r.content )

// Count steps
count = pipeline.count()  // 3

// Get steps
steps = pipeline.getSteps()  // Array of runnables

// Get names
steps.each( s => println( s.getName() ) )
```

### Debugging Pipelines

```java
// Name your steps
pipeline = aiMessage()
    .user( "Hello" )
    .withName( "greeting-template" )
    .to( aiModel().withName( "gpt-model" ) )
    .transform( r => r.content )

// Print pipeline structure
pipeline.print()
/*
Pipeline with 3 steps:
1. greeting-template
2. gpt-model
3. AiTransformRunnable
*/
```

## Advanced Features

### Storing Bindings

```java
// Pre-bind some values
template = aiMessage()
    .system( "You are ${role}" )
    .user( "Explain ${topic}" )
    .bind( { role: "a teacher" } )  // Stored

// Only need to provide topic
result = template.toDefaultModel().run( { topic: "AI" } )
```

### Parameter Management

```java
// Set default parameters
model = aiModel( "openai" )
    .withParams( {
        temperature: 0.7,
        max_tokens: 500
    } )

// Runtime parameters merge
model.run( {}, { temperature: 0.9 } )  // Uses 0.9, keeps max_tokens: 500

// Clear defaults
model.clearParams()
```

### Naming for Organization

```java
// Name components
analyzer = aiMessage()
    .system( "Analyze the following" )
    .withName( "analyzer-prompt" )

model = aiModel( "gpt-4" )
    .withName( "analysis-model" )

pipeline = analyzer.user( "${text}" ).to( model )
    .withName( "document-analyzer" )

println( pipeline.getName() )  // "document-analyzer"
```

## Error Handling

```java
try {
    result = pipeline.run( bindings )
} catch( "InvalidInput" e ) {
    // Handle input validation errors
    println( "Invalid input: " & e.message )
} catch( "TimeoutException" e ) {
    // Handle timeout
    println( "Request timed out" )
} catch( any e ) {
    // Handle other errors
    println( "Pipeline error: " & e.message )
}
```

## Performance Tips

1. **Reuse Pipelines**: Create once, run many times
2. **Cache Results**: Cache expensive pipeline outputs
3. **Use Appropriate Models**: Match model capabilities to task complexity
4. **Limit Max Tokens**: Control costs and response times
5. **Stream Long Responses**: Better UX for detailed outputs

## Next Steps

- **[Working with Models](models.md)** - AI models in pipelines
- **[Message Templates](messages.md)** - Advanced templating
- **[Transformers](transformers.md)** - Data transformation
- **[Pipeline Streaming](streaming.md)** - Real-time processing
