# Custom Vector Memory

This guide shows you how to create custom vector memory implementations by extending `BaseVectorMemory` and implementing the `IVectorMemory` interface. Custom vector memories allow you to integrate with any vector database or implement specialized semantic search behaviors.

## When to Build Custom Vector Memory

Consider building a custom vector memory when:

- **Integrating New Vector Databases**: Your organization uses a vector database not natively supported (e.g., Elasticsearch, MongoDB Atlas Vector Search, Redis Vector)
- **Custom Embedding Logic**: You need specialized embedding generation (e.g., custom models, pre-processing, caching)
- **Specialized Search**: You require advanced filtering, hybrid search, or custom ranking algorithms
- **Performance Optimization**: You need specific optimizations for your use case (e.g., approximate nearest neighbor tuning)
- **Multi-Collection Management**: You need to search across multiple collections with custom merging logic
- **Access Control**: You require row-level security or tenant isolation in vector search

## Understanding BaseVectorMemory

The `BaseVectorMemory` class provides most of the functionality you need:

### What BaseVectorMemory Provides

```boxlang
// Automatic handling of:
- Message storage and retrieval
- Embedding generation via configured provider
- Basic configuration management
- Message counting and clearing
- Export/import functionality (partial)
- System message handling
```

### What You Need to Implement

When extending `BaseVectorMemory`, you must implement these key methods:

```boxlang
/**
 * Store a message with its vector representation
 */
function add( required any message )

/**
 * Retrieve semantically relevant messages
 */
function getRelevant( required string query, numeric limit = 5 )

/**
 * Get all stored messages (for non-vector operations)
 */
function getAll()

/**
 * Remove all messages from vector storage
 */
function clear()

/**
 * Count total messages in vector storage
 */
function count()
```

### Key Properties in BaseVectorMemory

```boxlang
variables.collection           // Collection/index name
variables.embeddingProvider    // AI provider for embeddings (openai, etc.)
variables.embeddingModel       // Model to use (text-embedding-3-small, etc.)
variables.dimensions           // Vector dimensions (1536, 768, etc.)
variables.metric               // Distance metric (cosine, euclidean, dot)
variables.key                  // Conversation/session identifier
```

## IVectorMemory Interface

The complete interface you must implement:

```boxlang
interface {
    /**
     * Configure the vector memory
     */
    function configure( required struct config );

    /**
     * Set the conversation key
     */
    function key( required string key );

    /**
     * Add a message to vector storage
     */
    function add( required any message );

    /**
     * Get semantically relevant messages
     */
    function getRelevant( required string query, numeric limit = 5 );

    /**
     * Get all stored messages
     */
    function getAll();

    /**
     * Clear all messages
     */
    function clear();

    /**
     * Count stored messages
     */
    function count();

    /**
     * Set system message
     */
    function setSystemMessage( required string message );

    /**
     * Get system message
     */
    function getSystemMessage();

    /**
     * Export memory state
     */
    function export();

    /**
     * Import memory state
     */
    function import( required struct data );
}
```

## Example 1: ElasticsearchVectorMemory

A complete implementation using Elasticsearch with vector similarity search:

