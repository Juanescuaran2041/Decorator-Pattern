package com.pipelinelogs.app;

import com.pipelinelogs.builder.PipelineBuilder;
import com.pipelinelogs.model.Event;
import com.pipelinelogs.source.EventSource;
import com.pipelinelogs.source.EventSourceFactory;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServer {

    private static final int PORT = 8080;
    private static final Path WEB_FOLDER = Paths.get("web");

    private static final Map<String, String> WEB_FILES = Map.of(
            "/index.html", "text/html; charset=utf-8",
            "/styles.css", "text/css; charset=utf-8",
            "/app.js", "text/javascript; charset=utf-8");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/api/analyze", WebServer::handleAnalyze);
        server.createContext("/", WebServer::serveFile);
        server.start();
        System.out.println("Server started at http://localhost:" + PORT);
    }

    private static void handleAnalyze(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
        String origin = params.getOrDefault("origin", "test").trim();
        List<String> layers = Arrays.asList(
                params.getOrDefault("layers", "normalization,enrichment,filter,riskScore").split(","));
        try {
            EventSource pipeline = buildPipeline(origin, layers);
            respond(exchange, 200, "application/json; charset=utf-8", traverse(pipeline, origin));
        } catch (RuntimeException e) {
            respond(exchange, 400, "application/json; charset=utf-8",
                    "{\"error\":" + toJsonString(e.getMessage()) + "}");
        }
    }

    private static EventSource buildPipeline(String origin, List<String> layers) {
        EventSource source = EventSourceFactory.create(origin);
        PipelineBuilder builder = new PipelineBuilder(source);
        if (layers.contains("normalization")) {
            builder.withNormalization();
        }
        if (layers.contains("enrichment")) {
            builder.withEnrichment();
        }
        if (layers.contains("filter")) {
            builder.withFilter();
        }
        if (layers.contains("riskScore")) {
            builder.withRiskScore();
        }
        return builder.build();
    }

    private static String traverse(EventSource pipeline, String origin) {
        StringBuilder events = new StringBuilder();
        int processed = 0;
        int alerts = 0;
        Event event;
        while ((event = pipeline.next()) != null) {
            boolean alert = event.getScore() >= 50;
            if (alert) {
                alerts++;
            }
            if (processed > 0) {
                events.append(',');
            }
            processed++;
            events.append("{\"id\":").append(event.getId())
                    .append(",\"category\":").append(toJsonString(event.get("category")))
                    .append(",\"process\":").append(toJsonString(event.get("process")))
                    .append(",\"file\":").append(toJsonString(event.get("file")))
                    .append(",\"score\":").append(event.getScore())
                    .append(",\"alert\":").append(alert)
                    .append('}');
        }
        return "{\"origin\":" + toJsonString(origin.isEmpty() ? "test" : origin)
                + ",\"processed\":" + processed
                + ",\"alerts\":" + alerts
                + ",\"events\":[" + events + "]}";
    }

    private static void serveFile(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        String type = WEB_FILES.get(path);
        if (type == null) {
            respond(exchange, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }
        try {
            byte[] content = Files.readAllBytes(WEB_FOLDER.resolve(path.substring(1)));
            respondBytes(exchange, 200, type, content);
        } catch (IOException e) {
            respond(exchange, 500, "text/plain; charset=utf-8",
                    "web/ folder not found. Run the server from the project root.");
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) {
            return params;
        }
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                String key = URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    private static String toJsonString(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder json = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
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

    private static void respond(HttpExchange exchange, int code, String type, String body) throws IOException {
        respondBytes(exchange, code, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void respondBytes(HttpExchange exchange, int code, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
