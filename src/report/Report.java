package report;

/**
 * Producto final del Builder: metadatos y contenido ya ensamblado del reporte.
 */
public class Report {

    private final ReportMetadata metadata;
    private final String content;

    public Report(ReportMetadata metadata, String content) {
        this.metadata = metadata;
        this.content = content;
    }

    public ReportMetadata getMetadata() {
        return metadata;
    }

    public String getContent() {
        return content;
    }

    public String getFormat() {
        return metadata.getFormat();
    }

    public String getTitle() {
        return metadata.getTitle();
    }
}
