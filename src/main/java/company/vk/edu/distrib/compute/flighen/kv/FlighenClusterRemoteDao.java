package company.vk.edu.distrib.compute.flighen.kv;

import company.vk.edu.distrib.compute.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

public class FlighenClusterRemoteDao implements Dao<String> {
    private static final Logger log = LoggerFactory.getLogger(FlighenClusterRemoteDao.class);
    private static final String MEDIA_TYPE = "application/octet-stream";
    private final HttpClient client;
    private final Set<Integer> ports = new HashSet<>();

    public FlighenClusterRemoteDao(int... ports) throws IOException {
        if (ports.length == 0) {
            throw new IllegalArgumentException("Cluster must contain at least one node");
        }

        for (int port : ports) {
            appendNode(port);
        }

        this.client = HttpClient.newHttpClient();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        String encodedKey = URLEncoder.encode(
                key,
                StandardCharsets.UTF_8
        );

        int port = determineNodePort(key);

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

        int port = determineNodePort(key);

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

        int port = determineNodePort(key);

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

    private int determineNodePort(String key) {
        if (ports.isEmpty()) {
            throw new IllegalStateException("No nodes in cluster");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            int selectedPort = -1;
            byte[] maxHash = null;

            for (int port : ports) {
                digest.update(key.getBytes(StandardCharsets.UTF_8));
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(port).array());

                byte[] hash = digest.digest();

                if (maxHash == null || Arrays.compareUnsigned(hash, maxHash) > 0) {
                    maxHash = hash;
                    selectedPort = port;
                }
            }

            return selectedPort;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private void appendNode(int port) {
        if (!ports.add(port)) {
            log.warn("Node with this port has already been added");
        }
    }
}
