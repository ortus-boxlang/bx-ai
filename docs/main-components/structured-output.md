---
description: "Use structured output with AI pipelines for type-safe, composable workflows. Combine the power of runnables with guaranteed data structures for robust AI applications."
icon: box
---

# Structured Output in Pipelines

Use structured output with AI pipelines for type-safe, composable workflows. Combine the power of runnables with guaranteed data structures for robust AI applications.

## Quick Start

### Basic Pipeline with Structured Output

```java
// Define your data structure
class Person {
    property name="firstName" type="string";
    property name="lastName" type="string";
    property name="age" type="numeric";
}

// Create pipeline with structured output
pipeline = aiModel( "openai" )
    .structuredOutput( new Person() )

// Run and get typed result
person = pipeline.run( "Extract: John Doe, age 30" )
println( person.getFirstName() )  // "John"
```

## Pipeline Methods

### `.structuredOutput( schema )`

Define structured output for the pipeline:

```java
// With class
model = aiModel().structuredOutput( new Product() )

// With struct
model = aiModel().structuredOutput( {
    name: "",
    price: 0.0,
    inStock: false
} )

// With array
model = aiModel().structuredOutput( [new Contact()] )
```

### `.structuredOutputs( schemas )`

Extract multiple structures in one request:

```java
pipeline = aiModel()
    .structuredOutputs([
        { name: "person", schema: new Person() },
        { name: "company", schema: new Company() },
        { name: "contact", schema: new ContactInfo() }
    ])

result = pipeline.run( "John Doe works at TechCorp, email: john@techcorp.com" )

println( result.person.getFirstName() )     // "John"
println( result.company.getName() )         // "TechCorp"
println( result.contact.getEmail() )        // "john@techcorp.com"
```

### `.schema( jsonSchema )`

Use raw JSON schema for full control:

```java
schema = {
    "type": "object",
    "properties": {
        "title": { "type": "string" },
        "priority": { "type": "string", "enum": ["low", "medium", "high"] },
        "completed": { "type": "boolean" }
    },
    "required": ["title", "priority", "completed"]
}

pipeline = aiModel().schema( schema )
task = pipeline.run( "Add high priority task: Review PR #123" )
```

## Message Templates with Structured Output

Combine reusable prompts with typed outputs:

```java
// Template with placeholders
template = aiMessage()
    .system( "You are a ${role} expert" )
    .user( "Analyze ${topic} and extract key points" )

// Pipeline with structured output
pipeline = template
    .to( aiModel().structuredOutput( new Analysis() ) )

// Run with different inputs
techAnalysis = pipeline.run( {
    role: "technology",
    topic: "cloud computing trends"
} )

bizAnalysis = pipeline.run( {
    role: "business",
    topic: "market opportunities"
} )
```

## Chaining with Transformations

Process structured output through multiple stages:

```java
pipeline = aiModel()
    .structuredOutput( new Product() )
    .transform( product => {
        // Add calculated field
        product.setTaxAmount( product.getPrice() * 0.08 )
        return product
    } )
    .transform( product => {
        // Add to database
        productService.save( product )
        return product.getId()
    } )

productId = pipeline.run( "Premium Widget - $99.99" )
```

## Multi-Step Pipelines

Use structured output at different pipeline stages:

```java
// Step 1: Extract candidate info
extractPipeline = aiModel()
    .structuredOutput( new Candidate() )

// Step 2: Evaluate candidate
evaluatePipeline = aiMessage()
    .system( "You are an HR expert" )
    .user( "Evaluate this candidate: ${json}" )
    .to( aiModel().structuredOutput( new Evaluation() ) )

// Combine
fullPipeline = extractPipeline
    .transform( candidate => {
        return { json: jsonSerialize( candidate ) }
    } )
    .to( evaluatePipeline )

// Run
evaluation = fullPipeline.run( resumeText )
println( "Score: #evaluation.getScore()#" )
println( "Recommendation: #evaluation.getRecommendation()#" )
```

## Working with Arrays

Extract lists of structured items:

