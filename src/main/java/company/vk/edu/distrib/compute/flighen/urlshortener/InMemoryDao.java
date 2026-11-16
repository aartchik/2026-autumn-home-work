package company.vk.edu.distrib.compute.flighen.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDao implements Dao<String> {
    private static final String KEY_MUST_NOT_BE_NULL = "Key must not be null";

    private final Map<String, String> db;

    public InMemoryDao() {
        db = new ConcurrentHashMap<>();
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(KEY_MUST_NOT_BE_NULL);
        }

        String value = db.get(key);

        if (value == null) {
            throw new NoSuchElementException("db does not contain this key: %s".formatted(key));
        }

        return value;
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(KEY_MUST_NOT_BE_NULL);
        }

        db.put(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(KEY_MUST_NOT_BE_NULL);
        }

        db.remove(key);
    }

    @Override
    public void close() throws IOException {
        db.clear();
    }
}
