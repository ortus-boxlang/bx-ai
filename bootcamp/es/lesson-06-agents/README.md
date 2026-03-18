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

## 🧠 Parte 3: Memoria Multi-Tenant del Agente (25 mins)

La memoria permite a los agentes recordar la conversación. Para **aplicaciones de producción con múltiples usuarios**, la memoria multi-tenant es esencial.

### ¿Por Qué Importa la Memoria Multi-Tenant?

**SIN aislamiento multi-tenant** - ¡Los datos de todos se mezclan!

```
Usuario A → Agente → Memoria Compartida ← Usuario B
   ❌ "Soy Alex"  │  [Alex, Jordan, Sam]  │  ❌ "Soy Jordan"
   ❌ Ve los datos de Jordan/Sam          │  ❌ Ve los datos de Alex/Sam
```

**CON aislamiento multi-tenant** - Cada usuario obtiene memoria privada:

```
Usuario A → Agente → Memoria A  [Solo Alex]
Usuario B → Agente → Memoria B  [Solo Jordan]
Usuario C → Agente → Memoria C  [Solo Sam]
   ✅ Completamente aislado       ✅ Seguro       ✅ Privado
```

### Configuración Básica Multi-Tenant

```java
// Función helper para crear agente específico del usuario
function createUserAgent( userId, conversationId ) {
    return aiAgent(
        name: "UserAssistant",
        description: "Asistente personal que recuerda preferencias",
        instructions: "Recuerda preferencias y conversaciones pasadas del usuario.",
        memory: aiMemory( "windowed",
            key: "chat",
            userId: userId,                    // 🔑 Aísla por usuario
            conversationId: conversationId,    // 🔑 Múltiples chats por usuario
            config: { maxMessages: 20 }
        )
    )
}

// En tu aplicación web:
function handleChatRequest( event, rc, prc ) {
    // Obtener usuario autenticado de la sesión
    currentUserId = auth().user().getId()  // ej: "user-123"
    chatId = rc.chatId ?: "default"        // ej: "support-chat"

    // Crear agente con memoria aislada
    agente = createUserAgent( currentUserId, chatId )

    // Procesar mensaje
    respuesta = agente.run( rc.message )

    return { response: respuesta, userId: currentUserId, chatId: chatId }
}
```

### Múltiples Conversaciones por Usuario

Un usuario puede tener múltiples chats simultáneos:

```java
// Usuario tiene 3 chats diferentes:
// 1. Chat de soporte
agentesoporte = createUserAgent( "user-123", "support-chat" )
agentesoporte.run( "Necesito ayuda con mi orden" )

// 2. Chat de ventas
agenteVentas = createUserAgent( "user-123", "sales-chat" )
agenteVentas.run( "Dime sobre planes empresariales" )

// 3. Chat técnico
agenteTecnico = createUserAgent( "user-123", "technical-chat" )
agenteTecnico.run( "¿Cómo integro la API?" )

// Cada chat tiene memoria completamente separada!
```

### Patrón de Aplicación Web

```java
// handler-chat.cfc

class {
    property name="cacheService" inject="cachebox:default";

    function chat( event, rc, prc ) {
        // 1. Obtener usuario autenticado
        userId = auth().user().getId()
        tenantId = auth().user().getTenantId()  // Para apps multi-tenant

        // 2. Obtener o crear ID de conversación
        conversationId = rc.chatId ?: createUUID()

        // 3. Cachear agente por usuario + conversación (opcional, para rendimiento)
        cacheKey = "agent-#userId#-#conversationId#"
        agente = cacheService.getOrSet( cacheKey, () => {
            return createUserAgent( userId, tenantId, conversationId )
        }, 60 )  // Cachea por 60 minutos

        // 4. Procesar mensaje
        respuesta = agente.run( rc.message )

        // 5. Retornar respuesta
        return {
            response: respuesta,
            conversationId: conversationId,
            timestamp: now()
        }
    }

    private function createUserAgent( userId, tenantId, conversationId ) {
        return aiAgent(
            name: "CustomerAssistant",
            instructions: "Ayuda al cliente con sus preguntas.",
            memory: aiMemory( "cache",  // Usar cache para apps multi-servidor
                key: "chat",
                userId: userId,
                conversationId: conversationId,
                config: {
                    maxMessages: 50,
                    tenant: tenantId  // Aislamiento adicional por tenant
                }
            )
        )
    }
}
```

