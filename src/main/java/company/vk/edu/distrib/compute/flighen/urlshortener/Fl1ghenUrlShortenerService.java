package company.vk.edu.distrib.compute.flighen.urlshortener;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.flighen.kv.LinksDaoListener;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fl1ghenUrlShortenerService implements UrlShortenerService {
    private static final Logger log = LoggerFactory.getLogger(Fl1ghenUrlShortenerService.class);

    private final List<LinksDaoListener> linksDaoListeners = new ArrayList<>();
    private final HttpServer server;
    private final Dao<String> usersDao;
    private Dao<String> shortLinksDao;

    private boolean started;
    private boolean stopped;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(16);

    public Fl1ghenUrlShortenerService(int port) throws IOException {
        Path storageDir = Path.of(
                System.getProperty("java.io.tmpdir"),
                "fl1ghen-urlshortener"
        );

        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to create temp directory {}", storageDir, e);
            }
            throw e;
        }

        usersDao = new PersistentDao(
                storageDir.resolve("usersDb.txt")
        );
        shortLinksDao = new PersistentDao(
                storageDir.resolve("shortLinks.txt")
        );

        RedirectHandler redirectHandler = new RedirectHandler(shortLinksDao);
        LinkHandler linkHandler = new LinkHandler(shortLinksDao, port);

        linksDaoListeners.add(redirectHandler);
        linksDaoListeners.add(linkHandler);

        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.setExecutor(executor);

        server.createContext("/v0/status", new GetStatusHandler());
        server.createContext("/", redirectHandler);
        server.createContext("/internal/users", new UsersHandler(usersDao));
        HttpContext ctxLink = server.createContext("/v0/links", linkHandler);

        AuthFilter authFilter = new AuthFilter(usersDao);
        ctxLink.getFilters().add(authFilter);
    }

    @Override
    public void start() {
        server.start();
        started = true;
    }

    @Override
    public void stop() {
        server.stop(1);

        executor.shutdown();

        try {
            shortLinksDao.close();
        } catch (IOException e) {
            log.error("Failed to close shortLinksDao", e);
        }

        try {
            usersDao.close();
        } catch (IOException e) {
            log.error("Failed to close usersDao", e);
        }

        stopped = true;
    }

    @Override
    public void setLinksDao(Dao<String> dao) {
        if (started || stopped) {
            throw new IllegalStateException();
        }

        this.shortLinksDao = Objects.requireNonNull(dao, "Links Dao must not be null");

        for (LinksDaoListener listener : linksDaoListeners) {
            listener.onLinksDaoChanged(dao);
        }
    }
}
