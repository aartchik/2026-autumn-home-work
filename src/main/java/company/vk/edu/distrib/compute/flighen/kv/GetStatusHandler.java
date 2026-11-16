package company.vk.edu.distrib.compute.flighen.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static company.vk.edu.distrib.compute.flighen.kv.HttpStatusCodes.OK;
import static company.vk.edu.distrib.compute.flighen.kv.HttpStatusCodes.SERVICE_UNAVAILABLE;

public class GetStatusHandler implements HttpHandler {

    public static final String UP = "UP";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final var method = exchange.getRequestMethod();

        if (Objects.equals(method, "GET")) {
            exchange.sendResponseHeaders(OK, UP.length());
            exchange.getResponseBody().write(UP.getBytes(StandardCharsets.UTF_8));
        } else {
            exchange.sendResponseHeaders(SERVICE_UNAVAILABLE, 0);
        }

        exchange.close();
    }

}
