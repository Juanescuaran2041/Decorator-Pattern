import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * ConcreteComponent: reads real Windows events with the wevtutil tool.
 * The source can be a system log (for example "Security", requires administrator)
 * or the path of an exported .evtx file (does not require administrator).
 */
public class WindowsLogSource implements EventSource {

    private final String origin;
    private final int maximum;
    private Iterator<Event> iterator;

    public WindowsLogSource(String origin, int maximum) {
        this.origin = origin;
        this.maximum = maximum;
    }

    @Override
    public Event next() {
        // Events are read the first time they are requested.
        if (iterator == null) {
            iterator = readEvents().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<Event> readEvents() {
        List<String> command = new ArrayList<>();
        command.add("wevtutil");
        command.add("qe");
        command.add(origin);
        if (origin.toLowerCase().endsWith(".evtx")) {
            command.add("/lf:true");
        }
        command.add("/c:" + maximum);
        command.add("/rd:true");
        command.add("/f:xml");
        command.add("/e:Events");

        String output;
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            output = readText(process.getInputStream());
            if (process.waitFor() != 0) {
                throw new IllegalStateException("wevtutil could not read '" + origin + "': " + output.trim());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not run wevtutil: " + e.getMessage(), e);
        }

        List<Event> events = parseXml(output);
        // /rd:true brings the newest first; reversed to process in chronological order.
        Collections.reverse(events);
        return events;
    }

    private List<Event> parseXml(String xml) {
        List<Event> events = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            NodeList eventNodes = document.getElementsByTagName("Event");
            for (int i = 0; i < eventNodes.getLength(); i++) {
                Element node = (Element) eventNodes.item(i);
                int id = Integer.parseInt(node.getElementsByTagName("EventID").item(0).getTextContent().trim());
                Event event = new Event(id);

                NodeList fields = node.getElementsByTagName("Data");
                for (int j = 0; j < fields.getLength(); j++) {
                    Element field = (Element) fields.item(j);
                    String name = field.getAttribute("Name");
                    String value = field.getTextContent();
                    // 4663 and 4660 use ProcessName; 4688 uses NewProcessName.
                    if (name.equals("ProcessName") || name.equals("NewProcessName")) {
                        event.put("process", value);
                    } else if (name.equals("ObjectName")) {
                        event.put("file", value);
                    }
                }
                events.add(event);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse wevtutil XML: " + e.getMessage(), e);
        }
        return events;
    }

    private String readText(InputStream input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            bytes.write(buffer, 0, read);
        }
        // wevtutil writes in the Windows ANSI code page (for example windows-1252), not UTF-8.
        String encoding = System.getProperty("sun.jnu.encoding", "windows-1252");
        return new String(bytes.toByteArray(), Charset.forName(encoding));
    }
}
