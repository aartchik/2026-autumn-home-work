package company.vk.edu.distrib.compute.aartchik.moderation;

import company.vk.edu.distrib.compute.aartchik.urlshortener.UrlShortenerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class KafkaUnavailableTest {
    @TempDir
    Path directory;

    @Test
    void unavailableKafkaDoesNotBlockHttpOrShutdown() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        UrlShortenerServiceImpl service = new UrlShortenerServiceImpl(port, directory);
        service.setKafkaBootstrapServers("127.0.0.1:1");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            service.start();
            try (HttpClient client = HttpClient.newHttpClient()) {
                assertEquals(200, client.send(HttpRequest.newBuilder(
                                URI.create("http://localhost:" + port + "/internal/users"))
                        .POST(HttpRequest.BodyPublishers.ofString("user:pass")).build(),
                        HttpResponse.BodyHandlers.discarding()).statusCode());
                for (int index = 0; index < 20; index++) {
                    var response = client.send(HttpRequest.newBuilder(
                                    URI.create("http://localhost:" + port + "/v0/links"))
                            .header("Authorization", "Basic dXNlcjpwYXNz")
                            .POST(HttpRequest.BodyPublishers.ofString("https://example.com/offense"))
                            .build(), HttpResponse.BodyHandlers.ofString());
                    assertEquals(201, response.statusCode());
                    assertEquals(301, client.send(HttpRequest.newBuilder(URI.create(response.body())).build(),
                            HttpResponse.BodyHandlers.discarding()).statusCode());
                }
            } finally {
                service.stop();
            }
        });
        assertThrows(IllegalStateException.class, () -> service.setKafkaBootstrapServers("localhost:9092"));
        service.stop();
    }
}
