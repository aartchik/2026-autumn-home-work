package company.vk.edu.distrib.compute.mrglaster.kv.hashing;

import company.vk.edu.distrib.compute.mrglaster.kv.dao.EPRemoteDao;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class RendezvousHash {

    private static final String HASH_ALGORITHM = "SHA-256";

    public EPRemoteDao selectNode(String key, List<EPRemoteDao> nodes) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be null or blank");
        }
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes list cannot be null or empty");
        }

        EPRemoteDao bestNode = null;
        long bestHash = Long.MIN_VALUE;

        for (EPRemoteDao node : nodes) {
            long hash = hash(key, node);
            if (hash > bestHash) {
                bestHash = hash;
                bestNode = node;
            }
        }

        return bestNode;
    }

    private long hash(String key, EPRemoteDao node) {
        MessageDigest digest = createDigest();
        digest.update(key.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(node.getPort()).array());
        byte[] hashBytes = digest.digest();
        long folded = 0L;
        for (int i = 0; i < hashBytes.length; i += Long.BYTES) {
            long chunk = ByteBuffer.wrap(hashBytes, i, Long.BYTES).getLong();
            folded ^= chunk;
        }
        return folded;
    }

    private MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
