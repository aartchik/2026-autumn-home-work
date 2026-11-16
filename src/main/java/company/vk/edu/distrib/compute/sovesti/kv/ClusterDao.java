package company.vk.edu.distrib.compute.sovesti.kv;

import static java.util.stream.Collectors.maxBy;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import company.vk.edu.distrib.compute.Dao;

final class ClusterDao implements Dao<byte[]> {

    private final Map<String, Dao<byte[]>> daos = new ConcurrentHashMap<>();

    void putShard(String key, Dao<byte[]> dao) {
        daos.put(key, dao);
    }

    @Override
    public void close() throws IOException {
        for (Dao<byte[]> dao : daos.values()) {
            dao.close();
        }
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        return rendezvouz(key).get(key);
    }

    @Override
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        rendezvouz(key).upsert(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        rendezvouz(key).delete(key);
    }

    private Dao<byte[]> rendezvouz(String key) {
        return daos.entrySet()
            .stream()
            .map(Shard::new)
            .collect(maxBy(shardComparator(key)))
            .orElseThrow(() -> new IllegalStateException("DAO cluster is empty"))
            .dao();
    }

    private Comparator<Shard> shardComparator(String key) {
        return Comparator.<Shard>comparingInt(shard -> hash(key, shard)).thenComparing(Shard::key);
    }

    private int hash(String key, Shard shard) {
        MessageDigest md = digest();
        md.update(shard.key().getBytes());
        return Arrays.hashCode(md.digest(key.getBytes()));
    }

    private MessageDigest digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Shard(String key, Dao<byte[]> dao) {

        private Shard(Entry<String, Dao<byte[]>> entry) {
            this(entry.getKey(), entry.getValue());
        }

    }

}
