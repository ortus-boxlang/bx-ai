# aiChatStream

Stream AI responses in real-time with a callback function, ideal for UI updates and long responses.

## Syntax

```javascript
aiChatStream(messages, callback, params, options)
```

## Parameters

| Parameter  | Type     | Required | Description                                          |
| ---------- | -------- | -------- | ---------------------------------------------------- |
| `messages` | any      | Yes      | The messages to pass to the AI model                 |
| `callback` | function | Yes      | Function called with each chunk: `function(chunk)`   |
| `params`   | struct   | No       | Request parameters for the AI provider               |
| `options`  | struct   | No       | Request options (provider, apiKey, timeout, logging) |

### Options Structure

| Option        | Type    | Default      | Description                |
| ------------- | ------- | ------------ | -------------------------- |
| `provider`    | string  | (config)     | The AI provider to use     |
| `apiKey`      | string  | (config/env) | API key for the provider   |
| `timeout`     | numeric | `30`         | Request timeout in seconds |
| `logResponse` | boolean | `false`      | Log the response           |
| `logRequest`  | boolean | `false`      | Log the request            |

Note: `returnFormat` is not used in streaming - chunks are passed directly to callback.

## Returns

Returns `void`. All output is delivered through the callback function.

## Examples

### Basic Streaming

```javascript
// Stream to console
aiChatStream(
    "Tell me a story",
    ( chunk ) => {
        write( chunk );
        flush;
    }
);
```

### Build Complete Response

```javascript
// Accumulate chunks
fullResponse = "";

aiChatStream(
    "Explain BoxLang in detail",
    ( chunk ) => {
        fullResponse &= chunk;
        write( chunk ); // Show progress
        flush;
    }
);

// fullResponse contains complete text
println( "\n\nComplete response length: #fullResponse.len()#" );
```

### UI Integration

```javascript
// Update UI in real-time
function streamToUI( prompt ) {
    var responseDiv = getElementById( "ai-response" );
    var buffer = "";

    aiChatStream(
        prompt,
        ( chunk ) => {
            buffer &= chunk;
            // Update UI with accumulated text
            responseDiv.innerHTML = markdownToHTML( buffer );
        }
    );
}

streamToUI( "Explain dependency injection" );
```

### With Progress Tracking

```javascript
charCount = 0;
wordCount = 0;

aiChatStream(
    "Write a long essay",
    ( chunk ) => {
        charCount += chunk.len();
        wordCount += chunk.split( " " ).len();

        write( chunk );

        // Update progress every 100 chars
        if ( charCount % 100 == 0 ) {
            writeLog( "Received #charCount# chars, #wordCount# words" );
        }
    }
);
```

### Error Handling in Callback

```javascript
try {
    aiChatStream(
        "Hello",
        ( chunk ) => {
            try {
                // Process chunk
                processChunk( chunk );
            } catch( any e ) {
                writeLog( "Error processing chunk: #e.message#" );
            }
        }
    );
} catch( any e ) {
    writeLog( "Streaming error: #e.message#" );
}
```

### Streaming with Parameters

```javascript
// Creative writing with high temperature
aiChatStream(
    "Write a creative story",
    ( chunk ) => { write( chunk ); flush; },
    {
        temperature: 0.9,
        max_tokens: 1000
    }
);
```

### Multiple Providers

```javascript
// Stream from Claude
aiChatStream(
    "Explain quantum physics",
    ( chunk ) => { write( chunk ); },
    { model: "claude-3-opus-20240229" },
    { provider: "claude" }
);

// Stream from local Ollama
aiChatStream(
    "Hello",
    ( chunk ) => { write( chunk ); },
    { model: "llama2" },
    { provider: "ollama" }
);
```

### Streaming to File

```javascript
fileHandle = fileOpen( "/output/response.txt", "write" );

try {
    aiChatStream(
        "Generate documentation",
        ( chunk ) => {
            fileWrite( fileHandle, chunk );
        }
    );
} finally {
    fileClose( fileHandle );
}
```

### Server-Sent Events (SSE) Integration

```javascript
// Stream to browser via SSE
function streamToSSE( prompt ) {
    response.setContentType( "text/event-stream" );
    response.setHeader( "Cache-Control", "no-cache" );

    aiChatStream(
        prompt,
        ( chunk ) => {
            // Send SSE format
            writeOutput( "data: #chunk#\n\n" );
            flush;
        }
    );

    writeOutput( "data: [DONE]\n\n" );
}
```

### Conversation Streaming

```javascript
messages = [
    { role: "system", content: "You are a helpful assistant" },
    { role: "user", content: "Tell me about BoxLang" },
    { role: "assistant", content: "BoxLang is..." },
    { role: "user", content: "Tell me more" }
];

aiChatStream(
    messages,
    ( chunk ) => { write( chunk ); }
);
```

## Callback Function Signature

```javascript
function callback( required string chunk ) {
    // chunk: String fragment of the response
    // Called multiple times as response streams in
    // Return value is ignored
}
```

## Use Cases

### ✅ Long Responses

Provide immediate feedback for multi-paragraph responses.

### ✅ Real-Time UI Updates

Update chat interfaces as text arrives.

### ✅ Progress Indication

Show users that AI is "thinking" and generating.

### ✅ Server-Sent Events

Stream responses to browser clients.

### ✅ Token-by-Token Processing

Process response incrementally for real-time analysis.

### ❌ Short Responses

Use `aiChat()` for simple, quick requests.

### ❌ When You Need Complete Response First

Use `aiChat()` if you need full response before processing.

## Notes

* **Real-time delivery**: Chunks arrive as AI generates them
* **No buffering**: Response not accumulated automatically - callback handles each chunk
* **Provider support**: Not all providers support streaming (fallback to complete response)
* **No return format**: Streaming always delivers raw text chunks
* **Flush recommended**: Use `flush` in callback to push content to client immediately
* **Error handling**: Errors in callback don't stop stream - handle within callback
* **Thread blocking**: Function blocks until stream complete

## Related Functions

* [`aiChat()`](aichat.md) - Synchronous, complete response
* [`aiChatAsync()`](aichatasync.md) - Asynchronous with Future
* [`aiAgent()`](aiagent.md) - Agents support `.stream()` method

## Performance Tips

```javascript
// ✅ Flush after each chunk for UI
aiChatStream( prompt, ( chunk ) => {
    write( chunk );
    flush; // Push to client immediately
} );

// ✅ Batch small operations
buffer = "";
aiChatStream( prompt, ( chunk ) => {
    buffer &= chunk;
    if ( buffer.len() > 100 ) {
        processBuffer( buffer );
        buffer = "";
    }
} );

// ❌ Don't do heavy processing per chunk
aiChatStream( prompt, ( chunk ) => {
    // This runs for EVERY chunk - could be hundreds of times
    heavyDatabaseOperation(); // Bad!
} );

// ✅ Instead, accumulate and process once
chunks = [];
aiChatStream( prompt, ( c ) => chunks.append( c ) );
fullText = chunks.toList( "" );
processOnce( fullText ); // Better!
```
