/**
 * Decorator that adds the "category" field according to the Windows Event ID.
 */
public class WithEnrichment extends SourceDecorator {

    public WithEnrichment(EventSource source) {
        super(source);
    }

    @Override
    public Event next() {
        Event event = source.next();
        if (event == null) {
            return null;
        }

        event.put("category", categoryOf(event.getId()));
        return event;
    }

    private String categoryOf(int id) {
        switch (id) {
            case 4663:
                return "FILE_ACCESS";
            case 4660:
                return "FILE_DELETION";
            case 4688:
                return "PROCESS_CREATION";
            case 1102:
                return "LOG_CLEARED";
            default:
                return "OTHER";
        }
    }
}
