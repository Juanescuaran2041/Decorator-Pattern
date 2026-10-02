# PipelineLogsDecorator

Academic project for the **Design Patterns** course. It applies the structural **Decorator**
pattern to a Windows event log analysis pipeline aimed at **early ransomware detection**. It also
uses **Factory** (to choose the event source) and **Builder** (to assemble the pipeline from the
web page).

The project has two parts:

- **Backend (Java)**: all the pipeline logic, reading real Windows logs, and a minimal HTTP
  server that exposes the analysis as JSON.
- **Frontend (HTML + CSS + JavaScript)**: a web page that runs the analysis, lets the user pick
  the event origin, turn pipeline layers on or off, and see events and alerts.

## Developers

- Juan Esteban Cuaran Santander
- Hugo David Burgos Trejo
- Jose Nicolas Mora

## Main idea

An event source is wrapped by several layers (decorators). Each layer:

1. asks the inner layer for the event,
2. checks whether it is `null` (no more events),
3. adds its own behavior,
4. returns it to the outer layer.

Since every layer implements the same interface (`EventSource`), layers can be removed, added or
reordered without changing the others. For the same reason, the source can be swapped (test data
or a real Windows log) without touching any decorator.

## Technical constraints

- **Backend**: plain Java (Java 11 compatible), IntelliJ IDEA project. No Maven, no Gradle, no
  external libraries. Only what the JDK provides is used:
  - `com.sun.net.httpserver.HttpServer` for the web server,
  - `javax.xml.parsers` to read the XML of Windows events,
  - `ProcessBuilder` to run `wevtutil` (a tool included with Windows).
- **Frontend**: HTML, CSS and JavaScript with no frameworks or dependencies, using `fetch`.
- Flat structure in `src/`: one class per file.
- **Everything in English** (classes, methods, variables, values, UI and documentation) and
  **no comments** in the code: names must explain the code by themselves.
- Simple code, third-semester level: no unnecessary methods or extra abstractions.
- Classic `switch`, no `record`.

## Design patterns

### Decorator (main pattern)

| Pattern role         | Class              | Responsibility |
|----------------------|--------------------|----------------|
| Component            | `EventSource`      | Interface with `Event next()`; returns `null` when there are no more events. |
| ConcreteComponent    | `InMemorySource`   | Walks a `List<Event>` with an `Iterator` (test data). |
| ConcreteComponent    | `WindowsLogSource` | Reads real Windows events with `wevtutil` and turns them into `Event` objects. |
| Decorator            | `SourceDecorator`  | Abstract class that implements `EventSource` and keeps `protected final EventSource source`. |
| ConcreteDecorator    | `WithNormalization`| Keeps only the executable name in `process`, in lower case. |
| ConcreteDecorator    | `WithEnrichment`   | Adds the `category` field based on the Event ID. |
| ConcreteDecorator    | `WithFilter`       | Drops events whose category is `OTHER`. |
| ConcreteDecorator    | `WithRiskScore`    | Stateful decorator: adds a risk score to each event. |
| Data                 | `Event`            | Event ID, data map and accumulated score. |

```
                     «interface»
                     EventSource
                   + next(): Event
              ▲           ▲              ▲
              │           │              │
     InMemorySource  WindowsLogSource  SourceDecorator (abstract) ◇── source : EventSource
                                             ▲
            ┌────────────────┬───────────────┴──┬────────────────┐
    WithNormalization  WithEnrichment      WithFilter      WithRiskScore
```

### Factory

`EventSourceFactory.create(origin)` decides which source to create:

- `"test"` (or empty) → `InMemorySource` with `TestData.generate()`.
- any other value (`Security`, `System`, an `.evtx` path) → `WindowsLogSource`.

This way `Main` and `WebServer` do not repeat that decision or depend on the concrete source
classes.

### Builder

`PipelineBuilder` assembles the pipeline from the layers the user turns on in the web page:

```java
EventSource pipeline = new PipelineBuilder(source)
        .withNormalization()
        .withEnrichment()
        .withFilter()
        .withRiskScore()
        .build();
```

No matter the order in which the `with...` methods are called, `build()` always wraps the layers
in the correct pipeline order. This prevents the web page from building an invalid order (see
*Why does the decorator order matter?*).

`Main` does not use the builder: it nests the decorators by hand so the Decorator pattern is
clearly visible.

### Iterator

`InMemorySource` and `WindowsLogSource` walk their events with a Java `Iterator`.

## Supporting classes

