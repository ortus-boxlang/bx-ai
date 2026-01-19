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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class aiRequestContextTest extends BaseIntegrationTest {

	@DisplayName( "Can create an AiMessage with context" )
	@Test
	public void testAiMessageWithContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				contextData = {
					userId: "user-123",
					tenantId: "tenant-456",
					roles: ["admin", "editor"],
					ragDocuments: ["doc1", "doc2"]
				}

				message = aiMessage( "Hello" )
					.setContext( contextData )

				result = message.getContext()
				hasContext = message.hasContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 4 );
		assertThat( resultContext.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-123" );
		assertThat( resultContext.getAsString( Key.of( "tenantId" ) ) ).isEqualTo( "tenant-456" );
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isTrue();
	}

	@DisplayName( "Can create an AiMessage without context" )
	@Test
	public void testAiMessageWithoutContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage( "Hello" )

				result = message.getContext()
				hasContext = message.hasContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 0 );
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isFalse();
	}

	@DisplayName( "Can add context to an existing AiMessage" )
	@Test
	public void testAddContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage( "Hello" )
					.addContext( "userId", "user-123" )
					.addContext( "role", "admin" )

				result = message.getContext()
				hasContext = message.hasContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 2 );
		assertThat( resultContext.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-123" );
		assertThat( resultContext.getAsString( Key.of( "role" ) ) ).isEqualTo( "admin" );
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isTrue();
	}

	@DisplayName( "Can merge context into an existing AiMessage" )
	@Test
	public void testMergeContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage( "Hello" )
					.addContext( "userId", "user-123" )
					.mergeContext( { tenantId: "tenant-456", role: "admin" } )

				result = message.getContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
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
				message = aiMessage( "Hello" )
					.addContext( "userId", "old-user" )
					.addContext( "role", "viewer" )
					.mergeContext( { userId: "new-user" } )

				result = message.getContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
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
				message = aiMessage( "Hello" )
					.addContext( "userId", "user-123" )
					.addContext( "role", "admin" )

				userId = message.getContextValue( "userId" )
				role = message.getContextValue( "role" )
				missing = message.getContextValue( "missing" )
				missingWithDefault = message.getContextValue( "missing", "default-value" )
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
				message = aiMessage( "Hello" )
					.setContext( { userId: "user-123", permissions: ["read", "write"] } )

				result = message.getContext()
				hasContext = message.hasContext()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		IStruct resultContext = ( IStruct ) variables.get( "result" );
		assertThat( resultContext.size() ).isEqualTo( 2 );
		assertThat( resultContext.getAsString( Key.of( "userId" ) ) ).isEqualTo( "user-123" );
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isTrue();
	}

	@DisplayName( "Context is automatically bound to messages with ${context} placeholder via render()" )
	@Test
	public void testContextBindingInMessages() {

		// @formatter:off
		runtime.executeSource(
		    """
				contextData = {
					userId: "user-123",
					documents: ["doc1", "doc2"]
				}

				message = aiMessage( "Here is the context: ${context}. Now answer my question." )
					.setContext( contextData )

				// Render applies bindings including context
				messages = message.render()
				messageContent = messages[1].content
		    """,
		    context
		);
		// @formatter:on

		String messageContent = variables.getAsString( Key.of( "messageContent" ) );
		assertThat( messageContent ).contains( "user-123" );
		assertThat( messageContent ).contains( "doc1" );
		assertThat( messageContent ).contains( "doc2" );
		assertThat( messageContent ).doesNotContain( "${context}" );
	}

	@DisplayName( "Messages without ${context} placeholder are unchanged via render()" )
	@Test
	public void testNoContextPlaceholder() {

		// @formatter:off
		runtime.executeSource(
		    """
				contextData = {
					userId: "user-123"
				}

				message = aiMessage( "Hello, how are you?" )
					.setContext( contextData )

				messages = message.render()
				messageContent = messages[1].content
		    """,
		    context
		);
		// @formatter:on

		String messageContent = variables.getAsString( Key.of( "messageContent" ) );
		assertThat( messageContent ).isEqualTo( "Hello, how are you?" );
	}

	@DisplayName( "Context binding works with system messages via render()" )
	@Test
	public void testContextBindingInSystemMessage() {

		// @formatter:off
		runtime.executeSource(
		    """
				contextData = {
					role: "admin",
					permissions: ["read", "write"]
				}

				message = aiMessage()
					.system( "You are an AI assistant. User context: ${context}" )
					.user( "What can I do?" )
					.setContext( contextData )

				messages = message.render()
				systemContent = messages[1].content
		    """,
		    context
		);
		// @formatter:on

		String systemContent = variables.getAsString( Key.of( "systemContent" ) );
		assertThat( systemContent ).contains( "admin" );
		assertThat( systemContent ).contains( "read" );
		assertThat( systemContent ).contains( "write" );
		assertThat( systemContent ).doesNotContain( "${context}" );
	}

	@DisplayName( "Context can be combined with other bindings via render()" )
	@Test
	public void testContextWithOtherBindings() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage( "Hello ${name}, your context is: ${context}" )
					.bind( { name: "John" } )
					.setContext( { role: "admin" } )

				messages = message.render()
				messageContent = messages[1].content
		    """,
		    context
		);
		// @formatter:on

		String messageContent = variables.getAsString( Key.of( "messageContent" ) );
		assertThat( messageContent ).contains( "Hello John" );
		assertThat( messageContent ).contains( "admin" );
		assertThat( messageContent ).doesNotContain( "${name}" );
		assertThat( messageContent ).doesNotContain( "${context}" );
	}

	@DisplayName( "AiRequest constructor merges context from options and renders messages" )
	@Test
	public void testAiRequestContextFromOptions() {

		// @formatter:off
		runtime.executeSource(
		    """
				contextData = {
					userId: "user-123",
					role: "admin"
				}

				message = aiMessage( "Context: ${context}. Help me." )

				aiRequest = aiChatRequest(
					messages: message,
					options: { context: contextData }
				)

				messages = aiRequest.getMessages()
				messageContent = messages[1].content
		    """,
		    context
		);
		// @formatter:on

		String messageContent = variables.getAsString( Key.of( "messageContent" ) );
		assertThat( messageContent ).contains( "user-123" );
		assertThat( messageContent ).contains( "admin" );
		assertThat( messageContent ).doesNotContain( "${context}" );
	}

	@DisplayName( "AiRequest renders messages even without context" )
	@Test
	public void testAiRequestRendersWithoutContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage( "Hello ${name}" )
					.bind( { name: "World" } )

				aiRequest = aiChatRequest( message )

				messages = aiRequest.getMessages()
				messageContent = messages[1].content
		    """,
		    context
		);
		// @formatter:on

		String messageContent = variables.getAsString( Key.of( "messageContent" ) );
		assertThat( messageContent ).isEqualTo( "Hello World" );
	}

	@DisplayName( "AiRequest context merges with existing message context" )
	@Test
	public void testAiRequestContextMergesWithMessageContext() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage( "Context: ${context}" )
					.setContext( { existingKey: "existing-value" } )

				aiRequest = aiChatRequest(
					messages: message,
					options: { context: { newKey: "new-value" } }
				)

				messages = aiRequest.getMessages()
				messageContent = messages[1].content
		    """,
		    context
		);
		// @formatter:on

		String messageContent = variables.getAsString( Key.of( "messageContent" ) );
		assertThat( messageContent ).contains( "existingKey" );
		assertThat( messageContent ).contains( "existing-value" );
		assertThat( messageContent ).contains( "newKey" );
		assertThat( messageContent ).contains( "new-value" );
	}

	@DisplayName( "Can simulate RAG document injection into message context" )
	@Test
	public void testRAGDocumentSimulation() {

		// @formatter:off
		runtime.executeSource(
		    """
				// Simulate RAG document retrieval from a vector database
				// In real scenario, these would come from embeddings search
				ragDocuments = [
					{
						id: "doc-001",
						title: "BoxLang Installation Guide",
						content: "BoxLang is a modern dynamic JVM language. To install, use the following command: box install boxlang",
						score: 0.95,
						metadata: {
							source: "docs/getting-started.md",
							section: "installation"
						}
					},
					{
						id: "doc-002",
						title: "BoxLang BIF Creation",
						content: "Built-in Functions (BIFs) are created using the @BoxBIF annotation. All BIF functions must be standalone without instance state.",
						score: 0.89,
						metadata: {
							source: "docs/advanced/bifs.md",
							section: "creation"
						}
					},
					{
						id: "doc-003",
						title: "BoxLang Module Structure",
						content: "BoxLang modules are structured with src/main/bx/ for source code and ModuleConfig.bx as the module descriptor.",
						score: 0.82,
						metadata: {
							source: "docs/modules.md",
							section: "structure"
						}
					}
				]

				// Simulate building context from RAG documents
				ragContext = {
					query: "How do I install BoxLang?",
					retrievedDocuments: ragDocuments,
					documentCount: ragDocuments.len(),
					averageScore: 0.88,
					retrievalTimestamp: now(),
					vectorDatabase: "local-embeddings"
				}

				// Create message with RAG context using ${context} placeholder
				message = aiMessage(
					"Based on the following documentation: ${context}" & char(10) & char(10) &
					"Please answer: How do I install BoxLang and create a BIF?"
				)
				.setContext( ragContext )

				// Render message to apply context bindings
				messages = message.render()
				messageContent = messages[1].content

				// Also test retrieving specific context values
				hasContext = message.hasContext()
				retrievedDocs = message.getContextValue( "retrievedDocuments" )
				docCount = message.getContextValue( "documentCount" )
				query = message.getContextValue( "query" )
		    """,
		    context
		);
		// @formatter:on

		// Verify context was properly set and bound
		assertThat( variables.getAsBoolean( Key.of( "hasContext" ) ) ).isTrue();

		// Verify rendered message contains RAG document content
		String messageContent = variables.getAsString( Key.of( "messageContent" ) );
		assertThat( messageContent ).contains( "BoxLang Installation Guide" );
		assertThat( messageContent ).contains( "box install boxlang" );
		assertThat( messageContent ).contains( "@BoxBIF annotation" );
		assertThat( messageContent ).contains( "doc-001" );
		assertThat( messageContent ).contains( "doc-002" );
		assertThat( messageContent ).contains( "doc-003" );
		assertThat( messageContent ).doesNotContain( "${context}" );

		// Verify context values can be retrieved
		assertThat( variables.get( "retrievedDocs" ) ).isNotNull();
		assertThat( variables.getAsInteger( Key.of( "docCount" ) ) ).isEqualTo( 3 );
		assertThat( variables.getAsString( Key.of( "query" ) ) ).isEqualTo( "How do I install BoxLang?" );

		// Verify RAG documents array structure
		Array ragDocs = ( Array ) variables.get( "retrievedDocs" );
		assertThat( ragDocs.size() ).isEqualTo( 3 );

		// Verify first document structure
		IStruct firstDoc = ( IStruct ) ragDocs.get( 0 );
		assertThat( firstDoc.getAsString( Key.of( "id" ) ) ).isEqualTo( "doc-001" );
		assertThat( firstDoc.getAsString( Key.of( "title" ) ) ).isEqualTo( "BoxLang Installation Guide" );
		assertThat( firstDoc.getAsString( Key.of( "content" ) ) ).contains( "box install boxlang" );
		assertThat( firstDoc.get( Key.of( "score" ) ).toString() ).isEqualTo( "0.95" );

		// Verify metadata structure
		IStruct metadata = ( IStruct ) firstDoc.get( Key.of( "metadata" ) );
		assertThat( metadata.getAsString( Key.of( "source" ) ) ).isEqualTo( "docs/getting-started.md" );
		assertThat( metadata.getAsString( Key.of( "section" ) ) ).isEqualTo( "installation" );
	}

}
