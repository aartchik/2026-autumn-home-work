package company.vk.edu.distrib.compute.robert.api.models;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;

public record Request(
    String method,
    String path,
    String query,
    byte[] body,
    Map<String, String> headers
) {
    public Request {
        body = body.clone();
        headers = Map.copyOf(headers);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public static Request from(HttpExchange exchange) throws IOException {
        Map<String, String> headers = new ConcurrentHashMap<>();
        exchange.getRequestHeaders().forEach(
            (name, values) -> headers.put(
                name.toLowerCase(Locale.ROOT),
                values.getFirst()
            )
        );
        return new Request(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getRawPath(),
            Objects.requireNonNullElse(exchange.getRequestURI().getRawQuery(), ""),
            exchange.getRequestBody().readAllBytes(),
            headers
        );
    }
}
