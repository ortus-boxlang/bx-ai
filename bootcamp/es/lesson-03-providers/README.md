# Lección 3: Cambiando Proveedores

**⏱️ Duración: 45 minutos**

BoxLang AI te permite trabajar con múltiples proveedores de IA a través de una API unificada. ¡Esto significa que puedes cambiar entre OpenAI, Claude, Ollama y más sin cambiar tu código!

## 🎯 Lo que Aprenderás

- Entender diferentes proveedores de IA y sus fortalezas
- Cambiar proveedores en tiempo de ejecución
- Usar IA local con Ollama (¡gratis!)
- Implementar fallbacks de proveedores

---

## 📚 Parte 1: Entendiendo los Proveedores (10 mins)

### ¿Qué es un Proveedor?

Un proveedor es una empresa (o software) que ejecuta modelos de IA:

```
┌─────────────────────────────────────────────────────────────────┐
│                    PANORAMA DE PROVEEDORES DE IA                │
└─────────────────────────────────────────────────────────────────┘

  ☁️ PROVEEDORES EN LA NUBE (Pago)         🏠 LOCAL (Gratis)
  ───────────────────────────              ───────────────

  ┌──────────┐  ┌──────────┐             ┌──────────┐
  │  OpenAI  │  │  Claude  │             │  Ollama  │
  │  (GPT-4) │  │(Anthropic│             │ (Local)  │
  └──────────┘  └──────────┘             └──────────┘
       │             │                         │
       │             │                         │
       ▼             ▼                         ▼
  ┌──────────┐  ┌──────────┐             ┌──────────┐
  │  Gemini  │  │   Grok   │             │ ¡Tu PC!  │
  │ (Google) │  │   (xAI)  │             │ Privado  │
  └──────────┘  └──────────┘             └──────────┘
```

### Comparación de Proveedores

**Proveedores de Chat:**

| Proveedor | Fortalezas | Mejor Para | Costo |
|-----------|-----------|-----------|-------|
| **OpenAI** | Más capaz, mejores herramientas | Apps de producción | $$ |
| **Claude** | Contexto largo, razonamiento | Análisis complejo | $$ |
| **Gemini** | Multimodal (imágenes/video) | Tareas visuales | $ |
| **Mistral** | Rápido, eficiente, multilingüe | Apps europeas, bajo costo | $ |
| **HuggingFace** | Modelos de código abierto | Personalización, investigación | Gratis/$ |
| **Groq** | Ultra-rápido (inferencia de hardware) | Chat en tiempo real | $ |
| **DeepSeek** | Especializado en código | Generación/análisis de código | $ |
| **Perplexity** | Búsqueda + IA | Investigación, información actual | $$ |
| **OpenRouter** | Acceso a múltiples modelos | Experimentación, fallbacks | $ |
| **Ollama** | Privado, sin internet | Desarrollo, privacidad | Gratis |
| **Grok** | Datos en tiempo real | Eventos actuales | $$ |

**Proveedores de Embeddings:**

| Proveedor | Modelos | Mejor Para | Costo |
|-----------|---------|-----------|-------|
| **OpenAI** | text-embedding-3-small/large | Propósito general | $ |
| **Voyage** | voyage-3, voyage-code-3 | RAG, búsqueda de código | $$ |
| **Cohere** | embed-english-v3.0, multilingual | Apps multilingües | $ |
| **Ollama** | nomic-embed-text, mxbai-embed | Local, privado | Gratis |
| **Gemini** | gemini-embedding-001 | Integración con Google | $ |

### ¿Por Qué Cambiar de Proveedor?

1. **Costo** - Usa modelos más baratos para tareas simples
2. **Privacidad** - Ejecuta localmente con Ollama
3. **Capacidades** - Diferentes modelos destacan en diferentes cosas
4. **Fallback** - Si uno falla, prueba otro
5. **Velocidad** - Algunos son más rápidos que otros

---

## 💻 Parte 2: Usando Diferentes Proveedores (15 mins)

### Método 1: Parámetro de Opciones

