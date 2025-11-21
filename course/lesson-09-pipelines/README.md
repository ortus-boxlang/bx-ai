# Lesson 9: Advanced Pipelines

**Duration:** 90 minutes  
**Prerequisites:** Lessons 1-8 completed

## Learning Objectives

By the end of this lesson, you will:

- Understand runnable pipelines
- Chain multiple AI operations
- Build RAG (Retrieval Augmented Generation) systems
- Implement document processing pipelines
- Create reusable pipeline components

---

## Part 1: What are Pipelines?

### Simple Call (What we've done)

```java
answer = aiChat( "Translate this" )
```

### Pipeline (Chained operations)

```java
pipeline = aiModel( "openai" )
    .to( aiTransform( data => data.toUpper() ) )
    .to( aiTransform( data => data.trim() ) )

result = pipeline.run( "input" )
```

---

## Part 2: Building Pipelines

### Data Transformation Pipeline

```java
// Clean → Analyze → Format
pipeline = aiModel( "openai" )
    .to( aiTransform( data => {
        // Clean data
        return data.trim().reReplace( "[^a-zA-Z0-9\s]", "", "ALL" )
    } ) )
    .to( aiModel( "openai" ).withPrompt( "Analyze sentiment" ) )
    .to( aiTransform( data => {
        // Format output
        return {
            result: data,
            timestamp: now()
        }
    } ) )

result = pipeline.run( "This is great!!" )
```

---

## Part 3: RAG (Retrieval Augmented Generation)

### What is RAG?

1. **Retrieve** relevant documents
2. **Augment** prompt with context
3. **Generate** response with AI

```java
// RAG Pipeline
ragPipeline = aiTransform( query => {
        // Search documents
        return searchDocuments( query )
    } )
    .to( aiTransform( docs => {
        // Build context
        return "Context: " & docs.toList() & "\n\nQuestion: " & query
    } ) )
    .to( aiModel( "openai" ) )

answer = ragPipeline.run( "How do I install BoxLang?" )
```

---

## Examples to Run

### 1. `basic-pipeline.bxs`
Simple transformation pipeline

### 2. `rag-pipeline.bxs`
Document retrieval + generation

### 3. `document-processor.bxs`
Process multiple documents

### 4. `multi-stage-pipeline.bxs`
Complex multi-step workflow

---

## Lab Exercise: Document Processing Pipeline

**File:** `labs/document-pipeline.bxs`

**Objective:**  
Build a pipeline that processes documents through multiple stages.

---

## Key Takeaways

✅ Pipelines chain AI operations  
✅ Use aiTransform() for data manipulation  
✅ RAG combines retrieval + generation  
✅ Pipelines are reusable and composable  
✅ Great for complex workflows

---

## Next Lesson

**Lesson 10: Production Deployment** - Deploy AI apps to production.
