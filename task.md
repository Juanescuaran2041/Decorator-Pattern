# Tareas — PipelineLogsDecorator

Lista de trabajo para implementar el caso de estudio del patrón **Decorator** (con **Factory** y
**Builder** como apoyo), con backend en Java (lógica) y frontend web (HTML + CSS + JavaScript).
Ver [README.md](README.md) para el contexto completo.

Se avanza tarea por tarea y se hace un commit al terminar cada una.

## Reglas generales

- [ ] Backend en Java puro, compatible con Java 11 (`switch` clásico, sin `record`).
- [ ] Sin Maven, Gradle ni librerías externas (solo JDK: `HttpServer`, `javax.xml`, `ProcessBuilder`).
- [ ] Frontend en HTML, CSS y JavaScript sin frameworks ni dependencias.
- [ ] Toda la lógica de análisis en Java; el frontend solo pide y muestra datos.
- [ ] Clases Java directamente en `src/`, una clase por archivo; frontend en `web/`.
- [ ] Todo el código en inglés (clases, métodos, variables, valores, interfaz web).
- [ ] Sin comentarios en el código; los nombres explican el comportamiento.
- [ ] Código simple, de nivel de tercer semestre, sin métodos innecesarios.
- [ ] Cada decorador sigue la delegación explícita:
      delegar → verificar `null` → añadir comportamiento → devolver.
- [ ] No extraer un Template Method.

## Evaluación: lectura de logs reales de Windows

- [x] Comprobar `wevtutil` sobre el log Security sin administrador → `Acceso denegado`.
- [x] Comprobar `wevtutil` sobre el log System → devuelve XML.
- [x] Exportar un `.evtx` (`wevtutil epl`) y leerlo con `/lf:true` sin administrador → funciona.
- [x] Revisar la codificación de la salida → ANSI (`windows-1252`), no UTF-8.
- [x] Verificar que `/e:<raíz>` produce un XML con raíz única.
- [x] Conclusión documentada en el README: viable con `wevtutil` + `javax.xml`, sin librerías externas.

## Backend

### 1. Clase de datos

- [x] `Event`
  - [x] `final int id` (Event ID de Windows).
  - [x] `Map<String, String> data` (`process`, `file`, `category`).
  - [x] `int score` iniciado en 0.
  - [x] Métodos `getId()`, `get(key)`, `put(key, value)`, `getScore()`, `addScore(points)`.

### 2. Component y ConcreteComponents

- [x] `EventSource`: interfaz con `Event next()` (devuelve `null` al terminar).
- [x] `InMemorySource`: recibe `List<Event>` y la recorre con un `Iterator`.
- [ ] `WindowsLogSource(origin, maxEvents)`:
  - [x] Ejecutar `wevtutil qe <origin> /c:<maxEvents> /rd:true /f:xml /e:Events` con
        `ProcessBuilder` (añadir `/lf:true` si el origen termina en `.evtx`).
  - [x] Si `wevtutil` termina con error, lanzar `IllegalStateException` con su mensaje.
  - [x] Decodificar la salida con la página de códigos ANSI (`sun.jnu.encoding`).
  - [x] Leer el XML con `DocumentBuilder` (DOCTYPE deshabilitado).
  - [x] Mapear `EventID` → `id`, `ProcessName`/`NewProcessName` → `process`, `ObjectName` → `file`.
  - [x] Invertir la lista para procesar en orden cronológico.
  - [ ] Leer los eventos la primera vez que se llama a `next()`.

### 3. Decorator abstracto

- [ ] `SourceDecorator`: clase abstracta que implementa `EventSource`, con
      `protected final EventSource source` recibida por constructor.

### 4. Decoradores concretos

- [ ] `WithNormalization`: `process` → nombre del ejecutable en minúsculas
      (`C:\Windows\System32\CMD.EXE` → `cmd.exe`).
- [ ] `WithEnrichment`: campo `category` según Event ID
      (4663 `FILE_ACCESS`, 4660 `FILE_DELETE`, 4688 `PROCESS_CREATION`,
      1102 `LOG_CLEARED`, otro `OTHER`).
- [ ] `WithFilter`: ciclo que descarta eventos `OTHER` hasta encontrar uno válido o `null`.
- [ ] `WithRiskScore` (con estado):
  - [ ] Conteo de `FILE_ACCESS` por proceso en `Map<String, Integer> accessCountWithoutTimeWindow`;
        si supera 100, +50.
  - [ ] `file` termina en `.locked`, `.encrypted` o `.crypt`: +30.
  - [ ] Categoría `LOG_CLEARED`: +50.

### 5. Datos de prueba y Factory

- [ ] `TestData.generate()` devuelve una `List<Event>` nueva en cada llamada:
  - [ ] 120 eventos 4663 de `C:\Users\victima\AppData\Local\Temp\EVIL.EXE` sobre `documento_N.docx.locked`.
  - [ ] 5 eventos 4663 de `C:\Program Files\Office\WINWORD.EXE` sobre archivos `.docx`.
  - [ ] 3 eventos 4688 de `C:\Windows\System32\CMD.EXE`.
  - [ ] 1 evento 1102 (borrado del log de auditoría).
  - [ ] 10 eventos 4624 (inicio de sesión).