### Memoria Respaldada por Base de Datos Multi-Tenant

Para **persistencia empresarial de grado de producción**:

```java
// Schema de base de datos
/*
CREATE TABLE ai_conversations (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_conv (user_id, conversation_id),
    INDEX idx_tenant (tenant_id)
)
*/

function createDatabaseAgent( userId, tenantId, conversationId ) {
    return aiAgent(
        name: "EnterpriseAssistant",
        instructions: "Asistente empresarial profesional.",
        memory: aiMemory( "jdbc",
            key: "chat",
            userId: userId,
            conversationId: conversationId,
            config: {
                datasource: "myDB",
                tableName: "ai_conversations",
                maxMessages: 100,
                // Columnas personalizadas para aislamiento de tenant
                additionalColumns: {
                    tenant_id: tenantId
                }
            }
        )
    )
}
```

### Mejores Prácticas de Seguridad

```java
// ✅ BUENO: Validar siempre userId de sesión autenticada
function secureChat( event, rc, prc ) {
    // NO confíes en userId del cliente
    userId = auth().user().getId()  // ✅ Del servidor, seguro

    // Validar que el usuario tiene acceso a esta conversación
    if ( rc.keyExists( "chatId" ) ) {
        validateUserOwnsConversation( userId, rc.chatId )
    }

    agente = createUserAgent( userId, rc.chatId ?: "default" )
    return agente.run( rc.message )
}

// ❌ MALO: Nunca confíes en userId del cliente
function insecureChat( event, rc, prc ) {
    userId = rc.userId  // ❌ ¡Puede ser falsificado!
    agente = createUserAgent( userId, rc.chatId )
    return agente.run( rc.message )
}
```

### Componente de Tabla de Usuario Específico de Tenant

Para **máximo aislamiento**, usa tablas específicas de tenant:

```java
function createIsolatedAgent( userId, tenantId, conversationId ) {
    return aiAgent(
        name: "IsolatedAssistant",
        instructions: "Asistente completamente aislado.",
        memory: aiMemory( "jdbc",
            key: "chat",
            userId: userId,
            conversationId: conversationId,
            config: {
                datasource: "myDB",
                // Tabla dinámica por tenant
                tableName: "ai_conv_#tenantId#",  // ej: ai_conv_acme, ai_conv_widgets
                maxMessages: 100
            }
        )
    )
}
```

### Visualización: Aislamiento de Memoria

```
┌─────────────────────────────────────────────────────────────────┐
│                  AISLAMIENTO DE MEMORIA                         │
└─────────────────────────────────────────────────────────────────┘

NIVEL 1: Tenant
───────────────
  Tenant A                    Tenant B
  ┌──────────┐               ┌──────────┐
  │ Usuario 1│               │ Usuario 3│
  │ Usuario 2│               │ Usuario 4│
  └──────────┘               └──────────┘
       │                          │
       ▼                          ▼
  [Datos A]                  [Datos B]
  Completamente separado

NIVEL 2: Usuario + Conversación
────────────────────────────────
  Usuario 1
  ├─ Chat Soporte    → Memoria A
  ├─ Chat Ventas     → Memoria B
  └─ Chat Técnico    → Memoria C
     Cada chat está aislado
```

### Ejemplo: Sistema de Soporte al Cliente