```boxlang
import bxModules.bxai.models.util.TextChunker;

/**
 * Elasticsearch Vector Memory
 * Uses Elasticsearch dense_vector field type for semantic search
 */
class extends="BaseVectorMemory" implements="IVectorMemory" {

    property name="esClient" type="any";
    property name="indexName" type="string";

    /**
     * Configure Elasticsearch connection
     */
    function configure( required struct config ) {
        // Call parent configuration
        super.configure( arguments.config );

        // Elasticsearch-specific configuration
        variables.esHost = arguments.config.esHost ?: "localhost";
        variables.esPort = arguments.config.esPort ?: 9200;
        variables.esScheme = arguments.config.esScheme ?: "http";
        variables.esUser = arguments.config.esUser ?: "";
        variables.esPassword = arguments.config.esPassword ?: "";
        variables.indexName = arguments.config.collection ?: "ai_memory";

        // Initialize Elasticsearch HTTP client
        initializeClient();

        // Ensure index exists with proper mapping
        ensureIndex();

        return this;
    }

    /**
     * Initialize Elasticsearch HTTP client
     */
    private function initializeClient() {
        variables.esClient = {
            host: variables.esHost,
            port: variables.esPort,
            scheme: variables.esScheme,
            user: variables.esUser,
            password: variables.esPassword,
            baseURL: "#variables.esScheme#://#variables.esHost#:#variables.esPort#"
        };
    }

    /**
     * Ensure index exists with vector mapping
     */
    private function ensureIndex() {
        // Check if index exists
        var exists = esRequest(
            method: "HEAD",
            endpoint: "/#variables.indexName#"
        ).statusCode == 200;

        if ( !exists ) {
            // Create index with vector field mapping
            esRequest(
                method: "PUT",
                endpoint: "/#variables.indexName#",
                body: {
                    "mappings": {
                        "properties": {
                            "id": { "type": "keyword" },
                            "key": { "type": "keyword" },
                            "role": { "type": "keyword" },
                            "content": { "type": "text" },
                            "embedding": {
                                "type": "dense_vector",
                                "dims": variables.dimensions,
                                "index": true,
                                "similarity": mapMetricToES( variables.metric )
                            },
                            "timestamp": { "type": "date" },
                            "metadata": { "type": "object", "enabled": false }
                        }
                    }
                }
            );
        }
    }

    /**
     * Add message with vector embedding
     */
    function add( required any message ) {
        // Normalize message to struct
        var msg = isSimpleValue( arguments.message )
            ? { role: "user", content: arguments.message }
            : arguments.message;

        // Generate embedding for content
        var embedding = generateEmbedding( msg.content );

        // Create document
        var doc = {
            "id": createUUID(),
            "key": variables.key,
            "role": msg.role,
            "content": msg.content,
            "embedding": embedding,
            "timestamp": now(),
            "metadata": msg.metadata ?: {}
        };

        // Index document
        esRequest(
            method: "POST",
            endpoint: "/#variables.indexName#/_doc",
            body: doc
        );

        return this;
    }

    /**
     * Get semantically relevant messages using vector similarity
     */
    function getRelevant( required string query, numeric limit = 5 ) {
        // Generate embedding for query
        var queryEmbedding = generateEmbedding( arguments.query );

        // Search using k-nearest neighbors
        var response = esRequest(
            method: "POST",
            endpoint: "/#variables.indexName#/_search",
            body: {
                "query": {
                    "bool": {
                        "must": [
                            {
                                "knn": {
                                    "embedding": {
                                        "vector": queryEmbedding,
                                        "k": arguments.limit
                                    }
                                }
                            },
                            {
                                "term": { "key": variables.key }
                            }
                        ]
                    }
                },
                "size": arguments.limit,
                "_source": [ "role", "content", "timestamp", "metadata" ]
            }
        );

        // Transform results
        var results = [];
        if ( response.hits?.hits.len() ) {
            response.hits.hits.each( hit => {
                results.append({
                    role: hit._source.role,
                    content: hit._source.content,
                    score: hit._score,
                    timestamp: hit._source.timestamp,
                    metadata: hit._source.metadata
                });
            });
        }

        return results;
    }

    /**
     * Get all messages for this key
     */
    function getAll() {
        var response = esRequest(
            method: "POST",
            endpoint: "/#variables.indexName#/_search",
            body: {
                "query": {
                    "term": { "key": variables.key }
                },
                "sort": [ { "timestamp": "asc" } ],
                "size": 10000,
                "_source": [ "role", "content", "timestamp", "metadata" ]
            }
        );

        var messages = [];
        if ( response.hits?.hits.len() ) {
            response.hits.hits.each( hit => {
                messages.append({
                    role: hit._source.role,
                    content: hit._source.content,
                    timestamp: hit._source.timestamp,
                    metadata: hit._source.metadata
                });
            });
        }

        return messages;
    }

    /**
     * Clear all messages for this key
     */
    function clear() {
        esRequest(
            method: "POST",
            endpoint: "/#variables.indexName#/_delete_by_query",
            body: {
                "query": {
                    "term": { "key": variables.key }
                }
            }
        );
        return this;
    }

    /**
     * Count messages for this key
     */
    function count() {
        var response = esRequest(
            method: "GET",
            endpoint: "/#variables.indexName#/_count",
            body: {
                "query": {
                    "term": { "key": variables.key }
                }
            }
        );
        return response.count ?: 0;
    }

    /**
     * Make HTTP request to Elasticsearch
     */
    private function esRequest(
        required string method,
        required string endpoint,
        struct body = {}
    ) {
        var url = variables.esClient.baseURL & arguments.endpoint;

        var httpRequest = httpRequest( url )
            .setMethod( arguments.method )
            .addHeader( "Content-Type", "application/json" );

        // Add authentication if configured
        if ( variables.esClient.user.len() ) {
            var auth = toBase64( "#variables.esClient.user#:#variables.esClient.password#" );
            httpRequest.addHeader( "Authorization", "Basic #auth#" );
        }

        // Add body for non-GET/HEAD requests
        if ( ![ "GET", "HEAD" ].findNoCase( arguments.method ) && !arguments.body.isEmpty() ) {
            httpRequest.setBody( jsonSerialize( arguments.body ) );
        }

        var response = httpRequest.send();

        // Parse JSON response
        return response.getStatusCode() == 200 || response.getStatusCode() == 201
            ? jsonDeserialize( response.getFileContent() )
            : {};
    }

    /**
     * Map our metric names to Elasticsearch similarity functions
     */
    private function mapMetricToES( required string metric ) {
        return {
            "cosine": "cosine",
            "euclidean": "l2_norm",
            "dot": "dot_product"
        }[ arguments.metric ] ?: "cosine";
    }

    /**
     * Generate embedding using configured provider
     */
    private function generateEmbedding( required string text ) {
        // Use aiEmbed() BIF
        var result = aiEmbed(
            input: arguments.text,
            provider: variables.embeddingProvider,
            model: variables.embeddingModel
        );

        return result.embeddings[ 1 ];
    }
}
```

