# Lesson 2: Your First AI Chat

**Duration:** 45 minutes
**Prerequisites:** Lesson 1 completed, BoxLang AI module installed

## Learning Objectives

By the end of this lesson, you will:
- Understand AI message roles (system, user, assistant)
- Build multi-turn conversations
- Use different return formats (text, JSON, XML)
- Handle conversation context properly
- Build a simple Q&A bot

---

## Part 1: Understanding Messages

### What are Messages?

In AI conversations, messages have specific **roles**:

1. **System** - Instructions for the AI's behavior
2. **User** - Input from the human
3. **Assistant** - Responses from the AI

Think of it like directing a movie:
- **System** = Director's instructions
- **User** = The script/dialogue
- **Assistant** = The actor's performance

### Simple vs. Structured Messages

```java
// Simple: Just send text
answer = aiChat( "Hello!" )

// Structured: Full control over messages
answer = aiChat( [
    { role: "system", content: "You are a helpful assistant" },
    { role: "user", content: "Hello!" }
] )
```

---

## Part 2: The Power of System Messages

System messages set the AI's personality, tone, and behavior.

### Example: Customer Service Bot

```java
// Without system message
answer = aiChat( "I need help" )
// Generic response

// With system message
answer = aiChat( [
    {
        role: "system",
        content: "You are a friendly customer service representative for TechCo.
                  Be empathetic, solution-oriented, and always ask follow-up questions."
    },
    { role: "user", content: "I need help" }
] )
// Specific, branded response
```

### System Message Best Practices

```java
// ✅ GOOD: Clear, specific instructions
system = "You are a Python tutor for beginners.
          - Use simple language
          - Provide code examples
          - Encourage questions
          - Never assume prior knowledge"

// ❌ BAD: Vague instructions
system = "Help with Python"
```

---

## Part 3: Multi-Turn Conversations

Real conversations have history. Here's how to maintain context:

```java
// Build conversation history
conversation = []

// Turn 1: Initial question
conversation.append( { role: "user", content: "What is BoxLang?" } )
answer1 = aiChat( conversation )
conversation.append( { role: "assistant", content: answer1 } )

// Turn 2: Follow-up (AI remembers context)
conversation.append( { role: "user", content: "How do I install it?" } )
answer2 = aiChat( conversation )
conversation.append( { role: "assistant", content: answer2 } )

// Turn 3: Another follow-up
conversation.append( { role: "user", content: "Show me an example" } )
answer3 = aiChat( conversation )
```

### Conversation Pattern

```
System: You are a helpful assistant
User: What's the weather?
Assistant: I don't have real-time weather data...
User: How can I check it?              ← AI remembers we're talking about weather
Assistant: You can check weather by...
```

---

## Part 4: Return Formats

BoxLang AI supports multiple output formats:

### Text (Default)

```java
answer = aiChat( "Say hello" )
// Returns: "Hello! How can I help you today?"
```

### JSON Format

```java
answer = aiChat(
    "List 3 programming languages and their use cases",
    { responseFormat: "json" }
)

// Returns structured JSON:
// {
//   "languages": [
//     { "name": "Python", "useCase": "Data science and ML" },
//     { "name": "JavaScript", "useCase": "Web development" },
//     { "name": "Java", "useCase": "Enterprise applications" }
//   ]
// }

data = deserializeJSON( answer )
data.languages.each( lang => {
    println( "#lang.name#: #lang.useCase#" )
} )
```

### XML Format

```java
answer = aiChat(
    "Create an XML document with 3 book titles",
    { responseFormat: "xml" }
)

// Returns:
// <books>
//   <book><title>1984</title></book>
//   <book><title>Brave New World</title></book>
//   <book><title>Fahrenheit 451</title></book>
// </books>

xmlDoc = xmlParse( answer )
books = xmlSearch( xmlDoc, "//book/title" )
```

---

## Part 5: The aiMessage Helper

BoxLang AI provides a fluent builder for messages:

```java
// Create messages with fluent API
messages = [
    aiMessage().system( "You are a poet" ),
    aiMessage().user( "Write a haiku about code" )
]

answer = aiChat( messages )
```

### Dynamic Message Roles

```java
// The aiMessage() builder supports dynamic roles
msg = aiMessage().system( "You are helpful" )
msg = aiMessage().user( "Hello" )
msg = aiMessage().assistant( "Hi there!" )

// Shorthand - the role method name creates the message
messages = [
    aiMessage().system( "Be concise" ),
    aiMessage().user( "Explain async" )
]
```

---

## Part 6: Practical Examples

### Example 1: Q&A Bot with Memory

```java
// Initialize conversation
conversation = [
    aiMessage().system(
        "You are QuizBot, a friendly quiz master.
         Ask one trivia question at a time and verify answers."
    )
]

// Bot starts
conversation.append( aiMessage().user( "Start a quiz" ) )
response = aiChat( conversation )
conversation.append( aiMessage().assistant( response ) )

println( "QuizBot: " & response )

// User answers
userAnswer = "Paris"
conversation.append( aiMessage().user( userAnswer ) )
response = aiChat( conversation )
conversation.append( aiMessage().assistant( response ) )

println( "QuizBot: " & response )
```

### Example 2: Code Reviewer

