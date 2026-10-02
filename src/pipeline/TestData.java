package pipeline;

import java.util.ArrayList;
import java.util.List;
import model.Event;

/**
 * Genera la lista de eventos de prueba del caso de estudio.
 */
public class TestData {

    private TestData() {
    }

    public static List<Event> generate() {
        List<Event> events = new ArrayList<>();
        addRansomwareAccesses(events);
        addRegularOfficeAccesses(events);
        addProcessCreations(events);
        addAuditLogCleared(events);
        addLogons(events);
        return events;
    }

    private static void addRansomwareAccesses(List<Event> events) {
        for (int i = 1; i <= 120; i++) {
            Event event = new Event(4663);
            event.put("process", "C:\\Users\\victim\\AppData\\Local\\Temp\\EVIL.EXE");
            event.put("file", "document_" + i + ".docx.locked");
            events.add(event);
        }
    }

    private static void addRegularOfficeAccesses(List<Event> events) {
        for (int i = 1; i <= 5; i++) {
            Event event = new Event(4663);
            event.put("process", "C:\\Program Files\\Office\\WINWORD.EXE");
            event.put("file", "document_" + i + ".docx");
            events.add(event);
        }
    }

    private static void addProcessCreations(List<Event> events) {
        for (int i = 0; i < 3; i++) {
            Event event = new Event(4688);
            event.put("process", "C:\\Windows\\System32\\CMD.EXE");
            events.add(event);
        }
    }

    private static void addAuditLogCleared(List<Event> events) {
        events.add(new Event(1102));
    }

    private static void addLogons(List<Event> events) {
        for (int i = 0; i < 10; i++) {
            events.add(new Event(4624));
        }
    }
}
