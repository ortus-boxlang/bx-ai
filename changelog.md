# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### Added

- Consolidated AI request/response logging with execution time metrics for better performance insights.
- Improved AI request/response to include other metrics in order to provide better insights into performance and potential bottlenecks.

### Fixed

- Fixed a bug where provider options in the configuration file were not being merged into the request options when creating a service instance.
- Fixed a bug where the `aiService()` BIF was not correctly applying convention-based API key detection when `options.apiKey` was already set but empty. Now it checks if `options.apiKey` is empty before applying the convention key, allowing for proper fallback to environment variables or module settings.

## [2.1.0] - 2026-02-04

What's New: <https://ai.ortusbooks.com/readme/release-history/2.1.0>

### Added

- New event: `onMissingAiProvider` to handle cases where a requested provider is not found.
- `aiModel()` BIF now accepts an additional `options` struct to seed services.
- New configuration: `providers` so you can predefine multiple providers in the module config, with default `params` and `options`.

```js
"providers" : {
	"openai" : {
		"params" : {
			"model" : "gpt-4"
		},
		"options" : {
			"apiKey" : "my-openai-api-key"
		}
	},
	"ollama" : {
		"params" : {
			"model" : "qwen2.5:0.5b-instruct"
		},
		"options" : {
			"baseUrl" : "http://my-ollama-server:11434/"
		}
	}
}
```

- OllamaService now supports custom base URLs for both chat and embeddings endpoints via the `options.baseUrl` parameter.
- `AiBaseRequest.mergeServiceParams()` and `AiBaseRequest.mergeServiceHeaders()` methods now accept an `override` boolean argument to control whether existing values should be overwritten when merging.
- Local Ollama docker setup instructions updated to include the `nomic-embed-text` model for embeddings support.
- Ollama Service now supports embedding generation using the `nomic-embed-text` model.
- **Multi-Tenant Usage Tracking**: Provider-agnostic request tagging for per-tenant billing
  - New `tenantId` option for attributing AI usage to specific tenants
  - New `usageMetadata` option for custom tracking data (cost center, project, userId, etc.)
  - Enhanced `onAITokenCount` events with tenant context for interceptor-based billing
  - Works with all providers: OpenAI, Bedrock, Ollama, DeepSeek, etc.
  - Fully backward compatible - existing code works unchanged
- **Provider-Specific Options Support**: Generic `providerOptions` struct for provider-specific settings
  - New `providerOptions` option for passing provider-specific configuration (e.g., `inferenceProfileArn` for Bedrock)
  - New `getProviderOption(key, defaultValue)` method on requests for retrieving provider options
  - Enables extensibility for any provider-specific features without polluting the common interface
- **OpenSearch Vector Memory Provider**: Full integration with OpenSearch k-NN for semantic search
  - Support for OpenSearch 2.x and 3.x with automatic version detection and space type mapping
  - HNSW index configuration options (M, ef_construction, ef_search parameters)
  - Space type options: cosinesimilarity, l2, innerproduct
  - Basic authentication support (username/password)
  - AWS region configuration for SigV4 authentication with AWS OpenSearch Service
  - Multi-tenant isolation with userId and conversationId filtering
  - Comprehensive test coverage for configuration, validation, and operations
- **OpenAI-Compatible Embedding Support**: Vector memory providers now support custom embedding endpoints
  - New `embeddingOptions` configuration in `BaseVectorMemory` for passing options to embedding provider
  - Use `embeddingOptions.baseURL` for custom OpenAI-compatible embedding service URLs
  - Allows using self-hosted or alternative OpenAI-compatible embedding services
  - Works with providers like Ollama, LM Studio, and other compatible APIs
- **AWS Bedrock Streaming Support**: Full streaming support for Bedrock provider
  - Streaming via `InvokeModelWithResponseStream` API endpoint
  - Support for all model families: Claude, Titan, Llama, Mistral
  - AWS event-stream format parsing with base64 payload decoding
  - OpenAI-compatible streaming response format for consistent callback handling
  - Added more AiError exception handling for service json errors.

### Changed

- All AI provider services now inherit default chat and embedding parameters from the `IAiService` interface, ensuring consistent behavior across providers.
- `IAiService.configure()` method now accepts a generic `options` argument instead of `apiKey`, to better reflect its purpose and support more configuration options.
- `AiRequest` class renamed to `AiChatRequest` for clarity, and multi-modality support.

### Fixed

- Events for chat requests were incorrectly named in the ModuleConfig.bx file. Corrected to `onAIChatRequest`, `onAIChatRequestCreate`, and `onAIChatResponse`.
- `aiChat, aiChatStream` BIF was not passing headers to the AiChatRequest.
- `aiChat, aiChatStream, aiChatAsync` BIF was not using `aiChatRequest()` to build the request, but was building it manually.
- According to the MCP spec prompts should return a key named "arguments" not "args".
- AiRequest was not setting the model correctly from params.
- API key was not being passed to the service in `aiChat(), aiChatStream()` BIF.
- Typo of `chr()` --> `char()` in SSE formatting in MCPRequestProcessor and HTTPTransport.
- `AiModel.getModel()` was not returning the model name correctly when using predefined providers from config.
- Increased Docker Model Runner retry time to 5 seconds with 10 max retries to accommodate large model loading times
- Fixed `url` parameter conflict in OpenSearchVectorMemory by using `requestUrl` for HTTP requests

## [2.0.0] - 2026-01-19

What's New: <https://ai.ortusbooks.com/readme/release-history/2.0.0>

