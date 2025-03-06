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

Welcome to the BoxLang AI Module. This module is a BoxLang module that provides AI capabilities to your BoxLang applications.  The following AI providers are supported:

- [OpenAI](https://www.openai.com/)

> More coming soon.

## Settings

Here are the settings you can place in your `boxlang.json` file:

```json
{
	"modules" : {
		"bxai" : {
			// The provider of the AI: openai, google, aws, azure, deepseek
			provider = "openai",
			// The API Key for the provider
			apiKey = "",
			// The provider model to use, if any
			model = "gpt-4o-mini",
			// The provider properties according to provider, if any
			properties = {
			}
		}
	}
}
```

## Usage

This module exposes the following BoxLang functions:

- `aiChat( messages, model, struct data={}, boolean verbose=false )` : This function will allow you to chat with the AI provider and get responses back.
- `aiChatAsync( messages, model, struct data={}, boolean verbose=false )` : This function will allow you to chat with the AI provider asynchronously and give you back a BoxLang Completable Future.

### Arguments

- `messages` : The messages to chat with the AI.  This is provider dependent. Please see each section for more information.
- `model` : The model to use for the AI provider.  This is provider dependent. Please see each section for more information.
- `data` : The data to pass to the AI provider.  This is provider dependent. Please see each section for more information.
- `verbose` : A flag to output verbose information about the AI chat or just the response message.

```js
// Chat with the AI
aiChat( "What is the meaning of life?" );
```

----

## OpenAI

The OpenAI provider will allow you to interact with the following APIs:

- Chat API - https://platform.openai.com/docs/api-reference/chat
- Image API
- Embedding API

Please see [OpenAI API](https://beta.openai.com/docs/api-reference) for more information.

### aiChat()

You can use the `aiChat()` function to chat with the OpenAI API.  Here is more docs on this: https://platform.openai.com/docs/guides/text-generation

#### Messages

This can be any of the following

- A string : A message with a default `role` of `user` will be used
- A struct : A single message that must have a `role` and a `content` key
- An array of structs : An array of messages that must have a `role` and a `content` keys

```js
// Chat with the AI
aiChat( "What is the meaning of life?" );
```

```js
// Chat with the AI
aiChat( { role="developer", content="What is the meaning of life?" } );
```

```js
// Chat with the AI
aiChat( [
	{ role="developer", content="Be a helpful assistant" },
	{ role="user", content="What is the meaning of life?" }
] );
```

#### Model

The supported models for OpenAI are:

- `gpt-4o` : The large model
- `gpt-4o-mini` : The more affordable but slower model
- `gpt-4o-turbo` : The turbo model
- Much more, look at the docs.

You can find more information here: https://platform.openai.com/docs/models

#### Data

This is an arbitrary structure that will be passed to the OpenAI API alongsside the top level body.

```js
// Chat with the AI
aiChat( "What is BoxLang?", "gpt-4o-mini", { temperature=0.5, max_tokens=100 } );
```

#### Examples

Here are some examples of chatting with the AI:

```js
aiChat( "Write a haiku about recursion in programming." );

aiChat( {
	"role": "user",
	"content": "Write a haiku about recursion in programming."
} );

aiChat( [
	{
		"role": "developer",
		"content": "You are a helpful assistant."
	},
	{
		"role": "user",
		"content": "Write a haiku about recursion in programming."
	}
] );

// Analyze an image
aiChat( [
    {
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
	}
] );
```

## Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://www.ortussolutions.com). Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more. If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

> "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
