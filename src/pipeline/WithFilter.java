package pipeline;

import model.Event;
import model.EventSource;

/**
 * Decorador que descarta los eventos de categoria OTHER.
 */
public class WithFilter extends SourceDecorator {

    public WithFilter(EventSource source) {
        super(source);
    }

    @Override
    public Event next() {
        Event event;
        while ((event = source.next()) != null) {
            if (!"OTHER".equals(event.get("category"))) {
                return event;
            }
        }
        return null;
    }
}
