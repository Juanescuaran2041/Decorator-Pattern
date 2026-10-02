public class Main {

    public static void main(String[] args) {
        String origin = "test";
        if (args.length > 0) {
            origin = args[0];
        }
        EventSource source = EventSourceFactory.create(origin);

        EventSource pipeline =
                new WithRiskScore(
                        new WithFilter(
                                new WithEnrichment(
                                        new WithNormalization(source))));

        int processed = 0;
        int alerts = 0;
        Event event = pipeline.next();
        while (event != null) {
            processed++;
            if (event.getScore() >= 50) {
                alerts++;
                System.out.println("ALERT score=" + event.getScore()
                        + " category=" + event.get("category")
                        + " process=" + event.get("process")
                        + " file=" + event.get("file"));
            }
            event = pipeline.next();
        }

        System.out.println("----------------------------------------");
        System.out.println("Processed events: " + processed);
        System.out.println("Alerts: " + alerts);
    }
}
