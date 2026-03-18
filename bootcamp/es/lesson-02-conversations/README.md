# Lección 2: Conversaciones y Mensajes

**⏱️ Duración: 60 minutos**

En la última lección, hiciste llamadas individuales a la IA. ¡Ahora construyamos conversaciones reales donde la IA recuerda lo que dijiste!

## 🎯 Lo que Aprenderás

- Entender los roles de los mensajes (system, user, assistant)
- Construir conversaciones de múltiples turnos
- Usar la función `aiMessage()` para construcción fluida de mensajes
- Controlar el comportamiento de la IA con prompts de sistema

---

## 📚 Parte 1: Entendiendo los Mensajes (15 mins)

### Los Tres Roles

Cada conversación de IA usa tres tipos de mensajes:

```
┌─────────────────────────────────────────────────────────────────┐
│                      ROLES DE MENSAJES                          │
└─────────────────────────────────────────────────────────────────┘

  ┌─────────────┐
  │   SYSTEM    │  Establece la personalidad y reglas de la IA
  │  (oculto)   │  "Eres un asistente de programación..."
  └─────────────┘
         │
         ▼
  ┌─────────────┐
  │    USER     │  Tus mensajes (preguntas, solicitudes)
  │    (tú)     │  "¿Cómo escribo un bucle?"
  └─────────────┘
         │
         ▼
  ┌─────────────┐
  │  ASSISTANT  │  Respuestas de la IA
  │    (IA)     │  "Aquí está cómo escribir un bucle..."
  └─────────────┘
```

### Estructura del Mensaje

Los mensajes son simplemente structs con `role` y `content`:

```java
// Un solo mensaje
mensaje = { role: "user", content: "¡Hola!" }

// Un array de mensajes (una conversación)
mensajes = [
    { role: "system", content: "Eres un asistente útil." },
    { role: "user", content: "¡Hola!" },
    { role: "assistant", content: "¡Hola! ¿En qué puedo ayudarte?" },
    { role: "user", content: "¿Cómo está el clima?" }
]
```

### Por Qué Importan las Conversaciones

Sin historial de conversación, la IA no puede recordar nada:

```
SIN HISTORIAL                  CON HISTORIAL
───────────────                ─────────────
Tú: Mi nombre es Alex          Tú: Mi nombre es Alex
IA: ¡Gusto en conocerte!       IA: ¡Gusto en conocerte, Alex!

Tú: ¿Cuál es mi nombre?        Tú: ¿Cuál es mi nombre?
IA: No lo sé...                IA: ¡Tu nombre es Alex!
    (¡sin memoria!)                (¡recuerda!)
```

---

## 💻 Parte 2: Construyendo Conversaciones (20 mins)

### Método 1: Array de Mensajes

La forma más explícita de construir conversaciones:

```java
// array-conversacion.bxs
mensajes = [
    { role: "system", content: "Eres un tutor de matemáticas amigable. ¡Sé alentador!" },
    { role: "user", content: "¿Cuánto es 5 + 3?" }
]

respuesta = aiChat( mensajes )
println( respuesta )
// Salida: "¡Gran pregunta! 5 + 3 es igual a 8. ¡Lo estás haciendo genial!"
```

### Método 2: La Función aiMessage()

Una forma más limpia y fluida de construir mensajes:

```java
// mensajes-fluidos.bxs
mensajes = aiMessage()
    .system( "Eres un tutor de matemáticas amigable. ¡Sé alentador!" )
    .user( "¿Cuánto es 5 + 3?" )

respuesta = aiChat( mensajes )
println( respuesta )
```

### Método 3: Conversaciones Dinámicas

Construye una conversación sobre la marcha:

```java
// conversacion-dinamica.bxs
conversacion = aiMessage()
    .system( "Eres un asistente útil. Mantén las respuestas breves." )

// Primer intercambio
conversacion.user( "Hola, mi nombre es Jordan" )
respuesta1 = aiChat( conversacion )
println( "IA: " & respuesta1 )
conversacion.assistant( respuesta1 )

// Segundo intercambio
conversacion.user( "¿Cuál es mi nombre?" )
respuesta2 = aiChat( conversacion )
println( "IA: " & respuesta2 )
// Salida: "¡Tu nombre es Jordan!"
```

---

## 🎨 Parte 3: Prompts de Sistema (15 mins)

