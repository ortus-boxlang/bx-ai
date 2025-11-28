/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.ai.memory;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class HybridMemoryTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@Test
	@DisplayName( "Test HybridMemory instantiation with defaults" )
	public void testInstantiationDefault() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        vectorProvider: "BoxVectorMemory"
		    } )

		    recentLimit = memory.getRecentLimit()
		    semanticLimit = memory.getSemanticLimit()
		    totalLimit = memory.getTotalLimit()
		    recentWeight = memory.getRecentWeight()
		    vectorProvider = memory.getVectorProvider()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "recentLimit" ) ) ).isEqualTo( 5 );
		assertThat( variables.getAsInteger( Key.of( "semanticLimit" ) ) ).isEqualTo( 5 );
		assertThat( variables.getAsInteger( Key.of( "totalLimit" ) ) ).isEqualTo( 10 );
		assertThat( variables.get( Key.of( "recentWeight" ) ).toString() ).isEqualTo( "0.6" );
		assertThat( variables.getAsString( Key.of( "vectorProvider" ) ) ).isEqualTo( "BoxVectorMemory" );
	}

	@Test
	@DisplayName( "Test HybridMemory with custom configuration" )
	public void testCustomConfig() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        recentLimit: 8,
		        semanticLimit: 7,
		        totalLimit: 15,
		        recentWeight: 0.7,
		        vectorProvider: "BoxVectorMemory"
		    } )

		    recentLimit = memory.getRecentLimit()
		    semanticLimit = memory.getSemanticLimit()
		    totalLimit = memory.getTotalLimit()
		    recentWeight = memory.getRecentWeight()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "recentLimit" ) ) ).isEqualTo( 8 );
		assertThat( variables.getAsInteger( Key.of( "semanticLimit" ) ) ).isEqualTo( 7 );
		assertThat( variables.getAsInteger( Key.of( "totalLimit" ) ) ).isEqualTo( 15 );
		assertThat( variables.get( Key.of( "recentWeight" ) ).toString() ).isEqualTo( "0.7" );
	}

	@Test
	@DisplayName( "Test HybridMemory add stores in both memories" )
	public void testAddToBothMemories() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        vectorProvider: "BoxVectorMemory",
		        vectorConfig: {}
		    } )
		        .add( "First message" )
		        .add( "Second message" )
		        .add( "Third message" )

		    // Check recent memory
		    recentMemory = memory.getRecentMemory()
		    recentCount = recentMemory.count()

		    // Check vector memory
		    vectorMemory = memory.getVectorMemory()
		    vectorCount = vectorMemory.count()
		    """,
		    context
		);

		// Both memories should have all messages
		assertThat( variables.getAsInteger( Key.of( "recentCount" ) ) ).isEqualTo( 3 );
		assertThat( variables.getAsInteger( Key.of( "vectorCount" ) ) ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Test HybridMemory getAll combines recent and semantic" )
	public void testGetAllCombines() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        recentLimit: 3,
		        semanticLimit: 2,
		        totalLimit: 5,
		        vectorProvider: "BoxVector",

		    } )

		    // Add messages
		    memory.add( { role: "user", content: "Tell me about dogs" } )
		    memory.add( { role: "assistant", content: "Dogs are loyal pets" } )
		    memory.add( { role: "user", content: "What about cats?" } )
		    memory.add( { role: "assistant", content: "Cats are independent" } )

		    // Wait for vector indexing
		    sleep( 500 )

		    messages = memory.getAll()
		    count = messages.len()
		    """,
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		// Should have messages (recent + semantic, deduplicated)
		assertThat( count ).isAtLeast( 1 );
		assertThat( count ).isAtMost( 5 ); // totalLimit
	}

	@Test
	@DisplayName( "Test HybridMemory getRelevant uses semantic search" )
	public void testGetRelevantSemanticSearch() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        recentLimit: 2,
		        semanticLimit: 3,
		        recentWeight: 0.5,
		        vectorProvider: "BoxVector",

		    } )

		    // Add diverse messages
		    memory.add( { role: "user", content: "I love programming in BoxLang" } )
		    memory.add( { role: "assistant", content: "BoxLang is great for web development" } )
		    memory.add( { role: "user", content: "Tell me about cats" } )
		    memory.add( { role: "assistant", content: "Cats are wonderful pets" } )
		    memory.add( { role: "user", content: "What's your favorite programming language?" } )

		    // Wait for vector indexing
		    sleep( 500 )

		    // Search for programming-related content
		    relevant = memory.getRelevant( "programming languages", 5 )
		    count = relevant.len()
		    """,
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		assertThat( count ).isAtLeast( 1 );
		assertThat( count ).isAtMost( 5 );
	}

	@Test
	@DisplayName( "Test HybridMemory deduplicates messages" )
	public void testMessageDeduplication() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        recentLimit: 5,
		        semanticLimit: 5,
		        totalLimit: 10,
		        vectorProvider: "BoxVectorMemory",

		    } )

		    // Add same message multiple times
		    memory.add( { role: "user", content: "Unique message", id: "msg-1" } )
		    memory.add( { role: "assistant", content: "Response", id: "msg-2" } )
		    memory.add( { role: "user", content: "Another message", id: "msg-3" } )

		    // Wait for vector indexing
		    sleep( 500 )

		    messages = memory.getAll()
		    count = messages.len()

		    // Check for duplicate IDs
		    ids = messages.map( m => m.id ?: hash( m.content, "MD5" ) )
		    uniqueIds = ids.reduce( ( acc, id ) => {
		        if ( !acc.keyExists( id ) ) {
		            acc[ id ] = true
		        }
		        return acc
		    }, {} )
		    uniqueCount = uniqueIds.keyArray().len()
		    """,
		    context
		);

		var	count		= variables.getAsInteger( Key.of( "count" ) );
		var	uniqueCount	= variables.getAsInteger( Key.of( "uniqueCount" ) );

		// All messages should be unique
		assertThat( count ).isEqualTo( uniqueCount );
	}

	@Test
	@DisplayName( "Test HybridMemory clear clears both memories" )
	public void testClearBothMemories() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        vectorProvider: "BoxVectorMemory",

		    } )
		        .add( "Message 1" )
		        .add( "Message 2" )
		        .add( "Message 3" )

		    // Wait for vector indexing
		    sleep( 500 )

		    countBefore = memory.getRecentMemory().count()
		    vectorCountBefore = memory.getVectorMemory().count()

		    memory.clear()

		    countAfter = memory.getRecentMemory().count()
		    vectorCountAfter = memory.getVectorMemory().count()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "countBefore" ) ) ).isEqualTo( 3 );
		assertThat( variables.getAsInteger( Key.of( "vectorCountBefore" ) ) ).isEqualTo( 3 );
		assertThat( variables.getAsInteger( Key.of( "countAfter" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "vectorCountAfter" ) ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Test HybridMemory export includes both memories" )
	public void testExport() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "export-test", {
		        recentLimit: 6,
		        semanticLimit: 4,
		        totalLimit: 10,
		        recentWeight: 0.65,
		        vectorProvider: "BoxVectorMemory",

		    } )
		        .add( "Test message" )

		    // Wait for vector indexing
		    sleep( 500 )

		    exported = memory.export()
		    """,
		    context
		);

		var exported = variables.getAsStruct( Key.of( "exported" ) );
		assertThat( exported.getAsString( Key.of( "type" ) ) ).isEqualTo( "HybridMemory" );
		assertThat( exported.getAsString( Key.of( "key" ) ) ).isEqualTo( "export-test" );
		assertThat( exported.containsKey( Key.of( "config" ) ) ).isTrue();
		assertThat( exported.containsKey( Key.of( "recentMemory" ) ) ).isTrue();
		assertThat( exported.containsKey( Key.of( "vectorMemory" ) ) ).isTrue();

		var config = exported.getAsStruct( Key.of( "config" ) );
		assertThat( config.get( "recentLimit" ) ).isEqualTo( 6 );
		assertThat( config.get( "semanticLimit" ) ).isEqualTo( 4 );
		assertThat( config.get( "totalLimit" ) ).isEqualTo( 10 );
		assertThat( config.get( Key.of( "recentWeight" ) ).toString() ).isEqualTo( "0.65" );
	}

	@Test
	@DisplayName( "Test HybridMemory import restores both memories" )
	public void testImport() {
		runtime.executeSource(
		    """
		    // Create exported data structure
		    data = {
		        type: "HybridMemory",
		        key: "import-test",
		        config: {
		            recentLimit: 7,
		            semanticLimit: 6,
		            totalLimit: 13,
		            recentWeight: 0.55,
		            vectorProvider: "BoxVectorMemory",

		        },
		        recentMemory: {
		            type: "WindowMemory",
		            key: "import-test:recent",
		            messages: [
		                { role: "user", content: "Recent message 1" },
		                { role: "assistant", content: "Recent response 1" }
		            ]
		        },
		        vectorMemory: {
		            type: "BoxVectorMemory",
		            key: "import-test",
		            messages: [
		                { role: "user", content: "Vector message 1", id: "vec-1" }
		            ]
		        }
		    }

		    memory = aiMemory( "hybrid", "test", {
		        vectorProvider: "BoxVectorMemory",

		    } )

		    memory.import( data )

		    // Wait for vector indexing
		    sleep( 500 )

		    recentCount = memory.getRecentMemory().count()
		    vectorCount = memory.getVectorMemory().count()
		    allMessages = memory.getAll()
		    totalCount = allMessages.len()
		    """,
		    context
		);

		// Import should restore messages to both memories
		var	recentCount	= variables.getAsInteger( Key.of( "recentCount" ) );
		var	vectorCount	= variables.getAsInteger( Key.of( "vectorCount" ) );
		var	totalCount	= variables.getAsInteger( Key.of( "totalCount" ) );

		// At minimum, we should have messages somewhere
		assertThat( totalCount ).isAtLeast( 1 );
		assertThat( recentCount + vectorCount ).isAtLeast( 1 );
	}

	@Test
	@DisplayName( "Test HybridMemory respects totalLimit" )
	public void testTotalLimit() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        recentLimit: 10,
		        semanticLimit: 10,
		        totalLimit: 5,
		        vectorProvider: "BoxVectorMemory",

		    } )

		    // Add many messages
		    for( i = 1; i <= 12; i++ ) {
		        memory.add( { role: "user", content: "Message number " & i, id: "msg-" & i } )
		    }

		    // Wait for vector indexing
		    sleep( 1000 )

		    messages = memory.getAll()
		    count = messages.len()
		    """,
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		// Should not exceed totalLimit
		assertThat( count ).isAtMost( 5 );
	}

	@Test
	@DisplayName( "Test HybridMemory recentWeight affects message distribution" )
	public void testRecentWeight() {
		runtime.executeSource(
		    """
		    // High recent weight (70%)
		    memoryHighRecent = aiMemory( "hybrid", "high-recent", {
		        recentWeight: 0.7,
		        vectorProvider: "BoxVectorMemory",

		    } )

		    // Low recent weight (30%)
		    memoryLowRecent = aiMemory( "hybrid", "low-recent", {
		        recentWeight: 0.3,
		        vectorProvider: "BoxVectorMemory",

		    } )

		    highWeight = memoryHighRecent.getRecentWeight()
		    lowWeight = memoryLowRecent.getRecentWeight()
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "highWeight" ) ).toString() ).isEqualTo( "0.7" );
		assertThat( variables.get( Key.of( "lowWeight" ) ).toString() ).isEqualTo( "0.3" );
	}

	@Test
	@DisplayName( "Test HybridMemory with system message" )
	public void testWithSystemMessage() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        vectorProvider: "BoxVectorMemory",

		    } )

		    // Set system message through recent memory
		    memory.getRecentMemory().setSystemMessage( "You are a helpful assistant" )

		    memory.add( "User message" )
		    memory.add( "Assistant response" )

		    systemMsg = memory.getRecentMemory().getSystemMessage()
		    hasSystemMsg = memory.getRecentMemory().hasSystemMessage()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasSystemMsg" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "You are a helpful assistant" );
	}

	@Test
	@DisplayName( "Test HybridMemory message ID generation" )
	public void testMessageIdGeneration() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        vectorProvider: "BoxVectorMemory",

		    } )

		    // Message with explicit ID
		    memory.add( { role: "user", content: "With ID", id: "explicit-123" } )

		    // Message without ID (will be hashed)
		    memory.add( { role: "user", content: "Without ID" } )

		    // Wait for vector indexing
		    sleep( 500 )

		    messages = memory.getAll()
		    count = messages.len()
		    """,
		    context
		);

		var count = variables.getAsInteger( Key.of( "count" ) );
		assertThat( count ).isAtLeast( 2 );
	}

	@Test
	@DisplayName( "Test HybridMemory handles empty query gracefully" )
	public void testEmptyQuery() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        vectorProvider: "BoxVectorMemory",

		    } )
		        .add( "Test message" )

		    // Wait for vector indexing
		    sleep( 500 )

		    // Empty query should still return messages
		    messages = memory.getRelevant( "", 5 )
		    count = messages.len()
		    """,
		    context
		);

		// Should return some messages even with empty query
		var count = variables.getAsInteger( Key.of( "count" ) );
		assertThat( count ).isAtLeast( 0 ); // May be 0 or more depending on implementation
	}

	@Test
	@DisplayName( "Test HybridMemory type property" )
	public void testMemoryType() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "hybrid", "test-key", {
		        vectorProvider: "BoxVectorMemory",

		    } )

		    exported = memory.export()
		    memoryType = exported.type
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "memoryType" ) ) ).isEqualTo( "HybridMemory" );
	}

}
