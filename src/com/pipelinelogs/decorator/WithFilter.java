package com.pipelinelogs.decorator;

import com.pipelinelogs.model.Event;
import com.pipelinelogs.source.EventSource;

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
