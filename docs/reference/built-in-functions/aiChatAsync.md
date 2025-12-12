# aiChatAsync

Asynchronous version of `aiChat()` that returns a BoxLang Future for non-blocking AI requests.

## Syntax

```javascript
aiChatAsync(messages, params, options)
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `messages` | any | Yes | The messages to pass to the AI model (string, struct, array, or AiMessage) |
| `params` | struct | No | Request parameters for the AI provider |
| `options` | struct | No | Request options (provider, apiKey, returnFormat, timeout, logging) |

Parameters are identical to [`aiChat()`](./aiChat.md).

## Returns

Returns a BoxLang `Future` object that will eventually contain the AI response. Use `.get()` to retrieve the result (blocking) or `.then()` for callbacks.

## Examples

### Basic Async Request

```javascript
// Start async request
future = aiChatAsync( "What is BoxLang?" );

// Do other work...
doSomethingElse();

// Get result (blocks until ready)
response = future.get();
println( response );
```

### Multiple Concurrent Requests

```javascript
// Launch multiple requests in parallel
future1 = aiChatAsync( "Explain functions" );
future2 = aiChatAsync( "Explain classes" );
future3 = aiChatAsync( "Explain closures" );

// Continue working...

// Collect all results
results = [
    future1.get(),
    future2.get(),
    future3.get()
];

println( "Got #results.len()# responses" );
```

### With Callback

```javascript
// Process result asynchronously
future = aiChatAsync( "Tell me a joke" )
    .then( ( response ) => {
        println( "Joke: #response#" );
        return response.len();
    } )
    .then( ( length ) => {
        println( "Joke was #length# characters" );
    } );

// Returns immediately, callback runs when ready
```

### Error Handling

```javascript
future = aiChatAsync( "Hello" )
    .then( ( response ) => {
        println( "Success: #response#" );
    } )
    .onError( ( error ) => {
        println( "Error: #error.message#" );
    } );
```

### With Timeout

```javascript
try {
    // Wait max 5 seconds for result
    response = aiChatAsync( "Complex question..." )
        .get( timeout: 5000 );
} catch( "TimeoutException" e ) {
    println( "Request timed out" );
}
```

### Parallel Processing Pipeline

```javascript
function processQueries( queries ) {
    // Launch all requests
    futures = queries.map( ( q ) => aiChatAsync( q ) );
    
    // Wait for all to complete
    return futures.map( ( f ) => f.get() );
}

questions = [
    "What is AI?",
    "What is ML?",
    "What is NLP?"
];

answers = processQueries( questions );
```

### Background Processing

```javascript
// Fire and forget with callback
aiChatAsync( "Generate report for #date#" )
    .then( ( report ) => {
        fileWrite( "/reports/daily.txt", report );
        sendEmail( "Report ready", report );
    } );

// Code continues immediately
println( "Report generation started..." );
```

## Future Methods

### `.get(timeout)`
Blocks until result available, returns the AI response.
```javascript
response = future.get(); // Wait indefinitely
response = future.get( timeout: 5000 ); // Wait max 5 seconds
```

### `.then(callback)`
Executes callback when result ready, returns new Future.
```javascript
future.then( ( result ) => {
    // Process result
    return transformedResult;
} );
```

### `.onError(callback)`
Handles errors in the Future chain.
```javascript
future.onError( ( error ) => {
    writeLog( error.message );
} );
```

### `.isDone()`
Check if computation complete without blocking.
```javascript
if ( future.isDone() ) {
    response = future.get();
}
```

### `.cancel()`
Attempt to cancel the computation.
```javascript
future.cancel();
```

## Use Cases

### ✅ Multiple Independent Requests
```javascript
// Efficient parallel processing
futures = [
    aiChatAsync( "Query 1" ),
    aiChatAsync( "Query 2" ),
    aiChatAsync( "Query 3" )
];
results = futures.map( f => f.get() );
```

### ✅ Long-Running Operations
```javascript
// Don't block the thread
future = aiChatAsync( "Generate comprehensive report..." );
showLoadingIndicator();
response = future.get();
hideLoadingIndicator();
```

### ✅ Background Tasks
```javascript
// Process asynchronously
aiChatAsync( "Analyze logs" )
    .then( analysis => saveToDatabase( analysis ) );
```

### ❌ Simple Single Request
```javascript
// Just use aiChat() instead
response = aiChat( "Hello" ); // Simpler
```

## Notes

- **Extends aiChat()**: All `aiChat()` parameters and options supported
- **Non-blocking**: Returns immediately, computation runs in background
- **Thread pool**: Uses BoxLang's async executor
- **Error propagation**: Errors in async code bubble up to `.get()` or `.onError()`
- **Same return formats**: Supports all `returnFormat` options
- **Chaining**: Can chain multiple `.then()` calls for pipeline processing

## Related Functions

- [`aiChat()`](./aiChat.md) - Synchronous version
- [`aiChatStream()`](./aiChatStream.md) - Streaming version with callbacks
- [`aiAgent()`](./aiAgent.md) - For complex autonomous tasks

## Performance Tips

```javascript
// ✅ Batch parallel requests
futures = queries.map( q => aiChatAsync(q) );
results = futures.map( f => f.get() );

// ✅ Use callbacks for fire-and-forget
aiChatAsync( "task" ).then( result => process(result) );

// ❌ Don't call .get() immediately
response = aiChatAsync( "Hello" ).get(); // Defeats purpose

// ❌ Don't create unnecessary futures
response = aiChat( "Hello" ); // Use sync version
```
