package pipeline;

import model.EventSource;

/**
 * Factory que decide que fuente de eventos crear segun el origen solicitado.
 */
public class EventSourceFactory {

    private static final int MAX_REAL_EVENTS = 5000;

    private EventSourceFactory() {
    }

    public static EventSource create(String origin) {
        if (origin == null || origin.trim().isEmpty() || origin.equalsIgnoreCase("test")) {
            return new InMemorySource(TestData.generate());
        }
        return new WindowsLogSource(origin, MAX_REAL_EVENTS);
    }
}