El mensaje de sistema moldea cómo se comporta la IA.

### Ejemplo: Diferentes Personalidades

```java
// personalidades.bxs

// Personalidad de pirata
chatPirata = aiMessage()
    .system( "Eres un pirata amigable. Habla como pirata en todas las respuestas. Usa 'arr' y 'marinero' frecuentemente." )
    .user( "¿Cómo hago café?" )

println( "🏴‍☠️ El Pirata dice:" )
println( aiChat( chatPirata ) )
println()

// Personalidad de profesor
chatProfesor = aiMessage()
    .system( "Eres un profesor distinguido. Explica las cosas académicamente con terminología adecuada." )
    .user( "¿Cómo hago café?" )

println( "🎓 El Profesor dice:" )
println( aiChat( chatProfesor ) )
```

### Mejores Prácticas para Prompts de Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                  PLANTILLA DE PROMPT DE SISTEMA                 │
└─────────────────────────────────────────────────────────────────┘

Eres un [ROL].

[RASGOS DE PERSONALIDAD]

[REGLAS/RESTRICCIONES]

[FORMATO DE SALIDA]
```

**Ejemplo:**

```java
promptSistema = "
Eres un desarrollador senior de BoxLang.

Eres paciente, útil y explicas los conceptos claramente.

Reglas:
- Siempre incluye ejemplos de código
- Mantén las explicaciones bajo 100 palabras
- Si no sabes algo, dilo

Formato: Usa markdown para bloques de código.
"
```

---

## 🔄 Parte 4: Patrones de Conversación (10 mins)

### Patrón: Bucle de Chat

```java
// bucle-chat.bxs
conversacion = aiMessage()
    .system( "Eres un asistente útil. Sé conciso." )

println( "=== Chat con IA ===" )
println( "Escribe 'salir' para terminar" )
println()

ejecutando = true
while( ejecutando ) {
        entradaUsuario = cliRead( "Tú: " )

    if( entradaUsuario == "salir" ) {
        ejecutando = false
        println( "¡Adiós!" )
    } else {
        conversacion.user( entradaUsuario )
        respuesta = aiChat( conversacion )
        println( "IA: " & respuesta )
        conversacion.assistant( respuesta )
        println()
    }
}
```

### Patrón: Inyección de Contexto

Agrega información que la IA debería conocer:

```java
// inyeccion-contexto.bxs
contexto = "
Fecha de hoy: #now().format( 'yyyy-MM-dd' )#
Usuario: Miembro premium
Productos disponibles: BoxLang Pro, BoxLang Enterprise, BoxLang Cloud
"

conversacion = aiMessage()
    .system( "Eres un asistente de ventas. Usa este contexto: " & contexto )
    .user( "¿Qué productos tienen?" )

respuesta = aiChat( conversacion )
println( respuesta )
```

---

## 🧪 Laboratorio: Construye un Asistente de Chat

### El Objetivo

Crea un asistente de chat interactivo que:
1. Tenga una personalidad personalizada
2. Recuerde la conversación
3. Pueda ser personalizado por el usuario

### Instrucciones

1. Crea `asistente-chat.bxs`
2. Deja que el usuario elija una personalidad (Útil, Gracioso, Serio)
3. Inicia un bucle de chat
4. La IA debe recordar mensajes anteriores

### Solución

```java
// asistente-chat.bxs
println( "🤖 ¡Bienvenido al Asistente de Chat con IA!" )
println()
println( "Elige una personalidad:" )
println( "1. Útil - Amigable y solidario" )
println( "2. Gracioso - Ingenioso con chistes" )
println( "3. Serio - Profesional y formal" )
println()

eleccion = cliRead( "Ingresa 1, 2 o 3: " )

// Establecer personalidad según la elección
switch( eleccion ) {
    case "1":
        personalidad = "Eres un asistente útil y amigable. Sé cálido y solidario."
        println( "✅ ¡Modo Útil activado!" )
        break
    case "2":
        personalidad = "Eres un asistente gracioso que ama los chistes y los juegos de palabras. ¡Haz reír a la gente!"
        println( "😄 ¡Modo Gracioso activado!" )
        break
    case "3":
        personalidad = "Eres un asistente serio y profesional. Sé formal y preciso."
        println( "📋 ¡Modo Serio activado!" )
        break
    default:
        personalidad = "Eres un asistente útil."
        println( "✅ ¡Modo predeterminado activado!" )
}

