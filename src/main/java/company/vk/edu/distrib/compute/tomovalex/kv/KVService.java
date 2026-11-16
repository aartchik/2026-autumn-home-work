package company.vk.edu.distrib.compute.tomovalex.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KVService implements company.vk.edu.distrib.compute.kv.KVService {
    private static final String STATUS_PATH = "/v0/status";
    private static final String ENTITY_PATH = "/v0/entity";
    private static final String GET = "GET";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final int THREADS_COUNT = 8;

    private final HttpServer server;
    private final Dao<byte[]> dao;
    private final ExecutorService executor;

    public KVService(int port, Dao<byte[]> dao) throws IOException {
        this(port, dao, false);
    }

    public KVService(int port, Dao<byte[]> dao, boolean multithreaded) throws IOException {
        this.dao = dao;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.executor = Executors.newFixedThreadPool(multithreaded ? THREADS_COUNT : 1);
        server.setExecutor(executor);
        server.createContext("/", this::handleRequest);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(1);
        executor.close();
        try {
            dao.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if (STATUS_PATH.equals(path)) {
                sendEmptyResponse(exchange, GET.equals(exchange.getRequestMethod()) ? 200 : 405);
            } else if (ENTITY_PATH.equals(path)) {
                handleEntity(exchange);
            } else {
                sendEmptyResponse(exchange, 404);
            }
        }
    }

    private void handleEntity(HttpExchange exchange) throws IOException {
        try {
            String id = getId(exchange);
            if (id == null || id.isEmpty()) {
                sendEmptyResponse(exchange, 400);
                return;
            }

            handleEntityMethod(exchange, id);
        } catch (NoSuchElementException e) {
            sendEmptyResponse(exchange, 404);
        } catch (IllegalArgumentException e) {
            sendEmptyResponse(exchange, 400);
        } catch (IOException e) {
            sendEmptyResponse(exchange, 500);
        }
    }

    private void handleEntityMethod(HttpExchange exchange, String id) throws IOException {
        switch (exchange.getRequestMethod()) {
            case GET -> sendResponse(exchange, dao.get(id));
            case PUT -> {
                dao.upsert(id, exchange.getRequestBody().readAllBytes());
                sendEmptyResponse(exchange, 201);
            }
            case DELETE -> {
                dao.delete(id);
                sendEmptyResponse(exchange, 202);
            }
            default -> sendEmptyResponse(exchange, 405);
        }
    }

    private static String getId(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            String name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            if (Objects.equals(name, "id")) {
                return parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }

    private static void sendResponse(HttpExchange exchange, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, body.length == 0 ? -1 : body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static void sendEmptyResponse(HttpExchange exchange, int code) throws IOException {
        exchange.sendResponseHeaders(code, -1);
    }
}