### Usage Example

```boxlang
// Create Elasticsearch vector memory
memory = new ElasticsearchVectorMemory().configure({
    collection: "customer_support",
    esHost: "localhost",
    esPort: 9200,
    esUser: "elastic",
    esPassword: "password",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small",
    dimensions: 1536,
    metric: "cosine"
});

// Use with agent
agent = aiAgent(
    name: "SupportBot",
    memory: memory
);
```

## Example 2: RedisVectorMemory

Implementation using Redis with RediSearch vector similarity:

```boxlang
/**
 * Redis Vector Memory
 * Uses Redis Stack with vector similarity search (RediSearch)
 */
class extends="BaseVectorMemory" implements="IVectorMemory" {

    /**
     * Configure Redis connection
     */
    function configure( required struct config ) {
        super.configure( arguments.config );

        variables.redisHost = arguments.config.redisHost ?: "localhost";
        variables.redisPort = arguments.config.redisPort ?: 6379;
        variables.redisPassword = arguments.config.redisPassword ?: "";
        variables.indexName = "idx:#arguments.config.collection ?: 'ai_memory'#";

        // Initialize Redis connection
        initializeRedis();

        // Create index if needed
        ensureIndex();

        return this;
    }

    /**
     * Initialize Redis connection
     */
    private function initializeRedis() {
        variables.redisURL = "redis://#variables.redisHost#:#variables.redisPort#";

        // Test connection
        var response = redisCommand( "PING" );
        if ( response != "PONG" ) {
            throw( type="RedisConnectionException", message="Failed to connect to Redis" );
        }
    }

    /**
     * Ensure search index exists
     */
    private function ensureIndex() {
        // Check if index exists
        try {
            redisCommand( "FT.INFO", [ variables.indexName ] );
        } catch ( any e ) {
            // Create index with vector field
            redisCommand( "FT.CREATE", [
                variables.indexName,
                "ON", "HASH",
                "PREFIX", "1", "msg:#variables.key#:",
                "SCHEMA",
                "role", "TAG",
                "content", "TEXT",
                "embedding", "VECTOR", "HNSW", "6",
                    "TYPE", "FLOAT32",
                    "DIM", variables.dimensions,
                    "DISTANCE_METRIC", mapMetricToRedis( variables.metric ),
                "timestamp", "NUMERIC", "SORTABLE"
            ] );
        }
    }

    /**
     * Add message with embedding
     */
    function add( required any message ) {
        var msg = isSimpleValue( arguments.message )
            ? { role: "user", content: arguments.message }
            : arguments.message;

        var embedding = generateEmbedding( msg.content );
        var msgId = "msg:#variables.key#:#createUUID()#";

        // Store as Redis hash
        redisCommand( "HSET", [
            msgId,
            "role", msg.role,
            "content", msg.content,
            "embedding", serializeVector( embedding ),
            "timestamp", getTickCount(),
            "metadata", jsonSerialize( msg.metadata ?: {} )
        ] );

        return this;
    }

    /**
     * Get relevant messages using vector search
     */
    function getRelevant( required string query, numeric limit = 5 ) {
        var queryEmbedding = generateEmbedding( arguments.query );
        var vectorBlob = serializeVector( queryEmbedding );

        // Search using vector similarity
        var results = redisCommand( "FT.SEARCH", [
            variables.indexName,
            "*=>[KNN #arguments.limit# @embedding $vector AS score]",
            "PARAMS", "2", "vector", vectorBlob,
            "SORTBY", "score",
            "DIALECT", "2",
            "RETURN", "4", "role", "content", "timestamp", "score"
        ] );

        return parseRedisResults( results );
    }

    /**
     * Get all messages
     */
    function getAll() {
        var pattern = "msg:#variables.key#:*";
        var keys = redisCommand( "KEYS", [ pattern ] );

        var messages = [];
        keys.each( key => {
            var data = redisCommand( "HGETALL", [ key ] );
            messages.append( parseHashData( data ) );
        } );

        // Sort by timestamp
        messages.sort( ( a, b ) => compare( a.timestamp, b.timestamp ) );

        return messages;
    }

    /**
     * Clear all messages
     */
    function clear() {
        var pattern = "msg:#variables.key#:*";
        var keys = redisCommand( "KEYS", [ pattern ] );

        if ( keys.len() ) {
            redisCommand( "DEL", keys );
        }

        return this;
    }

    /**
     * Count messages
     */
    function count() {
        var pattern = "msg:#variables.key#:*";
        var keys = redisCommand( "KEYS", [ pattern ] );
        return keys.len();
    }

    /**
     * Execute Redis command via HTTP API or native client
     */
    private function redisCommand( required string command, array args = [] ) {
        // Simplified: Use HTTP API or native Redis client
        // This would use actual Redis client library in production

        var cmdArray = [ arguments.command ];
        cmdArray.append( arguments.args, true );

        // Implementation would call actual Redis client
        // For example: redisClient.execute( cmdArray )

        return executeRedisCommand( cmdArray );
    }

    /**
     * Serialize vector for Redis storage
     */
    private function serializeVector( required array vector ) {
        // Convert array of floats to binary blob
        var buffer = createObject( "java", "java.nio.ByteBuffer" )
            .allocate( arguments.vector.len() * 4 );

        arguments.vector.each( val => {
            buffer.putFloat( val castAs float );
        } );

        return buffer.array();
    }

    /**
     * Map metric to Redis format
     */
    private function mapMetricToRedis( required string metric ) {
        return {
            "cosine": "COSINE",
            "euclidean": "L2",
            "dot": "IP"
        }[ arguments.metric ] ?: "COSINE";
    }

    /**
     * Parse Redis search results
     */
    private function parseRedisResults( required array results ) {
        var messages = [];
        // Redis returns: [count, key1, [field1, value1, ...], key2, ...]
        var count = results[ 1 ];

        for ( var i = 2; i <= results.len(); i += 2 ) {
            var fields = results[ i + 1 ];
            var msg = {};

            for ( var j = 1; j <= fields.len(); j += 2 ) {
                msg[ fields[ j ] ] = fields[ j + 1 ];
            }

            messages.append( msg );
        }

        return messages;
    }

    /**
     * Generate embedding
     */
    private function generateEmbedding( required string text ) {
        var result = aiEmbed(
            input: arguments.text,
            provider: variables.embeddingProvider,
            model: variables.embeddingModel
        );
        return result.embeddings[ 1 ];
    }
}
```

