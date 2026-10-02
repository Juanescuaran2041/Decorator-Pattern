package report;

/**
 * Fabrica concreta de la familia HTML.
 */
public class HtmlReportFactory implements ReportFactory {

    @Override
    public HeaderFormatter createHeaderFormatter() {
        return new HtmlHeaderFormatter();
    }

    @Override
    public BodyFormatter createBodyFormatter() {
        return new HtmlBodyFormatter();
    }

    @Override
    public FooterFormatter createFooterFormatter() {
        return new HtmlFooterFormatter();
    }

    @Override
    public String getFormatName() {
        return "HTML";
    }
}
