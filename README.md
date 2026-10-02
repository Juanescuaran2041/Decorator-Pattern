# PipelineLogsDecorator

Proyecto académico de la asignatura **Patrones de Diseño**. Aplica el patrón estructural
**Decorator** a un pipeline de análisis de eventos de logs de Windows, orientado a la
**detección temprana de ransomware**. De forma complementaria usa **Factory** (para elegir la
fuente de eventos) y **Builder** (para armar el pipeline desde la web).

El proyecto tiene dos partes:

- **Backend (Java)**: toda la lógica del pipeline, la lectura de logs reales de Windows y un
  servidor HTTP mínimo que expone el análisis como JSON.
- **Frontend (HTML + CSS + JavaScript)**: una página web que ejecuta el análisis, permite elegir
  el origen de los eventos, activar o desactivar capas del pipeline y ver eventos y alertas.

## Desarrolladores

- Juan Esteban Cuaran Santander
- Hugo David Burgos Trejo
- Jose Nicolas Mora

## Idea general

Una fuente de eventos se envuelve con varias capas (decoradores). Cada capa:

1. pide el evento a la capa interior,
2. verifica si es `null` (fin de los eventos),
3. le añade su comportamiento,
4. lo devuelve a la capa exterior.

Como todas las capas implementan la misma interfaz (`EventSource`), se pueden quitar, añadir o
reordenar sin modificar las demás. Por la misma razón, la fuente se puede cambiar (datos de
prueba o log real de Windows) sin tocar ningún decorador.

## Restricciones técnicas

- **Backend**: Java puro (compatible con Java 11), proyecto de IntelliJ IDEA. Sin Maven,
  Gradle ni librerías externas. Solo se usa lo que trae el JDK:
  - `com.sun.net.httpserver.HttpServer` para el servidor web,
  - `javax.xml.parsers` para leer el XML de los eventos de Windows,
  - `ProcessBuilder` para ejecutar `wevtutil` (herramienta incluida en Windows).
- **Frontend**: HTML, CSS y JavaScript sin frameworks ni dependencias, usando `fetch`.
- Estructura plana en `src/`: una clase por archivo.
- **Código en inglés** (clases, métodos, variables, valores e interfaz) y **sin comentarios**:
  los nombres deben explicar el código por sí solos.
- Código simple, de nivel de tercer semestre: sin métodos innecesarios ni abstracciones extra.
- `switch` clásico, sin `record`.

## Patrones de diseño

### Decorator (patrón principal)

| Rol del patrón       | Clase              | Responsabilidad |
|----------------------|--------------------|-----------------|
| Component            | `EventSource`      | Interfaz con `Event next()`; devuelve `null` cuando no hay más eventos. |
| ConcreteComponent    | `InMemorySource`   | Recorre una `List<Event>` con un `Iterator` (datos de prueba). |
| ConcreteComponent    | `WindowsLogSource` | Lee eventos reales de Windows con `wevtutil` y los convierte a `Event`. |
| Decorator            | `SourceDecorator`  | Clase abstracta que implementa `EventSource` y guarda `protected final EventSource source`. |
| ConcreteDecorator    | `WithNormalization`| Deja solo el nombre del ejecutable en `process` y lo pasa a minúsculas. |
| ConcreteDecorator    | `WithEnrichment`   | Añade el campo `category` según el Event ID. |
| ConcreteDecorator    | `WithFilter`       | Descarta los eventos con categoría `OTHER`. |
| ConcreteDecorator    | `WithRiskScore`    | Decorador con estado: asigna puntaje de riesgo a cada evento. |
| Datos                | `Event`            | Event ID, mapa de datos y puntaje acumulado. |

```
                     «interface»
                     EventSource
                   + next(): Event
              ▲           ▲              ▲
              │           │              │
     InMemorySource  WindowsLogSource  SourceDecorator (abstracta) ◇── source : EventSource
                                             ▲
            ┌────────────────┬───────────────┴──┬────────────────┐
    WithNormalization  WithEnrichment      WithFilter      WithRiskScore
```

