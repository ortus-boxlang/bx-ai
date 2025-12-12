---
description: Reference documentation for BoxLang AI models - hierarchical overview of all model classes
icon: books
---

# Models Reference

This reference provides comprehensive documentation for all model classes in the BoxLang AI module. The classes are organized hierarchically by their architectural role and functional area.

## 📋 Architecture Overview

The BoxLang AI module follows a layered architecture:

1. **Interfaces** - Contracts defining core behaviors
2. **Abstract Base Classes** - Reusable implementations of common functionality
3. **Concrete Classes** - Specific implementations and utilities
4. **Provider Implementations** - AI service integrations
5. **Memory Implementations** - Conversation storage solutions
6. **Document Loaders** - Data ingestion from various sources
7. **Transformers** - Data transformation pipelines

---

## 🔌 Core Interfaces

Fundamental contracts that define the module's behavior.

### Runnable System
- **[IAiRunnable](./interfaces/IAiRunnable.md)** - Base interface for all runnable components (run, stream, to, getName)

### Service System
- **[IAiService](./interfaces/IAiService.md)** - Contract for AI provider implementations (configure, invoke, invokeStream, getName, getDefaults)
- **[ITool](./interfaces/ITool.md)** - Function calling interface for real-time tool execution (execute, getMetadata, validate)

### Memory System
- **[IAiMemory](./interfaces/IAiMemory.md)** - Conversation storage interface (add, get, clear, getHistory, etc.)
- **[IVectorMemory](./interfaces/IVectorMemory.md)** - Semantic search interface extending IAiMemory (search, embed, addWithEmbedding, reindex)

### Document System
- **[IDocumentLoader](./interfaces/IDocumentLoader.md)** - Document loading interface (load, supports, configure)

### Transformation System
- **[ITransformer](./interfaces/ITransformer.md)** - Data transformation interface (transform, configure)

---

## 🏗️ Abstract Base Classes

Reusable implementations providing common functionality.

### Runnable Foundation
- **[AiBaseRunnable](./base/AiBaseRunnable.md)** - Base class for all runnables (28 methods)
  - Implements: `IAiRunnable`
  - Provides: Pipeline chaining, error handling, metadata management
  - Extended by: AiAgent, AiMessage, AiModel, AiRunnableSequence, AiTransformRunnable

### Service Foundation
- **[BaseService](./base/BaseService.md)** - OpenAI-compatible provider base (20+ methods)
  - Implements: `IAiService`
  - Provides: HTTP client, request/response handling, streaming support
  - Extended by: All 13 AI provider implementations

### Memory Foundation
- **[BaseMemory](./base/BaseMemory.md)** - Common memory functionality (15+ methods)
  - Implements: `IAiMemory`
  - Provides: Message storage, summarization, filtering
  - Extended by: CacheMemory, FileMemory, SessionMemory, JdbcMemory, WindowMemory, SummaryMemory, HybridMemory

- **[BaseVectorMemory](./base/BaseVectorMemory.md)** - Semantic search with embeddings (20+ methods)
  - Extends: `BaseMemory`
  - Implements: `IVectorMemory`
  - Provides: Vector storage, similarity search, embedding generation
  - Extended by: All 9 vector store implementations

### Document Foundation
- **[BaseDocumentLoader](./base/BaseDocumentLoader.md)** - Common document loading (30+ methods)
  - Implements: `IDocumentLoader`
  - Provides: File detection, metadata extraction, recursive scanning
  - Extended by: All 12 document loader implementations

### Transformation Foundation
- **[BaseTransformer](./base/BaseTransformer.md)** - Common transformer functionality
  - Implements: `ITransformer`
  - Provides: Configuration management, validation
  - Extended by: CodeExtractorTransformer, JSONExtractorTransformer, XMLExtractorTransformer, TextCleanerTransformer

---

## 🎯 Core Concrete Classes

Primary classes for building AI applications.

### Agent & Orchestration
- **[AiAgent](./core/AiAgent.md)** - Autonomous agents with tools and memory
  - Extends: `AiBaseRunnable`
  - Features: Multi-step reasoning, tool execution, sub-agents, memory integration

