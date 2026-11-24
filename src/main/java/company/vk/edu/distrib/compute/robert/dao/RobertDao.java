package company.vk.edu.distrib.compute.robert.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.robert.validation.InputValidator;

public class RobertDao implements Dao<String> {
    private final ReentrantLock lock;

    private final Path directory;
    private final InputValidator<String> inputValidor;

    public RobertDao(
        Path storageRoot,
        String directoryName,
        InputValidator<String> initInputValidator
    ) throws IOException {
        lock = new ReentrantLock();
        directory = storageRoot.resolve(directoryName);
        inputValidor = initInputValidator;
        Files.createDirectories(directory);
    }

    @Override
    public void close() throws IOException {
        // No close
    }

    @Override
    public String get(String key) throws NoSuchElementException, IOException {
        inputValidor.validateKey(key);

        lock.lock();
        String value;
        try {
            Path file = key2file(key);
            if (!Files.exists(file)) { 
                throw new NoSuchElementException(); 
            }
            value = Files.readString(file);
        } finally {
            lock.unlock();
        }
        
        return value; 
    }

    public boolean create(String key, String value) throws IOException {
        inputValidor.validateKey(key);
        inputValidor.validateValue(value);
        
        lock.lock();
        try {
            Path file = key2file(key);
            if (Files.exists(file)) {
                return false; 
            }
            Files.writeString(file, value, StandardCharsets.UTF_8);
        } finally {
            lock.unlock();
        }
        
        return true;
    }

    public void update(String key, String value) throws NoSuchElementException, IOException {
        inputValidor.validateKey(key);
        inputValidor.validateValue(value);
        
        lock.lock();
        try {
            Path file = key2file(key);
            if (!Files.exists(file)) {
                throw new NoSuchElementException();
            } 
            Files.writeString(key2file(key), value, StandardCharsets.UTF_8);    
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        inputValidor.validateKey(key);
        inputValidor.validateValue(value);

        lock.lock();
        try {
            Files.writeString(key2file(key), value, StandardCharsets.UTF_8);
        } finally {
           lock.unlock();
        }    
    }

    @Override
    public void delete(String key) throws IOException {
        inputValidor.validateKey(key);
        
        lock.lock();
        try {
            Files.deleteIfExists(key2file(key));
        } finally {
            lock.unlock();        
        }
    }

    private Path key2file(String key) throws InvalidPathException {
        return directory.resolve(key + ".txt");
    }

}
