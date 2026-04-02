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
package ortus.boxlang.ai.runnables;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for AI Runnable interfaces and classes.
 * Tests the IAiRunnable interface, AiBaseRunnable abstract class,
 * AiRunnableSequence, and AiTransformRunnable implementations.
 */
public class RunnablesTest extends BaseIntegrationTest {

	@DisplayName( "Can create a basic transform runnable" )
	@Test
	public void testTransformRunnable() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Create a simple transform that uppercases the input
			transform = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( input ) => ucase( input )
			)

			result = transform.run( "hello world" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ) ).isEqualTo( "HELLO WORLD" );
	}

	@DisplayName( "Transform runnable can stream results" )
	@Test
	public void testTransformRunnableStream() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Create a transform runnable
			transform = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( input ) => input * 2
			)

			// Collect streaming results
			chunks = []
			transform.stream(
				( chunk, metadata ) => {
					chunks.append( chunk )
				},
				5
			)
			""",
			context
		);
		// @formatter:on

		var chunks = variables.getAsArray( Key.of( "chunks" ) );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isEqualTo( 1 );
		assertThat( chunks.get( 0 ).toString() ).isEqualTo( "10" );
	}

	@DisplayName( "Can create a runnable sequence" )
	@Test
	public void testRunnableSequence() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Create two transform runnables
			double = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( input ) => input * 2
			)
			addTen = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( input ) => input + 10
			)

			// Create a sequence
			sequence = new src.main.bx.models.runnables.AiRunnableSequence( [ double, addTen ] )

			// Run the sequence: 5 * 2 = 10, 10 + 10 = 20
			result = sequence.run( 5 )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ).toString() ).isEqualTo( "20" );
	}

	@DisplayName( "Runnable sequence can count steps" )
	@Test
	public void testRunnableSequenceCount() {
		// @formatter:off
		runtime.executeSource(
			"""
			transform1 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x * 2
			)
			transform2 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x + 1
			)
			transform3 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x * 3
			)

			sequence = new src.main.bx.models.runnables.AiRunnableSequence(
				[ transform1, transform2, transform3 ]
			)

			stepCount = sequence.count()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "stepCount" ) ).isEqualTo( 3 );
	}

	@DisplayName( "Can chain runnables using to() method" )
	@Test
	public void testRunnableChaining() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Create transforms
			double = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( input ) => input * 2
			)
			addFive = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( input ) => input + 5
			)

			// Chain them
			chain = double.to( addFive )

			// Should be: 3 * 2 = 6, 6 + 5 = 11
			result = chain.run( 3 )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ).toString() ).isEqualTo( "11" );
	}

	@DisplayName( "Can use transform() helper method" )
	@Test
	public void testTransformHelper() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Create a base transform
			double = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( input ) => input * 2
			)

			// Use the transform helper to add another step
			chain = double.transform( ( x ) => x + 100 )

			// Should be: 5 * 2 = 10, 10 + 100 = 110
			result = chain.run( 5 )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "result" ).toString() ).isEqualTo( "110" );
	}

	@DisplayName( "Can set and get name on runnable" )
	@Test
	public void testRunnableNaming() {
		// @formatter:off
		runtime.executeSource(
			"""
			transform = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x * 2
			)

			// Set a custom name
			transform.withName( "Doubler" )

			name = transform.getName()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "name" ) ).isEqualTo( "Doubler" );
	}

	@DisplayName( "Can manage default parameters" )
	@Test
	public void testRunnableParams() {
		// @formatter:off
		runtime.executeSource(
			"""
			transform = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x * 2
			)

			// Set default params
			transform.withParams( { temperature: 0.7, model: "gpt-4" } )

			// Merge with runtime params (runtime overrides default)
			merged = transform.getMergedParams( { model: "gpt-3.5", maxTokens: 100 } )

			// Clear params
			transform.clearParams()

			result = transform.getMergedParams();
			""",
			context
		);
		// @formatter:on

		var merged = variables.getAsStruct( Key.of( "merged" ) );
		assertThat( merged ).isNotNull();
		assertThat( merged.get( "temperature" ).toString() ).isEqualTo( "0.7" );
		assertThat( merged.get( "model" ) ).isEqualTo( "gpt-3.5" ); // Runtime override
		assertThat( merged.get( "maxTokens" ) ).isEqualTo( 100 );
		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 0 ); // Cleared
	}

	@DisplayName( "Sequence can get step information" )
	@Test
	public void testSequenceGetSteps() {
		// @formatter:off
		runtime.executeSource(
			"""
			t1 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x
			).withName( "First" )

			t2 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x
			).withName( "Second" )

			sequence = new src.main.bx.models.runnables.AiRunnableSequence( [ t1, t2 ] )

			steps = sequence.getSteps()
			""",
			context
		);
		// @formatter:on

		var steps = variables.getAsArray( Key.of( "steps" ) );
		assertThat( steps ).isNotNull();
		assertThat( steps.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Sequence to() creates new sequence with additional step" )
	@Test
	public void testSequenceToMethod() {
		// @formatter:off
		runtime.executeSource(
			"""
			t1 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x * 2
			)
			t2 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x + 5
			)
			t3 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x * 10
			)

			// Start with 2 steps
			seq1 = new src.main.bx.models.runnables.AiRunnableSequence( [ t1, t2 ] )
			count1 = seq1.count()

			// Add a third step
			seq2 = seq1.to( t3 )
			count2 = seq2.count()

			// Original should be unchanged
			countOriginal = seq1.count()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( "count1" ) ).isEqualTo( 2 );
		assertThat( variables.get( "count2" ) ).isEqualTo( 3 );
		assertThat( variables.get( "countOriginal" ) ).isEqualTo( 2 );
	}

	@DisplayName( "Can stream through a sequence" )
	@Test
	public void testSequenceStream() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Create transforms
			double = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x * 2
			)
			square = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x * x
			)

			sequence = new src.main.bx.models.runnables.AiRunnableSequence( [ double, square ] )

			// Collect streaming results
			chunks = []
			sequence.stream(
				( chunk, metadata ) => {
					chunks.append( chunk )
				},
				3
			)

			// Should be: 3 * 2 = 6, 6 * 6 = 36
			""",
			context
		);
		// @formatter:on

		var chunks = variables.getAsArray( Key.of( "chunks" ) );
		assertThat( chunks ).isNotNull();
		assertThat( chunks.size() ).isEqualTo( 1 );
		assertThat( chunks.get( 0 ).toString() ).isEqualTo( "36" );
	}

	@DisplayName( "Sequence print() outputs step information" )
	@Test
	public void testSequencePrint() {
		// @formatter:off
		runtime.executeSource(
			"""
			t1 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x
			).withName( "Transform1" )

			t2 = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x
			).withName( "Transform2" )

			sequence = new src.main.bx.models.runnables.AiRunnableSequence( [ t1, t2 ] )

			output = sequence.print()
			""",
			context
		);
		// @formatter:on

		var output = variables.getAsString( Key.of( "output" ) );
		assertThat( output ).contains( "AiRunnableSequence" );
		assertThat( output ).contains( "Transform1" );
		assertThat( output ).contains( "Transform2" );
	}

	@DisplayName( "Complex chaining scenario" )
	@Test
	public void testComplexChaining() {
		// @formatter:off
		runtime.executeSource(
			"""
			// Create a complex chain
			result = new src.main.bx.models.transformers.AiTransformRunnable(
				transformer = ( x ) => x + 1
			)
				.to( new src.main.bx.models.transformers.AiTransformRunnable(
					transformer = ( x ) => x * 2
				) )
				.transform( ( x ) => x - 5 )
				.to( new src.main.bx.models.transformers.AiTransformRunnable(
					transformer = ( x ) => x * 10
				) )
				.run( 10 )

				println( result )

			// Should be: 10 + 1 = 11, 11 * 2 = 22, 22 - 5 = 17, 17 * 10 = 170
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( result ).toString() ).isEqualTo( "170" );
	}

	// =========================================================================
	// Tool Methods
	// =========================================================================

	@DisplayName( "addTool() adds a single tool to the runnable" )
	@Test
	public void testAddSingleTool() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            tool = aiTool( "myTool", "A test tool", ( required string query ) => "result" )
            transform.addTool( tool )
            toolCount = transform.getTools().len()
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "toolCount" ) ).isEqualTo( 1 );
	}

	@DisplayName( "withTools() binds an array of tools to the runnable" )
	@Test
	public void testWithToolsAddsMultipleTools() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            tool1 = aiTool( "tool1", "First tool", ( required string query ) => "one" )
            tool2 = aiTool( "tool2", "Second tool", ( required string query ) => "two" )
            transform.withTools( [ tool1, tool2 ] )
            toolCount = transform.getTools().len()
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "toolCount" ) ).isEqualTo( 2 );
	}

	@DisplayName( "withTools() accepts a single tool (wraps in array automatically)" )
	@Test
	public void testWithToolsAcceptsSingleTool() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            tool = aiTool( "singleTool", "A standalone tool", ( required string query ) => "result" )
            transform.withTools( tool )
            toolCount = transform.getTools().len()
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "toolCount" ) ).isEqualTo( 1 );
	}

	@DisplayName( "hasTool() finds a tool by name string" )
	@Test
	public void testHasToolByName() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            tool = aiTool( "namedTool", "A named tool", ( required string query ) => "result" )
            transform.addTool( tool )
            found    = transform.hasTool( "namedTool" )
            notFound = transform.hasTool( "missingTool" )
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "found" ) ).isEqualTo( true );
		assertThat( variables.get( "notFound" ) ).isEqualTo( false );
	}

	@DisplayName( "hasTool() finds a tool by object reference" )
	@Test
	public void testHasToolByReference() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            tool      = aiTool( "refTool",   "Registered tool", ( required string query ) => "result" )
            otherTool = aiTool( "otherTool", "Not registered",  ( required string query ) => "result" )
            transform.addTool( tool )
            foundByRef    = transform.hasTool( tool )
            notFoundByRef = transform.hasTool( otherTool )
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "foundByRef" ) ).isEqualTo( true );
		assertThat( variables.get( "notFoundByRef" ) ).isEqualTo( false );
	}

	@DisplayName( "getTool() retrieves a registered tool by name" )
	@Test
	public void testGetToolByName() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            tool = aiTool( "fetchTool", "Fetch this tool", ( required string query ) => "result" )
            transform.addTool( tool )
            retrieved     = transform.getTool( "fetchTool" )
            retrievedName = retrieved.getName()
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "retrievedName" ) ).isEqualTo( "fetchTool" );
	}

	@DisplayName( "removeTools() removes a specific tool while keeping others" )
	@Test
	public void testRemoveTools() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            keepTool   = aiTool( "keepTool",   "Keep this one",    ( required string query ) => "keep" )
            removeTool = aiTool( "removeTool", "Remove this one",  ( required string query ) => "remove" )
            transform.withTools( [ keepTool, removeTool ] )
            transform.removeTools( removeTool )
            toolCount     = transform.getTools().len()
            keepRemains   = transform.hasTool( "keepTool" )
            removeIsGone  = transform.hasTool( "removeTool" )
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "toolCount" ) ).isEqualTo( 1 );
		assertThat( variables.get( "keepRemains" ) ).isEqualTo( true );
		assertThat( variables.get( "removeIsGone" ) ).isEqualTo( false );
	}

	@DisplayName( "clearTools() removes all registered tools" )
	@Test
	public void testClearTools() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            transform.withTools( [
                aiTool( "tool1", "Tool one",   ( required string query ) => "1" ),
                aiTool( "tool2", "Tool two",   ( required string query ) => "2" ),
                aiTool( "tool3", "Tool three", ( required string query ) => "3" )
            ] )
            beforeClear = transform.getTools().len()
            transform.clearTools()
            afterClear = transform.getTools().len()
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "beforeClear" ) ).isEqualTo( 3 );
		assertThat( variables.get( "afterClear" ) ).isEqualTo( 0 );
	}

	@DisplayName( "listTools() returns a name/description summary for each tool" )
	@Test
	public void testListTools() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            transform.withTools( [
                aiTool( "alpha", "Alpha description", ( required string query ) => "a" ),
                aiTool( "beta",  "Beta description",  ( required string query ) => "b" )
            ] )
            summary      = transform.listTools()
            summarySize  = summary.len()
            firstName    = summary[ 1 ].name
            firstDesc    = summary[ 1 ].description
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "summarySize" ) ).isEqualTo( 2 );
		assertThat( variables.get( "firstName" ) ).isEqualTo( "alpha" );
		assertThat( variables.get( "firstDesc" ) ).isEqualTo( "Alpha description" );
	}

	@DisplayName( "bindTools() is a deprecated alias for withTools()" )
	@Test
	public void testBindToolsDeprecated() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            tool = aiTool( "boundTool", "Bound via deprecated method", ( required string query ) => "result" )
            transform.bindTools( tool )
            toolCount = transform.getTools().len()
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "toolCount" ) ).isEqualTo( 1 );
	}

	@DisplayName( "addTools() is a deprecated alias for withTools()" )
	@Test
	public void testAddToolsDeprecated() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            tool1 = aiTool( "depTool1", "Deprecated add one", ( required string query ) => "1" )
            tool2 = aiTool( "depTool2", "Deprecated add two", ( required string query ) => "2" )
            transform.addTools( [ tool1, tool2 ] )
            toolCount = transform.getTools().len()
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "toolCount" ) ).isEqualTo( 2 );
	}

	// =========================================================================
	// Other Untested AiBaseRunnable Methods
	// =========================================================================

	@DisplayName( "pipe() is an alias for to() and produces the same chained result" )
	@Test
	public void testPipeAlias() {
		// @formatter:off
        runtime.executeSource(
            """
            double = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x * 2
            )
            addThree = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x + 3
            )
            // 4 * 2 = 8, 8 + 3 = 11
            result = double.pipe( addThree ).run( 4 )
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "result" ).toString() ).isEqualTo( "11" );
	}

	@DisplayName( "transformAndRun() applies a transform and executes in a single call" )
	@Test
	public void testTransformAndRun() {
		// @formatter:off
        runtime.executeSource(
            """
            double = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x * 2
            )
            // double 3 -> 6, then +7 -> 13
            result = double.transformAndRun( x => x + 7, 3 )
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "result" ).toString() ).isEqualTo( "13" );
	}

	@DisplayName( "Can set, merge, and clear default options on a runnable" )
	@Test
	public void testWithOptions() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            transform.withOptions( { returnFormat: "json", timeout: 30 } )

            // Runtime option overrides default
            merged = transform.getMergedOptions( { returnFormat: "single" } )

            transform.clearOptions()
            cleared = transform.getMergedOptions()
            """,
            context
        );
        // @formatter:on

		var merged = variables.getAsStruct( Key.of( "merged" ) );
		assertThat( merged ).isNotNull();
		assertThat( merged.get( "returnFormat" ) ).isEqualTo( "single" ); // Runtime override
		assertThat( merged.get( "timeout" ) ).isEqualTo( 30 );

		var cleared = variables.getAsStruct( Key.of( "cleared" ) );
		assertThat( cleared.size() ).isEqualTo( 0 );
	}

	@DisplayName( "singleMessage() sets returnFormat option to 'single'" )
	@Test
	public void testSingleMessageHelper() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            transform.singleMessage()
            returnFormat = transform.getMergedOptions().returnFormat
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "returnFormat" ) ).isEqualTo( "single" );
	}

	@DisplayName( "allMessages() sets returnFormat option to 'all'" )
	@Test
	public void testAllMessagesHelper() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            transform.allMessages()
            returnFormat = transform.getMergedOptions().returnFormat
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "returnFormat" ) ).isEqualTo( "all" );
	}

	@DisplayName( "asJson() sets returnFormat option to 'json'" )
	@Test
	public void testAsJsonHelper() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            transform.asJson()
            returnFormat = transform.getMergedOptions().returnFormat
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "returnFormat" ) ).isEqualTo( "json" );
	}

	@DisplayName( "asXml() sets returnFormat option to 'xml'" )
	@Test
	public void testAsXmlHelper() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            transform.asXml()
            returnFormat = transform.getMergedOptions().returnFormat
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "returnFormat" ) ).isEqualTo( "xml" );
	}

	@DisplayName( "rawResponse() sets returnFormat option to 'raw'" )
	@Test
	public void testRawResponseHelper() {
		// @formatter:off
        runtime.executeSource(
            """
            transform = new src.main.bx.models.transformers.AiTransformRunnable(
                transformer = ( x ) => x
            )
            transform.rawResponse()
            returnFormat = transform.getMergedOptions().returnFormat
            """,
            context
        );
        // @formatter:on

		assertThat( variables.get( "returnFormat" ) ).isEqualTo( "raw" );
	}

}
