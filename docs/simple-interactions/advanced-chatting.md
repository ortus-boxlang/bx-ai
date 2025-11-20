# Advanced Chatting

Master advanced AI interaction techniques including multi-turn conversations, AI tools, async operations, and streaming responses.

## Multi-Message Conversations

Create rich, contextual conversations with system prompts and conversation history.

### Conversation Arrays

```java
conversation = [
    { role: "system", content: "You are a helpful coding tutor" },
    { role: "user", content: "What is a variable?" },
    { role: "assistant", content: "A variable is a named container for storing data..." },
    { role: "user", content: "Show me an example in BoxLang" }
]

answer = aiChat( conversation )
```

### Message Roles

- **system**: Sets AI behavior and personality
- **user**: Your messages/questions
- **assistant**: AI's responses (for conversation history)

### Building Conversations Dynamically

```java
messages = [
    { role: "system", content: "You are a helpful assistant" }
]

// Add user question
messages.append( { role: "user", content: "What is BoxLang?" } )

// Get response
answer = aiChat( messages )

// Add AI response to history
messages.append( { role: "assistant", content: answer } )

// Continue conversation
messages.append( { role: "user", content: "Show me an example" } )
answer = aiChat( messages )
```

### Conversation Manager

```java
class {
    property name="messages" type="array";
    property name="systemPrompt" type="string";

    function init( required string systemPrompt ) {
        variables.systemPrompt = arguments.systemPrompt
        variables.messages = [
            { role: "system", content: arguments.systemPrompt }
        ]
        return this
    }

    function ask( required string question ) {
        // Add user message
        variables.messages.append({
            role: "user",
            content: arguments.question
        })

        // Get AI response
        answer = aiChat( variables.messages )

        // Add to history
        variables.messages.append({
            role: "assistant",
            content: answer
        })

        return answer
    }

    function reset() {
        variables.messages = [
            { role: "system", content: variables.systemPrompt }
        ]
    }
}

// Usage
chat = new ConversationManager( "You are a coding tutor" )
answer1 = chat.ask( "What is a function?" )
answer2 = chat.ask( "Show me an example" )  // Has context from answer1
```

## AI Tools

Enable AI to call functions and access real-time data.

### Creating Tools

```java
weatherTool = aiTool(
    "get_weather",
    "Get current weather for a location",
    ( args ) => {
        // Your weather API call here
        return {
            location: args.location,
            temp: 72,
            condition: "sunny"
        }
    }
).addParameter( "location", "string", "City name", true )
```

### Using Tools

```java
answer = aiChat(
    "What's the weather in San Francisco?",
    { tools: [ weatherTool ] }
)
// AI will call the tool and use the data in its response
```

### Multiple Tools

```java
// Weather tool
weatherTool = aiTool(
    "get_weather",
    "Get weather for a location",
    ( args ) => getWeatherData( args.location )
).addParameter( "location", "string", "City name", true )

// Calculator tool
calcTool = aiTool(
    "calculate",
    "Perform calculations",
    ( args ) => evaluate( args.expression )
).addParameter( "expression", "string", "Math expression", true )

// Search tool
searchTool = aiTool(
    "search",
    "Search for information",
    ( args ) => searchDatabase( args.query )
).addParameter( "query", "string", "Search query", true )

// Use all tools
answer = aiChat(
    "What's 25 * 34 plus the temperature in NYC?",
    { tools: [ weatherTool, calcTool ] }
)
```

### Tool Examples

**Database Query Tool:**

```java
dbTool = aiTool(
    "query_database",
    "Query the customer database",
    ( args ) => {
        query = queryExecute(
            "SELECT * FROM customers WHERE #args.field# = :value",
            { value: args.value }
        )
        return query
    }
)
    .addParameter( "field", "string", "Field to search", true )
    .addParameter( "value", "string", "Value to search for", true )

answer = aiChat(
    "Find all customers in California",
    { tools: [ dbTool ] }
)
```

**API Integration Tool:**

```java
apiTool = aiTool(
    "fetch_data",
    "Fetch data from external API",
    ( args ) => {
        result = cfhttp(
            url: "https://api.example.com/" & args.endpoint,
            method: "GET"
        )
        return deserializeJSON( result.fileContent )
    }
).addParameter( "endpoint", "string", "API endpoint", true )
```

## Message Builder

Use `aiMessage()` for structured message composition:

### Basic Usage

```java
message = aiMessage()
    .system( "You are a code reviewer" )
    .user( "Review this code: function test() { }" )

answer = aiChat( message.getMessages() )
```

### Chaining Messages

```java
message = aiMessage()
    .system( "You are a technical writer" )
    .user( "Explain variables" )
    .assistant( "Variables store data..." )
    .user( "Give me an example" )
    .assistant( "var name = 'John'" )
    .user( "Another example?" )

answer = aiChat( message.getMessages() )
```

### Reusable Templates

