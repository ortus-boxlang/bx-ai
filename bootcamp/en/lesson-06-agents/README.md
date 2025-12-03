# Lesson 6: Building Agents

**⏱️ Duration: 90 minutes**

In this final lesson, we bring everything together to build **autonomous AI agents**. Agents combine conversation memory, tools, and instructions to complete complex multi-step tasks on their own.

## 🎯 What You'll Learn

- Understand the difference between chat and agents
- Create agents with `aiAgent()`
- Add memory so agents remember context
- Give agents tools to interact with the world
- Build a complete assistant that handles complex tasks

---

## 📚 Part 1: What is an AI Agent? (15 mins)

### Chat vs Agent

So far, we've used **chat** - you control everything:

```
CHAT (You Control)
──────────────────
You: "Search for X"
AI: "Here's info about X"
You: "Now calculate Y"
AI: "Y equals 100"
You: (decide what to do next)
```

An **agent** controls its own workflow:

```
AGENT (AI Controls)
───────────────────
You: "Research X and calculate the impact"
Agent: (thinking...)
  1. I should search for X
  2. Now I'll analyze the data
  3. Let me calculate the impact
  4. Here's my comprehensive report!
```

### Agent Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       AI AGENT                                  │
└─────────────────────────────────────────────────────────────────┘

                    ┌─────────────────┐
                    │  INSTRUCTIONS   │
                    │ (System Prompt) │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
  │   MEMORY    │    │    LLM      │    │   TOOLS     │
  │ Conversation│◀──▶│  (Brain)    │◀──▶│ (Actions)   │
  │   History   │    │             │    │             │
  └─────────────┘    └─────────────┘    └─────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │    RESPONSE     │
                    └─────────────────┘


  💡 The agent decides:
     - What tools to use
     - In what order
     - How to combine results
     - When it's done
```

### Why Agents?

- ✅ **Multi-step tasks** - Break down complex problems
- ✅ **Autonomous** - Decide next steps independently
- ✅ **Context-aware** - Remember conversation history
- ✅ **Tool usage** - Call functions when needed
- ✅ **Goal-oriented** - Work toward a specific outcome

---

## 💻 Part 2: Creating Your First Agent (20 mins)

### The aiAgent() Function

```java
agent = aiAgent(
    name: "AgentName",
    description: "What this agent does",
    instructions: "How the agent should behave",
    tools: [ tool1, tool2 ],
    memory: aiMemory( "windowed" )
)

// Run the agent
result = agent.run( "Your request" )
```

### Example: Basic Agent

```java
// basic-agent.bxs
agent = aiAgent(
    name: "Helper",
    description: "A helpful AI assistant",
    instructions: "Be concise and friendly. Help users with their questions."
)

// First interaction
response1 = agent.run( "Hi, my name is Alex" )
println( response1 )
// Output: "Hello Alex! Nice to meet you. How can I help you today?"

// Agent remembers (has memory!)
response2 = agent.run( "What's my name?" )
println( response2 )
// Output: "Your name is Alex!"
```

### Example: Agent with Tools

```java
// tool-agent.bxs
// Create tools
weatherTool = aiTool(
    "get_weather",
    "Get weather for a city",
    ( args ) => {
        data = { "Boston": 72, "Miami": 85, "Denver": 65 }
        return "#data[ args.city ] ?: 70#°F in #args.city#"
    }
).describeCity( "City name" )

calculatorTool = aiTool(
    "calculate",
    "Perform math calculations",
    ( args ) => evaluate( args.expression )
).describeExpression( "Math expression" )

// Create agent with tools
agent = aiAgent(
    name: "SmartAssistant",
    description: "An assistant that can check weather and do math",
    instructions: "Help users with weather info and calculations.",
    tools: [ weatherTool, calculatorTool ]
)

