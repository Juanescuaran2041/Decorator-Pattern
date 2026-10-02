# Tareas — PipelineLogsDecorator

Lista de trabajo para implementar el caso de estudio del patrón **Decorator**, con backend
en Java (lógica) y frontend web (HTML + CSS + JavaScript).
Ver [README.md](README.md) para el contexto completo.

## Reglas generales

- [ ] Backend en Java puro, compatible con Java 11 (`switch` clásico, sin `record`).
- [ ] Sin Maven, Gradle ni librerías externas (solo JDK: `HttpServer`, `javax.xml`, `ProcessBuilder`).
- [ ] Frontend en HTML, CSS y JavaScript sin frameworks ni dependencias.
- [ ] Toda la lógica de análisis en Java; el frontend solo pide y muestra datos.
- [ ] Clases Java directamente en `src/`, una clase por archivo; frontend en `web/`.
- [ ] Nombres de clases, métodos, comentarios e interfaz en español.
- [ ] Cada decorador sigue la delegación explícita:
      delegar → verificar `null` → añadir comportamiento → devolver.
- [ ] No extraer un Template Method ni añadir interfaces o capas extra.

## Evaluación: lectura de logs reales de Windows

- [x] Comprobar `wevtutil` sobre el log Security sin administrador → `Acceso denegado`.
- [x] Comprobar `wevtutil` sobre el log System → devuelve XML.
- [x] Exportar un `.evtx` (`wevtutil epl`) y leerlo con `/lf:true` sin administrador → funciona.
- [x] Revisar la codificación de la salida → ANSI (`windows-1252`), no UTF-8.
- [x] Verificar que `/e:Eventos` produce un XML con raíz única.
- [x] Conclusión documentada en el README: viable con `wevtutil` + `javax.xml`, sin librerías externas.

## Backend

### 1. Clase de datos

- [ ] `Evento`
  - [ ] `final int id` (Event ID de Windows).
  - [ ] `Map<String, String> datos` (`proceso`, `archivo`, `categoria`).
  - [ ] `int puntaje` iniciado en 0.
  - [ ] Métodos `getId()`, `get(clave)`, `put(clave, valor)`, `getPuntaje()`, `sumarPuntaje(puntos)`.

### 2. Component y ConcreteComponents

- [ ] `FuenteEventos`: interfaz con `Evento siguiente()` (devuelve `null` al terminar).
- [ ] `FuenteEnMemoria`: recibe `List<Evento>` y la recorre con un `Iterator`.
- [ ] `FuenteLogWindows(origen, maximo)`:
  - [ ] Ejecutar `wevtutil qe <origen> /c:<maximo> /rd:true /f:xml /e:Eventos` con `ProcessBuilder`
        (añadir `/lf:true` si el origen termina en `.evtx`).
  - [ ] Si `wevtutil` termina con error, lanzar `IllegalStateException` con su mensaje.
  - [ ] Decodificar la salida con la página de códigos ANSI (`sun.jnu.encoding`).
  - [ ] Leer el XML con `DocumentBuilder` (DOCTYPE deshabilitado).
  - [ ] Mapear `EventID` → `id`, `ProcessName`/`NewProcessName` → `proceso`, `ObjectName` → `archivo`.
  - [ ] Invertir la lista para procesar en orden cronológico.
  - [ ] Leer los eventos la primera vez que se llama a `siguiente()`.

### 3. Decorator abstracto

- [ ] `DecoradorFuente`: clase abstracta que implementa `FuenteEventos`, con
      `protected final FuenteEventos fuente` recibida por constructor.

### 4. Decoradores concretos

- [ ] `ConNormalizacion`: `proceso` → nombre del ejecutable en minúsculas
      (`C:\Windows\System32\CMD.EXE` → `cmd.exe`).
- [ ] `ConEnriquecimiento`: campo `categoria` según Event ID
      (4663 `ACCESO_ARCHIVO`, 4660 `BORRADO_ARCHIVO`, 4688 `CREACION_PROCESO`,
      1102 `LOG_BORRADO`, otro `OTRO`).
- [ ] `ConFiltro`: ciclo que descarta eventos `OTRO` hasta encontrar uno válido o `null`.
- [ ] `ConPuntajeRiesgo` (con estado):
  - [ ] Conteo de `ACCESO_ARCHIVO` por proceso en `Map<String, Integer>`; si supera 100, +50.
  - [ ] `archivo` termina en `.locked`, `.encrypted` o `.crypt`: +30.
  - [ ] Categoría `LOG_BORRADO`: +50.
  - [ ] Comentario breve: el conteo es acumulado, sin ventana de tiempo (simplificación intencional).

