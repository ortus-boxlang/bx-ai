# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### Added

- **HuggingFace Provider Support**: Full integration with HuggingFace Inference API
    - New `HuggingFaceService` provider class extending BaseService
    - OpenAI-compatible API endpoint at `router.huggingface.co/v1`
    - Default model: `Qwen/Qwen2.5-72B-Instruct`
    - Support for chat completions and embeddings
    - Integration tests for HuggingFace provider
    - API key pattern: `HUGGINGFACE_API_KEY`
- **Embeddings Support**: Complete embeddings functionality for semantic search, clustering, and recommendations
    - New `aiEmbedding()` BIF for generating text embeddings
    - New `AiEmbeddingRequest` class to model embedding requests
    - New `embeddings()` method in `IAiService` interface
    - Support for single text and batch text embedding generation
    - Multiple return formats: raw, embeddings, first
    - **Provider Support**:
        - OpenAI: `text-embedding-3-small` and `text-embedding-3-large` models
        - Ollama: Local embeddings for privacy-sensitive use cases
        - DeepSeek: OpenAI-compatible embeddings API
        - Grok: OpenAI-compatible embeddings API
        - OpenRouter: Aggregated embeddings via multiple models
        - Gemini: Custom implementation with `text-embedding-004` model
    - New embedding-specific events: `onAIEmbeddingRequest`, `onAIEmbeddingResponse`, `beforeAIEmbedding`, `afterAIEmbedding`
    - Comprehensive embeddings documentation in README with examples
    - New `examples/embeddings-example.bx` demonstrating practical use cases
    - Integration tests for embeddings functionality
- ChatMessage now has the following new methods:
    - `format(bindings)` - Formats messages with provided bindings.
    - `render()` - Renders messages using stored bindings.
    - `bind( bindings )` - Binds variables to be used in message formatting.
    - `getBindings(), setBindings( bindings )` - Getters and setters for bindings.
- Detect API Keys by convention in `AIService()` BIF: `<PROVIDER>_API_KEY` from system settings
- **OpenRouter Provider Support**: Full integration with OpenRouter AI services
- Automatic JSON serialization for tool calls that don't return strings
- **Ollama Provider Support**: Complete integration with Ollama for local AI model execution
- **Comprehensive Provider Test Suite**: Individual test files for each AI provider
- **Streaming Support Validation**: Verified aiChatStream() functionality across all providers
- **Docker Compose Testing Infrastructure**: Automated local development and CI/CD support
- **Enhanced GitHub Actions Workflow**: Improved CI/CD pipeline with AI service support
- **BIF Reference Documentation**: Complete function reference table in README
- **Comprehensive Event Documentation**: Complete event system documentation

### Fixed

- If a tool argument doesn't have a description, it would cause an error when generating the schema. Default it to the argument name.
- **Model Name Compatibility**: Updated OllamaService default model from llama3.2 to qwen2.5:0.5b-instruct
- **Docker GPU Support**: Made GPU configuration optional in docker-compose.yml for systems without GPU access
- **Test Model References**: Corrected model names in Ollama tests to match available models

## [1.2.0] - 2025-06-19

### Added

- New gradle wrapper and build system
- New `Tool.getArgumentsSchema()` method to retrieve the arguments schema for use by any provider.
- New logging params for console debugging: `logRequestToConsole`, `logResponseToConsole`
- Tool support for Claude LLMs
- Tool message for open ai tools when no local tools are available.
- New `ChatMessage` helper method: `getNonSystemMessages()` to retrieve all messages except the system message.
- `ChatRequest` now has the original `ChatMessage` as a property, so you can access the original message in the request.
- Latest Claude Sonnet model support: `claude-sonnet-4-0` as its default.
- Streamline of env on tests
- Added to the config the following options: `logRequest`, `logResponse`, `timeout`, `returnFormat`, so you can control the behavior of the services globally.
- Some compatibilities so it can be used in CFML apps.
- Ability for AI responses to be influenced by the `onAIResponse` event.

### Fixed

- Version pinned to `1.0.0` in the `box.json` file by accident.

## [1.1.0] - 2025-05-17

### Added

- Claude LLM Support
- Ability for the services to pre-seed params into chat requests
- Ability for the services to pre-seed headers into chat requests
- Error logging for the services

### Fixed

- Custom headers could not be added due to closure encapsulation

## [1.0.1] - 2025-03-21

### Fixed

- Missing the `settings` in the module config.
- Invalid name for the module config.

## [1.0.0] - 2025-03-17

- First iteration of this module

[Unreleased]: https://github.com/ortus-boxlang/bx-ai/compare/v1.2.0...HEAD

[1.2.0]: https://github.com/ortus-boxlang/bx-ai/compare/v1.1.0...v1.2.0

[1.1.0]: https://github.com/ortus-boxlang/bx-ai/compare/v1.0.1...v1.1.0

[1.0.1]: https://github.com/ortus-boxlang/bx-ai/compare/v1.0.0...v1.0.1

[1.0.0]: https://github.com/ortus-boxlang/bx-ai/compare/75d7de99df83fbf553920bec4c601f825506820a...v1.0.0