### Factory

`EventSourceFactory.create(origin)` decide qué fuente crear:

- `"test"` (o vacío) → `InMemorySource` con `TestData.generate()`.
- cualquier otro valor (`Security`, `System`, ruta `.evtx`) → `WindowsLogSource`.

Así `Main` y `WebServer` no repiten esa decisión ni conocen las clases concretas de las fuentes.

### Builder

`PipelineBuilder` arma el pipeline a partir de las capas que el usuario activa en la web:

```java
EventSource pipeline = new PipelineBuilder(source)
        .withNormalization()
        .withEnrichment()
        .withFilter()
        .withRiskScore()
        .build();
```

Sin importar el orden en que se llamen los métodos `with...`, `build()` envuelve las capas
siempre en el orden correcto del pipeline. Esto evita que desde la web se pueda armar un orden
inválido (ver *¿Por qué importa el orden de los decoradores?*).

`Main` no usa el builder: ensambla los decoradores a mano para que el patrón Decorator se vea
explícitamente.

### Iterator

`InMemorySource` y `WindowsLogSource` recorren sus eventos con un `Iterator` de Java.

## Clases de apoyo

| Clase                | Responsabilidad |
|----------------------|-----------------|
| `TestData`           | Genera la lista de eventos de prueba. |
| `EventSourceFactory` | Crea la fuente adecuada según el origen. |
| `PipelineBuilder`    | Arma el pipeline con las capas activas, en el orden correcto. |
| `Main`               | Cliente de consola: ensambla el pipeline e imprime alertas y resumen. |
| `WebServer`          | Cliente web: sirve el frontend y expone `/api/analyze`. |

## Arquitectura

```
┌───────────────────────┐   GET /api/analyze?origin=...&layers=...   ┌─────────────────────────────┐
│  Frontend (web/)      │ ─────────────────────────────────────────▶ │  WebServer (Java)           │
│  index.html           │                                            │  1. EventSourceFactory      │
│  styles.css           │ ◀───────────────────────────────────────── │  2. PipelineBuilder         │
│  app.js               │            JSON (events, summary)          │  3. recorre y serializa     │
└───────────────────────┘                                            └─────────────────────────────┘
                                                                                   │
                        WithRiskScore → WithFilter → WithEnrichment → WithNormalization
                                                                                   │
                                                          ┌────────────────────────┴──────────────┐
                                                   InMemorySource                        WindowsLogSource
                                                   (TestData)                            (wevtutil → XML)
```

El mismo servidor entrega los archivos estáticos de `web/`, así que no hace falta otro
servidor ni configurar CORS. El servidor escucha solo en `127.0.0.1`, porque puede leer los
logs reales del equipo.

## Estructura del proyecto

```
PipelineLogsDecorator/
├── src/                        ← Backend (lógica en Java)
│   ├── Event.java
│   ├── EventSource.java
│   ├── InMemorySource.java
│   ├── WindowsLogSource.java
│   ├── SourceDecorator.java
│   ├── WithNormalization.java
│   ├── WithEnrichment.java
│   ├── WithFilter.java
│   ├── WithRiskScore.java
│   ├── TestData.java
│   ├── EventSourceFactory.java
│   ├── PipelineBuilder.java
│   ├── Main.java
│   └── WebServer.java
├── web/                        ← Frontend
│   ├── index.html
│   ├── styles.css
│   └── app.js
├── README.md
├── task.md
└── PipelineLogsDecorator.iml
```

## Detalle de los decoradores

