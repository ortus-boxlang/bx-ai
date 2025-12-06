# Document Loaders

Document loaders are a powerful feature for importing content from various sources (files, directories, URLs) into a standardized `Document` format that can be processed by AI workflows, stored in vector databases, or used for retrieval-augmented generation (RAG).

## Overview

The document loading system provides:

- **Multiple Loader Types**: Text, Markdown, CSV, JSON, HTTP, Tika, and Directory loaders
- **Consistent Document Format**: All loaders produce `Document` objects with content, metadata, id, and embedding properties
- **Fluent API**: Chain methods for easy configuration
- **Memory Integration**: Load directly into AI memory systems for RAG workflows
- **Chunking Support**: Automatic text chunking for large documents
- **Multi-Memory Fan-out**: Ingest to multiple memory systems simultaneously
- **Async Support**: Load documents asynchronously with `loadAsync()`

## BIF Reference

| BIF | Purpose | Returns |
|-----|---------|---------|
| `aiDocuments()` | Load documents from any source | Array of Documents |
| `aiDocumentLoader()` | Create loader instance for advanced config | IDocumentLoader |
| `aiDocumentLoaders()` | Get all registered loaders with metadata | Struct |
| `aiMemoryIngest()` | Ingest documents into memory with reporting | Ingestion Report |

## Quick Start

### Using `aiDocuments()`

The easiest way to load documents:

```java
// Load a text file
docs = aiDocuments( "/path/to/document.txt" )

// Load a directory of files
docs = aiDocuments( "/path/to/folder" )

// Load from URL
docs = aiDocuments( "https://example.com/page.html" )

// Load with explicit type
docs = aiDocuments( source: "/path/to/file.txt", type: "markdown" )

// Load with configuration
docs = aiDocuments(
    source: "/path/to/file.csv",
    config: { delimiter: ";", rowsAsDocuments: true }
)
```

### Using `aiDocumentLoader()`

For advanced configuration and method chaining:

```java
// Create and configure a markdown loader
loader = aiDocumentLoader( "/docs", "markdown" )
    .splitByHeaders( 2 )
    .removeCodeBlocks()
docs = loader.load()

// Create a directory loader with filters
loader = aiDocumentLoader( "/knowledge-base", "directory" )
    .recursive()
    .extensions( ["md", "txt"] )
docs = loader.load()

// Create an HTTP loader for web content
loader = aiDocumentLoader( "https://api.example.com/data", "http" )
    .contentType( "json" )
    .timeout( 60 )
    .header( "Authorization", "Bearer token" )
docs = loader.load()

// Load async
future = loader.loadAsync()
docs = future.get()
```

### Using `aiMemoryIngest()`

Ingest documents into memory with comprehensive reporting:

```java
// Single memory ingestion
result = aiMemoryIngest(
    memory = myVectorMemory,
    source = "/docs",
    type   = "markdown"
)

// With chunking and options
result = aiMemoryIngest(
    memory        = myVectorMemory,
    source        = "/knowledge-base",
    type          = "directory",
    loaderConfig  = { recursive: true, extensions: ["md", "txt"] },
    ingestOptions = { chunkSize: 500, overlap: 50 }
)

// Multi-memory fan-out (async supported)
result = aiMemoryIngest(
    memory = [ chromaMemory, pgVectorMemory ],
    source = "/docs",
    type   = "markdown",
    ingestOptions = { async: true }
)
```

**Ingestion Report Structure:**

```java
{
    documentsIn       : 12,      // Documents loaded from source
    chunksOut         : 57,      // Chunks after splitting
    stored            : 57,      // Successfully stored
    skipped           : 3,       // Skipped (errors)
    deduped           : 2,       // Deduplicated
    tokenCount        : 12345,   // Total tokens
    embeddingCalls    : 57,      // Embedding API calls
    estimatedCost     : 0.0042,  // Estimated cost (USD)
    errors            : [...],   // Error details
    memorySummary     : {...},   // Memory summary (or array for multi-memory)
    duration          : 5        // Duration in seconds
}
```

### Using `aiDocumentLoaders()`

Get information about all registered loaders:

```java
loaders = aiDocumentLoaders()

// Inspect available loaders
loaders.each( ( type, info ) => {
    println( "Type: #type#" )
    println( "  Extensions: #info.extensions.toList()#" )
    println( "  Capabilities: #info.capabilities.keyArray().toList()#" )
} )
```

### Document Structure

Each `Document` object has:

```java
{
    "id": "auto-generated-uuid",
    "content": "The extracted text content",
    "embedding": [],  // Empty array until embeddings are generated
    "metadata": {
        "source": "/path/to/file",
        "loader": "TextLoader",
        "loadedAt": "2024-01-01T12:00:00Z",
        "fileType": "text",
        "fileName": "file.txt",
        "fileSize": 1234,
        // ... additional loader-specific metadata
    }
}
```

