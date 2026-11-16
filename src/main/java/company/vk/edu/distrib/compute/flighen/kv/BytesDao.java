package company.vk.edu.distrib.compute.flighen.kv;

import company.vk.edu.distrib.compute.Dao;
import org.jspecify.annotations.NonNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BytesDao implements Dao<byte[]> {
    private final Path filePath;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public BytesDao(Path filePath) throws IOException {
        this.filePath = filePath;

        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        if (key == null || key.isBlank()) {
            throw new EmptyKeyException();
        }

        readLock.lock();
        try {
            byte[] value = readFile().get(key);

            if (value == null) {
                throw new NoSuchElementException("db does not contain this key: %s".formatted(key));
            }

            return value;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void upsert(String key, byte @NonNull [] value) throws IllegalArgumentException, IOException {
        if (key == null || key.isBlank()) {
            throw new EmptyKeyException();
        }

        writeLock.lock();
        try {
            Map<String, byte[]> db = readFile();
            db.put(key, value);
            writeFile(db);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        if (key == null || key.isBlank()) {
            throw new EmptyKeyException();
        }

        writeLock.lock();
        try {
            Map<String, byte[]> db = readFile();
            db.remove(key);
            writeFile(db);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() {
        // Changes are persisted during upsert and delete.
    }

    private void writeFile(Map<String, byte[]> db) throws IOException {
        Path tempFile = Files.createTempFile(
                filePath.toAbsolutePath().getParent(),
                filePath.getFileName().toString(),
                ".tmp"
        );

        try {
            writeData(tempFile, db);
            Files.move(
                    tempFile,
                    filePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void writeData(Path path, Map<String, byte[]> db) throws IOException {
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(path))) {
            for (Map.Entry<String, byte[]> entry : db.entrySet()) {
                byte[] keyBytes = entry.getKey()
                        .getBytes(StandardCharsets.UTF_8);

                byte[] value = entry.getValue();

                out.writeInt(keyBytes.length);
                out.write(keyBytes);

                out.writeInt(value.length);
                out.write(value);
            }
        }
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap") // The map is local and never shared between threads.
    private Map<String, byte[]> readFile() throws IOException {
        Map<String, byte[]> db = new HashMap<>();
        byte[] fileContents = Files.readAllBytes(filePath);
        ByteBuffer buffer = ByteBuffer.wrap(fileContents);

        while (buffer.hasRemaining()) {
            byte[] keyBytes = readBytes(buffer);
            byte[] value = readBytes(buffer);
            String key = decodeKey(keyBytes);

            db.put(key, value);
        }

        return db;
    }

    private byte[] readBytes(ByteBuffer buffer) throws IOException {
        if (buffer.remaining() < Integer.BYTES) {
            throw corruptedStorageException();
        }

        int length = buffer.getInt();
        if (length < 0 || buffer.remaining() < length) {
            throw corruptedStorageException();
        }

        byte[] value = new byte[length];
        buffer.get(value);
        return value;
    }

    private IOException corruptedStorageException() {
        return new IOException("Corrupted storage file: " + filePath);
    }

    private static String decodeKey(byte[] keyBytes) {
        return new String(keyBytes, StandardCharsets.UTF_8);
    }
}
