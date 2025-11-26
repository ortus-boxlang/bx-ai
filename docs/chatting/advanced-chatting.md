---
description: "Master advanced AI interaction techniques including multi-turn conversations, AI tools, async operations, and streaming responses."
icon: user-ninja
---

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

## Multimodal Content

Work with images, audio, video, and documents in your AI conversations.

### Images

Vision-capable models can analyze images alongside text.

#### Using Image URLs

```java
answer = aiChat(
    aiMessage()
        .user( "What is in this image?" )
        .image( "https://example.com/photo.jpg" )
)
```

#### Embedding Local Images

```java
answer = aiChat(
    aiMessage()
        .user( "Analyze this screenshot for bugs" )
        .embedImage( "/screenshots/ui-issue.png", "high" )
)
```

#### Multiple Images

```java
comparison = aiChat(
    aiMessage()
        .user( "What changed between these versions?" )
        .embedImage( "/designs/v1.png" )
        .embedImage( "/designs/v2.png" )
)
```

#### Detail Levels

- `"auto"` (default): Model decides
- `"low"`: Faster, cheaper, 512x512 resolution
- `"high"`: Full detail for complex images

```java
// Quick description
aiChat(
    aiMessage()
        .user( "Brief description" )
        .image( imageUrl, "low" )
)

// Detailed analysis
aiChat(
    aiMessage()
        .user( "Count all objects" )
        .image( imageUrl, "high" )
)
```

### Audio

Process audio files with AI models that support audio understanding.

**Supported by:** OpenAI (GPT-4o-audio), Gemini

```java
// Transcribe audio
transcript = aiChat(
    aiMessage()
        .user( "Transcribe this meeting recording" )
        .embedAudio( "/meetings/standup-2024-11-26.mp3" )
)

// Analyze audio content
analysis = aiChat(
    aiMessage()
        .user( "What are the main topics discussed?" )
        .audio( "https://example.com/podcast-episode.mp3" )
)

// Extract action items
actionItems = aiChat(
    aiMessage()
        .system( "Extract action items and owners from meeting audio" )
        .user( "Process this meeting" )
        .embedAudio( "/meetings/planning-session.mp3" )
)
```

**Supported formats:** mp3, mp4, mpeg, mpga, m4a, wav, webm

### Video

Analyze video content with AI models that support video understanding.

**Supported by:** Gemini (gemini-1.5-pro, gemini-2.0-flash)

```java
// Analyze video content
summary = aiChat(
    aiMessage()
        .user( "Summarize what happens in this video" )
        .embedVideo( "/videos/demo.mp4" ),
    {},
    { provider: "gemini" }
)

// Security footage analysis
report = aiChat(
    aiMessage()
        .system( "You are a security analyst" )
        .user( "Describe any suspicious activities" )
        .video( "https://example.com/security-cam-01.mp4" ),
    {},
    { provider: "gemini" }
)

// Tutorial analysis
steps = aiChat(
    aiMessage()
        .user( "List the step-by-step instructions shown" )
        .embedVideo( "/tutorials/how-to-setup.mp4" ),
    { model: "gemini-2.0-flash-exp" },
    { provider: "gemini" }
)
```

**Supported formats:** mp4, mpeg, mov, avi, flv, mpg, webm, wmv

### Documents and PDFs

Analyze documents, PDFs, and text files.

**Supported by:** Claude (Opus, Sonnet), OpenAI (GPT-4o)

```java
// Analyze PDF document
summary = aiChat(
    aiMessage()
        .user( "Summarize the key points from this document" )
        .embedPdf( "/reports/quarterly-results.pdf" )
)

// Extract specific information
dates = aiChat(
    aiMessage()
        .user( "Extract all important dates and deadlines" )
        .document( "https://example.com/project-plan.pdf", "Project Plan" )
)

// Legal document review
findings = aiChat(
    aiMessage()
        .system( "You are a legal analyst" )
        .user( "Identify potential issues in this contract" )
        .embedDocument( "/legal/vendor-contract.pdf", "Vendor Agreement" ),
    {},
    { provider: "claude" }
)

// Compare documents
comparison = aiChat(
    aiMessage()
        .user( "What are the differences between these contracts?" )
        .embedPdf( "/contracts/version-1.pdf", "Original" )
        .embedPdf( "/contracts/version-2.pdf", "Revised" )
)
```

