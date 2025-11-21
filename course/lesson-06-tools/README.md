# Lesson 6: Function Calling (Tools)

**Duration:** 90 minutes
**Prerequisites:** Lessons 1-5 completed

## Learning Objectives

By the end of this lesson, you will:

- Understand AI function calling (tools)
- Define tools with aiTool()
- Implement tool handlers
- Build AI agents with tool access
- Create practical tool-based applications

---

## Part 1: What are Tools?

### The Problem

```java
// AI doesn't know current information
aiChat( "What's the current temperature?" )
// Response: "I don't have access to real-time data"
```

### The Solution: Tools

```java
// Define a tool
weatherTool = aiTool()
    .setName( "get_weather" )
    .setDescription( "Get current weather for a location" )
    .setParameters( {
        type: "object",
        properties: {
            location: { type: "string", description: "City name" }
        },
        required: [ "location" ]
    } )
    .setHandler( ( args ) => {
        // Call real weather API
        return "72°F and sunny in #args.location#"
    } )

// AI can now use the tool!
answer = aiChat(
    "What's the weather in Seattle?",
    { tools: [ weatherTool ] }
)
```

---

## Part 2: Creating Tools

### Basic Tool

```java
calculatorTool = aiTool()
    .setName( "calculate" )
    .setDescription( "Perform mathematical calculations" )
    .setParameters( {
        type: "object",
        properties: {
            expression: {
                type: "string",
                description: "Math expression like '5 * 10'"
            }
        }
    } )
    .setHandler( ( args ) => {
        return evaluate( args.expression )
    } )
```

### Multi-Parameter Tool

```java
databaseTool = aiTool()
    .setName( "query_database" )
    .setDescription( "Query the database" )
    .setParameters( {
        type: "object",
        properties: {
            table: { type: "string" },
            condition: { type: "string" },
            limit: { type: "number", default: 10 }
        },
        required: [ "table" ]
    } )
    .setHandler( ( args ) => {
        // Execute database query
        return queryExecute(
            "SELECT * FROM #args.table# WHERE #args.condition# LIMIT #args.limit#"
        )
    } )
```

---

## Part 3: Using Tools

### Single Tool

```java
answer = aiChat(
    "What is 15 * 23?",
    { tools: [ calculatorTool ] }
)
// AI will call calculatorTool.handler({ expression: "15 * 23" })
// Then include result in response
```

### Multiple Tools

```java
tools = [
    weatherTool,
    calculatorTool,
    databaseTool
]

answer = aiChat(
    "What's the weather in Boston and how many users do we have?",
    { tools: tools }
)
// AI may call multiple tools to answer
```

---

## Examples to Run

### 1. `calculator-tool.bxs`
Basic calculator tool

### 2. `weather-tool.bxs`
API integration tool

### 3. `database-tool.bxs`
Database query tool

### 4. `multi-tool-agent.bxs`
Agent with multiple tools

---

## Lab Exercise: Tool-Based Assistant

**File:** `labs/tool-assistant.bxs`

**Objective:**
Build an assistant with multiple tools (calculator, time, search).

**Requirements:**
1. Create at least 3 tools
2. Tool handlers must work correctly
3. AI should choose appropriate tools
4. Handle tool errors gracefully
5. Show which tools were called

---

## Key Takeaways

✅ Tools give AI access to external data/functions
✅ Define with aiTool() - name, description, parameters, handler
✅ AI decides when and how to use tools
✅ Multiple tools enable complex agents
✅ Tool handlers execute your BoxLang code

---

## Next Lesson

**Lesson 7: Memory Systems** - Give AI long-term memory.
