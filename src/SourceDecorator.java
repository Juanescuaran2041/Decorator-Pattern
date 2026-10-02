/**
 * Abstract decorator: wraps another EventSource.
 * Each concrete decorator requests the event from this inner source and adds behavior.
 */
public abstract class SourceDecorator implements EventSource {

    protected final EventSource source;

    protected SourceDecorator(EventSource source) {
        this.source = source;
    }
}
