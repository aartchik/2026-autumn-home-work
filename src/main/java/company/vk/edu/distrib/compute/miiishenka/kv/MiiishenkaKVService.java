package company.vk.edu.distrib.compute.miiishenka.kv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.miiishenka.dao.PersistentDao;
import company.vk.edu.distrib.compute.miiishenka.http.ControllerHttpHandler;
import company.vk.edu.distrib.compute.miiishenka.kv.controller.BaseKVController;
import company.vk.edu.distrib.compute.miiishenka.kv.controller.EntityController;
import company.vk.edu.distrib.compute.miiishenka.kv.controller.StatusController;

public class MiiishenkaKVService implements KVService {
    private static final String WORKERS_ENV = "KV_WORKERS";
    private final HttpServer server;
    private final Dao<byte[]> entityDao;
    private final ExecutorService executor;
    private final AtomicBoolean ready = new AtomicBoolean();

    public MiiishenkaKVService(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        String workersValue = System.getenv(WORKERS_ENV);
        if (workersValue == null || workersValue.isBlank()) {
            executor = null;
        } else {
            int workers = Integer.parseInt(workersValue);
            if (workers <= 0) {
                throw new IllegalArgumentException("KV_WORKERS must be positive");
            }

            executor = Executors.newFixedThreadPool(workers);
            server.setExecutor(executor);
        }

        entityDao = PersistentDao.byteArrayDao("/tmp/miiishenka-entities");
        List<BaseKVController> controllers = List.of(
            new StatusController(ready::get),
            new EntityController(entityDao)
        );
        controllers.forEach(
                controller -> server.createContext(controller.getPath(), new ControllerHttpHandler(controller))
        );
    }

    @Override
    public void start() {
        server.start();
        ready.set(true);
    }

    @Override
    public void stop() {
        ready.set(false);
        try (entityDao) {
            server.stop(1);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist DAO data", e);
        } finally {
            if (executor != null) {
                executor.shutdown();
            }
        }
    }
}
