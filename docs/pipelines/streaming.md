# Pipeline Streaming

Stream data through pipelines in real-time for responsive applications. Streaming provides immediate feedback as AI generates responses.

## Basic Streaming

### Stream Through Pipeline

```java
pipeline = aiMessage()
    .user( "Tell me a story" )
    .toDefaultModel()

pipeline.stream( ( chunk ) => {
    content = chunk.choices?.first()?.delta?.content ?: ""
    print( content )
} )
```

### With Bindings

```java
pipeline = aiMessage()
    .system( "You are ${style}" )
    .user( "Write about ${topic}" )
    .toDefaultModel()

// stream( onChunk, input, params, options )
pipeline.stream(
    ( chunk ) => print( chunk.choices?.first()?.delta?.content ?: "" ),
    { style: "poetic", topic: "nature" }  // input bindings
)
```

### With Options

```java
pipeline = aiMessage()
    .user( "Write a story" )
    .toDefaultModel()

// stream( onChunk, input, params, options )
pipeline.stream(
    ( chunk ) => print( chunk.choices?.first()?.delta?.content ?: "" ),
    {},                      // input bindings
    { temperature: 0.8 },    // AI parameters
    { timeout: 120 }         // runtime options
)
```

## Options in Streaming

Streamers accept the same `options` parameter as `run()` methods:

### Default Options

```java
pipeline = aiMessage()
    .user( "Tell me about ${topic}" )
    .toDefaultModel()
    .withOptions( {
        timeout: 120,
        logRequest: true
    } )

// Uses default options
pipeline.stream(
    ( chunk ) => print( chunk.choices?.first()?.delta?.content ?: "" ),
    { topic: "AI" }
)
```

### Runtime Options Override

```java
pipeline = aiMessage()
    .user( "Write code" )
    .toDefaultModel()
    .withOptions( { timeout: 30 } )

// Override timeout at runtime
pipeline.stream(
    ( chunk ) => print( chunk.choices?.first()?.delta?.content ?: "" ),
    {},                      // input
    { temperature: 0.7 },    // params
    { timeout: 180 }         // options override
)
```

**Note:** Return format options don't apply to streaming - chunks are always in provider's streaming format.

## Message Streaming

Messages can stream their content:

```java
message = aiMessage()
    .system( "System prompt" )
    .user( "User message" )
    .assistant( "Assistant response" )

// Stream each message
message.stream( ( msg ) => {
    println( msg.role & ": " & msg.content )
} )
/*
Output:
system: System prompt
user: User message
assistant: Assistant response
*/
```

## Collecting Stream Data

### Full Response Collection

```java
fullResponse = ""
chunkCount = 0

pipeline = aiMessage()
    .user( "Explain ${topic}" )
    .toDefaultModel()

pipeline.stream(
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        fullResponse &= content
        chunkCount++
        print( content )
    },
    { topic: "AI pipelines" }
)

println( "\n\nCollected " & chunkCount & " chunks" )
println( "Total: " & len( fullResponse ) & " characters" )
```

### Structured Collection

```java
component {
    property name="chunks" type="array";
    property name="fullText" type="string" default="";

    function init() {
        variables.chunks = []
        return this
    }

    function onChunk( chunk ) {
        content = chunk.choices?.first()?.delta?.content ?: ""

        variables.chunks.append( {
            content: content,
            timestamp: now(),
            index: variables.chunks.len() + 1
        } )

        variables.fullText &= content
        print( content )
    }

    function getStats() {
        return {
            totalChunks: variables.chunks.len(),
            totalChars: len( variables.fullText ),
            fullText: variables.fullText
        }
    }
}

// Usage
collector = new StreamCollector()

pipeline.stream(
    ( chunk ) => collector.onChunk( chunk ),
    { topic: "AI" }
)

stats = collector.getStats()
```

## Streaming Patterns

### Progress Indicator

```java
print( "Generating" )

pipeline.stream(
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        if( len( content ) ) {
            print( "." )
        }
    }
)

println( " Done!" )
```

### Real-Time Display

```java
println( "AI Response:" )
println( "─".repeat( 50 ) )

pipeline.stream(
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        print( content )
    }
)

println( "\n" & "─".repeat( 50 ) )
```

### Chunk Processing

```java
words = []

pipeline.stream(
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""

        // Process words as they arrive
        if( content contains " " ) {
            words.append( content.trim() )
        }

        print( content )
    }
)

println( "\nReceived " & words.len() & " words" )
```

## Web Streaming

### Server-Sent Events (SSE)

```java
function streamToClient( required string question ) {
    response.setContentType( "text/event-stream" )
    response.setHeader( "Cache-Control", "no-cache" )
    response.setHeader( "Connection", "keep-alive" )

    pipeline = aiMessage()
        .user( arguments.question )
        .toDefaultModel()

    pipeline.stream( ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""

        if( len( content ) ) {
            writeOutput( "data: " & encodeForHTML( content ) & "\n\n" )
            flush()
        }
    } )

    writeOutput( "data: [DONE]\n\n" )
}
```

### WebSocket Streaming

```java
function streamToWebSocket( required websocket, required string question ) {
    pipeline = aiMessage()
        .user( arguments.question )
        .toDefaultModel()

    pipeline.stream( ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""

        if( len( content ) ) {
            arguments.websocket.send( serializeJSON( {
                type: "chunk",
                content: content
            } ) )
        }
    } )

    arguments.websocket.send( serializeJSON( {
        type: "done"
    } ) )
}
```

### JSON Streaming

