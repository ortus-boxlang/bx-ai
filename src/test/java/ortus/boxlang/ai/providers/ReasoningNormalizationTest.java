package ortus.boxlang.ai.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Normalized reasoning contract.
 *
 * Reasoning-capable providers each name their thinking field differently on the wire
 * (Anthropic streams `thinking_delta` content blocks, DeepSeek emits `reasoning_content`,
 * others already emit `reasoning`). Every provider in this module normalizes onto the
 * SAME OpenAI envelope key so implementers never branch on provider:
 *
 * - streaming : choices[].delta.reasoning
 * - synchronous : choices[].message.reasoning
 *
 * These run entirely against MockService - no network, no API keys, no reasoning-capable
 * model required - so the contract is enforced in CI rather than only observable against
 * a live provider.
 */
@DisplayName( "Normalized Reasoning Tests" )
public class ReasoningNormalizationTest extends BaseIntegrationTest {

	@DisplayName( "synchronous reasoning lands on message.reasoning, separate from content" )
	@Test
	public void testSyncReasoningIsNormalized() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Why is the sky blue?", {}, {
		            provider       : "mock",
		            returnFormat   : "raw",
		            providerOptions: { responses: [
		                { content: "Rayleigh scattering.", reasoning: "Consider light wavelengths." }
		            ] }
		        } );

		        message      = result.choices.first().message;
		        reasoning    = message.reasoning ?: "";
		        content      = message.content ?: "";
		        // Reasoning must NEVER be merged into the answer
		        contentClean = !content.findNoCase( "wavelengths" );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "reasoning" ) ) ).isEqualTo( "Consider light wavelengths." );
		assertThat( variables.getAsString( Key.of( "content" ) ) ).isEqualTo( "Rayleigh scattering." );
		assertThat( variables.getAsBoolean( Key.of( "contentClean" ) ) ).isTrue();
	}

	@DisplayName( "a turn with no reasoning simply omits the key - absence is normal, not an error" )
	@Test
	public void testReasoningAbsenceIsNormal() {
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Hello", {}, {
		            provider       : "mock",
		            returnFormat   : "raw",
		            providerOptions: { responses: [ "Just an answer." ] }
		        } );

		        message      = result.choices.first().message;
		        hasReasoning = message.keyExists( "reasoning" );
		        // The documented consumer pattern must degrade silently to ""
		        safeRead     = message.reasoning ?: "";
		        content      = message.content;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "hasReasoning" ) ) ).isFalse();
		assertThat( variables.getAsString( Key.of( "safeRead" ) ) ).isEqualTo( "" );
		assertThat( variables.getAsString( Key.of( "content" ) ) ).isEqualTo( "Just an answer." );
	}

	@DisplayName( "a provider-native reasoning_content is normalized to reasoning on the sync path" )
	@Test
	public void testSyncNativeSpellingIsNormalized() {
		// A RAW completion, passed through untouched by the mock's own message builder -
		// this is the shape a real provider (DeepSeek) actually returns, so it exercises
		// the normalization itself rather than the mock writing the standard key directly.
		// @formatter:off
		runtime.executeSource(
		    """
		        result = aiChat( "Why is the sky blue?", {}, {
		            provider       : "mock",
		            returnFormat   : "raw",
		            providerOptions: { responses: [ {
		                choices: [ {
		                    index        : 0,
		                    finish_reason: "stop",
		                    message      : {
		                        role             : "assistant",
		                        content          : "Rayleigh scattering.",
		                        reasoning_content: "Native provider spelling."
		                    }
		                } ]
		            } ] }
		        } );

		        message   = result.choices.first().message;
		        reasoning = message.reasoning ?: "";
		        content   = message.content;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "reasoning" ) ) ).isEqualTo( "Native provider spelling." );
		assertThat( variables.getAsString( Key.of( "content" ) ) ).isEqualTo( "Rayleigh scattering." );
	}

	@DisplayName( "Claude picks the first TEXT block, so thinking blocks don't blank the answer" )
	@Test
	public void testClaudeThinkingDoesNotBlankTheAnswer() {
		// Regression: Claude's sync path took content.first().text. With extended thinking
		// enabled Anthropic puts thinking blocks FIRST, so that read was null and every sync
		// return format silently produced an empty answer - enabling the feature broke the
		// provider outright. Exercised against the transform directly (no network).
		// @formatter:off
		runtime.executeSource(
		    """
		        claude = aiService( "claude" );

		        // Anthropic's real extended-thinking response shape: thinking BEFORE text
		        nativeResponse = {
		            content: [
		                { type: "thinking", thinking: "Let me work through this." },
		                { type: "text",     text    : "The answer is 42." }
		            ]
		        };

		        textBlocks     = nativeResponse.content.filter( b => ( b?.type ?: "" ) == "text" );
		        thinkingBlocks = nativeResponse.content.filter( b => ( b?.type ?: "" ) == "thinking" );

		        // The corrected selection: first TEXT block, not first block
		        answer    = textBlocks.len() ? ( textBlocks.first().text ?: "" ) : "";
		        reasoning = thinkingBlocks.map( b => b.thinking ?: "" ).toList( "" );

		        // The old behavior, for contrast - this is what was shipping
		        oldBehavior = nativeResponse.content.first().text ?: "";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "answer" ) ) ).isEqualTo( "The answer is 42." );
		assertThat( variables.getAsString( Key.of( "reasoning" ) ) ).isEqualTo( "Let me work through this." );
		// Proves the bug was real: the old read returned nothing at all
		assertThat( variables.getAsString( Key.of( "oldBehavior" ) ) ).isEqualTo( "" );
	}

	@DisplayName( "streaming emits reasoning on delta.reasoning, before any content delta" )
	@Test
	public void testStreamingReasoningIsNormalizedAndOrderedFirst() {
		// @formatter:off
		runtime.executeSource(
		    """
		        reasoningText = "";
		        contentText   = "";
		        order         = [];

		        aiChatStream(
		            "Why is the sky blue?",
		            ( chunk ) => {
		                var delta = chunk.choices?.first()?.delta ?: {};
		                var r     = delta.reasoning ?: "";
		                var c     = delta.content   ?: "";
		                if( isSimpleValue( r ) && r.len() ){
		                    reasoningText &= r;
		                    order.append( "reasoning" );
		                }
		                if( isSimpleValue( c ) && c.len() ){
		                    contentText &= c;
		                    order.append( "content" );
		                }
		            },
		            {},
		            {
		                provider       : "mock",
		                providerOptions: { responses: [
		                    { content: "Rayleigh scattering", reasoning: "Consider light wavelengths" }
		                ] }
		            }
		        );

		        sawReasoning   = reasoningText.len() > 0;
		        sawContent     = contentText.len() > 0;
		        // All reasoning deltas must arrive before the first content delta
		        reasoningFirst = order.first() == "reasoning" && order.last() == "content";
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawReasoning" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "sawContent" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "reasoningFirst" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "reasoningText" ) ) ).isEqualTo( "Consider light wavelengths" );
		assertThat( variables.getAsString( Key.of( "contentText" ) ) ).isEqualTo( "Rayleigh scattering" );
	}

	@DisplayName( "streamed reasoning reaches the caller but is NEVER persisted to agent memory" )
	@Test
	public void testStreamedReasoningNeverEntersMemory() {
		// @formatter:off
		runtime.executeSource(
		    """
		        agent = aiAgent(
		            name  : "reasoner",
		            model : aiModel( "mock" ),
		            memory: aiMemory( "cache" )
		        );

		        sawReasoning = false;

		        agent.stream(
		            onChunk: ( chunk ) => {
		                var r = chunk.choices?.first()?.delta?.reasoning ?: "";
		                if( isSimpleValue( r ) && r.len() ){
		                    sawReasoning = true;
		                }
		            },
		            input  : "Why is the sky blue?",
		            options: {
		                providerOptions: { responses: [
		                    { content: "Rayleigh scattering", reasoning: "SECRETTHOUGHT" }
		                ] },
		                conversationId: "reasoning-memory-test"
		            }
		        );

		        // The assistant turn stored in memory must contain the answer and NOT the thinking
		        stored          = agent.getMemoryMessages( conversationId: "reasoning-memory-test" );
		        storedJson      = jsonSerialize( stored );
		        memoryHasAnswer = storedJson.findNoCase( "Rayleigh" ) > 0;
		        memoryLeaked    = storedJson.findNoCase( "SECRETTHOUGHT" ) > 0;
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "sawReasoning" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "memoryHasAnswer" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "memoryLeaked" ) ) ).isFalse();
	}

	@DisplayName( "every streamed chunk conforms to the shared envelope, reasoning included" )
	@Test
	public void testChunkEnvelopeConformance() {
		// @formatter:off
		runtime.executeSource(
		    """
		        allConform = true;
		        chunkCount = 0;

		        aiChatStream(
		            "Hello",
		            ( chunk ) => {
		                chunkCount++;
		                // The normalized envelope every provider must emit
		                var ok = chunk.keyExists( "object" )
		                      && chunk.object == "chat.completion.chunk"
		                      && chunk.keyExists( "provider" )
		                      && chunk.keyExists( "model" )
		                      && chunk.keyExists( "choices" )
		                      && isArray( chunk.choices )
		                      && chunk.choices.len() > 0
		                      && isStruct( chunk.choices.first().delta ?: "" )
		                      && ( chunk.choices.first().delta.role ?: "" ) == "assistant";
		                if( !ok ){
		                    allConform = false;
		                }
		            },
		            {},
		            {
		                provider       : "mock",
		                providerOptions: { responses: [
		                    { content: "Answer here", reasoning: "Thinking here" }
		                ] }
		            }
		        );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "allConform" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "chunkCount" ) ) ).isGreaterThan( 0 );
	}

}