// Agent uses tools automatically!
println( agent.run( "What's the weather in Miami?" ) )
println( agent.run( "What's 20% of 150?" ) )
```

---

## 🧠 Part 3: Agent Memory (15 mins)

Memory lets agents remember the conversation:

### Memory Types

| Type | Description | Best For |
|------|-------------|----------|
| `windowed` | Keeps last N messages | Most use cases |
| `summary` | Summarizes old messages | Long conversations |
| `session` | Persists in web session | Web applications |
| `cache` | Distributed cache storage | Multi-server apps |
| `file` | JSON file persistence | Local storage |
| `jdbc` | Database storage | Enterprise apps |
| `vector` | Semantic search (11 providers) | RAG applications |

> 💡 **Multi-Tenant Memory**: All memory types support `userId` and `conversationId` parameters for multi-user applications. This ensures each user's conversations are completely isolated:
>
> ```java
> memory = aiMemory( "windowed",
>     key: createUUID(),
>     userId: session.userId,           // Isolate per user
>     conversationId: "support-chat",  // Multiple chats per user
>     config: { maxMessages: 20 }
> )
> ```
>
> This is essential for web applications where multiple users interact with your agent!

### Example: Agent with Memory

```java
// memory-agent.bxs
// Simple single-user memory (good for scripts/CLI)
agent = aiAgent(
    name: "PersonalAssistant",
    description: "A personal assistant that remembers your preferences",
    instructions: "Remember user preferences and past conversations.",
    memory: aiMemory( "windowed", { maxMessages: 20 } )
)

// Tell the agent things
agent.run( "My favorite color is blue" )
agent.run( "I live in Boston" )
agent.run( "I work as a software developer" )

// Ask about remembered info
println( agent.run( "What's my favorite color?" ) )
// Output: "Your favorite color is blue!"

println( agent.run( "Where do I live and what do I do?" ) )
// Output: "You live in Boston and work as a software developer!"

// Clear memory when needed
agent.clearMemory()
```

### Memory Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    MEMORY FLOW                                  │
└─────────────────────────────────────────────────────────────────┘

  Turn 1                Turn 2                Turn 3
  ──────                ──────                ──────

  User: "I'm Alex"      User: "My name?"      User: "Summarize"
        │                     │                     │
        ▼                     ▼                     ▼
  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
  │   MEMORY    │      │   MEMORY    │      │   MEMORY    │
  │ [Alex msg]  │      │ [Alex msg]  │      │ [Alex msg]  │
  │             │      │ [name resp] │      │ [name resp] │
  │             │      │ [name? msg] │      │ [name? msg] │
  │             │      │             │      │ [sum msg]   │
  └─────────────┘      └─────────────┘      └─────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
  AI: "Hi Alex!"       AI: "Alex!"           AI: "You're Alex,
                                                  you asked..."
```

---

## 🛠️ Part 4: Complete Agent Example (20 mins)

Let's build a **Customer Support Agent**:

```java
// support-agent.bxs

println( "🎧 Customer Support Agent" )
println( "═".repeat( 50 ) )
println()

// Simulated database
orders = {
    "ORD-001": { status: "Shipped", item: "Widget Pro", customer: "Alex" },
    "ORD-002": { status: "Processing", item: "Gadget X", customer: "Jordan" },
    "ORD-003": { status: "Delivered", item: "Tool Kit", customer: "Sam" }
}

products = {
    "Widget Pro": { price: 99.99, stock: 50 },
    "Gadget X": { price: 149.99, stock: 0 },
    "Tool Kit": { price: 79.99, stock: 25 }
}

// Tool: Look up order
orderTool = aiTool(
    "lookup_order",
    "Look up order status by order ID",
    ( args ) => {
        orderId = args.orderId.uCase()
        if( orders.keyExists( orderId ) ) {
            order = orders[ orderId ]
            return "Order #orderId#: #order.item# - Status: #order.status#"
        }
        return "Order #orderId# not found"
    }
).describeOrderId( "The order ID (e.g., ORD-001)" )

// Tool: Check product
productTool = aiTool(
    "check_product",
    "Check product price and availability",
    ( args ) => {
        productName = args.productName
        for( name in products.keyList() ) {
            if( name.findNoCase( productName ) > 0 ) {
                product = products[ name ]
                stock = product.stock > 0 ? "In Stock (#product.stock#)" : "Out of Stock"
                return "#name#: $#product.price# - #stock#"
            }
        }
        return "Product not found. Available: #products.keyList()#"
    }
).describeProductName( "Product name to check" )

// Tool: Create ticket
ticketTool = aiTool(
    "create_ticket",
    "Create a support ticket for issues that need human review",
    ( args ) => {
        ticketId = "TKT-" & randRange( 1000, 9999 )
        return "Created ticket #ticketId#: #args.issue#. A human agent will follow up."
    }
).describeIssue( "Description of the issue" )

// Create the support agent
supportAgent = aiAgent(
    name: "SupportBot",
    description: "A customer support agent for order and product inquiries",
    instructions: "
        You are a helpful customer support agent.

        Guidelines:
        - Be friendly and professional
        - Look up orders when customers ask about their orders
        - Check product info when asked about prices or availability
        - Create a support ticket for complex issues
        - If you don't know something, offer to create a ticket

        Always ask if there's anything else you can help with.
    ",
    tools: [ orderTool, productTool, ticketTool ],
    memory: aiMemory( "windowed", { maxMessages: 10 } )
)

// Chat loop
println( "Hi! I'm your support assistant. How can I help?" )
println( "─".repeat( 50 ) )
println()

running = true
while( running ) {
    print( "You: " )
    userInput = readLine()

    if( userInput.trim() == "quit" || userInput.trim() == "bye" ) {
        running = false
        println( "SupportBot: Thanks for contacting us! Have a great day! 👋" )
    } else {
        try {
            response = supportAgent.run( userInput )
            println( "SupportBot: " & response )
            println()
        } catch( any e ) {
            println( "SupportBot: I'm having trouble right now. Please try again." )
            println()
        }
    }
}
```

