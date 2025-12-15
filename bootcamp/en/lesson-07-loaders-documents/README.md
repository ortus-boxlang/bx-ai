# Lesson 7: Loaders & Documents

**⏱️ Duration: 60 minutes**

Welcome to document loaders! In this lesson, you'll learn how to import content from various sources (files, directories, URLs, databases) and prepare it for AI processing, including building a simple RAG (Retrieval-Augmented Generation) system.

## 🎯 What You'll Learn

- Load documents from files, directories, and URLs
- Work with different file formats (text, markdown, PDF, CSV, JSON)
- Chunk large documents for AI processing
- Ingest documents into vector memory
- Build a simple RAG system for context-aware AI

---

## 📚 Part 1: Understanding Document Loaders (10 mins)

### What is a Document Loader?

A document loader imports content from external sources and converts it into a standardized `Document` format that AI can process.

```
┌─────────────────────────────────────────────────────────────────┐
│                    DOCUMENT LOADING FLOW                        │
└─────────────────────────────────────────────────────────────────┘

  ┌─────────┐      ┌─────────┐      ┌─────────┐      ┌─────────┐
  │ Source  │─────▶│ Loader  │─────▶│Document │─────▶│   AI    │
  │ (Files) │      │         │      │ Objects │      │Processing│
  └─────────┘      └─────────┘      └─────────┘      └─────────┘
       │                                  │
       ▼                                  ▼
  • Text files                      • Standardized format
  • PDFs                            • With metadata
  • Web pages                       • Ready for chunking
  • Databases                       • Embeddable


  💡 Think of loaders as "import adapters" for AI
```

### Document Structure

Every loaded document has these properties:

```javascript
{
    id: "unique-doc-id",           // Unique identifier
    content: "The actual text...",  // Document content
    metadata: {                     // Additional information
        source: "/path/to/file.txt",
        type: "text",
        createdDate: "2024-01-15",
        custom: "any value"
    },
    embedding: []                   // Vector embedding (optional)
}
```

### Why Document Loaders?

- ✅ **Unified Interface** - Load any source the same way
- ✅ **Metadata Preservation** - Keep track of source, dates, etc.
- ✅ **RAG Ready** - Perfect for retrieval-augmented generation
- ✅ **Batch Processing** - Handle entire directories at once
- ✅ **Memory Integration** - Direct ingestion into vector stores

---

## 💻 Part 2: Basic File Loading (15 mins)

### The aiDocuments() BIF

```javascript
// Load a single text file
docs = aiDocuments( "/path/to/file.txt" ).load()

// Load with specific type
docs = aiDocuments( "/path/to/file.md", { type: "markdown" } ).load()

// Load from URL
docs = aiDocuments( "https://example.com/page.html" ).load()
```

### Example 1: Loading a Text File

**Create a sample document (`knowledge.txt`):**
```
BoxLang is a modern dynamic JVM language.
It runs on the Java Virtual Machine.
BoxLang supports both object-oriented and functional programming.
```

**Load and use it:**
```javascript
// 01-load-text-file.bxs

// Load the document
docs = aiDocuments( "knowledge.txt" ).load()

println( "Loaded #arrayLen( docs )# document(s)" )

// Access document properties
doc = docs[ 1 ]
println( "Content: #doc.content#" )
println( "Source: #doc.metadata.source#" )

// Use with AI
result = aiChat(
    message: "Based on this context: #doc.content#\n\nWhat is BoxLang?",
    provider: "openai"
)

println( "\nAI Response:" )
println( result )
```

**Run it:**
```bash
boxlang 01-load-text-file.bxs
# Loaded 1 document(s)
# Content: BoxLang is a modern dynamic JVM language...
# Source: knowledge.txt
# AI Response: BoxLang is a modern dynamic JVM language...
```

### Example 2: Loading Different File Types

```javascript
// 02-load-different-types.bxs

// Load a markdown file
mdDocs = aiDocuments( "readme.md", { type: "markdown" } ).load()
println( "Loaded #arrayLen( mdDocs )# markdown document(s)" )

// Load a CSV file
csvDocs = aiDocuments( "data.csv", { type: "csv" } ).load()
println( "Loaded #arrayLen( csvDocs )# CSV document(s)" )

// Load JSON
jsonDocs = aiDocuments( "config.json", { type: "json" } ).load()
println( "Loaded #arrayLen( jsonDocs )# JSON document(s)" )

// Each CSV row and JSON object becomes a separate document
for ( doc in csvDocs ) {
    println( "CSV Row: #doc.content#" )
}
```

