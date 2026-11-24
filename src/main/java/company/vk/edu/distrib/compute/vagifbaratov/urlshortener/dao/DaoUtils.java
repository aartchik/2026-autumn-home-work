package company.vk.edu.distrib.compute.vagifbaratov.urlshortener.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

public final class DaoUtils {
    public static final int SHORTLINK_LENGTH = 10;
    public static final String USERNAME_PATTERN = "^[A-Za-z][A-Za-z0-9._-]*$";

    private DaoUtils() {
    }

    public static void loadStorage(
            Path filepath,
            ConcurrentMap<String, String> storage
    ) throws IOException {
        if (!Files.exists(filepath)) {
            return;
        }

        final var properties = new Properties();

        try (var reader = Files.newBufferedReader(filepath)) {
            properties.load(reader);
        }

        for (String key : properties.stringPropertyNames()) {
            storage.put(key, properties.getProperty(key));
        }
    }

    public static void storeStorage(
            Path filepath,
            ConcurrentMap<String, String> storage
    ) throws IOException {
        Properties properties = new Properties();

        properties.putAll(storage);

        Files.createDirectories(filepath.toAbsolutePath().getParent());
        try (var writer = Files.newBufferedWriter(filepath)) {
            properties.store(writer, "Links storage");
        }
    }
}
