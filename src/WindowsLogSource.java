import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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

    private List<Event> readEvents() {
        String xml = runWevtutil();
        List<Event> events = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            NodeList eventNodes = document.getElementsByTagName("Event");
            for (int i = 0; i < eventNodes.getLength(); i++) {
                Element eventNode = (Element) eventNodes.item(i);
                String eventId = eventNode.getElementsByTagName("EventID").item(0).getTextContent();
                events.add(new Event(Integer.parseInt(eventId.trim())));
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("Could not read the wevtutil XML: " + e.getMessage());
        }
        return events;
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
            String windowsEncoding = System.getProperty("sun.jnu.encoding", "windows-1252");
            String output = new String(process.getInputStream().readAllBytes(), Charset.forName(windowsEncoding));
            if (process.waitFor() != 0) {
                throw new IllegalStateException("wevtutil could not read '" + origin + "': " + output.trim());
            }
            return output;
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Could not run wevtutil: " + e.getMessage());
        }
    }
}
