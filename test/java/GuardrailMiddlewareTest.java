package test.java;

import static org.junit.Assert.*;
import org.junit.Test;
import bx.middleware.builtin.GuardrailMiddleware;
import bx.middleware.AiMiddlewareResult;
import java.util.*;

public class GuardrailMiddlewareTest extends BaseMiddlewareTest {

	private Map<String, Object> ctx( String toolName, Map<String, Object> args ) {
		Map<String, Object> ctx = new HashMap<>();
		ctx.put( "toolName", toolName );
		ctx.put( "toolArgs", args != null ? args : new HashMap<>() );
		return ctx;
	}

	@Test
	public void testBlockedToolIsRejected() {
		GuardrailMiddleware mw = new GuardrailMiddleware(
			List.of( "dropTable", "deleteAll" ),
			new HashMap<>(),
			new HashMap<>()
		);

		assertTrue( mw.beforeToolCall( ctx( "dropTable", null ) ).isRejected() );
		assertTrue( mw.beforeToolCall( ctx( "deleteAll", null ) ).isRejected() );
	}

	@Test
	public void testBlockedToolIsCaseInsensitive() {
		GuardrailMiddleware mw = new GuardrailMiddleware(
			List.of( "dropTable" ),
			new HashMap<>(),
			new HashMap<>()
		);

		assertTrue( mw.beforeToolCall( ctx( "DROPTABLE", null ) ).isRejected() );
		assertTrue( mw.beforeToolCall( ctx( "DropTable", null ) ).isRejected() );
	}

	@Test
	public void testAllowedToolContinues() {
		GuardrailMiddleware mw = new GuardrailMiddleware(
			List.of( "dropTable" ),
			new HashMap<>(),
			new HashMap<>()
		);

		assertTrue( mw.beforeToolCall( ctx( "readRecord", null ) ).isContinue() );
	}

	@Test
	public void testBlockedArgIsRejectedWhenPresent() {
		Map<String, List<String>> blockedArgs = new HashMap<>();
		blockedArgs.put( "executeSQL", List.of( "rawQuery" ) );

		GuardrailMiddleware mw = new GuardrailMiddleware(
			new ArrayList<>(),
			blockedArgs,
			new HashMap<>()
		);

		Map<String, Object> args = new HashMap<>();
		args.put( "rawQuery", "SELECT *" );
		assertTrue( mw.beforeToolCall( ctx( "executeSQL", args ) ).isRejected() );
	}

	@Test
	public void testBlockedArgAllowedWhenAbsent() {
		Map<String, List<String>> blockedArgs = new HashMap<>();
		blockedArgs.put( "executeSQL", List.of( "rawQuery" ) );

		GuardrailMiddleware mw = new GuardrailMiddleware(
			new ArrayList<>(),
			blockedArgs,
			new HashMap<>()
		);

		Map<String, Object> args = new HashMap<>();
		args.put( "query", "SELECT *" );  // different key — not blocked
		assertTrue( mw.beforeToolCall( ctx( "executeSQL", args ) ).isContinue() );
	}

	@Test
	public void testArgPatternMatchRejects() {
		Map<String, Map<String, String>> argPatterns = new HashMap<>();
		Map<String, String> sqlPatterns = new HashMap<>();
		sqlPatterns.put( "query", "(?i)DROP|TRUNCATE|DELETE FROM" );
		argPatterns.put( "executeSQL", sqlPatterns );

		GuardrailMiddleware mw = new GuardrailMiddleware(
			new ArrayList<>(),
			new HashMap<>(),
			argPatterns
		);

		Map<String, Object> args = new HashMap<>();
		args.put( "query", "DROP TABLE users" );
		assertTrue( mw.beforeToolCall( ctx( "executeSQL", args ) ).isRejected() );
	}

	@Test
	public void testArgPatternNoMatchContinues() {
		Map<String, Map<String, String>> argPatterns = new HashMap<>();
		Map<String, String> sqlPatterns = new HashMap<>();
		sqlPatterns.put( "query", "(?i)DROP|TRUNCATE|DELETE FROM" );
		argPatterns.put( "executeSQL", sqlPatterns );

		GuardrailMiddleware mw = new GuardrailMiddleware(
			new ArrayList<>(),
			new HashMap<>(),
			argPatterns
		);

		Map<String, Object> args = new HashMap<>();
		args.put( "query", "SELECT * FROM users" );
		assertTrue( mw.beforeToolCall( ctx( "executeSQL", args ) ).isContinue() );
	}

	@Test
	public void testEvaluationOrderBlockedToolBeforePattern() {
		// Tool is both blocked AND matches a pattern — blocked tool check fires first
		Map<String, Map<String, String>> argPatterns = new HashMap<>();
		Map<String, String> patterns = new HashMap<>();
		patterns.put( "query", ".*" );
		argPatterns.put( "dropTable", patterns );

		GuardrailMiddleware mw = new GuardrailMiddleware(
			List.of( "dropTable" ),
			new HashMap<>(),
			argPatterns
		);

		AiMiddlewareResult result = mw.beforeToolCall( ctx( "dropTable", null ) );
		assertTrue( result.isRejected() );
		assertTrue( result.getReason().contains( "blocked by guardrail policy" ) );
	}
}