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
package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Tests for aiPopulate() BIF
 */
public class aiPopulateTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Can populate a class instance from JSON string" )
	public void testPopulateClassFromJSON() {
		// @formatter:off
		runtime.executeSource(
			"""
			jsonData = '{"firstName":"John","lastName":"Doe","age":30}';
			person = aiPopulate( new src.test.bx.Person(), jsonData );

			firstName = person.getFirstName();
			lastName = person.getLastName();
			age = person.getAge();
			isPerson = isInstanceOf( person, "Person" );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "firstName" ) ).toString() ).isEqualTo( "John" );
		assertThat( variables.get( Key.of( "lastName" ) ).toString() ).isEqualTo( "Doe" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 30 );
		assertThat( variables.getAsBoolean( Key.of( "isPerson" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Can populate a class instance from struct" )
	public void testPopulateClassFromStruct() {
		// @formatter:off
		runtime.executeSource(
			"""
			data = { firstName: "Jane", lastName: "Smith", age: 25 };
			person = aiPopulate( new src.test.bx.Person(), data );

			firstName = person.getFirstName();
			lastName = person.getLastName();
			age = person.getAge();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "firstName" ) ).toString() ).isEqualTo( "Jane" );
		assertThat( variables.get( Key.of( "lastName" ) ).toString() ).isEqualTo( "Smith" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 25 );
	}

	@Test
	@DisplayName( "Can populate array of class instances from JSON" )
	public void testPopulateArrayFromJSON() {
		// @formatter:off
		runtime.executeSource(
			"""
			jsonData = '[{"firstName":"John","lastName":"Doe","age":30},{"firstName":"Jane","lastName":"Smith","age":25}]';
			people = aiPopulate( [new src.test.bx.Person()], jsonData );

			count = people.len();
			firstName1 = people[1].getFirstName();
			firstName2 = people[2].getFirstName();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "count" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "firstName1" ) ).toString() ).isEqualTo( "John" );
		assertThat( variables.get( Key.of( "firstName2" ) ).toString() ).isEqualTo( "Jane" );
	}

	@Test
	@DisplayName( "Can populate array of class instances from array of structs" )
	public void testPopulateArrayFromStructArray() {
		// @formatter:off
		runtime.executeSource(
			"""
			data = [
				{ firstName: "Alice", lastName: "Brown", age: 28 },
				{ firstName: "Bob", lastName: "Johnson", age: 35 }
			];
			people = aiPopulate( [new src.test.bx.Person()], data );

			count = people.len();
			firstName1 = people[1].getFirstName();
			age2 = people[2].getAge();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "count" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "firstName1" ) ).toString() ).isEqualTo( "Alice" );
		assertThat( variables.get( Key.of( "age2" ) ) ).isEqualTo( 35 );
	}

	@Test
	@DisplayName( "Can populate struct from JSON" )
	public void testPopulateStructFromJSON() {
		// @formatter:off
		runtime.executeSource(
			"""
			jsonData = '{"name":"John","email":"john@example.com","active":true}';
			result = aiPopulate( { name: "", email: "", active: false }, jsonData );

			name = result.name;
			email = result.email;
			active = result.active;
			isStruct = isStruct( result );
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "name" ) ).toString() ).isEqualTo( "John" );
		assertThat( variables.get( Key.of( "email" ) ).toString() ).isEqualTo( "john@example.com" );
		assertThat( variables.getAsBoolean( Key.of( "active" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isStruct" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Works with class inheritance" )
	public void testPopulateInheritedClass() {
		// @formatter:off
		runtime.executeSource(
			"""
			jsonData = '{"firstName":"Alice","lastName":"Johnson","employeeId":"EMP001","department":"Engineering"}';
			employee = aiPopulate( new src.test.bx.Employee(), jsonData );

			firstName = employee.getFirstName();
			employeeId = employee.getEmployeeId();
			department = employee.getDepartment();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.get( Key.of( "firstName" ) ).toString() ).isEqualTo( "Alice" );
		assertThat( variables.get( Key.of( "employeeId" ) ).toString() ).isEqualTo( "EMP001" );
		assertThat( variables.get( Key.of( "department" ) ).toString() ).isEqualTo( "Engineering" );
	}

	@Test
	@DisplayName( "Throws error for invalid JSON" )
	public void testInvalidJSON() {
		// @formatter:off
		var result = runtime.executeSource(
			"""
			try {
				aiPopulate( new src.test.bx.Person(), '{invalid json}' );
				errorThrown = false;
			} catch( any e ) {
				errorThrown = true;
				errorType = e.type;
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "errorThrown" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "errorType" ) ).toString() ).isEqualTo( "InvalidArgument" );
	}

	@Test
	@DisplayName( "Throws error for invalid target type" )
	public void testInvalidTargetType() {
		// @formatter:off
		runtime.executeSource(
			"""
			try {
				aiPopulate( "invalid target", '{"name":"John"}' );
				errorThrown = false;
			} catch( any e ) {
				errorThrown = true;
				errorType = e.type;
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "errorThrown" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "errorType" ) ).toString() ).isEqualTo( "InvalidArgument" );
	}

	@Test
	@DisplayName( "Throws error when array target has wrong element count" )
	public void testInvalidArrayTarget() {
		// @formatter:off
		runtime.executeSource(
			"""
			try {
				// Empty array - should fail
				aiPopulate( [], '[{"name":"John"}]' );
				errorThrown = false;
			} catch( any e ) {
				errorThrown = true;
				errorType = e.type;
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "errorThrown" ) ) ).isTrue();
		assertThat( variables.get( Key.of( "errorType" ) ).toString() ).isEqualTo( "InvalidArgument" );
	}

}
