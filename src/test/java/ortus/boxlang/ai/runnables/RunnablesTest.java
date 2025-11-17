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

			println( transform.getName() )

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
			merged = transform.mergeParams( { model: "gpt-3.5", maxTokens: 100 } )
			""",
			context
		);
		// @formatter:on

		var merged = variables.getAsStruct( Key.of( "merged" ) );
		assertThat( merged ).isNotNull();
		assertThat( merged.get( "temperature" ).toString() ).isEqualTo( "0.7" );
		assertThat( merged.get( "model" ) ).isEqualTo( "gpt-3.5" ); // Runtime override
		assertThat( merged.get( "maxTokens" ) ).isEqualTo( 100 );
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

}
