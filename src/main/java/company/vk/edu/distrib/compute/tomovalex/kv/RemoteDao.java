package company.vk.edu.distrib.compute.tomovalex.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.NoSuchElementException;

public class RemoteDao implements Dao<String> {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final int NOT_FOUND = 404;
    private static final int BAD_REQUEST = 400;

    private final String entityUrl;
    private final HttpClient client;

    public RemoteDao(int port) {
        this.entityUrl = "http://localhost:" + port + "/v0/entity?id=";
        this.client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public String get(String key) throws IOException {
        HttpRequest request = requestBuilder(key).GET().build();
        HttpResponse<byte[]> response = send(request);
        if (response.statusCode() == NOT_FOUND) {
            throw new NoSuchElementException(key);
        }
        checkStatus(response, 200);
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        HttpRequest.Builder builder = requestBuilder(key);
        if (value == null) {
            throw new IllegalArgumentException();
        }
        HttpRequest request = builder.PUT(HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8)).build();
        checkStatus(send(request), 201);
    }

    @Override
    public void delete(String key) throws IOException {
        HttpRequest request = requestBuilder(key).DELETE().build();
        checkStatus(send(request), 202);
    }

    @Override
    public void close() {
        client.close();
    }

    private HttpRequest.Builder requestBuilder(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException();
        }
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        return HttpRequest.newBuilder(URI.create(entityUrl + encodedKey)).timeout(TIMEOUT);
    }

    private HttpResponse<byte[]> send(HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    private static void checkStatus(HttpResponse<byte[]> response, int expected) throws IOException {
        if (response.statusCode() == BAD_REQUEST) {
            throw new IllegalArgumentException("Invalid request");
        }
        if (response.statusCode() != expected) {
            throw new IOException("Unexpected HTTP status: " + response.statusCode());
        }
    }
}
