# Lección 8: Servidores y Clientes MCP

**⏱️ Duración: 60 minutos**

¡Bienvenido al Protocolo de Contexto de Modelo (MCP)! En esta lección final, aprenderás cómo construir servidores MCP que exponen herramientas a clientes de IA, y clientes MCP que se conectan a servidores MCP externos, permitiendo a tus aplicaciones de IA acceder a capacidades remotas.

## 🎯 Lo que Aprenderás

- Entender el Protocolo de Contexto de Modelo (MCP)
- Crear clientes MCP para conectarse a servidores
- Construir tu primer servidor MCP
- Exponer herramientas vía MCP
- Elegir entre transportes HTTP y STDIO
- Integrar servidores MCP con agentes de IA

---

## 📚 Parte 1: Entendiendo MCP (10 mins)

### ¿Qué es el Protocolo de Contexto de Modelo?

MCP es un **protocolo estandarizado** que permite a las aplicaciones de IA conectarse a herramientas externas, fuentes de datos y capacidades.

```
┌─────────────────────────────────────────────────────────────────┐
│                    ARQUITECTURA MCP                             │
└─────────────────────────────────────────────────────────────────┘

  ┌─────────────┐                              ┌─────────────┐
  │ Cliente IA  │                              │Servidor MCP │
  │ (Tu App)    │                              │ (Remoto)    │
  └──────┬──────┘                              └──────┬──────┘
         │                                            │
         │  1. "Listar herramientas disponibles"      │
         │───────────────────────────────────────────▶│
         │                                            │
         │  2. [herramientaClima, herramientaAcciones]│
         │◀───────────────────────────────────────────│
         │                                            │
         │  3. "Llamar herramientaClima(ciudad: Boston)"│
         │───────────────────────────────────────────▶│
         │                                            │
         │  4. { temp: 72, condiciones: "soleado" }   │
         │◀───────────────────────────────────────────│
         │                                            │
         ▼                                            ▼


  💡 Piensa en MCP como "estándares API para herramientas de IA"
```

### ¿Por qué MCP?

**Sin MCP (APIs Tradicionales):**
```javascript
// Integración personalizada para cada servicio
apiClima = new APIClima( claveApi )
apiAcciones = new APIAcciones( claveApi )
apiNoticias = new APINoticias( claveApi )

// Diferentes interfaces para cada una
clima = apiClima.obtenerClimaActual( "Boston" )
acciones = apiAcciones.obtenerCotizacion( "AAPL" )
noticias = apiNoticias.obtenerTitulares( "tecnologia" )
```

**Con MCP (Protocolo Estandarizado):**
```javascript
// Interfaz uniforme para todos los servicios
clienteMCP = MCP( "http://servidor-multi-servicio.com" )

// Descubrir capacidades
herramientas = clienteMCP.listTools()

// Llamar cualquier herramienta de la misma manera
clima = clienteMCP.send( "herramientaClima", { ciudad: "Boston" } )
acciones = clienteMCP.send( "herramientaAcciones", { simbolo: "AAPL" } )
noticias = clienteMCP.send( "herramientaNoticias", { categoria: "tecnologia" } )
```

### Componentes de MCP

```
┌─────────────────────────────────────────────────────────────────┐
│                     CAPACIDADES MCP                             │
└─────────────────────────────────────────────────────────────────┘

  ┌─────────────┐       ┌─────────────┐       ┌─────────────┐
  │🔧 HERRAMIENTAS│     │📚 RECURSOS  │       │💬 PROMPTS   │
  └─────────────┘       └─────────────┘       └─────────────┘
        │                      │                      │
        ▼                      ▼                      ▼
  Funciones que IA        Documentos y            Plantillas
  puede invocar          fuentes de datos         reutilizables


  Ejemplo: Servidor del Clima
  ────────────────────
  Herramientas:   obtenerClimaActual(), obtenerPronostico()
  Recursos:       Datos históricos del clima, reportes climáticos
  Prompts:        "Plantilla reporte del clima", "Plantilla pronóstico"
```

### MCP vs Herramientas Regulares

| Aspecto | Herramientas Regulares | Herramientas MCP |
|---------|----------------------|------------------|
| **Ubicación** | Funciones locales | Servidores remotos |
| **Descubrimiento** | Codificado fijo | Dinámico vía `listTools()` |
| **Protocolo** | Personalizado | JSON-RPC estandarizado |
| **Compartir** | Distribución de código | Endpoint HTTP/STDIO |
| **Actualizaciones** | Redesplegar app | Servidor se actualiza automáticamente |

