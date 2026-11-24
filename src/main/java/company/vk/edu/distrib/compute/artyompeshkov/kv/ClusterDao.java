package company.vk.edu.distrib.compute.artyompeshkov.kv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import company.vk.edu.distrib.compute.Dao;

class ClusterDao implements Dao<String> {
    private final List<RemoteDao> nodes;

    ClusterDao(int... ports) {
        if (ports.length == 0) {
            throw new IllegalArgumentException("Cluster must contain at least one node");
        }
        this.nodes = Arrays.stream(ports).mapToObj(RemoteDao::new).toList();
    }

    @Override
    public String get(String key) throws IOException {
        return ownerOf(key).get(key);
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        ownerOf(key).upsert(key, value);
    }

    @Override
    public void delete(String key) throws IOException {
        ownerOf(key).delete(key);
    }

    @Override
    public void close() {
        nodes.forEach(RemoteDao::close);
    }

    private RemoteDao ownerOf(String key) {
        int owner = 0;
        long ownerHash = hash(owner + ":" + key);
        for (int node = 1; node < nodes.size(); node++) {
            long nodeHash = hash(node + ":" + key);
            if (nodeHash > ownerHash) {
                owner = node;
                ownerHash = nodeHash;
            }
        }
        return nodes.get(owner);
    }

    private static long hash(String value) {
        try {
            byte[] result = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(result).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not supported", e);
        }
    }
}
