package company.vk.edu.distrib.compute.aartchik.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

final class KvHandler implements HttpHandler {
    private static final String ENTITY_PATH = "/v0/entity";
    private final FileDao dao;

    KvHandler(FileDao dao) {
        this.dao = dao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            Response response = process(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            if (response.body().length == 0) {
                exchange.sendResponseHeaders(response.status(), -1);
            } else {
                exchange.sendResponseHeaders(response.status(), response.body().length);
                exchange.getResponseBody().write(response.body());
            }
        }
    }

    private Response process(HttpExchange exchange) {
        try {
            return route(exchange);
        } catch (IllegalArgumentException invalid) {
            return new Response(400);
        } catch (NoSuchElementException missing) {
            return new Response(404);
        } catch (IOException failure) {
            return new Response(503);
        }
    }

    private Response route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getRawPath();
        if ("/v0/status".equals(path)) {
            return "GET".equals(exchange.getRequestMethod())
                    ? new Response(dao.isAvailable() ? 200 : 503)
                    : methodNotAllowed(exchange, "GET");
        }
        if (!ENTITY_PATH.equals(path)) {
            return new Response(404);
        }
        String key = readKey(exchange);
        return switch (exchange.getRequestMethod()) {
            case "GET" -> new Response(200, dao.get(key));
            case "PUT" -> {
                dao.upsert(key, exchange.getRequestBody().readAllBytes());
                yield new Response(201);
            }
            case "DELETE" -> {
                dao.delete(key);
                yield new Response(202);
            }
            default -> methodNotAllowed(exchange, "GET, PUT, DELETE");
        };
    }

    private static String readKey(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            throw new IllegalArgumentException("Missing id");
        }
        String key = "";
        boolean found = false;
        for (String parameter : query.split("&")) {
            int separator = parameter.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String name = URLDecoder.decode(parameter.substring(0, separator), StandardCharsets.UTF_8);
            if ("id".equals(name)) {
                if (found) {
                    throw new IllegalArgumentException("Duplicate id");
                }
                key = URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8);
                found = true;
            }
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Missing id");
        }
        return key;
    }

    private static Response methodNotAllowed(HttpExchange exchange, String methods) {
        exchange.getResponseHeaders().set("Allow", methods);
        return new Response(405);
    }

    private record Response(int status, byte[] body) {
        private Response(int status) {
            this(status, new byte[0]);
        }
    }
}