## Example 3: CachedVectorMemory

A wrapper that adds caching layer to any vector memory:

```boxlang
/**
 * Cached Vector Memory Wrapper
 * Adds intelligent caching to reduce embedding API calls
 */
class extends="BaseVectorMemory" implements="IVectorMemory" {

    property name="wrappedMemory" type="any";
    property name="embeddingCache" type="struct";
    property name="resultCache" type="struct";

    /**
     * Configure with wrapped memory
     */
    function configure( required struct config ) {
        super.configure( arguments.config );

        // Wrapped memory instance
        if ( !arguments.config.keyExists( "wrappedMemory" ) ) {
            throw( "CachedVectorMemory requires 'wrappedMemory' in config" );
        }

        variables.wrappedMemory = arguments.config.wrappedMemory;

        // Cache configuration
        variables.embeddingCacheTTL = arguments.config.embeddingCacheTTL ?: 3600; // 1 hour
        variables.resultCacheTTL = arguments.config.resultCacheTTL ?: 300; // 5 minutes
        variables.maxCacheSize = arguments.config.maxCacheSize ?: 1000;

        // Initialize caches
        variables.embeddingCache = {};
        variables.resultCache = {};

        return this;
    }

    /**
     * Add message (cache embedding)
     */
    function add( required any message ) {
        var msg = isSimpleValue( arguments.message )
            ? { role: "user", content: arguments.message }
            : arguments.message;

        // Check if we've already embedded this exact content
        var cacheKey = hash( msg.content, "MD5" );

        if ( !variables.embeddingCache.keyExists( cacheKey ) ) {
            // Not cached, will generate embedding
            println( "Cache MISS: Generating new embedding" );
        } else {
            println( "Cache HIT: Reusing cached embedding" );
            // Could inject cached embedding into wrapped memory here
        }

        // Delegate to wrapped memory
        variables.wrappedMemory.add( arguments.message );

        // Invalidate result cache
        variables.resultCache = {};

        return this;
    }

    /**
     * Get relevant with result caching
     */
    function getRelevant( required string query, numeric limit = 5 ) {
        // Create cache key from query + limit
        var cacheKey = hash( arguments.query & arguments.limit, "MD5" );

        // Check result cache
        if ( variables.resultCache.keyExists( cacheKey ) ) {
            var cached = variables.resultCache[ cacheKey ];

            // Check if still valid
            if ( getTickCount() - cached.timestamp < variables.resultCacheTTL * 1000 ) {
                println( "Result cache HIT for query: '#left(arguments.query, 30)#...'" );
                return cached.results;
            } else {
                println( "Result cache EXPIRED" );
                variables.resultCache.delete( cacheKey );
            }
        }

        println( "Result cache MISS: Executing search" );

        // Execute search
        var results = variables.wrappedMemory.getRelevant(
            query: arguments.query,
            limit: arguments.limit
        );

        // Cache results
        variables.resultCache[ cacheKey ] = {
            results: results,
            timestamp: getTickCount()
        };

        // Enforce max cache size
        if ( variables.resultCache.count() > variables.maxCacheSize ) {
            pruneCache( variables.resultCache );
        }

        return results;
    }

    /**
     * Delegate getAll to wrapped memory
     */
    function getAll() {
        return variables.wrappedMemory.getAll();
    }

    /**
     * Clear wrapped memory and caches
     */
    function clear() {
        variables.wrappedMemory.clear();
        variables.embeddingCache = {};
        variables.resultCache = {};
        return this;
    }

    /**
     * Delegate count to wrapped memory
     */
    function count() {
        return variables.wrappedMemory.count();
    }

    /**
     * Prune cache to max size (LRU)
     */
    private function pruneCache( required struct cache ) {
        // Simple pruning: remove oldest entries
        var entries = [];

        for ( var key in arguments.cache ) {
            entries.append({
                key: key,
                timestamp: arguments.cache[ key ].timestamp
            });
        }

        // Sort by timestamp
        entries.sort( ( a, b ) => compare( a.timestamp, b.timestamp ) );

        // Remove oldest 20%
        var removeCount = ceiling( entries.len() * 0.2 );
        for ( var i = 1; i <= removeCount; i++ ) {
            arguments.cache.delete( entries[ i ].key );
        }
    }
}
```

