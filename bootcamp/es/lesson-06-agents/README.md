# Lección 6: Construyendo Agentes

**⏱️ Duración: 90 minutos**

En esta lección final, reunimos todo para construir **agentes de IA autónomos**. Los agentes combinan memoria de conversación, herramientas e instrucciones para completar tareas complejas de múltiples pasos por su cuenta.

## 🎯 Lo que Aprenderás

- Entender la diferencia entre chat y agentes
- Crear agentes con `aiAgent()`
- Agregar memoria para que los agentes recuerden el contexto
- Dar herramientas a los agentes para interactuar con el mundo
- Construir un asistente completo que maneje tareas complejas

---

## 📚 Parte 1: ¿Qué es un Agente de IA? (15 mins)

### Chat vs Agente

Hasta ahora, hemos usado **chat** - tú controlas todo:

```
CHAT (Tú Controlas)
──────────────────
Tú: "Busca X"
IA: "Aquí está info sobre X"
Tú: "Ahora calcula Y"
IA: "Y es igual a 100"
Tú: (decides qué hacer después)
```

Un **agente** controla su propio flujo de trabajo:

```
AGENTE (IA Controla)
───────────────────
Tú: "Investiga X y calcula el impacto"
Agente: (pensando...)
  1. Debería buscar X
  2. Ahora analizaré los datos
  3. Déjame calcular el impacto
  4. ¡Aquí está mi reporte completo!
```

### Arquitectura del Agente

```
┌─────────────────────────────────────────────────────────────────┐
│                       AGENTE DE IA                              │
└─────────────────────────────────────────────────────────────────┘

                    ┌─────────────────┐
                    │  INSTRUCCIONES  │
                    │ (Prompt Sistema)│
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
  │   MEMORIA   │    │    LLM      │    │HERRAMIENTAS │
  │ Historial   │◀──▶│  (Cerebro)  │◀──▶│ (Acciones)  │
  │ Conversación│    │             │    │             │
  └─────────────┘    └─────────────┘    └─────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   RESPUESTA     │
                    └─────────────────┘


  💡 El agente decide:
     - Qué herramientas usar
     - En qué orden
     - Cómo combinar resultados
     - Cuándo ha terminado
```

### ¿Por Qué Agentes?

- ✅ **Tareas de múltiples pasos** - Descompone problemas complejos
- ✅ **Autónomo** - Decide los siguientes pasos independientemente
- ✅ **Consciente del contexto** - Recuerda el historial de conversación
- ✅ **Uso de herramientas** - Llama funciones cuando las necesita
- ✅ **Orientado a objetivos** - Trabaja hacia un resultado específico

---

## 💻 Parte 2: Creando Tu Primer Agente (20 mins)

### La Función aiAgent()

```java
agente = aiAgent(
    name: "NombreAgente",
    description: "Qué hace este agente",
    instructions: "Cómo debe comportarse el agente",
    tools: [ herramienta1, herramienta2 ],
    memory: aiMemory( "windowed" )
)

// Ejecutar el agente
resultado = agente.run( "Tu solicitud" )
```

### Ejemplo: Agente Básico

```java
// agente-basico.bxs
agente = aiAgent(
    name: "Helper",
    description: "A helpful AI assistant",
    instructions: "Be concise and friendly. Help users with their questions."
)

// Primera interacción
respuesta1 = agente.run( "Hi, my name is Alex" )
println( respuesta1 )
// Salida: "¡Hola Alex! Gusto en conocerte. ¿En qué puedo ayudarte hoy?"

// ¡El agente recuerda (tiene memoria!)
respuesta2 = agente.run( "What's my name?" )
println( respuesta2 )
// Salida: "¡Tu nombre es Alex!"
```

### Ejemplo: Agente con Herramientas