### 5. Datos de prueba

- [ ] `DatosPrueba.generar()` devuelve una `List<Evento>` nueva en cada llamada:
  - [ ] 120 eventos 4663 de `C:\Users\victima\AppData\Local\Temp\EVIL.EXE` sobre `documento_N.docx.locked`.
  - [ ] 5 eventos 4663 de `C:\Program Files\Office\WINWORD.EXE` sobre archivos `.docx`.
  - [ ] 3 eventos 4688 de `C:\Windows\System32\CMD.EXE`.
  - [ ] 1 evento 1102 (borrado del log de auditoría).
  - [ ] 10 eventos 4624 (inicio de sesión).

### 6. Main (consola)

- [ ] Sin argumentos usar `DatosPrueba`; con un argumento usar `FuenteLogWindows(args[0], 5000)`.
- [ ] Ensamblar el pipeline (de afuera hacia adentro):
      `ConPuntajeRiesgo → ConFiltro → ConEnriquecimiento → ConNormalizacion → Fuente`.
- [ ] Imprimir una línea `ALERTA` por cada evento con puntaje >= 50
      (puntaje, categoría, proceso y archivo).
- [ ] Imprimir resumen: eventos procesados y número de alertas.
- [ ] Si la fuente falla, mostrar `Error: <mensaje>` en lugar de la traza.

### 7. ServidorWeb

- [ ] Levantar `HttpServer` en `127.0.0.1:8080`.
- [ ] Servir solo `web/index.html`, `web/estilos.css` y `web/app.js` (lista cerrada) con el
      `Content-Type` correcto; `/` → `index.html`.
- [ ] `GET /api/analizar?origen=...&capas=...`:
  - [ ] `origen`: `prueba` (por defecto), nombre de log o ruta `.evtx`.
  - [ ] `capas`: `normalizacion`, `enriquecimiento`, `filtro`, `puntaje` (por defecto, todas).
  - [ ] Armar un pipeline nuevo en cada petición, respetando siempre el orden.
  - [ ] Responder JSON con `origen`, `capas`, `procesados`, `alertas` y `eventos`
        (`id`, `categoria`, `proceso`, `archivo`, `puntaje`, `alerta`).
  - [ ] Construir el JSON a mano escapando `\`, `"` y caracteres de control; ausentes como `null`.
  - [ ] Si la fuente falla, responder `400` con `{"error": "..."}`.
- [ ] Imprimir la URL al iniciar (`http://localhost:8080`).

## Frontend

### 8. `web/index.html`

- [ ] Selector de origen: datos de prueba o log de Windows (campo de texto para `Security` o ruta `.evtx`).
- [ ] Panel del pipeline con las capas en orden y una casilla por capa (todas marcadas).
- [ ] Botón **Ejecutar análisis**.
- [ ] Tarjetas de resumen: eventos procesados y alertas.
- [ ] Tabla de alertas: puntaje, Event ID, categoría, proceso, archivo.
- [ ] Tabla de todos los eventos procesados.
- [ ] Pie con los nombres de los desarrolladores.

### 9. `web/estilos.css`

- [ ] Diseño limpio y legible, con modo claro y oscuro; resaltar las filas con alerta.
- [ ] Usable en pantallas pequeñas (las tablas hacen scroll dentro de su contenedor).

### 10. `web/app.js`

- [ ] Leer origen y casillas, llamar a `/api/analizar` con `fetch`.
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
- [ ] Web: `java -cp out ServidorWeb` y abrir <http://localhost:8080>
  - [ ] Con todas las capas: 129 procesados y 21 alertas (igual que la consola).
  - [ ] Ejecutar dos veces seguidas da el mismo resultado (el estado no se arrastra).
  - [ ] Sin `filtro`: 139 procesados, 21 alertas.
  - [ ] Sin `enriquecimiento`: 139 procesados, 0 alertas.
  - [ ] Sin `normalizacion`: procesos con ruta completa y JSON válido.
  - [ ] Origen `Security` sin administrador: se muestra el mensaje de error.

## Documentación

- [x] `README.md` con descripción, arquitectura backend/frontend, roles del patrón, evaluación
      de logs reales, API, ejecución y desarrolladores.
- [x] `task.md` con la lista de tareas.
- [x] Explicación de por qué importa el orden de los decoradores (incluida en el README).
