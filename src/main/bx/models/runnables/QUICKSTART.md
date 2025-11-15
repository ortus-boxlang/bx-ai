# AI Runnables Quick Start

A quick reference for using AI Runnables.

## Basic Usage

```boxlang
// Create a simple transform
var uppercase = new src.main.bx.models.transformers.AiTransformRunnable(
    fn = ( input ) => ucase( input )
)

var result = uppercase.run( "hello" ) // Returns "HELLO"
```

## Chaining

```boxlang
// Chain multiple operations
var result = new src.main.bx.models.transformers.AiTransformRunnable(
    fn = ( x ) => x * 2
)
    .to( new src.main.bx.models.transformers.AiTransformRunnable(
        fn = ( x ) => x + 10
    ) )
    .run( 5 )
// Result: 20 (5 * 2 = 10, 10 + 10 = 20)
```

## Transform Helper

```boxlang
// Use transform() for inline transformations
var pipeline = new src.main.bx.models.transformers.AiTransformRunnable(
    fn = ( x ) => x + 1
)
    .transform( ( x ) => x * x )
    .run( 4 )
// Result: 25 (4 + 1 = 5, 5 * 5 = 25)
```

## Naming

```boxlang
// Name your runnables for debugging
var doubler = new src.main.bx.models.transformers.AiTransformRunnable(
    fn = ( x ) => x * 2
).withName( "Doubler" )

println( doubler.getName() ) // Outputs: Doubler
```

## Sequences

```boxlang
// Create explicit sequences
var sequence = new src.main.bx.models.runnables.AiRunnableSequence([
    step1,
    step2,
    step3
])

// Inspect the sequence
sequence.print()
println( "Steps: " & sequence.count() )

// Run it
var result = sequence.run( input )
```

## Streaming

```boxlang
// Stream results through a pipeline
transform.stream(
    input,
    ( chunk, metadata ) => {
        println( "Received: " & chunk )
    }
)
```

## Parameters

```boxlang
// Set default parameters
var transform = new src.main.bx.models.transformers.AiTransformRunnable(
    fn = ( x ) => x
)
    .withParams({ temperature: 0.7, model: "gpt-4" })

// Runtime params override defaults
var merged = transform.mergeParams({ model: "gpt-3.5" })
```

## Complete Example

```boxlang
// Build a named, parameterized pipeline
var pipeline = new src.main.bx.models.transformers.AiTransformRunnable(
    fn = ( text ) => trim( text )
)
    .withName( "Trim" )
    .transform( ( text ) => ucase( text ) )
    .transform( ( text ) => replace( text, " ", "_", "all" ) )
    .withParams({ description: "Text normalizer" })

// Use it
var result = pipeline.run( "  hello world  " )
// Result: "HELLO_WORLD"

// Debug it
println( pipeline.getName() )
pipeline.print()
```

## Common Patterns

### Data Transformation Pipeline
```boxlang
var normalizer = trimmer
    .transform( ( x ) => ucase( x ) )
    .transform( ( x ) => replace( x, " ", "_", "all" ) )
```

### Conditional Processing
```boxlang
var processor = new src.main.bx.models.transformers.AiTransformRunnable(
    fn = ( x ) => x > 0 ? x * 2 : x
)
```

### Complex Multi-Step
```boxlang
var complex = step1
    .to( step2 )
    .transform( intermediateTransform )
    .to( step3 )
    .transform( finalTransform )
```

## Tips

1. **Use `withName()`** for debugging complex pipelines
2. **Chain immutably** - `to()` creates new sequences
3. **Stream when possible** for better memory usage
4. **Set default params** at runnable level, override at runtime
5. **Use `print()`** to visualize your pipeline structure

## See Also

- Full documentation: `README.md` in this directory
- Examples: `/examples/runnable-example.bx`
- Tests: `/src/test/bx/specs/RunnablesSpec.bx`
