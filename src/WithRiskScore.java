import java.util.HashMap;
import java.util.Map;

public class WithRiskScore extends SourceDecorator {

    private static final int ACCESS_LIMIT = 100;

    private final Map<String, Integer> accessCountWithoutTimeWindow = new HashMap<>();

    public WithRiskScore(EventSource source) {
        super(source);
    }

    @Override
    public Event next() {
        Event event = source.next();
        if (event == null) {
            return null;
        }

        String category = event.get("category");
        String process = event.get("process");
        String file = event.get("file");

        if ("FILE_ACCESS".equals(category) && process != null) {
            int count = accessCountWithoutTimeWindow.getOrDefault(process, 0) + 1;
            accessCountWithoutTimeWindow.put(process, count);
            if (count > ACCESS_LIMIT) {
                event.addScore(50);
            }
        }

        if (file != null) {
            String fileName = file.toLowerCase();
            if (fileName.endsWith(".locked") || fileName.endsWith(".encrypted") || fileName.endsWith(".crypt")) {
                event.addScore(30);
            }
        }
        return event;
    }
}
