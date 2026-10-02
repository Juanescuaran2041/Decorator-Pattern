# Tasks — PipelineLogsDecorator

Work list to implement the **Decorator** pattern case study (with **Factory** and **Builder** as
support), with a Java backend (logic) and a web frontend (HTML + CSS + JavaScript).
See [README.md](README.md) for the full context.

Work goes task by task, with one commit for each completed subtask.

## General rules

- [ ] Plain Java backend, Java 11 compatible (classic `switch`, no `record`).
- [ ] No Maven, Gradle or external libraries (JDK only: `HttpServer`, `javax.xml`, `ProcessBuilder`).
- [ ] Frontend in HTML, CSS and JavaScript with no frameworks or dependencies.
- [ ] All analysis logic in Java; the frontend only requests and displays data.
- [ ] Java classes directly in `src/`, one class per file; frontend in `web/`.
- [ ] Everything in English (classes, methods, variables, values, web UI and documentation).
- [ ] No comments in the code; names explain the behavior.
- [ ] Simple, third-semester-level code, with no unnecessary methods.
- [ ] Every decorator follows explicit delegation:
      delegate → check `null` → add behavior → return.
- [ ] Do not extract a Template Method.

## Feasibility study: reading real Windows logs

- [x] Run `wevtutil` on the Security log without administrator rights → access denied.
- [x] Run `wevtutil` on the System log → returns XML.
- [x] Export an `.evtx` file (`wevtutil epl`) and read it with `/lf:true` without administrator rights → works.
- [x] Check the output encoding → ANSI (`windows-1252`), not UTF-8.
- [x] Check that `/e:<root>` produces XML with a single root.
- [x] Conclusion documented in the README: feasible with `wevtutil` + `javax.xml`, no external libraries.

## Backend

### 1. Data class

- [x] `Event`
  - [x] `final int id` (Windows Event ID).
  - [x] `Map<String, String> data` (`process`, `file`, `category`).
  - [x] `int score` starting at 0.
  - [x] Methods `getId()`, `get(key)`, `put(key, value)`, `getScore()`, `addScore(points)`.

### 2. Component and ConcreteComponents

- [x] `EventSource`: interface with `Event next()` (returns `null` at the end).
- [x] `InMemorySource`: receives a `List<Event>` and walks it with an `Iterator`.
- [x] `WindowsLogSource(origin, maxEvents)`:
  - [x] Run `wevtutil qe <origin> /c:<maxEvents> /rd:true /f:xml /e:Events` with
        `ProcessBuilder` (add `/lf:true` when the origin ends with `.evtx`).
  - [x] If `wevtutil` fails, throw `IllegalStateException` with its message.
  - [x] Decode the output with the ANSI code page (`sun.jnu.encoding`).
  - [x] Read the XML with `DocumentBuilder` (DOCTYPE disabled).
  - [x] Map `EventID` → `id`, `ProcessName`/`NewProcessName` → `process`, `ObjectName` → `file`.
  - [x] Reverse the list to process events in chronological order.
  - [x] Read the events the first time `next()` is called.

### 3. Abstract decorator

- [x] `SourceDecorator`: abstract class that implements `EventSource`, with
      `protected final EventSource source` received in the constructor.

### 4. Concrete decorators

- [x] `WithNormalization`: `process` → executable name in lower case
      (`C:\Windows\System32\CMD.EXE` → `cmd.exe`).
- [x] `WithEnrichment`: `category` field based on the Event ID
      (4663 `FILE_ACCESS`, 4660 `FILE_DELETE`, 4688 `PROCESS_CREATION`,
      1102 `LOG_CLEARED`, other `OTHER`).
- [x] `WithFilter`: loop that drops `OTHER` events until it finds a valid one or `null`.
- [x] `WithRiskScore` (stateful):
  - [x] Count `FILE_ACCESS` events per process in `Map<String, Integer> accessCountWithoutTimeWindow`;
        over 100, +50.
  - [x] `file` ends with `.locked`, `.encrypted` or `.crypt`: +30.
  - [x] Category `LOG_CLEARED`: +50.

### 5. Test data and Factory

- [x] `TestData.generate()` returns a new `List<Event>` on every call:
  - [x] 120 events 4663 from `C:\Users\victim\AppData\Local\Temp\EVIL.EXE` on `document_N.docx.locked`.
  - [x] 5 events 4663 from `C:\Program Files\Office\WINWORD.EXE` on regular `.docx` files.
  - [x] 3 events 4688 from `C:\Windows\System32\CMD.EXE`.
  - [x] 1 event 1102 (audit log cleared).
  - [x] 10 events 4624 (logon).
