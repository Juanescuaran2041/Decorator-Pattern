package report;

/**
 * Abstract Factory: familia de formateadores del encabezado de un reporte.
 */
public interface HeaderFormatter {

    String formatHeader(ReportMetadata metadata);
}
