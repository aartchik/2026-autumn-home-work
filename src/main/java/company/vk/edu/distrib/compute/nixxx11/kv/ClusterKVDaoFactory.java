package company.vk.edu.distrib.compute.nixxx11.kv;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import company.vk.edu.distrib.compute.kv.ClusterDaoFactoryTest;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.nixxx11.kv.dao.ClusterRemoteDao;

@ClusterDaoFactoryTest
public class ClusterKVDaoFactory implements RemoteDaoFactory<String> {
  @Override
  public ClusterRemoteDao create(final int... ports) throws IOException {
    final Set<URI> uris = new HashSet<>(ports.length);
    for (final int port : ports) {
      uris.add(URI.create("http://localhost:" + port + "/v0/entity"));
    }
    return new ClusterRemoteDao(uris);
  }
}
