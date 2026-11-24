package company.vk.edu.distrib.compute.vagifbaratov.urlshortener.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RedirectHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(RedirectHandler.class);
    private final Dao<String> dao;

    public RedirectHandler(Dao<String> dao) {
        this.dao = dao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final var pathParts = exchange.getRequestURI().getPath().split("/");
        final var id = pathParts[pathParts.length - 1];

        log.info("Received req GET / with id: {}", id);
        validateID(id);

        final var longLink = dao.get(id);
        exchange.getResponseHeaders().add("Location", longLink);
        exchange.sendResponseHeaders(301, 0);
        exchange.close();
    }

    private void validateID(String id) {
        if (!id.matches("[A-Za-z0-9]{10}")) {
            throw new IllegalArgumentException("Invalid ID: " + id);
        }
    }
}
