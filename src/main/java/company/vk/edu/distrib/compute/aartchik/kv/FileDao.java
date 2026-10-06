package company.vk.edu.distrib.compute.aartchik.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class FileDao implements Dao<byte[]> {
    private final Path directory;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean closed;

    FileDao(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }

    @Override
    public byte[] get(String key) throws IOException {
        Path path = pathFor(key);
        lock.readLock().lock();
        try {
            ensureOpen();
            return Files.readAllBytes(path);
        } catch (NoSuchFileException missing) {
            if (!Files.isDirectory(directory)) {
                throw missing;
            }
            throw new NoSuchElementException("Key not found", missing);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IOException {
        Path path = pathFor(key);
        lock.writeLock().lock();
        try {
            ensureOpen();
            Path temporary = Files.createTempFile(directory, "write-", ".tmp");
            try {
                write(temporary, value);
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(String key) throws IOException {
        Path path = pathFor(key);
        lock.writeLock().lock();
        try {
            ensureOpen();
            Files.deleteIfExists(path);
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean isAvailable() {
        lock.readLock().lock();
        try {
            return !closed && Files.isDirectory(directory)
                    && Files.isReadable(directory) && Files.isWritable(directory);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            closed = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Path pathFor(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(hash));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is unavailable", unavailable);
        }
    }

    private static void write(Path path, byte[] value) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(value);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("DAO is closed");
        }
    }
}
