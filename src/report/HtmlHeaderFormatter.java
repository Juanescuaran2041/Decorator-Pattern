package report;

/**
 * Encabezado HTML: documento, estilos y apertura de la tabla de eventos.
 */
public class HtmlHeaderFormatter implements HeaderFormatter {

    @Override
    public String formatHeader(ReportMetadata metadata) {
        StringBuilder header = new StringBuilder();
        header.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n");
        header.append("<meta charset=\"UTF-8\">\n");
        header.append("<title>").append(Html.escape(metadata.getTitle())).append("</title>\n");
        header.append("<style>");
        header.append("body{font-family:Arial,sans-serif;margin:24px;}");
        header.append("table{border-collapse:collapse;}");
        header.append("th,td{border:1px solid #ccc;padding:6px 10px;}");
        header.append("th{background:#eee;}tr.alert{background:#ffe0e0;}");
        header.append("</style>\n</head>\n<body>\n");
        header.append("<h1>").append(Html.escape(metadata.getTitle())).append("</h1>\n");
        header.append("<p>Format: ").append(Html.escape(metadata.getFormat()))
                .append(" | Generated: ").append(Html.escape(metadata.getGeneratedAt()))
                .append(" | Events: ").append(metadata.getTotalEvents())
                .append(" | Alerts: ").append(metadata.getAlerts());
        if (metadata.isIncludeRiskAnalysis()) {
            header.append(" | Average risk: ").append(metadata.getAverageRiskText());
        }
        header.append("</p>\n");
        header.append("<table>\n<thead>\n<tr><th>ID</th><th>Category</th><th>Process</th><th>File</th>");
        if (metadata.isIncludeRiskAnalysis()) {
            header.append("<th>Score</th><th>Alert</th>");
        }
        header.append("</tr>\n</thead>\n<tbody>");
        return header.toString();
    }
}
