package com.pipelinelogs.decorator;

import com.pipelinelogs.model.Event;
import com.pipelinelogs.source.EventSource;

public abstract class SourceDecorator implements EventSource {

    protected final EventSource source;

    protected SourceDecorator(EventSource source) {
        this.source = source;
    }
}
