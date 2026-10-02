package report;

/**
 * Encabezado en sintaxis JSON: abre el objeto con los metadatos y el arreglo de eventos.
 */
public class JsonHeaderFormatter implements HeaderFormatter {

    @Override
    public String formatHeader(ReportMetadata metadata) {
        StringBuilder header = new StringBuilder();
        header.append("{\n");
        header.append("  \"title\": ").append(Json.string(metadata.getTitle())).append(",\n");
        header.append("  \"format\": ").append(Json.string(metadata.getFormat())).append(",\n");
        header.append("  \"generatedAt\": ").append(Json.string(metadata.getGeneratedAt())).append(",\n");
        header.append("  \"totalEvents\": ").append(metadata.getTotalEvents()).append(",\n");
        header.append("  \"alerts\": ").append(metadata.getAlerts()).append(",\n");
        if (metadata.isIncludeRiskAnalysis()) {
            header.append("  \"averageRisk\": ").append(metadata.getAverageRiskText()).append(",\n");
        }
        header.append("  \"events\": [");
        return header.toString();
    }
}
