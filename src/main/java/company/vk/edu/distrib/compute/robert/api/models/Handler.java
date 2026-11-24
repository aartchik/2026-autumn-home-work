package company.vk.edu.distrib.compute.robert.api.models;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class Handler implements HttpHandler {
    protected static void sendResponse(HttpExchange exchange, Response response) throws IOException {
        response.headers().forEach((key, value) -> {
            exchange.getResponseHeaders().set(key, value);
        });

        byte[] bodyBytes = response.body();
        exchange.sendResponseHeaders(response.status(), bodyBytes.length);

        try (exchange) {
            exchange.getResponseBody().write(bodyBytes);
        }
    }
}
