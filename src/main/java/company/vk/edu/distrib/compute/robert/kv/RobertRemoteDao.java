package company.vk.edu.distrib.compute.robert.kv;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.robert.api.models.HttpStatus;

public class RobertRemoteDao implements Dao<String> {
    private final HttpClient client;
    private final String baseUrl;

    public RobertRemoteDao(int port) {
        this("http://localhost", port);
    }

    public RobertRemoteDao(String inputBaseUrl, int port) {
        baseUrl = inputBaseUrl + ":" + port;
        client = HttpClient.newHttpClient();
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        HttpRequest request = HttpRequest.newBuilder(makeUri(key)).GET().build();
        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        if (response.statusCode() == HttpStatus.OK.code()) {
            return new String(response.body(), StandardCharsets.UTF_8);
        } else if (response.statusCode() == HttpStatus.NOT_FOUND.code()) {
            throw new NoSuchElementException();
        } else {
            throw new IOException();
        }
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(value, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(makeUri(key)).PUT(bodyPublisher).build();
        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        if (response.statusCode() != HttpStatus.CREATED.code()) {
            throw new IOException();
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        HttpRequest request = HttpRequest.newBuilder(makeUri(key)).DELETE().build();
        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        if (response.statusCode() != HttpStatus.ACCEPTED.code()) {
            throw new IOException();
        }
    }

    private URI makeUri(String key) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        return URI.create(baseUrl + "/v0/entity?id=" + encodedKey);
    }
}