### Sample Interaction

```
🎧 Customer Support Agent
══════════════════════════════════════════════════

Hi! I'm your support assistant. How can I help?
──────────────────────────────────────────────────

You: Hi, I ordered something last week
SupportBot: I'd be happy to help you check on your order!
           Could you provide your order ID? It starts with ORD-.

You: It's ORD-001
SupportBot: I found your order! Order ORD-001 for Widget Pro is currently
           Shipped. It should arrive soon! Is there anything else I can help with?

You: How much does the Gadget X cost?
SupportBot: The Gadget X is $149.99, but unfortunately it's currently
           Out of Stock. Would you like me to create a ticket to notify
           you when it's back in stock?

You: Yes please
SupportBot: Created ticket TKT-4721: Customer wants notification when
           Gadget X is back in stock. A human agent will follow up.
           Is there anything else I can help with?

You: bye
SupportBot: Thanks for contacting us! Have a great day! 👋
```

---

## 🧪 Part 5: Lab - Build Your Own Agent (20 mins)

### The Challenge

Build a **Research Agent** that can:
1. Search for information (simulated)
2. Summarize findings
3. Remember the conversation

### Requirements

- Has a `search` tool
- Has a `summarize` tool
- Uses memory
- Follows clear instructions

### Starter Code

```java
// research-agent.bxs

println( "🔍 Research Agent" )
println( "═".repeat( 40 ) )
println()

// Simulated knowledge base
knowledgeBase = {
    "boxlang": "BoxLang is a modern dynamic JVM language with CFML compatibility.",
    "java": "Java is a widely-used programming language for enterprise applications.",
    "ai": "Artificial Intelligence enables machines to simulate human intelligence.",
    "llm": "Large Language Models are AI systems trained on vast text datasets."
}

// TODO: Create search tool
searchTool = aiTool(
    "search",
    "Search the knowledge base for information",
    ( args ) => {
        query = args.query.lCase()
        for( topic in knowledgeBase.keyList() ) {
            if( query.findNoCase( topic ) > 0 ) {
                return "Found: " & knowledgeBase[ topic ]
            }
        }
        return "No results for '#args.query#'. Try: #knowledgeBase.keyList()#"
    }
).describeQuery( "What to search for" )

// TODO: Create summarize tool
summarizeTool = aiTool(
    "summarize",
    "Create a brief summary of given text",
    ( args ) => {
        text = args.text
        // Simple simulation - in real app, could use AI
        return "Summary: " & left( text, 100 ) & "..."
    }
).describeText( "Text to summarize" )

// TODO: Create the research agent
researchAgent = aiAgent(
    name: "Researcher",
    description: "A research agent that searches and summarizes information",
    instructions: "
        You are a research assistant.
        - Search for topics when asked
        - Provide clear explanations
        - Summarize when requested
        - Remember what the user has asked about
    ",
    tools: [ searchTool, summarizeTool ],
    memory: aiMemory( "windowed", { maxMessages: 10 } )
)

// Chat loop
println( "Ask me to research something!" )
println( "Topics I know: #knowledgeBase.keyList()#" )
println( "─".repeat( 40 ) )
println()

running = true
while( running ) {
    print( "You: " )
    userInput = readLine()

    if( userInput.trim() == "quit" ) {
        running = false
        println( "Goodbye! 📚" )
    } else {
        try {
            response = researchAgent.run( userInput )
            println( "Researcher: " & response )
            println()
        } catch( any e ) {
            println( "Error: " & e.message )
            println()
        }
    }
}
```

---

## ✅ Knowledge Check

