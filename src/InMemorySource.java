import java.util.Iterator;
import java.util.List;

/**
 * ConcreteComponent: delivers the events from an in-memory list.
 * Simulates reading the Event Log with test data.
 */
public class InMemorySource implements EventSource {

    private final Iterator<Event> iterator;

    public InMemorySource(List<Event> events) {
        this.iterator = events.iterator();
    }

    @Override
    public Event next() {
        return iterator.hasNext() ? iterator.next() : null;
    }
}
