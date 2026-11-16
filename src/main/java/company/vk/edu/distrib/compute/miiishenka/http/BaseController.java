package company.vk.edu.distrib.compute.miiishenka.http;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.miiishenka.http.exception.HttpStatusException;
import company.vk.edu.distrib.compute.miiishenka.http.exception.MethodNotAllowedException;
import company.vk.edu.distrib.compute.miiishenka.http.exception.NotFoundException;

public abstract class BaseController {
    public abstract String getPath();

    public void get(HttpExchange exchange) throws HttpStatusException, IOException {
        throw new MethodNotAllowedException();
    }

    public void post(HttpExchange exchange) throws HttpStatusException, IOException {
        throw new MethodNotAllowedException();
    }

    public void put(HttpExchange exchange) throws HttpStatusException, IOException {
        throw new MethodNotAllowedException();
    }

    public void delete(HttpExchange exchange) throws HttpStatusException, IOException {
        throw new MethodNotAllowedException();
    }

    protected void validateExactPath(HttpExchange exchange) throws NotFoundException {
        if (!getPath().equals(exchange.getRequestURI().getPath())) {
            throw new NotFoundException();
        }
    }
}