## Available Loaders

### TextLoader

Loads plain text files (`.txt`, `.text`).

```java
import bxModules.bxai.models.loaders.TextLoader;

loader = new TextLoader( source: "/path/to/file.txt" )
docs = loader.load()
```

### MarkdownLoader

Loads Markdown files with optional header-based splitting.

```java
import bxModules.bxai.models.loaders.MarkdownLoader;

// Basic loading
loader = new MarkdownLoader( source: "/path/to/readme.md" )
docs = loader.load()

// Split by headers
loader = new MarkdownLoader( source: "/path/to/readme.md" )
    .splitByHeaders( 2 )  // Split at h2 headers
docs = loader.load()

// Remove code blocks and images
loader = new MarkdownLoader( source: "/path/to/readme.md" )
    .removeCodeBlocks()
    .removeImages()
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `splitByHeaders` | boolean | false | Split document by headers |
| `headerLevel` | numeric | 2 | Header level to split at (1-6) |
| `removeCodeBlocks` | boolean | false | Remove fenced code blocks |
| `removeImages` | boolean | false | Remove image references |
| `removeLinks` | boolean | false | Remove links (keeps text) |

### HTTPLoader

Loads content from HTTP/HTTPS URLs with automatic content type detection. This is the primary loader for all web-based content including HTML pages, JSON APIs, and XML feeds.

```java
import bxModules.bxai.models.loaders.HTTPLoader;

// Basic URL loading
loader = new HTTPLoader( source: "https://example.com/page.html" )
docs = loader.load()

// JSON API endpoint
loader = new HTTPLoader( source: "https://api.example.com/data" )
    .contentType( "json" )
    .header( "Authorization", "Bearer token" )
docs = loader.load()

// POST request with body
loader = new HTTPLoader( source: "https://api.example.com/submit" )
    .post()
    .body( { query: "search term" } )
    .timeout( 60 )
docs = loader.load()

// With proxy
loader = new HTTPLoader( source: "https://example.com" )
    .proxy( "proxy.example.com", 8080, "user", "password" )
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `contentType` | string | "auto" | Content type (auto, text, html, json, xml) |
| `method` | string | "GET" | HTTP method |
| `headers` | struct | {} | Request headers |
| `body` | string | "" | Request body |
| `timeout` | numeric | 30 | Request timeout in seconds |
| `connectionTimeout` | numeric | 30 | Connection timeout in seconds |
| `redirect` | boolean | true | Follow redirects |
| `extractText` | boolean | true | Extract text from HTML |
| `removeScripts` | boolean | true | Remove script tags from HTML |
| `removeStyles` | boolean | true | Remove style tags from HTML |

**Fluent HTTP Methods:**

- `.get()` - Set GET method
- `.post()` - Set POST method
- `.put()` - Set PUT method
- `.delete()` - Set DELETE method
- `.method( "PATCH" )` - Set custom method
- `.header( name, value )` - Add single header
- `.headers( { name: value } )` - Add multiple headers
- `.body( content )` - Set request body
- `.timeout( seconds )` - Set request timeout
- `.connectionTimeout( seconds )` - Set connection timeout
- `.redirect( true/false )` - Enable/disable redirects
- `.proxy( server, port, user?, password? )` - Configure proxy

### CSVLoader

Loads CSV files with header support and row-as-document options.

```java
import bxModules.bxai.models.loaders.CSVLoader;

// Basic loading
loader = new CSVLoader( source: "/path/to/data.csv" )
docs = loader.load()

// Create a document per row
loader = new CSVLoader( source: "/path/to/data.csv" )
    .rowsAsDocuments()
docs = loader.load()

// Custom delimiter and columns
loader = new CSVLoader( source: "/path/to/data.csv" )
    .delimiter( ";" )
    .columns( ["name", "description"] )
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `delimiter` | string | "," | Column delimiter |
| `hasHeaders` | boolean | true | First row contains headers |
| `rowsAsDocuments` | boolean | false | Create document per row |
| `columns` | array | [] | Columns to include |
| `skipRows` | numeric | 0 | Rows to skip at start |

### JSONLoader

Loads JSON files with field extraction options.

```java
import bxModules.bxai.models.loaders.JSONLoader;

// Basic loading
loader = new JSONLoader( source: "/path/to/data.json" )
docs = loader.load()

// Extract specific content field
loader = new JSONLoader( source: "/path/to/data.json" )
    .contentField( "content" )
    .metadataFields( ["title", "author", "date"] )
docs = loader.load()

