# aiTokens

Estimate token count for AI processing. Useful for staying within model limits, estimating costs, and optimizing prompt size before sending to AI providers.

## Syntax

```javascript
aiTokens(text, options)
```

## Parameters

| Parameter | Type   | Required | Default | Description                                    |
| --------- | ------ | -------- | ------- | ---------------------------------------------- |
| `text`    | any    | Yes      | -       | String or array of strings to count tokens for |
| `options` | struct | No       | `{}`    | Configuration struct for token estimation      |

### Options Structure

| Option     | Type    | Default        | Description                                      |
| ---------- | ------- | -------------- | ------------------------------------------------ |
| `method`   | string  | `"characters"` | Estimation method: "characters" or "words"       |
| `detailed` | boolean | `false`        | Return detailed statistics instead of just count |

### Estimation Methods

* **characters**: Fast estimation based on character count (\~4 chars = 1 token)
* **words**: Slightly more accurate based on word count (\~1.3 words = 1 token)

## Returns

Returns:

* **Numeric**: Token count estimate (when `detailed: false`)
* **Struct**: Detailed statistics (when `detailed: true`) with keys:
  * `tokens` - Estimated token count
  * `characters` - Total character count
  * `words` - Total word count
  * `chunks` - Number of text chunks (if array)
  * `method` - Estimation method used

## Examples

### Basic Token Count

```javascript
// Simple text estimation
tokens = aiTokens( "Hello, world!" );
println( "Tokens: #tokens#" ); // ~4 tokens
```

### Longer Text

```javascript
// Count tokens in longer text
text = "The quick brown fox jumps over the lazy dog. This is a test sentence.";
tokens = aiTokens( text );
println( "Tokens: #tokens#" ); // ~18 tokens
```

### Character-Based Estimation (Default)

```javascript
// Fast character-based estimation
text = "BoxLang is a modern dynamic JVM language";
charTokens = aiTokens( text, { method: "characters" } );
println( "Character-based: #charTokens# tokens" );
```

### Word-Based Estimation

```javascript
// More accurate word-based estimation
text = "BoxLang is a modern dynamic JVM language";
wordTokens = aiTokens( text, { method: "words" } );
println( "Word-based: #wordTokens# tokens" );
```

### Array of Text Chunks

```javascript
// Count tokens across multiple chunks
chunks = [
    "First paragraph of content.",
    "Second paragraph with more information.",
    "Third paragraph concluding the document."
];

totalTokens = aiTokens( chunks );
println( "Total tokens across chunks: #totalTokens#" );
```

### Detailed Statistics

```javascript
// Get full breakdown
text = "The quick brown fox jumps over the lazy dog.";
stats = aiTokens( text, { detailed: true } );

println( "Detailed Statistics:" );
println( "  Tokens: #stats.tokens#" );
println( "  Characters: #stats.characters#" );
println( "  Words: #stats.words#" );
println( "  Chunks: #stats.chunks#" );
println( "  Method: #stats.method#" );
```

### Check Before Sending

```javascript
// Verify within model limits before sending
prompt = "Explain quantum computing in detail...";
tokens = aiTokens( prompt );

if ( tokens > 4000 ) {
    println( "Prompt too long (#tokens# tokens). Truncating..." );
    // Truncate or chunk the prompt
} else {
    response = aiChat( prompt );
}
```

### Cost Estimation

```javascript
// Estimate API cost before calling
prompt = "Write a comprehensive guide...";
tokens = aiTokens( prompt );

// OpenAI pricing (example)
costPer1kTokens = 0.002; // $0.002 per 1k tokens
estimatedCost = ( tokens / 1000 ) * costPer1kTokens;

println( "Estimated tokens: #tokens#" );
println( "Estimated cost: $#numberFormat(estimatedCost, '0.0000')#" );

if ( estimatedCost < 0.10 ) {
    response = aiChat( prompt );
}
```

### Chunking Decision

```javascript
// Decide whether to chunk based on token count
document = fileRead( "large-document.txt" );
tokens = aiTokens( document );

println( "Document has #tokens# tokens" );

if ( tokens > 8000 ) {
    // Exceeds model limit, chunk it
    println( "Document too large, chunking..." );
    chunks = aiChunk( document, {
        chunkSize: 2000, // ~500 tokens per chunk
        overlap: 400
    });
    println( "Split into #chunks.len()# chunks" );
} else {
    // Process whole document
    println( "Processing entire document" );
    summary = aiChat( "Summarize: #document#" );
}
```

### Batch Processing

```javascript
// Estimate tokens for batch of prompts
prompts = [
    "What is AI?",
    "Explain machine learning",
    "What is deep learning?",
    "Define neural networks"
];

prompts.each( ( prompt, idx ) => {
    tokens = aiTokens( prompt );
    println( "Prompt ##idx##: #tokens# tokens" );
});

// Total tokens
totalTokens = aiTokens( prompts );
println( "Total: #totalTokens# tokens" );
```

### Optimize Prompt Size

