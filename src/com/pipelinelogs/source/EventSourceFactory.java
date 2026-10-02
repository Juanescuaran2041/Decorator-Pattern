package com.pipelinelogs.source;

import com.pipelinelogs.data.TestData;

public class EventSourceFactory {

    private static final int MAX_EVENTS = 5000;

    public static EventSource create(String origin) {
        if (origin == null || origin.isEmpty() || origin.equals("test")) {
            return new InMemorySource(TestData.generate());
        }
        return new WindowsLogSource(origin, MAX_EVENTS);
    }
}
