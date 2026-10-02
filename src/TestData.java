import java.util.ArrayList;
import java.util.List;

public class TestData {

    public static List<Event> generate() {
        List<Event> events = new ArrayList<>();

        for (int i = 1; i <= 120; i++) {
            events.add(create(4663, "C:\\Users\\victim\\AppData\\Local\\Temp\\EVIL.EXE",
                    "document_" + i + ".docx.locked"));
        }

        for (int i = 1; i <= 5; i++) {
            events.add(create(4663, "C:\\Program Files\\Office\\WINWORD.EXE",
                    "report_" + i + ".docx"));
        }

        for (int i = 1; i <= 3; i++) {
            events.add(create(4688, "C:\\Windows\\System32\\CMD.EXE", null));
        }

        events.add(create(1102, null, null));

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