```javascript
// Reduce prompt size if too large
function optimizePrompt( text, maxTokens = 2000 ) {
    tokens = aiTokens( text );

    if ( tokens <= maxTokens ) {
        return text;
    }

    // Truncate to approximate character limit
    approxChars = maxTokens * 4; // ~4 chars per token
    truncated = left( text, approxChars );

    return truncated;
}

largePrompt = fileRead( "large-prompt.txt" );
optimized = optimizePrompt( largePrompt, 2000 );
```

### Compare Methods

```javascript
// Compare estimation methods
text = "BoxLang is a modern dynamic JVM language with AI integration.";

charMethod = aiTokens( text, { method: "characters" } );
wordMethod = aiTokens( text, { method: "words" } );

println( "Character method: #charMethod# tokens" );
println( "Word method: #wordMethod# tokens" );
println( "Difference: #abs(charMethod - wordMethod)# tokens" );
```

### Message Token Count

```javascript
// Estimate tokens for full conversation
conversation = [
    { role: "system", content: "You are a helpful assistant" },
    { role: "user", content: "What is BoxLang?" },
    { role: "assistant", content: "BoxLang is a modern dynamic JVM language..." },
    { role: "user", content: "Tell me more about its features" }
];

// Convert to text for estimation
conversationText = conversation.map( msg => msg.content ).toList( " " );
tokens = aiTokens( conversationText );

println( "Conversation tokens: #tokens#" );
```

### Dynamic Context Management

```javascript
// Manage conversation context based on tokens
maxContextTokens = 4000;
conversation = [];

function addMessage( role, content ) {
    // Add new message
    conversation.append({ role: role, content: content });

    // Check total tokens
    allContent = conversation.map( m => m.content ).toList( " " );
    tokens = aiTokens( allContent );

    // Trim old messages if over limit
    while ( tokens > maxContextTokens && conversation.len() > 1 ) {
        conversation.deleteAt( 1 ); // Remove oldest (after system message)
        allContent = conversation.map( m => m.content ).toList( " " );
        tokens = aiTokens( allContent );
    }

    println( "Context tokens: #tokens#" );
}
```

### Pre-Flight Check

```javascript
// Check all aspects before API call
function preflightCheck( prompt, options = {} ) {
    tokens = aiTokens( prompt );
    stats = aiTokens( prompt, { detailed: true } );

    println( "=== Pre-Flight Check ===" );
    println( "Tokens: #tokens#" );
    println( "Characters: #stats.characters#" );
    println( "Words: #stats.words#" );

    maxTokens = options.maxTokens ?: 4000;

    if ( tokens > maxTokens ) {
        println( "⚠️  Exceeds limit of #maxTokens# tokens" );
        return false;
    }

    println( "✅ Within limits" );
    return true;
}

if ( preflightCheck( myPrompt, { maxTokens: 2000 } ) ) {
    response = aiChat( myPrompt );
}
```

### Budget Management

```javascript
// Track token usage for budget management
tokenBudget = 100000; // Monthly budget
tokensUsed = 0;

function trackTokens( text ) {
    tokens = aiTokens( text );
    tokensUsed += tokens;

    remaining = tokenBudget - tokensUsed;
    percentUsed = ( tokensUsed / tokenBudget ) * 100;

    println( "Tokens used: #tokensUsed# / #tokenBudget# (#numberFormat(percentUsed,'0.0')#%)" );
    println( "Remaining: #remaining# tokens" );

    if ( remaining < 0 ) {
        throw( "Token budget exceeded!" );
    }
}
```

## Notes

* ⚡ **Fast Estimation**: Not exact but close enough for most use cases
* 📏 **Model Limits**: Check provider limits (GPT-4: 8k-128k, Claude: 100k-200k)
* 💰 **Cost Planning**: Estimate API costs before making calls
* 🔍 **Optimization**: Use to optimize prompt size and reduce costs
* 📊 **Monitoring**: Track token usage for budget management
* ⚠️ **Approximation**: Estimates may vary ±10-20% from actual token count
* 🎯 **Rule of Thumb**: \~4 characters or \~1.3 words ≈ 1 token (English)

## Related Functions

* [`aiChunk()`](aichunk.md) - Chunk text when over token limits
* [`aiChat()`](aichat.md) - Send prompts to AI providers
* [`aiEmbed()`](aiembed.md) - Generate embeddings (also has token limits)
* [`aiService()`](aiservice.md) - Direct service invocation

## Best Practices

✅ **Check before sending** - Verify prompts fit within model limits

✅ **Estimate costs** - Calculate approximate API costs before calling

✅ **Use for optimization** - Trim unnecessary content to save tokens

✅ **Monitor usage** - Track token consumption for budget management

✅ **Chunk large content** - Use with `aiChunk()` for documents over limits

❌ **Don't rely on exact counts** - Estimates are approximations, allow buffer

❌ **Don't forget response tokens** - Model outputs also count toward limits

❌ **Don't ignore model limits** - Each model has specific token limits
