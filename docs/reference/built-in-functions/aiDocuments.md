# aiDocuments

Main entry point for document loading in AI workflows. Returns a fluent document loader for flexible configuration and execution.

## Syntax

```javascript
aiDocuments(source, config)
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `source` | string | Yes | Source to load from: file path, directory, URL, or SQL query |
| `config` | struct | No | Configuration options for the loader |

### Config Options

| Option | Type | Description |
|--------|------|-------------|
| `type` | string | Explicit loader type (auto-detected if omitted) |
| `recursive` | boolean | Recurse into subdirectories (directory loader) |
| `extensions` | array | File extensions to include: `["md", "txt"]` |
| `chunkSize` | numeric | Chunk size for splitting documents |
| `overlap` | numeric | Overlap between chunks |
| `delimiter` | string | CSV delimiter character |
| `encoding` | string | File encoding (default: UTF-8) |

## Returns

Returns an `IDocumentLoader` instance with fluent API for chaining configuration and execution.

## Supported Loader Types

| Type | Auto-Detected From | Description |
|------|------|-------------|
| `text` | `.txt` extension | Plain text files |
| `markdown` | `.md` extension | Markdown documents |
| `csv` | `.csv` extension | CSV data files |
| `json` | `.json` extension | JSON documents |
| `xml` | `.xml` extension | XML documents |
| `directory` | `directoryExists()` | Folder scanning |
| `http` | `http://` or `https://` | Web page loading |
| `feed` | `.rss`, `.atom`, `/feed` URLs | RSS/Atom feeds |
| `sql` | Starts with `SELECT`, `WITH` | Database queries |
| `crawler` | Explicit type needed | Website crawling |

## Fluent API Methods

### Configuration Methods
- `.recursive(boolean)` - Enable/disable recursive directory scanning
- `.extensions(array)` - Filter by file extensions
- `.chunkSize(numeric)` - Set chunk size
- `.overlap(numeric)` - Set chunk overlap
- `.filter(function)` - Filter documents with callback
- `.transform(function)` - Transform documents with callback
- `.onProgress(function)` - Progress callback: `(completed, total, doc) => {}`

### Execution Methods
- `.load()` - Load and return array of documents
- `.loadAsync()` - Load asynchronously, return Future
- `.toMemory(memory, options)` - Load and ingest into memory
- `.each(function)` - Stream process each document: `(doc) => {}`

## Examples

### Simple File Loading

```javascript
// Load a single markdown file
docs = aiDocuments( "/docs/readme.md" ).load();
println( "Loaded #docs.len()# documents" );
```

### Directory Loading

```javascript
// Load all markdown files from directory
docs = aiDocuments( "/docs", { 
    type: "directory",
    extensions: ["md"]
} ).load();

// Or use fluent API
docs = aiDocuments( "/docs" )
    .recursive()
    .extensions( ["md", "txt"] )
    .load();
```

### With Chunking

```javascript
// Load and chunk documents
docs = aiDocuments( "/knowledge-base" )
    .recursive()
    .chunkSize( 1000 )
    .overlap( 200 )
    .load();

println( "Created #docs.len()# chunks" );
```

### Filtering

```javascript
// Load only English documents
docs = aiDocuments( "/multilingual-docs" )
    .recursive()
    .filter( ( doc ) => doc.metadata.language == "en" )
    .load();
```

### Progress Tracking

```javascript
// Track loading progress
docs = aiDocuments( "/large-dataset" )
    .onProgress( ( completed, total, doc ) => {
        println( "Loading: #completed#/#total# - #doc.metadata.source#" );
    } )
    .load();
```

### Direct to Memory (RAG)

```javascript
// Create vector memory
vectorMemory = aiMemory( "chroma", {
    collection: "knowledge_base"
} );

// Load and ingest in one operation
result = aiDocuments( "/docs" )
    .recursive()
    .extensions( ["md", "txt", "pdf"] )
    .toMemory(
        memory: vectorMemory,
        options: {
            chunkSize: 1000,
            overlap: 200,
            trackTokens: true
        }
    );

println( "Ingested #result.documentsIn# docs as #result.chunksOut# chunks" );
println( "Estimated cost: $#result.estimatedCost#" );
```

### Stream Processing

```javascript
// Process large dataset without loading all into memory
aiDocuments( "/huge-dataset" )
    .each( ( doc ) => {
        // Process one document at a time
        analyzedDoc = analyzeDocument( doc );
        saveToDatabase( analyzedDoc );
    } );
```

