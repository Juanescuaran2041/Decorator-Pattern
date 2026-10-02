package report;

/**
 * Abstract Factory: familia de formateadores del pie de un reporte.
 */
public interface FooterFormatter {

    String formatFooter(ReportMetadata metadata);
}
