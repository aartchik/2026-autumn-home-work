package company.vk.edu.distrib.compute.aartchik.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class HttpDao implements Dao<String> {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private static final int NOT_FOUND = 404;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final int[] ports;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private boolean closed;

    HttpDao(int... ports) {
        if (ports.length == 0) {
            throw new IllegalArgumentException("At least one KV port is required");
        }
        this.ports = ports.clone();
        Arrays.sort(this.ports);
        for (int index = 0; index < this.ports.length; index++) {
            int port = this.ports[index];
            if (port <= 0 || port > 65535 || index > 0 && port == this.ports[index - 1]) {
                throw new IllegalArgumentException("KV ports must be valid and distinct");
            }
        }
    }

    @Override
    public String get(String key) throws IOException {
        lock.readLock().lock();
        try {
            HttpResponse<byte[]> response = send(request(key).GET().build());
            if (response.statusCode() == NOT_FOUND) {
                throw new NoSuchElementException("Key not found");
            }
            checkStatus(response, 200);
            return new String(response.body(), StandardCharsets.UTF_8);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        lock.readLock().lock();
        try {
            HttpRequest request = request(key)
                    .PUT(HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8))
                    .build();
            checkStatus(send(request), 201);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void delete(String key) throws IOException {
        lock.readLock().lock();
        try {
            checkStatus(send(request(key).DELETE().build()), 202);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (!closed) {
                closed = true;
                client.close();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private HttpRequest.Builder request(String key) throws IOException {
        if (closed) {
            throw new IOException("DAO is closed");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        String endpoint = "http://localhost:" + owner(key) + "/v0/entity?id=";
        return HttpRequest.newBuilder(URI.create(endpoint + URLEncoder.encode(key, StandardCharsets.UTF_8)))
                .timeout(TIMEOUT);
    }

    // Rendezvous hashing: the highest unsigned key/node score owns the key.
    // Sorted node identities also make score ties independent of configuration order.
    private int owner(String key) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is required by the JDK", unavailable);
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        int selected = ports[0];
        long highest = 0;
        boolean first = true;
        for (int port : ports) {
            digest.update(keyBytes);
            byte[] hash = digest.digest(ByteBuffer.allocate(Integer.BYTES).putInt(port).array());
            long score = ByteBuffer.wrap(hash).getLong();
            if (first || Long.compareUnsigned(score, highest) > 0) {
                selected = port;
                highest = score;
                first = false;
            }
        }
        return selected;
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
