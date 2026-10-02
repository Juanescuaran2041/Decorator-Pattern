import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WindowsLogSource implements EventSource {

    private final String origin;
    private final int maxEvents;

    public WindowsLogSource(String origin, int maxEvents) {
        this.origin = origin;
        this.maxEvents = maxEvents;
    }

    @Override
    public Event next() {
        return null;
    }

    private String runWevtutil() {
        List<String> command = new ArrayList<>();
        command.add("wevtutil");
        command.add("qe");
        command.add(origin);
        if (origin.toLowerCase().endsWith(".evtx")) {
            command.add("/lf:true");
        }
        command.add("/c:" + maxEvents);
        command.add("/rd:true");
        command.add("/f:xml");
        command.add("/e:Events");

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            if (process.waitFor() != 0) {
                throw new IllegalStateException("wevtutil could not read '" + origin + "': " + output.trim());
            }
            return output;
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Could not run wevtutil: " + e.getMessage());
        }
    }
}
