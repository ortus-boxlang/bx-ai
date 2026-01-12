# aiTransform

Create an AI Transform Runnable that applies transformation functions to data in AI pipelines. Supports custom closures, built-in transformers, and chainable operations for data processing.

## Syntax

```javascript
aiTransform(transformer, config)
```

## Parameters

| Parameter     | Type   | Required | Default | Description                                                                                        |
| ------------- | ------ | -------- | ------- | -------------------------------------------------------------------------------------------------- |
| `transformer` | any    | Yes      | -       | The transformation function (closure), internal transformer name, or full classpath to transformer |
| `config`      | struct | No       | `{}`    | Optional configuration struct for the transformer                                                  |

### Transformer Types

**Custom Closure:**

```javascript
aiTransform( data => data.uCase() )
```

**Built-in Transformers:**

* `"code"` or `"codeExtractor"` - Extract code blocks from AI responses
* `"json"` or `"jsonExtractor"` - Extract and parse JSON from responses
* `"text"` or `"textExtractor"` - Extract plain text content
* `"xml"` or `"xmlExtractor"` - Extract and parse XML from responses

**Custom Class:**

```javascript
aiTransform( "com.myapp.CustomTransformer" )
```

## Returns

Returns an `AiTransformRunnable` object implementing IAiRunnable with:

* Pipeline integration: `run()`, `stream()`, `to()` methods
* Chainable: Can be chained with other runnables
* Flexible: Supports any transformation logic

## Examples

### Basic Transformation

```javascript
// Simple uppercase transform
transformer = aiTransform( input => input.uCase() );
result = transformer.run( "hello world" );
println( result ); // "HELLO WORLD"
```

### In Pipeline

```javascript
// Transform AI response
pipeline = aiMessage( "List 3 colors" )
    .toDefaultModel()
    .transform( r => r.content )
    .transform( text => text.uCase() );

result = pipeline.run();
println( result ); // "RED, GREEN, BLUE"
```

### Extract Content

```javascript
// Extract just the content from response
extractor = aiTransform( response => response.content );

result = aiMessage( "What is AI?" )
    .toDefaultModel()
    .to( extractor )
    .run();

println( result ); // Plain text content
```

### Multiple Transforms

```javascript
// Chain multiple transformations
pipeline = aiMessage( "Explain ${topic}" )
    .toDefaultModel()
    .transform( r => r.content )           // Extract content
    .transform( content => content.trim() ) // Trim whitespace
    .transform( content => content.uCase() ); // Uppercase

result = pipeline.run({ topic: "AI" });
```

### Code Extractor

```javascript
// Extract code blocks from response
codeExtractor = aiTransform( "codeExtractor" );

pipeline = aiMessage( "Write a hello world function" )
    .toDefaultModel()
    .to( codeExtractor );

code = pipeline.run();
println( code ); // Just the code, no markdown
```

### JSON Extractor

```javascript
// Extract and parse JSON from response
jsonExtractor = aiTransform( "jsonExtractor" );

pipeline = aiMessage( "Return this as JSON: name=John, age=30" )
    .toDefaultModel()
    .to( jsonExtractor );

data = pipeline.run();
println( data.name ); // "John"
println( data.age );  // 30
```

### Text Extractor

```javascript
// Extract plain text (removes formatting)
textExtractor = aiTransform( "textExtractor" );

pipeline = aiMessage( "Format this as markdown: Hello **World**" )
    .toDefaultModel()
    .to( textExtractor );

text = pipeline.run();
println( text ); // "Hello World" (no markdown)
```

### XML Extractor

```javascript
// Extract and parse XML
xmlExtractor = aiTransform( "xmlExtractor" );

pipeline = aiMessage( "Return this as XML: <user><name>John</name></user>" )
    .toDefaultModel()
    .to( xmlExtractor );

xml = pipeline.run();
println( xml.user.name ); // "John"
```

### Custom Logic

```javascript
// Complex transformation logic
analyzer = aiTransform( response => {
    content = response.content;

    return {
        text: content,
        length: len( content ),
        wordCount: listLen( content, " " ),
        sentiment: content.findNoCase( "good" ) ? "positive" : "neutral",
        timestamp: now()
    };
});

result = aiMessage( "Write something good" )
    .toDefaultModel()
    .to( analyzer )
    .run();
```

### Error Handling

```javascript
// Safe transformation with error handling
safeTransform = aiTransform( data => {
    try {
        return jsonDeserialize( data.content );
    } catch ( any e ) {
        return { error: true, message: e.message };
    }
});

result = aiMessage( "Return JSON" )
    .toDefaultModel()
    .to( safeTransform )
    .run();
```

### Data Mapping

```javascript
// Map response to specific structure
mapper = aiTransform( response => {
    return {
        answer: response.content,
        model: response.model,
        tokens: response.usage.total_tokens,
        timestamp: now()
    };
});

result = aiMessage( "What is 2+2?" )
    .toDefaultModel()
    .to( mapper )
    .run();
```

