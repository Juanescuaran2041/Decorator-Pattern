package pipeline;

import model.Event;
import model.EventSource;

/**
 * Decorador que deja en process solo el nombre del ejecutable, en minusculas.
 */
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
            int lastSeparator = process.lastIndexOf('\\');
            if (lastSeparator >= 0) {
                process = process.substring(lastSeparator + 1);
            }
            event.put("process", process.toLowerCase());
        }
        return event;
    }
}
