package pipeline;

import model.EventSource;

/**
 * Builder que arma el pipeline con las capas activas, siempre en el orden correcto.
 */
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
        this.normalization = true;
        return this;
    }

    public PipelineBuilder withEnrichment() {
        this.enrichment = true;
        return this;
    }

    public PipelineBuilder withFilter() {
        this.filter = true;
        return this;
    }

    public PipelineBuilder withRiskScore() {
        this.riskScore = true;
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
