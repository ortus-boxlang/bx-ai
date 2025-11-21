# Lesson 4: Model Parameters

**Duration:** 45 minutes  
**Prerequisites:** Lessons 1-3 completed

## Learning Objectives

By the end of this lesson, you will:

- Understand key model parameters (temperature, max_tokens, top_p)
- Control AI creativity and randomness
- Optimize token usage
- Implement parameter presets for different use cases
- Use stop sequences effectively

---

## Part 1: Understanding Parameters

### What are Model Parameters?

Parameters control **how** the AI generates responses:

- **Temperature** - Controls randomness (0.0 = deterministic, 2.0 = creative)
- **max_tokens** - Maximum response length
- **top_p** - Nucleus sampling (alternative to temperature)
- **frequency_penalty** - Reduces repetition
- **presence_penalty** - Encourages new topics
- **stop** - Stop sequences to end generation

---

## Part 2: Temperature

### What is Temperature?

Temperature controls **creativity vs. consistency**:

```java
// Temperature 0 - Deterministic, consistent
answer = aiChat( "Say hello", { temperature: 0.0 } )
// Always: "Hello! How can I help you today?"

// Temperature 1 - Balanced (default)
answer = aiChat( "Say hello", { temperature: 1.0 } )
// Varied: "Hi there!", "Hello!", "Hey! What's up?"

// Temperature 2 - Very creative
answer = aiChat( "Say hello", { temperature: 2.0 } )
// Wild: "Greetings, earthling!", "Salutations!"
```

### Use Cases

| Temperature | Use Case | Example |
|-------------|----------|---------|
| 0.0 - 0.3 | Factual, consistent | Math, code, facts |
| 0.4 - 0.7 | Balanced | General chat, Q&A |
| 0.8 - 1.2 | Creative | Story writing, brainstorming |
| 1.3 - 2.0 | Experimental | Art, poetry, wild ideas |

---

## Part 3: Max Tokens

### Token Limits

```java
// Short response (50 tokens ≈ 37 words)
answer = aiChat(
    "Explain arrays",
    { max_tokens: 50 }
)

// Medium response (200 tokens ≈ 150 words)
answer = aiChat(
    "Explain arrays",
    { max_tokens: 200 }
)

// Long response (1000 tokens ≈ 750 words)
answer = aiChat(
    "Explain arrays",
    { max_tokens: 1000 }
)
```

### Cost Optimization

```java
// Expensive - Unlimited response
answer = aiChat( "Write an essay..." )
// Could generate 4000+ tokens = $$$

// Optimized - Limited response
answer = aiChat(
    "Write an essay summary in 200 words",
    { max_tokens: 300 }
)
// Maximum 300 tokens = predictable cost
```

---

## Part 4: Advanced Parameters

### Top P (Nucleus Sampling)

```java
// top_p 0.1 - Only most likely tokens
answer = aiChat(
    "Complete: The sky is...",
    { top_p: 0.1 }
)
// Output: "blue"

// top_p 0.9 - Broader selection
answer = aiChat(
    "Complete: The sky is...",
    { top_p: 0.9 }
)
// Output: "blue", "gray", "beautiful", etc.
```

### Frequency Penalty

```java
// No penalty - May repeat
answer = aiChat(
    "List benefits of exercise",
    { frequency_penalty: 0.0 }
)

// With penalty - Less repetition
answer = aiChat(
    "List benefits of exercise",
    { frequency_penalty: 0.5 }
)
```

### Presence Penalty

```java
// Encourage topic diversity
answer = aiChat(
    "Discuss programming",
    { presence_penalty: 0.6 }
)
// Will introduce varied subtopics
```

### Stop Sequences

```java
// Stop at specific text
answer = aiChat(
    "List 3 fruits: ",
    { stop: [ "4.", "\n\n" ] }
)
// Stops after listing 3, or at double newline
```

---

## Part 5: Parameter Presets

### Preset Patterns

```java
// Factual mode
FACTUAL = {
    temperature: 0.1,
    max_tokens: 500,
    top_p: 0.1
}

// Creative mode
CREATIVE = {
    temperature: 1.2,
    max_tokens: 1000,
    top_p: 0.9
}

// Balanced mode
BALANCED = {
    temperature: 0.7,
    max_tokens: 500,
    top_p: 0.8
}

// Code mode
CODE = {
    temperature: 0.2,
    max_tokens: 1000,
    frequency_penalty: 0.3
}

// Use presets
answer = aiChat( "Explain variables", FACTUAL )
story = aiChat( "Write a story", CREATIVE )
```

---

## Examples to Run

### 1. `temperature-demo.bxs`
See how temperature affects responses

### 2. `token-limits.bxs`
Control response length

### 3. `parameter-presets.bxs`
Reusable parameter configurations

### 4. `cost-optimization.bxs`
Reduce costs with smart parameters

---

## Lab Exercise: Parameter Tuner

**File:** `labs/parameter-tuner.bxs`

**Objective:**  
Create a tool that tests different parameter combinations and shows results.

**Requirements:**
1. Test same prompt with different temperatures
2. Test same prompt with different max_tokens
3. Compare response quality and cost
4. Create reusable presets
5. Show recommendations

---

## Key Takeaways

✅ Temperature controls creativity (0 = consistent, 2 = wild)  
✅ max_tokens limits response length and cost  
✅ Use low temperature for facts, high for creativity  
✅ Presets make parameter management easier  
✅ Always set max_tokens for cost control

---

## Next Lesson

**Lesson 5: Streaming Responses** - Learn real-time streaming for better UX.