---

## 🔌 Parte 2: Clientes MCP (15 mins)

### Crear un Cliente MCP

```javascript
// Conectar a un servidor MCP
cliente = MCP( "http://localhost:3000" )

// Descubrir herramientas disponibles
herramientas = cliente.listTools()

// Llamar una herramienta
resultado = cliente.send( "nombreHerramienta", { param: "valor" } )
```

### Ejemplo 1: Conectar a un Servidor MCP

```javascript
// 01-cliente-mcp-basico.bxs

// Conectar a un servidor MCP público (ejemplo)
cliente = MCP( "http://localhost:3000" )

// Descubrir qué herramientas están disponibles
println( "Descubriendo herramientas..." )
respuestaHerramientas = cliente.listTools()

if ( respuestaHerramientas.getSuccess() ) {
    herramientas = respuestaHerramientas.getData()
    
    println( "Herramientas disponibles: #arrayLen( herramientas )#" )
    
    for ( herramienta in herramientas ) {
        println( "- #herramienta.name#: #herramienta.description#" )
    }
} else {
    println( "Error: #respuestaHerramientas.getError()#" )
}
```

### Configuración del Cliente

```javascript
// Configurar tiempo de espera (milisegundos)
cliente = MCP( "http://localhost:3000" )
    .withTimeout( 5000 )  // tiempo de espera de 5 segundos

// Agregar autenticación
cliente = MCP( "http://localhost:3000" )
    .withBearerToken( "tu-token-api" )

// O autenticación básica
cliente = MCP( "http://localhost:3000" )
    .withAuth( "usuario", "contraseña" )

// Agregar encabezados personalizados
cliente = MCP( "http://localhost:3000" )
    .withHeaders( {
        "X-API-Key": "tu-clave",
        "X-Custom": "valor"
    } )
```

### Ejemplo 2: Usar Herramientas MCP

```javascript
// 02-cliente-mcp-llamar-herramienta.bxs

cliente = MCP( "http://localhost:3000" )
    .withTimeout( 10000 )

// Llamar una herramienta del clima
respuesta = cliente.send( "obtenerClima", {
    ciudad: "Boston",
    unidades: "celsius"
} )

if ( respuesta.getSuccess() ) {
    clima = respuesta.getData()
    println( "Clima en Boston:" )
    println( "Temperatura: #clima.temperatura#°C" )
    println( "Condiciones: #clima.condiciones#" )
} else {
    println( "Error: #respuesta.getError()#" )
}
```

### Manejo de Respuestas

Todos los métodos del cliente MCP devuelven un objeto de respuesta:

```javascript
respuesta = cliente.send( "nombreHerramienta", args )

// Verificar si fue exitoso
if ( respuesta.getSuccess() ) {
    datos = respuesta.getData()
    println( "Éxito: #datos#" )
} else {
    println( "Error: #respuesta.getError()#" )
    println( "Estado: #respuesta.getStatusCode()#" )
}
```

---

## 🌐 Parte 3: Construir un Servidor MCP (20 mins)

### Transportes de Servidor

Los servidores MCP pueden usar dos tipos de transporte:

```
┌─────────────────────────────────────────────────────────────────┐
│                  COMPARACIÓN DE TRANSPORTES                     │
└─────────────────────────────────────────────────────────────────┘

  Transporte HTTP                   Transporte STDIO
  ──────────────                    ───────────────
  ✅ Basado en web                  ✅ Herramientas de línea de comandos
  ✅ Estilo REST API                ✅ Apps de escritorio (Claude, VS Code)
  ✅ Clientes de navegador          ✅ Proceso a proceso
  ✅ Acceso remoto                  ✅ Ejecución local
  
  POST http://localhost/mcp.bxm    stdin → Servidor MCP → stdout
```

### Crear un Servidor MCP

```javascript
// Obtener o crear una instancia de servidor MCP
servidorMCP = mcpServer( "miServidor" )

// Registrar herramientas
servidorMCP.registerTool(
    aiTool()
        .name( "nombreHerramienta" )
        .description( "Qué hace esta herramienta" )
        .arguments( { param: "string" } )
        .execute( ( args ) => {
            return "Resultado de la herramienta"
        } )
)
```