```java
// Sistema de soporte completo con multi-tenancy
class {
    function initSupport( userId, tenantId ) {
        // Crear agente de soporte específico del usuario
        this.agent = aiAgent(
            name: "SupportAgent",
            instructions: "
                Eres un agente de soporte profesional.
                - Sé cortés y útil
                - Recuerda el historial de conversación del usuario
                - Escala problemas complejos
            ",
            tools: [
                lookupOrderTool,
                checkInventoryTool,
                createTicketTool
            ],
            memory: aiMemory( "cache",
                key: "support",
                userId: userId,
                conversationId: "support-main",
                config: {
                    maxMessages: 50,
                    tenant: tenantId,
                    // Limpiar conversación después de 24 horas
                    ttl: 86400
                }
            )
        )

        return this
    }

    function chat( message ) {
        return this.agent.run( message )
    }

    function getHistory() {
        return this.agent.getMemory().getMessages()
    }

    function clearHistory() {
        this.agent.clearMemory()
    }
}

// Uso:
support = new SupportChat().initSupport(
    userId: "user-123",
    tenantId: "acme-corp"
)

support.chat( "¿Cuál es el estado de mi orden?" )
support.chat( "¿Puedo cambiar la dirección de envío?" )
```

### Puntos Clave 🎯

| Concepto | Descripción |
|----------|-------------|
| **userId** | Aísla memoria por usuario - **SIEMPRE requerido en producción** |
| **conversationId** | Permite múltiples chats por usuario |
| **tenantId** | Aislamiento de nivel empresarial en `config.additionalColumns` |
| **Tipos de Memoria** | `cache`/`jdbc` mejores para multi-tenant; `windowed` solo desarrollo |
| **Seguridad** | Siempre obtener userId del servidor, nunca confiar en el cliente |

> 💡 **Nota de Producción**: Para aplicaciones web de producción, usa siempre `cache` o `jdbc` memoria con parámetros `userId` y `conversationId`. ¡Nunca uses memoria simple `windowed` sin estos parámetros en entornos multi-usuario!

---

## 🌊 Parte 4: Respuestas de Agente en Streaming (15 mins)

Los agentes soportan **streaming** igual que el chat - perfecto para UIs interactivas.

### Streaming Básico de Agente

```java
// agent-streaming-basic.bxs

agente = aiAgent(
    name: "Poet",
    description: "Un poeta que escribe versos hermosos",
    instructions: "Escribe poesía concisa y hermosa."
)

// Streaming palabra por palabra
agente.stream(
    "Escribe un poema corto sobre BoxLang",
    ( chunk ) => {
        print( chunk )  // Salida en tiempo real
    }
)

// Salida (aparece progresivamente):
// BoxLang
// brings
// joy
// to
// coding...
```

### Streaming con Herramientas

Los agentes llaman herramientas durante el streaming - puedes detectar estas llamadas:

```java
// agent-streaming-tools.bxs

weatherTool = aiTool(
    "get_weather",
    "Obtener clima actual para una ciudad",
    ( args ) => {
        // Simular llamada API de clima
        return "Soleado, 72°F en #args.city#"
    }
).describeCity( "Ciudad para verificar clima" )

agente = aiAgent(
    name: "WeatherBot",
    instructions: "Ayuda a los usuarios con información del clima.",
    tools: [ weatherTool ]
)

agente.stream(
    "¿Cómo está el clima en Boston?",
    ( chunk ) => {
        // Detectar llamadas a herramientas vs texto
        if ( chunk.contains( "get_weather" ) ) {
            println( "\n[🔧 Llamando herramienta de clima...]" )
        } else {
            print( chunk )
        }
    }
)
```

### Streaming con Memoria

La memoria funciona perfectamente con streaming:

```java
// agent-streaming-memory.bxs

agente = aiAgent(
    name: "Assistant",
    instructions: "Asistente útil que recuerda conversaciones.",
    memory: aiMemory( "windowed", { maxMessages: 10 } )
)

// Primera interacción
agente.stream( "Mi color favorito es azul", ( chunk ) => print( chunk ) )
println( "\n" )

// Segunda interacción - recuerda del contexto
agente.stream( "¿Cuál es mi color favorito?", ( chunk ) => print( chunk ) )
// Salida: "Tu color favorito es azul!"
```

### Patrón de Aplicación Web con Streaming

Para UIs de chat en tiempo real:

