import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Event;
import model.EventSource;
import pipeline.EventSourceFactory;
import pipeline.PipelineBuilder;
import report.AuditReportTemplate;
import report.CsvReportFactory;
import report.ExecutiveSummaryTemplate;
import report.Report;
import report.ReportBuilder;
import report.ReportMetadata;
import report.ReportTemplate;
import report.ReportTemplateRegistry;

/**
 * Cliente de consola: ensambla el pipeline, lo recorre, imprime las alertas y demuestra
 * la exportacion de reportes con los patrones Abstract Factory, Builder y Prototype.
 *
 * Sin argumentos usa los datos de prueba. Con un argumento lee eventos reales de Windows:
 *   java -cp out Main Security              (requiere consola de administrador)
 *   java -cp out Main C:\ruta\archivo.evtx
 */
public class Main {

    public static void main(String[] args) {
        String origin = args.length > 0 ? args[0] : "test";

        EventSource pipeline = new PipelineBuilder(EventSourceFactory.create(origin))
                .withNormalization()
                .withEnrichment()
                .withFilter()
                .withRiskScore()
                .build();

        List<Event> events = new ArrayList<>();
        try {
            Event event;
            while ((event = pipeline.next()) != null) {
                events.add(event);
            }
        } catch (IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        printAlerts(events);
        exportReports(events);
    }

    private static void printAlerts(List<Event> events) {
        int alerts = 0;
        for (Event event : events) {
            if (event.getScore() >= ReportMetadata.ALERT_THRESHOLD) {
                alerts++;
                System.out.printf("ALERT score=%d category=%s process=%s file=%s%n",
                        event.getScore(), event.get("category"),
                        event.get("process"), event.get("file"));
            }
        }
        System.out.println("----------------------------------------");
        System.out.println("Processed events: " + events.size());
        System.out.println("Alerts: " + alerts);
    }

    /**
     * Flujo completo: clonar una plantilla (Prototype), personalizarla con el ReportBuilder
     * (Builder) y generar el documento en varios formatos con las fabricas (Abstract Factory).
     */
    private static void exportReports(List<Event> events) {
        ReportTemplateRegistry registry = new ReportTemplateRegistry();
        registry.register("audit", new AuditReportTemplate());
        registry.register("executive", new ExecutiveSummaryTemplate());
        System.out.println("Available templates: " + registry.keys());

        ReportTemplate auditTemplate = registry.get("audit");
        auditTemplate.setTitle("Ransomware Audit - " + LocalDate.now());
        System.out.println("Cloned template: " + auditTemplate);

        Report jsonReport = new ReportBuilder()
                .setTitle(auditTemplate.getTitle())
                .setFactory(auditTemplate.getFactory())
                .includeRiskAnalysis(auditTemplate.isIncludeRiskAnalysis())
                .addEvents(events)
                .build();

        Report csvReport = new ReportBuilder()
                .setTitle("Ransomware Audit (CSV export)")
                .setFactory(new CsvReportFactory())
                .includeRiskAnalysis(true)
                .addEvents(events)
                .build();

        ReportTemplate executiveTemplate = registry.get("executive");
        System.out.println("Cloned template: " + executiveTemplate);

        Report htmlReport = new ReportBuilder()
                .setTitle(executiveTemplate.getTitle() + " - " + LocalDate.now())
                .setFactory(executiveTemplate.getFactory())
                .includeRiskAnalysis(executiveTemplate.isIncludeRiskAnalysis())
                .addEvents(events)
                .build();

        printAndSave(jsonReport, "report.json");
        printAndSave(csvReport, "report.csv");
        printAndSave(htmlReport, "report.html");
    }

    private static void printAndSave(Report report, String fileName) {
        System.out.println("=== " + report.getFormat() + " REPORT: " + report.getTitle() + " ===");
        System.out.println(report.getContent());
        try {
            Path directory = Path.of("reports");
            Files.createDirectories(directory);
            Path file = directory.resolve(fileName);
            Files.writeString(file, report.getContent());
            System.out.println("Saved: " + file.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Could not save the report: " + e.getMessage());
        }
    }
}
