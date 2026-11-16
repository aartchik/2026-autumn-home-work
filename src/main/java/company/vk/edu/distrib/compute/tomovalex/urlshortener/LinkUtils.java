package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import java.net.URI;
import java.util.Random;

public final class LinkUtils {
    public static final String LINKS_PATH = "/v0/links";

    private static final String ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ID_LEN = 10;
    private static final Random RND = new Random();

    private LinkUtils() {
    }

    public static boolean isValidId(String id) {
        return id.matches("[A-Za-z0-9]{10}");
    }

    public static String getIdFromLinksPath(String path) {
        String pathWithId = LINKS_PATH + "/";
        if (!path.startsWith(pathWithId)) {
            return null;
        }
        return path.substring(pathWithId.length());
    }

    public static String createShortLink(int port, String id) {
        return "http://localhost:%d/%s".formatted(port, id);
    }

    public static boolean isValidUrl(String url) {
        try {
            var uri = URI.create(url);
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String createRandomId() {
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < ID_LEN; i++) {
            int index = RND.nextInt(ID_CHARS.length());
            id.append(ID_CHARS.charAt(index));
        }
        return id.toString();
    }
}
