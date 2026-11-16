package company.vk.edu.distrib.compute.miiishenka.kv.controller;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.miiishenka.http.exception.HttpStatusException;

public class EntityController extends BaseKVController {
    private final Dao<byte[]> dao;

    public EntityController(Dao<byte[]> dao) {
        super();
        this.dao = dao;
    }

    @Override
    public String getPath() {
        return "/v0/entity";
    }

    @Override
    public void get(HttpExchange exchange) throws HttpStatusException, IOException {
        String id = extractId(exchange);
        byte[] value = dao.get(id);
        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseBody().write(value);
    }

    @Override
    public void put(HttpExchange exchange) throws HttpStatusException, IOException {
        String id = extractId(exchange);
        byte[] value = exchange.getRequestBody().readAllBytes();
        dao.upsert(id, value);
        exchange.sendResponseHeaders(201, 0);
    }

    @Override
    public void delete(HttpExchange exchange) throws HttpStatusException, IOException {
        String id = extractId(exchange);
        dao.delete(id);
        exchange.sendResponseHeaders(202, 0);
    }
}
