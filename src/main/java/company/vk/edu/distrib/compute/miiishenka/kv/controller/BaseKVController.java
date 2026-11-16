package company.vk.edu.distrib.compute.miiishenka.kv.controller;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.miiishenka.http.BaseController;
import company.vk.edu.distrib.compute.miiishenka.http.exception.BadRequestException;
import company.vk.edu.distrib.compute.miiishenka.http.exception.NotFoundException;

public abstract class BaseKVController extends BaseController {
    private static final String ID_PREFIX = "id=";

    protected String extractId(HttpExchange exchange) throws BadRequestException, NotFoundException {
        validateExactPath(exchange);
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || !rawQuery.startsWith(ID_PREFIX)) {
            throw new BadRequestException();
        }
        String id = rawQuery.substring(ID_PREFIX.length());
        if (id.isEmpty()) {
            throw new BadRequestException();
        }

        return id;
    }
}
