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
package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class aiRequestContextTest extends BaseIntegrationTest {

	@DisplayName( "Can create an AiRequest with context via options" )
	@Test
	public void testAiRequestWithContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				contextData = {
					userId: "user-123",
					tenantId: "tenant-456",
					roles: ["admin", "editor"],
					ragDocuments: ["doc1", "doc2"]
				}

				request = aiChatRequest(
					messages: "Hello",
					options: { context: contextData }
				)

				result = request.getContext()
				hasContext = request.hasContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "request" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 4 );
		assertThat( resultContext.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-123" );
		assertThat( resultContext.getAsString( Key.of( "tenantId" ) ) ).isEqualTo( "tenant-456" );
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isTrue();
	}

	@DisplayName( "Can create an AiRequest without context" )
	@Test
	public void testAiRequestWithoutContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				request = aiChatRequest(
					messages: "Hello"
				)

				result = request.getContext()
				hasContext = request.hasContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "request" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 0 );
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isFalse();
	}

	@DisplayName( "Can add context to an existing AiRequest" )
	@Test
	public void testAddContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				request = aiChatRequest( messages: "Hello" )
					.addContext( "userId", "user-123" )
					.addContext( "role", "admin" )

				result = request.getContext()
				hasContext = request.hasContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "request" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 2 );
		assertThat( resultContext.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-123" );
		assertThat( resultContext.getAsString( Key.of( "role" ) ) ).isEqualTo( "admin" );
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isTrue();
	}

	@DisplayName( "Can merge context into an existing AiRequest" )
	@Test
	public void testMergeContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				request = aiChatRequest(
					messages: "Hello",
					options: { context: { userId: "user-123" } }
				)
				.mergeContext( { tenantId: "tenant-456", role: "admin" } )

				result = request.getContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "request" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 3 );
		assertThat( resultContext.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-123" );
		assertThat( resultContext.getAsString( Key.of( "tenantId" ) ) ).isEqualTo( "tenant-456" );
		assertThat( resultContext.getAsString( Key.of( "role" ) ) ).isEqualTo( "admin" );
	}

	@DisplayName( "Merge context can override existing values" )
	@Test
	public void testMergeContextOverride() {

		// @formatter:off
		runtime.executeSource(
		    """
				request = aiChatRequest(
					messages: "Hello",
					options: { context: { userId: "old-user", role: "viewer" } }
				)
				.mergeContext( { userId: "new-user" } )

				result = request.getContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "request" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.getAsString( Key.of( "userId" ) ) ).isEqualTo( "new-user" );
		assertThat( resultContext.getAsString( Key.of( "role" ) ) ).isEqualTo( "viewer" );
	}

	@DisplayName( "Can get a specific context value" )
	@Test
	public void testGetContextValue() {

		// @formatter:off
		runtime.executeSource(
		    """
				request = aiChatRequest(
					messages: "Hello",
					options: { context: { userId: "user-123", role: "admin" } }
				)

				userId = request.getContextValue( "userId" )
				role = request.getContextValue( "role" )
				missing = request.getContextValue( "missing" )
				missingWithDefault = request.getContextValue( "missing", "default-value" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-123" );
		assertThat( variables.getAsString( Key.of( "role" ) ) ).isEqualTo( "admin" );
		assertThat( variables.getAsString( Key.of( "missing" ) ) ).isEqualTo( "" );
		assertThat( variables.getAsString( Key.of( "missingWithDefault" ) ) ).isEqualTo( "default-value" );
	}

	@DisplayName( "Can set context directly" )
	@Test
	public void testSetContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				request = aiChatRequest( messages: "Hello" )
				request.setContext( { userId: "user-123", permissions: ["read", "write"] } )

				result = request.getContext()
				hasContext = request.hasContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "request" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 2 );
		assertThat( resultContext.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-123" );
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isTrue();
	}

	@DisplayName( "Context is available in interceptors" )
	@Test
	public void testContextInInterceptors() {

		// @formatter:off
		runtime.executeSource(
		    """
				// Create a request with context
				contextData = {
					securityLevel: "high",
					ragContext: "Retrieved documents about BoxLang"
				}

				request = aiChatRequest(
					messages: "Hello",
					options: { context: contextData }
				)

				// Verify context is accessible
				result = request.getContext()
				secLevel = request.getContextValue( "securityLevel" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "request" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.getAsString( Key.of( "securityLevel" ) ) ).isEqualTo( "high" );
		assertThat( resultContext.getAsString( Key.of( "ragContext" ) ) ).isEqualTo( "Retrieved documents about BoxLang" );
	}

	@DisplayName( "Embedding request can have context" )
	@Test
	public void testEmbeddingRequestWithContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				import bxModules.bxai.models.requests.AiEmbeddingRequest;

				contextData = {
					source: "document-1",
					tenantId: "tenant-123"
				}

				request = new AiEmbeddingRequest(
					input: "Hello World",
					options: { context: contextData }
				)

				result = request.getContext()
				hasContext = request.hasContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "request" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 2 );
		assertThat( resultContext.getAsString( Key.of( "source" ) ) ).isEqualTo( "document-1" );
		assertThat( resultContext.getAsString( Key.of( "tenantId" ) ) ).isEqualTo( "tenant-123" );
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isTrue();
	}

}
