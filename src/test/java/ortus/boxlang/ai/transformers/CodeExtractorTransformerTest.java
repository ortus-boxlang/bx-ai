package ortus.boxlang.ai.transformers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

@DisplayName( "CodeExtractorTransformer Tests" )
public class CodeExtractorTransformerTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Test extract JavaScript code from markdown" )
	public void testExtractJavaScript() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "Here's a function:

```javascript
function hello() {
    console.log('Hello World');
}
```

Use it like that.";

			result = new CodeExtractorTransformer()
				.configure({ language: "javascript" })
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "function hello()" );
		assertThat( result ).contains( "console.log" );
		assertThat( result ).doesNotContain( "```" );
	}

	@Test
	@DisplayName( "Test extract Python code" )
	public void testExtractPython() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "Here's how to do it in Python:

```python
def greet(name):
    return f'Hello, {name}!'
```
";

			result = new CodeExtractorTransformer()
				.configure({ language: "python" })
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "def greet(name)" );
		assertThat( result ).contains( "return f'Hello" );
	}

	@Test
	@DisplayName( "Test extract all code blocks" )
	public void testExtractAllLanguages() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "Python example:
```python
print('hello')
```

JavaScript example:
```javascript
console.log('hello');
```
";

			result = new CodeExtractorTransformer()
				.configure({
					language: "all",
					multiple: true
				})
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = ( Array ) variables.get( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.get( 0 ).toString() ).contains( "print" );
		assertThat( result.get( 1 ).toString() ).contains( "console.log" );
	}

	@Test
	@DisplayName( "Test extract with metadata" )
	public void testExtractWithMetadata() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "```javascript
const x = 42;
```";

			result = new CodeExtractorTransformer()
				.configure({
					language: "javascript",
					returnMetadata: true
				})
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.getAsString( Key.of( "language" ) ) ).isEqualTo( "javascript" );
		assertThat( result.getAsString( Key.of( "code" ) ) ).contains( "const x = 42" );
	}

	@Test
	@DisplayName( "Test extract multiple blocks of same language" )
	public void testExtractMultipleSameLanguage() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "Function 1:
```javascript
function foo() { return 1; }
```

Function 2:
```javascript
function bar() { return 2; }
```
";

			result = new CodeExtractorTransformer()
				.configure({
					language: "javascript",
					multiple: true
				})
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = ( Array ) variables.get( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.get( 0 ).toString() ).contains( "foo()" );
		assertThat( result.get( 1 ).toString() ).contains( "bar()" );
	}

	@Test
	@DisplayName( "Test extract SQL code" )
	public void testExtractSQL() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "Here's your query:

```sql
SELECT * FROM users
WHERE status = 'active'
ORDER BY created_at DESC;
```
";

			result = new CodeExtractorTransformer()
				.configure({ language: "sql" })
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "SELECT * FROM users" );
		assertThat( result ).contains( "WHERE status = 'active'" );
	}

	@Test
	@DisplayName( "Test extract without language identifier" )
	public void testExtractNoLanguage() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "Here's the code:

```
function test() {
    return true;
}
```
";

			result = new CodeExtractorTransformer()
				.configure({ language: "all" })
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "function test()" );
	}

	@Test
	@DisplayName( "Test strip comments from JavaScript" )
	public void testStripComments() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "```javascript