- [ ] `EventSourceFactory.create(origin)`: `"test"` o vacío → `InMemorySource`;
      otro valor → `WindowsLogSource(origin, 5000)`.

### 6. Main (consola)

- [ ] Origen: `args[0]` si existe, si no `"test"`; crear la fuente con `EventSourceFactory`.
- [ ] Ensamblar el pipeline a mano (de afuera hacia adentro):
      `WithRiskScore → WithFilter → WithEnrichment → WithNormalization → Source`.
- [ ] Imprimir una línea `ALERT` por cada evento con puntaje >= 50
      (puntaje, categoría, proceso y archivo).
- [ ] Imprimir resumen: eventos procesados y número de alertas.
- [ ] Si la fuente falla, mostrar `Error: <mensaje>` en lugar de la traza.

### 7. Builder y WebServer

- [ ] `PipelineBuilder(source)` con `withNormalization()`, `withEnrichment()`, `withFilter()`,
      `withRiskScore()` y `build()`; `build()` envuelve siempre en el orden correcto.
- [ ] `WebServer`: `HttpServer` en `127.0.0.1:8080`.
- [ ] Servir solo `web/index.html`, `web/styles.css` y `web/app.js` (lista cerrada) con el
      `Content-Type` correcto; `/` → `index.html`.
- [ ] `GET /api/analyze?origin=...&layers=...`:
  - [ ] `origin`: `test` (por defecto), nombre de log o ruta `.evtx`.
  - [ ] `layers`: `normalization`, `enrichment`, `filter`, `riskScore` (por defecto, todas).
  - [ ] Crear la fuente con `EventSourceFactory` y un pipeline nuevo con `PipelineBuilder`
        en cada petición.
  - [ ] Responder JSON con `origin`, `processed`, `alerts` y `events`
        (`id`, `category`, `process`, `file`, `score`, `alert`).
  - [ ] Construir el JSON a mano escapando `\` y `"`; ausentes como `null`.
  - [ ] Si la fuente falla, responder `400` con `{"error": "..."}`.
- [ ] Imprimir la URL al iniciar (`http://localhost:8080`).

## Frontend

### 8. `web/index.html`

- [ ] Selector de origen: datos de prueba o log de Windows (campo de texto para `Security` o ruta `.evtx`).
- [ ] Panel del pipeline con las capas en orden y una casilla por capa (todas marcadas).
- [ ] Botón **Run analysis**.
- [ ] Tarjetas de resumen: eventos procesados y alertas.
- [ ] Tabla de alertas: puntaje, Event ID, categoría, proceso, archivo.
- [ ] Tabla de todos los eventos procesados.
- [ ] Pie con los nombres de los desarrolladores.

### 9. `web/styles.css`

- [ ] Diseño limpio y legible, con modo claro y oscuro; resaltar las filas con alerta.
- [ ] Usable en pantallas pequeñas (las tablas hacen scroll dentro de su contenedor).

### 10. `web/app.js`

- [ ] Leer origen y casillas, llamar a `/api/analyze` con `fetch`.
- [ ] Pintar resumen y tablas con los datos recibidos (insertar texto, no HTML, para no
      ejecutar contenido de los logs).
- [ ] Mostrar el mensaje de error del servidor o un aviso si no responde.

## Verificación

- [ ] Compilar: `javac -encoding UTF-8 -d out src/*.java`
- [ ] Consola: `java -cp out Main`
  - [ ] Los primeros 100 eventos de `evil.exe` suman 30 y no alertan.
  - [ ] Desde el evento 101, `evil.exe` suma 80 y alerta (20 alertas).
  - [ ] El evento 1102 alerta con 50 puntos.
  - [ ] `winword.exe` y `cmd.exe` no alertan.
  - [ ] Los eventos 4624 no aparecen.
  - [ ] Resumen: 129 eventos procesados, 21 alertas.
- [ ] Consola con logs reales:
  - [ ] `java -cp out Main Security` sin administrador → mensaje `Acceso denegado`, sin traza.
  - [ ] `java -cp out Main System` → se procesa sin errores.
  - [ ] `java -cp out Main C:\ruta\archivo.evtx` → se procesa sin errores.
- [ ] Web: `java -cp out WebServer` y abrir <http://localhost:8080>
  - [ ] Con todas las capas: 129 procesados y 21 alertas (igual que la consola).
  - [ ] Ejecutar dos veces seguidas da el mismo resultado (el estado no se arrastra).
  - [ ] Sin `filter`: 139 procesados, 21 alertas.
  - [ ] Sin `enrichment`: 139 procesados, 0 alertas.
  - [ ] Sin `normalization`: procesos con ruta completa y JSON válido.
  - [ ] Origen `Security` sin administrador: se muestra el mensaje de error.

## Documentación

- [x] `README.md` con descripción, patrones (Decorator, Factory, Builder), arquitectura,
      evaluación de logs reales, API, ejecución y desarrolladores.
- [x] `task.md` con la lista de tareas.
- [x] Explicación de por qué importa el orden de los decoradores (incluida en el README).