```java
class Task {
    property name="title" type="string";
    property name="priority" type="string";
    property name="dueDate" type="string";
}

pipeline = aiModel().structuredOutput( [new Task()] )

tasks = pipeline.run( "
    High priority: Fix login bug (due Friday)
    Medium priority: Update docs (due next week)
    Low priority: Refactor utils (no deadline)
" )

tasks.each( task => {
    println( "[#task.getPriority()#] #task.getTitle()# - #task.getDueDate()#" )
} )
```

## Parallel Processing

Process multiple items with structured output:

```java
emails = [
    "Order #123 from John Smith...",
    "Order #456 from Jane Doe...",
    "Order #789 from Bob Johnson..."
]

// Create pipeline
orderExtractor = aiModel().structuredOutput( new Order() )

// Process in parallel
futures = emails.map( email => {
    return async( () => orderExtractor.run( email ) )
} )

// Collect results
orders = futures.map( f => f.get() )
orders.each( order => {
    println( "Order #order.getOrderNumber()#: $#order.getTotal()#" )
} )
```

## Streaming with Structured Output

Stream progressive responses, get structured final result:

```java
pipeline = aiModel()
    .structuredOutput( new Article() )

// Stream shows progressive text
article = pipeline.stream(
    ( chunk ) => {
        print( chunk.choices?.first()?.delta?.content ?: "" )
    },
    "Write article about AI safety"
)

// Final result is typed
println( "\n\nTitle: #article.getTitle()#" )
println( "Author: #article.getAuthor()#" )
println( "Word Count: #article.getWordCount()#" )
```

## Integration with AI Agents

Agents with structured responses:

```java
agent = aiAgent(
    name: "DataExtractor",
    instructions: "Extract structured data from user input",
    model: aiModel().structuredOutput( new DataRecord() )
)

record = agent.run( "Customer: Alice, Phone: 555-1234, Address: 123 Main St" )

println( record.getCustomerName() )  // "Alice"
println( record.getPhone() )         // "555-1234"
```

## Memory with Structured Data

Store and retrieve structured output:

```java
memory = aiMemory( "file", { filepath: "conversations.json" } )

pipeline = aiMessage()
    .history( memory.getMessages() )
    .user( "${query}" )
    .to( aiModel().structuredOutput( new Response() ) )

response = pipeline.run( { query: "What's the weather?" } )

// Save structured response to memory
memory.add( { role: "user", content: "What's the weather?" } )
memory.add( { role: "assistant", content: jsonSerialize( response ) } )
memory.save()
```

## Advanced Patterns

### Conditional Structured Output

Choose output structure based on input:

```java
function analyzeContent( text ) {
    var pipeline

    if( text.findNoCase( "invoice" ) ) {
        pipeline = aiModel().structuredOutput( new Invoice() )
    } else if( text.findNoCase( "receipt" ) ) {
        pipeline = aiModel().structuredOutput( new Receipt() )
    } else {
        pipeline = aiModel().structuredOutput( new Document() )
    }

    return pipeline.run( text )
}
```

### Validation Pipeline

Validate and retry with structured output:

```java
pipeline = aiModel()
    .structuredOutput( new Product() )
    .transform( product => {
        // Validate
        if( product.getPrice() <= 0 ) {
            throw( "Invalid price" )
        }
        if( product.getName().len() == 0 ) {
            throw( "Missing product name" )
        }
        return product
    } )

try {
    product = pipeline.run( inputText )
} catch( any e ) {
    // Retry with more specific prompt
    product = pipeline.run( "Extract product with valid price and name: #inputText#" )
}
```

### Enrichment Pipeline

Add data to structured output:

```java
// Extract basic info
extractPipeline = aiModel().structuredOutput( new Customer() )

// Enrich with external data
enrichPipeline = extractPipeline
    .transform( customer => {
        // Look up customer in database
        existing = customerService.findByEmail( customer.getEmail() )

        if( !isNull( existing ) ) {
            customer.setCustomerId( existing.getId() )
            customer.setLoyaltyPoints( existing.getPoints() )
        }

        return customer
    } )

customer = enrichPipeline.run( "John Doe, john@example.com" )
```

### Aggregate Pattern

