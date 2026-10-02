/**
 * Console client: assembles the pipeline, traverses it and prints the alerts.
 *
 * Without arguments it uses the test data. With one argument it reads real Windows events:
 *   java -cp out Main Security              (requires administrator console)
 *   java -cp out Main C:\path\file.evtx
 */
public class Main {

    private static final int MAX_REAL_EVENTS = 5000;

    public static void main(String[] args) {
        EventSource baseSource;
        if (args.length > 0) {
            baseSource = new WindowsLogSource(args[0], MAX_REAL_EVENTS);
        } else {
            baseSource = new InMemorySource(TestData.generate());
        }

        // From outside to inside: score -> filter -> enrichment -> normalization -> source.
        EventSource pipeline =
                new WithRiskScore(
                        new WithFilter(
                                new WithEnrichment(
                                        new WithNormalization(baseSource))));

        int processed = 0;
        int alerts = 0;
        Event event;
        try {
            while ((event = pipeline.next()) != null) {
                processed++;
                if (event.getScore() >= 50) {
                    alerts++;
                    System.out.printf("ALERT score=%d category=%s process=%s file=%s%n",
                            event.getScore(), event.get("category"),
                            event.get("process"), event.get("file"));
                }
            }
        } catch (IllegalStateException e) {
            // For example, reading the Security log without administrator rights.
            System.out.println("Error: " + e.getMessage());
            return;
        }

        System.out.println("----------------------------------------");
        System.out.println("Processed events: " + processed);
        System.out.println("Alerts: " + alerts);
    }
}
