# Lesson 4: Structured Output

**⏱️ Duration: 60 minutes**

Getting text from AI is great, but what if you need **structured data**? In this lesson, you'll learn to extract type-safe objects, arrays, and complex data structures from AI responses.

## 🎯 What You'll Learn

- Extract structured data from AI responses
- Use BoxLang classes for type-safe output
- Use struct templates for quick extraction
- Extract arrays of objects
- Validate and transform AI output

---

## 📚 Part 1: Why Structured Output? (10 mins)

### The Problem

AI returns text, but you often need data:

```
┌─────────────────────────────────────────────────────────────────┐
│                   THE PROBLEM                                   │
└─────────────────────────────────────────────────────────────────┘

  Your Question:
  "Extract the person's name and age from: John is 30 years old"
  
  AI Response (Text):
  "The person's name is John and their age is 30."
  
  What You Actually Need:
  { name: "John", age: 30 }    ← STRUCTURED DATA!
```

### The Solution

BoxLang AI's **structured output** converts AI responses into proper data:

```
┌─────────────────────────────────────────────────────────────────┐
│                   THE SOLUTION                                  │
└─────────────────────────────────────────────────────────────────┘

  Text Input                    Structured Output
  ───────────                   ─────────────────
       │                              │
       ▼                              ▼
  ┌─────────┐     ┌─────────┐   ┌─────────────┐
  │ "John   │ ──▶ │   AI    │──▶│ { name:     │
  │ is 30   │     │ + Schema│   │   "John",   │
  │ years"  │     │         │   │   age: 30 } │
  └─────────┘     └─────────┘   └─────────────┘
```

### Benefits

- ✅ **Type Safety** - Get proper data types (strings, numbers, arrays)
- ✅ **Validation** - Schema ensures required fields exist
- ✅ **No Parsing** - No regex or string manipulation needed
- ✅ **Reliability** - Consistent output format

---

## 💻 Part 2: Using Struct Templates (15 mins)

The easiest way to get structured output is with a **struct template**.

### Basic Struct Template

```java
// struct-template.bxs
// Define the structure you want
template = {
    "name": "",
    "age": 0,
    "email": ""
}

// Ask AI to extract into this structure
result = aiChat( 
    "Extract person info: John Doe is 30 years old, email john@example.com" 
).structuredOutput( template )

// Use the result as a normal struct!
println( "Name: " & result.name )
println( "Age: " & result.age )
println( "Email: " & result.email )
```

### How It Works

```
┌─────────────────────────────────────────────────────────────────┐
│              STRUCT TEMPLATE FLOW                               │
└─────────────────────────────────────────────────────────────────┘

  1. Template          2. AI Extracts        3. You Get Data
  ───────────          ──────────────        ───────────────
  
  {                    "John Doe is 30       {
    name: "",          years old..."           name: "John Doe",
    age: 0,                 │                   age: 30,
    email: ""               ▼                   email: "john@..."
  }                    AI understands        }
                       the structure!
```

### More Template Examples

**Product Info:**
```java
productTemplate = {
    "name": "",
    "price": 0.0,
    "inStock": true,
    "category": ""
}

product = aiChat( 
    "Product: MacBook Pro, $2499, available, category: Electronics" 
).structuredOutput( productTemplate )

println( "#product.name# - $#product.price#" )
```

**Address:**
```java
addressTemplate = {
    "street": "",
    "city": "",
    "state": "",
    "zip": "",
    "country": ""
}

address = aiChat( 
    "Parse: 123 Main St, Boston, MA 02101, USA" 
).structuredOutput( addressTemplate )
```

---

## 🏗️ Part 3: Using Classes (15 mins)

For reusable structured output, define a **BoxLang class**.

### Define a Class

```java
// Person.bx
class {
    property name="name" type="string";
    property name="age" type="numeric";
    property name="email" type="string";
}
```

### Use the Class

```java
// class-extraction.bxs
// Extract into Person class
result = aiChat( 
    "Extract: Sarah Connor, 35, sarah@resistance.org" 
).structuredOutput( new Person() )

// Use getters!
println( "Name: " & result.getName() )
println( "Age: " & result.getAge() )
println( "Email: " & result.getEmail() )
```

### Complex Class Example

```java
// Order.bx
class {
    property name="orderId" type="string";
    property name="customerName" type="string";
    property name="total" type="numeric";
    property name="items" type="array";
    property name="status" type="string";
}
```

```java
// Extract order from text
order = aiChat( "
    Order #12345 for Jane Smith
    Total: $156.99
    Items: Widget (x2), Gadget (x1)
    Status: Shipped
" ).structuredOutput( new Order() )

println( "Order #order.getOrderId()# - $#order.getTotal()#" )
```

### Class vs Struct: When to Use

| Use Struct Templates | Use Classes |
|---------------------|-------------|
| Quick, one-off extraction | Reusable data structures |
| Simple flat data | Complex nested data |
| Prototyping | Production code |
| Ad-hoc queries | Defined entities |

---

## 📚 Part 4: Extracting Arrays (10 mins)

Need to extract **multiple items**? Use array syntax!

### Array of Structs

