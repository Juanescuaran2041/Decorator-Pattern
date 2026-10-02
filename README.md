# PipelineLogsDecorator

Proyecto académico de la asignatura **Patrones de Diseño**. Aplica el patrón estructural
**Decorator** a un pipeline de análisis de eventos de logs de Windows, orientado a la
**detección temprana de ransomware**.

El proyecto tiene dos partes:

- **Backend (Java)**: toda la lógica del pipeline y el patrón Decorator, la lectura de logs
  reales de Windows y un servidor HTTP mínimo que expone el análisis como JSON.
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

Como todas las capas implementan la misma interfaz (`FuenteEventos`), se pueden quitar,
añadir o reordenar sin modificar las demás. Por la misma razón, la fuente se puede cambiar
(datos de prueba o log real de Windows) sin tocar ningún decorador.

## Restricciones técnicas

- **Backend**: Java puro (compatible con Java 11), proyecto de IntelliJ IDEA. Sin Maven,
  Gradle ni librerías externas. Solo se usa lo que trae el JDK:
  - `com.sun.net.httpserver.HttpServer` para el servidor web,
  - `javax.xml.parsers` para leer el XML de los eventos de Windows,
  - `ProcessBuilder` para ejecutar `wevtutil` (herramienta incluida en Windows).
- **Frontend**: HTML, CSS y JavaScript sin frameworks ni dependencias, usando `fetch`.
- Estructura plana en `src/`: una clase por archivo.
- Nombres de clases, métodos, comentarios e interfaz en español.
- `switch` clásico, sin `record`.

## Arquitectura

```
┌───────────────────────┐   GET /api/analizar?origen=...&capas=...   ┌─────────────────────────────┐
│  Frontend (web/)      │ ─────────────────────────────────────────▶ │  ServidorWeb (Java)         │
│  index.html           │                                            │  1. elige la fuente         │
│  estilos.css          │ ◀───────────────────────────────────────── │  2. arma el pipeline        │
│  app.js               │            JSON (eventos, resumen)         │  3. recorre y serializa     │
└───────────────────────┘                                            └─────────────────────────────┘
                                                                                   │
                         ConPuntajeRiesgo → ConFiltro → ConEnriquecimiento → ConNormalizacion
                                                                                   │
                                                          ┌────────────────────────┴───────────────┐
                                                   FuenteEnMemoria                         FuenteLogWindows
                                                   (DatosPrueba)                           (wevtutil → XML)
```

El mismo servidor entrega los archivos estáticos de `web/`, así que no hace falta otro
servidor ni configurar CORS. El servidor escucha solo en `127.0.0.1`, porque puede leer los
logs reales del equipo.

## Roles del patrón Decorator

| Rol del patrón       | Clase                | Responsabilidad |
|----------------------|----------------------|-----------------|
| Component            | `FuenteEventos`      | Interfaz con `Evento siguiente()`; devuelve `null` cuando no hay más eventos. |
| ConcreteComponent    | `FuenteEnMemoria`    | Recorre una `List<Evento>` con un `Iterator` (datos de prueba). |
| ConcreteComponent    | `FuenteLogWindows`   | Lee eventos reales de Windows con `wevtutil` y los convierte a `Evento`. |
| Decorator            | `DecoradorFuente`    | Clase abstracta que implementa `FuenteEventos` y guarda `protected final FuenteEventos fuente`. |
| ConcreteDecorator    | `ConNormalizacion`   | Deja solo el nombre del ejecutable en `proceso` y lo pasa a minúsculas. |
| ConcreteDecorator    | `ConEnriquecimiento` | Añade el campo `categoria` según el Event ID. |
| ConcreteDecorator    | `ConFiltro`          | Descarta los eventos con categoría `OTRO`. |
| ConcreteDecorator    | `ConPuntajeRiesgo`   | Decorador con estado: asigna puntaje de riesgo a cada evento. |
| Datos                | `Evento`             | Event ID, mapa de datos y puntaje acumulado. |

Clases de apoyo (no forman parte del patrón):

