public abstract class SourceDecorator implements EventSource {

    protected final EventSource source;

    protected SourceDecorator(EventSource source) {
        this.source = source;
    }
}