### Message Building
- **[AiMessage](./core/AiMessage.md)** - Fluent message builder (30+ methods)
  - Extends: `AiBaseRunnable`
  - Features: Role-based messages (user, assistant, system, tool), multimodal content

### Model Integration
- **[AiModel](./core/AiModel.md)** - AI service wrapper for pipelines
  - Extends: `AiBaseRunnable`
  - Features: Provider abstraction, request building, streaming support

### Pipeline Management
- **[AiRunnableSequence](./core/AiRunnableSequence.md)** - Chains multiple runnables
  - Extends: `AiBaseRunnable`
  - Features: Sequential execution, state passing, error propagation

### Tool System
- **[Tool](./core/Tool.md)** - Function calling implementation
  - Implements: `ITool`
  - Features: Parameter validation, metadata management, execution handling

### Document Management
- **[Document](./core/Document.md)** - Document with metadata and embeddings (30+ methods)
  - Features: Content management, embedding storage, metadata handling

### Transformation
- **[AiTransformRunnable](./core/AiTransformRunnable.md)** - Lambda wrapper for transformations
  - Extends: `AiBaseRunnable`
  - Features: Inline transformations, pipeline integration

---

## 📦 Request Classes

Objects for building and managing AI requests.

- **[AiRequest](./requests/AiRequest.md)** - Primary request object with validation
- **[AiBaseRequest](./requests/AiBaseRequest.md)** - Base request functionality
- **[AiEmbeddingRequest](./requests/AiEmbeddingRequest.md)** - Embedding-specific requests

---

## 🤖 AI Provider Implementations

13 AI service integrations, all extending `BaseService`.

### OpenAI Ecosystem
- **[OpenAIService](./providers/OpenAIService.md)** - OpenAI (GPT-4, GPT-3.5, etc.)
- **[OpenRouterService](./providers/OpenRouterService.md)** - OpenRouter (unified API)

### Claude & Anthropic
- **[ClaudeService](./providers/ClaudeService.md)** - Claude (Opus, Sonnet, Haiku)

### Google
- **[GeminiService](./providers/GeminiService.md)** - Gemini (Pro, Flash, etc.)

### Open Source / Local
- **[OllamaService](./providers/OllamaService.md)** - Ollama (local models)
- **[HuggingFaceService](./providers/HuggingFaceService.md)** - Hugging Face models

### Specialized Providers
- **[GroqService](./providers/GroqService.md)** - Groq (high-speed inference)
- **[MistralService](./providers/MistralService.md)** - Mistral AI
- **[CohereService](./providers/CohereService.md)** - Cohere (embeddings, chat)
- **[PerplexityService](./providers/PerplexityService.md)** - Perplexity (research)
- **[DeepSeekService](./providers/DeepSeekService.md)** - DeepSeek
- **[GrokService](./providers/GrokService.md)** - Grok (xAI)

### Embedding Services
- **[VoyageService](./providers/VoyageService.md)** - Voyage AI (embeddings)

---

## 💾 Memory Implementations

16 memory types for conversation storage.

### Standard Memory (extends BaseMemory)

- **[CacheMemory](./memory/CacheMemory.md)** - In-memory cache storage
- **[FileMemory](./memory/FileMemory.md)** - File-based persistence
- **[SessionMemory](./memory/SessionMemory.md)** - Session-scoped storage
- **[JdbcMemory](./memory/JdbcMemory.md)** - Database storage via JDBC
- **[WindowMemory](./memory/WindowMemory.md)** - Sliding window (last N messages)
- **[SummaryMemory](./memory/SummaryMemory.md)** - Automatic summarization
- **[HybridMemory](./memory/HybridMemory.md)** - Combines multiple memory types

### Vector Memory (extends BaseVectorMemory)

Semantic search with embeddings:

