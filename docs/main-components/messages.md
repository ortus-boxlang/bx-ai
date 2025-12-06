---
description: "Build reusable, dynamic prompts with placeholders that get filled in at runtime. Message templates are the foundation of flexible AI pipelines."
icon: message
---

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

## Message Context

Beyond simple placeholder bindings, you can inject rich contextual data like security information, RAG documents, or application metadata using the **message context system**.

### The `${context}` Placeholder

Use the special `${context}` placeholder to inject contextual data that gets JSON-serialized:

```java
message = aiMessage()
    .system( "You are an assistant. User info: ${context}" )
    .user( "Help me with my account" )
    .setContext({
        userId: "user-123",
        role: "premium",
        permissions: ["read", "write"]
    })

messages = message.render()
// Context is automatically JSON-serialized into the system message
```

### Context Methods

```java
// Set entire context
message.setContext({ key: "value" })

// Add individual values
message.addContext( "userId", "user-123" )
message.addContext( "tenantId", "tenant-456" )

// Merge context
message.mergeContext({ newKey: "newValue" })

// Check for context
if ( message.hasContext() ) {
    context = message.getContext()
    userId = message.getContextValue( "userId", "default" )
}
```

### RAG with Context

Perfect for Retrieval Augmented Generation:

```java
// Retrieve relevant documents
docs = vectorStore.search( query: userQuestion, limit: 5 )

message = aiMessage()
    .system( "Use this context to answer: ${context}" )
    .user( userQuestion )
    .setContext({
        documents: docs.map( d => d.content ),
        sources: docs.map( d => d.metadata.source )
    })

response = aiChat( message.render() )
```

### Security Context

Inject user and tenant information securely:

```java
message = aiMessage()
    .system( "You are a support assistant. User context: ${context}" )
    .user( "What can I do?" )
    .setContext({
        userId: user.id,
        tenantId: user.tenantId,
        permissions: user.permissions,
        subscriptionTier: user.subscription.tier
    })
```

📖 **[Full Message Context Documentation](../advanced/message-context.md)** - Learn about multi-tenant patterns, RAG implementation, context with agents, and best practices.

---

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

### Images

Add images to messages for vision-capable AI models like GPT-4 Vision, Claude 3, or Gemini.

#### Image URLs

Use `image()` to add an image by URL:

```java
message = aiMessage()
    .user( "What is in this image?" )
    .image( "https://example.com/photo.jpg" )

// With detail level (auto, low, high)
message = aiMessage()
    .user( "Analyze this image in detail" )
    .image( "https://example.com/photo.jpg", "high" )
```

#### Embedded Images

Use `embedImage()` to read a local file and embed it as base64:

```java
message = aiMessage()
    .user( "What does this screenshot show?" )
    .embedImage( "/path/to/screenshot.png" )

// With detail level
message = aiMessage()
    .user( "Analyze this diagram" )
    .embedImage( "/path/to/diagram.jpg", "low" )
```

#### Multiple Images

Add multiple images to compare or analyze together:

```java
message = aiMessage()
    .user( "Compare these two images" )
    .image( "https://example.com/before.jpg" )
    .image( "https://example.com/after.jpg" )

// Or mix URLs and embedded images
message = aiMessage()
    .user( "Which one matches the reference?" )
    .embedImage( "/path/to/reference.png" )
    .image( "https://example.com/candidate1.jpg" )
    .image( "https://example.com/candidate2.jpg" )
```

#### Detail Levels

Control image processing with the `detail` parameter:

- **`auto`** (default): Model chooses based on image and task
- **`low`**: 512x512 resolution, faster and cheaper
- **`high`**: Full detail, better for complex images

```java
// Quick overview
message = aiMessage()
    .user( "Describe this image briefly" )
    .image( imageUrl, "low" )

// Detailed analysis
message = aiMessage()
    .user( "Count all objects in this image" )
    .image( imageUrl, "high" )
```

#### Practical Examples

