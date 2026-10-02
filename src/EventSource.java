/**
 * Component of the Decorator pattern.
 * Both real sources and decorators implement this interface.
 */
public interface EventSource {

    /** Returns the next event, or null when there are no more events. */
    Event next();
}
