package report;

/**
 * Encabezado CSV: comentarios con los metadatos y la fila de nombres de columna.
 */
public class CsvHeaderFormatter implements HeaderFormatter {

    @Override
    public String formatHeader(ReportMetadata metadata) {
        StringBuilder header = new StringBuilder();
        header.append("# title: ").append(metadata.getTitle()).append("\n");
        header.append("# format: ").append(metadata.getFormat()).append("\n");
        header.append("# generatedAt: ").append(metadata.getGeneratedAt()).append("\n");
        header.append("# totalEvents: ").append(metadata.getTotalEvents()).append("\n");
        header.append("# alerts: ").append(metadata.getAlerts()).append("\n");
        if (metadata.isIncludeRiskAnalysis()) {
            header.append("# averageRisk: ").append(metadata.getAverageRiskText()).append("\n");
        }
        header.append("id,category,process,file");
        if (metadata.isIncludeRiskAnalysis()) {
            header.append(",score,alert");
        }
        return header.toString();
    }
}
