package pipeline;

import java.util.HashMap;
import java.util.Map;
import model.Event;
import model.EventSource;

/**
 * Decorador con estado que acumula un puntaje de riesgo en cada evento.
 */
public class WithRiskScore extends SourceDecorator {

    private static final int ACCESS_LIMIT = 100;
    private static final int ACCESS_POINTS = 50;
    private static final int EXTENSION_POINTS = 30;
    private static final int LOG_CLEARED_POINTS = 50;

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
        scoreFileAccess(event);
        scoreExtension(event);
        if ("LOG_CLEARED".equals(event.get("category"))) {
            event.addScore(LOG_CLEARED_POINTS);
        }
        return event;
    }

    private void scoreFileAccess(Event event) {
        if (!"FILE_ACCESS".equals(event.get("category"))) {
            return;
        }
        String process = event.get("process");
        int count = accessCountWithoutTimeWindow.getOrDefault(process, 0) + 1;
        accessCountWithoutTimeWindow.put(process, count);
        if (count > ACCESS_LIMIT) {
            event.addScore(ACCESS_POINTS);
        }
    }

    private void scoreExtension(Event event) {
        String file = event.get("file");
        if (file == null) {
            return;
        }
        if (file.endsWith(".locked") || file.endsWith(".encrypted") || file.endsWith(".crypt")) {
            event.addScore(EXTENSION_POINTS);
        }
    }
}
