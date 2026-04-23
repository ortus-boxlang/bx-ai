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
			toolExists = aiToolRegistry().get( "generateImage@bxai" ).isPresent()
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

		var threw = variables.getAsBoolean( Key.of( "threw" ) );
		var errorType = variables.getAsString( Key.of( "errorType" ) );
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
			);
			try {
				aiImage(
					prompt  : "a sunset over the ocean",
					options : { size: "1792x1024" }
				);
			} catch( any e ) {
				// expected — API call may fail without a valid key
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "capturedPrompt" ) ) ).isEqualTo( "a sunset over the ocean" );
		assertThat( variables.getAsString( Key.of( "capturedSize" ) ) ).isEqualTo( "1792x1024" );
	}

	@DisplayName( "aiImage with OpenAI returns AiImageResponse with at least one image" )
	@Test
	public void testAiImageOpenAIReturnsResponse() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			response     = aiImage( "a simple red circle on a white background" )
			hasImages    = response.hasImages()
			imageCount   = response.getCount()
			firstURL     = response.getFirstURL()
			providerName = response.getProvider()
			""",
			context
		);
		// @formatter:on

		var hasImages    = variables.getAsBoolean( Key.of( "hasImages" ) );
		var imageCount   = variables.getAsInteger( Key.of( "imageCount" ) );
		var firstURL     = variables.getAsString( Key.of( "firstURL" ) );
		var providerName = variables.getAsString( Key.of( "providerName" ) );

		assertThat( hasImages ).isTrue();
		assertThat( imageCount ).isGreaterThan( 0 );
		assertThat( firstURL ).isNotEmpty();
		assertThat( providerName ).isEqualTo( "OpenAI" );
	}

	@DisplayName( "aiImage with Gemini Imagen returns AiImageResponse with binary image data" )
	@Test
	public void testAiImageGeminiReturnsResponse() {
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			response   = aiImage(
				"a simple blue square on a white background",
				options: { provider: "gemini", apiKey: geminiKey }
			)
			hasImages  = response.hasImages()
			imageCount = response.getCount()
			provider   = response.getProvider()
			// Gemini returns binary data directly (no URL)
			firstImage = response.getFirstImage()
			hasData    = !isNull( firstImage.data ) && arrayLen( firstImage.data ) > 0
			""".replace( "geminiKey", "\"" + dotenv.get( "GEMINI_API_KEY", "" ) + "\"" ),
			context
		);
		// @formatter:on

		var hasImages  = variables.getAsBoolean( Key.of( "hasImages" ) );
		var imageCount = variables.getAsInteger( Key.of( "imageCount" ) );
		var provider   = variables.getAsString( Key.of( "provider" ) );
		var hasData    = variables.getAsBoolean( Key.of( "hasData" ) );

		assertThat( hasImages ).isTrue();
		assertThat( imageCount ).isGreaterThan( 0 );
		assertThat( provider ).isEqualTo( "Gemini" );
		assertThat( hasData ).isTrue();
	}

	@DisplayName( "aiImage with outputFile option saves image to disk and returns path" )
	@Test
	public void testAiImageSavesToFile() {
		var outputPath = "/tmp/bxai-image-test.png";
		// @formatter:off
		executeWithTimeoutHandling(
			"""
			savedPath        = aiImage(
				"a simple green triangle",
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
			result       = aiImage( "a yellow star", options: { outputFile: "#outputPath#" } )
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

}