**Supported formats:** pdf, doc, docx, txt, xls, xlsx

### Mixed Multimodal

Combine multiple media types in a single conversation:

```java
// Document + Images
analysis = aiChat(
    aiMessage()
        .user( "Does the product match the specifications?" )
        .embedPdf( "/specs/product-spec.pdf" )
        .embedImage( "/photos/product-front.jpg" )
        .embedImage( "/photos/product-back.jpg" )
)

// Video + Audio sync check
result = aiChat(
    aiMessage()
        .user( "Is the audio properly synced with the video?" )
        .embedVideo( "/media/video-clip.mp4" )
        .embedAudio( "/media/audio-track.mp3" )
)

// Complete project review
review = aiChat(
    aiMessage()
        .system( "Review all materials for consistency and quality" )
        .user( "Analyze these project deliverables" )
        .embedDocument( "/project/requirements.pdf", "Requirements" )
        .embedImage( "/project/mockup.png" )
        .embedVideo( "/project/demo.mp4" )
        .embedAudio( "/project/stakeholder-feedback.mp3" )
)
```

### Provider Support Matrix

| Feature | OpenAI | Claude | Gemini | Ollama |
|---------|--------|--------|--------|--------|
| **Images** | ✅ GPT-4o, GPT-4-turbo | ✅ Claude 3+ | ✅ All vision models | ✅ LLaVA, Bakllava |
| **Audio** | ✅ GPT-4o-audio | ❌ | ✅ Gemini 1.5+, 2.0 | ❌ |
| **Video** | ❌ | ❌ | ✅ Gemini 1.5+, 2.0 | ❌ |
| **Documents** | ✅ GPT-4o | ✅ Claude 3+ | ⚠️ Via OCR | ❌ |

### File Size Guidelines

- **Images:** Up to 20MB per image
- **Audio:** Up to 25MB (OpenAI), 2GB (Gemini)
- **Video:** Up to 2GB (Gemini only)
- **Documents:** ~10MB for inline base64, larger files need upload API

**Note:** Large files consume significant context tokens. For files >10MB, consider using provider-specific file upload APIs (OpenAI `/v1/files`, Gemini File API).

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

## Structured Output

Get **type-safe, validated responses** using BoxLang classes or struct templates. Unlike `returnFormat: "json"`, structured output provides compile-time type safety and automatic validation.

### Why Structured Output?

- **Type Safety**: Get validated objects with proper types, not generic structs
- **Automatic Validation**: Schema constraints ensure correct data structure
- **Better Reliability**: Reduces hallucinations by strictly constraining format
- **IDE Support**: Full autocomplete and type checking with classes
- **No Manual Parsing**: Direct access to typed properties and methods

### Using Classes

```java
class UserProfile {
    property name="name" type="string";
    property name="email" type="string";
    property name="age" type="numeric";
    property name="skills" type="array";
    property name="preferences" type="struct";
}

// Get type-safe response
profile = aiChat( "Create a user profile for a senior developer" )
    .structuredOutput( new UserProfile() )

// Access with getters (type-safe)
println( "Name: #profile.getName()#" )
println( "Email: #profile.getEmail()#" )
println( "Age: #profile.getAge()#" )  // Guaranteed numeric
profile.getSkills().each( skill => println( "  - #skill#" ) )
```

### Using Struct Templates

```java
// Define expected structure
template = {
    "productId": 0,
    "productName": "",
    "price": 0.0,
    "category": "",
    "inStock": false,
    "tags": []
}

// Get structured response
product = aiChat( "Generate a product for a laptop" )
    .structuredOutput( template )

// All fields guaranteed to exist with correct types
println( "Product: #product.productName#" )
println( "Price: $#product.price#" )
println( "In Stock: #product.inStock#" )
```

### Multi-Turn Conversations with Structured Output

