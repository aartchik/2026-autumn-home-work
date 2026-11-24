package company.vk.edu.distrib.compute.vagifbaratov.urlshortener.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.util.NoSuchElementException;

public class ErrorHandler implements HttpHandler {
    private final HttpHandler delegate;

    public ErrorHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try (var exchange = httpExchange) {
            try {
                delegate.handle(exchange);
            } catch (NoSuchElementException e) {
                exchange.sendResponseHeaders(404, -1);
            } catch (IllegalArgumentException e) {
                exchange.sendResponseHeaders(422, -1);
            } catch (AuthenticationException e) {
                exchange.getResponseHeaders().add(
                        "WWW-Authenticate", "Basic realm=\"url-shortener\", charset=\"UTF-8\""
                );
                exchange.sendResponseHeaders(401, -1);
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }
}
