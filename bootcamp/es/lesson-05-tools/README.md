# Lección 5: Herramientas de IA

**⏱️ Duración: 60 minutos**

Hasta ahora, la IA solo ha usado su conocimiento de entrenamiento. Pero ¿qué pasa si la IA pudiera **llamar a tus funciones** para obtener datos en tiempo real, realizar cálculos o interactuar con sistemas externos? ¡Eso es lo que las **herramientas** permiten!

## 🎯 Lo que Aprenderás

- Entender las llamadas a funciones de IA
- Crear herramientas con `aiTool()`
- Describir argumentos de herramientas
- Dejar que la IA use múltiples herramientas
- Manejar resultados de herramientas

---

## 📚 Parte 1: ¿Qué Son las Herramientas de IA? (10 mins)

### El Problema

El conocimiento de la IA está congelado en el momento del entrenamiento. No puede:
- Verificar el clima de hoy
- Buscar datos en vivo
- Acceder a tu base de datos
- Realizar cálculos reales

### La Solución: Herramientas

Las **herramientas** son funciones que la IA puede llamar cuando necesita información:

```
┌─────────────────────────────────────────────────────────────────┐
│                  CÓMO FUNCIONAN LAS HERRAMIENTAS                │
└─────────────────────────────────────────────────────────────────┘

  Usuario: "¿Cuál es el clima en Boston?"
         │
         ▼
  ┌──────────────┐
  │     IA       │  "Necesito datos del clima..."
  │   piensa     │         │
  └──────────────┘         │
         │                 ▼
         │        ┌───────────────┐
         │        │  LLAMADA A    │
         │        │  HERRAMIENTA  │
         │        │ get_weather() │
         │        │ city="Boston" │
         │        └───────────────┘
         │                 │
         │                 ▼
         │        ┌───────────────┐
         │        │  Tu Función   │
         │        │ Devuelve: 72°F│
         │        └───────────────┘
         │                 │
         ▼                 ▼
  ┌──────────────────────────────┐
  │  IA: "¡Hace 72°F en Boston!" │
  └──────────────────────────────┘
```

### Por Qué Importan las Herramientas

- ✅ **Datos en tiempo real** - Clima, acciones, noticias
- ✅ **Cálculos** - Matemáticas, conversiones, estadísticas
- ✅ **Acceso a base de datos** - Buscar info de usuarios, productos
- ✅ **APIs externas** - Enviar emails, hacer reservaciones
- ✅ **Lógica personalizada** - Tus reglas de negocio

---

## 💻 Parte 2: Creando Tu Primera Herramienta (15 mins)

### La Función aiTool()

```java
herramienta = aiTool(
    "nombre_herramienta",      // Nombre que la IA usará para llamarla
    "Descripción",             // Explica cuándo usarla
    ( args ) => {              // Función a ejecutar
        // Tu código aquí
        return resultado
    }
)
```

### Ejemplo: Herramienta de Calculadora

```java
// herramienta-calculadora.bxs
herramientaCalculadora = aiTool(
    "calculator",
    "Performs mathematical calculations. Use for any math operations.",
    ( args ) => {
        // Parsea la expresión y calcula
        expression = args.expression
        result = evaluate( expression )
        return "Result: " & result
    }
).describeExpression( "The math expression to calculate, e.g. '2 + 2' or '100 * 0.15'" )

// Usa la herramienta
respuesta = aiChat(
    "What is 15% of 200?",
    { tools: [ herramientaCalculadora ] }
)

println( respuesta )
// Salida: "15% of 200 is 30"
```

### Ejemplo: Herramienta de Clima

```java
// herramienta-clima.bxs
herramientaClima = aiTool(
    "get_weather",
    "Get current weather for a city. Use when asked about weather.",
    ( args ) => {
        city = args.city

        // Datos de clima simulados (en app real, llama a API de clima)
        weatherData = {
            "Boston": { temp: 72, condition: "Sunny" },
            "New York": { temp: 68, condition: "Cloudy" },
            "Miami": { temp: 85, condition: "Hot and humid" }
        }

        if( weatherData.keyExists( city ) ) {
            data = weatherData[ city ]
            return "Weather in #city#: #data.temp#°F, #data.condition#"
        }

        return "Weather data not available for #city#"
    }
).describeCity( "The city name to get weather for" )

// Usa la herramienta
respuesta = aiChat(
    "What's the weather like in Boston today?",
    { tools: [ herramientaClima ] }
)

println( respuesta )
// Salida: "¡El clima en Boston es 72°F y soleado!"
```

---

## 🔧 Parte 3: Argumentos de Herramientas (10 mins)

### Describiendo Argumentos

Usa `describeArg()` o métodos dinámicos para explicar para qué es cada argumento:

