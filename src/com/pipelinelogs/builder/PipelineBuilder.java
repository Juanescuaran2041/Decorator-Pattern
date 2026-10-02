package com.pipelinelogs.builder;

import com.pipelinelogs.decorator.WithEnrichment;
import com.pipelinelogs.decorator.WithFilter;
import com.pipelinelogs.decorator.WithNormalization;
import com.pipelinelogs.decorator.WithRiskScore;
import com.pipelinelogs.source.EventSource;

public class PipelineBuilder {

    private final EventSource source;
    private boolean normalization;
    private boolean enrichment;
    private boolean filter;
    private boolean riskScore;

    public PipelineBuilder(EventSource source) {
        this.source = source;
    }

    public PipelineBuilder withNormalization() {
        normalization = true;
        return this;
    }

    public PipelineBuilder withEnrichment() {
        enrichment = true;
        return this;
    }

    public PipelineBuilder withFilter() {
        filter = true;
        return this;
    }

    public PipelineBuilder withRiskScore() {
        riskScore = true;
        return this;
    }

    public EventSource build() {
        EventSource pipeline = source;
        if (normalization) {
            pipeline = new WithNormalization(pipeline);
        }
        if (enrichment) {
            pipeline = new WithEnrichment(pipeline);
        }
        if (filter) {
            pipeline = new WithFilter(pipeline);
        }
        if (riskScore) {
            pipeline = new WithRiskScore(pipeline);
        }
        return pipeline;
    }
}