| Clase         | Responsabilidad |
|---------------|-----------------|
| `DatosPrueba` | Genera la lista de eventos de prueba. La usan `Main` y `ServidorWeb`. |
| `Main`        | Cliente de consola: ensambla el pipeline completo e imprime alertas y resumen. |
| `ServidorWeb` | Cliente web: sirve el frontend y expone `/api/analizar`. |

```
                    «interface»
                   FuenteEventos
                 + siguiente(): Evento
              ▲          ▲            ▲
              │          │            │
   FuenteEnMemoria  FuenteLogWindows  DecoradorFuente (abstracta) ◇── fuente : FuenteEventos
                                            ▲
             ┌──────────────┬───────────────┴──┬──────────────────┐
     ConNormalizacion  ConEnriquecimiento  ConFiltro       ConPuntajeRiesgo
```

## Estructura del proyecto

```
PipelineLogsDecorator/
├── src/                        ← Backend (lógica en Java)
│   ├── Evento.java
│   ├── FuenteEventos.java
│   ├── FuenteEnMemoria.java
│   ├── FuenteLogWindows.java
│   ├── DecoradorFuente.java
│   ├── ConNormalizacion.java
│   ├── ConEnriquecimiento.java
│   ├── ConFiltro.java
│   ├── ConPuntajeRiesgo.java
│   ├── DatosPrueba.java
│   ├── Main.java
│   └── ServidorWeb.java
├── web/                        ← Frontend
│   ├── index.html
│   ├── estilos.css
│   └── app.js
├── README.md
├── task.md
└── PipelineLogsDecorator.iml
```

## Detalle de los decoradores

### ConNormalizacion
Si el evento tiene `proceso`, conserva solo lo que va después del último `\` y lo pasa a
minúsculas. Ejemplo: `C:\Windows\System32\CMD.EXE` → `cmd.exe`.

### ConEnriquecimiento
Añade `categoria` según el Event ID de Windows:

| Event ID | Categoría          |
|----------|--------------------|
| 4663     | `ACCESO_ARCHIVO`   |
| 4660     | `BORRADO_ARCHIVO`  |
| 4688     | `CREACION_PROCESO` |
| 1102     | `LOG_BORRADO`      |
| otro     | `OTRO`             |

### ConFiltro
Pide eventos en un ciclo hasta encontrar uno cuya categoría no sea `OTRO`, o hasta llegar a
`null`.

### ConPuntajeRiesgo
| Regla | Puntos |
|-------|--------|
| El proceso supera 100 eventos `ACCESO_ARCHIVO` (conteo en `Map<String, Integer>`) | +50 |
| `archivo` termina en `.locked`, `.encrypted` o `.crypt` | +30 |
| Categoría `LOG_BORRADO` | +50 |

> El conteo es acumulado, sin ventana de tiempo. Es una simplificación intencional.

## Pipeline

Orden de ensamblaje (de afuera hacia adentro):

```
ConPuntajeRiesgo → ConFiltro → ConEnriquecimiento → ConNormalizacion → Fuente
```

```java
FuenteEventos pipeline =
        new ConPuntajeRiesgo(
            new ConFiltro(
                new ConEnriquecimiento(
                    new ConNormalizacion(
                        new FuenteEnMemoria(DatosPrueba.generar())))));
