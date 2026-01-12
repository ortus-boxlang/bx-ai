# aiEmbed

Generate vector embeddings for text using AI providers. Embeddings are numerical representations that capture semantic meaning, enabling semantic search, similarity comparison, clustering, and recommendations.

## Syntax

```javascript
aiEmbed(input, params, options)
```

## Parameters

| Parameter | Type   | Required | Default | Description                                                                          |
| --------- | ------ | -------- | ------- | ------------------------------------------------------------------------------------ |
| `input`   | any    | Yes      | -       | The text or array of texts to generate embeddings for                                |
| `params`  | struct | No       | `{}`    | Request parameters for the AI provider (e.g., `{ model: "text-embedding-3-large" }`) |
| `options` | struct | No       | `{}`    | Request options (e.g., `{ provider: "openai", returnFormat: "embeddings" }`)         |

### Options Structure

| Option         | Type    | Default      | Description                                             |
| -------------- | ------- | ------------ | ------------------------------------------------------- |
| `provider`     | string  | (config)     | The AI provider to use (openai, cohere, voyage, ollama) |
| `apiKey`       | string  | (config/env) | API key for the provider                                |
| `returnFormat` | string  | `"raw"`      | Response format: "raw", "embeddings", "first"           |
| `timeout`      | numeric | `30`         | Request timeout in seconds                              |

### Return Formats

* **"raw"** (default): Full API response with metadata
* **"embeddings"**: Array of embedding vectors only
* **"first"**: Single embedding vector (first item if batch)

## Returns

Returns embedding data based on `returnFormat`:

* **"raw"**: Complete response with `data`, `model`, `usage` metadata
* **"embeddings"**: Array of embedding vectors (arrays of floats)
* **"first"**: Single embedding vector (array of floats)

## Examples

### Basic Single Text Embedding

```javascript
// Simple embedding with defaults
embedding = aiEmbed( "BoxLang is a dynamic JVM language" );

// Full response structure
println( "Model: #embedding.model#" );
println( "Dimensions: #embedding.data.first().embedding.len()#" );
println( "Tokens used: #embedding.usage.total_tokens#" );
```

### Get Just the Vector

```javascript
// Return only the embedding vector
vector = aiEmbed(
    input: "Hello World",
    options: { returnFormat: "first" }
);

println( "Vector length: #vector.len()#" );
println( "First 5 values: #vector.slice(1, 5)#" );
```

### Batch Embeddings

```javascript
// Generate embeddings for multiple texts
texts = [
    "BoxLang is awesome",
    "AI makes development easier",
    "Vector embeddings capture meaning"
];

embeddings = aiEmbed( texts );

// Process each embedding
embeddings.data.each( ( item, index ) => {
    println( "Text ##index##: #texts[index]#" );
    println( "Embedding dimensions: #item.embedding.len()#" );
});
```

### Array of Vectors

```javascript
// Get just the vectors for storage
vectors = aiEmbed(
    input: ["Hello", "World", "BoxLang"],
    options: { returnFormat: "embeddings" }
);

// vectors is now an array of vectors
println( "Generated #vectors.len()# embeddings" );
```

### Specific Provider and Model

```javascript
// OpenAI with specific model
embedding = aiEmbed(
    input: "Semantic search query",
    params: {
        model: "text-embedding-3-large"
    },
    options: {
        provider: "openai"
    }
);

// Cohere embeddings
cohere = aiEmbed(
    input: "Document to embed",
    params: {
        model: "embed-english-v3.0",
        input_type: "search_document"
    },
    options: {
        provider: "cohere"
    }
);
```

### Semantic Search

```javascript
// 1. Embed query
query = "What is BoxLang?";
queryVector = aiEmbed(
    input: query,
    options: { returnFormat: "first" }
);

// 2. Embed documents
documents = [
    "BoxLang is a modern dynamic JVM language",
    "Java runs on the JVM",
    "Python is a popular scripting language"
];

docVectors = aiEmbed(
    input: documents,
    options: { returnFormat: "embeddings" }
);

// 3. Find most similar (cosine similarity)
similarities = docVectors.map( ( docVec ) => {
    return cosineSimilarity( queryVector, docVec );
});

// 4. Get best match
maxIndex = similarities.indexOf( similarities.max() );
println( "Best match: #documents[maxIndex]#" );
```

### Document Chunking with Embeddings

```javascript
// Read and chunk document
document = fileRead( "documentation.txt" );
chunks = aiChunk( document, {
    chunkSize: 500,
    overlap: 100
});

// Generate embeddings for all chunks
embeddedChunks = chunks.map( ( chunk, idx ) => {
    vector = aiEmbed(
        input: chunk,
        options: { returnFormat: "first" }
    );

    return {
        id: idx,
        text: chunk,
        embedding: vector
    };
});

// Store in vector database
vectorDB.insertMany( embeddedChunks );
```

