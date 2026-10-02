import java.util.HashMap;
import java.util.Map;

/**
 * Stateful decorator that adds ransomware risk score to each event.
 */
public class WithRiskScore extends SourceDecorator {

    private static final int ACCESS_THRESHOLD = 100;

    private final Map<String, Integer> accessesByProcess = new HashMap<>();

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
            int total = accessesByProcess.getOrDefault(process, 0) + 1;
            accessesByProcess.put(process, total);
            if (total > ACCESS_THRESHOLD) {
                event.addScore(50);
            }
        }

        if (file != null && hasEncryptedExtension(file.toLowerCase())) {
            event.addScore(30);
        }

        if ("LOG_CLEARED".equals(category)) {
            event.addScore(50);
        }
        return event;
    }

    private boolean hasEncryptedExtension(String file) {
        return file.endsWith(".locked")
                || file.endsWith(".encrypted")
                || file.endsWith(".crypt");
    }
}
