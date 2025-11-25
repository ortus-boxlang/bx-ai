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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class aiAgentTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "It can create a basic agent" )
	public void testBasicAgentCreation() {
		runtime.executeSource(
		    """
		    agent = aiAgent(
		        name: "TestAgent",
		        description: "A test agent",
		        instructions: "You are a helpful assistant"
		    )

		    result = agent.getConfig()
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertEquals( "TestAgent", result.get( "name" ) );
		assertEquals( "A test agent", result.get( "description" ) );
		assertEquals( "You are a helpful assistant", result.get( "instructions" ) );
		// Should have default window memory
		assertEquals( 1L, ( long ) ( ( Number ) result.get( "memoryCount" ) ).intValue() );
	}

	@Test
	@DisplayName( "It can create an agent with tools" )
	public void testAgentWithTools() {
		runtime.executeSource(
		    """
		    getTool = aiTool(
		        "get_test_data",
		        "Get test data",
		        key => {
		            return { result: "test_value" }
		        }
		    ).describeKey( "The key to retrieve" )

		    agent = aiAgent(
		        name: "ToolAgent",
		        tools: [ getTool ]
		    )

		    config = agent.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( 1L, ( long ) ( ( Number ) config.get( "toolCount" ) ).intValue() );
	}

	@Test
	@DisplayName( "It can create an agent with single memory" )
	public void testAgentWithSingleMemory() {
		runtime.executeSource(
		    """
		    memory = aiMemory( type: "simple" )

		    agent = aiAgent(
		        name: "MemoryAgent",
		        memory: memory
		    )

		    config = agent.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( 1L, ( long ) ( ( Number ) config.get( "memoryCount" ) ).intValue() );
	}

	@Test
	@DisplayName( "It can create an agent with multiple memories" )
	public void testAgentWithMultipleMemories() {
		runtime.executeSource(
		    """
		    memory1 = aiMemory( type: "window" )
		    memory2 = aiMemory( type: "window" )

		    agent = aiAgent(
		        name: "MultiMemoryAgent",
		        memory: [ memory1, memory2 ]
		    )

		    config = agent.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( 2L, ( long ) ( ( Number ) config.get( "memoryCount" ) ).intValue() );
	}

	@Test
	@DisplayName( "It supports fluent API for configuration" )
	public void testFluentAPI() {
		runtime.executeSource(
		    """
		    tool = aiTool(
		        "test_tool",
		        "A test tool",
		        args => { return "ok" }
		    )

		    model = aiModel( "openai" )

		    agent = aiAgent()
		        .setName( "FluentAgent" )
		        .setDescription( "Built with fluent API" )
		        .setInstructions( "Be helpful" )
		        .setModel( model )
		        .addTool( tool )

		    config = agent.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( "FluentAgent", config.get( "name" ) );
		assertEquals( "Built with fluent API", config.get( "description" ) );
		assertEquals( 1L, ( long ) ( ( Number ) config.get( "toolCount" ) ).intValue() );
	}

	@Test
	@DisplayName( "It can add memory using fluent API" )
	public void testFluentMemoryAPI() {
		runtime.executeSource(
		    """
		    memory1 = aiMemory( type: "window" )
		    memory2 = aiMemory( type: "window" )

		    agent = aiAgent()
		        .addMemory( memory1 )
		        .addMemory( memory2 )

		    config = agent.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( 3L, ( long ) ( ( Number ) config.get( "memoryCount" ) ).intValue() ); // 2 added + 1 default
	}

	@Test
	@DisplayName( "It can set parameters using fluent API" )
	public void testFluentParamsAPI() {
		runtime.executeSource(
		    """
		    agent = aiAgent()
		        .setParam( "temperature", 0.7 )
		        .setParam( "max_tokens", 100 )
		        .setParams({ top_p: 0.9 })

		    // We can't easily inspect params, but ensure no errors
		    result = true
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "result" ) ) );
	}

	@Test
	@DisplayName( "It can clear memory" )
	public void testClearMemory() {
		runtime.executeSource(
		    """
		    memory = aiMemory( type: "window" )
		    memory.add({ role: "user", content: "Hello" })

		    agent = aiAgent( memory: memory )

		    // Memory should have messages
		    beforeClear = agent.getMemoryMessages().len()

		    // Clear and check
		    agent.clearMemory()
		    afterClear = agent.getMemoryMessages().len()
		    """,
		    context
		);

		assertEquals( 1L, ( long ) variables.getAsInteger( Key.of( "beforeClear" ) ) );
		assertEquals( 0L, ( long ) variables.getAsInteger( Key.of( "afterClear" ) ) );
	}

	@Test
	@DisplayName( "It can build system message from description and instructions" )
	public void testSystemMessageBuilding() {
		runtime.executeSource(
		    """
		    agent = aiAgent(
		        description: "A coding assistant",
		        instructions: "Always provide code examples"
		    )

		    // We can't directly test the private method, but config should show values
		    config = agent.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( "A coding assistant", config.get( "description" ) );
		assertEquals( "Always provide code examples", config.get( "instructions" ) );
	}

	@Test
	@DisplayName( "It can be used in pipelines" )
	public void testAgentInPipeline() {
		runtime.executeSource(
		    """
		    agent = aiAgent(
		        name: "PipelineAgent",
		        description: "Works in pipelines"
		    )

		    // Agents extend AiBaseRunnable, so they can be chained
		    pipeline = agent.to( aiTransform( r => "transformed" ) )

		    // Ensure pipeline was created
		    result = !isNull( pipeline )
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "result" ) ) );
	}

	@Test
	@DisplayName( "Real AI Agent using default model and default memory" )
	public void testRealAIAgent() {
		// @formatter:off
		runtime.executeSource(
		    """
		    agent = aiAgent(
		        name: "RealAgent",
		        description: "An agent that uses real AI",
		        instructions: "Provide concise answers"
		    )

		    response = agent.run( "What is BoxLang?" )

		    println( response )
		    """,
		    context
		);
		// @formatter:on

		var response = variables.getAsString( Key.of( "response" ) );
		assertTrue( response != null && !response.isEmpty() );

	}

}
