package company.vk.edu.distrib.compute.vagifbaratov.urlshortener.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.vagifbaratov.urlshortener.ShortUrlGeneratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class LinksHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(LinksHandler.class);
    private final int port;
    private final Dao<String> dao;

    public LinksHandler(int port, Dao<String> dao) {
        this.port = port;
        this.dao = dao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final var method = exchange.getRequestMethod();

        switch (method) {
            case "POST" -> handlePost(exchange);
            case "GET" -> handleGet(exchange);
            case "PUT" -> handlePut(exchange);
            case "DELETE" -> handleDelete(exchange);
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        final var id = extractId(exchange);

        log.info("Received req DELETE /v0/links with id: {}", id);

        validateID(id);

        dao.delete(id);
        exchange.sendResponseHeaders(202, -1);
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        final var id = extractId(exchange);
        final var body = new String(exchange.getRequestBody().readAllBytes());

        log.info("Received req PUT /v0/links with id: {} body: {}", id, body);

        validateUrl(body);

        validateID(id);
        if (!existId(id)) {
            throw new NoSuchElementException();
        }

        dao.upsert(id, body);
        exchange.sendResponseHeaders(200, -1);
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        final var id = extractId(exchange);

        log.info("Received req GET /v0/links with id: {}", id);

        validateID(id);

        final var longLink = dao.get(id);
        writeTextBodyWithStatusCode(exchange, 200, longLink);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        final var body = new String(exchange.getRequestBody().readAllBytes());
        log.info("Received req POST /v0/links with body: {}", body);

        validateUrl(body);

        var id = ShortUrlGeneratorUtils.generate();
        while (existId(id)) {
            id = ShortUrlGeneratorUtils.generate();
        }

        dao.upsert(id, body);
        final var responseBody = "http://localhost:%d/%s".formatted(port, id);

        writeTextBodyWithStatusCode(exchange, 201, responseBody);
    }

    private boolean existId(String id) throws IOException {
        try {
            dao.get(id);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private String extractId(HttpExchange exchange) {
        final var pathParts = exchange.getRequestURI().getPath().split("/");
        return pathParts[pathParts.length - 1];
    }

    private void writeTextBodyWithStatusCode(
            HttpExchange exchange,
            int responseCode,
            String responseBody
    ) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(responseCode, responseBody.length());
        exchange.getResponseBody().write(
                responseBody.getBytes(StandardCharsets.UTF_8));
    }

    private void validateID(String id) {
        if (!id.matches("[A-Za-z0-9]{10}")) {
            throw new IllegalArgumentException("Invalid ID: " + id);
        }
    }

    private void validateUrl(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();

        if (!"http".equals(scheme)
                && !"https".equals(scheme)
                || uri.getHost() == null) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }
}

