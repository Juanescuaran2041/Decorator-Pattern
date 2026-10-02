package report;

import java.util.Locale;

/**
 * Metadatos que acompanan al reporte: titulo, formato, fecha y metricas calculadas.
 */
public class ReportMetadata {

    public static final int ALERT_THRESHOLD = 50;

    private final String title;
    private final String format;
    private final String generatedAt;
    private final int totalEvents;
    private final double averageRisk;
    private final int alerts;
    private final boolean includeRiskAnalysis;

    public ReportMetadata(String title, String format, String generatedAt, int totalEvents,
                          double averageRisk, int alerts, boolean includeRiskAnalysis) {
        this.title = title;
        this.format = format;
        this.generatedAt = generatedAt;
        this.totalEvents = totalEvents;
        this.averageRisk = averageRisk;
        this.alerts = alerts;
        this.includeRiskAnalysis = includeRiskAnalysis;
    }

    public String getTitle() {
        return title;
    }

    public String getFormat() {
        return format;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public double getAverageRisk() {
        return averageRisk;
    }

    public int getAlerts() {
        return alerts;
    }

    public boolean isIncludeRiskAnalysis() {
        return includeRiskAnalysis;
    }

    /**
     * Promedio de riesgo con dos decimales y punto decimal, independiente del idioma del sistema.
     */
    public String getAverageRiskText() {
        return String.format(Locale.US, "%.2f", averageRisk);
    }

    /**
     * Resumen final del reporte, incluyendo el riesgo promedio solo si el analisis esta activo.
     */
    public String summary() {
        String summary = totalEvents + " events processed, " + alerts + " alerts";
        if (includeRiskAnalysis) {
            summary += ", average risk " + getAverageRiskText();
        }
        return summary;
    }
}
