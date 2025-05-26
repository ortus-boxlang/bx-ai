# ⚡︎ BoxLang Module: BoxLang AI

```
|:------------------------------------------------------:|
| ⚡︎ B o x L a n g ⚡︎
| Dynamic : Modular : Productive
|:------------------------------------------------------:|
```

<blockquote>
	Copyright Since 2023 by Ortus Solutions, Corp
	<br>
	<a href="https://www.boxlang.io">www.boxlang.io</a> |
	<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</blockquote>

<p>&nbsp;</p>

## Welcome

Welcome to the BoxLang AI Module. This module provides AI generation capabilities to your [BoxLang](www.boxlang.io) applications in an easy to use and abstracted API, so you can interact with ANY AI provider in a consistent manner.

> We also have an `bx-aiplus` module that enhances this module with more AI providers, capabilities and features.  The `bx-aiplus` module is part of our [BoxLang +/++ subscriptions](https://boxlang.io/plans).

## License

BoxLang is open source and licensed under the [Apache 2](https://www.apache.org/licenses/LICENSE-2.0.html) license.

## Getting Started

You can easily get started with BoxLang AI by using the module installer:

```bash
install-bx-module bx-ai
```

If you would like to leverage it in your CommandBox Based Web applications, make sure you add it to your `server.json` or use `box install bx-ai`.

Once installed you can leverage the global functions (BIFs) in your BoxLang code.  Here is a simple example:

```java
// chat.bxs
answer = aiChat( "How amazing is BoxLang?" )
println( answer )
```

## Providers

The following are the AI providers supported by this module.  **Please note that in order to interact with these providers you will need to have an account with them and an API key.**

- [Claude Anthropic](https://www.anthropic.com/claude)
- [DeepSeek](https://www.deepseek.com/)
- [Gemini](https://gemini.google.com/)
- [Grok](https://grok.com/)
- [OpenAI](https://www.openai.com/)
- [Perplexity](https://docs.perplexity.ai/)

> More providers and features are available in our `bx-aiplus` module.

## Features

Here are some of the features of this module:

- Integration with multiple AI providers
- Compose raw chat requests
- Build message objects
- Create AI service objects
- Create AI tool objects
- Fluent API
- Asynchronous chat requests
- Global defaults
- And much more

## Tooling Matrix

Here is a matrix of the providers and if they support real-time tools.  Please keep checking as we will be adding more providers and features to this module.

| Provider   | Real-time Tools |
|-----------|-----------------|
| Claude    | [Coming Soon] |
| DeepSeek  | ✅              |
| Gemini    | [Coming Soon] |
| Grok      | ✅              |
| OpenAI    | ✅              |
| Perplexity| ✅              |

## Settings

Here are the settings you can place in your `boxlang.json` file:

```json
{
	"modules" : {
		"bxai" : {
			"settings": {
				// The provider to use: openai, deepseek, gemini, grok, perplexity, etc
				"provider" : "openai",
				// The API Key for the provider
				"apiKey" : "",
				// The default request params to use when calling a provider
				// Ex: { temperature: 0.5, max_tokens: 100, model: "gpt-3.5-turbo" }
				"defaultParams" : {
					// model: "gpt-3.5-turbo"
				},
				// The default timeout of the ai requests
				"timeout" : 30,
				// The default value to log the AI request into the ai.log
				"logRequest" : false,
				// The default value to log the AI response into the ai.log
				"logResponse" : false,
				// The default return format of the AI response: single, all, raw
				"returnFormat" : "single"
			}
		}
	}
}
```

## Global Functions (BIFs)

This module exposes the following BoxLang global functions (BIFs) for you to interact with the AI providers:

- `aiChat( messages, struct params={}, struct options={} )` : This function will allow you to chat with the AI provider and get responses back.  This is the easiest way to interact with the AI providers.
- `aiChatAsync( messages, struct params={}, struct options={} )` : This function will allow you to chat with the AI provider and get a BoxLang future back so you can build fluent asynchronous code pipelines.
- `aiChatRequest( messages, struct params, struct options, struct headers)` - This allows you to compose a raw chat request that you can then later send to an AI service.  The return is a `ChatRequest` object that you can then send to the AI service.
- `aiMessage( message )` - Allows you to build a message object that you can then use to send to the `aiChat()` or `aiChatRequest()` functions.  It allows you to fluently build up messages as well.
- `aiService( provider, apiKey )` - Creates a reference to an AI Service provider that you can then use to interact with the AI service.  This is useful if you want to create a service object and then use it multiple times.  You can pass in optional `provider` and `apiKey` to override the global settings.
- `aiTool( name, description, callable)` - Creates a tool object that you can use to add to a chat request for real-time system processing.  This is useful if you want to create a tool that can be used in multiple chat requests against localized resources.  You can then pass in the tool to the `aiChat()` or `aiChatRequest()` functions.

## aiChat()/aiChatAsync() - Chat with the AI

The `aiChat(), aiChatAsync()` functions are the easiest way to interact with the AI providers in a consistent and abstracted way.  Here are the signatures of the function:

```js
aiChat( messages, struct params={}, struct options={} )
aiChatAsync( messages, struct params={}, struct options={} )
```

Here are the parameters:

- `messages` : This can be any of the following
  - A `string` : A message with a default `role` of `user` will be used
  - A `struct` : A struct with a `role` and `content` key message
  - An `array of structs` : An array of messages that must have a `role` and a `content` keys
  - A `ChatMessage` object
- `params` : This is a struct of request parameters that will be passed to the AI provider.  This can be anything the provider supports.  Usually this is the `model`, `temperature`, `max_tokens`, etc.
- `options` : This is a struct of options that can be used to control the behavior of the AI provider.  The available options are:
  - `provider:string` : The provider to use, if not passed it will use the global setting
  - `apiKey:string` : The API Key to use, if not passed it will use the global setting
  - `timeout:numeric` : The timeout in milliseconds for the request. Default is 30 seconds.
  - `logRequest:boolean` : Log the request to the `ai.log`. Default is `false`
  - `logResponse:boolean` : Log the response to the `ai.log`. Default is `false`
  - `returnFormat:string` : The format of the response.  The default is a `single` message.  The available formats are:
    - `single` : A single message
    - `all` : An array of messages
    - `raw` : The raw response from the AI provider

The `aiChat()` function will return a message according to the `options.returnFormat` type. If you use `aiChatAsync()` it will return a BoxLang future so you can build fluent asynchronous code pipelines.

> Don't worry that you must do a `role` and `content` in your messages if you use a struct or an array of structs.  The ai providers will understand the structure and process it accordingly.

### System Messages

The `messages` argument as explained allows you to send 3 different types of messages.  Another caveat is that there can only be one `system` message per request.

### Examples

Here are some examples of chatting with the AI:

```js
// Simple chat
aiChat( "Write a haiku about recursion in programming." );

// Structural chat
aiChat( {
	"role": "user",
	"content": "Write a haiku about recursion in programming."
} );

// Using an array of messages
aiChat( [
	{
		"role": "system",
		"content": "You are a helpful assistant."
	},
	{
		"role": "user",
		"content": "Write a haiku about recursion in programming."
	}
] );

// Analyze an image
aiChat( {
	"role": "user",
	"content": [
		{
			"type": "text",
			"text": "What is in this image?"
		},
		{
			"type": "image_url",
			"image_url": {
				"url": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg"
			}
		}
	]
} );
```

Now let's do some async chatting.  The benefit of async chatting is that you can build fluent asynchronous code pipelines and not block the main thread.  Once you are ready for retrieval of the results, then you can use the blocking `get()` method on the future.

```java
var future = aiChatAsync( "Write a haiku about recursion in programming." )
    .then( result -> {
        println( "AI Response: " + result );
        return result;
    } )
    .onError( error -> {
        writeLog( text: "AI Chat failed: " + error.getMessage(), type: "error" );
        return "An error occurred. Please try again.";
    } );

// Later in the code, you can retrieve the result
<h2>Chat Response</h2>
<p>#future.get()#</p>

// Transforming and formatting the response
var future = aiChatAsync( "Write a haiku about recursion in programming." )
    .then( result -> "### AI-Generated Haiku
	<br>
	#result.trim()#
	" )
    .onError( error -> {
        writeLog( text: "AI Chat failed: " + error.getMessage(), type: "error" );
        return "### AI Error
		<p>#error.getMessage()#</p>
		<p>An unexpected error occurred</p>
		<p>Please try again</p>
		";
    } );

// Print or return the formatted result
println( future.get() );
```

## aiChatRequest() - Compose a Chat Request

The `aiChatRequest()` function allows you to compose a raw chat request that you can then later send to an AI service.  The return is a `ChatRequest` object that you can then send to the AI service.

```js
aiChatRequest( messages, struct params, struct options, struct headers )
```

Here are the parameters:

- `messages` : This can be any of the following
  - A `string` : A message with a default `role` of `user` will be used
  - A `struct` : A struct with a `role` and `content` key message
  - An `array of structs` : An array of messages that must have a `role` and a `content` keys
  - A `ChatMessage` object
- `params` : This is a struct of request parameters that will be passed to the AI provider.  This can be anything the provider supports.  Usually this is the `model`, `temperature`, `max_tokens`, etc.
- `options` : This is a struct of options that can be used to control the behavior of the AI provider.  The available options are:
  - `provider:string` : The provider to use, if not passed it will use the global setting
  - `apiKey:string` : The API Key to use, if not passed it will use the global setting
  - `timeout:numeric` : The timeout in milliseconds for the request. Default is 30 seconds.
  - `logRequest:boolean` : Log the request to the `ai.log`. Default is `false`
  - `logResponse:boolean` : Log the response to the `ai.log`. Default is `false`
  - `returnFormat:string` : The format of the response.  The default is a `single` message.  The available formats are:
    - `single` : A single message
    - `all` : An array of messages
    - `raw` : The raw response from the AI provider
- `headers` : This is a struct of headers that can be used to send to the AI provider.

### ChatRequest Properties

The `ChatRequest` object has several properties that you can use to interact with the request.  All of them have a getter and a setter.

- `messages:array` : The messages to send to the AI provider
- `params:struct` : The request parameters to send to the AI provider
- `provider:string` : The provider to use
- `apiKey:string` : The API Key to use
- `logRequest:boolean` : Log the request to the `ai.log`
- `logResponse:boolean` : Log the response to the `ai.log`
- `returnFormat:string` : The format of the response
- `timeout:numeric` : The timeout in milliseconds for the request. Default is 30 seconds.
- `sendAuthHeader:boolean` : Send the API Key as an Authorization header. Default is `true`
- `headers:struct` : The headers to send to the AI provider

### ChatRequest Methods

The `ChatRequest` object has several methods that you can use to interact with the request apart from the aforementioned properties setters and getters.

- `addHeader( name, value ):ChatRequest` : Add a header to the request
- `getTool( name ):Attempt` : Get a tool from the defined params
- `hasMessages():boolean` : Check if the request has messages
- `hasModel():boolean` : Check if the request has a model
- `setModelIfEmpty( model ):ChatRequest` : Set the model if it is empty
- `hasApiKey():boolean` : Check if the request has an API Key
- `setApiKeyIfEmpty( apiKey ):ChatRequest` : Set the API Key if it is empty

### Examples

Here are some examples of composing a chat request:

```js
// Simple chat request
chatRequest = aiChatRequest( "Write a haiku about recursion in programming." )
response = aiService().invoke( chatRequest )

// Advanced request
chatRequest = aiChatRequest( "Write a haiku about recursion in programming.", {
		"model": "gpt-3.5-turbo",
		"temperature": 0.5,
		"max_tokens": 100
	},
	{
		"provider": "grok",
		"timeout": 10,
		"logRequest": true,
		"logResponse": true,
		"returnFormat": "raw"
	} );
response = aiService().invoke( chatRequest )
```

## aiMessage() - Build a Message Object

This function allows you to build up messages that you can then use to send to the `aiChat()` or `aiChatRequest()` functions.  It allows you to fluently build up messages as well as it implements `onMissingMethod()`. Meaning that any method call that is not found in the `ChatMessage` object will be treated as `roled` message: `system( "message" ), user( "message" ), assistant( "message" )`.  This method returns a `ChatMessage` object.

This is also useful so you can keep track of your messages.

> Please note that the ai-plus module supports chat memory and more.

The `aiMessage()` function has the following signature:

```js
aiMessage( message )
```

Here are the parameters:

- `message` : This can be any of the following
  - A `string` : A message with a default `role` of `user` will be used
  - A `struct` : A struct with a `role` and `content` key message
  - An `array of structs` : An array of messages that must have a `role` and a `content` keys
  - A `ChatMessage` object itself.

### ChatMessage Methods

The `ChatMessage` object has several methods that you can use to interact with the message.

- `count():numeric` : Get the count of messages
- `getMessages():array` : Get the messages
- `setMessages( messagaes ):ChatMessage` : Set the messages
- `clear():ChatMessage` : Clear the messages
- `hasSystemMessage():boolean` : Check if the message has a system message
- `getSystemMessage():string` : Get the system message, if any.
- `replaceSystemMessage( content )` : Replace the system message with a new one
- `add( content ):ChatMessage` : Add a message to the messages array

### ChatMessage Dynamic Methods

The `ChatMessage` object is dynamic and will treat any method call that is not found as a **roled** message according to the name of the method you call.  This allows you to build up messages fluently.

```java
aiMessage()
	.system( "You are a helpful assistant." )
	.user( "Write a haiku about recursion in programming." )
	.user( "What is the capital of France?" )
```

### Examples

Here are a few examples of building up messages and sending them to the `aiChat()` or `aiChatRequest()` functions:

```js
aiChat(
	aiMessage()
		.system( "You are a helpful assistant." )
		.user( "Write a haiku about recursion in programming." )
		.user( "What is the capital of France?" )
)
```

## aiService() - Create an AI Service Object

This function allows you to create a reference to an AI Service provider that you can then use to interact with an AI service.  This is useful when you need to interact with a specific implementation of our `IAService` interface.

The `aiService()` function has the following signature:

```js
aiService( provider, apiKey )
```

Here are the parameters:

- `provider` : The provider to use, if not passed it will use the global setting
- `apiKey` : The API Key to use, if not passed it will use the global setting


### Service Methods

Here are some useful methods each provider implements and gets via the `BaseService` abstract class.

- `getName():string` : Get the name of the AI Service
- `configure( apiKey ):IService` : Configure the service with an override API key
- `invoke( chatRequest ):any` : Invoke the provider service with a ChatRequest object
- `getChatURL():string` : Get the chat URL of the provider
- `setChatURL( url ):IService` : Set the chat URL of the provider
- `defaults( struct params ):IService` : Set the default parameters for the provider

### IAiService Interface

Here is the interface that all AI Service providers must implement:

```java
/**
 * Interface for all AI Service classes
 */
interface{

	/**
	 * Get the name of the LLM
	 */
	function getName();

	/**
	 * Configure the service with an override API key
	 *
	 * @apiKey - The API key to use with the provider
	 *
	 * @return The service instance
	 */
	IService function configure( required any apiKey );

	/**
	 * Invoke the provider service with a ChatRequest object
	 *
	 * @chatRequest The ChatRequest object to send to the provider
	 *
	 * @return The response from the service, which can be anything according to their specs: string, or struct, or whatever
	 */
	function invoke( required ChatRequest chatRequest );

}
```

### BaseService

We have also provided a `BaseService` that implements the interface using the `OpenAI` standard.  This is a great starting point for you to create your own AI Service provider if needed.

### Examples

Here are a few examples of creating an AI Service object and interacting with it:

```js
// Create a service object
service = aiProvider( "grok" )
	.configure( "myApiKey" )
	.defaults( { model: "gpt-3.5-turbo", temperature: 0.5, max_tokens: 100 } )

// Invoke the service
response = service.invoke( aiChatRequest( "Write a haiku about recursion in programming." ) )
// Or
response = service.invoke(
	aiChatRequest( "Write a haiku about recursion in programming.", { model: "gpt-3.5-turbo", temperature: 0.5, max_tokens: 100 } )
)
```

## aiTool() - Create a Tool Object

This function allows you to create a tool object that you can use to add to a chat request for real-time system processing.  This is useful if you want to create a tool that can be used in multiple chat requests against localized resources.  You can then pass in the tool to the `aiChat()` or `aiChatRequest()` functions.

The `aiTool()` function has the following signature:

```js
aiTool( name, description, callable )
```

Here are the parameters:

- `name` : The name of the tool sent to the AI provider
- `description` : Describe the function. This is used by the AI to communicate the purpose of the function.
- `callable` : A closure/lambda to call when the tool is invoked.

Once a tool object is made, you can pass them into a chat's or chat request's `params` via the `tools` array.

```java
result = aiChat( messages = "How hot is it in Kansas City? What about San Salvador? Answer with only the name of the warmer city, nothing else.", params = {
	tools: [ tool1, tool2, tool3 ],
	seed: 27
} )
```

### Tool Properties

The `Tool` object has several properties that you can use to interact with the tool.

- `name:string` : The name of the tool
- `description:string` : The description of the tool
- `callable:function` : The closure/lambda to call when the tool is invoked
- `schema:struct` : The schema of the tool
- `argDescriptions:struc` : The argument descriptions of the tool

Each of them have a getter and a setter.

### Tool Methods

The `Tool` object has several methods that you can use to interact with the tool.

- `describeFunction( description ):Tool` : Describe the function of the tool
- `describeArg( name, description ):Tool` : Describe an argument of the tool
- `call( callable ):Tool` : Set the callable closure/lambda of the tool

### Dynamic Tool Methods

The `Tool` object also listens to dynamic methods so you can build fluent descriptions of the function or arguments using the `describe{argument}()` methods.

```java
aiTool(
	"myTool",
	( args ) -> {
		return "Hello World";
	} )
	.describe( "My Tool Function" )
	.describeName( "The name of the person" )
	.describeAge( "The age of the person" )
```

### Examples

Let's build a sample AI tool that can be used in a chat request and talk to our local runtime to get realtime weather information.

```java
tool = aiTool(
	"get_weather",
	"Get current temperature for a given location.",
	location => {
	if( location contains "Kansas City" ) {
		return "85"
	}

	if( location contains "San Salvador" ){
		return "90"
	}

	return "unknown";
}).describeLocation( "City and country e.g. Bogotá, Colombia" )

result = aiChat( "How hot is it in Kansas City? What about San Salvador? Answer with only the name of the warmer city, nothing else.", {
	tools: [ tool ],
	seed: 27
} )

println( result )
```

----

## Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://www.ortussolutions.com). Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more. If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

> "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
