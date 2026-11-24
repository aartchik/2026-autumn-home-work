package company.vk.edu.distrib.compute.robert.urlshortener.utils;

import java.security.SecureRandom;

public final class IdGeneratorUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private IdGeneratorUtils() {

    }

    public static String randomID(int length) {
        char[] id = new char[length];

        for (int i = 0; i < length; ++i) {
            int idx = RANDOM.nextInt(ALPHABET.length());
            id[i] = ALPHABET.charAt(idx);
        }
        
        return new String(id);
    }    
}
