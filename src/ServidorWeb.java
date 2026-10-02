import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente web: sirve el frontend de la carpeta web/ y expone el análisis en /api/analizar.
 * Usa el servidor HTTP incluido en el JDK, sin librerías externas.
 */
public class ServidorWeb {

    private static final int PUERTO = 8080;
    private static final int MAXIMO_EVENTOS_REALES = 5000;
    private static final Path CARPETA_WEB = Paths.get("web");

    // Solo se sirven estos archivos, así no se puede pedir nada fuera de web/.
    private static final Map<String, String> ARCHIVOS_WEB = Map.of(
            "/index.html", "text/html; charset=utf-8",
            "/estilos.css", "text/css; charset=utf-8",
            "/app.js", "text/javascript; charset=utf-8");

    public static void main(String[] args) throws IOException {
        // Solo escucha en la propia máquina: el análisis puede leer logs reales del equipo.
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", PUERTO), 0);
        servidor.createContext("/api/analizar", ServidorWeb::analizar);
        servidor.createContext("/", ServidorWeb::servirArchivo);
        servidor.start();
        System.out.println("Servidor iniciado en http://localhost:" + PUERTO);
    }

    private static void analizar(HttpExchange intercambio) throws IOException {
        Map<String, String> parametros = leerParametros(intercambio.getRequestURI().getRawQuery());
        String origen = parametros.getOrDefault("origen", "prueba").trim();
        List<String> capas = Arrays.asList(
                parametros.getOrDefault("capas", "normalizacion,enriquecimiento,filtro,puntaje").split(","));

        try {
            FuenteEventos pipeline = armarPipeline(origen, capas);
            responder(intercambio, 200, "application/json; charset=utf-8", recorrer(pipeline, origen, capas));
        } catch (RuntimeException e) {
            responder(intercambio, 400, "application/json; charset=utf-8",
                    "{\"error\":" + textoJson(e.getMessage()) + "}");
        }
    }

    /** Arma un pipeline nuevo en cada petición, porque ConPuntajeRiesgo guarda estado. */
    private static FuenteEventos armarPipeline(String origen, List<String> capas) {
        FuenteEventos fuente;
        if (origen.isEmpty() || origen.equals("prueba")) {
            fuente = new FuenteEnMemoria(DatosPrueba.generar());
        } else {
            fuente = new FuenteLogWindows(origen, MAXIMO_EVENTOS_REALES);
        }

        // Las capas activas se envuelven siempre en el mismo orden, de adentro hacia afuera.
        if (capas.contains("normalizacion")) {
            fuente = new ConNormalizacion(fuente);
        }
        if (capas.contains("enriquecimiento")) {
            fuente = new ConEnriquecimiento(fuente);
        }
        if (capas.contains("filtro")) {
            fuente = new ConFiltro(fuente);
        }
        if (capas.contains("puntaje")) {
            fuente = new ConPuntajeRiesgo(fuente);
        }
        return fuente;
    }

    private static String recorrer(FuenteEventos pipeline, String origen, List<String> capas) {
        StringBuilder eventos = new StringBuilder();
        int procesados = 0;
        int alertas = 0;
        Evento evento;
        while ((evento = pipeline.siguiente()) != null) {
            boolean alerta = evento.getPuntaje() >= 50;
            if (alerta) {
                alertas++;
            }
            if (procesados > 0) {
                eventos.append(',');
            }
            procesados++;
            eventos.append("{\"id\":").append(evento.getId())
                    .append(",\"categoria\":").append(textoJson(evento.get("categoria")))
                    .append(",\"proceso\":").append(textoJson(evento.get("proceso")))
                    .append(",\"archivo\":").append(textoJson(evento.get("archivo")))
                    .append(",\"puntaje\":").append(evento.getPuntaje())
                    .append(",\"alerta\":").append(alerta)
                    .append('}');
        }

        List<String> capasActivas = new ArrayList<>();
        for (String capa : Arrays.asList("normalizacion", "enriquecimiento", "filtro", "puntaje")) {
            if (capas.contains(capa)) {
                capasActivas.add(textoJson(capa));
            }
        }

        return "{\"origen\":" + textoJson(origen.isEmpty() ? "prueba" : origen)
                + ",\"capas\":[" + String.join(",", capasActivas) + "]"
                + ",\"procesados\":" + procesados
                + ",\"alertas\":" + alertas
                + ",\"eventos\":[" + eventos + "]}";
    }

    private static void servirArchivo(HttpExchange intercambio) throws IOException {
        String ruta = intercambio.getRequestURI().getPath();
        if (ruta.equals("/")) {
            ruta = "/index.html";
        }

        String tipo = ARCHIVOS_WEB.get(ruta);
        if (tipo == null) {
            responder(intercambio, 404, "text/plain; charset=utf-8", "No encontrado");
            return;
        }

        try {
            byte[] contenido = Files.readAllBytes(CARPETA_WEB.resolve(ruta.substring(1)));
            responderBytes(intercambio, 200, tipo, contenido);
        } catch (IOException e) {
            responder(intercambio, 500, "text/plain; charset=utf-8",
                    "No se encontró la carpeta web/. Ejecuta el servidor desde la raíz del proyecto.");
        }
    }

    private static Map<String, String> leerParametros(String consulta) {
        Map<String, String> parametros = new HashMap<>();
        if (consulta == null) {
            return parametros;
        }
        for (String par : consulta.split("&")) {
            int igual = par.indexOf('=');
            if (igual > 0) {
                String clave = URLDecoder.decode(par.substring(0, igual), StandardCharsets.UTF_8);
                String valor = URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8);
                parametros.put(clave, valor);
            }
        }
        return parametros;
    }

    /** Convierte un texto a JSON, escapando comillas, barras invertidas (rutas de Windows) y controles. */
    private static String textoJson(String texto) {
        if (texto == null) {
            return "null";
        }
        StringBuilder json = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"':
                    json.append("\\\"");
                    break;
                case '\\':
                    json.append("\\\\");
                    break;
                case '\n':
                    json.append("\\n");
                    break;
                case '\r':
                    json.append("\\r");
                    break;
                case '\t':
                    json.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
            }
        }
        return json.append('"').toString();
    }

    private static void responder(HttpExchange intercambio, int codigo, String tipo, String cuerpo) throws IOException {
        responderBytes(intercambio, codigo, tipo, cuerpo.getBytes(StandardCharsets.UTF_8));
    }

    private static void responderBytes(HttpExchange intercambio, int codigo, String tipo, byte[] cuerpo) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.sendResponseHeaders(codigo, cuerpo.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }
}
