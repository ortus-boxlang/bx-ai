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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

public class aiAgentTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

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
		    println( result )
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
		        instructions: "Provide concise answers",
				// A tool from the default set provided by the BoxLang MCP server, which should be available without additional config in the test environment
				tools: [ "now@bxai" ],
				// Add the BoxLang MCP doc server
				mcpServers: [ "https://boxlang.ortusbooks.com/~gitbook/mcp" ]
		    )

			println( agent.getConfig() )

		    response = agent.run( "What is BoxLang?", {},  {
				logResponseToConsole: true,
				logRequestToConsole: true
			} )

		    println( response )
		    """,
		    context
		);
		// @formatter:on

		var response = variables.getAsString( Key.of( "response" ) );
		assertTrue( response != null && !response.isEmpty() );

	}

	// ==================== SUB-AGENT TESTS ====================

	@Test
	@DisplayName( "It can create an agent with sub-agents" )
	public void testAgentWithSubAgents() {
		runtime.executeSource(
		    """
		    // Create sub-agents
		    mathAgent = aiAgent(
		        name: "MathAgent",
		        description: "A math specialist agent",
		        instructions: "You help with mathematical calculations"
		    )

		    codeAgent = aiAgent(
		        name: "CodeAgent",
		        description: "A coding specialist agent",
		        instructions: "You help with code review and writing"
		    )

		    // Create parent agent with sub-agents
		    mainAgent = aiAgent(
		        name: "MainAgent",
		        description: "A main orchestrator agent",
		        instructions: "Delegate to sub-agents when appropriate",
		        subAgents: [ mathAgent, codeAgent ]
		    )

		    config = mainAgent.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( "MainAgent", config.get( "name" ) );
		assertEquals( 2L, ( long ) ( ( Number ) config.get( "subAgentCount" ) ).intValue() );
	}

	@Test
	@DisplayName( "It can add sub-agents using fluent API" )
	public void testFluentSubAgentAPI() {
		runtime.executeSource(
		    """
		    subAgent1 = aiAgent(
		        name: "SubAgent1",
		        description: "First sub-agent"
		    )

		    subAgent2 = aiAgent(
		        name: "SubAgent2",
		        description: "Second sub-agent"
		    )

		    mainAgent = aiAgent( name: "MainAgent" )
		        .addSubAgent( subAgent1 )
		        .addSubAgent( subAgent2 )

		    config = mainAgent.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( 2L, ( long ) ( ( Number ) config.get( "subAgentCount" ) ).intValue() );
	}

	@Test
	@DisplayName( "It can get sub-agents" )
	public void testGetSubAgents() {
		runtime.executeSource(
		    """
		    subAgent = aiAgent(
		        name: "SubAgent",
		        description: "A sub-agent"
		    )

		    mainAgent = aiAgent(
		        name: "MainAgent",
		        subAgents: [ subAgent ]
		    )

		    subAgents = mainAgent.getSubAgents()
		    subAgentCount = subAgents.len()
		    """,
		    context
		);

		assertEquals( 1L, ( long ) variables.getAsInteger( Key.of( "subAgentCount" ) ) );
	}

	@Test
	@DisplayName( "It can get a sub-agent by name" )
	public void testGetSubAgentByName() {
		runtime.executeSource(
		    """
		    mathAgent = aiAgent(
		        name: "MathAgent",
		        description: "Math specialist"
		    )

		    codeAgent = aiAgent(
		        name: "CodeAgent",
		        description: "Code specialist"
		    )

		    mainAgent = aiAgent(
		        name: "MainAgent",
		        subAgents: [ mathAgent, codeAgent ]
		    )

		    foundAgent = mainAgent.getSubAgent( "MathAgent" )
		    foundAgentName = foundAgent.getConfig().name
		    """,
		    context
		);

		assertEquals( "MathAgent", variables.getAsString( Key.of( "foundAgentName" ) ) );
	}

	@Test
	@DisplayName( "It can check if a sub-agent exists" )
	public void testHasSubAgent() {
		runtime.executeSource(
		    """
		    subAgent = aiAgent(
		        name: "SubAgent",
		        description: "A sub-agent"
		    )

		    mainAgent = aiAgent(
		        name: "MainAgent",
		        subAgents: [ subAgent ]
		    )

		    hasExisting = mainAgent.hasSubAgent( "SubAgent" )
		    hasNonExisting = mainAgent.hasSubAgent( "NonExistent" )
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "hasExisting" ) ) );
		assertTrue( !variables.getAsBoolean( Key.of( "hasNonExisting" ) ) );
	}

	@Test
	@DisplayName( "It can set sub-agents using setSubAgents" )
	public void testSetSubAgents() {
		runtime.executeSource(
		    """
		    subAgent1 = aiAgent( name: "SubAgent1", description: "First" )
		    subAgent2 = aiAgent( name: "SubAgent2", description: "Second" )
		    subAgent3 = aiAgent( name: "SubAgent3", description: "Third" )

		    mainAgent = aiAgent(
		        name: "MainAgent",
		        subAgents: [ subAgent1 ]
		    )

		    // Check initial count
		    initialCount = mainAgent.getConfig().subAgentCount

		    // Replace with new sub-agents
		    mainAgent.setSubAgents( [ subAgent2, subAgent3 ] )

		    // Check updated count
		    updatedCount = mainAgent.getConfig().subAgentCount
		    """,
		    context
		);

		assertEquals( 1L, ( long ) variables.getAsInteger( Key.of( "initialCount" ) ) );
		assertEquals( 2L, ( long ) variables.getAsInteger( Key.of( "updatedCount" ) ) );
	}

	@Test
	@DisplayName( "Sub-agents are exposed in getConfig" )
	public void testSubAgentsInConfig() {
		runtime.executeSource(
		    """
		    subAgent = aiAgent(
		        name: "TestSubAgent",
		        description: "A test sub-agent description"
		    )

		    mainAgent = aiAgent(
		        name: "MainAgent",
		        subAgents: [ subAgent ]
		    )

		    config = mainAgent.getConfig()
		    subAgentInfo = config.subAgents[ 1 ]
		    """,
		    context
		);

		var subAgentInfo = variables.getAsStruct( Key.of( "subAgentInfo" ) );
		assertEquals( "TestSubAgent", subAgentInfo.get( "name" ) );
		assertEquals( "A test sub-agent description", subAgentInfo.get( "description" ) );
	}

	// ==================== PARENT-CHILD HELPER TESTS ====================

	@Test
	@DisplayName( "addSubAgent automatically sets parentAgent on the sub-agent" )
	public void testAddSubAgentSetsParent() {
		runtime.executeSource(
		    """
		    parent = aiAgent( name: "Parent", description: "Parent agent" )
		    child  = aiAgent( name: "Child",  description: "Child agent" )
		    parent.addSubAgent( child )

		    hasParent  = child.hasParentAgent()
		    parentName = child.getParentAgent().getAgentName()
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "hasParent" ) ) );
		assertEquals( "Parent", variables.getAsString( Key.of( "parentName" ) ) );
	}

	@Test
	@DisplayName( "setSubAgents clears parentAgent on replaced sub-agents" )
	public void testSetSubAgentsClearsOldParent() {
		runtime.executeSource(
		    """
		    parent   = aiAgent( name: "Parent",   description: "Parent" )
		    oldChild = aiAgent( name: "OldChild", description: "Old" )
		    newChild = aiAgent( name: "NewChild", description: "New" )

		    parent.addSubAgent( oldChild )
		    parent.setSubAgents( [ newChild ] )

		    oldChildHasParent = oldChild.hasParentAgent()
		    newChildHasParent = newChild.hasParentAgent()
		    """,
		    context
		);

		assertTrue( !variables.getAsBoolean( Key.of( "oldChildHasParent" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "newChildHasParent" ) ) );
	}

	@Test
	@DisplayName( "hasParentAgent returns false for root agents" )
	public void testHasParentAgentFalseForRoot() {
		runtime.executeSource(
		    """
		    agent  = aiAgent( name: "Root", description: "Root agent" )
		    result = agent.hasParentAgent()
		    """,
		    context
		);

		assertTrue( !variables.getAsBoolean( Key.of( "result" ) ) );
	}

	@Test
	@DisplayName( "isRootAgent returns true for top-level agents" )
	public void testIsRootAgentTrue() {
		runtime.executeSource(
		    """
		    agent  = aiAgent( name: "Root", description: "Root agent" )
		    result = agent.isRootAgent()
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "result" ) ) );
	}

	@Test
	@DisplayName( "isRootAgent returns false for sub-agents" )
	public void testIsRootAgentFalseForChild() {
		runtime.executeSource(
		    """
		    parent = aiAgent( name: "Parent", description: "Parent" )
		    child  = aiAgent( name: "Child",  description: "Child" )
		    parent.addSubAgent( child )

		    result = child.isRootAgent()
		    """,
		    context
		);

		assertTrue( !variables.getAsBoolean( Key.of( "result" ) ) );
	}

	@Test
	@DisplayName( "getRootAgent returns self when agent has no parent" )
	public void testGetRootAgentReturnsSelf() {
		runtime.executeSource(
		    """
		    agent    = aiAgent( name: "Root", description: "Root agent" )
		    rootName = agent.getRootAgent().getAgentName()
		    """,
		    context
		);

		assertEquals( "Root", variables.getAsString( Key.of( "rootName" ) ) );
	}

	@Test
	@DisplayName( "getRootAgent walks up to the top-level agent" )
	public void testGetRootAgentWalksUp() {
		runtime.executeSource(
		    """
		    root   = aiAgent( name: "Root",   description: "Root" )
		    middle = aiAgent( name: "Middle", description: "Middle" )
		    leaf   = aiAgent( name: "Leaf",   description: "Leaf" )

		    root.addSubAgent( middle )
		    middle.addSubAgent( leaf )

		    rootName = leaf.getRootAgent().getAgentName()
		    """,
		    context
		);

		assertEquals( "Root", variables.getAsString( Key.of( "rootName" ) ) );
	}

	@Test
	@DisplayName( "getAgentDepth returns 0 for root agents" )
	public void testGetAgentDepthRoot() {
		runtime.executeSource(
		    """
		    agent = aiAgent( name: "Root", description: "Root" )
		    depth = agent.getAgentDepth()
		    """,
		    context
		);

		assertEquals( 0L, ( long ) variables.getAsInteger( Key.of( "depth" ) ) );
	}

	@Test
	@DisplayName( "getAgentDepth returns correct depth for each level" )
	public void testGetAgentDepthNested() {
		runtime.executeSource(
		    """
		    root   = aiAgent( name: "Root",   description: "Root" )
		    middle = aiAgent( name: "Middle", description: "Middle" )
		    leaf   = aiAgent( name: "Leaf",   description: "Leaf" )

		    root.addSubAgent( middle )
		    middle.addSubAgent( leaf )

		    rootDepth   = root.getAgentDepth()
		    middleDepth = middle.getAgentDepth()
		    leafDepth   = leaf.getAgentDepth()
		    """,
		    context
		);

		assertEquals( 0L, ( long ) variables.getAsInteger( Key.of( "rootDepth" ) ) );
		assertEquals( 1L, ( long ) variables.getAsInteger( Key.of( "middleDepth" ) ) );
		assertEquals( 2L, ( long ) variables.getAsInteger( Key.of( "leafDepth" ) ) );
	}

	@Test
	@DisplayName( "getAgentPath returns slash-delimited path from root to agent" )
	public void testGetAgentPath() {
		runtime.executeSource(
		    """
		    root   = aiAgent( name: "root",   description: "Root" )
		    middle = aiAgent( name: "middle", description: "Middle" )
		    leaf   = aiAgent( name: "leaf",   description: "Leaf" )

		    root.addSubAgent( middle )
		    middle.addSubAgent( leaf )

		    rootPath   = root.getAgentPath()
		    middlePath = middle.getAgentPath()
		    leafPath   = leaf.getAgentPath()
		    """,
		    context
		);

		assertEquals( "/root", variables.getAsString( Key.of( "rootPath" ) ) );
		assertEquals( "/root/middle", variables.getAsString( Key.of( "middlePath" ) ) );
		assertEquals( "/root/middle/leaf", variables.getAsString( Key.of( "leafPath" ) ) );
	}

	@Test
	@DisplayName( "getAncestors returns empty array for root agents" )
	public void testGetAncestorsEmpty() {
		runtime.executeSource(
		    """
		    agent = aiAgent( name: "Root", description: "Root" )
		    count = agent.getAncestors().len()
		    """,
		    context
		);

		assertEquals( 0L, ( long ) variables.getAsInteger( Key.of( "count" ) ) );
	}

	@Test
	@DisplayName( "getAncestors returns ordered array from immediate parent to root" )
	public void testGetAncestors() {
		runtime.executeSource(
		    """
		    root   = aiAgent( name: "Root",   description: "Root" )
		    middle = aiAgent( name: "Middle", description: "Middle" )
		    leaf   = aiAgent( name: "Leaf",   description: "Leaf" )

		    root.addSubAgent( middle )
		    middle.addSubAgent( leaf )

		    ancestors      = leaf.getAncestors()
		    ancestorCount  = ancestors.len()
		    firstAncestor  = ancestors[ 1 ].getAgentName()
		    secondAncestor = ancestors[ 2 ].getAgentName()
		    """,
		    context
		);

		assertEquals( 2L, ( long ) variables.getAsInteger( Key.of( "ancestorCount" ) ) );
		assertEquals( "Middle", variables.getAsString( Key.of( "firstAncestor" ) ) );
		assertEquals( "Root", variables.getAsString( Key.of( "secondAncestor" ) ) );
	}

	@Test
	@DisplayName( "clearParentAgent removes the parent reference" )
	public void testClearParentAgent() {
		runtime.executeSource(
		    """
		    parent = aiAgent( name: "Parent", description: "Parent" )
		    child  = aiAgent( name: "Child",  description: "Child" )
		    parent.addSubAgent( child )

		    hasBefore = child.hasParentAgent()
		    child.clearParentAgent()
		    hasAfter = child.hasParentAgent()
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "hasBefore" ) ) );
		assertTrue( !variables.getAsBoolean( Key.of( "hasAfter" ) ) );
	}

	@Test
	@DisplayName( "setParentAgent throws when an agent is set as its own parent" )
	public void testSetParentAgentSelfReferenceThrows() {
		assertThrows( Exception.class, () -> {
			runtime.executeSource(
			    """
			    agent = aiAgent( name: "Agent", description: "Agent" )
			    agent.setParentAgent( agent )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "setParentAgent throws when the assignment would create a cycle" )
	public void testSetParentAgentCycleThrows() {
		assertThrows( Exception.class, () -> {
			runtime.executeSource(
			    """
			    parent = aiAgent( name: "Parent", description: "Parent" )
			    child  = aiAgent( name: "Child",  description: "Child" )
			    parent.addSubAgent( child )
			    // child -> parent -> child would be a cycle
			    child.addSubAgent( parent )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "getConfig includes parentAgent name, agentDepth, and agentPath" )
	public void testGetConfigIncludesHierarchyFields() {
		runtime.executeSource(
		    """
		    parent = aiAgent( name: "parent", description: "Parent" )
		    child  = aiAgent( name: "child",  description: "Child" )
		    parent.addSubAgent( child )

		    config = child.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( "parent", config.get( "parentAgent" ) );
		assertEquals( 1L, ( long ) ( ( Number ) config.get( "agentDepth" ) ).intValue() );
		assertEquals( "/parent/child", config.get( "agentPath" ) );
	}

	@Test
	@DisplayName( "getConfig shows empty parentAgent and depth 0 for root agents" )
	public void testGetConfigRootHierarchyFields() {
		runtime.executeSource(
		    """
		    agent  = aiAgent( name: "root", description: "Root" )
		    config = agent.getConfig()
		    """,
		    context
		);

		var config = variables.getAsStruct( Key.of( "config" ) );
		assertEquals( "", config.get( "parentAgent" ) );
		assertEquals( 0L, ( long ) ( ( Number ) config.get( "agentDepth" ) ).intValue() );
		assertEquals( "/root", config.get( "agentPath" ) );
	}

	// ==================== SKILL TESTS ====================

	@Test
	@DisplayName( "It can create an agent with always-on skills" )
	public void testAgentWithSkills() {
		runtime.executeSource(
		    """
		    skill1 = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
		    skill2 = aiSkill( name: "code-style", description: "Code style guide", content: "Prefer tabs over spaces." )

		    agent      = aiAgent( name: "SkillAgent", skills: [ skill1, skill2 ] )
		    skillCount = agent.getSkills().len()
		    """,
		    context
		);

		assertEquals( 2L, ( long ) ( ( Number ) variables.get( Key.of( "skillCount" ) ) ).intValue() );
	}

	@Test
	@DisplayName( "It can create an agent with lazy available-skills" )
	public void testAgentWithAvailableSkills() {
		runtime.executeSource(
		    """
		    skill1 = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
		    skill2 = aiSkill( name: "code-style", description: "Code style guide", content: "Prefer tabs over spaces." )

		    agent      = aiAgent( name: "SkillAgent", availableSkills: [ skill1, skill2 ] )
		    availCount = agent.getAvailableSkills().len()
		    """,
		    context
		);

		// 2 provided + any global skills auto-discovered (none in test env)
		assertTrue( ( ( Number ) variables.get( Key.of( "availCount" ) ) ).intValue() >= 2 );
	}

	@Test
	@DisplayName( "It can add a skill using the fluent addSkill() API" )
	public void testAgentAddSkillFluent() {
		runtime.executeSource(
		    """
		    skill = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
		    agent = aiAgent( name: "FluentSkillAgent" ).addSkill( skill )
		    skillCount = agent.getSkills().len()
		    """,
		    context
		);

		assertEquals( 1L, ( long ) ( ( Number ) variables.get( Key.of( "skillCount" ) ) ).intValue() );
	}

	@Test
	@DisplayName( "It can add a lazy skill using the fluent addAvailableSkill() API" )
	public void testAgentAddAvailableSkillFluent() {
		runtime.executeSource(
		    """
		    skill = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
		    agent = aiAgent( name: "FluentSkillAgent" ).addAvailableSkill( skill )
		    availCount = agent.getAvailableSkills().len()
		    """,
		    context
		);

		assertTrue( ( ( Number ) variables.get( Key.of( "availCount" ) ) ).intValue() >= 1 );
	}

	@Test
	@DisplayName( "buildSkillsContent() returns non-empty string when always-on skills are set" )
	public void testAgentBuildSkillsContent() {
		runtime.executeSource(
		    """
		    skill  = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
		    agent  = aiAgent( name: "SkillAgent", skills: [ skill ] )

		    content       = agent.buildSkillsContent()
		    hasHeader     = content.findNoCase( "## Skills" ) > 0
		    hasSkillBlock = content.findNoCase( "#### Skill: sql-tips" ) > 0
		    hasContent    = content.findNoCase( "Always use indexed columns." ) > 0
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "hasHeader" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "hasSkillBlock" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "hasContent" ) ) );
	}

	@Test
	@DisplayName( "buildSkillsContent() includes available-skills index section" )
	public void testAgentBuildSkillsContentWithAvailableSkills() {
		runtime.executeSource(
		    """
		    skill  = aiSkill( name: "lazy-skill", description: "Use only when needed", content: "Be careful." )
		    agent  = aiAgent( name: "LazySkillAgent", availableSkills: [ skill ] )

		    content        = agent.buildSkillsContent()
		    hasAvailHeader = content.findNoCase( "## Available Skills" ) > 0
		    hasIndex       = content.findNoCase( "- lazy-skill:" ) > 0
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "hasAvailHeader" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "hasIndex" ) ) );
	}

	@Test
	@DisplayName( "activateSkill() promotes a lazy skill to always-on pool" )
	public void testAgentActivateSkill() {
		runtime.executeSource(
		    """
		    skill = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
		    agent = aiAgent( name: "ActivateSkillAgent", availableSkills: [ skill ] )

		    beforeAvail  = agent.getAvailableSkills().filter( s => s.getName() == "sql-tips" ).len()
		    beforeActive = agent.getSkills().filter( s => s.getName() == "sql-tips" ).len()

		    agent.activateSkill( "sql-tips" )

		    afterAvail  = agent.getAvailableSkills().filter( s => s.getName() == "sql-tips" ).len()
		    afterActive = agent.getSkills().filter( s => s.getName() == "sql-tips" ).len()
		    """,
		    context
		);

		assertEquals( 1L, ( long ) ( ( Number ) variables.get( Key.of( "beforeAvail" ) ) ).intValue() );
		assertEquals( 0L, ( long ) ( ( Number ) variables.get( Key.of( "beforeActive" ) ) ).intValue() );
		assertEquals( 0L, ( long ) ( ( Number ) variables.get( Key.of( "afterAvail" ) ) ).intValue() );
		assertEquals( 1L, ( long ) ( ( Number ) variables.get( Key.of( "afterActive" ) ) ).intValue() );
	}

	@Test
	@DisplayName( "loadSkill tool is auto-registered when available skills are set" )
	public void testAgentLoadSkillToolRegistered() {
		runtime.executeSource(
		    """
		    skill       = aiSkill( name: "sql-tips", description: "SQL optimisation rules", content: "Always use indexed columns." )
		    agent       = aiAgent( name: "ToolRegAgent", availableSkills: [ skill ] )
		    hasLoadTool = agent.hasTool( "loadSkill" )
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "hasLoadTool" ) ) );
	}

	@Test
	@DisplayName( "loadSkill tool is NOT registered when no available skills are set" )
	public void testAgentLoadSkillToolNotRegisteredWithoutSkills() {
		runtime.executeSource(
		    """
		    agent       = aiAgent( name: "NoSkillAgent" )
		    hasLoadTool = agent.hasTool( "loadSkill" )
		    """,
		    context
		);

		assertFalse( variables.getAsBoolean( Key.of( "hasLoadTool" ) ) );
	}

	// ==================== ASYNC TESTS ====================

	@Test
	@DisplayName( "runAsync() on an agent returns a non-null BoxFuture" )
	public void testAgentRunAsyncReturnsFuture() {
		runtime.executeSource(
		    """
		    agent  = aiAgent( name: "AsyncAgent", description: "Async test agent" )
		    future = agent.runAsync( "Hello" )
		    isFuture = !isNull( future )
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "isFuture" ) ) );
	}

	@Test
	@DisplayName( "runAsync() on an AiModel returns a non-null BoxFuture" )
	public void testModelRunAsyncReturnsFuture() {
		runtime.executeSource(
		    """
		    model    = aiModel()
		    future   = model.runAsync( "Hello" )
		    isFuture = !isNull( future )
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "isFuture" ) ) );
	}

	@Test
	@DisplayName( "aiParallel() BIF creates an AiRunnableParallel instance" )
	public void testAiParallelBIFCreatesRunnableParallel() {
		runtime.executeSource(
		    """
		    agentA   = aiAgent( name: "AgentA", description: "First agent" )
		    agentB   = aiAgent( name: "AgentB", description: "Second agent" )
		    parallel = aiParallel({ a: agentA, b: agentB })
		    isParallel = parallel instanceof "bxModules.bxai.models.runnables.AiRunnableParallel"
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "isParallel" ) ) );
	}

	@Test
	@DisplayName( "AiRunnableParallel.run() returns a struct with all named keys" )
	public void testAiRunnableParallelRunReturnsMergedStruct() {
		runtime.executeSource(
		    """
		    runnableA = aiTransform( input => input & "-a" )
		    runnableB = aiTransform( input => input & "-b" )
		    parallel  = aiParallel({ a: runnableA, b: runnableB })
		    result    = parallel.run( "input" )
		    hasKeys   = isStruct( result ) && result.keyExists( "a" ) && result.keyExists( "b" )
		    hasValues = result.a == "input-a" && result.b == "input-b"
		    """,
		    context
		);

		assertTrue( variables.getAsBoolean( Key.of( "hasKeys" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "hasValues" ) ) );
	}

	@Test
	@DisplayName( "AiRunnableParallel.add() adds a named runnable and returns itself for chaining" )
	public void testAiRunnableParallelAdd() {
		runtime.executeSource(
		    """
		    agentA   = aiAgent( name: "AgentA", description: "First agent" )
		    agentB   = aiAgent( name: "AgentB", description: "Second agent" )
		    parallel = aiParallel({ a: agentA }).add( "b", agentB )
		    keyCount = parallel.getRunnables().count()
		    """,
		    context
		);

		assertEquals( 2L, ( long ) ( ( Number ) variables.get( Key.of( "keyCount" ) ) ).intValue() );
	}

	@Test
	@DisplayName( "AiRunnableParallel.getName() returns a descriptive string with runnable keys" )
	public void testAiRunnableParallelGetName() {
		runtime.executeSource(
		    """
		    agentA   = aiAgent( name: "AgentA", description: "First agent" )
		    parallel = aiParallel({ summary: agentA })
		    name     = parallel.getName()
		    """,
		    context
		);

		var name = ( String ) variables.get( Key.of( "name" ) );
		assertTrue( name.contains( "summary" ) );
	}

	@Test
	@DisplayName( "AiRunnableParallel.stream() throws AiRunnableParallel.NotSupported" )
	public void testAiRunnableParallelStreamThrows() {
		assertThrows( Exception.class, () -> {
			runtime.executeSource(
			    """
			    agentA   = aiAgent( name: "AgentA", description: "First agent" )
			    parallel = aiParallel({ a: agentA })
			    parallel.stream( chunk => writeOutput( chunk ), "input" )
			    """,
			    context
			);
		} );
	}

}