Pasa el proveedor en el struct de opciones:

```java
// llamada-openai.bxs
respuesta = aiChat(
    "¿Cuánto es 2 + 2?",
    {},                        // params
    { provider: "openai" }     // opciones
)
println( "OpenAI: " & respuesta )
```

### Método 2: Con Clave de API

Algunos proveedores necesitan claves de API específicas:

```java
// llamada-claude.bxs
respuesta = aiChat(
    "¿Cuánto es 2 + 2?",
    {},
    {
        provider: "claude",
        apiKey: getSystemSetting( "CLAUDE_API_KEY" )
    }
)
println( "Claude: " & respuesta )
```

### Método 3: Usando aiService()

Crea un servicio reutilizable:

```java
// ejemplo-servicio.bxs
servicioOpenai = aiService( "openai" )
servicioClaude = aiService( "claude" )

// Misma pregunta, diferentes proveedores
pregunta = aiMessage().user( "¿Qué es BoxLang?" )

respuestaOpenai = servicioOpenai.invoke(
    aiChatRequest( pregunta )
)
respuestaClaude = servicioClaude.invoke(
    aiChatRequest( pregunta )
)
```

### Referencia Rápida de Proveedores

**Proveedores de Chat:**

```java
// OpenAI
aiChat( "mensaje", {}, { provider: "openai" } )

// Claude
aiChat( "mensaje", {}, { provider: "claude" } )

// Gemini
aiChat( "mensaje", {}, { provider: "gemini" } )

// Mistral
aiChat( "mensaje", { model: "mistral-large-latest" }, { provider: "mistral" } )

// HuggingFace
aiChat( "mensaje", { model: "meta-llama/Llama-3-8b" }, { provider: "huggingface" } )

// Groq
aiChat( "mensaje", { model: "llama-3.3-70b-versatile" }, { provider: "groq" } )

// DeepSeek
aiChat( "mensaje", { model: "deepseek-chat" }, { provider: "deepseek" } )

// Perplexity
aiChat( "mensaje", { model: "llama-3.1-sonar-small-128k-online" }, { provider: "perplexity" } )

// Ollama (local)
aiChat( "mensaje", { model: "llama3.2" }, { provider: "ollama" } )

// OpenRouter
aiChat( "mensaje", {}, { provider: "openrouter" } )

// Grok
aiChat( "mensaje", {}, { provider: "grok" } )
```

**Proveedores de Embeddings:**

```java
// OpenAI
aiEmbeddings(
    text: "texto a codificar",
    provider: "openai",
    model: "text-embedding-3-small"  // o text-embedding-3-large
)

// Voyage (mejor para RAG)
aiEmbeddings(
    text: "texto a codificar",
    provider: "voyage",
    model: "voyage-3"  // o voyage-code-3 para código
)

// Cohere
aiEmbeddings(
    text: "texto a codificar",
    provider: "cohere",
    model: "embed-english-v3.0"
)

// Ollama (local)
aiEmbeddings(
    text: "texto a codificar",
    provider: "ollama",
    model: "nomic-embed-text"  // o mxbai-embed-large
)

// Gemini
aiEmbeddings(
    text: "texto a codificar",
    provider: "gemini",
    model: "gemini-embedding-001"
)
```

---

## 🏠 Parte 3: Usando Ollama (IA Local) (10 mins)

Ollama te permite ejecutar IA completamente local - **¡gratis, privado, sin internet requerido!**

### Configurar Ollama

1. **Descargar**: https://ollama.ai
2. **Instalar**: Ejecuta el instalador
3. **Descargar un modelo**:
   ```bash
   ollama pull llama3.2       # Propósito general
   ollama pull codellama      # Enfocado en código
   ollama pull mistral        # Rápido y capaz
   ```

### Usando Ollama en BoxLang

```java
// ejemplo-ollama.bxs
respuesta = aiChat(
    "Escribe un haiku sobre programación",
    { model: "llama3.2" },
    { provider: "ollama" }
)
println( respuesta )
```

### ¿Por Qué Usar Ollama?