##### Image Analysis Pipeline

```java
analyzer = aiMessage()
    .system( "You are an expert image analyst" )
    .user( "Analyze this image for ${aspect}" )
    .toDefaultModel()
    .transform( r => r.content )

// Analyze from URL
safetyReport = analyzer
    .image( "https://example.com/workplace.jpg" )
    .run( { aspect: "safety hazards" } )

// Analyze from file
qualityReport = analyzer
    .embedImage( "/photos/product.jpg" )
    .run( { aspect: "quality defects" } )
```

##### Document Scanner

```java
scanner = aiMessage()
    .system( "Extract text and key information from documents" )
    .user( "Extract ${fields} from this document" )
    .toDefaultModel()

// Scan invoice
result = scanner
    .embedImage( "/documents/invoice.pdf" )
    .run( { fields: "invoice number, date, total" } )

// Scan receipt
result = scanner
    .embedImage( "/receipts/receipt001.jpg", "high" )
    .run( { fields: "merchant, items, total" } )
```

##### Multi-Image Comparison

```java
comparer = aiMessage()
    .system( "Compare images and identify differences" )
    .user( "List all differences between these images" )
    .toDefaultModel()

differences = comparer
    .image( "https://example.com/original.jpg" )
    .image( "https://example.com/modified.jpg" )
    .run()
```

##### Vision-Based Content Generation

```java
generator = aiMessage()
    .system( "Generate ${format} based on images" )
    .user( "Create a ${format} describing these images" )
    .toDefaultModel()

// Generate alt text
altText = generator
    .embedImage( "/web/images/hero.jpg" )
    .run( { format: "concise alt text" } )

// Generate product description
description = generator
    .image( productImageUrl1 )
    .image( productImageUrl2 )
    .run( { format: "detailed product description" } )
```

**Note:** Image support requires vision-capable AI models. Check your provider's documentation for supported models and pricing.

### Audio

Add audio files to messages for AI models that support audio processing.

**Supported by:** OpenAI (GPT-4o-audio-preview), Gemini (1.5+, 2.0)

**Formats:** mp3, mp4, mpeg, mpga, m4a, wav, webm

#### Audio URLs

```java
message = aiMessage()
    .user( "Transcribe and summarize this audio" )
    .audio( "https://example.com/recording.mp3" )
```

#### Embedded Audio

```java
message = aiMessage()
    .user( "What language is spoken in this audio?" )
    .embedAudio( "/path/to/conversation.mp3" )
```

#### Audio Analysis Pipeline

```java
transcriber = aiMessage()
    .system( "Transcribe audio and extract ${info}" )
    .user( "Process this audio file" )
    .toDefaultModel()

// Transcribe meeting
transcript = transcriber
    .embedAudio( "/meetings/team-standup.mp3" )
    .run( { info: "action items and decisions" } )

// Analyze podcast
summary = transcriber
    .audio( podcastUrl )
    .run( { info: "key topics and timestamps" } )
```

### Video

Add video files to messages for AI models with video understanding capabilities.

**Supported by:** Gemini (gemini-1.5-pro, gemini-2.0-flash)

**Formats:** mp4, mpeg, mov, avi, flv, mpg, webm, wmv

#### Video URLs

```java
message = aiMessage()
    .user( "What is happening in this video?" )
    .video( "https://example.com/demo.mp4" )
```

#### Embedded Videos

```java
message = aiMessage()
    .user( "Analyze the actions in this security footage" )
    .embedVideo( "/videos/security-cam-01.mp4" )
```

#### Video Analysis Pipeline

```java
videoAnalyzer = aiMessage()
    .system( "You are a video content analyst" )
    .user( "Analyze this video for ${purpose}" )
    .toDefaultModel()

// Content moderation
report = videoAnalyzer
    .embedVideo( "/uploads/user-video.mp4" )
    .run(
        { purpose: "policy violations and inappropriate content" },
        {},
        { provider: "gemini" }
    )

// Tutorial analysis
summary = videoAnalyzer
    .video( tutorialUrl )
    .run(
        { purpose: "step-by-step instructions and key concepts" },
        {},
        { provider: "gemini" }
    )
```

