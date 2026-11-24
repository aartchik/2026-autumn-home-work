package company.vk.edu.distrib.compute.vagifbaratov.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;

public class ClusterRemoteDao implements Dao<String> {
    private final int[] ports;
    private final RemoteDao[] nodes;

    public ClusterRemoteDao(String host, int... ports) {
        this.ports = ports.clone();
        this.nodes = new RemoteDao[ports.length];

        for (int i = 0; i < ports.length; i++) {
            nodes[i] = new RemoteDao(host, ports[i]);
        }
    }

    private RemoteDao owner(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must be non-empty.");
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }

        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        long bestScore = 0;
        int selected = -1;

        for (int i = 0; i < ports.length; i++) {
            digest.update((byte) (ports[i] >> Byte.SIZE));
            digest.update((byte) ports[i]);

            long score = ByteBuffer.wrap(digest.digest(keyBytes)).getLong();
            if (i == 0 || Long.compareUnsigned(score, bestScore) > 0) {
                bestScore = score;
                selected = i;
            }
        }
        return nodes[selected];
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        return owner(key).get(key);
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        owner(key).upsert(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        owner(key).delete(key);
    }

    @Override
    public void close() throws IOException {
        for (RemoteDao node : nodes) {
            node.close();
        }
    }
}