### Filter and Process

```javascript
// Filter and process data
processor = aiTransform( response => {
    content = response.content;

    // Extract lines
    lines = listToArray( content, char(10) );

    // Filter non-empty
    filtered = lines.filter( line => len( trim( line ) ) > 0 );

    // Return processed
    return filtered.map( line => trim( line ) );
});

result = aiMessage( "List 5 items" )
    .toDefaultModel()
    .to( processor )
    .run();
```

### Streaming Transform

```javascript
// Transform streamed chunks
uppercase = aiTransform( chunk => chunk.uCase() );

pipeline = aiMessage( "Tell a story" )
    .toDefaultModel()
    .to( uppercase );

pipeline.stream( chunk => writeOutput( chunk ) );
// Output: "ONCE UPON A TIME..."
```

### Conditional Transform

```javascript
// Different transformation based on condition
conditionalTransform = aiTransform( response => {
    if ( response.content.findNoCase( "json" ) ) {
        return jsonDeserialize( response.content );
    } else {
        return response.content;
    }
});
```

### Aggregate Transform

```javascript
// Aggregate multiple responses
aggregator = aiTransform( responses => {
    if ( !isArray( responses ) ) {
        return responses;
    }

    return {
        count: responses.len(),
        combined: responses.map( r => r.content ).toList( char(10) ),
        avgLength: responses.reduce(
            ( sum, r ) => sum + len( r.content ),
            0
        ) / responses.len()
    };
});
```

### Validation Transform

```javascript
// Validate and transform
validator = aiTransform( response => {
    content = response.content;

    // Validate
    if ( len( content ) < 10 ) {
        throw( "Response too short" );
    }

    // Transform
    return {
        valid: true,
        content: content,
        validated: now()
    };
});
```

### Caching Transform

```javascript
// Cache expensive transformations
cachingTransform = aiTransform( data => {
    cacheKey = "transform_" & hash( data.content );

    if ( cacheExists( cacheKey ) ) {
        return cacheGet( cacheKey );
    }

    // Expensive transformation
    result = expensiveProcess( data.content );

    cachePut( cacheKey, result, 60 ); // Cache 1 hour
    return result;
});
```

### Pipeline Factory

```javascript
// Create reusable transform pipelines
function createPipeline( transformers ) {
    var pipeline = aiMessage( "${input}" ).toDefaultModel();

    transformers.each( t => {
        pipeline = pipeline.to( aiTransform( t ) );
    });

    return pipeline;
}

// Use factory
pipeline = createPipeline([
    r => r.content,
    text => text.trim(),
    text => text.uCase()
]);

result = pipeline.run({ input: "hello" });
```

### Response Enrichment

```javascript
// Enrich response with metadata
enricher = aiTransform( response => {
    return {
        content: response.content,
        metadata: {
            model: response.model,
            provider: response.provider ?: "unknown",
            tokens: response.usage?.total_tokens ?: 0,
            processed: now(),
            length: len( response.content )
        }
    };
});
```

### Built-in with Config

```javascript
// Configure built-in transformer
codeExtractor = aiTransform( "codeExtractor", {
    language: "javascript",
    includeComments: false
});

jsonExtractor = aiTransform( "jsonExtractor", {
    strict: true,
    returnRaw: false
});
```

## Notes

* 🔗 **IAiRunnable**: Implements full runnable interface for pipelines
* 🎨 **Flexible**: Supports closures, built-ins, and custom classes
* 🔄 **Chainable**: Compose multiple transforms in sequence
* 🚀 **Performance**: Transformations are efficient and lightweight
* 📦 **Reusable**: Create once, use in multiple pipelines
* 🎯 **Type Safe**: Validate inputs and outputs as needed
* 💡 **Built-ins**: Code, JSON, text, and XML extractors included

## Related Functions

* [`aiModel()`](aimodel.md) - Create model runnables
* [`aiMessage()`](aimessage.md) - Build messages with transform()
* [`aiAgent()`](aiagent.md) - Agents use transforms internally
* [`aiPopulate()`](aipopulate.md) - Populate classes from transformed data

## Best Practices

✅ **Chain transforms** - Break complex transformations into simple steps

✅ **Use built-ins** - Leverage code/json/text extractors when appropriate

✅ **Handle errors** - Wrap risky transformations in try/catch

✅ **Keep transforms pure** - Avoid side effects in transformation functions

✅ **Reuse transforms** - Create transform once, use in multiple pipelines

✅ **Stream-friendly** - Design transforms to work with both run() and stream()

❌ **Don't make heavy transforms** - Keep transformations lightweight and fast

❌ **Don't mutate input** - Return new data, don't modify input

❌ **Don't ignore errors** - Handle transformation failures gracefully
