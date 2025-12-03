# Lección 1: Primeros Pasos

**⏱️ Duración: 45 minutos**

¡Bienvenido a tu primer paso en el desarrollo de IA con BoxLang! En esta lección, configurarás tu entorno y harás tu primera llamada a IA.

## 🎯 Lo que Aprenderás

- Instalar y configurar el módulo bx-ai
- Entender las claves de API y los proveedores
- Hacer tu primera llamada a IA
- Entender los tokens (cómo la IA "ve" el texto)

---

## 📚 Parte 1: Entendiendo los Conceptos Básicos de IA (10 mins)

Antes de escribir código, entendamos con qué estamos trabajando.

### ¿Qué es un Modelo de Lenguaje Grande (LLM)?

Un LLM es un sistema de IA que:
- **Lee** y entiende texto
- **Genera** respuestas similares a las humanas
- **Ayuda** con programación, escritura, análisis y más

```
┌─────────────────────────────────────────────────────────────────┐
│                     CÓMO FUNCIONA LA IA                         │
└─────────────────────────────────────────────────────────────────┘

  Tu Pregunta           Procesamiento IA        Respuesta IA
  ──────────────        ───────────────         ──────────────
       │                      │                       │
       ▼                      ▼                       ▼
  ┌─────────┐           ┌─────────┐            ┌─────────┐
  │"¿Qué es │  ──────▶  │  Red    │  ──────▶   │"BoxLang │
  │BoxLang?"│           │ Neural  │            │es un    │
  │         │           │(miles de│            │lenguaje │
  │         │           │millones │            │JVM que..│
  │         │           │de params│            │         │
  └─────────┘           └─────────┘            └─────────┘
```

### ¿Qué es un Token?

La IA no ve las palabras como nosotros. Divide el texto en **tokens** - pequeñas piezas de texto.

```
┌─────────────────────────────────────────────────────────────────┐
│                      EJEMPLOS DE TOKENS                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   "Hola"              =  1 token                                │
│   "Hola mundo"        =  2 tokens                               │
│   "BoxLang es genial" =  4 tokens                               │
│                                                                 │
│   💡 Regla general: ~4 caracteres = 1 token                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Por qué importan los tokens:**
- ✅ Pagas por token (entrada + salida)
- ✅ Los modelos tienen límites de tokens (ventana de contexto)
- ✅ Más tokens = respuestas más largas

### ¿Qué es un Proveedor de IA?

Un proveedor es una empresa que ejecuta modelos de IA en la nube (o localmente):

| Proveedor | Modelos | Precio | Mejor Para |
|-----------|---------|--------|------------|
| **OpenAI** | GPT-4, GPT-4o-mini | Pago | Más capaz |
| **Claude** | Claude 3.5 Sonnet | Pago | Contexto largo, razonamiento |
| **Ollama** | Llama 3.2, Mistral | **Gratis** | Local/privado |

---

## 🛠️ Parte 2: Configuración (15 mins)

### Paso 1: Verificar BoxLang

Abre tu terminal y ejecuta:

```bash
boxlang --version
```

Deberías ver algo como `BoxLang 1.x.x`. Si no, [descarga BoxLang](https://boxlang.io).

### Paso 2: Instalar el Módulo bx-ai

```bash
install-bx-module bx-ai
```

O para aplicaciones web (CommandBox):
```bash
box install bx-ai
```

### Paso 3: Obtener una Clave de API

**Opción A: OpenAI (Recomendado para principiantes)**

1. Ve a https://platform.openai.com/api-keys
2. Regístrate o inicia sesión
3. Haz clic en "Create new secret key"
4. Copia la clave (comienza con `sk-`)
5. Agrega créditos a tu cuenta ($5 son suficientes para empezar)

**Opción B: Ollama (Gratis, ejecuta localmente)**

1. Descarga desde https://ollama.ai
2. Instala y ejecuta Ollama
3. Descarga un modelo:
   ```bash
   ollama pull llama3.2
   ```
4. ¡No necesitas clave de API!

### Paso 4: Configura tu Clave de API

Crea un archivo `.env` en tu proyecto:

```bash
# Para OpenAI
OPENAI_API_KEY=sk-tu-clave-aqui

# Para Claude
CLAUDE_API_KEY=sk-ant-tu-clave-aqui
```

> ⚠️ **¡Nunca hagas commit de claves de API a git!** Agrega `.env` a tu `.gitignore`.

---

## 💻 Parte 3: Tu Primera Llamada a IA (10 mins)

### La Función aiChat()

La forma más simple de hablar con la IA:

```java
resultado = aiChat( "Tu mensaje aquí" )
```

¡Eso es todo! Vamos a probarlo.

### Ejemplo 1: Hola IA

Crea un archivo llamado `hola-ai.bxs`:

```java
// hola-ai.bxs
// ¡Tu primera llamada a IA!

respuesta = aiChat( "¡Saluda a alguien que está aprendiendo BoxLang AI!" )
println( respuesta )
```

Ejecútalo:
```bash
boxlang hola-ai.bxs
```

**Salida esperada:**
```
¡Hola! ¡Bienvenido a tu viaje con BoxLang AI! Estoy emocionado de ayudarte
a aprender cómo construir aplicaciones increíbles con IA. ¡Empecemos! 🚀
```

### Ejemplo 2: Hacer una Pregunta

```java
// hacer-pregunta.bxs
pregunta = "¿Qué es BoxLang en una oración?"
respuesta = aiChat( pregunta )

