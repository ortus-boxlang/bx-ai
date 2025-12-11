# 📚 Document Loaders

Document loaders are a powerful feature for importing content from various sources (files, directories, URLs, databases) into a standardized `Document` format that can be processed by AI workflows, stored in vector databases, or used for retrieval-augmented generation (RAG).

## 📖 Table of Contents

- [Overview](#overview)
- [BIF Reference](#bif-reference)
- [Document Structure](#document-structure)
- [Available Loaders](#available-loaders)
  - [TextLoader](#textloader)
  - [MarkdownLoader](#markdownloader)
  - [CSVLoader](#csvloader)
  - [JSONLoader](#jsonloader)
  - [XMLLoader](#xmlloader)
  - [PDFLoader](#pdfloader)
  - [LogLoader](#logloader)
  - [HTTPLoader](#httploader)
  - [FeedLoader](#feedloader)
  - [SQLLoader](#sqlloader)
  - [DirectoryLoader](#directoryloader)
  - [WebCrawlerLoader](#webcrawlerloader)
- [Memory Integration](#memory-integration)
- [Chunking](#chunking)
- [Transformations](#transformations)
- [Advanced Usage](#advanced-usage)

## Overview

The document loading system provides:

- **Multiple Loader Types**: Text, Markdown, CSV, JSON, XML, PDF, Log, HTTP, Feed, SQL, Directory, and WebCrawler loaders
- **Consistent Document Format**: All loaders produce `Document` objects with content, metadata, id, and embedding properties
- **Fluent API**: Chain methods for easy configuration and transformation
- **Memory Integration**: Load directly into AI memory systems for RAG workflows via `toMemory()`
- **Chunking Support**: Automatic text chunking for large documents
- **Multi-Memory Fan-out**: Ingest to multiple memory systems simultaneously
- **Async Support**: Load documents asynchronously with `loadAsync()`
- **Filter/Transform**: Apply filters and transforms during loading

## BIF Reference

| BIF | Purpose | Returns |
|-----|---------|---------|
| `aiDocuments()` | Create fluent document loader | IDocumentLoader |

## Quick Start

### Using `aiDocuments()`

The main entry point for document loading - returns a fluent loader:

```javascriptscript
// Load a text file
docs = aiDocuments( "/path/to/document.txt" ).load()

// Load a directory of files
docs = aiDocuments( "/path/to/folder" ).load()

// Load from URL
docs = aiDocuments( "https://example.com/page.html" ).load()

// Load with explicit type via config
docs = aiDocuments( "/path/to/file.txt", { type: "markdown" } ).load()

// Load with chunking configuration
docs = aiDocuments( "/path/to/file.md" )
    .chunkSize( 500 )
    .overlap( 50 )
    .load()
```

### Fluent Configuration

The `aiDocuments()` BIF returns a loader that can be fluently configured:

```javascriptscript
// Create and configure a markdown loader
docs = aiDocuments( "/docs", { type: "markdown" } )
    .recursive()
    .chunkSize( 500 )
    .load()

// Create a directory loader with filters
docs = aiDocuments( "/knowledge-base" )
    .recursive()
    .extensions( [ "md", "txt" ] )
    .load()

// Create an HTTP loader for web content
docs = aiDocuments( "https://api.example.com/data", { type: "http" } )
    .timeout( 60 )
    .header( "Authorization", "Bearer token" )
    .load()

// Load async
future = aiDocuments( "/large-dataset" ).loadAsync()
docs = future.get()
```

### Using `aiMemoryIngest()`

### Memory Integration with `toMemory()`

Ingest documents into memory with comprehensive reporting:

```javascriptscript
// Single memory ingestion
result = aiDocuments( "/docs", { type: "markdown" } )
    .toMemory( myVectorMemory )

// With chunking options
result = aiDocuments( "/knowledge-base" )
    .recursive()
    .extensions( [ "md", "txt" ] )
    .toMemory( myVectorMemory, { chunkSize: 500, overlap: 50 } )

// Multi-memory fan-out (async supported)
result = aiDocuments( "/docs", { type: "markdown" } )
    .toMemory( [ chromaMemory, pgVectorMemory ], { async: true } )
```

**Ingestion Report Structure:**

```javascriptscript
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

### Filter and Transform

Apply filters and transforms during loading:

```javascriptscript
// Filter documents
docs = aiDocuments( "/docs" )
    .filter( ( doc ) => doc.hasContent() && doc.getContentLength() > 100 )
    .load()

// Transform documents
docs = aiDocuments( "/docs" )
    .map( ( doc ) => {
        doc.setMeta( "processed", true )
        return doc
    } )
    .load()

// Combined with progress tracking
aiDocuments( "/large-dataset" )
    .filter( ( doc ) => doc.hasContent() )
    .map( ( doc ) => doc.setMeta( "indexed", true ) )
    .onProgress( ( completed, total, doc ) => {
        println( "Loading: #completed#/#total# - #doc.getMeta( 'source' )#" )
    } )
    .each( ( doc ) => {
        myMemory.seed( [ doc ] )
    } )
```

### Document Structure

Each `Document` object has:

```javascriptscript
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

### Document Methods

The `Document` class provides utility methods:

```javascriptscript
import bxModules.bxai.models.loaders.Document;

doc = new Document( content: "Sample text", metadata: { source: "test" } )

// Content methods
doc.getContent()                    // Get text content
doc.getContentLength()              // Get content length
doc.hasContent()                    // Check if has content
doc.preview( 100 )                  // Get truncated preview

// Token estimation
doc.getTokenCount()                 // Estimate token count (~4 chars/token)
doc.exceedsTokenLimit( 4000 )       // Check if exceeds limit

// Validation
result = doc.validate( minLength: 10, requiredMetadata: [ "source" ] )
// Returns { valid: boolean, errors: array }

// Deduplication
doc.hash()                          // MD5 hash of content
doc.fingerprint()                   // Combined content+metadata hash
doc.equals( otherDoc )              // Compare by hash

// Chunking
chunks = doc.chunk( 500, 50 )       // Returns array of Document chunks
```

## Available Loaders

### TextLoader

Loads plain text files (`.txt`, `.text`).

```javascriptscript
import bxModules.bxai.models.loaders.TextLoader;

loader = new TextLoader( source: "/path/to/file.txt" )
docs = loader.load()
```

### MarkdownLoader

Loads Markdown files with optional header-based splitting.

```javascript
import bxModules.bxai.models.loaders.MarkdownLoader;

// Basic loading
loader = new MarkdownLoader( source: "/path/to/readme.md" )
docs = loader.load()

// Split by headers
loader = new MarkdownLoader( source: "/path/to/readme.md" )
    .headerSplit( 2 )  // Split at h2 headers
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

```javascript
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

```javascript
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

```javascript
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

### PDFLoader

Loads PDF documents with text extraction and metadata support using Apache PDFBox.

```javascript
import bxModules.bxai.models.loaders.PDFLoader;

// Basic PDF loading
loader = new PDFLoader( source: "/path/to/document.pdf" )
docs = loader.load()

// Extract specific page range
loader = new PDFLoader( source: "/path/to/report.pdf" )
    .pageRange( startPage: 5, endPage: 10 )
docs = loader.load()

// Enhanced text extraction options
loader = new PDFLoader( source: "/path/to/document.pdf" )
    .sortByPosition( true )
    .addMoreFormatting( true )
    .suppressDuplicates( true )
docs = loader.load()

// Load with metadata extraction
loader = new PDFLoader( source: "/path/to/document.pdf" )
    .includeMetadata( true )
docs = loader.load()

// Access PDF metadata
println( "Title: #docs[1].metadata.title#" )
println( "Author: #docs[1].metadata.author#" )
println( "Pages: #docs[1].metadata.pageCount#" )
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `sortByPosition` | boolean | false | Sort text by position on page |
| `addMoreFormatting` | boolean | false | Add additional formatting |
| `startPage` | numeric | 1 | First page to extract |
| `endPage` | numeric | 0 | Last page to extract (0 = all) |
| `suppressDuplicateOverlappingText` | boolean | true | Remove duplicate overlapping text |
| `includeMetadata` | boolean | true | Extract PDF metadata |

**Metadata Fields Extracted:**
- `title` - Document title
- `author` - Document author
- `subject` - Document subject
- `keywords` - Document keywords
- `creator` - Application that created the PDF
- `producer` - PDF producer software
- `creationDate` - When the PDF was created
- `pageCount` - Total number of pages
- `pdfVersion` - PDF version (e.g., "1.7")
- `isEncrypted` - Whether the PDF is encrypted

### LogLoader

Loads and parses application log files with pattern matching and filtering.

```javascript
import bxModules.bxai.models.loaders.LogLoader;

// Basic log loading
loader = new LogLoader( source: "/path/to/app.log" )
docs = loader.load()

// Load with log level filtering
loader = new LogLoader( source: "/path/to/app.log" )
    .filterByLevel( "ERROR" )
docs = loader.load()

// Load with date range
loader = new LogLoader( source: "/path/to/app.log" )
    .startDate( "2024-01-01" )
    .endDate( "2024-12-31" )
docs = loader.load()

// Custom log pattern matching
loader = new LogLoader( source: "/path/to/custom.log" )
    .pattern( "^\[(?<timestamp>.*?)\] (?<level>\w+): (?<message>.*)" )
docs = loader.load()

// Combine multiple filters
loader = new LogLoader( source: "/path/to/app.log" )
    .filterByLevel( ["ERROR", "WARN"] )
    .excludePattern( "HealthCheck" )
    .maxLines( 10000 )
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `pattern` | string | Auto-detect | Regex pattern to parse log entries |
| `filterByLevel` | string/array | "" | Log level(s) to include |
| `excludePattern` | string | "" | Regex pattern to exclude entries |
| `startDate` | string | "" | Include logs after this date |
| `endDate` | string | "" | Include logs before this date |
| `maxLines` | numeric | 0 | Max lines to load (0 = unlimited) |
| `includeTimestamp` | boolean | true | Include timestamp in metadata |

**Supported Log Formats:**
- Standard format: `[2024-01-01 10:00:00] ERROR: Message`
- Syslog format: `Jan 1 10:00:00 hostname app: Message`
- Custom regex patterns via `pattern()` configuration

### DirectoryLoader

Loads all files from a directory using appropriate loaders.

```javascript
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

### XMLLoader

Loads and parses XML documents with XPath support. Useful for config files, RSS feeds, and legacy data.

```javascript
import bxModules.bxai.models.loaders.XMLLoader;

// Basic XML loading
loader = new XMLLoader( source: "/path/to/data.xml" )
docs = loader.load()

// Extract specific elements as documents
loader = new XMLLoader( source: "/path/to/catalog.xml" )
    .elementPath( "//product" )
    .elementsAsDocuments()
    .contentElements( ["description", "details"] )
    .metadataElements( ["name", "sku", "price"] )
docs = loader.load()

// With namespace support
loader = new XMLLoader( source: "/path/to/soap-response.xml" )
    .namespaceAware()
    .namespaces( { "soap": "http://schemas.xmlsoap.org/soap/envelope/" } )
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `elementPath` | string | "" | XPath to extract elements from |
| `elementsAsDocuments` | boolean | false | Create document per matching element |
| `contentElements` | array | [] | XPath expressions for content |
| `metadataElements` | array | [] | XPath expressions for metadata |
| `preserveWhitespace` | boolean | false | Preserve whitespace in text |
| `includeAttributes` | boolean | true | Include attribute values |
| `namespaceAware` | boolean | true | Handle XML namespaces |
| `namespaces` | struct | {} | Namespace prefix mappings |

### FeedLoader

Loads RSS and Atom feeds, creating a document per feed item. Perfect for blog aggregation, news feeds, and content syndication.

```javascript
import bxModules.bxai.models.loaders.FeedLoader;

// Load RSS feed
loader = new FeedLoader( source: "https://example.com/feed.rss" )
docs = loader.load()

// Load with filtering
loader = new FeedLoader( source: "https://news.example.com/rss" )
    .maxItems( 10 )
    .sinceDate( dateAdd( "d", -7, now() ) )  // Last 7 days
    .categories( ["technology", "ai"] )
    .stripHtml()
docs = loader.load()

// Local feed file
loader = new FeedLoader( source: "/path/to/feed.xml" )
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `includeDescription` | boolean | true | Include item description |
| `includeContent` | boolean | true | Include full content |
| `stripHtml` | boolean | true | Strip HTML tags from content |
| `maxItems` | numeric | 0 | Maximum items to load (0 = all) |
| `sinceDate` | date | "" | Only load items since date |
| `categories` | array | [] | Filter by categories |
| `timeout` | numeric | 30 | HTTP timeout for URL feeds |

### SQLLoader

Loads documents from database queries. Converts query results to Document objects for RAG over structured data.

```javascript
import bxModules.bxai.models.loaders.SQLLoader;

// Basic query loading
loader = new SQLLoader( source: "SELECT * FROM articles" )
    .datasource( "mydb" )
    .contentColumn( "body" )
docs = loader.load()

// Multiple columns as content with template
loader = new SQLLoader( source: "SELECT * FROM products" )
    .datasource( "ecommerce" )
    .contentTemplate( "${name}: ${description}. Price: ${price}" )
    .metadataColumns( ["sku", "category", "created_at"] )
    .idColumn( "sku" )
docs = loader.load()

// Parameterized query
loader = new SQLLoader( source: "SELECT * FROM docs WHERE status = ?" )
    .datasource( "mydb" )
    .params( { 1: "published" } )
    .contentColumn( "content" )
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `datasource` | string | "" | Datasource name to use |
| `contentColumn` | string | "" | Column to use as document content |
| `contentColumns` | array | [] | Array of columns to combine as content |
| `contentTemplate` | string | "" | Template with `${column}` placeholders |
| `metadataColumns` | array | [] | Columns to extract as metadata |
| `idColumn` | string | "" | Column to use as document ID |
| `params` | struct | {} | Query parameters |
| `maxRows` | numeric | 0 | Maximum rows to return (0 = all) |
| `rowsAsDocuments` | boolean | true | Create document per row |

### WebCrawlerLoader

Crawls multiple web pages by following links. Respects robots.txt and supports depth-limited crawling. Uses JSoup for HTML parsing.

```javascript
import bxModules.bxai.models.loaders.WebCrawlerLoader;

// Basic crawling
loader = new WebCrawlerLoader( source: "https://example.com" )
docs = loader.load()

// Advanced crawling configuration
loader = new WebCrawlerLoader( source: "https://docs.example.com" )
    .maxPages( 50 )
    .maxDepth( 3 )
    .allowedPaths( ["/docs/", "/guides/"] )
    .excludedPaths( ["/api/", "/admin/"] )
    .contentSelector( "article.content" )
    .excludeSelectors( ["nav", "footer", ".sidebar"] )
    .delay( 2000 )  // 2 seconds between requests
docs = loader.load()

// Cross-domain crawling
loader = new WebCrawlerLoader( source: "https://main.example.com" )
    .followExternalLinks()
    .allowedDomains( ["docs.example.com", "blog.example.com"] )
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `maxPages` | numeric | 10 | Maximum pages to crawl |
| `maxDepth` | numeric | 2 | Maximum link depth |
| `followExternalLinks` | boolean | false | Follow links to other domains |
| `allowedDomains` | array | [] | Domains allowed for external links |
| `allowedPaths` | array | [] | Path prefixes to allow |
| `excludedPaths` | array | [] | Path prefixes to exclude |
| `urlPatterns` | array | [] | URL regex patterns to match |
| `excludeUrlPatterns` | array | [] | URL regex patterns to exclude |
| `respectRobotsTxt` | boolean | true | Respect robots.txt rules |
| `contentSelector` | string | "" | CSS selector for content extraction |
| `excludeSelectors` | array | [] | CSS selectors to exclude from content |
| `delay` | numeric | 1000 | Delay between requests in ms |
| `userAgent` | string | "BoxLang-WebCrawler/1.0" | User agent string |
| `deduplicateContent` | boolean | true | Skip pages with duplicate content |

## Loading Methods

All loaders support these loading methods:

| Method | Description |
|--------|-------------|
| `load()` | Load all documents synchronously |
| `loadAsync()` | Load all documents asynchronously (returns BoxFuture) |
| `loadAsStream()` | Load as Java Stream for lazy processing |
| `loadBatch( batchSize )` | Load documents in batches |

```javascript
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

```javascript
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

```javascript
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

```javascript
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

```javascript
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

```javascript
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

```javascript
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
