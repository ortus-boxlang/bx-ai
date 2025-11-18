# Transformers

Transform and process data between pipeline steps. Transformers are functions that modify, extract, or reshape data flowing through your pipeline.

## Creating Transformers

Transformers process data between pipeline steps. They implement the `IAiRunnable` interface but ignore the `options` parameter since they don't interact with AI providers.

### Inline Transform

```java
pipeline = aiMessage()
    .user( "Say hello" )
    .toDefaultModel()
    .transform( response => response.content )
```

### Using `aiTransform()`

```java
transformer = aiTransform( response => response.content.ucase() )

pipeline = aiMessage()
    .user( "Hello" )
    .toDefaultModel()
    .to( transformer )
```

### Named Transformer

```java
transformer = aiTransform( r => r.content )
    .withName( "content-extractor" )
```

## Common Transformations

### Extract Content

```java
pipeline = aiMessage()
    .user( "Explain AI" )
    .toDefaultModel()
    .transform( r => r.content )
// Input: { content: "AI is...", ... }
// Output: "AI is..."
```

### String Manipulation

```java
pipeline = aiMessage()
    .user( "List colors" )
    .toDefaultModel()
    .transform( r => r.content )
    .transform( s => s.ucase() )
    .transform( s => s.trim() )
```

### Parse JSON

```java
pipeline = aiMessage()
    .system( "Return only valid JSON" )
    .user( "Create person: name=${name}, age=${age}" )
    .toDefaultModel()
    .transform( r => r.content )
    .transform( s => deserializeJSON( s ) )

person = pipeline.run( { name: "Alice", age: 30 } )
// { name: "Alice", age: 30 }
```

### Extract Code

```java
codeExtractor = aiTransform( response => {
    content = response.content ?: ""
    // Extract from markdown code blocks
    code = content.reReplace( "(?s).*```[a-z]*\n(.*?)```.*", "\1", "one" )
    return code.trim()
} )

pipeline = aiMessage()
    .user( "Write a BoxLang function to ${task}" )
    .toDefaultModel()
    .to( codeExtractor )
```

## Chaining Transforms

### Sequential Processing

```java
pipeline = aiMessage()
    .user( "List 3 colors" )
    .toDefaultModel()
    .transform( r => r.content )          // Extract content
    .transform( s => s.listToArray() )    // Convert to array
    .transform( arr => arr.map( c => c.ucase() ) )  // Uppercase each

result = pipeline.run()
// ["RED", "BLUE", "GREEN"]
```

### Data Enrichment

```java
pipeline = aiMessage()
    .user( "Explain ${topic}" )
    .toDefaultModel()
    .transform( r => {
        return {
            content: r.content,
            topic: variables.topic,
            timestamp: now(),
            length: len( r.content )
        }
    } )

result = pipeline.run( { topic: "AI" } )
// { content: "...", topic: "AI", timestamp: {ts}, length: 150 }
```

## Options in Transformers

Transformers accept the `options` parameter for interface consistency but **ignore it** since they don't make AI requests:

```java
transformer = aiTransform( r => r.content.ucase() )

// Options parameter exists but has no effect on transformer behavior
result = transformer.run(
    { content: "hello" },     // input
    {},                       // params (ignored)
    { timeout: 60 }          // options (ignored by transformer)
)
// "HELLO"
```

**Why options exist:** Transformers implement `IAiRunnable` interface which requires the `options` parameter. This maintains a consistent API across all pipeline components, even though transformers don't use options.

**Options propagation:** When transformers are part of a pipeline sequence, options flow through to AI components:

```java
pipeline = aiMessage()
    .user( "Hello" )
    .toDefaultModel()
    .transform( r => r.content )  // Ignores options
    .withOptions( { returnFormat: "single" } )  // Applies to model, not transform

result = pipeline.run()  // Options affect the model step
```

## Advanced Transforms

### Conditional Logic

```java
ratingTransform = aiTransform( response => {
    content = response.content ?: "0"
    rating = val( content.reReplace( "[^0-9]", "", "all" ) )

    return {
        raw: content,
        rating: rating,
        quality: rating >= 7 ? "good" : "needs improvement",
        passed: rating >= 5
    }
} )

pipeline = aiMessage()
    .user( "Rate this from 1-10: ${item}" )
    .toDefaultModel()
    .to( ratingTransform )
```

### Error Handling

```java
safeTransform = aiTransform( response => {
    try {
        if( !structKeyExists( response, "content" ) ) {
            return "Error: No content returned"
        }

        data = deserializeJSON( response.content )
        return data
    } catch( any e ) {
        return {
            error: true,
            message: e.message,
            raw: response.content ?: ""
        }
    }
} )
```

### Data Validation

```java
validator = aiTransform( data => {
    errors = []

    if( !structKeyExists( data, "name" ) || len( data.name ) < 2 ) {
        errors.append( "Invalid name" )
    }

    if( !structKeyExists( data, "age" ) || data.age < 0 ) {
        errors.append( "Invalid age" )
    }

    return {
        valid: errors.len() == 0,
        errors: errors,
        data: data
    }
} )
```

