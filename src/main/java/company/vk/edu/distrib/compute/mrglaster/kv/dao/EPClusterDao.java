package company.vk.edu.distrib.compute.mrglaster.kv.dao;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.mrglaster.kv.hashing.RendezvousHash;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class EPClusterDao implements Dao<String> {

    private final List<EPRemoteDao> nodes = new CopyOnWriteArrayList<>();
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    private static final RendezvousHash RENDEZVOUS_HASH = new RendezvousHash();

    public EPClusterDao(int... ports) {
        if (ports == null || ports.length == 0) {
            throw new IllegalArgumentException("At least one node port must be provided");
        }
        Set<Integer> seenPorts = new HashSet<>();
        for (int port : ports) {
            if (MIN_PORT <= port && port <= MAX_PORT) {
                if (!seenPorts.add(port)) {
                    throw new IllegalArgumentException("Duplicate port: " + port);
                }
                nodes.add(new EPRemoteDao(HttpClient.newHttpClient(), port));
            } else {
                throw new IllegalArgumentException("Invalid port: " + port);
            }
        }
    }

    @Override
    public String get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        requireKey(key);
        return RENDEZVOUS_HASH.selectNode(key, nodes).get(key);
    }

    @Override
    public void upsert(String key, String value) throws IllegalArgumentException, IOException {
        requireKey(key);
        RENDEZVOUS_HASH.selectNode(key, nodes).upsert(key, value);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        requireKey(key);
        RENDEZVOUS_HASH.selectNode(key, nodes).delete(key);
    }

    @Override
    public void close() throws IOException {
        IOException firstException = null;
        for (EPRemoteDao node : nodes) {
            try {
                node.close();
            } catch (IOException e) {
                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
            }
        }
        if (firstException != null) {
            throw firstException;
        }
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be null or blank");
        }
    }
}
