# 📚 Document Loaders

> **Load documents from any source with a fluent, powerful API**

Document loaders extract content from various sources (files, URLs, databases) and convert them into standardized `Document` objects for AI workflows and RAG pipelines.

## 🎯 Quick Start

```javascript
// Simple file loading
docs = aiDocuments( "/path/to/file.md" ).load()

// Advanced with filtering and chunking
docs = aiDocuments( "/docs", { type: "directory" } )
    .recursive()
    .extensions( ["md", "txt"] )
    .filter( ( doc ) => doc.getContentLength() > 100 )
    .load()

// Direct to memory
result = aiDocuments( "/knowledge-base" )
    .toMemory( myVectorMemory, { chunkSize: 500, overlap: 50 } )
```

## 📖 Available Loaders

| Loader | Purpose | Best For |
|--------|---------|----------|
| [TextLoader](./text-loader.md) | 📄 Plain text files | Simple text, logs, notes |
| [MarkdownLoader](./markdown-loader.md) | 📝 Markdown with header splitting | Documentation, blogs, READMEs |
| [CSVLoader](./csv-loader.md) | 📊 CSV/TSV data | Tabular data, exports |
| [JSONLoader](./json-loader.md) | 🔷 JSON data | APIs, config files |
| [XMLLoader](./xml-loader.md) | 🗂️ XML documents | Config files, legacy systems |
| [FeedLoader](./feed-loader.md) | 📰 RSS/Atom feeds | News, blogs, podcasts |
| [SQLLoader](./sql-loader.md) | 🗄️ Database queries | Structured data, RAG over DBs |
| [HTTPLoader](./http-loader.md) | 🌐 HTTP/HTTPS URLs | Web pages, APIs |
| [TikaLoader](./tika-loader.md) | 📎 PDF, Word, Excel, etc. | Office documents |
| [WebCrawlerLoader](./webcrawler-loader.md) | 🕷️ Multi-page web scraping | Entire websites, documentation sites |
| [DirectoryLoader](./directory-loader.md) | 📁 Batch file loading | Multiple files, folder structures |

## 🚀 Core Concepts

### Document Object

Every loader returns standardized `Document` objects with:

```javascript
{
    id: "unique-id",              // Auto-generated or custom
    content: "document text...",  // The actual content
    metadata: { ... },            // Source, type, etc.
    embedding: []                 // Vector embedding (empty until computed)
}
```

### Fluent API

All loaders support a fluent, chainable API:

```javascript
loader = aiDocuments( source, config )
    .filter( predicate )          // Filter documents
    .map( transformer )           // Transform documents
    .chunkSize( 500 )            // Configure chunking
    .onProgress( callback )       // Track progress
```

### Loading Methods

| Method | Purpose | Returns |
|--------|---------|---------|
| `load()` | Load all documents | Array of Documents |
| `loadAsync()` | Async loading | BoxFuture<Array> |
| `loadAsStream()` | Java Stream | java.util.stream.Stream |
| `loadBatch(size)` | Batch loading | Array (up to size) |
| `each(callback)` | Process one-by-one | IDocumentLoader |
| `toMemory(memory, options)` | Load + seed memory | Ingestion report |

## 🎨 Common Patterns

### Pattern: Filter + Transform

```javascript
docs = aiDocuments( "/articles" )
    .filter( ( doc ) => doc.metadata.language == "en" )
    .map( ( doc ) => {
        doc.content = doc.content.uCase();
        return doc;
    } )
    .load()
```

### Pattern: Progress Tracking

```javascript
aiDocuments( "/large-dataset" )
    .onProgress( ( completed, total, doc ) => {
        println( "Loading: #completed#/#total# - #doc.metadata.source#" );
    } )
    .load()
```

### Pattern: Memory-Efficient Processing

```javascript
aiDocuments( "/huge-dataset" )
    .each( ( doc ) => {
        processDocument( doc );
        myMemory.seed( [ doc ] );
    } )
```

### Pattern: Batch Loading

```javascript
loader = aiDocuments( "/documents" )
var batch = loader.loadBatch( 100 )

while ( batch.len() > 0 ) {
    // Process batch
    myMemory.seed( batch )
    batch = loader.loadBatch( 100 )
}
```

### Pattern: Multi-Memory Fan-Out

```javascript
result = aiDocuments( "/docs" )
    .toMemory( 
        [ chromaMemory, pgVectorMemory ], 
        { chunkSize: 500, async: true }
    )
```

## 🔧 Configuration Options

### Common Options (All Loaders)

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `type` | string | auto-detect | Explicit loader type |
| `encoding` | string | UTF-8 | File encoding |
| `chunkSize` | numeric | 0 | Chunk size (0 = no chunking) |
| `overlap` | numeric | 0 | Overlap between chunks |
| `includeMetadata` | boolean | true | Include source metadata |
| `continueOnError` | boolean | true | Continue on load errors |