println()
println( "¡Chat iniciado! Escribe 'salir' para terminar." )
println( "─".repeat( 40 ) )

// Inicializar conversación con personalidad
conversacion = aiMessage()
    .system( personalidad )

// Bucle de chat
contadorMensajes = 0
ejecutando = true

while( ejecutando ) {
        entradaUsuario = cliRead( "Tú: " )

    if( entradaUsuario.trim() == "salir" ) {
        ejecutando = false
        println()
        println( "📊 Estadísticas: #contadorMensajes# mensajes intercambiados" )
        println( "👋 ¡Adiós!" )
    } else {
        conversacion.user( entradaUsuario )

        try {
            respuesta = aiChat( conversacion )
            println( "IA: " & respuesta )
            conversacion.assistant( respuesta )
            contadorMensajes++
        } catch( any e ) {
            println( "❌ Error: " & e.message )
        }

        println()
    }
}
```

### Ejecútalo

```bash
boxlang asistente-chat.bxs
```

### Salida de Ejemplo

```
🤖 ¡Bienvenido al Asistente de Chat con IA!

Elige una personalidad:
1. Útil - Amigable y solidario
2. Gracioso - Ingenioso con chistes
3. Serio - Profesional y formal

Ingresa 1, 2 o 3: 2
😄 ¡Modo Gracioso activado!

¡Chat iniciado! Escribe 'salir' para terminar.
────────────────────────────────────────
Tú: Cuéntame sobre BoxLang
IA: ¿BoxLang? Oh, es como si Java fuera a una fiesta, se divirtiera mucho,
    ¡y regresara como el chico cool del bloque JVM! Es dinámico,
    es moderno, ¡y no te juzga por dónde pones las llaves! 😄

Tú: salir

📊 Estadísticas: 1 mensajes intercambiados
👋 ¡Adiós!
```

---

## ✅ Verificación de Conocimientos

1. **¿Cuáles son los tres roles de mensajes?**
   - [x] system, user, assistant
   - [ ] admin, user, bot
   - [ ] input, process, output
   - [ ] start, middle, end

2. **¿Qué hace el mensaje de sistema?**
   - [ ] Almacena datos del usuario
   - [x] Establece la personalidad y reglas de la IA
   - [ ] Envía mensajes de error
   - [ ] Administra la base de datos

3. **¿Cómo agregas historial a una conversación?**
   - [x] Incluye mensajes anteriores en el array
   - [ ] Usa una función especial history()
   - [ ] La IA recuerda automáticamente
   - [ ] No puedes agregar historial

4. **¿Cuál es la forma fluida de construir mensajes?**
   - [ ] buildMessage()
   - [x] aiMessage()
   - [ ] createChat()
   - [ ] messageBuilder()

---

## 💬 Parte 5: Sistema de Contexto de Mensajes (20 mins)

### ¿Qué es el Contexto de Mensajes?

El **contexto de mensajes** te permite inyectar datos dinámicos en tus mensajes usando un placeholder `${context}`. Es mucho más potente y flexible que la concatenación de strings.

```
SIN CONTEXTO                        CON CONTEXTO
───────────────                     ────────────
"Hola " & userName                 "Hola ${context}"
                                   .setContext( userName )

❌ Manual, propenso a errores      ✅ Limpio, flexible, potente
```

### Por Qué Usar Contexto en Lugar de Concatenación

| Concatenación | Contexto de Mensajes |
|---------------|---------------------|
| ❌ Debe construir strings manualmente | ✅ Usa placeholder `${context}` |
| ❌ Difícil de formatear datos complejos | ✅ Renderiza automáticamente structs/arrays |
| ❌ Sin separación entre mensaje y datos | ✅ Separa template de datos |
| ❌ Difícil de reutilizar templates | ✅ Mismo template, diferentes datos |
| ❌ No hay escaping automático | ✅ Maneja formateo seguro |

### Métodos de Contexto

```java
// 1. setContext() - Establece TODO el contexto (sobrescribe)
mensaje.setContext( "Juan" )
mensaje.setContext( { name: "Juan", role: "admin" } )

// 2. addContext() - Agrega/actualiza campos específicos (merge)
mensaje.addContext( "name", "Juan" )
mensaje.addContext( { role: "admin", tenant: "acme" } )

