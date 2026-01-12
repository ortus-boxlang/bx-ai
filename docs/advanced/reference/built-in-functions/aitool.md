# aiTool

Create callable function tools that AI agents can use to gather information or take actions.

## Syntax

```javascript
aiTool(name, description, callable)
```

## Parameters

| Parameter     | Type     | Required | Description                                                   |
| ------------- | -------- | -------- | ------------------------------------------------------------- |
| `name`        | string   | Yes      | Unique function name (used by AI to identify the tool)        |
| `description` | string   | No       | Describes what the tool does (helps AI decide when to use it) |
| `callable`    | function | No       | The function the AI will execute                              |

## Returns

Returns a `Tool` instance that can be passed to AI agents or models.

## Examples

### Basic Tool

```javascript
// Simple weather tool
weatherTool = aiTool(
    "get_weather",
    "Get current weather for a location",
    ( location ) => {
        return "72°F and sunny in #location#";
    }
);

// Use with agent
agent = aiAgent(
    name: "WeatherBot",
    tools: [ weatherTool ]
);

response = agent.run( "What's the weather in Paris?" );
// Agent automatically calls get_weather("Paris")
```

### Tool with Multiple Parameters

```javascript
calculateTool = aiTool(
    "calculate",
    "Perform mathematical calculations",
    ( operation, num1, num2 ) => {
        switch( operation ) {
            case "add": return num1 + num2;
            case "subtract": return num1 - num2;
            case "multiply": return num1 * num2;
            case "divide": return num1 / num2;
        }
    }
);
```

### Database Lookup Tool

```javascript
userLookupTool = aiTool(
    "lookup_user",
    "Find user information by email or ID",
    ( identifier ) => {
        // Query database
        user = queryExecute(
            "SELECT * FROM users WHERE email = :id OR id = :id",
            { id: identifier }
        );

        if ( !user.recordCount ) {
            return "User not found";
        }

        return jsonSerialize({
            name: user.name,
            email: user.email,
            status: user.status
        });
    }
);
```

### API Integration Tool

```javascript
searchTool = aiTool(
    "search_docs",
    "Search documentation for specific topics",
    ( query ) => {
        return http( "https://api.example.com/search" )
            .method( "POST" )
			.asJson()
            .body( { q: query } )
            .send()
			.toList( char( 10 ) )
    }
);
```

### Multiple Tools

```javascript
// Create multiple tools
tools = [
    aiTool(
        "get_time",
        "Get current time",
        () => dateTimeFormat( now(), "full" )
    ),
    aiTool(
        "get_date",
        "Get current date",
        () => dateFormat( now(), "full" )
    ),
    aiTool(
        "calculate_age",
        "Calculate age from birth date",
        ( birthDate ) => dateDiff( "yyyy", birthDate, now() )
    )
];

// Use with agent
agent = aiAgent(
    name: "Helper",
    tools: tools
);
```

### Tool with Rich Output

```javascript
analyticsTool = aiTool(
    "get_analytics",
    "Get website analytics for specified date range",
    ( startDate, endDate ) => {
        data = getAnalyticsData( startDate, endDate );

        return {
            pageViews: data.views,
            uniqueVisitors: data.visitors,
            avgDuration: data.avgTime,
            topPages: data.popular.slice( 1, 5 ),
            summary: "Analytics from #startDate# to #endDate#"
        }.toJSON();
    }
);
```

### Tool with Validation

```javascript
emailTool = aiTool(
    "send_email",
    "Send an email to a recipient",
    ( recipient, subject, body ) => {
        // Validate
        if ( !isValid( "email", recipient ) ) {
            return "Error: Invalid email address";
        }

        // Send email
        mailService.send(
            to: recipient,
            subject: subject,
            body: body
        );

        return "Email sent successfully to #recipient#";
    }
);
```

## Tool Function Requirements

### Return Value

* Must return a value that can be cast to a string
* Return JSON strings for complex data: `return data.toJSON()`
* Return clear error messages on failure

### Parameters

* Parameters automatically inferred from function signature
* Parameter descriptions can be added via `@param` tags in function comments
* AI will pass arguments based on context

### Error Handling

```javascript
tool = aiTool(
    "risky_operation",
    "Performs an operation that might fail",
    ( input ) => {
        try {
            return performOperation( input );
        } catch( any e ) {
            // Return error as string
            return "Error: #e.message#";
        }
    }
);
```

## Use Cases

### ✅ External Data Retrieval

Fetch real-time data from APIs, databases, or files.

### ✅ Calculations

Perform complex computations AI can't do directly.

### ✅ System Actions

Execute system commands, file operations, etc.

### ✅ API Integrations

Connect AI to external services and APIs.

### ✅ Database Operations

Query or update database records.

### ❌ Heavy Processing

Don't use for long-running operations - AI requests time out.

### ❌ Stateful Operations

Keep tools stateless - AI may call multiple times.

## Tool Selection

AI automatically selects tools based on:

1. **Tool name**: Clear, descriptive names help AI choose correctly
2. **Description**: Detailed descriptions improve tool selection
3. **Context**: User query and conversation context
4. **Parameters**: Parameter names and types guide AI usage

```javascript
// ✅ Good tool definition
aiTool(
    "search_customer_orders",
    "Search for customer orders by email, order ID, or date range. Returns order details including status, items, and totals.",
    ( searchTerm ) => searchOrders( searchTerm )
);

// ❌ Poor tool definition
aiTool(
    "search",
    "Search stuff",
    ( x ) => search( x )
);
```

## Notes

* **Automatic invocation**: AI decides when to call tools based on user input
* **Multiple calls**: AI may call same tool multiple times in one response
* **Parameter inference**: AI determines arguments from context
* **Return format**: Always return string or JSON-serializable data
* **Tool descriptions**: Critical for helping AI choose correct tool
* **No async**: Tool functions must be synchronous

## Related Functions

* [`aiAgent()`](aiagent.md) - Use tools with agents
* [`aiModel()`](aimodel.md) - Attach tools to models

## Best Practices

```javascript
// ✅ Clear, descriptive names
aiTool("get_customer_by_email", "...", fn);

// ✅ Detailed descriptions
aiTool(
    "search_products",
    "Search product catalog by name, SKU, category, or price range. Returns up to 10 matching products with details.",
    searchFn
);

// ✅ Return JSON for complex data
( params ) => {
    return { status: "success", data: results }.toJSON();
}

// ✅ Handle errors gracefully
( input ) => {
    try {
        return process( input );
    } catch( any e ) {
        return "Error: #e.message#";
    }
}

// ❌ Don't use generic names
aiTool("tool1", "does stuff", fn); // Bad

// ❌ Don't return complex objects
( x ) => return queryObject; // Bad - return JSON string instead
```