### Ejemplo 3: Tu Primer Servidor MCP

```javascript
// 03-primer-servidor-mcp.bxs

// Crear servidor
servidorMCP = mcpServer( "servidorMatematico" )

// Registrar una herramienta calculadora
servidorMCP.registerTool(
    aiTool()
        .name( "calcular" )
        .description( "Realiza operaciones matemáticas básicas" )
        .arguments( {
            operacion: "string - add, subtract, multiply, divide",
            a: "numeric - primer número",
            b: "numeric - segundo número"
        } )
        .execute( ( args ) => {
            switch ( args.operacion ) {
                case "add":
                    return args.a + args.b
                case "subtract":
                    return args.a - args.b
                case "multiply":
                    return args.a * args.b
                case "divide":
                    return args.a / args.b
                default:
                    throw( message: "Operación desconocida: #args.operacion#" )
            }
        } )
)

println( "✅ Servidor MCP Matemático registrado" )
println( "Herramientas disponibles: #arrayLen( servidorMCP.getTools() )#" )

// Listar herramientas
for ( herramienta in servidorMCP.getTools() ) {
    println( "- #herramienta.name#: #herramienta.description#" )
}
```

### Endpoint HTTP

Para exponer tu servidor MCP vía HTTP, crea un archivo de endpoint:

```javascript
// www/mcp.bxm
<bx:script>
import bxModules.bxai.models.mcp.MCPRequestProcessor

// Manejar la solicitud HTTP
MCPRequestProcessor::startHttp()
</bx:script>
```

**Acceder a tu servidor:**
```bash
# Listar herramientas
POST http://localhost/mcp.bxm
{ "method": "tools/list" }

# Llamar una herramienta
POST http://localhost/mcp.bxm
{
    "method": "tools/call",
    "params": {
        "name": "calcular",
        "arguments": { "operacion": "add", "a": 5, "b": 3 }
    }
}
```

### Ejemplo 4: Servidor MCP del Clima

```javascript
// 04-servidor-mcp-clima.bxs

servidorMCP = mcpServer( "servidorClima" )

// Datos simulados del clima (en app real, llamar API)
datosClima = {
    "Boston": { temp: 22, condiciones: "Soleado", humedad: 65 },
    "Nueva York": { temp: 20, condiciones: "Nublado", humedad: 70 },
    "Miami": { temp: 29, condiciones: "Parcialmente Nublado", humedad: 80 }
}

// Registrar herramienta del clima
servidorMCP.registerTool(
    aiTool()
        .name( "obtenerClima" )
        .description( "Obtener el clima actual de una ciudad" )
        .arguments( {
            ciudad: "string - El nombre de la ciudad (Boston, Nueva York, Miami)"
        } )
        .execute( ( args ) => {
            if ( structKeyExists( datosClima, args.ciudad ) ) {
                return datosClima[ args.ciudad ]
            } else {
                return { error: "Ciudad no encontrada. Ciudades disponibles: Boston, Nueva York, Miami" }
            }
        } )
)

// Registrar herramienta de pronóstico
servidorMCP.registerTool(
    aiTool()
        .name( "obtenerPronostico" )
        .description( "Obtener pronóstico del tiempo a 3 días" )
        .arguments( {
            ciudad: "string - El nombre de la ciudad"
        } )
        .execute( ( args ) => {
            return [
                { dia: "Hoy", max: 24, min: 16, condiciones: "Soleado" },
                { dia: "Mañana", max: 22, min: 14, condiciones: "Nublado" },
                { dia: "Día 3", max: 21, min: 13, condiciones: "Lluvia" }
            ]
        } )
)

println( "✅ Servidor MCP del Clima listo" )
println( "Herramientas registradas:" )
for ( herramienta in servidorMCP.getTools() ) {
    println( "- #herramienta.name#" )
}
```

### 🧪 Lab 1: Construir un Servidor MCP (10 mins)

**Tarea:** Crear un servidor MCP con 2-3 herramientas personalizadas.

**Ideas:**
- **Herramientas de Texto** - `invertirTexto()`, `contarPalabras()`, `aMayusculas()`
- **Herramientas de Datos** - `convertirUnidades()`, `formatearFecha()`, `validarEmail()`
- **Herramientas Útiles** - `generarUUID()`, `hashTexto()`, `numeroAleatorio()`

