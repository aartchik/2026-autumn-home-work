package company.vk.edu.distrib.compute.robert.api.models;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.robert.urlshortener.utils.Credentials;

public class AuthHandler extends Handler {
    private static final String AUTHORIZATION = "authorization";
    private static final String BASIC = "basic";
    private static final Decoder DECODER = Base64.getDecoder();
    private static final int CREDENTIALS_PARTS_COUNT = 2;
    
    private final HttpHandler innerHandler;
    private final Dao<String> userDao;

    public AuthHandler(HttpHandler inputHandler, Dao<String> inputDao) {
        super();
        innerHandler = inputHandler;
        userDao = inputDao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            var authHeaders = exchange.getRequestHeaders();
            if (authHeaders == null) {
                sendUnauthorized(exchange);
                return;
            } 
            var auth = authHeaders.getOrDefault(AUTHORIZATION, null);
            if (auth == null) {
                sendUnauthorized(exchange);
                return;
            } 

            var list = auth.getFirst().split(" ");
            if (list.length != CREDENTIALS_PARTS_COUNT || !BASIC.equalsIgnoreCase(list[0].strip())) {
                sendUnauthorized(exchange);
                return;
            }

            var base64 = list[1];
            var str = new String(DECODER.decode(base64), StandardCharsets.UTF_8);

            Credentials creds = Credentials.from(str);
            int passwordHash = userDao.get(creds.user()).hashCode();
            if (passwordHash != creds.password().hashCode()) {
                sendUnauthorized(exchange);
                return;
            }
        } catch (IOException e) {
            Response response = Response.builder()
                .setStatus(HttpStatus.SERVICE_UNAVAILABLE.code())
                .build();
            sendResponse(exchange, response);
            return;
        } catch (Exception e) {
            Response response = Response.builder()
                .setStatus(HttpStatus.UNAUTHORIZED.code())
                .build();
            sendResponse(exchange, response);
            return;
        }

        innerHandler.handle(exchange);
    } 

    private static void sendUnauthorized(HttpExchange exchange) throws IOException {
        Response response = Response.builder()
            .setStatus(HttpStatus.UNAUTHORIZED.code())
            .build();
        sendResponse(exchange, response);
    }
}
