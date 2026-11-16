package company.vk.edu.distrib.compute.miiishenka.urlshortener.controller;

import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.miiishenka.http.BaseController;
import company.vk.edu.distrib.compute.miiishenka.http.exception.UnprocessableContentException;

public abstract class BaseUrlShortenerController extends BaseController {
    protected static final int SHORT_ID_LENGTH = 10;
    private static final Pattern SHORT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{%d}$".formatted(SHORT_ID_LENGTH));

    protected String extractShortId(HttpExchange exchange) throws UnprocessableContentException {
        String uriPrefix = getPath().endsWith("/") ? getPath() : getPath() + "/";
        String uriPath = exchange.getRequestURI().getPath();
        if (!uriPath.startsWith(uriPrefix)) {
            throw new UnprocessableContentException();
        }
        String shortId = uriPath.substring(uriPrefix.length());
        if (!SHORT_ID_PATTERN.matcher(shortId).matches()) {
            throw new UnprocessableContentException();
        }
        return shortId;
    }
}
