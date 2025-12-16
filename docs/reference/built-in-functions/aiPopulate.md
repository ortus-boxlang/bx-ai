# aiPopulate

Populate a class instance, struct, or array from JSON data or struct. This is useful for working with structured AI output, testing, custom workflows, or deserializing cached AI responses.

## Syntax

```javascript
aiPopulate(target, data)
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `target` | any | Yes | The target to populate - class instance, array with single instance `[new MyClass()]`, or struct template |
| `data` | any | Yes | The data to populate from - JSON string or struct |

### Target Types

- **Class Instance**: `new Employee()` - Populates properties
- **Array**: `[new Employee()]` - Populates array of instances
- **Struct**: `{ name: "", age: 0 }` - Returns deserialized data (validation template)

## Returns

Returns the populated target:
- **Class Instance**: The same instance with populated properties
- **Array**: Array of populated class instances
- **Struct**: Deserialized data struct

## Examples

### Basic Class Population

```javascript
// Define a class
class Employee {
    property name="firstName" type="string";
    property name="lastName" type="string";
    property name="age" type="numeric";
}

// Populate from JSON
jsonData = '{"firstName":"John","lastName":"Doe","age":30}';
employee = aiPopulate( new Employee(), jsonData );

println( employee.getFirstName() ); // "John"
println( employee.getAge() ); // 30
```

### From Struct

```javascript
// Populate from struct instead of JSON
structData = {
    firstName: "Jane",
    lastName: "Smith",
    age: 28
};

employee = aiPopulate( new Employee(), structData );
```

### Array Population

```javascript
// Populate array of instances
jsonArray = '[
    {"firstName":"John","lastName":"Doe"},
    {"firstName":"Jane","lastName":"Smith"},
    {"firstName":"Bob","lastName":"Jones"}
]';

employees = aiPopulate( [new Employee()], jsonArray );

println( "Total employees: #employees.len()#" );
employees.each( emp => {
    println( "#emp.getFirstName()# #emp.getLastName()#" );
});
```

### With AI Response

```javascript
// Use with structured AI output
class Task {
    property name="title" type="string";
    property name="priority" type="string";
    property name="completed" type="boolean";
}

// Get structured response from AI
response = aiChat(
    message: "Create 3 tasks for a project",
    params: {
        response_format: { type: "json_object" }
    },
    options: { returnFormat: "raw" }
);

// Populate from AI response
tasks = aiPopulate(
    [new Task()],
    response.choices[1].message.content
);
```

### Cached Response Population

```javascript
// Reuse cached AI responses
function getEmployeeData( employeeId ) {
    cacheKey = "employee_#employeeId#";

    // Check cache
    if ( cacheExists( cacheKey ) ) {
        cachedData = cacheGet( cacheKey );
        return aiPopulate( new Employee(), cachedData );
    }

    // Fetch fresh data
    response = aiChat( "Get employee #employeeId# data" );
    cacheSet( cacheKey, response );

    return aiPopulate( new Employee(), response );
}
```

### Testing Without AI

```javascript
// Test AI workflows without API calls
function processUserProfile( profileData ) {
    profile = aiPopulate( new UserProfile(), profileData );

    // Business logic
    if ( profile.getAge() < 18 ) {
        profile.setAccountType( "minor" );
    }

    return profile;
}

// Test with mock data
testData = { name: "Test User", age: 16, email: "test@example.com" };
result = processUserProfile( testData );

// No AI API call needed for testing
```

### Struct Validation Template

```javascript
// Use struct as validation template
template = {
    title: "",
    priority: "medium",
    completed: false
};

// Populate and validate structure
taskData = '{"title":"Build feature","priority":"high"}';
task = aiPopulate( template, taskData );

println( task.title ); // "Build feature"
println( task.priority ); // "high"
println( task.completed ); // false (default preserved)
```

### Complex Nested Objects

```javascript
class Address {
    property name="street" type="string";
    property name="city" type="string";
    property name="zipCode" type="string";
}

class Person {
    property name="name" type="string";
    property name="age" type="numeric";
    property name="address" type="any"; // Will be populated as struct
}

jsonData = '{
    "name": "John Doe",
    "age": 30,
    "address": {
        "street": "123 Main St",
        "city": "New York",
        "zipCode": "10001"
    }
}';

