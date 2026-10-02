public class Main {

    public static void main(String[] args) {
        String origin = "test";
        if (args.length > 0) {
            origin = args[0];
        }
        EventSource source = EventSourceFactory.create(origin);
    }
}