```java
// array-extraction.bxs
// Template for ONE task
taskTemplate = {
    "title": "",
    "priority": "",
    "done": false
}

// Wrap in array to extract MULTIPLE tasks
tasks = aiChat( "
    Extract tasks:
    1. Buy groceries (high priority, not done)
    2. Call mom (medium, done)
    3. Exercise (low, not done)
" ).structuredOutput( [ taskTemplate ] )

// Loop through results
for( task in tasks ) {
    status = task.done ? "✅" : "⬜"
    println( "#status# #task.title# [#task.priority#]" )
}
```

**Output:**
```
⬜ Buy groceries [high]
✅ Call mom [medium]
⬜ Exercise [low]
```

### Array of Classes

```java
// Product.bx
class {
    property name="name" type="string";
    property name="price" type="numeric";
}
```

```java
// Extract multiple products
products = aiChat( "
    Parse products:
    - iPhone 15: $999
    - AirPods Pro: $249
    - MacBook Air: $1299
" ).structuredOutput( [ new Product() ] )

total = 0
for( product in products ) {
    println( "#product.getName()#: $#product.getPrice()#" )
    total += product.getPrice()
}
println( "─".repeat( 30 ) )
println( "Total: $#total#" )
```

---

## 🧪 Part 5: Lab - Invoice Parser (10 mins)

### The Goal

Build an invoice parser that extracts:
- Invoice number
- Date
- Customer name
- Line items (array)
- Total amount

### Invoice Text

```
INVOICE #INV-2024-001

Date: November 15, 2024
Bill To: Acme Corporation

Items:
  - Web Development: $5,000
  - Design Services: $2,500
  - Hosting (1 year): $500

Total: $8,000
```

### Solution

```java
// invoice-parser.bxs

// Define line item structure
lineItemTemplate = {
    "description": "",
    "amount": 0.0
}

// Define invoice structure
invoiceTemplate = {
    "invoiceNumber": "",
    "date": "",
    "customerName": "",
    "lineItems": [ lineItemTemplate ],
    "total": 0.0
}

// Sample invoice text
invoiceText = "
INVOICE #INV-2024-001

Date: November 15, 2024
Bill To: Acme Corporation

Items:
  - Web Development: $5,000
  - Design Services: $2,500
  - Hosting (1 year): $500

Total: $8,000
"

// Extract structured data
invoice = aiChat( "Parse this invoice: " & invoiceText )
    .structuredOutput( invoiceTemplate )

// Display results
println( "📄 Invoice Parser Results" )
println( "═".repeat( 40 ) )
println()
println( "Invoice #: " & invoice.invoiceNumber )
println( "Date: " & invoice.date )
println( "Customer: " & invoice.customerName )
println()
println( "Line Items:" )

calculatedTotal = 0
for( item in invoice.lineItems ) {
    println( "  • #item.description#: $#numberFormat( item.amount, ',.00' )#" )
    calculatedTotal += item.amount
}

println()
println( "─".repeat( 40 ) )
println( "Total: $#numberFormat( invoice.total, ',.00' )#" )

// Verify total
if( calculatedTotal == invoice.total ) {
    println( "✅ Total verified!" )
} else {
    println( "⚠️ Total mismatch! Calculated: $#calculatedTotal#" )
}
```

### Expected Output

```
📄 Invoice Parser Results
════════════════════════════════════════

Invoice #: INV-2024-001
Date: November 15, 2024
Customer: Acme Corporation

Line Items:
  • Web Development: $5,000.00
  • Design Services: $2,500.00
  • Hosting (1 year): $500.00

────────────────────────────────────────
Total: $8,000.00
✅ Total verified!
```

---

## ✅ Knowledge Check

1. **What does structuredOutput() return?**
   - [ ] A string
   - [x] A struct or object matching your template
   - [ ] JSON text
   - [ ] An error

2. **How do you extract an array of items?**
   - [ ] Use extractArray()
   - [x] Wrap your template in [ ]
   - [ ] Call structuredOutput() multiple times
   - [ ] Use a loop

3. **When should you use a class instead of struct?**
   - [ ] Never
   - [x] For reusable, complex data structures
   - [ ] Only for arrays
   - [ ] When using Ollama

4. **What makes structured output reliable?**
   - [x] AI follows a schema to ensure correct format
   - [ ] It uses special servers
   - [ ] It requires Claude
   - [ ] It only works with numbers

---

## 📝 Summary

You learned:

| Concept | Description |
|---------|-------------|
| **Struct Template** | Quick structured extraction with `{}` |
| **Class** | Reusable type-safe extraction |
| **Arrays** | Extract multiple items with `[ ]` |
| **structuredOutput()** | Method to extract structured data |

### Key Code Patterns

```java
// Struct template
result = aiChat( "text" ).structuredOutput( { name: "", age: 0 } )

// Class
result = aiChat( "text" ).structuredOutput( new Person() )

// Array
results = aiChat( "text" ).structuredOutput( [ { item: "" } ] )

// Array of classes
results = aiChat( "text" ).structuredOutput( [ new Task() ] )
```

---

## ⏭️ Next Lesson

Now you can extract structured data! Let's learn to give AI access to real-world functions.

👉 **[Lesson 5: AI Tools](../lesson-05-tools/)**

---

## 📁 Lesson Files

```
lesson-04-structured-output/
├── README.md (this file)
├── examples/
│   ├── struct-template.bxs
│   ├── class-extraction.bxs
│   ├── array-extraction.bxs
│   └── Person.bx
└── labs/
    └── invoice-parser.bxs
```