```java
// Método 1: describeArg()
herramienta = aiTool( "search", "Search products", fn )
    .describeArg( "query", "The search terms" )
    .describeArg( "category", "Product category to filter by" )
    .describeArg( "maxResults", "Maximum number of results (default 10)" )

// Método 2: Métodos dinámicos (¡más limpio!)
herramienta = aiTool( "search", "Search products", fn )
    .describeQuery( "The search terms" )
    .describeCategory( "Product category to filter by" )
    .describeMaxResults( "Maximum number of results" )
```

### Por Qué Importan las Descripciones

Buenas descripciones ayudan a la IA a usar las herramientas correctamente:

```
❌ MAL: .describeCity( "city" )
   La IA podría pasar "NYC", "new york", "New York City"

✅ BIEN: .describeCity( "City name, e.g. 'Boston' or 'New York'" )
   ¡La IA entiende el formato esperado!
```

---

## 🔗 Parte 4: Múltiples Herramientas (15 mins)

La IA puede decidir qué herramienta usar (o usar múltiples):

### Ejemplo: Asistente Inteligente

```java
// asistente-inteligente.bxs

// Herramienta 1: Clima
herramientaClima = aiTool(
    "get_weather",
    "Get current weather for a city",
    ( args ) => {
        // Datos simulados
        data = { "Boston": 72, "Miami": 85, "Denver": 65 }
        city = args.city
        temp = data[ city ] ?: 70
        return "#temp#°F in #city#"
    }
).describeCity( "City name" )

// Herramienta 2: Calculadora
herramientaCalc = aiTool(
    "calculate",
    "Perform math calculations",
    ( args ) => {
        return evaluate( args.expression )
    }
).describeExpression( "Math expression like '2+2' or '100*0.2'" )

// Herramienta 3: Tiempo
herramientaTiempo = aiTool(
    "get_time",
    "Get current date and time",
    ( args ) => {
        return now().format( "EEEE, MMMM d, yyyy h:mm a" )
    }
)

// ¡La IA puede usar cualquiera de estas herramientas!
println( aiChat(
    "What's the weather in Miami?",
    { tools: [ herramientaClima, herramientaCalc, herramientaTiempo ] }
) )
// Usa herramienta de clima

println( aiChat(
    "What's 25% of 400?",
    { tools: [ herramientaClima, herramientaCalc, herramientaTiempo ] }
) )
// Usa herramienta de calculadora

println( aiChat(
    "What day is it?",
    { tools: [ herramientaClima, herramientaCalc, herramientaTiempo ] }
) )
// Usa herramienta de tiempo
```

### Selección de Herramientas

La IA elige automáticamente la herramienta correcta según la pregunta:

```
┌─────────────────────────────────────────────────────────────────┐
│                    SELECCIÓN DE HERRAMIENTAS                    │
└─────────────────────────────────────────────────────────────────┘

  "¿Clima en Boston?"       ──▶  get_weather( city="Boston" )

  "Calcula propina del 15%" ──▶  calculate( expression="0.15*50" )

  "¿Qué hora es?"           ──▶  get_time()

  "Cuéntame un chiste"      ──▶  (sin herramienta, IA responde directo)
```

---

## 🧪 Parte 5: Laboratorio - Bot del Clima (10 mins)

### El Objetivo

Construir un bot del clima que:
1. Tenga una herramienta de búsqueda de clima
2. Tenga una herramienta de conversión de temperatura
3. Pueda responder preguntas combinadas

### Solución

