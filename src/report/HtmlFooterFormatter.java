package report;

/**
 * Pie HTML: cierre de la tabla, resumen y fin del documento.
 */
public class HtmlFooterFormatter implements FooterFormatter {

    @Override
    public String formatFooter(ReportMetadata metadata) {
        return "</tbody>\n</table>\n<p>" + Html.escape(metadata.summary()) + "</p>\n</body>\n</html>";
    }
}
