package pipeline;

import model.EventSource;

/**
 * Decorator abstracto: envuelve otra fuente y delega en ella la lectura del siguiente evento.
 */
public abstract class SourceDecorator implements EventSource {

    protected final EventSource source;

    public SourceDecorator(EventSource source) {
        this.source = source;
    }
}