// 3. mergeContext() - Merge profundo con struct existente
mensaje.mergeContext( { settings: { theme: "dark" } } )

// 4. hasContext() - Verifica si el contexto existe
if ( mensaje.hasContext() ) { ... }

// 5. getContext() - Obtiene el contexto completo
contextData = mensaje.getContext()

// 6. getContextValue() - Obtiene un campo específico
userName = mensaje.getContextValue( "name" )
```

### render() vs format()

**Diferencia clave:** `render()` procesa el placeholder `${context}`, `format()` no.

```java
mensaje = aiMessage()
    .user( "Hola ${context}" )
    .setContext( "Juan" )

// render() - Procesa ${context}
mensaje.render()  // → [{ role: "user", content: "Hola Juan" }]

// format() - NO procesa ${context}
mensaje.format()  // → [{ role: "user", content: "Hola ${context}" }]
```

**Cuándo usar cada uno:**

| Método | Usa Cuando | Contexto Procesado |
|--------|------------|-------------------|
| `render()` | Enviando a IA con contexto dinámico | ✅ Sí |
| `format()` | Guardando templates, debugging, serializando | ❌ No |

### Caso de Uso 1: Contexto de Seguridad (Multi-Tenant)

Inyecta aislamiento de seguridad y tenant:

```java
// security-context.bxs

// Establecer contexto de seguridad desde sesión autenticada
securityContext = {
    userId: session.user.id,
    tenantId: session.tenant.id,
    role: session.user.role,
    permissions: session.user.permissions
}

mensaje = aiMessage()
    .system( "Eres un asistente útil. CONTEXTO DE SEGURIDAD: ${context}" )
    .user( "¿Cuáles son mis pedidos recientes?" )
    .setContext( jsonSerialize( securityContext ) )

// La IA ve: "CONTEXTO DE SEGURIDAD: {userId: 123, tenantId: 'acme', role: 'admin'...}"
respuesta = aiChat( mensaje )
```

**Por qué esto funciona:**
- 🔒 Inyección automática de tenant/usuario en cada mensaje
- 🛡️ La IA conoce el contexto de seguridad sin lógica de prompt manual
- 🎯 Funciona con cualquier proveedor de IA
- 📊 Fácil de auditar y registrar

### Caso de Uso 2: Recuperación de Documentos RAG

Inyecta documentos recuperados en prompts de IA:

```java
// rag-context.bxs

function searchDocuments( query ) {
    // Simular búsqueda en base de datos vectorial
    return [
        { id: 1, title: "Guía de Usuario", content: "La característica X funciona...", score: 0.95 },
        { id: 2, title: "FAQ", content: "Para configurar Y...", score: 0.87 }
    ]
}

// Consulta del usuario
userQuery = "¿Cómo configurar la característica X?"

// Recuperar documentos relevantes
docs = searchDocuments( userQuery )

// Construir contexto RAG
ragContext = {
    query: userQuery,
    retrievedDocuments: docs,
    documentCount: docs.len(),
    retrievalTimestamp: now()
}

// Crear prompt con contexto
mensaje = aiMessage()
    .system( "Responde preguntas usando SOLO estos documentos recuperados: ${context}" )
    .user( userQuery )
    .setContext( jsonSerialize( ragContext ) )

respuesta = aiChat( mensaje )
// La IA ve los documentos y responde basándose en ellos
```

**Potente para:**
- 🔍 Sistemas de base de conocimientos
- 📚 Búsqueda en documentación
- 💬 Soporte al cliente con artículos de ayuda
- 🏢 Asistentes empresariales con datos privados

### Caso de Uso 3: Preferencias del Usuario

Inyecta configuraciones del usuario:

```java
// user-preferences.bxs

userPreferences = {
    language: "español",
    tone: "profesional",
    verbosity: "conciso",
    topics: [ "tecnología", "negocios" ],
    timezone: "America/New_York"
}

mensaje = aiMessage()
    .system( "PREFERENCIAS DEL USUARIO: ${context}. Respeta siempre estas preferencias." )
    .user( "Resume las noticias de hoy" )
    .setContext( jsonSerialize( userPreferences ) )

