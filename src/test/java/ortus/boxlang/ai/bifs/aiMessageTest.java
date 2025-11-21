package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class aiMessageTest extends BaseIntegrationTest {

	@DisplayName( "Can create an empty aiMessage" )
	@Test
	public void testBasicMessage() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
				result = message.getMessages()
				bindings = message.getBindings()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array messages = ( Array ) variables.get( "result" );
		assertThat( messages.size() ).isEqualTo( 0 );
		IStruct bindings = ( IStruct ) variables.get( "bindings" );
		assertThat( bindings.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can create an aiMessage with initial role and content" )
	@Test
	public void testMessageWithContent() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage( { role = "user", content = "Hello, AI!"} )
				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array messages = ( Array ) variables.get( "result" );
		assertThat( messages.size() ).isEqualTo( 1 );

		IStruct firstMessage = ( IStruct ) messages.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, AI!" );
	}

	@DisplayName( "Can bind variables to a message" )
	@Test
	public void testBind() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.bind( { name = "John", age = 30 } )
				bindings = message.getBindings()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		IStruct bindings = ( IStruct ) variables.get( "bindings" );
		assertThat( bindings.size() ).isEqualTo( 2 );
		assertThat( bindings.getAsString( Key.of( "name" ) ) ).isEqualTo( "John" );
		assertThat( bindings.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
	}

	@DisplayName( "Can format messages with bindings" )
	@Test
	public void testFormat() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage( "Hello, ${name}! You are ${age} years old." )
				result = message.format( { name = "Jane", age = 25 } )
				original = message.getMessages()

				println( result )
				println(  original)
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array formatted = ( Array ) variables.get( "result" );
		assertThat( formatted.size() ).isEqualTo( 1 );

		IStruct firstMessage = ( IStruct ) formatted.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, Jane! You are 25 years old." );
		assertThat( firstMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
	}

	@DisplayName( "Can format multiple messages with bindings" )
	@Test
	public void testFormatMultipleMessages() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.add( { role = "system", content = "You are a ${role} assistant." } )
					.add( "Hello, ${name}! The weather in ${city} is ${weather}." )
				result = message.format( {
					role = "helpful",
					name = "Bob",
					city = "Paris",
					weather = "sunny"
				} )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array formatted = ( Array ) variables.get( "result" );
		assertThat( formatted.size() ).isEqualTo( 2 );

		IStruct systemMessage = ( IStruct ) formatted.get( 0 );
		assertThat( systemMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "system" );
		assertThat( systemMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "You are a helpful assistant." );

		IStruct userMessage = ( IStruct ) formatted.get( 1 );
		assertThat( userMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( userMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, Bob! The weather in Paris is sunny." );
	}

	@DisplayName( "Can render messages using stored bindings" )
	@Test
	public void testRender() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage( "Hello, ${name}! Welcome to ${app}." )
					.bind( { name = "Alice", app = "BoxLang AI" } )
				result = message.render()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array rendered = ( Array ) variables.get( "result" );
		assertThat( rendered.size() ).isEqualTo( 1 );

		IStruct firstMessage = ( IStruct ) rendered.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, Alice! Welcome to BoxLang AI." );
		assertThat( firstMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
	}

	@DisplayName( "Can render multiple messages with stored bindings" )
	@Test
	public void testRenderMultipleMessages() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.add( { role = "system", content = "You are ${role}." } )
					.add( "My name is ${name} and I live in ${city}." )
					.add( { role = "assistant", content = "Nice to meet you, ${name}!" } )
					.bind( {
						role = "a friendly assistant",
						name = "Carlos",
						city = "Madrid"
					} )
				result = message.render()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array rendered = ( Array ) variables.get( "result" );
		assertThat( rendered.size() ).isEqualTo( 3 );

		IStruct systemMessage = ( IStruct ) rendered.get( 0 );
		assertThat( systemMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "system" );
		assertThat( systemMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "You are a friendly assistant." );

		IStruct userMessage = ( IStruct ) rendered.get( 1 );
		assertThat( userMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( userMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "My name is Carlos and I live in Madrid." );

		IStruct assistantMessage = ( IStruct ) rendered.get( 2 );
		assertThat( assistantMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "assistant" );
		assertThat( assistantMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Nice to meet you, Carlos!" );
	}

	@DisplayName( "Can use aiMessage as a runnable" )
	@Test
	public void testMessageAsRunnable() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.user( "Hello, ${name}!" )
					.bind( { name: "World" } )
				result = message.run()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, World!" );
	}

	@DisplayName( "Can chain aiMessage with transforms" )
	@Test
	public void testMessageChaining() {

		// @formatter:off
		runtime.executeSource(
		    """
				pipeline = aiMessage()
					.user( "Hello, ${name}!" )
					.bind( { name: "AI" } )
					.to( aiTransform( messages => messages.len() ) )

				result = pipeline.run()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "pipeline" ) ).isNotNull();
		assertThat( ( ( Number ) variables.get( "result" ) ).intValue() ).isEqualTo( 1 );
	}

	@DisplayName( "Can stream aiMessage" )
	@Test
	public void testMessageStream() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.system( "You are ${role}." )
					.user( "Hello!" )
					.bind( { role: "helpful" } )

				chunks = []
				message.stream(
					onChunk = ( chunk, metadata ) => {
						chunks.append( chunk )
					}
				)
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array chunks = ( Array ) variables.get( "chunks" );
		assertThat( chunks.size() ).isEqualTo( 2 );

		IStruct systemMessage = ( IStruct ) chunks.get( 0 );
		assertThat( systemMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "You are helpful." );

		IStruct userMessage = ( IStruct ) chunks.get( 1 );
		assertThat( userMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello!" );
	}

	@DisplayName( "Can use withName on aiMessage" )
	@Test
	public void testMessageWithName() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.user( "Test" )
					.withName( "TestMessage" )

				name = message.getName()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "TestMessage" );
	}

	@DisplayName( "Can pass bindings via run input parameter" )
	@Test
	public void testMessageRunWithInputBindings() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.user( "Hello, ${name}!" )
					.system( "You are ${role}." )

				// Pass bindings as input to run()
				result = message.run( { name: "Alice", role: "helpful" } )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 2 );

		IStruct userMessage = ( IStruct ) result.get( 0 );
		assertThat( userMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, Alice!" );

		IStruct systemMessage = ( IStruct ) result.get( 1 );
		assertThat( systemMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "You are helpful." );
	}

	@DisplayName( "Input bindings merge with stored bindings" )
	@Test
	public void testMessageRunBindingsMerge() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.user( "Hello, ${name}! You are ${age} years old." )
					.bind( { name: "Bob" } )

				// Input bindings merge with stored bindings
				result = message.run( { age: 30 } )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, Bob! You are 30 years old." );
	}

	@DisplayName( "Input bindings override stored bindings" )
	@Test
	public void testMessageRunBindingsOverride() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.user( "Hello, ${name}!" )
					.bind( { name: "Original" } )

				// Input bindings override stored bindings
				result = message.run( { name: "Override" } )
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, Override!" );
	}

	@DisplayName( "Can use aiMessage without any bindings" )
	@Test
	public void testMessageRunWithoutBindings() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.user( "Hello, world!" )
					.system( "You are helpful." )

				result = message.run()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 2 );

		IStruct userMessage = ( IStruct ) result.get( 0 );
		assertThat( userMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello, world!" );

		IStruct systemMessage = ( IStruct ) result.get( 1 );
		assertThat( systemMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "You are helpful." );
	}

	@DisplayName( "Can add history from an array of messages" )
	@Test
	public void testHistoryWithArray() {

		// @formatter:off
		runtime.executeSource(
		    """
				historyMessages = [
					{ role: "system", content: "You are a helpful assistant." },
					{ role: "user", content: "Hello!" },
					{ role: "assistant", content: "Hi there! How can I help you?" }
				]

				message = aiMessage()
					.history( historyMessages )
					.user( "Tell me a joke" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 4 );

		IStruct systemMessage = ( IStruct ) result.get( 0 );
		assertThat( systemMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "system" );
		assertThat( systemMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "You are a helpful assistant." );

		IStruct userMessage1 = ( IStruct ) result.get( 1 );
		assertThat( userMessage1.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( userMessage1.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello!" );

		IStruct assistantMessage = ( IStruct ) result.get( 2 );
		assertThat( assistantMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "assistant" );
		assertThat( assistantMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hi there! How can I help you?" );

		IStruct userMessage2 = ( IStruct ) result.get( 3 );
		assertThat( userMessage2.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( userMessage2.getAsString( Key.of( "content" ) ) ).isEqualTo( "Tell me a joke" );
	}

	@DisplayName( "Can add history from another AiMessage instance" )
	@Test
	public void testHistoryWithAiMessage() {

		// @formatter:off
		runtime.executeSource(
		    """
				previousConversation = aiMessage()
					.system( "You are a helpful assistant." )
					.user( "What's 2+2?" )
					.assistant( "4" )

				newMessage = aiMessage()
					.history( previousConversation )
					.user( "What about 3+3?" )

				result = newMessage.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "newMessage" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 4 );

		IStruct systemMessage = ( IStruct ) result.get( 0 );
		assertThat( systemMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "system" );
		assertThat( systemMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "You are a helpful assistant." );

		IStruct userMessage1 = ( IStruct ) result.get( 1 );
		assertThat( userMessage1.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( userMessage1.getAsString( Key.of( "content" ) ) ).isEqualTo( "What's 2+2?" );

		IStruct assistantMessage = ( IStruct ) result.get( 2 );
		assertThat( assistantMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "assistant" );
		assertThat( assistantMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "4" );

		IStruct userMessage2 = ( IStruct ) result.get( 3 );
		assertThat( userMessage2.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( userMessage2.getAsString( Key.of( "content" ) ) ).isEqualTo( "What about 3+3?" );
	}

	@DisplayName( "History method throws error for invalid input" )
	@Test
	public void testHistoryWithInvalidInput() {

		// @formatter:off
		try {
			runtime.executeSource(
			    """
					message = aiMessage()
						.history( "invalid string input" )
			    """,
			    context
			);
			fail( "Should have thrown an exception for invalid history input" );
		} catch ( Exception e ) {
			assertThat( e.getMessage() ).contains( "History messages must be an array or an AiMessage instance" );
		}
		// @formatter:on
	}

	@DisplayName( "Can chain history with other message methods" )
	@Test
	public void testHistoryChaining() {

		// @formatter:off
		runtime.executeSource(
		    """
				historyMessages = [
					{ role: "user", content: "First question" },
					{ role: "assistant", content: "First answer" }
				]

				message = aiMessage()
					.system( "You are helpful" )
					.history( historyMessages )
					.user( "Second question" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 4 );

		// Verify order: system, user, assistant, user
		IStruct systemMsg = ( IStruct ) result.get( 0 );
		assertThat( systemMsg.getAsString( Key.of( "role" ) ) ).isEqualTo( "system" );

		IStruct user1 = ( IStruct ) result.get( 1 );
		assertThat( user1.getAsString( Key.of( "content" ) ) ).isEqualTo( "First question" );

		IStruct assistant = ( IStruct ) result.get( 2 );
		assertThat( assistant.getAsString( Key.of( "content" ) ) ).isEqualTo( "First answer" );

		IStruct user2 = ( IStruct ) result.get( 3 );
		assertThat( user2.getAsString( Key.of( "content" ) ) ).isEqualTo( "Second question" );
	}

	@DisplayName( "Can add an image URL to a message" )
	@Test
	public void testImageWithUrl() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.user( "What is in this image?" )
					.image( "https://example.com/image.jpg" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );

		// Content should be an array with text and image parts
		Array content = ( Array ) firstMessage.get( Key.of( "content" ) );
		assertThat( content.size() ).isEqualTo( 2 );

		// First part should be text
		IStruct textPart = ( IStruct ) content.get( 0 );
		assertThat( textPart.getAsString( Key.of( "type" ) ) ).isEqualTo( "text" );
		assertThat( textPart.getAsString( Key.of( "text" ) ) ).isEqualTo( "What is in this image?" );

		// Second part should be image
		IStruct imagePart = ( IStruct ) content.get( 1 );
		assertThat( imagePart.getAsString( Key.of( "type" ) ) ).isEqualTo( "image_url" );
		IStruct imageUrl = ( IStruct ) imagePart.get( Key.of( "image_url" ) );
		assertThat( imageUrl.getAsString( Key.of( "url" ) ) ).isEqualTo( "https://example.com/image.jpg" );
	}

	@DisplayName( "Can add an image with detail level" )
	@Test
	public void testImageWithDetail() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.user( "Analyze this image" )
					.image( "https://example.com/image.jpg", "high" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct	firstMessage	= ( IStruct ) result.get( 0 );
		Array	content			= ( Array ) firstMessage.get( Key.of( "content" ) );

		IStruct	imagePart		= ( IStruct ) content.get( 1 );
		IStruct	imageUrl		= ( IStruct ) imagePart.get( Key.of( "image_url" ) );
		assertThat( imageUrl.getAsString( Key.of( "url" ) ) ).isEqualTo( "https://example.com/image.jpg" );
		assertThat( imageUrl.getAsString( Key.of( "detail" ) ) ).isEqualTo( "high" );
	}

	@DisplayName( "Can add multiple images to a message" )
	@Test
	public void testMultipleImages() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.user( "Compare these images" )
					.image( "https://example.com/image1.jpg" )
					.image( "https://example.com/image2.jpg" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct	firstMessage	= ( IStruct ) result.get( 0 );
		Array	content			= ( Array ) firstMessage.get( Key.of( "content" ) );
		assertThat( content.size() ).isEqualTo( 3 ); // text + 2 images

		// Verify first image
		IStruct	imagePart1	= ( IStruct ) content.get( 1 );
		IStruct	imageUrl1	= ( IStruct ) imagePart1.get( Key.of( "image_url" ) );
		assertThat( imageUrl1.getAsString( Key.of( "url" ) ) ).isEqualTo( "https://example.com/image1.jpg" );

		// Verify second image
		IStruct	imagePart2	= ( IStruct ) content.get( 2 );
		IStruct	imageUrl2	= ( IStruct ) imagePart2.get( Key.of( "image_url" ) );
		assertThat( imageUrl2.getAsString( Key.of( "url" ) ) ).isEqualTo( "https://example.com/image2.jpg" );
	}

	@DisplayName( "Can add image without prior text message" )
	@Test
	public void testImageWithoutText() {

		// @formatter:off
		runtime.executeSource(
		    """
				message = aiMessage()
					.image( "https://example.com/image.jpg" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );

		// Content should be an array with just the image
		Array content = ( Array ) firstMessage.get( Key.of( "content" ) );
		assertThat( content.size() ).isEqualTo( 1 );

		IStruct imagePart = ( IStruct ) content.get( 0 );
		assertThat( imagePart.getAsString( Key.of( "type" ) ) ).isEqualTo( "image_url" );
	}

	@DisplayName( "Can embed an image from file path" )
	@Test
	public void testEmbedImage() {

		// @formatter:off
		runtime.executeSource(
		    """
				// Create a test image file with minimal PNG data
				testImagePath = "/tmp/test-image.png"
				// Minimal 1x1 PNG image (base64: iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==)
				testImageData = toBinary( "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" )
				fileWrite( testImagePath, testImageData )

				message = aiMessage()
					.user( "Analyze this embedded image" )
					.embedImage( testImagePath )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct	firstMessage	= ( IStruct ) result.get( 0 );
		Array	content			= ( Array ) firstMessage.get( Key.of( "content" ) );
		assertThat( content.size() ).isEqualTo( 2 );

		// Verify the image part contains a data URI
		IStruct	imagePart	= ( IStruct ) content.get( 1 );
		IStruct	imageUrl	= ( IStruct ) imagePart.get( Key.of( "image_url" ) );
		String	url			= imageUrl.getAsString( Key.of( "url" ) );
		assertThat( url ).startsWith( "data:image/png;base64," );
		assertThat( url ).contains( "iVBORw0KGgo" ); // Beginning of base64 PNG data
	}

	@DisplayName( "Can embed an image with detail level" )
	@Test
	public void testEmbedImageWithDetail() {

		// @formatter:off
		runtime.executeSource(
		    """
				// Create a test file with .jpg extension (tests MIME type detection)
				testImagePath = "/tmp/test-image.jpg"
				testImageData = toBinary( "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" )
				fileWrite( testImagePath, testImageData )

				message = aiMessage()
					.user( "Analyze this" )
					.embedImage( testImagePath, "low" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct	firstMessage	= ( IStruct ) result.get( 0 );
		Array	content			= ( Array ) firstMessage.get( Key.of( "content" ) );

		IStruct	imagePart		= ( IStruct ) content.get( 1 );
		IStruct	imageUrl		= ( IStruct ) imagePart.get( Key.of( "image_url" ) );
		String	url				= imageUrl.getAsString( Key.of( "url" ) );
		assertThat( url ).startsWith( "data:image/jpeg;base64," );
		assertThat( imageUrl.getAsString( Key.of( "detail" ) ) ).isEqualTo( "low" );
	}
}
