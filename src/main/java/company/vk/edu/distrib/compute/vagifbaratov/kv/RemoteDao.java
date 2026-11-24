package company.vk.edu.distrib.compute.vagifbaratov.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class RemoteDao implements Dao<String> {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String host;
    private final int port;

    private static final int NOT_FOUND = 404;
    private static final String GET = "GET";

    public RemoteDao(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String get(String key) throws IOException {
        return new String(request("GET", key, new byte[0], 200), StandardCharsets.UTF_8);
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        request("PUT", key, Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8), 201);
    }

    @Override
    public void delete(String key) throws IOException {
        request("DELETE", key, new byte[0], 202);
    }

    @Override
    public void close() {
        client.close();
    }

    private byte[] request(String method, String key, byte[] value, int expectedStatus) throws IOException {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must be non-empty");
        }

        var request = buildRequest(method, key, value);

        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", exception);
        }

        int status = response.statusCode();

        if (status == NOT_FOUND && GET.equals(method)) {
            throw new NoSuchElementException("Missing key: " + key);
        }

        if (status != expectedStatus) {
            throw new IOException("Unexpected KV response: " + status);
        }
        return response.body();
    }

    private HttpRequest buildRequest(String method, String key, byte[] value) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildEntityUri(key));

        switch (method) {
            case "GET" -> builder.GET();
            case "DELETE" -> builder.DELETE();
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofByteArray(value));
            default -> throw new IllegalArgumentException(
                    "Unsupported method: " + method
            );
        }

        return builder.build();
    }

    private URI buildEntityUri(String key) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String path = host + ":" + port + KVServiceImpl.ENTITY_PATH
                + "?" + KVServiceImpl.ID_ARG + "=";

        return URI.create(path + encodedKey);
    }
}

