# Document Loaders

Document loaders are a powerful feature for importing content from various sources (files, directories, URLs) into a standardized `Document` format that can be processed by AI workflows, stored in vector databases, or used for retrieval-augmented generation (RAG).

## Overview

The document loading system provides:

- **Multiple Loader Types**: Text, Markdown, HTML, CSV, JSON, and Directory loaders
- **Consistent Document Format**: All loaders produce `Document` objects with content and metadata
- **Fluent API**: Chain methods for easy configuration
- **Memory Integration**: Load directly into AI memory systems for RAG workflows
- **Chunking Support**: Automatic text chunking for large documents

## Quick Start

### Using the `aiLoad()` BIF

The easiest way to load documents is with the `aiLoad()` function:

```java
// Load a text file
docs = aiLoad( "/path/to/document.txt" )

// Load a directory of files
docs = aiLoad( "/path/to/folder" )

// Load with explicit type
docs = aiLoad( source: "/path/to/file.txt", type: "markdown" )

// Load with configuration
docs = aiLoad(
    source: "/path/to/file.csv",
    config: { delimiter: ";", rowsAsDocuments: true }
)
```

### Document Structure

Each `Document` object has:

```java
{
    "content": "The extracted text content",
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

### HTMLLoader

Loads HTML files or URLs, extracting text content.

```java
import bxModules.bxai.models.loaders.HTMLLoader;

// Load from file
loader = new HTMLLoader( source: "/path/to/page.html" )
docs = loader.load()

// Load from URL
loader = new HTMLLoader( source: "https://example.com/page.html" )
docs = loader.load()

// Extract specific tags
loader = new HTMLLoader( source: "/path/to/page.html" )
    .extractTags( ["article", "main"] )
docs = loader.load()

// Preserve links in output
loader = new HTMLLoader( source: "/path/to/page.html" )
    .preserveLinks()
docs = loader.load()
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `extractTags` | array | [] | Extract content from specific tags |
| `removeScripts` | boolean | true | Remove script tags |
| `removeStyles` | boolean | true | Remove style tags |
| `removeComments` | boolean | true | Remove HTML comments |
| `preserveLinks` | boolean | false | Keep links as "text (url)" format |

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

## Loading to Memory

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

## The Document Class

The `Document` class provides a consistent interface for working with loaded content:

```java
import bxModules.bxai.models.loaders.Document;

// Create a document
doc = new Document(
    content: "Hello world",
    metadata: { source: "test", author: "John" }
)

// Access content
println( doc.getContent() )           // "Hello world"
println( doc.hasContent() )           // true
println( doc.getContentLength() )     // 11

// Access metadata
println( doc.getMeta( "author" ) )    // "John"
println( doc.getMetadata() )          // { source: "test", author: "John" }

// Modify document
doc.setContent( "Updated content" )
doc.setMeta( "updated", true )
doc.addMetadata( { version: 2 } )

// Serialize/deserialize
json = doc.toJson()
restored = Document::fromJson( json )

// Clone document
copy = doc.clone()
```

## Custom Loaders

You can create custom loaders by extending `BaseDocumentLoader`:

```java
class PDFLoader extends="bxModules.bxai.models.loaders.BaseDocumentLoader" {

    function init( string source = "", struct config = {} ) {
        super.init( argumentCollection: arguments )
        return this
    }

    string function getName() {
        return "PDFLoader"
    }

    array function load() {
        // Your PDF loading logic here
        var content = extractPDFText( variables.source )

        return [
            createDocument(
                content: content,
                additionalMetadata: {
                    fileType: "pdf",
                    // ... additional metadata
                }
            )
        ]
    }
}
```

## RAG Pipeline Example

Here's a complete example of building a RAG pipeline with document loaders:

```java
// Step 1: Load documents from a directory
loader = new DirectoryLoader( source: "/knowledge-base" )
    .recursive()
    .extensions( ["md", "txt", "html"] )

// Step 2: Create vector memory
vectorMemory = aiMemory( "chroma", {
    collection: "docs",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small"
} )

// Step 3: Load with chunking into vector memory
docs = loader.loadTo( vectorMemory, {
    chunkSize: 1000,
    overlap: 200,
    strategy: "recursive"
} )

println( "Loaded #docs.len()# document chunks into vector memory" )

// Step 4: Create an agent with memory
agent = aiAgent(
    name: "KnowledgeAssistant",
    description: "An assistant with access to the knowledge base",
    memory: vectorMemory
)

// Step 5: Query the agent
response = agent.run( "What is BoxLang?" )
println( response )
```

## Best Practices

1. **Choose the Right Loader**: Use specialized loaders (Markdown, HTML, CSV, JSON) when possible for better metadata extraction.

2. **Configure Chunking**: For vector memory, use appropriate chunk sizes (500-1000 chars) with overlap (100-200 chars).

3. **Use Metadata**: Leverage metadata for filtering and context in RAG queries.

4. **Handle Large Directories**: Use `recursive()` sparingly and filter by `extensions()` to avoid loading unnecessary files.

5. **Error Handling**: DirectoryLoader continues on errors and includes error metadata - check for `loadError` in document metadata.

## See Also

- [Memory Systems](memory.md) - Standard and vector memory types
- [aiChunk() BIF](../chatting/chunking.md) - Text chunking strategies
- [Agents](agents.md) - Using agents with loaded documents