### Usage Example

```boxlang
// Wrap any vector memory with caching
baseMemory = aiMemory( "pinecone", {
    apiKey: getSystemSetting( "PINECONE_API_KEY" ),
    collection: "support_kb",
    embeddingProvider: "openai",
    embeddingModel: "text-embedding-3-small"
} );

// Add caching layer
cachedMemory = new CachedVectorMemory().configure({
    wrappedMemory: baseMemory,
    embeddingCacheTTL: 7200,  // 2 hours
    resultCacheTTL: 600,       // 10 minutes
    maxCacheSize: 500
});

// Use cached memory with agent
agent = aiAgent(
    name: "CachedBot",
    memory: cachedMemory
);
```

## Example 4: MultiCollectionVectorMemory

Search across multiple collections with custom ranking:

```boxlang
/**
 * Multi-Collection Vector Memory
 * Searches multiple collections and merges results intelligently
 */
class extends="BaseVectorMemory" implements="IVectorMemory" {

    property name="collections" type="array";
    property name="collectionWeights" type="struct";

    /**
     * Configure multiple collections
     */
    function configure( required struct config ) {
        super.configure( arguments.config );

        // Multiple collection configurations
        if ( !arguments.config.keyExists( "collections" ) ) {
            throw( "MultiCollectionVectorMemory requires 'collections' array" );
        }

        variables.collections = [];
        variables.collectionWeights = arguments.config.collectionWeights ?: {};

        // Initialize each collection memory
        arguments.config.collections.each( collectionConfig => {
            var memory = aiMemory( "boxvector", collectionConfig );
            variables.collections.append({
                name: collectionConfig.collection,
                memory: memory,
                weight: variables.collectionWeights[ collectionConfig.collection ] ?: 1.0
            });
        } );

        return this;
    }

    /**
     * Add to all collections
     */
    function add( required any message ) {
        variables.collections.each( coll => {
            coll.memory.add( arguments.message );
        } );
        return this;
    }

    /**
     * Search all collections and merge results
     */
    function getRelevant( required string query, numeric limit = 5 ) {
        var allResults = [];

        // Search each collection
        variables.collections.each( coll => {
            var results = coll.memory.getRelevant(
                query: arguments.query,
                limit: arguments.limit
            );

            // Add collection metadata and apply weight
            results.each( result => {
                result.collection = coll.name;
                result.originalScore = result.score;
                result.score = result.score * coll.weight;
                allResults.append( result );
            } );
        } );

        // Sort by weighted score
        allResults.sort( ( a, b ) => {
            return a.score > b.score ? -1 : 1;
        } );

        // Return top N results
        return allResults.slice( 1, min( arguments.limit, allResults.len() ) );
    }

    /**
     * Get all from all collections
     */
    function getAll() {
        var allMessages = [];

        variables.collections.each( coll => {
            var messages = coll.memory.getAll();
            messages.each( msg => {
                msg.collection = coll.name;
                allMessages.append( msg );
            } );
        } );

        return allMessages;
    }

    /**
     * Clear all collections
     */
    function clear() {
        variables.collections.each( coll => {
            coll.memory.clear();
        } );
        return this;
    }

    /**
     * Count across all collections
     */
    function count() {
        var total = 0;
        variables.collections.each( coll => {
            total += coll.memory.count();
        } );
        return total;
    }
}
```