```java
// agente-herramientas.bxs
// Crear herramientas
weatherTool = aiTool(
    "get_weather",
    "Get weather for a city",
    ( args ) => {
        data = { "Boston": 72, "Miami": 85, "Denver": 65 }
        return "#data[ args.city ] ?: 70#°F in #args.city#"
    }
).describeCity( "City name" )

calculatorTool = aiTool(
    "calculate",
    "Perform math calculations",
    ( args ) => evaluate( args.expression )
).describeExpression( "Math expression" )

// Crear agente con herramientas
agente = aiAgent(
    name: "SmartAssistant",
    description: "An assistant that can check weather and do math",
    instructions: "Help users with weather info and calculations.",
    tools: [ weatherTool, calculatorTool ]
)

// ¡El agente usa herramientas automáticamente!
println( agente.run( "What's the weather in Miami?" ) )
println( agente.run( "What's 20% of 150?" ) )
```

---

## 🧠 Parte 3: Memoria del Agente (15 mins)

La memoria permite a los agentes recordar la conversación:

### Tipos de Memoria

| Tipo | Descripción | Mejor Para |
|------|-------------|-----------|
| `windowed` | Mantiene los últimos N mensajes | La mayoría de casos |
| `summary` | Resume mensajes antiguos | Conversaciones largas |
| `session` | Persiste en sesión web | Aplicaciones web |
| `cache` | Almacenamiento en caché distribuido | Apps multi-servidor |
| `file` | Persistencia en archivo JSON | Almacenamiento local |
| `jdbc` | Almacenamiento en base de datos | Apps empresariales |
| `vector` | Búsqueda semántica (11 proveedores) | Aplicaciones RAG |

> 💡 **Memoria Multi-Tenant**: Todos los tipos de memoria soportan parámetros `userId` y `conversationId` para aplicaciones multi-usuario. Esto asegura que las conversaciones de cada usuario estén completamente aisladas:
>
> ```java
> memoria = aiMemory( "windowed",
>     key: createUUID(),
>     userId: session.userId,           // Aísla por usuario
>     conversationId: "chat-soporte",  // Múltiples chats por usuario
>     config: { maxMessages: 20 }
> )
> ```
>
> ¡Esto es esencial para aplicaciones web donde múltiples usuarios interactúan con tu agente!

### Ejemplo: Agente con Memoria

```java
// agente-memoria.bxs
// Memoria simple de un solo usuario (bueno para scripts/CLI)
agente = aiAgent(
    name: "PersonalAssistant",
    description: "A personal assistant that remembers your preferences",
    instructions: "Remember user preferences and past conversations.",
    memory: aiMemory( "windowed", { maxMessages: 20 } )
)

// Dile cosas al agente
agente.run( "My favorite color is blue" )
agente.run( "I live in Boston" )
agente.run( "I work as a software developer" )

// Pregunta sobre info recordada
println( agente.run( "What's my favorite color?" ) )
// Salida: "¡Tu color favorito es azul!"

println( agente.run( "Where do I live and what do I do?" ) )
// Salida: "¡Vives en Boston y trabajas como desarrollador de software!"

// Limpia la memoria cuando sea necesario
agente.clearMemory()
```

### Flujo de Memoria

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUJO DE MEMORIA                             │
└─────────────────────────────────────────────────────────────────┘

  Turno 1               Turno 2               Turno 3
  ──────                ──────                ──────

  Usuario: "Soy Alex"   Usuario: "¿Mi nombre?"  Usuario: "Resume"
        │                     │                     │
        ▼                     ▼                     ▼
  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
  │   MEMORIA   │      │   MEMORIA   │      │   MEMORIA   │
  │ [msg Alex]  │      │ [msg Alex]  │      │ [msg Alex]  │
  │             │      │ [resp nomb] │      │ [resp nomb] │
  │             │      │ [msg nomb?] │      │ [msg nomb?] │
  │             │      │             │      │ [msg resum] │
  └─────────────┘      └─────────────┘      └─────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
  IA: "¡Hola Alex!"    IA: "¡Alex!"          IA: "Eres Alex,
                                                  preguntaste..."
```

---

## 🛠️ Parte 4: Ejemplo Completo de Agente (20 mins)

Construyamos un **Agente de Soporte al Cliente**:

```java
// agente-soporte.bxs

println( "🎧 Agente de Soporte al Cliente" )
println( "═".repeat( 50 ) )
println()

// Base de datos simulada
orders = {
    "ORD-001": { status: "Shipped", item: "Widget Pro", customer: "Alex" },
    "ORD-002": { status: "Processing", item: "Gadget X", customer: "Jordan" },
    "ORD-003": { status: "Delivered", item: "Tool Kit", customer: "Sam" }
}

