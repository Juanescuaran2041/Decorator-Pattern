package pipeline;

import java.util.Iterator;
import java.util.List;
import model.Event;
import model.EventSource;

/**
 * ConcreteComponent que recorre en memoria una lista de eventos de prueba.
 */
public class InMemorySource implements EventSource {

    private final Iterator<Event> iterator;

    public InMemorySource(List<Event> events) {
        this.iterator = events.iterator();
    }

    @Override
    public Event next() {
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }
}
