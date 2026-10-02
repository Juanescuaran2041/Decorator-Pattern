package report;

import java.util.List;
import model.Event;

/**
 * Cuerpo en sintaxis JSON: un objeto por evento, separados por comas.
 */
public class JsonBodyFormatter implements BodyFormatter {

    @Override
    public String formatBody(List<Event> events, ReportMetadata metadata) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) {
                body.append(",\n");
            }
            Event event = events.get(i);
            body.append("    {\"id\": ").append(event.getId())
                    .append(", \"category\": ").append(Json.string(event.get("category")))
                    .append(", \"process\": ").append(Json.string(event.get("process")))
                    .append(", \"file\": ").append(Json.string(event.get("file")));
            if (metadata.isIncludeRiskAnalysis()) {
                body.append(", \"score\": ").append(event.getScore())
                        .append(", \"alert\": ").append(event.getScore() >= ReportMetadata.ALERT_THRESHOLD);
            }
            body.append("}");
        }
        return body.toString();
    }
}
