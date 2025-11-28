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
import ortus.boxlang.runtime.types.IStruct;

public class SummaryMemoryTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@Test
	@DisplayName( "Test SummaryMemory instantiation with defaults" )
	public void testInstantiationDefault() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary" )
		    maxMessages = memory.getMaxMessages()
		    summaryThreshold = memory.getSummaryThreshold()
		    summaryModel = memory.getSummaryModel()
		    summaryProvider = memory.getSummaryProvider()
		    hasSummary = memory.getHasSummary()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "maxMessages" ) ) ).isEqualTo( 20 );
		assertThat( variables.getAsInteger( Key.of( "summaryThreshold" ) ) ).isEqualTo( 10 );
		assertThat( variables.getAsString( Key.of( "summaryModel" ) ) ).isEqualTo( "gpt-4o-mini" );
		assertThat( variables.getAsString( Key.of( "summaryProvider" ) ) ).isEqualTo( "openai" );
		assertThat( variables.getAsBoolean( Key.of( "hasSummary" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Test SummaryMemory with custom config" )
	public void testCustomConfig() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary", "test-key", {
		        maxMessages: 15,
		        summaryThreshold: 8,
		        summaryModel: "gpt-4o",
		        summaryProvider: "openai"
		    } )

		    maxMessages = memory.getMaxMessages()
		    summaryThreshold = memory.getSummaryThreshold()
		    summaryModel = memory.getSummaryModel()
		    summaryProvider = memory.getSummaryProvider()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "maxMessages" ) ) ).isEqualTo( 15 );
		assertThat( variables.getAsInteger( Key.of( "summaryThreshold" ) ) ).isEqualTo( 8 );
		assertThat( variables.getAsString( Key.of( "summaryModel" ) ) ).isEqualTo( "gpt-4o" );
		assertThat( variables.getAsString( Key.of( "summaryProvider" ) ) ).isEqualTo( "openai" );
	}

	@Test
	@DisplayName( "Test SummaryMemory doesn't summarize below threshold" )
	public void testBelowThreshold() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary", "test-key", {
		        maxMessages: 10,
		        summaryThreshold: 5
		    } )

		    // Add 6 messages (below maxMessages threshold)
		    for( i = 1; i <= 6; i++ ) {
		        memory.add( "Message " & i )
		    }

		    count = memory.count()
		    hasSummary = memory.getHasSummary()
		    messages = memory.getAll()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 6 );
		assertThat( variables.getAsBoolean( Key.of( "hasSummary" ) ) ).isFalse();

		// All original messages should be intact
		var messages = variables.getAsArray( Key.of( "messages" ) );
		assertThat( messages.size() ).isEqualTo( 6 );
	}

	@Test
	@DisplayName( "Test SummaryMemory triggers summarization when threshold exceeded" )
	public void testSummarizationTriggered() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary", "test-key", {
		        maxMessages: 8,
		        summaryThreshold: 3
		    } )

		    // Add 9 messages (exceeds maxMessages of 8)
		    for( i = 1; i <= 9; i++ ) {
		        memory.add( { role: "user", content: "This is message number " & i } )
		    }

		    count = memory.count()
		    hasSummary = memory.getHasSummary()
		    messages = memory.getAll()
		    """,
		    context
		);

		var	count		= variables.getAsInteger( Key.of( "count" ) );
		var	hasSummary	= variables.getAsBoolean( Key.of( "hasSummary" ) );

		// Should have triggered summarization
		assertThat( hasSummary ).isTrue();

		// Should have: recent messages (threshold) + summary message
		assertThat( count ).isAtLeast( 4 );

		// First non-system message should be the summary
		var		messages	= variables.getAsArray( Key.of( "messages" ) );
		IStruct	firstMsg	= ( IStruct ) messages.get( 0 );
		assertThat( firstMsg.getAsString( Key.of( "role" ) ) ).isEqualTo( "assistant" );
		assertThat( firstMsg.getAsString( Key.of( "content" ) ) ).contains( "Previous conversation summary" );
	}

	@Test
	@DisplayName( "Test SummaryMemory preserves system message" )
	public void testSystemMessagePreservation() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary", "test-key", {
		        maxMessages: 5,
		        summaryThreshold: 2
		    } )
		        .setSystemMessage( "You are a helpful assistant" )

		    // Add messages below threshold
		    memory.add( "Message 1" )
		    memory.add( "Message 2" )
		    memory.add( "Message 3" )

		    systemMsg = memory.getSystemMessage()
		    hasSystemMsg = memory.hasSystemMessage()
		    """,
		    context
		);

		assertThat( variables.getAsBoolean( Key.of( "hasSystemMsg" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "systemMsg" ) ) ).isEqualTo( "You are a helpful assistant" );
	}

	@Test
	@DisplayName( "Test SummaryMemory clear resets summary state" )
	public void testClearResetsSummary() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary", "test-key" )
		        .add( "Message 1" )
		        .add( "Message 2" )

		    countBefore = memory.count()

		    memory.clear()

		    countAfter = memory.count()
		    hasSummary = memory.getHasSummary()
		    currentSummary = memory.getCurrentSummary()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "countBefore" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsInteger( Key.of( "countAfter" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsBoolean( Key.of( "hasSummary" ) ) ).isFalse();
		assertThat( variables.getAsString( Key.of( "currentSummary" ) ) ).isEmpty();
	}

	@Test
	@DisplayName( "Test SummaryMemory getSummary includes summary-specific fields" )
	public void testGetSummary() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary", "summary-test", {
		        maxMessages: 12,
		        summaryThreshold: 6,
		        summaryModel: "gpt-4o",
		        summaryProvider: "openai"
		    } )

		    summary = memory.getSummary()
		    """,
		    context
		);

		var summary = variables.getAsStruct( Key.of( "summary" ) );
		assertThat( summary.getAsString( Key.of( "type" ) ) ).isEqualTo( "SummaryMemory" );
		assertThat( summary.get( "maxMessages" ) ).isEqualTo( 12 );
		assertThat( summary.get( "summaryThreshold" ) ).isEqualTo( 6 );
		assertThat( summary.getAsString( Key.of( "summaryModel" ) ) ).isEqualTo( "gpt-4o" );
		assertThat( summary.getAsString( Key.of( "summaryProvider" ) ) ).isEqualTo( "openai" );
		assertThat( summary.containsKey( Key.of( "hasSummary" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SummaryMemory export includes all configuration" )
	public void testExport() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary", "export-test", {
		        maxMessages: 15,
		        summaryThreshold: 7,
		        summaryModel: "gpt-4o-mini",
		        summaryProvider: "openai"
		    } )
		        .add( "Test message" )

		    exported = memory.export()
		    """,
		    context
		);

		var exported = variables.getAsStruct( Key.of( "exported" ) );
		assertThat( exported.getAsString( Key.of( "type" ) ) ).isEqualTo( "SummaryMemory" );
		assertThat( exported.get( "maxMessages" ) ).isEqualTo( 15 );
		assertThat( exported.get( "summaryThreshold" ) ).isEqualTo( 7 );
		assertThat( exported.getAsString( Key.of( "summaryModel" ) ) ).isEqualTo( "gpt-4o-mini" );
		assertThat( exported.getAsString( Key.of( "summaryProvider" ) ) ).isEqualTo( "openai" );
		assertThat( exported.containsKey( Key.of( "currentSummary" ) ) ).isTrue();
		assertThat( exported.containsKey( Key.of( "hasSummary" ) ) ).isTrue();
		assertThat( exported.containsKey( Key.of( "messages" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Test SummaryMemory import restores configuration and state" )
	public void testImport() {
		runtime.executeSource(
		    """
		    data = {
		        key: "import-test",
		        maxMessages: 18,
		        summaryThreshold: 9,
		        summaryModel: "gpt-4o",
		        summaryProvider: "openai",
		        currentSummary: "This is a test summary",
		        hasSummary: true,
		        messages: [
		            { role: "user", content: "Recent message 1" },
		            { role: "assistant", content: "Recent response 1" }
		        ]
		    }

		    memory = aiMemory( "summary" )
		        .import( data )

		    maxMessages = memory.getMaxMessages()
		    summaryThreshold = memory.getSummaryThreshold()
		    summaryModel = memory.getSummaryModel()
		    summaryProvider = memory.getSummaryProvider()
		    currentSummary = memory.getCurrentSummary()
		    hasSummary = memory.getHasSummary()
		    count = memory.count()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "maxMessages" ) ) ).isEqualTo( 18 );
		assertThat( variables.getAsInteger( Key.of( "summaryThreshold" ) ) ).isEqualTo( 9 );
		assertThat( variables.getAsString( Key.of( "summaryModel" ) ) ).isEqualTo( "gpt-4o" );
		assertThat( variables.getAsString( Key.of( "summaryProvider" ) ) ).isEqualTo( "openai" );
		assertThat( variables.getAsString( Key.of( "currentSummary" ) ) ).isEqualTo( "This is a test summary" );
		assertThat( variables.getAsBoolean( Key.of( "hasSummary" ) ) ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "count" ) ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Test SummaryMemory with constructor parameters" )
	public void testConstructorParameters() {
		runtime.executeSource(
		    """
		    memory = new bxModules.bxai.models.memory.SummaryMemory(
		        key: "test-key",
		        maxMessages: 25,
		        summaryThreshold: 12,
		        summaryModel: "gpt-4",
		        summaryProvider: "openai"
		    )

		    maxMessages = memory.getMaxMessages()
		    summaryThreshold = memory.getSummaryThreshold()
		    summaryModel = memory.getSummaryModel()
		    summaryProvider = memory.getSummaryProvider()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "maxMessages" ) ) ).isEqualTo( 25 );
		assertThat( variables.getAsInteger( Key.of( "summaryThreshold" ) ) ).isEqualTo( 12 );
		assertThat( variables.getAsString( Key.of( "summaryModel" ) ) ).isEqualTo( "gpt-4" );
		assertThat( variables.getAsString( Key.of( "summaryProvider" ) ) ).isEqualTo( "openai" );
	}

	@Test
	@DisplayName( "Test SummaryMemory configure method" )
	public void testConfigureMethod() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary" )
		        .configure( {
		            maxMessages: 30,
		            summaryThreshold: 15,
		            summaryModel: "claude-3-5-sonnet-20241022",
		            summaryProvider: "claude"
		        } )

		    maxMessages = memory.getMaxMessages()
		    summaryThreshold = memory.getSummaryThreshold()
		    summaryModel = memory.getSummaryModel()
		    summaryProvider = memory.getSummaryProvider()
		    """,
		    context
		);

		assertThat( variables.getAsInteger( Key.of( "maxMessages" ) ) ).isEqualTo( 30 );
		assertThat( variables.getAsInteger( Key.of( "summaryThreshold" ) ) ).isEqualTo( 15 );
		assertThat( variables.getAsString( Key.of( "summaryModel" ) ) ).isEqualTo( "claude-3-5-sonnet-20241022" );
		assertThat( variables.getAsString( Key.of( "summaryProvider" ) ) ).isEqualTo( "claude" );
	}

	@Test
	@DisplayName( "Test SummaryMemory type property" )
	public void testMemoryType() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary" )
		    summary = memory.getSummary()
		    memoryType = summary.type
		    """,
		    context
		);

		assertThat( variables.getAsString( Key.of( "memoryType" ) ) ).isEqualTo( "SummaryMemory" );
	}

	@Test
	@DisplayName( "Test SummaryMemory progressive summarization" )
	public void testProgressiveSummarization() {
		runtime.executeSource(
		    """
		    memory = aiMemory( "summary", "progressive-test", {
		        maxMessages: 6,
		        summaryThreshold: 2
		    } )

		    // First batch: 7 messages (triggers first summarization)
		    for( i = 1; i <= 7; i++ ) {
		        memory.add( { role: "user", content: "First batch message " & i } )
		    }

		    countAfterFirst = memory.count()
		    hasSummaryAfterFirst = memory.getHasSummary()

		    // Second batch: Add more messages (triggers another summarization)
		    for( i = 1; i <= 5; i++ ) {
		        memory.add( { role: "user", content: "Second batch message " & i } )
		    }

		    countAfterSecond = memory.count()
		    hasSummaryAfterSecond = memory.getHasSummary()
		    currentSummary = memory.getCurrentSummary()
		    """,
		    context
		);

		// After first batch should have summary
		assertThat( variables.getAsBoolean( Key.of( "hasSummaryAfterFirst" ) ) ).isTrue();

		// After second batch should still have summary (updated)
		assertThat( variables.getAsBoolean( Key.of( "hasSummaryAfterSecond" ) ) ).isTrue();

		// Summary should not be empty
		assertThat( variables.getAsString( Key.of( "currentSummary" ) ) ).isNotEmpty();

		// Should keep recent messages within threshold
		var countAfterSecond = variables.getAsInteger( Key.of( "countAfterSecond" ) );
		assertThat( countAfterSecond ).isAtMost( 4 ); // summary + threshold messages
	}

}
