package pipeline;

import model.Event;
import model.EventSource;

/**
 * Decorador que agrega el campo category segun el Event ID de Windows.
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
        event.put("category", categoryFor(event.getId()));
        return event;
    }

    private String categoryFor(int eventId) {
        switch (eventId) {
            case 4663:
                return "FILE_ACCESS";
            case 4660:
                return "FILE_DELETE";
            case 4688:
                return "PROCESS_CREATION";
            case 1102:
                return "LOG_CLEARED";
            default:
                return "OTHER";
        }
    }
}
