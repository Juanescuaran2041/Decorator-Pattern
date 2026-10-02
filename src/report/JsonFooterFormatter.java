package report;

/**
 * Pie en sintaxis JSON: cierra el arreglo de eventos y agrega el resumen.
 */
public class JsonFooterFormatter implements FooterFormatter {

    @Override
    public String formatFooter(ReportMetadata metadata) {
        return "  ],\n  \"summary\": " + Json.string(metadata.summary()) + "\n}";
    }
}
