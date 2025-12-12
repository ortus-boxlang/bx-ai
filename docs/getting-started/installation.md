---
description: "Installation and configuration guide for BoxLang AI."
icon: download
---

# 📦 Installation & Configuration

Learn how to install BoxLang AI and configure it for your preferred AI provider.

## 📑 Table of Contents

- [System Requirements](#system-requirements)
- [Installation Methods](#installation-methods)
- [Configuration](#configuration)
- [Provider Configuration](#provider-configuration)
- [Running Ollama with Docker](#running-ollama-with-docker)
- [Configuration Options](#configuration-options)
- [Verification](#verification)
- [Troubleshooting](#troubleshooting)
- [Next Steps](#next-steps)

## ⚙️ System Requirements

- **BoxLang Runtime**: 1.8+
- **Internet**: Required for cloud providers (OpenAI, Claude, etc.)
- **Optional**: Docker for running Ollama locally

## 🚀 Installation Methods

### 📥 BoxLang Module Installer

The simplest way to install the module is via the BoxLang Module Installer globally:

```bash
install-bx-module bx-ai
```

This command downloads and installs the module globally, making it available to all BoxLang applications on your system.  If you want to install it locally in your cli or other runtimes:

```bash
install-bx-module bx-ai --local
```

### 📦 CommandBox Package Manager

For CommandBox-based web applications and runtimes

```bash
box install bx-ai
```

This adds the module to your application's dependencies and installs it in the appropriate location.

### 📋 Application Dependencies

Add to your `box.json` for managed dependencies:

```json
{
  "name": "my-boxlang-app",
  "version": "1.0.0",
  "dependencies": {
    "bx-ai": "^2"
  }
}
```

Then run:

```bash
box install
```

## 🔧 Configuration

Configure the module in your `boxlang.json` file to set defaults for your application.

### ⚡ Basic Configuration

```json
{
  "modules": {
    "bxai": {
      "settings": {
		// The default provider
        "provider": "openai",
		// The default API Key for the Default Provider
        "apiKey": "your-api-key-here"
      }
    }
  }
}
```

### 🎯 Complete Configuration

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

## 🤖 Provider Configuration

### 🟢 OpenAI (ChatGPT)

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

- `gpt-5` - Latest and most advanced
- `gpt-4` - Most capable, best for complex tasks
- `gpt-4-turbo` - Faster, more cost-effective
- `gpt-3.5-turbo` - Fast and affordable

### 🟣 Claude (Anthropic)

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

### 🔵 Gemini (Google)

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

### 🦙 Ollama (Local AI)

**Perfect for privacy, offline use, and zero API costs!**

#### 1️⃣ Install Ollama

Download from [https://ollama.ai](https://ollama.ai) for your platform:

- **macOS**: `brew install ollama`
- **Linux**: `curl -fsSL https://ollama.ai/install.sh | sh`
- **Windows**: Download installer from website

#### 2️⃣ Pull a Model

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

#### 3️⃣ Configure BoxLang

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

#### 4️⃣ Verify Installation

```bash
# Check Ollama is running
ollama list

# Test a model
ollama run llama3.2 "Hello!"
```

## 🐳 Running Ollama with Docker

For production deployments or easier setup, use the included Docker Compose configuration:

### 📋 Quick Start

```bash
  "apiKey": "xai-..."
}
```

**🤗 HuggingFace**:
- **Ollama Server** on `http://localhost:11434`
- **Web UI** on `http://localhost:3000`

### 🎯 What's Included

The Docker setup provides:

- ✅ **Ollama LLM Server** - Fully configured and ready to use
- ✅ **Web UI** - Browser-based interface for testing and management
- ✅ **Pre-loaded Model** - Automatically downloads `qwen2.5:0.5b-instruct`
- ✅ **Health Checks** - Automatic monitoring and restart capabilities
- ✅ **Persistent Storage** - Data stored locally in `./.ollama` directory
- ✅ **Production Ready** - Configured with proper restart policies

- `mistralai/Mistral-7B-Instruct-v0.3` - Fast and efficient for quick responses

**⚡ Groq**:deploying to production, update these settings in `docker-compose-ollama.yml`:**

1. **Change Default Credentials**

```yaml
   environment:
  "apiKey": "gsk_..."
}
```

**🔷 DeepSeek**:e Models** - Update the preloaded model:
   ```yaml
   command: |
     "ollama serve &
     sleep 10
  "apiKey": "sk-..."
}
```

**🟠 Mistral**:
1. **Add Resource Limits** (recommended):
   ```yaml
   deploy:
     resources:
       limits:
         memory: 8g
         cpus: '4.0'
   ```

2. **SSL/TLS** - Use a reverse proxy (nginx/traefik) for HTTPS

See the comments in `docker-compose-ollama.yml` for complete production setup notes.

### 📊 Managing the Service

- `mistral-large-latest` - Most capable

**🌐 OpenRouter** (Multi-model gateway):
docker compose -f docker-compose-ollama.yml up -d

# View logs

docker compose -f docker-compose-ollama.yml logs -f
  "apiKey": "sk-or-..."
}
```

**🔎 Perplexity**:ces
docker compose -f docker-compose-ollama.yml restart

# Check status
docker compose -f docker-compose-ollama.yml ps
```
}
```

## ⚙️ Configuration Options

### 📊 Settings Reference
{
  "modules": {
    "bxai": {
      "settings": {
        "provider": "ollama",
        "apiKey": "",
        "chatURL": "http://localhost:11434",
        "defaultParams": {
          "model": "qwen2.5:0.5b-instruct"
        }
      }
| `returnFormat` | string | `"single"` | Default return format: `single`, `all`, `json`, `xml` or `raw` |

### 🎛️ Default Parameters
```

### 🌐 Accessing the Web UI

Open your browser to `http://localhost:3000` and login with:
- **Username**: `boxlang` (default)
- **Password**: `rocks` (default)

**⚠️ Change these credentials before production use!**

| `frequency_penalty` | number | Encourage diversity | `0.1` |

### 🔐 Environment Variablesconfigurations) is stored in `./.ollama` directory:

```bash
.ollama/
├── server/     # Ollama model data
└── webui/      # Web UI data and settings
```

**Important**: Add `.ollama/` to your `.gitignore` to avoid committing large model files.

### 🔧 Other Providers

**🔸 Grok (xAI)**:

```json
{
  "provider": "grok",
  "apiKey": "xai-..."
}
```
export OPENAI_API_KEY="sk-..."
```

## ✅ Verification
{
  "provider": "huggingface",
  "apiKey": "hf_..."
}
```

**Get your API key**: [https://huggingface.co/settings/tokens](https://huggingface.co/settings/tokens)

**Popular models**:

- `Qwen/Qwen2.5-72B-Instruct` - Default, powerful general-purpose model for complex reasoning
- `meta-llama/Llama-3.1-8B-Instruct` - Meta's Llama model, balanced performance and speed
- `mistralai/Mistral-7B-Instruct-v0.3` - Fast and efficient for quick responses

**Groq**:
If configured correctly, you should see a response from your AI provider.

## 🔧 Troubleshooting

### ❌ "No API key provided"
}
```

**DeepSeek**:
answer = aiChat( "Hello", {}, { provider: "openai", apiKey: "sk-..." } )
```

### ⏱️ "Connection timeout",
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
}
```

### 🦙 Ollama not responding

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

## 🚀 Next Steps

- [Quick Start Guide](quickstart.md) - Get started with simple examples
- [Basic Chatting](../chatting/basic-chatting.md) - Learn the basics
- [Provider Information](../README.md#supported-providers) - Compare features and capabilities