respuesta = aiChat( mensaje )
// La IA responde en español, con tono profesional, conciso, enfocado en tech/negocios
```

### Caso de Uso 4: Estado Dinámico de la Aplicación

Inyecta el estado actual de la app:

```java
// app-state-context.bxs

appState = {
    currentPage: "/dashboard",
    userStats: {
        openTickets: 5,
        pendingTasks: 12,
        unreadMessages: 3
    },
    features: [ "reports", "analytics", "export" ],
    lastAction: "viewed_report_123"
}

mensaje = aiMessage()
    .system( "ESTADO DE LA APP: ${context}. Sé consciente del contexto." )
    .user( "¿Qué debería hacer a continuación?" )
    .setContext( jsonSerialize( appState ) )

respuesta = aiChat( mensaje )
// La IA sugiere acciones basadas en tickets abiertos, tareas pendientes, etc.
```

### Streaming con Contexto

El contexto funciona perfectamente con streaming:

```java
// streaming-with-context.bxs

mensaje = aiMessage()
    .user( "Explica ${context} en términos simples" )
    .setContext( "computación cuántica" )

aiChatStream( mensaje, ( chunk ) => {
    print( chunk )  // Salida palabra por palabra
})
```

### Mejores Prácticas

| ✅ HACER | ❌ NO HACER |
|---------|-----------|
| Usar `${context}` para datos dinámicos | Concatenar strings manualmente |
| `setContext()` para datos simples | Poner lógica compleja en context |
| `addContext()` para construir incrementalmente | Sobrescribir context accidentalmente |
| `render()` al enviar a IA | Usar `format()` para invocar a IA |
| Serializar structs complejos con `jsonSerialize()` | Pasar objetos sin serializar |
| Mantener contexto enfocado y relevante | Inyectar datos masivos innecesarios |

### Contexto vs Bindings

| Característica | Contexto (`${context}`) | Bindings (`${name}`) |
|----------------|------------------------|---------------------|
| **Propósito** | Inyección de datos en bloque | Múltiples placeholders nombrados |
| **Sintaxis** | `${context}` (un placeholder) | `${var1}`, `${var2}` (muchos) |
| **Cuándo usar** | RAG, seguridad, estado de app | Nombres, valores, IDs simples |
| **Formato de datos** | Struct/array complejo | Valores individuales |
| **Configuración** | `setContext({ ... })` | `bind({ var1: "x", var2: "y" })` |

**Ejemplo combinando ambos:**

```java
mensaje = aiMessage()
    .user( "Hola ${userName}, estos son tus datos: ${context}" )
    .bind({ userName: "Juan" })
    .setContext( jsonSerialize({ orders: [...], prefs: {...} }) )
```

---

## 🌊 Parte 6: Respuestas en Streaming (15 mins)

### ¿Qué es el Streaming?

En lugar de esperar la respuesta completa, obtén **salida en tiempo real palabra por palabra**:

```
REGULAR                           STREAMING
───────                           ─────────
Esperar...                        "La"
Esperar...                        "mejor"
Esperar...                        "manera"
"La mejor manera de..."           "de..."

❌ 3-5 segundos de espera         ✅ Respuesta instantánea
❌ Sin feedback                   ✅ Experiencia progresiva
```

### Usando aiChatStream()

```java
// streaming-basic.bxs

aiChatStream(
    "Escribe un poema corto sobre BoxLang",
    ( chunk ) => {
        print( chunk )  // Se llama para cada fragmento de texto
    }
)

// Salida (palabra por palabra):
// BoxLang
// brings
// joy
// to
// coding...
```

### Streaming con Conversaciones

```java
// streaming-conversation.bxs

conversacion = aiMessage()
    .system( "Eres un poeta conciso" )
    .user( "Escribe un haiku sobre programación" )

aiChatStream( conversacion, ( chunk ) => {
    print( chunk )
})
```

### Streaming para Aplicaciones Web

Para UIs web (HTMX, JavaScript):

```java
// web-streaming.cfm

// En tu handler:
function streamChat( event, rc, prc ) {
    // Establecer headers para SSE (Server-Sent Events)
    event.setHTTPHeader( name="Content-Type", value="text/event-stream" )
    event.setHTTPHeader( name="Cache-Control", value="no-cache" )
    event.setHTTPHeader( name="Connection", value="keep-alive" )

    aiChatStream(
        rc.message,
        ( chunk ) => {
            writeOutput( "data: #encodeForJSON( chunk )#\n\n" )
            flush  // Enviar inmediatamente al navegador
        }
    )
}
```

### Procesamiento Asíncrono con aiChatAsync()

Para tareas en segundo plano (sin streaming):

```java
// async-processing.bxs

