package company.vk.edu.distrib.compute.sovesti.urlshortener.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public final class HttpServers {

    public HttpServer create(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(executorFromEnv());
        return server;
    }

    private Executor executorFromEnv() {
        return Optional.ofNullable(System.getenv("SERVICE_THREADS"))
            .flatMap(this::parse)
            .map(Number::intValue)
            .map(Executors::newFixedThreadPool)
            .orElseGet(Executors::newSingleThreadExecutor);
    }

    private Optional<Number> parse(String raw) {
        return Optional.ofNullable(new DecimalFormat().parse(raw, new ParsePosition(0)));
    }
}