```java
function streamJSON( required string question ) {
    response.setContentType( "application/x-ndjson" )

    chunkIndex = 0

    pipeline = aiMessage()
        .user( arguments.question )
        .toDefaultModel()

    pipeline.stream( ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""

        if( len( content ) ) {
            output = serializeJSON( {
                index: chunkIndex++,
                content: content,
                timestamp: getTickCount()
            } )

            writeOutput( output & "\n" )
            flush()
        }
    } )
}
```

## Advanced Streaming

### Stream with Transforms

Note: Transforms run after streaming completes, not per-chunk.

```java
// This collects full response then transforms
pipeline = aiMessage()
    .user( "Explain AI" )
    .toDefaultModel()
    .transform( r => r.content )

// Streaming still works on the model step
fullText = ""

pipeline.stream(
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        fullText &= content
        print( content )
    }
)
```

### Conditional Streaming

```java
function conditionalStream( required string question, boolean useStream = true ) {
    pipeline = aiMessage()
        .user( arguments.question )
        .toDefaultModel()

    if( arguments.useStream ) {
        pipeline.stream( ( chunk ) => {
            print( chunk.choices?.first()?.delta?.content ?: "" )
        } )
    } else {
        result = pipeline.run()
        println( result.content )
    }
}
```

### Stream with Timeout

```java
startTime = getTickCount()
timeout = 30000  // 30 seconds

pipeline.stream( ( chunk ) => {
    if( getTickCount() - startTime > timeout ) {
        throw( type: "StreamTimeout", message: "Streaming took too long" )
    }

    content = chunk.choices?.first()?.delta?.content ?: ""
    print( content )
} )
```

## Practical Examples

### Interactive Chat

```java
component {
    property name="pipeline";

    function init() {
        variables.pipeline = aiMessage()
            .system( "You are helpful" )
            .user( "${message}" )
            .toDefaultModel()
        return this
    }

    function chat( required string message ) {
        println( "You: " & arguments.message )
        print( "AI: " )

        variables.pipeline.stream(
            ( chunk ) => {
                print( chunk.choices?.first()?.delta?.content ?: "" )
            },
            { message: arguments.message }
        )

        println( "\n" )
    }
}

// Usage
chat = new InteractiveChat()
chat.chat( "Hello!" )
chat.chat( "Tell me about BoxLang" )
```

### Markdown Renderer

```java
inCodeBlock = false
codeBuffer = ""

pipeline.stream( ( chunk ) => {
    content = chunk.choices?.first()?.delta?.content ?: ""

    // Track code blocks
    if( content contains "```" ) {
        inCodeBlock = !inCodeBlock
        if( !inCodeBlock ) {
            println( codeBuffer )
            codeBuffer = ""
        }
    }

    // Style code differently
    if( inCodeBlock ) {
        codeBuffer &= content
    } else {
        print( content )
    }
} )
```

### Progress Tracker

```java
progress = {
    chars: 0,
    words: 0,
    sentences: 0
}

pipeline.stream( ( chunk ) => {
    content = chunk.choices?.first()?.delta?.content ?: ""

    progress.chars += len( content )
    progress.words += content.listLen( " " )
    progress.sentences += content.reFindNoCase( "[.!?]" )

    // Update UI every 10 chunks
    if( progress.chars % 10 == 0 ) {
        println( "Progress: " & progress.chars & " chars" )
    }

    print( content )
} )

println( "\nFinal: " & serializeJSON( progress ) )
```

### Stream Multiplexer

```java
// Send stream to multiple outputs
outputs = [
    ( content ) => print( content ),
    ( content ) => writeLog( content ),
    ( content ) => cachePut( "last-response", content )
]

fullText = ""

pipeline.stream( ( chunk ) => {
    content = chunk.choices?.first()?.delta?.content ?: ""
    fullText &= content

    // Send to all outputs
    outputs.each( output => output( content ) )
} )
```

## Error Handling

### Stream Error Handling

```java
try {
    pipeline.stream(
        ( chunk ) => {
            content = chunk.choices?.first()?.delta?.content ?: ""
            print( content )
        }
    )
} catch( any e ) {
    println( "\nStream error: " & e.message )

    // Log error
    writeLog( "Stream failed: " & e.message )

    // Fallback to non-streaming
    result = pipeline.run()
    println( result.content )
}
```

### Graceful Degradation

```java
function robustStream( required pipeline, required bindings ) {
    try {
        // Try streaming
        arguments.pipeline.stream(
            ( chunk ) => print( chunk.choices?.first()?.delta?.content ?: "" ),
            arguments.bindings
        )
    } catch( any e ) {
        // Fall back to regular execution
        println( "Streaming failed, using regular execution..." )
        result = arguments.pipeline.run( arguments.bindings )
        println( result.content )
    }
}
```

## Best Practices

1. **Flush Output**: Call `flush()` in web contexts
2. **Handle Errors**: Streaming can fail mid-response
3. **Track State**: Monitor stream progress
4. **Set Timeouts**: Prevent infinite streams
5. **Buffer Appropriately**: Balance responsiveness and performance
6. **Test Disconnects**: Handle client disconnections
7. **Provide Feedback**: Show progress indicators

## Performance Tips

1. **Minimize Processing**: Keep chunk callbacks fast
2. **Buffer When Needed**: Don't flush every character
3. **Use Appropriate Models**: Some stream better than others
4. **Monitor Memory**: Long streams can accumulate
5. **Close Connections**: Clean up resources

## Next Steps

- **[Pipeline Overview](overview.md)** - Complete pipeline guide
- **[Working with Models](models.md)** - Model configuration
- **[Message Templates](messages.md)** - Dynamic prompts
- **[Transformers](transformers.md)** - Data transformation
