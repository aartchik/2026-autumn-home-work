package company.vk.edu.distrib.compute.vagifbaratov.urlshortener.dao;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static company.vk.edu.distrib.compute.vagifbaratov.urlshortener.dao.DaoUtils.loadStorage;
import static company.vk.edu.distrib.compute.vagifbaratov.urlshortener.dao.DaoUtils.storeStorage;
import static company.vk.edu.distrib.compute.vagifbaratov.urlshortener.dao.DaoUtils.SHORTLINK_LENGTH;

public class PersistentLinksDao implements Dao<String> {
    private final ConcurrentMap<String, String> storage = new ConcurrentHashMap<>();
    private final Path filepath;

    public PersistentLinksDao(Path filepath) throws IOException {
        this.filepath = filepath;

        loadStorage(filepath, storage);
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        validateKey(key);
        final var value = storage.get(key);
        if (value == null) {
            throw new NoSuchElementException("no value for key: " + key);
        }
        return value;
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        validateKey(key);
        storage.put(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        validateKey(key);
        storage.remove(key);
    }

    @Override
    public void close() throws IOException {
        storeStorage(filepath, storage);
    }

    private static void validateKey(String key) {
        if (key.length() != SHORTLINK_LENGTH) {
            throw new IllegalArgumentException("invalid key: " + key);
        }
    }
}