### Usage Example

```boxlang
// Create multi-collection memory
memory = new MultiCollectionVectorMemory().configure({
    collections: [
        {
            collection: "product_docs",
            embeddingProvider: "openai",
            embeddingModel: "text-embedding-3-small",
            dimensions: 1536
        },
        {
            collection: "support_tickets",
            embeddingProvider: "openai",
            embeddingModel: "text-embedding-3-small",
            dimensions: 1536
        },
        {
            collection: "faq_database",
            embeddingProvider: "openai",
            embeddingModel: "text-embedding-3-small",
            dimensions: 1536
        }
    ],
    collectionWeights: {
        "product_docs": 1.5,      // Prioritize product docs
        "support_tickets": 1.0,   // Standard priority
        "faq_database": 0.8       // Lower priority
    }
});

// Agent searches all collections
agent = aiAgent(
    name: "MultiSourceBot",
    memory: memory
);
```

## Testing Your Custom Vector Memory

### Unit Test Example

```boxlang
component extends="testbox.system.BaseSpec" {

    function run() {
        describe( "ElasticsearchVectorMemory", () => {

            beforeEach( () => {
                variables.memory = new ElasticsearchVectorMemory().configure({
                    collection: "test_memory",
                    esHost: "localhost",
                    esPort: 9200,
                    embeddingProvider: "openai",
                    embeddingModel: "text-embedding-3-small",
                    dimensions: 1536,
                    metric: "cosine"
                });
                variables.memory.key( "test-session-#createUUID()#" );
            });

            afterEach( () => {
                variables.memory.clear();
            });

            it( "can add and retrieve messages", () => {
                // Add messages
                memory.add( "BoxLang is a modern JVM language" );
                memory.add( "It has excellent Java interop" );
                memory.add( "The weather is sunny today" );

                expect( memory.count() ).toBe( 3 );

                // Search for relevant content
                var results = memory.getRelevant( "Tell me about BoxLang", 2 );

                expect( results ).toBeArray();
                expect( results.len() ).toBeGTE( 1 );
                expect( results[ 1 ].content ).toInclude( "BoxLang" );
            });

            it( "returns results sorted by relevance", () => {
                memory.add( "Python is a programming language" );
                memory.add( "BoxLang is built on the JVM" );
                memory.add( "Java runs on the JVM" );

                var results = memory.getRelevant( "JVM languages", 3 );

                // First result should be most relevant
                expect( results[ 1 ].score ).toBeGT( results[ 2 ].score );
            });

            it( "can clear all messages", () => {
                memory.add( "Test message 1" );
                memory.add( "Test message 2" );

                expect( memory.count() ).toBe( 2 );

                memory.clear();

                expect( memory.count() ).toBe( 0 );
            });

            it( "isolates messages by key", () => {
                memory.key( "session-1" );
                memory.add( "Message in session 1" );

                memory.key( "session-2" );
                memory.add( "Message in session 2" );

                memory.key( "session-1" );
                var session1Messages = memory.getAll();

                expect( session1Messages.len() ).toBe( 1 );
                expect( session1Messages[ 1 ].content ).toBe( "Message in session 1" );
            });

        });
    }
}
```