1. **What makes an agent different from chat?**
   - [ ] Agents are faster
   - [x] Agents decide their own next steps
   - [ ] Agents cost more
   - [ ] Agents don't use tools

2. **What does aiAgent() return?**
   - [ ] A string response
   - [x] An agent object you can run
   - [ ] A tool collection
   - [ ] A memory object

3. **How does an agent remember context?**
   - [ ] It doesn't
   - [ ] Via API calls
   - [x] Using memory (aiMemory)
   - [ ] Using cookies

4. **What method executes an agent?**
   - [ ] agent.chat()
   - [x] agent.run()
   - [ ] agent.execute()
   - [ ] agent.start()

---

## 📝 Summary

You learned:

| Concept | Description |
|---------|-------------|
| **Agent** | Autonomous AI that plans and executes |
| **aiAgent()** | Creates an agent |
| **Memory** | Stores conversation history |
| **Instructions** | Guides agent behavior |
| **Tools** | Actions agent can take |

### Key Code Pattern

```java
// Create agent
agent = aiAgent(
    name: "MyAgent",
    description: "What it does",
    instructions: "How to behave",
    tools: [ tool1, tool2 ],
    memory: aiMemory( "windowed" )
)

// Use agent
response = agent.run( "User request" )
```

---

## 🌐 Bonus: Multi-Tenant Agents for Web Apps

**For web applications with multiple users**, you'll want to isolate each user's conversation:

### Why Multi-Tenant?

Without isolation:
```java
// ❌ BAD: All users share the same memory!
agent = aiAgent(
    memory: aiMemory( "windowed" )
)
// User Alice's data leaks to User Bob!
```

With isolation:
```java
// ✅ GOOD: Each user has their own memory
function getUserAgent( userId, conversationId ) {
    return aiAgent(
        name: "WebAssistant",
        instructions: "Be helpful and professional",
        memory: aiMemory( "session",
            key: "chat",
            userId: userId,              // Isolate per user
            conversationId: conversationId,  // Multiple chats per user
            config: { maxMessages: 50 }
        )
    )
}

// In your web handler:
function chat( event, rc, prc ) {
    userId = auth().user().getId()  // From authenticated session
    conversationId = rc.chatId ?: createUUID()

    agent = getUserAgent( userId, conversationId )
    response = agent.run( rc.message )

    return { response: response, conversationId: conversationId }
}
```

### Key Points

- 🔒 **Security**: Each user's data is isolated
- 💬 **Multiple Chats**: Users can have multiple conversations
- 📊 **Scalability**: Works across distributed servers (with cache/jdbc memory)
- 🎯 **Enterprise Ready**: Production-grade multi-tenancy

> **Learn More**: See the [Multi-Tenant Memory Guide](../../../docs/advanced/multi-tenant-memory.md) for enterprise patterns!

---

## 🎉 Congratulations!

You've completed the BoxLang AI Bootcamp! You now know:

```
┌─────────────────────────────────────────────────────────────────┐
│                  SKILLS ACQUIRED                                │
└─────────────────────────────────────────────────────────────────┘

  ✅ Lesson 1: Setup & First AI Call
  ✅ Lesson 2: Conversations & Messages
  ✅ Lesson 3: Switching Providers
  ✅ Lesson 4: Structured Output
  ✅ Lesson 5: AI Tools
  ✅ Lesson 6: Building Agents

  You can now:
  ───────────
  • Make AI calls with aiChat()
  • Build multi-turn conversations
  • Use OpenAI, Claude, and Ollama
  • Extract type-safe structured data
  • Create tools AI can use
  • Build autonomous agents
```

## ⏭️ What's Next?

### Deep Dive: Full Course

Take the [12-lesson course](../../course/) for:
- Streaming responses
- Pipeline workflows
- Advanced memory systems
- Production deployment
- Vector embeddings
- And much more!

### Explore Examples

Check out the [examples folder](../../examples/) for more code.

### Build Something!

The best way to learn is by doing. Try building:
- A customer service bot
- A code review assistant
- A data analysis agent
- A personal productivity helper

---

## 📁 Lesson Files

```
lesson-06-agents/
├── README.md (this file)
├── examples/
│   ├── basic-agent.bxs
│   ├── tool-agent.bxs
│   └── memory-agent.bxs
└── labs/
    ├── support-agent.bxs
    └── research-agent.bxs
```

---

**Thank you for completing the bootcamp! 🎓**

Questions? Visit [GitHub Issues](https://github.com/ortus-boxlang/bx-ai/issues)
