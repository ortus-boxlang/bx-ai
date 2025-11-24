# Lesson 12: Structured Output - Type-Safe AI Responses

**Duration:** 60 minutes
**Prerequisites:** Lessons 1-4 completed, comfortable with BoxLang classes

## Learning Objectives

By the end of this lesson, you will:
- Understand why structured output matters for production AI apps
- Use BoxLang classes to define AI response schemas
- Extract data from unstructured text into typed objects
- Work with arrays of structured data
- Use struct definitions for quick schemas
- Handle multiple schemas in one request
- Manually populate objects with `aiPopulate()`
- Integrate structured output into pipelines

---

## Part 1: The Problem with Unstructured Responses

### Traditional AI Responses

When you ask AI a question, you get text back:

```java
answer = aiChat( "Extract person info: John Doe, age 30, john@example.com" )
println( answer )
// "The person is John Doe, 30 years old, email: john@example.com"
```

**Problems:**
-  Must parse manually (error-prone)
- Format varies between requests
- Hard to validate
- Can't use type safety
- Breaks easily with prompt changes

### Real-World Scenario

```java
// Extract 100 resumes
resumes.each( resume => {
    answer = aiChat( "Extract name and email from: #resume#" )

    // How do you parse this reliably?
    // "Name: John Doe, Email: john@example.com"
    // "John Doe (john@example.com)"
    // "The candidate is John Doe. You can reach them at john@example.com"

    // Different formats = parsing nightmare!
} )
```

---

## Part 2: Introduction to Structured Output

### What is Structured Output?

**Structured output** forces AI to return data in your exact format - every time.

```java
// Define your structure
class Person {
    property name="firstName" type="string";
    property name="lastName" type="string";
    property name="age" type="numeric";
    property name="email" type="string";
}

// Get structured response
person = aiChat(
    "Extract: John Doe, age 30, john@example.com",
    options = { returnFormat = new Person() }
)

// Use typed object
println( person.getFirstName() )  // "John"
println( person.getAge() )        // 30 (numeric!)
println( person.getEmail() )      // "john@example.com"
```

### How It Works

1. **Schema Generation**: BoxLang AI converts your class to JSON schema
2. **AI Constraint**: Schema sent to AI model to constrain output format
3. **Validation**: AI response validated against schema
4. **Population**: Data automatically fills your class instance

---

## Part 3: Your First Structured Output

### Step 1: Create a Simple Class

Create `Contact.bx`:

```java
class {
    property name="name" type="string";
    property name="email" type="string";
    property name="phone" type="string";
}
```

### Step 2: Use It

```java
contact = aiChat(
    "Extract contact: Alice Smith, alice@example.com, 555-1234",
    options = { returnFormat = new Contact() }
)

println( "Name: #contact.getName()#" )
println( "Email: #contact.getEmail()#" )
println( "Phone: #contact.getPhone()#" )
```

### Step 3: Try Different Inputs

```java
// Works with various formats
contact1 = aiChat(
    "Alice Smith (alice@example.com) Phone: 555-1234",
    options = { returnFormat = new Contact() }
)

contact2 = aiChat(
    "Call Alice at 555-1234 or email alice@example.com",
    options = { returnFormat = new Contact() }
)

// Both return Contact objects with same structure!
```

---

## Part 4: Using Struct Definitions

Don't want to create a class file? Use a struct!

### Quick Struct Schema

```java
// Define structure inline
template = {
    title: "",
    author: "",
    published: "",
    pages: 0
}

book = aiChat(
    "Extract book info: '1984' by George Orwell, published 1949, 328 pages",
    options = { returnFormat = template }
)

println( "Title: #book.title#" )
println( "Author: #book.author#" )
println( "Pages: #book.pages#" )
```

### When to Use What

**Use Classes When:**
- Building production applications
- Need methods on objects
- Want type safety across codebase
- Reusing structure in multiple places

**Use Structs When:**
- Prototyping
- One-off scripts
- Simple data extraction
- Quick experiments

---

## Part 5: Working with Arrays

Extract multiple items at once:

### Array Syntax

```java
class Task {
    property name="title" type="string";
    property name="priority" type="string";
    property name="dueDate" type="string";
}

// Note the array syntax: [new Task()]
tasks = aiChat(
    "Extract tasks:
    - Fix login bug (high priority, due Friday)
    - Update documentation (low priority, due next week)
    - Refactor code (medium priority, no deadline)",
    options = { returnFormat = [new Task()] }
)

// Returns array of Task objects
println( "Found #tasks.len()# tasks" )

tasks.each( task => {
    println( "[#task.getPriority()#] #task.getTitle()# - Due: #task.getDueDate()#" )
} )
```

### Real-World: Resume Parser

```java
class Experience {
    property name="company" type="string";
    property name="role" type="string";
    property name="years" type="numeric";
}

resume = "
    Senior Developer at Google (5 years)
    Junior Developer at Microsoft (2 years)
    Intern at Amazon (1 year)
"

experience = aiChat(
    "Extract work history: #resume#",
    options = { returnFormat = [new Experience()] }
)

totalYears = experience.reduce( (sum, exp) => sum + exp.getYears(), 0 )
println( "Total Experience: #totalYears# years" )
```

---

## Part 6: Multiple Schemas

Extract different data structures in ONE request:

