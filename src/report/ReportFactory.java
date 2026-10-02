package report;

/**
 * Fabrica abstracta que crea la familia completa de formateadores de un formato de salida.
 */
public interface ReportFactory {

    HeaderFormatter createHeaderFormatter();

    BodyFormatter createBodyFormatter();

    FooterFormatter createFooterFormatter();

    String getFormatName();
}
