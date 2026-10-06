package company.vk.edu.distrib.compute.aartchik.kv;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.kv.KVService;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class KvService implements KVService {
    private final int port;
    private boolean started;
    private boolean stopped;
    @Nullable private HttpServer server;
    @Nullable private FileDao dao;
    @Nullable private ExecutorService executor;

    KvService(int port) {
        this.port = port;
    }

    @Override
    public synchronized void start() {
        if (started || stopped) {
            throw new IllegalStateException("Service can only be started once");
        }
        started = true;
        try {
            FileDao storage = new FileDao(Path.of(
                    System.getProperty("java.io.tmpdir"), "aartchik-kv", Integer.toString(port)));
            dao = storage;
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            server = httpServer;
            if (!Boolean.getBoolean("aartchik.kv.singleThread")) {
                ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
                executor = workers;
                httpServer.setExecutor(workers);
            }
            httpServer.createContext("/", new KvHandler(storage));
            httpServer.start();
        } catch (IOException | RuntimeException failure) {
            stop();
            throw new IllegalStateException("Cannot start KV service", failure);
        }
    }

    @Override
    public synchronized void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        HttpServer httpServer = server;
        if (httpServer != null) {
            httpServer.stop(0);
        }
        ExecutorService workers = executor;
        if (workers != null) {
            workers.close();
        }
        FileDao storage = dao;
        if (storage != null) {
            storage.close();
        }
    }
}
