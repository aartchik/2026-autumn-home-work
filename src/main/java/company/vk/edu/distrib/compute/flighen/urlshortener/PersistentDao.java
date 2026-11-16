package company.vk.edu.distrib.compute.flighen.urlshortener;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("PMD.UseConcurrentHashMap") // All map access is guarded by the read/write lock.
public class PersistentDao implements Dao<String> {
    private static final int KEY_VALUE_PARTS = 2;
    private static final String KEY_MUST_NOT_BE_NULL = "Key must not be null";

    private final Path filePath;

    private final Map<String, String> db;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public PersistentDao(Path filePath) throws IOException {
        this.filePath = filePath;
        db = new HashMap<>();

        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }

        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

        for (String line : lines) {
            String[] parts = line.split(",", 2);
            if (parts.length == KEY_VALUE_PARTS) {
                db.put(parts[0], parts[1]);
            }
        }
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(KEY_MUST_NOT_BE_NULL);
        }

        readLock.lock();
        try {
            String value = db.get(key);

            if (value == null) {
                throw new NoSuchElementException("db does not contain this key: %s".formatted(key));
            }

            return value;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(KEY_MUST_NOT_BE_NULL);
        }

        writeLock.lock();
        try {
            Map<String, String> updatedDb = new HashMap<>(db);
            updatedDb.put(key, value);
            writeFile(updatedDb);

            db.clear();
            db.putAll(updatedDb);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(KEY_MUST_NOT_BE_NULL);
        }

        writeLock.lock();
        try {
            Map<String, String> updatedDb = new HashMap<>(db);
            updatedDb.remove(key);
            writeFile(updatedDb);

            db.clear();
            db.putAll(updatedDb);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() {
        // Changes are persisted during upsert and delete.
    }

    private void writeFile(Map<String, String> updatedDb) throws IOException {
        Path tempFile = Files.createTempFile(
                filePath.toAbsolutePath().getParent(),
                filePath.getFileName().toString(),
                ".tmp"
        );

        try {
            List<String> lines = new ArrayList<>();

            for (Map.Entry<String, String> entry : updatedDb.entrySet()) {
                lines.add(entry.getKey() + "," + entry.getValue());
            }

            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            Files.move(
                    tempFile,
                    filePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            throw e;
        }
    }
}