```

Para analizar un log real solo cambia la fuente; los decoradores son los mismos:

```java
new ConNormalizacion(new FuenteLogWindows("Security", 5000))
```

## Lectura de logs reales de Windows (evaluación de viabilidad)

El caso de estudio plantea que `FuenteEnMemoria` se reemplazará en el futuro por un lector real.
Se evaluó si eso es posible respetando la restricción de no usar librerías externas.

**Conclusión: sí es viable**, con `wevtutil` (incluido en Windows) para extraer los eventos en
XML y `javax.xml` (incluido en el JDK) para leerlos. Gracias al patrón, basta con añadir un
nuevo ConcreteComponent (`FuenteLogWindows`); ningún decorador cambia.

### Pruebas realizadas (Windows 11, usuario sin administrador)

| Prueba | Resultado |
|--------|-----------|
| `wevtutil qe Security /c:1 /f:xml` | ❌ `Acceso denegado` (código 5). El log Security requiere administrador. |
| `wevtutil qe System /c:1 /f:xml` | ✅ Devuelve el evento en XML. |
| Exportar con `wevtutil epl` y leer con `wevtutil qe archivo.evtx /lf:true` | ✅ Un `.evtx` se lee sin administrador. |
| Codificación de la salida | ⚠️ No es UTF-8 sino la página de códigos ANSI de Windows (`windows-1252`: `á` = `0xE1`). |
| Opción `/e:Eventos` | ✅ Envuelve todos los `<Event>` en un elemento raíz, así el XML es válido. |
| Pipeline sobre los logs System y `.evtx` exportado | ✅ Se leen y procesan; no contienen Event IDs de interés, así que el filtro los descarta. |

### Cómo funciona `FuenteLogWindows`

1. Ejecuta con `ProcessBuilder` (sin pasar por una shell):
   `wevtutil qe <origen> /c:<máximo> /rd:true /f:xml /e:Eventos`, añadiendo `/lf:true` si el
   origen es un archivo `.evtx`.
2. Si el código de salida no es 0, lanza `IllegalStateException` con el mensaje de `wevtutil`
   (por ejemplo, `Acceso denegado`).
3. Decodifica la salida con la página de códigos ANSI (`System.getProperty("sun.jnu.encoding")`).
4. Lee el XML con `DocumentBuilder` (con `DOCTYPE` deshabilitado) y crea un `Evento` por cada
   `<Event>`, usando estos campos de `<EventData>`:

   | Campo de Windows                         | Campo del `Evento` |
   |------------------------------------------|--------------------|
   | `System/EventID`                         | `id`               |
   | `ProcessName` (4663, 4660)               | `proceso`          |
   | `NewProcessName` (4688)                  | `proceso`          |
   | `ObjectName` (4663)                      | `archivo`          |

5. `/rd:true` trae los más recientes primero; la lista se invierte para procesar en orden
   cronológico.
6. Lee los eventos la primera vez que se llama a `siguiente()` y luego los entrega uno a uno.

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
wevtutil epl Security C:\logs\seguridad.evtx
```

El archivo exportado se puede analizar luego sin permisos de administrador.

### Limitaciones

- Solo funciona en Windows.
- No es en tiempo real: lee un lote de los últimos N eventos (por defecto 5000) en cada análisis.
- El evento 4660 no trae `ObjectName`; para saber qué archivo se borró habría que relacionarlo
  con el 4656 por `HandleId`. No se implementa (fuera del alcance).
- El conteo de `ConPuntajeRiesgo` sigue siendo acumulado, sin ventana de tiempo.

### Alternativas descartadas

| Alternativa | Motivo |
|-------------|--------|
| Interpretar el binario `.evtx` directamente en Java | Formato BinXML complejo; sobreingeniería para el alcance del proyecto. |
| JNA / API Win32 `EvtQuery` | Requiere librería externa. |
| PowerShell `Get-WinEvent` | Viable, pero más lento y depende de PowerShell; `wevtutil` es más simple. |

## API del backend

### `GET /api/analizar`

Elige la fuente, arma un pipeline nuevo (porque `ConPuntajeRiesgo` guarda estado), lo recorre
y devuelve el resultado en JSON.

| Parámetro | Descripción | Por defecto |
|-----------|-------------|-------------|
| `origen`  | `prueba` para los datos de prueba; o un log de Windows (`Security`, `System`) o la ruta de un archivo `.evtx`. | `prueba` |
| `capas`   | Lista separada por comas de las capas activas: `normalizacion`, `enriquecimiento`, `filtro`, `puntaje`. Se ensamblan siempre en el orden del pipeline. | todas |

Ejemplo: `GET /api/analizar?origen=prueba&capas=normalizacion,enriquecimiento,filtro,puntaje`

```json
{
  "origen": "prueba",
  "capas": ["normalizacion", "enriquecimiento", "filtro", "puntaje"],
  "procesados": 129,
  "alertas": 21,
  "eventos": [
    {
      "id": 4663,
      "categoria": "ACCESO_ARCHIVO",
      "proceso": "evil.exe",
      "archivo": "documento_101.docx.locked",
      "puntaje": 80,
      "alerta": true
    }
  ]
}
```

- Un evento es `alerta` cuando su puntaje es >= 50.
- Los campos que no existen se envían como `null`.
- El JSON se construye a mano escapando `\`, `"` y caracteres de control (sin normalización los
  procesos llegan con rutas de Windows).
