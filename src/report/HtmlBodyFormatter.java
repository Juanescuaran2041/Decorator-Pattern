package report;

import java.util.List;
import model.Event;

/**
 * Cuerpo HTML: una fila de tabla por evento, resaltando las alertas.
 */
public class HtmlBodyFormatter implements BodyFormatter {

    @Override
    public String formatBody(List<Event> events, ReportMetadata metadata) {
        StringBuilder body = new StringBuilder();
        for (Event event : events) {
            boolean alert = event.getScore() >= ReportMetadata.ALERT_THRESHOLD;
            body.append("\n<tr");
            if (alert && metadata.isIncludeRiskAnalysis()) {
                body.append(" class=\"alert\"");
            }
            body.append("><td>").append(event.getId()).append("</td>")
                    .append("<td>").append(Html.escape(event.get("category"))).append("</td>")
                    .append("<td>").append(Html.escape(event.get("process"))).append("</td>")
                    .append("<td>").append(Html.escape(event.get("file"))).append("</td>");
            if (metadata.isIncludeRiskAnalysis()) {
                body.append("<td>").append(event.getScore()).append("</td>")
                        .append("<td>").append(alert).append("</td>");
            }
            body.append("</tr>");
        }
        return body.toString();
    }
}
