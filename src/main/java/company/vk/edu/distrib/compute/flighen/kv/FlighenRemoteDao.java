package company.vk.edu.distrib.compute.flighen.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

public class FlighenRemoteDao implements Dao<String> {
    private static final String MEDIA_TYPE = "application/octet-stream";
    private final int port;
    private final HttpClient client;

    public FlighenRemoteDao(int port) throws IOException {
        this.port = port;
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        String encodedKey = URLEncoder.encode(
                key,
                StandardCharsets.UTF_8
        );

        URI uri = URI.create("http://localhost:%d/v0/entity?id=%s".formatted(port, encodedKey));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            switch (response.statusCode()) {
                case HttpStatusCodes.OK:
                    return new String(response.body(), StandardCharsets.UTF_8);
                case HttpStatusCodes.NOT_FOUND:
                    throw new NoSuchElementException("Key not found: " + key);
                case HttpStatusCodes.UNPROCESSABLE_ENTITY:
                    throw new IllegalArgumentException("Invalid key: " + key);
                default:
                    throw new IOException("Unexpected response status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        String encodedKey = URLEncoder.encode(
                key,
                StandardCharsets.UTF_8
        );

        URI uri = URI.create("http://localhost:%d/v0/entity?id=%s".formatted(port, encodedKey));

        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", MEDIA_TYPE)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(valueBytes))
                .build();

        try {
            HttpResponse<Void> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            switch (response.statusCode()) {
                case HttpStatusCodes.CREATED:
                    return;
                case HttpStatusCodes.UNPROCESSABLE_ENTITY:
                    throw new IllegalArgumentException("Invalid key: " + key);
                default:
                    throw new IOException("Unexpected response status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        String encodedKey = URLEncoder.encode(
                key,
                StandardCharsets.UTF_8
        );

        URI uri = URI.create("http://localhost:%d/v0/entity?id=%s".formatted(port, encodedKey));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .DELETE()
                .build();

        try {
            HttpResponse<byte[]> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            switch (response.statusCode()) {
                case HttpStatusCodes.ACCEPTED:
                    return;
                case HttpStatusCodes.NOT_FOUND:
                    throw new NoSuchElementException("Key not found: " + key);
                case HttpStatusCodes.UNPROCESSABLE_ENTITY:
                    throw new IllegalArgumentException("Invalid key: " + key);
                default:
                    throw new IOException("Unexpected response status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
