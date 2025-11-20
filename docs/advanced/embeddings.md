# Embeddings

Generate numerical vector representations of text that capture semantic meaning. Embeddings power semantic search, recommendations, clustering, and similarity detection.

## What are Embeddings?

Embeddings convert text into high-dimensional vectors (arrays of numbers) where semantically similar texts have similar vector representations.

**Example:**
```
"cat" → [0.2, -0.5, 0.8, 0.1, ...]      (1536 dimensions)
"kitten" → [0.3, -0.4, 0.7, 0.2, ...]   (similar vector)
"car" → [-0.6, 0.3, -0.2, 0.9, ...]     (different vector)
```

**Key Properties:**
- Similar meanings = Close vectors
- Different meanings = Distant vectors
- Math operations preserve semantic relationships
- Dimension count varies by model (typically 768-3072)

## The `aiEmbed()` Function

```java
aiEmbed( input, struct params = {}, struct options = {} )
```

Generate embeddings for single texts or batches.

### Basic Usage

#### Single Text

```java
// Generate embedding for one text
embedding = aiEmbed( "BoxLang is awesome" )

println( "Model: #embedding.model#" )
println( "Dimensions: #embedding.data.first().embedding.len()#" )

// Access the vector
vector = embedding.data.first().embedding
println( "First 5 values: #vector.slice( 1, 5 )#" )
```

#### Batch Processing

```java
// Generate embeddings for multiple texts
texts = [
    "BoxLang is a dynamic language",
    "Java runs on the JVM", 
    "Python is easy to learn"
]

response = aiEmbed( texts )

// Access all embeddings
response.data.each( (item, index) => {
    println( "Text #index#: #texts[ index ]#" )
    println( "Vector length: #item.embedding.len()#" )
} )
```

### Configuration Options

#### Provider Selection

```java
// Use specific provider
embedding = aiEmbed(
    input: "Hello World",
    options: { provider: "openai" }
)

// Use Claude (if available)
embedding = aiEmbed(
    input: "Hello World", 
    options: { provider: "claude" }
)

// Use local Ollama (free!)
embedding = aiEmbed(
    input: "Hello World",
    options: { provider: "ollama" }
)
```

#### Model Selection

```java
// OpenAI - High quality, expensive
embedding = aiEmbed(
    input: "Technical documentation text",
    params: { model: "text-embedding-3-large" },  // 3072 dimensions
    options: { provider: "openai" }
)

// OpenAI - Balanced, default
embedding = aiEmbed(
    input: "General text",
    params: { model: "text-embedding-3-small" },  // 1536 dimensions
    options: { provider: "openai" }
)

// Ollama - Free, local, private
embedding = aiEmbed(
    input: "Private data",
    params: { model: "nomic-embed-text" },  // 768 dimensions
    options: { provider: "ollama" }
)

// Gemini - Google's model
embedding = aiEmbed(
    input: "Search query text",
    params: { model: "text-embedding-004" },
    options: { provider: "gemini" }
)
```

### Return Formats

Control what data is returned:

#### Raw Response (Default)

Full API response with metadata:

```java
response = aiEmbed( "Hello", {}, { returnFormat: "raw" } )

println( response )
// {
//     object: "list",
//     data: [
//         { embedding: [...], index: 0, object: "embedding" }
//     ],
//     model: "text-embedding-3-small",
//     usage: { prompt_tokens: 2, total_tokens: 2 }
// }
```

#### Embeddings Array

Get just the vectors:

```java
embeddings = aiEmbed( 
    [ "Hello", "World" ],
    {},
    { returnFormat: "embeddings" }
)

println( embeddings )
// [
//     [0.1, -0.5, 0.3, ...],  // First vector
//     [0.2, -0.4, 0.4, ...]   // Second vector
// ]
```

#### First Vector

Get single vector for single input:

```java
vector = aiEmbed( 
    "Hello World",
    {},
    { returnFormat: "first" }
)

println( vector )
// [0.1, -0.5, 0.3, 0.8, ...]

// Perfect for single-text use cases
query = "How do I use embeddings?"
queryVector = aiEmbed( query, {}, { returnFormat: "first" } )
```

## Use Cases

### Semantic Search