person = aiPopulate( new Person(), jsonData );
println( person.getAddress().city ); // "New York"
```

### API Response Mapping

```javascript
// Map external API to internal class
class Product {
    property name="id" type="string";
    property name="name" type="string";
    property name="price" type="numeric";
}

// External API response
apiResponse = http( "https://api.example.com/products/123" )
	.asJson()
    .send()

// Populate from API
product = aiPopulate( new Product(), apiResponse );
```

### Batch Processing

```javascript
// Process multiple items
class Invoice {
    property name="invoiceNumber" type="string";
    property name="amount" type="numeric";
    property name="status" type="string";
}

// Get batch data
jsonBatch = '[
    {"invoiceNumber":"INV-001","amount":1500,"status":"paid"},
    {"invoiceNumber":"INV-002","amount":2300,"status":"pending"},
    {"invoiceNumber":"INV-003","amount":890,"status":"paid"}
]';

invoices = aiPopulate( [new Invoice()], jsonBatch );

// Filter and process
unpaid = invoices.filter( inv => inv.getStatus() == "pending" );
totalUnpaid = unpaid.reduce( ( sum, inv ) => sum + inv.getAmount(), 0 );
```

### Error Handling

```javascript
// Handle population errors
try {
    employee = aiPopulate( new Employee(), invalidJSON );
} catch ( any e ) {
    if ( e.type == "InvalidArgument" ) {
        println( "Invalid data format: #e.message#" );
        // Fallback or retry logic
    }
}
```

### With Default Values

```javascript
class Config {
    property name="timeout" type="numeric" default="30";
    property name="retries" type="numeric" default="3";
    property name="debug" type="boolean" default="false";
}

// Partial data - defaults preserved
partialData = '{"timeout":60}';
config = aiPopulate( new Config(), partialData );

println( config.getTimeout() ); // 60 (from data)
println( config.getRetries() ); // 3 (default)
println( config.getDebug() ); // false (default)
```

### Dynamic Class Population

```javascript
// Populate different classes based on type
function populateByType( type, data ) {
    switch( type ) {
        case "employee":
            return aiPopulate( new Employee(), data );
        case "customer":
            return aiPopulate( new Customer(), data );
        case "product":
            return aiPopulate( new Product(), data );
        default:
            throw( "Unknown type: #type#" );
    }
}

// Use dynamically
result = populateByType( "employee", jsonData );
```

### Workflow Integration

```javascript
// Complete workflow: AI → Populate → Process
function createTasksFromPrompt( prompt ) {
    // 1. Get AI response
    response = aiChat(
        message: prompt,
        params: { response_format: { type: "json_object" } },
        options: { returnFormat: "raw" }
    );

    // 2. Parse and populate
    tasks = aiPopulate(
        [new Task()],
        response.choices[1].message.content
    );

    // 3. Save to database
    tasks.each( task => {
        taskService.save( task );
    });

    return tasks;
}

tasks = createTasksFromPrompt( "Create tasks for website redesign project" );
```

## Notes

- 🎯 **Type Safety**: Maps JSON/struct data to typed class properties
- 🔄 **Reusability**: Populate same class from different data sources
- 🧪 **Testing**: Test AI workflows without actual API calls
- 💾 **Caching**: Deserialize cached AI responses efficiently
- 📦 **Flexibility**: Works with classes, arrays, and structs
- ⚡ **Performance**: No reflection overhead - direct property setting
- 🔧 **Integration**: Seamlessly integrates with structured output workflows

## Related Functions

- [`aiChat()`](aiChat.md) - Get structured AI responses
- [`aiService()`](aiService.md) - Direct service invocation
- [`aiMessage()`](aiMessage.md) - Build structured prompts

## Best Practices

✅ **Use for structured output** - Pair with `response_format: { type: "json_object" }`

✅ **Test without AI** - Use mock data for development and testing

✅ **Cache expensive calls** - Populate from cached JSON to avoid API costs

✅ **Define clear classes** - Use typed properties for validation

✅ **Handle arrays properly** - Wrap class instance in array: `[new Class()]`

✅ **Validate input** - Wrap in try/catch for production robustness

❌ **Don't skip validation** - Always validate JSON structure before populating

❌ **Don't ignore defaults** - Class property defaults are preserved

❌ **Don't assume structure** - Check AI response format before populating