### Voyage AI Embeddings

```javascript
// Voyage for high-quality embeddings
embedding = aiEmbed(
    input: "Technical documentation text",
    params: {
        model: "voyage-2"
    },
    options: {
        provider: "voyage"
    }
);
```

### Ollama Local Embeddings

```javascript
// Free local embeddings with Ollama
embedding = aiEmbed(
    input: "Local embedding example",
    params: {
        model: "nomic-embed-text"
    },
    options: {
        provider: "ollama"
    }
);

// No API key required!
```

### Cohere Input Types

```javascript
// Cohere supports different input types for optimization

// For search queries
queryEmbed = aiEmbed(
    input: "user search query",
    params: {
        model: "embed-english-v3.0",
        input_type: "search_query"
    },
    options: { provider: "cohere" }
);

// For documents to search
docEmbeds = aiEmbed(
    input: ["doc 1", "doc 2", "doc 3"],
    params: {
        model: "embed-english-v3.0",
        input_type: "search_document"
    },
    options: { provider: "cohere" }
);
```

### Multilingual Embeddings

```javascript
// Embed text in multiple languages
texts = [
    "Hello world",           // English
    "Hola mundo",            // Spanish
    "Bonjour le monde",      // French
    "こんにちは世界"          // Japanese
];

embeddings = aiEmbed(
    input: texts,
    params: {
        model: "text-embedding-3-large"  // Supports 100+ languages
    }
);
```

### Caching Embeddings

```javascript
// Cache expensive embeddings
function getEmbeddingCached( text ) {
    var cacheKey = "embed_" & hash( text );

    // Check cache
    if ( cacheExists( cacheKey ) ) {
        return cacheGet( cacheKey );
    }

    // Generate and cache
    var embedding = aiEmbed(
        input: text,
        options: { returnFormat: "first" }
    );

    cachePut( cacheKey, embedding, 60 ); // Cache 1 hour
    return embedding;
}
```

### RAG (Retrieval Augmented Generation)

```javascript
// Complete RAG workflow

// 1. Embed knowledge base
knowledgeBase = [
    "BoxLang is a modern dynamic JVM language",
    "BoxLang has native AI integration",
    "BoxLang supports Java interop"
];

kbEmbeddings = aiEmbed(
    input: knowledgeBase,
    options: { returnFormat: "embeddings" }
);

// 2. Embed user question
question = "Does BoxLang work with Java?";
questionEmbed = aiEmbed(
    input: question,
    options: { returnFormat: "first" }
);

// 3. Find relevant context
similarities = kbEmbeddings.map( kb => cosineSimilarity( questionEmbed, kb ) );
relevantDoc = knowledgeBase[ similarities.indexOf( similarities.max() ) ];

// 4. Generate answer with context
answer = aiChat( "Context: #relevantDoc#" & char(10) & "Question: #question#" );
```

## Notes

* 📐 **Dimensions**: Most models output 1024-3072 dimensional vectors
* 💰 **Cost**: Embedding models are typically much cheaper than chat models
* 🚀 **Performance**: Batch requests (arrays) are more efficient than individual calls
* 🔍 **Use Cases**: Semantic search, similarity, clustering, recommendations, anomaly detection
* 🌍 **Multilingual**: Modern embedding models support 100+ languages
* 💾 **Storage**: Vectors are large - consider compression for scale
* 🎯 **Events**: Fires `beforeAIEmbed` and `afterAIEmbed` events

## Related Functions

* [`aiChunk()`](aichunk.md) - Chunk large documents before embedding
* [`aiTokens()`](aitokens.md) - Estimate token usage
* [`aiMemory()`](aimemory.md) - Store and search embeddings with vector memory
* [`aiService()`](aiservice.md) - Get embedding service providers

## Best Practices

✅ **Batch when possible** - Send arrays of texts for better performance

✅ **Cache embeddings** - Embeddings are deterministic, cache for reuse

✅ **Chunk long documents** - Most models have token limits (e.g., 8192)

✅ **Use appropriate models** - Larger models (3-large) for critical search, smaller for scale

✅ **Normalize vectors** - Some similarity calculations require unit vectors

✅ **Store metadata** - Keep original text with embeddings for retrieval

❌ **Don't embed everything** - Embeddings cost money and storage, be selective

❌ **Don't forget rate limits** - Batch and throttle large embedding jobs

❌ **Don't mix models** - Use same model for queries and documents for consistency
