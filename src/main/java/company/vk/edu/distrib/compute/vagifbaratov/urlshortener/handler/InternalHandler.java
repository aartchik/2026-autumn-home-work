package company.vk.edu.distrib.compute.vagifbaratov.urlshortener.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class InternalHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(InternalHandler.class);
    private final Dao<String> credentialsDao;

    public InternalHandler(Dao<String> dao) {
        this.credentialsDao = dao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final var contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        final var body = new String(exchange.getRequestBody().readAllBytes());
        log.info("Received req with: content-type {}", contentType);

        if (!Objects.equals(contentType, "text/html; charset=utf-8")) {
            throw new IllegalArgumentException("Invalid content type: " + contentType);
        }
        final var credentials = body.split(":", 2);

        credentialsDao.upsert(credentials[0], credentials[1]);

        exchange.sendResponseHeaders(200, -1);
    }
}
