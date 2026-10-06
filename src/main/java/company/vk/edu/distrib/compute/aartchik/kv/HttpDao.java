package company.vk.edu.distrib.compute.aartchik.kv;

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
import java.util.concurrent.locks.ReentrantLock;

final class HttpDao implements Dao<String> {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private static final int NOT_FOUND = 404;

    private final ReentrantLock lock = new ReentrantLock();
    private final String endpoint;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private boolean closed;

    HttpDao(int port) {
        endpoint = "http://localhost:" + port + "/v0/entity?id=";
    }

    @Override
    public String get(String key) throws IOException {
        lock.lock();
        try {
            HttpResponse<byte[]> response = send(request(key).GET().build());
            if (response.statusCode() == NOT_FOUND) {
                throw new NoSuchElementException("Key not found");
            }
            checkStatus(response, 200);
            return new String(response.body(), StandardCharsets.UTF_8);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        lock.lock();
        try {
            HttpRequest request = request(key)
                    .PUT(HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8))
                    .build();
            checkStatus(send(request), 201);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IOException {
        lock.lock();
        try {
            checkStatus(send(request(key).DELETE().build()), 202);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            if (!closed) {
                closed = true;
                client.close();
            }
        } finally {
            lock.unlock();
        }
    }

    private HttpRequest.Builder request(String key) throws IOException {
        if (closed) {
            throw new IOException("DAO is closed");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        return HttpRequest.newBuilder(URI.create(endpoint + URLEncoder.encode(key, StandardCharsets.UTF_8)))
                .timeout(TIMEOUT);
    }

    private HttpResponse<byte[]> send(HttpRequest request) throws IOException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("KV request interrupted", interrupted);
        }
    }

    private static void checkStatus(HttpResponse<?> response, int expected) throws IOException {
        if (response.statusCode() != expected) {
            throw new IOException("Unexpected KV response: " + response.statusCode());
        }
    }
}