```
┌─────────────────────────────────────────────────────────────────┐
│                    BENEFICIOS DE OLLAMA                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  🆓  GRATIS     - Sin costos de API                            │
│  🔒  PRIVADO    - Los datos nunca salen de tu máquina          │
│  🌐  OFFLINE    - Funciona sin internet                        │
│  ⚡  RÁPIDO     - Sin latencia de red                          │
│  🛠️  LISTO-DEV  - Perfecto para desarrollo/pruebas             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Modelos Populares de Ollama

| Modelo | Tamaño | Mejor Para |
|--------|--------|-----------|
| `llama3.2` | 2GB | Propósito general |
| `llama3.2:1b` | 1GB | Rápido, tareas simples |
| `codellama` | 4GB | Generación de código |
| `mistral` | 4GB | Respuestas de alta calidad |
| `phi3` | 2GB | Modelo eficiente de Microsoft |

---

## 🔄 Parte 4: Fallbacks de Proveedores (10 mins)

### Patrón: Probar Múltiples Proveedores

Si un proveedor falla, prueba otro:

```java
// ejemplo-fallback.bxs
function chatConFallback( mensaje ) {
    // Probar proveedores en orden
    proveedores = [ "openai", "claude", "ollama" ]

    for( proveedor in proveedores ) {
        try {
            println( "Probando #proveedor#..." )

            opciones = { provider: proveedor }
            if( proveedor == "ollama" ) {
                params = { model: "llama3.2" }
            } else {
                params = {}
            }

            return aiChat( mensaje, params, opciones )
        } catch( any e ) {
            println( "  ❌ #proveedor# falló: #e.message#" )
            continue
        }
    }

    throw( message = "¡Todos los proveedores fallaron!" )
}

// Usarlo
try {
    respuesta = chatConFallback( "¿Cuánto es 2 + 2?" )
    println( "✅ Respuesta: " & respuesta )
} catch( any e ) {
    println( "❌ " & e.message )
}
```

### Patrón: Elegir por Tarea

```java
// enrutador-tareas.bxs
function enrutarPorTarea( tarea, mensaje ) {
    switch( tarea ) {
        case "codigo":
            // Usar OpenAI para código (mejores herramientas)
            return aiChat( mensaje, { model: "gpt-4o" }, { provider: "openai" } )

        case "analisis":
            // Usar Claude para análisis largo
            return aiChat( mensaje, {}, { provider: "claude" } )

        case "rapido":
            // Usar Ollama local para tareas rápidas
            return aiChat( mensaje, { model: "llama3.2" }, { provider: "ollama" } )

        default:
            return aiChat( mensaje )
    }
}

// Usarlo
respuestaCodigo = enrutarPorTarea( "codigo", "Escribe una función para ordenar un array" )
respuestaRapida = enrutarPorTarea( "rapido", "¿Cuánto es 5 + 5?" )
```

---

## 🧪 Laboratorio: App Multi-Proveedor

### El Objetivo

Crea una app que:
1. Deje al usuario elegir un proveedor
2. Compare respuestas de diferentes proveedores
3. Maneje errores elegantemente

### Solución

```java
// multi-proveedor.bxs
println( "🔀 Demo de IA Multi-Proveedor" )
println()
println( "Proveedores disponibles:" )
println( "1. OpenAI" )
println( "2. Claude" )
println( "3. Ollama (local)" )
println( "4. ¡Comparar todos!" )
println()

eleccion = cliRead( "Elige opción (1-4): " )

pregunta = cliRead( "Ingresa tu pregunta: " )

println()
println( "─".repeat( 50 ) )
println()

function llamarProveedor( nombre, opciones, params = {} ) {
    try {
        tiempoInicio = getTickCount()
        respuesta = aiChat( pregunta, params, opciones )
        duracion = getTickCount() - tiempoInicio

        println( "✅ #nombre# (#duracion#ms):" )
        println( respuesta )
        println()
        return true
    } catch( any e ) {
        println( "❌ #nombre# falló: #e.message#" )
        println()
        return false
    }
}