- [x] `EventSourceFactory.create(origin)`: `"test"` or empty → `InMemorySource`;
      any other value → `WindowsLogSource(origin, 5000)`.

### 6. Main (console)

- [x] Origin: `args[0]` if present, otherwise `"test"`; create the source with `EventSourceFactory`.
- [ ] Assemble the pipeline by hand (outside to inside):
      `WithRiskScore → WithFilter → WithEnrichment → WithNormalization → Source`.
- [ ] Print an `ALERT` line for every event with a score >= 50
      (score, category, process and file).
- [ ] Print a summary: processed events and number of alerts.
- [ ] If the source fails, print `Error: <message>` instead of the stack trace.

### 7. Builder and WebServer

- [ ] `PipelineBuilder(source)` with `withNormalization()`, `withEnrichment()`, `withFilter()`,
      `withRiskScore()` and `build()`; `build()` always wraps in the correct order.
- [ ] `WebServer`: `HttpServer` on `127.0.0.1:8080`.
- [ ] Serve only `web/index.html`, `web/styles.css` and `web/app.js` (fixed list) with the right
      `Content-Type`; `/` → `index.html`.
- [ ] `GET /api/analyze?origin=...&layers=...`:
  - [ ] `origin`: `test` (default), log name or `.evtx` path.
  - [ ] `layers`: `normalization`, `enrichment`, `filter`, `riskScore` (default: all).
  - [ ] Create the source with `EventSourceFactory` and a new pipeline with `PipelineBuilder`
        on every request.
  - [ ] Respond with JSON containing `origin`, `processed`, `alerts` and `events`
        (`id`, `category`, `process`, `file`, `score`, `alert`).
  - [ ] Build the JSON by hand, escaping `\` and `"`; missing fields as `null`.
  - [ ] If the source fails, respond `400` with `{"error": "..."}`.
- [ ] Print the URL on startup (`http://localhost:8080`).

## Frontend

### 8. `web/index.html`

- [ ] Origin selector: test data or Windows log (text field for `Security` or an `.evtx` path).
- [ ] Pipeline panel with the layers in order and one checkbox per layer (all checked).
- [ ] **Run analysis** button.
- [ ] Summary cards: processed events and alerts.
- [ ] Alerts table: score, Event ID, category, process, file.
- [ ] Table with every processed event.
- [ ] Footer with the developers' names.

### 9. `web/styles.css`

- [ ] Clean, readable design with light and dark mode; highlight rows with an alert.
- [ ] Usable on small screens (tables scroll inside their container).

### 10. `web/app.js`

- [ ] Read the origin and checkboxes, call `/api/analyze` with `fetch`.
- [ ] Render the summary and tables from the received data (insert text, not HTML, so log
      content is never executed).
- [ ] Show the server error message, or a notice if it does not respond.

## Verification

- [ ] Build: `javac -encoding UTF-8 -d out src/*.java`
- [ ] Console: `java -cp out Main`
  - [ ] The first 100 `evil.exe` events score 30 and do not raise an alert.
  - [ ] From event 101 on, `evil.exe` scores 80 and raises an alert (20 alerts).
  - [ ] Event 1102 raises an alert with 50 points.
  - [ ] `winword.exe` and `cmd.exe` do not raise alerts.
  - [ ] The 4624 events do not appear.
  - [ ] Summary: 129 processed events, 21 alerts.
- [ ] Console with real logs:
  - [ ] `java -cp out Main Security` without administrator rights → access denied message, no stack trace.
  - [ ] `java -cp out Main System` → processed with no errors.
  - [ ] `java -cp out Main C:\path\file.evtx` → processed with no errors.
- [ ] Web: `java -cp out WebServer` and open <http://localhost:8080>
  - [ ] With every layer: 129 processed and 21 alerts (same as the console).
  - [ ] Running twice in a row gives the same result (no state carried over).
  - [ ] Without `filter`: 139 processed, 21 alerts.
  - [ ] Without `enrichment`: 139 processed, 0 alerts.
  - [ ] Without `normalization`: processes as full paths and valid JSON.
  - [ ] Origin `Security` without administrator rights: the error message is shown.

## Documentation

- [x] `README.md` with description, patterns (Decorator, Factory, Builder), architecture,
      real log feasibility study, API, build/run instructions and developers.
- [x] `task.md` with the task list.
- [x] Explanation of why the decorator order matters (in the README).
- [x] Documentation translated to English.
