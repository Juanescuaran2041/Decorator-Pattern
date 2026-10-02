package com.pipelinelogs.source;

import com.pipelinelogs.model.Event;

public interface EventSource {

    Event next();
}