products = {
    "Widget Pro": { price: 99.99, stock: 50 },
    "Gadget X": { price: 149.99, stock: 0 },
    "Tool Kit": { price: 79.99, stock: 25 }
}

// Herramienta: Buscar orden
orderTool = aiTool(
    "lookup_order",
    "Look up order status by order ID",
    ( args ) => {
        orderId = args.orderId.uCase()
        if( orders.keyExists( orderId ) ) {
            order = orders[ orderId ]
            return "Order #orderId#: #order.item# - Status: #order.status#"
        }
        return "Order #orderId# not found"
    }
).describeOrderId( "The order ID (e.g., ORD-001)" )

// Herramienta: Verificar producto
productTool = aiTool(
    "check_product",
    "Check product price and availability",
    ( args ) => {
        productName = args.productName
        for( name in products.keyList() ) {
            if( name.findNoCase( productName ) > 0 ) {
                product = products[ name ]
                stock = product.stock > 0 ? "In Stock (#product.stock#)" : "Out of Stock"
                return "#name#: $#product.price# - #stock#"
            }
        }
        return "Product not found. Available: #products.keyList()#"
    }
).describeProductName( "Product name to check" )

// Herramienta: Crear ticket
ticketTool = aiTool(
    "create_ticket",
    "Create a support ticket for issues that need human review",
    ( args ) => {
        ticketId = "TKT-" & randRange( 1000, 9999 )
        return "Created ticket #ticketId#: #args.issue#. A human agent will follow up."
    }
).describeIssue( "Description of the issue" )

// Crear el agente de soporte
supportAgent = aiAgent(
    name: "SupportBot",
    description: "A customer support agent for order and product inquiries",
    instructions: "
        You are a helpful customer support agent.

        Guidelines:
        - Be friendly and professional
        - Look up orders when customers ask about their orders
        - Check product info when asked about prices or availability
        - Create a support ticket for complex issues
        - If you don't know something, offer to create a ticket

        Always ask if there's anything else you can help with.
    ",
    tools: [ orderTool, productTool, ticketTool ],
    memory: aiMemory( "windowed", { maxMessages: 10 } )
)

// Bucle de chat
println( "¡Hola! Soy tu asistente de soporte. ¿En qué puedo ayudarte?" )
println( "─".repeat( 50 ) )
println()

running = true
while( running ) {
    print( "Tú: " )
    userInput = readLine()

    if( userInput.trim() == "salir" || userInput.trim() == "bye" ) {
        running = false
        println( "SupportBot: ¡Gracias por contactarnos! ¡Que tengas un excelente día! 👋" )
    } else {
        try {
            response = supportAgent.run( userInput )
            println( "SupportBot: " & response )
            println()
        } catch( any e ) {
            println( "SupportBot: Estoy teniendo problemas ahora. Por favor intenta de nuevo." )
            println()
        }
    }
}
```

### Interacción de Ejemplo

```
🎧 Agente de Soporte al Cliente
══════════════════════════════════════════════════

¡Hola! Soy tu asistente de soporte. ¿En qué puedo ayudarte?
──────────────────────────────────────────────────

Tú: Hi, I ordered something last week
SupportBot: ¡Estaré encantado de ayudarte a verificar tu orden!
           ¿Podrías proporcionarme tu ID de orden? Comienza con ORD-.

Tú: It's ORD-001
SupportBot: ¡Encontré tu orden! La orden ORD-001 para Widget Pro está actualmente
           Enviada. ¡Debería llegar pronto! ¿Hay algo más en lo que pueda ayudarte?

Tú: How much does the Gadget X cost?
SupportBot: El Gadget X cuesta $149.99, pero desafortunadamente está actualmente
           Agotado. ¿Te gustaría que cree un ticket para notificarte
           cuando vuelva a estar disponible?

Tú: Yes please
SupportBot: Ticket creado TKT-4721: Cliente quiere notificación cuando
           Gadget X vuelva a estar disponible. Un agente humano dará seguimiento.
           ¿Hay algo más en lo que pueda ayudarte?