Find documents similar to a query using vector similarity:

```java
// 1. Embed all documents
documents = [
    "BoxLang is a dynamic JVM language",
    "Python is great for data science",
    "Java provides strong typing",
    "BoxLang compiles to Java bytecode"
]

docEmbeddings = aiEmbed( documents, {}, { returnFormat: "embeddings" } )

// 2. Embed search query
query = "Tell me about BoxLang"
queryEmbedding = aiEmbed( query, {}, { returnFormat: "first" } )

// 3. Calculate similarity scores
scores = docEmbeddings.map( (docEmb, index) => {
    return {
        index: index,
        document: documents[ index ],
        similarity: cosineSimilarity( queryEmbedding, docEmb )
    }
} )

// 4. Sort by similarity
scores.sort( (a, b) => b.similarity - a.similarity )

// 5. Show results
println( "Top matches for: #query#" )
scores.each( result => {
    println( "#numberFormat( result.similarity * 100, '0.0' )#% - #result.document#" )
} )

// Helper function for cosine similarity
function cosineSimilarity( v1, v2 ) {
    dot = 0
    mag1 = 0
    mag2 = 0
    
    for ( var i = 1; i <= v1.len(); i++ ) {
        dot += v1[ i ] * v2[ i ]
        mag1 += v1[ i ] * v1[ i ]
        mag2 += v2[ i ] * v2[ i ]
    }
    
    return dot / ( sqrt( mag1 ) * sqrt( mag2 ) )
}
```

### Text Clustering

Group similar texts together:

```java
// Articles to cluster
articles = [
    "Machine learning basics",
    "Introduction to neural networks",
    "Cooking pasta perfectly",
    "Deep learning fundamentals",
    "Italian cuisine recipes"
]

// Generate embeddings
embeddings = aiEmbed( articles, {}, { returnFormat: "embeddings" } )

// Calculate pairwise similarities
similarities = []
for ( var i = 1; i <= articles.len(); i++ ) {
    for ( var j = i + 1; j <= articles.len(); j++ ) {
        sim = cosineSimilarity( embeddings[ i ], embeddings[ j ] )
        similarities.append({
            doc1: i,
            doc2: j,
            text1: articles[ i ],
            text2: articles[ j ],
            similarity: sim
        })
    }
}

// Sort by similarity
similarities.sort( (a, b) => b.similarity - a.similarity )

// Show clusters (top similar pairs)
println( "Similar documents:" )
similarities.filter( s => s.similarity > 0.7 ).each( pair => {
    println( "- #pair.text1#" )
    println( "  #pair.text2#" )
    println( "  Similarity: #numberFormat( pair.similarity * 100, '0.0' )#%" )
    println()
} )
```

### Recommendations

Recommend items based on similarity:

```java
// Product descriptions
products = [
    { id: 1, name: "Laptop", desc: "Powerful computing device" },
    { id: 2, name: "Mouse", desc: "Computer pointing device" },
    { id: 3, name: "Book", desc: "Reading material" },
    { id: 4, name: "Keyboard", desc: "Computer typing device" },
    { id: 5, name: "Magazine", desc: "Periodic reading material" }
]

// Embed all products
productEmbeddings = products.map( p => {
    return {
        id: p.id,
        name: p.name,
        embedding: aiEmbed( p.desc, {}, { returnFormat: "first" } )
    }
} )

// User views a laptop
viewedProduct = products.first()
viewedEmbedding = productEmbeddings.first().embedding

// Find similar products
recommendations = productEmbeddings
    .filter( p => p.id != viewedProduct.id )  // Exclude viewed item
    .map( p => {
        return {
            id: p.id,
            name: p.name,
            similarity: cosineSimilarity( viewedEmbedding, p.embedding )
        }
    } )
    .sort( (a, b) => b.similarity - a.similarity )

// Show top 3 recommendations
println( "Because you viewed: #viewedProduct.name#" )
println( "You might also like:" )
recommendations.slice( 1, 3 ).each( rec => {
    println( "- #rec.name# (#numberFormat( rec.similarity * 100, '0.0' )#% match)" )
} )
```

### Duplicate Detection

Find duplicate or near-duplicate content:

