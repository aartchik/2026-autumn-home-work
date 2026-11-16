package company.vk.edu.distrib.compute.flighen.kv;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlighenKVService implements KVService {
    private static final Logger log = LoggerFactory.getLogger(FlighenKVService.class);

    private final HttpServer server;
    private final Dao<byte[]> dao;

    private final ExecutorService executor;

    public FlighenKVService(int port, int threads) throws IOException {
        Path storageDir = Path.of(
                System.getProperty("java.io.tmpdir"),
                "fl1ghen-urlshortener",
                String.valueOf(port)
        );

        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to create temp directory {}", storageDir, e);
            }
            throw e;
        }

        dao = new KeyListingBytesDao(
                storageDir.resolve("kv.db")
        );

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/v0/status", new GetStatusHandler());
        server.createContext("/v0/entity", new EntityHandler(dao));

        executor = Executors.newFixedThreadPool(threads);

        server.setExecutor(executor);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(1);
        executor.shutdown();
        try {
            dao.close();
            log.info("KVService data saved successfully");
        } catch (IOException e) {
            log.error("Failed to close bytesDao", e);
        }
    }
}
