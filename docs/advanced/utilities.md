# Utility Functions

The bx-ai module provides powerful utility functions for text processing, token management, and working with AI models. These utilities help you prepare data, estimate costs, and optimize your AI interactions.

## Table of Contents

- [Text Chunking](#text-chunking)
- [Token Counting](#token-counting)
- [Combining Utilities](#combining-utilities)
- [Tips and Tricks](#tips-and-tricks)
- [Object Population](#object-population)

## Text Chunking

Break large texts into manageable segments that fit within AI token limits. Essential for processing long documents, articles, or books.

### `aiChunk()` Function

```java
aiChunk( text, struct options = {} )
```

Split text into smaller chunks using intelligent strategies that preserve meaning and context.

#### Basic Usage

```java
// Simple chunking with defaults
text = "Your long document text here..."
chunks = aiChunk( text )

println( "Created #chunks.len()# chunks" )
chunks.each( chunk => println( chunk ) )
```

#### Configuration Options

```java
chunks = aiChunk(
    text: longDocument,
    options: {
        chunkSize: 2000,      // Maximum characters per chunk (default: 2000)
        overlap: 200,         // Character overlap between chunks (default: 200)
        strategy: "recursive" // Chunking strategy (default: "recursive")
    }
)
```

### Chunking Strategies

#### Recursive (Default - Recommended)

Intelligently splits by trying larger units first (paragraphs → sentences → words → characters):

```java
chunks = aiChunk( text, { strategy: "recursive" } )
```

**Best for:**
- Natural language documents
- Articles, blog posts, documentation
- Preserving semantic meaning
- General-purpose text processing

**How it works:**
1. Tries to split by paragraphs (double newlines)
2. If paragraphs too large, splits by sentences (. ! ?)
3. If sentences too large, splits by words
4. If words too large, splits by characters

#### Characters

Simple character-based splitting:

```java
chunks = aiChunk( text, {
    strategy: "characters",
    chunkSize: 1000
} )
```

**Best for:**
- Consistent chunk sizes
- Code or structured text
- Maximum control over size

#### Words

Splits on word boundaries:

```java
chunks = aiChunk( text, {
    strategy: "words",
    chunkSize: 500  // ~125 words
} )
```

**Best for:**
- Preserving complete words
- Avoiding mid-word breaks
- Language processing

#### Sentences

Splits on sentence boundaries:

```java
chunks = aiChunk( text, {
    strategy: "sentences",
    chunkSize: 2000
} )
```

**Best for:**
- Preserving complete thoughts
- Question answering systems
- Semantic search preparation

#### Paragraphs

Splits on paragraph boundaries:

```java
chunks = aiChunk( text, {
    strategy: "paragraphs",
    chunkSize: 3000
} )
```

**Best for:**
- Maintaining topic coherence
- Document summarization
- Large context windows

### Understanding Overlap

Overlap preserves context between chunks by including text from the previous chunk:

```java
// No overlap - chunks are independent
chunks = aiChunk( text, { overlap: 0 } )

// 200 char overlap - preserves context
chunks = aiChunk( text, {
    chunkSize: 2000,
    overlap: 200  // Last 200 chars of chunk N start chunk N+1
} )
```

**Why use overlap?**
- Prevents losing context at chunk boundaries
- Improves semantic search accuracy
- Better for question answering across chunks
- Helps AI models maintain coherence

**Recommended overlap:** 10-20% of chunk size

```java
// 20% overlap
chunks = aiChunk( text, {
    chunkSize: 2000,
    overlap: 400
} )
```

### Real-World Examples

#### Processing Long Documents

```java
// Load a large document
bookContent = fileRead( expandPath( "./book.txt" ) )

// Chunk it for processing
chunks = aiChunk(
    text: bookContent,
    options: {
        chunkSize: 3000,  // ~750 tokens
        overlap: 300,     // 10% overlap
        strategy: "recursive"
    }
)

// Process each chunk
summaries = chunks.map( chunk => {
    return aiChat(
        message: "Summarize this text: #chunk#",
        options: { returnFormat: "single" }
    )
} )

// Combine summaries
finalSummary = aiChat(
    message: "Synthesize these summaries into one: #summaries.toList( chr(10) )#",
    options: { returnFormat: "single" }
)
```

#### Semantic Search Preparation

```java
// Chunk documents for embedding
docs = [
    { title: "Guide to BoxLang", content: fileRead( "guide.txt" ) },
    { title: "AI Patterns", content: fileRead( "patterns.txt" ) }
]

// Create searchable chunks with metadata
searchableChunks = []
docs.each( doc => {
    chunks = aiChunk( doc.content, {
        chunkSize: 1000,
        overlap: 100
    } )

    chunks.each( (chunk, index) => {
        searchableChunks.append({
            text: chunk,
            source: doc.title,
            chunkIndex: index,
            embedding: aiEmbed( chunk, {}, { returnFormat: "first" } )
        })
    } )
} )

// Now you can search these chunks
```

#### Token-Aware Chunking

```java
// Estimate tokens and chunk accordingly
text = fileRead( "large-doc.txt" )
estimatedTokens = aiTokens( text )

if ( estimatedTokens > 8000 ) {
    // Model limit is 8k, chunk with safety margin
    chunks = aiChunk( text, {
        chunkSize: 3000,  // ~750 tokens per chunk
        overlap: 300
    } )

    // Process chunks separately
    results = chunks.map( chunk => processWithAI( chunk ) )
} else {
    // Fits in one request
    result = processWithAI( text )
}
```

## Token Counting

Estimate token usage before making API calls. Essential for cost management and staying within model limits.

### `aiTokens()` Function

```java
aiTokens( text, struct options = {} )
```

Estimate token count for text using industry-standard heuristics.

#### Basic Usage

```java
// Count tokens in text
text = "Hello, how are you today?"
tokens = aiTokens( text )
println( "Estimated tokens: #tokens#" )  // ~7 tokens
```

#### Estimation Methods

##### Characters Method (Default)

Uses the rule: **1 token ≈ 4 characters** (OpenAI standard):

```java
tokens = aiTokens( text, { method: "characters" } )
```

**Best for:**
- English text
- General-purpose estimation
- Quick calculations
- Conservative estimates

##### Words Method

Uses the multiplier: **1 token ≈ 1.3 words**:

```java
tokens = aiTokens( text, { method: "words" } )
```

**Best for:**
- Non-English text
- Technical content
- More accurate word-based languages

#### Detailed Statistics

Get comprehensive token analysis:

```java
stats = aiTokens( text, {
    method: "characters",
    detailed: true
} )

println( stats )
// {
//     tokens: 150,
//     characters: 600,
//     words: 120,
//     chunks: 1,
//     method: "characters"
// }
```

#### Batch Token Counting

Count tokens across multiple text chunks:

```java
chunks = [
    "First chunk of text",
    "Second chunk of text",
    "Third chunk of text"
]

totalTokens = aiTokens( chunks )
println( "Total tokens across all chunks: #totalTokens#" )

// Detailed batch analysis
stats = aiTokens( chunks, { detailed: true } )
println( "Chunks: #stats.chunks#" )
println( "Total tokens: #stats.tokens#" )
println( "Total words: #stats.words#" )
```

### Real-World Examples

#### Cost Estimation

```java
// Estimate cost before API call
text = "Your prompt text here..."
tokens = aiTokens( text )

// OpenAI pricing (example rates)
inputCostPer1k = 0.03   // $0.03 per 1k tokens
outputEstimate = 500     // Expect ~500 token response

totalTokens = tokens + outputEstimate
estimatedCost = ( totalTokens / 1000 ) * inputCostPer1k

println( "Estimated cost: $#numberFormat( estimatedCost, '0.00' )#" )

if ( estimatedCost > 0.10 ) {
    println( "Warning: High cost request" )
}
```

#### Model Selection

```java
// Choose model based on token count
prompt = buildLargePrompt()
tokens = aiTokens( prompt )

if ( tokens > 8000 ) {
    model = "gpt-4-turbo"  // 128k context
    println( "Using large context model" )
} else if ( tokens > 4000 ) {
    model = "gpt-4"        // 8k context
    println( "Using standard model" )
} else {
    model = "gpt-3.5-turbo"  // 4k context - cheaper
    println( "Using economy model" )
}

result = aiChat( prompt, { model: model } )
```

#### Request Validation

```java
// Validate before sending
function validateRequest( text, maxTokens = 4000 ) {
    tokens = aiTokens( text )

    if ( tokens > maxTokens ) {
        throw(
            type: "TokenLimitExceeded",
            message: "Request has #tokens# tokens, limit is #maxTokens#"
        )
    }

    return {
        valid: true,
        tokens: tokens,
        percentage: ( tokens / maxTokens ) * 100
    }
}

// Use it
try {
    validation = validateRequest( userPrompt, 4000 )
    println( "Request uses #validation.percentage#% of limit" )
    result = aiChat( userPrompt )
} catch ( any e ) {
    println( "Error: #e.message#" )
    // Maybe chunk the text instead
}
```

#### Batch Processing Optimization

```java
// Process texts in optimal batches
texts = [ /* array of many texts */ ]

// Calculate tokens for each
tokensPerText = texts.map( text => aiTokens( text ) )

// Group into batches that fit in context window
maxBatchTokens = 7000  // Leave room for response
batches = []
currentBatch = []
currentTokens = 0

tokensPerText.each( (tokens, index) => {
    if ( currentTokens + tokens > maxBatchTokens ) {
        // Start new batch
        batches.append( currentBatch )
        currentBatch = [ texts[ index ] ]
        currentTokens = tokens
    } else {
        // Add to current batch
        currentBatch.append( texts[ index ] )
        currentTokens += tokens
    }
} )

// Don't forget last batch
if ( currentBatch.len() ) {
    batches.append( currentBatch )
}

println( "Processing #texts.len()# texts in #batches.len()# batches" )
```

#### Dynamic Chunking

```java
// Chunk based on token limits, not just characters
function chunkByTokens( text, maxTokens = 2000 ) {
    // Estimate total tokens
    totalTokens = aiTokens( text )

    if ( totalTokens <= maxTokens ) {
        return [ text ]
    }

    // Calculate approximate chunk size in characters
    charsPerToken = len( text ) / totalTokens
    chunkSize = floor( maxTokens * charsPerToken * 0.9 )  // 90% safety margin

    // Chunk with calculated size
    return aiChunk( text, {
        chunkSize: chunkSize,
        overlap: floor( chunkSize * 0.1 )  // 10% overlap
    } )
}

// Use it
chunks = chunkByTokens( longDocument, 1000 )
println( "Created #chunks.len()# chunks of ~1000 tokens each" )
```

## Token Counting Guidelines

### Understanding Token Ratios

Different content types have different character-to-token ratios:

| Content Type | Characters per Token | Example |
|--------------|---------------------|---------|
| English text | ~4 | "Hello world" = 3 tokens |
| Code | ~3.5 | `function foo()` = 4 tokens |
| JSON | ~3 | `{"key":"value"}` = 6 tokens |
| Technical terms | ~5 | "Parameterization" = 4 tokens |

### Best Practices

1. **Always estimate before large requests**
   ```java
   tokens = aiTokens( prompt )
   if ( tokens > 3000 ) {
       // Consider chunking
   }
   ```

2. **Use detailed stats for optimization**
   ```java
   stats = aiTokens( text, { detailed: true } )
   avgTokensPerWord = stats.tokens / stats.words  // Your actual ratio
   ```

3. **Add safety margins**
   ```java
   estimatedTokens = aiTokens( text )
   maxAllowed = 4000
   if ( estimatedTokens > maxAllowed * 0.9 ) {  // 90% threshold
       println( "Warning: Approaching token limit" )
   }
   ```

4. **Cache token counts for repeated use**
   ```java
   // Cache expensive calculations
   tokenCache = {}
   function getTokenCount( text ) {
       hash = hash( text )
       if ( !tokenCache.keyExists( hash ) ) {
           tokenCache[ hash ] = aiTokens( text )
       }
       return tokenCache[ hash ]
   }
   ```

## Combining Utilities

Use chunking and token counting together for optimal processing:

```java
// Smart document processor
function processLargeDocument( filePath, maxTokensPerChunk = 1000 ) {
    // Load document
    content = fileRead( filePath )

    // Check if chunking needed
    totalTokens = aiTokens( content )
    println( "Document has ~#totalTokens# tokens" )

    if ( totalTokens <= maxTokensPerChunk ) {
        // Process directly
        return aiChat( "Summarize: #content#" )
    }

    // Calculate optimal chunk size
    totalChars = len( content )
    charsPerToken = totalChars / totalTokens
    chunkSize = floor( maxTokensPerChunk * charsPerToken * 0.9 )

    println( "Chunking into ~#ceiling( totalTokens / maxTokensPerChunk )# chunks" )

    // Create chunks
    chunks = aiChunk( content, {
        chunkSize: chunkSize,
        overlap: floor( chunkSize * 0.1 ),
        strategy: "recursive"
    } )

    // Verify chunk tokens
    chunks.each( (chunk, i) => {
        tokens = aiTokens( chunk )
        println( "Chunk #i#: ~#tokens# tokens" )
    } )

    // Process chunks
    return chunks.map( chunk => aiChat( "Summarize: #chunk#" ) )
}

// Use it
summaries = processLargeDocument( "./large-doc.txt", 800 )
```

## Tips and Tricks

### Optimal Chunk Sizes by Use Case

```java
// Embeddings (smaller is better)
chunks = aiChunk( text, { chunkSize: 500 } )

// Summarization (larger context helps)
chunks = aiChunk( text, { chunkSize: 4000 } )

// Question Answering (medium with overlap)
chunks = aiChunk( text, {
    chunkSize: 1500,
    overlap: 300
} )

// Code analysis (preserve functions/methods)
chunks = aiChunk( code, {
    chunkSize: 2000,
    strategy: "recursive"  // Tries paragraphs (blank lines between functions)
} )
```

### Memory-Efficient Streaming

```java
// Process large files without loading entirely into memory
function processLargeFile( path ) {
    file = fileOpen( path, "read" )
    buffer = ""
    results = []

    try {
        while ( !fileIsEOF( file ) ) {
            // Read chunks
            buffer &= fileReadLine( file )

            // When buffer is large enough, process it
            if ( aiTokens( buffer ) >= 1000 ) {
                result = aiChat( "Process: #buffer#" )
                results.append( result )
                buffer = ""
            }
        }

        // Process remaining buffer
        if ( len( buffer ) ) {
            results.append( aiChat( "Process: #buffer#" ) )
        }

    } finally {
        fileClose( file )
    }

    return results
}
```

### Intelligent Overlap Strategy

```java
// Adaptive overlap based on content type
function smartChunk( text, type = "general" ) {
    // Define strategies per content type
    strategies = {
        "code": { chunkSize: 2000, overlap: 100, strategy: "paragraphs" },
        "docs": { chunkSize: 3000, overlap: 400, strategy: "recursive" },
        "qa": { chunkSize: 1500, overlap: 300, strategy: "sentences" },
        "general": { chunkSize: 2000, overlap: 200, strategy: "recursive" }
    }

    config = strategies[ type ] ?: strategies.general
    return aiChunk( text, config )
}

// Use it
codeChunks = smartChunk( sourceCode, "code" )
docChunks = smartChunk( documentation, "docs" )
```

## Object Population

The `aiPopulate()` function lets you manually convert JSON data or structs into typed BoxLang objects. Perfect for testing, caching AI responses, or working with pre-existing data.

### `aiPopulate()` Function

```java
aiPopulate( template, data )
```

Populate a class instance, struct template, or array from JSON string or struct data.

#### Basic Usage with Classes

```java
class Person {
    property name="name" type="string";
    property name="age" type="numeric";
    property name="email" type="string";
}

// From JSON string
jsonData = '{"name":"John Doe","age":30,"email":"john@example.com"}'
person = aiPopulate( new Person(), jsonData )

println( person.getName() )  // John Doe
println( person.getAge() )   // 30

// From struct
data = { name: "Jane Smith", age: 25, email: "jane@example.com" }
person = aiPopulate( new Person(), data )
```

#### Array Population

```java
class Task {
    property name="title" type="string";
    property name="priority" type="string";
    property name="completed" type="boolean";
}

// From JSON array string
tasksJson = '[
    {"title":"Task 1","priority":"high","completed":false},
    {"title":"Task 2","priority":"low","completed":true}
]'
tasks = aiPopulate( [ new Task() ], tasksJson )

tasks.each( task => {
    println( "#task.getTitle()# [#task.getPriority()#]" )
} )
```

#### Struct Template Population

```java
// Define struct template
template = {
    "productId": 0,
    "productName": "",
    "price": 0.0,
    "inStock": false
}

// Populate from JSON
productJson = '{"productId":123,"productName":"Laptop","price":999.99,"inStock":true}'
product = aiPopulate( template, productJson )

println( "Product: #product.productName# - $#product.price#" )
```

### Use Cases

#### Testing with Mock Data

```java
// Create test fixtures without AI calls
function createTestPerson( name = "Test User" ) {
    return aiPopulate( new Person(), {
        name: arguments.name,
        age: 30,
        email: "#lCase(arguments.name.replace(' ',''))#@test.com"
    } )
}

// Use in tests
testPerson = createTestPerson( "John Doe" )
assert( testPerson.getName() == "John Doe" )
assert( testPerson.getEmail() == "johndoe@test.com" )
```

#### Caching AI Responses

```java
// Cache expensive AI extractions
function getPersonInfo( text ) {
    cacheKey = hash( text )

    // Check cache first
    if ( cacheExists( cacheKey ) ) {
        cachedJson = cacheGet( cacheKey )
        return aiPopulate( new Person(), cachedJson )
    }

    // Make AI call
    person = aiChat( "Extract person info from: #text#" )
        .structuredOutput( new Person() )

    // Cache the result as JSON
    cachePut(
        cacheKey,
        serializeJSON({
            name: person.getName(),
            age: person.getAge(),
            email: person.getEmail()
        }),
        60  // 60 minute timeout
    )

    return person
}

// First call hits AI, subsequent calls use cache
person1 = getPersonInfo( "John Doe, 30, john@example.com" )
person2 = getPersonInfo( "John Doe, 30, john@example.com" )  // From cache!
```

#### Converting Existing Data

```java
// You have legacy data in structs, need typed objects
legacyUsers = queryExecute( "SELECT * FROM users" )

typedUsers = legacyUsers.map( user => {
    return aiPopulate( new User(), {
        id: user.id,
        username: user.username,
        email: user.email,
        createdDate: user.created_at
    } )
} )

// Now work with type-safe objects
typedUsers.each( user => {
    sendEmail( user.getEmail(), "Welcome!" )
} )
```

#### Transforming API Responses

```java
// External API returns JSON
apiResponse = httpRequest( "https://api.example.com/products" )
    .send()
    .json()

// Convert to typed objects
products = apiResponse.data.map( item => {
    return aiPopulate( new Product(), item )
} )

// Type-safe operations
expensiveProducts = products.filter( p => p.getPrice() > 100 )
```

### With Nested Objects

```java
class Address {
    property name="street" type="string";
    property name="city" type="string";
    property name="zipCode" type="string";
}

class Customer {
    property name="name" type="string";
    property name="email" type="string";
    property name="address" type="any";  // Will contain Address instance
}

// Nested data
data = {
    "name": "John Doe",
    "email": "john@example.com",
    "address": {
        "street": "123 Main St",
        "city": "San Francisco",
        "zipCode": "94102"
    }
}

// Populate with nested objects
customer = aiPopulate( new Customer(), data )

// Access nested properties
println( customer.getName() )
println( customer.getAddress().getCity() )  // San Francisco
```

### Validation and Error Handling

```java
try {
    // Invalid JSON
    person = aiPopulate( new Person(), "not valid json" )
} catch( any e ) {
    println( "Invalid JSON: #e.message#" )
}

try {
    // Wrong data type for template
    result = aiPopulate( "not a template", { name: "Test" } )
} catch( any e ) {
    println( "Invalid template: #e.message#" )
}

try {
    // Mismatched array types
    tasks = aiPopulate( [ new Task() ], "not an array json" )
} catch( any e ) {
    println( "Invalid array data: #e.message#" )
}
```

### Comparison: aiPopulate vs Structured Output

| Feature | `aiPopulate()` | `.structuredOutput()` |
|---------|----------------|----------------------|
| Purpose | Manual population | AI extraction |
| Input | JSON/struct data | Natural language prompt |
| AI Call | ❌ No (instant) | ✅ Yes (costs tokens) |
| Use Case | Testing, caching, conversion | Live AI extraction |
| Type Safety | ✅ Yes | ✅ Yes |
| Validation | ✅ Yes | ✅ Yes |
| Best For | Known data, offline processing | Unknown data, AI parsing |

**Use `aiPopulate()` when:**
- Writing tests with mock data
- Working with cached responses
- Converting existing JSON/structs to typed objects
- No AI interpretation needed

**Use `.structuredOutput()` when:**
- Extracting data from natural language
- Need AI to understand and parse content
- Dealing with unstructured text
- Real-time data extraction

### Learn More

For complete details on structured output and object population:
- **[Structured Output Guide](../chatting/structured-output.md)** - Full documentation
- **[Advanced Chatting](../chatting/advanced-chatting.md#structured-output)** - Integration examples
- **[Course Lesson 12](../../course/lesson-12-structured-output/)** - Interactive learning