```java
// Content to check
contents = [
    "BoxLang is a modern JVM language",
    "Java is a programming language",
    "BoxLang runs on the JVM and is modern",  // Near duplicate of first
    "Python is interpreted",
    "BoxLang: a contemporary JVM-based language"  // Near duplicate of first
]

// Generate embeddings
embeddings = aiEmbed( contents, {}, { returnFormat: "embeddings" } )

// Find duplicates (similarity > 0.9)
duplicates = []
for ( var i = 1; i <= contents.len(); i++ ) {
    for ( var j = i + 1; j <= contents.len(); j++ ) {
        sim = cosineSimilarity( embeddings[ i ], embeddings[ j ] )
        if ( sim > 0.9 ) {
            duplicates.append({
                index1: i,
                index2: j,
                text1: contents[ i ],
                text2: contents[ j ],
                similarity: sim
            })
        }
    }
}

// Report duplicates
if ( duplicates.len() ) {
    println( "Found #duplicates.len()# potential duplicates:" )
    duplicates.each( dup => {
        println( "---" )
        println( "Original: #dup.text1#" )
        println( "Duplicate: #dup.text2#" )
        println( "Similarity: #numberFormat( dup.similarity * 100, '0.00' )#%" )
    } )
} else {
    println( "No duplicates found" )
}
```

### RAG (Retrieval Augmented Generation)

Combine embeddings with AI chat for intelligent Q&A:

```java
// Knowledge base
knowledgeBase = [
    "BoxLang compiles to Java bytecode and runs on the JVM",
    "BoxLang has dynamic typing with optional type hints",
    "BoxLang supports functional and object-oriented programming",
    "BoxLang modules can be installed via CommandBox",
    "BoxLang includes built-in functions for AI integration"
]

// Embed knowledge base
kbEmbeddings = aiEmbed( knowledgeBase, {}, { returnFormat: "embeddings" } )

// Function to answer questions
function answerQuestion( question ) {
    // 1. Embed the question
    questionEmb = aiEmbed( question, {}, { returnFormat: "first" } )
    
    // 2. Find most relevant knowledge
    relevantDocs = kbEmbeddings
        .map( (emb, index) => {
            return {
                text: knowledgeBase[ index ],
                similarity: cosineSimilarity( questionEmb, emb )
            }
        } )
        .sort( (a, b) => b.similarity - a.similarity )
        .slice( 1, 3 )  // Top 3
    
    // 3. Build context from relevant docs
    context = relevantDocs.map( d => d.text ).toList( chr(10) )
    
    // 4. Generate answer with context
    prompt = "
        Context:
        #context#
        
        Question: #question#
        
        Answer the question based only on the context above.
    "
    
    answer = aiChat( prompt, {}, { returnFormat: "single" } )
    
    return {
        question: question,
        answer: answer,
        sources: relevantDocs.map( d => d.text )
    }
}

// Use it
result = answerQuestion( "How do I install BoxLang modules?" )
println( "Q: #result.question#" )
println( "A: #result.answer#" )
println( "Sources used:" )
result.sources.each( s => println( "  - #s#" ) )
```

## Advanced Techniques

### Dimension Reduction

Some models support dimension reduction for faster processing:

```java
// OpenAI supports dimension parameter
embedding = aiEmbed(
    input: "Text to embed",
    params: { 
        model: "text-embedding-3-large",
        dimensions: 1024  // Reduce from 3072 to 1024
    },
    options: { provider: "openai" }
)

// Smaller vectors = faster similarity calculations
// Trade-off: slightly lower accuracy
```

### Caching Embeddings

Embeddings are expensive - cache them:

