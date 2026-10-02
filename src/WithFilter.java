/**
 * Decorator that discards events with category OTHER.
 * Requests events from the inner source until it finds a valid one or reaches null.
 */
public class WithFilter extends SourceDecorator {

    public WithFilter(EventSource source) {
        super(source);
    }

    @Override
    public Event next() {
        Event event = source.next();
        while (event != null && "OTHER".equals(event.get("category"))) {
            event = source.next();
        }
        return event;
    }
}
