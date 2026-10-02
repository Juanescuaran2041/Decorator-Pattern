import java.util.ArrayList;
import java.util.List;

/**
 * Generates the test events of the case study.
 */
public class TestData {

    /** Returns a new list on each call, because decorators modify the events. */
    public static List<Event> generate() {
        List<Event> events = new ArrayList<>();

        // Ransomware: massive access to already encrypted files.
        for (int i = 1; i <= 120; i++) {
            events.add(create(4663, "C:\\Users\\victim\\AppData\\Local\\Temp\\EVIL.EXE",
                    "document_" + i + ".docx.locked"));
        }

        // Normal Word usage.
        for (int i = 1; i <= 5; i++) {
            events.add(create(4663, "C:\\Program Files\\Office\\WINWORD.EXE",
                    "report_" + i + ".docx"));
        }

        // Process creation.
        for (int i = 1; i <= 3; i++) {
            events.add(create(4688, "C:\\Windows\\System32\\CMD.EXE", null));
        }

        // Audit log clearing.
        events.add(create(1102, null, null));

        // Logons: they must be filtered out as OTHER.
        for (int i = 1; i <= 10; i++) {
            events.add(create(4624, null, null));
        }
        return events;
    }

    private static Event create(int id, String process, String file) {
        Event event = new Event(id);
        if (process != null) {
            event.put("process", process);
        }
        if (file != null) {
            event.put("file", file);
        }
        return event;
    }
}
