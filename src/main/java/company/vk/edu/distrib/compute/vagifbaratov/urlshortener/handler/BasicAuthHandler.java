package company.vk.edu.distrib.compute.vagifbaratov.urlshortener.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Objects;

public class BasicAuthHandler implements HttpHandler {
    private static final String BASIC_PREFIX = "Basic ";
    private static final int CREDENTIALS_LENGTH = 2;
    private final HttpHandler delegate;
    private final Dao<String> credentialsDao;

    public BasicAuthHandler(HttpHandler delegate, Dao<String> credentialsDao) {
        this.delegate = delegate;
        this.credentialsDao = credentialsDao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final var authorization = exchange.getRequestHeaders().getFirst("Authorization");

        if (authorization == null || !authorization.startsWith(BASIC_PREFIX)) {
            throw new AuthenticationException("Unauthorized");
        }

        final var encoded = authorization.substring(BASIC_PREFIX.length());
        final var credentials = new String(
                Base64.getDecoder().decode(encoded),
                StandardCharsets.UTF_8
        ).split(":", 2);

        if (credentials.length != CREDENTIALS_LENGTH) {
            throw new AuthenticationException("Unauthorized");
        }

        if (!authenticate(credentials[0], credentials[1])) {
            throw new AuthenticationException("Unauthorized");
        }

        delegate.handle(exchange);
    }

    private boolean authenticate(String user, String password) throws IOException {
        try {
            final var expectedPassword = credentialsDao.get(user);
            if (Objects.equals(expectedPassword, password)) {
                return true;
            }
        } catch (NoSuchElementException e) {
            return false;
        }
        return false;
    }
}