- Si la fuente falla (por ejemplo, `Security` sin administrador) responde `400` con
  `{"error": "..."}`.

### `GET /`

Sirve solo `index.html`, `estilos.css` y `app.js` de la carpeta `web/` (lista cerrada, para
no exponer otros archivos).

## Frontend

La página (`web/index.html`) muestra:

- **Origen de los eventos**: datos de prueba o log real de Windows (campo de texto para
  `Security` o la ruta de un `.evtx`).
- **Panel del pipeline**: las capas en orden, cada una con una casilla para activarla o
  desactivarla, y el botón **Ejecutar análisis**.
- **Resumen**: tarjetas con eventos procesados y número de alertas.
- **Tabla de alertas**: puntaje, Event ID, categoría, proceso y archivo de cada evento con
  puntaje >= 50.
- **Tabla de eventos**: todos los eventos que salieron del pipeline; las filas con alerta se
  resaltan.
- **Mensaje de error**: si el servidor no responde o la fuente falla.

`app.js` lee el formulario, llama a `/api/analizar` con `fetch` y pinta el resultado. Toda la
lógica de análisis se hace en Java; el frontend solo muestra datos.

## Compilación y ejecución

### Desde IntelliJ IDEA
- **Versión web**: ejecutar `ServidorWeb` y abrir <http://localhost:8080>.
- **Versión consola**: ejecutar `Main` (con argumento opcional para usar un log real).

El directorio de trabajo debe ser la raíz del proyecto, para que el servidor encuentre `web/`.

### Desde la terminal (en la raíz del proyecto)

```bash
javac -encoding UTF-8 -d out src/*.java

# Versión web
java -cp out ServidorWeb
# abrir http://localhost:8080

# Versión consola con datos de prueba
java -cp out Main

# Versión consola con logs reales
java -cp out Main C:\logs\seguridad.evtx
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
- Desde el evento 101, `evil.exe` suma 80 puntos y genera **ALERTA** (20 alertas).
- El evento 1102 genera **ALERTA** con 50 puntos.
- `winword.exe` y `cmd.exe` no generan alertas.
- Los eventos 4624 se filtran (`OTRO`) y no aparecen en la salida.

Resumen final: **129 eventos procesados** y **21 alertas**. La consola y la web deben mostrar
los mismos números.

## ¿Por qué importa el orden de los decoradores?

Cada decorador depende de lo que hicieron las capas interiores:

- **Si `ConPuntajeRiesgo` quedara por dentro de `ConEnriquecimiento`**, recibiría eventos
  sin el campo `categoria`. No podría contar los `ACCESO_ARCHIVO` ni detectar `LOG_BORRADO`:
  `evil.exe` solo sumaría 30 puntos y el borrado del log 0, así que no habría ninguna alerta.
  Además, al estar también por dentro de `ConFiltro`, puntuaría eventos que luego se descartan.
- **Si quedara por dentro de `ConNormalizacion`** (justo sobre la fuente), además de no tener
  categoría, el `proceso` llegaría como ruta completa y con mayúsculas. El conteo por proceso
  usaría claves como `C:\...\EVIL.EXE`, de modo que el mismo ejecutable lanzado desde rutas
  distintas, o escrito con distinta capitalización, se contaría por separado y podría no
  superar nunca el umbral de 100. Con logs reales esto es frecuente.

En resumen: los decoradores que **preparan** los datos (normalizar, enriquecer, filtrar) deben ir
por dentro de los que los **consumen** (puntuar).

En el frontend se puede ver un efecto parecido al desactivar capas:

| Capas activas | Procesados | Alertas | Motivo |
|---------------|------------|---------|--------|
| Todas | 129 | 21 | Resultado esperado. |
| Sin `filtro` | 139 | 21 | Aparecen los 10 eventos 4624, con 0 puntos. |
| Sin `enriquecimiento` | 139 | 0 | No hay categorías: el filtro no reconoce ningún `OTRO` y solo se suman los 30 puntos por extensión. |
| Sin `normalizacion` | 129 | 21 | Mismo resultado, pero los procesos aparecen con ruta completa. |
