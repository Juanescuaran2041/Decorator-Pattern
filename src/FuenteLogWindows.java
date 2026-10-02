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
 * ConcreteComponent: lee eventos reales de Windows con la herramienta wevtutil.
 * El origen puede ser un log del sistema (por ejemplo "Security", requiere administrador)
 * o la ruta de un archivo .evtx exportado (no requiere administrador).
 */
public class FuenteLogWindows implements FuenteEventos {

    private final String origen;
    private final int maximo;
    private Iterator<Evento> iterador;

    public FuenteLogWindows(String origen, int maximo) {
        this.origen = origen;
        this.maximo = maximo;
    }

    @Override
    public Evento siguiente() {
        // Los eventos se leen la primera vez que se piden.
        if (iterador == null) {
            iterador = leerEventos().iterator();
        }
        return iterador.hasNext() ? iterador.next() : null;
    }

    private List<Evento> leerEventos() {
        List<String> comando = new ArrayList<>();
        comando.add("wevtutil");
        comando.add("qe");
        comando.add(origen);
        if (origen.toLowerCase().endsWith(".evtx")) {
            comando.add("/lf:true");
        }
        comando.add("/c:" + maximo);
        comando.add("/rd:true");
        comando.add("/f:xml");
        comando.add("/e:Eventos");

        String salida;
        try {
            Process proceso = new ProcessBuilder(comando).redirectErrorStream(true).start();
            salida = leerTexto(proceso.getInputStream());
            if (proceso.waitFor() != 0) {
                throw new IllegalStateException("wevtutil no pudo leer '" + origen + "': " + salida.trim());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo ejecutar wevtutil: " + e.getMessage(), e);
        }

        List<Evento> eventos = convertirXml(salida);
        // /rd:true trae los más recientes primero; se invierte para procesarlos en orden cronológico.
        Collections.reverse(eventos);
        return eventos;
    }

    private List<Evento> convertirXml(String xml) {
        List<Evento> eventos = new ArrayList<>();
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document documento = fabrica.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            NodeList nodosEvento = documento.getElementsByTagName("Event");
            for (int i = 0; i < nodosEvento.getLength(); i++) {
                Element nodo = (Element) nodosEvento.item(i);
                int id = Integer.parseInt(nodo.getElementsByTagName("EventID").item(0).getTextContent().trim());
                Evento evento = new Evento(id);

                NodeList campos = nodo.getElementsByTagName("Data");
                for (int j = 0; j < campos.getLength(); j++) {
                    Element campo = (Element) campos.item(j);
                    String nombre = campo.getAttribute("Name");
                    String valor = campo.getTextContent();
                    // 4663 y 4660 usan ProcessName; 4688 usa NewProcessName.
                    if (nombre.equals("ProcessName") || nombre.equals("NewProcessName")) {
                        evento.put("proceso", valor);
                    } else if (nombre.equals("ObjectName")) {
                        evento.put("archivo", valor);
                    }
                }
                eventos.add(evento);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo interpretar el XML de wevtutil: " + e.getMessage(), e);
        }
        return eventos;
    }

    private String leerTexto(InputStream entrada) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] bufer = new byte[8192];
        int leidos;
        while ((leidos = entrada.read(bufer)) != -1) {
            bytes.write(bufer, 0, leidos);
        }
        // wevtutil escribe en la página de códigos ANSI de Windows (por ejemplo windows-1252), no en UTF-8.
        String codificacion = System.getProperty("sun.jnu.encoding", "windows-1252");
        return new String(bytes.toByteArray(), Charset.forName(codificacion));
    }
}