**Requisitos:**
1. Crear `mcpServer()` con un nombre
2. Registrar al menos 2 herramientas con `registerTool()`
3. Cada herramienta debe tener descripción clara y argumentos
4. Probar localmente llamando herramientas directamente

**Código de inicio en `labs/lab-01-construir-servidor-mcp.bxs`**

---

## 🤖 Parte 4: Integrar MCP con Agentes (10 mins)

### Herramientas MCP en Agentes de IA

Las herramientas MCP pueden usarse con agentes de IA igual que las herramientas regulares:

```javascript
// Opción 1: Auto-descubrir herramientas desde servidor MCP
agente = aiAgent()
    .withInstructions( "Eres un asistente útil" )
    .withMCPServer( "http://localhost:3000" )
    .build()

// Opción 2: Agregar manualmente herramientas MCP específicas
clienteMCP = MCP( "http://localhost:3000" )
herramientas = clienteMCP.listTools().getData()

agente = aiAgent()
    .withInstructions( "Eres un asistente útil" )
    .withTools( herramientas )
    .build()
```

### Ejemplo 5: Agente con Servidor MCP

```javascript
// 05-agente-con-mcp.bxs

// Crear servidor MCP con herramientas de utilidad
servidorMCP = mcpServer( "utilidades" )

servidorMCP.registerTool(
    aiTool()
        .name( "obtenerHoraActual" )
        .description( "Obtener la hora actual" )
        .execute( () => now() )
)

servidorMCP.registerTool(
    aiTool()
        .name( "generarUUID" )
        .description( "Generar un UUID aleatorio" )
        .execute( () => createUUID() )
)

// Obtener las herramientas
herramientas = servidorMCP.getTools()

// Crear agente con herramientas MCP
agente = aiAgent()
    .withInstructions( "Eres un asistente de utilidades útil. Usa las herramientas disponibles cuando sea necesario." )
    .withTools( herramientas )
    .build()

// Probar el agente
println( agente.run( "¿Qué hora es?" ) )
println( agente.run( "Genera un UUID para mí" ) )
println( agente.run( "Genera 3 UUIDs y dime la hora actual" ) )
```

---

## 🔐 Parte 5: Servidores MCP de Producción (5 mins)

### Mejores Prácticas de Seguridad

```javascript
// Agregar autenticación al servidor MCP
servidorMCP = mcpServer( "servidorSeguro", {
    requireAuth: true,
    apiKeys: [ "tu-clave-secreta-1", "tu-clave-secreta-2" ]
} )

// Requerir encabezados específicos
servidorMCP.setAuthHandler( ( headers ) => {
    claveApi = headers[ "X-API-Key" ] ?: ""
    return arrayContains( getApiKeys(), claveApi )
} )
```

### Limitación de Tasa

```javascript
// Configurar límites de tasa
servidorMCP = mcpServer( "servidorLimitado", {
    rateLimit: {
        maxRequests: 100,
        windowSeconds: 60
    }
} )
```

### Configuración CORS

```javascript
// Configurar CORS para clientes web
servidorMCP = mcpServer( "servidorWeb", {
    cors: {
        allowOrigin: [ "https://miapp.com", "https://app.ejemplo.com" ],
        allowMethods: [ "POST", "OPTIONS" ],
        allowHeaders: [ "Content-Type", "Authorization" ]
    }
} )
```

---

## 🌍 Parte 6: Ejemplo del Mundo Real (5 mins)

### Ejemplo 6: Servidor MCP Multi-Servicio

