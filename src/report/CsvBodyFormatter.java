package report;

import java.util.List;
import model.Event;

/**
 * Cuerpo CSV: una fila por evento, con columnas de riesgo solo si el analisis esta activo.
 */
public class CsvBodyFormatter implements BodyFormatter {

    @Override
    public String formatBody(List<Event> events, ReportMetadata metadata) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) {
                body.append("\n");
            }
            Event event = events.get(i);
            body.append(event.getId()).append(",")
                    .append(Csv.escape(event.get("category"))).append(",")
                    .append(Csv.escape(event.get("process"))).append(",")
                    .append(Csv.escape(event.get("file")));
            if (metadata.isIncludeRiskAnalysis()) {
                body.append(",").append(event.getScore())
                        .append(",").append(event.getScore() >= ReportMetadata.ALERT_THRESHOLD);
            }
        }
        return body.toString();
    }
}
