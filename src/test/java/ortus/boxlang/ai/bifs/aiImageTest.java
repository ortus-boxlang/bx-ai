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

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for the aiImage BIF.
 * Live provider tests require API keys from the .env file (OPENAI_API_KEY, GEMINI_API_KEY).
 */
public class aiImageTest extends BaseIntegrationTest {

	@BeforeEach
	public void beforeEach() {
		moduleRecord.settings.put( "apiKey", dotenv.get( "OPENAI_API_KEY", "" ) );
		moduleRecord.settings.put( "provider", "openai" );
	}

	@DisplayName( "aiImage BIF is registered in the function list" )
	@Test
	public void testAiImageBIFRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
			bifExists = getFunctionList().keyExists( "aiImage" )
			""",
			context
		);
		// @formatter:on

		var bifExists = variables.getAsBoolean( Key.of( "bifExists" ) );
		assertThat( bifExists ).isTrue();
	}

	@DisplayName( "generateImage@bxai tool is registered in the tool registry" )
	@Test
	public void testImageToolRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
			toolExists = aiToolRegistry().has( "generateImage@bxai" )
			""",
			context
		);
		// @formatter:on

		var toolExists = variables.getAsBoolean( Key.of( "toolExists" ) );
		assertThat( toolExists ).isTrue();
	}

	@DisplayName( "aiImage throws UnsupportedCapability when provider does not support image generation" )
	@Test
	public void testAiImageThrowsForUnsupportedProvider() {
		// @formatter:off
		runtime.executeSource(
			"""
			threw = false;
			try {
				aiImage(
					prompt  : "a cat",
					options : { provider: "voyage" }
				);
			} catch( any e ){
				threw     = true;
				errorType = e.type;
			}
			""",
			context
		);
		// @formatter:on

		var	threw		= variables.getAsBoolean( Key.of( "threw" ) );
		var	errorType	= variables.getAsString( Key.of( "errorType" ) );
		assertThat( threw ).isTrue();
		assertThat( errorType ).isEqualTo( "UnsupportedCapability" );
	}

	@DisplayName( "beforeAIImageGeneration event fires with correct imageRequest context" )
	@Test
	public void testBeforeAIImageGenerationEventFires() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedPrompt = "";
			capturedSize   = "";
			BoxRegisterInterceptor(
				function( data ) {
					capturedPrompt = data.imageRequest.getPrompt();
					capturedSize   = data.imageRequest.getSize();
				},
				"beforeAIImageGeneration"
			)
			aiImage(
				prompt  : "a sunset over the ocean",
				options : { size: "1024x1024" }
			)
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedPrompt" ) ) ).isEqualTo( "a sunset over the ocean" );
		assertThat( variables.getAsString( Key.of( "capturedSize" ) ) ).isEqualTo( "1024x1024" );
	}

	@DisplayName( "aiImage with OpenAI returns AiImageResponse with at least one image" )
	@Test
	public void testAiImageOpenAIReturnsResponse() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			response      = aiImage( "a simple red circle on a white background" )
			hasImages     = response.hasImages()
			imageCount    = response.getCount()
			firstBase64   = response.getFirstBase64()
			providerName  = response.getProvider()
			""",
			context
		);
		// @formatter:on

		var	hasImages		= variables.getAsBoolean( Key.of( "hasImages" ) );
		var	imageCount		= variables.getAsInteger( Key.of( "imageCount" ) );
		var	firstBase64		= variables.getAsString( Key.of( "firstBase64" ) );
		var	providerName	= variables.getAsString( Key.of( "providerName" ) );

		assertThat( hasImages ).isTrue();
		assertThat( imageCount ).isGreaterThan( 0 );
		assertThat( firstBase64 ).isNotEmpty();
		assertThat( providerName ).isEqualTo( "OpenAI" );
	}

	@DisplayName( "aiImage with outputFile option saves image to disk and returns path" )
	@Test
	public void testAiImageSavesToFile() {
		var outputPath = "/tmp/bxai-image-test.png";
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			savedPath        = aiImage(
				prompt : "a simple green triangle",
				options: { outputFile: "#outputPath#" }
			)
			fileExistsResult = fileExists( savedPath )
			""".replace( "#outputPath#", outputPath ),
			context
		);
		// @formatter:on

		var fileExistsResult = variables.getAsBoolean( Key.of( "fileExistsResult" ) );
		assertThat( fileExistsResult ).isTrue();

		// Cleanup
		new java.io.File( outputPath ).delete();
	}

	@DisplayName( "aiImage returns a string path when outputFile is set, not an AiImageResponse" )
	@Test
	public void testAiImageWithOutputFileReturnsString() {
		var outputPath = "/tmp/bxai-image-string-test.png";
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			result       = aiImage( prompt: "a yellow star", options: { outputFile: "#outputPath#" } )
			resultIsString = isSimpleValue( result )
			""".replace( "#outputPath#", outputPath ),
			context
		);
		// @formatter:on

		var resultIsString = variables.getAsBoolean( Key.of( "resultIsString" ) );
		assertThat( resultIsString ).isTrue();

		// Cleanup
		new java.io.File( outputPath ).delete();
	}

	// ==========================================================================
	// Fluent Builder API Tests
	// ==========================================================================

	@DisplayName( "aiImage() with no arguments returns an AiImageRequest builder" )
	@Test
	public void testAiImageNoArgsReturnsBuilder() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiImage()
			isBuilder = isInstanceOf( result, "AiImageRequest" )
			""",
			context
		);
		// @formatter:on

		var isBuilder = variables.getAsBoolean( Key.of( "isBuilder" ) );
		assertThat( isBuilder ).isTrue();
	}

	@DisplayName( "AiImageRequest.of() static factory creates a request with the given prompt" )
	@Test
	public void testAiImageRequestOfStaticFactory() {
		// @formatter:off
		runtime.executeSource(
			"""
			import bxModules.bxai.models.requests.AiImageRequest
			imageRequest = AiImageRequest.of( "a red fox in autumn leaves" )
			result = imageRequest.getPrompt()
			""",
			context
		);
		// @formatter:on

		var result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "a red fox in autumn leaves" );
	}

	@DisplayName( "Fluent builder .landscape().high() sets size and quality correctly" )
	@Test
	public void testFluentBuilderSizeAndQuality() {
		// @formatter:off
		runtime.executeSource(
			"""
			result = aiImage()
				.prompt( "mountain landscape" )
				.landscape()
				.high()

			prompt = result.getPrompt()
			size   = result.getSize()
			quality = result.getQuality()
			""",
			context
		);
		// @formatter:on

		var	prompt	= variables.getAsString( Key.of( "prompt" ) );
		var	size	= variables.getAsString( Key.of( "size" ) );
		var	quality	= variables.getAsString( Key.of( "quality" ) );
		assertThat( prompt ).isEqualTo( "mountain landscape" );
		assertThat( size ).isEqualTo( "1536x1024" );
		assertThat( quality ).isEqualTo( "high" );

	}

	@DisplayName( "Fluent builder .portrait().low() sets portrait size and low quality" )
	@Test
	public void testFluentBuilderPortraitAndLowQuality() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedSize    = "";
			capturedQuality = "";
			BoxRegisterInterceptor(
				function( data ) {
					capturedSize    = data.imageRequest.getSize();
					capturedQuality = data.imageRequest.getQuality();
				},
				"beforeAIImageGeneration"
			);
			aiImage()
				.prompt( "portrait photo" )
				.portrait()
				.low()
				.generate();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedSize" ) ) ).isEqualTo( "1024x1536" );
		assertThat( variables.getAsString( Key.of( "capturedQuality" ) ) ).isEqualTo( "low" );
	}

	@DisplayName( "Fluent builder .style().instructions() sets style and instructions" )
	@Test
	public void testFluentBuilderStyleAndInstructions() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedStyle      = "";
			capturedInstructions = "";
			BoxRegisterInterceptor(
				function( data ) {
					capturedStyle      = data.imageRequest.getStyle();
					capturedInstructions = data.imageRequest.getInstructions();
				},
				"beforeAIImageGeneration"
			);
			aiImage()
				.prompt( "a cat" )
				.style( "vivid" )
				.instructions( "in the style of impressionist painting" )
				.generate();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedStyle" ) ) ).isEqualTo( "vivid" );
		assertThat( variables.getAsString( Key.of( "capturedInstructions" ) ) ).isEqualTo( "in the style of impressionist painting" );
	}

	@DisplayName( "Fluent builder .asWebp() sets output format to webp" )
	@Test
	public void testFluentBuilderOutputFormatWebp() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedFormat = "";
			BoxRegisterInterceptor(
				function( data ) { capturedFormat = data.imageRequest.getFormat(); },
				"beforeAIImageGeneration"
			);
			aiImage()
				.prompt( "a blue circle" )
				.asWebp()
				.generate();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedFormat" ) ) ).isEqualTo( "webp" );
	}

	@DisplayName( "Fluent builder .asJpeg() sets output format to jpeg" )
	@Test
	public void testFluentBuilderOutputFormatJpeg() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedFormat = "";
			BoxRegisterInterceptor(
				function( data ) { capturedFormat = data.imageRequest.getFormat(); },
				"beforeAIImageGeneration"
			);
			aiImage()
				.prompt( "a green square" )
				.asJpeg()
				.generate();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedFormat" ) ) ).isEqualTo( "jpeg" );
	}

	@DisplayName( "Fluent builder .outputFormat('b64_json') sets response format" )
	@Test
	public void testFluentBuilderOutputFormatB64Json() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedOutputFormat = "";
			BoxRegisterInterceptor(
				function( data ) { capturedOutputFormat = data.imageRequest.getOutputFormat(); },
				"beforeAIImageGeneration"
			);
			aiImage()
				.prompt( "a purple triangle" )
				.outputFormat( "b64_json" )
				.generate();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedOutputFormat" ) ) ).isEqualTo( "b64_json" );
	}

	@DisplayName( "Fluent builder .n(2) sets number of images" )
	@Test
	public void testFluentBuilderImageCount() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedN = 0;
			BoxRegisterInterceptor(
				function( data ) { capturedN = data.imageRequest.getN(); },
				"beforeAIImageGeneration"
			);
			aiImage()
				.prompt( "three colored dots" )
				.n( 2 )
				.generate();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "capturedN" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "Fluent builder .outputFile() saves image and returns file path" )
	@Test
	public void testFluentBuilderOutputFile() {
		var outputPath = "/tmp/bxai-fluent-image-test.png";
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			savedPath = aiImage()
				.prompt( "a simple orange diamond" )
				.outputFile( "#outputPath#" )
				.generate()
			fileExistsResult = fileExists( savedPath )
			""".replace( "#outputPath#", outputPath ),
			context
		);
		// @formatter:on

		var fileExistsResult = variables.getAsBoolean( Key.of( "fileExistsResult" ) );
		assertThat( fileExistsResult ).isTrue();

		// Cleanup
		new java.io.File( outputPath ).delete();
	}

	@DisplayName( "Fluent builder .outputCompression() sets compression param" )
	@Test
	public void testFluentBuilderOutputCompression() {
		// @formatter:off
		runtime.executeSource(
			"""
			capturedCompression = 0;
			BoxRegisterInterceptor(
				function( data ) { capturedCompression = data.imageRequest.getParams().output_compression ?: 0; },
				"beforeAIImageGeneration"
			);
			aiImage()
				.prompt( "a compressed image test" )
				.asJpeg()
				.outputCompression( 75 )
				.generate();
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsInteger( Key.of( "capturedCompression" ) ) ).isEqualTo( 75 );
	}

}
