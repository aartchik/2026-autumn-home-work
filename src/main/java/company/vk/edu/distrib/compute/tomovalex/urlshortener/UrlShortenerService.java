package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;

public class UrlShortenerService implements company.vk.edu.distrib.compute.urlshortener.UrlShortenerService {
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final String STATUS_PATH = "/v0/status";

    private final int port;
    private final HttpServer server;
    private Dao<String> linksDao;
    private final BasicAuthentication authentication;
    private boolean started;
    private boolean stopped;

    public UrlShortenerService(int port, Dao<String> linksDao, Dao<String> usersDao) throws IOException {
        this.port = port;
        this.linksDao = linksDao;
        this.authentication = new BasicAuthentication(usersDao);
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(STATUS_PATH, this::handleStatus);
        server.createContext(LinkUtils.LINKS_PATH, this::handleLinks);
        server.createContext(UserRegistrationHandler.PATH, new UserRegistrationHandler(usersDao));
        server.createContext("/", this::handleRedirect);
    }

    @Override
    public void start() {
        server.start();
        started = true;
    }

    @Override
    public void stop() {
        stopped = true;
        server.stop(1);
        try {
            linksDao.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void setLinksDao(Dao<String> dao) {
        if (started || stopped) {
            throw new IllegalStateException();
        }
        linksDao = Objects.requireNonNull(dao);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!STATUS_PATH.equals(exchange.getRequestURI().getPath())) {
            sendEmptyResponse(exchange, 404);
            return;
        }
        if (!GET.equals(exchange.getRequestMethod())) {
            sendEmptyResponse(exchange, 405);
            return;
        }

        sendEmptyResponse(exchange, 200);
    }

    private void handleLinks(HttpExchange exchange) throws IOException {
        if (!authentication.authenticate(exchange)) {
            return;
        }

        try {
            switch (exchange.getRequestMethod()) {
                case GET -> handleGetLink(exchange);
                case POST -> handleCreateLink(exchange);
                case PUT -> handleUpdateLink(exchange);
                case DELETE -> handleDeleteLink(exchange);
                default -> sendEmptyResponse(exchange, 404);
            }
        } catch (BufferOverflowException e) {
            sendEmptyResponse(exchange, 413);
        }
    }

    private void handleGetLink(HttpExchange exchange) throws IOException {
        String id = getValidLinkId(exchange);
        if (id == null) {
            return;
        }

        try {
            sendResponse(exchange, 200, linksDao.get(id));
        } catch (NoSuchElementException e) {
            sendEmptyResponse(exchange, 404);
        }
    }

    private void handleCreateLink(HttpExchange exchange) throws IOException {
        if (!LinkUtils.LINKS_PATH.equals(exchange.getRequestURI().getPath())) {
            sendEmptyResponse(exchange, 404);
            return;
        }

        String url = RequestBodyUtils.read(exchange.getRequestBody());
        if (!LinkUtils.isValidUrl(url)) {
            sendEmptyResponse(exchange, 422);
            return;
        }

        String id = generateId();
        linksDao.upsert(id, url);
        sendResponse(exchange, 201, LinkUtils.createShortLink(port, id));
    }

    private void handleUpdateLink(HttpExchange exchange) throws IOException {
        String id = getValidLinkId(exchange);
        if (id == null) {
            return;
        }

        String url = RequestBodyUtils.read(exchange.getRequestBody());
        if (!LinkUtils.isValidUrl(url)) {
            sendEmptyResponse(exchange, 422);
            return;
        }

        try {
            linksDao.get(id);
            linksDao.upsert(id, url);
            sendEmptyResponse(exchange, 200);
        } catch (NoSuchElementException e) {
            sendEmptyResponse(exchange, 404);
        }
    }

    private void handleDeleteLink(HttpExchange exchange) throws IOException {
        String id = getValidLinkId(exchange);
        if (id == null) {
            return;
        }

        linksDao.delete(id);
        sendEmptyResponse(exchange, 202);
    }

    private void handleRedirect(HttpExchange exchange) throws IOException {
        if (!GET.equals(exchange.getRequestMethod())) {
            if (!authentication.authenticate(exchange)) {
                return;
            }
            sendEmptyResponse(exchange, 405);
            return;
        }

        String id = exchange.getRequestURI().getPath().substring(1);
        if (!LinkUtils.isValidId(id)) {
            sendEmptyResponse(exchange, 422);
            return;
        }

        try {
            String url = linksDao.get(id);
            exchange.getResponseHeaders().set("Location", url);
            sendEmptyResponse(exchange, 301);
        } catch (NoSuchElementException e) {
            sendEmptyResponse(exchange, 404);
        }
    }

    private static String getValidLinkId(HttpExchange exchange) throws IOException {
        String id = LinkUtils.getIdFromLinksPath(exchange.getRequestURI().getPath());
        if (id == null) {
            sendEmptyResponse(exchange, 404);
            return null;
        }
        if (!LinkUtils.isValidId(id)) {
            sendEmptyResponse(exchange, 422);
            return null;
        }
        return id;
    }

    private String generateId() throws IOException {
        String generatedId = LinkUtils.createRandomId();
        while (linkExists(generatedId)) {
            generatedId = LinkUtils.createRandomId();
        }
        return generatedId;
    }

    private boolean linkExists(String id) throws IOException {
        try {
            linksDao.get(id);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private static void sendResponse(HttpExchange exchange, int code,String body) throws IOException {
        final var response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(code, response.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(response);
        }
    }

    private static void sendEmptyResponse(HttpExchange exchange,int code) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(code, -1);
        exchange.close();
    }

}