println( "P: " & pregunta )
println( "R: " & respuesta )
```

### Ejemplo 3: Usando Ollama (Gratis/Local)

Si instalaste Ollama:

```java
// ai-local.bxs
respuesta = aiChat(
    "¿Cuánto es 2 + 2?",
    { model: "llama3.2" },
    { provider: "ollama" }
)
println( respuesta )
```

### Ejemplo 4: Manejo de Errores

Siempre envuelve las llamadas a IA en try/catch:

```java
// llamada-segura.bxs
try {
    respuesta = aiChat( "Cuéntame un chiste de programación" )
    println( respuesta )
} catch( any e ) {
    println( "❌ Error: " & e.message )
    println( "💡 ¡Verifica tu clave de API!" )
}
```

---

## 🧪 Parte 4: Laboratorio - Bola Mágica 8 (10 mins)

¡Construyamos tu primera aplicación de IA: una Bola Mágica 8!

### El Objetivo

Crear un adivino impulsado por IA que responda preguntas de sí/no.

### Instrucciones

1. Crea un archivo `bola-magica-8.bxs`
2. Pide al usuario una pregunta
3. Envíala a la IA con instrucciones especiales
4. Muestra la respuesta mística

### Código Inicial

```java
// bola-magica-8.bxs

println( "🎱 ¡Bienvenido a la Bola Mágica 8 con IA! 🎱" )
println( "Hazme una pregunta de sí/no..." )
println( "" )

// Obtener la pregunta del usuario
print( "Tu pregunta: " )
pregunta = readLine()

// Crear el prompt mágico
prompt = "
Eres una Bola Mágica 8 mística.
Responde la siguiente pregunta de sí/no con UNA de estas respuestas clásicas:
- Es seguro
- Sin duda
- Sí definitivamente
- Puedes confiar en ello
- Muy probablemente
- Perspectiva buena
- Las señales apuntan a sí
- Respuesta nebulosa, intenta de nuevo
- Pregunta más tarde
- No puedo predecir ahora
- No cuentes con ello
- Mis fuentes dicen que no
- Perspectiva no tan buena
- Muy dudoso

Pregunta: #pregunta#

Responde con SOLO la frase de la Bola Mágica 8, nada más.
"

// Obtener la respuesta mística
try {
    respuesta = aiChat( prompt, { temperature: 0.9 } )
    println( "" )
    println( "🔮 La Bola Mágica 8 dice..." )
    println( "   " & respuesta )
} catch( any e ) {
    println( "❌ Los espíritus no están claros: " & e.message )
}
```

### Ejecútalo

```bash
boxlang bola-magica-8.bxs
```

### Salida de Ejemplo

```
🎱 ¡Bienvenido a la Bola Mágica 8 con IA! 🎱
Hazme una pregunta de sí/no...

Tu pregunta: ¿Aprenderé BoxLang AI hoy?

🔮 La Bola Mágica 8 dice...
   Es seguro
```

### Desafío

Modifica la Bola Mágica 8 para:
1. Seguir haciendo preguntas en un bucle
2. Escribir "salir" para terminar
3. Contar cuántas preguntas se hicieron

---

## ✅ Verificación de Conocimientos

Prueba tu comprensión:

1. **¿Qué hace un LLM?**
   - [ ] Solo escribe código
   - [x] Entiende y genera texto
   - [ ] Almacena bases de datos
   - [ ] Ejecuta servidores

2. **¿Qué es un token?**
   - [ ] Una contraseña
   - [x] Una pieza de texto que la IA procesa
   - [ ] Un tipo de variable
   - [ ] Un mensaje de error

3. **¿Qué proveedor es gratis y se ejecuta localmente?**
   - [ ] OpenAI
   - [ ] Claude
   - [x] Ollama
   - [ ] Gemini

4. **¿Qué función hace una llamada simple a IA?**
   - [ ] ai()
   - [x] aiChat()
   - [ ] sendAI()
   - [ ] chatBot()

---

## 📝 Resumen

Aprendiste:

| Concepto | Qué Significa |
|----------|---------------|
| **LLM** | IA que entiende y genera texto |
| **Token** | Una pieza de texto (~4 caracteres) |
| **Proveedor** | Empresa que ejecuta modelos de IA |
| **aiChat()** | Función para enviar mensajes a la IA |

### Código Clave

```java
// Llamada básica a IA
respuesta = aiChat( "Tu mensaje" )

// Con opciones
respuesta = aiChat(
    "Mensaje",
    { temperature: 0.7 },         // Parámetros
    { provider: "openai" }        // Opciones
)
```

---

## ⏭️ Siguiente Lección

¡Has hecho tu primera llamada a IA! Ahora aprendamos a tener conversaciones reales.

👉 **[Lección 2: Conversaciones y Mensajes](../lesson-02-conversations/)**

---

## 📁 Archivos de la Lección

```
lesson-01-getting-started/
├── README.md (este archivo)
├── examples/
│   ├── hola-ai.bxs
│   ├── hacer-pregunta.bxs
│   ├── ai-local.bxs
│   └── llamada-segura.bxs
└── labs/
    └── bola-magica-8.bxs
```