```java
// Simple file-based cache
component {
    property name="cacheDir" default="./embeddings-cache";
    
    function init() {
        if ( !directoryExists( variables.cacheDir ) ) {
            directoryCreate( variables.cacheDir )
        }
        return this
    }
    
    function getEmbedding( required string text, struct params = {} ) {
        // Generate cache key
        key = hash( text & serializeJSON( params ) )
        cachePath = "#variables.cacheDir#/#key#.json"
        
        // Check cache
        if ( fileExists( cachePath ) ) {
            cached = deserializeJSON( fileRead( cachePath ) )
            println( "Cache HIT: #left( text, 50 )#..." )
            return cached
        }
        
        // Generate new embedding
        println( "Cache MISS: #left( text, 50 )#..." )
        embedding = aiEmbed( text, params, { returnFormat: "first" } )
        
        // Cache it
        fileWrite( cachePath, serializeJSON( embedding ) )
        
        return embedding
    }
}

// Use cached embeddings
cache = new EmbeddingCache()

// First call - generates embedding
emb1 = cache.getEmbedding( "BoxLang is awesome" )

// Second call - uses cache (instant!)
emb2 = cache.getEmbedding( "BoxLang is awesome" )
```

### Batch Optimization

Process large datasets efficiently:

```java
// Process 1000 documents efficiently
documents = loadDocuments()  // Returns 1000 docs

// Batch into groups of 100 (API limits)
batchSize = 100
allEmbeddings = []

for ( var i = 1; i <= documents.len(); i += batchSize ) {
    batch = documents.slice( i, min( i + batchSize - 1, documents.len() ) )
    
    println( "Processing batch #ceiling( i / batchSize )#..." )
    
    // Single API call for entire batch
    batchEmbeddings = aiEmbed( batch, {}, { returnFormat: "embeddings" } )
    allEmbeddings.append( batchEmbeddings, true )
    
    // Rate limiting
    sleep( 1000 )  // 1 second between batches
}

println( "Generated #allEmbeddings.len()# embeddings" )
```

### Chunked Document Embeddings

Embed large documents by chunks:

```java
// Large document
document = fileRead( "large-book.txt" )

// Chunk it
chunks = aiChunk( document, { 
    chunkSize: 1000,
    overlap: 100,
    strategy: "recursive"
} )

// Embed all chunks with metadata
chunkEmbeddings = chunks.map( (chunk, index) => {
    return {
        chunkIndex: index,
        text: chunk,
        embedding: aiEmbed( chunk, {}, { returnFormat: "first" } ),
        tokenCount: aiTokens( chunk )
    }
} )

// Now you can search within the document
function searchDocument( query ) {
    queryEmb = aiEmbed( query, {}, { returnFormat: "first" } )
    
    results = chunkEmbeddings
        .map( chunk => {
            return {
                chunkIndex: chunk.chunkIndex,
                text: chunk.text,
                similarity: cosineSimilarity( queryEmb, chunk.embedding )
            }
        } )
        .sort( (a, b) => b.similarity - a.similarity )
        .slice( 1, 5 )  // Top 5 chunks
    
    return results
}

// Search
matches = searchDocument( "What is BoxLang?" )
matches.each( match => {
    println( "Chunk ##match.chunkIndex# (Score: #numberFormat( match.similarity * 100, '0.0' )#%)" )
    println( left( match.text, 100 ) & "..." )
    println()
} )
```

## Provider Comparison

### OpenAI

**Models:**
- `text-embedding-3-small` (1536 dimensions) - Default, balanced
- `text-embedding-3-large` (3072 dimensions) - Highest quality
- `text-embedding-ada-002` (1536 dimensions) - Legacy

**Pros:**
- High quality embeddings
- Good for English text
- Supports dimension reduction

**Cons:**
- Requires API key
- Costs money
- Data sent to OpenAI servers

**Usage:**
```java
embedding = aiEmbed(
    "Text",
    { model: "text-embedding-3-small" },
    { provider: "openai" }
)
```

### Ollama

**Models:**
- `nomic-embed-text` (768 dimensions) - Recommended
- `mxbai-embed-large` (1024 dimensions) - High quality
- Many others available

**Pros:**
- Completely free
- Runs locally
- Private - data stays on your machine
- No API key needed

**Cons:**
- Requires Ollama installation
- Slightly lower quality than OpenAI
- Slower than API calls

**Setup:**
```bash
# Install Ollama
brew install ollama  # macOS
# or download from ollama.ai

# Pull embedding model
ollama pull nomic-embed-text
```

**Usage:**
```java
embedding = aiEmbed(
    "Text",
    { model: "nomic-embed-text" },
    { provider: "ollama" }
)
```

### Gemini