## Practical Examples

### Markdown to HTML

```java
markdownTransform = aiTransform( response => {
    markdown = response.content ?: ""

    // Simple markdown parsing
    html = markdown
        .reReplace( "#{3} (.*)", "<h3>\1</h3>", "all" )
        .reReplace( "#{2} (.*)", "<h2>\1</h2>", "all" )
        .reReplace( "#{1} (.*)", "<h1>\1</h1>", "all" )
        .reReplace( "\*\*(.*?)\*\*", "<strong>\1</strong>", "all" )
        .reReplace( "\*(.*?)\*", "<em>\1</em>", "all" )

    return html
} )

pipeline = aiMessage()
    .user( "Write markdown about ${topic}" )
    .toDefaultModel()
    .to( markdownTransform )
```

### SQL Generator

```java
sqlTransform = aiTransform( response => {
    sql = response.content
        .reReplace( "(?s).*```sql\n(.*?)```.*", "\1", "one" )
        .trim()

    return {
        sql: sql,
        safe: !sql.reFindNoCase( "drop|delete|truncate" ),
        parameterized: sql.contains( "?" ) || sql.contains( ":" )
    }
} )

pipeline = aiMessage()
    .user( "Write SQL to ${task}" )
    .toDefaultModel()
    .to( sqlTransform )
```

### Response Cache

```java
component {
    property name="cache" type="struct";

    function init() {
        variables.cache = {}
        return this
    }

    function getCachedTransform() {
        return aiTransform( response => {
            key = hash( response.content )

            if( structKeyExists( variables.cache, key ) ) {
                return variables.cache[ key ]
            }

            processed = processResponse( response )
            variables.cache[ key ] = processed

            return processed
        } )
    }

    function processResponse( response ) {
        // Your processing logic
        return response.content.trim()
    }
}
```

### Multi-Format Output

```java
formatter = aiTransform( response => {
    content = response.content ?: ""

    return {
        text: content,
        html: "<p>" & content.replace( "\n", "</p><p>" ) & "</p>",
        markdown: content,
        json: serializeJSON( { content: content } ),
        length: len( content ),
        words: content.listLen( " " )
    }
} )
```

## Transform Patterns

### Filter Pattern

```java
filterTransform = aiTransform( items => {
    return items.filter( item => item.score > 5 )
} )
```

### Map Pattern

```java
mapTransform = aiTransform( items => {
    return items.map( item => {
        return {
            id: item.id,
            display: item.name.ucase()
        }
    } )
} )
```

### Reduce Pattern

```java
sumTransform = aiTransform( items => {
    return items.reduce( ( sum, item ) => sum + item.value, 0 )
} )
```

### Aggregate Pattern

```java
aggregator = aiTransform( data => {
    return {
        total: data.len(),
        average: data.sum() / data.len(),
        min: data.min(),
        max: data.max()
    }
} )
```

## Transform Library

```java
component {
    function extractContent() {
        return aiTransform( r => r.content )
    }

    function toUpperCase() {
        return aiTransform( s => s.ucase() )
    }

    function toLowerCase() {
        return aiTransform( s => s.lcase() )
    }

    function trim() {
        return aiTransform( s => s.trim() )
    }

    function parseJSON() {
        return aiTransform( s => deserializeJSON( s ) )
    }

    function extractCode( language = "" ) {
        return aiTransform( r => {
            pattern = "(?s).*```#language#\n(.*?)```.*"
            return r.content.reReplace( pattern, "\1", "one" ).trim()
        } )
    }

    function wordCount() {
        return aiTransform( s => s.listLen( " " ) )
    }

    function summarize( maxWords = 50 ) {
        return aiTransform( s => {
            words = s.listToArray( " " )
            if( words.len() <= maxWords ) return s
            return words.slice( 1, maxWords ).toList( " " ) & "..."
        } )
    }
}

// Usage
lib = new TransformLibrary()

pipeline = aiMessage()
    .user( "Explain AI" )
    .toDefaultModel()
    .to( lib.extractContent() )
    .to( lib.trim() )
    .to( lib.wordCount() )
```

## TransformAndRun Shortcut

Combine transform and run in one step:

```java
result = aiMessage()
    .user( "Say hello" )
    .toDefaultModel()
    .transformAndRun( r => r.content.ucase() )
// "HELLO!"
```

## Best Practices

1. **Keep Transforms Simple**: One responsibility per transform
2. **Handle Errors**: Use try/catch in transforms
3. **Document Logic**: Comment complex transformations
4. **Test Transforms**: Unit test transformation functions
5. **Chain Appropriately**: Logical sequence of operations
6. **Return Consistent Types**: Predictable output format
7. **Use Named Transforms**: For reusability

## Testing Transforms

```java
// Test transform independently
transformer = aiTransform( r => r.content.ucase() )

testInput = { content: "hello" }
result = transformer.run( testInput )

assert( result == "HELLO" )
```

## Next Steps

- **[Pipeline Streaming](streaming.md)** - Stream through transforms
- **[Working with Models](models.md)** - Model output transforms
- **[Pipeline Overview](overview.md)** - Complete pipeline guide
