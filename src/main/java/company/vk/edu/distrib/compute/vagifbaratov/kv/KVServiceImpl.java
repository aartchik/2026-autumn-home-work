package company.vk.edu.distrib.compute.vagifbaratov.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class KVServiceImpl implements KVService {
    public static final String ENTITY_PATH = "/v0/entity";
    public static final String STATUS_PATH = "/v0/status";
    public static final String ID_ARG = "id";

    private final HttpServer server;
    private final Dao<byte[]> dao;

    public KVServiceImpl(int port, Dao<byte[]> dao) throws IOException {
        this.dao = dao;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(0);
        try {
            dao.close();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                dispatch(exchange);
            } catch (IllegalArgumentException exception) {
                exchange.sendResponseHeaders(400, -1);
            } catch (NoSuchElementException exception) {
                exchange.sendResponseHeaders(404, -1);
            } catch (IOException exception) {
                exchange.sendResponseHeaders(503, -1);
            }
        }
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (STATUS_PATH.equals(path)) {
            exchange.sendResponseHeaders("GET".equals(method) ? 200 : 503, -1);
            return;
        }
        if (!path.startsWith(ENTITY_PATH)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        handleEntity(exchange);
    }

    private void handleEntity(HttpExchange exchange) throws IOException {
        String key = extractKey(exchange);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must be non-empty");
        }

        switch (exchange.getRequestMethod()) {
            case "GET" -> {
                byte[] value = dao.get(key);
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.sendResponseHeaders(200, value.length);
                if (value.length > 0) {
                    exchange.getResponseBody().write(value);
                }
            }
            case "PUT" -> {
                dao.upsert(key, exchange.getRequestBody().readAllBytes());
                exchange.sendResponseHeaders(201, -1);
            }
            case "DELETE" -> {
                dao.delete(key);
                exchange.sendResponseHeaders(202, -1);
            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private static String extractKey(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();

        if (query != null) {
            for (String parameter : query.split("&")) {
                String[] pair = parameter.split("=", 2);
                String argName = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);

                if (ID_ARG.equals(argName)) {
                    return pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                }
            }
        }
        return "";
    }
}