### Documents and PDFs

Add documents and PDFs to messages for AI models that support document understanding.

**Supported by:** Claude (Opus, Sonnet), OpenAI (GPT-4o)

**Formats:** pdf, doc, docx, txt, xls, xlsx

#### Document URLs

```java
message = aiMessage()
    .user( "Summarize this contract" )
    .document( "https://example.com/contract.pdf", "Service Agreement" )

// PDF alias
message = aiMessage()
    .user( "Extract key dates from this PDF" )
    .pdf( "https://example.com/schedule.pdf" )
```

#### Embedded Documents

```java
message = aiMessage()
    .user( "What are the main conclusions?" )
    .embedDocument( "/reports/quarterly-review.pdf", "Q4 2024 Review" )

// PDF alias
message = aiMessage()
    .user( "Find all mentions of security concerns" )
    .embedPdf( "/documents/audit-report.pdf" )
```

#### Document Analysis Pipeline

```java
docAnalyzer = aiMessage()
    .system( "You are a document analysis expert" )
    .user( "Analyze this document for ${focus}" )
    .toDefaultModel()

// Legal document review
findings = docAnalyzer
    .embedPdf( "/legal/contract-v2.pdf", "Software License" )
    .run( { focus: "liability clauses and termination conditions" } )

// Financial report analysis
insights = docAnalyzer
    .document( reportUrl, "Annual Report" )
    .run( { focus: "revenue trends and expense categories" } )
```

### Mixed Multimodal Content

Combine multiple media types in a single message:

```java
// Document + Images
message = aiMessage()
    .user( "Compare the document specs with these product images" )
    .embedPdf( "/specs/product-specs.pdf" )
    .embedImage( "/photos/product-front.jpg" )
    .embedImage( "/photos/product-back.jpg" )

// Video + Audio analysis
message = aiMessage()
    .user( "Does the audio match the video content?" )
    .embedVideo( "/media/video-clip.mp4" )
    .embedAudio( "/media/separate-audio.mp3" )

// Complete multimedia analysis
analyzer = aiMessage()
    .system( "Analyze all provided media comprehensively" )
    .user( "Review these materials for ${purpose}" )
    .toDefaultModel()

result = analyzer
    .embedDocument( "/reports/brief.pdf", "Project Brief" )
    .embedImage( "/mockups/ui-design.png" )
    .embedVideo( "/demos/prototype-demo.mp4" )
    .embedAudio( "/recordings/stakeholder-feedback.mp3" )
    .run( { purpose: "alignment with project requirements" } )
```

### Multimodal Provider Support

| Feature | OpenAI | Claude | Gemini |
|---------|--------|--------|--------|
| **Images** | ✅ GPT-4o, GPT-4-turbo | ✅ Claude 3+ | ✅ All vision models |
| **Audio** | ✅ GPT-4o-audio | ❌ | ✅ Gemini 1.5+, 2.0 |
| **Video** | ❌ | ❌ | ✅ Gemini 1.5+, 2.0 |
| **Documents** | ✅ GPT-4o | ✅ Claude 3+ | ⚠️ Via OCR |

**Important Notes:**

- **File Size Limits:** Images (20MB), Audio (25MB for OpenAI), Video (2GB for Gemini), Documents (~10MB inline)
- **Large Files:** For files >10MB, consider using provider-specific file upload APIs
- **Base64 Inline:** All `embed*` methods use base64 inline encoding for simplicity
- **Context Limits:** Large media files consume significant context tokens

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
- **[Message Context](../advanced/message-context.md)** - Inject security and RAG data into messages
- **[Transformers](transformers.md)** - Process responses
- **[Pipeline Streaming](streaming.md)** - Real-time template execution
