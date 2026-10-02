# Tareas — PipelineLogsDecorator

Lista de trabajo para implementar el caso de estudio del patrón **Decorator**, con backend
en Java (lógica) y frontend web (HTML + CSS + JavaScript).
Ver [README.md](README.md) para el contexto completo.

## Reglas generales

- [ ] Backend en Java puro, compatible con Java 11 (`switch` clásico, sin `record`).
- [ ] Sin Maven, Gradle ni librerías externas (el servidor usa `com.sun.net.httpserver` del JDK).
- [ ] Frontend en HTML, CSS y JavaScript sin frameworks ni dependencias.
- [ ] Toda la lógica de análisis en Java; el frontend solo pide y muestra datos.
- [ ] Clases Java directamente en `src/`, una clase por archivo; frontend en `web/`.
- [ ] Nombres de clases, métodos, comentarios e interfaz en español.
- [ ] Cada decorador sigue la delegación explícita:
      delegar → verificar `null` → añadir comportamiento → devolver.
- [ ] No extraer un Template Method ni añadir interfaces o capas extra.

## Backend

### 1. Clase de datos

- [ ] `Evento`
  - [ ] `final int id` (Event ID de Windows).
  - [ ] `Map<String, String> datos` (`proceso`, `archivo`, `categoria`).
  - [ ] `int puntaje` iniciado en 0.
  - [ ] Métodos `getId()`, `get(clave)`, `put(clave, valor)`, `getPuntaje()`, `sumarPuntaje(puntos)`.

### 2. Component y ConcreteComponent

- [ ] `FuenteEventos`: interfaz con `Evento siguiente()` (devuelve `null` al terminar).
- [ ] `FuenteEnMemoria`: recibe `List<Evento>` y la recorre con un `Iterator`.

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

- [ ] Ensamblar el pipeline (de afuera hacia adentro):
      `ConPuntajeRiesgo → ConFiltro → ConEnriquecimiento → ConNormalizacion → FuenteEnMemoria`.
- [ ] Imprimir una línea `ALERTA` por cada evento con puntaje >= 50
      (puntaje, categoría, proceso y archivo).
- [ ] Imprimir resumen: eventos procesados y número de alertas.

### 7. ServidorWeb

- [ ] Levantar `HttpServer` en el puerto 8080.
- [ ] `GET /` y archivos estáticos: servir `web/index.html`, `web/estilos.css`, `web/app.js`
      con el `Content-Type` correcto.
- [ ] `GET /api/analizar?capas=...`:
  - [ ] Leer las capas activas (`normalizacion`, `enriquecimiento`, `filtro`, `puntaje`);
        sin parámetro, todas.
  - [ ] Ensamblar un pipeline nuevo en cada petición (el `ConPuntajeRiesgo` tiene estado),
        respetando siempre el orden del pipeline.
  - [ ] Recorrer el pipeline y responder JSON con `capas`, `procesados`, `alertas` y `eventos`
        (`id`, `categoria`, `proceso`, `archivo`, `puntaje`, `alerta`).
  - [ ] Construir el JSON a mano, escapando `\` y `"`; campos ausentes como `null`.
- [ ] Imprimir en consola la URL al iniciar (`http://localhost:8080`).

## Frontend

### 8. `web/index.html`

- [ ] Panel del pipeline con las cuatro capas en orden y una casilla por capa (todas marcadas).
- [ ] Botón **Ejecutar análisis**.
- [ ] Tarjetas de resumen: eventos procesados y alertas.
- [ ] Tabla de alertas: puntaje, categoría, proceso, archivo.
- [ ] Tabla de todos los eventos procesados.

### 9. `web/estilos.css`

- [ ] Diseño limpio y legible; resaltar las filas con alerta.
- [ ] Usable en pantallas pequeñas (las tablas hacen scroll horizontal dentro de su contenedor).

### 10. `web/app.js`

- [ ] Leer las casillas y llamar a `/api/analizar?capas=...` con `fetch`.
- [ ] Pintar resumen y tablas con los datos recibidos.
- [ ] Mostrar un mensaje si el servidor no responde.

## Verificación

- [ ] Compilar: `javac -d out src/*.java`
- [ ] Consola: `java -cp out Main`
  - [ ] Los primeros 100 eventos de `evil.exe` suman 30 y no alertan.
  - [ ] Desde el evento 101, `evil.exe` suma 80 y alerta (20 alertas).
  - [ ] El evento 1102 alerta con 50 puntos.
  - [ ] `winword.exe` y `cmd.exe` no alertan.
  - [ ] Los eventos 4624 no aparecen.
  - [ ] Resumen: 129 eventos procesados, 21 alertas.
- [ ] Web: `java -cp out ServidorWeb` y abrir <http://localhost:8080>
  - [ ] Con todas las capas: 129 procesados y 21 alertas (igual que la consola).
  - [ ] Ejecutar dos veces seguidas da el mismo resultado (el estado no se arrastra).
  - [ ] Sin `filtro`: 139 procesados, siguen 21 alertas.
  - [ ] Sin `enriquecimiento`: 139 procesados, 0 alertas.
  - [ ] Sin `normalizacion`: los procesos aparecen con ruta completa y el JSON sigue siendo válido.

## Documentación

- [x] `README.md` con descripción, arquitectura backend/frontend, roles del patrón, API,
      ejecución y desarrolladores.
- [x] `task.md` con la lista de tareas.
- [x] Explicación de por qué importa el orden de los decoradores (incluida en el README).
