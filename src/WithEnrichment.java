public class WithEnrichment extends SourceDecorator {

    public WithEnrichment(EventSource source) {
        super(source);
    }

    @Override
    public Event next() {
        Event event = source.next();
        if (event == null) {
            return null;
        }

        String category;
        switch (event.getId()) {
            case 4663:
                category = "FILE_ACCESS";
                break;
            case 4660:
                category = "FILE_DELETE";
                break;
            case 4688:
                category = "PROCESS_CREATION";
                break;
            case 1102:
                category = "LOG_CLEARED";
                break;
            default:
                category = "OTHER";
        }
        event.put("category", category);
        return event;
    }
}