| Class                | Responsibility |
|----------------------|----------------|
| `TestData`           | Generates the list of test events. |
| `EventSourceFactory` | Creates the right source for the origin. |
| `PipelineBuilder`    | Assembles the pipeline with the active layers, in the correct order. |
| `Main`               | Console client: assembles the pipeline and prints alerts and a summary. |
| `WebServer`          | Web client: serves the frontend and exposes `/api/analyze`. |

## Architecture

```
┌───────────────────────┐   GET /api/analyze?origin=...&layers=...   ┌─────────────────────────────┐
│  Frontend (web/)      │ ─────────────────────────────────────────▶ │  WebServer (Java)           │
│  index.html           │                                            │  1. EventSourceFactory      │
│  styles.css           │ ◀───────────────────────────────────────── │  2. PipelineBuilder         │
│  app.js               │            JSON (events, summary)          │  3. iterate and serialize   │
└───────────────────────┘                                            └─────────────────────────────┘
                                                                                   │
                        WithRiskScore → WithFilter → WithEnrichment → WithNormalization
                                                                                   │
                                                          ┌────────────────────────┴──────────────┐
                                                   InMemorySource                        WindowsLogSource
                                                   (TestData)                            (wevtutil → XML)
```

The same server delivers the static files in `web/`, so no other server or CORS setup is needed.
The server only listens on `127.0.0.1`, because it can read the machine's real logs.

## Project structure

```
PipelineLogsDecorator/
├── src/                        ← Backend (Java logic)
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

## Decorator details

### WithNormalization
If the event has a `process`, it keeps only what comes after the last `\` and converts it to
lower case. Example: `C:\Windows\System32\CMD.EXE` → `cmd.exe`.

### WithEnrichment
Adds `category` based on the Windows Event ID:

| Event ID | Category           |
|----------|--------------------|
| 4663     | `FILE_ACCESS`      |
| 4660     | `FILE_DELETE`      |
| 4688     | `PROCESS_CREATION` |
| 1102     | `LOG_CLEARED`      |
| other    | `OTHER`            |

### WithFilter
Asks for events in a loop until it finds one whose category is not `OTHER`, or reaches `null`.

### WithRiskScore
| Rule | Points |
|------|--------|
| The process goes over 100 `FILE_ACCESS` events (counted in a `Map<String, Integer>`) | +50 |
| `file` ends with `.locked`, `.encrypted` or `.crypt` | +30 |
| Category `LOG_CLEARED` | +50 |

> The count is cumulative, with no time window. This is an intentional simplification. Since the
> code has no comments, the field name states it: `accessCountWithoutTimeWindow`.

## Pipeline

Assembly order (outside to inside):

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

To analyze a real log only the origin changes; the decorators stay the same:
`EventSourceFactory.create("Security")`.

## Reading real Windows logs (feasibility study)

The case study says the in-memory source will later be replaced by a real reader. We checked
whether that is possible while keeping the no-external-libraries rule.

**Conclusion: it is feasible**, using `wevtutil` (included with Windows) to export the events as
XML and `javax.xml` (included with the JDK) to read them. Thanks to the pattern, it only takes a
new ConcreteComponent (`WindowsLogSource`); no decorator changes.

### Tests performed (Windows 11, non-administrator user)

| Test | Result |
|------|--------|
| `wevtutil qe Security /c:1 /f:xml` | ❌ `Access denied` (exit code 5). The Security log requires administrator rights. |
| `wevtutil qe System /c:1 /f:xml` | ✅ Returns the event as XML. |
| Export with `wevtutil epl` and read with `wevtutil qe file.evtx /lf:true` | ✅ An `.evtx` file can be read without administrator rights. |
| Output encoding | ⚠️ Not UTF-8 but the Windows ANSI code page (`windows-1252`: `á` = `0xE1`). |
| `/e:Events` option | ✅ Wraps every `<Event>` in a root element, so the XML is valid. |
| Pipeline over the System log and an exported `.evtx` | ✅ Read and processed; they contain no relevant Event IDs, so the filter drops them. |

### How `WindowsLogSource` works

1. Runs with `ProcessBuilder` (no shell involved):
   `wevtutil qe <origin> /c:<max> /rd:true /f:xml /e:Events`, adding `/lf:true` when the origin
   is an `.evtx` file.
2. If the exit code is not 0, it throws `IllegalStateException` with the `wevtutil` message (for
   example, access denied).
3. Decodes the output with the ANSI code page (`System.getProperty("sun.jnu.encoding")`).
4. Reads the XML with `DocumentBuilder` (with `DOCTYPE` disabled) and creates one `Event` per
   `<Event>`, using these `<EventData>` fields:

   | Windows field                            | `Event` field |
   |------------------------------------------|---------------|
   | `System/EventID`                         | `id`          |
   | `ProcessName` (4663, 4660)               | `process`     |
   | `NewProcessName` (4688)                  | `process`     |
   | `ObjectName` (4663)                      | `file`        |

5. `/rd:true` returns the newest events first; the list is reversed to process them in
   chronological order.
6. Reads the events the first time `next()` is called and then returns them one by one.

### Requirements for the case study events to appear

| Event ID | Windows requirement |
|----------|---------------------|
| 4663 / 4660 | *Audit File System* policy enabled **and** a SACL (audit entry) set on the folders to watch. Without this Windows does not generate these events. |
| 4688 | *Audit Process Creation* policy enabled. |
| 1102 | Always generated when the security log is cleared. |

Example (administrator console; subcategory names depend on the Windows language):

```bat
auditpol /set /subcategory:"File System" /success:enable
auditpol /set /subcategory:"Process Creation" /success:enable
wevtutil epl Security C:\logs\security.evtx
```

The exported file can then be analyzed without administrator rights.

### Limitations

- Windows only.
- Not real time: each analysis reads a batch of the latest N events (5000 by default).
- Event 4660 has no `ObjectName`; finding out which file was deleted would require matching it
  with event 4656 by `HandleId`. Not implemented (out of scope).
- The `WithRiskScore` count is still cumulative, with no time window.

### Discarded alternatives

| Alternative | Reason |
|-------------|--------|
| Parsing the binary `.evtx` format directly in Java | Complex BinXML format; overengineering for this project. |
| JNA / Win32 `EvtQuery` API | Requires an external library. |
| PowerShell `Get-WinEvent` | Feasible, but slower and depends on PowerShell; `wevtutil` is simpler. |

## Backend API

### `GET /api/analyze`

Creates the source with `EventSourceFactory`, builds a new pipeline with `PipelineBuilder`
(because `WithRiskScore` keeps state), iterates it and returns the result as JSON.

| Parameter | Description | Default |
|-----------|-------------|---------|
| `origin`  | `test` for the test data; or a Windows log (`Security`, `System`) or the path of an `.evtx` file. | `test` |
| `layers`  | Comma-separated list of active layers: `normalization`, `enrichment`, `filter`, `riskScore`. They are always assembled in pipeline order. | all |

Example: `GET /api/analyze?origin=test&layers=normalization,enrichment,filter,riskScore`

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
      "file": "document_101.docx.locked",
      "score": 80,
      "alert": true
    }
  ]
}
```

