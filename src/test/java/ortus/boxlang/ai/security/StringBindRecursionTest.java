package ortus.boxlang.ai.security;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Documents the behavior of the core stringBind() BIF with respect to placeholder
 * recursion, which is the basis for the AiMessage binding-escape hardening.
 *
 * Finding: stringBind() does NOT recurse — a ${...} sequence that appears inside a bound
 * VALUE is left literal, not re-substituted. So the binding-escape in AiMessage is
 * defense-in-depth hardening (preventing a literal ${x} in untrusted data from confusing
 * a human reader or a downstream single-pass template), not a fix for an active
 * re-interpolation vulnerability.
 */
@DisplayName( "stringBind Recursion Behavior (baseline for binding-escape hardening)" )
public class StringBindRecursionTest extends BaseIntegrationTest {

	@DisplayName( "stringBind does not re-substitute ${} contained in a bound value" )
	@Test
	public void testStringBindDoesNotRecurse() {
		// @formatter:off
		runtime.executeSource(
		    """
		        bindings = { name: "World", evil: "${name}" };
		        out = stringBind( "Hello ${evil}", bindings );
		        // If it recursed, out would contain "World"; it does not
		        recurses = out.contains( "World" );
		        literalKept = out.contains( "$" & "{name}" );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "recurses" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "literalKept" ) ) ).isTrue();
	}

	@DisplayName( "AiMessage binding-escape prevents a literal ${} from a value surviving into output" )
	@Test
	public void testBindingEscapeNeutralizesValue() {
		// @formatter:off
		runtime.executeSource(
		    """
		        // Default-on escaping turns ${y} in the VALUE into $ {y} so it can never be
		        // mistaken for a placeholder by any downstream single-pass renderer.
		        msg = aiMessage().user( "value is ${x}" ).bind( { x: "${y}" } );
		        rendered = msg.render();
		        content = rendered[ 1 ].content;
		        escaped = !content.contains( "$" & "{y}" ) && content.contains( "$ {y}" );
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "escaped" ) ) ).isTrue();
	}

}
