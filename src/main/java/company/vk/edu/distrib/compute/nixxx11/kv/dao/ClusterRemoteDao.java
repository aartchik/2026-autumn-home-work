package company.vk.edu.distrib.compute.nixxx11.kv.dao;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import company.vk.edu.distrib.compute.Dao;

public class ClusterRemoteDao implements Dao<String> {
  private final List<RemoteDao> nodes;

  public ClusterRemoteDao(final Set<URI> baseUris) {
    this.nodes = new ArrayList<>(baseUris.size());
    try {
      for (final URI uri : baseUris) {
        nodes.add(new RemoteDao(uri));
      }
    } catch (final RuntimeException e) {
      closeNodes();
      throw e;
    }
  }

  @Override
  public String get(final String key) throws NoSuchElementException, IllegalArgumentException, IOException {
    final RemoteDao dao = chooseNode(key);
    return dao.get(key);
  }

  @Override
  public void upsert(final String key, final String value) throws IllegalArgumentException, IOException {
    final RemoteDao dao = chooseNode(key);
    dao.upsert(key, value);
  }

  @Override
  public void delete(final String key) throws IllegalArgumentException, IOException {
    final RemoteDao dao = chooseNode(key);
    dao.delete(key);
  }

  private RemoteDao chooseNode(final String key) {
    RemoteDao chosenNode = nodes.get(0);
    long maxWeight = rendezvousWeight(key, chosenNode.getBaseUri());
    for (int i = 1; i < nodes.size(); i++) {
      final RemoteDao node = nodes.get(i);
      final long weight = rendezvousWeight(key, node.getBaseUri());
      if (weight > maxWeight
          || (weight == maxWeight && chosenNode.getBaseUri().compareTo(node.getBaseUri()) > 0)) {
        maxWeight = weight;
        chosenNode = node;
      }
    }
    return chosenNode;
  }

  private static long rendezvousWeight(final String key, final URI node) {
    final MessageDigest sha256;
    try {
      sha256 = MessageDigest.getInstance("SHA-256");
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    sha256.update(key.getBytes(StandardCharsets.UTF_8));
    sha256.update(node.toString().getBytes(StandardCharsets.UTF_8));
    final byte[] digest = sha256.digest();
    return ByteBuffer.wrap(digest).getLong();
  }

  @Override
  public void close() {
    closeNodes();
  }

  private void closeNodes() {
    for (final RemoteDao dao : nodes) {
      dao.close();
    }
  }
}