```java
code = 'function divide(a, b) { return a / b; }'

review = aiChat( [
    aiMessage().system(
        "You are a code reviewer. Check for bugs, security issues, and best practices.
         Format: JSON with {bugs:[], security:[], suggestions:[]}"
    ),
    aiMessage().user( "Review this code: " & code )
], { responseFormat: "json" } )

issues = deserializeJSON( review )
println( "Bugs found: " & issues.bugs.len() )
```

### Example 3: Translation with Context

```java
conversation = [
    aiMessage().system( "You are a translator (English to Spanish)" )
]

// Translate sentence 1
conversation.append( aiMessage().user( "Translate: The dog is happy" ) )
response = aiChat( conversation )
conversation.append( aiMessage().assistant( response ) )

// Translate sentence 2 - AI remembers language pair
conversation.append( aiMessage().user( "Translate: It is playing" ) )
response = aiChat( conversation )
// AI knows "It" refers to "the dog" from previous context
```

---

## Part 7: Common Patterns

### Pattern 1: Role-Based Prompting

```java
function askExpert( question, expert ) {
    return aiChat( [
        aiMessage().system( "You are a #expert#. Answer briefly and accurately." ),
        aiMessage().user( question )
    ] )
}

// Use it
answer = askExpert( "What is polymorphism?", "programming teacher" )
```

### Pattern 2: Conversation Manager

```java
class ConversationManager {
    property messages;

    function init( systemPrompt = "" ) {
        variables.messages = []
        if ( len( systemPrompt ) ) {
            variables.messages.append( aiMessage().system( systemPrompt ) )
        }
        return this
    }

    function ask( question ) {
        variables.messages.append( aiMessage().user( question ) )
        answer = aiChat( variables.messages )
        variables.messages.append( aiMessage().assistant( answer ) )
        return answer
    }

    function reset() {
        variables.messages = []
        return this
    }
}

// Use it
chat = new ConversationManager( "You are a math tutor" )
println( chat.ask( "What is 15 * 23?" ) )
println( chat.ask( "Now divide that by 5" ) )  // AI remembers previous answer
```

### Pattern 3: Structured Output Parser

```java
function extractJSON( text ) {
    // Find JSON in text
    start = text.find( "{" )
    end = text.findLast( "}" )

    if ( start > 0 && end > 0 ) {
        json = text.mid( start, end - start + 1 )
        return deserializeJSON( json )
    }

    return {}
}

// Use with AI
answer = aiChat(
    "List your top 3 programming languages as JSON",
    { responseFormat: "json" }
)

data = extractJSON( answer )
```

---

## Examples to Run

### 1. `basic-chat.bxs`
Simple back-and-forth conversation

### 2. `multi-turn.bxs`
Conversation with context memory

### 3. `message-roles.bxs`
Understanding system, user, assistant roles

### 4. `return-formats.bxs`
JSON, XML, and text outputs

### 5. `conversation-manager.bxs`
Reusable conversation class

---

## Lab Exercise: Build a Q&A Bot

**File:** `labs/qa-bot.bxs`

**Objective:**
Create an interactive Q&A bot that:
1. Has a personality (defined in system message)
2. Maintains conversation context
3. Can answer follow-up questions
4. Provides structured responses

**Requirements:**
- Use a system message to define bot personality
- Maintain conversation history
- Handle at least 3 turns
- Use aiMessage() builder
- Return answers in JSON format

**Bonus Challenges:**
- Add conversation reset command
- Save conversation history to file
- Support multiple personalities (switch between experts)
- Add token counting and cost estimation

---

## Knowledge Check

1. **What are the three message roles?**
   - Answer: system, user, assistant

2. **Why are system messages important?**
   - Answer: They set the AI's behavior, personality, and output format

3. **How do you maintain conversation context?**
   - Answer: Keep an array of messages and append each turn (user + assistant)

4. **What's the difference between text and JSON return formats?**
   - Answer: Text is free-form, JSON is structured and parseable

5. **When should you use multi-turn conversations?**
   - Answer: When context from previous messages matters (follow-ups, clarifications, etc.)

---

## Homework

### Assignment 1: Personality Tester
Create a bot with 3 different personalities (serious, funny, poetic) and show how the same question gets different answers.

### Assignment 2: Interview Bot
Build a bot that conducts a technical interview with 5 questions, remembers previous answers, and provides a final evaluation.

### Assignment 3: Debate Simulator
Create two AI "debaters" with opposing views. Have them debate a topic with 3 rounds each.

---

## Key Takeaways

✅ Messages have roles (system, user, assistant)
✅ System messages control AI behavior
✅ Conversation history enables context
✅ Multiple return formats (text, JSON, XML)
✅ Use aiMessage() for fluent message building
✅ Pattern: Store conversation array, append each turn

---

## Next Lesson

**Lesson 3: AI Providers** - Learn about OpenAI, Claude, Gemini, Ollama and how to configure each provider.

---

## Additional Resources

- [Message Roles Documentation](../../docs/chatting/basic-chatting.md)
- [aiMessage() Reference](../../readme.md#aimessage)
- [Return Formats Guide](../../docs/advanced/return-formats.md)
- [Conversation Patterns](../../docs/main-components/messages.md)
