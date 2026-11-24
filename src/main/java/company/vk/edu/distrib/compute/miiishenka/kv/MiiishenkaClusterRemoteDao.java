package company.vk.edu.distrib.compute.miiishenka.kv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import company.vk.edu.distrib.compute.Dao;

public class MiiishenkaClusterRemoteDao implements Dao<String> {
    private static final Comparator<Map.Entry<Integer, Integer>> HASH_THEN_PORT_COMPARATOR =
            Map.Entry.<Integer, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey());
    private final Map<Integer, MiiishenkaRemoteDao> portToRemoteDao;

    public MiiishenkaClusterRemoteDao(int... ports) {
        if (ports == null || ports.length == 0) {
            throw new IllegalArgumentException("Should be at least 1 port");
        }
        portToRemoteDao = Arrays.stream(ports)
                .boxed()
                .collect(Collectors.toMap(Function.identity(), MiiishenkaRemoteDao::new));
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        return portToRemoteDao.get(selectPort(key)).get(key);
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        portToRemoteDao.get(selectPort(key)).upsert(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        portToRemoteDao.get(selectPort(key)).delete(key);
    }

    @Override
    public void close() throws IOException {
        for (MiiishenkaRemoteDao remoteDao : portToRemoteDao.values()) {
            remoteDao.close();
        }
    }

    private int selectPort(String key) {
        return portToRemoteDao.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), port -> hash(key, port)))
                .entrySet()
                .stream()
                .max(HASH_THEN_PORT_COMPARATOR)
                .orElseThrow()
                .getKey();
    }

    private static int hash(String key, int port) {
        return UUID.nameUUIDFromBytes((key + ':' + port).getBytes(StandardCharsets.UTF_8)).hashCode();
    }
}
