import java.util.Iterator;
import java.util.List;

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
