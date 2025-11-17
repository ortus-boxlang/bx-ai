package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

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

	@DisplayName( "Can prepend history as an array" )
	@Test
	public void testHistoryWithArray() {

		// @formatter:off
		runtime.executeSource(
		    """
				// Create history messages
				historyMessages = [
					{ role = "user", content = "What is BoxLang?" },
					{ role = "assistant", content = "BoxLang is a modern JVM language." }
				]

				// Create new message and prepend history
				message = aiMessage()
					.history( historyMessages )
					.user( "Tell me more" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 3 );

		// Verify history messages are first
		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "What is BoxLang?" );

		IStruct secondMessage = ( IStruct ) result.get( 1 );
		assertThat( secondMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "assistant" );
		assertThat( secondMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "BoxLang is a modern JVM language." );

		// Verify new message is last
		IStruct thirdMessage = ( IStruct ) result.get( 2 );
		assertThat( thirdMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "user" );
		assertThat( thirdMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Tell me more" );
	}

	@DisplayName( "Can prepend history as an AiMessage instance" )
	@Test
	public void testHistoryWithAiMessage() {

		// @formatter:off
		runtime.executeSource(
		    """
				// Create history as an AiMessage instance
				history = aiMessage()
					.user( "Hello" )
					.assistant( "Hi there!" )
					.user( "How are you?" )
					.assistant( "I'm doing well, thanks!" )

				// Create new message and prepend history
				message = aiMessage()
					.history( history )
					.user( "Great! Can you help me?" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 5 );

		// Verify history messages are first
		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello" );

		IStruct secondMessage = ( IStruct ) result.get( 1 );
		assertThat( secondMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hi there!" );

		IStruct thirdMessage = ( IStruct ) result.get( 2 );
		assertThat( thirdMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "How are you?" );

		IStruct fourthMessage = ( IStruct ) result.get( 3 );
		assertThat( fourthMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "I'm doing well, thanks!" );

		// Verify new message is last
		IStruct fifthMessage = ( IStruct ) result.get( 4 );
		assertThat( fifthMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Great! Can you help me?" );
	}

	@DisplayName( "Can chain history() with other methods" )
	@Test
	public void testHistoryChaining() {

		// @formatter:off
		runtime.executeSource(
		    """
				historyMessages = [
					{ role = "system", content = "You are helpful" },
					{ role = "user", content = "Previous question" }
				]

				message = aiMessage()
					.history( historyMessages )
					.user( "New question" )
					.bind( { name: "Alice" } )

				result = message.getMessages()
				name = message.getBindings().name
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 3 );
		assertThat( variables.getAsString( Key.of( "name" ) ) ).isEqualTo( "Alice" );
	}

	@DisplayName( "Can prepend empty history" )
	@Test
	public void testEmptyHistory() {

		// @formatter:off
		runtime.executeSource(
		    """
				emptyHistory = []

				message = aiMessage()
					.history( emptyHistory )
					.user( "Hello" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 1 );

		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Hello" );
	}

	@DisplayName( "Can prepend history to existing messages" )
	@Test
	public void testHistoryToExistingMessages() {

		// @formatter:off
		runtime.executeSource(
		    """
				// Create message with initial content
				message = aiMessage()
					.system( "You are helpful" )
					.user( "Current question" )

				// Now prepend history
				historyMessages = [
					{ role = "user", content = "Past question 1" },
					{ role = "assistant", content = "Past answer 1" }
				]
				message.history( historyMessages )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 4 );

		// Verify history comes first
		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Past question 1" );

		IStruct secondMessage = ( IStruct ) result.get( 1 );
		assertThat( secondMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Past answer 1" );

		// Verify original messages come after
		IStruct thirdMessage = ( IStruct ) result.get( 2 );
		assertThat( thirdMessage.getAsString( Key.of( "role" ) ) ).isEqualTo( "system" );

		IStruct fourthMessage = ( IStruct ) result.get( 3 );
		assertThat( fourthMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Current question" );
	}

	@DisplayName( "Can use history() multiple times" )
	@Test
	public void testMultipleHistoryCalls() {

		// @formatter:off
		runtime.executeSource(
		    """
				// Add first batch of history
				firstHistory = [
					{ role = "user", content = "Message 1" }
				]

				// Add second batch of history
				secondHistory = [
					{ role = "user", content = "Message 2" }
				]

				message = aiMessage()
					.history( firstHistory )
					.history( secondHistory )
					.user( "Message 3" )

				result = message.getMessages()
		    """,
		    context
		);
		// @formatter:on

		assertThat( variables.get( "message" ) ).isNotNull();
		Array result = ( Array ) variables.get( "result" );
		assertThat( result.size() ).isEqualTo( 3 );

		// Verify order - second history should be first (most recent prepend)
		IStruct firstMessage = ( IStruct ) result.get( 0 );
		assertThat( firstMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 2" );

		IStruct secondMessage = ( IStruct ) result.get( 1 );
		assertThat( secondMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 1" );

		IStruct thirdMessage = ( IStruct ) result.get( 2 );
		assertThat( thirdMessage.getAsString( Key.of( "content" ) ) ).isEqualTo( "Message 3" );
	}
}