switch( eleccion ) {
    case "1":
        llamarProveedor( "OpenAI", { provider: "openai" } )
        break

    case "2":
        llamarProveedor( "Claude", { provider: "claude" } )
        break

    case "3":
        llamarProveedor( "Ollama", { provider: "ollama" }, { model: "llama3.2" } )
        break

    case "4":
        println( "🔄 Comparando todos los proveedores..." )
        println()

        llamarProveedor( "OpenAI", { provider: "openai" } )
        llamarProveedor( "Claude", { provider: "claude" } )
        llamarProveedor( "Ollama", { provider: "ollama" }, { model: "llama3.2" } )

        println( "✨ ¡Comparación completa!" )
        break

    default:
        println( "¡Opción inválida!" )
}
```

### Ejecútalo

```bash
boxlang multi-proveedor.bxs
```

### Salida de Ejemplo

```
🔀 Demo de IA Multi-Proveedor

Proveedores disponibles:
1. OpenAI
2. Claude
3. Ollama (local)
4. ¡Comparar todos!

Elige opción (1-4): 4
Ingresa tu pregunta: ¿Qué es BoxLang?

──────────────────────────────────────────────────

🔄 Comparando todos los proveedores...

✅ OpenAI (842ms):
BoxLang es un lenguaje JVM moderno y dinámico que combina las mejores
características de Java con la productividad de lenguajes dinámicos.

✅ Claude (1203ms):
BoxLang es un lenguaje de programación contemporáneo que se ejecuta en la
Máquina Virtual de Java, ofreciendo tipado dinámico y estático...

✅ Ollama (234ms):
BoxLang es un lenguaje basado en JVM para construir aplicaciones modernas...

✨ ¡Comparación completa!
```

---

## ✅ Verificación de Conocimientos

1. **¿Qué proveedor se ejecuta localmente y es gratis?**
   - [ ] OpenAI
   - [ ] Claude
   - [x] Ollama
   - [ ] Gemini

2. **¿Cómo especificas un proveedor?**
   - [x] Pasándolo en el parámetro de opciones
   - [ ] Usando una función especial
   - [ ] Cambiando una variable global
   - [ ] Editando un archivo de configuración

3. **¿Por qué podrías cambiar de proveedor?**
   - [x] Costo, privacidad, diferentes capacidades
   - [ ] Es requerido para cada llamada
   - [ ] BoxLang lo obliga
   - [ ] Solo por seguridad

4. **¿Qué es un patrón de fallback?**
   - [ ] Una forma de formatear respuestas
   - [x] Probar otro proveedor si uno falla
   - [ ] Una técnica de depuración
   - [ ] Un formato de mensaje

---

## 📝 Resumen

Aprendiste:

| Concepto | Descripción |
|----------|-------------|
| **Proveedor** | Empresa/software que ejecuta modelos de IA |
| **Opciones** | Pasa `{ provider: "nombre" }` para cambiar |
| **Ollama** | IA local, gratis y privada |
| **Fallback** | Probar múltiples proveedores |
| **aiService()** | Crear conexiones reutilizables a proveedores |

### Patrones de Código Clave

```java
// Cambiar proveedor
aiChat( "msg", {}, { provider: "claude" } )

// Usar Ollama
aiChat( "msg", { model: "llama3.2" }, { provider: "ollama" } )

// Crear servicio
servicio = aiService( "openai" )
servicio.invoke( aiChatRequest( mensajes ) )
```

---

## ⏭️ Siguiente Lección

¡Ahora puedes cambiar de proveedores! Aprendamos cómo obtener datos estructurados de la IA.

👉 **[Lección 4: Salida Estructurada](../lesson-04-structured-output/)**

---

## 📁 Archivos de la Lección

```
lesson-03-providers/
├── README.md (este archivo)
├── examples/
│   ├── llamada-openai.bxs
│   ├── llamada-claude.bxs
│   ├── ejemplo-ollama.bxs
│   ├── ejemplo-servicio.bxs
│   └── ejemplo-fallback.bxs
└── labs/
    └── multi-proveedor.bxs
```
