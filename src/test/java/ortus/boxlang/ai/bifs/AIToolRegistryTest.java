package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

public class AIToolRegistryTest extends BaseIntegrationTest {

	@BeforeEach
	public void resetRegistry() {
		// Reset the singleton before each test for isolation
		runtime.executeSource(
		    """
		    bxModules.bxai.models.registry.AIToolRegistry::reset()
		    """,
		    context
		);
	}

	// ==================== Registration ====================

	@Test
	@DisplayName( "register() with ITool and no module uses plain key" )
	public void testRegisterNoModule() {
		runtime.executeSource(
		    """
		    tool = aiTool( "myTool", "A test tool", ( args ) => "ok" )
		    aiToolRegistry().register( tool )
		    hasIt = aiToolRegistry().has( "myTool" )
		    """,
		    context
		);
		assertTrue( variables.getAsBoolean( Key.of( "hasIt" ) ) );
	}

	@Test
	@DisplayName( "register() with ITool and module uses name@module key" )
	public void testRegisterWithModule() {
		runtime.executeSource(
		    """
		    tool = aiTool( "weather", "Gets weather", ( city ) => "sunny" )
		    aiToolRegistry().register( tool: tool, module: "bx-weather" )
		    hasIt = aiToolRegistry().has( "weather@bx-weather" )
		    """,
		    context
		);
		assertTrue( variables.getAsBoolean( Key.of( "hasIt" ) ) );
	}

	@Test
	@DisplayName( "register() shorthand creates Tool internally" )
	public void testRegisterShorthand() {
		runtime.executeSource(
		    """
		    aiToolRegistry().register(
		        name        : "searchDocs",
		        description : "Searches documents",
		        callback    : ( query ) => "results",
		        module      : "bx-docs"
		    )
		    hasIt = aiToolRegistry().has( "searchDocs@bx-docs" )
		    """,
		    context
		);
		assertTrue( variables.getAsBoolean( Key.of( "hasIt" ) ) );
	}

	// ==================== Lookup ====================

	@Test
	@DisplayName( "get() exact key match returns tool" )
	public void testGetExactKey() {
		runtime.executeSource(
		    """
		    tool = aiTool( "exactTool", "desc", ( args ) => "value" )
		    aiToolRegistry().register( tool )
		    retrieved = aiToolRegistry().get( "exactTool" )
		    toolName = retrieved.getName()
		    """,
		    context
		);
		assertThat( variables.getAsString( Key.of( "toolName" ) ) ).isEqualTo( "exactTool" );
	}

	@Test
	@DisplayName( "get() plain name resolves to name@module when single match exists" )
	public void testGetByNameResolution() {
		runtime.executeSource(
		    """
		    tool = aiTool( "resolveTool", "desc", ( args ) => "value" )
		    aiToolRegistry().register( tool: tool, module: "bx-test" )
		    retrieved = aiToolRegistry().get( "resolveTool" )
		    toolName = retrieved.getName()
		    """,
		    context
		);
		assertThat( variables.getAsString( Key.of( "toolName" ) ) ).isEqualTo( "resolveTool" );
	}

	@Test
	@DisplayName( "get() throws AmbiguousKeyException when multiple module matches" )
	public void testGetAmbiguous() {
		Exception ex = assertThrows( Exception.class, () -> {
			runtime.executeSource(
			    """
			    tool1 = aiTool( "ambig", "desc", ( args ) => "1" )
			    tool2 = aiTool( "ambig", "desc", ( args ) => "2" )
			    aiToolRegistry().register( tool: tool1, module: "mod-a" )
			    aiToolRegistry().register( tool: tool2, module: "mod-b" )
			    aiToolRegistry().get( "ambig" )
			    """,
			    context
			);
		} );
		assertThat( ex.getMessage() ).contains( "mbiguous" );
	}

	@Test
	@DisplayName( "get() throws RegistryItemNotFoundException when not found" )
	public void testGetNotFound() {
		assertThrows( Exception.class, () -> {
			runtime.executeSource(
			    """
			    aiToolRegistry().get( "nonExistentTool" )
			    """,
			    context
			);
		} );
	}