### 🧪 Lab 1: Load Your First Document (5 mins)

**Task:** Load a markdown file and ask AI to summarize it.

**Steps:**
1. Create a markdown file with some content (recipe, tutorial, notes)
2. Load it using `aiDocuments()`
3. Pass the content to `aiChat()` for summarization
4. Print the summary

**Starter code in `labs/lab-01-load-and-summarize.bxs`**

---

## 📁 Part 3: Directory Loading (10 mins)

### Loading Entire Directories

```javascript
// Load all files from a directory
docs = aiDocuments( "/path/to/folder" ).load()

// Load recursively (subdirectories)
docs = aiDocuments( "/path/to/folder" )
    .recursive()
    .load()

// Filter by file extensions
docs = aiDocuments( "/path/to/folder" )
    .recursive()
    .extensions( [ "md", "txt" ] )
    .load()
```

### Example 3: Knowledge Base from Directory

**Create directory structure:**
```
knowledge-base/
├── product-info.md
├── faq.txt
└── guides/
    ├── getting-started.md
    └── advanced.md
```

**Load everything:**
```javascript
// 03-load-directory.bxs

docs = aiDocuments( "knowledge-base" )
    .recursive()
    .extensions( [ "md", "txt" ] )
    .load()

println( "Loaded #arrayLen( docs )# documents from knowledge base" )

// Show what was loaded
for ( doc in docs ) {
    println( "- #doc.metadata.source#" )
}

// Use all documents for context
allContent = docs.map( d => d.content ).toList( "\n\n" )

result = aiChat(
    message: "Based on this knowledge base:\n\n#allContent#\n\nWhat products do we offer?",
    provider: "openai"
)

println( "\n" & result )
```

### Filtering Documents

```javascript
// Filter during load
docs = aiDocuments( "/docs" )
    .filter( doc => doc.getContentLength() > 100 )
    .load()

// Filter after load
largeDocs = docs.filter( doc => len( doc.content ) > 500 )
```

---

## ✂️ Part 4: Document Chunking (10 mins)

### Why Chunking?

Large documents need to be split into smaller chunks because:
- ✅ AI models have token limits
- ✅ Embeddings work better on focused content
- ✅ More precise retrieval in RAG systems

```
┌─────────────────────────────────────────────────────────────────┐
│                    CHUNKING STRATEGY                            │
└─────────────────────────────────────────────────────────────────┘

  Original Document (5000 words)
  ┌────────────────────────────────────────────────────┐
  │ Chapter 1: Introduction                            │
  │ Chapter 2: Getting Started                         │
  │ Chapter 3: Advanced Topics                         │
  │ Chapter 4: Best Practices                          │
  └────────────────────────────────────────────────────┘
                       │
                       ▼ (chunk with overlap)
         ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
         │Chunk 1 │─▶│Chunk 2 │─▶│Chunk 3 │─▶│Chunk 4 │
         │500 wds │  │500 wds │  │500 wds │  │500 wds │
         └────────┘  └────────┘  └────────┘  └────────┘
              └─────▶overlap◀─────┘
                    (50 words)

  💡 Overlap helps maintain context between chunks
```

### Chunking Configuration

```javascript
// Load with chunking
docs = aiDocuments( "/large-doc.txt" )
    .chunkSize( 500 )        // Characters per chunk
    .overlap( 50 )           // Overlap between chunks
    .load()

// Or specify in config
docs = aiDocuments( "/large-doc.txt", {
    chunkSize: 500,
    overlap: 50
} ).load()
```

### Example 4: Chunking a Large Document