```java
// handler-streaming-chat.cfc

function streamChat( event, rc, prc ) {
    // 1. Configurar headers SSE (Server-Sent Events)
    event.setHTTPHeader( name="Content-Type", value="text/event-stream" )
    event.setHTTPHeader( name="Cache-Control", value="no-cache" )
    event.setHTTPHeader( name="Connection", value="keep-alive" )
    event.setHTTPHeader( name="X-Accel-Buffering", value="no" )  // Nginx

    // 2. Obtener usuario y crear agente
    userId = auth().user().getId()
    conversationId = rc.chatId ?: "default"

    agente = createUserAgent( userId, conversationId )

    // 3. Transmitir respuesta
    agente.stream(
        rc.message,
        ( chunk ) => {
            // Enviar cada fragmento como evento SSE
            writeOutput( "data: #encodeForJSON( chunk )#\n\n" )
            flush  // Forzar envío inmediato al navegador
        }
    )

    // 4. Enviar evento de finalización
    writeOutput( "data: [DONE]\n\n" )
    flush
}
```

### Procesamiento Asíncrono con runAsync()

Para tareas de agente en segundo plano:

```java
// agent-async.bxs

agente = aiAgent(
    name: "DataAnalyzer",
    instructions: "Analiza conjuntos de datos grandes y proporciona insights.",
    tools: [ loadDataTool, analyzeTool, generateReportTool ]
)

// Iniciar análisis en segundo plano
println( "Iniciando análisis de datos..." )
future = agente.runAsync(
    "Analiza el dataset de ventas del Q4 y genera un reporte completo"
)

println( "Análisis ejecutándose en segundo plano, haciendo otro trabajo..." )

// Hacer otro trabajo aquí...
performOtherTasks()

// Esperar resultado cuando esté listo
println( "Esperando resultados del análisis..." )
reporte = future.get()  // Bloquea hasta completar

println( "Reporte: " & reporte )
```

### Comparación: Patrones de Ejecución

| Característica | `agent.run()` | `agent.stream()` | `agent.runAsync()` |
|----------------|---------------|------------------|--------------------|
| **Bloqueo** | ✅ Sí (espera) | ✅ Sí (pero streaming) | ❌ No (non-blocking) |
| **Feedback** | ⏳ Al final | ⚡ Progresivo | 🎯 Sin feedback hasta get() |
| **Caso de uso** | Scripts simples | UIs de chat | Tareas en segundo plano |
| **Experiencia UX** | Spinner de carga | Escritura en tiempo real | Sin bloqueo |
| **Complejidad** | 🟢 Simple | 🟡 Requiere callback | 🟠 Requiere futures |

### Tabla de Decisión: ¿Cuándo Usar Cada Patrón?

| Escenario | Patrón Recomendado | Razón |
|-----------|-------------------|-------|
| **Script CLI** | `run()` | Simple, directo |
| **Chat UI Web** | `stream()` | Feedback en tiempo real |
| **Tarea en segundo plano** | `runAsync()` | Sin bloqueo |
| **Análisis de datos largo** | `runAsync()` | Libera hilo principal |
| **Bot de chat simple** | `run()` | Suficientemente rápido |
| **Asistente IA empresarial** | `stream()` | Experiencia profesional |
| **Procesamiento batch** | `runAsync()` | Procesamiento paralelo |

---

## 🛠️ Parte 5: Ejemplo Completo de Agente (20 mins)

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
        userInput = cliRead( "Tú: " )

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

## 🧪 Parte 6: Laboratorio - Construye Tu Propio Agente (20 mins)

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
        userInput = cliRead( "Tú: " )

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
| **Memoria Multi-Tenant** | Aísla memoria por userId y conversationId |
| **Instrucciones** | Guía el comportamiento del agente |
| **Herramientas** | Acciones que el agente puede tomar |
| **agent.stream()** | Transmite respuestas en tiempo real |
| **agent.runAsync()** | Ejecuta agentes en segundo plano |

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

// Agente multi-tenant
function createUserAgent( userId, conversationId ) {
    return aiAgent(
        name: "Assistant",
        memory: aiMemory( "cache",
            userId: userId,
            conversationId: conversationId
        )
    )
}

// Streaming
agente.stream( "pregunta", ( chunk ) => print( chunk ) )

// Async
future = agente.runAsync( "tarea larga" )
resultado = future.get()
```

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

### Construye Algo

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
