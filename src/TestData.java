import java.util.ArrayList;
import java.util.List;

public class TestData {

    public static List<Event> generate() {
        List<Event> events = new ArrayList<>();

        for (int i = 1; i <= 120; i++) {
            Event event = new Event(4663);
            event.put("process", "C:\\Users\\victim\\AppData\\Local\\Temp\\EVIL.EXE");
            event.put("file", "document_" + i + ".docx.locked");
            events.add(event);
        }

        for (int i = 1; i <= 5; i++) {
            Event event = new Event(4663);
            event.put("process", "C:\\Program Files\\Office\\WINWORD.EXE");
            event.put("file", "report_" + i + ".docx");
            events.add(event);
        }

        for (int i = 1; i <= 3; i++) {
            Event event = new Event(4688);
            event.put("process", "C:\\Windows\\System32\\CMD.EXE");
            events.add(event);
        }
        return events;
    }
}