// Create document per array item
loader = new JSONLoader( source: "/path/to/array.json" )
    .arrayAsDocuments()
    .contentField( "text" )
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `contentField` | string | "" | Field to use as content |
| `metadataFields` | array | [] | Fields to extract as metadata |
| `arrayAsDocuments` | boolean | false | Create document per array item |

### TikaLoader

Loads PDF, Word, Excel, PowerPoint, and other document formats using Apache Tika.

```java
import bxModules.bxai.models.loaders.TikaLoader;

// Load a PDF
loader = new TikaLoader( source: "/path/to/document.pdf" )
docs = loader.load()

// Load with options
loader = new TikaLoader( source: "/path/to/report.docx" )
    .maxLength( 100000 )
    .enableOCR( true )
docs = loader.load()

// Password-protected document
loader = new TikaLoader( source: "/path/to/encrypted.pdf" )
    .password( "secret" )
docs = loader.load()

// Check if file type is supported
if ( TikaLoader::isSupported( "/path/to/file.pdf" ) ) {
    // Load the file
}
```

**Supported Extensions:**
- PDF: `.pdf`
- Word: `.doc`, `.docx`
- Excel: `.xls`, `.xlsx`
- PowerPoint: `.ppt`, `.pptx`
- OpenDocument: `.odt`, `.ods`, `.odp`
- Other: `.rtf`, `.epub`

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `maxLength` | numeric | -1 | Max characters to extract (-1 = unlimited) |
| `extractImages` | boolean | false | Extract embedded images |
| `extractTables` | boolean | true | Preserve table structure |
| `ocrEnabled` | boolean | false | Enable OCR for scanned docs |
| `passwordProtected` | string | "" | Password for encrypted documents |

### DirectoryLoader

Loads all files from a directory using appropriate loaders.

```java
import bxModules.bxai.models.loaders.DirectoryLoader;

// Load all supported files
loader = new DirectoryLoader( source: "/path/to/folder" )
docs = loader.load()

// Recursive loading with filters
loader = new DirectoryLoader( source: "/path/to/folder" )
    .recursive()
    .extensions( ["md", "txt"] )
    .exclude( ["README.md", "CHANGELOG.*"] )
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `recursive` | boolean | false | Scan subdirectories |
| `extensions` | array | [] | File extensions to include |
| `excludePatterns` | array | [] | Regex patterns to exclude |
| `includeHidden` | boolean | false | Include hidden files |

## Loading Methods

All loaders support these loading methods:

| Method | Description |
|--------|-------------|
| `load()` | Load all documents synchronously |
| `loadAsync()` | Load all documents asynchronously (returns BoxFuture) |
| `loadAsStream()` | Load as Java Stream for lazy processing |
| `loadBatch( batchSize )` | Load documents in batches |

```java
// Synchronous loading
docs = loader.load()

// Asynchronous loading
future = loader.loadAsync()
docs = future.get()

// Stream processing
stream = loader.loadAsStream()
count = stream.filter( doc => doc.getContentLength() > 100 ).count()

// Batch loading
batch1 = loader.loadBatch( 50 )  // First 50 docs
batch2 = loader.loadBatch( 50 )  // Next 50 docs
```

## Loading to Memory

### Using `loadTo()` Method

Loaders can store documents directly into AI memory systems:

```java
import bxModules.bxai.models.loaders.DirectoryLoader;

// Load documents into windowed memory
loader = new DirectoryLoader( source: "/docs" )
memory = aiMemory( "windowed" )
docs = loader.loadTo( memory )

// Load with chunking for vector memory
vectorMemory = aiMemory( "chroma", {
    collection: "knowledge_base",
    embeddingProvider: "openai"
} )
docs = loader.loadTo(
    vectorMemory,
    { chunkSize: 500, overlap: 50 }
)
```

### Using `aiMemoryIngest()` BIF (Recommended)

For comprehensive ingestion with reporting:

```java
// Single memory with full reporting
result = aiMemoryIngest(
    memory        = vectorMemory,
    source        = "/knowledge-base",
    type          = "directory",
    loaderConfig  = { recursive: true, extensions: ["md", "txt"] },
    ingestOptions = {
        chunkSize       : 500,
        overlap         : 50,
        trackTokens     : true,
        trackCost       : true,
        continueOnError : true
    }
)

