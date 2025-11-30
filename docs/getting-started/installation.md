---
description: "Installation and configuration guide for the BoxLang AI module."
icon: download
---

# Installation & Configuration

Learn how to install the BoxLang AI module and configure it for your preferred AI provider.

## System Requirements

- **BoxLang Runtime**: 1.7+
- **Internet**: Required for cloud providers (OpenAI, Claude, etc.)
- **Optional**: Docker for running Ollama locally

## Installation Methods

### BoxLang Module Installer

The simplest way to install the module:

```bash
install-bx-module bx-ai
```

This command downloads and installs the module globally, making it available to all BoxLang applications on your system.

### CommandBox Package Manager

For CommandBox-based web applications:

```bash
box install bx-ai
```

This adds the module to your application's dependencies and installs it in the appropriate location.

### Application Dependencies

Add to your `box.json` for managed dependencies:

```json
{
  "name": "my-boxlang-app",
  "version": "1.0.0",
  "dependencies": {
    "bx-ai": "^1.0.0"
  }
}
```

Then run:

```bash
box install
```

## Configuration

Configure the module in your `boxlang.json` file to set defaults for your application.

### Basic Configuration

```json
{
  "modules": {
    "bxai": {
      "settings": {
        "provider": "openai",
        "apiKey": "your-api-key-here"
      }
    }
  }
}
```

### Complete Configuration

```json
{
  "modules": {
    "bxai": {
      "settings": {
        "provider": "openai",
        "apiKey": "your-api-key-here",
        "defaultParams": {
          "model": "gpt-4",
          "temperature": 0.7,
          "max_tokens": 1000
        },
        "timeout": 30,
        "logRequest": false,
        "logRequestToConsole": false,
        "logResponse": false,
        "logResponseToConsole": false,
        "returnFormat": "single"
      }
    }
  }
}
```

## Provider Configuration

### OpenAI (ChatGPT)

```json
{
  "modules": {
    "bxai": {
      "settings": {
        "provider": "openai",
        "apiKey": "sk-...",
        "defaultParams": {
          "model": "gpt-4",
          "temperature": 0.7
        }
      }
    }
  }
}
```

**Get your API key**: [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)

**Popular models**:

- `gpt-4` - Most capable, best for complex tasks
- `gpt-4-turbo` - Faster, more cost-effective
- `gpt-3.5-turbo` - Fast and affordable

### Claude (Anthropic)

```json
{
  "modules": {
    "bxai": {
      "settings": {
        "provider": "claude",
        "apiKey": "sk-ant-...",
        "defaultParams": {
          "model": "claude-3-opus-20240229",
          "max_tokens": 4096
        }
      }
    }
  }
}
```

