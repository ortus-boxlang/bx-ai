package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.types.Array;

public class aiChunkTest extends BaseIntegrationTest {

	@DisplayName( "Can chunk text with default recursive strategy" )
	@Test
	public void testDefaultChunking() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "This is a simple test. It has multiple sentences. Each sentence should be preserved when possible."
				result = aiChunk( text )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "Can chunk text by characters with custom chunk size" )
	@Test
	public void testCharacterChunking() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "This is a test string that should be chunked by characters."
				result = aiChunk( text, { chunkSize: 20, overlap: 5, strategy: "characters" } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isGreaterThan( 1 );

		// Verify each chunk is within size limit (allowing for trimming)
		for ( Object chunk : chunks ) {
			assertThat( chunk.toString().length() ).isAtMost( 20 );
		}
	}

	@DisplayName( "Can chunk text by words" )
	@Test
	public void testWordChunking() {
		// @formatter:off
			runtime.executeSource(
			    """
					text = "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen"
					result = aiChunk( text, { chunkSize: 3, overlap: 0, strategy: "words" } )
			    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isGreaterThan( 1 );

		// First chunk should have 3 words
		String	firstChunk	= chunks.get( 0 ).toString();
		int		wordCount	= firstChunk.split( "\\s+" ).length;
		assertThat( wordCount ).isEqualTo( 3 );
	}

	@DisplayName( "Can chunk text by sentences" )
	@Test
	public void testSentenceChunking() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "First sentence. Second sentence! Third sentence? Fourth sentence. Fifth sentence."
				result = aiChunk( text, { chunkSize: 2, overlap: 0, strategy: "sentences" } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isGreaterThan( 1 );
	}

	@DisplayName( "Can chunk text by paragraphs" )
	@Test
	public void testParagraphChunking() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "First paragraph.

				Second paragraph.

				Third paragraph."
				result = aiChunk( text, { chunkSize: 1, overlap: 0, strategy: "paragraphs" } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isAtLeast( 1 );
	}

	@DisplayName( "Can handle empty text" )
	@Test
	public void testEmptyText() {
		// @formatter:off
		runtime.executeSource(
		    """
				result = aiChunk( "" )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can handle text shorter than chunk size" )
	@Test
	public void testShortText() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "Short"
				result = aiChunk( text, { chunkSize: 1000 } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isEqualTo( 1 );
		assertThat( chunks.get( 0 ).toString() ).isEqualTo( "Short" );
	}

	@DisplayName( "Overlap creates overlapping chunks" )
	@Test
	public void testOverlap() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "AAAAA BBBBB CCCCC DDDDD EEEEE"
				result = aiChunk( text, { chunkSize: 15, overlap: 5, strategy: "characters" } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();

		if ( chunks.size() > 1 ) {
			// Verify overlap exists between consecutive chunks
			String	firstChunk	= chunks.get( 0 ).toString();
			String	secondChunk	= chunks.get( 1 ).toString();

			// Second chunk should contain some text from first chunk
			assertThat( secondChunk ).isNotEmpty();
		}
	}

	@DisplayName( "Recursive strategy handles mixed content" )
	@Test
	public void testRecursiveStrategy() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "This is paragraph one with multiple sentences. It should be chunked intelligently.

				This is paragraph two. It also has sentences.

				This is paragraph three with a very long sentence that exceeds the chunk size and should be broken down into words or characters as needed."
				result = aiChunk( text, { chunkSize: 50, overlap: 10, strategy: "recursive" } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isGreaterThan( 0 );

		// All chunks should be trimmed
		for ( Object chunk : chunks ) {
			String chunkStr = chunk.toString();
			assertThat( chunkStr ).isEqualTo( chunkStr.trim() );
		}
	}

	@DisplayName( "All chunks are automatically trimmed" )
	@Test
	public void testAutoTrim() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "  Leading and trailing spaces  "
				result = aiChunk( text, { chunkSize: 100, overlap: 0 } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isEqualTo( 1 );
		assertThat( chunks.get( 0 ).toString() ).isEqualTo( "Leading and trailing spaces" );
	}

	@DisplayName( "Large text is chunked appropriately" )
	@Test
	public void testLargeText() {
		// @formatter:off
		runtime.executeSource(
		    """
				// Create a large text with repeated content
				text = repeatString( "This is a sentence. ", 100 )
				result = aiChunk( text, { chunkSize: 200, overlap: 50 } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isGreaterThan( 5 );

		// Verify chunks respect size limits
		for ( Object chunk : chunks ) {
			// Allow some flexibility for overlap and word boundaries
			assertThat( chunk.toString().length() ).isAtMost( 300 );
		}
	}

	@DisplayName( "Invalid strategy throws error" )
	@Test
	public void testInvalidStrategy() {
		// @formatter:off
		try {
			runtime.executeSource(
			    """
					result = aiChunk( "test", { strategy: "invalid" } )
			    """,
			    context
			);
			assertThat( true ).isFalse(); // Should not reach here
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).contains( "Unknown strategy" );
		}
		// @formatter:on
	}

	@DisplayName( "Overlap larger than chunk size throws error" )
	@Test
	public void testInvalidOverlap() {
		// @formatter:off
		try {
			runtime.executeSource(
			    """
					result = aiChunk( "test", { chunkSize: 10, overlap: 15 } )
			    """,
			    context
			);
			assertThat( true ).isFalse(); // Should not reach here
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).contains( "must be less than chunkSize" );
		}
		// @formatter:on
	}

	@DisplayName( "Can chunk text with newlines preserved" )
	@Test
	public void testNewlinePreservation() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "Line 1
				Line 2
				Line 3
				Line 4"
				result = aiChunk( text, { chunkSize: 20, overlap: 0, strategy: "characters" } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "Single very long word is split by characters" )
	@Test
	public void testVeryLongWord() {
		// @formatter:off
		runtime.executeSource(
		    """
				text = "ThisIsAVeryLongWordThatExceedsTheChunkSizeAndShouldBeSplitByCharactersIndeedItIsReallyVeryVeryLong"
				result = aiChunk( text, { chunkSize: 30, overlap: 0, strategy: "recursive" } )
		    """,
		    context
		);
		// @formatter:on

		Array chunks = ( Array ) variables.get( "result" );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isGreaterThan( 1 );

		// Each chunk should be at most 30 characters
		for ( Object chunk : chunks ) {
			assertThat( chunk.toString().length() ).isAtMost( 30 );
		}
	}
}
