package report;

/**
 * Fabrica concreta de la familia CSV (valores separados por comas).
 */
public class CsvReportFactory implements ReportFactory {

    @Override
    public HeaderFormatter createHeaderFormatter() {
        return new CsvHeaderFormatter();
    }

    @Override
    public BodyFormatter createBodyFormatter() {
        return new CsvBodyFormatter();
    }

    @Override
    public FooterFormatter createFooterFormatter() {
        return new CsvFooterFormatter();
    }

    @Override
    public String getFormatName() {
        return "CSV";
    }
}
