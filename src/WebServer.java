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

public class WebServer {

    private static final int PORT = 8080;
    private static final int ALERT_SCORE = 50;
    private static final String DEFAULT_LAYERS = "normalization,enrichment,filter,riskScore";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/api/analyze", WebServer::handleAnalyze);
        server.createContext("/", WebServer::handleStaticFile);
        server.start();
        System.out.println("Server running at http://localhost:" + PORT);
    }

    private static void handleAnalyze(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        String origin = getParameter(query, "origin", "test");
        String layers = getParameter(query, "layers", DEFAULT_LAYERS);

        try {
            PipelineBuilder builder = new PipelineBuilder(EventSourceFactory.create(origin));
            for (String layer : layers.split(",")) {
                addLayer(builder, layer.trim());
            }
            EventSource pipeline = builder.build();

            StringBuilder events = new StringBuilder();
            int processed = 0;
            int alerts = 0;
            Event event;
            while ((event = pipeline.next()) != null) {
                boolean alert = event.getScore() >= ALERT_SCORE;
                if (alert) {
                    alerts++;
                }
                if (processed > 0) {
                    events.append(",");
                }
                events.append(eventToJson(event, alert));
                processed++;
            }

            String json = "{\"origin\":" + jsonString(origin)
                    + ",\"processed\":" + processed
                    + ",\"alerts\":" + alerts
                    + ",\"events\":[" + events + "]}";
            send(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalStateException | IllegalArgumentException e) {
            String json = "{\"error\":" + jsonString(e.getMessage()) + "}";
            send(exchange, 400, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void addLayer(PipelineBuilder builder, String layer) {
        switch (layer) {
            case "normalization":
                builder.withNormalization();
                break;
            case "enrichment":
                builder.withEnrichment();
                break;
            case "filter":
                builder.withFilter();
                break;
            case "riskScore":
                builder.withRiskScore();
                break;
            case "":
                break;
            default:
                throw new IllegalArgumentException("Unknown layer: " + layer);
        }
    }

    private static String eventToJson(Event event, boolean alert) {
        return "{\"id\":" + event.getId()
                + ",\"category\":" + jsonString(event.get("category"))
                + ",\"process\":" + jsonString(event.get("process"))
                + ",\"file\":" + jsonString(event.get("file"))
                + ",\"score\":" + event.getScore()
                + ",\"alert\":" + alert + "}";
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String getParameter(String query, String name, String defaultValue) {
        if (query == null) {
            return defaultValue;
        }
        for (String pair : query.split("&")) {
            String[] keyAndValue = pair.split("=", 2);
            if (keyAndValue[0].equals(name) && keyAndValue.length == 2) {
                return URLDecoder.decode(keyAndValue[1], StandardCharsets.UTF_8);
            }
        }
        return defaultValue;
    }

    private static void handleStaticFile(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String fileName;
        String contentType;
        switch (path) {
            case "/":
            case "/index.html":
                fileName = "index.html";
                contentType = "text/html; charset=utf-8";
                break;
            case "/styles.css":
                fileName = "styles.css";
                contentType = "text/css; charset=utf-8";
                break;
            case "/app.js":
                fileName = "app.js";
                contentType = "application/javascript; charset=utf-8";
                break;
            default:
                send(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
                return;
        }

        Path file = Paths.get("web", fileName);
        if (!Files.exists(file)) {
            String message = "web/" + fileName + " not found. Run WebServer from the project root.";
            send(exchange, 404, "text/plain; charset=utf-8", message.getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(exchange, 200, contentType, Files.readAllBytes(file));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
