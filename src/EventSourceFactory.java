public class EventSourceFactory {

    private static final int MAX_REAL_EVENTS = 5000;

    public static EventSource create(String origin) {
        if (origin == null || origin.isBlank() || origin.equals("test")) {
            return new InMemorySource(TestData.generate());
        }
        return new WindowsLogSource(origin.trim(), MAX_REAL_EVENTS);
    }
}