### WithNormalization
Si el evento tiene `process`, conserva solo lo que va después del último `\` y lo pasa a
minúsculas. Ejemplo: `C:\Windows\System32\CMD.EXE` → `cmd.exe`.

### WithEnrichment
Añade `category` según el Event ID de Windows:

| Event ID | Categoría          |
|----------|--------------------|
| 4663     | `FILE_ACCESS`      |
| 4660     | `FILE_DELETE`      |
| 4688     | `PROCESS_CREATION` |
| 1102     | `LOG_CLEARED`      |
| otro     | `OTHER`            |

### WithFilter
Pide eventos en un ciclo hasta encontrar uno cuya categoría no sea `OTHER`, o hasta llegar a
`null`.

### WithRiskScore
| Regla | Puntos |
|-------|--------|
| El proceso supera 100 eventos `FILE_ACCESS` (conteo en `Map<String, Integer>`) | +50 |
| `file` termina en `.locked`, `.encrypted` o `.crypt` | +30 |
| Categoría `LOG_CLEARED` | +50 |

> El conteo es acumulado, sin ventana de tiempo. Es una simplificación intencional. Como el
> código no lleva comentarios, se indica con el nombre del atributo:
> `accessCountWithoutTimeWindow`.

## Pipeline

Orden de ensamblaje (de afuera hacia adentro):

```
WithRiskScore → WithFilter → WithEnrichment → WithNormalization → Source
```

```java
EventSource pipeline =
        new WithRiskScore(
            new WithFilter(
                new WithEnrichment(
                    new WithNormalization(
                        EventSourceFactory.create("test")))));