## Best Practices

### 1. Always Call `super.configure()`

```boxlang
function configure( required struct config ) {
    // CRITICAL: Call parent configuration first
    super.configure( arguments.config );

    // Then add your custom configuration
    variables.myCustomSetting = arguments.config.myCustomSetting;

    return this;
}
```

### 2. Validate Configuration

```boxlang
function configure( required struct config ) {
    super.configure( arguments.config );

    // Validate required fields
    if ( !arguments.config.keyExists( "apiEndpoint" ) ) {
        throw(
            type: "ConfigurationException",
            message: "CustomVectorMemory requires 'apiEndpoint' in configuration"
        );
    }

    // Validate dimensions match embedding model
    if ( variables.dimensions != getModelDimensions( variables.embeddingModel ) ) {
        throw(
            type: "ConfigurationException",
            message: "Dimensions (#variables.dimensions#) don't match model (#variables.embeddingModel#)"
        );
    }

    return this;
}
```

### 3. Handle Errors Gracefully

```boxlang
function getRelevant( required string query, numeric limit = 5 ) {
    try {
        // Attempt vector search
        var embedding = generateEmbedding( arguments.query );
        return searchVectors( embedding, arguments.limit );

    } catch ( "EmbeddingException" e ) {
        // Log error and return empty results
        writeLog(
            type: "error",
            file: "vector-memory",
            text: "Failed to generate embedding: #e.message#"
        );
        return [];

    } catch ( "VectorSearchException" e ) {
        // Fallback to text search if vector search fails
        writeLog(
            type: "warning",
            file: "vector-memory",
            text: "Vector search failed, using fallback: #e.message#"
        );
        return fallbackTextSearch( arguments.query, arguments.limit );
    }
}
```

### 4. Optimize Embedding Generation

```boxlang
/**
 * Generate embeddings with batching and caching
 */
private function generateEmbeddings( required array texts ) {
    // Batch embed for efficiency
    if ( arguments.texts.len() > 1 ) {
        var result = aiEmbed(
            input: arguments.texts,  // Batch input
            provider: variables.embeddingProvider,
            model: variables.embeddingModel
        );
        return result.embeddings;
    } else {
        var result = aiEmbed(
            input: arguments.texts[ 1 ],
            provider: variables.embeddingProvider,
            model: variables.embeddingModel
        );
        return [ result.embeddings[ 1 ] ];
    }
}
```

### 5. Implement Proper Export/Import

```boxlang
function export() {
    return {
        "type": "custom",
        "class": getMetadata( this ).name,
        "key": variables.key,
        "config": {
            "collection": variables.collection,
            "embeddingProvider": variables.embeddingProvider,
            "embeddingModel": variables.embeddingModel,
            "dimensions": variables.dimensions,
            "metric": variables.metric
        },
        "messages": getAll(),
        "metadata": {
            "count": count(),
            "exportedAt": now()
        }
    };
}

function import( required struct data ) {
    // Validate import data
    if ( !arguments.data.keyExists( "messages" ) ) {
        throw( "Import data must contain 'messages' array" );
    }

    // Clear existing data
    clear();

    // Import messages
    arguments.data.messages.each( msg => {
        add( msg );
    } );

    return this;
}
```