```java
class Analysis {
    property name="sentiment" type="string";
    property name="keyPoints" type="array";
    property name="score" type="numeric";
    property name="recommendation" type="string";
}

conversation = [
    { role: "system", content: "You are a product review analyzer" },
    { role: "user", content: "Analyze: Great laptop but expensive" }
]

// First analysis
analysis = aiChat( conversation )
    .structuredOutput( new Analysis() )

println( "Sentiment: #analysis.getSentiment()#" )
println( "Score: #analysis.getScore()#" )

// Continue conversation with typed context
conversation.append({
    role: "assistant",
    content: serializeJSON({
        sentiment: analysis.getSentiment(),
        score: analysis.getScore(),
        recommendation: analysis.getRecommendation()
    })
})
conversation.append({
    role: "user",
    content: "Compare with: Affordable but slow performance"
})

comparison = aiChat( conversation )
    .structuredOutput( new Analysis() )
```

### Extracting Arrays

```java
class Task {
    property name="title" type="string";
    property name="priority" type="string";
    property name="estimatedHours" type="numeric";
}

// Extract multiple items
tasks = aiChat( "Extract tasks from: Finish report by Friday (high, 4hrs), Review code tomorrow (medium, 2hrs), Update docs (low, 1hr)" )
    .structuredOutput( [ new Task() ] )

// Iterate with full type safety
tasks.each( task => {
    println( "#task.getTitle()# [#task.getPriority()#] - #task.getEstimatedHours()#hrs" )
} )
```

### Multiple Schemas (Extract Different Types)

```java
class Customer {
    property name="name" type="string";
    property name="email" type="string";
    property name="phone" type="string";
}

class Order {
    property name="orderId" type="string";
    property name="items" type="array";
    property name="total" type="numeric";
}

// Extract multiple related entities
result = aiChat( "Extract info: John Doe (john@example.com, 555-1234) ordered items A, B, C for $150, order #12345" )
    .structuredOutputs({
        "customer": new Customer(),
        "order": new Order()
    })

// Access each typed entity
println( "Customer: #result.customer.getName()#" )
println( "Email: #result.customer.getEmail()#" )
println( "Order: #result.order.getOrderId()#" )
println( "Total: $#result.order.getTotal()#" )
```

### With Tools

```java
class WeatherData {
    property name="temperature" type="numeric";
    property name="condition" type="string";
    property name="humidity" type="numeric";
    property name="forecast" type="string";
}

weatherTool = aiTool(
    "get_weather",
    "Get current weather",
    ( args ) => {
        return {
            temperature: 72,
            condition: "Sunny",
            humidity: 45,
            forecast: "Clear skies all day"
        }
    }
).addParameter( "location", "string", "City name", true )

// Get typed response with tool data
weather = aiChat( "What's the weather in San Francisco?" )
    .tools( [ weatherTool ] )
    .structuredOutput( new WeatherData() )

println( "Temperature: #weather.getTemperature()#°F" )
println( "Condition: #weather.getCondition()#" )
println( "Humidity: #weather.getHumidity()#%" )
```

### Structured Output vs JSON Return Format

| Feature | Structured Output | JSON Return Format |
|---------|------------------|-------------------|
| Type Safety | ✅ Full type safety with classes | ❌ Generic structs |
| Validation | ✅ Schema validation | ⚠️ Manual validation needed |
| IDE Support | ✅ Autocomplete, type hints | ❌ No type information |
| Reliability | ✅ Strict schema enforcement | ⚠️ May return invalid JSON |
| Complexity | Simple classes/templates | Manual parsing logic |
| Best For | Production code, type safety | Quick prototypes, flexible data |

**When to use Structured Output:**
- Production applications requiring reliability
- Type-safe code with compile-time checks
- Complex nested data structures
- When consistency is critical

**When to use JSON Return Format:**
- Quick prototypes or scripts
- Dynamic/unknown data structures
- When flexibility > type safety

### Learn More

For complete details on structured output including inheritance, validation, and advanced patterns, see:
- **[Structured Output Guide](structured-output.md)** - Complete documentation
- **[Pipeline Integration](../main-components/structured-output.md)** - Advanced patterns
- **[Course Lesson 12](../../course/lesson-12-structured-output/)** - Interactive learning

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
- **[Pipeline Overview](../main-components/overview.md)** - Learn about AI pipelines
- **[Message Templates](../main-components/messages.md)** - Advanced templating
