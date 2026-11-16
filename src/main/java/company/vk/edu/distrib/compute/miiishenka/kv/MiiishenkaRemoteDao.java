package company.vk.edu.distrib.compute.miiishenka.kv;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;

public class MiiishenkaRemoteDao implements Dao<String> {
    private final int port;
    private final HttpClient httpClient;

    public MiiishenkaRemoteDao(int port) {
        this.port = port;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        HttpRequest request = HttpRequest.newBuilder(getUri(key)).GET().build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            return response.body();
        }

        if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new NoSuchElementException();
        }

        throw new IOException("Unexpected error with code: " + response.statusCode());
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        HttpRequest request = HttpRequest.newBuilder(getUri(key))
                .PUT(HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == HttpURLConnection.HTTP_CREATED) {
            return;
        }

        if (response.statusCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new IllegalArgumentException();
        }

        throw new IOException("Unexpected error with code: " + response.statusCode());
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        HttpRequest request = HttpRequest.newBuilder(getUri(key)).DELETE().build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == HttpURLConnection.HTTP_ACCEPTED) {
            return;
        }

        if (response.statusCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new IllegalArgumentException();
        }

        throw new IOException("Unexpected error with code: " + response.statusCode());
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    private URI getUri(String key) {
        return URI.create("http://localhost:%d/v0/entity?id=%s".formatted(port, key));
    }

    private HttpResponse<String> send(HttpRequest httpRequest) throws IOException {
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request was interrupted", e);
        }
    }
}
