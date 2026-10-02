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

        Event event = pipeline.next();
        while (event != null) {
            if (event.getScore() >= 50) {
                System.out.println("ALERT score=" + event.getScore()
                        + " category=" + event.get("category")
                        + " process=" + event.get("process")
                        + " file=" + event.get("file"));
            }
            event = pipeline.next();
        }
    }
}
