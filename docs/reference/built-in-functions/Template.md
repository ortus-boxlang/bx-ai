# couchbaseGetBucket

Get a Couchbase bucket instance for bucket-level operations.

## Syntax

```js
couchbaseGetBucket(cacheName)
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `cacheName` | String | Yes | Name of the cache configuration |

## Returns

Returns the Couchbase `Bucket` instance with access to:
- Scope management
- Collection access
- Bucket statistics
- Ping operations
- View queries (legacy)

## Examples

### Basic Usage

```js
// Get bucket instance
bucket = couchbaseGetBucket("default");

// Get bucket name
println("Bucket name: #bucket.name()#");
```

### Access Collections

```js
bucket = couchbaseGetBucket("default");

// Get default scope and collection
scope = bucket.defaultScope();
collection = scope.defaultCollection();

// Or get specific scope/collection
customScope = bucket.scope("myapp");
customCollection = customScope.collection("users");
```

### Bucket Operations

```js
bucket = couchbaseGetBucket("default");

// Ping bucket health
pingResult = bucket.ping();
println("Bucket status: #pingResult.version()#");

// Get collections
allScopes = bucket.collections();
```

### Multiple Buckets

```js
// Different buckets for different data
usersBucket = couchbaseGetBucket("users-cache");
sessionsBucket = couchbaseGetBucket("sessions-cache");
vectorsBucket = couchbaseGetBucket("vectors-cache");

// Access their collections independently
usersCollection = usersBucket.defaultScope().defaultCollection();
sessionsCollection = sessionsBucket.defaultScope().defaultCollection();
vectorsCollection = vectorsBucket.defaultScope().defaultCollection();
```

### Bucket Statistics

```js
bucket = couchbaseGetBucket("default");

// Get bucket info (requires cluster admin)
cluster = couchbaseGetCluster("default");
bucketManager = cluster.buckets();
bucketSettings = bucketManager.getBucket(bucket.name());

println("RAM quota: #bucketSettings.ramQuota()# MB");
println("Replica count: #bucketSettings.numReplicas()#");
```

## Notes

- Bucket name is determined by cache configuration
- Bucket instance is cached and reused
- Use `couchbaseGetCollection()` for direct collection access
- Most operations should use collection-level access
- Bucket-level access is useful for management and statistics

## Related Functions

- [couchbaseGetCluster](CouchbaseGetCluster.md) - Get cluster connection
- [couchbaseGetScope](CouchbaseGetScope.md) - Get scope instance
- [couchbaseGetCollection](CouchbaseGetCollection.md) - Get collection instance

## See Also

- [API Usage Guide](../../api-usage.md)
- [Couchbase Bucket API](https://docs.couchbase.com/java-sdk/current/howtos/managing-connections.html)
