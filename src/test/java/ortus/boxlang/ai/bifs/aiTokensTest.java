package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.Struct;

public class aiTokensTest extends BaseIntegrationTest {

	@DisplayName( "Can estimate tokens for simple text using characters method" )
	@Test
	public void testSimpleTextCharacters() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "Hello, world!"
				result = aiTokens( text )
		    """,
		    context
		);
		// @formatter:on

		Object result = variables.get( "result" );
		assertThat( result ).isInstanceOf( Number.class );
		
		// "Hello, world!" = 13 chars / 4 = 3.25, ceiling = 4 tokens
		assertThat( ( ( Number ) result ).intValue() ).isEqualTo( 4 );
	}

	@DisplayName( "Can estimate tokens using words method" )
	@Test
	public void testWordsMethod() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "Hello world test"
				result = aiTokens( text, { method: "words" } )
		    """,
		    context
		);
		// @formatter:on

		Object result = variables.get( "result" );
		assertThat( result ).isInstanceOf( Number.class );
		
		// 3 words * 1.3 = 3.9, ceiling = 4 tokens
		assertThat( ( ( Number ) result ).intValue() ).isEqualTo( 4 );
	}

	@DisplayName( "Can count tokens for array of text chunks" )
	@Test
	public void testArrayOfChunks() {
		// @formatter:off
		runtime.executeSource(
		    """
				chunks = ["Hello", "world", "test"]
				result = aiTokens( chunks )
		    """,
		    context
		);
		// @formatter:on

		Object result = variables.get( "result" );
		assertThat( result ).isInstanceOf( Number.class );
		
		// "Helloworldtest" = 14 chars / 4 = 3.5, ceiling = 4 tokens
		assertThat( ( ( Number ) result ).intValue() ).isEqualTo( 4 );
	}

	@DisplayName( "Returns detailed statistics when detailed option is true" )
	@Test
	public void testDetailedStats() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "This is a test sentence."
				result = aiTokens( text, { detailed: true } )
		    """,
		    context
		);
		// @formatter:on

		Struct result = ( Struct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		assertThat( result.containsKey( "tokens" ) ).isTrue();
		assertThat( result.containsKey( "characters" ) ).isTrue();
		assertThat( result.containsKey( "words" ) ).isTrue();
		assertThat( result.containsKey( "chunks" ) ).isTrue();
		assertThat( result.containsKey( "method" ) ).isTrue();
		
		// 24 characters
		assertThat( ( ( Number ) result.get( "characters" ) ).intValue() ).isEqualTo( 24 );
		// 5 words
		assertThat( ( ( Number ) result.get( "words" ) ).intValue() ).isEqualTo( 5 );
		// 1 chunk
		assertThat( ( ( Number ) result.get( "chunks" ) ).intValue() ).isEqualTo( 1 );
		// 24 / 4 = 6 tokens
		assertThat( ( ( Number ) result.get( "tokens" ) ).intValue() ).isEqualTo( 6 );
		// method used
		assertThat( result.get( "method" ) ).isEqualTo( "characters" );
	}

	@DisplayName( "Handles empty text" )
	@Test
	public void testEmptyText() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = ""
				result = aiTokens( text )
		    """,
		    context
		);
		// @formatter:on

		Object result = variables.get( "result" );
		assertThat( result ).isInstanceOf( Number.class );
		assertThat( ( ( Number ) result ).intValue() ).isEqualTo( 0 );
	}

	@DisplayName( "Handles empty array" )
	@Test
	public void testEmptyArray() {
		// @formatter:off
		runtime.executeSource(
		    """
				chunks = []
				result = aiTokens( chunks )
		    """,
		    context
		);
		// @formatter:on

		Object result = variables.get( "result" );
		assertThat( result ).isInstanceOf( Number.class );
		assertThat( ( ( Number ) result ).intValue() ).isEqualTo( 0 );
	}

	@DisplayName( "Large text estimation" )
	@Test
	public void testLargeText() {
		// @formatter:off
		runtime.executeSource(
		    """
				// Create a large text (approximately 400 characters)
				text = repeatString( "This is a test sentence. ", 16 )
				result = aiTokens( text )
		    """,
		    context
		);
		// @formatter:on

		Object result = variables.get( "result" );
		assertThat( result ).isInstanceOf( Number.class );
		
		// 16 * 25 = 400 chars / 4 = 100 tokens
		assertThat( ( ( Number ) result ).intValue() ).isEqualTo( 100 );
	}

	@DisplayName( "Characters vs words comparison" )
	@Test
	public void testMethodComparison() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "The quick brown fox jumps over the lazy dog"
				charResult = aiTokens( text, { method: "characters" } )
				wordResult = aiTokens( text, { method: "words" } )
		    """,
		    context
		);
		// @formatter:on

		Object charResult = variables.get( "charResult" );
		Object wordResult = variables.get( "wordResult" );
		
		assertThat( charResult ).isInstanceOf( Number.class );
		assertThat( wordResult ).isInstanceOf( Number.class );
		
		// 44 chars / 4 = 11 tokens (characters method)
		assertThat( ( ( Number ) charResult ).intValue() ).isEqualTo( 11 );
		
		// 9 words * 1.3 = 11.7, ceiling = 12 tokens (words method)
		assertThat( ( ( Number ) wordResult ).intValue() ).isEqualTo( 12 );
	}

	@DisplayName( "Works with aiChunk output" )
	@Test
	public void testWithChunkedText() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "This is a test. It has multiple sentences. We will chunk it."
				chunks = aiChunk( text, { chunkSize: 20, overlap: 0, strategy: "characters" } )
				result = aiTokens( chunks )
		    """,
		    context
		);
		// @formatter:on

		Object result = variables.get( "result" );
		assertThat( result ).isInstanceOf( Number.class );
		
		// Chunks may trim whitespace, so token count is approximate
		assertThat( ( ( Number ) result ).intValue() ).isGreaterThan( 10 );
	}

	@DisplayName( "Detailed mode with array of chunks" )
	@Test
	public void testDetailedWithArray() {
		// @formatter:off
		runtime.executeSource(
		    """
				chunks = ["First chunk", "Second chunk", "Third chunk"]
				result = aiTokens( chunks, { detailed: true, method: "words" } )
		    """,
		    context
		);
		// @formatter:on

		Struct result = ( Struct ) variables.get( "result" );
		assertThat( result ).isNotNull();
		
		// 6 words total
		assertThat( ( ( Number ) result.get( "words" ) ).intValue() ).isEqualTo( 6 );
		// 3 chunks
		assertThat( ( ( Number ) result.get( "chunks" ) ).intValue() ).isEqualTo( 3 );
		// 6 * 1.3 = 7.8, ceiling = 8 tokens
		assertThat( ( ( Number ) result.get( "tokens" ) ).intValue() ).isEqualTo( 8 );
		// method used
		assertThat( result.get( "method" ) ).isEqualTo( "words" );
	}

}
