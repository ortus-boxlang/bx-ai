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
    print( "Tú: " )
    entradaUsuario = readLine()

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

print( "Ingresa 1, 2 o 3: " )
eleccion = readLine()

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
    print( "Tú: " )
    entradaUsuario = readLine()

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

## 📝 Resumen

Aprendiste:

| Concepto | Descripción |
|----------|-------------|
| **system** | Establece la personalidad y reglas de la IA |
| **user** | Tus mensajes a la IA |
| **assistant** | Respuestas de la IA |
| **aiMessage()** | Constructor fluido de mensajes |
| **Conversación** | Array de mensajes con contexto |

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