- **[BoxVectorMemory](./memory/BoxVectorMemory.md)** - Built-in vector storage
- **[ChromaVectorMemory](./memory/ChromaVectorMemory.md)** - Chroma integration
- **[PineconeVectorMemory](./memory/PineconeVectorMemory.md)** - Pinecone cloud
- **[QdrantVectorMemory](./memory/QdrantVectorMemory.md)** - Qdrant vector DB
- **[WeaviateVectorMemory](./memory/WeaviateVectorMemory.md)** - Weaviate integration
- **[MilvusVectorMemory](./memory/MilvusVectorMemory.md)** - Milvus vector DB
- **[TypesenseVectorMemory](./memory/TypesenseVectorMemory.md)** - Typesense search
- **[PostgresVectorMemory](./memory/PostgresVectorMemory.md)** - PostgreSQL with pgvector
- **[MysqlVectorMemory](./memory/MysqlVectorMemory.md)** - MySQL vector support

---

## 📄 Document Loaders

12 loaders for ingesting data from various sources (all extend `BaseDocumentLoader`).

### Text & Markup
- **[TextLoader](./loaders/TextLoader.md)** - Plain text files
- **[MarkdownLoader](./loaders/MarkdownLoader.md)** - Markdown with metadata
- **[PDFLoader](./loaders/PDFLoader.md)** - PDF extraction

### Structured Data
- **[CSVLoader](./loaders/CSVLoader.md)** - CSV files
- **[JSONLoader](./loaders/JSONLoader.md)** - JSON data
- **[XMLLoader](./loaders/XMLLoader.md)** - XML documents

### Web Content
- **[HTTPLoader](./loaders/HTTPLoader.md)** - Single URL fetching
- **[WebCrawlerLoader](./loaders/WebCrawlerLoader.md)** - Multi-page crawling
- **[FeedLoader](./loaders/FeedLoader.md)** - RSS/Atom feeds

### File Systems & Databases
- **[DirectoryLoader](./loaders/DirectoryLoader.md)** - Recursive directory scanning
- **[SQLLoader](./loaders/SQLLoader.md)** - Database query results
- **[LogLoader](./loaders/LogLoader.md)** - Log file parsing

---

## 🔄 Transformers

Data transformation pipeline components (all extend `BaseTransformer`).

- **[CodeExtractorTransformer](./transformers/CodeExtractorTransformer.md)** - Extract code blocks from markdown
- **[JSONExtractorTransformer](./transformers/JSONExtractorTransformer.md)** - Parse and validate JSON
- **[XMLExtractorTransformer](./transformers/XMLExtractorTransformer.md)** - Parse XML with XPath
- **[TextCleanerTransformer](./transformers/TextCleanerTransformer.md)** - Clean and normalize text

---

## 🛠️ Utility Classes

Helper classes providing static methods.

- **[SchemaBuilder](./util/SchemaBuilder.md)** - Generate JSON schemas for structured output
- **[TextChunker](./util/TextChunker.md)** - Split text with multiple strategies (character, token, sentence, paragraph, semantic)
- **[TokenCounter](./util/TokenCounter.md)** - Estimate token counts for models

---

## 🔌 Model Context Protocol (MCP)

Server and client implementations for MCP integration.

### Core MCP
- **[MCPServer](./mcp/MCPServer.md)** - MCP server for exposing tools
- **[MCPClient](./mcp/MCPClient.md)** - MCP client for consuming tools
- **[MCPRequestProcessor](./mcp/MCPRequestProcessor.md)** - Request handling
- **[MCPResponse](./mcp/MCPResponse.md)** - Response formatting
- **[MCPServerStats](./mcp/MCPServerStats.md)** - Server statistics

### Transport Layer
- **[ITransport](./mcp/transport/ITransport.md)** - Transport interface
- **[BaseTransport](./mcp/transport/BaseTransport.md)** - Common transport functionality
- **[StdioTransport](./mcp/transport/StdioTransport.md)** - Standard I/O transport
- **[HTTPTransport](./mcp/transport/HTTPTransport.md)** - HTTP transport

---

## 📊 Class Hierarchy Diagram