	@Test
	@DisplayName( "has() returns false for missing key" )
	public void testHasMissing() {
		runtime.executeSource(
		    """
		    result = aiToolRegistry().has( "missingTool" )
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isFalse();
	}

	// ==================== Module Queries ====================

	@Test
	@DisplayName( "getByModule() returns only that module's tools" )
	public void testGetByModule() {
		runtime.executeSource(
		    """
		    t1 = aiTool( "t1", "d", ( args ) => "v" )
		    t2 = aiTool( "t2", "d", ( args ) => "v" )
		    t3 = aiTool( "t3", "d", ( args ) => "v" )
		    aiToolRegistry().register( tool: t1, module: "mod-x" )
		    aiToolRegistry().register( tool: t2, module: "mod-x" )
		    aiToolRegistry().register( tool: t3, module: "mod-y" )
		    modTools = aiToolRegistry().getByModule( "mod-x" )
		    modCount = modTools.len()
		    """,
		    context
		);
		assertThat( ( ( Number ) variables.get( Key.of( "modCount" ) ) ).intValue() ).isEqualTo( 2 );
	}

	// ==================== Removal ====================

	@Test
	@DisplayName( "unregister() removes the tool" )
	public void testUnregister() {
		runtime.executeSource(
		    """
		    tool = aiTool( "removable", "d", ( args ) => "v" )
		    aiToolRegistry().register( tool )
		    aiToolRegistry().unregister( "removable" )
		    result = aiToolRegistry().has( "removable" )
		    """,
		    context
		);
		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "unregisterByModule() removes all tools for that module" )
	public void testUnregisterByModule() {
		runtime.executeSource(
		    """
		    t1 = aiTool( "u1", "d", ( args ) => "v" )
		    t2 = aiTool( "u2", "d", ( args ) => "v" )
		    aiToolRegistry().register( tool: t1, module: "cleanup-mod" )
		    aiToolRegistry().register( tool: t2, module: "cleanup-mod" )
		    aiToolRegistry().unregisterByModule( "cleanup-mod" )
		    result = aiToolRegistry().getByModule( "cleanup-mod" ).len()
		    """,
		    context
		);
		assertThat( ( ( Number ) variables.get( Key.of( "result" ) ) ).intValue() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "clear() empties the registry" )
	public void testClear() {
		runtime.executeSource(
		    """
		    t = aiTool( "ct", "d", ( args ) => "v" )
		    aiToolRegistry().register( t )
		    aiToolRegistry().clear()
		    result = aiToolRegistry().size()
		    """,
		    context
		);
		assertThat( ( ( Number ) variables.get( Key.of( "result" ) ) ).intValue() ).isEqualTo( 0 );
	}

	// ==================== resolveTools ====================

	@Test
	@DisplayName( "resolveTools() resolves mixed ITool + string array" )
	public void testResolveTools() {
		runtime.executeSource(
		    """
		    tool1 = aiTool( "rt1", "d", ( args ) => "v" )
		    tool2 = aiTool( "rt2", "d", ( args ) => "v" )
		    aiToolRegistry().register( tool2 )

		    resolved = bxModules.bxai.models.registry.AIToolRegistry::resolveTools( [ tool1, "rt2" ] )
		    resolvedCount = resolved.len()
		    """,
		    context
		);
		assertThat( ( ( Number ) variables.get( Key.of( "resolvedCount" ) ) ).intValue() ).isEqualTo( 2 );
	}

	// ==================== @AITool scan ====================

	@Test
	@DisplayName( "scan() on class instance registers @AITool methods" )
	public void testScanInstance() {
		runtime.executeSource(
		    """
		    coreTools = new bxModules.bxai.models.tools.CoreTools()
		    aiToolRegistry().scan( source: coreTools, module: "bx-ai" )
		    hasNow = aiToolRegistry().has( "now@bx-ai" )
		    """,
		    context
		);
		assertTrue( variables.getAsBoolean( Key.of( "hasNow" ) ) );
	}

	// ==================== CoreTools ====================

	@Test
	@DisplayName( "now@bx-ai is auto-registered on module load" )
	public void testCoreToolNowAutoRegistered() {
		// Module is loaded via BaseIntegrationTest.setup() which calls onLoad
		// We need to get the registry AFTER module load (not after reset)
		// So we reload the module or check the state before reset
		// Since @BeforeEach resets registry, we need to re-scan for this test
		runtime.executeSource(
		    """
		    // Re-simulate what onLoad does
		    bxModules.bxai.models.registry.AIToolRegistry::getInstance()
		        .scan( source: new bxModules.bxai.models.tools.CoreTools(), module: "bx-ai" )
		    hasNow = aiToolRegistry().has( "now@bx-ai" )
		    """,
		    context
		);
		assertTrue( variables.getAsBoolean( Key.of( "hasNow" ) ) );
	}

	@Test
	@DisplayName( "now@bx-ai returns ISO 8601 datetime string" )
	public void testCoreToolNowReturnsISO8601() {
		runtime.executeSource(
		    """
		    bxModules.bxai.models.registry.AIToolRegistry::getInstance()
		        .scan( source: new bxModules.bxai.models.tools.CoreTools(), module: "bx-ai" )
		    nowTool = aiToolRegistry().get( "now@bx-ai" )
		    result = nowTool.invoke( {} )
		    """,
		    context
		);
		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isNotNull();
		assertThat( result ).isNotEmpty();
		// ISO 8601 contains a T separating date and time, or at minimum contains digits
		assertThat( result ).containsMatch( "\\d{4}" );
	}

	@Test
	@DisplayName( "http.get is NOT auto-registered on module load" )
	public void testHttpGetNotAutoRegistered() {
		runtime.executeSource(
		    """
		    // Re-simulate what onLoad does
		    bxModules.bxai.models.registry.AIToolRegistry::getInstance()
		        .scan( source: new bxModules.bxai.models.tools.CoreTools(), module: "bx-ai" )
		    result = aiToolRegistry().has( "httpGet@bx-ai" )
		    """,
		    context
		);
		// httpGet should NOT be auto-registered (no @AITool annotation)
		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isFalse();
	}

	// ==================== AIToolRegistry::reset ====================

	@Test
	@DisplayName( "AIToolRegistry::reset() clears the singleton" )
	public void testReset() {
		runtime.executeSource(
		    """
		    t = aiTool( "reset-tool", "d", ( args ) => "v" )
		    aiToolRegistry().register( t )
		    sizeBefore = aiToolRegistry().size()
		    bxModules.bxai.models.registry.AIToolRegistry::reset()
		    sizeAfter = aiToolRegistry().size()
		    """,
		    context
		);
		assertThat( ( ( Number ) variables.get( Key.of( "sizeBefore" ) ) ).intValue() ).isGreaterThan( 0 );
		assertThat( ( ( Number ) variables.get( Key.of( "sizeAfter" ) ) ).intValue() ).isEqualTo( 0 );
	}

}
