---
description: "Learn how to build custom memory implementations by extending BaseMemory for specialized storage and retrieval requirements."
icon: puzzle-piece
---

# Building Custom Memory

This guide shows you how to create custom memory implementations for specialized requirements not covered by the built-in memory types. You'll learn to extend `BaseMemory` and implement the `IAiMemory` interface.

---

## Table of Contents

- [When to Build Custom Memory](#when-to-build-custom-memory)
- [Understanding BaseMemory](#understanding-basememory)
- [IAiMemory Interface](#iaimemory-interface)
- [Creating a Custom Memory](#creating-a-custom-memory)
- [Advanced Examples](#advanced-examples)
- [Testing Your Memory](#testing-your-memory)
- [Best Practices](#best-practices)

---

## When to Build Custom Memory

Consider building custom memory when you need:

- **Custom Storage Backend**: MongoDB, DynamoDB, Elasticsearch, etc.
- **Specialized Logic**: Custom message filtering, transformation, or routing
- **Integration Requirements**: Connect with existing systems or APIs
- **Performance Optimization**: Specialized caching or indexing strategies
- **Domain-Specific Behavior**: Industry-specific compliance or workflows

**Use Built-in Memory If:**
- Standard storage needs (file, database, cache, session)
- Common vector search requirements
- No custom logic needed

---

## Understanding BaseMemory

`BaseMemory` provides the foundation for all memory implementations:

### Provided Features

- Message storage in `variables.messages` array
- Unique key management with `key()` method
- Metadata storage with `metadata()` method
- Message validation and normalization
- Export/import functionality
- Event announcements

### What You Implement

- `configure()` - Initialize your custom storage
- `add()` - Store messages (optional override)
- `getAll()` - Retrieve messages
- `clear()` - Remove all messages
- Any custom methods specific to your needs

### Key Properties

```boxlang
property name="key" type="string";           // Unique identifier
property name="metadata" type="struct";      // Custom metadata
property name="messages" type="array";       // Message storage
property name="maxMessages" type="numeric";  // Message limit
property name="config" type="struct";        // Configuration
```

---

## IAiMemory Interface

The `IAiMemory` interface defines the contract all memory implementations must follow:

### Required Methods

```boxlang
/**
 * Configure the memory instance
 * @config Configuration struct
 * @return IAiMemory for chaining
 */
IAiMemory function configure( required struct config );

/**
 * Add a message to memory
 * @message String, struct, array, or AiMessage instance
 * @return IAiMemory for chaining
 */
IAiMemory function add( required any message );

/**
 * Get all messages from memory
 * @return Array of message structs
 */
array function getAll();

/**
 * Clear all messages from memory
 * @return IAiMemory for chaining
 */
IAiMemory function clear();

/**
 * Get or set the unique key
 * @key Optional key to set
 * @return String (getter) or IAiMemory (setter)
 */
any function key( string key );

/**
 * Get or set metadata
 * @metadata Optional metadata to set
 * @return Struct (getter) or IAiMemory (setter)
 */
any function metadata( struct metadata );

/**
 * Export memory state
 * @return Struct with messages, metadata, config
 */
struct function export();
```

---

## Creating a Custom Memory

### Example 1: Redis Memory

Store messages in Redis for distributed applications:

```boxlang
/**
 * Redis-backed Memory Implementation
 * Stores conversation history in Redis for distributed access
 */
import bxModules.bxai.models.memory.BaseMemory;

class extends="BaseMemory" {

    property name="redisHost" type="string" default="localhost";
    property name="redisPort" type="numeric" default=6379;
    property name="redisPassword" type="string" default="";
    property name="keyPrefix" type="string" default="ai:memory:";
    property name="ttl" type="numeric" default=3600;  // 1 hour default

    /**
     * Configure Redis connection and settings
     */
    IAiMemory function configure( required struct config ) {
        super.configure( arguments.config );

        // Extract Redis configuration
        if ( arguments.config.keyExists( "redisHost" ) ) {
            variables.redisHost = arguments.config.redisHost;
        }
        if ( arguments.config.keyExists( "redisPort" ) ) {
            variables.redisPort = arguments.config.redisPort;
        }
        if ( arguments.config.keyExists( "redisPassword" ) ) {
            variables.redisPassword = arguments.config.redisPassword;
        }
        if ( arguments.config.keyExists( "keyPrefix" ) ) {
            variables.keyPrefix = arguments.config.keyPrefix;
        }
        if ( arguments.config.keyExists( "ttl" ) ) {
            variables.ttl = arguments.config.ttl;
        }

        // Test connection
        testConnection();

        return this;
    }

    /**
     * Add message to Redis
     */
    IAiMemory function add( required any message ) {
        // Let BaseMemory handle validation and normalization
        super.add( arguments.message );

        // Save to Redis
        saveToRedis();

        return this;
    }

    /**
     * Get all messages from Redis
     */
    array function getAll() {
        return loadFromRedis();
    }

    /**
     * Clear all messages from Redis
     */
    IAiMemory function clear() {
        var redisKey = variables.keyPrefix & variables.key;

        httpRequest( buildRedisUrl( "del", redisKey ) )
            .setMethod( "POST" )
            .send();

        variables.messages = [];

        return this;
    }

    /**
     * Private: Save messages to Redis
     */
    private function saveToRedis() {
        var redisKey = variables.keyPrefix & variables.key;
        var data = jsonSerialize( variables.messages );

        httpRequest( buildRedisUrl( "setex", redisKey ) )
            .setMethod( "POST" )
            .setBody( jsonSerialize({
                key: redisKey,
                seconds: variables.ttl,
                value: data
            }) )
            .send();
    }

    /**
     * Private: Load messages from Redis
     */
    private array function loadFromRedis() {
        var redisKey = variables.keyPrefix & variables.key;

        var response = httpRequest( buildRedisUrl( "get", redisKey ) )
            .setMethod( "GET" )
            .send();

        if ( response.statusCode == 200 && response.fileContent.len() ) {
            var data = jsonDeserialize( response.fileContent );
            if ( !isNull( data ) && isArray( data ) ) {
                return data;
            }
        }

        return [];
    }

    /**
     * Private: Build Redis HTTP URL
     */
    private string function buildRedisUrl( required string command, string key = "" ) {
        var url = "http://#variables.redisHost#:#variables.redisPort#/#arguments.command#";
        if ( arguments.key.len() ) {
            url &= "/#arguments.key#";
        }
        return url;
    }

    /**
     * Private: Test Redis connection
     */
    private function testConnection() {
        try {
            httpRequest( buildRedisUrl( "ping" ) )
                .setTimeout( 5 )
                .setMethod( "GET" )
                .send();
        } catch( any e ) {
            throw(
                type: "RedisConnectionError",
                message: "Could not connect to Redis at #variables.redisHost#:#variables.redisPort#",
                detail: e.message
            );
        }
    }
}
```

**Usage:**

```boxlang
memory = new RedisMemory().configure({
    redisHost: "localhost",
    redisPort: 6379,
    redisPassword: "",
    ttl: 7200,  // 2 hours
    maxMessages: 50
})

agent = aiAgent(
    name: "Distributed Bot",
    memory: memory
)
```

---

### Example 2: Priority Memory

Automatically prioritize and filter messages based on importance:

```boxlang
/**
 * Priority-based Memory
 * Automatically filters messages based on priority levels
 */
import bxModules.bxai.models.memory.BaseMemory;

class extends="BaseMemory" {

    property name="minPriority" type="numeric" default=0;
    property name="priorityField" type="string" default="priority";

    static {
        PRIORITY_LEVELS = {
            CRITICAL: 100,
            HIGH: 75,
            MEDIUM: 50,
            LOW: 25,
            DEBUG: 0
        }
    }

    /**
     * Configure priority settings
     */
    IAiMemory function configure( required struct config ) {
        super.configure( arguments.config );

        if ( arguments.config.keyExists( "minPriority" ) ) {
            variables.minPriority = arguments.config.minPriority;
        }
        if ( arguments.config.keyExists( "priorityField" ) ) {
            variables.priorityField = arguments.config.priorityField;
        }

        return this;
    }

    /**
     * Add message with automatic priority detection
     */
    IAiMemory function add( required any message ) {
        // Normalize to struct
        var msg = normalizeMessage( arguments.message );

        // Auto-detect priority if not set
        if ( !msg.keyExists( variables.priorityField ) ) {
            msg[ variables.priorityField ] = detectPriority( msg.content );
        }

        // Only add if meets minimum priority
        if ( msg[ variables.priorityField ] >= variables.minPriority ) {
            variables.messages.append( msg );

            // Trim if needed
            if ( variables.maxMessages > 0 && variables.messages.len() > variables.maxMessages ) {
                trimByPriority();
            }
        }

        return this;
    }

    /**
     * Get messages above priority threshold
     */
    array function getAll() {
        return variables.messages.filter( function( msg ) {
            return msg.keyExists( variables.priorityField ) &&
                   msg[ variables.priorityField ] >= variables.minPriority;
        } );
    }

    /**
     * Get messages at specific priority level
     */
    array function getByPriority( required numeric priority ) {
        return variables.messages.filter( function( msg ) {
            return msg.keyExists( variables.priorityField ) &&
                   msg[ variables.priorityField ] == arguments.priority;
        } );
    }

    /**
     * Private: Normalize message to struct
     */
    private struct function normalizeMessage( required any message ) {
        if ( isSimpleValue( arguments.message ) ) {
            return {
                role: "user",
                content: arguments.message,
                timestamp: now()
            };
        } else if ( isStruct( arguments.message ) ) {
            param arguments.message.timestamp = now();
            return arguments.message;
        }
        throw( type: "InvalidMessage", message: "Invalid message type" );
    }

    /**
     * Private: Auto-detect priority from content
     */
    private numeric function detectPriority( required string content ) {
        var text = arguments.content.lcase();

        // Critical keywords
        if ( text.findNoCase( "error" ) || text.findNoCase( "critical" ) || text.findNoCase( "urgent" ) ) {
            return static.PRIORITY_LEVELS.CRITICAL;
        }

        // High priority keywords
        if ( text.findNoCase( "important" ) || text.findNoCase( "warning" ) ) {
            return static.PRIORITY_LEVELS.HIGH;
        }

        // Default to medium
        return static.PRIORITY_LEVELS.MEDIUM;
    }

    /**
     * Private: Trim messages keeping highest priority
     */
    private function trimByPriority() {
        // Sort by priority (descending)
        variables.messages.sort( function( a, b ) {
            var aPriority = a.keyExists( variables.priorityField ) ? a[ variables.priorityField ] : 0;
            var bPriority = b.keyExists( variables.priorityField ) ? b[ variables.priorityField ] : 0;
            return bPriority - aPriority;  // Descending
        } );

        // Keep only max messages
        variables.messages = variables.messages.slice( 1, variables.maxMessages );
    }
}
```

**Usage:**

```boxlang
memory = new PriorityMemory().configure({
    minPriority: 50,  // Only keep medium priority and above
    maxMessages: 20
})

// Critical messages always stored
memory.add({
    role: "user",
    content: "CRITICAL: System failure",
    priority: 100
})

// Low priority filtered out
memory.add({
    role: "user",
    content: "Debug information",
    priority: 10
})  // Won't be stored

messages = memory.getAll()  // Only critical/high/medium messages
```

---

### Example 3: Rotating Memory

Implements time-based rotation with archiving:

```boxlang
/**
 * Rotating Memory with Archive
 * Automatically rotates messages to archive based on time
 */
import bxModules.bxai.models.memory.BaseMemory;

class extends="BaseMemory" {

    property name="rotationHours" type="numeric" default=24;
    property name="archivePath" type="string" default="";
    property name="maxArchives" type="numeric" default=10;

    /**
     * Configure rotation settings
     */
    IAiMemory function configure( required struct config ) {
        super.configure( arguments.config );

        if ( arguments.config.keyExists( "rotationHours" ) ) {
            variables.rotationHours = arguments.config.rotationHours;
        }
        if ( arguments.config.keyExists( "archivePath" ) ) {
            variables.archivePath = arguments.config.archivePath;
        }
        if ( arguments.config.keyExists( "maxArchives" ) ) {
            variables.maxArchives = arguments.config.maxArchives;
        }

        // Ensure archive directory exists
        if ( variables.archivePath.len() && !directoryExists( variables.archivePath ) ) {
            directoryCreate( variables.archivePath );
        }

        return this;
    }

    /**
     * Add message with automatic rotation check
     */
    IAiMemory function add( required any message ) {
        // Check if rotation needed
        checkRotation();

        // Add message normally
        super.add( arguments.message );

        return this;
    }

    /**
     * Get all non-archived messages
     */
    array function getAll() {
        checkRotation();
        return variables.messages;
    }

    /**
     * Get archived messages
     */
    array function getArchived( string archiveFile = "" ) {
        if ( !variables.archivePath.len() ) {
            return [];
        }

        if ( arguments.archiveFile.len() ) {
            return loadArchive( arguments.archiveFile );
        }

        // Return all archives
        var archives = [];
        var files = directoryList( variables.archivePath, false, "name", "*.json" );

        files.each( function( file ) {
            archives.append({
                file: file,
                messages: loadArchive( file )
            });
        } );

        return archives;
    }

    /**
     * Private: Check if rotation needed
     */
    private function checkRotation() {
        if ( !variables.messages.len() ) {
            return;
        }

        // Check oldest message
        var oldest = variables.messages.first();
        if ( !oldest.keyExists( "timestamp" ) ) {
            return;
        }

        var cutoffTime = dateAdd( "h", -variables.rotationHours, now() );

        if ( oldest.timestamp < cutoffTime ) {
            rotateMessages();
        }
    }

    /**
     * Private: Rotate old messages to archive
     */
    private function rotateMessages() {
        if ( !variables.archivePath.len() ) {
            // No archive path, just clear old messages
            variables.messages = [];
            return;
        }

        // Archive old messages
        var archiveFile = variables.archivePath & "/archive_#dateFormat(now(), 'yyyy-mm-dd')#_#timeFormat(now(), 'HHnnss')#.json";
        fileWrite( archiveFile, jsonSerialize( variables.messages, true ) );

        // Clear current messages
        variables.messages = [];

        // Cleanup old archives
        cleanupOldArchives();
    }

    /**
     * Private: Remove old archives beyond maxArchives
     */
    private function cleanupOldArchives() {
        var files = directoryList( variables.archivePath, false, "query", "*.json" );

        if ( files.recordCount > variables.maxArchives ) {
            // Sort by date modified (oldest first)
            files = querySort( files, "dateLastModified" );

            // Delete oldest files
            for ( var i = 1; i <= files.recordCount - variables.maxArchives; i++ ) {
                fileDelete( files.directory[ i ] & "/" & files.name[ i ] );
            }
        }
    }

    /**
     * Private: Load archive file
     */
    private array function loadArchive( required string filename ) {
        var fullPath = variables.archivePath & "/" & arguments.filename;

        if ( !fileExists( fullPath ) ) {
            return [];
        }

        try {
            return jsonDeserialize( fileRead( fullPath ) );
        } catch( any e ) {
            writeLog( type: "error", log: "ai", text: "Failed to load archive: #e.message#" );
            return [];
        }
    }
}
```

**Usage:**

```boxlang
memory = new RotatingMemory().configure({
    rotationHours: 24,
    archivePath: "/var/logs/ai/archives",
    maxArchives: 30,
    maxMessages: 100
})

// Messages automatically rotated after 24 hours
agent = aiAgent( name: "Archiving Bot", memory: memory )

// Retrieve archived conversations
archives = memory.getArchived()
```

---

## Advanced Examples

### Example 4: Multi-Tenant Memory

```boxlang
/**
 * Multi-tenant Memory
 * Isolates conversations by tenant ID
 */
import bxModules.bxai.models.memory.BaseMemory;

class extends="BaseMemory" {

    property name="tenantId" type="string" default="";
    property name="datasource" type="string" default="";

    IAiMemory function configure( required struct config ) {
        super.configure( arguments.config );

        if ( !arguments.config.keyExists( "tenantId" ) ) {
            throw( type: "ConfigurationError", message: "tenantId is required" );
        }
        if ( !arguments.config.keyExists( "datasource" ) ) {
            throw( type: "ConfigurationError", message: "datasource is required" );
        }

        variables.tenantId = arguments.config.tenantId;
        variables.datasource = arguments.config.datasource;

        return this;
    }

    IAiMemory function add( required any message ) {
        super.add( arguments.message );

        // Save to tenant-specific table
        queryExecute(
            "INSERT INTO ai_memory (tenant_id, conversation_id, role, content, timestamp)
             VALUES (:tenantId, :key, :role, :content, :timestamp)",
            {
                tenantId: variables.tenantId,
                key: variables.key,
                role: arguments.message.role,
                content: arguments.message.content,
                timestamp: { value: now(), cfsqltype: "timestamp" }
            },
            { datasource: variables.datasource }
        );

        return this;
    }

    array function getAll() {
        var result = queryExecute(
            "SELECT role, content, timestamp
             FROM ai_memory
             WHERE tenant_id = :tenantId
               AND conversation_id = :key
             ORDER BY timestamp",
            {
                tenantId: variables.tenantId,
                key: variables.key
            },
            { datasource: variables.datasource }
        );

        return queryToArray( result );
    }

    IAiMemory function clear() {
        queryExecute(
            "DELETE FROM ai_memory
             WHERE tenant_id = :tenantId
               AND conversation_id = :key",
            {
                tenantId: variables.tenantId,
                key: variables.key
            },
            { datasource: variables.datasource }
        );

        variables.messages = [];
        return this;
    }

    private function queryToArray( required query qry ) {
        var result = [];
        for ( var row in arguments.qry ) {
            result.append({
                role: row.role,
                content: row.content,
                timestamp: row.timestamp
            });
        }
        return result;
    }
}
```

---

## Testing Your Memory

### Unit Tests

```boxlang
component extends="testbox.system.BaseSpec" {

    function run() {
        describe( "Custom Memory Tests", function() {

            it( "should configure correctly", function() {
                var memory = new MyCustomMemory().configure({
                    customOption: "value"
                });

                expect( memory ).toBeInstanceOf( "IAiMemory" );
            });

            it( "should add and retrieve messages", function() {
                var memory = new MyCustomMemory().configure({});

                memory.add( "Test message" );

                var messages = memory.getAll();
                expect( messages ).toBeArray();
                expect( messages.len() ).toBe( 1 );
                expect( messages[ 1 ].content ).toBe( "Test message" );
            });

            it( "should clear messages", function() {
                var memory = new MyCustomMemory().configure({});

                memory.add( "Message 1" );
                memory.add( "Message 2" );

                memory.clear();

                expect( memory.getAll().len() ).toBe( 0 );
            });

            it( "should handle maxMessages limit", function() {
                var memory = new MyCustomMemory().configure({
                    maxMessages: 3
                });

                memory.add( "Message 1" );
                memory.add( "Message 2" );
                memory.add( "Message 3" );
                memory.add( "Message 4" );  // Should trigger trim

                expect( memory.getAll().len() ).toBeLTE( 3 );
            });

        });
    }
}
```

---

## Best Practices

### 1. Always Call super.configure()

```boxlang
IAiMemory function configure( required struct config ) {
    super.configure( arguments.config );  // Essential!

    // Your custom configuration
    return this;
}
```

### 2. Validate Configuration

```boxlang
IAiMemory function configure( required struct config ) {
    super.configure( arguments.config );

    // Validate required fields
    if ( !arguments.config.keyExists( "requiredField" ) ) {
        throw( type: "ConfigurationError", message: "requiredField is required" );
    }

    return this;
}
```

### 3. Handle Errors Gracefully

```boxlang
array function getAll() {
    try {
        return loadFromExternalSource();
    } catch( any e ) {
        writeLog( type: "error", log: "ai", text: "Failed to load messages: #e.message#" );
        return []; // Fail gracefully
    }
}
```

### 4. Implement Export/Import

```boxlang
struct function export() {
    var data = super.export();
    // Add your custom data
    data.customField = variables.customField;
    return data;
}
```

### 5. Add Event Announcements

```boxlang
IAiMemory function add( required any message ) {
    super.add( arguments.message );

    // Announce custom event
    BoxAnnounce( "onCustomMemoryAdd", {
        key: variables.key,
        message: arguments.message
    } );

    return this;
}
```

### 6. Thread Safety

```boxlang
// Use locks for concurrent access
IAiMemory function add( required any message ) {
    lock name="memory_#variables.key#" type="exclusive" timeout="5" {
        super.add( arguments.message );
        saveToStorage();
    }
    return this;
}
```

---

## See Also

- [Memory Systems Guide](../main-components/memory.md) - Standard memory types
- [Vector Memory Guide](../main-components/vector-memory.md) - Vector memory implementations
- [Custom Vector Memory](custom-vector-memory.md) - Building vector memory providers
- [IAiMemory Interface](../../src/main/bx/models/memory/IAiMemory.bx) - Full interface specification

---

**Next Steps:** Explore [Custom Vector Memory](custom-vector-memory.md) for building embedding-based memory providers.