```

Para analizar un log real solo cambia el origen; los decoradores son los mismos:
`EventSourceFactory.create("Security")`.

## Lectura de logs reales de Windows (evaluación de viabilidad)

El caso de estudio plantea que la fuente en memoria se reemplazará en el futuro por un lector
real. Se evaluó si eso es posible respetando la restricción de no usar librerías externas.

**Conclusión: sí es viable**, con `wevtutil` (incluido en Windows) para extraer los eventos en
XML y `javax.xml` (incluido en el JDK) para leerlos. Gracias al patrón, basta con añadir un
nuevo ConcreteComponent (`WindowsLogSource`); ningún decorador cambia.

### Pruebas realizadas (Windows 11, usuario sin administrador)

| Prueba | Resultado |
|--------|-----------|
| `wevtutil qe Security /c:1 /f:xml` | ❌ `Acceso denegado` (código 5). El log Security requiere administrador. |
| `wevtutil qe System /c:1 /f:xml` | ✅ Devuelve el evento en XML. |
| Exportar con `wevtutil epl` y leer con `wevtutil qe archivo.evtx /lf:true` | ✅ Un `.evtx` se lee sin administrador. |
| Codificación de la salida | ⚠️ No es UTF-8 sino la página de códigos ANSI de Windows (`windows-1252`: `á` = `0xE1`). |
| Opción `/e:Events` | ✅ Envuelve todos los `<Event>` en un elemento raíz, así el XML es válido. |
| Pipeline sobre los logs System y `.evtx` exportado | ✅ Se leen y procesan; no contienen Event IDs de interés, así que el filtro los descarta. |

### Cómo funciona `WindowsLogSource`

1. Ejecuta con `ProcessBuilder` (sin pasar por una shell):
   `wevtutil qe <origin> /c:<max> /rd:true /f:xml /e:Events`, añadiendo `/lf:true` si el
   origen es un archivo `.evtx`.
2. Si el código de salida no es 0, lanza `IllegalStateException` con el mensaje de `wevtutil`
   (por ejemplo, `Acceso denegado`).
3. Decodifica la salida con la página de códigos ANSI (`System.getProperty("sun.jnu.encoding")`).
4. Lee el XML con `DocumentBuilder` (con `DOCTYPE` deshabilitado) y crea un `Event` por cada
   `<Event>`, usando estos campos de `<EventData>`:

   | Campo de Windows                         | Campo del `Event` |
   |------------------------------------------|-------------------|
   | `System/EventID`                         | `id`              |
   | `ProcessName` (4663, 4660)               | `process`         |
   | `NewProcessName` (4688)                  | `process`         |
   | `ObjectName` (4663)                      | `file`            |

5. `/rd:true` trae los más recientes primero; la lista se invierte para procesar en orden
   cronológico.
6. Lee los eventos la primera vez que se llama a `next()` y luego los entrega uno a uno.

### Requisitos para que aparezcan los eventos del caso de estudio

| Event ID | Requisito en Windows |
|----------|----------------------|
| 4663 / 4660 | Directiva *Auditar sistema de archivos* activada **y** una SACL (auditoría) configurada en las carpetas a vigilar. Sin esto Windows no genera estos eventos. |
| 4688 | Directiva *Auditar creación de procesos* activada. |
| 1102 | Se genera siempre al borrar el log de seguridad. |

Ejemplo (consola de administrador):

```bat
auditpol /set /subcategory:"Sistema de archivos" /success:enable
auditpol /set /subcategory:"Creación del proceso" /success:enable
wevtutil epl Security C:\logs\security.evtx
```

El archivo exportado se puede analizar luego sin permisos de administrador.

### Limitaciones

- Solo funciona en Windows.
- No es en tiempo real: lee un lote de los últimos N eventos (por defecto 5000) en cada análisis.
- El evento 4660 no trae `ObjectName`; para saber qué archivo se borró habría que relacionarlo
  con el 4656 por `HandleId`. No se implementa (fuera del alcance).
- El conteo de `WithRiskScore` sigue siendo acumulado, sin ventana de tiempo.

### Alternativas descartadas

| Alternativa | Motivo |
|-------------|--------|
| Interpretar el binario `.evtx` directamente en Java | Formato BinXML complejo; sobreingeniería para el alcance del proyecto. |
| JNA / API Win32 `EvtQuery` | Requiere librería externa. |
| PowerShell `Get-WinEvent` | Viable, pero más lento y depende de PowerShell; `wevtutil` es más simple. |

## API del backend

### `GET /api/analyze`

Crea la fuente con `EventSourceFactory`, arma un pipeline nuevo con `PipelineBuilder` (porque
`WithRiskScore` guarda estado), lo recorre y devuelve el resultado en JSON.

| Parámetro | Descripción | Por defecto |
|-----------|-------------|-------------|
| `origin`  | `test` para los datos de prueba; o un log de Windows (`Security`, `System`) o la ruta de un archivo `.evtx`. | `test` |
| `layers`  | Lista separada por comas de las capas activas: `normalization`, `enrichment`, `filter`, `riskScore`. Se ensamblan siempre en el orden del pipeline. | todas |

Ejemplo: `GET /api/analyze?origin=test&layers=normalization,enrichment,filter,riskScore`

```json
{
  "origin": "test",
  "processed": 129,
  "alerts": 21,
  "events": [
    {
      "id": 4663,
      "category": "FILE_ACCESS",
      "process": "evil.exe",
      "file": "documento_101.docx.locked",
      "score": 80,
      "alert": true
    }
  ]
}
```

- Un evento es `alert` cuando su puntaje es >= 50.
- Los campos que no existen se envían como `null`.
- El JSON se construye a mano escapando `\` y `"` (sin normalización los procesos llegan con
  rutas de Windows).
- Si la fuente falla (por ejemplo, `Security` sin administrador) responde `400` con
  `{"error": "..."}`.

### `GET /`

Sirve solo `index.html`, `styles.css` y `app.js` de la carpeta `web/` (lista cerrada, para no
exponer otros archivos).

## Frontend

La página (`web/index.html`) muestra:

- **Origen de los eventos**: datos de prueba o log real de Windows (campo de texto para
  `Security` o la ruta de un `.evtx`).
- **Panel del pipeline**: las capas en orden, cada una con una casilla para activarla o
  desactivarla, y el botón **Run analysis**.
- **Resumen**: tarjetas con eventos procesados y número de alertas.
- **Tabla de alertas**: puntaje, Event ID, categoría, proceso y archivo de cada evento con
  puntaje >= 50.
- **Tabla de eventos**: todos los eventos que salieron del pipeline; las filas con alerta se
  resaltan.
- **Mensaje de error**: si el servidor no responde o la fuente falla.