// Iniciar tarea en segundo plano
future = aiChatAsync(
    "Genera un reporte detallado de ventas del Q4"
)

println( "Tarea iniciada, haciendo otro trabajo..." )

// Hacer otro trabajo aquí...
performOtherTasks()

// Esperar resultado cuando lo necesites
respuesta = future.get()  // Bloquea hasta completar
println( "Reporte: " & respuesta )
```

### Comparación: Regular vs Streaming vs Async

| Característica | Regular `aiChat()` | Streaming `aiChatStream()` | Async `aiChatAsync()` |
|----------------|-------------------|---------------------------|----------------------|
| **Latencia** | ⏳ Alta (espera completa) | ⚡ Baja (inmediata) | ⏳ Alta (en segundo plano) |
| **Experiencia** | 😐 Spinner de carga | 😊 Feedback progresivo | 🎯 Sin bloqueo |
| **Caso de uso** | Scripts simples | Chat UIs, CLI interactivos | Tareas en segundo plano |
| **Bloqueo** | ✅ Sí (espera) | ✅ Sí (pero muestra progreso) | ❌ No (non-blocking) |
| **Complejidad** | 🟢 Simple | 🟡 Requiere callback | 🟠 Requiere manejo de futures |

### Cuándo Usar Cada Enfoque

```java
// 1. REGULAR - Scripts simples, tareas cortas
respuesta = aiChat( "¿2+2?" )
println( respuesta )  // "4"

// 2. STREAMING - UIs de chat, feedback al usuario
aiChatStream( "Explica la IA", ( chunk ) => print( chunk ) )
// Experiencia: IA escribe en tiempo real

// 3. ASYNC - Tareas en segundo plano, procesamiento largo
future = aiChatAsync( "Analiza 1000 registros" )
doOtherWork()
resultado = future.get()
```

---

## 📝 Resumen

Aprendiste:

| Concepto | Descripción |
|----------|-------------|
| **system** | Establece la personalidad y reglas de la IA |
| **user** | Tus mensajes a la IA |
| **assistant** | Respuestas de la IA |
| **aiMessage()** | Constructor fluido de mensajes |
| **Conversación** | Array de mensajes con contexto |
| **Contexto de Mensajes** | Inyección dinámica de datos con `${context}` |
| **setContext()** | Establece datos de contexto para inyección |
| **render()** | Procesa `${context}` antes de enviar a IA |
| **aiChatStream()** | Respuestas en tiempo real palabra por palabra |
| **aiChatAsync()** | Procesamiento en segundo plano sin bloqueo |

### Patrones de Código Clave

```java
// Método de array
mensajes = [
    { role: "system", content: "Sé útil" },
    { role: "user", content: "Hola" }
]

// Método fluido
mensajes = aiMessage()
    .system( "Sé útil" )
    .user( "Hola" )

// Construyendo conversación con el tiempo
conversacion.user( "Pregunta" )
respuesta = aiChat( conversacion )
conversacion.assistant( respuesta )

// Contexto de mensajes
mensaje = aiMessage()
    .user( "Resume ${context}" )
    .setContext( jsonSerialize({ docs: [...] }) )
respuesta = aiChat( mensaje.render() )

// Streaming
aiChatStream( "Explica IA", ( chunk ) => print( chunk ) )

// Async
future = aiChatAsync( "Tarea larga" )
resultado = future.get()
```

---

## ⏭️ Siguiente Lección

¡Ahora puedes construir conversaciones! Aprendamos cómo cambiar entre diferentes proveedores de IA.

👉 **[Lección 3: Cambiando Proveedores](../lesson-03-providers/)**

---

## 📁 Archivos de la Lección

```
lesson-02-conversations/
├── README.md (este archivo)
├── examples/
│   ├── array-conversacion.bxs
│   ├── mensajes-fluidos.bxs
│   ├── conversacion-dinamica.bxs
│   ├── personalidades.bxs
│   └── bucle-chat.bxs
└── labs/
    └── asistente-chat.bxs
```
