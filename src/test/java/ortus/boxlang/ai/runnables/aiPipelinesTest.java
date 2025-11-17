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
package ortus.boxlang.ai.runnables;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

public class aiPipelinesTest extends BaseIntegrationTest {

	@Test
	public void testMessageToDefaultModel() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create a simple message -> model pipeline
				pipeline = aiMessage()
					.system( "You are a helpful assistant" )
					.user( "Say hello" )
					.toDefaultModel()

				result = {
					pipelineCreated: !isNull( pipeline ),
					stepCount: pipeline.count(),
					hasRun: structKeyExists( pipeline, "run" )
				}
			""",
			context
		);
		// @formatter:on

		var resultStruct = variables.getAsStruct( result );
		assertThat( resultStruct.get( Key.of( "pipelineCreated" ) ) ).isEqualTo( true );
		assertThat( ( ( Number ) resultStruct.get( Key.of( "stepCount" ) ) ).intValue() ).isEqualTo( 2 );
		assertThat( resultStruct.get( Key.of( "hasRun" ) ) ).isEqualTo( true );
	}

	@Test
	public void testMessageToModelToTransform() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create message -> model -> transform pipeline
				pipeline = aiMessage()
					.user( "Hello ${name}" )
					.to( aiModel( "openai" ) )
					.transform( r => r.content ?: "no content" )

				result = {
					stepCount: pipeline.count(),
					steps: pipeline.getSteps()
				}

				println( result )
			""",
			context
		);
		// @formatter:on

		var resultStruct = variables.getAsStruct( result );
		assertThat( ( ( Number ) resultStruct.get( Key.of( "stepCount" ) ) ).intValue() ).isEqualTo( 3 );

		var steps = ( Array ) resultStruct.get( Key.of( "steps" ) );
		assertThat( steps.size() ).isEqualTo( 3 );
		assertThat( steps.get( 0 ).toString() ).contains( "AiMessage" );
		assertThat( steps.get( 1 ).toString() ).contains( "AiModel" );
		assertThat( steps.get( 2 ).toString() ).contains( "Transform" );
	}

	@Test
	public void testPipelineWithBindings() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create pipeline with template bindings
				pipeline = aiMessage()
					.system( "You are ${personality}" )
					.user( "Tell me about ${topic}" )
					.toDefaultModel()
					.transform( r => r.content ?: "no response" )

				result = {
					pipelineCreated: !isNull( pipeline ),
					stepCount: pipeline.count()
				}
			""",
			context
		);
		// @formatter:on

		var resultStruct = variables.getAsStruct( result );
		assertThat( resultStruct.get( Key.of( "pipelineCreated" ) ) ).isEqualTo( true );
		assertThat( ( ( Number ) resultStruct.get( Key.of( "stepCount" ) ) ).intValue() ).isEqualTo( 3 );
	}

	@Test
	public void testPipelineWithParams() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create pipeline with model parameters
				pipeline = aiMessage()
					.user( "Hello" )
					.to(
						aiModel( "openai" )
							.withParams( { temperature: 0.7, model: "gpt-4" } )
					)
					.transform( r => r.content ?: "" )

				result = {
					stepCount: pipeline.count()
				}
			""",
			context
		);
		// @formatter:on

		var resultStruct = variables.getAsStruct( result );
		assertThat( ( ( Number ) resultStruct.get( Key.of( "stepCount" ) ) ).intValue() ).isEqualTo( 3 );
	}

	@Test
	public void testMultipleTransforms() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create pipeline with multiple transformations
				pipeline = aiMessage()
					.user( "Count to 3" )
					.toDefaultModel()
					.transform( r => r.content ?: "" )
					.transform( s => s.ucase() )
					.transform( s => s.len() )

				result = {
					stepCount: pipeline.count()
				}
			""",
			context
		);
		// @formatter:on

		var resultStruct = variables.getAsStruct( result );
		assertThat( ( ( Number ) resultStruct.get( Key.of( "stepCount" ) ) ).intValue() ).isEqualTo( 5 );
	}

	@Test
	public void testNamedPipelineSteps() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create pipeline with named steps
				pipeline = aiMessage()
					.user( "Hello" )
					.withName( "greeting" )
					.to(
						aiModel( "openai" )
							.withName( "gpt-model" )
					)
					.transform( r => r.content ?: "" )

				steps = pipeline.getSteps()
				result = {
					step1Name: steps[1].name,
					step2Name: steps[2].name
				}
			""",
			context
		);
		// @formatter:on

		var resultStruct = variables.getAsStruct( result );
		assertThat( resultStruct.get( Key.of( "step1Name" ) ) ).isEqualTo( "greeting" );
		assertThat( resultStruct.get( Key.of( "step2Name" ) ) ).isEqualTo( "gpt-model" );
	}

	@Test
	public void testPipelineImmutability() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Test that chaining creates new sequences
				msg = aiMessage().user( "Hello" )
				pipeline1 = msg.toDefaultModel()
				pipeline2 = msg.to( aiModel( "openai" ) )

				result = {
					msg_count: msg.getName().len() > 0 ? 1 : 0,
					pipeline1_count: pipeline1.count(),
					pipeline2_count: pipeline2.count(),
					different: pipeline1 != pipeline2
				}
			""",
			context
		);
		// @formatter:on

		var resultStruct = variables.getAsStruct( result );
		assertThat( ( ( Number ) resultStruct.get( Key.of( "pipeline1_count" ) ) ).intValue() ).isEqualTo( 2 );
		assertThat( ( ( Number ) resultStruct.get( Key.of( "pipeline2_count" ) ) ).intValue() ).isEqualTo( 2 );
		assertThat( resultStruct.get( Key.of( "different" ) ) ).isEqualTo( true );
	}

	@Test
	public void testComplexPipeline() {
		// @formatter:off
		runtime.executeSource(
			"""
				// Create a complex multi-step pipeline
				pipeline = aiMessage()
					.system( "You are ${role}" )
					.user( "Question: ${question}" )
					.withName( "prompt-template" )
					.bind( { role: "teacher" } )
					.to(
						aiModel( "openai" )
							.withName( "llm" )
							.withParams( { temperature: 0.5 } )
					)
					.transform( r => r.content ?: "" )
					.transform( s => s.trim() )
					.withName( "complete-pipeline" )

				result = {
					totalSteps: pipeline.count(),
					pipelineName: pipeline.getName()
				}
			""",
			context
		);
		// @formatter:on

		var resultStruct = variables.getAsStruct( result );
		assertThat( ( ( Number ) resultStruct.get( Key.of( "totalSteps" ) ) ).intValue() ).isEqualTo( 4 );
		assertThat( resultStruct.get( Key.of( "pipelineName" ) ) ).isEqualTo( "complete-pipeline" );
	}
}