### 6. Monitor Performance

```boxlang
function getRelevant( required string query, numeric limit = 5 ) {
    var startTime = getTickCount();

    try {
        var results = performVectorSearch( arguments.query, arguments.limit );

        var duration = getTickCount() - startTime;

        // Log slow queries
        if ( duration > 1000 ) {  // > 1 second
            writeLog(
                type: "warning",
                file: "vector-memory",
                text: "Slow vector search (#duration#ms) for query: #left(arguments.query, 50)#"
            );
        }

        return results;

    } catch ( any e ) {
        writeLog(
            type: "error",
            file: "vector-memory",
            text: "Vector search failed after #getTickCount() - startTime#ms: #e.message#"
        );
        rethrow;
    }
}
```

### 7. Support Metadata Filtering

```boxlang
function getRelevant(
    required string query,
    numeric limit = 5,
    struct filters = {}
) {
    var embedding = generateEmbedding( arguments.query );

    // Build query with metadata filters
    var searchQuery = {
        "vector": embedding,
        "limit": arguments.limit
    };

    // Add metadata filters
    if ( !arguments.filters.isEmpty() ) {
        searchQuery.filters = arguments.filters;
    }

    return executeSearch( searchQuery );
}
```

## Common Patterns

### Pattern 1: Wrapper Pattern

Wrap existing memory to add functionality:

```boxlang
class extends="BaseVectorMemory" {
    property name="wrappedMemory";

    function configure( required struct config ) {
        super.configure( arguments.config );
        variables.wrappedMemory = arguments.config.wrappedMemory;
        return this;
    }

    // Add functionality before/after delegation
    function add( required any message ) {
        preProcess( arguments.message );
        variables.wrappedMemory.add( arguments.message );
        postProcess( arguments.message );
        return this;
    }
}
```

### Pattern 2: Adapter Pattern

Adapt existing clients to IVectorMemory interface:

```boxlang
class extends="BaseVectorMemory" {
    property name="thirdPartyClient";

    function configure( required struct config ) {
        super.configure( arguments.config );
        variables.thirdPartyClient = createThirdPartyClient( arguments.config );
        return this;
    }

    function add( required any message ) {
        // Adapt to third-party API
        variables.thirdPartyClient.insertDocument(
            collection: variables.collection,
            document: adaptMessage( arguments.message )
        );
        return this;
    }
}
```

### Pattern 3: Composite Pattern

Combine multiple vector memories:

```boxlang
class extends="BaseVectorMemory" {
    property name="memories" type="array";

    function configure( required struct config ) {
        super.configure( arguments.config );
        variables.memories = arguments.config.memories ?: [];
        return this;
    }

    function add( required any message ) {
        variables.memories.each( memory => {
            memory.add( arguments.message );
        } );
        return this;
    }

    function getRelevant( required string query, numeric limit = 5 ) {
        var allResults = [];

        variables.memories.each( memory => {
            allResults.append( memory.getRelevant( arguments.query, arguments.limit ), true );
        } );

        return mergeAndRank( allResults, arguments.limit );
    }
}
```

## Related Documentation

- [Vector Memory Overview](../main-components/vector-memory.md) - Learn about built-in vector memory types
- [Custom Memory](custom-memory.md) - Create custom standard memory implementations
- [Memory Systems](../main-components/memory.md) - Understanding memory in BoxLang AI
- [Embeddings](embeddings.md) - Working with vector embeddings

## Next Steps

1. **Start Simple**: Begin with BoxVector or extend an existing provider
2. **Test Thoroughly**: Write comprehensive unit tests
3. **Monitor Performance**: Track query times and cache hit rates
4. **Optimize**: Add caching, batching, and connection pooling
5. **Document**: Provide clear usage examples and configuration options

## Need Help?

- **Community**: [BoxLang Discord](https://discord.gg/boxlang)
- **Documentation**: [BoxLang AI Docs](https://github.com/ortus-boxlang/bx-ai)
- **Examples**: See `/examples/vector-memory/` for working examples
