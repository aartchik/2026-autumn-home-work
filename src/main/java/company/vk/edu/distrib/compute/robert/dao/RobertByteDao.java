package company.vk.edu.distrib.compute.robert.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.robert.validation.InputValidator;

public class RobertByteDao implements Dao<byte[]> {
    private static final Base64.Encoder KEY_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final ReentrantLock lock = new ReentrantLock();
    private final Path directory;
    private final InputValidator<byte[]> inputValidator;

    public RobertByteDao(
        Path storageRoot,
        String directoryName,
        InputValidator<byte[]> initInputValidator
    ) throws IOException {
        directory = storageRoot.resolve(directoryName);
        inputValidator = initInputValidator;
        Files.createDirectories(directory);
    }

    @Override
    public byte[] get(String key) throws IOException {
        inputValidator.validateKey(key);

        lock.lock();
        try {
            Path file = keyToFile(key);
            if (!Files.exists(file)) {
                throw new NoSuchElementException();
            }
            return Files.readAllBytes(file);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        inputValidator.validateKey(key);
        inputValidator.validateValue(value);

        lock.lock();
        try {
            writeAtomically(keyToFile(key), value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IOException {
        inputValidator.validateKey(key);

        lock.lock();
        try {
            Files.deleteIfExists(keyToFile(key));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        // No closeable resources.
    }

    private Path keyToFile(String key) {
        String encodedKey = KEY_ENCODER.encodeToString(key.getBytes(StandardCharsets.UTF_8));
        return directory.resolve(encodedKey + ".bin");
    }

    private void writeAtomically(Path target, byte[] value) throws IOException {
        Path temporary = Files.createTempFile(directory, ".entity-", ".tmp");
        try {
            Files.write(temporary, value);
            moveReplacing(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
