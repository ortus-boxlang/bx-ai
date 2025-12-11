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
package ortus.boxlang.ai.transformers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Test suite for JSONExtractorTransformer
 */
public class JSONExtractorTransformerTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Extract JSON from markdown code block" )
	public void testExtractFromMarkdown() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer();
		    input = '```json
		    {"name": "John", "age": 30}
		    ```';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( "name" ) ).isEqualTo( "John" );
		assertThat( result.get( "age" ) ).isEqualTo( 30 );
	}

	@Test
	@DisplayName( "Extract JSON from markdown without language specifier" )
	public void testExtractFromMarkdownNoLang() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer();
		    input = '```
		    {"name": "Jane", "active": true}
		    ```';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( "name" ) ).isEqualTo( "Jane" );
		assertThat( result.get( "active" ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "Extract JSON from mixed text" )
	public void testExtractFromMixedText() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer();
		    input = 'Here is the result: {"status": "success", "count": 42} Hope this helps!';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( "status" ) ).isEqualTo( "success" );
		assertThat( result.get( "count" ) ).isEqualTo( 42 );
	}

	@Test
	@DisplayName( "Extract JSON array from text" )
	public void testExtractJSONArray() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer();
		    input = 'The items are: [{"id": 1}, {"id": 2}, {"id": 3}]';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsArray( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 3 );
		assertThat( ( ( IStruct ) result.get( 0 ) ).get( "id" ) ).isEqualTo( 1 );
		assertThat( ( ( IStruct ) result.get( 1 ) ).get( "id" ) ).isEqualTo( 2 );
		assertThat( ( ( IStruct ) result.get( 2 ) ).get( "id" ) ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( "Handle invalid JSON in non-strict mode" )
	public void testInvalidJSONNonStrict() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer();
		    input = 'This is not JSON at all';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.isEmpty() ).isTrue();
	}

	@Test
	@DisplayName( "Extract specific path from JSON" )
	public void testExtractPath() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer({
		        extractPath: "user.name"
		    });
		    input = '{"user": {"name": "Alice", "age": 25}}';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "Alice" );
	}

	@Test
	@DisplayName( "Extract nested path with array index" )
	public void testExtractPathWithArrayIndex() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer({
		        extractPath: "items.0.title"
		    });
		    input = '{"items": [{"title": "First"}, {"title": "Second"}]}';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "First" );
	}

	@Test
	@DisplayName( "Return raw JSON string" )
	public void testReturnRaw() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer({
		        returnRaw: true
		    });
		    input = '```json
		    {"test": "value"}
		    ```';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "{\"test\": \"value\"}" );
	}

	@Test
	@DisplayName( "Transform array of text with JSON" )
	public void testTransformArray() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer();
		    input = [
		        '{"name": "Alice"}',
		        '```json
		        {"name": "Bob"}
		        ```',
		        'Result: {"name": "Charlie"}'
		    ];
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsArray( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 3 );
		assertThat( ( ( IStruct ) result.get( 0 ) ).get( "name" ) ).isEqualTo( "Alice" );
		assertThat( ( ( IStruct ) result.get( 1 ) ).get( "name" ) ).isEqualTo( "Bob" );
		assertThat( ( ( IStruct ) result.get( 2 ) ).get( "name" ) ).isEqualTo( "Charlie" );
	}

	@Test
	@DisplayName( "Configure extraction options" )
	public void testConfigure() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer()
		        .configure({ stripMarkdown: false });

		    input = '```json
		    {"value": 123}
		    ```';

		    // With stripMarkdown: false, markdown blocks remain but JSON can still be extracted
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( "value" ) ).isEqualTo( 123 );
	}

	@Test
	@DisplayName( "Extract nested JSON structure" )
	public void testNestedJSON() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer();
		    input = '{
		        "data": {
		            "items": [
		                {"id": 1, "meta": {"active": true}},
		                {"id": 2, "meta": {"active": false}}
		            ]
		        }
		    }';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var	result	= variables.getAsStruct( Key.of( "result" ) );
		var	data	= ( ( IStruct ) result.get( "data" ) );
		var	items	= ( ( Array ) data.get( "items" ) );
		assertThat( items.size() ).isEqualTo( 2 );
		assertThat( ( ( IStruct ) items.get( 0 ) ).get( "id" ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Extract from response with backticks around object" )
	public void testBackticksAroundObject() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer();
		    input = '`{"key": "value"}`';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( "key" ) ).isEqualTo( "value" );
	}

	@Test
	@DisplayName( "Validate required fields in schema" )
	public void testSchemaValidation() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    schema = {
		        "type": "object",
		        "required": ["name", "email"]
		    };

		    transformer = new JSONExtractorTransformer({
		        validateSchema: true,
		        schema: schema
		    });

		    input = '{"name": "John", "email": "john@example.com"}';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( "name" ) ).isEqualTo( "John" );
		assertThat( result.get( "email" ) ).isEqualTo( "john@example.com" );
	}

	@Test
	@DisplayName( "Handle complex markdown with explanation" )
	public void testComplexMarkdownResponse() {
		runtime.executeSource(
		    """
		    import bxModules.bxai.models.transformers.JSONExtractorTransformer;

		    transformer = new JSONExtractorTransformer();
		    input = 'Here is the analysis:

		    ```json
		    {
		        "sentiment": "positive",
		        "score": 0.85,
		        "keywords": ["great", "excellent", "amazing"]
		    }
		    ```

		    This indicates a very positive sentiment.';
		    result = transformer.transform(input);
		    """,
		    context
		);

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( "sentiment" ) ).isEqualTo( "positive" );
		assertThat( ( ( Number ) result.get( "score" ) ).doubleValue() ).isEqualTo( 0.85 );
		var keywords = ( ( Array ) result.get( "keywords" ) );
		assertThat( keywords.size() ).isEqualTo( 3 );
		assertThat( keywords.get( 0 ) ).isEqualTo( "great" );
	}

}
