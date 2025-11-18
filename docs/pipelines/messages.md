# Message Templates

Build reusable, dynamic prompts with placeholders that get filled in at runtime. Message templates are the foundation of flexible AI pipelines.

## Creating Messages

Use `aiMessage()` to build structured messages:

### Basic Message

```java
message = aiMessage()
    .user( "Hello, world!" )

messages = message.run()
// [{ role: "user", content: "Hello, world!" }]
```

### Multiple Messages

```java
message = aiMessage()
    .system( "You are a helpful assistant" )
    .user( "What is BoxLang?" )
    .assistant( "BoxLang is a modern JVM language" )
    .user( "Tell me more" )

messages = message.run()
```

### Message Roles

- **system**: Sets AI behavior/personality
- **user**: Your messages/questions
- **assistant**: AI responses (for conversation history)

## Template Placeholders

Use `${key}` syntax for dynamic values:

### Simple Placeholder

```java
message = aiMessage()
    .user( "Hello, ${name}!" )

messages = message.run( { name: "Alice" } )
// [{ role: "user", content: "Hello, Alice!" }]
```

### Multiple Placeholders

```java
message = aiMessage()
    .system( "You are a ${personality} ${role}" )
    .user( "Tell me about ${topic}" )

messages = message.run( {
    personality: "friendly",
    role: "teacher",
    topic: "BoxLang"
} )
```

### Complex Templates

```java
codeReviewer = aiMessage()
    .system( "You are a ${language} expert. Focus on ${aspect}." )
    .user( "Review this ${language} code:\n\n${code}\n\nPay attention to ${aspect}." )

messages = codeReviewer.run( {
    language: "BoxLang",
    aspect: "performance",
    code: "function sort(arr) { ... }"
} )
```

## Binding Strategies

### Runtime Bindings

Pass bindings when running:

```java
message = aiMessage()
    .user( "Greet ${name}" )

message.run( { name: "Alice" } )  // "Greet Alice"
message.run( { name: "Bob" } )    // "Greet Bob"
```

### Stored Bindings

Pre-bind values with `.bind()`:

```java
message = aiMessage()
    .system( "You are a ${role}" )
    .user( "Explain ${topic}" )
    .bind( { role: "teacher" } )

// Only need to provide topic
message.run( { topic: "variables" } )
// role is already "teacher"
```

### Binding Precedence

Runtime bindings override stored bindings:

```java
message = aiMessage()
    .user( "Hello, ${name}" )
    .bind( { name: "Default" } )

message.run()                     // "Hello, Default"
message.run( { name: "Custom" } ) // "Hello, Custom" (overridden)
```

### Merging Bindings

Bindings merge at each level:

```java
message = aiMessage()
    .system( "${role}: ${style}" )
    .user( "${task}" )
    .bind( { role: "Assistant", style: "helpful" } )

// Runtime adds 'task', merges with stored
message.run( { task: "Explain AI", style: "concise" } )
// role: "Assistant" (from stored)
// style: "concise" (runtime overrides)
// task: "Explain AI" (from runtime)
```

## Messages in Pipelines

### Basic Pipeline

```java
pipeline = aiMessage()
    .system( "You are helpful" )
    .user( "Explain ${topic}" )
    .toDefaultModel()

result = pipeline.run( { topic: "AI" } )
```

### Reusable Template

```java
// Create template once
explainer = aiMessage()
    .system( "You are a ${style} teacher" )
    .user( "Explain ${topic} in simple terms" )
    .toDefaultModel()
    .transform( r => r.content )

// Use many times
result1 = explainer.run( { style: "patient", topic: "variables" } )
result2 = explainer.run( { style: "concise", topic: "functions" } )
result3 = explainer.run( { style: "detailed", topic: "classes" } )
```

### Multi-Step Templates

```java
// Step 1: Generate
generator = aiMessage()
    .user( "Write ${language} code to ${task}" )

// Step 2: Review
reviewer = aiMessage()
    .user( "Review this code for ${aspect}: ${code}" )

// Combined pipeline
pipeline = generator
    .toDefaultModel()
    .transform( r => r.content )
    .to( reviewer )
    .toDefaultModel()

result = pipeline.run( {
    language: "BoxLang",
    task: "sort array",
    aspect: "efficiency",
    code: ""  // Populated by transform
} )
```

