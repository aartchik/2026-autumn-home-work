package company.vk.edu.distrib.compute.vagifbaratov.urlshortener.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class StatusHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(StatusHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log.info("Received req /v0/status");
        final var method = exchange.getRequestMethod();
        if (Objects.equals(method, "GET")) {
            exchange.sendResponseHeaders(200, 0);
        } else {
            exchange.sendResponseHeaders(405, 0);
        }

        exchange.close();
    }
}
