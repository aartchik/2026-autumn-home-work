package company.vk.edu.distrib.compute.aartchik.kv;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class ReplicaCoordinator implements AutoCloseable {
    static final String INTERNAL_PATH = "/internal/entity";
    private static final String GET = "GET";
    private static final Duration TIMEOUT = Duration.ofMillis(700);
    private final int port;
    private final FileDao dao;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT).version(HttpClient.Version.HTTP_1_1).build();

    ReplicaCoordinator(int port, FileDao dao) {
        this.port = port;
        this.dao = dao;
    }

    KvResponse execute(String method, String key, byte[] body, int ack, ReplicaConfiguration configuration) {
        int[] replicas = configuration.replicas(key);
        BlockingQueue<KvResponse> responses = new LinkedBlockingQueue<>();
        // Dispatch to every remote replica before waiting for acknowledgements.
        for (int replica : replicas) {
            if (replica != port) {
                forward(replica, method, key, body, responses);
            }
        }
        for (int replica : replicas) {
            if (replica == port) {
                responses.add(local(method, key, body));
            }
        }
        return collect(method, ack, replicas.length, responses);
    }

    KvResponse local(String method, String key, byte[] body) {
        try {
            return switch (method) {
                case GET -> new KvResponse(HttpURLConnection.HTTP_OK, dao.get(key));
                case "PUT" -> {
                    dao.upsert(key, body);
                    yield new KvResponse(201);
                }
                case "DELETE" -> {
                    dao.delete(key);
                    yield new KvResponse(HttpURLConnection.HTTP_OK);
                }
                default -> new KvResponse(405);
            };
        } catch (NoSuchElementException missing) {
            return new KvResponse(404);
        } catch (IOException failure) {
            return new KvResponse(503);
        }
    }

    private void forward(int replica, String method, String key, byte[] body, BlockingQueue<KvResponse> responses) {
        String address = "http://localhost:" + replica + INTERNAL_PATH + "?id="
                + URLEncoder.encode(key, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(address)).timeout(TIMEOUT)
                .method(method, body.length == 0 ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body)).build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).whenComplete((response, failure) -> {
            if (failure == null && response != null) {
                responses.add(new KvResponse(response.statusCode(), response.body()));
            } else {
                responses.add(new KvResponse(503));
            }
        });
    }

    private static KvResponse collect(String method, int ack, int total, BlockingQueue<KvResponse> responses) {
        long deadline = System.nanoTime() + TIMEOUT.plusMillis(200).toNanos();
        int confirmed = 0;
        KvResponse selected = new KvResponse(GET.equals(method) ? 404 : successStatus(method));
        try {
            for (int received = 0; received < total; received++) {
                long remaining = deadline - System.nanoTime();
                KvResponse response = responses.poll(Math.max(0, remaining), TimeUnit.NANOSECONDS);
                if (response == null) {
                    return new KvResponse(503);
                }
                if (isAcknowledgement(method, response)) {
                    confirmed++;
                    selected = selectResponse(selected, response);
                }
                if (confirmed >= ack) {
                    return selected;
                }
                if (confirmed + total - received - 1 < ack) {
                    return new KvResponse(503);
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return new KvResponse(503);
    }

    private static KvResponse selectResponse(KvResponse selected, KvResponse response) {
        return response.status() == HttpURLConnection.HTTP_OK ? response : selected;
    }

    private static boolean isAcknowledgement(String method, KvResponse response) {
        return response.status() == successStatus(method) || GET.equals(method) && response.status() == 404;
    }

    private static int successStatus(String method) {
        return switch (method) {
            case GET -> HttpURLConnection.HTTP_OK;
            case "PUT" -> 201;
            case "DELETE" -> HttpURLConnection.HTTP_OK;
            default -> 405;
        };
    }

    @Override
    public void close() {
        client.shutdownNow();
    }
}