## Pipeline Options

Messages in pipelines support the `options` parameter for controlling runtime behavior.

### Setting Default Options

```java
pipeline = aiMessage()
    .system( "You are helpful" )
    .user( "Explain ${topic}" )
    .toDefaultModel()
    .withOptions( {
        returnFormat: "single",
        timeout: 60,
        logRequest: true
    } )

result = pipeline.run( { topic: "AI" } )  // Uses default options
```

### Runtime Options Override

```java
pipeline = aiMessage()
    .user( "Hello" )
    .toDefaultModel()
    .withOptions( { returnFormat: "raw" } )

// Override at runtime - third parameter is options
result = pipeline.run(
    { name: "World" },           // input bindings
    { temperature: 0.7 },        // AI parameters
    { returnFormat: "single" }   // runtime options (overrides default)
)
```

### Convenience Methods

For return format, use convenience methods:

```java
// These are equivalent:
pipeline.withOptions( { returnFormat: "single" } )
pipeline.singleMessage()

// Extract just the content string
result = aiMessage()
    .user( "Say hello" )
    .toDefaultModel()
    .singleMessage()  // Convenience method
    .run()
// "Hello! How can I help you?"

// Get all messages
result = aiMessage()
    .user( "List colors" )
    .toDefaultModel()
    .allMessages()  // Convenience method
    .run()
// [{ role: "assistant", content: "Red, Blue, Green" }]

// Get raw response (default for pipelines)
result = aiMessage()
    .user( "Hello" )
    .toDefaultModel()
    .rawResponse()  // Explicit (raw is default)
    .run()
// { model: "gpt-3.5-turbo", choices: [...], usage: {...}, ... }
```

### Available Options

- `returnFormat:string` - `"raw"` (default), `"single"`, or `"all"`
- `timeout:numeric` - Request timeout in seconds
- `logRequest:boolean` - Log requests to `ai.log`
- `logRequestToConsole:boolean` - Log requests to console
- `logResponse:boolean` - Log responses to `ai.log`
- `logResponseToConsole:boolean` - Log responses to console
- `provider:string` - Override AI provider
- `apiKey:string` - Override API key

## Advanced Features

### Rendering Templates

Get formatted messages without running through AI:

```java
message = aiMessage()
    .system( "You are ${role}" )
    .user( "Task: ${task}" )
    .bind( { role: "assistant" } )

// Format with bindings
formatted = message.format( { task: "help user" } )
// Returns array of formatted messages

// Render with stored bindings only
rendered = message.render()
// Uses only .bind() values
```

### Named Messages

```java
message = aiMessage()
    .user( "Hello" )
    .withName( "greeting-template" )

println( message.getName() )  // "greeting-template"
```

### History

Use `history()` to inflate an `AiMessage` with a prior conversation. This accepts either an array of message structs or another `AiMessage` instance. Each message is appended to the current message list in order.

Signature:

```java
message.history( messages )  // messages = array of structs OR AiMessage instance
```

Behavior highlights:

- If passed an `AiMessage`, its internal messages are extracted and appended.
- If passed an array, each element (struct) is added via the normal `.add()` flow.
- The method will throw an error when passed anything other than an array or `AiMessage`.
- History can be chained fluently with other message methods.

Examples:

```java
// 1) Inflate from an array of messages
historyMessages = [
    { role: "system", content: "You are a helpful assistant." },
    { role: "user", content: "Hello!" },
    { role: "assistant", content: "Hi there!" }
]

message = aiMessage()
    .history( historyMessages )
    .user( "Tell me a joke" )

// 2) Inflate from another AiMessage instance
previous = aiMessage()
    .system( "You are helpful" )
    .user( "What's 2+2?" )
    .assistant( "4" )

message = aiMessage()
    .history( previous )
    .user( "What about 3+3?" )

// 3) Chaining with other methods
message = aiMessage()
    .system( "You are helpful" )
    .history( historyMessages )
    .user( "Follow up question" )
```

### Streaming Messages

Messages can stream their content:

```java
message = aiMessage()
    .system( "System prompt" )
    .user( "User message" )
    .assistant( "Assistant message" )

// Stream each message
message.stream( ( msg ) => {
    println( msg.role & ": " & msg.content )
} )
```

## Practical Examples

### FAQ Template

```java
faqTemplate = aiMessage()
    .system( "You are a ${company} support agent. Be ${tone}." )
    .user( "${question}" )
    .bind( {
        company: "Ortus Solutions",
        tone: "helpful and professional"
    } )
    .toDefaultModel()
    .transform( r => r.content )

answer1 = faqTemplate.run( { question: "What are your hours?" } )
answer2 = faqTemplate.run( { question: "How do I reset password?" } )
```

### Code Generator

```java
codeGen = aiMessage()
    .system( "You are an expert ${language} developer" )
    .user( "Write ${language} code to ${task}. ${requirements}" )
    .toDefaultModel()
    .transform( r => r.content )

// Use with different languages
boxlang = codeGen.run( {
    language: "BoxLang",
    task: "sort array",
    requirements: "Use native functions"
} )

java = codeGen.run( {
    language: "Java",
    task: "sort array",
    requirements: "Use streams"
} )
```

### Translation Pipeline

```java
translator = aiMessage()
    .system( "Translate from ${fromLang} to ${toLang}" )
    .user( "${text}" )
    .bind( { fromLang: "English" } )
    .toDefaultModel()
    .transform( r => r.content )

spanish = translator.run( { toLang: "Spanish", text: "Hello" } )
french = translator.run( { toLang: "French", text: "Hello" } )
```

### Context-Aware Assistant

```java
assistant = aiMessage()
    .system( "
        Context: ${context}
        Your role: ${role}
        User level: ${userLevel}
    " )
    .user( "${question}" )
    .bind( {
        context: "BoxLang documentation assistant",
        role: "helpful teacher",
        userLevel: "beginner"
    } )
    .toDefaultModel()

answer = assistant.run( { question: "What is a function?" } )
```

### Multi-Language Support

```java
component {
    property name="templates" type="struct";

    function init() {
        variables.templates = {
            en: aiMessage()
                .system( "You are helpful" )
                .user( "${question}" ),
            es: aiMessage()
                .system( "Eres un asistente útil" )
                .user( "${pregunta}" ),
            fr: aiMessage()
                .system( "Vous êtes serviable" )
                .user( "${question}" )
        }
        return this
    }

    function ask( required string question, string lang = "en" ) {
        template = variables.templates[ arguments.lang ]
        bindings = arguments.lang == "es"
            ? { pregunta: arguments.question }
            : { question: arguments.question }

        return template.toDefaultModel().run( bindings )
    }
}
```

## Tips and Best Practices

1. **Use Descriptive Names**: `${userId}` not `${id}`
2. **Provide Defaults**: Use `.bind()` for common values
3. **Document Templates**: Comment placeholder meanings
4. **Validate Inputs**: Check required bindings exist
5. **Escape Special Chars**: Handle user input safely
6. **Version Templates**: Track changes to prompts
7. **Test Variations**: Try different wording

## Template Library Pattern

```java
component {
    function greeting( required string style ) {
        return aiMessage()
            .system( "You are ${style}" )
            .user( "Greet ${name}" )
            .bind( { style: arguments.style } )
    }

    function explainer( required string role ) {
        return aiMessage()
            .system( "You are a ${role}" )
            .user( "Explain ${topic} in ${detail} detail" )
            .bind( { role: arguments.role, detail: "moderate" } )
    }

    function coder( required string language ) {
        return aiMessage()
            .system( "Expert ${language} developer" )
            .user( "Write ${language} code: ${task}" )
            .bind( { language: arguments.language } )
    }
}

// Usage
lib = new TemplateLibrary()
pipeline = lib.explainer( "teacher" ).toDefaultModel()
result = pipeline.run( { topic: "AI", detail: "simple" } )
```

## Next Steps

- **[Working with Models](models.md)** - Connect templates to AI
- **[Transformers](transformers.md)** - Process responses
- **[Pipeline Streaming](streaming.md)** - Real-time template execution