**Models:**
- `text-embedding-004` (768 dimensions)
- `embedding-001` (768 dimensions) - Legacy

**Pros:**
- Good quality
- Google infrastructure
- Competitive pricing

**Cons:**
- Requires API key
- Data sent to Google

**Usage:**
```java
embedding = aiEmbed(
    "Text",
    { model: "text-embedding-004" },
    { provider: "gemini" }
)
```

## Best Practices

### 1. Choose the Right Model

```java
// High-stakes semantic search - use best quality
embedding = aiEmbed( text, { model: "text-embedding-3-large" } )

// General purpose - balanced
embedding = aiEmbed( text, { model: "text-embedding-3-small" } )

// Privacy-first or cost-free - local Ollama
embedding = aiEmbed( text, { model: "nomic-embed-text" }, { provider: "ollama" } )
```

### 2. Batch When Possible

```java
// ❌ Inefficient - many API calls
texts.each( text => {
    embedding = aiEmbed( text )
} )

// ✅ Efficient - single API call
embeddings = aiEmbed( texts, {}, { returnFormat: "embeddings" } )
```

### 3. Cache Embeddings

```java
// Embeddings don't change - cache them
// Use database, Redis, or file system
```

### 4. Normalize Text

```java
function normalizeText( text ) {
    return text
        .trim()
        .lcase()
        .reReplace( "\s+", " ", "all" )  // Normalize whitespace
}

// Use normalized text for consistency
normalized = normalizeText( userInput )
embedding = aiEmbed( normalized )
```

### 5. Handle Errors Gracefully

```java
try {
    embedding = aiEmbed( text )
} catch ( any e ) {
    // Log error
    writeLog( "Embedding failed: #e.message#" )
    
    // Fallback strategy
    if ( e.message.findNoCase( "rate limit" ) ) {
        sleep( 5000 )
        embedding = aiEmbed( text )  // Retry
    } else {
        // Use cached or default embedding
        embedding = getDefaultEmbedding()
    }
}
```

### 6. Monitor Costs

```java
// Track API usage
function trackEmbedding( text, provider = "openai" ) {
    tokens = aiTokens( text )
    
    // Rough cost estimates (example rates)
    costs = {
        "openai": 0.0001,  // $0.0001 per 1k tokens
        "gemini": 0.00005,
        "ollama": 0  // Free!
    }
    
    cost = ( tokens / 1000 ) * costs[ provider ]
    
    // Log for tracking
    logUsage( provider, tokens, cost )
    
    return aiEmbed( text, {}, { provider: provider } )
}
```

## Troubleshooting

### Empty or Invalid Embeddings

```java
// Always validate embeddings
embedding = aiEmbed( text )
if ( !embedding.data.len() || !embedding.data.first().embedding.len() ) {
    throw( "Invalid embedding returned" )
}
```

### Dimension Mismatches

```java
// Ensure all embeddings have same dimensions
model = "text-embedding-3-small"  // Store this!

doc1Emb = aiEmbed( doc1, { model: model } )
doc2Emb = aiEmbed( doc2, { model: model } )  // Same model!

// ❌ Don't mix models
doc1Emb = aiEmbed( doc1, { model: "text-embedding-3-small" } )   // 1536 dims
doc2Emb = aiEmbed( doc2, { model: "text-embedding-3-large" } )   // 3072 dims
// Can't calculate similarity - different dimensions!
```

### Similarity Scores

```java
// Understand score meanings
// 1.0 = Identical
// 0.9+ = Very similar
// 0.7-0.9 = Similar
// 0.5-0.7 = Somewhat related
// <0.5 = Different

function interpretSimilarity( score ) {
    if ( score >= 0.9 ) return "Very similar"
    if ( score >= 0.7 ) return "Similar"
    if ( score >= 0.5 ) return "Somewhat related"
    return "Different"
}
```

## Summary

Embeddings enable powerful semantic understanding:

- **Generate**: Use `aiEmbed()` for single or batch processing
- **Search**: Compare vectors with cosine similarity
- **Optimize**: Cache embeddings, batch requests, choose right model
- **Apply**: Semantic search, clustering, recommendations, RAG

Start with OpenAI for quality, try Ollama for privacy and cost savings!
