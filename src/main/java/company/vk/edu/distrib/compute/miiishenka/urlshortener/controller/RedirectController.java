package company.vk.edu.distrib.compute.miiishenka.urlshortener.controller;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.miiishenka.http.exception.HttpStatusException;

public class RedirectController extends BaseUrlShortenerController {
    private final Dao<String> linksDao;

    public RedirectController(Dao<String> linksDao) {
        super();
        this.linksDao = linksDao;
    }

    @Override
    public String getPath() {
        return "/";
    }

    @Override
    public void get(HttpExchange exchange) throws HttpStatusException, IOException {
        String shortId = extractShortId(exchange);
        String longLink = linksDao.get(shortId);
        exchange.getResponseHeaders().add("Location", longLink);
        exchange.sendResponseHeaders(301, 0);
    }
}
