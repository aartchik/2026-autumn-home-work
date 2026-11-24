package company.vk.edu.distrib.compute.tomovalex.kv;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClusterDao implements Dao<String> {
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private final List<RemoteDao> nodes = new ArrayList<>();

    public ClusterDao(int... ports) {
        validatePorts(ports);
        int[] sortedPorts = ports.clone();
        Arrays.sort(sortedPorts);
        for (int i = 0; i < sortedPorts.length; i++) {
            if (i > 0 && sortedPorts[i] == sortedPorts[i - 1]) {
                continue;
            }
            nodes.add(new RemoteDao(sortedPorts[i]));
        }
    }

    private static void validatePorts(int... ports) {
        if (ports == null || ports.length == 0) {
            throw new IllegalArgumentException();
        }
        for (int port : ports) {
            if (port < MIN_PORT || port > MAX_PORT) {
                throw new IllegalArgumentException("Invalid port: " + port);
            }
        }
    }

    @Override
    public String get(String key) throws IOException {
        return selectNode(key).get(key);
    }

    @Override
    public void upsert(String key, String value) throws IOException {
        selectNode(key).upsert(key, value);
    }

    @Override
    public void delete(String key) throws IOException {
        selectNode(key).delete(key);
    }

    @Override
    public void close() {
        for (RemoteDao node : nodes) {
            node.close();
        }
    }

    private RemoteDao selectNode(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException();
        }
        RemoteDao selectedNode = nodes.getFirst();
        long bestScore = Long.MIN_VALUE;
        for (RemoteDao node : nodes) {
            long score = hash(key + ":" + node.getPort());
            if (score > bestScore) {
                selectedNode = node;
                bestScore = score;
            }
        }
        return selectedNode;
    }

    private static long hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(bytes).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

}