- An event is an `alert` when its score is >= 50.
- Missing fields are sent as `null`.
- The JSON is built by hand, escaping `\` and `"` (without normalization, processes arrive as
  Windows paths).
- If the source fails (for example, `Security` without administrator rights) it responds `400`
  with `{"error": "..."}`.

### `GET /`

Serves only `index.html`, `styles.css` and `app.js` from the `web/` folder (a fixed list, so no
other files are exposed).

## Frontend

The page (`web/index.html`) shows:

- **Event origin**: test data or a real Windows log (text field for `Security` or an `.evtx`
  path).
- **Pipeline panel**: the layers in order, each with a checkbox to turn it on or off, and the
  **Run analysis** button.
- **Summary**: cards with the number of processed events and alerts.
- **Alerts table**: score, Event ID, category, process and file of every event with a score
  >= 50.
- **Events table**: every event that came out of the pipeline; rows with an alert are
  highlighted.
- **Error message**: when the server does not respond or the source fails.

`app.js` reads the form, calls `/api/analyze` with `fetch` and renders the result. All the
analysis logic runs in Java; the frontend only displays data.

## Deployment guide

This guide covers everything from a fresh machine to a scheduled analysis of the real Security
log. Steps 1 to 5 are enough to run the project for class; steps 6 to 9 deploy it on a Windows
machine that should be monitored.

> The web version (`WebServer` and `web/`) is part of tasks 7 to 10. Until those tasks are merged,
> only the console version (`Main`) is available. See [task.md](task.md) for the current progress.

### 1. Requirements

| Tool | Version | Check |
|------|---------|-------|
| JDK (Temurin, Oracle or any OpenJDK) | 11 or newer | `java -version` and `javac -version` |
| Git | any | `git --version` |
| Windows | 10 or 11 | only needed to read real logs; test data runs on any OS |
| IntelliJ IDEA (optional) | Community or Ultimate | — |

No Maven, Gradle or extra libraries are needed. `wevtutil` comes with Windows.

### 2. Get the code

```bash
git clone https://github.com/Juanescuaran2041/Decorator-Pattern.git
cd Decorator-Pattern
```

