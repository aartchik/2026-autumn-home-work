package company.vk.edu.distrib.compute.miiishenka.urlshortener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.miiishenka.http.ControllerHttpHandler;
import company.vk.edu.distrib.compute.miiishenka.urlshortener.authorization.BasicAuthenticator;
import company.vk.edu.distrib.compute.miiishenka.urlshortener.controller.BaseUrlShortenerController;
import company.vk.edu.distrib.compute.miiishenka.urlshortener.controller.LinksController;
import company.vk.edu.distrib.compute.miiishenka.urlshortener.controller.RedirectController;
import company.vk.edu.distrib.compute.miiishenka.urlshortener.controller.StatusController;
import company.vk.edu.distrib.compute.miiishenka.urlshortener.controller.UserController;
import company.vk.edu.distrib.compute.miiishenka.dao.PersistentDao;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

public class MiiishenkaUrlShortenerService implements UrlShortenerService {
    private final int port;
    private final HttpServer server;
    private Dao<String> linksDao;
    private Dao<String> usersDao;
    private final AtomicBoolean ready = new AtomicBoolean();
    private State state = State.NEW;

    private enum State {
        NEW,
        STARTED,
        STOPPED
    }

    public MiiishenkaUrlShortenerService(int port) throws IOException {
        this.port = port;
        server = HttpServer.create(new InetSocketAddress(port), 0);
    }

    @Override
    public void start() {
        if (state != State.NEW) {
            throw new IllegalStateException();
        }
        try {
            if (linksDao == null) {
                linksDao = PersistentDao.stringDao("/tmp/miiishenka-links");
            }
            usersDao = PersistentDao.stringDao("/tmp/miiishenka-users");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        BasicAuthenticator basicAuthenticator = new BasicAuthenticator(usersDao);
        List<BaseUrlShortenerController> controllers = List.of(
                new StatusController(ready::get),
                new LinksController(linksDao, basicAuthenticator, port),
                new UserController(usersDao),
                new RedirectController(linksDao)
        );
        controllers.forEach(
                controller -> server.createContext(controller.getPath(), new ControllerHttpHandler(controller))
        );
        server.start();
        state = State.STARTED;
        ready.set(true);
    }

    @Override
    public void stop() {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }

        Dao<String> currentLinkDao = linksDao;
        Dao<String> currentUserDao = usersDao;
        try (currentUserDao; currentLinkDao) {
            ready.set(false);
            state = State.STOPPED;
            server.stop(1);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist DAO data", e);
        }
    }

    @Override
    public void setLinksDao(Dao<String> dao) {
        if (state != State.NEW) {
            throw new IllegalStateException();
        }
        this.linksDao = dao;
    }
}