**Get your API key**: [https://console.anthropic.com/](https://console.anthropic.com/)

**Popular models**:

- `claude-3-opus-20240229` - Most capable
- `claude-3-sonnet-20240229` - Balanced
- `claude-3-haiku-20240307` - Fastest

### Gemini (Google)

```json
{
  "modules": {
    "bxai": {
      "settings": {
        "provider": "gemini",
        "apiKey": "...",
        "defaultParams": {
          "model": "gemini-pro"
        }
      }
    }
  }
}
```

**Get your API key**: [https://makersuite.google.com/app/apikey](https://makersuite.google.com/app/apikey)

### Ollama (Local AI)

**Perfect for privacy, offline use, and zero API costs!**

#### 1. Install Ollama

Download from [https://ollama.ai](https://ollama.ai) for your platform:

- **macOS**: `brew install ollama`
- **Linux**: `curl -fsSL https://ollama.ai/install.sh | sh`
- **Windows**: Download installer from website

#### 2. Pull a Model

```bash
# Recommended general model
ollama pull llama3.2

# Smaller/faster options
ollama pull llama3.2:1b
ollama pull phi3

# Code-focused
ollama pull codellama

# High quality
ollama pull mistral
```

#### 3. Configure BoxLang

```json
{
  "modules": {
    "bxai": {
      "settings": {
        "provider": "ollama",
        "apiKey": "",
        "chatURL": "http://localhost:11434",
        "defaultParams": {
          "model": "llama3.2"
        }
      }
    }
  }
}
```

**Note**: Ollama doesn't require an API key for local use.

#### 4. Verify Installation

```bash
# Check Ollama is running
ollama list

# Test a model
ollama run llama3.2 "Hello!"
```

### Other Providers

**Grok (xAI)**:

```json
{
  "provider": "grok",
  "apiKey": "xai-..."
}
```

**Groq**:

```json
{
  "provider": "groq",
  "apiKey": "gsk_..."
}
```

**DeepSeek**:

```json
{
  "provider": "deepseek",
  "apiKey": "sk-..."
}
```

**Mistral**:

```json
{
  "provider": "mistral",
  "apiKey": "..."
}
```

**Get your API key**: [https://console.mistral.ai/](https://console.mistral.ai/)

**Popular models**:

- `mistral-small-latest` - Cost-effective, fast (default)
- `mistral-medium-latest` - Balanced performance
- `mistral-large-latest` - Most capable

**OpenRouter** (Multi-model gateway):

```json
{
  "provider": "openrouter",
  "apiKey": "sk-or-..."
}
```

**Perplexity**:

```json
{
  "provider": "perplexity",
  "apiKey": "pplx-..."
}
```

## Configuration Options

### Settings Reference

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `provider` | string | `"openai"` | Default AI provider to use |
| `apiKey` | string | `""` | API key for the provider |
| `defaultParams` | struct | `{}` | Default parameters for all requests |
| `timeout` | number | `30` | Request timeout in seconds |
| `logRequest` | boolean | `false` | Log requests to ai.log |
| `logRequestToConsole` | boolean | `false` | Log requests to console |
| `logResponse` | boolean | `false` | Log responses to ai.log |
| `logResponseToConsole` | boolean | `false` | Log responses to console |
| `returnFormat` | string | `"single"` | Default return format: `single`, `all`, `json`, `xml` or `raw` |

### Default Parameters

Common parameters you can set in `defaultParams`:

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `model` | string | The model to use | `"gpt-4"`, `"claude-3-opus"` |
| `temperature` | number | Randomness (0.0-1.0) | `0.7` |
| `max_tokens` | number | Maximum response length | `1000` |
| `top_p` | number | Nucleus sampling | `0.9` |
| `presence_penalty` | number | Reduce repetition | `0.1` |
| `frequency_penalty` | number | Encourage diversity | `0.1` |

### Environment Variables

You can use environment variables for API keys:

```json
{
  "modules": {
    "bxai": {
      "settings": {
        "provider": "openai",
        "apiKey": "${OPENAI_API_KEY}"
      }
    }
  }
}
```

Then set the environment variable:

```bash
export OPENAI_API_KEY="sk-..."
```

## Verification

Test your installation:

```java
// test-ai.bxs
answer = aiChat( "Say hello!" )
println( answer )
```

Run it:

```bash
boxlang test-ai.bxs
```

If configured correctly, you should see a response from your AI provider.

## Troubleshooting

### "No API key provided"

Make sure your API key is set in `boxlang.json` or passed directly:

```java
answer = aiChat( "Hello", {}, { provider: "openai", apiKey: "sk-..." } )
```

### "Connection timeout"

Increase the timeout setting:

```json
{
  "modules": {
    "bxai": {
      "settings": {
        "timeout": 60
      }
    }
  }
}
```

### Ollama not responding

Make sure Ollama is running:

```bash
ollama serve
```

Or check if it's already running:

```bash
curl http://localhost:11434/api/tags
```

## Next Steps

- [Quick Start Guide](quickstart.md) - Get started with simple examples
- [Basic Chatting](../chatting/basic-chatting.md) - Learn the basics
- [Provider Information](../README.md#supported-providers) - Compare features and capabilities