```javascript
// 04-chunk-large-document.bxs

// Create a large sample document
largeContent = repeatString( "BoxLang is a powerful JVM language. ", 100 )
fileWrite( "large-doc.txt", largeContent )

// Load WITHOUT chunking
docsNoChunk = aiDocuments( "large-doc.txt" ).load()
println( "Without chunking: #arrayLen( docsNoChunk )# document(s)" )
println( "Content length: #len( docsNoChunk[ 1 ].content )#" )

// Load WITH chunking
docsChunked = aiDocuments( "large-doc.txt" )
    .chunkSize( 200 )
    .overlap( 20 )
    .load()

println( "\nWith chunking: #arrayLen( docsChunked )# chunk(s)" )

// Show each chunk
for ( i = 1; i <= arrayLen( docsChunked ); i++ ) {
    println( "Chunk #i#: #len( docsChunked[ i ].content )# chars" )
}
```

**Output:**
```
Without chunking: 1 document(s)
Content length: 3700

With chunking: 20 chunk(s)
Chunk 1: 200 chars
Chunk 2: 200 chars
...
```

---

## 🧠 Part 5: Memory Integration & RAG (15 mins)

### What is RAG?

**RAG (Retrieval-Augmented Generation)** combines:
1. **Retrieval** - Find relevant documents from a knowledge base
2. **Augmentation** - Add those documents to the AI prompt
3. **Generation** - AI generates a response using the context

```
┌─────────────────────────────────────────────────────────────────┐
│                      RAG WORKFLOW                               │
└─────────────────────────────────────────────────────────────────┘

  Step 1: Index Documents           Step 2: Query & Retrieve
  ─────────────────────              ──────────────────────
  ┌──────────┐                       User Question
  │Documents │                             │
  │(chunked) │                             ▼
  └─────┬────┘                       ┌─────────┐
        │                            │ Vector  │
        ▼                            │ Search  │
  ┌──────────┐                       └────┬────┘
  │Embeddings│                            │
  └─────┬────┘                            ▼
        │                            Top 3 Relevant Chunks
        ▼
  ┌──────────┐                       Step 3: Generate Answer
  │  Vector  │                       ─────────────────────
  │  Memory  │                       ┌─────────────────┐
  └──────────┘                       │ Chunks + Query  │
                                     └────────┬────────┘
                                              ▼
                                         ┌─────────┐
                                         │   AI    │
                                         │Response │
                                         └─────────┘
```

### Direct Memory Ingestion

The `toMemory()` method loads documents directly into vector memory:

```javascript
// Create vector memory using aiMemory() with type
memory = aiMemory(
    memory: "chroma",
    config: { collectionName: "knowledge-base" }
)

// Load documents and ingest to memory
result = aiDocuments( "/docs" )
    .recursive()
    .extensions( [ "md", "txt" ] )
    .toMemory( memory, { chunkSize: 500, overlap: 50 } )

println( "Documents loaded: #result.documentsIn#" )
println( "Chunks created: #result.chunksOut#" )
println( "Stored in memory: #result.stored#" )
```

### Example 5: Building a Simple RAG System

```javascript
// 05-simple-rag-system.bxs

// Step 1: Create knowledge base documents
fileWrite( "docs/product-a.txt", "Product A is a web development tool for BoxLang developers." )
fileWrite( "docs/product-b.txt", "Product B is a testing framework for BoxLang applications." )
fileWrite( "docs/pricing.txt", "Product A costs $49/month. Product B costs $29/month." )

// Step 2: Create vector memory (Ollama embeddings - free!)
memory = aiMemory(
    memory: "chroma",
    config: {
        collectionName: "products",
        embeddingProvider: "ollama",
        embeddingModel: "nomic-embed-text"
    }
)

// Step 3: Load and ingest documents
println( "Loading documents into vector memory..." )
result = aiDocuments( "docs" )
    .recursive()
    .toMemory( memory )

println( "✅ Indexed #result.stored# document chunks" )

// Step 4: Query the RAG system
function askRAG( question ) {
    // Retrieve relevant documents
    results = memory.search( question, 3 )

    // Build context from retrieved documents
    context = results.map( r => r.content ).toList( "\n\n" )

    // Ask AI with context
    return aiChat(
        message: "Context:\n#context#\n\nQuestion: #question#",
        provider: "ollama",
        model: "qwen2.5:0.5b-instruct"
    )
}

// Step 5: Ask questions!
println( "\n" & askRAG( "What products do you offer?" ) )
println( "\n" & askRAG( "How much does Product A cost?" ) )
println( "\n" & askRAG( "What is Product B used for?" ) )
```

