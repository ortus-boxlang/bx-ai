package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class aiTransformTest extends BaseIntegrationTest {

	@DisplayName( "Can create a basic transform runnable" )
	@Test
	public void testBasicTransform() {

		// @formatter:off
		runtime.executeSource(
		    """
				transformer = aiTransform( input => input.ucase() )
				result = transformer.run( "hello world" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "transformer" ) ).isNotNull();
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "HELLO WORLD" );
	}

	@DisplayName( "Can transform with complex logic" )
	@Test
	public void testComplexTransform() {

		// @formatter:off
		runtime.executeSource(
		    """
				transformer = aiTransform( input => {
					return input.listToArray().map( item => item.trim().ucase() ).toList( ", " )
				} )
				result = transformer.run( "hello, world, test" )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "transformer" ) ).isNotNull();
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "HELLO, WORLD, TEST" );
	}

	@DisplayName( "Can transform structs" )
	@Test
	public void testStructTransform() {

		// @formatter:off
		runtime.executeSource(
		    """
				transformer = aiTransform( input => {
					return {
						fullName: input.firstName & " " & input.lastName,
						age: input.age
					}
				} )
				result = transformer.run( { firstName: "John", lastName: "Doe", age: 30 } )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "transformer" ) ).isNotNull();
		IStruct result = ( IStruct ) variables.get( "result" );
		assertThat( result.getAsString( Key.of( "fullName" ) ) ).isEqualTo( "John Doe" );
		assertThat( result.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "Can transform arrays" )
	@Test
	public void testArrayTransform() {

		// @formatter:off
		runtime.executeSource(
		    """
				transformer = aiTransform( input => input.map( item => item * 2 ) )
				result = transformer.run( [ 1, 2, 3, 4, 5 ] )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "transformer" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 5 );
		assertThat( ( ( Number ) result.get( 0 ) ).intValue() ).isEqualTo( 2 );
		assertThat( ( ( Number ) result.get( 4 ) ).intValue() ).isEqualTo( 10 );
	}

	@DisplayName( "Can stream through a transform" )
	@Test
	public void testStreamTransform() {

		// @formatter:off
		runtime.executeSource(
		    """
				transformer = aiTransform( input => input.ucase() )
				result = ""
				transformer.stream(
					onChunk = ( chunk, metadata ) => {
						result = chunk
					},
					input = "hello world"
				)
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "transformer" ) ).isNotNull();
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "HELLO WORLD" );
	}

	@DisplayName( "Can chain transforms" )
	@Test
	public void testChainedTransforms() {

		// @formatter:off
		runtime.executeSource(
		    """
				transformer = aiTransform( input => input.trim() )
					.to( aiTransform( input => input.ucase() ) )
					.to( aiTransform( input => input.replace( " ", "_" ) ) )
				result = transformer.run( "  hello world  " )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "transformer" ) ).isNotNull();
		assertThat( ( String ) variables.get( "result" ) ).isEqualTo( "HELLO_WORLD" );
	}

	@DisplayName( "Can get transformer name" )
	@Test
	public void testTransformerName() {

		// @formatter:off
		runtime.executeSource(
		    """
				transformer = aiTransform( input => input.ucase() )
					.withName( "UppercaseTransform" )
				name = transformer.getName()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "transformer" ) ).isNotNull();
		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "UppercaseTransform" );
	}
}
