package company.vk.edu.distrib.compute.robert.api.models;

import java.io.IOException;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.HttpExchange;

public abstract class AbstractHandler extends Handler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Response response;
        try {
            response = handleRequest(Request.from(exchange));
        } catch (BadRequestException e) {
            response = Response.builder()
                .setStatus(HttpStatus.BAD_REQUEST.code())
                .build();
        } catch (IllegalArgumentException e) {
            response = Response.builder()
                .setStatus(HttpStatus.UNPROCESSABLE_CONTENT.code())
                .build();
        } catch (NoSuchElementException e) {
            response = Response.builder()
                .setStatus(HttpStatus.NOT_FOUND.code())
                .build();
        } catch (IOException e) {
            response = Response.builder()
                .setStatus(HttpStatus.SERVICE_UNAVAILABLE.code())
                .build();
        }

        sendResponse(exchange, response);
    }

    private Response handleRequest(Request request) throws IOException {
        return switch (request.method()) {
            case "GET" -> get(request);
            case "POST" -> post(request);
            case "PUT" -> put(request);
            case "DELETE" -> delete(request);
            default -> Response.builder()
                .setStatus(HttpStatus.METHOD_NOT_ALLOWED.code())
                .build();
        };
    }

    public Response get(Request request) throws IOException {
        return Response.builder()
            .setStatus(HttpStatus.METHOD_NOT_ALLOWED.code())
            .build();
    }

    public Response post(Request request) throws IOException {
            return Response.builder()
        .setStatus(HttpStatus.METHOD_NOT_ALLOWED.code())
        .build();
    }

    public Response put(Request request) throws IOException {
            return Response.builder()
        .setStatus(HttpStatus.METHOD_NOT_ALLOWED.code())
        .build();
    }

    public Response delete(Request request) throws IOException {
            return Response.builder()
        .setStatus(HttpStatus.METHOD_NOT_ALLOWED.code())
        .build();
    }
}