Tú: bye
SupportBot: ¡Gracias por contactarnos! ¡Que tengas un excelente día! 👋
```

---

## 🧪 Parte 5: Laboratorio - Construye Tu Propio Agente (20 mins)

### El Desafío

Construye un **Agente de Investigación** que pueda:
1. Buscar información (simulada)
2. Resumir hallazgos
3. Recordar la conversación

### Requisitos

- Tiene una herramienta `search`
- Tiene una herramienta `summarize`
- Usa memoria
- Sigue instrucciones claras

### Código Inicial

```java
// agente-investigacion.bxs

println( "🔍 Agente de Investigación" )
println( "═".repeat( 40 ) )
println()

// Base de conocimiento simulada
knowledgeBase = {
    "boxlang": "BoxLang is a modern dynamic JVM language with CFML compatibility.",
    "java": "Java is a widely-used programming language for enterprise applications.",
    "ai": "Artificial Intelligence enables machines to simulate human intelligence.",
    "llm": "Large Language Models are AI systems trained on vast text datasets."
}

// TODO: Crear herramienta de búsqueda
searchTool = aiTool(
    "search",
    "Search the knowledge base for information",
    ( args ) => {
        query = args.query.lCase()
        for( topic in knowledgeBase.keyList() ) {
            if( query.findNoCase( topic ) > 0 ) {
                return "Found: " & knowledgeBase[ topic ]
            }
        }
        return "No results for '#args.query#'. Try: #knowledgeBase.keyList()#"
    }
).describeQuery( "What to search for" )

// TODO: Crear herramienta de resumen
summarizeTool = aiTool(
    "summarize",
    "Create a brief summary of given text",
    ( args ) => {
        text = args.text
        // Simulación simple - en app real, podría usar IA
        return "Summary: " & left( text, 100 ) & "..."
    }
).describeText( "Text to summarize" )

// TODO: Crear el agente de investigación
researchAgent = aiAgent(
    name: "Researcher",
    description: "A research agent that searches and summarizes information",
    instructions: "
        You are a research assistant.
        - Search for topics when asked
        - Provide clear explanations
        - Summarize when requested
        - Remember what the user has asked about
    ",
    tools: [ searchTool, summarizeTool ],
    memory: aiMemory( "windowed", { maxMessages: 10 } )
)

// Bucle de chat
println( "¡Pídeme que investigue algo!" )
println( "Temas que conozco: #knowledgeBase.keyList()#" )
println( "─".repeat( 40 ) )
println()

running = true
while( running ) {
    print( "Tú: " )
    userInput = readLine()

    if( userInput.trim() == "salir" ) {
        running = false
        println( "¡Adiós! 📚" )
    } else {
        try {
            response = researchAgent.run( userInput )
            println( "Investigador: " & response )
            println()
        } catch( any e ) {
            println( "Error: " & e.message )
            println()
        }
    }
}
```

---

## ✅ Verificación de Conocimientos

1. **¿Qué hace diferente a un agente del chat?**
   - [ ] Los agentes son más rápidos
   - [x] Los agentes deciden sus propios siguientes pasos
   - [ ] Los agentes cuestan más
   - [ ] Los agentes no usan herramientas

2. **¿Qué devuelve aiAgent()?**
   - [ ] Una respuesta de string
   - [x] Un objeto agente que puedes ejecutar
   - [ ] Una colección de herramientas
   - [ ] Un objeto de memoria

3. **¿Cómo recuerda el contexto un agente?**
   - [ ] No lo hace
   - [ ] Via llamadas API
   - [x] Usando memoria (aiMemory)
   - [ ] Usando cookies

4. **¿Qué método ejecuta un agente?**
   - [ ] agent.chat()
   - [x] agent.run()
   - [ ] agent.execute()
   - [ ] agent.start()

---

## 📝 Resumen

Aprendiste:

| Concepto | Descripción |
|----------|-------------|
| **Agente** | IA autónoma que planifica y ejecuta |
| **aiAgent()** | Crea un agente |
| **Memoria** | Almacena historial de conversación |
| **Instrucciones** | Guía el comportamiento del agente |
| **Herramientas** | Acciones que el agente puede tomar |

### Patrón de Código Clave

```java
// Crear agente
agente = aiAgent(
    name: "MiAgente",
    description: "Qué hace",
    instructions: "Cómo comportarse",
    tools: [ herramienta1, herramienta2 ],
    memory: aiMemory( "windowed" )
)