One of our biggest library updates yet! This release introduces a powerful new document loading system, comprehensive security features for MCP servers, and full support for several major AI providers including Mistral, HuggingFace, Groq, OpenRouter, and Ollama. Additionally, we have implemented complete embeddings functionality and made numerous enhancements and fixes across the board.

### Added

- **Document Loaders**: New document loading system for importing content from various sources
  - New `aiDocuments()` BIF for loading documents with automatic type detection
  - New `aiDocumentLoader()` BIF for creating loader instances with advanced configuration
  - New `aiDocumentLoaders()` BIF for retrieving all registered loaders with metadata
  - New `aiMemoryIngest()` BIF for ingesting documents into memory with comprehensive reporting:
    - Single memory or multi-memory fan-out support
    - Async processing for parallel ingestion
    - Automatic chunking with `aiChunk()` integration
    - Token counting with `aiTokens()` integration
    - Cost estimation for embedding operations
    - Detailed ingestion report (documentsIn, chunksOut, stored, skipped, deduped, tokenCount, embeddingCalls, estimatedCost, errors, memorySummary, duration)
  - New `Document` class for standardized document representation with content and metadata
  - New `IDocumentLoader` interface and `BaseDocumentLoader` abstract class for custom loaders
  - **Built-in Loaders**:
    - `TextLoader`: Plain text files (.txt, .text)
    - `MarkdownLoader`: Markdown files with header splitting, code block removal
    - `HTMLLoader`: HTML files and URLs with script/style removal, tag extraction
    - `CSVLoader`: CSV files with row-as-document mode, column filtering
    - `JSONLoader`: JSON files with field extraction, array-as-documents mode
    - `DirectoryLoader`: Batch loading from directories with recursive scanning
  - Fluent API for loader configuration
  - Integration with memory systems via `loadTo()` method and `aiMemoryIngest()` BIF
  - Automatic document chunking support for vector memory
  - Comprehensive documentation in `docs/main-components/document-loaders.md`
- **MCP Server Enterprise Security Features**: Comprehensive security enhancements for MCP servers
  - **CORS Configuration**:
    - `withCors(origins)` - Configure allowed origins (string or array)
    - `addCorsOrigin(origin)` - Add origin dynamically
    - `getCorsAllowedOrigins()` - Get configured origins array
    - `isCorsAllowed(origin)` - Check if origin is allowed with wildcard matching
    - Support for wildcard patterns (`*.example.com`)
    - Support for allowing all origins (`*`)
    - Dynamic `Access-Control-Allow-Origin` header in responses
    - CORS headers included in OPTIONS preflight responses
  - **Request Body Size Limits**:
    - `withBodyLimit(maxBytes)` - Set maximum request body size in bytes
    - `getMaxRequestBodySize()` - Get current limit (0 = unlimited)
    - Returns 413 Payload Too Large error when exceeded
    - Protects against DoS attacks with oversized payloads
  - **Custom API Key Validation**:
    - `withApiKeyProvider(provider)` - Set custom API key validation callback
    - `hasApiKeyProvider()` - Check if provider is configured
    - `verifyApiKey(apiKey, requestData)` - Manual key validation
    - Supports `X-API-Key` header and `Authorization: Bearer` token
    - Provider receives API key and request context for flexible validation
    - Returns 401 Unauthorized for invalid keys
  - **Security Headers**: Automatic inclusion of industry-standard security headers in all responses
    - `X-Content-Type-Options: nosniff`
    - `X-Frame-Options: DENY`
    - `X-XSS-Protection: 1; mode=block`
    - `Referrer-Policy: strict-origin-when-cross-origin`
    - `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`
    - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
    - `Permissions-Policy: geolocation=(), microphone=(), camera=()`
  - **Security Processing Order**: Body size → CORS → Basic Auth → API Key → Request processing
  - Comprehensive documentation in `docs/advanced/mcp-server.md` with examples
  - Security configuration examples in main README.md
  - 9 new integration tests covering all security features
- **Mistral AI Provider Support**: Full integration with Mistral AI services
  - New `MistralService` provider class with OpenAI-compatible API
  - Chat completions with streaming support
  - Embeddings support with `mistral-embed` model
  - Tool/function calling support
  - Default model: `mistral-small-latest`
  - API key detection via `MISTRAL_API_KEY` environment variable
  - Comprehensive integration tests
- **HuggingFace Provider Support**: Full integration with HuggingFace Inference API
  - New `HuggingFaceService` provider class extending BaseService
  - OpenAI-compatible API endpoint at `router.huggingface.co/v1`
  - Default model: `Qwen/Qwen2.5-72B-Instruct`
  - Support for chat completions and embeddings
  - Integration tests for HuggingFace provider
  - API key pattern: `HUGGINGFACE_API_KEY`
- **Groq Provider Support**: Full integration with Groq AI services for fast inference
  - Uses OpenAI-compatible API at `api.groq.com`
  - Default model: `llama-3.3-70b-versatile`
  - Support for chat completions, streaming, and embeddings
  - Environment variable: `GROQ_API_KEY`
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

[unreleased]: https://github.com/ortus-boxlang/bx-ai/compare/v2.1.0...HEAD
[2.1.0]: https://github.com/ortus-boxlang/bx-ai/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/ortus-boxlang/bx-ai/compare/v1.2.0...v2.0.0
[1.2.0]: https://github.com/ortus-boxlang/bx-ai/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ortus-boxlang/bx-ai/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/ortus-boxlang/bx-ai/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/ortus-boxlang/bx-ai/compare/75d7de99df83fbf553920bec4c601f825506820a...v1.0.0