```java
class Person {
    property name="name" type="string";
    property name="title" type="string";
}

class Company {
    property name="name" type="string";
    property name="industry" type="string";
}

class Meeting {
    property name="date" type="string";
    property name="time" type="string";
    property name="location" type="string";
}

// Extract ALL at once
result = aiModel( "openai" )
    .structuredOutputs([
        { name: "person", schema: new Person() },
        { name: "company", schema: new Company() },
        { name: "meeting", schema: new Meeting() }
    ])
    .run( "
        John Smith, VP of Engineering at TechCorp (Software Industry)
        wants to meet Tuesday at 2pm in Conference Room A
    " )

// Access each structure
println( "Person: #result.person.getName()# (#result.person.getTitle()#)" )
println( "Company: #result.company.getName()# (#result.company.getIndustry()#)" )
println( "Meeting: #result.meeting.getDate()# at #result.meeting.getTime()#" )
```

---

## Part 7: The aiPopulate() BIF

Manually populate objects from JSON (useful for testing):

```java
// From JSON string
jsonData = '{"name":"John Doe","email":"john@example.com","phone":"555-1234"}'
contact = aiPopulate( new Contact(), jsonData )

// From struct
data = { name: "Jane Smith", email: "jane@example.com", phone: "555-5678" }
contact = aiPopulate( new Contact(), data )

// From array
jsonArray = '[{"name":"Alice"},{"name":"Bob"}]'
contacts = aiPopulate( [new Contact()], jsonArray )
```

### Use Cases for aiPopulate()

1. **Testing**: Populate test data without AI calls
2. **Caching**: Hydrate objects from cached responses
3. **Database**: Convert DB query results to typed objects
4. **APIs**: Parse API JSON into BoxLang classes

---

## Part 8: Structured Output in Pipelines

Combine structured output with message templates:

```java
// Reusable extraction pipeline
template = aiMessage()
    .system( "Extract ${dataType} information accurately" )
    .user( "Data: ${input}" )

pipeline = template.to( aiModel().structuredOutput( new Contact() ) )

// Use multiple times
contact1 = pipeline.run( {
    dataType: "contact",
    input: "Alice Smith, alice@example.com"
} )

contact2 = pipeline.run( {
    dataType: "contact",
    input: "Bob Jones, bob@example.com"
} )
```

---

## Part 9: Best Practices

### 1. Use Descriptive Names

```java
// ✅ GOOD
class {
    property name="customerEmail" type="string";
    property name="orderTotal" type="numeric";
    property name="shippingAddress" type="string";
}

// ❌ BAD
class {
    property name="e" type="string";
    property name="t" type="numeric";
    property name="a" type="string";
}
```

### 2. Always Specify Types

```java
// ✅ GOOD
class {
    property name="age" type="numeric";
    property name="active" type="boolean";
    property name="score" type="numeric";
}

// ❌ BAD (defaults to string)
class {
    property name="age";
    property name="active";
    property name="score";
}
```

### 3. Provide Clear Instructions

```java
// ✅ GOOD
person = aiChat(
    "Extract person information from this text: John Doe, age 30",
    options = { returnFormat = new Person() }
)

// ❌ BAD (vague)
person = aiChat(
    "john doe 30",
    options = { returnFormat = new Person() }
)
```

### 4. Use Defaults for Optional Fields

```java
class {
    property name="name" type="string" default="";
    property name="email" type="string" default="";
    property name="phone" type="string" default="";  // Might be missing
}
```

---

## Part 10: Common Use Cases

### Invoice Processing

```java
class Invoice {
    property name="invoiceNumber" type="string";
    property name="vendor" type="string";
    property name="amount" type="numeric";
    property name="dueDate" type="string";
    property name="items" type="array";
}

invoice = aiChat( invoiceText, options = { returnFormat = new Invoice() } )

// Ready for database insert
invoiceService.create( invoice )
```

### Sentiment Analysis

```java
sentiment = aiChat(
    "Analyze sentiment: This product exceeded my expectations!",
    options = {
        returnFormat = {
            sentiment: "",      // positive/negative/neutral
            confidence: 0.0,    // 0.0 to 1.0
            keywords: []
        }
    }
)

if( sentiment.confidence > 0.8 && sentiment.sentiment == "positive" ) {
    // Highlight as positive review
}
```

### Log Parsing

```java
class LogEntry {
    property name="timestamp" type="string";
    property name="level" type="string";
    property name="message" type="string";
    property name="errorCode" type="string";
}

log = "2024-11-25 14:23:45 [ERROR] Database connection failed - Code: DB_001"

entry = aiChat(
    "Parse log entry: #log#",
    options = { returnFormat = new LogEntry() }
)

if( entry.getLevel() == "ERROR" ) {
    alertService.notify( entry )
}
```

---

## Summary

**What You Learned:**
✅ Why structured output matters for production apps
✅ Creating and using BoxLang classes for schemas
✅ Using struct definitions for quick schemas
✅ Extracting arrays of structured data
✅ Working with multiple schemas simultaneously
✅ Using aiPopulate() for manual object population
✅ Integrating structured output into pipelines
✅ Best practices for reliable data extraction

**Key Takeaways:**
- Structured output = type-safe, reliable AI responses
- Classes for production, structs for prototyping
- Array syntax: `[new MyClass()]` for multiple items
- Multiple schemas with `structuredOutputs()`
- `aiPopulate()` for testing and caching

---

## Next Steps

**Practice:**
- Complete the labs to build real-world extractors
- Try structured output with your own data
- Experiment with complex nested structures

**Advanced Topics:**
- Class inheritance with structured output
- JSON schema for maximum control
- Error handling and validation
- Performance optimization

**Related Lessons:**
- Lesson 4: Parameters - Control AI behavior
- Lesson 9: Pipelines - Compose complex workflows
- Lesson 10: Production - Deploy AI apps reliably
