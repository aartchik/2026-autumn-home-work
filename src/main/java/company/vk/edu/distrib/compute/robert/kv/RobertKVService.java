package company.vk.edu.distrib.compute.robert.kv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import com.sun.net.httpserver.HttpServer;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.robert.api.v0.StatusHandler;
import company.vk.edu.distrib.compute.robert.dao.RobertByteDao;
import company.vk.edu.distrib.compute.robert.kv.api.v0.EntityHandler;
import company.vk.edu.distrib.compute.robert.kv.validation.implementations.KVValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobertKVService implements KVService {
    private static final Logger log = LoggerFactory.getLogger(RobertKVService.class);
    private static final int MIN_EXECUTOR_THREADS = 1;

    private final int port;
    private final Dao<byte[]> dao;
    private final HttpServer httpServer;
    private final ReentrantLock lock = new ReentrantLock();
    private ServiceState state = ServiceState.NEW;
    private final ExecutorService executor;

    public RobertKVService(int initPort) throws IOException {
        port = initPort;

        Path storageRoot = Path.of(
            System.getProperty("java.io.tmpdir"),
            "vkedu-kv-robert",
            Integer.toString(port)
        );
        dao = new RobertByteDao(storageRoot, "entities", new KVValidator());
        httpServer = HttpServer.create();

        String value = System.getenv().getOrDefault("KV_EXECUTOR_THREADS", "1");
        int executorThreads = Integer.parseInt(value);

        if (executorThreads < MIN_EXECUTOR_THREADS) {
            throw new IllegalArgumentException();
        }

        executor = Executors.newFixedThreadPool(executorThreads);
        httpServer.setExecutor(executor);

        log.atDebug().log("Created unbound KV HTTP server");
    }

    private void initContexts() {
        httpServer.createContext(StatusHandler.PATH, new StatusHandler());
        httpServer.createContext(EntityHandler.PATH, new EntityHandler(dao));
    }

    @Override
    public void start() {
        lock.lock();
        try {
            if (state != ServiceState.NEW) {
                throw new IllegalStateException();
            }

            state = ServiceState.STARTED;
            initContexts();
            try {
                httpServer.bind(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), port),
                    0
                );
                httpServer.start();
                log.atInfo().log("KV service started on {}", httpServer.getAddress());
            } catch (IOException e) {
                state = ServiceState.STOPPED;
                executor.shutdown();
                throw new UncheckedIOException(e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            if (state != ServiceState.STARTED) {
                throw new IllegalStateException();
            }

            httpServer.stop(1);
            executor.shutdown();
            state = ServiceState.STOPPED;
            closeDao();
            log.atInfo().log("KV service stopped");
        } finally {
            lock.unlock();
        }
    }

    private void closeDao() {
        try {
            dao.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private enum ServiceState {
        NEW,
        STARTED,
        STOPPED
    }
}