// This is a comment
function foo() {
    /* Multi-line
       comment */
    return 42;
}
```";

			result = new CodeExtractorTransformer()
				.configure({
					language: "javascript",
					stripComments: true
				})
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).doesNotContain( "This is a comment" );
		assertThat( result ).doesNotContain( "Multi-line" );
		assertThat( result ).contains( "function foo()" );
		assertThat( result ).contains( "return 42" );
	}

	@Test
	@DisplayName( "Test no code found with non-strict mode" )
	public void testNoCodeNonStrict() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			result = new CodeExtractorTransformer()
				.configure({ language: "javascript" })
				.transform("No code here at all");
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEmpty();
	}

	@Test
	@DisplayName( "Test no code found with strict mode" )
	public void testNoCodeStrictMode() {
		// @formatter:off
		assertThrows( Exception.class, () -> {
			runtime.executeSource(
				"""
				import bxModules.bxai.models.transformers.CodeExtractorTransformer;

				new CodeExtractorTransformer()
					.configure({
						language: "javascript",
						strictMode: true
					})
					.transform("No code here");
				""",
				context
			);
		} );
		// @formatter:on
	}

	@Test
	@DisplayName( "Test transform array of texts" )
	public void testTransformArray() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			texts = [
				"```javascript\nconst a = 1;\n```",
				"```javascript\nconst b = 2;\n```"
			];

			result = new CodeExtractorTransformer()
				.configure({ language: "javascript" })
				.transform(texts);
			""",
			context
		);
		// @formatter:on

		var result = ( Array ) variables.get( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.get( 0 ).toString() ).contains( "const a = 1" );
		assertThat( result.get( 1 ).toString() ).contains( "const b = 2" );
	}

	@Test
	@DisplayName( "Test configure method" )
	public void testConfigure() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			transformer = new CodeExtractorTransformer();
			transformer.configure({
				language: "python",
				trim: true,
				stripMarkdown: true
			});

			result = transformer.transform("```python\nprint('test')\n```");
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "print('test')" );
	}

	@Test
	@DisplayName( "Test extract BoxLang code" )
	public void testExtractBoxLang() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "Here's BoxLang:

```boxlang
function calculate(x, y) {
    return x + y;
}
```
";

			result = new CodeExtractorTransformer()
				.configure( { language: "boxlang" } )
				.transform( text );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "function calculate" );
		assertThat( result ).contains( "return x + y" );
	}

	@Test
	@DisplayName( "Test extract with all metadata multiple blocks" )
	public void testMultipleBlocksWithMetadata() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "```python
print('first')
```

```javascript
console.log('second');
```
";

			result = new CodeExtractorTransformer()
				.configure({
					language: "all",
					multiple: true,
					returnMetadata: true
				})
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = ( Array ) variables.get( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 2 );

		var first = ( IStruct ) result.get( 0 );
		assertThat( first.getAsString( Key.of( "language" ) ) ).isEqualTo( "python" );
		assertThat( first.getAsString( Key.of( "code" ) ) ).contains( "print" );

		var second = ( IStruct ) result.get( 1 );
		assertThat( second.getAsString( Key.of( "language" ) ) ).isEqualTo( "javascript" );
		assertThat( second.getAsString( Key.of( "code" ) ) ).contains( "console.log" );
	}

	@Test
	@DisplayName( "Test code block without closing backticks" )
	public void testNoClosingBackticks() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "```javascript
function incomplete() {
    return 'no closing backticks';
}";

			result = new CodeExtractorTransformer()
				.configure({ language: "javascript" })
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "function incomplete()" );
		assertThat( result ).contains( "no closing backticks" );
	}

	@Test
	@DisplayName( "Test filter by specific language from mixed content" )
	public void testFilterSpecificLanguage() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "Python:
```python
def foo(): pass
```

JavaScript:
```javascript
function bar() {}
```

More Python:
```python
def baz(): pass
```
";

			result = new CodeExtractorTransformer()
				.configure({
					language: "python",
					multiple: true
				})
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = ( Array ) variables.get( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.get( 0 ).toString() ).contains( "def foo()" );
		assertThat( result.get( 1 ).toString() ).contains( "def baz()" );
		// Ensure JavaScript is not included
		result.forEach( code -> {
			assertThat( code.toString() ).doesNotContain( "function bar" );
		} );
	}

	@Test
	@DisplayName( "Test case insensitive language matching" )
	public void testCaseInsensitiveLanguage() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			text = "```JavaScript
console.log('mixed case');
```";

			result = new CodeExtractorTransformer()
				.configure({ language: "javascript" })
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "console.log" );
	}

	@Test
	@DisplayName( "Test strip Python comments" )
	public void testStripPythonComments() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.CodeExtractorTransformer;

			hashChar = char(35);
			text = "```python
" & hashChar & " This is a comment
def hello():
    " & hashChar & " Another comment
    return 'world'
```";

			result = new CodeExtractorTransformer()
				.configure({
					language: "python",
					stripComments: true
				})
				.transform(text);
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).doesNotContain( "This is a comment" );
		assertThat( result ).doesNotContain( "Another comment" );
		assertThat( result ).contains( "def hello()" );
		assertThat( result ).contains( "return 'world'" );
	}
}
