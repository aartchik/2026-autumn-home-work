package company.vk.edu.distrib.compute.flighen.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityHandler implements HttpHandler {
    private static final Logger log =
            LoggerFactory.getLogger(EntityHandler.class);

    private static final Pattern ID_PATTERN = Pattern.compile("^id=([A-Za-z0-9_-]*)$");

    private final Dao<byte[]> dao;

    public EntityHandler(Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final var method = exchange.getRequestMethod();

        try (exchange) {
            switch (method) {
                case "GET":
                    handeGet(exchange);
                    break;
                case "PUT":
                    handlePut(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange);
                    break;
                default:
                    exchange.sendResponseHeaders(HttpStatusCodes.FORBIDDEN, -1);
                    break;
            }
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error(
                        "I/O error while handling {} {}",
                        exchange.getRequestMethod(),
                        exchange.getRequestURI(),
                        e
                );
            }
            throw e;
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        Optional<String> parsedId = getParameterId(exchange);

        if (parsedId.isEmpty()) {
            return;
        }

        String id = parsedId.get();

        try {
            dao.delete(id);

            exchange.sendResponseHeaders(HttpStatusCodes.ACCEPTED, -1);
        } catch (EmptyKeyException e) {
            exchange.sendResponseHeaders(HttpStatusCodes.BAD_REQUEST, -1);
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(HttpStatusCodes.UNPROCESSABLE_ENTITY, -1);
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        Optional<String> parsedId = getParameterId(exchange);

        if (parsedId.isEmpty()) {
            return;
        }

        String id = parsedId.get();

        byte[] value = exchange.getRequestBody().readAllBytes();

        try {
            dao.upsert(id, value);

            exchange.sendResponseHeaders(HttpStatusCodes.CREATED, -1);
        } catch (EmptyKeyException e) {
            exchange.sendResponseHeaders(HttpStatusCodes.BAD_REQUEST, -1);
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(HttpStatusCodes.UNPROCESSABLE_ENTITY, -1);
        }
    }

    private void handeGet(HttpExchange exchange) throws IOException {
        Optional<String> parsedId = getParameterId(exchange);

        if (parsedId.isEmpty()) {
            return;
        }

        String id = parsedId.get();

        try {
            byte[] value = dao.get(id);

            exchange.sendResponseHeaders(HttpStatusCodes.OK, value.length);

            exchange.getResponseBody().write(value);
        } catch (EmptyKeyException e) {
            exchange.sendResponseHeaders(HttpStatusCodes.BAD_REQUEST, -1);
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(HttpStatusCodes.UNPROCESSABLE_ENTITY, -1);
        } catch (NoSuchElementException e) {
            exchange.sendResponseHeaders(HttpStatusCodes.NOT_FOUND, -1);
        }
    }

    private Optional<String> getParameterId(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();

        if (query == null) {
            exchange.sendResponseHeaders(HttpStatusCodes.UNPROCESSABLE_ENTITY, -1);
            return Optional.empty();
        }

        Matcher matcher = ID_PATTERN.matcher(query);

        if (!matcher.matches()) {
            exchange.sendResponseHeaders(HttpStatusCodes.UNPROCESSABLE_ENTITY, -1);
            return Optional.empty();
        }

        String id = matcher.group(1);

        if (id.isEmpty()) {
            exchange.sendResponseHeaders(HttpStatusCodes.BAD_REQUEST, -1);
            return Optional.empty();
        }

        return Optional.of(id);
    }
}