```
IAiRunnable
├── AiBaseRunnable (abstract)
│   ├── AiAgent
│   ├── AiMessage
│   ├── AiModel
│   ├── AiRunnableSequence
│   └── AiTransformRunnable

IAiService
└── BaseService (abstract)
    ├── OpenAIService
    ├── ClaudeService
    ├── GeminiService
    ├── OllamaService
    ├── GroqService
    ├── MistralService
    ├── CohereService
    ├── HuggingFaceService
    ├── OpenRouterService
    ├── PerplexityService
    ├── DeepSeekService
    ├── GrokService
    └── VoyageService

IAiMemory
├── BaseMemory (abstract)
│   ├── CacheMemory
│   ├── FileMemory
│   ├── SessionMemory
│   ├── JdbcMemory
│   ├── WindowMemory
│   ├── SummaryMemory
│   └── HybridMemory
└── IVectorMemory
    └── BaseVectorMemory (abstract)
        ├── BoxVectorMemory
        ├── ChromaVectorMemory
        ├── PineconeVectorMemory
        ├── QdrantVectorMemory
        ├── WeaviateVectorMemory
        ├── MilvusVectorMemory
        ├── TypesenseVectorMemory
        ├── PostgresVectorMemory
        └── MysqlVectorMemory

IDocumentLoader
└── BaseDocumentLoader (abstract)
    ├── TextLoader
    ├── MarkdownLoader
    ├── PDFLoader
    ├── CSVLoader
    ├── JSONLoader
    ├── XMLLoader
    ├── HTTPLoader
    ├── WebCrawlerLoader
    ├── DirectoryLoader
    ├── SQLLoader
    ├── LogLoader
    └── FeedLoader

ITransformer
└── BaseTransformer (abstract)
    ├── CodeExtractorTransformer
    ├── JSONExtractorTransformer
    ├── XMLExtractorTransformer
    └── TextCleanerTransformer

ITool
└── Tool

Standalone Classes
├── Document
├── AiRequest
├── AiBaseRequest
├── AiEmbeddingRequest
├── SchemaBuilder (utility)
├── TextChunker (utility)
└── TokenCounter (utility)
```

---

## 🚀 Quick Navigation

### By Use Case

**Building Conversational AI:**
- Start with [AiModel](./core/AiModel.md) for basic chat
- Add [AiMessage](./core/AiMessage.md) for structured conversations
- Use [IAiMemory](./interfaces/IAiMemory.md) implementations for persistence

**Creating Autonomous Agents:**
- Use [AiAgent](./core/AiAgent.md) for autonomous behavior
- Implement [ITool](./interfaces/ITool.md) for custom tools
- Combine with [IVectorMemory](./interfaces/IVectorMemory.md) for RAG

**RAG (Retrieval Augmented Generation):**
- Load data with [IDocumentLoader](./interfaces/IDocumentLoader.md) implementations
- Transform with [ITransformer](./interfaces/ITransformer.md) implementations
- Store in [IVectorMemory](./interfaces/IVectorMemory.md) for semantic search
- Use [TextChunker](./util/TextChunker.md) for optimal chunk sizes

**Building Pipelines:**
- Chain with [IAiRunnable](./interfaces/IAiRunnable.md) `.to()` method
- Use [AiRunnableSequence](./core/AiRunnableSequence.md) for complex flows
- Add [AiTransformRunnable](./core/AiTransformRunnable.md) for data transformations

---

## 📖 Documentation Conventions

All model documentation follows these conventions:

- **Public methods only** - Internal/private methods not documented
- **Static methods** - Utility class methods documented
- **Constructors** - All constructors with parameter details
- **Inheritance** - Clear hierarchy and inherited functionality
- **Examples** - Practical usage examples for each method
- **Return types** - Explicit return type documentation
- **Parameters** - Full parameter tables with types and descriptions

---

## 🔍 Index

### Interfaces
[IAiRunnable](./interfaces/IAiRunnable.md) | [IAiService](./interfaces/IAiService.md) | [ITool](./interfaces/ITool.md) | [IAiMemory](./interfaces/IAiMemory.md) | [IVectorMemory](./interfaces/IVectorMemory.md) | [IDocumentLoader](./interfaces/IDocumentLoader.md) | [ITransformer](./interfaces/ITransformer.md)

### Abstract Bases
[AiBaseRunnable](./base/AiBaseRunnable.md) | [BaseService](./base/BaseService.md) | [BaseMemory](./base/BaseMemory.md) | [BaseVectorMemory](./base/BaseVectorMemory.md) | [BaseDocumentLoader](./base/BaseDocumentLoader.md) | [BaseTransformer](./base/BaseTransformer.md)

