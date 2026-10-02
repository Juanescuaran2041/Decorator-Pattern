package report;

import java.util.Arrays;

/**
 * Plantilla predisenada para una auditoria tecnica detallada con analisis de riesgo.
 */
public class AuditReportTemplate extends ReportTemplate {

    public AuditReportTemplate() {
        super("Audit Report", new JsonReportFactory(), true,
                Arrays.asList("category != OTHER", "score >= 50 marked as alert"),
                Arrays.asList("totalEvents", "alerts", "averageRisk"));
    }
}
