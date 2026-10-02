package report;

import java.util.Arrays;

/**
 * Plantilla predisenada para un resumen ejecutivo, sin detalle de riesgo.
 */
public class ExecutiveSummaryTemplate extends ReportTemplate {

    public ExecutiveSummaryTemplate() {
        super("Executive Summary", new HtmlReportFactory(), false,
                Arrays.asList("only alerts"),
                Arrays.asList("totalEvents", "alerts"));
    }
}