### 🧪 Lab 2: Build Your RAG System (10 mins)

**Task:** Create a RAG system for a specific domain (recipes, code snippets, company docs).

**Requirements:**
1. Create 3-5 text/markdown files with related content
2. Load them into vector memory with `toMemory()`
3. Create a `queryKnowledge()` function that retrieves and uses context
4. Ask at least 3 different questions

**Starter code in `labs/lab-02-build-rag-system.bxs`**

---

## 🌐 Part 6: Advanced Loading (5 mins)

### Loading from URLs

```javascript
// Load web page
docs = aiDocuments( "https://boxlang.io/docs" ).load()

// Load with custom headers
docs = aiDocuments( "https://api.example.com/data", {
    type: "http",
    headers: { "Authorization": "Bearer token" }
} ).load()
```

### Loading from Databases

```javascript
// Load from SQL query
docs = aiDocuments( config: {
    type: "sql",
    datasource: "myDB",
    sql: "SELECT title, content, created_date FROM articles",
    contentColumn: "content",
    metadataColumns: [ "title", "created_date" ]
} ).load()
```

### Async Loading

```javascript
// Load asynchronously for large datasets
future = aiDocuments( "/huge-dataset" ).loadAsync()

// Do other work...
println( "Loading in background..." )

// Wait for completion
docs = future.get()
println( "Loaded #arrayLen( docs )# documents" )
```

---

## 📊 Part 7: Complete RAG Example (5 mins)

### Example 6: Customer Support Knowledge Base

```javascript
// 06-customer-support-rag.bxs

// Create comprehensive support knowledge base
fileWrite( "kb/returns.md", "# Returns\nCustomers can return items within 30 days." )
fileWrite( "kb/shipping.md", "# Shipping\nWe offer free shipping on orders over $50." )
fileWrite( "kb/warranty.md", "# Warranty\nAll products have a 1-year warranty." )

// Setup vector memory
memory = aiMemory(
    memory: "chroma",
    config: {
        collectionName: "customer-support",
        embeddingProvider: "ollama"
    }
)

// Ingest knowledge base
result = aiDocuments( "kb" )
    .recursive()
    .extensions( [ "md" ] )
    .toMemory( memory, { chunkSize: 300 } )

println( "Knowledge base ready: #result.stored# chunks indexed" )

// Create AI agent with RAG
supportAgent = aiAgent()
    .withInstructions( "You are a helpful customer support agent. Use the knowledge base to answer questions accurately." )
    .withMemory( memory )
    .build()

// Handle customer questions
questions = [
    "What is your return policy?",
    "Do you offer free shipping?",
    "How long is the warranty?"
]

for ( question in questions ) {
    println( "\nQ: #question#" )
    println( "A: #supportAgent.run( question )#" )
}
```

---

## 🎯 Summary

### What You Learned

✅ **Document Loaders** - Load files, directories, URLs, databases
✅ **File Formats** - Text, markdown, PDF, CSV, JSON, XML
✅ **Directory Loading** - Recursive loading with filters
✅ **Chunking** - Split large documents intelligently
✅ **Memory Integration** - Use `toMemory()` for RAG
✅ **RAG Systems** - Retrieval-Augmented Generation workflow

### Key BIFs

| BIF | Purpose |
|-----|---------|
| `aiDocuments()` | Create document loader |
| `aiMemory()` | Create memory (including vector memory) |

### Next Steps

Now that you can load and process documents, you're ready for:
- **Lesson 8** - MCP Servers & Clients
- **Advanced Topics** - Custom loaders, transformations, multi-memory systems

---

## 📝 Resources

- [Document Loaders Documentation](../../../docs/main-components/document-loaders.md)
- [Vector Memory Documentation](../../../docs/main-components/vector-memory.md)
- [RAG Documentation](../../../docs/main-components/rag.md)

---

**🎉 Congratulations!** You can now build RAG systems with BoxLang AI!

👉 **[Continue to Lesson 8: MCP Servers & Clients](../lesson-08-mcp-servers-clients/)**
