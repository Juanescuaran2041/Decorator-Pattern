package report;

/**
 * Fabrica concreta de la familia JSON.
 */
public class JsonReportFactory implements ReportFactory {

    @Override
    public HeaderFormatter createHeaderFormatter() {
        return new JsonHeaderFormatter();
    }

    @Override
    public BodyFormatter createBodyFormatter() {
        return new JsonBodyFormatter();
    }

    @Override
    public FooterFormatter createFooterFormatter() {
        return new JsonFooterFormatter();
    }

    @Override
    public String getFormatName() {
        return "JSON";
    }
}
