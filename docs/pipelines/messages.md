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

## Conversation History

Maintain context across interactions by prepending previous conversation exchanges using the `history()` method:

### Adding History from Array

```java
// Previous conversation
historyMessages = [
    { role: "user", content: "What is BoxLang?" },
    { role: "assistant", content: "BoxLang is a modern JVM language" },
    { role: "user", content: "What are its key features?" },
    { role: "assistant", content: "BoxLang has dynamic typing, Java interop..." }
]

// New conversation with history
message = aiMessage()
    .history( historyMessages )
    .user( "Can you give me an example?" )

messages = message.run()
// Returns all 5 messages with history first, then new message
```

### Adding History from AiMessage

```java
// Previous conversation as AiMessage
previousChat = aiMessage()
    .user( "Hello" )
    .assistant( "Hi! How can I help?" )
    .user( "Tell me about AI" )
    .assistant( "AI is artificial intelligence..." )

// Continue the conversation
message = aiMessage()
    .history( previousChat )
    .user( "What are some examples?" )

result = message.toDefaultModel().run()
```

### Chaining History

```java
// Build conversation incrementally
conversation = aiMessage()
    .system( "You are a helpful teacher" )
    .user( "Teach me about variables" )

// Add previous exchanges
oldHistory = [
    { role: "user", content: "What is programming?" },
    { role: "assistant", content: "Programming is..." }
]

conversation
    .history( oldHistory )
    .user( "Now explain functions" )
```

### Real-World Example: Chatbot

```java
component {
    property name="conversationHistory" type="array";

    function init() {
        variables.conversationHistory = []
        return this
    }

    function chat( required string userMessage ) {
        // Build message with full history
        message = aiMessage()
            .system( "You are a friendly chatbot" )
            .history( variables.conversationHistory )
            .user( arguments.userMessage )

        // Get AI response
        response = message.toDefaultModel().run()

        // Update history with this exchange
        variables.conversationHistory.append( {
            role: "user",
            content: arguments.userMessage
        } )
        variables.conversationHistory.append( {
            role: "assistant",
            content: response.content
        } )

        return response.content
    }

    function clearHistory() {
        variables.conversationHistory = []
        return this
    }
}

// Usage
chatbot = new Chatbot()
chatbot.chat( "Hello!" )                    // "Hi there! How can I help?"
chatbot.chat( "What's the weather like?" )  // Uses history from previous exchange
chatbot.chat( "Thanks!" )                   // Uses full conversation history
```

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
8. **Manage History**: Use `.history()` to maintain conversation context
   - Store conversation history in persistent storage for multi-turn dialogues
   - Limit history size to avoid exceeding API token limits
   - Clear old history when starting new conversation topics

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
