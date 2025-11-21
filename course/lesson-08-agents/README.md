# Lesson 8: AI Agents

**Duration:** 90 minutes
**Prerequisites:** Lessons 1-7 completed

## Learning Objectives

By the end of this lesson, you will:

- Understand AI agents vs. simple chat
- Build autonomous agents with tools
- Implement agent planning and execution
- Create specialized agent roles
- Handle agent errors and loops

---

## Part 1: What is an AI Agent?

### Simple Chat (What we've done)

```java
// You control the flow
answer = aiChat( "Search for X" )
// You handle the search
// You ask follow-up
```

### AI Agent (Autonomous)

```java
// Agent controls the flow
agent = aiAgent()
    .setTools( [ searchTool, calculatorTool ] )
    .setGoal( "Research topic X and calculate stats" )

result = agent.run()
// Agent decides:
// 1. Search for X
// 2. Analyze results
// 3. Calculate statistics
// 4. Return report
```

---

## Part 2: Creating Agents

### Basic Agent

```java
agent = aiAgent()
    .setName( "Research Assistant" )
    .setInstructions( "You research topics and provide detailed reports" )
    .setTools( [ webSearchTool, calculatorTool ] )

result = agent.run( "Research BoxLang and count its features" )
```

### Agent with Memory

```java
agent = aiAgent()
    .setName( "Personal Assistant" )
    .setMemory( aiMemory( "windowed", { maxMessages: 20 } ) )
    .setTools( [ calendarTool, emailTool, todoTool ] )

// Agent remembers across calls
agent.run( "Schedule meeting with John" )
agent.run( "What meetings do I have this week?" )
```

---

## Part 3: Agent Patterns

### Research Agent

```java
researchAgent = aiAgent()
    .setName( "Researcher" )
    .setInstructions( "
        You are a thorough researcher.
        1. Search for information
        2. Verify from multiple sources
        3. Summarize findings
    " )
    .setTools( [ searchTool, fetchWebpageTool ] )
```

### Code Assistant Agent

```java
codeAgent = aiAgent()
    .setName( "Code Helper" )
    .setInstructions( "
        You help with coding tasks.
        1. Search documentation
        2. Write code
        3. Test code
        4. Explain solution
    " )
    .setTools( [
        searchDocsTool,
        runCodeTool,
        lintTool
    ] )
```

---

## Examples to Run

### 1. `basic-agent.bxs`
Simple autonomous agent

### 2. `research-agent.bxs`
Information gathering agent

### 3. `code-agent.bxs`
Programming assistant agent

### 4. `multi-agent.bxs`
Multiple agents working together

---

## Lab Exercise: Research Assistant Agent

**File:** `labs/research-agent.bxs`

**Objective:**
Build an agent that researches topics and creates reports.

**Requirements:**
1. Create search and summarize tools
2. Agent should plan multi-step research
3. Generate structured report
4. Handle missing information
5. Cite sources

---

## Key Takeaways

✅ Agents are autonomous AI systems
✅ Agents plan and execute multi-step tasks
✅ Tools give agents capabilities
✅ Memory makes agents context-aware
✅ Good instructions guide agent behavior

---

## Next Lesson

**Lesson 9: Advanced Pipelines** - Build complex AI workflows.