### Core Classes
[AiAgent](./core/AiAgent.md) | [AiMessage](./core/AiMessage.md) | [AiModel](./core/AiModel.md) | [AiRunnableSequence](./core/AiRunnableSequence.md) | [Tool](./core/Tool.md) | [Document](./core/Document.md) | [AiTransformRunnable](./core/AiTransformRunnable.md)

### Providers (A-Z)
[ClaudeService](./providers/ClaudeService.md) | [CohereService](./providers/CohereService.md) | [DeepSeekService](./providers/DeepSeekService.md) | [GeminiService](./providers/GeminiService.md) | [GrokService](./providers/GrokService.md) | [GroqService](./providers/GroqService.md) | [HuggingFaceService](./providers/HuggingFaceService.md) | [MistralService](./providers/MistralService.md) | [OllamaService](./providers/OllamaService.md) | [OpenAIService](./providers/OpenAIService.md) | [OpenRouterService](./providers/OpenRouterService.md) | [PerplexityService](./providers/PerplexityService.md) | [VoyageService](./providers/VoyageService.md)

### Memory Types
[BoxVectorMemory](./memory/BoxVectorMemory.md) | [CacheMemory](./memory/CacheMemory.md) | [ChromaVectorMemory](./memory/ChromaVectorMemory.md) | [FileMemory](./memory/FileMemory.md) | [HybridMemory](./memory/HybridMemory.md) | [JdbcMemory](./memory/JdbcMemory.md) | [MilvusVectorMemory](./memory/MilvusVectorMemory.md) | [MysqlVectorMemory](./memory/MysqlVectorMemory.md) | [PineconeVectorMemory](./memory/PineconeVectorMemory.md) | [PostgresVectorMemory](./memory/PostgresVectorMemory.md) | [QdrantVectorMemory](./memory/QdrantVectorMemory.md) | [SessionMemory](./memory/SessionMemory.md) | [SummaryMemory](./memory/SummaryMemory.md) | [TypesenseVectorMemory](./memory/TypesenseVectorMemory.md) | [WeaviateVectorMemory](./memory/WeaviateVectorMemory.md) | [WindowMemory](./memory/WindowMemory.md)

### Document Loaders
[CSVLoader](./loaders/CSVLoader.md) | [DirectoryLoader](./loaders/DirectoryLoader.md) | [FeedLoader](./loaders/FeedLoader.md) | [HTTPLoader](./loaders/HTTPLoader.md) | [JSONLoader](./loaders/JSONLoader.md) | [LogLoader](./loaders/LogLoader.md) | [MarkdownLoader](./loaders/MarkdownLoader.md) | [PDFLoader](./loaders/PDFLoader.md) | [SQLLoader](./loaders/SQLLoader.md) | [TextLoader](./loaders/TextLoader.md) | [WebCrawlerLoader](./loaders/WebCrawlerLoader.md) | [XMLLoader](./loaders/XMLLoader.md)

### Transformers
[CodeExtractorTransformer](./transformers/CodeExtractorTransformer.md) | [JSONExtractorTransformer](./transformers/JSONExtractorTransformer.md) | [TextCleanerTransformer](./transformers/TextCleanerTransformer.md) | [XMLExtractorTransformer](./transformers/XMLExtractorTransformer.md)

### Utilities
[SchemaBuilder](./util/SchemaBuilder.md) | [TextChunker](./util/TextChunker.md) | [TokenCounter](./util/TokenCounter.md)

### MCP
[MCPClient](./mcp/MCPClient.md) | [MCPRequestProcessor](./mcp/MCPRequestProcessor.md) | [MCPResponse](./mcp/MCPResponse.md) | [MCPServer](./mcp/MCPServer.md) | [MCPServerStats](./mcp/MCPServerStats.md) | [ITransport](./mcp/transport/ITransport.md) | [BaseTransport](./mcp/transport/BaseTransport.md) | [StdioTransport](./mcp/transport/StdioTransport.md) | [HTTPTransport](./mcp/transport/HTTPTransport.md)
