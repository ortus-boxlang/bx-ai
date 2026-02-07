/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.ai.audit;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Test cases for AuditSanitizer - redacts sensitive data from audit entries
 */
public class AuditSanitizerTest extends BaseIntegrationTest {

	@BeforeAll
	public static void setup() {
		BaseIntegrationTest.setup();
	}

	@BeforeEach
	public void setupEach() {
		super.setupEach();
	}

	@Test
	@DisplayName( "Test AuditSanitizer instantiation with defaults" )
	public void testDefaultInstantiation() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()
		    patterns = sanitizer.getPatterns()
		    redactValue = sanitizer.getRedactValue()
		    """,
		    context
		);

		var patterns = variables.getAsArray( Key.of( "patterns" ) );
		assertThat( patterns.size() ).isGreaterThan( 0 );
		assertThat( variables.getAsString( Key.of( "redactValue" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test AuditSanitizer with custom config" )
	public void testCustomConfig() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer( {
		        sanitizePatterns: [ "ssn", "creditCard", "bankAccount" ],
		        redactValue: "***HIDDEN***"
		    } )

		    patterns = sanitizer.getPatterns()
		    redactValue = sanitizer.getRedactValue()
		    """,
		    context
		);

		var patterns = variables.getAsArray( Key.of( "patterns" ) );
		assertThat( patterns ).contains( "ssn" );
		assertThat( patterns ).contains( "creditCard" );
		assertThat( patterns ).contains( "bankAccount" );
		assertThat( variables.getAsString( Key.of( "redactValue" ) ) ).isEqualTo( "***HIDDEN***" );
	}

	@Test
	@DisplayName( "Test sanitize redacts password fields" )
	public void testSanitizePassword() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        username: "john",
		        password: "secret123",
		        userPassword: "mysecret"
		    }

		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "username" ) ) ).isEqualTo( "john" );
		assertThat( result.getAsString( Key.of( "password" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "userPassword" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test sanitize redacts apiKey fields" )
	public void testSanitizeApiKey() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        apiKey: "sk-1234567890",
		        api_key: "pk-0987654321",
		        openaiApiKey: "sk-proj-abc123"
		    }

		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "apiKey" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "api_key" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "openaiApiKey" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test sanitize redacts token fields" )
	public void testSanitizeToken() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        access_token: "access-123",
		        refresh_token: "refresh-456",
		        auth_token: "auth-789",
		        session_token: "session-abc"
		    }

		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "access_token" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "refresh_token" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "auth_token" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "session_token" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test sanitize redacts secret fields" )
	public void testSanitizeSecret() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        secret: "my-secret",
		        clientSecret: "client-secret-123",
		        secretKey: "secret-key-456"
		    }

		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "secret" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "clientSecret" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "secretKey" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test sanitize redacts credential fields" )
	public void testSanitizeCredential() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        credentials: "cred-data",
		        awsCredential: "aws-cred-123"
		    }

		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "credentials" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "awsCredential" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test sanitize redacts authorization fields" )
	public void testSanitizeAuthorization() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        authorization: "Bearer xyz123",
		        bearerAuth: "Basic abc123"
		    }

		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "authorization" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "bearerAuth" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test sanitize handles nested structs" )
	public void testSanitizeNestedStructs() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        user: {
		            name: "John",
		            password: "secret123"
		        },
		        config: {
		            endpoint: "https://api.example.com",
		            apiKey: "auth-token"
		        }
		    }

		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var	result	= variables.getAsStruct( Key.of( "result" ) );
		var	user	= ( IStruct ) result.get( Key.of( "user" ) );
		assertThat( user.getAsString( Key.of( "name" ) ) ).isEqualTo( "John" );
		assertThat( user.getAsString( Key.of( "password" ) ) ).isEqualTo( "[REDACTED]" );

		var config = ( IStruct ) result.get( Key.of( "config" ) );
		assertThat( config.getAsString( Key.of( "endpoint" ) ) ).isEqualTo( "https://api.example.com" );
		assertThat( config.getAsString( Key.of( "apiKey" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test sanitize handles arrays" )
	public void testSanitizeArrays() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        items: [
		            { name: "item1", apiKey: "key1" },
		            { name: "item2", apiKey: "key2" }
		        ]
		    }

		    result = sanitizer.sanitize( data )
		    items = result.items
		    """,
		    context
		);

		var	items	= variables.getAsArray( Key.of( "items" ) );
		var	item1	= ( IStruct ) items.get( 0 );
		var	item2	= ( IStruct ) items.get( 1 );

		assertThat( item1.getAsString( Key.of( "name" ) ) ).isEqualTo( "item1" );
		assertThat( item1.getAsString( Key.of( "apiKey" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( item2.getAsString( Key.of( "name" ) ) ).isEqualTo( "item2" );
		assertThat( item2.getAsString( Key.of( "apiKey" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test sanitize preserves non-sensitive data" )
	public void testPreservesNonSensitiveData() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        name: "John Doe",
		        email: "john@example.com",
		        age: 30,
		        active: true,
		        tags: [ "user", "premium" ]
		    }

		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "name" ) ) ).isEqualTo( "John Doe" );
		assertThat( result.getAsString( Key.of( "email" ) ) ).isEqualTo( "john@example.com" );
		assertThat( result.get( "age" ) ).isEqualTo( 30 );
		assertThat( result.getAsBoolean( Key.of( "active" ) ) ).isTrue();
		assertThat( result.getAsArray( Key.of( "tags" ) ).size() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test sanitize with case insensitive matching" )
	public void testCaseInsensitiveMatching() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()

		    data = {
		        PASSWORD: "secret1",
		        Password: "secret2",
		        APIKEY: "key1",
		        ApiKey: "key2"
		    }

		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "PASSWORD" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "Password" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "APIKEY" ) ) ).isEqualTo( "[REDACTED]" );
		assertThat( result.getAsString( Key.of( "ApiKey" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test sanitize truncates long strings" )
	public void testTruncateLongStrings() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer( {
		        maxInputSize: 100
		    } )

		    longText = repeatString( "a", 200 )
		    data = { content: longText }

		    result = sanitizer.sanitize( data )
		    resultLen = len( result.content )
		    """,
		    context
		);

		var resultLen = variables.getAsInteger( Key.of( "resultLen" ) );
		// 100 chars + "... [TRUNCATED]"
		assertThat( resultLen ).isLessThan( 200 );
	}

	@Test
	@DisplayName( "Test addPattern adds new pattern" )
	public void testAddPattern() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()
		        .addPattern( "customField" )

		    data = { customFieldValue: "sensitive-data" }
		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "customFieldValue" ) ) ).isEqualTo( "[REDACTED]" );
	}

	@Test
	@DisplayName( "Test removePattern removes a pattern" )
	public void testRemovePattern() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()
		        .removePattern( "password" )

		    data = { password: "should-not-be-redacted" }
		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "password" ) ) ).isEqualTo( "should-not-be-redacted" );
	}

	@Test
	@DisplayName( "Test setRedactValue changes redaction string" )
	public void testSetRedactValue() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer()
		        .setRedactValue( "***CENSORED***" )

		    data = { password: "secret" }
		    result = sanitizer.sanitize( data )
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "password" ) ) ).isEqualTo( "***CENSORED***" );
	}

	@Test
	@DisplayName( "Test sanitize with isOutput flag" )
	public void testSanitizeWithOutputFlag() {
		runtime.executeSource(
		    """
		    sanitizer = new bxModules.bxai.models.audit.AuditSanitizer( {
		        maxInputSize: 50,
		        maxOutputSize: 100
		    } )

		    longText = repeatString( "a", 75 )
		    data = { content: longText }

		    // Input mode - should truncate at 50
		    inputResult = sanitizer.sanitize( data, {}, false )
		    inputLen = len( inputResult.content )

		    // Output mode - should truncate at 100
		    outputResult = sanitizer.sanitize( data, {}, true )
		    outputLen = len( outputResult.content )
		    """,
		    context
		);

		var	inputLen	= variables.getAsInteger( Key.of( "inputLen" ) );
		var	outputLen	= variables.getAsInteger( Key.of( "outputLen" ) );

		// Input should be truncated (50 + truncation indicator)
		assertThat( inputLen ).isLessThan( 75 );
		// Output should not be truncated since 75 < 100
		assertThat( outputLen ).isEqualTo( 75 );
	}

}
