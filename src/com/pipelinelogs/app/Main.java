package com.pipelinelogs.app;

import com.pipelinelogs.decorator.WithEnrichment;
import com.pipelinelogs.decorator.WithFilter;
import com.pipelinelogs.decorator.WithNormalization;
import com.pipelinelogs.decorator.WithRiskScore;
import com.pipelinelogs.model.Event;
import com.pipelinelogs.source.EventSource;
import com.pipelinelogs.source.EventSourceFactory;

public class Main {

    public static void main(String[] args) {
        String origin = args.length > 0 ? args[0] : "test";
        EventSource pipeline;
        try {
            pipeline = new WithRiskScore(
                    new WithFilter(
                            new WithEnrichment(
                                    new WithNormalization(
                                            EventSourceFactory.create(origin)))));
        } catch (IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }
        int processed = 0;
        int alerts = 0;
        try {
            Event event;
            while ((event = pipeline.next()) != null) {
                processed++;
                if (event.getScore() >= 50) {
                    alerts++;
                    System.out.printf("ALERT score=%d category=%s process=%s file=%s%n",
                            event.getScore(),
                            event.get("category"),
                            event.get("process"),
                            event.get("file"));
                }
            }
        } catch (IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }
        System.out.println("----------------------------------------");
        System.out.println("Processed events: " + processed);
        System.out.println("Alerts: " + alerts);
    }
}