```java
// Create template
reviewTemplate = aiMessage()
    .system( "You are an expert code reviewer" )
    .user( "Review this ${language} code:\n${code}" )

// Use with different values
review1 = aiChat(
    reviewTemplate.format({
        language: "BoxLang",
        code: "function add(a,b) { return a+b }"
    })
)

review2 = aiChat(
    reviewTemplate.format({
        language: "Java",
        code: "public int add(int a, int b) { return a + b; }"
    })
)
```

## Async Chat Requests

Perform non-blocking AI operations.

### Basic Async

```java
// Start request
future = aiChatAsync( "Explain quantum computing" )

// Do other work
println( "Request sent, doing other work..." )
doOtherStuff()

// Get result when ready
answer = future.get()
println( answer )
```

### With Callbacks

```java
aiChatAsync( "What is BoxLang?" )
    .then( ( result ) => {
        println( "Success: " & result )
    } )
    .onError( ( error ) => {
        println( "Error: " & error.message )
    } )
```

### Multiple Concurrent Requests

```java
// Start multiple requests
future1 = aiChatAsync( "Explain AI" )
future2 = aiChatAsync( "Explain ML" )
future3 = aiChatAsync( "Explain DL" )

// Wait for all
answer1 = future1.get()
answer2 = future2.get()
answer3 = future3.get()

println( "AI: " & answer1 )
println( "ML: " & answer2 )
println( "DL: " & answer3 )
```

### Timeout Handling

```java
future = aiChatAsync( "Complex question" )

try {
    // Wait max 30 seconds
    answer = future.get( 30 )
} catch( "TimeoutException" e ) {
    println( "Request took too long" )
}
```

## Streaming Responses

Get real-time responses as they're generated.

### Basic Streaming

```java
aiChatStream(
    "Tell me a story about a robot",
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        print( content )
    }
)
println( "\nDone!" )
```

### With Parameters

```java
aiChatStream(
    "Write a detailed explanation of AI",
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        print( content )
    },
    {
        model: "gpt-4",
        temperature: 0.7,
        max_tokens: 1000
    }
)
```

### Collecting Stream Data

```java
fullResponse = ""
chunkCount = 0

aiChatStream(
    "Explain AI pipelines",
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        fullResponse &= content
        chunkCount++
        print( content )
    }
)

println( "\n\nReceived " & chunkCount & " chunks" )
println( "Total: " & len( fullResponse ) & " characters" )
```

### Web Streaming Example

```java
// In a web handler
function streamResponse( required string question ) {
    response.setContentType( "text/event-stream" )
    response.setHeader( "Cache-Control", "no-cache" )

    aiChatStream(
        arguments.question,
        ( chunk ) => {
            content = chunk.choices?.first()?.delta?.content ?: ""
            writeOutput( "data: " & content & "\n\n" )
            flush()
        }
    )

    writeOutput( "data: [DONE]\n\n" )
}
```

### Markdown Streaming Parser

```java
markdown = ""
inCodeBlock = false

aiChatStream(
    "Explain quicksort with code",
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        markdown &= content

        // Detect code blocks
        if( content contains "```" ) {
            inCodeBlock = !inCodeBlock
        }

        // Style output
        if( inCodeBlock ) {
            print( chr(27) & "[32m" & content & chr(27) & "[0m" )  // Green
        } else {
            print( content )
        }
    }
)
```

## Structured Data with JSON and XML

### JSON Return Format for Complex Data

Use `returnFormat: "json"` to automatically parse structured responses:

```java
// Generate complex user profile
profile = aiChat(
    "Create a user profile with name, email, age, skills array, and preferences object",
    {},
    { returnFormat: "json" }
)

// Direct access to parsed data
println( "Name: #profile.name#" )
println( "Email: #profile.email#" )
println( "Skills:" )
profile.skills.each( skill => println( "  - #skill#" ) )
println( "Theme: #profile.preferences.theme#" )
```

### Multi-Turn Conversation with JSON

```java
conversation = [
    { role: "system", content: "You are a data generator. Always respond with valid JSON." },
    { role: "user", content: "Create 3 products with id, name, and price" }
]

products = aiChat(
    conversation,
    { temperature: 0.3 },
    { returnFormat: "json" }
)

// Use the structured data
products.each( product => {
    println( "##product.id#: #product.name# - $#product.price#" )
} )

// Continue conversation with context
conversation.append({
    role: "assistant",
    content: serializeJSON( products )
})
conversation.append({
    role: "user",
    content: "Now add a 'category' field to each"
})

updatedProducts = aiChat(
    conversation,
    {},
    { returnFormat: "json" }
)
```

### JSON with Tools

```java
// Tool returns structured data
dataTool = aiTool(
    "get_user_data",
    "Fetch user data from database",
    ( args ) => {
        return {
            id: args.userId,
            name: "John Doe",
            email: "john@example.com",
            purchases: [ "item1", "item2" ]
        }
    }
).addParameter( "userId", "string", "User ID", true )

// AI response will be JSON formatted
userData = aiChat(
    "Get data for user 123 and format as JSON",
    { tools: [ dataTool ] },
    { returnFormat: "json" }
)