// Check results
println( "Loaded #result.documentsIn# documents as #result.chunksOut# chunks" )
println( "Stored: #result.stored#, Errors: #result.errors.len()#" )
println( "Estimated cost: $#result.estimatedCost#" )
```

**Ingestion Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `chunkSize` | numeric | 0 | Chunk size (0 = no chunking) |
| `overlap` | numeric | 0 | Overlap between chunks |
| `strategy` | string | "recursive" | Chunking strategy |
| `dedupe` | boolean | false | Enable deduplication |
| `dedupeThreshold` | numeric | 0.95 | Similarity threshold |
| `trackTokens` | boolean | true | Track token counts |
| `trackCost` | boolean | true | Estimate costs |
| `async` | boolean | false | Use async for multi-memory |
| `batchSize` | numeric | 100 | Batch size for processing |
| `continueOnError` | boolean | true | Continue on document errors |

## The Document Class

The `Document` class provides a consistent interface for working with loaded content:

```java
import bxModules.bxai.models.loaders.Document;

// Create a document
doc = new Document(
    id: "custom-id",          // Optional - auto-generates UUID if not provided
    content: "Hello world",
    metadata: { source: "test", author: "John" },
    embedding: []             // Optional - empty array by default
)

// Access content
println( doc.getContent() )           // "Hello world"
println( doc.hasContent() )           // true
println( doc.getContentLength() )     // 11

// Access id and embedding
println( doc.getId() )                // "custom-id" or auto-generated UUID
println( doc.hasEmbedding() )         // false (empty array)
doc.setEmbedding( [0.1, 0.2, 0.3] )   // Set embedding
println( doc.hasEmbedding() )         // true

// Access metadata
println( doc.getMeta( "author" ) )    // "John"
println( doc.getMetadata() )          // { source: "test", author: "John" }

// Modify document
doc.setContent( "Updated content" )
doc.setMeta( "updated", true )
doc.addMetadata( { version: 2 } )

// Serialize/deserialize (includes id and embedding)
json = doc.toJson()
restored = Document::fromJson( json )

// Clone document
copy = doc.clone()
```

## Error Handling

Loaders track errors encountered during loading:

```java
loader = new DirectoryLoader( source: "/docs" )
    .configure( { continueOnError: true } )

docs = loader.load()

// Check for errors
errors = loader.getErrors()
if ( errors.len() > 0 ) {
    errors.each( error => {
        println( "Error loading #error.source#: #error.message#" )
    } )
}
```

## Custom Loaders

You can create custom loaders by extending `BaseDocumentLoader`:

```java
class CustomLoader extends="bxModules.bxai.models.loaders.BaseDocumentLoader" {

    function init( string source = "", struct config = {} ) {
        super.init( argumentCollection: arguments )
        return this
    }

    string function getName() {
        return "CustomLoader"
    }

    array function load() {
        // Your loading logic here
        var content = loadMyContent( variables.source )

        return [
            createDocument(
                content: content,
                additionalMetadata: {
                    customField: "value"
                }
            )
        ]
    }
}
```

## RAG Pipeline Example

Here's a complete example of building a RAG pipeline with document loaders:

```java
// Step 1: Create vector memory
vectorMemory = aiMemory( "chroma", {
    collection: "docs",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small"
} )

// Step 2: Ingest documents with aiMemoryIngest
result = aiMemoryIngest(
    memory        = vectorMemory,
    source        = "/knowledge-base",
    type          = "directory",
    loaderConfig  = { recursive: true, extensions: ["md", "txt", "pdf"] },
    ingestOptions = { chunkSize: 1000, overlap: 200 }
)

println( "Loaded #result.documentsIn# docs as #result.chunksOut# chunks" )
println( "Estimated cost: $#result.estimatedCost#" )

// Step 3: Create an agent with memory
agent = aiAgent(
    name: "KnowledgeAssistant",
    description: "An assistant with access to the knowledge base",
    memory: vectorMemory
)

// Step 4: Query the agent
response = agent.run( "What is BoxLang?" )
println( response )
```

## Best Practices

1. **Use `aiMemoryIngest()` for Production**: It provides comprehensive reporting, error handling, and multi-memory support.

2. **Configure Chunking**: For vector memory, use appropriate chunk sizes (500-1000 chars) with overlap (100-200 chars).

3. **Use Metadata**: Leverage metadata for filtering and context in RAG queries.

4. **Handle Large Directories**: Use `recursive()` sparingly and filter by `extensions()` to avoid loading unnecessary files.

5. **Monitor Costs**: Use `trackCost: true` in ingestion options to estimate embedding costs before large ingestions.

6. **Multi-Memory for Redundancy**: Use array of memories for fan-out ingestion to multiple vector stores.

7. **Use Async for Large Loads**: Use `loadAsync()` or `ingestOptions.async` for non-blocking operations.

8. **Use HTTPLoader for Web Content**: The HTTPLoader handles all URL-based content including HTML pages, JSON APIs, and XML feeds.

## See Also

- [Memory Systems](memory.md) - Standard and vector memory types
- [aiChunk() BIF](../chatting/chunking.md) - Text chunking strategies
- [Agents](agents.md) - Using agents with loaded documents
