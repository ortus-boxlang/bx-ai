---
description: "Comprehensive guide to vector memory systems for semantic search and retrieval in BoxLang AI applications."
icon: brain-circuit
---

# Vector Memory Systems

Vector memory enables **semantic search and retrieval** using embeddings to find contextually relevant information based on meaning rather than just recency. This guide covers all vector memory implementations and how to choose the right one for your needs.

> **Looking for Standard Memory?** For conversation history management, see the [Memory Systems Guide](memory.md).

---

## Table of Contents

- [Overview](#overview)
- [How Vector Memory Works](#how-vector-memory-works)
- [Choosing a Vector Provider](#choosing-a-vector-provider)
- [Vector Memory Types](#vector-memory-types)
- [Hybrid Memory](#hybrid-memory)
- [Configuration Examples](#configuration-examples)
- [Best Practices](#best-practices)
- [Advanced Usage](#advanced-usage)

---

## Overview

Vector memory systems store conversation messages as embeddings (numerical vector representations) and enable **semantic similarity search**. Unlike standard memory that retrieves messages chronologically, vector memory finds the most relevant messages based on meaning.

### Key Benefits

- **Semantic Understanding**: Find relevant context based on meaning, not just keywords
- **Long-term Context**: Search across thousands of past messages efficiently
- **Intelligent Retrieval**: Get the most relevant history, even if discussed long ago
- **Scalable**: Handle large conversation datasets with specialized vector databases
- **Flexible**: Choose from local (in-memory), cloud, or self-hosted solutions

### Use Cases

- **Customer Support**: Retrieve relevant past support cases
- **Knowledge Bases**: Find similar questions and answers
- **Long Conversations**: Maintain context across lengthy interactions
- **Multi-session**: Remember user preferences across sessions
- **RAG Applications**: Combine document retrieval with AI responses

---

## How Vector Memory Works

### 1. Embedding Generation

When you add a message, it's converted to a vector embedding:

```java
memory = aiMemory( "chroma", {
    collection: "support_chat",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small"
} )

// Message is automatically embedded and stored
memory.add( "I need help with my billing account" )
```

### 2. Semantic Search

When retrieving context, vector memory finds similar messages:

```java
// Finds semantically similar messages, even with different wording
relevant = memory.getRelevant( query: "payment issues", limit: 5 )
// Returns messages about billing, invoices, charges, etc.
```

### 3. Integration with Agents

Agents automatically use vector memory for context:

```java
agent = aiAgent(
    name: "Support Bot",
    memory: memory
)

// Agent automatically retrieves relevant past conversations
agent.run( "What was my last invoice amount?" )
```

---

## Choosing a Vector Provider

### Quick Decision Matrix

| Provider | Best For | Setup | Cost | Performance |
|----------|---------|-------|------|-------------|
| **BoxVector** | Development, testing, small datasets | ✅ Instant | Free | Good |
| **Hybrid** | Balanced recent + semantic | ✅ Easy | Low | Excellent |
| **ChromaDB** | Python integration, local dev | ⚙️ Moderate | Free | Good |
| **PostgreSQL** | Existing Postgres infrastructure | ⚙️ Moderate | Low | Good |
| **MySQL** | Existing MySQL 9+ infrastructure | ⚙️ Moderate | Low | Good |
| **Pinecone** | Production, cloud-first | ⚙️ Easy | Paid | Excellent |
| **Qdrant** | Self-hosted, high performance | ⚙️ Complex | Free/Paid | Excellent |
| **Weaviate** | GraphQL, knowledge graphs | ⚙️ Complex | Free/Paid | Excellent |
| **Milvus** | Enterprise, massive scale | ⚙️ Complex | Free/Paid | Outstanding |

### Detailed Recommendations

**Start Development:**

- Use **BoxVector** for immediate prototyping
- Use **Hybrid** when you need both recent and semantic context

**Production (Cloud):**

- **Pinecone**: Best for cloud-native, managed service
- **Qdrant Cloud**: Excellent performance, generous free tier

**Production (Self-Hosted):**

- **PostgreSQL**: If you already use Postgres
- **MySQL**: If you already use MySQL 9+
- **Qdrant**: Best performance for self-hosted
- **Milvus**: Enterprise-grade, handles billions of vectors

**Special Use Cases:**

- **ChromaDB**: Python ML infrastructure
- **Weaviate**: Complex queries, GraphQL API
- **Hybrid**: Best of both worlds (recent + semantic)

---

## Vector Memory Types

### BoxVectorMemory

In-memory vector storage perfect for development and testing.

**Features:**
- No external dependencies
- Instant setup
- Full feature support
- Cosine similarity search

**Configuration:**

```java
memory = aiMemory( "boxvector", {
    collection: "dev_chat",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    cache: true,               // Cache embeddings
    cacheName: "default"
} )
```

**Best For:**
- Local development
- Testing
- Small datasets (< 10,000 messages)
- Proof of concepts

**Limitations:**
- Data lost on restart
- Limited to single instance
- Memory usage grows with dataset

---

### ChromaVectorMemory

[ChromaDB](https://www.trychroma.com/) integration for local vector storage.

**Features:**
- Local persistence
- Python ecosystem integration
- Easy Docker deployment
- Metadata filtering

**Setup:**

```bash
# Docker
docker run -p 8000:8000 chromadb/chroma

# Or Python
pip install chromadb
chroma run --host 0.0.0.0 --port 8000
```

**Configuration:**

```java
memory = aiMemory( "chroma", {
    collection: "customer_support",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    host: "localhost",         // ChromaDB host
    port: 8000,                // ChromaDB port
    protocol: "http",
    tenant: "default_tenant",
    database: "default_database",
    timeout: 30
} )
```

**Best For:**
- Python-based infrastructure
- Local development with persistence
- Medium datasets (< 1M vectors)

---

### PostgresVectorMemory

PostgreSQL with [pgvector](https://github.com/pgvector/pgvector) extension.

**Features:**
- Use existing Postgres infrastructure
- ACID compliance
- Familiar SQL queries
- Mature ecosystem

**Setup:**

```sql
-- Enable pgvector extension
CREATE EXTENSION vector;
```

**Configuration:**

```java
memory = aiMemory( "postgres", {
    collection: "ai_memory",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    datasource: "myPostgresDS",     // JDBC datasource
    tableName: "vector_memory",
    dimensions: 1536,                // Embedding dimensions
    metric: "cosine",                // Distance metric
    autoCreate: true                 // Auto-create table
} )
```

**Best For:**
- Existing PostgreSQL deployments
- Applications requiring SQL access
- Strong consistency requirements
- Medium-large datasets

---

### MysqlVectorMemory

MySQL 9+ with native [VECTOR](https://dev.mysql.com/doc/refman/9.0/en/vector-functions.html) data type support.

**Features:**

- Native vector storage (MySQL 9+)
- Use existing MySQL infrastructure
- ACID compliance
- Familiar SQL ecosystem
- Application-layer distance calculations (MySQL Community Edition compatible)

**Requirements:**

- MySQL 9.0 or later (Community or Enterprise Edition)
- Configured BoxLang datasource
- VECTOR data type support

**Setup:**

MySQL 9 Community Edition includes native VECTOR data type support. No extensions needed - tables are auto-created:

```sql
-- Tables are created automatically, but here's the structure:
CREATE TABLE bx_ai_vectors (
    id VARCHAR(255) PRIMARY KEY,
    text LONGTEXT NOT NULL,
    embedding VECTOR(1536) NOT NULL,  -- Native VECTOR type
    metadata JSON,
    collection VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_collection (collection)
);
```

**Configuration:**

```java
memory = aiMemory( "mysql", {
    collection: "ai_memory",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    datasource: "myMysqlDS",         // JDBC datasource name
    table: "bx_ai_vectors",          // Optional: default is "bx_ai_vectors"
    dimensions: 1536,                // Embedding dimensions
    distanceFunction: "COSINE",      // L2, COSINE, or DOT
    autoCreate: true                 // Auto-create table (default: true)
} )
```

**BoxLang Datasource Setup:**

```json
// boxlang.json
{
    "runtime": {
        "datasources": {
            "myMysqlDS": {
                "driver": "mysql",
                "connectionString": "jdbc:mysql://localhost:3306/mydb",
                "username": "user",
                "password": "pass"
            }
        }
    }
}
```

**Distance Functions:**

- **COSINE**: Cosine distance (1 - cosine similarity), best for semantic search
- **L2**: Euclidean distance (L2 norm), good for spatial data
- **DOT**: Dot product similarity, efficient for normalized vectors

**Usage Example:**

```java
// Create MySQL vector memory
memory = aiMemory( "mysql", {
    collection: "customer_support",
    datasource: "myMysqlDS",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    distanceFunction: "COSINE"
} )

// Use with agent
agent = aiAgent(
    name: "Support Bot",
    memory: memory
)

// Conversations are stored with vector embeddings
agent.run( "I need help with billing" )
agent.run( "What are the payment options?" )

// Semantically similar past conversations are automatically retrieved
agent.run( "Tell me about invoices" )  // Finds billing-related history
```

**Best For:**

- Existing MySQL 9+ deployments
- Organizations standardized on MySQL
- Applications requiring SQL access
- ACID compliance requirements
- Medium-large datasets (millions of vectors)

**Performance Notes:**

- Distance calculations performed in application layer (MySQL Community Edition compatible)
- MySQL HeatWave (Oracle Cloud) provides native DISTANCE() function for optimal performance
- Suitable for production use with proper indexing
- Table is automatically created with collection-based indexing

**MySQL Community vs HeatWave:**

- **Community Edition** (Free): VECTOR data type, app-layer distance calculations
- **HeatWave** (Oracle Cloud): Native DISTANCE() function, VECTOR INDEX, GPU acceleration

---

### PineconeVectorMemory

[Pinecone](https://www.pinecone.io/) managed cloud vector database.

**Features:**
- Fully managed, no ops
- Excellent performance
- Auto-scaling
- Built-in metadata filtering

**Setup:**

1. Sign up at [pinecone.io](https://www.pinecone.io/)
2. Create an index
3. Get API key

**Configuration:**

```java
memory = aiMemory( "pinecone", {
    collection: "prod_conversations",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    apiKey: "your-pinecone-api-key",        // Or use PINECONE_API_KEY env var
    environment: "us-west1-gcp",            // Your Pinecone environment
    projectId: "your-project-id",           // Optional: project ID
    dimensions: 1536,
    metric: "cosine"
} )
```

**Best For:**
- Production cloud deployments
- Teams without ML ops expertise
- Rapid scaling requirements
- Global deployments

**Pricing:**
- Free tier: 1GB storage, 100K operations/month
- Paid: Scales with usage

---

### QdrantVectorMemory

[Qdrant](https://qdrant.tech/) high-performance vector search engine.

**Features:**
- Rust-based (excellent performance)
- Rich filtering capabilities
- Payload support
- Self-hosted or cloud

**Setup:**

```bash
# Docker
docker run -p 6333:6333 qdrant/qdrant

# Or Qdrant Cloud - sign up at qdrant.tech
```

**Configuration:**

```java
memory = aiMemory( "qdrant", {
    collection: "chat_history",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    host: "localhost",              // Or Qdrant Cloud URL
    port: 6333,
    apiKey: "",                     // For Qdrant Cloud
    https: false,                   // Use true for cloud
    dimensions: 1536,
    metric: "cosine"
} )
```

**Best For:**
- High-performance requirements
- Self-hosted production
- Complex filtering needs
- Large datasets (millions of vectors)

**Qdrant Cloud:**
- Free tier: 1GB cluster
- Excellent developer experience

---

### WeaviateVectorMemory

[Weaviate](https://weaviate.io/) GraphQL vector database with knowledge graph capabilities.

**Features:**
- GraphQL API
- Automatic vectorization (optional)
- Knowledge graph functionality
- Rich schema support

**Setup:**

```bash
# Docker
docker run -p 8080:8080 semitechnologies/weaviate:latest

# Or Weaviate Cloud
```

**Configuration:**

```java
memory = aiMemory( "weaviate", {
    collection: "Conversations",    // Note: PascalCase for Weaviate classes
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    host: "localhost",              // Or WCS cluster URL
    port: 8080,
    scheme: "http",                 // Use "https" for WCS
    apiKey: "",                     // For Weaviate Cloud
    dimensions: 1536
} )
```

**Best For:**
- Complex entity relationships
- Knowledge graph requirements
- GraphQL preferences
- Multi-modal applications

---

### MilvusVectorMemory

[Milvus](https://milvus.io/) enterprise-grade distributed vector database.

**Features:**
- Massive scalability (billions of vectors)
- Distributed architecture
- GPU acceleration support
- Enterprise features

**Setup:**

```bash
# Docker Compose (Milvus Standalone)
wget https://github.com/milvus-io/milvus/releases/download/v2.3.0/milvus-standalone-docker-compose.yml -O docker-compose.yml
docker-compose up -d

# Or Zilliz Cloud (managed Milvus)
```

**Configuration:**

```java
memory = aiMemory( "milvus", {
    collection: "enterprise_conversations",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    host: "localhost",
    port: 19530,
    user: "",                       // Optional authentication
    password: "",
    dimensions: 1536,
    metric: "IP",                   // Inner Product (or "L2", "COSINE")
    indexType: "IVF_FLAT"          // Index type for performance
} )
```

**Best For:**
- Enterprise deployments
- Massive datasets (> 10M vectors)
- High throughput requirements
- GPU-accelerated search

---

## Hybrid Memory

**HybridMemory** combines the benefits of both standard memory (recency) and vector memory (relevance).

### How It Works

1. Maintains recent messages in a window
2. Stores all messages in vector database
3. Returns combination of recent + semantically relevant messages
4. Automatically deduplicates

### Configuration

```java
memory = aiMemory( "hybrid", {
    recentLimit: 5,                 // Number of recent messages
    semanticLimit: 5,               // Number of semantic matches
    totalLimit: 10,                 // Max combined messages
    recentWeight: 0.6,              // 60% recent, 40% semantic
    vectorProvider: "chroma",       // Vector backend
    vectorConfig: {                 // Vector provider config
        collection: "hybrid_chat",
        embeddingProvider: "openai",
        embeddingModel: "text-embedding-3-small"
    }
} )
```

### Benefits

- **Recent Context**: Always includes latest messages
- **Semantic Relevance**: Finds related past conversations
- **Balanced**: Best of both approaches
- **Automatic**: No manual context management

### Use Cases

```java
// Customer support with history
agent = aiAgent(
    name: "Support Agent",
    memory: aiMemory( "hybrid", {
        recentLimit: 3,             // Last 3 messages
        semanticLimit: 5,           // 5 relevant past cases
        vectorProvider: "pinecone",
        vectorConfig: {
            collection: "support_history",
            embeddingProvider: "openai"
        }
    } )
)

// Agent automatically gets recent conversation + relevant past cases
agent.run( "I'm having the same billing issue as before" )
```

---

## Configuration Examples

### Development Setup

```java
// Quick start with BoxVector
memory = aiMemory( "boxvector", {
    collection: "dev",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small"
} )

// Or use Hybrid for realistic testing
memory = aiMemory( "hybrid", {
    recentLimit: 3,
    semanticLimit: 3,
    vectorProvider: "boxvector"
} )
```

### Production (Cloud)

```java
// Pinecone (managed)
memory = aiMemory( "pinecone", {
    collection: "prod_chat",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    apiKey: getSystemSetting( "PINECONE_API_KEY" ),
    environment: "us-west1-gcp",
    dimensions: 1536
} )

// Qdrant Cloud
memory = aiMemory( "qdrant", {
    collection: "prod_conversations",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    host: "xyz.qdrant.io",
    port: 6333,
    apiKey: getSystemSetting( "QDRANT_API_KEY" ),
    https: true
} )
```

### Production (Self-Hosted)

```java
// PostgreSQL with pgvector
memory = aiMemory( "postgres", {
    collection: "ai_vectors",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    datasource: "mainDB",
    dimensions: 1536,
    autoCreate: true
} )

// Qdrant (Docker)
memory = aiMemory( "qdrant", {
    collection: "self_hosted_chat",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    host: "qdrant.internal.network",
    port: 6333
} )
```

### Embedding Provider Options

```java
// OpenAI (fastest, most accurate)
{
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small"  // Or "text-embedding-3-large"
}

// Ollama (local, free)
{
    embeddingProvider: "ollama",
    embeddingModel: "nomic-embed-text"        // Or "mxbai-embed-large"
}

// Any supported provider
{
    embeddingProvider: "deepseek",
    embeddingModel: "deepseek-embedding"
}
```

### With Caching

```java
memory = aiMemory( "chroma", {
    collection: "cached_chat",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    cache: true,                    // Enable embedding cache
    cacheName: "default",           // CacheBox provider
    cacheTimeout: 3600,             // 1 hour
    cacheLastAccessTimeout: 1800    // 30 minutes
} )
```

---

## Best Practices

### 1. Choose Appropriate Embedding Models

```java
// Small, fast, cost-effective
embeddingModel: "text-embedding-3-small"    // OpenAI - 1536 dimensions

// Large, more accurate
embeddingModel: "text-embedding-3-large"    // OpenAI - 3072 dimensions

// Local, free
embeddingModel: "nomic-embed-text"          // Ollama - 768 dimensions
```

### 2. Use Metadata for Filtering

```java
// Add metadata when storing
memory.add({
    text: "User reported billing issue",
    metadata: {
        userId: "user123",
        category: "billing",
        priority: "high",
        timestamp: now()
    }
})

// Filter on retrieval
relevant = memory.getRelevant(
    query: "payment problems",
    limit: 5,
    filter: { category: "billing", priority: "high" }
)
```

### 3. Optimize Collection Size

```java
// Periodic cleanup of old vectors
function cleanupOldVectors( memory, daysOld = 90 ) {
    var cutoffDate = dateAdd( "d", -daysOld, now() );
    memory.deleteByFilter({
        timestamp_lt: cutoffDate
    })
}
```

### 4. Monitor Performance

```java
// Enable logging for debugging
memory = aiMemory( "pinecone", {
    collection: "prod",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    cache: true,
    logRequests: true,          // Log vector operations
    logRequestToConsole: false
} )
```

### 5. Use Hybrid for User-Facing Apps

```java
// Combines recent conversation with relevant history
memory = aiMemory( "hybrid", {
    recentLimit: 5,              // Always include last 5 messages
    semanticLimit: 3,            // Add 3 relevant past messages
    totalLimit: 8,               // Max 8 total
    vectorProvider: "qdrant"
} )
```

### 6. Dimension Matching

Ensure embedding dimensions match across your application:

```java
// OpenAI text-embedding-3-small = 1536 dimensions
memory = aiMemory( "pinecone", {
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    dimensions: 1536                // Must match model output
} )
```

---

## Advanced Usage

### Custom Similarity Thresholds

```java
// Only retrieve highly relevant messages
relevant = memory.getRelevant(
    query: userInput,
    limit: 10,
    minScore: 0.8               // 80% similarity threshold
)
```

### Multi-Collection Strategy

```java
// Separate collections for different contexts
supportMemory = aiMemory( "pinecone", {
    collection: "customer_support",
    embeddingProvider: "openai"
} )

salesMemory = aiMemory( "pinecone", {
    collection: "sales_conversations",
    embeddingProvider: "openai"
} )

// Use appropriate memory based on context
memory = userType == "support" ? supportMemory : salesMemory
```

### Cross-Session Continuity

```java
// Per-user persistent memory
function getUserMemory( userId ) {
    return aiMemory( "postgres", {
        collection: "user_#userId#_history",
        embeddingProvider: "openai",
        embeddingModel: "text-embedding-3-small",
        datasource: "mainDB"
    } )
}

// Each user has their own vector collection
userMemory = getUserMemory( session.userId )
agent = aiAgent( name: "Assistant", memory: userMemory )
```

### Batch Operations

```java
// Add multiple messages efficiently
messages.each( function( msg ) {
    memory.add( msg )
})

// Better: use batch add (if supported by provider)
memory.addBatch( messages )
```

---

## Troubleshooting

### Common Issues

**1. Dimension Mismatch**
```
Error: Vector dimension mismatch
```
Solution: Ensure embedding model dimensions match collection configuration

**2. Connection Errors**
```
Error: Could not connect to vector database
```
Solution: Verify host, port, and network accessibility. Check firewall rules.

**3. API Key Issues**
```
Error: Unauthorized
```
Solution: Verify API keys for both embedding provider and vector database

**4. Slow Performance**
```
Searches taking too long
```
Solution:
- Enable caching for embeddings
- Use appropriate index type (Milvus, Qdrant)
- Reduce limit parameter
- Consider smaller embedding model

**5. Out of Memory**
```
Error: OutOfMemoryException (BoxVector)
```
Solution: Switch to persistent vector database (Chroma, Postgres, etc.)

---

## See Also

- [Memory Systems Guide](memory.md) - Standard conversation memory
- [Custom Vector Memory](../advanced/custom-vector-memory.md) - Build your own provider
- [Embeddings Guide](../advanced/embeddings.md) - Understanding embeddings
- [Agents Documentation](agents.md) - Using memory in agents
- [Examples](../../examples/vector-memory/) - Complete working examples

---

**Next Steps:** Try the [Vector Memory Examples](../../examples/vector-memory/) or learn about [building custom vector memory](../advanced/custom-vector-memory.md) providers.