```java
// bot-clima.bxs

println( "🌤️ Bot del Clima" )
println( "═".repeat( 40 ) )
println()

// Datos de clima (simulados)
weatherData = {
    "Boston": { temp: 72, condition: "Sunny", humidity: 45 },
    "New York": { temp: 68, condition: "Cloudy", humidity: 60 },
    "Miami": { temp: 85, condition: "Hot", humidity: 80 },
    "San Francisco": { temp: 62, condition: "Foggy", humidity: 70 },
    "Denver": { temp: 58, condition: "Clear", humidity: 30 }
}

// Herramienta 1: Obtener clima
weatherTool = aiTool(
    "get_weather",
    "Get current weather for a city. Returns temperature in Fahrenheit.",
    ( args ) => {
        city = args.city

        if( weatherData.keyExists( city ) ) {
            data = weatherData[ city ]
            return "Weather in #city#: #data.temp#°F, #data.condition#, Humidity: #data.humidity#%"
        }

        return "No weather data for #city#. Available cities: #weatherData.keyList()#"
    }
).describeCity( "City name exactly as: Boston, New York, Miami, San Francisco, or Denver" )

// Herramienta 2: Convertir temperatura
convertTool = aiTool(
    "convert_temperature",
    "Convert temperature between Fahrenheit and Celsius",
    ( args ) => {
        temp = args.temperature
        from = args.fromUnit.uCase()

        if( from == "F" || from == "FAHRENHEIT" ) {
            celsius = ( temp - 32 ) * 5/9
            return "#temp#°F = #numberFormat( celsius, '0.0' )#°C"
        } else {
            fahrenheit = ( temp * 9/5 ) + 32
            return "#temp#°C = #numberFormat( fahrenheit, '0.0' )#°F"
        }
    }
)
.describeTemperature( "The temperature value as a number" )
.describeFromUnit( "The unit to convert FROM: 'F' for Fahrenheit or 'C' for Celsius" )

// Herramienta 3: Comparar ciudades
compareTool = aiTool(
    "compare_cities",
    "Compare weather between two cities",
    ( args ) => {
        city1 = args.city1
        city2 = args.city2

        if( !weatherData.keyExists( city1 ) || !weatherData.keyExists( city2 ) ) {
            return "Can't compare - need valid cities"
        }

        data1 = weatherData[ city1 ]
        data2 = weatherData[ city2 ]
        diff = data1.temp - data2.temp

        if( diff > 0 ) {
            return "#city1# is #abs(diff)#°F warmer than #city2#"
        } else if( diff < 0 ) {
            return "#city2# is #abs(diff)#°F warmer than #city1#"
        } else {
            return "#city1# and #city2# are the same temperature"
        }
    }
)
.describeCity1( "First city to compare" )
.describeCity2( "Second city to compare" )

// Todas las herramientas
tools = [ weatherTool, convertTool, compareTool ]

// Bucle de chat
println( "¡Pregúntame sobre el clima! Escribe 'salir' para terminar." )
println( "─".repeat( 40 ) )
println()

running = true
while( running ) {
    print( "Tú: " )
    question = readLine()

    if( question.trim() == "salir" ) {
        running = false
        println( "☀️ ¡Adiós!" )
    } else {
        try {
            answer = aiChat( question, { tools: tools } )
            println( "Bot: " & answer )
            println()
        } catch( any e ) {
            println( "❌ Error: " & e.message )
            println()
        }
    }
}
```

### Interacción de Ejemplo

```
🌤️ Bot del Clima
════════════════════════════════════════

¡Pregúntame sobre el clima! Escribe 'salir' para terminar.
────────────────────────────────────────

Tú: What's the weather in Miami?
Bot: The weather in Miami is 85°F and hot with 80% humidity.

Tú: Convert that to Celsius
Bot: 85°F is 29.4°C.

Tú: Which is warmer, Boston or Denver?
Bot: Boston is 14°F warmer than Denver.

Tú: salir
☀️ ¡Adiós!
```

---

## ✅ Verificación de Conocimientos

1. **¿Qué permiten las herramientas de IA?**
   - [ ] Cambiar la personalidad de la IA
   - [x] La IA puede llamar a tus funciones
   - [ ] Acelerar respuestas
   - [ ] Reducir costos

2. **¿Cómo creas una herramienta?**
   - [x] aiTool( nombre, descripción, función )
   - [ ] createTool()
   - [ ] addFunction()
   - [ ] registerTool()

3. **¿Por qué describir los argumentos de herramientas?**
   - [ ] Requerido por BoxLang
   - [ ] Hace el código más rápido
   - [x] Ayuda a la IA a entender cómo usar la herramienta
   - [ ] Solo para documentación

4. **¿Puede la IA usar múltiples herramientas?**
   - [x] Sí, elige la correcta
   - [ ] No, solo una a la vez
   - [ ] Solo con configuración especial
   - [ ] Solo en pipelines

---

## 📝 Resumen

Aprendiste:

| Concepto | Descripción |
|----------|-------------|
| **Herramienta** | Función que la IA puede llamar |
| **aiTool()** | Crea un objeto de herramienta |
| **describe*()** | Explica argumentos a la IA |
| **tools: []** | Pasa herramientas a aiChat |

### Patrones de Código Clave

```java
// Crear una herramienta
herramienta = aiTool( "nombre", "descripción", ( args ) => {
    return resultado
}).describeArg( "qué es" )

// Usar herramientas
respuesta = aiChat( "pregunta", { tools: [ herramienta1, herramienta2 ] } )
```

---

## ⏭️ Siguiente Lección

¡Ahora puedes darle capacidades reales a la IA! Pongamos todo junto y construyamos agentes autónomos.

👉 **[Lección 6: Construyendo Agentes](../lesson-06-agents/)**

---

## 📁 Archivos de la Lección

```
lesson-05-tools/
├── README.md (este archivo)
├── examples/
│   ├── herramienta-calculadora.bxs
│   ├── herramienta-clima.bxs
│   └── asistente-inteligente.bxs
└── labs/
    └── bot-clima.bxs
```
