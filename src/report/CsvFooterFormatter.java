package report;

/**
 * Pie CSV: linea de comentario con el resumen final del reporte.
 */
public class CsvFooterFormatter implements FooterFormatter {

    @Override
    public String formatFooter(ReportMetadata metadata) {
        return "# summary: " + metadata.summary();
    }
}
