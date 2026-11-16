package company.vk.edu.distrib.compute.miiishenka.urlshortener.controller;

import java.io.IOException;
import java.util.function.BooleanSupplier;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.miiishenka.http.exception.HttpStatusException;

public class StatusController extends BaseUrlShortenerController {
    private final BooleanSupplier isReady;

    public StatusController(BooleanSupplier isReady) {
        super();
        this.isReady = isReady;
    }

    @Override
    public String getPath() {
        return "/v0/status";
    }

    @Override
    public void get(HttpExchange exchange) throws HttpStatusException, IOException {
        validateExactPath(exchange);
        exchange.sendResponseHeaders(isReady.getAsBoolean() ? 200 : 503, 0);
    }
}
