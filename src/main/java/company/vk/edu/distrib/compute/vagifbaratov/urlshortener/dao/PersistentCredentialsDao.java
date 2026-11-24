package company.vk.edu.distrib.compute.vagifbaratov.urlshortener.dao;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static company.vk.edu.distrib.compute.vagifbaratov.urlshortener.dao.DaoUtils.*;

public class PersistentCredentialsDao implements Dao<String> {
    private final ConcurrentMap<String, String> storage = new ConcurrentHashMap<>();
    private final Path filepath;

    public PersistentCredentialsDao(Path filepath) throws IOException {
        this.filepath = filepath;

        loadStorage(filepath, storage);
    }

    @Override
    public String get(String user) throws NoSuchElementException, IllegalArgumentException, IOException {
        validateUsername(user);
        final var password = storage.get(user);

        if (password == null) {
            throw new NoSuchElementException("No password for user: " + user);
        }

        return password;
    }

    @Override
    public void upsert(String user, String password) throws IllegalArgumentException, IOException {
        validateUsername(user);

        storage.put(user, password);
    }

    @Override
    public void delete(String user) throws IllegalArgumentException, IOException {
        validateUsername(user);

        storage.remove(user);
    }

    @Override
    public void close() throws IOException {
        storeStorage(filepath, storage);
    }

    private static void validateUsername(String user) {
        if (!user.matches(USERNAME_PATTERN)) {
            throw new IllegalArgumentException("Invalid username: " + user);
        }
    }
}
