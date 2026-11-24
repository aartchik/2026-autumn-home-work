package company.vk.edu.distrib.compute.robert.urlshortener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import com.sun.net.httpserver.HttpServer;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.robert.api.models.AuthHandler;
import company.vk.edu.distrib.compute.robert.api.v0.StatusHandler;
import company.vk.edu.distrib.compute.robert.dao.RobertDao;
import company.vk.edu.distrib.compute.robert.urlshortener.api.v0.ForwardHandler;
import company.vk.edu.distrib.compute.robert.urlshortener.api.v0.InternalUserHandler;
import company.vk.edu.distrib.compute.robert.urlshortener.api.v0.LinksForwardHandler;
import company.vk.edu.distrib.compute.robert.urlshortener.api.v0.LinksHandler;
import company.vk.edu.distrib.compute.robert.urlshortener.validation.implementations.UrlValidator;
import company.vk.edu.distrib.compute.robert.urlshortener.validation.implementations.UserValidator;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobertUrlShortenerService implements UrlShortenerService {
    private static final Logger log = LoggerFactory.getLogger(RobertUrlShortenerService.class);

    private final int port;
    private final RobertDao usersDao;
    private final HttpServer httpServer;
    private final ReentrantLock lock = new ReentrantLock();

    private Dao<String> linksDao;
    private ServiceState state = ServiceState.NEW;

    public RobertUrlShortenerService(int initPort) throws IOException {
        port = initPort;

        Path storageRoot = Path.of(
            System.getProperty("java.io.tmpdir"),
        "vkedu-urlshortener-robert"
        );

        UrlValidator urlValidator = new UrlValidator();
        UserValidator userValidator = new UserValidator();
        linksDao = new RobertDao(storageRoot, "links", urlValidator);
        usersDao = new RobertDao(storageRoot, "users", userValidator);

        httpServer = HttpServer.create();
        log.atDebug().log("Created unbound HTTP server");
    }

    private void initContexts() {
        httpServer.createContext(ForwardHandler.PATH, new ForwardHandler(linksDao));
        httpServer.createContext(LinksHandler.PATH, new AuthHandler(new LinksHandler(linksDao, port), usersDao));
        httpServer.createContext(StatusHandler.PATH, new StatusHandler());
        httpServer.createContext(InternalUserHandler.PATH, new InternalUserHandler(usersDao));
        httpServer.createContext(
            LinksForwardHandler.PATH, 
            new AuthHandler(
                new LinksForwardHandler(linksDao),
                usersDao
            )
        );
    }

    @Override
    public void setLinksDao(Dao<String> dao) {
        lock.lock();
        try {
            if (state != ServiceState.NEW) {
                throw new IllegalStateException();
            }
            linksDao = Objects.requireNonNull(dao);
        } finally {
            lock.unlock();
        }
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
                    new InetSocketAddress(
                        InetAddress.getLoopbackAddress(),
                        port
                    ),
                    0
                );
                httpServer.start();
                log.atInfo().log("Service started on {}", httpServer.getAddress());
            } catch (IOException e) {
                state = ServiceState.STOPPED;
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
            state = ServiceState.STOPPED;
            closeDaos();
            log.atInfo().log("Service stopped");
        } finally {
            lock.unlock();
        }
    }

    private void closeDaos() {
        try {
            linksDao.close();
            usersDao.close();
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
