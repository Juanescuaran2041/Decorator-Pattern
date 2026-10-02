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
        return events;
    }
}
