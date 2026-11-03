package company.vk.edu.distrib.compute.aartchik.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

final class KvHandler implements HttpHandler {
    private static final String ENTITY_PATH = "/v0/entity";
    private final FileDao dao;
    private final int port;
    private final ReplicaCoordinator coordinator;

    KvHandler(FileDao dao, int port, ReplicaCoordinator coordinator) {
        this.dao = dao;
        this.port = port;
        this.coordinator = coordinator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            KvResponse response = process(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            if (response.body().length == 0) {
                exchange.sendResponseHeaders(response.status(), -1);
            } else {
                exchange.sendResponseHeaders(response.status(), response.body().length);
                exchange.getResponseBody().write(response.body());
            }
        }
    }

    private KvResponse process(HttpExchange exchange) {
        try {
            return route(exchange);
        } catch (IllegalArgumentException invalid) {
            return new KvResponse(400);
        } catch (IOException failure) {
            return new KvResponse(503);
        }
    }

    private KvResponse route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getRawPath();
        if ("/v0/status".equals(path)) {
            return status(exchange);
        }
        if (!ENTITY_PATH.equals(path) && !ReplicaCoordinator.INTERNAL_PATH.equals(path)) {
            return new KvResponse(404);
        }
        String key = readKey(exchange);
        String method = exchange.getRequestMethod();
        if (!"GET".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method)) {
            return methodNotAllowed(exchange, "GET, PUT, DELETE");
        }
        if (ReplicaCoordinator.INTERNAL_PATH.equals(path)) {
            byte[] body = readBody(exchange, method);
            return coordinator.local(method, key, body);
        }
        ReplicaConfiguration configuration = ReplicaConfiguration.load(port);
        int ack = readAck(exchange, configuration.factor());
        return coordinator.execute(method, key, readBody(exchange, method), ack, configuration);
    }

    private KvResponse status(HttpExchange exchange) {
        return "GET".equals(exchange.getRequestMethod())
                ? new KvResponse(dao.isAvailable() ? 200 : 503)
                : methodNotAllowed(exchange, "GET");
    }

    private static int readAck(HttpExchange exchange, int factor) {
        String value = readParameter(exchange, "ack");
        if (value == null) {
            return 1;
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty ack");
        }
        if (!value.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid ack");
        }
        int ack = Integer.parseInt(value);
        if (ack < 1 || ack > factor) {
            throw new IllegalArgumentException("Invalid ack");
        }
        return ack;
    }

    private static String readKey(HttpExchange exchange) {
        String key = readParameter(exchange, "id");
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Missing id");
        }
        return key;
    }

    private static @Nullable String readParameter(HttpExchange exchange, String requested) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            throw new IllegalArgumentException("Missing id");
        }
        @Nullable String key = null;
        for (String parameter : query.split("&")) {
            int separator = parameter.indexOf('=');
            String name = URLDecoder.decode(separator < 0 ? parameter : parameter.substring(0, separator),
                    StandardCharsets.UTF_8);
            if (requested.equals(name)) {
                if (key != null) {
                    throw new IllegalArgumentException("Duplicate " + requested);
                }
                key = separator < 0 ? ""
                        : URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return key;
    }

    private static byte[] readBody(HttpExchange exchange, String method) throws IOException {
        return "PUT".equals(method) ? exchange.getRequestBody().readAllBytes() : new byte[0];
    }

    private static KvResponse methodNotAllowed(HttpExchange exchange, String methods) {
        exchange.getResponseHeaders().set("Allow", methods);
        return new KvResponse(405);
    }

}
