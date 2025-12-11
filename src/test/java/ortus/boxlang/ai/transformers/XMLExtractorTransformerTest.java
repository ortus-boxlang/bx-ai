package ortus.boxlang.ai.transformers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class XMLExtractorTransformerTest extends BaseIntegrationTest {

	@Test
	@DisplayName( "Test extract XML from markdown code block" )
	public void testExtractFromMarkdown() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			result = new XMLExtractorTransformer().transform( '```xml
			<person>
				<name>John Doe</name>
				<age>30</age>
			</person>
			```' );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test extract XML from markdown without language specifier" )
	public void testExtractFromMarkdownNoLang() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			result = new XMLExtractorTransformer().transform( '```
			<book>
				<title>Test Book</title>
			</book>
			```' );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test extract XML from mixed text" )
	public void testExtractFromMixedText() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			text = "Here's the data you requested: <data><value>42</value></data> Hope that helps!";
			result = new XMLExtractorTransformer().transform( text );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test handle invalid XML with non-strict mode" )
	public void testInvalidXMLNonStrict() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			result = new XMLExtractorTransformer().transform( "<invalid>Not closed properly" );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		// Should return empty struct in non-strict mode
		assertThat( result.isEmpty() ).isTrue();
	}

	@Test
	@DisplayName( "Test extract specific XPath query" )
	public void testExtractXPath() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			xml = "<person><name>John Doe</name><age>30</age></person>";
			result = new XMLExtractorTransformer()
				.configure({ xPath: "//name" })
				.transform( xml );
			""",
			context
		);
		// @formatter:on

		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test extract XPath with attribute" )
	public void testExtractXPathAttribute() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			xml = "<data><item id='1'>First</item><item id='2'>Second</item></data>";
			result = new XMLExtractorTransformer()
				.configure({ xPath: "//item/@id" })
				.transform( xml );
			""",
			context
		);
		// @formatter:on

		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test return raw XML string" )
	public void testReturnRaw() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			xml = "```xml
			<person>
				<name>John</name>
			</person>
			```";
			result = new XMLExtractorTransformer()
				.configure({ returnRaw: true })
				.transform( xml );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).contains( "<person>" );
		assertThat( result ).contains( "<name>John</name>" );
	}

	@Test
	@DisplayName( "Test transform array of text with XML" )
	public void testTransformArray() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			texts = [
				"<data><value>1</value></data>",
				"<data><value>2</value></data>"
			];
			result = new XMLExtractorTransformer().transform( texts );
			""",
			context
		);
		// @formatter:on

		var result = ( Array ) variables.get( Key.of( "result" ) );
		assertThat( result.size() ).isEqualTo( 2 );
		// Each element should be a parsed XML struct
		var first = ( IStruct ) result.get( 0 );
		assertThat( first.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test configure options" )
	public void testConfigure() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			transformer = new XMLExtractorTransformer();
			transformer.configure({
				stripMarkdown: false,
				strictMode: false,
				returnRaw: true
			});

			result = transformer.transform( "<test>Value</test>" );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "<test>Value</test>" );
	}

	@Test
	@DisplayName( "Test extract nested XML structure" )
	public void testNestedXML() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			xml = "<root>
				<parent>
					<child name='first'>
						<grandchild>Value1</grandchild>
					</child>
					<child name='second'>
						<grandchild>Value2</grandchild>
					</child>
				</parent>
			</root>";
			result = new XMLExtractorTransformer().transform( xml );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test XML with declaration" )
	public void testXMLDeclaration() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			xml = '<?xml version="1.0" encoding="UTF-8"?>
			<data>
				<item>Test</item>
			</data>';
			result = new XMLExtractorTransformer().transform( xml );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test XML with CDATA section" )
	public void testCDATA() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			xml = "<message>
				<content><![CDATA[<script>alert('test')</script>]]></content>
			</message>";
			result = new XMLExtractorTransformer().transform( xml );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test self-closing tags" )
	public void testSelfClosingTags() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			xml = "<document>
				<item id='1' value='test' />
				<item id='2' value='test2' />
			</document>";
			result = new XMLExtractorTransformer().transform( xml );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test complex markdown response with explanation" )
	public void testComplexMarkdownResponse() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			response = 'Here is the data you requested in XML format:

			```xml
			<users>
				<user id="1">
					<name>Alice</name>
					<email>alice@example.com</email>
				</user>
				<user id="2">
					<name>Bob</name>
					<email>bob@example.com</email>
				</user>
			</users>
			```

			This XML contains information about two users.';

			result = new XMLExtractorTransformer().transform( response );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "Test strict mode throws on no XML" )
	public void testStrictModeNoXML() {
		// @formatter:off
		assertThrows( Exception.class, () -> {
			runtime.executeSource(
				"""
				import bxModules.bxai.models.transformers.XMLExtractorTransformer;

				new XMLExtractorTransformer()
					.configure({ strictMode: true })
					.transform( "No XML here at all" );
				""",
				context
			);
		} );
		// @formatter:on
	}

	@Test
	@DisplayName( "Test backticks around XML" )
	public void testBackticksAroundXML() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.transformers.XMLExtractorTransformer;

			result = new XMLExtractorTransformer().transform( '`<data><value>test</value></data>`' );
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "XmlRoot" ) ) ).isNotNull();
	}
}