Collect structured results:

```java
results = []

pipeline = aiModel().structuredOutput( new Insight() )
    .transform( insight => {
        results.append( insight )
        return insight
    } )

documents.each( doc => {
    pipeline.run( doc.getContent() )
} )

// Aggregate all insights
aggregatePipeline = aiModel().structuredOutput( new Summary() )
summary = aggregatePipeline.run(
    "Summarize these insights: #jsonSerialize( results )#"
)
```

## Best Practices

### 1. Position Structured Output at Output Stage

```java
// ✅ GOOD: Structured output at final stage
pipeline = aiMessage()
    .user( "Extract data from: ${input}" )
    .to( aiModel().structuredOutput( new Data() ) )

// ❌ BAD: Unnecessary early structuring
pipeline = aiModel().structuredOutput( new Data() )
    .transform( data => processData( data ) )  // Already structured
    .to( aiModel() )  // Loses structure
```

### 2. Use Transform for Post-Processing

```java
// ✅ GOOD: Clean separation
pipeline = aiModel()
    .structuredOutput( new Order() )
    .transform( order => {
        order.setProcessedAt( now() )
        order.setStatus( "pending" )
        return order
    } )

// ❌ BAD: Mixing concerns
// Don't put business logic in AI prompts
```

### 3. Type Your Pipeline Variables

```java
// ✅ GOOD: Clear typing
Person pipeline = aiModel().structuredOutput( new Person() )
Person person = pipeline.run( "John Doe, 30" )

// ✅ ALSO GOOD: Explicit casting when needed
var pipeline = aiModel().structuredOutput( new Person() )
var result = pipeline.run( "John Doe, 30" )
Person person = result  // BoxLang handles type coercion
```

### 4. Cache Pipelines

```java
// ✅ GOOD: Reuse pipeline
variables.productExtractor = aiModel().structuredOutput( new Product() )

function extractProduct( text ) {
    return variables.productExtractor.run( text )
}

// ❌ BAD: Recreate every time
function extractProduct( text ) {
    return aiModel().structuredOutput( new Product() ).run( text )
}
```

## Error Handling

### Schema Validation Errors

```java
try {
    pipeline = aiModel().structuredOutput( [] )  // Empty array
    result = pipeline.run( "test" )
} catch( any e ) {
    println( "Schema error: #e.message#" )
    // "Cannot generate schema from empty array"
}
```

### Runtime Errors

```java
pipeline = aiModel().structuredOutput( new Person() )

try {
    person = pipeline.run( veryLongText )
} catch( any e ) {
    if( e.message.findNoCase( "token" ) ) {
        // Text too long, chunk it
        person = pipeline.run( veryLongText.left( 2000 ) )
    }
}
```

## Performance Tips

### 1. Batch Processing

```java
// Process multiple items in one call
pipeline = aiModel().structuredOutputs([
    { name: "item1", schema: new Product() },
    { name: "item2", schema: new Product() },
    { name: "item3", schema: new Product() }
])

// Better than 3 separate calls
result = pipeline.run( "Product 1: Widget, Product 2: Gadget, Product 3: Gizmo" )
```

### 2. Simple Schemas

```java
// ✅ GOOD: Simple, focused schema
class Product {
    property name="name" type="string";
    property name="price" type="numeric";
}

// ❌ BAD: Overly complex nested structure
class Product {
    property name="details" type="ProductDetails";
    property name="metadata" type="ProductMetadata";
    property name="relationships" type="ProductRelationships";
    // ... many more nested objects
}
```

### 3. Streaming for Long Responses

```java
// For lengthy structured content, use streaming
article = aiModel()
    .structuredOutput( new Article() )
    .withParams( { stream: true } )
    .stream( progressCallback, prompt )
```

## Related Documentation

- [Simple Structured Output](../chatting/structured-output.md) - Basic usage patterns
- [Working with Models](models.md) - Model configuration and setup
- [Message Templates](messages.md) - Creating reusable prompts
- [Transformers](transformers.md) - Data transformation in pipelines
- [Pipeline Overview](overview.md) - Understanding pipeline concepts