### Loading from Web

```javascript
// Load single web page
docs = aiDocuments( "https://example.com/article.html" ).load();

// Load RSS feed
posts = aiDocuments( "https://blog.example.com/feed.xml" ).load();
```

### Website Crawling

```javascript
// Crawl website
docs = aiDocuments( "https://example.com", {
    type: "crawler",
    maxPages: 50,
    maxDepth: 3
} ).load();
```

### Database Loading

```javascript
// Load from database
docs = aiDocuments( 
    "SELECT title, content, created_date FROM articles WHERE published = true",
    {
        type: "sql",
        datasource: "mydb"
    }
).load();
```

### CSV Loading

```javascript
// Load CSV with custom delimiter
docs = aiDocuments( "/data/products.csv", {
    delimiter: ";",
    hasHeaders: true
} ).load();
```

### Transformation

```javascript
// Transform documents during loading
docs = aiDocuments( "/docs" )
    .transform( ( doc ) => {
        // Add custom metadata
        doc.metadata.processed = now();
        doc.metadata.category = categorize( doc.content );
        return doc;
    } )
    .load();
```

### Async Loading

```javascript
// Load asynchronously
future = aiDocuments( "/large-docs" )
    .recursive()
    .loadAsync();

// Do other work...
doSomethingElse();

// Get results when ready
docs = future.get();
```

### Multiple Sources

```javascript
// Combine documents from multiple sources
pdfDocs = aiDocuments( "/pdfs", { type: "directory", extensions: ["pdf"] } ).load();
webDocs = aiDocuments( "https://example.com/docs" ).load();
sqlDocs = aiDocuments( "SELECT * FROM articles", { type: "sql" } ).load();

allDocs = pdfDocs.append( webDocs ).append( sqlDocs );
```

## toMemory() Report Structure

When using `.toMemory()`, a report struct is returned:

```javascript
{
    documentsIn: 10,           // Number of documents loaded
    chunksOut: 45,             // Number of chunks created
    stored: 45,                // Number of chunks stored
    skipped: 0,                // Number of chunks skipped
    deduped: 0,                // Number of duplicates removed
    tokenCount: 12500,         // Total tokens (if trackTokens: true)
    embeddingCalls: 45,        // Number of embedding API calls
    estimatedCost: 0.0025,     // Estimated cost (if trackCost: true)
    errors: [],                // Array of errors
    startTime: [date],         // Start timestamp
    endTime: [date],           // End timestamp
    duration: 1234             // Duration in milliseconds
}
```

## Document Structure

Each loaded document has:

```javascript
{
    content: "Document text content...",
    metadata: {
        source: "/path/to/file.md",
        type: "markdown",
        size: 1234,
        created: [date],
        modified: [date],
        // Loader-specific metadata
    }
}
```

## Notes

- **Auto-detection**: Loader type automatically detected from source (file extension, URL pattern, SQL syntax)
- **Fluent API**: Chain multiple configuration calls before execution
- **Lazy loading**: No processing until `.load()`, `.loadAsync()`, `.toMemory()`, or `.each()` called
- **Memory efficient**: `.each()` streams documents without loading all into memory
- **Chunking**: Automatic text chunking for RAG workflows
- **Progress tracking**: Built-in progress callbacks for long operations
- **Error handling**: Continues on error by default (configurable with `continueOnError`)

## Related Functions

- [`aiChunk()`](./aiChunk.md) - Manual text chunking
- [`aiMemory()`](./aiMemory.md) - Create memory instances
- [`aiEmbed()`](./aiEmbed.md) - Generate embeddings

## Best Practices

```javascript
// ✅ Use fluent API for readability
docs = aiDocuments( "/docs" )
    .recursive()
    .extensions( ["md"] )
    .chunkSize( 1000 )
    .load();

// ✅ Track progress for large datasets
aiDocuments( "/huge-dataset" )
    .onProgress( (c, t) => println("#c#/#t#") )
    .load();

// ✅ Use .each() for memory efficiency
aiDocuments( "/massive-files" )
    .each( doc => process(doc) );

// ✅ Filter early to reduce processing
docs = aiDocuments( "/all-docs" )
    .filter( d => d.metadata.date > cutoffDate )
    .load();

// ❌ Don't load huge datasets into memory
docs = aiDocuments( "/10GB-dataset" ).load(); // Bad!

// ✅ Use streaming or direct to memory instead
aiDocuments( "/10GB-dataset" )
    .toMemory( vectorMemory ); // Better!
```
