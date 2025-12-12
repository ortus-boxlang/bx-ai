# aiChunk

Chunk text into smaller, manageable segments for AI processing, embeddings, or semantic search. Supports multiple chunking strategies including recursive, character, word, sentence, and paragraph-based splitting.

## Syntax

```javascript
aiChunk(text, options)
```

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `text` | string | Yes | - | The text to chunk into segments |
| `options` | struct | No | `{}` | Configuration struct for chunking behavior |

### Options Structure

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `chunkSize` | numeric | `1000` | The maximum size of each chunk (in characters) |
| `overlap` | numeric | `200` | The number of overlapping characters between chunks |
| `strategy` | string | `"recursive"` | Chunking strategy: "recursive", "characters", "words", "sentences", "paragraphs" |

### Chunking Strategies

- **recursive**: Intelligently splits on paragraph, sentence, then word boundaries
- **characters**: Fixed-size character splitting (simple but may break words)
- **words**: Splits on word boundaries (preserves whole words)
- **sentences**: Splits on sentence boundaries (preserves complete sentences)
- **paragraphs**: Splits on paragraph boundaries (preserves complete paragraphs)

## Returns

Returns an array of text chunks (strings). Each chunk respects the `chunkSize` limit and includes `overlap` characters from the previous chunk for context continuity.

## Examples

### Basic Chunking

```javascript
// Simple chunking with defaults (1000 chars, 200 overlap)
text = "Very long document text...";
chunks = aiChunk( text );

println( "Total chunks: #chunks.len()#" );
chunks.each( ( chunk, index ) => {
    println( "Chunk ##index##: #chunk.len()# characters" );
});
```

### Custom Chunk Size

```javascript
// Smaller chunks for embeddings
text = fileRead( "documentation.txt" );
chunks = aiChunk( text, {
    chunkSize: 500,
    overlap: 100
});

// Generate embeddings for each chunk
embeddings = chunks.map( chunk => aiEmbed( chunk ) );
```

### Word-Based Strategy

```javascript
// Ensure chunks don't break words
article = "This is a long article with many paragraphs...";
chunks = aiChunk( article, {
    strategy: "words",
    chunkSize: 800,
    overlap: 150
});
```

### Sentence-Based Strategy

```javascript
// Keep sentences intact
document = "First sentence. Second sentence. Third sentence...";
chunks = aiChunk( document, {
    strategy: "sentences",
    chunkSize: 1500,
    overlap: 200
});
```

### Paragraph-Based Strategy

```javascript
// Preserve paragraph structure
book = "Chapter 1" & char(10) & char(10) & "Paragraph 1..." & char(10) & char(10) & "Paragraph 2...";
chunks = aiChunk( book, {
    strategy: "paragraphs",
    chunkSize: 2000,
    overlap: 0  // No overlap for paragraph-level chunking
});
```

### Recursive Strategy (Default)

```javascript
// Smart chunking with hierarchy: paragraphs → sentences → words
content = fileRead( "large-file.txt" );
chunks = aiChunk( content, {
    strategy: "recursive",
    chunkSize: 1000,
    overlap: 200
});

// Best for natural language processing
```

### Processing Large Documents

```javascript
// Chunk and process large document
document = fileRead( "report.txt" );

// Chunk into manageable pieces
chunks = aiChunk( document, {
    chunkSize: 800,
    overlap: 150
});

// Process each chunk
summaries = chunks.map( ( chunk ) => {
    return aiChat( "Summarize: #chunk#" );
});

// Combine summaries
finalSummary = aiChat( "Combine these summaries: " & summaries.toJSON() );
```

### For Vector Search

```javascript
// Prepare document for vector database
doc = fileRead( "knowledge-base.txt" );

// Create searchable chunks
chunks = aiChunk( doc, {
    chunkSize: 500,   // Optimal for embedding models
    overlap: 100,      // Context continuity
    strategy: "recursive"
});

// Store with embeddings
chunks.each( ( chunk, idx ) => {
    embedding = aiEmbed( chunk );
    vectorDB.insert({
        id: idx,
        text: chunk,
        vector: embedding
    });
});
```

### Estimate Before Chunking

```javascript
// Check token count before chunking
text = fileRead( "large-doc.txt" );
totalTokens = aiTokens( text );

println( "Total tokens: #totalTokens#" );

if ( totalTokens > 8000 ) {
    // Chunk for processing
    chunks = aiChunk( text, {
        chunkSize: 2000,  // ~500 tokens per chunk
        overlap: 400
    });

    println( "Split into #chunks.len()# chunks" );
}
```

### Chunk and Embed

```javascript
// Complete chunking + embedding workflow
document = fileRead( "content.txt" );

// Chunk the document
chunks = aiChunk( document, {
    chunkSize: 600,
    overlap: 120,
    strategy: "recursive"
});

// Generate embeddings
embeddedChunks = chunks.map( ( chunk, idx ) => {
    return {
        id: idx,
        text: chunk,
        embedding: aiEmbed( chunk, {
            options: { returnFormat: "first" }
        })
    };
});

// Now ready for vector storage
```

### Overlapping Context

```javascript
// Demonstrate overlap importance
text = "The cat sat on the mat. The dog ran in the park. The bird flew in the sky.";

// Without overlap
noOverlap = aiChunk( text, {
    chunkSize: 30,
    overlap: 0
});
println( "No overlap: #noOverlap.toJSON()#" );

// With overlap
withOverlap = aiChunk( text, {
    chunkSize: 30,
    overlap: 10
});
println( "With overlap: #withOverlap.toJSON()#" );
// Overlap preserves context between chunks
```

## Notes

- 📏 **Size Management**: Chunk size is in characters, not tokens (use `aiTokens()` to estimate)
- 🔄 **Overlap Benefits**: Overlap prevents context loss at chunk boundaries
- 🎯 **Strategy Selection**: Choose strategy based on content type and use case
- 💾 **Memory Efficiency**: Chunks large documents without loading entire content in memory
- 🔍 **Search Optimization**: Smaller chunks (400-600 chars) work best for semantic search
- 📚 **Embedding Limits**: Most embedding models have token limits (e.g., 8192 tokens)

## Related Functions

- [`aiEmbed()`](aiEmbed.md) - Generate embeddings for chunks
- [`aiTokens()`](aiTokens.md) - Estimate token counts
- [`aiVectorMemory()`](aiVectorMemory.md) - Store chunked documents with embeddings
- [`aiDocuments()`](aiDocuments.md) - Load and process documents

## Best Practices

✅ **Match chunk size to use case** - Smaller for search (500), larger for summarization (1500)

✅ **Use overlap for continuity** - 15-20% overlap prevents context loss

✅ **Choose appropriate strategy** - Recursive for mixed content, sentences for natural breaks

✅ **Test different settings** - Optimal size varies by content type and model

✅ **Estimate tokens first** - Use `aiTokens()` to verify chunks fit model limits

❌ **Don't chunk too small** - Very small chunks lose context (minimum ~200 characters)

❌ **Don't ignore overlap** - Zero overlap can break semantic meaning across boundaries

❌ **Don't use fixed character strategy** - It breaks words and sentences unnaturally
