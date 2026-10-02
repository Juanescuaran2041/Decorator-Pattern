public class WithNormalization extends SourceDecorator {

    public WithNormalization(EventSource source) {
        super(source);
    }

    @Override
    public Event next() {
        Event event = source.next();
        if (event == null) {
            return null;
        }

        String process = event.get("process");
        if (process != null) {
            String executable = process.substring(process.lastIndexOf('\\') + 1);
            event.put("process", executable.toLowerCase());
        }
        return event;
    }
}