All the commands below are run from the project root (the folder that contains `src/`).

### 3. Build

```bash
javac -encoding UTF-8 -d out src/*.java
```

- Works in Git Bash, `cmd` and PowerShell.
- `-encoding UTF-8` keeps the build independent of the system code page.
- The compiled classes go to `out/`, which is ignored by Git.
- To check Java 11 compatibility with a newer JDK, add `--release 11`.

### 4. Run the console version

```bash
# Test data from the case study
java -cp out Main

# An exported .evtx file (no administrator rights needed)
java -cp out Main C:\logs\security.evtx

# The live Security log (requires a console opened as administrator)
java -cp out Main Security
```

Expected output with the test data (last lines):

```
ALERT score=80 category=FILE_ACCESS process=evil.exe file=document_120.docx.locked
ALERT score=50 category=LOG_CLEARED process=null file=null
----------------------------------------
Processed events: 129
Alerts: 21
```

### 5. Run from IntelliJ IDEA

1. **File → Open** and select the project folder.
2. **File → Project Structure → Project**: choose a JDK 11 or newer as the SDK.
3. Open `src/Main.java` and click the green run arrow next to `main`.
4. To read a real log: **Run → Edit Configurations → Main → Program arguments**, for example
   `Security` or `C:\logs\security.evtx`.
5. For the web version, run `WebServer` the same way. Check that **Working directory** is
   `$PROJECT_DIR$`, otherwise the server cannot find `web/`. Then open <http://localhost:8080>.

### 6. Package as a JAR

```bash
javac -encoding UTF-8 -d out src/*.java
jar --create --file pipeline-logs.jar --main-class Main -C out .
```

Run it:

```bash
java -jar pipeline-logs.jar                      # console, test data
java -jar pipeline-logs.jar Security             # console, live Security log (administrator)
java -cp pipeline-logs.jar WebServer             # web version (web/ must be next to the JAR)
```

### 7. Prepare the Windows machine to monitor

Windows does not record file access or process creation by default. In a console opened as
administrator:

```bat
auditpol /set /subcategory:"File System" /success:enable
auditpol /set /subcategory:"Process Creation" /success:enable
```

The subcategory names follow the Windows language. On a Spanish Windows use
`auditpol /list /subcategory:*` to see the exact names.

Then add an audit entry (SACL) to every folder that should be watched, for example
`C:\Users\<user>\Documents`:

1. Right click the folder → **Properties → Security → Advanced → Auditing → Add**.
2. **Principal**: `Everyone`. **Type**: `Success`.
3. **Advanced permissions**: `Create files / write data`, `Delete`, `Read data`.
4. Apply. From now on Windows writes events 4663 and 4660 for that folder.

Check that events arrive:

```bat
wevtutil qe Security /q:"*[System[(EventID=4663)]]" /c:5 /rd:true /f:text
```

### 8. Deploy the program

1. Create a folder, for example `C:\PipelineLogs`.
2. Copy `pipeline-logs.jar` into it (and the `web/` folder if the web version is used).
3. Make sure `java` is on the system `PATH` (`where java`).
4. Run a first analysis as administrator:

   ```bat
   cd C:\PipelineLogs
   java -jar pipeline-logs.jar Security
   ```

If administrator rights are not allowed on that machine, an administrator can export the log
and the analysis can run anywhere:

```bat
wevtutil epl Security C:\PipelineLogs\security.evtx
java -jar pipeline-logs.jar C:\PipelineLogs\security.evtx
```

### 9. Schedule the analysis

Run the analysis every hour as `SYSTEM` (which can read the Security log) and append the result to
a file. In a console opened as administrator:

```bat
schtasks /create /tn "PipelineLogsDecorator" /sc hourly /ru SYSTEM ^
  /tr "cmd /c cd /d C:\PipelineLogs && java -jar pipeline-logs.jar Security >> alerts.log"
```

- Run it once now: `schtasks /run /tn "PipelineLogsDecorator"`.
- Check the result in `C:\PipelineLogs\alerts.log`.
- Remove it: `schtasks /delete /tn "PipelineLogsDecorator" /f`.

Each run reads the latest 5000 events and the count has no time window, so the same events can be
reported again in the next run. This is one of the known limitations of the project.

### 10. Deployment checklist

- [ ] `java -version` shows 11 or newer.
- [ ] `java -jar pipeline-logs.jar` prints 129 processed events and 21 alerts.
- [ ] `auditpol /get /category:*` shows *File System* and *Process Creation* with `Success`.
- [ ] The watched folders have an audit entry and `wevtutil` returns 4663 events.
- [ ] `java -jar pipeline-logs.jar Security` runs as administrator without errors.
- [ ] The scheduled task exists and writes to `alerts.log`.