### Loader-Specific Options

See individual loader pages for detailed options:
- **DirectoryLoader**: `recursive`, `extensions`, `exclude`
- **MarkdownLoader**: `splitByHeaders`, `removeCodeBlocks`
- **CSVLoader**: `delimiter`, `rowMode`, `columns`
- **HTTPLoader**: `method`, `headers`, `contentType`
- **FeedLoader**: `maxItems`, `sinceDate`, `categories`
- And more...

## 📊 Document Methods

### Content Operations

```javascript
doc.hasContent()                  // Check if content exists
doc.getContentLength()            // Get character count
doc.preview( 100 )               // Get first 100 chars
doc.getTokenCount()              // Estimate token count
doc.exceedsTokenLimit( 4000 )    // Check if over limit
```

### Chunking

```javascript
chunks = doc.chunk( 500, 100 )   // Split into 500-char chunks, 100 overlap
```

### Validation

```javascript
result = doc.validate(
    minLength: 50,
    maxLength: 5000,
    requiredMetadata: ["source", "author"]
)
// Returns: { valid: boolean, errors: array }
```

### Deduplication

```javascript
hash1 = doc1.hash()                    // MD5 hash of content
fingerprint = doc.fingerprint()        // Content + metadata hash
areEqual = doc1.equals( doc2 )         // Compare by hash
```

### Merging

```javascript
doc1.merge( doc2 )                     // Merge one document
doc1.merge( [doc2, doc3, doc4] )       // Merge multiple
```

## 🔄 Loader Lifecycle

```
aiDocuments(source)
    ↓
Configuration (fluent methods)
    ↓
Filters & Transforms
    ↓
Loading
    ↓
Post-Processing
    ↓
Documents / Memory
```

## 💡 Best Practices

### ✅ DO

- **Use specific loader types** for better performance
- **Filter early** to reduce memory usage
- **Use `each()`** for very large datasets
- **Enable progress tracking** for long operations
- **Validate documents** before expensive operations
- **Use `toMemory()`** for one-shot ingestion

### ❌ DON'T

- **Don't load entire directories** without filters
- **Don't skip error handling** in production
- **Don't ignore token limits** for LLMs
- **Don't forget to chunk** large documents
- **Don't reuse loaders** without calling `reset()`

## 🐛 Error Handling

```javascript
loader = aiDocuments( "/documents" )
    .configure( { continueOnError: true } )
    .load()

// Check for errors after loading
errors = loader.getErrors()
if ( errors.len() > 0 ) {
    for ( var error in errors ) {
        println( "Error: #error.message#" );
    }
}
```

## 📈 Performance Tips

1. **Use batch loading** for large datasets
2. **Enable filters early** in the pipeline
3. **Use `loadAsStream()`** for Java stream operations
4. **Chunk documents** before storing in vector memory
5. **Enable deduplication** to avoid storing duplicates
6. **Use async processing** for multi-memory fan-out

## 🔗 See Also

- [Memory Systems](../memory.md) - Where to store loaded documents
- [RAG Workflows](../rag.md) - Building retrieval-augmented generation
- [Chunking Strategies](../chunking.md) - How to split documents
- [Vector Memory](../vector-memory.md) - Semantic search with embeddings

## 📝 Quick Reference Cheat Sheet

```javascript
// Basic loading
docs = aiDocuments( "/file.txt" ).load()

// With config
docs = aiDocuments( "/file.txt", { encoding: "UTF-16" } ).load()

// Directory with filters
docs = aiDocuments( "/docs", { type: "directory" } )
    .recursive()
    .extensions( ["md", "txt"] )
    .filter( ( doc ) => doc.getContentLength() > 100 )
    .load()

// Direct to memory with chunking
result = aiDocuments( "/kb" )
    .toMemory( vectorMemory, { 
        chunkSize: 500, 
        overlap: 50, 
        dedupe: true 
    } )

// Progress tracking
aiDocuments( "/data" )
    .onProgress( ( n, total, doc ) => println( "#n#/#total#" ) )
    .load()

// Memory-efficient
aiDocuments( "/huge" ).each( ( doc ) => process( doc ) )

// Batch processing
loader = aiDocuments( "/batch" )
while ( (batch = loader.loadBatch( 100 )).len() > 0 ) {
    memory.seed( batch )
}

// Reset and reuse
loader.reset().setSource( "/new-source" ).load()

// Document operations
doc.preview( 200 )                     // First 200 chars
doc.chunk( 500, 100 )                  // Split with overlap
doc.validate( minLength: 50 )          // Validate
doc.hash()                            // Get hash
doc.equals( other )                    // Compare
doc.merge( other )                     // Combine
```

## 🎓 Learn More

Explore individual loader documentation:
- [Getting Started Guide](../getting-started.md)
- [Advanced Patterns](../advanced-patterns.md)
- [API Reference](../api-reference.md)
