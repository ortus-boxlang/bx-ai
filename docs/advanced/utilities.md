# Utility Functions

The bx-ai module provides powerful utility functions for text processing, token management, and working with AI models. These utilities help you prepare data, estimate costs, and optimize your AI interactions.

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