`app.js` lee el formulario, llama a `/api/analyze` con `fetch` y pinta el resultado. Toda la
lógica de análisis se hace en Java; el frontend solo muestra datos. La interfaz también está en
inglés.

## Compilación y ejecución

### Desde IntelliJ IDEA
- **Versión web**: ejecutar `WebServer` y abrir <http://localhost:8080>.
- **Versión consola**: ejecutar `Main` (con argumento opcional para usar un log real).

El directorio de trabajo debe ser la raíz del proyecto, para que el servidor encuentre `web/`.

### Desde la terminal (en la raíz del proyecto)

```bash
javac -encoding UTF-8 -d out src/*.java

# Versión web
java -cp out WebServer
# abrir http://localhost:8080

# Versión consola con datos de prueba
java -cp out Main

# Versión consola con logs reales
java -cp out Main C:\logs\security.evtx
java -cp out Main Security        # requiere consola de administrador
```

## Datos de prueba

| Cantidad | Event ID | Proceso                                         | Archivo                     |
|----------|----------|-------------------------------------------------|-----------------------------|
| 120      | 4663     | `C:\Users\victima\AppData\Local\Temp\EVIL.EXE`  | `documento_N.docx.locked`   |
| 5        | 4663     | `C:\Program Files\Office\WINWORD.EXE`           | `.docx` normales            |
| 3        | 4688     | `C:\Windows\System32\CMD.EXE`                   | —                           |
| 1        | 1102     | — (borrado del log de auditoría)                | —                           |
| 10       | 4624     | — (inicio de sesión)                            | —                           |

## Resultado esperado (datos de prueba, pipeline completo)

- Los primeros 100 eventos de `evil.exe` suman 30 puntos (por la extensión) y **no** alertan.
- Desde el evento 101, `evil.exe` suma 80 puntos y genera **ALERT** (20 alertas).
- El evento 1102 genera **ALERT** con 50 puntos.
- `winword.exe` y `cmd.exe` no generan alertas.
- Los eventos 4624 se filtran (`OTHER`) y no aparecen en la salida.

Resumen final: **129 eventos procesados** y **21 alertas**. La consola y la web deben mostrar
los mismos números.

## ¿Por qué importa el orden de los decoradores?

Cada decorador depende de lo que hicieron las capas interiores:

- **Si `WithRiskScore` quedara por dentro de `WithEnrichment`**, recibiría eventos sin el campo
  `category`. No podría contar los `FILE_ACCESS` ni detectar `LOG_CLEARED`: `evil.exe` solo
  sumaría 30 puntos y el borrado del log 0, así que no habría ninguna alerta. Además, al estar
  también por dentro de `WithFilter`, puntuaría eventos que luego se descartan.
- **Si quedara por dentro de `WithNormalization`** (justo sobre la fuente), además de no tener
  categoría, el `process` llegaría como ruta completa y con mayúsculas. El conteo por proceso
  usaría claves como `C:\...\EVIL.EXE`, de modo que el mismo ejecutable lanzado desde rutas
  distintas, o escrito con distinta capitalización, se contaría por separado y podría no
  superar nunca el umbral de 100. Con logs reales esto es frecuente.

En resumen: los decoradores que **preparan** los datos (normalizar, enriquecer, filtrar) deben ir
por dentro de los que los **consumen** (puntuar). Por eso `PipelineBuilder` fija el orden.

En el frontend se puede ver el efecto de quitar capas:

| Capas activas | Procesados | Alertas | Motivo |
|---------------|------------|---------|--------|
| Todas | 129 | 21 | Resultado esperado. |
| Sin `filter` | 139 | 21 | Aparecen los 10 eventos 4624, con 0 puntos. |
| Sin `enrichment` | 139 | 0 | No hay categorías: el filtro no reconoce ningún `OTHER` y solo se suman los 30 puntos por extensión. |
| Sin `normalization` | 129 | 21 | Mismo resultado, pero los procesos aparecen con ruta completa. |
