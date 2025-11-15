# AI Runnables

The AI Runnables package provides a flexible interface for creating composable, chainable operations for AI and data processing pipelines.

## Overview

The runnable interface allows you to:
- Create reusable transformation operations
- Chain operations together into sequences
- Stream results through pipelines
- Manage operation parameters
- Name and inspect pipeline steps

## Core Components

### IAiRunnable

The base interface that all runnables implement:

```java
interface {
    any function run( required any input, struct params = {} );
    any function stream( required any input, required function onChunk, struct params = {} );
}
```

### AiBaseRunnable

Abstract base class providing common functionality:

- **Naming**: `withName()`, `getName()`
- **Parameters**: `withParams()`, `mergeParams()`
- **Chaining**: `to()`, `transform()`

### AiRunnableSequence

Executes multiple runnables in sequence, where each step's output becomes the next step's input.

```boxlang
var sequence = new AiRunnableSequence( [ step1, step2, step3 ] )
var result = sequence.run( input )
```

Features:
- `count()` - Get number of steps
- `getSteps()` - Get detailed step information
- `print()` - Print sequence structure
- `to()` - Add another step
- `run()` - Execute all steps
- `stream()` - Stream through all steps (last step is streamed)

### AiTransformRunnable

Applies a transformation function to input (located in `models/transformers`):

```boxlang
var uppercase = new AiTransformRunnable( 
    fn = ( input ) => ucase( input ) 
)
```

## Usage Examples

### Simple Transform

```boxlang
var double = new AiTransformRunnable( 
    fn = ( x ) => x * 2 
).withName( "Double" )

var result = double.run( 5 ) // Returns 10
```

### Chaining

```boxlang
var chain = new AiTransformRunnable( fn = ( x ) => x * 2 )
    .to( new AiTransformRunnable( fn = ( x ) => x + 10 ) )

var result = chain.run( 5 ) // Returns 20
```

### Transform Helper

```boxlang
var pipeline = new AiTransformRunnable( fn = ( x ) => x + 1 )
    .transform( ( x ) => x * x )

var result = pipeline.run( 4 ) // Returns 25
```

### Parameter Management

```boxlang
var transform = new AiTransformRunnable( fn = ( x ) => x )
    .withParams( { temperature: 0.7, model: "gpt-4" } )

// Runtime params override defaults
var merged = transform.mergeParams( { model: "gpt-3.5" } )
```

### Streaming

```boxlang
var transform = new AiTransformRunnable( fn = ( x ) => x * 2 )

transform.stream( 
    5, 
    ( chunk, metadata ) => {
        println( "Received: " & chunk )
    }
)
```

### Sequence Inspection

```boxlang
var sequence = new AiRunnableSequence( [ step1, step2, step3 ] )

// Print sequence structure
sequence.print()

// Get step details
var steps = sequence.getSteps()
for ( var step in steps ) {
    println( "[#step.index#] #step.name#" )
}
```

## Advanced Usage

### Complex Pipeline

```boxlang
var result = new AiTransformRunnable( fn = ( x ) => x + 1 )
    .to( new AiTransformRunnable( fn = ( x ) => x * 2 ) )
    .transform( ( x ) => x - 5 )
    .to( new AiTransformRunnable( fn = ( x ) => x * 10 ) )
    .run( 10 )

// Steps: 10 + 1 = 11 -> 11 * 2 = 22 -> 22 - 5 = 17 -> 17 * 10 = 170
```

### Named Steps

```boxlang
var step1 = new AiTransformRunnable( fn = ( x ) => x * 2 ).withName( "Double" )
var step2 = new AiTransformRunnable( fn = ( x ) => x + 10 ).withName( "Add Ten" )

var sequence = new AiRunnableSequence( [ step1, step2 ] )
sequence.print()
// Output:
// AiRunnableSequence (2 steps)
//   [1] Double
//   [2] Add Ten
```

## Design Patterns

### Immutable Chaining

The `to()` method creates a new sequence without modifying the original:

```boxlang
var base = new AiTransformRunnable( fn = ( x ) => x * 2 )
var extended = base.to( new AiTransformRunnable( fn = ( x ) => x + 1 ) )

// base is unchanged, extended has both steps
```

### Metadata-Based Naming

If you don't set a name explicitly, the runnable will use its metadata:

```boxlang
var transform = new AiTransformRunnable( fn = ( x ) => x )
var name = transform.getName() // Returns class name from metadata
```

## Testing

See `src/test/bx/specs/RunnablesSpec.bx` and `src/test/java/ortus/boxlang/ai/runnables/RunnablesTest.java` for comprehensive test examples.

## Future Extensions

The runnable interface is designed to be extended. You can create custom runnables by:

1. Implementing `IAiRunnable` directly
2. Extending `AiBaseRunnable` and implementing `run()` and `stream()`
3. Using `AiTransformRunnable` for simple transformations

Potential future runnables:
- AI model invocation runnables
- Database query runnables
- HTTP request runnables
- Parallel execution runnables
- Conditional/branching runnables
