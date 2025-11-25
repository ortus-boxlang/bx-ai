Vector Memory Implementation Plan

This is my vector memory plan. Whaw do you think? What would you change? What would you add or remove?

Architecture Overview
New Interface: IVectorMemory (extends IAiMemory)

Adds vector-specific operations on top of existing memory interface
Methods: store(), search(), delete(), upsert(), getById()
Abstract Base: BaseVectorMemory (extends BaseMemory, implements IVectorMemory)

Common vector operations (embedding generation, metadata handling)
Abstract methods for provider-specific implementations
Built-in integration with aiEmbed() BIF for automatic embedding generation

Provider Implementations:

PostgresVectorMemory - pgvector extension (JDBC-based)
ChromaVectorMemory - HTTP REST API
WeaviateVectorMemory - HTTP REST API
PineconeVectorMemory - HTTP REST API
QdrantVectorMemory - HTTP REST API
MilvusVectorMemory - HTTP REST API

src/main/bx/models/memory/
├── vector/
│   ├── IVectorMemory.bx           # Vector-specific interface
│   ├── BaseVectorMemory.bx        # Abstract base implementation
│   ├── PostgresVectorMemory.bx    # pgvector (JDBC)
│   ├── ChromaVectorMemory.bx      # HTTP REST
│   ├── WeaviateVectorMemory.bx    # HTTP REST
│   ├── PineconeVectorMemory.bx    # HTTP REST
│   ├── QdrantVectorMemory.bx      # HTTP REST
│   └── MilvusVectorMemory.bx      # HTTP REST

API Design

IVectorMemory Interface

interface extends="IAiMemory" {
    // Store document with automatic embedding
    IVectorMemory function store(
        required string id,
        required string text,
        struct metadata = {},
        array embedding  // Optional pre-computed embedding
    );

    // Semantic search by text query
    array function search(
        required string query,
        numeric topK = 5,
        struct filter = {},  // Metadata filters
        numeric minScore = 0.0
    );

    // Search by pre-computed embedding vector
    array function searchByVector(
        required array embedding,
        numeric topK = 5,
        struct filter = {}
    );

    // Get document by ID
    struct function getById( required string id );

    // Upsert (insert or update)
    IVectorMemory function upsert(
        required string id,
        required string text,
        struct metadata = {}
    );

    // Delete by ID
    boolean function delete( required string id );

    // Delete by filter
    numeric function deleteByFilter( required struct filter );

    // Collection/Index management
    IVectorMemory function createCollection( required string name );
    boolean function collectionExists( required string name );
    IVectorMemory function deleteCollection( required string name );
}


Usage Examples

1. PostgreSQL pgvector (JDBC-based)

vector = aiMemory(
    memory: "postgres-vector",
    config: {
        datasource: "myDB",
        collection: "documents",
        dimensions: 1536,  // OpenAI ada-002
        embeddingProvider: "openai",
        embeddingModel: "text-embedding-ada-002"
    }
);

// Store with auto-embedding
vector.store(
    id: "doc1",
    text: "BoxLang is a modern JVM language",
    metadata: { category: "programming", year: 2024 }
);

// Semantic search
results = vector.search(
    query: "What is BoxLang?",
    topK: 5,
    filter: { category: "programming" }
);
// Returns: [{ id, text, score, metadata, embedding }, ...]


2. Chroma (HTTP REST)

vector = aiMemory(
    memory: "chroma",
    config: {
        endpoint: "http://localhost:8000",
        collection: "knowledge_base",
        embeddingProvider: "openai"
    }
);

vector.store(
    id: "doc2",
    text: "AI agents can use tools",
    metadata: { type: "concept" }
);

results = vector.search( query: "agent tools", topK: 3 );


pinecone

vector = aiMemory(
    memory: "pinecone",
    config: {
        apiKey: "pk-xxx",
        environment: "us-east1-gcp",
        indexName: "main-index",
        embeddingProvider: "openai"
    }
);

// Upsert (update or insert)
vector.upsert(
    id: "vec1",
    text: "Updated content",
    metadata: { version: 2 }
);


Implementation Phases
Phase 1: Core Foundation ✅
Create IVectorMemory interface
Implement BaseVectorMemory abstract class
Add embedding auto-generation via aiEmbed() BIF
Create test harness

Phase 2: JDBC Implementation 🔧
PostgresVectorMemory with pgvector extension
SQL schema generation for vector columns
HNSW index creation
Cosine/L2/Inner Product distance functions

Phase 3: HTTP Providers 🌐
ChromaVectorMemory (easiest, good for local dev)
QdrantVectorMemory (full-featured, Docker-friendly)
WeaviateVectorMemory (GraphQL + REST)
PineconeVectorMemory (cloud, requires API key)
MilvusVectorMemory (gRPC via HTTP gateway)

Phase 4: Integration 🔗
Update aiMemory() BIF factory
Add vector memory types to documentation
 - readme.md
 - docs/readme.md
 - pipelines/memory.md
 - advanced/vector-databases.md

Create examples in the examples folder
Add to course materials

Phase 5: Advanced Features 🚀
Hybrid search (vector + keyword)
Batch operations (bulk insert/delete)
Async operations for large datasets
Collection/namespace management
Metadata filtering optimization


Embedding Generation: Auto-generate via aiEmbed() BIF, but allow pre-computed embeddings
Provider Abstraction: Each provider implements same interface, different backends
Metadata Filtering: Provider-specific (some support rich queries, others basic key-value)
Distance Metrics: Configurable (cosine, euclidean, dot product) where supported
Error Handling: Graceful fallbacks, detailed error messages
Testing: Mock HTTP responses for HTTP providers, use testcontainers for JDBC

I already have a docker-compose.yaml in the root and it's running a Chroma instance for testing. Update as you see fit.

Remember, our motto is fluency, simplicity, productivity. The idea is to make all the integrations and BoxLang AI to be human and developer friendly.