println( "User: #userData.name#" )
println( "Purchases: #userData.purchases.len()#" )
```

### XML Return Format for Documents

```java
// Generate configuration XML
config = aiChat(
    "Create server config XML with host, port, database settings, and SSL enabled",
    {},
    { returnFormat: "xml" }
)

// Access parsed XML
println( "Host: #config.xmlRoot.server.host.xmlText#" )
println( "Port: #config.xmlRoot.server.port.xmlText#" )
println( "SSL: #config.xmlRoot.server.ssl.xmlText#" )
```

### XML Report Generation

```java
// Generate report as XML
report = aiChat(
    "Create a monthly sales report XML for January 2025 with 3 regions and their sales figures",
    { temperature: 0.3 },
    { returnFormat: "xml" }
)

// Parse and display
totalSales = 0
report.xmlRoot.report.regions.xmlChildren.each( region => {
    sales = val( region.sales.xmlText )
    totalSales += sales
    println( "#region.name.xmlText#: $#numberFormat( sales, '9,999' )#" )
} )
println( "Total: $#numberFormat( totalSales, '9,999' )#" )
```

### Async JSON Requests

```java
// Multiple async JSON requests
productsFuture = aiChatAsync(
    "Generate 3 products as JSON array",
    {},
    { returnFormat: "json" }
)

categoriesFuture = aiChatAsync(
    "Generate 5 product categories as JSON array",
    {},
    { returnFormat: "json" }
)

// Wait and use
products = productsFuture.get()
categories = categoriesFuture.get()

println( "Products: #products.len()#" )
println( "Categories: #categories.len()#" )

// Combine data
catalog = {
    categories: categories,
    products: products,
    timestamp: now()
}
```

### Streaming with JSON Accumulation

```java
// Stream response and parse JSON at end
jsonBuffer = ""

aiChatStream(
    "Generate a large JSON array of 20 cities with name, country, and population",
    ( chunk ) => {
        content = chunk.choices?.first()?.delta?.content ?: ""
        jsonBuffer &= content
        print( "." )
    },
    { temperature: 0.3 }
)

println( " Done!" )

// Parse accumulated JSON
cities = deserializeJSON( jsonBuffer )
println( "Generated #cities.len()# cities" )

// Process
cities.sort( (a, b) => b.population - a.population )
println( "Largest: #cities.first().name# (#numberFormat( cities.first().population )#)" )
```

## Practical Examples

### Interactive Chat Application

```java
class {
    property name="conversation";

    function init() {
        variables.conversation = [
            { role: "system", content: "You are a helpful assistant" }
        ]
        return this
    }

    function chat( required string message ) {
        // Add user message
        variables.conversation.append({
            role: "user",
            content: arguments.message
        })

        // Get response
        response = aiChat( variables.conversation )

        // Add to history
        variables.conversation.append({
            role: "assistant",
            content: response
        })

        return response
    }

    function streamChat( required string message, required function onChunk ) {
        variables.conversation.append({
            role: "user",
            content: arguments.message
        })

        fullResponse = ""

        aiChatStream(
            variables.conversation,
            ( chunk ) => {
                content = chunk.choices?.first()?.delta?.content ?: ""
                fullResponse &= content
                arguments.onChunk( content )
            }
        )

        variables.conversation.append({
            role: "assistant",
            content: fullResponse
        })

        return fullResponse
    }
}
```

### Smart Document Analyzer

```java
function analyzeDocument( required string document ) {
    // Extract key points async
    keyPointsFuture = aiChatAsync(
        "List key points from:\n" & arguments.document,
        { max_tokens: 200 }
    )

    // Generate summary async
    summaryFuture = aiChatAsync(
        "Summarize in 3 sentences:\n" & arguments.document,
        { max_tokens: 150 }
    )

    // Generate questions async
    questionsFuture = aiChatAsync(
        "Generate 5 questions about:\n" & arguments.document,
        { max_tokens: 200 }
    )

    return {
        keyPoints: keyPointsFuture.get(),
        summary: summaryFuture.get(),
        questions: questionsFuture.get()
    }
}
```

### Real-Time Code Assistant

```java
function codeAssistant( required string task ) {
    print( "Generating code" )

    code = ""

    aiChatStream(
        "Write BoxLang code to: " & arguments.task,
        ( chunk ) => {
            content = chunk.choices?.first()?.delta?.content ?: ""
            code &= content
            print( "." )
        },
        { model: "gpt-4", temperature: 0.4 }
    )

    println( " Done!" )
    return code
}
```

## Best Practices

1. **Use System Prompts**: Set clear context and behavior
2. **Manage Context Window**: Trim old messages for long conversations
3. **Handle Errors Gracefully**: Always use try/catch
4. **Stream Long Responses**: Better UX for detailed answers
5. **Cache When Possible**: Save costs and time
6. **Use Tools Wisely**: Only when real-time data is needed
7. **Test Async Operations**: Handle timeouts and failures

## Next Steps

- **[Service-Level Chatting](service-chatting.md)** - Direct service control
- **[Pipeline Overview](../pipelines/overview.md)** - Learn about AI pipelines
- **[Message Templates](../pipelines/messages.md)** - Advanced templating