```javascript
// 06-mcp-multi-servicio.bxs

servidorMCP = mcpServer( "multiServicio" )

// Servicio de usuarios
servidorMCP.registerTool(
    aiTool()
        .name( "obtenerUsuario" )
        .description( "Obtener información de usuario por ID" )
        .arguments( { idUsuario: "numeric - ID del usuario" } )
        .execute( ( args ) => {
            // En app real, consultar base de datos
            return {
                id: args.idUsuario,
                nombre: "Juan Pérez",
                email: "juan@ejemplo.com",
                fechaCreacion: "2024-01-15"
            }
        } )
)

// Servicio de pedidos
servidorMCP.registerTool(
    aiTool()
        .name( "obtenerPedidosUsuario" )
        .description( "Obtener historial de pedidos del usuario" )
        .arguments( { idUsuario: "numeric - ID del usuario" } )
        .execute( ( args ) => {
            return [
                { idPedido: 1001, total: 99.99, estado: "Entregado" },
                { idPedido: 1002, total: 149.99, estado: "Enviado" }
            ]
        } )
)

// Servicio de analíticas
servidorMCP.registerTool(
    aiTool()
        .name( "obtenerEstadisticasUsuario" )
        .description( "Obtener estadísticas y métricas del usuario" )
        .arguments( { idUsuario: "numeric - ID del usuario" } )
        .execute( ( args ) => {
            return {
                totalPedidos: 12,
                totalGastado: 1234.56,
                valorPromedioPedido: 102.88,
                fechaUltimoAcceso: "2024-12-15"
            }
        } )
)

println( "✅ Servidor MCP Multi-Servicio listo" )
println( "Registrado #arrayLen( servidorMCP.getTools() )# herramientas" )

// Crear agente que usa las herramientas MCP
agente = aiAgent()
    .withInstructions( "Eres un agente de servicio al cliente. Usa las herramientas disponibles para ayudar a los clientes." )
    .withTools( servidorMCP.getTools() )
    .build()

// Consulta del cliente
println( "\n" & agente.run( "Cuéntame sobre el usuario 12345 - su información, pedidos y estadísticas" ) )
```

### 🧪 Lab 2: Sistema MCP Completo (10 mins)

**Tarea:** Construir un servidor MCP completo y conectarlo con un agente de IA.

**Requisitos:**
1. Crear un servidor MCP con al menos 3 herramientas relacionadas (ej: catálogo productos, inventario, precios)
2. Crear un endpoint HTTP (`mcp.bxm`) para exponer el servidor
3. Crear un cliente MCP que se conecte a tu servidor
4. Crear un agente de IA que use tus herramientas MCP
5. Probar con consultas complejas de múltiples pasos

**Código de inicio en `labs/lab-02-sistema-mcp-completo.bxs`**

---

## 🎯 Resumen

### Lo que Aprendiste

✅ **Conceptos Básicos MCP** - Entender el Protocolo de Contexto de Modelo  
✅ **Clientes MCP** - Conectarse y usar servidores MCP  
✅ **Servidores MCP** - Construir y exponer herramientas vía MCP  
✅ **Transportes** - Comunicación HTTP y STDIO  
✅ **Integración con Agentes** - Usar herramientas MCP con agentes de IA  
✅ **Producción** - Seguridad, limitación de tasa, CORS  

### BIFs y Componentes Clave

| Función | Propósito |
|---------|-----------|
| `MCP()` | Crear cliente MCP |
| `mcpServer()` | Crear u obtener servidor MCP |
| `registerTool()` | Agregar herramientas al servidor MCP |
| `listTools()` | Descubrir capacidades del servidor |
| `send()` | Llamar herramientas del servidor MCP |

### Beneficios de MCP

- 🔌 **Estandarización** - Protocolo uniforme para todas las herramientas
- 🌐 **Acceso Remoto** - Herramientas en cualquier lugar, no solo local
- 🔍 **Descubrimiento** - Detección dinámica de herramientas
- 🔄 **Actualizaciones** - Servidor se actualiza sin redesplegar clientes
- 🤝 **Compartir** - Distribución fácil de herramientas

---

## 📝 Recursos

- [Documentación Cliente MCP](../../../docs/advanced/mcp-client.md)
- [Documentación Servidor MCP](../../../docs/advanced/mcp-server.md)
- [Especificación Oficial MCP](https://modelcontextprotocol.io/)
- [Documentación Completa BoxLang AI](../../../docs/README.md)

---

**🎉 ¡Felicitaciones!** ¡Has completado el Bootcamp de BoxLang AI!

### 🚀 ¿Qué Sigue?

1. **Construir Proyectos Reales** - Aplica lo que aprendiste
2. **Explorar Temas Avanzados**:
   - Proveedores y transformadores personalizados
   - RAG avanzado con memoria vectorial
   - Flujos de trabajo de pipelines
   - Despliegue en producción
3. **Únete a la Comunidad** - Comparte tus proyectos y obtén ayuda
4. **Lee la Documentación Completa** - Profundiza en cada característica

---

**¡Ahora tienes las habilidades para construir aplicaciones de IA listas para producción con BoxLang!** 🌟

👉 **[Volver al Resumen del Bootcamp](../README.md)**
