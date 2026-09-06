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
package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Deterministic tests for BaseService.populateStructuredOutput()'s lenient JSON recovery.
 *
 * Providers without a native structured-output channel (OpenAI-shaped, Gemini, Cohere, Ollama)
 * hand raw model prose to populateStructuredOutput(). All of these use a canned wrapLLMCall
 * middleware so no HTTP or credentials are involved.
 */
public class StructuredOutputParsingTest extends BaseIntegrationTest {

	/**
	 * Build a script that runs an OpenAI-shaped chat whose canned completion content is the
	 * supplied BoxLang string *expression*, with a struct structured-output schema.
	 *
	 * @param contentExpr a literal BoxLang expression (e.g. a single-quoted string)
	 */
	private String cannedContentScript( String contentExpr, String tail ) {
		// @formatter:off
		return """
			provider = aiService( "openai", { apiKey: "dummy-key" } )
			chatRequest = aiChatRequest(
				aiMessage().user( "Extract: John Doe, age 30" ),
				{ model: "gpt-4o-mini" },
				{
					provider: "openai",
					schema: {
						"type": "object",
						"properties": { "name": { "type": "string" }, "age": { "type": "integer" } },
						"required": [ "name", "age" ]
					}
				}
			)
			cannedContent = """ + contentExpr + """

			chatRequest.addMiddleware( {
				"wrapLLMCall": ( ctx, handler ) => {
					return {
						"choices": [ { "index": 0, "message": { "role": "assistant", "content": cannedContent } } ],
						"usage": { "prompt_tokens": 5, "completion_tokens": 8, "total_tokens": 13 }
					}
				}
			} )
			""" + tail;
		// @formatter:on
	}

	@DisplayName( "Prose-wrapped JSON still populates a structured output schema" )
	@Test
	public void testProseWrappedJSONPopulates() {
		// @formatter:off
		runtime.executeSource(
			cannedContentScript(
				"'Here is the result you asked for: { \"name\": \"John Doe\", \"age\": 30 } Let me know if you need anything else.'",
				"""
				result   = provider.chat( chatRequest )
				isStruct = isStruct( result )
				name     = result.name
				age      = result.age
				"""
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "Fenced JSON with leading prose still populates (regression guard)" )
	@Test
	public void testFencedJSONWithProsePopulates() {
		// @formatter:off
		runtime.executeSource(
			cannedContentScript(
				"'Sure thing!' & char(10) & '```json' & char(10) & '{ \"name\": \"John Doe\", \"age\": 30 }' & char(10) & '```' & char(10) & 'Hope that helps.'",
				"""
				result   = provider.chat( chatRequest )
				isStruct = isStruct( result )
				name     = result.name
				age      = result.age
				"""
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "Bare fenced JSON keeps working (fast path unchanged)" )
	@Test
	public void testBareFencedJSONStillWorks() {
		// @formatter:off
		runtime.executeSource(
			cannedContentScript(
				"'```json' & char(10) & '{ \"name\": \"John Doe\", \"age\": 30 }' & char(10) & '```'",
				"""
				result = provider.chat( chatRequest )
				name   = result.name
				age    = result.age
				"""
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John Doe" );
		assertThat( variables.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "Unparseable content throws StructuredOutputError instead of a raw JSON error" )
	@Test
	public void testUnparseableContentThrows() {
		// @formatter:off
		runtime.executeSource(
			cannedContentScript(
				"'I am sorry, but I cannot help with that request.'",
				"""
				caughtType = ""
				caughtMsg  = ""
				try {
					provider.chat( chatRequest )
				} catch( any e ) {
					caughtType = e.type
					caughtMsg  = e.message
				}
				"""
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "caughtType" ) ) ).isEqualTo( "StructuredOutputError" );
		assertThat( variables.get( Key.of( "caughtMsg" ) ).toString() ).contains( "no parseable JSON" );
	}

	@DisplayName( "Prose-wrapped empty object is a legitimate result, not an extraction failure" )
	@Test
	public void testProseWrappedEmptyObjectPopulates() {
		// "Here you go: {}" bypasses the isJSON() fast path and the extractor legitimately returns
		// an empty struct. The old "empty struct == extraction failed" heuristic threw here.
		// @formatter:off
		runtime.executeSource(
			cannedContentScript(
				"'Here you go: {} Nothing matched your query.'",
				"""
				caughtType = ""
				result     = ""
				try {
					result = provider.chat( chatRequest )
				} catch( any e ) {
					caughtType = e.type
				}
				didNotThrow = caughtType == ""
				isEmptyStruct = isStruct( result ) && structIsEmpty( result )
				"""
			),
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "didNotThrow" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isEmptyStruct" ) ) ).isTrue();
	}

	@DisplayName( "Prose containing braces but no valid JSON still throws StructuredOutputError" )
	@Test
	public void testProseWithBracesButNoJSONThrows() {
		// @formatter:off
		runtime.executeSource(
			cannedContentScript(
				"'I looked but the record { was never closed and there is no JSON here.'",
				"""
				caughtType = ""
				try {
					provider.chat( chatRequest )
				} catch( any e ) {
					caughtType = e.type
				}
				"""
			),
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "caughtType" ) ) ).isEqualTo( "StructuredOutputError" );
	}

	@DisplayName( "Prose-wrapped JSON array populates an array structured output schema" )
	@Test
	public void testProseWrappedArrayPopulates() {
		// @formatter:off
		runtime.executeSource(
			"""
			provider = aiService( "openai", { apiKey: "dummy-key" } )
			chatRequest = aiChatRequest(
				aiMessage().user( "List two people" ),
				{ model: "gpt-4o-mini" },
				{
					provider: "openai",
					returnFormat: [ new src.test.bx.Product() ]
				}
			)
			chatRequest.addMiddleware( {
				"wrapLLMCall": ( ctx, handler ) => {
					return {
						"choices": [ { "index": 0, "message": { "role": "assistant",
							"content": 'Here you go: { "items": [ { "name": "Alice", "price": 1, "category": "a" }, { "name": "Bob", "price": 2, "category": "b" } ] } Done.' } } ],
						"usage": { "prompt_tokens": 5, "completion_tokens": 8, "total_tokens": 13 }
					}
				}
			} )
			result  = provider.chat( chatRequest )
			isArray = isArray( result )
			count   = result.len()
			first   = result[ 1 ].getName()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isArray" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "first" ) ).toString() ).isEqualTo( "Alice" );
	}
}
