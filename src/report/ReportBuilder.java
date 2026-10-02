package report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Event;

/**
 * Builder que ensambla un Report paso a paso y coordina la fabrica de formateadores.
 */
public class ReportBuilder {

    private String title = "Event Report";
    private ReportFactory factory = new JsonReportFactory();
    private boolean includeRiskAnalysis;
    private final List<Event> events = new ArrayList<>();

    public ReportBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public ReportBuilder setFactory(ReportFactory factory) {
        this.factory = factory;
        return this;
    }

    public ReportBuilder includeRiskAnalysis(boolean include) {
        this.includeRiskAnalysis = include;
        return this;
    }

    public ReportBuilder addEvents(List<Event> events) {
        this.events.addAll(events);
        return this;
    }

    /**
     * Calcula los metadatos y pide a la fabrica el encabezado, el cuerpo y el pie.
     */
    public Report build() {
        ReportMetadata metadata = new ReportMetadata(title, factory.getFormatName(),
                LocalDateTime.now().toString(), events.size(), averageRisk(), countAlerts(),
                includeRiskAnalysis);

        HeaderFormatter header = factory.createHeaderFormatter();
        BodyFormatter body = factory.createBodyFormatter();
        FooterFormatter footer = factory.createFooterFormatter();

        String content = header.formatHeader(metadata) + "\n"
                + body.formatBody(events, metadata) + "\n"
                + footer.formatFooter(metadata);
        return new Report(metadata, content);
    }

    private double averageRisk() {
        if (events.isEmpty()) {
            return 0.0;
        }
        int total = 0;
        for (Event event : events) {
            total += event.getScore();
        }
        return (double) total / events.size();
    }

    private int countAlerts() {
        int alerts = 0;
        for (Event event : events) {
            if (event.getScore() >= ReportMetadata.ALERT_THRESHOLD) {
                alerts++;
            }
        }
        return alerts;
    }
}