### 11. Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| `javac` is not recognized | JDK not installed or not on `PATH` | Install a JDK and add its `bin` folder to `PATH`. |
| `error: unmappable character for encoding` | Build without `-encoding UTF-8` | Add `-encoding UTF-8` to `javac`. |
| `Error: wevtutil could not read 'Security': ... denied` | Console without administrator rights | Open the console as administrator, or analyze an exported `.evtx`. |
| `Processed events: 0` with a real log | No 4663 / 4660 / 4688 / 1102 events in the log | Enable auditing and add the SACL (step 7). |
| `Could not run wevtutil` | Not running on Windows | Use the test data, or run on Windows. |
| `Could not find or load main class Main` | Wrong classpath or not built | Build again and run from the project root with `-cp out`. |
| Web page shows a "web/ not found" error | Wrong working directory | Run `WebServer` from the project root (`$PROJECT_DIR$` in IntelliJ). |
| Port 8080 already in use | Another program uses the port | Close that program or change the port in `WebServer`. |

## Contributing workflow

This is how the team works on the project and keeps this README up to date:

1. **One branch per task** from `main`, for example `feature/builder_n_webserver` for task 7.
2. **One commit per completed subtask** of [task.md](task.md), ticking its checkbox in the same
   commit. Commit messages in English, in the imperative mood ("Add WithFilter decorator").
3. **Build before every commit**: `javac --release 11 -encoding UTF-8 -d out src/*.java`.
4. **Code rules**: English names, no comments, no unnecessary methods, one class per file.
5. **Update the README in the same branch** when a change affects it:
   - a new class → *Project structure* and the pattern tables,
   - a new command or option → *Deployment guide*,
   - a new endpoint or field → *Backend API*,
   - a known problem → *Troubleshooting*.
6. **Push the branch and open a pull request** to `main`; merge it once the expected result
   (129 processed events, 21 alerts) is confirmed.

## Test data

| Count | Event ID | Process                                         | File                        |
|-------|----------|-------------------------------------------------|-----------------------------|
| 120   | 4663     | `C:\Users\victim\AppData\Local\Temp\EVIL.EXE`   | `document_N.docx.locked`    |
| 5     | 4663     | `C:\Program Files\Office\WINWORD.EXE`           | regular `.docx` files       |
| 3     | 4688     | `C:\Windows\System32\CMD.EXE`                   | —                           |
| 1     | 1102     | — (audit log cleared)                           | —                           |
| 10    | 4624     | — (logon)                                       | —                           |

## Expected result (test data, full pipeline)

- The first 100 `evil.exe` events score 30 points (because of the extension) and do **not**
  raise an alert.
- From event 101 on, `evil.exe` scores 80 points and raises an **ALERT** (20 alerts).
- Event 1102 raises an **ALERT** with 50 points.
- `winword.exe` and `cmd.exe` do not raise alerts.
- The 4624 events are filtered (`OTHER`) and do not appear in the output.

Final summary: **129 processed events** and **21 alerts**. The console and the web page must
show the same numbers.

## Why does the decorator order matter?

Each decorator depends on what the inner layers already did:

- **If `WithRiskScore` were inside `WithEnrichment`**, it would receive events without the
  `category` field. It could not count `FILE_ACCESS` events or detect `LOG_CLEARED`: `evil.exe`
  would only score 30 points and the log clearing 0, so there would be no alerts at all. Also,
  being inside `WithFilter` too, it would score events that are dropped later.
- **If it were inside `WithNormalization`** (right on top of the source), besides having no
  category, `process` would arrive as a full path in upper case. The per-process count would use
  keys like `C:\...\EVIL.EXE`, so the same executable started from different paths, or written
  with different casing, would be counted separately and might never go over the limit of 100.
  With real logs this happens often.

In short: the decorators that **prepare** the data (normalize, enrich, filter) must be inside the
ones that **use** it (score). That is why `PipelineBuilder` fixes the order.

The effect of removing layers can be seen in the web page:

| Active layers | Processed | Alerts | Reason |
|---------------|-----------|--------|--------|
| All | 129 | 21 | Expected result. |
| Without `filter` | 139 | 21 | The 10 events 4624 appear, with 0 points. |
| Without `enrichment` | 139 | 0 | No categories: the filter finds no `OTHER` and only the 30 extension points are added. |
| Without `normalization` | 129 | 21 | Same result, but processes appear as full paths. |