// Usar agente
respuesta = agente.run( "Solicitud del usuario" )
```

---

## 🌐 Extra: Agentes Multi-Tenant para Apps Web

**Para aplicaciones web con múltiples usuarios**, querrás aislar la conversación de cada usuario:

### ¿Por Qué Multi-Tenant?

Sin aislamiento:
```java
// ❌ MALO: ¡Todos los usuarios comparten la misma memoria!
agente = aiAgent(
    memory: aiMemory( "windowed" )
)
// ¡Los datos de Alice se filtran a Bob!
```

Con aislamiento:
```java
// ✅ BUENO: Cada usuario tiene su propia memoria
function getUserAgent( userId, conversationId ) {
    return aiAgent(
        name: "WebAssistant",
        instructions: "Sé útil y profesional",
        memory: aiMemory( "session",
            key: "chat",
            userId: userId,              // Aísla por usuario
            conversationId: conversationId,  // Múltiples chats por usuario
            config: { maxMessages: 50 }
        )
    )
}

// En tu handler web:
function chat( event, rc, prc ) {
    userId = auth().user().getId()  // De la sesión autenticada
    conversationId = rc.chatId ?: createUUID()
    
    agente = getUserAgent( userId, conversationId )
    respuesta = agente.run( rc.message )
    
    return { response: respuesta, conversationId: conversationId }
}
```

### Puntos Clave

- 🔒 **Seguridad**: Los datos de cada usuario están aislados
- 💬 **Múltiples Chats**: Los usuarios pueden tener múltiples conversaciones
- 📊 **Escalabilidad**: Funciona en servidores distribuidos (con memoria cache/jdbc)
- 🎯 **Listo para Empresa**: Multi-tenancy de grado de producción

> **Aprende Más**: ¡Consulta la [Guía de Memoria Multi-Tenant](../../../docs/advanced/multi-tenant-memory.md) para patrones empresariales!

---

## 🎉 ¡Felicitaciones!

¡Has completado el Bootcamp de BoxLang AI! Ahora sabes:

```
┌─────────────────────────────────────────────────────────────────┐
│                  HABILIDADES ADQUIRIDAS                         │
└─────────────────────────────────────────────────────────────────┘

  ✅ Lección 1: Configuración y Primera Llamada a IA
  ✅ Lección 2: Conversaciones y Mensajes
  ✅ Lección 3: Cambiando Proveedores
  ✅ Lección 4: Salida Estructurada
  ✅ Lección 5: Herramientas de IA
  ✅ Lección 6: Construyendo Agentes

  Ahora puedes:
  ───────────
  • Hacer llamadas a IA con aiChat()
  • Construir conversaciones de múltiples turnos
  • Usar OpenAI, Claude y Ollama
  • Extraer datos estructurados con tipos seguros
  • Crear herramientas que la IA puede usar
  • Construir agentes autónomos
```

## ⏭️ ¿Qué Sigue?

### Profundiza: Curso Completo

Toma el [curso de 12 lecciones](../../course/) para:
- Respuestas en streaming
- Flujos de trabajo de pipelines
- Sistemas de memoria avanzados
- Despliegue en producción
- Embeddings vectoriales
- ¡Y mucho más!

### Explora Ejemplos

Revisa la [carpeta de ejemplos](../../examples/) para más código.

### ¡Construye Algo!

La mejor manera de aprender es haciendo. Intenta construir:
- Un bot de servicio al cliente
- Un asistente de revisión de código
- Un agente de análisis de datos
- Un ayudante de productividad personal

---

## 📁 Archivos de la Lección

```
lesson-06-agents/
├── README.md (este archivo)
├── examples/
│   ├── agente-basico.bxs
│   ├── agente-herramientas.bxs
│   └── agente-memoria.bxs
└── labs/
    ├── agente-soporte.bxs
    └── agente-investigacion.bxs
```

---

**¡Gracias por completar el bootcamp! 🎓**

¿Preguntas? Visita [GitHub Issues](https://github.com/ortus-boxlang/bx-ai/issues)
