package company.vk.edu.distrib.compute.vagifbaratov.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Objects;

public class PersistentByteArrayDao implements Dao<byte[]> {
    private final Path filepath;

    public PersistentByteArrayDao(Path directory) throws IOException {
        this.filepath = directory;
        Files.createDirectories(directory);
    }

    @Override
    public byte[] get(String key) throws IOException {
        Path file = pathFor(key);
        try {
            return Files.readAllBytes(file);
        } catch (NoSuchFileException exception) {
            throw new NoSuchElementException("Missing key: " + key, exception);
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        Path file = pathFor(key);
        Objects.requireNonNull(value, "value is null");
        Path temporary = Files.createTempFile(filepath, "upsert-", ".tmp");
        try {
            Files.write(temporary, value);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(pathFor(key));
    }

    @Override
    public void close() {
        //nothing to do.
    }

    private Path pathFor(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must be non-empty");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    key.getBytes(StandardCharsets.UTF_8)
            );

            return filepath.resolve(HexFormat.of().formatHex(hash) + ".data");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
