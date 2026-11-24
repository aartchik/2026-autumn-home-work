package company.vk.edu.distrib.compute.vagifbaratov.urlshortener;

import java.security.SecureRandom;

public final class ShortUrlGeneratorUtils {
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LENGTH = 10;

    private ShortUrlGeneratorUtils() {
    }

    public static String generate() {
        StringBuilder result = new StringBuilder(LENGTH);

        for (int i = 0; i < LENGTH; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            result.append(ALPHABET.charAt(index));
        }

        return result.toString();
    }
}
