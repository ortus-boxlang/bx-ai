# AI Guardrails

The BoxLang AI module provides a comprehensive guardrail system for validating and protecting AI interactions. Guardrails intercept AI requests and responses to enforce safety policies, filter content, protect sensitive information, and ensure compliance with organizational rules.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Built-in Guardrails](#built-in-guardrails)
  - [Content Filter](#content-filter)
  - [PII Redaction](#pii-redaction)
  - [Token Limit](#token-limit)
- [Guardrail Chain](#guardrail-chain)
- [Creating Custom Guardrails](#creating-custom-guardrails)
- [Event Integration](#event-integration)
- [Best Practices](#best-practices)

---

## Overview

Guardrails are safety mechanisms that:

- **Validate** content before it's sent to AI providers
- **Filter** prohibited content in requests and responses
- **Redact** personally identifiable information (PII)
- **Limit** token usage to prevent excessive costs
- **Block** or **warn** based on configurable policies

### Key Concepts

1. **Guardrail**: A single validation rule or policy
2. **GuardrailResult**: The outcome of a guardrail check (pass, fail, warning)
3. **GuardrailChain**: A collection of guardrails executed in sequence
4. **ChainResult**: Aggregated results from a chain execution

---

## Quick Start

### Creating a Simple Guardrail

```java
// Create a content filter guardrail
contentFilter = aiGuardrail( "contentFilter" )
    .block( "prohibited" )
    .block( "illegal" )
    .warn( "suspicious" );

// Create a PII detection guardrail
piiGuardrail = aiGuardrail( "pii" )
    .setAction( "redact" );

// Create a token limit guardrail
tokenGuardrail = aiGuardrail( "tokenLimit" )
    .setMaxTokens( 4000 );
```

### Using a Guardrail Chain

```java
// Create a chain with multiple guardrails
chain = aiGuardrailChain( [
    aiGuardrail( "contentFilter" ).block( "harmful" ),
    aiGuardrail( "pii" ).setAction( "redact" ),
    aiGuardrail( "tokenLimit" ).setMaxTokens( 4000 )
] );

// Register the chain as an event interceptor
chain.register();

// Now all AI requests will be validated automatically!
result = aiChat( "Hello, my email is john@example.com" );
```

---

## Built-in Guardrails

### Content Filter

Filters AI requests and responses for prohibited content patterns.

```java
// Create a content filter
filter = aiGuardrail( "contentFilter" );

// Add blocked patterns (will fail validation)
filter.block( "prohibited" )
      .block( "illegal" )
      .blockAll( [ "dangerous", "harmful" ] );

// Add warning patterns (will warn but allow)
filter.warn( "maybe" )
      .warnAll( [ "perhaps", "possibly" ] );

// Configure options
filter.configure( {
    checkRequests: true,    // Check outgoing requests
    checkResponses: true,   // Check incoming responses
    caseInsensitive: true   // Case-insensitive matching
} );
```

#### Pattern Types

- **Simple strings**: Exact substring matches
- **Regex patterns**: Full regex support for complex patterns

```java
// Block credit card patterns using regex
filter.block( "\d{4}-\d{4}-\d{4}-\d{4}" );

// Block email patterns
filter.block( "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}" );
```

### PII Redaction

Detects and optionally redacts Personally Identifiable Information.

```java
pii = aiGuardrail( "pii" );

// Set action: "block", "redact", or "warn"
pii.setAction( "redact" );

// Enable/disable specific PII types
pii.setPIIType( "email", true )
   .setPIIType( "phone", true )
   .setPIIType( "ssn", true )
   .setPIIType( "creditCard", true )
   .setPIIType( "ipAddress", false )
   .setPIIType( "dateOfBirth", false );

// Enable all PII detection
pii.enableAll();

// Or disable all
pii.disableAll();
```

#### Built-in PII Patterns

| Type | Description | Replacement |
|------|-------------|-------------|
| email | Email addresses | [EMAIL] |
| phone | US phone numbers | [PHONE] |
| ssn | Social Security Numbers | [SSN] |
| creditCard | Credit card numbers | [CREDIT_CARD] |
| ipAddress | IPv4 addresses | [IP_ADDRESS] |
| dateOfBirth | Date of birth patterns | [DOB] |

#### Custom PII Patterns

```java
pii.addPattern( "employeeId", "EMP-\d{6}", "[EMPLOYEE_ID]" )
   .addPattern( "customerId", "CUS-[A-Z]{2}\d{4}", "[CUSTOMER_ID]" );
```

### Token Limit

Validates that AI requests don't exceed configured token limits.

```java
tokenLimit = aiGuardrail( "tokenLimit" );

// Set maximum tokens for entire request
tokenLimit.setMaxTokens( 4000 );

// Set maximum tokens per individual message
tokenLimit.setMaxTokensPerMessage( 1000 );

// Set warning threshold (percentage)
tokenLimit.setWarningThreshold( 0.8 );  // Warn at 80%

// Configure estimation method
tokenLimit.setEstimationMethod( "characters" );  // or "words"
```

#### Token Estimation

Since exact tokenization varies by model, the guardrail uses estimation:

- **Characters method**: ~4 characters per token (default)
- **Words method**: ~0.75 words per token

```java
// Get estimated tokens for text
estimate = tokenLimit.estimateTokens( "Your text here" );
```

---

## Guardrail Chain

Chains execute multiple guardrails in sequence.

### Creating a Chain

```java
// Empty chain with configuration
chain = aiGuardrailChain( [], {
    stopOnFirstFailure: true,   // Stop on first failure
    throwOnFailure: true,       // Throw exception on failure
    name: "MyGuardrailChain"
} );

// Add guardrails
chain.add( aiGuardrail( "contentFilter" ).block( "test" ) )
     .add( aiGuardrail( "pii" ) );

// Or initialize with guardrails
chain = aiGuardrailChain( [
    aiGuardrail( "contentFilter" ),
    aiGuardrail( "pii" ),
    aiGuardrail( "tokenLimit" )
] );
```

### Manual Validation

```java
// Validate a request
chainResult = chain.validateRequest( {
    aiRequest: myRequest,
    dataPacket: myDataPacket,
    provider: myProvider
} );

// Check results
if ( chainResult.isPassed() ) {
    // All guardrails passed
} else if ( chainResult.hasWarnings() ) {
    // Some warnings but allowed to proceed
} else {
    // Validation failed
    failures = chainResult.getFailures();
}
```

### Automatic Event Registration

```java
// Register chain as an interceptor
chain.register();

// Now all onAIRequest and onAIResponse events are intercepted
// The chain will throw GuardrailViolation on failures

// Later, unregister if needed
chain.unregister();
```

### Chain Result Methods

```java
result = chain.validateRequest( data );

// Status checks
result.isPassed();      // All guardrails passed
result.hasFailed();     // Any guardrails failed
result.hasWarnings();   // Any warnings

// Get results by status
result.getResults();    // All results
result.getFailures();   // Only failures
result.getWarnings();   // Only warnings
result.getPassed();     // Only passed

// Get counts
counts = result.getCounts();
// { total: 3, passed: 2, failed: 0, warnings: 1 }

// Get violations
violations = result.getAllViolations();

// Summary
summary = result.getSummary();
// "All 3 guardrails passed" or "1 failed, 1 warnings, 1 passed"

// Serialization
struct = result.toStruct();
json = result.toJSON();
```

---

## Creating Custom Guardrails

Extend `AiBaseGuardrail` to create custom guardrails:

```java
// models/guardrails/CustomGuardrail.bx
class extends="bxModules.bxai.models.guardrails.AiBaseGuardrail" {

    function init( struct config = {} ) {
        super.init(
            name: "CustomGuardrail",
            description: "My custom validation logic",
            config: config
        );
        return this;
    }

    // Validate requests
    GuardrailResult function validateRequest( required struct data ) {
        var messages = data.aiRequest?.getMessages() ?: [];
        var content = extractTextFromMessages( messages );

        // Your validation logic
        if ( containsProhibitedContent( content ) ) {
            return fail( "Prohibited content detected", [ "violation details" ] );
        }

        if ( containsSuspiciousContent( content ) ) {
            return warning( "Suspicious content found" );
        }

        return pass();
    }

    // Validate responses
    GuardrailResult function validateResponse( required struct data ) {
        var content = extractTextFromResponse( data.response ?: {} );

        // Your response validation logic
        if ( shouldBlock( content ) ) {
            return fail( "Response blocked" );
        }

        return pass();
    }

    private boolean function containsProhibitedContent( content ) {
        // Your logic here
    }
}
```

### Using Custom Guardrails

```java
// Use with full class path
customGuardrail = aiGuardrail( "models.guardrails.CustomGuardrail", {
    myOption: "value"
} );

chain.add( customGuardrail );
```

---

## Event Integration

Guardrails integrate with the existing event system via these interception points:

### Guardrail Events

| Event | When | Data |
|-------|------|------|
| `onAIGuardrailValidation` | After guardrail validation | `{ guardrail, result, data }` |
| `onAIGuardrailViolation` | When guardrail fails | `{ guardrail, result, data }` |

### Using with Other Events

The guardrail chain hooks into:
- `onAIRequest` - Validates outgoing requests
- `onAIResponse` - Validates incoming responses

```java
// Register a chain that automatically intercepts AI operations
chain = aiGuardrailChain( [
    aiGuardrail( "contentFilter" ).block( "harmful" ),
    aiGuardrail( "pii" ).setAction( "redact" )
] ).register();

// All subsequent AI operations are now protected
result = aiChat( "What is harmful content?" );  // Will be blocked
```

---

## Best Practices

### 1. Layer Your Guardrails

Use multiple guardrails for defense in depth:

```java
chain = aiGuardrailChain( [
    // First: Block obvious violations
    aiGuardrail( "contentFilter" ).block( "illegal" ),

    // Second: Protect sensitive data
    aiGuardrail( "pii" ).setAction( "redact" ),

    // Third: Limit costs
    aiGuardrail( "tokenLimit" ).setMaxTokens( 4000 )
] );
```

### 2. Use Appropriate Actions

- **block**: For clear policy violations
- **redact**: For PII that can be safely removed
- **warn**: For suspicious but not necessarily bad content

### 3. Configure Per Environment

```java
if ( getEnvironment() == "production" ) {
    pii.setAction( "redact" );
} else {
    pii.setAction( "warn" );
}
```

### 4. Log Violations

```java
chain.onResultCallback( function( result, data ) {
    if ( result.isFailed() ) {
        writeLog(
            text: "Guardrail violation: #result.getMessage()#",
            type: "warning",
            log: "security"
        );
    }
} );
```

### 5. Handle Exceptions Gracefully

```java
try {
    result = aiChat( userInput );
} catch ( GuardrailViolation e ) {
    // Inform user their request was blocked
    return "Sorry, your request could not be processed due to content policies.";
}
```

### 6. Test Your Guardrails

Create test cases for:
- Content that should pass
- Content that should fail
- Edge cases and false positives

---

## Next Steps

- **[Event System](events.md)** - Learn about all AI events
- **[Pipeline Overview](../main-components/overview.md)** - Understanding AI pipelines
- **[Service-Level Chatting](../chatting/service-chatting.md)** - Direct service control

---

**Copyright** © 2023-2025 Ortus Solutions, Corp
